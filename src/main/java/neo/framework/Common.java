package neo.framework;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import static neo.Game.GameSys.SysCvar.__DATE__;
import static neo.Game.Game_local.game;
import neo.Renderer.GuiModel;
import static neo.Renderer.Image.globalImages;
import static neo.Renderer.RenderSystem.SCREEN_HEIGHT;
import static neo.Renderer.RenderSystem.SCREEN_WIDTH;
import static neo.Renderer.RenderSystem.SMALLCHAR_WIDTH;
import static neo.Renderer.RenderSystem.renderSystem;
import static neo.Sound.snd_system.soundSystem;
import neo.Sound.sound.idSoundWorld;
import static neo.TempDump.NOT;
import neo.TempDump.TODO_Exception;
import static neo.TempDump.bbtocb;
import static neo.TempDump.etoi;
import static neo.TempDump.isNotNullOrEmpty;
import neo.TempDump.void_callback;
import neo.Tools.Compilers.AAS.AASBuild.RunAASDir_f;
import neo.Tools.Compilers.AAS.AASBuild.RunAAS_f;
import neo.Tools.Compilers.AAS.AASBuild.RunReach_f;
import neo.Tools.Compilers.DMap.dmap.Dmap_f;
import neo.Tools.Compilers.RenderBump.renderbump.RenderBumpFlat_f;
import neo.Tools.Compilers.RenderBump.renderbump.RenderBump_f;
import neo.Tools.Compilers.RoqVQ.Roq.RoQFileEncode_f;
import static neo.Tools.edit_public.AFEditorInit;
import static neo.Tools.edit_public.DeclBrowserInit;
import static neo.Tools.edit_public.GUIEditorInit;
import static neo.Tools.edit_public.LightEditorInit;
import static neo.Tools.edit_public.MaterialEditorInit;
import static neo.Tools.edit_public.PDAEditorInit;
import static neo.Tools.edit_public.ParticleEditorInit;
import static neo.Tools.edit_public.RadiantInit;
import static neo.Tools.edit_public.ScriptEditorInit;
import static neo.Tools.edit_public.SoundEditorInit;
import neo.framework.Async.AsyncNetwork.idAsyncNetwork;
import static neo.framework.BuildDefines.ID_ALLOW_TOOLS;
import static neo.framework.BuildDefines.ID_DEDICATED;
import static neo.framework.BuildDefines.ID_DEMO_BUILD;
import static neo.framework.BuildDefines.ID_ENFORCE_KEY;
import static neo.framework.BuildDefines.MACOS_X;
import static neo.framework.BuildDefines._DEBUG;
import static neo.framework.BuildDefines._WIN32;
import static neo.framework.BuildDefines.__linux__;
import static neo.framework.BuildVersion.BUILD_NUMBER;
import static neo.framework.CVarSystem.CVAR_ARCHIVE;
import static neo.framework.CVarSystem.CVAR_BOOL;
import static neo.framework.CVarSystem.CVAR_FLOAT;
import static neo.framework.CVarSystem.CVAR_INIT;
import static neo.framework.CVarSystem.CVAR_INTEGER;
import static neo.framework.CVarSystem.CVAR_NOCHEAT;
import static neo.framework.CVarSystem.CVAR_ROM;
import static neo.framework.CVarSystem.CVAR_SERVERINFO;
import static neo.framework.CVarSystem.CVAR_SYSTEM;
import static neo.framework.CVarSystem.cvarSystem;
import neo.framework.CVarSystem.idCVar;
import static neo.framework.CmdSystem.CMD_FL_CHEAT;
import static neo.framework.CmdSystem.CMD_FL_SYSTEM;
import static neo.framework.CmdSystem.CMD_FL_TOOL;
import static neo.framework.CmdSystem.cmdExecution_t.CMD_EXEC_APPEND;
import static neo.framework.CmdSystem.cmdExecution_t.CMD_EXEC_NOW;
import neo.framework.CmdSystem.cmdFunction_t;
import static neo.framework.CmdSystem.cmdSystem;
import neo.framework.CmdSystem.idCmdSystem;
import neo.framework.CmdSystem.idCmdSystem.ArgCompletion_Integer;
import neo.framework.Common.Com_Crash_f;
import neo.framework.Common.Com_EditAFs_f;
import neo.framework.Common.Com_EditDecls_f;
import neo.framework.Common.Com_EditGUIs_f;
import neo.framework.Common.Com_EditLights_f;
import neo.framework.Common.Com_EditPDAs_f;
import neo.framework.Common.Com_EditParticles_f;
import neo.framework.Common.Com_EditScripts_f;
import neo.framework.Common.Com_EditSounds_f;
import neo.framework.Common.Com_Editor_f;
import neo.framework.Common.Com_Error_f;
import neo.framework.Common.Com_ExecMachineSpec_f;
import neo.framework.Common.Com_FinishBuild_f;
import neo.framework.Common.Com_Freeze_f;
import neo.framework.Common.Com_LocalizeGuiParmsTest_f;
import neo.framework.Common.Com_LocalizeGuis_f;
import neo.framework.Common.Com_LocalizeMapsTest_f;
import neo.framework.Common.Com_LocalizeMaps_f;
import neo.framework.Common.Com_MaterialEditor_f;
import neo.framework.Common.Com_Quit_f;
import neo.framework.Common.Com_ReloadEngine_f;
import neo.framework.Common.Com_ScriptDebugger_f;
import neo.framework.Common.Com_SetMachineSpec_f;
import neo.framework.Common.Com_StartBuild_f;
import neo.framework.Common.Com_WriteConfig_f;
import neo.framework.Common.PrintMemInfo_f;
import static neo.framework.Common.errorParm_t.ERP_DISCONNECT;
import static neo.framework.Common.errorParm_t.ERP_DROP;
import static neo.framework.Common.errorParm_t.ERP_FATAL;
import neo.framework.Compressor.idCompressor;
import static neo.framework.Console.console;
import static neo.framework.DeclManager.declManager;
import static neo.framework.EventLoop.eventLoop;
import static neo.framework.FileSystem_h.fileSystem;
import neo.framework.FileSystem_h.idFileList;
import neo.framework.File_h.idFile;
import neo.framework.File_h.idFile_Memory;
import neo.framework.KeyInput.idKeyInput;
import static neo.framework.Licensee.CONFIG_FILE;
import static neo.framework.Licensee.CONFIG_SPEC;
import static neo.framework.Licensee.ENGINE_VERSION;
import static neo.framework.Licensee.GAME_NAME;
import static neo.framework.Session.session;
import static neo.framework.UsercmdGen.USERCMD_MSEC;
import static neo.framework.UsercmdGen.usercmdGen;
import neo.idlib.CmdArgs.idCmdArgs;
import neo.idlib.Dict_h.idDict;
import neo.idlib.Dict_h.idKeyValue;
import neo.idlib.LangDict.idLangDict;
import neo.idlib.LangDict.idLangKeyValue;
import static neo.idlib.Lib.BIT;
import neo.idlib.Lib.idException;
import neo.idlib.Lib.idLib;
import neo.idlib.MapFile.idMapEntity;
import neo.idlib.MapFile.idMapFile;
import neo.idlib.Text.Base64.idBase64;
import static neo.idlib.Text.Lexer.LEXFL_ALLOWBACKSLASHSTRINGCONCAT;
import static neo.idlib.Text.Lexer.LEXFL_ALLOWMULTICHARLITERALS;
import static neo.idlib.Text.Lexer.LEXFL_NOFATALERRORS;
import static neo.idlib.Text.Lexer.LEXFL_NOSTRINGCONCAT;
import neo.idlib.Text.Lexer.idLexer;
import static neo.idlib.Text.Str.S_COLOR_RED;
import static neo.idlib.Text.Str.S_COLOR_YELLOW;
import neo.idlib.Text.Str.idStr;
import static neo.idlib.Text.Str.va;
import static neo.idlib.Text.Token.TT_STRING;
import neo.idlib.Text.Token.idToken;
import neo.idlib.containers.HashTable.idHashTable;
import neo.idlib.containers.StrList.idStrList;
import neo.idlib.math.Simd.idSIMD;
import neo.idlib.math.Vector.idVec4;
import neo.sys.sys_public;
import static neo.sys.sys_public.BUILD_STRING;
import static neo.sys.sys_public.CPUID_AMD;
import static neo.sys.sys_public.Sys_ListFiles;
import static neo.sys.win_cpu.Sys_ClockTicksPerSecond;
import static neo.sys.win_input.Sys_GrabMouseCursor;
import static neo.sys.win_input.Sys_InitScanTable;
import static neo.sys.win_main.DEBUG;
import static neo.sys.win_main.Sys_AlreadyRunning;
import static neo.sys.win_main.Sys_DoPreferences;
import static neo.sys.win_main.Sys_EnterCriticalSection;
import static neo.sys.win_main.Sys_Error;
import static neo.sys.win_main.Sys_GenerateEvents;
import static neo.sys.win_main.Sys_GetProcessorId;
import static neo.sys.win_main.Sys_Init;
import static neo.sys.win_main.Sys_LeaveCriticalSection;
import static neo.sys.win_main.Sys_Printf;
import static neo.sys.win_main.Sys_Quit;
import static neo.sys.win_main.Sys_SetClipboardData;
import static neo.sys.win_main.Sys_SetFatalError;
import static neo.sys.win_net.Sys_InitNetworking;
import static neo.sys.win_shared.Sys_GetSystemRam;
import static neo.sys.win_shared.Sys_GetVideoRam;
import static neo.sys.win_shared.Sys_Milliseconds;
import static neo.sys.win_syscon.Sys_ShowConsole;
import static neo.ui.UserInterface.uiManager;

/**
 *
 */
public class Common {

    public static final int EDITOR_NONE = 0;
    public static final int EDITOR_RADIANT = BIT(1);
    public static final int EDITOR_GUI = BIT(2);
    public static final int EDITOR_DEBUGGER = BIT(3);
    public static final int EDITOR_SCRIPT = BIT(4);
    public static final int EDITOR_LIGHT = BIT(5);
    public static final int EDITOR_SOUND = BIT(6);
    public static final int EDITOR_DECL = BIT(7);
    public static final int EDITOR_AF = BIT(8);
    public static final int EDITOR_PARTICLE = BIT(9);
    public static final int EDITOR_PDA = BIT(10);
    public static final int EDITOR_AAS = BIT(11);
    public static final int EDITOR_MATERIAL = BIT(12);
    //    
    //    
    public static final String STRTABLE_ID = "#str_";
    public static final int STRTABLE_ID_LENGTH = STRTABLE_ID.length();//5
    //
    private static idCommonLocal commonLocal = new idCommonLocal();
    public static /*final*/ idCommon common = commonLocal;
    static final boolean ID_WRITE_VERSION = false;

    public static class MemInfo_t {

        public idStr filebase;
//
        public int total;
        public int assetTotals;
//  
        // memory manager totals
        public int memoryManagerTotal;
//        
        // subsystem totals
        public int gameSubsystemTotal;
        public int renderSubsystemTotal;
//        
        // asset totals
        public int imageAssetsTotal;
        public int modelAssetsTotal;
        public int soundAssetsTotal;
    };

    public static abstract class idCommon {

//	public abstract						~idCommon( ) {}
        // Initialize everything.
        // if the OS allows, pass argc/argv directly (without executable name)
        // otherwise pass the command line in a single string (without executable name)
        public abstract void Init(int argc, final String[] argv, final String cmdline);

        // Shuts down everything.
        public abstract void Shutdown();

        // Shuts down everything.
        public abstract void Quit();

        // Returns true if common initialization is complete.
        public abstract boolean IsInitialized();

        // Called repeatedly as the foreground thread for rendering and game logic.
        public abstract void Frame();

        // Called repeatedly by blocking function calls with GUI interactivity.
        public abstract void GUIFrame(boolean execCmd, boolean network) throws idException;

        // Called 60 times a second from a background thread for sound mixing,
        // and input generation. Not called until idCommon::Init() has completed.
        public abstract void Async();

        // Checks for and removes command line "+set var arg" constructs.
        // If match is NULL, all set commands will be executed, otherwise
        // only a set with the exact name.  Only used during startup.
        // set once to clear the cvar from +set for early init code
        public abstract void StartupVariable(final String match, boolean once);

        // Initializes a tool with the given dictionary.
        public abstract void InitTool(final int toolFlag_t, final idDict dict);

        // Activates or deactivates a tool.
        public abstract void ActivateTool(boolean active);

        // Writes the user's configuration to a file
        public abstract void WriteConfigToFile(final String filename);

        // Writes cvars with the given flags to a file.
        public abstract void WriteFlaggedCVarsToFile(final String filename, int flags, final String setCmd) throws idException;

        // Begins redirection of console output to the given buffer.
        public abstract void BeginRedirect(StringBuilder buffer, int buffersize, void_callback<String> flush);

        // Stops redirection of console output.
        public abstract void EndRedirect();

        // Update the screen with every message printed.
        public abstract void SetRefreshOnPrint(boolean set);

        // Prints message to the console, which may cause a screen update if com_refreshOnPrint is set.
        public abstract void Printf(final String fmt, Object... args)/*id_attribute((format(printf,2,3)))*/;

        // Same as Printf, with a more usable API - Printf pipes to this.
        public abstract void VPrintf(final String fmt, Object... args);

        // Prints message that only shows up if the "developer" cvar is set,
        // and NEVER forces a screen update, which could cause reentrancy problems.
        public abstract void DPrintf(final String fmt, Object... args)/* id_attribute((format(printf,2,3)))*/;

        // Prints WARNING %s message and adds the warning message to a queue for printing later on.
        public abstract void Warning(final String fmt, Object... args)/* id_attribute((format(printf,2,3)))*/;

        // Prints WARNING %s message in yellow that only shows up if the "developer" cvar is set.
        public abstract void DWarning(final String fmt, Object... args)/* id_attribute((format(printf,2,3)))*/ throws idException;

        // Prints all queued warnings.
        public abstract void PrintWarnings() throws idException;

        // Removes all queued warnings.
        public abstract void ClearWarnings(final String reason);

        // Issues a C++ throw. Normal errors just abort to the game loop,
        // which is appropriate for media or dynamic logic errors.
        public abstract void Error(final String fmt, Object... args)/* id_attribute((format(printf,2,3)))*/ throws idException;

        // Fatal errors quit all the way to a system dialog box, which is appropriate for
        // static internal errors or cases where the system may be corrupted.
        public abstract void FatalError(final String fmt, Object... args)/* id_attribute((format(printf,2,3)))*/ throws idException;

        // Returns a pointer to the dictionary with language specific strings.
        public abstract idLangDict GetLanguageDict();

        // Returns key bound to the command
        public abstract String KeysFromBinding(final String bind);

        // Returns the binding bound to the key
        public abstract String BindingFromKey(final String key);

        // Directly sample a button.
        public abstract int ButtonState(int key);

        // Directly sample a keystate.
        public abstract int KeyState(int key);
    };
    static final int MAX_PRINT_MSG_SIZE = 4096;
    static final int MAX_WARNING_LIST = 256;

    enum errorParm_t {

