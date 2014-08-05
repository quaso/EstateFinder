package sk.kvaso.estate.utils;

import java.beans.PropertyDescriptor;
import java.util.List;
import java.util.Map;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.beans.factory.annotation.Autowired;

import sk.kvaso.estate.EstateStore;
import sk.kvaso.estate.model.Estate;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;

public class DatabaseUtils {

	@Autowired
	private EstateStore store;

	public void save() {
		final DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

		for (final Estate estate : this.store) {
			if (!estate.isPersisted()) {
				datastore.put(save(estate));
			}
		}
	}

	public void load() {
		final DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		final Query query = new Query("Estate");
		final List<Entity> loaded = datastore.prepare(query).asList(FetchOptions.Builder.withDefaults());

		for (final Entity e : loaded) {
			this.store.add(load(e));
		}
	}

	private Entity save(final Estate estate) {
		final Key key = KeyFactory.createKey("Estate", estate.getID());
		final Entity entity = new Entity("Estate", key);
		estate.setPersisted(true);

		final BeanWrapper beanWrapper = PropertyAccessorFactory.forBeanPropertyAccess(estate);
		final PropertyDescriptor[] propertyDescriptors = beanWrapper.getPropertyDescriptors();
		for (final PropertyDescriptor pd : propertyDescriptors) {
			try {
				final Object value = beanWrapper.getPropertyValue(pd.getName());
				if (value != null) {
					entity.setProperty(pd.getName(), value);
				}
			} catch (final Exception ex) {
				// ignore
			}
		}

		return entity;
	}

	private Estate load(final Entity entity) {
		final Estate estate = new Estate();

		final BeanWrapper beanWrapper = PropertyAccessorFactory.forBeanPropertyAccess(estate);

		final Map<String, Object> properties = entity.getProperties();
		for (final String propertyName : properties.keySet()) {
			try {
				beanWrapper.setPropertyValue(propertyName, properties.get(propertyName));
			} catch (final Exception ex) {
				// ignore
			}
		}

		return estate;
	}
}
