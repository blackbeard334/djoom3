package neo.framework;

import neo.Renderer.Material.idMaterial;
import static neo.Renderer.RenderSystem.BIGCHAR_HEIGHT;
import static neo.Renderer.RenderSystem.BIGCHAR_WIDTH;
import static neo.Renderer.RenderSystem.SCREEN_HEIGHT;
import static neo.Renderer.RenderSystem.SCREEN_WIDTH;
import static neo.Renderer.RenderSystem.SMALLCHAR_HEIGHT;
import static neo.Renderer.RenderSystem.SMALLCHAR_WIDTH;
import static neo.Renderer.RenderSystem.renderSystem;
import static neo.Sound.snd_system.soundSystem;
import neo.Sound.sound.soundDecoderInfo_t;
import static neo.TempDump.NOT;
import static neo.TempDump.ctos;
import static neo.TempDump.strLen;
import static neo.Tools.edit_public.MaterialEditorPrintConsole;
import static neo.Tools.edit_public.RadiantPrint;
import static neo.framework.Async.AsyncNetwork.MAX_ASYNC_CLIENTS;
import neo.framework.Async.AsyncNetwork.idAsyncNetwork;
import static neo.framework.BuildDefines.ID_ALLOW_TOOLS;
import static neo.framework.BuildDefines.ID_CONSOLE_LOCK;
import static neo.framework.BuildVersion.BUILD_NUMBER;
import static neo.framework.CVarSystem.CVAR_BOOL;
import static neo.framework.CVarSystem.CVAR_NOCHEAT;
import static neo.framework.CVarSystem.CVAR_SYSTEM;
import static neo.framework.CVarSystem.cvarSystem;
import neo.framework.CVarSystem.idCVar;
import static neo.framework.CmdSystem.CMD_FL_SYSTEM;
import static neo.framework.CmdSystem.cmdExecution_t.CMD_EXEC_APPEND;
import neo.framework.CmdSystem.cmdFunction_t;
import static neo.framework.CmdSystem.cmdSystem;
import static neo.framework.Common.EDITOR_MATERIAL;
import static neo.framework.Common.com_allowConsole;
import static neo.framework.Common.com_editors;
import static neo.framework.Common.com_frameTime;
import static neo.framework.Common.com_showAsyncStats;
import static neo.framework.Common.com_showFPS;
import static neo.framework.Common.com_showMemoryUsage;
import static neo.framework.Common.com_showSoundDecoders;
import static neo.framework.Common.common;
import static neo.framework.DeclManager.declManager;
import neo.framework.EditField.idEditField;
import static neo.framework.EventLoop.eventLoop;
import static neo.framework.FileSystem_h.fileSystem;
import neo.framework.File_h.idFile;
import static neo.framework.KeyInput.K_ALT;
import static neo.framework.KeyInput.K_CTRL;
import static neo.framework.KeyInput.K_DOWNARROW;
import static neo.framework.KeyInput.K_END;
import static neo.framework.KeyInput.K_ENTER;
import static neo.framework.KeyInput.K_F1;
import static neo.framework.KeyInput.K_F12;
import static neo.framework.KeyInput.K_HOME;
import static neo.framework.KeyInput.K_KP_ENTER;
import static neo.framework.KeyInput.K_MWHEELDOWN;
import static neo.framework.KeyInput.K_MWHEELUP;
import static neo.framework.KeyInput.K_PGDN;
import static neo.framework.KeyInput.K_PGUP;
import static neo.framework.KeyInput.K_SHIFT;
import static neo.framework.KeyInput.K_TAB;
import static neo.framework.KeyInput.K_UPARROW;
import neo.framework.KeyInput.idKeyInput;
import static neo.framework.Licensee.ENGINE_VERSION;
import static neo.framework.Session.session;
import neo.idlib.CmdArgs.idCmdArgs;
import neo.idlib.Heap.memoryStats_t;
import static neo.idlib.Lib.MAX_STRING_CHARS;
import static neo.idlib.Lib.colorCyan;
import static neo.idlib.Lib.colorWhite;
import neo.idlib.Lib.idException;
import static neo.idlib.Text.Str.C_COLOR_CYAN;
import static neo.idlib.Text.Str.C_COLOR_DEFAULT;
import static neo.idlib.Text.Str.C_COLOR_WHITE;
import neo.idlib.Text.Str.idStr;
import static neo.idlib.Text.Str.va;
import neo.idlib.math.Math_h.idMath;
import neo.idlib.math.Vector.idVec4;
import static neo.sys.sys_public.sysEventType_t.SE_CHAR;
import static neo.sys.sys_public.sysEventType_t.SE_KEY;
import neo.sys.sys_public.sysEvent_s;
import static neo.sys.win_input.Sys_GetConsoleKey;
import static neo.sys.win_input.Sys_GrabMouseCursor;
import neo.sys.win_main;
import static neo.sys.win_shared.Sys_Milliseconds;

