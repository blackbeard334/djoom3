package neo.ui;

import static neo.Renderer.Material.MF_DEFAULTED;
import static neo.Renderer.Material.SS_GUI;
import neo.Renderer.Material.idMaterial;
import static neo.Renderer.RenderSystem_init.r_skipGuiShaders;
import static neo.TempDump.NOT;
import static neo.TempDump.atof;
import static neo.TempDump.atoi;
import static neo.TempDump.btoi;
import static neo.TempDump.etoi;
import static neo.TempDump.isNotNullOrEmpty;
import static neo.TempDump.itob;
import static neo.TempDump.sizeof;
import static neo.framework.CVarSystem.CVAR_BOOL;
import static neo.framework.CVarSystem.CVAR_GUI;
import neo.framework.CVarSystem.idCVar;
import static neo.framework.Common.EDITOR_GUI;
import static neo.framework.Common.com_editors;
import static neo.framework.Common.common;
import static neo.framework.DeclManager.declManager;
import static neo.framework.DeclManager.declType_t.DECL_TABLE;
import neo.framework.DeclTable.idDeclTable;
import neo.framework.DemoFile.idDemoFile;
import neo.framework.File_h.idFile;
import static neo.framework.KeyInput.K_ENTER;
import static neo.framework.KeyInput.K_ESCAPE;
import static neo.framework.KeyInput.K_MOUSE1;
import static neo.framework.KeyInput.K_MOUSE2;
import static neo.framework.KeyInput.K_MOUSE3;
import static neo.framework.KeyInput.K_SHIFT;
import static neo.framework.KeyInput.K_TAB;
import neo.framework.KeyInput.idKeyInput;
import static neo.framework.Session.session;
import static neo.framework.UsercmdGen.USERCMD_MSEC;
import neo.idlib.Dict_h.idDict;
import neo.idlib.Dict_h.idKeyValue;
import static neo.idlib.Lib.colorBlack;
import static neo.idlib.Text.Lexer.LEXFL_ALLOWBACKSLASHSTRINGCONCAT;
import static neo.idlib.Text.Lexer.LEXFL_ALLOWMULTICHARLITERALS;
import static neo.idlib.Text.Lexer.LEXFL_NOFATALERRORS;
import static neo.idlib.Text.Lexer.LEXFL_NOSTRINGCONCAT;
import neo.idlib.Text.Parser.idParser;
import neo.idlib.Text.Str.idStr;
import static neo.idlib.Text.Str.va;
import static neo.idlib.Text.Token.TT_FLOAT;
import static neo.idlib.Text.Token.TT_INTEGER;
import static neo.idlib.Text.Token.TT_NAME;
import static neo.idlib.Text.Token.TT_NUMBER;
import neo.idlib.Text.Token.idToken;
import neo.idlib.containers.List.idList;
import neo.idlib.math.Interpolate.idInterpolateAccelDecelLinear;
import neo.idlib.math.Matrix.idMat3;
import static neo.idlib.math.Matrix.idMat3.getMat3_identity;
import neo.idlib.math.Rotation.idRotation;
import static neo.idlib.math.Vector.getVec3_origin;
import neo.idlib.math.Vector.idVec2;
import neo.idlib.math.Vector.idVec3;
import neo.idlib.math.Vector.idVec4;
import static neo.idlib.precompiled.MAX_EXPRESSION_OPS;
import static neo.idlib.precompiled.MAX_EXPRESSION_REGISTERS;
import static neo.sys.sys_public.sysEventType_t.SE_CHAR;
import static neo.sys.sys_public.sysEventType_t.SE_KEY;
import static neo.sys.sys_public.sysEventType_t.SE_MOUSE;
import static neo.sys.sys_public.sysEventType_t.SE_NONE;
import neo.sys.sys_public.sysEvent_s;
import neo.ui.BindWindow.idBindWindow;
import neo.ui.ChoiceWindow.idChoiceWindow;
import neo.ui.DeviceContext.idDeviceContext;
import static neo.ui.DeviceContext.idDeviceContext.CURSOR.CURSOR_ARROW;
import static neo.ui.DeviceContext.idDeviceContext.CURSOR.CURSOR_HAND;
import neo.ui.EditWindow.idEditWindow;
import neo.ui.FieldWindow.idFieldWindow;
import neo.ui.GameBearShootWindow.idGameBearShootWindow;
import neo.ui.GameBustOutWindow.idGameBustOutWindow;
import neo.ui.GameSSDWindow.idGameSSDWindow;
import neo.ui.GuiScript.idGuiScript;
import neo.ui.GuiScript.idGuiScriptList;
import neo.ui.ListWindow.idListWindow;
import neo.ui.MarkerWindow.idMarkerWindow;
import neo.ui.Rectangle.idRectangle;
import neo.ui.RegExp.idRegister;
import static neo.ui.RegExp.idRegister.REGTYPE.BOOL;
import static neo.ui.RegExp.idRegister.REGTYPE.FLOAT;
import static neo.ui.RegExp.idRegister.REGTYPE.RECTANGLE;
import static neo.ui.RegExp.idRegister.REGTYPE.STRING;
import static neo.ui.RegExp.idRegister.REGTYPE.VEC2;
import static neo.ui.RegExp.idRegister.REGTYPE.VEC4;
import neo.ui.RegExp.idRegisterList;
import neo.ui.RenderWindow.idRenderWindow;
import neo.ui.SimpleWindow.drawWin_t;
import neo.ui.SimpleWindow.idSimpleWindow;
import neo.ui.SliderWindow.idSliderWindow;
import neo.ui.UserInterfaceLocal.idUserInterfaceLocal;
import static neo.ui.Window.idWindow.ON.ON_ACTION;
import static neo.ui.Window.idWindow.ON.ON_ACTIONRELEASE;
import static neo.ui.Window.idWindow.ON.ON_ACTIVATE;
import static neo.ui.Window.idWindow.ON.ON_DEACTIVATE;
import static neo.ui.Window.idWindow.ON.ON_ESC;
import static neo.ui.Window.idWindow.ON.ON_FRAME;
import static neo.ui.Window.idWindow.ON.ON_MOUSEENTER;
import static neo.ui.Window.idWindow.ON.ON_MOUSEEXIT;
import static neo.ui.Window.idWindow.ON.ON_TRIGGER;
import static neo.ui.Window.idWindow.ON.SCRIPT_COUNT;
import static neo.ui.Window.wexpOpType_t.WOP_TYPE_ADD;
import static neo.ui.Window.wexpOpType_t.WOP_TYPE_AND;
import static neo.ui.Window.wexpOpType_t.WOP_TYPE_COND;
import static neo.ui.Window.wexpOpType_t.WOP_TYPE_DIVIDE;
import static neo.ui.Window.wexpOpType_t.WOP_TYPE_EQ;
import static neo.ui.Window.wexpOpType_t.WOP_TYPE_GE;
import static neo.ui.Window.wexpOpType_t.WOP_TYPE_GT;
import static neo.ui.Window.wexpOpType_t.WOP_TYPE_LE;
import static neo.ui.Window.wexpOpType_t.WOP_TYPE_LT;
import static neo.ui.Window.wexpOpType_t.WOP_TYPE_MOD;
import static neo.ui.Window.wexpOpType_t.WOP_TYPE_MULTIPLY;
import static neo.ui.Window.wexpOpType_t.WOP_TYPE_NE;
import static neo.ui.Window.wexpOpType_t.WOP_TYPE_OR;
import static neo.ui.Window.wexpOpType_t.WOP_TYPE_SUBTRACT;
import static neo.ui.Window.wexpOpType_t.WOP_TYPE_TABLE;
import static neo.ui.Window.wexpOpType_t.WOP_TYPE_VAR;
import static neo.ui.Window.wexpOpType_t.WOP_TYPE_VARB;
import static neo.ui.Window.wexpOpType_t.WOP_TYPE_VARF;
import static neo.ui.Window.wexpOpType_t.WOP_TYPE_VARI;
import static neo.ui.Window.wexpOpType_t.WOP_TYPE_VARS;
import static neo.ui.Window.wexpRegister_t.WEXP_REG_NUM_PREDEFINED;
import static neo.ui.Window.wexpRegister_t.WEXP_REG_TIME;
import static neo.ui.Winvar.VAR_GUIPREFIX;
import neo.ui.Winvar.idWinBackground;
import neo.ui.Winvar.idWinBool;
import neo.ui.Winvar.idWinFloat;
import neo.ui.Winvar.idWinInt;
import neo.ui.Winvar.idWinRectangle;
import neo.ui.Winvar.idWinStr;
import neo.ui.Winvar.idWinVar;
import neo.ui.Winvar.idWinVec4;

/**
 *
 */
public class Window {

    static final int     WIN_CHILD           = 0x00000001;
    static final int     WIN_CAPTION         = 0x00000002;
    static final int     WIN_BORDER          = 0x00000004;
    static final int     WIN_SIZABLE         = 0x00000008;
    static final int     WIN_MOVABLE         = 0x00000010;
    static final int     WIN_FOCUS           = 0x00000020;
    static final int     WIN_CAPTURE         = 0x00000040;
    static final int     WIN_HCENTER         = 0x00000080;
    static final int     WIN_VCENTER         = 0x00000100;
    static final int     WIN_MODAL           = 0x00000200;
    static final int     WIN_INTRANSITION    = 0x00000400;
    static final int     WIN_CANFOCUS        = 0x00000800;
    static final int     WIN_SELECTED        = 0x00001000;
    static final int     WIN_TRANSFORM       = 0x00002000;
    static final int     WIN_HOLDCAPTURE     = 0x00004000;
    static final int     WIN_NOWRAP          = 0x00008000;
    static final int     WIN_NOCLIP          = 0x00010000;
    static final int     WIN_INVERTRECT      = 0x00020000;
    static final int     WIN_NATURALMAT      = 0x00040000;
    static final int     WIN_NOCURSOR        = 0x00080000;
    static final int     WIN_MENUGUI         = 0x00100000;
    static final int     WIN_ACTIVE          = 0x00200000;
    static final int     WIN_SHOWCOORDS      = 0x00400000;
    static final int     WIN_SHOWTIME        = 0x00800000;
    static final int     WIN_WANTENTER       = 0x01000000;
    //
    static final int     WIN_DESKTOP         = 0x10000000;
    //
    static final String  CAPTION_HEIGHT      = "16.0";
    static final String  SCROLLER_SIZE       = "16.0";
    static final int     SCROLLBAR_SIZE      = 16;
    //
    static final int     MAX_WINDOW_NAME     = 32;
    static final int     MAX_LIST_ITEMS      = 1024;
    //
    static final String  DEFAULT_BACKCOLOR   = "1 1 1 1";
    static final String  DEFAULT_FORECOLOR   = "0 0 0 1";
    static final String  DEFAULT_BORDERCOLOR = "0 0 0 1";
    static final String  DEFAULT_TEXTSCALE   = "0.4";
    //
    static final int     TOP_PRIORITY        = 4;
    //
    static final boolean WRITE_GUIS          = false;

    enum wexpOpType_t {

        WOP_TYPE_ADD,
        WOP_TYPE_SUBTRACT,
        WOP_TYPE_MULTIPLY,
        WOP_TYPE_DIVIDE,
        WOP_TYPE_MOD,
        WOP_TYPE_TABLE,
        WOP_TYPE_GT,
        WOP_TYPE_GE,
        WOP_TYPE_LT,
        WOP_TYPE_LE,
        WOP_TYPE_EQ,
        WOP_TYPE_NE,
        WOP_TYPE_AND,
        WOP_TYPE_OR,
        WOP_TYPE_VAR,
        WOP_TYPE_VARS,
        WOP_TYPE_VARF,
        WOP_TYPE_VARI,
        WOP_TYPE_VARB,
        WOP_TYPE_COND
    };

    enum wexpRegister_t {

        WEXP_REG_TIME,
        WEXP_REG_NUM_PREDEFINED
    };

    static class wexpOp_t {

        wexpOpType_t opType;
        idWinVar     a, d;
        int b, c;

        /**
         *
         * @return
         */
        public int getA() {
            if (a instanceof idWinFloat) {
                return (int) ((idWinFloat) a).data;
            } else if (a instanceof idWinInt) {
                return ((idWinInt) a).data;
            }

            return -1;
        }

        public int getD() {
            if (d instanceof idWinFloat) {
                return (int) ((idWinFloat) d).data;
            } else if (d instanceof idWinInt) {
                return ((idWinInt) d).data;
            }

            return -1;
        }
    };

    static class idRegEntry {

        String             name;
        idRegister.REGTYPE type;
        int                index;

        public idRegEntry(String name, idRegister.REGTYPE type) {
            this.name = name;
            this.type = type;
        }
    };

    static class idTimeLineEvent {

        int             time;
        idGuiScriptList event;
        boolean         pending;

        idTimeLineEvent() {
            event = new idGuiScriptList();
        }
//	~idTimeLineEvent() {
//		delete event;
//	}

        int/*size_t*/ Size() {
            return sizeof(this) + event.Size();
        }
    };

    static class rvNamedEvent {

        public rvNamedEvent(final String name) {
            mEvent = new idGuiScriptList();
            mName = new idStr(name);
        }
        // ~rvNamedEvent(void)
        // {
        // delete mEvent;
        // }

        public int /*size_t */ Size() {
            return sizeof(this) + mEvent.Size();
        }

        idStr           mName;
        idGuiScriptList mEvent;
    };

    static class idTransitionData {

        idWinVar data;
        int      offset;
        idInterpolateAccelDecelLinear<idVec4> interp = new idInterpolateAccelDecelLinear<>();
    };

    public static class idWindow {

        protected              float       actualX;            // physical coords
        protected              float       actualY;            // ''
        protected              int         childID;            // this childs id
        protected /*unsigned*/ int         flags;              // visible, focus, mouseover, cursor, border, etc..
        protected              int         lastTimeRun;        //
        protected              idRectangle drawRect     = new idRectangle();// overall rect
        protected              idRectangle clientRect   = new idRectangle();// client area
        protected              idVec2      origin       = new idVec2();
        //
        protected int    timeLine; // time stamp used for various fx
        protected float  xOffset;
        protected float  yOffset;
        protected float  forceAspectWidth;
        protected float  forceAspectHeight;
        protected float  matScalex;
        protected float  matScaley;
        protected float  borderSize;
        protected float  textAlignx;
        protected float  textAligny;
        protected idStr  name;
        protected idStr  comment = new idStr();
        protected idVec2 shear = new idVec2();
        //
        protected /*signed*/   char textShadow;
        protected /*unsigned*/ char fontNum;
        protected /*unsigned*/ char cursor;
        protected /*signed*/   char textAlign;
        //
        protected idWinBool       noTime            = new idWinBool();
        protected idWinBool       visible           = new idWinBool();
        protected idWinBool       noEvents          = new idWinBool();
        protected idWinRectangle  rect              = new idWinRectangle();// overall rect
        protected idWinVec4       backColor         = new idWinVec4();
        protected idWinVec4       matColor          = new idWinVec4();
        protected idWinVec4       foreColor         = new idWinVec4();
        protected idWinVec4       hoverColor        = new idWinVec4();
        protected idWinVec4       borderColor       = new idWinVec4();
        protected idWinFloat      textScale         = new idWinFloat();
        protected idWinFloat      rotate            = new idWinFloat();
        protected idWinStr        text              = new idWinStr();
        protected idWinBackground backGroundName    = new idWinBackground();
        //
        protected idList<idWinVar>  definedVars = new idList<>();
        protected idList<idWinVar>  updateVars  = new idList<>();
        //
        protected idRectangle       textRect  = new idRectangle();// text extented rect
        protected idMaterial        background;                   // background asset
        //
        protected idWindow          parent;                       // parent window
        protected idList<idWindow>  children = new idList<>();    // child windows
        protected idList<drawWin_t> drawWindows = new idList<>();
        //
        protected idWindow          focusedChild;        // if a child window has the focus
        protected idWindow          captureChild;        // if a child window has mouse capture
        protected idWindow          overChild;           // if a child window has mouse capture
        protected boolean           hover;
        //
        protected idDeviceContext      dc;
        //
        protected idUserInterfaceLocal gui;
        //
        protected static final idCVar gui_debug = new idCVar("gui_debug", "0", CVAR_GUI | CVAR_BOOL, "");
        protected static final idCVar gui_edit  = new idCVar("gui_edit", "0", CVAR_GUI | CVAR_BOOL, "");
        //
        protected idGuiScriptList[] scripts = new idGuiScriptList[etoi(SCRIPT_COUNT)];
        protected boolean[]         saveTemps;
        //
        protected idList<idTimeLineEvent>  timeLineEvents = new idList<>();
        protected idList<idTransitionData> transitions = new idList<>();
        //
        protected static boolean[] registerIsTemporary = new boolean[MAX_EXPRESSION_REGISTERS]; // statics to assist during parsing
        //
        protected idList<wexpOp_t>     ops                 = new idList<>();// evaluate to make expressionRegisters
        protected idList<Float>        expressionRegisters = new idList<>();
        protected idList<wexpOp_t>[]   saveOps;                             // evaluate to make expressionRegisters
        protected idList<rvNamedEvent> namedEvents         = new idList<>();//  added named events
        protected idList<Float>[]      saveRegs;
        //
        protected idRegisterList regList = new idRegisterList();
        //
        protected idWinBool hideCursor = new idWinBool();
        //
        private static       idMat3     trans   = new idMat3();
        private static       idVec3     org     = new idVec3();
        private static       idRotation rot;
        private static final idVec3     vec     = new idVec3(0, 0, 1);
        private static       idMat3     smat    = new idMat3();
        //
        //
        private static int DBG_COUNTER = 0;
        private final  int DBG_COUNT= DBG_COUNTER++;

