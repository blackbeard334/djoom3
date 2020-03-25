package neo.ui;

import static neo.Renderer.RenderSystem_init.r_skipGuiShaders;
import static neo.TempDump.sizeof;
import static neo.framework.Common.common;
import static neo.framework.DeclManager.declManager;
import static neo.framework.DeclManager.declType_t.DECL_MATERIAL;
import static neo.framework.FileSystem_h.fileSystem;
import static neo.idlib.Text.Lexer.LEXFL_ALLOWBACKSLASHSTRINGCONCAT;
import static neo.idlib.Text.Lexer.LEXFL_ALLOWMULTICHARLITERALS;
import static neo.idlib.Text.Lexer.LEXFL_NOFATALERRORS;
import static neo.idlib.Text.Lexer.LEXFL_NOSTRINGCONCAT;
import static neo.idlib.Text.Str.va;
import static neo.sys.sys_public.sysEventType_t.SE_KEY;
import static neo.sys.sys_public.sysEventType_t.SE_MOUSE;
import static neo.ui.UserInterface.uiManagerLocal;
import static neo.ui.Window.WIN_DESKTOP;
import static neo.ui.Window.WIN_MENUGUI;

import java.nio.ByteBuffer;

import neo.Renderer.Material.idMaterial;
import neo.framework.DemoFile.idDemoFile;
import neo.framework.File_h.idFile;
import neo.framework.KeyInput.idKeyInput;
import neo.idlib.Dict_h.idDict;
import neo.idlib.Dict_h.idKeyValue;
import neo.idlib.Text.Parser.idParser;
import neo.idlib.Text.Str.idStr;
import neo.idlib.Text.Token.idToken;
import neo.idlib.containers.List.idList;
import neo.idlib.math.Vector.idVec4;
import neo.sys.sys_public.sysEvent_s;
import neo.ui.DeviceContext.idDeviceContext;
import neo.ui.ListGUI.idListGUI;
import neo.ui.ListGUILocal.idListGUILocal;
import neo.ui.Rectangle.idRectangle;
import neo.ui.UserInterface.idUserInterface;
import neo.ui.UserInterface.idUserInterface.idUserInterfaceManager;
import neo.ui.Window.idWindow;
import neo.ui.Winvar.idWinStr;
import neo.ui.Winvar.idWinVar;

/**
 *
 */
public class UserInterfaceLocal {

    /*
     ===============================================================================

     idUserInterfaceLocal

     ===============================================================================
     */
    public static class idUserInterfaceLocal extends idUserInterface {
        // friend class idUserInterfaceManagerLocal;

        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private boolean  active;
        private boolean  loading;
        private boolean  interactive;
        private boolean  uniqued;
        //
        private final idDict   state;
        private idWindow desktop;
        private idWindow bindHandler;
        //
        private final idStr  source;
        private final idStr  activateStr = new idStr();
        private final idStr  pendingCmd  = new idStr();
        private final idStr  returnCmd   = new idStr();
        private final long[] timeStamp   = {0};
        //
        private float cursorX;
        private float cursorY;
        //
        private int   time;
        //
        private int   refs;
        //
        //

        public idUserInterfaceLocal() {
            this.cursorX = this.cursorY = 0;
            this.desktop = null;
            this.loading = false;
            this.active = false;
            this.interactive = false;
            this.uniqued = false;
            this.bindHandler = null;
            //so the reg eval in gui parsing doesn't get bogus values
            this.time = 0;
            this.refs = 1;
            this.source = new idStr();
            this.state = new idDict();
        }

        // ~idUserInterfaceLocal();
        @Override
        public String Name() {
            return this.source.toString();
        }

        @Override
        public String Comment() {
            if (this.desktop != null) {
                return this.desktop.GetComment();
            }
            return "";
        }

        @Override
        public boolean IsInteractive() {
            return this.interactive;
        }

