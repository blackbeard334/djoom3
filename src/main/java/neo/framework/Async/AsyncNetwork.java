package neo.framework.Async;

import static neo.Game.Game_local.game;
import static neo.Renderer.RenderSystem.renderSystem;
import static neo.Sound.snd_system.soundSystem;
import static neo.TempDump.ctos;
import neo.framework.Async.AsyncClient.idAsyncClient;
import neo.framework.Async.AsyncServer.idAsyncServer;
import static neo.framework.BuildDefines.ID_DEDICATED;
import static neo.framework.BuildDefines.ID_DEMO_BUILD;
import static neo.framework.CVarSystem.CVAR_ARCHIVE;
import static neo.framework.CVarSystem.CVAR_BOOL;
import static neo.framework.CVarSystem.CVAR_INIT;
import static neo.framework.CVarSystem.CVAR_INTEGER;
import static neo.framework.CVarSystem.CVAR_NETWORKSYNC;
import static neo.framework.CVarSystem.CVAR_NOCHEAT;
import static neo.framework.CVarSystem.CVAR_ROM;
import static neo.framework.CVarSystem.CVAR_SERVERINFO;
import static neo.framework.CVarSystem.CVAR_SYSTEM;
import static neo.framework.CVarSystem.cvarSystem;
import neo.framework.CVarSystem.idCVar;
import static neo.framework.CmdSystem.CMD_FL_SYSTEM;
import static neo.framework.CmdSystem.cmdExecution_t.CMD_EXEC_NOW;
import neo.framework.CmdSystem.cmdFunction_t;
import static neo.framework.CmdSystem.cmdSystem;
import neo.framework.CmdSystem.idCmdSystem;
import static neo.framework.Common.com_asyncInput;
import static neo.framework.Common.common;
import static neo.framework.Console.console;
import static neo.framework.FileSystem_h.fileSystem;
import static neo.framework.Licensee.ASYNC_PROTOCOL_MAJOR;
import static neo.framework.Licensee.IDNET_HOST;
import static neo.framework.Licensee.IDNET_MASTER_PORT;
import static neo.framework.Session.session;
import static neo.framework.UsercmdGen.BUTTON_ATTACK;
import static neo.framework.UsercmdGen.inhibit_t.INHIBIT_ASYNC;
import static neo.framework.UsercmdGen.usercmdGen;
import neo.framework.UsercmdGen.usercmd_t;
import neo.idlib.BitMsg.idBitMsg;
import neo.idlib.CmdArgs.idCmdArgs;
import neo.idlib.Lib.idException;
import neo.idlib.Text.Str.idStr;
import neo.sys.sys_public.netadr_t;
import static neo.sys.win_input.Sys_GrabMouseCursor;
import static neo.sys.win_net.Sys_StringToNetAdr;
import static neo.sys.win_syscon.Sys_ShowConsole;

/**
 *
 */
public class AsyncNetwork {
    /*
     DOOM III gold:	33
     1.1 beta patch:	34
     1.1 patch:		35
     1.2 XP:			36-39
     1.3 patch:		40
     1.3.1:			41
     */

    public static final int ASYNC_PROTOCOL_MINOR   = 41;
    public static final int ASYNC_PROTOCOL_VERSION = (ASYNC_PROTOCOL_MAJOR << 16) + ASYNC_PROTOCOL_MINOR;

    public static int MAJOR_VERSION(final int v) {
        return (v >> 16);
    }

    //
    public static final int MAX_ASYNC_CLIENTS       = 32;
    //
    public static final int MAX_USERCMD_BACKUP      = 256;
    public static final int MAX_USERCMD_DUPLICATION = 25;
    public static final int MAX_USERCMD_RELAY       = 10;
    //
    // index 0 is hardcoded to be the idnet master
    // which leaves 4 to user customization
    public static final int MAX_MASTER_SERVERS      = 5;
    //
    public static final int MAX_NICKLEN             = 32;
    //
    // max number of servers that will be scanned for at a single IP address
    public static final int MAX_SERVER_PORTS        = 8;
    //
    // special game init ids
    public static final int GAME_INIT_ID_INVALID    = -1;
    public static final int GAME_INIT_ID_MAP_LOAD   = -2;
//

