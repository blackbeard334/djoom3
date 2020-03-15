package neo.Game;

import static neo.Game.GameSys.SysCvar.g_balanceTDM;
import static neo.Game.GameSys.SysCvar.g_voteFlags;
import static neo.Game.GameSys.SysCvar.si_fragLimit;
import static neo.Game.GameSys.SysCvar.si_gameType;
import static neo.Game.GameSys.SysCvar.si_gameTypeArgs;
import static neo.Game.GameSys.SysCvar.si_map;
import static neo.Game.GameSys.SysCvar.si_spectators;
import static neo.Game.GameSys.SysCvar.si_timeLimit;
import static neo.Game.GameSys.SysCvar.ui_skinArgs;
import static neo.Game.Game_local.GAME_RELIABLE_MESSAGE_CALLVOTE;
import static neo.Game.Game_local.GAME_RELIABLE_MESSAGE_CASTVOTE;
import static neo.Game.Game_local.GAME_RELIABLE_MESSAGE_CHAT;
import static neo.Game.Game_local.GAME_RELIABLE_MESSAGE_DB;
import static neo.Game.Game_local.GAME_RELIABLE_MESSAGE_DROPWEAPON;
import static neo.Game.Game_local.GAME_RELIABLE_MESSAGE_RESTART;
import static neo.Game.Game_local.GAME_RELIABLE_MESSAGE_SERVERINFO;
import static neo.Game.Game_local.GAME_RELIABLE_MESSAGE_SOUND_EVENT;
import static neo.Game.Game_local.GAME_RELIABLE_MESSAGE_SOUND_INDEX;
import static neo.Game.Game_local.GAME_RELIABLE_MESSAGE_STARTSTATE;
import static neo.Game.Game_local.GAME_RELIABLE_MESSAGE_STARTVOTE;
import static neo.Game.Game_local.GAME_RELIABLE_MESSAGE_TOURNEYLINE;
import static neo.Game.Game_local.GAME_RELIABLE_MESSAGE_UPDATEVOTE;
import static neo.Game.Game_local.GAME_RELIABLE_MESSAGE_VCHAT;
import static neo.Game.Game_local.GAME_RELIABLE_MESSAGE_WARMUPTIME;
import static neo.Game.Game_local.MAX_CLIENTS;
import static neo.Game.Game_local.MAX_GAME_MESSAGE_SIZE;
import static neo.Game.Game_local.gameLocal;
import static neo.Game.Game_local.gameSoundWorld;
import static neo.Game.Game_local.gameSoundChannel_t.SND_CHANNEL_ANY;
import static neo.Game.MultiplayerGame.gameType_t.GAME_DM;
import static neo.Game.MultiplayerGame.gameType_t.GAME_LASTMAN;
import static neo.Game.MultiplayerGame.gameType_t.GAME_SP;
import static neo.Game.MultiplayerGame.gameType_t.GAME_TDM;
import static neo.Game.MultiplayerGame.gameType_t.GAME_TOURNEY;
import static neo.Game.MultiplayerGame.idMultiplayerGame.gameState_t.COUNTDOWN;
import static neo.Game.MultiplayerGame.idMultiplayerGame.gameState_t.GAMEON;
import static neo.Game.MultiplayerGame.idMultiplayerGame.gameState_t.GAMEREVIEW;
import static neo.Game.MultiplayerGame.idMultiplayerGame.gameState_t.INACTIVE;
import static neo.Game.MultiplayerGame.idMultiplayerGame.gameState_t.NEXTGAME;
import static neo.Game.MultiplayerGame.idMultiplayerGame.gameState_t.SUDDENDEATH;
import static neo.Game.MultiplayerGame.idMultiplayerGame.gameState_t.WARMUP;
import static neo.Game.MultiplayerGame.idMultiplayerGame.msg_evt_t.MSG_DIED;
import static neo.Game.MultiplayerGame.idMultiplayerGame.msg_evt_t.MSG_FORCEREADY;
import static neo.Game.MultiplayerGame.idMultiplayerGame.msg_evt_t.MSG_FRAGLIMIT;
import static neo.Game.MultiplayerGame.idMultiplayerGame.msg_evt_t.MSG_HOLYSHIT;
import static neo.Game.MultiplayerGame.idMultiplayerGame.msg_evt_t.MSG_JOINTEAM;
import static neo.Game.MultiplayerGame.idMultiplayerGame.msg_evt_t.MSG_KILLED;
import static neo.Game.MultiplayerGame.idMultiplayerGame.msg_evt_t.MSG_KILLEDTEAM;
import static neo.Game.MultiplayerGame.idMultiplayerGame.msg_evt_t.MSG_SUDDENDEATH;
import static neo.Game.MultiplayerGame.idMultiplayerGame.msg_evt_t.MSG_SUICIDE;
import static neo.Game.MultiplayerGame.idMultiplayerGame.msg_evt_t.MSG_TELEFRAGGED;
import static neo.Game.MultiplayerGame.idMultiplayerGame.msg_evt_t.MSG_TIMELIMIT;
import static neo.Game.MultiplayerGame.idMultiplayerGame.vote_flags_t.VOTE_COUNT;
import static neo.Game.MultiplayerGame.idMultiplayerGame.vote_flags_t.VOTE_KICK;
import static neo.Game.MultiplayerGame.idMultiplayerGame.vote_flags_t.VOTE_MAP;
import static neo.Game.MultiplayerGame.idMultiplayerGame.vote_flags_t.VOTE_NONE;
import static neo.Game.MultiplayerGame.idMultiplayerGame.vote_flags_t.VOTE_RESTART;
import static neo.Game.MultiplayerGame.idMultiplayerGame.vote_result_t.VOTE_ABORTED;
import static neo.Game.MultiplayerGame.idMultiplayerGame.vote_result_t.VOTE_FAILED;
import static neo.Game.MultiplayerGame.idMultiplayerGame.vote_result_t.VOTE_PASSED;
import static neo.Game.MultiplayerGame.idMultiplayerGame.vote_result_t.VOTE_RESET;
import static neo.Game.MultiplayerGame.idMultiplayerGame.vote_result_t.VOTE_UPDATE;
import static neo.Game.MultiplayerGame.playerVote_t.PLAYER_VOTE_NO;
import static neo.Game.MultiplayerGame.playerVote_t.PLAYER_VOTE_NONE;
import static neo.Game.MultiplayerGame.playerVote_t.PLAYER_VOTE_WAIT;
import static neo.Game.MultiplayerGame.playerVote_t.PLAYER_VOTE_YES;
import static neo.Game.MultiplayerGame.snd_evt_t.SND_COUNT;
import static neo.Game.MultiplayerGame.snd_evt_t.SND_FIGHT;
import static neo.Game.MultiplayerGame.snd_evt_t.SND_ONE;
import static neo.Game.MultiplayerGame.snd_evt_t.SND_SUDDENDEATH;
import static neo.Game.MultiplayerGame.snd_evt_t.SND_THREE;
import static neo.Game.MultiplayerGame.snd_evt_t.SND_TWO;
import static neo.Game.MultiplayerGame.snd_evt_t.SND_VOTE;
import static neo.Game.MultiplayerGame.snd_evt_t.SND_VOTE_FAILED;
import static neo.Game.MultiplayerGame.snd_evt_t.SND_VOTE_PASSED;
import static neo.Game.MultiplayerGame.snd_evt_t.SND_YOULOSE;
import static neo.Game.MultiplayerGame.snd_evt_t.SND_YOUWIN;
import static neo.TempDump.NOT;
import static neo.TempDump.atoi;
import static neo.TempDump.btoi;
import static neo.TempDump.ctos;
import static neo.TempDump.etoi;
import static neo.TempDump.isNotNullOrEmpty;
import static neo.TempDump.itob;
import static neo.framework.Async.NetworkSystem.networkSystem;
import static neo.framework.BuildDefines._DEBUG;
import static neo.framework.BuildDefines.__linux__;
import static neo.framework.CVarSystem.CVAR_ARCHIVE;
import static neo.framework.CVarSystem.CVAR_BOOL;
import static neo.framework.CVarSystem.CVAR_GAME;
import static neo.framework.CVarSystem.cvarSystem;
import static neo.framework.CmdSystem.cmdSystem;
import static neo.framework.CmdSystem.cmdExecution_t.CMD_EXEC_APPEND;
import static neo.framework.CmdSystem.cmdExecution_t.CMD_EXEC_NOW;
import static neo.framework.Common.common;
import static neo.framework.DeclManager.declManager;
import static neo.framework.DeclManager.declType_t.DECL_SOUND;
import static neo.framework.FileSystem_h.fileSystem;
import static neo.idlib.Lib.MAX_STRING_CHARS;
import static neo.idlib.Lib.Min;
import static neo.idlib.Text.Str.va;
import static neo.ui.GameSSDWindow.MAX_POWERUPS;
import static neo.ui.UserInterface.uiManager;

import java.nio.ByteBuffer;
import java.util.stream.Stream;

import neo.TempDump;
import neo.Game.Entity.idEntity;
import neo.Game.Player.idPlayer;
import neo.Sound.snd_shader.idSoundShader;
import neo.framework.CVarSystem.idCVar;
import neo.framework.CmdSystem.cmdFunction_t;
import neo.framework.File_h.idFile;
import neo.idlib.BitMsg.idBitMsg;
import neo.idlib.BitMsg.idBitMsgDelta;
import neo.idlib.CmdArgs.idCmdArgs;
import neo.idlib.Dict_h.idDict;
import neo.idlib.Dict_h.idKeyValue;
import neo.idlib.Text.Str.idStr;
import neo.idlib.math.Math_h.idMath;
import neo.idlib.math.Vector.idVec3;
import neo.ui.ListGUI.idListGUI;
import neo.ui.UserInterface.idUserInterface;

/**
 *
 */
public class MultiplayerGame {

    /*
     ===============================================================================

     Basic DOOM multiplayer

     ===============================================================================
     */
    public enum gameType_t {

        GAME_SP,
        GAME_DM,
        GAME_TOURNEY,
        GAME_TDM,
        GAME_LASTMAN
    };

    public enum playerVote_t {

        PLAYER_VOTE_NONE,
        PLAYER_VOTE_NO,
        PLAYER_VOTE_YES,
        PLAYER_VOTE_WAIT	// mark a player allowed to vote
    };

    public static class mpPlayerState_s {

        int ping;			// player ping
        int fragCount;		// kills
        int teamFragCount;	// team kills
        int wins;			// wins
        playerVote_t vote;			// player's vote
        boolean scoreBoardUp;	// toggle based on player scoreboard button, used to activate de-activate the scoreboard gui
        boolean ingame;
    };
//    
    public static final int NUM_CHAT_NOTIFY = 5;
    public static final int CHAT_FADE_TIME = 400;
    public static final int FRAGLIMIT_DELAY = 2000;
//
    public static final int MP_PLAYER_MINFRAGS = -100;
    public static final int MP_PLAYER_MAXFRAGS = 100;
    public static final int MP_PLAYER_MAXWINS = 100;
    public static final int MP_PLAYER_MAXPING = 999;
//

    public static class mpChatLine_s {

        idStr line;
        short fade;			// starts high and decreases, line is removed once reached 0

        public mpChatLine_s() {
            line  = new idStr();
        }
    };

    public enum snd_evt_t {

        SND_YOUWIN,//= 0,
        SND_YOULOSE,
        SND_FIGHT,
        SND_VOTE,
        SND_VOTE_PASSED,
        SND_VOTE_FAILED,
        SND_THREE,
        SND_TWO,
        SND_ONE,
        SND_SUDDENDEATH,
        SND_COUNT
    };
    // could be a problem if players manage to go down sudden deaths till this .. oh well
    public static final int LASTMAN_NOLIVES = -20;
//
    public static final idCVar g_spectatorChat = new idCVar("g_spectatorChat", "0", CVAR_GAME | CVAR_ARCHIVE | CVAR_BOOL, "let spectators talk to everyone during game");
//

    public static class idMultiplayerGame {

        // state vars
        private gameState_t gameState;		// what state the current game is in
        private gameState_t nextState;		// state to switch to when nextStateSwitch is hit
        private int pingUpdateTime;             // time to update ping
        //
        private mpPlayerState_s[] playerState = new mpPlayerState_s[MAX_CLIENTS];
        //
        // keep track of clients which are willingly in spectator mode
        //
        // vote vars
        private vote_flags_t vote;		// active vote or VOTE_NONE
        private int voteTimeOut;                // when the current vote expires
        private int voteExecTime;               // delay between vote passed msg and execute
        private float yesVotes;                 // counter for yes votes
        private float noVotes;                  // and for no votes
        private idStr voteValue;		// the data voted upon ( server )
        private idStr voteString;		// the vote string ( client )
        private boolean voted;			// hide vote box ( client )
        private int[] kickVoteMap = new int[MAX_CLIENTS];
        //
        // time related
        private int nextStateSwitch;            // time next state switch
        private int warmupEndTime;              // warmup till..
        private int matchStartedTime;		// time current match started
        //
        // tourney
        private int[] currentTourneyPlayer = new int[2];// our current set of players
        private int lastWinner;				// plays again
        //
        // warmup
        private idStr warmupText;		// text shown in warmup area of screen
        private boolean one, two, three;	// keeps count down voice from repeating
        //
        // guis
        private idUserInterface scoreBoard;	// scoreboard
        private idUserInterface spectateGui;	// spectate info
        private idUserInterface guiChat;	// chat text
        private idUserInterface mainGui;	// ready / nick / votes etc.
        private idListGUI mapList;
        private idUserInterface msgmodeGui;	// message mode
        private int currentMenu;		// 0 - none, 1 - mainGui, 2 - msgmodeGui
        private int nextMenu;			// if 0, will do mainGui
        private boolean bCurrentMenuMsg;	// send menu state updates to server
        //
        // chat data
        private mpChatLine_s[] chatHistory = TempDump.allocArray(mpChatLine_s.class, NUM_CHAT_NOTIFY);
        private int chatHistoryIndex;
        private int chatHistorySize;		// 0 <= x < NUM_CHAT_NOTIFY
        private boolean chatDataUpdated;
        private int lastChatLineTime;
        //
        // rankings are used by UpdateScoreboard and UpdateHud
        private int numRankedPlayers;		// ranked players, others may be empty slots or spectators
        private idPlayer[] rankedPlayers = new idPlayer[MAX_CLIENTS];
        //
        private boolean pureReady;		// defaults to false, set to true once server game is running with pure checksums
        private int fragLimitTimeout;
        //
        private int[] switchThrottle = new int[3];
        private int voiceChatThrottle;
        //
        private gameType_t lastGameType;	// for restarts
        private int startFragLimit;		// synchronize to clients in initial state, set on -> GAMEON
        //
        //

        public idMultiplayerGame() {
            scoreBoard = null;
            spectateGui = null;
            guiChat = null;
            mainGui = null;
            mapList = null;
            msgmodeGui = null;
            lastGameType = GAME_SP;
            Clear();
        }

        public void Shutdown() {
            Clear();
        }

        // resets everything and prepares for a match
        public void Reset() {
            Clear();
            assert (null == scoreBoard && null == spectateGui && null == guiChat && null == mainGui && null == mapList);
            scoreBoard = uiManager.FindGui("guis/scoreboard.gui", true, false, true);
            spectateGui = uiManager.FindGui("guis/spectate.gui", true, false, true);
            guiChat = uiManager.FindGui("guis/chat.gui", true, false, true);
            mainGui = uiManager.FindGui("guis/mpmain.gui", true, false, true);
            mapList = uiManager.AllocListGUI();
            mapList.Config(mainGui, "mapList");
            // set this GUI so that our Draw function is still called when it becomes the active/fullscreen GUI
            mainGui.SetStateBool("gameDraw", true);
            mainGui.SetKeyBindingNames();
            mainGui.SetStateInt("com_machineSpec", cvarSystem.GetCVarInteger("com_machineSpec"));
            SetMenuSkin();
            msgmodeGui = uiManager.FindGui("guis/mpmsgmode.gui", true, false, true);
            msgmodeGui.SetStateBool("gameDraw", true);
            ClearGuis();
            ClearChatData();
            warmupEndTime = 0;
        }

        // setup local data for a new player
        public void SpawnPlayer(int clientNum) {

            boolean ingame = playerState[ clientNum].ingame;

            playerState = Stream.generate(mpPlayerState_s::new).limit(playerState.length).toArray(mpPlayerState_s[]::new);
            if (!gameLocal.isClient) {
                idPlayer p = (idPlayer) gameLocal.entities[ clientNum];
                p.spawnedTime = gameLocal.time;
                if (gameLocal.gameType == GAME_TDM) {
                    SwitchToTeam(clientNum, -1, p.team);
                }
                p.tourneyRank = 0;
                if (gameLocal.gameType == GAME_TOURNEY && gameState == GAMEON) {
                    p.tourneyRank++;
                }
                playerState[ clientNum].ingame = ingame;
            }
        }

