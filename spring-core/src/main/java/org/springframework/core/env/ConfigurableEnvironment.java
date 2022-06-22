/*
 * Copyright 2002-2018 the original author or authors.
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

import java.util.Map;

/**
 * Configuration interface to be implemented by most if not all {@link Environment} types.
 * Provides facilities for setting active and default profiles and manipulating underlying
 * property sources. Allows clients to set and validate required properties, customize the
 * conversion service and more through the {@link ConfigurablePropertyResolver}
 * superinterface.
 *
 * <h2>Manipulating property sources</h2>
 * <p>Property sources may be removed, reordered, or replaced; and additional
 * property sources may be added using the {@link MutablePropertySources}
 * instance returned from {@link #getPropertySources()}. The following examples
 * are against the {@link StandardEnvironment} implementation of
 * {@code ConfigurableEnvironment}, but are generally applicable to any implementation,
 * though particular default property sources may differ.
 *
 * <h4>Example: adding a new property source with highest search priority</h4>
 * <pre class="code">
 * ConfigurableEnvironment environment = new StandardEnvironment();
 * MutablePropertySources propertySources = environment.getPropertySources();
 * Map&lt;String, String&gt; myMap = new HashMap&lt;&gt;();
 * myMap.put("xyz", "myValue");
 * propertySources.addFirst(new MapPropertySource("MY_MAP", myMap));
 * </pre>
 *
 * <h4>Example: removing the default system properties property source</h4>
 * <pre class="code">
 * MutablePropertySources propertySources = environment.getPropertySources();
 * propertySources.remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME)
 * </pre>
 *
 * <h4>Example: mocking the system environment for testing purposes</h4>
 * <pre class="code">
 * MutablePropertySources propertySources = environment.getPropertySources();
 * MockPropertySource mockEnvVars = new MockPropertySource().withProperty("xyz", "myValue");
 * propertySources.replace(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, mockEnvVars);
 * </pre>
 *
 * When an {@link Environment} is being used by an {@code ApplicationContext}, it is
 * important that any such {@code PropertySource} manipulations be performed
 * <em>before</em> the context's {@link
 * org.springframework.context.support.AbstractApplicationContext#refresh() refresh()}
 * method is called. This ensures that all property sources are available during the
 * container bootstrap process, including use by {@linkplain
 * org.springframework.context.support.PropertySourcesPlaceholderConfigurer property
 * placeholder configurers}.
 *
 * @author Chris Beams
 * @since 3.1
 * @see StandardEnvironment
 * @see org.springframework.context.ConfigurableApplicationContext#getEnvironment
 */
// 大多数（如果不是全部）{@link Environment} 类型要实现的配置接口。提供用于设置活动和默认配置文件以及操作基础属性源的工具。
// 允许客户端通过 {@link ConfigurablePropertyResolver} 超级接口设置和验证所需的属性、自定义转换服务等。
//
// <h2>操作属性源<h2> <p>属性源可以被移除、重新排序或替换；并且可以使用从 {@link getPropertySources()}
// 返回的 {@link MutablePropertySources} 实例添加其他属性源。以下示例针对 {@code ConfigurableEnvironment} 的
// {@link StandardEnvironment} 实现，但通常适用于任何实现，尽管特定的默认属性源可能不同。
//
// 当一个 {@link Environment} 被一个 {@code ApplicationContext} 使用时，任何这样的 {@code PropertySource} 操作都
// 必须在上下文的 {@link org.springframework.context 之前执行。 support.AbstractApplicationContextrefresh()
// refresh()} 方法被调用。这确保在容器引导过程中所有属性源都可用，包括由
// {@linkplain org.springframework.context.support.PropertySourcesPlaceholderConfigurer 属性占位符配置器}使用。
//
// Spring 3.1 条件配置：API - ConfigurableEnvironment 注解 - @Profile
// 可写方法:addActiveProfile(String profile),setActiveProfiles(String... profiles),setDefaultProfiles(String... profiles)
// 可读方法:getActiveProfiles(),getDefaultProfiles()
// 匹配方法: acceptsProfiles(String... profiles),acceptsProfiles(Profiles profiles)
//
// 可以动态调整 Profile,以及 PropertySource
public interface ConfigurableEnvironment extends Environment, ConfigurablePropertyResolver {

