package sk.kvaso.estate.collector;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.Set;

import org.jsoup.nodes.Document;

import sk.kvaso.estate.db.Estate;

public interface ICollector {

	public String getName();

	public void init();

	public URL getURL(final int page) throws MalformedURLException;

	public Set<Estate> parse(final Document doc, final Date date, final int page) throws Exception;

}