/**
 *
 */
public class Console {

    static final idConsoleLocal localConsole = new idConsoleLocal();
    public static final idConsole console = localConsole;	// statically initialized to an idConsoleLocal

    /*
     ===============================================================================

     The console is strictly for development and advanced users. It should
     never be used to convey actual game information to the user, which should
     always be done through a GUI.

     The force options are for the editor console display window, which
     doesn't respond to pull up / pull down

     ===============================================================================
     */
    public static abstract class idConsole {

//	virtual			~idConsole( void ) {}
        public abstract void Init() throws idException;

        public abstract void Shutdown();

        // can't be combined with Init, because Init happens before renderer is started
        public abstract void LoadGraphics() throws idException;

        public abstract boolean ProcessEvent(final sysEvent_s event, boolean forceAccept) throws idException;

        // the system code can release the mouse pointer when the console is active
        public abstract boolean Active();

        // clear the timers on any recent prints that are displayed in the notify lines
        public abstract void ClearNotifyLines();

        // some console commands, like timeDemo, will force the console closed before they start
        public abstract void Close();

        public abstract void Draw(boolean forceFullScreen);

        public abstract void Print(final String text);
    };
    /**
     *
     *
     *
     */
    static final int LINE_WIDTH = 78;
    static final int NUM_CON_TIMES = 4;
    static final int CON_TEXTSIZE = 0x30000;
    static final int TOTAL_LINES = (CON_TEXTSIZE / LINE_WIDTH);
    static final int CONSOLE_FIRSTREPEAT = 200;
    static final int CONSOLE_REPEAT = 100;
    //                                     
    static final int COMMAND_HISTORY = 64;
    //    

    // the console will query the cvar and command systems for
    // command completion information
    static class idConsoleLocal extends idConsole {

        //
        //============================
        //
        private boolean keyCatching;
        //
        private final short[] text = new short[CON_TEXTSIZE];
        private int current;		// line where next message will be printed
        private int x;			// offset in current line for next print
        private int display;		// bottom of console displays this line
        private int lastKeyEvent;	// time of last key event for scroll delay
        private int nextKeyEvent;	// keyboard repeat rate
        //
        private float displayFrac;	// approaches finalFrac at scr_conspeed
        private float finalFrac;	// 0.0 to 1.0 lines of console to display
        private int fracTime;		// time of last displayFrac update
        //
        private int vislines;		// in scanlines
        //
        private final int[] times = new int[NUM_CON_TIMES];	// cls.realtime time the line was generated for transparent notify lines
        //
        private idVec4 color;
        //
        private final idEditField[] historyEditLines = new idEditField[COMMAND_HISTORY];
        //
        private int nextHistoryLine;    // the last line in the history buffer, not masked
        private int historyLine;	// the line being displayed from history buffer will be <= nextHistoryLine
        //
        private idEditField consoleField;
        //
        private static final idCVar con_speed = new idCVar("con_speed", "3", CVAR_SYSTEM, "speed at which the console moves up and down");
        private static final idCVar con_notifyTime = new idCVar("con_notifyTime", "3", CVAR_SYSTEM, "time messages are displayed onscreen when console is pulled up");
        private static final idCVar con_noPrint;
        //
        private idMaterial whiteShader;
        private idMaterial consoleShader;
        //
        //

        static {
            if (win_main.DEBUG) {
                con_noPrint = new idCVar("con_noPrint", "0", CVAR_BOOL | CVAR_SYSTEM | CVAR_NOCHEAT, "print on the console but not onscreen when console is pulled up");
            } else {
                con_noPrint = new idCVar("con_noPrint", "1", CVAR_BOOL | CVAR_SYSTEM | CVAR_NOCHEAT, "print on the console but not onscreen when console is pulled up");
            }
        }

        @Override
        public void Init() throws idException {
            int i;

            keyCatching = false;

            lastKeyEvent = -1;
            nextKeyEvent = CONSOLE_FIRSTREPEAT;

            consoleField = new idEditField();//.Clear();

            consoleField.SetWidthInChars(LINE_WIDTH);

            for (i = 0; i < COMMAND_HISTORY; i++) {
                historyEditLines[i] = new idEditField();//.Clear();
                historyEditLines[i].SetWidthInChars(LINE_WIDTH);
            }

            cmdSystem.AddCommand("clear", Con_Clear_f.getInstance(), CMD_FL_SYSTEM, "clears the console");
            cmdSystem.AddCommand("conDump", Con_Dump_f.getInstance(), CMD_FL_SYSTEM, "dumps the console text to a file");
        }

        @Override
        public void Shutdown() {
            cmdSystem.RemoveCommand("clear");
            cmdSystem.RemoveCommand("conDump");
        }

