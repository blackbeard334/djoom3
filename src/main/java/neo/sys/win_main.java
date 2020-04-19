package neo.sys;

import static neo.TempDump.NOT;
import static neo.TempDump.atobb;
import static neo.TempDump.ctos;
import static neo.TempDump.fopenOptions;
import static neo.Tools.edit_public.AFEditorRun;
import static neo.Tools.edit_public.DeclBrowserRun;
import static neo.Tools.edit_public.GUIEditorRun;
import static neo.Tools.edit_public.LightEditorRun;
import static neo.Tools.edit_public.MaterialEditorRun;
import static neo.Tools.edit_public.PDAEditorRun;
import static neo.Tools.edit_public.ParticleEditorRun;
import static neo.Tools.edit_public.RadiantRun;
import static neo.Tools.edit_public.ScriptEditorRun;
import static neo.Tools.edit_public.SoundEditorRun;
import static neo.framework.BuildDefines.ID_ALLOW_TOOLS;
import static neo.framework.CVarSystem.CVAR_SYSTEM;
import static neo.framework.CmdSystem.CMD_FL_SYSTEM;
import static neo.framework.CmdSystem.CMD_FL_TOOL;
import static neo.framework.CmdSystem.cmdSystem;
import static neo.framework.Common.EDITOR_AF;
import static neo.framework.Common.EDITOR_DECL;
import static neo.framework.Common.EDITOR_GUI;
import static neo.framework.Common.EDITOR_LIGHT;
import static neo.framework.Common.EDITOR_MATERIAL;
import static neo.framework.Common.EDITOR_PARTICLE;
import static neo.framework.Common.EDITOR_PDA;
import static neo.framework.Common.EDITOR_RADIANT;
import static neo.framework.Common.EDITOR_SCRIPT;
import static neo.framework.Common.EDITOR_SOUND;
import static neo.framework.Common.com_editors;
import static neo.framework.Common.com_skipRenderer;
import static neo.framework.Common.common;
import static neo.idlib.Lib.MAX_STRING_CHARS;
import static neo.idlib.Lib.idLib.cvarSystem;
import static neo.sys.sys_public.CPUID_3DNOW;
import static neo.sys.sys_public.CPUID_AMD;
import static neo.sys.sys_public.CPUID_GENERIC;
import static neo.sys.sys_public.CPUID_HTT;
import static neo.sys.sys_public.CPUID_INTEL;
import static neo.sys.sys_public.CPUID_MMX;
import static neo.sys.sys_public.CPUID_NONE;
import static neo.sys.sys_public.CPUID_SSE;
import static neo.sys.sys_public.CPUID_SSE2;
import static neo.sys.sys_public.CPUID_SSE3;
import static neo.sys.sys_public.CPUID_UNSUPPORTED;
import static neo.sys.sys_public.CRITICAL_SECTION_ZERO;
import static neo.sys.sys_public.MAX_CRITICAL_SECTIONS;
import static neo.sys.sys_public.MAX_THREADS;
import static neo.sys.sys_public.TRIGGER_EVENT_ZERO;
import static neo.sys.sys_public.sysEventType_t.SE_CONSOLE;
import static neo.sys.sys_public.xthreadPriority.THREAD_ABOVE_NORMAL;
import static neo.sys.sys_public.xthreadPriority.THREAD_HIGHEST;
import static neo.sys.win_cpu.Sys_ClockTicksPerSecond;
import static neo.sys.win_cpu.Sys_GetCPUId;
import static neo.sys.win_glimp.GLimp_Shutdown;
import static neo.sys.win_input.Sys_InitInput;
import static neo.sys.win_input.Sys_ShutdownInput;
import static neo.sys.win_local.win32;
import static neo.sys.win_local.Win32Vars_t.win_viewlog;
import static neo.sys.win_shared.Sys_GetCurrentUser;
import static neo.sys.win_shared.Sys_GetSystemRam;
import static neo.sys.win_shared.Sys_GetVideoRam;
import static neo.sys.win_shared.Sys_Milliseconds;
import static neo.sys.win_syscon.Conbuf_AppendText;
import static neo.sys.win_syscon.Sys_ConsoleInput;
import static neo.sys.win_syscon.Sys_CreateConsole;
import static neo.sys.win_syscon.Sys_DestroyConsole;
import static neo.sys.win_syscon.Sys_ShowConsole;
import static neo.sys.win_syscon.Win_SetErrorText;

import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Paths;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import neo.TempDump;
import neo.TempDump.NeoFixStrings;
import neo.TempDump.TODO_Exception;
import neo.framework.CVarSystem.idCVar;
import neo.framework.CmdSystem.cmdFunction_t;
import neo.framework.Async.AsyncNetwork.idAsyncNetwork;
import neo.idlib.CmdArgs.idCmdArgs;
import neo.idlib.Lib.idException;
import neo.idlib.Text.Lexer.idLexer;
import neo.idlib.Text.Str.idStr;
import neo.idlib.Text.Token.idToken;
import neo.idlib.containers.StrList.idStrList;
import neo.sys.sys_public.sysEventType_t;
import neo.sys.sys_public.sysEvent_s;
import neo.sys.sys_public.sysMemoryStats_s;
import neo.sys.sys_public.xthreadInfo;
import neo.sys.sys_public.xthreadPriority;
import neo.sys.sys_public.xthread_t;
import neo.sys.win_local.Win32Vars_t;
import neo.sys.RC.CreateResourceIDs_f;

/**
 *
 */
public class win_main {//TODO: rename to plain "main" or something.

    public static final boolean DEBUG = true;

    static final int MAXPRINTMSG = 4096;

    static final int MAX_QUED_EVENTS  = 256;
    static final int MASK_QUED_EVENTS = (MAX_QUED_EVENTS - 1);

    static final int OSR2_BUILD_NUMBER  = 1111;
    static final int WIN98_BUILD_NUMBER = 1998;

    static final StringBuilder sys_cmdline = new StringBuilder(MAX_STRING_CHARS);

    static        xthreadInfo                         threadInfo;
    public static ScheduledExecutorService /*HANDLE*/ hTimer;

    static /*unsigned*/ int debug_total_alloc         = 0;
    static /*unsigned*/ int debug_total_alloc_count   = 0;
    static /*unsigned*/ int debug_current_alloc       = 0;
    static /*unsigned*/ int debug_current_alloc_count = 0;
    static /*unsigned*/ int debug_frame_alloc         = 0;
    static /*unsigned*/ int debug_frame_alloc_count   = 0;

    static final idCVar sys_showMallocs = new idCVar("sys_showMallocs", "0", CVAR_SYSTEM, "");

    /*
     ================
     Sys_GetExeLaunchMemoryStatus
     ================
     */
    public static void Sys_GetExeLaunchMemoryStatus(sysMemoryStats_s stats) {
        throw new TODO_Exception();
//	stats = exeLaunchMemoryStats;
    }

    /*
     ==================
     Sys_Createthread
     ==================
     */
    public static void Sys_CreateThread(xthread_t function, Object parms, xthreadPriority priority, xthreadInfo info, final String name, xthreadInfo[] threads/*[MAX_THREADS]*/, int[] thread_count) {

        final Thread temp = new Thread(function);
        info.threadId = temp.getId();
        info.threadHandle = temp;//TODO: do we need this?
        if (priority == THREAD_HIGHEST) {
            info.threadHandle.setPriority(Thread.MAX_PRIORITY);    //  we better sleep enough to do this
        } else if (priority == THREAD_ABOVE_NORMAL) {
            info.threadHandle.setPriority(Thread.NORM_PRIORITY + 2);
        }
        info.name = name;
        if (thread_count[0] < MAX_THREADS) {
            threads[thread_count[0]++] = info;
            info.threadHandle.start();
        } else {
            common.DPrintf("WARNING: MAX_THREADS reached\n");
        }
    }

