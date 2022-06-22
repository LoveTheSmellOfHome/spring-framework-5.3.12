/*
 * Copyright 2002-2020 the original author or authors.
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
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * {@link org.springframework.context.MessageSource} implementation that
 * accesses resource bundles using specified basenames. This class relies
 * on the underlying JDK's {@link java.util.ResourceBundle} implementation,
 * in combination with the JDK's standard message parsing provided by
 * {@link java.text.MessageFormat}.
 *
 * <p>This MessageSource caches both the accessed ResourceBundle instances and
 * the generated MessageFormats for each message. It also implements rendering of
 * no-arg messages without MessageFormat, as supported by the AbstractMessageSource
 * base class. The caching provided by this MessageSource is significantly faster
 * than the built-in caching of the {@code java.util.ResourceBundle} class.
 *
 * <p>The basenames follow {@link java.util.ResourceBundle} conventions: essentially,
 * a fully-qualified classpath location. If it doesn't contain a package qualifier
 * (such as {@code org.mypackage}), it will be resolved from the classpath root.
 * Note that the JDK's standard ResourceBundle treats dots as package separators:
 * This means that "test.theme" is effectively equivalent to "test/theme".
 *
 * <p>On the classpath, bundle resources will be read with the locally configured
 * {@link #setDefaultEncoding encoding}: by default, ISO-8859-1; consider switching
 * this to UTF-8, or to {@code null} for the platform default encoding. On the JDK 9+
 * module path where locally provided {@code ResourceBundle.Control} handles are not
 * supported, this MessageSource always falls back to {@link ResourceBundle#getBundle}
 * retrieval with the platform default encoding: UTF-8 with a ISO-8859-1 fallback on
 * JDK 9+ (configurable through the "java.util.PropertyResourceBundle.encoding" system
 * property). Note that {@link #loadBundle(Reader)}/{@link #loadBundle(InputStream)}
 * won't be called in this case either, effectively ignoring overrides in subclasses.
 * Consider implementing a JDK 9 {@code java.util.spi.ResourceBundleProvider} instead.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Qimiao Chen
 * @see #setBasenames
 * @see ReloadableResourceBundleMessageSource
 * @see java.util.ResourceBundle
 * @see java.text.MessageFormat
 */
// {@link org.springframework.context.MessageSource} 实现，使用指定的基本名称访问资源包。
// 此类依赖于底层 JDK 的 {@link java.util.ResourceBundle} 实现，并结合 {@link java.text.MessageFormat} 提供的
// JDK 标准消息解析。
//
// <p>这个 MessageSource 缓存了访问的 ResourceBundle 实例和为每条消息生成的 MessageFormats。
// 它还实现了在没有 MessageFormat 的情况下呈现无参数消息，如 AbstractMessageSource 基类所支持的那样。
// 此 MessageSource 提供的缓存明显快于 {@code java.util.ResourceBundle} 类的内置缓存。
//
// <p>基本名称遵循 {@link java.util.ResourceBundle} 约定：本质上是一个完全限定的类路径位置。
// 如果它不包含包限定符（例如 {@code org.mypackage}），它将从类路径根目录解析。
// 请注意，JDK 的标准 ResourceBundle 将点视为包分隔符：这意味着“test.theme”实际上等同于“test/theme”。
//
// <p>在类路径上，将使用本地配置的 {@link setDefaultEncoding encoding} 读取包资源：默认为 ISO-8859-1；
// 考虑将其切换为 UTF-8，或为平台默认编码切换为 {@code null}。在不支持本地提供的 {@code ResourceBundle.Control}
// 句柄的 JDK 9+ 模块路径上，此 MessageSource 始终回退到具有平台默认编码的 {@link ResourceBundlegetBundle}
// 检索：UTF-8 和 ISO-8859-1 JDK 9+ 的回退（可通过“java.util.PropertyResourceBundle.encoding”系统属性进行配置）。
// 请注意，在这种情况下也不会调用 {@link loadBundle(Reader)}{@link loadBundle(InputStream)}，
// 从而有效地忽略子类中的覆盖。考虑改为实现 JDK 9 {@code java.util.spi.ResourceBundleProvider}。
public class ResourceBundleMessageSource extends AbstractResourceBasedMessageSource implements BeanClassLoaderAware {