        /*
         ==============
         LoadGraphics

         Can't be combined with init, because init happens before
         the renderSystem is initialized
         ==============
         */
        @Override
        public void LoadGraphics() throws idException {
            charSetShader = declManager.FindMaterial("textures/bigchars");
            whiteShader = declManager.FindMaterial("_white");
            consoleShader = declManager.FindMaterial("console");
        }

        @Override
        public boolean ProcessEvent(sysEvent_s event, boolean forceAccept) throws idException {
            boolean consoleKey;
            consoleKey = event.evType == SE_KEY && (event.evValue == Sys_GetConsoleKey(false) || event.evValue == Sys_GetConsoleKey(true));

            if (ID_CONSOLE_LOCK) {
                // If the console's not already down, and we have it turned off, check for ctrl+alt
                if (!keyCatching && !com_allowConsole.GetBool()) {
                    if (!idKeyInput.IsDown(K_CTRL) || !idKeyInput.IsDown(K_ALT)) {
                        consoleKey = false;
                    }
                }
            }

            // we always catch the console key event
            if (!forceAccept && consoleKey) {
                // ignore up events
                if (event.evValue2 == 0) {
                    return true;
                }

                consoleField.ClearAutoComplete();

                // a down event will toggle the destination lines
                if (keyCatching) {
                    Close();
                    Sys_GrabMouseCursor(true);
                    cvarSystem.SetCVarBool("ui_chat", false);
                } else {
                    consoleField.Clear();
                    keyCatching = true;
                    if (idKeyInput.IsDown(K_SHIFT)) {
                        // if the shift key is down, don't open the console as much
                        SetDisplayFraction(0.2f);
                    } else {
                        SetDisplayFraction(0.5f);
                    }
                    cvarSystem.SetCVarBool("ui_chat", true);
                }
                return true;
            }

            // if we aren't key catching, dump all the other events
            if (!forceAccept && !keyCatching) {
                return false;
            }

            // handle key and character events
            if (event.evType == SE_CHAR) {
                // never send the console key as a character
                if (event.evValue != Sys_GetConsoleKey(false) && event.evValue != Sys_GetConsoleKey(true)) {
                    consoleField.CharEvent(event.evValue);
                }
                return true;
            }

            if (event.evType == SE_KEY) {
                // ignore up key events
                if (event.evValue2 == 0) {
                    return true;
                }

                KeyDownEvent(event.evValue);
                return true;
            }

            // we don't handle things like mouse, joystick, and network packets
            return false;
        }

        @Override
        public boolean Active() {
            return keyCatching;
        }

        @Override
        public void ClearNotifyLines() {
            int i;

            for (i = 0; i < NUM_CON_TIMES; i++) {
                times[i] = 0;
            }
        }

        @Override
        public void Close() {
            keyCatching = false;
            SetDisplayFraction(0);
            displayFrac = 0;	// don't scroll to that point, go immediately
            ClearNotifyLines();
        }

        /*
         ================
         Print

         Handles cursor positioning, line wrapping, etc
         ================
         */
        @Override
        public void Print(String txt) {
            int y;
            int c, l;
            int color;
            int txt_p = 0;

            if (ID_ALLOW_TOOLS) {
                RadiantPrint(txt);

                if ((com_editors & EDITOR_MATERIAL) != 0) {
                    MaterialEditorPrintConsole(txt);
                }
            }

            color = idStr.ColorIndex(C_COLOR_CYAN);

            while (txt_p < txt.length()
                    && (c = txt.charAt(txt_p)) != 0) {
                if (idStr.IsColor(txt.substring(txt_p))) {
                    final char colorChar = txt.charAt(txt_p + 1);
                    if (colorChar == C_COLOR_DEFAULT) {
                        color = idStr.ColorIndex(C_COLOR_CYAN);
                    } else {
                        color = idStr.ColorIndex(colorChar);
                    }
                    txt_p += 2;
                    continue;
                }

                y = current % TOTAL_LINES;

                // if we are about to print a new word, check to see
                // if we should wrap to the new line
                if (c > ' ' && (x == 0 || text[y * LINE_WIDTH + x - 1] <= ' ')) {
                    // count word length
                    for (l = 0; l < LINE_WIDTH && l < txt.length(); l++) {
                        if (txt.charAt(l) <= ' ') {
                            break;
                        }
                    }

                    // word wrap
                    if (l != LINE_WIDTH && (x + l >= LINE_WIDTH)) {
                        Linefeed();
                    }
                }

                txt_p++;

                switch (c) {
                    case '\n':
                        Linefeed();
                        break;
                    case '\t':
                        do {
                            text[y * LINE_WIDTH + x] = (short) ((color << 8) | ' ');
                            x++;
                            if (x >= LINE_WIDTH) {
                                Linefeed();
                                x = 0;
                            }
                        } while ((x & 3) != 0);
                        break;
                    case '\r':
                        x = 0;
                        break;
                    default:	// display character and advance
                        text[y * LINE_WIDTH + x] = (short) ((color << 8) | c);
                        x++;
                        if (x >= LINE_WIDTH) {
                            Linefeed();
                            x = 0;
                        }
                        break;
                }
            }

            // mark time for transparent overlay
            if (current >= 0) {
                times[current % NUM_CON_TIMES] = com_frameTime;
            }
        }

