package sk.kvaso.estate.collector;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;

import sk.kvaso.estate.EstateStore;
import sk.kvaso.estate.collector.impl.ICollector;
import sk.kvaso.estate.db.DatabaseUtils;
import sk.kvaso.estate.db.Estate;

import com.google.appengine.api.ThreadManager;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions;

public class DataCollector implements InitializingBean {
	private static final Logger log = Logger.getLogger(DataCollector.class.getName());

	private static final Pattern DIACRITIC_PATTERN = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

	private final Set<ICollector> collectors;

	@Autowired
	private EstateStore store;

	@Autowired
	private DatabaseUtils databaseUtils;

	private boolean paused = false;

	@Autowired
	public DataCollector(final Set<ICollector> collectors) {
		this.collectors = collectors;
	}

	private Date lastScan;

	public synchronized void collect(final boolean force) throws Throwable {
		if (this.paused && !force) {
			log.info("Collectting is paused");
			return;
		}

		if (this.store.isEmpty()) {
			log.info("Loading data from database");
			//			this.databaseUtils.load();
		} else {
			log.info("Found in-memory data: " + this.store.size());
		}

		final boolean wasEmpty = this.store.isEmpty();
		if (wasEmpty) {
			log.info("Store is empty");
		}

		this.lastScan = new Date();

		final List<Estate> detectedEstates = Collections.synchronizedList(new ArrayList<Estate>());
		ThreadFactory threadFactory;
		try {
			threadFactory = ThreadManager.currentRequestThreadFactory();
		} catch (final NullPointerException ex) {
			threadFactory = new ThreadFactory() {

				@Override
				public Thread newThread(final Runnable r) {
					return new Thread(r);
				}
			};
		}
		final List<Thread> threads = new ArrayList<>();

		for (final ICollector collector : this.collectors) {
			log.info("Using [" + collector.getName() + "]");
			final Thread thread = threadFactory.newThread(new CollectRunnable(collector, wasEmpty, detectedEstates));
			threads.add(thread);
			thread.start();
		}

		boolean finished;
		do {
			log.info("Waiting for results");
			Thread.sleep(5000);
			finished = true;
			for (final Thread thread : threads) {
				if (thread.isAlive()) {
					finished = false;
					break;
				}
			}
		} while (!finished);

		log.info("Cleaning results");

		this.store.setStreets(getStreets(this.store, detectedEstates));

		// guess street name out of title
		for (final Estate e : detectedEstates) {
			if (StringUtils.isEmpty(e.getSTREET()) || e.getSTREET().contains("okolie")
					|| e.getSTREET().contains("Bratislava")) {
				final String title = deAccent(fixStreetName(e.getTITLE())).toLowerCase();
				for (final String street : this.store.getStreets()) {
					final String s = deAccent(street).toLowerCase();
					if (title.contains(s)) {
						e.setSTREET(street);
						break;
					}
				}
			}
		}

		try {
			// replace street names with diacritics
			for (final Estate e : detectedEstates) {
				final String str = deAccent(e.getSTREET());
				for (final String street : this.store.getStreets()) {
					if (str.equals(deAccent(street))) {
						e.setSTREET(street);
						break;
					}
				}
			}
		} catch (final Throwable t) {
			t.printStackTrace();
		}

		this.store.setStreets(getStreets(this.store, detectedEstates));

		log.info("Detected " + detectedEstates.size() + " estates");

		final Map<String, String> newEstatesCount = mergeEstates(this.store, detectedEstates, true);

		log.info("Total " + this.store.size() + " estates");

		//		this.databaseUtils.save();

		if (!wasEmpty && newEstatesCount.size() > 0) {
			sendMail(newEstatesCount);
		}

		log.info("done");
	}

	private Map<String, String> mergeEstates(final Collection<Estate> allEstates, final Collection<Estate> newEstates,
			final boolean setId) {
		final Map<String, String> result = new HashMap<>();

		for (final Estate newEstate : newEstates) {
			boolean isSame = false;
			for (final Estate estate : allEstates) {
				if (isTheSame(estate, newEstate)) {
					isSame = true;
					if (estate.getTHUMBNAIL() == null) {
						estate.setTHUMBNAIL(newEstate.getTHUMBNAIL());
					}
					break;
				}
			}
			if (!isSame) {
				log.info("New found: " + newEstate.getURL());
				result.put(newEstate.getTITLE(), newEstate.getURL());
				if (setId) {
					newEstate.setID(allEstates.size() + 1);
				}
				newEstate.setVISIBLE(true);
				allEstates.add(newEstate);
			}
		}

		return result;
	}

