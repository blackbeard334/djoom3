package neo.Renderer;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Objects;
import neo.Renderer.Interaction.idInteraction;
import static neo.Renderer.Material.MAX_ENTITY_SHADER_PARMS;
import neo.Renderer.Material.idMaterial;
import neo.Renderer.Model.idRenderModel;
import static neo.Renderer.RenderSystem_init.r_materialOverride;
import neo.Renderer.tr_local.areaReference_s;
import neo.Renderer.tr_local.idRenderEntityLocal;
import neo.Renderer.tr_local.idRenderLightLocal;
import static neo.Renderer.tr_local.tr;
import neo.Sound.sound.idSoundEmitter;
import neo.TempDump.Atomics;
import neo.TempDump.SERiAL;
import static neo.TempDump.isNotNullOrEmpty;
import neo.framework.CmdSystem.cmdFunction_t;
import static neo.framework.DeclManager.declManager;
import neo.framework.DeclSkin.idDeclSkin;
import neo.framework.DemoFile.idDemoFile;
import neo.idlib.BV.Bounds.idBounds;
import neo.idlib.BV.Box.idBox;
import neo.idlib.BV.Frustum.idFrustum;
import neo.idlib.BV.Sphere.idSphere;
import neo.idlib.CmdArgs.idCmdArgs;
import neo.idlib.Lib.idException;
import static neo.idlib.Lib.idLib.common;
import neo.idlib.geometry.JointTransform.idJointMat;
import neo.idlib.geometry.Winding.idFixedWinding;
import neo.idlib.geometry.Winding.idWinding;
import neo.idlib.math.Matrix.idMat3;
import neo.idlib.math.Plane.idPlane;
import neo.idlib.math.Vector.idVec3;
import neo.idlib.math.Vector.idVec4;
import static neo.idlib.math.Vector.vec3_origin;
import neo.ui.UserInterface.idUserInterface;

/**
 *
 */
public class RenderWorld {

    /*
     ===============================================================================

     Render World

     ===============================================================================
     */
    public static final String PROC_FILE_EXT = "proc";
    public static final String PROC_FILE_ID = "mapProcFile003";
    //
    // shader parms
    public static final int MAX_GLOBAL_SHADER_PARMS = 12;
    //
    public static final int SHADERPARM_RED = 0;
    public static final int SHADERPARM_GREEN = 1;
    public static final int SHADERPARM_BLUE = 2;
    public static final int SHADERPARM_ALPHA = 3;
    public static final int SHADERPARM_TIMESCALE = 3;
    public static final int SHADERPARM_TIMEOFFSET = 4;
    public static final int SHADERPARM_DIVERSITY = 5;          // random between 0.0 and 1.0 for some effects (muzzle flashes, etc)
    public static final int SHADERPARM_MODE = 7;               // for selecting which shader passes to enable
    public static final int SHADERPARM_TIME_OF_DEATH = 7;	// for the monster skin-burn-away effect enable and time offset
    //
    // model parms
    public static final int SHADERPARM_MD5_SKINSCALE = 8;	// for scaling vertex offsets on md5 models (jack skellington effect)
    //
    public static final int SHADERPARM_MD3_FRAME = 8;
    public static final int SHADERPARM_MD3_LASTFRAME = 9;
    public static final int SHADERPARM_MD3_BACKLERP = 10;
    //
    public static final int SHADERPARM_BEAM_END_X = 8;         // for _beam models
    public static final int SHADERPARM_BEAM_END_Y = 9;
    public static final int SHADERPARM_BEAM_END_Z = 10;
    public static final int SHADERPARM_BEAM_WIDTH = 11;
    //
    public static final int SHADERPARM_SPRITE_WIDTH = 8;
    public static final int SHADERPARM_SPRITE_HEIGHT = 9;
    //
    public static final int SHADERPARM_PARTICLE_STOPTIME = 8;	// don't spawn any more particles after this time
    //
    // guis
    public static final int MAX_RENDERENTITY_GUI = 3;
    //    

    public static abstract class deferredEntityCallback_t implements SERiAL {

        public abstract boolean run(renderEntity_s e, renderView_s v);
    }

    public static class renderEntity_s {

