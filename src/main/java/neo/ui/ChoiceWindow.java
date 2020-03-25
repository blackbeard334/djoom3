package neo.ui;

import static neo.TempDump.NOT;
import static neo.TempDump.etoi;
import static neo.framework.CVarSystem.cvarSystem;
import static neo.framework.Common.common;
import static neo.framework.KeyInput.K_KP_LEFTARROW;
import static neo.framework.KeyInput.K_KP_RIGHTARROW;
import static neo.framework.KeyInput.K_LEFTARROW;
import static neo.framework.KeyInput.K_MOUSE1;
import static neo.framework.KeyInput.K_MOUSE2;
import static neo.framework.KeyInput.K_RIGHTARROW;
import static neo.idlib.Lib.colorBlack;
import static neo.idlib.Text.Lexer.LEXFL_ALLOWBACKSLASHSTRINGCONCAT;
import static neo.idlib.Text.Lexer.LEXFL_ALLOWMULTICHARLITERALS;
import static neo.idlib.Text.Lexer.LEXFL_ALLOWPATHNAMES;
import static neo.idlib.Text.Lexer.LEXFL_NOFATALERRORS;
import static neo.idlib.Text.Str.va;
import static neo.sys.sys_public.sysEventType_t.SE_CHAR;
import static neo.sys.sys_public.sysEventType_t.SE_KEY;
import static neo.ui.Window.WIN_CANFOCUS;
import static neo.ui.Window.WIN_FOCUS;
import static neo.ui.Window.idWindow.ON.ON_ACTION;
import static neo.ui.Window.idWindow.ON.ON_ACTIONRELEASE;

import neo.framework.CVarSystem.idCVar;
import neo.idlib.Text.Lexer.idLexer;
import neo.idlib.Text.Parser.idParser;
import neo.idlib.Text.Str.idStr;
import neo.idlib.Text.Token.idToken;
import neo.idlib.containers.StrList.idStrList;
import neo.idlib.math.Vector.idVec4;
import neo.sys.sys_public.sysEvent_s;
import neo.ui.DeviceContext.idDeviceContext;
import neo.ui.Rectangle.idRectangle;
import neo.ui.SimpleWindow.drawWin_t;
import neo.ui.UserInterfaceLocal.idUserInterfaceLocal;
import neo.ui.Window.idWindow;
import neo.ui.Winvar.idMultiWinVar;
import neo.ui.Winvar.idWinBool;
import neo.ui.Winvar.idWinStr;
import neo.ui.Winvar.idWinVar;

/**
 *
 */
public class ChoiceWindow {

    public static class idChoiceWindow extends idWindow {

        private int currentChoice;
        private int choiceType;
        private final idStr latchedChoices = new idStr();
        private final idWinStr choicesStr = new idWinStr();
        private final idStr latchedVals = new idStr();
        private final idWinStr choiceVals = new idWinStr();
        private final idStrList choices = new idStrList();
        private final idStrList values = new idStrList();
        //
        private final idWinStr guiStr = new idWinStr();
        private final idWinStr cvarStr = new idWinStr();
        private idCVar cvar;
        private final idMultiWinVar updateStr = new idMultiWinVar();
        //
        private final idWinBool liveUpdate = new idWinBool();
        private final idWinStr updateGroup = new idWinStr();
        //
        //

        public idChoiceWindow(idUserInterfaceLocal gui) {
            super(gui);
            this.gui = gui;
            CommonInit();
        }

        public idChoiceWindow(idDeviceContext dc, idUserInterfaceLocal gui) {
            super(dc, gui);
            this.dc = dc;
            this.gui = gui;
            CommonInit();
        }
//	virtual				~idChoiceWindow();
//

