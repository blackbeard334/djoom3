package neo.framework;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.stream.Stream;

import neo.Game.Game.escReply_t;
import static neo.Game.Game.escReply_t.ESC_GUI;
import static neo.Game.Game.escReply_t.ESC_IGNORE;
import neo.Game.Game.gameReturn_t;
import static neo.Game.Game_local.game;
import neo.Renderer.Material.idMaterial;
import static neo.Renderer.RenderSystem.SCREEN_HEIGHT;
import static neo.Renderer.RenderSystem.SCREEN_WIDTH;
import static neo.Renderer.RenderSystem.renderSystem;
import static neo.Renderer.RenderSystem_init.R_ScreenshotFilename;
import neo.Renderer.RenderWorld.renderView_s;
import static neo.Sound.snd_system.soundSystem;
import neo.Sound.sound.idSoundWorld;

import static neo.TempDump.*;

import static neo.framework.Async.AsyncNetwork.MAX_ASYNC_CLIENTS;
import neo.framework.Async.AsyncNetwork.idAsyncNetwork;
import static neo.framework.Async.ServerScan.serverSort_t.SORT_GAME;
import static neo.framework.Async.ServerScan.serverSort_t.SORT_GAMETYPE;
import static neo.framework.Async.ServerScan.serverSort_t.SORT_MAP;
import static neo.framework.Async.ServerScan.serverSort_t.SORT_PING;
import static neo.framework.Async.ServerScan.serverSort_t.SORT_PLAYERS;
import static neo.framework.Async.ServerScan.serverSort_t.SORT_SERVERNAME;
import static neo.framework.BuildDefines.ID_CONSOLE_LOCK;
import static neo.framework.BuildDefines.ID_DEDICATED;
import static neo.framework.BuildDefines.ID_DEMO_BUILD;
import static neo.framework.BuildDefines.ID_ENFORCE_KEY;
import static neo.framework.BuildDefines._WIN32;
import static neo.framework.BuildDefines.__linux__;
import static neo.framework.CVarSystem.CVAR_ARCHIVE;
import static neo.framework.CVarSystem.CVAR_BOOL;
import static neo.framework.CVarSystem.CVAR_GUI;
import static neo.framework.CVarSystem.CVAR_INTEGER;
import static neo.framework.CVarSystem.CVAR_NETWORKSYNC;
import static neo.framework.CVarSystem.CVAR_ROM;
import static neo.framework.CVarSystem.CVAR_SERVERINFO;
import static neo.framework.CVarSystem.CVAR_SYSTEM;
import static neo.framework.CVarSystem.CVAR_USERINFO;
import static neo.framework.CVarSystem.cvarSystem;
import neo.framework.CVarSystem.idCVar;
import static neo.framework.CmdSystem.CMD_FL_CHEAT;
import static neo.framework.CmdSystem.CMD_FL_SYSTEM;
import static neo.framework.CmdSystem.cmdExecution_t.CMD_EXEC_APPEND;
import static neo.framework.CmdSystem.cmdExecution_t.CMD_EXEC_INSERT;
import static neo.framework.CmdSystem.cmdExecution_t.CMD_EXEC_NOW;
import static neo.framework.CmdSystem.cmdSystem;
import neo.framework.CmdSystem.idCmdSystem;
import static neo.framework.Common.EDITOR_GUI;
import static neo.framework.Common.EDITOR_RADIANT;
import static neo.framework.Common.com_allowConsole;
import static neo.framework.Common.com_asyncInput;
import static neo.framework.Common.com_asyncSound;
import static neo.framework.Common.com_editorActive;
import static neo.framework.Common.com_editors;
import static neo.framework.Common.com_frameTime;
import static neo.framework.Common.com_machineSpec;
import static neo.framework.Common.com_speeds;
import static neo.framework.Common.com_ticNumber;
import static neo.framework.Common.com_updateLoadSize;
import static neo.framework.Common.common;
import static neo.framework.Common.time_gameDraw;
import static neo.framework.Common.time_gameFrame;
import static neo.framework.Console.console;
import neo.framework.DeclEntityDef.idDeclEntityDef;
import static neo.framework.DeclManager.declManager;
import static neo.framework.DeclManager.declType_t.DECL_ENTITYDEF;
import static neo.framework.DeclManager.declType_t.DECL_MAPDEF;
import neo.framework.DeclManager.idDecl;
import static neo.framework.DemoFile.demoSystem_t.DS_FINISHED;
import static neo.framework.DemoFile.demoSystem_t.DS_RENDER;
import static neo.framework.DemoFile.demoSystem_t.DS_SOUND;
import static neo.framework.DemoFile.demoSystem_t.DS_VERSION;
import neo.framework.DemoFile.idDemoFile;
import static neo.framework.EventLoop.eventLoop;
import neo.framework.FileSystem_h.backgroundDownload_s;
import static neo.framework.FileSystem_h.dlStatus_t.DL_ABORTING;
import static neo.framework.FileSystem_h.fileSystem;
import neo.framework.FileSystem_h.idFileList;
import neo.framework.FileSystem_h.idModList;
import neo.framework.File_h.idFile;

import static neo.framework.KeyInput.K_ESCAPE;
import static neo.framework.KeyInput.K_F1;
import static neo.framework.KeyInput.K_F12;
import neo.framework.KeyInput.idKeyInput;
import static neo.framework.Licensee.BASE_GAMEDIR;
import static neo.framework.Licensee.CDKEY_FILE;
import static neo.framework.Licensee.CDKEY_TEXT;
import static neo.framework.Licensee.GAME_NAME;
import static neo.framework.Licensee.RENDERDEMO_VERSION;
import static neo.framework.Licensee.SAVEGAME_VERSION;
import static neo.framework.Licensee.XPKEY_FILE;
import static neo.framework.Session.FindUnusedFileName;
import neo.framework.Session.HandleGuiCommand_t;
import neo.framework.Session.LoadGame_f;
import static neo.framework.Session.MAX_LOGGED_STATS;
import neo.framework.Session.SaveGame_f;
import neo.framework.Session.Sess_WritePrecache_f;
import neo.framework.Session.Session_AVICmdDemo_f;
import neo.framework.Session.Session_AVIDemo_f;
import neo.framework.Session.Session_AVIGame_f;
import neo.framework.Session.Session_CompressDemo_f;
import neo.framework.Session.Session_DemoShot_f;
import neo.framework.Session.Session_DevMap_f;
import neo.framework.Session.Session_Disconnect_f;
import neo.framework.Session.Session_EndOfDemo_f;
import neo.framework.Session.Session_ExitCmdDemo_f;
import neo.framework.Session.Session_Hitch_f;
import neo.framework.Session.Session_Map_f;
import neo.framework.Session.Session_PlayCmdDemo_f;
import neo.framework.Session.Session_PlayDemo_f;
import neo.framework.Session.Session_PromptKey_f;
import neo.framework.Session.Session_RecordDemo_f;
import neo.framework.Session.Session_RescanSI_f;
import neo.framework.Session.Session_StopRecordingDemo_f;
import neo.framework.Session.Session_TestGUI_f;
import neo.framework.Session.Session_TestMap_f;
import neo.framework.Session.Session_TimeCmdDemo_f;
import neo.framework.Session.Session_TimeDemoQuit_f;
import neo.framework.Session.Session_TimeDemo_f;
import neo.framework.Session.Session_WriteCmdDemo_f;
import neo.framework.Session.TakeViewNotes2_f;
import neo.framework.Session.TakeViewNotes_f;
import neo.framework.Session.idSession;
import neo.framework.Session.logStats_t;
import neo.framework.Session.msgBoxType_t;
import static neo.framework.Session.msgBoxType_t.MSG_CDKEY;
import static neo.framework.Session.msgBoxType_t.MSG_OK;
import static neo.framework.Session.msgBoxType_t.MSG_OKCANCEL;
import static neo.framework.Session.msgBoxType_t.MSG_PROMPT;
import static neo.framework.Session.msgBoxType_t.MSG_WAIT;
import static neo.framework.Session.msgBoxType_t.MSG_YESNO;
import static neo.framework.Session.sessLocal;
import static neo.framework.Session.session;
import static neo.framework.Session_local.idSessionLocal.cdKeyState_t.CDKEY_CHECKING;
import static neo.framework.Session_local.idSessionLocal.cdKeyState_t.CDKEY_INVALID;
import static neo.framework.Session_local.idSessionLocal.cdKeyState_t.CDKEY_NA;
import static neo.framework.Session_local.idSessionLocal.cdKeyState_t.CDKEY_OK;
import static neo.framework.Session_local.idSessionLocal.cdKeyState_t.CDKEY_UNKNOWN;
import static neo.framework.Session_local.timeDemo_t.TD_NO;
import static neo.framework.Session_local.timeDemo_t.TD_YES;
import neo.framework.Session_menu.idListSaveGameCompare;
import static neo.framework.UsercmdGen.MAX_BUFFERED_USERCMD;
import static neo.framework.UsercmdGen.USERCMD_MSEC;
import static neo.framework.UsercmdGen.inhibit_t.INHIBIT_SESSION;
import static neo.framework.UsercmdGen.usercmdGen;
import neo.framework.UsercmdGen.usercmd_t;
import neo.idlib.CmdArgs.idCmdArgs;
import neo.idlib.Dict_h.idDict;
import neo.idlib.Dict_h.idKeyValue;
import static neo.idlib.Lib.LittleLong;
import static neo.idlib.Lib.MAX_STRING_CHARS;
import static neo.idlib.Lib.Max;
import static neo.idlib.Lib.Min;
import static neo.idlib.Lib.colorBlack;
import neo.idlib.Lib.idException;
import static neo.idlib.Text.Lexer.LEXFL_NOERRORS;
import static neo.idlib.Text.Lexer.LEXFL_NOSTRINGCONCAT;
import neo.idlib.Text.Lexer.idLexer;
import static neo.idlib.Text.Str.Measure_t.MEASURE_BANDWIDTH;
import static neo.idlib.Text.Str.Measure_t.MEASURE_SIZE;
import neo.idlib.Text.Str.idStr;
import static neo.idlib.Text.Str.va;
import neo.idlib.Text.Token.idToken;
import neo.idlib.containers.List.idList;
import neo.idlib.containers.StrList.idStrList;
import static neo.idlib.hashing.CRC32.CRC32_BlockChecksum;
import static neo.sys.sys_local.Sys_TimeStampToStr;
import static neo.sys.sys_public.TRIGGER_EVENT_ONE;
import static neo.sys.sys_public.sysEventType_t.SE_KEY;
import static neo.sys.sys_public.sysEventType_t.SE_NONE;
import neo.sys.sys_public.sysEvent_s;
import static neo.sys.win_input.Sys_GrabMouseCursor;
import neo.sys.win_main;
import static neo.sys.win_main.Sys_ClearEvents;
import static neo.sys.win_main.Sys_GenerateEvents;
import static neo.sys.win_main.Sys_IsWindowVisible;
import static neo.sys.win_main.Sys_Sleep;
import static neo.sys.win_main.Sys_WaitForEvent;
import static neo.sys.win_shared.Sys_Milliseconds;

import neo.ui.ListGUI.idListGUI;
import neo.ui.UserInterface.idUserInterface;
import static neo.ui.UserInterface.uiManager;

/**
 *
 */
public class Session_local {

    static class logCmd_t implements SERiAL {

        private static final transient int BYTES = usercmd_t.BYTES + Integer.BYTES;

        usercmd_t cmd;
        int consistencyHash;

        @Override
        public ByteBuffer AllocBuffer() {
            return ByteBuffer.allocate(BYTES);
        }

        @Override
        public void Read(final ByteBuffer buffer) {
            throw new TODO_Exception();
        }

        @Override
        public ByteBuffer Write() {
            throw new TODO_Exception();
        }
    };

    static class fileTIME_T {

        int index;
        long/*ID_TIME_T*/ timeStamp;
//					operator int() const { return timeStamp; }
    };

    public static class mapSpawnData_t {

        public idDict      serverInfo           = new idDict();
        public idDict      syncedCVars          = new idDict();
        public idDict[]    userInfo             = new idDict[MAX_ASYNC_CLIENTS];
        public idDict[]    persistentPlayerInfo = new idDict[MAX_ASYNC_CLIENTS];
        public usercmd_t[] mapSpawnUsercmd      = new usercmd_t[MAX_ASYNC_CLIENTS];        // needed for tracking delta angles

        public mapSpawnData_t() {
            for (int a = 0; a < MAX_ASYNC_CLIENTS; a++) {
                userInfo[a] = new idDict();
                persistentPlayerInfo[a] = new idDict();
                mapSpawnUsercmd[a] = new usercmd_t();
            }
        }
    };

    enum timeDemo_t {

        TD_NO,
        TD_YES,
        TD_YES_THEN_QUIT
    };
//
    static int USERCMD_PER_DEMO_FRAME = 2;
    static int CONNECT_TRANSMIT_TIME  = 1000;
    static int MAX_LOGGED_USERCMDS    = 60 * 60 * 60;    // one hour of single player, 15 minutes of four player

    /*
     ===============================================================================

     SESSION LOCAL

     ===============================================================================
     */
    public static class idSessionLocal extends idSession {

        //
        //=====================================
        //
        public static final idCVar com_showAngles       = new idCVar("com_showAngles", "0", CVAR_SYSTEM | CVAR_BOOL, "");
        public static final idCVar com_showTics         = new idCVar("com_showTics", "0", CVAR_SYSTEM | CVAR_BOOL, "");
        public static final idCVar com_minTics          = new idCVar("com_minTics", "1", CVAR_SYSTEM, "");
        public static final idCVar com_fixedTic         = new idCVar("com_fixedTic", "0", CVAR_SYSTEM | CVAR_INTEGER, "", 0, 10);
        public static final idCVar com_showDemo         = new idCVar("com_showDemo", "0", CVAR_SYSTEM | CVAR_BOOL, "");
        public static final idCVar com_skipGameDraw     = new idCVar("com_skipGameDraw", "0", CVAR_SYSTEM | CVAR_BOOL, "");
        public static final idCVar com_aviDemoWidth     = new idCVar("com_aviDemoWidth", "256", CVAR_SYSTEM, "");
        public static final idCVar com_aviDemoHeight    = new idCVar("com_aviDemoHeight", "256", CVAR_SYSTEM, "");
        public static final idCVar com_aviDemoSamples   = new idCVar("com_aviDemoSamples", "16", CVAR_SYSTEM, "");
        public static final idCVar com_aviDemoTics      = new idCVar("com_aviDemoTics", "2", CVAR_SYSTEM | CVAR_INTEGER, "", 1, 60);
        public static final idCVar com_wipeSeconds      = new idCVar("com_wipeSeconds", "1", CVAR_SYSTEM, "");
        public static final idCVar com_guid             = new idCVar("com_guid", "", CVAR_SYSTEM | CVAR_ARCHIVE | CVAR_ROM, "");
        //
        public static final idCVar gui_configServerRate = new idCVar("gui_configServerRate", "0", CVAR_GUI | CVAR_ARCHIVE | CVAR_ROM | CVAR_INTEGER, "");
        //
        public int          timeHitch;
        //
        public boolean      menuActive;
        public idSoundWorld menuSoundWorld;             // so the game soundWorld can be muted
        //
        public boolean      insideExecuteMapChange;     // draw loading screen and update screen on prints
        public int          bytesNeededForMapLoad;      //
        //
        // we don't want to redraw the loading screen for every single
        // console print that happens
        public int          lastPacifierTime;
        //
        // this is the information required to be set before ExecuteMapChange() is called,
        // which can be saved off at any time with the following commands so it can all be played back
        public mapSpawnData_t mapSpawnData   = new mapSpawnData_t();
        public idStr          currentMapName = new idStr();// for checking reload on same level
        public boolean mapSpawned;                      // cleared on Stop()
        //
        public int     numClients;                      // from serverInfo
        //
        public int     logIndex;
        public logCmd_t[] loggedUsercmds;
        public int statIndex;
        public logStats_t[] loggedStats;
        public int     lastSaveIndex;
        // each game tic, numClients usercmds will be added, until full
        //
        public boolean insideUpdateScreen;              // true while inside ::UpdateScreen()
        //
        public boolean loadingSaveGame;                 // currently loading map from a SaveGame
        public idFile  savegameFile;                    // this is the savegame file to load from
        public int     savegameVersion;
        //
        public idFile  cmdDemoFile;                     // if non-zero, we are reading commands from a file
        //
        public int     latchedTicNumber;                // set to com_ticNumber each frame
        public int     lastGameTic;                     // while latchedTicNumber > lastGameTic, run game frames
        public int     lastDemoTic;
        public boolean syncNextGameFrame;
        //
        //
        public boolean aviCaptureMode;                  // if true, screenshots will be taken and sound captured
        public idStr aviDemoShortName = new idStr();    //
        public float              aviDemoFrameCount;
        public int                aviTicStart;
        //
        public timeDemo_t         timeDemo;
        public int                timeDemoStartTime;
        public int                numDemoFrames;        // for timeDemo and demoShot
        public int                demoTimeOffset;
        public renderView_s       currentDemoRenderView;
        // the next one will be read when 
        // com_frameTime + demoTimeOffset > currentDemoRenderView.
        //        
        // TODO: make this private (after sync networking removal and idnet tweaks)
        public idUserInterface    guiActive;
        public HandleGuiCommand_t guiHandle;
        //        
        public idUserInterface    guiInGame;
        public idUserInterface    guiMainMenu;
        public idListGUI          guiMainMenu_MapList;  // easy map list handling
        public idUserInterface    guiRestartMenu;
        public idUserInterface    guiLoading;
        public idUserInterface    guiIntro;
        public idUserInterface    guiGameOver;
        public idUserInterface    guiTest;
        public idUserInterface    guiTakeNotes;
        // 
        public idUserInterface    guiMsg;
        public idUserInterface    guiMsgRestore;        // store the calling GUI for restore
        public idStr[] msgFireBack = {new idStr(), new idStr()};
        public           boolean    msgRunning;
        public           int        msgRetIndex;
        public           boolean    msgIgnoreButtons;
        // 
        public           boolean    waitingOnBind;
        // 
        public /*const*/ idMaterial whiteMaterial;
        // 
        public /*const*/ idMaterial wipeMaterial;
        public           int        wipeStartTic;
        public           int        wipeStopTic;
        public           boolean    wipeHold;
        //
        // #if ID_CONSOLE_LOCK
        public           int        emptyDrawCount;     // watchdog to force the main menu to restart
        // #endif
        //	

        enum cdKeyState_t {

            CDKEY_UNKNOWN, // need to perform checks on the key
            CDKEY_INVALID, // that key is wrong
            CDKEY_OK,      // valid
            CDKEY_CHECKING,// sent a check request ( gameAuth only )
            CDKEY_NA	   // does not apply, xp key when xp is not present
        };
        //
        private static final int    CDKEY_BUF_LEN      = 17;
        private static final int    CDKEY_AUTH_TIMEOUT = 5000;
        //
        private final        char[] cdkey              = new char[CDKEY_BUF_LEN];
        private cdKeyState_t cdkey_state;
        private final char[] xpkey = new char[CDKEY_BUF_LEN];
        private cdKeyState_t xpkey_state;
        private int          authEmitTimeout;
        private boolean      authWaitBox;
        //
        private idStr authMsg = new idStr();
        //
        //=====================================

        public idSessionLocal() {
            guiInGame = guiMainMenu = guiIntro
                    = guiRestartMenu = guiLoading = guiGameOver = guiActive
                    = guiTest = guiMsg = guiMsgRestore = guiTakeNotes = null;

            menuSoundWorld = null;
            loggedUsercmds = Stream.generate(logCmd_t::new).limit(MAX_LOGGED_USERCMDS).toArray(logCmd_t[]::new);
            loggedStats = Stream.generate(logStats_t::new).limit(MAX_LOGGED_STATS).toArray(logStats_t[]::new);

            Clear();
        }

