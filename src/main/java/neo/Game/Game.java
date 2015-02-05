package neo.Game;

import java.util.Scanner;
import neo.CM.CollisionModel.idCollisionModelManager;
import static neo.Game.AFEntity.GetArgString;
import neo.Game.AFEntity.GetJointTransform;
import neo.Game.AFEntity.idAFEntity_Base;
import neo.Game.AFEntity.idAFEntity_Generic;
import neo.Game.AFEntity.jointTransformData_t;
import static neo.Game.Animation.Anim.FRAME2MS;
import neo.Game.Animation.Anim.frameBlend_t;
import neo.Game.Animation.Anim.idMD5Anim;
import static neo.Game.Animation.Anim_Blend.ANIM_GetModelDefFromEntityDef;
import neo.Game.Animation.Anim_Blend.idAnim;
import neo.Game.Animation.Anim_Blend.idDeclModelDef;
import static neo.Game.Entity.AddRenderGui;
import static neo.Game.Entity.EV_Activate;
import static neo.Game.Entity.TH_THINK;
import neo.Game.Entity.idEntity;
import neo.Game.Game.idGame;
import neo.Game.Game.idGameEdit;
import static neo.Game.Game_local.MAX_GENTITIES;
import static neo.Game.Game_local.animationLib;
import static neo.Game.Game_local.gameLocal;
import static neo.Game.Game_local.gameSoundChannel_t.SND_CHANNEL_ANY;
import neo.Game.Player.idPlayer;
import static neo.Game.Sound.SSF_GLOBAL;
import static neo.Game.Sound.SSF_LOOPING;
import static neo.Game.Sound.SSF_NO_OCCLUSION;
import static neo.Game.Sound.SSF_OMNIDIRECTIONAL;
import static neo.Game.Sound.SSF_UNCLAMPED;
import neo.Renderer.Model.idMD5Joint;
import neo.Renderer.Model.idRenderModel;
import neo.Renderer.ModelManager.idRenderModelManager;
import static neo.Renderer.ModelManager.renderModelManager;
import neo.Renderer.RenderSystem.idRenderSystem;
import static neo.Renderer.RenderWorld.MAX_RENDERENTITY_GUI;
import static neo.Renderer.RenderWorld.SHADERPARM_BLUE;
import static neo.Renderer.RenderWorld.SHADERPARM_GREEN;
import static neo.Renderer.RenderWorld.SHADERPARM_MODE;
import static neo.Renderer.RenderWorld.SHADERPARM_RED;
import static neo.Renderer.RenderWorld.SHADERPARM_TIMEOFFSET;
import static neo.Renderer.RenderWorld.SHADERPARM_TIMESCALE;
import neo.Renderer.RenderWorld.idRenderWorld;
import neo.Renderer.RenderWorld.renderEntity_s;
import neo.Renderer.RenderWorld.renderLight_s;
import neo.Sound.snd_shader.idSoundShader;
import neo.Sound.snd_shader.soundShaderParms_t;
import neo.Sound.sound.idSoundEmitter;
import neo.Sound.sound.idSoundSystem;
import neo.Sound.sound.idSoundWorld;
import static neo.TempDump.NOT;
import static neo.TempDump.dynamic_cast;
import static neo.TempDump.etoi;
import static neo.TempDump.indexOf;
import static neo.TempDump.isNotNullOrEmpty;
import neo.Tools.Compilers.AAS.AASFileManager.idAASFileManager;
import neo.framework.Async.NetworkSystem.idNetworkSystem;
import neo.framework.CVarSystem.idCVarSystem;
import neo.framework.CmdSystem.idCmdSystem;
import neo.framework.Common.idCommon;
import neo.framework.DeclAF.declAFJointMod_t;
import static neo.framework.DeclAF.declAFJointMod_t.DECLAF_JOINTMOD_AXIS;
import static neo.framework.DeclAF.declAFJointMod_t.DECLAF_JOINTMOD_BOTH;
import static neo.framework.DeclAF.declAFJointMod_t.DECLAF_JOINTMOD_ORIGIN;
import neo.framework.DeclAF.idDeclAF;
import neo.framework.DeclAF.idDeclAF_Body;
import static neo.framework.DeclManager.declManager;
import static neo.framework.DeclManager.declType_t.DECL_AF;
import static neo.framework.DeclManager.declType_t.DECL_MODELDEF;
import neo.framework.DeclManager.idDecl;
import neo.framework.DeclManager.idDeclManager;
import neo.framework.FileSystem_h.idFileSystem;
import neo.framework.File_h.idFile;
import neo.framework.UsercmdGen.usercmd_t;
import neo.idlib.BitMsg.idBitMsg;
import neo.idlib.Dict_h.idDict;
import neo.idlib.Dict_h.idKeyValue;
import static neo.idlib.Lib.MAX_STRING_CHARS;
import neo.idlib.MapFile.idMapEntity;
import neo.idlib.MapFile.idMapFile;
import neo.idlib.Text.Str.idStr;
import static neo.idlib.Text.Str.va;
import neo.idlib.geometry.JointTransform.idJointMat;
import neo.idlib.geometry.JointTransform.idJointQuat;
import static neo.idlib.geometry.TraceModel.traceModel_t.TRM_BONE;
import neo.idlib.math.Angles.idAngles;
import static neo.idlib.math.Math_h.MS2SEC;
import neo.idlib.math.Math_h.idMath;
import neo.idlib.math.Matrix.idMat3;
import static neo.idlib.math.Matrix.idMat3.mat3_identity;
import static neo.idlib.math.Simd.SIMDProcessor;
import neo.idlib.math.Vector.idVec3;
import static neo.idlib.math.Vector.vec3_origin;
import neo.sys.sys_public.idSys;
import neo.ui.UserInterface.idUserInterface;
import neo.ui.UserInterface.idUserInterface.idUserInterfaceManager;

/**
 *
 */
public class Game {

    /*
     ===============================================================================

     Public game interface with methods to run the game.

     ===============================================================================
     */
    // default scripts
    public static final String SCRIPT_DEFAULTDEFS = "script/doom_defs.script";
    public static final String SCRIPT_DEFAULT     = "script/doom_main.script";
    public static final String SCRIPT_DEFAULTFUNC = "doom_main";
//

    public static class gameReturn_t {

        public char[] sessionCommand = new char[MAX_STRING_CHARS];    // "map", "disconnect", "victory", etc
        public int     consistencyHash;                         // used to check for network game divergence
        public int     health;
        public int     heartRate;
        public int     stamina;
        public int     combat;
        public boolean syncNextGameFrame;                       // used when cinematics are skipped to prevent session from simulating several game frames to
                                                                // keep the game time in sync with real time
    }

    ;

    public enum allowReply_t {

        ALLOW_YES,//= 0,
        ALLOW_BADPASS,      // core will prompt for password and connect again
        ALLOW_NOTYET,       // core will wait with transmitted message
        ALLOW_NO            // core will abort with transmitted message
    }

    ;