    /*
     ===============================================================================

     Asynchronous Networking.

     ===============================================================================
     */
// unreliable server -> client messages
    public enum SERVER_UNRELIABLE {

        SERVER_UNRELIABLE_MESSAGE_EMPTY,
        SERVER_UNRELIABLE_MESSAGE_PING,
        SERVER_UNRELIABLE_MESSAGE_GAMEINIT,
        SERVER_UNRELIABLE_MESSAGE_SNAPSHOT
    };

    // reliable server -> client messages
    public enum SERVER_RELIABLE {

        SERVER_RELIABLE_MESSAGE_PURE,
        SERVER_RELIABLE_MESSAGE_RELOAD,
        SERVER_RELIABLE_MESSAGE_CLIENTINFO,
        SERVER_RELIABLE_MESSAGE_SYNCEDCVARS,
        SERVER_RELIABLE_MESSAGE_PRINT,
        SERVER_RELIABLE_MESSAGE_DISCONNECT,
        SERVER_RELIABLE_MESSAGE_APPLYSNAPSHOT,
        SERVER_RELIABLE_MESSAGE_GAME,
        SERVER_RELIABLE_MESSAGE_ENTERGAME
    };

    // unreliable client -> server messages
    public enum CLIENT_UNRELIABLE {

        CLIENT_UNRELIABLE_MESSAGE_EMPTY,
        CLIENT_UNRELIABLE_MESSAGE_PINGRESPONSE,
        CLIENT_UNRELIABLE_MESSAGE_USERCMD
    };

    // reliable client -> server messages
    public enum CLIENT_RELIABLE {

        CLIENT_RELIABLE_MESSAGE_PURE,
        CLIENT_RELIABLE_MESSAGE_CLIENTINFO,
        CLIENT_RELIABLE_MESSAGE_PRINT,
        CLIENT_RELIABLE_MESSAGE_DISCONNECT,
        CLIENT_RELIABLE_MESSAGE_GAME
    };

    // server print messages
    public enum SERVER_PRINT {

        SERVER_PRINT_MISC,
        SERVER_PRINT_BADPROTOCOL,
        SERVER_PRINT_RCON,
        SERVER_PRINT_GAMEDENY,
        SERVER_PRINT_BADCHALLENGE
    };

    public enum SERVER_DL {

        _0_,
        SERVER_DL_REDIRECT,
        SERVER_DL_LIST,
        SERVER_DL_NONE
    };

    public enum SERVER_PAK {

        SERVER_PAK_NO,
        SERVER_PAK_YES,
        SERVER_PAK_END
    };

    static class master_s {

        idCVar   var;
        netadr_t address;
        boolean  resolved;
    }/*master_t*/;

    public static class idAsyncNetwork {

