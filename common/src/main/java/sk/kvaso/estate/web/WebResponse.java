package sk.kvaso.estate.web;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Set;

import sk.kvaso.estate.db.Estate;

public class WebResponse implements Serializable {
	/**
	 *
	 */
	private static final long serialVersionUID = 2341457033164929931L;

	private List<Estate> estates;
	private Date lastUpdate;
	private Set<String> streets;

	public final List<Estate> getEstates() {
		return this.estates;
	}

	public final void setEstates(final List<Estate> estates) {
		this.estates = estates;
	}

	public final Date getLastUpdate() {
		return this.lastUpdate;
	}

	public final void setLastUpdate(final Date lastUpdate) {
		this.lastUpdate = lastUpdate;
	}

	public final Set<String> getStreets() {
		return this.streets;
	}

	public final void setStreets(final Set<String> streets) {
		this.streets = streets;
	}
}
