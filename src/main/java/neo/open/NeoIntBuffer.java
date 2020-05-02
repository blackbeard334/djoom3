package neo.open;

import java.nio.IntBuffer;

/**
 * Replacement to a combination of int[] and int for use of java.nio.IntBuffer (e.g. for openal & opengl)<br/> 
 * Handles int Array and number of Elements in int Array.<br/>
 *
 * In a later step this can be replaced with java.nio.IntBuffer.
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