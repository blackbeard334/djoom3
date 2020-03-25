package neo.ui;

import static neo.framework.Common.com_frameTime;
import static neo.idlib.Text.Str.va;

import neo.idlib.Text.Str.idStr;
import neo.idlib.containers.List.idList;
import neo.ui.ListGUI.idListGUI;
import neo.ui.UserInterface.idUserInterface;

/**
 *
 */
public class ListGUILocal {

    /*
     ===============================================================================

     feed data to a listDef
     each item has an id and a display string

     ===============================================================================
     */
    public static class idListGUILocal extends idListGUI {

        private idUserInterface m_pGUI;
        private final idStr m_name;
        private int m_water;
        private final idList<Integer> m_ids;
        private boolean m_stateUpdates;
        //
        //

        public idListGUILocal() {
            this.m_pGUI = null;
            this.m_name = new idStr();
            this.m_water = 0;
            this.m_ids = new idList<>();
            this.m_stateUpdates = true;
        }
        // idListGUI interface

        @Override
        public void Config(idUserInterface pGUI, final String name) {
            this.m_pGUI = pGUI;
            this.m_name.oSet("" + name);
        }

        @Override
        public void Add(int id, final idStr s) {
            final int i = this.m_ids.FindIndex(id);
            if (i == -1) {
                Append(s);
                this.m_ids.Append(id);
            } else {
                this.oSet(i, s);
            }
            StateChanged();
        }

        // use the element count as index for the ids
        @Override
        public void Push(final idStr s) {
            Append(s);
            this.m_ids.Append(this.m_ids.Num());
            StateChanged();
        }

        @Override
        public boolean Del(int id) {
            final int i = this.m_ids.FindIndex(id);
            if (i == -1) {
                return false;
            }
            this.m_ids.RemoveIndex(i);
            this.RemoveIndex(i);
            StateChanged();
            return true;
        }

        @Override
        public void Clear() {
            this.m_ids.Clear();
            super.Clear();
            if (this.m_pGUI != null) {
                // will clear all the GUI variables and will set m_water back to 0
                StateChanged();
            }
        }

        @Override
        public int Num() {
            return super.Num();
        }

        @Override
        public int GetSelection(String[] s, int size, int _sel /*= 0*/) {// returns the id, not the list index (or -1)
            if (s != null) {
//                s[0] = '\0';
                s[0] = "";
            }
            int sel = this.m_pGUI.State().GetInt(va("%s_sel_%d", this.m_name, _sel), "-1");
            if ((sel == -1) || (sel >= this.m_ids.Num())) {
                return -1;
            }
            if (s != null) {
                idStr.snPrintf(s, size, this.m_pGUI.State().GetString(va("%s_item_%d", this.m_name, sel), ""));
            }
            // don't let overflow
            if (sel >= this.m_ids.Num()) {
                sel = 0;
            }
            this.m_pGUI.SetStateInt(va("%s_selid_0", this.m_name), this.m_ids.oGet(sel));
            return this.m_ids.oGet(sel);
        }

        @Override
        public void SetSelection(int sel) {
            this.m_pGUI.SetStateInt(va("%s_sel_0", this.m_name), sel);
            StateChanged();
        }

        @Override
        public int GetNumSelections() {
            return this.m_pGUI.State().GetInt(va("%s_numsel", this.m_name));
        }

        @Override
        public boolean IsConfigured() {
            return this.m_pGUI != null;
        }

        @Override
        public void SetStateChanges(boolean enable) {
            this.m_stateUpdates = enable;
            StateChanged();
        }

        @Override
        public void Shutdown() {
            this.m_pGUI = null;
            this.m_name.Clear();
            Clear();
        }

        private void StateChanged() {
            int i;

            if (!this.m_stateUpdates) {
                return;
            }

            for (i = 0; i < Num(); i++) {
                this.m_pGUI.SetStateString(va("%s_item_%d", this.m_name, i), this.oGet(i).toString());
            }
            for (i = Num(); i < this.m_water; i++) {
                this.m_pGUI.SetStateString(va("%s_item_%d", this.m_name, i), "");
            }
            this.m_water = Num();
            this.m_pGUI.StateChanged(com_frameTime);
        }
    }
}