        public static final idAsyncServer server      = new idAsyncServer();
        public static final idAsyncClient client      = new idAsyncClient();
        //
        public static final idCVar verbose                     = new idCVar("net_verbose", "0", CVAR_SYSTEM | CVAR_INTEGER | CVAR_NOCHEAT, "1 = verbose output, 2 = even more verbose output", 0, 2, new idCmdSystem.ArgCompletion_Integer(0, 2));
        public static final idCVar allowCheats                 = new idCVar("net_allowCheats", "0", CVAR_SYSTEM | CVAR_BOOL | CVAR_NETWORKSYNC, "Allow cheats in network game");
        public static final idCVar serverDedicated;// if set run a dedicated server
        public static final idCVar serverSnapshotDelay         = new idCVar("net_serverSnapshotDelay", "50", CVAR_SYSTEM | CVAR_INTEGER | CVAR_NOCHEAT, "delay between snapshots in milliseconds");
        public static final idCVar serverMaxClientRate         = new idCVar("net_serverMaxClientRate", "16000", CVAR_SYSTEM | CVAR_INTEGER | CVAR_ARCHIVE | CVAR_NOCHEAT, "maximum rate to a client in bytes/sec");
        public static final idCVar clientMaxRate               = new idCVar("net_clientMaxRate", "16000", CVAR_SYSTEM | CVAR_INTEGER | CVAR_ARCHIVE | CVAR_NOCHEAT, "maximum rate requested by client from server in bytes/sec");
        public static final idCVar serverMaxUsercmdRelay       = new idCVar("net_serverMaxUsercmdRelay", "5", CVAR_SYSTEM | CVAR_INTEGER | CVAR_NOCHEAT, "maximum number of usercmds from other clients the server relays to a client", 1, MAX_USERCMD_RELAY, new idCmdSystem.ArgCompletion_Integer(1, MAX_USERCMD_RELAY));
        public static final idCVar serverZombieTimeout         = new idCVar("net_serverZombieTimeout", "5", CVAR_SYSTEM | CVAR_INTEGER | CVAR_NOCHEAT, "disconnected client timeout in seconds");
        public static final idCVar serverClientTimeout         = new idCVar("net_serverClientTimeout", "40", CVAR_SYSTEM | CVAR_INTEGER | CVAR_NOCHEAT, "client time out in seconds");
        public static final idCVar clientServerTimeout         = new idCVar("net_clientServerTimeout", "40", CVAR_SYSTEM | CVAR_INTEGER | CVAR_NOCHEAT, "server time out in seconds");
        public static final idCVar serverDrawClient            = new idCVar("net_serverDrawClient", "-1", CVAR_SYSTEM | CVAR_INTEGER, "number of client for which to draw view on server");
        public static final idCVar serverRemoteConsolePassword = new idCVar("net_serverRemoteConsolePassword", "", CVAR_SYSTEM | CVAR_NOCHEAT, "remote console password");
        public static final idCVar clientPrediction            = new idCVar("net_clientPrediction", "16", CVAR_SYSTEM | CVAR_INTEGER | CVAR_NOCHEAT, "additional client side prediction in milliseconds");
        public static final idCVar clientMaxPrediction         = new idCVar("net_clientMaxPrediction", "1000", CVAR_SYSTEM | CVAR_INTEGER | CVAR_NOCHEAT, "maximum number of milliseconds a client can predict ahead of server.");
        public static final idCVar clientUsercmdBackup         = new idCVar("net_clientUsercmdBackup", "5", CVAR_SYSTEM | CVAR_INTEGER | CVAR_NOCHEAT, "number of usercmds to resend");
        public static final idCVar clientRemoteConsoleAddress  = new idCVar("net_clientRemoteConsoleAddress", "localhost", CVAR_SYSTEM | CVAR_NOCHEAT, "remote console address");
        public static final idCVar clientRemoteConsolePassword = new idCVar("net_clientRemoteConsolePassword", "", CVAR_SYSTEM | CVAR_NOCHEAT, "remote console password");
        public static final idCVar master0                     = new idCVar("net_master0", IDNET_HOST + ":" + IDNET_MASTER_PORT, CVAR_SYSTEM | CVAR_ROM, "idnet master server address");
        public static final idCVar master1                     = new idCVar("net_master1", "", CVAR_SYSTEM | CVAR_ARCHIVE, "1st master server address");
        public static final idCVar master2                     = new idCVar("net_master2", "", CVAR_SYSTEM | CVAR_ARCHIVE, "2nd master server address");
        public static final idCVar master3                     = new idCVar("net_master3", "", CVAR_SYSTEM | CVAR_ARCHIVE, "3rd master server address");
        public static final idCVar master4                     = new idCVar("net_master4", "", CVAR_SYSTEM | CVAR_ARCHIVE, "4th master server address");
        public static final idCVar LANServer                   = new idCVar("net_LANServer", "0", CVAR_SYSTEM | CVAR_BOOL | CVAR_NOCHEAT, "config LAN games only - affects clients and servers");
        public static final idCVar serverReloadEngine          = new idCVar("net_serverReloadEngine", "0", CVAR_SYSTEM | CVAR_INTEGER | CVAR_NOCHEAT, "perform a full reload on next map restart  = new idCVar(including flushing referenced pak files) - decreased if > 0");
        public static final idCVar serverAllowServerMod        = new idCVar("net_serverAllowServerMod", "0", CVAR_SYSTEM | CVAR_BOOL | CVAR_NOCHEAT, "allow server-side mods");
        public static final idCVar idleServer                  = new idCVar("si_idleServer", "0", CVAR_SYSTEM | CVAR_BOOL | CVAR_INIT | CVAR_SERVERINFO, "game clients are idle");
        public static final idCVar clientDownload              = new idCVar("net_clientDownload", "1", CVAR_SYSTEM | CVAR_INTEGER | CVAR_ARCHIVE, "client pk4 downloads policy: 0 - never, 1 - ask, 2 - always  = new idCVar(will still prompt for binary code)");
        //
        private static int realTime;
        private static final master_s[] masters = new master_s[MAX_MASTER_SERVERS];    // master1 etc.
        //    
        //