        /*
         ===============
         idSessionLocal::Init

         Called in an orderly fashion at system startup,
         so commands, cvars, files, etc are all available
         ===============
         */
        @Override
        public void Init() throws idException {

            common.Printf("-------- Initializing Session --------\n");

            cmdSystem.AddCommand("writePrecache", Sess_WritePrecache_f.getInstance(), CMD_FL_SYSTEM | CMD_FL_CHEAT, "writes precache commands");

//            if (ID_DEDICATED) {
                cmdSystem.AddCommand("map", Session_Map_f.getInstance(), CMD_FL_SYSTEM, "loads a map", idCmdSystem.ArgCompletion_MapName.getInstance());
                cmdSystem.AddCommand("devmap", Session_DevMap_f.getInstance(), CMD_FL_SYSTEM, "loads a map in developer mode", idCmdSystem.ArgCompletion_MapName.getInstance());
                cmdSystem.AddCommand("testmap", Session_TestMap_f.getInstance(), CMD_FL_SYSTEM, "tests a map", idCmdSystem.ArgCompletion_MapName.getInstance());

                cmdSystem.AddCommand("writeCmdDemo", Session_WriteCmdDemo_f.getInstance(), CMD_FL_SYSTEM, "writes a command demo");
                cmdSystem.AddCommand("playCmdDemo", Session_PlayCmdDemo_f.getInstance(), CMD_FL_SYSTEM, "plays back a command demo");
                cmdSystem.AddCommand("timeCmdDemo", Session_TimeCmdDemo_f.getInstance(), CMD_FL_SYSTEM, "times a command demo");
                cmdSystem.AddCommand("exitCmdDemo", Session_ExitCmdDemo_f.getInstance(), CMD_FL_SYSTEM, "exits a command demo");
                cmdSystem.AddCommand("aviCmdDemo", Session_AVICmdDemo_f.getInstance(), CMD_FL_SYSTEM, "writes AVIs for a command demo");
                cmdSystem.AddCommand("aviGame", Session_AVIGame_f.getInstance(), CMD_FL_SYSTEM, "writes AVIs for the current game");

                cmdSystem.AddCommand("recordDemo", Session_RecordDemo_f.getInstance(), CMD_FL_SYSTEM, "records a demo");
                cmdSystem.AddCommand("stopRecording", Session_StopRecordingDemo_f.getInstance(), CMD_FL_SYSTEM, "stops demo recording");
                cmdSystem.AddCommand("playDemo", Session_PlayDemo_f.getInstance(), CMD_FL_SYSTEM, "plays back a demo", idCmdSystem.ArgCompletion_DemoName.getInstance());
                cmdSystem.AddCommand("timeDemo", Session_TimeDemo_f.getInstance(), CMD_FL_SYSTEM, "times a demo", idCmdSystem.ArgCompletion_DemoName.getInstance());
                cmdSystem.AddCommand("timeDemoQuit", Session_TimeDemoQuit_f.getInstance(), CMD_FL_SYSTEM, "times a demo and quits", idCmdSystem.ArgCompletion_DemoName.getInstance());
                cmdSystem.AddCommand("aviDemo", Session_AVIDemo_f.getInstance(), CMD_FL_SYSTEM, "writes AVIs for a demo", idCmdSystem.ArgCompletion_DemoName.getInstance());
                cmdSystem.AddCommand("compressDemo", Session_CompressDemo_f.getInstance(), CMD_FL_SYSTEM, "compresses a demo file", idCmdSystem.ArgCompletion_DemoName.getInstance());
//            }

            cmdSystem.AddCommand("disconnect", Session_Disconnect_f.getInstance(), CMD_FL_SYSTEM, "disconnects from a game");

            if (ID_DEMO_BUILD) {
                cmdSystem.AddCommand("endOfDemo", Session_EndOfDemo_f.getInstance(), CMD_FL_SYSTEM, "ends the demo version of the game");
            }

            cmdSystem.AddCommand("demoShot", Session_DemoShot_f.getInstance(), CMD_FL_SYSTEM, "writes a screenshot for a demo");
            cmdSystem.AddCommand("testGUI", Session_TestGUI_f.getInstance(), CMD_FL_SYSTEM, "tests a gui");

            if (ID_DEDICATED) {
                cmdSystem.AddCommand("saveGame", SaveGame_f.getInstance(), CMD_FL_SYSTEM | CMD_FL_CHEAT, "saves a game");
                cmdSystem.AddCommand("loadGame", LoadGame_f.getInstance(), CMD_FL_SYSTEM | CMD_FL_CHEAT, "loads a game", idCmdSystem.ArgCompletion_SaveGame.getInstance());
            }

            cmdSystem.AddCommand("takeViewNotes", TakeViewNotes_f.getInstance(), CMD_FL_SYSTEM, "take notes about the current map from the current view");
            cmdSystem.AddCommand("takeViewNotes2", TakeViewNotes2_f.getInstance(), CMD_FL_SYSTEM, "extended take view notes");

            cmdSystem.AddCommand("rescanSI", Session_RescanSI_f.getInstance(), CMD_FL_SYSTEM, "internal - rescan serverinfo cvars and tell game");

            cmdSystem.AddCommand("promptKey", Session_PromptKey_f.getInstance(), CMD_FL_SYSTEM, "prompt and sets the CD Key");

            cmdSystem.AddCommand("hitch", Session_Hitch_f.getInstance(), CMD_FL_SYSTEM | CMD_FL_CHEAT, "hitches the game");

            // the same idRenderWorld will be used for all games
            // and demos, insuring that level specific models
            // will be freed
            rw = renderSystem.AllocRenderWorld();
            sw = soundSystem.AllocSoundWorld(rw);

            menuSoundWorld = soundSystem.AllocSoundWorld(rw);

            // we have a single instance of the main menu
            if (ID_DEMO_BUILD) {//#ifndef
                guiMainMenu = uiManager.FindGui("guis/demo_mainmenu.gui", true, false, true);
            } else {
                guiMainMenu = uiManager.FindGui("guis/mainmenu.gui", true, false, true);
            }
            guiMainMenu_MapList = uiManager.AllocListGUI();
            guiMainMenu_MapList.Config(guiMainMenu, "mapList");
            idAsyncNetwork.client.serverList.GUIConfig(guiMainMenu, "serverList");
            guiRestartMenu = uiManager.FindGui("guis/restart.gui", true, false, true);
            guiGameOver = uiManager.FindGui("guis/gameover.gui", true, false, true);
            guiMsg = uiManager.FindGui("guis/msg.gui", true, false, true);
            guiTakeNotes = uiManager.FindGui("guis/takeNotes.gui", true, false, true);
            guiIntro = uiManager.FindGui("guis/intro.gui", true, false, true);

            whiteMaterial = declManager.FindMaterial("_white");

            guiInGame = null;
            guiTest = null;

            guiActive = null;
            guiHandle = null;

            ReadCDKey();

            common.Printf("session initialized\n");
            common.Printf("--------------------------------------\n");
        }

        @Override
        public void Shutdown() {
            int i;

            if (aviCaptureMode) {
                EndAVICapture();
            }

            Stop();

            if (rw != null) {
//		delete rw;
                rw = null;
            }

            if (sw != null) {
//		delete sw;
                sw = null;
            }

            if (menuSoundWorld != null) {
//		delete menuSoundWorld;
                menuSoundWorld = null;
            }

            mapSpawnData.serverInfo.Clear();
            mapSpawnData.syncedCVars.Clear();
            for (i = 0; i < MAX_ASYNC_CLIENTS; i++) {
                mapSpawnData.userInfo[i].Clear();
                mapSpawnData.persistentPlayerInfo[i].Clear();
            }

            if (guiMainMenu_MapList != null) {
                guiMainMenu_MapList.Shutdown();
                uiManager.FreeListGUI(guiMainMenu_MapList);
                guiMainMenu_MapList = null;
            }

            Clear();
        }

        /*
         ===============
         idSessionLocal::Stop

         called on errors and game exits
         ===============
         */
        @Override
        public void Stop() {
            ClearWipe();

            // clear mapSpawned and demo playing flags
            UnloadMap();

            // disconnect async client
            idAsyncNetwork.client.DisconnectFromServer();

            // kill async server
            idAsyncNetwork.server.Kill();

            if (sw != null) {
                sw.StopAllSounds();
            }

            insideUpdateScreen = false;
            insideExecuteMapChange = false;

            // drop all guis
            SetGUI(null, null);
        }

        @Override
        public void UpdateScreen() {
            UpdateScreen(true);
        }

        private static int DBG_EndFrame = 0;

        @Override
        public void UpdateScreen(boolean outOfSequence) {

            if (_WIN32) {

                if (com_editors != 0) {
                    if (!Sys_IsWindowVisible()) {
                        return;
                    }
                }
            }

            if (insideUpdateScreen) {
                common.FatalError("idSessionLocal::UpdateScreen: recursively called");
                return;
            }

            insideUpdateScreen = true;

            // if this is a long-operation update and we are in windowed mode,
            // release the mouse capture back to the desktop
            if (outOfSequence) {
                Sys_GrabMouseCursor(false);
            }

            renderSystem.BeginFrame(renderSystem.GetScreenWidth(), renderSystem.GetScreenHeight());

            // draw everything
            Draw();

            DBG_EndFrame++;
            if (com_speeds.GetBool()) {
                int[] time_frontend = {0}, time_backend = {0};
                renderSystem.EndFrame(time_frontend, time_backend);
                Common.time_frontend = time_frontend[0];
                Common.time_backend = time_backend[0];
            } else {
                renderSystem.EndFrame(null, null);
            }

            insideUpdateScreen = false;
        }

        @Override
        public void PacifierUpdate() {
            if (!insideExecuteMapChange) {
                return;
            }

            // never do pacifier screen updates while inside the
            // drawing code, or we can have various recursive problems
            if (insideUpdateScreen) {
                return;
            }

            int time = eventLoop.Milliseconds();

            if (time - lastPacifierTime < 100) {
                return;
            }
            lastPacifierTime = time;

            if (guiLoading != null && bytesNeededForMapLoad != 0) {
                float n = fileSystem.GetReadCount();
                float pct = (n / bytesNeededForMapLoad);
                // pct = idMath::ClampFloat( 0.0f, 100.0f, pct );
                guiLoading.SetStateFloat("map_loading", pct);
                guiLoading.StateChanged(com_frameTime);
            }

            Sys_GenerateEvents();

            UpdateScreen();

            idAsyncNetwork.client.PacifierUpdate();
            idAsyncNetwork.server.PacifierUpdate();
        }

        @Override
        public void Frame() throws idException {

            if (com_asyncSound.GetInteger() == 0) {
                soundSystem.AsyncUpdate(Sys_Milliseconds());
            }

            // Editors that completely take over the game
            if (com_editorActive && (com_editors & (EDITOR_RADIANT | EDITOR_GUI)) != 0) {
                return;
            }

            // if the console is down, we don't need to hold
            // the mouse cursor
            if (console.Active() || com_editorActive) {
                Sys_GrabMouseCursor(false);
            } else {
                Sys_GrabMouseCursor(true);
            }

            // save the screenshot and audio from the last draw if needed
            if (aviCaptureMode) {
                idStr name;

                name = new idStr(va("demos/%s/%s_%05i.tga", aviDemoShortName.toString(), aviDemoShortName.toString(), aviTicStart));

                float ratio = 30.0f / (1000.0f / USERCMD_MSEC / com_aviDemoTics.GetInteger());
                aviDemoFrameCount += ratio;
                if (aviTicStart + 1 != (int) aviDemoFrameCount) {
                    // skipped frames so write them out
                    int c = (int) (aviDemoFrameCount - aviTicStart);
                    while (c-- != 0) {
                        renderSystem.TakeScreenshot(com_aviDemoWidth.GetInteger(), com_aviDemoHeight.GetInteger(), name.toString(), com_aviDemoSamples.GetInteger(), null);
                        name.oSet(va("demos/%s/%s_%05i.tga", aviDemoShortName.toString(), aviDemoShortName.toString(), ++aviTicStart));
                    }
                }
                aviTicStart = (int) aviDemoFrameCount;

                // remove any printed lines at the top before taking the screenshot
                console.ClearNotifyLines();

                // this will call Draw, possibly multiple times if com_aviDemoSamples is > 1
                renderSystem.TakeScreenshot(com_aviDemoWidth.GetInteger(), com_aviDemoHeight.GetInteger(), name.toString(), com_aviDemoSamples.GetInteger(), null);
            }

            // at startup, we may be backwards
            if (latchedTicNumber > com_ticNumber) {
                latchedTicNumber = com_ticNumber;
            }

            // se how many tics we should have before continuing
            int minTic = latchedTicNumber + 1;
            if (com_minTics.GetInteger() > 1) {
                minTic = lastGameTic + com_minTics.GetInteger();
            }

            if (readDemo != null) {
                if (null == timeDemo && numDemoFrames != 1) {
                    minTic = lastDemoTic + USERCMD_PER_DEMO_FRAME;
                } else {
                    // timedemos and demoshots will run as fast as they can, other demos
                    // will not run more than 30 hz
                    minTic = latchedTicNumber;
                }
            } else if (writeDemo != null) {
                minTic = lastGameTic + USERCMD_PER_DEMO_FRAME;		// demos are recorded at 30 hz
            }

            // fixedTic lets us run a forced number of usercmd each frame without timing
            if (com_fixedTic.GetInteger() != 0) {
                minTic = latchedTicNumber;
            }

            // FIXME: deserves a cleanup and abstraction
            if (_WIN32) {
                // Spin in place if needed.  The game should yield the cpu if
                // it is running over 60 hz, because there is fundamentally
                // nothing useful for it to do.
                while (true) {
                    latchedTicNumber = com_ticNumber;
//                    System.out.printf("Frame(%d, %d)\n", latchedTicNumber, minTic);
                    if (latchedTicNumber >= minTic) {
                        break;
                    }
                    Sys_Sleep(1);
                    win_main.hTimer.isTerminated();
//                    if (win_main.DEBUG) {
//                        //TODO:the debugger slows the code too much at this point, so we shall manually move to the next frame.
//                        com_ticNumber = minTic;
//                    }
                }
            } else {
                while (true) {
                    latchedTicNumber = com_ticNumber;
                    if (latchedTicNumber >= minTic) {
                        break;
                    }
                    Sys_WaitForEvent(TRIGGER_EVENT_ONE);
                }
            }

            if (authEmitTimeout != 0) {
                // waiting for a game auth
                if (Sys_Milliseconds() > authEmitTimeout) {
                    // expired with no reply
                    // means that if a firewall is blocking the master, we will let through
                    common.DPrintf("no reply from auth\n");
                    if (authWaitBox) {
                        // close the wait box
                        StopBox();
                        authWaitBox = false;
                    }
                    if (cdkey_state == CDKEY_CHECKING) {
                        cdkey_state = CDKEY_OK;
                    }
                    if (xpkey_state == CDKEY_CHECKING) {
                        xpkey_state = CDKEY_OK;
                    }
                    // maintain this empty as it's set by auth denials
                    authMsg.Empty();
                    authEmitTimeout = 0;
                    SetCDKeyGuiVars();
                }
            }

            // send frame and mouse events to active guis
            GuiFrameEvents();

            // advance demos
            if (readDemo != null) {
                AdvanceRenderDemo(false);
                return;
            }

            //------------ single player game tics --------------
            if (!mapSpawned || guiActive != null) {
                if (!com_asyncInput.GetBool()) {
                    // early exit, won't do RunGameTic .. but still need to update mouse position for GUIs
                    usercmdGen.GetDirectUsercmd();
                }
            }

            if (!mapSpawned) {
                return;
            }

            if (guiActive != null) {
                lastGameTic = latchedTicNumber;
                return;
            }

            // in message box / GUIFrame, idSessionLocal::Frame is used for GUI interactivity
            // but we early exit to avoid running game frames
            if (idAsyncNetwork.IsActive()) {
                return;
            }

            // check for user info changes
            if ((cvarSystem.GetModifiedFlags() & CVAR_USERINFO) != 0) {
                mapSpawnData.userInfo[0] = cvarSystem.MoveCVarsToDict(CVAR_USERINFO);
                game.SetUserInfo(0, mapSpawnData.userInfo[0], false, false);
                cvarSystem.ClearModifiedFlags(CVAR_USERINFO);
            }

            // see how many usercmds we are going to run
            int numCmdsToRun = latchedTicNumber - lastGameTic;

            // don't let a long onDemand sound load unsync everything
            if (timeHitch != 0) {
                int skip = timeHitch / USERCMD_MSEC;
                lastGameTic += skip;
                numCmdsToRun -= skip;
                timeHitch = 0;
            }

            // don't get too far behind after a hitch
            if (numCmdsToRun > 10) {
                lastGameTic = latchedTicNumber - 10;
            }

            // never use more than USERCMD_PER_DEMO_FRAME,
            // which makes it go into slow motion when recording
            if (writeDemo != null) {
                int fixedTic = USERCMD_PER_DEMO_FRAME;
                // we should have waited long enough
                if (numCmdsToRun < fixedTic) {
                    common.Error("idSessionLocal::Frame: numCmdsToRun < fixedTic");
                }
                // we may need to dump older commands
                lastGameTic = latchedTicNumber - fixedTic;
            } else if (com_fixedTic.GetInteger() > 0) {
                // this may cause commands run in a previous frame to
                // be run again if we are going at above the real time rate
                lastGameTic = latchedTicNumber - com_fixedTic.GetInteger();
            } else if (aviCaptureMode) {
                lastGameTic = latchedTicNumber - com_aviDemoTics.GetInteger();
            }

            // force only one game frame update this frame.  the game code requests this after skipping cinematics
            // so we come back immediately after the cinematic is done instead of a few frames later which can
            // cause sounds played right after the cinematic to not play.
            if (syncNextGameFrame) {
                lastGameTic = latchedTicNumber - 1;
                syncNextGameFrame = false;
            }

            // create client commands, which will be sent directly
            // to the game
            if (com_showTics.GetBool()) {
                common.Printf("%d ", latchedTicNumber - lastGameTic);
            }

            int gameTicsToRun = latchedTicNumber - lastGameTic;
            int i;
            for (i = 0; i < gameTicsToRun; i++) {
                RunGameTic();
                if (!mapSpawned) {
                    // exited game play
                    break;
                }
                if (syncNextGameFrame) {
                    // long game frame, so break out and continue executing as if there was no hitch
                    break;
                }
            }
        }

        @Override
        public boolean IsMultiplayer() {
            return idAsyncNetwork.IsActive();
        }

        @Override
        public boolean ProcessEvent(sysEvent_s event) throws idException {
            // hitting escape anywhere brings up the menu
            if (NOT(guiActive) && event.evType == SE_KEY && event.evValue2 == 1 && event.evValue == K_ESCAPE) {
                console.Close();
                if (game != null) {
                    idUserInterface[] gui = {null};
                    escReply_t op;
                    op = game.HandleESC(gui);
                    if (op == ESC_IGNORE) {
                        return true;
                    } else if (op == ESC_GUI) {
                        SetGUI(gui[0], null);
                        return true;
                    }
                }
                StartMenu();
                return true;
            }

            // let the pull-down console take it if desired
            if (console.ProcessEvent(event, false)) {
                return true;
            }

            // if we are testing a GUI, send all events to it
            if (guiTest != null) {
                // hitting escape exits the testgui
                if (event.evType == SE_KEY && event.evValue2 == 1 && event.evValue == K_ESCAPE) {
                    guiTest = null;
                    return true;
                }

                cmd = guiTest.HandleEvent(event, com_frameTime).toCharArray();
                if (cmd != null && cmd[0] != '\0') {
                    common.Printf("testGui event returned: '%s'\n", cmd);
                }
                return true;
            }

            // menus / etc
            if (guiActive != null) {
                MenuEvent(event);
                return true;
            }

            // if we aren't in a game, force the console to take it
            if (!mapSpawned) {
                console.ProcessEvent(event, true);
                return true;
            }

            // in game, exec bindings for all key downs
            if (event.evType == SE_KEY && event.evValue2 == 1) {
                idKeyInput.ExecKeyBinding(event.evValue);
                return true;
            }

            return false;
        }
        private static char[] cmd;//TODO:stringify?

        @Override
        public void StartMenu(boolean playIntro) throws idException {
            if (guiActive == guiMainMenu) {
                return;
            }

            if (readDemo != null) {
                // if we're playing a demo, esc kills it
                UnloadMap();
            }

            // pause the game sound world
            if (sw != null && !sw.IsPaused()) {
                sw.Pause();
            }

            // start playing the menu sounds
            soundSystem.SetPlayingSoundWorld(menuSoundWorld);

            SetGUI(guiMainMenu, null);
            guiMainMenu.HandleNamedEvent(playIntro ? "playIntro" : "noIntro");

            if (fileSystem.HasD3XP()) {
                guiMainMenu.SetStateString("game_list", common.GetLanguageDict().GetString("#str_07202"));
            } else {
                guiMainMenu.SetStateString("game_list", common.GetLanguageDict().GetString("#str_07212"));
            }

            console.Close();

        }

        public void ExitMenu() {
            guiActive = null;

            // go back to the game sounds
            soundSystem.SetPlayingSoundWorld(sw);

            // unpause the game sound world
            if (sw != null && sw.IsPaused()) {
                sw.UnPause();
            }
        }