    public enum escReply_t {

        ESC_IGNORE,//= 0,	// do nothing
        ESC_MAIN,           // start main menu GUI
        ESC_GUI             // set an explicit GUI
    }

    ;
    //
    public static final int TIME_GROUP1 = 0;
    public static final int TIME_GROUP2 = 1;
//

    public static abstract class idGame {
        // virtual						~idGame() {}

        // Initialize the game for the first time.
        public abstract void Init();

        // Shut down the entire game.
        public abstract void Shutdown();

        // Set the local client number. Distinguishes listen ( == 0 ) / dedicated ( == -1 )
        public abstract void SetLocalClient(int clientNum);

        // Sets the user info for a client.
        // if canModify is true, the game can modify the user info in the returned dictionary pointer, server will forward the change back
        // canModify is never true on network client
        public abstract idDict SetUserInfo(int clientNum, final idDict userInfo, boolean isClient, boolean canModify);

        // Retrieve the game's userInfo dict for a client.
        public abstract idDict GetUserInfo(int clientNum);

        // The game gets a chance to alter userinfo before they are emitted to server.
        public abstract void ThrottleUserInfo();

        // Sets the serverinfo at map loads and when it changes.
        public abstract void SetServerInfo(final idDict serverInfo);

        // The session calls this before moving the single player game to a new level.
        public abstract idDict GetPersistentPlayerInfo(int clientNum);

        // The session calls this right before a new level is loaded.
        public abstract void SetPersistentPlayerInfo(int clientNum, final idDict playerInfo);

        // Loads a map and spawns all the entities.
        public abstract void InitFromNewMap(final String mapName, idRenderWorld renderWorld, idSoundWorld soundWorld, boolean isServer, boolean isClient, int randseed);

        // Loads a map from a savegame file.
        public abstract boolean InitFromSaveGame(final String mapName, idRenderWorld renderWorld, idSoundWorld soundWorld, idFile saveGameFile);

        // Saves the current game state, the session may have written some data to the file already.
        public abstract void SaveGame(idFile saveGameFile);

        // Shut down the current map.
        public abstract void MapShutdown();

        // Caches media referenced from in key/value pairs in the given dictionary.
        public abstract void CacheDictionaryMedia(final idDict dict);

        // Spawns the player entity to be used by the client.
        public abstract void SpawnPlayer(int clientNum);

        // Runs a game frame, may return a session command for level changing, etc
        public abstract gameReturn_t RunFrame(final usercmd_t[] clientCmds);

        // Makes rendering and sound system calls to display for a given clientNum.
        public abstract boolean Draw(int clientNum);

        // Let the game do it's own UI when ESCAPE is used
        public abstract escReply_t HandleESC(idUserInterface[] gui);

        // get the games menu if appropriate ( multiplayer )
        public abstract idUserInterface StartMenu();

        // When the game is running it's own UI fullscreen, GUI commands are passed through here
        // return NULL once the fullscreen UI mode should stop, or "main" to go to main menu
        public abstract String HandleGuiCommands(final String menuCommand);

        // main menu commands not caught in the engine are passed here
        public abstract void HandleMainMenuCommands(final String menuCommand, idUserInterface gui);

        // Early check to deny connect.
        public abstract allowReply_t ServerAllowClient(int numClients, final String IP, final String guid, final String password, char[] reason/*[MAX_STRING_CHARS]*/);

        // Connects a client.
        public abstract void ServerClientConnect(int clientNum, final String guid);

        // Spawns the player entity to be used by the client.
        public abstract void ServerClientBegin(int clientNum);

        // Disconnects a client and removes the player entity from the game.
        public abstract void ServerClientDisconnect(int clientNum);

        // Writes initial reliable messages a client needs to recieve when first joining the game.
        public abstract void ServerWriteInitialReliableMessages(int clientNum);

        // Writes a snapshot of the server game state for the given client.
        public abstract void ServerWriteSnapshot(int clientNum, int sequence, idBitMsg msg, byte[] clientInPVS, int numPVSClients);

        // Patches the network entity states at the server with a snapshot for the given client.
        public abstract boolean ServerApplySnapshot(int clientNum, int sequence);

        // Processes a reliable message from a client.
        public abstract void ServerProcessReliableMessage(int clientNum, final idBitMsg msg);

        // Reads a snapshot and updates the client game state.
        public abstract void ClientReadSnapshot(int clientNum, int sequence, final int gameFrame, final int gameTime, final int dupeUsercmds, final int aheadOfServer, final idBitMsg msg);

        // Patches the network entity states at the client with a snapshot.
        public abstract boolean ClientApplySnapshot(int clientNum, int sequence);

        // Processes a reliable message from the server.
        public abstract void ClientProcessReliableMessage(int clientNum, final idBitMsg msg);

        // Runs prediction on entities at the client.
        public abstract gameReturn_t ClientPrediction(int clientNum, final usercmd_t[] clientCmds, boolean lastPredictFrame);

        // Used to manage divergent time-lines
        public abstract void SelectTimeGroup(int timeGroup);

        public abstract int GetTimeGroupTime(int timeGroup);

        public abstract void GetBestGameType(final String map, final String gametype, char[] buf/*[ MAX_STRING_CHARS ]*/);

        // Returns a summary of stats for a given client
        public abstract void GetClientStats(int clientNum, String[] data, final int len);

        // Switch a player to a particular team
        public abstract void SwitchTeam(int clientNum, int team);

        public abstract boolean DownloadRequest(final String IP, final String guid, final String paks, char[] urls/*[ MAX_STRING_CHARS ]*/);

        public abstract void GetMapLoadingGUI(char[] gui/*[ MAX_STRING_CHARS ]*/);
    };

    public static class refSound_t {

        public idSoundEmitter     referenceSound;   // this is the interface to the sound system, created
                                                    // with idSoundWorld::AllocSoundEmitter() when needed
        public idVec3             origin;
        public int                listenerId;       // SSF_PRIVATE_SOUND only plays if == listenerId from PlaceListener
                                                    // no spatialization will be performed if == listenerID
        public idSoundShader      shader;           // this really shouldn't be here, it is a holdover from single channel behavior
        public float              diversity;        // 0.0 to 1.0 value used to select which
                                                    // samples in a multi-sample list from the shader are used
        public boolean            waitfortrigger;   // don't start it at spawn time
        public soundShaderParms_t parms;            // override volume, flags, etc
    };

    //enum {
    public static final int TEST_PARTICLE_MODEL    = 0;
    public static final int TEST_PARTICLE_IMPACT   = 1 + TEST_PARTICLE_MODEL;
    public static final int TEST_PARTICLE_MUZZLE   = 2 + TEST_PARTICLE_MODEL;
    public static final int TEST_PARTICLE_FLIGHT   = 3 + TEST_PARTICLE_MODEL;
    public static final int TEST_PARTICLE_SELECTED = 4 + TEST_PARTICLE_MODEL;
//};

    // FIXME: this interface needs to be reworked but it properly separates code for the time being
    public static class idGameEdit {

