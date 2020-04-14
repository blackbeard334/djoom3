package neo.open;

/**
 * Handles int Array and number of Elements in int Array. 
 *
 * In a later step this can be replaced with a List or an IntBuffer.
 * 
 */
public class NeoIntArray {
	private int numValues;

	private int[] values;

	public NeoIntArray() {
		this.values = null;
		this.numValues = 0;
	}

	public int decNumIndexes() {
		return --numValues;
	}

	public int[] getIndexes() {
		return values;
	}

	public int getNumIndexes() {
		return numValues;
	}

	public int incNumIndexes() {
		return ++numValues;
	}

	public void setIndexes(int[] indexes) {
		this.values = indexes;
	}

	public void setNumIndexes(int numIndexes) {
		this.numValues = numIndexes;
	}
}