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

import java.util.Properties;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.CollectionFactory;
import org.springframework.lang.Nullable;

/**
 * Factory for {@link java.util.Properties} that reads from a YAML source,
 * exposing a flat structure of String property values.
 *
 * <p>YAML is a nice human-readable format for configuration, and it has some
 * useful hierarchical properties. It's more or less a superset of JSON, so it
 * has a lot of similar features.
 *
 * <p><b>Note: All exposed values are of type {@code String}</b> for access through
 * the common {@link Properties#getProperty} method (e.g. in configuration property
 * resolution through {@link PropertyResourceConfigurer#setProperties(Properties)}).
 * If this is not desirable, use {@link YamlMapFactoryBean} instead.
 *
 * <p>The Properties created by this factory have nested paths for hierarchical
 * objects, so for instance this YAML
 *
 * <pre class="code">
 * environments:
 *   dev:
 *     url: https://dev.bar.com
 *     name: Developer Setup
 *   prod:
 *     url: https://foo.bar.com
 *     name: My Cool App
 * </pre>
 *
 * is transformed into these properties:
 *
 * <pre class="code">
 * environments.dev.url=https://dev.bar.com
 * environments.dev.name=Developer Setup
 * environments.prod.url=https://foo.bar.com
 * environments.prod.name=My Cool App
 * </pre>
 *
 * Lists are split as property keys with <code>[]</code> dereferencers, for
 * example this YAML:
 *
 * <pre class="code">
 * servers:
 * - dev.bar.com
 * - foo.bar.com
 * </pre>
 *
 * becomes properties like this:
 *
 * <pre class="code">
 * servers[0]=dev.bar.com
 * servers[1]=foo.bar.com
 * </pre>
 *
 * <p>Requires SnakeYAML 1.18 or higher, as of Spring Framework 5.0.6.
 *
 * @author Dave Syer
 * @author Stephane Nicoll
 * @author Juergen Hoeller
 * @since 4.1
 */
// 从 YAML 源读取的 {@link java.util.Properties} 工厂，公开字符串属性值的平面结构。
// <p>YAML 是一种很好的人类可读的配置格式，它有一些有用的层次属性。它或多或少是 JSON 的超集，因此它具有许多相似的功能。
// <p><b>注意：所有公开的值都是 {@code String}<b> 类型，用于通过常见的 {@link PropertiesgetProperty}
// 方法访问（例如，通过 {@link PropertyResourceConfigurersetProperties(Properties)} 进行配置属性解析）。
// 如果这是不可取的，请改用 {@link YamlMapFactoryBean}。 <p>此工厂创建的属性具有分层对象的嵌套路径，例如此 YAML
public class YamlPropertiesFactoryBean extends YamlProcessor implements FactoryBean<Properties>, InitializingBean {

	private boolean singleton = true;

	@Nullable
	private Properties properties;


	/**
	 * Set if a singleton should be created, or a new object on each request
	 * otherwise. Default is {@code true} (a singleton).
	 */
	// 设置是否应创建单例，否则为每个请求创建一个新对象。默认为 {@code true}（单例）。
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
			this.properties = createProperties();
		}
	}

	@Override
	@Nullable
	public Properties getObject() {
		return (this.properties != null ? this.properties : createProperties());
	}

	@Override
	public Class<?> getObjectType() {
		return Properties.class;
	}


	/**
	 * Template method that subclasses may override to construct the object
	 * returned by this factory. The default implementation returns a
	 * properties with the content of all resources.
	 * <p>Invoked lazily the first time {@link #getObject()} is invoked in
	 * case of a shared singleton; else, on each {@link #getObject()} call.
	 * @return the object returned by this factory
	 * @see #process(MatchCallback)
	 */
	// 子类可以重写以构造此工厂返回的对象的模板方法。默认实现返回一个包含所有资源内容的属性
	// <p>在共享单例的情况下第一次调用 {@link getObject()} 时延迟调用；否则，在每个 {@link getObject()} 调用中。
	// @return 这个工厂返回的对象
	protected Properties createProperties() {
		Properties result = CollectionFactory.createStringAdaptingProperties();
		process((properties, map) -> result.putAll(properties));
		return result;
	}

}