        // virtual						~idGameEdit() {}
        // These are the canonical idDict to parameter parsing routines used by both the game and tools.
        /*
         ================
         idGameEdit::ParseSpawnArgsToRenderLight

         parse the light parameters
         this is the canonical renderLight parm parsing,
         which should be used by dmap and the editor
         ================
         */
        public void ParseSpawnArgsToRenderLight(final idDict args, renderLight_s renderLight) {
            boolean gotTarget, gotUp, gotRight;
            String texture;
            idVec3 color = new idVec3();

            renderLight.clear();//memset( renderLight, 0, sizeof( *renderLight ) );

            if (!args.GetVector("light_origin", "", renderLight.origin)) {
                args.GetVector("origin", "", renderLight.origin);
            }

            gotTarget = args.GetVector("light_target", "", renderLight.target);
            gotUp = args.GetVector("light_up", "", renderLight.up);
            gotRight = args.GetVector("light_right", "", renderLight.right);
            args.GetVector("light_start", "0 0 0", renderLight.start);
            if (!args.GetVector("light_end", "", renderLight.end)) {
                renderLight.end = new idVec3(renderLight.target);
            }

            // we should have all of the target/right/up or none of them
            if ((gotTarget || gotUp || gotRight) != (gotTarget && gotUp && gotRight)) {
                gameLocal.Printf("Light at (%f,%f,%f) has bad target info\n",
                        renderLight.origin.oGet(0), renderLight.origin.oGet(1), renderLight.origin.oGet(2));
                return;
            }

            if (!gotTarget) {
                renderLight.pointLight = true;

                // allow an optional relative center of light and shadow offset
                args.GetVector("light_center", "0 0 0", renderLight.lightCenter);

                // create a point light
                if (!args.GetVector("light_radius", "300 300 300", renderLight.lightRadius)) {
                    float[] radius = {0};

                    args.GetFloat("light", "300", radius);
                    renderLight.lightRadius.oSet(0, renderLight.lightRadius.oSet(1, renderLight.lightRadius.oSet(2, radius[0])));
                }
            }

            // get the rotation matrix in either full form, or single angle form
            idAngles angles = new idAngles();
            idMat3 mat = new idMat3();
            if (!args.GetMatrix("light_rotation", "1 0 0 0 1 0 0 0 1", mat)) {
                if (!args.GetMatrix("rotation", "1 0 0 0 1 0 0 0 1", mat)) {
                    angles.oSet(1, args.GetFloat("angle", "0"));
                    angles.oSet(0, 0);
                    angles.oSet(1, idMath.AngleNormalize360(angles.oGet(1)));
                    angles.oSet(2, 0);
                    mat = angles.ToMat3();
                }
            }

            // fix degenerate identity matrices
            mat.oGet(0).FixDegenerateNormal();
            mat.oGet(1).FixDegenerateNormal();
            mat.oGet(2).FixDegenerateNormal();

            renderLight.axis = mat;

            // check for other attributes
            args.GetVector("_color", "1 1 1", color);
            renderLight.shaderParms[SHADERPARM_RED] = color.oGet(0);
            renderLight.shaderParms[SHADERPARM_GREEN] = color.oGet(1);
            renderLight.shaderParms[SHADERPARM_BLUE] = color.oGet(2);
            renderLight.shaderParms[SHADERPARM_TIMESCALE] = args.GetFloat("shaderParm3", "1");
            if (NOT(renderLight.shaderParms[SHADERPARM_TIMEOFFSET] = args.GetFloat("shaderParm4", "0"))) {
                // offset the start time of the shader to sync it to the game time
                renderLight.shaderParms[SHADERPARM_TIMEOFFSET] = -MS2SEC(gameLocal.time);
            }

            renderLight.shaderParms[5] = args.GetFloat("shaderParm5", "0");
            renderLight.shaderParms[6] = args.GetFloat("shaderParm6", "0");
            renderLight.shaderParms[SHADERPARM_MODE] = args.GetFloat("shaderParm7", "0");
            renderLight.noShadows = args.GetBool("noshadows", "0");
            renderLight.noSpecular = args.GetBool("nospecular", "0");
            renderLight.parallel = args.GetBool("parallel", "0");

            texture = args.GetString("texture", "lights/squarelight1");
            // allow this to be NULL
            renderLight.shader = declManager.FindMaterial(texture, false);
        }

        /*
         ================
         idGameEdit::ParseSpawnArgsToRenderEntity

         parse the static model parameters
         this is the canonical renderEntity parm parsing,
         which should be used by dmap and the editor
         ================
         */
        public void ParseSpawnArgsToRenderEntity(final idDict args, renderEntity_s renderEntity) {
            int i;
            String temp;
            idVec3 color = new idVec3();
            float angle;
            idDeclModelDef modelDef;

            renderEntity.clear();//	memset( renderEntity, 0, sizeof( *renderEntity ) );//TODO:clear?
            temp = args.GetString("model");

            modelDef = null;
            if (isNotNullOrEmpty(temp)) {
                modelDef = (idDeclModelDef) dynamic_cast(idDeclModelDef.class, declManager.FindType(DECL_MODELDEF, temp, false));
                if (modelDef != null) {
                    renderEntity.hModel = modelDef.ModelHandle();
                }
                if (NOT(renderEntity.hModel)) {
                    renderEntity.hModel = renderModelManager.FindModel(temp);
                }
            }
            if (renderEntity.hModel != null) {
                renderEntity.bounds = renderEntity.hModel.Bounds(renderEntity);
            } else {
                renderEntity.bounds.Zero();
            }

            temp = args.GetString("skin");
            if (isNotNullOrEmpty(temp)) {
                renderEntity.customSkin = declManager.FindSkin(temp);
            } else if (modelDef != null) {
                renderEntity.customSkin = modelDef.GetDefaultSkin();
            }

            temp = args.GetString("shader");
            if (isNotNullOrEmpty(temp)) {
                renderEntity.customShader = declManager.FindMaterial(temp);
            }

            args.GetVector("origin", "0 0 0", renderEntity.origin);

            // get the rotation matrix in either full form, or single angle form
            if (!args.GetMatrix("rotation", "1 0 0 0 1 0 0 0 1", renderEntity.axis)) {
                angle = args.GetFloat("angle");
                if (angle != 0.0f) {
                    renderEntity.axis = new idAngles(0.0f, angle, 0.0f).ToMat3();
                } else {
                    renderEntity.axis.Identity();
                }
            }

            renderEntity.referenceSound = null;

            // get shader parms
            args.GetVector("_color", "1 1 1", color);
            renderEntity.shaderParms[SHADERPARM_RED] = color.oGet(0);
            renderEntity.shaderParms[SHADERPARM_GREEN] = color.oGet(1);
            renderEntity.shaderParms[SHADERPARM_BLUE] = color.oGet(2);
            renderEntity.shaderParms[3] = args.GetFloat("shaderParm3", "1");
            renderEntity.shaderParms[4] = args.GetFloat("shaderParm4", "0");
            renderEntity.shaderParms[5] = args.GetFloat("shaderParm5", "0");
            renderEntity.shaderParms[6] = args.GetFloat("shaderParm6", "0");
            renderEntity.shaderParms[7] = args.GetFloat("shaderParm7", "0");
            renderEntity.shaderParms[8] = args.GetFloat("shaderParm8", "0");
            renderEntity.shaderParms[9] = args.GetFloat("shaderParm9", "0");
            renderEntity.shaderParms[10] = args.GetFloat("shaderParm10", "0");
            renderEntity.shaderParms[11] = args.GetFloat("shaderParm11", "0");

            // check noDynamicInteractions flag
            renderEntity.noDynamicInteractions = args.GetBool("noDynamicInteractions");

            // check noshadows flag
            renderEntity.noShadow = args.GetBool("noshadows");

            // check noselfshadows flag
            renderEntity.noSelfShadow = args.GetBool("noselfshadows");

            // init any guis, including entity-specific states
            for (i = 0; i < MAX_RENDERENTITY_GUI; i++) {
                temp = args.GetString(i == 0 ? "gui" : va("gui%d", i + 1));
                if (isNotNullOrEmpty(temp)) {
                    AddRenderGui(temp, renderEntity.gui[i], args);
                }
            }
        }

