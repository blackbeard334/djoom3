package neo.framework;

import static neo.TempDump.ctos;
import neo.TempDump.void_callback;
import static neo.framework.BuildDefines.MACOS_X;
import static neo.framework.CVarSystem.CVAR_ARCHIVE;
import static neo.framework.CVarSystem.cvarSystem;
import static neo.framework.CmdSystem.CMD_FL_SYSTEM;
import neo.framework.CmdSystem.argCompletion_t;
import static neo.framework.CmdSystem.cmdExecution_t.CMD_EXEC_APPEND;
import neo.framework.CmdSystem.cmdFunction_t;
import static neo.framework.CmdSystem.cmdSystem;
import static neo.framework.Common.common;
import neo.framework.File_h.idFile;
import static neo.framework.UsercmdGen.usercmdGen;
import neo.idlib.CmdArgs.idCmdArgs;
import static neo.idlib.Lib.MAX_STRING_CHARS;
import neo.idlib.Lib.idException;
import neo.idlib.Text.Str.idStr;
import static neo.idlib.Text.Str.va;
import static neo.sys.win_input.Sys_MapCharForKey;

/**
 *
 */
public class KeyInput {
    /*
     ===============================================================================

     Key Input

     ===============================================================================
     */

    // these are the key numbers that are used by the key system
// normal keys should be passed as lowercased ascii
// Some high ascii (> 127) characters that are mapped directly to keys on
// western european keyboards are inserted in this table so that those keys
// are bindable (otherwise they get bound as one of the special keys in this table)
//    
//    
    public static final int K_TAB                  = 9;
    public static final int K_ENTER                = 13;
    public static final int K_ESCAPE               = 27;
    public static final int K_SPACE                = 32;
    //
    public static final int K_BACKSPACE            = 127;
    //
    public static final int K_COMMAND              = 128;
    public static final int K_CAPSLOCK             = 129;
    public static final int K_SCROLL               = 130;
    public static final int K_POWER                = 131;
    public static final int K_PAUSE                = 132;
    //
    public static final int K_UPARROW              = 133;
    public static final int K_DOWNARROW            = 134;
    public static final int K_LEFTARROW            = 135;
    public static final int K_RIGHTARROW           = 136;
    //
//                          // The 3 windows keys
    public static final int K_LWIN                 = 137;
    public static final int K_RWIN                 = 138;
    public static final int K_MENU                 = 139;
    //
    public static final int K_ALT                  = 140;
    public static final int K_CTRL                 = 141;
    public static final int K_SHIFT                = 142;
    public static final int K_INS                  = 143;
    public static final int K_DEL                  = 144;
    public static final int K_PGDN                 = 145;
    public static final int K_PGUP                 = 146;
    public static final int K_HOME                 = 147;
    public static final int K_END                  = 148;
    //
    public static final int K_F1                   = 149;
    public static final int K_F2                   = 150;
    public static final int K_F3                   = 151;
    public static final int K_F4                   = 152;
    public static final int K_F5                   = 153;
    public static final int K_F6                   = 154;
    public static final int K_F7                   = 155;
    public static final int K_F8                   = 156;
    public static final int K_F9                   = 157;
    public static final int K_F10                  = 158;
    public static final int K_F11                  = 159;
    public static final int K_F12                  = 160;
    public static final int K_INVERTED_EXCLAMATION = 161;    // upside down !
    public static final int K_F13                  = 162;
    public static final int K_F14                  = 163;
    public static final int K_F15                  = 164;
    //
    public static final int K_KP_HOME              = 165;
    public static final int K_KP_UPARROW           = 166;
    public static final int K_KP_PGUP              = 167;
    public static final int K_KP_LEFTARROW         = 168;
    public static final int K_KP_5                 = 169;
    public static final int K_KP_RIGHTARROW        = 170;
    public static final int K_KP_END               = 171;
    public static final int K_KP_DOWNARROW         = 172;
    public static final int K_KP_PGDN              = 173;
    public static final int K_KP_ENTER             = 174;
    public static final int K_KP_INS               = 175;
    public static final int K_KP_DEL               = 176;
    public static final int K_KP_SLASH             = 177;
    public static final int K_SUPERSCRIPT_TWO      = 178;        // superscript 2
    public static final int K_KP_MINUS             = 179;
    public static final int K_ACUTE_ACCENT         = 180;        // accute accent
    public static final int K_KP_PLUS              = 180;
    public static final int K_KP_NUMLOCK           = 181;
    public static final int K_KP_STAR              = 182;
    public static final int K_KP_EQUALS            = 183;
    //
    public static final int K_MASCULINE_ORDINATOR  = 186;
    //                       // K_MOUSE enums must be contiguous (no char codes in the middle)
    public static final int K_MOUSE1               = 187;
    public static final int K_MOUSE2               = 188;
    public static final int K_MOUSE3               = 189;
    public static final int K_MOUSE4               = 190;
    public static final int K_MOUSE5               = 191;
    public static final int K_MOUSE6               = 192;
    public static final int K_MOUSE7               = 193;
    public static final int K_MOUSE8               = 194;
    //
    public static final int K_MWHEELDOWN           = 195;
    public static final int K_MWHEELUP             = 196;
    //
    public static final int K_JOY1                 = 197;
    public static final int K_JOY2                 = 198;
    public static final int K_JOY3                 = 199;
    public static final int K_JOY4                 = 200;
    public static final int K_JOY5                 = 201;
    public static final int K_JOY6                 = 202;
    public static final int K_JOY7                 = 203;
    public static final int K_JOY8                 = 204;
    public static final int K_JOY9                 = 205;
    public static final int K_JOY10                = 206;
    public static final int K_JOY11                = 207;
    public static final int K_JOY12                = 208;
    public static final int K_JOY13                = 209;
    public static final int K_JOY14                = 210;
    public static final int K_JOY15                = 211;
    public static final int K_JOY16                = 212;
    public static final int K_JOY17                = 213;
    public static final int K_JOY18                = 214;
    public static final int K_JOY19                = 215;
    public static final int K_JOY20                = 216;
    public static final int K_JOY21                = 217;
    public static final int K_JOY22                = 218;
    public static final int K_JOY23                = 219;
    public static final int K_JOY24                = 220;
    public static final int K_JOY25                = 221;
    public static final int K_JOY26                = 222;
    public static final int K_JOY27                = 223;
    public static final int K_GRAVE_A              = 224;    // lowercase a with grave accent
    public static final int K_JOY28                = 225;
    public static final int K_JOY29                = 226;
    public static final int K_JOY30                = 227;
    public static final int K_JOY31                = 228;
    public static final int K_JOY32                = 229;
    //
    public static final int K_AUX1                 = 230;
    public static final int K_CEDILLA_C            = 231;    // lowercase c with Cedilla
    public static final int K_GRAVE_E              = 232;    // lowercase e with grave accent
    public static final int K_AUX2                 = 231;
    public static final int K_AUX3                 = 232;
    public static final int K_AUX4                 = 233;
    public static final int K_GRAVE_I              = 236;    // lowercase i with grave accent
    public static final int K_AUX5                 = 237;
    public static final int K_AUX6                 = 238;
    public static final int K_AUX7                 = 239;
    public static final int K_AUX8                 = 240;
    public static final int K_TILDE_N              = 241;    // lowercase n with tilde
    public static final int K_GRAVE_O              = 242;    // lowercase o with grave accent
    public static final int K_AUX9                 = 243;
    public static final int K_AUX10                = 244;
    public static final int K_AUX11                = 245;
    public static final int K_AUX12                = 246;
    public static final int K_AUX13                = 247;
    public static final int K_AUX14                = 248;
    public static final int K_GRAVE_U              = 249;    // lowercase u with grave accent
    public static final int K_AUX15                = 250;
    public static final int K_AUX16                = 251;
    //
    public static final int K_PRINT_SCR            = 252;    // SysRq / PrintScr
    public static final int K_RIGHT_ALT            = 253;    // used by some languages as "Alt-Gr"
    public static final int K_LAST_KEY             = 254;    // this better be < 256!
//    
//    

