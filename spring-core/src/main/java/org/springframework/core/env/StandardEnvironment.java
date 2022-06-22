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

package org.springframework.core.env;

/**
 * {@link Environment} implementation suitable for use in 'standard' (i.e. non-web)
 * applications.
 *
 * <p>In addition to the usual functions of a {@link ConfigurableEnvironment} such as
 * property resolution and profile-related operations, this implementation configures two
 * default property sources, to be searched in the following order:
 * <ul>
 * <li>{@linkplain AbstractEnvironment#getSystemProperties() system properties}
 * <li>{@linkplain AbstractEnvironment#getSystemEnvironment() system environment variables}
 * </ul>
 *
 * That is, if the key "xyz" is present both in the JVM system properties as well as in
 * the set of environment variables for the current process, the value of key "xyz" from
 * system properties will return from a call to {@code environment.getProperty("xyz")}.
 * This ordering is chosen by default because system properties are per-JVM, while
 * environment variables may be the same across many JVMs on a given system.  Giving
 * system properties precedence allows for overriding of environment variables on a
 * per-JVM basis.
 *
 * <p>These default property sources may be removed, reordered, or replaced; and
 * additional property sources may be added using the {@link MutablePropertySources}
 * instance available from {@link #getPropertySources()}. See
 * {@link ConfigurableEnvironment} Javadoc for usage examples.
 *
 * <p>See {@link SystemEnvironmentPropertySource} javadoc for details on special handling
 * of property names in shell environments (e.g. Bash) that disallow period characters in
 * variable names.
 *
 * @author Chris Beams
 * @author Phillip Webb
 * @since 3.1
 * @see ConfigurableEnvironment
 * @see SystemEnvironmentPropertySource
 * @see org.springframework.web.context.support.StandardServletEnvironment
 */
// {@link Environment} 实现适用于“标准”（即非网络）应用程序
//
// <p>除了 {@link ConfigurableEnvironment} 的常用功能（例如属性解析和配置文件相关操作）之外，此实现还配置了两个默认属性源，按以下顺序搜索：
// <ul>
// <li>{@ linkplain AbstractEnvironment#getSystemProperties() 系统属性}
// <li>{@linkplain AbstractEnvironment#getSystemEnvironment() 系统环境变量}
// <ul>
//
// 也就是说，如果 JVM 系统属性以及当前进程的环境变量集中都存在 key “xyz”，则系统属性中的键“xyz”的值将从
// {@code environment.getProperty("xyz")}。默认情况下选择此顺序是因为系统属性是针对每个 JVM 的，
// 而给定系统上的许多 JVM 中的环境变量可能相同。赋予系统属性优先权允许在每个 JVM 的基础上覆盖环境变量。
//
// <p>请参阅 {@link SystemEnvironmentPropertySource} javadoc，了解有关在 shell 环境（例如 Bash）
// 中不允许在变量名称中使用句点字符的属性名称的特殊处理的详细信息。
public class StandardEnvironment extends AbstractEnvironment {

	/** System environment property source name: {@value}. */
	// 系统环境变量,优先级最高
	public static final String SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME = "systemEnvironment";

	/** JVM system properties property source name: {@value}. */
	// Java 的 Properties 属性，优先级次之
	public static final String SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME = "systemProperties";


	/**
	 * Create a new {@code StandardEnvironment} instance with a default
	 * {@link MutablePropertySources} instance.
	 */
	// 使用默认的 {@link MutablePropertySources} 实例创建一个新的 {@code StandardEnvironment} 实例。
	public StandardEnvironment() {
	}

	/**
	 * Create a new {@code StandardEnvironment} instance with a specific
	 * {@link MutablePropertySources} instance.
	 * @param propertySources property sources to use
	 * @since 5.3.4
	 */
	// 使用特定的 {@link MutablePropertySources} 实例创建一个新的 {@code StandardEnvironment} 实例。
	// @param propertySources 要使用的属性源
	protected StandardEnvironment(MutablePropertySources propertySources) {
		// propertySources 变化时候，propertyResolver =  new PropertySourcesPropertyResolver(propertySources);
		// 也会跟着变化
		super(propertySources);
	}


	/**
	 * Customize the set of property sources with those appropriate for any standard
	 * Java environment:
	 * <ul>
	 * <li>{@value #SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME}
	 * <li>{@value #SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME}
	 * </ul>
	 * <p>Properties present in {@value #SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME} will
	 * take precedence over those in {@value #SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME}.
	 * @see AbstractEnvironment#customizePropertySources(MutablePropertySources)
	 * @see #getSystemProperties()
	 * @see #getSystemEnvironment()
	 */
	// 使用适用于任何标准 Java 环境的属性源自定义一组属性源：
	// 。"systemProperties"
	// 。"systemEnvironment"
	// “systemProperties”中的属性优先于“systemEnvironment”中的属性
	@Override
	protected void customizePropertySources(MutablePropertySources propertySources) {
		propertySources.addLast(
				new PropertiesPropertySource(SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME, getSystemProperties()));
		propertySources.addLast(
				new SystemEnvironmentPropertySource(SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, getSystemEnvironment()));
	}

}