        // checks rules and updates state of the mp game
        public void Run() {
            int i, timeLeft;
            idPlayer player;
            int gameReviewPause;

            assert (gameLocal.isMultiplayer);
            assert (!gameLocal.isClient);

            pureReady = true;

            if (gameState == INACTIVE) {
                lastGameType = gameLocal.gameType;
                NewState(WARMUP);
            }

            CheckVote();

            CheckRespawns();

            if (nextState != INACTIVE && gameLocal.time > nextStateSwitch) {
                NewState(nextState);
                nextState = INACTIVE;
            }

            // don't update the ping every frame to save bandwidth
            if (gameLocal.time > pingUpdateTime) {
                for (i = 0; i < gameLocal.numClients; i++) {
                    playerState[i].ping = networkSystem.ServerGetClientPing(i);
                }
                pingUpdateTime = gameLocal.time + 1000;
            }

            warmupText.oSet("");

            switch (gameState) {
                case GAMEREVIEW: {
                    if (nextState == INACTIVE) {
                        gameReviewPause = cvarSystem.GetCVarInteger("g_gameReviewPause");
                        nextState = NEXTGAME;
                        nextStateSwitch = gameLocal.time + 1000 * gameReviewPause;
                    }
                    break;
                }
                case NEXTGAME: {
                    if (nextState == INACTIVE) {
                        // game rotation, new map, gametype etc.
                        if (gameLocal.NextMap()) {
                            cmdSystem.BufferCommandText(CMD_EXEC_APPEND, "serverMapRestart\n");
                            return;
                        }
                        NewState(WARMUP);
                        if (gameLocal.gameType == GAME_TOURNEY) {
                            CycleTourneyPlayers();
                        }
                        // put everyone back in from endgame spectate
                        for (i = 0; i < gameLocal.numClients; i++) {
                            idEntity ent = gameLocal.entities[ i];
                            if (ent != null && ent.IsType(idPlayer.class)) {
                                if (!((idPlayer) ent).wantSpectate) {
                                    CheckRespawns((idPlayer) ent);
                                }
                            }
                        }
                    }
                    break;
                }
                case WARMUP: {
                    if (AllPlayersReady()) {
                        NewState(COUNTDOWN);
                        nextState = GAMEON;
                        nextStateSwitch = gameLocal.time + 1000 * cvarSystem.GetCVarInteger("g_countDown");
                    }
                    warmupText.oSet("Warming up.. waiting for players to get ready");
                    one = two = three = false;
                    break;
                }
                case COUNTDOWN: {
                    timeLeft = (nextStateSwitch - gameLocal.time) / 1000 + 1;
                    if (timeLeft == 3 && !three) {
                        PlayGlobalSound(-1, SND_THREE);
                        three = true;
                    } else if (timeLeft == 2 && !two) {
                        PlayGlobalSound(-1, SND_TWO);
                        two = true;
                    } else if (timeLeft == 1 && !one) {
                        PlayGlobalSound(-1, SND_ONE);
                        one = true;
                    }
                    warmupText.oSet(va("Match starts in %d", timeLeft));
                    break;
                }
                case GAMEON: {
                    player = FragLimitHit();
                    if (player != null) {
                        // delay between detecting frag limit and ending game. let the death anims play
                        if (0 == fragLimitTimeout) {
                            common.DPrintf("enter FragLimit timeout, player %d is leader\n", player.entityNumber);
                            fragLimitTimeout = gameLocal.time + FRAGLIMIT_DELAY;
                        }
                        if (gameLocal.time > fragLimitTimeout) {
                            NewState(GAMEREVIEW, player);
                            PrintMessageEvent(-1, MSG_FRAGLIMIT, player.entityNumber);
                        }
                    } else {
                        if (fragLimitTimeout != 0) {
                            // frag limit was hit and cancelled. means the two teams got even during FRAGLIMIT_DELAY
                            // enter sudden death, the next frag leader will win
                            SuddenRespawn();
                            PrintMessageEvent(-1, MSG_HOLYSHIT);
                            fragLimitTimeout = 0;
                            NewState(SUDDENDEATH);
                        } else if (TimeLimitHit()) {
                            player = FragLeader();
                            if (null == player) {
                                NewState(SUDDENDEATH);
                            } else {
                                NewState(GAMEREVIEW, player);
                                PrintMessageEvent(-1, MSG_TIMELIMIT);
                            }
                        }
                    }
                    break;
                }
                case SUDDENDEATH: {
                    player = FragLeader();
                    if (player != null) {
                        if (0 == fragLimitTimeout) {
                            common.DPrintf("enter sudden death FragLeader timeout, player %d is leader\n", player.entityNumber);
                            fragLimitTimeout = gameLocal.time + FRAGLIMIT_DELAY;
                        }
                        if (gameLocal.time > fragLimitTimeout) {
                            NewState(GAMEREVIEW, player);
                            PrintMessageEvent(-1, MSG_FRAGLIMIT, player.entityNumber);
                        }
                    } else if (fragLimitTimeout != 0) {
                        SuddenRespawn();
                        PrintMessageEvent(-1, MSG_HOLYSHIT);
                        fragLimitTimeout = 0;
                    }
                    break;
                }
			default:
				// TODO check unused Enum case labels
				break;
            }
        }

        // draws mp hud, scoredboard, etc.. 
        public boolean Draw(int clientNum) {
            idPlayer player, viewPlayer;

            // clear the render entities for any players that don't need
            // icons and which might not be thinking because they weren't in
            // the last snapshot.
            for (int i = 0; i < gameLocal.numClients; i++) {
                player = (idPlayer) gameLocal.entities[ i];
                if (player != null && !player.NeedsIcon()) {
                    player.HidePlayerIcons();
                }
            }

            player = viewPlayer = (idPlayer) gameLocal.entities[ clientNum];

            if (player == null) {
                return false;
            }

            if (player.spectating) {
                viewPlayer = (idPlayer) gameLocal.entities[ player.spectator];
                if (viewPlayer == null) {
                    return false;
                }
            }

            UpdatePlayerRanks();
            UpdateHud(viewPlayer, player.hud);
            // use the hud of the local player
            viewPlayer.playerView.RenderPlayerView(player.hud);

            if (currentMenu != 0) {
// if (false){
                // // uncomment this if you want to track when players are in a menu
                // if ( !bCurrentMenuMsg ) {
                // idBitMsg	outMsg;
                // byte		msgBuf[ 128 ];

                // outMsg.Init( msgBuf, sizeof( msgBuf ) );
                // outMsg.WriteByte( GAME_RELIABLE_MESSAGE_MENU );
                // outMsg.WriteBits( 1, 1 );
                // networkSystem.ClientSendReliableMessage( outMsg );
                // bCurrentMenuMsg = true;
                // }
// }
                if (player.wantSpectate) {
                    mainGui.SetStateString("spectext", common.GetLanguageDict().GetString("#str_04249"));
                } else {
                    mainGui.SetStateString("spectext", common.GetLanguageDict().GetString("#str_04250"));
                }
                DrawChat();
                if (currentMenu == 1) {
                    UpdateMainGui();
                    mainGui.Redraw(gameLocal.time);
                } else {
                    msgmodeGui.Redraw(gameLocal.time);
                }
            } else {
// if (false){
                // // uncomment this if you want to track when players are in a menu
                // if ( bCurrentMenuMsg ) {
                // idBitMsg	outMsg;
                // byte		msgBuf[ 128 ];

                // outMsg.Init( msgBuf, sizeof( msgBuf ) );
                // outMsg.WriteByte( GAME_RELIABLE_MESSAGE_MENU );
                // outMsg.WriteBits( 0, 1 );
                // networkSystem.ClientSendReliableMessage( outMsg );
                // bCurrentMenuMsg = false;
                // }
// }
                if (player.spectating) {
                    String[] spectatetext = new String[2];
                    int ispecline = 0;
                    if (gameLocal.gameType == GAME_TOURNEY) {
                        if (!player.wantSpectate) {
                            spectatetext[ 0] = common.GetLanguageDict().GetString("#str_04246");
                            switch (player.tourneyLine) {
                                case 0:
                                    spectatetext[ 0] += common.GetLanguageDict().GetString("#str_07003");
                                    break;
                                case 1:
                                    spectatetext[ 0] += common.GetLanguageDict().GetString("#str_07004");
                                    break;
                                case 2:
                                    spectatetext[ 0] += common.GetLanguageDict().GetString("#str_07005");
                                    break;
                                default:
                                    spectatetext[ 0] += va(common.GetLanguageDict().GetString("#str_07006"), player.tourneyLine);
                                    break;
                            }
                            ispecline++;
                        }
                    } else if (gameLocal.gameType == GAME_LASTMAN) {
                        if (!player.wantSpectate) {
                            spectatetext[ 0] = common.GetLanguageDict().GetString("#str_07007");
                            ispecline++;
                        }
                    }
                    if (player.spectator != player.entityNumber) {
                        spectatetext[ ispecline] = va(common.GetLanguageDict().GetString("#str_07008"), viewPlayer.GetUserInfo().GetString("ui_name"));
                    } else if (0 == ispecline) {
                        spectatetext[ 0] = common.GetLanguageDict().GetString("#str_04246");
                    }
                    spectateGui.SetStateString("spectatetext0", spectatetext[0]);
                    spectateGui.SetStateString("spectatetext1", spectatetext[1]);
                    if (vote != VOTE_NONE) {
                        spectateGui.SetStateString("vote", va("%s (y: %d n: %d)", voteString, (int) yesVotes, (int) noVotes));
                    } else {
                        spectateGui.SetStateString("vote", "");
                    }
                    spectateGui.Redraw(gameLocal.time);
                }
                DrawChat();
                DrawScoreBoard(player);
            }

            return true;
        }

        // updates a player vote
        public void PlayerVote(int clientNum, playerVote_t vote) {
            playerState[ clientNum].vote = vote;
        }

        // updates frag counts and potentially ends the match in sudden death
        public void PlayerDeath(idPlayer dead, idPlayer killer, boolean telefrag) {

            // don't do PrintMessageEvent and shit
            assert (!gameLocal.isClient);

            if (killer != null) {
                if (gameLocal.gameType == GAME_LASTMAN) {
                    playerState[ dead.entityNumber].fragCount--;
                } else if (gameLocal.gameType == GAME_TDM) {
                    if (killer == dead || killer.team == dead.team) {
                        // suicide or teamkill
                        TeamScore(killer.entityNumber, killer.team, -1);
                    } else {
                        TeamScore(killer.entityNumber, killer.team, +1);
                    }
                } else {
                    playerState[ killer.entityNumber].fragCount += (killer == dead) ? -1 : 1;
                }
            }

            if (killer != null && killer.equals(dead)) {
                PrintMessageEvent(-1, MSG_SUICIDE, dead.entityNumber);
            } else if (killer != null) {
                if (telefrag) {
                    PrintMessageEvent(-1, MSG_TELEFRAGGED, dead.entityNumber, killer.entityNumber);
                } else if (gameLocal.gameType == GAME_TDM && dead.team == killer.team) {
                    PrintMessageEvent(-1, MSG_KILLEDTEAM, dead.entityNumber, killer.entityNumber);
                } else {
                    PrintMessageEvent(-1, MSG_KILLED, dead.entityNumber, killer.entityNumber);
                }
            } else {
                PrintMessageEvent(-1, MSG_DIED, dead.entityNumber);
                playerState[ dead.entityNumber].fragCount--;
            }
        }

        public void AddChatLine(final String fmt, Object... objects) {//id_attribute((format(printf,2,3)));
            idStr temp;
//            va_list argptr;
//
//            va_start(argptr, fmt);
//            vsprintf(temp, fmt, argptr);
//            va_end(argptr);
            temp = new idStr(String.format(fmt, objects));

            gameLocal.Printf("%s\n", temp.toString());

            chatHistory[ chatHistoryIndex % NUM_CHAT_NOTIFY].line = temp;
            chatHistory[ chatHistoryIndex % NUM_CHAT_NOTIFY].fade = 6;

            chatHistoryIndex++;
            if (chatHistorySize < NUM_CHAT_NOTIFY) {
                chatHistorySize++;
            }
            chatDataUpdated = true;
            lastChatLineTime = gameLocal.time;
        }

        public void UpdateMainGui() {
            int i;
            mainGui.SetStateInt("readyon", gameState == WARMUP ? 1 : 0);
            mainGui.SetStateInt("readyoff", gameState != WARMUP ? 1 : 0);
//	idStr strReady = cvarSystem.GetCVarString( "ui_ready" );
            String strReady = cvarSystem.GetCVarString("ui_ready");
            if (strReady.equals("ready")) {
                strReady = common.GetLanguageDict().GetString("#str_04248");
            } else {
                strReady = common.GetLanguageDict().GetString("#str_04247");
            }
            mainGui.SetStateString("ui_ready", strReady);
            mainGui.SetStateInt("teamon", gameLocal.gameType == GAME_TDM ? 1 : 0);
            mainGui.SetStateInt("teamoff", gameLocal.gameType != GAME_TDM ? 1 : 0);
            if (gameLocal.gameType == GAME_TDM) {
                idPlayer p = gameLocal.GetClientByNum(gameLocal.localClientNum);
                mainGui.SetStateInt("team", p.team);
            }
            // setup vote
            mainGui.SetStateInt("voteon", (vote != VOTE_NONE && !voted) ? 1 : 0);
            mainGui.SetStateInt("voteoff", (vote != VOTE_NONE && !voted) ? 0 : 1);
            // last man hack
            mainGui.SetStateInt("isLastMan", gameLocal.gameType == GAME_LASTMAN ? 1 : 0);
            // send the current serverinfo values
            for (i = 0; i < gameLocal.serverInfo.GetNumKeyVals(); i++) {
                final idKeyValue keyval = gameLocal.serverInfo.GetKeyVal(i);
                mainGui.SetStateString(keyval.GetKey().toString(), keyval.GetValue().toString());
            }
            mainGui.StateChanged(gameLocal.time);
            if (__linux__) {
                // replacing the oh-so-useful s_reverse with sound backend prompt
                mainGui.SetStateString("driver_prompt", "1");
            } else {
                mainGui.SetStateString("driver_prompt", "0");
            }
        }

        public idUserInterface StartMenu() {

            if (mainGui == null) {
                return null;
            }

            int i, j;
            if (currentMenu != 0) {
                currentMenu = 0;
                cvarSystem.SetCVarBool("ui_chat", false);
            } else {
                if (nextMenu >= 2) {
                    currentMenu = nextMenu;
                } else {
                    // for default and explicit
                    currentMenu = 1;
                }
                cvarSystem.SetCVarBool("ui_chat", true);
            }
            nextMenu = 0;
            gameLocal.sessionCommand.oSet("");	// in case we used "game_startMenu" to trigger the menu
            if (currentMenu == 1) {
                UpdateMainGui();

                // UpdateMainGui sets most things, but it doesn't set these because
                // it'd be pointless and/or harmful to set them every frame (for various reasons)
                // Currenty the gui doesn't update properly if they change anyway, so we'll leave it like this.
                // setup callvote
                if (vote == VOTE_NONE) {
                    boolean callvote_ok = false;
                    for (i = 0; i < VOTE_COUNT.ordinal(); i++) {
                        // flag on means vote is denied, so default value 0 means all votes and -1 disables
                        mainGui.SetStateInt(va("vote%d", i), itob(g_voteFlags.GetInteger() & (1 << i)) ? 0 : 1);
                        if (NOT(g_voteFlags.GetInteger() & (1 << i))) {
                            callvote_ok = true;
                        }
                    }
                    mainGui.SetStateInt("callvote", btoi(callvote_ok));
                } else {
                    mainGui.SetStateInt("callvote", 2);
                }

                // player kick data
                String kickList = "";
                j = 0;
                for (i = 0; i < gameLocal.numClients; i++) {
                    if (gameLocal.entities[i] != null && gameLocal.entities[i].IsType(idPlayer.class)) {
                        if (!kickList.isEmpty()) {
                            kickList += ";";
                        }
                        kickList += va("\"%d - %s\"", i, gameLocal.userInfo[i].GetString("ui_name"));
                        kickVoteMap[ j] = i;
                        j++;
                    }
                }
                mainGui.SetStateString("kickChoices", kickList);

                mainGui.SetStateString("chattext", "");
                mainGui.Activate(true, gameLocal.time);
                return mainGui;
            } else if (currentMenu == 2) {
                // the setup is done in MessageMode
                msgmodeGui.Activate(true, gameLocal.time);
                cvarSystem.SetCVarBool("ui_chat", true);
                return msgmodeGui;
            }
            return null;
        }