    public static class idKeyInput {

        public static void Init() throws idException {

            keys = new idKey[MAX_KEYS];
            for (int k = 0; k < keys.length; k++) {
                keys[k] = new idKey();
            }

            // register our functions
            cmdSystem.AddCommand("bind", Key_Bind_f.getInstance(), CMD_FL_SYSTEM, "binds a command to a key", idKeyInput.ArgCompletion_KeyName.getInstance());
            cmdSystem.AddCommand("bindunbindtwo", Key_BindUnBindTwo_f.getInstance(), CMD_FL_SYSTEM, "binds a key but unbinds it first if there are more than two binds");
            cmdSystem.AddCommand("unbind", Key_Unbind_f.getInstance(), CMD_FL_SYSTEM, "unbinds any command from a key", idKeyInput.ArgCompletion_KeyName.getInstance());
            cmdSystem.AddCommand("unbindall", Key_Unbindall_f.getInstance(), CMD_FL_SYSTEM, "unbinds any commands from all keys");
            cmdSystem.AddCommand("listBinds", Key_ListBinds_f.getInstance(), CMD_FL_SYSTEM, "lists key bindings");
        }

        public static void Shutdown() {
//	delete [] keys;
            keys = null;
        }
//

        public static class ArgCompletion_KeyName extends argCompletion_t {