        static {
            if (ID_DEDICATED) {// dedicated executable can only have a value of 1 for net_serverDedicated
                serverDedicated = new idCVar("net_serverDedicated", "1", CVAR_SERVERINFO | CVAR_SYSTEM | CVAR_INTEGER | CVAR_NOCHEAT | CVAR_ROM, "");
            } else {
                serverDedicated = new idCVar("net_serverDedicated", "0", CVAR_SERVERINFO | CVAR_SYSTEM | CVAR_INTEGER | CVAR_NOCHEAT, "1 = text console dedicated server, 2 = graphical dedicated server", 0, 2, new idCmdSystem.ArgCompletion_Integer(0, 2));
            }

            for (int m = 0; m < masters.length; m++) {
                masters[m] = new master_s();
            }
        }

        public idAsyncNetwork() {
        }

        public static void Init() throws idException {

            realTime = 0;

//	memset( masters, 0, sizeof( masters ) );
            masters[0].var = master0;
            masters[1].var = master1;
            masters[2].var = master2;
            masters[3].var = master3;
            masters[4].var = master4;

            if (!ID_DEMO_BUILD) {//#ifndef
                cmdSystem.AddCommand("spawnServer", SpawnServer_f.getInstance(), CMD_FL_SYSTEM, "spawns a server", idCmdSystem.ArgCompletion_MapName.getInstance());
                cmdSystem.AddCommand("nextMap", NextMap_f.getInstance(), CMD_FL_SYSTEM, "loads the next map on the server");
                cmdSystem.AddCommand("connect", Connect_f.getInstance(), CMD_FL_SYSTEM, "connects to a server");
                cmdSystem.AddCommand("reconnect", Reconnect_f.getInstance(), CMD_FL_SYSTEM, "reconnect to the last server we tried to connect to");
                cmdSystem.AddCommand("serverInfo", GetServerInfo_f.getInstance(), CMD_FL_SYSTEM, "shows server info");
                cmdSystem.AddCommand("LANScan", GetLANServers_f.getInstance(), CMD_FL_SYSTEM, "scans LAN for servers");
                cmdSystem.AddCommand("listServers", ListServers_f.getInstance(), CMD_FL_SYSTEM, "lists scanned servers");
                cmdSystem.AddCommand("rcon", RemoteConsole_f.getInstance(), CMD_FL_SYSTEM, "sends remote console command to server");
                cmdSystem.AddCommand("heartbeat", Heartbeat_f.getInstance(), CMD_FL_SYSTEM, "send a heartbeat to the the master servers");
                cmdSystem.AddCommand("kick", Kick_f.getInstance(), CMD_FL_SYSTEM, "kick a client by connection number");
                cmdSystem.AddCommand("checkNewVersion", CheckNewVersion_f.getInstance(), CMD_FL_SYSTEM, "check if a new version of the game is available");
                cmdSystem.AddCommand("updateUI", UpdateUI_f.getInstance(), CMD_FL_SYSTEM, "internal - cause a sync down of game-modified userinfo");
            }
        }

