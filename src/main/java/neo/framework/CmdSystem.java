package neo.framework;

import static neo.framework.CVarSystem.cvarSystem;
import static neo.framework.CmdSystem.cmdExecution_t.CMD_EXEC_APPEND;
import static neo.framework.CmdSystem.cmdExecution_t.CMD_EXEC_INSERT;
import static neo.framework.DeclManager.declManager;
import static neo.framework.FileSystem_h.fileSystem;
import static neo.framework.Session.session;
import static neo.idlib.Lib.BIT;
import static neo.idlib.Lib.idLib.common;
import static neo.idlib.Text.Str.va;

import java.nio.ByteBuffer;

import neo.TempDump.NeoFixStrings;
import neo.TempDump.void_callback;
import neo.framework.DeclManager.declType_t;
import neo.framework.FileSystem_h.idFileList;
import neo.idlib.CmdArgs.idCmdArgs;
import neo.idlib.Lib.idException;
import neo.idlib.Text.Str.idStr;
import neo.idlib.containers.List.cmp_t;
import neo.idlib.containers.List.idList;
import neo.idlib.containers.StrList.idStrList;
import neo.open.Nio;

/**
 *
 */
public class CmdSystem {
    /*
     ===============================================================================

     Console command execution and command text buffering.

     Any number of commands can be added in a frame from several different
     sources. Most commands come from either key bindings or console line input,
     but entire text files can be execed.

     Command execution takes a null terminated string, breaks it into tokens,
     then searches for a command or variable that matches the first token.

     ===============================================================================
     */

    private static idCmdSystemLocal cmdSystemLocal = new idCmdSystemLocal();
    public static idCmdSystem cmdSystem = cmdSystemLocal;

// command flags
//typedef enum {
    public static final long CMD_FL_ALL = -1;
    public static final long CMD_FL_CHEAT = BIT(0);	// command is considered a cheat
    public static final long CMD_FL_SYSTEM = BIT(1);	// system command
    public static final long CMD_FL_RENDERER = BIT(2);	// renderer command
    public static final long CMD_FL_SOUND = BIT(3);	// sound command
    public static final long CMD_FL_GAME = BIT(4);	// game command
    public static final long CMD_FL_TOOL = BIT(5);	// tool command
//} cmdFlags_t;

// parameters for command buffer stuffing
    public enum cmdExecution_t {

        CMD_EXEC_NOW, // don't return until completed
        CMD_EXEC_INSERT, // insert at current position, but don't run yet
        CMD_EXEC_APPEND						// add to end of the command buffer (normal case)
    }

    // command function
    public static abstract class cmdFunction_t {

        public abstract void run(final idCmdArgs args) throws idException;
    }

    // argument completion function
    public static abstract class argCompletion_t {
//typedef void (*argCompletion_t)( final idCmdArgs args, void_callback<String> callback );

        public abstract void run(final idCmdArgs args, void_callback<String> callback) throws idException;

        public void run(idCmdArgs args, void_callback<String> callback, int type) {
        }
    }

    public static abstract class idCmdSystem {
//
//public	virtual				~idCmdSystem( void ) {}
//

        public abstract void Init() throws idException;

        public abstract void Shutdown();

        // Registers a command and the function to call for it.
        public abstract void AddCommand(final String cmdName, cmdFunction_t function, long flags, final String description, argCompletion_t argCompletion) throws idException;

        public void AddCommand(final String cmdName, cmdFunction_t function, long flags, final String description/*, argCompletion_t argCompletion = NULL*/) throws idException {
            AddCommand(cmdName, function, flags, description, null);
        }

        // Removes a command.
        public abstract void RemoveCommand(final String cmdName);
        // Remove all commands with one of the flags set.

        public abstract void RemoveFlaggedCommands(int flags);

        // Command and argument completion using callback for each valid string.
        public abstract void CommandCompletion(void_callback<String> callback) throws idException;

        public abstract void ArgCompletion(final String cmdString, void_callback<String> callback) throws idException;

        // Adds command text to the command buffer, does not add a final \n
        public abstract void BufferCommandText(cmdExecution_t exec, final String text) throws idException;

        // Pulls off \n \r or ; terminated lines of text from the command buffer and
        // executes the commands. Stops when the buffer is empty.
        // Normally called once per frame, but may be explicitly invoked.
        public abstract void ExecuteCommandBuffer() throws idException;

