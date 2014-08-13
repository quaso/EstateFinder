package sk.kvaso.estate.collector.impl;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import sk.kvaso.estate.collector.AbstractCollector;
import sk.kvaso.estate.db.Estate;

@Component
public class TrhCollectorImpl extends AbstractCollector {
	private static final Logger log = Logger.getLogger(TrhCollectorImpl.class.getName());

	@Override
	public String getName() {
		return "trh.sk";
	}

	@Override
	public URL getURL(final int page) throws MalformedURLException {
		return new URL(
				"http://www.trh.sk/vyhladavanie.html?districts[0]=1&townships[0]=5&cities[0]=19&localities[0]=429&typeId=1&advertisingTypeId=1&order=1&priceTo=140000&priceType=3&areaFrom=70&categories[0]=7&categories[1]=8&floor=1&loggia=1&elevator=1&page="
						+ page);
	}

	@Override
	public Set<Estate> parse(final Document doc, final Date date, final int page) throws Exception {
		final Set<Estate> result = new HashSet<>();

		final Elements inzeratElements = doc.getElementsByClass("advert");
		for (final Element inzerat : inzeratElements) {
			final Estate estate = new Estate();

			final Element elementA = inzerat.getElementsByClass("description").first().getElementsByTag("a")
					.first();
			String url = elementA.attr("href");
			if (url.startsWith("/")) {
				url = "http://www.trh.sk" + url;
			}
			estate.getURLs().add(setFirstUrl(url, page));
			estate.setTITLE(elementA.ownText());

			estate.setTHUMBNAIL(inzerat.getElementsByClass("image").first().getElementsByTag("img").first().attr("src"));

			estate.setPRICE(inzerat.getElementsByClass("priceWrap").first().getElementsByClass("number").first()
					.ownText());

			final Element attributes = inzerat.getElementsByClass("attributes").first();

			for (int i = 0; i < attributes.children().size(); i++) {
				final Element child = attributes.child(i);
				switch (child.ownText()) {
					case "Plocha:" :
						estate.setAREA(getArea(attributes.child(i + 1).ownText()));
						break;
					case "Poschodie:" :
						estate.getNOTES().add("Poschodie: " + attributes.child(i + 1).ownText());
						break;
					case "Lokalita:" :
						estate.setSTREET(getStreet(attributes.child(i + 1).ownText()));
						break;
				}
			}
			estate.setTIMESTAMP(date);

			result.add(estate);
		}

		return result;
	}

	private String getStreet(final String str) {
		String result = "";

		final int i = str.lastIndexOf(",");
		if (i > 0) {
			result = str.substring(i).replaceAll("(Ružinov)", "").replaceAll("Ružinov", "").replaceAll(",", "").trim();
			if ("A. Mraza".equals(result) || "A. Mráza".equals(result) || "A.Mraza".equals(result)
					|| "A.Mráza".equals(result)) {
				result = "Andreja Mráza";
			}
			result = StringUtils.substringBefore(result, "(").trim();
		}
		return result;
	}

	private int getArea(final String str) {
		try {
			return (int) Math.round(Double.valueOf(str.substring(0, str.indexOf(" "))));
		} catch (final NumberFormatException ex) {
			return -1;
		}
	}

	@Override
	protected Logger getLogger() {
		return log;
	}
}