        protected idWindow() {
        }

        public idWindow(idUserInterfaceLocal gui) {
            dc = null;
            this.gui = gui;
            CommonInit();
        }

        public idWindow(idDeviceContext d, idUserInterfaceLocal ui) {
            dc = d;
            gui = ui;
            CommonInit();
        }

        /** ~idWindow() */
        public void close() {
            CleanUp();
        }

        public enum ON {

            ON_MOUSEENTER,//= 0,
            ON_MOUSEEXIT,
            ON_ACTION,
            ON_ACTIVATE,
            ON_DEACTIVATE,
            ON_ESC,
            ON_FRAME,
            ON_TRIGGER,
            ON_ACTIONRELEASE,
            ON_ENTER,
            ON_ENTERRELEASE,
            SCRIPT_COUNT
        };

        public enum ADJUST {

            ADJUST_MOVE,//= 0,
            ADJUST_TOP,
            ADJUST_RIGHT,
            ADJUST_BOTTOM,
            ADJUST_LEFT,
            ADJUST_TOPLEFT,
            ADJUST_BOTTOMRIGHT,
            ADJUST_TOPRIGHT,
            ADJUST_BOTTOMLEFT
        };
//        public static final String[] ScriptNames = new String[SCRIPT_COUNT.ordinal()];
        public static final String[] ScriptNames = {
            "onMouseEnter",
            "onMouseExit",
            "onAction",
            "onActivate",
            "onDeactivate",
            "onESC",
            "onEvent",
            "onTrigger",
            "onActionRelease",
            "onEnter",
            "onEnterRelease"
        };
        public static final idRegEntry[] RegisterVars = {
            new idRegEntry("forecolor", VEC4),
            new idRegEntry("hovercolor", VEC4),
            new idRegEntry("backcolor", VEC4),
            new idRegEntry("bordercolor", VEC4),
            new idRegEntry("rect", RECTANGLE),
            new idRegEntry("matcolor", VEC4),
            new idRegEntry("scale", VEC2),
            new idRegEntry("translate", VEC2),
            new idRegEntry("rotate", FLOAT),
            new idRegEntry("textscale", FLOAT),
            new idRegEntry("visible", BOOL),
            new idRegEntry("noevents", BOOL),
            new idRegEntry("text", STRING),
            new idRegEntry("background", STRING),
            new idRegEntry("runscript", STRING),
            new idRegEntry("varbackground", STRING),
            new idRegEntry("cvar", STRING),
            new idRegEntry("choices", STRING),
            new idRegEntry("choiceVar", STRING),
            new idRegEntry("bind", STRING),
            new idRegEntry("modelRotate", VEC4),
            new idRegEntry("modelOrigin", VEC4),
            new idRegEntry("lightOrigin", VEC4),
            new idRegEntry("lightColor", VEC4),
            new idRegEntry("viewOffset", VEC4),
            new idRegEntry("hideCursor", BOOL)
        };
        public static final int NumRegisterVars = RegisterVars.length;

        public void SetDC(idDeviceContext d) {
            dc = d;
            //if (flags & WIN_DESKTOP) {
            dc.SetSize(forceAspectWidth, forceAspectHeight);
            //}
            int c = children.Num();
            for (int i = 0; i < c; i++) {
                children.oGet(i).SetDC(d);
            }
        }

        public idDeviceContext GetDC() {
            return dc;
        }

        public idWindow SetFocus(idWindow w, boolean scripts /*= true*/) {
            // only one child can have the focus
            idWindow lastFocus = null;
            if ((w.flags & WIN_CANFOCUS) != 0) {
                lastFocus = gui.GetDesktop().focusedChild;
                if (lastFocus != null) {
                    lastFocus.flags &= ~WIN_FOCUS;
                    lastFocus.LoseFocus();
                }

                //  call on lose focus
                if (scripts && lastFocus != null) {
                    // calling this broke all sorts of guis
                    // lastFocus.RunScript(ON_MOUSEEXIT);
                }
                //  call on gain focus
                if (scripts && w != null) {
                    // calling this broke all sorts of guis
                    // w.RunScript(ON_MOUSEENTER);
                }

                w.flags |= WIN_FOCUS;
                w.GainFocus();
                gui.GetDesktop().focusedChild = w;
            }

            return lastFocus;
        }

        public idWindow SetFocus(idWindow w) {
            return this.SetFocus(w, false);
        }

        public idWindow SetCapture(idWindow w) {
            // only one child can have the focus

            idWindow last = null;
            int c = children.Num();
            for (int i = 0; i < c; i++) {
                if ((children.oGet(i).flags & WIN_CAPTURE) != 0) {
                    last = children.oGet(i);
                    //last.flags &= ~WIN_CAPTURE;
                    last.LoseCapture();
                    break;
                }
            }

            w.flags |= WIN_CAPTURE;
            w.GainCapture();
            gui.GetDesktop().captureChild = w;
            return last;
        }

        public void SetParent(idWindow w) {
            parent = w;
        }

        public void SetFlag(/*unsigned*/int f) {
            flags |= f;
        }

        public void ClearFlag(/*unsigned*/int f) {
            flags &= ~f;
        }

        public /*unsigned*/ int GetFlags() {
            return flags;
        }

        public void Move(float x, float y) {
            idRectangle rct = rect.data;
            rct.x = x;
            rct.y = y;
            idRegister reg = RegList().FindReg("rect");
            if (reg != null) {
                reg.Enable(false);
            }
            rect.data = rct;
        }

        public void BringToTop(idWindow w) {

            if (w != null && 0 == (w.flags & WIN_MODAL)) {
                return;
            }

            int c = children.Num();
            for (int i = 0; i < c; i++) {
                if (children.oGet(i).equals(w)) {
                    // this is it move from i - 1 to 0 to i to 1 then shove this one into 0
                    for (int j = i + 1; j < c; j++) {
                        children.oSet(j - 1, children.oGet(j));
                    }
                    children.oSet(c - 1, w);
                    break;
                }
            }
        }

        public void Adjust(float xd, float yd) {
        }

        public void SetAdjustMode(idWindow child) {
        }

        public void Size(float x, float y, float w, float h) {
            idRectangle rct = rect.data;
            rct.x = x;
            rct.y = y;
            rct.w = w;
            rct.h = h;
            rect.data = rct;
            CalcClientRect(0, 0);
        }

        public void SetupFromState() {
//	idStr str;
            background = null;

            SetupBackground();

            if (borderSize != 0) {
                flags |= WIN_BORDER;
            }

            if (regList.FindReg("rotate") != null || regList.FindReg("shear") != null) {
                flags |= WIN_TRANSFORM;
            }

            CalcClientRect(0, 0);
            if (scripts[etoi(ON_ACTION)] != null) {
                cursor = (char) etoi(CURSOR_HAND);
                flags |= WIN_CANFOCUS;
            }
        }

        private static int DBG_SetupBackground = 0;
        public void SetupBackground() {    DBG_SetupBackground++;
            if (backGroundName.Length() != 0) {
                background = declManager.FindMaterial(backGroundName.data);
                background.SetImageClassifications(1);    // just for resource tracking
                if (background != null && !background.TestMaterialFlag(MF_DEFAULTED)) {
                    background.SetSort(SS_GUI);
                }
            }
            backGroundName.SetMaterialPtr(background);
        }

        private static final drawWin_t dw = new drawWin_t();

        public drawWin_t FindChildByName(final String _name) {
            if (idStr.Icmp(name.toString(), _name) == 0) {
                dw.simp = null;
                dw.win = this;
                return dw;
            }
            int c = drawWindows.Num();
            for (int i = 0; i < c; i++) {
                if (drawWindows.oGet(i).win != null) {
                    if (idStr.Icmp(drawWindows.oGet(i).win.name, _name) == 0) {
                        return drawWindows.oGet(i);
                    }
                    drawWin_t win = drawWindows.oGet(i).win.FindChildByName(_name);
                    if (win != null) {
                        return win;
                    }
                } else {
                    if (idStr.Icmp(drawWindows.oGet(i).simp.name, _name) == 0) {
                        return drawWindows.oGet(i);
                    }
                }
            }
            return null;
        }

        public idSimpleWindow FindSimpleWinByName(final String _name) {
            throw new UnsupportedOperationException();
        }

        public idWindow GetParent() {
            return parent;
        }

        public idUserInterfaceLocal GetGui() {
            return gui;
        }

        public boolean Contains(float x, float y) {
            idRectangle r = drawRect;
            r.x = actualX;
            r.y = actualY;
            return r.Contains(x, y);
        }

        public int/*size_t*/ Size() {
            int c = children.Num();
            int sz = 0;
            for (int i = 0; i < c; i++) {
                sz += children.oGet(i).Size();
            }
            sz += sizeof(this) + Allocated();
            return sz;
        }

        public int/*size_t*/ Allocated() {
            int i, c;
            int sz = name.Allocated();
            sz += text.Size();
            sz += backGroundName.Size();

            c = definedVars.Num();
            for (i = 0; i < c; i++) {
                sz += definedVars.oGet(i).Size();
            }

            for (i = 0; i < SCRIPT_COUNT.ordinal(); i++) {
                if (scripts[i] != null) {
                    sz += scripts[i].Size();
                }
            }
            c = timeLineEvents.Num();
            for (i = 0; i < c; i++) {
                sz += timeLineEvents.oGet(i).Size();
            }

            c = namedEvents.Num();
            for (i = 0; i < c; i++) {
                sz += namedEvents.oGet(i).Size();
            }

            c = drawWindows.Num();
            for (i = 0; i < c; i++) {
                if (drawWindows.oGet(i).simp != null) {
                    sz += drawWindows.oGet(i).simp.Size();
                }
            }

            return sz;
        }

        public idStr GetStrPtrByName(final String _name) {
            return null;
        }

        public idWinVar GetWinVarByName(final String _name, boolean fixup /*= false*/, drawWin_t[] owner /*= NULL*/) {
            idWinVar retVar = null;

            if (owner != null) {
                owner[0] = null;
            }

            if (idStr.Icmp(_name, "notime") == 0) {
                retVar = noTime;
            }
            if (idStr.Icmp(_name, "background") == 0) {
                retVar = backGroundName;
            }
            if (idStr.Icmp(_name, "visible") == 0) {
                retVar = visible;
            }
            if (idStr.Icmp(_name, "rect") == 0) {
                retVar = rect;
            }
            if (idStr.Icmp(_name, "backColor") == 0) {
                retVar = backColor;
            }
            if (idStr.Icmp(_name, "matColor") == 0) {
                retVar = matColor;
            }
            if (idStr.Icmp(_name, "foreColor") == 0) {
                retVar = foreColor;
            }
            if (idStr.Icmp(_name, "hoverColor") == 0) {
                retVar = hoverColor;
            }
            if (idStr.Icmp(_name, "borderColor") == 0) {
                retVar = borderColor;
            }
            if (idStr.Icmp(_name, "textScale") == 0) {
                retVar = textScale;
            }
            if (idStr.Icmp(_name, "rotate") == 0) {
                retVar = rotate;
            }
            if (idStr.Icmp(_name, "noEvents") == 0) {
                retVar = noEvents;
            }
            if (idStr.Icmp(_name, "text") == 0) {
                retVar = text;
            }
            if (idStr.Icmp(_name, "backGroundName") == 0) {
                retVar = backGroundName;
            }
            if (idStr.Icmp(_name, "hidecursor") == 0) {
                retVar = hideCursor;
            }

            idStr key = new idStr(_name);
            boolean guiVar = (key.Find(VAR_GUIPREFIX) >= 0);
            int c = definedVars.Num();
            for (int i = 0; i < c; i++) {
                if (idStr.Icmp(_name, (guiVar) ? va("%s", definedVars.oGet(i).GetName()) : definedVars.oGet(i).GetName()) == 0) {
                    retVar = definedVars.oGet(i);
                    break;
                }
            }

            if (retVar != null) {
                if (fixup && _name.charAt(0) != '$') {
                    DisableRegister(_name);
                }

                if (owner != null && parent != null) {
                    owner[0] = parent.FindChildByName(name.toString());
                }

                return retVar;
            }

            int len = key.Length();
            if (len > 5 && guiVar) {
                idWinVar var = new idWinStr();
                var.Init(_name, this);
                definedVars.Append(var);
                return var;
            } else if (fixup) {
                int n = key.Find("::");
                if (n > 0) {
                    idStr winName = key.Left(n);
                    idStr var = key.Right(key.Length() - n - 2);
                    drawWin_t win = GetGui().GetDesktop().FindChildByName(winName.toString());
                    if (win != null) {
                        if (win.win != null) {
                            return win.win.GetWinVarByName(var.toString(), false, owner);
                        } else {
                            if (owner != null) {
                                owner[0] = win;
                            }
                            return win.simp.GetWinVarByName(var.toString());
                        }
                    }
                }
            }
            return null;
        }

        public idWinVar GetWinVarByName(final String _name, boolean winLookup /*= false*/) {
            return GetWinVarByName(_name, winLookup, null);
        }

        public idWinVar GetWinVarByName(final String _name) {
            return GetWinVarByName(_name, false);
        }

        public int GetWinVarOffset(idWinVar wv, drawWin_t owner) {
            int ret = -1;

            //TODO:think of something after all implementaions are clear.
//	if ( wv == rect ) {
//		ret = (int)&( ( idWindow * ) 0 ).rect;
//	}
//
//	if ( wv == &backColor ) {
//		ret = (int)&( ( idWindow * ) 0 ).backColor;
//	}
//
//	if ( wv == &matColor ) {
//		ret = (int)&( ( idWindow * ) 0 ).matColor;
//	}
//
//	if ( wv == &foreColor ) {
//		ret = (int)&( ( idWindow * ) 0 ).foreColor;
//	}
//
//	if ( wv == &hoverColor ) {
//		ret = (int)&( ( idWindow * ) 0 ).hoverColor;
//	}
//
//	if ( wv == &borderColor ) {
//		ret = (int)&( ( idWindow * ) 0 ).borderColor;
//	}
//
//	if ( wv == &textScale ) {
//		ret = (int)&( ( idWindow * ) 0 ).textScale;
//	}
//
//	if ( wv == &rotate ) {
//		ret = (int)&( ( idWindow * ) 0 ).rotate;
//	}
            if (ret != -1) {
                owner.win = this;
                return ret;
            }

            for (int i = 0; i < drawWindows.Num(); i++) {
                if (drawWindows.oGet(i).win != null) {
                    ret = drawWindows.oGet(i).win.GetWinVarOffset(wv, owner);
                } else {
                    ret = drawWindows.oGet(i).simp.GetWinVarOffset(wv, owner);
                }
                if (ret != -1) {
                    break;
                }
            }

            return ret;
        }

        public float GetMaxCharHeight() {
            SetFont();
            return dc.MaxCharHeight(textScale.data);
        }

        public float GetMaxCharWidth() {
            SetFont();
            return dc.MaxCharWidth(textScale.data);
        }

        public void SetFont() {
            dc.SetFont(fontNum);
        }

        public void SetInitialState(final String _name) {
            name = new idStr(_name);
            matScalex = 1.0f;
            matScaley = 1.0f;
            forceAspectWidth = 640.0f;
            forceAspectHeight = 480.0f;
            noTime.data = false;
            visible.data = true;
            flags = 0;
        }

        public void AddChild(idWindow win) {
            win.childID = children.Append(win);
        }
        private static String buff = "";//[16384];

        public void DebugDraw(int time, float x, float y) {
            if (dc != null) {
                dc.EnableClipping(false);
                if (gui_debug.GetInteger() == 1) {
                    dc.DrawRect(drawRect.x, drawRect.y, drawRect.w, drawRect.h, 1, idDeviceContext.colorRed);
                } else if (gui_debug.GetInteger() == 2) {
//			char out[1024];
                    String out;//[1024];
                    idStr str;
                    str = new idStr(text.c_str());

                    if (str.Length() != 0) {
                        buff = String.format("%s\n", str);
                    }

                    out = String.format("Rect: %0.1f, %0.1f, %0.1f, %0.1f\n", rect.x(), rect.y(), rect.w(), rect.h());
                    buff += out;
                    out = String.format("Draw Rect: %0.1f, %0.1f, %0.1f, %0.1f\n", drawRect.x, drawRect.y, drawRect.w, drawRect.h);
                    buff += out;
                    out = String.format("Client Rect: %0.1f, %0.1f, %0.1f, %0.1f\n", clientRect.x, clientRect.y, clientRect.w, clientRect.h);
                    buff += out;
                    out = String.format("Cursor: %0.1f : %0.1f\n", gui.CursorX(), gui.CursorY());
                    buff += out;

                    //idRectangle tempRect = textRect;
                    //tempRect.x += offsetX;
                    //drawRect.y += offsetY;
                    dc.DrawText(buff, textScale.data, textAlign, foreColor.data, textRect, true);
                }
                dc.EnableClipping(true);
            }
        }