        // Base for path/file auto-completion.
        public abstract void ArgCompletion_FolderExtension(final idCmdArgs args, void_callback<String> callback, final String folder, boolean stripFolder, Object... objects) throws idException;

        // Base for decl name auto-completion.
        public abstract void ArgCompletion_DeclName(final idCmdArgs args, void_callback<String> callback, int type) throws idException;

        // Adds to the command buffer in tokenized form ( CMD_EXEC_NOW or CMD_EXEC_APPEND only )
        public abstract void BufferCommandArgs(cmdExecution_t exec, final idCmdArgs args) throws idException;

        // Setup a reloadEngine to happen on next command run, and give a command to execute after reload
        public abstract void SetupReloadEngine(final idCmdArgs args) throws idException;

        public abstract boolean PostReloadEngine() throws idException;

        // Default argument completion functions.
        public static class ArgCompletion_Boolean extends argCompletion_t {

            private static final argCompletion_t instance = new ArgCompletion_Boolean();

            public static argCompletion_t getInstance() {
                return instance;
            }

            @Override
            public void run(idCmdArgs args, void_callback<String> callback) throws idException {
                callback.run(va("%s 0", args.Argv(0)));
                callback.run(va("%s 1", args.Argv(0)));
            }
        }

        public static class ArgCompletion_Integer extends argCompletion_t {

            private final int min, max;

            public ArgCompletion_Integer(int min, int max) {
                this.min = min;
                this.max = max;
            }

            @Override
            public void run(idCmdArgs args, void_callback<String> callback) throws idException {
                for (int i = this.min; i <= this.max; i++) {
                    callback.run(va("%s %d", args.Argv(0), i));
                }
            }
        }

//	template<final String *strings>
        public static class ArgCompletion_String extends argCompletion_t {

            private final String[] listDeclStrings;

            public ArgCompletion_String(final String[] listDeclStrings) {
                this.listDeclStrings = listDeclStrings;
            }

            @Override
            public void run(idCmdArgs args, void_callback<String> callback) throws idException {
                for (int i = 0; this.listDeclStrings[i] != null; i++) {
                    callback.run(va("%s %s", args.Argv(0), this.listDeclStrings[i]));
                }
            }
        }
//	template<int type>

        public static class ArgCompletion_Decl extends argCompletion_t {

            private final declType_t type;

            public ArgCompletion_Decl(declType_t type) {
                this.type = type;
            }

            @Override
            public void run(idCmdArgs args, void_callback<String> callback) throws idException {
                cmdSystem.ArgCompletion_DeclName(args, callback, this.type.ordinal());
            }
        }

        public static class ArgCompletion_FileName extends argCompletion_t {

            private static final argCompletion_t instance = new ArgCompletion_FileName();

            public static argCompletion_t getInstance() {
                return instance;
            }

            @Override
            public void run(idCmdArgs args, void_callback<String> callback) throws idException {
                cmdSystem.ArgCompletion_FolderExtension(args, callback, "/", true, "", null);
            }
        }

        public static class ArgCompletion_MapName extends argCompletion_t {

            private static final argCompletion_t instance = new ArgCompletion_MapName();

            public static argCompletion_t getInstance() {
                return instance;
            }

            @Override
            public void run(idCmdArgs args, void_callback<String> callback) throws idException {
                cmdSystem.ArgCompletion_FolderExtension(args, callback, "maps/", true, ".map", null);
            }
        }

        public static class ArgCompletion_ModelName extends argCompletion_t {

            private static final argCompletion_t instance = new ArgCompletion_ModelName();

            public static argCompletion_t getInstance() {
                return instance;
            }

            @Override
            public void run(idCmdArgs args, void_callback<String> callback) throws idException {
                cmdSystem.ArgCompletion_FolderExtension(args, callback, "models/", false, ".lwo", ".ase", ".md5mesh", ".ma", null);
            }
        }

        public static class ArgCompletion_SoundName extends argCompletion_t {

            private static final argCompletion_t instance = new ArgCompletion_SoundName();

            public static argCompletion_t getInstance() {
                return instance;
            }

            @Override
            public void run(idCmdArgs args, void_callback<String> callback) throws idException {
                cmdSystem.ArgCompletion_FolderExtension(args, callback, "sound/", false, ".wav", ".ogg", null);
            }
        }