            private static final argCompletion_t instance = new ArgCompletion_KeyName();

            public static argCompletion_t getInstance() {
                return instance;
            }

            @Override
            public void run(idCmdArgs args, void_callback<String> callback) throws idException {
                int kn;
                int i;

                for (i = 0; i < unnamedkeys.length() - 1; i++) {
                    callback.run(va("%s %c", args.Argv(0), unnamedkeys.charAt(i)));
                }

                for (kn = 0; kn < keynames.length; kn++) {
                    callback.run(va("%s %s", args.Argv(0), keynames[kn].name));
                }
            }
        }

        /*
         ===================
         idKeyInput::PreliminaryKeyEvent

         Tracks global key up/down state
         Called by the system for both key up and key down events
         ===================
         */
        public static void PreliminaryKeyEvent(int keyNum, boolean down) throws idException {
            keys[keyNum].down = down;

            if (ID_DOOM_LEGACY) {
                if (down) {
                    lastKeys[ 0 + (lastKeyIndex & 15)] = (char) keyNum;
                    lastKeys[16 + (lastKeyIndex & 15)] = (char) keyNum;
                    lastKeyIndex = (lastKeyIndex + 1) & 15;
                    for (int i = 0; cheatCodes[i] != null; i++) {
                        int l = cheatCodes[i].length();
                        assert (l <= 16);
                        if (idStr.Icmpn(ctos(lastKeys).substring(16 + (lastKeyIndex & 15) - l), cheatCodes[i], l) == 0) {
                            common.Printf("your memory serves you well!\n");
                            break;
                        }
                    }
                }
            }
        }

        public static boolean IsDown(int keyNum) {
            if (keyNum == -1) {
                return false;
            }

            return keys[keyNum].down;
        }

        public static int GetUsercmdAction(int keyNum) {
            return keys[keyNum].usercmdAction;
        }

        public static boolean GetOverstrikeMode() {
            return key_overstrikeMode;
        }

        public static void SetOverstrikeMode(boolean state) {
            key_overstrikeMode = state;
        }

        public static void ClearStates() throws idException {
            int i;

            for (i = 0; i < MAX_KEYS; i++) {
                if (keys[i].down) {
                    PreliminaryKeyEvent(i, false);
                }
                keys[i].down = false;
            }

            // clear the usercommand states
            usercmdGen.Clear();
        }

        /*
         ===================
         idKeyInput::StringToKeyNum

         Returns a key number to be used to index keys[] by looking at
         the given string.  Single ascii characters return themselves, while
         the K_* names are matched up.

         0x11 will be interpreted as raw hex, which will allow new controlers
         to be configured even if they don't have defined names.
         ===================
         */
        public static int StringToKeyNum(final String str) {
            int kn;

            if (null == str || str.isEmpty()) {
                return -1;
            }
            if (1 == str.length()) {
                return str.charAt(0);
            }

            // check for hex code
            if (str.charAt(0) == '0' && str.charAt(0) == 'x' && str.length() == 4) {
                int n1, n2;

                n1 = str.charAt(2);
                if (n1 >= '0' && n1 <= '9') {
                    n1 -= '0';
                } else if (n1 >= 'a' && n1 <= 'f') {
                    n1 = n1 - 'a' + 10;
                } else {
                    n1 = 0;
                }

                n2 = str.charAt(3);
                if (n2 >= '0' && n2 <= '9') {
                    n2 -= '0';
                } else if (n2 >= 'a' && n2 <= 'f') {
                    n2 = n2 - 'a' + 10;
                } else {
                    n2 = 0;
                }

                return n1 * 16 + n2;
            }

            // scan for a text match
            for (kn = 0; kn < keynames.length; kn++) {
                if (0 == idStr.Icmp(str, keynames[kn].name)) {
                    return keynames[kn].keynum;
                }
            }

            return -1;
        }
        /*
         ===================
         idKeyInput::KeyNumToString

         Returns a string (either a single ascii char, a K_* name, or a 0x11 hex string) for the
         given keynum.
         ===================
         */
        static char[] tinystr = new char[5];

