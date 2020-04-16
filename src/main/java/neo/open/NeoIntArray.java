package neo.open;

/**
 * Handles int Array and number of Elements in int Array. 
 *
 * In a later step this can be replaced with java.util.List or java.nio.IntBuffer.
 * 
 */
public class NeoIntArray {
	private int numValues; // position pointer, remaining = values.length - numValues

	private int[] values;

	public NeoIntArray() {
		this.values = null;
		this.numValues = 0;
	}

	public int decNumValues() {
		return --numValues;
	}

	public int[] getValues() {
		return values;
	}

	public int getNumValues() {
		return numValues;
	}

	public int incNumValues() {
		return ++numValues;
	}

	public void setValues(int[] values) {
		this.values = values;
	}

	public int setNumValues(int numValues) {
		return this.numValues = numValues;
	}
}