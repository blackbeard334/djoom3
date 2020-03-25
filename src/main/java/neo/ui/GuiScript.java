package neo.ui;

import static neo.Renderer.Material.SS_GUI;
import static neo.TempDump.atoi;
import static neo.TempDump.dynamic_cast;
import static neo.TempDump.sizeof;
import static neo.framework.CmdSystem.cmdSystem;
import static neo.framework.CmdSystem.cmdExecution_t.CMD_EXEC_APPEND;
import static neo.framework.Common.STRTABLE_ID;
import static neo.framework.Common.STRTABLE_ID_LENGTH;
import static neo.framework.Common.common;
import static neo.framework.DeclManager.declManager;
import static neo.framework.Session.session;
import static neo.idlib.Lib.idLib.cvarSystem;
import static neo.idlib.Text.Lexer.LEXFL_ALLOWBACKSLASHSTRINGCONCAT;
import static neo.idlib.Text.Lexer.LEXFL_ALLOWMULTICHARLITERALS;
import static neo.idlib.Text.Lexer.LEXFL_NOSTRINGCONCAT;
import static neo.ui.Window.WIN_NOCURSOR;

import neo.Renderer.Material.idMaterial;
import neo.framework.DemoFile.idDemoFile;
import neo.framework.File_h.idFile;
import neo.idlib.Text.Parser.idParser;
import neo.idlib.Text.Str.idStr;
import neo.idlib.Text.Token.idToken;
import neo.idlib.containers.List.idList;
import neo.ui.Rectangle.idRectangle;
import neo.ui.SimpleWindow.drawWin_t;
import neo.ui.Window.idWindow;
import neo.ui.Winvar.idWinBackground;
import neo.ui.Winvar.idWinFloat;
import neo.ui.Winvar.idWinRectangle;
import neo.ui.Winvar.idWinStr;
import neo.ui.Winvar.idWinVar;
import neo.ui.Winvar.idWinVec4;

/**
 *
 */
public class GuiScript {

    public static class idGSWinVar {

        public idWinVar var;
        public boolean own;

        public idGSWinVar() {
            this.var = null;
            this.own = false;
        }
    }

    static class guiCommandDef_t {

        String name;
        Handler handler;
        // void (*handler) (idWindow *window, idList<idGSWinVar> *src);
        int mMinParms;
        int mMaxParms;

        public guiCommandDef_t(String name, Handler handler, int mMinParms, int mMaxParms) {
            this.name = name;
            this.handler = handler;
            this.mMinParms = mMinParms;
            this.mMaxParms = mMaxParms;
        }
    }
    static final guiCommandDef_t[] commandList = {
        new guiCommandDef_t("set", Script_Set.getInstance(), 2, 999),
        new guiCommandDef_t("setFocus", Script_SetFocus.getInstance(), 1, 1),
        new guiCommandDef_t("endGame", Script_EndGame.getInstance(), 0, 0),
        new guiCommandDef_t("resetTime", Script_ResetTime.getInstance(), 0, 2),
        new guiCommandDef_t("showCursor", Script_ShowCursor.getInstance(), 1, 1),
        new guiCommandDef_t("resetCinematics", Script_ResetCinematics.getInstance(), 0, 2),
        new guiCommandDef_t("transition", Script_Transition.getInstance(), 4, 6),
        new guiCommandDef_t("localSound", Script_LocalSound.getInstance(), 1, 1),
        new guiCommandDef_t("runScript", Script_RunScript.getInstance(), 1, 1),
        new guiCommandDef_t("evalRegs", Script_EvalRegs.getInstance(), 0, 0)
    };
    static final int scriptCommandCount = commandList.length;

    public static class idGuiScript {
        // friend class idGuiScriptList;
        // friend class idWindow;

        protected int conditionReg;
        protected idGuiScriptList ifList;
        protected idGuiScriptList elseList;
        private final idList<idGSWinVar> parms;
        private Handler handler;
        //
        //

