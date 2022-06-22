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

package org.springframework.beans.factory.config;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.CollectionFactory;
import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.reader.UnicodeReader;
import org.yaml.snakeyaml.representer.Representer;

import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Base class for YAML factories.
 *
 * <p>Requires SnakeYAML 1.18 or higher, as of Spring Framework 5.0.6.
 *
 * @author Dave Syer
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author Brian Clozel
 * @since 4.1
 */
// YAML 工厂的父类
public abstract class YamlProcessor {

	private final Log logger = LogFactory.getLog(getClass());

	private ResolutionMethod resolutionMethod = ResolutionMethod.OVERRIDE;

	private Resource[] resources = new Resource[0];

	private List<DocumentMatcher> documentMatchers = Collections.emptyList();

	private boolean matchDefault = true;

	private Set<String> supportedTypes = Collections.emptySet();


	/**
	 * A map of document matchers allowing callers to selectively use only
	 * some of the documents in a YAML resource. In YAML documents are
	 * separated by {@code ---} lines, and each document is converted
	 * to properties before the match is made. E.g.
	 * <pre class="code">
	 * environment: dev
	 * url: https://dev.bar.com
	 * name: Developer Setup
	 * ---
	 * environment: prod
	 * url:https://foo.bar.com
	 * name: My Cool App
	 * </pre>
	 * when mapped with
	 * <pre class="code">
	 * setDocumentMatchers(properties -&gt;
	 *     ("prod".equals(properties.getProperty("environment")) ? MatchStatus.FOUND : MatchStatus.NOT_FOUND));
	 * </pre>
	 * would end up as
	 * <pre class="code">
	 * environment=prod
	 * url=https://foo.bar.com
	 * name=My Cool App
	 * </pre>
	 */
	public void setDocumentMatchers(DocumentMatcher... matchers) {
		this.documentMatchers = Arrays.asList(matchers);
	}

	/**
	 * Flag indicating that a document for which all the
	 * {@link #setDocumentMatchers(DocumentMatcher...) document matchers} abstain will
	 * nevertheless match. Default is {@code true}.
	 */
	// 指示所有 {@link setDocumentMatchers(DocumentMatcher...) 文档匹配器}
	// 弃权的文档仍然匹配的标志。默认值为 {@code true}。
	public void setMatchDefault(boolean matchDefault) {
		this.matchDefault = matchDefault;
	}

	/**
	 * Method to use for resolving resources. Each resource will be converted to a Map,
	 * so this property is used to decide which map entries to keep in the final output
	 * from this factory. Default is {@link ResolutionMethod#OVERRIDE}.
	 */
	// 用于解析资源的方法。每个资源都将转换为 Map，因此此属性用于决定将哪些 Map 条目
	// 保留在该工厂的最终输出中。默认值为 {@link ResolutionMethod#OVERRIDE}。
	public void setResolutionMethod(ResolutionMethod resolutionMethod) {
		Assert.notNull(resolutionMethod, "ResolutionMethod must not be null");
		this.resolutionMethod = resolutionMethod;
	}

	/**
	 * Set locations of YAML {@link Resource resources} to be loaded.
	 * @see ResolutionMethod
	 */
	// 设置要加载的 YAML {@link Resource resources} 的位置。
	public void setResources(Resource... resources) {
		this.resources = resources;
	}

	/**
	 * Set the supported types that can be loaded from YAML documents.
	 * <p>If no supported types are configured, only Java standard classes
	 * (as defined in {@link org.yaml.snakeyaml.constructor.SafeConstructor})
	 * encountered in YAML documents will be supported.
	 * If an unsupported type is encountered, an {@link IllegalStateException}
	 * will be thrown when the corresponding YAML node is processed.
	 * @param supportedTypes the supported types, or an empty array to clear the
	 * supported types
	 * @since 5.1.16
	 * @see #createYaml()
	 */
	// 设置可以从 YAML 文档加载的支持类型。
	// <p>如果未配置支持的类型，则仅支持 YAML 文档中遇到的 Java
	// 标准类（如 {@link org.yaml.snakeyaml.constructor.SafeConstructor} 中所定义）。
	// 如果遇到不受支持的类型，则在处理相应的 YAML 节点时将抛出 {@link IllegalStateException}。
	// @param supportedTypes 支持的类型，或者一个空数组来清除支持的类型
	public void setSupportedTypes(Class<?>... supportedTypes) {
		if (ObjectUtils.isEmpty(supportedTypes)) {
			this.supportedTypes = Collections.emptySet();
		}
		else {
			Assert.noNullElements(supportedTypes, "'supportedTypes' must not contain null elements");
			this.supportedTypes = Arrays.stream(supportedTypes).map(Class::getName)
					.collect(Collectors.collectingAndThen(Collectors.toSet(), Collections::unmodifiableSet));
		}
	}