        public void CalcClientRect(float xofs, float yofs) {
            drawRect.oSet(rect.data);

            if ((flags & WIN_INVERTRECT) != 0) {
                drawRect.x = rect.x() - rect.w();
                drawRect.y = rect.y() - rect.h();
            }

            if ((flags & (WIN_HCENTER | WIN_VCENTER)) != 0 && parent != null) {
                // in this case treat xofs and yofs as absolute top left coords
                // and ignore the original positioning
                if ((flags & WIN_HCENTER) != 0) {
                    drawRect.x = (parent.rect.w() - rect.w()) / 2;
                } else {
                    drawRect.y = (parent.rect.h() - rect.h()) / 2;
                }
            }

            drawRect.x += xofs;
            drawRect.y += yofs;

            clientRect.oSet(drawRect);
            if (rect.h() > 0.0 && rect.w() > 0.0) {

                if (((flags & WIN_BORDER) != 0) && borderSize != 0.0) {
                    clientRect.x += borderSize;
                    clientRect.y += borderSize;
                    clientRect.w -= borderSize;
                    clientRect.h -= borderSize;
                }

                textRect.oSet(clientRect);
                textRect.x += 2.0;
                textRect.w -= 2.0;
                textRect.y += 2.0;
                textRect.h -= 2.0;

                textRect.x += textAlignx;
                textRect.y += textAligny;

            }
            origin.Set(rect.x() + (rect.w() / 2), rect.y() + (rect.h() / 2));

        }

        public void CommonInit() {
            childID = 0;
            flags = 0;
            lastTimeRun = 0;
            origin.Zero();
            fontNum = 0;
            timeLine = -1;
            xOffset = yOffset = 0.0f;
            cursor = 0;
            forceAspectWidth = 640;
            forceAspectHeight = 480;
            matScalex = 1;
            matScaley = 1;
            borderSize = 0;
            noTime.data = false;
            visible.data = true;
            textAlign = 0;
            textAlignx = 0;
            textAligny = 0;
            noEvents.data = false;
            rotate.data = 0;
            shear.Zero();
            textScale.data = 0.35f;
            backColor.Zero();
            foreColor.oSet(new idVec4(1, 1, 1, 1));
            hoverColor.oSet(new idVec4(1, 1, 1, 1));
            matColor.oSet(new idVec4(1, 1, 1, 1));
            borderColor.Zero();
            background = null;
            backGroundName.oSet(new idStr(""));
            focusedChild = null;
            captureChild = null;
            overChild = null;
            parent = null;
            saveOps = null;
            saveRegs = null;
            timeLine = -1;
            textShadow = 0;
            hover = false;

            for (int i = 0; i < SCRIPT_COUNT.ordinal(); i++) {
                scripts[i] = null;
            }

            hideCursor.data = false;
        }

        public void CleanUp() {
            int i, c = drawWindows.Num();
            for (i = 0; i < c; i++) {
//		delete drawWindows[i].simp;
                drawWindows.oSet(i, null);
            }

            // ensure the register list gets cleaned up
            regList.Reset();

            // Cleanup the named events
            namedEvents.DeleteContents(true);

            drawWindows.Clear();
            children.DeleteContents(true);
            definedVars.DeleteContents(true);
            timeLineEvents.DeleteContents(true);
            for (i = 0; i < SCRIPT_COUNT.ordinal(); i++) {
//		delete scripts[i];
                scripts[i] = null;
            }
            CommonInit();
        }

        public void DrawBorderAndCaption(final idRectangle drawRect) {
            if (((flags & WIN_BORDER) != 0) && borderSize != 0 && borderColor.w() != 0) {
                dc.DrawRect(drawRect.x, drawRect.y, drawRect.w, drawRect.h, borderSize, borderColor.data);
            }
        }

        public void DrawCaption(int time, float x, float y) {
        }

        public void SetupTransforms(float x, float y) {

            trans.Identity();
            org.Set(origin.x + x, origin.y + y, 0);

            if (rotate.data != 0) {
                rot.Set(org, vec, rotate.data);
                trans = rot.ToMat3();
            }

            if (shear.x != 0 || shear.y != 0) {
                smat.Identity();
                smat.oSet(0, 1, shear.x);
                smat.oSet(1, 0, shear.y);
                trans.oMulSet(smat);
            }

            if (!trans.IsIdentity()) {
                dc.SetTransformInfo(org, trans);
            }
        }

        public boolean Contains(final idRectangle sr, float x, float y) {
            idRectangle r = sr;
            r.x += actualX - drawRect.x;
            r.y += actualY - drawRect.y;
            return r.Contains(x, y);
        }

        public String GetName() {
            return name.toString();//TODO:return idStr???
        }

        static int simpleCount = 0, plainCount = 0;

        public boolean Parse(idParser src, boolean rebuild /*= true*/) {
            idToken token = new idToken(), token2, token3, token4, token5, token6, token7;
            idStr work;
            drawWin_t dwt;

            if (rebuild) {
                CleanUp();
            }

            timeLineEvents.Clear();
            transitions.Clear();

            namedEvents.DeleteContents(true);

            src.ExpectTokenType(TT_NAME, 0, token);

            SetInitialState(token.toString());

            src.ExpectTokenString("{");
            src.ExpectAnyToken(token);

            boolean ret = true;

            // attach a window wrapper to the window if the gui editor is running
//            if (ID_ALLOW_TOOLS) {
//                if ((com_editors & EDITOR_GUI) != 0) {
//                    new rvGEWindowWrapper(this, rvGEWindowWrapper.WT_NORMAL);
//                }
//            }
//
            while (!token.equals("}")) {
                // track what was parsed so we can maintain it for the guieditor
                src.SetMarker();
                dwt = new drawWin_t();

                if (token.equals("windowDef") || token.equals("animationDef")) {
                    if (token.equals("animationDef")) {
                        visible.data = false;
                        rect.data = new idRectangle(0, 0, 0, 0);
                    }
                    src.ExpectTokenType(TT_NAME, 0, token);
                    token2 = token;
//                    System.out.printf(">>>>>>>>%s\n", token.toString());
                    src.UnreadToken(token);
                    drawWin_t dw = FindChildByName(token2.toString());
                    if (dw != null && dw.win != null) {
                        SaveExpressionParseState();
                        dw.win.Parse(src, rebuild);
                        RestoreExpressionParseState();
                    } else {
                        idWindow win = new idWindow(dc, gui);
                        SaveExpressionParseState();
                        win.Parse(src, rebuild);
                        RestoreExpressionParseState();
                        win.SetParent(this);
                        dwt.simp = null;
                        dwt.win = null;
                        if (win.IsSimple()) {
                            idSimpleWindow simple = new idSimpleWindow(win);
                            dwt.simp = simple;
                            drawWindows.Append(dwt);
			    win.close();//delete win;
                            simpleCount++;
                        } else {
                            AddChild(win);
                            SetFocus(win, false);
                            dwt.win = win;
//                            System.out.println(dwt.win.text.c_str());
                            drawWindows.Append(dwt);
                            plainCount++;
                        }
                    }
                } else if (token.equals("editDef")) {
                    idEditWindow win = new idEditWindow(dc, gui);
                    SaveExpressionParseState();
                    win.Parse(src, rebuild);
                    RestoreExpressionParseState();
                    AddChild(win);
                    win.SetParent(this);
                    dwt.simp = null;
                    dwt.win = win;
                    drawWindows.Append(dwt);
                } else if (token.equals("choiceDef")) {
                    idChoiceWindow win = new idChoiceWindow(dc, gui);
                    SaveExpressionParseState();
                    win.Parse(src, rebuild);
                    RestoreExpressionParseState();
                    AddChild(win);
                    win.SetParent(this);
                    dwt.simp = null;
                    dwt.win = win;
                    drawWindows.Append(dwt);
                } else if (token.equals("sliderDef")) {
                    idSliderWindow win = new idSliderWindow(dc, gui);
                    SaveExpressionParseState();
                    win.Parse(src, rebuild);
                    RestoreExpressionParseState();
                    AddChild(win);
                    win.SetParent(this);
                    dwt.simp = null;
                    dwt.win = win;
                    drawWindows.Append(dwt);
                } else if (token.equals("markerDef")) {
                    idMarkerWindow win = new idMarkerWindow(dc, gui);
                    SaveExpressionParseState();
                    win.Parse(src, rebuild);
                    RestoreExpressionParseState();
                    AddChild(win);
                    win.SetParent(this);
                    dwt.simp = null;
                    dwt.win = win;
                    drawWindows.Append(dwt);
                } else if (token.equals("bindDef")) {
                    idBindWindow win = new idBindWindow(dc, gui);
                    SaveExpressionParseState();
                    win.Parse(src, rebuild);
                    RestoreExpressionParseState();
                    AddChild(win);
                    win.SetParent(this);
                    dwt.simp = null;
                    dwt.win = win;
                    drawWindows.Append(dwt);
                } else if (token.equals("listDef")) {
                    idListWindow win = new idListWindow(dc, gui);
                    SaveExpressionParseState();
                    win.Parse(src, rebuild);
                    RestoreExpressionParseState();
                    AddChild(win);
                    win.SetParent(this);
                    dwt.simp = null;
                    dwt.win = win;
                    drawWindows.Append(dwt);
                } else if (token.equals("fieldDef")) {
                    idFieldWindow win = new idFieldWindow(dc, gui);
                    SaveExpressionParseState();
                    win.Parse(src, rebuild);
                    RestoreExpressionParseState();
                    AddChild(win);
                    win.SetParent(this);
                    dwt.simp = null;
                    dwt.win = win;
                    drawWindows.Append(dwt);
                } else if (token.equals("renderDef")) {
                    idRenderWindow win = new idRenderWindow(dc, gui);
                    SaveExpressionParseState();
                    win.Parse(src, rebuild);
                    RestoreExpressionParseState();
                    AddChild(win);
                    win.SetParent(this);
                    dwt.simp = null;
                    dwt.win = win;
                    drawWindows.Append(dwt);
                } else if (token.equals("gameSSDDef")) {
                    idGameSSDWindow win = new idGameSSDWindow(dc, gui);
                    SaveExpressionParseState();
                    win.Parse(src, rebuild);
                    RestoreExpressionParseState();
                    AddChild(win);
                    win.SetParent(this);
                    dwt.simp = null;
                    dwt.win = win;
                    drawWindows.Append(dwt);
                } else if (token.equals("gameBearShootDef")) {
                    idGameBearShootWindow win = new idGameBearShootWindow(dc, gui);
                    SaveExpressionParseState();
                    win.Parse(src, rebuild);
                    RestoreExpressionParseState();
                    AddChild(win);
                    win.SetParent(this);
                    dwt.simp = null;
                    dwt.win = win;
                    drawWindows.Append(dwt);
                } else if (token.equals("gameBustOutDef")) {
                    idGameBustOutWindow win = new idGameBustOutWindow(dc, gui);
                    SaveExpressionParseState();
                    win.Parse(src, rebuild);
                    RestoreExpressionParseState();
                    AddChild(win);
                    win.SetParent(this);
                    dwt.simp = null;
                    dwt.win = win;
                    drawWindows.Append(dwt);
                } // 
                //  added new onEvent
                else if (token.equals("onNamedEvent")) {
                    // Read the event name
                    if (!src.ReadToken(token)) {
                        src.Error("Expected event name");
                        return false;
                    }

                    rvNamedEvent ev = new rvNamedEvent(token.toString());

                    src.SetMarker();

                    if (!ParseScript(src, ev.mEvent)) {
                        ret = false;
                        break;
                    }

                    // If we are in the gui editor then add the internal var to the 
                    // the wrapper
//                    if (ID_ALLOW_TOOLS) {
//                        if ((com_editors & EDITOR_GUI) != 0) {
//                            idStr str = new idStr();
//                            idStr out = new idStr();
//
//                            // Grab the string from the last marker
//                            src.GetStringFromMarker(str, false);
//
//                            // Parse it one more time to knock unwanted tabs out
//                            idLexer src2 = new idLexer(str.toString(), str.Length(), "", src.GetFlags());
//                            src2.ParseBracedSectionExact(out, 1);
//
//                            // Save the script		
//                            rvGEWindowWrapper.GetWrapper(this).GetScriptDict().Set(va("onEvent %s", token.c_str()), out);
//                        }
//                    }
                    namedEvents.Append(ev);
                } else if (token.equals("onTime")) {
                    idTimeLineEvent ev = new idTimeLineEvent();

                    if (!src.ReadToken(token)) {
                        src.Error("Unexpected end of file");
                        return false;
                    }
                    ev.time = atoi(token.toString());

                    // reset the mark since we dont want it to include the time
                    src.SetMarker();

                    if (!ParseScript(src, ev.event, null/*ev.time*/)) {
                        ret = false;
                        break;
                    }

                    // add the script to the wrappers script list
                    // If we are in the gui editor then add the internal var to the 
                    // the wrapper
//                    if (ID_ALLOW_TOOLS) {
//                        if ((com_editors & EDITOR_GUI) != 0) {
//                            idStr str = new idStr();
//                            idStr out = new idStr();
//
//                            // Grab the string from the last marker
//                            src.GetStringFromMarker(str, false);
//
//                            // Parse it one more time to knock unwanted tabs out
//                            idLexer src2 = new idLexer(str.toString(), str.Length(), "", src.GetFlags());
//                            src2.ParseBracedSectionExact(out, 1);
//
//                            // Save the script		
//                            rvGEWindowWrapper.GetWrapper(this).GetScriptDict().Set(va("onTime %d", ev.time), out);
//                        }
//                    }
                    // this is a timeline event
                    ev.pending = true;
//                    System.out.println("pending +++++++++ " + ev);
                    timeLineEvents.Append(ev);
                } else if (token.equals("definefloat")) {
                    src.ReadToken(token);
                    work = token;
                    work.ToLower();
                    idWinFloat varf = new idWinFloat();
                    varf.SetName(work.toString());
                    definedVars.Append(varf);

                    // add the float to the editors wrapper dict
                    // Set the marker after the float name
                    src.SetMarker();

                    // Read in the float 
                    regList.AddReg(work.toString(), etoi(FLOAT), src, this, varf);

                    // If we are in the gui editor then add the float to the defines
//                    if (ID_ALLOW_TOOLS) {
//                        if ((com_editors & EDITOR_GUI) != 0) {
//                            idStr str;
//
//                            // Grab the string from the last marker and save it in the wrapper
//                            src.GetStringFromMarker(str, true);
//                            rvGEWindowWrapper.GetWrapper(this).GetVariableDict().Set(va("definefloat\t\"%s\"", token.c_str()), str);
//                        }
//                    }
                } else if (token.equals("definevec4")) {
                    src.ReadToken(token);
                    work = token;
                    work.ToLower();
                    idWinVec4 var = new idWinVec4();
                    var.SetName(work.toString());

                    // set the marker so we can determine what was parsed
                    // set the marker after the vec4 name
                    src.SetMarker();

                    // FIXME: how about we add the var to the desktop instead of this window so it won't get deleted
                    //        when this window is destoyed which even happens during parsing with simple windows ?
                    //definedVars.Append(var);
                    gui.GetDesktop().definedVars.Append(var);
                    gui.GetDesktop().regList.AddReg(work.toString(), etoi(VEC4), src, gui.GetDesktop(), var);

                    // store the original vec4 for the editor
                    // If we are in the gui editor then add the float to the defines
//                    if (ID_ALLOW_TOOLS) {
//                        if ((com_editors & EDITOR_GUI) != 0) {
//                            idStr str = new idStr();
//
//                            // Grab the string from the last marker and save it in the wrapper
//                            src.GetStringFromMarker(str, true);
//                            rvGEWindowWrapper.GetWrapper(this).GetVariableDict().Set(va("definevec4\t\"%s\"", token.c_str()), str);
//                        }
//                    }
                } else if (token.equals("float")) {
                    src.ReadToken(token);
                    work = token;
                    work.ToLower();
                    idWinFloat varf = new idWinFloat();
                    varf.SetName(work.toString());
                    definedVars.Append(varf);

                    // add the float to the editors wrapper dict
                    // set the marker to after the float name
                    src.SetMarker();

                    // Parse the float
                    regList.AddReg(work.toString(), etoi(FLOAT), src, this, varf);

                    // If we are in the gui editor then add the float to the defines
//                    if (ID_ALLOW_TOOLS) {
//                        if ((com_editors & EDITOR_GUI) != 0) {
//                            idStr str;
//
//                            // Grab the string from the last marker and save it in the wrapper
//                            src.GetStringFromMarker(str, true);
//                            rvGEWindowWrapper.GetWrapper(this).GetVariableDict().Set(va("float\t\"%s\"", token.c_str()), str);
//                        }
//                    }
                } else if (ParseScriptEntry(token.toString(), src)) {
                    // add the script to the wrappers script list
                    // If we are in the gui editor then add the internal var to the 
                    // the wrapper
//                    if (ID_ALLOW_TOOLS) {
//                        if ((com_editors & EDITOR_GUI) != 0) {
//                            idStr str = new idStr();
//                            idStr out = new idStr();
//
//                            // Grab the string from the last marker
//                            src.GetStringFromMarker(str, false);
//
//                            // Parse it one more time to knock unwanted tabs out
//                            idLexer src2 = new idLexer(str.toString(), str.Length(), "", src.GetFlags());
//                            src2.ParseBracedSectionExact(out, 1);
//
//                            // Save the script		
//                            rvGEWindowWrapper.GetWrapper(this).GetScriptDict().Set(token, out);
//                        }
//                    }
                } else if (ParseInternalVar(token.toString(), src)) {
                    // gui editor support		
                    // If we are in the gui editor then add the internal var to the 
                    // the wrapper
//                    if (ID_ALLOW_TOOLS) {
//                        if ((com_editors & EDITOR_GUI) != 0) {
//                            idStr str = new idStr();
//                            src.GetStringFromMarker(str);
//                            rvGEWindowWrapper.GetWrapper(this).SetStateKey(token, str, false);
//                        }
//                    }
                } else {
                    ParseRegEntry(token.toString(), src);
                    // hook into the main window parsing for the gui editor
                    // If we are in the gui editor then add the internal var to the 
                    // the wrapper
//                    if (ID_ALLOW_TOOLS) {
//                        if ((com_editors & EDITOR_GUI) != 0) {
//                            idStr str;
//                            src.GetStringFromMarker(str);
//                            rvGEWindowWrapper.GetWrapper(this).SetStateKey(token, str, false);
//                        }
//                    }
                }
                if (!src.ReadToken(token)) {
                    src.Error("Unexpected end of file");
                    ret = false;
                    break;
                }
            }

            if (ret) {
                EvalRegs(-1, true);
            }

            SetupFromState();
            PostParse();

            // hook into the main window parsing for the gui editor
            // If we are in the gui editor then add the internal var to the 
            // the wrapper
//            if (ID_ALLOW_TOOLS) {
//                if ((com_editors & EDITOR_GUI) != 0) {
//                    rvGEWindowWrapper.GetWrapper(this).Finish();
//                }
//            }
//
            return ret;
        }