        public String HandleGuiCommands(final String _menuCommand) {
            idUserInterface currentGui;
            String voteValue;
            int vote_clientNum;
            int icmd;
            idCmdArgs args = new idCmdArgs();

            if (_menuCommand.isEmpty()) {
                common.Printf("idMultiplayerGame::HandleGuiCommands: empty command\n");
                return "continue";
            }
            assert (currentMenu != 0);
            if (currentMenu == 1) {
                currentGui = mainGui;
            } else {
                currentGui = msgmodeGui;
            }

            args.TokenizeString(_menuCommand, false);

            for (icmd = 0; icmd < args.Argc();) {
                final String cmd = args.Argv(icmd++);

                if (0 == idStr.Icmp(cmd, ";")) {
                    continue;
                } else if (0 == idStr.Icmp(cmd, "video")) {
                    String vcmd = "";
                    if (args.Argc() - icmd >= 1) {
                        vcmd = args.Argv(icmd++);
                    }

                    int oldSpec = cvarSystem.GetCVarInteger("com_machineSpec");

                    if (idStr.Icmp(vcmd, "low") == 0) {
                        cvarSystem.SetCVarInteger("com_machineSpec", 0);
                    } else if (idStr.Icmp(vcmd, "medium") == 0) {
                        cvarSystem.SetCVarInteger("com_machineSpec", 1);
                    } else if (idStr.Icmp(vcmd, "high") == 0) {
                        cvarSystem.SetCVarInteger("com_machineSpec", 2);
                    } else if (idStr.Icmp(vcmd, "ultra") == 0) {
                        cvarSystem.SetCVarInteger("com_machineSpec", 3);
                    } else if (idStr.Icmp(vcmd, "recommended") == 0) {
                        cmdSystem.BufferCommandText(CMD_EXEC_NOW, "setMachineSpec\n");
                    }

                    if (oldSpec != cvarSystem.GetCVarInteger("com_machineSpec")) {
                        currentGui.SetStateInt("com_machineSpec", cvarSystem.GetCVarInteger("com_machineSpec"));
                        currentGui.StateChanged(gameLocal.realClientTime);
                        cmdSystem.BufferCommandText(CMD_EXEC_NOW, "execMachineSpec\n");
                    }

                    if (idStr.Icmp(vcmd, "restart") == 0) {
                        cmdSystem.BufferCommandText(CMD_EXEC_APPEND, "vid_restart\n");
                    }

                    continue;
                } else if (0 == idStr.Icmp(cmd, "play")) {
                    if (args.Argc() - icmd >= 1) {
                        String snd = args.Argv(icmd++);
                        int channel = 1;
                        if (snd.length() == 1) {
                            channel = Integer.parseInt(snd);
                            snd = args.Argv(icmd++);
                        }
                        gameSoundWorld.PlayShaderDirectly(snd, channel);
                    }
                    continue;
                } else if (0 == idStr.Icmp(cmd, "mpSkin")) {
                    String skin;
                    if (args.Argc() - icmd >= 1) {
                        skin = args.Argv(icmd++);
                        cvarSystem.SetCVarString("ui_skin", skin);
                    }
                    SetMenuSkin();
                    continue;
                } else if (0 == idStr.Icmp(cmd, "quit")) {
                    cmdSystem.BufferCommandText(CMD_EXEC_APPEND, "quit\n");
                    return null;
                } else if (0 == idStr.Icmp(cmd, "disconnect")) {
                    cmdSystem.BufferCommandText(CMD_EXEC_APPEND, "disconnect\n");
                    return null;
                } else if (0 == idStr.Icmp(cmd, "close")) {
                    DisableMenu();
                    return null;
                } else if (0 == idStr.Icmp(cmd, "spectate")) {
                    ToggleSpectate();
                    DisableMenu();
                    return null;
                } else if (0 == idStr.Icmp(cmd, "chatmessage")) {
                    int mode = currentGui.State().GetInt("messagemode");
                    if (mode != 0) {
                        cmdSystem.BufferCommandText(CMD_EXEC_NOW, va("sayTeam \"%s\"", currentGui.State().GetString("chattext")));
                    } else {
                        cmdSystem.BufferCommandText(CMD_EXEC_NOW, va("say \"%s\"", currentGui.State().GetString("chattext")));
                    }
                    currentGui.SetStateString("chattext", "");
                    if (currentMenu == 1) {
                        return "continue";
                    } else {
                        DisableMenu();
                        return null;
                    }
                } else if (0 == idStr.Icmp(cmd, "readytoggle")) {
                    ToggleReady();
                    DisableMenu();
                    return null;
                } else if (0 == idStr.Icmp(cmd, "teamtoggle")) {
                    ToggleTeam();
                    DisableMenu();
                    return null;
                } else if (0 == idStr.Icmp(cmd, "callVote")) {
                    vote_flags_t voteIndex = vote_flags_t.values()[mainGui.State().GetInt("voteIndex")];
                    if (voteIndex == VOTE_MAP) {
                        int mapNum = mapList.GetSelection(null, 0);
                        if (mapNum >= 0) {
                            final idDict dict = fileSystem.GetMapDecl(mapNum);
                            if (dict != null) {
                                ClientCallVote(VOTE_MAP, dict.GetString("path"));
                            }
                        }
                    } else {
                        voteValue = mainGui.State().GetString("str_voteValue");
                        if (voteIndex == VOTE_KICK) {
                            vote_clientNum = kickVoteMap[ Integer.parseInt(voteValue)];
                            ClientCallVote(voteIndex, va("%d", vote_clientNum));
                        } else {
                            ClientCallVote(voteIndex, voteValue);
                        }
                    }
                    DisableMenu();
                    return null;
                } else if (0 == idStr.Icmp(cmd, "voteyes")) {
                    CastVote(gameLocal.localClientNum, true);
                    DisableMenu();
                    return null;
                } else if (0 == idStr.Icmp(cmd, "voteno")) {
                    CastVote(gameLocal.localClientNum, false);
                    DisableMenu();
                    return null;
                } else if (0 == idStr.Icmp(cmd, "bind")) {
                    if (args.Argc() - icmd >= 2) {
                        String key = args.Argv(icmd++);
                        String bind = args.Argv(icmd++);
                        cmdSystem.BufferCommandText(CMD_EXEC_NOW, va("bindunbindtwo \"%s\" \"%s\"", key, bind));
                        mainGui.SetKeyBindingNames();
                    }
                    continue;
                } else if (0 == idStr.Icmp(cmd, "clearbind")) {
                    if (args.Argc() - icmd >= 1) {
                        String bind = args.Argv(icmd++);
                        cmdSystem.BufferCommandText(CMD_EXEC_NOW, va("unbind \"%s\"", bind));
                        mainGui.SetKeyBindingNames();
                    }
                    continue;
                } else if (0 == idStr.Icmp(cmd, "MAPScan")) {
                    final String gametype = gameLocal.serverInfo.GetString("si_gameType");
                    if (gametype == null || !gametype.isEmpty() || idStr.Icmp(gametype, "singleplayer") == 0) {
//                        gametype = "Deathmatch";
                        gameLocal.serverInfo.Set("si_gameType", "Deathmatch");//TODO:double check that this actually works.
                    }

                    int i, num;
                    String si_map = gameLocal.serverInfo.GetString("si_map");
                    idDict dict;

                    mapList.Clear();
                    mapList.SetSelection(-1);
                    num = fileSystem.GetNumMaps();
                    for (i = 0; i < num; i++) {
                        dict = fileSystem.GetMapDecl(i);
                        if (dict != null) {
                            // any MP gametype supported
                            boolean isMP = false;
                            int igt = GAME_SP.ordinal() + 1;
                            while (si_gameTypeArgs[igt] != null) {
                                if (dict.GetBool(si_gameTypeArgs[ igt])) {
                                    isMP = true;
                                    break;
                                }
                                igt++;
                            }
                            if (isMP) {
                                String mapName = dict.GetString("name");
                                if (isNotNullOrEmpty(mapName)) {
                                    mapName = dict.GetString("path");
                                }
                                mapName = common.GetLanguageDict().GetString(mapName);
                                mapList.Add(i, new idStr(mapName));
                                if (si_map.equals(dict.GetString("path"))) {
                                    mapList.SetSelection(mapList.Num() - 1);
                                }
                            }
                        }
                    }
                    // set the current level shot
                    SetMapShot();
                    return "continue";
                } else if (0 == idStr.Icmp(cmd, "click_maplist")) {
                    SetMapShot();
                    return "continue";
                } else if (cmd.startsWith("sound")) {
                    // pass that back to the core, will know what to do with it
                    return _menuCommand;
                }
                common.Printf("idMultiplayerGame::HandleGuiCommands: '%s'	unknown\n", cmd);

            }
            return "continue";
        }

        public void SetMenuSkin() {
            // skins
            String str = cvarSystem.GetCVarString("mod_validSkins");
            String uiSkin = cvarSystem.GetCVarString("ui_skin");
            String skin;
            int skinId = 1;
            int count = 1;
            while (str.length() != 0) {
                int n = str.indexOf(";");
                if (n >= 0) {
                    skin = str.substring(0, n);
                    str = str.substring(/*str.length() - n - 1*/ n + 1);
                } else {
                    skin = str;
                    str = "";
                }
                if (skin.equals(uiSkin)) {
                    skinId = count;
                }
                count++;
            }

            for (int i = 0; i < count; i++) {
                mainGui.SetStateInt(va("skin%d", i + 1), 0);
            }
            mainGui.SetStateInt(va("skin%d", skinId), 1);
        }
        public static final int ASYNC_PLAYER_FRAG_BITS = -idMath.BitsForInteger(MP_PLAYER_MAXFRAGS - MP_PLAYER_MINFRAGS);	// player can have negative frags
        public static final int ASYNC_PLAYER_WINS_BITS = idMath.BitsForInteger(MP_PLAYER_MAXWINS);
        public static final int ASYNC_PLAYER_PING_BITS = idMath.BitsForInteger(MP_PLAYER_MAXPING);

        public void WriteToSnapshot(idBitMsgDelta msg) {
            int i;
            int value;

            msg.WriteByte(etoi(gameState));
            msg.WriteShort(currentTourneyPlayer[ 0]);
            msg.WriteShort(currentTourneyPlayer[ 1]);
            for (i = 0; i < MAX_CLIENTS; i++) {
                // clamp all values to min/max possible value that we can send over
                value = idMath.ClampInt(MP_PLAYER_MINFRAGS, MP_PLAYER_MAXFRAGS, playerState[i].fragCount);
                msg.WriteBits(value, ASYNC_PLAYER_FRAG_BITS);
                value = idMath.ClampInt(MP_PLAYER_MINFRAGS, MP_PLAYER_MAXFRAGS, playerState[i].teamFragCount);
                msg.WriteBits(value, ASYNC_PLAYER_FRAG_BITS);
                value = idMath.ClampInt(0, MP_PLAYER_MAXWINS, playerState[i].wins);
                msg.WriteBits(value, ASYNC_PLAYER_WINS_BITS);
                value = idMath.ClampInt(0, MP_PLAYER_MAXPING, playerState[i].ping);
                msg.WriteBits(value, ASYNC_PLAYER_PING_BITS);
                msg.WriteBits(btoi(playerState[i].ingame), 1);
            }
        }

        public void ReadFromSnapshot(final idBitMsgDelta msg) {
            int i;
            final gameState_t newState;

            newState = gameState_t.values()[msg.ReadByte()];
            if (newState != gameState) {
                gameLocal.DPrintf("%s . %s\n", GameStateStrings[ gameState.ordinal()], GameStateStrings[ newState.ordinal()]);
                gameState = newState;
                // these could be gathered in a BGNewState() kind of thing, as we have to do them in NewState as well
                if (gameState == GAMEON) {
                    matchStartedTime = gameLocal.time;
                    cvarSystem.SetCVarString("ui_ready", "Not Ready");
                    switchThrottle[ 1] = 0;	// passby the throttle
                    startFragLimit = gameLocal.serverInfo.GetInt("si_fragLimit");
                }
            }
            currentTourneyPlayer[ 0] = msg.ReadShort();
            currentTourneyPlayer[ 1] = msg.ReadShort();
            for (i = 0; i < MAX_CLIENTS; i++) {
                playerState[i].fragCount = msg.ReadBits(ASYNC_PLAYER_FRAG_BITS);
                playerState[i].teamFragCount = msg.ReadBits(ASYNC_PLAYER_FRAG_BITS);
                playerState[i].wins = msg.ReadBits(ASYNC_PLAYER_WINS_BITS);
                playerState[i].ping = msg.ReadBits(ASYNC_PLAYER_PING_BITS);
                playerState[i].ingame = msg.ReadBits(1) != 0;
            }
        }

        // game state
        public enum gameState_t {

            INACTIVE,//= 0,						// not running
            WARMUP, // warming up
            COUNTDOWN, // post warmup pre-game
            GAMEON, // game is on
            SUDDENDEATH, // game is on but in sudden death, first frag wins
            GAMEREVIEW, // game is over, scoreboard is up. we wait si_gameReviewPause seconds (which has a min value)
            NEXTGAME,
            STATE_COUNT
        };
        // handy verbose
        public static final String[] GameStateStrings = {//new String[STATE_COUNT];
            "INACTIVE",
            "WARMUP",
            "COUNTDOWN",
            "GAMEON",
            "SUDDENDEATH",
            "GAMEREVIEW",
            "NEXTGAME"
        };

        public gameState_t GetGameState() {
            return gameState;
        }
        // global sounds transmitted by index - 0 .. SND_COUNT
        // sounds in this list get precached on MP start
        public static final String[] GlobalSoundStrings = {//new String[ SND_COUNT ];
            "sound/feedback/voc_youwin.wav",
            "sound/feedback/voc_youlose.wav",
            "sound/feedback/fight.wav",
            "sound/feedback/vote_now.wav",
            "sound/feedback/vote_passed.wav",
            "sound/feedback/vote_failed.wav",
            "sound/feedback/three.wav",
            "sound/feedback/two.wav",
            "sound/feedback/one.wav",
            "sound/feedback/sudden_death.wav",};

        public void PlayGlobalSound(int to, snd_evt_t evt, final String shader /*= NULL*/) {
            idSoundShader shaderDecl;

            if (to == -1 || to == gameLocal.localClientNum) {
                if (shader != null) {
                    gameSoundWorld.PlayShaderDirectly(shader);
                } else {
                    gameSoundWorld.PlayShaderDirectly(GlobalSoundStrings[etoi(evt)]);
                }
            }

            if (!gameLocal.isClient) {
                idBitMsg outMsg = new idBitMsg();
                ByteBuffer msgBuf = ByteBuffer.allocate(1024);
                outMsg.Init(msgBuf, msgBuf.capacity());

                if (shader != null) {
                    shaderDecl = declManager.FindSound(shader);
                    if (NOT(shaderDecl)) {
                        return;
                    }
                    outMsg.WriteByte(GAME_RELIABLE_MESSAGE_SOUND_INDEX);
                    outMsg.WriteLong(gameLocal.ServerRemapDecl(to, DECL_SOUND, shaderDecl.Index()));
                } else {
                    outMsg.WriteByte(GAME_RELIABLE_MESSAGE_SOUND_EVENT);
                    outMsg.WriteByte(etoi(evt));
                }

                networkSystem.ServerSendReliableMessage(to, outMsg);
            }
        }

        public void PlayGlobalSound(int to, snd_evt_t evt) {
            PlayGlobalSound(to, evt, null);
        }

        // more compact than a chat line
        public enum msg_evt_t {

            MSG_SUICIDE,// = 0,
            MSG_KILLED,
            MSG_KILLEDTEAM,
            MSG_DIED,
            MSG_VOTE,
            MSG_VOTEPASSED,
            MSG_VOTEFAILED,
            MSG_SUDDENDEATH,
            MSG_FORCEREADY,
            MSG_JOINEDSPEC,
            MSG_TIMELIMIT,
            MSG_FRAGLIMIT,
            MSG_TELEFRAGGED,
            MSG_JOINTEAM,
            MSG_HOLYSHIT,
            MSG_COUNT
        };

