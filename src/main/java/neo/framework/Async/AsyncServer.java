package neo.framework.Async;

import static java.lang.Math.random;
import static neo.Game.Game.allowReply_t.ALLOW_YES;
import static neo.Game.Game_local.game;
import static neo.TempDump.ctos;
import static neo.TempDump.etoi;
import static neo.TempDump.isNotNullOrEmpty;
import static neo.TempDump.sizeof;
import static neo.TempDump.strLen;
import static neo.framework.Async.AsyncNetwork.ASYNC_PROTOCOL_MINOR;
import static neo.framework.Async.AsyncNetwork.ASYNC_PROTOCOL_VERSION;
import static neo.framework.Async.AsyncNetwork.GAME_INIT_ID_MAP_LOAD;
import static neo.framework.Async.AsyncNetwork.MAX_ASYNC_CLIENTS;
import static neo.framework.Async.AsyncNetwork.MAX_MASTER_SERVERS;
import static neo.framework.Async.AsyncNetwork.MAX_USERCMD_BACKUP;
import static neo.framework.Async.AsyncNetwork.MAX_USERCMD_RELAY;
import static neo.framework.Async.AsyncNetwork.SERVER_DL.SERVER_DL_LIST;
import static neo.framework.Async.AsyncNetwork.SERVER_DL.SERVER_DL_NONE;
import static neo.framework.Async.AsyncNetwork.SERVER_DL.SERVER_DL_REDIRECT;
import static neo.framework.Async.AsyncNetwork.SERVER_PAK.SERVER_PAK_END;
import static neo.framework.Async.AsyncNetwork.SERVER_PAK.SERVER_PAK_NO;
import static neo.framework.Async.AsyncNetwork.SERVER_PAK.SERVER_PAK_YES;
import static neo.framework.Async.AsyncNetwork.SERVER_PRINT.SERVER_PRINT_BADCHALLENGE;
import static neo.framework.Async.AsyncNetwork.SERVER_PRINT.SERVER_PRINT_BADPROTOCOL;
import static neo.framework.Async.AsyncNetwork.SERVER_PRINT.SERVER_PRINT_GAMEDENY;
import static neo.framework.Async.AsyncNetwork.SERVER_PRINT.SERVER_PRINT_MISC;
import static neo.framework.Async.AsyncNetwork.SERVER_PRINT.SERVER_PRINT_RCON;
import static neo.framework.Async.AsyncNetwork.SERVER_RELIABLE.SERVER_RELIABLE_MESSAGE_APPLYSNAPSHOT;
import static neo.framework.Async.AsyncNetwork.SERVER_RELIABLE.SERVER_RELIABLE_MESSAGE_CLIENTINFO;
import static neo.framework.Async.AsyncNetwork.SERVER_RELIABLE.SERVER_RELIABLE_MESSAGE_DISCONNECT;
import static neo.framework.Async.AsyncNetwork.SERVER_RELIABLE.SERVER_RELIABLE_MESSAGE_ENTERGAME;
import static neo.framework.Async.AsyncNetwork.SERVER_RELIABLE.SERVER_RELIABLE_MESSAGE_GAME;
import static neo.framework.Async.AsyncNetwork.SERVER_RELIABLE.SERVER_RELIABLE_MESSAGE_PRINT;
import static neo.framework.Async.AsyncNetwork.SERVER_RELIABLE.SERVER_RELIABLE_MESSAGE_PURE;
import static neo.framework.Async.AsyncNetwork.SERVER_RELIABLE.SERVER_RELIABLE_MESSAGE_RELOAD;
import static neo.framework.Async.AsyncNetwork.SERVER_RELIABLE.SERVER_RELIABLE_MESSAGE_SYNCEDCVARS;
import static neo.framework.Async.AsyncNetwork.SERVER_UNRELIABLE.SERVER_UNRELIABLE_MESSAGE_EMPTY;
import static neo.framework.Async.AsyncNetwork.SERVER_UNRELIABLE.SERVER_UNRELIABLE_MESSAGE_GAMEINIT;
import static neo.framework.Async.AsyncNetwork.SERVER_UNRELIABLE.SERVER_UNRELIABLE_MESSAGE_PING;
import static neo.framework.Async.AsyncNetwork.SERVER_UNRELIABLE.SERVER_UNRELIABLE_MESSAGE_SNAPSHOT;
import static neo.framework.Async.AsyncServer.authReplyMsg_t.AUTH_REPLY_MAXSTATES;
import static neo.framework.Async.AsyncServer.authReplyMsg_t.AUTH_REPLY_PRINT;
import static neo.framework.Async.AsyncServer.authReplyMsg_t.AUTH_REPLY_UNKNOWN;
import static neo.framework.Async.AsyncServer.authReplyMsg_t.AUTH_REPLY_WAITING;
import static neo.framework.Async.AsyncServer.authReply_t.AUTH_DENY;
import static neo.framework.Async.AsyncServer.authReply_t.AUTH_MAXSTATES;
import static neo.framework.Async.AsyncServer.authReply_t.AUTH_NONE;
import static neo.framework.Async.AsyncServer.authReply_t.AUTH_OK;
import static neo.framework.Async.AsyncServer.authState_t.CDK_OK;
import static neo.framework.Async.AsyncServer.authState_t.CDK_ONLYLAN;
import static neo.framework.Async.AsyncServer.authState_t.CDK_PUREOK;
import static neo.framework.Async.AsyncServer.authState_t.CDK_PUREWAIT;
import static neo.framework.Async.AsyncServer.authState_t.CDK_WAIT;
import static neo.framework.Async.AsyncServer.serverClientState_t.SCS_CONNECTED;
import static neo.framework.Async.AsyncServer.serverClientState_t.SCS_FREE;
import static neo.framework.Async.AsyncServer.serverClientState_t.SCS_INGAME;
import static neo.framework.Async.AsyncServer.serverClientState_t.SCS_PUREWAIT;
import static neo.framework.Async.AsyncServer.serverClientState_t.SCS_ZOMBIE;
import static neo.framework.Async.MsgChannel.CONNECTIONLESS_MESSAGE_ID;
import static neo.framework.Async.MsgChannel.CONNECTIONLESS_MESSAGE_ID_MASK;
import static neo.framework.Async.MsgChannel.MAX_MESSAGE_SIZE;
import static neo.framework.BuildDefines.ID_CLIENTINFO_TAGS;
import static neo.framework.CVarSystem.CVAR_CHEAT;
import static neo.framework.CVarSystem.CVAR_NETWORKSYNC;
import static neo.framework.CVarSystem.CVAR_USERINFO;
import static neo.framework.CVarSystem.cvarSystem;
import static neo.framework.CmdSystem.cmdSystem;
import static neo.framework.CmdSystem.cmdExecution_t.CMD_EXEC_APPEND;
import static neo.framework.CmdSystem.cmdExecution_t.CMD_EXEC_NOW;
import static neo.framework.Common.com_showAsyncStats;
import static neo.framework.Common.common;
import static neo.framework.DeclManager.declManager;
import static neo.framework.FileSystem_h.MAX_PURE_PAKS;
import static neo.framework.FileSystem_h.fileSystem;
import static neo.framework.Licensee.ASYNC_PROTOCOL_MAJOR;
import static neo.framework.Licensee.NUM_SERVER_PORTS;
import static neo.framework.Licensee.PORT_SERVER;
import static neo.framework.Session.sessLocal;
import static neo.framework.Session.session;
import static neo.framework.Session.msgBoxType_t.MSG_OK;
import static neo.framework.UsercmdGen.USERCMD_MSEC;
import static neo.framework.UsercmdGen.usercmdGen;
import static neo.idlib.Lib.MAX_STRING_CHARS;
import static neo.idlib.Lib.Max;
import static neo.idlib.Lib.Min;
import static neo.idlib.Text.Str.va;
import static neo.sys.sys_public.netadrtype_t.NA_BAD;
import static neo.sys.win_main.Sys_Sleep;
import static neo.sys.win_net.Sys_CompareNetAdrBase;
import static neo.sys.win_net.Sys_IsLANAddress;
import static neo.sys.win_net.Sys_NetAdrToString;
import static neo.sys.win_shared.Sys_Milliseconds;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Arrays;

import neo.TempDump.void_callback;
import neo.Game.Game.allowReply_t;
import neo.Game.Game.gameReturn_t;
import neo.framework.FileSystem_h.findFile_t;
import neo.framework.UsercmdGen.usercmd_t;
import neo.framework.Async.AsyncNetwork.CLIENT_RELIABLE;
import neo.framework.Async.AsyncNetwork.CLIENT_UNRELIABLE;
import neo.framework.Async.AsyncNetwork.idAsyncNetwork;
import neo.framework.Async.MsgChannel.idMsgChannel;
import neo.idlib.BitMsg.idBitMsg;
import neo.idlib.Dict_h.idDict;
import neo.idlib.Lib.idException;
import neo.idlib.Text.Str.idStr;
import neo.idlib.containers.StrList.idStrList;
import neo.idlib.math.Math_h.idMath;
import neo.sys.sys_public.idPort;
import neo.sys.sys_public.netadr_t;

/**
 *
 */
public class AsyncServer {

    /*
     ===============================================================================

     Network Server for asynchronous networking.

     ===============================================================================
     */
    // MAX_CHALLENGES is made large to prevent a denial of service attack that could cycle
    // all of them out before legitimate users connected
    static final int MAX_CHALLENGES    = 1024;
    //
    // if we don't hear from authorize server, assume it is down
    static final int AUTHORIZE_TIMEOUT = 5000;

    // states for the server's authorization process
    enum authState_t {

        CDK_WAIT, // we are waiting for a confirm/deny from auth
        // this is subject to timeout if we don't hear from auth
        // or a permanent wait if auth said so
        CDK_OK,
        CDK_ONLYLAN,
        CDK_PUREWAIT,
        CDK_PUREOK,
        CDK_MAXSTATES
    };

    // states from the auth server, while the client is in CDK_WAIT
    enum authReply_t {

        AUTH_NONE, // no reply yet
        AUTH_OK, // this client is good
        AUTH_WAIT, // wait - keep sending me srvAuth though
        AUTH_DENY, // denied - don't send me anything about this client anymore
        AUTH_MAXSTATES
    };

    // message from auth to be forwarded back to the client
    // some are locally hardcoded to save space, auth has the possibility to send a custom reply
    enum authReplyMsg_t {

        AUTH_REPLY_WAITING, // waiting on an initial reply from auth
        AUTH_REPLY_UNKNOWN, // client unknown to auth
        AUTH_REPLY_DENIED, // access denied
        AUTH_REPLY_PRINT, // custom message
        AUTH_REPLY_SRVWAIT, // auth server replied and tells us he's working on it
        AUTH_REPLY_MAXSTATES
    };

    static class challenge_s {

        netadr_t       address;        // client address
        int            clientId;       // client identification
        int            challenge;      // challenge code
        int            time;           // time the challenge was created
        int            pingTime;       // time the challenge response was sent to client
        boolean        connected;      // true if the client is connected
        authState_t    authState;      // local state regarding the client
        authReply_t    authReply;      // cd key check replies
        authReplyMsg_t authReplyMsg;   // default auth messages
        idStr          authReplyPrint; // custom msg
        char[] guid = new char[12];    // guid
        int OS;

        challenge_s() {
            this.authReplyPrint = new idStr();
        }
    } /*challenge_t*/

    ;

    enum serverClientState_t {

        SCS_FREE, // can be reused for a new connection
        SCS_ZOMBIE, // client has been disconnected, but don't reuse connection for a couple seconds
        SCS_PUREWAIT, // client needs to update it's pure checksums before we can go further
        SCS_CONNECTED, // client is connected
        SCS_INGAME            // client is in the game
    };

    static class serverClient_s {

        int                 OS;
        int                 clientId;
        serverClientState_t clientState;
        int                 clientPrediction;
        int                 clientAheadTime;
        int                 clientRate;
        int                 clientPing;
        int                 gameInitSequence;
        int                 gameFrame;
        int                 gameTime;
        idMsgChannel channel = new idMsgChannel();
        int lastConnectTime;
        int lastEmptyTime;
        int lastPingTime;
        int lastSnapshotTime;
        int lastPacketTime;
        int lastInputTime;
        int snapshotSequence;
        int acknowledgeSnapshotSequence;
        int numDuplicatedUsercmds;
        char[] guid = new char[12];     // Even Balance - M. Quinn

        boolean isClientConnected() {
            return etoi(clientState) < etoi(SCS_CONNECTED);
        }
    }/* serverClient_t*/

    ;
    //
    static final int      MIN_RECONNECT_TIME = 2000;
    static final int      EMPTY_RESEND_TIME  = 500;
    static final int      PING_RESEND_TIME   = 500;
    static final int      NOINPUT_IDLE_TIME  = 30000;
    //
    static final int      HEARTBEAT_MSEC     = 5 * 60 * 1000;
    //
    // must be kept in sync with authReplyMsg_t
    static final String[] authReplyMsg       = {
            //	"Waiting for authorization",
            "#str_07204",
            //	"Client unknown to auth",
            "#str_07205",
            //	"Access denied - CD Key in use",
            "#str_07206",
            //	"Auth custom message", // placeholder - we propagate a message from the master
            "#str_07207",
            //	"Authorize Server - Waiting for client"
            "#str_07208"
    };
    static final String[] authReplyStr       = {
            "AUTH_NONE",
            "AUTH_OK",
            "AUTH_WAIT",
            "AUTH_DENY"
    };

    public static class idAsyncServer {