        public idGuiScript() {
            this.ifList = null;
            this.elseList = null;
            this.conditionReg = -1;
            this.handler = null;
            this.parms = new idList<>();
            this.parms.SetGranularity(2);
        }

        // ~idGuiScript();
        public boolean Parse(idParser src) {
            int i;

            // first token should be function call
            // then a potentially variable set of parms
            // ended with a ;
            idToken token = new idToken();
            if (!src.ReadToken(token)) {
                src.Error("Unexpected end of file");
                return false;
            }

            this.handler = null;

            for (i = 0; i < scriptCommandCount; i++) {
                if (idStr.Icmp(token, commandList[i].name) == 0) {
                    this.handler = commandList[i].handler;
                    break;
                }
            }

            if (this.handler == null) {
                src.Error("Uknown script call %s", token);
            }
            // now read parms til ;
            // all parms are read as idWinStr's but will be fixed up later 
            // to be proper types
            while (true) {
                if (!src.ReadToken(token = new idToken())) {
                    src.Error("Unexpected end of file");
                    return false;
                }

                if (idStr.Icmp(token, ";") == 0) {
                    break;
                }

                if (idStr.Icmp(token, "}") == 0) {
                    src.UnreadToken(token);
                    break;
                }

                final idWinStr str = new idWinStr();
                str.data = token;
                final idGSWinVar wv = new idGSWinVar();
                wv.own = true;
                wv.var = str;
                this.parms.Append(wv);
            }

            // 
            //  verify min/max params
            if ((this.handler != null) && ((this.parms.Num() < commandList[i].mMinParms) || (this.parms.Num() > commandList[i].mMaxParms))) {
                src.Error("incorrect number of parameters for script %s", commandList[i].name);
            }
            // 

            return true;
        }

        public void Execute(idWindow win) {
            if (this.handler != null) {
                this.handler.run(win, this.parms);
            }
        }