        public void PrintMessageEvent(int to, msg_evt_t evt, int parm1 /*= -1*/, int parm2 /*= -1*/) {
            switch (evt) {
                case MSG_SUICIDE:
                    assert (parm1 >= 0);
                    AddChatLine(common.GetLanguageDict().GetString("#str_04293"), gameLocal.userInfo[ parm1].GetString("ui_name"));
                    break;
                case MSG_KILLED:
                    assert (parm1 >= 0 && parm2 >= 0);
                    AddChatLine(common.GetLanguageDict().GetString("#str_04292"), gameLocal.userInfo[ parm1].GetString("ui_name"), gameLocal.userInfo[ parm2].GetString("ui_name"));
                    break;
                case MSG_KILLEDTEAM:
                    assert (parm1 >= 0 && parm2 >= 0);
                    AddChatLine(common.GetLanguageDict().GetString("#str_04291"), gameLocal.userInfo[ parm1].GetString("ui_name"), gameLocal.userInfo[ parm2].GetString("ui_name"));
                    break;
                case MSG_TELEFRAGGED:
                    assert (parm1 >= 0 && parm2 >= 0);
                    AddChatLine(common.GetLanguageDict().GetString("#str_04290"), gameLocal.userInfo[ parm1].GetString("ui_name"), gameLocal.userInfo[ parm2].GetString("ui_name"));
                    break;
                case MSG_DIED:
                    assert (parm1 >= 0);
                    AddChatLine(common.GetLanguageDict().GetString("#str_04289"), gameLocal.userInfo[ parm1].GetString("ui_name"));
                    break;
                case MSG_VOTE:
                    AddChatLine(common.GetLanguageDict().GetString("#str_04288"));
                    break;
                case MSG_SUDDENDEATH:
                    AddChatLine(common.GetLanguageDict().GetString("#str_04287"));
                    break;
                case MSG_FORCEREADY:
                    AddChatLine(common.GetLanguageDict().GetString("#str_04286"), gameLocal.userInfo[ parm1].GetString("ui_name"));
                    if (gameLocal.entities[ parm1] != null && gameLocal.entities[ parm1].IsType(idPlayer.class)) {
                        ((idPlayer) gameLocal.entities[parm1]).forcedReady = true;
                    }
                    break;
                case MSG_JOINEDSPEC:
                    AddChatLine(common.GetLanguageDict().GetString("#str_04285"), gameLocal.userInfo[ parm1].GetString("ui_name"));
                    break;
                case MSG_TIMELIMIT:
                    AddChatLine(common.GetLanguageDict().GetString("#str_04284"));
                    break;
                case MSG_FRAGLIMIT:
                    if (gameLocal.gameType == GAME_LASTMAN) {
                        AddChatLine(common.GetLanguageDict().GetString("#str_04283"), gameLocal.userInfo[ parm1].GetString("ui_name"));
                    } else if (gameLocal.gameType == GAME_TDM) {
                        AddChatLine(common.GetLanguageDict().GetString("#str_04282"), gameLocal.userInfo[ parm1].GetString("ui_team"));
                    } else {
                        AddChatLine(common.GetLanguageDict().GetString("#str_04281"), gameLocal.userInfo[ parm1].GetString("ui_name"));
                    }
                    break;
                case MSG_JOINTEAM:
                    AddChatLine(common.GetLanguageDict().GetString("#str_04280"), gameLocal.userInfo[ parm1].GetString("ui_name"), parm2 != 0 ? common.GetLanguageDict().GetString("#str_02500") : common.GetLanguageDict().GetString("#str_02499"));
                    break;
                case MSG_HOLYSHIT:
                    AddChatLine(common.GetLanguageDict().GetString("#str_06732"));
                    break;
                default:
                    gameLocal.DPrintf("PrintMessageEvent: unknown message type %d\n", evt);
                    return;
            }
            if (!gameLocal.isClient) {
                idBitMsg outMsg = new idBitMsg();
                ByteBuffer msgBuf = ByteBuffer.allocate(1024);
                outMsg.Init(msgBuf, msgBuf.capacity());
                outMsg.WriteByte(GAME_RELIABLE_MESSAGE_DB);
                outMsg.WriteByte(etoi(evt));
                outMsg.WriteByte(parm1);
                outMsg.WriteByte(parm2);
                networkSystem.ServerSendReliableMessage(to, outMsg);
            }
        }

        public void PrintMessageEvent(int to, msg_evt_t evt, int parm1 /*= -1*/) {
            PrintMessageEvent(to, evt, parm1, -1);
        }

        public void PrintMessageEvent(int to, msg_evt_t evt) {
            PrintMessageEvent(to, evt, -1);
        }

        public void DisconnectClient(int clientNum) {
            if (lastWinner == clientNum) {
                lastWinner = -1;
            }
            UpdatePlayerRanks();
            CheckAbortGame();
        }

        public static class ForceReady_f extends cmdFunction_t {

            private static final cmdFunction_t instance = new ForceReady_f();

            private ForceReady_f() {
            }

            public static cmdFunction_t getInstance() {
                return instance;
            }

            @Override
            public void run(idCmdArgs args) {
                if (!gameLocal.isMultiplayer || gameLocal.isClient) {
                    common.Printf("forceReady: multiplayer server only\n");
                    return;
                }
                gameLocal.mpGame.ForceReady();
            }
        };

        public static class DropWeapon_f extends cmdFunction_t {

            private static final cmdFunction_t instance = new DropWeapon_f();

            private DropWeapon_f() {
            }

            public static cmdFunction_t getInstance() {
                return instance;
            }

            @Override
            public void run(idCmdArgs args) {
                if (!gameLocal.isMultiplayer) {
                    common.Printf("clientDropWeapon: only valid in multiplayer\n");
                    return;
                }
                idBitMsg outMsg = new idBitMsg();
                ByteBuffer msgBuf = ByteBuffer.allocate(128);
                outMsg.Init(msgBuf, msgBuf.capacity());
                outMsg.WriteByte(GAME_RELIABLE_MESSAGE_DROPWEAPON);
                networkSystem.ClientSendReliableMessage(outMsg);
            }
        };

        public static class MessageMode_f extends cmdFunction_t {

            private static final cmdFunction_t instance = new MessageMode_f();

            private MessageMode_f() {
            }

            public static cmdFunction_t getInstance() {
                return instance;
            }

            @Override
            public void run(idCmdArgs args) {
                gameLocal.mpGame.MessageMode(args);
            }
        };

        public static class VoiceChat_f extends cmdFunction_t {

            private static final cmdFunction_t instance = new VoiceChat_f();

            private VoiceChat_f() {
            }

            public static cmdFunction_t getInstance() {
                return instance;
            }

            @Override
            public void run(idCmdArgs args) {
                gameLocal.mpGame.VoiceChat(args, false);
            }
        };

        public static class VoiceChatTeam_f extends cmdFunction_t {

            private static final cmdFunction_t instance = new VoiceChatTeam_f();

            private VoiceChatTeam_f() {
            }

            public static cmdFunction_t getInstance() {
                return instance;
            }

            @Override
            public void run(idCmdArgs args) {
                gameLocal.mpGame.VoiceChat(args, true);
            }
        };

        public enum vote_flags_t {

            VOTE_RESTART,//= 0,
            VOTE_TIMELIMIT,
            VOTE_FRAGLIMIT,
            VOTE_GAMETYPE,
            VOTE_KICK,
            VOTE_MAP,
            VOTE_SPECTATORS,
            VOTE_NEXTMAP,
            VOTE_COUNT,
            VOTE_NONE
        };

        public enum vote_result_t {

            VOTE_UPDATE,
            VOTE_FAILED,
            VOTE_PASSED, // passed, but no reset yet
            VOTE_ABORTED,
            VOTE_RESET		// tell clients to reset vote state
        };

        /*
         ================
         idMultiplayerGame::Vote_f
         FIXME: voting from console
         ================
         */
        public static void Vote_f(final idCmdArgs args) {
        }

        /*
         ================
         idMultiplayerGame::CallVote_f
         FIXME: voting from console
         ================
         */
        public static void CallVote_f(final idCmdArgs args) {
        }

        public void ClientCallVote(vote_flags_t voteIndex, final String voteValue) {
            idBitMsg outMsg = new idBitMsg();
            ByteBuffer msgBuf = ByteBuffer.allocate(MAX_GAME_MESSAGE_SIZE);

            // send 
            outMsg.Init(msgBuf, MAX_GAME_MESSAGE_SIZE);
            outMsg.WriteByte(GAME_RELIABLE_MESSAGE_CALLVOTE);
            outMsg.WriteByte(etoi(voteIndex));
            outMsg.WriteString(voteValue);
            networkSystem.ClientSendReliableMessage(outMsg);
        }

        public void ServerCallVote(int clientNum, final idBitMsg msg) {
            vote_flags_t voteIndex;
            long vote_timeLimit, vote_fragLimit, vote_clientNum, vote_gameTypeIndex; //, vote_kickIndex;
            String value;
            char[] value2 = new char[MAX_STRING_CHARS];

            assert (clientNum != -1);
            assert (!gameLocal.isClient);

            voteIndex = vote_flags_t.values()[msg.ReadByte()];
            msg.ReadString(value2, MAX_STRING_CHARS);

            value = ctos(value2);

            // sanity checks - setup the vote
            if (vote != VOTE_NONE) {
                gameLocal.ServerSendChatMessage(clientNum, "server", common.GetLanguageDict().GetString("#str_04273"));
                common.DPrintf("client %d: called vote while voting already in progress - ignored\n", clientNum);
                return;
            }
            switch (voteIndex) {
                case VOTE_RESTART:
                    ServerStartVote(clientNum, voteIndex, "");
                    ClientStartVote(clientNum, common.GetLanguageDict().GetString("#str_04271"));
                    break;
                case VOTE_NEXTMAP:
                    ServerStartVote(clientNum, voteIndex, "");
                    ClientStartVote(clientNum, common.GetLanguageDict().GetString("#str_04272"));
                    break;
                case VOTE_TIMELIMIT:
                    vote_timeLimit = Long.parseLong(value, 10);
                    if (vote_timeLimit == gameLocal.serverInfo.GetInt("si_timeLimit")) {
                        gameLocal.ServerSendChatMessage(clientNum, "server", common.GetLanguageDict().GetString("#str_04270"));
                        common.DPrintf("client %d: already at the voted Time Limit\n", clientNum);
                        return;
                    }
                    if (vote_timeLimit < si_timeLimit.GetMinValue() || vote_timeLimit > si_timeLimit.GetMaxValue()) {
                        gameLocal.ServerSendChatMessage(clientNum, "server", common.GetLanguageDict().GetString("#str_04269"));
                        common.DPrintf("client %d: timelimit value out of range for vote: %s\n", clientNum, value);
                        return;
                    }
                    ServerStartVote(clientNum, voteIndex, value);
                    ClientStartVote(clientNum, va(common.GetLanguageDict().GetString("#str_04268"), vote_timeLimit));
                    break;
                case VOTE_FRAGLIMIT:
                    vote_fragLimit = Long.parseLong(value, 10);
                    if (vote_fragLimit == gameLocal.serverInfo.GetInt("si_fragLimit")) {
                        gameLocal.ServerSendChatMessage(clientNum, "server", common.GetLanguageDict().GetString("#str_04267"));
                        common.DPrintf("client %d: already at the voted Frag Limit\n", clientNum);
                        return;
                    }
                    if (vote_fragLimit < si_fragLimit.GetMinValue() || vote_fragLimit > si_fragLimit.GetMaxValue()) {
                        gameLocal.ServerSendChatMessage(clientNum, "server", common.GetLanguageDict().GetString("#str_04266"));
                        common.DPrintf("client %d: fraglimit value out of range for vote: %s\n", clientNum, value);
                        return;
                    }
                    ServerStartVote(clientNum, voteIndex, value);
                    ClientStartVote(clientNum, va(common.GetLanguageDict().GetString("#str_04303"), gameLocal.gameType == GAME_LASTMAN ? common.GetLanguageDict().GetString("#str_04264") : common.GetLanguageDict().GetString("#str_04265"), vote_fragLimit));
                    break;
                case VOTE_GAMETYPE:
                    vote_gameTypeIndex = Long.parseLong(value, 10);
                    assert (vote_gameTypeIndex >= 0 && vote_gameTypeIndex <= 3);
                    switch ((int) vote_gameTypeIndex) {
                        case 0:
                            value = "Deathmatch";
                            break;
                        case 1:
                            value = "Tourney";
                            break;
                        case 2:
                            value = "Team DM";
                            break;
                        case 3:
                            value = "Last Man";
                            break;
                    }
                    if (NOT(idStr.Icmp(value, gameLocal.serverInfo.GetString("si_gameType")))) {
                        gameLocal.ServerSendChatMessage(clientNum, "server", common.GetLanguageDict().GetString("#str_04259"));
                        common.DPrintf("client %d: already at the voted Game Type\n", clientNum);
                        return;
                    }
                    ServerStartVote(clientNum, voteIndex, value);
                    ClientStartVote(clientNum, va(common.GetLanguageDict().GetString("#str_04258"), value));
                    break;
                case VOTE_KICK:
                    vote_clientNum = Long.parseLong(value, 10);
                    if (vote_clientNum == gameLocal.localClientNum) {
                        gameLocal.ServerSendChatMessage(clientNum, "server", common.GetLanguageDict().GetString("#str_04257"));
                        common.DPrintf("client %d: called kick for the server host\n", clientNum);
                        return;
                    }
                    ServerStartVote(clientNum, voteIndex, va("%d", vote_clientNum));
                    ClientStartVote(clientNum, va(common.GetLanguageDict().GetString("#str_04302"), vote_clientNum, gameLocal.userInfo[(int) vote_clientNum].GetString("ui_name")));
                    break;
                case VOTE_MAP: {
                    if (idStr.FindText(gameLocal.serverInfo.GetString("si_map"), value) != -1) {
                        gameLocal.ServerSendChatMessage(clientNum, "server", va(common.GetLanguageDict().GetString("#str_04295"), value));
                        common.DPrintf("client %d: already running the voted map: %s\n", clientNum, value);
                        return;
                    }
                    int num = fileSystem.GetNumMaps();
                    int i;
                    idDict dict = null;
                    boolean haveMap = false;
                    for (i = 0; i < num; i++) {
                        dict = fileSystem.GetMapDecl(i);
                        if (dict != null && NOT(idStr.Icmp(dict.GetString("path"), value))) {
                            haveMap = true;
                            break;
                        }
                    }
                    if (!haveMap) {
                        gameLocal.ServerSendChatMessage(clientNum, "server", va(common.GetLanguageDict().GetString("#str_04296"), value));
                        common.Printf("client %d: map not found: %s\n", clientNum, value);
                        return;
                    }
                    ServerStartVote(clientNum, voteIndex, value);
                    ClientStartVote(clientNum, va(common.GetLanguageDict().GetString("#str_04256"), common.GetLanguageDict().GetString(dict != null ? dict.GetString("name") : value)));
                    break;
                }
                case VOTE_SPECTATORS:
                    if (gameLocal.serverInfo.GetBool("si_spectators")) {
                        ServerStartVote(clientNum, voteIndex, "");
                        ClientStartVote(clientNum, common.GetLanguageDict().GetString("#str_04255"));
                    } else {
                        ServerStartVote(clientNum, voteIndex, "");
                        ClientStartVote(clientNum, common.GetLanguageDict().GetString("#str_04254"));
                    }
                    break;
                default:
                    gameLocal.ServerSendChatMessage(clientNum, "server", va(common.GetLanguageDict().GetString("#str_04297"), voteIndex.ordinal()));
                    common.DPrintf("client %d: unknown vote index %d\n", clientNum, voteIndex);
            }
        }

        public void ClientStartVote(int clientNum, final String _voteString) {
            idBitMsg outMsg = new idBitMsg();
            ByteBuffer msgBuf;

            if (!gameLocal.isClient) {
                msgBuf = ByteBuffer.allocate(MAX_GAME_MESSAGE_SIZE);
                outMsg.Init(msgBuf, MAX_GAME_MESSAGE_SIZE);
                outMsg.WriteByte(GAME_RELIABLE_MESSAGE_STARTVOTE);
                outMsg.WriteByte(clientNum);
                outMsg.WriteString(_voteString);
                networkSystem.ServerSendReliableMessage(-1, outMsg);
            }

            voteString.oSet(_voteString);
            AddChatLine(va(common.GetLanguageDict().GetString("#str_04279"), gameLocal.userInfo[ clientNum].GetString("ui_name")));
            gameSoundWorld.PlayShaderDirectly(GlobalSoundStrings[etoi(SND_VOTE)]);
            if (clientNum == gameLocal.localClientNum) {
                voted = true;
            } else {
                voted = false;
            }
            if (gameLocal.isClient) {
                // the the vote value to something so the vote line is displayed
                vote = VOTE_RESTART;
                yesVotes = 1;
                noVotes = 0;
            }
        }

        public void ServerStartVote(int clientNum, vote_flags_t voteIndex, final String voteValue) {
            int i;

            assert (vote == VOTE_NONE);

            // setup
            yesVotes = 1;
            noVotes = 0;
            vote = voteIndex;
            this.voteValue.oSet(voteValue);
            voteTimeOut = gameLocal.time + 20000;
            // mark players allowed to vote - only current ingame players, players joining during vote will be ignored
            for (i = 0; i < gameLocal.numClients; i++) {
                if (gameLocal.entities[ i] != null && gameLocal.entities[ i].IsType(idPlayer.class)) {
                    playerState[ i].vote = (i == clientNum) ? PLAYER_VOTE_YES : PLAYER_VOTE_WAIT;
                } else {
                    playerState[i].vote = PLAYER_VOTE_NONE;
                }
            }
        }

