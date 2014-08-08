package sk.kvaso.estate.db;

import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;

import sk.kvaso.estate.EstateStore;

public class DatabaseUtils {
	private static final Logger log = Logger.getLogger(DatabaseUtils.class.getName());

	@Autowired
	private EstateStore store;

	public void save() {
		//		final DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		//		for (final Estate estate : this.store) {
		//			try {
		//				datastore.put(save(estate));
		//			} catch (final OverQuotaException ex) {
		//				estate.setPersisted(false);
		//								estate.setGoogle_entity(null);
		//				log.warning("Cannot store to database: Quota exceeded");
		//				return;
		//			} catch (final Exception ex) {
		//				estate.setPersisted(false);
		//								estate.setGoogle_entity(null);
		//				log.severe("Error storing estate: " + ex.getMessage());
		//				return;
		//			}
		//		}
		//		log.info("Data persisted");
	}

	public void load() {
		//		try {
		//			final DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		//			final Query query = new Query("Estate");
		//			final List<Entity> loaded = datastore.prepare(query).asList(FetchOptions.Builder.withDefaults());
		//
		//			for (final Entity e : loaded) {
		//				this.store.add(load(e));
		//			}
		//			log.info("Data loaded");
		//		} catch (final OverQuotaException ex) {
		//			log.warning("Cannot read from database: Quota exceeded");
		//		}
	}

	//	private Entity save(final Estate estate) {
	//		final Entity entity = null;
	//		if (estate.getGoogle_entity() != null) {
	//			entity = estate.getGoogle_entity();
	//		} else {
	//			final Key key = KeyFactory.createKey("Estate", estate.getID());
	//			entity = new Entity("Estate", key);
	//			estate.setGoogle_entity(entity);
	//		}
	//		estate.setPersisted(true);
	//
	//		final BeanWrapper beanWrapper = PropertyAccessorFactory.forBeanPropertyAccess(estate);
	//		final PropertyDescriptor[] propertyDescriptors = beanWrapper.getPropertyDescriptors();
	//		for (final PropertyDescriptor pd : propertyDescriptors) {
	//			if (!"google_entity".equals(pd.getName())) {
	//				try {
	//					final Object value = beanWrapper.getPropertyValue(pd.getName());
	//					if (value != null) {
	//						entity.setProperty(pd.getName(), value);
	//					}
	//				} catch (final Exception ex) {
	//					// ignore
	//				}
	//			}
	//		}

	//		return entity;
	//	}

	//	private Estate load(final Entity entity) {
	//		final Estate estate = new Estate();

	//		final BeanWrapper beanWrapper = PropertyAccessorFactory.forBeanPropertyAccess(estate);
	//
	//		final Map<String, Object> properties = entity.getProperties();
	//		for (final String propertyName : properties.keySet()) {
	//			if ("google_entity".equals(propertyName)) {
	//				//				estate.setGoogle_entity(entity);
	//			} else {
	//				try {
	//					beanWrapper.setPropertyValue(propertyName, properties.get(propertyName));
	//				} catch (final Exception ex) {
	//					// ignore
	//				}
	//			}
	//		}

	//		return estate;
	//	}
}