        private boolean    active;                                                    // true if server is active
        private int        realTime;                                                  // absolute time
        //
        private int        serverTime;                                                // local server time
        private idPort     serverPort;                                                // UDP port
        private int        serverId;                                                  // server identification
        private BigInteger serverDataChecksum;                                        // checksum of the data used by the server
        private int        localClientNum;                                            // local client on listen server
        //
        private challenge_s[]    challenges = new challenge_s[MAX_CHALLENGES];        // to prevent invalid IPs from connecting
        private serverClient_s[] clients    = new serverClient_s[MAX_ASYNC_CLIENTS];  // clients
        private usercmd_t[][]    userCmds   = new usercmd_t[MAX_USERCMD_BACKUP][MAX_ASYNC_CLIENTS];
        //
        private int      gameInitId;                                                  // game initialization identification
        private int      gameFrame;                                                   // local game frame
        private int      gameTime;                                                    // local game time
        private int      gameTimeResidual;                                            // left over time from previous frame
        //
        private netadr_t rconAddress;
        //	
        private int      nextHeartbeatTime;
        private int      nextAsyncStatsTime;
        //
        private boolean  serverReloadingEngine;                                       // flip-flop to not loop over when net_serverReloadEngine is on
        //
        private boolean  noRconOutput;                                                // for default rcon response when command is silent
        //
        private int      lastAuthTime;                                                // global for auth server timeout
        //
        // track the max outgoing rate over the last few secs to watch for spikes
        // dependent on net_serverSnapshotDelay. 50ms, for a 3 seconds backlog -> 60 samples
        private static final int   stats_numsamples = 60;
        private              int[] stats_outrate    = new int[stats_numsamples];
        private int stats_current;
        private int stats_average_sum;
        private int stats_max;
        private int stats_max_index;
        //
        //

        public idAsyncServer() {
            int i, j;

            active = false;
            realTime = 0;
            serverTime = 0;
            serverId = 0;
            serverDataChecksum = BigInteger.ZERO;
            localClientNum = -1;
            gameInitId = 0;
            gameFrame = 0;
            gameTime = 0;
            gameTimeResidual = 0;
            for (i = 0; i < MAX_CHALLENGES; i++) {
//            memset(challenges, 0, sizeof(challenges));
                challenges[i] = new challenge_s();
            }
            for (i = 0; i < MAX_ASYNC_CLIENTS; i++) {
//            memset(challenges, 0, sizeof(challenges));
                clients[i] = new serverClient_s();
                ClearClient(i);
//            memset(userCmds, 0, sizeof(userCmds));
                for (j = 0; j < MAX_USERCMD_BACKUP; j++) {
                    userCmds[j][i] = new usercmd_t();
                }
            }

            serverReloadingEngine = false;
            nextHeartbeatTime = 0;
            nextAsyncStatsTime = 0;
            noRconOutput = true;
            lastAuthTime = 0;

//            memset(stats_outrate, 0, sizeof(stats_outrate));
            stats_current = 0;
            stats_average_sum = 0;
            stats_max = 0;
            stats_max_index = 0;

            this.serverPort = new idPort();
        }

        public boolean InitPort() {
            int lastPort;

            // if this is the first time we have spawned a server, open the UDP port
            if (0 == serverPort.GetPort()) {
                if (cvarSystem.GetCVarInteger("net_port") != 0) {
                    if (!serverPort.InitForPort(cvarSystem.GetCVarInteger("net_port"))) {
                        common.Printf("Unable to open server on port %d (net_port)\n", cvarSystem.GetCVarInteger("net_port"));
                        return false;
                    }
                } else {
                    // scan for multiple ports, in case other servers are running on this IP already
                    for (lastPort = 0; lastPort < NUM_SERVER_PORTS; lastPort++) {
                        if (serverPort.InitForPort(PORT_SERVER + lastPort)) {
                            break;
                        }
                    }
                    if (lastPort >= NUM_SERVER_PORTS) {
                        common.Printf("Unable to open server network port.\n");
                        return false;
                    }
                }
            }

            return true;
        }

        public void ClosePort() {
            int i;

            serverPort.Close();
            for (i = 0; i < MAX_CHALLENGES; i++) {
                challenges[i].authReplyPrint.Clear();
            }
        }

        public void Spawn() {
            int i;
            int[] size = new int[1];
            ByteBuffer msgBuf = ByteBuffer.allocate(MAX_MESSAGE_SIZE);
            netadr_t[] from = new netadr_t[1];

            // shutdown any current game
            session.Stop();

            if (active) {
                return;
            }

            if (!InitPort()) {
                return;
            }

            // trash any currently pending packets
            while (serverPort.GetPacket(from, msgBuf, size, msgBuf.capacity())) {
            }

            // reset cheats cvars
            if (!idAsyncNetwork.allowCheats.GetBool()) {
                cvarSystem.ResetFlaggedVariables(CVAR_CHEAT);
            }

//	memset( challenges, 0, sizeof( challenges ) );
//	memset( userCmds, 0, sizeof( userCmds ) );
            Arrays.fill(challenges, 0);
            Arrays.fill(userCmds, 0);
            for (i = 0; i < MAX_ASYNC_CLIENTS; i++) {
                ClearClient(i);
            }

            common.Printf("Server spawned on port %d.\n", serverPort.GetPort());

            // calculate a checksum on some of the essential data used
            serverDataChecksum = declManager.GetChecksum();

            // get a pseudo random server id, but don't use the id which is reserved for connectionless packets
            serverId = Sys_Milliseconds() & CONNECTIONLESS_MESSAGE_ID_MASK;

            active = true;

            nextHeartbeatTime = 0;
            nextAsyncStatsTime = 0;

            ExecuteMapChange();
        }

        public void Kill() {
            int i, j;

            if (!active) {
                return;
            }

            // drop all clients
            for (i = 0; i < MAX_ASYNC_CLIENTS; i++) {
                DropClient(i, "#str_07135");
            }

            // send some empty messages to the zombie clients to make sure they disconnect
            for (j = 0; j < 4; j++) {
                for (i = 0; i < MAX_ASYNC_CLIENTS; i++) {
                    if (clients[i].clientState == SCS_ZOMBIE) {
                        if (clients[i].channel.UnsentFragmentsLeft()) {
                            clients[i].channel.SendNextFragment(serverPort, serverTime);
                        } else {
                            SendEmptyToClient(i, true);
                        }
                    }
                }
                Sys_Sleep(10);
            }

            // reset any pureness
            fileSystem.ClearPureChecksums();

            active = false;

            // shutdown any current game
            session.Stop();
        }

        public void ExecuteMapChange() throws idException {
            int i;
            idBitMsg msg = new idBitMsg();
            ByteBuffer msgBuf = ByteBuffer.allocate(MAX_MESSAGE_SIZE);
            idStr mapName;
            findFile_t ff;
            boolean addonReload = false;
            char[] bestGameType = new char[MAX_STRING_CHARS];

            assert (active);

            // reset any pureness
            fileSystem.ClearPureChecksums();

            // make sure the map/gametype combo is good
            game.GetBestGameType(cvarSystem.GetCVarString("si_map"), cvarSystem.GetCVarString("si_gametype"), bestGameType);
            cvarSystem.SetCVarString("si_gametype", ctos(bestGameType));

            // initialize map settings
            cmdSystem.BufferCommandText(CMD_EXEC_NOW, "rescanSI");

            mapName = new idStr(String.format("maps/%s", sessLocal.mapSpawnData.serverInfo.GetString("si_map")));
            mapName.SetFileExtension(".map");
            ff = fileSystem.FindFile(mapName.toString(), !serverReloadingEngine);
            switch (ff) {
                case FIND_NO:
                    common.Printf("Can't find map %s\n", mapName.toString());
                    cmdSystem.BufferCommandText(CMD_EXEC_APPEND, "disconnect\n");
                    return;
                case FIND_ADDON:
                    // NOTE: we have no problem with addon dependencies here because if the map is in
                    // an addon pack that's already on search list, then all it's deps are assumed to be on search as well
                    common.Printf("map %s is in an addon pak - reloading\n", mapName.toString());
                    addonReload = true;
                    break;
                default:
                    break;
            }

            // if we are asked to do a full reload, the strategy is completely different
            if (!serverReloadingEngine && (addonReload || idAsyncNetwork.serverReloadEngine.GetInteger() != 0)) {
                if (idAsyncNetwork.serverReloadEngine.GetInteger() != 0) {
                    common.Printf("net_serverReloadEngine enabled - doing a full reload\n");
                }
                // tell the clients to reconnect
                // FIXME: shouldn't they wait for the new pure list, then reload?
                // in a lot of cases this is going to trigger two reloadEngines for the clients
                // one to restart, the other one to set paks right ( with addon for instance )
                // can fix by reconnecting without reloading and waiting for the server to tell..
                for (i = 0; i < MAX_ASYNC_CLIENTS; i++) {
                    if (clients[i].clientState.ordinal() >= SCS_PUREWAIT.ordinal() && i != localClientNum) {
                        msg.Init(msgBuf, msgBuf.capacity());
                        msg.WriteByte(SERVER_RELIABLE_MESSAGE_RELOAD.ordinal());
                        SendReliableMessage(i, msg);
                        clients[i].clientState = SCS_ZOMBIE; // so we don't bother sending a disconnect
                    }
                }
                cmdSystem.BufferCommandText(CMD_EXEC_NOW, "reloadEngine");
                serverReloadingEngine = true; // don't get caught in endless loop
                cmdSystem.BufferCommandText(CMD_EXEC_APPEND, "spawnServer\n");
                // decrease feature
                if (idAsyncNetwork.serverReloadEngine.GetInteger() > 0) {
                    idAsyncNetwork.serverReloadEngine.SetInteger(idAsyncNetwork.serverReloadEngine.GetInteger() - 1);
                }
                return;
            }
            serverReloadingEngine = false;

            serverTime = 0;

            // initialize game id and time
            gameInitId ^= Sys_Milliseconds();	// NOTE: make sure the gameInitId is always a positive number because negative numbers have special meaning
            gameFrame = 0;
            gameTime = 0;
            gameTimeResidual = 0;
//            memset(userCmds, 0, sizeof(userCmds));
            userCmds = new usercmd_t[MAX_USERCMD_BACKUP][MAX_ASYNC_CLIENTS];

            if (idAsyncNetwork.serverDedicated.GetInteger() == 0) {
                InitLocalClient(0);
            } else {
                localClientNum = -1;
            }

            // re-initialize all connected clients for the new map
            for (i = 0; i < MAX_ASYNC_CLIENTS; i++) {
                if (clients[i].clientState.ordinal() >= SCS_PUREWAIT.ordinal() && i != localClientNum) {

                    InitClient(i, clients[i].clientId, clients[i].clientRate);

                    SendGameInitToClient(i);

                    if (sessLocal.mapSpawnData.serverInfo.GetBool("si_pure")) {
                        clients[i].clientState = SCS_PUREWAIT;
                    }
                }
            }

            // setup the game pak checksums
            // since this is not dependant on si_pure we catch anything bad before loading map
            if (sessLocal.mapSpawnData.serverInfo.GetInt("si_pure") != 0) {
                if (!fileSystem.UpdateGamePakChecksums()) {
                    session.MessageBox(MSG_OK, common.GetLanguageDict().GetString("#str_04337"), common.GetLanguageDict().GetString("#str_04338"), true);
                    cmdSystem.BufferCommandText(CMD_EXEC_APPEND, "disconnect\n");
                    return;
                }
            }

            // load map
            sessLocal.ExecuteMapChange();

            if (localClientNum >= 0) {
                BeginLocalClient();
            } else {
                game.SetLocalClient(-1);
            }

            if (sessLocal.mapSpawnData.serverInfo.GetInt("si_pure") != 0) {
                // lock down the pak list
                fileSystem.UpdatePureServerChecksums();
                // tell the clients so they can work out their pure lists
                for (i = 0; i < MAX_ASYNC_CLIENTS; i++) {
                    if (clients[i].clientState == SCS_PUREWAIT) {
                        if (!SendReliablePureToClient(i)) {
                            clients[i].clientState = SCS_CONNECTED;
                        }
                    }
                }
            }

            // serverTime gets reset, force a heartbeat so timings restart
            MasterHeartbeat(true);
        }
//

        public int GetPort() {
            return serverPort.GetPort();
        }

        public netadr_t GetBoundAdr() {
            return serverPort.GetAdr();
        }

        public boolean IsActive() {
            return active;
        }

        public int GetDelay() {
            return gameTimeResidual;
        }

        public int GetOutgoingRate() {
            int i, rate;

            rate = 0;
            for (i = 0; i < MAX_ASYNC_CLIENTS; i++) {
                final serverClient_s client = clients[i];

                if (client.clientState.ordinal() >= SCS_CONNECTED.ordinal()) {
                    rate += client.channel.GetOutgoingRate();
                }
            }
            return rate;
        }

        public int GetIncomingRate() {
            int i, rate;

            rate = 0;
            for (i = 0; i < MAX_ASYNC_CLIENTS; i++) {
                final serverClient_s client = clients[i];

                if (client.isClientConnected()) {
                    rate += client.channel.GetIncomingRate();
                }
            }
            return rate;
        }

        public boolean IsClientInGame(int clientNum) {
            return (clients[clientNum].clientState.ordinal() >= SCS_INGAME.ordinal());
        }

        public int GetClientPing(int clientNum) {
            final serverClient_s client = clients[clientNum];

            if (client.isClientConnected()) {
                return 99999;
            } else {
                return client.clientPing;
            }
        }

        public int GetClientPrediction(int clientNum) {
            final serverClient_s client = clients[clientNum];

            if (client.isClientConnected()) {
                return 99999;
            } else {
                return client.clientPrediction;
            }
        }

        public int GetClientTimeSinceLastPacket(int clientNum) {
            final serverClient_s client = clients[clientNum];

            if (client.isClientConnected()) {
                return 99999;
            } else {
                return serverTime - client.lastPacketTime;
            }
        }

