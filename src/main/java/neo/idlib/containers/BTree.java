package neo.idlib.containers;

import neo.idlib.Heap.idBlockAlloc;

/**
 *
 */
@Deprecated //not used
public class BTree {

    /*
     ===============================================================================

     Balanced Search Tree

     ===============================================================================
     */
    public class idBTreeNode<objType, keyType> {

        public keyType key;			// key used for sorting
        public objType object;			// if != NULL pointer to object stored in leaf node
        public idBTreeNode<objType, keyType> parent;		// parent node
        public idBTreeNode<objType, keyType> next;		// next sibling
        public idBTreeNode<objType, keyType> prev;		// prev sibling
        public int numChildren;                 // number of children
        public idBTreeNode<objType, keyType> firstChild;		// first child
        public idBTreeNode<objType, keyType> lastChild;		// last child
    }

    public static class idBTree<objType, keyType> {

        private idBTreeNode<objType, keyType> root;
        private idBlockAlloc<idBTreeNode<objType, keyType>> nodeAllocator;
        //
        //

        public idBTree(int maxChildrenPerNode) {
//            assert (maxChildrenPerNode.intValue()>= 4);
            this.maxChildrenPerNode = maxChildrenPerNode;
            this.root = null;
        }
//public									~idBTree( void );
//

        public void Init() {
            this.root = AllocNode();
        }

        public void Shutdown() {
            this.nodeAllocator.Shutdown();
            this.root = null;
        }
//

        public idBTreeNode<objType, keyType> Add(objType object, keyType key) {						// add an object to the tree
            idBTreeNode<objType, keyType> node, child, newNode;

            if (this.root.numChildren >= this.maxChildrenPerNode) {
                newNode = AllocNode();
                newNode.key = this.root.key;
                newNode.firstChild = this.root;
                newNode.lastChild = this.root;
                newNode.numChildren = 1;
                this.root.parent = newNode;
                SplitNode(this.root);
                this.root = newNode;
            }

            newNode = AllocNode();
            newNode.key = key;
            newNode.object = object;

            for (node = this.root; node.firstChild != null; node = child) {

                if (GT(key,/*>*/ node.key)) {
                    node.key = key;
                }

                // find the first child with a key larger equal to the key of the new node
                for (child = node.firstChild; child.next != null; child = child.next) {
                    if (LTE(key,/*<=*/ child.key)) {
                        break;
                    }
                }

                if (child.object != null) {

                    if (LTE(key,/*<=*/ child.key)) {
                        // insert new node before child
                        if (child.prev != null) {
                            child.prev.next = newNode;
                        } else {
                            node.firstChild = newNode;
                        }
                        newNode.prev = child.prev;
                        newNode.next = child;
                        child.prev = newNode;
                    } else {
                        // insert new node after child
                        if (child.next != null) {
                            child.next.prev = newNode;
                        } else {
                            node.lastChild = newNode;
                        }
                        newNode.prev = child;
                        newNode.next = child.next;
                        child.next = newNode;
                    }

                    newNode.parent = node;
                    node.numChildren++;

//#ifdef BTREE_CHECK
//			CheckTree();
//#endif
                    return newNode;
                }

                // make sure the child has room to store another node
                if (child.numChildren >= this.maxChildrenPerNode) {
                    SplitNode(child);
                    if (LTE(key,/*<=*/ child.prev.key)) {
                        child = child.prev;
                    }
                }
            }

            // we only end up here if the root node is empty
            newNode.parent = this.root;
            this.root.key = key;
            this.root.firstChild = newNode;
            this.root.lastChild = newNode;
            this.root.numChildren++;

//#ifdef BTREE_CHECK
//	CheckTree();
//#endif
            return newNode;
        }