        public idRenderModel hModel;			// this can only be null if callback is set
        //
        public int entityNum;
        public int bodyId;
        //
        // Entities that are expensive to generate, like skeletal models, can be
        // deferred until their bounds are found to be in view, in the frustum
        // of a shadowing light that is in view, or contacted by a trace / overlay test.
        // This is also used to do visual cueing on items in the view
        // The renderView may be NULL if the callback is being issued for a non-view related
        // source.
        // The callback function should clear renderEntity->callback if it doesn't
        // want to be called again next time the entity is referenced (ie, if the
        // callback has now made the entity valid until the next updateEntity)
        public idBounds bounds;                         // only needs to be set for deferred models and md5s
        public deferredEntityCallback_t callback;
        //
        public ByteBuffer callbackData;			// used for whatever the callback wants
        //
        // player bodies and possibly player shadows should be suppressed in views from
        // that player's eyes, but will show up in mirrors and other subviews
        // security cameras could suppress their model in their subviews if we add a way
        // of specifying a view number for a remoteRenderMap view
        public int suppressSurfaceInViewID;
        public int suppressShadowInViewID;
        //
        // world models for the player and weapons will not cast shadows from view weapon
        // muzzle flashes
        public int suppressShadowInLightID;
        //
        // if non-zero, the surface and shadow (if it casts one)
        // will only show up in the specific view, ie: player weapons
        public int allowSurfaceInViewID;
        //
        // positioning
        // axis rotation vectors must be unit length for many
        // R_LocalToGlobal functions to work, so don't scale models!
        // axis vectors are [0] = forward, [1] = left, [2] = up
        public idVec3 origin;
        public idMat3 axis;
        //
        // texturing
        public idMaterial customShader;		// if non-0, all surfaces will use this
        public idMaterial referenceShader;		// used so flares can reference the proper light shader
        public idDeclSkin customSkin;			// 0 for no remappings
        public idSoundEmitter referenceSound;		// for shader sound tables, allowing effects to vary with sounds
        public final float[] shaderParms = new float[MAX_ENTITY_SHADER_PARMS];	// can be used in any way by shader or model generation
        // networking: see WriteGUIToSnapshot / ReadGUIFromSnapshot
        public idUserInterface[] gui = new idUserInterface[MAX_RENDERENTITY_GUI];
        //
        public renderView_s remoteRenderView;		// any remote camera surfaces will use this
        //
        public int numJoints;
        public idJointMat[] joints;			// array of joints that will modify vertices.
        // NULL if non-deformable model.  NOT freed by renderer
        //
        public float modelDepthHack;			// squash depth range so particle effects don't clip into walls
        //
        // options to override surface shader flags (replace with material parameters?)
        public boolean noSelfShadow;			// cast shadows onto other objects,but not self
        public boolean noShadow;			// no shadow at all
        //
        public boolean noDynamicInteractions;	// don't create any light / shadow interactions after
        // the level load is completed.  This is a performance hack
        // for the gigantic outdoor meshes in the monorail map, so
        // all the lights in the moving monorail don't touch the meshes
        //
        public boolean weaponDepthHack;		// squash depth range so view weapons don't poke into walls
        // this automatically implies noShadow
        public int forceUpdate;			// force an update (NOTE: not a bool to keep this struct a multiple of 4 bytes)//TODO:
        public int timeGroup;
        public int xrayIndex;

        public renderEntity_s() {
            this.origin = new idVec3();
            this.axis = new idMat3();
            this.bounds = new idBounds();
        }

        public renderEntity_s(renderEntity_s newEntity) {

            this.hModel = newEntity.hModel;
            this.entityNum = newEntity.entityNum;
            this.bodyId = newEntity.bodyId;
            this.bounds = new idBounds(newEntity.bounds);
            this.callback = newEntity.callback;
            this.callbackData = newEntity.callbackData;
            this.suppressSurfaceInViewID = newEntity.suppressSurfaceInViewID;
            this.suppressShadowInViewID = newEntity.suppressShadowInViewID;
            this.suppressShadowInLightID = newEntity.suppressShadowInLightID;
            this.allowSurfaceInViewID = newEntity.allowSurfaceInViewID;
            this.origin = new idVec3(newEntity.origin);
            this.axis = new idMat3(newEntity.axis);
            this.customShader = newEntity.customShader;
            this.referenceShader = newEntity.referenceShader;
            this.customSkin = newEntity.customSkin;
            this.referenceSound = newEntity.referenceSound;
            System.arraycopy(newEntity.shaderParms, 0, this.shaderParms, 0, this.shaderParms.length);
            System.arraycopy(newEntity.gui, 0, this.gui, 0, this.gui.length);
            this.remoteRenderView = newEntity.remoteRenderView;
            this.numJoints = newEntity.numJoints;
            this.joints = newEntity.joints;
            this.modelDepthHack = newEntity.modelDepthHack;
            this.noSelfShadow = newEntity.noSelfShadow;
            this.noShadow = newEntity.noShadow;
            this.noDynamicInteractions = newEntity.noDynamicInteractions;
            this.weaponDepthHack = newEntity.weaponDepthHack;
            this.forceUpdate = newEntity.forceUpdate;
            this.timeGroup = newEntity.timeGroup;
            this.xrayIndex = newEntity.xrayIndex;
        }

        public void atomicSet(Atomics.renderEntityShadow shadow) {
            this.hModel = shadow.hModel;
            this.entityNum = shadow.entityNum[0];
            this.bodyId = shadow.bodyId[0];
            this.bounds = shadow.bounds;
            this.callback = shadow.callback;
            this.callbackData = shadow.callbackData;
            this.suppressSurfaceInViewID = shadow.suppressSurfaceInViewID[0];
            this.suppressShadowInViewID = shadow.suppressShadowInViewID[0];
            this.suppressShadowInLightID = shadow.suppressShadowInLightID[0];
            this.allowSurfaceInViewID = shadow.allowSurfaceInViewID[0];
            this.origin = shadow.origin;
            this.axis = shadow.axis;
            this.customShader = shadow.customShader;
            this.referenceShader = shadow.referenceShader;
            this.customSkin = shadow.customSkin;
            this.referenceSound = shadow.referenceSound;
            this.remoteRenderView = shadow.remoteRenderView;
            this.numJoints = shadow.numJoints[0];
            this.joints = shadow.joints;
            this.modelDepthHack = shadow.modelDepthHack[0];
            this.noSelfShadow = shadow.noSelfShadow[0];
            this.noShadow = shadow.noShadow[0];
            this.noDynamicInteractions = shadow.noDynamicInteractions[0];
            this.weaponDepthHack = shadow.weaponDepthHack[0];
            this.forceUpdate = shadow.forceUpdate[0];
            this.timeGroup = shadow.timeGroup[0];
            this.xrayIndex = shadow.xrayIndex[0];
        }