        public void FixupParms(idWindow win) {
            if (this.handler == Script_Set.getInstance()) {
                boolean precacheBackground = false;
                boolean precacheSounds = false;
                idWinStr str = (idWinStr) this.parms.oGet(0).var;
                assert (str != null);
                idWinVar dest = win.GetWinVarByName(str.data.toString(), true);
                if (dest != null) {
//			delete parms[0].var;
                    this.parms.oGet(0).var = dest;
                    this.parms.oGet(0).own = false;

                    if (dest instanceof idWinBackground) {//TODO:cast null comparison. EDIT: not possible with static typing, use "instanceof" instead.
                        precacheBackground = true;
                    }
                } else if (idStr.Icmp(str.c_str(), "cmd") == 0) {
                    precacheSounds = true;
                }
                final int parmCount = this.parms.Num();
                for (int i = 1; i < parmCount; i++) {
                    str = (idWinStr) this.parms.oGet(i).var;
                    if (idStr.Icmpn(str.data, "gui::", 5) == 0) {

                        //  always use a string here, no point using a float if it is one
                        //  FIXME: This creates duplicate variables, while not technically a problem since they
                        //  are all bound to the same guiDict, it does consume extra memory and is generally a bad thing
                        final idWinStr defvar = new idWinStr();
                        defvar.Init(str.data.toString(), win);
                        win.AddDefinedVar(defvar);
//				delete parms[i].var;
                        this.parms.oGet(0).var = defvar;
                        this.parms.oGet(0).own = false;

                        //dest = win.GetWinVarByName(*str, true);
                        //if (dest) {
                        //	delete parms[i].var;
                        //	parms[i].var = dest;
                        //	parms[i].own = false;
                        //}
                        // 
                    } else if (str.equals('$')) {
                        // 
                        //  dont include the $ when asking for variable
                        dest = win.GetGui().GetDesktop().GetWinVarByName(str.c_str().substring(1), true);
                        // 					
                        if (dest != null) {
//					delete parms[i].var;
                            this.parms.oGet(i).var = dest;
                            this.parms.oGet(i).own = false;
                        }
                    } else if (idStr.Cmpn(str.c_str(), STRTABLE_ID, STRTABLE_ID_LENGTH) == 0) {
                        str.Set(common.GetLanguageDict().GetString(str.c_str()));
                    } else if (precacheBackground) {
                        final idMaterial mat = declManager.FindMaterial(str.c_str());
                        mat.SetSort(SS_GUI);
                    } else if (precacheSounds) {
                        // Search for "play <...>"
                        final idToken token = new idToken();
                        final idParser parser = new idParser(LEXFL_NOSTRINGCONCAT | LEXFL_ALLOWMULTICHARLITERALS | LEXFL_ALLOWBACKSLASHSTRINGCONCAT);
                        parser.LoadMemory(str.c_str(), str.Length(), "command");

                        while (parser.ReadToken(token)) {
                            if (token.Icmp("play") == 0) {
                                if (parser.ReadToken(token) && !token.IsEmpty()) {
                                    declManager.FindSound(token);
                                }
                            }
                        }
                    }
                }
            } else if (this.handler == Script_Transition.getInstance()) {
                if (this.parms.Num() < 4) {
                    common.Warning("Window %s in gui %s has a bad transition definition", win.GetName(), win.GetGui().GetSourceFile());
                }
                idWinStr str = (idWinStr) this.parms.oGet(0).var;
                assert (str != null);

                // 
                final drawWin_t[] destOwner = {null};
                idWinVar dest = win.GetWinVarByName(str.data.toString(), true, destOwner);
                // 

                if (dest != null) {
//			delete parms[0].var;
                    this.parms.oGet(0).var = dest;
                    this.parms.oGet(0).own = false;
                } else {
                    common.Warning("Window %s in gui %s: a transition does not have a valid destination var %s", win.GetName(), win.GetGui().GetSourceFile(), str.c_str());
                }

                // 
                //  support variables as parameters		
                int c;
                for (c = 1; c < 3; c++) {
                    str = (idWinStr) this.parms.oGet(c).var;

                    final idWinVec4 v4 = new idWinVec4();
                    this.parms.oGet(c).var = v4;
                    this.parms.oGet(c).own = true;

                    final drawWin_t[] owner = {null};

                    if (str.data.oGet(0) == '$') {
                        dest = win.GetWinVarByName(str.c_str().substring(1), true, owner);
                    } else {
                        dest = null;
                    }

                    if (dest != null) {
                        idWindow ownerparent;
                        idWindow destparent;
                        if (owner[0] != null) {
                            ownerparent = owner[0].simp != null ? owner[0].simp.GetParent() : owner[0].win.GetParent();
                            destparent = destOwner[0].simp != null ? destOwner[0].simp.GetParent() : destOwner[0].win.GetParent();

                            // If its the rectangle they are referencing then adjust it 
                            if ((ownerparent != null) && (destparent != null)
                                    && (dest == (owner[0].simp != null ? owner[0].simp.GetWinVarByName("rect") : owner[0].win.GetWinVarByName("rect")))) {
                                idRectangle rect;
                                rect = ((idWinRectangle) dest).data;
                                ownerparent.ClientToScreen(rect);
                                destparent.ScreenToClient(rect);
                                v4.oSet(rect.ToVec4());
                            } else {
                                v4.Set(dest.c_str());
                            }
                        } else {
                            v4.Set(dest.c_str());
                        }
                    } else {
                        v4.Set(str.data);
                    }

//			delete str;
                }
                // 

            } else {
                final int c = this.parms.Num();
                for (int i = 0; i < c; i++) {
                    this.parms.oGet(i).var.Init(this.parms.oGet(i).var.c_str(), win);
                }
            }
        }

        public int/*size_t*/ Size() {
            int sz = sizeof(this);
            for (int i = 0; i < this.parms.Num(); i++) {
                sz += this.parms.oGet(i).var.Size();
            }
            return sz;
        }

