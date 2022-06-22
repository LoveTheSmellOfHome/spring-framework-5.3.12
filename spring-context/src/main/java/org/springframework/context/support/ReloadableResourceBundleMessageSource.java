/*
 * Copyright 2002-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.context.support;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePropertiesPersister;
import org.springframework.lang.Nullable;
import org.springframework.util.PropertiesPersister;
import org.springframework.util.StringUtils;

/**
 * Spring-specific {@link org.springframework.context.MessageSource} implementation
 * that accesses resource bundles using specified basenames, participating in the
 * Spring {@link org.springframework.context.ApplicationContext}'s resource loading.
 *
 * <p>In contrast to the JDK-based {@link ResourceBundleMessageSource}, this class uses
 * {@link java.util.Properties} instances as its custom data structure for messages,
 * loading them via a {@link org.springframework.util.PropertiesPersister} strategy
 * from Spring {@link Resource} handles. This strategy is not only capable of
 * reloading files based on timestamp changes, but also of loading properties files
 * with a specific character encoding. It will detect XML property files as well.
 *
 * <p>Note that the basenames set as {@link #setBasenames "basenames"} property
 * are treated in a slightly different fashion than the "basenames" property of
 * {@link ResourceBundleMessageSource}. It follows the basic ResourceBundle rule of not
 * specifying file extension or language codes, but can refer to any Spring resource
 * location (instead of being restricted to classpath resources). With a "classpath:"
 * prefix, resources can still be loaded from the classpath, but "cacheSeconds" values
 * other than "-1" (caching forever) might not work reliably in this case.
 *
 * <p>For a typical web application, message files could be placed in {@code WEB-INF}:
 * e.g. a "WEB-INF/messages" basename would find a "WEB-INF/messages.properties",
 * "WEB-INF/messages_en.properties" etc arrangement as well as "WEB-INF/messages.xml",
 * "WEB-INF/messages_en.xml" etc. Note that message definitions in a <i>previous</i>
 * resource bundle will override ones in a later bundle, due to sequential lookup.

 * <p>This MessageSource can easily be used outside of an
 * {@link org.springframework.context.ApplicationContext}: it will use a
 * {@link org.springframework.core.io.DefaultResourceLoader} as default,
 * simply getting overridden with the ApplicationContext's resource loader
 * if running in a context. It does not have any other specific dependencies.
 *
 * <p>Thanks to Thomas Achleitner for providing the initial implementation of
 * this message source!
 *
 * @author Juergen Hoeller
 * @see #setCacheSeconds
 * @see #setBasenames
 * @see #setDefaultEncoding
 * @see #setFileEncodings
 * @see #setPropertiesPersister
 * @see #setResourceLoader
 * @see ResourcePropertiesPersister
 * @see org.springframework.core.io.DefaultResourceLoader
 * @see ResourceBundleMessageSource
 * @see java.util.ResourceBundle
 */