        public static class ArgCompletion_ImageName extends argCompletion_t {

            private static final argCompletion_t instance = new ArgCompletion_ImageName();

            public static argCompletion_t getInstance() {
                return instance;
            }

            @Override
            public void run(idCmdArgs args, void_callback<String> callback) throws idException {
                cmdSystem.ArgCompletion_FolderExtension(args, callback, "/", false, ".tga", ".dds", ".jpg", ".pcx", null);
            }
        }

        public static class ArgCompletion_VideoName extends argCompletion_t {

            private static final argCompletion_t instance = new ArgCompletion_VideoName();

            public static argCompletion_t getInstance() {
                return instance;
            }

            @Override
            public void run(idCmdArgs args, void_callback<String> callback) throws idException {
                cmdSystem.ArgCompletion_FolderExtension(args, callback, "video/", false, ".roq", null);
            }
        }

        public static class ArgCompletion_ConfigName extends argCompletion_t {

            private static final argCompletion_t instance = new ArgCompletion_ConfigName();

            public static argCompletion_t getInstance() {
                return instance;
            }

            @Override
            public void run(idCmdArgs args, void_callback<String> callback) throws idException {
                cmdSystem.ArgCompletion_FolderExtension(args, callback, "/", true, ".cfg", null);
            }
        }

        public static class ArgCompletion_SaveGame extends argCompletion_t {

            private static final argCompletion_t instance = new ArgCompletion_SaveGame();

            public static argCompletion_t getInstance() {
                return instance;
            }

            @Override
            public void run(idCmdArgs args, void_callback<String> callback) throws idException {
                cmdSystem.ArgCompletion_FolderExtension(args, callback, "SaveGames/", true, ".save", null);
            }
        }

        public static class ArgCompletion_DemoName extends argCompletion_t {

            private static final argCompletion_t instance = new ArgCompletion_DemoName();

            public static argCompletion_t getInstance() {
                return instance;
            }

            @Override
            public void run(idCmdArgs args, void_callback<String> callback) throws idException {
                cmdSystem.ArgCompletion_FolderExtension(args, callback, "demos/", true, ".demo", null);
            }
        }
    }

    /*
     ===============================================================================

     idCmdSystemLocal

     ===============================================================================
     */
    public static class commandDef_s {

        commandDef_s next;
        String name;
        cmdFunction_t function;
        argCompletion_t argCompletion;
        long flags;
        String description;

        private void oSet(commandDef_s last) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    }

    static class idCmdSystemLocal extends idCmdSystem {

        private static final int MAX_CMD_BUFFER = 0x10000;
        //
        private commandDef_s commands;
        //
        private int wait;
        private int textLength;
        private byte[] textBuf = new byte[MAX_CMD_BUFFER];
        private idStr completionString;
        private final idStrList completionParms;
        // piggybacks on the text buffer, avoids tokenize again and screwing it up
        private final idList<idCmdArgs> tokenizedCmds;
        // a command stored to be executed after a reloadEngine and all associated commands have been processed
        private idCmdArgs postReload;
        //
        //

        idCmdSystemLocal() {
            this.completionString = new idStr();
            this.completionParms = new idStrList();
            this.tokenizedCmds = new idList<idCmdArgs>();
            this.postReload = new idCmdArgs();
        }

        @Override
        public void Init() throws idException {

            AddCommand("listCmds", List_f.getInstance(), CMD_FL_SYSTEM, "lists commands");
            AddCommand("listSystemCmds", SystemList_f.getInstance(), CMD_FL_SYSTEM, "lists system commands");
            AddCommand("listRendererCmds", RendererList_f.getInstance(), CMD_FL_SYSTEM, "lists renderer commands");
            AddCommand("listSoundCmds", SoundList_f.getInstance(), CMD_FL_SYSTEM, "lists sound commands");
            AddCommand("listGameCmds", GameList_f.getInstance(), CMD_FL_SYSTEM, "lists game commands");
            AddCommand("listToolCmds", ToolList_f.getInstance(), CMD_FL_SYSTEM, "lists tool commands");
            AddCommand("exec", Exec_f.getInstance(), CMD_FL_SYSTEM, "executes a config file", ArgCompletion_ConfigName.getInstance());//TODO:extend argCompletion_t
            AddCommand("vstr", Vstr_f.getInstance(), CMD_FL_SYSTEM, "inserts the current value of a cvar as command text");
            AddCommand("echo", Echo_f.getInstance(), CMD_FL_SYSTEM, "prints text");
            AddCommand("parse", Parse_f.getInstance(), CMD_FL_SYSTEM, "prints tokenized string");
            AddCommand("wait", Wait_f.getInstance(), CMD_FL_SYSTEM, "delays remaining buffered commands one or more frames");

            this.completionString = new idStr("*");

            this.textLength = 0;
        }

