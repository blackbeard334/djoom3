package neo.Game.Script;

import neo.CM.CollisionModel.trace_s;
import neo.Game.AFEntity.idAFEntity_Base;
import neo.Game.Camera.idCamera;
import static neo.Game.Entity.EV_Activate;
import neo.Game.Entity.idEntity;

import static neo.Game.Entity.EV_CacheSoundShader;
import static neo.Game.Entity.EV_SetShaderParm;
import static neo.Game.Entity.signalNum_t.NUM_SIGNALS;
import static neo.Game.Entity.signalNum_t.SIG_TRIGGER;
import neo.Game.GameSys.Class.eventCallback_t;
import neo.Game.GameSys.Class.eventCallback_t0;
import neo.Game.GameSys.Class.eventCallback_t1;
import neo.Game.GameSys.Class.eventCallback_t2;
import neo.Game.GameSys.Class.eventCallback_t3;
import neo.Game.GameSys.Class.eventCallback_t4;
import neo.Game.GameSys.Class.eventCallback_t5;
import neo.Game.GameSys.Class.eventCallback_t6;
import neo.Game.GameSys.Class.idClass;
import neo.Game.GameSys.Class.idEventArg;
import neo.Game.GameSys.Event.idEventDef;
import neo.Game.GameSys.SaveGame.idRestoreGame;
import neo.Game.GameSys.SaveGame.idSaveGame;

import static neo.Game.GameSys.Class.EV_Remove;
import static neo.Game.GameSys.SysCvar.g_debugScript;
import static neo.Game.Game_local.ENTITYNUM_NONE;
import static neo.Game.Game_local.MAX_GENTITIES;
import static neo.Game.Game_local.gameLocal;
import static neo.Game.Game_local.gameRenderWorld;
import static neo.Game.Game_local.gameSoundWorld;
import static neo.Game.Physics.Clip.CLIPMODEL_ID_TO_JOINT_HANDLE;
import neo.Game.Physics.Physics_AF.idAFBody;
import neo.Game.Player.idPlayer;
import neo.Game.Script.Script_Interpreter.idInterpreter;
import neo.Game.Script.Script_Program.function_t;
import static neo.Renderer.RenderWorld.MAX_GLOBAL_SHADER_PARMS;
import static neo.TempDump.btoi;
import static neo.TempDump.etoi;
import static neo.framework.CmdSystem.cmdExecution_t.CMD_EXEC_NOW;
import neo.framework.CmdSystem.cmdFunction_t;
import static neo.framework.CmdSystem.cmdSystem;
import static neo.framework.DeclManager.declManager;
import static neo.framework.UsercmdGen.USERCMD_HZ;
import neo.idlib.BV.Bounds.idBounds;
import neo.idlib.CmdArgs.idCmdArgs;
import neo.idlib.Dict_h.idDict;
import static neo.idlib.Lib.idLib.cvarSystem;
import neo.idlib.Text.Str.idStr;
import static neo.idlib.Text.Str.va;
import neo.idlib.containers.List.idList;
import neo.idlib.math.Angles.idAngles;
import static neo.idlib.math.Math_h.DEG2RAD;
import static neo.idlib.math.Math_h.MS2SEC;
import static neo.idlib.math.Math_h.SEC2MS;
import neo.idlib.math.Math_h.idMath;
import static neo.idlib.math.Vector.getVec3_origin;
import neo.idlib.math.Vector.idVec3;
import neo.idlib.math.Vector.idVec4;

