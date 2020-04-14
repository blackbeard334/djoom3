package neo.open;

/**
 * Handles an Array and number of Elements of this Array.
 *
 * In a later step this can be replaced with a List or an subclass of
 * java.nio.Buffer (if applicable).
 * 
 */
public class NeoArray<T> {
	private int numValues;

	private T[] values;

	public NeoArray() {
		this.values = null;
		this.numValues = 0;
	}

	public int decNumIndexes() {
		return --numValues;
	}

	public T[] getIndexes() {
		return values;
	}

	public int getNumIndexes() {
		return numValues;
	}

	public int incNumIndexes() {
		return ++numValues;
	}

	public void setIndexes(T[] indexes) {
		this.values = indexes;
	}

	public void setNumIndexes(int numIndexes) {
		this.numValues = numIndexes;
	}
}