	/**
	 * Provide an opportunity for subclasses to process the Yaml parsed from the supplied
	 * resources. Each resource is parsed in turn and the documents inside checked against
	 * the {@link #setDocumentMatchers(DocumentMatcher...) matchers}. If a document
	 * matches it is passed into the callback, along with its representation as Properties.
	 * Depending on the {@link #setResolutionMethod(ResolutionMethod)} not all of the
	 * documents will be parsed.
	 * @param callback a callback to delegate to once matching documents are found
	 * @see #createYaml()
	 */
	// 为子类提供处理从提供的资源解析的 Yaml 的机会。依次解析每个资源，
	// 并根据 {@link setDocumentMatchers(DocumentMatcher...) 匹配器}检查内部文档。
	// 如果文档匹配，则将其连同其作为属性的表示一起传递到回调中。
	// 根据 {@link setResolutionMethod(ResolutionMethod)}，并非所有文档都会被解析。
	protected void process(MatchCallback callback) {
		Yaml yaml = createYaml();
		for (Resource resource : this.resources) {
			boolean found = process(callback, yaml, resource);
			if (this.resolutionMethod == ResolutionMethod.FIRST_FOUND && found) {
				return;
			}
		}
	}

	/**
	 * Create the {@link Yaml} instance to use.
	 * <p>The default implementation sets the "allowDuplicateKeys" flag to {@code false},
	 * enabling built-in duplicate key handling in SnakeYAML 1.18+.
	 * <p>As of Spring Framework 5.1.16, if custom {@linkplain #setSupportedTypes
	 * supported types} have been configured, the default implementation creates
	 * a {@code Yaml} instance that filters out unsupported types encountered in
	 * YAML documents. If an unsupported type is encountered, an
	 * {@link IllegalStateException} will be thrown when the node is processed.
	 * @see LoaderOptions#setAllowDuplicateKeys(boolean)
	 */
	// 创建要使用的 {@link Yaml} 实例。
	// <p>默认实现将“allowDuplicateKeys”标志设置为 {@code false}，从而在 SnakeYAML 1.18+ 中启用内置重复密钥处理。
	// <p>从 Spring Framework 5.1.16 开始，如果已配置自定义 {@linkplain setSupportedTypes 支持类型}，
	// 则默认实现会创建一个 {@code Yaml} 实例，用于过滤掉 YAML 文档中遇到的不受支持的类型。
	// 如果遇到不受支持的类型，则在处理节点时将抛出 {@link IllegalStateException}。
	protected Yaml createYaml() {
		LoaderOptions loaderOptions = new LoaderOptions();
		loaderOptions.setAllowDuplicateKeys(false);
		return new Yaml(new FilteringConstructor(loaderOptions), new Representer(),
				new DumperOptions(), loaderOptions);
	}

	private boolean process(MatchCallback callback, Yaml yaml, Resource resource) {
		int count = 0;
		try {
			if (logger.isDebugEnabled()) {
				logger.debug("Loading from YAML: " + resource);
			}
			try (Reader reader = new UnicodeReader(resource.getInputStream())) {
				for (Object object : yaml.loadAll(reader)) {
					if (object != null && process(asMap(object), callback)) {
						count++;
						if (this.resolutionMethod == ResolutionMethod.FIRST_FOUND) {
							break;
						}
					}
				}
				if (logger.isDebugEnabled()) {
					logger.debug("Loaded " + count + " document" + (count > 1 ? "s" : "") +
							" from YAML resource: " + resource);
				}
			}
		}
		catch (IOException ex) {
			handleProcessError(resource, ex);
		}
		return (count > 0);
	}

