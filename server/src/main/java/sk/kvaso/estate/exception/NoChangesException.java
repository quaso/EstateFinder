package sk.kvaso.estate.exception;

public class NoChangesException extends Exception {

	/**
	 *
	 */
	private static final long serialVersionUID = -733484113572325230L;

	private final String collectorName;

	public NoChangesException(final String collectorName) {
		this.collectorName = collectorName;
	}

	public final String getCollectorName() {
		return this.collectorName;
	}
}