        public int GetClientTimeSinceLastInput(int clientNum) {
            final serverClient_s client = clients[clientNum];

            if (client.isClientConnected()) {
                return 99999;
            } else {
                return serverTime - client.lastInputTime;
            }
        }

        public int GetClientOutgoingRate(int clientNum) {
            final serverClient_s client = clients[clientNum];

            if (client.isClientConnected()) {
                return -1;
            } else {
                return client.channel.GetOutgoingRate();
            }
        }

        public int GetClientIncomingRate(int clientNum) {
            final serverClient_s client = clients[clientNum];

            if (client.isClientConnected()) {
                return -1;
            } else {
                return client.channel.GetIncomingRate();
            }
        }

        public float GetClientOutgoingCompression(int clientNum) {
            final serverClient_s client = clients[clientNum];

            if (client.isClientConnected()) {
                return 0.0f;
            } else {
                return client.channel.GetOutgoingCompression();
            }
        }

        public float GetClientIncomingCompression(int clientNum) {
            final serverClient_s client = clients[clientNum];

            if (client.isClientConnected()) {
                return 0.0f;
            } else {
                return client.channel.GetIncomingCompression();
            }
        }

        public float GetClientIncomingPacketLoss(int clientNum) {
            final serverClient_s client = clients[clientNum];

            if (client.isClientConnected()) {
                return 0.0f;
            } else {
                return client.channel.GetIncomingPacketLoss();
            }
        }

        public int GetNumClients() {
            int ret = 0;
            for (int i = 0; i < MAX_ASYNC_CLIENTS; i++) {
                if (clients[i].clientState.ordinal() >= SCS_CONNECTED.ordinal()) {
                    ret++;
                }
            }
            return ret;
        }

        public int GetNumIdleClients() {
            int ret = 0;
            for (int i = 0; i < MAX_ASYNC_CLIENTS; i++) {
                if (clients[i].clientState.ordinal() >= SCS_CONNECTED.ordinal()) {
                    if (serverTime - clients[i].lastInputTime > NOINPUT_IDLE_TIME) {
                        ret++;
                    }
                }
            }
            return ret;
        }

        public int GetLocalClientNum() {
            return localClientNum;
        }
//

        public void RunFrame() throws idException {
            int i, msec;
            int[] size = new int[1];
            boolean newPacket;
            idBitMsg msg = new idBitMsg();
            ByteBuffer msgBuf = ByteBuffer.allocate(MAX_MESSAGE_SIZE);
            netadr_t[] from = new netadr_t[1];
            int outgoingRate, incomingRate;
            float outgoingCompression, incomingCompression;

            msec = UpdateTime(100);

            if (0 == serverPort.GetPort()) {
                return;
            }

            if (!active) {
                ProcessConnectionLessMessages();
                return;
            }

            gameTimeResidual += msec;

            // spin in place processing incoming packets until enough time lapsed to run a new game frame
            do {
                do {
                    // blocking read with game time residual timeout
                    newPacket = serverPort.GetPacketBlocking(from, msgBuf, size, msgBuf.capacity(), USERCMD_MSEC - gameTimeResidual - 1);
                    if (newPacket) {
                        msg.Init(msgBuf, msgBuf.capacity());
                        msg.SetSize(size[0]);
                        msg.BeginReading();
                        if (ProcessMessage(from[0], msg)) {
                            return;	// return because rcon was used
                        }
                    }

                    msec = UpdateTime(100);
                    gameTimeResidual += msec;

                } while (newPacket);

            } while (gameTimeResidual < USERCMD_MSEC);

            // send heart beat to master servers
            MasterHeartbeat();

            // check for clients that timed out
            CheckClientTimeouts();

            if (idAsyncNetwork.idleServer.GetBool() == (0 == GetNumClients() || GetNumIdleClients() != GetNumClients())) {
                idAsyncNetwork.idleServer.SetBool(!idAsyncNetwork.idleServer.GetBool());
                // the need to propagate right away, only this
                sessLocal.mapSpawnData.serverInfo.Set("si_idleServer", idAsyncNetwork.idleServer.GetString());
                game.SetServerInfo(sessLocal.mapSpawnData.serverInfo);
            }

            // make sure the time doesn't wrap
            if (serverTime > 0x70000000) {
                ExecuteMapChange();
                return;
            }

            // check for synchronized cvar changes
            if ((cvarSystem.GetModifiedFlags() & CVAR_NETWORKSYNC) != 0) {
                idDict newCvars;
                newCvars = cvarSystem.MoveCVarsToDict(CVAR_NETWORKSYNC);
                SendSyncedCvarsBroadcast(newCvars);
                cvarSystem.ClearModifiedFlags(CVAR_NETWORKSYNC);
            }

            // check for user info changes of the local client
            if ((cvarSystem.GetModifiedFlags() & CVAR_USERINFO) != 0) {
                if (localClientNum >= 0) {
                    idDict newInfo;
                    game.ThrottleUserInfo();
                    newInfo = cvarSystem.MoveCVarsToDict(CVAR_USERINFO);
                    SendUserInfoBroadcast(localClientNum, newInfo);
                }
                cvarSystem.ClearModifiedFlags(CVAR_USERINFO);
            }

            // advance the server game
            while (gameTimeResidual >= USERCMD_MSEC) {

                // sample input for the local client
                LocalClientInput();

                // duplicate usercmds for clients if no new ones are available
                DuplicateUsercmds(gameFrame, gameTime);

                // advance game
                gameReturn_t ret = game.RunFrame(userCmds[gameFrame & (MAX_USERCMD_BACKUP - 1)]);

                idAsyncNetwork.ExecuteSessionCommand(ret.sessionCommand);

                // update time
                gameFrame++;
                gameTime += USERCMD_MSEC;
                gameTimeResidual -= USERCMD_MSEC;
            }

            // duplicate usercmds so there is always at least one available to send with snapshots
            DuplicateUsercmds(gameFrame, gameTime);

            // send snapshots to connected clients
            for (i = 0; i < MAX_ASYNC_CLIENTS; i++) {
                serverClient_s client = clients[i];

                if (client.clientState == SCS_FREE || i == localClientNum) {
                    continue;
                }

                // modify maximum rate if necesary
                if (idAsyncNetwork.serverMaxClientRate.IsModified()) {
                    client.channel.SetMaxOutgoingRate(Min(client.clientRate, idAsyncNetwork.serverMaxClientRate.GetInteger()));
                }

                // if the channel is not yet ready to send new data
                if (!client.channel.ReadyToSend(serverTime)) {
                    continue;
                }

                // send additional message fragments if the last message was too large to send at once
                if (client.channel.UnsentFragmentsLeft()) {
                    client.channel.SendNextFragment(serverPort, serverTime);
                    continue;
                }

                if (client.clientState == SCS_INGAME) {
                    if (!SendSnapshotToClient(i)) {
                        SendPingToClient(i);
                    }
                } else {
                    SendEmptyToClient(i);
                }
            }

            if (com_showAsyncStats.GetBool()) {

                UpdateAsyncStatsAvg();

                // dedicated will verbose to console
                if (idAsyncNetwork.serverDedicated.GetBool() && serverTime >= nextAsyncStatsTime) {
                    common.Printf("delay = %d msec, total outgoing rate = %d KB/s, total incoming rate = %d KB/s\n", GetDelay(),
                            GetOutgoingRate() >> 10, GetIncomingRate() >> 10);

                    for (i = 0; i < MAX_ASYNC_CLIENTS; i++) {

                        outgoingRate = GetClientOutgoingRate(i);
                        incomingRate = GetClientIncomingRate(i);
                        outgoingCompression = GetClientOutgoingCompression(i);
                        incomingCompression = GetClientIncomingCompression(i);

                        if (outgoingRate != -1 && incomingRate != -1) {
                            common.Printf("client %d: out rate = %d B/s (% -2.1f%%), in rate = %d B/s (% -2.1f%%)\n",
                                    i, outgoingRate, outgoingCompression, incomingRate, incomingCompression);
                        }
                    }

                    idStr msg1 = new idStr();
                    GetAsyncStatsAvgMsg(msg1);
                    common.Printf(va("%s\n", msg1.toString()));

                    nextAsyncStatsTime = serverTime + 1000;
                }
            }

            idAsyncNetwork.serverMaxClientRate.ClearModified();
        }

        public void ProcessConnectionLessMessages() {
            int id;
            int[] size = new int[1];
            idBitMsg msg = new idBitMsg();
            ByteBuffer msgBuf = ByteBuffer.allocate(MAX_MESSAGE_SIZE);
            netadr_t[] from = new netadr_t[1];

            if (0 == serverPort.GetPort()) {
                return;
            }

            while (serverPort.GetPacket(from, msgBuf, size, msgBuf.capacity())) {
                msg.Init(msgBuf, msgBuf.capacity());
                msg.SetSize(size[0]);
                msg.BeginReading();
                id = msg.ReadShort();
                if (id == CONNECTIONLESS_MESSAGE_ID) {
                    ConnectionlessMessage(from[0], msg);
                }
            }
        }

        public void RemoteConsoleOutput(final String string) {
            noRconOutput = false;
            PrintOOB(rconAddress, SERVER_PRINT_RCON.ordinal(), string);
        }

        public void SendReliableGameMessage(int clientNum, final idBitMsg msg) {
            int i;
            idBitMsg outMsg = new idBitMsg();
            ByteBuffer msgBuf = ByteBuffer.allocate(MAX_MESSAGE_SIZE);

            outMsg.Init(msgBuf, msgBuf.capacity());
            outMsg.WriteByte(SERVER_RELIABLE_MESSAGE_GAME.ordinal());
            outMsg.WriteData(msg.GetData(), msg.GetSize());

            if (clientNum >= 0 && clientNum < MAX_ASYNC_CLIENTS) {
                if (clients[clientNum].clientState == SCS_INGAME) {
                    SendReliableMessage(clientNum, outMsg);
                }
                return;
            }

            for (i = 0; i < MAX_ASYNC_CLIENTS; i++) {
                if (clients[i].clientState != SCS_INGAME) {
                    continue;
                }
                SendReliableMessage(i, outMsg);
            }
        }

        public void SendReliableGameMessageExcluding(int clientNum, final idBitMsg msg) {
            int i;
            idBitMsg outMsg = new idBitMsg();
            ByteBuffer msgBuf = ByteBuffer.allocate(MAX_MESSAGE_SIZE);

            assert (clientNum >= 0 && clientNum < MAX_ASYNC_CLIENTS);

            outMsg.Init(msgBuf, msgBuf.capacity());
            outMsg.WriteByte(SERVER_RELIABLE_MESSAGE_GAME.ordinal());
            outMsg.WriteData(msg.GetData(), msg.GetSize());

            for (i = 0; i < MAX_ASYNC_CLIENTS; i++) {
                if (i == clientNum) {
                    continue;
                }
                if (clients[i].clientState != SCS_INGAME) {
                    continue;
                }
                SendReliableMessage(i, outMsg);
            }
        }

        public void LocalClientSendReliableMessage(final idBitMsg msg) {
            if (localClientNum < 0) {
                common.Printf("LocalClientSendReliableMessage: no local client\n");
                return;
            }
            game.ServerProcessReliableMessage(localClientNum, msg);
        }
//

        public void MasterHeartbeat() {
            MasterHeartbeat(false);
        }

        public void MasterHeartbeat(boolean force /*= false*/) {
            if (idAsyncNetwork.LANServer.GetBool()) {
                if (force) {
                    common.Printf("net_LANServer is enabled. Not sending heartbeats\n");
                }
                return;
            }
            if (force) {
                nextHeartbeatTime = 0;
            }
            // not yet
            if (serverTime < nextHeartbeatTime) {
                return;
            }
            nextHeartbeatTime = serverTime + HEARTBEAT_MSEC;
            for (int i = 0; i < MAX_MASTER_SERVERS; i++) {
                netadr_t adr = new netadr_t();
                if (idAsyncNetwork.GetMasterAddress(i, adr)) {
                    common.Printf("Sending heartbeat to %s\n", Sys_NetAdrToString(adr));
                    idBitMsg outMsg = new idBitMsg();
                    ByteBuffer msgBuf = ByteBuffer.allocate(MAX_MESSAGE_SIZE);
                    outMsg.Init(msgBuf, msgBuf.capacity());
                    outMsg.WriteShort(CONNECTIONLESS_MESSAGE_ID);
                    outMsg.WriteString("heartbeat");
                    serverPort.SendPacket(adr, outMsg.GetData(), outMsg.GetSize());
                }
            }
        }

        public String DropClient(int clientNum, final String reason) {
            int i;
            idBitMsg msg = new idBitMsg();
            ByteBuffer msgBuf = ByteBuffer.allocate(MAX_MESSAGE_SIZE);
            String returnString;

            serverClient_s client = clients[clientNum];

            if (client.clientState.ordinal() <= SCS_ZOMBIE.ordinal()) {
                return "";
            }

            if (client.clientState.ordinal() >= SCS_PUREWAIT.ordinal() && clientNum != localClientNum) {
                msg.Init(msgBuf, msgBuf.capacity());
                msg.WriteByte(SERVER_RELIABLE_MESSAGE_DISCONNECT.ordinal());
                msg.WriteLong(clientNum);
                msg.WriteString(reason);
                for (i = 0; i < MAX_ASYNC_CLIENTS; i++) {
                    // clientNum so SCS_PUREWAIT client gets it's own disconnect msg
                    if (i == clientNum || clients[i].clientState.ordinal() >= SCS_CONNECTED.ordinal()) {
                        SendReliableMessage(i, msg);
                    }
                }
            }

            returnString = common.GetLanguageDict().GetString(reason);
            common.Printf("client %d %s\n", clientNum, reason);
            cmdSystem.BufferCommandText(CMD_EXEC_NOW, va("addChatLine \"%s^0 %s\"", sessLocal.mapSpawnData.userInfo[clientNum].GetString("ui_name"), reason));

            // remove the player from the game
            game.ServerClientDisconnect(clientNum);

            client.clientState = SCS_ZOMBIE;

            return returnString;
        }

//
        public void PacifierUpdate() {
            int i;

            if (!IsActive()) {
                return;
            }
            realTime = Sys_Milliseconds();
            ProcessConnectionLessMessages();
            for (i = 0; i < MAX_ASYNC_CLIENTS; i++) {
                if (clients[i].clientState.ordinal() >= SCS_PUREWAIT.ordinal()) {
                    if (clients[i].channel.UnsentFragmentsLeft()) {
                        clients[i].channel.SendNextFragment(serverPort, serverTime);
                    } else {
                        SendEmptyToClient(i);
                    }
                }
            }
        }
//


