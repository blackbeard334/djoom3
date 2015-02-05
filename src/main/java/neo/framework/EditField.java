package neo.framework;

import neo.Renderer.Material.idMaterial;
import static neo.Renderer.RenderSystem.SMALLCHAR_WIDTH;
import static neo.Renderer.RenderSystem.renderSystem;
import static neo.TempDump.ctos;
import static neo.TempDump.strLen;
import neo.TempDump.void_callback;
import static neo.framework.CVarSystem.cvarSystem;
import static neo.framework.CmdSystem.cmdSystem;
import static neo.framework.Common.com_ticNumber;
import static neo.framework.Common.common;
import neo.framework.EditField.FindMatches;
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
import neo.framework.KeyInput.idKeyInput;
import neo.idlib.CmdArgs.idCmdArgs;
import static neo.idlib.Lib.colorWhite;
import neo.idlib.Lib.idException;
import static neo.idlib.Text.Str.S_COLOR_WHITE;
import neo.idlib.Text.Str.idStr;
import static neo.sys.win_main.Sys_GetClipboardData;

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
        private char[] buffer = new char[MAX_EDIT_LINE];
        private autoComplete_s autoComplete;
        //
        //

        public idEditField() {
            widthInChars = 0;
            autoComplete = new autoComplete_s();
            Clear();
        }