import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class Script_Thread {

    static final idEventDef EV_Thread_Execute             = new idEventDef("<execute>", null);
    public static final idEventDef EV_Thread_SetCallback         = new idEventDef("<script_setcallback>", null);
    //
    // script callable events
    static final idEventDef EV_Thread_TerminateThread     = new idEventDef("terminate", "d");
    static final idEventDef EV_Thread_Pause               = new idEventDef("pause", null);
    public static final idEventDef EV_Thread_Wait                = new idEventDef("wait", "f");
    public static final idEventDef EV_Thread_WaitFrame           = new idEventDef("waitFrame");
    static final idEventDef EV_Thread_WaitFor             = new idEventDef("waitFor", "e");
    static final idEventDef EV_Thread_WaitForThread       = new idEventDef("waitForThread", "d");
    static final idEventDef EV_Thread_Print               = new idEventDef("print", "s");
    static final idEventDef EV_Thread_PrintLn             = new idEventDef("println", "s");
    static final idEventDef EV_Thread_Say                 = new idEventDef("say", "s");
    static final idEventDef EV_Thread_Assert              = new idEventDef("assert", "f");
    static final idEventDef EV_Thread_Trigger             = new idEventDef("trigger", "e");
    static final idEventDef EV_Thread_SetCvar             = new idEventDef("setcvar", "ss");
    static final idEventDef EV_Thread_GetCvar             = new idEventDef("getcvar", "s", 's');
    static final idEventDef EV_Thread_Random              = new idEventDef("random", "f", 'f');
    static final idEventDef EV_Thread_GetTime             = new idEventDef("getTime", null, 'f');
    static final idEventDef EV_Thread_KillThread          = new idEventDef("killthread", "s");
    static final idEventDef EV_Thread_SetThreadName       = new idEventDef("threadname", "s");
    static final idEventDef EV_Thread_GetEntity           = new idEventDef("getEntity", "s", 'e');
    static final idEventDef EV_Thread_Spawn               = new idEventDef("spawn", "s", 'e');
    static final idEventDef EV_Thread_CopySpawnArgs       = new idEventDef("copySpawnArgs", "e");
    static final idEventDef EV_Thread_SetSpawnArg         = new idEventDef("setSpawnArg", "ss");
    static final idEventDef EV_Thread_SpawnString         = new idEventDef("SpawnString", "ss", 's');
    static final idEventDef EV_Thread_SpawnFloat          = new idEventDef("SpawnFloat", "sf", 'f');
    static final idEventDef EV_Thread_SpawnVector         = new idEventDef("SpawnVector", "sv", 'v');
    static final idEventDef EV_Thread_ClearPersistantArgs = new idEventDef("clearPersistantArgs");
    static final idEventDef EV_Thread_SetPersistantArg    = new idEventDef("setPersistantArg", "ss");
    static final idEventDef EV_Thread_GetPersistantString = new idEventDef("getPersistantString", "s", 's');
    static final idEventDef EV_Thread_GetPersistantFloat  = new idEventDef("getPersistantFloat", "s", 'f');
    static final idEventDef EV_Thread_GetPersistantVector = new idEventDef("getPersistantVector", "s", 'v');
    static final idEventDef EV_Thread_AngToForward        = new idEventDef("angToForward", "v", 'v');
    static final idEventDef EV_Thread_AngToRight          = new idEventDef("angToRight", "v", 'v');
    static final idEventDef EV_Thread_AngToUp             = new idEventDef("angToUp", "v", 'v');
    static final idEventDef EV_Thread_Sine                = new idEventDef("sin", "f", 'f');
    static final idEventDef EV_Thread_Cosine              = new idEventDef("cos", "f", 'f');
    static final idEventDef EV_Thread_SquareRoot          = new idEventDef("sqrt", "f", 'f');
    static final idEventDef EV_Thread_Normalize           = new idEventDef("vecNormalize", "v", 'v');
    static final idEventDef EV_Thread_VecLength           = new idEventDef("vecLength", "v", 'f');
    static final idEventDef EV_Thread_VecDotProduct       = new idEventDef("DotProduct", "vv", 'f');
    static final idEventDef EV_Thread_VecCrossProduct     = new idEventDef("CrossProduct", "vv", 'v');
    static final idEventDef EV_Thread_VecToAngles         = new idEventDef("VecToAngles", "v", 'v');
    static final idEventDef EV_Thread_OnSignal            = new idEventDef("onSignal", "des");
    static final idEventDef EV_Thread_ClearSignal         = new idEventDef("clearSignalThread", "de");
    static final idEventDef EV_Thread_SetCamera           = new idEventDef("setCamera", "e");
    static final idEventDef EV_Thread_FirstPerson         = new idEventDef("firstPerson", null);
    static final idEventDef EV_Thread_Trace               = new idEventDef("trace", "vvvvde", 'f');
    static final idEventDef EV_Thread_TracePoint          = new idEventDef("tracePoint", "vvde", 'f');
    static final idEventDef EV_Thread_GetTraceFraction    = new idEventDef("getTraceFraction", null, 'f');
    static final idEventDef EV_Thread_GetTraceEndPos      = new idEventDef("getTraceEndPos", null, 'v');
    static final idEventDef EV_Thread_GetTraceNormal      = new idEventDef("getTraceNormal", null, 'v');
    static final idEventDef EV_Thread_GetTraceEntity      = new idEventDef("getTraceEntity", null, 'e');
    static final idEventDef EV_Thread_GetTraceJoint       = new idEventDef("getTraceJoint", null, 's');
    static final idEventDef EV_Thread_GetTraceBody        = new idEventDef("getTraceBody", null, 's');
    static final idEventDef EV_Thread_FadeIn              = new idEventDef("fadeIn", "vf");
    static final idEventDef EV_Thread_FadeOut             = new idEventDef("fadeOut", "vf");
    static final idEventDef EV_Thread_FadeTo              = new idEventDef("fadeTo", "vff");
    static final idEventDef EV_Thread_StartMusic          = new idEventDef("music", "s");
    static final idEventDef EV_Thread_Error               = new idEventDef("error", "s");
    static final idEventDef EV_Thread_Warning             = new idEventDef("warning", "s");
    static final idEventDef EV_Thread_StrLen              = new idEventDef("strLength", "s", 'd');
    static final idEventDef EV_Thread_StrLeft             = new idEventDef("strLeft", "sd", 's');
    static final idEventDef EV_Thread_StrRight            = new idEventDef("strRight", "sd", 's');
    static final idEventDef EV_Thread_StrSkip             = new idEventDef("strSkip", "sd", 's');
    static final idEventDef EV_Thread_StrMid              = new idEventDef("strMid", "sdd", 's');
    static final idEventDef EV_Thread_StrToFloat          = new idEventDef("strToFloat", "s", 'f');
    static final idEventDef EV_Thread_RadiusDamage        = new idEventDef("radiusDamage", "vEEEsf");
    static final idEventDef EV_Thread_IsClient            = new idEventDef("isClient", null, 'f');
    static final idEventDef EV_Thread_IsMultiplayer       = new idEventDef("isMultiplayer", null, 'f');
    static final idEventDef EV_Thread_GetFrameTime        = new idEventDef("getFrameTime", null, 'f');
    static final idEventDef EV_Thread_GetTicsPerSecond    = new idEventDef("getTicsPerSecond", null, 'f');
    static final idEventDef EV_Thread_DebugLine           = new idEventDef("debugLine", "vvvf");
    static final idEventDef EV_Thread_DebugArrow          = new idEventDef("debugArrow", "vvvdf");
    static final idEventDef EV_Thread_DebugCircle         = new idEventDef("debugCircle", "vvvfdf");
    static final idEventDef EV_Thread_DebugBounds         = new idEventDef("debugBounds", "vvvf");
    static final idEventDef EV_Thread_DrawText            = new idEventDef("drawText", "svfvdf");
    static final idEventDef EV_Thread_InfluenceActive     = new idEventDef("influenceActive", null, 'd');

    public static class idThread extends idClass {
        public static final int BYTES = Integer.BYTES * 14;//TODO

        protected static Map<idEventDef, eventCallback_t> eventCallbacks = new HashMap<>();
        static {
            eventCallbacks.put(EV_Thread_Execute, (eventCallback_t0<idThread>)  idThread::Event_Execute);
            eventCallbacks.put(EV_Thread_TerminateThread, (eventCallback_t1<idThread>) idThread::Event_TerminateThread);
            eventCallbacks.put(EV_Thread_Pause, (eventCallback_t0<idThread>) idThread::Event_Pause);
            eventCallbacks.put(EV_Thread_Wait, (eventCallback_t1<idThread>) idThread::Event_Wait);
            eventCallbacks.put(EV_Thread_WaitFrame, (eventCallback_t0<idThread>) idThread::Event_WaitFrame);
            eventCallbacks.put(EV_Thread_WaitFor, (eventCallback_t1<idThread>) idThread::Event_WaitFor);
            eventCallbacks.put(EV_Thread_WaitForThread, (eventCallback_t1<idThread>) idThread::Event_WaitForThread);
            eventCallbacks.put(EV_Thread_Print, (eventCallback_t1<idThread>) idThread::Event_Print);
            eventCallbacks.put(EV_Thread_PrintLn, (eventCallback_t1<idThread>) idThread::Event_PrintLn);
            eventCallbacks.put(EV_Thread_Say, (eventCallback_t1<idThread>) idThread::Event_Say);
            eventCallbacks.put(EV_Thread_Assert, (eventCallback_t1<idThread>) idThread::Event_Assert);
            eventCallbacks.put(EV_Thread_Trigger, (eventCallback_t1<idThread>) idThread::Event_Trigger);
            eventCallbacks.put(EV_Thread_SetCvar, (eventCallback_t2<idThread>) idThread::Event_SetCvar);
            eventCallbacks.put(EV_Thread_GetCvar, (eventCallback_t1<idThread>) idThread::Event_GetCvar);
            eventCallbacks.put(EV_Thread_Random, (eventCallback_t1<idThread>) idThread::Event_Random);
            eventCallbacks.put(EV_Thread_GetTime, (eventCallback_t0<idThread>) idThread::Event_GetTime);
            eventCallbacks.put(EV_Thread_KillThread, (eventCallback_t1<idThread>) idThread::Event_KillThread);
            eventCallbacks.put(EV_Thread_SetThreadName, (eventCallback_t1<idThread>) idThread::Event_SetThreadName);
            eventCallbacks.put(EV_Thread_GetEntity, (eventCallback_t1<idThread>) idThread::Event_GetEntity);
            eventCallbacks.put(EV_Thread_Spawn, (eventCallback_t1<idThread>) idThread::Event_Spawn);
            eventCallbacks.put(EV_Thread_CopySpawnArgs, (eventCallback_t1<idThread>) idThread::Event_CopySpawnArgs);
            eventCallbacks.put(EV_Thread_SetSpawnArg, (eventCallback_t2<idThread>) idThread::Event_SetSpawnArg);
            eventCallbacks.put(EV_Thread_SpawnString, (eventCallback_t2<idThread>) idThread::Event_SpawnString);
            eventCallbacks.put(EV_Thread_SpawnFloat, (eventCallback_t2<idThread>) idThread::Event_SpawnFloat);
            eventCallbacks.put(EV_Thread_SpawnVector, (eventCallback_t2<idThread>) idThread::Event_SpawnVector);
            eventCallbacks.put(EV_Thread_ClearPersistantArgs, (eventCallback_t0<idThread>) idThread::Event_ClearPersistantArgs);
            eventCallbacks.put(EV_Thread_SetPersistantArg, (eventCallback_t2<idThread>) idThread::Event_SetPersistantArg);
            eventCallbacks.put(EV_Thread_GetPersistantString, (eventCallback_t1<idThread>) idThread::Event_GetPersistantString);
            eventCallbacks.put(EV_Thread_GetPersistantFloat, (eventCallback_t1<idThread>) idThread::Event_GetPersistantFloat);
            eventCallbacks.put(EV_Thread_GetPersistantVector, (eventCallback_t1<idThread>) idThread::Event_GetPersistantVector);
            eventCallbacks.put(EV_Thread_AngToForward, (eventCallback_t1<idThread>) idThread::Event_AngToForward);
            eventCallbacks.put(EV_Thread_AngToRight, (eventCallback_t1<idThread>) idThread::Event_AngToRight);
            eventCallbacks.put(EV_Thread_AngToUp, (eventCallback_t1<idThread>) idThread::Event_AngToUp);
            eventCallbacks.put(EV_Thread_Sine, (eventCallback_t1<idThread>) idThread::Event_GetSine);
            eventCallbacks.put(EV_Thread_Cosine, (eventCallback_t1<idThread>) idThread::Event_GetCosine);
            eventCallbacks.put(EV_Thread_SquareRoot, (eventCallback_t1<idThread>) idThread::Event_GetSquareRoot);
            eventCallbacks.put(EV_Thread_Normalize, (eventCallback_t1<idThread>) idThread::Event_VecNormalize);
            eventCallbacks.put(EV_Thread_VecLength, (eventCallback_t1<idThread>) idThread::Event_VecLength);
            eventCallbacks.put(EV_Thread_VecDotProduct, (eventCallback_t2<idThread>) idThread::Event_VecDotProduct);
            eventCallbacks.put(EV_Thread_VecCrossProduct, (eventCallback_t2<idThread>) idThread::Event_VecCrossProduct);
            eventCallbacks.put(EV_Thread_VecToAngles, (eventCallback_t1<idThread>) idThread::Event_VecToAngles);
            eventCallbacks.put(EV_Thread_OnSignal, (eventCallback_t3<idThread>) idThread::Event_OnSignal);
            eventCallbacks.put(EV_Thread_ClearSignal, (eventCallback_t2<idThread>) idThread::Event_ClearSignalThread);
            eventCallbacks.put(EV_Thread_SetCamera, (eventCallback_t1<idThread>) idThread::Event_SetCamera);
            eventCallbacks.put(EV_Thread_FirstPerson, (eventCallback_t0<idThread>) idThread::Event_FirstPerson);
            eventCallbacks.put(EV_Thread_Trace, (eventCallback_t6<idThread>) idThread::Event_Trace);
            eventCallbacks.put(EV_Thread_TracePoint, (eventCallback_t4<idThread>) idThread::Event_TracePoint);
            eventCallbacks.put(EV_Thread_GetTraceFraction, (eventCallback_t0<idThread>) idThread::Event_GetTraceFraction);
            eventCallbacks.put(EV_Thread_GetTraceEndPos, (eventCallback_t0<idThread>) idThread::Event_GetTraceEndPos);
            eventCallbacks.put(EV_Thread_GetTraceNormal, (eventCallback_t0<idThread>) idThread::Event_GetTraceNormal);
            eventCallbacks.put(EV_Thread_GetTraceEntity, (eventCallback_t0<idThread>) idThread::Event_GetTraceEntity);
            eventCallbacks.put(EV_Thread_GetTraceJoint, (eventCallback_t0<idThread>) idThread::Event_GetTraceJoint);
            eventCallbacks.put(EV_Thread_GetTraceBody, (eventCallback_t0<idThread>) idThread::Event_GetTraceBody);
            eventCallbacks.put(EV_Thread_FadeIn, (eventCallback_t2<idThread>) idThread::Event_FadeIn);
            eventCallbacks.put(EV_Thread_FadeOut, (eventCallback_t2<idThread>) idThread::Event_FadeOut);
            eventCallbacks.put(EV_Thread_FadeTo, (eventCallback_t3<idThread>) idThread::Event_FadeTo);
            eventCallbacks.put(EV_SetShaderParm, (eventCallback_t2<idThread>) idThread::Event_SetShaderParm);
            eventCallbacks.put(EV_Thread_StartMusic, (eventCallback_t1<idThread>) idThread::Event_StartMusic);
            eventCallbacks.put(EV_Thread_Warning, (eventCallback_t1<idThread>) idThread::Event_Warning);
            eventCallbacks.put(EV_Thread_Error, (eventCallback_t1<idThread>) idThread::Event_Error);
            eventCallbacks.put(EV_Thread_StrLen, (eventCallback_t1<idThread>) idThread::Event_StrLen);
            eventCallbacks.put(EV_Thread_StrLeft, (eventCallback_t2<idThread>) idThread::Event_StrLeft);
            eventCallbacks.put(EV_Thread_StrRight, (eventCallback_t2<idThread>) idThread::Event_StrRight);
            eventCallbacks.put(EV_Thread_StrSkip, (eventCallback_t2<idThread>) idThread::Event_StrSkip);
            eventCallbacks.put(EV_Thread_StrMid, (eventCallback_t3<idThread>) idThread::Event_StrMid);
            eventCallbacks.put(EV_Thread_StrToFloat, (eventCallback_t1<idThread>) idThread::Event_StrToFloat);
            eventCallbacks.put(EV_Thread_RadiusDamage, (eventCallback_t6<idThread>) idThread::Event_RadiusDamage);
            eventCallbacks.put(EV_Thread_IsClient, (eventCallback_t0<idThread>) idThread::Event_IsClient);
            eventCallbacks.put(EV_Thread_IsMultiplayer, (eventCallback_t0<idThread>) idThread::Event_IsMultiplayer);
            eventCallbacks.put(EV_Thread_GetFrameTime, (eventCallback_t0<idThread>) idThread::Event_GetFrameTime);
            eventCallbacks.put(EV_Thread_GetTicsPerSecond, (eventCallback_t0<idThread>) idThread::Event_GetTicsPerSecond);
            eventCallbacks.put(EV_CacheSoundShader, (eventCallback_t1<idThread>) idThread::Event_CacheSoundShader);
            eventCallbacks.put(EV_Thread_DebugLine, (eventCallback_t4<idThread>) idThread::Event_DebugLine);
            eventCallbacks.put(EV_Thread_DebugArrow, (eventCallback_t5<idThread>) idThread::Event_DebugArrow);
            eventCallbacks.put(EV_Thread_DebugCircle, (eventCallback_t6<idThread>) idThread::Event_DebugCircle);
            eventCallbacks.put(EV_Thread_DebugBounds, (eventCallback_t4<idThread>) idThread::Event_DebugBounds);
            eventCallbacks.put(EV_Thread_DrawText, (eventCallback_t6<idThread>) idThread::Event_DrawText);
            eventCallbacks.put(EV_Thread_InfluenceActive, (eventCallback_t0<idThread>) idThread::Event_InfluenceActive);
        }
//        // CLASS_PROTOTYPE( idThread );
//        public static final idTypeInfo Type = new idTypeInfo(null, null, eventCallbacks, null, null, null, null);
        //
        //
        private static idThread      currentThread;
        //
        private        idThread      waitingForThread;
        private        int           waitingFor;
        private        int           waitingUntil;
        private        idInterpreter interpreter = new idInterpreter();
        //
        private        idDict        spawnArgs;
        //
        private        int           threadNum;
        private        idStr         threadName = new idStr();
        //
        private        int           lastExecuteTime;
        private        int           creationTime;
        //
        private        boolean       manualControl;
        //
        private static int           threadIndex;
        private static idList<idThread> threadList = new idList<>();
        //
        private static trace_s          trace      = new trace_s();
//
//

        @Override
        public void Init() {
            // create a unique threadNum
            do {
                threadIndex++;
                if (threadIndex == 0) {
                    threadIndex = 1;
                }
            } while (GetThread(threadIndex) != null);

            threadNum = threadIndex;
            threadList.Append(this);

            creationTime = gameLocal.time;
            lastExecuteTime = 0;
            manualControl = false;

            ClearWaitFor();

            interpreter.SetThread(this);
        }

        private void Pause() {
            ClearWaitFor();
            interpreter.doneProcessing = true;
        }

        private void Event_Execute() {
            Execute();
        }

        private static void Event_SetThreadName(idThread t, final idEventArg<String> name) {
            t.SetThreadName(name.value);
        }

        //
        // script callable Events
        //
        private static void Event_TerminateThread(idThread t, idEventArg<Integer> num) {
            idThread thread;

            thread = t.GetThread(num.value);
            t.KillThread(num.value);
        }

        private void Event_Pause() {
            Pause();
        }

        private static void Event_Wait(idThread t, idEventArg<Float> time) {
            t.WaitSec(time.value);
        }

        private void Event_WaitFrame() {
            WaitFrame();
        }

        private static void Event_WaitFor(idThread t, idEventArg<idEntity> e) {
            idEntity ent = e.value;
            if (ent != null && ent.RespondsTo(EV_Thread_SetCallback)) {
                ent.ProcessEvent(EV_Thread_SetCallback);
                if (gameLocal.program.GetReturnedInteger() != 0) {
                    t.Pause();
                    t.waitingFor = ent.entityNumber;
                }
            }
        }

        private static void Event_WaitForThread(idThread t, idEventArg<Integer> num) {
            idThread thread;

            thread = GetThread(num.value);
            if (null == thread) {
                if (g_debugScript.GetBool()) {
                    // just print a warning and continue executing
                    t.Warning("Thread %d not running", num.value);
                }
            } else {
                t.Pause();
                t.waitingForThread = thread;
            }
        }

        private static void Event_Print(idThread t, final idEventArg<String> text) {
            gameLocal.Printf("%s", text.value);
        }

        private static void Event_PrintLn(idThread t, final idEventArg<String> text) {
            gameLocal.Printf("%s\n", text.value);
        }

        private static void Event_Say(idThread t, final idEventArg<String> text) {
            cmdSystem.BufferCommandText(CMD_EXEC_NOW, va("say \"%s\"", text.value));
        }

        private static void Event_Assert(idThread t, idEventArg<Float> value) {
            assert (value.value != 0);
        }

        private static void Event_Trigger(idThread t, idEventArg<idEntity> e) {
            idEntity ent = e.value;
            if (ent != null) {
                ent.Signal(SIG_TRIGGER);
                ent.ProcessEvent(EV_Activate, gameLocal.GetLocalPlayer());
                ent.TriggerGuis();
            }
        }

        private static void Event_SetCvar(idThread t, final idEventArg<String> name, final idEventArg<String> value) {
            cvarSystem.SetCVarString(name.value, value.value);
        }

        private static void Event_GetCvar(idThread t, final idEventArg<String> name) {
            ReturnString(cvarSystem.GetCVarString(name.value));
        }

        private static void Event_Random(idThread t, idEventArg<Float> range) {
            float result;

            result = gameLocal.random.RandomFloat();
            ReturnFloat(range.value * result);
        }

        private void Event_GetTime() {
            ReturnFloat(MS2SEC(gameLocal.realClientTime));
        }

        private static void Event_KillThread(idThread t, final idEventArg<String> name) {
            KillThread(name.value);
        }

        private static void Event_GetEntity(idThread t, final idEventArg<String> n) {
            int entnum;
            idEntity ent;
            String name = n.value;

            assert (name != null);

            if (name.charAt(0) == '*') {
                entnum = Integer.parseInt(name.substring(1));
                if ((entnum < 0) || (entnum >= MAX_GENTITIES)) {
                    t.Error("Entity number in string out of range.");
                }
                ReturnEntity(gameLocal.entities[entnum]);
            } else {
                ent = gameLocal.FindEntity(name);
                ReturnEntity(ent);
            }
        }

        private static void Event_Spawn(idThread t, final idEventArg<String> classname) {
            idEntity[] ent = {null};

            t.spawnArgs.Set("classname", classname.value);
            gameLocal.SpawnEntityDef(t.spawnArgs, ent);
            ReturnEntity(ent[0]);
            t.spawnArgs.Clear();
        }

        private static void Event_CopySpawnArgs(idThread t, idEventArg<idEntity> ent) {
            t.spawnArgs.Copy(ent.value.spawnArgs);
        }

        private static void Event_SetSpawnArg(idThread t, final idEventArg<String> key, final idEventArg<String> value) {
            t.spawnArgs.Set(key.value, value.value);
        }

        private static void Event_SpawnString(idThread t, final idEventArg<String> key, final idEventArg<String> defaultvalue) {
            String[] result = {null};

            t.spawnArgs.GetString(key.value, defaultvalue.value, result);
            ReturnString(result[0]);
        }

        private static void Event_SpawnFloat(idThread t, final idEventArg<String> key, idEventArg<Float> defaultvalue) {
            float[] result = {0};

            t.spawnArgs.GetFloat(key.value, va("%f", defaultvalue.value), result);
            ReturnFloat(result[0]);
        }

        private static void Event_SpawnVector(idThread t, final idEventArg<String> key, idEventArg<idVec3> d) {
            idVec3 result = new idVec3();
            idVec3 defaultvalue = d.value;

            t.spawnArgs.GetVector(key.value, va("%f %f %f", defaultvalue.x, defaultvalue.y, defaultvalue.z), result);
            ReturnVector(result);
        }

        private void Event_ClearPersistantArgs() {
            gameLocal.persistentLevelInfo.Clear();
        }

        private static void Event_SetPersistantArg(idThread t, final idEventArg<String> key, final idEventArg<String> value) {
            gameLocal.persistentLevelInfo.Set(key.value, value.value);
        }

        private static void Event_GetPersistantString(idThread t, final idEventArg<String> key) {
            String[] result = {null};

            gameLocal.persistentLevelInfo.GetString(key.value, "", result);
            ReturnString(result[0]);
        }

        private static void Event_GetPersistantFloat(idThread t, final idEventArg<String> key) {
            float[] result = {0};

            gameLocal.persistentLevelInfo.GetFloat(key.value, "0", result);
            ReturnFloat(result[0]);
        }

        private static void Event_GetPersistantVector(idThread t, final idEventArg<String> key) {
            idVec3 result = new idVec3();

            gameLocal.persistentLevelInfo.GetVector(key.value, "0 0 0", result);
            ReturnVector(result);
        }

        private static void Event_AngToForward(idThread t, idEventArg<idAngles> ang) {
            ReturnVector(ang.value.ToForward());
        }

        private static void Event_AngToRight(idThread t, idEventArg<idAngles> ang) {
            idVec3 vec = new idVec3();

            ang.value.ToVectors(null, vec);
            ReturnVector(vec);
        }

        private static void Event_AngToUp(idThread t, idEventArg<idAngles> ang) {
            idVec3 vec = new idVec3();

            ang.value.ToVectors(null, null, vec);
            ReturnVector(vec);
        }

        private static void Event_GetSine(idThread t, idEventArg<Float> angle) {
            ReturnFloat(idMath.Sin(DEG2RAD(angle.value)));
        }

        private static void Event_GetCosine(idThread t, idEventArg<Float> angle) {
            ReturnFloat(idMath.Cos(DEG2RAD(angle.value)));
        }

        private static void Event_GetSquareRoot(idThread t, idEventArg<Float> theSquare) {
            ReturnFloat(idMath.Sqrt(theSquare.value));
        }

        private static void Event_VecNormalize(idThread t, idEventArg<idVec3> vec) {
            idVec3 n;

            n = vec.value;
            n.Normalize();
            ReturnVector(n);
        }

        private static void Event_VecLength(idThread t, idEventArg<idVec3> vec) {
            ReturnFloat(vec.value.Length());
        }

        private static void Event_VecDotProduct(idThread t, idEventArg<idVec3> vec1, idEventArg<idVec3> vec2) {
            ReturnFloat(vec1.value.oMultiply(vec2.value));
        }

        private static void Event_VecCrossProduct(idThread t, idEventArg<idVec3> vec1, idEventArg<idVec3> vec2) {
            ReturnVector(vec1.value.Cross(vec2.value));
        }

        private static void Event_VecToAngles(idThread t, idEventArg<idVec3> vec) {
            idAngles ang = vec.value.ToAngles();
            ReturnVector(new idVec3(ang.oGet(0), ang.oGet(1), ang.oGet(2)));
        }

        private static void Event_OnSignal(idThread t, idEventArg<Integer> s, idEventArg<idEntity> e, final idEventArg<String> f) {
            function_t function;
            int signal = s.value;
            idEntity ent = e.value;
            String func = f.value;

            assert (func != null);

            if (null == ent) {
                t.Error("Entity not found");
            }

            if ((signal < 0) || (signal >= etoi(NUM_SIGNALS))) {
                t.Error("Signal out of range");
            }

            function = gameLocal.program.FindFunction(func);
            if (null == function) {
                t.Error("Function '%s' not found", func);
            }

            ent.SetSignal(signal, t, function);
        }

        private static void Event_ClearSignalThread(idThread t, idEventArg<Integer> s, idEventArg<idEntity> e) {
            int signal = s.value;
            idEntity ent = e.value;

            if (null == ent) {
                t.Error("Entity not found");
            }

            if ((signal < 0) || (signal >= etoi(NUM_SIGNALS))) {
                t.Error("Signal out of range");
            }

            ent.ClearSignalThread(signal, t);
        }

        private static void Event_SetCamera(idThread t, idEventArg<idEntity> e) {
            idEntity ent = e.value;

            if (null == ent) {
                t.Error("Entity not found");
                return;
            }

            if (!ent.IsType(idCamera.class)) {
                t.Error("Entity is not a camera");
                return;
            }

            gameLocal.SetCamera((idCamera) ent);
        }

        private void Event_FirstPerson() {
            gameLocal.SetCamera(null);
        }

        private static void Event_Trace(idThread t, final idEventArg<idVec3> s, final idEventArg<idVec3> e, final idEventArg<idVec3> mi,
                                        final idEventArg<idVec3> ma, idEventArg<Integer> c, idEventArg<idEntity> p) {
            idVec3 start = s.value;
            idVec3 end = e.value;
            idVec3 mins = mi.value;
            idVec3 maxs = ma.value;
            int contents_mask = c.value;
            idEntity passEntity = p.value;

            {
                trace_s[] trace = {t.trace};
                if (mins.equals(getVec3_origin()) && maxs.equals(getVec3_origin())) {
                    gameLocal.clip.TracePoint(trace, start, end, contents_mask, passEntity);
                } else {
                    gameLocal.clip.TraceBounds(trace, start, end, new idBounds(mins, maxs), contents_mask, passEntity);
                }
                t.trace = trace[0];
            }
            ReturnFloat(trace.fraction);
        }

        private static void Event_TracePoint(idThread t, final idEventArg<idVec3> startA, final idEventArg<idVec3> endA, idEventArg<Integer> c, idEventArg<idEntity> p) {
            idVec3 start = startA.value;
            idVec3 end = endA.value;
            int contents_mask = c.value;
            idEntity passEntity = p.value;
            {
                trace_s[] trace = {t.trace};
                gameLocal.clip.TracePoint(trace, start, end, contents_mask, passEntity);
                t.trace = trace[0];
            }
            ReturnFloat(trace.fraction);
        }

        private void Event_GetTraceFraction() {
            ReturnFloat(trace.fraction);
        }

        private void Event_GetTraceEndPos() {
            ReturnVector(trace.endpos);
        }

        private void Event_GetTraceNormal() {
            if (trace.fraction < 1.0f) {
                ReturnVector(trace.c.normal);
            } else {
                ReturnVector(getVec3_origin());
            }
        }

        private void Event_GetTraceEntity() {
            if (trace.fraction < 1.0f) {
                ReturnEntity(gameLocal.entities[ trace.c.entityNum]);
            } else {
                ReturnEntity((idEntity) null);
            }
        }

        private void Event_GetTraceJoint() {
            if (trace.fraction < 1.0f && trace.c.id < 0) {
                idAFEntity_Base af = (idAFEntity_Base) gameLocal.entities[trace.c.entityNum];
                if (af != null && af.IsType(idAFEntity_Base.class) && af.IsActiveAF()) {
                    ReturnString(af.GetAnimator().GetJointName(CLIPMODEL_ID_TO_JOINT_HANDLE(trace.c.id)));
                    return;
                }
            }
            ReturnString("");
        }

        private void Event_GetTraceBody() {
            if (trace.fraction < 1.0f && trace.c.id < 0) {
                idAFEntity_Base af = (idAFEntity_Base) gameLocal.entities[ trace.c.entityNum];
                if (af != null && af.IsType(idAFEntity_Base.class) && af.IsActiveAF()) {
                    int bodyId = af.BodyForClipModelId(trace.c.id);
                    idAFBody body = af.GetAFPhysics().GetBody(bodyId);
                    if (body != null) {
                        ReturnString(body.GetName());
                        return;
                    }
                }
            }
            ReturnString("");
        }

        private static void Event_FadeIn(idThread t, idEventArg<idVec3> colorA, idEventArg<Float> time) {
            idVec4 fadeColor = new idVec4();
            idPlayer player;
            idVec3 color = colorA.value;

            player = gameLocal.GetLocalPlayer();
            if (player != null) {
                fadeColor.Set(color.oGet(0), color.oGet(1), color.oGet(2), 0.0f);
                player.playerView.Fade(fadeColor, (int) SEC2MS(time.value));
            }
        }

        private static void Event_FadeOut(idThread t, idEventArg<idVec3> colorA, idEventArg<Float> time) {
            idVec4 fadeColor = new idVec4();
            idPlayer player;
            idVec3 color = colorA.value;

            player = gameLocal.GetLocalPlayer();
            if (player != null) {
                fadeColor.Set(color.oGet(0), color.oGet(1), color.oGet(2), 1.0f);
                player.playerView.Fade(fadeColor, (int) SEC2MS(time.value));
            }
        }

        private static void Event_FadeTo(idThread t, idEventArg<idVec3> colorA, idEventArg<Float> alpha, idEventArg<Float> time) {
            idVec4 fadeColor = new idVec4();
            idPlayer player;
            idVec3 color = colorA.value;

            player = gameLocal.GetLocalPlayer();
            if (player != null) {
                fadeColor.Set(color.oGet(0), color.oGet(1), color.oGet(2), alpha.value);
                player.playerView.Fade(fadeColor, (int) SEC2MS(time.value));
            }
        }

        private static void Event_SetShaderParm(idThread t, idEventArg<Integer> parmnumA, idEventArg<Float> value) {
            int parmnum = parmnumA.value;

            if ((parmnum < 0) || (parmnum >= MAX_GLOBAL_SHADER_PARMS)) {
                t.Error("shader parm index (%d) out of range", parmnum);
            }

            gameLocal.globalShaderParms[parmnum] = value.value;
        }

        private static void Event_StartMusic(idThread t, final idEventArg<String> text) {
            gameSoundWorld.PlayShaderDirectly(text.value);
        }

        private static void Event_Warning(idThread t, final idEventArg<String> text) {
            t.Warning("%s", text.value);
        }

        private static void Event_Error(idThread t, final idEventArg<String> text) {
            t.Error("%s", text.value);
        }

        private static void Event_StrLen(idThread t, final idEventArg<String> string) {
            int len;

            len = string.value.length();
            idThread.ReturnInt(len);
        }

        private static void Event_StrLeft(idThread t, final idEventArg<String> stringA, idEventArg<Integer> numA) {
            int len;
            String string = stringA.value;
            int num = numA.value;

            if (num < 0) {
                idThread.ReturnString("");
                return;
            }

            len = string.length();
            if (len < num) {
                idThread.ReturnString(string);
                return;
            }

            idStr result = new idStr(string, 0, num);
            idThread.ReturnString(result);
        }

        private static void Event_StrRight(idThread t, final idEventArg<String> stringA, idEventArg<Integer> numA) {
            int len;
            String string = stringA.value;
            int num = numA.value;

            if (num < 0) {
                idThread.ReturnString("");
                return;
            }

            len = string.length();
            if (len < num) {
                idThread.ReturnString(string);
                return;
            }

            idThread.ReturnString(string + (len - num));
        }

        private static void Event_StrSkip(idThread t, final idEventArg<String> stringA, idEventArg<Integer> numA) {
            int len;
            String string = stringA.value;
            int num = numA.value;

            if (num < 0) {
                idThread.ReturnString(string);
                return;
            }

            len = string.length();
            if (len < num) {
                idThread.ReturnString("");
                return;
            }

            idThread.ReturnString(string + num);
        }

        private static void Event_StrMid(idThread t, final idEventArg<String> stringA, idEventArg<Integer> startA, idEventArg<Integer> numA) {
            int len;
            String string = stringA.value;
            int start = startA.value;
            int num = numA.value;

            if (num < 0) {
                idThread.ReturnString("");
                return;
            }

            if (start < 0) {
                start = 0;
            }
            len = string.length();
            if (start > len) {
                start = len;
            }

            if (start + num > len) {
                num = len - start;
            }

            idStr result = new idStr(string, start, start + num);
            idThread.ReturnString(result);
        }

        private static void Event_StrToFloat(idThread t, final idEventArg<String> string) {
            float result;

            result = Float.parseFloat(string.value);
            idThread.ReturnFloat(result);
        }

        private static void Event_RadiusDamage(idThread t, final idEventArg<idVec3> origin, idEventArg<idEntity> inflictor, idEventArg<idEntity> attacker,
                                               idEventArg<idEntity> ignore, final idEventArg<String> damageDefName, idEventArg<Float> dmgPower) {
            gameLocal.RadiusDamage(origin.value, inflictor.value, attacker.value, ignore.value, ignore.value, damageDefName.value, dmgPower.value);
        }

        private void Event_IsClient() {
            idThread.ReturnFloat(btoi(gameLocal.isClient));
        }

        private void Event_IsMultiplayer() {
            idThread.ReturnFloat(btoi(gameLocal.isMultiplayer));
        }

        private void Event_GetFrameTime() {
            idThread.ReturnFloat(MS2SEC(gameLocal.msec));
        }

        private void Event_GetTicsPerSecond() {
            idThread.ReturnFloat(USERCMD_HZ);
        }

        private static void Event_CacheSoundShader(idThread t, final idEventArg<String> soundName) {
            declManager.FindSound(soundName.value);
        }

        private static void Event_DebugLine(idThread t, final idEventArg<idVec3> colorA, final idEventArg<idVec3> start, final idEventArg<idVec3> end, final idEventArg<Float> lifetime) {
            idVec3 color = colorA.value;
            gameRenderWorld.DebugLine(new idVec4(color.x, color.y, color.z, 0.0f), start.value, end.value, (int) SEC2MS(lifetime.value));
        }

        private static void Event_DebugArrow(idThread t, final idEventArg<idVec3> colorA, final idEventArg<idVec3> start, final idEventArg<idVec3> end, final idEventArg<Integer> size, final idEventArg<Float> lifetime) {
            idVec3 color = colorA.value;
            gameRenderWorld.DebugArrow(new idVec4(color.x, color.y, color.z, 0.0f), start.value, end.value, size.value, (int) SEC2MS(lifetime.value));
        }

        private static void Event_DebugCircle(idThread t, final idEventArg<idVec3> colorA, final idEventArg<idVec3> origin, final idEventArg<idVec3> dir, final idEventArg<Float> radius, final idEventArg<Integer> numSteps, final idEventArg<Float> lifetime) {
            idVec3 color = colorA.value;
            gameRenderWorld.DebugCircle(new idVec4(color.x, color.y, color.z, 0.0f), origin.value, dir.value, radius.value, numSteps.value, (int) SEC2MS(lifetime.value));
        }

        private static void Event_DebugBounds(idThread t, final idEventArg<idVec3> colorA, final idEventArg<idVec3> mins, final idEventArg<idVec3> maxs, final idEventArg<Float> lifetime) {
            idVec3 color = colorA.value;
            gameRenderWorld.DebugBounds(new idVec4(color.x, color.y, color.z, 0.0f), new idBounds(mins.value, maxs.value), getVec3_origin(), (int) SEC2MS(lifetime.value));
        }

        private static void Event_DrawText(idThread t, final idEventArg<String> text, final idEventArg<idVec3> origin, idEventArg<Float> scale, final idEventArg<idVec3> colorA, final idEventArg<Integer> align, final idEventArg<Float> lifetime) {
            idVec3 color = colorA.value;
            gameRenderWorld.DrawText(text.value, origin.value, scale.value, new idVec4(color.x, color.y, color.z, 0.0f), gameLocal.GetLocalPlayer().viewAngles.ToMat3(), align.value, (int) SEC2MS(lifetime.value));
        }

        private void Event_InfluenceActive() {
            idPlayer player;

            player = gameLocal.GetLocalPlayer();
            if (player != null && player.GetInfluenceLevel() != 0) {
                idThread.ReturnInt(true);
            } else {
                idThread.ReturnInt(false);
            }
        }

        public idThread() {
            Init();
            SetThreadName(va("thread_%d", threadIndex));
            if (g_debugScript.GetBool()) {
                gameLocal.Printf("%d: create thread (%d) '%s'\n", gameLocal.time, threadNum, threadName);
            }
        }

        public idThread(idEntity self, final function_t func) {
            assert (self != null);

            Init();
            SetThreadName(self.name.toString());
            interpreter.EnterObjectFunction(self, func, false);
            if (g_debugScript.GetBool()) {
                gameLocal.Printf("%d: create thread (%d) '%s'\n", gameLocal.time, threadNum, threadName);
            }
        }

        public idThread(final function_t func) {
            assert (func != null);

            Init();
            SetThreadName(func.Name());
            interpreter.EnterFunction(func, false);
            if (g_debugScript.GetBool()) {
                gameLocal.Printf("%d: create thread (%d) '%s'\n", gameLocal.time, threadNum, threadName);
            }
        }

        public idThread(idInterpreter source, final function_t func, int args) {
            Init();
            interpreter.ThreadCall(source, func, args);
            if (g_debugScript.GetBool()) {
                gameLocal.Printf("%d: create thread (%d) '%s'\n", gameLocal.time, threadNum, threadName);
            }
        }

        public idThread(idInterpreter source, idEntity self, final function_t func, int args) {
            assert (self != null);

            Init();
            SetThreadName(self.name.toString());
            interpreter.ThreadCall(source, func, args);
            if (g_debugScript.GetBool()) {
                gameLocal.Printf("%d: create thread (%d) '%s'\n", gameLocal.time, threadNum, threadName);
            }
        }
        // virtual						~idThread();

        // tells the thread manager not to delete this thread when it ends
        public void ManualDelete() {
            interpreter.terminateOnExit = false;
        }

        // save games
        @Override
        public void Save(idSaveGame savefile) {				// archives object for save game file

            // We will check on restore that threadNum is still the same,
            // threads should have been restored in the same order.
            savefile.WriteInt(threadNum);

            savefile.WriteObject(waitingForThread);
            savefile.WriteInt(waitingFor);
            savefile.WriteInt(waitingUntil);

            interpreter.Save(savefile);

            savefile.WriteDict(spawnArgs);
            savefile.WriteString(threadName);

            savefile.WriteInt(lastExecuteTime);
            savefile.WriteInt(creationTime);

            savefile.WriteBool(manualControl);
        }

        @Override
        public void Restore(idRestoreGame savefile) {				// unarchives object from save game file
            threadNum = savefile.ReadInt();

            savefile.ReadObject(/*reinterpret_cast<idClass *&>*/waitingForThread);
            waitingFor = savefile.ReadInt();
            waitingUntil = savefile.ReadInt();

            interpreter.Restore(savefile);

            savefile.ReadDict(spawnArgs);
            savefile.ReadString(threadName);

            lastExecuteTime = savefile.ReadInt();
            creationTime = savefile.ReadInt();

            manualControl = savefile.ReadBool();
        }

        public void EnableDebugInfo() {
            interpreter.debug = true;
        }

        public void DisableDebugInfo() {
            interpreter.debug = false;
        }

        public void WaitMS(int time) {
            Pause();
            waitingUntil = gameLocal.time + time;
        }

        public void WaitSec(float time) {
            WaitMS((int) SEC2MS(time));
        }

        public void WaitFrame() {
            Pause();

            // manual control threads don't set waitingUntil so that they can be run again
            // that frame if necessary.
            if (!manualControl) {
                waitingUntil = gameLocal.time + gameLocal.msec;
            }
        }

        /*
         ================
         idThread::CallFunction

         NOTE: If this is called from within a event called by this thread, the function arguments will be invalid after calling this function.
         ================
         */
        public void CallFunction(final function_t func, boolean clearStack) {
            ClearWaitFor();
            interpreter.EnterFunction(func, clearStack);
        }

        /*
         ================
         idThread::CallFunction

         NOTE: If this is called from within a event called by this thread, the function arguments will be invalid after calling this function.
         ================
         */
        public void CallFunction(idEntity self, final function_t func, boolean clearStack) {
            assert (self != null);
            ClearWaitFor();
            interpreter.EnterObjectFunction(self, func, clearStack);
        }

        public void DisplayInfo() {
            gameLocal.Printf(
                    "%12i: '%s'\n"
                    + "        File: %s(%d)\n"
                    + "     Created: %d (%d ms ago)\n"
                    + "      Status: ",
                    threadNum, threadName,
                    interpreter.CurrentFile(), interpreter.CurrentLine(),
                    creationTime, gameLocal.time - creationTime);

            if (interpreter.threadDying) {
                gameLocal.Printf("Dying\n");
            } else if (interpreter.doneProcessing) {
                gameLocal.Printf(
                        "Paused since %d (%d ms)\n"
                        + "      Reason: ", lastExecuteTime, gameLocal.time - lastExecuteTime);
                if (waitingForThread != null) {
                    gameLocal.Printf("Waiting for thread #%3d '%s'\n", waitingForThread.GetThreadNum(), waitingForThread.GetThreadName());
                } else if ((waitingFor != ENTITYNUM_NONE) && (gameLocal.entities[ waitingFor] != null)) {
                    gameLocal.Printf("Waiting for entity #%3d '%s'\n", waitingFor, gameLocal.entities[ waitingFor].name);
                } else if (waitingUntil != 0) {
                    gameLocal.Printf("Waiting until %d (%d ms total wait time)\n", waitingUntil, waitingUntil - lastExecuteTime);
                } else {
                    gameLocal.Printf("None\n");
                }
            } else {
                gameLocal.Printf("Processing\n");
            }

            interpreter.DisplayInfo();

            gameLocal.Printf("\n");
        }

        public static idThread GetThread(int num) {
            int i;
            int n;
            idThread thread;

            n = threadList.Num();
            for (i = 0; i < n; i++) {
                thread = threadList.oGet(i);
                if (thread.GetThreadNum() == num) {
                    return thread;
                }
            }

            return null;
        }

        @Override
        public idClass CreateInstance() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public java.lang.Class/*idTypeInfo*/ GetType() {
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
        public void oSet(idClass oGet) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        /*
         ================
         idThread::ListThreads_f
         ================
         */
        public static class ListThreads_f extends cmdFunction_t {

            private static final cmdFunction_t instance = new ListThreads_f();

            private ListThreads_f() {
            }

            public static cmdFunction_t getInstance() {
                return instance;
            }

            @Override
            public void run(idCmdArgs args) {
                int i;
                int n;

                n = threadList.Num();
                for (i = 0; i < n; i++) {
                    //threadList[ i ].DisplayInfo();
                    gameLocal.Printf("%3d: %-20s : %s(%d)\n", threadList.oGet(i).threadNum, threadList.oGet(i).threadName, threadList.oGet(i).interpreter.CurrentFile(), threadList.oGet(i).interpreter.CurrentLine());
                }
                gameLocal.Printf("%d active threads\n\n", n);
            }
        };

        public static void Restart() {
            int i;
            int n;

            // reset the threadIndex
            threadIndex = 0;

            currentThread = null;
            n = threadList.Num();
//	for( i = n - 1; i >= 0; i-- ) {
//		delete threadList[ i ];
//	}
            threadList.Clear();

//	memset( &trace, 0, sizeof( trace ) );
            trace = new trace_s();
            trace.c.entityNum = ENTITYNUM_NONE;
        }

        public static void ObjectMoveDone(int threadnum, idEntity obj) {
            idThread thread;

            if (0 == threadnum) {
                return;
            }

            thread = GetThread(threadnum);
            if (thread != null) {
                thread.ObjectMoveDone(obj);
            }
        }

        public static idList<idThread> GetThreads() {
            return threadList;
        }

        public boolean IsDoneProcessing() {
            return interpreter.doneProcessing;
        }

        public boolean IsDying() {
            return interpreter.threadDying;
        }

        public void End() {
            // Tell thread to die.  It will exit on its own.
            Pause();
            interpreter.threadDying = true;
        }

        public static void KillThread(final String name) {
            int i;
            int num;
            int len;
            int ptr;
            idThread thread;

            // see if the name uses a wild card
            ptr = name.indexOf('*');
            if (ptr != -1) {
                len = ptr - name.length();//TODO:double check this puhlease!
            } else {
                len = name.length();
            }

            // kill only those threads whose name matches name
            num = threadList.Num();
            for (i = 0; i < num; i++) {
                thread = threadList.oGet(i);
                if (0 == idStr.Cmpn(thread.GetThreadName(), name, len)) {
                    thread.End();
                }
            }
        }

        public static void KillThread(int num) {
            idThread thread;

            thread = GetThread(num);
            if (thread != null) {
                // Tell thread to die.  It will delete itself on it's own.
                thread.End();
            }
        }

        public boolean Execute() {
//            return false;//HACKME::6
            idThread oldThread;
            boolean done;

            if (manualControl && (waitingUntil > gameLocal.time)) {
                return false;
            }

            oldThread = currentThread;
            currentThread = this;

            lastExecuteTime = gameLocal.time;
            ClearWaitFor();
            done = interpreter.Execute();
            if (done) {
                End();
                if (interpreter.terminateOnExit) {
                    PostEventMS(EV_Remove, 0);
                }
            } else if (!manualControl) {
                if (waitingUntil > lastExecuteTime) {
                    PostEventMS(EV_Thread_Execute, waitingUntil - lastExecuteTime);
                } else if (interpreter.MultiFrameEventInProgress()) {
                    PostEventMS(EV_Thread_Execute, gameLocal.msec);
                }
            }

            currentThread = oldThread;

            return done;
        }

        public void ManualControl() {
            manualControl = true;
            CancelEvents(EV_Thread_Execute);
        }

        public void DoneProcessing() {
            interpreter.doneProcessing = true;
        }

        public void ContinueProcessing() {
            interpreter.doneProcessing = false;
        }

        public boolean ThreadDying() {
            return interpreter.threadDying;
        }

        public void EndThread() {
            interpreter.threadDying = true;
        }

        /*
         ================
         idThread::IsWaiting

         Checks if thread is still waiting for some event to occur.
         ================
         */
        public boolean IsWaiting() {
            if (waitingForThread != null || (waitingFor != ENTITYNUM_NONE)) {
                return true;
            }

            if (waitingUntil != 0 && (waitingUntil > gameLocal.time)) {
                return true;
            }

            return false;
        }

        public void ClearWaitFor() {
            waitingFor = ENTITYNUM_NONE;
            waitingForThread = null;
            waitingUntil = 0;
        }

        public boolean IsWaitingFor(idEntity obj) {
            assert (obj != null);
            return waitingFor == obj.entityNumber;
        }

        public void ObjectMoveDone(idEntity obj) {
            assert (obj != null);

            if (IsWaitingFor(obj)) {
                ClearWaitFor();
                DelayedStart(0);
            }
        }

        public void ThreadCallback(idThread thread) {
            if (interpreter.threadDying) {
                return;
            }

            if (thread == waitingForThread) {
                ClearWaitFor();
                DelayedStart(0);
            }
        }

        public void DelayedStart(int delay) {
            CancelEvents(EV_Thread_Execute);
            if (gameLocal.time <= 0) {
                delay++;
            }
            PostEventMS(EV_Thread_Execute, delay);
        }

        public boolean Start() {
            boolean result;

            CancelEvents(EV_Thread_Execute);
            result = Execute();

            return result;
        }

        public idThread WaitingOnThread() {
            return waitingForThread;
        }

        public void SetThreadNum(int num) {
            threadNum = num;
        }

        public int GetThreadNum() {
            return threadNum;
        }

        public void SetThreadName(final String name) {
            threadName.oSet(name);
        }

        public String GetThreadName() {
            return threadName.toString();
        }

        public void Error(final String fmt, Object... objects) {// const id_attribute((format(printf,2,3)));
            String text = String.format(fmt, objects);
            interpreter.Error(text);
        }

        public void Warning(final String fmt, Object... objects) {// const id_attribute((format(printf,2,3)));
            String text = String.format(fmt, objects);
            interpreter.Warning(text);
        }

        public static idThread CurrentThread() {
            return currentThread;
        }

        public static int CurrentThreadNum() {
            if (currentThread != null) {
                return currentThread.GetThreadNum();
            } else {
                return 0;
            }
        }

        public static boolean BeginMultiFrameEvent(idEntity ent, final idEventDef event) {
            if (null == currentThread) {
                gameLocal.Error("idThread::BeginMultiFrameEvent called without a current thread");
            }
            return currentThread.interpreter.BeginMultiFrameEvent(ent, event);
        }

        public static void EndMultiFrameEvent(idEntity ent, final idEventDef event) {
            if (null == currentThread) {
                gameLocal.Error("idThread::EndMultiFrameEvent called without a current thread");
            }
            currentThread.interpreter.EndMultiFrameEvent(ent, event);
        }

        public static void ReturnString(final String text) {
            gameLocal.program.ReturnString(text);
        }

        public static void ReturnString(final idStr text) {
            ReturnString(text.toString());
        }

        public static void ReturnFloat(float value) {
            gameLocal.program.ReturnFloat(value);
        }

        public static void ReturnInt(int value) {
            // true integers aren't supported in the compiler,
            // so int values are stored as floats
            gameLocal.program.ReturnFloat(value);
        }

        public static void ReturnInt(boolean value) {
            ReturnInt(value ? 1 : 0);
        }

        public static void ReturnVector(final idVec3 vec) {
            gameLocal.program.ReturnVector(vec);
        }

        public static void ReturnEntity(idEntity ent) {
            gameLocal.program.ReturnEntity(ent);
        }
    };
}