        public boolean Parse(idParser src) {
            return Parse(src, true);
        }
        private static boolean actionDownRun;
        private static boolean actionUpRun;

        public String HandleEvent(final sysEvent_s event, boolean[] updateVisuals) {

            cmd.oSet("");

            if ((flags & WIN_DESKTOP) != 0) {
                actionDownRun = false;
                actionUpRun = false;
                if (expressionRegisters.Num() != 0 && ops.Num() != 0) {
                    EvalRegs();
                }
                RunTimeEvents(gui.GetTime());
                CalcRects(0, 0);
                dc.SetCursor(etoi(CURSOR_ARROW));
            }

            if (visible.data && !noEvents.data) {

                if (event.evType == SE_KEY) {
                    EvalRegs(-1, true);
                    if (updateVisuals != null) {
                        updateVisuals[0] = true;
                    }

                    if (event.evValue == K_MOUSE1) {

                        if (0 == event.evValue2 && GetCaptureChild() != null) {
                            GetCaptureChild().LoseCapture();
                            gui.GetDesktop().captureChild = null;
                            return "";
                        }

                        int c = children.Num();
                        while (--c >= 0) {
                            if (children.oGet(c).visible.data
                                    && children.oGet(c).Contains(children.oGet(c).drawRect, gui.CursorX(), gui.CursorY())
                                    && !(children.oGet(c).noEvents.data)) {
                                idWindow child = children.oGet(c);
                                if (event.evValue2 != 0) {
                                    BringToTop(child);
                                    SetFocus(child);
                                    if ((child.flags & WIN_HOLDCAPTURE) != 0) {
                                        SetCapture(child);
                                    }
                                }
                                if (child.Contains(child.clientRect, gui.CursorX(), gui.CursorY())) {
                                    //if ((gui_edit.GetBool() && (child.flags & WIN_SELECTED)) || (!gui_edit.GetBool() && (child.flags & WIN_MOVABLE))) {
                                    //	SetCapture(child);
                                    //}
                                    SetFocus(child);
                                    final String childRet = child.HandleEvent(event, updateVisuals);
                                    if (childRet != null && !childRet.isEmpty()) {
                                        return childRet;
                                    }
                                    if ((child.flags & WIN_MODAL) != 0) {
                                        return "";
                                    }
                                } else {
                                    if (event.evValue2 != 0) {
                                        SetFocus(child);
                                        boolean capture = true;
                                        if (capture && ((child.flags & WIN_MOVABLE) != 0 || gui_edit.GetBool())) {
                                            SetCapture(child);
                                        }
                                        return "";
                                    } else {
                                    }
                                }
                            }
                        }
                        if (event.evValue2 != 0 && !actionDownRun) {
                            actionDownRun = RunScript(ON_ACTION);
                        } else if (!actionUpRun) {
                            actionUpRun = RunScript(ON_ACTIONRELEASE);
                        }
                    } else if (event.evValue == K_MOUSE2) {

                        if (0 == event.evValue2 && GetCaptureChild() != null) {
                            GetCaptureChild().LoseCapture();
                            gui.GetDesktop().captureChild = null;
                            return "";
                        }

                        int c = children.Num();
                        while (--c >= 0) {
                            if (children.oGet(c).visible.data
                                    && children.oGet(c).Contains(children.oGet(c).drawRect, gui.CursorX(), gui.CursorY())
                                    && !(children.oGet(c).noEvents.data)) {
                                idWindow child = children.oGet(c);
                                if (event.evValue2 != 0) {
                                    BringToTop(child);
                                    SetFocus(child);
                                }
                                if (child.Contains(child.clientRect, gui.CursorX(), gui.CursorY()) || GetCaptureChild() == child) {
                                    if ((gui_edit.GetBool() && (child.flags & WIN_SELECTED) != 0) || (!gui_edit.GetBool() && (child.flags & WIN_MOVABLE) != 0)) {
                                        SetCapture(child);
                                    }
                                    final String childRet = child.HandleEvent(event, updateVisuals);
                                    if (childRet != null && !childRet.isEmpty()) {
                                        return childRet;
                                    }
                                    if ((child.flags & WIN_MODAL) != 0) {
                                        return "";
                                    }
                                }
                            }
                        }
                    } else if (event.evValue == K_MOUSE3) {
                        if (gui_edit.GetBool()) {
                            int c = children.Num();
                            for (int i = 0; i < c; i++) {
                                if (children.oGet(i).drawRect.Contains(gui.CursorX(), gui.CursorY())) {
                                    if (event.evValue2 != 0) {
                                        children.oGet(i).flags ^= WIN_SELECTED;
                                        if ((children.oGet(i).flags & WIN_SELECTED) != 0) {
                                            flags &= ~WIN_SELECTED;
                                            return "childsel";
                                        }
                                    }
                                }
                            }
                        }
                    } else if (event.evValue == K_TAB && event.evValue2 != 0) {
                        if (GetFocusedChild() != null) {
                            final String childRet = GetFocusedChild().HandleEvent(event, updateVisuals);
                            if (childRet != null && !childRet.isEmpty()) {
                                return childRet;
                            }

                            // If the window didn't handle the tab, then move the focus to the next window
                            // or the previous window if shift is held down
                            int direction = 1;
                            if (idKeyInput.IsDown(K_SHIFT)) {
                                direction = -1;
                            }

                            idWindow currentFocus = GetFocusedChild();
                            idWindow child = GetFocusedChild();
                            idWindow parent = child.GetParent();
                            while (parent != null) {
                                boolean foundFocus = false;
                                boolean recurse = false;
                                int index = 0;
                                if (child != null) {
                                    index = parent.GetChildIndex(child) + direction;
                                } else if (direction < 0) {
                                    index = parent.GetChildCount() - 1;
                                }
                                while (index < parent.GetChildCount() && index >= 0) {
                                    idWindow testWindow = parent.GetChild(index);
                                    if (testWindow == currentFocus) {
                                        // we managed to wrap around and get back to our starting window
                                        foundFocus = true;
                                        break;
                                    }
                                    if (testWindow != null && !testWindow.noEvents.data && testWindow.visible.data) {
                                        if ((testWindow.flags & WIN_CANFOCUS) != 0) {
                                            SetFocus(testWindow);
                                            foundFocus = true;
                                            break;
                                        } else if (testWindow.GetChildCount() > 0) {
                                            parent = testWindow;
                                            child = null;
                                            recurse = true;
                                            break;
                                        }
                                    }
                                    index += direction;
                                }
                                if (foundFocus) {
                                    // We found a child to focus on
                                    break;
                                } else if (recurse) {
                                    // We found a child with children
                                    continue;
                                } else {
                                    // We didn't find anything, so go back up to our parent
                                    child = parent;
                                    parent = child.GetParent();
                                    if (parent == gui.GetDesktop()) {
                                        // We got back to the desktop, so wrap around but don't actually go to the desktop
                                        parent = null;
                                        child = null;
                                    }
                                }
                            }
                        }
                    } else if (event.evValue == K_ESCAPE && event.evValue2 != 0) {
                        if (GetFocusedChild() != null) {
                            final String childRet = GetFocusedChild().HandleEvent(event, updateVisuals);
                            if (childRet != null && !childRet.isEmpty()) {
                                return childRet;
                            }
                        }
                        RunScript(ON_ESC);
                    } else if (event.evValue == K_ENTER) {
                        if (GetFocusedChild() != null) {
                            final String childRet = GetFocusedChild().HandleEvent(event, updateVisuals);
                            if (childRet != null && !childRet.isEmpty()) {
                                return childRet;
                            }
                        }
                        if ((flags & WIN_WANTENTER) != 0) {
                            if (event.evValue2 != 0) {
                                RunScript(ON_ACTION);
                            } else {
                                RunScript(ON_ACTIONRELEASE);
                            }
                        }
                    } else {
                        if (GetFocusedChild() != null) {
                            final String childRet = GetFocusedChild().HandleEvent(event, updateVisuals);
                            if (childRet != null && !childRet.isEmpty()) {
                                return childRet;
                            }
                        }
                    }

                } else if (event.evType == SE_MOUSE) {
                    if (updateVisuals != null) {
                        updateVisuals[0] = true;
                    }
                    final String mouseRet = RouteMouseCoords(event.evValue, event.evValue2);
                    if (mouseRet != null && !mouseRet.isEmpty()) {
                        return mouseRet;
                    }
                } else if (event.evType == SE_NONE) {
                } else if (event.evType == SE_CHAR) {
                    if (GetFocusedChild() != null) {
                        final String childRet = GetFocusedChild().HandleEvent(event, updateVisuals);
                        if (childRet != null && !childRet.isEmpty()) {
                            return childRet;
                        }
                    }
                }
            }

            gui.GetReturnCmd().oSet(cmd);
            if (gui.GetPendingCmd().Length() != 0) {
                gui.GetReturnCmd().oPluSet(" ; ");
                gui.GetReturnCmd().oPluSet(gui.GetPendingCmd());
                gui.GetPendingCmd().Clear();
            }
            cmd.oSet("");
            return gui.GetReturnCmd().toString();
        }

        public void CalcRects(float x, float y) {
            CalcClientRect(0, 0);
            drawRect.Offset(x, y);
            clientRect.Offset(x, y);
            actualX = drawRect.x;
            actualY = drawRect.y;
            int c = drawWindows.Num();
            for (int i = 0; i < c; i++) {
                if (drawWindows.oGet(i).win != null) {
                    drawWindows.oGet(i).win.CalcRects(clientRect.x + xOffset, clientRect.y + yOffset);
                }
            }
            drawRect.Offset(-x, -y);
            clientRect.Offset(-x, -y);
        }
        public static int bla1 = 0, bla2 = 0, drawCursorTotal = 0;

        public void Redraw(float x, float y) {
            idStr str;

            if (r_skipGuiShaders.GetInteger() == 1 || dc == null) {
                return;
            }

            int time = gui.GetTime();

            if (((flags & WIN_DESKTOP) != 0) && r_skipGuiShaders.GetInteger() != 3) {
                RunTimeEvents(time);
            }

            if (r_skipGuiShaders.GetInteger() == 2) {
                return;
            }

            if ((flags & WIN_SHOWTIME) != 0) {
                dc.DrawText(va(" %0.1f seconds\n%s", (float) (time - timeLine) / 1000, gui.State().GetString("name")), 0.35f, 0, dc.colorWhite, new idRectangle(100, 0, 80, 80), false);
            }

            if ((flags & WIN_SHOWCOORDS) != 0) {
                dc.EnableClipping(false);
                str = new idStr(String.format("x: %d y: %d  cursorx: %d cursory: %d", (int) rect.x(), (int) rect.y(), (int) gui.CursorX(), (int) gui.CursorY()));
                dc.DrawText(str.toString(), 0.25f, 0, dc.colorWhite, new idRectangle(0, 0, 100, 20), false);
                dc.EnableClipping(true);
            }

            if (!visible.data) {
                return;
            }

            CalcClientRect(0, 0);

            SetFont();
            //if (flags & WIN_DESKTOP) {
            // see if this window forces a new aspect ratio
            dc.SetSize(forceAspectWidth, forceAspectHeight);
            //}

            //FIXME: go to screen coord tracking
            drawRect.Offset(x, y);
            clientRect.Offset(x, y);
            textRect.Offset(x, y);
            actualX = drawRect.x;
            actualY = drawRect.y;

            idVec3 oldOrg = new idVec3();
            idMat3 oldTrans = new idMat3();

            dc.GetTransformInfo(oldOrg, oldTrans);

            SetupTransforms(x, y);
            DrawBackground(drawRect);
            DrawBorderAndCaption(drawRect);

            if (0 == (flags & WIN_NOCLIP)) {
                dc.PushClipRect(clientRect);
            }

            if (r_skipGuiShaders.GetInteger() < 5) {
//                bla++;
                Draw(time, x, y);
            }

            if (gui_debug.GetInteger() != 0) {
                DebugDraw(time, x, y);
            }

            int c = drawWindows.Num();
            for (int i = 0; i < c; i++) {
                if (drawWindows.oGet(i).win != null) {
                    bla1++;
                    drawWindows.oGet(i).win.Redraw(clientRect.x + xOffset, clientRect.y + yOffset);
                } else {
                    bla2++;
                    drawWindows.oGet(i).simp.Redraw(clientRect.x + xOffset, clientRect.y + yOffset);
                }
            }

            // Put transforms back to what they were before the children were processed
            dc.SetTransformInfo(oldOrg, oldTrans);

            if (0 == (flags & WIN_NOCLIP)) {
                dc.PopClipRect();
            }

            drawCursorTotal++;
            if (gui_edit.GetBool()
                    || ((flags & WIN_DESKTOP) != 0
                    && 0 == (flags & WIN_NOCURSOR)
                    && !hideCursor.data
                    && (gui.Active() || ((flags & WIN_MENUGUI) != 0)))) {
                dc.SetTransformInfo(getVec3_origin(), getMat3_identity());
                gui.DrawCursor();
            }

            if (gui_debug.GetInteger() != 0 && (flags & WIN_DESKTOP) != 0) {
                dc.EnableClipping(false);
                str = new idStr(String.format("x: %1.f y: %1.f", gui.CursorX(), gui.CursorY()));
                dc.DrawText(str.toString(), 0.25f, 0, dc.colorWhite, new idRectangle(0, 0, 100, 20), false);
                dc.DrawText(gui.GetSourceFile(), 0.25f, 0, dc.colorWhite, new idRectangle(0, 20, 300, 20), false);
                dc.EnableClipping(true);
            }

            drawRect.Offset(-x, -y);
            clientRect.Offset(-x, -y);
            textRect.Offset(-x, -y);
        }

        public void ArchiveToDictionary(idDict dict, boolean useNames /*= true*/) {
            //FIXME: rewrite without state
            int c = children.Num();
            for (int i = 0; i < c; i++) {
                children.oGet(i).ArchiveToDictionary(dict);
            }
        }

        public void ArchiveToDictionary(idDict dict) {
            ArchiveToDictionary(dict, true);
        }

        public void InitFromDictionary(idDict dict, boolean byName /*= true*/) {
            //FIXME: rewrite without state
            int c = children.Num();
            for (int i = 0; i < c; i++) {
                children.oGet(i).InitFromDictionary(dict);
            }
        }

        public void InitFromDictionary(idDict dict) {
            InitFromDictionary(dict, true);
        }

        public void PostParse() {
        }

        static int DEBUG_Activate = 0;

        public void Activate(boolean activate, idStr act) {
            DEBUG_Activate++;

            final int n = ((activate) ? ON_ACTIVATE : ON_DEACTIVATE).ordinal();

            //  make sure win vars are updated before activation
            UpdateWinVars();

            RunScript(n);
            int c = children.Num();
            for (int i = 0; i < c; i++) {
                children.oGet(i).Activate(activate, act);
            }

            if (act.Length() != 0) {
                act.Append(" ; ");
            }
        }

        public void Trigger() {
            RunScript(ON_TRIGGER);
            int c = children.Num();
            for (int i = 0; i < c; i++) {
                children.oGet(i).Trigger();
            }
            StateChanged(true);
        }

        public void GainFocus() {
        }

        public void LoseFocus() {
        }

        public void GainCapture() {
        }

        public void LoseCapture() {
            flags &= ~WIN_CAPTURE;
        }

        public void Sized() {
        }

        public void Moved() {
        }

        public void Draw(int time, float x, float y) {
            if (text.Length() == 0) {
                return;
            }
            if (textShadow != 0) {
                idStr shadowText = new idStr(text.data);
                idRectangle shadowRect = textRect;

                shadowText.RemoveColors();
                shadowRect.x += textShadow;
                shadowRect.y += textShadow;

                dc.DrawText(shadowText.toString(), textScale.data, textAlign, colorBlack, shadowRect, !itob(flags & WIN_NOWRAP), -1);
            }
            dc.DrawText(text.data.toString(), textScale.data, textAlign, foreColor.data, textRect, !itob(flags & WIN_NOWRAP), -1);

            if (gui_edit.GetBool()) {
                dc.EnableClipping(false);
                dc.DrawText(va("x: %d  y: %d", (int) rect.x(), (int) rect.y()), 0.25f, 0, dc.colorWhite, new idRectangle(rect.x(), rect.y() - 15, 100, 20), false);
                dc.DrawText(va("w: %d  h: %d", (int) rect.w(), (int) rect.h()), 0.25f, 0, dc.colorWhite, new idRectangle(rect.x() + rect.w(), rect.w() + rect.h() + 5, 100, 20), false);
                dc.EnableClipping(true);
            }

        }