        public void clear() {
            renderEntity_s newEntity = new renderEntity_s();

            this.hModel = newEntity.hModel;
            this.entityNum = newEntity.entityNum;
            this.bodyId = newEntity.bodyId;
            this.bounds = newEntity.bounds;
            this.callback = newEntity.callback;
            this.callbackData = newEntity.callbackData;
            this.suppressSurfaceInViewID = newEntity.suppressSurfaceInViewID;
            this.suppressShadowInViewID = newEntity.suppressShadowInViewID;
            this.suppressShadowInLightID = newEntity.suppressShadowInLightID;
            this.allowSurfaceInViewID = newEntity.allowSurfaceInViewID;
            this.origin = newEntity.origin;
            this.axis = newEntity.axis;
            this.customShader = newEntity.customShader;
            this.referenceShader = newEntity.referenceShader;
            this.customSkin = newEntity.customSkin;
            this.referenceSound = newEntity.referenceSound;
            this.remoteRenderView = newEntity.remoteRenderView;
            this.numJoints = newEntity.numJoints;
            this.joints = newEntity.joints;
            this.modelDepthHack = newEntity.modelDepthHack;
            this.noSelfShadow = newEntity.noSelfShadow;
            this.noShadow = newEntity.noShadow;
            this.noDynamicInteractions = newEntity.noDynamicInteractions;
            this.weaponDepthHack = newEntity.weaponDepthHack;
            this.forceUpdate = newEntity.forceUpdate;
            this.timeGroup = newEntity.timeGroup;
            this.xrayIndex = newEntity.xrayIndex;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 71 * hash + Objects.hashCode(this.hModel);
            hash = 71 * hash + this.entityNum;
            hash = 71 * hash + this.bodyId;
            hash = 71 * hash + Objects.hashCode(this.bounds);
            hash = 71 * hash + Objects.hashCode(this.callback);
            hash = 71 * hash + Objects.hashCode(this.callbackData);
            hash = 71 * hash + this.suppressSurfaceInViewID;
            hash = 71 * hash + this.suppressShadowInViewID;
            hash = 71 * hash + this.suppressShadowInLightID;
            hash = 71 * hash + this.allowSurfaceInViewID;
            hash = 71 * hash + Objects.hashCode(this.origin);
            hash = 71 * hash + Objects.hashCode(this.axis);
            hash = 71 * hash + Objects.hashCode(this.customShader);
            hash = 71 * hash + Objects.hashCode(this.referenceShader);
            hash = 71 * hash + Objects.hashCode(this.customSkin);
            hash = 71 * hash + Objects.hashCode(this.referenceSound);
            hash = 71 * hash + Arrays.hashCode(this.shaderParms);
            hash = 71 * hash + Arrays.deepHashCode(this.gui);
            hash = 71 * hash + Objects.hashCode(this.remoteRenderView);
            hash = 71 * hash + this.numJoints;
            hash = 71 * hash + Arrays.deepHashCode(this.joints);
            hash = 71 * hash + Float.floatToIntBits(this.modelDepthHack);
            hash = 71 * hash + (this.noSelfShadow ? 1 : 0);
            hash = 71 * hash + (this.noShadow ? 1 : 0);
            hash = 71 * hash + (this.noDynamicInteractions ? 1 : 0);
            hash = 71 * hash + (this.weaponDepthHack ? 1 : 0);
            hash = 71 * hash + this.forceUpdate;
            hash = 71 * hash + this.timeGroup;
            hash = 71 * hash + this.xrayIndex;
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final renderEntity_s other = (renderEntity_s) obj;
            if (!Objects.equals(this.hModel, other.hModel)) {
                return false;
            }
            if (this.entityNum != other.entityNum) {
                return false;
            }
            if (this.bodyId != other.bodyId) {
                return false;
            }
            if (!Objects.equals(this.bounds, other.bounds)) {
                return false;
            }
            if (!Objects.equals(this.callback, other.callback)) {
                return false;
            }
            if (!Objects.equals(this.callbackData, other.callbackData)) {
                return false;
            }
            if (this.suppressSurfaceInViewID != other.suppressSurfaceInViewID) {
                return false;
            }
            if (this.suppressShadowInViewID != other.suppressShadowInViewID) {
                return false;
            }
            if (this.suppressShadowInLightID != other.suppressShadowInLightID) {
                return false;
            }
            if (this.allowSurfaceInViewID != other.allowSurfaceInViewID) {
                return false;
            }
            if (!Objects.equals(this.origin, other.origin)) {
                return false;
            }
            if (!Objects.equals(this.axis, other.axis)) {
                return false;
            }
            if (!Objects.equals(this.customShader, other.customShader)) {
                return false;
            }
            if (!Objects.equals(this.referenceShader, other.referenceShader)) {
                return false;
            }
            if (!Objects.equals(this.customSkin, other.customSkin)) {
                return false;
            }
            if (!Objects.equals(this.referenceSound, other.referenceSound)) {
                return false;
            }
            if (!Arrays.equals(this.shaderParms, other.shaderParms)) {
                return false;
            }
            if (!Arrays.deepEquals(this.gui, other.gui)) {
                return false;
            }
            if (!Objects.equals(this.remoteRenderView, other.remoteRenderView)) {
                return false;
            }
            if (this.numJoints != other.numJoints) {
                return false;
            }
            if (!Arrays.deepEquals(this.joints, other.joints)) {
                return false;
            }
            if (Float.floatToIntBits(this.modelDepthHack) != Float.floatToIntBits(other.modelDepthHack)) {
                return false;
            }
            if (this.noSelfShadow != other.noSelfShadow) {
                return false;
            }
            if (this.noShadow != other.noShadow) {
                return false;
            }
            if (this.noDynamicInteractions != other.noDynamicInteractions) {
                return false;
            }
            if (this.weaponDepthHack != other.weaponDepthHack) {
                return false;
            }
            if (this.forceUpdate != other.forceUpdate) {
                return false;
            }
            if (this.timeGroup != other.timeGroup) {
                return false;
            }
            if (this.xrayIndex != other.xrayIndex) {
                return false;
            }
            return true;
        }
    };

