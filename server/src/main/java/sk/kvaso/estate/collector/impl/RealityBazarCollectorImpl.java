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
public class RealityBazarCollectorImpl implements ICollector {
	private static final Logger log = Logger.getLogger(RealityBazarCollectorImpl.class.getName());

	@Override
	public String getName() {
		return "reality.bazar.sk";
	}

	@Override
	public URL getURL(final int page) throws MalformedURLException {
		return new URL("http://reality.bazar.sk/?p[location]=t14&p[param1][from]=100000&p[param1][to]=140000&p[page]="
				+ page);
	}

	@Override
	public Set<Estate> parse(final Document doc, final Date date) throws Exception {
		final Set<Estate> result = new HashSet<>();

		final Elements inzeratElements = doc.getElementsByClass("span35");
		for (final Element inzerat : inzeratElements) {
			final Estate estate = new Estate();

			final Element elementHeader = inzerat.getElementsByTag("header").first();
			final Element elementA = elementHeader.getElementsByTag("a").first();
			estate.setURL(elementA.attr("href"));
			estate.setTITLE(elementA.ownText());

			log.fine(estate.getTITLE());

			estate.setTHUMBNAIL(inzerat.getElementsByClass("photo-img4").first().getElementsByTag("img").first()
					.attr("data-src"));

			estate.setPRICE(getPrice(elementHeader.getElementsByTag("strong").first().ownText()));

			estate.setSHORT_TEXT(inzerat.getElementsByClass("kratky").first().ownText());

			if (!(estate.getTITLE().contains("3-i") || estate.getTITLE().contains("3I")
					|| estate.getTITLE().contains("3i") ||
					estate.getSHORT_TEXT().contains("3-i") || estate.getSHORT_TEXT().contains("3I") || estate
					.getSHORT_TEXT().contains("3i"))) {
				continue;
			}

			estate.setAREA(getArea(estate.getSHORT_TEXT()));

			estate.setTIMESTAMP(date);

			result.add(estate);
		}

		return result;
	}

	private int getArea(final String str) {
		int max = -1;

		int i = 0;
		while ((i = str.indexOf("m2", i)) > -1) {
			int s = i - 2;
			while (StringUtils.isNumeric("" + str.charAt(s)) || str.charAt(s) == ',') {
				s--;
			}
			final String priceStr = str.substring(s + 1, i).trim().replaceAll(",", ".");
			try {
				final int price = (int) Math.round(Double.valueOf(priceStr));
				if (price > max) {
					max = price;
				}
			} catch (final NumberFormatException ex) {
				return -1;
			}
			i += 2;
		}
		return max;
	}

	private String getPrice(final String str) {
		return str.substring(0, str.lastIndexOf(" "));
	}
}
