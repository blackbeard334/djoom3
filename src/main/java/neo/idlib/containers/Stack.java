package neo.idlib.containers;

/**
 *
 */
public class Stack {
    /*
     ===============================================================================

     Stack template

     ===============================================================================
     */

    class idStackTemplate<type> {

        private int top;
        //        private type bottom;
        //
        private static final int    STACK_BLOCK_SIZE = 10;
        private              type[] stack            = (type[]) new Object[10];
        //
        //

        public idStackTemplate() {
        }
//

        public void Add(type element) {//push
            if (this.top >= this.stack.length) {//reached top of stack
                expand();
            }

            this.stack[this.top++] = element;
        }

        public type Get() {//pop
            if (this.top < 0) {//reached bottom
                return null;
            }

            if (((this.stack.length - STACK_BLOCK_SIZE) > 0) && (this.top < (this.stack.length - STACK_BLOCK_SIZE))) {//reached block threshold
                shrink();
            }

            return this.stack[this.top--];
        }

        private void expand() {
            final type[] temp = this.stack;
            this.stack = (type[]) new Object[this.stack.length + STACK_BLOCK_SIZE];

            System.arraycopy(temp, 0, this.stack, 0, temp.length);
        }

        private void shrink() {
            final type[] temp = this.stack;
            this.stack = (type[]) new Object[this.stack.length - STACK_BLOCK_SIZE];

            System.arraycopy(temp, 0, this.stack, 0, this.stack.length);
        }
    }
}
