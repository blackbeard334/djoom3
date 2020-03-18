package neo.framework.Async;

import static neo.Game.Game.allowReply_t.ALLOW_BADPASS;
import static neo.Game.Game.allowReply_t.ALLOW_NO;
import static neo.Game.Game.allowReply_t.ALLOW_YES;
import static neo.Game.Game_local.game;
import static neo.Sound.snd_system.soundSystem;
import static neo.TempDump.NOT;
import static neo.TempDump.ctos;
import static neo.TempDump.isNotNullOrEmpty;
import static neo.TempDump.memcmp;
import static neo.framework.Async.AsyncClient.authKeyMsg_t.AUTHKEY_BADKEY;
import static neo.framework.Async.AsyncClient.clientState_t.CS_CHALLENGING;
import static neo.framework.Async.AsyncClient.clientState_t.CS_CONNECTED;
import static neo.framework.Async.AsyncClient.clientState_t.CS_CONNECTING;
import static neo.framework.Async.AsyncClient.clientState_t.CS_DISCONNECTED;
import static neo.framework.Async.AsyncClient.clientState_t.CS_INGAME;
import static neo.framework.Async.AsyncClient.clientState_t.CS_PURERESTART;
import static neo.framework.Async.AsyncClient.clientUpdateState_t.UPDATE_DLING;
import static neo.framework.Async.AsyncClient.clientUpdateState_t.UPDATE_DONE;
import static neo.framework.Async.AsyncClient.clientUpdateState_t.UPDATE_NONE;
import static neo.framework.Async.AsyncClient.clientUpdateState_t.UPDATE_READY;
import static neo.framework.Async.AsyncClient.clientUpdateState_t.UPDATE_SENT;
import static neo.framework.Async.AsyncNetwork.ASYNC_PROTOCOL_MINOR;
import static neo.framework.Async.AsyncNetwork.ASYNC_PROTOCOL_VERSION;
import static neo.framework.Async.AsyncNetwork.GAME_INIT_ID_INVALID;
import static neo.framework.Async.AsyncNetwork.GAME_INIT_ID_MAP_LOAD;
import static neo.framework.Async.AsyncNetwork.MAX_ASYNC_CLIENTS;
import static neo.framework.Async.AsyncNetwork.MAX_NICKLEN;
import static neo.framework.Async.AsyncNetwork.MAX_SERVER_PORTS;
import static neo.framework.Async.AsyncNetwork.MAX_USERCMD_BACKUP;
import static neo.framework.Async.AsyncNetwork.MAX_USERCMD_RELAY;
import static neo.framework.Async.AsyncNetwork.CLIENT_RELIABLE.CLIENT_RELIABLE_MESSAGE_CLIENTINFO;
import static neo.framework.Async.AsyncNetwork.CLIENT_RELIABLE.CLIENT_RELIABLE_MESSAGE_DISCONNECT;
import static neo.framework.Async.AsyncNetwork.CLIENT_RELIABLE.CLIENT_RELIABLE_MESSAGE_GAME;
import static neo.framework.Async.AsyncNetwork.CLIENT_RELIABLE.CLIENT_RELIABLE_MESSAGE_PURE;
import static neo.framework.Async.AsyncNetwork.CLIENT_UNRELIABLE.CLIENT_UNRELIABLE_MESSAGE_EMPTY;
import static neo.framework.Async.AsyncNetwork.CLIENT_UNRELIABLE.CLIENT_UNRELIABLE_MESSAGE_PINGRESPONSE;
import static neo.framework.Async.AsyncNetwork.CLIENT_UNRELIABLE.CLIENT_UNRELIABLE_MESSAGE_USERCMD;
import static neo.framework.Async.AsyncNetwork.SERVER_DL.SERVER_DL_LIST;
import static neo.framework.Async.AsyncNetwork.SERVER_DL.SERVER_DL_REDIRECT;
import static neo.framework.Async.AsyncNetwork.SERVER_PAK.SERVER_PAK_END;
import static neo.framework.Async.AsyncNetwork.SERVER_PAK.SERVER_PAK_NO;
import static neo.framework.Async.AsyncNetwork.SERVER_PAK.SERVER_PAK_YES;
import static neo.framework.Async.AsyncNetwork.SERVER_PRINT.SERVER_PRINT_BADCHALLENGE;
import static neo.framework.Async.AsyncNetwork.SERVER_PRINT.SERVER_PRINT_GAMEDENY;
import static neo.framework.Async.MsgChannel.CONNECTIONLESS_MESSAGE_ID;
import static neo.framework.Async.MsgChannel.CONNECTIONLESS_MESSAGE_ID_MASK;
import static neo.framework.Async.MsgChannel.MAX_MESSAGE_SIZE;
import static neo.framework.BuildDefines.ID_CLIENTINFO_TAGS;
import static neo.framework.BuildDefines.ID_FAKE_PURE;
import static neo.framework.CVarSystem.CVAR_CHEAT;
import static neo.framework.CVarSystem.CVAR_USERINFO;
import static neo.framework.CVarSystem.cvarSystem;
import static neo.framework.CmdSystem.cmdSystem;
import static neo.framework.CmdSystem.cmdExecution_t.CMD_EXEC_APPEND;
import static neo.framework.CmdSystem.cmdExecution_t.CMD_EXEC_NOW;
import static neo.framework.Common.common;
import static neo.framework.DeclManager.declManager;
import static neo.framework.FileSystem_h.MAX_PURE_PAKS;
import static neo.framework.FileSystem_h.fileSystem;
import static neo.framework.FileSystem_h.dlMime_t.FILE_EXEC;
import static neo.framework.FileSystem_h.dlStatus_t.DL_DONE;
import static neo.framework.FileSystem_h.dlStatus_t.DL_WAIT;
import static neo.framework.FileSystem_h.dlType_t.DLTYPE_URL;
import static neo.framework.File_h.fsOrigin_t.FS_SEEK_END;
import static neo.framework.File_h.fsOrigin_t.FS_SEEK_SET;
import static neo.framework.Licensee.ASYNC_PROTOCOL_MAJOR;
import static neo.framework.Licensee.PORT_SERVER;
import static neo.framework.Session.sessLocal;
import static neo.framework.Session.session;
import static neo.framework.Session.msgBoxType_t.MSG_ABORT;
import static neo.framework.Session.msgBoxType_t.MSG_CDKEY;
import static neo.framework.Session.msgBoxType_t.MSG_OK;
import static neo.framework.Session.msgBoxType_t.MSG_PROMPT;
import static neo.framework.Session.msgBoxType_t.MSG_YESNO;
import static neo.framework.UsercmdGen.USERCMD_MSEC;
import static neo.framework.UsercmdGen.usercmdGen;
import static neo.idlib.Lib.MAX_STRING_CHARS;
import static neo.idlib.Lib.Min;
import static neo.idlib.Lib.idLib.sys;
import static neo.idlib.Text.Str.va;
import static neo.idlib.Text.Str.Measure_t.MEASURE_SIZE;
import static neo.idlib.math.Math_h.INTSIGNBITSET;
import static neo.sys.sys_public.BUILD_OS_ID;
import static neo.sys.sys_public.PORT_ANY;
import static neo.sys.sys_public.netadrtype_t.NA_BROADCAST;
import static neo.sys.sys_public.netadrtype_t.NA_LOOPBACK;
import static neo.sys.win_net.Sys_CompareNetAdrBase;
import static neo.sys.win_net.Sys_NetAdrToString;
import static neo.sys.win_net.Sys_StringToNetAdr;
import static neo.sys.win_shared.Sys_Milliseconds;
import static neo.ui.UserInterface.uiManager;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Arrays;

import neo.Game.Game.gameReturn_t;
import neo.framework.FileSystem_h.backgroundDownload_s;
import neo.framework.FileSystem_h.dlMime_t;
import neo.framework.FileSystem_h.fsPureReply_t;
import neo.framework.File_h.idFile;
import neo.framework.File_h.idFile_Permanent;
import neo.framework.Session.HandleGuiCommand_t;
import neo.framework.UsercmdGen.usercmd_t;
import neo.framework.Async.AsyncNetwork.SERVER_RELIABLE;
import neo.framework.Async.AsyncNetwork.SERVER_UNRELIABLE;
import neo.framework.Async.AsyncNetwork.idAsyncNetwork;
import neo.framework.Async.MsgChannel.idMsgChannel;
import neo.framework.Async.ServerScan.idServerScan;
import neo.framework.Async.ServerScan.networkServer_t;
import neo.idlib.BitMsg.idBitMsg;
import neo.idlib.Dict_h.idDict;
import neo.idlib.Lib.idException;
import neo.idlib.Text.Str.idStr;
import neo.idlib.containers.List.idList;
import neo.idlib.containers.StrPool;
import neo.idlib.math.Math_h.idMath;
import neo.idlib.math.Random.idRandom;
import neo.sys.sys_public.idPort;
import neo.sys.sys_public.netadr_t;
import neo.ui.UserInterface.idUserInterface;

/**
 *
 */
public class AsyncClient {

    static final int SETUP_CONNECTION_RESEND_TIME = 1000;
    static final int EMPTY_RESEND_TIME            = 500;
    static final int PREDICTION_FAST_ADJUST       = 4;
//

    /*
     ===============================================================================

     Network Client for asynchronous networking.

     ===============================================================================
     */
    enum clientState_t {

        CS_DISCONNECTED,
        CS_PURERESTART,
        CS_CHALLENGING,
        CS_CONNECTING,
        CS_CONNECTED,
        CS_INGAME
    };

    enum authKeyMsg_t {

        AUTHKEY_BADKEY,
        AUTHKEY_GUID
    };

    enum authBadKeyStatus_t {

        AUTHKEY_BAD_INVALID,
        AUTHKEY_BAD_BANNED,
        AUTHKEY_BAD_INUSE,
        AUTHKEY_BAD_MSG
    };

    enum clientUpdateState_t {

        UPDATE_NONE,
        UPDATE_SENT,
        UPDATE_READY,
        UPDATE_DLING,
        UPDATE_DONE
    };

    static class pakDlEntry_t {

        idStr url;
        idStr filename;
        int size;
        int checksum;
    };

    public static class idAsyncClient {

        public idServerScan serverList = new idServerScan();
        //
        //
        private boolean       active;                // true if client is active
        private int           realTime;              // absolute time
        //
        private int           clientTime;            // client local time
        private idPort        clientPort;            // UDP port
        private int           clientId;              // client identification
        private BigInteger    clientDataChecksum;    // checksum of the data used by the client
        private int           clientNum;             // client number on server
        private clientState_t clientState;           // client state
        private int           clientPrediction;      // how far the client predicts ahead
        private int           clientPredictTime;     // prediction time used to send user commands
        //
        private netadr_t      serverAddress;         // IP address of server
        private int           serverId;              // server identification
        private int           serverChallenge;       // challenge from server
        private int           serverMessageSequence; // sequence number of last server message
        //
        private netadr_t      lastRconAddress;       // last rcon address we emitted to
        private int           lastRconTime;          // when last rcon emitted
        //
        private idMsgChannel  channel;               // message channel to server
        private int           lastConnectTime;       // last time a connect message was sent
        private int           lastEmptyTime;         // last time an empty message was sent
        private int           lastPacketTime;        // last time a packet was received from the server
        private int           lastSnapshotTime;      // last time a snapshot was received
        //
        private int           snapshotSequence;      // sequence number of the last received snapshot
        private int           snapshotGameFrame;     // game frame number of the last received snapshot
        private int           snapshotGameTime;      // game time of the last received snapshot
        //
        private int           gameInitId;            // game initialization identification
        private int           gameFrame;             // local game frame
        private int           gameTime;              // local game time
        private int           gameTimeResidual;      // left over time from previous frame
        //
        private usercmd_t[][] userCmds = new usercmd_t[MAX_USERCMD_BACKUP][MAX_ASYNC_CLIENTS];
        //
        private idUserInterface     guiNetMenu;
        //
        private clientUpdateState_t updateState;
        private int                 updateSentTime;
        private idStr updateMSG = new idStr();
        private idStr updateURL = new idStr();
        private boolean updateDirectDownload;
        private idStr updateFile = new idStr();
        private dlMime_t updateMime;
        private idStr updateFallback = new idStr();
        private boolean showUpdateMessage;
        //
        private backgroundDownload_s backgroundDownload = new backgroundDownload_s();
        private int dltotal;
        private int dlnow;
        //
        private int lastFrameDelta;
        //
        private int dlRequest;                              // randomized number to keep track of the requests
        private int[] dlChecksums = new int[MAX_PURE_PAKS]; // 0-terminated, first element is the game pak checksum or 0
        private int dlCount;                                // total number of paks we request download for ( including the game pak )
        private idList<pakDlEntry_t> dlList = new idList<>();// list of paks to download, with url and name
        private int currentDlSize;
        private int totalDlSize;                            // for partial progress stuff
        //
        //

