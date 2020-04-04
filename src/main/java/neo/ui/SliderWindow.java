package neo.ui;

import static neo.Renderer.Material.SS_GUI;
import static neo.TempDump.NOT;
import static neo.TempDump.isNotNullOrEmpty;
import static neo.framework.Common.common;
import static neo.framework.DeclManager.declManager;
import static neo.framework.KeyInput.K_KP_LEFTARROW;
import static neo.framework.KeyInput.K_KP_RIGHTARROW;
import static neo.framework.KeyInput.K_LEFTARROW;
import static neo.framework.KeyInput.K_MOUSE1;
import static neo.framework.KeyInput.K_MOUSE2;
import static neo.framework.KeyInput.K_RIGHTARROW;
import static neo.idlib.Lib.idLib.cvarSystem;
import static neo.sys.sys_public.sysEventType_t.SE_KEY;
import static neo.ui.Window.WIN_CANFOCUS;
import static neo.ui.Window.WIN_CAPTURE;
import static neo.ui.Window.WIN_FOCUS;
import static neo.ui.Window.WIN_HOLDCAPTURE;

import neo.Renderer.Material.idMaterial;
import neo.framework.CVarSystem.idCVar;
import neo.idlib.Text.Parser.idParser;
import neo.idlib.Text.Str.idStr;
import neo.idlib.math.Vector.idVec4;
import neo.sys.sys_public.sysEvent_s;
import neo.ui.DeviceContext.idDeviceContext;
import neo.ui.Rectangle.idRectangle;
import neo.ui.SimpleWindow.drawWin_t;
import neo.ui.UserInterfaceLocal.idUserInterfaceLocal;
import neo.ui.Window.idWindow;
import neo.ui.Winvar.idWinBool;
import neo.ui.Winvar.idWinFloat;
import neo.ui.Winvar.idWinStr;
import neo.ui.Winvar.idWinVar;

/**
 *
 */
public class SliderWindow {

    public static class idSliderWindow extends idWindow {

        private final idWinFloat value = new idWinFloat();
        private float low;
        private float high;
        private float thumbWidth;
        private float thumbHeight;
        private float stepSize;
        private float lastValue;
        private final idRectangle thumbRect = new idRectangle();
        private idMaterial thumbMat;
        private boolean    vertical;
        private boolean    verticalFlip;
        private boolean    scrollbar;
        private idWindow   buddyWin;
        private final idStr    thumbShader = new idStr();
        //	
        private final idWinStr cvarStr     = new idWinStr();
        private idCVar  cvar;
        private boolean cvar_init;
        private final idWinBool liveUpdate = new idWinBool();
        private idWinStr cvarGroup;
        //
        //

        public idSliderWindow(idUserInterfaceLocal gui) {
            super(gui);
            this.gui = gui;
            CommonInit();
        }

        public idSliderWindow(idDeviceContext dc, idUserInterfaceLocal gui) {
            super(dc, gui);
            this.dc = dc;
            this.gui = gui;
            CommonInit();
        }
//	// virtual				~idSliderWindow();

        public void InitWithDefaults(final String _name, final idRectangle _rect, final idVec4 _foreColor, final idVec4 _matColor, final String _background, final String thumbShader, boolean _vertical, boolean _scrollbar) {
            SetInitialState(_name);
            this.rect.oSet(_rect);
            this.foreColor.oSet(_foreColor);
            this.matColor.oSet(_matColor);
            this.thumbMat = declManager.FindMaterial(thumbShader);
            this.thumbMat.SetSort(SS_GUI);
            this.thumbWidth = this.thumbMat.GetImageWidth();
            this.thumbHeight = this.thumbMat.GetImageHeight();
            this.background = declManager.FindMaterial(_background);
            this.background.SetSort(SS_GUI);
            this.vertical = _vertical;
            this.scrollbar = _scrollbar;
            this.flags |= WIN_HOLDCAPTURE;
        }

        public void SetRange(float _low, float _high, float _step) {
            this.low = _low;
            this.high = _high;
            this.stepSize = _step;
        }

        public float GetLow() {
            return this.low;
        }

        public float GetHigh() {
            return this.high;
        }

        public void SetValue(float _value) {
            this.value.data = _value;
        }

        public float GetValue() {
            return this.value.data;
        }

        @Override
        public int/*size_t*/ Allocated() {
            return super.Allocated();
        }

