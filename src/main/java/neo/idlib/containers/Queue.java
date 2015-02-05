package neo.idlib.containers;

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

    public static class idQueueTemplate<type> {//TODO:fix the nextOffset part.

        private final static int QUEUE_BLOCK_SIZE = 10;
        //
        private int first;
        private int last;
        private type[] queue      = (type[]) new Object[10];
        private int    nextOffset = 0;
        //
        //

        public idQueueTemplate() {
//            first = last = null;
        }

        public void Add(type element) {

            if (nextOffset >= queue.length) {
//		QUEUE_NEXT_PTR(last) = element;
                expandQueue();
            }
            queue[nextOffset] = element;
//            last = element;
            nextOffset++;
        }

        public type Get() {
            if (nextOffset < 0) {
                return null;
            }

            if (nextOffset <= queue.length - QUEUE_BLOCK_SIZE) {
                shrinkQueue();
            }

            return queue[nextOffset--];
        }

        private void expandQueue() {
            final type[] tempQueue = queue;
            queue = (type[]) new Object[queue.length + QUEUE_BLOCK_SIZE];

            System.arraycopy(tempQueue, 0, queue, 0, tempQueue.length);
        }

        private void shrinkQueue() {
            final type[] tempQueue = queue;
            queue = (type[]) new Object[queue.length - QUEUE_BLOCK_SIZE];

            System.arraycopy(tempQueue, 0, queue, 0, queue.length);
        }
    };
}
