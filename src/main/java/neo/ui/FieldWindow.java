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
            float scale = textScale.oCastFloat();
            int len = text.Length();
            cursorPos = gui.State().GetInt(cursorVar.getData());
            if (len != lastTextLength || cursorPos != lastCursorPos) {
                CalcPaintOffset(len);
            }
            idRectangle rect = textRect;
            if (paintOffset >= len) {
                paintOffset = 0;
            }
            if (cursorPos > len) {
                cursorPos = len;
            }
//            dc->DrawText(&text[paintOffset], scale, 0, foreColor, rect, false, ((flags & WIN_FOCUS) || showCursor) ? cursorPos - paintOffset : -1);
            dc.DrawText(text.data.getData().substring(paintOffset), scale, 0, foreColor.data, rect, false, (itob(flags & WIN_FOCUS) || showCursor) ? cursorPos - paintOffset : -1);
        }

        @Override
        protected boolean ParseInternalVar(final String _name, idParser src) {
            if (idStr.Icmp(_name, "cursorvar") == 0) {
                ParseString(src, cursorVar);
                return true;
            }
            if (idStr.Icmp(_name, "showcursor") == 0) {
                showCursor = src.ParseBool();
                return true;
            }
            return super.ParseInternalVar(_name, src);
        }

        private void CommonInit() {
            cursorPos = 0;
            lastTextLength = 0;
            lastCursorPos = 0;
            paintOffset = 0;
            showCursor = false;
        }

        private void CalcPaintOffset(int len) {
            lastCursorPos = cursorPos;
            lastTextLength = len;
            paintOffset = 0;
            int tw = dc.TextWidth(text.data, textScale.data, -1);
            if (tw < textRect.w) {
                return;
            }
            while (tw > textRect.w && len > 0) {
                tw = dc.TextWidth(text.data, textScale.data, --len);
                paintOffset++;
            }
        }
    };
}