    public static class renderLight_s {

        public idMat3 axis;				// rotation vectors, must be unit length
        public idVec3 origin = new idVec3();
        //
        // if non-zero, the light will not show up in the specific view,
        // which may be used if we want to have slightly different muzzle
        // flash lights for the player and other views
        public int suppressLightInViewID;
        //
        // if non-zero, the light will only show up in the specific view
        // which can allow player gun gui lights and such to not effect everyone
        public int allowLightInViewID;
        //
        // I am sticking the four bools together so there are no unused gaps in
        // the padded structure, which could confuse the memcmp that checks for redundant
        // updates
        public boolean noShadows;			// (should we replace this with material parameters on the shader?)
        public boolean noSpecular;			// (should we replace this with material parameters on the shader?)
        //
        public boolean pointLight;			// otherwise a projection light (should probably invert the sense of this, because points are way more common)
        public boolean parallel;			// lightCenter gives the direction to the light at infinity
        public idVec3 lightRadius = new idVec3();       // xyz radius for point lights
        public idVec3 lightCenter = new idVec3();       // offset the lighting direction for shading and
        // shadows, relative to origin
        //
        // frustum definition for projected lights, all reletive to origin
        // FIXME: we should probably have real plane equations here, and offer
        // a helper function for conversion from this format
        public idVec3 target = new idVec3();
        public idVec3 right = new idVec3();
        public idVec3 up = new idVec3();
        public idVec3 start = new idVec3();
        public idVec3 end = new idVec3();
        //
        // Dmap will generate an optimized shadow volume named _prelight_<lightName>
        // for the light against all the _area* models in the map.  The renderer will
        // ignore this value if the light has been moved after initial creation
        public idRenderModel prelightModel;
        //
        // muzzle flash lights will not cast shadows from player and weapon world models
        public int lightId;
        //
        //
        public idMaterial shader;			// NULL = either lights/defaultPointLight or lights/defaultProjectedLight
        public final float[] shaderParms = new float[MAX_ENTITY_SHADER_PARMS];		// can be used in any way by shader
        public idSoundEmitter referenceSound;		// for shader sound tables, allowing effects to vary with sounds

        public renderLight_s() {
        }

        //copy constructor
        public renderLight_s(final renderLight_s other) {
            this.axis = new idMat3(other.axis);
            this.origin = new idVec3(other.origin);

            this.suppressLightInViewID = other.suppressLightInViewID;

            this.allowLightInViewID = other.allowLightInViewID;

            this.noShadows = other.noShadows;
            this.noSpecular = other.noSpecular;

            this.pointLight = other.pointLight;
            this.parallel = other.parallel;
            this.lightRadius = new idVec3(other.lightRadius);
            this.lightCenter = new idVec3(other.lightCenter);

            this.target = new idVec3(other.target);
            this.right = new idVec3(other.right);
            this.up = new idVec3(other.up);
            this.start = new idVec3(other.start);
            this.end = new idVec3(other.end);

            this.prelightModel = other.prelightModel;

            this.lightId = other.lightId;

            this.shader = other.shader;
            System.arraycopy(other.shaderParms, 0, this.shaderParms, 0, other.shaderParms.length);
            this.referenceSound = other.referenceSound;
        }

        public void clear() {//TODO:hardcoded values
            final renderLight_s temp = new renderLight_s();
            this.axis = temp.axis;
            this.origin = temp.origin;
            this.suppressLightInViewID = temp.suppressLightInViewID;
            this.allowLightInViewID = temp.allowLightInViewID;
            this.noShadows = temp.noShadows;
            this.noSpecular = temp.noSpecular;
            this.pointLight = temp.pointLight;
            this.parallel = temp.parallel;
            this.lightRadius = temp.lightRadius;
            this.lightCenter = temp.lightCenter;
            this.target = temp.target;
            this.right = temp.right;
            this.up = temp.up;
            this.start = temp.start;
            this.end = temp.end;
            this.prelightModel = temp.prelightModel;
            this.lightId = temp.lightId;
            this.shader = temp.shader;
            this.referenceSound = temp.referenceSound;
        }