        public static String KeyNumToString(int keyNum, boolean localized) throws idException {
//	keyname_t	kn;	
            int i, j;

            if (keyNum == -1) {
                return "<KEY NOT FOUND>";
            }

            if (keyNum < 0 || keyNum > 255) {
                return "<OUT OF RANGE>";
            }

            // check for printable ascii (don't use quote)
            if (keyNum > 32 && keyNum < 127 && keyNum != '"' && keyNum != ';' && keyNum != '\'') {
                tinystr[0] = Sys_MapCharForKey(keyNum);
                tinystr[1] = 0;
                return ctos(tinystr);
            }

            // check for a key string
            for (keyname_t kn : keynames) {
                if (keyNum == kn.keynum) {
                    if (!localized || kn.strId.charAt(0) != '#') {
                        return kn.name;
                    } else {
                        if (MACOS_X) {

                            switch (kn.keynum) {
                                case K_ENTER:
                                case K_BACKSPACE:
                                case K_ALT:
                                case K_INS:
                                case K_PRINT_SCR:
//                                    return OSX_GetLocalizedString(kn.name);
//                                    break;
                                default:
                                    return common.GetLanguageDict().GetString(kn.strId);
//                                    break;
                            }
                        } else {
                            return common.GetLanguageDict().GetString(kn.strId);
                        }
                    }
                }
            }

            // check for European high-ASCII characters
            if (localized && keyNum >= 161 && keyNum <= 255) {
                tinystr[0] = (char) keyNum;
                tinystr[1] = 0;
                return ctos(tinystr);
            }

            // make a hex string
            i = keyNum >> 4;
            j = keyNum & 15;

            tinystr[0] = '0';
            tinystr[1] = 'x';
            tinystr[2] = (char) (i > 9 ? i - 10 + 'a' : i + '0');
            tinystr[3] = (char) (j > 9 ? j - 10 + 'a' : j + '0');
            tinystr[4] = 0;

            return ctos(tinystr);
        }

        public static void SetBinding(int keyNum, final String binding) {
            if (keyNum == -1) {
                return;
            }

            // Clear out all button states so we aren't stuck forever thinking this key is held down
            usercmdGen.Clear();

            // allocate memory for new binding
            keys[keyNum].binding = new idStr(binding);

            // find the action for the async command generation
            keys[keyNum].usercmdAction = usercmdGen.CommandStringUsercmdData(binding);

            // consider this like modifying an archived cvar, so the
            // file write will be triggered at the next oportunity
            cvarSystem.SetModifiedFlags(CVAR_ARCHIVE);
        }

        public static String GetBinding(int keyNum) {
            if (keyNum == -1) {
                return "";
            }

            return keys[keyNum].binding.toString();
        }

        public static boolean UnbindBinding(final String binding) {
            boolean unbound = false;
            int i;

            if (binding != null) {
                for (i = 0; i < MAX_KEYS; i++) {
                    if (keys[i].binding.Icmp(binding) == 0) {
                        SetBinding(i, "");
                        unbound = true;
                    }
                }
            }
            return unbound;
        }

        public static int NumBinds(final String binding) {
            int i, count = 0;

            if (binding != null) {
                for (i = 0; i < MAX_KEYS; i++) {
                    if (keys[i].binding.Icmp(binding) == 0) {
                        count++;
                    }
                }
            }
            return count;
        }

