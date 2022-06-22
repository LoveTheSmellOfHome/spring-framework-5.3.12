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

/**
 * Interface representing the environment in which the current application is running.
 * Models two key aspects of the application environment: <em>profiles</em> and
 * <em>properties</em>. Methods related to property access are exposed via the
 * {@link PropertyResolver} superinterface.
 *
 * <p>A <em>profile</em> is a named, logical group of bean definitions to be registered
 * with the container only if the given profile is <em>active</em>. Beans may be assigned
 * to a profile whether defined in XML or via annotations; see the spring-beans 3.1 schema
 * or the {@link org.springframework.context.annotation.Profile @Profile} annotation for
 * syntax details. The role of the {@code Environment} object with relation to profiles is
 * in determining which profiles (if any) are currently {@linkplain #getActiveProfiles
 * active}, and which profiles (if any) should be {@linkplain #getDefaultProfiles active
 * by default}.
 *
 * <p><em>Properties</em> play an important role in almost all applications, and may
 * originate from a variety of sources: properties files, JVM system properties, system
 * environment variables, JNDI, servlet context parameters, ad-hoc Properties objects,
 * Maps, and so on. The role of the environment object with relation to properties is to
 * provide the user with a convenient service interface for configuring property sources
 * and resolving properties from them.
 *
 * <p>Beans managed within an {@code ApplicationContext} may register to be {@link
 * org.springframework.context.EnvironmentAware EnvironmentAware} or {@code @Inject} the
 * {@code Environment} in order to query profile state or resolve properties directly.
 *
 * <p>In most cases, however, application-level beans should not need to interact with the
 * {@code Environment} directly but instead may have to have {@code ${...}} property
 * values replaced by a property placeholder configurer such as
 * {@link org.springframework.context.support.PropertySourcesPlaceholderConfigurer
 * PropertySourcesPlaceholderConfigurer}, which itself is {@code EnvironmentAware} and
 * as of Spring 3.1 is registered by default when using
 * {@code <context:property-placeholder/>}.
 *
 * <p>Configuration of the environment object must be done through the
 * {@code ConfigurableEnvironment} interface, returned from all
 * {@code AbstractApplicationContext} subclass {@code getEnvironment()} methods. See
 * {@link ConfigurableEnvironment} Javadoc for usage examples demonstrating manipulation
 * of property sources prior to application context {@code refresh()}.
 *
 * @author Chris Beams
 * @since 3.1
 * @see PropertyResolver
 * @see EnvironmentCapable
 * @see ConfigurableEnvironment
 * @see AbstractEnvironment
 * @see StandardEnvironment
 * @see org.springframework.context.EnvironmentAware
 * @see org.springframework.context.ConfigurableApplicationContext#getEnvironment
 * @see org.springframework.context.ConfigurableApplicationContext#setEnvironment
 * @see org.springframework.context.support.AbstractApplicationContext#createEnvironment
 */
// 表示当前应用程序运行环境的接口。对应用程序环境的两个关键方面进行建模：<em>profiles<em> 和 <em>properties<em>。
// 与属性访问相关的方法通过 {@link PropertyResolver} 超接口公开。
//
// <p><em>profile<em> 是一个命名的、逻辑​​的 bean 定义组，仅当给定的配置文件是 <em>active<em> 时才向容器注册。
// Bean 可以分配给配置文件，无论是在 XML 中定义还是通过注释；有关语法详细信息，请参阅 spring-beans 3.1 架构或
// {@link org.springframework.context.annotation.Profile @Profile} 注释。
// 与配置文件相关的 {@code Environment} 对象的作用是确定哪些配置文件（如果有）当前是 {@linkplain getActiveProfiles active}，
// 哪些配置文件（如果有）应该是 {@linkplain getDefaultProfiles active by default}。
//
// <p><em>Properties<em>在几乎所有应用程序中都扮演着重要的角色，并且可能来源于多种来源：Properties文件、JVM 系统Properties、
// 系统环境变量、JNDI、servlet 上下文参数、ad-hoc 属性对象、地图等。与属性相关的环境对象的作用是为用户提供方便的服务接口，
// 用于配置属性源并从中解析属性。
//
// <p>在 {@code ApplicationContext} 中管理的 Bean 可以注册为
// {@link org.springframework.context.EnvironmentAware EnvironmentAware} 或 {@code @Inject} {@code Environment}，
// 以便直接地查询配置文件状态或解析属性
//
// p>然而，在大多数情况下，应用程序级 bean 不需要直接与 {@code Environment} 交互，而是可能必须将 {@code {...}} 属性值
// 替换为属性占位符配置器，例如作为
// {@link org.springframework.context.support.PropertySourcesPlaceholderConfigurer
// PropertySourcesPlaceholderConfigurer}，它本身是 {@code EnvironmentAware} 并且从 Spring 3.1 开始，
// 在使用 {@code <context:property-placeholder>} 时默认注册。
//
// <p>环境对象的配置必须通过 {@code ConfigurableEnvironment} 接口完成，从所有 {@code AbstractApplicationContext}
// 子类 {@code getEnvironment()} 方法返回。请参阅 {@link ConfigurableEnvironment} Javadoc 以获取演示在应用程序上下文
// {@code refresh()} 之前操作属性源的用法示例。
//
// Environment 对象隶属于 ApplicationContext,通过注册单例的方式把外部对象注册到 BeanFactory 中
// 所以我们可以同过依赖查找和依赖注入的方式能够找到 Environment 对象的原因.
// {@link AbstractApplicationContext}#prepareBeanFactory
//
// 职责：管理 Spring 配置属性源 PropertySource; 管理 Profiles
public interface Environment extends PropertyResolver {