        /*
         ==============
         Draw

         ForceFullScreen is used by the editor
         ==============
         */
        @Override
        public void Draw(boolean forceFullScreen) {
            float y = 0.0f;

            if (NOT(charSetShader)) {
                return;
            }

            if (forceFullScreen) {
                // if we are forced full screen because of a disconnect, 
                // we want the console closed when we go back to a session state
                Close();
                // we are however catching keyboard input
                keyCatching = true;
            }

            Scroll();

            UpdateDisplayFraction();

            if (forceFullScreen) {
                DrawSolidConsole(1.0f);
            } else if (displayFrac != 0.0f) {
                DrawSolidConsole(displayFrac);
            } else {
                // only draw the notify lines if the developer cvar is set,
                // or we are a debug build
                if (!con_noPrint.GetBool()) {
                    DrawNotify();
                }
            }

            if (com_showFPS.GetBool()) {
                y = SCR_DrawFPS(0);
            }

            if (com_showMemoryUsage.GetBool()) {
                y = SCR_DrawMemoryUsage(y);
            }

            if (com_showAsyncStats.GetBool()) {
                y = SCR_DrawAsyncStats(y);
            }

            if (com_showSoundDecoders.GetBool()) {
                y = SCR_DrawSoundDecoders(y);
            }
        }

        /*
         ================
         idConsoleLocal.Dump

         Save the console contents out to a file
         ================
         */
        public void Dump(final String fileName) throws idException {
            int l, x, i;
            int line;
            idFile f;
            char[] buffer = new char[LINE_WIDTH + 3];

            f = fileSystem.OpenFileWrite(fileName);
            if (null == f) {
                common.Warning("couldn't open %s", fileName);
                return;
            }

            // skip empty lines
            l = current - TOTAL_LINES + 1;
            if (l < 0) {
                l = 0;
            }
            for (; l <= current; l++) {
                line = (l % TOTAL_LINES) * LINE_WIDTH;
                for (x = 0; x < LINE_WIDTH; x++) {
                    if ((text[line + x] & 0xff) > ' ') {
                        break;
                    }
                }
                if (x != LINE_WIDTH) {
                    break;
                }
            }

            // write the remaining lines
            for (; l <= current; l++) {
                line = (l % TOTAL_LINES) * LINE_WIDTH;
                for (i = 0; i < LINE_WIDTH; i++) {
                    buffer[i] = (char) (text[line + i] & 0xff);
                }
                for (x = LINE_WIDTH - 1; x >= 0; x--) {
                    if (buffer[x] <= ' ') {
                        buffer[x] = 0;
                    } else {
                        break;
                    }
                }
                buffer[x + 1] = '\r';
                buffer[x + 2] = '\n';
                buffer[x + 3] = 0;
                f.WriteString(buffer);
            }

            fileSystem.CloseFile(f);
        }

        public void Clear() {
            int i;

            for (i = 0; i < CON_TEXTSIZE; i++) {
                text[i] = (short) ((idStr.ColorIndex(C_COLOR_CYAN) << 8) | ' ');
            }

            Bottom();		// go to end
        }
        //============================
        public idMaterial charSetShader;
//
//