        public void ClientUpdateVote(vote_result_t status, int yesCount, int noCount) {
            idBitMsg outMsg = new idBitMsg();
            ByteBuffer msgBuf = ByteBuffer.allocate(MAX_GAME_MESSAGE_SIZE);

            if (!gameLocal.isClient) {
                outMsg.Init(msgBuf, MAX_GAME_MESSAGE_SIZE);
                outMsg.WriteByte(GAME_RELIABLE_MESSAGE_UPDATEVOTE);
                outMsg.WriteByte(etoi(status));
                outMsg.WriteByte(yesCount);
                outMsg.WriteByte(noCount);
                networkSystem.ServerSendReliableMessage(-1, outMsg);
            }

            if (vote == VOTE_NONE) {
                // clients coming in late don't get the vote start and are not allowed to vote
                return;
            }

            switch (status) {
                case VOTE_FAILED:
                    AddChatLine(common.GetLanguageDict().GetString("#str_04278"));
                    gameSoundWorld.PlayShaderDirectly(GlobalSoundStrings[ etoi(SND_VOTE_FAILED)]);
                    if (gameLocal.isClient) {
                        vote = VOTE_NONE;
                    }
                    break;
                case VOTE_PASSED:
                    AddChatLine(common.GetLanguageDict().GetString("#str_04277"));
                    gameSoundWorld.PlayShaderDirectly(GlobalSoundStrings[ etoi(SND_VOTE_PASSED)]);
                    break;
                case VOTE_RESET:
                    if (gameLocal.isClient) {
                        vote = VOTE_NONE;
                    }
                    break;
                case VOTE_ABORTED:
                    AddChatLine(common.GetLanguageDict().GetString("#str_04276"));
                    if (gameLocal.isClient) {
                        vote = VOTE_NONE;
                    }
                    break;
                default:
                    break;
            }
            if (gameLocal.isClient) {
                yesVotes = yesCount;
                noVotes = noCount;
            }
        }

        public void CastVote(int clientNum, boolean castVote) {
            idBitMsg outMsg = new idBitMsg();
            ByteBuffer msgBuf = ByteBuffer.allocate(128);

            if (clientNum == gameLocal.localClientNum) {
                voted = true;
            }

            if (gameLocal.isClient) {
                outMsg.Init(msgBuf, msgBuf.capacity());
                outMsg.WriteByte(GAME_RELIABLE_MESSAGE_CASTVOTE);
                outMsg.WriteByte(btoi(castVote));
                networkSystem.ClientSendReliableMessage(outMsg);
                return;
            }

            // sanity
            if (vote == VOTE_NONE) {
                gameLocal.ServerSendChatMessage(clientNum, "server", common.GetLanguageDict().GetString("#str_04275"));
                common.DPrintf("client %d: cast vote while no vote in progress\n", clientNum);
                return;
            }
            if (playerState[ clientNum].vote != PLAYER_VOTE_WAIT) {
                gameLocal.ServerSendChatMessage(clientNum, "server", common.GetLanguageDict().GetString("#str_04274"));
                common.DPrintf("client %d: cast vote - vote %d != PLAYER_VOTE_WAIT\n", clientNum, playerState[ clientNum].vote);
                return;
            }

            if (castVote) {
                playerState[ clientNum].vote = PLAYER_VOTE_YES;
                yesVotes++;
            } else {
                playerState[ clientNum].vote = PLAYER_VOTE_NO;
                noVotes++;
            }

            ClientUpdateVote(VOTE_UPDATE, (int) yesVotes, (int) noVotes);
        }

        /*
         ================
         idMultiplayerGame::ExecuteVote
         the votes are checked for validity/relevance before they are started
         we assume that they are still legit when reaching here
         ================
         */
        public void ExecuteVote() {
            boolean needRestart;
            switch (vote) {
                case VOTE_RESTART:
                    gameLocal.MapRestart();
                    break;
                case VOTE_TIMELIMIT:
                    si_timeLimit.SetInteger(atoi(voteValue));
                    needRestart = gameLocal.NeedRestart();
                    cmdSystem.BufferCommandText(CMD_EXEC_NOW, "rescanSI");
                    if (needRestart) {
                        cmdSystem.BufferCommandText(CMD_EXEC_APPEND, "nextMap");
                    }
                    break;
                case VOTE_FRAGLIMIT:
                    si_fragLimit.SetInteger(atoi(voteValue));
                    needRestart = gameLocal.NeedRestart();
                    cmdSystem.BufferCommandText(CMD_EXEC_NOW, "rescanSI");
                    if (needRestart) {
                        cmdSystem.BufferCommandText(CMD_EXEC_APPEND, "nextMap");
                    }
                    break;
                case VOTE_GAMETYPE:
                    si_gameType.SetString(voteValue.toString());
                    gameLocal.MapRestart();
                    break;
                case VOTE_KICK:
                    cmdSystem.BufferCommandText(CMD_EXEC_NOW, va("kick %s", voteValue));
                    break;
                case VOTE_MAP:
                    si_map.SetString(voteValue.toString());
                    gameLocal.MapRestart();
                    break;
                case VOTE_SPECTATORS:
                    si_spectators.SetBool(!si_spectators.GetBool());
                    needRestart = gameLocal.NeedRestart();
                    cmdSystem.BufferCommandText(CMD_EXEC_NOW, "rescanSI");
                    if (needRestart) {
                        cmdSystem.BufferCommandText(CMD_EXEC_APPEND, "nextMap");
                    }
                    break;
                case VOTE_NEXTMAP:
                    cmdSystem.BufferCommandText(CMD_EXEC_APPEND, "serverNextMap\n");
                    break;
			default:
				break;
            }
        }

        public void WantKilled(int clientNum) {
            idEntity ent = gameLocal.entities[ clientNum];
            if (ent != null && ent.IsType(idPlayer.class)) {
                ((idPlayer) ent).Kill(false, false);
            }
        }

        public int NumActualClients(boolean countSpectators, int[] teamcounts /*= NULL*/) {
            idPlayer p;
            int c = 0;

            if (teamcounts != null) {
                teamcounts[ 0] = teamcounts[ 1] = 0;
            }
            for (int i = 0; i < gameLocal.numClients; i++) {
                idEntity ent = gameLocal.entities[ i];
                if (null == ent || !ent.IsType(idPlayer.class)) {
                    continue;
                }
                p = (idPlayer) ent;
                if (countSpectators || CanPlay(p)) {
                    c++;
                }
                if (teamcounts != null && CanPlay(p)) {
                    teamcounts[ p.team]++;
                }
            }
            return c;
        }

        public void DropWeapon(int clientNum) {
            assert (!gameLocal.isClient);
            idEntity ent = gameLocal.entities[ clientNum];
            if (null == ent || !ent.IsType(idPlayer.class)) {
                return;
            }
            ((idPlayer) ent).DropWeapon(false);
        }

        public void MapRestart() {
            int clientNum;

            assert (!gameLocal.isClient);
            if (gameState != WARMUP) {
                NewState(WARMUP);
                nextState = INACTIVE;
                nextStateSwitch = 0;
            }
            if (g_balanceTDM.GetBool() && lastGameType != GAME_TDM && gameLocal.gameType == GAME_TDM) {
                for (clientNum = 0; clientNum < gameLocal.numClients; clientNum++) {
                    if (gameLocal.entities[ clientNum] != null && gameLocal.entities[ clientNum].IsType(idPlayer.class)) {
                        if (((idPlayer) gameLocal.entities[ clientNum]).BalanceTDM()) {
                            // core is in charge of syncing down userinfo changes
                            // it will also call back game through SetUserInfo with the current info for update
                            cmdSystem.BufferCommandText(CMD_EXEC_NOW, va("updateUI %d\n", clientNum));
                        }
                    }
                }
            }
            lastGameType = gameLocal.gameType;
        }
        // called by idPlayer whenever it detects a team change (init or switch)

        public void SwitchToTeam(int clientNum, int oldteam, int newteam) {
            idEntity ent;
            int i;

            assert (gameLocal.gameType == GAME_TDM);
            assert (oldteam != newteam);
            assert (!gameLocal.isClient);

            if (!gameLocal.isClient && newteam >= 0 && IsInGame(clientNum)) {
                PrintMessageEvent(-1, MSG_JOINTEAM, clientNum, newteam);
            }
            // assign the right teamFragCount
            for (i = 0; i < gameLocal.numClients; i++) {
                if (i == clientNum) {
                    continue;
                }
                ent = gameLocal.entities[ i];
                if (ent != null && ent.IsType(idPlayer.class) && ((idPlayer) ent).team == newteam) {
                    playerState[ clientNum].teamFragCount = playerState[ i].teamFragCount;
                    break;
                }
            }
            if (i == gameLocal.numClients) {
                // alone on this team
                playerState[ clientNum].teamFragCount = 0;
            }
            if (gameState == GAMEON && oldteam != -1) {
                // when changing teams during game, kill and respawn
                idPlayer p = (idPlayer) gameLocal.entities[ clientNum];
                if (p.IsInTeleport()) {
                    p.ServerSendEvent(idPlayer.EVENT_ABORT_TELEPORTER, null, false, -1);
                    p.SetPrivateCameraView(null);
                }
                p.Kill(true, true);
                CheckAbortGame();
            }
        }

        public boolean IsPureReady() {
            return pureReady;
        }

        public void ProcessChatMessage(int clientNum, boolean team, final String name, final String text, final String sound) {
            idBitMsg outMsg = new idBitMsg();
            ByteBuffer msgBuf = ByteBuffer.allocate(256);
            String prefix = null;
            int send_to; // 0 - all, 1 - specs, 2 - team
            int i;
            idEntity ent;
            idPlayer p;
            String prefixed_name;

            assert (!gameLocal.isClient);

            if (clientNum >= 0) {
                p = (idPlayer) gameLocal.entities[ clientNum];
                if (!(p != null && p.IsType(idPlayer.class))) {
                    return;
                }

                if (p.spectating) {
                    prefix = "spectating";
                    if (team || (!g_spectatorChat.GetBool() && (gameState == GAMEON || gameState == SUDDENDEATH))) {
                        // to specs
                        send_to = 1;
                    } else {
                        // to all
                        send_to = 0;
                    }
                } else if (team) {
                    prefix = "team";
                    // to team
                    send_to = 2;
                } else {
                    // to all
                    send_to = 0;
                }
            } else {
                p = null;
                send_to = 0;
            }
            // put the message together
            outMsg.Init(msgBuf, msgBuf.capacity());
            outMsg.WriteByte(GAME_RELIABLE_MESSAGE_CHAT);
            if (prefix != null) {//TODO:&& !isEmptu
                prefixed_name = va("(%s) %s", prefix, name);
            } else {
                prefixed_name = name;
            }
            outMsg.WriteString(prefixed_name);
            outMsg.WriteString(text, -1, false);
            if (0 == send_to) {
                AddChatLine("%s^0: %s\n", prefixed_name, text);
                networkSystem.ServerSendReliableMessage(-1, outMsg);
                if (sound != null) {
                    PlayGlobalSound(-1, SND_COUNT, sound);
                }
            } else {
                for (i = 0; i < gameLocal.numClients; i++) {
                    ent = gameLocal.entities[ i];
                    if (null == ent || !ent.IsType(idPlayer.class)) {
                        continue;
                    }
                    if (send_to == 1 && ((idPlayer) ent).spectating) {
                        if (sound != null) {
                            PlayGlobalSound(i, SND_COUNT, sound);
                        }
                        if (i == gameLocal.localClientNum) {
                            AddChatLine("%s^0: %s\n", prefixed_name, text);
                        } else {
                            networkSystem.ServerSendReliableMessage(i, outMsg);
                        }
                    } else if (send_to == 2 && ((idPlayer) ent).team == p.team) {
                        if (sound != null) {
                            PlayGlobalSound(i, SND_COUNT, sound);
                        }
                        if (i == gameLocal.localClientNum) {
                            AddChatLine("%s^0: %s\n", prefixed_name, text);
                        } else {
                            networkSystem.ServerSendReliableMessage(i, outMsg);
                        }
                    }
                }
            }
        }

        public void ProcessVoiceChat(int clientNum, boolean team, int index) {
            idDict spawnArgs;
            idKeyValue keyval;
            String name;
            idStr snd_key;
            String text_key;
            idPlayer p;

            p = (idPlayer) gameLocal.entities[ clientNum];
            if (!(p != null && p.IsType(idPlayer.class))) {
                return;
            }

            if (p.spectating) {
                return;
            }

            // lookup the sound def
            spawnArgs = gameLocal.FindEntityDefDict("player_doommarine", false);
            keyval = spawnArgs.MatchPrefix("snd_voc_", null);
            while (index > 0 && keyval != null) {
                keyval = spawnArgs.MatchPrefix("snd_voc_", keyval);
                index--;
            }
            if (null == keyval) {
                common.DPrintf("ProcessVoiceChat: unknown chat index %d\n", index);
                return;
            }
            snd_key = keyval.GetKey();
            name = gameLocal.userInfo[ clientNum].GetString("ui_name");
            text_key = String.format("txt_%s", snd_key.Right(snd_key.Length() - 4).toString());
            if (team || gameState == COUNTDOWN || gameState == GAMEREVIEW) {
                ProcessChatMessage(clientNum, team, name, spawnArgs.GetString(text_key), spawnArgs.GetString(snd_key.toString()));
            } else {
                p.StartSound(snd_key.toString(), SND_CHANNEL_ANY, 0, true, null);
                ProcessChatMessage(clientNum, team, name, spawnArgs.GetString(text_key), null);
            }
        }

        public void Precache() {
            int i;
            idFile f;

            if (!gameLocal.isMultiplayer) {
                return;
            }
            gameLocal.FindEntityDefDict("player_doommarine", false);;

            // skins
            idStr str = new idStr(cvarSystem.GetCVarString("mod_validSkins"));
            idStr skin;
            while (str.Length() != 0) {
                int n = str.Find(";");
                if (n >= 0) {
                    skin = str.Left(n);
                    str = str.Right(str.Length() - n - 1);
                } else {
                    skin = str;
                    str.oSet("");
                }
                declManager.FindSkin(skin, false);
            }

            for (i = 0; ui_skinArgs[i] != null; i++) {
                declManager.FindSkin(ui_skinArgs[i], false);
            }
            // MP game sounds
            for (i = 0; i < SND_COUNT.ordinal(); i++) {
                f = fileSystem.OpenFileRead(GlobalSoundStrings[ i]);
                fileSystem.CloseFile(f);
            }
            // MP guis. just make sure we hit all of them
            i = 0;
            while (MPGuis[ i] != null) {
                uiManager.FindGui(MPGuis[ i], true);
                i++;
            }
        }

        // throttle UI switch rates
        public void ThrottleUserInfo() {
            int i;

            assert (gameLocal.localClientNum >= 0);

            i = 0;
            while (ThrottleVars[ i] != null) {
                if (idStr.Icmp(gameLocal.userInfo[ gameLocal.localClientNum].GetString(ThrottleVars[ i]),
                        cvarSystem.GetCVarString(ThrottleVars[ i])) != 0) {
                    if (gameLocal.realClientTime < switchThrottle[ i]) {
                        AddChatLine(common.GetLanguageDict().GetString("#str_04299"), common.GetLanguageDict().GetString(ThrottleVarsInEnglish[ i]), (switchThrottle[ i] - gameLocal.time) / 1000 + 1);
                        cvarSystem.SetCVarString(ThrottleVars[ i], gameLocal.userInfo[ gameLocal.localClientNum].GetString(ThrottleVars[ i]));
                    } else {
                        switchThrottle[ i] = gameLocal.time + ThrottleDelay[ i] * 1000;
                    }
                }
                i++;
            }
        }

        public void ToggleSpectate() {
            boolean spectating;
            assert (gameLocal.isClient || gameLocal.localClientNum == 0);

            spectating = (idStr.Icmp(cvarSystem.GetCVarString("ui_spectate"), "Spectate") == 0);
            if (spectating) {
                // always allow toggling to play
                cvarSystem.SetCVarString("ui_spectate", "Play");
            } else {
                // only allow toggling to spectate if spectators are enabled.
                if (gameLocal.serverInfo.GetBool("si_spectators")) {
                    cvarSystem.SetCVarString("ui_spectate", "Spectate");
                } else {
                    gameLocal.mpGame.AddChatLine(common.GetLanguageDict().GetString("#str_06747"));
                }
            }
        }

        public void ToggleReady() {
            boolean ready;
            assert (gameLocal.isClient || gameLocal.localClientNum == 0);

            ready = (idStr.Icmp(cvarSystem.GetCVarString("ui_ready"), "Ready") == 0);
            if (ready) {
                cvarSystem.SetCVarString("ui_ready", "Not Ready");
            } else {
                cvarSystem.SetCVarString("ui_ready", "Ready");
            }
        }

        public void ToggleTeam() {
            boolean team;
            assert (gameLocal.isClient || gameLocal.localClientNum == 0);

            team = (idStr.Icmp(cvarSystem.GetCVarString("ui_team"), "Red") == 0);
            if (team) {
                cvarSystem.SetCVarString("ui_team", "Blue");
            } else {
                cvarSystem.SetCVarString("ui_team", "Red");
            }
        }

        public void ClearFrags(int clientNum) {
            playerState[clientNum].fragCount = 0;
        }

        public void EnterGame(int clientNum) {
            assert (!gameLocal.isClient);

            if (!playerState[ clientNum].ingame) {
                playerState[ clientNum].ingame = true;
                if (gameLocal.isMultiplayer) {
                    // can't use PrintMessageEvent as clients don't know the nickname yet
                    gameLocal.ServerSendChatMessage(-1, common.GetLanguageDict().GetString("#str_02047"), va(common.GetLanguageDict().GetString("#str_07177"), gameLocal.userInfo[ clientNum].GetString("ui_name")));
                }
            }
        }

        public boolean CanPlay(idPlayer p) {
            return !p.wantSpectate && playerState[ p.entityNumber].ingame;
        }