        @Override
        public String HandleEvent(final sysEvent_s event, boolean[] updateVisuals) {
            int key;
            boolean runAction = false;
            boolean runAction2 = false;

            if (event.evType == SE_KEY) {
                key = event.evValue;

                if ((key == K_RIGHTARROW) || (key == K_KP_RIGHTARROW) || (key == K_MOUSE1)) {
                    // never affects the state, but we want to execute script handlers anyway
                    if (0 == event.evValue2) {
                        RunScript(etoi(ON_ACTIONRELEASE));
                        return this.cmd.toString();
                    }
                    this.currentChoice++;
                    if (this.currentChoice >= this.choices.Num()) {
                        this.currentChoice = 0;
                    }
                    runAction = true;
                }

                if ((key == K_LEFTARROW) || (key == K_KP_LEFTARROW) || (key == K_MOUSE2)) {
                    // never affects the state, but we want to execute script handlers anyway
                    if (0 == event.evValue2) {
                        RunScript(etoi(ON_ACTIONRELEASE));
                        return this.cmd.toString();
                    }
                    this.currentChoice--;
                    if (this.currentChoice < 0) {
                        this.currentChoice = this.choices.Num() - 1;
                    }
                    runAction = true;
                }

                if (0 == event.evValue2) {
                    // is a key release with no action catch
                    return "";
                }

            } else if (event.evType == SE_CHAR) {

                key = event.evValue;

                int potentialChoice = -1;
                for (int i = 0; i < this.choices.Num(); i++) {
                    if (Character.toUpperCase(key) == Character.toUpperCase(this.choices.oGet(i).oGet(0))) {
                        if ((i < this.currentChoice) && (potentialChoice < 0)) {
                            potentialChoice = i;
                        } else if (i > this.currentChoice) {
                            potentialChoice = -1;
                            this.currentChoice = i;
                            break;
                        }
                    }
                }
                if (potentialChoice >= 0) {
                    this.currentChoice = potentialChoice;
                }

                runAction = true;
                runAction2 = true;

            } else {
                return "";
            }

            if (runAction) {
                RunScript(etoi(ON_ACTION));
            }

            if (this.choiceType == 0) {
                this.cvarStr.Set(va("%d", this.currentChoice));
            } else if (this.values.Num() != 0) {
                this.cvarStr.Set(this.values.oGet(this.currentChoice));
            } else {
                this.cvarStr.Set(this.choices.oGet(this.currentChoice));
            }

            UpdateVars(false);

            if (runAction2) {
                RunScript(etoi(ON_ACTIONRELEASE));
            }

            return this.cmd.toString();
        }

        @Override
        public void PostParse() {
            super.PostParse();
            UpdateChoicesAndVals();

            InitVars();
            UpdateChoice();
            UpdateVars(false);

            this.flags |= WIN_CANFOCUS;
        }

        @Override
        public void Draw(int time, float x, float y) {
            idVec4 color = this.foreColor.oCastIdVec4();

            UpdateChoicesAndVals();
            UpdateChoice();

            // FIXME: It'd be really cool if textAlign worked, but a lot of the guis have it set wrong because it used to not work
            this.textAlign = 0;

            if (this.textShadow != 0) {
                final idStr shadowText = this.choices.oGet(this.currentChoice);
                final idRectangle shadowRect = this.textRect;

                shadowText.RemoveColors();
                shadowRect.x += this.textShadow;
                shadowRect.y += this.textShadow;

                this.dc.DrawText(shadowText, this.textScale.data, this.textAlign, colorBlack, shadowRect, false, -1);
            }

            if (this.hover && NOT(this.noEvents) && Contains(this.gui.CursorX(), this.gui.CursorY())) {
                color = this.hoverColor.oCastIdVec4();
            } else {
                this.hover = false;
            }
            if ((this.flags & WIN_FOCUS) != 0) {
                color = this.hoverColor.oCastIdVec4();
            }

            this.dc.DrawText(this.choices.oGet(this.currentChoice), this.textScale.data, this.textAlign, color, this.textRect, false, -1);
        }

        @Override
        public void Activate(boolean activate, idStr act) {
            super.Activate(activate, act);
            if (activate) {
                // sets the gui state based on the current choice the window contains
                UpdateChoice();
            }
        }

