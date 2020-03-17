package neo.Game.GameSys;

import static java.lang.Math.tan;
import static neo.Game.Entity.EV_Activate;
import static neo.Game.Entity.signalNum_t.SIG_TRIGGER;
import static neo.Game.Game.SCRIPT_DEFAULT;
import static neo.Game.GameSys.Class.EV_Remove;
import static neo.Game.GameSys.SysCvar.aas_test;
import static neo.Game.GameSys.SysCvar.g_testDeath;
import static neo.Game.GameSys.SysCvar.pm_normalviewheight;
import static neo.Game.Game_local.GAME_RELIABLE_MESSAGE_CHAT;
import static neo.Game.Game_local.GAME_RELIABLE_MESSAGE_KILL;
import static neo.Game.Game_local.GAME_RELIABLE_MESSAGE_TCHAT;
import static neo.Game.Game_local.MAX_GAME_MESSAGE_SIZE;
import static neo.Game.Game_local.MAX_GENTITIES;
import static neo.Game.Game_local.animationLib;
import static neo.Game.Game_local.gameLocal;
import static neo.Game.Game_local.gameRenderWorld;
import static neo.Game.Player.BERSERK;
import static neo.Game.Player.EV_Player_SelectWeapon;
import static neo.Game.Player.INVISIBILITY;
import static neo.Game.Player.MAX_WEAPONS;
import static neo.Game.Weapon.AMMO_NUMTYPES;
import static neo.Renderer.Model.INVALID_JOINT;
import static neo.Renderer.RenderWorld.MAX_RENDERENTITY_GUI;
import static neo.TempDump.NOT;
import static neo.framework.Async.NetworkSystem.networkSystem;
import static neo.framework.CmdSystem.cmdSystem;
import static neo.framework.CmdSystem.cmdExecution_t.CMD_EXEC_NOW;
import static neo.framework.Common.STRTABLE_ID;
import static neo.framework.Common.STRTABLE_ID_LENGTH;
import static neo.framework.Common.common;
import static neo.framework.FileSystem_h.fileSystem;
import static neo.idlib.Lib.BIT;
import static neo.idlib.Lib.idLib.cvarSystem;
import static neo.idlib.Text.Lexer.LEXFL_ALLOWPATHNAMES;
import static neo.idlib.Text.Lexer.LEXFL_NOFATALERRORS;
import static neo.idlib.Text.Lexer.LEXFL_NOSTRINGCONCAT;
import static neo.idlib.Text.Lexer.LEXFL_NOSTRINGESCAPECHARS;
import static neo.idlib.Text.Str.S_COLOR_WHITE;
import static neo.idlib.Text.Str.va;
import static neo.idlib.Text.Str.idStr.parseStr;
import static neo.idlib.math.Math_h.DEG2RAD;
import static neo.idlib.math.Math_h.SEC2MS;
import static neo.idlib.math.Matrix.idMat3.getMat3_default;

import java.nio.ByteBuffer;
import java.util.stream.Stream;

import neo.TempDump.void_callback;
import neo.CM.CollisionModel_local;
import neo.Game.AFEntity.idAFEntity_Base;
import neo.Game.AFEntity.idAFEntity_Generic;
import neo.Game.AFEntity.idAFEntity_WithAttachedHead;
import neo.Game.Entity.idEntity;
import neo.Game.FX.idEntityFx;
import neo.Game.Light.idLight;
import neo.Game.Misc.idStaticEntity;
import neo.Game.Moveable.idMoveable;
import neo.Game.Player.idPlayer;
import neo.Game.Projectile.idProjectile;
import neo.Game.Weapon.idWeapon;
import neo.Game.AI.AAS.idAAS;
import neo.Game.AI.AI.idAI;
import neo.Game.Animation.Anim.idAnimManager;
import neo.Game.Animation.Anim_Blend.idAnimator;
import neo.Game.Animation.Anim_Import.idModelExport;
import neo.Game.Script.Script_Program.function_t;
import neo.Game.Script.Script_Thread.idThread;
import neo.Renderer.Material.idMaterial;
import neo.Renderer.Model.idRenderModel;
import neo.Renderer.Model.modelSurface_s;
import neo.Renderer.Model.srfTriangles_s;
import neo.Renderer.RenderWorld.renderEntity_s;
import neo.Renderer.RenderWorld.renderView_s;
import neo.framework.CmdSystem.argCompletion_t;
import neo.framework.CmdSystem.cmdFunction_t;
import neo.framework.File_h.idFile;
import neo.idlib.BitMsg.idBitMsg;
import neo.idlib.CmdArgs.idCmdArgs;
import neo.idlib.Dict_h.idDict;
import neo.idlib.Dict_h.idKeyValue;
import neo.idlib.MapFile.idMapEntity;
import neo.idlib.MapFile.idMapFile;
import neo.idlib.Text.Lexer.idLexer;
import neo.idlib.Text.Str.idStr;
import neo.idlib.Text.Token.idToken;
import neo.idlib.containers.StrList.idStrList;
import neo.idlib.math.Angles.idAngles;
import neo.idlib.math.Math_h.idMath;
import neo.idlib.math.Vector.idVec3;
import neo.idlib.math.Vector.idVec4;
import neo.idlib.math.Matrix.idMat3;
import neo.idlib.math.Matrix.idMat4;

/**
 *
 */
public class SysCmds {

    /*
     ==================
     Cmd_GetFloatArg
     ==================
     */
    public static float Cmd_GetFloatArg(final idCmdArgs args, int[] argNum) {
        String value;

        value = args.Argv(argNum[0]++);
        return Float.parseFloat(value);
    }

    /*
     ===================
     Cmd_EntityList_f
     ===================
     */
    public static class Cmd_EntityList_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new Cmd_EntityList_f();