	/**
	 * Specify the set of profiles active for this {@code Environment}. Profiles are
	 * evaluated during container bootstrap to determine whether bean definitions
	 * should be registered with the container.
	 * <p>Any existing active profiles will be replaced with the given arguments; call
	 * with zero arguments to clear the current set of active profiles. Use
	 * {@link #addActiveProfile} to add a profile while preserving the existing set.
	 * @throws IllegalArgumentException if any profile is null, empty or whitespace-only
	 * @see #addActiveProfile
	 * @see #setDefaultProfiles
	 * @see org.springframework.context.annotation.Profile
	 * @see AbstractEnvironment#ACTIVE_PROFILES_PROPERTY_NAME
	 */
	// 指定此 {@code Environment} 的活动配置文件集。在容器引导期间评估配置文件以确定是否应向容器注册 bean 定义。
	// <p>任何现有的活动配置文件都将被给定的参数替换；使用零参数调用以清除当前的活动配置文件集。
	// 使用 {@link addActiveProfile} 添加配置文件，同时保留现有集
	// 在 Spring Boot 中可以通过外部化配置脚本 --spring.profiles.active = even 添加到 Program argument 来激活profiles
	// 在 JVM 中可以通过 -D 参数添加到 VM options 虚拟机配置中 -Dspring.profiles.active=even 来指定 profile 环境
	void setActiveProfiles(String... profiles);

	/**
	 * Add a profile to the current set of active profiles.
	 * @throws IllegalArgumentException if the profile is null, empty or whitespace-only
	 * @see #setActiveProfiles
	 */
	// 将配置文件添加到当前的活动配置文件集
	void addActiveProfile(String profile);

	/**
	 * Specify the set of profiles to be made active by default if no other profiles
	 * are explicitly made active through {@link #setActiveProfiles}.
	 * @throws IllegalArgumentException if any profile is null, empty or whitespace-only
	 * @see AbstractEnvironment#DEFAULT_PROFILES_PROPERTY_NAME
	 */
	// 如果没有其他配置文件通过 {@link setActiveProfiles} 显式激活，则指定默认激活的配置文件集
	void setDefaultProfiles(String... profiles);

	/**
	 * Return the {@link PropertySources} for this {@code Environment} in mutable form,
	 * allowing for manipulation of the set of {@link PropertySource} objects that should
	 * be searched when resolving properties against this {@code Environment} object.
	 * The various {@link MutablePropertySources} methods such as
	 * {@link MutablePropertySources#addFirst addFirst},
	 * {@link MutablePropertySources#addLast addLast},
	 * {@link MutablePropertySources#addBefore addBefore} and
	 * {@link MutablePropertySources#addAfter addAfter} allow for fine-grained control
	 * over property source ordering. This is useful, for example, in ensuring that
	 * certain user-defined property sources have search precedence over default property
	 * sources such as the set of system properties or the set of system environment
	 * variables.
	 * @see AbstractEnvironment#customizePropertySources
	 */
	// 以可变形式返回此 {@code Environment} 的 {@link PropertySources}，允许操作在针对此 {@code Environment} 对象
	// 解析属性时应搜索的一组 {@link PropertySource} 对象。各种 {@link MutablePropertySources} 方法，
	// 例如 {@link MutablePropertySourcesaddFirst addFirst}、{@link MutablePropertySourcesaddLast
	// {@link MutablePropertySourcesaddBefore addBefore} 和 {@link MutablePropertySourcesaddAfter addAfter} 允许
	// 对属性源排序进行细粒度控制。例如，这在确保某些用户定义的属性源比默认属性源（例如系统属性集或系统环境变量集）
	// 具有搜索优先级时很有用。
	// 是 Environment 和 PropertySources 属性来源关联方法
	MutablePropertySources getPropertySources();

