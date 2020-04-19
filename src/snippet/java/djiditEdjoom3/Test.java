package djiditEdjoom3;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import neo.open.Nio;

public class Test {

	public static void main(String[] args) {
		//testPutGet();
		testCopy();
	}

	public static void testCopy() {
		FloatBuffer fb1 = ByteBuffer.allocate(16*Nio.SIZEOF_FLOAT).asFloatBuffer();
		System.out.println("allocate");
		put(fb1, 1);
		get(fb1);
		System.out.println("copy");
		FloatBuffer fb2 = copy(fb1);
		get(fb2);
		System.out.println("compare");
		compare(fb1, fb2);
		System.out.println("duplicate");
		fb2 = fb1.duplicate().flip();
		get(fb2);
		System.out.println("compare");
		compare(fb1, fb2);
	}

	public static FloatBuffer copy(FloatBuffer fb1) {
		FloatBuffer fb2 = ByteBuffer.allocate(fb1.capacity()*Nio.SIZEOF_FLOAT).asFloatBuffer();
		fb2.put(fb1).flip();
		return fb2;
	}

	public static void compare(FloatBuffer fb1, FloatBuffer fb2) {
		for (int i = 0; i < fb1.capacity(); i++) {
			System.out.println(""+i+" "+fb1.get(i)+" "+fb2.get(i)+" "+(fb1.get(i) == fb2.get(i)));
		}
	}

	public static void testPutGet() {
		int i = 1;
		FloatBuffer fb = ByteBuffer.allocate(16*Nio.SIZEOF_FLOAT).asFloatBuffer();
		System.out.println("allocate");
//		put(fb, i++);
//		get(fb);
//		fb.position(16);
//		fb.flip();
//		System.out.println("flip");
//		get(fb);
		put(fb, i++);
		get(fb);
		fb.rewind();
		System.out.println("rewind");
		get(fb);
		put(fb, i++);
		get(fb);
		fb.clear();
		System.out.println("clear");
		get(fb);
		put(fb, i++);
		get(fb);
	}

	public static void put(FloatBuffer fb, int a) {
		print(fb);
		for (int i = 0; i < fb.capacity(); i++) {
			fb.put(i, (float) (i+1) * a * 1.0f);
			print(fb);
		}
	}

	public static void get(FloatBuffer fb) {
		print(fb);
		for (int i = 0; i < fb.capacity(); i++) {
			System.out.println(""+i+" "+fb.get(i));
			print(fb);
		}
	}

	public static void print(FloatBuffer fb) {
		System.out.println(
				"mark <= position " + fb.position() + " <= limit " + fb.limit() + " <= capacity" + fb.capacity());

	}

}