        @Override
        public int/*size_t*/ Allocated() {
            return super.Allocated();
        }

        @Override
        public idWinVar GetWinVarByName(final String _name, boolean winLookup /*= false*/, drawWin_t[] owner /*= NULL*/) {
            if (idStr.Icmp(_name, "choices") == 0) {
                return this.choicesStr;
            }
            if (idStr.Icmp(_name, "values") == 0) {
                return this.choiceVals;
            }
            if (idStr.Icmp(_name, "cvar") == 0) {
                return this.cvarStr;
            }
            if (idStr.Icmp(_name, "gui") == 0) {
                return this.guiStr;
            }
            if (idStr.Icmp(_name, "liveUpdate") == 0) {
                return this.liveUpdate;
            }
            if (idStr.Icmp(_name, "updateGroup") == 0) {
                return this.updateGroup;
            }

            return super.GetWinVarByName(_name, winLookup, owner);
        }

        @Override
        public void RunNamedEvent(final String eventName) {
            idStr event, group;

            if (0 == idStr.Cmpn(eventName, "cvar read ", 10)) {
                event = new idStr(eventName);
                group = event.Mid(10, event.Length() - 10);
                if (0 == group.Cmp(this.updateGroup.data)) {
                    UpdateVars(true, true);
                }
            } else if (0 == idStr.Cmpn(eventName, "cvar write ", 11)) {
                event = new idStr(eventName);
                group = event.Mid(11, event.Length() - 11);
                if (0 == group.Cmp(this.updateGroup.data)) {
                    UpdateVars(false, true);
                }
            }
        }

        @Override
        protected boolean ParseInternalVar(final String _name, idParser src) {
            if (idStr.Icmp(_name, "choicetype") == 0) {
                this.choiceType = src.ParseInt();
                return true;
            }
            if (idStr.Icmp(_name, "currentchoice") == 0) {
                this.currentChoice = src.ParseInt();
                return true;
            }
            return super.ParseInternalVar(_name, src);
        }

        private void CommonInit() {
            this.currentChoice = 0;
            this.choiceType = 0;
            this.cvar = null;
            this.liveUpdate.data = true;
            this.choices.Clear();
        }

        private void UpdateChoice() {
            if (0 == this.updateStr.Num()) {
                return;
            }
            UpdateVars(true);
            this.updateStr.Update();
            if (this.choiceType == 0) {
                // ChoiceType 0 stores current as an integer in either cvar or gui
                // If both cvar and gui are defined then cvar wins, but they are both updated
                if (this.updateStr.oGet(0).NeedsUpdate()) {
                    try {
                        this.currentChoice = Integer.parseInt(this.updateStr.oGet(0).c_str());
                    } catch (final NumberFormatException e) {
                        this.currentChoice = 0;
                    }
                }
                ValidateChoice();
            } else {
                // ChoiceType 1 stores current as a cvar string
                final int c = (this.values.Num() != 0) ? this.values.Num() : this.choices.Num();
                int i;
                for (i = 0; i < c; i++) {
                    if (idStr.Icmp(this.cvarStr.c_str(), ((this.values.Num() != 0) ? this.values.oGet(i) : this.choices.oGet(i)).toString()) == 0) {
                        break;
                    }
                }
                if (i == c) {
                    i = 0;
                }
                this.currentChoice = i;
                ValidateChoice();
            }
        }

        private void ValidateChoice() {
            if ((this.currentChoice < 0) || (this.currentChoice >= this.choices.Num())) {
                this.currentChoice = 0;
            }
            if (this.choices.Num() == 0) {
                this.choices.Append("No Choices Defined");
            }
        }

        private void InitVars() {
            if (this.cvarStr.Length() != 0) {
                this.cvar = cvarSystem.Find(this.cvarStr.c_str());
                if (null == this.cvar) {
                    common.Warning("idChoiceWindow::InitVars: gui '%s' window '%s' references undefined cvar '%s'", this.gui.GetSourceFile(), this.name, this.cvarStr.c_str());
                    return;
                }
                this.updateStr.Append(this.cvarStr);
            }
            if (this.guiStr.Length() != 0) {
                this.updateStr.Append(this.guiStr);
            }
            this.updateStr.SetGuiInfo(this.gui.GetStateDict());
            this.updateStr.Update();
        }

