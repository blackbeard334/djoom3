package neo.ui;

import static neo.Renderer.Material.SS_GUI;
import static neo.TempDump.NOT;
import static neo.TempDump.ctos;
import static neo.TempDump.etoi;
import static neo.TempDump.isNotNullOrEmpty;
import static neo.TempDump.itob;
import static neo.framework.CVarSystem.cvarSystem;
import static neo.framework.DeclManager.declManager;
import static neo.framework.FileSystem_h.fileSystem;
import static neo.framework.KeyInput.K_BACKSPACE;
import static neo.framework.KeyInput.K_CTRL;
import static neo.framework.KeyInput.K_DEL;
import static neo.framework.KeyInput.K_DOWNARROW;
import static neo.framework.KeyInput.K_END;
import static neo.framework.KeyInput.K_ENTER;
import static neo.framework.KeyInput.K_ESCAPE;
import static neo.framework.KeyInput.K_HOME;
import static neo.framework.KeyInput.K_INS;
import static neo.framework.KeyInput.K_KP_ENTER;
import static neo.framework.KeyInput.K_LEFTARROW;
import static neo.framework.KeyInput.K_RIGHTARROW;
import static neo.framework.KeyInput.K_UPARROW;
import static neo.idlib.Lib.colorWhite;
import static neo.idlib.Lib.idLib.common;
import static neo.sys.sys_public.sysEventType_t.SE_CHAR;
import static neo.sys.sys_public.sysEventType_t.SE_KEY;
import static neo.sys.win_input.Sys_GetConsoleKey;
import static neo.ui.Window.WIN_CANFOCUS;
import static neo.ui.Window.WIN_FOCUS;
import static neo.ui.Window.idWindow.ON.ON_ACTION;
import static neo.ui.Window.idWindow.ON.ON_ACTIONRELEASE;
import static neo.ui.Window.idWindow.ON.ON_ENTER;
import static neo.ui.Window.idWindow.ON.ON_ENTERRELEASE;
import static neo.ui.Window.idWindow.ON.ON_ESC;

import java.nio.ByteBuffer;

import neo.Renderer.Material.idMaterial;
import neo.framework.CVarSystem.idCVar;
import neo.framework.KeyInput.idKeyInput;
import neo.idlib.Text.Parser.idParser;
import neo.idlib.Text.Str.idStr;
import neo.idlib.containers.List.idList;
import neo.idlib.math.Math_h.idMath;
import neo.idlib.math.Vector.idVec4;
import neo.sys.sys_public.sysEvent_s;
import neo.ui.DeviceContext.idDeviceContext;
import neo.ui.Rectangle.idRectangle;
import neo.ui.SimpleWindow.drawWin_t;
import neo.ui.SliderWindow.idSliderWindow;
import neo.ui.UserInterfaceLocal.idUserInterfaceLocal;
import neo.ui.Window.idWindow;
import neo.ui.Winvar.idWinBool;
import neo.ui.Winvar.idWinStr;
import neo.ui.Winvar.idWinVar;

/**
 *
 */
public class EditWindow {

    public static final int MAX_EDITFIELD = 4096;

    static class idEditWindow extends idWindow {

        private int maxChars;
        private int paintOffset;
        private int cursorPos;
        private int cursorLine;
        private int cvarMax;
        private boolean wrap;
        private boolean readonly;
        private boolean numeric;
        private idStr sourceFile = new idStr();
        private idSliderWindow scroller;
        private idList<Integer> breaks = new idList<>();
        private float sizeBias;
        private int textIndex;
        private int lastTextLength;
        private boolean forceScroll;
        private idWinBool password = new idWinBool();
        //
        private idWinStr cvarStr = new idWinStr();
        private idCVar cvar;
        //
        private idWinBool liveUpdate = new idWinBool();
        private idWinStr cvarGroup = new idWinStr();
        //
        //

        public idEditWindow(idUserInterfaceLocal gui) {
            super(gui);
            this.gui = gui;
            CommonInit();
        }

        public idEditWindow(idDeviceContext dc, idUserInterfaceLocal gui) {
            super(dc, gui);
            this.dc = dc;
            this.gui = gui;
            CommonInit();
        }
//	// virtual 			~idEditWindow();
//