        public void MouseExit() {

            if (noEvents.data) {
                return;
            }

            RunScript(ON_MOUSEEXIT);
        }

        public void MouseEnter() {

            if (noEvents.data) {
                return;
            }

            RunScript(ON_MOUSEENTER);
        }

        public void DrawBackground(final idRectangle drawRect) {
            if (backColor.w() != 0) {
                dc.DrawFilledRect(drawRect.x, drawRect.y, drawRect.w, drawRect.h, backColor.data);
            }

            if (background != null && matColor.w() != 0) {
                float scalex, scaley;
                if ((flags & WIN_NATURALMAT) != 0) {
                    scalex = drawRect.w / background.GetImageWidth();
                    scaley = drawRect.h / background.GetImageHeight();
                } else {
                    scalex = matScalex;
                    scaley = matScaley;
                }
                dc.DrawMaterial(drawRect.x, drawRect.y, drawRect.w, drawRect.h, background, matColor.data, scalex, scaley);
            }
        }

        public String RouteMouseCoords(float xd, float yd) {
            String str;
            if (GetCaptureChild() != null) {
                //FIXME: unkludge this whole mechanism
                return GetCaptureChild().RouteMouseCoords(xd, yd);
            }

            if (xd == -2000 || yd == -2000) {
                return "";
            }

            int c = children.Num();
            while (c > 0) {
                idWindow child = children.oGet(--c);
                if (child.visible.data && !child.noEvents.data && child.Contains(child.drawRect, gui.CursorX(), gui.CursorY())) {

                    dc.SetCursor(child.cursor);
                    child.hover = true;

                    if (overChild != child) {
                        if (overChild != null) {
                            overChild.MouseExit();
                            str = overChild.cmd.toString();
                            if (isNotNullOrEmpty(str)) {
                                gui.GetDesktop().AddCommand(str);
                                overChild.cmd.oSet("");
                            }
                        }
                        overChild = child;
                        overChild.MouseEnter();
                        str = overChild.cmd.toString();
                        if (isNotNullOrEmpty(str)) {
                            gui.GetDesktop().AddCommand(str);
                            overChild.cmd.oSet("");
                        }
                    } else {
                        if (0 == (child.flags & WIN_HOLDCAPTURE)) {
                            child.RouteMouseCoords(xd, yd);
                        }
                    }
                    return "";
                }
            }
            if (overChild != null) {
                overChild.MouseExit();
                str = overChild.cmd.toString();
                if (isNotNullOrEmpty(str)) {
                    gui.GetDesktop().AddCommand(str);
                    overChild.cmd.oSet("");
                }
                overChild = null;
            }
            return "";
        }

        public void SetBuddy(idWindow buddy) {
        }

        public void HandleBuddyUpdate(idWindow buddy) {
        }

        public void StateChanged(boolean redraw) {

            UpdateWinVars();

            if (expressionRegisters.Num() != 0 && ops.Num() != 0) {
                EvalRegs();
            }

            int c = drawWindows.Num();
            for (int i = 0; i < c; i++) {
                if (drawWindows.oGet(i).win != null) {
                    drawWindows.oGet(i).win.StateChanged(redraw);
                } else {
                    drawWindows.oGet(i).simp.StateChanged(redraw);
                }
            }

            if (redraw) {
                if ((flags & WIN_DESKTOP) != 0) {
                    Redraw(0.0f, 0.0f);
                }
                if (background != null && background.CinematicLength() != 0) {
                    background.UpdateCinematic(gui.GetTime());
                }
            }
        }

        public void ReadFromDemoFile(idDemoFile f, boolean rebuild /*= true*/) {

            // should never hit unless we re-enable WRITE_GUIS
            if (!WRITE_GUIS) {
                assert (false);
//}else{
//
//	if (rebuild) {
//		CommonInit();
//	}
//	
//	f.SetLog(true, "window1");
//	backGroundName = f.ReadHashString();
//	f.SetLog(true, backGroundName);
//	if ( backGroundName[0] ) {
//		background = declManager.FindMaterial(backGroundName);
//	} else {
//		background = null;
//	}
//	f.ReadUnsignedChar( cursor );
//	f.ReadUnsignedInt( flags );
//	f.ReadInt( timeLine );
//	f.ReadInt( lastTimeRun );
//	idRectangle rct = rect;
//	f.ReadFloat( rct.x );
//	f.ReadFloat( rct.y );
//	f.ReadFloat( rct.w );
//	f.ReadFloat( rct.h );
//	f.ReadFloat( drawRect.x );
//	f.ReadFloat( drawRect.y );
//	f.ReadFloat( drawRect.w );
//	f.ReadFloat( drawRect.h );
//	f.ReadFloat( clientRect.x );
//	f.ReadFloat( clientRect.y );
//	f.ReadFloat( clientRect.w );
//	f.ReadFloat( clientRect.h );
//	f.ReadFloat( textRect.x );
//	f.ReadFloat( textRect.y );
//	f.ReadFloat( textRect.w );
//	f.ReadFloat( textRect.h );
//	f.ReadFloat( xOffset);
//	f.ReadFloat( yOffset);
//	int i, c;
//
//	idStr work;
//	if (rebuild) {
//		f.SetLog(true, (work + "-scripts"));
//		for (i = 0; i < SCRIPT_COUNT; i++) {
//			bool b;
//			f.ReadBool( b );
//			if (b) {
////				delete scripts[i];
//				scripts[i] = new idGuiScriptList;
//				scripts[i].ReadFromDemoFile(f);
//			}
//		}
//
//		f.SetLog(true, (work + "-timelines"));
//		f.ReadInt( c );
//		for (i = 0; i < c; i++) {
//			idTimeLineEvent *tl = new idTimeLineEvent;
//			f.ReadInt( tl.time );
//			f.ReadBool( tl.pending );
//			tl.event.ReadFromDemoFile(f);
//			if (rebuild) {
//				timeLineEvents.Append(tl);
//			} else {
//				assert(i < timeLineEvents.Num());
//				timeLineEvents[i].time = tl.time;
//				timeLineEvents[i].pending = tl.pending;
//			}
//		}
//	}
//
//	f.SetLog(true, (work + "-transitions"));
//	f.ReadInt( c );
//	for (i = 0; i < c; i++) {
//		idTransitionData td;
//		td.data = NULL;
//		f.ReadInt ( td.offset );
//
//		float startTime, accelTime, linearTime, decelTime;
//		idVec4 startValue, endValue;	   
//		f.ReadFloat( startTime );
//		f.ReadFloat( accelTime );
//		f.ReadFloat( linearTime );
//		f.ReadFloat( decelTime );
//		f.ReadVec4( startValue );
//		f.ReadVec4( endValue );
//		td.interp.Init( startTime, accelTime, decelTime, accelTime + linearTime + decelTime, startValue, endValue );
//		
//		// read this for correct data padding with the win32 savegames
//		// the extrapolate is correctly initialized through the above Init call
//		int extrapolationType;
//		float duration;
//		idVec4 baseSpeed, speed;
//		float currentTime;
//		idVec4 currentValue;
//		f.ReadInt( extrapolationType );
//		f.ReadFloat( startTime );
//		f.ReadFloat( duration );
//		f.ReadVec4( startValue );
//		f.ReadVec4( baseSpeed );
//		f.ReadVec4( speed );
//		f.ReadFloat( currentTime );
//		f.ReadVec4( currentValue );
//
//		transitions.Append(td);
//	}
//
//	f.SetLog(true, (work + "-regstuff"));
//	if (rebuild) {
//		f.ReadInt( c );
//		for (i = 0; i < c; i++) {
//			wexpOp_t w;
//			f.ReadInt( (int&)w.opType );
//			f.ReadInt( w.a );
//			f.ReadInt( w.b );
//			f.ReadInt( w.c );
//			f.ReadInt( w.d );
//			ops.Append(w);
//		}
//
//		f.ReadInt( c );
//		for (i = 0; i < c; i++) {
//			float ff;
//			f.ReadFloat( ff );
//			expressionRegisters.Append(ff);
//		}
//	
//		regList.ReadFromDemoFile(f);
//
//	}
//	f.SetLog(true, (work + "-children"));
//	f.ReadInt( c );
//	for (i = 0; i < c; i++) {
//		if (rebuild) {
//			idWindow *win = new idWindow(dc, gui);
//			win.ReadFromDemoFile(f);
//			AddChild(win);
//		} else {
//			for (int j = 0; j < c; j++) {
//				if (children[j].childID == i) {
//					children[j].ReadFromDemoFile(f,rebuild);
//					break;
//				} else {
//					continue;
//				}
//			}
//		}
//	}
            } /* WRITE_GUIS */

        }

        public void ReadFromDemoFile(idDemoFile f) {
            ReadFromDemoFile(f, true);
        }

        public void WriteToDemoFile(idDemoFile f) {
            // should never hit unless we re-enable WRITE_GUIS
            if (WRITE_GUIS) {
                assert (false);
//}else{
//
//	f->SetLog(true, "window");
//	f->WriteHashString(backGroundName);
//	f->SetLog(true, backGroundName);
//	f->WriteUnsignedChar( cursor );
//	f->WriteUnsignedInt( flags );
//	f->WriteInt( timeLine );
//	f->WriteInt( lastTimeRun );
//	idRectangle rct = rect;
//	f->WriteFloat( rct.x );
//	f->WriteFloat( rct.y );
//	f->WriteFloat( rct.w );
//	f->WriteFloat( rct.h );
//	f->WriteFloat( drawRect.x );
//	f->WriteFloat( drawRect.y );
//	f->WriteFloat( drawRect.w );
//	f->WriteFloat( drawRect.h );
//	f->WriteFloat( clientRect.x );
//	f->WriteFloat( clientRect.y );
//	f->WriteFloat( clientRect.w );
//	f->WriteFloat( clientRect.h );
//	f->WriteFloat( textRect.x );
//	f->WriteFloat( textRect.y );
//	f->WriteFloat( textRect.w );
//	f->WriteFloat( textRect.h );
//	f->WriteFloat( xOffset );
//	f->WriteFloat( yOffset );
//	idStr work;
//	f->SetLog(true, work);
//
// 	int i, c;
//
//	f->SetLog(true, (work + "-transitions"));
//	c = transitions.Num();
//	f->WriteInt( c );
//	for (i = 0; i < c; i++) {
//		f->WriteInt( 0 );
//		f->WriteInt( transitions[i].offset );
//		
//		f->WriteFloat( transitions[i].interp.GetStartTime() );
//		f->WriteFloat( transitions[i].interp.GetAccelTime() );
//		f->WriteFloat( transitions[i].interp.GetLinearTime() );
//		f->WriteFloat( transitions[i].interp.GetDecelTime() );
//		f->WriteVec4( transitions[i].interp.GetStartValue() );
//		f->WriteVec4( transitions[i].interp.GetEndValue() );
//
//		// write to keep win32 render demo format compatiblity - we don't actually read them back anymore
//		f->WriteInt( transitions[i].interp.GetExtrapolate()->GetExtrapolationType() );
//		f->WriteFloat( transitions[i].interp.GetExtrapolate()->GetStartTime() );
//		f->WriteFloat( transitions[i].interp.GetExtrapolate()->GetDuration() );
//		f->WriteVec4( transitions[i].interp.GetExtrapolate()->GetStartValue() );
//		f->WriteVec4( transitions[i].interp.GetExtrapolate()->GetBaseSpeed() );
//		f->WriteVec4( transitions[i].interp.GetExtrapolate()->GetSpeed() );
//		f->WriteFloat( transitions[i].interp.GetExtrapolate()->GetCurrentTime() );
//		f->WriteVec4( transitions[i].interp.GetExtrapolate()->GetCurrentValue() );
//	}
//
//	f->SetLog(true, (work + "-regstuff"));
//
//	f->SetLog(true, (work + "-children"));
//	c = children.Num();
//	f->WriteInt( c );
//	for (i = 0; i < c; i++) {
//		for (int j = 0; j < c; j++) {
//			if (children[j]->childID == i) {
//				children[j]->WriteToDemoFile(f);
//				break;
//			} else {
//				continue;
//			}
//		}
//	}
            } /* WRITE_GUIS */

        }

        // SaveGame support
        public void WriteSaveGameString(final String string, idFile savefile) {
            int len = string.length();

            savefile.WriteInt(len);
            savefile.WriteString(string);
        }

        public void WriteSaveGameString(final idStr string, idFile savefile) {
            WriteSaveGameString(string.toString(), savefile);
        }

        public void WriteSaveGameTransition(idTransitionData trans, idFile savefile) {
            drawWin_t dw = new drawWin_t(), fdw;
            idStr winName = new idStr("");
            dw.simp = null;
            dw.win = null;
            int offset = gui.GetDesktop().GetWinVarOffset(trans.data, dw);
            if (dw.win != null || dw.simp != null) {
                winName = new idStr((dw.win != null) ? dw.win.GetName() : dw.simp.name.toString());
            }
            fdw = gui.GetDesktop().FindChildByName(winName.toString());
            if (offset != -1 && fdw != null && (fdw.win != null || fdw.simp != null)) {
                savefile.WriteInt(offset);
                WriteSaveGameString(winName.toString(), savefile);
                savefile.Write(trans.interp);
            } else {
                offset = -1;
                savefile.WriteInt(offset);
            }
        }

        public void WriteToSaveGame(idFile savefile) {
            int i;

            WriteSaveGameString(cmd, savefile);

            savefile.WriteFloat(actualX);
            savefile.WriteFloat(actualY);
            savefile.WriteInt(childID);
            savefile.WriteInt(flags);
            savefile.WriteInt(lastTimeRun);
            savefile.Write(drawRect);
            savefile.Write(clientRect);
            savefile.Write(origin);
            savefile.WriteChar(fontNum);
            savefile.WriteInt(timeLine);
            savefile.WriteFloat(xOffset);
            savefile.WriteFloat(yOffset);
            savefile.WriteChar(cursor);
            savefile.WriteFloat(forceAspectWidth);
            savefile.WriteFloat(forceAspectHeight);
            savefile.WriteFloat(matScalex);
            savefile.WriteFloat(matScaley);
            savefile.WriteFloat(borderSize);
            savefile.WriteChar(textAlign);
            savefile.WriteFloat(textAlignx);
            savefile.WriteFloat(textAligny);
            savefile.WriteChar(textShadow);
            savefile.Write(shear);

            WriteSaveGameString(name, savefile);
            WriteSaveGameString(comment, savefile);

            // WinVars
            noTime.WriteToSaveGame(savefile);
            visible.WriteToSaveGame(savefile);
            rect.WriteToSaveGame(savefile);
            backColor.WriteToSaveGame(savefile);
            matColor.WriteToSaveGame(savefile);
            foreColor.WriteToSaveGame(savefile);
            hoverColor.WriteToSaveGame(savefile);
            borderColor.WriteToSaveGame(savefile);
            textScale.WriteToSaveGame(savefile);
            noEvents.WriteToSaveGame(savefile);
            rotate.WriteToSaveGame(savefile);
            text.WriteToSaveGame(savefile);
            backGroundName.WriteToSaveGame(savefile);
            hideCursor.WriteToSaveGame(savefile);

            // Defined Vars
            for (i = 0; i < definedVars.Num(); i++) {
                definedVars.oGet(i).WriteToSaveGame(savefile);
            }

            savefile.Write(textRect);

            // Window pointers saved as the child ID of the window
            int winID;

            winID = focusedChild != null ? focusedChild.childID : -1;
            savefile.WriteInt(winID);

            winID = captureChild != null ? captureChild.childID : -1;
            savefile.WriteInt(winID);

            winID = overChild != null ? overChild.childID : -1;
            savefile.WriteInt(winID);

            // Scripts
            for (i = 0; i < SCRIPT_COUNT.ordinal(); i++) {
                if (scripts[i] != null) {
                    scripts[i].WriteToSaveGame(savefile);
                }
            }

            // TimeLine Events
            for (i = 0; i < timeLineEvents.Num(); i++) {
                if (timeLineEvents.oGet(i) != null) {
                    savefile.WriteBool(timeLineEvents.oGet(i).pending);
                    savefile.WriteInt(timeLineEvents.oGet(i).time);
                    if (timeLineEvents.oGet(i).event != null) {
                        timeLineEvents.oGet(i).event.WriteToSaveGame(savefile);
                    }
                }
            }

            // Transitions
            int num = transitions.Num();

            savefile.WriteInt(num);
            for (i = 0; i < transitions.Num(); i++) {
                WriteSaveGameTransition(transitions.oGet(i), savefile);
            }

            // Named Events
            for (i = 0; i < namedEvents.Num(); i++) {
                if (namedEvents.oGet(i) != null) {
                    WriteSaveGameString(namedEvents.oGet(i).mName.toString(), savefile);
                    if (namedEvents.oGet(i).mEvent != null) {
                        namedEvents.oGet(i).mEvent.WriteToSaveGame(savefile);
                    }
                }
            }

            // regList
            regList.WriteToSaveGame(savefile);

            // Save children
            for (i = 0; i < drawWindows.Num(); i++) {
                drawWin_t window = drawWindows.oGet(i);

                if (window.simp != null) {
                    window.simp.WriteToSaveGame(savefile);
                } else if (window.win != null) {
                    window.win.WriteToSaveGame(savefile);
                }
            }
        }

