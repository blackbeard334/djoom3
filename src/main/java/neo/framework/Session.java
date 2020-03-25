package neo.framework;

import static neo.Game.Game_local.game;
import static neo.Renderer.ModelManager.renderModelManager;
import static neo.Sound.snd_system.soundSystem;
import static neo.TempDump.SERIAL_SIZE;
import static neo.TempDump.isNotNullOrEmpty;
import static neo.framework.CVarSystem.CVAR_SERVERINFO;
import static neo.framework.CVarSystem.cvarSystem;
import static neo.framework.CmdSystem.cmdSystem;
import static neo.framework.CmdSystem.cmdExecution_t.CMD_EXEC_NOW;
import static neo.framework.Common.common;
import static neo.framework.Console.console;
import static neo.framework.DeclManager.declManager;
import static neo.framework.FileSystem_h.fileSystem;
import static neo.framework.Session_local.timeDemo_t.TD_YES;
import static neo.framework.Session_local.timeDemo_t.TD_YES_THEN_QUIT;
import static neo.idlib.Text.Str.va;
import static neo.sys.win_main.Sys_EnterCriticalSection;
import static neo.sys.win_main.Sys_LeaveCriticalSection;
import static neo.sys.win_main.Sys_Sleep;
import static neo.ui.UserInterface.uiManager;

import java.nio.ByteBuffer;

import neo.TempDump.SERiAL;
import neo.TempDump.TODO_Exception;
import neo.Renderer.RenderWorld.idRenderWorld;
import neo.Sound.sound.idSoundWorld;
import neo.framework.CmdSystem.cmdFunction_t;
import neo.framework.DemoFile.idDemoFile;
import neo.framework.FileSystem_h.backgroundDownload_s;
import neo.framework.FileSystem_h.findFile_t;
import neo.framework.File_h.idFile;
import neo.framework.Session_local.idSessionLocal;
import neo.framework.Async.AsyncNetwork.idAsyncNetwork;
import neo.idlib.CmdArgs.idCmdArgs;
import neo.idlib.Lib.idException;
import neo.idlib.Text.Str.idStr;
import neo.sys.sys_public.sysEvent_s;
import neo.ui.UserInterface.idUserInterface;

/**
 *
 */
public class Session {

    public static final idSessionLocal sessLocal = new idSessionLocal();
    public static final idSession session = sessLocal;
    /*
     ===============================================================================

     The session is the glue that holds games together between levels.

     ===============================================================================
     */

    // needed by the gui system for the load game menu
    public static class logStats_t implements SERiAL {

        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public static final transient int SIZE = SERIAL_SIZE(new logStats_t());

        public int health;
        public int heartRate;
        public int stamina;
        public int combat;

        @Override
        public ByteBuffer AllocBuffer() {
            throw new TODO_Exception();
        }

        @Override
        public void Read(ByteBuffer buffer) {
            throw new TODO_Exception();
        }

        @Override
        public ByteBuffer Write() {
            throw new TODO_Exception();
        }
    }
    public static final int MAX_LOGGED_STATS = 60 * 120;		// log every half second 

    public enum msgBoxType_t {

        MSG_OK,
        MSG_ABORT,
        MSG_OKCANCEL,
        MSG_YESNO,
        MSG_PROMPT,
        MSG_CDKEY,
        MSG_INFO,
        MSG_WAIT
    }

//typedef const char * (*HandleGuiCommand_t)( const char * );
    public static abstract class HandleGuiCommand_t {

        public abstract String run(final String input);
    }

    public static abstract class idSession {

        // The render world and sound world used for this session.
        public idRenderWorld rw;
        public idSoundWorld sw;
        // The renderer and sound system will write changes to writeDemo.
        // Demos can be recorded and played at the same time when splicing.
        public idDemoFile readDemo;
        public idDemoFile writeDemo;
        public int renderdemoVersion;

//	public abstract			~idSession() {}
        // Called in an orderly fashion at system startup,
        // so commands, cvars, files, etc are all available.
        public abstract void Init() throws idException;