        public static boolean ExecKeyBinding(int keyNum) throws idException {
            // commands that are used by the async thread
            // don't add text
            if (keys[keyNum].usercmdAction != 0) {
                return false;
            }

            // send the bound action
            if (keys[keyNum].binding.Length() != 0) {
                cmdSystem.BufferCommandText(CMD_EXEC_APPEND, keys[keyNum].binding.toString());
                cmdSystem.BufferCommandText(CMD_EXEC_APPEND, "\n");
            }
            return true;
        }
        /*
         ============
         idKeyInput::KeysFromBinding
         returns the localized name of the key for the binding
         ============
         */
        private static char[] keyName = new char[MAX_STRING_CHARS];

        public static String KeysFromBinding(final String bind) throws idException {
            int i;

            keyName[0] = '\0';
            if (bind != null) {
                for (i = 0; i < MAX_KEYS; i++) {
                    if (keys[i].binding.Icmp(bind) == 0) {
                        if (keyName[0] != '\0') {
                            idStr.Append(keyName, MAX_STRING_CHARS, common.GetLanguageDict().GetString("#str_07183"));
                        }
                        idStr.Append(keyName, (keyName).length, KeyNumToString(i, true));
                    }
                }
            }
            if (keyName[0] == '\0') {
                idStr.Copynz(keyName, common.GetLanguageDict().GetString("#str_07133"), (keyName).length);
            }
            idStr.ToLower(keyName);
            return ctos(keyName);
        }

        /*
         ============
         idKeyInput::BindingFromKey
         returns the binding for the localized name of the key
         ============
         */
        public static String BindingFromKey(final String key) {
            final int keyNum = idKeyInput.StringToKeyNum(key);
            if (keyNum < 0 || keyNum >= MAX_KEYS) {
                return null;
            }
            return keys[keyNum].binding.toString();
        }

        public static boolean KeyIsBoundTo(int keyNum, final String binding) {
            if (keyNum >= 0 && keyNum < MAX_KEYS) {
                return (keys[keyNum].binding.Icmp(binding) == 0);
            }
            return false;
        }

        /*
         ============
         idKeyInput::WriteBindings

         Writes lines containing "bind key value"
         ============
         */
        public static void WriteBindings(idFile f) throws idException {
            int i;

            f.Printf("unbindall\n");

            for (i = 0; i < MAX_KEYS; i++) {
                if (keys[i].binding.Length() != 0) {
                    final String name = KeyNumToString(i, false);

                    // handle the escape character nicely
                    if ("\\".equals(name)) {
                        f.Printf("bind \"\\\" \"%s\"\n", keys[i].binding);
                    } else {
                        f.Printf("bind \"%s\" \"%s\"\n", KeyNumToString(i, false), keys[i].binding);
                    }
                }
            }
        }
    };

    static class keyname_t {