        @Override
        public void Draw(int time, float x, float y) {
            idVec4 color = foreColor.oCastIdVec4();

            UpdateCvar(true);

            int len = text.Length();
            if (len != lastTextLength) {
                scroller.SetValue(0.0f);
                EnsureCursorVisible();
                lastTextLength = len;
            }
            float scale = textScale.oCastFloat();

            String pass = "";
            final String buffer;
            if (password != null) {
                int temp = 0;//text;
                for (; temp < text.Length(); temp++) {
                    pass += "*";
                }
                buffer = pass;
            } else {
                buffer = text.c_str();
            }

            if (cursorPos > len) {
                cursorPos = len;
            }

            idRectangle rect = textRect;

            rect.x -= paintOffset;
            rect.w += paintOffset;

            if (wrap && scroller.GetHigh() > 0.0f) {
                float lineHeight = GetMaxCharHeight() + 5;
                rect.y -= scroller.GetValue() * lineHeight;
                rect.w -= sizeBias;
                rect.h = (breaks.Num() + 1) * lineHeight;
            }

            if (hover && !noEvents.oCastBoolean() && Contains(gui.CursorX(), gui.CursorY())) {
                color = hoverColor.oCastIdVec4();
            } else {
                hover = false;
            }
            if ((flags & WIN_FOCUS) != 0) {
                color = hoverColor.oCastIdVec4();
            }

            dc.DrawText(buffer, scale, 0, color, rect, wrap, itob(flags & WIN_FOCUS) ? cursorPos : -1);
        }
        private static char[] buffer = new char[MAX_EDITFIELD];