        void atomicSet(Atomics.renderLightShadow shadow) {
            this.axis = shadow.axis;
            this.origin = shadow.origin;

            this.suppressLightInViewID = shadow.suppressLightInViewID[0];

            this.allowLightInViewID = shadow.allowLightInViewID[0];

            this.noShadows = shadow.noShadows[0];
            this.noSpecular = shadow.noSpecular[0];

            this.pointLight = shadow.pointLight[0];
            this.parallel = shadow.parallel[0];
            this.lightRadius = shadow.lightRadius;
            this.lightCenter = shadow.lightCenter;

            this.target = shadow.target;
            this.right = shadow.right;
            this.up = shadow.up;
            this.start = shadow.start;

            this.end = shadow.end;

            this.prelightModel = shadow.prelightModel;

            this.lightId = shadow.lightId[0];
            this.shader = shadow.shader;
            this.referenceSound = shadow.referenceSound;
        }
    };

    public static class renderView_s implements SERiAL {
        // player views will set this to a non-zero integer for model suppress / allow
        // subviews (mirrors, cameras, etc) will always clear it to zero

        public int viewID;
        //
        // sized from 0 to SCREEN_WIDTH / SCREEN_HEIGHT (640/480), not actual resolution
        public int x, y, width, height;
        //
        public float fov_x, fov_y;
        public idVec3 vieworg = new idVec3();
        public idMat3 viewaxis = new idMat3();		// transformation matrix, view looks down the positive X axis
        //
        public boolean cramZNear;		// for cinematics, we want to set ZNear much lower
        public boolean forceUpdate;		// for an update 
        //
        // time in milliseconds for shader effects and other time dependent rendering issues
        public int time;
        public float[] shaderParms = new float[MAX_GLOBAL_SHADER_PARMS];// can be used in any way by shader
        public idMaterial globalMaterial;				 // used to override everything draw

        public renderView_s() {
        }

        renderView_s(final renderView_s renderView) {
            this.viewID = renderView.viewID;
            this.x = renderView.x;
            this.y = renderView.y;
            this.width = renderView.width;
            this.height = renderView.height;
            this.fov_x = renderView.fov_x;
            this.fov_y = renderView.fov_y;
            this.vieworg = new idVec3(renderView.vieworg);
            this.viewaxis = new idMat3(renderView.viewaxis);
            this.cramZNear = renderView.cramZNear;
            this.forceUpdate = renderView.forceUpdate;
            this.time = renderView.time;
            this.globalMaterial = renderView.globalMaterial;
        }

        public void atomicSet(Atomics.renderViewShadow shadow) {
            this.viewID = shadow.viewID[0];

            this.x = shadow.x[0];
            this.y = shadow.y[0];
            this.width = shadow.width[0];
            this.height = shadow.height[0];

            this.fov_x = shadow.fov_x[0];
            this.fov_y = shadow.fov_y[0];
            this.vieworg = new idVec3(shadow.vieworg);
            this.viewaxis = new idMat3(shadow.viewaxis);

            this.cramZNear = shadow.cramZNear[0];
            this.forceUpdate = shadow.forceUpdate[0];

            this.time = shadow.time[0];
            for (int a = 0; a < MAX_GLOBAL_SHADER_PARMS; a++) {
                this.shaderParms[a] = shadow.shaderParms[a][0];
            }
            this.globalMaterial = shadow.globalMaterial;
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
    };

    // exitPortal_t is returned by idRenderWorld::GetPortal()
    public static class exitPortal_t {

        public int[] areas = new int[2];	// areas connected by this portal
        public idWinding w;			// winding points have counter clockwise ordering seen from areas[0]
        public int blockingBits;               // PS_BLOCK_VIEW, PS_BLOCK_AIR, etc
        public int/*qhandle_t */ portalHandle;
    };

    // guiPoint_t is returned by idRenderWorld::GuiTrace()
    public static class guiPoint_t {

        public float x, y;			// 0.0 to 1.0 range if trace hit a gui, otherwise -1
        public int guiId;			// id of gui ( 0, 1, or 2 ) that the trace happened against
    };

    // modelTrace_t is for tracing vs. visual geometry
    public static class modelTrace_s {

        public float fraction;			// fraction of trace completed
        public idVec3 point;			// end point of trace in global space
        public idVec3 normal;			// hit triangle normal vector in global space
        public idMaterial material;		// material of hit surface
        public renderEntity_s entity;		// render entity that was hit
        public int jointNumber;                 // md5 joint nearest to the hit triangle

        public void clear() {
            this.point = new idVec3();
            this.normal = new idVec3();
            this.material = new idMaterial();
            this.entity = new renderEntity_s();
            this.fraction = this.jointNumber = 0;
        }

    };
    static final int NUM_PORTAL_ATTRIBUTES = 3;

    public enum portalConnection_t {

        PS_BLOCK_NONE,
        //
        PS_BLOCK_VIEW,
        PS_BLOCK_LOCATION,// game map location strings often stop in hallways
        /* *******************************************/ __3,
        PS_BLOCK_AIR, // windows between pressurized and unpresurized areas
        //
        PS_BLOCK_ALL;//= (1 << NUM_PORTAL_ATTRIBUTES) - 1;//TODO:
    };

    public static abstract class idRenderWorld {

        //	virtual					~idRenderWorld() {};
        // The same render world can be reinitialized as often as desired
        // a NULL or empty mapName will create an empty, single area world
        public abstract boolean InitFromMap(final String mapName) throws idException;