        @Override
        public void SetGUI(idUserInterface gui, HandleGuiCommand_t handle) {
            String cmd;

            guiActive = gui;
            guiHandle = handle;
            if (guiMsgRestore != null) {
                common.DPrintf("idSessionLocal::SetGUI: cleared an active message box\n");
                guiMsgRestore = null;
            }
            if (NOT(guiActive)) {
                return;
            }

            if (guiActive == guiMainMenu) {
                SetSaveGameGuiVars();
                SetMainMenuGuiVars();
            } else if (guiActive == guiRestartMenu) {
                SetSaveGameGuiVars();
            }

            sysEvent_s ev;
            ev = new sysEvent_s();//memset( ev, 0, sizeof( ev ) );
            ev.evType = SE_NONE;

            cmd = guiActive.HandleEvent(ev, com_frameTime);
            guiActive.Activate(true, com_frameTime);
        }
        static int frameEvents = 0;

        @Override
        public void GuiFrameEvents() {
            frameEvents++;
            String cmd;
            sysEvent_s ev;
            idUserInterface gui;

            // stop generating move and button commands when a local console or menu is active
            // running here so SP, async networking and no game all go through it
            if (console.Active() || guiActive != null) {
                usercmdGen.InhibitUsercmd(INHIBIT_SESSION, true);
            } else {
                usercmdGen.InhibitUsercmd(INHIBIT_SESSION, false);
            }

            if (guiTest != null) {
                gui = guiTest;
            } else if (guiActive != null) {
                gui = guiActive;
            } else {
                return;
            }

//	memset( &ev, 0, sizeof( ev ) );
            ev = new sysEvent_s();
            ev.evType = SE_NONE;
//            System.out.println(System.nanoTime()+"com_frameTime="+com_frameTime+" "+Common.com_ticNumber);
            cmd = gui.HandleEvent(ev, com_frameTime);
            if (isNotNullOrEmpty(cmd)) {
                DispatchCommand(guiActive, cmd, false);
            }
        }

        @Override
        public String MessageBox(msgBoxType_t type, String message) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public String MessageBox(msgBoxType_t type, String message, String title) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public String MessageBox(msgBoxType_t type, String message, String title, boolean wait) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public String MessageBox(msgBoxType_t type, String message, String title, boolean wait, String fire_yes) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public String MessageBox(msgBoxType_t type, String message, String title, boolean wait, String fire_yes, String fire_no) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public String MessageBox(msgBoxType_t type, String message, String title, boolean wait, String fire_yes, String fire_no, boolean network) {
            common.DPrintf("MessageBox: %s - %s\n", "" + title, "" + message);

            if (!BoxDialogSanityCheck()) {
                return null;
            }

            guiMsg.SetStateString("title", "" + title);
            guiMsg.SetStateString("message", "" + message);
            if (type == MSG_WAIT) {
                guiMsg.SetStateString("visible_msgbox", "0");
                guiMsg.SetStateString("visible_waitbox", "1");
            } else {
                guiMsg.SetStateString("visible_msgbox", "1");
                guiMsg.SetStateString("visible_waitbox", "0");
            }

            guiMsg.SetStateString("visible_entry", "0");
            guiMsg.SetStateString("visible_cdkey", "0");
            switch (type) {
                case MSG_INFO:
                    guiMsg.SetStateString("mid", "");
                    guiMsg.SetStateString("visible_mid", "0");
                    guiMsg.SetStateString("visible_left", "0");
                    guiMsg.SetStateString("visible_right", "0");
                    break;
                case MSG_OK:
                    guiMsg.SetStateString("mid", common.GetLanguageDict().GetString("#str_04339"));
                    guiMsg.SetStateString("visible_mid", "1");
                    guiMsg.SetStateString("visible_left", "0");
                    guiMsg.SetStateString("visible_right", "0");
                    break;
                case MSG_ABORT:
                    guiMsg.SetStateString("mid", common.GetLanguageDict().GetString("#str_04340"));
                    guiMsg.SetStateString("visible_mid", "1");
                    guiMsg.SetStateString("visible_left", "0");
                    guiMsg.SetStateString("visible_right", "0");
                    break;
                case MSG_OKCANCEL:
                    guiMsg.SetStateString("left", common.GetLanguageDict().GetString("#str_04339"));
                    guiMsg.SetStateString("right", common.GetLanguageDict().GetString("#str_04340"));
                    guiMsg.SetStateString("visible_mid", "0");
                    guiMsg.SetStateString("visible_left", "1");
                    guiMsg.SetStateString("visible_right", "1");
                    break;
                case MSG_YESNO:
                    guiMsg.SetStateString("left", common.GetLanguageDict().GetString("#str_04341"));
                    guiMsg.SetStateString("right", common.GetLanguageDict().GetString("#str_04342"));
                    guiMsg.SetStateString("visible_mid", "0");
                    guiMsg.SetStateString("visible_left", "1");
                    guiMsg.SetStateString("visible_right", "1");
                    break;
                case MSG_PROMPT:
                    guiMsg.SetStateString("left", common.GetLanguageDict().GetString("#str_04339"));
                    guiMsg.SetStateString("right", common.GetLanguageDict().GetString("#str_04340"));
                    guiMsg.SetStateString("visible_mid", "0");
                    guiMsg.SetStateString("visible_left", "1");
                    guiMsg.SetStateString("visible_right", "1");
                    guiMsg.SetStateString("visible_entry", "1");
                    guiMsg.HandleNamedEvent("Prompt");
                    break;
                case MSG_CDKEY:
                    guiMsg.SetStateString("left", common.GetLanguageDict().GetString("#str_04339"));
                    guiMsg.SetStateString("right", common.GetLanguageDict().GetString("#str_04340"));
                    guiMsg.SetStateString("visible_msgbox", "0");
                    guiMsg.SetStateString("visible_cdkey", "1");
                    guiMsg.SetStateString("visible_hasxp", fileSystem.HasD3XP() ? "1" : "0");
		    // the current cdkey / xpkey values may have bad/random data in them
                    // it's best to avoid printing them completely, unless the key is good
                    if (cdkey_state == CDKEY_OK) {
                        guiMsg.SetStateString("str_cdkey", new String(cdkey));
                        guiMsg.SetStateString("visible_cdchk", "0");
                    } else {
                        guiMsg.SetStateString("str_cdkey", "");
                        guiMsg.SetStateString("visible_cdchk", "1");
                    }
                    guiMsg.SetStateString("str_cdchk", "");
                    if (xpkey_state == CDKEY_OK) {
                        guiMsg.SetStateString("str_xpkey", new String(xpkey));
                        guiMsg.SetStateString("visible_xpchk", "0");
                    } else {
                        guiMsg.SetStateString("str_xpkey", "");
                        guiMsg.SetStateString("visible_xpchk", "1");
                    }
                    guiMsg.SetStateString("str_xpchk", "");
                    guiMsg.HandleNamedEvent("CDKey");
                    break;
                case MSG_WAIT:
                    break;
                default:
                    common.Printf("idSessionLocal::MessageBox: unknown msg box type\n");
            }
            msgFireBack[0].oSet("" + fire_yes);
            msgFireBack[1].oSet("" + fire_no);
            guiMsgRestore = guiActive;
            guiActive = guiMsg;
            guiMsg.SetCursor(325, 290);
            guiActive.Activate(true, com_frameTime);
            msgRunning = true;
            msgRetIndex = -1;

            if (wait) {
                // play one frame ignoring events so we don't get confused by parasite button releases
                msgIgnoreButtons = true;
                common.GUIFrame(true, network);
                msgIgnoreButtons = false;
                while (msgRunning) {
                    common.GUIFrame(true, network);
                }
                if (msgRetIndex < 0) {
                    // MSG_WAIT and other StopBox calls
                    return null;
                }
                if (type == MSG_PROMPT) {
                    if (msgRetIndex == 0) {
                        guiMsg.State().GetString("str_entry", "", msgFireBack[0]);
                        return msgFireBack[0].toString();
                    } else {
                        return null;
                    }
                } else if (type == MSG_CDKEY) {
                    if (msgRetIndex == 0) {
                        // the visible_ values distinguish looking at a valid key, or editing it
                        msgFireBack[0].oSet(String.format("%1s;%16s;%2s;%1s;%16s;%2s",
                                guiMsg.State().GetString("visible_cdchk"),
                                guiMsg.State().GetString("str_cdkey"),
                                guiMsg.State().GetString("str_cdchk"),
                                guiMsg.State().GetString("visible_xpchk"),
                                guiMsg.State().GetString("str_xpkey"),
                                guiMsg.State().GetString("str_xpchk")));
                        return msgFireBack[0].toString();
                    } else {
                        return null;
                    }
                } else {
                    return msgFireBack[msgRetIndex].toString();
                }
            }
            return null;
        }

        @Override
        public void StopBox() {
            if (guiActive == guiMsg) {
                HandleMsgCommands("stop");
            }
        }

        @Override
        public void DownloadProgressBox(backgroundDownload_s bgl, String title) {
            DownloadProgressBox(bgl, title, 0);
        }

        @Override
        public void DownloadProgressBox(backgroundDownload_s bgl, String title, int progress_start) {
            DownloadProgressBox(bgl, title, progress_start, 100);
        }

        @Override
        public void DownloadProgressBox(backgroundDownload_s bgl, String title, int progress_start, int progress_end) {
            int dlnow = 0, dltotal = 0;
            int startTime = Sys_Milliseconds();
            int lapsed;
            idStr sNow = new idStr(), sTotal = new idStr(), sBW = new idStr();
            String sETA, sMsg;

            if (!BoxDialogSanityCheck()) {
                return;
            }

            guiMsg.SetStateString("visible_msgbox", "1");
            guiMsg.SetStateString("visible_waitbox", "0");

            guiMsg.SetStateString("visible_entry", "0");
            guiMsg.SetStateString("visible_cdkey", "0");

            guiMsg.SetStateString("mid", "Cancel");
            guiMsg.SetStateString("visible_mid", "1");
            guiMsg.SetStateString("visible_left", "0");
            guiMsg.SetStateString("visible_right", "0");

            guiMsg.SetStateString("title", title);
            guiMsg.SetStateString("message", "Connecting..");

            guiMsgRestore = guiActive;
            guiActive = guiMsg;
            msgRunning = true;

            while (true) {
                while (msgRunning) {
                    common.GUIFrame(true, false);
                    if (bgl.completed) {
                        guiActive = guiMsgRestore;
                        guiMsgRestore = null;
                        return;
                    } else if (bgl.url.dltotal != dltotal || bgl.url.dlnow != dlnow) {
                        dltotal = bgl.url.dltotal;
                        dlnow = bgl.url.dlnow;
                        lapsed = Sys_Milliseconds() - startTime;
                        sNow.BestUnit("%.2f", dlnow, MEASURE_SIZE);
                        if (lapsed > 2000) {
                            sBW.BestUnit("%.1f", (1000.0f * dlnow) / lapsed, MEASURE_BANDWIDTH);
                        } else {
                            sBW = new idStr("-- KB/s");
                        }
                        if (dltotal != 0) {
                            sTotal.BestUnit("%.2f", dltotal, MEASURE_SIZE);
                            if (lapsed < 2000) {
                                sMsg = String.format("%s / %s", sNow.toString(), sTotal.toString());
                            } else {
                                sETA = String.format("%.0f sec", ((float) dltotal / (float) dlnow - 1.0f) * lapsed / 1000);
                                sMsg = String.format("%s / %s ( %s - %s )", sNow.toString(), sTotal.toString(), sBW.toString(), sETA);
                            }
                        } else {
                            if (lapsed < 2000) {
                                sMsg = sNow.toString();
                            } else {
                                sMsg = String.format("%s - %s", sNow.toString(), sBW.toString());
                            }
                        }
                        if (dltotal != 0) {
                            guiMsg.SetStateString("progress", va("%d", progress_start + dlnow * (progress_end - progress_start) / dltotal));
                        } else {
                            guiMsg.SetStateString("progress", "0");
                        }
                        guiMsg.SetStateString("message", sMsg);
                    }
                }
                // abort was used - tell the downloader and wait till final stop
                bgl.url.status = DL_ABORTING;
                guiMsg.SetStateString("title", "Aborting..");
                guiMsg.SetStateString("visible_mid", "0");
                // continue looping
                guiMsgRestore = guiActive;
                guiActive = guiMsg;
                msgRunning = true;
            }
        }

        @Override
        public void SetPlayingSoundWorld() {
            if ((guiActive != null) && (guiActive.equals(guiMainMenu) || guiActive.equals(guiIntro) || guiActive.equals(guiLoading != null) || (guiActive.equals(guiMsg) && !mapSpawned))) {
                soundSystem.SetPlayingSoundWorld(menuSoundWorld);
            } else {
                soundSystem.SetPlayingSoundWorld(sw);
            }
        }


        /*
         ===============
         idSessionLocal::TimeHitch

         this is used by the sound system when an OnDemand sound is loaded, so the game action
         doesn't advance and get things out of sync
         ===============
         */
        @Override
        public void TimeHitch(int msec) {
            timeHitch += msec;
        }

        @Override
        public void ReadCDKey() {
            String filename;
            idFile f;
            ByteBuffer buffer = ByteBuffer.allocate(32 * 2);//=new char[32];

            cdkey_state = CDKEY_UNKNOWN;

            filename = "../" + BASE_GAMEDIR + "/" + CDKEY_FILE;
            f = fileSystem.OpenExplicitFileRead(fileSystem.RelativePathToOSPath(filename, "fs_savepath"));
            if (null == f) {
                common.Printf("Couldn't read %s.\n", filename);
                cdkey[0] = '\0';
            } else {
//		memset( buffer, 0, sizeof(buffer) );
                f.Read(buffer, CDKEY_BUF_LEN - 1);
                fileSystem.CloseFile(f);
                idStr.Copynz(cdkey, new String(buffer.array()), CDKEY_BUF_LEN);
            }

            xpkey_state = CDKEY_UNKNOWN;

            filename = "../" + BASE_GAMEDIR + "/" + XPKEY_FILE;
            f = fileSystem.OpenExplicitFileRead(fileSystem.RelativePathToOSPath(filename, "fs_savepath"));
            if (null == f) {
                common.Printf("Couldn't read %s.\n", filename);
                xpkey[0] = '\0';
            } else {
//		memset( buffer, 0, sizeof(buffer) );
                buffer.clear();
                f.Read(buffer, CDKEY_BUF_LEN - 1);
                fileSystem.CloseFile(f);
                idStr.Copynz(xpkey, new String(buffer.array()), CDKEY_BUF_LEN);
            }
        }

        @Override
        public void WriteCDKey() {
            String filename;
            idFile f;
            String OSPath;

            filename = "../" + BASE_GAMEDIR + "/" + CDKEY_FILE;
            // OpenFileWrite advertises creating directories to the path if needed, but that won't work with a '..' in the path
            // occasionally on windows, but mostly on Linux and OSX, the fs_savepath/base may not exist in full
            OSPath = fileSystem.BuildOSPath(cvarSystem.GetCVarString("fs_savepath"), BASE_GAMEDIR, CDKEY_FILE);
            fileSystem.CreateOSPath(OSPath);
            f = fileSystem.OpenFileWrite(filename);
            if (null == f) {
                common.Printf("Couldn't write %s.\n", filename);
                return;
            }
            f.Printf("%s%s", cdkey, CDKEY_TEXT);
            fileSystem.CloseFile(f);

            filename = "../" + BASE_GAMEDIR + "/" + XPKEY_FILE;
            f = fileSystem.OpenFileWrite(filename);
            if (null == f) {
                common.Printf("Couldn't write %s.\n", filename);
                return;
            }
            f.Printf("%s%s", xpkey, CDKEY_TEXT);
            fileSystem.CloseFile(f);
        }

        @Override
        public String GetCDKey(boolean xp) {
            if (!xp) {
                return ctos(cdkey);
            }
            if (xpkey_state == CDKEY_OK || xpkey_state == CDKEY_CHECKING) {
                return ctos(cdkey);
            }
            return null;
        }
        // digits to letters table
        static final String CDKEY_DIGITS = "TWSBJCGD7PA23RLH";

        /*
         ================
         idSessionLocal::CheckKey
         the function will only modify keys to _OK or _CHECKING if the offline checks are passed
         if the function returns false, the offline checks failed, and offline_valid holds which keys are bad
         ================
         */
        @Override
        public boolean CheckKey(String key, boolean netConnect, boolean[] offline_valid) {
            char[][] lkey = new char[2][CDKEY_BUF_LEN];
            char[][] l_chk = new char[2][3];
            char[] s_chk = new char[3];
            int imax, i_key;
            /*unsigned*/ int checksum, chk8;//TODO:bitwise ops on longs!?
            boolean[] edited_key = new boolean[2];

            // make sure have a right input string
            assert (key.length() == (CDKEY_BUF_LEN - 1) * 2 + 4 + 3 + 4);

            edited_key[0] = (key.charAt(0) == '1');
            idStr.Copynz(lkey[0], key + 2, CDKEY_BUF_LEN);
            idStr.ToUpper(lkey[0]);
            idStr.Copynz(l_chk[0], key + CDKEY_BUF_LEN + 2, 3);
            idStr.ToUpper(l_chk[0]);
            edited_key[1] = (key.charAt(CDKEY_BUF_LEN + 2 + 3) == '1');
            idStr.Copynz(lkey[1], key + CDKEY_BUF_LEN + 7, CDKEY_BUF_LEN);
            idStr.ToUpper(lkey[1]);
            idStr.Copynz(l_chk[1], key + CDKEY_BUF_LEN * 2 + 7, 3);
            idStr.ToUpper(l_chk[1]);

            if (fileSystem.HasD3XP()) {
                imax = 2;
            } else {
                imax = 1;
            }
            offline_valid[0] = offline_valid[1] = true;
            for (i_key = 0; i_key < imax; i_key++) {
                // check that the characters are from the valid set
                int i;
                for (i = 0; i < CDKEY_BUF_LEN - 1; i++) {
                    if (-1 == CDKEY_DIGITS.indexOf(lkey[i_key][i])) {
                        offline_valid[i_key] = false;
                        continue;
                    }
                }

                if (edited_key[i_key]) {
                    // verify the checksum for edited keys only
                    checksum = (int) CRC32_BlockChecksum(lkey[i_key], CDKEY_BUF_LEN - 1);
                    chk8 = (checksum & 0xff) ^ (((checksum & 0xff00) >> 8) ^ (((checksum & 0xff0000) >> 16) ^ ((checksum & 0xff000000) >> 24)));
                    idStr.snPrintf(s_chk, 3, "%02X", chk8);
                    if (idStr.Icmp(ctos(l_chk[i_key]), ctos(s_chk)) != 0) {
                        offline_valid[i_key] = false;
                        continue;
                    }
                }
            }

            if (!offline_valid[0] || !offline_valid[1]) {
                return false;
            }

            // offline checks passed, we'll return true and optionally emit key check requests
            // the function should only modify the key states if the offline checks passed successfully
            // set the keys, don't send a game auth if we are net connecting
            idStr.Copynz(cdkey, lkey[0], CDKEY_BUF_LEN);
            cdkey_state = netConnect ? CDKEY_OK : CDKEY_CHECKING;
            if (fileSystem.HasD3XP()) {
                idStr.Copynz(xpkey, lkey[1], CDKEY_BUF_LEN);
                xpkey_state = netConnect ? CDKEY_OK : CDKEY_CHECKING;
            } else {
                xpkey_state = CDKEY_NA;
            }
            if (!netConnect) {
                EmitGameAuth();
            }
            SetCDKeyGuiVars();

            return true;
        }

        /*
         ===============
         idSessionLocal::CDKeysAreValid
         checking that the key is present and uses only valid characters
         if d3xp is installed, check for a valid xpkey as well
         emit an auth packet to the master if possible and needed
         ===============
         */
        @Override
        public boolean CDKeysAreValid(boolean strict) {
            int i;
            boolean emitAuth = false;

            if (cdkey_state == CDKEY_UNKNOWN) {
                if (cdkey.length != CDKEY_BUF_LEN - 1) {
                    cdkey_state = CDKEY_INVALID;
                } else {
                    for (i = 0; i < CDKEY_BUF_LEN - 1; i++) {
                        if (-1 == CDKEY_DIGITS.indexOf(cdkey[i])) {
                            cdkey_state = CDKEY_INVALID;
                            break;
                        }
                    }
                }
                if (cdkey_state == CDKEY_UNKNOWN) {
                    cdkey_state = CDKEY_CHECKING;
                    emitAuth = true;
                }
            }
            if (xpkey_state == CDKEY_UNKNOWN) {
                if (fileSystem.HasD3XP()) {
                    if (ctos(xpkey).length() != CDKEY_BUF_LEN - 1) {
                        xpkey_state = CDKEY_INVALID;
                    } else {
                        for (i = 0; i < CDKEY_BUF_LEN - 1; i++) {
                            if (-1 == CDKEY_DIGITS.indexOf(xpkey[i])) {
                                xpkey_state = CDKEY_INVALID;
                            }
                        }
                    }
                    if (xpkey_state == CDKEY_UNKNOWN) {
                        xpkey_state = CDKEY_CHECKING;
                        emitAuth = true;
                    }
                } else {
                    xpkey_state = CDKEY_NA;
                }
            }
            if (emitAuth) {
                EmitGameAuth();
            }
            // make sure to keep the mainmenu gui up to date in case we made state changes
            SetCDKeyGuiVars();
            if (strict) {
                return cdkey_state == CDKEY_OK && (xpkey_state == CDKEY_OK || xpkey_state == CDKEY_NA);
            } else {
                return (cdkey_state == CDKEY_OK || cdkey_state == CDKEY_CHECKING) && (xpkey_state == CDKEY_OK || xpkey_state == CDKEY_CHECKING || xpkey_state == CDKEY_NA);
            }
        }