    /*
     ==================
     Sys_DestroyThread
     ==================
     */
    public static void Sys_DestroyThread(xthreadInfo info) {
        throw new TODO_Exception();
//	WaitForSingleObject( (HANDLE)info.threadHandle, INFINITE);
//	CloseHandle( (HANDLE)info.threadHandle );
//	info.threadHandle = 0;
    }

    /*
     ==================
     Sys_Sentry
     ==================
     */
    public static void Sys_Sentry() {
        final int j = 0;
    }

    /*
     ==================
     Sys_GetThreadName
     ==================
     */
    public static String Sys_GetThreadName(int[] index) {
        throw new TODO_Exception();
//	int id = GetCurrentThreadId();
//	for( int i = 0; i < g_thread_count; i++ ) {
//		if ( id == g_threads[i]->threadId ) {
//			if ( index ) {
//				*index = i;
//			}
//			return g_threads[i]->name;
//		}
//	}
//	if ( index ) {
//		*index = -1;
//	}
//	return "main";
    }


    /*
     ==================
     Sys_EnterCriticalSection
     ==================
     */
    public static void Sys_EnterCriticalSection(int index) {
        assert ((index >= 0) && (index < MAX_CRITICAL_SECTIONS));
//		Sys_DebugPrintf( "busy lock '%s' in thread '%s'\n", lock->name, Sys_GetThreadName() );
        win32.criticalSections[index].lock();
    }

    public static void Sys_EnterCriticalSection() {
        Sys_EnterCriticalSection(CRITICAL_SECTION_ZERO);
    }

    /*
     ==================
     Sys_LeaveCriticalSection
     ==================
     */
    public static void Sys_LeaveCriticalSection(int index) {
        assert ((index >= 0) && (index < MAX_CRITICAL_SECTIONS));
        if (((ReentrantLock) win32.criticalSections[index]).isLocked()) {
            win32.criticalSections[index].unlock();
        }
    }

    public static void Sys_LeaveCriticalSection() {
        Sys_LeaveCriticalSection(CRITICAL_SECTION_ZERO);
    }

    /*
     ==================
     Sys_WaitForEvent
     ==================
     */
    public static void Sys_WaitForEvent(int index) {
        throw new TODO_Exception();
//	assert( index == 0 );
//	if ( !win32.backgroundDownloadSemaphore ) {
//		win32.backgroundDownloadSemaphore = CreateEvent( NULL, TRUE, FALSE, NULL );
//	}
//	WaitForSingleObject( win32.backgroundDownloadSemaphore, INFINITE );
//	ResetEvent( win32.backgroundDownloadSemaphore );
    }

    public static void Sys_WaitForEvent() {
        Sys_WaitForEvent(TRIGGER_EVENT_ZERO);
    }


    /*
     ==================
     Sys_TriggerEvent
     ==================
     */
    public static void Sys_TriggerEvent(int index) {
        throw new TODO_Exception();
//	assert( index == 0 );
//	SetEvent( win32.backgroundDownloadSemaphore );
    }

    public static void Sys_TriggerEvent() {
        Sys_TriggerEvent(TRIGGER_EVENT_ZERO);
    }

    /*
     ==================
     Sys_DebugMemory_f
     ==================
     */
    static void Sys_DebugMemory_f() {
        common.Printf("Total allocation %8dk in %d blocks\n", debug_total_alloc / 1024, debug_total_alloc_count);
        common.Printf("Current allocation %8dk in %d blocks\n", debug_current_alloc / 1024, debug_current_alloc_count);
    }

    /*
     ==================
     Sys_MemFrame
     ==================
     */
    static void Sys_MemFrame() {
        if (sys_showMallocs.GetInteger() != 0) {
            common.Printf("Frame: %8dk in %5d blocks\n", debug_frame_alloc / 1024, debug_frame_alloc_count);
        }

        debug_frame_alloc = 0;
        debug_frame_alloc_count = 0;
    }

    /*
     ==================
     Sys_FlushCacheMemory

     On windows, the vertex buffers are write combined, so they
     don't need to be flushed from the cache
     ==================
     */
    public static void Sys_FlushCacheMemory(Object base, int bytes) {
    }

    /*
     =============
     Sys_Error

     Show the early console as an error dialog
     =============
     */
    public static void Sys_Error(final String fmt, Object... arg) {
        final StringBuilder text = new StringBuilder(4096);

//	va_start( argptr, error );
//	vsprintf( text, error, argptr );
//	va_end( argptr);
        text.append(String.format(fmt, arg));

        Conbuf_AppendText(text.toString());
        Conbuf_AppendText("\n");

        Win_SetErrorText(text.toString());
        Sys_ShowConsole(1, true);

//        timeEndPeriod(1);
//
        Sys_ShutdownInput();

        GLimp_Shutdown();

//	// wait for the user to quit
        while (true) {
//            if (!GetMessage( & msg, NULL, 0, 0)) {
//                common->Quit();
//            }
//		TranslateMessage( &msg );
//      	DispatchMessage( &msg );
        }
//
//        Sys_DestroyConsole();
//
//        System.exit(1);
    }

    /*
     ==============
     Sys_Quit
     ==============
     */
    public static void Sys_Quit() {

//	timeEndPeriod( 1 );
        Sys_ShutdownInput();
        Sys_DestroyConsole();
        System.exit(0);//ExitProcess(0);
    }


    /*
     ==============
     Sys_Printf
     ==============
     */
    public static void Sys_Printf(final String fmt, Object... arg) {
        final StringBuilder msg = new StringBuilder(MAXPRINTMSG);
        msg.append(String.format(fmt, arg));

        if (Win32Vars_t.win_outputDebugString.GetBool()) {
            System.out.print(msg.toString());//OutputDebugString(msg);
        }
        if (Win32Vars_t.win_outputEditString.GetBool()) {
            Conbuf_AppendText(msg.toString());
        }
    }

    /*
     ==============
     Sys_DebugPrintf
     ==============
     */
    public static void Sys_DebugPrintf(final String fmt, Object... arg) {
        System.out.printf(fmt + '\n', arg);
    }

    /*
     ==============
     Sys_DebugVPrintf
     ==============
     */
    public static void Sys_DebugVPrintf(final String fmt, Object... arg) {
        throw new TODO_Exception();
//	char msg[MAXPRINTMSG];
//
//	idStr::vsnPrintf( msg, MAXPRINTMSG-1, fmt, arg );
//	msg[ sizeof(msg)-1 ] = '\0';
//
//	OutputDebugString( msg );
    }