        @Override
        public String HandleEvent(final sysEvent_s event, boolean[] updateVisuals) {
            String ret = "";

            if (wrap) {
                // need to call this to allow proper focus and capturing on embedded children
                ret = super.HandleEvent(event, updateVisuals);
                if (ret != null && !ret.isEmpty()) {
                    return ret;
                }
            }

            if ((event.evType != SE_CHAR && event.evType != SE_KEY)) {
                return ret;
            }

            idStr.Copynz(buffer, text.c_str(), buffer.length);
            int key = event.evValue;
            int len = text.Length();

            if (event.evType == SE_CHAR) {
                if (event.evValue == Sys_GetConsoleKey(false) || event.evValue == Sys_GetConsoleKey(true)) {
                    return "";
                }

                if (updateVisuals != null) {
                    updateVisuals[0] = true;
                }

                if (maxChars != 0 && len > maxChars) {
                    len = maxChars;
                }

                if ((key == K_ENTER || key == K_KP_ENTER) && event.evValue2 != 0) {
                    RunScript(etoi(ON_ACTION));
                    RunScript(etoi(ON_ENTER));
                    return cmd.toString();
                }

                if (key == K_ESCAPE) {
                    RunScript(etoi(ON_ESC));
                    return cmd.toString();
                }

                if (readonly) {
                    return "";
                }

                if (key == 'h' - 'a' + 1 || key == K_BACKSPACE) {	// ctrl-h is backspace
                    if (cursorPos > 0) {
                        if (cursorPos >= len) {
                            buffer[len - 1] = 0;
                            cursorPos = len - 1;
                        } else {
//					memmove( &buffer[ cursorPos - 1 ], &buffer[ cursorPos ], len + 1 - cursorPos);
                            System.arraycopy(buffer, cursorPos, buffer, cursorPos - 1, len + 1 - cursorPos);
                            cursorPos--;
                        }

                        text.data.oSet(buffer);
                        UpdateCvar(false);
                        RunScript(etoi(ON_ACTION));
                    }

                    return "";
                }

                //
                // ignore any non printable chars (except enter when wrap is enabled)
                //
                if (wrap && (key == K_ENTER || key == K_KP_ENTER)) {
                } else if (!idStr.CharIsPrintable(key)) {
                    return "";
                }

                if (numeric) {
                    if ((key < '0' || key > '9') && key != '.') {
                        return "";
                    }
                }

                if (dc.GetOverStrike()) {
                    if (maxChars != 0 && cursorPos >= maxChars) {
                        return "";
                    }
                } else {
                    if ((len == MAX_EDITFIELD - 1) || (maxChars != 0 && len >= maxChars)) {
                        return "";
                    }
//			memmove( &buffer[ cursorPos + 1 ], &buffer[ cursorPos ], len + 1 - cursorPos );
                    System.arraycopy(buffer, cursorPos, buffer, cursorPos + 1, len + 1 - cursorPos);
                }

                buffer[cursorPos] = (char) key;

                text.data.oSet(buffer);
                UpdateCvar(false);
                RunScript(etoi(ON_ACTION));

                if (cursorPos < len + 1) {
                    cursorPos++;
                }
                EnsureCursorVisible();

            } else if (event.evType == SE_KEY && event.evValue2 != 0) {

                if (updateVisuals != null) {
                    updateVisuals[0] = true;
                }

                if (key == K_DEL) {
                    if (readonly) {
                        return ret;
                    }
                    if (cursorPos < len) {
//				memmove( &buffer[cursorPos], &buffer[cursorPos + 1], len - cursorPos);
                        System.arraycopy(buffer, cursorPos + 1, buffer, cursorPos, len - cursorPos);
                        text.data.oSet(buffer);
                        UpdateCvar(false);
                        RunScript(etoi(ON_ACTION));
                    }
                    return ret;
                }

                if (key == K_RIGHTARROW) {
                    if (cursorPos < len) {
                        if (idKeyInput.IsDown(K_CTRL)) {
                            // skip to next word
                            while ((cursorPos < len) && (buffer[ cursorPos] != ' ')) {
                                cursorPos++;
                            }

                            while ((cursorPos < len) && (buffer[ cursorPos] == ' ')) {
                                cursorPos++;
                            }
                        } else {
                            if (cursorPos < len) {
                                cursorPos++;
                            }
                        }
                    }

                    EnsureCursorVisible();

                    return ret;
                }

                if (key == K_LEFTARROW) {
                    if (idKeyInput.IsDown(K_CTRL)) {
                        // skip to previous word
                        while ((cursorPos > 0) && (buffer[ cursorPos - 1] == ' ')) {
                            cursorPos--;
                        }

                        while ((cursorPos > 0) && (buffer[ cursorPos - 1] != ' ')) {
                            cursorPos--;
                        }
                    } else {
                        if (cursorPos > 0) {
                            cursorPos--;
                        }
                    }

                    EnsureCursorVisible();

                    return ret;
                }

                if (key == K_HOME) {
                    if (idKeyInput.IsDown(K_CTRL) || cursorLine <= 0 || (cursorLine >= breaks.Num())) {
                        cursorPos = 0;
                    } else {
                        cursorPos = breaks.oGet(cursorLine);
                    }
                    EnsureCursorVisible();
                    return ret;
                }

                if (key == K_END) {
                    if (idKeyInput.IsDown(K_CTRL) || (cursorLine < -1) || (cursorLine >= breaks.Num() - 1)) {
                        cursorPos = len;
                    } else {
                        cursorPos = breaks.oGet(cursorLine + 1) - 1;
                    }
                    EnsureCursorVisible();
                    return ret;
                }

                if (key == K_INS) {
                    if (!readonly) {
                        dc.SetOverStrike(!dc.GetOverStrike());
                    }
                    return ret;
                }

                if (key == K_DOWNARROW) {
                    if (idKeyInput.IsDown(K_CTRL)) {
                        scroller.SetValue(scroller.GetValue() + 1.0f);
                    } else {
                        if (cursorLine < breaks.Num() - 1) {
                            int offset = cursorPos - breaks.oGet(cursorLine);
                            cursorPos = breaks.oGet(cursorLine + 1) + offset;
                            EnsureCursorVisible();
                        }
                    }
                }

                if (key == K_UPARROW) {
                    if (idKeyInput.IsDown(K_CTRL)) {
                        scroller.SetValue(scroller.GetValue() - 1.0f);
                    } else {
                        if (cursorLine > 0) {
                            int offset = cursorPos - breaks.oGet(cursorLine);
                            cursorPos = breaks.oGet(cursorLine - 1) + offset;
                            EnsureCursorVisible();
                        }
                    }
                }

                if (key == K_ENTER || key == K_KP_ENTER) {
                    RunScript(etoi(ON_ACTION));
                    RunScript(etoi(ON_ENTER));
                    return cmd.toString();
                }

                if (key == K_ESCAPE) {
                    RunScript(etoi(ON_ESC));
                    return cmd.toString();
                }

            } else if (event.evType == SE_KEY && 0 == event.evValue2) {
                if (key == K_ENTER || key == K_KP_ENTER) {
                    RunScript(etoi(ON_ENTERRELEASE));
                    return cmd.toString();
                } else {
                    RunScript(etoi(ON_ACTIONRELEASE));
                }
            }

            return ret;
        }