        ERP_NONE,
        ERP_FATAL, // exit the entire game with a popup window
        ERP_DROP, // print to console and disconnect from game
        ERP_DISCONNECT					// don't kill server
    };
    static final String BUILD_DEBUG = _DEBUG ? "-debug" : "";

    static class version_s {

//        char[] string = new char[256];
        final String string;

        version_s() {
            string = String.format("%s.%d%s %s %s", ENGINE_VERSION, BUILD_NUMBER, BUILD_DEBUG, BUILD_STRING, __DATE__/*, __TIME__*/);
        }
    };
    static final version_s version = new version_s();
//
//    
//    
    public static final idCVar com_version = new idCVar("si_version", version.string, CVAR_SYSTEM | CVAR_ROM | CVAR_SERVERINFO, "engine version");
    public static final idCVar com_skipRenderer = new idCVar("com_skipRenderer", "0", CVAR_BOOL | CVAR_SYSTEM, "skip the renderer completely");
    public static final idCVar com_machineSpec = new idCVar("com_machineSpec", "-1", CVAR_INTEGER | CVAR_ARCHIVE | CVAR_SYSTEM, "hardware classification, -1 = not detected, 0 = low quality, 1 = medium quality, 2 = high quality, 3 = ultra quality");
    public static final idCVar com_purgeAll = new idCVar("com_purgeAll", "0", CVAR_BOOL | CVAR_ARCHIVE | CVAR_SYSTEM, "purge everything between level loads");
    public static final idCVar com_memoryMarker = new idCVar("com_memoryMarker", "-1", CVAR_INTEGER | CVAR_SYSTEM | CVAR_INIT, "used as a marker for memory stats");
    public static final idCVar com_preciseTic = new idCVar("com_preciseTic", "1", CVAR_BOOL | CVAR_SYSTEM, "run one game tick every async thread update");
    public static final idCVar com_asyncInput = new idCVar("com_asyncInput", "0", CVAR_BOOL | CVAR_SYSTEM, "sample input from the async thread");
    public static final String ASYNCSOUND_INFO = "0: mix sound inline, 1: memory mapped async mix, 2: callback mixing, 3: write async mix";
//
    public static final idCVar com_asyncSound = (MACOS_X ? new idCVar("com_asyncSound", "2", CVAR_INTEGER | CVAR_SYSTEM | CVAR_ROM, ASYNCSOUND_INFO)
            : (__linux__ ? new idCVar("com_asyncSound", "3", CVAR_INTEGER | CVAR_SYSTEM | CVAR_ROM, ASYNCSOUND_INFO)
                    : new idCVar("com_asyncSound", "1", CVAR_INTEGER | CVAR_SYSTEM, ASYNCSOUND_INFO, 0, 1)));
//
    public static final idCVar com_forceGenericSIMD = new idCVar("com_forceGenericSIMD", "0", CVAR_BOOL | CVAR_SYSTEM | CVAR_NOCHEAT, "force generic platform independent SIMD");
    public static final idCVar com_developer = new idCVar("developer", "1", CVAR_BOOL | CVAR_SYSTEM | CVAR_NOCHEAT, "developer mode");
    public static final idCVar com_allowConsole = new idCVar("com_allowConsole", "0", CVAR_BOOL | CVAR_SYSTEM | CVAR_NOCHEAT, "allow toggling console with the tilde key");
    public static final idCVar com_speeds = new idCVar("com_speeds", "0", CVAR_BOOL | CVAR_SYSTEM | CVAR_NOCHEAT, "show engine timings");
    public static final idCVar com_showFPS = new idCVar("com_showFPS", "1", CVAR_BOOL | CVAR_SYSTEM | CVAR_ARCHIVE | CVAR_NOCHEAT, "show frames rendered per second");
    public static final idCVar com_showMemoryUsage = new idCVar("com_showMemoryUsage", "0", CVAR_BOOL | CVAR_SYSTEM | CVAR_NOCHEAT, "show total and per frame memory usage");
    public static final idCVar com_showAsyncStats = new idCVar("com_showAsyncStats", "0", CVAR_BOOL | CVAR_SYSTEM | CVAR_NOCHEAT, "show async network stats");
    public static final idCVar com_showSoundDecoders = new idCVar("com_showSoundDecoders", "0", CVAR_BOOL | CVAR_SYSTEM | CVAR_NOCHEAT, "show sound decoders");
    public static final idCVar com_timestampPrints = new idCVar("com_timestampPrints", "0", CVAR_SYSTEM, "print time with each console print, 1 = msec, 2 = sec", 0, 2, new ArgCompletion_Integer(0, 2));
    public static final idCVar com_timescale = new idCVar("timescale", "1", CVAR_SYSTEM | CVAR_FLOAT, "scales the time", 0.1f, 10.0f);
    public static final idCVar com_logFile = new idCVar("logFile", "0", CVAR_SYSTEM | CVAR_NOCHEAT, "1 = buffer log, 2 = flush after each print", 0, 2, new ArgCompletion_Integer(0, 2));
    public static final idCVar com_logFileName = new idCVar("logFileName", "qconsole.log", CVAR_SYSTEM | CVAR_NOCHEAT, "name of log file, if empty, qconsole.log will be used");
    public static final idCVar com_makingBuild = new idCVar("com_makingBuild", "0", CVAR_BOOL | CVAR_SYSTEM, "1 when making a build");
    public static final idCVar com_updateLoadSize = new idCVar("com_updateLoadSize", "0", CVAR_BOOL | CVAR_SYSTEM | CVAR_NOCHEAT, "update the load size after loading a map");
    public static final idCVar com_videoRam = new idCVar("com_videoRam", "64", CVAR_INTEGER | CVAR_SYSTEM | CVAR_NOCHEAT | CVAR_ARCHIVE, "holds the last amount of detected video ram");
//
    public static final idCVar com_product_lang_ext = new idCVar("com_product_lang_ext", "1", CVAR_INTEGER | CVAR_SYSTEM | CVAR_ARCHIVE, "Extension to use when creating language files.");
//
//
    // com_speeds times
    public static          int     time_gameFrame;
    public static          int     time_gameDraw;
    public static          int     time_frontend;        // renderSystem frontend time
    public static          int     time_backend;         // renderSystem backend time
    public static volatile int     com_frameTime;        // time for the current frame in milliseconds
    public static          int     com_frameNumber;      // variable frame number
    public static volatile int     com_ticNumber;        // 60 hz tics
    public static          int     com_editors;          // currently opened editor(s)
    public static          boolean com_editorActive;     // true if an editor has focus
//    
//#ifdef _WIN32
    public static final String       DMAP_MSGID    = "DMAPOutput";
    public static final String       DMAP_DONE     = "DMAPDone";
    public static       long/*HWND*/ com_hwndMsg   = 0;
    public static       boolean      com_outputMsg = false;
           static       long         com_msgID     = -1;
//#endif

    public static class idCommonLocal extends idCommon {

        private boolean com_fullyInitialized;
        private boolean com_refreshOnPrint;		// update the screen every print for dmap
        private int com_errorEntered;                   // 0, ERP_DROP, etc
        private boolean com_shuttingDown;
//
        private idFile logFile;
//
        private String[] errorMessage = {null};//new char[MAX_PRINT_MSG_SIZE];
//
        private StringBuilder rd_buffer;
        private int rd_buffersize;
        private void_callback<String> rd_flush/*)( const char *buffer )*/;
        private idStr warningCaption;
        private idStrList warningList;
        private idStrList errorList;
//
        private int gameDLL;
//
        private idLangDict languageDict;
//#ifdef ID_WRITE_VERSION
        idCompressor config_compressor;
//#endif
//        private static final Lock SINGLE_ASYNC_TIC_LOCK = new ReentrantLock();//TODO:collect the locks into a single bundle.
        //
        //

        class asyncStats_t {

            int milliseconds;                           // should always be incremeting by 60hz
            int deltaMsec;				// should always be 16
            int timeConsumed;                           // msec spent in Com_AsyncThread()
            int clientPacketsReceived;
            int serverPacketsReceived;
            int mostRecentServerPacketSequence;
        };
        private static final int MAX_ASYNC_STATS = 1024;
        private final asyncStats_t[] com_asyncStats;	// indexed by com_ticNumber
        private int prevAsyncMsec;
        private int lastTicMsec;
        //
        final static int MAX_CONSOLE_LINES = 32;
        int com_numConsoleLines;
        idCmdArgs[] com_consoleLines;
        //

        public idCommonLocal() {
            com_fullyInitialized = false;
            com_refreshOnPrint = false;
            com_errorEntered = 0;
            com_shuttingDown = false;

            logFile = null;

//	strcpy( errorMessage, "" );
            rd_buffer = null;
            rd_buffersize = 0;
            rd_flush = null;
            this.warningList = new idStrList();
            this.errorList = new idStrList();
            this.languageDict = new idLangDict();

            gameDLL = 0;
            this.com_asyncStats = new asyncStats_t[MAX_ASYNC_STATS];
            for (int c = 0; c < com_asyncStats.length; c++) {
                com_asyncStats[c] = new asyncStats_t();
            }
            this.com_consoleLines = new idCmdArgs[MAX_CONSOLE_LINES];
            for (int c = 0; c < com_consoleLines.length; c++) {
                com_consoleLines[c] = new idCmdArgs();
            }

            if (ID_WRITE_VERSION) {
                config_compressor = null;
            }

        }

        @Override
        public void Init(int argc, String[] argv, String cmdline) {
            try {

                // set interface pointers used by idLib
                idLib.sys = sys_public.sys;
                idLib.common = common;
                idLib.cvarSystem = cvarSystem;
                idLib.fileSystem = fileSystem;

                // initialize idLib
                idLib.Init();

                // clear warning buffer
                ClearWarnings(GAME_NAME + " initialization");

                // parse command line options
                idCmdArgs args;
                if (isNotNullOrEmpty(cmdline)) {
                    // tokenize if the OS doesn't do it for us
                    args = new idCmdArgs();
                    args.TokenizeString(cmdline, true);
                    int[] cArg = {argc};
                    argv = args.GetArgs(cArg);
                    argc = cArg[0];
                }
                ParseCommandLine(argc, argv);

                // init console command system
                cmdSystem.Init();

                // init CVar system
                cvarSystem.Init();

                // start file logging right away, before early console or whatever
                StartupVariable("win_outputDebugString", false);

                // register all static CVars
                idCVar.RegisterStaticVars();

                // print engine version
                Printf("%s\n", version.string);

                // initialize key input/binding, done early so bind command exists
                idKeyInput.Init();

                // init the console so we can take printsF
                console.Init();

                // get architecture info
                Sys_Init();

                // initialize networking
                Sys_InitNetworking();

                // override cvars from command line
                StartupVariable(null, false);

                if (NOT(idAsyncNetwork.serverDedicated.GetInteger()) && Sys_AlreadyRunning()) {
                    Sys_Quit();
                }

                // initialize processor specific SIMD implementation
                InitSIMD();

                // init commands
                InitCommands();

                if (ID_WRITE_VERSION) {
                    config_compressor = idCompressor.AllocArithmetic();
                }

                // game specific initialization
                InitGame();

                // don't add startup commands if no CD key is present
                if ((ID_ENFORCE_KEY && (!session.CDKeysAreValid(false) || !AddStartupCommands()))
                        || !AddStartupCommands()) {

                    // if the user didn't give any commands, run default action
                    session.StartMenu(true);
                }

                Printf("--- Common Initialization Complete ---\n");

                // print all warnings queued during initialization
                PrintWarnings();

                if (ID_DEDICATED) {
                    Printf("\nType 'help' for dedicated server info.\n\n");
                }

                // remove any prints from the notify lines
                console.ClearNotifyLines();

                ClearCommandLine();

                com_fullyInitialized = true;
            } catch (idException e) {
                Sys_Error("Error during initialization");
            }
        }

        @Override
        public void Shutdown() {

            com_shuttingDown = true;

            idAsyncNetwork.server.Kill();
            idAsyncNetwork.client.Shutdown();

            // game specific shut down
            ShutdownGame(false);

//            // shut down non-portable system services
//            Sys_Shutdown();
//
            // shut down the console
            console.Shutdown();

            // shut down the key system
            idKeyInput.Shutdown();

            // shut down the cvar system
            cvarSystem.Shutdown();

            // shut down the console command system
            cmdSystem.Shutdown();

            if (ID_WRITE_VERSION) {
                //	delete config_compressor;
                config_compressor = null;
            }

            // free any buffered warning messages
            ClearWarnings(GAME_NAME + " shutdown");
            warningCaption.Clear();
            errorList.Clear();

            // free language dictionary
            languageDict.Clear();

            // enable leak test
//            Mem_EnableLeakTest("doom");
            // shutdown idLib
            idLib.ShutDown();
        }

        @Override
        public void Quit() {

            if (ID_ALLOW_TOOLS) {
                if ((com_editors & EDITOR_RADIANT) != 0) {
                    RadiantInit();
                    return;
                }
            }

            // don't try to shutdown if we are in a recursive error
            if (0 == com_errorEntered) {
                Shutdown();
            }

            Sys_Quit();
        }

        @Override
        public boolean IsInitialized() {
            return com_fullyInitialized;
        }
        private static int lastTime;

        @Override
        public void Frame() {
            try {
//
//                // pump all the events
//                Sys_GenerateEvents();
//
                // write config file if anything changed
                WriteConfiguration();

                // change SIMD implementation if required
                if (com_forceGenericSIMD.IsModified()) {
                    InitSIMD();
                }

                eventLoop.RunEventLoop();

                com_frameTime = com_ticNumber * USERCMD_MSEC;
//                System.out.println(System.nanoTime()+"com_frameTime=>"+com_frameTime);

                idAsyncNetwork.RunFrame();

                if (idAsyncNetwork.IsActive()) {
                    if (idAsyncNetwork.serverDedicated.GetInteger() != 1) {
                        session.GuiFrameEvents();
                        session.UpdateScreen(false);
                    }
                } else {
                    session.Frame();
                    GuiModel.idGuiModel.bla = true;

                    // normal, in-sequence screen update
                    session.UpdateScreen(false);
//                    int a = GuiModel.idGuiModel.bla1;
//                    a = GuiModel.idGuiModel.bla2;
//                    a = GuiModel.idGuiModel.bla3;
//                    a = GuiModel.idGuiModel.bla4;
                }

                // report timing information
                if (com_speeds.GetBool()) {
//			 int	lastTime;
                    int nowTime = Sys_Milliseconds();
                    int com_frameMsec = nowTime - lastTime;
                    lastTime = nowTime;
                    Printf("frame:%d all:%3d gfr:%3d rf:%3d bk:%3d\n", com_frameNumber, com_frameMsec, time_gameFrame, time_frontend, time_backend);
                    time_gameFrame = 0;
                    time_gameDraw = 0;
                }

                com_frameNumber++;

                // set idLib frame number for frame based memory dumps
                idLib.frameNumber = com_frameNumber;
//
//                // the FPU stack better be empty at this point or some bad code or compiler bug left values on the stack
//                if (!Sys_FPU_StackIsEmpty()) {
//                    Printf(Sys_FPU_GetState());
//                    FatalError("idCommon::Frame: the FPU stack is not empty at the end of the frame\n");
//                }
            } catch (idException ex) {
                return;			// an ERP_DROP was thrown
            }
        }