        public void ReadSaveGameString(idStr string, idFile savefile) {
            int len;

            len = savefile.ReadInt();
            if (len < 0) {
                common.Warning("idWindow::ReadSaveGameString: invalid length");
            }

            string.Fill(' ', len);
            savefile.ReadString(string);//TODO:read to buffer
        }

        public void ReadSaveGameTransition(idTransitionData trans, idFile savefile) {
            int offset;

            offset = savefile.ReadInt();
            if (offset != -1) {
                idStr winName = new idStr();
                ReadSaveGameString(winName, savefile);
                savefile.Read(trans.interp);
                trans.data = null;
                trans.offset = offset;
                if (winName.Length() != 0) {
                    idWinStr strVar = new idWinStr();
                    strVar.Set(winName);
                    trans.data = strVar;
                }
            }
        }

        public void ReadFromSaveGame(idFile savefile) {
            int i;

            transitions.Clear();

            ReadSaveGameString(cmd, savefile);

            actualX = savefile.ReadFloat();
            actualY = savefile.ReadFloat();
            childID = savefile.ReadInt();
            flags = savefile.ReadInt();
            lastTimeRun = savefile.ReadInt();
            savefile.Read(drawRect);
            savefile.Read(clientRect);
            savefile.Read(origin);
            fontNum = (char) savefile.ReadChar();
            timeLine = savefile.ReadInt();
            xOffset = savefile.ReadFloat();
            yOffset = savefile.ReadFloat();
            cursor = (char) savefile.ReadChar();
            forceAspectWidth = savefile.ReadFloat();
            forceAspectHeight = savefile.ReadFloat();
            matScalex = savefile.ReadFloat();
            matScaley = savefile.ReadFloat();
            borderSize = savefile.ReadFloat();
            textAlign = (char) savefile.ReadChar();
            textAlignx = savefile.ReadFloat();
            textAligny = savefile.ReadFloat();
            textShadow = (char) savefile.ReadChar();
            savefile.Read(shear);

            ReadSaveGameString(name, savefile);
            ReadSaveGameString(comment, savefile);

            // WinVars
            noTime.ReadFromSaveGame(savefile);
            visible.ReadFromSaveGame(savefile);
            rect.ReadFromSaveGame(savefile);
            backColor.ReadFromSaveGame(savefile);
            matColor.ReadFromSaveGame(savefile);
            foreColor.ReadFromSaveGame(savefile);
            hoverColor.ReadFromSaveGame(savefile);
            borderColor.ReadFromSaveGame(savefile);
            textScale.ReadFromSaveGame(savefile);
            noEvents.ReadFromSaveGame(savefile);
            rotate.ReadFromSaveGame(savefile);
            text.ReadFromSaveGame(savefile);
            backGroundName.ReadFromSaveGame(savefile);

            if (session.GetSaveGameVersion() >= 17) {
                hideCursor.ReadFromSaveGame(savefile);
            } else {
                hideCursor.data = false;
            }

            // Defined Vars
            for (i = 0; i < definedVars.Num(); i++) {
                definedVars.oGet(i).ReadFromSaveGame(savefile);
            }

            savefile.Read(textRect);

            // Window pointers saved as the child ID of the window
            int winID = -1;

            winID = savefile.ReadInt();
            for (i = 0; i < children.Num(); i++) {
                if (children.oGet(i).childID == winID) {
                    focusedChild = children.oGet(i);
                }
            }
            winID = savefile.ReadInt();
            for (i = 0; i < children.Num(); i++) {
                if (children.oGet(i).childID == winID) {
                    captureChild = children.oGet(i);
                }
            }
            winID = savefile.ReadInt();
            for (i = 0; i < children.Num(); i++) {
                if (children.oGet(i).childID == winID) {
                    overChild = children.oGet(i);
                }
            }

            // Scripts
            for (i = 0; i < SCRIPT_COUNT.ordinal(); i++) {
                if (scripts[i] != null) {
                    scripts[i].ReadFromSaveGame(savefile);
                }
            }

            // TimeLine Events
            for (i = 0; i < timeLineEvents.Num(); i++) {
                if (timeLineEvents.oGet(i) != null) {
                    timeLineEvents.oGet(i).pending = savefile.ReadBool();
                    timeLineEvents.oGet(i).time = savefile.ReadInt();
                    if (timeLineEvents.oGet(i).event != null) {
                        timeLineEvents.oGet(i).event.ReadFromSaveGame(savefile);
                    }
                }
            }

            // Transitions
            int num;
            num = savefile.ReadInt();
            for (i = 0; i < num; i++) {
                idTransitionData trans = new idTransitionData();
                trans.data = null;
                ReadSaveGameTransition(trans, savefile);
                if (trans.data != null) {
                    transitions.Append(trans);
                }
            }

            // Named Events
            for (i = 0; i < namedEvents.Num(); i++) {
                if (namedEvents.oGet(i) != null) {
                    ReadSaveGameString(namedEvents.oGet(i).mName, savefile);
                    if (namedEvents.oGet(i).mEvent != null) {
                        namedEvents.oGet(i).mEvent.ReadFromSaveGame(savefile);
                    }
                }
            }

            // regList
            regList.ReadFromSaveGame(savefile);

            // Read children
            for (i = 0; i < drawWindows.Num(); i++) {
                drawWin_t window = drawWindows.oGet(i);

                if (window.simp != null) {
                    window.simp.ReadFromSaveGame(savefile);
                } else if (window.win != null) {
                    window.win.ReadFromSaveGame(savefile);
                }
            }

            if ((flags & WIN_DESKTOP) != 0) {
                FixupTransitions();
            }
        }

        public void FixupTransitions() {
            int i, c = transitions.Num();
            for (i = 0; i < c; i++) {
                drawWin_t dw = gui.GetDesktop().FindChildByName(((idWinStr) transitions.oGet(i).data).c_str());
//		delete transitions[i].data;
                transitions.oGet(i).data = null;
                if (dw != null && (dw.win != null || dw.simp != null)) {//TODO:
//			if ( dw.win ) {
//				if ( transitions.oGet(i).offset == (int)( ( idWindow  ) 0 ).rect ) {
//					transitions.oGet(i).data = dw.win.rect;
//				} else if ( transitions.oGet(i).offset == (int)( ( idWindow * ) 0 ).backColor ) {
//					transitions[i].data = dw.win.backColor;
//				} else if ( transitions[i].offset == (int)( ( idWindow * ) 0 ).matColor ) {
//					transitions[i].data = dw.win.matColor;
//				} else if ( transitions[i].offset == (int)( ( idWindow * ) 0 ).foreColor ) {
//					transitions[i].data = dw.win.foreColor;
//				} else if ( transitions[i].offset == (int)( ( idWindow * ) 0 ).borderColor ) {
//					transitions[i].data = dw.win.borderColor;
//				} else if ( transitions[i].offset == (int)( ( idWindow * ) 0 ).textScale ) {
//					transitions[i].data = dw.win.textScale;
//				} else if ( transitions[i].offset == (int)( ( idWindow * ) 0 ).rotate ) {
//					transitions[i].data = dw.win.rotate;
//				}
//			} else {
//				if ( transitions[i].offset == (int)( ( idSimpleWindow * ) 0 ).rect ) {
//					transitions[i].data = dw.simp.rect;
//				} else if ( transitions[i].offset == (int)( ( idSimpleWindow * ) 0 ).backColor ) {
//					transitions[i].data = dw.simp.backColor;
//				} else if ( transitions[i].offset == (int)( ( idSimpleWindow * ) 0 ).matColor ) {
//					transitions[i].data = dw.simp.matColor;
//				} else if ( transitions[i].offset == (int)( ( idSimpleWindow * ) 0 ).foreColor ) {
//					transitions[i].data = dw.simp.foreColor;
//				} else if ( transitions[i].offset == (int)( ( idSimpleWindow * ) 0 ).borderColor ) {
//					transitions[i].data = dw.simp.borderColor;
//				} else if ( transitions[i].offset == (int)( ( idSimpleWindow * ) 0 ).textScale ) {
//					transitions[i].data = dw.simp.textScale;
//				} else if ( transitions[i].offset == (int)( ( idSimpleWindow * ) 0 ).rotate ) {
//					transitions[i].data = dw.simp.rotate;
//				}
//			}
                }
                if (transitions.oGet(i).data == null) {
                    transitions.RemoveIndex(i);
                    i--;
                    c--;
                }
            }
            for (c = 0; c < children.Num(); c++) {
                children.oGet(c).FixupTransitions();
            }
        }

        public void HasAction() {
        }

        public void HasScripts() {
        }

        public void FixupParms() {
            int i;
            int c = children.Num();
            for (i = 0; i < c; i++) {
                children.oGet(i).FixupParms();
            }
            for (i = 0; i < SCRIPT_COUNT.ordinal(); i++) {
                if (scripts[i] != null) {
                    scripts[i].FixupParms(this);
                }
            }

            c = timeLineEvents.Num();
            for (i = 0; i < c; i++) {
                timeLineEvents.oGet(i).event.FixupParms(this);
            }

            c = namedEvents.Num();
            for (i = 0; i < c; i++) {
                namedEvents.oGet(i).mEvent.FixupParms(this);
            }

            c = ops.Num();
            for (i = 0; i < c; i++) {
                if (ops.oGet(i).b == -2) {
                    // need to fix this up
                    final String p = ops.oGet(i).a.c_str();
                    idWinVar var = GetWinVarByName(p, true);
//                    System.out.println("=="+p);
//			delete []p;
                    ops.oGet(i).a = /*(int)*/ idWinVar.clone(var);
                    ops.oGet(i).b = -1;
                }
            }

            if ((flags & WIN_DESKTOP) != 0) {
                CalcRects(0, 0);
            }

        }

        public void GetScriptString(final String name, idStr out) {
        }

        public void SetScriptParams() {
        }

        public boolean HasOps() {
            return (ops.Num() > 0);
        }
        private static float[] regs = new float[MAX_EXPRESSION_REGISTERS];
        private static idWindow lastEval;

        public float EvalRegs(int test /*= -1*/, boolean force /*= false*/) {

            if (!force && test >= 0 && test < MAX_EXPRESSION_REGISTERS && lastEval == this) {
                return regs[test];
            }

            lastEval = this;

            if (expressionRegisters.Num() != 0) {
                regList.SetToRegs(regs);
                EvaluateRegisters(regs);
                regList.GetFromRegs(regs);
            }

            if (test >= 0 && test < MAX_EXPRESSION_REGISTERS) {
                return regs[test];
            }

            return 0.0f;
        }

        public float EvalRegs(int test /*= -1*/) {
            return this.EvalRegs(test, false);
        }

        public float EvalRegs() {
            return this.EvalRegs(-1);
        }

        public void StartTransition() {
            flags |= WIN_INTRANSITION;
        }

        public void AddTransition(idWinVar dest, idVec4 from, idVec4 to, int time, float accelTime, float decelTime) {
            idTransitionData data = new idTransitionData();
            data.data = dest;
            data.interp.Init(gui.GetTime(), accelTime * time, decelTime * time, time, from, to);
            transitions.Append(data);
        }

        public void ResetTime(int t) {

            timeLine = gui.GetTime() - t;

            int i, c = timeLineEvents.Num();
            for (i = 0; i < c; i++) {
                if (timeLineEvents.oGet(i).time >= t) {
                    timeLineEvents.oGet(i).pending = true;
                }
            }

            noTime.data = false;

            c = transitions.Num();
            for (i = 0; i < c; i++) {
                idTransitionData data = transitions.oGet(i);
                if (data.interp.IsDone(gui.GetTime()) && data.data != null) {
                    transitions.RemoveIndex(i);
                    i--;
                    c--;
                }
            }

        }

        public void ResetCinematics() {
            if (background != null) {
                background.ResetCinematicTime(gui.GetTime());
            }
        }

        public int NumTransitions() {
            int c = transitions.Num();
            for (int i = 0; i < children.Num(); i++) {
                c += children.oGet(i).NumTransitions();
            }
            return c;
        }

        public boolean ParseScript(idParser src, idGuiScriptList list, int[] timeParm /*= NULL*/, boolean elseBlock /*= false*/) {

            boolean ifElseBlock = false;

            idToken token = new idToken();

            // scripts start with { ( unless parm is true ) and have ; separated command lists.. commands are command,
            // arg.. basically we want everything between the { } as it will be interpreted at
            // run time
            if (elseBlock) {
                src.ReadToken(token);

                if (0 == token.Icmp("if")) {
                    ifElseBlock = true;
                }

                src.UnreadToken(token);

                if (!ifElseBlock && !src.ExpectTokenString("{")) {
                    return false;
                }
            } else if (!src.ExpectTokenString("{")) {
                return false;
            }

            int nest = 0;

            while (true) {
                if (!src.ReadToken(token)) {
                    src.Error("Unexpected end of file");
                    return false;
                }

                if (token.equals("{")) {
                    nest++;
                }

                if (token.equals("}")) {
                    if (nest-- <= 0) {
                        return true;
                    }
                }

                idGuiScript gs = new idGuiScript();
                if (token.Icmp("if") == 0) {
                    gs.conditionReg = ParseExpression(src);
                    gs.ifList = new idGuiScriptList();
                    ParseScript(src, gs.ifList, null);
                    if (src.ReadToken(token)) {
                        if (token.equals("else")) {
                            gs.elseList = new idGuiScriptList();
                            // pass true to indicate we are parsing an else condition
                            ParseScript(src, gs.elseList, null, true);
                        } else {
                            src.UnreadToken(token);
                        }
                    }

                    list.Append(gs);

                    // if we are parsing an else if then return out so 
                    // the initial "if" parser can handle the rest of the tokens
                    if (ifElseBlock) {
                        return true;
                    }
                    continue;
                } else {
                    src.UnreadToken(token);
                }

                // empty { } is not allowed
                if (token.equals("{")) {
                    src.Error("Unexpected {");
//			 delete gs;
                    return false;
                }

                gs.Parse(src);
                list.Append(gs);
            }

        }

        public boolean ParseScript(idParser src, idGuiScriptList list, int[] timeParm /*= NULL*/) {
            return this.ParseScript(src, list, timeParm, false);
        }

        public boolean ParseScript(idParser src, idGuiScriptList list) {
            return this.ParseScript(src, list, null);
        }

        public boolean RunScript(int n) {
            if (n >= ON_MOUSEENTER.ordinal() && n < SCRIPT_COUNT.ordinal()) {
                return RunScriptList(scripts[n]);
            }
            return false;
        }

        public boolean RunScript(Enum n) {
            return this.RunScript(etoi(n));
        }

        public boolean RunScriptList(idGuiScriptList src) {
            if (src == null) {
                return false;
            }
            src.Execute(this);
            return true;
        }

        public void SetRegs(final String key, final String val) {
        }

        /*
         ================
         idWindow::ParseExpression

         Returns a register index
         ================
         */
        public int ParseExpression(idParser src, idWinVar var /*= NULL*/, int component /*= 0*/) {
            return ParseExpressionPriority(src, TOP_PRIORITY, var);
        }

        public int ParseExpression(idParser src, idWinVar var /*= NULL*/) {
            return ParseExpression(src, var, 0);
        }

        public int ParseExpression(idParser src) {
            return ParseExpression(src, null);
        }

        public int ExpressionConstant(float f) {
            int i;

            for (i = etoi(WEXP_REG_NUM_PREDEFINED); i < expressionRegisters.Num(); i++) {
                if (!registerIsTemporary[i] && expressionRegisters.oGet(i) == f) {
                    return i;
                }
            }
            if (expressionRegisters.Num() == MAX_EXPRESSION_REGISTERS) {
                common.Warning("expressionConstant: gui %s hit MAX_EXPRESSION_REGISTERS", gui.GetSourceFile());
                return 0;
            }

            int c = expressionRegisters.Num();
            if (i > c) {
                while (i > c) {
                    expressionRegisters.Append(-9999999f);
                    i--;
                }
            }

            i = expressionRegisters.Append(f);
            registerIsTemporary[i] = false;
            return i;
        }

        public idRegisterList RegList() {
            return regList;
        }

        public void AddCommand(final String _cmd) {
            String str = cmd.toString();
            if (!str.isEmpty()) {
                str += " ; ";
                str += _cmd;
            } else {
                str = _cmd;
            }
            cmd.oSet(str);
        }

        static int DEBUG_updateVars = 0;

        public void AddUpdateVar(idWinVar var) {
            var.DEBUG_COUNTER = DEBUG_updateVars++;
            updateVars.AddUnique(var);
//            System.out.printf("%d %s\n", DEBUG_updateVars, var.GetName());
        }

        public boolean Interactive() {
            if (scripts[ON_ACTION.ordinal()] != null) {
                return true;
            }
            int c = children.Num();
            for (int i = 0; i < c; i++) {
                if (children.oGet(i).Interactive()) {
                    return true;
                }
            }
            return false;
        }

        public boolean ContainsStateVars() {
            if (updateVars.Num() != 0) {
                return true;
            }
            int c = children.Num();
            for (int i = 0; i < c; i++) {
                if (children.oGet(i).ContainsStateVars()) {
                    return true;
                }
            }
            return false;
        }