        @Override
        public void PostParse() {
            super.PostParse();

            if (maxChars == 0) {
                maxChars = 10;
            }
            if (sourceFile.Length() != 0) {
                ByteBuffer[] buffer = {null};
                fileSystem.ReadFile(sourceFile, buffer);
                text.data.oSet(new String(buffer[0].array()));
                fileSystem.FreeFile(buffer);
            }

            InitCvar();
            InitScroller(false);

            EnsureCursorVisible();

            flags |= WIN_CANFOCUS;
        }

        @Override
        public void GainFocus() {
            cursorPos = text.Length();
            EnsureCursorVisible();
        }

        @Override
        public int/*size_t*/ Allocated() {
            return super.Allocated();
        }

        @Override
        public idWinVar GetWinVarByName(final String _name, boolean winLookup /*= false*/, drawWin_t[] owner /*= NULL*/) {
            if (idStr.Icmp(_name, "cvar") == 0) {
                return cvarStr;
            }
            if (idStr.Icmp(_name, "password") == 0) {
                return password;
            }
            if (idStr.Icmp(_name, "liveUpdate") == 0) {
                return liveUpdate;
            }
            if (idStr.Icmp(_name, "cvarGroup") == 0) {
                return cvarGroup;
            }
            return super.GetWinVarByName(_name, winLookup, owner);
        }

        @Override
        public void HandleBuddyUpdate(idWindow buddy) {
        }

        @Override
        public void Activate(boolean activate, idStr act) {
            super.Activate(activate, act);
            if (activate) {
                UpdateCvar(true, true);
                EnsureCursorVisible();
            }
        }

        @Override
        public void RunNamedEvent(final String eventName) {
            idStr event, group;

            if (0 == idStr.Cmpn(eventName, "cvar read ", 10)) {
                event = new idStr(eventName);
                group = event.Mid(10, event.Length() - 10);
                if (NOT(group.Cmp(cvarGroup.data))) {
                    UpdateCvar(true, true);
                }
            } else if (0 == idStr.Cmpn(eventName, "cvar write ", 11)) {
                event = new idStr(eventName);
                group = event.Mid(11, event.Length() - 11);
                if (NOT(group.Cmp(cvarGroup.data))) {
                    UpdateCvar(false, true);
                }
            }
        }

        @Override
        protected boolean ParseInternalVar(final String _name, idParser src) {
            if (idStr.Icmp(_name, "maxchars") == 0) {
                maxChars = src.ParseInt();
                return true;
            }
            if (idStr.Icmp(_name, "numeric") == 0) {
                numeric = src.ParseBool();
                return true;
            }
            if (idStr.Icmp(_name, "wrap") == 0) {
                wrap = src.ParseBool();
                return true;
            }
            if (idStr.Icmp(_name, "readonly") == 0) {
                readonly = src.ParseBool();
                return true;
            }
            if (idStr.Icmp(_name, "forceScroll") == 0) {
                forceScroll = src.ParseBool();
                return true;
            }
            if (idStr.Icmp(_name, "source") == 0) {
                ParseString(src, sourceFile);
                return true;
            }
            if (idStr.Icmp(_name, "password") == 0) {
                password.data = src.ParseBool();
                return true;
            }
            if (idStr.Icmp(_name, "cvarMax") == 0) {
                cvarMax = src.ParseInt();
                return true;
            }

            return super.ParseInternalVar(_name, src);
        }

        private void InitCvar() {
            if (!isNotNullOrEmpty(cvarStr.data)) {
                if (text.GetName() == null) {
                    common.Warning("idEditWindow::InitCvar: gui '%s' window '%s' has an empty cvar string", gui.GetSourceFile(), name);
                }
                cvar = null;
                return;
            }

            cvar = cvarSystem.Find(cvarStr.data.toString());
            if (null == cvar) {
                common.Warning("idEditWindow::InitCvar: gui '%s' window '%s' references undefined cvar '%s'", gui.GetSourceFile(), name, cvarStr.c_str());
                return;
            }
        }

        // true: read the updated cvar from cvar system
        // false: write to the cvar system
        // force == true overrides liveUpdate 0
        private void UpdateCvar(boolean read, boolean force /*= false*/) {
            if (force || liveUpdate.oCastBoolean()) {
                if (cvar != null) {
                    if (read) {
                        text.data.oSet(cvar.GetString());
                    } else {
                        cvar.SetString(text.data.toString());
                        if (cvarMax != 0 && (cvar.GetInteger() > cvarMax)) {
                            cvar.SetInteger(cvarMax);
                        }
                    }
                }
            }
        }

        private void UpdateCvar(boolean read) {
            this.UpdateCvar(read, false);
        }