        public keyname_t(String name, int keynum, String strId) {
            this.name = name;
            this.keynum = keynum;
            this.strId = strId;
        }
        String name;
        int keynum;
        String strId;	// localized string id
    };
//    
// keys that can be set without a special name
    static final String unnamedkeys = "*,-=./[\\]1234567890abcdefghijklmnopqrstuvwxyz";
//    
//    
// #if MACOS_X
// const char* OSX_GetLocalizedString( const char* );
// #endif
//    
//    
// names not in this list can either be lowercase ascii, or '0xnn' hex sequences
    static final keyname_t keynames[] = {
        new keyname_t("TAB", K_TAB, "#str_07018"),
        new keyname_t("ENTER", K_ENTER, "#str_07019"),
        new keyname_t("ESCAPE", K_ESCAPE, "#str_07020"),
        new keyname_t("SPACE", K_SPACE, "#str_07021"),
        new keyname_t("BACKSPACE", K_BACKSPACE, "#str_07022"),
        new keyname_t("UPARROW", K_UPARROW, "#str_07023"),
        new keyname_t("DOWNARROW", K_DOWNARROW, "#str_07024"),
        new keyname_t("LEFTARROW", K_LEFTARROW, "#str_07025"),
        new keyname_t("RIGHTARROW", K_RIGHTARROW, "#str_07026"),
        //
        new keyname_t("ALT", K_ALT, "#str_07027"),
        new keyname_t("RIGHTALT", K_RIGHT_ALT, "#str_07027"),
        new keyname_t("CTRL", K_CTRL, "#str_07028"),
        new keyname_t("SHIFT", K_SHIFT, "#str_07029"),
        //
        new keyname_t("LWIN", K_LWIN, "#str_07030"),
        new keyname_t("RWIN", K_RWIN, "#str_07031"),
        new keyname_t("MENU", K_MENU, "#str_07032"),
        //
        new keyname_t("COMMAND", K_COMMAND, "#str_07033"),
        //
        new keyname_t("CAPSLOCK", K_CAPSLOCK, "#str_07034"),
        new keyname_t("SCROLL", K_SCROLL, "#str_07035"),
        new keyname_t("PRINTSCREEN", K_PRINT_SCR, "#str_07179"),
        //	
        new keyname_t("F1", K_F1, "#str_07036"),
        new keyname_t("F2", K_F2, "#str_07037"),
        new keyname_t("F3", K_F3, "#str_07038"),
        new keyname_t("F4", K_F4, "#str_07039"),
        new keyname_t("F5", K_F5, "#str_07040"),
        new keyname_t("F6", K_F6, "#str_07041"),
        new keyname_t("F7", K_F7, "#str_07042"),
        new keyname_t("F8", K_F8, "#str_07043"),
        new keyname_t("F9", K_F9, "#str_07044"),
        new keyname_t("F10", K_F10, "#str_07045"),
        new keyname_t("F11", K_F11, "#str_07046"),
        new keyname_t("F12", K_F12, "#str_07047"),
        //
        new keyname_t("INS", K_INS, "#str_07048"),
        new keyname_t("DEL", K_DEL, "#str_07049"),
        new keyname_t("PGDN", K_PGDN, "#str_07050"),
        new keyname_t("PGUP", K_PGUP, "#str_07051"),
        new keyname_t("HOME", K_HOME, "#str_07052"),
        new keyname_t("END", K_END, "#str_07053"),
        //
        new keyname_t("MOUSE1", K_MOUSE1, "#str_07054"),
        new keyname_t("MOUSE2", K_MOUSE2, "#str_07055"),
        new keyname_t("MOUSE3", K_MOUSE3, "#str_07056"),
        new keyname_t("MOUSE4", K_MOUSE4, "#str_07057"),
        new keyname_t("MOUSE5", K_MOUSE5, "#str_07058"),
        new keyname_t("MOUSE6", K_MOUSE6, "#str_07059"),
        new keyname_t("MOUSE7", K_MOUSE7, "#str_07060"),
        new keyname_t("MOUSE8", K_MOUSE8, "#str_07061"),
        //
        new keyname_t("MWHEELUP", K_MWHEELUP, "#str_07131"),
        new keyname_t("MWHEELDOWN", K_MWHEELDOWN, "#str_07132"),
        //
        new keyname_t("JOY1", K_JOY1, "#str_07062"),
        new keyname_t("JOY2", K_JOY2, "#str_07063"),
        new keyname_t("JOY3", K_JOY3, "#str_07064"),
        new keyname_t("JOY4", K_JOY4, "#str_07065"),
        new keyname_t("JOY5", K_JOY5, "#str_07066"),
        new keyname_t("JOY6", K_JOY6, "#str_07067"),
        new keyname_t("JOY7", K_JOY7, "#str_07068"),
        new keyname_t("JOY8", K_JOY8, "#str_07069"),
        new keyname_t("JOY9", K_JOY9, "#str_07070"),
        new keyname_t("JOY10", K_JOY10, "#str_07071"),
        new keyname_t("JOY11", K_JOY11, "#str_07072"),
        new keyname_t("JOY12", K_JOY12, "#str_07073"),
        new keyname_t("JOY13", K_JOY13, "#str_07074"),
        new keyname_t("JOY14", K_JOY14, "#str_07075"),
        new keyname_t("JOY15", K_JOY15, "#str_07076"),
        new keyname_t("JOY16", K_JOY16, "#str_07077"),
        new keyname_t("JOY17", K_JOY17, "#str_07078"),
        new keyname_t("JOY18", K_JOY18, "#str_07079"),
        new keyname_t("JOY19", K_JOY19, "#str_07080"),
        new keyname_t("JOY20", K_JOY20, "#str_07081"),
        new keyname_t("JOY21", K_JOY21, "#str_07082"),
        new keyname_t("JOY22", K_JOY22, "#str_07083"),
        new keyname_t("JOY23", K_JOY23, "#str_07084"),
        new keyname_t("JOY24", K_JOY24, "#str_07085"),
        new keyname_t("JOY25", K_JOY25, "#str_07086"),
        new keyname_t("JOY26", K_JOY26, "#str_07087"),
        new keyname_t("JOY27", K_JOY27, "#str_07088"),
        new keyname_t("JOY28", K_JOY28, "#str_07089"),
        new keyname_t("JOY29", K_JOY29, "#str_07090"),
        new keyname_t("JOY30", K_JOY30, "#str_07091"),
        new keyname_t("JOY31", K_JOY31, "#str_07092"),
        new keyname_t("JOY32", K_JOY32, "#str_07093"),
        //
        new keyname_t("AUX1", K_AUX1, "#str_07094"),
        new keyname_t("AUX2", K_AUX2, "#str_07095"),
        new keyname_t("AUX3", K_AUX3, "#str_07096"),
        new keyname_t("AUX4", K_AUX4, "#str_07097"),
        new keyname_t("AUX5", K_AUX5, "#str_07098"),
        new keyname_t("AUX6", K_AUX6, "#str_07099"),
        new keyname_t("AUX7", K_AUX7, "#str_07100"),
        new keyname_t("AUX8", K_AUX8, "#str_07101"),
        new keyname_t("AUX9", K_AUX9, "#str_07102"),
        new keyname_t("AUX10", K_AUX10, "#str_07103"),
        new keyname_t("AUX11", K_AUX11, "#str_07104"),
        new keyname_t("AUX12", K_AUX12, "#str_07105"),
        new keyname_t("AUX13", K_AUX13, "#str_07106"),
        new keyname_t("AUX14", K_AUX14, "#str_07107"),
        new keyname_t("AUX15", K_AUX15, "#str_07108"),
        new keyname_t("AUX16", K_AUX16, "#str_07109"),
        //
        new keyname_t("KP_HOME", K_KP_HOME, "#str_07110"),
        new keyname_t("KP_UPARROW", K_KP_UPARROW, "#str_07111"),
        new keyname_t("KP_PGUP", K_KP_PGUP, "#str_07112"),
        new keyname_t("KP_LEFTARROW", K_KP_LEFTARROW, "#str_07113"),
        new keyname_t("KP_5", K_KP_5, "#str_07114"),
        new keyname_t("KP_RIGHTARROW", K_KP_RIGHTARROW, "#str_07115"),
        new keyname_t("KP_END", K_KP_END, "#str_07116"),
        new keyname_t("KP_DOWNARROW", K_KP_DOWNARROW, "#str_07117"),
        new keyname_t("KP_PGDN", K_KP_PGDN, "#str_07118"),
        new keyname_t("KP_ENTER", K_KP_ENTER, "#str_07119"),
        new keyname_t("KP_INS", K_KP_INS, "#str_07120"),
        new keyname_t("KP_DEL", K_KP_DEL, "#str_07121"),
        new keyname_t("KP_SLASH", K_KP_SLASH, "#str_07122"),
        new keyname_t("KP_MINUS", K_KP_MINUS, "#str_07123"),
        new keyname_t("KP_PLUS", K_KP_PLUS, "#str_07124"),
        new keyname_t("KP_NUMLOCK", K_KP_NUMLOCK, "#str_07125"),
        new keyname_t("KP_STAR", K_KP_STAR, "#str_07126"),
        new keyname_t("KP_EQUALS", K_KP_EQUALS, "#str_07127"),
        //
        new keyname_t("PAUSE", K_PAUSE, "#str_07128"),
        //
        new keyname_t("SEMICOLON", ';', "#str_07129"), // because a raw semicolon separates commands
        new keyname_t("APOSTROPHE", '\'', "#str_07130"), // because a raw apostrophe messes with parsing
        //
        new keyname_t(null, 0, null)
    };
//    
//    
    static final int MAX_KEYS = 256;
//    
//    

