package neo.ui;

import static neo.Renderer.Material.MF_DEFAULTED;
import static neo.Renderer.Material.SS_GUI;
import static neo.TempDump.etoi;
import static neo.TempDump.isNotNullOrEmpty;
import static neo.framework.Common.common;
import static neo.framework.DeclManager.declManager;
import static neo.framework.KeyInput.K_CTRL;
import static neo.framework.KeyInput.K_DOWNARROW;
import static neo.framework.KeyInput.K_ENTER;
import static neo.framework.KeyInput.K_KP_ENTER;
import static neo.framework.KeyInput.K_MOUSE1;
import static neo.framework.KeyInput.K_MOUSE2;
import static neo.framework.KeyInput.K_MWHEELDOWN;
import static neo.framework.KeyInput.K_MWHEELUP;
import static neo.framework.KeyInput.K_PGDN;
import static neo.framework.KeyInput.K_PGUP;
import static neo.framework.KeyInput.K_UPARROW;
import static neo.idlib.Lib.idLib.sys;
import static neo.idlib.Text.Lexer.LEXFL_NOFATALERRORS;
import static neo.idlib.Text.Lexer.LEXFL_NOSTRINGCONCAT;
import static neo.idlib.Text.Lexer.LEXFL_NOSTRINGESCAPECHARS;
import static neo.idlib.Text.Str.va;
import static neo.sys.sys_public.sysEventType_t.SE_CHAR;
import static neo.sys.sys_public.sysEventType_t.SE_KEY;
import static neo.ui.DeviceContext.idDeviceContext.ALIGN.ALIGN_CENTER;
import static neo.ui.DeviceContext.idDeviceContext.ALIGN.ALIGN_LEFT;
import static neo.ui.DeviceContext.idDeviceContext.ALIGN.ALIGN_RIGHT;
import static neo.ui.Window.MAX_LIST_ITEMS;
import static neo.ui.Window.WIN_CANFOCUS;
import static neo.ui.Window.WIN_FOCUS;
import static neo.ui.Window.idWindow.ON.ON_ENTER;

import neo.Renderer.Material.idMaterial;
import neo.framework.KeyInput.idKeyInput;
import neo.idlib.Text.Parser.idParser;
import neo.idlib.Text.Str.idStr;
import neo.idlib.Text.Token.idToken;
import neo.idlib.containers.HashTable.idHashTable;
import neo.idlib.containers.List.idList;
import neo.idlib.containers.StrList.idStrList;
import neo.idlib.math.Vector.idVec2;
import neo.idlib.math.Vector.idVec4;
import neo.sys.sys_public.sysEvent_s;
import neo.ui.DeviceContext.idDeviceContext;
import neo.ui.Rectangle.idRectangle;
import neo.ui.SimpleWindow.drawWin_t;
import neo.ui.SliderWindow.idSliderWindow;
import neo.ui.UserInterfaceLocal.idUserInterfaceLocal;
import neo.ui.Window.idWindow;
import neo.ui.Winvar.idWinVar;

/**
 *
 */
public class ListWindow {

    // Number of pixels above the text that the rect starts
    static final int pixelOffset = 3;
//    
    // number of pixels between columns
    static final int tabBorder = 4;
//    
    // Time in milliseconds between clicks to register as a double-click
    static final int doubleClickSpeed = 300;
//    
    // enum {
    public static final int TAB_TYPE_TEXT = 0;
    public static final int TAB_TYPE_ICON = 1;
// };

    public static class idTabRect {

        int x;
        int w;
        int align;
        int valign;
        int type;
        idVec2 iconSize = new idVec2();
        float iconVOffset;
    }

    public static class idListWindow extends idWindow {

        private final idList<idTabRect> tabInfo = new idList<>();
        private int top;
        private float sizeBias;
        private boolean horizontal;
        private final idStr tabStopStr = new idStr();
        private final idStr tabAlignStr = new idStr();
        private final idStr tabVAlignStr = new idStr();
        private final idStr tabTypeStr = new idStr();
        private final idStr tabIconSizeStr = new idStr();
        private final idStr tabIconVOffsetStr = new idStr();
        private final idHashTable< idMaterial> iconMaterials = new idHashTable<>();
        private boolean multipleSel;
        //
        private final idStrList listItems = new idStrList();
        private idSliderWindow scroller;
        private final idList<Integer> currentSel = new idList<>();
        private final idStr listName = new idStr();
        //
        private int clickTime;
        //
        private int typedTime;
        private final idStr typed = new idStr();
        //
        //