        public static void Shutdown() {
            client.serverList.Shutdown();
            client.DisconnectFromServer();
            client.ClearServers();
            client.ClosePort();
            server.Kill();
            server.ClosePort();
        }

        public static boolean IsActive() {
            return (server.IsActive() || client.IsActive());
        }

        public static void RunFrame() {
            if (console.Active()) {
                Sys_GrabMouseCursor(false);
                usercmdGen.InhibitUsercmd(INHIBIT_ASYNC, true);
            } else {
                Sys_GrabMouseCursor(true);
                usercmdGen.InhibitUsercmd(INHIBIT_ASYNC, false);
            }
            client.RunFrame();
            server.RunFrame();
        }
//

        public static void WriteUserCmdDelta(idBitMsg msg, final usercmd_t cmd, final usercmd_t base) {
            if (base != null) {
                msg.WriteDeltaLongCounter(base.gameTime, cmd.gameTime);
                msg.WriteDeltaByte(base.buttons, cmd.buttons);
                msg.WriteDeltaShort(base.mx, cmd.mx);
                msg.WriteDeltaShort(base.my, cmd.my);
                msg.WriteDeltaChar(base.forwardmove, cmd.forwardmove);
                msg.WriteDeltaChar(base.rightmove, cmd.rightmove);
                msg.WriteDeltaChar(base.upmove, cmd.upmove);
                msg.WriteDeltaShort(base.angles[0], cmd.angles[0]);
                msg.WriteDeltaShort(base.angles[1], cmd.angles[1]);
                msg.WriteDeltaShort(base.angles[2], cmd.angles[2]);
                return;
            }

            msg.WriteLong(cmd.gameTime);
            msg.WriteByte(cmd.buttons);
            msg.WriteShort(cmd.mx);
            msg.WriteShort(cmd.my);
            msg.WriteChar(cmd.forwardmove);
            msg.WriteChar(cmd.rightmove);
            msg.WriteChar(cmd.upmove);
            msg.WriteShort(cmd.angles[0]);
            msg.WriteShort(cmd.angles[1]);
            msg.WriteShort(cmd.angles[2]);
        }

        public static void ReadUserCmdDelta(final idBitMsg msg, usercmd_t cmd, final usercmd_t base) throws idException {
//	memset( &cmd, 0, sizeof( cmd ) );

            if (base != null) {
                cmd.gameTime = msg.ReadDeltaLongCounter(base.gameTime);
                cmd.buttons = (byte) msg.ReadDeltaByte(base.buttons);
                cmd.mx = (short) msg.ReadDeltaShort(base.mx);
                cmd.my = (short) msg.ReadDeltaShort(base.my);
                cmd.forwardmove = (byte) msg.ReadDeltaChar(base.forwardmove);
                cmd.rightmove = (byte) msg.ReadDeltaChar(base.rightmove);
                cmd.upmove = (byte) msg.ReadDeltaChar(base.upmove);
                cmd.angles[0] = (short) msg.ReadDeltaShort(base.angles[0]);
                cmd.angles[1] = (short) msg.ReadDeltaShort(base.angles[1]);
                cmd.angles[2] = (short) msg.ReadDeltaShort(base.angles[2]);
                return;
            }

            cmd.gameTime = msg.ReadLong();
            cmd.buttons = (byte) msg.ReadByte();
            cmd.mx = (short) msg.ReadShort();
            cmd.my = (short) msg.ReadShort();
            cmd.forwardmove = (byte) msg.ReadChar();
            cmd.rightmove = (byte) msg.ReadChar();
            cmd.upmove = (byte) msg.ReadChar();
            cmd.angles[0] = (short) msg.ReadShort();
            cmd.angles[1] = (short) msg.ReadShort();
            cmd.angles[2] = (short) msg.ReadShort();
        }
//