	@Nullable
	private ClassLoader bundleClassLoader;

	@Nullable
	private ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();

	/**
	 * Cache to hold loaded ResourceBundles.
	 * This Map is keyed with the bundle basename, which holds a Map that is
	 * keyed with the Locale and in turn holds the ResourceBundle instances.
	 * This allows for very efficient hash lookups, significantly faster
	 * than the ResourceBundle class's own cache.
	 */
	// 缓存以保存加载的 ResourceBundles。这个 Map 以 bundle basename 为键，
	// 它包含一个以 Locale 为键的 Map，进而包含 ResourceBundle 实例。这允许非常有效的哈希查找，
	// 比 ResourceBundle 类自己的缓存快得多。
	private final Map<String, Map<Locale, ResourceBundle>> cachedResourceBundles =
			new ConcurrentHashMap<>();

	/**
	 * Cache to hold already generated MessageFormats.
	 * This Map is keyed with the ResourceBundle, which holds a Map that is
	 * keyed with the message code, which in turn holds a Map that is keyed
	 * with the Locale and holds the MessageFormat values. This allows for
	 * very efficient hash lookups without concatenated keys.
	 * @see #getMessageFormat
	 */
	// 缓存以保存已生成的 MessageFormats。此 Map 以 ResourceBundle 为键，ResourceBundle 保存一个以消息代码为键的 Map，
	// 而后者又包含一个以 Locale 为键并保存 MessageFormat 值的 Map。这允许在没有连接键的情况下进行非常有效的哈希查找。
	private final Map<ResourceBundle, Map<String, Map<Locale, MessageFormat>>> cachedBundleMessageFormats =
			new ConcurrentHashMap<>();

	@Nullable
	private volatile MessageSourceControl control = new MessageSourceControl();


	public ResourceBundleMessageSource() {
		setDefaultEncoding("ISO-8859-1");
	}


	/**
	 * Set the ClassLoader to load resource bundles with.
	 * <p>Default is the containing BeanFactory's
	 * {@link org.springframework.beans.factory.BeanClassLoaderAware bean ClassLoader},
	 * or the default ClassLoader determined by
	 * {@link org.springframework.util.ClassUtils#getDefaultClassLoader()}
	 * if not running within a BeanFactory.
	 */
	// 设置 ClassLoader 以加载资源包。
	// <p>默认是包含 BeanFactory 的
	// {@link org.springframework.beans.factory.BeanClassLoaderAware bean ClassLoader}，
	// 或者如果不在 BeanFactory 中运行，则由
	// {@link org.springframework.util.ClassUtilsgetDefaultClassLoader()} 确定的默认 ClassLoader。
	public void setBundleClassLoader(ClassLoader classLoader) {
		this.bundleClassLoader = classLoader;
	}

	/**
	 * Return the ClassLoader to load resource bundles with.
	 * <p>Default is the containing BeanFactory's bean ClassLoader.
	 * @see #setBundleClassLoader
	 */
	// 返回 ClassLoader 以加载资源包。
	// <p>默认是包含 BeanFactory 的 bean ClassLoader
	@Nullable
	protected ClassLoader getBundleClassLoader() {
		return (this.bundleClassLoader != null ? this.bundleClassLoader : this.beanClassLoader);
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.beanClassLoader = classLoader;
	}


	/**
	 * Resolves the given message code as key in the registered resource bundles,
	 * returning the value found in the bundle as-is (without MessageFormat parsing).
	 */
	// 将给定的消息代码解析为注册资源包中的键，按原样返回在包中找到的值（没有 MessageFormat 解析）。
	@Override
	protected String resolveCodeWithoutArguments(String code, Locale locale) {
		Set<String> basenames = getBasenameSet();
		for (String basename : basenames) {
			ResourceBundle bundle = getResourceBundle(basename, locale);
			if (bundle != null) {
				String result = getStringOrNull(bundle, code);
				if (result != null) {
					return result;
				}
			}
		}
		return null;
	}