        @Override
        public void ClearCDKey(boolean[] valid) {
            if (!valid[0]) {
//		memset( cdkey, 0, CDKEY_BUF_LEN );
                Arrays.fill(cdkey, '0');//TODO:is '0' the same as 0????
                cdkey_state = CDKEY_UNKNOWN;
            } else if (cdkey_state == CDKEY_CHECKING) {
                // if a key was in checking and not explicitely asked for clearing, put it back to ok
                cdkey_state = CDKEY_OK;
            }
            if (!valid[1]) {
//		memset( xpkey, 0, CDKEY_BUF_LEN );
                Arrays.fill(cdkey, '0');
                xpkey_state = CDKEY_UNKNOWN;
            } else if (xpkey_state == CDKEY_CHECKING) {
                xpkey_state = CDKEY_OK;
            }
            WriteCDKey();
        }

        @Override
        public void SetCDKeyGuiVars() {
            if (NOT(guiMainMenu)) {
                return;
            }
            guiMainMenu.SetStateString("str_d3key_state", common.GetLanguageDict().GetString(va("#str_071%d", 86 + cdkey_state.ordinal())));
            guiMainMenu.SetStateString("str_xpkey_state", common.GetLanguageDict().GetString(va("#str_071%d", 86 + xpkey_state.ordinal())));
        }

        @Override
        public boolean WaitingForGameAuth() {
            return authEmitTimeout != 0;
        }

        @Override
        public void CDKeysAuthReply(boolean valid, String auth_msg) {
            assert (authEmitTimeout > 0);
            if (authWaitBox) {
                // close the wait box
                StopBox();
                authWaitBox = false;
            }
            if (!valid) {
                common.DPrintf("auth key is invalid\n");
                authMsg = new idStr(auth_msg);
                if (cdkey_state == CDKEY_CHECKING) {
                    cdkey_state = CDKEY_INVALID;
                }
                if (xpkey_state == CDKEY_CHECKING) {
                    xpkey_state = CDKEY_INVALID;
                }
            } else {
                common.DPrintf("client is authed in\n");
                if (cdkey_state == CDKEY_CHECKING) {
                    cdkey_state = CDKEY_OK;
                }
                if (xpkey_state == CDKEY_CHECKING) {
                    xpkey_state = CDKEY_OK;
                }
            }
            authEmitTimeout = 0;
            SetCDKeyGuiVars();
        }

        @Override
        public String GetCurrentMapName() {
            return currentMapName.toString();
        }

        @Override
        public int GetSaveGameVersion() {
            return savegameVersion;
        }
//        
//        
//        
//        
        //=====================================
//

        public int GetLocalClientNum() {
            if (idAsyncNetwork.client.IsActive()) {
                return idAsyncNetwork.client.GetLocalClientNum();
            } else if (idAsyncNetwork.server.IsActive()) {
                if (idAsyncNetwork.serverDedicated.GetInteger() == 0) {
                    return 0;
                } else if (idAsyncNetwork.server.IsClientInGame(idAsyncNetwork.serverDrawClient.GetInteger())) {
                    return idAsyncNetwork.serverDrawClient.GetInteger();
                } else {
                    return -1;
                }
            } else {
                return 0;
            }
        }

        /*
         ===============
         idSessionLocal::MoveToNewMap

         Leaves the existing userinfo and serverinfo
         ===============
         */
        public void MoveToNewMap(final String mapName) {
            mapSpawnData.serverInfo.Set("si_map", mapName);

            ExecuteMapChange();

            if (!mapSpawnData.serverInfo.GetBool("devmap")) {
                // Autosave at the beginning of the level
                SaveGame(GetAutoSaveName(mapName), true);
            }

            SetGUI(null, null);
        }

        public void StartNewGame(final String mapName) {
            StartNewGame(mapName, false);
        }
        // loads a map and starts a new game on it

        public void StartNewGame(final String mapName, boolean devmap/*= false*/) {
            if (ID_DEDICATED) {
                common.Printf("Dedicated servers cannot start singleplayer games.\n");
                return;
            } else {
                if (ID_ENFORCE_KEY) {
                    // strict check. don't let a game start without a definitive answer
                    if (!CDKeysAreValid(true)) {
                        boolean prompt = true;
                        if (MaybeWaitOnCDKey()) {
                            // check again, maybe we just needed more time
                            if (CDKeysAreValid(true)) {
                                // can continue directly
                                prompt = false;
                            }
                        }
                        if (prompt) {
                            cmdSystem.BufferCommandText(CMD_EXEC_NOW, "promptKey force");
                            cmdSystem.ExecuteCommandBuffer();
                        }
                    }
                }
                if (idAsyncNetwork.server.IsActive()) {
                    common.Printf("Server running, use si_map / serverMapRestart\n");
                    return;
                }
                if (idAsyncNetwork.client.IsActive()) {
                    common.Printf("Client running, disconnect from server first\n");
                    return;
                }

                // clear the userInfo so the player starts out with the defaults
                mapSpawnData.userInfo[0].Clear();
                mapSpawnData.persistentPlayerInfo[0].Clear();
                mapSpawnData.userInfo[0] = cvarSystem.MoveCVarsToDict(CVAR_USERINFO);

                mapSpawnData.serverInfo.Clear();
                mapSpawnData.serverInfo = cvarSystem.MoveCVarsToDict(CVAR_SERVERINFO);
                mapSpawnData.serverInfo.Set("si_gameType", "singleplayer");

                // set the devmap key so any play testing items will be given at
                // spawn time to set approximately the right weapons and ammo
                if (devmap) {
                    mapSpawnData.serverInfo.Set("devmap", "1");
                }

                mapSpawnData.syncedCVars.Clear();
                mapSpawnData.syncedCVars = cvarSystem.MoveCVarsToDict(CVAR_NETWORKSYNC);

                MoveToNewMap(mapName);
            }
        }

//        public void PlayIntroGui();
//
//
//        public void LoadSession(final String name);
//
//        public void SaveSession(final String name);
//

        /*
         ===============
         idSessionLocal::DrawWipeModel

         Draw the fade material over everything that has been drawn
         ===============
         */
        // called by Draw when the scene to scene wipe is still running
        public void DrawWipeModel() {
            int latchedTic = com_ticNumber;

            if (wipeStartTic >= wipeStopTic) {
                return;
            }

            if (!wipeHold && latchedTic >= wipeStopTic) {
                return;
            }

            float fade = (float) (latchedTic - wipeStartTic) / (wipeStopTic - wipeStartTic);
            renderSystem.SetColor4(1, 1, 1, fade);
            renderSystem.DrawStretchPic(0, 0, 640, 480, 0, 0, 1, 1, wipeMaterial);
        }

        /*
         ================
         idSessionLocal::StartWipe

         Draws and captures the current state, then starts a wipe with that image
         ================
         */
        public void StartWipe(final String _wipeMaterial) {
            this.StartWipe(_wipeMaterial, false);
        }

        public void StartWipe(final String _wipeMaterial, boolean hold/*= false*/) {
            console.Close();

            // render the current screen into a texture for the wipe model
            renderSystem.CropRenderSize(640, 480, true);

            Draw();

            renderSystem.CaptureRenderToImage("_scratch");
            renderSystem.UnCrop();

            wipeMaterial = declManager.FindMaterial(_wipeMaterial, false);

            wipeStartTic = com_ticNumber;
            wipeStopTic = (int) (wipeStartTic + 1000.0f / USERCMD_MSEC * com_wipeSeconds.GetFloat());
            wipeHold = hold;
        }

        public void CompleteWipe() {
            if (com_ticNumber == 0) {
                // if the async thread hasn't started, we would hang here
                wipeStopTic = 0;
                UpdateScreen(true);
                return;
            }
            while (com_ticNumber < wipeStopTic) {
                if (ID_CONSOLE_LOCK) {
                    emptyDrawCount = 0;
                }
                UpdateScreen(true);
            }
        }

        public void ClearWipe() {
            wipeHold = false;
            wipeStopTic = 0;
            wipeStartTic = wipeStopTic + 1;
        }

        public void ShowLoadingGui() {
            if (com_ticNumber == 0) {
                return;
            }
            console.Close();

            // introduced in D3XP code. don't think it actually fixes anything, but doesn't hurt either
            if (true) {
                // Try and prevent the while loop from being skipped over (long hitch on the main thread?)
                int stop = Sys_Milliseconds() + 1000;
                int force = 10;
                while (Sys_Milliseconds() < stop || force-- > 0) {
                    com_frameTime = com_ticNumber * USERCMD_MSEC;
                    session.Frame();
                    session.UpdateScreen(false);
                }
            } else {
                final int stop = (int) (com_ticNumber + 1000.0f / USERCMD_MSEC * 1.0f);
                while (com_ticNumber < stop) {
                    com_frameTime = com_ticNumber * USERCMD_MSEC;
                    session.Frame();
                    session.UpdateScreen(false);
                }
            }
        }

        /*
         ===============
         idSessionLocal::ScrubSaveGameFileName

         Turns a bad file name into a good one or your money back
         ===============
         */
        public void ScrubSaveGameFileName(idStr saveFileName) {
            int i;
            idStr inFileName;

            inFileName = new idStr(saveFileName);
            inFileName.RemoveColors();
            inFileName.StripFileExtension();

            saveFileName.Clear();

            int len = inFileName.Length();
            for (i = 0; i < len; i++) {
                if ("',.~!@#$%^&*()[]{}<>\\|/=?+;:-\'\"".indexOf(inFileName.oGet(i)) > -1) {
                    // random junk
                    saveFileName.Append('_');
                } else if (inFileName.oGet(i) >= 128) {
                    // high ascii chars
                    saveFileName.Append('_');
                } else if (inFileName.oGet(i) == ' ') {
                    saveFileName.Append('_');
                } else {
                    saveFileName.Append(inFileName.oGet(i));
                }
            }
        }

        public String GetAutoSaveName(String mapName) throws idException {
            final idDecl mapDecl = declManager.FindType(DECL_MAPDEF, mapName, false);
            final idDeclEntityDef mapDef = (idDeclEntityDef) mapDecl;
            if (mapDef != null) {
                mapName = common.GetLanguageDict().GetString(mapDef.dict.GetString("name", mapName));
            }
            // Fixme: Localization
            return va("^3AutoSave:^0 %s", mapName);
        }

        public boolean LoadGame(final String saveName) throws idException {
            if (ID_DEDICATED) {
                common.Printf("Dedicated servers cannot load games.\n");
                return false;
            } else {
                int i;
                idStr in, loadFile, saveMap = new idStr(), gamename = new idStr();

                if (IsMultiplayer()) {
                    common.Printf("Can't load during net play.\n");
                    return false;
                }

                //Hide the dialog box if it is up.
                StopBox();

                loadFile = new idStr(saveName);
                ScrubSaveGameFileName(loadFile);
                loadFile.SetFileExtension(".save");

                in = new idStr("savegames/");
                in.Append(loadFile);

                // Open savegame file
                // only allow loads from the game directory because we don't want a base game to load
                idStr game = new idStr(cvarSystem.GetCVarString("fs_game"));
                savegameFile = fileSystem.OpenFileRead(in.toString(), true, game.Length() != 0 ? game.toString() : null);

                if (savegameFile == null) {
                    common.Warning("Couldn't open savegame file %s", in.toString());
                    return false;
                }

                loadingSaveGame = true;

                // Read in save game header
                // Game Name / Version / Map Name / Persistant Player Info
                // game
                savegameFile.ReadString(gamename);

                // if this isn't a savegame for the correct game, abort loadgame
                if (!gamename.toString().equals(GAME_NAME)) {
                    common.Warning("Attempted to load an invalid savegame: %s", in.toString());

                    loadingSaveGame = false;
                    fileSystem.CloseFile(savegameFile);
                    savegameFile = null;
                    return false;
                }

                {// version
                    int[] savegameVersion = {0};
                    savegameFile.ReadInt(savegameVersion);
                    this.savegameVersion = savegameVersion[0];
                }

                // map
                savegameFile.ReadString(saveMap);

                // persistent player info
                for (i = 0; i < MAX_ASYNC_CLIENTS; i++) {
                    mapSpawnData.persistentPlayerInfo[i].ReadFromFileHandle(savegameFile);
                }

                // check the version, if it doesn't match, cancel the loadgame,
                // but still load the map with the persistant playerInfo from the header
                // so that the player doesn't lose too much progress.
                if (savegameVersion != SAVEGAME_VERSION
                        && !(savegameVersion == 16 && SAVEGAME_VERSION == 17)) {	// handle savegame v16 in v17
                    common.Warning("Savegame Version mismatch: aborting loadgame and starting level with persistent data");
                    loadingSaveGame = false;
                    fileSystem.CloseFile(savegameFile);
                    savegameFile = null;
                }

                common.DPrintf("loading a v%d savegame\n", savegameVersion);

                if (saveMap.Length() > 0) {

                    // Start loading map
                    mapSpawnData.serverInfo.Clear();

                    mapSpawnData.serverInfo = cvarSystem.MoveCVarsToDict(CVAR_SERVERINFO);
                    mapSpawnData.serverInfo.Set("si_gameType", "singleplayer");

                    mapSpawnData.serverInfo.Set("si_map", saveMap.toString());

                    mapSpawnData.syncedCVars.Clear();
                    mapSpawnData.syncedCVars = cvarSystem.MoveCVarsToDict(CVAR_NETWORKSYNC);

                    mapSpawnData.mapSpawnUsercmd[0] = usercmdGen.TicCmd(latchedTicNumber);
                    // make sure no buttons are pressed
                    mapSpawnData.mapSpawnUsercmd[0].buttons = 0;

                    ExecuteMapChange();

                    SetGUI(null, null);
                }

                if (loadingSaveGame) {
                    fileSystem.CloseFile(savegameFile);
                    loadingSaveGame = false;
                    savegameFile = null;
                }

                return true;
            }
        }

        public boolean SaveGame(final String saveName, boolean autosave /*= false*/) throws idException {
            return false;//HACKME::8
//            if (ID_DEDICATED) {
//                common.Printf("Dedicated servers cannot save games.\n");
//                return false;
//            } else {
//                int i;
//                idStr gameFile, previewFile, descriptionFile;
//                String mapName;
//
//                if (!mapSpawned) {
//                    common.Printf("Not playing a game.\n");
//                    return false;
//                }
//
//                if (IsMultiplayer()) {
//                    common.Printf("Can't save during net play.\n");
//                    return false;
//                }
//
//                if (game.GetPersistentPlayerInfo(0).GetInt("health") <= 0) {
//                    MessageBox(MSG_OK, common.GetLanguageDict().GetString("#str_04311"), common.GetLanguageDict().GetString("#str_04312"), true);
//                    common.Printf("You must be alive to save the game\n");
//                    return false;
//                }
//
//                if (Sys_GetDriveFreeSpace(cvarSystem.GetCVarString("fs_savepath")) < 25) {
//                    MessageBox(MSG_OK, common.GetLanguageDict().GetString("#str_04313"), common.GetLanguageDict().GetString("#str_04314"), true);
//                    common.Printf("Not enough drive space to save the game\n");
//                    return false;
//                }
//
//                idSoundWorld pauseWorld = soundSystem.GetPlayingSoundWorld();
//                if (pauseWorld != null) {
//                    pauseWorld.Pause();
//                    soundSystem.SetPlayingSoundWorld(null);
//                }
//
//                // setup up filenames and paths
//                gameFile = new idStr(saveName);
//                ScrubSaveGameFileName(gameFile);
//
//                gameFile = new idStr("savegames/" + gameFile);
//                gameFile.SetFileExtension(".save");
//
//                previewFile = new idStr(gameFile);
//                previewFile.SetFileExtension(".tga");
//
//                descriptionFile = new idStr(gameFile);
//                descriptionFile.SetFileExtension(".txt");
//
//                // Open savegame file
//                idFile fileOut = fileSystem.OpenFileWrite(gameFile.toString());
//                if (fileOut == null) {
//                    common.Warning("Failed to open save file '%s'\n", gameFile.toString());
//                    if (pauseWorld != null) {
//                        soundSystem.SetPlayingSoundWorld(pauseWorld);
//                        pauseWorld.UnPause();
//                    }
//                    return false;
//                }
//
//                // Write SaveGame Header:
//                // Game Name / Version / Map Name / Persistant Player Info
//                // game
//                final String gamename = GAME_NAME;
//                fileOut.WriteString(gamename);
//
//                // version
//                fileOut.WriteInt(SAVEGAME_VERSION);
//
//                // map
//                mapName = mapSpawnData.serverInfo.GetString("si_map");
//                fileOut.WriteString(mapName);
//
//                // persistent player info
//                for (i = 0; i < MAX_ASYNC_CLIENTS; i++) {
//                    mapSpawnData.persistentPlayerInfo[i] = game.GetPersistentPlayerInfo(i);
//                    mapSpawnData.persistentPlayerInfo[i].WriteToFileHandle(fileOut);
//                }
//
//                // let the game save its state
//                game.SaveGame(fileOut);
//
//                // close the sava game file
//                fileSystem.CloseFile(fileOut);
//
//                // Write screenshot
//                if (!autosave) {
//                    renderSystem.CropRenderSize(320, 240, false);
//                    game.Draw(0);
//                    renderSystem.CaptureRenderToFile(previewFile.toString(), true);
//                    renderSystem.UnCrop();
//                }
//
//                // Write description, which is just a text file with
//                // the unclean save name on line 1, map name on line 2, screenshot on line 3
//                idFile fileDesc = fileSystem.OpenFileWrite(descriptionFile.toString());
//                if (fileDesc == null) {
//                    common.Warning("Failed to open description file '%s'\n", descriptionFile);
//                    if (pauseWorld != null) {
//                        soundSystem.SetPlayingSoundWorld(pauseWorld);
//                        pauseWorld.UnPause();
//                    }
//                    return false;
//                }
//
//                idStr description = new idStr(saveName);
//                description.Replace("\\", "\\\\");
//                description.Replace("\"", "\\\"");
//
//                final idDeclEntityDef mapDef = (idDeclEntityDef) declManager.FindType(DECL_MAPDEF, mapName, false);
//                if (mapDef != null) {
//                    mapName = common.GetLanguageDict().GetString(mapDef.dict.GetString("name", mapName));
//                }
//
//                fileDesc.Printf("\"%s\"\n", description);
//                fileDesc.Printf("\"%s\"\n", mapName);
//
//                if (autosave) {
//                    idStr sshot = new idStr(mapSpawnData.serverInfo.GetString("si_map"));
//                    sshot.StripPath();
//                    sshot.StripFileExtension();
//                    fileDesc.Printf("\"guis/assets/autosave/%s\"\n", sshot.toString());
//                } else {
//                    fileDesc.Printf("\"\"\n");
//                }
//
//                fileSystem.CloseFile(fileDesc);
//
//                if (pauseWorld != null) {
//                    soundSystem.SetPlayingSoundWorld(pauseWorld);
//                    pauseWorld.UnPause();
//                }
//
//                syncNextGameFrame = true;
//
//                return true;
//            }
        }

        public boolean SaveGame(final String saveName) throws idException {
            return SaveGame(saveName, false);

        }

        public String GetAuthMsg() {
            return authMsg.toString();
        }

        public void Clear() {

            insideUpdateScreen = false;
            insideExecuteMapChange = false;

            loadingSaveGame = false;
            savegameFile = null;
            savegameVersion = 0;

            currentMapName.Clear();
            aviDemoShortName.Clear();
            msgFireBack[0].Clear();
            msgFireBack[1].Clear();

            timeHitch = 0;

            rw = null;
            sw = null;
            menuSoundWorld = null;
            readDemo = null;
            writeDemo = null;
            renderdemoVersion = 0;
            cmdDemoFile = null;

            syncNextGameFrame = false;
            mapSpawned = false;
            guiActive = null;
            aviCaptureMode = false;
            timeDemo = TD_NO;
            waitingOnBind = false;
            lastPacifierTime = 0;

            msgRunning = false;
            guiMsgRestore = null;
            msgIgnoreButtons = false;

            bytesNeededForMapLoad = 0;

            if (ID_CONSOLE_LOCK) {
                emptyDrawCount = 0;
            }
            ClearWipe();

            loadGameList.Clear();
            modsList.Clear();

            authEmitTimeout = 0;
            authWaitBox = false;

            authMsg.Clear();
        }
//
        static final int ANGLE_GRAPH_HEIGHT = 128;
        static final int ANGLE_GRAPH_STRETCH = 3;