        /*
         ================
         idGameEdit::ParseSpawnArgsToRefSound

         parse the sound parameters
         this is the canonical refSound parm parsing,
         which should be used by dmap and the editor
         ================
         */
        public void ParseSpawnArgsToRefSound(final idDict args, refSound_t refSound) {
            String temp;

            //	memset( refSound, 0, sizeof( *refSound ) );//TODO:clear?
            refSound.parms.minDistance = args.GetFloat("s_mindistance");
            refSound.parms.maxDistance = args.GetFloat("s_maxdistance");
            refSound.parms.volume = args.GetFloat("s_volume");
            refSound.parms.shakes = args.GetFloat("s_shakes");

            args.GetVector("origin", "0 0 0", refSound.origin);

            refSound.referenceSound = null;

            // if a diversity is not specified, every sound start will make
            // a random one.  Specifying diversity is usefull to make multiple
            // lights all share the same buzz sound offset, for instance.
            refSound.diversity = args.GetFloat("s_diversity", "-1");
            refSound.waitfortrigger = args.GetBool("s_waitfortrigger");

            if (args.GetBool("s_omni")) {
                refSound.parms.soundShaderFlags |= SSF_OMNIDIRECTIONAL;
            }
            if (args.GetBool("s_looping")) {
                refSound.parms.soundShaderFlags |= SSF_LOOPING;
            }
            if (args.GetBool("s_occlusion")) {
                refSound.parms.soundShaderFlags |= SSF_NO_OCCLUSION;
            }
            if (args.GetBool("s_global")) {
                refSound.parms.soundShaderFlags |= SSF_GLOBAL;
            }
            if (args.GetBool("s_unclamped")) {
                refSound.parms.soundShaderFlags |= SSF_UNCLAMPED;
            }
            refSound.parms.soundClass = args.GetInt("s_soundClass");

            temp = args.GetString("s_shader");
            if (isNotNullOrEmpty(temp)) {
                refSound.shader = declManager.FindSound(temp);
            }
        }

        // Animation system calls for non-game based skeletal rendering.
        public idRenderModel ANIM_GetModelFromEntityDef(final String classname) {
            idDict args;

            args = gameLocal.FindEntityDefDict(classname, false);
            if (null == args) {
                return null;
            }

            return ANIM_GetModelFromEntityDef(args);
        }

        public idVec3 ANIM_GetModelOffsetFromEntityDef(final String classname) {
            idDict args;
            idDeclModelDef modelDef;

            args = gameLocal.FindEntityDefDict(classname, false);
            if (null == args) {
                return vec3_origin;
            }

            modelDef = ANIM_GetModelDefFromEntityDef(args);
            if (null == modelDef) {
                return vec3_origin;
            }

            return modelDef.GetVisualOffset();
        }

        public idRenderModel ANIM_GetModelFromEntityDef(final idDict args) {
            idRenderModel model;
            idDeclModelDef modelDef;

            model = null;

            String name = args.GetString("model");
            modelDef = (idDeclModelDef) declManager.FindType(DECL_MODELDEF, name, false);
            if (modelDef != null) {
                model = modelDef.ModelHandle();
            }

            if (null == model) {
                model = renderModelManager.FindModel(name);
            }

            if (model != null && model.IsDefaultModel()) {
                return null;
            }

            return model;
        }

        public idRenderModel ANIM_GetModelFromName(final String modelName) {
            idDeclModelDef modelDef;
            idRenderModel model;

            model = null;
            modelDef = (idDeclModelDef) declManager.FindType(DECL_MODELDEF, modelName, false);
            if (modelDef != null) {
                model = modelDef.ModelHandle();
            }
            if (null == model) {
                model = renderModelManager.FindModel(modelName);
            }
            return model;
        }

        public idMD5Anim ANIM_GetAnimFromEntityDef(final String classname, final String animname) {
            idDict args;
            idMD5Anim md5anim;
            idAnim anim;
            int animNum;
            String modelname;
            idDeclModelDef modelDef;

            args = gameLocal.FindEntityDefDict(classname, false);
            if (null == args) {
                return null;
            }

            md5anim = null;
            modelname = args.GetString("model");
            modelDef = (idDeclModelDef) declManager.FindType(DECL_MODELDEF, modelname, false);
            if (modelDef != null) {
                animNum = modelDef.GetAnim(animname);
                if (animNum != 0) {
                    anim = modelDef.GetAnim(animNum);
                    if (anim != null) {
                        md5anim = anim.MD5Anim(0);
                    }
                }
            }
            return md5anim;
        }

        public int ANIM_GetNumAnimsFromEntityDef(final idDict args) {
            String modelname;
            idDeclModelDef modelDef;

            modelname = args.GetString("model");
            modelDef = (idDeclModelDef) declManager.FindType(DECL_MODELDEF, modelname, false);
            if (modelDef != null) {
                return modelDef.NumAnims();
            }
            return 0;
        }

        public String ANIM_GetAnimNameFromEntityDef(final idDict args, int animNum) {
            String modelname;
            idDeclModelDef modelDef;

            modelname = args.GetString("model");
            modelDef = (idDeclModelDef) declManager.FindType(DECL_MODELDEF, modelname, false);
            if (modelDef != null) {
                final idAnim anim = modelDef.GetAnim(animNum);
                if (anim != null) {
                    return anim.FullName();
                }
            }
            return "";
        }

        public idMD5Anim ANIM_GetAnim(final String fileName) {
            return animationLib.GetAnim(fileName);
        }

        public int ANIM_GetLength(final idMD5Anim anim) {
            if (null == anim) {
                return 0;
            }
            return anim.Length();
        }

