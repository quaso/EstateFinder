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
public class NehnutelnostiCollectorImpl extends AbstractCollector {
	private static final Logger log = Logger.getLogger(ZoznamRealitCollectorImpl.class.getName());

	@Override
	public String getName() {
		return "nehnutelnosti.sk";
	}

	@Override
	public URL getURL(final int page) throws MalformedURLException {
		String str = "http://www.nehnutelnosti.sk/bratislava-ii-ruzinov/3-izbove-byty/predaj?p[param1][to]=135000&p[param11][from]=70&p[foto]=1";
		if (page > 1) {
			str += "&p[page]=" + page;
		}
		return new URL(str);
	}

	@Override
	public Set<Estate> parse(final Document doc, final Date date, final int page) throws Exception {
		final Set<Estate> result = new HashSet<>();

		final Elements inzeratElements = doc.getElementsByClass("inzerat");
		for (final Element inzerat : inzeratElements) {
			final Estate estate = new Estate();

			final Element elementA = inzerat.getElementsByClass("advertisement-head").first().getElementsByTag("a")
					.first();
			estate.getURLs().add(setFirstUrl(elementA.attr("href"), page));
			estate.setTITLE(elementA.ownText());

			estate.setTHUMBNAIL(inzerat.getElementsByClass("advertPhoto").first().getElementsByTag("img").first()
					.attr("data-src"));

			final Element content = inzerat.getElementsByClass("inzerat-content").first();
			estate.setSTREET(getStreet(content.getElementsByClass("locationText").first().ownText()));
			estate.setAREA(getArea(content.getElementsByClass("estate-area").first().getElementsByTag("span").first()
					.ownText()));
			estate.getNOTES().add(content.getElementsByClass("advertisement-condition").first().ownText());
			estate.setSHORT_TEXT(content.getElementsByClass("advertisement-content-p").first().ownText());

			estate.setPRICE(getPrice(inzerat.getElementsByClass("cena").first().getElementsByTag("span").first()
					.ownText()));
			estate.setTIMESTAMP(date);

			result.add(estate);
		}
		return result;
	}

	private String getStreet(final String str) {
		String result = "";
		final int i = str.indexOf(",");
		if (i > 0) {
			result = str.substring(0, i).replaceAll("\\(Ružinov\\)", "").replaceAll("Ružinov\\b", "").trim();
			result = StringUtils.substringBefore(result, "(").trim();
		}
		return result;
	}

	private int getArea(final String str) {
		return Integer.valueOf(str.substring(0, str.indexOf(" ")));
	}

	private String getPrice(final String str) {
		return str.substring(0, str.lastIndexOf(" "));
	}

	@Override
	protected Logger getLogger() {
		return log;
	}
}