        public void WriteToSaveGame(idFile savefile) {
            int i;

            if (this.ifList != null) {
                this.ifList.WriteToSaveGame(savefile);
            }
            if (this.elseList != null) {
                this.elseList.WriteToSaveGame(savefile);
            }

            savefile.WriteInt(this.conditionReg);

            for (i = 0; i < this.parms.Num(); i++) {
                if (this.parms.oGet(i).own) {
                    this.parms.oGet(i).var.WriteToSaveGame(savefile);
                }
            }
        }

        public void ReadFromSaveGame(idFile savefile) {
            int i;

            if (this.ifList != null) {
                this.ifList.ReadFromSaveGame(savefile);
            }
            if (this.elseList != null) {
                this.elseList.ReadFromSaveGame(savefile);
            }

            this.conditionReg = savefile.ReadInt();

            for (i = 0; i < this.parms.Num(); i++) {
                if (this.parms.oGet(i).own) {
                    this.parms.oGet(i).var.ReadFromSaveGame(savefile);
                }
            }
        }

//protected	void (*handler) (idWindow *window, idList<idGSWinVar> *src);
    }

    static class idGuiScriptList {

        idList<idGuiScript> list;

        public idGuiScriptList() {
            this.list = new idList<>();
            this.list.SetGranularity(4);
        }

        // ~idGuiScriptList() { list.DeleteContents(true); };
        public void Execute(idWindow win) {
            final int c = this.list.Num();
            for (int i = 0; i < c; i++) {
                final idGuiScript gs = this.list.oGet(i);
                assert (gs != null);
                if (gs.conditionReg >= 0) {
                    if (win.HasOps()) {
                        final float f = win.EvalRegs(gs.conditionReg);
                        if (f != 0) {
                            if (gs.ifList != null) {
                                win.RunScriptList(gs.ifList);
                            }
                        } else if (gs.elseList != null) {
                            win.RunScriptList(gs.elseList);
                        }
                    }
                }
                gs.Execute(win);
            }
        }

        public void Append(idGuiScript gs) {
            this.list.Append(gs);
        }

        public int/*size_t*/ Size() {
            int sz = sizeof(this);
            for (int i = 0; i < this.list.Num(); i++) {
                sz += this.list.oGet(i).Size();
            }
            return sz;
        }

        public void FixupParms(idWindow win) {
            final int c = this.list.Num();
            for (int i = 0; i < c; i++) {
                final idGuiScript gs = this.list.oGet(i);
                gs.FixupParms(win);
                if (gs.ifList != null) {
                    gs.ifList.FixupParms(win);
                }
                if (gs.elseList != null) {
                    gs.elseList.FixupParms(win);
                }
            }
        }

        public void ReadFromDemoFile(idDemoFile f) {
        }

        public void WriteToDemoFile(idDemoFile f) {
        }

        public void WriteToSaveGame(idFile savefile) {
            int i;

            for (i = 0; i < this.list.Num(); i++) {
                this.list.oGet(i).WriteToSaveGame(savefile);
            }
        }

        public void ReadFromSaveGame(idFile savefile) {
            int i;

            for (i = 0; i < this.list.Num(); i++) {
                this.list.oGet(i).ReadFromSaveGame(savefile);
            }
        }
    }

    static abstract class Handler {

        public abstract void run(idWindow window, idList<idGSWinVar> src);
    }

    /*
     =========================
     Script_Set
     =========================
     */
    static class Script_Set extends Handler {

        private static final Handler instance = new Script_Set();
        private static int scriptSetTotal = 0;

        private Script_Set() {
        }

        public static Handler getInstance() {
            return instance;
        }

