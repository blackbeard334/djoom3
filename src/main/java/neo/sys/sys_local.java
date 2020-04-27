package neo.sys;

import static neo.TempDump.btoi;
import static neo.framework.BuildDefines._WIN32;
import static neo.framework.BuildDefines.__linux__;
import static neo.framework.CVarSystem.CVAR_ARCHIVE;
import static neo.framework.CVarSystem.CVAR_SYSTEM;
import static neo.framework.CVarSystem.cvarSystem;
import static neo.framework.KeyInput.K_MOUSE1;
import static neo.sys.sys_public.CPUSTRING;
import static neo.sys.sys_public.sysEventType_t.SE_KEY;
import static neo.sys.sys_public.sysEventType_t.SE_MOUSE;
import static neo.sys.win_cpu.Sys_ClockTicksPerSecond;
import static neo.sys.win_cpu.Sys_FPU_EnableExceptions;
import static neo.sys.win_cpu.Sys_FPU_GetState;
import static neo.sys.win_cpu.Sys_FPU_SetDAZ;
import static neo.sys.win_cpu.Sys_FPU_SetFTZ;
import static neo.sys.win_cpu.Sys_FPU_StackIsEmpty;
import static neo.sys.win_cpu.Sys_GetClockTicks;
import static neo.sys.win_main.Sys_DLL_GetProcAddress;
import static neo.sys.win_main.Sys_DLL_Load;
import static neo.sys.win_main.Sys_DLL_Unload;
import static neo.sys.win_main.Sys_DebugVPrintf;
import static neo.sys.win_main.Sys_GetProcessorId;
import static neo.sys.win_main.Sys_GetProcessorString;
import static neo.sys.win_shared.Sys_GetCallStack;
import static neo.sys.win_shared.Sys_GetCallStackCurStr;
import static neo.sys.win_shared.Sys_GetCallStackStr;
import static neo.sys.win_shared.Sys_LockMemory;
import static neo.sys.win_shared.Sys_ShutdownSymbols;
import static neo.sys.win_shared.Sys_UnlockMemory;

import java.text.SimpleDateFormat;
import java.util.Date;

import neo.TempDump.TODO_Exception;
import neo.framework.CVarSystem.idCVar;
import neo.framework.CmdSystem.idCmdSystem;
import neo.idlib.Lib.idException;
import neo.idlib.Text.Str.idStr;
import neo.sys.sys_public.idSys;
import neo.sys.sys_public.sysEvent_s;

/**
 *
 */
public class sys_local {

    static idSysLocal sysLocal = new idSysLocal();

    static final String[] sysLanguageNames = {
        "english", "spanish", "italian", "german", "french", "russian",
        "polish", "korean", "japanese", "chinese", null
    };
//
    static final idCVar sys_lang = new idCVar("sys_lang", "english", CVAR_SYSTEM | CVAR_ARCHIVE, "", sysLanguageNames, new idCmdSystem.ArgCompletion_String(sysLanguageNames));

    /*
     ==============================================================

     idSysLocal

     ==============================================================
     */
    static class idSysLocal extends idSys {

        @Override
        public void DebugPrintf(final String fmt, Object... arg) {
            Sys_DebugVPrintf(fmt, arg);
        }

        @Override
        public void DebugVPrintf(final String fmt, Object... arg) {
            Sys_DebugVPrintf(fmt, arg);
        }

        @Override
        public double GetClockTicks() {
            return Sys_GetClockTicks();
        }

        @Override
        public double ClockTicksPerSecond() {
            return Sys_ClockTicksPerSecond();
        }

        @Override
        public int GetProcessorId() {
            return Sys_GetProcessorId();
        }

        @Override
        public String GetProcessorString() {
            return Sys_GetProcessorString();
        }

        @Override
        public String FPU_GetState() {
            return Sys_FPU_GetState();
        }

        @Override
        public boolean FPU_StackIsEmpty() {
            return Sys_FPU_StackIsEmpty();
        }

        @Override
        public void FPU_SetFTZ(boolean enable) {
            Sys_FPU_SetFTZ(enable);
        }

        @Override
        public void FPU_SetDAZ(boolean enable) {
            Sys_FPU_SetDAZ(enable);
        }

        @Override
        public void FPU_EnableExceptions(int exceptions) {
            Sys_FPU_EnableExceptions(exceptions);
        }

        @Override
        public boolean LockMemory(Object ptr, int bytes) {
            return Sys_LockMemory(ptr, bytes);
        }

        @Override
        public boolean UnlockMemory(Object ptr, int bytes) {
            return Sys_UnlockMemory(ptr, bytes);
        }

        @Override
        public void GetCallStack(long callStack, int callStackSize) {
            Sys_GetCallStack(callStack, callStackSize);
        }

        @Override
        public String GetCallStackStr(long callStack, int callStackSize) {
            return Sys_GetCallStackStr(callStack, callStackSize);
        }

        @Override
        public String GetCallStackCurStr(int depth) {
            return Sys_GetCallStackCurStr(depth);
        }

        @Override
        public void ShutdownSymbols() {
            Sys_ShutdownSymbols();
        }

        @Override
        public int DLL_Load(String dllName) {
            return Sys_DLL_Load(dllName);
        }

        @Override
        public Object DLL_GetProcAddress(int dllHandle, String procName) {
            return Sys_DLL_GetProcAddress(dllHandle, procName);
        }

