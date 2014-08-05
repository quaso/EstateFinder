package sk.kvaso.estate.model;

import java.io.Serializable;

@SuppressWarnings("serial")
public abstract class Data implements Serializable {

	private long ID;

	private boolean persisted;

	private boolean VISIBLE;

	public final long getID() {
		return this.ID;
	}

	public final void setID(final long iD) {
		this.ID = iD;
	}

	public final boolean isPersisted() {
		return this.persisted;
	}

	public final void setPersisted(final boolean persisted) {
		this.persisted = persisted;
	}

	public final boolean isVISIBLE() {
		return this.VISIBLE;
	}

	public final void setVISIBLE(final boolean vISIBLE) {
		this.VISIBLE = vISIBLE;
	}

}