        @Override
        public void GUIFrame(boolean execCmd, boolean network) throws idException {
            Sys_GenerateEvents();
            eventLoop.RunEventLoop(execCmd);	// and execute any commands
            com_frameTime = com_ticNumber * USERCMD_MSEC;
            if (network) {
                idAsyncNetwork.RunFrame();
            }
            session.Frame();
            session.UpdateScreen(false);
        }

        /*
         =================
         idCommonLocal::SingleAsyncTic

         The system will asyncronously call this function 60 times a second to
         handle the time-critical functions that we don't want limited to
         the frame rate:

         sound mixing
         user input generation (conditioned by com_asyncInput)
         packet server operation
         packet client operation

         We are not using thread safe libraries, Fso any functionality put here must
         be VERY VERY careful about what it calls.
         =================
         */
        @Override
        public void Async() {
//            System.out.println(">>>>>>"+System.nanoTime());
            if (com_shuttingDown) {
                return;
            }

            int msec = Sys_Milliseconds();
            if (0 == lastTicMsec) {
                lastTicMsec = msec - USERCMD_MSEC;
            }

            if (!com_preciseTic.GetBool()) {
                // just run a single tic, even if the exact msec isn't precise
                SingleAsyncTic();
                return;
            }

            int ticMsec = USERCMD_MSEC;

            // the number of msec per tic can be varies with the timescale cvar
            float timescale = com_timescale.GetFloat();
            if (timescale != 1.0f) {
                ticMsec /= timescale;
                if (ticMsec < 1) {
                    ticMsec = 1;
                }
            }

            // don't skip too many
            if (timescale == 1.0f) {
                if (lastTicMsec + 10 * USERCMD_MSEC < msec) {
                    lastTicMsec = msec - 10 * USERCMD_MSEC;
                }
            }

            while (lastTicMsec + ticMsec <= msec) {
                SingleAsyncTic();
                lastTicMsec += ticMsec;
            }
//            System.out.println("<<<<<<<"+System.nanoTime());
        }
        /*
         ============================================================================

         COMMAND LINE FUNCTIONS

         + characters separate the commandLine string into multiple console
         command lines.

         All of these are valid:

         doom +set test blah +map test
         doom set test blah+map test
         doom set test blah + map test

         ============================================================================
         */


        /*
         ==================
         idCommonLocal::StartupVariable

         Searches for command line parameters that are set commands.
         If match is not NULL, only that cvar will be looked for.
         That is necessary because cddir and basedir need to be set
         before the filesystem is started, but all other sets should
         be after execing the config and default.
         ==================
         */
        @Override
        public void StartupVariable(String match, boolean once) {
            int i;
            String s;

            i = 0;
            while (i < com_numConsoleLines) {
//                if ( strcmp( com_consoleLines[ i ].Argv( 0 ), "set" ) ) {//TODO:strcmp equals returns false.
                if (!"set".equals(com_consoleLines[i].Argv(0))) {
                    i++;
                    continue;
                }

                s = com_consoleLines[i].Argv(1);

                if (null == match || 0 == idStr.Icmp(s, match)) {
                    cvarSystem.SetCVarString(s, com_consoleLines[i].Argv(2));
                    if (once) {
                        // kill the line
                        int j = i + 1;
                        while (j < com_numConsoleLines) {
                            com_consoleLines[j - 1] = com_consoleLines[j];
                            j++;
                        }
                        com_numConsoleLines--;
                        continue;
                    }
                }
                i++;
            }
        }

        @Override
        public void InitTool(int toolFlag_t, idDict dict) {
            if (ID_ALLOW_TOOLS) {
                if ((toolFlag_t & EDITOR_SOUND) != 0) {
                    SoundEditorInit(dict);
                } else if ((toolFlag_t & EDITOR_LIGHT) != 0) {
                    LightEditorInit(dict);
                } else if ((toolFlag_t & EDITOR_PARTICLE) != 0) {
                    ParticleEditorInit(dict);
                } else if ((toolFlag_t & EDITOR_AF) != 0) {
                    AFEditorInit(dict);
                }
            }
        }

        /*
         ==================
         idCommonLocal::ActivateTool

         Activates or Deactivates a tool
         ==================
         */ @Override
        public void ActivateTool(boolean active) {
            com_editorActive = active;
            Sys_GrabMouseCursor(!active);
        }

        @Override
        public void WriteConfigToFile(String filename) {
            idFile f;

            f = fileSystem.OpenFileWrite(filename);
            if (null == f) {
                Printf("Couldn't write %s.\n", filename);
                return;
            }

            if (ID_WRITE_VERSION) {
//                long ID_TIME_T;
                String curTime;
                String runtag;
                idFile_Memory compressed = new idFile_Memory("compressed");
                idBase64 out = new idBase64();
                assert (config_compressor != null);
//                ID_TIME_T = time(null);
                curTime = new Date().toString();
                runtag = String.format("%s - %s", cvarSystem.GetCVarString("si_version"), curTime);
                config_compressor.Init(compressed, true, 8);
                config_compressor.WriteString(runtag);//
                config_compressor.FinishCompress();
                out.Encode(/*(const byte *)*/compressed.GetDataPtr(), compressed.Length());
                f.Printf("// %s\n", out.c_str());
            }

            idKeyInput.WriteBindings(f);
            cvarSystem.WriteFlaggedVariables(CVAR_ARCHIVE, "seta", f);
            fileSystem.CloseFile(f);
        }

        @Override
        public void WriteFlaggedCVarsToFile(String filename, int flags, String setCmd) throws idException {
            idFile f;

            f = fileSystem.OpenFileWrite(filename);
            if (null == f) {
                Printf("Couldn't write %s.\n", filename);
                return;
            }
            cvarSystem.WriteFlaggedVariables(flags, setCmd, f);
            fileSystem.CloseFile(f);
        }

        @Override
        public void BeginRedirect(StringBuilder buffer, int buffersize, void_callback<String> flush) {
            if (null == buffer || 0 == buffersize || null == flush) {
                return;
            }
            rd_buffer = buffer;
            rd_buffersize = buffersize;
            rd_flush = flush;

//	*rd_buffer = 0;
        }

        @Override
        public void EndRedirect() {
            if (rd_flush != null && rd_buffer.length() != 0) {// '\0') {
                rd_flush.run(rd_buffer.toString());
            }

            rd_buffer = null;
            rd_buffersize = 0;
            rd_flush = null;
        }

        @Override
        public void SetRefreshOnPrint(boolean set) {
            com_refreshOnPrint = set;
        }

        /*
         ==================
         idCommonLocal::Printf

         Both client and server can use this, and it will output to the appropriate place.

         A raw string should NEVER be passed as fmt, because of "%f" type crashers.
         ==================
         */
        @Override
        public void Printf(String fmt, Object... args) {
//	va_list argptr;
//	va_start( argptr, fmt );
            VPrintf(fmt, args);
//	va_end( argptr );
        }
        static boolean logFileFailed = false, recursing;

        /*
         ==================
         idCommonLocal::VPrintf

         A raw string should NEVER be passed as fmt, because of "%f" type crashes.
         ==================
         */
        @Override
        public void VPrintf(String fmt, Object... args) {
            String[] msg = {null};//new char(MAX_PRINT_MSG_SIZE);
            int timeLength;

            // if the cvar system is not initialized
            if (!cvarSystem.IsInitialized()) {
                return;
            }

            // optionally put a timestamp at the beginning of each print,
            // so we can see how long different init sections are taking
            if (com_timestampPrints.GetInteger() != 0) {
                int t = Sys_Milliseconds();
                if (com_timestampPrints.GetInteger() == 1) {
                    t /= 1000;
                }
//                sprintf(msg, "[%i]", t);
                msg[0] = String.format("[%d]", t);
                timeLength = msg[0].length();
            } else {
                timeLength = 0;
            }

            // don't overflow
            if (idStr.vsnPrintf(msg, MAX_PRINT_MSG_SIZE - timeLength - 1, fmt, args) < 0) {
                msg[0] = "\n";
//                msg[0][msg[0].length - 2] = '\n';
//                msg[0][msg[0].length - 1] = '\0'; // avoid output garbling
                Sys_Printf("idCommon::VPrintf: truncated to %d characters\n", msg[0].length() /*- 1*/);
            }

            if (rd_buffer != null) {
                if ((msg[0].length() + rd_buffer.length()) > (rd_buffersize - 1)) {
                    rd_flush.run(rd_buffer.toString());
//			*rd_buffer = 0;
                }
//		strcat( rd_buffer, msg );
                rd_buffer.append(msg[0]);
                return;
            }

            // echo to console buffer
            console.Print(msg[0]);

            // remove any color codes
            idStr.RemoveColors(msg[0]);

            // echo to dedicated console and early console
            Sys_Printf("%s", msg[0]);

            // print to script debugger server
            // DebuggerServerPrint( msg );
//#if 0	// !@#
//#if defined(_DEBUG) && defined(WIN32)
//	if ( strlen( msg ) < 512 ) {
//		TRACE( msg );
//	}
//#endif
//#endif
            // logFile
            if (com_logFile.GetInteger() != 0 && !logFileFailed && fileSystem.IsInitialized()) {
//		static bool recursing;

                if (null == logFile && !recursing) {
                    final String newTime = new Date().toString();
                    final String fileName = (!com_logFileName.GetString().isEmpty() ? com_logFileName.GetString() : "qconsole.log");

                    // fileSystem.OpenFileWrite can cause recursive prints into here
                    recursing = true;

                    logFile = fileSystem.OpenFileWrite(fileName);
                    if (null == logFile) {
                        logFileFailed = true;
                        FatalError("failed to open log file '%s'\n", fileName);
                    }

                    recursing = false;

                    if (com_logFile.GetInteger() > 1) {
                        // force it to not buffer so we get valid
                        // data even if we are crashing
                        logFile.ForceFlush();
                    }

                    Printf("log file '%s' opened on %s\n", fileName, newTime);
                }
                if (logFile != null) {
                    logFile.WriteString(msg[0]);
                    logFile.Flush();	// ForceFlush doesn't help a whole lot
                }
            }

            // don't trigger any updates if we are in the process of doing a fatal error
            if (com_errorEntered != etoi(ERP_FATAL)) {
                // update the console if we are in a long-running command, like dmap
                if (com_refreshOnPrint) {
                    session.UpdateScreen();
                }

                // let session redraw the animated loading screen if necessary
                session.PacifierUpdate();
            }

//            if (_WIN32) {
//
//                if (com_outputMsg ) {
//                    if (com_msgID == -1) {
//                        com_msgID = ::RegisterWindowMessage(DMAP_MSGID);
//                        if (!FindEditor()) {
//                            com_outputMsg = false;
//                        } else {
//                            Sys_ShowWindow(false);
//                        }
//                    }
//                    if (com_hwndMsg) {
//                        ATOM atom = ::GlobalAddAtom(msg);
//                        ::PostMessage(com_hwndMsg, com_msgID, 0, static_cast < LPARAM > (atom));
//                    }
//                }
//
//            }
        }

        /*
         ==================
         idCommonLocal::DPrintf

         prints message that only shows up if the "developer" cvar is set
         ==================
         */
        @Override
        public void DPrintf(final String fmt, Object... args) {
//	va_list		argptr;
            String[] msg = {null};//new char[MAX_PRINT_MSG_SIZE];

            if (!cvarSystem.IsInitialized() || !com_developer.GetBool()) {
                return;			// don't confuse non-developers with techie stuff...
            }

//	va_start( argptr, fmt );
            idStr.vsnPrintf(msg, MAX_PRINT_MSG_SIZE, fmt, args);
//	va_end( argptr );
//            msg[MAX_PRINT_MSG_SIZE - 1] = '\0';
//
            // never refresh the screen, which could cause reentrency problems
            boolean temp = com_refreshOnPrint;
            com_refreshOnPrint = false;

            Printf(S_COLOR_RED + "%s", msg[0]);

            com_refreshOnPrint = temp;
        }

        /*
         ==================
         idCommonLocal::Warning

         prints WARNING %s and adds the warning message to a queue to be printed later on
         ==================
         */
        @Override
        public void Warning(final String fmt, Object... args) {
//	va_list		argptr;
            String[] msg = {null};//[MAX_PRINT_MSG_SIZE];

//	va_start( argptr, fmt );
            idStr.vsnPrintf(msg, MAX_PRINT_MSG_SIZE, fmt, args);
//	va_end( argptr );
//            msg[MAX_PRINT_MSG_SIZE - 1] = 0;

            Printf(S_COLOR_YELLOW + "WARNING: " + S_COLOR_RED + "%s\n", msg[0]);

            if (warningList.Num() < MAX_WARNING_LIST) {
                warningList.AddUnique(msg[0]);
            }
        }

        /*
         ==================
         idCommonLocal::DWarning

         prints warning message in yellow that only shows up if the "developer" cvar is set
         ==================
         */
        @Override
        public void DWarning(final String fmt, Object... args) throws idException {
//	va_list		argptr;
            String[] msg = {null};//new char[MAX_PRINT_MSG_SIZE];

            if (!com_developer.GetBool()) {
                return;			// don't confuse non-developers with techie stuff...
            }

//	va_start( argptr, fmt );
            idStr.vsnPrintf(msg, MAX_PRINT_MSG_SIZE, fmt, args);
//	va_end( argptr );
//            msg[MAX_PRINT_MSG_SIZE - 1] = '\0';

            Printf(S_COLOR_YELLOW + "WARNING: %s\n", msg[0]);
        }

        @Override
        public void PrintWarnings() throws idException {
            int i;

            if (0 == warningList.Num()) {
                return;
            }

            warningList.Sort();

            Printf("------------- Warnings ---------------\n");
            Printf("during %s...\n", warningCaption);

            for (i = 0; i < warningList.Num(); i++) {
                Printf(S_COLOR_YELLOW + "WARNING: " + S_COLOR_RED + "%s\n", warningList.oGet(i));
            }
            if (warningList.Num() != 0) {
                if (warningList.Num() >= MAX_WARNING_LIST) {
                    Printf("more than %d warnings\n", MAX_WARNING_LIST);
                } else {
                    Printf("%d warnings\n", warningList.Num());
                }
            }
        }

        @Override
        public void ClearWarnings(String reason) {
            warningCaption = new idStr(reason);
            warningList.Clear();
        }
        static int lastErrorTime;
        static int errorCount;