        public idListWindow(idUserInterfaceLocal gui) {
            super(gui);
            this.gui = gui;
            CommonInit();
        }

        public idListWindow(idDeviceContext dc, idUserInterfaceLocal gui) {
            super(dc, gui);
            this.dc = dc;
            this.gui = gui;
            CommonInit();
        }

        @Override
        public String HandleEvent(final sysEvent_s event, boolean[] updateVisuals) {
            // need to call this to allow proper focus and capturing on embedded children
            final String ret = super.HandleEvent(event, updateVisuals);

            final float vert = GetMaxCharHeight();
            final int numVisibleLines = (int) (this.textRect.h / vert);

            int key = event.evValue;

            if (event.evType == SE_KEY) {
                if (0 == event.evValue2) {
                    // We only care about key down, not up
                    return ret;
                }

                if ((key == K_MOUSE1) || (key == K_MOUSE2)) {
                    // If the user clicked in the scroller, then ignore it
                    if (this.scroller.Contains(this.gui.CursorX(), this.gui.CursorY())) {
                        return ret;
                    }
                }

                if (((key == K_ENTER) || (key == K_KP_ENTER))) {
                    RunScript(etoi(ON_ENTER));
                    return this.cmd.toString();
                }

                if (key == K_MWHEELUP) {
                    key = K_UPARROW;
                } else if (key == K_MWHEELDOWN) {
                    key = K_DOWNARROW;
                }

                if (key == K_MOUSE1) {
                    if (Contains(this.gui.CursorX(), this.gui.CursorY())) {
                        final int cur = (int) ((this.gui.CursorY() - this.actualY - pixelOffset) / vert) + this.top;
                        if ((cur >= 0) && (cur < this.listItems.Num())) {
                            if (this.multipleSel && idKeyInput.IsDown(K_CTRL)) {
                                if (IsSelected(cur)) {
                                    ClearSelection(cur);
                                } else {
                                    AddCurrentSel(cur);
                                }
                            } else {
                                if (IsSelected(cur) && (this.gui.GetTime() < (this.clickTime + doubleClickSpeed))) {
                                    // Double-click causes ON_ENTER to get run
                                    RunScript(etoi(ON_ENTER));
                                    return this.cmd.toString();
                                }
                                SetCurrentSel(cur);

                                this.clickTime = this.gui.GetTime();
                            }
                        } else {
                            SetCurrentSel(this.listItems.Num() - 1);
                        }
                    }
                } else if ((key == K_UPARROW) || (key == K_PGUP) || (key == K_DOWNARROW) || (key == K_PGDN)) {
                    int numLines = 1;

                    if ((key == K_PGUP) || (key == K_PGDN)) {
                        numLines = numVisibleLines / 2;
                    }

                    if ((key == K_UPARROW) || (key == K_PGUP)) {
                        numLines = -numLines;
                    }

                    if (idKeyInput.IsDown(K_CTRL)) {
                        this.top += numLines;
                    } else {
                        SetCurrentSel(GetCurrentSel() + numLines);
                    }
                } else {
                    return ret;
                }
            } else if (event.evType == SE_CHAR) {
                if (!idStr.CharIsPrintable(key)) {
                    return ret;
                }

                if (this.gui.GetTime() > (this.typedTime + 1000)) {
                    this.typed.oSet("");
                }
                this.typedTime = this.gui.GetTime();
                this.typed.Append((char) key);

                for (int i = 0; i < this.listItems.Num(); i++) {
                    if (idStr.Icmpn(this.typed, this.listItems.oGet(i), this.typed.Length()) == 0) {
                        SetCurrentSel(i);
                        break;
                    }
                }

            } else {
                return ret;
            }

            if (GetCurrentSel() < 0) {
                SetCurrentSel(0);
            }

            if (GetCurrentSel() >= this.listItems.Num()) {
                SetCurrentSel(this.listItems.Num() - 1);
            }

            if (this.scroller.GetHigh() > 0.0f) {
                if (!idKeyInput.IsDown(K_CTRL)) {
                    if (this.top > (GetCurrentSel() - 1)) {
                        this.top = GetCurrentSel() - 1;
                    }
                    if (this.top < ((GetCurrentSel() - numVisibleLines) + 2)) {
                        this.top = (GetCurrentSel() - numVisibleLines) + 2;
                    }
                }

                if (this.top > (this.listItems.Num() - 2)) {
                    this.top = this.listItems.Num() - 2;
                }
                if (this.top < 0) {
                    this.top = 0;
                }
                this.scroller.SetValue(this.top);
            } else {
                this.top = 0;
                this.scroller.SetValue(0.0f);
            }

            if (key != K_MOUSE1) {
                // Send a fake mouse click event so onAction gets run in our parents
                final sysEvent_s ev = sys.GenerateMouseButtonEvent(1, true);
                super.HandleEvent(ev, updateVisuals);
            }

            if (this.currentSel.Num() > 0) {
                for (int i = 0; i < this.currentSel.Num(); i++) {
                    this.gui.SetStateInt(va("%s_sel_%d", this.listName, i), this.currentSel.oGet(i));
                }
            } else {
                this.gui.SetStateInt(va("%s_sel_0", this.listName), 0);
            }
            this.gui.SetStateInt(va("%s_numsel", this.listName), this.currentSel.Num());

            return ret;
        }