        public void Remove(idBTreeNode<objType, keyType> node) {				// remove an object node from the tree
            idBTreeNode<objType, keyType> parent;

            assert (node.object != null);

            // unlink the node from it's parent
            if (node.prev != null) {
                node.prev.next = node.next;
            } else {
                node.parent.firstChild = node.next;
            }
            if (node.next != null) {
                node.next.prev = node.prev;
            } else {
                node.parent.lastChild = node.prev;
            }
            node.parent.numChildren--;

            // make sure there are no parent nodes with a single child
            for (parent = node.parent; (parent != this.root) && (parent.numChildren <= 1); parent = parent.parent) {

                if (parent.next != null) {
                    parent = MergeNodes(parent, parent.next);
                } else if (parent.prev != null) {
                    parent = MergeNodes(parent.prev, parent);
                }

                // a parent may not use a key higher than the key of it's last child
                if (GT(parent.key /*>*/, parent.lastChild.key)) {
                    parent.key = parent.lastChild.key;
                }

                if (parent.numChildren > this.maxChildrenPerNode) {
                    SplitNode(parent);
                    break;
                }
            }
            for (; (parent != null) && (parent.lastChild != null); parent = parent.parent) {
                // a parent may not use a key higher than the key of it's last child
                if (GT(parent.key /*>*/, parent.lastChild.key)) {
                    parent.key = parent.lastChild.key;
                }
            }

            // free the node
            FreeNode(node);

            // remove the root node if it has a single internal node as child
            if ((this.root.numChildren == 1) && (this.root.firstChild.object == null)) {
                final idBTreeNode<objType, keyType> oldRoot = this.root;
                this.root.firstChild.parent = null;
                this.root = this.root.firstChild;
                FreeNode(oldRoot);
            }

//#ifdef BTREE_CHECK
//	CheckTree();
//#endif
        }
//

        public objType Find(keyType key) {									// find an object using the given key
            idBTreeNode<objType, keyType> node;

            for (node = this.root.firstChild; node != null; node = node.firstChild) {
                while (node.next != null) {
                    if (GTE(node.key, /*>=*/ key)) {
                        break;
                    }
                    node = node.next;
                }
                if (node.object != null) {
                    if (node.key == key) {
                        return node.object;
                    } else {
                        return null;
                    }
                }
            }
            return null;
        }

        public objType FindSmallestLargerEqual(keyType key) {				// find an object with the smallest key larger equal the given key
            idBTreeNode<objType, keyType> node;

            for (node = this.root.firstChild; node != null; node = node.firstChild) {
                while (node.next != null) {
                    if (GTE(node.key, key)) {
                        break;
                    }
                    node = node.next;
                }
                if (node.object != null) {
                    if (GTE(node.key, key)) {
                        return node.object;
                    } else {
                        return null;
                    }
                }
            }
            return null;
        }

        public objType FindLargestSmallerEqual(keyType key) {				// find an object with the largest key smaller equal the given key
            idBTreeNode<objType, keyType> node;

            for (node = this.root.lastChild; node != null; node = node.lastChild) {
                while (node.prev != null) {
                    if (LTE(node.key, key)) {
                        break;
                    }
                    node = node.prev;
                }
                if (node.object != null) {
                    if (LTE(node.key, key)) {
                        return node.object;
                    } else {
                        return null;
                    }
                }
            }
            return null;
        }
//

        public idBTreeNode<objType, keyType> GetRoot() {										// returns the root node of the tree
            return this.root;
        }

        public int GetNodeCount() {									// returns the total number of nodes in the tree
            return this.nodeAllocator.GetAllocCount();
        }

        public idBTreeNode<objType, keyType> GetNext(idBTreeNode<objType, keyType> node) {		// goes through all nodes of the tree
            if (node.firstChild != null) {
                return node.firstChild;
            } else {
                while ((node != null) && (node.next == null)) {
                    node = node.parent;
                }
                return node;
            }
        }

        public idBTreeNode<objType, keyType> GetNextLeaf(idBTreeNode<objType, keyType> node) {	// goes through all leaf nodes of the tree
            if (node.firstChild != null) {
                while (node.firstChild != null) {
                    node = node.firstChild;
                }
                return node;
            } else {
                while ((node != null) && (node.next == null)) {
                    node = node.parent;
                }
                if (node != null) {
                    node = node.next;
                    while (node.firstChild != null) {
                        node = node.firstChild;//TODO:what the fuck does this loop do?
                    }
                    return node;
                } else {
                    return null;
                }
            }
        }

        private idBTreeNode<objType, keyType> AllocNode() {
            final idBTreeNode<objType, keyType> node = this.nodeAllocator.Alloc();
            node.key = null;
            node.parent = null;
            node.next = null;
            node.prev = null;
            node.numChildren = 0;
            node.firstChild = null;
            node.lastChild = null;
            node.object = null;
            return node;
        }

        private void FreeNode(idBTreeNode<objType, keyType> node) {
            this.nodeAllocator.Free(node);
        }