        /*
         ====================
         KeyDownEvent

         Handles history and console scrollback
         ====================
         */
        private void KeyDownEvent(int key) throws idException {

            // Execute F key bindings
            if (key >= K_F1 && key <= K_F12) {
                idKeyInput.ExecKeyBinding(key);
                return;
            }

            // ctrl-L clears screen
            if (key == 'l' && idKeyInput.IsDown(K_CTRL)) {
                Clear();
                return;
            }

            // enter finishes the line
            if (key == K_ENTER || key == K_KP_ENTER) {

                common.Printf("]%s\n", consoleField.GetBuffer());

                cmdSystem.BufferCommandText(CMD_EXEC_APPEND, ctos(consoleField.GetBuffer()));	// valid command
                cmdSystem.BufferCommandText(CMD_EXEC_APPEND, "\n");

                // copy line to history buffer
                historyEditLines[nextHistoryLine % COMMAND_HISTORY] = consoleField;
                nextHistoryLine++;
                historyLine = nextHistoryLine;

                consoleField.Clear();
                consoleField.SetWidthInChars(LINE_WIDTH);

                session.UpdateScreen();// force an update, because the command
                // may take some time
                return;
            }

            // command completion
            if (key == K_TAB) {
                consoleField.AutoComplete();
                return;
            }

            // command history (ctrl-p ctrl-n for unix style)
            if ((key == K_UPARROW)
                    || ((Character.toLowerCase(key) == 'p') && idKeyInput.IsDown(K_CTRL))) {
                if (nextHistoryLine - historyLine < COMMAND_HISTORY && historyLine > 0) {
                    historyLine--;
                }
                consoleField = historyEditLines[historyLine % COMMAND_HISTORY];
                return;
            }

            if ((key == K_DOWNARROW)
                    || ((Character.toLowerCase(key) == 'n') && idKeyInput.IsDown(K_CTRL))) {
                if (historyLine == nextHistoryLine) {
                    return;
                }
                historyLine++;
                consoleField = historyEditLines[historyLine % COMMAND_HISTORY];
                return;
            }

            // console scrolling
            if (key == K_PGUP) {
                PageUp();
                lastKeyEvent = eventLoop.Milliseconds();
                nextKeyEvent = CONSOLE_FIRSTREPEAT;
                return;
            }

            if (key == K_PGDN) {
                PageDown();
                lastKeyEvent = eventLoop.Milliseconds();
                nextKeyEvent = CONSOLE_FIRSTREPEAT;
                return;
            }

            if (key == K_MWHEELUP) {
                PageUp();
                return;
            }

            if (key == K_MWHEELDOWN) {
                PageDown();
                return;
            }

            // ctrl-home = top of console
            if (key == K_HOME && idKeyInput.IsDown(K_CTRL)) {
                Top();
                return;
            }

            // ctrl-end = bottom of console
            if (key == K_END && idKeyInput.IsDown(K_CTRL)) {
                Bottom();
                return;
            }

            // pass to the normal editline routine
            consoleField.KeyDownEvent(key);
        }

//
        private void Linefeed() {
            int i;

            // mark time for transparent overlay
            if (current >= 0) {
                times[current % NUM_CON_TIMES] = com_frameTime;
            }

            x = 0;
            if (display == current) {
                display++;
            }
            current++;
            for (i = 0; i < LINE_WIDTH; i++) {
                text[(current % TOTAL_LINES) * LINE_WIDTH + i] = (short) ((idStr.ColorIndex(C_COLOR_CYAN) << 8) | ' ');
            }
        }
//

        private void PageUp() {
            display -= 2;
            if (current - display >= TOTAL_LINES) {
                display = current - TOTAL_LINES + 1;
            }
        }

        private void PageDown() {
            display += 2;
            if (display > current) {
                display = current;
            }
        }

        private void Top() {
            display = 0;
        }

        private void Bottom() {
            display = current;
        }
//

        /*
         ================
         DrawInput

         Draw the editline after a ] prompt
         ================
         */
        private void DrawInput() {
            int y, autoCompleteLength;

            y = vislines - (SMALLCHAR_HEIGHT * 2);

            if (consoleField.GetAutoCompleteLength() != 0) {
                autoCompleteLength = strLen(consoleField.GetBuffer()) - consoleField.GetAutoCompleteLength();

                if (autoCompleteLength > 0) {
                    renderSystem.SetColor4(.8f, .2f, .2f, .45f);

                    renderSystem.DrawStretchPic(2 * SMALLCHAR_WIDTH + consoleField.GetAutoCompleteLength() * SMALLCHAR_WIDTH,
                            y + 2, autoCompleteLength * SMALLCHAR_WIDTH, SMALLCHAR_HEIGHT - 2, 0, 0, 0, 0, whiteShader);

                }
            }

            renderSystem.SetColor(idStr.ColorForIndex(C_COLOR_CYAN));

            renderSystem.DrawSmallChar(1 * SMALLCHAR_WIDTH, y, ']', localConsole.charSetShader);

            consoleField.Draw(2 * SMALLCHAR_WIDTH, y, SCREEN_WIDTH - 3 * SMALLCHAR_WIDTH, true, charSetShader);
        }

        /*
         ================
         DrawNotify

         Draws the last few lines of output transparently over the game top
         ================
         */static int drawNotifyTotal = 0;
        private void DrawNotify() {
            int x, v;
            int text_p;
            int i;
            int time;
            int currentColor;
            drawNotifyTotal++;

            if (con_noPrint.GetBool()) {
                return;
            }

            currentColor = idStr.ColorIndex(C_COLOR_WHITE);
            renderSystem.SetColor(idStr.ColorForIndex(currentColor));

            v = 0;
            for (i = current - NUM_CON_TIMES + 1; i <= current; i++) {
                if (i < 0) {
                    continue;
                }
                time = times[i % NUM_CON_TIMES];
                if (time == 0) {
                    continue;
                }
                time = com_frameTime - time;
                if (time > con_notifyTime.GetFloat() * 1000) {
                    continue;
                }
                text_p = (i % TOTAL_LINES) * LINE_WIDTH;
//		text_p = text + (i % TOTAL_LINES)*LINE_WIDTH;

                for (x = 0; x < LINE_WIDTH; x++) {
                    if ((text[text_p + x] & 0xff) == ' ') {
                        continue;
                    }
                    if (idStr.ColorIndex(text[text_p + x] >> 8) != currentColor) {
                        currentColor = idStr.ColorIndex(text[text_p + x] >> 8);
                        renderSystem.SetColor(idStr.ColorForIndex(currentColor));
                    }
                    renderSystem.DrawSmallChar((x + 1) * SMALLCHAR_WIDTH, v, text[text_p + x] & 0xff, localConsole.charSetShader);
                }

                v += SMALLCHAR_HEIGHT;
            }

            renderSystem.SetColor(colorCyan);
        }