        @Override
        public void Shutdown() {
            final commandDef_s cmd;

//            for (cmd = commands; cmd != null; cmd = commands) {
//                commands = commands.next;
//                cmd.name = cmd.description = null;
////                Mem_Free(cmd.name);
////                Mem_Free(cmd.description);
////		delete cmd;
//            }
            this.commands = null;

            this.completionString.Clear();
            this.completionParms.Clear();
            this.tokenizedCmds.Clear();
            this.postReload.Clear();
        }

        @Override
        public void AddCommand(String cmdName, cmdFunction_t function, long flags, String description, argCompletion_t argCompletion) throws idException {
            commandDef_s cmd;

            // fail if the command already exists
            for (cmd = this.commands; cmd != null; cmd = cmd.next) {
                if (idStr.Cmp(cmdName, cmd.name) == 0) {
                    if (function != cmd.function) {
                        common.Printf("idCmdSystemLocal::AddCommand: %s already defined\n", cmdName);
                    }
                    return;
                }
            }

            cmd = new commandDef_s();
            cmd.name = cmdName;//Mem_CopyString(cmdName);
            cmd.function = function;
            cmd.argCompletion = argCompletion;
            cmd.flags = flags;
            cmd.description = description;//Mem_CopyString(description);
            cmd.next = this.commands;
            this.commands = cmd;
        }

        @Override
        public void RemoveCommand(String cmdName) {
            commandDef_s cmd, last;

            for (last = cmd = this.commands; cmd != null; cmd = cmd.next) {
                if (idStr.Cmp(cmdName, cmd.name) == 0) {
                    if (cmd == this.commands) {//first iteration.
                        this.commands = cmd.next;//TODO:BOINTER. edit: check if this equals **last;
                    } else {//set last.next to last.next.next,
                        //where last.next is the current cmd. so basically setting overwriting the current node.
                        last.next = cmd.next;
                    }
//                    cmd.name = cmd.description = null;
//                    Mem_Free(cmd.name);
//                    Mem_Free(cmd.description);
//			delete cmd;
                    return;
                }
                last = cmd;
            }
        }

        @Override
        public void RemoveFlaggedCommands(int flags) {
            commandDef_s cmd, last;

            for (last = this.commands, cmd = last; cmd != null; cmd = last) {
                //if ((cmd.flags & flags) != 0) {
                    this.commands = cmd.next;
//                    cmd.name = cmd.description = null;
//                    Mem_Free(cmd.name);
//                    Mem_Free(cmd.description);
//			delete cmd;
                	//throw new TODO_Exception(); // endless loop
                    //continue;
                //}
                last = cmd.next;
            }
        }

        @Override
        public void CommandCompletion(void_callback<String> callback) throws idException {
            commandDef_s cmd;

            for (cmd = this.commands; cmd != null; cmd = cmd.next) {
                callback.run(cmd.name);
            }
        }

        @Override
        public void ArgCompletion(String cmdString, void_callback<String> callback) throws idException {
            commandDef_s cmd;
            final idCmdArgs args = new idCmdArgs();

            args.TokenizeString(cmdString, false);

            for (cmd = this.commands; cmd != null; cmd = cmd.next) {
                if (null == cmd.argCompletion) {
                    continue;
                }
                if (idStr.Icmp(args.Argv(0), cmd.name) == 0) {
                    cmd.argCompletion.run(args, callback);
                    break;
                }
            }
        }

