package sk.kvaso.estate.db;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.ReflectionUtils.FieldCallback;
import org.springframework.util.ReflectionUtils.FieldFilter;

import sk.kvaso.estate.EstateStore;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.PreparedQuery.TooManyResultsException;
import com.google.appengine.api.datastore.Query;
import com.google.apphosting.api.ApiProxy.OverQuotaException;

public class DatabaseUtils {
	private static final Logger log = Logger.getLogger(DatabaseUtils.class.getName());

	private boolean databaseWriteQuotaExceeded = false;

	public void saveEstates(final EstateStore store) throws Exception {
		final DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		int storedCount = 0;
		for (final Estate estate : store) {
			try {
				if (!estate.isPersisted() || estate.isDirty()) {
					datastore.put(save(estate, false));
					estate.setDirty(false);
					estate.setPersisted(true);
					storedCount++;
				}
				this.databaseWriteQuotaExceeded = false;
			} catch (final OverQuotaException ex) {
				if (!estate.isPersisted()) {
					estate.setGoogle_entity(null);
				}
				log.warning("Cannot store to database: Quota exceeded");
				this.databaseWriteQuotaExceeded = true;
				throw ex;
			} catch (final Exception ex) {
				if (!estate.isPersisted()) {
					estate.setGoogle_entity(null);
				}
				log.severe("Error storing estate: " + printStackTrace(ex));
				throw ex;
			}
		}
		log.info("Data persisted: " + storedCount);
	}

	public void saveAppState(final AppState state) {
		final DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		try {
			datastore.put(save(state, true));
			log.info("App status saved");
			this.databaseWriteQuotaExceeded = false;
		} catch (final OverQuotaException ex) {
			if (!state.isPersisted()) {
				state.setGoogle_entity(null);
			}
			log.warning("Cannot store to database: Quota exceeded");
			this.databaseWriteQuotaExceeded = true;
		} catch (final Exception ex) {
			if (!state.isPersisted()) {
				state.setGoogle_entity(null);
			}
			log.severe("Error storing app state: " + printStackTrace(ex));
		}
	}

	public void loadEstates(final EstateStore store) {
		try {
			final DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
			final Query query = new Query(Estate.class.getSimpleName());
			final List<Entity> loaded = datastore.prepare(query).asList(FetchOptions.Builder.withDefaults());

			int loadedCount = 0;
			for (final Entity e : loaded) {
				final Estate estate = load(e, new Estate(), false);
				if (estate.getAREA() > 0) {
					store.add(estate);
					loadedCount++;
				}
			}
			log.info("Data loaded: " + loadedCount);
		} catch (final OverQuotaException ex) {
			log.warning("Cannot read from database: Quota exceeded");
		} catch (final Exception ex) {
			log.warning("Cannot read from database " + printStackTrace(ex));
		}
	}

	public void loadAppState(final AppState state) {
		if (this.databaseWriteQuotaExceeded) {
			log.warning("Database contains old appstate, because write quota was exceeded");
			state.clear();
			return;
		}

		try {
			final DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
			final Query query = new Query(AppState.class.getSimpleName());
			final Entity entity = datastore.prepare(query).asSingleEntity();
			if (entity != null) {
				final AppState temp = new AppState();
				load(entity, temp, true);
				state.copyFrom(temp);
				log.info("App status loaded " + state.getGoogle_entity().getKey().toString());
			}
		} catch (final OverQuotaException ex) {
			log.warning("Cannot read from database: Quota exceeded");
		} catch (final TooManyResultsException ex) {
			log.warning("TooManyResults for AppState");
		} catch (final Exception ex) {
			log.warning("Cannot read from database " + printStackTrace(ex));
		}
	}

	public void deleteAllEstates(final EstateStore store) {
		final DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		final Query query = new Query("Estate");
		final List<Entity> loaded = datastore.prepare(query).asList(FetchOptions.Builder.withDefaults());

		final List<Key> keys = new ArrayList<>();

		for (final Entity e : loaded) {
			keys.add(e.getKey());
		}

		log.info("Deleting estates " + keys.size());

		datastore.delete(keys);

		for (final Estate e : store) {
			e.setGoogle_entity(null);
			e.setPersisted(false);
			e.setDirty(true);
		}

		log.info("All estates deleted");
	}

	public void clearAppState(final AppState appState) {
		appState.clear();
		save(appState, false);

		log.info("App state cleared");
	}