        private void CommonInit() {
            maxChars = 128;
            numeric = false;
            paintOffset = 0;
            cursorPos = 0;
            cursorLine = 0;
            cvarMax = 0;
            wrap = false;
            sourceFile.oSet("");
            scroller = null;
            sizeBias = 0;
            lastTextLength = 0;
            forceScroll = false;
            password.data = false;
            cvar = null;
            liveUpdate.data = true;
            readonly = false;

            scroller = new idSliderWindow(dc, gui);
        }

        private void EnsureCursorVisible() {
            if (readonly) {
                cursorPos = -1;
            } else if (maxChars == 1) {
                cursorPos = 0;
            }

            if (NOT(dc)) {
                return;
            }

            SetFont();
            if (!wrap) {
                int cursorX = 0;
                if (password.data) {
                    cursorX = cursorPos * dc.CharWidth('*', textScale.data);
                } else {
                    int i = 0;
                    while (i < text.Length() && i < cursorPos) {
                        if (idStr.IsColor(ctos(text.data.oGet(i)))) {
                            i += 2;
                        } else {
                            cursorX += dc.CharWidth(text.data.oGet(i), textScale.data);
                            i++;
                        }
                    }
                }
                int maxWidth = (int) GetMaxCharWidth();
                int left = cursorX - maxWidth;
                int right = (int) ((cursorX - textRect.w) + maxWidth);

                if (paintOffset > left) {
                    // When we go past the left side, we want the text to jump 6 characters
                    paintOffset = left - maxWidth * 6;
                }
                if (paintOffset < right) {
                    paintOffset = right;
                }
                if (paintOffset < 0) {
                    paintOffset = 0;
                }
                scroller.SetRange(0.0f, 0.0f, 1.0f);

            } else {
                // Word wrap

                breaks.Clear();
                idRectangle rect = textRect;
                rect.w -= sizeBias;
                dc.DrawText(text.data, textScale.data, textAlign, colorWhite, rect, true, (itob(flags & WIN_FOCUS)) ? cursorPos : -1, true, breaks);

                int fit = (int) (textRect.h / (GetMaxCharHeight() + 5));
                if (fit < breaks.Num() + 1) {
                    scroller.SetRange(0, breaks.Num() + 1 - fit, 1);
                } else {
                    // The text fits completely in the box
                    scroller.SetRange(0.0f, 0.0f, 1.0f);
                }

                if (forceScroll) {
                    scroller.SetValue(breaks.Num() - fit);
                } else if (readonly) {
                } else {
                    cursorLine = 0;
                    for (int i = 1; i < breaks.Num(); i++) {
                        if (cursorPos >= breaks.oGet(i)) {
                            cursorLine = i;
                        } else {
                            break;
                        }
                    }
                    int topLine = idMath.FtoiFast(scroller.GetValue());
                    if (cursorLine < topLine) {
                        scroller.SetValue(cursorLine);
                    } else if (cursorLine >= topLine + fit) {
                        scroller.SetValue((cursorLine - fit) + 1);
                    }
                }
            }
        }

        /*
         ================
         idEditWindow::InitScroller

         This is the same as in idListWindow
         ================
         */
        private void InitScroller(boolean horizontal) {
            String thumbImage = "guis/assets/scrollbar_thumb.tga";
            String barImage = "guis/assets/scrollbarv.tga";
            String scrollerName = "_scrollerWinV";

            if (horizontal) {
                barImage = "guis/assets/scrollbarh.tga";
                scrollerName = "_scrollerWinH";
            }

            final idMaterial mat = declManager.FindMaterial(barImage);
            mat.SetSort(SS_GUI);
            sizeBias = mat.GetImageWidth();

            idRectangle scrollRect = new idRectangle();
            if (horizontal) {
                sizeBias = mat.GetImageHeight();
                scrollRect.x = 0;
                scrollRect.y = (clientRect.h - sizeBias);
                scrollRect.w = clientRect.w;
                scrollRect.h = sizeBias;
            } else {
                scrollRect.x = (clientRect.w - sizeBias);
                scrollRect.y = 0;
                scrollRect.w = sizeBias;
                scrollRect.h = clientRect.h;
            }

            scroller.InitWithDefaults(scrollerName, scrollRect, foreColor.data, matColor.data, mat.GetName(), thumbImage, !horizontal, true);
            InsertChild(scroller, null);
            scroller.SetBuddy(this);
        }
    };
}