	/**
	 * Resolves the given message code as key in the registered resource bundles,
	 * using a cached MessageFormat instance per message code.
	 */
	// 使用每个消息代码缓存的 MessageFormat 实例将给定的消息代码解析为注册资源包中的键。
	@Override
	@Nullable
	protected MessageFormat resolveCode(String code, Locale locale) {
		// 有序的 LinkedHashSet
		Set<String> basenames = getBasenameSet();
		for (String basename : basenames) {
			// 从缓存中获取 ResourceBundle
			ResourceBundle bundle = getResourceBundle(basename, locale);
			if (bundle != null) {
				MessageFormat messageFormat = getMessageFormat(bundle, code, locale);
				if (messageFormat != null) {
					return messageFormat;
				}
			}
		}
		return null;
	}


	/**
	 * Return a ResourceBundle for the given basename and Locale,
	 * fetching already generated ResourceBundle from the cache.
	 * @param basename the basename of the ResourceBundle
	 * @param locale the Locale to find the ResourceBundle for
	 * @return the resulting ResourceBundle, or {@code null} if none
	 * found for the given basename and Locale
	 */
	// 为给定的 basename 和 Locale 返回一个 ResourceBundle，从缓存中获取已经生成的 ResourceBundle。
	// @param basename ResourceBundle 的基本名称
	// @param locale 用于查找 ResourceBundle 的 Locale
	// @return 生成的 ResourceBundle，如果没有找到给定的 basename 和 Locale，则返回 {@code null}
	@Nullable
	protected ResourceBundle getResourceBundle(String basename, Locale locale) {
		if (getCacheMillis() >= 0) {
			// Fresh ResourceBundle.getBundle call in order to let ResourceBundle
			// do its native caching, at the expense of more extensive lookup steps.
			// 新鲜的 ResourceBundle.getBundle 调用是为了让 ResourceBundle 执行其本机缓存，代价是更广泛的查找步骤。
			return doGetBundle(basename, locale);
		}
		else {
			// Cache forever: prefer locale cache over repeated getBundle calls.
			// 永远缓存：比重复的 getBundle 调用更喜欢区域设置缓存。
			Map<Locale, ResourceBundle> localeMap = this.cachedResourceBundles.get(basename);
			if (localeMap != null) {
				ResourceBundle bundle = localeMap.get(locale);
				if (bundle != null) {
					return bundle;
				}
			}
			try {
				ResourceBundle bundle = doGetBundle(basename, locale);
				if (localeMap == null) {
					localeMap = this.cachedResourceBundles.computeIfAbsent(basename, bn -> new ConcurrentHashMap<>());
				}
				localeMap.put(locale, bundle);
				return bundle;
			}
			catch (MissingResourceException ex) {
				if (logger.isWarnEnabled()) {
					logger.warn("ResourceBundle [" + basename + "] not found for MessageSource: " + ex.getMessage());
				}
				// Assume bundle not found
				// -> do NOT throw the exception to allow for checking parent message source.
				// 假设未找到包 -> 不要抛出异常以允许检查父消息源。
				return null;
			}
		}
	}

