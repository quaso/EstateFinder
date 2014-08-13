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
public class RealitySmeCollectorImpl extends AbstractCollector {
	private static final Logger log = Logger.getLogger(RealitySmeCollectorImpl.class.getName());

	@Override
	public String getName() {
		return "reality.sme.sk";
	}

	@Override
	public URL getURL(final int page) throws MalformedURLException {
		return new URL(
				"http://reality.sme.sk/predaj/3-izbovy-byt/bratislava-ruzinov/cena-od-10000-do-140000/rozloha-od-70/?order=od-najnovsich&page="
						+ page);
	}

	@Override
	public Set<Estate> parse(final Document doc, final Date date, final int page) throws Exception {
		if (doc.html().contains("sledky."))
		{
			return null;
		}

		final Set<Estate> result = new HashSet<>();

		final Elements inzeratElements = doc.getElementsByClass("article-list");
		for (final Element inzerat : inzeratElements) {
			final Estate estate = new Estate();

			final Element elementA = inzerat.getElementsByTag("a").first();
			estate.getURLs().add(setFirstUrl("http://reality.sme.sk" + elementA.attr("href"), page));
			estate.setTITLE(elementA.attr("title"));
			final Element status = elementA.getElementsByClass("estate-status").first();
			if (status != null) {
				if (status.ownText().contains("Predan") || status.ownText().contains("Rezervovan")) {
					continue;
				}
			}

			estate.setTHUMBNAIL(elementA.getElementsByTag("img").first().attr("src"));

			final String[] info = inzerat.getElementsByTag("small").first().ownText().split(",");
			try {
				estate.setSTREET(info[0].trim());
				estate.setAREA(getArea(info[1].trim()));
				estate.getNOTES().add(info[2].trim());
			} catch (final Exception ex) {
				estate.getNOTES().add(StringUtils.join(info, ","));
				if (estate.getAREA() == 0) {
					estate.setAREA(-1);
				}
				final Elements elements = inzerat.getElementsByClass("clearfix");
				for (final Element e : elements) {
					try {
						switch (e.child(0).ownText()) {
							case "Lokalita" :
								estate.setSTREET(e.child(1).attr("title"));
								break;
							case "Rozloha" :
								estate.setAREA(getArea(e.child(1).attr("title")));
								break;
						}
					} catch (final Exception ex2) {

					}
				}

			}

			estate.setPRICE(getPrice(inzerat.getElementsByClass("price-format").first().ownText()));
			estate.setTIMESTAMP(date);

			result.add(estate);
		}

		return result;
	}

	private int getArea(final String str) {
		return (int) Math.round(Double.valueOf(str.substring(0, str.indexOf(" "))));
	}

	private String getPrice(final String str) {
		return str.substring(0, str.lastIndexOf(" "));
	}

	@Override
	protected Logger getLogger() {
		return log;
	}
}
