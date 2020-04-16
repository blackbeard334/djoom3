package neo.open;

import java.nio.IntBuffer;

/**
 * Handles int Array and number of Elements in int Array. 
 *
 * In a later step this can be replaced with java.util.List or java.nio.IntBuffer.
 * 
 */
public class NeoIntBuffer {
	private int numValues; // position pointer, remaining = values.length - numValues

	private IntBuffer values;

	public NeoIntBuffer() {
		this.values = null;
		this.numValues = 0;
	}

	public int decNumValues() {
		return --numValues;
	}

	public IntBuffer getValues() {
		return values;
	}

//	/**
//	 * 	
//	 * @return
//	 * 
//	 * @deprecated use IntBuffer instead int[]
//	 */
//	public int[] getValuesAsIntArray() {
//		int[] intArray = new int[getNumValues()];
//		for (int i = 0; i < intArray.length; i++) {
//			intArray[i] = getValues().get(i);
//		}
//		return intArray;
//	}

	public int getNumValues() {
		return numValues;
	}

	public int incNumValues() {
		return ++numValues;
	}

	public void createValues(int numElements) {
		this.values = Nio.newIntBuffer(numElements);
	}

	public void setValues(IntBuffer values) {
		this.values = Nio.newIntBuffer(values.capacity());
		for (int i = 0; i < values.capacity(); i++) {
			this.values.put(i, values.get(i));
		}
	}

	public int setNumValues(int numValues) {
		return this.numValues = numValues;
	}
}