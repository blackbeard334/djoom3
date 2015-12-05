package neo.Game.Script;

import neo.CM.CollisionModel.trace_s;
import neo.Game.AFEntity.idAFEntity_Base;
import neo.Game.Camera.idCamera;
import static neo.Game.Entity.EV_Activate;
import neo.Game.Entity.idEntity;
import static neo.Game.Entity.signalNum_t.NUM_SIGNALS;
import static neo.Game.Entity.signalNum_t.SIG_TRIGGER;
import static neo.Game.GameSys.Class.EV_Remove;
import neo.Game.GameSys.Class.idClass;
import neo.Game.GameSys.Event.idEventDef;
import neo.Game.GameSys.SaveGame.idRestoreGame;
import neo.Game.GameSys.SaveGame.idSaveGame;
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
import neo.TempDump.TODO_Exception;
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
import neo.idlib.math.Vector.idVec3;
import neo.idlib.math.Vector.idVec4;
import static neo.idlib.math.Vector.vec3_origin;

/**
 *
 */
public class Script_Thread {

    static final idEventDef EV_Thread_Execute             = new idEventDef("<execute>", null);
    static final idEventDef EV_Thread_SetCallback         = new idEventDef("<script_setcallback>", null);
    //
    // script callable events
    static final idEventDef EV_Thread_TerminateThread     = new idEventDef("terminate", "d");
    static final idEventDef EV_Thread_Pause               = new idEventDef("pause", null);
    static final idEventDef EV_Thread_Wait                = new idEventDef("wait", "f");
    static final idEventDef EV_Thread_WaitFrame           = new idEventDef("waitFrame");
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

//        // CLASS_PROTOTYPE( idThread );
//        public static final idTypeInfo Type = new idTypeInfo(null, null, eventCallbacks, null, null, null, null);
//        public idEventFunc<idThread>[] eventcallbacks;
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

        private void Event_SetThreadName(final String name) {
            SetThreadName(name);
        }

        //
        // script callable Events
        //
        private void Event_TerminateThread(int num) {
            idThread thread;

            thread = GetThread(num);
            KillThread(num);
        }

        private void Event_Pause() {
            Pause();
        }

        private void Event_Wait(float time) {
            WaitSec(time);
        }

        private void Event_WaitFrame() {
            WaitFrame();
        }

        private void Event_WaitFor(idEntity ent) {
            if (ent != null && ent.RespondsTo(EV_Thread_SetCallback)) {
                ent.ProcessEvent(EV_Thread_SetCallback);
                if (gameLocal.program.GetReturnedInteger() != 0) {
                    Pause();
                    waitingFor = ent.entityNumber;
                }
            }
        }

        private void Event_WaitForThread(int num) {
            idThread thread;

            thread = GetThread(num);
            if (null == thread) {
                if (g_debugScript.GetBool()) {
                    // just print a warning and continue executing
                    Warning("Thread %d not running", num);
                }
            } else {
                Pause();
                waitingForThread = thread;
            }
        }

        private void Event_Print(final String text) {
            gameLocal.Printf("%s", text);
        }

        private void Event_PrintLn(final String text) {
            gameLocal.Printf("%s\n", text);
        }

        private void Event_Say(final String text) {
            cmdSystem.BufferCommandText(CMD_EXEC_NOW, va("say \"%s\"", text));
        }

        private void Event_Assert(float value) {
            assert (value != 0);
        }

        private void Event_Trigger(idEntity ent) {
            if (ent != null) {
                ent.Signal(SIG_TRIGGER);
                ent.ProcessEvent(EV_Activate, gameLocal.GetLocalPlayer());
                ent.TriggerGuis();
            }
        }

        private void Event_SetCvar(final String name, final String value) {
            cvarSystem.SetCVarString(name, value);
        }

        private void Event_GetCvar(final String name) {
            ReturnString(cvarSystem.GetCVarString(name));
        }

        private void Event_Random(float range) {
            float result;

            result = gameLocal.random.RandomFloat();
            ReturnFloat(range * result);
        }

