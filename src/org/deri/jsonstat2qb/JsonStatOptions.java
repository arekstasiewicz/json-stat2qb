package org.deri.jsonstat2qb;

/**
 * Configuration options for describing a JSON-stat dataset.
 */
public class JsonStatOptions {

	private String encoding = null;

	/**
	 * Creates a new instance with default values.
	 */
	public JsonStatOptions() {}

	/**
	 * Creates a new instance and initializes it with values from another
	 * instance.
	 */
	public JsonStatOptions(JsonStatOptions defaults) {
		overrideWith(defaults);
	}

	/**
	 * Override values in this object with those from the other. Anything that
	 * is <code>null</code> in the other object will be ignored.
	 */
	public void overrideWith(JsonStatOptions other) {
		if (other.encoding != null) {
			this.encoding = other.encoding;
		}
	}

	/**
	 * Specify the JSON-stat file's character encoding. <code>null</code>
	 * signifies unknown encoding, that is, auto-detection. The default is
	 * <code>null</code>.
	 */
	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}

	/**
	 * Returns the JSON-stat file's character encoding, or <code>null</code> if
	 * unknown.
	 */
	public String getEncoding() {
		return encoding;
	}

}
