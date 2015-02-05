package neo.ui;

import neo.idlib.Text.Str.idStr;
import neo.idlib.containers.List.idList;
import neo.ui.UserInterface.idUserInterface;

/**
 *
 */
public class ListGUI {

    /*
     ===============================================================================

     feed data to a listDef
     each item has an id and a display string

     ===============================================================================
     */
    public static abstract class idListGUI extends idList<idStr> {//TODO:what kind of impact does this inheritance which is farther inherited by ListGUILocal have!?

        // virtual				~idListGUI() { }
        public abstract void Config(idUserInterface pGUI, final String name);

        public abstract void Add(int id, final idStr s);

        // use the element count as index for the ids
        public abstract void Push(final idStr s);

        public abstract boolean Del(int id);

//        public abstract void Clear();
//
//        public abstract int Num();
//        
        public abstract int GetSelection(String[] s, int size, int sel /*= 0*/); // returns the id, not the list index (or -1)

        public final int GetSelection(String[] s, int size) {
            return GetSelection(s, size, 0);
        }

        public abstract void SetSelection(int sel);

        public abstract int GetNumSelections();

        public abstract boolean IsConfigured();

        // by default, any modification to the list will trigger a full GUI refresh immediately
        public abstract void SetStateChanges(boolean enable);

        public abstract void Shutdown();
    };
}