        private Cmd_EntityList_f() {
        }

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) {
            int e;
            idEntity check;
            int count;
            int/*size_t*/ size;
            String match;

            if (args.Argc() > 1) {
                match = args.Args();
                match = match.replaceAll(" ", "");
            } else {
                match = "";
            }

            count = 0;
            size = 0;

            gameLocal.Printf("%-4s  %-20s %-20s %s\n", " Num", "EntityDef", "Class", "Name");
            gameLocal.Printf("--------------------------------------------------------------------\n");
            for (e = 0; e < MAX_GENTITIES; e++) {
                check = gameLocal.entities[ e];

                if (null == check) {
                    continue;
                }

                if (!check.name.Filter(match, true)) {
                    continue;
                }

                gameLocal.Printf("%4d: %-20s %-20s %s\n", e,
                        check.GetEntityDefName(), check.GetClassname(), check.name);

                count++;
                size += check.spawnArgs.Allocated();
            }

            gameLocal.Printf("...%d entities\n...%d bytes of spawnargs\n", count, size);
        }
    };

    /*
     ===================
     Cmd_ActiveEntityList_f
     ===================
     */
    public static class Cmd_ActiveEntityList_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new Cmd_ActiveEntityList_f();

        private Cmd_ActiveEntityList_f() {
        }

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) {
            idEntity check;
            int count;

            count = 0;

            gameLocal.Printf("%-4s  %-20s %-20s %s\n", " Num", "EntityDef", "Class", "Name");
            gameLocal.Printf("--------------------------------------------------------------------\n");
            for (check = gameLocal.activeEntities.Next(); check != null; check = check.activeNode.Next()) {
                char dormant = check.fl.isDormant ? '-' : ' ';
                gameLocal.Printf("%4d:%c%-20s %-20s %s\n", check.entityNumber, dormant, check.GetEntityDefName(), check.GetClassname(), check.name);
                count++;
            }

            gameLocal.Printf("...%d active entities\n", count);
        }
    };

    /*
     ===================
     Cmd_ListSpawnArgs_f
     ===================
     */
    public static class Cmd_ListSpawnArgs_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new Cmd_ListSpawnArgs_f();

        private Cmd_ListSpawnArgs_f() {
        }

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) {
            int i;
            idEntity ent;

            ent = gameLocal.FindEntity(args.Argv(1));
            if (null == ent) {
                gameLocal.Printf("entity not found\n");
                return;
            }

            for (i = 0; i < ent.spawnArgs.GetNumKeyVals(); i++) {
                final idKeyValue kv = ent.spawnArgs.GetKeyVal(i);
                gameLocal.Printf("\"%s\"  " + S_COLOR_WHITE + "\"%s\"\n", kv.GetKey(), kv.GetValue());
            }
        }
    };

    /*
     ===================
     Cmd_ReloadScript_f
     ===================
     */
    public static class Cmd_ReloadScript_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new Cmd_ReloadScript_f();

        private Cmd_ReloadScript_f() {
        }

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) {
            // shutdown the map because entities may point to script objects
            gameLocal.MapShutdown();

            // recompile the scripts
            gameLocal.program.Startup(SCRIPT_DEFAULT);

            // error out so that the user can rerun the scripts
            gameLocal.Error("Exiting map to reload scripts");
        }
    };

    /*
     ===================
     Cmd_Script_f
     ===================
     */
    public static class Cmd_Script_f extends cmdFunction_t {

        private static int funcCount = 0;
        private static final cmdFunction_t instance = new Cmd_Script_f();

        private Cmd_Script_f() {
        }

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) {
            String script;
            String text;
            String funcname;
            idThread thread;
            function_t func;
            idEntity ent;

            if (!gameLocal.CheatsOk()) {
                return;
            }

            funcname = String.format("ConsoleFunction_%d", funcCount++);

            script = args.Args();
            text = String.format("void %s() {%s;}\n", funcname, script);
            if (gameLocal.program.CompileText("console", text, true)) {
                func = gameLocal.program.FindFunction(funcname);
                if (func != null) {
                    // set all the entity names in case the user named one in the script that wasn't referenced in the default script
                    for (ent = gameLocal.spawnedEntities.Next(); ent != null; ent = ent.spawnNode.Next()) {
                        gameLocal.program.SetEntity(ent.name.getData(), ent);
                    }

                    thread = new idThread(func);
                    thread.Start();
                }
            }
        }
    };

    /*
     ==================
     KillEntities

     Kills all the entities of the given class in a level.
     ==================
     */
    static void KillEntities(final idCmdArgs args, final java.lang.Class/*idTypeInfo*/ superClass) {
        idEntity ent;
        idStrList ignore = new idStrList();
        String name;
        int i;

        if (NOT(gameLocal.GetLocalPlayer()) || !gameLocal.CheatsOk(false)) {
            return;
        }

        for (i = 1; i < args.Argc(); i++) {
            name = args.Argv(i);
            ignore.Append(parseStr(name));
        }

        for (ent = gameLocal.spawnedEntities.Next(); ent != null; ent = ent.spawnNode.Next()) {
            if (ent.IsType(superClass)) {
                for (i = 0; i < ignore.Num(); i++) {
                    if (ignore.oGet(i).equals(ent.name)) {
                        break;
                    }
                }

                if (i >= ignore.Num()) {
                    ent.PostEventMS(EV_Remove, 0);
                }
            }
        }
    }

    /*
     ==================
     Cmd_KillMonsters_f

     Kills all the monsters in a level.
     ==================
     */
    public static class Cmd_KillMonsters_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new Cmd_KillMonsters_f();

        private Cmd_KillMonsters_f() {
        }

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) {

            KillEntities(args, idAI.class);

            // kill any projectiles as well since they have pointers to the monster that created them
            KillEntities(args, idProjectile.class);
        }
    };

    /*
     ==================
     Cmd_KillMovables_f

     Kills all the moveables in a level.
     ==================
     */
    public static class Cmd_KillMovables_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new Cmd_KillMovables_f();

        private Cmd_KillMovables_f() {
        }

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) {
            if (NOT(gameLocal.GetLocalPlayer()) || !gameLocal.CheatsOk(false)) {
                return;
            }
            KillEntities(args, idMoveable.class);
        }
    };

    /*
     ==================
     Cmd_KillRagdolls_f

     Kills all the ragdolls in a level.
     ==================
     */
    public static class Cmd_KillRagdolls_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new Cmd_KillRagdolls_f();

        private Cmd_KillRagdolls_f() {
        }

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) {
            if (NOT(gameLocal.GetLocalPlayer()) || !gameLocal.CheatsOk(false)) {
                return;
            }
            KillEntities(args, idAFEntity_Generic.class);
            KillEntities(args, idAFEntity_WithAttachedHead.class);
        }
    };

    /*
     ==================
     Cmd_Give_f

     Give items to a client
     ==================
     */
    public static class Cmd_Give_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new Cmd_Give_f();

        private Cmd_Give_f() {
        }

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) {
            String name;
            int i;
            boolean give_all;
            idPlayer player;

            player = gameLocal.GetLocalPlayer();
            if (player == null || !gameLocal.CheatsOk()) {
                return;
            }

            name = args.Argv(1);

            if (idStr.Icmp(name, "all") == 0) {
                give_all = true;
            } else {
                give_all = false;
            }

            if (give_all || (idStr.Cmpn(name, "weapon", 6) == 0)) {
                if (gameLocal.world.spawnArgs.GetBool("no_Weapons")) {
                    gameLocal.world.spawnArgs.SetBool("no_Weapons", false);
                    for (i = 0; i < gameLocal.numClients; i++) {
                        if (gameLocal.entities[i] != null) {
                            gameLocal.entities[i].PostEventSec(EV_Player_SelectWeapon, 0.5f, gameLocal.entities[ i].spawnArgs.GetString("def_weapon1"));
                        }
                    }
                }
            }

            if ((idStr.Cmpn(name, "weapon_", 7) == 0) || (idStr.Cmpn(name, "item_", 5) == 0) || (idStr.Cmpn(name, "ammo_", 5) == 0)) {
                player.GiveItem(name);
                return;
            }

            if (give_all || idStr.Icmp(name, "health") == 0) {
                player.health = player.inventory.maxHealth;
                if (!give_all) {
                    return;
                }
            }

            if (give_all || idStr.Icmp(name, "weapons") == 0) {
                player.inventory.weapons = BIT(MAX_WEAPONS) - 1;
                player.CacheWeapons();

                if (!give_all) {
                    return;
                }
            }

            if (give_all || idStr.Icmp(name, "ammo") == 0) {
                for (i = 0; i < AMMO_NUMTYPES; i++) {
                    player.inventory.ammo[ i] = player.inventory.MaxAmmoForAmmoClass(player, idWeapon.GetAmmoNameForNum(i));
                }
                if (!give_all) {
                    return;
                }
            }

            if (give_all || idStr.Icmp(name, "armor") == 0) {
                player.inventory.armor = player.inventory.maxarmor;
                if (!give_all) {
                    return;
                }
            }

            if (idStr.Icmp(name, "berserk") == 0) {
                player.GivePowerUp(BERSERK, (int) SEC2MS(30.0f));
                return;
            }

            if (idStr.Icmp(name, "invis") == 0) {
                player.GivePowerUp(INVISIBILITY, (int) SEC2MS(30.0f));
                return;
            }

            if (idStr.Icmp(name, "pda") == 0) {
                player.GivePDA(parseStr(args.Argv(2)), null);
                return;
            }

            if (idStr.Icmp(name, "video") == 0) {
                player.GiveVideo(args.Argv(2), null);
                return;
            }

            if (!give_all && !player.Give(args.Argv(1), args.Argv(2))) {
                gameLocal.Printf("unknown item\n");
            }
        }
    };

    /*
     ==================
     Cmd_CenterView_f

     Centers the players pitch
     ==================
     */
    public static class Cmd_CenterView_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new Cmd_CenterView_f();

        private Cmd_CenterView_f() {
        }

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) {
            idPlayer player;
            idAngles ang;

            player = gameLocal.GetLocalPlayer();
            if (player == null) {
                return;
            }

            ang = new idAngles(player.viewAngles);
            ang.pitch = 0.0f;
            player.SetViewAngles(ang);
        }
    };

    /*
     ==================
     Cmd_God_f

     Sets client to godmode

     argv(0) god
     ==================
     */
    public static class Cmd_God_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new Cmd_God_f();

        private Cmd_God_f() {
        }

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) {
            String msg;
            idPlayer player;

            player = gameLocal.GetLocalPlayer();
            if (player == null || !gameLocal.CheatsOk()) {
                return;
            }

            if (player.godmode) {
                player.godmode = false;
                msg = "godmode OFF\n";
            } else {
                player.godmode = true;
                msg = "godmode ON\n";
            }

            gameLocal.Printf("%s", msg);
        }
    };

    /*
     ==================
     Cmd_Notarget_f

     Sets client to notarget

     argv(0) notarget
     ==================
     */
    public static class Cmd_Notarget_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new Cmd_Notarget_f();

        private Cmd_Notarget_f() {
        }

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) {
            String msg;
            idPlayer player;

            player = gameLocal.GetLocalPlayer();
            if (player == null || !gameLocal.CheatsOk()) {
                return;
            }

            if (player.fl.notarget) {
                player.fl.notarget = false;
                msg = "notarget OFF\n";
            } else {
                player.fl.notarget = true;
                msg = "notarget ON\n";
            }

            gameLocal.Printf("%s", msg);
        }
    };

    /*
     ==================
     Cmd_Noclip_f

     argv(0) noclip
     ==================
     */
    public static class Cmd_Noclip_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new Cmd_Noclip_f();

        private Cmd_Noclip_f() {
        }

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) {
            String msg;
            idPlayer player;

            player = gameLocal.GetLocalPlayer();
            if (player == null || !gameLocal.CheatsOk()) {
                return;
            }

            if (player.noclip) {
                msg = "noclip OFF\n";
            } else {
                msg = "noclip ON\n";
            }
            player.noclip = !player.noclip;

            gameLocal.Printf("%s", msg);
        }
    };

    /*
     =================
     Cmd_Kill_f
     =================
     */
    public static class Cmd_Kill_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new Cmd_Kill_f();

        private Cmd_Kill_f() {
        }

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) {
            idPlayer player;

            if (gameLocal.isMultiplayer) {
                if (gameLocal.isClient) {
                    idBitMsg outMsg = new idBitMsg();
                    ByteBuffer msgBuf = ByteBuffer.allocate(MAX_GAME_MESSAGE_SIZE);
                    outMsg.Init(msgBuf, msgBuf.capacity());
                    outMsg.WriteByte(GAME_RELIABLE_MESSAGE_KILL);
                    networkSystem.ClientSendReliableMessage(outMsg);
                } else {
                    player = gameLocal.GetClientByCmdArgs(args);
                    if (player == null) {
                        common.Printf("kill <client nickname> or kill <client index>\n");
                        return;
                    }
                    player.Kill(false, false);
                    cmdSystem.BufferCommandText(CMD_EXEC_NOW, va("say killed client %d '%s^0'\n", player.entityNumber, gameLocal.userInfo[ player.entityNumber].GetString("ui_name")));
                }
            } else {
                player = gameLocal.GetLocalPlayer();
                if (player == null) {
                    return;
                }
                player.Kill(false, false);
            }
        }
    };

    /*
     =================
     Cmd_PlayerModel_f
     =================
     */
    public static class Cmd_PlayerModel_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new Cmd_PlayerModel_f();

        private Cmd_PlayerModel_f() {
        }

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) {
            idPlayer player;
            String name;
            idVec3 pos;
            idAngles ang;

            player = gameLocal.GetLocalPlayer();
            if (player == null || !gameLocal.CheatsOk()) {
                return;
            }

            if (args.Argc() < 2) {
                gameLocal.Printf("usage: playerModel <modelname>\n");
                return;
            }

            name = args.Argv(1);
            player.spawnArgs.Set("model", name);

            pos = player.GetPhysics().GetOrigin();
            ang = new idAngles(player.viewAngles);
            player.SpawnToPoint(pos, ang);
        }
    };

    /*
     ==================
     Cmd_Say
     ==================
     */
    static void Cmd_Say(boolean team, final idCmdArgs args) {
        String name;
        idStr text;
        final String cmd = team ? "sayTeam" : "say";

        if (!gameLocal.isMultiplayer) {
            gameLocal.Printf("%s can only be used in a multiplayer game\n", cmd);
            return;
        }

        if (args.Argc() < 2) {
            gameLocal.Printf("usage: %s <text>\n", cmd);
            return;
        }

        text = new idStr(args.Args());
        if (text.Length() == 0) {
            return;
        }

        if (text.oGet(text.Length() - 1) == '\n') {
            text.oSet(text.Length() - 1, '\0');
        }
        name = "player";

        idPlayer player;

        // here we need to special case a listen server to use the real client name instead of "server"
        // "server" will only appear on a dedicated server
        if (gameLocal.isClient || cvarSystem.GetCVarInteger("net_serverDedicated") == 0) {
            player = gameLocal.localClientNum >= 0 ? (idPlayer) gameLocal.entities[ gameLocal.localClientNum] : null;
            if (player != null) {
                name = player.GetUserInfo().GetString("ui_name", "player");
            }
        } else {
            name = "server";
        }

        if (gameLocal.isClient) {
            idBitMsg outMsg = new idBitMsg();
            ByteBuffer msgBuf = ByteBuffer.allocate(256);
            outMsg.Init(msgBuf, msgBuf.capacity());
            outMsg.WriteByte(team ? GAME_RELIABLE_MESSAGE_TCHAT : GAME_RELIABLE_MESSAGE_CHAT);
            outMsg.WriteString(name);
            outMsg.WriteString(text.getData(), -1, false);
            networkSystem.ClientSendReliableMessage(outMsg);
        } else {
            gameLocal.mpGame.ProcessChatMessage(gameLocal.localClientNum, team, name, text.getData(), null);
        }
    }

    /*
     ==================
     Cmd_Say_f
     ==================
     */
    public static class Cmd_Say_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new Cmd_Say_f();

        private Cmd_Say_f() {
        }

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) {
            Cmd_Say(false, args);
        }
    };

    /*
     ==================
     Cmd_SayTeam_f
     ==================
     */
    public static class Cmd_SayTeam_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new Cmd_SayTeam_f();

        private Cmd_SayTeam_f() {
        }

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) {
            Cmd_Say(true, args);
        }
    };

    /*
     ==================
     Cmd_AddChatLine_f
     ==================
     */
    public static class Cmd_AddChatLine_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new Cmd_AddChatLine_f();

        private Cmd_AddChatLine_f() {
        }

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) {
            gameLocal.mpGame.AddChatLine(args.Argv(1));
        }
    };

    /*
     ==================
     Cmd_Kick_f
     ==================
     */
    public static class Cmd_Kick_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new Cmd_Kick_f();

        private Cmd_Kick_f() {
        }

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) {
            idPlayer player;

            if (!gameLocal.isMultiplayer) {
                gameLocal.Printf("kick can only be used in a multiplayer game\n");
                return;
            }

            if (gameLocal.isClient) {
                gameLocal.Printf("You have no such power. This is a server command\n");
                return;
            }

            player = gameLocal.GetClientByCmdArgs(args);
            if (player == null) {
                gameLocal.Printf("usage: kick <client nickname> or kick <client index>\n");
                return;
            }
            cmdSystem.BufferCommandText(CMD_EXEC_NOW, va("say kicking out client %d '%s^0'\n", player.entityNumber, gameLocal.userInfo[ player.entityNumber].GetString("ui_name")));
            cmdSystem.BufferCommandText(CMD_EXEC_NOW, va("kick %d\n", player.entityNumber));
        }
    };

    /*
     ==================
     Cmd_GetViewpos_f
     ==================
     */
    public static class Cmd_GetViewpos_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new Cmd_GetViewpos_f();

        private Cmd_GetViewpos_f() {
        }

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) {
            idPlayer player;
            idVec3 origin = new idVec3();
            idMat3 axis = new idMat3();

            player = gameLocal.GetLocalPlayer();
            if (player == null) {
                return;
            }

            final renderView_s view = player.GetRenderView();
            if (view != null) {
                gameLocal.Printf("(%s) %.1f\n", view.vieworg.ToString(), view.viewaxis.oGet(0).ToYaw());
            } else {
                player.GetViewPos(origin, axis);
                gameLocal.Printf("(%s) %.1f\n", origin.ToString(), axis.oGet(0).ToYaw());
            }
        }
    };

    /*
     =================
     Cmd_SetViewpos_f
     =================
     */
    public static class Cmd_SetViewpos_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new Cmd_SetViewpos_f();

        private Cmd_SetViewpos_f() {
        }

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) {
            idVec3 origin = new idVec3();
            idAngles angels = new idAngles();
            int i;
            idPlayer player;

            player = gameLocal.GetLocalPlayer();
            if (player == null || !gameLocal.CheatsOk()) {
                return;
            }

            if ((args.Argc() != 4) && (args.Argc() != 5)) {
                gameLocal.Printf("usage: setviewpos <x> <y> <z> <yaw>\n");
                return;
            }

            angels.Zero();
            if (args.Argc() == 5) {
                angels.yaw = Float.parseFloat(args.Argv(4));
            }

            for (i = 0; i < 3; i++) {
                origin.oSet(i, Float.parseFloat(args.Argv(i + 1)));
            }
            origin.z -= pm_normalviewheight.GetFloat() - 0.25f;

            player.Teleport(origin, angels, null);
        }
    };

    /*
     =================
     Cmd_Teleport_f
     =================
     */
    public static class Cmd_Teleport_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new Cmd_Teleport_f();

        private Cmd_Teleport_f() {
        }

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) {
            idVec3 origin;
            idAngles angles = new idAngles();
            idPlayer player;
            idEntity ent;

            player = gameLocal.GetLocalPlayer();
            if (player == null || !gameLocal.CheatsOk()) {
                return;
            }

            if (args.Argc() != 2) {
                gameLocal.Printf("usage: teleport <name of entity to teleport to>\n");
                return;
            }

            ent = gameLocal.FindEntity(args.Argv(1));
            if (NOT(ent)) {
                gameLocal.Printf("entity not found\n");
                return;
            }

            angles.Zero();
            angles.yaw = ent.GetPhysics().GetAxis().oGet(0).ToYaw();
            origin = ent.GetPhysics().GetOrigin();

            player.Teleport(origin, angles, ent);
        }
    };

    /*
     =================
     Cmd_Trigger_f
     =================
     */
    public static class Cmd_Trigger_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new Cmd_Trigger_f();

        private Cmd_Trigger_f() {
        }

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) {
            idVec3 origin;
            idAngles angles;
            idPlayer player;
            idEntity ent;

            player = gameLocal.GetLocalPlayer();
            if (player == null || !gameLocal.CheatsOk()) {
                return;
            }

            if (args.Argc() != 2) {
                gameLocal.Printf("usage: trigger <name of entity to trigger>\n");
                return;
            }

            ent = gameLocal.FindEntity(args.Argv(1));
            if (NOT(ent)) {
                gameLocal.Printf("entity not found\n");
                return;
            }

            ent.Signal(SIG_TRIGGER);
            ent.ProcessEvent(EV_Activate, player);
            ent.TriggerGuis();
        }
    };

    /*
     ===================
     Cmd_Spawn_f
     ===================
     */
    public static class Cmd_Spawn_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new Cmd_Spawn_f();

        private Cmd_Spawn_f() {
        }

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) {
            String key, value;
            int i;
            float yaw;
            idVec3 org;
            idPlayer player;
            idDict dict = new idDict();

            player = gameLocal.GetLocalPlayer();
            if (player == null || !gameLocal.CheatsOk(false)) {
                return;
            }

            if ((args.Argc() & 1) != 0) {	// must always have an even number of arguments
                gameLocal.Printf("usage: spawn classname [key/value pairs]\n");
                return;
            }

            yaw = player.viewAngles.yaw;

            value = args.Argv(1);
            dict.Set("classname", value);
            dict.Set("angle", va("%f", yaw + 180));

            org = player.GetPhysics().GetOrigin().oPlus(new idAngles(0, yaw, 0).ToForward().oMultiply(80).oPlus(new idVec3(0, 0, 1)));
            dict.Set("origin", org.ToString());

            for (i = 2; i < args.Argc() - 1; i += 2) {

                key = args.Argv(i);
                value = args.Argv(i + 1);

                dict.Set(key, value);
            }

            gameLocal.SpawnEntityDef(dict);
        }
    };

    /*
     ==================
     Cmd_Damage_f

     Damages the specified entity
     ==================
     */
    public static class Cmd_Damage_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new Cmd_Damage_f();

        private Cmd_Damage_f() {
        }

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) {
            if (NOT(gameLocal.GetLocalPlayer()) || !gameLocal.CheatsOk(false)) {
                return;
            }
            if (args.Argc() != 3) {
                gameLocal.Printf("usage: damage <name of entity to damage> <damage>\n");
                return;
            }

            idEntity ent = gameLocal.FindEntity(args.Argv(1));
            if (NOT(ent)) {
                gameLocal.Printf("entity not found\n");
                return;
            }

            ent.Damage(gameLocal.world, gameLocal.world, new idVec3(0, 0, 1), "damage_moverCrush", Integer.parseInt(args.Argv(2)), INVALID_JOINT);
        }
    };


    /*
     ==================
     Cmd_Remove_f

     Removes the specified entity
     ==================
     */
    public static class Cmd_Remove_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new Cmd_Remove_f();

        private Cmd_Remove_f() {
        }

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) {
            if (NOT(gameLocal.GetLocalPlayer()) || !gameLocal.CheatsOk(false)) {
                return;
            }
            if (args.Argc() != 2) {
                gameLocal.Printf("usage: remove <name of entity to remove>\n");
                return;
            }

            idEntity ent = gameLocal.FindEntity(args.Argv(1));
            if (NOT(ent)) {
                gameLocal.Printf("entity not found\n");
                return;
            }

//        delete ent;
        }
    };

    /*
     ===================
     Cmd_TestLight_f
     ===================
     */
    public static class Cmd_TestLight_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new Cmd_TestLight_f();

        private Cmd_TestLight_f() {
        }

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) {
            int i;
            idStr filename = new idStr();
            String key, value, name = null;
            idPlayer player;
            idDict dict = new idDict();

            player = gameLocal.GetLocalPlayer();
            if (player == null || !gameLocal.CheatsOk(false)) {
                return;
            }

            renderView_s rv = player.GetRenderView();

            float fov = (float) tan(idMath.M_DEG2RAD * rv.fov_x / 2);

            dict.SetMatrix("rotation", getMat3_default());
            dict.SetVector("origin", rv.vieworg);
            dict.SetVector("light_target", rv.viewaxis.oGet(0));
            dict.SetVector("light_right", rv.viewaxis.oGet(1).oMultiply(-fov));
            dict.SetVector("light_up", rv.viewaxis.oGet(2).oMultiply(fov));
            dict.SetVector("light_start", rv.viewaxis.oGet(0).oMultiply(16));
            dict.SetVector("light_end", rv.viewaxis.oGet(0).oMultiply(1000));

            if (args.Argc() >= 2) {
                value = args.Argv(1);
                filename.oSet(args.Argv(1));
                filename.DefaultFileExtension(".tga");
                dict.Set("texture", filename);
            }

            dict.Set("classname", "light");
            for (i = 2; i < args.Argc() - 1; i += 2) {

                key = args.Argv(i);
                value = args.Argv(i + 1);

                dict.Set(key, value);
            }

            for (i = 0; i < MAX_GENTITIES; i++) {
                name = va("spawned_light_%d", i);		// not just light_, or it might pick up a prelight shadow
                if (NOT(gameLocal.FindEntity(name))) {
                    break;
                }
            }
            dict.Set("name", name);

            gameLocal.SpawnEntityDef(dict);

            gameLocal.Printf("Created new light\n");
        }
    };

    /*
     ===================
     Cmd_TestPointLight_f
     ===================
     */
    public static class Cmd_TestPointLight_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new Cmd_TestPointLight_f();

        private Cmd_TestPointLight_f() {
        }

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) {
            String key, value, name = null;
            int i;
            idPlayer player;
            idDict dict = new idDict();

            player = gameLocal.GetLocalPlayer();
            if (player == null || !gameLocal.CheatsOk(false)) {
                return;
            }

            dict.SetVector("origin", player.GetRenderView().vieworg);

            if (args.Argc() >= 2) {
                value = args.Argv(1);
                dict.Set("light", value);
            } else {
                dict.Set("light", "300");
            }

            dict.Set("classname", "light");
            for (i = 2; i < args.Argc() - 1; i += 2) {

                key = args.Argv(i);
                value = args.Argv(i + 1);

                dict.Set(key, value);
            }

            for (i = 0; i < MAX_GENTITIES; i++) {
                name = va("light_%d", i);
                if (NOT(gameLocal.FindEntity(name))) {
                    break;
                }
            }
            dict.Set("name", name);

            gameLocal.SpawnEntityDef(dict);

            gameLocal.Printf("Created new point light\n");
        }
    };

    /*
     ==================
     Cmd_PopLight_f
     ==================
     */
    public static class Cmd_PopLight_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new Cmd_PopLight_f();

        private Cmd_PopLight_f() {
        }

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) {
            idEntity ent;
            idMapEntity mapEnt;
            idMapFile mapFile = gameLocal.GetLevelMap();
            idLight lastLight;
            int last;

            if (!gameLocal.CheatsOk()) {
                return;
            }

            boolean removeFromMap = (args.Argc() > 1);

            lastLight = null;
            last = -1;
            for (ent = gameLocal.spawnedEntities.Next(); ent != null; ent = ent.spawnNode.Next()) {
                if (!ent.IsType(idLight.class)) {
                    continue;
                }

                if (gameLocal.spawnIds[ ent.entityNumber] > last) {
                    last = gameLocal.spawnIds[ ent.entityNumber];
                    lastLight = (idLight) ent;
                }
            }

            if (lastLight != null) {
                // find map file entity
                mapEnt = mapFile.FindEntity(lastLight.name.getData());

                if (removeFromMap && mapEnt != null) {
                    mapFile.RemoveEntity(mapEnt);
                }
                gameLocal.Printf("Removing light %d\n", lastLight.GetLightDefHandle());
//            delete lastLight;
            } else {
                gameLocal.Printf("No lights to clear.\n");
            }
        }
    };

    /*
     ====================
     Cmd_ClearLights_f
     ====================
     */
    public static class Cmd_ClearLights_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new Cmd_ClearLights_f();

        private Cmd_ClearLights_f() {
        }

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) {
            idEntity ent;
            idEntity next;
            idLight light;
            idMapEntity mapEnt;
            idMapFile mapFile = gameLocal.GetLevelMap();

            boolean removeFromMap = (args.Argc() > 1);

            gameLocal.Printf("Clearing all lights.\n");
            for (ent = gameLocal.spawnedEntities.Next(); ent != null; ent = next) {
                next = ent.spawnNode.Next();
                if (!ent.IsType(idLight.class)) {
                    continue;
                }

                light = (idLight) ent;
                mapEnt = mapFile.FindEntity(light.name.getData());

                if (removeFromMap && mapEnt != null) {
                    mapFile.RemoveEntity(mapEnt);
                }

//            delete light;
            }
        }
    };

    /*
     ==================
     Cmd_TestFx_f
     ==================
     */
    public static class Cmd_TestFx_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new Cmd_TestFx_f();

        private Cmd_TestFx_f() {
        }

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) {
            idVec3 offset;
            String name;
            idPlayer player;
            idDict dict = new idDict();

            player = gameLocal.GetLocalPlayer();
            if (player == null || !gameLocal.CheatsOk()) {
                return;
            }

            // delete the testModel if active
            if (gameLocal.testFx != null) {
//            delete gameLocal.testFx;
                gameLocal.testFx = null;
            }

            if (args.Argc() < 2) {
                return;
            }

            name = args.Argv(1);

            offset = player.GetPhysics().GetOrigin().oPlus(player.viewAngles.ToForward().oMultiply(100.0f));

            dict.Set("origin", offset.ToString());
            dict.Set("test", "1");
            dict.Set("fx", name);
            gameLocal.testFx = (idEntityFx) gameLocal.SpawnEntityType(idEntityFx.class, dict);
        }
    };

    static final int MAX_DEBUGLINES = 128;

    public static class gameDebugLine_t {

        boolean used;
        idVec3  start = new idVec3(), end = new idVec3();
        int     color;
        boolean blink;
        boolean arrow;
    };
    static gameDebugLine_t[] debugLines = Stream.generate(gameDebugLine_t::new).limit(MAX_DEBUGLINES).toArray(gameDebugLine_t[]::new);


    /*
     ==================
     Cmd_AddDebugLine_f
     ==================
     */
    public static class Cmd_AddDebugLine_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new Cmd_AddDebugLine_f();

        private Cmd_AddDebugLine_f() {
        }

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) {
            int i;
            int[] argNum = {0};
            String value;

            if (!gameLocal.CheatsOk()) {
                return;
            }

            if (args.Argc() < 7) {
                gameLocal.Printf("usage: addline <x y z> <x y z> <color>\n");
                return;
            }
            for (i = 0; i < MAX_DEBUGLINES; i++) {
                if (!debugLines[i].used) {
                    break;
                }
            }
            if (i >= MAX_DEBUGLINES) {
                gameLocal.Printf("no free debug lines\n");
                return;
            }
            value = args.Argv(0);
            if (NOT(idStr.Icmp(value, "addarrow"))) {
                debugLines[i].arrow = true;
            } else {
                debugLines[i].arrow = false;
            }
            debugLines[i].used = true;
            debugLines[i].blink = false;
            argNum[0] = 1;
            debugLines[i].start.x = Cmd_GetFloatArg(args, argNum);
            debugLines[i].start.y = Cmd_GetFloatArg(args, argNum);
            debugLines[i].start.z = Cmd_GetFloatArg(args, argNum);
            debugLines[i].end.x = Cmd_GetFloatArg(args, argNum);
            debugLines[i].end.y = Cmd_GetFloatArg(args, argNum);
            debugLines[i].end.z = Cmd_GetFloatArg(args, argNum);
            debugLines[i].color = (int) Cmd_GetFloatArg(args, argNum);
        }
    };

    /*
     ==================
     Cmd_RemoveDebugLine_f
     ==================
     */
    public static class Cmd_RemoveDebugLine_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new Cmd_RemoveDebugLine_f();

        private Cmd_RemoveDebugLine_f() {
        }

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) {
            int i, num;
            String value;

            if (!gameLocal.CheatsOk()) {
                return;
            }

            if (args.Argc() < 2) {
                gameLocal.Printf("usage: removeline <num>\n");
                return;
            }
            value = args.Argv(1);
            num = Integer.parseInt(value);
            for (i = 0; i < MAX_DEBUGLINES; i++) {
                if (debugLines[i].used) {
                    if (--num < 0) {
                        break;
                    }
                }
            }
            if (i >= MAX_DEBUGLINES) {
                gameLocal.Printf("line not found\n");
                return;
            }
            debugLines[i].used = false;
        }
    };

    /*
     ==================
     Cmd_BlinkDebugLine_f
     ==================
     */
    public static class Cmd_BlinkDebugLine_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new Cmd_BlinkDebugLine_f();

        private Cmd_BlinkDebugLine_f() {
        }

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) {
            int i, num;
            String value;

            if (!gameLocal.CheatsOk()) {
                return;
            }

            if (args.Argc() < 2) {
                gameLocal.Printf("usage: blinkline <num>\n");
                return;
            }
            value = args.Argv(1);
            num = Integer.parseInt(value);
            for (i = 0; i < MAX_DEBUGLINES; i++) {
                if (debugLines[i].used) {
                    if (--num < 0) {
                        break;
                    }
                }
            }
            if (i >= MAX_DEBUGLINES) {
                gameLocal.Printf("line not found\n");
                return;
            }
            debugLines[i].blink = !debugLines[i].blink;
        }
    };

    /*
     ==================
     PrintFloat
     ==================
     */
    static void PrintFloat(float f) {
//        char[] buf = new char[128];
//        char i;
//
//        for (i = sprintf(buf, "%3.2f", f); i < 7; i++) {
//            buf[i] = ' ';
//        }
//        buf[i] = '\0';
//        gameLocal.Printf(buf);
        gameLocal.Printf(String.format("%3.2f", f));
    }

    /*
     ==================
     Cmd_ListDebugLines_f
     ==================
     */
    public static class Cmd_ListDebugLines_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new Cmd_ListDebugLines_f();

        private Cmd_ListDebugLines_f() {
        }

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) {
            int i, num;

            if (!gameLocal.CheatsOk()) {
                return;
            }

            num = 0;
            gameLocal.Printf("line num: x1     y1     z1     x2     y2     z2     c  b  a\n");
            for (i = 0; i < MAX_DEBUGLINES; i++) {
                if (debugLines[i].used) {
                    gameLocal.Printf("line %3d: ", num);
                    PrintFloat(debugLines[i].start.x);
                    PrintFloat(debugLines[i].start.y);
                    PrintFloat(debugLines[i].start.z);
                    PrintFloat(debugLines[i].end.x);
                    PrintFloat(debugLines[i].end.y);
                    PrintFloat(debugLines[i].end.z);
                    gameLocal.Printf("%d  %d  %d\n", debugLines[i].color, debugLines[i].blink, debugLines[i].arrow);
                    num++;
                }
            }
            if (NOT(num)) {
                gameLocal.Printf("no debug lines\n");
            }
        }
    };

    /*
     ==================
     D_DrawDebugLines
     ==================
     */
    public static void D_DrawDebugLines() {
        int i;
        idVec3 forward, right = new idVec3(), up = new idVec3(), p1, p2;
        idVec4 color;
        float l;

        for (i = 0; i < MAX_DEBUGLINES; i++) {
            if (debugLines[i].used) {
                if (!debugLines[i].blink || ((gameLocal.time & (1 << 9)) != 0)) {
                    color = new idVec4(debugLines[i].color & 1, (debugLines[i].color >> 1) & 1, (debugLines[i].color >> 2) & 1, 1);
                    gameRenderWorld.DebugLine(color, debugLines[i].start, debugLines[i].end);
                    //
                    if (debugLines[i].arrow) {
                        // draw a nice arrow
                        forward = debugLines[i].end.oMinus(debugLines[i].start);
                        l = forward.Normalize() * 0.2f;
                        forward.NormalVectors(right, up);

                        if (l > 3.0f) {
                            l = 3.0f;
                        }
                        p1 = debugLines[i].end.oMinus(forward.oMultiply(l).oPlus(right.oMultiply(l * 0.4f)));
                        p2 = debugLines[i].end.oMinus(forward.oMultiply(l).oMinus(right.oMultiply(l * 0.4f)));
                        gameRenderWorld.DebugLine(color, debugLines[i].end, p1);
                        gameRenderWorld.DebugLine(color, debugLines[i].end, p2);
                        gameRenderWorld.DebugLine(color, p1, p2);
                    }
                }
            }
        }
    }

    /*
     ==================
     Cmd_ListCollisionModels_f
     ==================
     */
    public static class Cmd_ListCollisionModels_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new Cmd_ListCollisionModels_f();

        private Cmd_ListCollisionModels_f() {
        }

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) {
            if (!gameLocal.CheatsOk()) {
                return;
            }

            CollisionModel_local.collisionModelManager.ListModels();
        }
    };

    /*
     ==================
     Cmd_CollisionModelInfo_f
     ==================
     */
    public static class Cmd_CollisionModelInfo_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new Cmd_CollisionModelInfo_f();

        private Cmd_CollisionModelInfo_f() {
        }

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) {
            String value;

            if (!gameLocal.CheatsOk()) {
                return;
            }

            if (args.Argc() < 2) {
                gameLocal.Printf("usage: collisionModelInfo <modelNum>\n" + "use 'all' instead of the model number for accumulated info\n");
                return;
            }

            value = args.Argv(1);
            if (NOT(idStr.Icmp(value, "all"))) {
                CollisionModel_local.collisionModelManager.ModelInfo(-1);
            } else {
                CollisionModel_local.collisionModelManager.ModelInfo(Integer.parseInt(value));
            }
        }
    };

    /*
     ==================
     Cmd_ExportModels_f
     ==================
     */
    public static class Cmd_ExportModels_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new Cmd_ExportModels_f();

        private Cmd_ExportModels_f() {
        }

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) {
            idModelExport exporter = new idModelExport();
            idStr name = new idStr();

            // don't allow exporting models when cheats are disabled,
            // but if we're not in the game, it's ok
            if (gameLocal.GetLocalPlayer() != null && !gameLocal.CheatsOk(false)) {
                return;
            }

            if (args.Argc() < 2) {
                exporter.ExportModels("def", ".def");
            } else {
                name.oSet(args.Argv(1));
                name.oSet("def/" + name);
                name.DefaultFileExtension(".def");
                exporter.ExportDefFile(name.getData());
            }
        }
    };

    /*
     ==================
     Cmd_ReexportModels_f
     ==================
     */
    public static class Cmd_ReexportModels_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new Cmd_ReexportModels_f();

        private Cmd_ReexportModels_f() {
        }

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) {
            idModelExport exporter = new idModelExport();
            idStr name = new idStr();

            // don't allow exporting models when cheats are disabled,
            // but if we're not in the game, it's ok
            if (gameLocal.GetLocalPlayer() != null && !gameLocal.CheatsOk(false)) {
                return;
            }

            idAnimManager.forceExport = true;
            if (args.Argc() < 2) {
                exporter.ExportModels("def", ".def");
            } else {
                name.oSet(args.Argv(1));
                name.oSet("def/" + name);
                name.DefaultFileExtension(".def");
                exporter.ExportDefFile(name.getData());
            }
            idAnimManager.forceExport = false;
        }
    };

    /*
     ==================
     Cmd_ReloadAnims_f
     ==================
     */
    public static class Cmd_ReloadAnims_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new Cmd_ReloadAnims_f();

        private Cmd_ReloadAnims_f() {
        }

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) {
            // don't allow reloading anims when cheats are disabled,
            // but if we're not in the game, it's ok
            if (gameLocal.GetLocalPlayer() != null && !gameLocal.CheatsOk(false)) {
                return;
            }

            animationLib.ReloadAnims();
        }
    };

    /*
     ==================
     Cmd_ListAnims_f
     ==================
     */
    public static class Cmd_ListAnims_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new Cmd_ListAnims_f();

        private Cmd_ListAnims_f() {
        }

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) {
            idEntity ent;
            int num;
            int /*size_t*/ size;
            int /*size_t*/ alloced;
            idAnimator animator;
            String classname;
            idDict dict;
            int i;

            if (args.Argc() > 1) {
                animator = new idAnimator();

                classname = args.Argv(1);

                dict = gameLocal.FindEntityDefDict(classname, false);
                if (NOT(dict)) {
                    gameLocal.Printf("Entitydef '%s' not found\n", classname);
                    return;
                }
                animator.SetModel(dict.GetString("model"));

                gameLocal.Printf("----------------\n");
                num = animator.NumAnims();
                for (i = 0; i < num; i++) {
                    gameLocal.Printf("%s\n", animator.AnimFullName(i));
                }
                gameLocal.Printf("%d anims\n", num);
            } else {
                animationLib.ListAnims();

                size = 0;
                num = 0;
                for (ent = gameLocal.spawnedEntities.Next(); ent != null; ent = ent.spawnNode.Next()) {
                    animator = ent.GetAnimator();
                    if (animator != null) {
                        alloced = animator.Allocated();
                        size += alloced;
                        num++;
                    }
                }

                gameLocal.Printf("%d memory used in %d entity animators\n", size, num);
            }
        }
    };

    /*
     ==================
     Cmd_AASStats_f
     ==================
     */
    public static class Cmd_AASStats_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new Cmd_AASStats_f();

        private Cmd_AASStats_f() {
        }

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) {
            int aasNum;

            if (!gameLocal.CheatsOk()) {
                return;
            }

            aasNum = aas_test.GetInteger();
            idAAS aas = gameLocal.GetAAS(aasNum);
            if (NOT(aas)) {
                gameLocal.Printf("No aas #%d loaded\n", aasNum);
            } else {
                aas.Stats();
            }
        }
    };

    /*
     ==================
     Cmd_TestDamage_f
     ==================
     */
    public static class Cmd_TestDamage_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new Cmd_TestDamage_f();

        private Cmd_TestDamage_f() {
        }

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) {
            idPlayer player;
            String damageDefName;

            player = gameLocal.GetLocalPlayer();
            if (player == null || !gameLocal.CheatsOk()) {
                return;
            }

            if (args.Argc() < 2 || args.Argc() > 3) {
                gameLocal.Printf("usage: testDamage <damageDefName> [angle]\n");
                return;
            }

            damageDefName = args.Argv(1);

            idVec3 dir;
            if (args.Argc() == 3) {
                float angle = Float.parseFloat(args.Argv(2));

                float[] d1 = {0}, d0 = {0};
                idMath.SinCos((float) DEG2RAD(angle), d1, d0);
                dir = new idVec3(d0[0], d1[0], 0);
            } else {
                dir = new idVec3();
//            dir.Zero();
            }

            // give the player full health before and after
            // running the damage
            player.health = player.inventory.maxHealth;
            player.Damage(null, null, dir, damageDefName, 1.0f, INVALID_JOINT);
            player.health = player.inventory.maxHealth;
        }
    };

    /*
     ==================
     Cmd_TestBoneFx_f
     ==================
     */
    public static class Cmd_TestBoneFx_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new Cmd_TestBoneFx_f();

        private Cmd_TestBoneFx_f() {
        }

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) {
            idPlayer player;
            String bone, fx;

            player = gameLocal.GetLocalPlayer();
            if (player == null || !gameLocal.CheatsOk()) {
                return;
            }

            if (args.Argc() < 3 || args.Argc() > 4) {
                gameLocal.Printf("usage: testBoneFx <fxName> <boneName>\n");
                return;
            }

            fx = args.Argv(1);
            bone = args.Argv(2);

            player.StartFxOnBone(fx, bone);
        }
    };

    /*
     ==================
     Cmd_TestDamage_f
     ==================
     */
    public static class Cmd_TestDeath_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new Cmd_TestDeath_f();

        private Cmd_TestDeath_f() {
        }

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) {
            idPlayer player;

            player = gameLocal.GetLocalPlayer();
            if (player == null || !gameLocal.CheatsOk()) {
                return;
            }

            idVec3 dir;
            float[] d1 = {0}, d0 = {0};
            idMath.SinCos((float) DEG2RAD(45.0f), d1, d0);
            dir = new idVec3(d0[0], d1[0], 0);

            g_testDeath.SetBool(true);
            player.Damage(null, null, dir, "damage_triggerhurt_1000", 1.0f, INVALID_JOINT);
            if (args.Argc() >= 2) {
                player.SpawnGibs(dir, "damage_triggerhurt_1000");
            }
        }
    };

    /*
     ==================
     Cmd_WeaponSplat_f
     ==================
     */
    public static class Cmd_WeaponSplat_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new Cmd_WeaponSplat_f();

        private Cmd_WeaponSplat_f() {
        }

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) {
            idPlayer player;

            player = gameLocal.GetLocalPlayer();
            if (player == null || !gameLocal.CheatsOk()) {
                return;
            }

            player.weapon.GetEntity().BloodSplat(2.0f);
        }
    };

    /*
     ==================
     Cmd_SaveSelected_f
     ==================
     */
    public static class Cmd_SaveSelected_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new Cmd_SaveSelected_f();

        private Cmd_SaveSelected_f() {

        }

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) {
            int i;
            idPlayer player;
            idEntity s;
            idMapEntity mapEnt;
            idMapFile mapFile = gameLocal.GetLevelMap();
            idDict dict = new idDict();
            idStr mapName;
            String name = null;

            player = gameLocal.GetLocalPlayer();
            if (player == null || !gameLocal.CheatsOk()) {
                return;
            }

            s = player.dragEntity.GetSelected();
            if (NOT(s)) {
                gameLocal.Printf("no entity selected, set g_dragShowSelection 1 to show the current selection\n");
                return;
            }

            if (args.Argc() > 1) {
                mapName = new idStr(args.Argv(1));
                mapName.oSet("maps/" + mapName);
            } else {
                mapName = new idStr(mapFile.GetName());
            }

            // find map file entity
            mapEnt = mapFile.FindEntity(s.name.getData());
            // create new map file entity if there isn't one for this articulated figure
            if (NOT(mapEnt)) {
                mapEnt = new idMapEntity();
                mapFile.AddEntity(mapEnt);
                for (i = 0; i < 9999; i++) {
                    name = va("%s_%d", s.GetEntityDefName(), i);
                    if (NOT(gameLocal.FindEntity(name))) {
                        break;
                    }
                }
                s.name.oSet(name);
                mapEnt.epairs.Set("classname", s.GetEntityDefName());
                mapEnt.epairs.Set("name", s.name);
            }

            if (s.IsType(idMoveable.class)) {
                // save the moveable state
                mapEnt.epairs.Set("origin", s.GetPhysics().GetOrigin().ToString(8));
                mapEnt.epairs.Set("rotation", s.GetPhysics().GetAxis().ToString(8));
            } else if (s.IsType(idAFEntity_Generic.class) || s.IsType(idAFEntity_WithAttachedHead.class)) {
                // save the articulated figure state
                dict.Clear();
                ((idAFEntity_Base) s).SaveState(dict);
                mapEnt.epairs.Copy(dict);
            }

            // write out the map file
            mapFile.Write(mapName.getData(), ".map");
        }
    };

    /*
     ==================
     Cmd_DeleteSelected_f
     ==================
     */
    public static class Cmd_DeleteSelected_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new Cmd_DeleteSelected_f();

        private Cmd_DeleteSelected_f() {
        }

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) {
            idPlayer player;

            player = gameLocal.GetLocalPlayer();
            if (player == null || !gameLocal.CheatsOk()) {
                return;
            }

            if (player != null) {
                player.dragEntity.DeleteSelected();
            }
        }
    };

    /*
     ==================
     Cmd_SaveMoveables_f
     ==================
     */
    public static class Cmd_SaveMoveables_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new Cmd_SaveMoveables_f();

        private Cmd_SaveMoveables_f() {
        }

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) {
            int e, i;
            idMoveable m;
            idMapEntity mapEnt;
            idMapFile mapFile = gameLocal.GetLevelMap();
            idStr mapName = new idStr();
            String name = null;

            if (!gameLocal.CheatsOk()) {
                return;
            }

            for (e = 0; e < MAX_GENTITIES; e++) {
                m = ((idMoveable) gameLocal.entities[ e]);

                if (NOT(m) || !m.IsType(idMoveable.class)) {
                    continue;
                }

                if (m.IsBound()) {
                    continue;
                }

                if (!m.IsAtRest()) {
                    break;
                }
            }

            if (e < MAX_GENTITIES) {
                gameLocal.Warning("map not saved because the moveable entity %s is not at rest", gameLocal.entities[ e].name);
                return;
            }

            if (args.Argc() > 1) {
                mapName.oSet(args.Argv(1));
                mapName.oSet("maps/" + mapName);
            } else {
                mapName.oSet(mapFile.GetName());
            }

            for (e = 0; e < MAX_GENTITIES; e++) {
                m = ((idMoveable) gameLocal.entities[ e]);

                if (NOT(m) || !m.IsType(idMoveable.class)) {
                    continue;
                }

                if (m.IsBound()) {
                    continue;
                }

                // find map file entity
                mapEnt = mapFile.FindEntity(m.name);
                // create new map file entity if there isn't one for this articulated figure
                if (NOT(mapEnt)) {
                    mapEnt = new idMapEntity();
                    mapFile.AddEntity(mapEnt);
                    for (i = 0; i < 9999; i++) {
                        name = va("%s_%d", m.GetEntityDefName(), i);
                        if (NOT(gameLocal.FindEntity(name))) {
                            break;
                        }
                    }
                    m.name.oSet(name);
                    mapEnt.epairs.Set("classname", m.GetEntityDefName());
                    mapEnt.epairs.Set("name", m.name);
                }
                // save the moveable state
                mapEnt.epairs.Set("origin", m.GetPhysics().GetOrigin().ToString(8));
                mapEnt.epairs.Set("rotation", m.GetPhysics().GetAxis().ToString(8));
            }

            // write out the map file
            mapFile.Write(mapName.getData(), ".map");
        }
    };

    /*
     ==================
     Cmd_SaveRagdolls_f
     ==================
     */
    public static class Cmd_SaveRagdolls_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new Cmd_SaveRagdolls_f();

        private Cmd_SaveRagdolls_f() {
        }

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) {
            int e, i;
            idAFEntity_Base af;
            idMapEntity mapEnt;
            idMapFile mapFile = gameLocal.GetLevelMap();
            idDict dict = new idDict();
            idStr mapName = new idStr();
            String name = null;

            if (!gameLocal.CheatsOk()) {
                return;
            }

            if (args.Argc() > 1) {
                mapName.oSet(args.Argv(1));
                mapName.oSet("maps/" + mapName);
            } else {
                mapName.oSet(mapFile.GetName());
            }

            for (e = 0; e < MAX_GENTITIES; e++) {
                af = ((idAFEntity_Base) gameLocal.entities[ e]);

                if (NOT(af)) {
                    continue;
                }

                if (!af.IsType(idAFEntity_WithAttachedHead.class) && !af.IsType(idAFEntity_Generic.class)) {
                    continue;
                }

                if (af.IsBound()) {
                    continue;
                }

                if (!af.IsAtRest()) {
                    gameLocal.Warning("the articulated figure for entity %s is not at rest", gameLocal.entities[ e].name);
                }

                dict.Clear();
                af.SaveState(dict);

                // find map file entity
                mapEnt = mapFile.FindEntity(af.name.getData());
                // create new map file entity if there isn't one for this articulated figure
                if (NOT(mapEnt)) {
                    mapEnt = new idMapEntity();
                    mapFile.AddEntity(mapEnt);
                    for (i = 0; i < 9999; i++) {
                        name = va("%s_%d", af.GetEntityDefName(), i);
                        if (NOT(gameLocal.FindEntity(name))) {
                            break;
                        }
                    }
                    af.name.oSet(name);
                    mapEnt.epairs.Set("classname", af.GetEntityDefName());
                    mapEnt.epairs.Set("name", af.name);
                }
                // save the articulated figure state
                mapEnt.epairs.Copy(dict);
            }

            // write out the map file
            mapFile.Write(mapName.getData(), ".map");
        }
    };

    /*
     ==================
     Cmd_BindRagdoll_f
     ==================
     */
    public static class Cmd_BindRagdoll_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new Cmd_BindRagdoll_f();

        private Cmd_BindRagdoll_f() {
        }

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) {
            idPlayer player;

            player = gameLocal.GetLocalPlayer();
            if (player == null || !gameLocal.CheatsOk()) {
                return;
            }

            if (player != null) {
                player.dragEntity.BindSelected();
            }
        }
    };

    /*
     ==================
     Cmd_UnbindRagdoll_f
     ==================
     */
    public static class Cmd_UnbindRagdoll_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new Cmd_UnbindRagdoll_f();

        private Cmd_UnbindRagdoll_f() {
        }

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) {
            idPlayer player;

            player = gameLocal.GetLocalPlayer();
            if (player == null || !gameLocal.CheatsOk()) {
                return;
            }

            if (player != null) {
                player.dragEntity.UnbindSelected();
            }
        }
    };

    /*
     ==================
     Cmd_GameError_f
     ==================
     */
    public static class Cmd_GameError_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new Cmd_GameError_f();

        private Cmd_GameError_f() {
        }

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) {
            gameLocal.Error("game error");
        }
    };

    /*
     ==================
     Cmd_SaveLights_f
     ==================
     */
    public static class Cmd_SaveLights_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new Cmd_SaveLights_f();

        private Cmd_SaveLights_f() {
        }

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) {
            int e, i;
            idLight light;
            idMapEntity mapEnt;
            idMapFile mapFile = gameLocal.GetLevelMap();
            idDict dict = new idDict();
            idStr mapName = new idStr();
            String name = null;

            if (!gameLocal.CheatsOk()) {
                return;
            }

            if (args.Argc() > 1) {
                mapName.oSet(args.Argv(1));
                mapName.oSet("maps/" + mapName);
            } else {
                mapName.oSet(mapFile.GetName());
            }

            for (e = 0; e < MAX_GENTITIES; e++) {
                light = ((idLight) gameLocal.entities[ e]);

                if (NOT(light) || !light.IsType(idLight.class)) {
                    continue;
                }

                dict.Clear();
                light.SaveState(dict);

                // find map file entity
                mapEnt = mapFile.FindEntity(light.name.getData());
                // create new map file entity if there isn't one for this light
                if (NOT(mapEnt)) {
                    mapEnt = new idMapEntity();
                    mapFile.AddEntity(mapEnt);
                    for (i = 0; i < 9999; i++) {
                        name = va("%s_%d", light.GetEntityDefName(), i);
                        if (NOT(gameLocal.FindEntity(name))) {
                            break;
                        }
                    }
                    light.name.oSet(name);
                    mapEnt.epairs.Set("classname", light.GetEntityDefName());
                    mapEnt.epairs.Set("name", light.name);
                }
                // save the light state
                mapEnt.epairs.Copy(dict);
            }

            // write out the map file
            mapFile.Write(mapName.getData(), ".map");
        }
    };


    /*
     ==================
     Cmd_SaveParticles_f
     ==================
     */
    public static class Cmd_SaveParticles_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new Cmd_SaveParticles_f();

        private Cmd_SaveParticles_f() {
        }

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) {
            int e;
            idEntity ent;
            idMapEntity mapEnt;
            idMapFile mapFile = gameLocal.GetLevelMap();
            idDict dict = new idDict();
            idStr mapName = new idStr(), strModel;

            if (!gameLocal.CheatsOk()) {
                return;
            }

            if (args.Argc() > 1) {
                mapName.oSet(args.Argv(1));
                mapName.oSet("maps/" + mapName);
            } else {
                mapName.oSet(mapFile.GetName());
            }

            for (e = 0; e < MAX_GENTITIES; e++) {

                ent = ((idStaticEntity) gameLocal.entities[ e]);

                if (NOT(ent)) {
                    continue;
                }

                strModel = new idStr(ent.spawnArgs.GetString("model"));
                if (strModel.Length() != 0 && strModel.Find(".prt") > 0) {
                    dict.Clear();
                    dict.Set("model", ent.spawnArgs.GetString("model"));
                    dict.SetVector("origin", ent.GetPhysics().GetOrigin());

                    // find map file entity
                    mapEnt = mapFile.FindEntity(ent.name.getData());
                    // create new map file entity if there isn't one for this entity
                    if (NOT(mapEnt)) {
                        continue;
                    }
                    // save the particle state
                    mapEnt.epairs.Copy(dict);
                }
            }

            // write out the map file
            mapFile.Write(mapName.getData(), ".map");
        }
    };


    /*
     ==================
     Cmd_DisasmScript_f
     ==================
     */
    public static class Cmd_DisasmScript_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new Cmd_DisasmScript_f();

        private Cmd_DisasmScript_f() {
        }

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) {
            gameLocal.program.Disassemble();
        }
    };

    /*
     ==================
     Cmd_TestSave_f
     ==================
     */
    public static class Cmd_TestSave_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new Cmd_TestSave_f();

        private Cmd_TestSave_f() {
        }

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) {
            idFile f;

            f = fileSystem.OpenFileWrite("test.sav");
            gameLocal.SaveGame(f);
            fileSystem.CloseFile(f);
        }
    };

    /*
     ==================
     Cmd_RecordViewNotes_f
     ==================
     */
    public static class Cmd_RecordViewNotes_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new Cmd_RecordViewNotes_f();

        private Cmd_RecordViewNotes_f() {
        }

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) {
            idPlayer player;
            idVec3 origin = new idVec3();
            idMat3 axis = new idMat3();

            if (args.Argc() <= 3) {
                return;
            }

            player = gameLocal.GetLocalPlayer();
            if (player == null) {
                return;
            }

            player.GetViewPos(origin, axis);

            // Argv(1) = filename for map (viewnotes/mapname/person)
            // Argv(2) = note number (person0001)
            // Argv(3) = comments
            idStr str = new idStr(args.Argv(1));
            str.SetFileExtension(".txt");
            idFile file = fileSystem.OpenFileAppend(str.getData());
            if (file != null) {
                file.WriteFloatString("\"view\"\t( %s )\t( %s )\r\n", origin.ToString(), axis.ToString());
                file.WriteFloatString("\"comments\"\t\"%s: %s\"\r\n\r\n", args.Argv(2), args.Argv(3));
                fileSystem.CloseFile(file);
            }

            idStr viewComments = new idStr(args.Argv(1));
            viewComments.StripLeading("viewnotes/");
            viewComments.oPluSet(" -- Loc: ");
            viewComments.oPluSet(origin.ToString());
            viewComments.oPluSet("\n");
            viewComments.oPluSet(args.Argv(3));
            player.hud.SetStateString("viewcomments", viewComments.getData());
            player.hud.HandleNamedEvent("showViewComments");
        }
    };

    /*
     ==================
     Cmd_CloseViewNotes_f
     ==================
     */
    public static class Cmd_CloseViewNotes_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new Cmd_CloseViewNotes_f();

        private Cmd_CloseViewNotes_f() {
        }

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) {
            idPlayer player = gameLocal.GetLocalPlayer();

            if (player == null) {
                return;
            }

            player.hud.SetStateString("viewcomments", "");
            player.hud.HandleNamedEvent("hideViewComments");
        }
    };

    /*
     ==================
     Cmd_ShowViewNotes_f
     ==================
     */
    public static class Cmd_ShowViewNotes_f extends cmdFunction_t {

        static final idLexer parser = new idLexer(LEXFL_ALLOWPATHNAMES | LEXFL_NOSTRINGESCAPECHARS | LEXFL_NOSTRINGCONCAT | LEXFL_NOFATALERRORS);
        private static final cmdFunction_t instance = new Cmd_ShowViewNotes_f();

        private Cmd_ShowViewNotes_f() {
        }

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) {
            idToken token = new idToken();
            idPlayer player;
            idVec3 origin = new idVec3();
            idMat3 axis = new idMat3();

            player = gameLocal.GetLocalPlayer();

            if (player == null) {
                return;
            }

            if (!parser.IsLoaded()) {
                idStr str = new idStr("viewnotes/");
                str.oPluSet(gameLocal.GetMapName());
                str.StripFileExtension();
                str.oPluSet("/");
                if (args.Argc() > 1) {
                    str.oPluSet(args.Argv(1));
                } else {
                    str.oPluSet("comments");
                }
                str.SetFileExtension(".txt");
                if (!parser.LoadFile(str.getData())) {
                    gameLocal.Printf("No view notes for %s\n", gameLocal.GetMapName());
                    return;
                }
            }

            if (parser.ExpectTokenString("view") && parser.Parse1DMatrix(3, origin)
                    && parser.Parse1DMatrix(9, axis) && parser.ExpectTokenString("comments") && parser.ReadToken(token)) {
                player.hud.SetStateString("viewcomments", token.getData());
                player.hud.HandleNamedEvent("showViewComments");
                player.Teleport(origin, axis.ToAngles(), null);
            } else {
                parser.FreeSource();
                player.hud.HandleNamedEvent("hideViewComments");
                return;
            }
        }
    };

    /*
     =================
     Cmd_NextGUI_f
     =================
     */
    public static class Cmd_NextGUI_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new Cmd_NextGUI_f();

        private Cmd_NextGUI_f() {
        }

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) {
            idVec3 origin;
            idAngles angles;
            idPlayer player;
            idEntity ent;
            int[] guiSurfaces = {0};
            boolean newEnt;
            renderEntity_s renderEnt;
            int surfIndex;
            srfTriangles_s geom;
            idMat4 modelMatrix;
            idVec3 normal;
            idVec3 center;
            modelSurface_s[] surfaces = new modelSurface_s[MAX_RENDERENTITY_GUI];

            player = gameLocal.GetLocalPlayer();
            if (player == null || !gameLocal.CheatsOk()) {
                return;
            }

            if (args.Argc() != 1) {
                gameLocal.Printf("usage: nextgui\n");
                return;
            }

            // start at the last entity
            ent = gameLocal.lastGUIEnt.GetEntity();

            // see if we have any gui surfaces left to go to on the current entity.
//        guiSurfaces = 0;
            newEnt = false;
            if (ent == null) {
                newEnt = true;
            } else if (FindEntityGUIs(ent, surfaces, MAX_RENDERENTITY_GUI, guiSurfaces) == true) {
                if (gameLocal.lastGUI >= guiSurfaces[0]) {
                    newEnt = true;
                }
            } else {
                // no actual gui surfaces on this ent, so skip it
                newEnt = true;
            }

            if (newEnt == true) {
                // go ahead and skip to the next entity with a gui...
                if (ent == null) {
                    ent = gameLocal.spawnedEntities.Next();
                } else {
                    ent = ent.spawnNode.Next();
                }

                for (; ent != null; ent = ent.spawnNode.Next()) {
                    if (ent.spawnArgs.GetString("gui", null) != null) {
                        break;
                    }

                    if (ent.spawnArgs.GetString("gui2", null) != null) {
                        break;
                    }

                    if (ent.spawnArgs.GetString("gui3", null) != null) {
                        break;
                    }

                    // try the next entity
                    gameLocal.lastGUIEnt.oSet(ent);
                }

                gameLocal.lastGUIEnt.oSet(ent);
                gameLocal.lastGUI = 0;

                if (NOT(ent)) {
                    gameLocal.Printf("No more gui entities. Starting over...\n");
                    return;
                }
            }

            if (FindEntityGUIs(ent, surfaces, MAX_RENDERENTITY_GUI, guiSurfaces) == false) {
                gameLocal.Printf("Entity \"%s\" has gui properties but no gui surfaces.\n", ent.name);
            }

            if (guiSurfaces[0] == 0) {
                gameLocal.Printf("Entity \"%s\" has gui properties but no gui surfaces!\n", ent.name);
                return;
            }

            gameLocal.Printf("Teleporting to gui entity \"%s\", gui #%d.\n", ent.name, gameLocal.lastGUI);

            renderEnt = ent.GetRenderEntity();
            surfIndex = gameLocal.lastGUI++;
            geom = surfaces[ surfIndex].geometry;
            if (geom == null) {
                gameLocal.Printf("Entity \"%s\" has gui surface %d without geometry!\n", ent.name, surfIndex);
                return;
            }

            assert (geom.facePlanes != null);

            modelMatrix = new idMat4(renderEnt.axis, renderEnt.origin);
            normal = geom.facePlanes[0].Normal().oMultiply(renderEnt.axis);
            center = geom.bounds.GetCenter().oMultiply(modelMatrix);

            origin = center.oPlus(normal.oMultiply(32.0f));
            origin.z -= player.EyeHeight();
            normal.oMulSet(-1.0f);
            angles = normal.ToAngles();

            //	make sure the player is in noclip
            player.noclip = true;
            player.Teleport(origin, angles, null);
        }

        /*
         =================
         FindEntityGUIs

         helper function for Cmd_NextGUI_f.  Checks the passed entity to determine if it
         has any valid gui surfaces.
         =================
         */
        boolean FindEntityGUIs(idEntity ent, final modelSurface_s[] surfaces, int maxSurfs, int[] guiSurfaces) {
            renderEntity_s renderEnt;
            idRenderModel renderModel;
            modelSurface_s surf;
            idMaterial shader;
            int i;

            assert (surfaces != null);
            assert (ent != null);

//	memset( surfaces, 0x00, sizeof( modelSurface_t *) * maxSurfs );//TODO: make sure the loop below loops over the entire array
            guiSurfaces[0] = 0;

            renderEnt = ent.GetRenderEntity();
            renderModel = renderEnt.hModel;
            if (renderModel == null) {
                return false;
            }

            for (i = 0; i < renderModel.NumSurfaces(); i++) {
                surf = renderModel.Surface(i);
                if (surf == null) {
                    continue;
                }
                shader = surf.shader;
                if (shader == null) {
                    continue;
                }
                if (shader.GetEntityGui() > 0) {
                    surfaces[ guiSurfaces[0]++] = surf;
                }
            }

            return (guiSurfaces[0] != 0);
        }
    };

    public static class ArgCompletion_DefFile extends argCompletion_t {

        private static final argCompletion_t instance = new ArgCompletion_DefFile();

        private ArgCompletion_DefFile() {
        }

        public static argCompletion_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args, void_callback<String> callback) {
            cmdSystem.ArgCompletion_FolderExtension(args, callback, "def/", true, ".def", null);
        }
    };

    /*
     ===============
     Cmd_TestId_f
     outputs a string from the string table for the specified id
     ===============
     */
    public static class Cmd_TestId_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new Cmd_TestId_f();

        private Cmd_TestId_f() {
        }

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) {
            String id = "";
            int i;
            if (args.Argc() == 1) {
                common.Printf("usage: testid <string id>\n");
                return;
            }

            for (i = 1; i < args.Argc(); i++) {
                id += args.Argv(i);
            }
            if (idStr.Cmpn(id, STRTABLE_ID, STRTABLE_ID_LENGTH) != 0) {
                id = STRTABLE_ID + id;
            }
            gameLocal.mpGame.AddChatLine(common.GetLanguageDict().GetString(id), "<nothing>", "<nothing>", "<nothing>");
        }
    };
}
