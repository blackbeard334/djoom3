package neo.framework.Async;

import static neo.TempDump.NOT;
import static neo.TempDump.ctos;
import static neo.framework.Async.AsyncNetwork.MAX_ASYNC_CLIENTS;
import static neo.framework.Async.AsyncNetwork.MAX_NICKLEN;
import neo.framework.Async.AsyncNetwork.idAsyncNetwork;
import static neo.framework.Async.ServerScan.scan_state_t.IDLE;
import static neo.framework.Async.ServerScan.scan_state_t.LAN_SCAN;
import static neo.framework.Async.ServerScan.scan_state_t.NET_SCAN;
import static neo.framework.Async.ServerScan.scan_state_t.WAIT_ON_INIT;
import static neo.framework.Async.ServerScan.serverSort_t.SORT_GAME;
import static neo.framework.Async.ServerScan.serverSort_t.SORT_GAMETYPE;
import static neo.framework.Async.ServerScan.serverSort_t.SORT_MAP;
import static neo.framework.Async.ServerScan.serverSort_t.SORT_PING;
import static neo.framework.Async.ServerScan.serverSort_t.SORT_PLAYERS;
import static neo.framework.Async.ServerScan.serverSort_t.SORT_SERVERNAME;
import static neo.framework.CVarSystem.CVAR_ARCHIVE;
import static neo.framework.CVarSystem.CVAR_GUI;
import static neo.framework.CVarSystem.CVAR_INTEGER;
import neo.framework.CVarSystem.idCVar;
import static neo.framework.Common.common;
import neo.framework.DeclEntityDef.idDeclEntityDef;
import static neo.framework.DeclManager.declManager;
import static neo.framework.DeclManager.declType_t.DECL_MAPDEF;
import neo.framework.DeclManager.idDecl;
import static neo.framework.FileSystem_h.fileSystem;
import static neo.framework.Licensee.GAME_NAME;
import static neo.framework.Licensee.PORT_SERVER;
import neo.idlib.Dict_h.idDict;
import neo.idlib.Dict_h.idKeyValue;
import static neo.idlib.Lib.MAX_STRING_CHARS;
import static neo.idlib.Lib.Min;
import neo.idlib.Lib.idException;
import neo.idlib.Text.Str.idStr;
import static neo.idlib.Text.Str.va;
import neo.idlib.containers.List.cmp_t;
import neo.idlib.containers.List.idList;
import static neo.sys.sys_public.BUILD_OS_ID;
import neo.sys.sys_public.netadr_t;
import static neo.sys.win_net.Sys_NetAdrToString;
import static neo.sys.win_net.Sys_StringToNetAdr;
import static neo.sys.win_shared.Sys_Milliseconds;
import neo.ui.ListGUI.idListGUI;
import neo.ui.UserInterface.idUserInterface;
import static neo.ui.UserInterface.uiManager;

/**
 *
 */
public class ServerScan {
    /*
     ===============================================================================

     Scan for servers, on the LAN or from a list
     Update a listDef GUI through usage of idListGUI class
     When updating large lists of servers, sends out getInfo in small batches to avoid congestion

     ===============================================================================
     */

    static final idCVar       gui_filter_password = new idCVar("gui_filter_password", "0", CVAR_GUI | CVAR_INTEGER | CVAR_ARCHIVE, "Password filter");
    static final idCVar       gui_filter_players  = new idCVar("gui_filter_players", "0", CVAR_GUI | CVAR_INTEGER | CVAR_ARCHIVE, "Players filter");
    static final idCVar       gui_filter_gameType = new idCVar("gui_filter_gameType", "0", CVAR_GUI | CVAR_INTEGER | CVAR_ARCHIVE, "Gametype filter");
    static final idCVar       gui_filter_idle     = new idCVar("gui_filter_idle", "0", CVAR_GUI | CVAR_INTEGER | CVAR_ARCHIVE, "Idle servers filter");
    static final idCVar       gui_filter_game     = new idCVar("gui_filter_game", "0", CVAR_GUI | CVAR_INTEGER | CVAR_ARCHIVE, "Game filter");
    //
    static final String       l_gameTypes[]       = {
            "Deathmatch",
            "Tourney",
            "Team DM",
            "Last Man",
            "CTF",
            null
    };
    //
    static       idServerScan l_serverScan        = null;
//    