	/**
	 * Obtain the resource bundle for the given basename and Locale.
	 * @param basename the basename to look for
	 * @param locale the Locale to look for
	 * @return the corresponding ResourceBundle
	 * @throws MissingResourceException if no matching bundle could be found
	 * @see java.util.ResourceBundle#getBundle(String, Locale, ClassLoader)
	 * @see #getBundleClassLoader()
	 */
	// 获取给定 basename 和 Locale 的资源包。
	// @param basename 要查找的基本名称
	// @param locale 要查找的语言环境
	// @return 对应的ResourceBundle
	// 如果找不到匹配的包，@throws MissingResourceException
	protected ResourceBundle doGetBundle(String basename, Locale locale) throws MissingResourceException {
		ClassLoader classLoader = getBundleClassLoader();
		Assert.state(classLoader != null, "No bundle ClassLoader set");

		MessageSourceControl control = this.control;
		if (control != null) {
			try {
				return ResourceBundle.getBundle(basename, locale, classLoader, control);
			}
			catch (UnsupportedOperationException ex) {
				// Probably in a Jigsaw environment on JDK 9+
				// 可能在 JDK 9+ 的 Jigsaw 环境中
				this.control = null;
				String encoding = getDefaultEncoding();
				if (encoding != null && logger.isInfoEnabled()) {
					logger.info("ResourceBundleMessageSource is configured to read resources with encoding '" +
							encoding + "' but ResourceBundle.Control not supported in current system environment: " +
							ex.getMessage() + " - falling back to plain ResourceBundle.getBundle retrieval with the " +
							"platform default encoding. Consider setting the 'defaultEncoding' property to 'null' " +
							"for participating in the platform default and therefore avoiding this log message.");
				}
			}
		}

		// Fallback: plain getBundle lookup without Control handle
		// 回退：没有控制句柄的普通 getBundle 查找
		return ResourceBundle.getBundle(basename, locale, classLoader);
	}

	/**
	 * Load a property-based resource bundle from the given reader.
	 * <p>This will be called in case of a {@link #setDefaultEncoding "defaultEncoding"},
	 * including {@link ResourceBundleMessageSource}'s default ISO-8859-1 encoding.
	 * Note that this method can only be called with a {@code ResourceBundle.Control}:
	 * When running on the JDK 9+ module path where such control handles are not
	 * supported, any overrides in custom subclasses will effectively get ignored.
	 * <p>The default implementation returns a {@link PropertyResourceBundle}.
	 * @param reader the reader for the target resource
	 * @return the fully loaded bundle
	 * @throws IOException in case of I/O failure
	 * @since 4.2
	 * @see #loadBundle(InputStream)
	 * @see PropertyResourceBundle#PropertyResourceBundle(Reader)
	 */
	// 从给定的读取器加载基于属性的资源包。
	// <p>这将在 {@link setDefaultEncoding "defaultEncoding"} 的情况下调用，包括 {@link ResourceBundleMessageSource}
	// 的默认 ISO-8859-1 编码。请注意，此方法只能使用 {@code ResourceBundle.Control} 调用：
	// 在不支持此类控制句柄的 JDK 9+ 模块路径上运行时，自定义子类中的任何覆盖都将被有效地忽略。
	// <p>默认实现返回一个 {@link PropertyResourceBundle}。
	protected ResourceBundle loadBundle(Reader reader) throws IOException {
		return new PropertyResourceBundle(reader);
	}

	/**
	 * Load a property-based resource bundle from the given input stream,
	 * picking up the default properties encoding on JDK 9+.
	 * <p>This will only be called with {@link #setDefaultEncoding "defaultEncoding"}
	 * set to {@code null}, explicitly enforcing the platform default encoding
	 * (which is UTF-8 with a ISO-8859-1 fallback on JDK 9+ but configurable
	 * through the "java.util.PropertyResourceBundle.encoding" system property).
	 * Note that this method can only be called with a {@code ResourceBundle.Control}:
	 * When running on the JDK 9+ module path where such control handles are not
	 * supported, any overrides in custom subclasses will effectively get ignored.
	 * <p>The default implementation returns a {@link PropertyResourceBundle}.
	 * @param inputStream the input stream for the target resource
	 * @return the fully loaded bundle
	 * @throws IOException in case of I/O failure
	 * @since 5.1
	 * @see #loadBundle(Reader)
	 * @see PropertyResourceBundle#PropertyResourceBundle(InputStream)
	 */
	// 从给定的输入流加载基于属性的资源包，选择 JDK 9+ 上的默认属性编码。
	// <p>这只会在 {@link setDefaultEncoding "defaultEncoding"} 设置为 {@code null} 的情况下调用，
	// 明确强制执行平台默认编码（它是 UTF-8，在 JDK 9+ 上具有 ISO-8859-1 回退，
	// 但是可通过“java.util.PropertyResourceBundle.encoding”系统属性进行配置）。
	// 请注意，此方法只能使用 {@code ResourceBundle.Control} 调用：在不支持此类控制句柄的 JDK 9+ 模块路径上运行时，
	// 自定义子类中的任何覆盖都将被有效地忽略。
	// <p>默认实现返回一个 {@link PropertyResourceBundle}。
	protected ResourceBundle loadBundle(InputStream inputStream) throws IOException {
		return new PropertyResourceBundle(inputStream);
	}

