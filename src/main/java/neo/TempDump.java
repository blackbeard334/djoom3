package neo;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import neo.CM.CollisionModel_local.cm_polygon_s;
import static neo.Renderer.Material.MAX_ENTITY_SHADER_PARMS;
import neo.Renderer.Material.idMaterial;
import neo.Renderer.Model.idRenderModel;
import static neo.Renderer.RenderWorld.MAX_GLOBAL_SHADER_PARMS;
import static neo.Renderer.RenderWorld.MAX_RENDERENTITY_GUI;
import neo.Renderer.RenderWorld.deferredEntityCallback_t;
import neo.Renderer.RenderWorld.renderView_s;
import neo.Sound.sound.idSoundEmitter;
import neo.framework.DeclSkin.idDeclSkin;
import neo.idlib.BV.Bounds.idBounds;
import neo.idlib.CmdArgs.idCmdArgs;
import neo.idlib.Lib.idException;
import neo.idlib.Text.Str.idStr;
import neo.idlib.geometry.JointTransform.idJointMat;
import neo.idlib.math.Curve;
import neo.idlib.math.Matrix.idMat3;
import neo.idlib.math.Vector.idVec3;
import neo.ui.UserInterface.idUserInterface;
import org.lwjgl.BufferUtils;
import org.lwjgl.openal.AL10;

/**
 *
 */
public class TempDump {//TODO:rename/refactor to ToolBox or something

    public static abstract class void_callback<type> {

        public abstract void run(type... objects) throws idException;
    }

    public static abstract class argCompletion_t<E> {
        //TODO
//    public abstract void run(type... objects);

        public abstract void load(final idCmdArgs args, void_callback<String> callback);
    }

    /**
     * Our humble java implementation of the C++ strlen function, with NULL
     * checks.
     *
     * @param str a char array.
     * @return -1 if the array is NULL or the location of the first terminator.
     */
    public static int strLen(final char[] str) {
        int len;

        if (NOT(str)) {
            return -1;
        }

        for (len = 0; len < str.length; len++) {
            if (str[len] == '\0') {
                break;
            }
        }

        return len;
    }

    public static int strLen(final String str) {
        return strLen(str.toCharArray());
    }

    public static boolean memcmp(final int[] ptr1, final int[] ptr2, final int size_t) {
        return memcmp(ptr1, 0, ptr2, 0, size_t);
    }

    public static boolean memcmp(final int[] ptr1, final int p1_offset, final int[] ptr2, final int p2_offset, final int size_t) {
        for (int i = 0; i < size_t; i++) {
            if (ptr1[p1_offset + i] != ptr2[p2_offset + i]) {
                return false;
            }
        }
        return true;
    }

    public static boolean memcmp(byte[] a, byte[] b, int length) {
        if (null == a || null == b
                || a.length < length || b.length < length) {
            return false;
        }

        return (Arrays.equals(
                Arrays.copyOf(a, length),
                Arrays.copyOf(b, length)));
    }

    @Deprecated
    public static int sizeof(char[] object) {
        throw new UnsupportedOperationException();
    }

    @Deprecated
    public static int sizeof(Object object) {
        throw new UnsupportedOperationException();
    }

    /**
     * returns the serialized size of the object in bytes.<p>
     * NB: the output of this method should ALWAYS be tagged <b>transient</b>.
     */
    public static int SERIAL_SIZE(final Object object) {
        return -1;
//        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
//                ObjectOutputStream oos = new ObjectOutputStream(baos)) {
//            oos.writeObject(object);
//            return baos.toByteArray().length;
//        } catch (IOException ex) {
//            Logger.getLogger(object.getClass().getName()).log(Level.SEVERE, null, ex);
//        }
//
//        throw new RuntimeException("unable to determine size!");
    }

    /**
     *
     * @param unknownArray our unknown array.
     * @return -1 for <b>NULL</b> objects or <b>non-arrays</b> and the array's
     * dimensions for actual arrays.
     */
    public static int arrayDimensions(Object unknownArray) {
        if (null == unknownArray) {
            return -1;
        }

        return unknownArray.getClass().toString().split("\\[").length - 1;
    }

    public static byte[] flatten(byte[][] input) {
        final int height = input.length;
        final int width = input[0].length;
        byte[] output = new byte[height * width];

        for (int a = 0; a < height; a++) {
            System.arraycopy(input[a], 0, output, width, width);
        }

        return output;
    }