        public static boolean DuplicateUsercmd(final usercmd_t previousUserCmd, usercmd_t currentUserCmd, int frame, int time) {

            if (currentUserCmd.gameTime <= previousUserCmd.gameTime) {

                currentUserCmd = previousUserCmd;
                currentUserCmd.gameFrame = frame;
                currentUserCmd.gameTime = time;
                currentUserCmd.duplicateCount++;

                if (currentUserCmd.duplicateCount > MAX_USERCMD_DUPLICATION) {
                    currentUserCmd.buttons &= ~BUTTON_ATTACK;
                    if (Math.abs(currentUserCmd.forwardmove) > 2) {
                        currentUserCmd.forwardmove >>= 1;
                    }
                    if (Math.abs(currentUserCmd.rightmove) > 2) {
                        currentUserCmd.rightmove >>= 1;
                    }
                    if (Math.abs(currentUserCmd.upmove) > 2) {
                        currentUserCmd.upmove >>= 1;
                    }
                }

                return true;
            }
            return false;
        }

        public static boolean UsercmdInputChanged(final usercmd_t previousUserCmd, final usercmd_t currentUserCmd) {
            return previousUserCmd.buttons != currentUserCmd.buttons
                    || previousUserCmd.forwardmove != currentUserCmd.forwardmove
                    || previousUserCmd.rightmove != currentUserCmd.rightmove
                    || previousUserCmd.upmove != currentUserCmd.upmove
                    || previousUserCmd.angles[0] != currentUserCmd.angles[0]
                    || previousUserCmd.angles[1] != currentUserCmd.angles[1]
                    || previousUserCmd.angles[2] != currentUserCmd.angles[2];
        }

//
        // returns true if the corresponding master is set to something (and could be resolved)
        public static boolean GetMasterAddress(int index, netadr_t adr) {
            if (null == masters[index].var) {
                return false;
            }
            if (masters[index].var.GetString().isEmpty()) {
                return false;
            }
            if (!masters[index].resolved || masters[index].var.IsModified()) {
                masters[index].var.ClearModified();
                if (!Sys_StringToNetAdr(masters[index].var.GetString(), masters[index].address, true)) {
                    common.Printf("Failed to resolve master%d: %s\n", index, masters[index].var.GetString());
                    masters[index].address = new netadr_t();//memset( &masters[ index ].address, 0, sizeof( netadr_t ) );
                    masters[index].resolved = true;
                    return false;
                }
                if (masters[index].address.port == 0) {
                    masters[index].address.port = IDNET_MASTER_PORT;
                }
                masters[index].resolved = true;
            }
            adr.oSet(masters[index].address);
            return true;
        }

        // get the hardcoded idnet master, equivalent to GetMasterAddress( 0, .. )
        public static netadr_t GetMasterAddress() {
            netadr_t ret = new netadr_t();
            GetMasterAddress(0, ret);
            return masters[0].address;
        }

        public static void GetNETServers() {
            client.GetNETServers();
        }

        public static void ExecuteSessionCommand(final String sessCmd) {
            if (!sessCmd.isEmpty()) {
                if (0 == idStr.Icmp(sessCmd, "game_startmenu")) {
                    session.SetGUI(game.StartMenu(), null);
                }
            }
        }

        public static void ExecuteSessionCommand(final char[] sessCmd) {
            ExecuteSessionCommand(ctos(sessCmd));
        }

        // same message used for offline check and network reply
        public static void BuildInvalidKeyMsg(idStr msg, boolean[] valid/*[2 ]*/) throws idException {
            if (!valid[0]) {
                msg.oPluSet(common.GetLanguageDict().GetString("#str_07194"));
            }
            if (fileSystem.HasD3XP() && !valid[1]) {
                if (msg.Length() != 0) {
                    msg.oPluSet("\n");
                }
                msg.oPluSet(common.GetLanguageDict().GetString("#str_07195"));
            }
            msg.oPluSet("\n");
            msg.oPluSet(common.GetLanguageDict().GetString("#str_04304"));
        }

