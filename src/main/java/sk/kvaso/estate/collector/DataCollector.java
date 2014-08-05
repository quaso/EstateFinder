package sk.kvaso.estate.collector;

import java.util.Date;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;

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
import sk.kvaso.estate.model.Estate;
import sk.kvaso.estate.utils.DatabaseUtils;

import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions;

public class DataCollector implements InitializingBean {

	private final Set<ICollector> collectors;

	@Autowired
	private EstateStore store;

	@Autowired
	private DatabaseUtils databaseUtils;

	@Autowired
	public DataCollector(final Set<ICollector> collectors) {
		this.collectors = collectors;
	}

	private Date lastScan;

	public void collect() {
		if (this.store.isEmpty()) {
			this.databaseUtils.load();
		}

		final boolean storeWasEmpty = this.store.isEmpty();

		int newEstatesAdded = 0;

		//		System.setProperty("http.proxyHost", "localhost");
		//		System.setProperty("http.proxyPort", "3128");

		for (final ICollector collector : this.collectors) {
			try {
				for (int page = 1;; page++) {
					System.out.println("---- page " + page + " ----");
					final Document doc = Jsoup.parse(collector.getURL(page), 10000);
					final Set<Estate> estatesInPage = collector.parse(doc);
					if (CollectionUtils.isEmpty(estatesInPage)) {
						break;
					}
					newEstatesAdded += mergeEstates(estatesInPage);
				}
			} catch (final Exception e) {
				e.printStackTrace();
			}
		}
		this.lastScan = new Date();

		this.databaseUtils.save();

		//		if (!storeWasEmpty &&
		if (newEstatesAdded > 0) {
			sendMail(newEstatesAdded);
		}

		System.out.println("done");
	}

	private int mergeEstates(final Set<Estate> newEstates) {
		int newEstatesAdded = 0;

		for (final Estate newEstate : newEstates) {
			boolean isSame = false;
			for (final Estate estate : this.store) {
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
				newEstate.setID(this.store.size() + 1);
				newEstate.setVISIBLE(true);
				this.store.add(newEstate);
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

	private void sendMail(final int newEstatesAdded) {
		final Properties props = new Properties();
		final Session session = Session.getDefaultInstance(props, null);

		final Message msg = new MimeMessage(session);
		try {
			msg.setFrom(new InternetAddress("admin@quasoestatefinder.com", "Quaso Estate Finder Admin"));
			msg.addRecipient(Message.RecipientType.TO,
					new InternetAddress("martinkvasnicka@gmail.com", "Martin Kvasnicka"));
			msg.setSubject("New estates found: " + newEstatesAdded);
			msg.setText("http://quasoestatefinder.appspot.com/");
			Transport.send(msg);
		} catch (final Exception ex) {
			ex.printStackTrace();
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
}