        @Override
        public idWinVar GetWinVarByName(final String _name, boolean winLookup /*= false*/, drawWin_t[] owner /*= NULL*/) {

            if (idStr.Icmp(_name, "value") == 0) {
                return this.value;
            }
            if (idStr.Icmp(_name, "cvar") == 0) {
                return this.cvarStr;
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
        public String HandleEvent(final sysEvent_s event, boolean[] updateVisuals) {

            if (!((event.evType == SE_KEY) && (event.evValue2 != 0))) {
                return "";
            }

            final int key = event.evValue;

            if ((event.evValue2 != 0) && (key == K_MOUSE1)) {
                SetCapture(this);
                RouteMouseCoords(0.0f, 0.0f);
                return "";
            }

            if ((key == K_RIGHTARROW) || (key == K_KP_RIGHTARROW) || ((key == K_MOUSE2) && (this.gui.CursorY() > this.thumbRect.y))) {
                this.value.data = this.value.data + this.stepSize;
            }

            if ((key == K_LEFTARROW) || (key == K_KP_LEFTARROW) || ((key == K_MOUSE2) && (this.gui.CursorY() < this.thumbRect.y))) {
                this.value.data = this.value.data - this.stepSize;
            }

            if (this.buddyWin != null) {
                this.buddyWin.HandleBuddyUpdate(this);
            } else {
                this.gui.SetStateFloat(this.cvarStr.data.getData(), this.value.data);
                UpdateCvar(false);
            }

            return "";
        }

        @Override
        public void PostParse() {
            super.PostParse();
            this.value.data = 0;
            this.thumbMat = declManager.FindMaterial(this.thumbShader);
            this.thumbMat.SetSort(SS_GUI);
            this.thumbWidth = this.thumbMat.GetImageWidth();
            this.thumbHeight = this.thumbMat.GetImageHeight();
            //vertical = state.GetBool("vertical");
            //scrollbar = state.GetBool("scrollbar");
            this.flags |= (WIN_HOLDCAPTURE | WIN_CANFOCUS);
            InitCvar();
        }

        @Override
        public void Draw(int time, float x, float y) {
            final idVec4 color = this.foreColor.data;

            if ((null == this.cvar) && (null == this.buddyWin)) {
                return;
            }

            if ((0 == this.thumbWidth) || (0 == this.thumbHeight)) {
                this.thumbWidth = this.thumbMat.GetImageWidth();
                this.thumbHeight = this.thumbMat.GetImageHeight();
            }

            UpdateCvar(true);
            if (this.value.data > this.high) {
                this.value.data = this.high;
            } else if (this.value.data < this.low) {
                this.value.data = this.low;
            }

            final float range = this.high - this.low;

            if (range <= 0.0f) {
                return;
            }

            float thumbPos = (range != 0) ? (this.value.data - this.low) / range : 0;
            if (this.vertical) {
                if (this.verticalFlip) {
                    thumbPos = 1.f - thumbPos;
                }
                thumbPos *= this.drawRect.h - this.thumbHeight;
                thumbPos += this.drawRect.y;
                this.thumbRect.y = thumbPos;
                this.thumbRect.x = this.drawRect.x;
            } else {
                thumbPos *= this.drawRect.w - this.thumbWidth;
                thumbPos += this.drawRect.x;
                this.thumbRect.x = thumbPos;
                this.thumbRect.y = this.drawRect.y;
            }
            this.thumbRect.w = this.thumbWidth;
            this.thumbRect.h = this.thumbHeight;

            if (this.hover && !this.noEvents.oCastBoolean() && Contains(this.gui.CursorX(), this.gui.CursorY())) {
                color.oSet(this.hoverColor.data);
            } else {
                this.hover = false;
            }
            if ((this.flags & WIN_CAPTURE) != 0) {
                color.oSet(this.hoverColor.data);
                this.hover = true;
            }

            this.dc.DrawMaterial(this.thumbRect.x, this.thumbRect.y, this.thumbRect.w, this.thumbRect.h, this.thumbMat, color);
            if ((this.flags & WIN_FOCUS) != 0) {
                this.dc.DrawRect(this.thumbRect.x + 1.0f, this.thumbRect.y + 1.0f, this.thumbRect.w - 2.0f, this.thumbRect.h - 2.0f, 1.0f, color);
            }
        }

        @Override
        public void DrawBackground(final idRectangle _drawRect) {
            if ((null == this.cvar) && (null == this.buddyWin)) {
                return;
            }

            if ((this.high - this.low) <= 0.0f) {
                return;
            }

            final idRectangle r = _drawRect;
            if (!this.scrollbar) {
                if (this.vertical) {
                    r.y += this.thumbHeight / 2.f;
                    r.h -= this.thumbHeight;
                } else {
                    r.x += this.thumbWidth / 2.0;
                    r.w -= this.thumbWidth;
                }
            }
            super.DrawBackground(r);
        }

        @Override
        public String RouteMouseCoords(float xd, float yd) {
            float pct;

            if (NOT(this.flags & WIN_CAPTURE)) {
                return "";
            }

            final idRectangle r = this.drawRect;
            r.x = this.actualX;
            r.y = this.actualY;
            r.x += this.thumbWidth / 2.0;
            r.w -= this.thumbWidth;
            if (this.vertical) {
                r.y += this.thumbHeight / 2;
                r.h -= this.thumbHeight;
                if ((this.gui.CursorY() >= r.y) && (this.gui.CursorY() <= r.Bottom())) {
                    pct = (this.gui.CursorY() - r.y) / r.h;
                    if (this.verticalFlip) {
                        pct = 1.f - pct;
                    }
                    this.value.data = this.low + ((this.high - this.low) * pct);
                } else if (this.gui.CursorY() < r.y) {
                    if (this.verticalFlip) {
                        this.value.data = this.high;
                    } else {
                        this.value.data = this.low;
                    }
                } else {
                    if (this.verticalFlip) {
                        this.value.data = this.low;
                    } else {
                        this.value.data = this.high;
                    }
                }
            } else {
                r.x += this.thumbWidth / 2;
                r.w -= this.thumbWidth;
                if ((this.gui.CursorX() >= r.x) && (this.gui.CursorX() <= r.Right())) {
                    pct = (this.gui.CursorX() - r.x) / r.w;
                    this.value.data = this.low + ((this.high - this.low) * pct);
                } else if (this.gui.CursorX() < r.x) {
                    this.value.data = this.low;
                } else {
                    this.value.data = this.high;
                }
            }

            if (this.buddyWin != null) {
                this.buddyWin.HandleBuddyUpdate(this);
            } else {
                this.gui.SetStateFloat(this.cvarStr.data.getData(), this.value.data);
            }
            UpdateCvar(false);

            return "";
        }

        @Override
        public void Activate(boolean activate, idStr act) {
            super.Activate(activate, act);
            if (activate) {
                UpdateCvar(true, true);
            }
        }

        @Override
        public void SetBuddy(idWindow buddy) {
            this.buddyWin = buddy;
        }

        @Override
        public void RunNamedEvent(final String eventName) {
            idStr event, group;

            if (0 == idStr.Cmpn(eventName, "cvar read ", 10)) {
                event = new idStr(eventName);
                group = new idStr(event.Mid(10, event.Length() - 10));
                if (NOT(group.Cmp(this.cvarGroup.data))) {
                    UpdateCvar(true, true);
                }
            } else if (0 == idStr.Cmpn(eventName, "cvar write ", 11)) {
                event = new idStr(eventName);
                group = new idStr(event.Mid(11, event.Length() - 11));
                if (NOT(group.Cmp(this.cvarGroup.data))) {
                    UpdateCvar(false, true);
                }
            }
        }

        @Override
        protected boolean ParseInternalVar(final String _name, idParser src) {
            if ((idStr.Icmp(_name, "stepsize") == 0) || (idStr.Icmp(_name, "step") == 0)) {
                this.stepSize = src.ParseFloat();
                return true;
            }
            if (idStr.Icmp(_name, "low") == 0) {
                this.low = src.ParseFloat();
                return true;
            }
            if (idStr.Icmp(_name, "high") == 0) {
                this.high = src.ParseFloat();
                return true;
            }
            if (idStr.Icmp(_name, "vertical") == 0) {
                this.vertical = src.ParseBool();
                return true;
            }
            if (idStr.Icmp(_name, "verticalflip") == 0) {
                this.verticalFlip = src.ParseBool();
                return true;
            }
            if (idStr.Icmp(_name, "scrollbar") == 0) {
                this.scrollbar = src.ParseBool();
                return true;
            }
            if (idStr.Icmp(_name, "thumbshader") == 0) {
                ParseString(src, this.thumbShader);
                declManager.FindMaterial(this.thumbShader);
                return true;
            }
            return super.ParseInternalVar(_name, src);
        }

        private void CommonInit() {
            this.value.data = 0;
            this.low = 0;
            this.high = 100.0f;
            this.stepSize = 1.0f;
            this.thumbMat = declManager.FindMaterial("_default");
            this.buddyWin = null;

            this.cvar = null;
            this.cvar_init = false;
            this.liveUpdate.data = true;

            this.vertical = false;
            this.scrollbar = false;

            this.verticalFlip = false;
        }

        private void InitCvar() {
            if (!isNotNullOrEmpty(this.cvarStr.c_str())) {
                if (null == this.buddyWin) {
                    common.Warning("idSliderWindow.InitCvar: gui '%s' window '%s' has an empty cvar string", this.gui.GetSourceFile(), this.name);
                }
                this.cvar_init = true;
                this.cvar = null;
                return;
            }

            this.cvar = cvarSystem.Find(this.cvarStr.data.getData());
            if (null == this.cvar) {
                common.Warning("idSliderWindow.InitCvar: gui '%s' window '%s' references undefined cvar '%s'", this.gui.GetSourceFile(), this.name, this.cvarStr.c_str());
                this.cvar_init = true;
                return;
            }
        }

        // true: read the updated cvar from cvar system
        // false: write to the cvar system
        // force == true overrides liveUpdate 0
        private void UpdateCvar(boolean read, boolean force /*= false*/) {
            if ((this.buddyWin != null) || (null == this.cvar)) {
                return;
            }
            if (force || this.liveUpdate.oCastBoolean()) {
                this.value.data = this.cvar.GetFloat();
                if (this.value.data != this.gui.State().GetFloat(this.cvarStr.data.getData())) {
                    if (read) {
                        this.gui.SetStateFloat(this.cvarStr.data.getData(), this.value.data);
                    } else {
                        this.value.data = this.gui.State().GetFloat(this.cvarStr.data.getData());
                        this.cvar.SetFloat(this.value.data);
                    }
                }
            }
        }

        private void UpdateCvar(boolean read) {
            this.UpdateCvar(read, false);
        }
    }
}
