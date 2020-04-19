package neo.Game;

import static neo.Game.Entity.signalNum_t.NUM_SIGNALS;
import static neo.Game.Entity.signalNum_t.SIG_BLOCKED;
import static neo.Game.Entity.signalNum_t.SIG_REMOVED;
import static neo.Game.Entity.signalNum_t.SIG_TOUCH;
import static neo.Game.Entity.signalNum_t.SIG_TRIGGER;
import static neo.Game.GameSys.Class.EV_Remove;
import static neo.Game.GameSys.SysCvar.g_bloodEffects;
import static neo.Game.GameSys.SysCvar.g_decals;
import static neo.Game.Game_local.ENTITYNUM_NONE;
import static neo.Game.Game_local.GAME_RELIABLE_MESSAGE_DELETE_ENT;
import static neo.Game.Game_local.GAME_RELIABLE_MESSAGE_EVENT;
import static neo.Game.Game_local.GENTITYNUM_BITS;
import static neo.Game.Game_local.MASK_SOLID;
import static neo.Game.Game_local.MAX_CLIENTS;
import static neo.Game.Game_local.MAX_EVENT_PARAM_SIZE;
import static neo.Game.Game_local.MAX_GAME_MESSAGE_SIZE;
import static neo.Game.Game_local.MAX_GENTITIES;
import static neo.Game.Game_local.gameLocal;
import static neo.Game.Game_local.gameRenderWorld;
import static neo.Game.Game_local.gameSoundWorld;
import static neo.Game.Game_local.gameSoundChannel_t.SND_CHANNEL_ANY;
import static neo.Game.Game_local.gameSoundChannel_t.SND_CHANNEL_BODY;
import static neo.Game.Game_local.gameState_t.GAMESTATE_SHUTDOWN;
import static neo.Game.Game_local.gameState_t.GAMESTATE_STARTUP;
import static neo.Game.Mover.EV_PartBlocked;
import static neo.Game.Mover.EV_ReachedAng;
import static neo.Game.Mover.EV_ReachedPos;
import static neo.Game.Mover.EV_TeamBlocked;
import static neo.Game.Script.Script_Thread.EV_Thread_Wait;
import static neo.Game.Script.Script_Thread.EV_Thread_WaitFrame;
import static neo.Renderer.Material.CONTENTS_TRIGGER;
import static neo.Renderer.Material.MAX_ENTITY_SHADER_PARMS;
import static neo.Renderer.Material.surfTypes_t.SURFTYPE_METAL;
import static neo.Renderer.Material.surfTypes_t.SURFTYPE_NONE;
import static neo.Renderer.Model.INVALID_JOINT;
import static neo.Renderer.Model.dynamicModel_t.DM_CACHED;
import static neo.Renderer.ModelManager.renderModelManager;
import static neo.Renderer.RenderWorld.MAX_GLOBAL_SHADER_PARMS;
import static neo.Renderer.RenderWorld.MAX_RENDERENTITY_GUI;
import static neo.Renderer.RenderWorld.SHADERPARM_ALPHA;
import static neo.Renderer.RenderWorld.SHADERPARM_BLUE;
import static neo.Renderer.RenderWorld.SHADERPARM_GREEN;
import static neo.Renderer.RenderWorld.SHADERPARM_MODE;
import static neo.Renderer.RenderWorld.SHADERPARM_RED;
import static neo.TempDump.NOT;
import static neo.TempDump.atoi;
import static neo.TempDump.etoi;
import static neo.framework.Async.NetworkSystem.networkSystem;
import static neo.framework.Common.STRTABLE_ID;
import static neo.framework.Common.STRTABLE_ID_LENGTH;
import static neo.framework.DeclManager.declManager;
import static neo.framework.DeclManager.declType_t.DECL_ENTITYDEF;
import static neo.framework.DeclManager.declType_t.DECL_MATERIAL;
import static neo.framework.DeclManager.declType_t.DECL_PARTICLE;
import static neo.framework.DeclManager.declType_t.DECL_SOUND;
import static neo.idlib.Lib.LittleBitField;
import static neo.idlib.Lib.MAX_WORLD_SIZE;
import static neo.idlib.Lib.PackColor;
import static neo.idlib.Lib.UnpackColor;
import static neo.idlib.Lib.idLib.common;
import static neo.idlib.Text.Str.va;
import static neo.idlib.math.Math_h.MS2SEC;
import static neo.idlib.math.Matrix.idMat3.getMat3_identity;
import static neo.idlib.math.Vector.getVec3_origin;
import static neo.ui.UserInterface.uiManager;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import neo.TempDump;
import neo.CM.CollisionModel.trace_s;
import neo.Game.AFEntity.idAFEntity_Base;
import neo.Game.Actor.idActor;
import neo.Game.FX.idEntityFx;
import neo.Game.Game.refSound_t;
import neo.Game.Game_local.idEntityPtr;
import neo.Game.Game_local.idGameLocal;
import neo.Game.Player.idPlayer;
import neo.Game.Pvs.pvsHandle_t;
import neo.Game.Animation.Anim.jointModTransform_t;
import neo.Game.Animation.Anim_Blend.idAnim;
import neo.Game.Animation.Anim_Blend.idAnimator;
import neo.Game.GameSys.Class;
import neo.Game.GameSys.Class.eventCallback_t;
import neo.Game.GameSys.Class.eventCallback_t0;
import neo.Game.GameSys.Class.eventCallback_t1;
import neo.Game.GameSys.Class.eventCallback_t2;
import neo.Game.GameSys.Class.eventCallback_t3;
import neo.Game.GameSys.Class.eventCallback_t4;
import neo.Game.GameSys.Class.idEventArg;
import neo.Game.GameSys.Event.idEventDef;
import neo.Game.GameSys.SaveGame.idRestoreGame;
import neo.Game.GameSys.SaveGame.idSaveGame;
import neo.Game.Physics.Clip.idClipModel;
import neo.Game.Physics.Physics.idPhysics;
import neo.Game.Physics.Physics.impactInfo_s;
import neo.Game.Physics.Physics_AF.idPhysics_AF;
import neo.Game.Physics.Physics_Actor.idPhysics_Actor;
import neo.Game.Physics.Physics_Parametric.idPhysics_Parametric;
import neo.Game.Physics.Physics_Static.idPhysics_Static;
import neo.Game.Script.Script_Program.function_t;
import neo.Game.Script.Script_Program.idScriptObject;
import neo.Game.Script.Script_Thread.idThread;
import neo.Renderer.Material.idMaterial;
import neo.Renderer.RenderWorld.deferredEntityCallback_t;
import neo.Renderer.RenderWorld.renderEntity_s;
import neo.Renderer.RenderWorld.renderView_s;
import neo.Sound.snd_shader.idSoundShader;
import neo.Sound.sound.idSoundEmitter;
import neo.framework.DeclEntityDef.idDeclEntityDef;
import neo.framework.DeclParticle.idDeclParticle;
import neo.framework.DeclSkin.idDeclSkin;
import neo.idlib.BitMsg.idBitMsg;
import neo.idlib.BitMsg.idBitMsgDelta;
import neo.idlib.Dict_h.idDict;
import neo.idlib.Dict_h.idKeyValue;
import neo.idlib.BV.Bounds.idBounds;
import neo.idlib.Text.Lexer.idLexer;
import neo.idlib.Text.Str.idStr;
import neo.idlib.Text.Token.idToken;
import neo.idlib.containers.LinkList.idLinkList;
import neo.idlib.containers.List.idList;
import neo.idlib.geometry.JointTransform.idJointMat;
import neo.idlib.geometry.TraceModel.idTraceModel;
import neo.idlib.math.Angles.idAngles;
import neo.idlib.math.Curve.idCurve_BSpline;
import neo.idlib.math.Curve.idCurve_CatmullRomSpline;
import neo.idlib.math.Curve.idCurve_NURBS;
import neo.idlib.math.Curve.idCurve_NonUniformBSpline;
import neo.idlib.math.Curve.idCurve_Spline;
import neo.idlib.math.Math_h.idMath;
import neo.idlib.math.Plane.idPlane;
import neo.idlib.math.Vector.idVec3;
import neo.idlib.math.Vector.idVec4;
import neo.idlib.math.Matrix.idMat3;
import neo.open.Nio;
import neo.ui.UserInterface.idUserInterface;

/**
 *
 */
public class Entity {

    /*
     ===============================================================================

     Game entity base class.

     ===============================================================================
     */
    public static final int DELAY_DORMANT_TIME = 3000;
    //

    // overridable events
    public static final idEventDef EV_PostSpawn          = new idEventDef("<postspawn>", null);
    public static final idEventDef EV_FindTargets        = new idEventDef("<findTargets>", null);
    public static final idEventDef EV_Touch              = new idEventDef("<touch>", "et");
    public static final idEventDef EV_GetName            = new idEventDef("getName", null, 's');
    public static final idEventDef EV_SetName            = new idEventDef("setName", "s");
    public static final idEventDef EV_Activate           = new idEventDef("activate", "e");
    public static final idEventDef EV_ActivateTargets    = new idEventDef("activateTargets", "e");
    public static final idEventDef EV_NumTargets         = new idEventDef("numTargets", null, 'f');
    public static final idEventDef EV_GetTarget          = new idEventDef("getTarget", "f", 'e');
    public static final idEventDef EV_RandomTarget       = new idEventDef("randomTarget", "s", 'e');
    public static final idEventDef EV_Bind               = new idEventDef("bind", "e");
    public static final idEventDef EV_BindPosition       = new idEventDef("bindPosition", "e");
    public static final idEventDef EV_BindToJoint        = new idEventDef("bindToJoint", "esf");
    public static final idEventDef EV_Unbind             = new idEventDef("unbind", null);
    public static final idEventDef EV_RemoveBinds        = new idEventDef("removeBinds");
    public static final idEventDef EV_SpawnBind          = new idEventDef("<spawnbind>", null);
    public static final idEventDef EV_SetOwner           = new idEventDef("setOwner", "e");
    public static final idEventDef EV_SetModel           = new idEventDef("setModel", "s");
    public static final idEventDef EV_SetSkin            = new idEventDef("setSkin", "s");
    public static final idEventDef EV_GetWorldOrigin     = new idEventDef("getWorldOrigin", null, 'v');
    public static final idEventDef EV_SetWorldOrigin     = new idEventDef("setWorldOrigin", "v");
    public static final idEventDef EV_GetOrigin          = new idEventDef("getOrigin", null, 'v');
    public static final idEventDef EV_SetOrigin          = new idEventDef("setOrigin", "v");
    public static final idEventDef EV_GetAngles          = new idEventDef("getAngles", null, 'v');
    public static final idEventDef EV_SetAngles          = new idEventDef("setAngles", "v");
    public static final idEventDef EV_GetLinearVelocity  = new idEventDef("getLinearVelocity", null, 'v');
    public static final idEventDef EV_SetLinearVelocity  = new idEventDef("setLinearVelocity", "v");
    public static final idEventDef EV_GetAngularVelocity = new idEventDef("getAngularVelocity", null, 'v');
    public static final idEventDef EV_SetAngularVelocity = new idEventDef("setAngularVelocity", "v");
    public static final idEventDef EV_GetSize            = new idEventDef("getSize", null, 'v');
    public static final idEventDef EV_SetSize            = new idEventDef("setSize", "vv");
    public static final idEventDef EV_GetMins            = new idEventDef("getMins", null, 'v');
    public static final idEventDef EV_GetMaxs            = new idEventDef("getMaxs", null, 'v');
    public static final idEventDef EV_IsHidden           = new idEventDef("isHidden", null, 'd');
    public static final idEventDef EV_Hide               = new idEventDef("hide", null);
    public static final idEventDef EV_Show               = new idEventDef("show", null);
    public static final idEventDef EV_Touches            = new idEventDef("touches", "E", 'd');
    public static final idEventDef EV_ClearSignal        = new idEventDef("clearSignal", "d");
    public static final idEventDef EV_GetShaderParm      = new idEventDef("getShaderParm", "d", 'f');
    public static final idEventDef EV_SetShaderParm      = new idEventDef("setShaderParm", "df");
    public static final idEventDef EV_SetShaderParms     = new idEventDef("setShaderParms", "ffff");
    public static final idEventDef EV_SetColor           = new idEventDef("setColor", "fff");
    public static final idEventDef EV_GetColor           = new idEventDef("getColor", null, 'v');
    public static final idEventDef EV_CacheSoundShader   = new idEventDef("cacheSoundShader", "s");
    public static final idEventDef EV_StartSoundShader   = new idEventDef("startSoundShader", "sd", 'f');
    public static final idEventDef EV_StartSound         = new idEventDef("startSound", "sdd", 'f');
    public static final idEventDef EV_StopSound          = new idEventDef("stopSound", "dd");
    public static final idEventDef EV_FadeSound          = new idEventDef("fadeSound", "dff");
    public static final idEventDef EV_SetGuiParm         = new idEventDef("setGuiParm", "ss");
    public static final idEventDef EV_SetGuiFloat        = new idEventDef("setGuiFloat", "sf");
    public static final idEventDef EV_GetNextKey         = new idEventDef("getNextKey", "ss", 's');
    public static final idEventDef EV_SetKey             = new idEventDef("setKey", "ss");
    public static final idEventDef EV_GetKey             = new idEventDef("getKey", "s", 's');
    public static final idEventDef EV_GetIntKey          = new idEventDef("getIntKey", "s", 'f');
    public static final idEventDef EV_GetFloatKey        = new idEventDef("getFloatKey", "s", 'f');
    public static final idEventDef EV_GetVectorKey       = new idEventDef("getVectorKey", "s", 'v');
    public static final idEventDef EV_GetEntityKey       = new idEventDef("getEntityKey", "s", 'e');
    public static final idEventDef EV_RestorePosition    = new idEventDef("restorePosition");
    public static final idEventDef EV_UpdateCameraTarget = new idEventDef("<updateCameraTarget>", null);
    public static final idEventDef EV_DistanceTo         = new idEventDef("distanceTo", "E", 'f');
    public static final idEventDef EV_DistanceToPoint    = new idEventDef("distanceToPoint", "v", 'f');
    public static final idEventDef EV_StartFx            = new idEventDef("startFx", "s");
    public static final idEventDef EV_HasFunction        = new idEventDef("hasFunction", "s", 'd');
    public static final idEventDef EV_CallFunction       = new idEventDef("callFunction", "s");
    public static final idEventDef EV_SetNeverDormant    = new idEventDef("setNeverDormant", "d");
    //
    public static final idEventDef EV_GetJointHandle     = new idEventDef("getJointHandle", "s", 'd');
    public static final idEventDef EV_ClearAllJoints     = new idEventDef("clearAllJoints");
    public static final idEventDef EV_ClearJoint         = new idEventDef("clearJoint", "d");
    public static final idEventDef EV_SetJointPos        = new idEventDef("setJointPos", "ddv");
    public static final idEventDef EV_SetJointAngle      = new idEventDef("setJointAngle", "ddv");
    public static final idEventDef EV_GetJointPos        = new idEventDef("getJointPos", "d", 'v');
    public static final idEventDef EV_GetJointAngle      = new idEventDef("getJointAngle", "d", 'v');
    //

    // Think flags
//enum {
    public static final int TH_ALL             = -1;
    public static final int TH_THINK           = 1;        // run think function each frame
    public static final int TH_PHYSICS         = 2;        // run physics each frame
    public static final int TH_ANIMATE         = 4;        // update animation each frame
    public static final int TH_UPDATEVISUALS   = 8;        // update renderEntity
    public static final int TH_UPDATEPARTICLES = 16;
//};

    //
    // Signals
    // make sure to change script/doom_defs.script if you add any, or change their order
    //
    public enum signalNum_t {

        SIG_TOUCH,  // object was touched
        SIG_USE,    // object was used
        SIG_TRIGGER,// object was activated
        SIG_REMOVED,// object was removed from the game
        SIG_DAMAGE, // object was damaged
        SIG_BLOCKED,// object was blocked
        //
        SIG_MOVER_POS1, // mover at position 1 (door closed)
        SIG_MOVER_POS2, // mover at position 2 (door open)
        SIG_MOVER_1TO2, // mover changing from position 1 to 2
        SIG_MOVER_2TO1, // mover changing from position 2 to 1
        //
        NUM_SIGNALS
    }

    // FIXME: At some point we may want to just limit it to one thread per signal, but
    // for now, I'm allowing multiple threads.  We should reevaluate this later in the project
    public static final int MAX_SIGNAL_THREADS = 16;    // probably overkill, but idList uses a granularity of 16
    //

    public static class signal_t {

        int        threadnum;
        function_t function;
    }

    public static class signalList_t {

        public idList<signal_t>[] signal = new idList[etoi(NUM_SIGNALS)];
    }

    public static class idEntity extends neo.Game.GameSys.Class.idClass implements neo.TempDump.NiLLABLE<idEntity>, neo.TempDump.SERiAL {
        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		//	ABSTRACT_PROTOTYPE( idEntity );
        private static Map<idEventDef, eventCallback_t> eventCallbacks = new HashMap<>();
        static {
            eventCallbacks.putAll(Class.idClass.getEventCallBacks());
            eventCallbacks.put(EV_GetName, (eventCallback_t0<idEntity>) idEntity::Event_GetName);
            eventCallbacks.put(EV_SetName, (eventCallback_t1<idEntity>) idEntity::Event_SetName);
            eventCallbacks.put(EV_FindTargets, (eventCallback_t0<idEntity>) idEntity::Event_FindTargets);
            eventCallbacks.put(EV_ActivateTargets, (eventCallback_t1<idEntity>) idEntity::Event_ActivateTargets);
            eventCallbacks.put(EV_NumTargets, (eventCallback_t0<idEntity>) idEntity::Event_NumTargets);
            eventCallbacks.put(EV_GetTarget, (eventCallback_t1<idEntity>) idEntity::Event_GetTarget);
            eventCallbacks.put(EV_RandomTarget, (eventCallback_t1<idEntity>) idEntity::Event_RandomTarget);
            eventCallbacks.put(EV_BindToJoint, (eventCallback_t3<idEntity>) idEntity::Event_BindToJoint);
            eventCallbacks.put(EV_RemoveBinds, (eventCallback_t0<idEntity>) idEntity::Event_RemoveBinds);
            eventCallbacks.put(EV_Bind, (eventCallback_t1<idEntity>) idEntity::Event_Bind);
            eventCallbacks.put(EV_BindPosition, (eventCallback_t1<idEntity>) idEntity::Event_BindPosition);
            eventCallbacks.put(EV_Unbind, (eventCallback_t0<idEntity>) idEntity::Event_Unbind);
            eventCallbacks.put(EV_SpawnBind, (eventCallback_t0<idEntity>) idEntity::Event_SpawnBind);
            eventCallbacks.put(EV_SetOwner, (eventCallback_t1<idEntity>) idEntity::Event_SetOwner);
            eventCallbacks.put(EV_SetModel, (eventCallback_t1<idEntity>) idEntity::Event_SetModel);
            eventCallbacks.put(EV_SetSkin, (eventCallback_t1<idEntity>) idEntity::Event_SetSkin);
            eventCallbacks.put(EV_GetShaderParm, (eventCallback_t1<idEntity>) idEntity::Event_GetShaderParm);
            eventCallbacks.put(EV_SetShaderParm, (eventCallback_t2<idEntity>) idEntity::Event_SetShaderParm);
            eventCallbacks.put(EV_SetShaderParms, (eventCallback_t4<idEntity>) idEntity::Event_SetShaderParms);
            eventCallbacks.put(EV_SetColor, (eventCallback_t3<idEntity>) idEntity::Event_SetColor);
            eventCallbacks.put(EV_GetColor, (eventCallback_t0<idEntity>) idEntity::Event_GetColor);
            eventCallbacks.put(EV_IsHidden, (eventCallback_t0<idEntity>) idEntity::Event_IsHidden);
            eventCallbacks.put(EV_Hide, (eventCallback_t0<idEntity>) idEntity::Event_Hide);
            eventCallbacks.put(EV_Show, (eventCallback_t0<idEntity>) idEntity::Event_Show);
            eventCallbacks.put(EV_CacheSoundShader, (eventCallback_t1<idEntity>) idEntity::Event_CacheSoundShader);
            eventCallbacks.put(EV_StartSoundShader, (eventCallback_t2<idEntity>) idEntity::Event_StartSoundShader);
            eventCallbacks.put(EV_StartSound, (eventCallback_t3<idEntity>) idEntity::Event_StartSound);
            eventCallbacks.put(EV_StopSound, (eventCallback_t2<idEntity>) idEntity::Event_StopSound);
            eventCallbacks.put(EV_FadeSound, (eventCallback_t3<idEntity>) idEntity::Event_FadeSound);
            eventCallbacks.put(EV_GetWorldOrigin, (eventCallback_t0<idEntity>) idEntity::Event_GetWorldOrigin);
            eventCallbacks.put(EV_SetWorldOrigin, (eventCallback_t1<idEntity>) idEntity::Event_SetWorldOrigin);
            eventCallbacks.put(EV_GetOrigin, (eventCallback_t0<idEntity>) idEntity::Event_GetOrigin);
            eventCallbacks.put(EV_SetOrigin, (eventCallback_t1<idEntity>) idEntity::Event_SetOrigin);
            eventCallbacks.put(EV_GetAngles, (eventCallback_t0<idEntity>) idEntity::Event_GetAngles);
            eventCallbacks.put(EV_SetAngles, (eventCallback_t1<idEntity>) idEntity::Event_SetAngles);
            eventCallbacks.put(EV_GetLinearVelocity, (eventCallback_t0<idEntity>) idEntity::Event_GetLinearVelocity);
            eventCallbacks.put(EV_SetLinearVelocity, (eventCallback_t1<idEntity>) idEntity::Event_SetLinearVelocity);
            eventCallbacks.put(EV_GetAngularVelocity, (eventCallback_t0<idEntity>) idEntity::Event_GetAngularVelocity);
            eventCallbacks.put(EV_SetAngularVelocity, (eventCallback_t1<idEntity>) idEntity::Event_SetAngularVelocity);
            eventCallbacks.put(EV_GetSize, (eventCallback_t0<idEntity>) idEntity::Event_GetSize);
            eventCallbacks.put(EV_SetSize, (eventCallback_t2<idEntity>) idEntity::Event_SetSize);
            eventCallbacks.put(EV_GetMins, (eventCallback_t0<idEntity>) idEntity::Event_GetMins);
            eventCallbacks.put(EV_GetMaxs, (eventCallback_t0<idEntity>) idEntity::Event_GetMaxs);
            eventCallbacks.put(EV_Touches, (eventCallback_t1<idEntity>) idEntity::Event_Touches);
            eventCallbacks.put(EV_SetGuiParm, (eventCallback_t2<idEntity>) idEntity::Event_SetGuiParm);
            eventCallbacks.put(EV_SetGuiFloat, (eventCallback_t2<idEntity>) idEntity::Event_SetGuiFloat);
            eventCallbacks.put(EV_GetNextKey, (eventCallback_t2<idEntity>) idEntity::Event_GetNextKey);
            eventCallbacks.put(EV_SetKey, (eventCallback_t2<idEntity>) idEntity::Event_SetKey);
            eventCallbacks.put(EV_GetKey, (eventCallback_t1<idEntity>) idEntity::Event_GetKey);
            eventCallbacks.put(EV_GetIntKey, (eventCallback_t1<idEntity>) idEntity::Event_GetIntKey);
            eventCallbacks.put(EV_GetFloatKey, (eventCallback_t1<idEntity>) idEntity::Event_GetFloatKey);
            eventCallbacks.put(EV_GetVectorKey, (eventCallback_t1<idEntity>) idEntity::Event_GetVectorKey);
            eventCallbacks.put(EV_GetEntityKey, (eventCallback_t1<idEntity>) idEntity::Event_GetEntityKey);
            eventCallbacks.put(EV_RestorePosition, (eventCallback_t0<idEntity>) idEntity::Event_RestorePosition);
            eventCallbacks.put(EV_UpdateCameraTarget, (eventCallback_t0<idEntity>) idEntity::Event_UpdateCameraTarget);
            eventCallbacks.put(EV_DistanceTo, (eventCallback_t1<idEntity>) idEntity::Event_DistanceTo);
            eventCallbacks.put(EV_DistanceToPoint, (eventCallback_t1<idEntity>) idEntity::Event_DistanceToPoint);
            eventCallbacks.put(EV_StartFx, (eventCallback_t1<idEntity>) idEntity::Event_StartFx);
            eventCallbacks.put(EV_Thread_WaitFrame, (eventCallback_t0<idEntity>) idEntity::Event_WaitFrame);
            eventCallbacks.put(EV_Thread_Wait, (eventCallback_t1<idEntity>) idEntity::Event_Wait);
            eventCallbacks.put(EV_HasFunction, (eventCallback_t1<idEntity>) idEntity::Event_HasFunction);
            eventCallbacks.put(EV_CallFunction, (eventCallback_t1<idEntity>) idEntity::Event_CallFunction);
            eventCallbacks.put(EV_SetNeverDormant, (eventCallback_t1<idEntity>) idEntity::Event_SetNeverDormant);
        }