        public idAsyncClient() {
            guiNetMenu = null;
            updateState = UPDATE_NONE;
            channel = new idMsgChannel();
            Clear();

            this.clientPort = new idPort();
        }

        public void Shutdown() {
            guiNetMenu = null;
            updateMSG.Clear();
            updateURL.Clear();
            updateFile.Clear();
            updateFallback.Clear();
            backgroundDownload.url.url.Clear();
            dlList.Clear();
        }

        public boolean InitPort() {
            // if this is the first time we connect to a server, open the UDP port
            if (0 == clientPort.GetPort()) {
                if (!clientPort.InitForPort(PORT_ANY)) {
                    common.Printf("Couldn't open client network port.\n");
                    return false;
                }
            }
            // maintain it valid between connects and ui manager reloads
            guiNetMenu = uiManager.FindGui("guis/netmenu.gui", true, false, true);

            return true;
        }

        public void ClosePort() {
            clientPort.Close();
        }

        public void ConnectToServer(final netadr_t adr) {
            // shutdown any current game. that includes network disconnect
            session.Stop();

            if (!InitPort()) {
                return;
            }

            if (cvarSystem.GetCVarBool("net_serverDedicated")) {
                common.Printf("Can't connect to a server as dedicated\n");
                return;
            }

            // trash any currently pending packets
            ClearPendingPackets();

            serverAddress = adr;

            // clear the client state
            Clear();

            // get a pseudo random client id, but don't use the id which is reserved for connectionless packets
            clientId = Sys_Milliseconds() & CONNECTIONLESS_MESSAGE_ID_MASK;

            // calculate a checksum on some of the essential data used
            clientDataChecksum = declManager.GetChecksum();

            // start challenging the server
            clientState = CS_CHALLENGING;

            active = true;

            guiNetMenu = uiManager.FindGui("guis/netmenu.gui", true, false, true);
            guiNetMenu.SetStateString("status", va(common.GetLanguageDict().GetString("#str_06749"), Sys_NetAdrToString(adr)));
            session.SetGUI(guiNetMenu, HandleGuiCommand.getInstance());
        }

        public void ConnectToServer(final String address) throws idException {
            int serverNum;
            netadr_t adr = new netadr_t();

            if (idStr.IsNumeric(address)) {
                serverNum = Integer.parseInt(address);
                if (serverNum < 0 || serverNum >= serverList.Num()) {
                    session.MessageBox(MSG_OK, va(common.GetLanguageDict().GetString("#str_06733"), serverNum), common.GetLanguageDict().GetString("#str_06735"), true);
                    return;
                }
                adr = serverList.oGet(serverNum).adr;
            } else {
                if (!Sys_StringToNetAdr(address, adr, true)) {
                    session.MessageBox(MSG_OK, va(common.GetLanguageDict().GetString("#str_06734"), address), common.GetLanguageDict().GetString("#str_06735"), true);
                    return;
                }
            }
            if (0 == adr.port) {
                adr.port = PORT_SERVER;
            }

            common.Printf("\"%s\" resolved to %s\n", address, Sys_NetAdrToString(adr));

            ConnectToServer(adr);
        }

        public void Reconnect() {
            ConnectToServer(serverAddress);
        }

        public void DisconnectFromServer() {
            idBitMsg msg = new idBitMsg();
            ByteBuffer msgBuf = ByteBuffer.allocate(MAX_MESSAGE_SIZE);

            if (clientState.ordinal() >= CS_CONNECTED.ordinal()) {
                // if we were actually connected, clear the pure list
                fileSystem.ClearPureChecksums();

                // send reliable disconnect to server
                msg.Init(msgBuf, msgBuf.capacity());
                msg.WriteByte(CLIENT_RELIABLE_MESSAGE_DISCONNECT.ordinal());
                msg.WriteString("disconnect");

                if (!channel.SendReliableMessage(msg)) {
                    common.Error("client.server reliable messages overflow\n");
                }

                SendEmptyToServer(true);
                SendEmptyToServer(true);
                SendEmptyToServer(true);
            }

            if (clientState != CS_PURERESTART) {
                channel.Shutdown();
                clientState = CS_DISCONNECTED;
            }

            active = false;
        }

        public void GetServerInfo(final netadr_t adr) {
            idBitMsg msg = new idBitMsg();
            ByteBuffer msgBuf = ByteBuffer.allocate(MAX_MESSAGE_SIZE);

            if (!InitPort()) {
                return;
            }

            msg.Init(msgBuf, msgBuf.capacity());
            msg.WriteShort(CONNECTIONLESS_MESSAGE_ID);
            msg.WriteString("getInfo");
            msg.WriteLong(serverList.GetChallenge());    // challenge

            clientPort.SendPacket(adr, msg.GetData(), msg.GetSize());
        }

        public void GetServerInfo(final String address) {
            netadr_t adr = new netadr_t();

            if (address != null && !address.isEmpty()) {
                if (!Sys_StringToNetAdr(address, adr, true)) {
                    common.Printf("Couldn't get server address for \"%s\"\n", address);
                    return;
                }
            } else if (active) {
                adr = serverAddress;
            } else if (idAsyncNetwork.server.IsActive()) {
                // used to be a Sys_StringToNetAdr( "localhost", &adr, true ); and send a packet over loopback
                // but this breaks with net_ip ( typically, for multi-homed servers )
                idAsyncNetwork.server.PrintLocalServerInfo();
                return;
            } else {
                common.Printf("no server found\n");
                return;
            }

            if (0 == adr.port) {
                adr.port = PORT_SERVER;
            }

            GetServerInfo(adr);
        }

        public void GetLANServers() {
            int i;
            idBitMsg msg = new idBitMsg();
            ByteBuffer msgBuf = ByteBuffer.allocate(MAX_MESSAGE_SIZE);
            netadr_t broadcastAddress = new netadr_t();

            if (!InitPort()) {
                return;
            }

            idAsyncNetwork.LANServer.SetBool(true);

            serverList.SetupLANScan();

            msg.Init(msgBuf, msgBuf.capacity());
            msg.WriteShort(CONNECTIONLESS_MESSAGE_ID);
            msg.WriteString("getInfo");
            msg.WriteLong(serverList.GetChallenge());

            broadcastAddress.type = NA_BROADCAST;
            for (i = 0; i < MAX_SERVER_PORTS; i++) {
                broadcastAddress.port = (short) (PORT_SERVER + i);
                clientPort.SendPacket(broadcastAddress, msg.GetData(), msg.GetSize());
            }
        }

        public void GetNETServers() {
            idBitMsg msg = new idBitMsg();
            ByteBuffer msgBuf = ByteBuffer.allocate(MAX_MESSAGE_SIZE);

            idAsyncNetwork.LANServer.SetBool(false);

            // NetScan only clears GUI and results, not the stored list
            serverList.Clear();
            serverList.NetScan();
            serverList.StartServers(true);

            msg.Init(msgBuf, msgBuf.capacity());
            msg.WriteShort(CONNECTIONLESS_MESSAGE_ID);
            msg.WriteString("getServers");
            msg.WriteLong(ASYNC_PROTOCOL_VERSION);
            msg.WriteString(cvarSystem.GetCVarString("fs_game"));
            msg.WriteBits(cvarSystem.GetCVarInteger("gui_filter_password"), 2);
            msg.WriteBits(cvarSystem.GetCVarInteger("gui_filter_players"), 2);
            msg.WriteBits(cvarSystem.GetCVarInteger("gui_filter_gameType"), 2);

            netadr_t adr = new netadr_t();
            if (idAsyncNetwork.GetMasterAddress(0, adr)) {
                clientPort.SendPacket(adr, msg.GetData(), msg.GetSize());
            }
        }

        public void ListServers() {
            int i;

            for (i = 0; i < serverList.Num(); i++) {
                common.Printf("%3d: %s %dms (%s)\n", i, serverList.oGet(i).serverInfo.GetString("si_name"), serverList.oGet(i).ping, Sys_NetAdrToString(serverList.oGet(i).adr));
            }
        }

        public void ClearServers() {
            serverList.Clear();
        }

        public void RemoteConsole(final String command) {
            netadr_t adr = new netadr_t();
            idBitMsg msg = new idBitMsg();
            ByteBuffer msgBuf = ByteBuffer.allocate(MAX_MESSAGE_SIZE);

            if (!InitPort()) {
                return;
            }

            if (active) {
                adr = serverAddress;
            } else {
                Sys_StringToNetAdr(idAsyncNetwork.clientRemoteConsoleAddress.GetString(), adr, true);
            }

            if (0 == adr.port) {
                adr.port = PORT_SERVER;
            }

            lastRconAddress = adr;
            lastRconTime = realTime;

            msg.Init(msgBuf, msgBuf.capacity());
            msg.WriteShort(CONNECTIONLESS_MESSAGE_ID);
            msg.WriteString("rcon");
            msg.WriteString(idAsyncNetwork.clientRemoteConsolePassword.GetString());
            msg.WriteString(command);

            clientPort.SendPacket(adr, msg.GetData(), msg.GetSize());
        }

        public boolean IsPortInitialized() {
            return clientPort.GetPort() != 0;
        }

        public boolean IsActive() {
            return active;
        }

        public int GetLocalClientNum() {
            return clientNum;
        }

        public int GetPrediction() {
            if (clientState.ordinal() < CS_CONNECTED.ordinal()) {
                return -1;
            } else {
                return clientPrediction;
            }
        }

        public int GetTimeSinceLastPacket() {
            if (clientState.ordinal() < CS_CONNECTED.ordinal()) {
                return -1;
            } else {
                return clientTime - lastPacketTime;
            }
        }

        public int GetOutgoingRate() {
            if (clientState.ordinal() < CS_CONNECTED.ordinal()) {
                return -1;
            } else {
                return channel.GetOutgoingRate();
            }
        }

        public int GetIncomingRate() {
            if (clientState.ordinal() < CS_CONNECTED.ordinal()) {
                return -1;
            } else {
                return channel.GetIncomingRate();
            }
        }

        public float GetOutgoingCompression() {
            if (clientState.ordinal() < CS_CONNECTED.ordinal()) {
                return 0.0f;
            } else {
                return channel.GetOutgoingCompression();
            }
        }

        public float GetIncomingCompression() {
            if (clientState.ordinal() < CS_CONNECTED.ordinal()) {
                return 0.0f;
            } else {
                return channel.GetIncomingCompression();
            }
        }

        public float GetIncomingPacketLoss() {
            if (clientState.ordinal() < CS_CONNECTED.ordinal()) {
                return 0.0f;
            } else {
                return channel.GetIncomingPacketLoss();
            }
        }

        public int GetPredictedFrames() {
            return lastFrameDelta;
        }
//

