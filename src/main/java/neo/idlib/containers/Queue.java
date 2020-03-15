package neo.idlib.containers;

import java.util.LinkedList;

/**
 *
 */
public class Queue {//TODO:test this
    /*
     ===============================================================================

     Queue template

     ===============================================================================
     */
//#define idQueue( type, next )		idQueueTemplate<type, (int)&(((type*)NULL)->next)>

    public static class idQueueTemplate<type> extends LinkedList<type> {//TODO:fix the nextOffset part.

//        private final static int QUEUE_BLOCK_SIZE = 10;
//        //
//        private int first;
//        private int last;
//        private type[] queue      = (type[]) new Object[10];
//        private int    nextOffset = 0;
//        //
//        //

        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public idQueueTemplate() {
//            first = last = null;
        }

        public boolean Add(type element) {
//            if (nextOffset >= queue.length) {
////		QUEUE_NEXT_PTR(last) = element;
//                expandQueue();
//            }
//            queue[nextOffset] = element;
////            last = element;
//            nextOffset++;
            return super.add(element);
        }

        public type Get() {
            if (super.isEmpty()) {
                return null;
            }
//
//            if (nextOffset <= queue.length - QUEUE_BLOCK_SIZE) {
//                shrinkQueue();
//            }
//
//            return queue[--nextOffset];
            return super.pop();
        }

//        private void expandQueue() {
//            final type[] tempQueue = queue;
//            queue = (type[]) new Object[queue.length + QUEUE_BLOCK_SIZE];
//
//            System.arraycopy(tempQueue, 0, queue, 0, tempQueue.length);
//        }
//
//        private void shrinkQueue() {
//            final type[] tempQueue = queue;
//            queue = (type[]) new Object[queue.length - QUEUE_BLOCK_SIZE];
//
//            System.arraycopy(tempQueue, 0, queue, 0, queue.length);
//        }
    }
}