    private static class idKey {

        public boolean down;
        public int repeats;		// if > 1, it is autorepeating
        public idStr binding;
        public int usercmdAction;	// for testing by the asyncronous usercmd generation

        public idKey() {
            down = false;
            repeats = 0;
            binding = new idStr();
            usercmdAction = 0;
        }
    };
//    
//    
    static boolean key_overstrikeMode = false;
    static idKey[] keys = null;
//    
//    
    static final boolean ID_DOOM_LEGACY = false;
//    
    static final String[] cheatCodes = {
        "iddqd", // Invincibility
        "idkfa", // All weapons, keys, ammo, and 200% armor
        "idfa", // Reset ammunition
        "idspispopd", // Walk through walls
        "idclip", // Walk through walls
        "idchoppers", // Chainsaw
        /*
         "idbeholds",	// Berserker strength
         "idbeholdv",	// Temporary invincibility
         "idbeholdi",	// Temporary invisibility
         "idbeholda",	// Full automap
         "idbeholdr",	// Anti-radiation suit
         "idbeholdl",	// Light amplification visor
         "idclev",		// Level select
         "iddt",			// Toggle full map; full map and objects; normal map
         "idmypos",		// Display coordinates and heading
         "idmus",		// Change music to indicated level
         "fhhall",		// Kill all enemies in level
         "fhshh",		// Invisible to enemies until attack
         */
        null
    };
//    
    static char[] lastKeys = new char[32];
    static int lastKeyIndex;
    /////////////////////////////
    /*
     ===================
     Key_Unbind_f
     ===================
     */