        private void SplitNode(idBTreeNode<objType, keyType> node) {
            int i;
            idBTreeNode<objType, keyType> child, newNode;

            // allocate a new node
            newNode = AllocNode();
            newNode.parent = node.parent;

            // divide the children over the two nodes
            child = node.firstChild;
            child.parent = newNode;
            for (i = 3; i < node.numChildren; i += 2) {
                child = child.next;
                child.parent = newNode;
            }

            newNode.key = child.key;
            newNode.numChildren = node.numChildren / 2;
            newNode.firstChild = node.firstChild;
            newNode.lastChild = child;

            node.numChildren -= newNode.numChildren;
            node.firstChild = child.next;

            child.next.prev = null;
            child.next = null;

            // add the new child to the parent before the split node
            assert (node.parent.numChildren < this.maxChildrenPerNode);

            if (node.prev != null) {
                node.prev.next = newNode;
            } else {
                node.parent.firstChild = newNode;
            }
            newNode.prev = node.prev;
            newNode.next = node;
            node.prev = newNode;

            node.parent.numChildren++;
        }

        private idBTreeNode<objType, keyType> MergeNodes(idBTreeNode<objType, keyType> node1, idBTreeNode<objType, keyType> node2) {
            idBTreeNode<objType, keyType> child;

            assert (node1.parent == node2.parent);
            assert ((node1.next == node2) && (node2.prev == node1));
            assert ((node1.object == null) && (node2.object == null));
            assert ((node1.numChildren >= 1) && (node2.numChildren >= 1));

            for (child = node1.firstChild; child.next != null; child = child.next) {
                child.parent = node2;
            }
            child.parent = node2;
            child.next = node2.firstChild;
            node2.firstChild.prev = child;
            node2.firstChild = node1.firstChild;
            node2.numChildren += node1.numChildren;

            // unlink the first node from the parent
            if (node1.prev != null) {
                node1.prev.next = node2;
            } else {
                node1.parent.firstChild = node2;
            }
            node2.prev = node1.prev;
            node2.parent.numChildren--;

            FreeNode(node1);

            return node2;
        }
//

        private void CheckTree_r(idBTreeNode<objType, keyType> node, int numNodes) {
            int numChildren;
            idBTreeNode<objType, keyType> child;

            numNodes++;

            // the root node may have zero children and leaf nodes always have zero children, all other nodes should have at least 2 and at most maxChildrenPerNode children
            assert ((node == this.root) || ((node.object != null) && (node.numChildren == 0)) || ((node.numChildren >= 2) && (node.numChildren <= this.maxChildrenPerNode)));
            // the key of a node may never be larger than the key of it's last child
            assert ((node.lastChild == null) || LTE(node.key, node.lastChild.key));

            numChildren = 0;
            for (child = node.firstChild; child != null; child = child.next) {
                numChildren++;
                // make sure the children are properly linked
                if (child.prev == null) {
                    assert (node.firstChild == child);
                } else {
                    assert (child.prev.next == child);
                }
                if (child.next == null) {
                    assert (node.lastChild == child);
                } else {
                    assert (child.next.prev == child);
                }
                // recurse down the tree
                CheckTree_r(child, numNodes);
            }
            // the number of children should equal the number of linked children
            assert (numChildren == node.numChildren);
        }

        private void CheckTree() {
            final int numNodes = 0;
            idBTreeNode<objType, keyType> node, lastNode;

            CheckTree_r(this.root, numNodes);

            // the number of nodes in the tree should equal the number of allocated nodes
            assert (numNodes == this.nodeAllocator.GetAllocCount());

            // all the leaf nodes should be ordered
            lastNode = GetNextLeaf(GetRoot());
            if (lastNode != null) {
                for (node = GetNextLeaf(lastNode); node != null; lastNode = node, node = GetNextLeaf(node)) {
                    assert (LTE(lastNode.key, node.key));
                }
            }
        }
        private final int maxChildrenPerNode;
    }

    //Greater Than
    static boolean GT(Object object1, Object object2) {
        return ((Number) object1).doubleValue() > ((Number) object2).doubleValue();
    }

    //Greater Than or Equal
    static boolean GTE(Object object1, Object object2) {
        return ((Number) object1).doubleValue() >= ((Number) object2).doubleValue();
    }

    //Less Than
    static boolean LT(Object object1, Object object2) {
        return ((Number) object1).doubleValue() < ((Number) object2).doubleValue();
    }

    //Less Than or Equal
    static boolean LTE(Object object1, Object object2) {
        return ((Number) object1).doubleValue() <= ((Number) object2).doubleValue();
    }
}