        @Override
        public void DLL_Unload(int dllHandle) {
            Sys_DLL_Unload(dllHandle);
        }

        @Override
        public void DLL_GetFileName(String baseName, String[] dllName, int maxLength) {
            if (_WIN32) {
                idStr.snPrintf(dllName, maxLength, "%s" + CPUSTRING + ".dll", baseName);
            } else if (__linux__) {
                idStr.snPrintf(dllName, maxLength, "%s" + CPUSTRING + ".so", baseName);
// #elif defined( MACOS_X )
                // idStr::snPrintf( dllName, maxLength, "%s" ".dylib", baseName );
            } else {
// #error OS define is required
                throw new idException("OS define is required");
// #endif
            }
        }

        @Override
        public sysEvent_s GenerateMouseButtonEvent(int button, boolean down) {
            sysEvent_s ev = new sysEvent_s();
            ev.evType = SE_KEY;
            ev.evValue = K_MOUSE1 + button - 1;
            ev.evValue2 = btoi(down);
            ev.evPtrLength = 0;
            ev.evPtr = null;
            return ev;
        }

        @Override
        public sysEvent_s GenerateMouseMoveEvent(int deltax, int deltay) {
            sysEvent_s ev = new sysEvent_s();
            ev.evType = SE_MOUSE;
            ev.evValue = deltax;
            ev.evValue2 = deltay;
            ev.evPtrLength = 0;
            ev.evPtr = null;
            return ev;
        }
        static boolean doexit_spamguard = false;

        @Override
        public void OpenURL(String url, boolean doExit) {
            throw new TODO_Exception();
//            HWND wnd;
//
//            if (doexit_spamguard) {
//                common.DPrintf("OpenURL: already in an exit sequence, ignoring %s\n", url);
//                return;
//            }
//
//            common.Printf("Open URL: %s\n", url);
//
//            if (!ShellExecute(null, "open", url, null, null, SW_RESTORE)) {
//                common.Error("Could not open url: '%s' ", url);
//                return;
//            }
//
//            wnd = GetForegroundWindow();
//            if (wnd) {
//                ShowWindow(wnd, SW_MAXIMIZE);
//            }
//
//            if (doExit) {
//                doexit_spamguard = true;
//                cmdSystem.BufferCommandText(CMD_EXEC_APPEND, "quit\n");
//            }
        }

        @Override
        public void StartProcess(String exePath, boolean doExit) {
            throw new TODO_Exception();
//            char[] szPathOrig = new char[_MAX_PATH];
//            STARTUPINFO si;
//            PROCESS_INFORMATION pi;
//
//            ZeroMemory(si, sizeof(si));
//            si.cb = sizeof(si);
//
//            strncpy(szPathOrig, exePath, _MAX_PATH);
//
//            if (!CreateProcess(null, szPathOrig, null, null, FALSE, 0, null, null, si, pi)) {
//                common.Error("Could not start process: '%s' ", szPathOrig);
//                return;
//            }
//
//            if (doExit) {
//                cmdSystem.BufferCommandText(CMD_EXEC_APPEND, "quit\n");
//            }
        }
    };
    /*
     =================
     Sys_TimeStampToStr
     =================
     */
    static String timeString;//= new char[MAX_STRING_CHARS];

    public static String Sys_TimeStampToStr(long/*ID_TIME_T*/ timeStamp) {
//        timeString[0] = '\0';

//        tm time = localtime(timeStamp);
        final Date time = new Date();
        String out;

        idStr lang = new idStr(cvarSystem.GetCVarString("sys_lang"));
        if (lang.Icmp("english") == 0) {
            // english gets "month/day/year  hour:min" + "am" or "pm"
            out = new SimpleDateFormat("MM/dd/yyyy\thh:mmaa").format(time).toLowerCase();
//            out.oSet(va("%02d", time.tm_mon + 1));
//            out.oPluSet("/");
//            out.oPluSet(va("%02d", time.tm_mday));
//            out.oPluSet("/");
//            out.oPluSet(va("%d", time.tm_year + 1900));
//            out.oPluSet("\t");
//            if (time.tm_hour > 12) {
//                out.oPluSet(va("%02d", time.tm_hour - 12));
//            } else if (time.tm_hour == 0) {
//                out.oPluSet("12");
//            } else {
//                out.oPluSet(va("%02d", time.tm_hour));
//            }
//            out.oPluSet(":");
//            out.oPluSet(va("%02d", time.tm_min));
//            if (time.tm_hour >= 12) {
//                out.oPluSet("pm");
//            } else {
//                out.oPluSet("am");
//            }
        } else {
            // europeans get "day/month/year  24hour:min"
            out = new SimpleDateFormat("dd/MM/yyyy\tHH:mm").format(time);
//            out.oSet(va("%02d", time.tm_mday));
//            out.oPluSet("/");
//            out.oPluSet(va("%02d", time.tm_mon + 1));
//            out.oPluSet("/");
//            out.oPluSet(va("%d", time.tm_year + 1900));
//            out.oPluSet("\t");
//            out.oPluSet(va("%02d", time.tm_hour));
//            out.oPluSet(":");
//            out.oPluSet(va("%02d", time.tm_min));
        }
//        idStr.Copynz(timeString, out, sizeof(timeString));
//        

        return timeString = out;
    }
}