        public static final int MAX_PVS_AREAS = 4;
        //
        public       int                           entityNumber;      // index into the entity list
        public       int                           entityDefNumber;   // index into the entity def list
        //
        public       idLinkList<idEntity>          spawnNode;         // for being linked into spawnedEntities list
        public       idLinkList<idEntity>          activeNode;        // for being linked into activeEntities list
        //
        public       idLinkList<idEntity>          snapshotNode;      // for being linked into snapshotEntities list
        public       int                           snapshotSequence;  // last snapshot this entity was in
        public       int                           snapshotBits;      // number of bits this entity occupied in the last snapshot
        //
        public       idStr                         name;              // name of entity
        public       idDict                        spawnArgs;         // key/value pairs used to spawn and initialize entity
        public       idScriptObject                scriptObject;      // contains all script defined data for this entity
        //
        public       int                           thinkFlags;        // TH_? flags
        public       int                           dormantStart;      // time that the entity was first closed off from player
        public       boolean                       cinematic;         // during cinematics, entity will only think if cinematic is set
        //
        public       renderView_s                  renderView;        // for camera views from this entity
        public       idEntity                      cameraTarget;      // any remoteRenderMap shaders will use this
        //
        public final idList<idEntityPtr<idEntity>> targets;           // when this entity is activated these entities entity are activated
        //
        public       int                           health;            // FIXME: do all objects really need health?
//

        @Override
        public Class.idClass CreateInstance() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override//TODO:final this
        public java.lang.Class/*idTypeInfo*/ GetType() {//TODO: make method final
            return getClass();
        }

        @Override
        public eventCallback_t getEventCallBack(idEventDef event) {
            return eventCallbacks.get(event);
        }

        public static Map<idEventDef, eventCallback_t> getEventCallBacks() {
            return eventCallbacks;
        }


