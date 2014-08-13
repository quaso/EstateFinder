package sk.kvaso.estate.db;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class AppState extends Data {

	/**
	 *
	 */
	private static final long serialVersionUID = 4273568745809586135L;

	public AppState() {
		this.setID(1L);
	}

	private Date lastView;
	private Date lastScan;
	private final Map<String, String> firstURLs = Collections.synchronizedMap(new HashMap<String, String>());

	public final Date getLastView() {
		return this.lastView;
	}

	public final void setLastView(final Date lastView) {
		this.lastView = lastView;
		this.setDirty(true);
	}

	public final Date getLastScan() {
		return this.lastScan;
	}

	public final void setLastScan(final Date lastScan) {
		this.lastScan = lastScan;
		this.setDirty(true);
	}

	public final String getFirstURL(final String collectorName) {
		return this.firstURLs.get(collectorName);
	}

	public final void setFirstURL(final String collectorName, final String url) {
		if (!url.equals(getFirstURL(collectorName))) {
			this.firstURLs.put(collectorName, url);
			this.setDirty(true);
		}
	}

	public void copyFrom(final AppState other) {
		if (other.getLastScan() != null) {
			if (this.lastScan == null || other.getLastScan().after(this.lastScan)) {
				this.lastScan = other.getLastScan();
			}
		}
		if (other.getLastView() != null) {
			if (this.lastView == null || other.getLastView().after(this.lastView)) {
				this.lastView = other.getLastView();
			}
		}
		for (final Entry<String, String> entry : other.firstURLs.entrySet()) {
			this.firstURLs.put(entry.getKey(), entry.getValue());
		}

		this.setGoogle_entity(other.getGoogle_entity());
		this.setID(other.getID());
		this.setPersisted(true);
		this.setDirty(false);
	}

	public void clear() {
		for (final String key : this.firstURLs.keySet()) {
			this.firstURLs.put(key, "");
		}
		this.setDirty(true);
	}
}
