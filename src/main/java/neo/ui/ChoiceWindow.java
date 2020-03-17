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
        private idStr latchedChoices = new idStr();
        private idWinStr choicesStr = new idWinStr();
        private idStr latchedVals = new idStr();
        private idWinStr choiceVals = new idWinStr();
        private idStrList choices = new idStrList();
        private idStrList values = new idStrList();
        //
        private idWinStr guiStr = new idWinStr();
        private idWinStr cvarStr = new idWinStr();
        private idCVar cvar;
        private idMultiWinVar updateStr = new idMultiWinVar();
        //
        private idWinBool liveUpdate = new idWinBool();
        private idWinStr updateGroup = new idWinStr();
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

                if (key == K_RIGHTARROW || key == K_KP_RIGHTARROW || key == K_MOUSE1) {
                    // never affects the state, but we want to execute script handlers anyway
                    if (0 == event.evValue2) {
                        RunScript(etoi(ON_ACTIONRELEASE));
                        return cmd.getData();
                    }
                    currentChoice++;
                    if (currentChoice >= choices.Num()) {
                        currentChoice = 0;
                    }
                    runAction = true;
                }

                if (key == K_LEFTARROW || key == K_KP_LEFTARROW || key == K_MOUSE2) {
                    // never affects the state, but we want to execute script handlers anyway
                    if (0 == event.evValue2) {
                        RunScript(etoi(ON_ACTIONRELEASE));
                        return cmd.getData();
                    }
                    currentChoice--;
                    if (currentChoice < 0) {
                        currentChoice = choices.Num() - 1;
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
                for (int i = 0; i < choices.Num(); i++) {
                    if (Character.toUpperCase(key) == Character.toUpperCase(choices.oGet(i).oGet(0))) {
                        if (i < currentChoice && potentialChoice < 0) {
                            potentialChoice = i;
                        } else if (i > currentChoice) {
                            potentialChoice = -1;
                            currentChoice = i;
                            break;
                        }
                    }
                }
                if (potentialChoice >= 0) {
                    currentChoice = potentialChoice;
                }

                runAction = true;
                runAction2 = true;

            } else {
                return "";
            }

            if (runAction) {
                RunScript(etoi(ON_ACTION));
            }

            if (choiceType == 0) {
                cvarStr.Set(va("%d", currentChoice));
            } else if (values.Num() != 0) {
                cvarStr.Set(values.oGet(currentChoice));
            } else {
                cvarStr.Set(choices.oGet(currentChoice));
            }

            UpdateVars(false);

            if (runAction2) {
                RunScript(etoi(ON_ACTIONRELEASE));
            }

            return cmd.getData();
        }

        @Override
        public void PostParse() {
            super.PostParse();
            UpdateChoicesAndVals();

            InitVars();
            UpdateChoice();
            UpdateVars(false);

            flags |= WIN_CANFOCUS;
        }

        @Override
        public void Draw(int time, float x, float y) {
            idVec4 color = foreColor.oCastIdVec4();

            UpdateChoicesAndVals();
            UpdateChoice();

            // FIXME: It'd be really cool if textAlign worked, but a lot of the guis have it set wrong because it used to not work
            textAlign = 0;

            if (textShadow != 0) {
                idStr shadowText = choices.oGet(currentChoice);
                idRectangle shadowRect = textRect;

                shadowText.RemoveColors();
                shadowRect.x += textShadow;
                shadowRect.y += textShadow;

                dc.DrawText(shadowText, textScale.data, textAlign, colorBlack, shadowRect, false, -1);
            }

            if (hover && NOT(noEvents) && Contains(gui.CursorX(), gui.CursorY())) {
                color = hoverColor.oCastIdVec4();
            } else {
                hover = false;
            }
            if ((flags & WIN_FOCUS) != 0) {
                color = hoverColor.oCastIdVec4();
            }

            dc.DrawText(choices.oGet(currentChoice), textScale.data, textAlign, color, textRect, false, -1);
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
                return choicesStr;
            }
            if (idStr.Icmp(_name, "values") == 0) {
                return choiceVals;
            }
            if (idStr.Icmp(_name, "cvar") == 0) {
                return cvarStr;
            }
            if (idStr.Icmp(_name, "gui") == 0) {
                return guiStr;
            }
            if (idStr.Icmp(_name, "liveUpdate") == 0) {
                return liveUpdate;
            }
            if (idStr.Icmp(_name, "updateGroup") == 0) {
                return updateGroup;
            }

            return super.GetWinVarByName(_name, winLookup, owner);
        }

        @Override
        public void RunNamedEvent(final String eventName) {
            idStr event, group;

            if (0 == idStr.Cmpn(eventName, "cvar read ", 10)) {
                event = new idStr(eventName);
                group = event.Mid(10, event.Length() - 10);
                if (0 == group.Cmp(updateGroup.data)) {
                    UpdateVars(true, true);
                }
            } else if (0 == idStr.Cmpn(eventName, "cvar write ", 11)) {
                event = new idStr(eventName);
                group = event.Mid(11, event.Length() - 11);
                if (0 == group.Cmp(updateGroup.data)) {
                    UpdateVars(false, true);
                }
            }
        }

        @Override
        protected boolean ParseInternalVar(final String _name, idParser src) {
            if (idStr.Icmp(_name, "choicetype") == 0) {
                choiceType = src.ParseInt();
                return true;
            }
            if (idStr.Icmp(_name, "currentchoice") == 0) {
                currentChoice = src.ParseInt();
                return true;
            }
            return super.ParseInternalVar(_name, src);
        }

        private void CommonInit() {
            currentChoice = 0;
            choiceType = 0;
            cvar = null;
            liveUpdate.data = true;
            choices.Clear();
        }

        private void UpdateChoice() {
            if (0 == updateStr.Num()) {
                return;
            }
            UpdateVars(true);
            updateStr.Update();
            if (choiceType == 0) {
                // ChoiceType 0 stores current as an integer in either cvar or gui
                // If both cvar and gui are defined then cvar wins, but they are both updated
                if (updateStr.oGet(0).NeedsUpdate()) {
                    try {
                        currentChoice = Integer.parseInt(updateStr.oGet(0).c_str());
                    } catch (NumberFormatException e) {
                        currentChoice = 0;
                    }
                }
                ValidateChoice();
            } else {
                // ChoiceType 1 stores current as a cvar string
                int c = (values.Num() != 0) ? values.Num() : choices.Num();
                int i;
                for (i = 0; i < c; i++) {
                    if (idStr.Icmp(cvarStr.c_str(), ((values.Num() != 0) ? values.oGet(i) : choices.oGet(i)).getData()) == 0) {
                        break;
                    }
                }
                if (i == c) {
                    i = 0;
                }
                currentChoice = i;
                ValidateChoice();
            }
        }

        private void ValidateChoice() {
            if (currentChoice < 0 || currentChoice >= choices.Num()) {
                currentChoice = 0;
            }
            if (choices.Num() == 0) {
                choices.Append("No Choices Defined");
            }
        }

        private void InitVars() {
            if (cvarStr.Length() != 0) {
                cvar = cvarSystem.Find(cvarStr.c_str());
                if (null == cvar) {
                    common.Warning("idChoiceWindow::InitVars: gui '%s' window '%s' references undefined cvar '%s'", gui.GetSourceFile(), name, cvarStr.c_str());
                    return;
                }
                updateStr.Append(cvarStr);
            }
            if (guiStr.Length() != 0) {
                updateStr.Append(guiStr);
            }
            updateStr.SetGuiInfo(gui.GetStateDict());
            updateStr.Update();
        }

        // true: read the updated cvar from cvar system, gui from dict
        // false: write to the cvar system, to the gui dict
        // force == true overrides liveUpdate 0
        private void UpdateVars(boolean read, boolean force /*= false*/) {
            if (force || liveUpdate.data) {
                if (cvar != null && cvarStr.NeedsUpdate()) {
                    if (read) {
                        cvarStr.Set(cvar.GetString());
                    } else {
                        cvar.SetString(cvarStr.c_str());
                    }
                }
                if (!read && guiStr.NeedsUpdate()) {
                    guiStr.Set(va("%d", currentChoice));
                }
            }
        }

        private void UpdateVars(boolean read) {
            UpdateVars(read, false);
        }

        private void UpdateChoicesAndVals() {
            idToken token = new idToken();
            idStr str2 = new idStr(), str3 = new idStr();
            idLexer src = new idLexer();

            if (latchedChoices.Icmp(choicesStr.data) != 0) {
                choices.Clear();
                src.FreeSource();
                src.SetFlags(LEXFL_NOFATALERRORS | LEXFL_ALLOWPATHNAMES | LEXFL_ALLOWMULTICHARLITERALS | LEXFL_ALLOWBACKSLASHSTRINGCONCAT);
                src.LoadMemory(choicesStr.data, choicesStr.Length(), "<ChoiceList>");
                if (src.IsLoaded()) {
                    while (src.ReadToken(token)) {
                        if (token.equals(";")) {
                            if (str2.Length() != 0) {
                                str2.StripTrailingWhitespace();
                                str2.oSet(common.GetLanguageDict().GetString(str2));
                                choices.Append(str2);
                                str2.oSet("");
                            }
                            continue;
                        }
                        str2.Append(token);
                        str2.Append(" ");
                    }
                    if (str2.Length() != 0) {
                        str2.StripTrailingWhitespace();
                        choices.Append(str2);
                    }
                }
                latchedChoices.oSet(choicesStr.c_str());
            }
            if (choiceVals.Length() != 0 && latchedVals.Icmp(choiceVals.data) != 0) {
                values.Clear();
                src.FreeSource();
                src.SetFlags(LEXFL_ALLOWPATHNAMES | LEXFL_ALLOWMULTICHARLITERALS | LEXFL_ALLOWBACKSLASHSTRINGCONCAT);
                src.LoadMemory(choiceVals.data, choiceVals.Length(), "<ChoiceVals>");
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
                                values.Append(str2);
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
                        values.Append(str2);
                    }
                }
                if (choices.Num() != values.Num()) {
                    common.Warning("idChoiceWindow:: gui '%s' window '%s' has value count unequal to choices count", gui.GetSourceFile(), name);
                }
                latchedVals.oSet(choiceVals.c_str());
            }
        }
    };
}