        // true: read the updated cvar from cvar system, gui from dict
        // false: write to the cvar system, to the gui dict
        // force == true overrides liveUpdate 0
        private void UpdateVars(boolean read, boolean force /*= false*/) {
            if (force || this.liveUpdate.data) {
                if ((this.cvar != null) && this.cvarStr.NeedsUpdate()) {
                    if (read) {
                        this.cvarStr.Set(this.cvar.GetString());
                    } else {
                        this.cvar.SetString(this.cvarStr.c_str());
                    }
                }
                if (!read && this.guiStr.NeedsUpdate()) {
                    this.guiStr.Set(va("%d", this.currentChoice));
                }
            }
        }

        private void UpdateVars(boolean read) {
            UpdateVars(read, false);
        }

        private void UpdateChoicesAndVals() {
            final idToken token = new idToken();
            final idStr str2 = new idStr(), str3 = new idStr();
            final idLexer src = new idLexer();

            if (this.latchedChoices.Icmp(this.choicesStr.data) != 0) {
                this.choices.Clear();
                src.FreeSource();
                src.SetFlags(LEXFL_NOFATALERRORS | LEXFL_ALLOWPATHNAMES | LEXFL_ALLOWMULTICHARLITERALS | LEXFL_ALLOWBACKSLASHSTRINGCONCAT);
                src.LoadMemory(this.choicesStr.data, this.choicesStr.Length(), "<ChoiceList>");
                if (src.IsLoaded()) {
                    while (src.ReadToken(token)) {
                        if (token.equals(";")) {
                            if (str2.Length() != 0) {
                                str2.StripTrailingWhitespace();
                                str2.oSet(common.GetLanguageDict().GetString(str2));
                                this.choices.Append(str2);
                                str2.oSet("");
                            }
                            continue;
                        }
                        str2.Append(token);
                        str2.Append(" ");
                    }
                    if (str2.Length() != 0) {
                        str2.StripTrailingWhitespace();
                        this.choices.Append(str2);
                    }
                }
                this.latchedChoices.oSet(this.choicesStr.c_str());
            }
            if ((this.choiceVals.Length() != 0) && (this.latchedVals.Icmp(this.choiceVals.data) != 0)) {
                this.values.Clear();
                src.FreeSource();
                src.SetFlags(LEXFL_ALLOWPATHNAMES | LEXFL_ALLOWMULTICHARLITERALS | LEXFL_ALLOWBACKSLASHSTRINGCONCAT);
                src.LoadMemory(this.choiceVals.data, this.choiceVals.Length(), "<ChoiceVals>");
                str2.oSet("");
                boolean negNum = false;
                if (src.IsLoaded()) {
                    while (src.ReadToken(token)) {
                        if (token.equals("-")) {
                            negNum = true;
                            continue;
                        }
                        if (token.equals(";")) {
                            if (str2.Length() != 0) {
                                str2.StripTrailingWhitespace();
                                this.values.Append(str2);
                                str2.oSet("");//TODO:what Da fuk? EDIT:yes yes, vision gets blury at 4 in teh morning!
                            }
                            continue;
                        }
                        if (negNum) {
                            str2.oPluSet("-");
                            negNum = false;
                        }
                        str2.oPluSet(token);
                        str2.oPluSet(" ");
                    }
                    if (str2.Length() != 0) {
                        str2.StripTrailingWhitespace();
                        this.values.Append(str2);
                    }
                }
                if (this.choices.Num() != this.values.Num()) {
                    common.Warning("idChoiceWindow:: gui '%s' window '%s' has value count unequal to choices count", this.gui.GetSourceFile(), this.name);
                }
                this.latchedVals.oSet(this.choiceVals.c_str());
            }
        }
    }
}
