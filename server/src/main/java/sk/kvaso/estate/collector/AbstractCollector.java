package sk.kvaso.estate.collector;

import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;

import sk.kvaso.estate.db.AppState;
import sk.kvaso.estate.exception.NoChangesException;

public abstract class AbstractCollector implements ICollector {

	@Autowired
	private AppState appState;

	private String firstUrl;

	@Override
	public void init() {
		this.firstUrl = null;
	}

	protected final String setFirstUrl(final String url, final int page) throws NoChangesException {
		if (page == 1 && this.firstUrl == null) {
			this.firstUrl = url;
			if (this.firstUrl.equals(this.getFirstUrl())) {
				throw new NoChangesException(getName());
			}
			this.appState.setFirstURL(getName(), url);
			getLogger().info("Last url " + url);
		}
		return url;
	}

	private String getFirstUrl() {
		return this.appState.getFirstURL(getName());
	}

	protected abstract Logger getLogger();
}
