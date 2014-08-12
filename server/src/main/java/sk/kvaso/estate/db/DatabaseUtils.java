package sk.kvaso.estate.db;

import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.beans.factory.annotation.Autowired;

import sk.kvaso.estate.EstateStore;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;
import com.google.apphosting.api.ApiProxy.OverQuotaException;

public class DatabaseUtils {
	private static final Logger log = Logger.getLogger(DatabaseUtils.class.getName());

	@Autowired
	private EstateStore store;

	public void save() throws Exception {
		final DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		int storedCount = 0;
		for (final Estate estate : this.store) {
			try {
				if (!estate.isPersisted()) {
					datastore.put(save(estate));
					storedCount++;
				}
			} catch (final OverQuotaException ex) {
				estate.setPersisted(false);
				estate.setGoogle_entity(null);
				log.warning("Cannot store to database: Quota exceeded");
				throw ex;
			} catch (final Exception ex) {
				estate.setPersisted(false);
				estate.setGoogle_entity(null);
				log.severe("Error storing estate: " + ex.getMessage());
				throw ex;
			}
		}
		log.info("Data persisted: " + storedCount);
	}

	public void load() {
		try {
			final DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
			final Query query = new Query("Estate");
			final List<Entity> loaded = datastore.prepare(query).asList(FetchOptions.Builder.withDefaults());

			int loadedCount = 0;
			for (final Entity e : loaded) {
				this.store.add(load(e));
				loadedCount++;
			}
			log.info("Data loaded: " + loadedCount);
		} catch (final OverQuotaException ex) {
			log.warning("Cannot read from database: Quota exceeded");
		} catch (final NullPointerException ex) {
			log.warning("Cannot read from database");
		}
	}

	public void deleteAll() {
		final DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		final Query query = new Query("Estate");
		final List<Entity> loaded = datastore.prepare(query).asList(FetchOptions.Builder.withDefaults());

		final List<Key> keys = new ArrayList<>();

		for (final Entity e : loaded) {
			keys.add(e.getKey());
		}

		log.info("Deleting " + keys.size());

		datastore.delete(keys);

		log.info("Deleted all");
	}

	public int count() {
		final DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		final Query query = new Query("Estate");
		final List<Entity> loaded = datastore.prepare(query).asList(FetchOptions.Builder.withDefaults());
		log.info("Count is: " + loaded.size());
		return loaded.size();
	}

	private Entity save(final Estate estate) {
		Entity entity = null;
		if (estate.getGoogle_entity() != null) {
			entity = estate.getGoogle_entity();
		} else {
			final Key key = KeyFactory.createKey("Estate", estate.getID());
			entity = new Entity("Estate", key);
			estate.setGoogle_entity(entity);
		}
		estate.setPersisted(true);

		final BeanWrapper beanWrapper = PropertyAccessorFactory.forBeanPropertyAccess(estate);
		final PropertyDescriptor[] propertyDescriptors = beanWrapper.getPropertyDescriptors();
		for (final PropertyDescriptor pd : propertyDescriptors) {
			if (!"google_entity".equals(pd.getName())) {
				try {
					final Object value = beanWrapper.getPropertyValue(pd.getName());
					if (value != null) {
						entity.setProperty(pd.getName(), value);
					}
				} catch (final Exception ex) {
					// ignore
				}
			}
		}

		return entity;
	}

	private Estate load(final Entity entity) {
		final Estate estate = new Estate();

		final BeanWrapper beanWrapper =
				PropertyAccessorFactory.forBeanPropertyAccess(estate);

		final Map<String, Object> properties = entity.getProperties();
		for (final String propertyName : properties.keySet()) {
			if ("google_entity".equals(propertyName)) {
				estate.setGoogle_entity(entity);
			} else {
				try {
					beanWrapper.setPropertyValue(propertyName, properties.get(propertyName));
				} catch (final Exception ex) {
					// ignore
				}
			}
		}

		return estate;
	}
}