        // Shut down the session.
        public abstract void Shutdown();

        // Called on errors and game exits.
        public abstract void Stop();

        // Redraws the screen, handling games, guis, console, etc
        // during normal once-a-frame updates, outOfSequence will be false,
        // but when the screen is updated in a modal manner, as with utility
        // output, the mouse cursor will be released if running windowed.
        public abstract void UpdateScreen( /*boolean outOfSequence = true*/);

        public abstract void UpdateScreen(boolean outOfSequence);

        // Called when console prints happen, allowing the loading screen
        // to redraw if enough time has passed.
        public abstract void PacifierUpdate();

        // Called every frame, possibly spinning in place if we are
        // above maxFps, or we haven't advanced at least one demo frame.
        // Returns the number of milliseconds since the last frame.
        public abstract void Frame() throws idException;

        // Returns true if a multiplayer game is running.
        // CVars and commands are checked differently in multiplayer mode.
        public abstract boolean IsMultiplayer();

        // Processes the given event.
        public abstract boolean ProcessEvent(final sysEvent_s event) throws idException;

        // Activates the main menu
        public abstract void StartMenu(boolean playIntro) throws idException;

        public void StartMenu( /*boolean playIntro = false*/) throws idException {
            StartMenu(false);
        }

        public abstract void SetGUI(idUserInterface gui, HandleGuiCommand_t handle);

        // Updates gui and dispatched events to it
        public abstract void GuiFrameEvents();

        // fires up the optional GUI event, also returns them if you set wait to true
        // if MSG_PROMPT and wait, returns the prompt string or NULL if aborted
        // if MSG_CDKEY and want, returns the cd key or NULL if aborted
        // network tells wether one should still run the network loop in a wait dialog
        public abstract String MessageBox(msgBoxType_t type, final String message/*, final String title = NULL, boolean wait = false, final String fire_yes = NULL, final String fire_no = NULL, boolean network = false*/);

        public abstract String MessageBox(msgBoxType_t type, final String message, final String title /*, boolean wait = false, final String fire_yes = NULL, final String fire_no = NULL, boolean network = false*/);

        public abstract String MessageBox(msgBoxType_t type, final String message, final String title, boolean wait /*, final String fire_yes = NULL, final String fire_no = NULL, boolean network = false*/);

        public abstract String MessageBox(msgBoxType_t type, final String message, final String title, boolean wait, final String fire_yes /*, final String fire_no = NULL, boolean network = false*/);

        public abstract String MessageBox(msgBoxType_t type, final String message, final String title, boolean wait, final String fire_yes, final String fire_no /*, boolean network = false*/);

        public abstract String MessageBox(msgBoxType_t type, final String message, final String title, boolean wait, final String fire_yes, final String fire_no, boolean network);

        public abstract void StopBox();
        // monitor this download in a progress box to either abort or completion

        public abstract void DownloadProgressBox(backgroundDownload_s bgl, final String title/*, int progress_start = 0, int progress_end = 100*/);

        public abstract void DownloadProgressBox(backgroundDownload_s bgl, final String title, int progress_start /*= 0, int progress_end = 100*/);

        public abstract void DownloadProgressBox(backgroundDownload_s bgl, final String title, int progress_start, int progress_end);

        public abstract void SetPlayingSoundWorld();

        // this is used by the sound system when an OnDemand sound is loaded, so the game action
        // doesn't advance and get things out of sync
        public abstract void TimeHitch(int msec);

        // read and write the cd key data to files
        // doesn't perform any validity checks
        public abstract void ReadCDKey();

        public abstract void WriteCDKey();

        // returns NULL for if xp is true and xp key is not valid or not present
        public abstract String GetCDKey(boolean xp);//TODO:string pointer?

        // check keys for validity when typed in by the user ( with checksum verification )
        // store the new set of keys if they are found valid
        public abstract boolean CheckKey(final String key, boolean netConnect, boolean[] offline_valid/*[ 2 ]*/);