        /*
         ==================
         idAsyncNetwork::SpawnServer_f
         ==================
         */
        private static class SpawnServer_f extends cmdFunction_t {

            private static final cmdFunction_t instance = new SpawnServer_f();

            private SpawnServer_f() {
            }

            public static cmdFunction_t getInstance() {
                return instance;
            }

            @Override
            public void run(idCmdArgs args) throws idException {

                if (args.Argc() > 1) {
                    cvarSystem.SetCVarString("si_map", args.Argv(1));
                }

                // don't let a server spawn with singleplayer game type - it will crash
                if (idStr.Icmp(cvarSystem.GetCVarString("si_gameType"), "singleplayer") == 0) {
                    cvarSystem.SetCVarString("si_gameType", "deathmatch");
                }
                com_asyncInput.SetBool(false);
                // make sure the current system state is compatible with net_serverDedicated
                switch (cvarSystem.GetCVarInteger("net_serverDedicated")) {
                    case 0:
                    case 2:
                        if (!renderSystem.IsOpenGLRunning()) {
                            common.Warning("OpenGL is not running, net_serverDedicated == %d", cvarSystem.GetCVarInteger("net_serverDedicated"));
                        }
                        break;
                    case 1:
                        if (renderSystem.IsOpenGLRunning()) {
                            Sys_ShowConsole(1, false);
                            renderSystem.ShutdownOpenGL();
                        }
                        soundSystem.SetMute(true);
                        soundSystem.ShutdownHW();
                        break;
                }
                // use serverMapRestart if we already have a running server
                if (server.IsActive()) {
                    cmdSystem.BufferCommandText(CMD_EXEC_NOW, "serverMapRestart");
                } else {
                    server.Spawn();
                }
            }
        };

        /*
         ==================
         idAsyncNetwork::NextMap_f
         ==================
         */
        private static class NextMap_f extends cmdFunction_t {

            private static final cmdFunction_t instance = new NextMap_f();

            private NextMap_f() {
            }

            public static cmdFunction_t getInstance() {
                return instance;
            }

            @Override
            public void run(idCmdArgs args) {
                server.ExecuteMapChange();
            }
        };

        /*
         ==================
         idAsyncNetwork::Connect_f
         ==================
         */
        private static class Connect_f extends cmdFunction_t {

            private static final cmdFunction_t instance = new Connect_f();

            private Connect_f() {
            }

            public static cmdFunction_t getInstance() {
                return instance;
            }

            @Override
            public void run(idCmdArgs args) throws idException {
                if (server.IsActive()) {
                    common.Printf("already running a server\n");
                    return;
                }
                if (args.Argc() != 2) {
                    common.Printf("USAGE: connect <serverName>\n");
                    return;
                }
                com_asyncInput.SetBool(false);
                client.ConnectToServer(args.Argv(1));
            }
        };

        /*
         ==================
         idAsyncNetwork::Reconnect_f
         ==================
         */
        private static class Reconnect_f extends cmdFunction_t {

            private static final cmdFunction_t instance = new Reconnect_f();

            private Reconnect_f() {
            }

            public static cmdFunction_t getInstance() {
                return instance;
            }

            @Override
            public void run(idCmdArgs args) {
                client.Reconnect();
            }
        };

        /*
         ==================
         idAsyncNetwork::GetServerInfo_f
         ==================
         */
        private static class GetServerInfo_f extends cmdFunction_t {

            private static final cmdFunction_t instance = new GetServerInfo_f();

            private GetServerInfo_f() {
            }

            public static cmdFunction_t getInstance() {
                return instance;
            }

            @Override
            public void run(idCmdArgs args) {
                client.GetServerInfo(args.Argv(1));
            }
        };

