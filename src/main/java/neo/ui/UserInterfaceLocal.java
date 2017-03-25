package neo.ui;

import java.nio.ByteBuffer;
import neo.Renderer.Material.idMaterial;
import static neo.Renderer.RenderSystem_init.r_skipGuiShaders;
import static neo.TempDump.sizeof;
import static neo.framework.Common.common;
import static neo.framework.DeclManager.declManager;
import static neo.framework.DeclManager.declType_t.DECL_MATERIAL;

import neo.framework.DemoFile.idDemoFile;
import static neo.framework.FileSystem_h.fileSystem;
import neo.framework.File_h.idFile;
import neo.framework.KeyInput.idKeyInput;
import neo.idlib.Dict_h.idDict;
import neo.idlib.Dict_h.idKeyValue;
import static neo.idlib.Text.Lexer.LEXFL_ALLOWBACKSLASHSTRINGCONCAT;
import static neo.idlib.Text.Lexer.LEXFL_ALLOWMULTICHARLITERALS;
import static neo.idlib.Text.Lexer.LEXFL_NOFATALERRORS;
import static neo.idlib.Text.Lexer.LEXFL_NOSTRINGCONCAT;
import neo.idlib.Text.Parser.idParser;
import neo.idlib.Text.Str.idStr;
import static neo.idlib.Text.Str.va;
import neo.idlib.Text.Token.idToken;
import neo.idlib.containers.List.idList;
import neo.idlib.math.Vector.idVec4;
import static neo.sys.sys_public.sysEventType_t.SE_KEY;
import static neo.sys.sys_public.sysEventType_t.SE_MOUSE;
import neo.sys.sys_public.sysEvent_s;
import neo.ui.DeviceContext.idDeviceContext;
import neo.ui.ListGUI.idListGUI;
import neo.ui.ListGUILocal.idListGUILocal;
import neo.ui.Rectangle.idRectangle;
import neo.ui.UserInterface.idUserInterface;
import neo.ui.UserInterface.idUserInterface.idUserInterfaceManager;
import static neo.ui.UserInterface.uiManagerLocal;
import static neo.ui.Window.WIN_DESKTOP;
import static neo.ui.Window.WIN_MENUGUI;
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

        private boolean  active;
        private boolean  loading;
        private boolean  interactive;
        private boolean  uniqued;
        //
        private idDict   state;
        private idWindow desktop;
        private idWindow bindHandler;
        //
        private idStr  source;
        private idStr  activateStr = new idStr();
        private idStr  pendingCmd  = new idStr();
        private idStr  returnCmd   = new idStr();
        private long[] timeStamp   = {0};
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
            cursorX = cursorY = 0;
            desktop = null;
            loading = false;
            active = false;
            interactive = false;
            uniqued = false;
            bindHandler = null;
            //so the reg eval in gui parsing doesn't get bogus values
            time = 0;
            refs = 1;
            this.source = new idStr();
            this.state = new idDict();
        }

        // ~idUserInterfaceLocal();
        @Override
        public String Name() {
            return source.toString();
        }

        @Override
        public String Comment() {
            if (desktop != null) {
                return desktop.GetComment();
            }
            return "";
        }

        @Override
        public boolean IsInteractive() {
            return interactive;
        }

        @Override
        public boolean InitFromFile(final String qpath, boolean rebuild /*= true*/, boolean cache /*= true*/) {

            if (!(qpath != null && !qpath.isEmpty())) {
                // FIXME: Memory leak!!
                return false;
            }

//            int sz = sizeof(idWindow.class);
//            sz = sizeof(idSimpleWindow.class);
            loading = true;

            if (rebuild || desktop == null) {
                desktop = new idWindow(this);
            }
//            System.out.println("FAAAAAAAAAAAAAAAAAAR " + desktop);

            source.oSet(qpath);
            state.Set("text", "Test Text!");

            idParser src = new idParser(LEXFL_NOFATALERRORS | LEXFL_NOSTRINGCONCAT | LEXFL_ALLOWMULTICHARLITERALS | LEXFL_ALLOWBACKSLASHSTRINGCONCAT);

            //Load the timestamp so reload guis will work correctly
            fileSystem.ReadFile(qpath, null, timeStamp);

            src.LoadFile(qpath);

            if (src.IsLoaded()) {
                idToken token = new idToken();
                while (src.ReadToken(token)) {
                    if (idStr.Icmp(token, "windowDef") == 0) {
                        desktop.SetDC(uiManagerLocal.dc);
                        if (desktop.Parse(src, rebuild)) {
                            desktop.SetFlag(WIN_DESKTOP);
                            desktop.FixupParms();
                        }
//                        continue;
                    }
                }

                state.Set("name", qpath);
            } else {
                desktop.SetDC(uiManagerLocal.dc);
                desktop.SetFlag(WIN_DESKTOP);
                desktop.name = new idStr("Desktop");
                desktop.text = new idWinStr(va("Invalid GUI: %s", qpath));//TODO:clean this mess up.
                desktop.rect.oSet(new idRectangle(0.0f, 0.0f, 640.0f, 480.0f));
                desktop.drawRect.oSet(desktop.rect.data);
                desktop.foreColor.oSet(new idVec4(1.0f, 1.0f, 1.0f, 1.0f));
                desktop.backColor.oSet(new idVec4(0.0f, 0.0f, 0.0f, 1.0f));
                desktop.SetupFromState();
                common.Warning("Couldn't load gui: '%s'", qpath);
            }

            interactive = desktop.Interactive();

            if (uiManagerLocal.guis.Find(this) == null) {
                uiManagerLocal.guis.Append(this);
            }

            loading = false;

            return true;
        }

        @Override
        public String HandleEvent(final sysEvent_s event, int _time, boolean[] updateVisuals) {

            time = _time;
//            System.out.println(System.nanoTime()+"HandleEvent time="+_time+" "+Common.com_ticNumber);

            if (bindHandler != null && event.evType == SE_KEY && event.evValue2 == 1) {
                final String ret = bindHandler.HandleEvent(event, updateVisuals);
                bindHandler = null;
                return ret;
            }

            if (event.evType == SE_MOUSE) {
                cursorX += event.evValue;
                cursorY += event.evValue2;

                if (cursorX < 0) {
                    cursorX = 0;
                }
                if (cursorY < 0) {
                    cursorY = 0;
                }
            }

            if (desktop != null) {
                return desktop.HandleEvent(event, updateVisuals);
            }

            return "";
        }

        @Override
        public void HandleNamedEvent(final String namedEvent) {
            desktop.RunNamedEvent(namedEvent);
        }

        @Override
        public void Redraw(int _time) {
            if (r_skipGuiShaders.GetInteger() > 5) {
                return;
            }
            if (!loading && desktop != null) {
                time = _time;
                uiManagerLocal.dc.PushClipRect(uiManagerLocal.screenRect);
                desktop.Redraw(0, 0);
                uiManagerLocal.dc.PopClipRect();
            }
        }

        @Override
        public void DrawCursor() {
            float[] cursorX = {this.cursorX}, cursorY = {this.cursorY};
            if (null == desktop || (desktop.GetFlags() & WIN_MENUGUI) != 0) {
                uiManagerLocal.dc.DrawCursor(cursorX, cursorY, 32.0f);
            } else {
                uiManagerLocal.dc.DrawCursor(cursorX, cursorY, 64.0f);
            }
        }

        @Override
        public idDict State() {
            return state;
        }

        @Override
        public void DeleteStateVar(final String varName) {
            state.Delete(varName);
        }

        @Override
        public void SetStateString(final String varName, final String value) {
            state.Set(varName, value);
        }

        @Override
        public void SetStateBool(final String varName, final boolean value) {
            state.SetBool(varName, value);
        }

        @Override
        public void SetStateInt(final String varName, final int value) {
            state.SetInt(varName, value);
        }

        @Override
        public void SetStateFloat(final String varName, final float value) {
            state.SetFloat(varName, value);
        }

        // Gets a gui state variable
        @Override
        public String GetStateString(final String varName, final String defaultString /*= ""*/) {
            return state.GetString(varName, defaultString);
        }

        public boolean GetStateBool(final String varName, final String defaultString /*= "0"*/) {
            return state.GetBool(varName, defaultString);
        }

        @Override
        public int GetStateInt(final String varName, final String defaultString /*= "0"*/) {
            return state.GetInt(varName, defaultString);
        }

        @Override
        public float GetStateFloat(final String varName, final String defaultString /*= "0"*/) {
            return state.GetFloat(varName, defaultString);
        }

        @Override
        public void StateChanged(int _time, boolean redraw) {
            time = _time;
            if (desktop != null) {
                desktop.StateChanged(redraw);
            }
            if (state.GetBool("noninteractive")) {
                interactive = false;
            } else {
                if (desktop != null) {
                    interactive = desktop.Interactive();
                } else {
                    interactive = false;
                }
            }
        }

        @Override
        public String Activate(boolean activate, int _time) {
            time = _time;
            active = activate;
            if (desktop != null) {
                activateStr.oSet("");
                desktop.Activate(activate, activateStr);
                return activateStr.toString();
            }
            return "";
        }

        @Override
        public void Trigger(int _time) {
            time = _time;
            if (desktop != null) {
                desktop.Trigger();
            }
        }

        @Override
        public void ReadFromDemoFile(idDemoFile f) {
//	idStr work;
            f.ReadDict(state);
            source.oSet(state.GetString("name"));

            if (desktop == null) {
                f.Log("creating new gui\n");
                desktop = new idWindow(this);
                desktop.SetFlag(WIN_DESKTOP);
                desktop.SetDC(uiManagerLocal.dc);
                desktop.ReadFromDemoFile(f);
            } else {
                f.Log("re-using gui\n");
                desktop.ReadFromDemoFile(f, false);
            }

            cursorX = f.ReadFloat();
            cursorY = f.ReadFloat();

            boolean add = true;
            int c = uiManagerLocal.demoGuis.Num();
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
            f.WriteDict(state);
            if (desktop != null) {
                desktop.WriteToDemoFile(f);
            }

            f.WriteFloat(cursorX);
            f.WriteFloat(cursorY);
        }

        @Override
        public boolean WriteToSaveGame(idFile savefile) {
            int len;
            idKeyValue kv;
            String string;

            int num = state.GetNumKeyVals();
            savefile.WriteInt(num);

            for (int i = 0; i < num; i++) {
                kv = state.GetKeyVal(i);
                len = kv.GetKey().Length();
                string = kv.GetKey().toString();
                savefile.WriteInt(len);
                savefile.WriteString(string);

                len = kv.GetValue().Length();
                string = kv.GetValue().toString();
                savefile.WriteInt(len);
                savefile.WriteString(string);
            }

            savefile.WriteBool(active);
            savefile.WriteBool(interactive);
            savefile.WriteBool(uniqued);
            savefile.WriteInt(time);
            len = activateStr.Length();
            savefile.WriteInt(len);
            savefile.WriteString(activateStr);
            len = pendingCmd.Length();
            savefile.WriteInt(len);
            savefile.WriteString(pendingCmd);
            len = returnCmd.Length();
            savefile.WriteInt(len);
            savefile.WriteString(returnCmd);

            savefile.WriteFloat(cursorX);
            savefile.WriteFloat(cursorY);

            desktop.WriteToSaveGame(savefile);

            return true;
        }

        @Override
        public boolean ReadFromSaveGame(idFile savefile) {
            int num;
            int i, len;
            idStr key = new idStr();
            idStr value = new idStr();

            num = savefile.ReadInt();

            state.Clear();
            for (i = 0; i < num; i++) {
                len = savefile.ReadInt();
                key.Fill(' ', len);
                savefile.ReadString(key);

                len = savefile.ReadInt();
                value.Fill(' ', len);
                savefile.ReadString(value);

                state.Set(key, value);
            }

            active = savefile.ReadBool();
            interactive = savefile.ReadBool();
            uniqued = savefile.ReadBool();
            time = savefile.ReadInt();

            len = savefile.ReadInt();
            activateStr.Fill(' ', len);
            savefile.ReadString(activateStr);
            len = savefile.ReadInt();
            pendingCmd.Fill(' ', len);
            savefile.ReadString(pendingCmd);
            len = savefile.ReadInt();
            returnCmd.Fill(' ', len);
            savefile.ReadString(returnCmd);

            cursorX = savefile.ReadFloat();
            cursorY = savefile.ReadFloat();

            desktop.ReadFromSaveGame(savefile);

            return true;
        }

        @Override
        public void SetKeyBindingNames() {
            if (null == desktop) {
                return;
            }
            // walk the windows
            RecurseSetKeyBindingNames(desktop);
        }

        @Override
        public boolean IsUniqued() {
            return uniqued;
        }

        @Override
        public void SetUniqued(boolean b) {
            uniqued = b;
        }

        @Override
        public void SetCursor(float x, float y) {
            cursorX = x;
            cursorY = y;
        }

        @Override
        public float CursorX() {
            return cursorX;
        }

        @Override
        public float CursorY() {
            return cursorY;
        }

        public int/*size_t*/ Size() {
            int sz = (int) (sizeof(this) + state.Size() + source.Allocated());
            if (desktop != null) {
                sz += desktop.Size();
            }
            return sz;
        }

        public idDict GetStateDict() {
            return state;
        }

        public String GetSourceFile() {
            return source.toString();
        }

        public long[]/*ID_TIME_T*/ GetTimeStamp() {
            return timeStamp;
        }

        public idWindow GetDesktop() {
            return desktop;
        }

        public void SetBindHandler(idWindow win) {
            bindHandler = win;
        }

        public boolean Active() {
            return active;
        }

        public int GetTime() {
            return time;
        }

        public void SetTime(int _time) {
            time = _time;
        }

        public void ClearRefs() {
            refs = 0;
        }

        public void AddRef() {
            refs++;
        }

        public int GetRefs() {
            return refs;
        }

        public void RecurseSetKeyBindingNames(idWindow window) {
            int i;
            idWinVar v = window.GetWinVarByName("bind");
            if (v != null) {
                SetStateString(v.GetName(), idKeyInput.KeysFromBinding(v.GetName()));
            }
            i = 0;
            while (i < window.GetChildCount()) {
                idWindow next = window.GetChild(i);
                if (next != null) {
                    RecurseSetKeyBindingNames(next);
                }
                i++;
            }
        }

        public idStr GetPendingCmd() {
            return pendingCmd;
        }

        public idStr GetReturnCmd() {
            return returnCmd;
        }

        @Override
        public boolean GetStateboolean(String varName, String defaultString) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
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
    };

    /*
     ===============================================================================

     idUserInterfaceManagerLocal

     ===============================================================================
     */
    public static class idUserInterfaceManagerLocal extends idUserInterfaceManager {
        // friend class idUserInterfaceLocal;

        private idRectangle screenRect = new idRectangle();
        private idDeviceContext dc = new idDeviceContext();
        private idList<idUserInterfaceLocal> guis = new idList<>();
        private idList<idUserInterfaceLocal> demoGuis = new idList<>();
        //
        //

        @Override
        public void Init() {
            screenRect = new idRectangle(0, 0, 640, 480);
            dc.Init();
        }

        @Override
        public void Shutdown() {
            guis.DeleteContents(true);
            demoGuis.DeleteContents(true);
            dc.Shutdown();
        }

        @Override
        public void Touch(final String name) {
            idUserInterface gui = Alloc();
            gui.InitFromFile(name);
//	delete gui;
        }

        @Override
        public void WritePrecacheCommands(idFile f) {

            int c = guis.Num();
            for (int i = 0; i < c; i++) {
                String str = String.format("touchGui %s\n", guis.oGet(i).Name());
                common.Printf("%s", str);
                f.Printf("%s", str);
            }
        }

        @Override
        public void SetSize(float width, float height) {
            dc.SetSize(width, height);
        }

        @Override
        public void BeginLevelLoad() {
            int c = guis.Num();
            for (int i = 0; i < c; i++) {
                if ((guis.oGet(i).GetDesktop().GetFlags() & WIN_MENUGUI) == 0) {
                    guis.oGet(i).ClearRefs();
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
            int c = guis.Num();
            for (int i = 0; i < c; i++) {
                if (guis.oGet(i).GetRefs() == 0) {
                    //common.Printf( "purging %s.\n", guis[i].GetSourceFile() );

                    // use this to make sure no materials still reference this gui
                    boolean remove = true;
                    for (int j = 0; j < declManager.GetNumDecls(DECL_MATERIAL); j++) {
                        final idMaterial material = (idMaterial) (declManager.DeclByIndex(DECL_MATERIAL, j, false));
                        if (material.GlobalGui() == guis.oGet(i)) {
                            remove = false;
                            break;
                        }
                    }
                    if (remove) {
//				delete guis[ i ];
                        guis.RemoveIndex(i);
                        i--;
                        c--;
                    }
                }
            }
        }

        @Override
        public void Reload(boolean all) {
            long[]/*ID_TIME_T*/ ts = new long[1];

            int c = guis.Num();
            for (int i = 0; i < c; i++) {
                if (!all) {
                    fileSystem.ReadFile(guis.oGet(i).GetSourceFile(), null, ts);
                    if (ts[0] <= guis.oGet(i).GetTimeStamp()[0]) {
                        continue;
                    }
                }

                guis.oGet(i).InitFromFile(guis.oGet(i).GetSourceFile());
                common.Printf("reloading %s.\n", guis.oGet(i).GetSourceFile());
            }
        }

        @Override
        public void ListGuis() {
            int c = guis.Num();
            common.Printf("\n   size   refs   name\n");
            int /*size_t*/ total = 0;
            int copies = 0;
            int unique = 0;
            for (int i = 0; i < c; i++) {
                idUserInterfaceLocal gui = guis.oGet(i);
                int /*size_t*/ sz = gui.Size();
                boolean isUnique = guis.oGet(i).interactive;
                if (isUnique) {
                    unique++;
                } else {
                    copies++;
                }
                common.Printf("%6.1fk %4d (%s) %s ( %d transitions )\n", sz / 1024.0f, guis.oGet(i).GetRefs(), isUnique ? "unique" : "copy", guis.oGet(i).GetSourceFile(), guis.oGet(i).desktop.NumTransitions());
                total += sz;
            }
            common.Printf("===========\n  %d total Guis ( %d copies, %d unique ), %.2f total Mbytes", c, copies, unique, total / (1024.0f * 1024.0f));
        }

        @Override
        public boolean CheckGui(final String qpath) {
            idFile file = fileSystem.OpenFileRead(qpath);
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
                int c = guis.Num();
                for (int i = 0; i < c; i++) {
                    if (guis.oGet(i) == gui) {
//				delete guis[i];
                        guis.RemoveIndex(i);
                        return;
                    }
                }
            }
        }

        @Override
        public idUserInterface FindGui(final String qpath, boolean autoLoad /*= false*/, boolean needInteractive /*= false*/, boolean forceUnique /*= false*/) {
            int c = guis.Num();

            for (int i = 0; i < c; i++) {
//		idUserInterfaceLocal gui = guis.oGet(i);
                if (0 == idStr.Icmp(guis.oGet(i).GetSourceFile(), qpath)) {
                    if (!forceUnique && (needInteractive || guis.oGet(i).IsInteractive())) {
                        break;
                    }
                    guis.oGet(i).AddRef();
                    return guis.oGet(i);
                }
            }

            if (autoLoad) {
                idUserInterface gui = Alloc();
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
            int c = demoGuis.Num();
            for (int i = 0; i < c; i++) {
                if (0 == idStr.Icmp(demoGuis.oGet(i).GetSourceFile(), qpath)) {
                    return demoGuis.oGet(i);
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

    };
}