        /*
         ==================
         idAsyncServer::UpdateUI
         if the game modifies userInfo, it will call this through command system
         we then need to get the info from the game, and broadcast to clients
         ( using DeltaDict and our current mapSpawnData as a base )
         ==================
         */
        public void UpdateUI(int clientNum) {
            final idDict info = game.GetUserInfo(clientNum);

            if (null == info) {
                common.Warning("idAsyncServer::UpdateUI: no info from game\n");
                return;
            }

            SendUserInfoBroadcast(clientNum, info, true);
        }

//
        public void UpdateAsyncStatsAvg() {
            stats_average_sum -= stats_outrate[stats_current];
            stats_outrate[stats_current] = idAsyncNetwork.server.GetOutgoingRate();
            if (stats_outrate[stats_current] > stats_max) {
                stats_max = stats_outrate[stats_current];
                stats_max_index = stats_current;
            } else if (stats_current == stats_max_index) {
                // find the new max
                int i;
                stats_max = 0;
                for (i = 0; i < stats_numsamples; i++) {
                    if (stats_outrate[i] > stats_max) {
                        stats_max = stats_outrate[i];
                        stats_max_index = i;
                    }
                }
            }
            stats_average_sum += stats_outrate[stats_current];
            stats_current++;
            stats_current %= stats_numsamples;
        }

        public void GetAsyncStatsAvgMsg(idStr msg) {
            msg.oSet(String.format("avrg out: %d B/s - max %d B/s ( over %d ms )", stats_average_sum / stats_numsamples, stats_max, idAsyncNetwork.serverSnapshotDelay.GetInteger() * stats_numsamples));
        }
//

        /*
         ===============
         idAsyncServer::PrintLocalServerInfo
         see (client) "getInfo" -> (server) "infoResponse" -> (client)ProcessGetInfoMessage
         ===============
         */
        public void PrintLocalServerInfo() {
            int i;

            common.Printf("server '%s' IP = %s\nprotocol %d.%d OS mask 0x%x\n",
                    sessLocal.mapSpawnData.serverInfo.GetString("si_name"),
                    Sys_NetAdrToString(serverPort.GetAdr()),
                    ASYNC_PROTOCOL_MAJOR,
                    ASYNC_PROTOCOL_MINOR,
                    fileSystem.GetOSMask());
            sessLocal.mapSpawnData.serverInfo.Print();
            for (i = 0; i < MAX_ASYNC_CLIENTS; i++) {
                serverClient_s client = clients[i];
                if (client.clientState.ordinal() < SCS_CONNECTED.ordinal()) {
                    continue;
                }
                common.Printf("client %2d: %s, ping = %d, rate = %d\n", i,
                        sessLocal.mapSpawnData.userInfo[i].GetString("ui_name", "Player"),
                        client.clientPing, client.channel.GetMaxOutgoingRate());
            }
        }

        private void PrintOOB(final netadr_t to, int opcode, final String string) {
            idBitMsg outMsg = new idBitMsg();
            ByteBuffer msgBuf = ByteBuffer.allocate(MAX_MESSAGE_SIZE);

            outMsg.Init(msgBuf, msgBuf.capacity());
            outMsg.WriteShort(CONNECTIONLESS_MESSAGE_ID);
            outMsg.WriteString("print");
            outMsg.WriteLong(opcode);
            outMsg.WriteString(string);
            serverPort.SendPacket(to, outMsg.GetData(), outMsg.GetSize());
        }

        private void DuplicateUsercmds(int frame, int time) {
            int i, previousIndex, currentIndex;

            previousIndex = (frame - 1) & (MAX_USERCMD_BACKUP - 1);
            currentIndex = frame & (MAX_USERCMD_BACKUP - 1);

            // duplicate previous user commands if no new commands are available for a client
            for (i = 0; i < MAX_ASYNC_CLIENTS; i++) {
                if (clients[i].clientState == SCS_FREE) {
                    continue;
                }

                if (idAsyncNetwork.DuplicateUsercmd(userCmds[previousIndex][i], userCmds[currentIndex][i], frame, time)) {
                    clients[i].numDuplicatedUsercmds++;
                }
            }
        }

        private void ClearClient(int clientNum) {
            serverClient_s client = clients[clientNum];
            client.clientId = 0;
            client.clientState = SCS_FREE;
            client.clientPrediction = 0;
            client.clientAheadTime = 0;
            client.clientRate = 0;
            client.clientPing = 0;
            client.gameInitSequence = 0;
            client.gameFrame = 0;
            client.gameTime = 0;
            client.channel.Shutdown();
            client.lastConnectTime = 0;
            client.lastEmptyTime = 0;
            client.lastPingTime = 0;
            client.lastSnapshotTime = 0;
            client.lastPacketTime = 0;
            client.lastInputTime = 0;
            client.snapshotSequence = 0;
            client.acknowledgeSnapshotSequence = 0;
            client.numDuplicatedUsercmds = 0;
        }

        private void InitClient(int clientNum, int clientId, int clientRate) {
            int i;

            // clear the user info
            sessLocal.mapSpawnData.userInfo[clientNum].Clear();	// always start with a clean base

            // clear the server client
            serverClient_s client = clients[clientNum];
            client.clientId = clientId;
            client.clientState = SCS_CONNECTED;
            client.clientPrediction = 0;
            client.clientAheadTime = 0;
            client.gameInitSequence = -1;
            client.gameFrame = 0;
            client.gameTime = 0;
            client.channel.ResetRate();
            client.clientRate = (clientRate != 0 ? clientRate : idAsyncNetwork.serverMaxClientRate.GetInteger());
            client.channel.SetMaxOutgoingRate(Min(idAsyncNetwork.serverMaxClientRate.GetInteger(), client.clientRate));
            client.clientPing = 0;
            client.lastConnectTime = serverTime;
            client.lastEmptyTime = serverTime;
            client.lastPingTime = serverTime;
            client.lastSnapshotTime = serverTime;
            client.lastPacketTime = serverTime;
            client.lastInputTime = serverTime;
            client.acknowledgeSnapshotSequence = 0;
            client.numDuplicatedUsercmds = 0;

            // clear the user commands
            for (i = 0; i < MAX_USERCMD_BACKUP; i++) {
//                memset( & userCmds[i][clientNum], 0, sizeof(userCmds[i][clientNum]));
                userCmds[i][clientNum] = new usercmd_t();
//                userCmds[i][clientNum] = null;//TODO:which?
            }

            // let the game know a player connected
            game.ServerClientConnect(clientNum, ctos(client.guid));
        }

        private void InitLocalClient(int clientNum) {
            netadr_t badAddress = new netadr_t();

            localClientNum = clientNum;
            InitClient(clientNum, 0, 0);
//	memset( &badAddress, 0, sizeof( badAddress ) );
            badAddress.type = NA_BAD;
            clients[clientNum].channel.Init(badAddress, serverId);
            clients[clientNum].clientState = SCS_INGAME;
            sessLocal.mapSpawnData.userInfo[clientNum] = cvarSystem.MoveCVarsToDict(CVAR_USERINFO);
        }

        private void BeginLocalClient() {
            game.SetLocalClient(localClientNum);
            game.SetUserInfo(localClientNum, sessLocal.mapSpawnData.userInfo[localClientNum], false, false);
            game.ServerClientBegin(localClientNum);
        }

        private void LocalClientInput() {
            int index;

            if (localClientNum < 0) {
                return;
            }

            index = gameFrame & (MAX_USERCMD_BACKUP - 1);
            userCmds[index][localClientNum] = usercmdGen.GetDirectUsercmd();
            userCmds[index][localClientNum].gameFrame = gameFrame;
            userCmds[index][localClientNum].gameTime = gameTime;
            if (idAsyncNetwork.UsercmdInputChanged(userCmds[(gameFrame - 1) & (MAX_USERCMD_BACKUP - 1)][localClientNum], userCmds[index][localClientNum])) {
                clients[localClientNum].lastInputTime = serverTime;
            }
            clients[localClientNum].gameFrame = gameFrame;
            clients[localClientNum].gameTime = gameTime;
            clients[localClientNum].lastPacketTime = serverTime;
        }

        private void CheckClientTimeouts() {
            int i, zombieTimeout, clientTimeout;

            zombieTimeout = serverTime - idAsyncNetwork.serverZombieTimeout.GetInteger() * 1000;
            clientTimeout = serverTime - idAsyncNetwork.serverClientTimeout.GetInteger() * 1000;

            for (i = 0; i < MAX_ASYNC_CLIENTS; i++) {
                serverClient_s client = clients[i];

                if (i == localClientNum) {
                    continue;
                }

                if (client.lastPacketTime > serverTime) {
                    client.lastPacketTime = serverTime;
                    continue;
                }

                if (client.clientState == SCS_ZOMBIE && client.lastPacketTime < zombieTimeout) {
                    client.channel.Shutdown();
                    client.clientState = SCS_FREE;
                    continue;
                }

                if (client.clientState.ordinal() >= SCS_PUREWAIT.ordinal() && client.lastPacketTime < clientTimeout) {
                    DropClient(i, "#str_07137");
                    continue;
                }
            }
        }

        private void SendPrintBroadcast(final String string) {
            int i;
            idBitMsg msg = new idBitMsg();
            ByteBuffer msgBuf = ByteBuffer.allocate(MAX_MESSAGE_SIZE);

            msg.Init(msgBuf, msgBuf.capacity());
            msg.WriteByte(SERVER_RELIABLE_MESSAGE_PRINT.ordinal());
            msg.WriteString(string);

            for (i = 0; i < MAX_ASYNC_CLIENTS; i++) {
                if (clients[i].clientState.ordinal() >= SCS_CONNECTED.ordinal()) {
                    SendReliableMessage(i, msg);
                }
            }
        }

        private void SendPrintToClient(int clientNum, final String string) {
            idBitMsg msg = new idBitMsg();
            ByteBuffer msgBuf = ByteBuffer.allocate(MAX_MESSAGE_SIZE);

            serverClient_s client = clients[clientNum];

            if (client.clientState.ordinal() < SCS_CONNECTED.ordinal()) {
                return;
            }

            msg.Init(msgBuf, msgBuf.capacity());
            msg.WriteByte(SERVER_RELIABLE_MESSAGE_PRINT.ordinal());
            msg.WriteString(string);

            SendReliableMessage(clientNum, msg);
        }

        private void SendUserInfoBroadcast(int userInfoNum, final idDict info) {
            SendUserInfoBroadcast(userInfoNum, info, false);
        }

        private void SendUserInfoBroadcast(int userInfoNum, final idDict info, boolean sendToAll /*= false */) {
            idBitMsg msg = new idBitMsg();
            ByteBuffer msgBuf = ByteBuffer.allocate(MAX_MESSAGE_SIZE);
            idDict gameInfo;
            boolean gameModifiedInfo;

            gameInfo = game.SetUserInfo(userInfoNum, info, false, true);
            if (gameInfo != null) {
                gameModifiedInfo = true;
            } else {
                gameModifiedInfo = false;
                gameInfo = info;
            }

            if (userInfoNum == localClientNum) {
                common.DPrintf("local user info modified by server\n");
                cvarSystem.SetCVarsFromDict(gameInfo);
                cvarSystem.ClearModifiedFlags(CVAR_USERINFO); // don't emit back
            }

            msg.Init(msgBuf, msgBuf.capacity());
            msg.WriteByte(SERVER_RELIABLE_MESSAGE_CLIENTINFO.ordinal());
            msg.WriteByte(userInfoNum);
            if (gameModifiedInfo || sendToAll) {
                msg.WriteBits(0, 1);
            } else {
                msg.WriteBits(1, 1);
            }

            if (ID_CLIENTINFO_TAGS) {
                msg.WriteLong((int) sessLocal.mapSpawnData.userInfo[userInfoNum].Checksum());
                common.DPrintf("broadcast for client %d: 0x%x\n", userInfoNum, sessLocal.mapSpawnData.userInfo[userInfoNum].Checksum());
                sessLocal.mapSpawnData.userInfo[userInfoNum].Print();
            }

            if (gameModifiedInfo || sendToAll) {
                msg.WriteDeltaDict(gameInfo, null);
            } else {
                msg.WriteDeltaDict(gameInfo, sessLocal.mapSpawnData.userInfo[userInfoNum]);
            }

            for (int i = 0; i < MAX_ASYNC_CLIENTS; i++) {
                if (clients[i].clientState.ordinal() >= SCS_CONNECTED.ordinal() && (sendToAll || i != userInfoNum || gameModifiedInfo)) {
                    SendReliableMessage(i, msg);
                }
            }

            sessLocal.mapSpawnData.userInfo[userInfoNum] = gameInfo;
        }