        /*
         ===============
         idSessionLocal::DrawCmdGraph

         Graphs yaw angle for testing smoothness
         ===============
         */
        public void DrawCmdGraph() throws idException {
            if (!com_showAngles.GetBool()) {
                return;
            }
            renderSystem.SetColor4(0.1f, 0.1f, 0.1f, 1.0f);
            renderSystem.DrawStretchPic(0, 480 - ANGLE_GRAPH_HEIGHT, MAX_BUFFERED_USERCMD * ANGLE_GRAPH_STRETCH, ANGLE_GRAPH_HEIGHT, 0, 0, 1, 1, whiteMaterial);
            renderSystem.SetColor4(0.9f, 0.9f, 0.9f, 1.0f);
            for (int i = 0; i < MAX_BUFFERED_USERCMD - 4; i++) {
                usercmd_t cmd = usercmdGen.TicCmd(latchedTicNumber - (MAX_BUFFERED_USERCMD - 4) + i);
                int h = cmd.angles[1];
                h >>= 8;
                h &= (ANGLE_GRAPH_HEIGHT - 1);
                renderSystem.DrawStretchPic(i * ANGLE_GRAPH_STRETCH, 480 - h, 1, h, 0, 0, 1, 1, whiteMaterial);
            }
        }

        static int DBG_Draw=0;
        public void Draw() throws idException {
            boolean fullConsole = false;

            if (insideExecuteMapChange) {
                if (guiLoading != null) {
                    guiLoading.Redraw(com_frameTime);
                }
                if (guiActive == guiMsg) {
                    guiMsg.Redraw(com_frameTime);
                }
            } else if (guiTest != null) {
                // if testing a gui, clear the screen and draw it
                // clear the background, in case the tested gui is transparent
                // NOTE that you can't use this for aviGame recording, it will tick at real com_frameTime between screenshots..
                renderSystem.SetColor(colorBlack);
                renderSystem.DrawStretchPic(0, 0, 640, 480, 0, 0, 1, 1, declManager.FindMaterial("_white"));
                guiTest.Redraw(com_frameTime);
            } else if (guiActive != null && !guiActive.State().GetBool("gameDraw")) {

                // draw the frozen gui in the background
                if (guiActive == guiMsg && guiMsgRestore != null) {
                    guiMsgRestore.Redraw(com_frameTime);
                }

                // draw the menus full screen
                if (guiActive == guiTakeNotes && !com_skipGameDraw.GetBool()) {
                    game.Draw(GetLocalClientNum());
                }

                DBG_Draw++;
                guiActive.Redraw(com_frameTime);
            } else if (readDemo != null) {
                rw.RenderScene(currentDemoRenderView);
                renderSystem.DrawDemoPics();
            } else if (mapSpawned) {
                boolean gameDraw = false;
                // normal drawing for both single and multi player
                if (!com_skipGameDraw.GetBool() && GetLocalClientNum() >= 0) {
                    // draw the game view
                    int start = Sys_Milliseconds();
                    gameDraw = game.Draw(GetLocalClientNum());
                    int end = Sys_Milliseconds();
                    time_gameDraw += (end - start);	// note time used for com_speeds
                }
                if (!gameDraw) {
                    renderSystem.SetColor(colorBlack);
                    renderSystem.DrawStretchPic(0, 0, 640, 480, 0, 0, 1, 1, declManager.FindMaterial("_white"));
                }

                // save off the 2D drawing from the game
                if (writeDemo != null) {
                    renderSystem.WriteDemoPics();
                }
            } else {
                if (ID_CONSOLE_LOCK) {
                    if (com_allowConsole.GetBool()) {
                        console.Draw(true);
                    } else {
                        emptyDrawCount++;
                        if (emptyDrawCount > 5) {
                            // it's best if you can avoid triggering the watchgod by doing the right thing somewhere else
                            assert (false);
                            common.Warning("idSession: triggering mainmenu watchdog");
                            emptyDrawCount = 0;
                            StartMenu();
                        }
                        renderSystem.SetColor4(0, 0, 0, 1);
                        renderSystem.DrawStretchPic(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT, 0, 0, 1, 1, declManager.FindMaterial("_white"));
                    }
                } else {
                    // draw the console full screen - this should only ever happen in developer builds
                    console.Draw(true);
                }
                fullConsole = true;
            }

            if (ID_CONSOLE_LOCK) {
                if (!fullConsole && emptyDrawCount != 0) {
                    common.DPrintf("idSession: %d empty frame draws\n", emptyDrawCount);
                    emptyDrawCount = 0;
                }
                fullConsole = false;
            }

            // draw the wipe material on top of this if it hasn't completed yet
            DrawWipeModel();

            // draw debug graphs
            DrawCmdGraph();

            // draw the half console / notify console on top of everything
            if (!fullConsole) {
                console.Draw(false);
            }
        }


        /*
         ==============
         idSessionLocal::WriteCmdDemo

         Dumps the accumulated commands for the current level.
         This should still work after disconnecting from a level
         ==============
         */
        public void WriteCmdDemo(final String demoName, boolean save /*= false*/) throws idException {

            if (demoName.isEmpty()) {
                common.Printf("idSessionLocal::WriteCmdDemo: no name specified\n");
                return;
            }

            idStr statsName = new idStr();
            if (save) {
                statsName = new idStr(demoName);
                statsName.StripFileExtension();
                statsName.DefaultFileExtension(".stats");
            }

            common.Printf("writing save data to %s\n", demoName);

            idFile cmdDemoFile = fileSystem.OpenFileWrite(demoName);
            if (null == cmdDemoFile) {
                common.Printf("Couldn't open for writing %s\n", demoName);
                return;
            }

            if (save) {

                cmdDemoFile.WriteInt(logIndex);//cmdDemoFile->Write( &logIndex, sizeof( logIndex ) );//TODO
            }

            SaveCmdDemoToFile(cmdDemoFile);

            if (save) {
                idFile statsFile = fileSystem.OpenFileWrite(statsName.toString());
                if (statsFile != null) {
                    statsFile.WriteInt(statIndex);//statsFile->Write( &statIndex, sizeof( statIndex ) );//TODO
                    for (int i = 0; i < numClients * statIndex; i++) {
                        statsFile.Write(loggedStats[i].Write());
                    }
                    fileSystem.CloseFile(statsFile);
                }
            }

            fileSystem.CloseFile(cmdDemoFile);
        }

        public void WriteCmdDemo(final String demoName) throws idException {
            WriteCmdDemo(demoName, false);
        }

        public void StartPlayingCmdDemo(final String demoName) throws idException {
            // exit any current game
            Stop();

            idStr fullDemoName = new idStr("demos/");
            fullDemoName.Append(demoName);
            fullDemoName.DefaultFileExtension(".cdemo");
            cmdDemoFile = fileSystem.OpenFileRead(fullDemoName.toString());

            if (cmdDemoFile == null) {
                common.Printf("Couldn't open %s\n", fullDemoName.toString());
                return;
            }

            guiLoading = uiManager.FindGui("guis/map/loading.gui", true, false, true);
            //cmdDemoFile.Read(&loadGameTime, sizeof(loadGameTime));

            LoadCmdDemoFromFile(cmdDemoFile);

            // start the map
            ExecuteMapChange();

            cmdDemoFile = fileSystem.OpenFileRead(fullDemoName.toString());

            // have to do this twice as the execmapchange clears the cmddemofile
            LoadCmdDemoFromFile(cmdDemoFile);

            // run one frame to get the view angles correct
            RunGameTic();
        }

        public void TimeCmdDemo(final String demoName) throws idException {
            StartPlayingCmdDemo(demoName);
            ClearWipe();
            UpdateScreen();

            int startTime = Sys_Milliseconds();
            int count = 0;
            int minuteStart, minuteEnd;
            float sec;

            // run all the frames in sequence
            minuteStart = startTime;

            while (cmdDemoFile != null) {
                RunGameTic();
                count++;

                if (count / 3600 != (count - 1) / 3600) {
                    minuteEnd = Sys_Milliseconds();
                    sec = (float) ((minuteEnd - minuteStart) / 1000.0);//divide by double and roundup to float
                    minuteStart = minuteEnd;
                    common.Printf("minute %d took %3.1f seconds\n", count / 3600, sec);
                    UpdateScreen();
                }
            }

            int endTime = Sys_Milliseconds();
            sec = (float) ((endTime - startTime) / 1000.0);
            common.Printf("%d seconds of game, replayed in %5.1f seconds\n", count / 60, sec);
        }

        public void SaveCmdDemoToFile(idFile file) throws idException {

            mapSpawnData.serverInfo.WriteToFileHandle(file);

            for (int i = 0; i < MAX_ASYNC_CLIENTS; i++) {
                mapSpawnData.userInfo[i].WriteToFileHandle(file);
                mapSpawnData.persistentPlayerInfo[i].WriteToFileHandle(file);
            }

            for (usercmd_t t : mapSpawnData.mapSpawnUsercmd) {
                file.Write(t.Write()/*, sizeof( mapSpawnData.mapSpawnUsercmd )*/);
            }

            if (numClients < 1) {
                numClients = 1;
            }
            for (int i = 0; i < numClients * logIndex; i++) {
                file.Write(loggedUsercmds[i].Write() /* sizeof(loggedUsercmds[0])*/);
            }
        }

        public void LoadCmdDemoFromFile(idFile file) throws idException {

            mapSpawnData.serverInfo.ReadFromFileHandle(file);

            for (int i = 0; i < MAX_ASYNC_CLIENTS; i++) {
                mapSpawnData.userInfo[i].ReadFromFileHandle(file);
                mapSpawnData.persistentPlayerInfo[i].ReadFromFileHandle(file);
            }
            for (usercmd_t t : mapSpawnData.mapSpawnUsercmd) {
                file.Read(t.Write()/*, sizeof( mapSpawnData.mapSpawnUsercmd )*/);
            }
        }

        public void StartRecordingRenderDemo(final String demoName) {
            if (writeDemo != null) {
                // allow it to act like a toggle
                StopRecordingRenderDemo();
                return;
            }

            if (isNotNullOrEmpty(demoName)) {
                common.Printf("idSessionLocal::StartRecordingRenderDemo: no name specified\n");
                return;
            }

            console.Close();

            writeDemo = new idDemoFile();
            if (!writeDemo.OpenForWriting(demoName)) {
                common.Printf("error opening %s\n", demoName);
//		delete writeDemo;
                writeDemo = null;
                return;
            }

            common.Printf("recording to %s\n", writeDemo.GetName());

            writeDemo.WriteInt(DS_VERSION.ordinal());
            writeDemo.WriteInt(RENDERDEMO_VERSION);

            // if we are in a map already, dump the current state
            sw.StartWritingDemo(writeDemo);
            rw.StartWritingDemo(writeDemo);
        }

        public void StopRecordingRenderDemo() {
            if (NOT(writeDemo)) {
                common.Printf("idSessionLocal::StopRecordingRenderDemo: not recording\n");
                return;
            }
            sw.StopWritingDemo();
            rw.StopWritingDemo();

            writeDemo.Close();
            common.Printf("stopped recording %s.\n", writeDemo.GetName());
//	delete writeDemo;
            writeDemo = null;
        }

        public void StartPlayingRenderDemo(idStr demoName) throws idException {
            if (isNotNullOrEmpty(demoName)) {
                common.Printf("idSessionLocal::StartPlayingRenderDemo: no name specified\n");
                return;
            }

            // make sure localSound / GUI intro music shuts up
            sw.StopAllSounds();
            sw.PlayShaderDirectly("", 0);
            menuSoundWorld.StopAllSounds();
            menuSoundWorld.PlayShaderDirectly("", 0);

            // exit any current game
            Stop();

            // automatically put the console away
            console.Close();

            // bring up the loading screen manually, since demos won't
            // call ExecuteMapChange()
            guiLoading = uiManager.FindGui("guis/map/loading.gui", true, false, true);
            guiLoading.SetStateString("demo", common.GetLanguageDict().GetString("#str_02087"));
            readDemo = new idDemoFile();
            demoName.DefaultFileExtension(".demo");
            if (!readDemo.OpenForReading(demoName.toString())) {
                common.Printf("couldn't open %s\n", demoName);
//		delete readDemo;
                readDemo = null;
                Stop();
                StartMenu();
                soundSystem.SetMute(false);
                return;
            }

            insideExecuteMapChange = true;
            UpdateScreen();
            insideExecuteMapChange = false;
            guiLoading.SetStateString("demo", "");

            // setup default render demo settings
            // that's default for <= Doom3 v1.1
            renderdemoVersion = 1;
            savegameVersion = 16;

            AdvanceRenderDemo(true);

            numDemoFrames = 1;

            lastDemoTic = -1;
            timeDemoStartTime = Sys_Milliseconds();
        }

        public void StartPlayingRenderDemo(String demoName) throws idException {
            StartPlayingRenderDemo(new idStr(demoName));
        }

        public void StopPlayingRenderDemo() {
            if (NOT(writeDemo)) {
                common.Printf("idSessionLocal::StopRecordingRenderDemo: not recording\n");
                return;
            }
            sw.StopWritingDemo();
            rw.StopWritingDemo();

            writeDemo.Close();
            common.Printf("stopped recording %s.\n", writeDemo.GetName());
//	delete writeDemo;
            writeDemo = null;
        }

        public void CompressDemoFile(final String scheme, final String demoName) {
            idStr fullDemoName = new idStr("demos/");
            fullDemoName.Append(demoName);
            fullDemoName.DefaultFileExtension(".demo");
            idStr compressedName = fullDemoName;
            compressedName.StripFileExtension();
            compressedName.Append("_compressed.demo");

            int savedCompression = cvarSystem.GetCVarInteger("com_compressDemos");
            boolean savedPreload = cvarSystem.GetCVarBool("com_preloadDemos");
            cvarSystem.SetCVarBool("com_preloadDemos", false);
            cvarSystem.SetCVarInteger("com_compressDemos", Integer.parseInt(scheme));

            idDemoFile demoread = new idDemoFile(), demowrite = new idDemoFile();
            if (!demoread.OpenForReading(fullDemoName.toString())) {
                common.Printf("Could not open %s for reading\n", fullDemoName.toString());
                return;
            }
            if (!demowrite.OpenForWriting(compressedName.toString())) {
                common.Printf("Could not open %s for writing\n", compressedName.toString());
                demoread.Close();
                cvarSystem.SetCVarBool("com_preloadDemos", savedPreload);
                cvarSystem.SetCVarInteger("com_compressDemos", savedCompression);
                return;
            }
            common.SetRefreshOnPrint(true);
            common.Printf("Compressing %s to %s...\n", fullDemoName, compressedName);

            ByteBuffer buffer = ByteBuffer.allocate(bufferSize * 2);
            int bytesRead;
            while (0 != (bytesRead = demoread.Read(buffer))) {
                demowrite.Write(buffer, bytesRead);
                common.Printf(".");
            }

            demoread.Close();
            demowrite.Close();

            cvarSystem.SetCVarBool("com_preloadDemos", savedPreload);
            cvarSystem.SetCVarInteger("com_compressDemos", savedCompression);

            common.Printf("Done\n");
            common.SetRefreshOnPrint(false);

        }
        static final int bufferSize = 65535;

        public void TimeRenderDemo(final String demoName, boolean twice /*= false*/) throws idException {
            idStr demo = new idStr(demoName);

            // no sound in time demos
            soundSystem.SetMute(true);

            StartPlayingRenderDemo(demo);

            if (twice && readDemo != null) {
                // cycle through once to precache everything
                guiLoading.SetStateString("demo", common.GetLanguageDict().GetString("#str_04852"));
                guiLoading.StateChanged(com_frameTime);
                while (readDemo != null) {
                    insideExecuteMapChange = true;
                    UpdateScreen();
                    insideExecuteMapChange = false;
                    AdvanceRenderDemo(true);
                }
                guiLoading.SetStateString("demo", "");
                StartPlayingRenderDemo(demo);
            }

            if (null == readDemo) {
                return;
            }

            timeDemo = TD_YES;
        }

        public void TimeRenderDemo(final String demoName) throws idException {
            TimeRenderDemo(demoName, false);
        }

        public void AVIRenderDemo(final String _demoName) {
            idStr demoName = new idStr(_demoName);	// copy off from va() buffer

            StartPlayingRenderDemo(demoName);
            if (null == readDemo) {
                return;
            }

            BeginAVICapture(demoName.toString());

            // I don't understand why I need to do this twice, something
            // strange with the nvidia swapbuffers?
            UpdateScreen();
        }

        public void AVICmdDemo(final String demoName) {
            StartPlayingCmdDemo(demoName);

            BeginAVICapture(demoName);

        }

        /*
         ================
         idSessionLocal::AVIGame

         Start AVI recording the current game session
         ================
         */
        public void AVIGame(final String[] demoName) {
            if (aviCaptureMode) {
                EndAVICapture();
                return;
            }

            if (!mapSpawned) {
                common.Printf("No map spawned.\n");
            }

            if (!isNotNullOrEmpty(demoName[0])) {
                String filename = FindUnusedFileName("demos/game%03i.game");
                demoName[0] = filename;

                // write a one byte stub .game file just so the FindUnusedFileName works,
                fileSystem.WriteFile(demoName[0], atobb(demoName[0]), 1);
            }

            BeginAVICapture(demoName[0]);
        }

        public void BeginAVICapture(final String demoName) {
            idStr name = new idStr(demoName);
            name.ExtractFileBase(aviDemoShortName);
            aviCaptureMode = true;
            aviDemoFrameCount = 0;
            aviTicStart = 0;
            sw.AVIOpen(va("demos/%s/", aviDemoShortName), aviDemoShortName.toString());
        }

        public void EndAVICapture() {
            if (!aviCaptureMode) {
                return;
            }

            sw.AVIClose();

            // write a .roqParam file so the demo can be converted to a roq file
            idFile f = fileSystem.OpenFileWrite(va("demos/%s/%s.roqParam", aviDemoShortName, aviDemoShortName));
            f.Printf("INPUT_DIR demos/%s\n", aviDemoShortName);
            f.Printf("FILENAME demos/%s/%s.RoQ\n", aviDemoShortName, aviDemoShortName);
            f.Printf("\nINPUT\n");
            f.Printf("%s_*.tga [00000-%05i]\n", aviDemoShortName, (int) (aviDemoFrameCount - 1));
            f.Printf("END_INPUT\n");
//	delete f;

            common.Printf("captured %d frames for %s.\n", (int) aviDemoFrameCount, aviDemoShortName);

            aviCaptureMode = false;
        }
//

        public void AdvanceRenderDemo(boolean singleFrameOnly) throws idException {
            if (lastDemoTic == -1) {
                lastDemoTic = latchedTicNumber - 1;
            }

            int skipFrames = 0;

            if (!aviCaptureMode && null == timeDemo && !singleFrameOnly) {
                skipFrames = ((latchedTicNumber - lastDemoTic) / USERCMD_PER_DEMO_FRAME) - 1;
                // never skip too many frames, just let it go into slightly slow motion
                if (skipFrames > 4) {
                    skipFrames = 4;
                }
                lastDemoTic = latchedTicNumber - latchedTicNumber % USERCMD_PER_DEMO_FRAME;
            } else {
                // always advance a single frame with avidemo and timedemo
                lastDemoTic = latchedTicNumber;
            }

            while (skipFrames > -1) {
                int[] ds = {DS_FINISHED.ordinal()};

                readDemo.ReadInt(ds);
                if (ds[0] == DS_FINISHED.ordinal()) {
                    if (numDemoFrames != 1) {
                        // if the demo has a single frame (a demoShot), continuously replay
                        // the renderView that has already been read
                        Stop();
                        StartMenu();
                    }
                    break;
                }
                if (ds[0] == DS_RENDER.ordinal()) {
                    int[] demoTimeOffset = {0};
                    if (rw.ProcessDemoCommand(readDemo, currentDemoRenderView, demoTimeOffset)) {
                        // a view is ready to render
                        skipFrames--;
                        numDemoFrames++;
                    }
                    this.demoTimeOffset = demoTimeOffset[0];
                    continue;
                }
                if (ds[0] == DS_SOUND.ordinal()) {
                    sw.ProcessDemoCommand(readDemo);
                    continue;
                }
                // appears in v1.2, with savegame format 17
                if (ds[0] == DS_VERSION.ordinal()) {
                    int[] renderdemoVersion = {0};
                    readDemo.ReadInt(renderdemoVersion);
                    this.renderdemoVersion = renderdemoVersion[0];
                    common.Printf("reading a v%d render demo\n", renderdemoVersion[0]);
                    // set the savegameVersion to current for render demo paths that share the savegame paths
                    savegameVersion = SAVEGAME_VERSION;
                    continue;
                }
                common.Error("Bad render demo token");
            }

            if (com_showDemo.GetBool()) {
                common.Printf("frame:%d DemoTic:%d latched:%d skip:%d\n", numDemoFrames, lastDemoTic, latchedTicNumber, skipFrames);
            }

        }