        public int ANIM_GetNumFrames(final idMD5Anim anim) {
            if (null == anim) {
                return 0;
            }
            return anim.NumFrames();
        }

        public void ANIM_CreateAnimFrame(final idRenderModel model, final idMD5Anim anim, int numJoints, idJointMat[] joints, int time, final idVec3 offset, boolean remove_origin_offset) {
            int i;
            frameBlend_t frame = new frameBlend_t();
            idMD5Joint[] md5joints;
            int[] index;

            if (null == model || model.IsDefaultModel() || null == anim) {
                return;
            }

            if (numJoints != model.NumJoints()) {
                gameLocal.Error("ANIM_CreateAnimFrame: different # of joints in renderEntity_t than in model (%s)", model.Name());
            }

            if (0 == model.NumJoints()) {
                // FIXME: Print out a warning?
                return;
            }

            if (null == joints) {
                gameLocal.Error("ANIM_CreateAnimFrame: NULL joint frame pointer on model (%s)", model.Name());
            }

            if (numJoints != anim.NumJoints()) {
                gameLocal.Warning("Model '%s' has different # of joints than anim '%s'", model.Name(), anim.Name());
                for (i = 0; i < numJoints; i++) {
                    joints[i].SetRotation(mat3_identity);
                    joints[i].SetTranslation(offset);
                }
                return;
            }

            // create index for all joints
            index = new int[numJoints];
            for (i = 0; i < numJoints; i++) {
                index[i] = i;
            }

            // create the frame
            anim.ConvertTimeToFrame(time, 1, frame);
            idJointQuat[] jointFrame = new idJointQuat[numJoints];
            anim.GetInterpolatedFrame(frame, jointFrame, index, numJoints);

            // convert joint quaternions to joint matrices
            SIMDProcessor.ConvertJointQuatsToJointMats(joints, jointFrame, numJoints);

            // first joint is always root of entire hierarchy
            if (remove_origin_offset) {
                joints[0].SetTranslation(offset);
            } else {
                joints[0].SetTranslation(joints[0].ToVec3().oPlus(offset));
            }

            // transform the children
            md5joints = model.GetJoints();
            for (i = 1; i < numJoints; i++) {
                joints[i].oMulSet(joints[indexOf(md5joints[i].parent, md5joints)]);
            }
        }

        public idRenderModel ANIM_CreateMeshForAnim(idRenderModel model, final String classname, String[] animName, int frame, boolean remove_origin_offset) {
            renderEntity_s ent;
            idDict args;
            String temp;
            idRenderModel newmodel;
            idMD5Anim md5anim;
            idStr filename;
            idStr extension = new idStr();
            idAnim anim;
            int animNum;
            idVec3 offset = new idVec3();
            idDeclModelDef modelDef;

            if (null == model || model.IsDefaultModel()) {
                return null;
            }

            args = gameLocal.FindEntityDefDict(classname, false);
            if (null == args) {
                return null;
            }

            ent = new renderEntity_s();//	memset( &ent, 0, sizeof( ent ) );

            ent.bounds.Clear();
            ent.suppressSurfaceInViewID = 0;

            modelDef = ANIM_GetModelDefFromEntityDef(args);
            if (modelDef != null) {
                animNum = modelDef.GetAnim(animName[0]);
                if (0 == animNum) {
                    return null;
                }
                anim = modelDef.GetAnim(animNum);
                if (null == anim) {
                    return null;
                }
                md5anim = anim.MD5Anim(0);
                ent.customSkin = modelDef.GetDefaultSkin();
                offset = modelDef.GetVisualOffset();
            } else {
                filename = new idStr(animName[0]);
                filename.ExtractFileExtension(extension);
                if (0 == extension.Length()) {
                    animName[0] = args.GetString(va("anim %s", animName[0]));
                }

                md5anim = animationLib.GetAnim(animName[0]);
                offset.Zero();
            }

            if (null == md5anim) {
                return null;
            }

            temp = args.GetString("skin", "");
            if (isNotNullOrEmpty(temp)) {
                ent.customSkin = declManager.FindSkin(temp);
            }

            ent.numJoints = model.NumJoints();
            ent.joints = new idJointMat[ent.numJoints];

            ANIM_CreateAnimFrame(model, md5anim, ent.numJoints, ent.joints, FRAME2MS(frame), offset, remove_origin_offset);

            newmodel = model.InstantiateDynamicModel(ent, null, null);

            ent.joints = null;//Mem_Free16(ent.joints);

            return newmodel;
        }

        // Articulated Figure calls for AF editor and Radiant.
        public boolean AF_SpawnEntity(final String fileName) {
            idDict args = new idDict();
            idPlayer player;
            idAFEntity_Generic ent;
            idDeclAF af;
            idVec3 org;
            float yaw;

            player = gameLocal.GetLocalPlayer();
            if (NOT(player) || !gameLocal.CheatsOk(false)) {
                return false;
            }

            af = (idDeclAF) dynamic_cast(idDeclAF.class, declManager.FindType(DECL_AF, fileName));
            if (NOT(af)) {
                return false;
            }

            yaw = player.viewAngles.yaw;
            args.Set("angle", va("%f", yaw + 180));
            org = player.GetPhysics().GetOrigin().oPlus(new idAngles(0, yaw, 0).ToForward().oMultiply(80).oPlus(new idVec3(0, 0, 1)));
            args.Set("origin", org.ToString());
            args.Set("spawnclass", "idAFEntity_Generic");
            if (isNotNullOrEmpty(af.model)) {
                args.Set("model", af.model.toString());
            } else {
                args.Set("model", fileName);
            }
            if (isNotNullOrEmpty(af.skin)) {
                args.Set("skin", af.skin.toString());
            }
            args.Set("articulatedFigure", fileName);
            args.Set("nodrop", "1");
            ent = (idAFEntity_Generic) gameLocal.SpawnEntityType(idAFEntity_Generic.class, args);

            // always update this entity
            ent.BecomeActive(TH_THINK);
            ent.KeepRunningPhysics();
            ent.fl.forcePhysicsUpdate = true;

            player.dragEntity.SetSelected(ent);

            return true;
        }

        public void AF_UpdateEntities(final String fileName) {
            idEntity ent;
            idAFEntity_Base af;
            idStr name;

            name = new idStr(fileName);
            name.StripFileExtension();

            // reload any idAFEntity_Generic which uses the given articulated figure file
            for (ent = gameLocal.spawnedEntities.Next(); ent != null; ent = ent.spawnNode.Next()) {
                if (ent.IsType(idAFEntity_Base.class)) {
                    af = (idAFEntity_Base) ent;
                    if (name.Icmp(af.GetAFName()) == 0) {
                        af.LoadAF();
                        af.GetAFPhysics().PutToRest();
                    }
                }
            }
        }