    // storage for incoming servers / server scan
    static class inServer_t {

        netadr_t adr;
        int      id;
        int      time;
    };

    // the menu gui uses a hard-coded control type to display a list of network games
    static class networkServer_t {

        netadr_t adr;
        idDict   serverInfo;
        int      ping;
        int      id;            // idnet mode sends an id for each server in list
        int      clients;
        char[][] nickname = new char[MAX_NICKLEN][MAX_ASYNC_CLIENTS];
        short[]  pings    = new short[MAX_ASYNC_CLIENTS];
        int[]    rate     = new int[MAX_ASYNC_CLIENTS];
        int OSMask;
        int challenge;
    };

    public enum serverSort_t {

        SORT_PING,
        SORT_SERVERNAME,
        SORT_PLAYERS,
        SORT_GAMETYPE,
        SORT_MAP,
        SORT_GAME
    };

    public enum scan_state_t {

        IDLE,
        WAIT_ON_INIT,
        LAN_SCAN,
        NET_SCAN
    };

    /*
     ================
     idServerScan
     ================
     */
    public static class idServerScan extends idList<networkServer_t> {

        private static final int MAX_PINGREQUESTS = 32;     // how many servers to query at once
        private static final int REPLY_TIMEOUT    = 999;    // how long should we wait for a reply from a game server
        private static final int INCOMING_TIMEOUT = 1500;   // when we got an incoming server list, how long till we decide the list is done
        private static final int REFRESH_START    = 10000;  // how long to wait when sending the initial refresh request
        //
        private scan_state_t       scan_state;
        //	
        private boolean            incoming_net;            // set to true while new servers are fed through AddServer
        private boolean            incoming_useTimeout;
        private int                incoming_lastTime;
        //	
        private int                lan_pingtime;            // holds the time of LAN scan
        //	
        // servers we're waiting for a reply from
        // won't exceed MAX_PINGREQUESTS elements
        // holds index of net_servers elements, indexed by 'from' string
        private idDict             net_info;
        //
        private idList<inServer_t> net_servers;
        // where we are in net_servers list for getInfo emissions ( NET_SCAN only )
        // we may either be waiting on MAX_PINGREQUESTS, or for net_servers to grow some more ( through AddServer )
        private int cur_info;
        ///
        private idUserInterface m_pGUI;
        private idListGUI listGUI;
        //
        private serverSort_t m_sort;
        private boolean m_sortAscending;
        private idList<Integer> m_sortedServers;            // use ascending for the walking order
        //
        private idStr screenshot;
        private int challenge;                              // challenge for current scan
        //	
        private int endWaitTime;                            // when to stop waiting on a port init
        //
        //

        public idServerScan() {
            m_pGUI = null;
            m_sort = SORT_PING;
            m_sortAscending = true;
            challenge = 0;

            this.net_info = new idDict();
            this.net_servers = new idList<>();
            this.m_sortedServers = new idList<>();
            this.screenshot = new idStr();
            LocalClear();
        }