        public void RunGameTic() throws idException {
            logCmd_t logCmd = new logCmd_t();
            usercmd_t[] cmd = {null};

            // if we are doing a command demo, read or write from the file
            if (cmdDemoFile != null) {
                if (0 == cmdDemoFile.Read(logCmd/*, sizeof( logCmd )*/)) {
                    common.Printf("Command demo completed at logIndex %d\n", logIndex);
                    fileSystem.CloseFile(cmdDemoFile);
                    cmdDemoFile = null;
                    if (aviCaptureMode) {
                        EndAVICapture();
                        Shutdown();
                    }
                    // we fall out of the demo to normal commands
                    // the impulse and chat character toggles may not be correct, and the view
                    // angle will definitely be wrong
                } else {
                    cmd[0] = logCmd.cmd;
                    cmd[0].ByteSwap();
                    logCmd.consistencyHash = LittleLong(logCmd.consistencyHash);
                }
            }

            // if we didn't get one from the file, get it locally
            if (null == cmdDemoFile) {
                // get a locally created command
                if (com_asyncInput.GetBool()) {
                    cmd[0] = usercmdGen.TicCmd(lastGameTic);
                } else {
                    cmd[0] = usercmdGen.GetDirectUsercmd();
                }
                lastGameTic++;
            }

            // run the game logic every player move
            int start = Sys_Milliseconds();
            gameReturn_t ret = game.RunFrame(cmd);

            int end = Sys_Milliseconds();
            time_gameFrame += end - start;	// note time used for com_speeds

            // check for constency failure from a recorded command
            if (cmdDemoFile != null) {
                if (ret.consistencyHash != logCmd.consistencyHash) {
                    common.Printf("Consistency failure on logIndex %d\n", logIndex);
                    Stop();
                    return;
                }
            }

            // save the cmd for cmdDemo archiving
            if (logIndex < MAX_LOGGED_USERCMDS) {
                loggedUsercmds[logIndex].cmd = cmd[0];
                // save the consistencyHash for demo playback verification
                loggedUsercmds[logIndex].consistencyHash = ret.consistencyHash;
                if (logIndex % 30 == 0 && statIndex < MAX_LOGGED_STATS) {
                    loggedStats[statIndex].health = ret.health;
                    loggedStats[statIndex].heartRate = ret.heartRate;
                    loggedStats[statIndex].stamina = ret.stamina;
                    loggedStats[statIndex].combat = ret.combat;
                    statIndex++;
                }
                logIndex++;
            }

            syncNextGameFrame = ret.syncNextGameFrame;

            if (ret.sessionCommand[0] != 0) {
                idCmdArgs args = new idCmdArgs();

                args.TokenizeString(ctos(ret.sessionCommand), false);

                if (0 == idStr.Icmp(args.Argv(0), "map")) {
                    // get current player states
                    for (int i = 0; i < numClients; i++) {
                        mapSpawnData.persistentPlayerInfo[i] = game.GetPersistentPlayerInfo(i);
                    }
                    // clear the devmap key on serverinfo, so player spawns
                    // won't get the map testing items
                    mapSpawnData.serverInfo.Delete("devmap");

                    // go to the next map
                    MoveToNewMap(args.Argv(1));
                } else if (0 == idStr.Icmp(args.Argv(0), "devmap")) {
                    mapSpawnData.serverInfo.Set("devmap", "1");
                    MoveToNewMap(args.Argv(1));
                } else if (0 == idStr.Icmp(args.Argv(0), "died")) {
                    // restart on the same map
                    UnloadMap();
                    SetGUI(guiRestartMenu, null);
                } else if (0 == idStr.Icmp(args.Argv(0), "disconnect")) {
                    cmdSystem.BufferCommandText(CMD_EXEC_INSERT, "stoprecording ; disconnect");
                } else if (0 == idStr.Icmp(args.Argv(0), "endOfDemo")) {
                    cmdSystem.BufferCommandText(CMD_EXEC_NOW, "endOfDemo");
                }
            }
        }

        public void FinishCmdLoad() {
        }

        public void LoadLoadingGui(final String mapName) {
            // load / program a gui to stay up on the screen while loading
            idStr stripped = new idStr(mapName).StripFileExtension().StripPath();

            final String guiMap = va("guis/map/%." + MAX_STRING_CHARS + "s.gui", stripped.toString());//char guiMap[ MAX_STRING_CHARS ];
            // give the gamecode a chance to override
            game.GetMapLoadingGUI(guiMap.toCharArray());

            if (uiManager.CheckGui(guiMap)) {
                guiLoading = uiManager.FindGui(guiMap, true, false, true);
            } else {
                guiLoading = uiManager.FindGui("guis/map/loading.gui", true, false, true);
            }
            guiLoading.SetStateFloat("map_loading", 0.0f);
        }

//
        /*
         ================
         idSessionLocal::DemoShot

         A demoShot is a single frame demo
         ================
         */
        public void DemoShot(final String demoName) {
            StartRecordingRenderDemo(demoName);

            // force draw one frame
            UpdateScreen();

            StopRecordingRenderDemo();
        }
//

        public void TestGUI(final String guiName) {
            if (guiName != null) {
                guiTest = uiManager.FindGui(guiName, true, false, true);
            } else {
                guiTest = null;
            }
        }
//

        public int GetBytesNeededForMapLoad(final String mapName) throws idException {
            final idDecl mapDecl = declManager.FindType(DECL_MAPDEF, mapName, false);
            final idDeclEntityDef mapDef = (idDeclEntityDef) mapDecl;
            if (mapDef != null) {
                return mapDef.dict.GetInt(va("size%d", Max(0, com_machineSpec.GetInteger())));
            } else {
                if (com_machineSpec.GetInteger() < 2) {
                    return 200 * 1024 * 1024;
                } else {
                    return 400 * 1024 * 1024;
                }
            }
        }

        public void SetBytesNeededForMapLoad(final String mapName, int bytesNeeded) throws idException {
            idDecl mapDecl = /*const_cast<idDecl *>*/ (declManager.FindType(DECL_MAPDEF, mapName, false));
            idDeclEntityDef mapDef = (idDeclEntityDef) mapDecl;

            if (com_updateLoadSize.GetBool() && mapDef != null) {
                // we assume that if com_updateLoadSize is true then the file is writable

                mapDef.dict.SetInt(va("size%d", com_machineSpec.GetInteger()), bytesNeeded);

                idStr declText = new idStr("\nmapDef ");
                declText.Append(mapDef.GetName());
                declText.Append(" {\n");
                for (int i = 0; i < mapDef.dict.GetNumKeyVals(); i++) {
                    final idKeyValue kv = mapDef.dict.GetKeyVal(i);
                    if (kv != null && (kv.GetKey().Cmp("classname") != 0)) {
                        declText.Append("\t\"" + kv.GetKey() + "\"\t\t\"" + kv.GetValue() + "\"\n");
                    }
                }
                declText.Append("}");
                mapDef.SetText(declText.toString());
                mapDef.ReplaceSourceFileText();
            }
        }
//

        /*
         ===============
         idSessionLocal::ExecuteMapChange

         Performs the initialization of a game based on mapSpawnData, used for both single
         player and multiplayer, but not for renderDemos, which don't
         create a game at all.
         Exits with mapSpawned = true
         ===============
         */
        public void ExecuteMapChange() {
            ExecuteMapChange(false);
        }

        public void ExecuteMapChange(boolean noFadeWipe/*= false*/) throws idException {
            int i;
            boolean reloadingSameMap;

            // close console and remove any prints from the notify lines
            console.Close();

            if (IsMultiplayer()) {
                // make sure the mp GUI isn't up, or when players get back in the
                // map, mpGame's menu and the gui will be out of sync.
                SetGUI(null, null);
            }

            // mute sound
            soundSystem.SetMute(true);

            // clear all menu sounds
            menuSoundWorld.ClearAllSoundEmitters();

            // unpause the game sound world
            // NOTE: we UnPause again later down. not sure this is needed
            if (sw.IsPaused()) {
                sw.UnPause();
            }

            if (!noFadeWipe) {
                // capture the current screen and start a wipe
                StartWipe("wipeMaterial", true);

                // immediately complete the wipe to fade out the level transition
                // run the wipe to completion
                CompleteWipe();
            }

            // extract the map name from serverinfo
            idStr mapString = new idStr(mapSpawnData.serverInfo.GetString("si_map"));

            idStr fullMapName = new idStr("maps/");
            fullMapName.Append(mapString);
            fullMapName.StripFileExtension();

            // shut down the existing game if it is running
            UnloadMap();

            // don't do the deferred caching if we are reloading the same map
            if (fullMapName == currentMapName) {
                reloadingSameMap = true;
            } else {
                reloadingSameMap = false;
                currentMapName = fullMapName;
            }

            // note which media we are going to need to load
            if (!reloadingSameMap) {
                declManager.BeginLevelLoad();
                renderSystem.BeginLevelLoad();
                soundSystem.BeginLevelLoad();
            }

            uiManager.BeginLevelLoad();
            uiManager.Reload(true);

            // set the loading gui that we will wipe to
            LoadLoadingGui(mapString.toString());

            // cause prints to force screen updates as a pacifier,
            // and draw the loading gui instead of game draws
            insideExecuteMapChange = true;

            // if this works out we will probably want all the sizes in a def file although this solution will 
            // work for new maps etc. after the first load. we can also drop the sizes into the default.cfg
            fileSystem.ResetReadCount();
            if (!reloadingSameMap) {
                bytesNeededForMapLoad = GetBytesNeededForMapLoad(mapString.toString());
            } else {
                bytesNeededForMapLoad = 30 * 1024 * 1024;
            }

            ClearWipe();

            // let the loading gui spin for 1 second to animate out
            ShowLoadingGui();

            // note any warning prints that happen during the load process
            common.ClearWarnings(mapString.toString());

            // release the mouse cursor
            // before we do this potentially long operation
            Sys_GrabMouseCursor(false);

            // if net play, we get the number of clients during mapSpawnInfo processing
            if (!idAsyncNetwork.IsActive()) {
                numClients = 1;
            }

            int start = Sys_Milliseconds();

            common.Printf("--------- Map Initialization ---------\n");
            common.Printf("Map: %s\n", mapString);

            // let the renderSystem load all the geometry
            if (!rw.InitFromMap(fullMapName.toString())) {
                common.Error("couldn't load %s", fullMapName);
            }

            // for the synchronous networking we needed to roll the angles over from
            // level to level, but now we can just clear everything
            usercmdGen.InitForNewMap();
//	memset( mapSpawnData.mapSpawnUsercmd, 0, sizeof( mapSpawnData.mapSpawnUsercmd ) );
            mapSpawnData.mapSpawnUsercmd = Stream.generate(usercmd_t::new).limit(mapSpawnData.mapSpawnUsercmd.length).toArray(usercmd_t[]::new);

            // set the user info
            for (i = 0; i < numClients; i++) {
                game.SetUserInfo(i, mapSpawnData.userInfo[i], idAsyncNetwork.client.IsActive(), false);
                game.SetPersistentPlayerInfo(i, mapSpawnData.persistentPlayerInfo[i]);
            }

            // load and spawn all other entities ( from a savegame possibly )
            if (loadingSaveGame && savegameFile != null) {
                if (game.InitFromSaveGame(fullMapName + ".map", rw, sw, savegameFile) == false) {
                    // If the loadgame failed, restart the map with the player persistent data
                    loadingSaveGame = false;
                    fileSystem.CloseFile(savegameFile);
                    savegameFile = null;

                    game.SetServerInfo(mapSpawnData.serverInfo);
                    game.InitFromNewMap(fullMapName + ".map", rw, sw, idAsyncNetwork.server.IsActive(), idAsyncNetwork.client.IsActive(), Sys_Milliseconds());
                }
            } else {
                game.SetServerInfo(mapSpawnData.serverInfo);
                game.InitFromNewMap(fullMapName + ".map", rw, sw, idAsyncNetwork.server.IsActive(), idAsyncNetwork.client.IsActive(), Sys_Milliseconds());
            }

            if (!idAsyncNetwork.IsActive() && !loadingSaveGame) {
                // spawn players
                for (i = 0; i < numClients; i++) {
                    game.SpawnPlayer(i);
                }
            }

            // actually purge/load the media
            if (!reloadingSameMap) {
                renderSystem.EndLevelLoad();
                soundSystem.EndLevelLoad(mapString.toString());
                declManager.EndLevelLoad();
                SetBytesNeededForMapLoad(mapString.toString(), fileSystem.GetReadCount());
            }
            uiManager.EndLevelLoad();

            if (!idAsyncNetwork.IsActive() && !loadingSaveGame) {
                // run a few frames to allow everything to settle
                for (i = 0; i < 10; i++) {
                    game.RunFrame(mapSpawnData.mapSpawnUsercmd/*[0]*/);
                }
            }

            common.Printf("-----------------------------------\n");

            int msec = Sys_Milliseconds() - start;
            common.Printf("%6d msec to load %s\n", msec, mapString);

            // let the renderSystem generate interactions now that everything is spawned
            rw.GenerateAllInteractions();

            common.PrintWarnings();

            if (guiLoading != null && bytesNeededForMapLoad != 0) {
                float pct = guiLoading.State().GetFloat("map_loading");
                if (pct < 0.0f) {
                    pct = 0.0f;
                }
                while (pct < 1.0f) {
                    guiLoading.SetStateFloat("map_loading", pct);
                    guiLoading.StateChanged(com_frameTime);
                    Sys_GenerateEvents();
                    UpdateScreen();
                    pct += 0.05f;
                }
            }

            // capture the current screen and start a wipe
            StartWipe("wipe2Material");

            usercmdGen.Clear();

            // start saving commands for possible writeCmdDemo usage
            logIndex = 0;
            statIndex = 0;
            lastSaveIndex = 0;

            // don't bother spinning over all the tics we spent loading
            lastGameTic = latchedTicNumber = com_ticNumber;

            // remove any prints from the notify lines
            console.ClearNotifyLines();

            // stop drawing the laoding screen
            insideExecuteMapChange = false;

//            Sys_SetPhysicalWorkMemory(-1, -1);

            // set the game sound world for playback
            soundSystem.SetPlayingSoundWorld(sw);

            // when loading a save game the sound is paused
            if (sw.IsPaused()) {
                // unpause the game sound world
                sw.UnPause();
            }

            // restart entity sound playback
            soundSystem.SetMute(false);

            // we are valid for game draws now
            mapSpawned = true;
            Sys_ClearEvents();
        }

        /*
         ===============
         idSessionLocal::UnloadMap

         Performs cleanup that needs to happen between maps, or when a
         game is exited.
         Exits with mapSpawned = false
         ===============
         */
        public void UnloadMap() {
            StopPlayingRenderDemo();

            // end the current map in the game
            if (game != null) {
                game.MapShutdown();
            }

            if (cmdDemoFile != null) {
                fileSystem.CloseFile(cmdDemoFile);
                cmdDemoFile = null;
            }

            if (writeDemo != null) {
                StopRecordingRenderDemo();
            }

            mapSpawned = false;
        }
//

        // return true if we actually waiting on an auth reply
        public boolean MaybeWaitOnCDKey() {
            if (authEmitTimeout > 0) {
                authWaitBox = true;
                sessLocal.MessageBox(MSG_WAIT, common.GetLanguageDict().GetString("#str_07191"), null, true, null, null, true);
                return true;
            }
            return false;
        }
//
//	//------------------
//	// Session_menu.cpp
//
        public idStrList loadGameList = new idStrList();
        public idStrList modsList = new idStrList();
//

        public idUserInterface GetActiveMenu() {
            return guiActive;
        }
//

        public void DispatchCommand(idUserInterface gui, final String menuCommand, boolean doIngame /*= true*/) throws idException {

            if (NOT(gui)) {
                gui = guiActive;
            }

            if (gui == guiMainMenu) {
                HandleMainMenuCommands(menuCommand);
                return;
            } else if (gui == guiIntro) {
                HandleIntroMenuCommands(menuCommand);
            } else if (gui == guiMsg) {
                HandleMsgCommands(menuCommand);
            } else if (gui == guiTakeNotes) {
                HandleNoteCommands(menuCommand);
            } else if (gui == guiRestartMenu) {
                HandleRestartMenuCommands(menuCommand);
            } else if ((game != null) && (guiActive != null) && guiActive.State().GetBool("gameDraw")) {
                final String cmd = game.HandleGuiCommands(menuCommand);
                if (null == cmd) {
                    guiActive = null;
                } else if (idStr.Icmp(cmd, "main") == 0) {
                    StartMenu();
                } else if (cmd.startsWith("sound ")) {
                    // pipe the GUI sound commands not handled by the game to the main menu code
                    HandleMainMenuCommands(cmd);
                }
            } else if (guiHandle != null) {
                if ( /*(*guiHandle)*/(menuCommand) != null) {
                    return;
                }
            } else if (!doIngame) {
                common.DPrintf("idSessionLocal::DispatchCommand: no dispatch found for command '%s'\n", menuCommand);
            }

            if (doIngame) {
                HandleInGameCommands(menuCommand);
            }
        }

        public void DispatchCommand(idUserInterface gui, final String menuCommand) throws idException {
            DispatchCommand(gui, menuCommand, false);
        }

        /*
         ==============
         idSessionLocal::MenuEvent

         Executes any commands returned by the gui
         ==============
         */
        public void MenuEvent(final sysEvent_s event) {
            final String menuCommand;

            if (guiActive == null) {
                return;
            }

            menuCommand = guiActive.HandleEvent(event, com_frameTime);

            if (null == menuCommand || menuCommand.isEmpty()) {
                // If the menu didn't handle the event, and it's a key down event for an F key, run the bind
                if (event.evType == SE_KEY && event.evValue2 == 1 && event.evValue >= K_F1 && event.evValue <= K_F12) {
                    idKeyInput.ExecKeyBinding(event.evValue);
                }
                return;
            }

            DispatchCommand(guiActive, menuCommand);
        }