        private void SendUserInfoToClient(int clientNum, int userInfoNum, final idDict info) {
            idBitMsg msg = new idBitMsg();
            ByteBuffer msgBuf = ByteBuffer.allocate(MAX_MESSAGE_SIZE);

            if (clients[clientNum].clientState.ordinal() < SCS_CONNECTED.compareTo(SCS_FREE)) {
                return;
            }

            msg.Init(msgBuf, msgBuf.capacity());
            msg.WriteByte(SERVER_RELIABLE_MESSAGE_CLIENTINFO.ordinal());
            msg.WriteByte(userInfoNum);
            msg.WriteBits(0, 1);

            if (ID_CLIENTINFO_TAGS) {
                msg.WriteLong(0);
                common.DPrintf("user info %d to client %d: null base\n", userInfoNum, clientNum);
            }

            msg.WriteDeltaDict(info, null);

            SendReliableMessage(clientNum, msg);
        }

        private void SendSyncedCvarsBroadcast(final idDict cvars) {
            idBitMsg msg = new idBitMsg();
            ByteBuffer msgBuf = ByteBuffer.allocate(MAX_MESSAGE_SIZE);
            int i;

            msg.Init(msgBuf, msgBuf.capacity());
            msg.WriteByte(SERVER_RELIABLE_MESSAGE_SYNCEDCVARS.ordinal());
            msg.WriteDeltaDict(cvars, sessLocal.mapSpawnData.syncedCVars);

            for (i = 0; i < MAX_ASYNC_CLIENTS; i++) {
                if (clients[i].clientState.ordinal() >= SCS_CONNECTED.ordinal()) {
                    SendReliableMessage(i, msg);
                }
            }

            sessLocal.mapSpawnData.syncedCVars = cvars;
        }

        private void SendSyncedCvarsToClient(int clientNum, final idDict cvars) {
            idBitMsg msg = new idBitMsg();
            ByteBuffer msgBuf = ByteBuffer.allocate(MAX_MESSAGE_SIZE);

            if (clients[clientNum].clientState.ordinal() < SCS_CONNECTED.ordinal()) {
                return;
            }

            msg.Init(msgBuf, msgBuf.capacity());
            msg.WriteByte(SERVER_RELIABLE_MESSAGE_SYNCEDCVARS.ordinal());
            msg.WriteDeltaDict(cvars, null);

            SendReliableMessage(clientNum, msg);
        }

        private void SendApplySnapshotToClient(int clientNum, int sequence) {
            idBitMsg msg = new idBitMsg();
            ByteBuffer msgBuf = ByteBuffer.allocate(MAX_MESSAGE_SIZE);

            msg.Init(msgBuf, msgBuf.capacity());
            msg.WriteByte(SERVER_RELIABLE_MESSAGE_APPLYSNAPSHOT.ordinal());
            msg.WriteLong(sequence);

            SendReliableMessage(clientNum, msg);
        }

        private boolean SendEmptyToClient(int clientNum) {
            return SendEmptyToClient(clientNum, false);
        }

        private boolean SendEmptyToClient(int clientNum, boolean force/*= false*/) {
            idBitMsg msg = new idBitMsg();
            ByteBuffer msgBuf = ByteBuffer.allocate(MAX_MESSAGE_SIZE);

            serverClient_s client = clients[clientNum];

            if (client.lastEmptyTime > realTime) {
                client.lastEmptyTime = realTime;
            }

            if (!force && (realTime - client.lastEmptyTime < EMPTY_RESEND_TIME)) {
                return false;
            }

            if (idAsyncNetwork.verbose.GetInteger() != 0) {
                common.Printf("sending empty to client %d: gameInitId = %d, gameFrame = %d, gameTime = %d\n", clientNum, gameInitId, gameFrame, gameTime);
            }

            msg.Init(msgBuf, msgBuf.capacity());
            msg.WriteLong(gameInitId);
            msg.WriteByte(SERVER_UNRELIABLE_MESSAGE_EMPTY.ordinal());

            client.channel.SendMessage(serverPort, serverTime, msg);

            client.lastEmptyTime = realTime;

            return true;
        }

        private boolean SendPingToClient(int clientNum) {
            idBitMsg msg = new idBitMsg();
            ByteBuffer msgBuf = ByteBuffer.allocate(MAX_MESSAGE_SIZE);

            serverClient_s client = clients[clientNum];

            if (client.lastPingTime > realTime) {
                client.lastPingTime = realTime;
            }

            if (realTime - client.lastPingTime < PING_RESEND_TIME) {
                return false;
            }

            if (idAsyncNetwork.verbose.GetInteger() == 2) {
                common.Printf("pinging client %d: gameInitId = %d, gameFrame = %d, gameTime = %d\n", clientNum, gameInitId, gameFrame, gameTime);
            }

            msg.Init(msgBuf, msgBuf.capacity());
            msg.WriteLong(gameInitId);
            msg.WriteByte(SERVER_UNRELIABLE_MESSAGE_PING.ordinal());
            msg.WriteLong(realTime);

            client.channel.SendMessage(serverPort, serverTime, msg);

            client.lastPingTime = realTime;

            return true;
        }

        private void SendGameInitToClient(int clientNum) {
            idBitMsg msg = new idBitMsg();
            ByteBuffer msgBuf = ByteBuffer.allocate(MAX_MESSAGE_SIZE);

            if (idAsyncNetwork.verbose.GetInteger() != 0) {
                common.Printf("sending gameinit to client %d: gameInitId = %d, gameFrame = %d, gameTime = %d\n", clientNum, gameInitId, gameFrame, gameTime);
            }

            serverClient_s client = clients[clientNum];

            // clear the unsent fragments. might flood winsock but that's ok
            while (client.channel.UnsentFragmentsLeft()) {
                client.channel.SendNextFragment(serverPort, serverTime);
            }

            msg.Init(msgBuf, msgBuf.capacity());
            msg.WriteLong(gameInitId);
            msg.WriteByte(SERVER_UNRELIABLE_MESSAGE_GAMEINIT.ordinal());
            msg.WriteLong(gameFrame);
            msg.WriteLong(gameTime);
            msg.WriteDeltaDict(sessLocal.mapSpawnData.serverInfo, null);
            client.gameInitSequence = client.channel.SendMessage(serverPort, serverTime, msg);
        }

        private boolean SendSnapshotToClient(int clientNum) {
            int i, j, index, numUsercmds;
            idBitMsg msg = new idBitMsg();
            ByteBuffer msgBuf = ByteBuffer.allocate(MAX_MESSAGE_SIZE);
            usercmd_t last;
            byte[] clientInPVS = new byte[MAX_ASYNC_CLIENTS >> 3];

            serverClient_s client = clients[clientNum];

            if (serverTime - client.lastSnapshotTime < idAsyncNetwork.serverSnapshotDelay.GetInteger()) {
                return false;
            }

            if (idAsyncNetwork.verbose.GetInteger() == 2) {
                common.Printf("sending snapshot to client %d: gameInitId = %d, gameFrame = %d, gameTime = %d\n", clientNum, gameInitId, gameFrame, gameTime);
            }

            // how far is the client ahead of the server minus the packet delay
            client.clientAheadTime = client.gameTime - (gameTime + gameTimeResidual);

            // write the snapshot
            msg.Init(msgBuf, msgBuf.capacity());
            msg.WriteLong(gameInitId);
            msg.WriteByte(SERVER_UNRELIABLE_MESSAGE_SNAPSHOT.ordinal());
            msg.WriteLong(client.snapshotSequence);
            msg.WriteLong(gameFrame);
            msg.WriteLong(gameTime);
            msg.WriteByte(idMath.ClampChar(client.numDuplicatedUsercmds));
            msg.WriteShort(idMath.ClampShort(client.clientAheadTime));

            // write the game snapshot
            game.ServerWriteSnapshot(clientNum, client.snapshotSequence, msg, clientInPVS, MAX_ASYNC_CLIENTS);

            // write the latest user commands from the other clients in the PVS to the snapshot
            for (last = null, i = 0; i < MAX_ASYNC_CLIENTS; i++) {
                /*serverClient_t*/ client = clients[i];

                if (client.clientState == SCS_FREE || i == clientNum) {
                    continue;
                }

                // if the client is not in the PVS
                if (0 == (clientInPVS[i >> 3] & (1 << (i & 7)))) {
                    continue;
                }

                int maxRelay = idMath.ClampInt(1, MAX_USERCMD_RELAY, idAsyncNetwork.serverMaxUsercmdRelay.GetInteger());

                // Max( 1, to always send at least one cmd, which we know we have because we call DuplicateUsercmds in RunFrame
                numUsercmds = Max(1, Min(client.gameFrame, gameFrame + maxRelay) - gameFrame);
                msg.WriteByte(i);
                msg.WriteByte(numUsercmds);
                for (j = 0; j < numUsercmds; j++) {
                    index = (gameFrame + j) & (MAX_USERCMD_BACKUP - 1);
                    idAsyncNetwork.WriteUserCmdDelta(msg, userCmds[index][i], last);
                    last = userCmds[index][i];
                }
            }
            msg.WriteByte(MAX_ASYNC_CLIENTS);

            client.channel.SendMessage(serverPort, serverTime, msg);

            client.lastSnapshotTime = serverTime;
            client.snapshotSequence++;
            client.numDuplicatedUsercmds = 0;

            return true;
        }

        private void ProcessUnreliableClientMessage(int clientNum, final idBitMsg msg) throws idException {
            int i, id, acknowledgeSequence, clientGameInitId, clientGameFrame, numUsercmds, index;
            usercmd_t last;

            serverClient_s client = clients[clientNum];

            if (client.clientState == SCS_ZOMBIE) {
                return;
            }

            acknowledgeSequence = msg.ReadLong();
            clientGameInitId = msg.ReadLong();

            // while loading a map the client may send empty messages to keep the connection alive
            if (clientGameInitId == GAME_INIT_ID_MAP_LOAD) {
                if (idAsyncNetwork.verbose.GetInteger() != 0) {
                    common.Printf("ignore unreliable msg from client %d, gameInitId == ID_MAP_LOAD\n", clientNum);
                }
                return;
            }

            // check if the client is in the right game
            if (clientGameInitId != gameInitId) {
                if (acknowledgeSequence > client.gameInitSequence) {
                    // the client is connected but not in the right game
                    client.clientState = SCS_CONNECTED;

                    // send game init to client
                    SendGameInitToClient(clientNum);

                    if (sessLocal.mapSpawnData.serverInfo.GetBool("si_pure")) {
                        client.clientState = SCS_PUREWAIT;
                        if (!SendReliablePureToClient(clientNum)) {
                            client.clientState = SCS_CONNECTED;
                        }
                    }
                } else if (idAsyncNetwork.verbose.GetInteger() != 0) {
                    common.Printf("ignore unreliable msg from client %d, wrong gameInit, old sequence\n", clientNum);
                }
                return;
            }

            client.acknowledgeSnapshotSequence = msg.ReadLong();

            if (client.clientState == SCS_CONNECTED) {

                // the client is in the right game
                client.clientState = SCS_INGAME;

                // send the user info of other clients
                for (i = 0; i < MAX_ASYNC_CLIENTS; i++) {
                    if (clients[i].clientState.ordinal() >= SCS_CONNECTED.ordinal() && i != clientNum) {
                        SendUserInfoToClient(clientNum, i, sessLocal.mapSpawnData.userInfo[i]);
                    }
                }

                // send synchronized cvars to client
                SendSyncedCvarsToClient(clientNum, sessLocal.mapSpawnData.syncedCVars);

                SendEnterGameToClient(clientNum);

                // get the client running in the game
                game.ServerClientBegin(clientNum);

                // write any reliable messages to initialize the client game state
                game.ServerWriteInitialReliableMessages(clientNum);
            } else if (client.clientState == SCS_INGAME) {

                // apply the last snapshot the client received
                if (game.ServerApplySnapshot(clientNum, client.acknowledgeSnapshotSequence)) {
                    SendApplySnapshotToClient(clientNum, client.acknowledgeSnapshotSequence);
                }
            }

            // process the unreliable message
            id = msg.ReadByte();
            if (id < CLIENT_UNRELIABLE.values().length) {
                switch (CLIENT_UNRELIABLE.values()[id]) {
                    case CLIENT_UNRELIABLE_MESSAGE_EMPTY: {
                        if (idAsyncNetwork.verbose.GetInteger() != 0) {
                            common.Printf("received empty message for client %d\n", clientNum);
                        }
                        break;
                    }
                    case CLIENT_UNRELIABLE_MESSAGE_PINGRESPONSE: {
                        client.clientPing = realTime - msg.ReadLong();
                        break;
                    }
                    case CLIENT_UNRELIABLE_MESSAGE_USERCMD: {

                        client.clientPrediction = msg.ReadShort();

                        // read user commands
                        clientGameFrame = msg.ReadLong();
                        numUsercmds = msg.ReadByte();
                        for (last = null, i = clientGameFrame - numUsercmds + 1; i <= clientGameFrame; i++) {
                            index = i & (MAX_USERCMD_BACKUP - 1);
                            idAsyncNetwork.ReadUserCmdDelta(msg, userCmds[index][clientNum], last);
                            userCmds[index][clientNum].gameFrame = i;
                            userCmds[index][clientNum].duplicateCount = 0;
                            if (idAsyncNetwork.UsercmdInputChanged(userCmds[(i - 1) & (MAX_USERCMD_BACKUP - 1)][clientNum], userCmds[index][clientNum])) {
                                client.lastInputTime = serverTime;
                            }
                            last = userCmds[index][clientNum];
                        }

                        if (last != null) {
                            client.gameFrame = last.gameFrame;
                            client.gameTime = last.gameTime;
                        }

                        if (idAsyncNetwork.verbose.GetInteger() == 2) {
                            common.Printf("received user command for client %d, gameInitId = %d, gameFrame, %d gameTime %d\n", clientNum, clientGameInitId, client.gameFrame, client.gameTime);
                        }
                        break;
                    }
                }
            } else {
//                default: {
                common.Printf("unknown unreliable message %d from client %d\n", id, clientNum);
//                    break;
            }
        }