        public void SetChildWinVarVal(final String name, final String var, final String val) {
            drawWin_t dw = FindChildByName(name);
            idWinVar wv = null;
            if (dw != null) {
                if (dw.simp != null) {
                    wv = dw.simp.GetWinVarByName(var);
                } else if (dw.win != null) {
                    wv = dw.win.GetWinVarByName(var);
                }
                if (wv != null) {
                    wv.Set(val);
                    wv.SetEval(false);
                }
            }
        }

        public idWindow GetFocusedChild() {
            if ((flags & WIN_DESKTOP) != 0) {
                return gui.GetDesktop().focusedChild;
            }
            return null;
        }

        public idWindow GetCaptureChild() {
            if ((flags & WIN_DESKTOP) != 0) {
                return gui.GetDesktop().captureChild;
            }
            return null;
        }

        public String GetComment() {
            return comment.toString();
        }

        public void SetComment(final String p) {
            comment.oSet(p);
        }
        public idStr cmd = new idStr();

        public void RunNamedEvent(final String eventName) {
            int i;
            int c;

            // Find and run the event	
            c = namedEvents.Num();
            for (i = 0; i < c; i++) {
                if (namedEvents.oGet(i).mName.Icmp(eventName) != 0) {
                    continue;
                }

                UpdateWinVars();

                // Make sure we got all the current values for stuff
                if (expressionRegisters.Num() != 0 && ops.Num() != 0) {
                    EvalRegs(-1, true);
                }

                RunScriptList(namedEvents.oGet(i).mEvent);

                break;
            }

            // Run the event in all the children as well
            c = children.Num();
            for (i = 0; i < c; i++) {
                children.oGet(i).RunNamedEvent(eventName);
            }
        }

        public void AddDefinedVar(idWinVar var) {
            definedVars.AddUnique(var);
        }

        public idWindow FindChildByPoint(float x, float y, idWindow below /*= NULL*/) {
            return FindChildByPoint(x, y, below);
        }

        public int GetChildIndex(idWindow window) {
            int find;
            for (find = 0; find < drawWindows.Num(); find++) {
                if (drawWindows.oGet(find).win == window) {
                    return find;
                }
            }
            return -1;
        }

        /*
         ================
         idWindow::GetChildCount

         Returns the number of children
         ================
         */
        public int GetChildCount() {
            return drawWindows.Num();
        }

        /*
         ================
         idWindow::GetChild

         Returns the child window at the given index
         ================
         */private static int DBG_GetChild = 0;
        public idWindow GetChild(int index) {
            DBG_GetChild++;
            final drawWin_t win_t = drawWindows.oGet(index);
            final idWindow win = win_t.win;
            if (win_t != null && win_t.DBG_index == 10670) {
                int a = 0;
            }
            return win;
        }

        /*
         ================
         idWindow::RemoveChild

         Removes the child from the list of children.   Note that the child window being
         removed must still be deallocated by the caller
         ================
         */
        public void RemoveChild(idWindow win) {
            int find;

            // Remove the child window
            children.Remove(win);

            for (find = 0; find < drawWindows.Num(); find++) {
                if (drawWindows.oGet(find).win == win) {
                    drawWindows.RemoveIndex(find);
                    break;
                }
            }
        }

        /*
         ================
         idWindow::InsertChild

         Inserts the given window as a child into the given location in the zorder.
         ================
         */
        public boolean InsertChild(idWindow win, idWindow before) {
            AddChild(win);

            win.parent = this;

            drawWin_t dwt = new drawWin_t();
            dwt.simp = null;
            dwt.win = win;

            // If not inserting before anything then just add it at the end
            if (before != null) {
                int index;
                index = GetChildIndex(before);
                if (index != -1) {
                    drawWindows.Insert(dwt, index);
                    return true;
                }
            }

            drawWindows.Append(dwt);
            return true;
        }

        public void ScreenToClient(idRectangle rect) {
            int x;
            int y;
            idWindow p;

            for (p = this, x = 0, y = 0; p != null; p = p.parent) {
                x += p.rect.x();
                y += p.rect.y();
            }

            rect.x -= x;
            rect.y -= y;
        }

        public void ClientToScreen(idRectangle rect) {
            int x;
            int y;
            idWindow p;

            for (p = this, x = 0, y = 0; p != null; p = p.parent) {
                x += p.rect.x();
                y += p.rect.y();
            }

            rect.x += x;
            rect.y += y;
        }

        {
            int x;
            int y;
            idWindow p;

            for (p = this, x = 0, y = 0; p != null; p = p.parent) {
                x += p.rect.x();
                y += p.rect.y();
            }

            rect.data.x -= x;
            rect.data.y -= y;
        }

        public boolean UpdateFromDictionary(idDict dict) {
            idKeyValue kv;
            int i;

            SetDefaults();

            // Clear all registers since they will get recreated
            regList.Reset();
            expressionRegisters.Clear();
            ops.Clear();

            for (i = 0; i < dict.GetNumKeyVals(); i++) {
                kv = dict.GetKeyVal(i);

                // Special case name
                if (NOT(kv.GetKey().Icmp("name"))) {
                    name = kv.GetValue();
                    continue;
                }

                idParser src = new idParser(kv.GetValue().toString(), kv.GetValue().Length(), "",
                        LEXFL_NOFATALERRORS | LEXFL_NOSTRINGCONCAT | LEXFL_ALLOWMULTICHARLITERALS | LEXFL_ALLOWBACKSLASHSTRINGCONCAT);
                if (!ParseInternalVar(kv.GetKey().toString(), src)) {
                    // Kill the old register since the parse reg entry will add a new one
                    if (!ParseRegEntry(kv.GetKey().toString(), src)) {
                        continue;
                    }
                }
            }

            EvalRegs(-1, true);

            SetupFromState();
            PostParse();

            return true;
        }
        // friend		class rvGEWindowWrapper;

        /*
         ================
         idWindow::FindChildByPoint

         Finds the window under the given point
         ================
         */
        protected idWindow FindChildByPoint(float x, float y, idWindow[] below) {
            int c = children.Num();

            // If we are looking for a window below this one then
            // the next window should be good, but this one wasnt it
            if (below[0] == this) {
                below[0] = null;
                return null;
            }

            if (!Contains(drawRect, x, y)) {
                return null;
            }

            for (int i = c - 1; i >= 0; i--) {
                idWindow found = children.oGet(i).FindChildByPoint(x, y, below);
                if (found != null) {
                    if (below[0] != null) {
                        continue;
                    }

                    return found;
                }
            }

            return this;
        }

        /*
         ================
         idWindow::SetDefaults

         Set the window do a default window with no text, no background and 
         default colors, etc..
         ================
         */
        protected void SetDefaults() {
            forceAspectWidth = 640.0f;
            forceAspectHeight = 480.0f;
            matScalex = 1;
            matScaley = 1;
            borderSize = 0;
            noTime.data = false;
            visible.data = true;
            textAlign = 0;
            textAlignx = 0;
            textAligny = 0;
            noEvents.data = false;
            rotate.data = 0;
            shear.Zero();
            textScale.data = 0.35f;
            backColor.Zero();
            foreColor.oSet(new idVec4(1, 1, 1, 1));
            hoverColor.oSet(new idVec4(1, 1, 1, 1));
            matColor.oSet(new idVec4(1, 1, 1, 1));
            borderColor.Zero();
            text.data.oSet("");

            background = null;
            backGroundName.data.oSet("");
        }
////
//// friend class idSimpleWindow;
//// friend class idUserInterfaceLocal;

        protected boolean IsSimple() {

            // dont do simple windows when in gui editor
            if ((com_editors & EDITOR_GUI) != 0) {
                return false;
            }

            if (ops.Num() != 0) {
                return false;
            }
            if ((flags & (WIN_HCENTER | WIN_VCENTER)) != 0) {
                return false;
            }
            if (children.Num() != 0 || drawWindows.Num() != 0) {
                return false;
            }
            for (int i = 0; i < SCRIPT_COUNT.ordinal(); i++) {
                if (scripts[i] != null) {
                    return false;
                }
            }
            if (timeLineEvents.Num() != 0) {
                return false;
            }

            if (namedEvents.Num() != 0) {
                return false;
            }

            return true;
        }

        protected void UpdateWinVars() {
            int c = updateVars.Num();
            for (int i = 0; i < c; i++) {
//                System.out.printf("%d %s\n", DEBUG_Activate, updateVars.oGet(i).c_str());
                updateVars.oGet(i).Update();
            }
        }

        protected void DisableRegister(final String _name) {
            idRegister reg = RegList().FindReg(_name);
            if (reg != null) {
                reg.Enable(false);
            }
        }

        protected void Transition() {
            int i, c = transitions.Num();
            boolean clear = true;

            for (i = 0; i < c; i++) {
                idTransitionData data = transitions.oGet(i);
                idWinRectangle r = null;
                idWinFloat val = null;
                idWinVec4 v4 = null;
                if (data.data instanceof idWinVec4) {
                    v4 = (idWinVec4) data.data;
                } else if (data.data instanceof idWinFloat) {//TODO:check empty cast(s)(below too I think). EDIT:casts are to check types.
                    val = (idWinFloat) data.data;
                } else {
                    r = (idWinRectangle) data.data;
                }

                if (data.interp.IsDone(gui.GetTime()) && data.data != null) {
                    if (v4 != null) {
                        v4.oSet(data.interp.GetEndValue());
                    } else if (val != null) {
                        val.oSet(data.interp.GetEndValue().oGet(0));
                    } else {
                        r.oSet(data.interp.GetEndValue());
                    }
                } else {
                    clear = false;
                    if (data.data != null) {
                        if (v4 != null) {
                            v4.oSet(data.interp.GetCurrentValue(gui.GetTime()));
                        } else if (val != null) {
                            val.oSet(data.interp.GetCurrentValue(gui.GetTime()).oGet(0));
                        } else {
                            r.oSet(data.interp.GetCurrentValue(gui.GetTime()));
                        }
                    } else {
                        common.Warning("Invalid transitional data for window %s in gui %s", GetName(), gui.GetSourceFile());
                    }
                }
            }

            if (clear) {
                transitions.SetNum(0, false);
                flags &= ~WIN_INTRANSITION;
            }
        }

        protected void Time() {

            if (noTime.data) {
                return;
            }

            if (timeLine == -1) {
                timeLine = gui.GetTime();
            }

            cmd.oSet("");

            int c = timeLineEvents.Num();
            if (c > 0) {
                for (int i = 0; i < c; i++) {
                    if (timeLineEvents.oGet(i).pending && gui.GetTime() - timeLine >= timeLineEvents.oGet(i).time) {
                        timeLineEvents.oGet(i).pending = false;
                        RunScriptList(timeLineEvents.oGet(i).event);
                    }
                }
            }
            if (gui.Active()) {
                gui.GetPendingCmd().oPluSet(cmd);
            }
        }

        protected boolean RunTimeEvents(int time) {

            if (time - lastTimeRun < USERCMD_MSEC) {
                //common->Printf("Skipping gui time events at %d\n", time);
                return false;
            }

            lastTimeRun = time;

            UpdateWinVars();

            if (expressionRegisters.Num() != 0 && ops.Num() != 0) {
                EvalRegs();
            }

            if ((flags & WIN_INTRANSITION) != 0) {
                Transition();
            }

            Time();

            // renamed ON_EVENT to ON_FRAME
            RunScript(ON_FRAME);

            int c = children.Num();
            for (int i = 0; i < c; i++) {
                children.oGet(i).RunTimeEvents(time);
            }

            return true;
        }

        /*
         ================
         idHeap::Dump

         dump contents of the heap
         ================
         */
        @Deprecated
        protected void Dump() {
            throw new UnsupportedOperationException();
//            page_s pg;
//
//            for (pg = smallFirstUsedPage; pg; pg = pg.next) {
//                idLib.common.Printf("%p  bytes %-8d  (in use by small heap)\n", pg.data, pg.dataSize);
//            }
//
//            if (smallCurPage) {
//                pg = smallCurPage;
//                idLib.common.Printf("%p  bytes %-8d  (small heap active page)\n", pg.data, pg.dataSize);
//            }
//
//            for (pg = mediumFirstUsedPage; pg; pg = pg.next) {
//                idLib.common.Printf("%p  bytes %-8d  (completely used by medium heap)\n", pg.data, pg.dataSize);
//            }
//
//            for (pg = mediumFirstFreePage; pg; pg = pg.next) {
//                idLib.common.Printf("%p  bytes %-8d  (partially used by medium heap)\n", pg.data, pg.dataSize);
//            }
//
//            for (pg = largeFirstUsedPage; pg; pg = pg.next) {
//                idLib.common.Printf("%p  bytes %-8d  (fully used by large heap)\n", pg.data, pg.dataSize);
//            }
//
//            idLib.common.Printf("pages allocated : %d\n", pagesAllocated);
        }

        protected int ExpressionTemporary() {
            if (expressionRegisters.Num() == MAX_EXPRESSION_REGISTERS) {
                common.Warning("expressionTemporary: gui %s hit MAX_EXPRESSION_REGISTERS", gui.GetSourceFile());
                return 0;
            }
            int i = expressionRegisters.Num();
            registerIsTemporary[i] = true;
            i = expressionRegisters.Append(0f);
            return i;
        }

        protected wexpOp_t ExpressionOp() {
            if (ops.Num() == MAX_EXPRESSION_OPS) {
                common.Warning("expressionOp: gui %s hit MAX_EXPRESSION_OPS", gui.GetSourceFile());
                return ops.oGet(0);
            }
            wexpOp_t wop = new wexpOp_t();
//	memset(&wop, 0, sizeof(wexpOp_t));
            int i = ops.Append(wop);
            return ops.oGet(i);
        }

        protected int EmitOp(idWinVar a, int b, wexpOpType_t opType, wexpOp_t[] opp /*= NULL*/) {
            wexpOp_t op;
            /*
             // optimize away identity operations
             if ( opType == WOP_TYPE_ADD ) {
             if ( !registerIsTemporary[a] && shaderRegisters[a] == 0 ) {
             return b;
             }
             if ( !registerIsTemporary[b] && shaderRegisters[b] == 0 ) {
             return a;
             }
             if ( !registerIsTemporary[a] && !registerIsTemporary[b] ) {
             return ExpressionConstant( shaderRegisters[a] + shaderRegisters[b] );
             }
             }
             if ( opType == WOP_TYPE_MULTIPLY ) {
             if ( !registerIsTemporary[a] && shaderRegisters[a] == 1 ) {
             return b;
             }
             if ( !registerIsTemporary[a] && shaderRegisters[a] == 0 ) {
             return a;
             }
             if ( !registerIsTemporary[b] && shaderRegisters[b] == 1 ) {
             return a;
             }
             if ( !registerIsTemporary[b] && shaderRegisters[b] == 0 ) {
             return b;
             }
             if ( !registerIsTemporary[a] && !registerIsTemporary[b] ) {
             return ExpressionConstant( shaderRegisters[a] * shaderRegisters[b] );
             }
             }
             */
            op = ExpressionOp();

            op.opType = opType;
            op.a = idWinVar.clone(a);
            op.b = b;
            op.c = ExpressionTemporary();

            if (opp != null) {
                opp[0] = op;
            }
            return op.c;
        }

        protected int EmitOp(idWinVar a, int b, wexpOpType_t opType) {
            return this.EmitOp(a, b, opType, null);
        }

        protected int ParseEmitOp(idParser src, idWinVar a, wexpOpType_t opType, int priority, wexpOp_t[] opp /*= NULL*/) {
            int b = ParseExpressionPriority(src, priority);
            return EmitOp(a, b, opType, opp);
        }

        protected int ParseEmitOp(idParser src, idWinVar a, wexpOpType_t opType, int priority) {
            return ParseEmitOp(src, a, opType, priority, null);
        }


        /*
         ================
         idWindow::ParseTerm

         Returns a register index
         =================
         */
        protected int ParseTerm(idParser src, idWinVar var /*= NULL*/, int component /*= 0*/) {

            idToken token = new idToken();
            idWinVar a;
            int b;

            src.ReadToken(token);

            if (token.equals("(")) {
                b = ParseExpression(src);
                src.ExpectTokenString(")");
                return b;
            }

            if (0 == token.Icmp("time")) {
                return etoi(WEXP_REG_TIME);
            }

            // parse negative numbers
            if (token.equals("-")) {
                src.ReadToken(token);
                if (token.type == TT_NUMBER || token.equals(".")) {
                    return ExpressionConstant(-(float) token.GetFloatValue());
                }
                src.Warning("Bad negative number '%s'", token);
                return 0;
            }

            if (token.type == TT_NUMBER || token.equals(".") || token.equals("-")) {
                return ExpressionConstant(token.GetFloatValue());
            }

            // see if it is a table name
            final idDeclTable table = (idDeclTable) declManager.FindType(DECL_TABLE, token, false);
            if (table != null) {
                a = new idWinInt(table.Index());
                // parse a table expression
                src.ExpectTokenString("[");
                b = ParseExpression(src);
                src.ExpectTokenString("]");
                return EmitOp(a, b, WOP_TYPE_TABLE);
            }

            if (var == null) {
                var = GetWinVarByName(token.toString(), true);
            }
            if (var != null) {
                a = /*(int)*/ var;
                //assert(dynamic_cast<idWinVec4*>(var));
                var.Init(token.toString(), this);
                b = component;
                if (var instanceof idWinVec4) {// if (dynamic_cast < idWinVec4 > (var)) {
                    if (src.ReadToken(token)) {
                        if (token.equals("[")) {
                            b = ParseExpression(src);
                            src.ExpectTokenString("]");
                        } else {
                            src.UnreadToken(token);
                        }
                    }
                    return EmitOp(a, b, WOP_TYPE_VAR);
                } else if (var instanceof idWinFloat) {//dynamic_cast < idWinFloat > (var)) {
                    return EmitOp(a, b, WOP_TYPE_VARF);
                } else if (var instanceof idWinInt) {//(dynamic_cast < idWinInt > (var)) {
                    return EmitOp(a, b, WOP_TYPE_VARI);
                } else if (var instanceof idWinBool) {//(dynamic_cast < idWinBool > (var)) {
                    return EmitOp(a, b, WOP_TYPE_VARB);
                } else if (var instanceof idWinStr) {//(dynamic_cast < idWinStr > (var)) {
                    return EmitOp(a, b, WOP_TYPE_VARS);
                } else {
                    src.Warning("Var expression not vec4, float or int '%s'", token);
                }
                return 0;
            } else {
                // ugly but used for post parsing to fixup named vars
                final String p = token.toString();//new char[token.Length() + 1];
//                strcpy(p, token);
//                a = (int) p;
                a = new idWinStr(p);
                b = -2;
                return EmitOp(a, b, WOP_TYPE_VAR);
            }

        }

