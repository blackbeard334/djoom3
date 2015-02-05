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
            if (top >= stack.length) {//reached top of stack
                expand();
            }

            stack[top++] = element;
        }

        public type Get() {//pop
            if (top < 0) {//reached bottom
                return null;
            }

            if (stack.length - STACK_BLOCK_SIZE > 0 && top < stack.length - STACK_BLOCK_SIZE) {//reached block threshold
                shrink();
            }

            return stack[top--];
        }

        private void expand() {
            final type[] temp = stack;
            stack = (type[]) new Object[stack.length + STACK_BLOCK_SIZE];

            System.arraycopy(temp, 0, stack, 0, temp.length);
        }

        private void shrink() {
            final type[] temp = stack;
            stack = (type[]) new Object[stack.length - STACK_BLOCK_SIZE];

            System.arraycopy(temp, 0, stack, 0, stack.length);
        }
    };
}