    public static byte[] flatten(byte[][][] input) {
        final int height = input.length;
        final int width = input[0].length;
        final int length = input[0][0].length;
        byte[] output = new byte[height * width * length];

        for (int a = 0; a < height; a++) {
            final int x = a * width * length;
            for (int b = 0; b < width; b++) {
                final int y = b * length;
                System.arraycopy(input[a][b], 0, output, x + y, length);
            }
        }

        return output;
    }

    /**
     *
     * @param character character to insert.
     * @param index position at which the character is inserted.
     * @param string the input string
     * @return substring before <b>index</b> + character + substring after
     * <b>index</b>.
     */
    public static String replaceByIndex(final char character, final int index, final String string) {
        return string.substring(0, index) + character + string.substring(index + 1);
    }

    public static boolean isNotNullOrEmpty(final String stringy) {
        return stringy != null && !stringy.isEmpty() && !stringy.startsWith("\0");
    }

    public static boolean isNotNullOrEmpty(final idStr stringy) {
        return stringy != null && !stringy.IsEmpty() && '\0' != stringy.oGet(0);
    }

    /**
     * @return -1 if <b>v1</b> not in <b>vList</b>.
     */
    public static int indexOf(Object v1, Object[] vList) {
        int i;

        if (v1 != null && vList != null) {
            for (i = 0; i < vList.length; i++) {
                if (vList[i].equals(v1)) {
                    return i;
                }
            }
        }

        //we should NEVER get here!
        return -1;
    }

    /**
     * Equivalent to <b>!object</b>.
     *
     * @param objects
     * @return True if <b>ALL</b> objects[0...i] = null.
     */
    public static boolean NOT(final Object... objects) {
        //TODO: make sure incoming object isn't Integer or Float...etc.
        if (objects == null) return true;

        for (Object o : objects) {
            if (o != null) {
                return false;
            }
        }
        return true;
    }

    /**
     *
     * @param number
     * @return
     */
    public static int SNOT(final double number) {
        return (0 == number ? 1 : 0);
    }

    public static boolean NOT(final double number) {
        return (0 == number);
    }

    /**
     * Enum TO Int
     *
     * ORDINALS!! mine arch enemy!!
     */
    public static int etoi(Enum enumeration) {
        return enumeration.ordinal();
    }

    /**
     * Boolean TO Int
     */
    public static int btoi(boolean bool) {
        return bool ? 1 : 0;
    }

    /**
     * Byte TO Int
     */
    public static int btoi(byte b) {
        return b & 0xFF;
    }

    /**
     * Byte TO Int
     */
    public static int btoi(ByteBuffer b) {
        return b.get(0) & 0xFF;
    }

    /**
     * Int TO Boolean
     */
    public static boolean itob(int i) {
        return i != 0;
    }

    public static int ftoi(float f) {
        return Float.floatToIntBits(f);
    }