        @Override
        public void BufferCommandText(cmdExecution_t exec, String text) throws idException {
            switch (exec) {
                case CMD_EXEC_NOW: {
                    ExecuteCommandText(text);
                    break;
                }
                case CMD_EXEC_INSERT: {
                    InsertCommandText(text);
                    break;
                }
                case CMD_EXEC_APPEND: {
                    AppendCommandText(text);
                    break;
                }
                default: {
                    common.FatalError("idCmdSystemLocal::BufferCommandText: bad exec type");
                }
            }
        }

        private static int DBG_ExecuteCommandBuffer = 0;
        @Override
        public void ExecuteCommandBuffer() throws idException {
            int i;
            char[] text = null;
            String txt;
            int quotes;
            idCmdArgs args = new idCmdArgs();
            

            while (this.textLength != 0) {
                DBG_ExecuteCommandBuffer++;

                if (this.wait != 0) {
                    // skip out while text still remains in buffer, leaving it for next frame
                    this.wait--;
                    break;
                }

                // find a \n or ; line break
                text = new String(this.textBuf).toCharArray();//TODO:??
                    
                quotes = 0;
                for (i = 0; i < this.textLength; i++) {
                    if (text[i] == '"') {
                        quotes++;
                    }
                    if ((0 == (quotes & 1)) && (text[i] == ';')) {
                        break;	// don't break if inside a quoted string
                    }
                    if ((text[i] == '\n') || (text[i] == '\r')) {
                        break;
                    }
                }

//                text[i] = 0;
                final String bla = new String(text);
                txt = bla.substring(0, i);//do not use ctos!
                if (0 == idStr.Cmp(txt, "_execTokenized")) {
                    args = this.tokenizedCmds.oGet(0);
                    this.tokenizedCmds.RemoveIndex(0);
                } else {
                    args.TokenizeString(txt, false);
                }

                // delete the text from the command buffer and move remaining commands down
                // this is necessary because commands (exec) can insert data at the
                // beginning of the text buffer
                if (i == this.textLength) {
                    this.textLength = 0;
                } else {
                    final byte[] textBuf2 = this.textBuf;
                    i++;
                    this.textLength -= i;
                    this.textBuf = new byte[this.textBuf.length];//memmove(text, text + i, textLength);
                    Nio.arraycopy(textBuf2, i, this.textBuf, 0, this.textLength);
                }

                // execute the command line that we have already tokenized
                ExecuteTokenizedString(args);
            }
        }

        @Override
        public void ArgCompletion_FolderExtension(idCmdArgs args, void_callback<String> callback, String folder, boolean stripFolder, Object... objects) throws idException {
            int i;
            String string;
//            String extension;
//            va_list argPtr;

            string = args.Argv(0);
            string += " ";
            string += args.Argv(1);

            if (this.completionString.Icmp(string) != 0) {
                idStr parm;
				final idStr path = new idStr();
                idFileList names;

                this.completionString = new idStr(string);
                this.completionParms.Clear();

                parm = new idStr(args.Argv(1));
                parm.ExtractFilePath(path);
                if (stripFolder || (path.Length() == 0)) {
                    path.oSet(folder).Append(path);
                }
                path.StripTrailing('/');

                // list folders
                names = fileSystem.ListFiles(path.getData(), "/", true, true);
                for (i = 0; i < names.GetNumFiles(); i++) {
                    idStr name = new idStr(names.GetFile(i));
                    if (stripFolder) {
                        name.Strip(folder);
                    } else {
                        name.Strip("/");
                    }
                    name = new idStr(args.Argv(0) + (" " + name) + "/");
                    this.completionParms.Append(name);
                }
                fileSystem.FreeFileList(names);

                // list files
//                va_start(argPtr, stripFolder);
//                for (extension = va_arg(argPtr, String); extension != null; extension = va_arg(argPtr, String)) {
                for (final Object extension : objects) {
                    names = fileSystem.ListFiles(path.getData(), extension.toString(), true, true);
                    for (i = 0; i < names.GetNumFiles(); i++) {
                        final idStr name = new idStr(names.GetFile(i));
                        if (stripFolder) {
                            name.Strip(folder);
                        } else {
                            name.Strip("/");
                        }
                        name.oSet(args.Argv(0) + (" " + name));
                        this.completionParms.Append(name);
                    }
                    fileSystem.FreeFileList(names);
                }
//                va_end(argPtr);
            }
            for (i = 0; i < this.completionParms.Num(); i++) {
                callback.run(this.completionParms.oGet(i).getData());
            }
        }