        public void RunFrame() {
            int msec;
            int[] size = new int[1];
            boolean newPacket;
            idBitMsg msg = new idBitMsg();
            ByteBuffer msgBuf = ByteBuffer.allocate(MAX_MESSAGE_SIZE);
            netadr_t[] from = {null};

            msec = UpdateTime(100);

            if (0 == clientPort.GetPort()) {
                return;
            }

            // handle ongoing pk4 downloads and patch downloads
            HandleDownloads();

            gameTimeResidual += msec;

            // spin in place processing incoming packets until enough time lapsed to run a new game frame
            do {

                do {

                    // blocking read with game time residual timeout
                    newPacket = clientPort.GetPacketBlocking(from, msgBuf, size, msgBuf.capacity(), USERCMD_MSEC - (gameTimeResidual + clientPredictTime) - 1);
                    if (newPacket) {
                        msg.Init(msgBuf, msgBuf.capacity());
                        msg.SetSize(size[0]);
                        msg.BeginReading();
                        ProcessMessage(from[0], msg);
                    }

                    msec = UpdateTime(100);
                    gameTimeResidual += msec;

                } while (newPacket);

            } while (gameTimeResidual + clientPredictTime < USERCMD_MSEC);

            // update server list
            serverList.RunFrame();

            if (clientState == CS_DISCONNECTED) {
                usercmdGen.GetDirectUsercmd();
                gameTimeResidual = USERCMD_MSEC - 1;
                clientPredictTime = 0;
                return;
            }

            if (clientState == CS_PURERESTART) {
                clientState = CS_DISCONNECTED;
                Reconnect();
                gameTimeResidual = USERCMD_MSEC - 1;
                clientPredictTime = 0;
                return;
            }

            // if not connected setup a connection
            if (clientState.ordinal() < CS_CONNECTED.ordinal()) {
                // also need to read mouse for the connecting guis
                usercmdGen.GetDirectUsercmd();
                SetupConnection();
                gameTimeResidual = USERCMD_MSEC - 1;
                clientPredictTime = 0;
                return;
            }

            if (CheckTimeout()) {
                return;
            }

            // if not yet in the game send empty messages to keep data flowing through the channel
            if (clientState.ordinal() < CS_INGAME.ordinal()) {
                Idle();
                gameTimeResidual = 0;
                return;
            }

            // check for user info changes
            if ((cvarSystem.GetModifiedFlags() & CVAR_USERINFO) != 0) {
                game.ThrottleUserInfo();
                SendUserInfoToServer();
                game.SetUserInfo(clientNum, sessLocal.mapSpawnData.userInfo[clientNum], true, false);
                cvarSystem.ClearModifiedFlags(CVAR_USERINFO);
            }

            if (gameTimeResidual + clientPredictTime >= USERCMD_MSEC) {
                lastFrameDelta = 0;
            }

            // generate user commands for the predicted time
            while (gameTimeResidual + clientPredictTime >= USERCMD_MSEC) {

                // send the user commands of this client to the server
                SendUsercmdsToServer();

                // update time
                gameFrame++;
                gameTime += USERCMD_MSEC;
                gameTimeResidual -= USERCMD_MSEC;

                // run from the snapshot up to the local game frame
                while (snapshotGameFrame < gameFrame) {

                    lastFrameDelta++;

                    // duplicate usercmds for clients if no new ones are available
                    DuplicateUsercmds(snapshotGameFrame, snapshotGameTime);

                    // indicate the last prediction frame before a render
                    boolean lastPredictFrame = (snapshotGameFrame + 1 >= gameFrame && gameTimeResidual + clientPredictTime < USERCMD_MSEC);

                    // run client prediction
                    gameReturn_t ret = game.ClientPrediction(clientNum, userCmds[snapshotGameFrame & (MAX_USERCMD_BACKUP - 1)], lastPredictFrame);

                    idAsyncNetwork.ExecuteSessionCommand(ret.sessionCommand);

                    snapshotGameFrame++;
                    snapshotGameTime += USERCMD_MSEC;
                }
            }
        }

        public void SendReliableGameMessage(final idBitMsg msg) {
            idBitMsg outMsg = new idBitMsg();
            ByteBuffer msgBuf = ByteBuffer.allocate(MAX_MESSAGE_SIZE);

            if (clientState.ordinal() < CS_INGAME.ordinal()) {
                return;
            }

            outMsg.Init(msgBuf, msgBuf.capacity());
            outMsg.WriteByte(CLIENT_RELIABLE_MESSAGE_GAME.ordinal());
            outMsg.WriteData(msg.GetData(), msg.GetSize());
            if (!channel.SendReliableMessage(outMsg)) {
                common.Error("client->server reliable messages overflow\n");
            }
        }
//

        public void SendVersionCheck() {
            SendVersionCheck(false);
        }

        public void SendVersionCheck(boolean fromMenu /*= false */) {
            idBitMsg msg = new idBitMsg();
            ByteBuffer msgBuf = ByteBuffer.allocate(MAX_MESSAGE_SIZE);

            if (updateState != UPDATE_NONE && !fromMenu) {
                common.DPrintf("up-to-date check was already performed\n");
                return;
            }

            InitPort();
            msg.Init(msgBuf, msgBuf.capacity());
            msg.WriteShort(CONNECTIONLESS_MESSAGE_ID);
            msg.WriteString("versionCheck");
            msg.WriteLong(ASYNC_PROTOCOL_VERSION);
            msg.WriteShort(BUILD_OS_ID);
            msg.WriteString(cvarSystem.GetCVarString("si_version"));
            msg.WriteString(cvarSystem.GetCVarString("com_guid"));
            clientPort.SendPacket(idAsyncNetwork.GetMasterAddress(), msg.GetData(), msg.GetSize());

            common.DPrintf("sent a version check request\n");

            updateState = UPDATE_SENT;
            updateSentTime = clientTime;
            showUpdateMessage = fromMenu;
        }

        // pass NULL for the keys you don't care to auth for
        // returns false if internet link doesn't appear to be available
        public boolean SendAuthCheck(final String cdkey, final String xpkey) {
            idBitMsg msg = new idBitMsg();
            ByteBuffer msgBuf = ByteBuffer.allocate(MAX_MESSAGE_SIZE);

            msg.Init(msgBuf, msgBuf.capacity());
            msg.WriteShort(CONNECTIONLESS_MESSAGE_ID);
            msg.WriteString("gameAuth");
            msg.WriteLong(ASYNC_PROTOCOL_VERSION);
            msg.WriteByte(cdkey != null ? 1 : 0);
            msg.WriteString(cdkey != null ? cdkey : "");
            msg.WriteByte(xpkey != null ? 1 : 0);
            msg.WriteString(xpkey != null ? xpkey : "");
            InitPort();
            clientPort.SendPacket(idAsyncNetwork.GetMasterAddress(), msg.GetData(), msg.GetSize());
            return true;
        }
//

        public void PacifierUpdate() {
            if (!IsActive()) {
                return;
            }
            realTime = Sys_Milliseconds();
            SendEmptyToServer(false, true);
        }

        private void Clear() {
            int i, j;

            active = false;
            realTime = 0;
            clientTime = 0;
            clientId = 0;
            clientDataChecksum = BigInteger.ZERO;
            clientNum = 0;
            clientState = CS_DISCONNECTED;
            clientPrediction = 0;
            clientPredictTime = 0;
            serverId = 0;
            serverChallenge = 0;
            serverMessageSequence = 0;
            lastConnectTime = -9999;
            lastEmptyTime = -9999;
            lastPacketTime = -9999;
            lastSnapshotTime = -9999;
            snapshotGameFrame = 0;
            snapshotGameTime = 0;
            snapshotSequence = 0;
            gameInitId = GAME_INIT_ID_INVALID;
            gameFrame = 0;
            gameTimeResidual = 0;
            gameTime = 0;
//	memset( userCmds, 0, sizeof( userCmds ) );
            for (i = 0; i < MAX_USERCMD_BACKUP; i++) {
                for (j = 0; j < MAX_ASYNC_CLIENTS; j++) {
                    userCmds[i][j] = new usercmd_t();
                }
            }
            backgroundDownload.completed = true;
            lastRconTime = 0;
            showUpdateMessage = false;
            lastFrameDelta = 0;

            dlRequest = -1;
            dlCount = -1;
//	memset( dlChecksums, 0, sizeof( int ) * MAX_PURE_PAKS );
            Arrays.fill(dlChecksums, 0);
            currentDlSize = 0;
            totalDlSize = 0;
        }

        private void ClearPendingPackets() {
            int[] size = new int[1];
            ByteBuffer msgBuf = ByteBuffer.allocate(MAX_MESSAGE_SIZE);
            netadr_t[] from = new netadr_t[1];

            while (clientPort.GetPacket(from, msgBuf, size, msgBuf.capacity()));
        }

        private void DuplicateUsercmds(int frame, int time) {
            int i, previousIndex, currentIndex;

            previousIndex = (frame - 1) & (MAX_USERCMD_BACKUP - 1);
            currentIndex = frame & (MAX_USERCMD_BACKUP - 1);

            // duplicate previous user commands if no new commands are available for a client
            for (i = 0; i < MAX_ASYNC_CLIENTS; i++) {
                idAsyncNetwork.DuplicateUsercmd(userCmds[previousIndex][i], userCmds[currentIndex][i], frame, time);
            }
        }

        private void SendUserInfoToServer() {
            idBitMsg msg = new idBitMsg();
            ByteBuffer msgBuf = ByteBuffer.allocate(MAX_MESSAGE_SIZE);
            idDict info;

            if (clientState.ordinal() < CS_CONNECTED.ordinal()) {
                return;
            }

            info = cvarSystem.MoveCVarsToDict(CVAR_USERINFO);

            // send reliable client info to server
            msg.Init(msgBuf, msgBuf.capacity());
            msg.WriteByte(CLIENT_RELIABLE_MESSAGE_CLIENTINFO.ordinal());
            msg.WriteDeltaDict(info, sessLocal.mapSpawnData.userInfo[clientNum]);

            if (!channel.SendReliableMessage(msg)) {
                common.Error("client.server reliable messages overflow\n");
            }

            sessLocal.mapSpawnData.userInfo[clientNum] = info;
        }

        private void SendEmptyToServer() {
            SendEmptyToServer(false);
        }

        private void SendEmptyToServer(boolean force) {
            SendEmptyToServer(force, false);
        }

        private void SendEmptyToServer(boolean force/* = false*/, boolean mapLoad /*= false*/) {
            idBitMsg msg = new idBitMsg();
            ByteBuffer msgBuf = ByteBuffer.allocate(MAX_MESSAGE_SIZE);

            if (lastEmptyTime > realTime) {
                lastEmptyTime = realTime;
            }

            if (!force && (realTime - lastEmptyTime < EMPTY_RESEND_TIME)) {
                return;
            }

            if (idAsyncNetwork.verbose.GetInteger() != 0) {
                common.Printf("sending empty to server, gameInitId = %d\n", mapLoad ? GAME_INIT_ID_MAP_LOAD : gameInitId);
            }

            msg.Init(msgBuf, msgBuf.capacity());
            msg.WriteLong(serverMessageSequence);
            msg.WriteLong(mapLoad ? GAME_INIT_ID_MAP_LOAD : gameInitId);
            msg.WriteLong(snapshotSequence);
            msg.WriteByte(CLIENT_UNRELIABLE_MESSAGE_EMPTY.ordinal());

            channel.SendMessage(clientPort, clientTime, msg);

            while (channel.UnsentFragmentsLeft()) {
                channel.SendNextFragment(clientPort, clientTime);
            }

            lastEmptyTime = realTime;
        }

        private void SendPingResponseToServer(int time) {
            idBitMsg msg = new idBitMsg();
            ByteBuffer msgBuf = ByteBuffer.allocate(MAX_MESSAGE_SIZE);

            if (idAsyncNetwork.verbose.GetInteger() == 2) {
                common.Printf("sending ping response to server, gameInitId = %d\n", gameInitId);
            }

            msg.Init(msgBuf, msgBuf.capacity());
            msg.WriteLong(serverMessageSequence);
            msg.WriteLong(gameInitId);
            msg.WriteLong(snapshotSequence);
            msg.WriteByte(CLIENT_UNRELIABLE_MESSAGE_PINGRESPONSE.ordinal());
            msg.WriteLong(time);

            channel.SendMessage(clientPort, clientTime, msg);
            while (channel.UnsentFragmentsLeft()) {
                channel.SendNextFragment(clientPort, clientTime);
            }
        }

