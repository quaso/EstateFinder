package sk.kvaso.estate.collector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

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

import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions;

public class DataCollector implements InitializingBean {
	private static final Logger log = Logger.getLogger(DataCollector.class.getName());

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
			this.databaseUtils.load();
		}

		final boolean wasEmpty = this.store.isEmpty();

		int newEstatesCount = 0;

		//		System.setProperty("http.proxyHost", "localhost");
		//		System.setProperty("http.proxyPort", "3128");

		this.lastScan = new Date();

		final List<Estate> detectedEstates = new ArrayList<Estate>();

		for (final ICollector collector : this.collectors) {
			log.info("Using [" + collector.getName() + "]");
			try {
				for (int page = 1;; page++) {
					log.info("---- page " + page + " ----");
					final Document doc = Jsoup.parse(collector.getURL(page), 10000);
					final Set<Estate> estatesInPage = collector.parse(doc, this.lastScan);
					if (CollectionUtils.isEmpty(estatesInPage)) {
						break;
					}
					newEstatesCount += mergeEstates(detectedEstates, estatesInPage, false);
				}
			} catch (final Exception e) {
				e.printStackTrace();
			}
		}

		final Set<String> streets = getStreets(this.store, detectedEstates);
		for (final Estate e : detectedEstates) {
			if (StringUtils.isEmpty(e.getSTREET())) {
				final String title = e.getTITLE().replaceAll("A.MRAZA", "Andreja Mráza")
						.replaceAll("A. MRAZA", "Andreja Mráza").replaceAll("A.MRÁZA", "Andreja Mráza")
						.replaceAll("A. MRÁZA", "Andreja Mráza").replaceAll("A.Mraza", "Andreja Mráza")
						.replaceAll("A. Mraza", "Andreja Mráza").replaceAll("A.Mráza", "Andreja Mráza")
						.replaceAll("A. Mráza", "Andreja Mráza");
				for (final String street : streets) {
					if (title.contains(street)) {
						e.setSTREET(street);
						break;
					}
				}
			}
		}

		mergeEstates(this.store, detectedEstates, true);

		this.databaseUtils.save();

		if (!wasEmpty && newEstatesCount > 0) {
			sendMail(newEstatesCount);
		}

		log.info("done");
	}

	private int mergeEstates(final Collection<Estate> allEstates, final Collection<Estate> newEstates,
			final boolean setId) {
		int newEstatesAdded = 0;

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
				newEstatesAdded++;
				if (setId) {
					newEstate.setID(allEstates.size() + 1);
				}
				newEstate.setVISIBLE(true);
				allEstates.add(newEstate);
			}
		}

		return newEstatesAdded;
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
		final Set<String> result = new HashSet<>();
		for (final Collection<Estate> c : estates) {
			for (final Estate e : c) {
				if (!StringUtils.isEmpty(e.getSTREET())) {
					result.add(e.getSTREET());
				}
			}
		}
		return result;
	}

	private void sendMail(final int newEstatesCount) {
		final Properties props = new Properties();
		final Session session = Session.getDefaultInstance(props, null);

		final Message msg = new MimeMessage(session);
		try {
			msg.setFrom(new InternetAddress("martinkvasnicka@gmail.com", "Quaso Estate Finder Admin"));
			msg.addRecipient(Message.RecipientType.TO,
					new InternetAddress("martinkvasnicka@gmail.com", "Martin Kvasnicka"));
			msg.setSubject("New estates found: " + newEstatesCount);
			msg.setText("http://quasoestatefinder.appspot.com/");
			Transport.send(msg);
		} catch (final Exception ex) {
			log.warning("Cannot send mail " + ex.getMessage());
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		QueueFactory.getDefaultQueue().add(
				TaskOptions.Builder.withUrl("/collectCron").countdownMillis(TimeUnit.MINUTES.toMillis(1)));
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
}