        private void ProcessReliableClientMessages(int clientNum) {
            idBitMsg msg = new idBitMsg();
            ByteBuffer msgBuf = ByteBuffer.allocate(MAX_MESSAGE_SIZE);
            int id;

            serverClient_s client = clients[clientNum];

            msg.Init(msgBuf, msgBuf.capacity());

            while (client.channel.GetReliableMessage(msg)) {
                id = msg.ReadByte();
                if (id < CLIENT_RELIABLE.values().length) {
                    switch (CLIENT_RELIABLE.values()[id]) {
                        case CLIENT_RELIABLE_MESSAGE_CLIENTINFO: {
                            idDict info = new idDict();
                            msg.ReadDeltaDict(info, sessLocal.mapSpawnData.userInfo[clientNum]);
                            SendUserInfoBroadcast(clientNum, info);
                            break;
                        }
                        case CLIENT_RELIABLE_MESSAGE_PRINT: {
                            char[] string = new char[MAX_STRING_CHARS];
                            msg.ReadString(string, string.length);
                            common.Printf("%s\n", ctos(string));
                            break;
                        }
                        case CLIENT_RELIABLE_MESSAGE_DISCONNECT: {
                            DropClient(clientNum, "#str_07138");
                            break;
                        }
                        case CLIENT_RELIABLE_MESSAGE_PURE: {
                            // we get this message once the client has successfully updated it's pure list
                            ProcessReliablePure(clientNum, msg);
                            break;
                        }
                        default:
            				// TODO check unused Enum case labels
                            break;
                    }
                } else {
//                    default: {
                    // pass reliable message on to game code
                    game.ServerProcessReliableMessage(clientNum, msg);
//                        break;
                }
            }
        }

        private void ProcessChallengeMessage(final netadr_t from, final idBitMsg msg) throws idException {
            int i, clientId, oldest, oldestTime;
            idBitMsg outMsg = new idBitMsg();
            ByteBuffer msgBuf = ByteBuffer.allocate(MAX_MESSAGE_SIZE);

            clientId = msg.ReadLong();

            oldest = 0;
            oldestTime = 0x7fffffff;

            // see if we already have a challenge for this ip
            for (i = 0; i < MAX_CHALLENGES; i++) {
                if (!challenges[i].connected && Sys_CompareNetAdrBase(from, challenges[i].address) && clientId == challenges[i].clientId) {
                    break;
                }
                if (challenges[i].time < oldestTime) {
                    oldestTime = challenges[i].time;
                    oldest = i;
                }
            }

            if (i >= MAX_CHALLENGES) {
                // this is the first time this client has asked for a challenge
                i = oldest;
                challenges[i].address = from;
                challenges[i].clientId = clientId;
                challenges[i].challenge = (((int) random() << 16) ^ ((int) random())) ^ serverTime;
                challenges[i].time = serverTime;
                challenges[i].connected = false;
                challenges[i].authState = CDK_WAIT;
                challenges[i].authReply = AUTH_NONE;
                challenges[i].authReplyMsg = AUTH_REPLY_WAITING;
                challenges[i].authReplyPrint = new idStr("");
                challenges[i].guid[0] = '\0';
            }
            challenges[i].pingTime = serverTime;

            common.Printf("sending challenge 0x%x to %s\n", challenges[i].challenge, Sys_NetAdrToString(from));

            outMsg.Init(msgBuf, msgBuf.capacity());
            outMsg.WriteShort(CONNECTIONLESS_MESSAGE_ID);
            outMsg.WriteString("challengeResponse");
            outMsg.WriteLong(challenges[i].challenge);
            outMsg.WriteShort(serverId);
            outMsg.WriteString(cvarSystem.GetCVarString("fs_game_base"));
            outMsg.WriteString(cvarSystem.GetCVarString("fs_game"));

            serverPort.SendPacket(from, outMsg.GetData(), outMsg.GetSize());

            if (Sys_IsLANAddress(from)) {
                // no CD Key check for LAN clients
                challenges[i].authState = CDK_OK;
            } else {
                if (idAsyncNetwork.LANServer.GetBool()) {
                    common.Printf("net_LANServer is enabled. Client %s is not a LAN address, will be rejected\n", Sys_NetAdrToString(from));
                    challenges[i].authState = CDK_ONLYLAN;
                } else {
                    // emit a cd key confirmation request
                    outMsg.BeginWriting();
                    outMsg.WriteShort(CONNECTIONLESS_MESSAGE_ID);
                    outMsg.WriteString("srvAuth");
                    outMsg.WriteLong(ASYNC_PROTOCOL_VERSION);
                    outMsg.WriteNetadr(from);
                    outMsg.WriteLong(-1); // this identifies "challenge" auth vs "connect" auth
                    // protocol 1.37 addition
                    outMsg.WriteByte(fileSystem.RunningD3XP() ? 1 : 0);
                    serverPort.SendPacket(idAsyncNetwork.GetMasterAddress(), outMsg.GetData(), outMsg.GetSize());
                }
            }
        }

        private void ProcessConnectMessage(final netadr_t from, final idBitMsg msg) throws idException {
            int clientNum = 0, protocol, clientDataChecksum, challenge, clientId, ping, clientRate;
            idBitMsg outMsg = new idBitMsg();
            ByteBuffer msgBuf = ByteBuffer.allocate(MAX_MESSAGE_SIZE);
            char[] guid = new char[12];
            char[] password = new char[17];
            int i, ichallenge, islot, OS, numClients;

            protocol = msg.ReadLong();
            OS = msg.ReadShort();

            // check the protocol version
            if (protocol != ASYNC_PROTOCOL_VERSION) {
                // that's a msg back to a client, we don't know about it's localization, so send english
                PrintOOB(from, SERVER_PRINT_BADPROTOCOL.ordinal(), va("server uses protocol %d.%d\n", ASYNC_PROTOCOL_MAJOR, ASYNC_PROTOCOL_MINOR));
                return;
            }

            clientDataChecksum = msg.ReadLong();
            challenge = msg.ReadLong();
            clientId = msg.ReadShort();
            clientRate = msg.ReadLong();

            // check the client data - only for non pure servers
            if (0 == sessLocal.mapSpawnData.serverInfo.GetInt("si_pure") && clientDataChecksum != serverDataChecksum.intValue()) {
                PrintOOB(from, SERVER_PRINT_MISC.ordinal(), "#str_04842");
                return;
            }

            if ((ichallenge = ValidateChallenge(from, challenge, clientId)) == -1) {
                return;
            }
            challenges[ichallenge].OS = OS;

            msg.ReadString(guid, guid.length);

            switch (challenges[ichallenge].authState) {
                case CDK_PUREWAIT:
                    SendPureServerMessage(from, OS);
                    return;
                case CDK_ONLYLAN:
                    common.DPrintf("%s: not a lan client\n", Sys_NetAdrToString(from));
                    PrintOOB(from, SERVER_PRINT_MISC.ordinal(), "#str_04843");
                    return;
                case CDK_WAIT:
                    if (challenges[ichallenge].authReply == AUTH_NONE && Min(serverTime - lastAuthTime, serverTime - challenges[ichallenge].time) > AUTHORIZE_TIMEOUT) {
                        common.DPrintf("%s: Authorize server timed out\n", Sys_NetAdrToString(from));
                        break; // will continue with the connecting process
                    }
                    String msg2,
                     l_msg;
                    if (challenges[ichallenge].authReplyMsg != AUTH_REPLY_PRINT) {
                        msg2 = authReplyMsg[challenges[ichallenge].authReplyMsg.ordinal()];
                    } else {
                        msg2 = challenges[ichallenge].authReplyPrint.toString();
                    }
                    l_msg = common.GetLanguageDict().GetString(msg2);

                    common.DPrintf("%s: %s\n", Sys_NetAdrToString(from), l_msg);

                    if (challenges[ichallenge].authReplyMsg == AUTH_REPLY_UNKNOWN || challenges[ichallenge].authReplyMsg == AUTH_REPLY_WAITING) {
                        // the client may be trying to connect to us in LAN mode, and the server disagrees
                        // let the client know so it would switch to authed connection
                        idBitMsg outMsg2 = new idBitMsg();
                        ByteBuffer msgBuf2 = ByteBuffer.allocate(MAX_MESSAGE_SIZE);
                        outMsg2.Init(msgBuf2, msgBuf2.capacity());
                        outMsg2.WriteShort(CONNECTIONLESS_MESSAGE_ID);
                        outMsg2.WriteString("authrequired");
                        serverPort.SendPacket(from, outMsg2.GetData(), outMsg2.GetSize());
                    }

                    PrintOOB(from, SERVER_PRINT_MISC.ordinal(), msg2);

                    // update the guid in the challenges
                    idStr.snPrintf(challenges[ichallenge].guid, sizeof(challenges[ichallenge].guid), ctos(guid));

                    // once auth replied denied, stop sending further requests
                    if (challenges[ichallenge].authReply != AUTH_DENY) {
                        // emit a cd key confirmation request
                        outMsg.Init(msgBuf, msgBuf.capacity());
                        outMsg.WriteShort(CONNECTIONLESS_MESSAGE_ID);
                        outMsg.WriteString("srvAuth");
                        outMsg.WriteLong(ASYNC_PROTOCOL_VERSION);
                        outMsg.WriteNetadr(from);
                        outMsg.WriteLong(clientId);
                        outMsg.WriteString(ctos(guid));
                        // protocol 1.37 addition
                        outMsg.WriteByte(fileSystem.RunningD3XP() ? 1 : 0);
                        serverPort.SendPacket(idAsyncNetwork.GetMasterAddress(), outMsg.GetData(), outMsg.GetSize());
                    }
                    return;
                default:
                    assert (challenges[ichallenge].authState == CDK_OK || challenges[ichallenge].authState == CDK_PUREOK);
            }

            numClients = 0;
            for (i = 0; i < MAX_ASYNC_CLIENTS; i++) {
                serverClient_s client = clients[i];
                if (client.clientState.ordinal() >= SCS_PUREWAIT.ordinal()) {
                    numClients++;
                }
            }

            // game may be passworded, client banned by IP or GUID
            // if authState == CDK_PUREOK, the check was already performed once before entering pure checks
            // but meanwhile, the max players may have been reached
            msg.ReadString(password, password.length);
            char[] reason = new char[MAX_STRING_CHARS];
            allowReply_t reply = game.ServerAllowClient(numClients, Sys_NetAdrToString(from), ctos(guid), ctos(password), reason);
            if (reply != ALLOW_YES) {
                common.DPrintf("game denied connection for %s\n", Sys_NetAdrToString(from));

                // SERVER_PRINT_GAMEDENY passes the game opcode through. Don't use PrintOOB
                outMsg.Init(msgBuf, msgBuf.capacity());
                outMsg.WriteShort(CONNECTIONLESS_MESSAGE_ID);
                outMsg.WriteString("print");
                outMsg.WriteLong(SERVER_PRINT_GAMEDENY.ordinal());
                outMsg.WriteLong(reply.ordinal());
                outMsg.WriteString(ctos(reason));
                serverPort.SendPacket(from, outMsg.GetData(), outMsg.GetSize());

                return;
            }

            // enter pure checks if necessary
            if (sessLocal.mapSpawnData.serverInfo.GetInt("si_pure") != 0 && challenges[ichallenge].authState != CDK_PUREOK) {
                if (SendPureServerMessage(from, OS)) {
                    challenges[ichallenge].authState = CDK_PUREWAIT;
                    return;
                }
            }

            // push back decl checksum here when running pure. just an additional safe check
            if (sessLocal.mapSpawnData.serverInfo.GetInt("si_pure") != 0 && clientDataChecksum != serverDataChecksum.intValue()) {
                PrintOOB(from, SERVER_PRINT_MISC.ordinal(), "#str_04844");
                return;
            }

            ping = serverTime - challenges[ichallenge].pingTime;
            common.Printf("challenge from %s connecting with %d ping\n", Sys_NetAdrToString(from), ping);
            challenges[ichallenge].connected = true;

            // find a slot for the client
            for (islot = 0; islot < 3; islot++) {
                for (clientNum = 0; clientNum < MAX_ASYNC_CLIENTS; clientNum++) {
                    serverClient_s client = clients[clientNum];

                    if (islot == 0) {
                        // if this slot uses the same IP and port
                        if (Sys_CompareNetAdrBase(from, client.channel.GetRemoteAddress())
                                && (clientId == client.clientId || from.port == client.channel.GetRemoteAddress().port)) {
                            break;
                        }
                    } else if (islot == 1) {
                        // if this client is not connected and the slot uses the same IP
                        if (client.clientState.ordinal() >= SCS_PUREWAIT.ordinal()) {
                            continue;
                        }
                        if (Sys_CompareNetAdrBase(from, client.channel.GetRemoteAddress())) {
                            break;
                        }
                    } else if (islot == 2) {
                        // if this slot is free
                        if (client.clientState == SCS_FREE) {
                            break;
                        }
                    }
                }

                if (clientNum < MAX_ASYNC_CLIENTS) {
                    // initialize
                    clients[clientNum].channel.Init(from, serverId);
                    clients[clientNum].OS = OS;
                    System.arraycopy(guid, 0, clients[clientNum].guid, 0, 12);
                    clients[clientNum].guid[11] = 0;
                    break;
                }
            }

            // if no free spots available
            if (clientNum >= MAX_ASYNC_CLIENTS) {
                PrintOOB(from, SERVER_PRINT_MISC.ordinal(), "#str_04845");
                return;
            }

            common.Printf("sending connect response to %s\n", Sys_NetAdrToString(from));

            // send connect response message
            outMsg.Init(msgBuf, msgBuf.capacity());
            outMsg.WriteShort(CONNECTIONLESS_MESSAGE_ID);
            outMsg.WriteString("connectResponse");
            outMsg.WriteLong(clientNum);
            outMsg.WriteLong(gameInitId);
            outMsg.WriteLong(gameFrame);
            outMsg.WriteLong(gameTime);
            outMsg.WriteDeltaDict(sessLocal.mapSpawnData.serverInfo, null);

            serverPort.SendPacket(from, outMsg.GetData(), outMsg.GetSize());

            InitClient(clientNum, clientId, clientRate);

            clients[clientNum].gameInitSequence = 1;
            clients[clientNum].snapshotSequence = 1;

            // clear the challenge struct so a reconnect from this client IP starts clean
//            memset( & challenges[ichallenge], 0, sizeof(challenge_t));
            challenges[ichallenge] = new challenge_s();
        }