        private void SendUsercmdsToServer() {
            int i, numUsercmds, index;
            idBitMsg msg = new idBitMsg();
            ByteBuffer msgBuf = ByteBuffer.allocate(MAX_MESSAGE_SIZE);
            usercmd_t last;

            if (idAsyncNetwork.verbose.GetInteger() == 2) {
                common.Printf("sending usercmd to server: gameInitId = %d, gameFrame = %d, gameTime = %d\n", gameInitId, gameFrame, gameTime);
            }

            // generate user command for this client
            index = gameFrame & (MAX_USERCMD_BACKUP - 1);
            userCmds[index][clientNum] = usercmdGen.GetDirectUsercmd();
            userCmds[index][clientNum].gameFrame = gameFrame;
            userCmds[index][clientNum].gameTime = gameTime;

            // send the user commands to the server
            msg.Init(msgBuf, msgBuf.capacity());
            msg.WriteLong(serverMessageSequence);
            msg.WriteLong(gameInitId);
            msg.WriteLong(snapshotSequence);
            msg.WriteByte(CLIENT_UNRELIABLE_MESSAGE_USERCMD.ordinal());
            msg.WriteShort(clientPrediction);

            numUsercmds = idMath.ClampInt(0, 10, idAsyncNetwork.clientUsercmdBackup.GetInteger()) + 1;

            // write the user commands
            msg.WriteLong(gameFrame);
            msg.WriteByte(numUsercmds);
            for (last = null, i = gameFrame - numUsercmds + 1; i <= gameFrame; i++) {
                index = i & (MAX_USERCMD_BACKUP - 1);
                idAsyncNetwork.WriteUserCmdDelta(msg, userCmds[index][clientNum], last);
                last = userCmds[index][clientNum];
            }

            channel.SendMessage(clientPort, clientTime, msg);
            while (channel.UnsentFragmentsLeft()) {
                channel.SendNextFragment(clientPort, clientTime);
            }
        }

        private void InitGame(int serverGameInitId, int serverGameFrame, int serverGameTime, final idDict serverSI) {
            gameInitId = serverGameInitId;
            gameFrame = snapshotGameFrame = serverGameFrame;
            gameTime = snapshotGameTime = serverGameTime;
            gameTimeResidual = 0;
//	memset( userCmds, 0, sizeof( userCmds ) );
            Arrays.fill(userCmds, 0);

            for (int i = 0; i < MAX_ASYNC_CLIENTS; i++) {
                sessLocal.mapSpawnData.userInfo[i].Clear();
            }

            sessLocal.mapSpawnData.serverInfo = serverSI;
        }

        private void ProcessUnreliableServerMessage(final idBitMsg msg) throws idException {
            int i, j, index, numDuplicatedUsercmds, aheadOfServer, numUsercmds, delta;
            int serverGameInitId, serverGameFrame, serverGameTime;
            idDict serverSI = new idDict();
            usercmd_t last;
            boolean pureWait;

            serverGameInitId = msg.ReadLong();

            if (msg.ReadByte() < SERVER_UNRELIABLE.values().length) {
                SERVER_UNRELIABLE id = SERVER_UNRELIABLE.values()[msg.ReadByte()];
                switch (id) {
                    case SERVER_UNRELIABLE_MESSAGE_EMPTY: {
                        if (idAsyncNetwork.verbose.GetInteger() != 0) {
                            common.Printf("received empty message from server\n");
                        }
                        break;
                    }
                    case SERVER_UNRELIABLE_MESSAGE_PING: {
                        if (idAsyncNetwork.verbose.GetInteger() == 2) {
                            common.Printf("received ping message from server\n");
                        }
                        SendPingResponseToServer(msg.ReadLong());
                        break;
                    }
                    case SERVER_UNRELIABLE_MESSAGE_GAMEINIT: {
                        serverGameFrame = msg.ReadLong();
                        serverGameTime = msg.ReadLong();
                        msg.ReadDeltaDict(serverSI, null);
                        pureWait = serverSI.GetBool("si_pure");

                        InitGame(serverGameInitId, serverGameFrame, serverGameTime, serverSI);

                        channel.ResetRate();

                        if (idAsyncNetwork.verbose.GetInteger() != 0) {
                            common.Printf("received gameinit, gameInitId = %d, gameFrame = %d, gameTime = %d\n", gameInitId, gameFrame, gameTime);
                        }

                        // mute sound
                        soundSystem.SetMute(true);

                        // ensure chat icon goes away when the GUI is changed...
                        //cvarSystem.SetCVarBool( "ui_chat", false );
                        if (pureWait) {
                            guiNetMenu = uiManager.FindGui("guis/netmenu.gui", true, false, true);
                            session.SetGUI(guiNetMenu, HandleGuiCommand.getInstance());
                            session.MessageBox(MSG_ABORT, common.GetLanguageDict().GetString("#str_04317"), common.GetLanguageDict().GetString("#str_04318"), false, "pure_abort");
                        } else {
                            // load map
                            session.SetGUI(null, null);
                            sessLocal.ExecuteMapChange();
                        }

                        break;
                    }
                    case SERVER_UNRELIABLE_MESSAGE_SNAPSHOT: {
                        // if the snapshot is from a different game
                        if (serverGameInitId != gameInitId) {
                            if (idAsyncNetwork.verbose.GetInteger() != 0) {
                                common.Printf("ignoring snapshot with != gameInitId\n");
                            }
                            break;
                        }

                        snapshotSequence = msg.ReadLong();
                        snapshotGameFrame = msg.ReadLong();
                        snapshotGameTime = msg.ReadLong();
                        numDuplicatedUsercmds = msg.ReadByte();
                        aheadOfServer = msg.ReadShort();

                        // read the game snapshot
                        game.ClientReadSnapshot(clientNum, snapshotSequence, snapshotGameFrame, snapshotGameTime, numDuplicatedUsercmds, aheadOfServer, msg);

                        // read user commands of other clients from the snapshot
                        for (last = null, i = msg.ReadByte(); i < MAX_ASYNC_CLIENTS; i = msg.ReadByte()) {
                            numUsercmds = msg.ReadByte();
                            if (numUsercmds > MAX_USERCMD_RELAY) {
                                common.Error("snapshot %d contains too many user commands for client %d", snapshotSequence, i);
                                break;
                            }
                            for (j = 0; j < numUsercmds; j++) {
                                index = (snapshotGameFrame + j) & (MAX_USERCMD_BACKUP - 1);
                                idAsyncNetwork.ReadUserCmdDelta(msg, userCmds[index][i], last);
                                userCmds[index][i].gameFrame = snapshotGameFrame + j;
                                userCmds[index][i].duplicateCount = 0;
                                last = userCmds[index][i];
                            }
                            // clear all user commands after the ones just read from the snapshot
                            for (j = numUsercmds; j < MAX_USERCMD_BACKUP; j++) {
                                index = (snapshotGameFrame + j) & (MAX_USERCMD_BACKUP - 1);
                                userCmds[index][i].gameFrame = 0;
                                userCmds[index][i].gameTime = 0;
                            }
                        }

                        // if this is the first snapshot after a game init was received
                        if (clientState == CS_CONNECTED) {
                            gameTimeResidual = 0;
                            clientState = CS_INGAME;
                            assert (NOT(sessLocal.GetActiveMenu()));
                            if (idAsyncNetwork.verbose.GetInteger() != 0) {
                                common.Printf("received first snapshot, gameInitId = %d, gameFrame %d gameTime %d\n", gameInitId, snapshotGameFrame, snapshotGameTime);
                            }
                        }

                        // if the snapshot is newer than the clients current game time
                        if (gameTime < snapshotGameTime || gameTime > snapshotGameTime + idAsyncNetwork.clientMaxPrediction.GetInteger()) {
                            gameFrame = snapshotGameFrame;
                            gameTime = snapshotGameTime;
                            gameTimeResidual = idMath.ClampInt(-idAsyncNetwork.clientMaxPrediction.GetInteger(), idAsyncNetwork.clientMaxPrediction.GetInteger(), gameTimeResidual);
                            clientPredictTime = idMath.ClampInt(-idAsyncNetwork.clientMaxPrediction.GetInteger(), idAsyncNetwork.clientMaxPrediction.GetInteger(), clientPredictTime);
                        }

                        // adjust the client prediction time based on the snapshot time
                        clientPrediction -= (1 - (INTSIGNBITSET(aheadOfServer - idAsyncNetwork.clientPrediction.GetInteger()) << 1));
                        clientPrediction = idMath.ClampInt(idAsyncNetwork.clientPrediction.GetInteger(), idAsyncNetwork.clientMaxPrediction.GetInteger(), clientPrediction);
                        delta = gameTime - (snapshotGameTime + clientPrediction);
                        clientPredictTime -= (delta / PREDICTION_FAST_ADJUST) + (1 - (INTSIGNBITSET(delta) << 1));

                        lastSnapshotTime = clientTime;

                        if (idAsyncNetwork.verbose.GetInteger() == 2) {
                            common.Printf("received snapshot, gameInitId = %d, gameFrame = %d, gameTime = %d\n", gameInitId, gameFrame, gameTime);
                        }

                        if (numDuplicatedUsercmds != 0 && (idAsyncNetwork.verbose.GetInteger() == 2)) {
                            common.Printf("server duplicated %d user commands before snapshot %d\n", numDuplicatedUsercmds, snapshotGameFrame);
                        }
                        break;
                    }
                }
            } else {
//		default: {
                common.Printf("unknown unreliable server message %d\n", msg.ReadByte());
//			break;
            }
        }

        private void ProcessReliableServerMessages() throws idException {
            idBitMsg msg = new idBitMsg();
            ByteBuffer msgBuf = ByteBuffer.allocate(MAX_MESSAGE_SIZE);
            SERVER_RELIABLE id;

            msg.Init(msgBuf, msgBuf.capacity());

            while (channel.GetReliableMessage(msg)) {
                if (msg.ReadByte() < SERVER_RELIABLE.values().length) {
                    id = SERVER_RELIABLE.values()[msg.ReadByte()];
                    switch (id) {
                        case SERVER_RELIABLE_MESSAGE_CLIENTINFO: {
                            int clientNum;
                            clientNum = msg.ReadByte();

                            idDict info = sessLocal.mapSpawnData.userInfo[clientNum];
                            boolean haveBase = (msg.ReadBits(1) != 0);

                            if (ID_CLIENTINFO_TAGS) {
                                long checksum = info.Checksum();
                                int srv_checksum = msg.ReadLong();
                                if (checksum != srv_checksum) {
                                    common.DPrintf("SERVER_RELIABLE_MESSAGE_CLIENTINFO %d (haveBase: %s): != checksums srv: 0x%x local: 0x%x\n", clientNum, haveBase ? "true" : "false", checksum, srv_checksum);
                                    info.Print();
                                } else {
                                    common.DPrintf("SERVER_RELIABLE_MESSAGE_CLIENTINFO %d (haveBase: %s): checksums ok 0x%x\n", clientNum, haveBase ? "true" : "false", checksum);
                                }
                            }

                            if (haveBase) {
                                msg.ReadDeltaDict(info, info);
                            } else {
                                msg.ReadDeltaDict(info, null);
                            }

                            // server forces us to a different userinfo
                            if (clientNum == this.clientNum) {
                                common.DPrintf("local user info modified by server\n");
                                cvarSystem.SetCVarsFromDict(info);
                                cvarSystem.ClearModifiedFlags(CVAR_USERINFO); // don't emit back
                            }
                            game.SetUserInfo(clientNum, info, true, false);
                            break;
                        }
                        case SERVER_RELIABLE_MESSAGE_SYNCEDCVARS: {
                            idDict info = sessLocal.mapSpawnData.syncedCVars;
                            msg.ReadDeltaDict(info, info);
                            cvarSystem.SetCVarsFromDict(info);
                            if (!idAsyncNetwork.allowCheats.GetBool()) {
                                cvarSystem.ResetFlaggedVariables(CVAR_CHEAT);
                            }
                            break;
                        }
                        case SERVER_RELIABLE_MESSAGE_PRINT: {
                            char[] string = new char[MAX_STRING_CHARS];
                            msg.ReadString(string, MAX_STRING_CHARS);
                            common.Printf("%s\n", string);
                            break;
                        }
                        case SERVER_RELIABLE_MESSAGE_DISCONNECT: {
                            int clientNum;
                            char[] string = new char[MAX_STRING_CHARS];
                            clientNum = msg.ReadLong();
                            ReadLocalizedServerString(msg, string, MAX_STRING_CHARS);
                            if (clientNum == this.clientNum) {
                                session.Stop();
                                session.MessageBox(MSG_OK, ctos(string), common.GetLanguageDict().GetString("#str_04319"), true);
                                session.StartMenu();
                            } else {
                                common.Printf("client %d %s\n", clientNum, string);
                                cmdSystem.BufferCommandText(CMD_EXEC_NOW, va("addChatLine \"%s^0 %s\"", sessLocal.mapSpawnData.userInfo[clientNum].GetString("ui_name"), string));
                                sessLocal.mapSpawnData.userInfo[clientNum].Clear();
                            }
                            break;
                        }
                        case SERVER_RELIABLE_MESSAGE_APPLYSNAPSHOT: {
                            int sequence;
                            sequence = msg.ReadLong();
                            if (!game.ClientApplySnapshot(clientNum, sequence)) {
                                session.Stop();
                                common.Error("couldn't apply snapshot %d", sequence);
                            }
                            break;
                        }
                        case SERVER_RELIABLE_MESSAGE_PURE: {
                            ProcessReliableMessagePure(msg);
                            break;
                        }
                        case SERVER_RELIABLE_MESSAGE_RELOAD: {
                            if (idAsyncNetwork.verbose.GetBool()) {
                                common.Printf("got MESSAGE_RELOAD from server\n");
                            }
                            // simply reconnect, so that if the server restarts in pure mode we can get the right list and avoid spurious reloads
                            cmdSystem.BufferCommandText(CMD_EXEC_APPEND, "reconnect\n");
                            break;
                        }
                        case SERVER_RELIABLE_MESSAGE_ENTERGAME: {
                            SendUserInfoToServer();
                            game.SetUserInfo(clientNum, sessLocal.mapSpawnData.userInfo[clientNum], true, false);
                            cvarSystem.ClearModifiedFlags(CVAR_USERINFO);
                            break;
                        }
					default:
						// TODO check unused Enum case labels
						break;
                    }
                } else {
//			default: {
                    // pass reliable message on to game code
                    game.ClientProcessReliableMessage(clientNum, msg);
//				break;
//			}
                }
            }
        }