	/**
	 * Return a MessageFormat for the given bundle and code,
	 * fetching already generated MessageFormats from the cache.
	 * @param bundle the ResourceBundle to work on
	 * @param code the message code to retrieve
	 * @param locale the Locale to use to build the MessageFormat
	 * @return the resulting MessageFormat, or {@code null} if no message
	 * defined for the given code
	 * @throws MissingResourceException if thrown by the ResourceBundle
	 */
	// 返回给定包和代码的 MessageFormat，从缓存中获取已生成的 MessageFormat
	// @param bundle 要处理的 ResourceBundle
	// @param code 要检索的消息代码,文案模板编码
	// @param locale 用于构建 MessageFormat 的 Locale
	// @return 生成的 MessageFormat，如果没有为给定代码定义消息，则为 {@code null}
	// @throws MissingResourceException 如果由 ResourceBundle 抛出
	@Nullable
	protected MessageFormat getMessageFormat(ResourceBundle bundle, String code, Locale locale)
			throws MissingResourceException {

		// 在只读的情况下 MessageFormat 是线程安全的，
		// 所谓线程不安全是指在相同数据结构中既有读操作又有写操作，造成了读写不一致的情况。当我们不小心将 MessageFormat 暴露出去
		// messageFormat.applyPattern(messageFormatPattern) 这个方法来进行重置。在多线程下是可以被别人修改的，
		// 如果不暴漏则是线程安全的
		Map<String, Map<Locale, MessageFormat>> codeMap = this.cachedBundleMessageFormats.get(bundle);
		Map<Locale, MessageFormat> localeMap = null;
		if (codeMap != null) {
			localeMap = codeMap.get(code);
			if (localeMap != null) {
				MessageFormat result = localeMap.get(locale);
				if (result != null) {
					return result;
				}
			}
		}

		String msg = getStringOrNull(bundle, code);
		if (msg != null) {
			if (codeMap == null) {
				codeMap = this.cachedBundleMessageFormats.computeIfAbsent(bundle, b -> new ConcurrentHashMap<>());
			}
			if (localeMap == null) {
				localeMap = codeMap.computeIfAbsent(code, c -> new ConcurrentHashMap<>());
			}
			// 当 MessageFormat 不存在时候，创建一个 直接 new 一个新的
			MessageFormat result = createMessageFormat(msg, locale);
			localeMap.put(locale, result);
			return result;
		}

		return null;
	}

	/**
	 * Efficiently retrieve the String value for the specified key,
	 * or return {@code null} if not found.
	 * <p>As of 4.2, the default implementation checks {@code containsKey}
	 * before it attempts to call {@code getString} (which would require
	 * catching {@code MissingResourceException} for key not found).
	 * <p>Can be overridden in subclasses.
	 * @param bundle the ResourceBundle to perform the lookup in
	 * @param key the key to look up
	 * @return the associated value, or {@code null} if none
	 * @since 4.2
	 * @see ResourceBundle#getString(String)
	 * @see ResourceBundle#containsKey(String)
	 */
	// 有效地检索指定键的字符串值，如果未找到则返回 {@code null}。
	// <p>从 4.2 开始，默认实现会在尝试调用 {@code getString} 之前检查 {@code containsKey}
	//（这将需要捕获 {@code MissingResourceException} 以获取未找到的键）。
	// <p>可以在子类中覆盖。
	@Nullable
	protected String getStringOrNull(ResourceBundle bundle, String key) {
		if (bundle.containsKey(key)) {
			try {
				return bundle.getString(key);
			}
			catch (MissingResourceException ex) {
				// Assume key not found for some other reason
				// -> do NOT throw the exception to allow for checking parent message source.
				// 假设由于某些其他原因找不到 key -> 不要抛出异常以允许检查父消息源。
			}
		}
		return null;
	}