        @Override
        public void run(idWindow window, idList<idGSWinVar> src) {
            scriptSetTotal++;
            final String key;
			String val;
            idWinStr dest = (idWinStr) dynamic_cast(idWinStr.class, src.oGet(0).var);
            if (dest != null) {
                if (idStr.Icmp(dest.data, "cmd") == 0) {
                    dest = (idWinStr) src.oGet(1).var;
                    final int parmCount = src.Num();
                    if (parmCount > 2) {
                        val = dest.c_str();
                        int i = 2;
                        while (i < parmCount) {
                            val += " \"";
                            val += src.oGet(i).var.c_str();
                            val += "\"";
                            i++;
                        }
                        window.AddCommand(val);
                    } else {
                        window.AddCommand(dest.data.toString());
                    }
                    return;
                }
            }
            src.oGet(0).var.Set(src.oGet(1).var.c_str());
            src.oGet(0).var.SetEval(false);
        }
    }

    /*
     =========================
     Script_SetFocus
     =========================
     */
    static class Script_SetFocus extends Handler {

        private static final Handler instance = new Script_SetFocus();

        private Script_SetFocus() {
        }

        public static Handler getInstance() {
            return instance;
        }

        @Override
        public void run(idWindow window, idList<idGSWinVar> src) {
            final idWinStr parm = (idWinStr) src.oGet(0).var;
            if (parm != null) {
                final drawWin_t win = window.GetGui().GetDesktop().FindChildByName(parm.data.toString());
                if ((win != null) && (win.win != null)) {
                    window.SetFocus(win.win);
                }
            }
        }
    }

    /*
     =========================
     Script_ShowCursor
     =========================
     */
    static class Script_ShowCursor extends Handler {

        private static final Handler instance = new Script_ShowCursor();

        private Script_ShowCursor() {
        }

        public static Handler getInstance() {
            return instance;
        }

        @Override
        public void run(idWindow window, idList<idGSWinVar> src) {
            final idWinStr parm = (idWinStr) src.oGet(0).var;
            if (parm != null) {
                if (Integer.parseInt(parm.data.toString()) != 0) {
                    window.GetGui().GetDesktop().ClearFlag(WIN_NOCURSOR);
                } else {
                    window.GetGui().GetDesktop().SetFlag(WIN_NOCURSOR);
                }
            }
        }
    }

    /*
     =========================
     Script_RunScript

     run scripts must come after any set cmd set's in the script
     =========================
     */
    static class Script_RunScript extends Handler {

        private static final Handler instance = new Script_RunScript();

        private Script_RunScript() {
        }

        public static Handler getInstance() {
            return instance;
        }

        @Override
        public void run(idWindow window, idList<idGSWinVar> src) {
            final idWinStr parm = (idWinStr) src.oGet(0).var;
            if (parm != null) {
                String str = window.cmd.toString();
                str += " ; runScript ";
                str += parm.c_str();
                window.cmd.oSet(str);
            }
        }
    }

    /*
     =========================
     Script_LocalSound
     =========================
     */
    static class Script_LocalSound extends Handler {

        private static final Handler instance = new Script_LocalSound();

        private Script_LocalSound() {
        }

        public static Handler getInstance() {
            return instance;
        }

        @Override
        public void run(idWindow window, idList<idGSWinVar> src) {
            final idWinStr parm = (idWinStr) src.oGet(0).var;
            if (parm != null) {
                session.sw.PlayShaderDirectly(parm.data.toString());
            }
        }
    }

    /*
     =========================
     Script_EvalRegs
     =========================
     */
    static class Script_EvalRegs extends Handler {

        private static final Handler instance = new Script_EvalRegs();

        private Script_EvalRegs() {
        }

        public static Handler getInstance() {
            return instance;
        }

        @Override
        public void run(idWindow window, idList<idGSWinVar> src) {
            window.EvalRegs(-1, true);
        }
    }

    /*
     =========================
     Script_EndGame
     =========================
     */
    static class Script_EndGame extends Handler {

        private static final Handler instance = new Script_EndGame();

        private Script_EndGame() {
        }

        public static Handler getInstance() {
            return instance;
        }

        @Override
        public void run(idWindow window, idList<idGSWinVar> src) {
            cvarSystem.SetCVarBool("g_nightmare", true);
            cmdSystem.BufferCommandText(CMD_EXEC_APPEND, "disconnect\n");
        }
    }

