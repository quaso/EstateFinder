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

import sk.kvaso.estate.db.Estate;

@Component
public class ZoznamRealitCollectorImpl implements ICollector {
	private static final Logger log = Logger.getLogger(NehnutelnostiCollectorImpl.class.getName());

	@Override
	public String getName() {
		return "zoznamrealit.sk";
	}

	@Override
	public URL getURL(final int page) throws MalformedURLException {
		String str = "http://www.zoznamrealit.sk/reality";
		if (page > 1) {
			str += "/" + (page - 1) + "/";
		}
		str += "?q=okres-bratislava-ruzinov|druh-3-izbovy-byt|typ-predaj|cenado-140000|rozlohaod-70|balkon_lodzia_terasa-1|ibasfoto-1&ref=qs";
		return new URL(str);
	}

	@Override
	public Set<Estate> parse(final Document doc, final Date date) throws Exception {
		final Set<Estate> result = new HashSet<>();

		final Elements inzeratElements = doc.getElementsByClass("re");
		for (final Element inzerat : inzeratElements) {
			final Estate estate = new Estate();

			final Element elementA = inzerat.getElementsByTag("a").first();
			estate.setURL("http://www.zoznamrealit.sk" + elementA.attr("href"));
			final Element elementA2 = inzerat.getElementsByTag("h2").first().child(0);
			estate.setTITLE(elementA2.ownText());

			estate.setPRICE(getPrice(inzerat.getElementsByTag("strong").first().ownText()));
			if (estate.getPRICE() == null) {
				continue;
			}

			log.fine(estate.getTITLE());

			estate.setSHORT_TEXT(elementA2.attr("title"));

			estate.setTHUMBNAIL(inzerat.getElementsByTag("img").first().attr("src"));

			final Element content = inzerat.getElementsByTag("h3").last();
			final String[] str = content.html().split("br");
			for (final String s : str) {
				final String[] data = s.split("\\|");
				for (final String d : data) {
					if (d.contains("plocha")) {
						estate.setAREA(getArea(StringUtils.substringBetween(d, "<strong>", "</strong>")));
					} else if (d.contains("Ulica")) {
						estate.setSTREET(getStreet(StringUtils.substringBetween(d, "<strong>", "</strong>")));
					}
				}
			}
			estate.setTIMESTAMP(date);

			result.add(estate);
		}

		return result;
	}

	private String getStreet(final String str) {
		return str.replaceAll("&aacute;", "a");
	}

	private int getArea(final String str) {
		return Double.valueOf(str.substring(0, str.indexOf(" "))).intValue();
	}

	private String getPrice(final String str) {
		if (str.contains("m")) {
			return null;
		}
		try {
			final String result = str.substring(0, str.lastIndexOf(",")).replaceAll("\\.", "");
			if (StringUtils.isNumeric(result)) {
				return result;
			}
		} catch (final Exception ex) {
		}
		return null;
	}
}