        private void Event_GetTime() {
            ReturnFloat(MS2SEC(gameLocal.realClientTime));
        }

        private void Event_KillThread(final String name) {
            KillThread(name);
        }

        private void Event_GetEntity(final String name) {
            int entnum;
            idEntity ent;

            assert (name != null);

            if (name.charAt(0) == '*') {
                entnum = Integer.parseInt(name.substring(1));
                if ((entnum < 0) || (entnum >= MAX_GENTITIES)) {
                    Error("Entity number in string out of range.");
                }
                ReturnEntity(gameLocal.entities[entnum]);
            } else {
                ent = gameLocal.FindEntity(name);
                ReturnEntity(ent);
            }
        }

        private void Event_Spawn(final String classname) {
            idEntity[] ent = {null};

            spawnArgs.Set("classname", classname);
            gameLocal.SpawnEntityDef(spawnArgs, ent);
            ReturnEntity(ent[0]);
            spawnArgs.Clear();
        }

        private void Event_CopySpawnArgs(idEntity ent) {
            spawnArgs.Copy(ent.spawnArgs);
        }

        private void Event_SetSpawnArg(final String key, final String value) {
            spawnArgs.Set(key, value);
        }

        private void Event_SpawnString(final String key, final String defaultvalue) {
            String[] result = {null};

            spawnArgs.GetString(key, defaultvalue, result);
            ReturnString(result[0]);
        }

        private void Event_SpawnFloat(final String key, float defaultvalue) {
            float[] result = {0};

            spawnArgs.GetFloat(key, va("%f", defaultvalue), result);
            ReturnFloat(result[0]);
        }

        private void Event_SpawnVector(final String key, idVec3 defaultvalue) {
            idVec3 result = new idVec3();

            spawnArgs.GetVector(key, va("%f %f %f", defaultvalue.x, defaultvalue.y, defaultvalue.z), result);
            ReturnVector(result);
        }

        private void Event_ClearPersistantArgs() {
            gameLocal.persistentLevelInfo.Clear();
        }

        private void Event_SetPersistantArg(final String key, final String value) {
            gameLocal.persistentLevelInfo.Set(key, value);
        }

        private void Event_GetPersistantString(final String key) {
            String[] result = {null};

            gameLocal.persistentLevelInfo.GetString(key, "", result);
            ReturnString(result[0]);
        }

        private void Event_GetPersistantFloat(final String key) {
            float[] result = {0};

            gameLocal.persistentLevelInfo.GetFloat(key, "0", result);
            ReturnFloat(result[0]);
        }

        private void Event_GetPersistantVector(final String key) {
            idVec3 result = new idVec3();

            gameLocal.persistentLevelInfo.GetVector(key, "0 0 0", result);
            ReturnVector(result);
        }

        private void Event_AngToForward(idAngles ang) {
            ReturnVector(ang.ToForward());
        }

        private void Event_AngToRight(idAngles ang) {
            idVec3 vec = new idVec3();

            ang.ToVectors(null, vec);
            ReturnVector(vec);
        }

        private void Event_AngToUp(idAngles ang) {
            idVec3 vec = new idVec3();

            ang.ToVectors(null, null, vec);
            ReturnVector(vec);
        }

        private void Event_GetSine(float angle) {
            ReturnFloat(idMath.Sin(DEG2RAD(angle)));
        }

        private void Event_GetCosine(float angle) {
            ReturnFloat(idMath.Cos(DEG2RAD(angle)));
        }

        private void Event_GetSquareRoot(float theSquare) {
            ReturnFloat(idMath.Sqrt(theSquare));
        }

        private void Event_VecNormalize(idVec3 vec) {
            idVec3 n;

            n = vec;
            n.Normalize();
            ReturnVector(n);
        }

        private void Event_VecLength(idVec3 vec) {
            ReturnFloat(vec.Length());
        }

        private void Event_VecDotProduct(idVec3 vec1, idVec3 vec2) {
            ReturnFloat(vec1.oMultiply(vec2));
        }