    /*
     ==============
     Sys_Sleep
     ==============
     */
    public static void Sys_Sleep(int msec) {
        LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(msec));
    }

    /*
     ==============
     Sys_ShowWindow
     ==============
     */
    public static void Sys_ShowWindow(boolean show) {
        throw new TODO_Exception();
//	::ShowWindow( win32.hWnd, show ? SW_SHOW : SW_HIDE );
    }

    /*
     ==============
     Sys_IsWindowVisible
     ==============
     */
    public static boolean Sys_IsWindowVisible() {
        throw new TODO_Exception();
//	return ( ::IsWindowVisible( win32.hWnd ) != 0 );
    }

    /*
     ==============
     Sys_Mkdir
     ==============
     */
    public static void Sys_Mkdir(final String path) {
//	_mkdir (path);
        Paths.get(path).toFile().mkdir();
    }

    public static void Sys_Mkdir(final idStr path) {
        Sys_Mkdir(path.getData());
    }

    /*
     =================
     Sys_FileTimeStamp
     =================
     */
    public static long/*ID_TIME_T*/ Sys_FileTimeStamp(final String fp) {

        final File st = Paths.get(fp).toFile();

        if (st.exists()) {
//        Files.getLastModifiedTime(Paths.get(fp), LinkOption.NOFOLLOW_LINKS).toMillis();
            return st.lastModified();
        }
        return 0;
    }

    public static String Sys_Cwd() {
        final String cwd = System.getProperty("user.dir");

        return cwd;
    }

    /*
     ==============
     Sys_DefaultCDPath
     ==============
     */
    public static String Sys_DefaultCDPath() {
        return "";
    }

    /*
     ==============
     Sys_DefaultBasePath
     ==============
     */
    public static String Sys_DefaultBasePath() {
        return Sys_Cwd();
    }

    /*
     ==============
     Sys_DefaultSavePath
     ==============
     */
    public static String Sys_DefaultSavePath() {
        return cvarSystem.GetCVarString("fs_basepath");
    }


    /*
     ==============
     Sys_EXEPath
     ==============
     */
    public static String Sys_EXEPath() {
        throw new TODO_Exception();
//	static char exe[ MAX_OSPATH ];
//	GetModuleFileName( NULL, exe, sizeof( exe ) - 1 );
//	return exe;
    }

    /*
     ==============
     Sys_ListFiles
     ==============
     */
    public static int Sys_ListFiles(final String directory, final String extension, idStrList list) {
        FilenameFilter search;
        File /*_finddata_t*/ findinfo;
//	int			findhandle;
//        final boolean _A_SUBDIR;

        search = (pathname, name) -> {
            // passing a slash as extension will find directories
            if (extension.equals('/')) {
//                    _A_SUBDIR = false;
                return pathname.isDirectory();
            } else {
//                    _A_SUBDIR = true;
                return name.endsWith(extension);
            }
        };

        findinfo = new File(directory);

        // search
        list.Clear();

        if (!findinfo.exists()) {
            return -1;
        }

        for (final File findhandle : findinfo.listFiles(search)) {
//            if (_A_SUBDIR ^ (findinfo.isDirectory())) {
            list.Append(findhandle.getName());
//            }
        }

        return list.Num();
    }


    /*
     ================
     Sys_GetClipboardData
     ================
     */
    public static String Sys_GetClipboardData() {
        try {
            return (String) Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
//	char *data = NULL;
//	char *cliptext;
//
//	if ( OpenClipboard( NULL ) != 0 ) {
//		HANDLE hClipboardData;
//
//		if ( ( hClipboardData = GetClipboardData( CF_TEXT ) ) != 0 ) {
//			if ( ( cliptext = (char *)GlobalLock( hClipboardData ) ) != 0 ) {
//				data = (char *)Mem_Alloc( GlobalSize( hClipboardData ) + 1 );
//				strcpy( data, cliptext );
//				GlobalUnlock( hClipboardData );
//				
//				strtok( data, "\n\r\b" );
//			}
//		}
//		CloseClipboard();
//	}
//	return data;
        } catch (UnsupportedFlavorException | IOException ex) {
            Logger.getLogger(win_main.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    /*
     ================
     Sys_SetClipboardData
     ================
     */
    public static void Sys_SetClipboardData(final String string) {
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(string), null);
//	HGLOBAL HMem;
//	char *PMem;
//
//	// allocate memory block
//	HMem = (char *)::GlobalAlloc( GMEM_MOVEABLE | GMEM_DDESHARE, strlen( string ) + 1 );
//	if ( HMem == NULL ) {
//		return;
//	}
//	// lock allocated memory and obtain a pointer
//	PMem = (char *)::GlobalLock( HMem );
//	if ( PMem == NULL ) {
//		return;
//	}
//	// copy text into allocated memory block
//	lstrcpy( PMem, string );
//	// unlock allocated memory
//	::GlobalUnlock( HMem );
//	// open Clipboard
//	if ( !OpenClipboard( 0 ) ) {
//		::GlobalFree( HMem );
//		return;
//	}
//	// remove current Clipboard contents
//	EmptyClipboard();
//	// supply the memory handle to the Clipboard
//	SetClipboardData( CF_TEXT, HMem );
//	HMem = 0;
//	// close Clipboard
//	CloseClipboard();
    }

    public static void Sys_SetClipboardData(final char[] string) {
        Sys_SetClipboardData(ctos(string));
    }

    /*
     ========================================================================

     DLL Loading

     ========================================================================
     */

    /*
     =====================
     Sys_DLL_Load
     =====================
     */
    public static int Sys_DLL_Load(final String dllName) {
        throw new TODO_Exception();
//	HINSTANCE	libHandle;
//	libHandle = LoadLibrary( dllName );
//	if ( libHandle ) {
//		// since we can't have LoadLibrary load only from the specified path, check it did the right thing
//		char loadedPath[ MAX_OSPATH ];
//		GetModuleFileName( libHandle, loadedPath, sizeof( loadedPath ) - 1 );
//		if ( idStr::IcmpPath( dllName, loadedPath ) ) {
//			Sys_Printf( "ERROR: LoadLibrary '%s' wants to load '%s'\n", dllName, loadedPath );
//			Sys_DLL_Unload( (int)libHandle );
//			return 0;
//		}
//	}
//	return (int)libHandle;
    }

    /*
     =====================
     Sys_DLL_GetProcAddress
     =====================
     */
    public static Object Sys_DLL_GetProcAddress(int dllHandle, final String procName) {
        throw new TODO_Exception();
//	return GetProcAddress( (HINSTANCE)dllHandle, procName ); 
    }

    /*
     =====================
     Sys_DLL_Unload
     =====================
     */
    public static void Sys_DLL_Unload(int dllHandle) {
        throw new TODO_Exception();
//	if ( !dllHandle ) {
//		return;
//	}
//	if ( FreeLibrary( (HINSTANCE)dllHandle ) == 0 ) {
//		int lastError = GetLastError();
//		LPVOID lpMsgBuf;
//		FormatMessage(
//			FORMAT_MESSAGE_ALLOCATE_BUFFER,
//		    NULL,
//			lastError,
//			MAKELANGID(LANG_NEUTRAL, SUBLANG_DEFAULT), // Default language
//			(LPTSTR) &lpMsgBuf,
//			0,
//			NULL 
//		);
//		Sys_Error( "Sys_DLL_Unload: FreeLibrary failed - %s (%d)", lpMsgBuf, lastError );
//	}
    }

    /*
     ========================================================================

     EVENT LOOP

     ========================================================================
     */
    static sysEvent_s[] eventQue  = new sysEvent_s[MAX_QUED_EVENTS];
    static int          eventHead = 0;
    static int          eventTail = 0;

    /*
     ================
     Sys_QueEvent

     Ptr should either be null, or point to a block of data that can
     be freed by the game later.
     ================
     */
    public static void Sys_QueEvent(long time, sysEventType_t type, int value, int value2, int ptrLength, ByteBuffer ptr) {
        sysEvent_s ev;

        ev = eventQue[eventHead & MASK_QUED_EVENTS] = new sysEvent_s();

        if ((eventHead - eventTail) >= MAX_QUED_EVENTS) {
            common.Printf("Sys_QueEvent: overflow\n");
            // we are discarding an event, but don't leak memory
            if (ev.evPtr != null) {
                ev.evPtr.clear();//Mem_Free( ev->evPtr );
            }
            eventTail++;
        }

        eventHead++;

        ev.evType = type;
        ev.evValue = value;
        ev.evValue2 = value2;
        ev.evPtrLength = ptrLength;
        ev.evPtr = ptr;
    }

    /*
     =============
     Sys_PumpEvents

     This allows windows to be moved during renderbump
     =============
     */

    /**
     * @deprecated not needed for java
     */
    @Deprecated
    public static void Sys_PumpEvents() {
        throw new TODO_Exception();
//    MSG msg;
//
//	// pump the message loop
//	while( PeekMessage( &msg, NULL, 0, 0, PM_NOREMOVE ) ) {
//		if ( !GetMessage( &msg, NULL, 0, 0 ) ) {
//			common->Quit();
//		}
//
//		// save the msg time, because wndprocs don't have access to the timestamp
//		if ( win32.sysMsgTime && win32.sysMsgTime > (int)msg.time ) {
//			// don't ever let the event times run backwards	
////			common->Printf( "Sys_PumpEvents: win32.sysMsgTime (%i) > msg.time (%i)\n", win32.sysMsgTime, msg.time );
//		} else {
//			win32.sysMsgTime = msg.time;
//		}
//
//#ifdef ID_ALLOW_TOOLS
//		if ( GUIEditorHandleMessage ( &msg ) ) {	
//			continue;
//		}
//#endif
// 
//		TranslateMessage (&msg);
//      	DispatchMessage (&msg);
//	}
    }

    /*
     ================
     Sys_GenerateEvents
     ================
     */
    private static boolean entered = false;

    public static void Sys_GenerateEvents() {

        String s;

        if (entered) {
            return;
        }
        entered = true;

//        // pump the message loop
//        Sys_PumpEvents();
//
        // make sure mouse and joystick are only called once a frame
//        IN_Frame();//TODO:do we need this function?

        // check for console commands
        s = Sys_ConsoleInput();
        if (s != null) {
            int len;

            len = s.length();
            Sys_QueEvent(0, SE_CONSOLE, 0, 0, len, atobb(s));
        }

        entered = false;
    }

    /*
     ================
     Sys_ClearEvents
     ================
     */
    public static void Sys_ClearEvents() {
        eventHead = eventTail = 0;
    }

    /*
     ================
     Sys_GetEvent
     ================
     */
    public static sysEvent_s Sys_GetEvent() {
        sysEvent_s ev;

        // return if we have data
        if (eventHead > eventTail) {
            eventTail++;
            return eventQue[(eventTail - 1) & MASK_QUED_EVENTS];
        }

        // return the empty event 
//	memset( &ev, 0, sizeof( ev ) );
        ev = new sysEvent_s();

        return ev;
    }

//================================================================

    /*
     =================
     Sys_In_Restart_f

     Restart the input subsystem
     =================
     */
    public static class Sys_In_Restart_f extends cmdFunction_t {

        public static final cmdFunction_t INSTANCE = new Sys_In_Restart_f();

        private Sys_In_Restart_f() {
        }

        @Override
        public void run(idCmdArgs args) throws idException {
            Sys_ShutdownInput();
            Sys_InitInput();
        }
    }


    /*
     ==================
     Sys_AsyncThread
     ==================
     */
    static class Sys_AsyncThread extends xthread_t {

        int wakeNumber;
        int startTime;

        @Override
        public void run() {

        	System.out.println(NeoFixStrings.BLAAAAAAAAAAAAAAAAAA);
//            startTime = Sys_Milliseconds();
//            wakeNumber = 0;
//
//            while (true) {
//#ifdef WIN32	
//		// this will trigger 60 times a second
//		int r = WaitForSingleObject( hTimer, 100 );
//		if ( r != WAIT_OBJECT_0 ) {
//			OutputDebugString( "idPacketServer::PacketServerInterrupt: bad wait return" );
//		}
//#endif
//
//#if 0
//		wakeNumber++;
//		int		msec = Sys_Milliseconds();
//		int		deltaTime = msec - startTime;
//		startTime = msec;
//
//		char	str[1024];
//		sprintf( str, "%i ", deltaTime );
//		OutputDebugString( str );
//#endif
//
//
            common.Async();
//            }
        }
    }

    /*
     ==============
     Sys_StartAsyncThread

     Start the thread that will call idCommon::Async()
     ==============
     */private static int count = 0;
    public static void Sys_StartAsyncThread() {

        // create an auto-reset event that happens 60 times a second
//        hTimer = Executors.newSingleThreadScheduledExecutor(r -> new Thread(NeoFixStrings.BLA + (thread++)));
        hTimer = Executors.newSingleThreadScheduledExecutor(r -> {
            final Thread thread = new Thread(r, NeoFixStrings.BLA + "-" + (count++));
            thread.setPriority(Thread.MAX_PRIORITY);
            return thread;
        });
//        hTimer = Executors.newScheduledThreadPool(1);
        threadInfo = new xthreadInfo();
        if (null == hTimer) {
            common.Error("idPacketServer::Spawn: CreateWaitableTimer failed");
        }

//        Sys_CreateThread(new Sys_AsyncThread(), null, THREAD_ABOVE_NORMAL, threadInfo, "Async", g_threads, g_thread_count);
//        if (NOT(threadInfo.threadHandle)) {
//            common.Error("Sys_StartAsyncThread: failed");
//        }

//        hTimer.scheduleAtFixedRate(threadInfo.threadHandle, 0, USERCMD_MSEC, TimeUnit.MILLISECONDS);
        hTimer.scheduleAtFixedRate((Runnable) () -> {//TODO:debug the line above.(info.threadHandle.start();??)
//                if (!DEBUG) {//TODO:Session_local.java::742
            common.Async();
//                }
        }, 0, 1000000000L / 60, TimeUnit.NANOSECONDS);
//        if (SET_THREAD_AFFINITY) {
//            // give the async thread an affinity for the second cpu
//            SetThreadAffinityMask(threadInfo.threadHandle, 2);
//        }
    }

    /*
     ================
     Sys_AlreadyRunning

     returns true if there is a copy of D3 running already
     ================
     */
    public static boolean Sys_AlreadyRunning() {
        if (!TempDump.isDeadCodeTrue()) {
            return false;
        }
        throw new TODO_Exception();
//#ifndef DEBUG
//	if ( !win32.win_allowMultipleInstances.GetBool() ) {
//		HANDLE hMutexOneInstance = ::CreateMutex( NULL, FALSE, "DOOM3" );
//		if ( ::GetLastError() == ERROR_ALREADY_EXISTS || ::GetLastError() == ERROR_ACCESS_DENIED ) {
//			return true;
//		}
//	}
//#endif
//	return false;
    }

    /*
     ================
     Sys_Init

     The cvar system must already be setup
     ================
     */
    public static void Sys_Init() {
//
//        CoInitialize(null);
//
//        // make sure the timer is high precision, otherwise
//        // NT gets 18ms resolution
//        timeBeginPeriod(1);
//
        // get WM_TIMER messages pumped every millisecond
//	SetTimer( NULL, 0, 100, NULL );
        cmdSystem.AddCommand("in_restart", Sys_In_Restart_f.INSTANCE, CMD_FL_SYSTEM, "restarts the input system");
        if (DEBUG) {
            cmdSystem.AddCommand("createResourceIDs", CreateResourceIDs_f.INSTANCE, CMD_FL_TOOL, "assigns resource IDs in _resouce.h files");
        }
// #if 0
//	 cmdSystem.AddCommand( "setAsyncSound", Sys_SetAsyncSound_f, CMD_FL_SYSTEM, "set the async sound option" );
// #endif

        //
        // Windows user name
        //
        Win32Vars_t.win_username.SetString(Sys_GetCurrentUser());

        //
        // Windows version
        //
        Win32Vars_t.sys_arch.SetString(System.getProperty("os.name"));
//	win32.osversion.dwOSVersionInfoSize = sizeof( win32.osversion );
//
//	if ( !GetVersionEx( (LPOSVERSIONINFO)&win32.osversion ) )
//		Sys_Error( "Couldn't get OS info" );
//
//	if ( win32.osversion.dwMajorVersion < 4 ) {
//		Sys_Error( GAME_NAME " requires Windows version 4 (NT) or greater" );
//	}
//	if ( win32.osversion.dwPlatformId == VER_PLATFORM_WIN32s ) {
//		Sys_Error( GAME_NAME " doesn't run on Win32s" );
//	}
//
//	if( win32.osversion.dwPlatformId == VER_PLATFORM_WIN32_NT ) {
//		if( win32.osversion.dwMajorVersion <= 4 ) {
//			win32.sys_arch.SetString( "WinNT (NT)" );
//		} else if( win32.osversion.dwMajorVersion == 5 && win32.osversion.dwMinorVersion == 0 ) {
//			win32.sys_arch.SetString( "Win2K (NT)" );
//		} else if( win32.osversion.dwMajorVersion == 5 && win32.osversion.dwMinorVersion == 1 ) {
//			win32.sys_arch.SetString( "WinXP (NT)" );
//		} else if ( win32.osversion.dwMajorVersion == 6 ) {
//			win32.sys_arch.SetString( "Vista" );
//		} else {
//			win32.sys_arch.SetString( "Unknown NT variant" );
//		}
//	} else if( win32.osversion.dwPlatformId == VER_PLATFORM_WIN32_WINDOWS ) {
//		if( win32.osversion.dwMajorVersion == 4 && win32.osversion.dwMinorVersion == 0 ) {
//			// Win95
//			if( win32.osversion.szCSDVersion[1] == 'C' ) {
//				win32.sys_arch.SetString( "Win95 OSR2 (95)" );
//			} else {
//				win32.sys_arch.SetString( "Win95 (95)" );
//			}
//		} else if( win32.osversion.dwMajorVersion == 4 && win32.osversion.dwMinorVersion == 10 ) {
//			// Win98
//			if( win32.osversion.szCSDVersion[1] == 'A' ) {
//				win32.sys_arch.SetString( "Win98SE (95)" );
//			} else {
//				win32.sys_arch.SetString( "Win98 (95)" );
//			}
//		} else if( win32.osversion.dwMajorVersion == 4 && win32.osversion.dwMinorVersion == 90 ) {
//			// WinMe
//		  	win32.sys_arch.SetString( "WinMe (95)" );
//		} else {
//		  	win32.sys_arch.SetString( "Unknown 95 variant" );
//		}
//	} else {
//		win32.sys_arch.SetString( "unknown Windows variant" );
//	}

        //
        // CPU type
        //
        if (NOT(idStr.Icmp(Win32Vars_t.sys_cpustring.GetString(), "detect"))) {
            idStr string;

            common.Printf("%1.0f MHz ", Sys_ClockTicksPerSecond() / 1000000.0f);

            win32.cpuid = Sys_GetCPUId();

            string = new idStr();//Clear();

            if ((win32.cpuid & CPUID_AMD) != 0) {
                string.oPluSet("AMD CPU");
            } else if ((win32.cpuid & CPUID_INTEL) != 0) {
                string.oPluSet("Intel CPU");
            } else if ((win32.cpuid & CPUID_UNSUPPORTED) != 0) {
                string.oPluSet("unsupported CPU");
            } else {
                string.oPluSet("generic CPU");
            }

            string.oPluSet(" with ");
            if ((win32.cpuid & CPUID_MMX) != 0) {
                string.oPluSet("MMX & ");
            }
            if ((win32.cpuid & CPUID_3DNOW) != 0) {
                string.oPluSet("3DNow! & ");
            }
            if ((win32.cpuid & CPUID_SSE) != 0) {
                string.oPluSet("SSE & ");
            }
            if ((win32.cpuid & CPUID_SSE2) != 0) {
                string.oPluSet("SSE2 & ");
            }
            if ((win32.cpuid & CPUID_SSE3) != 0) {
                string.oPluSet("SSE3 & ");
            }
            if ((win32.cpuid & CPUID_HTT) != 0) {
                string.oPluSet("HTT & ");
            }
            string.StripTrailing(" & ");
            string.StripTrailing(" with ");
            Win32Vars_t.sys_cpustring.SetString(string.getData());
        } else {
            common.Printf("forcing CPU type to ");
            final idLexer src = new idLexer(Win32Vars_t.sys_cpustring.GetString(), Win32Vars_t.sys_cpustring.GetString().length(), "sys_cpustring");
            final idToken token = new idToken();

            int id = CPUID_NONE;
            while (src.ReadToken(token)) {
                if (token.Icmp("generic") == 0) {
                    id |= CPUID_GENERIC;
                } else if (token.Icmp("intel") == 0) {
                    id |= CPUID_INTEL;
                } else if (token.Icmp("amd") == 0) {
                    id |= CPUID_AMD;
                } else if (token.Icmp("mmx") == 0) {
                    id |= CPUID_MMX;
                } else if (token.Icmp("3dnow") == 0) {
                    id |= CPUID_3DNOW;
                } else if (token.Icmp("sse") == 0) {
                    id |= CPUID_SSE;
                } else if (token.Icmp("sse2") == 0) {
                    id |= CPUID_SSE2;
                } else if (token.Icmp("sse3") == 0) {
                    id |= CPUID_SSE3;
                } else if (token.Icmp("htt") == 0) {
                    id |= CPUID_HTT;
                }
            }
            if (id == CPUID_NONE) {
                common.Printf("WARNING: unknown sys_cpustring '%s'\n", Win32Vars_t.sys_cpustring.GetString());
                id = CPUID_GENERIC;
            }
            win32.cpuid = /*(cpuid_t)*/ id;
        }

        common.Printf("%s\n", Win32Vars_t.sys_cpustring.GetString());
        common.Printf("%d MB System Memory\n", Sys_GetSystemRam());
        common.Printf("%d MB Video Memory\n", Sys_GetVideoRam());
    }

    /*
     ================
     Sys_Shutdown
     ================
     */
    public static void Sys_Shutdown() {
        throw new TODO_Exception();
//        CoUninitialize();
    }

    /*
     ================
     Sys_GetProcessorId
     ================
     */
    public static int/*cpuid_t*/ Sys_GetProcessorId() {
        return win32.cpuid;
    }

    /*
     ================
     Sys_GetProcessorString
     ================
     */
    public static String Sys_GetProcessorString() {
        throw new TODO_Exception();
//	return win32.sys_cpustring.GetString();
    }

    //=======================================================================
//#define SET_THREAD_AFFINITY
    /*
     ====================
     Win_Frame
     ====================
     */
    public static void Win_Frame() {

        // if "viewlog" has been modified, show or hide the log console
        if (win_viewlog.IsModified()) {
            if (!com_skipRenderer.GetBool() && (idAsyncNetwork.serverDedicated.GetInteger() != 1)) {
                Sys_ShowConsole(win_viewlog.GetInteger(), false);
            }
            win_viewlog.ClearModified();
        }
    }


    /*
     ====================
     TestChkStk
     ====================
     */
    public static void TestChkStk() {
        throw new TODO_Exception();
//	int		buffer[0x1000];
//
//	buffer[0] = 1;
    }

    /*
     ====================
     HackChkStk
     ====================
     */
    public static void HackChkStk() {
        throw new TODO_Exception();
//	DWORD	old;
//	VirtualProtect( _chkstk, 6, PAGE_EXECUTE_READWRITE, &old );
//	*(byte *)_chkstk = 0xe9;
//	*(int *)((int)_chkstk+1) = (int)clrstk - (int)_chkstk - 5;
//
//	TestChkStk();
    }

    /*
     ====================
     GetExceptionCodeInfo
     ====================
     */
    public static String GetExceptionCodeInfo(int/*UINT*/ code) {
        throw new TODO_Exception();
//	switch( code ) {
//		case EXCEPTION_ACCESS_VIOLATION: return "The thread tried to read from or write to a virtual address for which it does not have the appropriate access.";
//		case EXCEPTION_ARRAY_BOUNDS_EXCEEDED: return "The thread tried to access an array element that is out of bounds and the underlying hardware supports bounds checking.";
//		case EXCEPTION_BREAKPOINT: return "A breakpoint was encountered.";
//		case EXCEPTION_DATATYPE_MISALIGNMENT: return "The thread tried to read or write data that is misaligned on hardware that does not provide alignment. For example, 16-bit values must be aligned on 2-byte boundaries; 32-bit values on 4-byte boundaries, and so on.";
//		case EXCEPTION_FLT_DENORMAL_OPERAND: return "One of the operands in a floating-point operation is denormal. A denormal value is one that is too small to represent as a standard floating-point value.";
//		case EXCEPTION_FLT_DIVIDE_BY_ZERO: return "The thread tried to divide a floating-point value by a floating-point divisor of zero.";
//		case EXCEPTION_FLT_INEXACT_RESULT: return "The result of a floating-point operation cannot be represented exactly as a decimal fraction.";
//		case EXCEPTION_FLT_INVALID_OPERATION: return "This exception represents any floating-point exception not included in this list.";
//		case EXCEPTION_FLT_OVERFLOW: return "The exponent of a floating-point operation is greater than the magnitude allowed by the corresponding type.";
//		case EXCEPTION_FLT_STACK_CHECK: return "The stack overflowed or underflowed as the result of a floating-point operation.";
//		case EXCEPTION_FLT_UNDERFLOW: return "The exponent of a floating-point operation is less than the magnitude allowed by the corresponding type.";
//		case EXCEPTION_ILLEGAL_INSTRUCTION: return "The thread tried to execute an invalid instruction.";
//		case EXCEPTION_IN_PAGE_ERROR: return "The thread tried to access a page that was not present, and the system was unable to load the page. For example, this exception might occur if a network connection is lost while running a program over the network.";
//		case EXCEPTION_INT_DIVIDE_BY_ZERO: return "The thread tried to divide an integer value by an integer divisor of zero.";
//		case EXCEPTION_INT_OVERFLOW: return "The result of an integer operation caused a carry out of the most significant bit of the result.";
//		case EXCEPTION_INVALID_DISPOSITION: return "An exception handler returned an invalid disposition to the exception dispatcher. Programmers using a high-level language such as C should never encounter this exception.";
//		case EXCEPTION_NONCONTINUABLE_EXCEPTION: return "The thread tried to continue execution after a noncontinuable exception occurred.";
//		case EXCEPTION_PRIV_INSTRUCTION: return "The thread tried to execute an instruction whose operation is not allowed in the current machine mode.";
//		case EXCEPTION_SINGLE_STEP: return "A trace trap or other single-instruction mechanism signaled that one instruction has been executed.";
//		case EXCEPTION_STACK_OVERFLOW: return "The thread used up its stack.";
//		default: return "Unknown exception";
//	}
    }

    /*
     ====================
     EmailCrashReport

     emailer originally from Raven/Quake 4
     ====================
     */
//public static void EmailCrashReport( LPSTR messageText ) {throw new TODO_Exception();
//	LPMAPISENDMAIL	MAPISendMail;
//	MapiMessage		message;
//	static int lastEmailTime = 0;
//
//	if ( Sys_Milliseconds() < lastEmailTime + 10000 ) {
//		return;
//	}
//
//	lastEmailTime = Sys_Milliseconds();
//
//	HINSTANCE mapi = LoadLibrary( "MAPI32.DLL" ); 
//	if( mapi ) {
//		MAPISendMail = ( LPMAPISENDMAIL )GetProcAddress( mapi, "MAPISendMail" );
//		if( MAPISendMail ) {
//			MapiRecipDesc toProgrammers =
//			{
//				0,										// ulReserved
//					MAPI_TO,							// ulRecipClass
//					"DOOM 3 Crash",						// lpszName
//					"SMTP:programmers@idsoftware.com",	// lpszAddress
//					0,									// ulEIDSize
//					0									// lpEntry
//			};
//
//			memset( &message, 0, sizeof( message ) );
//			message.lpszSubject = "DOOM 3 Fatal Error";
//			message.lpszNoteText = messageText;
//			message.nRecipCount = 1;
//			message.lpRecips = &toProgrammers;
//
//			MAPISendMail(
//				0,									// LHANDLE lhSession
//				0,									// ULONG ulUIParam
//				&message,							// lpMapiMessage lpMessage
//				MAPI_DIALOG,						// FLAGS flFlags
//				0									// ULONG ulReserved
//				);
//		}
//		FreeLibrary( mapi );
//	}
//}
    /*
     ====================
     _except_handler
     ====================
     */
//public static EXCEPTION_DISPOSITION __cdecl _except_handler( struct _EXCEPTION_RECORD *ExceptionRecord, void * EstablisherFrame,
//												struct _CONTEXT *ContextRecord, void * DispatcherContext ) {throw new TODO_Exception();
//
//	static char msg[ 8192 ];
//	char FPUFlags[2048];
//
//	Sys_FPU_PrintStateFlags( FPUFlags, ContextRecord->FloatSave.ControlWord,
//										ContextRecord->FloatSave.StatusWord,
//										ContextRecord->FloatSave.TagWord,
//										ContextRecord->FloatSave.ErrorOffset,
//										ContextRecord->FloatSave.ErrorSelector,
//										ContextRecord->FloatSave.DataOffset,
//										ContextRecord->FloatSave.DataSelector );
//
//
//	sprintf( msg, 
//		"Please describe what you were doing when DOOM 3 crashed!\n"
//		"If this text did not pop into your email client please copy and email it to programmers@idsoftware.com\n"
//			"\n"
//			"-= FATAL EXCEPTION =-\n"
//			"\n"
//			"%s\n"
//			"\n"
//			"0x%x at address 0x%08x\n"
//			"\n"
//			"%s\n"
//			"\n"
//			"EAX = 0x%08x EBX = 0x%08x\n"
//			"ECX = 0x%08x EDX = 0x%08x\n"
//			"ESI = 0x%08x EDI = 0x%08x\n"
//			"EIP = 0x%08x ESP = 0x%08x\n"
//			"EBP = 0x%08x EFL = 0x%08x\n"
//			"\n"
//			"CS = 0x%04x\n"
//			"SS = 0x%04x\n"
//			"DS = 0x%04x\n"
//			"ES = 0x%04x\n"
//			"FS = 0x%04x\n"
//			"GS = 0x%04x\n"
//			"\n"
//			"%s\n",
//			com_version.GetString(),
//			ExceptionRecord->ExceptionCode,
//			ExceptionRecord->ExceptionAddress,
//			GetExceptionCodeInfo( ExceptionRecord->ExceptionCode ),
//			ContextRecord->Eax, ContextRecord->Ebx,
//			ContextRecord->Ecx, ContextRecord->Edx,
//			ContextRecord->Esi, ContextRecord->Edi,
//			ContextRecord->Eip, ContextRecord->Esp,
//			ContextRecord->Ebp, ContextRecord->EFlags,
//			ContextRecord->SegCs,
//			ContextRecord->SegSs,
//			ContextRecord->SegDs,
//			ContextRecord->SegEs,
//			ContextRecord->SegFs,
//			ContextRecord->SegGs,
//			FPUFlags
//		);
//
//	EmailCrashReport( msg );
//	common->FatalError( msg );
//
//    // Tell the OS to restart the faulting instruction
//    return ExceptionContinueExecution;
//}

    /*
     ==================
     WinMain
     ==================
     */
//public static int WINAPI WinMain( HINSTANCE hInstance, HINSTANCE hPrevInstance, LPSTR lpCmdLine, int nCmdShow ) {
//
//	const HCURSOR hcurSave = ::SetCursor( LoadCursor( 0, IDC_WAIT ) );
//
//	Sys_SetPhysicalWorkMemory( 192 << 20, 1024 << 20 );
//
//	Sys_GetCurrentMemoryStatus( exeLaunchMemoryStats );
//
//#if 0
//    DWORD handler = (DWORD)_except_handler;
//    __asm
//    {                           // Build EXCEPTION_REGISTRATION record:
//        push    handler         // Address of handler function
//        push    FS:[0]          // Address of previous handler
//        mov     FS:[0],ESP      // Install new EXECEPTION_REGISTRATION
//    }
//#endif
//
//	win32.hInstance = hInstance;
//	idStr::Copynz( sys_cmdline, lpCmdLine, sizeof( sys_cmdline ) );
//
//	// done before Com/Sys_Init since we need this for error output
//	Sys_CreateConsole();
//
//	// no abort/retry/fail errors
//	SetErrorMode( SEM_FAILCRITICALERRORS );
//
//	for ( int i = 0; i < MAX_CRITICAL_SECTIONS; i++ ) {
//		InitializeCriticalSection( &win32.criticalSections[i] );
//	}
//
//	// get the initial time base
//	Sys_Milliseconds();
//
//#ifdef DEBUG
//	// disable the painfully slow MS heap check every 1024 allocs
//	_CrtSetDbgFlag( 0 );
//#endif
//
////	Sys_FPU_EnableExceptions( TEST_FPU_EXCEPTIONS );
//	Sys_FPU_SetPrecision( FPU_PRECISION_DOUBLE_EXTENDED );
//
//	common->Init( 0, NULL, lpCmdLine );
//
//#if TEST_FPU_EXCEPTIONS != 0
//	common->Printf( Sys_FPU_GetState() );
//#endif
//
//#ifndef	ID_DEDICATED
//	if ( win32.win_notaskkeys.GetInteger() ) {
//		DisableTaskKeys( TRUE, FALSE, /*( win32.win_notaskkeys.GetInteger() == 2 )*/ FALSE );
//	}
//#endif
//
//	Sys_StartAsyncThread();
//
//	// hide or show the early console as necessary
//	if ( win32.win_viewlog.GetInteger() || com_skipRenderer.GetBool() || idAsyncNetwork::serverDedicated.GetInteger() ) {
//		Sys_ShowConsole( 1, true );
//	} else {
//		Sys_ShowConsole( 0, false );
//	}
//
//#ifdef SET_THREAD_AFFINITY 
//	// give the main thread an affinity for the first cpu
//	SetThreadAffinityMask( GetCurrentThread(), 1 );
//#endif
//
//	::SetCursor( hcurSave );
//
//	// Launch the script debugger
//	if ( strstr( lpCmdLine, "+debugger" ) ) {
//		// DebuggerClientInit( lpCmdLine );
//		return 0;
//	}
//
//	::SetFocus( win32.hWnd );
//
//    // main game loop
//	while( 1 ) {
//
//		Win_Frame();
//
//#ifdef DEBUG
//		Sys_MemFrame();
//#endif
//
//		// set exceptions, even if some crappy syscall changes them!
//		Sys_FPU_EnableExceptions( TEST_FPU_EXCEPTIONS );
//
//#ifdef ID_ALLOW_TOOLS
//		if ( com_editors ) {
//			if ( com_editors & EDITOR_GUI ) {
//				// GUI editor
//				GUIEditorRun();
//			} else if ( com_editors & EDITOR_RADIANT ) {
//				// Level Editor
//				RadiantRun();
//			}
//			else if (com_editors & EDITOR_MATERIAL ) {
//				//BSM Nerve: Add support for the material editor
//				MaterialEditorRun();
//			}
//			else {
//				if ( com_editors & EDITOR_LIGHT ) {
//					// in-game Light Editor
//					LightEditorRun();
//				}
//				if ( com_editors & EDITOR_SOUND ) {
//					// in-game Sound Editor
//					SoundEditorRun();
//				}
//				if ( com_editors & EDITOR_DECL ) {
//					// in-game Declaration Browser
//					DeclBrowserRun();
//				}
//				if ( com_editors & EDITOR_AF ) {
//					// in-game Articulated Figure Editor
//					AFEditorRun();
//				}
//				if ( com_editors & EDITOR_PARTICLE ) {
//					// in-game Particle Editor
//					ParticleEditorRun();
//				}
//				if ( com_editors & EDITOR_SCRIPT ) {
//					// in-game Script Editor
//					ScriptEditorRun();
//				}
//				if ( com_editors & EDITOR_PDA ) {
//					// in-game PDA Editor
//					PDAEditorRun();
//				}
//			}
//		}
//#endif
//		// run the game
//		common->Frame();
//	}
//
//	// never gets here
//	return 0;
//}

    /*
     ====================
     clrstk

     I tried to get the run time to call this at every function entry, but
     ====================
     */
    static int parmBytes;

    public static /*__declspec( naked )*/ void clrstk() {
        throw new TODO_Exception();
//	// eax = bytes to add to stack
//	__asm {
//		mov		[parmBytes],eax
//        neg     eax                     ; compute new stack pointer in eax
//        add     eax,esp
//        add     eax,4
//        xchg    eax,esp
//        mov     eax,dword ptr [eax]		; copy the return address
//        push    eax
//        
//        ; clear to zero
//        push	edi
//        push	ecx
//        mov		edi,esp
//        add		edi,12
//        mov		ecx,[parmBytes]
//		shr		ecx,2
//        xor		eax,eax
//		cld
//        rep	stosd
//        pop		ecx
//        pop		edi
//        
//        ret
//	}
    }

    /*
     ==================
     Sys_SetFatalError
     ==================
     */
    public static void Sys_SetFatalError(final String error) {
    }

    public static void Sys_SetFatalError(final char[] error) {
        Sys_SetFatalError(ctos(error));
    }

    /*
     ==================
     Sys_DoPreferences
     ==================
     */
    public static void Sys_DoPreferences() {
    }

    public static boolean remove(final String path) {
        return Paths.get(path).toFile().delete();
    }

    public static boolean remove(final idStr path) {
        return remove(path.getData());
    }

    public static FileChannel tmpfile() throws IOException {

        final File tmp = File.createTempFile(NeoFixStrings.BLA, NeoFixStrings.BLA);
        tmp.deleteOnExit();

        return FileChannel.open(tmp.toPath(), fopenOptions("wb+"));
    }

    /*
     *
     *
     *
     *                    _        
     *                   (_)       
     *  _ __ ___    __ _  _  _ __  
     * | '_ ` _ \  / _` || || '_ \ 
     * | | | | | || (_| || || | | |
     * |_| |_| |_| \__,_||_||_| |_|
     *                      
     *
     *
     *
     *
     *
     *
     *
     */
//    static final int TEST_FPU_EXCEPTIONS
//            /*	=FPU_EXCEPTION_INVALID_OPERATION |		*/
//            /*	FPU_EXCEPTION_DENORMALIZED_OPERAND |	*/
//            /*	FPU_EXCEPTION_DIVIDE_BY_ZERO |			*/
//            /*	FPU_EXCEPTION_NUMERIC_OVERFLOW |		*/
//            /*	FPU_EXCEPTION_NUMERIC_UNDERFLOW |		*/
//            /*	FPU_EXCEPTION_INEXACT_RESULT |			*/
//            = 0;
    static final boolean SET_THREAD_AFFINITY = false;

    public static void main(String[] lpCmdLine) {//cmd arguments need to be escaped and surrounded by quotes to preserve spacing.
        // TODO: check if any of the disabled commands below can be salvaged for java.

//	const HCURSOR hcurSave = ::SetCursor( LoadCursor( 0, IDC_WAIT ) );
//
//	Sys_SetPhysicalWorkMemory( 192 << 20, 1024 << 20 );
//
//	Sys_GetCurrentMemoryStatus( exeLaunchMemoryStats );
//
//#if 0
//    DWORD handler = (DWORD)_except_handler;
//    __asm
//    {                           // Build EXCEPTION_REGISTRATION record:
//        push    handler         // Address of handler function
//        push    FS:[0]          // Address of previous handler
//        mov     FS:[0],ESP      // Install new EXECEPTION_REGISTRATION
//    }
//#endif
//
//	win32.hInstance = hInstance;
        idStr.Copynz(sys_cmdline, lpCmdLine);

        // done before Com/Sys_Init since we need this for error output
        Sys_CreateConsole();

//        // no abort/retry/fail errors
//        SetErrorMode(SEM_FAILCRITICALERRORS);
//
        for (int i = 0; i < MAX_CRITICAL_SECTIONS; i++) {
//            InitializeCriticalSection( &win32.criticalSections[i] );
            win32.criticalSections[i] = new ReentrantLock();//TODO: see if we can use synchronized blocks instead?
        }
        // get the initial time base
        Sys_Milliseconds();
//
//        if (DEBUG) {
//            // disable the painfully slow MS heap check every 1024 allocs
//            _CrtSetDbgFlag(0);
//        }
//
//	Sys_FPU_EnableExceptions( TEST_FPU_EXCEPTIONS );
//        Sys_FPU_SetPrecision(etoi(FPU_PRECISION_DOUBLE_EXTENDED));

        common.Init(0, null, sys_cmdline.toString());
//
//        if (TEST_FPU_EXCEPTIONS != 0) {
//            common.Printf(Sys_FPU_GetState());
//        }
//
//        if (ID_DEDICATED) {
//            if (win32.win_notaskkeys.GetInteger() != 0) {
//                DisableTaskKeys(true, false, /*( win32.win_notaskkeys.GetInteger() == 2 )*/ false);
//            }
//        }
//
        Sys_StartAsyncThread();

        // hide or show the early console as necessary
        if ((Win32Vars_t.win_viewlog.GetInteger() != 0)
                || com_skipRenderer.GetBool()
                || (idAsyncNetwork.serverDedicated.GetInteger() != 0)) {
            Sys_ShowConsole(1, true);
        } else {
            Sys_ShowConsole(0, false);
        }
//
//        if (SET_THREAD_AFFINITY) {
//            // give the main thread an affinity for the first cpu
//            SetThreadAffinityMask(GetCurrentThread(), 1);
//        }
//
//	::SetCursor( hcurSave );
//
        // Launch the script debugger
//        if ( strstr( lpCmdLine, "+debugger" ) ) {
        if (sys_cmdline.indexOf("+debugger") == 0) {
            // DebuggerClientInit( lpCmdLine );
            Sys_ShowConsole(1, true);
            return;//0;
        }
//
//	::SetFocus( win32.hWnd );
//
        // main game loop
        while (true) {

            Win_Frame();

            if (DEBUG) {
                Sys_MemFrame();
            }
//
//            // set exceptions, even if some crappy syscall changes them!
//            Sys_FPU_EnableExceptions(TEST_FPU_EXCEPTIONS);
//
            if (ID_ALLOW_TOOLS) {
                if (com_editors != 0) {
                    if ((com_editors & EDITOR_GUI) != 0) {
                        // GUI editor
                        GUIEditorRun();
                    } else if ((com_editors & EDITOR_RADIANT) != 0) {
                        // Level Editor
                        RadiantRun();
                    } else if ((com_editors & EDITOR_MATERIAL) != 0) {
                        //BSM Nerve: Add support for the material editor
                        MaterialEditorRun();
                    } else {
                        if ((com_editors & EDITOR_LIGHT) != 0) {
                            // in-game Light Editor
                            LightEditorRun();
                        }
                        if ((com_editors & EDITOR_SOUND) != 0) {
                            // in-game Sound Editor
                            SoundEditorRun();
                        }
                        if ((com_editors & EDITOR_DECL) != 0) {
                            // in-game Declaration Browser
                            DeclBrowserRun();
                        }
                        if ((com_editors & EDITOR_AF) != 0) {
                            // in-game Articulated Figure Editor
                            AFEditorRun();
                        }
                        if ((com_editors & EDITOR_PARTICLE) != 0) {
                            // in-game Particle Editor
                            ParticleEditorRun();
                        }
                        if ((com_editors & EDITOR_SCRIPT) != 0) {
                            // in-game Script Editor
                            ScriptEditorRun();
                        }
                        if ((com_editors & EDITOR_PDA) != 0) {
                            // in-game PDA Editor
                            PDAEditorRun();
                        }
                    }
                }
            }
            // run the game
            common.Frame();
        }

        // never gets here
//	return 0;
    }

}