        @Override
        public void ArgCompletion_DeclName(idCmdArgs args, void_callback<String> callback, int type) throws idException {
            int i, num;

            if (declManager == null) {
                return;
            }
            num = declManager.GetNumDecls(declType_t.values()[type]);
            for (i = 0; i < num; i++) {
                callback.run(args.Argv(0) + " " + declManager.DeclByIndex(declType_t.values()[type], i, false).GetName());
            }
        }

        @Override
        public void BufferCommandArgs(cmdExecution_t exec, idCmdArgs args) throws idException {
            switch (exec) {
                case CMD_EXEC_NOW: {
                    ExecuteTokenizedString(args);
                    break;
                }
                case CMD_EXEC_APPEND: {
                    AppendCommandText("_execTokenized\n");
                    this.tokenizedCmds.Append(args);
                    break;
                }
                default: {
                    common.FatalError("idCmdSystemLocal::BufferCommandArgs: bad exec type");
                }
            }
        }

        @Override
        public void SetupReloadEngine(idCmdArgs args) throws idException {
            BufferCommandText(CMD_EXEC_APPEND, "reloadEngine\n");
            this.postReload = args;
        }

        @Override
        public boolean PostReloadEngine() throws idException {
            if (0 == this.postReload.Argc()) {
                return false;
            }
            BufferCommandArgs(CMD_EXEC_APPEND, this.postReload);
            this.postReload.Clear();
            return true;
        }
//        
//        
//        

        public void SetWait(int numFrames) {
            this.wait = numFrames;
        }

        public commandDef_s GetCommands() {
            return this.commands;
        }

        private void ExecuteTokenizedString(final idCmdArgs args) throws idException {
            commandDef_s cmd, prev;

            // execute the command line
            if (0 == args.Argc()) {
                return;		// no tokens
            }

            if (args.Argv(0).equals(NeoFixStrings.BLA1)) {
                args.oSet("map game/alphalabs1");//HACKME::11
            }

            // check registered command functions	
            for (prev = cmd = this.commands; cmd != null; cmd = cmd.next) {
//                cmd = prev;
                if (idStr.Icmp(args.Argv(0), cmd.name) == 0) {
                    // rearrange the links so that the command will be
                    // near the head of the list next time it is used
                    if (cmd != this.commands) {//no re-arranging necessary for first element.
                        prev.next = cmd.next;
                        cmd.next = this.commands;
                        this.commands = cmd;
                    }

                    if (((cmd.flags & (CMD_FL_CHEAT | CMD_FL_TOOL)) != 0) && (session != null)
                            && session.IsMultiplayer() && !cvarSystem.GetCVarBool("net_allowCheats")) {
                        common.Printf("Command '%s' not valid in multiplayer mode.\n", cmd.name);
                        return;
                    }
                    // perform the action
                    if (null == cmd.function) {
                        break;
                    } else {
                        cmd.function.run(args);
                    }
                    return;
                }
                prev = cmd;
            }

            // check cvars
            if (cvarSystem.Command(args)) {
                return;
            }

            common.Printf("Unknown command '%s'\n", args.Argv(0));
        }

        /*
         ============
         idCmdSystemLocal::ExecuteCommandText

         Tokenizes, then executes.
         ============
         */
        private void ExecuteCommandText(final String text) throws idException {
            ExecuteTokenizedString(new idCmdArgs(text, false));
        }

        /*
         ============
         idCmdSystemLocal::InsertCommandText

         Adds command text immediately after the current command
         Adds a \n to the text
         ============
         */
        private void InsertCommandText(final String text) throws idException {
            int len;
            int i;

            len = text.length() + 1;
            if ((len + this.textLength) > this.textBuf.length) {
                common.Printf("idCmdSystemLocal::InsertText: buffer overflow\n");
                return;
            }

            // move the existing command text
            for (i = this.textLength - 1; i >= 0; i--) {
                this.textBuf[i + len] = this.textBuf[i];
            }

            // copy the new text in
//            memcpy(textBuf, text, len - 1);
            Nio.arraycopy(text.getBytes(), 0, this.textBuf, 0, len - 1);

            // add a \n
            this.textBuf[len - 1] = '\n';

            this.textLength += len;
        }

