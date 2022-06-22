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

package org.springframework.context;

import java.io.Closeable;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ProtocolResolver;
import org.springframework.core.metrics.ApplicationStartup;
import org.springframework.lang.Nullable;

/**
 * SPI interface to be implemented by most if not all application contexts.
 * Provides facilities to configure an application context in addition
 * to the application context client methods in the
 * {@link org.springframework.context.ApplicationContext} interface.
 *
 * <p>Configuration and lifecycle methods are encapsulated here to avoid
 * making them obvious to ApplicationContext client code. The present
 * methods should only be used by startup and shutdown code.
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author Sam Brannen
 * @since 03.11.2003
 */
// SPI 接口将由大多数（如果不是全部）应用程序上下文实现。 除了ApplicationContext接口中的
// ApplicationContext程序上下文客户端方法之外，还提供配置应用程序上下文的工具
// 配置和生命周期方法被封装在这里，以避免它们对 ApplicationContext 客户端代码显而易见。 目前的方法只能由启动和关闭代码使用。
public interface ConfigurableApplicationContext extends ApplicationContext, Lifecycle, Closeable {

	/**
	 * Any number of these characters are considered delimiters between
	 * multiple context config paths in a single String value.
	 * @see org.springframework.context.support.AbstractXmlApplicationContext#setConfigLocation
	 * @see org.springframework.web.context.ContextLoader#CONFIG_LOCATION_PARAM
	 * @see org.springframework.web.servlet.FrameworkServlet#setContextConfigLocation
	 */
	// 任何数量的这些字符都被视为单个字符串值中多个上下文配置路径之间的分隔符
	String CONFIG_LOCATION_DELIMITERS = ",; \t\n";

	/**
	 * Name of the ConversionService bean in the factory.
	 * If none is supplied, default conversion rules apply.
	 * @since 3.0
	 * @see org.springframework.core.convert.ConversionService
	 */
	// 工厂中 ConversionService bean 的名称。如果未提供，则应用默认转换规则
	String CONVERSION_SERVICE_BEAN_NAME = "conversionService";

	/**
	 * Name of the LoadTimeWeaver bean in the factory. If such a bean is supplied,
	 * the context will use a temporary ClassLoader for type matching, in order
	 * to allow the LoadTimeWeaver to process all actual bean classes.
	 * @since 2.5
	 * @see org.springframework.instrument.classloading.LoadTimeWeaver
	 */
	// 工厂中 LoadTimeWeaver bean 的名称。如果提供了这样的 bean，
	// 则上下文将使用临时 ClassLoader 进行类型匹配，以允许 LoadTimeWeaver 处理所有实际的 bean 类
	String LOAD_TIME_WEAVER_BEAN_NAME = "loadTimeWeaver";

	/**
	 * Name of the {@link Environment} bean in the factory.
	 * @since 3.1
	 */
	// 工厂中 Environment bean 的名称,直接依赖查找 Environment 名称
	String ENVIRONMENT_BEAN_NAME = "environment";

	/**
	 * Name of the System properties bean in the factory.
	 * @see java.lang.System#getProperties()
	 */
	// 工厂中系统属性 bean 的名称
	String SYSTEM_PROPERTIES_BEAN_NAME = "systemProperties";

	/**
	 * Name of the System environment bean in the factory.
	 * @see java.lang.System#getenv()
	 */
	// 工厂中系统环境 bean 的名称
	String SYSTEM_ENVIRONMENT_BEAN_NAME = "systemEnvironment";

	/**
	 * Name of the {@link ApplicationStartup} bean in the factory.
	 * @since 5.3
	 */
	// 工厂中ApplicationStartup bean 的名称
	String APPLICATION_STARTUP_BEAN_NAME = "applicationStartup";

	/**
	 * {@link Thread#getName() Name} of the {@linkplain #registerShutdownHook()
	 * shutdown hook} thread: {@value}.
	 * @since 5.2
	 * @see #registerShutdownHook()
	 */
	// 关闭钩子线程的Name ：“SpringContextShutdownHook”
	String SHUTDOWN_HOOK_THREAD_NAME = "SpringContextShutdownHook";


	/**
	 * Set the unique id of this application context.
	 * @since 3.0
	 */
	// 设置此应用程序上下文的唯一 ID
	void setId(String id);