        // verify the current set of keys for validity
        // strict -> keys in state CDKEY_CHECKING state are not ok
        public abstract boolean CDKeysAreValid(boolean strict);
        // wipe the key on file if the network check finds it invalid

        public abstract void ClearCDKey(boolean[] valid/*[ 2 ]*/);

        // configure gui variables for mainmenu.gui and cd key state
        public abstract void SetCDKeyGuiVars();

        public abstract boolean WaitingForGameAuth();

        // got reply from master about the keys. if !valid, auth_msg given
        public abstract void CDKeysAuthReply(boolean valid, final String auth_msg);

        public abstract String GetCurrentMapName();

        public abstract int GetSaveGameVersion();
    }
//    
//    
//    
////    idCVar	idSessionLocal::com_showAngles( "com_showAngles", "0", CVAR_SYSTEM | CVAR_BOOL, "" );
//idCVar	idSessionLocal::com_minTics( "com_minTics", "1", CVAR_SYSTEM, "" );
//idCVar	idSessionLocal::com_showTics( "com_showTics", "0", CVAR_SYSTEM | CVAR_BOOL, "" );
//idCVar	idSessionLocal::com_fixedTic( "com_fixedTic", "0", CVAR_SYSTEM | CVAR_INTEGER, "", 0, 10 );
//idCVar	idSessionLocal::com_showDemo( "com_showDemo", "0", CVAR_SYSTEM | CVAR_BOOL, "" );
//idCVar	idSessionLocal::com_skipGameDraw( "com_skipGameDraw", "0", CVAR_SYSTEM | CVAR_BOOL, "" );
//idCVar	idSessionLocal::com_aviDemoSamples( "com_aviDemoSamples", "16", CVAR_SYSTEM, "" );
//idCVar	idSessionLocal::com_aviDemoWidth( "com_aviDemoWidth", "256", CVAR_SYSTEM, "" );
//idCVar	idSessionLocal::com_aviDemoHeight( "com_aviDemoHeight", "256", CVAR_SYSTEM, "" );
//idCVar	idSessionLocal::com_aviDemoTics( "com_aviDemoTics", "2", CVAR_SYSTEM | CVAR_INTEGER, "", 1, 60 );
//idCVar	idSessionLocal::com_wipeSeconds( "com_wipeSeconds", "1", CVAR_SYSTEM, "" );
//idCVar	idSessionLocal::com_guid( "com_guid", "", CVAR_SYSTEM | CVAR_ARCHIVE | CVAR_ROM, "" );
//
//
// these must be kept up to date with window Levelshot in guis/mainmenu.gui
    static int PREVIEW_X = 211;
    static int PREVIEW_Y = 31;
    static int PREVIEW_WIDTH = 398;
    static int PREVIEW_HEIGHT = 298;
//

    void RandomizeStack() {
        // attempt to force uninitialized stack memory bugs
        final int bytes = 4000000;
        final byte[] buf = new byte[bytes];

        final byte fill = (byte) ((int) (Math.random()) & 255);
        for (int i = 0; i < bytes; i++) {
            buf[i] = fill;
        }
    }
//
//    
//    
//    

