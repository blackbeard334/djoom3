package neo.idlib.containers;

import neo.idlib.Text.Str.idStr;
import neo.idlib.containers.List.cmp_t;
import neo.idlib.containers.List.idList;
import neo.idlib.containers.StrList.idStrPtr;

/**
 *
 */
public class StrList {
    /*
     ===============================================================================

     idStrList

     ===============================================================================
     */

    public static class idStrList extends idList<idStr> {

        public idStrList() {
            super(idStr.class);
        }

        public idStrList(int newgranularity) {
            super(newgranularity, idStr.class);
        }

        /*
         ================
         idStrList::Sort

         Sorts the list of strings alphabetically. Creates a list of pointers to the actual strings and sorts the
         pointer list. Then copies the strings into another list using the ordered list of pointers.
         ================
         */
        @Override
        public void Sort(cmp_t compare) {
            int i;

            if (0 == num) {
                return;
            }

            idList<idStr> other = new idList<>();
            idList<idStrPtr> pointerList = new idList<>();

            pointerList.SetNum(num);
            for (i = 0; i < num; i++) {
                pointerList.oSet(i, this.oGet(i));
            }

            pointerList.Sort();

            other.SetNum(num);
            other.SetGranularity(granularity);
            for (i = 0; i < other.Num(); i++) {
                other.oSet(i, pointerList.oGet(i));
            }

            this.Swap(other);
        }

        /*
         ================
         idStrList::SortSubSection

         Sorts a subsection of the list of strings alphabetically.
         ================
         */
        @Override
        public void SortSubSection(int startIndex, int endIndex, cmp_t compare) {
            int i, s;

            if (0 == num) {
                return;
            }
            if (startIndex < 0) {
                startIndex = 0;
            }
            if (endIndex >= num) {
                endIndex = num - 1;
            }
            if (startIndex >= endIndex) {
                return;
            }

            idList<idStr> other = new idList<>();
            idList<idStrPtr> pointerList = new idList<>();

            s = endIndex - startIndex + 1;
            other.SetNum(s);
            pointerList.SetNum(s);
            for (i = 0; i < s; i++) {
                other.oSet(i, this.oGet(startIndex + i));
                pointerList.oSet(i, other.oGet(i));
            }

            pointerList.Sort();

            for (i = 0; i < s; i++) {
                this.oSet(startIndex + i, pointerList.oGet(i));
            }
        }

        @Override
        public int Size() {
            int s = 0;
            int i;

//	s = sizeof( *this );
            for (i = 0; i < Num(); i++) {
                s += this.oGet(i).Size();
            }

            return s;
        }

        /*
         ================
         idStrListSortPaths

         Sorts the list of path strings alphabetically and makes sure folders come first.
         ================
         */
        public static void idStrListSortPaths(idStrList list) {
            int i;

            if (0 == list.Num()) {
                return;
            }

            idList<idStr> other = new idList<>();
            idList<idStrPtr> pointerList = new idList<>();

            pointerList.SetNum(list.Num());
            for (i = 0; i < list.Num(); i++) {
                pointerList.oSet(i, list.oGet(i));
            }

            pointerList.Sort(new idListSortComparePaths());

            other.SetNum(list.Num());
            other.SetGranularity(list.GetGranularity());
            for (i = 0; i < other.Num(); i++) {
                other.oSet(i, pointerList.oGet(i));
            }

            list.Swap(other);
        }

        public int AddUnique(String obj) {
            return super.AddUnique(new idStr(obj));
        }

        public int Append(String obj) {
            return super.Append(new idStr(obj));
        }

    };

    class idStrPtrList extends idList<idStr> {
    }

    class idStrPtr extends idStr {
    }

    /*
     ===============================================================================

     idStrList path sorting

     ===============================================================================
     */

    /*
     ================
     idListSortComparePaths

     Compares two pointers to strings. Used to sort a list of string pointers alphabetically in idList<idStr>::Sort.
     ================
     */
    static class idListSortComparePaths implements cmp_t<idStr> {

        @Override
        public int compare(idStr a, idStr b) {
            return a.IcmpPath(b.toString());
        }
    }

    /*
     ================
     idListSortCompare<idStrPtr>

     Compares two pointers to strings. Used to sort a list of string pointers alphabetically in idList<idStr>::Sort.
     ================
     */
    public static class idListSortCompare implements cmp_t<idStr> {

        @Override
        public int compare(idStr a, idStr b) {
            return a.Icmp(b);
        }
    }
}