        public boolean IsInGame(int clientNum) {
            return playerState[clientNum].ingame;
        }

        public boolean WantRespawn(idPlayer p) {
            return p.forceRespawn && !p.wantSpectate && playerState[ p.entityNumber].ingame;
        }

        public void ServerWriteInitialReliableMessages(int clientNum) {
            idBitMsg outMsg = new idBitMsg();
            ByteBuffer msgBuf = ByteBuffer.allocate(MAX_GAME_MESSAGE_SIZE);
            int i;
            idEntity ent;

            outMsg.Init(msgBuf, MAX_GAME_MESSAGE_SIZE);
            outMsg.BeginWriting();
            outMsg.WriteByte(GAME_RELIABLE_MESSAGE_STARTSTATE);
            // send the game state and start time
            outMsg.WriteByte(etoi(gameState));
            outMsg.WriteLong(matchStartedTime);
            outMsg.WriteShort(startFragLimit);
            // send the powerup states and the spectate states
            for (i = 0; i < gameLocal.numClients; i++) {
                ent = gameLocal.entities[ i];
                if (i != clientNum && ent != null && ent.IsType(idPlayer.class)) {
                    outMsg.WriteShort(i);
                    outMsg.WriteShort(((idPlayer) ent).inventory.powerups);
                    outMsg.WriteBits(btoi(((idPlayer) ent).spectating), 1);
                }
            }
            outMsg.WriteShort(MAX_CLIENTS);
            networkSystem.ServerSendReliableMessage(clientNum, outMsg);

            // we send SI in connectResponse messages, but it may have been modified already
            outMsg.BeginWriting();
            outMsg.WriteByte(GAME_RELIABLE_MESSAGE_SERVERINFO);
            outMsg.WriteDeltaDict(gameLocal.serverInfo, null);
            networkSystem.ServerSendReliableMessage(clientNum, outMsg);

            // warmup time
            if (gameState == COUNTDOWN) {
                outMsg.BeginWriting();
                outMsg.WriteByte(GAME_RELIABLE_MESSAGE_WARMUPTIME);
                outMsg.WriteLong(warmupEndTime);
                networkSystem.ServerSendReliableMessage(clientNum, outMsg);
            }
        }

        public void ClientReadStartState(final idBitMsg msg) {
            int i, client, powerup;

            // read the state in preparation for reading snapshot updates
            gameState = gameState_t.values()[msg.ReadByte()];
            matchStartedTime = msg.ReadLong();
            startFragLimit = msg.ReadShort();
            while ((client = msg.ReadShort()) != MAX_CLIENTS) {
                assert (gameLocal.entities[ client] != null && gameLocal.entities[ client].IsType(idPlayer.class));
                powerup = msg.ReadShort();
                for (i = 0; i < MAX_POWERUPS; i++) {
                    if ((powerup & (1 << i)) != 0) {
                        ((idPlayer) gameLocal.entities[ client]).GivePowerUp(i, 0);
                    }
                }
                boolean spectate = (msg.ReadBits(1) != 0);
                ((idPlayer) gameLocal.entities[ client]).Spectate(spectate);
            }
        }

        public void ClientReadWarmupTime(final idBitMsg msg) {
            warmupEndTime = msg.ReadLong();
        }

        public void ServerClientConnect(int clientNum) {
//	memset( &playerState[ clientNum ], 0, sizeof( playerState[ clientNum ] ) );
            playerState[clientNum] = new mpPlayerState_s();
//            for (int i = clientNum; i < playerState.length; i++) {
//                playerState[i] = new mpPlayerState_s();
//            }
        }

        public void PlayerStats(int clientNum, String[] data, final int len) {

            idEntity ent;
            int team;

            data[0] = null;

            // make sure we don't exceed the client list
            if (clientNum < 0 || clientNum > gameLocal.numClients) {
                return;
            }

            // find which team this player is on
            ent = gameLocal.entities[ clientNum];
            if (ent != null && ent.IsType(idPlayer.class)) {
                team = ((idPlayer) ent).team;
            } else {
                return;
            }

            idStr.snPrintf(data, len, "team=%d score=%ld tks=%ld", team, playerState[ clientNum].fragCount, playerState[ clientNum].teamFragCount);

            return;

        }
//
//
        private static final String[] MPGuis = {
            "guis/mphud.gui",
            "guis/mpmain.gui",
            "guis/mpmsgmode.gui",
            "guis/netmenu.gui",
            null
        };
        private static final String[] ThrottleVars = {
            "ui_spectate",
            "ui_ready",
            "ui_team",
            null
        };
        private static final String[] ThrottleVarsInEnglish = {
            "#str_06738",
            "#str_06737",
            "#str_01991",
            null
        };
        private static final int[] ThrottleDelay = {
            8,
            5,
            5
        };

        private void UpdatePlayerRanks() {
            int i, j, k;
            idPlayer[] players = new idPlayer[MAX_CLIENTS];
            idEntity ent;
            idPlayer player;

//	memset( players, 0, sizeof( players ) );
            numRankedPlayers = 0;

            for (i = 0; i < gameLocal.numClients; i++) {
                ent = gameLocal.entities[ i];
                if (null == ent || !ent.IsType(idPlayer.class)) {
                    continue;
                }
                player = (idPlayer) ent;
                if (!CanPlay(player)) {
                    continue;
                }
                if (gameLocal.gameType == GAME_TOURNEY) {
                    if (i != currentTourneyPlayer[ 0] && i != currentTourneyPlayer[ 1]) {
                        continue;
                    }
                }
                if (gameLocal.gameType == GAME_LASTMAN && playerState[ i].fragCount == LASTMAN_NOLIVES) {
                    continue;
                }
                for (j = 0; j < numRankedPlayers; j++) {
                    boolean insert = false;
                    if (gameLocal.gameType == GAME_TDM) {
                        if (player.team != players[ j].team) {
                            if (playerState[ i].teamFragCount > playerState[ players[ j].entityNumber].teamFragCount) {
                                // team scores
                                insert = true;
                            } else if (playerState[ i].teamFragCount == playerState[ players[ j].entityNumber].teamFragCount && player.team < players[ j].team) {
                                // at equal scores, sort by team number
                                insert = true;
                            }
                        } else if (playerState[ i].fragCount > playerState[ players[ j].entityNumber].fragCount) {
                            // in the same team, sort by frag count
                            insert = true;
                        }
                    } else {
                        insert = (playerState[ i].fragCount > playerState[ players[ j].entityNumber].fragCount);
                    }
                    if (insert) {
                        for (k = numRankedPlayers; k > j; k--) {
                            players[ k] = players[ k - 1];
                        }
                        players[ j] = player;
                        break;
                    }
                }
                if (j == numRankedPlayers) {
                    players[ numRankedPlayers] = player;
                }
                numRankedPlayers++;
            }

//	memcpy( rankedPlayers, players, sizeof( players ) );
            System.arraycopy(players, 0, rankedPlayers, 0, players.length);
        }

        // updates the passed gui with current score information
        private void UpdateRankColor(idUserInterface gui, final String mask, int i, final idVec3 vec) {
            for (int j = 1; j < 4; j++) {
                gui.SetStateFloat(va(mask, i, j), vec.oGet(j - 1));
            }
        }

        private void UpdateScoreboard(idUserInterface scoreBoard, idPlayer player) {
            int i, j, iline, k;
            String gameinfo;
            String livesinfo;
            String timeinfo;
            idEntity ent;
            idPlayer p;
            int value;

            scoreBoard.SetStateString("scoretext", gameLocal.gameType == GAME_LASTMAN ? common.GetLanguageDict().GetString("#str_04242") : common.GetLanguageDict().GetString("#str_04243"));

            iline = 0; // the display lines
            if (gameState != WARMUP) {
                for (i = 0; i < numRankedPlayers; i++) {
                    // ranked player
                    iline++;
                    scoreBoard.SetStateString(va("player%d", iline), rankedPlayers[ i].GetUserInfo().GetString("ui_name"));
                    if (gameLocal.gameType == GAME_TDM) {
                        value = idMath.ClampInt(MP_PLAYER_MINFRAGS, MP_PLAYER_MAXFRAGS, playerState[ rankedPlayers[ i].entityNumber].fragCount);
                        scoreBoard.SetStateInt(va("player%d_tdm_score", iline), value);
                        value = idMath.ClampInt(MP_PLAYER_MINFRAGS, MP_PLAYER_MAXFRAGS, playerState[ rankedPlayers[ i].entityNumber].teamFragCount);
                        scoreBoard.SetStateString(va("player%d_tdm_tscore", iline), va("/ %d", value));
                        scoreBoard.SetStateString(va("player%d_score", iline), "");
                    } else {
                        value = idMath.ClampInt(MP_PLAYER_MINFRAGS, MP_PLAYER_MAXFRAGS, playerState[ rankedPlayers[ i].entityNumber].fragCount);
                        scoreBoard.SetStateInt(va("player%d_score", iline), value);
                        scoreBoard.SetStateString(va("player%d_tdm_tscore", iline), "");
                        scoreBoard.SetStateString(va("player%d_tdm_score", iline), "");
                    }
                    value = idMath.ClampInt(0, MP_PLAYER_MAXWINS, playerState[ rankedPlayers[ i].entityNumber].wins);
                    scoreBoard.SetStateInt(va("player%d_wins", iline), value);
                    scoreBoard.SetStateInt(va("player%d_ping", iline), playerState[ rankedPlayers[ i].entityNumber].ping);
                    // set the color band
                    scoreBoard.SetStateInt(va("rank%d", iline), 1);
                    UpdateRankColor(scoreBoard, "rank%d_color%d", iline, rankedPlayers[ i].colorBar);
                    if (rankedPlayers[ i] == player) {
                        // highlight who we are
                        scoreBoard.SetStateInt("rank_self", iline);
                    }
                }
            }

            // if warmup, this draws everyone, otherwise it goes over spectators only
            // when doing warmup we loop twice to draw ready/not ready first *then* spectators
            // NOTE: in tourney, shows spectators according to their playing rank order?
            for (k = 0; k < (gameState == WARMUP ? 2 : 1); k++) {
                for (i = 0; i < MAX_CLIENTS; i++) {
                    ent = gameLocal.entities[ i];
                    if (null == ent || !ent.IsType(idPlayer.class)) {
                        continue;
                    }
                    if (gameState != WARMUP) {
                        // check he's not covered by ranks already
                        for (j = 0; j < numRankedPlayers; j++) {
                            if (ent.equals(rankedPlayers[ j])) {
                                break;
                            }
                        }
                        if (j != numRankedPlayers) {
                            continue;
                        }
                    }
                    p = (idPlayer) ent;
                    if (gameState == WARMUP) {
                        if (k == 0 && p.spectating) {
                            continue;
                        }
                        if (k == 1 && !p.spectating) {
                            continue;
                        }
                    }

                    iline++;
                    if (!playerState[ i].ingame) {
                        scoreBoard.SetStateString(va("player%d", iline), common.GetLanguageDict().GetString("#str_04244"));
                        scoreBoard.SetStateString(va("player%d_score", iline), common.GetLanguageDict().GetString("#str_04245"));
                        // no color band
                        scoreBoard.SetStateInt(va("rank%d", iline), 0);
                    } else {
                        scoreBoard.SetStateString(va("player%d", iline), gameLocal.userInfo[ i].GetString("ui_name"));
                        if (gameState == WARMUP) {
                            if (p.spectating) {
                                scoreBoard.SetStateString(va("player%d_score", iline), common.GetLanguageDict().GetString("#str_04246"));
                                // no color band
                                scoreBoard.SetStateInt(va("rank%d", iline), 0);
                            } else {
                                scoreBoard.SetStateString(va("player%d_score", iline), p.IsReady() ? common.GetLanguageDict().GetString("#str_04247") : common.GetLanguageDict().GetString("#str_04248"));
                                // set the color band
                                scoreBoard.SetStateInt(va("rank%d", iline), 1);
                                UpdateRankColor(scoreBoard, "rank%d_color%d", iline, p.colorBar);
                            }
                        } else {
                            if (gameLocal.gameType == GAME_LASTMAN && playerState[ i].fragCount == LASTMAN_NOLIVES) {
                                scoreBoard.SetStateString(va("player%d_score", iline), common.GetLanguageDict().GetString("#str_06736"));
                                // set the color band
                                scoreBoard.SetStateInt(va("rank%d", iline), 1);
                                UpdateRankColor(scoreBoard, "rank%d_color%d", iline, p.colorBar);
                            } else {
                                scoreBoard.SetStateString(va("player%d_score", iline), common.GetLanguageDict().GetString("#str_04246"));
                                // no color band
                                scoreBoard.SetStateInt(va("rank%d", iline), 0);
                            }
                        }
                    }
                    scoreBoard.SetStateString(va("player%d_tdm_tscore", iline), "");
                    scoreBoard.SetStateString(va("player%d_tdm_score", iline), "");
                    scoreBoard.SetStateString(va("player%d_wins", iline), "");
                    scoreBoard.SetStateInt(va("player%d_ping", iline), playerState[ i].ping);
                    if (i == player.entityNumber) {
                        // highlight who we are
                        scoreBoard.SetStateInt("rank_self", iline);
                    }
                }
            }

            // clear remaining lines (empty slots)
            iline++;
            while (iline < 5) {
                scoreBoard.SetStateString(va("player%d", iline), "");
                scoreBoard.SetStateString(va("player%d_score", iline), "");
                scoreBoard.SetStateString(va("player%d_tdm_tscore", iline), "");
                scoreBoard.SetStateString(va("player%d_tdm_score", iline), "");
                scoreBoard.SetStateString(va("player%d_wins", iline), "");
                scoreBoard.SetStateString(va("player%d_ping", iline), "");
                scoreBoard.SetStateInt(va("rank%d", iline), 0);
                iline++;
            }

            gameinfo = va("%s: %s", common.GetLanguageDict().GetString("#str_02376"), gameLocal.serverInfo.GetString("si_gameType"));
            if (gameLocal.gameType == GAME_LASTMAN) {
                if (gameState == GAMEON || gameState == SUDDENDEATH) {
                    livesinfo = va("%s: %d", common.GetLanguageDict().GetString("#str_04264"), startFragLimit);
                } else {
                    livesinfo = va("%s: %d", common.GetLanguageDict().GetString("#str_04264"), gameLocal.serverInfo.GetInt("si_fragLimit"));
                }

            } else {
                livesinfo = va("%s: %d", common.GetLanguageDict().GetString("#str_01982"), gameLocal.serverInfo.GetInt("si_fragLimit"));
            }
            if (gameLocal.serverInfo.GetInt("si_timeLimit") > 0) {
                timeinfo = va("%s: %d", common.GetLanguageDict().GetString("#str_01983"), gameLocal.serverInfo.GetInt("si_timeLimit"));
            } else {
                timeinfo = va("%s", common.GetLanguageDict().GetString("#str_07209"));
            }
            scoreBoard.SetStateString("gameinfo", gameinfo);
            scoreBoard.SetStateString("livesinfo", livesinfo);
            scoreBoard.SetStateString("timeinfo", timeinfo);

            scoreBoard.Redraw(gameLocal.time);
        }

        private void ClearGuis() {
            int i;

            for (i = 0; i < MAX_CLIENTS; i++) {
                scoreBoard.SetStateString(va("player%d", i + 1), "");
                scoreBoard.SetStateString(va("player%d_score", i + 1), "");
                scoreBoard.SetStateString(va("player%d_tdm_tscore", i + 1), "");
                scoreBoard.SetStateString(va("player%d_tdm_score", i + 1), "");
                scoreBoard.SetStateString(va("player%d_wins", i + 1), "");
                scoreBoard.SetStateString(va("player%d_status", i + 1), "");
                scoreBoard.SetStateInt(va("rank%d", i + 1), 0);
                scoreBoard.SetStateInt("rank_self", 0);

                idPlayer player = (idPlayer) gameLocal.entities[ i];
                if (null == player || null == player.hud) {
                    continue;
                }
                player.hud.SetStateString(va("player%d", i + 1), "");
                player.hud.SetStateString(va("player%d_score", i + 1), "");
                player.hud.SetStateString(va("player%d_ready", i + 1), "");
                scoreBoard.SetStateInt(va("rank%d", i + 1), 0);
                player.hud.SetStateInt("rank_self", 0);
            }
        }

        private void DrawScoreBoard(idPlayer player) {
            if (player.scoreBoardOpen || gameState == GAMEREVIEW) {
                if (!playerState[ player.entityNumber].scoreBoardUp) {
                    scoreBoard.Activate(true, gameLocal.time);
                    playerState[ player.entityNumber].scoreBoardUp = true;
                }
                UpdateScoreboard(scoreBoard, player);
            } else {
                if (playerState[ player.entityNumber].scoreBoardUp) {
                    scoreBoard.Activate(false, gameLocal.time);
                    playerState[ player.entityNumber].scoreBoardUp = false;
                }
            }
        }

