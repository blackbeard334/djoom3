package neo.open;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public class IntByteBuffer {

	private ByteBuffer byteBuffer;
	private IntBuffer intBuffer;

	public IntByteBuffer() {
		super();
	}

	public void clear() {
		this.byteBuffer = null;
		this.intBuffer = null;
	}

	public void createBuffer(ByteBuffer bb) {
		this.byteBuffer = bb.duplicate();
		this.intBuffer = byteBuffer.asIntBuffer();
	}

	public void createBuffer(int length) {
		this.byteBuffer = Nio.newByteBuffer(length * 4);
		this.intBuffer = byteBuffer.asIntBuffer();
	}

	@Deprecated
	public void createBuffer(int[] indexes) {
		int length = indexes == null ? 0 : indexes.length;
		createBuffer(length);
		if (length > 0) {
			intBuffer.put(indexes);
		}
	}

	@Deprecated
	public int[] getAsIntArray() {
		if (intBuffer.hasArray()) {
			return intBuffer.array();
		} else {
			return new int[intBuffer.capacity()];
		}

	}

	public ByteBuffer getByteBuffer() {
		return byteBuffer;
	}

	public IntBuffer getIntBuffer() {
		return intBuffer;
	}

	@Deprecated
	public void putFromIntArray(int[] in, int length) {
		for (int i = 0; i < length; i++) {
			intBuffer.put(i, in[i]);
		}
	}

	@Deprecated
	public void putFromIntArray(int[] in, int inIndex, int index, int length) {
		for (int i = 0; i < length; i++) {
			intBuffer.put(index + i, in[inIndex + i]);
		}
	}

	public void putFromIntBuffer(IntBuffer in, int length) {
		for (int i = 0; i < length; i++) {
			intBuffer.put(i, in.get(i));
		}
	}

	/**
     * Copies an array from the specified source array, beginning at the
     * specified position, to the specified position of the destination array.
     * A subsequence of array components are copied from the source
     * array referenced by {@code src} to the destination array
     * referenced by {@code dest}. The number of components copied is
     * equal to the {@code length} argument. The components at
     * positions {@code srcPos} through
     * {@code srcPos+length-1} in the source array are copied into
     * positions {@code destPos} through
     * {@code destPos+length-1}, respectively, of the destination
     * array.
     * <p>
     * If the {@code src} and {@code dest} arguments refer to the
     * same array object, then the copying is performed as if the
     * components at positions {@code srcPos} through
     * {@code srcPos+length-1} were first copied to a temporary
     * array with {@code length} components and then the contents of
     * the temporary array were copied into positions
     * {@code destPos} through {@code destPos+length-1} of the
     * destination array.
     * <p>
     * If {@code dest} is {@code null}, then a
     * {@code NullPointerException} is thrown.
     * <p>
     * If {@code src} is {@code null}, then a
     * {@code NullPointerException} is thrown and the destination
     * array is not modified.
     * <p>
     * Otherwise, if any of the following is true, an
     * {@code ArrayStoreException} is thrown and the destination is
     * not modified:
     * <ul>
     * <li>The {@code src} argument refers to an object that is not an
     *     array.
     * <li>The {@code dest} argument refers to an object that is not an
     *     array.
     * <li>The {@code src} argument and {@code dest} argument refer
     *     to arrays whose component types are different primitive types.
     * <li>The {@code src} argument refers to an array with a primitive
     *    component type and the {@code dest} argument refers to an array
     *     with a reference component type.
     * <li>The {@code src} argument refers to an array with a reference
     *    component type and the {@code dest} argument refers to an array
     *     with a primitive component type.
     * </ul>
     * <p>
     * Otherwise, if any of the following is true, an
     * {@code IndexOutOfBoundsException} is
     * thrown and the destination is not modified:
     * <ul>
     * <li>The {@code srcPos} argument is negative.
     * <li>The {@code destPos} argument is negative.
     * <li>The {@code length} argument is negative.
     * <li>{@code srcPos+length} is greater than
     *     {@code src.length}, the length of the source array.
     * <li>{@code destPos+length} is greater than
     *     {@code dest.length}, the length of the destination array.
     * </ul>
     * <p>
     * Otherwise, if any actual component of the source array from
     * position {@code srcPos} through
     * {@code srcPos+length-1} cannot be converted to the component
     * type of the destination array by assignment conversion, an
     * {@code ArrayStoreException} is thrown. In this case, let
     * <b><i>k</i></b> be the smallest nonnegative integer less than
     * length such that {@code src[srcPos+}<i>k</i>{@code ]}
     * cannot be converted to the component type of the destination
     * array; when the exception is thrown, source array components from
     * positions {@code srcPos} through
     * {@code srcPos+}<i>k</i>{@code -1}
     * will already have been copied to destination array positions
     * {@code destPos} through
     * {@code destPos+}<i>k</I>{@code -1} and no other
     * positions of the destination array will have been modified.
     * (Because of the restrictions already itemized, this
     * paragraph effectively applies only to the situation where both
     * arrays have component types that are reference types.)
	 * 
	 * @param in
	 * @param inIndex
	 * @param index
	 * @param length
     * @throws     IndexOutOfBoundsException  if copying would cause
     *             access of data outside array bounds.
     * @throws     ArrayStoreException  if an element in the {@code src}
     *             array could not be stored into the {@code dest} array
     *             because of a type mismatch.
     * @throws     NullPointerException if either {@code src} or
     *             {@code dest} is {@code null}.
	 */
	public void putFromIntBuffer(IntBuffer src, int srcPos, int destPos, int length) {
		/*
Copies an array from the specified source array, beginning at the specified position, to the specified position of the destination array. A subsequence of array components are copied from the source array referenced by src to the destination array referenced by dest. The number of components copied is equal to the length argument. The components at positions srcPos through srcPos+length-1 in the source array are copied into positions destPos through destPos+length-1, respectively, of the destination array. 
If the src and dest arguments refer to the same array object, then the copying is performed as if the components at positions srcPos through srcPos+length-1 were first copied to a temporary array with length components and then the contents of the temporary array were copied into positions destPos through destPos+length-1 of the destination array. 

If dest is null, then a NullPointerException is thrown. 

If src is null, then a NullPointerException is thrown and the destination array is not modified. 

Otherwise, if any of the following is true, an ArrayStoreException is thrown and the destination is not modified: 

The src argument refers to an object that is not an array. 
The dest argument refers to an object that is not an array. 
The src argument and dest argument refer to arrays whose component types are different primitive types. 
The src argument refers to an array with a primitive component type and the dest argument refers to an array with a reference component type. 
The src argument refers to an array with a reference component type and the dest argument refers to an array with a primitive component type. 
Otherwise, if any of the following is true, an IndexOutOfBoundsException is thrown and the destination is not modified: 

The srcPos argument is negative. 
The destPos argument is negative. 
The length argument is negative. 
srcPos+length is greater than src.length, the length of the source array. 
destPos+length is greater than dest.length, the length of the destination array. 
Otherwise, if any actual component of the source array from position srcPos through srcPos+length-1 cannot be converted to the component type of the destination array by assignment conversion, an ArrayStoreException is thrown. In this case, let k be the smallest nonnegative integer less than length such that src[srcPos+k] cannot be converted to the component type of the destination array; when the exception is thrown, source array components from positions srcPos through srcPos+k-1 will already have been copied to destination array positions destPos through destPos+k-1 and no other positions of the destination array will have been modified. (Because of the restrictions already itemized, this paragraph effectively applies only to the situation where both arrays have component types that are reference types.)

Parameters:
src the source array.
srcPos starting position in the source array.
dest the destination array.
destPos starting position in the destination data.
length the number of array elements to be copied.
Throws:
IndexOutOfBoundsException - if copying would cause access of data outside array bounds.
ArrayStoreException - if an element in the src array could not be stored into the dest array because of a type mismatch.
NullPointerException - if either src or dest is null.
		 */
		for (int i = 0; i < length; i++) {
			intBuffer.put(destPos + i, src.get(srcPos + i));
		}
	}
}
