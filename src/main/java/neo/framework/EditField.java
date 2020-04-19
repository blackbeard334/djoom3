package neo.framework;

import static neo.Renderer.RenderSystem.SMALLCHAR_WIDTH;
import static neo.Renderer.RenderSystem.renderSystem;
import static neo.TempDump.ctos;
import static neo.TempDump.strLen;
import static neo.framework.CVarSystem.cvarSystem;
import static neo.framework.CmdSystem.cmdSystem;
import static neo.framework.Common.com_ticNumber;
import static neo.framework.Common.common;
import static neo.framework.KeyInput.K_ALT;
import static neo.framework.KeyInput.K_BACKSPACE;
import static neo.framework.KeyInput.K_CAPSLOCK;
import static neo.framework.KeyInput.K_CTRL;
import static neo.framework.KeyInput.K_DEL;
import static neo.framework.KeyInput.K_END;
import static neo.framework.KeyInput.K_HOME;
import static neo.framework.KeyInput.K_INS;
import static neo.framework.KeyInput.K_KP_INS;
import static neo.framework.KeyInput.K_LEFTARROW;
import static neo.framework.KeyInput.K_RIGHTARROW;
import static neo.framework.KeyInput.K_SHIFT;
import static neo.idlib.Lib.colorWhite;
import static neo.idlib.Text.Str.S_COLOR_WHITE;
import static neo.sys.win_main.Sys_GetClipboardData;

import neo.TempDump.void_callback;
import neo.Renderer.Material.idMaterial;
import neo.framework.KeyInput.idKeyInput;
import neo.idlib.CmdArgs.idCmdArgs;
import neo.idlib.Lib.idException;
import neo.idlib.Text.Str.idStr;
import neo.open.Nio;

/**
 *
 */
public class EditField {
    /*
     ===============================================================================

     Edit field

     ===============================================================================
     */

    static final int MAX_EDIT_LINE = 256;
    static autoComplete_s globalAutoComplete;

    static class autoComplete_s {

        boolean valid;
        int length;
        char[] completionString = new char[MAX_EDIT_LINE];
        char[] currentMatch = new char[MAX_EDIT_LINE];
        int matchCount;
        int matchIndex;
        int findMatchIndex;
    } /*autoComplete_t*/;

    public static class idEditField {

        private int cursor;
        private int scroll;
        private int widthInChars;
        private final char[] buffer = new char[MAX_EDIT_LINE];
        private autoComplete_s autoComplete;
        //
        //

        public idEditField() {
            this.widthInChars = 0;
            this.autoComplete = new autoComplete_s();
            Clear();
        }
//public					~idEditField();

        public void Clear() {
            this.buffer[0] = 0;
            this.cursor = 0;
            this.scroll = 0;
            this.autoComplete.length = 0;
            this.autoComplete.valid = false;
        }

        public void SetWidthInChars(int w) {
            assert (w <= MAX_EDIT_LINE);
            this.widthInChars = w;
        }

        public void SetCursor(int c) {
            assert (c <= MAX_EDIT_LINE);
            this.cursor = c;
        }

        public int GetCursor() {
            return this.cursor;
        }

        public void ClearAutoComplete() {
            if ((this.autoComplete.length > 0) && (this.autoComplete.length <= ctos(this.buffer).length())) {
                this.buffer[this.autoComplete.length] = '\0';
                if (this.cursor > this.autoComplete.length) {
                    this.cursor = this.autoComplete.length;
                }
            }
            this.autoComplete.length = 0;
            this.autoComplete.valid = false;
        }

        public int GetAutoCompleteLength() {
            return this.autoComplete.length;
        }