        private void ProcessChallengeResponseMessage(final netadr_t from, final idBitMsg msg) throws idException {
            char[] serverGame = new char[MAX_STRING_CHARS], serverGameBase = new char[MAX_STRING_CHARS];
            String serverGameStr, serverGameBaseStr;

            if (clientState != CS_CHALLENGING) {
                common.Printf("Unwanted challenge response received.\n");
                return;
            }

            serverChallenge = msg.ReadLong();
            serverId = msg.ReadShort();
            msg.ReadString(serverGameBase, MAX_STRING_CHARS);
            msg.ReadString(serverGame, MAX_STRING_CHARS);
            serverGameStr = ctos(serverGame);
            serverGameBaseStr = ctos(serverGameBase);

            // the server is running a different game... we need to reload in the correct fs_game
            // even pure pak checks would fail if we didn't, as there are files we may not even see atm
            // NOTE: we could read the pure list from the server at the same time and set it up for the restart
            // ( if the client can restart directly with the right pak order, then we avoid an extra reloadEngine later.. )
            if (idStr.Icmp(cvarSystem.GetCVarString("fs_game_base"), serverGameBaseStr) != 0
                    || idStr.Icmp(cvarSystem.GetCVarString("fs_game"), serverGameStr) != 0) {
                // bug #189 - if the server is running ROE and ROE is not locally installed, refuse to connect or we might crash
                if (!fileSystem.HasD3XP() && (0 == idStr.Icmp(serverGameBaseStr, "d3xp") || 0 == idStr.Icmp(serverGameStr, "d3xp"))) {
                    common.Printf("The server is running Doom3: Resurrection of Evil expansion pack. RoE is not installed on this client. Aborting the connection..\n");
                    cmdSystem.BufferCommandText(CMD_EXEC_APPEND, "disconnect\n");
                    return;
                }
                common.Printf("The server is running a different mod (%s-%s). Restarting..\n", serverGameBaseStr, serverGameStr);
                cvarSystem.SetCVarString("fs_game_base", serverGameBaseStr);
                cvarSystem.SetCVarString("fs_game", serverGameStr);
                cmdSystem.BufferCommandText(CMD_EXEC_NOW, "reloadEngine");
                cmdSystem.BufferCommandText(CMD_EXEC_APPEND, "reconnect\n");
                return;
            }

            common.Printf("received challenge response 0x%x from %s\n", serverChallenge, Sys_NetAdrToString(from));

            // start sending connect packets instead of challenge request packets
            clientState = CS_CONNECTING;
            lastConnectTime = -9999;

            // take this address as the new server address.  This allows
            // a server proxy to hand off connections to multiple servers
            serverAddress = from;
        }

        private void ProcessConnectResponseMessage(final netadr_t from, final idBitMsg msg) {
            int serverGameInitId, serverGameFrame, serverGameTime;
            idDict serverSI = new idDict();

            if (clientState.ordinal() >= CS_CONNECTED.ordinal()) {
                common.Printf("Duplicate connect received.\n");
                return;
            }
            if (clientState != CS_CONNECTING) {
                common.Printf("Connect response packet while not connecting.\n");
                return;
            }
            if (!Sys_CompareNetAdrBase(from, serverAddress)) {
                common.Printf("Connect response from a different server.\n");
                common.Printf("%s should have been %s\n", Sys_NetAdrToString(from), Sys_NetAdrToString(serverAddress));
                return;
            }

            common.Printf("received connect response from %s\n", Sys_NetAdrToString(from));

            channel.Init(from, clientId);
            clientNum = msg.ReadLong();
            clientState = CS_CONNECTED;
            lastPacketTime = -9999;

            serverGameInitId = msg.ReadLong();
            serverGameFrame = msg.ReadLong();
            serverGameTime = msg.ReadLong();
            msg.ReadDeltaDict(serverSI, null);

            InitGame(serverGameInitId, serverGameFrame, serverGameTime, serverSI);

            // load map
            session.SetGUI(null, null);
            sessLocal.ExecuteMapChange();

            clientPredictTime = clientPrediction = idMath.ClampInt(0, idAsyncNetwork.clientMaxPrediction.GetInteger(), clientTime - lastConnectTime);
        }

        private void ProcessDisconnectMessage(final netadr_t from, final idBitMsg msg) {
            if (clientState == CS_DISCONNECTED) {
                common.Printf("Disconnect packet while not connected.\n");
                return;
            }
            if (!Sys_CompareNetAdrBase(from, serverAddress)) {
                common.Printf("Disconnect packet from unknown server.\n");
                return;
            }
            session.Stop();
            session.MessageBox(MSG_OK, common.GetLanguageDict().GetString("#str_04320"), null, true);
            session.StartMenu();
        }

        private void ProcessInfoResponseMessage(final netadr_t from, final idBitMsg msg) throws idException {
            int i, protocol, index;
            networkServer_t serverInfo = new networkServer_t();
            boolean verbose = false;

            if (from.type == NA_LOOPBACK || cvarSystem.GetCVarBool("developer")) {
                verbose = true;
            }

            serverInfo.clients = 0;
            serverInfo.adr = from;
            serverInfo.challenge = msg.ReadLong();			// challenge
            protocol = msg.ReadLong();
            if (protocol != ASYNC_PROTOCOL_VERSION) {
                common.Printf("server %s ignored - protocol %d.%d, expected %d.%d\n", Sys_NetAdrToString(serverInfo.adr), protocol >> 16, protocol & 0xffff, ASYNC_PROTOCOL_MAJOR, ASYNC_PROTOCOL_MINOR);
                return;
            }
            msg.ReadDeltaDict(serverInfo.serverInfo, null);

            if (verbose) {
                common.Printf("server IP = %s\n", Sys_NetAdrToString(serverInfo.adr));
                serverInfo.serverInfo.Print();
            }
            for (i = msg.ReadByte(); i < MAX_ASYNC_CLIENTS; i = msg.ReadByte()) {
                serverInfo.pings[serverInfo.clients] = (short) msg.ReadShort();
                serverInfo.rate[serverInfo.clients] = msg.ReadLong();
                msg.ReadString(serverInfo.nickname[serverInfo.clients], MAX_NICKLEN);
                if (verbose) {
                    common.Printf("client %2d: %s, ping = %d, rate = %d\n", i, serverInfo.nickname[serverInfo.clients], serverInfo.pings[serverInfo.clients], serverInfo.rate[serverInfo.clients]);
                }
                serverInfo.clients++;
            }
            serverInfo.OSMask = msg.ReadLong();
            index = serverList.InfoResponse(serverInfo) != 0 ? 1 : 0;

            common.Printf("%d: server %s - protocol %d.%d - %s\n", index, Sys_NetAdrToString(serverInfo.adr), protocol >> 16, protocol & 0xffff, serverInfo.serverInfo.GetString("si_name"));
        }

        private void ProcessPrintMessage(final netadr_t from, final idBitMsg msg) throws idException {
            char[] str = new char[MAX_STRING_CHARS];
            int opcode;
            int game_opcode = ALLOW_YES.ordinal();
            String retpass, string;

            opcode = msg.ReadLong();
            if (opcode == SERVER_PRINT_GAMEDENY.ordinal()) {
                game_opcode = msg.ReadLong();
            }
            ReadLocalizedServerString(msg, str, MAX_STRING_CHARS);
            string = ctos(str);
            common.Printf("%s\n", string);
            guiNetMenu.SetStateString("status", string);
            if (opcode == SERVER_PRINT_GAMEDENY.ordinal()) {
                if (game_opcode == ALLOW_BADPASS.ordinal()) {
                    retpass = session.MessageBox(MSG_PROMPT, common.GetLanguageDict().GetString("#str_04321"), string, true, "passprompt_ok");
                    ClearPendingPackets();
                    guiNetMenu.SetStateString("status", common.GetLanguageDict().GetString("#str_04322"));
                    if (retpass != null) {
                        // #790
                        cvarSystem.SetCVarString("password", "");
                        cvarSystem.SetCVarString("password", retpass);
                    } else {
                        cmdSystem.BufferCommandText(CMD_EXEC_NOW, "disconnect");
                    }
                } else if (game_opcode == ALLOW_NO.ordinal()) {
                    session.MessageBox(MSG_OK, string, common.GetLanguageDict().GetString("#str_04323"), true);
                    ClearPendingPackets();
                    cmdSystem.BufferCommandText(CMD_EXEC_NOW, "disconnect");
                }
                // ALLOW_NOTYET just keeps running as usual. The GUI has an abort button
            } else if (opcode == SERVER_PRINT_BADCHALLENGE.ordinal() && clientState.ordinal() >= CS_CONNECTING.ordinal()) {
                cmdSystem.BufferCommandText(CMD_EXEC_NOW, "reconnect");
            }
        }

        private void ProcessServersListMessage(final netadr_t from, final idBitMsg msg) {
            if (!Sys_CompareNetAdrBase(idAsyncNetwork.GetMasterAddress(), from)) {
                common.DPrintf("received a server list from %s - not a valid master\n", Sys_NetAdrToString(from));
                return;
            }
            while (msg.GetRemaingData() != 0) {
                int a, b, c, d;
                a = msg.ReadByte();
                b = msg.ReadByte();
                c = msg.ReadByte();
                d = msg.ReadByte();
                serverList.AddServer(serverList.Num(), va("%d.%d.%d.%d:%d", a, b, c, d, msg.ReadShort()));
            }
        }

