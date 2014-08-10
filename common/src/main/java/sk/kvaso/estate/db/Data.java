package sk.kvaso.estate.db;

import java.io.Serializable;

import com.google.appengine.api.datastore.Entity;

@SuppressWarnings("serial")
public abstract class Data implements Serializable {

	private long ID;

	private boolean VISIBLE;

	private boolean persisted;

	private Entity google_entity;

	public final long getID() {
		return this.ID;
	}

	public final void setID(final long iD) {
		this.ID = iD;
	}

	public final boolean isVISIBLE() {
		return this.VISIBLE;
	}

	public final void setVISIBLE(final boolean vISIBLE) {
		this.VISIBLE = vISIBLE;
	}

	final boolean isPersisted() {
		return this.persisted;
	}

	final void setPersisted(final boolean persisted) {
		this.persisted = persisted;
	}

	final Entity getGoogle_entity() {
		return this.google_entity;
	}

	final void setGoogle_entity(final Entity google_entity) {
		this.google_entity = google_entity;
	}

}