        public int InfoResponse(networkServer_t server) throws idException {
            if (scan_state == IDLE) {
                return 0;
            }

            idStr serv = new idStr(Sys_NetAdrToString(server.adr));

            if (server.challenge != challenge) {
                common.DPrintf("idServerScan::InfoResponse - ignoring response from %s, wrong challenge %d.", serv.toString(), server.challenge);
                return 0;
            }

            if (scan_state == NET_SCAN) {
                final idKeyValue info = net_info.FindKey(serv.toString());
                if (null == info) {
                    common.DPrintf("idServerScan::InfoResponse NET_SCAN: reply from unknown %s\n", serv.toString());
                    return 0;
                }
                int id = Integer.parseInt(info.GetValue().toString());
                net_info.Delete(serv.toString());
                inServer_t iserv = net_servers.oGet(id);
                server.ping = Sys_Milliseconds() - iserv.time;
                server.id = iserv.id;
            } else {
                server.ping = Sys_Milliseconds() - lan_pingtime;
                server.id = 0;

                // check for duplicate servers
                for (int i = 0; i < Num(); i++) {
//                    if (memcmp((this.oGet(i).adr, server.adr, sizeof(netadr_t)) == 0) {
                    if (!this.oGet(i).adr.equals(server.adr)) {//TODO:override equals?
                        common.DPrintf("idServerScan::InfoResponse LAN_SCAN: duplicate server %s\n", serv.toString());
                        return 1;
                    }
                }
            }

            final String si_map = server.serverInfo.GetString("si_map");
            final idDecl mapDecl = declManager.FindType(DECL_MAPDEF, si_map, false);
            final idDeclEntityDef mapDef = (idDeclEntityDef) (mapDecl);
            if (mapDef != null) {
                final String mapName = common.GetLanguageDict().GetString(mapDef.dict.GetString("name", si_map));
                server.serverInfo.Set("si_mapName", mapName);
            } else {
                server.serverInfo.Set("si_mapName", si_map);
            }

            int index = Append(server);
            // for now, don't maintain sorting when adding new info response servers
            m_sortedServers.Append(Num() - 1);
            if (listGUI.IsConfigured() && !IsFiltered(server)) {
                GUIAdd(Num() - 1, server);
            }
            if (listGUI.GetSelection(null, 0) == (Num() - 1)) {
                GUIUpdateSelected();
            }

            return index;
        }
//
        // add an internet server - ( store a numeric id along with it )

        public void AddServer(int id, final String srv) {
            inServer_t s = new inServer_t();

            incoming_net = true;
            incoming_lastTime = Sys_Milliseconds() + INCOMING_TIMEOUT;
            s.id = id;

            // using IPs, not hosts
            if (!Sys_StringToNetAdr(srv, s.adr, false)) {
                common.DPrintf("idServerScan::AddServer: failed to parse server %s\n", srv);
                return;
            }
            if (0 == s.adr.port) {
                s.adr.port = PORT_SERVER;
            }

            net_servers.Append(s);
        }
//
        // we are going to feed server entries to be pinged
        // if timeout is true, use a timeout once we start AddServer to trigger EndServers and decide the scan is done

        public void StartServers(boolean timeout) {
            incoming_net = true;
            incoming_useTimeout = timeout;
            incoming_lastTime = Sys_Milliseconds() + REFRESH_START;
        }

        // we are done filling up the list of server entries
        public void EndServers() {
            incoming_net = false;
            l_serverScan = this;
            m_sortedServers.Sort(new Cmp());
            ApplyFilter();
        }
//

        // scan the current list of servers - used for refreshes and while receiving a fresh list
        public void NetScan() {
            if (!idAsyncNetwork.client.IsPortInitialized()) {
                // if the port isn't open, initialize it, but wait for a short
                // time to let the OS do whatever magic things it needs to do...
                idAsyncNetwork.client.InitPort();
                // start the scan one second from now...
                scan_state = WAIT_ON_INIT;
                endWaitTime = Sys_Milliseconds() + 1000;
                return;
            }

            // make sure the client port is open
            idAsyncNetwork.client.InitPort();

            scan_state = NET_SCAN;
            challenge++;

            super.Clear();
            m_sortedServers.Clear();
            cur_info = 0;
            net_info.Clear();
            listGUI.Clear();
            GUIUpdateSelected();
            common.DPrintf("NetScan with challenge %d\n", challenge);

            while (cur_info < Min(net_servers.Num(), MAX_PINGREQUESTS)) {
                netadr_t serv = net_servers.oGet(cur_info).adr;
                EmitGetInfo(serv);
                net_servers.oGet(cur_info).time = Sys_Milliseconds();
                net_info.SetInt(Sys_NetAdrToString(serv), cur_info);
                cur_info++;
            }
        }

        // clear
        @Override
        public void Clear() {
            LocalClear();
            super.Clear();
        }