	/**
	 * Return the set of profiles explicitly made active for this environment. Profiles
	 * are used for creating logical groupings of bean definitions to be registered
	 * conditionally, for example based on deployment environment. Profiles can be
	 * activated by setting {@linkplain AbstractEnvironment#ACTIVE_PROFILES_PROPERTY_NAME
	 * "spring.profiles.active"} as a system property or by calling
	 * {@link ConfigurableEnvironment#setActiveProfiles(String...)}.
	 * <p>If no profiles have explicitly been specified as active, then any
	 * {@linkplain #getDefaultProfiles() default profiles} will automatically be activated.
	 * @see #getDefaultProfiles
	 * @see ConfigurableEnvironment#setActiveProfiles
	 * @see AbstractEnvironment#ACTIVE_PROFILES_PROPERTY_NAME
	 */
	// 返回为此环境明确激活的配置文件集。配置文件用于创建有条件注册的 bean 定义的逻辑分组，例如基于部署环境。
	// 可以通过将 {@linkplain AbstractEnvironmentACTIVE_PROFILES_PROPERTY_NAME "spring.profiles.active"}
	// 设置为系统属性或调用 {@link ConfigurableEnvironmentsetActiveProfiles(String...)} 来激活配置文件。
	// <p>如果没有明确指定为活动的配置文件，则任何{@linkplain getDefaultProfiles() 默认配置文件}将自动被激活。
	String[] getActiveProfiles();

	/**
	 * Return the set of profiles to be active by default when no active profiles have
	 * been set explicitly.
	 * @see #getActiveProfiles
	 * @see ConfigurableEnvironment#setDefaultProfiles
	 * @see AbstractEnvironment#DEFAULT_PROFILES_PROPERTY_NAME
	 */
	// 当没有明确设置活动配置文件时，默认情况下返回要激活的配置文件集
	String[] getDefaultProfiles();

	/**
	 * Return whether one or more of the given profiles is active or, in the case of no
	 * explicit active profiles, whether one or more of the given profiles is included in
	 * the set of default profiles. If a profile begins with '!' the logic is inverted,
	 * i.e. the method will return {@code true} if the given profile is <em>not</em> active.
	 * For example, {@code env.acceptsProfiles("p1", "!p2")} will return {@code true} if
	 * profile 'p1' is active or 'p2' is not active.
	 * @throws IllegalArgumentException if called with zero arguments
	 * or if any profile is {@code null}, empty, or whitespace only
	 * @see #getActiveProfiles
	 * @see #getDefaultProfiles
	 * @see #acceptsProfiles(Profiles)
	 * @deprecated as of 5.1 in favor of {@link #acceptsProfiles(Profiles)}
	 */
	// 返回一个或多个给定的配置文件是否处于活动状态，或者在没有明确的活动配置文件的情况下，
	// 一个或多个给定的配置文件是否包含在一组默认配置文件中。如果配置文件以“！”开头逻辑是相反的，即如果给定的配置文件 <em>not<em> 活动，
	// 该方法将返回 {@code true}。例如，{@code env.acceptsProfiles("p1", "!p2")} 将返回 {@code true}
	// 如果配置文件 'p1' 处于活动状态或 'p2' 未处于活动状态。
	@Deprecated
	boolean acceptsProfiles(String... profiles);

	/**
	 * Return whether the {@linkplain #getActiveProfiles() active profiles}
	 * match the given {@link Profiles} predicate.
	 */
	// 返回 {@linkplain getActiveProfiles() 活动配置文件}是否与给定的 {@link Profiles} 谓词匹配
	boolean acceptsProfiles(Profiles profiles);

}