        //-------------- Entity and Light Defs -----------------
        // entityDefs and lightDefs are added to a given world to determine
        // what will be drawn for a rendered scene.  Most update work is defered
        // until it is determined that it is actually needed for a given view.
        public abstract int AddEntityDef(final renderEntity_s re);

        public abstract void UpdateEntityDef(int entityHandle, final renderEntity_s re);

        public abstract void FreeEntityDef(int entityHandle);

        public abstract renderEntity_s GetRenderEntity(int entityHandle);

        public abstract int AddLightDef(final renderLight_s rlight);

        public abstract void UpdateLightDef(int lightHandle, final renderLight_s rlight);

        public abstract void FreeLightDef(int lightHandle);

        public abstract renderLight_s GetRenderLight(int lightHandle);

        // Force the generation of all light / surface interactions at the start of a level
        // If this isn't called, they will all be dynamically generated
        public abstract void GenerateAllInteractions();

        // returns true if this area model needs portal sky to draw
        public abstract boolean CheckAreaForPortalSky(int areaNum);

        //-------------- Decals and Overlays  -----------------
        // Creates decals on all world surfaces that the winding projects onto.
        // The projection origin should be infront of the winding plane.
        // The decals are projected onto world geometry between the winding plane and the projection origin.
        // The decals are depth faded from the winding plane to a certain distance infront of the
        // winding plane and the same distance from the projection origin towards the winding.
        public abstract void ProjectDecalOntoWorld(final idFixedWinding winding, final idVec3 projectionOrigin, final boolean parallel, final float fadeDepth, final idMaterial material, final int startTime);

        // Creates decals on static models.
        public abstract void ProjectDecal(int entityHandle, final idFixedWinding winding, final idVec3 projectionOrigin, final boolean parallel, final float fadeDepth, final idMaterial material, final int startTime);

        // Creates overlays on dynamic models.
        public abstract void ProjectOverlay(int entityHandle, final idPlane[] localTextureAxis/*[2]*/, final idMaterial material);

        // Removes all decals and overlays from the given entity def.
        public abstract void RemoveDecals(int entityHandle);

        //-------------- Scene Rendering -----------------
        // some calls to material functions use the current renderview time when servicing cinematics.  this function
        // ensures that any parms accessed (such as time) are properly set.
        public abstract void SetRenderView(final renderView_s renderView);

        // rendering a scene may actually render multiple subviews for mirrors and portals, and
        // may render composite textures for gui console screens and light projections
        // It would also be acceptable to render a scene multiple times, for "rear view mirrors", etc
        public abstract void RenderScene(final renderView_s renderView);

        //-------------- Portal Area Information -----------------
        // returns the number of portals
        public abstract int NumPortals();

        // returns 0 if no portal contacts the bounds
        // This is used by the game to identify portals that are contained
        // inside doors, so the connection between areas can be topologically
        // terminated when the door shuts.
        public abstract int FindPortal(final idBounds b);

        // doors explicitly close off portals when shut
        // multiple bits can be set to block multiple things, ie: ( PS_VIEW | PS_LOCATION | PS_AIR )
        public abstract void SetPortalState(int portal, int blockingBits);

        public abstract int GetPortalState(int portal);

        // returns true only if a chain of portals without the given connection bits set
        // exists between the two areas (a door doesn't separate them, etc)
        public abstract boolean AreasAreConnected(int areaNum1, int areaNum2, portalConnection_t connection);

        // returns the number of portal areas in a map, so game code can build information
        // tables for the different areas
        public abstract int NumAreas();

        // Will return -1 if the point is not in an area, otherwise
        // it will return 0 <= value < NumAreas()
        public abstract int PointInArea(final idVec3 point);

        // fills the *areas array with the numbers of the areas the bounds cover
        // returns the total number of areas the bounds cover
        public abstract int BoundsInAreas(final idBounds bounds, int[] areas, int maxAreas);

        // Used by the sound system to do area flowing
        public abstract int NumPortalsInArea(int areaNum);

        // returns one portal from an area
        public abstract exitPortal_t GetPortal(int areaNum, int portalNum);

        //-------------- Tracing  -----------------
        // Checks a ray trace against any gui surfaces in an entity, returning the
        // fraction location of the trace on the gui surface, or -1,-1 if no hit.
        // This doesn't do any occlusion testing, simply ignoring non-gui surfaces.
        // start / end are in global world coordinates.
        public abstract guiPoint_t GuiTrace(int entityHandle, final idVec3 start, final idVec3 end);

        // Traces vs the render model, possibly instantiating a dynamic version, and returns true if something was hit
        public abstract boolean ModelTrace(modelTrace_s trace, int entityHandle, final idVec3 start, final idVec3 end, final float radius);

        // Traces vs the whole rendered world. FIXME: we need some kind of material flags.
        public abstract boolean Trace(modelTrace_s trace, final idVec3 start, final idVec3 end, final float radius, boolean skipDynamic /*= true*/, boolean skipPlayer/* = false*/);

        public boolean Trace(modelTrace_s trace, final idVec3 start, final idVec3 end, final float radius, boolean skipDynamic) {
            return Trace(trace, start, end, radius, skipDynamic, false);
        }

        public boolean Trace(modelTrace_s trace, final idVec3 start, final idVec3 end, final float radius) {
            return Trace(trace, start, end, radius, true);
        }