    static class Key_Unbind_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new Key_Unbind_f();

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) throws idException {
            int b;

            if (args.Argc() != 2) {
                common.Printf("unbind <key> : remove commands from a key\n");
                return;
            }

            b = idKeyInput.StringToKeyNum(args.Argv(1));
            if (b == -1) {
                // If it wasn't a key, it could be a command
                if (!idKeyInput.UnbindBinding(args.Argv(1))) {
                    common.Printf("\"%s\" isn't a valid key\n", args.Argv(1));
                }
            } else {
                idKeyInput.SetBinding(b, "");
            }
        }
    };

    /*
     ===================
     Key_Unbindall_f
     ===================
     */
    static class Key_Unbindall_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new Key_Unbindall_f();

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) {
            int i;

            for (i = 0; i < MAX_KEYS; i++) {
                idKeyInput.SetBinding(i, "");
            }
        }
    };

    /*
     ===================
     Key_Bind_f
     ===================
     */
    static class Key_Bind_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new Key_Bind_f();

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) throws idException {
            int i, c, b;
            String cmd;//= new char[MAX_STRING_CHARS];

            c = args.Argc();

            if (c < 2) {
                common.Printf("bind <key> [command] : attach a command to a key\n");
                return;
            }
            b = idKeyInput.StringToKeyNum(args.Argv(1));
            if (b == -1) {
                common.Printf("\"%s\" isn't a valid key\n", args.Argv(1));
                return;
            }

            if (c == 2) {
                if (keys[b].binding.Length() != 0) {
                    common.Printf("\"%s\" = \"%s\"\n", args.Argv(1), keys[b].binding.toString());
                } else {
                    common.Printf("\"%s\" is not bound\n", args.Argv(1));
                }
                return;
            }

            // copy the rest of the command line
            cmd = "";//[0] = 0;		// start out with a null string
//            String cmd_str=new String (cmd);
            for (i = 2; i < c; i++) {
                cmd += args.Argv(i);
                if (i != (c - 1)) {
                    cmd += " ";
                }
            }

            idKeyInput.SetBinding(b, cmd);
        }
    };

    /*
     ============
     Key_BindUnBindTwo_f

     binds keynum to bindcommand and unbinds if there are already two binds on the key
     ============
     */
    static class Key_BindUnBindTwo_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new Key_BindUnBindTwo_f();

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) throws idException {
            int c = args.Argc();
            if (c < 3) {
                common.Printf("bindunbindtwo <keynum> [command]\n");
                return;
            }
            int key = Integer.parseInt(args.Argv(1));
            final String bind = args.Argv(2);
            if (idKeyInput.NumBinds(bind) >= 2 && !idKeyInput.KeyIsBoundTo(key, bind)) {
                idKeyInput.UnbindBinding(bind);
            }
            idKeyInput.SetBinding(key, bind);
        }
    };


    /*
     ============
     Key_ListBinds_f
     ============
     */
    static class Key_ListBinds_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new Key_ListBinds_f();

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) throws idException {
            int i;

            for (i = 0; i < MAX_KEYS; i++) {
                if (keys[i].binding.Length() != 0) {
                    common.Printf("%s \"%s\"\n", idKeyInput.KeyNumToString(i, false), keys[i].binding.toString());
                }
            }
        }
    };
}