        public void AutoComplete() throws idException {
            final char[] completionArgString = new char[MAX_EDIT_LINE];
            final idCmdArgs args = new idCmdArgs();
            final void_callback findMatches = FindMatches.getInstance();
            final void_callback findIndexMatch = FindIndexMatch.getInstance();
            final void_callback printMatches = PrintMatches.getInstance();

            if (!this.autoComplete.valid) {
                args.TokenizeString(ctos(this.buffer), false);
                idStr.Copynz(this.autoComplete.completionString, args.Argv(0), this.autoComplete.completionString.length);
                idStr.Copynz(completionArgString, args.Args(), completionArgString.length);
                this.autoComplete.matchCount = 0;
                this.autoComplete.matchIndex = 0;
                this.autoComplete.currentMatch[0] = 0;

                if (strLen(this.autoComplete.completionString) == 0) {
                    return;
                }

                globalAutoComplete = this.autoComplete;

                cmdSystem.CommandCompletion(findMatches);
                cvarSystem.CommandCompletion(findMatches);

                this.autoComplete = globalAutoComplete;

                if (this.autoComplete.matchCount == 0) {
                    return;	// no matches
                }

                // when there's only one match or there's an argument
                if ((this.autoComplete.matchCount == 1) || (completionArgString[0] != '\0')) {

                    /// try completing arguments
                    idStr.Append(this.autoComplete.completionString, this.autoComplete.completionString.length, " ");
                    idStr.Append(this.autoComplete.completionString, this.autoComplete.completionString.length, ctos(completionArgString));
                    this.autoComplete.matchCount = 0;

                    globalAutoComplete = this.autoComplete;

                    cmdSystem.ArgCompletion(ctos(this.autoComplete.completionString), findMatches);
                    cvarSystem.ArgCompletion(ctos(this.autoComplete.completionString), findMatches);

                    this.autoComplete = globalAutoComplete;

                    idStr.snPrintf(this.buffer, this.buffer.length, "%s", this.autoComplete.currentMatch);

                    if (this.autoComplete.matchCount == 0) {
                        // no argument matches
                        idStr.Append(this.buffer, this.buffer.length, " ");
                        idStr.Append(this.buffer, this.buffer.length, ctos(completionArgString));
                        SetCursor(strLen(this.buffer));
                        return;
                    }
                } else {

                    // multiple matches, complete to shortest
                    idStr.snPrintf(this.buffer, this.buffer.length, "%s", ctos(this.autoComplete.currentMatch));
                    if (strLen(completionArgString) != 0) {
                        idStr.Append(this.buffer, this.buffer.length, " ");
                        idStr.Append(this.buffer, this.buffer.length, ctos(completionArgString));
                    }
                }

                this.autoComplete.length = strLen(this.buffer);
                this.autoComplete.valid = (this.autoComplete.matchCount != 1);
                SetCursor(this.autoComplete.length);

                common.Printf("]%s\n", ctos(this.buffer));

                // run through again, printing matches
                globalAutoComplete = this.autoComplete;

                cmdSystem.CommandCompletion(printMatches);
                cmdSystem.ArgCompletion(ctos(this.autoComplete.completionString), printMatches);
                cvarSystem.CommandCompletion(PrintCvarMatches.getInstance());
                cmdSystem.ArgCompletion(ctos(this.autoComplete.completionString), printMatches);

            } else if (this.autoComplete.matchCount != 1) {

                // get the next match and show instead
                this.autoComplete.matchIndex++;
                if (this.autoComplete.matchIndex == this.autoComplete.matchCount) {
                    this.autoComplete.matchIndex = 0;
                }
                this.autoComplete.findMatchIndex = 0;

                globalAutoComplete = this.autoComplete;

                cmdSystem.CommandCompletion(findIndexMatch);
                cmdSystem.ArgCompletion(ctos(this.autoComplete.completionString), findIndexMatch);
                cvarSystem.CommandCompletion(findIndexMatch);
                cmdSystem.ArgCompletion(ctos(this.autoComplete.completionString), findIndexMatch);

                this.autoComplete = globalAutoComplete;

                // and print it
                idStr.snPrintf(this.buffer, this.buffer.length, ctos(this.autoComplete.currentMatch));
                if (this.autoComplete.length > strLen(this.buffer)) {
                    this.autoComplete.length = strLen(this.buffer);
                }
                SetCursor(this.autoComplete.length);
            }
        }

        public void CharEvent(int ch) {
            int len;

            if (ch == (('v' - 'a') + 1)) {	// ctrl-v is paste
                Paste();
                return;
            }

            if (ch == (('c' - 'a') + 1)) {	// ctrl-c clears the field
                Clear();
                return;
            }

            len = strLen(this.buffer);

            if ((ch == (('h' - 'a') + 1)) || (ch == K_BACKSPACE)) {	// ctrl-h is backspace
                if (this.cursor > 0) {
//			memmove( buffer + cursor - 1, buffer + cursor, len + 1 - cursor );
                    Nio.arraycopy(this.buffer, this.cursor, this.buffer, this.cursor - 1, (len + 1) - this.cursor);
                    this.cursor--;
                    if (this.cursor < this.scroll) {
                        this.scroll--;
                    }
                }
                return;
            }

            if (ch == (('a' - 'a') + 1)) {	// ctrl-a is home
                this.cursor = 0;
                this.scroll = 0;
                return;
            }

            if (ch == (('e' - 'a') + 1)) {	// ctrl-e is end
                this.cursor = len;
                this.scroll = this.cursor - this.widthInChars;
                return;
            }

            //
            // ignore any other non printable chars
            //
            if ((ch < 32) || (ch > 125)) {
                return;
            }

            if (idKeyInput.GetOverstrikeMode()) {
                if (this.cursor == (MAX_EDIT_LINE - 1)) {
                    return;
                }
                this.buffer[this.cursor] = (char) ch;
                this.cursor++;
            } else {	// insert mode
                if (len == (MAX_EDIT_LINE - 1)) {
                    return; // all full
                }
//		memmove( buffer + cursor + 1, buffer + cursor, len + 1 - cursor );
                Nio.arraycopy(this.buffer, this.cursor, this.buffer, this.cursor + 1, (len + 1) - this.cursor);
                this.buffer[this.cursor] = (char) ch;
                this.cursor++;
            }

            if (this.cursor >= this.widthInChars) {
                this.scroll++;
            }

            if (this.cursor == (len + 1)) {
                this.buffer[this.cursor] = 0;
            }
        }