        // Traces vs the world model bsp tree.
        public abstract boolean FastWorldTrace(modelTrace_s trace, final idVec3 start, final idVec3 end);

        //-------------- Demo Control  -----------------
        // Writes a loadmap command to the demo, and clears archive counters.
        public abstract void StartWritingDemo(idDemoFile demo);

        public abstract void StopWritingDemo();

        // Returns true when demoRenderView has been filled in.
        // adds/updates/frees entityDefs and lightDefs based on the current demo file
        // and returns the renderView to be used to render this frame.
        // a demo file may need to be advanced multiple times if the framerate
        // is less than 30hz
        // demoTimeOffset will be set if a new map load command was processed before
        // the next renderScene
        public abstract boolean ProcessDemoCommand(idDemoFile readDemo, renderView_s demoRenderView, int[] demoTimeOffset);

        // this is used to regenerate all interactions ( which is currently only done during influences ), there may be a less 
        // expensive way to do it
        public abstract void RegenerateWorld();

        //-------------- Debug Visualization  -----------------
        // Line drawing for debug visualization
        public abstract void DebugClearLines(int time);		// a time of 0 will clear all lines and text

        public abstract void DebugLine(final idVec4 color, final idVec3 start, final idVec3 end, final int lifetime /*= 0*/, final boolean depthTest/* = false*/);

        public void DebugLine(final idVec4 color, final idVec3 start, final idVec3 end, final int lifetime /*= 0*/) {
            DebugLine(color, start, end, lifetime, false);
        }

        public void DebugLine(final idVec4 color, final idVec3 start, final idVec3 end) {
            DebugLine(color, start, end, 0);
        }

        public abstract void DebugArrow(final idVec4 color, final idVec3 start, final idVec3 end, int size, final int lifetime /*= 0*/);

        public void DebugArrow(final idVec4 color, final idVec3 start, final idVec3 end, int size) {
            DebugArrow(color, start, end, size, 0);
        }

        public abstract void DebugWinding(final idVec4 color, final idWinding w, final idVec3 origin, final idMat3 axis, final int lifetime /*= 0*/, final boolean depthTest /*= false*/);

        public void DebugWinding(final idVec4 color, final idWinding w, final idVec3 origin, final idMat3 axis, final int lifetime /*= 0*/) {
            DebugWinding(color, w, origin, axis, lifetime, false);
        }

        public void DebugWinding(final idVec4 color, final idWinding w, final idVec3 origin, final idMat3 axis) {
            DebugWinding(color, w, origin, axis, 0);
        }

        public abstract void DebugCircle(final idVec4 color, final idVec3 origin, final idVec3 dir, final float radius, final int numSteps, final int lifetime/* = 0*/, final boolean depthTest /*= false */);

        public void DebugCircle(final idVec4 color, final idVec3 origin, final idVec3 dir, final float radius, final int numSteps, final int lifetime/* = 0*/) {
            DebugCircle(color, origin, dir, radius, numSteps, lifetime, false);
        }

        public void DebugCircle(final idVec4 color, final idVec3 origin, final idVec3 dir, final float radius, final int numSteps) {
            DebugCircle(color, origin, dir, radius, numSteps, 0);
        }

        public abstract void DebugSphere(final idVec4 color, final idSphere sphere, final int lifetime/* = 0*/, boolean depthTest/* = false */);

        public void DebugSphere(final idVec4 color, final idSphere sphere, final int lifetime/* = 0*/) {
            DebugSphere(color, sphere, lifetime, false);
        }

        public void DebugSphere(final idVec4 color, final idSphere sphere) {
            DebugSphere(color, sphere, 0);
        }

        public abstract void DebugBounds(final idVec4 color, final idBounds bounds, final idVec3 org/* = vec3_origin*/, final int lifetime/* = 0*/);

        public void DebugBounds(final idVec4 color, final idBounds bounds, final idVec3 org/* = vec3_origin*/) {
            DebugBounds(color, bounds, org, 0);
        }

        public void DebugBounds(final idVec4 color, final idBounds bounds) {
            DebugBounds(color, bounds, vec3_origin);
        }

        public abstract void DebugBox(final idVec4 color, final idBox box, final int lifetime/* = 0*/);

        public void DebugBox(final idVec4 color, final idBox box) {
            DebugBox(color, box, 0);
        }

        public abstract void DebugFrustum(final idVec4 color, final idFrustum frustum, final boolean showFromOrigin/* = false*/, final int lifetime /*= 0*/);

        public void DebugFrustum(final idVec4 color, final idFrustum frustum, final boolean showFromOrigin/* = false*/) {
            DebugFrustum(color, frustum, showFromOrigin, 0);
        }

        public void DebugFrustum(final idVec4 color, final idFrustum frustum) {
            DebugFrustum(color, frustum, false);
        }

        public abstract void DebugCone(final idVec4 color, final idVec3 apex, final idVec3 dir, float radius1, float radius2, final int lifetime /*= 0*/);

        public void DebugCone(final idVec4 color, final idVec3 apex, final idVec3 dir, float radius1, float radius2) {
            DebugCone(color, apex, dir, radius1, radius2, 0);
        }

        public abstract void DebugAxis(final idVec3 origin, final idMat3 axis);

        // Polygon drawing for debug visualization.
        public abstract void DebugClearPolygons(int time);		// a time of 0 will clear all polygons