        private void ProcessAuthKeyMessage(final netadr_t from, final idBitMsg msg) throws idException {
            authKeyMsg_t authMsg;
            char[] read_string = new char[MAX_STRING_CHARS];
            String retkey;
            authBadKeyStatus_t authBadStatus;
            int key_index;
            boolean[] valid = new boolean[2];
            String auth_msg = "";
            idStr auth_msg2 = new idStr();

            if (clientState != CS_CONNECTING && !session.WaitingForGameAuth()) {
                common.Printf("clientState != CS_CONNECTING, not waiting for game auth, authKey ignored\n");
                return;
            }

            authMsg = authKeyMsg_t.values()[msg.ReadByte()];//TODO:out of bounds check.
            if (authMsg == AUTHKEY_BADKEY) {
                valid[0] = valid[1] = true;
//                key_index = 0;
                authBadStatus = authBadKeyStatus_t.values()[msg.ReadByte()];
                switch (authBadStatus) {
                    case AUTHKEY_BAD_INVALID:
                        valid[0] = (msg.ReadByte() == 1);
                        valid[1] = (msg.ReadByte() == 1);
                        idAsyncNetwork.BuildInvalidKeyMsg(auth_msg2, valid);
                        auth_msg = auth_msg2.toString();
                        break;
                    case AUTHKEY_BAD_BANNED:
                        key_index = msg.ReadByte();
                        auth_msg = common.GetLanguageDict().GetString(va("#str_0719%1d", 6 + key_index));
                        auth_msg += "\n";
                        auth_msg += common.GetLanguageDict().GetString("#str_04304");
                        valid[key_index] = false;
                        break;
                    case AUTHKEY_BAD_INUSE:
                        key_index = msg.ReadByte();
                        auth_msg = common.GetLanguageDict().GetString(va("#str_0719%1d", 8 + key_index));
                        auth_msg += "\n";
                        auth_msg += common.GetLanguageDict().GetString("#str_04304");
                        valid[key_index] = false;
                        break;
                    case AUTHKEY_BAD_MSG:
                        // a general message explaining why this key is denied
                        // no specific use for this atm. let's not clear the keys either
                        msg.ReadString(read_string, MAX_STRING_CHARS);
                        auth_msg = ctos(read_string);
                        break;
                }
                common.DPrintf("auth deny: %s\n", auth_msg);

                // keys to be cleared. applies to both net connect and game auth
                session.ClearCDKey(valid);

                // get rid of the bad key - at least that's gonna annoy people who stole a fake key
                if (clientState == CS_CONNECTING) {
                    while (true) {
                        // here we use the auth status message
                        retkey = session.MessageBox(MSG_CDKEY, auth_msg, common.GetLanguageDict().GetString("#str_04325"), true);
                        if (isNotNullOrEmpty(retkey)) {
                            if (session.CheckKey(retkey, true, valid)) {
                                cmdSystem.BufferCommandText(CMD_EXEC_NOW, "reconnect");
                            } else {
                                // build a more precise message about the offline check failure
                                idAsyncNetwork.BuildInvalidKeyMsg(auth_msg2, valid);
                                auth_msg = auth_msg2.toString();
                                session.MessageBox(MSG_OK, auth_msg, common.GetLanguageDict().GetString("#str_04327"), true);
                                continue;
                            }
                        } else {
                            cmdSystem.BufferCommandText(CMD_EXEC_NOW, "disconnect");
                        }
                        break;
                    }
                } else {
                    // forward the auth status information to the session code
                    session.CDKeysAuthReply(false, auth_msg);
                }
            } else {
                msg.ReadString(read_string, MAX_STRING_CHARS);
                cvarSystem.SetCVarString("com_guid", ctos(read_string));
                common.Printf("guid set to %s\n", read_string);
                session.CDKeysAuthReply(true, null);
            }
        }

        private void ProcessVersionMessage(final netadr_t from, final idBitMsg msg) {
            char[] string = new char[MAX_STRING_CHARS];

            if (updateState != UPDATE_SENT) {
                common.Printf("ProcessVersionMessage: version reply, != UPDATE_SENT\n");
                return;
            }

            common.Printf("A new version is available\n");
            msg.ReadString(string, MAX_STRING_CHARS);
            updateMSG = new idStr(string);
            updateDirectDownload = (msg.ReadByte() != 0);
            msg.ReadString(string, MAX_STRING_CHARS);
            updateURL = new idStr(string);
            updateMime = dlMime_t.values()[msg.ReadByte()];
            msg.ReadString(string, MAX_STRING_CHARS);
            updateFallback = new idStr(string);
            updateState = UPDATE_READY;
        }

        private void ConnectionlessMessage(final netadr_t from, final idBitMsg msg) throws idException {
            char[] str = new char[MAX_STRING_CHARS * 2];  // M. Quinn - Even Balance - PB packets can go beyond 1024
            String string;

            msg.ReadString(str, str.length);
            string = ctos(str);

            // info response from a server, are accepted from any source
            if (idStr.Icmp(string, "infoResponse") == 0) {
                ProcessInfoResponseMessage(from, msg);
                return;
            }

            // from master server:
            if (Sys_CompareNetAdrBase(from, idAsyncNetwork.GetMasterAddress())) {
                // server list
                if (idStr.Icmp(string, "servers") == 0) {
                    ProcessServersListMessage(from, msg);
                    return;
                }

                if (idStr.Icmp(string, "authKey") == 0) {
                    ProcessAuthKeyMessage(from, msg);
                    return;
                }

                if (idStr.Icmp(string, "newVersion") == 0) {
                    ProcessVersionMessage(from, msg);
                    return;
                }
            }

            // ignore if not from the current/last server
            if (!Sys_CompareNetAdrBase(from, serverAddress) && (lastRconTime + 10000 < realTime || !Sys_CompareNetAdrBase(from, lastRconAddress))) {
                common.DPrintf("got message '%s' from bad source: %s\n", string, Sys_NetAdrToString(from));
                return;
            }

            // challenge response from the server we are connecting to
            if (idStr.Icmp(string, "challengeResponse") == 0) {
                ProcessChallengeResponseMessage(from, msg);
                return;
            }

            // connect response from the server we are connecting to
            if (idStr.Icmp(string, "connectResponse") == 0) {
                ProcessConnectResponseMessage(from, msg);
                return;
            }

            // a disconnect message from the server, which will happen if the server
            // dropped the connection but is still getting packets from this client
            if (idStr.Icmp(string, "disconnect") == 0) {
                ProcessDisconnectMessage(from, msg);
                return;
            }

            // print request from server
            if (idStr.Icmp(string, "print") == 0) {
                ProcessPrintMessage(from, msg);
                return;
            }

            // server pure list
            if (idStr.Icmp(string, "pureServer") == 0) {
                ProcessPureMessage(from, msg);
                return;
            }

            if (idStr.Icmp(string, "downloadInfo") == 0) {
                ProcessDownloadInfoMessage(from, msg);
            }

            if (idStr.Icmp(string, "authrequired") == 0) {
                // server telling us that he's expecting an auth mode connect, just in case we're trying to connect in LAN mode
                if (idAsyncNetwork.LANServer.GetBool()) {
                    common.Warning("server %s requests master authorization for this client. Turning off LAN mode\n", Sys_NetAdrToString(from));
                    idAsyncNetwork.LANServer.SetBool(false);
                }
            }

            common.DPrintf("ignored message from %s: %s\n", Sys_NetAdrToString(from), string);
        }

        private void ProcessMessage(final netadr_t from, idBitMsg msg) {
            int id;

            id = msg.ReadShort();

            // check for a connectionless packet
            if (id == CONNECTIONLESS_MESSAGE_ID) {
                ConnectionlessMessage(from, msg);
                return;
            }

            if (clientState.ordinal() < CS_CONNECTED.ordinal()) {
                return;		// can't be a valid sequenced packet
            }

            if (msg.GetRemaingData() < 4) {
                common.DPrintf("%s: tiny packet\n", Sys_NetAdrToString(from));
                return;
            }

            // is this a packet from the server
            if (!Sys_CompareNetAdrBase(from, channel.GetRemoteAddress()) || id != serverId) {
                common.DPrintf("%s: sequenced server packet without connection\n", Sys_NetAdrToString(from));
                return;
            }

            int[] serverMessageSequence = {0};
            if (!channel.Process(from, clientTime, msg, serverMessageSequence)) {
                this.serverMessageSequence = serverMessageSequence[0];
                return;		// out of order, duplicated, fragment, etc.
            }
            this.serverMessageSequence = serverMessageSequence[0];

            lastPacketTime = clientTime;
            ProcessReliableServerMessages();
            ProcessUnreliableServerMessage(msg);
        }

        private void SetupConnection() throws idException {
            idBitMsg msg = new idBitMsg();
            ByteBuffer msgBuf = ByteBuffer.allocate(MAX_MESSAGE_SIZE);

            if (clientTime - lastConnectTime < SETUP_CONNECTION_RESEND_TIME) {
                return;
            }

            if (clientState == CS_CHALLENGING) {
                common.Printf("sending challenge to %s\n", Sys_NetAdrToString(serverAddress));
                msg.Init(msgBuf, MAX_MESSAGE_SIZE);
                msg.WriteShort(CONNECTIONLESS_MESSAGE_ID);
                msg.WriteString("challenge");
                msg.WriteLong(clientId);
                clientPort.SendPacket(serverAddress, msg.GetData(), msg.GetSize());
            } else if (clientState == CS_CONNECTING) {
                common.Printf("sending connect to %s with challenge 0x%x\n", Sys_NetAdrToString(serverAddress), serverChallenge);
                msg.Init(msgBuf, MAX_MESSAGE_SIZE);
                msg.WriteShort(CONNECTIONLESS_MESSAGE_ID);
                msg.WriteString("connect");
                msg.WriteLong(ASYNC_PROTOCOL_VERSION);
                if (ID_FAKE_PURE) {
                    // fake win32 OS - might need to adapt depending on the case
                    msg.WriteShort(0);
                } else {
                    msg.WriteShort(BUILD_OS_ID);
                }
                msg.WriteLong(clientDataChecksum.intValue());
                msg.WriteLong(serverChallenge);
                msg.WriteShort(clientId);
                msg.WriteLong(cvarSystem.GetCVarInteger("net_clientMaxRate"));
                msg.WriteString(cvarSystem.GetCVarString("com_guid"));
                msg.WriteString(cvarSystem.GetCVarString("password"), -1, false);
                // do not make the protocol depend on PB
                msg.WriteShort(0);
                clientPort.SendPacket(serverAddress, msg.GetData(), msg.GetSize());

                if (idAsyncNetwork.LANServer.GetBool()) {
                    common.Printf("net_LANServer is set, connecting in LAN mode\n");
                } else {
                    // emit a cd key authorization request
                    // modified at protocol 1.37 for XP key addition
                    msg.BeginWriting();
                    msg.WriteShort(CONNECTIONLESS_MESSAGE_ID);
                    msg.WriteString("clAuth");
                    msg.WriteLong(ASYNC_PROTOCOL_VERSION);
                    msg.WriteNetadr(serverAddress);
                    // if we don't have a com_guid, this will request a direct reply from auth with it
                    msg.WriteByte(!cvarSystem.GetCVarString("com_guid").isEmpty() ? 1 : 0);
                    // send the main key, and flag an extra byte to add XP key
                    msg.WriteString(session.GetCDKey(false));
                    final String xpkey = session.GetCDKey(true);
                    msg.WriteByte(xpkey != null ? 1 : 0);
                    if (xpkey != null) {
                        msg.WriteString(xpkey);
                    }
                    clientPort.SendPacket(idAsyncNetwork.GetMasterAddress(), msg.GetData(), msg.GetSize());
                }
            } else {
                return;
            }

            lastConnectTime = clientTime;
        }

        private void ProcessPureMessage(final netadr_t from, final idBitMsg msg) {
            idBitMsg outMsg = new idBitMsg();
            ByteBuffer msgBuf = ByteBuffer.allocate(MAX_MESSAGE_SIZE);
            int i;
            int[] inChecksums = new int[MAX_PURE_PAKS];
            int[] gamePakChecksum = new int[1];

            if (clientState != CS_CONNECTING) {
                common.Printf("clientState != CS_CONNECTING, pure msg ignored\n");
                return;
            }

            if (!ValidatePureServerChecksums(from, msg)) {
                return;
            }

            fileSystem.GetPureServerChecksums(inChecksums, -1, gamePakChecksum);
            outMsg.Init(msgBuf, MAX_MESSAGE_SIZE);
            outMsg.WriteShort(CONNECTIONLESS_MESSAGE_ID);
            outMsg.WriteString("pureClient");
            outMsg.WriteLong(serverChallenge);
            outMsg.WriteShort(clientId);
            i = 0;
            while (inChecksums[ i] != 0) {
                outMsg.WriteLong(inChecksums[ i++]);
            }
            outMsg.WriteLong(0);
            outMsg.WriteLong(gamePakChecksum[0]);
            clientPort.SendPacket(from, outMsg.GetData(), outMsg.GetSize());
        }

