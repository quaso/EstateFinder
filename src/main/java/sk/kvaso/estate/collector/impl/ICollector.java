package sk.kvaso.estate.collector.impl;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Set;

import org.jsoup.nodes.Document;

import sk.kvaso.estate.model.Estate;

public interface ICollector {

	public URL getURL(final int page) throws MalformedURLException;

	public Set<Estate> parse(final Document doc) throws Exception;

}