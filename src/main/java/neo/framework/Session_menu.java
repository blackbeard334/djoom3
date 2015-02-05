/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package neo.framework;

import neo.framework.Session_local.fileTIME_T;
import neo.idlib.containers.List.cmp_t;

/**
 *
 */
public class Session_menu {

    /*
     ===============
     idListSaveGameCompare
     ===============
     */
    static class idListSaveGameCompare implements cmp_t<fileTIME_T> {

        @Override
        public int compare(final fileTIME_T a, final fileTIME_T b) {
            return (int) (b.timeStamp - a.timeStamp);
        }
    };
}
