package sk.kvaso.estate.db;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public class Estate extends Data {
	/**
	 *
	 */
	private static final long serialVersionUID = -6798875380525236717L;

	private boolean VISIBLE;

	private String STREET;

	private int AREA;

	private String PRICE;

	private String TITLE;

	private String SHORT_TEXT;

	private final Set<String> NOTES = new HashSet<>();

	private String TEXT;

	private final Set<String> URLs = new HashSet<>();

	private String THUMBNAIL;

	// @OneToMany(orphanRemoval = true)
	// @JoinColumn(name = "ESTATE_ID", referencedColumnName = "ID")
	private final Set<Picture> PICTURES = new HashSet<>();

	private Date TIMESTAMP;

	public final boolean isVISIBLE() {
		return this.VISIBLE;
	}

	public final void setVISIBLE(final boolean vISIBLE) {
		this.VISIBLE = vISIBLE;
		this.setDirty(true);
	}

	public final String getSTREET() {
		return this.STREET;
	}

	public final void setSTREET(final String sTREET) {
		this.STREET = sTREET;
	}

	public final int getAREA() {
		return this.AREA;
	}

	public final void setAREA(final int aREA) {
		this.AREA = aREA;
	}

	public final String getPRICE() {
		return this.PRICE;
	}

	public final void setPRICE(final String pRICE) {
		this.PRICE = pRICE;
	}

	public final String getTITLE() {
		return this.TITLE;
	}

	public final void setTITLE(final String tITLE) {
		this.TITLE = tITLE;
	}

	public final String getSHORT_TEXT() {
		return this.SHORT_TEXT;
	}

	public final void setSHORT_TEXT(final String sHORT_TEXT) {
		this.SHORT_TEXT = sHORT_TEXT;
		if (this.SHORT_TEXT != null && this.SHORT_TEXT.length() >= 500) {
			this.SHORT_TEXT = this.SHORT_TEXT.substring(0, 500);
		}
	}

	public final Set<String> getNOTES() {
		return this.NOTES;
	}

	public final String getTEXT() {
		return this.TEXT;
	}

	public final void setTEXT(final String tEXT) {
		this.TEXT = tEXT;
	}

	public final Set<String> getURLs() {
		return this.URLs;
	}

	public final String getTHUMBNAIL() {
		return this.THUMBNAIL;
	}

	public final void setTHUMBNAIL(final String tHUMBNAIL) {
		this.THUMBNAIL = tHUMBNAIL;
	}

	public final Set<Picture> getPICTURES() {
		return this.PICTURES;
	}

	public final Date getTIMESTAMP() {
		return this.TIMESTAMP;
	}

	public final void setTIMESTAMP(final Date tIMESTAMP) {
		this.TIMESTAMP = tIMESTAMP;
	}
}