        @Override
        public void PostParse() {
            super.PostParse();

            InitScroller(this.horizontal);

            final idList<Integer> tabStops = new idList<>();
            final idList<Integer> tabAligns = new idList<>();
            if (this.tabStopStr.Length() != 0) {
                final idParser src = new idParser(this.tabStopStr.toString(), this.tabStopStr.Length(), "tabstops", LEXFL_NOFATALERRORS | LEXFL_NOSTRINGCONCAT | LEXFL_NOSTRINGESCAPECHARS);
                final idToken tok = new idToken();
                while (src.ReadToken(tok)) {
                    if (tok.equals(",")) {
                        continue;
                    }
                    tabStops.Append(Integer.parseInt(tok.toString()));
                }
            }
            if (this.tabAlignStr.Length() != 0) {
                final idParser src = new idParser(this.tabAlignStr.toString(), this.tabAlignStr.Length(), "tabaligns", LEXFL_NOFATALERRORS | LEXFL_NOSTRINGCONCAT | LEXFL_NOSTRINGESCAPECHARS);
                final idToken tok = new idToken();
                while (src.ReadToken(tok)) {
                    if (tok.equals(",")) {
                        continue;
                    }
                    tabAligns.Append(Integer.parseInt(tok.toString()));
                }
            }
            final idList<Integer> tabVAligns = new idList<>();
            if (this.tabVAlignStr.Length() != 0) {
                final idParser src = new idParser(this.tabVAlignStr.toString(), this.tabVAlignStr.Length(), "tabvaligns", LEXFL_NOFATALERRORS | LEXFL_NOSTRINGCONCAT | LEXFL_NOSTRINGESCAPECHARS);
                final idToken tok = new idToken();
                while (src.ReadToken(tok)) {
                    if (tok.equals(",")) {
                        continue;
                    }
                    tabVAligns.Append(Integer.parseInt(tok.toString()));
                }
            }

            final idList<Integer> tabTypes = new idList<>();
            if (this.tabTypeStr.Length() != 0) {
                final idParser src = new idParser(this.tabTypeStr.toString(), this.tabTypeStr.Length(), "tabtypes", LEXFL_NOFATALERRORS | LEXFL_NOSTRINGCONCAT | LEXFL_NOSTRINGESCAPECHARS);
                final idToken tok = new idToken();
                while (src.ReadToken(tok)) {
                    if (tok.equals(",")) {
                        continue;
                    }
                    tabTypes.Append(Integer.parseInt(tok.toString()));
                }
            }
            final idList<idVec2> tabSizes = new idList<>();
            if (this.tabIconSizeStr.Length() != 0) {
                final idParser src = new idParser(this.tabIconSizeStr.toString(), this.tabIconSizeStr.Length(), "tabiconsizes", LEXFL_NOFATALERRORS | LEXFL_NOSTRINGCONCAT | LEXFL_NOSTRINGESCAPECHARS);
                final idToken tok = new idToken();
                while (src.ReadToken(tok)) {
                    if (tok.equals(",")) {
                        continue;
                    }
                    final idVec2 size = new idVec2();
                    size.x = Integer.parseInt(tok.toString());

                    src.ReadToken(tok);	//","
                    src.ReadToken(tok);

                    size.y = Integer.parseInt(tok.toString());
                    tabSizes.Append(size);
                }
            }

            final idList<Float> tabIconVOffsets = new idList<>();
            if (this.tabIconVOffsetStr.Length() != 0) {
                final idParser src = new idParser(this.tabIconVOffsetStr.toString(), this.tabIconVOffsetStr.Length(), "tabiconvoffsets", LEXFL_NOFATALERRORS | LEXFL_NOSTRINGCONCAT | LEXFL_NOSTRINGESCAPECHARS);
                final idToken tok = new idToken();
                while (src.ReadToken(tok)) {
                    if (tok.equals(",")) {
                        continue;
                    }
                    tabIconVOffsets.Append(Float.parseFloat(tok.toString()));
                }
            }

            final int c = tabStops.Num();
            final boolean doAligns = (tabAligns.Num() == tabStops.Num());
            for (int i = 0; i < c; i++) {
                final idTabRect r = new idTabRect();
                r.x = tabStops.oGet(i);
                r.w = (i < (c - 1)) ? tabStops.oGet(i + 1) - r.x - tabBorder : -1;
                r.align = (doAligns) ? tabAligns.oGet(i) : 0;
                if (tabVAligns.Num() > 0) {
                    r.valign = tabVAligns.oGet(i);
                } else {
                    r.valign = 0;
                }
                if (tabTypes.Num() > 0) {
                    r.type = tabTypes.oGet(i);
                } else {
                    r.type = TAB_TYPE_TEXT;
                }
                if (tabSizes.Num() > 0) {
                    r.iconSize = tabSizes.oGet(i);
                } else {
                    r.iconSize.Zero();
                }
                if (tabIconVOffsets.Num() > 0) {
                    r.iconVOffset = tabIconVOffsets.oGet(i);
                } else {
                    r.iconVOffset = 0;
                }
                this.tabInfo.Append(r);
            }
            this.flags |= WIN_CANFOCUS;
        }