//public					~idEditField();

        public void Clear() {
            buffer[0] = 0;
            cursor = 0;
            scroll = 0;
            autoComplete.length = 0;
            autoComplete.valid = false;
        }

        public void SetWidthInChars(int w) {
            assert (w <= MAX_EDIT_LINE);
            widthInChars = w;
        }

        public void SetCursor(int c) {
            assert (c <= MAX_EDIT_LINE);
            cursor = c;
        }

        public int GetCursor() {
            return cursor;
        }

        public void ClearAutoComplete() {
            if (autoComplete.length > 0 && autoComplete.length <= ctos(buffer).length()) {
                buffer[autoComplete.length] = '\0';
                if (cursor > autoComplete.length) {
                    cursor = autoComplete.length;
                }
            }
            autoComplete.length = 0;
            autoComplete.valid = false;
        }

        public int GetAutoCompleteLength() {
            return autoComplete.length;
        }

        public void AutoComplete() throws idException {
            char[] completionArgString = new char[MAX_EDIT_LINE];
            idCmdArgs args = new idCmdArgs();
            final void_callback findMatches = FindMatches.getInstance();
            final void_callback findIndexMatch = FindIndexMatch.getInstance();
            final void_callback printMatches = PrintMatches.getInstance();

            if (!autoComplete.valid) {
                args.TokenizeString(ctos(buffer), false);
                idStr.Copynz(autoComplete.completionString, args.Argv(0), autoComplete.completionString.length);
                idStr.Copynz(completionArgString, args.Args(), completionArgString.length);
                autoComplete.matchCount = 0;
                autoComplete.matchIndex = 0;
                autoComplete.currentMatch[0] = 0;

                if (strLen(autoComplete.completionString) == 0) {
                    return;
                }

                globalAutoComplete = autoComplete;

                cmdSystem.CommandCompletion(findMatches);
                cvarSystem.CommandCompletion(findMatches);

                autoComplete = globalAutoComplete;

                if (autoComplete.matchCount == 0) {
                    return;	// no matches
                }

                // when there's only one match or there's an argument
                if (autoComplete.matchCount == 1 || completionArgString[0] != '\0') {

                    /// try completing arguments
                    idStr.Append(autoComplete.completionString, autoComplete.completionString.length, " ");
                    idStr.Append(autoComplete.completionString, autoComplete.completionString.length, ctos(completionArgString));
                    autoComplete.matchCount = 0;

                    globalAutoComplete = autoComplete;

                    cmdSystem.ArgCompletion(ctos(autoComplete.completionString), findMatches);
                    cvarSystem.ArgCompletion(ctos(autoComplete.completionString), findMatches);

                    autoComplete = globalAutoComplete;

                    idStr.snPrintf(buffer, buffer.length, "%s", autoComplete.currentMatch);

                    if (autoComplete.matchCount == 0) {
                        // no argument matches
                        idStr.Append(buffer, buffer.length, " ");
                        idStr.Append(buffer, buffer.length, ctos(completionArgString));
                        SetCursor(strLen(buffer));
                        return;
                    }
                } else {

                    // multiple matches, complete to shortest
                    idStr.snPrintf(buffer, buffer.length, "%s", autoComplete.currentMatch);
                    if (strLen(completionArgString) != 0) {
                        idStr.Append(buffer, buffer.length, " ");
                        idStr.Append(buffer, buffer.length, ctos(completionArgString));
                    }
                }

                autoComplete.length = strLen(buffer);
                autoComplete.valid = (autoComplete.matchCount != 1);
                SetCursor(autoComplete.length);

                common.Printf("]%s\n", buffer);

                // run through again, printing matches
                globalAutoComplete = autoComplete;

                cmdSystem.CommandCompletion(printMatches);
                cmdSystem.ArgCompletion(ctos(autoComplete.completionString), printMatches);
                cvarSystem.CommandCompletion(PrintCvarMatches.getInstance());
                cmdSystem.ArgCompletion(ctos(autoComplete.completionString), printMatches);

            } else if (autoComplete.matchCount != 1) {

                // get the next match and show instead
                autoComplete.matchIndex++;
                if (autoComplete.matchIndex == autoComplete.matchCount) {
                    autoComplete.matchIndex = 0;
                }
                autoComplete.findMatchIndex = 0;

                globalAutoComplete = autoComplete;

                cmdSystem.CommandCompletion(findIndexMatch);
                cmdSystem.ArgCompletion(ctos(autoComplete.completionString), findIndexMatch);
                cvarSystem.CommandCompletion(findIndexMatch);
                cmdSystem.ArgCompletion(ctos(autoComplete.completionString), findIndexMatch);

                autoComplete = globalAutoComplete;

                // and print it
                idStr.snPrintf(buffer, buffer.length, ctos(autoComplete.currentMatch));
                if (autoComplete.length > (int) strLen(buffer)) {
                    autoComplete.length = strLen(buffer);
                }
                SetCursor(autoComplete.length);
            }
        }

        public void CharEvent(int ch) {
            int len;

            if (ch == 'v' - 'a' + 1) {	// ctrl-v is paste
                Paste();
                return;
            }

            if (ch == 'c' - 'a' + 1) {	// ctrl-c clears the field
                Clear();
                return;
            }

            len = strLen(buffer);

            if (ch == 'h' - 'a' + 1 || ch == K_BACKSPACE) {	// ctrl-h is backspace
                if (cursor > 0) {
//			memmove( buffer + cursor - 1, buffer + cursor, len + 1 - cursor );
                    System.arraycopy(buffer, cursor, buffer, cursor - 1, len + 1 - cursor);
                    cursor--;
                    if (cursor < scroll) {
                        scroll--;
                    }
                }
                return;
            }

            if (ch == 'a' - 'a' + 1) {	// ctrl-a is home
                cursor = 0;
                scroll = 0;
                return;
            }

            if (ch == 'e' - 'a' + 1) {	// ctrl-e is end
                cursor = len;
                scroll = cursor - widthInChars;
                return;
            }

            //
            // ignore any other non printable chars
            //
            if (ch < 32) {
                return;
            }

            if (idKeyInput.GetOverstrikeMode()) {
                if (cursor == MAX_EDIT_LINE - 1) {
                    return;
                }
                buffer[cursor] = (char) ch;
                cursor++;
            } else {	// insert mode
                if (len == MAX_EDIT_LINE - 1) {
                    return; // all full
                }
//		memmove( buffer + cursor + 1, buffer + cursor, len + 1 - cursor );
                System.arraycopy(buffer, cursor, buffer, cursor + 1, len + 1 - cursor);
                buffer[cursor] = (char) ch;
                cursor++;
            }

            if (cursor >= widthInChars) {
                scroll++;
            }

            if (cursor == len + 1) {
                buffer[cursor] = 0;
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

            len = strLen(buffer);

            if (key == K_DEL) {
                if (autoComplete.length != 0) {
                    ClearAutoComplete();
                } else if (cursor < len) {
//			memmove( buffer + cursor, buffer + cursor + 1, len - cursor );
                    System.arraycopy(buffer, cursor + 1, buffer, cursor, len - cursor);
                }
                return;
            }

            if (key == K_RIGHTARROW) {
                if (idKeyInput.IsDown(K_CTRL)) {
                    // skip to next word
                    while ((cursor < len) && (buffer[cursor] != ' ')) {
                        cursor++;
                    }

                    while ((cursor < len) && (buffer[cursor] == ' ')) {
                        cursor++;
                    }
                } else {
                    cursor++;
                }

                if (cursor > len) {
                    cursor = len;
                }

                if (cursor >= scroll + widthInChars) {
                    scroll = cursor - widthInChars + 1;
                }

                if (autoComplete.length > 0) {
                    autoComplete.length = cursor;
                }
                return;
            }

            if (key == K_LEFTARROW) {
                if (idKeyInput.IsDown(K_CTRL)) {
                    // skip to previous word
                    while ((cursor > 0) && (buffer[cursor - 1] == ' ')) {
                        cursor--;
                    }

                    while ((cursor > 0) && (buffer[cursor - 1] != ' ')) {
                        cursor--;
                    }
                } else {
                    cursor--;
                }

                if (cursor < 0) {
                    cursor = 0;
                }
                if (cursor < scroll) {
                    scroll = cursor;
                }

                if (autoComplete.length != 0) {
                    autoComplete.length = cursor;
                }
                return;
            }

            if (key == K_HOME || (Character.toLowerCase(key) == 'a' && idKeyInput.IsDown(K_CTRL))) {
                cursor = 0;
                scroll = 0;
                if (autoComplete.length != 0) {
                    autoComplete.length = cursor;
                    autoComplete.valid = false;
                }
                return;
            }

            if (key == K_END || (Character.toLowerCase(key) == 'e' && idKeyInput.IsDown(K_CTRL))) {
                cursor = len;
                if (cursor >= scroll + widthInChars) {
                    scroll = cursor - widthInChars + 1;
                }
                if (autoComplete.length != 0) {
                    autoComplete.length = cursor;
                    autoComplete.valid = false;
                }
                return;
            }

            if (key == K_INS) {
                idKeyInput.SetOverstrikeMode(!idKeyInput.GetOverstrikeMode());
                return;
            }

            // clear autocompletion buffer on normal key input
            if (key != K_CAPSLOCK && key != K_ALT && key != K_CTRL && key != K_SHIFT) {
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
            return buffer;
        }

        public void Draw(int x, int y, int width, boolean showCursor, final idMaterial shader) throws idException {
            int len;
            int drawLen;
            int prestep;
            int cursorChar;
            char[] str = new char[MAX_EDIT_LINE];
            int size;

            size = SMALLCHAR_WIDTH;

            drawLen = widthInChars;
            len = strLen(buffer) + 1;

            // guarantee that cursor will be visible
            if (len <= drawLen) {
                prestep = 0;
            } else {
                if (scroll + drawLen > len) {
                    scroll = len - drawLen;
                    if (scroll < 0) {
                        scroll = 0;
                    }
                }
                prestep = scroll;

                // Skip color code
                if (idStr.IsColor(ctos(buffer).substring(prestep))) {
                    prestep += 2;
                }
                if (prestep > 0 && idStr.IsColor(ctos(buffer).substring(prestep - 1))) {
                    prestep++;
                }
            }

            if (prestep + drawLen > len) {
                drawLen = len - prestep;
            }

            // extract <drawLen> characters from the field at <prestep>
            if (drawLen >= MAX_EDIT_LINE) {
                common.Error("drawLen >= MAX_EDIT_LINE");
            }

//	memcpy( str, buffer + prestep, drawLen );
            System.arraycopy(buffer, prestep, str, 0, drawLen);
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
            for (int i = 0; i < cursor; i++) {
                if (idStr.IsColor(ctos(str[i]))) {//TODO:check
                    i++;
                    prestep += 2;
                }
            }

            renderSystem.DrawSmallChar(x + (cursor - prestep) * size, y, cursorChar, shader);
        }

        public void SetBuffer(final String buf) {
            Clear();
            idStr.Copynz(buffer, buf, buffer.length);
            SetCursor(strLen(buffer));
        }
    };

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
    };

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
    };

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
    };

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
    };
}