        public boolean HandleSaveGameMenuCommand(idCmdArgs args, int icmd) throws idException {

            final String cmd = args.Argv(icmd - 1);

            if (0 == idStr.Icmp(cmd, "loadGame")) {
                int choice = guiActive.State().GetInt("loadgame_sel_0");
                if (choice >= 0 && choice < loadGameList.Num()) {
                    sessLocal.LoadGame(loadGameList.oGet(choice).toString());
                }
                return true;
            }

            if (0 == idStr.Icmp(cmd, "saveGame")) {
                final String saveGameName = guiActive.State().GetString("saveGameName");
                if (saveGameName != null && saveGameName.isEmpty()) {

                    // First see if the file already exists unless they pass '1' to authorize the overwrite
                    if (icmd == args.Argc() || Integer.parseInt(args.Argv(icmd++)) == 0) {
                        idStr saveFileName = new idStr(saveGameName);
                        sessLocal.ScrubSaveGameFileName(saveFileName);
                        saveFileName = new idStr("savegames/" + saveFileName);
                        saveFileName.SetFileExtension(".save");

                        idStr game = new idStr(cvarSystem.GetCVarString("fs_game"));
                        idFile file;
                        if (game.Length() != 0) {
                            file = fileSystem.OpenFileRead(saveFileName.toString(), true, game.toString());
                        } else {
                            file = fileSystem.OpenFileRead(saveFileName.toString());
                        }

                        if (file != null) {
                            fileSystem.CloseFile(file);

                            // The file exists, see if it's an autosave
                            saveFileName.SetFileExtension(".txt");
                            idLexer src = new idLexer(LEXFL_NOERRORS | LEXFL_NOSTRINGCONCAT);
                            if (src.LoadFile(saveFileName.toString())) {
                                idToken tok = new idToken();
                                src.ReadToken(tok); // Name
                                src.ReadToken(tok); // Map
                                src.ReadToken(tok); // Screenshot
                                if (!tok.IsEmpty()) {
                                    // NOTE: base/ gui doesn't handle that one
                                    guiActive.HandleNamedEvent("autosaveOverwriteError");
                                    return true;
                                }
                            }
                            guiActive.HandleNamedEvent("saveGameOverwrite");
                            return true;
                        }
                    }

                    sessLocal.SaveGame(saveGameName);
                    SetSaveGameGuiVars();
                    guiActive.StateChanged(com_frameTime);
                }
                return true;
            }

            if (0 == idStr.Icmp(cmd, "deleteGame")) {
                int choice = guiActive.State().GetInt("loadgame_sel_0");
                if (choice >= 0 && choice < loadGameList.Num()) {
                    fileSystem.RemoveFile(va("savegames/%s.save", loadGameList.oGet(choice).toString()));
                    fileSystem.RemoveFile(va("savegames/%s.tga", loadGameList.oGet(choice).toString()));
                    fileSystem.RemoveFile(va("savegames/%s.txt", loadGameList.oGet(choice).toString()));
                    SetSaveGameGuiVars();
                    guiActive.StateChanged(com_frameTime);
                }
                return true;
            }

            if (0 == idStr.Icmp(cmd, "updateSaveGameInfo")) {
                int choice = guiActive.State().GetInt("loadgame_sel_0");
                if (choice >= 0 && choice < loadGameList.Num()) {
                    final idMaterial material;

                    idStr saveName, description;
                    String screenshot;
                    idLexer src = new idLexer(LEXFL_NOERRORS | LEXFL_NOSTRINGCONCAT);
                    if (src.LoadFile(va("savegames/%s.txt", loadGameList.oGet(choice).toString()))) {
                        idToken tok = new idToken();

                        src.ReadToken(tok);
                        saveName = tok;

                        src.ReadToken(tok);
                        description = tok;

                        src.ReadToken(tok);
                        screenshot = tok.toString();

                    } else {
                        saveName = loadGameList.oGet(choice);
                        description = loadGameList.oGet(choice);
                        screenshot = "";
                    }
                    if (screenshot.length() == 0) {
                        screenshot = va("savegames/%s.tga", loadGameList.oGet(choice).toString());
                    }
                    material = declManager.FindMaterial(screenshot);
                    if (material != null) {
                        material.ReloadImages(false);
                    }
                    guiActive.SetStateString("loadgame_shot", screenshot);

                    saveName.RemoveColors();
                    guiActive.SetStateString("saveGameName", saveName.toString());
                    guiActive.SetStateString("saveGameDescription", description.toString());

                    long[] timeStamp = {0};
                    fileSystem.ReadFile(va("savegames/%s.save", loadGameList.oGet(choice).toString()), null, timeStamp);
                    idStr date = new idStr(Sys_TimeStampToStr(timeStamp[0]));
                    int tab = date.Find('\t');
                    idStr time = date.Right(date.Length() - tab - 1);
                    guiActive.SetStateString("saveGameDate", date.Left(tab).toString());
                    guiActive.SetStateString("saveGameTime", time.toString());
                }
                return true;
            }

            return false;
        }

        /*
         ==============
         idSessionLocal::HandleInGameCommands

         Executes any commands returned by the gui
         ==============
         */
        public void HandleInGameCommands(final String menuCommand) {
            // execute the command from the menu
            idCmdArgs args = new idCmdArgs();

            args.TokenizeString(menuCommand, false);

            /*final*/ String cmd = args.Argv(0);
            if (0 == idStr.Icmp(cmd, "close")) {
                if (guiActive != null) {
                    sysEvent_s ev = new sysEvent_s();
                    ev.evType = SE_NONE;
//			final String cmd;
                    args.oSet(guiActive.HandleEvent(ev, com_frameTime));
                    guiActive.Activate(false, com_frameTime);
                    guiActive = null;
                }
            }
        }

        /*
         ==============
         idSessionLocal::HandleMainMenuCommands

         Executes any commands returned by the gui
         ==============
         */
        public void HandleMainMenuCommands(final String menuCommand) throws idException {
            // execute the command from the menu
            int icmd;
            idCmdArgs args = new idCmdArgs();

            args.TokenizeString(menuCommand, false);

            for (icmd = 0; icmd < args.Argc();) {
                final String cmd = args.Argv(icmd++);

                if (HandleSaveGameMenuCommand(args, icmd)) {
                    continue;
                }

                // always let the game know the command is being run
                if (game != null) {
                    game.HandleMainMenuCommands(cmd, guiActive);
                }

                if (0 == idStr.Icmp(cmd, "startGame")) {
                    cvarSystem.SetCVarInteger("g_skill", guiMainMenu.State().GetInt("skill"));
                    if (icmd < args.Argc()) {
                        StartNewGame(args.Argv(icmd++));
                    } else {
                        if (ID_DEMO_BUILD) {
                            StartNewGame("game/mars_city1");
                        } else {
                            StartNewGame("game/demo_mars_city1");
                        }
                    }
                    // need to do this here to make sure com_frameTime is correct or the gui activates with a time that 
                    // is "however long map load took" time in the past
                    common.GUIFrame(false, false);
                    SetGUI(guiIntro, null);
                    guiIntro.StateChanged(com_frameTime, true);
                    // stop playing the game sounds
                    soundSystem.SetPlayingSoundWorld(menuSoundWorld);

                    continue;
                }

                if (0 == idStr.Icmp(cmd, "quit")) {
                    ExitMenu();
                    common.Quit();
                    return;
                }

                if (0 == idStr.Icmp(cmd, "loadMod")) {
                    int choice = guiActive.State().GetInt("modsList_sel_0");
                    if (choice >= 0 && choice < modsList.Num()) {
                        cvarSystem.SetCVarString("fs_game", modsList.oGet(choice).toString());
                        cmdSystem.BufferCommandText(CMD_EXEC_APPEND, "reloadEngine menu\n");
                    }
                }

                if (0 == idStr.Icmp(cmd, "UpdateServers")) {
                    if (guiActive.State().GetBool("lanSet")) {
                        cmdSystem.BufferCommandText(CMD_EXEC_NOW, "LANScan");
                    } else {
                        idAsyncNetwork.GetNETServers();
                    }
                    continue;
                }

                if (0 == idStr.Icmp(cmd, "RefreshServers")) {
                    if (guiActive.State().GetBool("lanSet")) {
                        cmdSystem.BufferCommandText(CMD_EXEC_NOW, "LANScan");
                    } else {
                        idAsyncNetwork.client.serverList.NetScan();
                    }
                    continue;
                }

                if (0 == idStr.Icmp(cmd, "FilterServers")) {
                    idAsyncNetwork.client.serverList.ApplyFilter();
                    continue;
                }

                if (0 == idStr.Icmp(cmd, "sortServerName")) {
                    idAsyncNetwork.client.serverList.SetSorting(SORT_SERVERNAME);
                    continue;
                }

                if (0 == idStr.Icmp(cmd, "sortGame")) {
                    idAsyncNetwork.client.serverList.SetSorting(SORT_GAME);
                    continue;
                }

                if (0 == idStr.Icmp(cmd, "sortPlayers")) {
                    idAsyncNetwork.client.serverList.SetSorting(SORT_PLAYERS);
                    continue;
                }

                if (0 == idStr.Icmp(cmd, "sortPing")) {
                    idAsyncNetwork.client.serverList.SetSorting(SORT_PING);
                    continue;
                }

                if (0 == idStr.Icmp(cmd, "sortGameType")) {
                    idAsyncNetwork.client.serverList.SetSorting(SORT_GAMETYPE);
                    continue;
                }

                if (0 == idStr.Icmp(cmd, "sortMap")) {
                    idAsyncNetwork.client.serverList.SetSorting(SORT_MAP);
                    continue;
                }

                if (0 == idStr.Icmp(cmd, "serverList")) {
                    idAsyncNetwork.client.serverList.GUIUpdateSelected();
                    continue;
                }

                if (0 == idStr.Icmp(cmd, "LANConnect")) {
                    int sel = guiActive.State().GetInt("serverList_selid_0");
                    cmdSystem.BufferCommandText(CMD_EXEC_NOW, va("Connect %d\n", sel));
                    return;
                }

                if (0 == idStr.Icmp(cmd, "MAPScan")) {
                    /*final*/ String gametype = cvarSystem.GetCVarString("si_gameType");
                    if (gametype == null || gametype.isEmpty() || idStr.Icmp(gametype, "singleplayer") == 0) {
                        gametype = "Deathmatch";
                    }

                    int i, num;
                    idStr si_map = new idStr(cvarSystem.GetCVarString("si_map"));
                    idDict dict;

                    guiMainMenu_MapList.Clear();
                    guiMainMenu_MapList.SetSelection(0);
                    num = fileSystem.GetNumMaps();
                    for (i = 0; i < num; i++) {
                        dict = fileSystem.GetMapDecl(i);
                        if (dict != null && dict.GetBool(gametype)) {
                            /*final*/ String mapName = dict.GetString("name");
                            if (!isNotNullOrEmpty(mapName)) {
                                mapName = dict.GetString("path");
                            }
                            mapName = common.GetLanguageDict().GetString(mapName);
                            guiMainMenu_MapList.Add(i, new idStr(mapName));
                            if (0 == si_map.Icmp(dict.GetString("path"))) {
                                guiMainMenu_MapList.SetSelection(guiMainMenu_MapList.Num() - 1);
                            }
                        }
                    }
                    i = guiMainMenu_MapList.GetSelection(null, 0);
                    if (i >= 0) {
                        dict = fileSystem.GetMapDecl(i);
                    } else {
                        dict = null;
                    }
                    cvarSystem.SetCVarString("si_map", (dict != null ? dict.GetString("path") : ""));

                    // set the current level shot
                    UpdateMPLevelShot();
                    continue;
                }

                if (0 == idStr.Icmp(cmd, "click_mapList")) {
                    int mapNum = guiMainMenu_MapList.GetSelection(null, 0);
                    final idDict dict = fileSystem.GetMapDecl(mapNum);
                    if (dict != null) {
                        cvarSystem.SetCVarString("si_map", dict.GetString("path"));
                    }
                    UpdateMPLevelShot();
                    continue;
                }

                if (0 == idStr.Icmp(cmd, "inetConnect")) {
                    final String s = guiMainMenu.State().GetString("inetGame");

                    if (null == s || s.isEmpty()) {
                        // don't put the menu away if there isn't a valid selection
                        continue;
                    }

                    cmdSystem.BufferCommandText(CMD_EXEC_NOW, va("connect %s", s));
                    return;
                }

                if (0 == idStr.Icmp(cmd, "startMultiplayer")) {
                    int dedicated = guiActive.State().GetInt("dedicated");
                    cvarSystem.SetCVarBool("net_LANServer", guiActive.State().GetBool("server_type"));
                    if (gui_configServerRate.GetInteger() > 0) {
                        // guess the best rate for upstream, number of internet clients
                        if (gui_configServerRate.GetInteger() == 5 || cvarSystem.GetCVarBool("net_LANServer")) {
                            cvarSystem.SetCVarInteger("net_serverMaxClientRate", 25600);
                        } else {
                            // internet players
                            int n_clients = cvarSystem.GetCVarInteger("si_maxPlayers");
                            if (0 == dedicated) {
                                n_clients--;
                            }
                            int maxclients = 0;
                            switch (gui_configServerRate.GetInteger()) {
                                case 1:
                                    // 128 kbits
                                    cvarSystem.SetCVarInteger("net_serverMaxClientRate", 8000);
                                    maxclients = 2;
                                    break;
                                case 2:
                                    // 256 kbits
                                    cvarSystem.SetCVarInteger("net_serverMaxClientRate", 9500);
                                    maxclients = 3;
                                    break;
                                case 3:
                                    // 384 kbits
                                    cvarSystem.SetCVarInteger("net_serverMaxClientRate", 10500);
                                    maxclients = 4;
                                    break;
                                case 4:
                                    // 512 and above..
                                    cvarSystem.SetCVarInteger("net_serverMaxClientRate", 14000);
                                    maxclients = 4;
                                    break;
                            }
                            if (n_clients > maxclients) {
                                if (isNotNullOrEmpty(MessageBox(MSG_OKCANCEL, va(common.GetLanguageDict().GetString("#str_04315"), dedicated != 0 ? maxclients : Min(8, maxclients + 1)), common.GetLanguageDict().GetString("#str_04316"), true, "OK"))) {//[0] == '\0') {
                                    continue;
                                }
                                cvarSystem.SetCVarInteger("si_maxPlayers", dedicated != 0 ? maxclients : Min(8, maxclients + 1));
                            }
                        }
                    }

                    if (0 == dedicated && !cvarSystem.GetCVarBool("net_LANServer") && cvarSystem.GetCVarInteger("si_maxPlayers") > 4) {
                        // "Dedicated server mode is recommended for internet servers with more than 4 players. Continue in listen mode?"
//				if ( !MessageBox( MSG_YESNO, common.GetLanguageDict().GetString ( "#str_00100625" ), common.GetLanguageDict().GetString ( "#str_00100626" ), true, "yes" )[0] ) {
                        if (MessageBox(MSG_YESNO, common.GetLanguageDict().GetString("#str_00100625"), common.GetLanguageDict().GetString("#str_00100626"), true, "yes").isEmpty()) {
                            continue;
                        }
                    }

                    if (dedicated != 0) {
                        cvarSystem.SetCVarInteger("net_serverDedicated", 1);
                    } else {
                        cvarSystem.SetCVarInteger("net_serverDedicated", 0);
                    }

                    ExitMenu();
                    // may trigger a reloadEngine - APPEND
                    cmdSystem.BufferCommandText(CMD_EXEC_APPEND, "SpawnServer\n");
                    return;
                }

                if (0 == idStr.Icmp(cmd, "mpSkin")) {
                    idStr skin;
                    if (args.Argc() - icmd >= 1) {
                        skin = new idStr(args.Argv(icmd++));
                        cvarSystem.SetCVarString("ui_skin", skin.toString());
                        SetMainMenuSkin();
                    }
                    continue;
                }

                if (0 == idStr.Icmp(cmd, "close")) {
                    // if we aren't in a game, the menu can't be closed
                    if (mapSpawned) {
                        ExitMenu();
                    }
                    continue;
                }

                if (0 == idStr.Icmp(cmd, "resetdefaults")) {
                    cmdSystem.BufferCommandText(CMD_EXEC_NOW, "exec default.cfg");
                    guiMainMenu.SetKeyBindingNames();
                    continue;
                }

                if (0 == idStr.Icmp(cmd, "bind")) {
                    if (args.Argc() - icmd >= 2) {
                        int key = Integer.parseInt(args.Argv(icmd++));
                        String bind = args.Argv(icmd++);
                        if (idKeyInput.NumBinds(bind) >= 2 && !idKeyInput.KeyIsBoundTo(key, bind)) {
                            idKeyInput.UnbindBinding(bind);
                        }
                        idKeyInput.SetBinding(key, bind);
                        guiMainMenu.SetKeyBindingNames();
                    }
                    continue;
                }

                if (0 == idStr.Icmp(cmd, "play")) {
                    if (args.Argc() - icmd >= 1) {
                        idStr snd = new idStr(args.Argv(icmd++));
                        int channel = 1;
                        if (snd.Length() == 1) {
                            channel = Integer.parseInt(snd.toString());
                            snd = new idStr(args.Argv(icmd++));
                        }
                        menuSoundWorld.PlayShaderDirectly(snd.toString(), channel);

                    }
                    continue;
                }

                if (0 == idStr.Icmp(cmd, "music")) {
                    if (args.Argc() - icmd >= 1) {
                        idStr snd = new idStr(args.Argv(icmd++));
                        menuSoundWorld.PlayShaderDirectly(snd.toString(), 2);
                    }
                    continue;
                }

                // triggered from mainmenu or mpmain
                if (0 == idStr.Icmp(cmd, "sound")) {
                    idStr vcmd = new idStr();
                    if (args.Argc() - icmd >= 1) {
                        vcmd = new idStr(args.Argv(icmd++));
                    }
                    if (0 == vcmd.Length() || 0 == vcmd.Icmp("speakers")) {
                        int old = cvarSystem.GetCVarInteger("s_numberOfSpeakers");
                        cmdSystem.BufferCommandText(CMD_EXEC_NOW, "s_restart\n");
                        if (old != cvarSystem.GetCVarInteger("s_numberOfSpeakers")) {
                            if (_WIN32) {
                                MessageBox(MSG_OK, common.GetLanguageDict().GetString("#str_04142"), common.GetLanguageDict().GetString("#str_04141"), true);
                            } else {
                                // a message that doesn't mention the windows control panel
                                MessageBox(MSG_OK, common.GetLanguageDict().GetString("#str_07230"), common.GetLanguageDict().GetString("#str_04141"), true);
                            }
                        }
                    }
                    if (0 == vcmd.Icmp("eax")) {
                        if (cvarSystem.GetCVarBool("s_useEAXReverb")) {
                            int eax = soundSystem.IsEAXAvailable();
                            switch (eax) {
                                case 2:
                                    // OpenAL subsystem load failed
                                    MessageBox(MSG_OK, common.GetLanguageDict().GetString("#str_07238"), common.GetLanguageDict().GetString("#str_07231"), true);
                                    break;
                                case 1:
                                    // when you restart
                                    MessageBox(MSG_OK, common.GetLanguageDict().GetString("#str_04137"), common.GetLanguageDict().GetString("#str_07231"), true);
                                    break;
                                case -1:
                                    cvarSystem.SetCVarBool("s_useEAXReverb", false);
                                    // disabled
                                    MessageBox(MSG_OK, common.GetLanguageDict().GetString("#str_07233"), common.GetLanguageDict().GetString("#str_07231"), true);
                                    break;
                                case 0:
                                    cvarSystem.SetCVarBool("s_useEAXReverb", false);
                                    // not available
                                    MessageBox(MSG_OK, common.GetLanguageDict().GetString("#str_07232"), common.GetLanguageDict().GetString("#str_07231"), true);
                                    break;
                            }
                        } else {
                            // also turn off OpenAL so we fully go back to legacy mixer
                            cvarSystem.SetCVarBool("s_useOpenAL", false);
                            // when you restart
                            MessageBox(MSG_OK, common.GetLanguageDict().GetString("#str_04137"), common.GetLanguageDict().GetString("#str_07231"), true);
                        }
                    }
                    if (0 == vcmd.Icmp("drivar")) {
                        cmdSystem.BufferCommandText(CMD_EXEC_NOW, "s_restart\n");
                    }
                    continue;
                }

                if (0 == idStr.Icmp(cmd, "video")) {
                    idStr vcmd = new idStr();
                    if (args.Argc() - icmd >= 1) {
                        vcmd = new idStr(args.Argv(icmd++));
                    }

                    int oldSpec = com_machineSpec.GetInteger();

                    if (idStr.Icmp(vcmd.toString(), "low") == 0) {
                        com_machineSpec.SetInteger(0);
                    } else if (idStr.Icmp(vcmd.toString(), "medium") == 0) {
                        com_machineSpec.SetInteger(1);
                    } else if (idStr.Icmp(vcmd.toString(), "high") == 0) {
                        com_machineSpec.SetInteger(2);
                    } else if (idStr.Icmp(vcmd.toString(), "ultra") == 0) {
                        com_machineSpec.SetInteger(3);
                    } else if (idStr.Icmp(vcmd.toString(), "recommended") == 0) {
                        cmdSystem.BufferCommandText(CMD_EXEC_NOW, "setMachineSpec\n");
                    }

                    if (oldSpec != com_machineSpec.GetInteger()) {
                        guiActive.SetStateInt("com_machineSpec", com_machineSpec.GetInteger());
                        guiActive.StateChanged(com_frameTime);
                        cmdSystem.BufferCommandText(CMD_EXEC_NOW, "execMachineSpec\n");
                    }

                    if (idStr.Icmp(vcmd.toString(), "restart") == 0) {
                        guiActive.HandleNamedEvent("cvar write render");
                        cmdSystem.BufferCommandText(CMD_EXEC_NOW, "vid_restart\n");
                    }

                    continue;
                }

                if (0 == idStr.Icmp(cmd, "clearBind")) {
                    if (args.Argc() - icmd >= 1) {
                        idKeyInput.UnbindBinding(args.Argv(icmd++));
                        guiMainMenu.SetKeyBindingNames();
                    }
                    continue;
                }

                // FIXME: obsolete
                if (0 == idStr.Icmp(cmd, "chatdone")) {
                    idStr temp = new idStr(guiActive.State().GetString("chattext"));
                    temp.Append("\r");
                    guiActive.SetStateString("chattext", "");
                    continue;
                }

                if (0 == idStr.Icmp(cmd, "exec")) {

                    //Backup the language so we can restore it after defaults.
                    idStr lang = new idStr(cvarSystem.GetCVarString("sys_lang"));

                    cmdSystem.BufferCommandText(CMD_EXEC_NOW, args.Argv(icmd++));
                    if (idStr.Icmp("cvar_restart", args.Argv(icmd - 1)) == 0) {
                        cmdSystem.BufferCommandText(CMD_EXEC_NOW, "exec default.cfg");
                        cmdSystem.BufferCommandText(CMD_EXEC_NOW, "setMachineSpec\n");

                        //Make sure that any r_brightness changes take effect
                        float bright = cvarSystem.GetCVarFloat("r_brightness");
                        cvarSystem.SetCVarFloat("r_brightness", 0.0f);
                        cvarSystem.SetCVarFloat("r_brightness", bright);

                        //Force user info modified after a reset to defaults
                        cvarSystem.SetModifiedFlags(CVAR_USERINFO);

                        guiActive.SetStateInt("com_machineSpec", com_machineSpec.GetInteger());

                        //Restore the language
                        cvarSystem.SetCVarString("sys_lang", lang.toString());

                    }
                    continue;
                }

                if (0 == idStr.Icmp(cmd, "loadBinds")) {
                    guiMainMenu.SetKeyBindingNames();
                    continue;
                }

                if (0 == idStr.Icmp(cmd, "systemCvars")) {
                    guiActive.HandleNamedEvent("cvar read render");
                    guiActive.HandleNamedEvent("cvar read sound");
                    continue;
                }

                if (0 == idStr.Icmp(cmd, "SetCDKey")) {
                    // we can't do this from inside the HandleMainMenuCommands code, otherwise the message box stuff gets confused
                    cmdSystem.BufferCommandText(CMD_EXEC_APPEND, "promptKey\n");
                    continue;
                }

                if (0 == idStr.Icmp(cmd, "CheckUpdate")) {
                    idAsyncNetwork.client.SendVersionCheck();
                    continue;
                }

                if (0 == idStr.Icmp(cmd, "CheckUpdate2")) {
                    idAsyncNetwork.client.SendVersionCheck(true);
                    continue;
                }

                if (0 == idStr.Icmp(cmd, "checkKeys")) {
                    if (ID_ENFORCE_KEY) {
                        // not a strict check so you silently auth in the background without bugging the user
                        if (!session.CDKeysAreValid(false)) {
                            cmdSystem.BufferCommandText(CMD_EXEC_NOW, "promptKey force");
                            cmdSystem.ExecuteCommandBuffer();
                        }
                    }
                    continue;
                }

                // triggered from mainmenu or mpmain
                if (0 == idStr.Icmp(cmd, "punkbuster")) {
                    idStr vcmd;
                    if (args.Argc() - icmd >= 1) {
                        vcmd = new idStr(args.Argv(icmd++));
                    }
                    // filtering PB based on enabled/disabled
                    idAsyncNetwork.client.serverList.ApplyFilter();
                    SetPbMenuGuiVars();
                    continue;
                }
            }
        }