	/**
	 * Return the value of {@link System#getProperties()} if allowed by the current
	 * {@link SecurityManager}, otherwise return a map implementation that will attempt
	 * to access individual keys using calls to {@link System#getProperty(String)}.
	 * <p>Note that most {@code Environment} implementations will include this system
	 * properties map as a default {@link PropertySource} to be searched. Therefore, it is
	 * recommended that this method not be used directly unless bypassing other property
	 * sources is expressly intended.
	 * <p>Calls to {@link Map#get(Object)} on the Map returned will never throw
	 * {@link IllegalAccessException}; in cases where the SecurityManager forbids access
	 * to a property, {@code null} will be returned and an INFO-level log message will be
	 * issued noting the exception.
	 */
	// 如果当前 {@link SecurityManager} 允许，则返回 {@link SystemgetProperties()} 的值，否则返回一个映射实现，
	// 该实现将尝试使用对 {@link SystemgetProperty(String)} 的调用访问各个键。
	// <p>请注意，大多数 {@code Environment} 实现将包含此系统属性映射作为要搜索的默认 {@link PropertySource}。
	// 因此，建议不要直接使用此方法，除非明确打算绕过其他属性源。
	// <p>在返回的地图上调用 {@link Mapget(Object)} 永远不会抛出 {@link IllegalAccessException}；
	// 在 SecurityManager 禁止访问某个属性的情况下，将返回 {@code null} 并发出一条 INFO 级别的日志消息，指出异常。
	Map<String, Object> getSystemProperties();

	/**
	 * Return the value of {@link System#getenv()} if allowed by the current
	 * {@link SecurityManager}, otherwise return a map implementation that will attempt
	 * to access individual keys using calls to {@link System#getenv(String)}.
	 * <p>Note that most {@link Environment} implementations will include this system
	 * environment map as a default {@link PropertySource} to be searched. Therefore, it
	 * is recommended that this method not be used directly unless bypassing other
	 * property sources is expressly intended.
	 * <p>Calls to {@link Map#get(Object)} on the Map returned will never throw
	 * {@link IllegalAccessException}; in cases where the SecurityManager forbids access
	 * to a property, {@code null} will be returned and an INFO-level log message will be
	 * issued noting the exception.
	 */
	// 如果当前 {@link SecurityManager} 允许，则返回 {@link Systemgetenv()} 的值，否则返回一个映射实现，
	// 该实现将尝试使用对 {@link Systemgetenv(String)} 的调用访问各个键。
	// <p>请注意，大多数 {@link Environment} 实现将包含此系统环境映射作为要搜索的默认 {@link PropertySource}。
	// 因此，建议不要直接使用此方法，除非明确打算绕过其他属性源。
	// <p>在返回的Map上调用 {@link Mapget(Object)} 永远不会抛出 {@link IllegalAccessException}；
	// 在 SecurityManager 禁止访问某个属性的情况下，将返回 {@code null} 并发出一条 INFO 级别的日志消息，指出异常。
	Map<String, Object> getSystemEnvironment();

	/**
	 * Append the given parent environment's active profiles, default profiles and
	 * property sources to this (child) environment's respective collections of each.
	 * <p>For any identically-named {@code PropertySource} instance existing in both
	 * parent and child, the child instance is to be preserved and the parent instance
	 * discarded. This has the effect of allowing overriding of property sources by the
	 * child as well as avoiding redundant searches through common property source types,
	 * e.g. system environment and system properties.
	 * <p>Active and default profile names are also filtered for duplicates, to avoid
	 * confusion and redundant storage.
	 * <p>The parent environment remains unmodified in any case. Note that any changes to
	 * the parent environment occurring after the call to {@code merge} will not be
	 * reflected in the child. Therefore, care should be taken to configure parent
	 * property sources and profile information prior to calling {@code merge}.
	 * @param parent the environment to merge with
	 * @since 3.1.2
	 * @see org.springframework.context.support.AbstractApplicationContext#setParent
	 */
	// 将给定的父环境的活动配置文件、默认配置文件和属性源附加到此（子）环境各自的集合中。
	// <p>对于存在于父和子中的任何同名 {@code PropertySource} 实例，将保留子实例并丢弃父实例。
	// 这具有允许子项覆盖属性源以及避免通过常见属性源类型进行冗余搜索的效果，例如系统环境和系统属性。
	// <p>活动和默认配置文件名称也会过滤重复，以避免混淆和冗余存储。
	// <p>在任何情况下，父环境都保持不变。请注意，在调用 {@code merge} 之后发生的对父环境的任何更改都不会反映在子环境中。
	// 因此，在调用 {@code merge} 之前应注意配置父属性源和配置文件信息
	void merge(ConfigurableEnvironment parent);

}