        /*
         =================
         idWindow::ParseExpressionPriority

         Returns a register index
         =================
         */
        protected int ParseExpressionPriority(idParser src, int priority, idWinVar var /*= NULL*/, int component /*= 0*/) {
            idToken token = new idToken();
            idWinInt a;

            if (priority == 0) {
                return ParseTerm(src, var, component);
            }

            a = new idWinInt(ParseExpressionPriority(src, priority - 1, var, component));

            if (!src.ReadToken(token)) {
                // we won't get EOF in a real file, but we can
                // when parsing from generated strings
                return a.data;
            }

            if (priority == 1 && token.equals("*")) {
                return ParseEmitOp(src, a, WOP_TYPE_MULTIPLY, priority);
            }
            if (priority == 1 && token.equals("/")) {
                return ParseEmitOp(src, a, WOP_TYPE_DIVIDE, priority);
            }
            if (priority == 1 && token.equals("%")) {	// implied truncate both to integer
                return ParseEmitOp(src, a, WOP_TYPE_MOD, priority);
            }
            if (priority == 2 && token.equals("+")) {
                return ParseEmitOp(src, a, WOP_TYPE_ADD, priority);
            }
            if (priority == 2 && token.equals("-")) {
                return ParseEmitOp(src, a, WOP_TYPE_SUBTRACT, priority);
            }
            if (priority == 3 && token.equals(">")) {
                return ParseEmitOp(src, a, WOP_TYPE_GT, priority);
            }
            if (priority == 3 && token.equals(">=")) {
                return ParseEmitOp(src, a, WOP_TYPE_GE, priority);
            }
            if (priority == 3 && token.equals("<")) {
                return ParseEmitOp(src, a, WOP_TYPE_LT, priority);
            }
            if (priority == 3 && token.equals("<=")) {
                return ParseEmitOp(src, a, WOP_TYPE_LE, priority);
            }
            if (priority == 3 && token.equals("==")) {
                return ParseEmitOp(src, a, WOP_TYPE_EQ, priority);
            }
            if (priority == 3 && token.equals("!=")) {
                return ParseEmitOp(src, a, WOP_TYPE_NE, priority);
            }
            if (priority == 4 && token.equals("&&")) {
                return ParseEmitOp(src, a, WOP_TYPE_AND, priority);
            }
            if (priority == 4 && token.equals("||")) {
                return ParseEmitOp(src, a, WOP_TYPE_OR, priority);
            }
            if (priority == 4 && token.equals("?")) {
                wexpOp_t[] oop = {null};
                int o = ParseEmitOp(src, a, WOP_TYPE_COND, priority, oop);
                if (!src.ReadToken(token)) {
                    return o;
                }
                if (token.equals(":")) {
                    oop[0].d = new idWinInt(ParseExpressionPriority(src, priority - 1, var));
                }
                return o;
            }

            // assume that anything else terminates the expression
            // not too robust error checking...
            src.UnreadToken(token);

            return a.data;
        }

        protected int ParseExpressionPriority(idParser src, int priority, idWinVar var /*= NULL*/) {
            return this.ParseExpressionPriority(src, priority, var, 0);
        }

        protected int ParseExpressionPriority(idParser src, int priority) {
            return this.ParseExpressionPriority(src, priority, null);
        }

        /*
         ===============
         idWindow::EvaluateRegisters

         Parameters are taken from the localSpace and the renderView,
         then all expressions are evaluated, leaving the shader registers
         set to their apropriate values.
         ===============
         */private static int DBG_EvaluateRegisters = 0;
        protected void EvaluateRegisters(float[] registers) {DBG_EvaluateRegisters++;

            int i, b;
            wexpOp_t op;
            idVec4 v;

            int erc = expressionRegisters.Num();
            int oc = ops.Num();
            // copy the constants
            for (i = etoi(WEXP_REG_NUM_PREDEFINED); i < erc; i++) {
                registers[i] = expressionRegisters.oGet(i);
            }

            // copy the local and global parameters
            registers[etoi(WEXP_REG_TIME)] = gui.GetTime();

            for (i = 0; i < oc; i++) {
                op = ops.oGet(i);
                if (op.b == -2) {
                    continue;
                }
                switch (op.opType) {
                    case WOP_TYPE_ADD:
                        registers[op.c] = registers[op.getA()] + registers[op.b];
                        break;
                    case WOP_TYPE_SUBTRACT:
                        registers[op.c] = registers[op.getA()] - registers[op.b];
                        break;
                    case WOP_TYPE_MULTIPLY:
                        registers[op.c] = registers[op.getA()] * registers[op.b];
                        break;
                    case WOP_TYPE_DIVIDE:
                        if (registers[op.b] == 0.0f) {
                            common.Warning("Divide by zero in window '%s' in %s", GetName(), gui.GetSourceFile());
                            registers[op.c] = registers[op.getA()];
                        } else {
                            registers[op.c] = registers[op.getA()] / registers[op.b];
                        }
                        break;
                    case WOP_TYPE_MOD:
                        b = (int) registers[op.b];
                        b = b != 0 ? b : 1;
                        registers[op.c] = (int) registers[op.getA()] % b;
                        break;
                    case WOP_TYPE_TABLE: {
                        final idDeclTable table = (idDeclTable) declManager.DeclByIndex(DECL_TABLE, op.getA());
                        registers[op.c] = table.TableLookup(registers[op.b]);
                    }
                    break;
                    case WOP_TYPE_GT:
                        registers[op.c] = registers[op.getA()] > registers[op.b] ? 1 : 0;
                        break;
                    case WOP_TYPE_GE:
                        registers[op.c] = registers[op.getA()] >= registers[op.b] ? 1 : 0;
                        break;
                    case WOP_TYPE_LT:
                        registers[op.c] = registers[op.getA()] < registers[op.b] ? 1 : 0;
                        break;
                    case WOP_TYPE_LE:
                        registers[op.c] = registers[op.getA()] <= registers[op.b] ? 1 : 0;
                        break;
                    case WOP_TYPE_EQ:
                        registers[op.c] = registers[op.getA()] == registers[op.b] ? 1 : 0;
                        break;
                    case WOP_TYPE_NE:
                        registers[op.c] = registers[op.getA()] != registers[op.b] ? 1 : 0;
                        break;
                    case WOP_TYPE_COND:
                        registers[op.c] = (registers[op.getA()]) != 0 ? registers[op.b] : registers[op.getD()];
                        break;
                    case WOP_TYPE_AND:
                        registers[op.c] = (registers[op.getA()] != 0 && registers[op.b] != 0) ? 1 : 0;
                        break;
                    case WOP_TYPE_OR:
                        registers[op.c] = (registers[op.getA()] != 0 || registers[op.b] != 0) ? 1 : 0;
                        break;
                    case WOP_TYPE_VAR:
                        if (NOT(op.a)) {
                            registers[op.c] = 0.0f;
                            break;
                        }
                        if (op.b >= 0 && registers[op.b] >= 0 && registers[op.b] < 4) {
                            // grabs vector components
                            idWinVec4 var = (idWinVec4) (op.a);
                            registers[op.c] = (var.data).oGet((int) registers[op.b]);
                        } else {
                            registers[op.c] = ((idWinVar) (op.a)).x();
                        }
                        break;
                    case WOP_TYPE_VARS:
                        if (op.a != null) {
                            idWinStr var = (idWinStr) (op.a);
                            registers[op.c] = atof(var.c_str());
                        } else {
                            registers[op.c] = 0;
                        }
                        break;
                    case WOP_TYPE_VARF:
                        if (op.a != null) {
                            idWinFloat var = (idWinFloat) (op.a);
                            registers[op.c] = var.data;
                        } else {
                            registers[op.c] = 0;
                        }
                        break;
                    case WOP_TYPE_VARI:
                        if (op.a != null) {
                            idWinInt var = (idWinInt) (op.a);
                            registers[op.c] = var.data;
                        } else {
                            registers[op.c] = 0;
                        }
                        break;
                    case WOP_TYPE_VARB:
                        if (op.a != null) {
                            idWinBool var = (idWinBool) (op.a);
                            registers[op.c] = btoi(var.data);
                        } else {
                            registers[op.c] = 0;
                        }
                        break;
                    default:
                        common.FatalError("R_EvaluateExpression: bad opcode");
                }
//                System.out.println("===" + registers[op.c]);
            }

        }

        protected void SaveExpressionParseState() {
            saveTemps = new boolean[MAX_EXPRESSION_REGISTERS];
//	memcpy(saveTemps, registerIsTemporary, MAX_EXPRESSION_REGISTERS * sizeof(bool));
            System.arraycopy(registerIsTemporary, 0, saveTemps, 0, MAX_EXPRESSION_REGISTERS);
        }

        protected void RestoreExpressionParseState() {
//	memcpy(registerIsTemporary, saveTemps, MAX_EXPRESSION_REGISTERS * sizeof(bool));
            System.arraycopy(saveTemps, 0, registerIsTemporary, 0, MAX_EXPRESSION_REGISTERS);
//            Mem_Free(saveTemps);
            saveTemps = null;
        }

        protected void ParseBracedExpression(idParser src) {
            src.ExpectTokenString("{");
            ParseExpression(src);
            src.ExpectTokenString("}");
        }

        protected boolean ParseScriptEntry(final String name, idParser src) {
            for (int i = 0; i < SCRIPT_COUNT.ordinal(); i++) {
                if (idStr.Icmp(name, ScriptNames[i]) == 0) {
                    // delete scripts[i];
                    scripts[i] = new idGuiScriptList();
                    return ParseScript(src, scripts[i]);
                }
            }
            return false;
        }

        private static int DBG_ParseRegEntry = 0;
        protected boolean ParseRegEntry(final String name, idParser src) {
            idStr work;
            work = new idStr(name);
            work.ToLower();

            idWinVar var = GetWinVarByName(work.toString(), false);
            if (var != null) {
                for (int i = 0; i < NumRegisterVars; i++) {
                    if (idStr.Icmp(work, RegisterVars[i].name) == 0) {
                        regList.AddReg(work.toString(), etoi(RegisterVars[i].type), src, this, var);
                        DBG_ParseRegEntry++;
                        return true;
                    }
                }
            }

            // not predefined so just read the next token and add it to the state
            idToken tok = new idToken();
            idVec4 v;
            idWinInt vari = new idWinInt();
            idWinFloat varf = new idWinFloat();
            idWinStr vars = new idWinStr();
            if (src.ReadToken(tok)) {
                if (var != null) {
                    var.Set(tok);
                    return true;
                }
                switch (tok.type) {
                    case TT_NUMBER:
                        if ((tok.subtype & TT_INTEGER) != 0) {
//                            vari = new idWinInt();
                            vari.data = atoi(tok);
                            vari.SetName(work.toString());
                            definedVars.Append(vari);
                        } else if ((tok.subtype & TT_FLOAT) != 0) {
//                            varf = new idWinFloat();
                            varf.data = atof(tok);
                            varf.SetName(work.toString());
                            definedVars.Append(varf);
                        } else {
//                            vars = new idWinStr();
                            vars.data = tok;
                            vars.SetName(work.toString());
                            definedVars.Append(vars);
                        }
                        break;
                    default:
                        vars = new idWinStr();
                        vars.data = tok;
                        vars.SetName(work.toString());
                        definedVars.Append(vars);
                        break;
                }
            }

            return true;
        }

        protected boolean ParseInternalVar(final String _name, idParser src) {

            if (idStr.Icmp(_name, "showtime") == 0) {
                if (src.ParseBool()) {
                    flags |= WIN_SHOWTIME;
                }
                return true;
            }
            if (idStr.Icmp(_name, "showcoords") == 0) {
                if (src.ParseBool()) {
                    flags |= WIN_SHOWCOORDS;
                }
                return true;
            }
            if (idStr.Icmp(_name, "forceaspectwidth") == 0) {
                forceAspectWidth = src.ParseFloat();
                return true;
            }
            if (idStr.Icmp(_name, "forceaspectheight") == 0) {
                forceAspectHeight = src.ParseFloat();
                return true;
            }
            if (idStr.Icmp(_name, "matscalex") == 0) {
                matScalex = src.ParseFloat();
                return true;
            }
            if (idStr.Icmp(_name, "matscaley") == 0) {
                matScaley = src.ParseFloat();
                return true;
            }
            if (idStr.Icmp(_name, "bordersize") == 0) {
                borderSize = src.ParseFloat();
                return true;
            }
            if (idStr.Icmp(_name, "nowrap") == 0) {
                if (src.ParseBool()) {
                    flags |= WIN_NOWRAP;
                }
                return true;
            }
            if (idStr.Icmp(_name, "shadow") == 0) {
                textShadow = (char) src.ParseInt();
                return true;
            }
            if (idStr.Icmp(_name, "textalign") == 0) {
                textAlign = (char) src.ParseInt();
                return true;
            }
            if (idStr.Icmp(_name, "textalignx") == 0) {
                textAlignx = src.ParseFloat();
                return true;
            }
            if (idStr.Icmp(_name, "textaligny") == 0) {
                textAligny = src.ParseFloat();
                return true;
            }
            if (idStr.Icmp(_name, "shear") == 0) {
                shear.x = src.ParseFloat();
                idToken tok = new idToken();
                src.ReadToken(tok);
                if (tok.Icmp(",") != 0) {
                    src.Error("Expected comma in shear definiation");
                    return false;
                }
                shear.y = src.ParseFloat();
                return true;
            }
            if (idStr.Icmp(_name, "wantenter") == 0) {
                if (src.ParseBool()) {
                    flags |= WIN_WANTENTER;
                }
                return true;
            }
            if (idStr.Icmp(_name, "naturalmatscale") == 0) {
                if (src.ParseBool()) {
                    flags |= WIN_NATURALMAT;
                }
                return true;
            }
            if (idStr.Icmp(_name, "noclip") == 0) {
                if (src.ParseBool()) {
                    flags |= WIN_NOCLIP;
                }
                return true;
            }
            if (idStr.Icmp(_name, "nocursor") == 0) {
                if (src.ParseBool()) {
                    flags |= WIN_NOCURSOR;
                }
                return true;
            }
            if (idStr.Icmp(_name, "menugui") == 0) {
                if (src.ParseBool()) {
                    flags |= WIN_MENUGUI;
                }
                return true;
            }
            if (idStr.Icmp(_name, "modal") == 0) {
                if (src.ParseBool()) {
                    flags |= WIN_MODAL;
                }
                return true;
            }
            if (idStr.Icmp(_name, "invertrect") == 0) {
                if (src.ParseBool()) {
                    flags |= WIN_INVERTRECT;
                }
                return true;
            }
            if (idStr.Icmp(_name, "name") == 0) {
                ParseString(src, name);
                return true;
            }
            if (idStr.Icmp(_name, "play") == 0) {
                common.Warning("play encountered during gui parse.. see Robert\n");
                idStr playStr = new idStr();
                ParseString(src, playStr);
                return true;
            }
            if (idStr.Icmp(_name, "comment") == 0) {
                ParseString(src, comment);
                return true;
            }
            if (idStr.Icmp(_name, "font") == 0) {
                idStr fontStr = new idStr();
                ParseString(src, fontStr);
                fontNum = (char) dc.FindFont(fontStr.toString());
                return true;
            }
            return false;
        }

        protected void ParseString(idParser src, idStr out) {
            idToken tok = new idToken();
            if (src.ReadToken(tok)) {
                out.oSet(tok);
            }
        }

        protected void ParseVec4(idParser src, idVec4 out) {
            idToken tok = new idToken();
            src.ReadToken(tok);
            out.x = atof(tok);
            src.ExpectTokenString(",");
            src.ReadToken(tok);
            out.y = atof(tok);
            src.ExpectTokenString(",");
            src.ReadToken(tok);
            out.z = atof(tok);
            src.ExpectTokenString(",");
            src.ReadToken(tok);
            out.w = atof(tok);
        }

        protected void ConvertRegEntry(final String name, idParser src, idStr out, int tabs) {
        }
    };
}