	/**
	 * Show the configuration of this MessageSource.
	 */
	// 显示此 MessageSource 的配置。
	@Override
	public String toString() {
		return getClass().getName() + ": basenames=" + getBasenameSet();
	}


	/**
	 * Custom implementation of {@code ResourceBundle.Control}, adding support
	 * for custom file encodings, deactivating the fallback to the system locale
	 * and activating ResourceBundle's native cache, if desired.
	 */
	// {@code ResourceBundle.Control} 的自定义实现，添加对自定义文件编码的支持，
	// 停用对系统区域设置的回退，并在需要时激活 ResourceBundle 的本机缓存
	private class MessageSourceControl extends ResourceBundle.Control {

		@Override
		@Nullable
		public ResourceBundle newBundle(String baseName, Locale locale, String format, ClassLoader loader, boolean reload)
				throws IllegalAccessException, InstantiationException, IOException {

			// Special handling of default encoding
			// 默认编码的特殊处理
			if (format.equals("java.properties")) {
				String bundleName = toBundleName(baseName, locale);
				final String resourceName = toResourceName(bundleName, "properties");
				final ClassLoader classLoader = loader;
				final boolean reloadFlag = reload;
				InputStream inputStream;
				try {
					inputStream = AccessController.doPrivileged((PrivilegedExceptionAction<InputStream>) () -> {
						InputStream is = null;
						if (reloadFlag) {
							URL url = classLoader.getResource(resourceName);
							if (url != null) {
								URLConnection connection = url.openConnection();
								if (connection != null) {
									connection.setUseCaches(false);
									is = connection.getInputStream();
								}
							}
						}
						else {
							is = classLoader.getResourceAsStream(resourceName);
						}
						return is;
					});
				}
				catch (PrivilegedActionException ex) {
					throw (IOException) ex.getException();
				}
				if (inputStream != null) {
					String encoding = getDefaultEncoding();
					if (encoding != null) {
						try (InputStreamReader bundleReader = new InputStreamReader(inputStream, encoding)) {
							return loadBundle(bundleReader);
						}
					}
					else {
						try (InputStream bundleStream = inputStream) {
							return loadBundle(bundleStream);
						}
					}
				}
				else {
					return null;
				}
			}
			else {
				// Delegate handling of "java.class" format to standard Control
				// 将“java.class”格式的处理委托给标准控件
				return super.newBundle(baseName, locale, format, loader, reload);
			}
		}

		@Override
		@Nullable
		public Locale getFallbackLocale(String baseName, Locale locale) {
			Locale defaultLocale = getDefaultLocale();
			return (defaultLocale != null && !defaultLocale.equals(locale) ? defaultLocale : null);
		}

		@Override
		public long getTimeToLive(String baseName, Locale locale) {
			long cacheMillis = getCacheMillis();
			return (cacheMillis >= 0 ? cacheMillis : super.getTimeToLive(baseName, locale));
		}

		@Override
		public boolean needsReload(
				String baseName, Locale locale, String format, ClassLoader loader, ResourceBundle bundle, long loadTime) {

			if (super.needsReload(baseName, locale, format, loader, bundle, loadTime)) {
				cachedBundleMessageFormats.remove(bundle);
				return true;
			}
			else {
				return false;
			}
		}
	}

}