        /*
         ============
         idCmdSystemLocal::AppendCommandText

         Adds command text at the end of the buffer, does NOT add a final \n
         ============
         */
        private void AppendCommandText(final String text) throws idException {
            int l;

            l = text.length();

            if ((this.textLength + l) >= this.textBuf.length) {
                common.Printf("idCmdSystemLocal::AppendText: buffer overflow\n");
                return;
            }
//	memcpy( textBuf + textLength, text, l );
            Nio.arraycopy(text.getBytes(), 0, this.textBuf, this.textLength, l);//TODO:check 1 at the end. EDIT: it was an L ya blind fool!
            this.textLength += l;
        }

        private static void ListByFlags(final idCmdArgs args, long cmdFlags_t) throws idException {
            int i;
            String match;
            commandDef_s cmd;
            final idList<commandDef_s> cmdList = new idList<commandDef_s>();

            if (args.Argc() > 1) {
                match = args.Args(1, -1);
                match = match.replaceAll(" ", "");
            } else {
                match = "";
            }

            for (cmd = cmdSystemLocal.GetCommands(); cmd != null; cmd = cmd.next) {
                if (0 == (cmd.flags & cmdFlags_t)) {
                    continue;
                }
                if (!match.isEmpty() && new idStr(cmd.name).Filter(match, false)) {
                    continue;
                }

                cmdList.Append(cmd);
            }

            cmdList.Sort();

            for (i = 0; i < cmdList.Num(); i++) {
                cmd = cmdList.oGet(i);

                common.Printf("  %-21s %s\n", cmd.name, cmd.description);
            }

            common.Printf("%d commands\n", cmdList.Num());
        }

        private static class List_f extends cmdFunction_t {

            private static final cmdFunction_t instance = new List_f();

            private List_f() {
            }

            public static cmdFunction_t getInstance() {
                return instance;
            }

            @Override
            public void run(idCmdArgs args) throws idException {
                idCmdSystemLocal.ListByFlags(args, CMD_FL_ALL);
            }
        }

        private static class SystemList_f extends cmdFunction_t {

            private static final cmdFunction_t instance = new SystemList_f();

            private SystemList_f() {
            }

            public static cmdFunction_t getInstance() {
                return instance;
            }

            @Override
            public void run(idCmdArgs args) throws idException {
                idCmdSystemLocal.ListByFlags(args, CMD_FL_SYSTEM);
            }
        }

        private static class RendererList_f extends cmdFunction_t {

            private static final cmdFunction_t instance = new RendererList_f();

            private RendererList_f() {
            }

            public static cmdFunction_t getInstance() {
                return instance;
            }

            @Override
            public void run(idCmdArgs args) throws idException {
                idCmdSystemLocal.ListByFlags(args, CMD_FL_RENDERER);
            }
        }

        private static class SoundList_f extends cmdFunction_t {

            private static final cmdFunction_t instance = new SoundList_f();

            private SoundList_f() {
            }

            public static cmdFunction_t getInstance() {
                return instance;
            }

            @Override
            public void run(idCmdArgs args) throws idException {
                idCmdSystemLocal.ListByFlags(args, CMD_FL_SOUND);
            }
        }

        private static class GameList_f extends cmdFunction_t {

            private static final cmdFunction_t instance = new GameList_f();

            private GameList_f() {
            }

            public static cmdFunction_t getInstance() {
                return instance;
            }

            @Override
            public void run(idCmdArgs args) throws idException {
                idCmdSystemLocal.ListByFlags(args, CMD_FL_GAME);
            }
        }

        private static class ToolList_f extends cmdFunction_t {

            private static final cmdFunction_t instance = new ToolList_f();

            private ToolList_f() {
            }

            public static cmdFunction_t getInstance() {
                return instance;
            }

            @Override
            public void run(idCmdArgs args) throws idException {
                idCmdSystemLocal.ListByFlags(args, CMD_FL_TOOL);
            }
        }
//private	static void				Exec_f( const idCmdArgs &args );

        private static class Exec_f extends cmdFunction_t {

            private static final cmdFunction_t instance = new Exec_f();

            private Exec_f() {
            }

            public static cmdFunction_t getInstance() {
                return instance;
            }