        @Override
        public boolean InitFromFile(final String qpath, boolean rebuild /*= true*/, boolean cache /*= true*/) {

            if (!((qpath != null) && !qpath.isEmpty())) {
                // FIXME: Memory leak!!
                return false;
            }

//            int sz = sizeof(idWindow.class);
//            sz = sizeof(idSimpleWindow.class);
            this.loading = true;

            if (rebuild || (this.desktop == null)) {
                this.desktop = new idWindow(this);
            }
//            System.out.println(NeoFixStrings.FAAAAAAAAAAAAAAAAAAR + " " + desktop);

            this.source.oSet(qpath);
            this.state.Set("text", "Test Text!");

            final idParser src = new idParser(LEXFL_NOFATALERRORS | LEXFL_NOSTRINGCONCAT | LEXFL_ALLOWMULTICHARLITERALS | LEXFL_ALLOWBACKSLASHSTRINGCONCAT);

            //Load the timestamp so reload guis will work correctly
            fileSystem.ReadFile(qpath, null, this.timeStamp);

            src.LoadFile(qpath);

            if (src.IsLoaded()) {
                final idToken token = new idToken();
                while (src.ReadToken(token)) {
                    if (idStr.Icmp(token, "windowDef") == 0) {
                        this.desktop.SetDC(uiManagerLocal.dc);
                        if (this.desktop.Parse(src, rebuild)) {
                            this.desktop.SetFlag(WIN_DESKTOP);
                            this.desktop.FixupParms();
                        }
//                        continue;
                    }
                }

                this.state.Set("name", qpath);
            } else {
                this.desktop.SetDC(uiManagerLocal.dc);
                this.desktop.SetFlag(WIN_DESKTOP);
                this.desktop.name = new idStr("Desktop");
                this.desktop.text = new idWinStr(va("Invalid GUI: %s", qpath));//TODO:clean this mess up.
                this.desktop.rect.oSet(new idRectangle(0.0f, 0.0f, 640.0f, 480.0f));
                this.desktop.drawRect.oSet(this.desktop.rect.data);
                this.desktop.foreColor.oSet(new idVec4(1.0f, 1.0f, 1.0f, 1.0f));
                this.desktop.backColor.oSet(new idVec4(0.0f, 0.0f, 0.0f, 1.0f));
                this.desktop.SetupFromState();
                common.Warning("Couldn't load gui: '%s'", qpath);
            }

            this.interactive = this.desktop.Interactive();

            if (uiManagerLocal.guis.Find(this) == null) {
                uiManagerLocal.guis.Append(this);
            }

            this.loading = false;

            return true;
        }

        @Override
        public String HandleEvent(final sysEvent_s event, int _time, boolean[] updateVisuals) {

            this.time = _time;
//            System.out.println(System.nanoTime()+"HandleEvent time="+_time+" "+Common.com_ticNumber);

            if ((this.bindHandler != null) && (event.evType == SE_KEY) && (event.evValue2 == 1)) {
                final String ret = this.bindHandler.HandleEvent(event, updateVisuals);
                this.bindHandler = null;
                return ret;
            }

            if (event.evType == SE_MOUSE) {
                this.cursorX += event.evValue;
                this.cursorY += event.evValue2;

                if (this.cursorX < 0) {
                    this.cursorX = 0;
                }
                if (this.cursorY < 0) {
                    this.cursorY = 0;
                }
            }

            if (this.desktop != null) {
                return this.desktop.HandleEvent(event, updateVisuals);
            }

            return "";
        }

        @Override
        public void HandleNamedEvent(final String namedEvent) {
            this.desktop.RunNamedEvent(namedEvent);
        }

        @Override
        public void Redraw(int _time) {
            if (r_skipGuiShaders.GetInteger() > 5) {
                return;
            }
            if (!this.loading && (this.desktop != null)) {
                this.time = _time;
                uiManagerLocal.dc.PushClipRect(uiManagerLocal.screenRect);
                this.desktop.Redraw(0, 0);
                uiManagerLocal.dc.PopClipRect();
            }
        }

