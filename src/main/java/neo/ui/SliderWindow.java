package neo.ui;

import static neo.Renderer.Material.SS_GUI;
import neo.Renderer.Material.idMaterial;
import static neo.TempDump.NOT;
import static neo.TempDump.isNotNullOrEmpty;
import neo.framework.CVarSystem.idCVar;
import static neo.framework.Common.common;
import static neo.framework.DeclManager.declManager;
import static neo.framework.KeyInput.K_KP_LEFTARROW;
import static neo.framework.KeyInput.K_KP_RIGHTARROW;
import static neo.framework.KeyInput.K_LEFTARROW;
import static neo.framework.KeyInput.K_MOUSE1;
import static neo.framework.KeyInput.K_MOUSE2;
import static neo.framework.KeyInput.K_RIGHTARROW;
import static neo.idlib.Lib.idLib.cvarSystem;
import neo.idlib.Text.Parser.idParser;
import neo.idlib.Text.Str.idStr;
import neo.idlib.math.Vector.idVec4;
import static neo.sys.sys_public.sysEventType_t.SE_KEY;
import neo.sys.sys_public.sysEvent_s;
import neo.ui.DeviceContext.idDeviceContext;
import neo.ui.Rectangle.idRectangle;
import neo.ui.SimpleWindow.drawWin_t;
import neo.ui.UserInterfaceLocal.idUserInterfaceLocal;
import static neo.ui.Window.WIN_CANFOCUS;
import static neo.ui.Window.WIN_CAPTURE;
import static neo.ui.Window.WIN_FOCUS;
import static neo.ui.Window.WIN_HOLDCAPTURE;
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

        private idWinFloat value = new idWinFloat();
        private float low;
        private float high;
        private float thumbWidth;
        private float thumbHeight;
        private float stepSize;
        private float lastValue;
        private idRectangle thumbRect = new idRectangle();
        private idMaterial thumbMat;
        private boolean    vertical;
        private boolean    verticalFlip;
        private boolean    scrollbar;
        private idWindow   buddyWin;
        private idStr    thumbShader = new idStr();
        //	
        private idWinStr cvarStr     = new idWinStr();
        private idCVar  cvar;
        private boolean cvar_init;
        private idWinBool liveUpdate = new idWinBool();
        private idWinStr cvarGroup;
        //
        //

        public idSliderWindow(idUserInterfaceLocal gui) {
            super();
            this.gui = gui;
            super.CommonInit();
            CommonInit();
        }

        public idSliderWindow(idDeviceContext dc, idUserInterfaceLocal gui) {
            super();
            this.dc = dc;
            this.gui = gui;
            super.CommonInit();
            CommonInit();
        }