	public int count() {
		final DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		final Query query = new Query("Estate");
		final List<Entity> loaded = datastore.prepare(query).asList(FetchOptions.Builder.withDefaults());
		log.info("Count is: " + loaded.size());
		return loaded.size();
	}

	@SuppressWarnings("unchecked")
	private Entity save(final Data data, final boolean fineLoggingEnabled) {

		class EntityResolver {
			public Entity getEntity(final Data data) {
				Entity entity;
				if (data.getGoogle_entity() != null) {
					entity = data.getGoogle_entity();
					if (fineLoggingEnabled) {
						log.fine("Found key " + entity.getKey().toString());
					}
				} else {
					final Key key = KeyFactory.createKey(data.getDatabaseName(), data.getID());
					entity = new Entity(data.getDatabaseName(), key);
					data.setGoogle_entity(entity);
				}
				return entity;
			}
		}

		final Entity entity = new EntityResolver().getEntity(data);
		ReflectionUtils.doWithFields(data.getClass(), new FieldCallback() {

			@Override
			public void doWith(final Field field) throws IllegalArgumentException, IllegalAccessException {
				ReflectionUtils.makeAccessible(field);
				final Object value = field.get(data);
				if (value != null) {
					if (value instanceof Map) {
						if (fineLoggingEnabled) {
							log.fine("Storing Map " + field.getName());
						}
						for (final Map.Entry<String, String> entry : ((Map<String, String>) value).entrySet()) {
							entity.setProperty(field.getName() + " " + entry.getKey(), entry.getValue());
							if (fineLoggingEnabled) {
								log.fine("Storing Map key " + entry.getKey());
							}
						}
					} else {
						if (fineLoggingEnabled) {
							log.fine("Storing property " + field.getName());
						}
						entity.setProperty(field.getName(), value);
					}
				}
			}
		}, new FieldFilter() {

			@Override
			public boolean matches(final Field field) {
				switch (field.getName()) {
					case "serialVersionUID" :
					case "google_entity" :
					case "dirty" :
						return false;
					default :
						return true;
				}
			}
		});

		return entity;
	}

	@SuppressWarnings("unchecked")
	private <T extends Data> T load(final Entity entity, final T data, final boolean fineLoggingEnabled)
			throws Exception {
		final Map<String, Object> properties = entity.getProperties();
		final Map<String, Map<String, String>> maps = new HashMap<>();

		if (fineLoggingEnabled) {
			log.fine("Loading properties " + StringUtils.join(properties.keySet(), ", "));
		}

		try {
			for (final String propertyName : properties.keySet()) {
				if (propertyName.contains(" ")) {
					if (fineLoggingEnabled) {
						log.fine("Found complex property '" + propertyName + "'");
					}
					final String propertyNameReal = StringUtils.substringBefore(propertyName, " ");
					if (!maps.containsKey(propertyNameReal)) {
						final Field field = ReflectionUtils.findField(data.getClass(), propertyNameReal);
						if (field != null) {
							final Object value = field.get(data);
							if (value instanceof Map) {
								maps.put(propertyNameReal, (Map<String, String>) value);
							} else {
								log.warning("Unrecognized complex property '" + propertyName + "'");
								continue;
							}
						} else {
							log.warning("Unknown complex property '" + propertyName + "'");
							continue;
						}
					}
					final Map<String, String> map = maps.get(propertyNameReal);
					final String keyName = StringUtils.substringAfter(propertyName, " ");
					if (fineLoggingEnabled) {
						log.fine("Loading key '" + keyName + "' from complex property '" + propertyName + "'");
					}
					map.put(keyName, (String) properties.get(propertyName));
				} else {
					if (fineLoggingEnabled) {
						log.fine("Loading simple property '" + propertyName + "'");
					}
					final Field field = ReflectionUtils.findField(data.getClass(), propertyName);
					if (field != null) {
						ReflectionUtils.makeAccessible(field);
						try {
							field.set(data, properties.get(propertyName));
						} catch (final Exception ex) {
							// ignore
						}
					} else {
						log.warning("Unknown simple property '" + propertyName + "'");
					}
				}
			}
		} catch (final Exception ex) {
			log.severe("Error loading data " + printStackTrace(ex));
			throw ex;
		}

		data.setGoogle_entity(entity);
		data.setDirty(false);
		data.setPersisted(true);

		return data;
	}

	public static String printStackTrace(final Throwable t) {
		final StringWriter str = new StringWriter();
		final PrintWriter pw = new PrintWriter(str);
		t.printStackTrace(pw);
		return str.toString();
	}
}
