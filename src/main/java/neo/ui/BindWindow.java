package neo.ui;

import static neo.TempDump.NOT;
import static neo.framework.Common.common;
import static neo.framework.KeyInput.K_ESCAPE;
import static neo.framework.KeyInput.K_MOUSE1;
import neo.idlib.Text.Str.idStr;
import neo.idlib.math.Vector.idVec4;
import static neo.sys.sys_public.sysEventType_t.SE_KEY;
import neo.sys.sys_public.sysEvent_s;
import neo.ui.DeviceContext.idDeviceContext;
import neo.ui.SimpleWindow.drawWin_t;
import neo.ui.UserInterfaceLocal.idUserInterfaceLocal;
import static neo.ui.Window.WIN_CANFOCUS;
import static neo.ui.Window.WIN_HOLDCAPTURE;
import neo.ui.Window.idWindow;
import neo.ui.Winvar.idWinStr;
import neo.ui.Winvar.idWinVar;

/**
 *
 */
public class BindWindow {

    static class idBindWindow extends idWindow {

        private idWinStr bindName = new idWinStr();
        private boolean waitingOnKey;
        //
        //

        public idBindWindow(idUserInterfaceLocal gui) {
            super();
            this.gui = gui;
            super.CommonInit();
            CommonInit();
        }

        public idBindWindow(idDeviceContext dc, idUserInterfaceLocal gui) {
            super();
            this.dc = dc;
            this.gui = gui;
            super.CommonInit();
            CommonInit();
        }
//	virtual ~idBindWindow();
        private static final StringBuilder ret = new StringBuilder(256);

        @Override
        public String HandleEvent(sysEvent_s event, boolean[] updateVisuals) {

            if (!(event.evType == SE_KEY && event.evValue2 != 0)) {
                return "";
            }

            int key = event.evValue;

            if (waitingOnKey) {
                waitingOnKey = false;
                if (key == K_ESCAPE) {
                    idStr.snPrintf(ret, ret.capacity(), "clearbind \"%s\"", bindName.GetName());
                } else {
                    idStr.snPrintf(ret, ret.capacity(), "bind %d \"%s\"", key, bindName.GetName());
                }
                return ret.toString();
            } else {
                if (key == K_MOUSE1) {
                    waitingOnKey = true;
                    gui.SetBindHandler(this);
                    return "";
                }
            }

            return "";
        }

        @Override
        public void PostParse() {
            super.PostParse();
            bindName.SetGuiInfo(gui.GetStateDict(), bindName.c_str());
            bindName.Update();
            //bindName = state.GetString("bind");
            flags |= (WIN_HOLDCAPTURE | WIN_CANFOCUS);
        }

        @Override
        public void Draw(int time, float x, float y) {
            idVec4 color = foreColor.oCastIdVec4();

            String str;
            if (waitingOnKey) {
                str = common.GetLanguageDict().GetString("#str_07000");
            } else if (bindName.Length() != 0) {
                str = bindName.c_str();
            } else {
                str = common.GetLanguageDict().GetString("#str_07001");
            }

            if (waitingOnKey || (hover && NOT(noEvents) && Contains(gui.CursorX(), gui.CursorY()))) {
                color = hoverColor.oCastIdVec4();
            } else {
                hover = false;
            }

            dc.DrawText(str, textScale.data, textAlign, color, textRect, false, -1);
        }

        @Override
        public int Allocated() {
            return super.Allocated();
        }

        @Override
        public idWinVar GetWinVarByName(String _name, boolean winLookup, drawWin_t[] owner) {

            if (idStr.Icmp(_name, "bind") == 0) {
                return bindName;
            }

            return super.GetWinVarByName(_name, winLookup, owner);
        }

        @Override
        public void Activate(boolean activate, idStr act) {
            super.Activate(activate, act);
            bindName.Update();
        }

        @Override
        public void CommonInit() {
            bindName.data.oSet("");
            waitingOnKey = false;
        }

        @Override
        public void GainFocus() {
            throw new UnsupportedOperationException("Not supported.");
        }

        @Override
        public void LoseFocus() {
            throw new UnsupportedOperationException("Not supported.");
        }

        @Override
        public void GainCapture() {
            throw new UnsupportedOperationException("Not supported.");
        }

        @Override
        public void Sized() {
            throw new UnsupportedOperationException("Not supported.");
        }

        @Override
        public void Moved() {
            throw new UnsupportedOperationException("Not supported.");
        }

        @Override
        public void SetBuddy(idWindow buddy) {
            throw new UnsupportedOperationException("Not supported.");
        }

        @Override
        public void HandleBuddyUpdate(idWindow buddy) {
            throw new UnsupportedOperationException("Not supported.");
        }

        @Override
        public void HasAction() {
            throw new UnsupportedOperationException("Not supported.");
        }

        @Override
        public void HasScripts() {
            throw new UnsupportedOperationException("Not supported.");
        }

    };
}