        @Override
        public void Draw(int time, float x, float y) {
            idVec4 color;
            final idStr work = new idStr();
            final int count = this.listItems.Num();
            final idRectangle rect = this.textRect;
            final float scale = this.textScale.data;
            final float lineHeight = GetMaxCharHeight();

            float bottom = this.textRect.Bottom();
            float width = this.textRect.w;

            if (this.scroller.GetHigh() > 0.0f) {
                if (this.horizontal) {
                    bottom -= this.sizeBias;
                } else {
                    width -= this.sizeBias;
                    rect.w = width;
                }
            }

            if (this.noEvents.oCastBoolean() || !Contains(this.gui.CursorX(), this.gui.CursorY())) {
                this.hover = false;
            }

            for (int i = this.top; i < count; i++) {
                if (IsSelected(i)) {
                    rect.h = lineHeight;
                    this.dc.DrawFilledRect(rect.x, rect.y + pixelOffset, rect.w, rect.h, this.borderColor.data);
                    if ((this.flags & WIN_FOCUS) != 0) {
                        final idVec4 color2 = this.borderColor.data;
                        color2.w = 1.0f;
                        this.dc.DrawRect(rect.x, rect.y + pixelOffset, rect.w, rect.h, 1.0f, color2);
                    }
                }
                rect.y++;
                rect.h = lineHeight - 1;
                if (this.hover && !this.noEvents.oCastBoolean() && Contains(rect, this.gui.CursorX(), this.gui.CursorY())) {
                    color = this.hoverColor.data;
                } else {
                    color = this.foreColor.data;
                }
                rect.h = lineHeight + pixelOffset;
                rect.y--;

                if (this.tabInfo.Num() > 0) {
                    int start = 0;
                    int tab = 0;
                    int stop = this.listItems.oGet(i).Find('\t', 0);
                    while (start < this.listItems.oGet(i).Length()) {
                        if (tab >= this.tabInfo.Num()) {
                            common.Warning("idListWindow::Draw: gui '%s' window '%s' tabInfo.Num() exceeded", this.gui.GetSourceFile(), this.name);
                            break;
                        }
                        this.listItems.oGet(i).Mid(start, stop - start, work);

                        rect.x = this.textRect.x + this.tabInfo.oGet(tab).x;
                        rect.w = (this.tabInfo.oGet(tab).w == -1) ? width - this.tabInfo.oGet(tab).x : this.tabInfo.oGet(tab).w;
                        this.dc.PushClipRect(rect);

                        if (this.tabInfo.oGet(tab).type == TAB_TYPE_TEXT) {
                            this.dc.DrawText(work, scale, this.tabInfo.oGet(tab).align, color, rect, false, -1);
                        } else if (this.tabInfo.oGet(tab).type == TAB_TYPE_ICON) {

                            final idMaterial[] hashMat = {null};
                            idMaterial iconMat;

                            // leaving the icon name empty doesn't draw anything
                            if (isNotNullOrEmpty(work)) {

                                if (this.iconMaterials.Get(work.toString(), hashMat) == false) {
                                    iconMat = declManager.FindMaterial("_default");
                                } else {
                                    iconMat = hashMat[0];
                                }

                                final idRectangle iconRect = new idRectangle();
                                iconRect.w = this.tabInfo.oGet(tab).iconSize.x;
                                iconRect.h = this.tabInfo.oGet(tab).iconSize.y;

                                if (this.tabInfo.oGet(tab).align == etoi(ALIGN_LEFT)) {
                                    iconRect.x = rect.x;
                                } else if (this.tabInfo.oGet(tab).align == etoi(ALIGN_CENTER)) {
                                    iconRect.x = (rect.x + (rect.w / 2.0f)) - (iconRect.w / 2.0f);
                                } else if (this.tabInfo.oGet(tab).align == etoi(ALIGN_RIGHT)) {
                                    iconRect.x = (rect.x + rect.w) - iconRect.w;
                                }

                                if (this.tabInfo.oGet(tab).valign == 0) { //Top
                                    iconRect.y = rect.y + this.tabInfo.oGet(tab).iconVOffset;
                                } else if (this.tabInfo.oGet(tab).valign == 1) { //Center
                                    iconRect.y = ((rect.y + (rect.h / 2.0f)) - (iconRect.h / 2.0f)) + this.tabInfo.oGet(tab).iconVOffset;
                                } else if (this.tabInfo.oGet(tab).valign == 2) { //Bottom
                                    iconRect.y = ((rect.y + rect.h) - iconRect.h) + this.tabInfo.oGet(tab).iconVOffset;
                                }

                                this.dc.DrawMaterial(iconRect.x, iconRect.y, iconRect.w, iconRect.h, iconMat, new idVec4(1.0f, 1.0f, 1.0f, 1.0f), 1.0f, 1.0f);
                            }
                        }

                        this.dc.PopClipRect();

                        start = stop + 1;
                        stop = this.listItems.oGet(i).Find('\t', start);
                        if (stop < 0) {
                            stop = this.listItems.oGet(i).Length();
                        }
                        tab++;
                    }
                    rect.x = this.textRect.x;
                    rect.w = width;
                } else {
                    this.dc.DrawText(this.listItems.oGet(i), scale, 0, color, rect, false, -1);
                }
                rect.y += lineHeight;
                if (rect.y > bottom) {
                    break;
                }
            }
        }