        public void AF_UndoChanges() {
            int i, c;
            idEntity ent;
            idAFEntity_Base af;
            idDeclAF decl;

            c = declManager.GetNumDecls(DECL_AF);
            for (i = 0; i < c; i++) {
                decl = (idDeclAF) (declManager.DeclByIndex(DECL_AF, i, false));
                if (!decl.modified) {
                    continue;
                }

                decl.Invalidate();
                declManager.FindType(DECL_AF, decl.GetName());

                // reload all AF entities using the file
                for (ent = gameLocal.spawnedEntities.Next(); ent != null; ent = ent.spawnNode.Next()) {
                    if (ent.IsType(idAFEntity_Base.class)) {
                        af = (idAFEntity_Base) ent;
                        if (idStr.Icmp(decl.GetName(), af.GetAFName()) == 0) {
                            af.LoadAF();
                        }
                    }
                }
            }
        }

        public idRenderModel AF_CreateMesh(final idDict args, idVec3 meshOrigin, idMat3 meshAxis, boolean []poseIsSet) {
            int i, jointNum;
            idDeclAF af;
            idDeclAF_Body fb = new idDeclAF_Body();
            renderEntity_s ent;
            idVec3 origin = new idVec3();
            idVec3[] bodyOrigin, newBodyOrigin, modifiedOrigin;
            idMat3 axis = new idMat3();
            idMat3[] bodyAxis, newBodyAxis, modifiedAxis;
            declAFJointMod_t[] jointMod;
            idAngles angles = new idAngles();
            idDict defArgs;
            idKeyValue arg;
            idStr name;
            jointTransformData_t data = new jointTransformData_t();
            String classname, afName, modelName;
            idRenderModel md5;
            idDeclModelDef modelDef;
            idMD5Anim MD5anim;
            idMD5Joint MD5joint;
            idMD5Joint[] MD5joints;
            int numMD5joints;
            idJointMat[] originalJoints;
            int parentNum;

            poseIsSet[0] = false;
            meshOrigin.Zero();
            meshAxis.Identity();

            classname = args.GetString("classname");
            defArgs = gameLocal.FindEntityDefDict(classname);

            // get the articulated figure
            afName = GetArgString(args, defArgs, "articulatedFigure");
            af = (idDeclAF) dynamic_cast(idDeclAF.class, declManager.FindType(DECL_AF, afName));
            if (NOT(af)) {
                return null;
            }

            // get the md5 model
            modelName = GetArgString(args, defArgs, "model");
            modelDef = (idDeclModelDef) dynamic_cast(idDeclModelDef.class, declManager.FindType(DECL_MODELDEF, modelName, false));
            if (NOT(modelDef)) {
                return null;
            }

            // make sure model hasn't been purged
            if (modelDef.ModelHandle() != null && !modelDef.ModelHandle().IsLoaded()) {
                modelDef.ModelHandle().LoadModel();
            }

            // get the md5
            md5 = modelDef.ModelHandle();
            if (NOT(md5) || md5.IsDefaultModel()) {
                return null;
            }

            // get the articulated figure pose anim
            int animNum = modelDef.GetAnim("af_pose");
            if (NOT(animNum)) {
                return null;
            }
            final idAnim anim = modelDef.GetAnim(animNum);
            if (NOT(anim)) {
                return null;
            }
            MD5anim = anim.MD5Anim(0);
            MD5joints = md5.GetJoints();
            numMD5joints = md5.NumJoints();

            // setup a render entity
            ent = new renderEntity_s();//memset( &ent, 0, sizeof( ent ) );
            ent.customSkin = modelDef.GetSkin();
            ent.bounds.Clear();
            ent.numJoints = numMD5joints;
            ent.joints = new idJointMat[ent.numJoints];

            // create animation from of the af_pose
            ANIM_CreateAnimFrame(md5, MD5anim, ent.numJoints, ent.joints, 1, modelDef.GetVisualOffset(), false);

            // buffers to store the initial origin and axis for each body
            bodyOrigin = new idVec3[af.bodies.Num()];
            bodyAxis = new idMat3[af.bodies.Num()];
            newBodyOrigin = new idVec3[af.bodies.Num()];
            newBodyAxis = new idMat3[af.bodies.Num()];

            // finish the AF positions
            data.ent = ent;
            data.joints = MD5joints;
            af.Finish(GetJointTransform.INSTANCE, ent.joints, data);

            // get the initial origin and axis for each AF body
            for (i = 0; i < af.bodies.Num(); i++) {
                fb = af.bodies.oGet(i);

                if (fb.modelType == TRM_BONE) {
                    // axis of bone trace model
                    axis.oSet(2, fb.v2.ToVec3().oMinus(fb.v1.ToVec3()));
                    axis.oGet(2).Normalize();
                    axis.oGet(2).NormalVectors(axis.oGet(0), axis.oGet(1));
                    axis.oSet(1, axis.oGet(1).oNegative());
                } else {
                    axis = fb.angles.ToMat3();
                }

                newBodyOrigin[i] = bodyOrigin[i] = fb.origin.ToVec3();
                newBodyAxis[i] = bodyAxis[i] = axis;
            }

            // get any new body transforms stored in the key/value pairs
            for (arg = args.MatchPrefix("body ", null); arg != null; arg = args.MatchPrefix("body ", arg)) {
                name = arg.GetKey();
                name.Strip("body ");
                for (i = 0; i < af.bodies.Num(); i++) {
                    fb = af.bodies.oGet(i);
                    if (fb.name.Icmp(name) == 0) {
                        break;
                    }
                }
                if (i >= af.bodies.Num()) {
                    continue;
                }
//		sscanf( arg.GetValue(), "%f %f %f %f %f %f", &origin.x, &origin.y, &origin.z, &angles.pitch, &angles.yaw, &angles.roll );
                Scanner sscanf = new Scanner(arg.GetValue().toString());
                origin.x = sscanf.nextFloat();
                origin.y = sscanf.nextFloat();
                origin.z = sscanf.nextFloat();
                angles.pitch = sscanf.nextFloat();
                angles.yaw = sscanf.nextFloat();
                angles.roll = sscanf.nextFloat();

                if (fb.jointName.Icmp("origin") == 0) {
                    meshAxis = bodyAxis[i].Transpose().oMultiply(angles.ToMat3());
                    meshOrigin.oSet(origin.oMinus(bodyOrigin[i].oMultiply(meshAxis)));
                    poseIsSet[0] = true;
                } else {
                    newBodyOrigin[i] = origin;
                    newBodyAxis[i] = angles.ToMat3();
                }
            }

            // save the original joints
            originalJoints = new idJointMat[numMD5joints];
            System.arraycopy(ent.joints, 0, originalJoints, 0, numMD5joints);//memcpy(originalJoints, ent.joints, numMD5joints * sizeof(originalJoints[0]));

            // buffer to store the joint mods
            jointMod = new declAFJointMod_t[numMD5joints];//memset(jointMod, -1, numMD5joints * sizeof(declAFJointMod_t));
            modifiedOrigin = new idVec3[numMD5joints];//memset(modifiedOrigin, 0, numMD5joints * sizeof(idVec3));
            for (int m = 0; m < modifiedOrigin.length; m++) {
                modifiedOrigin[m] = new idVec3();
            }
            modifiedAxis = new idMat3[numMD5joints];//memset(modifiedAxis, 0, numMD5joints * sizeof(idMat3));
            for (int m = 0; m < modifiedAxis.length; m++) {
                modifiedAxis[m] = new idMat3();
            }

            // get all the joint modifications
            for (i = 0; i < af.bodies.Num(); i++) {
                fb = af.bodies.oGet(i);

                if (fb.jointName.Icmp("origin") == 0) {
                    continue;
                }

                for (jointNum = 0; jointNum < numMD5joints; jointNum++) {
                    if (MD5joints[jointNum].name.Icmp(fb.jointName) == 0) {
                        break;
                    }
                }

                if (jointNum >= 0 && jointNum < ent.numJoints) {
                    jointMod[ jointNum] = fb.jointMod;
                    modifiedAxis[ jointNum] = (bodyAxis[i].oMultiply(originalJoints[jointNum].ToMat3().Transpose())).Transpose().oMultiply((newBodyAxis[i].oMultiply(meshAxis.Transpose())));
                    // FIXME: calculate correct modifiedOrigin
                    modifiedOrigin[ jointNum] = originalJoints[ jointNum].ToVec3();
                }
            }

            // apply joint modifications to the skeleton
            for (i = 1; i < numMD5joints; i++) {
                MD5joint = MD5joints[i];

                parentNum = indexOf(MD5joint.parent, MD5joints);
                idMat3 parentAxis = originalJoints[ parentNum].ToMat3();
                idMat3 localm = originalJoints[i].ToMat3().oMultiply(parentAxis.Transpose());
                idVec3 localt = (originalJoints[i].ToVec3().oMinus(originalJoints[ parentNum].ToVec3())).oMultiply(parentAxis.Transpose());

                switch (jointMod[i]) {
                    case DECLAF_JOINTMOD_ORIGIN: {
                        ent.joints[ i].SetRotation(localm.oMultiply(ent.joints[ parentNum].ToMat3()));
                        ent.joints[ i].SetTranslation(modifiedOrigin[ i]);
                        break;
                    }
                    case DECLAF_JOINTMOD_AXIS: {
                        ent.joints[ i].SetRotation(modifiedAxis[ i]);
                        ent.joints[ i].SetTranslation(ent.joints[ parentNum].ToVec3().oPlus(localt.oMultiply(ent.joints[ parentNum].ToMat3())));
                        break;
                    }
                    case DECLAF_JOINTMOD_BOTH: {
                        ent.joints[ i].SetRotation(modifiedAxis[ i]);
                        ent.joints[ i].SetTranslation(modifiedOrigin[ i]);
                        break;
                    }
                    default: {
                        ent.joints[ i].SetRotation(localm.oMultiply(ent.joints[ parentNum].ToMat3()));
                        ent.joints[ i].SetTranslation(ent.joints[ parentNum].ToVec3().oPlus(localt.oMultiply(ent.joints[ parentNum].ToMat3())));
                        break;
                    }
                }
            }

            // instantiate a mesh using the joint information from the render entity
            return md5.InstantiateDynamicModel(ent, null, null);
        }