        private void UpdateHud(idPlayer player, idUserInterface hud) {
            int i;

            if (null == hud) {
                return;
            }

            hud.SetStateBool("warmup", Warmup());

            if (gameState == WARMUP) {
                if (player.IsReady()) {
                    hud.SetStateString("warmuptext", common.GetLanguageDict().GetString("#str_04251"));
                } else {
                    hud.SetStateString("warmuptext", common.GetLanguageDict().GetString("#str_07002"));
                }
            }

            hud.SetStateString("timer", (Warmup()) ? common.GetLanguageDict().GetString("#str_04251") : (gameState == SUDDENDEATH) ? common.GetLanguageDict().GetString("#str_04252") : GameTime());
            if (vote != VOTE_NONE) {
                hud.SetStateString("vote", va("%s (y: %d n: %d)", voteString, (int) yesVotes, (int) noVotes));
            } else {
                hud.SetStateString("vote", "");
            }

            hud.SetStateInt("rank_self", 0);
            if (gameState == GAMEON) {
                for (i = 0; i < numRankedPlayers; i++) {
                    if (gameLocal.gameType == GAME_TDM) {
                        hud.SetStateInt(va("player%d_score", i + 1), playerState[ rankedPlayers[ i].entityNumber].teamFragCount);
                    } else {
                        hud.SetStateInt(va("player%d_score", i + 1), playerState[ rankedPlayers[ i].entityNumber].fragCount);
                    }
                    hud.SetStateInt(va("rank%d", i + 1), 1);
                    UpdateRankColor(hud, "rank%d_color%d", i + 1, rankedPlayers[ i].colorBar);
                    if (rankedPlayers[ i] == player) {
                        hud.SetStateInt("rank_self", i + 1);
                    }
                }
            }
            for (i = (gameState == GAMEON ? numRankedPlayers : 0); i < 5; i++) {
                hud.SetStateString(va("player%d", i + 1), "");
                hud.SetStateString(va("player%d_score", i + 1), "");
                hud.SetStateInt(va("rank%d", i + 1), 0);
            }
        }

        private boolean Warmup() {
            return (gameState == WARMUP);
        }

        private void CheckVote() {
            int numVoters, i;

            if (vote == VOTE_NONE) {
                return;
            }

            if (voteExecTime != 0) {
                if (gameLocal.time > voteExecTime) {
                    voteExecTime = 0;
                    ClientUpdateVote(VOTE_RESET, 0, 0);
                    ExecuteVote();
                    vote = VOTE_NONE;
                }
                return;
            }

            // count voting players
            numVoters = 0;
            for (i = 0; i < gameLocal.numClients; i++) {
                idEntity ent = gameLocal.entities[ i];
                if (null == ent || !ent.IsType(idPlayer.class)) {
                    continue;
                }
                if (playerState[ i].vote != PLAYER_VOTE_NONE) {
                    numVoters++;
                }
            }
            if (0 == numVoters) {
                // abort
                vote = VOTE_NONE;
                ClientUpdateVote(VOTE_ABORTED, (int) yesVotes, (int) noVotes);
                return;
            }
            if (yesVotes / numVoters > 0.5f) {
                ClientUpdateVote(VOTE_PASSED, (int) yesVotes, (int) noVotes);
                voteExecTime = gameLocal.time + 2000;
                return;
            }
            if (gameLocal.time > voteTimeOut || noVotes / numVoters >= 0.5f) {
                ClientUpdateVote(VOTE_FAILED, (int) yesVotes, (int) noVotes);
                vote = VOTE_NONE;
                return;
            }
        }

        private boolean AllPlayersReady() {
            int i;
            idEntity ent;
            idPlayer p;
            int[] team = new int[2];

            if (NumActualClients(false, team) <= 1) {
                return false;
            }

            if (gameLocal.gameType == GAME_TDM) {
                if (0 == team[ 0] || 0 == team[ 1]) {
                    return false;
                }
            }

            if (!gameLocal.serverInfo.GetBool("si_warmup")) {
                return true;
            }

            for (i = 0; i < gameLocal.numClients; i++) {
                if (gameLocal.gameType == GAME_TOURNEY && i != currentTourneyPlayer[ 0] && i != currentTourneyPlayer[ 1]) {
                    continue;
                }
                ent = gameLocal.entities[ i];
                if (null == ent || !ent.IsType(idPlayer.class)) {
                    continue;
                }
                p = (idPlayer) ent;
                if (CanPlay(p) && !p.IsReady()) {
                    return false;
                }
                team[ p.team]++;
            }

            return true;
        }

        /*
         ================
         idMultiplayerGame::FragLimitHit
         return the winning player (team player)
         if there is no FragLeader(), the game is tied and we return NULL
         ================
         */
        private idPlayer FragLimitHit() {
            int i;
            int fragLimit = gameLocal.serverInfo.GetInt("si_fragLimit");
            idPlayer leader;

            leader = FragLeader();
            if (null == leader) {
                return null;
            }

            if (fragLimit <= 0) {
                fragLimit = MP_PLAYER_MAXFRAGS;
            }

            if (gameLocal.gameType == GAME_LASTMAN) {
                // we have a leader, check if any other players have frags left
                assert (!((idPlayer) leader).lastManOver);
                for (i = 0; i < gameLocal.numClients; i++) {
                    idEntity ent = gameLocal.entities[ i];
                    if (null == ent || !ent.IsType(idPlayer.class)) {
                        continue;
                    }
                    if (!CanPlay((idPlayer) ent)) {
                        continue;
                    }
                    if (ent.equals(leader)) {
                        continue;
                    }
                    if (playerState[ ent.entityNumber].fragCount > 0) {
                        return null;
                    }
                }
                // there is a leader, his score may even be negative, but no one else has frags left or is !lastManOver
                return leader;
            } else if (gameLocal.gameType == GAME_TDM) {
                if (playerState[ leader.entityNumber].teamFragCount >= fragLimit) {
                    return leader;
                }
            } else {
                if (playerState[ leader.entityNumber].fragCount >= fragLimit) {
                    return leader;
                }
            }

            return null;
        }

        /*
         ================
         idMultiplayerGame::FragLeader
         return the current winner ( or a player from the winning team )
         NULL if even
         ================
         */
        private idPlayer FragLeader() {
            int i;
            int[] frags = new int[MAX_CLIENTS];
            idPlayer leader = null;
            idEntity ent;
            idPlayer p;
            int high = -9999;
            int count = 0;
            boolean[] teamLead/*[ 2 ]*/ = {false, false};

            for (i = 0; i < gameLocal.numClients; i++) {
                ent = gameLocal.entities[ i];
                if (null == ent || !ent.IsType(idPlayer.class)) {
                    continue;
                }
                if (!CanPlay((idPlayer) ent)) {
                    continue;
                }
                if (gameLocal.gameType == GAME_TOURNEY && ent.entityNumber != currentTourneyPlayer[ 0] && ent.entityNumber != currentTourneyPlayer[ 1]) {
                    continue;
                }
                if (((idPlayer) ent).lastManOver) {
                    continue;
                }

                int fragc = (gameLocal.gameType == GAME_TDM) ? playerState[i].teamFragCount : playerState[i].fragCount;
                if (fragc > high) {
                    high = fragc;
                }

                frags[ i] = fragc;
            }

            for (i = 0; i < gameLocal.numClients; i++) {
                ent = gameLocal.entities[ i];
                if (null == ent || !ent.IsType(idPlayer.class)) {
                    continue;
                }
                p = (idPlayer) ent;
                p.SetLeader(false);

                if (!CanPlay(p)) {
                    continue;
                }
                if (gameLocal.gameType == GAME_TOURNEY && ent.entityNumber != currentTourneyPlayer[ 0] && ent.entityNumber != currentTourneyPlayer[ 1]) {
                    continue;
                }
                if (p.lastManOver) {
                    continue;
                }
                if (p.spectating) {
                    continue;
                }

                if (frags[ i] >= high) {
                    leader = p;
                    count++;
                    p.SetLeader(true);
                    if (gameLocal.gameType == GAME_TDM) {
                        teamLead[ p.team] = true;
                    }
                }
            }

            if (gameLocal.gameType != GAME_TDM) {
                // more than one player at the highest frags
                if (count > 1) {
                    return null;
                } else {
                    return leader;
                }
            } else {
                if (teamLead[ 0] && teamLead[ 1]) {
                    // even game in team play
                    return null;
                }
                return leader;
            }
        }

        boolean TimeLimitHit() {
            int timeLimit = gameLocal.serverInfo.GetInt("si_timeLimit");
            if (timeLimit != 0) {
                if (gameLocal.time >= matchStartedTime + timeLimit * 60000) {
                    return true;
                }
            }
            return false;
        }

        private void NewState(gameState_t news, idPlayer player /*= NULL */) {
            idBitMsg outMsg = new idBitMsg();
            ByteBuffer msgBuf = ByteBuffer.allocate(MAX_GAME_MESSAGE_SIZE);
            int i;

            assert (news != gameState);
            assert (!gameLocal.isClient);
            gameLocal.DPrintf("%s . %s\n", GameStateStrings[etoi(gameState)], GameStateStrings[etoi(news)]);
            switch (news) {
                case GAMEON: {
                    gameLocal.LocalMapRestart();
                    outMsg.Init(msgBuf, MAX_GAME_MESSAGE_SIZE);
                    outMsg.WriteByte(GAME_RELIABLE_MESSAGE_RESTART);
                    outMsg.WriteBits(0, 1);
                    networkSystem.ServerSendReliableMessage(-1, outMsg);

                    PlayGlobalSound(-1, SND_FIGHT);
                    matchStartedTime = gameLocal.time;
                    fragLimitTimeout = 0;
                    for (i = 0; i < gameLocal.numClients; i++) {
                        idEntity ent = gameLocal.entities[ i];
                        if (null == ent || !ent.IsType(idPlayer.class)) {
                            continue;
                        }
                        idPlayer p = (idPlayer) ent;
                        p.SetLeader(false); // don't carry the flag from previous games
                        if (gameLocal.gameType == GAME_TOURNEY && currentTourneyPlayer[ 0] != i && currentTourneyPlayer[ 1] != i) {
                            p.ServerSpectate(true);
                            p.tourneyRank++;
                        } else {
                            int fragLimit = gameLocal.serverInfo.GetInt("si_fragLimit");
                            int startingCount = (gameLocal.gameType == GAME_LASTMAN) ? fragLimit : 0;
                            playerState[ i].fragCount = startingCount;
                            playerState[ i].teamFragCount = startingCount;
                            if (!((idPlayer) ent).wantSpectate) {
                                ((idPlayer) ent).ServerSpectate(false);
                                if (gameLocal.gameType == GAME_TOURNEY) {
                                    p.tourneyRank = 0;
                                }
                            }
                        }
                        if (CanPlay(p)) {
                            p.lastManPresent = true;
                        } else {
                            p.lastManPresent = false;
                        }
                    }
                    cvarSystem.SetCVarString("ui_ready", "Not Ready");
                    switchThrottle[ 1] = 0;	// passby the throttle
                    startFragLimit = gameLocal.serverInfo.GetInt("si_fragLimit");
                    break;
                }
                case GAMEREVIEW: {
                    nextState = INACTIVE;	// used to abort a game. cancel out any upcoming state change
                    // set all players not ready and spectating
                    for (i = 0; i < gameLocal.numClients; i++) {
                        idEntity ent = gameLocal.entities[i];
                        if (NOT(ent) || !ent.IsType(idPlayer.class)) {
                            continue;
                        }
                        ((idPlayer) ent).forcedReady = false;
                        ((idPlayer) ent).ServerSpectate(true);
                    }
                    UpdateWinsLosses(player);
                    break;
                }
                case SUDDENDEATH: {
                    PrintMessageEvent(-1, MSG_SUDDENDEATH);
                    PlayGlobalSound(-1, SND_SUDDENDEATH);
                    break;
                }
                case COUNTDOWN: {
                    idBitMsg outMsg2 = new idBitMsg();
                    msgBuf = ByteBuffer.allocate(128);

                    warmupEndTime = gameLocal.time + 1000 * cvarSystem.GetCVarInteger("g_countDown");

                    outMsg2.Init(msgBuf, msgBuf.capacity());
                    outMsg2.WriteByte(GAME_RELIABLE_MESSAGE_WARMUPTIME);
                    outMsg2.WriteLong(warmupEndTime);
                    networkSystem.ServerSendReliableMessage(-1, outMsg2);

                    break;
                }
                default:
                    break;
            }

            gameState = news;
        }

        private void NewState(gameState_t news) {
            NewState(news, null);
        }

        private void UpdateWinsLosses(idPlayer winner) {
            if (winner != null) {
                // run back through and update win/loss count
                for (int i = 0; i < gameLocal.numClients; i++) {
                    idEntity ent = gameLocal.entities[ i];
                    if (null == ent || !ent.IsType(idPlayer.class)) {
                        continue;
                    }
                    idPlayer player = (idPlayer) ent;
                    if (gameLocal.gameType == GAME_TDM) {
                        if (player == winner || (player != winner && player.team == winner.team)) {
                            playerState[ i].wins++;
                            PlayGlobalSound(player.entityNumber, SND_YOUWIN);
                        } else {
                            PlayGlobalSound(player.entityNumber, SND_YOULOSE);
                        }
                    } else if (gameLocal.gameType == GAME_LASTMAN) {
                        if (player == winner) {
                            playerState[ i].wins++;
                            PlayGlobalSound(player.entityNumber, SND_YOUWIN);
                        } else if (!player.wantSpectate) {
                            PlayGlobalSound(player.entityNumber, SND_YOULOSE);
                        }
                    } else if (gameLocal.gameType == GAME_TOURNEY) {
                        if (player == winner) {
                            playerState[ i].wins++;
                            PlayGlobalSound(player.entityNumber, SND_YOUWIN);
                        } else if (i == currentTourneyPlayer[ 0] || i == currentTourneyPlayer[ 1]) {
                            PlayGlobalSound(player.entityNumber, SND_YOULOSE);
                        }
                    } else {
                        if (player == winner) {
                            playerState[i].wins++;
                            PlayGlobalSound(player.entityNumber, SND_YOUWIN);
                        } else if (!player.wantSpectate) {
                            PlayGlobalSound(player.entityNumber, SND_YOULOSE);
                        }
                    }
                }
            }
            if (winner != null) {
                lastWinner = winner.entityNumber;
            } else {
                lastWinner = -1;
            }
        }

        /*
         ================
         idMultiplayerGame::FillTourneySlots
         NOTE: called each frame during warmup to keep the tourney slots filled
         ================
         */
        // fill any empty tourney slots based on the current tourney ranks
        private void FillTourneySlots() {
            int i, j, rankmax, rankmaxindex;
            idEntity ent;
            idPlayer p;

            // fill up the slots based on tourney ranks
            for (i = 0; i < 2; i++) {
                if (currentTourneyPlayer[ i] != -1) {
                    continue;
                }
                rankmax = -1;
                rankmaxindex = -1;
                for (j = 0; j < gameLocal.numClients; j++) {
                    ent = gameLocal.entities[ j];
                    if (null == ent || !ent.IsType(idPlayer.class)) {
                        continue;
                    }
                    if (currentTourneyPlayer[ 0] == j || currentTourneyPlayer[ 1] == j) {
                        continue;
                    }
                    p = (idPlayer) ent;
                    if (p.wantSpectate) {
                        continue;
                    }
                    if (p.tourneyRank >= rankmax) {
                        // when ranks are equal, use time in game
                        if (p.tourneyRank == rankmax) {
                            assert (rankmaxindex >= 0);
                            if (p.spawnedTime > ((idPlayer) gameLocal.entities[ rankmaxindex]).spawnedTime) {
                                continue;
                            }
                        }
                        rankmax = ((idPlayer) ent).tourneyRank;
                        rankmaxindex = j;
                    }
                }
                currentTourneyPlayer[ i] = rankmaxindex; // may be -1 if we found nothing
            }
        }

        private void CycleTourneyPlayers() {
            int i;
            idEntity ent;
            idPlayer player;

            currentTourneyPlayer[ 0] = -1;
            currentTourneyPlayer[ 1] = -1;
            // if any, winner from last round will play again
            if (lastWinner != -1) {
                idEntity ent2 = gameLocal.entities[lastWinner];
                if (ent2 != null && ent2.IsType(idPlayer.class)) {
                    currentTourneyPlayer[ 0] = lastWinner;
                }
            }
            FillTourneySlots();
            // force selected players in/out of the game and update the ranks
            for (i = 0; i < gameLocal.numClients; i++) {
                if (currentTourneyPlayer[ 0] == i || currentTourneyPlayer[ 1] == i) {
                    player = (idPlayer) gameLocal.entities[ i];
                    player.ServerSpectate(false);
                } else {
                    ent = gameLocal.entities[ i];
                    if (ent != null && ent.IsType(idPlayer.class)) {
                        player = (idPlayer) gameLocal.entities[ i];
                        player.ServerSpectate(true);
                    }
                }
            }
            UpdateTourneyLine();
        }