        @Override
        public void Activate(boolean activate, idStr act) {
            super.Activate(activate, act);

            if (activate) {
                UpdateList();
            }
        }

        @Override
        public void HandleBuddyUpdate(idWindow buddy) {
            this.top = (int) this.scroller.GetValue();
        }

        @Override
        public void StateChanged(boolean redraw /*= false*/) {
            UpdateList();
        }

        @Override
        public int/*size_t*/ Allocated() {
            return super.Allocated();
        }

        @Override
        public idWinVar GetWinVarByName(final String _name, boolean winLookup /*= false*/, drawWin_t[] owner /*= NULL*/) {
            return super.GetWinVarByName(_name, winLookup, owner);
        }

        public void UpdateList() {
            final idStr str = new idStr(), strName;
            this.listItems.Clear();
            for (int i = 0; i < MAX_LIST_ITEMS; i++) {
                if (this.gui.State().GetString(va("%s_item_%d", this.listName, i), "", str)) {
                    if (str.Length() != 0) {
                        this.listItems.Append(str);
                    }
                } else {
                    break;
                }
            }
            final float vert = GetMaxCharHeight();
            final int fit = (int) (this.textRect.h / vert);
            if (this.listItems.Num() < fit) {
                this.scroller.SetRange(0.0f, 0.0f, 1.0f);
            } else {
                this.scroller.SetRange(0.0f, (this.listItems.Num() - fit) + 1.0f, 1.0f);
            }

            SetCurrentSel(this.gui.State().GetInt(va("%s_sel_0", this.listName)));

            float value = this.scroller.GetValue();
            if (value > (this.listItems.Num() - 1)) {
                value = this.listItems.Num() - 1;
            }
            if (value < 0.0f) {
                value = 0.0f;
            }
            this.scroller.SetValue(value);
            this.top = (int) value;

            this.typedTime = 0;
            this.clickTime = 0;
            this.typed.oSet("");
        }