	/**
	 * Set the parent of this application context.
	 * <p>Note that the parent shouldn't be changed: It should only be set outside
	 * a constructor if it isn't available when an object of this class is created,
	 * for example in case of WebApplicationContext setup.
	 * @param parent the parent context
	 * @see org.springframework.web.context.ConfigurableWebApplicationContext
	 */
	// 设置此应用程序上下文的父级。
	//请注意，不应更改父级：如果在创建此类的对象时它不可用，则仅应在构造函数之外设置它，例如在
	// WebApplicationContext 设置的情况下。
	void setParent(@Nullable ApplicationContext parent);

	/**
	 * Set the {@code Environment} for this application context.
	 * @param environment the new environment
	 * @since 3.1
	 */
	// 为这个应用程序上下文设置环境
	void setEnvironment(ConfigurableEnvironment environment);

	/**
	 * Return the {@code Environment} for this application context in configurable
	 * form, allowing for further customization.
	 * @since 3.1
	 */
	// 以可配置的形式返回此应用程序上下文的环境，允许进一步自定义，间接依赖查找 Environment
	@Override
	ConfigurableEnvironment getEnvironment();

	/**
	 * Set the {@link ApplicationStartup} for this application context.
	 * <p>This allows the application context to record metrics
	 * during startup.
	 * @param applicationStartup the new context event factory
	 * @since 5.3
	 */
	// 为此应用程序上下文设置 ApplicationStartup。这允许应用程序上下文在启动期间记录指标
	void setApplicationStartup(ApplicationStartup applicationStartup);

	/**
	 * Return the {@link ApplicationStartup} for this application context.
	 * @since 5.3
	 */
	ApplicationStartup getApplicationStartup();

	/**
	 * Add a new BeanFactoryPostProcessor that will get applied to the internal
	 * bean factory of this application context on refresh, before any of the
	 * bean definitions get evaluated. To be invoked during context configuration.
	 * @param postProcessor the factory processor to register
	 */
	// 添加一个新的 BeanFactoryPostProcessor，它将在刷新时应用于此应用程序上下文的内部 bean 工厂，
	// 然后再评估任何 bean 定义。在上下文配置期间调用。
	void addBeanFactoryPostProcessor(BeanFactoryPostProcessor postProcessor);

	/**
	 * Add a new ApplicationListener that will be notified on context events
	 * such as context refresh and context shutdown.
	 * <p>Note that any ApplicationListener registered here will be applied
	 * on refresh if the context is not active yet, or on the fly with the
	 * current event multicaster in case of a context that is already active.
	 * @param listener the ApplicationListener to register
	 * @see org.springframework.context.event.ContextRefreshedEvent
	 * @see org.springframework.context.event.ContextClosedEvent
	 */
	// 添加一个新的 ApplicationListener，它将在上下文事件（例如上下文刷新和上下文关闭）上收到通知
	// 请注意，如果上下文尚未激活，则此处注册的任何 ApplicationListener 将在刷新时应用，
	// 或者在上下文已经激活的情况下使用当前事件广播器动态应用。
	void addApplicationListener(ApplicationListener<?> listener);

	/**
	 * Specify the ClassLoader to load class path resources and bean classes with.
	 * <p>This context class loader will be passed to the internal bean factory.
	 * @since 5.2.7
	 * @see org.springframework.core.io.DefaultResourceLoader#DefaultResourceLoader(ClassLoader)
	 * @see org.springframework.beans.factory.config.ConfigurableBeanFactory#setBeanClassLoader
	 */
	// 指定用于加载类路径资源和 bean classes的 ClassLoader。此上下文类加载器将传递给内部 bean 工厂
	void setClassLoader(ClassLoader classLoader);

	/**
	 * Register the given protocol resolver with this application context,
	 * allowing for additional resource protocols to be handled.
	 * <p>Any such resolver will be invoked ahead of this context's standard
	 * resolution rules. It may therefore also override any default rules.
	 * @since 4.3
	 */
	// 使用此应用程序上下文注册给定的协议解析器，允许处理其他资源协议
	// 将在此上下文的标准解析规则之前调用任何此类解析器。因此，它也可以覆盖任何默认规则
	void addProtocolResolver(ProtocolResolver resolver);

