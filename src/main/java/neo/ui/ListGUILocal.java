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
        private idStr m_name;
        private int m_water;
        private idList<Integer> m_ids;
        private boolean m_stateUpdates;
        //
        //

        public idListGUILocal() {
            m_pGUI = null;
            m_name = new idStr();
            m_water = 0;
            m_ids = new idList<>();
            m_stateUpdates = true;
        }
        // idListGUI interface

        @Override
        public void Config(idUserInterface pGUI, final String name) {
            m_pGUI = pGUI;
            m_name.oSet("" + name);
        }

        @Override
        public void Add(int id, final idStr s) {
            int i = m_ids.FindIndex(id);
            if (i == -1) {
                Append(s);
                m_ids.Append(id);
            } else {
                this.oSet(i, s);
            }
            StateChanged();
        }

        // use the element count as index for the ids
        @Override
        public void Push(final idStr s) {
            Append(s);
            m_ids.Append(m_ids.Num());
            StateChanged();
        }

        @Override
        public boolean Del(int id) {
            int i = m_ids.FindIndex(id);
            if (i == -1) {
                return false;
            }
            m_ids.RemoveIndex(i);
            this.RemoveIndex(i);
            StateChanged();
            return true;
        }

        @Override
        public void Clear() {
            m_ids.Clear();
            super.Clear();
            if (m_pGUI != null) {
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
            int sel = m_pGUI.State().GetInt(va("%s_sel_%d", m_name, _sel), "-1");
            if (sel == -1 || sel >= m_ids.Num()) {
                return -1;
            }
            if (s != null) {
                idStr.snPrintf(s, size, m_pGUI.State().GetString(va("%s_item_%d", m_name, sel), ""));
            }
            // don't let overflow
            if (sel >= m_ids.Num()) {
                sel = 0;
            }
            m_pGUI.SetStateInt(va("%s_selid_0", m_name), m_ids.oGet(sel));
            return m_ids.oGet(sel);
        }

        @Override
        public void SetSelection(int sel) {
            m_pGUI.SetStateInt(va("%s_sel_0", m_name), sel);
            StateChanged();
        }

        @Override
        public int GetNumSelections() {
            return m_pGUI.State().GetInt(va("%s_numsel", m_name));
        }

        @Override
        public boolean IsConfigured() {
            return m_pGUI != null;
        }

        @Override
        public void SetStateChanges(boolean enable) {
            m_stateUpdates = enable;
            StateChanged();
        }

        @Override
        public void Shutdown() {
            m_pGUI = null;
            m_name.Clear();
            Clear();
        }

        private void StateChanged() {
            int i;

            if (!m_stateUpdates) {
                return;
            }

            for (i = 0; i < Num(); i++) {
                m_pGUI.SetStateString(va("%s_item_%d", m_name, i), this.oGet(i).toString());
            }
            for (i = Num(); i < m_water; i++) {
                m_pGUI.SetStateString(va("%s_item_%d", m_name, i), "");
            }
            m_water = Num();
            m_pGUI.StateChanged(com_frameTime);
        }
    };
}