    /*
     =========================
     Script_ResetTime
     =========================
     */
    static class Script_ResetTime extends Handler {

        private static final Handler instance = new Script_ResetTime();

        private Script_ResetTime() {
        }

        public static Handler getInstance() {
            return instance;
        }

        @Override
        public void run(idWindow window, idList<idGSWinVar> src) {
            idWinStr parm = (idWinStr) src.oGet(0).var;
            drawWin_t win = null;
            if ((parm != null) && (src.Num() > 1)) {
                win = window.GetGui().GetDesktop().FindChildByName(parm.data.toString());
                parm = (idWinStr) src.oGet(1).var;
            }
            if ((win != null) && (win.win != null)) {
                win.win.ResetTime(Integer.parseInt(parm.data.toString()));
                win.win.EvalRegs(-1, true);
            } else {
                window.ResetTime(Integer.parseInt(parm.data.toString()));
                window.EvalRegs(-1, true);
            }
        }
    }

    /*
     =========================
     Script_ResetCinematics
     =========================
     */
    static class Script_ResetCinematics extends Handler {

        private static final Handler instance = new Script_ResetCinematics();

        private Script_ResetCinematics() {
        }

        public static Handler getInstance() {
            return instance;
        }

        @Override
        public void run(idWindow window, idList<idGSWinVar> src) {
            window.ResetCinematics();
        }
    }

    /*
     =========================
     Script_Transition
     =========================
     */
    static class Script_Transition extends Handler {

        private static final Handler instance = new Script_Transition();

        private Script_Transition() {
        }

        public static Handler getInstance() {
            return instance;
        }

        @Override
        public void run(idWindow window, idList<idGSWinVar> src) {
            // transitions always affect rect or vec4 vars
            if (src.Num() >= 4) {
                idWinRectangle rect = null;
                final idWinVec4 vec4 = (idWinVec4) dynamic_cast(idWinVec4.class, src.oGet(0).var);
                // 
                //  added float variable
                idWinFloat val = null;
                // 
                if (null == vec4) {
                    rect = (idWinRectangle) dynamic_cast(idWinRectangle.class, src.oGet(0).var);
                    // 
                    //  added float variable					
                    if (null == rect) {
                        val = (idWinFloat) src.oGet(0).var;
                    }
                    // 
                }
                final idWinVec4 from = (idWinVec4) dynamic_cast(idWinVec4.class, src.oGet(1).var);
                final idWinVec4 to = (idWinVec4) dynamic_cast(idWinVec4.class, src.oGet(2).var);
                final idWinStr timeStr = (idWinStr) dynamic_cast(idWinStr.class, src.oGet(3).var);
                // 
                //  added float variable					
                if (!(((vec4 != null) || (rect != null) || (val != null))
                        && (from != null) && (to != null) && (timeStr != null))) {
                    // 
                    common.Warning("Bad transition in gui %s in window %s\n", window.GetGui().GetSourceFile(), window.GetName());
                    return;
                }
                final int time = atoi(timeStr.data.toString());
                float ac = 0.0f;
                float dc = 0.0f;
                if (src.Num() > 4) {
                    final idWinStr acv = (idWinStr) dynamic_cast(idWinStr.class, src.oGet(4).var);
                    final idWinStr dcv = (idWinStr) dynamic_cast(idWinStr.class, src.oGet(5).var);
                    assert ((acv != null) && (dcv != null));
                    ac = Float.parseFloat(acv.data.toString());
                    dc = Float.parseFloat(dcv.data.toString());
                }

                if (vec4 != null) {
                    vec4.SetEval(false);
                    window.AddTransition(vec4, from.data, to.data, time, ac, dc);
                    // 
                    //  added float variable					
                } else if (val != null) {
                    val.SetEval(false);
                    window.AddTransition(val, from.data, to.data, time, ac, dc);
                    // 
                } else {
                    rect.SetEval(false);
                    window.AddTransition(rect, from.data, to.data, time, ac, dc);
                }
                window.StartTransition();
            }
        }
    }
}
