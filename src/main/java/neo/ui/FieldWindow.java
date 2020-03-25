package neo.ui;

import static neo.TempDump.itob;
import static neo.ui.Window.WIN_FOCUS;

import neo.idlib.Text.Parser.idParser;
import neo.idlib.Text.Str.idStr;
import neo.ui.DeviceContext.idDeviceContext;
import neo.ui.Rectangle.idRectangle;
import neo.ui.UserInterfaceLocal.idUserInterfaceLocal;
import neo.ui.Window.idWindow;

/**
 *
 */
public class FieldWindow {

    static class idFieldWindow extends idWindow {

        private int cursorPos;
        private int lastTextLength;
        private int lastCursorPos;
        private int paintOffset;
        private boolean showCursor;
        private idStr cursorVar;
        //
        //

        public idFieldWindow(idUserInterfaceLocal gui) {
            super(gui);
            this.gui = gui;
            CommonInit();
        }

        public idFieldWindow(idDeviceContext dc, idUserInterfaceLocal gui) {
            super(dc, gui);
            this.dc = dc;
            this.gui = gui;
            CommonInit();
        }
        //virtual ~idFieldWindow();

        @Override
        public void Draw(int time, float x, float y) {
            final float scale = this.textScale.oCastFloat();
            final int len = this.text.Length();
            this.cursorPos = this.gui.State().GetInt(this.cursorVar.getData());
            if ((len != this.lastTextLength) || (this.cursorPos != this.lastCursorPos)) {
                CalcPaintOffset(len);
            }
            final idRectangle rect = this.textRect;
            if (this.paintOffset >= len) {
                this.paintOffset = 0;
            }
            if (this.cursorPos > len) {
                this.cursorPos = len;
            }
//            dc->DrawText(&text[paintOffset], scale, 0, foreColor, rect, false, ((flags & WIN_FOCUS) || showCursor) ? cursorPos - paintOffset : -1);
            this.dc.DrawText(this.text.data.getData().substring(this.paintOffset), scale, 0, this.foreColor.data, rect, false, (itob(this.flags & WIN_FOCUS) || this.showCursor) ? this.cursorPos - this.paintOffset : -1);
        }

        @Override
        protected boolean ParseInternalVar(final String _name, idParser src) {
            if (idStr.Icmp(_name, "cursorvar") == 0) {
                ParseString(src, this.cursorVar);
                return true;
            }
            if (idStr.Icmp(_name, "showcursor") == 0) {
                this.showCursor = src.ParseBool();
                return true;
            }
            return super.ParseInternalVar(_name, src);
        }

        private void CommonInit() {
            this.cursorPos = 0;
            this.lastTextLength = 0;
            this.lastCursorPos = 0;
            this.paintOffset = 0;
            this.showCursor = false;
        }

        private void CalcPaintOffset(int len) {
            this.lastCursorPos = this.cursorPos;
            this.lastTextLength = len;
            this.paintOffset = 0;
            int tw = this.dc.TextWidth(this.text.data, this.textScale.data, -1);
            if (tw < this.textRect.w) {
                return;
            }
            while ((tw > this.textRect.w) && (len > 0)) {
                tw = this.dc.TextWidth(this.text.data, this.textScale.data, --len);
                this.paintOffset++;
            }
        }
    }
}