        public void KeyDownEvent(int key) {
            int len;

            // shift-insert is paste
            if (((key == K_INS) || (key == K_KP_INS)) && idKeyInput.IsDown(K_SHIFT)) {
                ClearAutoComplete();
                Paste();
                return;
            }

            len = strLen(this.buffer);

            if (key == K_DEL) {
                if (this.autoComplete.length != 0) {
                    ClearAutoComplete();
                } else if (this.cursor < len) {
//			memmove( buffer + cursor, buffer + cursor + 1, len - cursor );
                    Nio.arraycopy(this.buffer, this.cursor + 1, this.buffer, this.cursor, len - this.cursor);
                }
                return;
            }

            if (key == K_RIGHTARROW) {
                if (idKeyInput.IsDown(K_CTRL)) {
                    // skip to next word
                    while ((this.cursor < len) && (this.buffer[this.cursor] != ' ')) {
                        this.cursor++;
                    }

                    while ((this.cursor < len) && (this.buffer[this.cursor] == ' ')) {
                        this.cursor++;
                    }
                } else {
                    this.cursor++;
                }

                if (this.cursor > len) {
                    this.cursor = len;
                }

                if (this.cursor >= (this.scroll + this.widthInChars)) {
                    this.scroll = (this.cursor - this.widthInChars) + 1;
                }

                if (this.autoComplete.length > 0) {
                    this.autoComplete.length = this.cursor;
                }
                return;
            }

            if (key == K_LEFTARROW) {
                if (idKeyInput.IsDown(K_CTRL)) {
                    // skip to previous word
                    while ((this.cursor > 0) && (this.buffer[this.cursor - 1] == ' ')) {
                        this.cursor--;
                    }

                    while ((this.cursor > 0) && (this.buffer[this.cursor - 1] != ' ')) {
                        this.cursor--;
                    }
                } else {
                    this.cursor--;
                }

                if (this.cursor < 0) {
                    this.cursor = 0;
                }
                if (this.cursor < this.scroll) {
                    this.scroll = this.cursor;
                }

                if (this.autoComplete.length != 0) {
                    this.autoComplete.length = this.cursor;
                }
                return;
            }

            if ((key == K_HOME) || ((Character.toLowerCase(key) == 'a') && idKeyInput.IsDown(K_CTRL))) {
                this.cursor = 0;
                this.scroll = 0;
                if (this.autoComplete.length != 0) {
                    this.autoComplete.length = this.cursor;
                    this.autoComplete.valid = false;
                }
                return;
            }

            if ((key == K_END) || ((Character.toLowerCase(key) == 'e') && idKeyInput.IsDown(K_CTRL))) {
                this.cursor = len;
                if (this.cursor >= (this.scroll + this.widthInChars)) {
                    this.scroll = (this.cursor - this.widthInChars) + 1;
                }
                if (this.autoComplete.length != 0) {
                    this.autoComplete.length = this.cursor;
                    this.autoComplete.valid = false;
                }
                return;
            }

            if (key == K_INS) {
                idKeyInput.SetOverstrikeMode(!idKeyInput.GetOverstrikeMode());
                return;
            }

            // clear autocompletion buffer on normal key input
            if ((key != K_CAPSLOCK) && (key != K_ALT) && (key != K_CTRL) && (key != K_SHIFT)) {
                ClearAutoComplete();
            }
        }

        public void Paste() {
            String cbd;
            int pasteLen, i;

            cbd = Sys_GetClipboardData();

            if (null == cbd) {
                return;
            }

            // send as if typed, so insert / overstrike works properly
            pasteLen = cbd.length();
            for (i = 0; i < pasteLen; i++) {
                CharEvent(cbd.charAt(i));
            }

//            Heap.Mem_Free(cbd);
        }

        public char[] GetBuffer() {
            return this.buffer;
        }