	private void handleProcessError(Resource resource, IOException ex) {
		if (this.resolutionMethod != ResolutionMethod.FIRST_FOUND &&
				this.resolutionMethod != ResolutionMethod.OVERRIDE_AND_IGNORE) {
			throw new IllegalStateException(ex);
		}
		if (logger.isWarnEnabled()) {
			logger.warn("Could not load map from " + resource + ": " + ex.getMessage());
		}
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> asMap(Object object) {
		// YAML can have numbers as keys
		// YAML 可以将数字作为键
		Map<String, Object> result = new LinkedHashMap<>();
		if (!(object instanceof Map)) {
			// A document can be a text literal
			// 文档可以是文本文字
			result.put("document", object);
			return result;
		}

		Map<Object, Object> map = (Map<Object, Object>) object;
		map.forEach((key, value) -> {
			if (value instanceof Map) {
				value = asMap(value);
			}
			if (key instanceof CharSequence) {
				result.put(key.toString(), value);
			}
			else {
				// It has to be a map key in this case
				// 在这种情况下它必须是 map 的键
				result.put("[" + key.toString() + "]", value);
			}
		});
		return result;
	}

	private boolean process(Map<String, Object> map, MatchCallback callback) {
		Properties properties = CollectionFactory.createStringAdaptingProperties();
		properties.putAll(getFlattenedMap(map));

		if (this.documentMatchers.isEmpty()) {
			if (logger.isDebugEnabled()) {
				logger.debug("Merging document (no matchers set): " + map);
			}
			callback.process(properties, map);
			return true;
		}

		MatchStatus result = MatchStatus.ABSTAIN;
		for (DocumentMatcher matcher : this.documentMatchers) {
			MatchStatus match = matcher.matches(properties);
			result = MatchStatus.getMostSpecific(match, result);
			if (match == MatchStatus.FOUND) {
				if (logger.isDebugEnabled()) {
					logger.debug("Matched document with document matcher: " + properties);
				}
				callback.process(properties, map);
				return true;
			}
		}

		if (result == MatchStatus.ABSTAIN && this.matchDefault) {
			if (logger.isDebugEnabled()) {
				logger.debug("Matched document with default matcher: " + map);
			}
			callback.process(properties, map);
			return true;
		}

		if (logger.isDebugEnabled()) {
			logger.debug("Unmatched document: " + map);
		}
		return false;
	}

	/**
	 * Return a flattened version of the given map, recursively following any nested Map
	 * or Collection values. Entries from the resulting map retain the same order as the
	 * source. When called with the Map from a {@link MatchCallback} the result will
	 * contain the same values as the {@link MatchCallback} Properties.
	 * @param source the source map
	 * @return a flattened map
	 * @since 4.1.3
	 */
	// 返回给定地图的扁平版本，递归地遵循任何嵌套的 Map 或 Collection 值。结果映射中的条目与源保持相同的顺序。
	// 当从 {@link MatchCallback} 使用 Map 调用时，结果将包含与 {@link MatchCallback} 属性相同的值。
	// @param source 源 map
	// @return 一个扁平化的 MAP
	protected final Map<String, Object> getFlattenedMap(Map<String, Object> source) {
		Map<String, Object> result = new LinkedHashMap<>();
		buildFlattenedMap(result, source, null);
		return result;
	}

	private void buildFlattenedMap(Map<String, Object> result, Map<String, Object> source, @Nullable String path) {
		source.forEach((key, value) -> {
			if (StringUtils.hasText(path)) {
				if (key.startsWith("[")) {
					key = path + key;
				}
				else {
					key = path + '.' + key;
				}
			}
			if (value instanceof String) {
				result.put(key, value);
			}
			else if (value instanceof Map) {
				// Need a compound key
				@SuppressWarnings("unchecked")
				Map<String, Object> map = (Map<String, Object>) value;
				buildFlattenedMap(result, map, key);
			}
			else if (value instanceof Collection) {
				// Need a compound key
				@SuppressWarnings("unchecked")
				Collection<Object> collection = (Collection<Object>) value;
				if (collection.isEmpty()) {
					result.put(key, "");
				}
				else {
					int count = 0;
					for (Object object : collection) {
						buildFlattenedMap(result, Collections.singletonMap(
								"[" + (count++) + "]", object), key);
					}
				}
			}
			else {
				result.put(key, (value != null ? value : ""));
			}
		});
	}


	/**
	 * Callback interface used to process the YAML parsing results.
	 */
	// 用于处理 YAML 解析结果的回调接口
	@FunctionalInterface
	public interface MatchCallback {

		/**
		 * Process the given representation of the parsing results.
		 * @param properties the properties to process (as a flattened
		 * representation with indexed keys in case of a collection or map)
		 * @param map the result map (preserving the original value structure
		 * in the YAML document)
		 */
		// 处理解析结果的给定展示
		// @param properties 要处理的属性（在集合或映射的情况下作为带有索引键的扁平表示）
		// @param 映射结果映射（保留 YAML 文档中的原始值结构）
		void process(Properties properties, Map<String, Object> map);
	}


	/**
	 * Strategy interface used to test if properties match.
	 */
	// 用于测试属性是否匹配的策略接口
	@FunctionalInterface
	public interface DocumentMatcher {

		/**
		 * Test if the given properties match.
		 * @param properties the properties to test
		 * @return the status of the match
		 */
		// 测试给定的属性是否匹配。
		// @param properties 要测试的属性
		// @return 匹配的状态
		MatchStatus matches(Properties properties);
	}


	/**
	 * Status returned from {@link DocumentMatcher#matches(java.util.Properties)}.
	 */
	// 从 {@link DocumentMatchermatches(java.util.Properties)} 返回的状态。
	public enum MatchStatus {

		/**
		 * A match was found.
		 */
		// 找到了匹配项
		FOUND,

		/**
		 * No match was found.
		 */
		// 未找到匹配项
		NOT_FOUND,

		/**
		 * The matcher should not be considered.
		 */
		// 不应该考虑匹配器。
		ABSTAIN;

		/**
		 * Compare two {@link MatchStatus} items, returning the most specific status.
		 */
		// 比较两个 {@link MatchStatus} 项目，返回最具体的状态
		public static MatchStatus getMostSpecific(MatchStatus a, MatchStatus b) {
			return (a.ordinal() < b.ordinal() ? a : b);
		}
	}


	/**
	 * Method to use for resolving resources.
	 */
	// 用于解析资源的方法
	public enum ResolutionMethod {

		/**
		 * Replace values from earlier in the list.
		 */
		// 替换列表中前面的值
		OVERRIDE,

		/**
		 * Replace values from earlier in the list, ignoring any failures.
		 */
		// 替换列表中较早的值，忽略任何失败
		OVERRIDE_AND_IGNORE,

		/**
		 * Take the first resource in the list that exists and use just that.
		 */
		// 获取列表中存在的第一个资源并使用它
		FIRST_FOUND
	}


	/**
	 * {@link Constructor} that supports filtering of unsupported types.
	 * <p>If an unsupported type is encountered in a YAML document, an
	 * {@link IllegalStateException} will be thrown from {@link #getClassForName}.
	 */
	// @link Constructor} 支持过滤不支持的类型。
	// <p>如果在 YAML 文档中遇到不支持的类型，则会从 {@link getClassForName} 抛出一个 {@link IllegalStateException}
	private class FilteringConstructor extends Constructor {

		FilteringConstructor(LoaderOptions loaderOptions) {
			super(loaderOptions);
		}

		@Override
		protected Class<?> getClassForName(String name) throws ClassNotFoundException {
			Assert.state(YamlProcessor.this.supportedTypes.contains(name),
					() -> "Unsupported type encountered in YAML document: " + name);
			return super.getClassForName(name);
		}
	}

}