        private void Event_VecCrossProduct(idVec3 vec1, idVec3 vec2) {
            ReturnVector(vec1.Cross(vec2));
        }

        private void Event_VecToAngles(idVec3 vec) {
            idAngles ang = vec.ToAngles();
            ReturnVector(new idVec3(ang.oGet(0), ang.oGet(1), ang.oGet(2)));
        }

        private void Event_OnSignal(int signal, idEntity ent, final String func) {
            function_t function;

            assert (func != null);

            if (null == ent) {
                Error("Entity not found");
            }

            if ((signal < 0) || (signal >= etoi(NUM_SIGNALS))) {
                Error("Signal out of range");
            }

            function = gameLocal.program.FindFunction(func);
            if (null == function) {
                Error("Function '%s' not found", func);
            }

            ent.SetSignal(signal, this, function);
        }

        private void Event_ClearSignalThread(int signal, idEntity ent) {
            if (null == ent) {
                Error("Entity not found");
            }

            if ((signal < 0) || (signal >= etoi(NUM_SIGNALS))) {
                Error("Signal out of range");
            }

            ent.ClearSignalThread(signal, this);
        }

        private void Event_SetCamera(idEntity ent) {
            if (null == ent) {
                Error("Entity not found");
                return;
            }

            if (!ent.IsType(idCamera.class)) {
                Error("Entity is not a camera");
                return;
            }

            gameLocal.SetCamera((idCamera) ent);
        }

        private void Event_FirstPerson() {
            gameLocal.SetCamera(null);
        }

        private void Event_Trace(final idVec3 start, final idVec3 end, final idVec3 mins, final idVec3 maxs, int contents_mask, idEntity passEntity) {
            {
                trace_s[] trace = {this.trace};
                if (mins.equals(vec3_origin) && maxs.equals(vec3_origin)) {
                    gameLocal.clip.TracePoint(trace, start, end, contents_mask, passEntity);
                } else {
                    gameLocal.clip.TraceBounds(trace, start, end, new idBounds(mins, maxs), contents_mask, passEntity);
                }
                this.trace = trace[0];
            }
            ReturnFloat(trace.fraction);
        }