        // called each game frame. Updates the scanner state, takes care of ongoing scans
        public void RunFrame() {
            if (scan_state == IDLE) {
                return;
            }

            if (scan_state == WAIT_ON_INIT) {
                if (Sys_Milliseconds() >= endWaitTime) {
                    scan_state = IDLE;
                    NetScan();
                }
                return;
            }

            int timeout_limit = Sys_Milliseconds() - REPLY_TIMEOUT;

            if (scan_state == LAN_SCAN) {
                if (timeout_limit > lan_pingtime) {
                    common.Printf("Scanned for servers on the LAN\n");
                    scan_state = IDLE;
                }
                return;
            }

            // if scan_state == NET_SCAN
            // check for timeouts
            int i = 0;
            while (i < net_info.GetNumKeyVals()) {
                if (timeout_limit > net_servers.oGet(Integer.parseInt(net_info.GetKeyVal(i).GetValue().toString())).time) {
                    common.DPrintf("timeout %s\n", net_info.GetKeyVal(i).GetKey().toString());
                    net_info.Delete(net_info.GetKeyVal(i).GetKey().toString());
                } else {
                    i++;
                }
            }

            // possibly send more queries
            while (cur_info < net_servers.Num() && net_info.GetNumKeyVals() < MAX_PINGREQUESTS) {
                netadr_t serv = net_servers.oGet(cur_info).adr;
                EmitGetInfo(serv);
                net_servers.oGet(cur_info).time = Sys_Milliseconds();
                net_info.SetInt(Sys_NetAdrToString(serv), cur_info);
                cur_info++;
            }

            // update state
            if ((!incoming_net || (incoming_useTimeout && Sys_Milliseconds() > incoming_lastTime)) && net_info.GetNumKeyVals() == 0) {
                EndServers();
                // the list is complete, we are no longer waiting for any getInfo replies
                common.Printf("Scanned %d servers.\n", cur_info);
                scan_state = IDLE;
            }
        }

        public scan_state_t GetState() {
            return scan_state;
        }

        public void SetState(scan_state_t scan_state) {
            this.scan_state = scan_state;
        }
//	

        public boolean GetBestPing(networkServer_t serv) {
            int i, ic;
            ic = Num();
            if (0 == ic) {
                return false;
            }
            serv = this.oGet(0);
            for (i = 0; i < ic; i++) {
                if (this.oGet(i).ping < serv.ping) {
                    serv = this.oGet(i);
                }
            }
            return true;
        }
//
        // prepare for a LAN scan. idAsyncClient does the network job (UDP broadcast), we do the storage

        public void SetupLANScan() {
            Clear();
            GUIUpdateSelected();
            scan_state = LAN_SCAN;
            challenge++;
            lan_pingtime = Sys_Milliseconds();
            common.DPrintf("SetupLANScan with challenge %d\n", challenge);
        }
//

        public void GUIConfig(idUserInterface pGUI, final String name) {
            m_pGUI = pGUI;
            if (listGUI == null) {
                listGUI = uiManager.AllocListGUI();
            }
            listGUI.Config(pGUI, name);
        }

        // update the GUI fields with information about the currently selected server
        public void GUIUpdateSelected() throws idException {
            String[] screenshot = {null};//new char[MAX_STRING_CHARS];

            if (NOT(m_pGUI)) {
                return;
            }
            int i = listGUI.GetSelection(null, 0);
            if (i == -1 || i >= Num()) {
                m_pGUI.SetStateString("server_name", "");
                m_pGUI.SetStateString("player1", "");
                m_pGUI.SetStateString("player2", "");
                m_pGUI.SetStateString("player3", "");
                m_pGUI.SetStateString("player4", "");
                m_pGUI.SetStateString("player5", "");
                m_pGUI.SetStateString("player6", "");
                m_pGUI.SetStateString("player7", "");
                m_pGUI.SetStateString("player8", "");
                m_pGUI.SetStateString("server_map", "");
                m_pGUI.SetStateString("browser_levelshot", "");
                m_pGUI.SetStateString("server_gameType", "");
                m_pGUI.SetStateString("server_IP", "");
                m_pGUI.SetStateString("server_passworded", "");
            } else {
                m_pGUI.SetStateString("server_name", this.oGet(i).serverInfo.GetString("si_name"));
                for (int j = 0; j < 8; j++) {
                    if (this.oGet(i).clients > j) {
                        m_pGUI.SetStateString(va("player%d", j + 1), ctos(this.oGet(i).nickname[j]));
                    } else {
                        m_pGUI.SetStateString(va("player%d", j + 1), "");
                    }
                }
                m_pGUI.SetStateString("server_map", this.oGet(i).serverInfo.GetString("si_mapName"));
                fileSystem.FindMapScreenshot(this.oGet(i).serverInfo.GetString("si_map"), screenshot, MAX_STRING_CHARS);
                m_pGUI.SetStateString("browser_levelshot", screenshot[0]);
                m_pGUI.SetStateString("server_gameType", this.oGet(i).serverInfo.GetString("si_gameType"));
                m_pGUI.SetStateString("server_IP", Sys_NetAdrToString(this.oGet(i).adr));
                if (this.oGet(i).serverInfo.GetBool("si_usePass")) {
                    m_pGUI.SetStateString("server_passworded", "PASSWORD REQUIRED");
                } else {
                    m_pGUI.SetStateString("server_passworded", "");
                }
            }
        }