        private void ProcessRemoteConsoleMessage(final netadr_t from, final idBitMsg msg) {
            idBitMsg outMsg;
            StringBuilder msgBuf = new StringBuilder(952);
            char[] string = new char[MAX_STRING_CHARS];

            if (idAsyncNetwork.serverRemoteConsolePassword.GetString().isEmpty()) {
                PrintOOB(from, SERVER_PRINT_MISC.ordinal(), "#str_04846");
                return;
            }

            msg.ReadString(string, string.length);

            if (idStr.Icmp(ctos(string), idAsyncNetwork.serverRemoteConsolePassword.GetString()) != 0) {
                PrintOOB(from, SERVER_PRINT_MISC.ordinal(), "#str_04847");
                return;
            }

            msg.ReadString(string, string.length);

            common.Printf("rcon from %s: %s\n", Sys_NetAdrToString(from), string);

            rconAddress = from;
            noRconOutput = true;
            common.BeginRedirect(msgBuf, msgBuf.capacity(), RConRedirect.getInstance());

            cmdSystem.BufferCommandText(CMD_EXEC_NOW, ctos(string));

            common.EndRedirect();

            if (noRconOutput) {
                PrintOOB(rconAddress, SERVER_PRINT_RCON.ordinal(), "#str_04848");
            }
        }

        private void ProcessGetInfoMessage(final netadr_t from, final idBitMsg msg) {
            int i, challenge;
            idBitMsg outMsg = new idBitMsg();
            ByteBuffer msgBuf = ByteBuffer.allocate(MAX_MESSAGE_SIZE);

            if (!IsActive()) {
                return;
            }

            common.DPrintf("Sending info response to %s\n", Sys_NetAdrToString(from));

            challenge = msg.ReadLong();

            outMsg.Init(msgBuf, msgBuf.capacity());
            outMsg.WriteShort(CONNECTIONLESS_MESSAGE_ID);
            outMsg.WriteString("infoResponse");
            outMsg.WriteLong(challenge);
            outMsg.WriteLong(ASYNC_PROTOCOL_VERSION);
            outMsg.WriteDeltaDict(sessLocal.mapSpawnData.serverInfo, null);

            for (i = 0; i < MAX_ASYNC_CLIENTS; i++) {
                serverClient_s client = clients[i];

                if (client.clientState.ordinal() < SCS_CONNECTED.ordinal()) {
                    continue;
                }

                outMsg.WriteByte(i);
                outMsg.WriteShort(client.clientPing);
                outMsg.WriteLong(client.channel.GetMaxOutgoingRate());
                outMsg.WriteString(sessLocal.mapSpawnData.userInfo[i].GetString("ui_name", "Player"));
            }
            outMsg.WriteByte(MAX_ASYNC_CLIENTS);
            outMsg.WriteLong(fileSystem.GetOSMask());

            serverPort.SendPacket(from, outMsg.GetData(), outMsg.GetSize());
        }

        private boolean ConnectionlessMessage(final netadr_t from, final idBitMsg msg) {
            char[] chrs = new char[MAX_STRING_CHARS * 2];  // M. Quinn - Even Balance - PB Packets need more than 1024
            String string;

            msg.ReadString(chrs, chrs.length);
            string = ctos(chrs);

            // info request
            if (idStr.Icmp(string, "getInfo") == 0) {
                ProcessGetInfoMessage(from, msg);
                return false;
            }

            // remote console
            if (idStr.Icmp(string, "rcon") == 0) {
                ProcessRemoteConsoleMessage(from, msg);
                return true;
            }

            if (!active) {
                PrintOOB(from, SERVER_PRINT_MISC.ordinal(), "#str_04849");
                return false;
            }

            // challenge from a client
            if (idStr.Icmp(string, "challenge") == 0) {
                ProcessChallengeMessage(from, msg);
                return false;
            }

            // connect from a client
            if (idStr.Icmp(string, "connect") == 0) {
                ProcessConnectMessage(from, msg);
                return false;
            }

            // pure mesasge from a client
            if (idStr.Icmp(string, "pureClient") == 0) {
                ProcessPureMessage(from, msg);
                return false;
            }

            // download request
            if (idStr.Icmp(string, "downloadRequest") == 0) {
                ProcessDownloadRequestMessage(from, msg);
            }

            // auth server
            if (idStr.Icmp(string, "auth") == 0) {
                if (!Sys_CompareNetAdrBase(from, idAsyncNetwork.GetMasterAddress())) {
                    common.Printf("auth: bad source %s\n", Sys_NetAdrToString(from));
                    return false;
                }
                if (idAsyncNetwork.LANServer.GetBool()) {
                    common.Printf("auth message from master. net_LANServer is enabled, ignored.\n");
                }
                ProcessAuthMessage(msg);
                return false;
            }

            return false;
        }

        private boolean ProcessMessage(final netadr_t from, idBitMsg msg) {
            int i, id;
            int[] sequence = new int[1];
            idBitMsg outMsg = new idBitMsg();
            ByteBuffer msgBuf = ByteBuffer.allocate(MAX_MESSAGE_SIZE);

            id = msg.ReadShort();

            // check for a connectionless message
            if (id == CONNECTIONLESS_MESSAGE_ID) {
                return ConnectionlessMessage(from, msg);
            }

            if (msg.GetRemaingData() < 4) {
                common.DPrintf("%s: tiny packet\n", Sys_NetAdrToString(from));
                return false;
            }

            // find out which client the message is from
            for (i = 0; i < MAX_ASYNC_CLIENTS; i++) {
                serverClient_s client = clients[i];

                if (client.clientState == SCS_FREE) {
                    continue;
                }

                // This does not compare the UDP port, because some address translating
                // routers will change that at arbitrary times.
                if (!Sys_CompareNetAdrBase(from, client.channel.GetRemoteAddress()) || id != client.clientId) {
                    continue;
                }

                // make sure it is a valid, in sequence packet
                if (!client.channel.Process(from, serverTime, msg, sequence)) {
                    return false;		// out of order, duplicated, fragment, etc.
                }

                // zombie clients still need to do the channel processing to make sure they don't
                // need to retransmit the final reliable message, but they don't do any other processing
                if (client.clientState == SCS_ZOMBIE) {
                    return false;
                }

                client.lastPacketTime = serverTime;

                ProcessReliableClientMessages(i);
                ProcessUnreliableClientMessage(i, msg);

                return false;
            }

            // if we received a sequenced packet from an address we don't recognize,
            // send an out of band disconnect packet to it
            outMsg.Init(msgBuf, msgBuf.capacity());
            outMsg.WriteShort(CONNECTIONLESS_MESSAGE_ID);
            outMsg.WriteString("disconnect");
            serverPort.SendPacket(from, outMsg.GetData(), outMsg.GetSize());

            return false;
        }

        private void ProcessAuthMessage(final idBitMsg msg) throws idException {
            netadr_t client_from = new netadr_t();
            char[] client_guid = new char[12], string = new char[MAX_STRING_CHARS];
            int i, clientId;
            authReply_t reply;
            authReplyMsg_t replyMsg = AUTH_REPLY_WAITING;
            idStr replyPrintMsg = new idStr();

            reply = authReply_t.values()[msg.ReadByte()];
            if (reply.ordinal() <= 0 || reply.ordinal() >= AUTH_MAXSTATES.ordinal()) {
                common.DPrintf("auth: invalid reply %d\n", reply);
                return;
            }
            clientId = msg.ReadShort();
            msg.ReadNetadr(client_from);
            msg.ReadString(client_guid, client_guid.length);
            if (reply != AUTH_OK) {
                replyMsg = authReplyMsg_t.values()[msg.ReadByte()];
                if (replyMsg.ordinal() <= 0 || replyMsg.ordinal() >= AUTH_REPLY_MAXSTATES.ordinal()) {
                    common.DPrintf("auth: invalid reply msg %d\n", replyMsg);
                    return;
                }
                if (replyMsg == AUTH_REPLY_PRINT) {
                    msg.ReadString(string, MAX_STRING_CHARS);
                    replyPrintMsg.oSet(ctos(string));
                }
            }

            lastAuthTime = serverTime;

            // no message parsing below
            for (i = 0; i < MAX_CHALLENGES; i++) {
                if (!challenges[i].connected && challenges[i].clientId == clientId) {
                    // return if something is wrong
                    // break if we have found a valid auth
                    if (0 == strLen(challenges[i].guid)) {
                        common.DPrintf("auth: client %s has no guid yet\n", Sys_NetAdrToString(challenges[i].address));
                        return;
                    }
                    if (idStr.Cmp(challenges[i].guid, client_guid) != 0) {
                        common.DPrintf("auth: client %s %s not matched, auth server says guid %s\n", Sys_NetAdrToString(challenges[i].address), challenges[i].guid, client_guid);
                        return;
                    }
                    if (!Sys_CompareNetAdrBase(client_from, challenges[i].address)) {
                        // let auth work when server and master don't see the same IP
                        common.DPrintf("auth: matched guid '%s' for != IPs %s and %s\n", client_guid, Sys_NetAdrToString(client_from), Sys_NetAdrToString(challenges[i].address));
                    }
                    break;
                }
            }
            if (i >= MAX_CHALLENGES) {
                common.DPrintf("auth: failed client lookup %s %s\n", Sys_NetAdrToString(client_from), client_guid);
                return;
            }

            if (challenges[i].authState != CDK_WAIT) {
                common.DWarning("auth: challenge 0x%x %s authState %d != CDK_WAIT", challenges[i].challenge, Sys_NetAdrToString(challenges[i].address), challenges[i].authState);
                return;
            }

            idStr.snPrintf(challenges[i].guid, 12, ctos(client_guid));
            if (reply == AUTH_OK) {
                challenges[i].authState = CDK_OK;
                common.Printf("client %s %s is authed\n", Sys_NetAdrToString(client_from), client_guid);
            } else {
                final String msg1;
                if (replyMsg != AUTH_REPLY_PRINT) {
                    msg1 = authReplyMsg[replyMsg.ordinal()];
                } else {
                    msg1 = replyPrintMsg.toString();
                }
                // maybe localize it
                final String l_msg = common.GetLanguageDict().GetString(msg1);
                common.DPrintf("auth: client %s %s - %s %s\n", Sys_NetAdrToString(client_from), client_guid, authReplyStr[reply.ordinal()], l_msg);
                challenges[i].authReply = reply;
                challenges[i].authReplyMsg = replyMsg;
                challenges[i].authReplyPrint = replyPrintMsg;
            }
        }

        private boolean SendPureServerMessage(final netadr_t to, int OS) {										// returns false if no pure paks on the list
            idBitMsg outMsg = new idBitMsg();
            ByteBuffer msgBuf = ByteBuffer.allocate(MAX_MESSAGE_SIZE);
            int[] serverChecksums = new int[MAX_PURE_PAKS];
            int[] gamePakChecksum = new int[1];
            int i;

            fileSystem.GetPureServerChecksums(serverChecksums, OS, gamePakChecksum);
            if (0 == serverChecksums[0]) {
                // happens if you run fully expanded assets with si_pure 1
                common.Warning("pure server has no pak files referenced");
                return false;
            }
            common.DPrintf("client %s: sending pure pak list\n", Sys_NetAdrToString(to));

            // send our list of required paks
            outMsg.Init(msgBuf, msgBuf.capacity());
            outMsg.WriteShort(CONNECTIONLESS_MESSAGE_ID);
            outMsg.WriteString("pureServer");

            i = 0;
            while (serverChecksums[i] != 0) {
                outMsg.WriteLong(serverChecksums[i++]);
            }
            outMsg.WriteLong(0);

            // write the pak checksum for game code
            outMsg.WriteLong(gamePakChecksum[0]);

            serverPort.SendPacket(to, outMsg.GetData(), outMsg.GetSize());
            return true;
        }

        private void ProcessPureMessage(final netadr_t from, final idBitMsg msg) {
            int iclient, challenge, clientId;
            idStr reply = new idStr();

            challenge = msg.ReadLong();
            clientId = msg.ReadShort();

            if ((iclient = ValidateChallenge(from, challenge, clientId)) == -1) {
                return;
            }

            if (challenges[iclient].authState != CDK_PUREWAIT) {
                common.DPrintf("client %s: got pure message, not in CDK_PUREWAIT\n", Sys_NetAdrToString(from));
                return;
            }

            if (!VerifyChecksumMessage(iclient, from, msg, reply, challenges[iclient].OS)) {
                PrintOOB(from, SERVER_PRINT_MISC.ordinal(), reply.toString());
                return;
            }

            common.DPrintf("client %s: passed pure checks\n", Sys_NetAdrToString(from));
            challenges[iclient].authState = CDK_PUREOK; // next connect message will get the client through completely
        }