//	// virtual				~idSliderWindow();

        public void InitWithDefaults(final String _name, final idRectangle _rect, final idVec4 _foreColor, final idVec4 _matColor, final String _background, final String thumbShader, boolean _vertical, boolean _scrollbar) {
            SetInitialState(_name);
            rect.oSet(_rect);
            foreColor.oSet(_foreColor);
            matColor.oSet(_matColor);
            thumbMat = declManager.FindMaterial(thumbShader);
            thumbMat.SetSort(SS_GUI);
            thumbWidth = thumbMat.GetImageWidth();
            thumbHeight = thumbMat.GetImageHeight();
            background = declManager.FindMaterial(_background);
            background.SetSort(SS_GUI);
            vertical = _vertical;
            scrollbar = _scrollbar;
            flags |= WIN_HOLDCAPTURE;
        }

        public void SetRange(float _low, float _high, float _step) {
            low = _low;
            high = _high;
            stepSize = _step;
        }

        public float GetLow() {
            return low;
        }

        public float GetHigh() {
            return high;
        }

        public void SetValue(float _value) {
            value.data = _value;
        }

        public float GetValue() {
            return value.data;
        }

        @Override
        public int/*size_t*/ Allocated() {
            return super.Allocated();
        }

        @Override
        public idWinVar GetWinVarByName(final String _name, boolean winLookup /*= false*/, drawWin_t[] owner /*= NULL*/) {

            if (idStr.Icmp(_name, "value") == 0) {
                return value;
            }
            if (idStr.Icmp(_name, "cvar") == 0) {
                return cvarStr;
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
        public String HandleEvent(final sysEvent_s event, boolean[] updateVisuals) {

            if (!(event.evType == SE_KEY && event.evValue2 != 0)) {
                return "";
            }

            int key = event.evValue;

            if (event.evValue2 != 0 && key == K_MOUSE1) {
                SetCapture(this);
                RouteMouseCoords(0.0f, 0.0f);
                return "";
            }

            if (key == K_RIGHTARROW || key == K_KP_RIGHTARROW || (key == K_MOUSE2 && gui.CursorY() > thumbRect.y)) {
                value.data = value.data + stepSize;
            }

            if (key == K_LEFTARROW || key == K_KP_LEFTARROW || (key == K_MOUSE2 && gui.CursorY() < thumbRect.y)) {
                value.data = value.data - stepSize;
            }

            if (buddyWin != null) {
                buddyWin.HandleBuddyUpdate(this);
            } else {
                gui.SetStateFloat(cvarStr.data.toString(), value.data);
                UpdateCvar(false);
            }

            return "";
        }

        @Override
        public void PostParse() {
            super.PostParse();
            value.data = 0;
            thumbMat = declManager.FindMaterial(thumbShader);
            thumbMat.SetSort(SS_GUI);
            thumbWidth = thumbMat.GetImageWidth();
            thumbHeight = thumbMat.GetImageHeight();
            //vertical = state.GetBool("vertical");
            //scrollbar = state.GetBool("scrollbar");
            flags |= (WIN_HOLDCAPTURE | WIN_CANFOCUS);
            InitCvar();
        }

        @Override
        public void Draw(int time, float x, float y) {
            idVec4 color = foreColor.data;

            if (null == cvar && null == buddyWin) {
                return;
            }

            if (0 == thumbWidth || 0 == thumbHeight) {
                thumbWidth = thumbMat.GetImageWidth();
                thumbHeight = thumbMat.GetImageHeight();
            }

            UpdateCvar(true);
            if (value.data > high) {
                value.data = high;
            } else if (value.data < low) {
                value.data = low;
            }

            float range = high - low;

            if (range <= 0.0f) {
                return;
            }

            float thumbPos = (range != 0) ? (value.data - low) / range : 0;
            if (vertical) {
                if (verticalFlip) {
                    thumbPos = 1.f - thumbPos;
                }
                thumbPos *= drawRect.h - thumbHeight;
                thumbPos += drawRect.y;
                thumbRect.y = thumbPos;
                thumbRect.x = drawRect.x;
            } else {
                thumbPos *= drawRect.w - thumbWidth;
                thumbPos += drawRect.x;
                thumbRect.x = thumbPos;
                thumbRect.y = drawRect.y;
            }
            thumbRect.w = thumbWidth;
            thumbRect.h = thumbHeight;

            if (hover && !noEvents.oCastBoolean() && Contains(gui.CursorX(), gui.CursorY())) {
                color = hoverColor.data;
            } else {
                hover = false;
            }
            if ((flags & WIN_CAPTURE) != 0) {
                color = hoverColor.data;
                hover = true;
            }

            dc.DrawMaterial(thumbRect.x, thumbRect.y, thumbRect.w, thumbRect.h, thumbMat, color);
            if ((flags & WIN_FOCUS) != 0) {
                dc.DrawRect(thumbRect.x + 1.0f, thumbRect.y + 1.0f, thumbRect.w - 2.0f, thumbRect.h - 2.0f, 1.0f, color);
            }
        }

        @Override
        public void DrawBackground(final idRectangle _drawRect) {
            if (null == cvar && null == buddyWin) {
                return;
            }

            if (high - low <= 0.0f) {
                return;
            }

            idRectangle r = _drawRect;
            if (!scrollbar) {
                if (vertical) {
                    r.y += thumbHeight / 2.f;
                    r.h -= thumbHeight;
                } else {
                    r.x += thumbWidth / 2.0;
                    r.w -= thumbWidth;
                }
            }
            super.DrawBackground(r);
        }

        @Override
        public String RouteMouseCoords(float xd, float yd) {
            float pct;

            if (NOT(flags & WIN_CAPTURE)) {
                return "";
            }

            idRectangle r = drawRect;
            r.x = actualX;
            r.y = actualY;
            r.x += thumbWidth / 2.0;
            r.w -= thumbWidth;
            if (vertical) {
                r.y += thumbHeight / 2;
                r.h -= thumbHeight;
                if (gui.CursorY() >= r.y && gui.CursorY() <= r.Bottom()) {
                    pct = (gui.CursorY() - r.y) / r.h;
                    if (verticalFlip) {
                        pct = 1.f - pct;
                    }
                    value.data = low + (high - low) * pct;
                } else if (gui.CursorY() < r.y) {
                    if (verticalFlip) {
                        value.data = high;
                    } else {
                        value.data = low;
                    }
                } else {
                    if (verticalFlip) {
                        value.data = low;
                    } else {
                        value.data = high;
                    }
                }
            } else {
                r.x += thumbWidth / 2;
                r.w -= thumbWidth;
                if (gui.CursorX() >= r.x && gui.CursorX() <= r.Right()) {
                    pct = (gui.CursorX() - r.x) / r.w;
                    value.data = low + (high - low) * pct;
                } else if (gui.CursorX() < r.x) {
                    value.data = low;
                } else {
                    value.data = high;
                }
            }

            if (buddyWin != null) {
                buddyWin.HandleBuddyUpdate(this);
            } else {
                gui.SetStateFloat(cvarStr.data.toString(), value.data);
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
            buddyWin = buddy;
        }

        @Override
        public void RunNamedEvent(final String eventName) {
            idStr event, group;

            if (0 == idStr.Cmpn(eventName, "cvar read ", 10)) {
                event = new idStr(eventName);
                group = new idStr(event.Mid(10, event.Length() - 10));
                if (NOT(group.Cmp(cvarGroup.data))) {
                    UpdateCvar(true, true);
                }
            } else if (0 == idStr.Cmpn(eventName, "cvar write ", 11)) {
                event = new idStr(eventName);
                group = new idStr(event.Mid(11, event.Length() - 11));
                if (NOT(group.Cmp(cvarGroup.data))) {
                    UpdateCvar(false, true);
                }
            }
        }

        @Override
        protected boolean ParseInternalVar(final String _name, idParser src) {
            if (idStr.Icmp(_name, "stepsize") == 0 || idStr.Icmp(_name, "step") == 0) {
                stepSize = src.ParseFloat();
                return true;
            }
            if (idStr.Icmp(_name, "low") == 0) {
                low = src.ParseFloat();
                return true;
            }
            if (idStr.Icmp(_name, "high") == 0) {
                high = src.ParseFloat();
                return true;
            }
            if (idStr.Icmp(_name, "vertical") == 0) {
                vertical = src.ParseBool();
                return true;
            }
            if (idStr.Icmp(_name, "verticalflip") == 0) {
                verticalFlip = src.ParseBool();
                return true;
            }
            if (idStr.Icmp(_name, "scrollbar") == 0) {
                scrollbar = src.ParseBool();
                return true;
            }
            if (idStr.Icmp(_name, "thumbshader") == 0) {
                ParseString(src, thumbShader);
                declManager.FindMaterial(thumbShader);
                return true;
            }
            return super.ParseInternalVar(_name, src);
        }

        @Override
        public void CommonInit() {
            value.data = 0;
            low = 0;
            high = 100.0f;
            stepSize = 1.0f;
            thumbMat = declManager.FindMaterial("_default");
            buddyWin = null;

            cvar = null;
            cvar_init = false;
            liveUpdate.data = true;

            vertical = false;
            scrollbar = false;

            verticalFlip = false;
        }

        private void InitCvar() {
            if (!isNotNullOrEmpty(cvarStr.c_str())) {
                if (null == buddyWin) {
                    common.Warning("idSliderWindow.InitCvar: gui '%s' window '%s' has an empty cvar string", gui.GetSourceFile(), name);
                }
                cvar_init = true;
                cvar = null;
                return;
            }

            cvar = cvarSystem.Find(cvarStr.data.toString());
            if (null == cvar) {
                common.Warning("idSliderWindow.InitCvar: gui '%s' window '%s' references undefined cvar '%s'", gui.GetSourceFile(), name, cvarStr.c_str());
                cvar_init = true;
                return;
            }
        }

        // true: read the updated cvar from cvar system
        // false: write to the cvar system
        // force == true overrides liveUpdate 0
        private void UpdateCvar(boolean read, boolean force /*= false*/) {
            if (buddyWin != null || null == cvar) {
                return;
            }
            if (force || liveUpdate.oCastBoolean()) {
                value.data = cvar.GetFloat();
                if (value.data != gui.State().GetFloat(cvarStr.data.toString())) {
                    if (read) {
                        gui.SetStateFloat(cvarStr.data.toString(), value.data);
                    } else {
                        value.data = gui.State().GetFloat(cvarStr.data.toString());
                        cvar.SetFloat(value.data);
                    }
                }
            }
        }

        private void UpdateCvar(boolean read) {
            this.UpdateCvar(read, false);
        }
    };
}