        public void Shutdown() {
            m_pGUI = null;
            if (listGUI != null) {
                listGUI.Config(null, null);
                uiManager.FreeListGUI(listGUI);
                listGUI = null;
            }
            screenshot.Clear();
        }

        public void ApplyFilter() throws idException {
            int i;
            networkServer_t serv;
            idStr s;

            listGUI.SetStateChanges(false);
            listGUI.Clear();
            for (i = m_sortAscending ? 0 : m_sortedServers.Num() - 1;
                    m_sortAscending ? i < m_sortedServers.Num() : i >= 0;
                    i += m_sortAscending ? 1 : -1) {
                serv = this.oGet(m_sortedServers.oGet(i));
                if (!IsFiltered(serv)) {
                    GUIAdd(m_sortedServers.oGet(i), serv);
                }
            }
            GUIUpdateSelected();
            listGUI.SetStateChanges(true);
        }

        // there is an internal toggle, call twice with same sort to switch
        public void SetSorting(serverSort_t sort) {
            l_serverScan = this;
            if (sort == m_sort) {
                m_sortAscending = !m_sortAscending;
            } else {
                m_sort = sort;
                m_sortAscending = true; // is the default for any new sort
                m_sortedServers.Sort(new Cmp());
            }
            // trigger a redraw
            ApplyFilter();
        }

        public int GetChallenge() {
            return challenge;
        }

        // we need to clear some internal data as well
        private void LocalClear() {
            scan_state = IDLE;
            incoming_net = false;
            lan_pingtime = -1;
            net_info.Clear();
            net_servers.Clear();
            cur_info = 0;
            if (listGUI != null) {
                listGUI.Clear();
            }
            incoming_useTimeout = false;
            m_sortedServers.Clear();
        }

        private void EmitGetInfo(netadr_t serv) {
            idAsyncNetwork.client.GetServerInfo(serv);
        }

        private void GUIAdd(int id, final networkServer_t server) throws idException {
            String name = server.serverInfo.GetString("si_name", GAME_NAME + " Server");
            boolean d3xp = false;
            boolean mod = false;

            if (0 == idStr.Icmp(server.serverInfo.GetString("fs_game"), "d3xp")
                    || 0 == idStr.Icmp(server.serverInfo.GetString("fs_game_base"), "d3xp")) {
                d3xp = true;
            }
            if (server.serverInfo.GetString("fs_game").charAt(0) != '\0') {
                mod = true;
            }

            name += "\t";
            if (server.serverInfo.GetString("sv_punkbuster").charAt(0) == '1') {
                name += "mtr_PB";
            }

            name += "\t";
            if (d3xp) {
                // FIXME: even for a 'D3XP mod'
                // could have a specific icon for this case
                name += "mtr_doom3XPIcon";
            } else if (mod) {
                name += "mtr_doom3Mod";
            } else {
                name += "mtr_doom3Icon";
            }
            name += "\t";
            name += va("%d/%d\t", server.clients, server.serverInfo.GetInt("si_maxPlayers"));
            name += (server.ping > -1) ? va("%d\t", server.ping) : "na\t";
            name += server.serverInfo.GetString("si_gametype");
            name += "\t";
            name += server.serverInfo.GetString("si_mapName");
            name += "\t";
            listGUI.Add(id, new idStr(name));
        }

