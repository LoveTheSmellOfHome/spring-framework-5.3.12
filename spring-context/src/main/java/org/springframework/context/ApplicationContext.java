/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.context;

import org.springframework.beans.factory.HierarchicalBeanFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.core.env.EnvironmentCapable;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.lang.Nullable;

/**
 * Central interface to provide configuration for an application.
 * This is read-only while the application is running, but may be
 * reloaded if the implementation supports this.
 *
 * <p>An ApplicationContext provides:
 * <ul>
 * <li>Bean factory methods for accessing application components.
 * Inherited from {@link org.springframework.beans.factory.ListableBeanFactory}.
 * <li>The ability to load file resources in a generic fashion.
 * Inherited from the {@link org.springframework.core.io.ResourceLoader} interface.
 * <li>The ability to publish events to registered listeners.
 * Inherited from the {@link ApplicationEventPublisher} interface.
 * <li>The ability to resolve messages, supporting internationalization.
 * Inherited from the {@link MessageSource} interface.
 * <li>Inheritance from a parent context. Definitions in a descendant context
 * will always take priority. This means, for example, that a single parent
 * context can be used by an entire web application, while each servlet has
 * its own child context that is independent of that of any other servlet.
 * </ul>
 *
 * <p>In addition to standard {@link org.springframework.beans.factory.BeanFactory}
 * lifecycle capabilities, ApplicationContext implementations detect and invoke
 * {@link ApplicationContextAware} beans as well as {@link ResourceLoaderAware},
 * {@link ApplicationEventPublisherAware} and {@link MessageSourceAware} beans.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see ConfigurableApplicationContext
 * @see org.springframework.beans.factory.BeanFactory
 * @see org.springframework.core.io.ResourceLoader
 */
// 为应用程序提供配置的中央接口。 这在应用程序运行时是只读的，但如果实现支持它可能会重新加载。
// ApplicationContext 提供：
// 1.用于访问应用程序组件的 Bean 工厂方法。 继承自ListableBeanFactory 。
// 2.以通用方式加载文件资源的能力。 继承自org.springframework.core.io.ResourceLoader接口。
// 3.能够将事件发布到注册的侦听器。 继承自ApplicationEventPublisher接口
// 4.解析消息的能力，支持国际化。继承自 MessageSource 接口
// 5.从父上下文继承。后代上下文中的定义将始终优先。这意味着，例如，单个父上下文可以被整个 Web 应用程序使用，
// 	 而每个 servlet 都有自己的子上下文，该子上下文独立于任何其他 servlet 的子上下文。
// 除了标准的 org.springframework.beans.factory.BeanFactory 生命周期功能之外，ApplicationContext 实现检测和
// 调用 ApplicationContextAware bean 以及 ResourceLoaderAware、ApplicationEventPublisherAware 和
// MessageSourceAware bean。
//
// 门面模式：面面俱到，BeanFactory，Environment...等等的实现
public interface ApplicationContext extends EnvironmentCapable, ListableBeanFactory, HierarchicalBeanFactory,
		MessageSource, ApplicationEventPublisher, ResourcePatternResolver {

	/**
	 * Return the unique id of this application context.
	 * @return the unique id of the context, or {@code null} if none
	 */
	// 返回此应用程序上下文的唯一 ID。
	// @return 上下文的唯一 id，或者 {@code null} 如果没有
	@Nullable
	String getId();

	/**
	 * Return a name for the deployed application that this context belongs to.
	 * @return a name for the deployed application, or the empty String by default
	 */
	// 返回此上下文所属的已部署应用程序的名称。
	// @return 已部署应用程序的名称，或默认为空字符串
	String getApplicationName();

	/**
	 * Return a friendly name for this context.
	 * @return a display name for this context (never {@code null})
	 */
	// 返回此上下文的友好名称。
	// @return 此上下文的显示名称（从不{@code null}）
	String getDisplayName();

	/**
	 * Return the timestamp when this context was first loaded.
	 * @return the timestamp (ms) when this context was first loaded
	 */
	// 返回首次加载此上下文时的时间戳。
	// @return 首次加载此上下文时的时间戳（毫秒）
	long getStartupDate();

	/**
	 * Return the parent context, or {@code null} if there is no parent
	 * and this is the root of the context hierarchy.
	 * @return the parent context, or {@code null} if there is no parent
	 */
	// 返回父上下文，如果没有父上下文并且这是上下文层次结构的根，则返回 {@code null}。
	// @return 父上下文，如果没有父上下文，或者 {@code null}
	// 层次性
	@Nullable
	ApplicationContext getParent();

	/**
	 * Expose AutowireCapableBeanFactory functionality for this context.
	 * <p>This is not typically used by application code, except for the purpose of
	 * initializing bean instances that live outside of the application context,
	 * applying the Spring bean lifecycle (fully or partly) to them.
	 * <p>Alternatively, the internal BeanFactory exposed by the
	 * {@link ConfigurableApplicationContext} interface offers access to the
	 * {@link AutowireCapableBeanFactory} interface too. The present method mainly
	 * serves as a convenient, specific facility on the ApplicationContext interface.
	 * <p><b>NOTE: As of 4.2, this method will consistently throw IllegalStateException
	 * after the application context has been closed.</b> In current Spring Framework
	 * versions, only refreshable application contexts behave that way; as of 4.2,
	 * all application context implementations will be required to comply.
	 * @return the AutowireCapableBeanFactory for this context
	 * @throws IllegalStateException if the context does not support the
	 * {@link AutowireCapableBeanFactory} interface, or does not hold an
	 * autowire-capable bean factory yet (e.g. if {@code refresh()} has
	 * never been called), or if the context has been closed already
	 * @see ConfigurableApplicationContext#refresh()
	 * @see ConfigurableApplicationContext#getBeanFactory()
	 */
	// 为此上下文公开 AutowireCapableBeanFactory 功能。
	// <p>这通常不被应用程序代码使用，除非用于初始化位于应用程序上下文之外的 bean 实例，
	// 将 Spring bean 生命周期（全部或部分）应用于它们。
	//
	// <p>或者，由 {@link ConfigurableApplicationContext} 接口公开的内部 BeanFactory 也提供对
	// {@link AutowireCapableBeanFactory} 接口的访问。本方法主要用作 ApplicationContext 接口上的一个方便的、特定的工具。
	//
	// <p><b>注意：从 4.2 开始，此方法将在应用程序上下文关闭后始终抛出 IllegalStateException。
	// <b> 在当前的 Spring Framework 版本中，只有可刷新的应用程序上下文才会这样做；
	// 从 4.2 开始，所有应用程序上下文实现都需要遵守。
	//
	// @return 此上下文的 AutowireCapableBeanFactory
	// @throws IllegalStateException 如果上下文不支持 {@link AutowireCapableBeanFactory} 接口，
	// 或者还没有拥有自动装配能力的 bean 工厂（例如，如果 {@code refresh()} 从未被调用过），或者如果上下文已经被调用已经关闭
	AutowireCapableBeanFactory getAutowireCapableBeanFactory() throws IllegalStateException;

}