	public boolean isTheSame(final Estate e1, final Estate e2) {
		if (e1.getURL().equals(e2.getURL())) {
			return true;
		}
		if (e1.getAREA() == e2.getAREA()) {
			if (!StringUtils.isEmpty(e1.getSTREET()) && !StringUtils.isEmpty(e2.getSTREET())) {
				if (e1.getSTREET().equalsIgnoreCase(e2.getSTREET())) {
					return true;
				}
			} else if (!StringUtils.isEmpty(e1.getTEXT()) && !StringUtils.isEmpty(e2.getTEXT())) {
				if (StringUtils.getJaroWinklerDistance(e1.getTEXT(), e2.getTEXT()) > 0.8) {
					return true;
				}
			}
		}
		return false;
	}

	@SafeVarargs
	private final Set<String> getStreets(final Collection<Estate>... estates) {
		final Set<String> result = new TreeSet<>();
		for (final Collection<Estate> c : estates) {
			for (final Estate e : c) {
				if (!StringUtils.isEmpty(e.getSTREET())) {
					if ("000".equals(e.getSTREET())) {
						e.setSTREET(null);
					} else {
						final String street = fixStreetName(e.getSTREET());

						final String s2 = deAccent(street);
						boolean add = true;

						for (final String s : result) {
							final String s_ = deAccent(s);
							if (s2.equals(s_)) {
								if (StringUtils.getJaroWinklerDistance(s2, street) < StringUtils
										.getJaroWinklerDistance(s2, s)) {
									result.remove(s);
								} else {
									add = false;
								}
								break;
							}
						}

						if (add) {
							result.add(street);
						}
					}
				}
			}
		}

		return result;
	}

	private String fixStreetName(final String str) {
		return str.replaceAll("A.MRAZA", "Andreja Mráza")
				.replaceAll("A. MRAZA", "Andreja Mráza").replaceAll("A.MRÁZA", "Andreja Mráza")
				.replaceAll("A. MRÁZA", "Andreja Mráza").replaceAll("A.Mraza", "Andreja Mráza")
				.replaceAll("A. Mraza", "Andreja Mráza").replaceAll("A.Mráza", "Andreja Mráza")
				.replaceAll("A. Mráza", "Andreja Mráza");
	}

	private String deAccent(final String str) {
		if (str == null) {
			return "";
		}
		final String nfdNormalizedString = Normalizer.normalize(str, Normalizer.Form.NFD);
		return DIACRITIC_PATTERN.matcher(nfdNormalizedString).replaceAll("");
	}

	private void sendMail(final Map<String, String> newEstates) {
		final Properties props = new Properties();
		final Session session = Session.getDefaultInstance(props, null);

		final Message msg = new MimeMessage(session);
		try {
			msg.setFrom(new InternetAddress("martinkvasnicka@gmail.com",
					"Quaso Estate Finder Admin"));
			msg.addRecipient(Message.RecipientType.TO,
					new InternetAddress("martinkvasnicka@gmail.com",
							"Martin Kvasnicka"));
			msg.setSubject("New estates found: " + newEstates.size());
			final StringBuffer buff = new StringBuffer();
			for (final Entry<String, String> e : newEstates.entrySet()) {
				buff.append(e.getKey());
				buff.append("\n");
				buff.append(e.getValue());
				buff.append("\n");
				buff.append("-------------------------\n\n");
			}
			buff.append("http://quasoestatefinder.appspot.com/");

			msg.setText(buff.toString());
			Transport.send(msg);
		} catch (final Exception ex) {
			log.warning("Cannot send mail " + ex.getMessage());
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		try {
			QueueFactory.getDefaultQueue().add(
					TaskOptions.Builder.withUrl("/rest/collectCron").countdownMillis(TimeUnit.MINUTES.toMillis(1)));
		} catch (final NullPointerException ex) {

		}
	}

	public final Date getLastScan() {
		return this.lastScan;
	}

	public final boolean isPaused() {
		return this.paused;
	}

	public final void setPaused(final boolean paused) {
		this.paused = paused;
	}

	private class CollectRunnable implements Runnable {

		private final ICollector collector;
		private final boolean wasEmpty;
		private final List<Estate> detectedEstates;

		public CollectRunnable(final ICollector collector, final boolean wasEmpty, final List<Estate> detectedEstates) {
			this.collector = collector;
			this.wasEmpty = wasEmpty;
			this.detectedEstates = detectedEstates;
		}

		@Override
		public void run() {
			try {
				for (int page = 1;; page++) {
					if (!this.wasEmpty && page > 2) {
						break;
					}
					log.info("---- page " + page + " ----");
					final Document doc = Jsoup.parse(this.collector.getURL(page), 10000);
					final Set<Estate> estatesInPage = this.collector.parse(doc, DataCollector.this.lastScan);
					if (CollectionUtils.isEmpty(estatesInPage)) {
						break;
					}
					mergeEstates(this.detectedEstates, estatesInPage, false);
				}
			} catch (final Exception e) {
				e.printStackTrace();
			}
		}
	}
}