        /*
         ================
         DrawSolidConsole

         Draws the console with the solid background
         ================
         */
        private void DrawSolidConsole(float frac) {
            int i, x;
            float y;
            int rows;
            int text_p;
            int row;
            int lines;
            int currentColor;

            lines = idMath.FtoiFast(SCREEN_HEIGHT * frac);
            if (lines <= 0) {
                return;
            }

            if (lines > SCREEN_HEIGHT) {
                lines = SCREEN_HEIGHT;
            }

            // draw the background
            y = frac * SCREEN_HEIGHT - 2;
            if (y < 1.0f) {
                y = 0.0f;
            } else {
                renderSystem.DrawStretchPic(0, 0, SCREEN_WIDTH, y, 0, 1.0f - displayFrac, 1, 1, consoleShader);
            }

            renderSystem.SetColor(colorCyan);
            renderSystem.DrawStretchPic(0, y, SCREEN_WIDTH, 2, 0, 0, 0, 0, whiteShader);
            renderSystem.SetColor(colorWhite);

            // draw the version number
            renderSystem.SetColor(idStr.ColorForIndex(C_COLOR_CYAN));

            char[] version = va("%s.%d", ENGINE_VERSION, BUILD_NUMBER).toCharArray();
            i = version.length;

            for (x = 0; x < i; x++) {
                renderSystem.DrawSmallChar(SCREEN_WIDTH - (i - x) * SMALLCHAR_WIDTH,
                        (lines - (SMALLCHAR_HEIGHT + SMALLCHAR_HEIGHT / 2)), version[x], localConsole.charSetShader);

            }

            // draw the text
            vislines = lines;
            rows = (lines - SMALLCHAR_WIDTH) / SMALLCHAR_WIDTH;		// rows of text to draw

            y = lines - (SMALLCHAR_HEIGHT * 3);

            // draw from the bottom up
            if (display != current) {
                // draw arrows to show the buffer is backscrolled
                renderSystem.SetColor(idStr.ColorForIndex(C_COLOR_CYAN));
                for (x = 0; x < LINE_WIDTH; x += 4) {
                    renderSystem.DrawSmallChar((x + 1) * SMALLCHAR_WIDTH, idMath.FtoiFast(y), '^', localConsole.charSetShader);
                }
                y -= SMALLCHAR_HEIGHT;
                rows--;
            }

            row = display;

            if (x == 0) {
                row--;
            }

            currentColor = idStr.ColorIndex(C_COLOR_WHITE);
            renderSystem.SetColor(idStr.ColorForIndex(currentColor));

            for (i = 0; i < rows; i++, y -= SMALLCHAR_HEIGHT, row--) {
                if (row < 0) {
                    break;
                }
                if (current - row >= TOTAL_LINES) {
                    // past scrollback wrap point
                    continue;
                }

                text_p = (row % TOTAL_LINES) * LINE_WIDTH;

                for (x = 0; x < LINE_WIDTH; x++) {
                    if ((text[text_p + x] & 0xff) == ' ') {
                        continue;
                    }

                    if (idStr.ColorIndex(text[text_p + x] >> 8) != currentColor) {
                        currentColor = idStr.ColorIndex(text[text_p + x] >> 8);
                        renderSystem.SetColor(idStr.ColorForIndex(currentColor));
                    }
                    renderSystem.DrawSmallChar((x + 1) * SMALLCHAR_WIDTH, idMath.FtoiFast(y), text[text_p + x] & 0xff, localConsole.charSetShader);
                }
            }

            // draw the input prompt, user text, and cursor if desired
            DrawInput();

            renderSystem.SetColor(colorCyan);
        }
//

        /*
         ==============
         Scroll
         deals with scrolling text because we don't have key repeat
         ==============
         */
        private void Scroll() {
            if (lastKeyEvent == -1 || (lastKeyEvent + 200) > eventLoop.Milliseconds()) {
                return;
            }
            // console scrolling
            if (idKeyInput.IsDown(K_PGUP)) {
                PageUp();
                nextKeyEvent = CONSOLE_REPEAT;
                return;
            }

            if (idKeyInput.IsDown(K_PGDN)) {
                PageDown();
                nextKeyEvent = CONSOLE_REPEAT;
//                return;
            }
        }