        @Override
        protected boolean ParseInternalVar(final String _name, idParser src) {
            if (idStr.Icmp(_name, "horizontal") == 0) {
                this.horizontal = src.ParseBool();
                return true;
            }
            if (idStr.Icmp(_name, "listname") == 0) {
                ParseString(src, this.listName);
                return true;
            }
            if (idStr.Icmp(_name, "tabstops") == 0) {
                ParseString(src, this.tabStopStr);
                return true;
            }
            if (idStr.Icmp(_name, "tabaligns") == 0) {
                ParseString(src, this.tabAlignStr);
                return true;
            }
            if (idStr.Icmp(_name, "multipleSel") == 0) {
                this.multipleSel = src.ParseBool();
                return true;
            }
            if (idStr.Icmp(_name, "tabvaligns") == 0) {
                ParseString(src, this.tabVAlignStr);
                return true;
            }
            if (idStr.Icmp(_name, "tabTypes") == 0) {
                ParseString(src, this.tabTypeStr);
                return true;
            }
            if (idStr.Icmp(_name, "tabIconSizes") == 0) {
                ParseString(src, this.tabIconSizeStr);
                return true;
            }
            if (idStr.Icmp(_name, "tabIconVOffset") == 0) {
                ParseString(src, this.tabIconVOffsetStr);
                return true;
            }

            final idStr strName = new idStr(_name);
            if (idStr.Icmp(strName.Left(4), "mtr_") == 0) {
                final idStr matName = new idStr();
                final idMaterial mat;

                ParseString(src, matName);
                mat = declManager.FindMaterial(matName);
                mat.SetImageClassifications(1);	// just for resource tracking
                if ((mat != null) && !mat.TestMaterialFlag(MF_DEFAULTED)) {
                    mat.SetSort(SS_GUI);
                }
                this.iconMaterials.Set(_name, mat);
                return true;
            }

            return super.ParseInternalVar(_name, src);
        }

        private void CommonInit() {
            this.typed.oSet("");
            this.typedTime = 0;
            this.clickTime = 0;
            this.currentSel.Clear();
            this.top = 0;
            this.sizeBias = 0;
            this.horizontal = false;
            this.scroller = new idSliderWindow(this.dc, this.gui);
            this.multipleSel = false;
        }

        /*
         ================
         idListWindow::InitScroller

         This is the same as in idEditWindow
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

        private void SetCurrentSel(int sel) {
            this.currentSel.Clear();
            this.currentSel.Append(sel);
        }

        private void AddCurrentSel(int sel) {
            this.currentSel.Append(sel);
        }

        private int GetCurrentSel() {
            return (this.currentSel.Num() != 0) ? this.currentSel.oGet(0) : 0;
        }

        private boolean IsSelected(int index) {
            return (this.currentSel.FindIndex(index) >= 0);
        }

        private void ClearSelection(int sel) {
            final int cur = this.currentSel.FindIndex(sel);
            if (cur >= 0) {
                this.currentSel.RemoveIndex(cur);
            }
        }
    }
}