        @Override
        public void Error(final String fmt, Object... args) throws idException {
//	va_list		argptr;
            int currentTime;

            int code = etoi(ERP_DROP);

            // always turn this off after an error
            com_refreshOnPrint = false;

            // when we are running automated scripts, make sure we
            // know if anything failed
            if (cvarSystem.GetCVarInteger("fs_copyfiles") != 0) {
                code = etoi(ERP_FATAL);
            }

            // if we don't have GL running, make it a fatal error
            if (!renderSystem.IsOpenGLRunning()) {
                code = etoi(ERP_FATAL);
            }

            // if we got a recursive error, make it fatal
            if (com_errorEntered != 0) {
                // if we are recursively erroring while exiting
                // from a fatal error, just kill the entire
                // process immediately, which will prevent a
                // full screen rendering window covering the
                // error dialog
                if (com_errorEntered == etoi(ERP_FATAL)) {
                    Sys_Quit();
                }
                code = etoi(ERP_FATAL);
            }

            // if we are getting a solid stream of ERP_DROP, do an ERP_FATAL
            currentTime = Sys_Milliseconds();
            if (currentTime - lastErrorTime < 100) {
                if (++errorCount > 3) {
                    code = etoi(ERP_FATAL);
                }
            } else {
                errorCount = 0;
            }
            lastErrorTime = currentTime;

            com_errorEntered = code;

//	va_start (argptr,fmt);
            idStr.vsnPrintf(errorMessage, MAX_PRINT_MSG_SIZE, fmt, args);
//	va_end (argptr);
//            errorMessage[errorMessage[.length - 1] = '\0';//TODO:is this needed?

            // copy the error message to the clip board
            Sys_SetClipboardData(errorMessage[0]);

            // add the message to the error list
            errorList.AddUnique(new idStr(errorMessage[0]));

            // Dont shut down the session for gui editor or debugger
            if (0 == (com_editors & (EDITOR_GUI | EDITOR_DEBUGGER))) {
                session.Stop();
            }

            if (code == etoi(ERP_DISCONNECT)) {
                com_errorEntered = 0;
                throw new idException(errorMessage[0]);
                // The gui editor doesnt want thing to com_error so it handles exceptions instead
            } else if ((com_editors & (EDITOR_GUI | EDITOR_DEBUGGER)) != 0) {
                com_errorEntered = 0;
                throw new idException(errorMessage[0]);
            } else if (code == etoi(ERP_DROP)) {
                Printf("********************\nERROR: %s\n********************\n", errorMessage[0]);
                com_errorEntered = 0;
                throw new idException(errorMessage[0]);
            } else {
                Printf("********************\nERROR: %s\n********************\n", errorMessage[0]);
            }

            if (cvarSystem.GetCVarBool("r_fullscreen")) {
                cmdSystem.BufferCommandText(CMD_EXEC_NOW, "vid_restart partial windowed\n");
            }

            Shutdown();

            Sys_Error("%s", errorMessage[0]);
        }

        /*
         ==================
         idCommonLocal::FatalError

         Dump out of the game to a system dialog
         ==================
         */
        @Override
        public void FatalError(final String fmt, Object... args) throws idException {
//	va_list		argptr;

            // if we got a recursive error, make it fatal
            if (com_errorEntered != 0) {
                // if we are recursively erroring while exiting
                // from a fatal error, just kill the entire
                // process immediately, which will prevent a
                // full screen rendering window covering the
                // error dialog

                Sys_Printf("FATAL: recursed fatal error:\n%s\n", errorMessage[0]);

//		va_start( argptr, fmt );
                idStr.vsnPrintf(errorMessage, MAX_PRINT_MSG_SIZE, fmt, args);
//		va_end( argptr );
//                errorMessage[errorMessage.length - 1] = '\0';//TODO:useless

                Sys_Printf("%s\n", errorMessage[0]);

                // write the console to a log file?
                Sys_Quit();
            }
            com_errorEntered = etoi(ERP_FATAL);

//	va_start( argptr, fmt );
            idStr.vsnPrintf(errorMessage, MAX_PRINT_MSG_SIZE, fmt, args);
//	va_end( argptr );
//            errorMessage[errorMessage.length - 1] = '\0';

            if (cvarSystem.GetCVarBool("r_fullscreen")) {
                cmdSystem.BufferCommandText(CMD_EXEC_NOW, "vid_restart partial windowed\n");
            }

            Sys_SetFatalError(errorMessage[0]);

            Shutdown();

            Sys_Error("%s", errorMessage[0]);
        }

        @Override
        public idLangDict GetLanguageDict() {
            return languageDict;
        }
//
//        
//        

        /*
         ===============
         KeysFromBinding()
         Returns the key bound to the command
         ===============
         */
        @Override
        public String KeysFromBinding(String bind) {
            return idKeyInput.KeysFromBinding(bind);
        }

        /*
         ===============
         BindingFromKey()
         Returns the binding bound to key
         ===============
         */
        @Override
        public String BindingFromKey(String key) {
            return idKeyInput.BindingFromKey(key);
        }
//
//        
//        

        /*
         ===============
         ButtonState()
         Returns the state of the button
         ===============
         */ @Override
        public int ButtonState(int key) {
            return usercmdGen.ButtonState(key);
        }

        /*
         ===============
         ButtonState()
         Returns the state of the key
         ===============
         */
        @Override
        public int KeyState(int key) {
            return usercmdGen.KeyState(key);
        }

        public void InitGame() throws idException {
            // initialize the file system
            fileSystem.Init();

            // initialize the declaration manager
            declManager.Init();

            // force r_fullscreen 0 if running a tool
            CheckToolMode();

            idFile file = fileSystem.OpenExplicitFileRead(fileSystem.RelativePathToOSPath(CONFIG_SPEC, "fs_savepath"));
            boolean sysDetect = (null == file);
            if (!sysDetect) {
                fileSystem.CloseFile(file);
            } else {
                file = fileSystem.OpenFileWrite(CONFIG_SPEC);
                fileSystem.CloseFile(file);
            }

            idCmdArgs args = new idCmdArgs();
            if (sysDetect) {
                SetMachineSpec();
                Com_ExecMachineSpec_f.getInstance().run(args);
            }

            // initialize the renderSystem data structures, but don't start OpenGL yet
            renderSystem.Init();

            // initialize string database right off so we can use it for loading messages
            InitLanguageDict();

            PrintLoadingMessage(common.GetLanguageDict().GetString("#str_04344"));

            // load the font, etc
            console.LoadGraphics();

            // init journalling, etc
            eventLoop.Init();

            PrintLoadingMessage(common.GetLanguageDict().GetString("#str_04345"));

            // exec the startup scripts
            cmdSystem.BufferCommandText(CMD_EXEC_APPEND, "exec editor.cfg\n");
            cmdSystem.BufferCommandText(CMD_EXEC_APPEND, "exec default.cfg\n");

            // skip the config file if "safe" is on the command line
            if (!SafeMode()) {
                cmdSystem.BufferCommandText(CMD_EXEC_APPEND, "exec " + CONFIG_FILE + "\n");
            }
            cmdSystem.BufferCommandText(CMD_EXEC_APPEND, "exec autoexec.cfg\n");

            // reload the language dictionary now that we've loaded config files
            cmdSystem.BufferCommandText(CMD_EXEC_APPEND, "reloadLanguage\n");

            // run cfg execution
            cmdSystem.ExecuteCommandBuffer();

            // re-override anything from the config files with command line args
            StartupVariable(null, false);

            // if any archived cvars are modified after this, we will trigger a writing of the config file
            cvarSystem.ClearModifiedFlags(CVAR_ARCHIVE);

            // cvars are initialized, but not the rendering system. Allow preference startup dialog
            Sys_DoPreferences();

            // init the user command input code
            usercmdGen.Init();

            PrintLoadingMessage(common.GetLanguageDict().GetString("#str_04346"));

            // start the sound system, but don't do any hardware operations yet
            soundSystem.Init();

            PrintLoadingMessage(common.GetLanguageDict().GetString("#str_04347"));

            // init async network
            idAsyncNetwork.Init();

            if (ID_DEDICATED) {
                idAsyncNetwork.server.InitPort();
                cvarSystem.SetCVarBool("s_noSound", true);
            } else {
                if (idAsyncNetwork.serverDedicated.GetInteger() == 1) {
                    idAsyncNetwork.server.InitPort();
                    cvarSystem.SetCVarBool("s_noSound", true);
                } else {
                    // init OpenGL, which will open a window and connect sound and input hardware
                    PrintLoadingMessage(common.GetLanguageDict().GetString("#str_04348"));
                    InitRenderSystem();
                }
            }

            PrintLoadingMessage(common.GetLanguageDict().GetString("#str_04349"));

            // initialize the user interfaces
            uiManager.Init();

            // startup the script debugger
            // DebuggerServerInit();
            PrintLoadingMessage(common.GetLanguageDict().GetString("#str_04350"));

            // load the game dll
            LoadGameDLL();

            PrintLoadingMessage(common.GetLanguageDict().GetString("#str_04351"));

            // init the session
            session.Init();

            // have to do this twice.. first one sets the correct r_mode for the renderer init
            // this time around the backend is all setup correct.. a bit fugly but do not want
            // to mess with all the gl init at this point.. an old vid card will never qualify for 
            if (sysDetect) {
                SetMachineSpec();
                Com_ExecMachineSpec_f.getInstance().run(args);
                cvarSystem.SetCVarInteger("s_numberOfSpeakers", 6);
                cmdSystem.BufferCommandText(CMD_EXEC_NOW, "s_restart\n");
                cmdSystem.ExecuteCommandBuffer();
            }
        }

        public void ShutdownGame(boolean reloading) {

            // kill sound first
            idSoundWorld sw = soundSystem.GetPlayingSoundWorld();
            if (sw != null) {
                sw.StopAllSounds();
            }
            soundSystem.ClearBuffer();

            // shutdown the script debugger
            // DebuggerServerShutdown();
            idAsyncNetwork.client.Shutdown();

            // shut down the session
            session.Shutdown();

            // shut down the user interfaces
            uiManager.Shutdown();

            // shut down the sound system
            soundSystem.Shutdown();

            // shut down async networking
            idAsyncNetwork.Shutdown();

            // shut down the user command input code
            usercmdGen.Shutdown();

            // shut down the event loop
            eventLoop.Shutdown();

            // shut down the renderSystem
            renderSystem.Shutdown();

            // shutdown the decl manager
            declManager.Shutdown();

            // unload the game dll
            UnloadGameDLL();

            // dump warnings to "warnings.txt"
            if (DEBUG) {
                DumpWarnings();
            }
            // only shut down the log file after all output is done
            CloseLogFile();

            // shut down the file system
            fileSystem.Shutdown(reloading);
        }

        // localization
        public void InitLanguageDict() throws idException {
//            idStr fileName;
            languageDict.Clear();

            //D3XP: Instead of just loading a single lang file for each language
            //we are going to load all files that begin with the language name
            //similar to the way pak files work. So you can place english001.lang
            //to add new strings to the english language dictionary
            idFileList langFiles;
            langFiles = fileSystem.ListFilesTree("strings", ".lang", true);

            idStrList langList = langFiles.GetList();

            StartupVariable("sys_lang", false);	// let it be set on the command line - this is needed because this init happens very early
            idStr langName = new idStr(cvarSystem.GetCVarString("sys_lang"));

            //Loop through the list and filter
            idStrList currentLangList = langList;
            FilterLangList(currentLangList, langName);

            if (currentLangList.Num() == 0) {
                // reset cvar to default and try to load again
                cmdSystem.BufferCommandText(CMD_EXEC_NOW, "reset sys_lang");
                langName = new idStr(cvarSystem.GetCVarString("sys_lang"));
                currentLangList = langList;
                FilterLangList(currentLangList, langName);
            }

            for (int i = 0; i < currentLangList.Num(); i++) {
                //common.Printf("%s\n", currentLangList[i].c_str());
                languageDict.Load(currentLangList.oGet(i).toString(), false);
            }

            fileSystem.FreeFileList(langFiles);

            Sys_InitScanTable();
        }

        public void LocalizeGui(final String fileName, idLangDict langDict) throws idException {
            idStr out = new idStr(), ws = new idStr(), work;
            ByteBuffer[] buffer = {null};
            out.Empty();
            int k;
            char ch;
            char slash = '\\';
            char tab = 't';
            char nl = 'n';
            idLexer src = new idLexer(LEXFL_NOFATALERRORS | LEXFL_NOSTRINGCONCAT | LEXFL_ALLOWMULTICHARLITERALS | LEXFL_ALLOWBACKSLASHSTRINGCONCAT);
            if (fileSystem.ReadFile(fileName, buffer) > 0) {
                src.LoadMemory(bbtocb(buffer[0]), bbtocb(buffer[0]).capacity(), fileName);
                if (src.IsLoaded()) {
                    idFile outFile = fileSystem.OpenFileWrite(fileName);
                    common.Printf("Processing %s\n", fileName);
                    session.UpdateScreen();
                    idToken token = new idToken();
                    while (src.ReadToken(token)) {
                        src.GetLastWhiteSpace(ws);
                        out.Append(ws);
                        if (token.type == TT_STRING) {
                            out.Append(va("\"%s\"", token));
                        } else {
                            out.Append(token);
                        }
                        if (out.Length() > 200000) {
                            outFile.WriteString(out/*, out.Length()*/);
                            out.oSet("");
                        }
                        work = token.Right(6);
                        if (token.Icmp("text") == 0 || work.Icmp("::text") == 0 || token.Icmp("choices") == 0) {
                            if (src.ReadToken(token)) {
                                // see if already exists, if so save that id to this position in this file
                                // otherwise add this to the list and save the id to this position in this file
                                src.GetLastWhiteSpace(ws);
                                out.Append(ws);
                                token.oSet(langDict.AddString(token.toString()));
                                out.Append("\"");
                                for (k = 0; k < token.Length(); k++) {
                                    ch = token.oGet(k);
                                    if (ch == '\t') {
                                        out.Append(slash);
                                        out.Append(tab);
                                    } else if (ch == '\n' || ch == '\r') {
                                        out.Append(slash);
                                        out.Append(nl);
                                    } else {
                                        out.Append(ch);
                                    }
                                }
                                out.Append("\"");
                            }
                        } else if (token.Icmp("comment") == 0) {
                            if (src.ReadToken(token)) {
                                // need to write these out by hand to preserve any \n's
                                // see if already exists, if so save that id to this position in this file
                                // otherwise add this to the list and save the id to this position in this file
                                src.GetLastWhiteSpace(ws);
                                out.Append(ws);
                                out.Append("\"");
                                for (k = 0; k < token.Length(); k++) {
                                    ch = token.oGet(k);
                                    if (ch == '\t') {
                                        out.Append(slash);
                                        out.Append(tab);
                                    } else if (ch == '\n' || ch == '\r') {
                                        out.Append(slash);
                                        out.Append(nl);
                                    } else {
                                        out.Append(ch);
                                    }
                                }
                                out.Append("\"");
                            }
                        }
                    }
                    outFile.WriteString(out);
                    fileSystem.CloseFile(outFile);
                }
                fileSystem.FreeFile(buffer);
            }
        }