        /*
         ==================
         idAsyncNetwork::GetLANServers_f
         ==================
         */
        private static class GetLANServers_f extends cmdFunction_t {

            private static final cmdFunction_t instance = new GetLANServers_f();

            private GetLANServers_f() {
            }

            public static cmdFunction_t getInstance() {
                return instance;
            }

            @Override
            public void run(idCmdArgs args) {
                client.GetLANServers();
            }
        };

        /*
         ==================
         idAsyncNetwork::ListServers_f
         ==================
         */
        private static class ListServers_f extends cmdFunction_t {

            private static final cmdFunction_t instance = new ListServers_f();

            private ListServers_f() {
            }

            public static cmdFunction_t getInstance() {
                return instance;
            }

            @Override
            public void run(idCmdArgs args) {
                client.ListServers();
            }
        };

        /*
         ==================
         idAsyncNetwork::RemoteConsole_f
         ==================
         */
        private static class RemoteConsole_f extends cmdFunction_t {

            private static final cmdFunction_t instance = new RemoteConsole_f();

            private RemoteConsole_f() {
            }

            public static cmdFunction_t getInstance() {
                return instance;
            }

            @Override
            public void run(idCmdArgs args) {
                client.RemoteConsole(args.Args());
            }
        };

        /*
         ==================
         idAsyncNetwork::Heartbeat_f
         ==================
         */
        private static class Heartbeat_f extends cmdFunction_t {

            private static final cmdFunction_t instance = new Heartbeat_f();

            private Heartbeat_f() {
            }

            public static cmdFunction_t getInstance() {
                return instance;
            }

            @Override
            public void run(idCmdArgs args) {
                if (!server.IsActive()) {
                    common.Printf("server is not running\n");
                    return;
                }
                server.MasterHeartbeat(true);
            }
        };

        /*
         ==================
         idAsyncNetwork::Kick_f
         ==================
         */
        private static class Kick_f extends cmdFunction_t {

            private static final cmdFunction_t instance = new Kick_f();

            private Kick_f() {
            }

            public static cmdFunction_t getInstance() {
                return instance;
            }

            @Override
            public void run(idCmdArgs args) {
                idStr clientId;
                int iclient;

                if (!server.IsActive()) {
                    common.Printf("server is not running\n");
                    return;
                }

                clientId = new idStr(args.Argv(1));
                if (!clientId.IsNumeric()) {
                    common.Printf("usage: kick <client number>\n");
                    return;
                }
                iclient = Integer.parseInt(clientId.toString());

                if (server.GetLocalClientNum() == iclient) {
                    common.Printf("can't kick the host\n");
                    return;
                }

                server.DropClient(iclient, "#str_07134");
            }
        };


        /*
         ==================
         idAsyncNetwork::CheckNewVersion_f
         ==================
         */
        private static class CheckNewVersion_f extends cmdFunction_t {

            private static final cmdFunction_t instance = new CheckNewVersion_f();

            private CheckNewVersion_f() {
            }

            public static cmdFunction_t getInstance() {
                return instance;
            }

            @Override
            public void run(idCmdArgs args) {
                client.SendVersionCheck();
            }
        };

        /*
         =================
         idAsyncNetwork::UpdateUI_f
         =================
         */
        private static class UpdateUI_f extends cmdFunction_t {

            private static final cmdFunction_t instance = new UpdateUI_f();

            private UpdateUI_f() {
            }

            public static cmdFunction_t getInstance() {
                return instance;
            }

            @Override
            public void run(idCmdArgs args) {
                if (args.Argc() != 2) {
                    common.Warning("idAsyncNetwork::UpdateUI_f: wrong arguments\n");
                    return;
                }
                if (!server.IsActive()) {
                    common.Warning("idAsyncNetwork::UpdateUI_f: server is not active\n");
                    return;
                }
                int clientNum = Integer.parseInt(args.Args(1));
                server.UpdateUI(clientNum);
            }
        };
    };
}