    public static int atoi(String ascii) {
        try {
            return Integer.parseInt(ascii.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public static boolean atob(String ascii) {
        return itob(atoi(ascii));
    }

    public static int atoi(idStr ascii) {
        return atoi(ascii.toString());
    }

    public static int atoi(char[] ascii) {
        return atoi(ctos(ascii));
    }

    public static float atof(String ascii) {
        try {
            return Float.parseFloat(ascii.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public static float atof(idStr ascii) {
        return atof(ascii.toString());
    }

    public static String ctos(char[] ascii) {//TODO:rename this moronic overloading!

        if (NOT(ascii)) {
            return null;
        }

        for (int a = 0; a < ascii.length; a++) {
            if ('\0' == ascii[a]) {
                return new String(ascii).substring(0, a);
            }
        }

        return new String(ascii);
    }

    public static String ctos(char ascii) {

        return "" + ascii;
    }

    public static String btos(byte[] bytes, int offset) {

        if (NOT(bytes)) {
            return null;
        }

        return new String(Arrays.copyOf(bytes, offset));
    }

    public static String btos(byte[] bytes) {
        return btos(bytes, 0);
    }

    public static ByteBuffer atobb(String ascii) {

        if (NOT(ascii)) {
            return null;
        }

//        return ByteBuffer.wrap(ascii.getBytes());
        return Charset.forName("UTF-8").encode(ascii);
    }

    public static ByteBuffer atobb(idStr ascii) {

        if (NOT(ascii)) {
            return null;
        }

        return atobb(ascii.toString());
    }

    public static ByteBuffer atobb(char[] ascii) {

        if (NOT(ascii)) {
            return null;
        }

        return atobb(ctos(ascii));
    }

    public static ByteBuffer stobb(short[] arrau) {
        ByteBuffer buffer;

        if (NOT(arrau)) {
            return null;
        }
        buffer = ByteBuffer.allocate(arrau.length * 2);
        buffer.asShortBuffer().put(arrau);

        return (ByteBuffer) buffer.flip();
    }

    public static CharBuffer atocb(String ascii) {

        if (NOT(ascii)) {
            return null;
        }

        return CharBuffer.wrap(ascii.toCharArray());
    }

    public static CharBuffer bbtocb(final ByteBuffer buffer) {

//        buffer.rewind();
//        return Charset.forName("UTF-8").decode(buffer);
        return Charset.forName("ISO-8859-1").decode(buffer);
    }

    public static String bbtoa(final ByteBuffer buffer) {

        return bbtocb(buffer).toString();
    }

    public static ByteBuffer wrapToNativeBuffer(final byte[] bytes) {

        if (null == bytes) {
            return null;
        }

        return (ByteBuffer) BufferUtils.createByteBuffer(bytes.length).put(bytes).flip();
    }

    /**
     * Integer array TO Int array
     */
    public static int[] itoi(Integer[] integerArray) {
        int[] intArray = new int[integerArray.length];
        for (int a = 0; a < intArray.length; a++) {
            intArray[a] = integerArray[a];
        }

        return intArray;
    }

    public static byte[] itob(int[] intArray) {
        ByteBuffer buffer = ByteBuffer.allocate(intArray.length * 4);
        buffer.asIntBuffer().put(intArray);

        return buffer.array();
    }

    public static int[] btoia(ByteBuffer buffer) {
        int[] intArray = new int[buffer.capacity() / 4];

        for (int i = 0; i < intArray.length; i++) {
            intArray[i] = buffer.getInt(4 * i);
        }

        return intArray;
    }

    public static long ntohl(byte[] ip) {
        ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.put(ip);
        buffer.flip();
        buffer.limit(8);

        return buffer.getLong(0);
    }

    public static Set<StandardOpenOption> fopenOptions(String mode) {
        Set<StandardOpenOption> temp = new HashSet<>();

        if (null == mode) {
            return null;
        }

        //it's all binary here.
        mode = mode.replace("b", "").replace("t", "");

        if (mode.contains("r")) {
            temp.add(StandardOpenOption.READ);
            if (mode.contains("r+")) {
                temp.add(StandardOpenOption.WRITE);
            }
        }
        if (mode.contains("w")) {
            temp.add(StandardOpenOption.CREATE);
            temp.add(StandardOpenOption.TRUNCATE_EXISTING);
            temp.add(StandardOpenOption.WRITE);
            if (mode.contains("w+")) {
                temp.add(StandardOpenOption.READ);
            }
        }
        if (mode.contains("a")) {
            temp.add(StandardOpenOption.APPEND);
            temp.add(StandardOpenOption.CREATE);
            temp.add(StandardOpenOption.WRITE);
            if (mode.contains("a+")) {
                temp.add(StandardOpenOption.READ);
            }
        }

        return temp;
    }

    public static void fprintf(FileChannel logFile, String text) throws IOException {
        logFile.write(ByteBuffer.wrap(text.getBytes()));
    }

    public static long[] reinterpret_cast_long_array(final byte[] array) {
        final long[] temp = new long[array.length];

        for (int b = 0, l = 0; b < array.length; l++) {
            temp[l] |= (array[b++] & 0xFFL) << 56;
            temp[l] |= (array[b++] & 0xFFL) << 48;
            temp[l] |= (array[b++] & 0xFFL) << 40;
            temp[l] |= (array[b++] & 0xFFL) << 32;
            temp[l] |= (array[b++] & 0xFFL) << 24;
            temp[l] |= (array[b++] & 0xFFL) << 16;
            temp[l] |= (array[b++] & 0xFFL) << 8;
            temp[l] |= (array[b++] & 0xFFL) << 0;
        }

        return temp;
    }

    public static Object dynamic_cast(Class glass, Object object) {
        if (glass.isInstance(object)) {
            return object;
        }
        return null;
    }

    /**
     * Prints the call stack from main to the point the function is called from.
     *
     * @param text some we would like to be put on top of our block.
     */
    public static void printCallStack(final String text) {

        final StackTraceElement[] elements = Thread.currentThread().getStackTrace();

        System.out.printf("----------------%s----------------\n", text);
        //e=2, skip current call, and calling class.
        for (int e = 2; e < elements.length; e++) {
            System.out.printf("%s.%s\n", elements[e].getClassName(), elements[e].getMethodName());
        }
        System.out.printf("------------------------------\n");
    }
    private static final Map<String, Integer> CALL_STACK_MAP = new HashMap<>();

    public static void countCallStack() {

        final StackTraceElement[] elements = Thread.currentThread().getStackTrace();

        //e=2, skip current call, and calling class.
        for (int e = 2; e < elements.length; e++) {
            final String key = String.format("%s.%s->%d\n", elements[e].getClassName(), elements[e].getMethodName(), elements[e].getLineNumber());

            if (CALL_STACK_MAP.containsKey(key)) {
                int value = CALL_STACK_MAP.get(key);
                CALL_STACK_MAP.put(key, value + 1);//increment
            } else {
                CALL_STACK_MAP.put(key, 1);
            }
        }
    }
    
    private static void breakOnALError() {
        final int e;
        if ((e = AL10.alGetError()) != 0) {
            throw new RuntimeException(e + " minutes, to miiiiiiiidnight!");
        }
    }

    public static void printCallStackCount() {
        System.out.println(Arrays.toString(CALL_STACK_MAP.entrySet().toArray()));
    }

    @Deprecated
    public static <T> T[] allocArray(Class<T> clazz, int length) {
        
        T[] array = (T[]) Array.newInstance(clazz, length);

        for (int a = 0; a < length; a++) {
            try {
                array[a] = (T) clazz.getConstructor().newInstance();
            } catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                throw new TODO_Exception();//missing default constructor
            }
        }

        return array;
    }
    
    /**
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     */
    @Deprecated
    public static interface SERiAL extends Serializable {//TODO:remove Serializable

        public static final int SIZE = Integer.MIN_VALUE;
        public static final int BYTES = SIZE / Byte.SIZE;

        /**
         * Prepares an <b>empty</b> ByteBuffer representation of the class for
         * reading.
         *
         * @return
         */
        public ByteBuffer AllocBuffer();

        /**
         * Reads the ByteBuffer and converts and sets its values to the current
         * object.
         *
         * @param buffer
         */
        public void Read(final ByteBuffer buffer);

        /**
         * Prepares a ByteBuffer representation of the class for writing.
         *
         * @return
         */
        public ByteBuffer Write();
    };

    /**
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     */
    public static interface NiLLABLE<type> {

        public type oSet(type node);

        public boolean isNULL();

    };

    /**
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     */    

    public static final class reflects {

        /**
         *
         *
         *
         *
         *
         *
         *
         *
         *
         *
         *
         *
         *
         *
         *
         *
         *
         *
         *
         *
         */
        private final static String GET_DIMENSION = "GetDimension";
        private final static String ZERO = "Zero";
        private final static String O_GET = "oGet";
        private final static String O_SET = "oSet";
        private final static String O_MULTIPLY = "oMultiply";
        private final static String O_PLUS = "oPlus";
        private final static String O_MINUS = "oMinus";

        public static int GetDimension(Object object) {
            final Class clazz = object.getClass();
            int returnValue = 0;

            try {
                Method getDimension = clazz.getDeclaredMethod(GET_DIMENSION);
                returnValue = (int) getDimension.invoke(object);
            } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                Logger.getLogger(Curve.class.getName()).log(Level.SEVERE, null, ex);
            }

            return returnValue;
        }

        public static void Zero(Object object) {
            final Class clazz = object.getClass();
            Method getDimension;

            try {
                getDimension = clazz.getDeclaredMethod(ZERO);
                getDimension.invoke(object);
            } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                Logger.getLogger(Curve.class.getName()).log(Level.SEVERE, null, ex);
            }

        }

        public static Object _Get(Object object, final String declaredField) {
            final Class clazz = object.getClass();
            Field field;
            Object returnObject = null;

            try {
                field = clazz.getDeclaredField(declaredField);
                returnObject = field.get(object);
            } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException ex) {
                Logger.getLogger(TempDump.class.getName()).log(Level.SEVERE, null, ex);
            }

            return returnObject;
        }

        public static float _Get(Object object, int index) {
            return _GetMul(object, index, 1);//TODO:you know what to do
        }

        public static float _GetMul(Object object, int index, float value) {
            final Class clazz = object.getClass();
            float returnValue = 0;
            Method oGet, oMultiply;
            Object returnObject;

            try {
//                System.out.printf("%s\n\n", Arrays.toString(clazz.getDeclaredMethods()));
                oGet = clazz.getDeclaredMethod(O_GET, int.class);
                returnObject = oGet.invoke(object, index);
                try {
                    oMultiply = returnObject.getClass().getDeclaredMethod(O_MULTIPLY);
                    returnValue = (float) oMultiply.invoke(returnObject, value);//object becomes float when multiplied(idMat)
                } catch (NoSuchMethodException ex) {
                    returnValue = ((float) returnObject) * value;//object that has float(idVec)
                }
            } catch (NoSuchMethodException ex) {
                returnValue = ((float) object) * value;//float
            } catch (SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                Logger.getLogger(Curve.class.getName()).log(Level.SEVERE, null, ex);
            }

            return returnValue;
        }

        public static float _GetGet(Object object, int x, int y) {
            final Class clazz = object.getClass();
            float returnValue = 0;
            Method oGet, oGet2;
            Object returnObject;

            try {
                oGet = clazz.getDeclaredMethod(O_GET);
                returnObject = oGet.invoke(object, x);
                oGet2 = returnObject.getClass().getDeclaredMethod(O_GET);
                returnValue = (float) oGet2.invoke(returnObject, y);
            } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                Logger.getLogger(Curve.class.getName()).log(Level.SEVERE, null, ex);
            }

            return returnValue;
        }

        public static float _GetSet(Object object, int x, int y, float value) {
            final Class clazz = object.getClass();
            float returnValue = 0;
            Method oGet, oGet2;
            Object returnObject;

            try {
                oGet = clazz.getDeclaredMethod(O_GET);
                returnObject = oGet.invoke(object, x);
                oGet2 = returnObject.getClass().getDeclaredMethod(O_SET);
                returnValue = (float) oGet2.invoke(returnObject, y, value);
            } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                Logger.getLogger(Curve.class.getName()).log(Level.SEVERE, null, ex);
            }

            return returnValue;
        }

        public static Object _Multiply(final Object object1, final Object object2) {
            return ooOOoooOOoo(object1, object2, O_MULTIPLY);
        }

        public static Object _Plus(final Object object1, final Object object2) {
            return ooOOoooOOoo(object1, object2, O_PLUS);
        }

        public static Object _Minus(final Object object1, final Object object2) {
            return ooOOoooOOoo(object1, object2, O_MINUS);
        }

        /**
         * Resolves the object types and processes the mathematical operation
         * accordingly, whether it's <b>overridden</b>(e.g oPlus, oMinus..etc)
         * or not.
         *
         * @param object1
         * @param object2
         * @param O_METHOD
         * @return
         */
        private static Object ooOOoooOOoo(final Object object1, final Object object2, final String O_METHOD) {
            final Class class1 = object1.getClass();
            final Class class2 = object2.getClass();
            final Method method1;
            final Method method2;
            Object returnObject = null;

            try {
                method1 = class1.getDeclaredMethod(O_METHOD, class2);
                returnObject = method1.invoke(object1, object2);
            } catch (NoSuchMethodException nox) {
                try {//try teh other way around.
                    method2 = class2.getDeclaredMethod(O_METHOD, class1);
                    returnObject = method2.invoke(object2, object1);
                } catch (Exception ex) {
                    //we should only get here if both our objects are primitives.
                    switch (O_METHOD) {
                        case O_PLUS:
                            returnObject = Double.parseDouble(object1.toString()) + Double.parseDouble(object2.toString());//both objects are integrals
                            break;
                        case O_MINUS:
                            returnObject = Double.parseDouble(object1.toString()) - Double.parseDouble(object2.toString());//both objects are integrals
                            break;
                        case O_MULTIPLY:
                        default:
                            returnObject = Double.parseDouble(object1.toString()) * Double.parseDouble(object2.toString());//both objects are integrals
                    }
//                    throw nox;//catch it there↓↓
                }
            } catch (SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {//←←here
                Logger.getLogger(TempDump.class.getName()).log(Level.SEVERE, null, ex);
            }

            return returnObject;
        }
    }

    public static class Atomics {

        public static class renderViewShadow {

            public int[] viewID = {0};
            //
            public int[] x = {0}, y = {0}, width = {0}, height = {0};
            //
            public float[] fov_x = {0}, fov_y = {0};
            public idVec3 vieworg = new idVec3();
            public idMat3 viewaxis = new idMat3();
            //
            public boolean[] cramZNear = {false};
            public boolean[] forceUpdate = {false};
            //
            public int[] time = {0};
            public float[][] shaderParms = new float[MAX_GLOBAL_SHADER_PARMS][];
            public idMaterial globalMaterial = new idMaterial();
        };

        public static class renderEntityShadow {

            public idRenderModel hModel;
            //
            public int[] entityNum = {0};
            public int[] bodyId = {0};
            //      
            public idBounds bounds;
            public deferredEntityCallback_t callback;
            //
            public ByteBuffer callbackData;
            //
            public int[] suppressSurfaceInViewID = {0};
            public int[] suppressShadowInViewID = {0};
            //
            public int[] suppressShadowInLightID = {0};
            //
            public int[] allowSurfaceInViewID = {0};
            //
            public idVec3 origin;
            public idMat3 axis;
            //
            public idMaterial customShader;
            public idMaterial referenceShader;
            public idDeclSkin customSkin;
            public idSoundEmitter referenceSound;
            public float[][] shaderParms = new float[MAX_ENTITY_SHADER_PARMS][];
            // 
            public idUserInterface[] gui = new idUserInterface[MAX_RENDERENTITY_GUI];
            //
            public renderView_s remoteRenderView;
            //
            public int[] numJoints = {0};
            public idJointMat[] joints;
            //
            public float[] modelDepthHack = {0};
            //
            public boolean[] noSelfShadow = {false};
            public boolean[] noShadow = {false};
            //
            public boolean[] noDynamicInteractions = {false};
            //
            public boolean[] weaponDepthHack = {false};
            // 
            public int[] forceUpdate = {0};
            public int[] timeGroup = {0};
            public int[] xrayIndex = {0};
        };

        public static class renderLightShadow {

            public idMat3 axis;
            public idVec3 origin;
            //
            public int[] suppressLightInViewID = {0};
            //
            public int[] allowLightInViewID = {0};
            //
            public boolean[] noShadows = {false};
            public boolean[] noSpecular = {false};
            //
            public boolean[] pointLight = {false};
            public boolean[] parallel = {false};
            public idVec3 lightRadius;
            public idVec3 lightCenter;
            //
            public idVec3 target;
            public idVec3 right;
            public idVec3 up;
            public idVec3 start;
            public idVec3 end;
            //
            public idRenderModel prelightModel;
            //
            public int[] lightId = {0};
            //
            //
            public idMaterial shader;
            public float[] shaderParms = new float[MAX_ENTITY_SHADER_PARMS];
            public idSoundEmitter referenceSound;
        };
    }

    /**
     * Decorator classes so when we want to know how big a pointer is we call
     * Pointer.SIZE.
     */
    public static final class CPP_class {//TODO:create enum instead.

        public static final class Pointer {

            /** A 32bit C++ pointer is 32bits wide, duh! */
            public static final transient int SIZE = 32;
        }

        public static final class Bool {

            /** A C++ bool is 8bits. */
            public static final transient int SIZE = Byte.SIZE;
        }

        public static final class Char {

            /** A C++ char is also 8bits wide. */
            public static final transient int SIZE = Byte.SIZE;
        }

        public static final class Enum {

            /** A C++ enum is as big as an int. */
            public static final transient int SIZE = Integer.SIZE;

            /** A C++ long is 4 bytes. */
            public static final transient int Long = Integer.SIZE;
        }

        public static final class Long {

            /** A C++ long is 4 bytes. */
            public static final transient int SIZE = Integer.SIZE;
        }
    }

    /**
     * you lazy ass bitch!!
     */
    public static final class TODO_Exception extends UnsupportedOperationException {

        public TODO_Exception() {
            printStackTrace();
            System.err.println(
                    "Woe to you, Oh Earth and Sea, for the Devil sends the\n"
                    + "beast with wrath, because he knows the time is short...\n"
                    + "Let him who hath understanding reckon the number of the beast,\n"
                    + "for it is a human number,\n"
                    + "its numbers, is");
            System.exit(666);
        }
    }
    
    public static final class Deprecation_Exception extends UnsupportedOperationException {

        public Deprecation_Exception() {
            printStackTrace();
            System.err.println(
                    "DARKNESS!!\n"
                    + "Imprisoning me\n"
                    + "All that I see\n"
                    + "Absolute horror\n"
                    + "I cannot live..."
                    + "I cannot die..."
                    + "body my holding cell!");
            System.exit(666);
        }
    }
}