        /*
         ==============
         SetDisplayFraction

         Causes the console to start opening the desired amount.
         ==============
         */
        private void SetDisplayFraction(float frac) {
            finalFrac = frac;
            fracTime = com_frameTime;
        }


        /*
         ==============
         UpdateDisplayFraction

         Scrolls the console up or down based on conspeed
         ==============
         */
        private void UpdateDisplayFraction() {
            if (con_speed.GetFloat() <= 0.1f) {
                fracTime = com_frameTime;
                displayFrac = finalFrac;
                return;
            }

            // scroll towards the destination height
            if (finalFrac < displayFrac) {
                displayFrac -= con_speed.GetFloat() * (com_frameTime - fracTime) * 0.001f;
                if (finalFrac > displayFrac) {
                    displayFrac = finalFrac;
                }
                fracTime = com_frameTime;
            } else if (finalFrac > displayFrac) {
                displayFrac += con_speed.GetFloat() * (com_frameTime - fracTime) * 0.001f;
                if (finalFrac < displayFrac) {
                    displayFrac = finalFrac;
                }
                fracTime = com_frameTime;
            }
        }

    };


    /*
     =============================================================================

     Misc stats

     =============================================================================
     */

    /*
     ==================
     SCR_DrawTextLeftAlign
     ==================
     */
    static void SCR_DrawTextLeftAlign(float[] y, final String fmt, Object... text) {
        String[] string = {null};//new char[MAX_STRING_CHARS];
//	va_list argptr;
//	va_start( argptr, text );
        idStr.vsnPrintf(string, MAX_STRING_CHARS, fmt, text);
//	va_end( argptr );
        renderSystem.DrawSmallStringExt(0, (int) (y[0] + 2), string[0].toCharArray(), colorWhite, true, localConsole.charSetShader);
        y[0] += SMALLCHAR_HEIGHT + 4;
    }

    /*
     ==================
     SCR_DrawTextRightAlign
     ==================
     */
    static void SCR_DrawTextRightAlign(float[] y, final String fmt, Object... text) {
        String[] string = {null};//new char[MAX_STRING_CHARS];
//	va_list argptr;
//	va_start( argptr, text );
        int i = idStr.vsnPrintf(string, MAX_STRING_CHARS, fmt, text);
//	va_end( argptr );
        renderSystem.DrawSmallStringExt(635 - i * SMALLCHAR_WIDTH, (int) (y[0] + 2), string[0].toCharArray(), colorWhite, true, localConsole.charSetShader);
        y[0] += SMALLCHAR_HEIGHT + 4;
    }
    /*
     ==================
     SCR_DrawFPS
     ==================
     */
    static final int FPS_FRAMES = 4;
    static int[] previousTimes = new int[FPS_FRAMES];
    static int index;
    static int previous;

    static float SCR_DrawFPS(float y) {
        String s;
        int w;
        int i, total;
        int fps;
        int t, frameTime;

        // don't use serverTime, because that will be drifting to
        // correct for internet lag changes, timescales, timedemos, etc
        t = Sys_Milliseconds();
        frameTime = t - previous;
        previous = t;

        previousTimes[index % FPS_FRAMES] = frameTime;
        index++;
        if (index > FPS_FRAMES) {
            // average multiple frames together to smooth changes out a bit
            total = 0;
            for (i = 0; i < FPS_FRAMES; i++) {
                total += previousTimes[i];
            }
            if (0 == total) {
                total = 1;
            }
            fps = 10000 * FPS_FRAMES / total;
            fps = (fps + 5) / 10;

            s = va("%dfps", fps);
            w = s.length() * BIGCHAR_WIDTH;

            renderSystem.DrawBigStringExt(635 - w, idMath.FtoiFast(y) + 2, s, colorWhite, true, localConsole.charSetShader);
        }

        return y + BIGCHAR_HEIGHT + 4;
    }

    /*
     ==================
     SCR_DrawMemoryUsage
     ==================
     */
    static float SCR_DrawMemoryUsage(float y) {
        memoryStats_t[] allocs = new memoryStats_t[1], frees = new memoryStats_t[1];
        float[] yy = {y};

//        Mem_GetStats(allocs);
//        SCR_DrawTextRightAlign(yy, "total allocated memory: %4d, %4dkB", allocs[0].num, allocs[0].totalSize >> 10);
//
//        Mem_GetFrameStats(allocs, frees);
//        SCR_DrawTextRightAlign(yy, "frame alloc: %4d, %4dkB  frame free: %4d, %4dkB", allocs[0].num, allocs[0].totalSize >> 10, frees[0].num, frees[0].totalSize >> 10);
//
//        Mem_ClearFrameStats();
        return yy[0];
    }