        /*
         ================
         idMultiplayerGame::UpdateTourneyLine
         we manipulate tourneyRank on player entities for internal ranking. it's easier to deal with.
         but we need a real wait list to be synced down to clients for GUI
         ignore current players, ignore wantSpectate
         ================
         */
        // walk through the tourneyRank to build a wait list for the clients
        private void UpdateTourneyLine() {
            int i, j, imax, max, globalmax = -1;
            idPlayer p;

            assert (!gameLocal.isClient);
            if (gameLocal.gameType != GAME_TOURNEY) {
                return;
            }

            for (j = 1; j <= gameLocal.numClients; j++) {
                max = -1;
                imax = -1;
                for (i = 0; i < gameLocal.numClients; i++) {
                    if (currentTourneyPlayer[ 0] == i || currentTourneyPlayer[ 1] == i) {
                        continue;
                    }
                    p = (idPlayer) gameLocal.entities[ i];
                    if (null == p || p.wantSpectate) {
                        continue;
                    }
                    if (p.tourneyRank > max && (globalmax == -1 || p.tourneyRank < globalmax)) {
                        imax = i;
                        max = p.tourneyRank;
                    }
                }
                if (imax == -1) {
                    break;
                }

                idBitMsg outMsg = new idBitMsg();
                ByteBuffer msgBuf = ByteBuffer.allocate(1024);
                outMsg.Init(msgBuf, msgBuf.capacity());
                outMsg.WriteByte(GAME_RELIABLE_MESSAGE_TOURNEYLINE);
                outMsg.WriteByte(j);
                networkSystem.ServerSendReliableMessage(imax, outMsg);

                globalmax = max;
            }
        }
//	private static final char []buff = new char[16];
        private static String buff;

        private String GameTime() {
            int m, s, t, ms;

            if (gameState == COUNTDOWN) {
                ms = warmupEndTime - gameLocal.realClientTime;
                s = ms / 1000 + 1;
                if (ms <= 0) {
//                    strcpy(buff, "WMP --");
                    buff = "WMP --";
                } else {
                    buff = String.format("WMP %d", s);
                }
            } else {
                int timeLimit = gameLocal.serverInfo.GetInt("si_timeLimit");
                if (timeLimit != 0) {
                    ms = (timeLimit * 60000) - (gameLocal.time - matchStartedTime);
                } else {
                    ms = gameLocal.time - matchStartedTime;
                }
                if (ms < 0) {
                    ms = 0;
                }

                s = ms / 1000;
                m = s / 60;
                s -= m * 60;
                t = s / 10;
                s -= t * 10;

                buff = String.format("%d:%d%d", m, t, s);
            }
            return buff;
        }

        private void Clear() {
            int i;

            gameState = INACTIVE;
            nextState = INACTIVE;
            pingUpdateTime = 0;
            vote = VOTE_NONE;
            voteTimeOut = 0;
            voteExecTime = 0;
            nextStateSwitch = 0;
            matchStartedTime = 0;
            currentTourneyPlayer[ 0] = -1;
            currentTourneyPlayer[ 1] = -1;
            one = two = three = false;
            playerState = Stream.generate(mpPlayerState_s::new).limit(playerState.length).toArray(mpPlayerState_s[]::new);
            lastWinner = -1;
            currentMenu = 0;
            bCurrentMenuMsg = false;
            nextMenu = 0;
            pureReady = false;
            scoreBoard = null;
            spectateGui = null;
            guiChat = null;
            mainGui = null;
            msgmodeGui = null;
            if (mapList != null) {
                uiManager.FreeListGUI(mapList);
                mapList = null;
            }
            fragLimitTimeout = 0;
//	memset( &switchThrottle, 0, sizeof( switchThrottle ) );
            switchThrottle = new int[switchThrottle.length];
            voiceChatThrottle = 0;
            for (i = 0; i < NUM_CHAT_NOTIFY; i++) {
                chatHistory[ i].line.Clear();
            }
            warmupText = new idStr();
            voteValue = new idStr();
            voteString = new idStr();
            startFragLimit = -1;
        }

        private boolean EnoughClientsToPlay() {
            int[] team = new int[2];
            int clients = NumActualClients(false, team);
            if (gameLocal.gameType == GAME_TDM) {
                return clients >= 2 && team[ 0] != 0 && team[ 1] != 0;
            } else {
                return clients >= 2;
            }
        }

        private void ClearChatData() {
            chatHistoryIndex = 0;
            chatHistorySize = 0;
            chatDataUpdated = true;
        }

        private void DrawChat() {
            int i, j;
            if (guiChat != null) {
                if (gameLocal.time - lastChatLineTime > CHAT_FADE_TIME) {
                    if (chatHistorySize > 0) {
                        for (i = chatHistoryIndex - chatHistorySize; i < chatHistoryIndex; i++) {
                            chatHistory[ i % NUM_CHAT_NOTIFY].fade--;
                            if (chatHistory[ i % NUM_CHAT_NOTIFY].fade < 0) {
                                chatHistorySize--; // this assumes the removals are always at the beginning
                            }
                        }
                        chatDataUpdated = true;
                    }
                    lastChatLineTime = gameLocal.time;
                }
                if (chatDataUpdated) {
                    j = 0;
                    i = chatHistoryIndex - chatHistorySize;
                    while (i < chatHistoryIndex) {
                        guiChat.SetStateString(va("chat%d", j), chatHistory[i % NUM_CHAT_NOTIFY].line.toString());
                        // don't set alpha above 4, the gui only knows that
                        guiChat.SetStateInt(va("alpha%d", j), Min(4, (int) chatHistory[i % NUM_CHAT_NOTIFY].fade));
                        j++;
                        i++;
                    }
                    while (j < NUM_CHAT_NOTIFY) {
                        guiChat.SetStateString(va("chat%d", j), "");
                        j++;
                    }
                    guiChat.Activate(true, gameLocal.time);
                    chatDataUpdated = false;
                }
                guiChat.Redraw(gameLocal.time);
            }
        }

        // go through the clients, and see if they want to be respawned, and if the game allows it
        // called during normal gameplay for death -> respawn cycles
        // and for a spectator who want back in the game (see param)
        private void CheckRespawns(idPlayer spectator /*= NULL*/) {
            for (int i = 0; i < gameLocal.numClients; i++) {
                idEntity ent = gameLocal.entities[ i];
                if (null == ent || !ent.IsType(idPlayer.class)) {
                    continue;
                }
                idPlayer p = (idPlayer) ent;
                // once we hit sudden death, nobody respawns till game has ended
                if (WantRespawn(p) || p == spectator) {
                    if (gameState == SUDDENDEATH && gameLocal.gameType != GAME_LASTMAN) {
                        // respawn rules while sudden death are different
                        // sudden death may trigger while a player is dead, so there are still cases where we need to respawn
                        // don't do any respawns while we are in end game delay though
                        if (0 == fragLimitTimeout) {
                            if (gameLocal.gameType == GAME_TDM || p.IsLeader()) {
                                if (_DEBUG) {
                                    if (gameLocal.gameType == GAME_TOURNEY) {
                                        assert (p.entityNumber == currentTourneyPlayer[ 0] || p.entityNumber == currentTourneyPlayer[ 1]);
                                    }
                                }
                                p.ServerSpectate(false);
                            } else if (!p.IsLeader()) {
                                // sudden death is rolling, this player is not a leader, have him spectate
                                p.ServerSpectate(true);
                                CheckAbortGame();
                            }
                        }
                    } else {
                        if (gameLocal.gameType == GAME_DM
                                || gameLocal.gameType == GAME_TDM) {
                            if (gameState == WARMUP || gameState == COUNTDOWN || gameState == GAMEON) {
                                p.ServerSpectate(false);
                            }
                        } else if (gameLocal.gameType == GAME_TOURNEY) {
                            if (i == currentTourneyPlayer[ 0] || i == currentTourneyPlayer[ 1]) {
                                if (gameState == WARMUP || gameState == COUNTDOWN || gameState == GAMEON) {
                                    p.ServerSpectate(false);
                                }
                            } else if (gameState == WARMUP) {
                                // make sure empty tourney slots get filled first
                                FillTourneySlots();
                                if (i == currentTourneyPlayer[ 0] || i == currentTourneyPlayer[ 1]) {
                                    p.ServerSpectate(false);
                                }
                            }
                        } else if (gameLocal.gameType == GAME_LASTMAN) {
                            if (gameState == WARMUP || gameState == COUNTDOWN) {
                                p.ServerSpectate(false);
                            } else if (gameState == GAMEON || gameState == SUDDENDEATH) {
                                if (gameState == GAMEON && playerState[ i].fragCount > 0 && p.lastManPresent) {
                                    assert (!p.lastManOver);
                                    p.ServerSpectate(false);
                                } else if (p.lastManPlayAgain && p.lastManPresent) {
                                    assert (gameState == SUDDENDEATH);
                                    p.ServerSpectate(false);
                                } else {
                                    // if a fragLimitTimeout was engaged, do NOT mark lastManOver as that could mean
                                    // everyone ends up spectator and game is stalled with no end
                                    // if the frag limit delay is engaged and cancels out before expiring, LMN players are
                                    // respawned to play the tie again ( through SuddenRespawn and lastManPlayAgain )
                                    if (0 == fragLimitTimeout && !p.lastManOver) {
                                        common.DPrintf("client %d has lost all last man lives\n", i);
                                        // end of the game for this guy, send him to spectators
                                        p.lastManOver = true;
                                        // clients don't have access to lastManOver
                                        // so set the fragCount to something silly ( used in scoreboard and player ranking )
                                        playerState[ i].fragCount = LASTMAN_NOLIVES;
                                        p.ServerSpectate(true);

                                        //Check for a situation where the last two player dies at the same time and don't
                                        //try to respawn manually...This was causing all players to go into spectate mode
                                        //and the server got stuck
                                        {
                                            int j;
                                            for (j = 0; j < gameLocal.numClients; j++) {
                                                if (null == gameLocal.entities[ j]) {
                                                    continue;
                                                }
                                                if (!CanPlay((idPlayer) gameLocal.entities[ j])) {
                                                    continue;
                                                }
                                                if (!((idPlayer) gameLocal.entities[ j]).lastManOver) {
                                                    break;
                                                }
                                            }
                                            if (j == gameLocal.numClients) {
                                                //Everyone is dead so don't allow this player to spectate
                                                //so the match will end
                                                p.ServerSpectate(false);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else if (p.wantSpectate && !p.spectating) {
                    playerState[ i].fragCount = 0; // whenever you willingly go spectate during game, your score resets
                    p.ServerSpectate(true);
                    UpdateTourneyLine();
                    CheckAbortGame();
                }
            }
        }

        private void CheckRespawns() {
            CheckRespawns(null);
        }

        private void ForceReady() {

            for (int i = 0; i < gameLocal.numClients; i++) {
                idEntity ent = gameLocal.entities[ i];
                if (null == ent || !ent.IsType(idPlayer.class)) {
                    continue;
                }
                idPlayer p = (idPlayer) ent;
                if (!p.IsReady()) {
                    PrintMessageEvent(-1, MSG_FORCEREADY, i);
                    p.forcedReady = true;
                }
            }
        }

        // when clients disconnect or join spectate during game, check if we need to end the game
        private void CheckAbortGame() {
            int i;
            if (gameLocal.gameType == GAME_TOURNEY && gameState == WARMUP) {
                // if a tourney player joined spectators, let someone else have his spot
                for (i = 0; i < 2; i++) {
                    if (NOT(gameLocal.entities[ currentTourneyPlayer[i]]) || ((idPlayer) gameLocal.entities[ currentTourneyPlayer[ i]]).spectating) {
                        currentTourneyPlayer[ i] = -1;
                    }
                }
            }
            // only checks for aborts . game review below
            if (gameState != COUNTDOWN && gameState != GAMEON && gameState != SUDDENDEATH) {
                return;
            }
            switch (gameLocal.gameType) {
                case GAME_TOURNEY:
                    for (i = 0; i < 2; i++) {
                        if (NOT(gameLocal.entities[ currentTourneyPlayer[i]]) || ((idPlayer) gameLocal.entities[ currentTourneyPlayer[ i]]).spectating) {
                            NewState(GAMEREVIEW);
                            return;
                        }
                    }
                    break;
                default:
                    if (!EnoughClientsToPlay()) {
                        NewState(GAMEREVIEW);
                    }
                    break;
            }
        }

        private void MessageMode(final idCmdArgs args) {
            String mode;
            int imode;

            if (!gameLocal.isMultiplayer) {
                common.Printf("clientMessageMode: only valid in multiplayer\n");
                return;
            }
            if (null == mainGui) {
                common.Printf("no local client\n");
                return;
            }
            mode = args.Argv(1);
            if (!mode.isEmpty()) {
                imode = 0;
            } else {
                imode = Integer.parseInt(mode);
            }
            msgmodeGui.SetStateString("messagemode", imode != 0 ? "1" : "0");
            msgmodeGui.SetStateString("chattext", "");
            nextMenu = 2;
            // let the session know that we want our ingame main menu opened
            gameLocal.sessionCommand.oSet("game_startmenu");
        }

        private void DisableMenu() {
            gameLocal.sessionCommand.oSet("");	// in case we used "game_startMenu" to trigger the menu
            if (currentMenu == 1) {
                mainGui.Activate(false, gameLocal.time);
            } else if (currentMenu == 2) {
                msgmodeGui.Activate(false, gameLocal.time);
            }
            currentMenu = 0;
            nextMenu = 0;
            cvarSystem.SetCVarBool("ui_chat", false);
        }

        private void SetMapShot() {
//            char[] screenshot = new char[MAX_STRING_CHARS];
            String[] screenshot = {null};
            int mapNum = mapList.GetSelection(null, 0);
            idDict dict = null;
            if (mapNum >= 0) {
                dict = fileSystem.GetMapDecl(mapNum);
            }
            fileSystem.FindMapScreenshot(dict != null ? dict.GetString("path") : "", screenshot, MAX_STRING_CHARS);
            mainGui.SetStateString("current_levelshot", screenshot[0]);
        }
        // scores in TDM

        private void TeamScore(int entityNumber, int team, int delta) {
            playerState[ entityNumber].fragCount += delta;
            for (int i = 0; i < gameLocal.numClients; i++) {
                idEntity ent = gameLocal.entities[ i];
                if (null == ent || !ent.IsType(idPlayer.class)) {
                    continue;
                }
                idPlayer player = (idPlayer) ent;
                if (player.team == team) {
                    playerState[ player.entityNumber].teamFragCount += delta;
                }
            }
        }

        private void VoiceChat(final idCmdArgs args, boolean team) {
            idBitMsg outMsg = new idBitMsg();
            ByteBuffer msgBuf = ByteBuffer.allocate(128);
            String voc;
            idDict spawnArgs;
            idKeyValue keyval;
            int index;

            if (!gameLocal.isMultiplayer) {
                common.Printf("clientVoiceChat: only valid in multiplayer\n");
                return;
            }
            if (args.Argc() != 2) {
                common.Printf("clientVoiceChat: bad args\n");
                return;
            }
            // throttle
            if (gameLocal.realClientTime < voiceChatThrottle) {
                return;
            }

            voc = args.Argv(1);
            spawnArgs = gameLocal.FindEntityDefDict("player_doommarine", false);
            keyval = spawnArgs.MatchPrefix("snd_voc_", null);
            index = 0;
            while (keyval != null) {
                if (0 == keyval.GetValue().Icmp(voc)) {
                    break;
                }
                keyval = spawnArgs.MatchPrefix("snd_voc_", keyval);
                index++;
            }
            if (null == keyval) {
                common.Printf("Voice command not found: %s\n", voc);
                return;
            }
            voiceChatThrottle = gameLocal.realClientTime + 1000;

            outMsg.Init(msgBuf, msgBuf.capacity());
            outMsg.WriteByte(GAME_RELIABLE_MESSAGE_VCHAT);
            outMsg.WriteLong(index);
            outMsg.WriteBits(team ? 1 : 0, 1);
            networkSystem.ClientSendReliableMessage(outMsg);
        }

        private void DumpTourneyLine() {
            int i;
            for (i = 0; i < gameLocal.numClients; i++) {
                if (gameLocal.entities[ i] != null && gameLocal.entities[ i].IsType(idPlayer.class)) {
                    common.Printf("client %d: rank %d\n", i, ((idPlayer) gameLocal.entities[ i]).tourneyRank);
                }
            }
        }

        /*
         ================
         idMultiplayerGame::SuddenRespawns
         solely for LMN if an end game ( fragLimitTimeout ) was entered and aborted before expiration
         LMN players which still have lives left need to be respawned without being marked lastManOver
         ================
         */
        private void SuddenRespawn() {
            int i;

            if (gameLocal.gameType != GAME_LASTMAN) {
                return;
            }

            for (i = 0; i < gameLocal.numClients; i++) {
                if (null == gameLocal.entities[ i] || !gameLocal.entities[ i].IsType(idPlayer.class)) {
                    continue;
                }
                if (!CanPlay((idPlayer) gameLocal.entities[ i])) {
                    continue;
                }
                if (((idPlayer) gameLocal.entities[ i]).lastManOver) {
                    continue;
                }
                ((idPlayer) gameLocal.entities[ i]).lastManPlayAgain = true;
            }
        }
    };
}
