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
import java.util.concurrent.CountDownLatch;
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
import sk.kvaso.estate.db.AppState;
import sk.kvaso.estate.db.DatabaseUtils;
import sk.kvaso.estate.db.Estate;
import sk.kvaso.estate.exception.NoChangesException;

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

	@Autowired
	private AppState appState;

	private boolean paused = false;

	/**
	 * 1 = first collect, 2 = second collect, 3 = any other collect
	 */
	private int collectingState = 0;

	@Autowired
	public DataCollector(final Set<ICollector> collectors) {
		this.collectors = collectors;
	}

	private boolean collecting = false;

	public void collect(final boolean force) throws Throwable {
		if (this.paused && !force) {
			log.info("Collectting is paused");
			return;
		}

		if (this.collecting) {
			while (this.collecting) {
				Thread.sleep(1000);
			}
			return;
		}

		this.collecting = true;
		if (this.collectingState < 2) {
			this.collectingState++;
		}

		this.databaseUtils.loadAppState(this.appState);

		try {
			if (this.store.isEmpty()) {
				log.info("Loading data from database");
				this.databaseUtils.loadEstates(this.store);
			} else {
				log.info("Found in-memory data: " + this.store.size());
			}

			final boolean wasEmpty = this.store.isEmpty();
			if (wasEmpty) {
				log.info("Store is empty");
			}

			this.appState.setLastScan(new Date());

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
			final CountDownLatch latch = new CountDownLatch(this.collectors.size());

			for (final ICollector collector : this.collectors) {
				log.info("Using [" + collector.getName() + "]");
				final Thread thread = threadFactory.newThread(new CollectRunnable(collector, detectedEstates, latch));
				thread.start();
			}

			latch.await();
			log.info("Cleaning results");

			this.store.setStreets(getStreets(this.store, detectedEstates));

			// guess street name out of title or notes
			for (final Estate e : detectedEstates) {
				if (StringUtils.isEmpty(e.getSTREET()) || e.getSTREET().contains("okolie")
						|| e.getSTREET().contains("Bratislava")) {
					final String title = deAccent(fixStreetName(e.getTITLE())).toLowerCase();
					for (final String street : this.store.getStreets()) {
						String s = deAccent(street).toLowerCase();
						if (e.getSTREET() != null && e.getSTREET().contains("Bratislava")) {
							s = StringUtils.left(s, s.length() - 1);
						}
						if (title.contains(s)) {
							e.setSTREET(street);
							break;
						} else {
							if (!e.getNOTES().isEmpty()) {
								for (final String note : e.getNOTES()) {
									if (deAccent(note).toLowerCase().contains(s)) {
										e.setSTREET(street);
										break;
									}
								}
							} else {
								if (!StringUtils.isEmpty(e.getSHORT_TEXT())
										&& deAccent(e.getSHORT_TEXT()).toLowerCase().contains(s)) {
									e.setSTREET(street);
									break;
								}
							}
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

			final Map<String, Estate> newEstatesCount = mergeEstates(this.store, detectedEstates, true);

			log.info("Total " + this.store.size() + " estates");

			this.databaseUtils.saveEstates(this.store);
			this.databaseUtils.saveAppState(this.appState);

			if (!wasEmpty && newEstatesCount.size() > 0 && this.collectingState == 2) {
				sendMail(newEstatesCount);
			}

			log.info("done");
		} finally {
			this.collecting = false;
		}
	}

	private synchronized Map<String, Estate> mergeEstates(final Collection<Estate> allEstates,
			final Collection<Estate> newEstates, final boolean setId) {
		final Map<String, Estate> result = new HashMap<>();

		for (final Estate newEstate : newEstates) {
			if (newEstate.getAREA() <= 0) {
				continue;
			}
			boolean isSame = false;
			for (final Estate estate : allEstates) {
				if (isTheSame(estate, newEstate)) {
					isSame = true;
					if (estate.getTHUMBNAIL() == null) {
						estate.setTHUMBNAIL(newEstate.getTHUMBNAIL());
					}
					estate.getURLs().addAll(newEstate.getURLs());
					break;
				}
			}
			if (!isSame) {
				//				log.info("New found: " + newEstate.getTITLE());
				result.put(newEstate.getTITLE(), newEstate);
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
		if (CollectionUtils.containsAny(e1.getURLs(), e2.getURLs())) {
			return true;
		}

		if (e1.getAREA() == e2.getAREA()) {
			if (!StringUtils.isEmpty(e1.getSTREET()) && !StringUtils.isEmpty(e2.getSTREET())) {
				if (e1.getSTREET().equalsIgnoreCase(e2.getSTREET())) {
					return true;
				}
			} else if (!StringUtils.isEmpty(e1.getSHORT_TEXT()) && !StringUtils.isEmpty(e2.getSHORT_TEXT())) {
				if (StringUtils.getJaroWinklerDistance(e1.getSHORT_TEXT(), e2.getSHORT_TEXT()) > 0.8) {
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

	private void sendMail(final Map<String, Estate> newEstates) {
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
			for (final Entry<String, Estate> e : newEstates.entrySet()) {
				buff.append(e.getKey());
				buff.append("\n");
				buff.append(e.getValue().getURLs().iterator().next());
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
					TaskOptions.Builder.withUrl("/rest/collectCron").countdownMillis(TimeUnit.SECONDS.toMillis(10)));
		} catch (final NullPointerException ex) {

		}
	}

	public final boolean isPaused() {
		return this.paused;
	}

	public final void setPaused(final boolean paused) {
		this.paused = paused;
	}

	private class CollectRunnable implements Runnable {

		private final ICollector collector;
		private final List<Estate> detectedEstates;
		private final CountDownLatch latch;

		public CollectRunnable(final ICollector collector, final List<Estate> detectedEstates,
				final CountDownLatch latch) {
			this.collector = collector;
			this.detectedEstates = detectedEstates;
			this.latch = latch;
		}

		@Override
		public void run() {
			try {
				this.collector.init();
				for (int page = 1;; page++) {
					final Document doc = Jsoup.parse(this.collector.getURL(page), 10000);
					final Set<Estate> estatesInPage = this.collector.parse(doc,
							DataCollector.this.appState.getLastScan(), page);
					if (CollectionUtils.isEmpty(estatesInPage)) {
						break;
					}
					log.fine("[" + this.collector.getName() + "] found " + estatesInPage.size() + " estates at page "
							+ page);
					mergeEstates(this.detectedEstates, estatesInPage, false);
				}
			} catch (final NoChangesException ex) {
				log.info("[" + ex.getCollectorName() + "] does not contain any changes since last parsing");
			} catch (final Exception ex) {
				log.severe("Error collecting data for " + this.collector.getName() + ": "
						+ DatabaseUtils.printStackTrace(ex));
			}
			this.latch.countDown();
		}
	}
}
