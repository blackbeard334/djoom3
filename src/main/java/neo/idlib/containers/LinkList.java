package neo.idlib.containers;

/**
 * //TODO:implement starcraft linked list
 */
public class LinkList {
    /*
     ==============================================================================

     idLinkList

     Circular linked list template

     ==============================================================================
     */

    public static class idLinkList<type> {

        private idLinkList<type> head;
        private idLinkList<type> next;
        private idLinkList<type> prev;
        private type owner;
        //
        //
        private static int DBG_counter = 0;
        private final  int DBG_count = DBG_counter++;

        /*
         ================
         idLinkList<type>::idLinkList

         Node is initialized to be the head of an empty list
         ================
         */
        public idLinkList() {
            this.owner = null;
            this.head = this;
            this.next = this;
            this.prev = this;
        }
        
        public idLinkList(final type owner) {
            this();
            this.owner = owner;
        }
//public						~idLinkList();
//

        /*
         ================
         idLinkList<type>::IsListEmpty

         Returns true if the list is empty.
         ================
         */
        public boolean IsListEmpty() {
            return this.head.next == this.head;
        }

        /*
         ================
         idLinkList<type>::InList

         Returns true if the node is in a list.  If called on the head of a list, will always return false.
         ================
         */
        public boolean InList() {
            return this.head != this;
        }

        /*
         ================
         idLinkList<type>::Num

         Returns the number of nodes in the list.
         ================
         */
        public int Num() {
            idLinkList<type> node;
            int num;

            num = 0;
            for (node = this.head.next; node != this.head; node = node.next) {
                num++;
            }

            return num;
        }

        /*
         ================
         idLinkList<type>::Clear

         If node is the head of the list, clears the list.  Otherwise it just removes the node from the list.
         ================
         */
        public void Clear() {
            if (this.head == this) {
                while (this.next != this) {
                    this.next.Remove();
                }
            } else {
                Remove();
            }
        }

        /*
         ================
         idLinkList<type>::InsertBefore

         Places the node before the existing node in the list.  If the existing node is the head,
         then the new node is placed at the end of the list.
         ================
         */
        public void InsertBefore(idLinkList<type> node) {
            Remove();

            this.next = node;
            this.prev = node.prev;
            node.prev = this;
            this.prev.next = this;
            this.head = node.head;
        }

        /*
         ================
         idLinkList<type>::InsertAfter

         Places the node after the existing node in the list.  If the existing node is the head,
         then the new node is placed at the beginning of the list.
         ================
         */
        public void InsertAfter(idLinkList<type> node) {
            Remove();

            this.prev = node;
            this.next = node.next;
            node.next = this;
            this.next.prev = this;
            this.head = node.head;
        }

        /*
         ================
         idLinkList<type>::AddToEnd

         Adds node at the end of the list
         ================
         */
        public void AddToEnd(idLinkList<type> node) {
            InsertBefore(node.head);
        }

        /*
         ================
         idLinkList<type>::AddToFront

         Adds node at the beginning of the list
         ================
         */
        public void AddToFront(idLinkList<type> node) {
            InsertAfter(node.head);
        }

        /*
         ================
         idLinkList<type>::Remove

         Removes node from list
         ================
         */
        public void Remove() {
            this.prev.next = this.next;
            this.next.prev = this.prev;

            this.next = this;
            this.prev = this;
            this.head = this;
        }

        /*
         ================
         idLinkList<type>::Next

         Returns the next object in the list, or NULL if at the end.
         ================
         */
		public type Next() {
            if ((null == this.next) || (this.next == this.head)) {
                return null;
            }
            if (this.next.owner != null) {
                final type obj = this.next.owner;
                return obj;
            } else {
            	// Done ToDo: beware missing links in linked lists(a.next.next.next.NULL.next).
                final type obj = this.next.Next();
                return obj;
            }
        }

        /*
         ================
         idLinkList<type>::Prev

         Returns the previous object in the list, or NULL if at the beginning.
         ================
         */
        public type Prev() {
            if ((null == this.prev) || (this.prev == this.head)) {
                return null;
            }
            if (this.prev.owner != null) {
                final type obj = this.prev.owner;
                return obj;
            } else {
            	// Done ToDo: beware missing links in linked lists(a.next.next.next.NULL.next).
            	final type obj = this.prev.Prev();
                return obj;
            }
        }
//

        /*
         ================
         idLinkList<type>::Owner

         Gets the object that is associated with this node.
         ================
         */
        public type Owner() {
            return this.owner;
        }

        /*
         ================
         idLinkList<type>::SetOwner

         Sets the object that this node is associated with.
         ================
         */
        public void SetOwner(type object) {
            this.owner = object;
        }
//

        /*
         ================
         idLinkList<type>::ListHead

         Returns the head of the list.  If the node isn't in a list, it returns
         a pointer to itself.
         ================
         */
        public idLinkList<type> ListHead() {
            return this.head;
        }

        /*
         ================
         idLinkList<type>::NextNode

         Returns the next node in the list, or NULL if at the end.
         ================
         */
        public idLinkList<type> NextNode() {
            if (this.next == this.head) {
                return null;
            }
            return this.next;
        }

        /*
         ================
         idLinkList<type>::PrevNode

         Returns the previous node in the list, or NULL if at the beginning.
         ================
         */
        public idLinkList<type> PrevNode() {
            if (this.prev == this.head) {
                return null;
            }
            return this.prev;
        }
    }
}