        // Entity selection.
        public void ClearEntitySelection() {
            idEntity ent;

            for (ent = gameLocal.spawnedEntities.Next(); ent != null; ent = ent.spawnNode.Next()) {
                ent.fl.selected = false;
            }
            gameLocal.editEntities.ClearSelectedEntities();
        }

        public int GetSelectedEntities(idEntity[] list, int max) {
            int num = 0;
            idEntity ent;

            for (ent = gameLocal.spawnedEntities.Next(); ent != null; ent = ent.spawnNode.Next()) {
                if (ent.fl.selected) {
                    list[num++] = ent;
                    if (num >= max) {
                        break;
                    }
                }
            }
            return num;
        }

        public void AddSelectedEntity(idEntity ent) {
            if (ent != null) {
                gameLocal.editEntities.AddSelectedEntity(ent);
            }
        }

        // Selection methods
        public void TriggerSelected() {
            idEntity ent;
            for (ent = gameLocal.spawnedEntities.Next(); ent != null; ent = ent.spawnNode.Next()) {
                if (ent.fl.selected) {
                    ent.ProcessEvent(EV_Activate, gameLocal.GetLocalPlayer());
                }
            }
        }

        // Entity defs and spawning.
        public idDict FindEntityDefDict(final String name, boolean makeDefault /*= true*/) {
            return gameLocal.FindEntityDefDict(name, makeDefault);
        }

        public void SpawnEntityDef(final idDict args, idEntity[] ent) {
            gameLocal.SpawnEntityDef(args, ent);
        }

        public idEntity FindEntity(final String name) {
            return gameLocal.FindEntity(name);
        }

        /*
         =============
         idGameEdit::GetUniqueEntityName

         generates a unique name for a given classname
         =============
         */
        static StringBuilder name = new StringBuilder(1024);

        public String GetUniqueEntityName(final String classname) {
            int id;

            // can only have MAX_GENTITIES, so if we have a spot available, we're guaranteed to find one
            for (id = 0; id < MAX_GENTITIES; id++) {
                idStr.snPrintf(name, name.capacity(), "%s_%d", classname, id);
                if (NOT(gameLocal.FindEntity(name.toString()))) {
                    return name.toString();
                }
            }

            // id == MAX_GENTITIES + 1, which can't be in use if we get here
            idStr.snPrintf(name, name.capacity(), "%s_%d", classname, id);
            return name.toString();
        }

        // Entity methods.
        public void EntityGetOrigin(idEntity ent, idVec3 org) {
            if (ent != null) {
                org.oSet(ent.GetPhysics().GetOrigin());
            }
        }

        public void EntityGetAxis(idEntity ent, idMat3 axis) {
            if (ent != null) {
                axis.oSet(ent.GetPhysics().GetAxis());
            }
        }

        public void EntitySetOrigin(idEntity ent, final idVec3 org) {
            if (ent != null) {
                ent.SetOrigin(org);
            }
        }

        public void EntitySetAxis(idEntity ent, final idMat3 axis) {
            if (ent != null) {
                ent.SetAxis(axis);
            }
        }

        public void EntityTranslate(idEntity ent, final idVec3 org) {
            if (ent != null) {
                ent.GetPhysics().Translate(org);
            }
        }

        public idDict EntityGetSpawnArgs(idEntity ent) {
            if (ent != null) {
                return ent.spawnArgs;
            }
            return null;
        }

        public void EntityUpdateChangeableSpawnArgs(idEntity ent, final idDict dict) {
            if (ent != null) {
                ent.UpdateChangeableSpawnArgs(dict);
            }
        }

        public void EntityChangeSpawnArgs(idEntity ent, final idDict newArgs) {
            if (ent != null) {
                for (int i = 0; i < newArgs.GetNumKeyVals(); i++) {
                    final idKeyValue kv = newArgs.GetKeyVal(i);

                    if (kv.GetValue().Length() > 0) {
                        ent.spawnArgs.Set(kv.GetKey(), kv.GetValue());
                    } else {
                        ent.spawnArgs.Delete(kv.GetKey());
                    }
                }
            }
        }