        @Override
        public void DrawCursor() {
            final float[] cursorX = {this.cursorX}, cursorY = {this.cursorY};
            if ((null == this.desktop) || ((this.desktop.GetFlags() & WIN_MENUGUI) != 0)) {
                uiManagerLocal.dc.DrawCursor(cursorX, cursorY, 32.0f);
            } else {
                uiManagerLocal.dc.DrawCursor(cursorX, cursorY, 64.0f);
            }
        }

        @Override
        public idDict State() {
            return this.state;
        }

        @Override
        public void DeleteStateVar(final String varName) {
            this.state.Delete(varName);
        }

        @Override
        public void SetStateString(final String varName, final String value) {
            this.state.Set(varName, value);
        }

        @Override
        public void SetStateBool(final String varName, final boolean value) {
            this.state.SetBool(varName, value);
        }

        @Override
        public void SetStateInt(final String varName, final int value) {
            this.state.SetInt(varName, value);
        }

        @Override
        public void SetStateFloat(final String varName, final float value) {
            this.state.SetFloat(varName, value);
        }

        // Gets a gui state variable
        @Override
        public String GetStateString(final String varName, final String defaultString /*= ""*/) {
            return this.state.GetString(varName, defaultString);
        }

        public boolean GetStateBool(final String varName, final String defaultString /*= "0"*/) {
            return this.state.GetBool(varName, defaultString);
        }

        @Override
        public int GetStateInt(final String varName, final String defaultString /*= "0"*/) {
            return this.state.GetInt(varName, defaultString);
        }

        @Override
        public float GetStateFloat(final String varName, final String defaultString /*= "0"*/) {
            return this.state.GetFloat(varName, defaultString);
        }

        @Override
        public void StateChanged(int _time, boolean redraw) {
            this.time = _time;
            if (this.desktop != null) {
                this.desktop.StateChanged(redraw);
            }
            if (this.state.GetBool("noninteractive")) {
                this.interactive = false;
            } else {
                if (this.desktop != null) {
                    this.interactive = this.desktop.Interactive();
                } else {
                    this.interactive = false;
                }
            }
        }

        @Override
        public String Activate(boolean activate, int _time) {
            this.time = _time;
            this.active = activate;
            if (this.desktop != null) {
                this.activateStr.oSet("");
                this.desktop.Activate(activate, this.activateStr);
                return this.activateStr.toString();
            }
            return "";
        }

        @Override
        public void Trigger(int _time) {
            this.time = _time;
            if (this.desktop != null) {
                this.desktop.Trigger();
            }
        }

        @Override
        public void ReadFromDemoFile(idDemoFile f) {
//	idStr work;
            f.ReadDict(this.state);
            this.source.oSet(this.state.GetString("name"));

            if (this.desktop == null) {
                f.Log("creating new gui\n");
                this.desktop = new idWindow(this);
                this.desktop.SetFlag(WIN_DESKTOP);
                this.desktop.SetDC(uiManagerLocal.dc);
                this.desktop.ReadFromDemoFile(f);
            } else {
                f.Log("re-using gui\n");
                this.desktop.ReadFromDemoFile(f, false);
            }

            this.cursorX = f.ReadFloat();
            this.cursorY = f.ReadFloat();

            boolean add = true;
            final int c = uiManagerLocal.demoGuis.Num();
            for (int i = 0; i < c; i++) {
                if (uiManagerLocal.demoGuis.oGet(i).equals(this)) {
                    add = false;
                    break;
                }
            }

            if (add) {
                uiManagerLocal.demoGuis.Append(this);
            }
        }

        @Override
        public void WriteToDemoFile(idDemoFile f) {
//	idStr work;
            f.WriteDict(this.state);
            if (this.desktop != null) {
                this.desktop.WriteToDemoFile(f);
            }

            f.WriteFloat(this.cursorX);
            f.WriteFloat(this.cursorY);
        }