    /*
     ==================
     SCR_DrawAsyncStats
     ==================
     */
    static float SCR_DrawAsyncStats(float y) {
        int i, outgoingRate, incomingRate;
        float outgoingCompression, incomingCompression;
        float[] yy = {y};

        if (idAsyncNetwork.server.IsActive()) {

            SCR_DrawTextRightAlign(yy, "server delay = %d msec", idAsyncNetwork.server.GetDelay());
            SCR_DrawTextRightAlign(yy, "total outgoing rate = %d KB/s", idAsyncNetwork.server.GetOutgoingRate() >> 10);
            SCR_DrawTextRightAlign(yy, "total incoming rate = %d KB/s", idAsyncNetwork.server.GetIncomingRate() >> 10);

            for (i = 0; i < MAX_ASYNC_CLIENTS; i++) {

                outgoingRate = idAsyncNetwork.server.GetClientOutgoingRate(i);
                incomingRate = idAsyncNetwork.server.GetClientIncomingRate(i);
                outgoingCompression = idAsyncNetwork.server.GetClientOutgoingCompression(i);
                incomingCompression = idAsyncNetwork.server.GetClientIncomingCompression(i);

                if (outgoingRate != -1 && incomingRate != -1) {
                    SCR_DrawTextRightAlign(yy, "client %d: out rate = %d B/s (% -2.1f%%), in rate = %d B/s (% -2.1f%%)", i, outgoingRate, outgoingCompression, incomingRate, incomingCompression);
                }
            }

            idStr msg = new idStr();
            idAsyncNetwork.server.GetAsyncStatsAvgMsg(msg);
            SCR_DrawTextRightAlign(yy, msg.toString());

        } else if (idAsyncNetwork.client.IsActive()) {

            outgoingRate = idAsyncNetwork.client.GetOutgoingRate();
            incomingRate = idAsyncNetwork.client.GetIncomingRate();
            outgoingCompression = idAsyncNetwork.client.GetOutgoingCompression();
            incomingCompression = idAsyncNetwork.client.GetIncomingCompression();

            if (outgoingRate != -1 && incomingRate != -1) {
                SCR_DrawTextRightAlign(yy, "out rate = %d B/s (% -2.1f%%), in rate = %d B/s (% -2.1f%%)", outgoingRate, outgoingCompression, incomingRate, incomingCompression);
            }

            SCR_DrawTextRightAlign(yy, "packet loss = %d%%, client prediction = %d", (int) idAsyncNetwork.client.GetIncomingPacketLoss(), idAsyncNetwork.client.GetPrediction());

            SCR_DrawTextRightAlign(yy, "predicted frames: %d", idAsyncNetwork.client.GetPredictedFrames());

        }

        return yy[0];
    }

    /*
     ==================
     SCR_DrawSoundDecoders
     ==================
     */
    static float SCR_DrawSoundDecoders(float y) {
        int index, numActiveDecoders;
        soundDecoderInfo_t decoderInfo = new soundDecoderInfo_t();
        float[] yy = {y};

        index = -1;
        numActiveDecoders = 0;
        while ((index = soundSystem.GetSoundDecoderInfo(index, decoderInfo)) != -1) {
            int localTime = decoderInfo.current44kHzTime - decoderInfo.start44kHzTime;
            int sampleTime = decoderInfo.num44kHzSamples / decoderInfo.numChannels;
            int percent;
            if (localTime > sampleTime) {
                if (decoderInfo.looping) {
                    percent = (localTime % sampleTime) * 100 / sampleTime;
                } else {
                    percent = 100;
                }
            } else {
                percent = localTime * 100 / sampleTime;
            }
            SCR_DrawTextLeftAlign(yy, "%3d: %3d%% (%1.2f) %s: %s (%dkB)", numActiveDecoders, percent, decoderInfo.lastVolume, decoderInfo.format.toString(), decoderInfo.name.toString(), decoderInfo.numBytes >> 10);
            numActiveDecoders++;
        }
        return yy[0];
    }
//=========================================================================

    /*
     ==============
     Con_Clear_f
     ==============
     */
    private static class Con_Clear_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new Con_Clear_f();

        private Con_Clear_f() {
        }

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) {
            localConsole.Clear();
        }
    }

    /*
     ==============
     Con_Dump_f
     ==============
     */
    private static class Con_Dump_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new Con_Dump_f();

        private Con_Dump_f() {
        }

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) throws idException {
            if (args.Argc() != 2) {
                common.Printf("usage: conDump <filename>\n");
                return;
            }

            String fileName = new idStr(args.Argv(1)).DefaultFileExtension(".txt").toString();

            common.Printf("Dumped console text to %s.\n", fileName);

            localConsole.Dump(fileName);
        }
    }
}