        private boolean ValidatePureServerChecksums(final netadr_t from, final idBitMsg msg) throws idException {
            int i, numChecksums, numMissingChecksums;
            int[] inChecksums = new int[MAX_PURE_PAKS];
            int inGamePakChecksum;
            int[] missingChecksums = new int[MAX_PURE_PAKS];
            int[] missingGamePakChecksum = new int[1];
            idBitMsg dlmsg = new idBitMsg();
            ByteBuffer msgBuf = ByteBuffer.allocate(MAX_MESSAGE_SIZE);

            // read checksums
            // pak checksums, in a 0-terminated list
            numChecksums = 0;
            do {
                i = msg.ReadLong();
                inChecksums[ numChecksums++] = i;
                // just to make sure a broken message doesn't crash us
                if (numChecksums >= MAX_PURE_PAKS) {
                    common.Warning("MAX_PURE_PAKS ( %d ) exceeded in idAsyncClient.ProcessPureMessage\n", MAX_PURE_PAKS);
                    return false;
                }
            } while (i != 0);
            inChecksums[ numChecksums] = 0;
            inGamePakChecksum = msg.ReadLong();

            fsPureReply_t reply = fileSystem.SetPureServerChecksums(inChecksums, inGamePakChecksum, missingChecksums, missingGamePakChecksum);
            switch (reply) {
                case PURE_RESTART:
                    // need to restart the filesystem with a different pure configuration
                    cmdSystem.BufferCommandText(CMD_EXEC_NOW, "disconnect");
                    // restart with the right FS configuration and get back to the server
                    clientState = CS_PURERESTART;
                    fileSystem.SetRestartChecksums(inChecksums, inGamePakChecksum);
                    cmdSystem.BufferCommandText(CMD_EXEC_NOW, "reloadEngine");
                    return false;
                case PURE_MISSING: {

                    String checkSums = "";

                    i = 0;
                    while (missingChecksums[ i] != 0) {
                        checkSums += va("0x%x ", missingChecksums[i++]);
                    }
                    numMissingChecksums = i;

                    if (idAsyncNetwork.clientDownload.GetInteger() == 0) {
                        // never any downloads
                        String message = va(common.GetLanguageDict().GetString("#str_07210"), Sys_NetAdrToString(from));

                        if (numMissingChecksums > 0) {
                            message += va(common.GetLanguageDict().GetString("#str_06751"), numMissingChecksums, checkSums);
                        }
                        if (missingGamePakChecksum[0] != 0) {
                            message += va(common.GetLanguageDict().GetString("#str_06750"), missingGamePakChecksum[0]);
                        }

                        common.Printf(message);
                        cmdSystem.BufferCommandText(CMD_EXEC_NOW, "disconnect");
                        session.MessageBox(MSG_OK, message, common.GetLanguageDict().GetString("#str_06735"), true);
                    } else {
                        if (clientState.compareTo(CS_CONNECTED) != -1) {
                            // we are already connected, reconnect to negociate the paks in connectionless mode
                            cmdSystem.BufferCommandText(CMD_EXEC_NOW, "reconnect");
                            return false;
                        }
                        // ask the server to send back download info
                        common.DPrintf("missing %d paks: %s\n", numMissingChecksums + (missingGamePakChecksum[0] != 0 ? 1 : 0), checkSums);
                        if (missingGamePakChecksum[0] != 0) {
                            common.DPrintf("game code pak: 0x%x\n", missingGamePakChecksum[0]);
                        }
                        // store the requested downloads
                        GetDownloadRequest(missingChecksums, numMissingChecksums, missingGamePakChecksum[0]);
                        // build the download request message
                        // NOTE: in a specific function?
                        dlmsg.Init(msgBuf, msgBuf.capacity());
                        dlmsg.WriteShort(CONNECTIONLESS_MESSAGE_ID);
                        dlmsg.WriteString("downloadRequest");
                        dlmsg.WriteLong(serverChallenge);
                        dlmsg.WriteShort(clientId);
                        // used to make sure the server replies to the same download request
                        dlmsg.WriteLong(dlRequest);
                        // special case the code pak - if we have a 0 checksum then we don't need to download it
                        dlmsg.WriteLong(missingGamePakChecksum[0]);
                        // 0-terminated list of missing paks
                        i = 0;
                        while (missingChecksums[ i] != 0) {
                            dlmsg.WriteLong(missingChecksums[ i++]);
                        }
                        dlmsg.WriteLong(0);
                        clientPort.SendPacket(from, dlmsg.GetData(), dlmsg.GetSize());
                    }

                    return false;
                }
                case PURE_NODLL:
                    common.Printf(common.GetLanguageDict().GetString("#str_07211"), Sys_NetAdrToString(from));
                    cmdSystem.BufferCommandText(CMD_EXEC_NOW, "disconnect");
                    return false;
                default:
//                    return true;
            }
            return true;
        }

        private void ProcessReliableMessagePure(final idBitMsg msg) {
            idBitMsg outMsg = new idBitMsg();
            ByteBuffer msgBuf = ByteBuffer.allocate(MAX_MESSAGE_SIZE);
            int[] inChecksums = new int[MAX_PURE_PAKS];
            int i;
            int[] gamePakChecksum = new int[1];
            int serverGameInitId;

            session.SetGUI(null, null);

            serverGameInitId = msg.ReadLong();

            if (serverGameInitId != gameInitId) {
                common.DPrintf("ignoring pure server checksum from an outdated gameInitId (%d)\n", serverGameInitId);
                return;
            }

            if (!ValidatePureServerChecksums(serverAddress, msg)) {

                return;
            }

            if (idAsyncNetwork.verbose.GetInteger() != 0) {
                common.Printf("received new pure server info. ExecuteMapChange and report back\n");
            }

            // it is now ok to load the next map with updated pure checksums
            sessLocal.ExecuteMapChange(true);

            // upon receiving our pure list, the server will send us SCS_INGAME and we'll start getting snapshots
            fileSystem.GetPureServerChecksums(inChecksums, -1, gamePakChecksum);
            outMsg.Init(msgBuf, msgBuf.capacity());
            outMsg.WriteByte(CLIENT_RELIABLE_MESSAGE_PURE.ordinal());

            outMsg.WriteLong(gameInitId);

            i = 0;
            while (inChecksums[ i] != 0) {
                outMsg.WriteLong(inChecksums[ i++]);
            }
            outMsg.WriteLong(0);
            outMsg.WriteLong(gamePakChecksum[0]);

            if (!channel.SendReliableMessage(outMsg)) {
                common.Error("client.server reliable messages overflow\n");
            }
        }

        private static class HandleGuiCommand extends HandleGuiCommand_t {

            private static final HandleGuiCommand_t instance = new HandleGuiCommand();

            private HandleGuiCommand() {
            }

            public static HandleGuiCommand_t getInstance() {
                return instance;
            }

            @Override
            public String run(final String input) {
                return idAsyncNetwork.client.HandleGuiCommandInternal(input);
            }
        }

        private String HandleGuiCommandInternal(final String cmd) {
            if (0 == idStr.Cmp(cmd, "abort") || 0 == idStr.Cmp(cmd, "pure_abort")) {
                common.DPrintf("connection aborted\n");
                cmdSystem.BufferCommandText(CMD_EXEC_NOW, "disconnect");
                return "";
            } else {
                common.DWarning("idAsyncClient::HandleGuiCommand: unknown cmd %s", cmd);
            }
            return null;
        }

        /*
         ==================
         idAsyncClient::SendVersionDLUpdate

         sending those packets is not strictly necessary. just a way to tell the update server
         about what is going on. allows the update server to have a more precise view of the overall
         network load for the updates
         ==================
         */
        private void SendVersionDLUpdate(int state) {
            idBitMsg msg = new idBitMsg();
            ByteBuffer msgBuf = ByteBuffer.allocate(MAX_MESSAGE_SIZE);

            msg.Init(msgBuf, msgBuf.capacity());
            msg.WriteShort(CONNECTIONLESS_MESSAGE_ID);
            msg.WriteString("versionDL");
            msg.WriteLong(ASYNC_PROTOCOL_VERSION);
            msg.WriteShort(state);
            clientPort.SendPacket(idAsyncNetwork.GetMasterAddress(), msg.GetData(), msg.GetSize());
        }

        private void HandleDownloads() throws idException {

            if (updateState == UPDATE_SENT && clientTime > updateSentTime + 2000) {
                // timing out on no reply
                updateState = UPDATE_DONE;
                if (showUpdateMessage) {
                    session.MessageBox(MSG_OK, common.GetLanguageDict().GetString("#str_04839"), common.GetLanguageDict().GetString("#str_04837"), true);
                    showUpdateMessage = false;
                }
                common.DPrintf("No update available\n");
            } else if (backgroundDownload.completed) {
                // only enter these if the download slot is free
                if (updateState == UPDATE_READY) {
                    //
                    if (session.MessageBox(MSG_YESNO, updateMSG.toString(), common.GetLanguageDict().GetString("#str_04330"), true, "yes").isEmpty() == false) {
                        if (!updateDirectDownload) {
                            sys.OpenURL(updateURL.toString(), true);
                            updateState = UPDATE_DONE;
                        } else {

                            // we're just creating the file at toplevel inside fs_savepath
                            updateURL.ExtractFileName(updateFile);
                            idFile_Permanent f = (idFile_Permanent) fileSystem.OpenFileWrite(updateFile.toString());
                            dltotal = 0;
                            dlnow = 0;

                            backgroundDownload.completed = false;
                            backgroundDownload.opcode = DLTYPE_URL;
                            backgroundDownload.f = f;
                            backgroundDownload.url.status = DL_WAIT;
                            backgroundDownload.url.dlnow = 0;
                            backgroundDownload.url.dltotal = 0;
                            backgroundDownload.url.url = updateURL;
                            fileSystem.BackgroundDownload(backgroundDownload);

                            updateState = UPDATE_DLING;
                            SendVersionDLUpdate(0);
                            session.DownloadProgressBox(backgroundDownload, va("Downloading %s\n", updateFile));
                            updateState = UPDATE_DONE;
                            if (backgroundDownload.url.status == DL_DONE) {
                                SendVersionDLUpdate(1);
                                idStr fullPath = new idStr(f.GetFullPath());
                                fileSystem.CloseFile(f);
                                if (session.MessageBox(MSG_YESNO, common.GetLanguageDict().GetString("#str_04331"), common.GetLanguageDict().GetString("#str_04332"), true, "yes").isEmpty() == false) {
                                    if (updateMime == FILE_EXEC) {
                                        sys.StartProcess(fullPath.toString(), true);
                                    } else {
                                        sys.OpenURL(va("file://%s", fullPath.toString()), true);
                                    }
                                } else {
                                    session.MessageBox(MSG_OK, va(common.GetLanguageDict().GetString("#str_04333"), fullPath), common.GetLanguageDict().GetString("#str_04334"), true);
                                }
                            } else {
                                if (!backgroundDownload.url.dlerror.isEmpty()) {
                                    common.Warning("update download failed. curl error: %s", backgroundDownload.url.dlerror);
                                }
                                SendVersionDLUpdate(2);
                                idStr name = new idStr(f.GetName());
                                fileSystem.CloseFile(f);
                                fileSystem.RemoveFile(name.toString());
                                session.MessageBox(MSG_OK, common.GetLanguageDict().GetString("#str_04335"), common.GetLanguageDict().GetString("#str_04336"), true);
                                if (updateFallback.Length() != 0) {
                                    sys.OpenURL(updateFallback.toString(), true);
                                } else {
                                    common.Printf("no fallback URL\n");
                                }
                            }
                        }
                    } else {
                        updateState = UPDATE_DONE;
                    }
                } else if (dlList.Num() != 0) {

                    int numPaks = dlList.Num();
                    int pakCount = 1;
                    int progress_start, progress_end;
                    currentDlSize = 0;

                    do {

                        if (dlList.oGet(0).url.oGet(0) == '\0') {
                            // ignore empty files
                            dlList.RemoveIndex(0);
                            continue;
                        }
                        common.Printf("start download for %s\n", dlList.oGet(0).url);

                        idFile_Permanent f = (idFile_Permanent) (fileSystem.MakeTemporaryFile());
                        if (null == f) {
                            common.Warning("could not create temporary file");
                            dlList.Clear();
                            return;
                        }

                        backgroundDownload.completed = false;
                        backgroundDownload.opcode = DLTYPE_URL;
                        backgroundDownload.f = f;
                        backgroundDownload.url.status = DL_WAIT;
                        backgroundDownload.url.dlnow = 0;
                        backgroundDownload.url.dltotal = dlList.oGet(0).size;
                        backgroundDownload.url.url = dlList.oGet(0).url;
                        fileSystem.BackgroundDownload(backgroundDownload);
                        String dltitle;
                        // "Downloading %s"
                        dltitle = String.format(common.GetLanguageDict().GetString("#str_07213"), dlList.oGet(0).filename.toString());
                        if (numPaks > 1) {
                            dltitle += va(" (%d/%d)", pakCount, numPaks);
                        }
                        if (totalDlSize != 0) {
                            progress_start = (int) ((float) currentDlSize * 100.0f / (float) totalDlSize);
                            progress_end = (int) ((float) (currentDlSize + dlList.oGet(0).size) * 100.0f / (float) totalDlSize);
                        } else {
                            progress_start = 0;
                            progress_end = 100;
                        }
                        session.DownloadProgressBox(backgroundDownload, dltitle, progress_start, progress_end);
                        if (backgroundDownload.url.status == DL_DONE) {
                            idFile saveas;
                            final int CHUNK_SIZE = 1024 * 1024;
                            ByteBuffer buf;
                            int remainlen;
                            int readlen;
                            int retlen;
                            int checksum;

                            common.Printf("file downloaded\n");
                            idStr finalPath = new idStr(cvarSystem.GetCVarString("fs_savepath"));
                            finalPath.AppendPath(dlList.oGet(0).filename.toString());
                            fileSystem.CreateOSPath(finalPath.toString());
                            // do the final copy ourselves so we do by small chunks in case the file is big
                            saveas = fileSystem.OpenExplicitFileWrite(finalPath.toString());
                            buf = ByteBuffer.allocate(CHUNK_SIZE);// Mem_Alloc(CHUNK_SIZE);
                            f.Seek(0, FS_SEEK_END);
                            remainlen = f.Tell();
                            f.Seek(0, FS_SEEK_SET);
                            while (remainlen != 0) {
                                readlen = Min(remainlen, CHUNK_SIZE);
                                retlen = f.Read(buf, readlen);
                                if (retlen != readlen) {
                                    common.FatalError("short read %d of %d in idFileSystem.HandleDownload", retlen, readlen);
                                }
                                retlen = saveas.Write(buf, readlen);
                                if (retlen != readlen) {
                                    common.FatalError("short write %d of %d in idFileSystem.HandleDownload", retlen, readlen);
                                }
                                remainlen -= readlen;
                            }
                            fileSystem.CloseFile(f);
                            fileSystem.CloseFile(saveas);
                            common.Printf("saved as %s\n", finalPath);
                            buf = null;//Mem_Free(buf);

                            // add that file to our paks list
                            checksum = fileSystem.AddZipFile(dlList.oGet(0).filename.toString());

                            // verify the checksum to be what the server says
                            if (0 == checksum || checksum != dlList.oGet(0).checksum) {
                                // "pak is corrupted ( checksum 0x%x, expected 0x%x )"
                                session.MessageBox(MSG_OK, va(common.GetLanguageDict().GetString("#str_07214"), checksum, dlList.oGet(0).checksum), "Download failed", true);
                                fileSystem.RemoveFile(dlList.oGet(0).filename.toString());
                                dlList.Clear();
                                return;
                            }

                            currentDlSize += dlList.oGet(0).size;

                        } else {
                            common.Warning("download failed: %s", dlList.oGet(0).url);
                            if (!backgroundDownload.url.dlerror.isEmpty()) {
                                common.Warning("curl error: %s", backgroundDownload.url.dlerror);
                            }
                            // "The download failed or was cancelled"
                            // "Download failed"
                            session.MessageBox(MSG_OK, common.GetLanguageDict().GetString("#str_07215"), common.GetLanguageDict().GetString("#str_07216"), true);
                            dlList.Clear();
                            return;
                        }

                        pakCount++;
                        dlList.RemoveIndex(0);
                    } while (dlList.Num() != 0);

                    // all downloads successful - do the dew
                    cmdSystem.BufferCommandText(CMD_EXEC_APPEND, "reconnect\n");
                }
            }
        }