        public void Draw(int x, int y, int width, boolean showCursor, final idMaterial shader) throws idException {
            int len;
            int drawLen;
            int prestep;
            int cursorChar;
            final char[] str = new char[MAX_EDIT_LINE];
            int size;

            size = SMALLCHAR_WIDTH;

            drawLen = this.widthInChars;
            len = strLen(this.buffer) + 1;

            // guarantee that cursor will be visible
            if (len <= drawLen) {
                prestep = 0;
            } else {
                if ((this.scroll + drawLen) > len) {
                    this.scroll = len - drawLen;
                    if (this.scroll < 0) {
                        this.scroll = 0;
                    }
                }
                prestep = this.scroll;

                // Skip color code
                if (idStr.IsColor(ctos(this.buffer).substring(prestep))) {
                    prestep += 2;
                }
                if ((prestep > 0) && idStr.IsColor(ctos(this.buffer).substring(prestep - 1))) {
                    prestep++;
                }
            }

            if ((prestep + drawLen) > len) {
                drawLen = len - prestep;
            }

            // extract <drawLen> characters from the field at <prestep>
            if (drawLen >= MAX_EDIT_LINE) {
                common.Error("drawLen >= MAX_EDIT_LINE");
            }

//	memcpy( str, buffer + prestep, drawLen );
            Nio.arraycopy(this.buffer, prestep, str, 0, drawLen);
            str[drawLen] = 0;

            // draw it
            renderSystem.DrawSmallStringExt(x, y, str, colorWhite, false, shader);

            // draw the cursor
            if (!showCursor) {
                return;
            }

            if (((com_ticNumber >> 4) & 1) == 1) {
                return;		// off blink
            }

            if (idKeyInput.GetOverstrikeMode()) {
                cursorChar = 11;
            } else {
                cursorChar = 10;
            }

            // Move the cursor back to account for color codes
            for (int i = 0; i < this.cursor; i++) {
                if (idStr.IsColor(ctos(str[i]))) {//TODO:check
                    i++;
                    prestep += 2;
                }
            }

            renderSystem.DrawSmallChar(x + ((this.cursor - prestep) * size), y, cursorChar, shader);
        }

        public void SetBuffer(final String buf) {
            Clear();
            idStr.Copynz(this.buffer, buf, this.buffer.length);
            SetCursor(strLen(this.buffer));
        }
    }

    /*
     ===============
     FindMatches
     ===============
     */
    static class FindMatches extends void_callback<String> {

        private static final void_callback instance = new FindMatches();

        public static void_callback getInstance() {
            return instance;
        }

        @Override
        public void run(String... objects) {
            final String s = objects[0];

            int i;

            if (idStr.Icmpn(s, ctos(globalAutoComplete.completionString), strLen(globalAutoComplete.completionString)) != 0) {
                return;
            }
            globalAutoComplete.matchCount++;
            if (globalAutoComplete.matchCount == 1) {
                idStr.Copynz(globalAutoComplete.currentMatch, s, globalAutoComplete.currentMatch.length);
                return;
            }

            // cut currentMatch to the amount common with s
            for (i = 0; i < s.length(); i++) {
                if (Character.toLowerCase(globalAutoComplete.currentMatch[i]) != Character.toLowerCase(s.charAt(i))) {
                    globalAutoComplete.currentMatch[i] = 0;
                    break;
                }
            }
            globalAutoComplete.currentMatch[i] = 0;
        }
    }

    /*
     ===============
     FindIndexMatch
     ===============
     */
    static class FindIndexMatch extends void_callback<String> {

        private static final void_callback instance = new FindIndexMatch();

        public static void_callback getInstance() {
            return instance;
        }

        @Override
        public void run(String... objects) {
            final String s = objects[0];
            final String completionStr = ctos(globalAutoComplete.completionString);

            if (idStr.Icmpn(s, completionStr, completionStr.length()) != 0) {
                return;
            }

            if (globalAutoComplete.findMatchIndex == globalAutoComplete.matchIndex) {
                idStr.Copynz(globalAutoComplete.currentMatch, s, globalAutoComplete.currentMatch.length);
            }

            globalAutoComplete.findMatchIndex++;
        }
    }

    /*
     ===============
     PrintMatches
     ===============
     */
    static class PrintMatches extends void_callback<String> {

        private static final void_callback instance = new PrintMatches();

        public static void_callback getInstance() {
            return instance;
        }

        @Override
        public void run(String... objects) throws idException {
            final String s = objects[0];
            final String currentMatch = ctos(globalAutoComplete.currentMatch);

            if (idStr.Icmpn(s, currentMatch, currentMatch.length()) == 0) {
                common.Printf("    %s\n", s);
            }
        }
    }

    /*
     ===============
     PrintCvarMatches
     ===============
     */
    static class PrintCvarMatches extends void_callback<String> {

        private static final void_callback instance = new PrintCvarMatches();

        public static void_callback getInstance() {
            return instance;
        }

        @Override
        public void run(String... objects) throws idException {
            final String s = objects[0];
            final String currentMatch = ctos(globalAutoComplete.currentMatch);

            if (idStr.Icmpn(s, currentMatch, currentMatch.length()) == 0) {
                common.Printf("    %s" + S_COLOR_WHITE + " = \"%s\"\n", s, cvarSystem.GetCVarString(s));
            }
        }
    }
}
