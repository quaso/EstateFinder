package sk.kvaso.estate;

import java.util.LinkedList;
import java.util.Set;

import sk.kvaso.estate.db.Estate;

public class EstateStore extends LinkedList<Estate> {

	/**
	 *
	 */
	private static final long serialVersionUID = -4587159287329762583L;

	private Set<String> streets;

	public final Set<String> getStreets() {
		return this.streets;
	}

	public final void setStreets(final Set<String> streets) {
		this.streets = streets;
	}

}