// Spring 特定的 {@link org.springframework.context.MessageSource} 实现，它使用指定的基本名称访问资源包，
// 参与 Spring {@link org.springframework.context.ApplicationContext} 的资源加载。
//
// <p>与基于 JDK 的 {@link ResourceBundleMessageSource} 相比，该类使用 {@link java.util.Properties} 实例
// 作为其自定义消息数据结构来存储，通过 {@link org.springframework.util 加载它们。来自 Spring {@link Resource} 的
// PropertiesPersister} 策略处理。此策略不仅能够根据时间戳更改重新加载文件，还能够加载具有特定字符编码的属性文件。
// 它也会检测 XML 属性文件。
//
// <p>请注意，设置为 {@link setBasenames "basenames"} 属性的 basenames 的处理方式与
// {@link ResourceBundleMessageSource} 的 "basenames" 属性略有不同。
// 它遵循不指定文件扩展名或语言代码的基本 ResourceBundle 规则，但可以引用任何 Spring 资源位置（而不是仅限于类路径资源）。
// 使用“classpath:”前缀，仍然可以从类路径加载资源，但“-1”（永远缓存）以外的“cacheSeconds”值在这种情况下可能无法可靠地工作
public class ReloadableResourceBundleMessageSource extends AbstractResourceBasedMessageSource
		implements ResourceLoaderAware {

	private static final String PROPERTIES_SUFFIX = ".properties";

	private static final String XML_SUFFIX = ".xml";


	@Nullable
	private Properties fileEncodings;

	private boolean concurrentRefresh = true;

	private PropertiesPersister propertiesPersister = ResourcePropertiesPersister.INSTANCE;

	private ResourceLoader resourceLoader = new DefaultResourceLoader();

	// Cache to hold filename lists per Locale
	// 缓存以保存每个区域设置的文件名列表
	private final ConcurrentMap<String, Map<Locale, List<String>>> cachedFilenames = new ConcurrentHashMap<>();

	// Cache to hold already loaded properties per filename
	// 缓存以保存每个文件名已加载的属性
	private final ConcurrentMap<String, PropertiesHolder> cachedProperties = new ConcurrentHashMap<>();

	// Cache to hold already loaded properties per filename
	// 缓存以保存每个文件名已加载的属性
	private final ConcurrentMap<Locale, PropertiesHolder> cachedMergedProperties = new ConcurrentHashMap<>();


	/**
	 * Set per-file charsets to use for parsing properties files.
	 * <p>Only applies to classic properties files, not to XML files.
	 * @param fileEncodings a Properties with filenames as keys and charset
	 * names as values. Filenames have to match the basename syntax,
	 * with optional locale-specific components: e.g. "WEB-INF/messages"
	 * or "WEB-INF/messages_en".
	 * @see #setBasenames
	 * @see org.springframework.util.PropertiesPersister#load
	 */
	// 设置用于解析属性文件的每个文件字符集。
	// <p>仅适用于经典属性文件，不适用于 XML 文件。
	// @param fileEncodings 以文件名作为键和字符集名称作为值的属性。文件名必须与 basename 语法匹配，
	// 并带有可选的特定于语言环境的组件：例如“WEB-INFmessages”或“WEB-INFmessages_en”。
	public void setFileEncodings(Properties fileEncodings) {
		this.fileEncodings = fileEncodings;
	}

	/**
	 * Specify whether to allow for concurrent refresh behavior, i.e. one thread
	 * locked in a refresh attempt for a specific cached properties file whereas
	 * other threads keep returning the old properties for the time being, until
	 * the refresh attempt has completed.
	 * <p>Default is "true": this behavior is new as of Spring Framework 4.1,
	 * minimizing contention between threads. If you prefer the old behavior,
	 * i.e. to fully block on refresh, switch this flag to "false".
	 * @since 4.1
	 * @see #setCacheSeconds
	 */
	// 指定是否允许并发刷新行为，即一个线程锁定在特定缓存属性文件的刷新尝试中，而其他线程暂时保持返回旧属性，直到刷新尝试完成。
	// <p>默认为“true”：此行为从 Spring Framework 4.1 开始是新的，可最大程度地减少线程之间的争用。
	// 如果您更喜欢旧行为，即完全阻止刷新，请将此标志切换为“false”。
	public void setConcurrentRefresh(boolean concurrentRefresh) {
		this.concurrentRefresh = concurrentRefresh;
	}

	/**
	 * Set the PropertiesPersister to use for parsing properties files.
	 * <p>The default is ResourcePropertiesPersister.
	 * @see ResourcePropertiesPersister#INSTANCE
	 */
	// 设置 PropertiesPersister 以用于解析属性文件。
	// <p>默认值为 ResourcePropertiesPersister。
	public void setPropertiesPersister(@Nullable PropertiesPersister propertiesPersister) {
		this.propertiesPersister =
				(propertiesPersister != null ? propertiesPersister : ResourcePropertiesPersister.INSTANCE);
	}

	/**
	 * Set the ResourceLoader to use for loading bundle properties files.
	 * <p>The default is a DefaultResourceLoader. Will get overridden by the
	 * ApplicationContext if running in a context, as it implements the
	 * ResourceLoaderAware interface. Can be manually overridden when
	 * running outside of an ApplicationContext.
	 * @see org.springframework.core.io.DefaultResourceLoader
	 * @see org.springframework.context.ResourceLoaderAware
	 */
	// 设置 ResourceLoader 以用于加载包属性文件。
	// <p>默认是一个 DefaultResourceLoader。如果在上下文中运行，将被 ApplicationContext 覆盖，
	// 因为它实现了 ResourceLoaderAware 接口。在 ApplicationContext 之外运行时可以手动覆盖。
	@Override
	public void setResourceLoader(@Nullable ResourceLoader resourceLoader) {
		this.resourceLoader = (resourceLoader != null ? resourceLoader : new DefaultResourceLoader());
	}


	/**
	 * Resolves the given message code as key in the retrieved bundle files,
	 * returning the value found in the bundle as-is (without MessageFormat parsing).
	 */
	// 将给定的消息代码解析为检索到的包文件中的键，按原样返回在包中找到的值（没有 MessageFormat 解析）
	@Override
	protected String resolveCodeWithoutArguments(String code, Locale locale) {
		if (getCacheMillis() < 0) {
			PropertiesHolder propHolder = getMergedProperties(locale);
			String result = propHolder.getProperty(code);
			if (result != null) {
				return result;
			}
		}
		else {
			for (String basename : getBasenameSet()) {
				List<String> filenames = calculateAllFilenames(basename, locale);
				for (String filename : filenames) {
					PropertiesHolder propHolder = getProperties(filename);
					String result = propHolder.getProperty(code);
					if (result != null) {
						return result;
					}
				}
			}
		}
		return null;
	}

	/**
	 * Resolves the given message code as key in the retrieved bundle files,
	 * using a cached MessageFormat instance per message code.
	 */
	// 将给定的消息代码解析为检索到的包文件中的键，使用每个消息代码的缓存 MessageFormat 实例。
	@Override
	@Nullable
	protected MessageFormat resolveCode(String code, Locale locale) {
		if (getCacheMillis() < 0) {
			PropertiesHolder propHolder = getMergedProperties(locale);
			MessageFormat result = propHolder.getMessageFormat(code, locale);
			if (result != null) {
				return result;
			}
		}
		else {
			for (String basename : getBasenameSet()) {
				List<String> filenames = calculateAllFilenames(basename, locale);
				for (String filename : filenames) {
					PropertiesHolder propHolder = getProperties(filename);
					MessageFormat result = propHolder.getMessageFormat(code, locale);
					if (result != null) {
						return result;
					}
				}
			}
		}
		return null;
	}


	/**
	 * Get a PropertiesHolder that contains the actually visible properties
	 * for a Locale, after merging all specified resource bundles.
	 * Either fetches the holder from the cache or freshly loads it.
	 * <p>Only used when caching resource bundle contents forever, i.e.
	 * with cacheSeconds &lt; 0. Therefore, merged properties are always
	 * cached forever.
	 */
	// 合并所有指定的资源包后，获取包含 Locale 的实际可见属性的 PropertiesHolder。从缓存中获取持有者或重新加载它。
	// <p>仅在永久缓存资源包内容时使用，即 cacheSeconds < 0。因此，合并的属性总是被永久缓存。
	protected PropertiesHolder getMergedProperties(Locale locale) {
		PropertiesHolder mergedHolder = this.cachedMergedProperties.get(locale);
		if (mergedHolder != null) {
			return mergedHolder;
		}

		Properties mergedProps = newProperties();
		long latestTimestamp = -1;
		String[] basenames = StringUtils.toStringArray(getBasenameSet());
		for (int i = basenames.length - 1; i >= 0; i--) {
			List<String> filenames = calculateAllFilenames(basenames[i], locale);
			for (int j = filenames.size() - 1; j >= 0; j--) {
				String filename = filenames.get(j);
				PropertiesHolder propHolder = getProperties(filename);
				if (propHolder.getProperties() != null) {
					mergedProps.putAll(propHolder.getProperties());
					if (propHolder.getFileTimestamp() > latestTimestamp) {
						latestTimestamp = propHolder.getFileTimestamp();
					}
				}
			}
		}

		mergedHolder = new PropertiesHolder(mergedProps, latestTimestamp);
		PropertiesHolder existing = this.cachedMergedProperties.putIfAbsent(locale, mergedHolder);
		if (existing != null) {
			mergedHolder = existing;
		}
		return mergedHolder;
	}

	/**
	 * Calculate all filenames for the given bundle basename and Locale.
	 * Will calculate filenames for the given Locale, the system Locale
	 * (if applicable), and the default file.
	 * @param basename the basename of the bundle
	 * @param locale the locale
	 * @return the List of filenames to check
	 * @see #setFallbackToSystemLocale
	 * @see #calculateFilenamesForLocale
	 */
	// 计算给定包基本名称和区域设置的所有文件名。将计算给定区域设置、系统区域设置（如果适用）和默认文件的文件名。
	protected List<String> calculateAllFilenames(String basename, Locale locale) {
		Map<Locale, List<String>> localeMap = this.cachedFilenames.get(basename);
		if (localeMap != null) {
			List<String> filenames = localeMap.get(locale);
			if (filenames != null) {
				return filenames;
			}
		}

		// Filenames for given Locale
		// 给定语言环境的文件名
		List<String> filenames = new ArrayList<>(7);
		filenames.addAll(calculateFilenamesForLocale(basename, locale));

		// Filenames for default Locale, if any
		// 默认语言环境的文件名，如果有的话
		Locale defaultLocale = getDefaultLocale();
		if (defaultLocale != null && !defaultLocale.equals(locale)) {
			List<String> fallbackFilenames = calculateFilenamesForLocale(basename, defaultLocale);
			for (String fallbackFilename : fallbackFilenames) {
				if (!filenames.contains(fallbackFilename)) {
					// Entry for fallback locale that isn't already in filenames list.
					// 尚未在文件名列表中的后备语言环境的条目。
					filenames.add(fallbackFilename);
				}
			}
		}

		// Filename for default bundle file
		// 默认包文件的文件名
		filenames.add(basename);

		if (localeMap == null) {
			localeMap = new ConcurrentHashMap<>();
			Map<Locale, List<String>> existing = this.cachedFilenames.putIfAbsent(basename, localeMap);
			if (existing != null) {
				localeMap = existing;
			}
		}
		localeMap.put(locale, filenames);
		return filenames;
	}

	/**
	 * Calculate the filenames for the given bundle basename and Locale,
	 * appending language code, country code, and variant code.
	 * <p>For example, basename "messages", Locale "de_AT_oo" &rarr; "messages_de_AT_OO",
	 * "messages_de_AT", "messages_de".
	 * <p>Follows the rules defined by {@link java.util.Locale#toString()}.
	 * @param basename the basename of the bundle
	 * @param locale the locale
	 * @return the List of filenames to check
	 */
	// 计算给定包基本名称和区域设置的文件名，附加语言代码、国家/地区代码和变体代码。
	// <p>例如，基本名称“messages”、区域设置“de_AT_oo”→“messages_de_AT_OO”、“messages_de_AT”、“messages_de”。
	// <p>遵循 {@link java.util.LocaletoString()} 定义的规则。
	protected List<String> calculateFilenamesForLocale(String basename, Locale locale) {
		List<String> result = new ArrayList<>(3);
		String language = locale.getLanguage();
		String country = locale.getCountry();
		String variant = locale.getVariant();
		StringBuilder temp = new StringBuilder(basename);

		temp.append('_');
		if (language.length() > 0) {
			temp.append(language);
			result.add(0, temp.toString());
		}

		temp.append('_');
		if (country.length() > 0) {
			temp.append(country);
			result.add(0, temp.toString());
		}

		if (variant.length() > 0 && (language.length() > 0 || country.length() > 0)) {
			temp.append('_').append(variant);
			result.add(0, temp.toString());
		}

		return result;
	}


	/**
	 * Get a PropertiesHolder for the given filename, either from the
	 * cache or freshly loaded.
	 * @param filename the bundle filename (basename + Locale)
	 * @return the current PropertiesHolder for the bundle
	 */
	// 从缓存或新加载的给定文件名获取 PropertiesHolder。
	protected PropertiesHolder getProperties(String filename) {
		PropertiesHolder propHolder = this.cachedProperties.get(filename);
		long originalTimestamp = -2;

		if (propHolder != null) {
			originalTimestamp = propHolder.getRefreshTimestamp();
			if (originalTimestamp == -1 || originalTimestamp > System.currentTimeMillis() - getCacheMillis()) {
				// Up to date
				// 最新
				return propHolder;
			}
		}
		else {
			propHolder = new PropertiesHolder();
			PropertiesHolder existingHolder = this.cachedProperties.putIfAbsent(filename, propHolder);
			if (existingHolder != null) {
				propHolder = existingHolder;
			}
		}

		// At this point, we need to refresh...
		// 此时，我们需要刷新...
		if (this.concurrentRefresh && propHolder.getRefreshTimestamp() >= 0) {
			// A populated but stale holder -> could keep using it.
			if (!propHolder.refreshLock.tryLock()) {
				// Getting refreshed by another thread already ->
				// let's return the existing properties for the time being.
				return propHolder;
			}
		}
		else {
			propHolder.refreshLock.lock();
		}
		try {
			PropertiesHolder existingHolder = this.cachedProperties.get(filename);
			if (existingHolder != null && existingHolder.getRefreshTimestamp() > originalTimestamp) {
				return existingHolder;
			}
			return refreshProperties(filename, propHolder);
		}
		finally {
			propHolder.refreshLock.unlock();
		}
	}

	/**
	 * Refresh the PropertiesHolder for the given bundle filename.
	 * The holder can be {@code null} if not cached before, or a timed-out cache entry
	 * (potentially getting re-validated against the current last-modified timestamp).
	 * @param filename the bundle filename (basename + Locale)
	 * @param propHolder the current PropertiesHolder for the bundle
	 */
	// 刷新给定包文件名的 PropertiesHolder。如果之前没有缓存，持有者可以是 {@code null}，
	// 或者是超时缓存条目（可能会根据当前最后修改的时间戳重新验证）。
	// @param filename 包文件名 (basename + Locale)
	// @param propHolder 包的当前 PropertiesHolder
	protected PropertiesHolder refreshProperties(String filename, @Nullable PropertiesHolder propHolder) {
		long refreshTimestamp = (getCacheMillis() < 0 ? -1 : System.currentTimeMillis());

		Resource resource = this.resourceLoader.getResource(filename + PROPERTIES_SUFFIX);
		if (!resource.exists()) {
			resource = this.resourceLoader.getResource(filename + XML_SUFFIX);
		}

		if (resource.exists()) {
			long fileTimestamp = -1;
			if (getCacheMillis() >= 0) {
				// Last-modified timestamp of file will just be read if caching with timeout.
				// 如果缓存超时，则只会读取文件的最后修改时间戳
				try {
					// 1.资源管理的时候，lastModified 不一定存在，就不刷新
					// 2.默认实现通常是在 classpath 下，是一个文件资源的形式，如果是在 Docker 等容器(静态化技术)中，
					// 通常文件是不变的，那么这个实现是无用的
					// 3.如果是在 DB 或者 redis 实现会好的多。
					fileTimestamp = resource.lastModified();
					if (propHolder != null && propHolder.getFileTimestamp() == fileTimestamp) {
						if (logger.isDebugEnabled()) {
							logger.debug("Re-caching properties for filename [" + filename + "] - file hasn't been modified");
						}
						propHolder.setRefreshTimestamp(refreshTimestamp);
						return propHolder;
					}
				}
				catch (IOException ex) {
					// Probably a class path resource: cache it forever.
					// 可能是类路径资源：永远缓存它
					if (logger.isDebugEnabled()) {
						logger.debug(resource + " could not be resolved in the file system - assuming that it hasn't changed", ex);
					}
					fileTimestamp = -1;
				}
			}
			try {
				Properties props = loadProperties(resource, filename);
				propHolder = new PropertiesHolder(props, fileTimestamp);
			}
			catch (IOException ex) {
				if (logger.isWarnEnabled()) {
					logger.warn("Could not parse properties file [" + resource.getFilename() + "]", ex);
				}
				// Empty holder representing "not valid".
				// 代表“无效”的空持有人。
				propHolder = new PropertiesHolder();
			}
		}

		else {
			// Resource does not exist.
			// 资源不存在。
			if (logger.isDebugEnabled()) {
				logger.debug("No properties file found for [" + filename + "] - neither plain properties nor XML");
			}
			// Empty holder representing "not found".
			// 代表“未找到”的空持有人。
			propHolder = new PropertiesHolder();
		}

		propHolder.setRefreshTimestamp(refreshTimestamp);
		this.cachedProperties.put(filename, propHolder);
		return propHolder;
	}

	/**
	 * Load the properties from the given resource.
	 * @param resource the resource to load from
	 * @param filename the original bundle filename (basename + Locale)
	 * @return the populated Properties instance
	 * @throws IOException if properties loading failed
	 */
	// 从给定资源加载属性
	protected Properties loadProperties(Resource resource, String filename) throws IOException {
		Properties props = newProperties();
		try (InputStream is = resource.getInputStream()) {
			String resourceFilename = resource.getFilename();
			if (resourceFilename != null && resourceFilename.endsWith(XML_SUFFIX)) {
				if (logger.isDebugEnabled()) {
					logger.debug("Loading properties [" + resource.getFilename() + "]");
				}
				this.propertiesPersister.loadFromXml(props, is);
			}
			else {
				String encoding = null;
				if (this.fileEncodings != null) {
					encoding = this.fileEncodings.getProperty(filename);
				}
				if (encoding == null) {
					encoding = getDefaultEncoding();
				}
				if (encoding != null) {
					if (logger.isDebugEnabled()) {
						logger.debug("Loading properties [" + resource.getFilename() + "] with encoding '" + encoding + "'");
					}
					this.propertiesPersister.load(props, new InputStreamReader(is, encoding));
				}
				else {
					if (logger.isDebugEnabled()) {
						logger.debug("Loading properties [" + resource.getFilename() + "]");
					}
					this.propertiesPersister.load(props, is);
				}
			}
			return props;
		}
	}

	/**
	 * Template method for creating a plain new {@link Properties} instance.
	 * The default implementation simply calls {@link Properties#Properties()}.
	 * <p>Allows for returning a custom {@link Properties} extension in subclasses.
	 * Overriding methods should just instantiate a custom {@link Properties} subclass,
	 * with no further initialization or population to be performed at that point.
	 * @return a plain Properties instance
	 * @since 4.2
	 */
	// 用于创建一个普通的新 {@link Properties} 实例的模板方法。默认实现只是调用 {@link PropertiesProperties()}
	// <p>允许在子类中返回自定义的 {@link Properties} 扩展。
	// 覆盖方法应该只实例化一个自定义的 {@link Properties} 子类，此时不需要执行进一步的初始化或填充。
	protected Properties newProperties() {
		return new Properties();
	}


	/**
	 * Clear the resource bundle cache.
	 * Subsequent resolve calls will lead to reloading of the properties files.
	 */
	// 清除资源包缓存。随后的解析调用将导致重新加载属性文件。
	public void clearCache() {
		logger.debug("Clearing entire resource bundle cache");
		this.cachedProperties.clear();
		this.cachedMergedProperties.clear();
	}

	/**
	 * Clear the resource bundle caches of this MessageSource and all its ancestors.
	 * @see #clearCache
	 */
	// 清除此 MessageSource 及其所有祖先的资源包缓存
	public void clearCacheIncludingAncestors() {
		clearCache();
		if (getParentMessageSource() instanceof ReloadableResourceBundleMessageSource) {
			((ReloadableResourceBundleMessageSource) getParentMessageSource()).clearCacheIncludingAncestors();
		}
	}


	@Override
	public String toString() {
		return getClass().getName() + ": basenames=" + getBasenameSet();
	}


	/**
	 * PropertiesHolder for caching.
	 * Stores the last-modified timestamp of the source file for efficient
	 * change detection, and the timestamp of the last refresh attempt
	 * (updated every time the cache entry gets re-validated).
	 */
	// 用于缓存的 PropertiesHolder。
	// 存储源文件的最后修改时间戳以进行有效的更改检测，以及上次刷新尝试的时间戳（每次缓存条目重新验证时更新）。
	protected class PropertiesHolder {

		@Nullable
		private final Properties properties;

		private final long fileTimestamp;

		private volatile long refreshTimestamp = -2;

		private final ReentrantLock refreshLock = new ReentrantLock();

		/** Cache to hold already generated MessageFormats per message code. */
		// 缓存以保存每个消息代码已生成的 MessageFormats
		private final ConcurrentMap<String, Map<Locale, MessageFormat>> cachedMessageFormats =
				new ConcurrentHashMap<>();

		public PropertiesHolder() {
			this.properties = null;
			this.fileTimestamp = -1;
		}

		public PropertiesHolder(Properties properties, long fileTimestamp) {
			this.properties = properties;
			this.fileTimestamp = fileTimestamp;
		}

		@Nullable
		public Properties getProperties() {
			return this.properties;
		}

		public long getFileTimestamp() {
			return this.fileTimestamp;
		}

		public void setRefreshTimestamp(long refreshTimestamp) {
			this.refreshTimestamp = refreshTimestamp;
		}

		public long getRefreshTimestamp() {
			return this.refreshTimestamp;
		}

		@Nullable
		public String getProperty(String code) {
			if (this.properties == null) {
				return null;
			}
			return this.properties.getProperty(code);
		}

		@Nullable
		public MessageFormat getMessageFormat(String code, Locale locale) {
			if (this.properties == null) {
				return null;
			}
			Map<Locale, MessageFormat> localeMap = this.cachedMessageFormats.get(code);
			if (localeMap != null) {
				MessageFormat result = localeMap.get(locale);
				if (result != null) {
					return result;
				}
			}
			String msg = this.properties.getProperty(code);
			if (msg != null) {
				if (localeMap == null) {
					localeMap = new ConcurrentHashMap<>();
					Map<Locale, MessageFormat> existing = this.cachedMessageFormats.putIfAbsent(code, localeMap);
					if (existing != null) {
						localeMap = existing;
					}
				}
				MessageFormat result = createMessageFormat(msg, locale);
				localeMap.put(locale, result);
				return result;
			}
			return null;
		}
	}

}