        public void LocalizeMapData(final String fileName, idLangDict langDict) throws idException {
            ByteBuffer[] buffer = {null};
            idLexer src = new idLexer(LEXFL_NOFATALERRORS | LEXFL_NOSTRINGCONCAT | LEXFL_ALLOWMULTICHARLITERALS | LEXFL_ALLOWBACKSLASHSTRINGCONCAT);

            common.SetRefreshOnPrint(true);

            if (fileSystem.ReadFile(fileName, buffer) > 0) {
                src.LoadMemory(bbtocb(buffer[0]), bbtocb(buffer[0]).capacity(), fileName);
                if (src.IsLoaded()) {
                    common.Printf("Processing %s\n", fileName);
                    idStr mapFileName;
                    idToken token = new idToken(), token2 = new idToken();
                    idLangDict replaceArgs = new idLangDict();
                    while (src.ReadToken(token)) {
                        mapFileName = token;
                        replaceArgs.Clear();
                        src.ExpectTokenString("{");
                        while (src.ReadToken(token)) {
                            if (token.equals("}")) {
                                break;
                            }
                            if (src.ReadToken(token2)) {
                                if (token2.equals("}")) {
                                    break;
                                }
                                replaceArgs.AddKeyVal(token.toString(), token2.toString());
                            }
                        }
                        common.Printf("  localizing map %s...\n", mapFileName);
                        LocalizeSpecificMapData(mapFileName.toString(), langDict, replaceArgs);
                    }
                }
                fileSystem.FreeFile(buffer);
            }

            common.SetRefreshOnPrint(false);
        }

        public void LocalizeSpecificMapData(final String fileName, idLangDict langDict, final idLangDict replaceArgs) throws idException {
//	idStr out, ws, work;

            idMapFile map = new idMapFile();
            if (map.Parse(fileName, false, false)) {
                int count = map.GetNumEntities();
                for (int i = 0; i < count; i++) {
                    idMapEntity ent = map.GetEntity(i);
                    if (ent != null) {
                        for (int j = 0; j < replaceArgs.GetNumKeyVals(); j++) {
                            final idLangKeyValue kv = replaceArgs.GetKeyVal(j);
                            final String temp = ent.epairs.GetString(kv.key.toString());
                            if (temp != null && !temp.isEmpty()) {
                                idStr val = kv.value;
                                if (val.toString().equals(temp)) {
                                    ent.epairs.Set(kv.key.toString(), langDict.AddString(temp));
                                }
                            }
                        }
                    }
                }
                map.Write(fileName, ".map");
            }
        }

        public void SetMachineSpec() throws idException {
            long cpuid_t = Sys_GetProcessorId();
            double ghz = Sys_ClockTicksPerSecond() * 0.000000001f;
            int cores = Runtime.getRuntime().availableProcessors();
            int vidRam = 512;// Sys_GetVideoRam();
            int sysRam = Sys_GetSystemRam();
            boolean[] oldCard = {false};
            boolean[] nv10or20 = {false};

            renderSystem.GetCardCaps(oldCard, nv10or20);

            Printf("Detected\n \t%d x %.2f GHz CPU\n\t%d MB of System memory\n\t%d MB"
                    + " of Video memory on %s\n\n", cores, ghz, sysRam, vidRam,
                    (oldCard[0]) ? "a less than optimal video architecture"
                            : "an optimal video architecture");

            if (ghz >= 2.75f && vidRam >= 512 && sysRam >= 1024 && !oldCard[0]) {//TODO:try to make this shit work.
                Printf("This system qualifies for Ultra quality!\n");
                com_machineSpec.SetInteger(3);
            } else if (ghz >= ((cpuid_t & CPUID_AMD) != 0 ? 1.9f : 2.19f) && vidRam >= 256 && sysRam >= 512 && !oldCard[0]) {
                Printf("This system qualifies for High quality!\n");
                com_machineSpec.SetInteger(2);
            } else if (ghz >= ((cpuid_t & CPUID_AMD) != 0 ? 1.1f : 1.25f) && vidRam >= 128 && sysRam >= 384) {
                Printf("This system qualifies for Medium quality.\n");
                com_machineSpec.SetInteger(1);
            } else {
                Printf("This system qualifies for Low quality.\n");
                com_machineSpec.SetInteger(0);
            }
            com_videoRam.SetInteger(vidRam);
        }

        private void InitCommands() throws idException {
            cmdSystem.AddCommand("error", Com_Error_f.getInstance(), CMD_FL_SYSTEM | CMD_FL_CHEAT, "causes an error");
            cmdSystem.AddCommand("crash", Com_Crash_f.getInstance(), CMD_FL_SYSTEM | CMD_FL_CHEAT, "causes a crash");
            cmdSystem.AddCommand("freeze", Com_Freeze_f.getInstance(), CMD_FL_SYSTEM | CMD_FL_CHEAT, "freezes the game for a number of seconds");
            cmdSystem.AddCommand("quit", Com_Quit_f.getInstance(), CMD_FL_SYSTEM, "quits the game");
            cmdSystem.AddCommand("exit", Com_Quit_f.getInstance(), CMD_FL_SYSTEM, "exits the game");
            cmdSystem.AddCommand("writeConfig", Com_WriteConfig_f.getInstance(), CMD_FL_SYSTEM, "writes a config file");
            cmdSystem.AddCommand("reloadEngine", Com_ReloadEngine_f.getInstance(), CMD_FL_SYSTEM, "reloads the engine down to including the file system");
            cmdSystem.AddCommand("setMachineSpec", Com_SetMachineSpec_f.getInstance(), CMD_FL_SYSTEM, "detects system capabilities and sets com_machineSpec to appropriate value");
            cmdSystem.AddCommand("execMachineSpec", Com_ExecMachineSpec_f.getInstance(), CMD_FL_SYSTEM, "execs the appropriate config files and sets cvars based on com_machineSpec");

            if (!ID_DEMO_BUILD && !ID_DEDICATED) {
                // compilers
                cmdSystem.AddCommand("dmap", Dmap_f.getInstance(), CMD_FL_TOOL, "compiles a map", idCmdSystem.ArgCompletion_MapName.getInstance());
                cmdSystem.AddCommand("renderbump", RenderBump_f.getInstance(), CMD_FL_TOOL, "renders a bump map", idCmdSystem.ArgCompletion_ModelName.getInstance());
                cmdSystem.AddCommand("renderbumpFlat", RenderBumpFlat_f.getInstance(), CMD_FL_TOOL, "renders a flat bump map", idCmdSystem.ArgCompletion_ModelName.getInstance());
                cmdSystem.AddCommand("runAAS", RunAAS_f.getInstance(), CMD_FL_TOOL, "compiles an AAS file for a map", idCmdSystem.ArgCompletion_MapName.getInstance());
                cmdSystem.AddCommand("runAASDir", RunAASDir_f.getInstance(), CMD_FL_TOOL, "compiles AAS files for all maps in a folder", idCmdSystem.ArgCompletion_MapName.getInstance());
                cmdSystem.AddCommand("runReach", RunReach_f.getInstance(), CMD_FL_TOOL, "calculates reachability for an AAS file", idCmdSystem.ArgCompletion_MapName.getInstance());
                cmdSystem.AddCommand("roq", RoQFileEncode_f.getInstance(), CMD_FL_TOOL, "encodes a roq file");
            }

            if (ID_ALLOW_TOOLS) {
                // editors
                cmdSystem.AddCommand("editor", Com_Editor_f.getInstance(), CMD_FL_TOOL, "launches the level editor Radiant");
                cmdSystem.AddCommand("editLights", Com_EditLights_f.getInstance(), CMD_FL_TOOL, "launches the in-game Light Editor");
                cmdSystem.AddCommand("editSounds", Com_EditSounds_f.getInstance(), CMD_FL_TOOL, "launches the in-game Sound Editor");
                cmdSystem.AddCommand("editDecls", Com_EditDecls_f.getInstance(), CMD_FL_TOOL, "launches the in-game Declaration Editor");
                cmdSystem.AddCommand("editAFs", Com_EditAFs_f.getInstance(), CMD_FL_TOOL, "launches the in-game Articulated Figure Editor");
                cmdSystem.AddCommand("editParticles", Com_EditParticles_f.getInstance(), CMD_FL_TOOL, "launches the in-game Particle Editor");
                cmdSystem.AddCommand("editScripts", Com_EditScripts_f.getInstance(), CMD_FL_TOOL, "launches the in-game Script Editor");
                cmdSystem.AddCommand("editGUIs", Com_EditGUIs_f.getInstance(), CMD_FL_TOOL, "launches the GUI Editor");
                cmdSystem.AddCommand("editPDAs", Com_EditPDAs_f.getInstance(), CMD_FL_TOOL, "launches the in-game PDA Editor");
                cmdSystem.AddCommand("debugger", Com_ScriptDebugger_f.getInstance(), CMD_FL_TOOL, "launches the Script Debugger");

                //BSM Nerve: Add support for the material editor
                cmdSystem.AddCommand("materialEditor", Com_MaterialEditor_f.getInstance(), CMD_FL_TOOL, "launches the Material Editor");
            }

            cmdSystem.AddCommand("printMemInfo", PrintMemInfo_f.getInstance(), CMD_FL_SYSTEM, "prints memory debugging data");

            // idLib commands
//            cmdSystem.AddCommand("memoryDump", Mem_Dump_f.getInstance(), CMD_FL_SYSTEM | CMD_FL_CHEAT, "creates a memory dump");
//            cmdSystem.AddCommand("memoryDumpCompressed", Mem_DumpCompressed_f.getInstance(), CMD_FL_SYSTEM | CMD_FL_CHEAT, "creates a compressed memory dump");
            cmdSystem.AddCommand("showStringMemory", idStr.ShowMemoryUsage_f.getInstance(), CMD_FL_SYSTEM, "shows memory used by strings");
            cmdSystem.AddCommand("showDictMemory", idDict.ShowMemoryUsage_f.getInstance(), CMD_FL_SYSTEM, "shows memory used by dictionaries");
            cmdSystem.AddCommand("listDictKeys", idDict.ListKeys_f.getInstance(), CMD_FL_SYSTEM | CMD_FL_CHEAT, "lists all keys used by dictionaries");
            cmdSystem.AddCommand("listDictValues", idDict.ListValues_f.getInstance(), CMD_FL_SYSTEM | CMD_FL_CHEAT, "lists all values used by dictionaries");
            cmdSystem.AddCommand("testSIMD", idSIMD.Test_f.getInstance(), CMD_FL_SYSTEM | CMD_FL_CHEAT, "test SIMD code");

            // localization
            cmdSystem.AddCommand("localizeGuis", Com_LocalizeGuis_f.getInstance(), CMD_FL_SYSTEM | CMD_FL_CHEAT, "localize guis");
            cmdSystem.AddCommand("localizeMaps", Com_LocalizeMaps_f.getInstance(), CMD_FL_SYSTEM | CMD_FL_CHEAT, "localize maps");
            cmdSystem.AddCommand("reloadLanguage", Com_ReloadLanguage_f.getInstance(), CMD_FL_SYSTEM, "reload language dict");

            //D3XP Localization
            cmdSystem.AddCommand("localizeGuiParmsTest", Com_LocalizeGuiParmsTest_f.getInstance(), CMD_FL_SYSTEM, "Create test files that show gui parms localized and ignored.");
            cmdSystem.AddCommand("localizeMapsTest", Com_LocalizeMapsTest_f.getInstance(), CMD_FL_SYSTEM, "Create test files that shows which strings will be localized.");

            // build helpers
            cmdSystem.AddCommand("startBuild", Com_StartBuild_f.getInstance(), CMD_FL_SYSTEM | CMD_FL_CHEAT, "prepares to make a build");
            cmdSystem.AddCommand("finishBuild", Com_FinishBuild_f.getInstance(), CMD_FL_SYSTEM | CMD_FL_CHEAT, "finishes the build process");

            if (ID_DEDICATED) {
                cmdSystem.AddCommand("help", Com_Help_f.getInstance(), CMD_FL_SYSTEM, "shows help");
            }
        }

        private void InitRenderSystem() {
            if (com_skipRenderer.GetBool()) {
                return;
            }

            renderSystem.InitOpenGL();
            PrintLoadingMessage(common.GetLanguageDict().GetString("#str_04343"));
        }

        private void InitSIMD() {
            idSIMD.InitProcessor("doom", com_forceGenericSIMD.GetBool());
            com_forceGenericSIMD.ClearModified();
        }

        /*
         ==================
         idCommonLocal::AddStartupCommands

         Adds command line parameters as script statements
         Commands are separated by + signs

         Returns true if any late commands were added, which
         will keep the demoloop from immediately starting
         ==================
         */
        private boolean AddStartupCommands() throws idException {
            int i;
            boolean added;

            added = false;
            // quote every token, so args with semicolons can work
            for (i = 0; i < com_numConsoleLines; i++) {
                if (0 == com_consoleLines[i].Argc()) {
                    continue;
                }

                // set commands won't override menu startup
                if (idStr.Icmpn(com_consoleLines[i].Argv(0), "set", 3) != 0) {
                    added = true;
                }
                // directly as tokenized so nothing gets screwed
                cmdSystem.BufferCommandArgs(CMD_EXEC_APPEND, com_consoleLines[i]);
            }

            return added;
        }

        private void ParseCommandLine(int argc, final String[] argv) {
            int i, current_count;

            com_numConsoleLines = 0;
            current_count = 0;
            // API says no program path
            for (i = 0; i < argc; i++) {
                if (argv[i].charAt(0) == '+') {
                    com_numConsoleLines++;
                    com_consoleLines[com_numConsoleLines - 1].AppendArg(argv[i].substring(1));
                } else {
                    if (0 == com_numConsoleLines) {
                        com_numConsoleLines++;
                    }
                    com_consoleLines[com_numConsoleLines - 1].AppendArg(argv[i]);
                }
            }
        }

        private void ClearCommandLine() {
            com_numConsoleLines = 0;
        }

        /*
         ==================
         idCommonLocal::SafeMode

         Check for "safe" on the command line, which will
         skip loading of config file (DoomConfig.cfg)
         ==================
         */
        private boolean SafeMode() {
            int i;

            for (i = 0; i < com_numConsoleLines; i++) {
                if (0 == idStr.Icmp(com_consoleLines[i].Argv(0), "safe")
                        || 0 == idStr.Icmp(com_consoleLines[i].Argv(0), "cvar_restart")) {
                    com_consoleLines[i].Clear();
                    return true;
                }
            }
            return false;
        }

        /*
         ==================
         idCommonLocal::CheckToolMode

         Check for "renderbump", "dmap", or "editor" on the command line,
         and force fullscreen off in those cases
         ==================
         */
        private void CheckToolMode() {
            int i;

            for (i = 0; i < com_numConsoleLines; i++) {
                if (0 == idStr.Icmp(com_consoleLines[i].Argv(0), "guieditor")) {
                    com_editors |= EDITOR_GUI;
                } else if (0 == idStr.Icmp(com_consoleLines[i].Argv(0), "debugger")) {
                    com_editors |= EDITOR_DEBUGGER;
                } else if (0 == idStr.Icmp(com_consoleLines[i].Argv(0), "editor")) {
                    com_editors |= EDITOR_RADIANT;
                } // Nerve: Add support for the material editor
                else if (0 == idStr.Icmp(com_consoleLines[i].Argv(0), "materialEditor")) {
                    com_editors |= EDITOR_MATERIAL;
                }

                if (0 == idStr.Icmp(com_consoleLines[i].Argv(0), "renderbump")
                        || 0 == idStr.Icmp(com_consoleLines[i].Argv(0), "editor")
                        || 0 == idStr.Icmp(com_consoleLines[i].Argv(0), "guieditor")
                        || 0 == idStr.Icmp(com_consoleLines[i].Argv(0), "debugger")
                        || 0 == idStr.Icmp(com_consoleLines[i].Argv(0), "dmap")
                        || 0 == idStr.Icmp(com_consoleLines[i].Argv(0), "materialEditor")) {
                    cvarSystem.SetCVarBool("r_fullscreen", false);
                    return;
                }
            }
        }