        @Override
        public boolean WriteToSaveGame(idFile savefile) {
            int len;
            idKeyValue kv;
            String string;

            final int num = this.state.GetNumKeyVals();
            savefile.WriteInt(num);

            for (int i = 0; i < num; i++) {
                kv = this.state.GetKeyVal(i);
                len = kv.GetKey().Length();
                string = kv.GetKey().toString();
                savefile.WriteInt(len);
                savefile.WriteString(string);

                len = kv.GetValue().Length();
                string = kv.GetValue().toString();
                savefile.WriteInt(len);
                savefile.WriteString(string);
            }

            savefile.WriteBool(this.active);
            savefile.WriteBool(this.interactive);
            savefile.WriteBool(this.uniqued);
            savefile.WriteInt(this.time);
            len = this.activateStr.Length();
            savefile.WriteInt(len);
            savefile.WriteString(this.activateStr);
            len = this.pendingCmd.Length();
            savefile.WriteInt(len);
            savefile.WriteString(this.pendingCmd);
            len = this.returnCmd.Length();
            savefile.WriteInt(len);
            savefile.WriteString(this.returnCmd);

            savefile.WriteFloat(this.cursorX);
            savefile.WriteFloat(this.cursorY);

            this.desktop.WriteToSaveGame(savefile);

            return true;
        }

        @Override
        public boolean ReadFromSaveGame(idFile savefile) {
            int num;
            int i, len;
            final idStr key = new idStr();
            final idStr value = new idStr();

            num = savefile.ReadInt();

            this.state.Clear();
            for (i = 0; i < num; i++) {
                len = savefile.ReadInt();
                key.Fill(' ', len);
                savefile.ReadString(key);

                len = savefile.ReadInt();
                value.Fill(' ', len);
                savefile.ReadString(value);

                this.state.Set(key, value);
            }

            this.active = savefile.ReadBool();
            this.interactive = savefile.ReadBool();
            this.uniqued = savefile.ReadBool();
            this.time = savefile.ReadInt();

            len = savefile.ReadInt();
            this.activateStr.Fill(' ', len);
            savefile.ReadString(this.activateStr);
            len = savefile.ReadInt();
            this.pendingCmd.Fill(' ', len);
            savefile.ReadString(this.pendingCmd);
            len = savefile.ReadInt();
            this.returnCmd.Fill(' ', len);
            savefile.ReadString(this.returnCmd);

            this.cursorX = savefile.ReadFloat();
            this.cursorY = savefile.ReadFloat();

            this.desktop.ReadFromSaveGame(savefile);

            return true;
        }

        @Override
        public void SetKeyBindingNames() {
            if (null == this.desktop) {
                return;
            }
            // walk the windows
            RecurseSetKeyBindingNames(this.desktop);
        }

        @Override
        public boolean IsUniqued() {
            return this.uniqued;
        }

        @Override
        public void SetUniqued(boolean b) {
            this.uniqued = b;
        }

        @Override
        public void SetCursor(float x, float y) {
            this.cursorX = x;
            this.cursorY = y;
        }

        @Override
        public float CursorX() {
            return this.cursorX;
        }

        @Override
        public float CursorY() {
            return this.cursorY;
        }

        public int/*size_t*/ Size() {
            int sz = (int) (sizeof(this) + this.state.Size() + this.source.Allocated());
            if (this.desktop != null) {
                sz += this.desktop.Size();
            }
            return sz;
        }

        public idDict GetStateDict() {
            return this.state;
        }

        public String GetSourceFile() {
            return this.source.toString();
        }

        public long[]/*ID_TIME_T*/ GetTimeStamp() {
            return this.timeStamp;
        }

        public idWindow GetDesktop() {
            return this.desktop;
        }

        public void SetBindHandler(idWindow win) {
            this.bindHandler = win;
        }