        public void EntityUpdateVisuals(idEntity ent) {
            if (ent != null) {
                ent.UpdateVisuals();
            }
        }

        public void EntitySetModel(idEntity ent, final String val) {
            if (ent != null) {
                ent.spawnArgs.Set("model", val);
                ent.SetModel(val);
            }
        }

        public void EntityStopSound(idEntity ent) {
            if (ent != null) {
                ent.StopSound(etoi(SND_CHANNEL_ANY), false);
            }
        }

        public void EntityDelete(idEntity ent) {
        }

        public void EntitySetColor(idEntity ent, final idVec3 color) {
            if (ent != null) {
                ent.SetColor(color);
            }
        }

        // Player methods.
        public boolean PlayerIsValid() {
            return (gameLocal.GetLocalPlayer() != null);
        }

        public void PlayerGetOrigin(idVec3 org) {
            org.oSet(gameLocal.GetLocalPlayer().GetPhysics().GetOrigin());
        }

        public void PlayerGetAxis(idMat3 axis) {
            axis.oSet(gameLocal.GetLocalPlayer().GetPhysics().GetAxis());
        }

        public void PlayerGetViewAngles(idAngles angles) {
            angles.oSet(gameLocal.GetLocalPlayer().viewAngles);
        }

        public void PlayerGetEyePosition(idVec3 org) {
            org.oSet(gameLocal.GetLocalPlayer().GetEyePosition());
        }

        // In game map editing support.
        public idDict MapGetEntityDict(final String name) {
            idMapFile mapFile = gameLocal.GetLevelMap();
            if (mapFile != null && isNotNullOrEmpty(name)) {
                idMapEntity mapent = mapFile.FindEntity(name);
                if (mapent != null) {
                    return mapent.epairs;
                }
            }
            return null;
        }

        public void MapSave(final String path /*= NULL*/) {
            idMapFile mapFile = gameLocal.GetLevelMap();
            if (mapFile != null) {
                mapFile.Write((path != null) ? path : mapFile.GetName(), ".map");
            }
        }

        public void MapSetEntityKeyVal(final String name, final String key, final String val) {
            idMapFile mapFile = gameLocal.GetLevelMap();
            if (mapFile != null && isNotNullOrEmpty(name)) {
                idMapEntity mapent = mapFile.FindEntity(name);
                if (mapent != null) {
                    mapent.epairs.Set(key, val);
                }
            }
        }

        public void MapCopyDictToEntity(final String name, final idDict dict) {
            idMapFile mapFile = gameLocal.GetLevelMap();
            if (mapFile != null && isNotNullOrEmpty(name)) {
                idMapEntity mapent = mapFile.FindEntity(name);
                if (mapent != null) {
                    for (int i = 0; i < dict.GetNumKeyVals(); i++) {
                        final idKeyValue kv = dict.GetKeyVal(i);
                        final String key = kv.GetKey().toString();
                        final String val = kv.GetValue().toString();
                        mapent.epairs.Set(key, val);
                    }
                }
            }
        }

        public int MapGetUniqueMatchingKeyVals(final String key, final String[] list, final int max) {
            idMapFile mapFile = gameLocal.GetLevelMap();
            int count = 0;
            if (mapFile != null) {
                for (int i = 0; i < mapFile.GetNumEntities(); i++) {
                    idMapEntity ent = mapFile.GetEntity(i);
                    if (ent != null) {
                        final String k = ent.epairs.GetString(key);
                        if (isNotNullOrEmpty(k) && count < max) {
                            list[count++] = k;
                        }
                    }
                }
            }
            return count;
        }

        public void MapAddEntity(final idDict dict) {
            idMapFile mapFile = gameLocal.GetLevelMap();
            if (mapFile != null) {
                idMapEntity ent = new idMapEntity();
                ent.epairs = dict;
                mapFile.AddEntity(ent);
            }
        }

        public int MapGetEntitiesMatchingClassWithString(final String classname, final String match, final String[] list, final int max) {
            idMapFile mapFile = gameLocal.GetLevelMap();
            int count = 0;
            if (mapFile != null) {
                int entCount = mapFile.GetNumEntities();
                for (int i = 0; i < entCount; i++) {
                    idMapEntity ent = mapFile.GetEntity(i);
                    if (ent != null) {
                        idStr work = new idStr(ent.epairs.GetString("classname"));
                        if (work.Icmp(classname) == 0) {
                            if (isNotNullOrEmpty(match)) {
                                work.oSet(ent.epairs.GetString("soundgroup"));
                                if (count < max && work.Icmp(match) == 0) {
                                    list[count++] = ent.epairs.GetString("name");
                                }
                            } else if (count < max) {
                                list[count++] = ent.epairs.GetString("name");
                            }
                        }
                    }
                }
            }
            return count;
        }

        public void MapRemoveEntity(final String name) {
            idMapFile mapFile = gameLocal.GetLevelMap();
            if (mapFile != null) {
                idMapEntity ent = mapFile.FindEntity(name);
                if (ent != null) {
                    mapFile.RemoveEntity(ent);
                }
            }
        }

        public void MapEntityTranslate(final String name, final idVec3 v) {
            idMapFile mapFile = gameLocal.GetLevelMap();
            if (mapFile != null && isNotNullOrEmpty(name)) {
                idMapEntity mapent = mapFile.FindEntity(name);
                if (mapent != null) {
                    idVec3 origin = new idVec3();
                    mapent.epairs.GetVector("origin", "", origin);
                    origin.oPluSet(v);
                    mapent.epairs.SetVector("origin", origin);
                }
            }
        }

    };

    /*
     ===============================================================================

     Game API.

     ===============================================================================
     */
    public static final int GAME_API_VERSION = 8;

    public static class gameImport_t {

        public int                     version;                 // API version
        public idSys                   sys;                     // non-portable system services
        public idCommon                common;                  // common
        public idCmdSystem             cmdSystem;               // console command system
        public idCVarSystem            cvarSystem;              // console variable system
        public idFileSystem            fileSystem;              // file system
        public idNetworkSystem         networkSystem;           // network system
        public idRenderSystem          renderSystem;            // render system
        public idSoundSystem           soundSystem;             // sound system
        public idRenderModelManager    renderModelManager;      // render model manager
        public idUserInterfaceManager  uiManager;               // user interface manager
        public idDeclManager           declManager;             // declaration manager
        public idAASFileManager        AASFileManager;          // AAS file manager
        public idCollisionModelManager collisionModelManager;   // collision model manager
    };

    public static class gameExport_t {

        public int        version;                              // API version
        public idGame     game;                                 // interface to run the game
        public idGameEdit gameEdit;                             // interface for in-game editing
    }
    ;
//extern "C" {
//typedef gameExport_t * (*GetGameAPI_t)( gameImport_t *import );
//}
}