        private void CloseLogFile() {
            if (logFile != null) {
                com_logFile.SetBool(false); // make sure no further VPrintf attempts to open the log file again
                fileSystem.CloseFile(logFile);
                logFile = null;
            }
        }

        /*
         ===============
         idCommonLocal::WriteConfiguration

         Writes key bindings and archived cvars to config file if modified
         ===============
         */
        private void WriteConfiguration() {
            // if we are quiting without fully initializing, make sure
            // we don't write out anything
            if (!com_fullyInitialized) {
                return;
            }

            if (0 == (cvarSystem.GetModifiedFlags() & CVAR_ARCHIVE)) {
                return;
            }
            cvarSystem.ClearModifiedFlags(CVAR_ARCHIVE);

            // disable printing out the "Writing to:" message
            boolean developer = com_developer.GetBool();
            com_developer.SetBool(false);

            WriteConfigToFile(CONFIG_FILE);
            session.WriteCDKey();

            // restore the developer cvar
            com_developer.SetBool(developer);
        }

        private void DumpWarnings() {
            int i;
            idFile warningFile;

            if (0 == warningList.Num()) {
                return;
            }

            warningFile = fileSystem.OpenFileWrite("warnings.txt", "fs_savepath");
            if (warningFile != null) {

                warningFile.Printf("------------- Warnings ---------------\n\n");
                warningFile.Printf("during %s...\n", warningCaption);
                warningList.Sort();
                for (i = 0; i < warningList.Num(); i++) {
                    warningList.oGet(i).RemoveColors();
                    warningFile.Printf("WARNING: %s\n", warningList.oGet(i));
                }
                if (warningList.Num() >= MAX_WARNING_LIST) {
                    warningFile.Printf("\nmore than %d warnings!\n", MAX_WARNING_LIST);
                } else {
                    warningFile.Printf("\n%d warnings.\n", warningList.Num());
                }

                warningFile.Printf("\n\n-------------- Errors ---------------\n\n");
                errorList.Sort();
                for (i = 0; i < errorList.Num(); i++) {
                    errorList.oGet(i).RemoveColors();
                    warningFile.Printf("ERROR: %s", errorList.oGet(i));
                }

                warningFile.ForceFlush();

                fileSystem.CloseFile(warningFile);

                if (_WIN32 && !_DEBUG) {
                    String osPath;
                    osPath = fileSystem.RelativePathToOSPath("warnings.txt", "fs_savepath");
                    try {
//                    WinExec(va("Notepad.exe %s", osPath.c_str()), SW_SHOW);
                        Runtime.getRuntime().exec(va("Notepad.exe %s", osPath));
                    } catch (IOException ex) {
                        Logger.getLogger(Common.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        }

        private /*synchronized*/ void SingleAsyncTic() {
            // main thread code can prevent this from happening while modifying
            // critical data structures
            Sys_EnterCriticalSection();
            try {
                asyncStats_t stat = com_asyncStats[com_ticNumber & (MAX_ASYNC_STATS - 1)];//memset( stat, 0, sizeof( *stat ) );
                stat.milliseconds = Sys_Milliseconds();
                stat.deltaMsec = stat.milliseconds - com_asyncStats[(com_ticNumber - 1) & (MAX_ASYNC_STATS - 1)].milliseconds;

                if (usercmdGen != null && com_asyncInput.GetBool()) {
                    usercmdGen.UsercmdInterrupt();
                }

                switch (com_asyncSound.GetInteger()) {
                    case 1:
                        soundSystem.AsyncUpdate(stat.milliseconds);
                        break;
                    case 3:
                        soundSystem.AsyncUpdateWrite(stat.milliseconds);
                        break;
                }

                // we update com_ticNumber after all the background tasks
                // have completed their work for this tic
                com_ticNumber++;
//                System.out.println(System.nanoTime()+"com_ticNumber=" + com_ticNumber);
                stat.timeConsumed = Sys_Milliseconds() - stat.milliseconds;
            } finally {
                Sys_LeaveCriticalSection();
            }
        }
        static float DEBUG_fraction = 0;

        private void LoadGameDLL() throws idException {
//            if (__DOOM_DLL__) {
//                char[] dllPath = new char[MAX_OSPATH];
//
//                gameImport_t gameImport = new gameImport_t();
//                gameExport_t gameExport;
//                GetGameAPI_t GetGameAPI;
//
//                fileSystem.FindDLL("game", dllPath, true);
//
//                if ('\0' == dllPath[0]) {
//                    common.FatalError("couldn't find game dynamic library");
//                    return;
//                }
//                common.DPrintf("Loading game DLL: '%s'\n", new String(dllPath));
//                gameDLL = sys.DLL_Load(new String(dllPath));
//                if (0 == gameDLL) {
//                    common.FatalError("couldn't load game dynamic library");
//                    return;
//                }
//
//                GetGameAPI = Sys_DLL_GetProcAddress(gameDLL, "GetGameAPI");
//                if (!GetGameAPI) {
//                    Sys_DLL_Unload(gameDLL);
//                    gameDLL = 0;
//                    common.FatalError("couldn't find game DLL API");
//                    return;
//                }
//
//                gameImport.version = GAME_API_VERSION;
//                gameImport.sys = sys;
//                gameImport.common = common;
//                gameImport.cmdSystem = cmdSystem;
//                gameImport.cvarSystem = cvarSystem;
//                gameImport.fileSystem = fileSystem;
//                gameImport.networkSystem = networkSystem;
//                gameImport.renderSystem = renderSystem;
//                gameImport.soundSystem = soundSystem;
//                gameImport.renderModelManager = renderModelManager;
//                gameImport.uiManager = uiManager;
//                gameImport.declManager = declManager;
//                gameImport.AASFileManager = AASFileManager;
//                gameImport.collisionModelManager = collisionModelManager;
//
//                gameExport = GetGameAPI(gameImport);
//
//                if (gameExport.version != GAME_API_VERSION) {
//                    Sys_DLL_Unload(gameDLL);
//                    gameDLL = 0;
//                    common.FatalError("wrong game DLL API version");
//                    return;
//                }
//
//                game = gameExport.game;
//                gameEdit = gameExport.gameEdit;
//
//            }

            // initialize the game object
            if (game != null) {
                game.Init();
            }
        }

        private void UnloadGameDLL() {

            // shut down the game object
            if (game != null) {
                game.Shutdown();
            }

//            if (__DOOM_DLL__) {
//
//                if (gameDLL) {
//                    Sys_DLL_Unload(gameDLL);
//                    gameDLL = null;
//                }
//                game = null;
//                gameEdit = null;
//
//            }
        }

        private void PrintLoadingMessage(final String msg) {
            if (msg == null || msg.isEmpty()) {
                return;
            }
            renderSystem.BeginFrame(renderSystem.GetScreenWidth(), renderSystem.GetScreenHeight());
            renderSystem.DrawStretchPic(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT, 0, 0, 1, 1, declManager.FindMaterial("splashScreen"));
            int len = msg.length();
            renderSystem.DrawSmallStringExt((640 - len * SMALLCHAR_WIDTH) / 2, 410, msg.toCharArray(),
                    new idVec4(0.0f, 0.81f, 0.94f, 1.0f), true, declManager.FindMaterial("textures/bigchars"));
            renderSystem.EndFrame(null, null);
        }

        private void FilterLangList(idStrList list, idStr lang) {

            idStr temp;
            for (int i = 0; i < list.Num(); i++) {
                temp = list.oGet(i);
                temp = temp.Right(temp.Length() - ("strings/").length());
                temp = temp.Left(lang.Length());
                if (idStr.Icmp(temp, lang) != 0) {
                    list.RemoveIndex(i);
                    i--;
                }
            }
        }
    };
//    
//    
//    
//    
//    
//    
//    
//    
//    
    //============================================================================


    /*
     ==================
     Com_Editor_f

     we can start the editor dynamically, but we won't ever get back
     ==================
     */
    static class Com_Editor_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new Com_Editor_f();

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) {
            RadiantInit();
        }
    };

    /*
     =============
     Com_ScriptDebugger_f
     =============
     */
    static class Com_ScriptDebugger_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new Com_ScriptDebugger_f();

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) throws idException {
            // Make sure it wasnt on the command line
            if (0 == (com_editors & EDITOR_DEBUGGER)) {
                common.Printf("Script debugger is currently disabled\n");
                // DebuggerClientLaunch();
            }
        }
    };

    /*
     =============
     Com_EditGUIs_f
     =============
     */
    static class Com_EditGUIs_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new Com_EditGUIs_f();

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) {
            GUIEditorInit();
        }
    };

    /*
     =============
     Com_MaterialEditor_f
     =============
     */
    static class Com_MaterialEditor_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new Com_MaterialEditor_f();

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) {
            // Turn off sounds
            soundSystem.SetMute(true);
            MaterialEditorInit();
        }
    };


    /*
     ============
     idCmdSystemLocal.PrintMemInfo_f

     This prints out memory debugging data
     ============
     */
    static class PrintMemInfo_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new PrintMemInfo_f();

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) throws idException {
            MemInfo_t mi = new MemInfo_t();//memset( &mi, 0, sizeof( mi ) );

            mi.filebase = new idStr(session.GetCurrentMapName());

            renderSystem.PrintMemInfo(mi);			// textures and models
            soundSystem.PrintMemInfo(mi);			// sounds

            common.Printf(" Used image memory: %s bytes\n", idStr.FormatNumber(mi.imageAssetsTotal));
            mi.assetTotals += mi.imageAssetsTotal;

            common.Printf(" Used model memory: %s bytes\n", idStr.FormatNumber(mi.modelAssetsTotal));
            mi.assetTotals += mi.modelAssetsTotal;

            common.Printf(" Used sound memory: %s bytes\n", idStr.FormatNumber(mi.soundAssetsTotal));
            mi.assetTotals += mi.soundAssetsTotal;

            common.Printf(" Used asset memory: %s bytes\n", idStr.FormatNumber(mi.assetTotals));

            // write overview file
            idFile f;

            f = fileSystem.OpenFileAppend("maps/printmeminfo.txt");
            if (null == f) {
                return;
            }

            f.Printf("total(%s ) image(%s ) model(%s ) sound(%s ): %s\n", idStr.FormatNumber(mi.assetTotals), idStr.FormatNumber(mi.imageAssetsTotal),
                    idStr.FormatNumber(mi.modelAssetsTotal), idStr.FormatNumber(mi.soundAssetsTotal), mi.filebase);

            fileSystem.CloseFile(f);
        }
    };


    /*
     ==================
     Com_EditLights_f
     ==================
     */
    static class Com_EditLights_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new Com_EditLights_f();

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) {
            LightEditorInit(null);
            cvarSystem.SetCVarInteger("g_editEntityMode", 1);
        }
    };

    /*
     ==================
     Com_EditSounds_f
     ==================
     */
    static class Com_EditSounds_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new Com_EditSounds_f();

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) {
            SoundEditorInit(null);
            cvarSystem.SetCVarInteger("g_editEntityMode", 2);
        }
    };

    /*
     ==================
     Com_EditDecls_f
     ==================
     */
    static class Com_EditDecls_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new Com_EditDecls_f();

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) {
            DeclBrowserInit(null);
        }
    }

    /*
     ==================
     Com_EditAFs_f
     ==================
     */
    static class Com_EditAFs_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new Com_EditAFs_f();

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) {
            AFEditorInit(null);
        }
    }

    /*
     ==================
     Com_EditParticles_f
     ==================
     */
    static class Com_EditParticles_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new Com_EditParticles_f();

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) {
            ParticleEditorInit(null);
        }
    };

    /*
     ==================
     Com_EditScripts_f
     ==================
     */
    static class Com_EditScripts_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new Com_EditScripts_f();

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) {
            ScriptEditorInit(null);
        }
    };

    /*
     ==================
     Com_EditPDAs_f
     ==================
     */
    static class Com_EditPDAs_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new Com_EditPDAs_f();

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) {
            PDAEditorInit(null);
        }
    };


    /*
     ==================
     Com_Error_f

     Just throw a fatal error to test error shutdown procedures.
     ==================
     */
    static class Com_Error_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new Com_Error_f();

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) throws idException {
            if (!com_developer.GetBool()) {
                commonLocal.Printf("error may only be used in developer mode\n");
                return;
            }

            if (args.Argc() > 1) {
                commonLocal.FatalError("Testing fatal error");
            } else {
                commonLocal.Error("Testing drop error");
            }
        }
    };

    /*
     ==================
     Com_Freeze_f

     Just freeze in place for a given number of seconds to test error recovery.
     ==================
     */
    static class Com_Freeze_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new Com_Freeze_f();

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) throws idException {
            float s;
            int start, now;

            if (args.Argc() != 2) {
                commonLocal.Printf("freeze <seconds>\n");
                return;
            }

            if (!com_developer.GetBool()) {
                commonLocal.Printf("freeze may only be used in developer mode\n");
                return;
            }

            s = Integer.parseInt(args.Argv(1));

            start = eventLoop.Milliseconds();

            while (true) {
                now = eventLoop.Milliseconds();
                if ((now - start) * 0.001f > s) {
                    break;
                }
            }
        }
    };

    /*
     =================
     Com_Crash_f

     A way to force a bus error for development reasons
     =================
     */
    static class Com_Crash_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new Com_Crash_f();

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) throws idException {
            if (!com_developer.GetBool()) {
                commonLocal.Printf("crash may only be used in developer mode\n");
//                return;
            }

//	* ( int * ) 0 = 0x12345678;//not needed for java
        }
    };

    /*
     =================
     Com_Quit_f
     =================
     */
    static class Com_Quit_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new Com_Quit_f();

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) {
            commonLocal.Quit();
        }
    };

    /*
     ===============
     Com_WriteConfig_f

     Write the config file to a specific name
     ===============
     */
    static class Com_WriteConfig_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new Com_WriteConfig_f();

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) throws idException {
            idStr filename;

            if (args.Argc() != 2) {
                commonLocal.Printf("Usage: writeconfig <filename>\n");
                return;
            }

            filename = new idStr(args.Argv(1));
            filename.DefaultFileExtension(".cfg");
            commonLocal.Printf("Writing %s.\n", filename);
            commonLocal.WriteConfigToFile(filename.toString());
        }
    };

    /*
     =================
     Com_SetMachineSpecs_f
     =================
     */
    static class Com_SetMachineSpec_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new Com_SetMachineSpec_f();

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) throws idException {
            commonLocal.SetMachineSpec();
        }
    };

    /*
     =================
     Com_ExecMachineSpecs_f
     =================
     */