    /*
     =================
     Session_RescanSI_f
     =================
     */
    static class Session_RescanSI_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new Session_RescanSI_f();

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) throws idException {
            sessLocal.mapSpawnData.serverInfo = cvarSystem.MoveCVarsToDict(CVAR_SERVERINFO);
            if ((game != null) && idAsyncNetwork.server.IsActive()) {
                game.SetServerInfo(sessLocal.mapSpawnData.serverInfo);
            }
        }
    }

    /*
     ==================
     Session_Map_f

     Restart the server on a different map
     ==================
     */
    static class Session_Map_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new Session_Map_f();

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) throws idException {
            idStr map;
            String string;
            findFile_t ff;
            final idCmdArgs rl_args = new idCmdArgs();

            map = new idStr(args.Argv(1));
            if (0 == map.Length()) {
                return;
            }
            map.StripFileExtension();

            // make sure the level exists before trying to change, so that
            // a typo at the server console won't end the game
            // handle addon packs through reloadEngine
            string = String.format("maps/%s.map", map.toString());
            ff = fileSystem.FindFile(string, true);
            switch (ff) {
                case FIND_NO:
                    common.Printf("Can't find map %s\n", string);
                    return;
                case FIND_ADDON:
                    common.Printf("map %s is in an addon pak - reloading\n", string);
                    rl_args.AppendArg("map");
                    rl_args.AppendArg(map.toString());
                    cmdSystem.SetupReloadEngine(rl_args);
                    return;
                default:
                    break;
            }

            cvarSystem.SetCVarBool("developer", false);
            sessLocal.StartNewGame(map.toString(), true);
        }
    }

    /*
     ==================
     Session_DevMap_f

     Restart the server on a different map in developer mode
     ==================
     */
    static class Session_DevMap_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new Session_DevMap_f();

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) throws idException {
            idStr map;
            String string;
            findFile_t ff;
            final idCmdArgs rl_args = new idCmdArgs();

            map = new idStr(args.Argv(1));
            if (0 == map.Length()) {
                return;
            }
            map.StripFileExtension();

            // make sure the level exists before trying to change, so that
            // a typo at the server console won't end the game
            // handle addon packs through reloadEngine
            string = String.format("maps/%s.map", map.toString());
            ff = fileSystem.FindFile(string, true);
            switch (ff) {
                case FIND_NO:
                    common.Printf("Can't find map %s\n", string);
                    return;
                case FIND_ADDON:
                    common.Printf("map %s is in an addon pak - reloading\n", string);
                    rl_args.AppendArg("devmap");
                    rl_args.AppendArg(map.toString());
                    cmdSystem.SetupReloadEngine(rl_args);
                    return;
                default:
                    break;
            }

            cvarSystem.SetCVarBool("developer", true);
            sessLocal.StartNewGame(map.toString(), true);
        }
    }

    /*
     ==================
     Session_TestMap_f
     ==================
     */
    static class Session_TestMap_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new Session_TestMap_f();

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) throws idException {
            idStr map;
            String string;

            map = new idStr(args.Argv(1));
            if (0 == map.Length()) {
                return;
            }
            map.StripFileExtension();

            cmdSystem.BufferCommandText(CMD_EXEC_NOW, "disconnect");

            string = String.format("dmap maps/%s.map", map.toString());
            cmdSystem.BufferCommandText(CMD_EXEC_NOW, string);

            string = String.format("devmap %s", map);//TODO:can this shit format char*?
            cmdSystem.BufferCommandText(CMD_EXEC_NOW, string);
        }
    }

    /*
     ==================
     Sess_WritePrecache_f
     ==================
     */
    static class Sess_WritePrecache_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new Sess_WritePrecache_f();

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) throws idException {
            if (args.Argc() != 2) {
                common.Printf("USAGE: writePrecache <execFile>\n");
                return;
            }
            final idStr str = new idStr(args.Argv(1));
            str.DefaultFileExtension(".cfg");
            final idFile f = fileSystem.OpenFileWrite(str.toString());
            declManager.WritePrecacheCommands(f);
            renderModelManager.WritePrecacheCommands(f);
            uiManager.WritePrecacheCommands(f);

            fileSystem.CloseFile(f);
        }
    }
    /*
     ===================
     Session_PromptKey_f
     ===================
     */ static boolean recursed = false;

    static class Session_PromptKey_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new Session_PromptKey_f();

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) throws idException {
            final String retkey;
            final boolean[] valid = new boolean[2];

            if (recursed) {
                common.Warning("promptKey recursed - aborted");
                return;
            }
            recursed = true;
            //HACKME::5:disable the serial messageBox
//            do {
//                // in case we're already waiting for an auth to come back to us ( may happen exceptionally )
//                if (sessLocal.MaybeWaitOnCDKey()) {
//                    if (sessLocal.CDKeysAreValid(true)) {
//                        recursed = false;
//                        return;
//                    }
//                }
//                // the auth server may have replied and set an error message, otherwise use a default
//                String prompt_msg = sessLocal.GetAuthMsg();
//                if (prompt_msg.isEmpty()/*[ 0 ] == '\0'*/) {
//                    prompt_msg = common.GetLanguageDict().GetString("#str_04308");
//                }
////                for (int d = 0; d < common.GetLanguageDict().args.Size(); d++) {
////                    LangDict.idLangKeyValue bla = common.GetLanguageDict().args.oGet(d);
////                    System.out.println(bla.key + " >>> " + bla.value);
////                }
//                retkey = sessLocal.MessageBox(MSG_CDKEY, prompt_msg, common.GetLanguageDict().GetString("#str_04305"), true, null, null, true);
//                if (retkey != null) {
//                    if (sessLocal.CheckKey(retkey, false, valid)) {
//                        // if all went right, then we may have sent an auth request to the master ( unless the prompt is used during a net connect )
//                        boolean canExit = true;
//                        if (sessLocal.MaybeWaitOnCDKey()) {
//                            // wait on auth reply, and got denied, prompt again
//                            if (!sessLocal.CDKeysAreValid(true)) {
//                                // server says key is invalid - MaybeWaitOnCDKey was interrupted by a CDKeysAuthReply call, which has set the right error message
//                                // the invalid keys have also been cleared in the process
//                                sessLocal.MessageBox(MSG_OK, sessLocal.GetAuthMsg(), common.GetLanguageDict().GetString("#str_04310"), true, null, null, true);
//                                canExit = false;
//                            }
//                        }
//                        if (canExit) {
//                            // make sure that's saved on file
//                            sessLocal.WriteCDKey();
//                            sessLocal.MessageBox(MSG_OK, common.GetLanguageDict().GetString("#str_04307"), common.GetLanguageDict().GetString("#str_04305"), true, null, null, true);
//                            break;
//                        }
//                    } else {
//                        // offline check sees key invalid
//                        // build a message about keys being wrong. do not attempt to change the current key state though
//                        // ( the keys may be valid, but user would have clicked on the dialog anyway, that kind of thing )
//                        idStr msg = new idStr();
//                        idAsyncNetwork.BuildInvalidKeyMsg(msg, valid);
//                        sessLocal.MessageBox(MSG_OK, msg.toString(), common.GetLanguageDict().GetString("#str_04310"), true, null, null, true);
//                    }
//                } else if (args.Argc() == 2 && idStr.Icmp(args.Argv(1), "force") == 0) {
//                    // cancelled in force mode
//                    cmdSystem.BufferCommandText(CMD_EXEC_APPEND, "quit\n");
//                    cmdSystem.ExecuteCommandBuffer();
//                }
//            } while (retkey != null);
            recursed = false;
        }
    }

    /*
     ================
     Session_DemoShot_f
     ================
     */
    static class Session_DemoShot_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new Session_DemoShot_f();

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) {
            if (args.Argc() != 2) {
                final String filename = FindUnusedFileName("demos/shot%03i.demo");
                sessLocal.DemoShot(filename);
            } else {
                sessLocal.DemoShot(va("demos/shot_%s.demo", args.Argv(1)));
            }
        }
    }

    /*
     ================
     Session_RecordDemo_f
     ================
     */
    static class Session_RecordDemo_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new Session_RecordDemo_f();

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) {
            if (args.Argc() != 2) {
                final String filename = FindUnusedFileName("demos/demo%03i.demo");
                sessLocal.StartRecordingRenderDemo(filename);
            } else {
                sessLocal.StartRecordingRenderDemo(va("demos/%s.demo", args.Argv(1)));
            }
        }
    }

    /*
     ================
     Session_CompressDemo_f
     ================
     */
    static class Session_CompressDemo_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new Session_CompressDemo_f();

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) throws idException {
            if (args.Argc() == 2) {
                sessLocal.CompressDemoFile("2", args.Argv(1));
            } else if (args.Argc() == 3) {
                sessLocal.CompressDemoFile(args.Argv(2), args.Argv(1));
            } else {
                common.Printf("use: CompressDemo <file> [scheme]\nscheme is the same as com_compressDemo, defaults to 2");
            }
        }
    }

    /*
     ================
     Session_StopRecordingDemo_f
     ================
     */
    static class Session_StopRecordingDemo_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new Session_StopRecordingDemo_f();

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) {
            sessLocal.StopRecordingRenderDemo();
        }
    }

    /*
     ================
     Session_PlayDemo_f
     ================
     */
    static class Session_PlayDemo_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new Session_PlayDemo_f();

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) {
            if (args.Argc() >= 2) {
                sessLocal.StartPlayingRenderDemo(va("demos/%s", args.Argv(1)));
            }
        }
    }

    /*
     ================
     Session_TimeDemo_f
     ================
     */
    static class Session_TimeDemo_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new Session_TimeDemo_f();

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) {
            if (args.Argc() >= 2) {
                sessLocal.TimeRenderDemo(va("demos/%s", args.Argv(1)), (args.Argc() > 2));
            }
        }
    }

    /*
     ================
     Session_TimeDemoQuit_f
     ================
     */
    static class Session_TimeDemoQuit_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new Session_TimeDemoQuit_f();

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) {
            sessLocal.TimeRenderDemo(va("demos/%s", args.Argv(1)));
            if (sessLocal.timeDemo == TD_YES) {
                // this allows hardware vendors to automate some testing
                sessLocal.timeDemo = TD_YES_THEN_QUIT;
            }
        }
    }

    /*
     ================
     Session_AVIDemo_f
     ================
     */
    static class Session_AVIDemo_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new Session_AVIDemo_f();

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) {
            sessLocal.AVIRenderDemo(va("demos/%s", args.Argv(1)));
        }
    }

    /*
     ================
     Session_AVIGame_f
     ================
     */
    static class Session_AVIGame_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new Session_AVIGame_f();

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) {
            final String[] Argv = {args.Argv(1)};
            final boolean empty = !isNotNullOrEmpty(Argv[0]);
            sessLocal.AVIGame(Argv);//TODO:back reference

            if (empty) {
                args.oSet(Argv[0]);
            }
        }
    }

    /*
     ================
     Session_AVICmdDemo_f
     ================
     */
    static class Session_AVICmdDemo_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new Session_AVICmdDemo_f();

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) {
            sessLocal.AVICmdDemo(args.Argv(1));
        }
    }

    /*
     ================
     Session_WriteCmdDemo_f
     ================
     */
    static class Session_WriteCmdDemo_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new Session_WriteCmdDemo_f();

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) throws idException {
            if (args.Argc() == 1) {
                final String filename = FindUnusedFileName("demos/cmdDemo%03i.cdemo");
                sessLocal.WriteCmdDemo(filename);
            } else if (args.Argc() == 2) {
                sessLocal.WriteCmdDemo(va("demos/%s.cdemo", args.Argv(1)));
            } else {
                common.Printf("usage: writeCmdDemo [demoName]\n");
            }
        }
    }

    /*
     ================
     Session_PlayCmdDemo_f
     ================
     */
    static class Session_PlayCmdDemo_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new Session_PlayCmdDemo_f();

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) {
            sessLocal.StartPlayingCmdDemo(args.Argv(1));
        }
    }

    /*
     ================
     Session_TimeCmdDemo_f
     ================
     */
    static class Session_TimeCmdDemo_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new Session_TimeCmdDemo_f();

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) {
            sessLocal.TimeCmdDemo(args.Argv(1));
        }
    }

    /*
     ================
     Session_Disconnect_f
     ================
     */
    static class Session_Disconnect_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new Session_Disconnect_f();

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) {
            sessLocal.Stop();
            sessLocal.StartMenu();
            if (soundSystem != null) {
                soundSystem.SetMute(false);
            }
        }
    }

    /*
     ================
     Session_EndOfDemo_f
     ================
     */
    static class Session_EndOfDemo_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new Session_EndOfDemo_f();

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) {
            sessLocal.Stop();
            sessLocal.StartMenu();
            if (soundSystem != null) {
                soundSystem.SetMute(false);
            }
            if (sessLocal.guiActive != null) {
                sessLocal.guiActive.HandleNamedEvent("endOfDemo");
            }
        }
    }

    /*
     ================
     Session_ExitCmdDemo_f
     ================
     */
    static class Session_ExitCmdDemo_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new Session_ExitCmdDemo_f();

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) throws idException {
            if (null == sessLocal.cmdDemoFile) {
                common.Printf("not reading from a cmdDemo\n");
                return;
            }
            fileSystem.CloseFile(sessLocal.cmdDemoFile);
            common.Printf("Command demo exited at logIndex %d\n", sessLocal.logIndex);
            sessLocal.cmdDemoFile = null;
        }
    }

    /*
     ================
     Session_TestGUI_f
     ================
     */
    static class Session_TestGUI_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new Session_TestGUI_f();

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) {
            sessLocal.TestGUI(args.Argv(1));
        }
    }

    /*
     ===============
     LoadGame_f
     ===============
     */
    static class LoadGame_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new LoadGame_f();

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) throws idException {
            console.Close();
            if ((args.Argc() < 2) || (idStr.Icmp(args.Argv(1), "quick") == 0)) {
                final String saveName = common.GetLanguageDict().GetString("#str_07178");
                sessLocal.LoadGame(saveName);
            } else {
                sessLocal.LoadGame(args.Argv(1));
            }
        }
    }

    /*
     ===============
     SaveGame_f
     ===============
     */
    static class SaveGame_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new SaveGame_f();

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) throws idException {
            if ((args.Argc() < 2) || (idStr.Icmp(args.Argv(1), "quick") == 0)) {
                final String saveName = common.GetLanguageDict().GetString("#str_07178");
                if (sessLocal.SaveGame(saveName)) {
                    common.Printf("%s\n", saveName);
                }
            } else {
                if (sessLocal.SaveGame(args.Argv(1))) {
                    common.Printf("Saved %s\n", args.Argv(1));
                }
            }
        }
    }

    /*
     ===============
     TakeViewNotes_f
     ===============
     */
    static class TakeViewNotes_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new TakeViewNotes_f();

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) {
            final String p = (args.Argc() > 1) ? args.Argv(1) : "";
            sessLocal.TakeNotes(p);
        }
    }

    /*
     ===============
     TakeViewNotes2_f
     ===============
     */
    static class TakeViewNotes2_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new TakeViewNotes2_f();

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) {
            final String p = (args.Argc() > 1) ? args.Argv(1) : "";
            sessLocal.TakeNotes(p, true);
        }
    }

    /*
     ===============
     Session_Hitch_f
     ===============
     */
    static class Session_Hitch_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new Session_Hitch_f();

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) {
            final idSoundWorld sw = soundSystem.GetPlayingSoundWorld();
            if (sw != null) {
                soundSystem.SetMute(true);
                sw.Pause();
                Sys_EnterCriticalSection();
            }
            if (args.Argc() == 2) {
                Sys_Sleep(Integer.parseInt(args.Argv(1)));
            } else {
                Sys_Sleep(100);
            }
            if (sw != null) {
                Sys_LeaveCriticalSection();
                sw.UnPause();
                soundSystem.SetMute(false);
            }
        }
    }

//
    //////////////////////////////////////////////////////////////////////////////////////////////////
//

    /*
     ================
     FindUnusedFileName
     ================
     */
    static String FindUnusedFileName(final String format) {
        int i;
        String filename = "";//=new char[1024];

        for (i = 0; i < 999; i++) {
            filename = String.format(format, i);
            final int len = fileSystem.ReadFile(filename, null, null);
            if (len <= 0) {
                return filename;	// file doesn't exist
            }
        }

        return filename;
    }
}