        /*
         ==============
         idSessionLocal::HandleChatMenuCommands

         Executes any commands returned by the gui
         ==============
         */
        public void HandleChatMenuCommands(final String menuCommand) {
            // execute the command from the menu
            int i;
            idCmdArgs args = new idCmdArgs();

            args.TokenizeString(menuCommand, false);

            for (i = 0; i < args.Argc();) {
                final String cmd = args.Argv(i++);

                if (idStr.Icmp(cmd, "chatactive") == 0) {
                    //chat.chatMode = CHAT_GLOBAL;
                    continue;
                }
                if (idStr.Icmp(cmd, "chatabort") == 0) {
                    //chat.chatMode = CHAT_NONE;
                    continue;
                }
                if (idStr.Icmp(cmd, "netready") == 0) {
                    boolean b = cvarSystem.GetCVarBool("ui_ready");
                    cvarSystem.SetCVarBool("ui_ready", !b);
                    continue;
                }
                if (idStr.Icmp(cmd, "netstart") == 0) {
                    cmdSystem.BufferCommandText(CMD_EXEC_NOW, "netcommand start\n");
                    continue;
                }
            }
        }


        /*
         ==============
         idSessionLocal::HandleIntroMenuCommands

         Executes any commands returned by the gui
         ==============
         */
        public void HandleIntroMenuCommands(final String menuCommand) {
            // execute the command from the menu
            int i;
            idCmdArgs args = new idCmdArgs();

            args.TokenizeString(menuCommand, false);

            for (i = 0; i < args.Argc();) {
                final String cmd = args.Argv(i++);

                if (0 == idStr.Icmp(cmd, "startGame")) {
                    menuSoundWorld.ClearAllSoundEmitters();
                    ExitMenu();
                    continue;
                }

                if (0 == idStr.Icmp(cmd, "play")) {
                    if (args.Argc() - i >= 1) {
                        String snd = args.Argv(i++);
                        menuSoundWorld.PlayShaderDirectly(snd);
                    }
                    continue;
                }
            }
        }

        /*
         ==============
         idSessionLocal::HandleRestartMenuCommands

         Executes any commands returned by the gui
         ==============
         */
        public void HandleRestartMenuCommands(final String menuCommand) throws idException {
            // execute the command from the menu
            int icmd;
            idCmdArgs args = new idCmdArgs();

            args.TokenizeString(menuCommand, false);

            for (icmd = 0; icmd < args.Argc();) {
                final String cmd = args.Argv(icmd++);

                if (HandleSaveGameMenuCommand(args, icmd)) {
                    continue;
                }

                if (0 == idStr.Icmp(cmd, "restart")) {
                    if (!LoadGame(GetAutoSaveName(mapSpawnData.serverInfo.GetString("si_map")))) {
                        // If we can't load the autosave then just restart the map
                        MoveToNewMap(mapSpawnData.serverInfo.GetString("si_map"));
                    }
                    continue;
                }

                if (0 == idStr.Icmp(cmd, "quit")) {
                    ExitMenu();
                    common.Quit();
                    return;
                }

                if (0 == idStr.Icmp(cmd, "exec")) {
                    cmdSystem.BufferCommandText(CMD_EXEC_APPEND, args.Argv(icmd++));
                    continue;
                }

                if (0 == idStr.Icmp(cmd, "play")) {
                    if (args.Argc() - icmd >= 1) {
                        String snd = args.Argv(icmd++);
                        sw.PlayShaderDirectly(snd);
                    }
                    continue;
                }
            }
        }

        public void HandleMsgCommands(final String menuCommand) {
            assert (guiActive == guiMsg);
            // "stop" works even on first frame
            if (idStr.Icmp(menuCommand, "stop") == 0) {
                // force hiding the current dialog
                guiActive = guiMsgRestore;
                guiMsgRestore = null;
                msgRunning = false;
                msgRetIndex = -1;
            }
            if (msgIgnoreButtons) {
                common.DPrintf("MessageBox HandleMsgCommands 1st frame ignore\n");
                return;
            }
            if (idStr.Icmp(menuCommand, "mid") == 0 || idStr.Icmp(menuCommand, "left") == 0) {
                guiActive = guiMsgRestore;
                guiMsgRestore = null;
                msgRunning = false;
                msgRetIndex = 0;
                DispatchCommand(guiActive, msgFireBack[0].toString());
            } else if (idStr.Icmp(menuCommand, "right") == 0) {
                guiActive = guiMsgRestore;
                guiMsgRestore = null;
                msgRunning = false;
                msgRetIndex = 1;
                DispatchCommand(guiActive, msgFireBack[1].toString());
            }
        }
        static final String NOTEDATFILE = "C:/notenumber.dat";

        public void HandleNoteCommands(final String menuCommand) throws idException {
            guiActive = null;

            if (idStr.Icmp(menuCommand, "note") == 0 && mapSpawned) {

                idFile file = null;
                for (int tries = 0; tries < 10; tries++) {
                    file = fileSystem.OpenExplicitFileRead(NOTEDATFILE);
                    if (file != null) {
                        break;
                    }
                    Sys_Sleep(500);
                }
                int[] noteNumber = {1000};
                if (file != null) {
                    file.ReadInt(noteNumber);//4);
                    fileSystem.CloseFile(file);
                }

                int i;
                idStr str, noteNum, shotName = new idStr(), workName, fileName = new idStr("viewnotes/");
                idStrList fileList = new idStrList();

                String severity = null;
                String p = guiTakeNotes.State().GetString("notefile");
                if (p == null || p.isEmpty()) {
                    p = cvarSystem.GetCVarString("ui_name");
                }

                boolean extended = guiTakeNotes.State().GetBool("extended");
                if (extended) {
                    if (guiTakeNotes.State().GetInt("severity") == 1) {
                        severity = "WishList_Viewnotes/";
                    } else {
                        severity = "MustFix_Viewnotes/";
                    }
                    fileName.Append(severity);

                    final idDecl mapDecl = declManager.FindType(DECL_ENTITYDEF, mapSpawnData.serverInfo.GetString("si_map"), false);
                    final idDeclEntityDef mapInfo = (idDeclEntityDef) mapDecl;

                    if (mapInfo != null) {
                        fileName.Append(mapInfo.dict.GetString("devname"));
                    } else {
                        fileName.Append(mapSpawnData.serverInfo.GetString("si_map"));
                        fileName.StripFileExtension();
                    }

                    int count = guiTakeNotes.State().GetInt("person_numsel");
                    if (count == 0) {
                        fileList.Append(new idStr(fileName.toString() + "/Nobody"));
                    } else {
                        for (i = 0; i < count; i++) {
                            int person = guiTakeNotes.State().GetInt(va("person_sel_%d", i));
                            workName = new idStr(fileName + "/");
                            workName.oPluSet(guiTakeNotes.State().GetString(va("person_item_%d", person), "Nobody"));
                            fileList.Append(workName);
                        }
                    }
                } else {
                    fileName.Append("maps/");
                    fileName.Append(mapSpawnData.serverInfo.GetString("si_map"));
                    fileName.StripFileExtension();
                    fileList.Append(fileName);
                }

                boolean bCon = cvarSystem.GetCVarBool("con_noPrint");
                cvarSystem.SetCVarBool("con_noPrint", true);
                for (i = 0; i < fileList.Num(); i++) {
                    workName = fileList.oGet(i);
                    workName.Append("/");
                    workName.Append(p);
                    int[] workNote = {noteNumber[0]};
                    R_ScreenshotFilename(workNote, workName.toString(), shotName);

                    noteNum = shotName;
                    noteNum.StripPath();
                    noteNum.StripFileExtension();

                    if (severity != null && !severity.isEmpty()) {
                        workName = new idStr(severity);
                        workName.Append("viewNotes");
                    }

                    str = new idStr(String.format("recordViewNotes \"%s\" \"%s\" \"%s\"\n", workName.toString(), noteNum.toString(), guiTakeNotes.State().GetString("note")));

                    cmdSystem.BufferCommandText(CMD_EXEC_NOW, str.toString());
                    cmdSystem.ExecuteCommandBuffer();

                    UpdateScreen();
                    renderSystem.TakeScreenshot(renderSystem.GetScreenWidth(), renderSystem.GetScreenHeight(), shotName.toString(), 1, null);
                }
                noteNumber[0]++;

                for (int tries = 0; tries < 10; tries++) {
                    file = fileSystem.OpenExplicitFileWrite("p:/viewnotes/notenumber.dat");
                    if (file != null) {
                        break;
                    }
                    Sys_Sleep(500);
                }
                if (file != null) {
                    file.WriteInt(noteNumber[0]);//, 4);
                    fileSystem.CloseFile(file);
                }

                cmdSystem.BufferCommandText(CMD_EXEC_NOW, "closeViewNotes\n");
                cvarSystem.SetCVarBool("con_noPrint", bCon);
            }
        }

        public void GetSaveGameList(idStrList fileList, idList<fileTIME_T> fileTimes) {
            int i;
            idFileList files;

            // NOTE: no fs_game_base for savegames
            idStr game = new idStr(cvarSystem.GetCVarString("fs_game"));
            if (game.Length() != 0) {
                files = fileSystem.ListFiles("savegames", ".save", false, false, game.toString());
            } else {
                files = fileSystem.ListFiles("savegames", ".save");
            }

            fileList.oSet(files.GetList());
            fileSystem.FreeFileList(files);

            for (i = 0; i < fileList.Num(); i++) {
                long[] timeStamp = {0};

                fileSystem.ReadFile("savegames/" + fileList.oGet(i), null, timeStamp);
                fileList.oGet(i).StripLeading('/');
                fileList.oGet(i).StripFileExtension();

                fileTIME_T ft = new fileTIME_T();
                ft.index = i;
                ft.timeStamp = timeStamp[0];
                fileTimes.Append(ft);
            }

            fileTimes.Sort(new idListSaveGameCompare());
        }

        private static final String[] PEOPLE = {
            "Tim", "Kenneth", "Robert",
            "Matt", "Mal", "Jerry", "Steve", "Pat",
            "Xian", "Ed", "Fred", "James", "Eric", "Andy", "Seneca", "Patrick", "Kevin",
            "MrElusive", "Jim", "Brian", "John", "Adrian", "Nobody"
        };

        private static final int NUM_PEOPLE = PEOPLE.length;

        public void TakeNotes(final String p, boolean extended /*= false*/) {
            if (!mapSpawned) {
                common.Printf("No map loaded!\n");
                return;
            }

            if (extended) {
                guiTakeNotes = uiManager.FindGui("guis/takeNotes2.gui", true, false, true);

//                final String[] people;
//                if (false) {
//                    people = new String[]{
//                        "Nobody", "Adam", "Brandon", "David", "PHook", "Jay", "Jake",
//                        "PatJ", "Brett", "Ted", "Darin", "Brian", "Sean"
//                    };
//                } else {
//                    people = new String[]{
//                        "Tim", "Kenneth", "Robert",
//                        "Matt", "Mal", "Jerry", "Steve", "Pat",
//                        "Xian", "Ed", "Fred", "James", "Eric", "Andy", "Seneca", "Patrick", "Kevin",
//                        "MrElusive", "Jim", "Brian", "John", "Adrian", "Nobody"
//                    };
//                }
//                
//                final int numPeople = PEOPLE.length;
//
                idListGUI guiList_people = uiManager.AllocListGUI();
                guiList_people.Config(guiTakeNotes, "person");
                for (int i = 0; i < NUM_PEOPLE; i++) {
                    guiList_people.Push(new idStr(PEOPLE[i]));
                }
                uiManager.FreeListGUI(guiList_people);

            } else {
                guiTakeNotes = uiManager.FindGui("guis/takeNotes.gui", true, false, true);
            }

            SetGUI(guiTakeNotes, null);
            guiActive.SetStateString("note", "");
            guiActive.SetStateString("notefile", p);
            guiActive.SetStateBool("extended", extended);
            guiActive.Activate(true, com_frameTime);
        }

        public void TakeNotes(final String p) {
            TakeNotes(p, false);
        }

        public void UpdateMPLevelShot() {
//            char[] screenshot = new char[MAX_STRING_CHARS];
            String[] screenshot = {null};
            fileSystem.FindMapScreenshot(cvarSystem.GetCVarString("si_map"), screenshot, MAX_STRING_CHARS);
            guiMainMenu.SetStateString("current_levelshot", screenshot[0]);
        }

        public void SetSaveGameGuiVars() {
            int i;
            idStr name;
            idStrList fileList = new idStrList();
            idList<fileTIME_T> fileTimes = new idList<>();

            loadGameList.Clear();
            fileList.Clear();
            fileTimes.Clear();

            GetSaveGameList(fileList, fileTimes);

            loadGameList.SetNum(fileList.Num());
            for (i = 0; i < fileList.Num(); i++) {
                loadGameList.oSet(i, fileList.oGet(fileTimes.oGet(i).index));

                idLexer src = new idLexer(LEXFL_NOERRORS | LEXFL_NOSTRINGCONCAT);
                if (src.LoadFile(va("savegames/%s.txt", loadGameList.oGet(i)))) {
                    idToken tok = new idToken();
                    src.ReadToken(tok);
                    name = tok;
                } else {
                    name = loadGameList.oGet(i);
                }

                name.Append("\t");

                String date = Sys_TimeStampToStr(fileTimes.oGet(i).timeStamp);
                name.Append(date);

                guiActive.SetStateString(va("loadgame_item_%d", i), name.toString());
            }
            guiActive.DeleteStateVar(va("loadgame_item_%d", fileList.Num()));

            guiActive.SetStateString("loadgame_sel_0", "-1");
            guiActive.SetStateString("loadgame_shot", "guis/assets/blankLevelShot");

        }

        public void SetMainMenuGuiVars() {

            guiMainMenu.SetStateString("serverlist_sel_0", "-1");
            guiMainMenu.SetStateString("serverlist_selid_0", "-1");

            guiMainMenu.SetStateInt("com_machineSpec", com_machineSpec.GetInteger());

            // "inetGame" will hold a hand-typed inet address, which is not archived to a cvar
            guiMainMenu.SetStateString("inetGame", "");

            // key bind names
            guiMainMenu.SetKeyBindingNames();

            // flag for in-game menu
            if (mapSpawned) {
                guiMainMenu.SetStateString("inGame", IsMultiplayer() ? "2" : "1");
            } else {
                guiMainMenu.SetStateString("inGame", "0");
            }

            SetCDKeyGuiVars();
            if (ID_DEMO_BUILD) {
                guiMainMenu.SetStateString("nightmare", "0");
            } else {
                guiMainMenu.SetStateString("nightmare", cvarSystem.GetCVarBool("g_nightmare") ? "1" : "0");
            }
            guiMainMenu.SetStateString("browser_levelshot", "guis/assets/splash/pdtempa");

            SetMainMenuSkin();
            // Mods Menu
            SetModsMenuGuiVars();

            guiMsg.SetStateString("visible_hasxp", fileSystem.HasD3XP() ? "1" : "0");

            if (__linux__) {
                guiMainMenu.SetStateString("driver_prompt", "1");
            } else {
                guiMainMenu.SetStateString("driver_prompt", "0");
            }

            SetPbMenuGuiVars();
        }

        public void SetModsMenuGuiVars() {
            int i;
            idModList list = fileSystem.ListMods();

            modsList.SetNum(list.GetNumMods());

            // Build the gui list
            for (i = 0; i < list.GetNumMods(); i++) {
                guiActive.SetStateString(va("modsList_item_%d", i), list.GetDescription(i));
                modsList.oSet(i, list.GetMod(i));
            }
            guiActive.DeleteStateVar(va("modsList_item_%d", list.GetNumMods()));
            guiActive.SetStateString("modsList_sel_0", "-1");

            fileSystem.FreeModList(list);
        }

        public void SetMainMenuSkin() {
            // skins
            idStr str = new idStr(cvarSystem.GetCVarString("mod_validSkins"));
            idStr uiSkin = new idStr(cvarSystem.GetCVarString("ui_skin"));
            idStr skin;
            int skinId = 1;
            int count = 1;
            while (str.Length() != 0) {
                int n = str.Find(";");
                if (n >= 0) {
                    skin = str.Left(n);
                    str = str.Right(str.Length() - n - 1);
                } else {
                    skin = str;
                    str.oSet("");
                }
                if (skin.Icmp(uiSkin.toString()) == 0) {
                    skinId = count;
                }
                count++;
            }

            for (int i = 0; i < count; i++) {
                guiMainMenu.SetStateInt(va("skin%d", i + 1), 0);
            }
            guiMainMenu.SetStateInt(va("skin%d", skinId), 1);
        }

        public void SetPbMenuGuiVars() {
        }

        private boolean BoxDialogSanityCheck() {
            if (!common.IsInitialized()) {
                common.DPrintf("message box sanity check: !common.IsInitialized()\n");
                return false;
            }
            if (NOT(guiMsg)) {
                return false;
            }
            if (guiMsgRestore != null) {
                common.DPrintf("message box sanity check: recursed\n");
                return false;
            }
            if (cvarSystem.GetCVarInteger("net_serverDedicated") != 0) {
                common.DPrintf("message box sanity check: not compatible with dedicated server\n");
                return false;
            }
            return true;
        }

        /*
         ===============
         idSessionLocal::EmitGameAuth
         we toggled some key state to CDKEY_CHECKING. send a standalone auth packet to validate
         ===============
         */
        private void EmitGameAuth() {
            // make sure the auth reply is empty, we use it to indicate an auth reply
            authMsg.Empty();
            if (idAsyncNetwork.client.SendAuthCheck(cdkey_state == CDKEY_CHECKING ? ctos(cdkey) : null, xpkey_state == CDKEY_CHECKING ? ctos(xpkey) : null)) {
                authEmitTimeout = Sys_Milliseconds() + CDKEY_AUTH_TIMEOUT;
                common.DPrintf("authing with the master..\n");
            } else {
                // net is not available
                common.DPrintf("sendAuthCheck failed\n");
                if (cdkey_state == CDKEY_CHECKING) {
                    cdkey_state = CDKEY_OK;
                }
                if (xpkey_state == CDKEY_CHECKING) {
                    xpkey_state = CDKEY_OK;
                }
            }
        }
    };
}