        public boolean Active() {
            return this.active;
        }

        public int GetTime() {
            return this.time;
        }

        public void SetTime(int _time) {
            this.time = _time;
        }

        public void ClearRefs() {
            this.refs = 0;
        }

        public void AddRef() {
            this.refs++;
        }

        public int GetRefs() {
            return this.refs;
        }

        public void RecurseSetKeyBindingNames(idWindow window) {
            int i;
            final idWinVar v = window.GetWinVarByName("bind");
            if (v != null) {
                SetStateString(v.GetName(), idKeyInput.KeysFromBinding(v.GetName()));
            }
            i = 0;
            while (i < window.GetChildCount()) {
                final idWindow next = window.GetChild(i);
                if (next != null) {
                    RecurseSetKeyBindingNames(next);
                }
                i++;
            }
        }

        public idStr GetPendingCmd() {
            return this.pendingCmd;
        }

        public idStr GetReturnCmd() {
            return this.returnCmd;
        }

        @Override
        public boolean GetStateboolean(String varName, String defaultString) {
        	return this.state.GetBool(varName, defaultString);
        }

        @Override
        public void oSet(idUserInterface FindGui) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public ByteBuffer AllocBuffer() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void Read(ByteBuffer buffer) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public ByteBuffer Write() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    }

    /*
     ===============================================================================

     idUserInterfaceManagerLocal

     ===============================================================================
     */
    public static class idUserInterfaceManagerLocal extends idUserInterfaceManager {
        // friend class idUserInterfaceLocal;

        private idRectangle screenRect = new idRectangle();
        private final idDeviceContext dc = new idDeviceContext();
        private final idList<idUserInterfaceLocal> guis = new idList<>();
        private final idList<idUserInterfaceLocal> demoGuis = new idList<>();
        //
        //

        @Override
        public void Init() {
            this.screenRect = new idRectangle(0, 0, 640, 480);
            this.dc.Init();
        }

        @Override
        public void Shutdown() {
            this.guis.DeleteContents(true);
            this.demoGuis.DeleteContents(true);
            this.dc.Shutdown();
        }

        @Override
        public void Touch(final String name) {
            final idUserInterface gui = Alloc();
            gui.InitFromFile(name);
//	delete gui;
        }

        @Override
        public void WritePrecacheCommands(idFile f) {

            final int c = this.guis.Num();
            for (int i = 0; i < c; i++) {
                final String str = String.format("touchGui %s\n", this.guis.oGet(i).Name());
                common.Printf("%s", str);
                f.Printf("%s", str);
            }
        }

        @Override
        public void SetSize(float width, float height) {
            this.dc.SetSize(width, height);
        }

        @Override
        public void BeginLevelLoad() {
            final int c = this.guis.Num();
            for (int i = 0; i < c; i++) {
                if ((this.guis.oGet(i).GetDesktop().GetFlags() & WIN_MENUGUI) == 0) {
                    this.guis.oGet(i).ClearRefs();
                    /*
                     delete guis[ i ];
                     guis.RemoveIndex( i );
                     i--; c--;
                     */
                }
            }
        }

        @Override
        public void EndLevelLoad() {
            int c = this.guis.Num();
            for (int i = 0; i < c; i++) {
                if (this.guis.oGet(i).GetRefs() == 0) {
                    //common.Printf( "purging %s.\n", guis[i].GetSourceFile() );

                    // use this to make sure no materials still reference this gui
                    boolean remove = true;
                    for (int j = 0; j < declManager.GetNumDecls(DECL_MATERIAL); j++) {
                        final idMaterial material = (idMaterial) (declManager.DeclByIndex(DECL_MATERIAL, j, false));
                        if (material.GlobalGui() == this.guis.oGet(i)) {
                            remove = false;
                            break;
                        }
                    }
                    if (remove) {
//				delete guis[ i ];
                        this.guis.RemoveIndex(i);
                        i--;
                        c--;
                    }
                }
            }
        }

