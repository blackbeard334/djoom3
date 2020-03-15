package neo.ui;

import neo.TempDump.SERiAL;
import neo.framework.DemoFile.idDemoFile;
import neo.framework.File_h.idFile;
import neo.idlib.Dict_h.idDict;
import neo.idlib.Text.Str.idStr;
import neo.sys.sys_public.sysEvent_s;
import neo.ui.ListGUI.idListGUI;
import neo.ui.UserInterface.idUserInterface.idUserInterfaceManager;
import neo.ui.UserInterfaceLocal.idUserInterfaceManagerLocal;

/**
 *
 */
public class UserInterface {

    static idUserInterfaceManagerLocal uiManagerLocal = new idUserInterfaceManagerLocal();
    public static idUserInterfaceManager uiManager = uiManagerLocal;

    /*
     ===============================================================================

     Draws an interactive 2D surface.
     Used for all user interaction with the game.

     ===============================================================================
     */
    public static abstract class idUserInterface implements SERiAL {

        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		// virtual						~idUserInterface() {};
        // Returns the name of the gui.
        public abstract String Name();

        // Returns a comment on the gui.
        public abstract String Comment();

        // Returns true if the gui is interactive.
        public abstract boolean IsInteractive();

        public abstract boolean IsUniqued();

        public abstract void SetUniqued(boolean b);

        // returns false if it failed to load
        public abstract boolean InitFromFile(final String qpath, boolean rebuild /*= true*/, boolean cache /*= true*/);

        public final boolean InitFromFile(final String qpath, boolean rebuild /*= true*/) {
            return InitFromFile(qpath, rebuild, true);
        }

        public final boolean InitFromFile(final String qpath) {
            return InitFromFile(qpath, true);
        }

        public final boolean InitFromFile(final idStr qpath) {
            return InitFromFile(qpath.toString(), true);
        }

        // handles an event, can return an action string, the caller interprets
        // any return and acts accordingly
        public abstract String HandleEvent(final sysEvent_s event, int time, boolean[] updateVisuals /*= NULL*/);

        public final String HandleEvent(final sysEvent_s event, int time) {
            return HandleEvent(event, time, null);
        }

        // handles a named event
        public abstract void HandleNamedEvent(final String eventName);

        // repaints the ui
        public abstract void Redraw(int time);

        // repaints the cursor
        public abstract void DrawCursor();

        // Provides read access to the idDict that holds this gui's state.
        public abstract idDict State();

        // Removes a gui state variable
        public abstract void DeleteStateVar(final String varName);

        // Sets a gui state variable.
        public abstract void SetStateString(final String varName, final String value);

        public abstract void SetStateBool(final String varName, final boolean value);

        public abstract void SetStateInt(final String varName, final int value);

        public abstract void SetStateFloat(final String varName, final float value);

        // Gets a gui state variable
        public abstract String GetStateString(final String varName, final String defaultString /*= ""*/);

        public final String GetStateString(final String varName) {
            return GetStateString(varName, "");
        }

        public abstract boolean GetStateboolean(final String varName, final String defaultString /*= "0"*/);

        public final boolean GetStateboolean(final String varName) {
            return GetStateboolean(varName, "0");
        }

        public abstract int GetStateInt(final String varName, final String defaultString /*= "0"*/);

        public final int GetStateInt(final String varName) {
            return GetStateInt(varName, "0");
        }

        public abstract float GetStateFloat(final String varName, final String defaultString /*= "0"*/);

        public final float GetStateFloat(final String varName) {
            return GetStateFloat(varName, "0");
        }

        // The state has changed and the gui needs to update from the state idDict.
        public abstract void StateChanged(int time, boolean redraw /*= false*/);

        public final void StateChanged(int time) {
            StateChanged(time, false);
        }

        // Activated the gui.
        public abstract String Activate(boolean activate, int time);

        // Triggers the gui and runs the onTrigger scripts.
        public abstract void Trigger(int time);

        public abstract void ReadFromDemoFile(idDemoFile f);

        public abstract void WriteToDemoFile(idDemoFile f);

        public abstract boolean WriteToSaveGame(idFile savefile);

        public abstract boolean ReadFromSaveGame(idFile savefile);

        public abstract void SetKeyBindingNames();

        public abstract void SetCursor(float x, float y);

        public abstract float CursorX();

        public abstract float CursorY();

        public abstract void oSet(idUserInterface FindGui);

        public static abstract class idUserInterfaceManager {

            // virtual 						~idUserInterfaceManager( void ) {};
            public abstract void Init();

            public abstract void Shutdown();

            public abstract void Touch(final String name);

            public abstract void WritePrecacheCommands(idFile f);

            // Sets the size for 640x480 adjustment.
            public abstract void SetSize(float width, float height);

            public abstract void BeginLevelLoad();

            public abstract void EndLevelLoad();

            // Reloads changed guis, or all guis.
            public abstract void Reload(boolean all);

            // lists all guis
            public abstract void ListGuis();

            // Returns true if gui exists.
            public abstract boolean CheckGui(final String qpath);

            // Allocates a new gui.
            public abstract idUserInterface Alloc();

            // De-allocates a gui.. ONLY USE FOR PRECACHING
            public abstract void DeAlloc(idUserInterface gui);

            // Returns NULL if gui by that name does not exist.
            public abstract idUserInterface FindGui(final String qpath, boolean autoLoad /*= false*/, boolean needUnique /*= false*/, boolean forceUnique /*= false*/);

            public final idUserInterface FindGui(final String qpath, boolean autoLoad /*= false*/, boolean needUnique /*= false*/) {
                return FindGui(qpath, autoLoad, needUnique, false);
            }

            public final idUserInterface FindGui(final String qpath, boolean autoLoad /*= false*/) {
                return FindGui(qpath, autoLoad, false);
            }

            public final idUserInterface FindGui(final String qpath) {
                return FindGui(qpath, false);
            }

            // Returns NULL if gui by that name does not exist.
            public abstract idUserInterface FindDemoGui(final String qpath);

            // Allocates a new GUI list handler
            public abstract idListGUI AllocListGUI();

            // De-allocates a list gui
            public abstract void FreeListGUI(idListGUI listgui);
        };
    };

    public static void setUiManager(idUserInterfaceManager uiManager) {
        UserInterface.uiManager = UserInterface.uiManagerLocal = (idUserInterfaceManagerLocal) uiManager;
    }
}
