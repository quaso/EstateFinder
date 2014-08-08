package sk.kvaso.estate.db;


public class Picture extends Data {
	/**
	 *
	 */
	private static final long serialVersionUID = 1225302063768600279L;

	private String URL;

	public final String getURL() {
		return this.URL;
	}

	public final void setURL(final String uRL) {
		this.URL = uRL;
	}
}
