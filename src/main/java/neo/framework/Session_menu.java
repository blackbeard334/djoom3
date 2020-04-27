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

        /**
         * Ordinary sort, but with <b>null</b> objects at the end.
         */
        @Override
        public int compare(final fileTIME_T a, final fileTIME_T b) {
            //nulls should come at the end
            if (null == a) {
                if (null == b) {
                    return 0;
                }
                return 1;
            }

            if (null == b) {
                return -1;
            }

            return (int) (b.timeStamp - a.timeStamp);
        }
    };
}