        private void Idle() {
            // also need to read mouse for the connecting guis
            usercmdGen.GetDirectUsercmd();

            SendEmptyToServer();
        }

        private int UpdateTime(int clamp) {
            int time, msec;

            time = Sys_Milliseconds();
            msec = idMath.ClampInt(0, clamp, time - realTime);
            realTime = time;
            clientTime += msec;
            return msec;
        }

        private void ReadLocalizedServerString(final idBitMsg msg, char[] out, int maxLen) {
            msg.ReadString(out, maxLen);
            // look up localized string. if the message is not an #str_ format, we'll just get it back unchanged
            idStr.snPrintf(out, maxLen - 1, "%s", common.GetLanguageDict().GetString(ctos(out)));
        }

        private boolean CheckTimeout() {
            if (lastPacketTime > 0 && (lastPacketTime + idAsyncNetwork.clientServerTimeout.GetInteger() * 1000 < clientTime)) {
                session.StopBox();
                session.MessageBox(MSG_OK, common.GetLanguageDict().GetString("#str_04328"), common.GetLanguageDict().GetString("#str_04329"), true);
                cmdSystem.BufferCommandText(CMD_EXEC_NOW, "disconnect");
                return true;
            }
            return false;
        }

        private void ProcessDownloadInfoMessage(final netadr_t from, final idBitMsg msg) throws idException {
            char[] buf = new char[MAX_STRING_CHARS];
            int srvDlRequest = msg.ReadLong();
            int infoType = msg.ReadByte();
            int pakDl;
            int pakIndex;

            pakDlEntry_t entry = new pakDlEntry_t();
            boolean gotAllFiles = true;
            idStr sizeStr = new StrPool.idPoolStr();
            boolean gotGame = false;

            if (dlRequest == -1 || srvDlRequest != dlRequest) {
                common.Warning("bad download id from server, ignored");
                return;
            }
            // mark the dlRequest as dead now whatever how we process it
            dlRequest = -1;

            if (infoType == SERVER_DL_REDIRECT.ordinal()) {
                msg.ReadString(buf, MAX_STRING_CHARS);
                cmdSystem.BufferCommandText(CMD_EXEC_NOW, "disconnect");
                // "You are missing required pak files to connect to this server.\nThe server gave a web page though:\n%s\nDo you want to go there now?"
                // "Missing required files"
                if (isNotNullOrEmpty(session.MessageBox(MSG_YESNO, va(common.GetLanguageDict().GetString("#str_07217"), buf),
                        common.GetLanguageDict().GetString("#str_07218"), true, "yes"))) {
                    sys.OpenURL(ctos(buf), true);
                }
            } else if (infoType == SERVER_DL_LIST.ordinal()) {
                cmdSystem.BufferCommandText(CMD_EXEC_NOW, "disconnect");
                if (dlList.Num() != 0) {
                    common.Warning("tried to process a download list while already busy downloading things");
                    return;
                }
                // read the URLs, check against what we requested, prompt for download
                pakIndex = -1;
                totalDlSize = 0;
                do {
                    pakIndex++;
                    pakDl = msg.ReadByte();
                    if (pakDl == SERVER_PAK_YES.ordinal()) {
                        if (pakIndex == 0) {
                            gotGame = true;
                        }
                        msg.ReadString(buf, MAX_STRING_CHARS);
                        entry.filename = new idStr(buf);
                        msg.ReadString(buf, MAX_STRING_CHARS);
                        entry.url = new idStr(buf);
                        entry.size = msg.ReadLong();
                        // checksums are not transmitted, we read them from the dl request we sent
                        entry.checksum = dlChecksums[ pakIndex];
                        totalDlSize += entry.size;
                        dlList.Append(entry);
                        common.Printf("download %s from %s ( 0x%x )\n", entry.filename, entry.url, entry.checksum);
                    } else if (pakDl == SERVER_PAK_NO.ordinal()) {
                        msg.ReadString(buf, MAX_STRING_CHARS);
                        entry.filename = new idStr(buf);
                        entry.url = new idStr("");
                        entry.size = 0;
                        entry.checksum = 0;
                        dlList.Append(entry);
                        // first pak is game pak, only fail it if we actually requested it
                        if (pakIndex != 0 || dlChecksums[ 0] != 0) {
                            common.Printf("no download offered for %s ( 0x%x )\n", entry.filename, dlChecksums[ pakIndex]);
                            gotAllFiles = false;
                        }
                    } else {
                        assert (pakDl == SERVER_PAK_END.ordinal());
                    }
                } while (pakDl != SERVER_PAK_END.ordinal());
                if (dlList.Num() < dlCount) {
                    common.Printf("%d files were ignored by the server\n", dlCount - dlList.Num());
                    gotAllFiles = false;
                }
                sizeStr.BestUnit("%.2f", totalDlSize, MEASURE_SIZE);
                cmdSystem.BufferCommandText(CMD_EXEC_NOW, "disconnect");
                if (totalDlSize == 0) {
                    // was no downloadable stuff for us
                    // "Can't connect to the pure server: no downloads offered"
                    // "Missing required files"
                    dlList.Clear();
                    session.MessageBox(MSG_OK, common.GetLanguageDict().GetString("#str_07219"), common.GetLanguageDict().GetString("#str_07218"), true);
                    return;
                }
                boolean asked = false;
                if (gotGame) {
                    asked = true;
                    // "You need to download game code to connect to this server. Are you sure? You should only answer yes if you trust the server administrators."
                    // "Missing game binaries"
                    if (session.MessageBox(MSG_YESNO, common.GetLanguageDict().GetString("#str_07220"), common.GetLanguageDict().GetString("#str_07221"), true, "yes").isEmpty()) {
                        dlList.Clear();
                        return;
                    }
                }
                if (!gotAllFiles) {
                    asked = true;
                    // "The server only offers to download some of the files required to connect ( %s ). Download anyway?"
                    // "Missing required files"
                    if (NOT(session.MessageBox(MSG_YESNO, va(common.GetLanguageDict().GetString("#str_07222"), sizeStr.toString()),
                            common.GetLanguageDict().GetString("#str_07218"), true, "yes"))) {//TODO:check whether a NOT on the whole string is the same as an empty string
                        dlList.Clear();
                        return;
                    }
                }
                if (!asked && idAsyncNetwork.clientDownload.GetInteger() == 1) {
                    // "You need to download some files to connect to this server ( %s ), proceed?"
                    // "Missing required files"
                    if (NOT(session.MessageBox(MSG_YESNO, va(common.GetLanguageDict().GetString("#str_07224"), sizeStr.toString()),
                            common.GetLanguageDict().GetString("#str_07218"), true, "yes"))) {
                        dlList.Clear();
                        return;
                    }
                }
            } else {
                cmdSystem.BufferCommandText(CMD_EXEC_NOW, "disconnect");
                // "You are missing some files to connect to this server, and the server doesn't provide downloads."
                // "Missing required files"
                session.MessageBox(MSG_OK, common.GetLanguageDict().GetString("#str_07223"), common.GetLanguageDict().GetString("#str_07218"), true);
            }
        }

        private int GetDownloadRequest(final int[] checksums/*[MAX_PURE_PAKS]*/, int count, int gamePakChecksum) {
            assert (0 == checksums[ count]); // 0-terminated
//            if (memcmp(dlChecksums + 1, checksums, sizeof(int) * count) || gamePakChecksum != dlChecksums[ 0]) {
            if (memcmp(dlChecksums, 1, checksums, 0, count) || gamePakChecksum != dlChecksums[ 0]) {
                idRandom newreq = new idRandom();

                dlChecksums[ 0] = gamePakChecksum;
//                memcpy(dlChecksums + 1, checksums, sizeof(int) * MAX_PURE_PAKS);
                memcmp(dlChecksums, 1, checksums, 0, MAX_PURE_PAKS);

                newreq.SetSeed(Sys_Milliseconds());
                dlRequest = newreq.RandomInt();
                dlCount = count + (gamePakChecksum != 0 ? 1 : 0);
                return dlRequest;
            }
            // this is the same dlRequest, we haven't heard from the server. keep the same id
            return dlRequest;
        }
    };
}