        private int ValidateChallenge(final netadr_t from, int challenge, int clientId) {	// returns -1 if validate failed
            int i;
            for (i = 0; i < MAX_ASYNC_CLIENTS; i++) {
                final serverClient_s client = clients[i];

                if (client.clientState == SCS_FREE) {
                    continue;
                }
                if (Sys_CompareNetAdrBase(from, client.channel.GetRemoteAddress())
                        && (clientId == client.clientId || from.port == client.channel.GetRemoteAddress().port)) {
                    if (serverTime - client.lastConnectTime < MIN_RECONNECT_TIME) {
                        common.Printf("%s: reconnect rejected : too soon\n", Sys_NetAdrToString(from));
                        return -1;
                    }
                    break;
                }
            }

            for (i = 0; i < MAX_CHALLENGES; i++) {
                if (Sys_CompareNetAdrBase(from, challenges[i].address) && from.port == challenges[i].address.port) {
                    if (challenge == challenges[i].challenge) {
                        break;
                    }
                }
            }
            if (i == MAX_CHALLENGES) {
                PrintOOB(from, SERVER_PRINT_BADCHALLENGE.ordinal(), "#str_04840");
                return -1;
            }
            return i;
        }

        private boolean SendReliablePureToClient(int clientNum) {
            idBitMsg msg = new idBitMsg();
            ByteBuffer msgBuf = ByteBuffer.allocate(MAX_MESSAGE_SIZE);
            int[] serverChecksums = new int[MAX_PURE_PAKS];
            int i;
            int[] gamePakChecksum = new int[1];

            fileSystem.GetPureServerChecksums(serverChecksums, clients[clientNum].OS, gamePakChecksum);
            if (0 == serverChecksums[0]) {
                // happens if you run fully expanded assets with si_pure 1
                common.Warning("pure server has no pak files referenced");
                return false;
            }

            common.DPrintf("client %d: sending pure pak list (reliable channel) @ gameInitId %d\n", clientNum, gameInitId);

            msg.Init(msgBuf, msgBuf.capacity());
            msg.WriteByte(SERVER_RELIABLE_MESSAGE_PURE.ordinal());

            msg.WriteLong(gameInitId);

            i = 0;
            while (serverChecksums[i] != 0) {
                msg.WriteLong(serverChecksums[i++]);
            }
            msg.WriteLong(0);
            msg.WriteLong(gamePakChecksum[0]);

            SendReliableMessage(clientNum, msg);

            return true;
        }

        private void ProcessReliablePure(int clientNum, final idBitMsg msg) {
            idStr reply = new idStr();
            idBitMsg outMsg = new idBitMsg();
            ByteBuffer msgBuf = ByteBuffer.allocate(MAX_MESSAGE_SIZE);
            int clientGameInitId;

            clientGameInitId = msg.ReadLong();
            if (clientGameInitId != gameInitId) {
                common.DPrintf("client %d: ignoring reliable pure from an old gameInit (%d)\n", clientNum, clientGameInitId);
                return;
            }

            if (clients[clientNum].clientState != SCS_PUREWAIT) {
                // should not happen unless something is very wrong. still, don't let this crash us, just get rid of the client
                common.DPrintf("client %d: got reliable pure while != SCS_PUREWAIT, sending a reload\n", clientNum);
                outMsg.Init(msgBuf, msgBuf.capacity());
                outMsg.WriteByte(SERVER_RELIABLE_MESSAGE_RELOAD.ordinal());
                SendReliableMessage(clientNum, msg);
                // go back to SCS_CONNECTED to sleep on the client until it goes away for a reconnect
                clients[clientNum].clientState = SCS_CONNECTED;
                return;
            }

            if (!VerifyChecksumMessage(clientNum, null, msg, reply, clients[clientNum].OS)) {
                reply.oSet(DropClient(clientNum, reply.toString()));
                return;
            }
            common.DPrintf("client %d: passed pure checks (reliable channel)\n", clientNum);
            clients[clientNum].clientState = SCS_CONNECTED;
        }

        private boolean VerifyChecksumMessage(int clientNum, final netadr_t from, final idBitMsg msg, idStr reply, int OS) { // if from is null, clientNum is used for error messages
            int i, numChecksums;
            int[] checksums = new int[MAX_PURE_PAKS];
            int gamePakChecksum;
            int[] serverChecksums = new int[MAX_PURE_PAKS];
            int[] serverGamePakChecksum = new int[1];

            // pak checksums, in a 0-terminated list
            numChecksums = 0;
            do {
                i = msg.ReadLong();
                checksums[numChecksums++] = i;
                // just to make sure a broken client doesn't crash us
                if (numChecksums >= MAX_PURE_PAKS) {
                    common.Warning("MAX_PURE_PAKS ( %d ) exceeded in idAsyncServer.ProcessPureMessage\n", MAX_PURE_PAKS);
                    reply.oSet("#str_07144");
                    return false;
                }
            } while (i != 0);
            numChecksums--;

            // code pak checksum
            gamePakChecksum = msg.ReadLong();

            fileSystem.GetPureServerChecksums(serverChecksums, OS, serverGamePakChecksum);
            assert (serverChecksums[0] != 0);

            // compare the lists
            if (serverGamePakChecksum[0] != gamePakChecksum) {
                common.Printf("client %s: invalid game code pak ( 0x%x )\n", from != null ? Sys_NetAdrToString(from) : va("%d", clientNum), gamePakChecksum);
                reply.oSet("#str_07145");
                return false;
            }
            for (i = 0; serverChecksums[i] != 0; i++) {
                if (checksums[i] != serverChecksums[i]) {
                    common.DPrintf("client %s: pak missing ( 0x%x )\n", from != null ? Sys_NetAdrToString(from) : va("%d", clientNum), serverChecksums[i]);
                    reply.oSet(String.format("pak missing ( 0x%x )\n", serverChecksums[i]));
                    return false;
                }
            }
            if (checksums[i] != 0) {
                common.DPrintf("client %s: extra pak file referenced ( 0x%x )\n", from != null ? Sys_NetAdrToString(from) : va("%d", clientNum), checksums[i]);
                reply.oSet(String.format("extra pak file referenced ( 0x%x )\n", checksums[i]));
                return false;
            }
            return true;
        }

        private void SendReliableMessage(int clientNum, final idBitMsg msg) {				// checks for overflow and disconnects the faulty client
            if (clientNum == localClientNum) {
                return;
            }
            if (!clients[clientNum].channel.SendReliableMessage(msg)) {
                clients[clientNum].channel.ClearReliableMessages();
                DropClient(clientNum, "#str_07136");
            }
        }

        private int UpdateTime(int clamp) {
            int time, msec;

            time = Sys_Milliseconds();
            msec = idMath.ClampInt(0, clamp, time - realTime);
            realTime = time;
            serverTime += msec;
            return msec;
        }

        private void SendEnterGameToClient(int clientNum) {
            idBitMsg msg = new idBitMsg();
            ByteBuffer msgBuf = ByteBuffer.allocate(MAX_MESSAGE_SIZE);

            msg.Init(msgBuf, msgBuf.capacity());
            msg.WriteByte(SERVER_RELIABLE_MESSAGE_ENTERGAME.ordinal());
            SendReliableMessage(clientNum, msg);
        }

        private void ProcessDownloadRequestMessage(final netadr_t from, final idBitMsg msg) throws idException {
            int challenge, clientId, iclient, numPaks, i;
            int dlGamePak;
            int dlPakChecksum;
            int[] dlSize = new int[MAX_PURE_PAKS];	// sizes
            idStrList pakNames = new idStrList();	// relative path
            idStrList pakURLs = new idStrList();	// game URLs
            char[] pakbuf = new char[MAX_STRING_CHARS];
            idStr paklist = new idStr();
            ByteBuffer msgBuf = ByteBuffer.allocate(MAX_MESSAGE_SIZE);
            ByteBuffer tmpBuf = ByteBuffer.allocate(MAX_MESSAGE_SIZE);
            idBitMsg outMsg = new idBitMsg(), tmpMsg = new idBitMsg();
            int dlRequest;
            int voidSlots = 0;				// to count and verbose the right number of paks requested for downloads

            challenge = msg.ReadLong();
            clientId = msg.ReadShort();
            dlRequest = msg.ReadLong();

            if ((iclient = ValidateChallenge(from, challenge, clientId)) == -1) {
                return;
            }

            if (challenges[iclient].authState != CDK_PUREWAIT) {
                common.DPrintf("client %s: got download request message, not in CDK_PUREWAIT\n", Sys_NetAdrToString(from));
                return;
            }

            // the first token of the pak names list passed to the game will be empty if no game pak is requested
            dlGamePak = msg.ReadLong();
            if (dlGamePak != 0) {
                if (0 == (dlSize[0] = fileSystem.ValidateDownloadPakForChecksum(dlGamePak, pakbuf, true))) {
                    common.Warning("client requested unknown game pak 0x%x", dlGamePak);
                    pakbuf[0] = '\0';
                    voidSlots++;
                }
            } else {
                pakbuf[0] = '\0';
                voidSlots++;
            }
            pakNames.Append(new idStr(pakbuf));
            numPaks = 1;

            // read the checksums, build path names and pass that to the game code
            dlPakChecksum = msg.ReadLong();
            while (dlPakChecksum != 0) {
                if (0 == (dlSize[numPaks] = fileSystem.ValidateDownloadPakForChecksum(dlPakChecksum, pakbuf, false))) {
                    // we pass an empty token to the game so our list doesn't get offset
                    common.Warning("client requested an unknown pak 0x%x", dlPakChecksum);
                    pakbuf[0] = '\0';
                    voidSlots++;
                }
                pakNames.Append(new idStr(pakbuf));
                numPaks++;
                dlPakChecksum = msg.ReadLong();
            }

            for (i = 0; i < pakNames.Num(); i++) {
                if (i > 0) {
                    paklist.oPluSet(";");
                }
                paklist.oPluSet(pakNames.oGet(i).toString());
            }

            // read the message and pass it to the game code
            common.DPrintf("got download request for %d paks - %s\n", numPaks - voidSlots, paklist.toString());

            outMsg.Init(msgBuf, msgBuf.capacity());
            outMsg.WriteShort(CONNECTIONLESS_MESSAGE_ID);
            outMsg.WriteString("downloadInfo");
            outMsg.WriteLong(dlRequest);
            if (!game.DownloadRequest(Sys_NetAdrToString(from), ctos(challenges[iclient].guid), paklist.toString(), pakbuf)) {
                common.DPrintf("game: no downloads\n");
                outMsg.WriteByte(SERVER_DL_NONE.ordinal());
                serverPort.SendPacket(from, outMsg.GetData(), outMsg.GetSize());
                return;
            }

            String token;
            int type = 0, next;

            token = ctos(pakbuf);
            next = token.indexOf(';');
            while (isNotNullOrEmpty(token)) {
                if (next != -1) {
                    pakbuf[next] = '\0';
                }

                if (type == 0) {
                    type = Integer.parseInt(token);
                } else if (type == SERVER_DL_REDIRECT.ordinal()) {
                    common.DPrintf("download request: redirect to URL %s\n", token);
                    outMsg.WriteByte(SERVER_DL_REDIRECT.ordinal());
                    outMsg.WriteString(token);
                    serverPort.SendPacket(from, outMsg.GetData(), outMsg.GetSize());
                    return;
                } else if (type == SERVER_DL_LIST.ordinal()) {
                    pakURLs.Append(new idStr(token));
                } else {
                    common.DPrintf("wrong op type %d\n", type);
                    next = -1;
                    token = null;
                }

                if (next != -1) {
                    token = token.substring(++next);
                    next = token.indexOf(';');
                } else {
                    token = null;
                }
            }

            if (type == SERVER_DL_LIST.ordinal()) {
                int totalDlSize = 0;
                int numActualPaks = 0;

                // put the answer packet together
                outMsg.WriteByte(SERVER_DL_LIST.ordinal());

                tmpMsg.Init(tmpBuf, MAX_MESSAGE_SIZE);

                for (i = 0; i < pakURLs.Num(); i++) {
                    tmpMsg.BeginWriting();
                    if (0 == dlSize[i] || 0 == pakURLs.oGet(i).Length()) {
                        // still send the relative path so the client knows what it missed
                        tmpMsg.WriteByte(SERVER_PAK_NO.ordinal());
                        tmpMsg.WriteString(pakNames.oGet(i).toString());
                    } else {
                        totalDlSize += dlSize[i];
                        numActualPaks++;
                        tmpMsg.WriteByte(SERVER_PAK_YES.ordinal());
                        tmpMsg.WriteString(pakNames.oGet(i).toString());
                        tmpMsg.WriteString(pakURLs.oGet(i).toString());
                        tmpMsg.WriteLong(dlSize[i]);
                    }

                    // keep last 5 bytes for an 'end of message' - SERVER_PAK_END and the totalDlSize long
                    if (outMsg.GetRemainingSpace() - tmpMsg.GetSize() > 5) {
                        outMsg.WriteData(tmpMsg.GetData(), tmpMsg.GetSize());
                    } else {
                        outMsg.WriteByte(SERVER_PAK_END.ordinal());
                        break;
                    }
                }
                if (i == pakURLs.Num()) {
                    // put a closure even if size not exceeded
                    outMsg.WriteByte(SERVER_PAK_END.ordinal());
                }
                common.DPrintf("download request: download %d paks, %d bytes\n", numActualPaks, totalDlSize);

                serverPort.SendPacket(from, outMsg.GetData(), outMsg.GetSize());
            }
        }
    };

    /*
     ==================
     RConRedirect
     ==================
     */
    static class RConRedirect extends void_callback<String> {

        private static final void_callback instance = new RConRedirect();

        public static void_callback getInstance() {
            return instance;
        }

        @Override
        public void run(String... objects) throws idException {
            idAsyncNetwork.server.RemoteConsoleOutput(objects[0]);
        }
    };

}