        private boolean IsFiltered(final networkServer_t server) throws idException {
            int i;
            idKeyValue keyval;

            // OS support filter
//            if (false) {
//                // filter out pure servers that won't provide checksumed game code for client OS
//                keyval = server.serverInfo.FindKey("si_pure");
//                if (keyval != null && 0 == idStr.Cmp(keyval.GetValue().toString(), "1")) {
//                    if ((server.OSMask & (1 << BUILD_OS_ID)) == 0) {
//                        return true;
//                    }
//                }
//            } else
            {
                if ((server.OSMask & (1 << BUILD_OS_ID)) == 0) {
                    return true;
                }
            }
            // password filter
            keyval = server.serverInfo.FindKey("si_usePass");
            if (keyval != null && gui_filter_password.GetInteger() == 1) {
                // show passworded only
                if (keyval.GetValue().oGet(0) == '0') {
                    return true;
                }
            } else if (keyval != null && gui_filter_password.GetInteger() == 2) {
                // show no password only
                if (keyval.GetValue().oGet(0) != '0') {
                    return true;
                }
            }
            // players filter
            keyval = server.serverInfo.FindKey("si_maxPlayers");
            if (keyval != null) {
                if (gui_filter_players.GetInteger() == 1 && server.clients == Integer.parseInt(keyval.GetValue().toString())) {
                    return true;
                } else if (gui_filter_players.GetInteger() == 2 && (0 == server.clients || server.clients == Integer.parseInt(keyval.GetValue().toString()))) {
                    return true;
                }
            }
            // gametype filter
            keyval = server.serverInfo.FindKey("si_gameType");
            if (keyval != null && gui_filter_gameType.GetInteger() != 0) {
                i = 0;
                while (l_gameTypes[i] != null) {
                    if (0 == keyval.GetValue().Icmp(l_gameTypes[i])) {
                        break;
                    }
                    i++;
                }
                if (l_gameTypes[i] != null && i != gui_filter_gameType.GetInteger() - 1) {
                    return true;
                }
            }
            // idle server filter
            keyval = server.serverInfo.FindKey("si_idleServer");
            if (keyval != null && 0 == gui_filter_idle.GetInteger()) {
                if (0 == keyval.GetValue().Icmp("1")) {
                    return true;
                }
            }

            // autofilter D3XP games if the user does not has the XP installed
            if (!fileSystem.HasD3XP() && 0 == idStr.Icmp(server.serverInfo.GetString("fs_game"), "d3xp")) {
                return true;
            }

            // filter based on the game doom or XP
            if (gui_filter_game.GetInteger() == 1) { //Only Doom
                if (idStr.Icmp(server.serverInfo.GetString("fs_game"), "") != 0) {
                    return true;
                }
            } else if (gui_filter_game.GetInteger() == 2) { //Only D3XP
                if (idStr.Icmp(server.serverInfo.GetString("fs_game"), "d3xp") != 0) {
                    return true;
                }
            }

            return false;
        }

        private static class Cmp implements cmp_t<Integer> {

            @Override
            public int compare(final Integer a, final Integer b) {
                networkServer_t serv1, serv2;
                idStr s1 = new idStr(), s2 = new idStr();
                int ret;

                serv1 = l_serverScan.oGet(a);
                serv2 = l_serverScan.oGet(b);
                switch (l_serverScan.m_sort) {
                    case SORT_PING:
                        ret = serv1.ping < serv2.ping ? -1 : (serv1.ping > serv2.ping ? 1 : 0);
                        return ret;
                    case SORT_SERVERNAME:
                        serv1.serverInfo.GetString("si_name", "", s1);
                        serv2.serverInfo.GetString("si_name", "", s2);
                        return s1.IcmpNoColor(s2);
                    case SORT_PLAYERS:
                        ret = serv1.clients < serv2.clients ? -1 : (serv1.clients > serv2.clients ? 1 : 0);
                        return ret;
                    case SORT_GAMETYPE:
                        serv1.serverInfo.GetString("si_gameType", "", s1);
                        serv2.serverInfo.GetString("si_gameType", "", s2);
                        return s1.Icmp(s2);
                    case SORT_MAP:
                        serv1.serverInfo.GetString("si_mapName", "", s1);
                        serv2.serverInfo.GetString("si_mapName", "", s2);
                        return s1.Icmp(s2);
                    case SORT_GAME:
                        serv1.serverInfo.GetString("fs_game", "", s1);
                        serv2.serverInfo.GetString("fs_game", "", s2);
                        return s1.Icmp(s2);
                }
                return 0;
            }
        };
    };
}