        private void Event_TracePoint(final idVec3 start, final idVec3 end, int contents_mask, idEntity passEntity) {
            {
                trace_s[] trace = {this.trace};
                gameLocal.clip.TracePoint(trace, start, end, contents_mask, passEntity);
                this.trace = trace[0];
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
                ReturnVector(vec3_origin);
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

        private void Event_FadeIn(idVec3 color, float time) {
            idVec4 fadeColor = new idVec4();
            idPlayer player;

            player = gameLocal.GetLocalPlayer();
            if (player != null) {
                fadeColor.Set(color.oGet(0), color.oGet(1), color.oGet(2), 0.0f);
                player.playerView.Fade(fadeColor, (int) SEC2MS(time));
            }
        }

        private void Event_FadeOut(idVec3 color, float time) {
            idVec4 fadeColor = new idVec4();
            idPlayer player;

            player = gameLocal.GetLocalPlayer();
            if (player != null) {
                fadeColor.Set(color.oGet(0), color.oGet(1), color.oGet(2), 1.0f);
                player.playerView.Fade(fadeColor, (int) SEC2MS(time));
            }
        }

        private void Event_FadeTo(idVec3 color, float alpha, float time) {
            idVec4 fadeColor = new idVec4();
            idPlayer player;

            player = gameLocal.GetLocalPlayer();
            if (player != null) {
                fadeColor.Set(color.oGet(0), color.oGet(1), color.oGet(2), alpha);
                player.playerView.Fade(fadeColor, (int) SEC2MS(time));
            }
        }

        private void Event_SetShaderParm(int parmnum, float value) {
            if ((parmnum < 0) || (parmnum >= MAX_GLOBAL_SHADER_PARMS)) {
                Error("shader parm index (%d) out of range", parmnum);
            }

            gameLocal.globalShaderParms[ parmnum] = value;
        }

        private void Event_StartMusic(final String text) {
            gameSoundWorld.PlayShaderDirectly(text);
        }

        private void Event_Warning(final String text) {
            Warning("%s", text);
        }

        private void Event_Error(final String text) {
            Error("%s", text);
        }

        private void Event_StrLen(final String string) {
            int len;

            len = string.length();
            idThread.ReturnInt(len);
        }

        private void Event_StrLeft(final String string, int num) {
            int len;

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

        private void Event_StrRight(final String string, int num) {
            int len;

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

        private void Event_StrSkip(final String string, int num) {
            int len;

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

        private void Event_StrMid(final String string, int start, int num) {
            int len;

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

        private void Event_StrToFloat(final String string) {
            float result;

            result = Float.parseFloat(string);
            idThread.ReturnFloat(result);
        }

        private void Event_RadiusDamage(final idVec3 origin, idEntity inflictor, idEntity attacker, idEntity ignore, final String damageDefName, float dmgPower) {
            gameLocal.RadiusDamage(origin, inflictor, attacker, ignore, ignore, damageDefName, dmgPower);
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

        private void Event_CacheSoundShader(final String soundName) {
            declManager.FindSound(soundName);
        }

        private void Event_DebugLine(final idVec3 color, final idVec3 start, final idVec3 end, final float lifetime) {
            gameRenderWorld.DebugLine(new idVec4(color.x, color.y, color.z, 0.0f), start, end, (int) SEC2MS(lifetime));
        }

        private void Event_DebugArrow(final idVec3 color, final idVec3 start, final idVec3 end, final int size, final float lifetime) {
            gameRenderWorld.DebugArrow(new idVec4(color.x, color.y, color.z, 0.0f), start, end, size, (int) SEC2MS(lifetime));
        }

        private void Event_DebugCircle(final idVec3 color, final idVec3 origin, final idVec3 dir, final float radius, final int numSteps, final float lifetime) {
            gameRenderWorld.DebugCircle(new idVec4(color.x, color.y, color.z, 0.0f), origin, dir, radius, numSteps, (int) SEC2MS(lifetime));
        }

        private void Event_DebugBounds(final idVec3 color, final idVec3 mins, final idVec3 maxs, final float lifetime) {
            gameRenderWorld.DebugBounds(new idVec4(color.x, color.y, color.z, 0.0f), new idBounds(mins, maxs), vec3_origin, (int) SEC2MS(lifetime));
        }

        private void Event_DrawText(final String text, final idVec3 origin, float scale, final idVec3 color, final int align, final float lifetime) {
            gameRenderWorld.DrawText(text, origin, scale, new idVec4(color.x, color.y, color.z, 0.0f), gameLocal.GetLocalPlayer().viewAngles.ToMat3(), align, (int) SEC2MS(lifetime));
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
                    gameLocal.Printf("Waiting for thread #%3i '%s'\n", waitingForThread.GetThreadNum(), waitingForThread.GetThreadName());
                } else if ((waitingFor != ENTITYNUM_NONE) && (gameLocal.entities[ waitingFor] != null)) {
                    gameLocal.Printf("Waiting for entity #%3i '%s'\n", waitingFor, gameLocal.entities[ waitingFor].name);
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
                    gameLocal.Printf("%3i: %-20s : %s(%d)\n", threadList.oGet(i).threadNum, threadList.oGet(i).threadName, threadList.oGet(i).interpreter.CurrentFile(), threadList.oGet(i).interpreter.CurrentLine());
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
            throw new TODO_Exception();
//            va_list argptr;
//            char[] text = new char[1024];
//            
//            va_start(argptr, fmt);
//            vsprintf(text, fmt, argptr);
//            va_end(argptr);
//            
//            interpreter.Error(text);
        }

        public void Warning(final String fmt, Object... objects) {// const id_attribute((format(printf,2,3)));
            throw new TODO_Exception();
//            va_list argptr;
//            char[] text = new char[1024];
//            
//            va_start(argptr, fmt);
//            vsprintf(text, fmt, argptr);
//            va_end(argptr);
//            
//            interpreter.Warning(text);
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