        @Override
        public idEntity oSet(idEntity node) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean isNULL() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public ByteBuffer AllocBuffer() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void Read(ByteBuffer buffer) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public ByteBuffer Write() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void oSet(Class.idClass oGet) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public static class entityFlags_s implements TempDump.SERiAL {

            /**
			 * 
			 */
			private static final long serialVersionUID = 1L;
			public boolean notarget;            // if true never attack or target this entity
            public boolean noknockback;         // if true no knockback from hits
            public boolean takedamage;          // if true this entity can be damaged
            public boolean hidden;              // if true this entity is not visible
            public boolean bindOrientated;      // if true both the master orientation is used for binding
            public boolean solidForTeam;        // if true this entity is considered solid when a physics team mate pushes entities
            public boolean forcePhysicsUpdate;  // if true always update from the physics whether the object moved or not
            public boolean selected;            // if true the entity is selected for editing
            public boolean neverDormant;        // if true the entity never goes dormant
            public boolean isDormant;           // if true the entity is dormant
            public boolean hasAwakened;         // before a monster has been awakened the first time, use full PVS for dormant instead of area-connected
            public boolean networkSync;         // if true the entity is synchronized over the network

            @Override
            public ByteBuffer AllocBuffer() {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            @Override
            public void Read(ByteBuffer buffer) {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            @Override
            public ByteBuffer Write() {
                throw new UnsupportedOperationException("Not supported yet.");
            }
        }

        public    entityFlags_s        fl;
        //
        //
        protected renderEntity_s       renderEntity;                    // used to present a model to the renderer
        protected int                  modelDefHandle;                  // handle to static renderer model
        protected refSound_t           refSound;                        // used to present sound to the audio engine
        //
        private final   idPhysics_Static     defaultPhysicsObj;               // default physics object
        private   idPhysics            physics;                         // physics used for this entity
        private   idEntity             bindMaster;                      // entity bound to if unequal NULL
        private   int/*jointHandle_t*/ bindJoint;                       // joint bound to if unequal INVALID_JOINT
        private   int                  bindBody;                        // body bound to if unequal -1
        private   idEntity             teamMaster;                      // master of the physics team
        private   idEntity             teamChain;                       // next entity in physics team
        //
        private   int                  numPVSAreas;                     // number of renderer areas the entity covers
        private final int[] PVSAreas = new int[MAX_PVS_AREAS];          // numbers of the renderer areas the entity covers
        //
        private signalList_t signals;
        //
        private int          mpGUIState;                                // local cache to avoid systematic SetStateInt
        //
        //
        //
        private static int DBG_counter = 0;
        private final  int DBG_count = DBG_counter++;
//

        //        public static final idTypeInfo Type;
//
//        public static idClass CreateInstance();
//
//        public abstract idTypeInfo GetType();
//        public static idEventFunc<idEntity>[] eventCallbacks;
//
		public idEntity() {
			{
		        @SuppressWarnings("unchecked")
				final
		        idList<idEntityPtr<idEntity>> targets = new idList<idEntityPtr<idEntity>>((java.lang.Class<idEntityPtr<idEntity>>) new idEntityPtr<idEntity>().getClass());
		        this.targets = targets;
			}

            this.entityNumber = ENTITYNUM_NONE;
            this.entityDefNumber = -1;

            this.spawnNode = new idLinkList<>();
            this.spawnNode.SetOwner(this);
            this.activeNode = new idLinkList<>();
            this.activeNode.SetOwner(this);

            this.snapshotNode = new idLinkList<>();
            this.snapshotNode.SetOwner(this);
            this.snapshotSequence = -1;
            this.snapshotBits = 0;

            this.name = new idStr();
            this.spawnArgs = new idDict();
            this.scriptObject = new idScriptObject();

            this.thinkFlags = 0;
            this.dormantStart = 0;
            this.cinematic = false;
            this.renderView = null;
            this.cameraTarget = null;
            this.health = 0;

            this.physics = null;
            this.bindMaster = null;
            this.bindJoint = INVALID_JOINT;
            this.bindBody = -1;
            this.teamMaster = null;
            this.teamChain = null;
            this.signals = null;

//            memset(PVSAreas, 0, sizeof(PVSAreas));
            this.numPVSAreas = -1;

            this.fl = new entityFlags_s();//	memset( &fl, 0, sizeof( fl ) );
            this.fl.neverDormant = true;// most entities never go dormant

            this.renderEntity = new renderEntity_s();//memset( &renderEntity, 0, sizeof( renderEntity ) );
            this.modelDefHandle = -1;
            this.refSound = new refSound_t();//memset( &refSound, 0, sizeof( refSound ) );
            this.defaultPhysicsObj = new idPhysics_Static();

            this.mpGUIState = -1;

        }

        @Override
        protected void _deconstructor() {
            if ((gameLocal.GameState() != GAMESTATE_SHUTDOWN) && !gameLocal.isClient && this.fl.networkSync && (this.entityNumber >= MAX_CLIENTS)) {
                final idBitMsg msg = new idBitMsg();
                final byte[] msgBuf = new byte[MAX_GAME_MESSAGE_SIZE];

                msg.Init(msgBuf);
                msg.WriteByte(GAME_RELIABLE_MESSAGE_DELETE_ENT);
                msg.WriteBits(gameLocal.GetSpawnId(this), 32);
                networkSystem.ServerSendReliableMessage(-1, msg);
            }

            DeconstructScriptObject();
            this.scriptObject.Free();

            if (this.thinkFlags != 0) {
                BecomeInactive(this.thinkFlags);
            }
            this.activeNode.Remove();

            Signal(SIG_REMOVED);

            // we have to set back the default physics object before unbinding because the entity
            // specific physics object might be an entity variable and as such could already be destroyed.
            SetPhysics(null);

            // remove any entities that are bound to me
            RemoveBinds();

            // unbind from master
            Unbind();
            QuitTeam();

            gameLocal.RemoveEntityFromHash(this.name.getData(), this);

//            delete renderView;
            this.renderView = null;

//            delete signals;
            this.signals = null;

            FreeModelDef();
            FreeSoundEmitter(false);

            gameLocal.UnregisterEntity(this);

            delete(this.teamChain);
            delete(this.teamMaster);
            delete(this.bindMaster);
            delete(this.physics);
            if (this.physics != this.defaultPhysicsObj) {
				delete(this.defaultPhysicsObj);
			}
            delete(this.cameraTarget);
            super._deconstructor();
        }

        @Override
        public void Spawn() {
            int i;
            final String[] temp = {null};
            idVec3 origin;
            idMat3 axis;
            idKeyValue networkSync;
            final String[] classname = {null};
            final String[] scriptObjectName = {null};

            gameLocal.RegisterEntity(this);

            this.spawnArgs.GetString("classname", null, classname);
            final idDeclEntityDef def = gameLocal.FindEntityDef(classname[0], false);
            if (def != null) {
                this.entityDefNumber = def.Index();
            }

            FixupLocalizedStrings();

            // parse static models the same way the editor display does
            GameEdit.gameEdit.ParseSpawnArgsToRenderEntity(this.spawnArgs, this.renderEntity);

            this.renderEntity.entityNum = this.entityNumber;

            // go dormant within 5 frames so that when the map starts most monsters are dormant
            this.dormantStart = (gameLocal.time - DELAY_DORMANT_TIME) + (idGameLocal.msec * 5);

            origin = new idVec3(this.renderEntity.origin);
            axis = new idMat3(this.renderEntity.axis);

            // do the audio parsing the same way dmap and the editor do
            GameEdit.gameEdit.ParseSpawnArgsToRefSound(this.spawnArgs, this.refSound);

            // only play SCHANNEL_PRIVATE when sndworld.PlaceListener() is called with this listenerId
            // don't spatialize sounds from the same entity
            this.refSound.listenerId = this.entityNumber + 1;

            this.cameraTarget = null;
            temp[0] = this.spawnArgs.GetString("cameraTarget");
            if ((temp[0] != null) && !temp[0].isEmpty()) {
                // update the camera taget
                PostEventMS(EV_UpdateCameraTarget, 0);
            }

            for (i = 0; i < MAX_RENDERENTITY_GUI; i++) {
                UpdateGuiParms(this.renderEntity.gui[i], this.spawnArgs);
            }

            this.fl.solidForTeam = this.spawnArgs.GetBool("solidForTeam", "0");
            this.fl.neverDormant = this.spawnArgs.GetBool("neverDormant", "0");
            this.fl.hidden = this.spawnArgs.GetBool("hide", "0");
            if (this.fl.hidden) {
                // make sure we're hidden, since a spawn function might not set it up right
                PostEventMS(EV_Hide, 0);
            }
            this.cinematic = this.spawnArgs.GetBool("cinematic", "0");

            networkSync = this.spawnArgs.FindKey("networkSync");
            if (networkSync != null) {
                this.fl.networkSync = (atoi(networkSync.GetValue()) != 0);
            }

            if (false) {
                if (!gameLocal.isClient) {
                    // common.DPrintf( "NET: DBG %s - %s is synced: %s\n", spawnArgs.GetString( "classname", "" ), GetType().classname, fl.networkSync ? "true" : "false" );
                    if ((this.spawnArgs.GetString("classname", "").charAt(0) == '\0') && !this.fl.networkSync) {
                        common.DPrintf("NET: WRN %s entity, no classname, and no networkSync?\n", GetType().getName());
                    }
                }
            }

            // every object will have a unique name
            temp[0] = this.spawnArgs.GetString("name", va("%s_%s_%d", GetClassname(), this.spawnArgs.GetString("classname"), this.entityNumber));
            SetName(temp[0]);

            // if we have targets, wait until all entities are spawned to get them
            if ((this.spawnArgs.MatchPrefix("target") != null) || (this.spawnArgs.MatchPrefix("guiTarget") != null)) {
                if (gameLocal.GameState() == GAMESTATE_STARTUP) {
                    PostEventMS(EV_FindTargets, 0);
                } else {
                    // not during spawn, so it's ok to get the targets
                    FindTargets();
                }
            }

            this.health = this.spawnArgs.GetInt("health");

            InitDefaultPhysics(origin, axis);

            SetOrigin(origin);
            SetAxis(axis);

            temp[0] = this.spawnArgs.GetString("model");
            if ((temp[0] != null) && !temp[0].isEmpty()) {
                SetModel(temp[0]);
            }

            if (this.spawnArgs.GetString("bind", "", temp)) {
                PostEventMS(EV_SpawnBind, 0);
            }

            // auto-start a sound on the entity
            if ((this.refSound.shader != null) && !this.refSound.waitfortrigger) {
                StartSoundShader(this.refSound.shader, SND_CHANNEL_ANY, 0, false, null);
            }

            // setup script object
            if (ShouldConstructScriptObjectAtSpawn() && this.spawnArgs.GetString("scriptobject", null, scriptObjectName)) {
                if (!this.scriptObject.SetType(scriptObjectName[0])) {
                    idGameLocal.Error("Script object '%s' not found on entity '%s'.", scriptObjectName[0], this.name);
                }

                ConstructScriptObject();
            }
        }

        @Override
        public void Save(idSaveGame savefile) {
            int i, j;

            savefile.WriteInt(this.entityNumber);
            savefile.WriteInt(this.entityDefNumber);

            // spawnNode and activeNode are restored by gameLocal
            savefile.WriteInt(this.snapshotSequence);
            savefile.WriteInt(this.snapshotBits);

            savefile.WriteDict(this.spawnArgs);
            savefile.WriteString(this.name);
            this.scriptObject.Save(savefile);

            savefile.WriteInt(this.thinkFlags);
            savefile.WriteInt(this.dormantStart);
            savefile.WriteBool(this.cinematic);

            savefile.WriteObject(this.cameraTarget);

            savefile.WriteInt(this.health);

            savefile.WriteInt(this.targets.Num());
            for (i = 0; i < this.targets.Num(); i++) {
                this.targets.oGet(i).Save(savefile);
            }

            final entityFlags_s flags = this.fl;
            LittleBitField(flags/*, sizeof(flags)*/);
            savefile.Write(flags/*, sizeof(flags)*/);

            savefile.WriteRenderEntity(this.renderEntity);
            savefile.WriteInt(this.modelDefHandle);
            savefile.WriteRefSound(this.refSound);

            savefile.WriteObject(this.bindMaster);
            savefile.WriteJoint(this.bindJoint);
            savefile.WriteInt(this.bindBody);
            savefile.WriteObject(this.teamMaster);
            savefile.WriteObject(this.teamChain);

            savefile.WriteStaticObject(this.defaultPhysicsObj);

            savefile.WriteInt(this.numPVSAreas);
            for (i = 0; i < MAX_PVS_AREAS; i++) {
                savefile.WriteInt(this.PVSAreas[i]);
            }

            if (null == this.signals) {
                savefile.WriteBool(false);
            } else {
                savefile.WriteBool(true);
                for (i = 0; i < NUM_SIGNALS.ordinal(); i++) {
                    savefile.WriteInt(this.signals.signal[i].Num());
                    for (j = 0; j < this.signals.signal[i].Num(); j++) {
                        savefile.WriteInt(this.signals.signal[i].oGet(j).threadnum);
                        savefile.WriteString(this.signals.signal[i].oGet(j).function.Name());
                    }
                }
            }

            savefile.WriteInt(this.mpGUIState);
        }

        @Override
        public void Restore(idRestoreGame savefile) {
            int i, j;
            final int[] num = {0};
            final idStr funcname = new idStr();

            this.entityNumber = savefile.ReadInt();
            this.entityDefNumber = savefile.ReadInt();

            // spawnNode and activeNode are restored by gameLocal
            this.snapshotSequence = savefile.ReadInt();
            this.snapshotBits = savefile.ReadInt();

            savefile.ReadDict(this.spawnArgs);
            savefile.ReadString(this.name);
            SetName(this.name);

            this.scriptObject.Restore(savefile);

            this.thinkFlags = savefile.ReadInt();
            this.dormantStart = savefile.ReadInt();
            this.cinematic = savefile.ReadBool();

            savefile.ReadObject(/*reinterpret_cast<idClass*&>*/(this.cameraTarget));

            this.health = savefile.ReadInt();

            this.targets.Clear();
            savefile.ReadInt(num);
            this.targets.SetNum(num[0]);
            for (i = 0; i < num[0]; i++) {
                this.targets.oGet(i).Restore(savefile);
            }

            savefile.Read(this.fl);
            LittleBitField(this.fl);

            savefile.ReadRenderEntity(this.renderEntity);
            this.modelDefHandle = savefile.ReadInt();
            savefile.ReadRefSound(this.refSound);

            savefile.ReadObject(/*reinterpret_cast<idClass*&>*/(this.bindMaster));
            this.bindJoint = savefile.ReadJoint();
            this.bindBody = savefile.ReadInt();
            savefile.ReadObject(/*reinterpret_cast<idClass*&>*/(this.teamMaster));
            savefile.ReadObject(/*reinterpret_cast<idClass*&>*/(this.teamChain));

            savefile.ReadStaticObject(this.defaultPhysicsObj);
            RestorePhysics(this.defaultPhysicsObj);

            this.numPVSAreas = savefile.ReadInt();
            for (i = 0; i < MAX_PVS_AREAS; i++) {
                this.PVSAreas[i] = savefile.ReadInt();
            }

            final boolean[] readsignals = new boolean[1];
            savefile.ReadBool(readsignals);
            if (readsignals[0]) {
                this.signals = new signalList_t();
                for (i = 0; i < NUM_SIGNALS.ordinal(); i++) {
                    savefile.ReadInt(num);
                    this.signals.signal[i].SetNum(num[0]);
                    for (j = 0; j < num[0]; j++) {
                        this.signals.signal[i].oGet(j).threadnum = savefile.ReadInt();
                        savefile.ReadString(funcname);
                        this.signals.signal[i].oGet(j).function = gameLocal.program.FindFunction(funcname);
                        if (null == this.signals.signal[i].oGet(j).function) {
                            savefile.Error("Function '%s' not found", funcname.getData());
                        }
                    }
                }
            }

            this.mpGUIState = savefile.ReadInt();

            // restore must retrieve modelDefHandle from the renderer
            if (this.modelDefHandle != -1) {
                this.modelDefHandle = gameRenderWorld.AddEntityDef(this.renderEntity);
            }
        }

        public String GetEntityDefName() {
            if (this.entityDefNumber < 0) {
                return "*unknown*";
            }
            return declManager.DeclByIndex(DECL_ENTITYDEF, this.entityDefNumber, false).GetName();
        }

        public void SetName(final String newname) {
            if (this.name.Length() != 0) {
                gameLocal.RemoveEntityFromHash(this.name.getData(), this);
                gameLocal.program.SetEntity(this.name.getData(), null);
            }

            this.name.oSet(newname);
            if (this.name.Length() != 0) {
//            if ( ( name == "NULL" ) || ( name == "null_entity" ) ) {
                if (("NULL".equals(newname)) || ("null_entity".equals(newname))) {
                    idGameLocal.Error("Cannot name entity '%s'.  '%s' is reserved for script.", this.name, this.name);
                }
                gameLocal.AddEntityToHash(this.name.getData(), this);
                gameLocal.program.SetEntity(this.name.getData(), this);
            }
        }

        public void SetName(final idStr newname) {
            SetName(newname.getData());
        }

        public String GetName() {
            return this.name.getData();
        }

        /*
         ===============
         idEntity::UpdateChangeableSpawnArgs

         Any key val pair that might change during the course of the game ( via a gui or whatever )
         should be initialize here so a gui or other trigger can change something and have it updated
         properly. An optional source may be provided if the values reside in an outside dictionary and
         first need copied over to spawnArgs
         ===============
         */
        public void UpdateChangeableSpawnArgs(idDict source) {
            int i;
            String target;

            if (null == source) {//TODO:null check
                source = this.spawnArgs;
            }
            this.cameraTarget = null;
            target = source.GetString("cameraTarget");
            if ((target != null) && !target.isEmpty()) {
                // update the camera taget
                PostEventMS(EV_UpdateCameraTarget, 0);
            }

            for (i = 0; i < MAX_RENDERENTITY_GUI; i++) {
                UpdateGuiParms(this.renderEntity.gui[i], source);
            }
        }

        /*
         =============
         idEntity::GetRenderView

         This is used by remote camera views to look from an entity
         =============
         */
        // clients generate views based on all the player specific options,
        // cameras have custom code, and everything else just uses the axis orientation
        public renderView_s GetRenderView() {
            if (null == this.renderView) {
                this.renderView = new renderView_s();
            }
            //	memset( renderView, 0, sizeof( *renderView ) );

            this.renderView.vieworg = new idVec3(GetPhysics().GetOrigin());
            this.renderView.fov_x = 120;
            this.renderView.fov_y = 120;
            this.renderView.viewaxis = new idMat3(GetPhysics().GetAxis());

            // copy global shader parms
            Nio.arraycopy(gameLocal.globalShaderParms, 0, this.renderView.shaderParms, 0, MAX_GLOBAL_SHADER_PARMS);

            this.renderView.globalMaterial = gameLocal.GetGlobalMaterial();

            this.renderView.time = gameLocal.time;

            return this.renderView;
        }

        /* **********************************************************************

         Thinking

         ***********************************************************************/
        // thinking
        public void Think() {
            RunPhysics();
            Present();
        }

        /*
         ================
         idEntity::CheckDormant

         Monsters and other expensive entities that are completely closed
         off from the player can skip all of their work
         ================
         */
        public boolean CheckDormant() {    // dormant == on the active list, but out of PVS
            boolean dormant;

            dormant = DoDormantTests();
            if (dormant && !this.fl.isDormant) {
                this.fl.isDormant = true;
                DormantBegin();
            } else if (!dormant && this.fl.isDormant) {
                this.fl.isDormant = false;
                DormantEnd();
            }

            return dormant;
        }

        /*
         ================
         idEntity::DormantBegin

         called when entity becomes dormant
         ================
         */
        public void DormantBegin() {
        }

        /*
         ================
         idEntity::DormantEnd

         called when entity wakes from being dormant
         ================
         */
        public void DormantEnd() {
        }

        public boolean IsActive() {
            return this.activeNode.InList();
        }

        public void BecomeActive(int flags) {
            if ((flags & TH_PHYSICS) != 0) {
                // enable the team master if this entity is part of a physics team
                if ((this.teamMaster != null) && (this.teamMaster != this)) {
                    this.teamMaster.BecomeActive(TH_PHYSICS);
                } else if (0 == (this.thinkFlags & TH_PHYSICS)) {
                    // if this is a pusher
                    if (this.physics.IsType(idPhysics_Parametric.class) || this.physics.IsType(idPhysics_Actor.class)) {
                        gameLocal.sortPushers = true;
                    }
                }
            }

            final int oldFlags = this.thinkFlags;
            this.thinkFlags |= flags;
            if (this.thinkFlags != 0) {
                if (!IsActive()) {
                    this.activeNode.AddToEnd(gameLocal.activeEntities);
                } else if (0 == oldFlags) {
                    // we became inactive this frame, so we have to decrease the count of entities to deactivate
                    gameLocal.numEntitiesToDeactivate--;
                }
            }
        }

        public void BecomeInactive(int flags) {
            if ((flags & TH_PHYSICS) != 0) {
                // may only disable physics on a team master if no team members are running physics or bound to a joints
                if (this.teamMaster == this) {
                    for (idEntity ent = this.teamMaster.teamChain; ent != null; ent = ent.teamChain) {
                        if (((ent.thinkFlags & TH_PHYSICS) != 0) || ((ent.bindMaster == this) && (ent.bindJoint != INVALID_JOINT))) {
                            flags &= ~TH_PHYSICS;
                            break;
                        }
                    }
                }
            }

            if (this.thinkFlags != 0) {
                this.thinkFlags &= ~flags;
                if ((0 == this.thinkFlags) && IsActive()) {
                    gameLocal.numEntitiesToDeactivate++;
                }
            }

            if ((flags & TH_PHYSICS) != 0) {
                // if this entity has a team master
                if ((this.teamMaster != null) && !this.teamMaster.equals(this)) {
                    // if the team master is at rest
                    if (this.teamMaster.IsAtRest()) {
                        this.teamMaster.BecomeInactive(TH_PHYSICS);
                    }
                }
            }
        }

        public void UpdatePVSAreas(final idVec3 pos) {
            int i;

            this.numPVSAreas = gameLocal.pvs.GetPVSAreas(new idBounds(pos), this.PVSAreas, MAX_PVS_AREAS);
            i = this.numPVSAreas;
            while (i < MAX_PVS_AREAS) {
                this.PVSAreas[i++] = 0;
            }
        }

        /* **********************************************************************

         Visuals

         ***********************************************************************/
        // visuals
        /*
         ================
         idEntity::Present

         Present is called to allow entities to generate refEntities, lights, etc for the renderer.
         ================
         */
        public void Present() {

            if (!gameLocal.isNewFrame) {
                return;
            }

            // don't present to the renderer if the entity hasn't changed
            if (0 == (this.thinkFlags & TH_UPDATEVISUALS)) {
                return;
            }
            BecomeInactive(TH_UPDATEVISUALS);

            // camera target for remote render views
            if ((this.cameraTarget != null) && gameLocal.InPlayerPVS(this)) {
                this.renderEntity.remoteRenderView = this.cameraTarget.GetRenderView();
            }

            // if set to invisible, skip
            if ((null == this.renderEntity.hModel) || IsHidden()) {
                return;
            }

            // add to refresh list
            if (this.modelDefHandle == -1) {
                this.modelDefHandle = gameRenderWorld.AddEntityDef(this.renderEntity);
                final int a = 0;
            } else {
                gameRenderWorld.UpdateEntityDef(this.modelDefHandle, this.renderEntity);
            }
        }

        public renderEntity_s GetRenderEntity() {
            return this.renderEntity;
        }

        public int GetModelDefHandle() {
            return this.modelDefHandle;
        }

        public void SetModel(final String modelname) {
            assert (modelname != null);

            FreeModelDef();

            this.renderEntity.hModel = renderModelManager.FindModel(modelname);

            if (this.renderEntity.hModel != null) {
                this.renderEntity.hModel.Reset();
            }

            this.renderEntity.callback = null;
            this.renderEntity.numJoints = 0;
            this.renderEntity.joints = null;
            if (this.renderEntity.hModel != null) {
                this.renderEntity.bounds.oSet(this.renderEntity.hModel.Bounds(this.renderEntity));
            } else {
                this.renderEntity.bounds.Zero();
            }

            UpdateVisuals();
        }

        public void SetSkin(final idDeclSkin skin) {
            this.renderEntity.customSkin = skin;
            UpdateVisuals();
        }

        public idDeclSkin GetSkin() {
            return this.renderEntity.customSkin;
        }

        public void SetShaderParm(int parmnum, float value) {
            if ((parmnum < 0) || (parmnum >= MAX_ENTITY_SHADER_PARMS)) {
                gameLocal.Warning("shader parm index (%d) out of range", parmnum);
                return;
            }

            this.renderEntity.shaderParms[parmnum] = value;
            UpdateVisuals();
        }

        public void SetColor(float red, float green, float blue) {
            this.renderEntity.shaderParms[SHADERPARM_RED] = red;
            this.renderEntity.shaderParms[SHADERPARM_GREEN] = green;
            this.renderEntity.shaderParms[SHADERPARM_BLUE] = blue;
            UpdateVisuals();
        }

        public void SetColor(final idVec3 color) {
            SetColor(color.oGet(0), color.oGet(1), color.oGet(2));
//	UpdateVisuals();
        }

        public void GetColor(idVec3 out) {
            out.oSet(0, this.renderEntity.shaderParms[SHADERPARM_RED]);
            out.oSet(1, this.renderEntity.shaderParms[SHADERPARM_GREEN]);
            out.oSet(2, this.renderEntity.shaderParms[SHADERPARM_BLUE]);
        }

        public void SetColor(final idVec4 color) {
            this.renderEntity.shaderParms[SHADERPARM_RED] = color.oGet(0);
            this.renderEntity.shaderParms[SHADERPARM_GREEN] = color.oGet(1);
            this.renderEntity.shaderParms[SHADERPARM_BLUE] = color.oGet(2);
            this.renderEntity.shaderParms[SHADERPARM_ALPHA] = color.oGet(3);
            UpdateVisuals();
        }

        public void GetColor(idVec4 out) {
            out.oSet(0, this.renderEntity.shaderParms[SHADERPARM_RED]);
            out.oSet(1, this.renderEntity.shaderParms[SHADERPARM_GREEN]);
            out.oSet(2, this.renderEntity.shaderParms[SHADERPARM_BLUE]);
            out.oSet(3, this.renderEntity.shaderParms[SHADERPARM_ALPHA]);
        }

        public void FreeModelDef() {
            if (this.modelDefHandle != -1) {
                gameRenderWorld.FreeEntityDef(this.modelDefHandle);
                this.modelDefHandle = -1;
            }
        }

        public void FreeLightDef() {
        }

        public void Hide() {
            if (!IsHidden()) {
                this.fl.hidden = true;
                FreeModelDef();
                UpdateVisuals();
            }
        }

        public void Show() {
            if (IsHidden()) {
                this.fl.hidden = false;
                UpdateVisuals();
            }
        }

        public boolean IsHidden() {
            return this.fl.hidden;
        }

        public void UpdateVisuals() {
            UpdateModel();
            UpdateSound();
        }

        public void UpdateModel() {
            UpdateModelTransform();

            // check if the entity has an MD5 model
            final idAnimator animator = GetAnimator();
            if ((animator != null) && (animator.ModelHandle() != null)) {
                // set the callback to update the joints
                this.renderEntity.callback = idEntity.ModelCallback.getInstance();
            }

            // set to invalid number to force an update the next time the PVS areas are retrieved
            ClearPVSAreas();

            // ensure that we call Present this frame
            BecomeActive(TH_UPDATEVISUALS);
        }

        public void UpdateModelTransform() {
            final idVec3 origin = new idVec3();
            final idMat3 axis = new idMat3();

            if (GetPhysicsToVisualTransform(origin, axis)) {
                this.renderEntity.axis.oSet(axis.oMultiply(GetPhysics().GetAxis()));
                this.renderEntity.origin.oSet(GetPhysics().GetOrigin().oPlus(origin.oMultiply(this.renderEntity.axis)));
            } else {
                this.renderEntity.axis.oSet(GetPhysics().GetAxis());
                this.renderEntity.origin.oSet(GetPhysics().GetOrigin());
            }
        }

        public void ProjectOverlay(final idVec3 origin, final idVec3 dir, float size, final String material) {
            final float[] s = new float[1], c = new float[1];
            final idMat3 axis = new idMat3(), axistemp = new idMat3();
            final idVec3 localOrigin = new idVec3();
            final idVec3[] localAxis = new idVec3[2];
            final idPlane[] localPlane = new idPlane[2];

            // make sure the entity has a valid model handle
            if (this.modelDefHandle < 0) {
                return;
            }

            // only do this on dynamic md5 models
            if (this.renderEntity.hModel.IsDynamicModel() != DM_CACHED) {
                return;
            }

            idMath.SinCos16(gameLocal.random.RandomFloat() * idMath.TWO_PI, s, c);

            axis.oSet(2, dir.oNegative());
            axis.oGet(2).NormalVectors(axistemp.oGet(0), axistemp.oGet(1));
            axis.oSet(0, axistemp.oGet(0).oMultiply(c[0]).oPlus(axistemp.oGet(1).oMultiply(-s[0])));
            axis.oSet(1, axistemp.oGet(0).oMultiply(-s[0]).oPlus(axistemp.oGet(1).oMultiply(-c[0])));

            this.renderEntity.axis.ProjectVector(origin.oMinus(this.renderEntity.origin), localOrigin);
            this.renderEntity.axis.ProjectVector(axis.oGet(0), localAxis[0]);
            this.renderEntity.axis.ProjectVector(axis.oGet(1), localAxis[1]);

            size = 1.0f / size;
            localAxis[0].oMulSet(size);
            localAxis[1].oMulSet(size);

            localPlane[0].oSet(localAxis[0]);
            localPlane[0].oSet(3, -(localOrigin.oMultiply(localAxis[0])) + 0.5f);

            localPlane[1].oSet(localAxis[1]);
            localPlane[1].oSet(3, -(localOrigin.oMultiply(localAxis[1])) + 0.5f);

            final idMaterial mtr = declManager.FindMaterial(material);

            // project an overlay onto the model
            gameRenderWorld.ProjectOverlay(this.modelDefHandle, localPlane, mtr);

            // make sure non-animating models update their overlay
            UpdateVisuals();
        }

        public int GetNumPVSAreas() {
            if (this.numPVSAreas < 0) {
                UpdatePVSAreas();
            }
            return this.numPVSAreas;
        }

        public int[] GetPVSAreas() {
            if (this.numPVSAreas < 0) {
                UpdatePVSAreas();
            }
            return this.PVSAreas;
        }

        public void ClearPVSAreas() {
            this.numPVSAreas = -1;
        }

        /*
         ================
         idEntity::PhysicsTeamInPVS

         FIXME: for networking also return true if any of the entity shadows is in the PVS
         ================
         */
        public boolean PhysicsTeamInPVS(pvsHandle_t pvsHandle) {
            idEntity part;

            if (this.teamMaster != null) {
                for (part = this.teamMaster; part != null; part = part.teamChain) {
                    if (gameLocal.pvs.InCurrentPVS(pvsHandle, part.GetPVSAreas(), part.GetNumPVSAreas())) {
                        return true;
                    }
                }
            } else {
                return gameLocal.pvs.InCurrentPVS(pvsHandle, GetPVSAreas(), GetNumPVSAreas());
            }
            return false;
        }

        // animation
        public boolean UpdateAnimationControllers() {
            // any ragdoll and IK animation controllers should be updated here
            return false;
        }

        public boolean UpdateRenderEntity(renderEntity_s renderEntity, final renderView_s renderView) {
            if (gameLocal.inCinematic && gameLocal.skipCinematic) {
                return false;
            }

            final idAnimator animator = GetAnimator();
            if (animator != null) {
                return animator.CreateFrame(gameLocal.time, false);
            }

            return false;
        }

        /*
         ================
         idEntity::ModelCallback

         NOTE: may not change the game state whatsoever!
         ================
         */
        public static class ModelCallback extends deferredEntityCallback_t {

            /**
			 * 
			 */
			private static final long serialVersionUID = 1L;
			public static final deferredEntityCallback_t instance = new ModelCallback();

            private ModelCallback() {
            }

            public static deferredEntityCallback_t getInstance() {
                return instance;
            }

            @Override
            public boolean run(renderEntity_s e, renderView_s v) {
                idEntity ent;

                ent = gameLocal.entities[e.entityNum];
                if (null == ent) {
                    idGameLocal.Error("idEntity::ModelCallback: callback with NULL game entity");
                }

                return ent.UpdateRenderEntity(e, v);
            }

            @Override
            public ByteBuffer AllocBuffer() {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            @Override
            public void Read(ByteBuffer buffer) {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            @Override
            public ByteBuffer Write() {
                throw new UnsupportedOperationException("Not supported yet.");
            }
        }

        /*
         ================
         idEntity::GetAnimator

         Subclasses will be responsible for allocating animator.
         ================
         */
        public idAnimator GetAnimator() {    // returns animator object used by this entity
            return null;
        }

        /* **********************************************************************

         Sound

         ***********************************************************************/
        // sound
        /*
         ================
         idEntity::CanPlayChatterSounds

         Used for playing chatter sounds on monsters.
         ================
         */
        public boolean CanPlayChatterSounds() {
            return true;
        }

        public boolean StartSound(final String soundName, final int/*s_channelType*/ channel, int soundShaderFlags, boolean broadcast, int[] length) {
            idSoundShader shader;
            final idStr sound = new idStr();

            if (length != null) {
                length[0] = 0;
            }

            // we should ALWAYS be playing sounds from the def.
            // hardcoded sounds MUST be avoided at all times because they won't get precached.
            assert (idStr.Icmpn(soundName, "snd_", 4) == 0);

            if (!this.spawnArgs.GetString(soundName, "", sound)) {
                return false;
            }

            if (sound.IsEmpty()) {
//            if (sound.oGet(0) == '\0') {
                return false;
            }

            if (!gameLocal.isNewFrame) {
                // don't play the sound, but don't report an error
                return true;
            }

            shader = declManager.FindSound(sound);
            return StartSoundShader(shader, channel, soundShaderFlags, broadcast, length);
        }

        public boolean StartSound(final String soundName, final Enum channel, int soundShaderFlags, boolean broadcast, int[] length) {
            return StartSound(soundName, channel.ordinal(), soundShaderFlags, broadcast, length);
        }

        public boolean StartSoundShader(final idSoundShader shader, final int/*s_channelType*/ channel, int soundShaderFlags, boolean broadcast, int[] length) {
            float diversity;
            int len;

            if (length != null) {
                length[0] = 0;
            }

            if (NOT(shader)) {
                return false;
            }

            if (!gameLocal.isNewFrame) {
                return true;
            }

            if (gameLocal.isServer && broadcast) {
                final idBitMsg msg = new idBitMsg();
                final ByteBuffer msgBuf = ByteBuffer.allocate(MAX_EVENT_PARAM_SIZE);

                msg.Init(msgBuf, MAX_EVENT_PARAM_SIZE);
                msg.BeginWriting();
                msg.WriteLong(gameLocal.ServerRemapDecl(-1, DECL_SOUND, shader.Index()));
                msg.WriteByte(channel);
                ServerSendEvent(EVENT_STARTSOUNDSHADER, msg, false, -1);
            }

            // set a random value for diversity unless one was parsed from the entity
            if (this.refSound.diversity < 0.0f) {
                diversity = gameLocal.random.RandomFloat();
            } else {
                diversity = this.refSound.diversity;
            }

            // if we don't have a soundEmitter allocated yet, get one now
            if (NOT(this.refSound.referenceSound)) {
                this.refSound.referenceSound = gameSoundWorld.AllocSoundEmitter();
            }

            UpdateSound();

            len = this.refSound.referenceSound.StartSound(shader, channel, diversity, soundShaderFlags);
            if (length != null) {
                length[0] = len;
            }

            // set reference to the sound for shader synced effects
            this.renderEntity.referenceSound = this.refSound.referenceSound;

            return true;
        }

        public boolean StartSoundShader(final idSoundShader shader, final Enum channel, int soundShaderFlags, boolean broadcast, int[] length) {
            return StartSoundShader(shader, channel.ordinal(), soundShaderFlags, broadcast, length);
        }

        public void StopSound(final int/*s_channelType*/ channel, boolean broadcast) {    // pass SND_CHANNEL_ANY to stop all sounds
            if (!gameLocal.isNewFrame) {
                return;
            }

            if (gameLocal.isServer && broadcast) {
                final idBitMsg msg = new idBitMsg();
                final ByteBuffer msgBuf = ByteBuffer.allocate(MAX_EVENT_PARAM_SIZE);

                msg.Init(msgBuf, MAX_EVENT_PARAM_SIZE);
                msg.BeginWriting();
                msg.WriteByte(channel);
                ServerSendEvent(EVENT_STOPSOUNDSHADER, msg, false, -1);
            }

            if (this.refSound.referenceSound != null) {
                this.refSound.referenceSound.StopSound(channel);
            }
        }

        /*
         ================
         idEntity::SetSoundVolume

         Must be called before starting a new sound.
         ================
         */
        public void SetSoundVolume(float volume) {
            this.refSound.parms.volume = volume;
        }

        public void UpdateSound() {
            if (this.refSound.referenceSound != null) {
                final idVec3 origin = new idVec3();
                final idMat3 axis = new idMat3();

                if (GetPhysicsToSoundTransform(origin, axis)) {
                    this.refSound.origin = GetPhysics().GetOrigin().oPlus(origin.oMultiply(axis));
                } else {
                    this.refSound.origin = GetPhysics().GetOrigin();
                }

                this.refSound.referenceSound.UpdateEmitter(this.refSound.origin, this.refSound.listenerId, this.refSound.parms);
            }
        }

        public int GetListenerId() {
            return this.refSound.listenerId;
        }

        public idSoundEmitter GetSoundEmitter() {
            return this.refSound.referenceSound;
        }

        public void FreeSoundEmitter(boolean immediate) {
            if (this.refSound.referenceSound != null) {
                this.refSound.referenceSound.Free(immediate);
                this.refSound.referenceSound = null;
            }
        }

        /* **********************************************************************

         entity binding

         ***********************************************************************/
        // entity binding
        public void PreBind() {
        }

        public void PostBind() {
        }

        public void PreUnbind() {
        }

        public void PostUnbind() {
        }

        public void JoinTeam(idEntity teammember) {
            idEntity ent;
            idEntity master;
            idEntity prev;
            idEntity next;

            // if we're already on a team, quit it so we can join this one
            if ((this.teamMaster != null) && (this.teamMaster != this)) {
                QuitTeam();
            }

            assert (teammember != null);

            if (teammember == this) {
                this.teamMaster = this;
                return;
            }

            // check if our new team mate is already on a team
            master = teammember.teamMaster;
            if (null == master) {
                // he's not on a team, so he's the new teamMaster
                master = teammember;
                teammember.teamMaster = teammember;
                teammember.teamChain = this;

                // make anyone who's bound to me part of the new team
                for (ent = this.teamChain; ent != null; ent = ent.teamChain) {
                    ent.teamMaster = master;
                }
            } else {
                // skip past the chain members bound to the entity we're teaming up with
                prev = teammember;
                next = teammember.teamChain;
                if (this.bindMaster != null) {
                    // if we have a bindMaster, join after any entities bound to the entity
                    // we're joining
                    while ((next != null) && next.IsBoundTo(teammember)) {
                        prev = next;
                        next = next.teamChain;
                    }
                } else {
                    // if we're not bound to someone, then put us at the end of the team
                    while (next != null) {
                        prev = next;
                        next = next.teamChain;
                    }
                }

                // make anyone who's bound to me part of the new team and
                // also find the last member of my team
                for (ent = this; ent.teamChain != null; ent = ent.teamChain) {
                    ent.teamChain.teamMaster = master;
                }

                prev.teamChain = this;
                ent.teamChain = next;
            }

            this.teamMaster = master;

            // reorder the active entity list
            gameLocal.sortTeamMasters = true;
        }

        /*
         ================
         idEntity::Bind

         bind relative to the visual position of the master
         ================
         */
        public void Bind(idEntity master, boolean orientated) {

            if (!InitBind(master)) {
                return;
            }

            PreBind();

            this.bindJoint = INVALID_JOINT;
            this.bindBody = -1;
            this.bindMaster = master;
            this.fl.bindOrientated = orientated;

            FinishBind();

            PostBind();
        }


        /*
         ================
         idEntity::BindToJoint

         bind relative to a joint of the md5 model used by the master
         ================
         */
        public void BindToJoint(idEntity master, final String jointname, boolean orientated) {
            int/*jointHandle_t*/ jointnum;
            idAnimator masterAnimator;

            if (!InitBind(master)) {
                return;
            }

            masterAnimator = master.GetAnimator();
            if (NOT(masterAnimator)) {
                gameLocal.Warning("idEntity::BindToJoint: entity '%s' cannot support skeletal models.", master.GetName());
                return;
            }

            jointnum = masterAnimator.GetJointHandle(jointname);
            if (jointnum == INVALID_JOINT) {
                gameLocal.Warning("idEntity::BindToJoint: joint '%s' not found on entity '%s'.", jointname, master.GetName());
            }

            PreBind();

            this.bindJoint = jointnum;
            this.bindBody = -1;
            this.bindMaster = master;
            this.fl.bindOrientated = orientated;

            FinishBind();

            PostBind();
        }

        /*
         ================
         idEntity::BindToJoint

         bind relative to a joint of the md5 model used by the master
         ================
         */
        public void BindToJoint(idEntity master, int/*jointHandle_t*/ jointnum, boolean orientated) {

            if (!InitBind(master)) {
                return;
            }

            PreBind();

            this.bindJoint = jointnum;
            this.bindBody = -1;
            this.bindMaster = master;
            this.fl.bindOrientated = orientated;

            FinishBind();

            PostBind();
        }

        /*
         ================
         idEntity::BindToBody

         bind relative to a collision model used by the physics of the master
         ================
         */
        public void BindToBody(idEntity master, int bodyId, boolean orientated) {

            if (!InitBind(master)) {
                return;
            }

            if (bodyId < 0) {
                gameLocal.Warning("idEntity::BindToBody: body '%d' not found.", bodyId);
            }

            PreBind();

            this.bindJoint = INVALID_JOINT;
            this.bindBody = bodyId;
            this.bindMaster = master;
            this.fl.bindOrientated = orientated;

            FinishBind();

            PostBind();
        }

        public void Unbind() {
            idEntity prev;
            idEntity next;
            idEntity last;
            idEntity ent;

            // remove any bind constraints from an articulated figure
            if (IsType(idAFEntity_Base.class)) {
                ((idAFEntity_Base) this).RemoveBindConstraints();
            }

            if (null == this.bindMaster) {
                return;
            }

            if (null == this.teamMaster) {
                // Teammaster already has been freed
                this.bindMaster = null;
                return;
            }

            PreUnbind();

            if (this.physics != null) {
                this.physics.SetMaster(null, this.fl.bindOrientated);
            }

            // We're still part of a team, so that means I have to extricate myself
            // and any entities that are bound to me from the old team.
            // Find the node previous to me in the team
            prev = this.teamMaster;
            for (ent = this.teamMaster.teamChain; (ent != null) && (ent != this); ent = ent.teamChain) {
                prev = ent;
            }

            assert (ent == this); // If ent is not pointing to this, then something is very wrong.

            // Find the last node in my team that is bound to me.
            // Also find the first node not bound to me, if one exists.
            last = this;
            for (next = this.teamChain; next != null; next = next.teamChain) {
                if (!next.IsBoundTo(this)) {
                    break;
                }

                // Tell them I'm now the teamMaster
                next.teamMaster = this;
                last = next;
            }

            // disconnect the last member of our team from the old team
            last.teamChain = null;

            // connect up the previous member of the old team to the node that
            // follow the last node bound to me (if one exists).
            if (this.teamMaster != this) {
                prev.teamChain = next;
                if ((null == next) && (this.teamMaster == prev)) {
                    prev.teamMaster = null;
                }
            } else if (next != null) {
                // If we were the teamMaster, then the nodes that were not bound to me are now
                // a disconnected chain.  Make them into their own team.
                for (ent = next; ent.teamChain != null; ent = ent.teamChain) {
                    ent.teamMaster = next;
                }
                next.teamMaster = next;
            }

            // If we don't have anyone on our team, then clear the team variables.
            if (this.teamChain != null) {
                // make myself my own team
                this.teamMaster = this;
            } else {
                // no longer a team
                this.teamMaster = null;
            }

            this.bindJoint = INVALID_JOINT;
            this.bindBody = -1;
            this.bindMaster = null;

            PostUnbind();
        }

        public boolean IsBound() {
            if (this.bindMaster != null) {
                return true;
            }
            return false;
        }

        public boolean IsBoundTo(idEntity master) {
            idEntity ent;

            if (null == this.bindMaster) {
                return false;
            }

            for (ent = this.bindMaster; ent != null; ent = ent.bindMaster) {
                if (ent == master) {
                    return true;
                }
            }

            return false;
        }

        public idEntity GetBindMaster() {
            return this.bindMaster;
        }

        public int/*jointHandle_t*/ GetBindJoint() {
            return this.bindJoint;
        }

        public int GetBindBody() {
            return this.bindBody;
        }

        public idEntity GetTeamMaster() {
            return this.teamMaster;
        }

        public idEntity GetNextTeamEntity() {
            return this.teamChain;
        }

        public void ConvertLocalToWorldTransform(idVec3 offset, idMat3 axis) {
            UpdateModelTransform();

            offset.oSet(this.renderEntity.origin.oPlus(offset.oMultiply(this.renderEntity.axis)));
            axis.oMulSet(this.renderEntity.axis);
        }

        /*
         ================
         idEntity::GetLocalVector

         Takes a vector in worldspace and transforms it into the parent
         object's localspace.

         Note: Does not take origin into acount.  Use getLocalCoordinate to
         convert coordinates.
         ================
         */
        public idVec3 GetLocalVector(final idVec3 vec) {
            final idVec3 pos = new idVec3();

            if (null == this.bindMaster) {
                return vec;
            }

            final idVec3 masterOrigin = new idVec3();
            final idMat3 masterAxis = new idMat3();

            GetMasterPosition(masterOrigin, masterAxis);
            masterAxis.ProjectVector(vec, pos);

            return pos;
        }

        /*
         ================
         idEntity::GetLocalCoordinates

         Takes a vector in world coordinates and transforms it into the parent
         object's local coordinates.
         ================
         */
        public idVec3 GetLocalCoordinates(final idVec3 vec) {
            final idVec3 pos = new idVec3();

            if (null == this.bindMaster) {
                return vec;
            }

            final idVec3 masterOrigin = new idVec3();
            final idMat3 masterAxis = new idMat3();

            GetMasterPosition(masterOrigin, masterAxis);
            masterAxis.ProjectVector(vec.oMinus(masterOrigin), pos);

            return pos;
        }

        /*
         ================
         idEntity::GetWorldVector

         Takes a vector in the parent object's local coordinates and transforms
         it into world coordinates.

         Note: Does not take origin into acount.  Use getWorldCoordinate to
         convert coordinates.
         ================
         */
        public idVec3 GetWorldVector(final idVec3 vec) {
            final idVec3 pos = new idVec3();

            if (null == this.bindMaster) {
                return vec;
            }

            final idVec3 masterOrigin = new idVec3();
            final idMat3 masterAxis = new idMat3();

            GetMasterPosition(masterOrigin, masterAxis);
            masterAxis.UnprojectVector(vec, pos);

            return pos;
        }


        /*
         ================
         idEntity::GetWorldCoordinates

         Takes a vector in the parent object's local coordinates and transforms
         it into world coordinates.
         ================
         */
        public idVec3 GetWorldCoordinates(final idVec3 vec) {
            final idVec3 pos = new idVec3();

            if (null == this.bindMaster) {
                return vec;
            }

            final idVec3 masterOrigin = new idVec3();
            final idMat3 masterAxis = new idMat3();

            GetMasterPosition(masterOrigin, masterAxis);
            masterAxis.UnprojectVector(vec, pos);
            pos.oPluSet(masterOrigin);

            return pos;
        }

        public boolean GetMasterPosition(idVec3 masterOrigin, idMat3 masterAxis) {
            final idVec3 localOrigin = new idVec3();
            final idMat3 localAxis = new idMat3();
            idAnimator masterAnimator;

            if (this.bindMaster != null) {
                // if bound to a joint of an animated model
                if (this.bindJoint != INVALID_JOINT) {
                    masterAnimator = this.bindMaster.GetAnimator();
                    if (null == masterAnimator) {
                        masterOrigin.oSet(getVec3_origin());
                        masterAxis.oSet(getMat3_identity());
                        return false;
                    } else {
                        masterAnimator.GetJointTransform(this.bindJoint, gameLocal.time, masterOrigin, masterAxis);
                        masterAxis.oMulSet(this.bindMaster.renderEntity.axis);
                        masterOrigin.oSet(this.bindMaster.renderEntity.origin.oPlus(masterOrigin.oMultiply(this.bindMaster.renderEntity.axis)));
                    }
                } else if ((this.bindBody >= 0) && (this.bindMaster.GetPhysics() != null)) {
                    masterOrigin.oSet(this.bindMaster.GetPhysics().GetOrigin(this.bindBody));
                    masterAxis.oSet(this.bindMaster.GetPhysics().GetAxis(this.bindBody));
                } else {
                    masterOrigin.oSet(this.bindMaster.renderEntity.origin);
                    masterAxis.oSet(this.bindMaster.renderEntity.axis);
                }
                return true;
            } else {
                masterOrigin.oSet(getVec3_origin());
                masterAxis.oSet(getMat3_identity());
                return false;
            }
        }

        public void GetWorldVelocities(idVec3 linearVelocity, idVec3 angularVelocity) {

            linearVelocity.oSet(this.physics.GetLinearVelocity());
            angularVelocity.oSet(this.physics.GetAngularVelocity());

            if (this.bindMaster != null) {
                final idVec3 masterOrigin = new idVec3(), masterLinearVelocity = new idVec3(), masterAngularVelocity = new idVec3();
                final idMat3 masterAxis = new idMat3();

                // get position of master
                GetMasterPosition(masterOrigin, masterAxis);

                // get master velocities
                this.bindMaster.GetWorldVelocities(masterLinearVelocity, masterAngularVelocity);

                // linear velocity relative to master plus master linear and angular velocity
                linearVelocity.oSet(linearVelocity.oMultiply(masterAxis).oPlus(masterLinearVelocity.oPlus(masterAngularVelocity.Cross(GetPhysics().GetOrigin().oMinus(masterOrigin)))));
            }
        }

        /* **********************************************************************

         Physics.
	
         ***********************************************************************/
        // physics
        // set a new physics object to be used by this entity
        public void SetPhysics(idPhysics phys) {
            // clear any contacts the current physics object has
            if (this.physics != null) {
                this.physics.ClearContacts();
            }
            // set new physics object or set the default physics if NULL
            if (phys != null) {
                this.defaultPhysicsObj.SetClipModel(null, 1.0f);
                this.physics = phys;
                this.physics.Activate();
            } else {
                this.physics = this.defaultPhysicsObj;
            }
            this.physics.UpdateTime(gameLocal.time);
            this.physics.SetMaster(this.bindMaster, this.fl.bindOrientated);
        }

        // get the physics object used by this entity
        public idPhysics GetPhysics() {
            return this.physics;
        }

        // restore physics pointer for save games
        public void RestorePhysics(idPhysics phys) {
            assert (phys != null);
            // restore physics pointer
            this.physics = phys;
        }

        private static int DBG_RunPhysics = 0;
        // run the physics for this entity
        public boolean RunPhysics() {
            int i, reachedTime, startTime, endTime;
            idEntity part, blockedPart, blockingEntity = new idEntity();
            final trace_s results;
            boolean moved;

            // don't run physics if not enabled
            if (0 == (this.thinkFlags & TH_PHYSICS)) {
                // however do update any animation controllers
                if (UpdateAnimationControllers()) {
                    BecomeActive(TH_ANIMATE);
                }
                return false;
            }

            // if this entity is a team slave don't do anything because the team master will handle everything
            if ((this.teamMaster != null) && (this.teamMaster != this)) {
                return false;
            }

            startTime = gameLocal.previousTime;
            endTime = gameLocal.time;

            gameLocal.push.InitSavingPushedEntityPositions();
            blockedPart = null;

            // save the physics state of the whole team and disable the team for collision detection
            for (part = this; part != null; part = part.teamChain) {
                if (part.physics != null) {
                    if (!part.fl.solidForTeam) {
                        part.physics.DisableClip();
                    }
                    part.physics.SaveState();
                }
            }
                                                      DBG_name = this.name.getData();
            // move the whole team
            for (part = this; part != null; part = part.teamChain) {

                if (part.physics != null) {
                                             if(this.name.equals("marscity_civilian1_1_head")) {
												DBG_RunPhysics++;
											}
                    // run physics
                    moved = part.physics.Evaluate(endTime - startTime, endTime);

                    // check if the object is blocked
                    blockingEntity = part.physics.GetBlockingEntity();
                    if (blockingEntity != null) {
                        blockedPart = part;
                        break;
                    }

                    // if moved or forced to update the visual position and orientation from the physics
                    if (moved || part.fl.forcePhysicsUpdate) {
                        part.UpdateFromPhysics(false);
                    }

                    // update any animation controllers here so an entity bound
                    // to a joint of this entity gets the correct position
                    if (part.UpdateAnimationControllers()) {
                        part.BecomeActive(TH_ANIMATE);
                    }
                }
            }

            // enable the whole team for collision detection
            for (part = this; part != null; part = part.teamChain) {
                if (part.physics != null) {
                    if (!part.fl.solidForTeam) {
                        part.physics.EnableClip();
                    }
                }
            }

            // if one of the team entities is a pusher and blocked
            if (blockedPart != null) {
                // move the parts back to the previous position
                for (part = this; part != blockedPart; part = part.teamChain) {

                    if (part.physics != null) {

                        // restore the physics state
                        part.physics.RestoreState();

                        // move back the visual position and orientation
                        part.UpdateFromPhysics(true);
                    }
                }
                for (part = this; part != null; part = part.teamChain) {
                    if (part.physics != null) {
                        // update the physics time without moving
                        part.physics.UpdateTime(endTime);
                    }
                }

                // restore the positions of any pushed entities
                gameLocal.push.RestorePushedEntityPositions();

                if (gameLocal.isClient) {
                    return false;
                }

                // if the master pusher has a "blocked" function, call it
                Signal(SIG_BLOCKED);
                ProcessEvent(EV_TeamBlocked, blockedPart, blockingEntity);
                // call the blocked function on the blocked part
                blockedPart.ProcessEvent(EV_PartBlocked, blockingEntity);
                return false;
            }

            // set pushed
            for (i = 0; i < gameLocal.push.GetNumPushedEntities(); i++) {
                final idEntity ent = gameLocal.push.GetPushedEntity(i);
                ent.physics.SetPushed(endTime - startTime);
            }

            if (gameLocal.isClient) {
                return true;
            }

            // post reached event if the current time is at or past the end point of the motion
            for (part = this; part != null; part = part.teamChain) {

                if (part.physics != null) {

                    reachedTime = part.physics.GetLinearEndTime();
                    if ((startTime < reachedTime) && (endTime >= reachedTime)) {
                        part.ProcessEvent(EV_ReachedPos);
                    }
                    reachedTime = part.physics.GetAngularEndTime();
                    if ((startTime < reachedTime) && (endTime >= reachedTime)) {
                        part.ProcessEvent(EV_ReachedAng);
                    }
                }
            }

            return true;
        }
        public static String DBG_name = "";

        // set the origin of the physics object (relative to bindMaster if not NULL)
        public void SetOrigin(final idVec3 org) {

            GetPhysics().SetOrigin(org);

            UpdateVisuals();
        }

        // set the axis of the physics object (relative to bindMaster if not NULL)
        public void SetAxis(final idMat3 axis) {

            if (GetPhysics().IsType(idPhysics_Actor.class)) {
                ((idActor) this).viewAxis.oSet(axis);
            } else {
                GetPhysics().SetAxis(axis);
            }

            UpdateVisuals();
        }

        // use angles to set the axis of the physics object (relative to bindMaster if not NULL)
        public void SetAngles(final idAngles ang) {
            SetAxis(ang.ToMat3());
        }

        // get the floor position underneath the physics object
        public boolean GetFloorPos(float max_dist, idVec3 floorpos) {
            final trace_s[] result = {new trace_s()};

            if (!GetPhysics().HasGroundContacts()) {
                GetPhysics().ClipTranslation(result, GetPhysics().GetGravityNormal().oMultiply(max_dist), null);
                if (result[0].fraction < 1.0f) {
                    floorpos.oSet(result[0].endpos);
                    return true;
                } else {
                    floorpos.oSet(GetPhysics().GetOrigin());
                    return false;
                }
            } else {
                floorpos.oSet(GetPhysics().GetOrigin());
                return true;
            }
        }

        // retrieves the transformation going from the physics origin/axis to the visual origin/axis
        public boolean GetPhysicsToVisualTransform(idVec3 origin, idMat3 axis) {
            return false;
        }

        // retrieves the transformation going from the physics origin/axis to the sound origin/axis
        public boolean GetPhysicsToSoundTransform(idVec3 origin, idMat3 axis) {
            // by default play the sound at the center of the bounding box of the first clip model
            if (GetPhysics().GetNumClipModels() > 0) {
                origin.oSet(GetPhysics().GetBounds().GetCenter());
                axis.Identity();
                return true;
            }
            return false;
        }

        // called from the physics object when colliding, should return true if the physics simulation should stop
        public boolean Collide(final trace_s collision, final idVec3 velocity) {
            // this entity collides with collision.c.entityNum
            return false;
        }

        // retrieves impact information, 'ent' is the entity retrieving the info
        public impactInfo_s GetImpactInfo(idEntity ent, int id, final idVec3 point) {
            return GetPhysics().GetImpactInfo(id, point);
        }

        // apply an impulse to the physics object, 'ent' is the entity applying the impulse
        public void ApplyImpulse(idEntity ent, int id, final idVec3 point, final idVec3 impulse) {
            GetPhysics().ApplyImpulse(id, point, impulse);
        }

        // add a force to the physics object, 'ent' is the entity adding the force
        public void AddForce(idEntity ent, int id, final idVec3 point, final idVec3 force) {
            GetPhysics().AddForce(id, point, force);
        }

        // activate the physics object, 'ent' is the entity activating this entity
        public void ActivatePhysics(idEntity ent) {
            GetPhysics().Activate();
        }

        // returns true if the physics object is at rest
        public boolean IsAtRest() {
            return GetPhysics().IsAtRest();
        }

        // returns the time the physics object came to rest
        public int GetRestStartTime() {
            return GetPhysics().GetRestStartTime();
        }

        // add a contact entity
        public void AddContactEntity(idEntity ent) {
            GetPhysics().AddContactEntity(ent);
        }

        // remove a touching entity
        public void RemoveContactEntity(idEntity ent) {
            GetPhysics().RemoveContactEntity(ent);
        }

        /* **********************************************************************

         Damage
	
         ***********************************************************************/
        // damage
        /*
         ============
         idEntity::CanDamage

         Returns true if the inflictor can directly damage the target.  Used for
         explosions and melee attacks.
         ============
         */
        // returns true if this entity can be damaged from the given origin
        public boolean CanDamage(final idVec3 origin, idVec3 damagePoint) {
            idVec3 dest;
            final trace_s[] tr = {null};
            idVec3 midpoint;

            // use the midpoint of the bounds instead of the origin, because
            // bmodels may have their origin at 0,0,0
            midpoint = GetPhysics().GetAbsBounds().oGet(0).oPlus(GetPhysics().GetAbsBounds().oGet(1)).oMultiply(0.5f);

            dest = midpoint;
            gameLocal.clip.TracePoint(tr, origin, dest, MASK_SOLID, null);
            if ((tr[0].fraction == 1.0) || (gameLocal.GetTraceEntity(tr[0]) == this)) {
                damagePoint.oSet(tr[0].endpos);
                return true;
            }

            // this should probably check in the plane of projection, rather than in world coordinate
            dest = midpoint;
            dest.oPluSet(0, 15.0f);
            dest.oPluSet(1, 15.0f);
            gameLocal.clip.TracePoint(tr, origin, dest, MASK_SOLID, null);
            if ((tr[0].fraction == 1.0) || (gameLocal.GetTraceEntity(tr[0]) == this)) {
                damagePoint.oSet(tr[0].endpos);
                return true;
            }

            dest = midpoint;
            dest.oPluSet(0, 15.0f);
            dest.oMinSet(1, 15.0f);
            gameLocal.clip.TracePoint(tr, origin, dest, MASK_SOLID, null);
            if ((tr[0].fraction == 1.0) || (gameLocal.GetTraceEntity(tr[0]) == this)) {
                damagePoint.oSet(tr[0].endpos);
                return true;
            }

            dest = midpoint;
            dest.oMinSet(0, 15.0f);
            dest.oPluSet(1, 15.0f);
            gameLocal.clip.TracePoint(tr, origin, dest, MASK_SOLID, null);
            if ((tr[0].fraction == 1.0) || (gameLocal.GetTraceEntity(tr[0]) == this)) {
                damagePoint.oSet(tr[0].endpos);
                return true;
            }

            dest = midpoint;
            dest.oMinSet(0, 15.0f);
            dest.oMinSet(1, 15.0f);
            gameLocal.clip.TracePoint(tr, origin, dest, MASK_SOLID, null);
            if ((tr[0].fraction == 1.0) || (gameLocal.GetTraceEntity(tr[0]) == this)) {
                damagePoint.oSet(tr[0].endpos);
                return true;
            }

            dest = midpoint;
            dest.oPluSet(2, 15.0f);
            gameLocal.clip.TracePoint(tr, origin, dest, MASK_SOLID, null);
            if ((tr[0].fraction == 1.0) || (gameLocal.GetTraceEntity(tr[0]) == this)) {
                damagePoint.oSet(tr[0].endpos);
                return true;
            }

            dest = midpoint;
            dest.oMinSet(2, 15.0f);
            gameLocal.clip.TracePoint(tr, origin, dest, MASK_SOLID, null);
            if ((tr[0].fraction == 1.0) || (gameLocal.GetTraceEntity(tr[0]) == this)) {
                damagePoint.oSet(tr[0].endpos);
                return true;
            }

            return false;
        }

        /*
         ============
         Damage

         this		entity that is being damaged
         inflictor	entity that is causing the damage
         attacker	entity that caused the inflictor to damage targ
         example: this=monster, inflictor=rocket, attacker=player

         dir			direction of the attack for knockback in global space
         point		point at which the damage is being inflicted, used for headshots
         damage		amount of damage being inflicted

         inflictor, attacker, dir, and point can be NULL for environmental effects

         ============
         */
        // applies damage to this entity
        public void Damage(idEntity inflictor, idEntity attacker, final idVec3 dir, final String damageDefName, final float damageScale, final int location) {
            if (!this.fl.takedamage) {
                return;
            }

            if (null == inflictor) {
                inflictor = gameLocal.world;
            }

            if (null == attacker) {
                attacker = gameLocal.world;
            }

            final idDict damageDef = gameLocal.FindEntityDefDict(damageDefName, false);
            if (null == damageDef) {
                idGameLocal.Error("Unknown damageDef '%s'\n", damageDefName);
            }

            final int[] damage = {damageDef.GetInt("damage")};

            // inform the attacker that they hit someone
            attacker.DamageFeedback(this, inflictor, damage);
            if (0 == damage[0]) {
                // do the damage
                this.health -= damage[0];
                if (this.health <= 0) {
                    if (this.health < -999) {
                        this.health = -999;
                    }

                    Killed(inflictor, attacker, damage[0], dir, location);
                } else {
                    Pain(inflictor, attacker, damage[0], dir, location);
                }
            }
        }

        // adds a damage effect like overlays, blood, sparks, debris etc.
        public void AddDamageEffect(final trace_s collision, final idVec3 velocity, final String damageDefName) {
            String sound, decal, key;

            final idDeclEntityDef def = gameLocal.FindEntityDef(damageDefName, false);
            if (def == null) {
                return;
            }

            final String materialType = gameLocal.sufaceTypeNames[collision.c.material.GetSurfaceType().ordinal()];

            // start impact sound based on material type
            key = va("snd_%s", materialType);
            sound = this.spawnArgs.GetString(key);
            if (sound.isEmpty()) {// == '\0' ) {
                sound = def.dict.GetString(key);
            }
            if (!sound.isEmpty()) {// != '\0' ) {
                StartSoundShader(declManager.FindSound(sound), SND_CHANNEL_BODY, 0, false, null);
            }

            if (g_decals.GetBool()) {
                // place a wound overlay on the model
                key = va("mtr_wound_%s", materialType);
                decal = this.spawnArgs.RandomPrefix(key, gameLocal.random);
                if (decal.isEmpty()) {// == '\0' ) {
                    decal = def.dict.RandomPrefix(key, gameLocal.random);
                }
                if (!decal.isEmpty()) {// != '\0' ) {
                    final idVec3 dir = velocity;
                    dir.Normalize();
                    ProjectOverlay(collision.c.point, dir, 20.0f, decal);
                }
            }
        }

        /*
         ================
         idEntity::DamageFeedback

         callback function for when another entity received damage from this entity.  damage can be adjusted and returned to the caller.
         ================
         */
        // callback function for when another entity received damage from this entity.  damage can be adjusted and returned to the caller.
        public void DamageFeedback(idEntity victim, idEntity inflictor, int[] damage) {
            // implemented in subclasses
        }

        /*
         ============
         idEntity::Pain

         Called whenever an entity recieves damage.  Returns whether the entity responds to the pain.
         This is a virtual function that subclasses are expected to implement.
         ============
         */
        // notifies this entity that it is in pain
        public boolean Pain(idEntity inflictor, idEntity attacker, int damage, final idVec3 dir, int location) {
            return false;
        }

        /*
         ============
         idEntity::Killed

         Called whenever an entity's health is reduced to 0 or less.
         This is a virtual function that subclasses are expected to implement.
         ============
         */
        // notifies this entity that is has been killed
        public void Killed(idEntity inflictor, idEntity attacker, int damage, final idVec3 dir, int location) {
        }

        /* **********************************************************************

         Script functions
	
         ***********************************************************************/
        // scripting
/*
         ================
         idEntity::ShouldConstructScriptObjectAtSpawn

         Called during idEntity::Spawn to see if it should construct the script object or not.
         Overridden by subclasses that need to spawn the script object themselves.
         ================
         */
        public boolean ShouldConstructScriptObjectAtSpawn() {
            return true;
        }

        /*
         ================
         idEntity::ConstructScriptObject

         Called during idEntity::Spawn.  Calls the constructor on the script object.
         Can be overridden by subclasses when a thread doesn't need to be allocated.
         ================
         */
        public idThread ConstructScriptObject() {
            idThread thread;
            function_t constructor;

            // init the script object's data
            this.scriptObject.ClearObject();

            // call script object's constructor
            constructor = this.scriptObject.GetConstructor();
            if (constructor != null) {
                // start a thread that will initialize after Spawn is done being called
                thread = new idThread();
                thread.SetThreadName(this.name.getData());
                thread.CallFunction(this, constructor, true);
                thread.DelayedStart(0);
            } else {
                thread = null;
            }

            // clear out the object's memory
            this.scriptObject.ClearObject();

            return thread;
        }

        /*
         ================
         idEntity::DeconstructScriptObject

         Called during idEntity::~idEntity.  Calls the destructor on the script object.
         Can be overridden by subclasses when a thread doesn't need to be allocated.
         Not called during idGameLocal::MapShutdown.
         ================
         */
        public void DeconstructScriptObject() {
            idThread thread;
            function_t destructor;

            // don't bother calling the script object's destructor on map shutdown
            if (gameLocal.GameState() == GAMESTATE_SHUTDOWN) {
                return;
            }

            // call script object's destructor
            destructor = this.scriptObject.GetDestructor();
            if (destructor != null) {
                // start a thread that will run immediately and be destroyed
                thread = new idThread();
                thread.SetThreadName(this.name.getData());
                thread.CallFunction(this, destructor, true);
                thread.Execute();
//		delete thread;
            }
        }

        public void SetSignal(signalNum_t _signalnum, idThread thread, final function_t function) {
            SetSignal(_signalnum.ordinal(), thread, function);
        }

        public void SetSignal(int _signalnum, idThread thread, final function_t function) {
            int i;
            int num;
            final signal_t sig = new signal_t();
            int threadnum;
            final int signalnum = _signalnum;

            assert ((signalnum >= 0) && (signalnum < NUM_SIGNALS.ordinal()));

            if (null == this.signals) {
                this.signals = new signalList_t();
            }

            assert (thread != null);
            threadnum = thread.GetThreadNum();

            num = this.signals.signal[signalnum].Num();
            for (i = 0; i < num; i++) {
                if (this.signals.signal[signalnum].oGet(i).threadnum == threadnum) {
                    this.signals.signal[signalnum].oGet(i).function = function;
                    return;
                }
            }

            if (num >= MAX_SIGNAL_THREADS) {
                thread.Error("Exceeded maximum number of signals per object");
            }

            sig.threadnum = threadnum;
            sig.function = function;
            this.signals.signal[signalnum].Append(sig);
        }

        public void ClearSignal(idThread thread, signalNum_t _signalnum) {
            final int signalnum = _signalnum.ordinal();
            assert (thread != null);
            if ((signalnum < 0) || (signalnum >= NUM_SIGNALS.ordinal())) {
                idGameLocal.Error("Signal out of range");
            }

            if (null == this.signals) {
                return;
            }

            this.signals.signal[signalnum].Clear();
        }

        public void ClearSignalThread(signalNum_t _signalnum, idThread thread) {
            ClearSignalThread(_signalnum.ordinal(), thread);
        }

        public void ClearSignalThread(int _signalnum, idThread thread) {
            int i;
            int num;
            int threadnum;
            final int signalnum = _signalnum;

            assert (thread != null);

            if ((signalnum < 0) || (signalnum >= NUM_SIGNALS.ordinal())) {
                idGameLocal.Error("Signal out of range");
            }

            if (null == this.signals) {
                return;
            }

            threadnum = thread.GetThreadNum();

            num = this.signals.signal[signalnum].Num();
            for (i = 0; i < num; i++) {
                if (this.signals.signal[signalnum].oGet(i).threadnum == threadnum) {
                    this.signals.signal[signalnum].RemoveIndex(i);
                    return;
                }
            }
        }

        public boolean HasSignal(signalNum_t _signalnum) {
            final int signalnum = _signalnum.ordinal();
            if (null == this.signals) {
                return false;
            }
            assert ((signalnum >= 0) && (signalnum < NUM_SIGNALS.ordinal()));
            return (this.signals.signal[signalnum].Num() > 0);
        }

        public void Signal(signalNum_t _signalnum) {
            int i;
            int num;
            final signal_t[] sigs = new signal_t[MAX_SIGNAL_THREADS];
            idThread thread;
            final int signalnum = _signalnum.ordinal();

            assert ((signalnum >= 0) && (signalnum < NUM_SIGNALS.ordinal()));

            if (null == this.signals) {
                return;
            }

            // we copy the signal list since each thread has the potential
            // to end any of the threads in the list.  By copying the list
            // we don't have to worry about the list changing as we're
            // processing it.
            num = this.signals.signal[signalnum].Num();
            for (i = 0; i < num; i++) {
                sigs[i] = this.signals.signal[signalnum].oGet(i);
            }

            // clear out the signal list so that we don't get into an infinite loop
            this.signals.signal[signalnum].Clear();

            for (i = 0; i < num; i++) {
                thread = idThread.GetThread(sigs[i].threadnum);
                if (thread != null) {
                    thread.CallFunction(this, sigs[i].function, true);
                    thread.Execute();
                }
            }
        }

        public void SignalEvent(idThread thread, signalNum_t _signalNum) {
            final int signalNum = etoi(_signalNum);
            if ((signalNum < 0) || (signalNum >= etoi(NUM_SIGNALS))) {
                idGameLocal.Error("Signal out of range");
            }

            if (null == this.signals) {
                return;
            }

            Signal(_signalNum);
        }

        /* **********************************************************************

         Guis.
	
         ***********************************************************************/
        // gui
        public void TriggerGuis() {
            int i;
            for (i = 0; i < MAX_RENDERENTITY_GUI; i++) {
                if (this.renderEntity.gui[i] != null) {
                    this.renderEntity.gui[i].Trigger(gameLocal.time);
                }
            }
        }

        public boolean HandleGuiCommands(idEntity entityGui, final String cmds) {
            idEntity targetEnt;
            boolean ret = false;
            if ((entityGui != null) && (cmds != null) && !cmds.isEmpty()) {
                final idLexer src = new idLexer();
                final idToken token = new idToken(), token2 = new idToken();
				idToken token3 = new idToken();
				final idToken token4 = new idToken();
                src.LoadMemory(cmds, cmds.length(), "guiCommands");
                while (true) {

                    if (!src.ReadToken(token)) {
                        return ret;
                    }

                    if (token.equals(";")) {
                        continue;
                    }

                    if (token.Icmp("activate") == 0) {
                        boolean targets = true;
                        if (src.ReadToken(token2)) {
                            if (token2.equals(";")) {
                                src.UnreadToken(token2);
                            } else {
                                targets = false;
                            }
                        }

                        if (targets) {
                            entityGui.ActivateTargets(this);
                        } else {
                            final idEntity ent = gameLocal.FindEntity(token2);
                            if (ent != null) {
                                ent.Signal(SIG_TRIGGER);
                                ent.PostEventMS(EV_Activate, 0, this);
                            }
                        }

                        entityGui.renderEntity.shaderParms[SHADERPARM_MODE] = 1.0f;
                        continue;
                    }

                    if (token.Icmp("runScript") == 0) {
                        if (src.ReadToken(token2)) {
                            while (src.CheckTokenString("::")) {
//						idToken token3;
                                token3 = new idToken();
                                if (!src.ReadToken(token3)) {
                                    idGameLocal.Error("Expecting function name following '::' in gui for entity '%s'", entityGui.name);
                                }
                                token2.Append("::" + token3.getData());
                            }
                            final function_t func = gameLocal.program.FindFunction(token2);
                            if (null == func) {
                                idGameLocal.Error("Can't find function '%s' for gui in entity '%s'", token2, entityGui.name);
                            } else {
                                final idThread thread = new idThread(func);
                                thread.DelayedStart(0);
                            }
                        }
                        continue;
                    }

                    if (token.Icmp("play") == 0) {
                        if (src.ReadToken(token2)) {
                            final idSoundShader shader = declManager.FindSound(token2);
                            entityGui.StartSoundShader(shader, SND_CHANNEL_ANY, 0, false, null);
                        }
                        continue;
                    }

                    if (token.Icmp("setkeyval") == 0) {
                        if (src.ReadToken(token2) && src.ReadToken(token3) && src.ReadToken(token4)) {
                            final idEntity ent = gameLocal.FindEntity(token2);
                            if (ent != null) {
                                ent.spawnArgs.Set(token3, token4);
                                ent.UpdateChangeableSpawnArgs(null);
                                ent.UpdateVisuals();
                            }
                        }
                        continue;
                    }

                    if (token.Icmp("setshaderparm") == 0) {
                        if (src.ReadToken(token2) && src.ReadToken(token3)) {
                            entityGui.SetShaderParm(Integer.parseInt(token2.getData()), Float.parseFloat(token3.getData()));
                            entityGui.UpdateVisuals();
                        }
                        continue;
                    }

                    if (token.Icmp("close") == 0) {
                        ret = true;
                        continue;
                    }

                    if (0 == token.Icmp("turkeyscore")) {
                        if (src.ReadToken(token2) && (entityGui.renderEntity.gui[0] != null)) {
                            int score = entityGui.renderEntity.gui[0].State().GetInt("score");
                            score += Integer.parseInt(token2.getData());
                            entityGui.renderEntity.gui[0].SetStateInt("score", score);
                            if ((gameLocal.GetLocalPlayer() != null) && (score >= 25000) && !gameLocal.GetLocalPlayer().inventory.turkeyScore) {
                                gameLocal.GetLocalPlayer().GiveEmail("highScore");
                                gameLocal.GetLocalPlayer().inventory.turkeyScore = true;
                            }
                        }
                        continue;
                    }

                    // handy for debugging GUI stuff
                    if (0 == token.Icmp("print")) {
                        String msg = "";
                        while (src.ReadToken(token2)) {
                            if (token2.equals(";")) {
                                src.UnreadToken(token2);
                                break;
                            }
                            msg += token2.getData();
                        }
                        common.Printf("ent gui 0x%x '%s': %s\n", this.entityNumber, this.name, msg);
                        continue;
                    }

                    // if we get to this point we don't know how to handle it
                    src.UnreadToken(token);
                    if (!HandleSingleGuiCommand(entityGui, src)) {
                        // not handled there see if entity or any of its targets can handle it
                        // this will only work for one target atm
                        if (entityGui.HandleSingleGuiCommand(entityGui, src)) {
                            continue;
                        }

                        final int c = entityGui.targets.Num();
                        int i;
                        for (i = 0; i < c; i++) {
                            targetEnt = entityGui.targets.oGet(i).GetEntity();
                            if ((targetEnt != null) && targetEnt.HandleSingleGuiCommand(entityGui, src)) {
                                break;
                            }
                        }

                        if (i == c) {
                            // not handled
                            common.DPrintf("idEntity::HandleGuiCommands: '%s' not handled\n", token);
                            src.ReadToken(token);
                        }
                    }

                }
            }
            return ret;
        }

        public boolean HandleSingleGuiCommand(idEntity entityGui, idLexer src) {
            return false;
        }

        /* **********************************************************************

         Targets
	
         ***********************************************************************/
        // targets
        /*
         ===============
         idEntity::FindTargets

         We have to wait until all entities are spawned
         Used to build lists of targets after the entity is spawned.  Since not all entities
         have been spawned when the entity is created at map load time, we have to wait
         ===============
         */
        public void FindTargets() {
            int i;

            // targets can be a list of multiple names
            gameLocal.GetTargets(this.spawnArgs, this.targets, "target");

            // ensure that we don't target ourselves since that could cause an infinite loop when activating entities
            for (i = 0; i < this.targets.Num(); i++) {
                if (this.targets.oGet(i).GetEntity() == this) {
                    idGameLocal.Error("Entity '%s' is targeting itself", this.name);
                }
            }
        }

        public void RemoveNullTargets() {
            int i;

            for (i = this.targets.Num() - 1; i >= 0; i--) {
                if (NOT(this.targets.oGet(i).GetEntity())) {
                    this.targets.RemoveIndex(i);
                }
            }
        }

        /*
         ==============================
         idEntity::ActivateTargets

         "activator" should be set to the entity that initiated the firing.
         ==============================
         */
        public void ActivateTargets(idEntity activator) {
            idEntity ent;
            int i, j;

            for (i = 0; i < this.targets.Num(); i++) {
                ent = this.targets.oGet(i).GetEntity();
                if (null == ent) {
                    continue;
                }
                if (ent.RespondsTo(EV_Activate) || ent.HasSignal(SIG_TRIGGER)) {
                    ent.Signal(SIG_TRIGGER);
                    ent.ProcessEvent(EV_Activate, activator);
                }
                for (j = 0; j < MAX_RENDERENTITY_GUI; j++) {
                    if (ent.renderEntity.gui[j] != null) {
                        ent.renderEntity.gui[j].Trigger(gameLocal.time);
                    }
                }
            }
        }

        /* **********************************************************************

         Misc.
	
         ***********************************************************************/
        // misc
        public void Teleport(final idVec3 origin, final idAngles angles, idEntity destination) {
            GetPhysics().SetOrigin(origin);
            GetPhysics().SetAxis(angles.ToMat3());

            UpdateVisuals();
        }

        /*
         ============
         idEntity::TouchTriggers

         Activate all trigger entities touched at the current position.
         ============
         */
        public boolean TouchTriggers() {
            int i, numClipModels, numEntities;
            idClipModel cm;
            final idClipModel[] clipModels = new idClipModel[MAX_GENTITIES];
            idEntity ent;
            final trace_s trace = new trace_s();//memset( &trace, 0, sizeof( trace ) );

            trace.endpos.oSet(GetPhysics().GetOrigin());
            trace.endAxis.oSet(GetPhysics().GetAxis());

            numClipModels = gameLocal.clip.ClipModelsTouchingBounds(GetPhysics().GetAbsBounds(), CONTENTS_TRIGGER, clipModels, MAX_GENTITIES);
            numEntities = 0;

            for (i = 0; i < numClipModels; i++) {
                cm = clipModels[i];

                // don't touch it if we're the owner
                if (cm.GetOwner() == this) {
                    continue;
                }

                ent = cm.GetEntity();

                if (!ent.RespondsTo(EV_Touch) && !ent.HasSignal(SIG_TOUCH)) {
                    continue;
                }

                if (NOT(GetPhysics().ClipContents(cm))) {
                    continue;
                }

                numEntities++;

                trace.c.contents = cm.GetContents();
                trace.c.entityNum = cm.GetEntity().entityNumber;
                trace.c.id = cm.GetId();

                ent.Signal(SIG_TOUCH);
                ent.ProcessEvent(EV_Touch, this, trace);

                if (NOT(gameLocal.entities[this.entityNumber])) {
                    gameLocal.Printf("entity was removed while touching triggers\n");
                    return true;
                }
            }

            return (numEntities != 0);
        }

        public idCurve_Spline<idVec3> GetSpline() {
            int i, numPoints, t;
            idKeyValue kv;
            final idLexer lex = new idLexer();
            final idVec3 v = new idVec3();
            idCurve_Spline<idVec3> spline;
            final String curveTag = "curve_";

            kv = this.spawnArgs.MatchPrefix(curveTag);
            if (null == kv) {
                return null;
            }

            final idStr str = kv.GetKey().Right(kv.GetKey().Length() - curveTag.length());
            if (str.Icmp("CatmullRomSpline") == 0) {
                spline = new idCurve_CatmullRomSpline<>(idVec3.class);
            } else if (str.Icmp("nubs") == 0) {
                spline = new idCurve_NonUniformBSpline<>(idVec3.class);
            } else if (str.Icmp("nurbs") == 0) {
                spline = new idCurve_NURBS<>(idVec3.class);
            } else {
                spline = new idCurve_BSpline<>(idVec3.class);
            }

            spline.SetBoundaryType(idCurve_Spline.BT_CLAMPED);

            lex.LoadMemory(kv.GetValue().getData(), kv.GetValue().Length(), curveTag);
            numPoints = lex.ParseInt();
            lex.ExpectTokenString("(");
            for (t = i = 0; i < numPoints; i++, t += 100) {
                v.x = lex.ParseFloat();
                v.y = lex.ParseFloat();
                v.z = lex.ParseFloat();
                spline.AddValue(t, v);
            }
            lex.ExpectTokenString(")");

            return spline;
        }

        public void ShowEditingDialog() {
        }

        //
        // enum {
        public static final int EVENT_STARTSOUNDSHADER = 0;
        public static final int EVENT_STOPSOUNDSHADER  = 1;
        public static final int EVENT_MAXEVENTS        = 2;
        // };
//

        /* **********************************************************************

         Network
	
         ***********************************************************************/
        public void ClientPredictionThink() {
            RunPhysics();
            Present();
        }

        public void WriteToSnapshot(idBitMsgDelta msg) {
        }

        public void ReadFromSnapshot(final idBitMsgDelta msg) {
        }

        public boolean ServerReceiveEvent(int event, int time, final idBitMsg msg) {
            switch (event) {
                case 0:
                default:
                    return false;
            }
        }

        public boolean ClientReceiveEvent(int event, int time, final idBitMsg msg) {
            int index;
            idSoundShader shader;
            int/*s_channelType*/ channel;

            switch (event) {
                case EVENT_STARTSOUNDSHADER: {
                    // the sound stuff would early out
                    assert (gameLocal.isNewFrame);
                    if (time < (gameLocal.realClientTime - 1000)) {
                        // too old, skip it ( reliable messages don't need to be parsed in full )
                        common.DPrintf("ent 0x%x: start sound shader too old (%d ms)\n", this.entityNumber, gameLocal.realClientTime - time);
                        return true;
                    }
                    index = gameLocal.ClientRemapDecl(DECL_SOUND, msg.ReadLong());
                    if ((index >= 0) && (index < declManager.GetNumDecls(DECL_SOUND))) {
                        shader = declManager.SoundByIndex(index, false);
                        channel = /*(s_channelType)*/ msg.ReadByte();
                        StartSoundShader(shader, channel, 0, false, null);
                    }
                    return true;
                }
                case EVENT_STOPSOUNDSHADER: {
                    // the sound stuff would early out
                    assert (gameLocal.isNewFrame);
                    channel = /*(s_channelType)*/ msg.ReadByte();
                    StopSound(channel, false);
                    return true;
                }
                default: {
                    return false;
                }
            }
//            return false;
        }

        public void WriteBindToSnapshot(idBitMsgDelta msg) {
            int bindInfo;

            if (this.bindMaster != null) {
                bindInfo = this.bindMaster.entityNumber;
                bindInfo |= (this.fl.bindOrientated ? 1 : 0) << GENTITYNUM_BITS;
                if (this.bindJoint != INVALID_JOINT) {
                    bindInfo |= 1 << (GENTITYNUM_BITS + 1);
                    bindInfo |= this.bindJoint << (3 + GENTITYNUM_BITS);
                } else if (this.bindBody != -1) {
                    bindInfo |= 2 << (GENTITYNUM_BITS + 1);
                    bindInfo |= this.bindBody << (3 + GENTITYNUM_BITS);
                }
            } else {
                bindInfo = ENTITYNUM_NONE;
            }
            msg.WriteBits(bindInfo, GENTITYNUM_BITS + 3 + 9);
        }

        public void ReadBindFromSnapshot(final idBitMsgDelta msg) {
            int bindInfo, bindEntityNum, bindPos;
            boolean bindOrientated;
            idEntity master;

            bindInfo = msg.ReadBits(GENTITYNUM_BITS + 3 + 9);
            bindEntityNum = bindInfo & ((1 << GENTITYNUM_BITS) - 1);

            if (bindEntityNum != ENTITYNUM_NONE) {
                master = gameLocal.entities[bindEntityNum];

                bindOrientated = ((bindInfo >> GENTITYNUM_BITS) & 1) == 1;
                bindPos = (bindInfo >> (GENTITYNUM_BITS + 3));
                switch ((bindInfo >> (GENTITYNUM_BITS + 1)) & 3) {
                    case 1: {
                        BindToJoint(master, /*(jointHandle_t)*/ bindPos, bindOrientated);
                        break;
                    }
                    case 2: {
                        BindToBody(master, bindPos, bindOrientated);
                        break;
                    }
                    default: {
                        Bind(master, bindOrientated);
                        break;
                    }
                }
            } else if (this.bindMaster != null) {
                Unbind();
            }
        }

        public void WriteColorToSnapshot(idBitMsgDelta msg) {
            final idVec4 color = new idVec4(
                    this.renderEntity.shaderParms[SHADERPARM_RED],
                    this.renderEntity.shaderParms[SHADERPARM_GREEN],
                    this.renderEntity.shaderParms[SHADERPARM_BLUE],
                    this.renderEntity.shaderParms[SHADERPARM_ALPHA]);
            msg.WriteLong((int) PackColor(color));
        }

        public void ReadColorFromSnapshot(final idBitMsgDelta msg) {
            final idVec4 color = new idVec4();

            UnpackColor(msg.ReadLong(), color);
            this.renderEntity.shaderParms[SHADERPARM_RED] = color.oGet(0);
            this.renderEntity.shaderParms[SHADERPARM_GREEN] = color.oGet(1);
            this.renderEntity.shaderParms[SHADERPARM_BLUE] = color.oGet(2);
            this.renderEntity.shaderParms[SHADERPARM_ALPHA] = color.oGet(3);
        }

        public void WriteGUIToSnapshot(idBitMsgDelta msg) {
            // no need to loop over MAX_RENDERENTITY_GUI at this time
            if (this.renderEntity.gui[0] != null) {
                msg.WriteByte(this.renderEntity.gui[0].State().GetInt("networkState"));
            } else {
                msg.WriteByte(0);
            }
        }

        public void ReadGUIFromSnapshot(final idBitMsgDelta msg) {
            int state;
            idUserInterface gui;
            state = msg.ReadByte();
            gui = this.renderEntity.gui[0];
            if ((gui != null) && (state != this.mpGUIState)) {
                this.mpGUIState = state;
                gui.SetStateInt("networkState", state);
                gui.HandleNamedEvent("networkState");
            }
        }

        /*
         ================
         idEntity::ServerSendEvent

         Saved events are also sent to any client that connects late so all clients
         always receive the events nomatter what time they join the game.
         ================
         */
        public void ServerSendEvent(int eventId, final idBitMsg msg, boolean saveEvent, int excludeClient) {
            final idBitMsg outMsg = new idBitMsg();
            final ByteBuffer msgBuf = ByteBuffer.allocate(MAX_GAME_MESSAGE_SIZE);

            if (!gameLocal.isServer) {
                return;
            }

            // prevent dupe events caused by frame re-runs
            if (!gameLocal.isNewFrame) {
                return;
            }

            outMsg.Init(msgBuf, MAX_GAME_MESSAGE_SIZE);
            outMsg.BeginWriting();
            outMsg.WriteByte(GAME_RELIABLE_MESSAGE_EVENT);
            outMsg.WriteBits(gameLocal.GetSpawnId(this), 32);
            outMsg.WriteByte(eventId);
            outMsg.WriteLong(gameLocal.time);
            if (msg != null) {
                outMsg.WriteBits(msg.GetSize(), idMath.BitsForInteger(MAX_EVENT_PARAM_SIZE));
                outMsg.WriteData(msg.GetData(), msg.GetSize());
            } else {
                outMsg.WriteBits(0, idMath.BitsForInteger(MAX_EVENT_PARAM_SIZE));
            }

            if (excludeClient != -1) {
                networkSystem.ServerSendReliableMessageExcluding(excludeClient, outMsg);
            } else {
                networkSystem.ServerSendReliableMessage(-1, outMsg);
            }

            if (saveEvent) {
                gameLocal.SaveEntityNetworkEvent(this, eventId, msg);
            }
        }

        public void ClientSendEvent(int eventId, final idBitMsg msg) {
            final idBitMsg outMsg = new idBitMsg();
            final ByteBuffer msgBuf = ByteBuffer.allocate(MAX_GAME_MESSAGE_SIZE);

            if (!gameLocal.isClient) {
                return;
            }

            // prevent dupe events caused by frame re-runs
            if (!gameLocal.isNewFrame) {
                return;
            }

            outMsg.Init(msgBuf, MAX_GAME_MESSAGE_SIZE);
            outMsg.BeginWriting();
            outMsg.WriteByte(GAME_RELIABLE_MESSAGE_EVENT);
            outMsg.WriteBits(gameLocal.GetSpawnId(this), 32);
            outMsg.WriteByte(eventId);
            outMsg.WriteLong(gameLocal.time);
            if (msg != null) {
                outMsg.WriteBits(msg.GetSize(), idMath.BitsForInteger(MAX_EVENT_PARAM_SIZE));
                outMsg.WriteData(msg.GetData(), msg.GetSize());
            } else {
                outMsg.WriteBits(0, idMath.BitsForInteger(MAX_EVENT_PARAM_SIZE));
            }

            networkSystem.ClientSendReliableMessage(outMsg);
        }

        private void FixupLocalizedStrings() {
            for (int i = 0; i < this.spawnArgs.GetNumKeyVals(); i++) {
                final idKeyValue kv = this.spawnArgs.GetKeyVal(i);
                if (idStr.Cmpn(kv.GetValue().getData(), STRTABLE_ID, STRTABLE_ID_LENGTH) == 0) {
                    this.spawnArgs.Set(kv.GetKey(), common.GetLanguageDict().GetString(kv.GetValue()));
                }
            }
        }

        /*
         ================
         idEntity::DoDormantTests

         Monsters and other expensive entities that are completely closed
         off from the player can skip all of their work
         ================
         */
        private boolean DoDormantTests() {                // dormant == on the active list, but out of PVS

            if (this.fl.neverDormant) {
                return false;
            }

            // if the monster area is not topologically connected to a player
            if (!gameLocal.InPlayerConnectedArea(this)) {
                if (this.dormantStart == 0) {
                    this.dormantStart = gameLocal.time;
                }
                if ((gameLocal.time - this.dormantStart) < DELAY_DORMANT_TIME) {
                    // just got closed off, don't go dormant yet
                    return false;
                }
                return true;
            } else {
                // the monster area is topologically connected to a player, but if
                // the monster hasn't been woken up before, do the more precise PVS check
                if (!this.fl.hasAwakened) {
                    if (!gameLocal.InPlayerPVS(this)) {
                        return true;        // stay dormant
                    }
                }

                // wake up
                this.dormantStart = 0;
                this.fl.hasAwakened = true;        // only go dormant when area closed off now, not just out of PVS
                return false;
            }

//            return false; 
        }


        /* **********************************************************************

         Physics.
	
         ***********************************************************************/
        // physics
        // initialize the default physics
        private static int DBG_InitDefaultPhysics = 0;
        private void InitDefaultPhysics(final idVec3 origin, final idMat3 axis) {
            final String[] temp = new String[1];
            idClipModel clipModel = null;DBG_InitDefaultPhysics++;

            // check if a clipmodel key/value pair is set
            if (this.spawnArgs.GetString("clipmodel", "", temp)) {
                if (idClipModel.CheckModel(temp[0]) != 0) {
                    clipModel = new idClipModel(temp[0]);
                }
            }

            if (!this.spawnArgs.GetBool("noclipmodel", "0")) {

                // check if mins/maxs or size key/value pairs are set
                if (NOT(clipModel)) {
                    final idVec3 size = new idVec3();
                    final idBounds bounds = new idBounds();
                    boolean setClipModel = false;

                    if (this.spawnArgs.GetVector("mins", null, bounds.oGet(0))
                            && this.spawnArgs.GetVector("maxs", null, bounds.oGet(1))) {
                        setClipModel = true;
                        if ((bounds.oGet(0).oGet(0) > bounds.oGet(1).oGet(0)) || (bounds.oGet(0).oGet(1) > bounds.oGet(1).oGet(1)) || (bounds.oGet(0).oGet(2) > bounds.oGet(1).oGet(2))) {
                            idGameLocal.Error("Invalid bounds '%s'-'%s' on entity '%s'", bounds.oGet(0).ToString(), bounds.oGet(1).ToString(), this.name);
                        }
                    } else if (this.spawnArgs.GetVector("size", null, size)) {
                        if ((size.x < 0.0f) || (size.y < 0.0f) || (size.z < 0.0f)) {
                            idGameLocal.Error("Invalid size '%s' on entity '%s'", size.ToString(), this.name);
                        }
                        bounds.oGet(0).Set(size.x * -0.5f, size.y * -0.5f, 0.0f);
                        bounds.oGet(1).Set(size.x * 0.5f, size.y * 0.5f, size.z);
                        setClipModel = true;
                    }

                    if (setClipModel) {
                        final int[] numSides = {0};
                        final idTraceModel trm = new idTraceModel();

                        if (this.spawnArgs.GetInt("cylinder", "0", numSides) && (numSides[0] > 0)) {
                            trm.SetupCylinder(bounds, Math.max(numSides[0], 3));
                        } else if (this.spawnArgs.GetInt("cone", "0", numSides) && (numSides[0] > 0)) {
                            trm.SetupCone(bounds, Math.max(numSides[0], 3));
                        } else {
                            trm.SetupBox(bounds);
                        }
                        clipModel = new idClipModel(trm);
                    }
                }

                // check if the visual model can be used as collision model
                if (NOT(clipModel)) {
                    temp[0] = this.spawnArgs.GetString("model");
                    if ((temp[0] != null) && (!temp[0].isEmpty())) {
                        if (idClipModel.CheckModel(temp[0]) != 0) {
                            clipModel = new idClipModel(temp[0]);
                        }
                    }
                }
            }

            this.defaultPhysicsObj.SetSelf(this);
            this.defaultPhysicsObj.SetClipModel(clipModel, 1.0f);
            this.defaultPhysicsObj.SetOrigin(origin);
            this.defaultPhysicsObj.SetAxis(axis);

            this.physics = this.defaultPhysicsObj;
        }

        // update visual position from the physics
        private void UpdateFromPhysics(boolean moveBack) {

            if (IsType(idActor.class)) {
                final idActor actor = (idActor) this;

                // set master delta angles for actors
                if (GetBindMaster() != null) {
                    final idAngles delta = actor.GetDeltaViewAngles();
                    if (moveBack) {
                        delta.yaw -= ((idPhysics_Actor) this.physics).GetMasterDeltaYaw();
                    } else {
                        delta.yaw += ((idPhysics_Actor) this.physics).GetMasterDeltaYaw();
                    }
                    actor.SetDeltaViewAngles(delta);
                }
            }

            UpdateVisuals();
        }

        // entity binding
        private boolean InitBind(idEntity master) {        // initialize an entity binding

            if ((null == master) || master.equals(gameLocal.world)) {
                // this can happen in scripts, so safely exit out.
                return false;
            }

            if (master.equals(this)) {//TODO:equals
                idGameLocal.Error("Tried to bind an object to itself.");
                return false;
            }

            if (this == gameLocal.world) {
                idGameLocal.Error("Tried to bind world to another entity");
                return false;
            }

            // unbind myself from my master
            Unbind();

            // add any bind constraints to an articulated figure
            if ((master != null) && IsType(idAFEntity_Base.class)) {
                ((idAFEntity_Base) this).AddBindConstraints();
            }

            return true;
        }

        private void FinishBind() {                // finish an entity binding

            // set the master on the physics object
            this.physics.SetMaster(this.bindMaster, this.fl.bindOrientated);

            // We are now separated from our previous team and are either
            // an individual, or have a team of our own.  Now we can join
            // the new bindMaster's team.  Bindmaster must be set before
            // joining the team, or we will be placed in the wrong position
            // on the team.
            JoinTeam(this.bindMaster);

            // if our bindMaster is enabled during a cinematic, we must be, too
            this.cinematic = this.bindMaster.cinematic;

            // make sure the team master is active so that physics get run
            this.teamMaster.BecomeActive(TH_PHYSICS);
        }

        private void RemoveBinds() {                // deletes any entities bound to this object
            idEntity ent;
            idEntity next;

            for (ent = this.teamChain; ent != null; ent = next) {
                next = ent.teamChain;
                if (ent.bindMaster == this) {
                    ent.Unbind();
                    ent.PostEventMS(EV_Remove, 0);
                    next = this.teamChain;
                }
            }
        }

        private void QuitTeam() {                    // leave the current team
            idEntity ent;

            if (null == this.teamMaster) {
                return;
            }

            // check if I'm the teamMaster
            if (this.teamMaster == this) {
                // do we have more than one teammate?
                if (null == this.teamChain.teamChain) {
                    // no, break up the team
                    this.teamChain.teamMaster = null;
                } else {
                    // yes, so make the first teammate the teamMaster
                    for (ent = this.teamChain; ent != null; ent = ent.teamChain) {
                        ent.teamMaster = this.teamChain;
                    }
                }
            } else {
                assert (this.teamMaster != null);
                assert (this.teamMaster.teamChain != null);

                // find the previous member of the teamChain
                ent = this.teamMaster;
                while (ent.teamChain != this) {
                    assert (ent.teamChain != null); // this should never happen
                    ent = ent.teamChain;
                }

                // remove this from the teamChain
                ent.teamChain = this.teamChain;

                // if no one is left on the team, break it up
                if (null == this.teamMaster.teamChain) {
                    this.teamMaster.teamMaster = null;
                }
            }

            this.teamMaster = null;
            this.teamChain = null;
        }

        private void UpdatePVSAreas() {
            int localNumPVSAreas;
            final int[] localPVSAreas = new int[32];
            final idBounds modelAbsBounds = new idBounds();
            int i;

            modelAbsBounds.FromTransformedBounds(this.renderEntity.bounds, this.renderEntity.origin, this.renderEntity.axis);
            localNumPVSAreas = gameLocal.pvs.GetPVSAreas(modelAbsBounds, localPVSAreas, localPVSAreas.length);

            // FIXME: some particle systems may have huge bounds and end up in many PVS areas
            // the first MAX_PVS_AREAS may not be visible to a network client and as a result the particle system may not show up when it should
            if (localNumPVSAreas > MAX_PVS_AREAS) {
                localNumPVSAreas = gameLocal.pvs.GetPVSAreas(new idBounds(modelAbsBounds.GetCenter()).Expand(64.0f), localPVSAreas, localPVSAreas.length);
            }

            for (this.numPVSAreas = 0; (this.numPVSAreas < MAX_PVS_AREAS) && (this.numPVSAreas < localNumPVSAreas); this.numPVSAreas++) {
                this.PVSAreas[this.numPVSAreas] = localPVSAreas[this.numPVSAreas];
            }

            for (i = this.numPVSAreas; i < MAX_PVS_AREAS; i++) {
                this.PVSAreas[i] = 0;
            }
        }

        /* **********************************************************************

         Events
	
         ***********************************************************************/
        // events
        private void Event_GetName() {
            idThread.ReturnString(this.name.getData());
        }

        private static void Event_SetName(idEntity e, final idEventArg<String> newName) {
            e.SetName(newName.value);
        }

        private void Event_FindTargets() {
            FindTargets();
        }

        /*
         ============
         idEntity::Event_ActivateTargets

         Activates any entities targeted by this entity.  Mainly used as an
         event to delay activating targets.
         ============
         */
        private static void Event_ActivateTargets(idEntity e, idEventArg<idEntity> activator) {
            e.ActivateTargets(activator.value);
        }

        private void Event_NumTargets() {
            idThread.ReturnFloat(this.targets.Num());
        }

        private static void Event_GetTarget(idEntity e, idEventArg<Float> index) {
            int i;

            i = index.value.intValue();
            if ((i < 0) || (i >= e.targets.Num())) {
                idThread.ReturnEntity(null);
            } else {
                idThread.ReturnEntity(e.targets.oGet(i).GetEntity());
            }
        }

        private static void Event_RandomTarget(idEntity e, final idEventArg<String> ignor) {
            int num;
            idEntity ent;
            int i;
            int ignoreNum;
            final String ignore = ignor.value;

            e.RemoveNullTargets();
            if (0 == e.targets.Num()) {
                idThread.ReturnEntity(null);
                return;
            }

            ignoreNum = -1;
            if ((ignore != null) && (!ignore.isEmpty()) && (e.targets.Num() > 1)) {
                for (i = 0; i < e.targets.Num(); i++) {
                    ent = e.targets.oGet(i).GetEntity();
                    if ((ent != null) && (ent.name.equals(ignore))) {
                        ignoreNum = i;
                        break;
                    }
                }
            }

            if (ignoreNum >= 0) {
                num = gameLocal.random.RandomInt(e.targets.Num() - 1);
                if (num >= ignoreNum) {
                    num++;
                }
            } else {
                num = gameLocal.random.RandomInt(e.targets.Num());
            }

            ent = e.targets.oGet(num).GetEntity();
            idThread.ReturnEntity(ent);
        }

        private static void Event_Bind(idEntity e, idEventArg<idEntity> master) {
            e.Bind(master.value, true);
        }

        private static void Event_BindPosition(idEntity e, idEventArg<idEntity> master) {
            e.Bind(master.value, false);
        }

        private static void Event_BindToJoint(idEntity e, idEventArg<idEntity> master, final idEventArg<String> jointname, idEventArg<Float> orientated) {
            e.BindToJoint(master.value, jointname.value, (orientated.value != 0));
        }

        private void Event_Unbind() {
            Unbind();
        }

        private void Event_RemoveBinds() {
            RemoveBinds();
        }

        private void Event_SpawnBind() {
            idEntity parent;
            final String[] bind = new String[1], joint = new String[1], bindanim = new String[1];
            int/*jointHandle_t*/ bindJoint;
            boolean bindOrientated;
            final int[] id = new int[1];
            idAnim anim;
            int animNum;
            idAnimator parentAnimator;

            if (this.spawnArgs.GetString("bind", "", bind)) {
                if (idStr.Icmp(bind[0], "worldspawn") == 0) {
                    //FIXME: Completely unneccessary since the worldspawn is called "world"
                    parent = gameLocal.world;
                } else {
                    parent = gameLocal.FindEntity(bind[0]);
                }
                bindOrientated = this.spawnArgs.GetBool("bindOrientated", "1");
                if (parent != null) {
                    // bind to a joint of the skeletal model of the parent
                    if (this.spawnArgs.GetString("bindToJoint", "", joint) && (joint[0] != null)) {//TODO:check if java actually compiles them in the right order.
                        parentAnimator = parent.GetAnimator();
                        if (NOT(parentAnimator)) {
                            idGameLocal.Error("Cannot bind to joint '%s' on '%s'.  Entity does not support skeletal models.", joint[0], this.name);
                        }
                        bindJoint = parentAnimator.GetJointHandle(joint[0]);
                        if (bindJoint == INVALID_JOINT) {
                            idGameLocal.Error("Joint '%s' not found for bind on '%s'", joint[0], this.name);
                        }

                        // bind it relative to a specific anim
                        if ((parent.spawnArgs.GetString("bindanim", "", bindanim) || parent.spawnArgs.GetString("anim", "", bindanim)) && (bindanim[0] != null)) {
                            animNum = parentAnimator.GetAnim(bindanim[0]);
                            if (0 == animNum) {
                                idGameLocal.Error("Anim '%s' not found for bind on '%s'", bindanim[0], this.name);
                            }
                            anim = parentAnimator.GetAnim(animNum);
                            if (NOT(anim)) {
                                idGameLocal.Error("Anim '%s' not found for bind on '%s'", bindanim[0], this.name);
                            }

                            // make sure parent's render origin has been set
                            parent.UpdateModelTransform();

                            //FIXME: need a BindToJoint that accepts a joint position
                            parentAnimator.CreateFrame(gameLocal.time, true);
                            final idJointMat[] frame = parent.renderEntity.joints;
                            GameEdit.gameEdit.ANIM_CreateAnimFrame(parentAnimator.ModelHandle(), anim.MD5Anim(0), parent.renderEntity.numJoints, frame, 0, parentAnimator.ModelDef().GetVisualOffset(), parentAnimator.RemoveOrigin());
                            BindToJoint(parent, joint[0], bindOrientated);
                            parentAnimator.ForceUpdate();
                        } else {
                            BindToJoint(parent, joint[0], bindOrientated);
                        }
                    } // bind to a body of the physics object of the parent
                    else if (this.spawnArgs.GetInt("bindToBody", "0", id)) {
                        BindToBody(parent, id[0], bindOrientated);
                    } // bind to the parent
                    else {
                        Bind(parent, bindOrientated);
                    }
                }
            }
        }

        private static void Event_SetOwner(idEntity e, idEventArg<idEntity> owner) {
            int i;

            for (i = 0; i < e.GetPhysics().GetNumClipModels(); i++) {
                e.GetPhysics().GetClipModel(i).SetOwner(owner.value);
            }
        }

        private static void Event_SetModel(idEntity e, final idEventArg<String> modelname) {
            e.SetModel(modelname.value);
        }

        private static void Event_SetSkin(idEntity e, final idEventArg<String> skinname) {
            e.renderEntity.customSkin = declManager.FindSkin(skinname.value);
            e.UpdateVisuals();
        }

        private static void Event_GetShaderParm(idEntity e, idEventArg<Integer> parm) {
            final int parmnum = parm.value;
            if ((parmnum < 0) || (parmnum >= MAX_ENTITY_SHADER_PARMS)) {
                idGameLocal.Error("shader parm index (%d) out of range", parmnum);
            }

            idThread.ReturnFloat(e.renderEntity.shaderParms[parmnum]);
        }

        private static void Event_SetShaderParm(idEntity e, idEventArg<Integer> parmnum, idEventArg<Float> value) {
            e.SetShaderParm(parmnum.value, value.value);
        }

        private static void Event_SetShaderParms(idEntity e, idEventArg<Float> parm0, idEventArg<Float> parm1, idEventArg<Float> parm2, idEventArg<Float> parm3) {
            e.renderEntity.shaderParms[SHADERPARM_RED] = parm0.value;
            e.renderEntity.shaderParms[SHADERPARM_GREEN] = parm1.value;
            e.renderEntity.shaderParms[SHADERPARM_BLUE] = parm2.value;
            e.renderEntity.shaderParms[SHADERPARM_ALPHA] = parm3.value;
            e.UpdateVisuals();
        }

        private static void Event_SetColor(idEntity e, idEventArg<Float> red, idEventArg<Float> green, idEventArg<Float> blue) {
            e.SetColor(red.value, green.value, blue.value);
        }

        private void Event_GetColor() {
            final idVec3 out = new idVec3();

            GetColor(out);
            idThread.ReturnVector(out);
        }

        private void Event_IsHidden() {
            idThread.ReturnInt(this.fl.hidden);
        }

        private void Event_Hide() {
            Hide();
        }

        private void Event_Show() {
            Show();
        }

        private static void Event_CacheSoundShader(idEntity e, final idEventArg<String> soundName) {
            declManager.FindSound(soundName.value);
        }

        private static void Event_StartSoundShader(idEntity e, final idEventArg<String> soundName, idEventArg<Integer> channel) {
            final int[] length = new int[1];

            e.StartSoundShader(declManager.FindSound(soundName.value), /*(s_channelType)*/ channel.value, 0, false, length);
            idThread.ReturnFloat(MS2SEC(length[0]));
        }

        private static void Event_StopSound(idEntity e, idEventArg<Integer> channel, idEventArg<Integer> netSync) {
            e.StopSound(channel.value, (netSync.value != 0));
        }

        private static void Event_StartSound(idEntity e, final idEventArg<String> soundName, idEventArg<Integer> channel, idEventArg<Integer> netSync) {
            final int[] time = new int[1];

            e.StartSound(soundName.value, /*(s_channelType)*/ channel.value, 0, (netSync.value != 0), time);
            idThread.ReturnFloat(MS2SEC(time[0]));
        }

        private static void Event_FadeSound(idEntity e, idEventArg<Integer> channel, idEventArg<Float> to, idEventArg<Float> over) {
            if (e.refSound.referenceSound != null) {
                e.refSound.referenceSound.FadeSound(channel.value, to.value, over.value);
            }
        }

        private void Event_GetWorldOrigin() {
            idThread.ReturnVector(GetPhysics().GetOrigin());
        }

        private static void Event_SetWorldOrigin(idEntity e, final idEventArg<idVec3> org) {
            final idVec3 neworg = e.GetLocalCoordinates(org.value);
            e.SetOrigin(neworg);
        }

        private void Event_GetOrigin() {
            idThread.ReturnVector(GetLocalCoordinates(GetPhysics().GetOrigin()));
        }

        private static void Event_SetOrigin(idEntity e, final idEventArg<idVec3> org) {
            e.SetOrigin(org.value);
        }

        private void Event_GetAngles() {
            final idAngles ang = GetPhysics().GetAxis().ToAngles();
            idThread.ReturnVector(new idVec3(ang.oGet(0), ang.oGet(1), ang.oGet(2)));
        }

        private static void Event_SetAngles(idEntity e, final idEventArg<idAngles> ang) {
            e.SetAngles(ang.value);
        }

        private static void Event_SetLinearVelocity(idEntity e, final idEventArg<idVec3> velocity) {
            e.GetPhysics().SetLinearVelocity(velocity.value);
        }

        private void Event_GetLinearVelocity() {
            idThread.ReturnVector(GetPhysics().GetLinearVelocity());
        }

        private static void Event_SetAngularVelocity(idEntity e, final idEventArg<idVec3> velocity) {
            e.GetPhysics().SetAngularVelocity(velocity.value);
        }

        private void Event_GetAngularVelocity() {
            idThread.ReturnVector(GetPhysics().GetAngularVelocity());
        }

        private static void Event_SetSize(idEntity e, final idEventArg<idVec3> mins, final idEventArg<idVec3> maxs) {
            e.GetPhysics().SetClipBox(new idBounds(mins.value, maxs.value), 1.0f);
        }

        private void Event_GetSize() {
            idBounds bounds;

            bounds = GetPhysics().GetBounds();
            idThread.ReturnVector(bounds.oGet(1).oMinus(bounds.oGet(0)));
        }

        private void Event_GetMins() {
            idThread.ReturnVector(GetPhysics().GetBounds().oGet(0));
        }

        private void Event_GetMaxs() {
            idThread.ReturnVector(GetPhysics().GetBounds().oGet(1));
        }

        private static void Event_Touches(idEntity e, idEventArg<idEntity> ent) {
            if (NOT(ent.value)) {
                idThread.ReturnInt(false);
                return;
            }

            final idBounds myBounds = e.GetPhysics().GetAbsBounds();
            final idBounds entBounds = ent.value.GetPhysics().GetAbsBounds();

            idThread.ReturnInt(myBounds.IntersectsBounds(entBounds));
        }

        private static void Event_SetGuiParm(idEntity e, final idEventArg<String> k, final idEventArg<String> v) {
            final String key = k.value;
            final String val = v.value;
            for (int i = 0; i < MAX_RENDERENTITY_GUI; i++) {
                if (e.renderEntity.gui[i] != null) {
                    if (idStr.Icmpn(key, "gui_", 4) == 0) {
                        e.spawnArgs.Set(key, val);
                    }
                    e.renderEntity.gui[i].SetStateString(key, val);
                    e.renderEntity.gui[i].StateChanged(gameLocal.time);
                }
            }
        }

        private static void Event_SetGuiFloat(idEntity e, final idEventArg<String> key, idEventArg<Float> f) {
            for (int i = 0; i < MAX_RENDERENTITY_GUI; i++) {
                if (e.renderEntity.gui[i] != null) {
                    e.renderEntity.gui[i].SetStateString(key.value, va("%f", f.value));
                    e.renderEntity.gui[i].StateChanged(gameLocal.time);
                }
            }
        }

        private static void Event_GetNextKey(idEntity e, final idEventArg<String> prefix, final idEventArg<String> lastMatch) {
            final idKeyValue kv;
            final idKeyValue previous;

            if (!lastMatch.value.isEmpty()) {
                previous = e.spawnArgs.FindKey(lastMatch.value);
            } else {
                previous = null;
            }

            kv = e.spawnArgs.MatchPrefix(prefix.value, previous);
            if (null == kv) {
                idThread.ReturnString("");
            } else {
                idThread.ReturnString(kv.GetKey());
            }
        }

        private static void Event_SetKey(idEntity e, final idEventArg<String> key, final idEventArg<String> value) {
            e.spawnArgs.Set(key.value, value.value);
        }

        private static void Event_GetKey(idEntity e, final idEventArg<String> key) {
            final String[] value = new String[1];

            e.spawnArgs.GetString(key.value, "", value);
            idThread.ReturnString(value[0]);
        }

        private static void Event_GetIntKey(idEntity e, final idEventArg<String> key) {
            final int[] value = new int[1];

            e.spawnArgs.GetInt(key.value, "0", value);

            // scripts only support floats
            idThread.ReturnFloat(value[0]);
        }

        private static void Event_GetFloatKey(idEntity e, final idEventArg<String> key) {
            final float[] value = new float[1];

            e.spawnArgs.GetFloat(key.value, "0", value);
            idThread.ReturnFloat(value[0]);
        }

        private static void Event_GetVectorKey(idEntity e, final idEventArg<String> key) {
            final idVec3 value = new idVec3();

            e.spawnArgs.GetVector(key.value, "0 0 0", value);
            idThread.ReturnVector(value);
        }

        private static void Event_GetEntityKey(idEntity e, final idEventArg<String> key) {
            idEntity ent;
            final String[] entName = new String[1];

            if (!e.spawnArgs.GetString(key.value, null, entName)) {
                idThread.ReturnEntity(null);
                return;
            }

            ent = gameLocal.FindEntity(entName[0]);
            if (null == ent) {
                gameLocal.Warning("Couldn't find entity '%s' specified in '%s' key in entity '%s'", entName, key, e.name);
            }

            idThread.ReturnEntity(ent);
        }

        private void Event_RestorePosition() {
            final idVec3 org = new idVec3();
            idAngles angles = new idAngles();
            final idMat3 axis = new idMat3();
            idEntity part;

            this.spawnArgs.GetVector("origin", "0 0 0", org);

            // get the rotation matrix in either full form, or single angle form
            if (this.spawnArgs.GetMatrix("rotation", "1 0 0 0 1 0 0 0 1", axis)) {
                angles = axis.ToAngles();
            } else {
                angles.oSet(0, 0);
                angles.oSet(1, this.spawnArgs.GetFloat("angle"));
                angles.oSet(2, 0);
            }

            Teleport(org, angles, null);

            for (part = this.teamChain; part != null; part = part.teamChain) {
                if (part.bindMaster != this) {
                    continue;
                }
                if (part.GetPhysics().IsType(idPhysics_Parametric.class)) {
                    if (((idPhysics_Parametric) part.GetPhysics()).IsPusher()) {
                        gameLocal.Warning("teleported '%s' which has the pushing mover '%s' bound to it\n", GetName(), part.GetName());
                    }
                } else if (part.GetPhysics().IsType(idPhysics_AF.class)) {
                    gameLocal.Warning("teleported '%s' which has the articulated figure '%s' bound to it\n", GetName(), part.GetName());
                }
            }
        }

        private void Event_UpdateCameraTarget() {
            final String target;
            idKeyValue kv;
            idVec3 dir;

            target = this.spawnArgs.GetString("cameraTarget");

            this.cameraTarget = gameLocal.FindEntity(target);

            if (this.cameraTarget != null) {
                kv = this.cameraTarget.spawnArgs.MatchPrefix("target", null);
                while (kv != null) {
                    final idEntity ent = gameLocal.FindEntity(kv.GetValue());
                    if ((ent != null) && (idStr.Icmp(ent.GetEntityDefName(), "target_null") == 0)) {
                        dir = ent.GetPhysics().GetOrigin().oMinus(this.cameraTarget.GetPhysics().GetOrigin());
                        dir.Normalize();
                        this.cameraTarget.SetAxis(dir.ToMat3());
                        SetAxis(dir.ToMat3());
                        break;
                    }
                    kv = this.cameraTarget.spawnArgs.MatchPrefix("target", kv);
                }
            }
            UpdateVisuals();
        }

        private static void Event_DistanceTo(idEntity e, idEventArg<idEntity> ent) {
            if (null == ent.value) {
                // just say it's really far away
                idThread.ReturnFloat(MAX_WORLD_SIZE);
            } else {
                final float dist = e.GetPhysics().GetOrigin().oMinus(ent.value.GetPhysics().GetOrigin()).LengthFast();
                idThread.ReturnFloat(dist);
            }
        }

        private static void Event_DistanceToPoint(idEntity e, final idEventArg<idVec3> point) {
            final float dist = e.GetPhysics().GetOrigin().oMinus(point.value).LengthFast();
            idThread.ReturnFloat(dist);
        }

        private static void Event_StartFx(idEntity e, final idEventArg<String> fx) {
            idEntityFx.StartFx(fx.value, null, null, e, true);
        }

        private void Event_WaitFrame() {
            idThread thread;

            thread = idThread.CurrentThread();
            if (thread != null) {
                thread.WaitFrame();
            }
        }

        private void Event_Wait(idEventArg<Float> time) {
            final idThread thread = idThread.CurrentThread();

            if (null == thread) {
                idGameLocal.Error("Event 'wait' called from outside thread");
            }

            thread.WaitSec(time.value);
        }

        private void Event_HasFunction(final idEventArg<String> name) {
            function_t func;

            func = this.scriptObject.GetFunction(name.value);
            if (func != null) {
                idThread.ReturnInt(true);
            } else {
                idThread.ReturnInt(false);
            }
        }

        private void Event_CallFunction(final idEventArg<String> _funcName) {
            final String funcName = _funcName.value;
            function_t func;
            idThread thread;

            thread = idThread.CurrentThread();
            if (null == thread) {
                idGameLocal.Error("Event 'callFunction' called from outside thread");
            }

            func = this.scriptObject.GetFunction(funcName);
            if (NOT(func)) {
                idGameLocal.Error("Unknown function '%s' in '%s'", funcName, this.scriptObject.GetTypeName());
            }

            if (func.type.NumParameters() != 1) {
                idGameLocal.Error("Function '%s' has the wrong number of parameters for 'callFunction'", funcName);
            }
            if (!this.scriptObject.GetTypeDef().Inherits(func.type.GetParmType(0))) {
                idGameLocal.Error("Function '%s' is the wrong type for 'callFunction'", funcName);
            }

            // function args will be invalid after this call
            thread.CallFunction(this, func, false);
        }

        private void Event_SetNeverDormant(idEventArg<Integer> enable) {
            this.fl.neverDormant = (enable.value != 0);
            this.dormantStart = 0;
        }
    }

    /*
     ===============================================================================

     Animated entity base class.

     ===============================================================================
     */
    public static class damageEffect_s {

        int/*jointHandle_t*/ jointNum;
        idVec3         localOrigin;
        idVec3         localNormal;
        int            time;
        idDeclParticle type;
        damageEffect_s next;
    }

    /*
     ===============================================================================

     idAnimatedEntity

     ===============================================================================
     */
    public static class idAnimatedEntity extends idEntity {
        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private static Map<idEventDef, eventCallback_t> eventCallbacks = new HashMap<>();
        static {
            eventCallbacks.putAll(idEntity.getEventCallBacks());
            eventCallbacks.put(EV_GetJointHandle, (eventCallback_t1<idAnimatedEntity>) idAnimatedEntity::Event_GetJointHandle);
            eventCallbacks.put(EV_ClearAllJoints, (eventCallback_t0<idAnimatedEntity>) idAnimatedEntity::Event_ClearAllJoints);
            eventCallbacks.put(EV_ClearJoint, (eventCallback_t1<idAnimatedEntity>) idAnimatedEntity::Event_ClearJoint);
            eventCallbacks.put(EV_SetJointPos, (eventCallback_t3<idAnimatedEntity>) idAnimatedEntity::Event_SetJointPos);
            eventCallbacks.put(EV_SetJointAngle, (eventCallback_t3<idAnimatedEntity>) idAnimatedEntity::Event_SetJointAngle);
            eventCallbacks.put(EV_GetJointPos, (eventCallback_t1<idAnimatedEntity>) idAnimatedEntity::Event_GetJointPos);
            eventCallbacks.put(EV_GetJointAngle, (eventCallback_t1<idAnimatedEntity>) idAnimatedEntity::Event_GetJointAngle);
        }

        // enum {
        public static final int EVENT_ADD_DAMAGE_EFFECT = idEntity.EVENT_MAXEVENTS;
        public static final int EVENT_MAXEVENTS         = EVENT_ADD_DAMAGE_EFFECT + 1;
        // };
        //
        protected idAnimator     animator;
        protected damageEffect_s damageEffects;
        //
        //

        //public	CLASS_PROTOTYPE( idAnimatedEntity );
//        public static idTypeInfo Type;
//        public static idClass CreateInstance();
//
//        public idTypeInfo GetType();
//        public static idEventFunc<idAnimatedEntity>[] eventCallbacks;
//
        public idAnimatedEntity() {
            this.animator = new idAnimator();
            this.animator.SetEntity(this);
            this.damageEffects = null;
        }

//							// ~idAnimatedEntity();

        /*
         ================
         idAnimatedEntity::Save

         archives object for save game file
         ================
         */
        @Override
        public void Save(idSaveGame savefile) {
            this.animator.Save(savefile);

            // Wounds are very temporary, ignored at this time
            //damageEffect_s			*damageEffects;
        }

        /*
         ================
         idAnimatedEntity::Restore

         unarchives object from save game file
         ================
         */
        @Override
        public void Restore(idRestoreGame savefile) {
            this.animator.Restore(savefile);

            // check if the entity has an MD5 model
            if (this.animator.ModelHandle() != null) {
                // set the callback to update the joints
                this.renderEntity.callback = idEntity.ModelCallback.getInstance();
                {
                    final idJointMat[][] joints = {null};
                    this.renderEntity.numJoints = this.animator.GetJoints(joints);
                    this.renderEntity.joints = joints[0];
                }
                this.animator.GetBounds(gameLocal.time, this.renderEntity.bounds);
                if (this.modelDefHandle != -1) {
                    gameRenderWorld.UpdateEntityDef(this.modelDefHandle, this.renderEntity);
                }
            }
        }

        @Override
        public void ClientPredictionThink() {
            RunPhysics();
            UpdateAnimation();
            Present();
        }

        @Override
        public void Think() {
            RunPhysics();
            UpdateAnimation();
            Present();
            UpdateDamageEffects();
        }

        public void UpdateAnimation() {
            // don't do animations if they're not enabled
            if (0 == (this.thinkFlags & TH_ANIMATE)) {
                return;
            }

            // is the model an MD5?
            if (NOT(this.animator.ModelHandle())) {
                // no, so nothing to do
                return;
            }

            // call any frame commands that have happened in the past frame
            if (!this.fl.hidden) {
                this.animator.ServiceAnims(gameLocal.previousTime, gameLocal.time);
            }

            // if the model is animating then we have to update it
            if (!this.animator.FrameHasChanged(gameLocal.time)) {
                // still fine the way it was
                return;
            }

            // get the latest frame bounds
            this.animator.GetBounds(gameLocal.time, this.renderEntity.bounds);
            if (this.renderEntity.bounds.IsCleared() && !this.fl.hidden) {
                gameLocal.DPrintf("%d: inside out bounds\n", gameLocal.time);
            }

            // update the renderEntity
            UpdateVisuals();

            // the animation is updated
            this.animator.ClearForceUpdate();
        }

        @Override
        public idAnimator GetAnimator() {
            return this.animator;
        }

        @Override
        public void SetModel(final String modelname) {
            FreeModelDef();

            this.renderEntity.hModel = this.animator.SetModel(modelname);
            if (NOT(this.renderEntity.hModel)) {
                super.SetModel(modelname);
                return;
            }

            if (null == this.renderEntity.customSkin) {
                this.renderEntity.customSkin = this.animator.ModelDef().GetDefaultSkin();
            }

            // set the callback to update the joints
            this.renderEntity.callback = idEntity.ModelCallback.getInstance();
            {
                final idJointMat[][] joints = {null};
                this.renderEntity.numJoints = this.animator.GetJoints(joints);
                this.renderEntity.joints = joints[0];
            }
            this.animator.GetBounds(gameLocal.time, this.renderEntity.bounds);

            UpdateVisuals();
        }

        public boolean GetJointWorldTransform(int/*jointHandle_t*/ jointHandle, int currentTime, idVec3 offset, idMat3 axis) {
            if (!this.animator.GetJointTransform(jointHandle, currentTime, offset, axis)) {
                return false;
            }

            ConvertLocalToWorldTransform(offset, axis);
            return true;
        }

        public boolean GetJointTransformForAnim(int/*jointHandle_t*/ jointHandle, int animNum, int frameTime, idVec3 offset, idMat3 axis) {
            idAnim anim;
            int numJoints;
            idJointMat[] frame;

            anim = this.animator.GetAnim(animNum);
            if (null == anim) {
                assert (false);
                return false;
            }

            numJoints = this.animator.NumJoints();
            if ((jointHandle < 0) || (jointHandle >= numJoints)) {
                assert (false);
                return false;
            }

            frame = Stream.generate(idJointMat::new).limit(numJoints).toArray(idJointMat[]::new);
            GameEdit.gameEdit.ANIM_CreateAnimFrame(this.animator.ModelHandle(), anim.MD5Anim(0), this.renderEntity.numJoints, frame, frameTime, this.animator.ModelDef().GetVisualOffset(), this.animator.RemoveOrigin());

            offset.oSet(frame[jointHandle].ToVec3());
            axis.oSet(frame[jointHandle].ToMat3());

            return true;
        }

        public int GetDefaultSurfaceType() {
            return etoi(SURFTYPE_METAL);
        }

        @Override
        public void AddDamageEffect(final trace_s collision, final idVec3 velocity, final String damageDefName) {
            String sound, decal, key;

            final idDeclEntityDef def = gameLocal.FindEntityDef(damageDefName, false);
            if (def == null) {
                return;
            }

            final String materialType = gameLocal.sufaceTypeNames[collision.c.material.GetSurfaceType().ordinal()];

            // start impact sound based on material type
            key = va("snd_%s", materialType);
            sound = this.spawnArgs.GetString(key);
            if ((sound == null) || sound.isEmpty()) {// == '\0' ) {
                sound = def.dict.GetString(key);
            }
            if ((sound != null) && !sound.isEmpty()) {// != '\0' ) {
                StartSoundShader(declManager.FindSound(sound), SND_CHANNEL_BODY, 0, false, null);
            }

            if (g_decals.GetBool()) {
                // place a wound overlay on the model
                key = va("mtr_wound_%s", materialType);
                decal = this.spawnArgs.RandomPrefix(key, gameLocal.random);
                if ((decal == null) || decal.isEmpty()) {// == '\0' ) {
                    decal = def.dict.RandomPrefix(key, gameLocal.random);
                }
                if ((decal != null) && !decal.isEmpty()) {// != '\0' ) {
                    final idVec3 dir = new idVec3(velocity);
                    dir.Normalize();
                    ProjectOverlay(collision.c.point, dir, 20.0f, decal);
                }
            }
        }

        public void AddLocalDamageEffect(int/*jointHandle_t*/ jointNum, final idVec3 localOrigin, final idVec3 localNormal, final idVec3 localDir, final idDeclEntityDef def, final idMaterial collisionMaterial) {
            String sound, splat, decal, bleed, key;
            damageEffect_s de;
            idVec3 origin, dir;
            idMat3 axis;

            axis = this.renderEntity.joints[jointNum].ToMat3().oMultiply(this.renderEntity.axis);
            origin = this.renderEntity.origin.oPlus(this.renderEntity.joints[jointNum].ToVec3().oMultiply(this.renderEntity.axis));

            origin = origin.oPlus(localOrigin.oMultiply(axis));
            dir = localDir.oMultiply(axis);

            int type = collisionMaterial.GetSurfaceType().ordinal();
            if (type == SURFTYPE_NONE.ordinal()) {
                type = GetDefaultSurfaceType();
            }

            final String materialType = gameLocal.sufaceTypeNames[type];

            // start impact sound based on material type
            key = va("snd_%s", materialType);
            sound = this.spawnArgs.GetString(key);
            if ((sound == null) || sound.isEmpty()) {// == '\0' ) {
                sound = def.dict.GetString(key);
            }
            if ((sound != null) && !sound.isEmpty()) {// != '\0' ) {
                StartSoundShader(declManager.FindSound(sound), SND_CHANNEL_BODY, 0, false, null);
            }

            // blood splats are thrown onto nearby surfaces
            key = va("mtr_splat_%s", materialType);
            splat = this.spawnArgs.RandomPrefix(key, gameLocal.random);
            if ((splat == null) || splat.isEmpty()) {// == '\0' ) {
                splat = def.dict.RandomPrefix(key, gameLocal.random);
            }
            if ((splat != null) && !splat.isEmpty()) {// 1= '\0' ) {
                gameLocal.BloodSplat(origin, dir, 64.0f, splat);
            }

            // can't see wounds on the player model in single player mode
            if (!(IsType(idPlayer.class) && !gameLocal.isMultiplayer)) {
                // place a wound overlay on the model
                key = va("mtr_wound_%s", materialType);
                decal = this.spawnArgs.RandomPrefix(key, gameLocal.random);
                if ((decal == null) || decal.isEmpty()) {// == '\0' ) {
                    decal = def.dict.RandomPrefix(key, gameLocal.random);
                }
                if ((decal != null) && !decal.isEmpty()) {// == '\0' ) {
                    ProjectOverlay(origin, dir, 20.0f, decal);
                }
            }

            // a blood spurting wound is added
            key = va("smoke_wound_%s", materialType);
            bleed = this.spawnArgs.GetString(key);
            if ((bleed == null) || bleed.isEmpty()) {// == '\0' ) {
                bleed = def.dict.GetString(key);
            }
            if ((bleed != null) && !bleed.isEmpty()) {// == '\0' ) {
                de = new damageEffect_s();
                de.next = this.damageEffects;
                this.damageEffects = de;

                de.jointNum = jointNum;
                de.localOrigin = new idVec3(localOrigin);
                de.localNormal = new idVec3(localNormal);
                de.type = (idDeclParticle) declManager.FindType(DECL_PARTICLE, bleed);
                de.time = gameLocal.time;
            }
        }

        public void UpdateDamageEffects() {
            damageEffect_s de;
            damageEffect_s prev;

            // free any that have timed out
            prev = this.damageEffects;
            while (prev != null) {
                de = prev;
                if (de.time == 0) {    // FIXME:SMOKE
                    this.damageEffects = de.next;
//			*prev = de.next;
//			delete de;
                } else {
                    prev = de.next;
                }
            }

            if (!g_bloodEffects.GetBool()) {
                return;
            }

            // emit a particle for each bleeding wound
            for (de = this.damageEffects; de != null; de = de.next) {
                idVec3 origin = new idVec3(), start;
                final idMat3 axis = new idMat3();

                this.animator.GetJointTransform(de.jointNum, gameLocal.time, origin, axis);
                axis.oMulSet(this.renderEntity.axis);
                origin = this.renderEntity.origin.oPlus(origin.oMultiply(this.renderEntity.axis));
                start = origin.oPlus(de.localOrigin.oMultiply(axis));
                if (!gameLocal.smokeParticles.EmitSmoke(de.type, de.time, gameLocal.random.CRandomFloat(), start, axis)) {
                    de.time = 0;
                }
            }
        }

        @Override
        public boolean ClientReceiveEvent(int event, int time, final idBitMsg msg) {
            int damageDefIndex;
            int materialIndex;
            int/*jointHandle_s*/ jointNum;
            final idVec3 localOrigin = new idVec3();
			idVec3 localNormal, localDir;

            switch (event) {
                case EVENT_ADD_DAMAGE_EFFECT: {
                    jointNum = /*(jointHandle_s)*/ msg.ReadShort();
                    localOrigin.oSet(0, msg.ReadFloat());
                    localOrigin.oSet(1, msg.ReadFloat());
                    localOrigin.oSet(2, msg.ReadFloat());
                    localNormal = msg.ReadDir(24);
                    localDir = msg.ReadDir(24);
                    damageDefIndex = gameLocal.ClientRemapDecl(DECL_ENTITYDEF, msg.ReadLong());
                    materialIndex = gameLocal.ClientRemapDecl(DECL_MATERIAL, msg.ReadLong());
                    final idDeclEntityDef damageDef = (idDeclEntityDef) declManager.DeclByIndex(DECL_ENTITYDEF, damageDefIndex);
                    final idMaterial collisionMaterial = (idMaterial) declManager.DeclByIndex(DECL_MATERIAL, materialIndex);
                    AddLocalDamageEffect(jointNum, localOrigin, localNormal, localDir, damageDef, collisionMaterial);
                    return true;
                }
                default: {
                    return super.ClientReceiveEvent(event, time, msg);
                }
            }
//            return false;
        }


        /*
         ================
         idAnimatedEntity::Event_GetJointHandle

         looks up the number of the specified joint.  returns INVALID_JOINT if the joint is not found.
         ================
         */
        private static void Event_GetJointHandle(idAnimatedEntity e, final idEventArg<String> jointname) {
//            jointHandle_t joint = new jointHandle_t();
            int joint;

            joint = e.animator.GetJointHandle(jointname.value);
            idThread.ReturnInt(joint);
        }

        /*
         ================
         idAnimatedEntity::Event_ClearAllJoints

         removes any custom transforms on all joints
         ================
         */
        private void Event_ClearAllJoints() {
            this.animator.ClearAllJoints();
        }

        /*
         ================
         idAnimatedEntity::Event_ClearJoint

         removes any custom transforms on the specified joint
         ================
         */
        private static void Event_ClearJoint(idAnimatedEntity e, idEventArg<Integer>/*jointHandle_t*/ jointnum) {
            e.animator.ClearJoint(jointnum.value);
        }

        /*
         ================
         idAnimatedEntity::Event_SetJointPos

         modifies the position of the joint based on the transform type
         ================
         */
        private static void Event_SetJointPos(idAnimatedEntity e, idEventArg<Integer>/*jointHandle_t*/ jointnum, idEventArg<jointModTransform_t> transform_type, final idEventArg<idVec3> pos) {
            e.animator.SetJointPos(jointnum.value, transform_type.value, pos.value);
        }

        /*
         ================
         idAnimatedEntity::Event_SetJointAngle

         modifies the orientation of the joint based on the transform type
         ================
         */
        private static void Event_SetJointAngle(idAnimatedEntity e, idEventArg<Integer>/*jointHandle_t*/ jointnum, idEventArg<Integer>/*jointModTransform_t*/ transform_type, final idEventArg<idVec3> angles) {
            idMat3 mat;

            mat = angles.value.ToMat3();
            e.animator.SetJointAxis(jointnum.value, jointModTransform_t.values()[transform_type.value], mat);
        }

        /*
         ================
         idAnimatedEntity::Event_GetJointPos

         returns the position of the joint in worldspace
         ================
         */
        private static void Event_GetJointPos(idAnimatedEntity e, idEventArg<Integer>/*jointHandle_t*/ jointnum) {
            final idVec3 offset = new idVec3();
            final idMat3 axis = new idMat3();

            if (!e.GetJointWorldTransform(jointnum.value, gameLocal.time, offset, axis)) {
                gameLocal.Warning("Joint # %d out of range on entity '%s'", jointnum, e.name);
            }

            idThread.ReturnVector(offset);
        }


        /*
         ================
         idAnimatedEntity::Event_GetJointAngle

         returns the orientation of the joint in worldspace
         ================
         */
        private static void Event_GetJointAngle(idAnimatedEntity e, idEventArg<Integer>/*jointHandle_t*/ jointnum) {
            final idVec3 offset = new idVec3();
            final idMat3 axis = new idMat3();

            if (!e.GetJointWorldTransform(jointnum.value, gameLocal.time, offset, axis)) {
                gameLocal.Warning("Joint # %d out of range on entity '%s'", jointnum, e.name);
            }

            final idAngles ang = axis.ToAngles();
            final idVec3 vec = new idVec3(ang.oGet(0), ang.oGet(1), ang.oGet(2));
            idThread.ReturnVector(vec);
        }

        /**
         * inherited grandfather functions.
         */
        public final void idEntity_Hide() {
            super.Hide();
        }

        public final void idEntity_Show() {
            super.Show();
        }

        public final impactInfo_s idEntity_GetImpactInfo(idEntity ent, int id, final idVec3 point) {
            return super.GetImpactInfo(ent, id, point);
        }

        public final void idEntity_ApplyImpulse(idEntity ent, int id, final idVec3 point, final idVec3 impulse) {
            super.ApplyImpulse(ent, id, point, impulse);
        }

        public final void idEntity_AddForce(idEntity ent, int id, final idVec3 point, final idVec3 force) {
            super.AddForce(ent, id, point, force);
        }

        public final boolean idEntity_GetPhysicsToVisualTransform(idVec3 origin, idMat3 axis) {
            return super.GetPhysicsToVisualTransform(origin, axis);
        }

        public final void idEntity_FreeModelDef() {
            super.FreeModelDef();
        }

        public final void idEntity_Present() {
            super.Present();
        }

        public final void idEntity_ProjectOverlay(final idVec3 origin, final idVec3 dir, float size, final String material) {
            super.ProjectOverlay(origin, dir, size, material);
        }

        public final boolean idEntity_ServerReceiveEvent(int event, int time, final idBitMsg msg) {
            return super.ServerReceiveEvent(event, time, msg);
        }

        @Override
        public eventCallback_t getEventCallBack(idEventDef event) {
            return eventCallbacks.get(event);
        }

        public static Map<idEventDef, eventCallback_t> getEventCallBacks() {
            return eventCallbacks;
        }

    }

    /*
     ================
     UpdateGuiParms
     ================
     */
    static void UpdateGuiParms(idUserInterface gui, final idDict args) {
        if ((gui == null) || (args == null)) {
            return;
        }
        idKeyValue kv = args.MatchPrefix("gui_parm", null);
        while (kv != null) {
            gui.SetStateString(kv.GetKey().getData(), kv.GetValue().getData());
            kv = args.MatchPrefix("gui_parm", kv);
        }
        gui.SetStateBool("noninteractive", args.GetBool("gui_noninteractive"));
        gui.StateChanged(gameLocal.time);
    }

    /*
     ================
     AddRenderGui
     ================
     */
    public static idUserInterface AddRenderGui(final String name, final idDict args) {
        idUserInterface gui;

        final idKeyValue kv = args.MatchPrefix("gui_parm", null);
        gui = uiManager.FindGui(name, true, (kv != null));
        UpdateGuiParms(gui, args);

        return gui;
    }

}
