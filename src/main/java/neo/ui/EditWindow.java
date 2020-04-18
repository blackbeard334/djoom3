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
import neo.open.Nio;
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
        private final idStr sourceFile = new idStr();
        private idSliderWindow scroller;
        private final idList<Integer> breaks = new idList<>();
        private float sizeBias;
        private int textIndex;
        private int lastTextLength;
        private boolean forceScroll;
        private final idWinBool password = new idWinBool();
        //
        private final idWinStr cvarStr = new idWinStr();
        private idCVar cvar;
        //
        private final idWinBool liveUpdate = new idWinBool();
        private final idWinStr cvarGroup = new idWinStr();
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
            idVec4 color = this.foreColor.oCastIdVec4();

            UpdateCvar(true);

            final int len = this.text.Length();
            if (len != this.lastTextLength) {
                this.scroller.SetValue(0.0f);
                EnsureCursorVisible();
                this.lastTextLength = len;
            }
            final float scale = this.textScale.oCastFloat();

            String pass = "";
            final String buffer;
            if (this.password != null) {
                int temp = 0;//text;
                for (; temp < this.text.Length(); temp++) {
                    pass += "*";
                }
                buffer = pass;
            } else {
                buffer = this.text.c_str();
            }

            if (this.cursorPos > len) {
                this.cursorPos = len;
            }

            final idRectangle rect = this.textRect;

            rect.x -= this.paintOffset;
            rect.w += this.paintOffset;

            if (this.wrap && (this.scroller.GetHigh() > 0.0f)) {
                final float lineHeight = GetMaxCharHeight() + 5;
                rect.y -= this.scroller.GetValue() * lineHeight;
                rect.w -= this.sizeBias;
                rect.h = (this.breaks.Num() + 1) * lineHeight;
            }

            if (this.hover && !this.noEvents.oCastBoolean() && Contains(this.gui.CursorX(), this.gui.CursorY())) {
                color = this.hoverColor.oCastIdVec4();
            } else {
                this.hover = false;
            }
            if ((this.flags & WIN_FOCUS) != 0) {
                color = this.hoverColor.oCastIdVec4();
            }

            this.dc.DrawText(buffer, scale, 0, color, rect, this.wrap, itob(this.flags & WIN_FOCUS) ? this.cursorPos : -1);
        }
        private static char[] buffer = new char[MAX_EDITFIELD];

        @Override
        public String HandleEvent(final sysEvent_s event, boolean[] updateVisuals) {
            String ret = "";

            if (this.wrap) {
                // need to call this to allow proper focus and capturing on embedded children
                ret = super.HandleEvent(event, updateVisuals);
                if ((ret != null) && !ret.isEmpty()) {
                    return ret;
                }
            }

            if (((event.evType != SE_CHAR) && (event.evType != SE_KEY))) {
                return ret;
            }

            idStr.Copynz(buffer, this.text.c_str(), buffer.length);
            final int key = event.evValue;
            int len = this.text.Length();

            if (event.evType == SE_CHAR) {
                if ((event.evValue == Sys_GetConsoleKey(false)) || (event.evValue == Sys_GetConsoleKey(true))) {
                    return "";
                }

                if (updateVisuals != null) {
                    updateVisuals[0] = true;
                }

                if ((this.maxChars != 0) && (len > this.maxChars)) {
                    len = this.maxChars;
                }

                if (((key == K_ENTER) || (key == K_KP_ENTER)) && (event.evValue2 != 0)) {
                    RunScript(etoi(ON_ACTION));
                    RunScript(etoi(ON_ENTER));
                    return this.cmd.getData();
                }

                if (key == K_ESCAPE) {
                    RunScript(etoi(ON_ESC));
                    return this.cmd.getData();
                }

                if (this.readonly) {
                    return "";
                }

                if ((key == (('h' - 'a') + 1)) || (key == K_BACKSPACE)) {	// ctrl-h is backspace
                    if (this.cursorPos > 0) {
                        if (this.cursorPos >= len) {
                            buffer[len - 1] = 0;
                            this.cursorPos = len - 1;
                        } else {
//					memmove( &buffer[ cursorPos - 1 ], &buffer[ cursorPos ], len + 1 - cursorPos);
                            Nio.arraycopy(buffer, this.cursorPos, buffer, this.cursorPos - 1, (len + 1) - this.cursorPos);
                            this.cursorPos--;
                        }

                        this.text.data.oSet(buffer);
                        UpdateCvar(false);
                        RunScript(etoi(ON_ACTION));
                    }

                    return "";
                }

                //
                // ignore any non printable chars (except enter when wrap is enabled)
                //
                if (this.wrap && ((key == K_ENTER) || (key == K_KP_ENTER))) {
                } else if (!idStr.CharIsPrintable(key)) {
                    return "";
                }

                if (this.numeric) {
                    if (((key < '0') || (key > '9')) && (key != '.')) {
                        return "";
                    }
                }

                if (this.dc.GetOverStrike()) {
                    if ((this.maxChars != 0) && (this.cursorPos >= this.maxChars)) {
                        return "";
                    }
                } else {
                    if ((len == (MAX_EDITFIELD - 1)) || ((this.maxChars != 0) && (len >= this.maxChars))) {
                        return "";
                    }
//			memmove( &buffer[ cursorPos + 1 ], &buffer[ cursorPos ], len + 1 - cursorPos );
                    Nio.arraycopy(buffer, this.cursorPos, buffer, this.cursorPos + 1, (len + 1) - this.cursorPos);
                }

                buffer[this.cursorPos] = (char) key;

                this.text.data.oSet(buffer);
                UpdateCvar(false);
                RunScript(etoi(ON_ACTION));

                if (this.cursorPos < (len + 1)) {
                    this.cursorPos++;
                }
                EnsureCursorVisible();

            } else if ((event.evType == SE_KEY) && (event.evValue2 != 0)) {

                if (updateVisuals != null) {
                    updateVisuals[0] = true;
                }

                if (key == K_DEL) {
                    if (this.readonly) {
                        return ret;
                    }
                    if (this.cursorPos < len) {
//				memmove( &buffer[cursorPos], &buffer[cursorPos + 1], len - cursorPos);
                        Nio.arraycopy(buffer, this.cursorPos + 1, buffer, this.cursorPos, len - this.cursorPos);
                        this.text.data.oSet(buffer);
                        UpdateCvar(false);
                        RunScript(etoi(ON_ACTION));
                    }
                    return ret;
                }

                if (key == K_RIGHTARROW) {
                    if (this.cursorPos < len) {
                        if (idKeyInput.IsDown(K_CTRL)) {
                            // skip to next word
                            while ((this.cursorPos < len) && (buffer[ this.cursorPos] != ' ')) {
                                this.cursorPos++;
                            }

                            while ((this.cursorPos < len) && (buffer[ this.cursorPos] == ' ')) {
                                this.cursorPos++;
                            }
                        } else {
                            if (this.cursorPos < len) {
                                this.cursorPos++;
                            }
                        }
                    }

                    EnsureCursorVisible();

                    return ret;
                }

                if (key == K_LEFTARROW) {
                    if (idKeyInput.IsDown(K_CTRL)) {
                        // skip to previous word
                        while ((this.cursorPos > 0) && (buffer[ this.cursorPos - 1] == ' ')) {
                            this.cursorPos--;
                        }

                        while ((this.cursorPos > 0) && (buffer[ this.cursorPos - 1] != ' ')) {
                            this.cursorPos--;
                        }
                    } else {
                        if (this.cursorPos > 0) {
                            this.cursorPos--;
                        }
                    }

                    EnsureCursorVisible();

                    return ret;
                }

                if (key == K_HOME) {
                    if (idKeyInput.IsDown(K_CTRL) || (this.cursorLine <= 0) || (this.cursorLine >= this.breaks.Num())) {
                        this.cursorPos = 0;
                    } else {
                        this.cursorPos = this.breaks.oGet(this.cursorLine);
                    }
                    EnsureCursorVisible();
                    return ret;
                }

                if (key == K_END) {
                    if (idKeyInput.IsDown(K_CTRL) || (this.cursorLine < -1) || (this.cursorLine >= (this.breaks.Num() - 1))) {
                        this.cursorPos = len;
                    } else {
                        this.cursorPos = this.breaks.oGet(this.cursorLine + 1) - 1;
                    }
                    EnsureCursorVisible();
                    return ret;
                }

                if (key == K_INS) {
                    if (!this.readonly) {
                        this.dc.SetOverStrike(!this.dc.GetOverStrike());
                    }
                    return ret;
                }

                if (key == K_DOWNARROW) {
                    if (idKeyInput.IsDown(K_CTRL)) {
                        this.scroller.SetValue(this.scroller.GetValue() + 1.0f);
                    } else {
                        if (this.cursorLine < (this.breaks.Num() - 1)) {
                            final int offset = this.cursorPos - this.breaks.oGet(this.cursorLine);
                            this.cursorPos = this.breaks.oGet(this.cursorLine + 1) + offset;
                            EnsureCursorVisible();
                        }
                    }
                }

                if (key == K_UPARROW) {
                    if (idKeyInput.IsDown(K_CTRL)) {
                        this.scroller.SetValue(this.scroller.GetValue() - 1.0f);
                    } else {
                        if (this.cursorLine > 0) {
                            final int offset = this.cursorPos - this.breaks.oGet(this.cursorLine);
                            this.cursorPos = this.breaks.oGet(this.cursorLine - 1) + offset;
                            EnsureCursorVisible();
                        }
                    }
                }

                if ((key == K_ENTER) || (key == K_KP_ENTER)) {
                    RunScript(etoi(ON_ACTION));
                    RunScript(etoi(ON_ENTER));
                    return this.cmd.getData();
                }

                if (key == K_ESCAPE) {
                    RunScript(etoi(ON_ESC));
                    return this.cmd.getData();
                }

            } else if ((event.evType == SE_KEY) && (0 == event.evValue2)) {
                if ((key == K_ENTER) || (key == K_KP_ENTER)) {
                    RunScript(etoi(ON_ENTERRELEASE));
                    return this.cmd.getData();
                } else {
                    RunScript(etoi(ON_ACTIONRELEASE));
                }
            }

            return ret;
        }

        @Override
        public void PostParse() {
            super.PostParse();

            if (this.maxChars == 0) {
                this.maxChars = 10;
            }
            if (this.sourceFile.Length() != 0) {
                final ByteBuffer[] buffer = {null};
                fileSystem.ReadFile(this.sourceFile, buffer);
                this.text.data.oSet(new String(buffer[0].array()));
                fileSystem.FreeFile(buffer);
            }

            InitCvar();
            InitScroller(false);

            EnsureCursorVisible();

            this.flags |= WIN_CANFOCUS;
        }

        @Override
        public void GainFocus() {
            this.cursorPos = this.text.Length();
            EnsureCursorVisible();
        }

        @Override
        public int/*size_t*/ Allocated() {
            return super.Allocated();
        }

        @Override
        public idWinVar GetWinVarByName(final String _name, boolean winLookup /*= false*/, drawWin_t[] owner /*= NULL*/) {
            if (idStr.Icmp(_name, "cvar") == 0) {
                return this.cvarStr;
            }
            if (idStr.Icmp(_name, "password") == 0) {
                return this.password;
            }
            if (idStr.Icmp(_name, "liveUpdate") == 0) {
                return this.liveUpdate;
            }
            if (idStr.Icmp(_name, "cvarGroup") == 0) {
                return this.cvarGroup;
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
                if (NOT(group.Cmp(this.cvarGroup.data))) {
                    UpdateCvar(true, true);
                }
            } else if (0 == idStr.Cmpn(eventName, "cvar write ", 11)) {
                event = new idStr(eventName);
                group = event.Mid(11, event.Length() - 11);
                if (NOT(group.Cmp(this.cvarGroup.data))) {
                    UpdateCvar(false, true);
                }
            }
        }

        @Override
        protected boolean ParseInternalVar(final String _name, idParser src) {
            if (idStr.Icmp(_name, "maxchars") == 0) {
                this.maxChars = src.ParseInt();
                return true;
            }
            if (idStr.Icmp(_name, "numeric") == 0) {
                this.numeric = src.ParseBool();
                return true;
            }
            if (idStr.Icmp(_name, "wrap") == 0) {
                this.wrap = src.ParseBool();
                return true;
            }
            if (idStr.Icmp(_name, "readonly") == 0) {
                this.readonly = src.ParseBool();
                return true;
            }
            if (idStr.Icmp(_name, "forceScroll") == 0) {
                this.forceScroll = src.ParseBool();
                return true;
            }
            if (idStr.Icmp(_name, "source") == 0) {
                ParseString(src, this.sourceFile);
                return true;
            }
            if (idStr.Icmp(_name, "password") == 0) {
                this.password.data = src.ParseBool();
                return true;
            }
            if (idStr.Icmp(_name, "cvarMax") == 0) {
                this.cvarMax = src.ParseInt();
                return true;
            }

            return super.ParseInternalVar(_name, src);
        }

        private void InitCvar() {
            if (!isNotNullOrEmpty(this.cvarStr.data)) {
                if (this.text.GetName() == null) {
                    common.Warning("idEditWindow::InitCvar: gui '%s' window '%s' has an empty cvar string", this.gui.GetSourceFile(), this.name);
                }
                this.cvar = null;
                return;
            }

            this.cvar = cvarSystem.Find(this.cvarStr.data.getData());
            if (null == this.cvar) {
                common.Warning("idEditWindow::InitCvar: gui '%s' window '%s' references undefined cvar '%s'", this.gui.GetSourceFile(), this.name, this.cvarStr.c_str());
                return;
            }
        }

        // true: read the updated cvar from cvar system
        // false: write to the cvar system
        // force == true overrides liveUpdate 0
        private void UpdateCvar(boolean read, boolean force /*= false*/) {
            if (force || this.liveUpdate.oCastBoolean()) {
                if (this.cvar != null) {
                    if (read) {
                        this.text.data.oSet(this.cvar.GetString());
                    } else {
                        this.cvar.SetString(this.text.data.getData());
                        if ((this.cvarMax != 0) && (this.cvar.GetInteger() > this.cvarMax)) {
                            this.cvar.SetInteger(this.cvarMax);
                        }
                    }
                }
            }
        }

        private void UpdateCvar(boolean read) {
            this.UpdateCvar(read, false);
        }

        private void CommonInit() {
            this.maxChars = 128;
            this.numeric = false;
            this.paintOffset = 0;
            this.cursorPos = 0;
            this.cursorLine = 0;
            this.cvarMax = 0;
            this.wrap = false;
            this.sourceFile.oSet("");
            this.scroller = null;
            this.sizeBias = 0;
            this.lastTextLength = 0;
            this.forceScroll = false;
            this.password.data = false;
            this.cvar = null;
            this.liveUpdate.data = true;
            this.readonly = false;

            this.scroller = new idSliderWindow(this.dc, this.gui);
        }

        private void EnsureCursorVisible() {
            if (this.readonly) {
                this.cursorPos = -1;
            } else if (this.maxChars == 1) {
                this.cursorPos = 0;
            }

            if (NOT(this.dc)) {
                return;
            }

            SetFont();
            if (!this.wrap) {
                int cursorX = 0;
                if (this.password.data) {
                    cursorX = this.cursorPos * this.dc.CharWidth('*', this.textScale.data);
                } else {
                    int i = 0;
                    while ((i < this.text.Length()) && (i < this.cursorPos)) {
                        if (idStr.IsColor(ctos(this.text.data.oGet(i)))) {
                            i += 2;
                        } else {
                            cursorX += this.dc.CharWidth(this.text.data.oGet(i), this.textScale.data);
                            i++;
                        }
                    }
                }
                final int maxWidth = (int) GetMaxCharWidth();
                final int left = cursorX - maxWidth;
                final int right = (int) ((cursorX - this.textRect.w) + maxWidth);

                if (this.paintOffset > left) {
                    // When we go past the left side, we want the text to jump 6 characters
                    this.paintOffset = left - (maxWidth * 6);
                }
                if (this.paintOffset < right) {
                    this.paintOffset = right;
                }
                if (this.paintOffset < 0) {
                    this.paintOffset = 0;
                }
                this.scroller.SetRange(0.0f, 0.0f, 1.0f);

            } else {
                // Word wrap

                this.breaks.Clear();
                final idRectangle rect = this.textRect;
                rect.w -= this.sizeBias;
                this.dc.DrawText(this.text.data, this.textScale.data, this.textAlign, colorWhite, rect, true, (itob(this.flags & WIN_FOCUS)) ? this.cursorPos : -1, true, this.breaks);

                final int fit = (int) (this.textRect.h / (GetMaxCharHeight() + 5));
                if (fit < (this.breaks.Num() + 1)) {
                    this.scroller.SetRange(0, (this.breaks.Num() + 1) - fit, 1);
                } else {
                    // The text fits completely in the box
                    this.scroller.SetRange(0.0f, 0.0f, 1.0f);
                }

                if (this.forceScroll) {
                    this.scroller.SetValue(this.breaks.Num() - fit);
                } else if (this.readonly) {
                } else {
                    this.cursorLine = 0;
                    for (int i = 1; i < this.breaks.Num(); i++) {
                        if (this.cursorPos >= this.breaks.oGet(i)) {
                            this.cursorLine = i;
                        } else {
                            break;
                        }
                    }
                    final int topLine = idMath.FtoiFast(this.scroller.GetValue());
                    if (this.cursorLine < topLine) {
                        this.scroller.SetValue(this.cursorLine);
                    } else if (this.cursorLine >= (topLine + fit)) {
                        this.scroller.SetValue((this.cursorLine - fit) + 1);
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
            final String thumbImage = "guis/assets/scrollbar_thumb.tga";
            String barImage = "guis/assets/scrollbarv.tga";
            String scrollerName = "_scrollerWinV";

            if (horizontal) {
                barImage = "guis/assets/scrollbarh.tga";
                scrollerName = "_scrollerWinH";
            }

            final idMaterial mat = declManager.FindMaterial(barImage);
            mat.SetSort(SS_GUI);
            this.sizeBias = mat.GetImageWidth();

            final idRectangle scrollRect = new idRectangle();
            if (horizontal) {
                this.sizeBias = mat.GetImageHeight();
                scrollRect.x = 0;
                scrollRect.y = (this.clientRect.h - this.sizeBias);
                scrollRect.w = this.clientRect.w;
                scrollRect.h = this.sizeBias;
            } else {
                scrollRect.x = (this.clientRect.w - this.sizeBias);
                scrollRect.y = 0;
                scrollRect.w = this.sizeBias;
                scrollRect.h = this.clientRect.h;
            }

            this.scroller.InitWithDefaults(scrollerName, scrollRect, this.foreColor.data, this.matColor.data, mat.GetName(), thumbImage, !horizontal, true);
            InsertChild(this.scroller, null);
            this.scroller.SetBuddy(this);
        }
    }
}
