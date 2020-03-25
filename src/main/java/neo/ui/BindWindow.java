package neo.ui;

import static neo.TempDump.NOT;
import static neo.framework.Common.common;
import static neo.framework.KeyInput.K_ESCAPE;
import static neo.framework.KeyInput.K_MOUSE1;
import static neo.sys.sys_public.sysEventType_t.SE_KEY;
import static neo.ui.Window.WIN_CANFOCUS;
import static neo.ui.Window.WIN_HOLDCAPTURE;

import neo.idlib.Text.Str.idStr;
import neo.idlib.math.Vector.idVec4;
import neo.sys.sys_public.sysEvent_s;
import neo.ui.DeviceContext.idDeviceContext;
import neo.ui.SimpleWindow.drawWin_t;
import neo.ui.UserInterfaceLocal.idUserInterfaceLocal;
import neo.ui.Window.idWindow;
import neo.ui.Winvar.idWinStr;
import neo.ui.Winvar.idWinVar;

/**
 *
 */
public class BindWindow {

    static class idBindWindow extends idWindow {

        private final idWinStr bindName = new idWinStr();
        private boolean waitingOnKey;
        //
        //

        public idBindWindow(idUserInterfaceLocal gui) {
            super(gui);
            this.gui = gui;
            CommonInit();
        }

        public idBindWindow(idDeviceContext dc, idUserInterfaceLocal gui) {
            super(dc, gui);
            this.dc = dc;
            this.gui = gui;
            CommonInit();
        }
//	virtual ~idBindWindow();
        private static final StringBuilder ret = new StringBuilder(256);

        @Override
        public String HandleEvent(sysEvent_s event, boolean[] updateVisuals) {

            if (!((event.evType == SE_KEY) && (event.evValue2 != 0))) {
                return "";
            }

            final int key = event.evValue;

            if (this.waitingOnKey) {
                this.waitingOnKey = false;
                if (key == K_ESCAPE) {
                    idStr.snPrintf(ret, ret.capacity(), "clearbind \"%s\"", this.bindName.GetName());
                } else {
                    idStr.snPrintf(ret, ret.capacity(), "bind %d \"%s\"", key, this.bindName.GetName());
                }
                return ret.toString();
            } else {
                if (key == K_MOUSE1) {
                    this.waitingOnKey = true;
                    this.gui.SetBindHandler(this);
                    return "";
                }
            }

            return "";
        }

        @Override
        public void PostParse() {
            super.PostParse();
            this.bindName.SetGuiInfo(this.gui.GetStateDict(), this.bindName.c_str());
            this.bindName.Update();
            //bindName = state.GetString("bind");
            this.flags |= (WIN_HOLDCAPTURE | WIN_CANFOCUS);
        }

        @Override
        public void Draw(int time, float x, float y) {
            idVec4 color = this.foreColor.oCastIdVec4();

            String str;
            if (this.waitingOnKey) {
                str = common.GetLanguageDict().GetString("#str_07000");
            } else if (this.bindName.Length() != 0) {
                str = this.bindName.c_str();
            } else {
                str = common.GetLanguageDict().GetString("#str_07001");
            }

            if (this.waitingOnKey || (this.hover && NOT(this.noEvents) && Contains(this.gui.CursorX(), this.gui.CursorY()))) {
                color = this.hoverColor.oCastIdVec4();
            } else {
                this.hover = false;
            }

            this.dc.DrawText(str, this.textScale.data, this.textAlign, color, this.textRect, false, -1);
        }

        @Override
        public int Allocated() {
            return super.Allocated();
        }

        @Override
        public idWinVar GetWinVarByName(String _name, boolean winLookup, drawWin_t[] owner) {

            if (idStr.Icmp(_name, "bind") == 0) {
                return this.bindName;
            }

            return super.GetWinVarByName(_name, winLookup, owner);
        }

        @Override
        public void Activate(boolean activate, idStr act) {
            super.Activate(activate, act);
            this.bindName.Update();
        }

        private void CommonInit() {
            this.bindName.data.oSet("");
            this.waitingOnKey = false;
        }
    }
}