	/**
	 * Load or refresh the persistent representation of the configuration, which
	 * might be from Java-based configuration, an XML file, a properties file, a
	 * relational database schema, or some other format.
	 * <p>As this is a startup method, it should destroy already created singletons
	 * if it fails, to avoid dangling resources. In other words, after invocation
	 * of this method, either all or no singletons at all should be instantiated.
	 * @throws BeansException if the bean factory could not be initialized
	 * @throws IllegalStateException if already initialized and multiple refresh
	 * attempts are not supported
	 */
	// 加载或刷新配置的持久表示，它可能来自基于 Java 的配置、XML 文件、属性文件、关系数据库模式或某种其他格式。
	// 由于这是一种启动方法，如果失败，它应该销毁已经创建的单例，以避免悬空资源。
	// 换句话说，在调用这个方法之后，要么全部实例化，要么根本不实例化。
	void refresh() throws BeansException, IllegalStateException;

	/**
	 * Register a shutdown hook with the JVM runtime, closing this context
	 * on JVM shutdown unless it has already been closed at that time.
	 * <p>This method can be called multiple times. Only one shutdown hook
	 * (at max) will be registered for each context instance.
	 * <p>As of Spring Framework 5.2, the {@linkplain Thread#getName() name} of
	 * the shutdown hook thread should be {@link #SHUTDOWN_HOOK_THREAD_NAME}.
	 * @see java.lang.Runtime#addShutdownHook
	 * @see #close()
	 */
	// 向 JVM 运行时注册一个关闭挂钩，在 JVM 关闭时关闭此上下文，除非当时它已经关闭
	// 这个方法可以被多次调用。每个上下文实例只会注册一个关闭挂钩（最多）
	// 从 Spring Framework 5.2 开始，关闭钩子线程的名称应该是SHUTDOWN_HOOK_THREAD_NAME
	void registerShutdownHook();

	/**
	 * Close this application context, releasing all resources and locks that the
	 * implementation might hold. This includes destroying all cached singleton beans.
	 * <p>Note: Does <i>not</i> invoke {@code close} on a parent context;
	 * parent contexts have their own, independent lifecycle.
	 * <p>This method can be called multiple times without side effects: Subsequent
	 * {@code close} calls on an already closed context will be ignored.
	 */
	// 关闭此应用程序上下文，释放实现可能持有的所有资源和锁。这包括销毁所有缓存的单例 bean
	// 注意：不会在父上下文上调用 close；父上下文有自己独立的生命周期。
	// 可以多次调用此方法而不会产生副作用：将忽略对已关闭上下文的后续close调用。
	@Override
	void close();

	/**
	 * Determine whether this application context is active, that is,
	 * whether it has been refreshed at least once and has not been closed yet.
	 * @return whether the context is still active
	 * @see #refresh()
	 * @see #close()
	 * @see #getBeanFactory()
	 */
	// 判断这个应用上下文是否处于活动状态，即是否至少刷新过一次并且还没有关闭。
	//返回值：上下文是否仍处于活动状态
	boolean isActive();

	/**
	 * Return the internal bean factory of this application context.
	 * Can be used to access specific functionality of the underlying factory.
	 * <p>Note: Do not use this to post-process the bean factory; singletons
	 * will already have been instantiated before. Use a BeanFactoryPostProcessor
	 * to intercept the BeanFactory setup process before beans get touched.
	 * <p>Generally, this internal factory will only be accessible while the context
	 * is active, that is, in-between {@link #refresh()} and {@link #close()}.
	 * The {@link #isActive()} flag can be used to check whether the context
	 * is in an appropriate state.
	 * @return the underlying bean factory
	 * @throws IllegalStateException if the context does not hold an internal
	 * bean factory (usually if {@link #refresh()} hasn't been called yet or
	 * if {@link #close()} has already been called)
	 * @see #isActive()
	 * @see #refresh()
	 * @see #close()
	 * @see #addBeanFactoryPostProcessor
	 */
	// 返回此应用程序上下文的内部 bean 工厂。可用于访问底层工厂的特定功能
	// 注意：不要用它来对 bean factory 进行后处理；单例之前已经被实例化了。
	// 使用 BeanFactoryPostProcessor 在 bean 被接触之前拦截 BeanFactory 设置过程
	// 通常，只有在上下文处于活动状态时，即在 refresh() 和 close() 之间，才能访问此内部工厂。
	// isActive() 标志可用于检查上下文是否处于适当的状态。
	// @Return 底层的工厂
	ConfigurableListableBeanFactory getBeanFactory() throws IllegalStateException;

}
