package sk.kvaso.estate.db;

import java.io.Serializable;

import com.google.appengine.api.datastore.Entity;

@SuppressWarnings("serial")
public abstract class Data implements Serializable {

	private long ID;

	private boolean persisted = false;

	private boolean dirty = true;

	private Entity google_entity = null;

	String getDatabaseName() {
		return this.getClass().getSimpleName();
	}

	public final long getID() {
		return this.ID;
	}

	public final void setID(final long iD) {
		this.ID = iD;
	}

	public final boolean isPersisted() {
		return this.persisted;
	}

	final void setPersisted(final boolean persisted) {
		this.persisted = persisted;
	}

	public final boolean isDirty() {
		return this.dirty;
	}

	final void setDirty(final boolean dirty) {
		this.dirty = dirty;
	}

	final Entity getGoogle_entity() {
		return this.google_entity;
	}

	final void setGoogle_entity(final Entity google_entity) {
		this.google_entity = google_entity;
	}

}
