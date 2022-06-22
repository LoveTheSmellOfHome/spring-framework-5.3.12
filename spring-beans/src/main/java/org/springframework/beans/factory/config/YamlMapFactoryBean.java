/*
 * Copyright 2002-2019 the original author or authors.
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

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.lang.Nullable;

/**
 * Factory for a {@code Map} that reads from a YAML source, preserving the
 * YAML-declared value types and their structure.
 *
 * <p>YAML is a nice human-readable format for configuration, and it has some
 * useful hierarchical properties. It's more or less a superset of JSON, so it
 * has a lot of similar features.
 *
 * <p>If multiple resources are provided the later ones will override entries in
 * the earlier ones hierarchically; that is, all entries with the same nested key
 * of type {@code Map} at any depth are merged. For example:
 *
 * <pre class="code">
 * foo:
 *   bar:
 *    one: two
 * three: four
 * </pre>
 *
 * plus (later in the list)
 *
 * <pre class="code">
 * foo:
 *   bar:
 *    one: 2
 * five: six
 * </pre>
 *
 * results in an effective input of
 *
 * <pre class="code">
 * foo:
 *   bar:
 *    one: 2
 * three: four
 * five: six
 * </pre>
 *
 * Note that the value of "foo" in the first document is not simply replaced
 * with the value in the second, but its nested values are merged.
 *
 * <p>Requires SnakeYAML 1.18 or higher, as of Spring Framework 5.0.6.
 *
 * @author Dave Syer
 * @author Juergen Hoeller
 * @since 4.1
 */
// 从 YAML 源读取的 {@code Map} 工厂，保留 YAML 声明的值类型及其结构
// <p>YAML 是一种很好的人类可读的配置格式，它有一些有用的层次属性。它或多或少是 JSON 的超集，因此它具有许多相似的功能。
// <p>如果提供了多个资源，后面的资源会分层覆盖前面的条目；也就是说，在任何深度具有相同嵌套键的 {@code Map} 类型的所有条目都被合并。例如：
//
// 请注意，第一个文档中“foo”的值不是简单地替换为第二个文档中的值，而是合并了其嵌套值。
// <p>从 Spring Framework 5.0.6 开始，需要 SnakeYAML 1.18 或更高版本
public class YamlMapFactoryBean extends YamlProcessor implements FactoryBean<Map<String, Object>>, InitializingBean {

	private boolean singleton = true;

	@Nullable
	private Map<String, Object> map;


	/**
	 * Set if a singleton should be created, or a new object on each request
	 * otherwise. Default is {@code true} (a singleton).
	 */
	// 设置是否应创建单例，否则为每个请求创建一个新对象。默认为 {@code true}（单例）
	public void setSingleton(boolean singleton) {
		this.singleton = singleton;
	}

	@Override
	public boolean isSingleton() {
		return this.singleton;
	}

	@Override
	public void afterPropertiesSet() {
		if (isSingleton()) {
			this.map = createMap();
		}
	}

	@Override
	@Nullable
	public Map<String, Object> getObject() {
		return (this.map != null ? this.map : createMap());
	}

	@Override
	public Class<?> getObjectType() {
		return Map.class;
	}


	/**
	 * Template method that subclasses may override to construct the object
	 * returned by this factory.
	 * <p>Invoked lazily the first time {@link #getObject()} is invoked in
	 * case of a shared singleton; else, on each {@link #getObject()} call.
	 * <p>The default implementation returns the merged {@code Map} instance.
	 * @return the object returned by this factory
	 * @see #process(MatchCallback)
	 */
	// 子类可以重写以构造此工厂返回的对象的模板方法。
	// <p>在共享单例的情况下第一次调用 {@link getObject()} 时延迟调用；否则，在每个 {@link getObject()} 调用中。
	// <p>默认实现返回合并后的 {@code Map} 实例。
	protected Map<String, Object> createMap() {
		Map<String, Object> result = new LinkedHashMap<>();
		process((properties, map) -> merge(result, map));
		return result;
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private void merge(Map<String, Object> output, Map<String, Object> map) {
		map.forEach((key, value) -> {
			Object existing = output.get(key);
			if (value instanceof Map && existing instanceof Map) {
				// Inner cast required by Eclipse IDE.
				Map<String, Object> result = new LinkedHashMap<>((Map<String, Object>) existing);
				merge(result, (Map) value);
				output.put(key, result);
			}
			else {
				output.put(key, value);
			}
		});
	}

}