// #ifdef MACOS_X
// void OSX_GetVideoCard( int& outVendorId, int& outDeviceId );
// boolean OSX_GetCPUIdentification( int& cpuId, boolean& oldArchitecture );
// #endif
    static class Com_ExecMachineSpec_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new Com_ExecMachineSpec_f();

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) {
            if (com_machineSpec.GetInteger() == 3) {
                cvarSystem.SetCVarInteger("image_anisotropy", 1, CVAR_ARCHIVE);
                cvarSystem.SetCVarInteger("image_lodbias", 0, CVAR_ARCHIVE);
                cvarSystem.SetCVarInteger("image_forceDownSize", 0, CVAR_ARCHIVE);
                cvarSystem.SetCVarInteger("image_roundDown", 1, CVAR_ARCHIVE);
                cvarSystem.SetCVarInteger("image_preload", 1, CVAR_ARCHIVE);
                cvarSystem.SetCVarInteger("image_useAllFormats", 1, CVAR_ARCHIVE);
                cvarSystem.SetCVarInteger("image_downSizeSpecular", 0, CVAR_ARCHIVE);
                cvarSystem.SetCVarInteger("image_downSizeBump", 0, CVAR_ARCHIVE);
                cvarSystem.SetCVarInteger("image_downSizeSpecularLimit", 64, CVAR_ARCHIVE);
                cvarSystem.SetCVarInteger("image_downSizeBumpLimit", 256, CVAR_ARCHIVE);
                cvarSystem.SetCVarInteger("image_usePrecompressedTextures", 0, CVAR_ARCHIVE);
                cvarSystem.SetCVarInteger("image_downsize", 0, CVAR_ARCHIVE);
                cvarSystem.SetCVarString("image_filter", "GL_LINEAR_MIPMAP_LINEAR", CVAR_ARCHIVE);
                cvarSystem.SetCVarInteger("image_anisotropy", 8, CVAR_ARCHIVE);
                cvarSystem.SetCVarInteger("image_useCompression", 0, CVAR_ARCHIVE);
                cvarSystem.SetCVarInteger("image_ignoreHighQuality", 0, CVAR_ARCHIVE);
                cvarSystem.SetCVarInteger("s_maxSoundsPerShader", 0, CVAR_ARCHIVE);
                cvarSystem.SetCVarInteger("r_mode", 5, CVAR_ARCHIVE);
                cvarSystem.SetCVarInteger("image_useNormalCompression", 0, CVAR_ARCHIVE);
                cvarSystem.SetCVarInteger("r_multiSamples", 0, CVAR_ARCHIVE);
            } else if (com_machineSpec.GetInteger() == 2) {
                cvarSystem.SetCVarString("image_filter", "GL_LINEAR_MIPMAP_LINEAR", CVAR_ARCHIVE);
                cvarSystem.SetCVarInteger("image_anisotropy", 1, CVAR_ARCHIVE);
                cvarSystem.SetCVarInteger("image_lodbias", 0, CVAR_ARCHIVE);
                cvarSystem.SetCVarInteger("image_forceDownSize", 0, CVAR_ARCHIVE);
                cvarSystem.SetCVarInteger("image_roundDown", 1, CVAR_ARCHIVE);
                cvarSystem.SetCVarInteger("image_preload", 1, CVAR_ARCHIVE);
                cvarSystem.SetCVarInteger("image_useAllFormats", 1, CVAR_ARCHIVE);
                cvarSystem.SetCVarInteger("image_downSizeSpecular", 0, CVAR_ARCHIVE);
                cvarSystem.SetCVarInteger("image_downSizeBump", 0, CVAR_ARCHIVE);
                cvarSystem.SetCVarInteger("image_downSizeSpecularLimit", 64, CVAR_ARCHIVE);
                cvarSystem.SetCVarInteger("image_downSizeBumpLimit", 256, CVAR_ARCHIVE);
                cvarSystem.SetCVarInteger("image_usePrecompressedTextures", 1, CVAR_ARCHIVE);
                cvarSystem.SetCVarInteger("image_downsize", 0, CVAR_ARCHIVE);
                cvarSystem.SetCVarInteger("image_anisotropy", 8, CVAR_ARCHIVE);
                cvarSystem.SetCVarInteger("image_useCompression", 1, CVAR_ARCHIVE);
                cvarSystem.SetCVarInteger("image_ignoreHighQuality", 0, CVAR_ARCHIVE);
                cvarSystem.SetCVarInteger("s_maxSoundsPerShader", 0, CVAR_ARCHIVE);
                cvarSystem.SetCVarInteger("image_useNormalCompression", 0, CVAR_ARCHIVE);
                cvarSystem.SetCVarInteger("r_mode", 4, CVAR_ARCHIVE);
                cvarSystem.SetCVarInteger("r_multiSamples", 0, CVAR_ARCHIVE);
            } else if (com_machineSpec.GetInteger() == 1) {
                cvarSystem.SetCVarString("image_filter", "GL_LINEAR_MIPMAP_LINEAR", CVAR_ARCHIVE);
                cvarSystem.SetCVarInteger("image_anisotropy", 1, CVAR_ARCHIVE);
                cvarSystem.SetCVarInteger("image_lodbias", 0, CVAR_ARCHIVE);
                cvarSystem.SetCVarInteger("image_downSize", 0, CVAR_ARCHIVE);
                cvarSystem.SetCVarInteger("image_forceDownSize", 0, CVAR_ARCHIVE);
                cvarSystem.SetCVarInteger("image_roundDown", 1, CVAR_ARCHIVE);
                cvarSystem.SetCVarInteger("image_preload", 1, CVAR_ARCHIVE);
                cvarSystem.SetCVarInteger("image_useCompression", 1, CVAR_ARCHIVE);
                cvarSystem.SetCVarInteger("image_useAllFormats", 1, CVAR_ARCHIVE);
                cvarSystem.SetCVarInteger("image_usePrecompressedTextures", 1, CVAR_ARCHIVE);
                cvarSystem.SetCVarInteger("image_downSizeSpecular", 0, CVAR_ARCHIVE);
                cvarSystem.SetCVarInteger("image_downSizeBump", 0, CVAR_ARCHIVE);
                cvarSystem.SetCVarInteger("image_downSizeSpecularLimit", 64, CVAR_ARCHIVE);
                cvarSystem.SetCVarInteger("image_downSizeBumpLimit", 256, CVAR_ARCHIVE);
                cvarSystem.SetCVarInteger("image_useNormalCompression", 2, CVAR_ARCHIVE);
                cvarSystem.SetCVarInteger("r_mode", 3, CVAR_ARCHIVE);
                cvarSystem.SetCVarInteger("r_multiSamples", 0, CVAR_ARCHIVE);
            } else {
                cvarSystem.SetCVarString("image_filter", "GL_LINEAR_MIPMAP_LINEAR", CVAR_ARCHIVE);
                cvarSystem.SetCVarInteger("image_anisotropy", 1, CVAR_ARCHIVE);
                cvarSystem.SetCVarInteger("image_lodbias", 0, CVAR_ARCHIVE);
                cvarSystem.SetCVarInteger("image_roundDown", 1, CVAR_ARCHIVE);
                cvarSystem.SetCVarInteger("image_preload", 1, CVAR_ARCHIVE);
                cvarSystem.SetCVarInteger("image_useAllFormats", 1, CVAR_ARCHIVE);
                cvarSystem.SetCVarInteger("image_usePrecompressedTextures", 1, CVAR_ARCHIVE);
                cvarSystem.SetCVarInteger("image_downSize", 1, CVAR_ARCHIVE);
                cvarSystem.SetCVarInteger("image_anisotropy", 0, CVAR_ARCHIVE);
                cvarSystem.SetCVarInteger("image_useCompression", 1, CVAR_ARCHIVE);
                cvarSystem.SetCVarInteger("image_ignoreHighQuality", 1, CVAR_ARCHIVE);
                cvarSystem.SetCVarInteger("s_maxSoundsPerShader", 1, CVAR_ARCHIVE);
                cvarSystem.SetCVarInteger("image_downSizeSpecular", 1, CVAR_ARCHIVE);
                cvarSystem.SetCVarInteger("image_downSizeBump", 1, CVAR_ARCHIVE);
                cvarSystem.SetCVarInteger("image_downSizeSpecularLimit", 64, CVAR_ARCHIVE);
                cvarSystem.SetCVarInteger("image_downSizeBumpLimit", 256, CVAR_ARCHIVE);
                cvarSystem.SetCVarInteger("r_mode", 3, CVAR_ARCHIVE);
                cvarSystem.SetCVarInteger("image_useNormalCompression", 2, CVAR_ARCHIVE);
                cvarSystem.SetCVarInteger("r_multiSamples", 0, CVAR_ARCHIVE);
            }

            if (Sys_GetVideoRam() < 128) {
                cvarSystem.SetCVarBool("image_ignoreHighQuality", true, CVAR_ARCHIVE);
                cvarSystem.SetCVarInteger("image_downSize", 1, CVAR_ARCHIVE);
                cvarSystem.SetCVarInteger("image_downSizeLimit", 256, CVAR_ARCHIVE);
                cvarSystem.SetCVarInteger("image_downSizeSpecular", 1, CVAR_ARCHIVE);
                cvarSystem.SetCVarInteger("image_downSizeSpecularLimit", 64, CVAR_ARCHIVE);
                cvarSystem.SetCVarInteger("image_downSizeBump", 1, CVAR_ARCHIVE);
                cvarSystem.SetCVarInteger("image_downSizeBumpLimit", 256, CVAR_ARCHIVE);
            }

            if (Sys_GetSystemRam() < 512) {
                cvarSystem.SetCVarBool("image_ignoreHighQuality", true, CVAR_ARCHIVE);
                cvarSystem.SetCVarInteger("s_maxSoundsPerShader", 1, CVAR_ARCHIVE);
                cvarSystem.SetCVarInteger("image_downSize", 1, CVAR_ARCHIVE);
                cvarSystem.SetCVarInteger("image_downSizeLimit", 256, CVAR_ARCHIVE);
                cvarSystem.SetCVarInteger("image_downSizeSpecular", 1, CVAR_ARCHIVE);
                cvarSystem.SetCVarInteger("image_downSizeSpecularLimit", 64, CVAR_ARCHIVE);
                cvarSystem.SetCVarBool("com_purgeAll", true, CVAR_ARCHIVE);
                cvarSystem.SetCVarBool("r_forceLoadImages", true, CVAR_ARCHIVE);
            } else {
                cvarSystem.SetCVarBool("com_purgeAll", false, CVAR_ARCHIVE);
                cvarSystem.SetCVarBool("r_forceLoadImages", false, CVAR_ARCHIVE);
            }

            boolean[] oldCard = {false};
            boolean[] nv10or20 = {false};
            renderSystem.GetCardCaps(oldCard, nv10or20);
            if (oldCard[0]) {
                cvarSystem.SetCVarBool("g_decals", false, CVAR_ARCHIVE);
                cvarSystem.SetCVarBool("g_projectileLights", false, CVAR_ARCHIVE);
                cvarSystem.SetCVarBool("g_doubleVision", false, CVAR_ARCHIVE);
                cvarSystem.SetCVarBool("g_muzzleFlash", false, CVAR_ARCHIVE);
            } else {
                cvarSystem.SetCVarBool("g_decals", true, CVAR_ARCHIVE);
                cvarSystem.SetCVarBool("g_projectileLights", true, CVAR_ARCHIVE);
                cvarSystem.SetCVarBool("g_doubleVision", true, CVAR_ARCHIVE);
                cvarSystem.SetCVarBool("g_muzzleFlash", true, CVAR_ARCHIVE);
            }
            if (nv10or20[0]) {
                cvarSystem.SetCVarInteger("image_useNormalCompression", 1, CVAR_ARCHIVE);
            }

//if( MACOS_X){
//	// On low settings, G4 systems & 64MB FX5200/NV34 Systems should default shadows off
//	boolean oldArch;
//	int vendorId, deviceId, cpuId;
//	OSX_GetVideoCard( vendorId, deviceId );
//	OSX_GetCPUIdentification( cpuId, oldArch );
//	boolean isFX5200 = vendorId == 0x10DE && ( deviceId & 0x0FF0 ) == 0x0320;
//	if ( ( oldArch || ( isFX5200 && Sys_GetVideoRam() < 128 ) ) && com_machineSpec.GetInteger() == 0 ) {
//		cvarSystem.SetCVarBool( "r_shadows", false, CVAR_ARCHIVE );
//	} else {
//		cvarSystem.SetCVarBool( "r_shadows", true, CVAR_ARCHIVE );
//	}
//}
        }
    };

    /*
     =================
     Com_ReloadEngine_f
     =================
     */
    static class Com_ReloadEngine_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new Com_ReloadEngine_f();

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) throws idException {
            boolean menu = false;

            if (!commonLocal.IsInitialized()) {
                return;
            }

            if (args.Argc() > 1 && idStr.Icmp(args.Argv(1), "menu") == 0) {
                menu = true;
            }

            common.Printf("============= ReloadEngine start =============\n");
            if (!menu) {
                Sys_ShowConsole(1, false);
            }
            commonLocal.ShutdownGame(true);
            commonLocal.InitGame();
            if (!menu && !idAsyncNetwork.serverDedicated.GetBool()) {
                Sys_ShowConsole(0, false);
            }
            common.Printf("============= ReloadEngine end ===============\n");

            if (!cmdSystem.PostReloadEngine()) {
                if (menu) {
                    session.StartMenu();
                }
            }
        }
    };

    static class ListHash extends idHashTable<idStrList> {

    }

    /*
     =================
     LocalizeMaps_f
     =================
     */
    static class Com_LocalizeMaps_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new Com_LocalizeMaps_f();

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) throws idException {
            if (args.Argc() < 2) {
                common.Printf("Usage: localizeMaps <count | dictupdate | all> <map>\n");
                return;
            }

            int strCount = 0;

            boolean count;
            boolean dictUpdate = false;
            boolean write = false;

            if (idStr.Icmp(args.Argv(1), "count") == 0) {
                count = true;
            } else if (idStr.Icmp(args.Argv(1), "dictupdate") == 0) {
                count = true;
                dictUpdate = true;
            } else if (idStr.Icmp(args.Argv(1), "all") == 0) {
                count = true;
                dictUpdate = true;
                write = true;
            } else {
                common.Printf("Invalid Command\n");
                common.Printf("Usage: localizeMaps <count | dictupdate | all>\n");
                return;

            }

            idLangDict strTable = new idLangDict();
            String filename = va("strings/english%.3i.lang", com_product_lang_ext.GetInteger());
            if (strTable.Load(filename) == false) {
                //This is a new file so set the base index
                strTable.SetBaseID(com_product_lang_ext.GetInteger() * 100000);
            }

            common.SetRefreshOnPrint(true);

            ListHash listHash = new ListHash();
            LoadMapLocalizeData(listHash);

            idStrList excludeList = new idStrList();
            LoadGuiParmExcludeList(excludeList);

            if (args.Argc() == 3) {
                strCount += LocalizeMap(args.Argv(2), strTable, listHash, excludeList, write);
            } else {
                idStrList files = new idStrList();
                GetFileList("z:/d3xp/d3xp/maps/game", "*.map", files);
                for (int i = 0; i < files.Num(); i++) {
                    String file = fileSystem.OSPathToRelativePath(files.oGet(i).toString());
                    strCount += LocalizeMap(file, strTable, listHash, excludeList, write);
                }
            }

            if (count) {
                common.Printf("Localize String Count: %d\n", strCount);
            }

            common.SetRefreshOnPrint(false);

            if (dictUpdate) {
                strTable.Save(filename);
            }
        }
    };

    /*
     =================
     LocalizeGuis_f
     =================
     */
    static class Com_LocalizeGuis_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new Com_LocalizeGuis_f();

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) throws idException {

            if (args.Argc() != 2) {
                common.Printf("Usage: localizeGuis <all | gui>\n");
                return;
            }

            idLangDict strTable = new idLangDict();

            String filename = va("strings/english%.3i.lang", com_product_lang_ext.GetInteger());
            if (strTable.Load(filename) == false) {
                //This is a new file so set the base index
                strTable.SetBaseID(com_product_lang_ext.GetInteger() * 100000);
            }

            idFileList files;
            if (idStr.Icmp(args.Argv(1), "all") == 0) {
                String game = cvarSystem.GetCVarString("fs_game");
                if (!game.isEmpty()) {
                    files = fileSystem.ListFilesTree("guis", "*.gui", true, game);
                } else {
                    files = fileSystem.ListFilesTree("guis", "*.gui", true);
                }
                for (int i = 0; i < files.GetNumFiles(); i++) {
                    commonLocal.LocalizeGui(files.GetFile(i), strTable);
                }
                fileSystem.FreeFileList(files);

                if (game.length() != 0) {
                    files = fileSystem.ListFilesTree("guis", "*.pd", true, game);
                } else {
                    files = fileSystem.ListFilesTree("guis", "*.pd", true, "d3xp");
                }

                for (int i = 0; i < files.GetNumFiles(); i++) {
                    commonLocal.LocalizeGui(files.GetFile(i), strTable);
                }
                fileSystem.FreeFileList(files);

            } else {
                commonLocal.LocalizeGui(args.Argv(1), strTable);
            }
            strTable.Save(filename);
        }
    };

    static class Com_LocalizeGuiParmsTest_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new Com_LocalizeGuiParmsTest_f();

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) throws idException {

            common.SetRefreshOnPrint(true);

            idFile localizeFile = fileSystem.OpenFileWrite("gui_parm_localize.csv");
            idFile noLocalizeFile = fileSystem.OpenFileWrite("gui_parm_nolocalize.csv");

            idStrList excludeList = new idStrList();
            LoadGuiParmExcludeList(excludeList);

            idStrList files = new idStrList();
            GetFileList("z:/d3xp/d3xp/maps/game", "*.map", files);

            for (int i = 0; i < files.Num(); i++) {

                common.Printf("Testing Map '%s'\n", files.oGet(i));
                idMapFile map = new idMapFile();

                String file = fileSystem.OSPathToRelativePath(files.oGet(i).toString());
                if (map.Parse(file, false, false)) {
                    int count = map.GetNumEntities();
                    for (int j = 0; j < count; j++) {
                        idMapEntity ent = map.GetEntity(j);
                        if (ent != null) {
                            idKeyValue kv = ent.epairs.MatchPrefix("gui_parm");
                            while (kv != null) {
                                if (TestGuiParm(kv.GetKey(), kv.GetValue(), excludeList)) {
                                    String out = va("%s,%s,%s\r\n", kv.GetValue(), kv.GetKey(), file);
                                    localizeFile.WriteString(out);
                                } else {
                                    String out = va("%s,%s,%s\r\n", kv.GetValue(), kv.GetKey(), file);
                                    noLocalizeFile.WriteString(out);//TODO:writeString?
                                }
                                kv = ent.epairs.MatchPrefix("gui_parm", kv);
                            }
                        }
                    }
                }
            }

            fileSystem.CloseFile(localizeFile);
            fileSystem.CloseFile(noLocalizeFile);

            common.SetRefreshOnPrint(false);
        }
    };

    static class Com_LocalizeMapsTest_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new Com_LocalizeMapsTest_f();

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) throws idException {

            ListHash listHash = new ListHash();
            LoadMapLocalizeData(listHash);

            common.SetRefreshOnPrint(true);

            idFile localizeFile = fileSystem.OpenFileWrite("map_localize.csv");

            idStrList files = new idStrList();
            GetFileList("z:/d3xp/d3xp/maps/game", "*.map", files);

            for (int i = 0; i < files.Num(); i++) {

                common.Printf("Testing Map '%s'\n", files.oGet(i));
                idMapFile map = new idMapFile();

                String file = fileSystem.OSPathToRelativePath(files.oGet(i).toString());
                if (map.Parse(file, false, false)) {
                    int count = map.GetNumEntities();
                    for (int j = 0; j < count; j++) {
                        idMapEntity ent = map.GetEntity(j);
                        if (ent != null) {

                            //Temp code to get a list of all entity key value pairs
					/*idStr static classname = ent.epairs.GetString("static classname");
                             if(static classname == "worldspawn" || static classname == "func_static" || static classname == "light" || static classname == "speaker" || static classname.Left(8) == "trigger_") {
                             continue;
                             }
                             for( int i = 0; i < ent.epairs.GetNumKeyVals(); i++) {
                             const idKeyValue* kv = ent.epairs.GetKeyVal(i);
                             idStr out = va("%s,%s,%s,%s\r\n", static classname.c_str(), kv.GetKey().c_str(), kv.GetValue().c_str(), file.c_str());
                             localizeFile.Write( out.c_str(), out.Length() );
                             }*/
                            String /*static*/ className = ent.epairs.GetString("static classname");

                            //Hack: for info_location
                            boolean hasLocation = false;

                            idStrList[] list = {null};
                            listHash.Get(/*static*/className, list);
                            if (list[0] != null) {

                                for (int k = 0; k < list[0].Num(); k++) {

                                    String val = ent.epairs.GetString(list[0].oGet(k).toString(), "");

                                    if (/*static*/className.equals("info_location") && list[0].oGet(k).equals("location")) {
                                        hasLocation = true;
                                    }

                                    if (isNotNullOrEmpty(val) && TestMapVal(val)) {

                                        if (!hasLocation || list[0].oGet(k).equals("location")) {
                                            String out = va("%s,%s,%s\r\n", val, list[0].oGet(k), file);
                                            localizeFile.WriteString(out);
                                        }
                                    }
                                }
                            }

                            listHash.Get("all", list);
                            if (list[0] != null) {
                                for (int k = 0; k < list[0].Num(); k++) {
                                    String val = ent.epairs.GetString(list[0].oGet(k).toString(), "");
                                    if (isNotNullOrEmpty(val) && TestMapVal(val)) {
                                        String out = va("%s,%s,%s\r\n", val, list[0].oGet(k), file);
                                        localizeFile.WriteString(out);
                                    }
                                }
                            }
                        }
                    }
                }
            }

            fileSystem.CloseFile(localizeFile);

            common.SetRefreshOnPrint(false);
        }
    };

    /*
     =================
     Com_StartBuild_f
     =================
     */
    static class Com_StartBuild_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new Com_StartBuild_f();

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) {
            globalImages.StartBuild();
        }
    };

    /*
     =================
     Com_FinishBuild_f
     =================
     */
    static class Com_FinishBuild_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new Com_FinishBuild_f();

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) {
            if (game != null) {
                game.CacheDictionaryMedia(null);
            }
            globalImages.FinishBuild((args.Argc() > 1));
        }
    };

    /*
     ==============
     Com_Help_f
     ==============
     */
    static class Com_Help_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new Com_Help_f();

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) throws idException {
            common.Printf("\nCommonly used commands:\n");
            common.Printf("  spawnServer      - start the server.\n");
            common.Printf("  disconnect       - shut down the server.\n");
            common.Printf("  listCmds         - list all console commands.\n");
            common.Printf("  listCVars        - list all console variables.\n");
            common.Printf("  kick             - kick a client by number.\n");
            common.Printf("  gameKick         - kick a client by name.\n");
            common.Printf("  serverNextMap    - immediately load next map.\n");
            common.Printf("  serverMapRestart - restart the current map.\n");
            common.Printf("  serverForceReady - force all players to ready status.\n");
            common.Printf("\nCommonly used variables:\n");
            common.Printf("  si_name          - server name (change requires a restart to see)\n");
            common.Printf("  si_gametype      - type of game.\n");
            common.Printf("  si_fragLimit     - max kills to win (or lives in Last Man Standing).\n");
            common.Printf("  si_timeLimit     - maximum time a game will last.\n");
            common.Printf("  si_warmup        - do pre-game warmup.\n");
            common.Printf("  si_pure          - pure server.\n");
            common.Printf("  g_mapCycle       - name of .scriptcfg file for cycling maps.\n");
            common.Printf("See mapcycle.scriptcfg for an example of a mapcyle script.\n\n");
        }
    };

    /*
     =================
     ReloadLanguage_f
     =================
     */
    static class Com_ReloadLanguage_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new Com_ReloadLanguage_f();

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) throws idException {
            commonLocal.InitLanguageDict();
        }
    };

    static void LoadMapLocalizeData(ListHash listHash) {
        throw new TODO_Exception();
//        String fileName = "map_localize.cfg";
//        Object[] buffer = {null};
//        idLexer src = new idLexer(LEXFL_NOFATALERRORS | LEXFL_NOSTRINGCONCAT | LEXFL_ALLOWMULTICHARLITERALS | LEXFL_ALLOWBACKSLASHSTRINGCONCAT);
//
//        if (fileSystem.ReadFile(fileName, buffer) > 0) {
//            src.LoadMemory(buffer, strlen(buffer), fileName);
//            if (src.IsLoaded()) {
//                String classname;
//                idToken token = new idToken();
//
//                while (src.ReadToken(token)) {
//                    classname = token.toString();
//                    src.ExpectTokenString("{");
//
//                    idStrList list = new idStrList();
//                    while (src.ReadToken(token)) {
//                        if (token.equals("}")) {
//                            break;
//                        }
//                        list.Append(token);
//                    }
//
//                    listHash.Set(classname, list);
//                }
//            }
//            fileSystem.FreeFile(buffer);
//        }
    }

    static void LoadGuiParmExcludeList(idStrList list) {
        throw new TODO_Exception();
//        String fileName = "guiparm_exclude.cfg";
//        Object[] buffer = {null};
//        idLexer src = new idLexer(LEXFL_NOFATALERRORS | LEXFL_NOSTRINGCONCAT | LEXFL_ALLOWMULTICHARLITERALS | LEXFL_ALLOWBACKSLASHSTRINGCONCAT);
//
//        if (fileSystem.ReadFile(fileName, buffer) > 0) {
//            src.LoadMemory(buffer, strlen(buffer), fileName);
//            if (src.IsLoaded()) {
////			idStr classname;
//                idToken token = new idToken();
//
//                while (src.ReadToken(token)) {
//                    list.Append(token);
//                }
//            }
//            fileSystem.FreeFile(buffer);
//        }
    }

    static boolean TestMapVal(idStr str) {
        //Already Localized?
        if (str.Find("#str_") != -1) {
            return false;
        }

        return true;
    }

    static boolean TestMapVal(String str) {
        return str.contains("#str_");
    }

    static boolean TestGuiParm(final String parm, final String value, idStrList excludeList) {

        idStr testVal = new idStr(value);

        //Already Localized?
        if (testVal.Find("#str_") != -1) {
            return false;
        }

        //Numeric
        if (testVal.IsNumeric()) {
            return false;
        }

        //Contains ::
        if (testVal.Find("::") != -1) {
            return false;
        }

        //Contains /
        if (testVal.Find("/") != -1) {
            return false;
        }

        if (excludeList.Find(testVal) != 0) {
            return false;
        }

        return true;
    }

    static boolean TestGuiParm(final idStr parm, final idStr value, idStrList excludeList) {
        return TestGuiParm(parm.toString(), value.toString(), excludeList);
    }

    static void GetFileList(final String dir, final String ext, idStrList list) {

        //Recurse Subdirectories
        idStrList dirList = new idStrList();
        Sys_ListFiles(dir, "/", dirList);
        for (int i = 0; i < dirList.Num(); i++) {
            if (dirList.oGet(i).equals(".") || dirList.oGet(i).equals("..")) {
                continue;
            }
            String fullName = va("%s/%s", dir, dirList.oGet(i));
            GetFileList(fullName, ext, list);
        }

        idStrList fileList = new idStrList();
        Sys_ListFiles(dir, ext, fileList);
        for (int i = 0; i < fileList.Num(); i++) {
            idStr fullName = new idStr(va("%s/%s", dir, fileList.oGet(i)));
            list.Append(fullName);
        }
    }

    static int LocalizeMap(final String mapName, idLangDict langDict, ListHash listHash, idStrList excludeList, boolean writeFile) {
        throw new TODO_Exception();
//	common.Printf("Localizing Map '%s'\n", mapName);
//
//	int strCount = 0;
//	
//	idMapFile map = new idMapFile();
//	if ( map.Parse(mapName, false, false ) ) {
//		int count = map.GetNumEntities();
//		for ( int j = 0; j < count; j++ ) {
//			idMapEntity ent = map.GetEntity( j );
//			if ( ent !=null) {
//
//				String className = ent.epairs.GetString("classname");
//
//				//Hack: for info_location
//				boolean hasLocation = false;
//
//				idStrList []list={null};
//				listHash.Get(className, list);
//				if(list[0]!=null) {
//
//					for(int k = 0; k < list[0].Num(); k++) {
//
//						String val = ent.epairs.GetString(list[0].oGet(k).toString(), "");
//						
//						if(val.Length() && className == "info_location" && (*list[0])[k] == "location") {
//							hasLocation = true;
//						}
//
//						if(val.Length() && TestMapVal(val)) {
//							
//							if(!hasLocation || (*list[0])[k] == "location") {
//								//Localize it!!!
//								strCount++;
//								ent.epairs.Set( (*list[0])[k], langDict.AddString( val ) );
//							}
//						}
//					}
//				}
//
//				listHash.Get("all", &list[0]);
//				if(list[0]) {
//					for(int k = 0; k < list[0].Num(); k++) {
//						idStr val = ent.epairs.GetString((*list[0])[k], "");
//						if(val.Length() && TestMapVal(val)) {
//							//Localize it!!!
//							strCount++;
//							ent.epairs.Set( (*list[0])[k], langDict.AddString( val ) );
//						}
//					}
//				}
//
//				//Localize the gui_parms
//				const idKeyValue* kv = ent.epairs.MatchPrefix("gui_parm");
//				while( kv ) {
//					if(TestGuiParm(kv.GetKey(), kv.GetValue(), excludeList)) {
//						//Localize It!
//						strCount++;
//						ent.epairs.Set( kv.GetKey(), langDict.AddString( kv.GetValue() ) );
//					}
//					kv = ent.epairs.MatchPrefix( "gui_parm", kv );
//				}
//			}
//		}
//		if(writeFile && strCount > 0)  {
//			//Before we write the map file lets make a backup of the original
//			idStr file =  fileSystem.RelativePathToOSPath(mapName);
//			idStr bak = file.Left(file.Length() - 4);
//			bak.Append(".bak_loc");
//			fileSystem.CopyFile( file, bak );
//			
//			map.Write( mapName, ".map" );
//		}
//	}
//
//	common.Printf("Count: %d\n", strCount);
//	return strCount;
    }

    public static void setCommon(idCommon common) {
        Common.common = Common.commonLocal = (idCommonLocal) common;
    }
}