            @Override
            public void run(idCmdArgs args) throws idException {
                final ByteBuffer[] f = {null};
                int len;
                idStr filename;

                if (args.Argc() != 2) {
                    common.Printf("exec <filename> : execute a script file\n");
                    return;
                }

                filename = new idStr(args.Argv(1));
                filename.DefaultFileExtension(".cfg");
                len = fileSystem.ReadFile(filename.getData(),/*reinterpret_cast<void **>*/ f, null);
                if (null == f[0]) {
                    common.Printf("couldn't exec %s\n", args.Argv(1));
                    return;
                }
                common.Printf("execing %s\n", args.Argv(1));

                cmdSystemLocal.BufferCommandText(CMD_EXEC_INSERT, new String(f[0].array()));

                fileSystem.FreeFile(f);
            }
        }

        /*
         ===============
         idCmdSystemLocal::Vstr_f

         Inserts the current value of a cvar as command text
         ===============
         */
        private static class Vstr_f extends cmdFunction_t {

            private static final cmdFunction_t instance = new Vstr_f();

            private Vstr_f() {
            }

            public static cmdFunction_t getInstance() {
                return instance;
            }

            @Override
            public void run(idCmdArgs args) throws idException {
                String v;

                if (args.Argc() != 2) {
                    common.Printf("vstr <variablename> : execute a variable command\n");
                    return;
                }

                v = cvarSystem.GetCVarString(args.Argv(1));

                cmdSystemLocal.BufferCommandText(CMD_EXEC_APPEND, va("%s\n", v));
            }
        }

        /*
         ===============
         idCmdSystemLocal::Echo_f

         Just prints the rest of the line to the console
         ===============
         */
        private static class Echo_f extends cmdFunction_t {

            private static final cmdFunction_t instance = new Echo_f();

            private Echo_f() {
            }

            public static cmdFunction_t getInstance() {
                return instance;
            }

            @Override
            public void run(idCmdArgs args) throws idException {
                int i;

                for (i = 1; i < args.Argc(); i++) {
                    common.Printf("%s ", args.Argv(i));
                }
                common.Printf("\n");
            }
        }

        /*
         ============
         idCmdSystemLocal::Parse_f

         This just prints out how the rest of the line was parsed, as a debugging tool.
         ============
         */
        private static class Parse_f extends cmdFunction_t {

            private static final cmdFunction_t instance = new Parse_f();

            private Parse_f() {
            }

            public static cmdFunction_t getInstance() {
                return instance;
            }

            @Override
            public void run(idCmdArgs args) throws idException {
                int i;

                for (i = 0; i < args.Argc(); i++) {
                    common.Printf("%d: %s\n", i, args.Argv(i));
                }
            }
        }

        /*
         ============
         idCmdSystemLocal::Wait_f

         Causes execution of the remainder of the command buffer to be delayed until next frame.
         ============
         */
        private static class Wait_f extends cmdFunction_t {

            private static final cmdFunction_t instance = new Wait_f();

            private Wait_f() {
            }

            public static cmdFunction_t getInstance() {
                return instance;
            }

            @Override
            public void run(idCmdArgs args) {
                if (args.Argc() == 2) {
                    cmdSystemLocal.SetWait(Integer.parseInt(args.Argv(1)));
                } else {
                    cmdSystemLocal.SetWait(1);
                }
            }
        }
//private	static void				PrintMemInfo_f( const idCmdArgs &args );

        private static class PrintMemInfo_f extends cmdFunction_t {

            private static final cmdFunction_t instance = new PrintMemInfo_f();

            private PrintMemInfo_f() {
            }

            public static cmdFunction_t getInstance() {
                return instance;
            }

            @Override
            public void run(idCmdArgs args) throws idException {
                idCmdSystemLocal.ListByFlags(args, CMD_FL_SYSTEM);
            }
        }
    }

    /*
     ============
     idCmdSystemLocal::ListByFlags
     ============
     */
    // NOTE: the const wonkyness is required to make msvc happy
    public static class idListSortCompare implements cmp_t<commandDef_s> {

        @Override
        public int compare(final commandDef_s a, final commandDef_s b) {
            return idStr.Icmp(a.name, b.name);
        }
    }

    public static void setCmdSystem(idCmdSystem cmdSystem) {
        CmdSystem.cmdSystem = CmdSystem.cmdSystemLocal = (idCmdSystemLocal) cmdSystem;
    }
}