        @Override
        public void Reload(boolean all) {
            final long[]/*ID_TIME_T*/ ts = new long[1];

            final int c = this.guis.Num();
            for (int i = 0; i < c; i++) {
                if (!all) {
                    fileSystem.ReadFile(this.guis.oGet(i).GetSourceFile(), null, ts);
                    if (ts[0] <= this.guis.oGet(i).GetTimeStamp()[0]) {
                        continue;
                    }
                }

                this.guis.oGet(i).InitFromFile(this.guis.oGet(i).GetSourceFile());
                common.Printf("reloading %s.\n", this.guis.oGet(i).GetSourceFile());
            }
        }

        @Override
        public void ListGuis() {
            final int c = this.guis.Num();
            common.Printf("\n   size   refs   name\n");
            int /*size_t*/ total = 0;
            int copies = 0;
            int unique = 0;
            for (int i = 0; i < c; i++) {
                final idUserInterfaceLocal gui = this.guis.oGet(i);
                final int /*size_t*/ sz = gui.Size();
                final boolean isUnique = this.guis.oGet(i).interactive;
                if (isUnique) {
                    unique++;
                } else {
                    copies++;
                }
                common.Printf("%6.1fk %4d (%s) %s ( %d transitions )\n", sz / 1024.0f, this.guis.oGet(i).GetRefs(), isUnique ? "unique" : "copy", this.guis.oGet(i).GetSourceFile(), this.guis.oGet(i).desktop.NumTransitions());
                total += sz;
            }
            common.Printf("===========\n  %d total Guis ( %d copies, %d unique ), %.2f total Mbytes", c, copies, unique, total / (1024.0f * 1024.0f));
        }

        @Override
        public boolean CheckGui(final String qpath) {
            final idFile file = fileSystem.OpenFileRead(qpath);
            if (file != null) {
                fileSystem.CloseFile(file);
                return true;
            }
            return false;
        }

        @Override
        public idUserInterface Alloc() {
            return new idUserInterfaceLocal();
        }

        @Override
        public void DeAlloc(idUserInterface gui) {
            if (gui != null) {
                final int c = this.guis.Num();
                for (int i = 0; i < c; i++) {
                    if (this.guis.oGet(i) == gui) {
//				delete guis[i];
                        this.guis.RemoveIndex(i);
                        return;
                    }
                }
            }
        }

        @Override
        public idUserInterface FindGui(final String qpath, boolean autoLoad /*= false*/, boolean needInteractive /*= false*/, boolean forceUnique /*= false*/) {
            final int c = this.guis.Num();

            for (int i = 0; i < c; i++) {
//		idUserInterfaceLocal gui = guis.oGet(i);
                if (0 == idStr.Icmp(this.guis.oGet(i).GetSourceFile(), qpath)) {
                    if (!forceUnique && (needInteractive || this.guis.oGet(i).IsInteractive())) {
                        break;
                    }
                    this.guis.oGet(i).AddRef();
                    return this.guis.oGet(i);
                }
            }

            if (autoLoad) {
                final idUserInterface gui = Alloc();
                if (gui.InitFromFile(qpath)) {
                    gui.SetUniqued(forceUnique ? false : needInteractive);
                    return gui;
//                } else {
//			delete gui;
                }
            }
            return null;
        }

        @Override
        public idUserInterface FindDemoGui(final String qpath) {
            final int c = this.demoGuis.Num();
            for (int i = 0; i < c; i++) {
                if (0 == idStr.Icmp(this.demoGuis.oGet(i).GetSourceFile(), qpath)) {
                    return this.demoGuis.oGet(i);
                }
            }
            return null;
        }

        @Override
        public idListGUI AllocListGUI() {
            return new idListGUILocal();
        }

        @Override
        public void FreeListGUI(idListGUI listgui) {
//            delete listgui;
            listgui.Clear();
        }

    }
}