        public abstract void DebugPolygon(final idVec4 color, final idWinding winding, final int lifeTime/* = 0*/, final boolean depthTest /*= false*/);

        public void DebugPolygon(final idVec4 color, final idWinding winding, final int lifeTime/* = 0*/) {
            DebugPolygon(color, winding, lifeTime, false);
        }

        public void DebugPolygon(final idVec4 color, final idWinding winding) {
            DebugPolygon(color, winding, 0);
        }

        // Text drawing for debug visualization.
        public abstract void DrawText(final String text, final idVec3 origin, float scale, final idVec4 color, final idMat3 viewAxis, final int align /*= 1*/, final int lifetime /*= 0*/, boolean depthTest/* = false*/);

        public void DrawText(final String text, final idVec3 origin, float scale, final idVec4 color, final idMat3 viewAxis, final int align /*= 1*/, final int lifetime /*= 0*/) {
            DrawText(text, origin, scale, color, viewAxis, align, lifetime, false);
        }

        public void DrawText(final String text, final idVec3 origin, float scale, final idVec4 color, final idMat3 viewAxis, final int align /*= 1*/) {
            DrawText(text, origin, scale, color, viewAxis, align, 0);
        }

        public void DrawText(final String text, final idVec3 origin, float scale, final idVec4 color, final idMat3 viewAxis) {
            DrawText(text, origin, scale, color, viewAxis, 1);
        }
    };

    /*
     ===================
     R_ListRenderLightDefs_f
     ===================
     */
    public static class R_ListRenderLightDefs_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new R_ListRenderLightDefs_f();

        private R_ListRenderLightDefs_f() {
        }

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) {
            int i;
            idRenderLightLocal ldef;

            if (null == tr.primaryWorld) {
                return;
            }
            int active = 0;
            int totalRef = 0;
            int totalIntr = 0;

            for (i = 0; i < tr.primaryWorld.lightDefs.Num(); i++) {
                ldef = tr.primaryWorld.lightDefs.oGet(i);
                if (null == ldef) {
                    common.Printf("%4i: FREED\n", i);
                    continue;
                }

                // count up the interactions
                int iCount = 0;
                for (idInteraction inter = ldef.firstInteraction; inter != null; inter = inter.lightNext) {
                    iCount++;
                }
                totalIntr += iCount;

                // count up the references
                int rCount = 0;
                for (areaReference_s ref = ldef.references; ref != null; ref = ref.ownerNext) {
                    rCount++;
                }
                totalRef += rCount;

                common.Printf("%4i: %3i intr %2i refs %s\n", i, iCount, rCount, ldef.lightShader.GetName());
                active++;
            }

            common.Printf("%d lightDefs, %d interactions, %d areaRefs\n", active, totalIntr, totalRef);
        }
    };

    /*
     ===================
     R_ListRenderEntityDefs_f
     ===================
     */
    public static class R_ListRenderEntityDefs_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new R_ListRenderEntityDefs_f();

        private R_ListRenderEntityDefs_f() {
        }

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) {
            int i;
            idRenderEntityLocal mdef;

            if (null == tr.primaryWorld) {
                return;
            }
            int active = 0;
            int totalRef = 0;
            int totalIntr = 0;

            for (i = 0; i < tr.primaryWorld.entityDefs.Num(); i++) {
                mdef = tr.primaryWorld.entityDefs.oGet(i);
                if (null == mdef) {
                    common.Printf("%4i: FREED\n", i);
                    continue;
                }

                // count up the interactions
                int iCount = 0;
                for (idInteraction inter = mdef.firstInteraction; inter != null; inter = inter.entityNext) {
                    iCount++;
                }
                totalIntr += iCount;

                // count up the references
                int rCount = 0;
                for (areaReference_s ref = mdef.entityRefs; ref != null; ref = ref.ownerNext) {
                    rCount++;
                }
                totalRef += rCount;

                common.Printf("%4i: %3i intr %2i refs %s\n", i, iCount, rCount, mdef.parms.hModel.Name());
                active++;
            }

            common.Printf("total active: %d\n", active);
        }
    };

    /*
     ===============
     R_GlobalShaderOverride
     ===============
     */
    public static boolean R_GlobalShaderOverride(final idMaterial[] shader) throws idException {

        if (!shader[0].IsDrawn()) {
            return false;
        }

        if (tr.primaryRenderView.globalMaterial != null) {
            shader[0] = tr.primaryRenderView.globalMaterial;
            return true;
        }

        if (isNotNullOrEmpty(r_materialOverride.GetString())) {
            shader[0] = declManager.FindMaterial(r_materialOverride.GetString());
            return true;
        }

        return false;
    }

    /*
     ===============
     R_RemapShaderBySkin
     ===============
     */
    public static idMaterial R_RemapShaderBySkin(final idMaterial shader, final idDeclSkin skin, final idMaterial customShader) {

        if (null == shader) {
            return null;
        }

        // never remap surfaces that were originally nodraw, like collision hulls
        if (!shader.IsDrawn()) {
            return shader;
        }

        if (customShader != null) {
            // this is sort of a hack, but cause deformed surfaces to map to empty surfaces,
            // so the item highlight overlay doesn't highlight the autosprite surface
            if (shader.Deform() != null) {
                return null;
            }
            return customShader;
        }

        if (null == skin /*|| null == shader*/) {
            return shader;
        }

        return skin.RemapShaderBySkin(shader);
    }

}
