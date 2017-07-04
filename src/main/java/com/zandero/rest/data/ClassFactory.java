package com.zandero.rest.data;

import com.zandero.rest.exception.ClassFactoryException;
import com.zandero.utils.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Simple class instance cache
 */
public abstract class ClassFactory<T> {

	private final static Logger log = LoggerFactory.getLogger(ClassFactory.class);

	private Map<String, T> cache = new HashMap<>();

	protected Map<String, Class<? extends T>> classTypes = new LinkedHashMap<>();

	protected Map<String, Class<? extends T>> mediaTypes = new LinkedHashMap<>();

	public ClassFactory() {
		init();
	}

	abstract protected void init();

	public void clear() {

		// clears any additionally registered writers and initializes defaults
		classTypes.clear();
		mediaTypes.clear();
		cache.clear();

		init();
	}

	private void cache(T instance) {

		cache.put(instance.getClass().getName(), instance);
	}

	private T getCached(Class<? extends T> reader) {

		return cache.get(reader.getName());
	}

	protected T getClassInstance(Class<? extends T> clazz) throws ClassFactoryException {

		if (clazz == null) {
			return null;
		}

		try {

			T instance = getCached(clazz);
			if (instance == null) {

				instance = clazz.newInstance();
				cache(instance);
			}

			return instance;
		}
		catch (InstantiationException | IllegalAccessException e) {
			log.error("Failed to instantiate class '" + clazz.getName() + "' " + e.getMessage(), e);
			throw new ClassFactoryException("Failed to instantiate class of type: " + clazz.getName() + ", class needs empty constructor!", e);
		}
	}

	public void register(String mediaType, Class<? extends T> clazz) {

		Assert.notNull(mediaType, "Missing media type!");
		Assert.notNull(clazz, "Missing media type class");

		MediaType type = MediaType.valueOf(mediaType);
		Assert.notNull(type, "Unknown media type given: " + mediaType);

		String key = MediaTypeHelper.getKey(type);
		mediaTypes.put(key, clazz);
	}

	public void register(MediaType mediaType, Class<? extends T> clazz) {

		Assert.notNull(mediaType, "Missing media type!");
		Assert.notNull(clazz, "Missing media type class");

		String key = MediaTypeHelper.getKey(mediaType);
		mediaTypes.put(key, clazz);
	}

	public void register(Class<?> response, Class<? extends T> clazz) {

		Assert.notNull(response, "Missing response class!");
		Assert.notNull(clazz, "Missing response type class");

		classTypes.put(response.getName(), clazz);
	}

	protected T get(Class<?> type, Class<? extends T> byDefinition, MediaType[] mediaTypes) throws ClassFactoryException {

		Class<? extends T> clazz = byDefinition;

		// 2. if no writer is specified ... try to find appropriate writer by response type
		if (clazz == null) {

			if (type != null) {
				// try to find appropriate class if mapped
				clazz = classTypes.get(getKey(type));
			}
		}

		if (clazz == null) { // try by consumes

			if (mediaTypes != null && mediaTypes.length > 0) {

				for (MediaType mediaType : mediaTypes) {
					clazz = get(mediaType);
					if (clazz != null) {
						break;
					}
				}
			}
		}

		if (clazz != null) {
			return getClassInstance(clazz);
		}

		return null;
	}

	private Class<? extends T> get(MediaType mediaType) {

		if (mediaType == null) {
			return null;
		}

		return mediaTypes.get(MediaTypeHelper.getKey(mediaType));
	}

	public T get(String mediaType) throws ClassFactoryException {

		Class<? extends T> clazz = get(MediaType.valueOf(mediaType));
		return getClassInstance(clazz);
	}

	protected String getKey(Class<?> clazz) {

		return clazz.getTypeName();
	}
}
