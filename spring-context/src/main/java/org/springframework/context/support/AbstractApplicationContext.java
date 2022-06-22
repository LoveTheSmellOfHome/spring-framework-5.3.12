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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.CachedIntrospectionResults;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.support.ResourceEditorRegistrar;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ApplicationStartupAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.HierarchicalMessageSource;
import org.springframework.context.LifecycleProcessor;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceAware;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.PayloadApplicationEvent;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.ContextStartedEvent;
import org.springframework.context.event.ContextStoppedEvent;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.context.expression.StandardBeanExpressionResolver;
import org.springframework.context.weaving.LoadTimeWeaverAware;
import org.springframework.context.weaving.LoadTimeWeaverAwareProcessor;
import org.springframework.core.NativeDetector;
import org.springframework.core.ResolvableType;
import org.springframework.core.SpringProperties;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.metrics.ApplicationStartup;
import org.springframework.core.metrics.StartupStep;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Abstract implementation of the {@link org.springframework.context.ApplicationContext}
 * interface. Doesn't mandate the type of storage used for configuration; simply
 * implements common context functionality. Uses the Template Method design pattern,
 * requiring concrete subclasses to implement abstract methods.
 *
 * <p>In contrast to a plain BeanFactory, an ApplicationContext is supposed
 * to detect special beans defined in its internal bean factory:
 * Therefore, this class automatically registers
 * {@link org.springframework.beans.factory.config.BeanFactoryPostProcessor BeanFactoryPostProcessors},
 * {@link org.springframework.beans.factory.config.BeanPostProcessor BeanPostProcessors},
 * and {@link org.springframework.context.ApplicationListener ApplicationListeners}
 * which are defined as beans in the context.
 *
 * <p>A {@link org.springframework.context.MessageSource} may also be supplied
 * as a bean in the context, with the name "messageSource"; otherwise, message
 * resolution is delegated to the parent context. Furthermore, a multicaster
 * for application events can be supplied as an "applicationEventMulticaster" bean
 * of type {@link org.springframework.context.event.ApplicationEventMulticaster}
 * in the context; otherwise, a default multicaster of type
 * {@link org.springframework.context.event.SimpleApplicationEventMulticaster} will be used.
 *
 * <p>Implements resource loading by extending
 * {@link org.springframework.core.io.DefaultResourceLoader}.
 * Consequently treats non-URL resource paths as class path resources
 * (supporting full class path resource names that include the package path,
 * e.g. "mypackage/myresource.dat"), unless the {@link #getResourceByPath}
 * method is overridden in a subclass.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @author Stephane Nicoll
 * @author Sam Brannen
 * @author Sebastien Deleuze
 * @author Brian Clozel
 * @since January 21, 2001
 * @see #refreshBeanFactory
 * @see #getBeanFactory
 * @see org.springframework.beans.factory.config.BeanFactoryPostProcessor
 * @see org.springframework.beans.factory.config.BeanPostProcessor
 * @see org.springframework.context.event.ApplicationEventMulticaster
 * @see org.springframework.context.ApplicationListener
 * @see org.springframework.context.MessageSource
 */
// {@link org.springframework.context.ApplicationContext} 接口的抽象实现。
// 不强制要求用于配置的存储类型；简单地实现常见的上下文功能。使用模板方法设计模式，需要具体的子类来实现抽象方法。
//
// <p>与普通的 BeanFactory 相比，ApplicationContext 应该检测在其内部 bean 工厂中定义的特殊 bean：
// 因此，此类自动注册 {@link org.springframework.beans.factory.config.BeanFactoryPostProcessor BeanFactoryPostProcessors}、
// {@link org.springframework.beans.factory.config.BeanPostProcessor BeanPostProcessors} 和
// {@link org.springframework.context.ApplicationListener ApplicationListeners}，它们在上下文中被定义为 bean。
//
// <p>{@link org.springframework.context.MessageSource} 也可以作为上下文中的 bean 提供，名称为“messageSource”；
// 否则，消息解析将委托给父上下文。此外，应用程序事件的广播器可以在上下文中作为
// {@link org.springframework.context.event.ApplicationEventMulticaster} 类型的
// “applicationEventMulticaster”bean 提供；否则，将使用
// {@link org.springframework.context.event.SimpleApplicationEventMulticaster} 类型的默认广播器。
//
// <p>通过扩展 {@link org.springframework.core.io.DefaultResourceLoader} 实现资源加载。
// 因此，将非 URL 资源路径视为类路径资源（支持包含包路径的完整类路径资源名称，例如“mypackage/myresource.dat”），除非 {@link getResourceByPath} 方法在子类中被覆盖。
public abstract class AbstractApplicationContext extends DefaultResourceLoader
		implements ConfigurableApplicationContext {

	/**
	 * Name of the MessageSource bean in the factory.
	 * If none is supplied, message resolution is delegated to the parent.
	 * @see MessageSource
	 */
	// 工厂中 MessageSource bean 的名称。 如果未提供，则将消息解析委托给父级
	public static final String MESSAGE_SOURCE_BEAN_NAME = "messageSource";

	/**
	 * Name of the LifecycleProcessor bean in the factory.
	 * If none is supplied, a DefaultLifecycleProcessor is used.
	 * @see org.springframework.context.LifecycleProcessor
	 * @see org.springframework.context.support.DefaultLifecycleProcessor
	 */
	// 工厂中 LifecycleProcessor bean 的名称。如果没有提供，则使用 DefaultLifecycleProcessor。
	public static final String LIFECYCLE_PROCESSOR_BEAN_NAME = "lifecycleProcessor";

	/**
	 * Name of the ApplicationEventMulticaster bean in the factory.
	 * If none is supplied, a default SimpleApplicationEventMulticaster is used.
	 * @see org.springframework.context.event.ApplicationEventMulticaster
	 * @see org.springframework.context.event.SimpleApplicationEventMulticaster
	 */
	// 工厂中 ApplicationEventMulticaster bean 的名称。如果没有提供，则使用默认的 SimpleApplicationEventMulticaster。
	public static final String APPLICATION_EVENT_MULTICASTER_BEAN_NAME = "applicationEventMulticaster";

	/**
	 * Boolean flag controlled by a {@code spring.spel.ignore} system property that instructs Spring to
	 * ignore SpEL, i.e. to not initialize the SpEL infrastructure.
	 * <p>The default is "false".
	 */
	// 由 {@code spring.spel.ignore} 系统属性控制的布尔标志，指示 Spring 忽略 SpEL，即不初始化 SpEL 基础结构。 <p>默认值为“假”
	private static final boolean shouldIgnoreSpel = SpringProperties.getFlag("spring.spel.ignore");


	static {
		// Eagerly load the ContextClosedEvent class to avoid weird classloader issues
		// on application shutdown in WebLogic 8.1. (Reported by Dustin Woods.)
		// 急切地加载 ContextClosedEvent 类以避免在 WebLogic 8.1 中关闭应用程序时
		// 出现奇怪的类加载器问题。 （达斯汀伍兹报道。）
		ContextClosedEvent.class.getName();
	}


	/** Logger used by this class. Available to subclasses. */
	protected final Log logger = LogFactory.getLog(getClass());

	/** Unique id for this context, if any. */
	// 此上下文的唯一 ID（如果有）
	private String id = ObjectUtils.identityToString(this);

	/** Display name. */
	// 显示名称
	private String displayName = ObjectUtils.identityToString(this);

	/** Parent context. */
	// 父上下文
	@Nullable
	private ApplicationContext parent;

	/** Environment used by this context. */
	// 此上下文使用的环境
	@Nullable
	private ConfigurableEnvironment environment;

	/** BeanFactoryPostProcessors to apply on refresh. */
	// BeanFactoryPostProcessors 在刷新时应用
	private final List<BeanFactoryPostProcessor> beanFactoryPostProcessors = new ArrayList<>();

	/** System time in milliseconds when this context started. */
	// 此上下文启动时的系统时间（以毫秒为单位）
	private long startupDate;

	/** Flag that indicates whether this context is currently active. */
	// 指示此上下文当前是否处于活动状态的标志
	private final AtomicBoolean active = new AtomicBoolean();

	/** Flag that indicates whether this context has been closed already. */
	// 指示当前上下文是否已关闭的标志，确保原子性
	private final AtomicBoolean closed = new AtomicBoolean();

	/** Synchronization monitor for the "refresh" and "destroy". */
	// “刷新”和“销毁”的同步监视器
	private final Object startupShutdownMonitor = new Object();

	/** Reference to the JVM shutdown hook, if registered. */
	// 对 JVM 关闭挂钩的引用（如果已注册）
	@Nullable
	private Thread shutdownHook;

	/** ResourcePatternResolver used by this context. */
	// 此上下文使用的 ResourcePatternResolver
	private ResourcePatternResolver resourcePatternResolver;

	/** LifecycleProcessor for managing the lifecycle of beans within this context. */
	// LifecycleProcessor 用于在此上下文中管理 bean 的生命周期。
	@Nullable
	private LifecycleProcessor lifecycleProcessor;

	/** MessageSource we delegate our implementation of this interface to. */
	// MessageSource 我们将MessageSource的实现委托给此接口
	@Nullable
	private MessageSource messageSource;

	/** Helper class used in event publishing. */
	// 事件发布中使用的帮助类
	@Nullable
	private ApplicationEventMulticaster applicationEventMulticaster;

	/** Application startup metrics. **/
	// 应用程序启动指标
	private ApplicationStartup applicationStartup = ApplicationStartup.DEFAULT;

	/** Statically specified listeners. */
	// 静态指定的侦听器
	private final Set<ApplicationListener<?>> applicationListeners = new LinkedHashSet<>();

	/** Local listeners registered before refresh. */
	// 刷新前注册的本地侦听器
	@Nullable
	private Set<ApplicationListener<?>> earlyApplicationListeners;

	/** ApplicationEvents published before the multicaster setup. */
	// 在广播器设置之前发布的 ApplicationEvents，Spring 4 开始支持，早期应用上下文事件，
	@Nullable
	private Set<ApplicationEvent> earlyApplicationEvents;


	/**
	 * Create a new AbstractApplicationContext with no parent.
	 */
	// 创建一个没有父级的新 AbstractApplicationContext
	public AbstractApplicationContext() {
		this.resourcePatternResolver = getResourcePatternResolver();
	}

	/**
	 * Create a new AbstractApplicationContext with the given parent context.
	 * @param parent the parent context
	 */
	// 使用给定的父上下文创建一个新的 AbstractApplicationContext
	public AbstractApplicationContext(@Nullable ApplicationContext parent) {
		this();
		setParent(parent);
	}


	//---------------------------------------------------------------------
	// Implementation of ApplicationContext interface
	// ApplicationContext 接口的实现
	//---------------------------------------------------------------------

	/**
	 * Set the unique id of this application context.
	 * <p>Default is the object id of the context instance, or the name
	 * of the context bean if the context is itself defined as a bean.
	 * @param id the unique id of the context
	 */
	// 设置此应用程序上下文的唯一 ID。 <p>默认是上下文实例的对象 id，如果上下文本身被定义为 bean，
	// 则为上下文 bean 的名称。 @param id 上下文的唯一 id
	@Override
	public void setId(String id) {
		this.id = id;
	}

	@Override
	public String getId() {
		return this.id;
	}

	@Override
	public String getApplicationName() {
		return "";
	}

	/**
	 * Set a friendly name for this context.
	 * Typically done during initialization of concrete context implementations.
	 * <p>Default is the object id of the context instance.
	 */
	// 为此上下文设置一个友好名称。通常在具体上下文实现的初始化期间完成。 <p>默认是上下文实例的对象 ID。
	public void setDisplayName(String displayName) {
		Assert.hasLength(displayName, "Display name must not be empty");
		this.displayName = displayName;
	}

	/**
	 * Return a friendly name for this context.
	 * @return a display name for this context (never {@code null})
	 */
	// 返回此上下文的友好名称。 @return 此上下文的显示名称（从不{@code null}）
	@Override
	public String getDisplayName() {
		return this.displayName;
	}

	/**
	 * Return the parent context, or {@code null} if there is no parent
	 * (that is, this context is the root of the context hierarchy).
	 */
	// 返回父上下文，如果没有父上下文（即，此上下文是上下文层次结构的根），则返回 {@code null}
	@Override
	@Nullable
	public ApplicationContext getParent() {
		return this.parent;
	}

	/**
	 * Set the {@code Environment} for this application context.
	 * <p>Default value is determined by {@link #createEnvironment()}. Replacing the
	 * default with this method is one option but configuration through {@link
	 * #getEnvironment()} should also be considered. In either case, such modifications
	 * should be performed <em>before</em> {@link #refresh()}.
	 * @see org.springframework.context.support.AbstractApplicationContext#createEnvironment
	 */
	// 为此应用程序上下文设置 {@code Environment}。 <p>默认值由 {@link createEnvironment()} 决定。使用此方法替换默认值是一种选择，
	// 但还应考虑通过 {@link getEnvironment()} 进行配置。在任何一种情况下，此类修改都应该在
	// <em>before<em> {@link refresh()} 之前执行。
	@Override
	public void setEnvironment(ConfigurableEnvironment environment) {
		this.environment = environment;
	}

	/**
	 * Return the {@code Environment} for this application context in configurable
	 * form, allowing for further customization.
	 * <p>If none specified, a default environment will be initialized via
	 */
	//以可配置的形式返回此应用程序上下文的 {@code Environment}，允许进一步自定义。
	// <p>如果没有指定，默认环境将通过{@link #createEnvironment()}.
	@Override
	public ConfigurableEnvironment getEnvironment() {
		if (this.environment == null) {
			// 在不同场景中实现不同，web 中 new StandardServletEnvironment();
			// 普通场景 return new StandardEnvironment();
			this.environment = createEnvironment();
		}
		return this.environment;
	}

	/**
	 * Create and return a new {@link StandardEnvironment}.
	 * <p>Subclasses may override this method in order to supply
	 * a custom {@link ConfigurableEnvironment} implementation.
	 */
	// 创建并返回一个新的 {@link StandardEnvironment}。
	// <p>子类可以重写此方法以提供自定义的 {@link ConfigurableEnvironment} 实现
	protected ConfigurableEnvironment createEnvironment() {
		return new StandardEnvironment();
	}

	/**
	 * Return this context's internal bean factory as AutowireCapableBeanFactory,
	 * if already available.
	 * @see #getBeanFactory()
	 */
	// 如果已经可用，则将此上下文的内建 bean 工厂返回为 AutowireCapableBeanFactory
	// @see #getBeanFactory()
	@Override
	public AutowireCapableBeanFactory getAutowireCapableBeanFactory() throws IllegalStateException {
		return getBeanFactory();
	}

	/**
	 * Return the timestamp (ms) when this context was first loaded.
	 */
	// 首次加载此上下文时返回时间戳（毫秒）
	@Override
	public long getStartupDate() {
		return this.startupDate;
	}

	/**
	 * Publish the given event to all listeners.
	 * <p>Note: Listeners get initialized after the MessageSource, to be able
	 * to access it within listener implementations. Thus, MessageSource
	 * implementations cannot publish events.
	 * @param event the event to publish (may be application-specific or a
	 * standard framework event)
	 */
	// 将给定的事件发布给所有侦听器。 <p>注意：监听器在 MessageSource 之后被初始化，以便能
	// 够在监听器实现中访问它。因此，MessageSource 实现无法发布事件。 @param event 要发布的事件（可能是特定于应用程序或标准框架事件）
	@Override
	public void publishEvent(ApplicationEvent event) {
		publishEvent(event, null);
	}

	/**
	 * Publish the given event to all listeners.
	 * <p>Note: Listeners get initialized after the MessageSource, to be able
	 * to access it within listener implementations. Thus, MessageSource
	 * implementations cannot publish events.
	 * @param event the event to publish (may be an {@link ApplicationEvent}
	 * or a payload object to be turned into a {@link PayloadApplicationEvent})
	 */
	// 将给定的事件发布给所有侦听器。 <p>注意：监听器在 MessageSource 之后被初始化，以便能够在监听器实现中访问它。因此，MessageSource
	// 实现无法发布事件。 @param event 要发布的事件（可能是 {@link ApplicationEvent} 或要转换
	// 为 {@link PayloadApplicationEvent} 的有效负载对象）
	@Override
	public void publishEvent(Object event) {
		publishEvent(event, null);
	}

	/**
	 * Publish the given event to all listeners.
	 * @param event the event to publish (may be an {@link ApplicationEvent}
	 * or a payload object to be turned into a {@link PayloadApplicationEvent})
	 * @param eventType the resolved event type, if known
	 * @since 4.2
	 */
	// 将给定的事件发布给所有监听器
	protected void publishEvent(Object event, @Nullable ResolvableType eventType) {
		Assert.notNull(event, "Event must not be null");

		// Decorate event as an ApplicationEvent if necessary
		// 如有必要，将事件装饰为 ApplicationEvent
		ApplicationEvent applicationEvent;
		if (event instanceof ApplicationEvent) {
			applicationEvent = (ApplicationEvent) event;
		}
		else {
			applicationEvent = new PayloadApplicationEvent<>(this, event);
			if (eventType == null) {
				eventType = ((PayloadApplicationEvent<?>) applicationEvent).getResolvableType();
			}
		}

		// Multicast right now if possible - or lazily once the multicaster is initialized
		// 如果可能，立即进行广播 - 或者在广播器初始化后懒加载
		// 懒加载，由于 ApplicationEventMulticaster 没被初始化，无法将事件广播，所以先用 Set<ApplicationEvent> 将
		// AbstractApplicationContext#registerBeanPostProcessors(beanFactory) 中调到的事件先缓存起来，到
		// AbstractApplicationContext#initApplicationEventMulticaster(); 完成后在
		// AbstractApplicationContext#registerListeners();中将这些事件回放（广播出去）。
		// Spring 3.0 会有此 Bug,在处理配置类中同时实现 ApplicationEventPublisherAware，BeanPostProcessor
		// registerBeanPostProcessors(beanFactory)的流程早于initApplicationEventMulticaster()，
		// 因此需要缓存待 ApplicationEventMulticaster 完成初始化后进行事件回放（将缓存事件广播出去）
		if (this.earlyApplicationEvents != null) {
			this.earlyApplicationEvents.add(applicationEvent);
		}
		else {
			// ApplicationEventPublisher  和 ApplicationEventMulticaster 进行关联，广播事件
			// 当前类作为二者的桥梁，内部委派 ApplicationEventMulticaster
			getApplicationEventMulticaster().multicastEvent(applicationEvent, eventType);
		}

		// Publish event via parent context as well...
		// 也通过父上下文发布事件，层次性事件处理,this.getId = "current-context" ,this.getParent().getId = "parent-contect"
		// 如果有父上下文，在父上下文中递归传播事件
		if (this.parent != null) {
			if (this.parent instanceof AbstractApplicationContext) {
				((AbstractApplicationContext) this.parent).publishEvent(event, eventType);
			}
			else {
				this.parent.publishEvent(event);
			}
		}
	}

	/**
	 * Return the internal ApplicationEventMulticaster used by the context.
	 * @return the internal ApplicationEventMulticaster (never {@code null})
	 * @throws IllegalStateException if the context has not been initialized yet
	 */
	// 返回上下文使用的内建 ApplicationEventMulticaster
	ApplicationEventMulticaster getApplicationEventMulticaster() throws IllegalStateException {
		// 应用程序上下文还没有调用 refresh()方法，此时 applicationEventMulticaster == null，抛出异常
		if (this.applicationEventMulticaster == null) {
			throw new IllegalStateException("ApplicationEventMulticaster not initialized - " +
					"call 'refresh' before multicasting events via the context: " + this);
		}
		return this.applicationEventMulticaster;
	}

	@Override
	public void setApplicationStartup(ApplicationStartup applicationStartup) {
		Assert.notNull(applicationStartup, "applicationStartup should not be null");
		this.applicationStartup = applicationStartup;
	}

	@Override
	public ApplicationStartup getApplicationStartup() {
		return this.applicationStartup;
	}

	/**
	 * Return the internal LifecycleProcessor used by the context.
	 * @return the internal LifecycleProcessor (never {@code null})
	 * @throws IllegalStateException if the context has not been initialized yet
	 */
	// 返回上下文使用的内部 LifecycleProcessor。 @return 内部 LifecycleProcessor（从不{@code null}）
	LifecycleProcessor getLifecycleProcessor() throws IllegalStateException {
		if (this.lifecycleProcessor == null) {
			throw new IllegalStateException("LifecycleProcessor not initialized - " +
					"call 'refresh' before invoking lifecycle methods via the context: " + this);
		}
		return this.lifecycleProcessor;
	}

	/**
	 * Return the ResourcePatternResolver to use for resolving location patterns
	 * into Resource instances. Default is a
	 * {@link org.springframework.core.io.support.PathMatchingResourcePatternResolver},
	 * supporting Ant-style location patterns.
	 * <p>Can be overridden in subclasses, for extended resolution strategies,
	 * for example in a web environment.
	 * <p><b>Do not call this when needing to resolve a location pattern.</b>
	 * Call the context's {@code getResources} method instead, which
	 * will delegate to the ResourcePatternResolver.
	 * @return the ResourcePatternResolver for this context
	 * @see #getResources
	 * @see org.springframework.core.io.support.PathMatchingResourcePatternResolver
	 */
	// 返回 ResourcePatternResolver 以用于将位置模式解析为 Resource 实例。默认是一个
	// {@link org.springframework.core.io.support.PathMatchingResourcePatternResolver}，支持 Ant 风格的定位模式。
	// <p>可以在子类中覆盖，用于扩展解析策略，例如在 Web 环境中。 <p><b>当需要解析位置模式时不要调用它。<b>改为调用上下文
	// 的 {@code getResources} 方法，该方法将委托给 ResourcePatternResolver。
	protected ResourcePatternResolver getResourcePatternResolver() {
		return new PathMatchingResourcePatternResolver(this);
	}


	//---------------------------------------------------------------------
	// Implementation of ConfigurableApplicationContext interface
	// ConfigurableApplicationContext 接口的实现
	//---------------------------------------------------------------------

	/**
	 * Set the parent of this application context.
	 * <p>The parent {@linkplain ApplicationContext#getEnvironment() environment} is
	 * {@linkplain ConfigurableEnvironment#merge(ConfigurableEnvironment) merged} with
	 * this (child) application context environment if the parent is non-{@code null} and
	 * its environment is an instance of {@link ConfigurableEnvironment}.
	 * @see ConfigurableEnvironment#merge(ConfigurableEnvironment)
	 */
	// 设置此应用程序上下文的父级。 <p>父{@linkplain ApplicationContextgetEnvironment() 环境}与此（子）应用程序上下文
	// 环境{@linkplain ConfigurableEnvironmentmerge(ConfigurableEnvironment) 合并}，如果父级为非{@code null} 并且其环境
	// 是{ @link 可配置环境}
	@Override
	public void setParent(@Nullable ApplicationContext parent) {
		this.parent = parent;
		if (parent != null) {
			Environment parentEnvironment = parent.getEnvironment();
			if (parentEnvironment instanceof ConfigurableEnvironment) {
				getEnvironment().merge((ConfigurableEnvironment) parentEnvironment);
			}
		}
	}

	@Override
	public void addBeanFactoryPostProcessor(BeanFactoryPostProcessor postProcessor) {
		Assert.notNull(postProcessor, "BeanFactoryPostProcessor must not be null");
		this.beanFactoryPostProcessors.add(postProcessor);
	}

	/**
	 * Return the list of BeanFactoryPostProcessors that will get applied
	 * to the internal BeanFactory.
	 */
	// 返回将应用于内部 BeanFactory 的 BeanFactoryPostProcessors 列表
	public List<BeanFactoryPostProcessor> getBeanFactoryPostProcessors() {
		return this.beanFactoryPostProcessors;
	}

	@Override
	public void addApplicationListener(ApplicationListener<?> listener) {
		Assert.notNull(listener, "ApplicationListener must not be null");
		if (this.applicationEventMulticaster != null) {
			// 通过委派的方式去调用 applicationEventMulticaster
			this.applicationEventMulticaster.addApplicationListener(listener);
		}
		this.applicationListeners.add(listener);
	}

	/**
	 * Return the list of statically specified ApplicationListeners.
	 */
	// 返回静态指定的 ApplicationListeners 列表
	public Collection<ApplicationListener<?>> getApplicationListeners() {
		return this.applicationListeners;
	}

	// Spring 应用上下文生命周期管理,刷新操作是Spring 上下文广义的启动
	@Override
	public void refresh() throws BeansException, IllegalStateException {
		synchronized (this.startupShutdownMonitor) {
			StartupStep contextRefresh = this.applicationStartup.start("spring.context.refresh");

			// Prepare this context for refreshing.
			// 准备此上下文以进行刷新
			// 1.Spring 应用上下文启动准备阶段
			prepareRefresh();

			// Tell the subclass to refresh the internal bean factory.
			// 告诉子类刷新内建bean工厂
			// 2.BeanFactory 创建阶段 -  创建了 DefaultListableBeanFactory
			ConfigurableListableBeanFactory beanFactory = obtainFreshBeanFactory();

			// Prepare the bean factory for use in this context.
			// 3.准备在此上下文中使用的 bean 工厂。BeanFactory 准备阶段(BeanFactory 初始化阶段)
			prepareBeanFactory(beanFactory);

			try {
				// Allows post-processing of the bean factory in context subclasses.
				// 允许在上下文子类中对 bean 工厂进行后处理
				// 4.BeanFactory 后置处理阶段
				// 4.1利用继承来实现 BeanFactory 后置处理
				postProcessBeanFactory(beanFactory);

				StartupStep beanPostProcess = this.applicationStartup.start("spring.context.beans.post-process");

				// Invoke factory processors registered as beans in the context.
				// 调用在上下文中注册为 bean 的工厂处理器。
				// beanFactory #postProcessBeanFactory 产生回调早于
				// bean 的初始化 @See finishBeanFactoryInitialization(beanFactory);
				// 4.2利用组合方式实现 BeanFactory 后置处理，利用接口 BeanFactoryPostProcessor
				invokeBeanFactoryPostProcessors(beanFactory);

				// Register bean processors that intercept bean creation.
				// 注册拦截 bean 创建的 bean 处理器。（ApplicationEventMulticaster 未初始化，如果有配置类实现了
				// ApplicationEventPublisherAware 接口），需要将这部分接口回调中的事件先缓存，在 #registerListeners() 事件回放
				// Spring 3 中不能将 ApplicationEventPublisherAware，以及 BeanPostProcessor、BeanFactoryPostProcessor
				// 放在一起，事件处理不当会报空指针，导致整个应用上下文失败
				// 5.BeanFactory 注册 BeanPostProcessor 阶段
				registerBeanPostProcessors(beanFactory);
				beanPostProcess.end();

				// Initialize message source for this context.
				// 初始化此上下文的消息源。初始化此上下文的消息源。
				// 6.初始化内建 Bean MessageSource - Spring 国际化
				initMessageSource();

				// Initialize event multicaster for this context.
				// 为此上下文初始化事件广播器。
				// 7.初始化内建 Bean ApplicationEventMulticaster - Spring 事件广播器
				initApplicationEventMulticaster();

				// Initialize other special beans in specific context subclasses.
				// 初始化特定上下文子类中的其他特殊bean
				// 8.Spring 应用上下文刷新阶段
				onRefresh();

				// Check for listener beans and register them.
				// 检查监听器 bean 并注册它们。缓存事件回放
				// 9.Spring 事件监听器注册阶段 - 注册 Spring 事件监听器
				registerListeners();

				// Instantiate all remaining (non-lazy-init) singletons.
				// 实例化所有剩余的（非延迟初始化）单例。
				// bean 的初始化，类型转换服务注入工厂，
				// 10.BeanFactory 初始化完成阶段（完成了 BeanDefinition 的注册，也完成了 BeanFactory 的初始化）
				finishBeanFactoryInitialization(beanFactory);

				// Last step: publish corresponding event.
				// 最后一步：发布相应的事件，发布 ContextRefreshedEvent
				// 11.Spring 应用上下刷新完成阶段
				finishRefresh();
			}

			catch (BeansException ex) {
				if (logger.isWarnEnabled()) {
					logger.warn("Exception encountered during context initialization - " +
							"cancelling refresh attempt: " + ex);
				}

				// Destroy already created singletons to avoid dangling resources.
				// 销毁已经创建的单例以避免悬空资源
				destroyBeans();

				// Reset 'active' flag.
				// 重置“活动”标志
				cancelRefresh(ex);

				// Propagate exception to caller.
				// 将异常传播给调用者。
				throw ex;
			}

			finally {
				// Reset common introspection caches in Spring's core, since we
				// might not ever need metadata for singleton beans anymore...
				// 重置 Spring 核心中的常见内省缓存，因为我们可能不再需要单例 bean 的元数据...
				resetCommonCaches();
				contextRefresh.end();
			}
		}
	}

	/**
	 * Prepare this context for refreshing, setting its startup date and
	 * active flag as well as performing any initialization of property sources.
	 */
	// 准备此上下文以进行刷新、设置其启动日期和活动标志以及执行属性源的任何初始化
	// 1.Spring 应用上下文启动准备阶段
	protected void prepareRefresh() {
		// Switch to active.
		// 启动时间，将层次性上下文启动做区别
		this.startupDate = System.currentTimeMillis();
		// 状态标识
		this.closed.set(false);
		this.active.set(true);

		if (logger.isDebugEnabled()) {
			if (logger.isTraceEnabled()) {
				logger.trace("Refreshing " + this);
			}
			else {
				logger.debug("Refreshing " + getDisplayName());
			}
		}

		// Initialize any placeholder property sources in the context environment.
		// 在上下文环境中初始化任何占位符属性源
		// Environment 还没准备好，怎么就会准备 PropertySource? 实际上去获取相应的环境，然后将相关上下文和配置作为外部化属性源
		// 事实上在 prepareRefresh 之前就是创建 ApplicationContext 的时候，已经将 Environment 提前给装配好了，这是最早的
		// Environment 对象创建。Spring Boot 外部化配置文件 ApplicationProperties 文件就是如此关联起来的。

		// a.初始化 PropertySources(外部化配置属性源)
		initPropertySources();

		// Validate that all properties marked as required are resolvable:
		// see ConfigurablePropertyResolver#setRequiredProperties
		// b.检验 Environment 中必须属性
		getEnvironment().validateRequiredProperties();

		// Store pre-refresh ApplicationListeners...
		// 存储预刷新 ApplicationListeners...
		// c.初始化事件监听器集合,包括 applicationListeners 和 earlyApplicationListeners
		if (this.earlyApplicationListeners == null) {
			this.earlyApplicationListeners = new LinkedHashSet<>(this.applicationListeners);
		}
		else {
			// Reset local application listeners to pre-refresh state.
			// 将本地应用程序监听器重置为刷新前状态
			this.applicationListeners.clear();
			this.applicationListeners.addAll(this.earlyApplicationListeners);
		}

		// Allow for the collection of early ApplicationEvents,
		// to be published once the multicaster is available...
		// 允许收集早期的 ApplicationEvents，一旦广播器可用就发布......
		// 初始化早期 Spring 事件集合
		this.earlyApplicationEvents = new LinkedHashSet<>();
	}

	/**
	 * <p>Replace any stub property sources with actual instances.
	 * @see org.springframework.core.env.PropertySource.StubPropertySource
	 * @see org.springframework.web.context.support.WebApplicationContextUtils#initServletPropertySources
	 */
	// 用实际实例替换任何根属性源
	protected void initPropertySources() {
		// For subclasses: do nothing by default.
		// 默认没有做任何事情需要子类来实现，子类负责初始化和web servlet 相关的 servletContext，servletConfig 作为属性源
		// 也就是说 web 程序去扩展Spring 的属性配置源，这里提供了扩展点
	}

	/**
	 * Tell the subclass to refresh the internal bean factory.
	 * @return the fresh BeanFactory instance
	 * @see #refreshBeanFactory()
	 * @see #getBeanFactory()
	 */
	// 告诉子类刷新内部bean工厂
	// 2.BeanFactory 创建阶段 -  创建了 DefaultListableBeanFactory
	protected ConfigurableListableBeanFactory obtainFreshBeanFactory() {
		// a. 刷新 Spring
		refreshBeanFactory();
		// b. 返回 Spring 应用上下文底层 BeanFactory
		return getBeanFactory();
	}

	/**
	 * Configure the factory's standard context characteristics,
	 * such as the context's ClassLoader and post-processors.
	 * @param beanFactory the BeanFactory to configure
	 */
	// 配置工厂的标准上下文特征，例如上下文的 ClassLoader 和后处理器
	// 3.准备在此上下文中使用的 bean 工厂。BeanFactory 准备阶段(BeanFactory 初始化阶段)
	protected void prepareBeanFactory(ConfigurableListableBeanFactory beanFactory) {
		// Tell the internal bean factory to use the context's class loader etc.
		// 告诉内建 bean 工厂使用上下文的类加载器等
		// a.关联 ClassLoader，为什么设置 classLoader?
		// 1.加载 XML 配置信息中 class = "..." 信息。
		// 2.不使用 AbstractApplicationContext 自己的 ClassLoader,这样就会实现类隔离机制。帮助我们实现一些特殊需求
		beanFactory.setBeanClassLoader(getClassLoader());
		if (!shouldIgnoreSpel) {
			// b.设置 Bean 表达式处理器，与 Spring El 密切相关。
			beanFactory.setBeanExpressionResolver(new StandardBeanExpressionResolver(beanFactory.getBeanClassLoader()));
		}
		// c.添加 PropertyEditorRegistrar 实现 - ResourceEditorRegistrar,将 xml 中 Property 属性转换成相应类型
		beanFactory.addPropertyEditorRegistrar(new ResourceEditorRegistrar(this, getEnvironment()));

		// Configure the bean factory with context callbacks.
		// 使用上下文回调配置 bean 工厂
		// d.ApplicationContext 统一的 Aware 回调处理器 - ApplicationContextAwareProcessor
		beanFactory.addBeanPostProcessor(new ApplicationContextAwareProcessor(this));
		// f.Aware 接口不允许 @Autowired 依赖注入，只能通过接口回调 setXX() 注入。
		// 我们不关心 Aware 接口，我们关心的是它回调返回来的对象
		beanFactory.ignoreDependencyInterface(EnvironmentAware.class);
		beanFactory.ignoreDependencyInterface(EmbeddedValueResolverAware.class);
		beanFactory.ignoreDependencyInterface(ResourceLoaderAware.class);
		beanFactory.ignoreDependencyInterface(ApplicationEventPublisherAware.class);
		beanFactory.ignoreDependencyInterface(MessageSourceAware.class);
		beanFactory.ignoreDependencyInterface(ApplicationContextAware.class);
		beanFactory.ignoreDependencyInterface(ApplicationStartupAware.class);

		// BeanFactory interface not registered as resolvable type in a plain factory.
		// MessageSource registered (and found for autowiring) as a bean.

		// BeanFactory 接口未在普通工厂中注册为可解析类型。
		// MessageSource 注册（并发现用于自动装配-类型，对象）作为 bean。
		// ResolvableDependency 对象是可处理的外部对象(依赖注入)，并不是一个 bean，所以依赖注入的来源比依赖查找的多一些
		// 依赖注入的外部对象有两个：一个是 registerResolvableDependency();另一个就是
		// {@link SingletonBeanRegistry}#registerSingleton()
		// g.注册 Spring 内建对象（可处理的外部依赖对象），是依赖注入和依赖查找最重要的区别
		// 这 4 个接口都可以被 @Autowired,但是它们的 Aware 接口都不可以
		beanFactory.registerResolvableDependency(BeanFactory.class, beanFactory);
		beanFactory.registerResolvableDependency(ResourceLoader.class, this);
		beanFactory.registerResolvableDependency(ApplicationEventPublisher.class, this);
		beanFactory.registerResolvableDependency(ApplicationContext.class, this);

		// Register early post-processor for detecting inner beans as ApplicationListeners.
		// 将用于检测内部 bean 的早期后处理器注册为 ApplicationListeners
		// h.注册一个 ApplicationListenerDetector 即Application探测器
		beanFactory.addBeanPostProcessor(new ApplicationListenerDetector(this));

		// Detect a LoadTimeWeaver and prepare for weaving, if found.
		// 检测 LoadTimeWeaver 并准备织入（如果找到）
		if (!NativeDetector.inNativeImage() && beanFactory.containsBean(LOAD_TIME_WEAVER_BEAN_NAME)) {
			// i. Spring AOP 相关的
			beanFactory.addBeanPostProcessor(new LoadTimeWeaverAwareProcessor(beanFactory));
			// Set a temporary ClassLoader for type matching.
			beanFactory.setTempClassLoader(new ContextTypeMatchClassLoader(beanFactory.getBeanClassLoader()));
		}

		// Register default environment beans.
		// j.注册默认环境 beans 内建单例对象 - Environment、Java System Properties 以及 OS 环境变量
		if (!beanFactory.containsLocalBean(ENVIRONMENT_BEAN_NAME)) {
			// Environment 对象隶属于 ApplicationContext,通过注册单例的方式把外部对象注册到 BeanFactory 中
			// 所以我们可以同过依赖查找和依赖注入的方式能够找到 Environment 对象的原因
			beanFactory.registerSingleton(ENVIRONMENT_BEAN_NAME, getEnvironment());
		}
		if (!beanFactory.containsLocalBean(SYSTEM_PROPERTIES_BEAN_NAME)) {
			// 依赖注入的核心
			beanFactory.registerSingleton(SYSTEM_PROPERTIES_BEAN_NAME, getEnvironment().getSystemProperties());
		}
		if (!beanFactory.containsLocalBean(SYSTEM_ENVIRONMENT_BEAN_NAME)) {
			beanFactory.registerSingleton(SYSTEM_ENVIRONMENT_BEAN_NAME, getEnvironment().getSystemEnvironment());
		}
		if (!beanFactory.containsLocalBean(APPLICATION_STARTUP_BEAN_NAME)) {
			beanFactory.registerSingleton(APPLICATION_STARTUP_BEAN_NAME, getApplicationStartup());
		}
	}

	/**
	 * Modify the application context's internal bean factory after its standard
	 * initialization. All bean definitions will have been loaded, but no beans
	 * will have been instantiated yet. This allows for registering special
	 * BeanPostProcessors etc in certain ApplicationContext implementations.
	 * @param beanFactory the bean factory used by the application context
	 */
	// 在标准初始化之后修改应用程序上下文的内部 bean 工厂。所有 bean 定义都将被加载，但尚未实例化任何 bean。
	// 这允许在某些 ApplicationContext 实现中注册特殊的 BeanPostProcessor 等
	// beanFactory 后置处理的第一种方式(继承方式)：模板方法 postProcessBeanFactory()，由子类具体实现 ，和第二种一起出现时这种先调用
	// beanFactory 后置处理的第二种方式(组合方式)：BeanFactoryPostProcessor API 来进行实现，灵活度更高
	protected void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
	}

	/**
	 * Instantiate and invoke all registered BeanFactoryPostProcessor beans,
	 * respecting explicit order if given.
	 * <p>Must be called before singleton instantiation.
	 */
	// 实例化并调用所有已注册的 BeanFactoryPostProcessor bean，如果给出，则遵守显式顺序。 <p>必须在单例实例化之前调用。
	// beanFactory 后置处理的第二种方式(组合方式)：BeanFactoryPostProcessor API 来进行实现，灵活度更高
	protected void invokeBeanFactoryPostProcessors(ConfigurableListableBeanFactory beanFactory) {
		// 只有在 Spring ApplicationContext 中才会调用 BeanFactoryPostProcessors
		PostProcessorRegistrationDelegate.invokeBeanFactoryPostProcessors(beanFactory, getBeanFactoryPostProcessors());

		// Detect a LoadTimeWeaver and prepare for weaving, if found in the meantime
		// (e.g. through an @Bean method registered by ConfigurationClassPostProcessor)
		// 检测 LoadTimeWeaver 并准备织入（如果同时发现
		if (!NativeDetector.inNativeImage() && beanFactory.getTempClassLoader() == null && beanFactory.containsBean(LOAD_TIME_WEAVER_BEAN_NAME)) {
			beanFactory.addBeanPostProcessor(new LoadTimeWeaverAwareProcessor(beanFactory));
			beanFactory.setTempClassLoader(new ContextTypeMatchClassLoader(beanFactory.getBeanClassLoader()));
		}
	}

	/**
	 * Instantiate and register all BeanPostProcessor beans,
	 * respecting explicit order if given.
	 * <p>Must be called before any instantiation of application beans.
	 */
	// 实例化并注册所有 BeanPostProcessor bean，如果给出，则遵守显式顺序。 <p>必须在应用程序 bean 的任何实例化之前调用。
	// 5.BeanFactory 注册 BeanPostProcessor 阶段
	protected void registerBeanPostProcessors(ConfigurableListableBeanFactory beanFactory) {
		// 通过代理的工具方法来帮助我们实现
		PostProcessorRegistrationDelegate.registerBeanPostProcessors(beanFactory, this);
	}

	/**
	 * Initialize the MessageSource.
	 * Use parent's if none defined in this context.
	 */
	// 初始化消息源。如果在此上下文中没有定义，则使用父级。
	// 6.初始化内建 Bean MessageSource - Spring 国际化
	protected void initMessageSource() {
		ConfigurableListableBeanFactory beanFactory = getBeanFactory();
		// 在当前工厂中查找是否包含 "messageSource" ，如果包含
		if (beanFactory.containsLocalBean(MESSAGE_SOURCE_BEAN_NAME)) {
			// 如果应用上下文中有这个 bean ,那么获取当前 bean 作为 MessageSource 来处理相关职能
			// 依赖查找操作：在 Spring Boot 中会默认通过 @Bean 创建一个 MessageSource 对象。
			// ({@link MessageSourceAutoConfiguration} Spring Boot 中相关类)
			this.messageSource = beanFactory.getBean(MESSAGE_SOURCE_BEAN_NAME, MessageSource.class);
			// Make MessageSource aware of parent MessageSource.
			// 让 MessageSource 知道父 MessageSource
			if (this.parent != null && this.messageSource instanceof HierarchicalMessageSource) {
				HierarchicalMessageSource hms = (HierarchicalMessageSource) this.messageSource;
				if (hms.getParentMessageSource() == null) {
					// Only set parent context as parent MessageSource if no parent MessageSource
					// registered already.
					hms.setParentMessageSource(getInternalParentMessageSource());
				}
			}
			if (logger.isTraceEnabled()) {
				logger.trace("Using MessageSource [" + this.messageSource + "]");
			}
		}
		else {
			// Use empty MessageSource to be able to accept getMessage calls.
			// 使用空的 MessageSource(空壳对象) 能够接受 getMessage 调用
			// 如果应用上下文不存在 "messageSource" 这个 bean,那么创建一个新的 DelegatingMessageSource 对象，
			// 并指定它的父 MessageSource
			DelegatingMessageSource dms = new DelegatingMessageSource();
			dms.setParentMessageSource(getInternalParentMessageSource()); // 支持层次性
			this.messageSource = dms;
			// 注册内建单例对象
			beanFactory.registerSingleton(MESSAGE_SOURCE_BEAN_NAME, this.messageSource);
			if (logger.isTraceEnabled()) {
				logger.trace("No '" + MESSAGE_SOURCE_BEAN_NAME + "' bean, using [" + this.messageSource + "]");
			}
		}
	}

	/**
	 * Initialize the ApplicationEventMulticaster.
	 * Uses SimpleApplicationEventMulticaster if none defined in the context.
	 * @see org.springframework.context.event.SimpleApplicationEventMulticaster
	 */
	// 初始化 ApplicationEventMulticaster。如果上下文中没有定义，则使用 SimpleApplicationEventMulticaster。
	// 7.初始化内建 Bean ApplicationEventMulticaster - Spring 事件广播器
	protected void initApplicationEventMulticaster() {
		ConfigurableListableBeanFactory beanFactory = getBeanFactory();
		// 首先搜索下有没有个 bean 名称叫做 "applicationEventMulticaster",所以说 Spring 的广播器名称是固定的
		if (beanFactory.containsLocalBean(APPLICATION_EVENT_MULTICASTER_BEAN_NAME)) {
			// 通过名称 + 类型在当前 beanFactory 中依赖查找，是个具体对象
			this.applicationEventMulticaster =
					beanFactory.getBean(APPLICATION_EVENT_MULTICASTER_BEAN_NAME, ApplicationEventMulticaster.class);
			if (logger.isTraceEnabled()) {
				logger.trace("Using ApplicationEventMulticaster [" + this.applicationEventMulticaster + "]");
			}
		}
		else {
			// new 一个 SimpleApplicationEventMulticaster，然后注册到当前工厂作为 单例 bean，
			// 这个对象生命周期不是由容器内来管理的，而是外部完成，完成后和容器进行关联
			// 两层语义：1.它可以注册 ApplicationListener 监听   2.它可以广播事件
			this.applicationEventMulticaster = new SimpleApplicationEventMulticaster(beanFactory);
			beanFactory.registerSingleton(APPLICATION_EVENT_MULTICASTER_BEAN_NAME, this.applicationEventMulticaster);
			if (logger.isTraceEnabled()) {
				logger.trace("No '" + APPLICATION_EVENT_MULTICASTER_BEAN_NAME + "' bean, using " +
						"[" + this.applicationEventMulticaster.getClass().getSimpleName() + "]");
			}
		}
	}

	/**
	 * Initialize the LifecycleProcessor.
	 * Uses DefaultLifecycleProcessor if none defined in the context.
	 * @see org.springframework.context.support.DefaultLifecycleProcessor
	 */
	// 初始化 LifecycleProcessor。如果上下文中没有定义，则使用 DefaultLifecycleProcessor。
	protected void initLifecycleProcessor() {
		ConfigurableListableBeanFactory beanFactory = getBeanFactory();
		if (beanFactory.containsLocalBean(LIFECYCLE_PROCESSOR_BEAN_NAME)) {
			// 通过依赖查找去找
			this.lifecycleProcessor =
					beanFactory.getBean(LIFECYCLE_PROCESSOR_BEAN_NAME, LifecycleProcessor.class);
			if (logger.isTraceEnabled()) {
				logger.trace("Using LifecycleProcessor [" + this.lifecycleProcessor + "]");
			}
		}
		else {
			// 找不到定义一个默认的 DefaultLifecycleProcessor
			DefaultLifecycleProcessor defaultProcessor = new DefaultLifecycleProcessor();
			defaultProcessor.setBeanFactory(beanFactory);
			this.lifecycleProcessor = defaultProcessor;
			// 注册成单例
			beanFactory.registerSingleton(LIFECYCLE_PROCESSOR_BEAN_NAME, this.lifecycleProcessor);
			if (logger.isTraceEnabled()) {
				logger.trace("No '" + LIFECYCLE_PROCESSOR_BEAN_NAME + "' bean, using " +
						"[" + this.lifecycleProcessor.getClass().getSimpleName() + "]");
			}
		}
	}

	/**
	 * Template method which can be overridden to add context-specific refresh work.
	 * Called on initialization of special beans, before instantiation of singletons.
	 * <p>This implementation is empty.
	 * @throws BeansException in case of errors
	 * @see #refresh()
	 */
	// 可以覆盖的模板方法以添加特定于上下文的刷新工作。在单例实例化之前调用特殊 bean 的初始化。
	// <p>这个实现是空的。
	// 在 Spring Boot 中会扩展这个实现，比如 Web 或 ReactiveWeb 两种实现都会创建一个 Server 实现。
	// 因为 Spring Boot 是通过上下文的方式去引导容器（tomcat 容器/ReactiveWeb 容器）来进行启动的。
	// 它和我们传统的方式是反的。传统 JavaEE 方式是通过容器启动，Spring 来驱动它的上下文启动。
	// 8.Spring 应用上下文刷新阶段
	protected void onRefresh() throws BeansException {
		// For subclasses: do nothing by default.
	}

	/**
	 * Add beans that implement ApplicationListener as listeners.
	 * Doesn't affect other listeners, which can be added without being beans.
	 */
	// 添加实现 ApplicationListener 作为侦听器的 bean。不影响其他监听器，可以不加bean
	// 9.Spring 事件监听器注册阶段 - 注册 Spring 事件监听器
	protected void registerListeners() {
		// Register statically specified listeners first.
		// 首先注册静态指定的监听器。
		// a.添加当前应用上下文所关联的 ApplicationListener 对象（集合）
		for (ApplicationListener<?> listener : getApplicationListeners()) {
			getApplicationEventMulticaster().addApplicationListener(listener);
		}

		// Do not initialize FactoryBeans here: We need to leave all regular beans
		// uninitialized to let post-processors apply to them!
		// 不要在此处初始化 FactoryBeans：我们需要保留所有常规 bean
		// 未初始化以让后处理器应用于它们！
		// b.通过依赖查找的方式添加 BeanFactory 所注册 ApplicationListener Beans
		String[] listenerBeanNames = getBeanNamesForType(ApplicationListener.class, true, false);
		for (String listenerBeanName : listenerBeanNames) {
			// 此处仅仅将事件名称添加到广播器中，并没有将事件作为对象传入，实现了按需广播。
			// 也就是真正广播时才将事件变成对象（Bean 的初始化延迟）
			getApplicationEventMulticaster().addApplicationListenerBean(listenerBeanName);
		}

		// Publish early application events now that we finally have a multicaster...
		// 我们最终有了广播器后，发布早期应用程序事件,在 初始化前的 registerBeanPostProcessors(beanFactory);
		// 中有
		Set<ApplicationEvent> earlyEventsToProcess = this.earlyApplicationEvents;
		this.earlyApplicationEvents = null; // 将早期事件置空
		if (!CollectionUtils.isEmpty(earlyEventsToProcess)) {
			// 事件回放：正式发布早期事件，见
			// publishEvent(Object event, @Nullable ResolvableType eventType) 加入早期事件
			for (ApplicationEvent earlyEvent : earlyEventsToProcess) {
				getApplicationEventMulticaster().multicastEvent(earlyEvent);
			}
		}
	}

	/**
	 * Finish the initialization of this context's bean factory,
	 * initializing all remaining singleton beans.
	 */
	// 完成此上下文的 bean 工厂的初始化，初始化所有剩余的单例 bean。
	// 10.BeanFactory 初始化完成阶段（完成了 BeanDefinition 的注册，也完成了 BeanFactory 的初始化）
	protected void finishBeanFactoryInitialization(ConfigurableListableBeanFactory beanFactory) {
		// Initialize conversion service for this context.
		// ConversionService bean 类型转换服务
		// a.BeanFactory 关联 ConversionService Bean，如果存在
		if (beanFactory.containsBean(CONVERSION_SERVICE_BEAN_NAME) &&
				beanFactory.isTypeMatch(CONVERSION_SERVICE_BEAN_NAME, ConversionService.class)) {
			// 将 ConversionService bean 写入ConfigurableListableBeanFactory，
			// 通过依赖查找的方式将 ConversionService 关联到 BeanFactory
			beanFactory.setConversionService(
					beanFactory.getBean(CONVERSION_SERVICE_BEAN_NAME, ConversionService.class));
		}

		// Register a default embedded value resolver if no BeanFactoryPostProcessor
		// (such as a PropertySourcesPlaceholderConfigurer bean) registered any before:
		// at this point, primarily for resolution in annotation attribute values.

		//如果之前没有注册任何 BeanFactoryPostProcessor（例如 PropertySourcesPlaceholderConfigurer bean），
		// 则注册一个默认的 StringValueResolver 嵌入值解析器：此时，主要用于注解属性值的解析(主要处理占位符)。
		// b.添加 StringValueResolver 对象
		if (!beanFactory.hasEmbeddedValueResolver()) {
			beanFactory.addEmbeddedValueResolver(strVal -> getEnvironment().resolvePlaceholders(strVal));
		}

		// Initialize LoadTimeWeaverAware beans early to allow for registering their transformers early.
		// 尽早初始化 LoadTimeWeaverAware bean 以允许尽早注册它们的转换器。
		// c.依赖查找 LoadTimeWeaverAware Bean - AOP 相关
		String[] weaverAwareNames = beanFactory.getBeanNamesForType(LoadTimeWeaverAware.class, false, false);
		for (String weaverAwareName : weaverAwareNames) {
			getBean(weaverAwareName);
		}

		// Stop using the temporary ClassLoader for type matching.
		// 停止使用临时 ClassLoader 进行类型匹配。有两个 ClassLoader,临时的 ClassLoader 一般用于 AOP,非主 ClassLoader
		// d.BeanFactory 临时 ClassLoader 置为 null
		beanFactory.setTempClassLoader(null);

		// Allow for caching all bean definition metadata, not expecting further changes.
		// 允许缓存所有 bean 定义元数据，不期待后续进一步更改元数据。
		// 不是不能改，是不期待改。并不是说一旦冻结就不能注册 BeanDefinition 了
		// e.BeanFactory 冻结配置
		beanFactory.freezeConfiguration();

		// Instantiate all remaining (non-lazy-init) singletons.
		// 实例化所有剩余的（非延迟初始化）单例。
		// f.BeanFactory 实例化非延迟单例 Beans
		beanFactory.preInstantiateSingletons();
	}

	/**
	 * Finish the refresh of this context, invoking the LifecycleProcessor's
	 * onRefresh() method and publishing the
	 * {@link org.springframework.context.event.ContextRefreshedEvent}.
	 */
	// 完成此上下文的刷新，调用 LifecycleProcessor 的 onRefresh() 方法并发布
	// 11.Spring 应用上下刷新完成阶段
	@SuppressWarnings("deprecation")
	protected void finishRefresh() {
		// Clear context-level resource caches (such as ASM metadata from scanning).
		// 清除上下文级资源缓存（例如来自扫描的 ASM 元数据）
		// a.清除 ResourceLoader 缓存 @since 5.0
		clearResourceCaches();

		// Initialize lifecycle processor for this context.
		// 为此上下文初始化生命周期处理器
		// b.初始化 LifecycleProcessor 对象 - initLifecycleProcessor()
		initLifecycleProcessor();

		// Propagate refresh to lifecycle processor first.
		// 首先将传播刷新到生命周期处理器。
		// c.调用 LifecycleProcessor#onRefresh() 方法
		getLifecycleProcessor().onRefresh();

		// Publish the final event.
		// 发布最终事件,ContextRefreshedEvent 告诉我们当前应用上下文已经可用了,已经刷新完好了.
		// 你可以做任何事情了(发布事件,依赖查找,依赖注入等任何事情)
		// d.发布 Spring 应用上下文已刷新事件 - ContextRefreshedEvent
		publishEvent(new ContextRefreshedEvent(this));

		// Participate in LiveBeansView MBean, if active.
		// 参与 LiveBeansView MBean（如果处于活动状态）
		// e.向 MBeanServer 托管 Live Beans
		if (!NativeDetector.inNativeImage()) {
			LiveBeansView.registerApplicationContext(this);
		}
	}

	/**
	 * Cancel this context's refresh attempt, resetting the {@code active} flag
	 * after an exception got thrown.
	 * @param ex the exception that led to the cancellation
	 */
	// 取消此上下文的刷新尝试，在引发异常后重置active标志
	protected void cancelRefresh(BeansException ex) {
		this.active.set(false);
	}

	/**
	 * Reset Spring's common reflection metadata caches, in particular the
	 * {@link ReflectionUtils}, {@link AnnotationUtils}, {@link ResolvableType}
	 * and {@link CachedIntrospectionResults} caches.
	 * @since 4.2
	 * @see ReflectionUtils#clearCache()
	 * @see AnnotationUtils#clearCache()
	 * @see ResolvableType#clearCache()
	 * @see CachedIntrospectionResults#clearClassLoader(ClassLoader)
	 */
	// 重置 Spring 的常见反射元数据告诉缓存，尤其是 {@link ReflectionUtils}, {@link AnnotationUtils}, {@link ResolvableType}
	// and {@link CachedIntrospectionResults} 缓存.
	protected void resetCommonCaches() {
		ReflectionUtils.clearCache();
		AnnotationUtils.clearCache();
		ResolvableType.clearCache();
		CachedIntrospectionResults.clearClassLoader(getClassLoader());
	}


	/**
	 * Register a shutdown hook {@linkplain Thread#getName() named}
	 * {@code SpringContextShutdownHook} with the JVM runtime, closing this
	 * context on JVM shutdown unless it has already been closed at that time.
	 * <p>Delegates to {@code doClose()} for the actual closing procedure.
	 * @see Runtime#addShutdownHook
	 * @see ConfigurableApplicationContext#SHUTDOWN_HOOK_THREAD_NAME
	 * @see #close()
	 * @see #doClose()
	 */
	// 向 JVM 运行时注册一个名为SpringContextShutdownHook的关闭挂钩，在 JVM 关闭时关闭此上下文，除非当时它已经关闭。
	// 委托doClose()进行实际的关闭程序
	@Override
	public void registerShutdownHook() {
		if (this.shutdownHook == null) {
			// No shutdown hook registered yet.
			// 还没有注册关闭钩子
			this.shutdownHook = new Thread(SHUTDOWN_HOOK_THREAD_NAME) {
				// 当信号到达时执行线程的run()方法
				@Override
				public void run() {
					synchronized (startupShutdownMonitor) {
						// 2.通过当前线程来关闭应用程序
						doClose();
					}
				}
			};
			// 1.将当前线程 shutdownHook 增加到 Runtime 里面去。它允许应用程序与应用程序运行的环境进行交互
			Runtime.getRuntime().addShutdownHook(this.shutdownHook);
		}
	}

	/**
	 * Callback for destruction of this instance, originally attached
	 * to a {@code DisposableBean} implementation (not anymore in 5.0).
	 * <p>The {@link #close()} method is the native way to shut down
	 * an ApplicationContext, which this method simply delegates to.
	 * @deprecated as of Spring Framework 5.0, in favor of {@link #close()}
	 */
	// 用于销毁此实例的回调，最初附加到DisposableBean实现（不再在 5.0 中）。
	// close()方法是关闭 ApplicationContext 的本机方法，该方法只是委托给它。
	// 已弃用 从 Spring Framework 5.0 开始，支持close()
	@Deprecated
	public void destroy() {
		close();
	}

	/**
	 * Close this application context, destroying all beans in its bean factory.
	 * <p>Delegates to {@code doClose()} for the actual closing procedure.
	 * Also removes a JVM shutdown hook, if registered, as it's not needed anymore.
	 * @see #doClose()
	 * @see #registerShutdownHook()
	 */
	// 关闭此应用程序上下文，销毁其 bean 工厂中的所有 bean。
	// 委托 doClose() 进行实际的关闭程序。 还删除 JVM 关闭挂钩（如果已注册），因为不再需要它。
	// 14.Spring 应用上下文关闭阶段
	@Override
	public void close() {
		synchronized (this.startupShutdownMonitor) {
			doClose(); // 这个方法可以覆盖
			// If we registered a JVM shutdown hook, we don't need it anymore now:
			// We've already explicitly closed the context.
			// 如果我们注册了一个 JVM 关闭钩子，我们现在就不再需要它了：我们已经明确地关闭了上下文
			if (this.shutdownHook != null) {
				try {
					// 我们有一种优雅的关闭程序的方式，通过信号关闭，比如 kill -3,先通知再关闭，
					// 如果是 kill -9 就是没有通知直接关闭。当人为关闭时我们提供一种优雅的方式称之为 shutdownHook
					// 在 Spring 里边当信号到达时候，这个线程的 run() 会被执行。
					Runtime.getRuntime().removeShutdownHook(this.shutdownHook);
				}
				catch (IllegalStateException ex) {
					// ignore - VM is already shutting down
				}
			}
		}
	}

	/**
	 * Actually performs context closing: publishes a ContextClosedEvent and
	 * destroys the singletons in the bean factory of this application context.
	 * <p>Called by both {@code close()} and a JVM shutdown hook, if any.
	 * @see org.springframework.context.event.ContextClosedEvent
	 * @see #destroyBeans()
	 * @see #close()
	 * @see #registerShutdownHook()
	 */
	// 实际执行上下文关闭：发布一个 ContextClosedEvent 并销毁此应用程序上下文的 bean 工厂中的单例。
	// 由close()和 JVM 关闭挂钩（如果有close()调用。
	@SuppressWarnings("deprecation")
	protected void doClose() {
		// Check whether an actual close attempt is necessary...
		// 检查是否需要实际关闭尝试...
		if (this.active.get() && this.closed.compareAndSet(false, true)) { // 修改closed 变为true
			if (logger.isDebugEnabled()) {
				logger.debug("Closing " + this);
			}

			if (!NativeDetector.inNativeImage()) {
				// Live Beans JMX 撤销托管
				LiveBeansView.unregisterApplicationContext(this);
			}

			try {
				// Publish shutdown event.
				// 发布 ContextClosedEvent 事件
				publishEvent(new ContextClosedEvent(this));
			}
			catch (Throwable ex) {
				logger.warn("Exception thrown from ApplicationListener handling ContextClosedEvent", ex);
			}

			// Stop all Lifecycle beans, to avoid delays during individual destruction.
			// 停止所有生命周期 bean，以避免在单个销毁过程中出现延迟
			if (this.lifecycleProcessor != null) {
				try {
					this.lifecycleProcessor.onClose();
				}
				catch (Throwable ex) {
					logger.warn("Exception thrown from LifecycleProcessor on context close", ex);
				}
			}

			// Destroy all cached singletons in the context's BeanFactory.
			// 销毁上下文的 BeanFactory 中所有缓存的单例
			destroyBeans();

			// Close the state of this context itself.
			// 关闭此上下文本身的状态
			closeBeanFactory();

			// Let subclasses do some final clean-up if they wish...
			// 如果他们愿意，让子类做一些最后的清理......
			onClose();

			// Reset local application listeners to pre-refresh state.
			// 将本地应用程序监听器重置为预刷新状态
			if (this.earlyApplicationListeners != null) {
				this.applicationListeners.clear();
				this.applicationListeners.addAll(this.earlyApplicationListeners);
			}

			// Switch to inactive.
			// 切换到非活动状态，修改状态位
			this.active.set(false);
		}
	}

	/**
	 * Template method for destroying all beans that this context manages.
	 * The default implementation destroy all cached singletons in this context,
	 * invoking {@code DisposableBean.destroy()} and/or the specified
	 * "destroy-method".
	 * <p>Can be overridden to add context-specific bean destruction steps
	 * right before or right after standard singleton destruction,
	 * while the context's BeanFactory is still active.
	 * @see #getBeanFactory()
	 * @see org.springframework.beans.factory.config.ConfigurableBeanFactory#destroySingletons()
	 */
	// 用于销毁此上下文管理的所有 bean 的模板方法。默认实现销毁此上下文中的所有缓存单例，
	// 调用 {@code DisposableBean.destroy()} 和/或指定的“destroy-method”。
	// <p>可以重写以在标准单例销毁之前或之后添加上下文特定的 bean 销毁步骤，
	// 同时上下文的 BeanFactory 仍然处于活动状态。
	protected void destroyBeans() {
		getBeanFactory().destroySingletons();
	}

	/**
	 * Template method which can be overridden to add context-specific shutdown work.
	 * The default implementation is empty.
	 * <p>Called at the end of {@link #doClose}'s shutdown procedure, after
	 * this context's BeanFactory has been closed. If custom shutdown logic
	 * needs to execute while the BeanFactory is still active, override
	 * the {@link #destroyBeans()} method instead.
	 */
	// 可以重写以添加特定于上下文的关闭工作的模板方法。 默认实现为空
	//
	// 在此上下文的 BeanFactory 关闭后，在doClose的关闭过程结束时调用。
	// 如果需要在 BeanFactory 仍处于活动状态时执行自定义关闭逻辑，请改写destroyBeans()方法
	protected void onClose() {
		// For subclasses: do nothing by default.
	}

	@Override
	public boolean isActive() {
		return this.active.get();
	}

	/**
	 * Assert that this context's BeanFactory is currently active,
	 * throwing an {@link IllegalStateException} if it isn't.
	 * <p>Invoked by all {@link BeanFactory} delegation methods that depend
	 * on an active context, i.e. in particular all bean accessor methods.
	 * <p>The default implementation checks the {@link #isActive() 'active'} status
	 * of this context overall. May be overridden for more specific checks, or for a
	 * no-op if {@link #getBeanFactory()} itself throws an exception in such a case.
	 */
	// 断言此上下文的 BeanFactory 当前处于活动状态，如果不是，则抛出 IllegalStateException
	// 由依赖于活动上下文的所有BeanFactory委托方法调用，即特别是所有 bean 访问器方法
	//
	// 默认实现会整体检查此上下文的'active'状态。 对于更具体的检查，
	// 或者如果getBeanFactory()本身在这种情况下抛出异常，则可能会被覆盖。
	protected void assertBeanFactoryActive() {
		if (!this.active.get()) {
			if (this.closed.get()) {
				throw new IllegalStateException(getDisplayName() + " has been closed already");
			}
			else {
				throw new IllegalStateException(getDisplayName() + " has not been refreshed yet");
			}
		}
	}


	//---------------------------------------------------------------------
	// Implementation of BeanFactory interface
	// 实现 BeanFactory 接口
	//---------------------------------------------------------------------

	@Override
	public Object getBean(String name) throws BeansException {
		assertBeanFactoryActive();
		return getBeanFactory().getBean(name);
	}

	// 如果 name 是 Factory bean 的名称，则 当前类型可以是通过工厂查询的其他 bean 的类型
	@Override
	public <T> T getBean(String name, Class<T> requiredType) throws BeansException {
		assertBeanFactoryActive();
		return getBeanFactory().getBean(name, requiredType);
	}

	// 依赖查找能力来源于谁？
	// 组合设计模式：在接口上 ApplicationContext  继承 BeanFactory；在实现上 ApplicationContext 组合了 BeanFactory
	// ApplicationContext 本身不具备依赖查找的能力，它是通过组合的方式使用 BeanFactory  提供的依赖查找的能力
	@Override
	public Object getBean(String name, Object... args) throws BeansException {
		assertBeanFactoryActive();
		// 先获取 BeanFactory，借助 BeanFactory 查找
		return getBeanFactory().getBean(name, args);
	}

	@Override
	public <T> T getBean(Class<T> requiredType) throws BeansException {
		assertBeanFactoryActive();
		return getBeanFactory().getBean(requiredType);
	}

	@Override
	public <T> T getBean(Class<T> requiredType, Object... args) throws BeansException {
		assertBeanFactoryActive();
		return getBeanFactory().getBean(requiredType, args);
	}

	@Override
	public <T> ObjectProvider<T> getBeanProvider(Class<T> requiredType) {
		assertBeanFactoryActive();
		return getBeanFactory().getBeanProvider(requiredType);
	}

	@Override
	public <T> ObjectProvider<T> getBeanProvider(ResolvableType requiredType) {
		assertBeanFactoryActive();
		return getBeanFactory().getBeanProvider(requiredType);
	}

	@Override
	public boolean containsBean(String name) {
		return getBeanFactory().containsBean(name);
	}

	@Override
	public boolean isSingleton(String name) throws NoSuchBeanDefinitionException {
		assertBeanFactoryActive();
		return getBeanFactory().isSingleton(name);
	}

	@Override
	public boolean isPrototype(String name) throws NoSuchBeanDefinitionException {
		assertBeanFactoryActive();
		return getBeanFactory().isPrototype(name);
	}

	@Override
	public boolean isTypeMatch(String name, ResolvableType typeToMatch) throws NoSuchBeanDefinitionException {
		assertBeanFactoryActive();
		return getBeanFactory().isTypeMatch(name, typeToMatch);
	}

	@Override
	public boolean isTypeMatch(String name, Class<?> typeToMatch) throws NoSuchBeanDefinitionException {
		assertBeanFactoryActive();
		return getBeanFactory().isTypeMatch(name, typeToMatch);
	}

	@Override
	@Nullable
	public Class<?> getType(String name) throws NoSuchBeanDefinitionException {
		assertBeanFactoryActive();
		return getBeanFactory().getType(name);
	}

	@Override
	@Nullable
	public Class<?> getType(String name, boolean allowFactoryBeanInit) throws NoSuchBeanDefinitionException {
		assertBeanFactoryActive();
		return getBeanFactory().getType(name, allowFactoryBeanInit);
	}

	@Override
	public String[] getAliases(String name) {
		return getBeanFactory().getAliases(name);
	}


	//---------------------------------------------------------------------
	// Implementation of ListableBeanFactory interface
	// 实现 ListableBeanFactory 接口
	//---------------------------------------------------------------------

	@Override
	public boolean containsBeanDefinition(String beanName) {
		return getBeanFactory().containsBeanDefinition(beanName);
	}

	// 获取 bean 工厂中 bean 定义的数量
	@Override
	public int getBeanDefinitionCount() {
		return getBeanFactory().getBeanDefinitionCount();
	}

	@Override
	public String[] getBeanDefinitionNames() {
		return getBeanFactory().getBeanDefinitionNames();
	}

	@Override
	public <T> ObjectProvider<T> getBeanProvider(Class<T> requiredType, boolean allowEagerInit) {
		assertBeanFactoryActive();
		return getBeanFactory().getBeanProvider(requiredType, allowEagerInit);
	}

	@Override
	public <T> ObjectProvider<T> getBeanProvider(ResolvableType requiredType, boolean allowEagerInit) {
		assertBeanFactoryActive();
		return getBeanFactory().getBeanProvider(requiredType, allowEagerInit);
	}

	@Override
	public String[] getBeanNamesForType(ResolvableType type) {
		assertBeanFactoryActive();
		return getBeanFactory().getBeanNamesForType(type);
	}

	@Override
	public String[] getBeanNamesForType(ResolvableType type, boolean includeNonSingletons, boolean allowEagerInit) {
		assertBeanFactoryActive();
		return getBeanFactory().getBeanNamesForType(type, includeNonSingletons, allowEagerInit);
	}

	@Override
	public String[] getBeanNamesForType(@Nullable Class<?> type) {
		assertBeanFactoryActive();
		return getBeanFactory().getBeanNamesForType(type);
	}

	// 通过依赖查找的方式添加 BeanFactory 所注册 ApplicationListener Beans，此处依赖查找并不是查找 bean
	// 而是通过 BeanDefinition 的方式来查询 Bean 的名称
	@Override
	public String[] getBeanNamesForType(@Nullable Class<?> type, boolean includeNonSingletons, boolean allowEagerInit) {
		assertBeanFactoryActive();
		return getBeanFactory().getBeanNamesForType(type, includeNonSingletons, allowEagerInit);
	}

	@Override
	public <T> Map<String, T> getBeansOfType(@Nullable Class<T> type) throws BeansException {
		assertBeanFactoryActive();
		return getBeanFactory().getBeansOfType(type);
	}

	@Override
	public <T> Map<String, T> getBeansOfType(@Nullable Class<T> type, boolean includeNonSingletons, boolean allowEagerInit)
			throws BeansException {

		assertBeanFactoryActive();
		return getBeanFactory().getBeansOfType(type, includeNonSingletons, allowEagerInit);
	}

	@Override
	public String[] getBeanNamesForAnnotation(Class<? extends Annotation> annotationType) {
		assertBeanFactoryActive();
		return getBeanFactory().getBeanNamesForAnnotation(annotationType);
	}

	@Override
	public Map<String, Object> getBeansWithAnnotation(Class<? extends Annotation> annotationType)
			throws BeansException {

		assertBeanFactoryActive();
		return getBeanFactory().getBeansWithAnnotation(annotationType);
	}

	@Override
	@Nullable
	public <A extends Annotation> A findAnnotationOnBean(String beanName, Class<A> annotationType)
			throws NoSuchBeanDefinitionException {

		assertBeanFactoryActive();
		return getBeanFactory().findAnnotationOnBean(beanName, annotationType);
	}


	//---------------------------------------------------------------------
	// Implementation of HierarchicalBeanFactory interface
	// 实现 HierarchicalBeanFactory(层次性BeanFactory) 接口
	//---------------------------------------------------------------------

	@Override
	@Nullable
	public BeanFactory getParentBeanFactory() {
		return getParent();
	}

	@Override
	public boolean containsLocalBean(String name) {
		return getBeanFactory().containsLocalBean(name);
	}

	/**
	 * Return the internal bean factory of the parent context if it implements
	 * ConfigurableApplicationContext; else, return the parent context itself.
	 * @see org.springframework.context.ConfigurableApplicationContext#getBeanFactory
	 */
	// 如果实现了 ConfigurableApplicationContext，则返回父上下文的内部 bean 工厂； 否则，返回父上下文本身
	@Nullable
	protected BeanFactory getInternalParentBeanFactory() {
		return (getParent() instanceof ConfigurableApplicationContext ?
				((ConfigurableApplicationContext) getParent()).getBeanFactory() : getParent());
	}


	//---------------------------------------------------------------------
	// Implementation of MessageSource interface
	// MessageSource 接口的实现
	//---------------------------------------------------------------------

	// 使用委派模式：委派给本地 bean
	@Override
	public String getMessage(String code, @Nullable Object[] args, @Nullable String defaultMessage, Locale locale) {
		return getMessageSource().getMessage(code, args, defaultMessage, locale);
	}

	@Override
	public String getMessage(String code, @Nullable Object[] args, Locale locale) throws NoSuchMessageException {
		return getMessageSource().getMessage(code, args, locale);
	}

	@Override
	public String getMessage(MessageSourceResolvable resolvable, Locale locale) throws NoSuchMessageException {
		return getMessageSource().getMessage(resolvable, locale);
	}

	/**
	 * Return the internal MessageSource used by the context.
	 * @return the internal MessageSource (never {@code null})
	 * @throws IllegalStateException if the context has not been initialized yet
	 */
	// 返回上下文使用的内部 MessageSource
	private MessageSource getMessageSource() throws IllegalStateException {
		if (this.messageSource == null) {
			throw new IllegalStateException("MessageSource not initialized - " +
					"call 'refresh' before accessing messages via the context: " + this);
		}
		return this.messageSource;
	}

	/**
	 * Return the internal message source of the parent context if it is an
	 * AbstractApplicationContext too; else, return the parent context itself.
	 */
	// 如果父上下文也是 AbstractApplicationContext，则返回父上下文的内部消息源； 否则，返回父上下文本身
	@Nullable
	protected MessageSource getInternalParentMessageSource() {
		return (getParent() instanceof AbstractApplicationContext ?
				((AbstractApplicationContext) getParent()).messageSource : getParent());
	}


	//---------------------------------------------------------------------
	// Implementation of ResourcePatternResolver interface
	//---------------------------------------------------------------------

	@Override
	public Resource[] getResources(String locationPattern) throws IOException {
		return this.resourcePatternResolver.getResources(locationPattern);
	}


	//---------------------------------------------------------------------
	// Implementation of Lifecycle interface
	//---------------------------------------------------------------------

	// 发布 ContextStartedEvent 事件
	// 13.狭义的Spring 应用上下文启动阶段（非必须调用）生命周期的补充
	@Override
	public void start() {
		// a.启动 LifecycleProcessor
		getLifecycleProcessor().start();
		// b.发布 Spring 应用上下文已启动事件 - ContextStartedEvent
		publishEvent(new ContextStartedEvent(this));
	}

	// 发布 ContextStoppedEvent 事件
	// 14.Spring 应用上下文停止阶段 生命周期的补充
	@Override
	public void stop() {
		// a.停止 LifecycleProcessor
		getLifecycleProcessor().stop();
		// b.发布 Spring 应用上下文已停止事件 - ContextStoppedEvent
		publishEvent(new ContextStoppedEvent(this));
	}

	@Override
	public boolean isRunning() {
		return (this.lifecycleProcessor != null && this.lifecycleProcessor.isRunning());
	}


	//---------------------------------------------------------------------
	// Abstract methods that must be implemented by subclasses
	//---------------------------------------------------------------------

	/**
	 * Subclasses must implement this method to perform the actual configuration load.
	 * The method is invoked by {@link #refresh()} before any other initialization work.
	 * <p>A subclass will either create a new bean factory and hold a reference to it,
	 * or return a single BeanFactory instance that it holds. In the latter case, it will
	 * usually throw an IllegalStateException if refreshing the context more than once.
	 * @throws BeansException if initialization of the bean factory failed
	 * @throws IllegalStateException if already initialized and multiple refresh
	 * attempts are not supported
	 */
	protected abstract void refreshBeanFactory() throws BeansException, IllegalStateException;

	/**
	 * Subclasses must implement this method to release their internal bean factory.
	 * This method gets invoked by {@link #close()} after all other shutdown work.
	 * <p>Should never throw an exception but rather log shutdown failures.
	 */
	protected abstract void closeBeanFactory();

	/**
	 * Subclasses must return their internal bean factory here. They should implement the
	 * lookup efficiently, so that it can be called repeatedly without a performance penalty.
	 * <p>Note: Subclasses should check whether the context is still active before
	 * returning the internal bean factory. The internal factory should generally be
	 * considered unavailable once the context has been closed.
	 * @return this application context's internal bean factory (never {@code null})
	 * @throws IllegalStateException if the context does not hold an internal bean factory yet
	 * (usually if {@link #refresh()} has never been called) or if the context has been
	 * closed already
	 * @see #refreshBeanFactory()
	 * @see #closeBeanFactory()
	 */
	@Override
	public abstract ConfigurableListableBeanFactory getBeanFactory() throws IllegalStateException;


	/**
	 * Return information about this context.
	 */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(getDisplayName());
		sb.append(", started on ").append(new Date(getStartupDate()));
		ApplicationContext parent = getParent();
		if (parent != null) {
			sb.append(", parent: ").append(parent.getDisplayName());
		}
		return sb.toString();
	}

}
