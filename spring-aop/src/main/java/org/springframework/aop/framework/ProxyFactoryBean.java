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

package org.springframework.aop.framework;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.aopalliance.aop.Advice;
import org.aopalliance.intercept.Interceptor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aop.Advisor;
import org.springframework.aop.TargetSource;
import org.springframework.aop.framework.adapter.AdvisorAdapterRegistry;
import org.springframework.aop.framework.adapter.GlobalAdvisorAdapterRegistry;
import org.springframework.aop.framework.adapter.UnknownAdviceTypeException;
import org.springframework.aop.target.SingletonTargetSource;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.FactoryBeanNotInitializedException;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;

/**
 * {@link org.springframework.beans.factory.FactoryBean} implementation that builds an
 * AOP proxy based on beans in Spring {@link org.springframework.beans.factory.BeanFactory}.
 *
 * <p>{@link org.aopalliance.intercept.MethodInterceptor MethodInterceptors} and
 * {@link org.springframework.aop.Advisor Advisors} are identified by a list of bean
 * names in the current bean factory, specified through the "interceptorNames" property.
 * The last entry in the list can be the name of a target bean or a
 * {@link org.springframework.aop.TargetSource}; however, it is normally preferable
 * to use the "targetName"/"target"/"targetSource" properties instead.
 *
 * <p>Global interceptors and advisors can be added at the factory level. The specified
 * ones are expanded in an interceptor list where an "xxx*" entry is included in the
 * list, matching the given prefix with the bean names (e.g. "global*" would match
 * both "globalBean1" and "globalBean2", "*" all defined interceptors). The matching
 * interceptors get applied according to their returned order value, if they implement
 * the {@link org.springframework.core.Ordered} interface.
 *
 * <p>Creates a JDK proxy when proxy interfaces are given, and a CGLIB proxy for the
 * actual target class if not. Note that the latter will only work if the target class
 * does not have final methods, as a dynamic subclass will be created at runtime.
 *
 * <p>It's possible to cast a proxy obtained from this factory to {@link Advised},
 * or to obtain the ProxyFactoryBean reference and programmatically manipulate it.
 * This won't work for existing prototype references, which are independent. However,
 * it will work for prototypes subsequently obtained from the factory. Changes to
 * interception will work immediately on singletons (including existing references).
 * However, to change interfaces or target it's necessary to obtain a new instance
 * from the factory. This means that singleton instances obtained from the factory
 * do not have the same object identity. However, they do have the same interceptors
 * and target, and changing any reference will change all objects.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see #setInterceptorNames
 * @see #setProxyInterfaces
 * @see org.aopalliance.intercept.MethodInterceptor
 * @see org.springframework.aop.Advisor
 * @see Advised
 */
// FactoryBean实现，它基于 Spring BeanFactory bean 构建 AOP 代理。
//
// MethodInterceptors 和 Advisors 由当前 bean 工厂中的 bean 名称列表标识，
// 通过“interceptorNames”属性指定。 列表中的最后一个条目可以是目标 bean 的名称或 TargetSource ；
// 但是，通常最好改用“targetName”/“target”/“targetSource”属性。
//
// 可以在工厂级别添加全局拦截器和顾问。 指定的在拦截器列表中展开，其中包含“xxx*”条目，将给定前缀
// 与 bean 名称匹配（例如，“global*”将匹配“globalBean1”和“globalBean2”，“*”所有定义的拦截器）。
// 如果匹配的拦截器实现了org.springframework.core.Ordered接口，则根据它们返回的订单值应用匹配的拦截器。
//
// 给定代理接口时创建一个 JDK 代理，如果没有，则为实际目标类创建一个 CGLIB 代理。
// 请注意，后者仅在目标类没有最终方法时才有效，因为将在运行时创建动态子类。
//
// 可以将从该工厂获得的代理 Advised 为 Advised ，或者获取 ProxyFactoryBean 引用并以编程方式对其进行操作。
// 这不适用于现有的原型参考，它们是独立的。 但是，它将适用于随后从工厂获得的原型。 对拦截的更改将
// 立即对单例（包括现有引用）起作用。 但是，要更改接口或目标，必须从工厂获取新实例。 这意味着从工厂
// 获得的单例实例不具有相同的对象标识。 但是，它们确实具有相同的拦截器和目标，并且更改任何引用都会更改所有对象
//
// XML 配置驱动 - 创建 AOP 代理，FactoryBean 是一种特殊生成 bean definition 的方式，
// 它和传统的 bean 是一种间接的通过工厂的方式来进行创建 bean.通过调用 FactoryBean.getObject()来进行访问
//
// ProxyFactoryBean 和 ProxyFactory 的实现大同小异，前者更适合于 IoC 容器，后者则可以脱离 IoC 容器来使用
//
// AOP 和 IoC 打通的核心 API ProxyFactoryBean
//
// FactoryBean 与 BeanFactory 区别：FactoryBean 是一种特殊的 bean,它是生成 bean 的一种动态形式，
@SuppressWarnings("serial")
public class ProxyFactoryBean extends ProxyCreatorSupport
		implements FactoryBean<Object>, BeanClassLoaderAware, BeanFactoryAware {

	/**
	 * This suffix in a value in an interceptor list indicates to expand globals.
	 */
	// 拦截器列表中的值中的此后缀表示扩展全局变量
	public static final String GLOBAL_SUFFIX = "*";


	protected final Log logger = LogFactory.getLog(getClass());

	// 定义拦截器 bean 的名称，xml 中可以逗号分割
	@Nullable
	private String[] interceptorNames;

	// 代理 bean 的名称，可以通过依赖查找的方式来查找相应的对象
	// AOP 中关联 Spring IoC 的一个 bean,进行依赖查找，
	// 对于框架内部而言都是依赖查找，但是对于外部应用都是依赖注入，它底层没有自己注入自己的能力
	// Spring Boot 就是外部应用，它有依赖注入能力，它底层是 Spring Framework，它的部分组件具有依赖注入能力
	@Nullable
	private String targetName;

	private boolean autodetectInterfaces = true;

	// 默认是单例
	private boolean singleton = true;

	// 定义 顾问适配注册表
	private AdvisorAdapterRegistry advisorAdapterRegistry = GlobalAdvisorAdapterRegistry.getInstance();

	// 非冻结状态
	private boolean freezeProxy = false;

	@Nullable
	private transient ClassLoader proxyClassLoader = ClassUtils.getDefaultClassLoader();

	private transient boolean classLoaderConfigured = false;

	// 定义工厂
	@Nullable
	private transient BeanFactory beanFactory;

	/** Whether the advisor chain has already been initialized. */
	// 顾问链是否已经初始化
	private boolean advisorChainInitialized = false;

	/** If this is a singleton, the cached singleton proxy instance. */
	// 如果这是一个单例，则缓存的单例代理实例
	@Nullable
	private Object singletonInstance;


	/**
	 * Set the names of the interfaces we're proxying. If no interface
	 * is given, a CGLIB for the actual class will be created.
	 * <p>This is essentially equivalent to the "setInterfaces" method,
	 * but mirrors TransactionProxyFactoryBean's "setProxyInterfaces".
	 * @see #setInterfaces
	 * @see AbstractSingletonProxyFactoryBean#setProxyInterfaces
	 */
	// 设置我们正在代理的接口的名称。 如果没有给出接口，则为实际类创建一个 CGLIB。
	// 这本质上等同于“setInterfaces”方法，但反映了 TransactionProxyFactoryBean 的“setProxyInterfaces”。
	public void setProxyInterfaces(Class<?>[] proxyInterfaces) throws ClassNotFoundException {
		setInterfaces(proxyInterfaces);
	}

	/**
	 * Set the list of Advice/Advisor bean names. This must always be set
	 * to use this factory bean in a bean factory.
	 * <p>The referenced beans should be of type Interceptor, Advisor or Advice
	 * The last entry in the list can be the name of any bean in the factory.
	 * If it's neither an Advice nor an Advisor, a new SingletonTargetSource
	 * is added to wrap it. Such a target bean cannot be used if the "target"
	 * or "targetSource" or "targetName" property is set, in which case the
	 * "interceptorNames" array must contain only Advice/Advisor bean names.
	 * <p><b>NOTE: Specifying a target bean as final name in the "interceptorNames"
	 * list is deprecated and will be removed in a future Spring version.</b>
	 * Use the {@link #setTargetName "targetName"} property instead.
	 * @see org.aopalliance.intercept.MethodInterceptor
	 * @see org.springframework.aop.Advisor
	 * @see org.aopalliance.aop.Advice
	 * @see org.springframework.aop.target.SingletonTargetSource
	 */
	// 设置 Advice/Advisor bean 名称列表。 这必须始终设置为在 bean 工厂中使用此工厂 bean。
	//
	// 引用的 bean 应该是 Interceptor、Advisor 或 Advice 类型。列表中的最后一个条目
	// 可以是工厂中任何 bean 的名称。 如果它既不是 Advice 也不是 Advisor，则添加一个
	// 新的 SingletonTargetSource 来包装它。 如果设置了“target”或“targetSource”
	// 或“targetName”属性，则无法使用此类目标 bean，在这种情况下，“interceptorNames”数组
	// 必须仅包含 Advice/Advisor bean 名称。
	//
	// 注意：在“interceptorNames”列表中将目标 bean 指定为最终名称已被弃用，并将在
	// 未来的 Spring 版本中删除。 请改用"targetName"属性。
	public void setInterceptorNames(String... interceptorNames) {
		this.interceptorNames = interceptorNames;
	}

	/**
	 * Set the name of the target bean. This is an alternative to specifying
	 * the target name at the end of the "interceptorNames" array.
	 * <p>You can also specify a target object or a TargetSource object
	 * directly, via the "target"/"targetSource" property, respectively.
	 * @see #setInterceptorNames(String[])
	 * @see #setTarget(Object)
	 * @see #setTargetSource(org.springframework.aop.TargetSource)
	 */
	// 设置目标 bean 的名称。 这是在 “interceptorNames” 数组末尾指定目标名称的替代方法。
	// 您还可以分别通过 “target”/“targetSource” 属性直接指定目标对象或 TargetSource 对象。
	public void setTargetName(String targetName) {
		this.targetName = targetName;
	}

	/**
	 * Set whether to autodetect proxy interfaces if none specified.
	 * <p>Default is "true". Turn this flag off to create a CGLIB
	 * proxy for the full target class if no interfaces specified.
	 * @see #setProxyTargetClass
	 */
	// 如果未指定，则设置是否自动检测代理接口。
	// 默认为“真”。 如果未指定接口，请关闭此标志以为完整的目标类创建 CGLIB 代理
	public void setAutodetectInterfaces(boolean autodetectInterfaces) {
		this.autodetectInterfaces = autodetectInterfaces;
	}

	/**
	 * Set the value of the singleton property. Governs whether this factory
	 * should always return the same proxy instance (which implies the same target)
	 * or whether it should return a new prototype instance, which implies that
	 * the target and interceptors may be new instances also, if they are obtained
	 * from prototype bean definitions. This allows for fine control of
	 * independence/uniqueness in the object graph.
	 */
	// 设置单例属性的值。 管理这个工厂是否应该总是返回相同的代理实例（这意味着相同的目标）或者
	// 它是否应该返回一个新的原型实例，这意味着目标和拦截器也可能是新的实例，如果它们是从
	// 原型 bean 定义中获得的。 这允许对对象图中的独立性/唯一性进行精细控制
	public void setSingleton(boolean singleton) {
		this.singleton = singleton;
	}

	/**
	 * Specify the AdvisorAdapterRegistry to use.
	 * Default is the global AdvisorAdapterRegistry.
	 * @see org.springframework.aop.framework.adapter.GlobalAdvisorAdapterRegistry
	 */
	// 指定要使用的 AdvisorAdapterRegistry。 默认为全局 AdvisorAdapterRegistry。
	public void setAdvisorAdapterRegistry(AdvisorAdapterRegistry advisorAdapterRegistry) {
		this.advisorAdapterRegistry = advisorAdapterRegistry;
	}

	@Override
	public void setFrozen(boolean frozen) {
		this.freezeProxy = frozen;
	}

	/**
	 * Set the ClassLoader to generate the proxy class in.
	 * <p>Default is the bean ClassLoader, i.e. the ClassLoader used by the
	 * containing BeanFactory for loading all bean classes. This can be
	 * overridden here for specific proxies.
	 */
	// 设置 ClassLoader 以在其中生成代理类。
	// 默认为 bean ClassLoader，即包含 BeanFactory 用于加载所有 bean 类的 ClassLoader。
	// 这可以在此处为特定代理覆盖。
	public void setProxyClassLoader(@Nullable ClassLoader classLoader) {
		this.proxyClassLoader = classLoader;
		this.classLoaderConfigured = (classLoader != null);
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		if (!this.classLoaderConfigured) {
			this.proxyClassLoader = classLoader;
		}
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
		checkInterceptorNames();
	}


	/**
	 * Return a proxy. Invoked when clients obtain beans from this factory bean.
	 * Create an instance of the AOP proxy to be returned by this factory.
	 * The instance will be cached for a singleton, and create on each call to
	 * {@code getObject()} for a proxy.
	 * @return a fresh AOP proxy reflecting the current state of this factory
	 */
	// 返回代理。 当客户端从这个工厂 bean 获取 bean 时调用。 创建该工厂返回的 AOP 代理的实例。 该实例将为单例缓存，并在每次调用getObject()为代理创建。
	// 返回值：反映该工厂当前状态的新 AOP 代理
	@Override
	@Nullable
	public Object getObject() throws BeansException {
		// 初始化 Advisor 链
		initializeAdvisorChain();
		if (isSingleton()) {
			return getSingletonInstance();
		}
		else {
			if (this.targetName == null) {
				logger.info("Using non-singleton proxies with singleton targets is often undesirable. " +
						"Enable prototype proxies by setting the 'targetName' property.");
			}
			return newPrototypeInstance();
		}
	}

	/**
	 * Return the type of the proxy. Will check the singleton instance if
	 * already created, else fall back to the proxy interface (in case of just
	 * a single one), the target bean type, or the TargetSource's target class.
	 * @see org.springframework.aop.TargetSource#getTargetClass
	 */
	// 返回代理的类型。 将检查单例实例是否已创建，否则回退到代理接口（如果只有一个）、
	// 目标 bean 类型或 TargetSource 的目标类。
	@Override
	public Class<?> getObjectType() {
		synchronized (this) { // 优化操作：singletonInstance 创建好之后就会缓存，这里直接返回缓存
			if (this.singletonInstance != null) {
				return this.singletonInstance.getClass();
			}
		}
		// 获取来自父类的代理接口，框架内部去判断当前 targetObject 它实现的所有的接口作为一个数组存储
		Class<?>[] ifcs = getProxiedInterfaces();
		if (ifcs.length == 1) {
			// 如果是一个接口直接返回
			return ifcs[0];
		}
		else if (ifcs.length > 1) {
			// 如果是多个接口，将其变成成组合接口
			return createCompositeInterface(ifcs);
		}
		else if (this.targetName != null && this.beanFactory != null) {
			// 从内部工厂中获取目标对象类型
			// 如果没有接口，就会对目标对象进行依赖查找
			return this.beanFactory.getType(this.targetName);
		}
		else {
			// 以上条件都不成立直接返回 targetClass
			return getTargetClass();
		}
	}

	@Override
	public boolean isSingleton() {
		return this.singleton;
	}


	/**
	 * Create a composite interface Class for the given interfaces,
	 * implementing the given interfaces in one single Class.
	 * <p>The default implementation builds a JDK proxy class for the
	 * given interfaces.
	 * @param interfaces the interfaces to merge
	 * @return the merged interface as Class
	 * @see java.lang.reflect.Proxy#getProxyClass
	 */
	// 为给定接口创建一个组合接口类，在一个类中实现给定接口。
	// 默认实现为给定接口构建一个 JDK 代理类。
	// 参形：
	//			interfaces - 要合并的接口
	// 返回值：
	//			合并后的接口为 Class
	protected Class<?> createCompositeInterface(Class<?>[] interfaces) {
		return ClassUtils.createCompositeInterface(interfaces, this.proxyClassLoader);
	}

	/**
	 * Return the singleton instance of this class's proxy object,
	 * lazily creating it if it hasn't been created already.
	 * @return the shared singleton proxy
	 */
	// 返回此类代理对象的单例实例，如果尚未创建它，则延迟创建它。
	// 返回值：共享单例代理
	private synchronized Object getSingletonInstance() {
		if (this.singletonInstance == null) {
			this.targetSource = freshTargetSource();
			if (this.autodetectInterfaces && getProxiedInterfaces().length == 0 && !isProxyTargetClass()) {
				// Rely on AOP infrastructure to tell us what interfaces to proxy.
				// 依靠 AOP 基础设施来告诉我们代理哪些接口
				Class<?> targetClass = getTargetClass();
				if (targetClass == null) {
					throw new FactoryBeanNotInitializedException("Cannot determine target class for proxy");
				}
				setInterfaces(ClassUtils.getAllInterfacesForClass(targetClass, this.proxyClassLoader));
			}
			// Initialize the shared singleton instance.
			// 初始化共享单例实例
			super.setFrozen(this.freezeProxy);
			// 调用标准实现
			this.singletonInstance = getProxy(createAopProxy());
		}
		return this.singletonInstance;
	}

	/**
	 * Create a new prototype instance of this class's created proxy object,
	 * backed by an independent AdvisedSupport configuration.
	 * @return a totally independent proxy, whose advice we may manipulate in isolation
	 */
	// 创建此类的已创建代理对象的新原型实例，由独立的 AdvisedSupport 配置支持。
	// 返回值：一个完全独立的代理人，我们可以单独操纵他的建议
	private synchronized Object newPrototypeInstance() {
		// In the case of a prototype, we need to give the proxy
		// an independent instance of the configuration.
		// In this case, no proxy will have an instance of this object's configuration,
		// but will have an independent copy.
		// 在原型的情况下，我们需要给代理一个独立的配置实例。
		// 在这种情况下，没有代理将拥有此对象配置的实例，但会有独立的副本。
		ProxyCreatorSupport copy = new ProxyCreatorSupport(getAopProxyFactory());

		// The copy needs a fresh advisor chain, and a fresh TargetSource.
		// 副本需要一个新的顾问链和一个新的 TargetSource
		TargetSource targetSource = freshTargetSource();
		copy.copyConfigurationFrom(this, targetSource, freshAdvisorChain());
		if (this.autodetectInterfaces && getProxiedInterfaces().length == 0 && !isProxyTargetClass()) {
			// Rely on AOP infrastructure to tell us what interfaces to proxy.
			// 依靠 AOP 基础设施来告诉我们代理哪些接口
			Class<?> targetClass = targetSource.getTargetClass();
			if (targetClass != null) {
				copy.setInterfaces(ClassUtils.getAllInterfacesForClass(targetClass, this.proxyClassLoader));
			}
		}
		copy.setFrozen(this.freezeProxy);
		// 调用标准实现
		return getProxy(copy.createAopProxy());
	}

	/**
	 * Return the proxy object to expose.
	 * <p>The default implementation uses a {@code getProxy} call with
	 * the factory's bean class loader. Can be overridden to specify a
	 * custom class loader.
	 * @param aopProxy the prepared AopProxy instance to get the proxy from
	 * @return the proxy object to expose
	 * @see AopProxy#getProxy(ClassLoader)
	 */
	// 返回要公开的代理对象。
	// 默认实现使用带有工厂的 bean 类加载器的getProxy调用。 可以重写以指定自定义类加载器。
	// 参形：
	//			aopProxy - 准备好的 AopProxy 实例来获取代理
	// 返回值：
	//			要公开的代理对象
	protected Object getProxy(AopProxy aopProxy) {
		return aopProxy.getProxy(this.proxyClassLoader);
	}

	/**
	 * Check the interceptorNames list whether it contains a target name as final element.
	 * If found, remove the final name from the list and set it as targetName.
	 */
	// 检查interceptorNames 列表是否包含目标名称作为最终元素。 如果找到，则从列表中
	// 删除最终名称并将其设置为 targetName
	private void checkInterceptorNames() {
		if (!ObjectUtils.isEmpty(this.interceptorNames)) {
			String finalName = this.interceptorNames[this.interceptorNames.length - 1];
			if (this.targetName == null && this.targetSource == EMPTY_TARGET_SOURCE) {
				// The last name in the chain may be an Advisor/Advice or a target/TargetSource.
				// Unfortunately we don't know; we must look at type of the bean.
				if (!finalName.endsWith(GLOBAL_SUFFIX) && !isNamedBeanAnAdvisorOrAdvice(finalName)) {
					// The target isn't an interceptor.
					this.targetName = finalName;
					if (logger.isDebugEnabled()) {
						logger.debug("Bean with name '" + finalName + "' concluding interceptor chain " +
								"is not an advisor class: treating it as a target or TargetSource");
					}
					this.interceptorNames = Arrays.copyOf(this.interceptorNames, this.interceptorNames.length - 1);
				}
			}
		}
	}

	/**
	 * Look at bean factory metadata to work out whether this bean name,
	 * which concludes the interceptorNames list, is an Advisor or Advice,
	 * or may be a target.
	 * @param beanName bean name to check
	 * @return {@code true} if it's an Advisor or Advice
	 */
	// 查看 bean factory 元数据以确定这个 bean 名称（它包含在 interceptorNames 列表中）
	// 是 Advisor 还是 Advice，或者可能是一个目标。
	// 参形：
	//			beanName – 要检查的 bean 名称
	// 返回值：
	//			如果是顾问或建议，则为true
	private boolean isNamedBeanAnAdvisorOrAdvice(String beanName) {
		Assert.state(this.beanFactory != null, "No BeanFactory set");
		Class<?> namedBeanClass = this.beanFactory.getType(beanName);
		if (namedBeanClass != null) {
			return (Advisor.class.isAssignableFrom(namedBeanClass) || Advice.class.isAssignableFrom(namedBeanClass));
		}
		// Treat it as an target bean if we can't tell.
		// 如果我们无法判断，请将其视为目标 bean
		if (logger.isDebugEnabled()) {
			logger.debug("Could not determine type of bean with name '" + beanName +
					"' - assuming it is neither an Advisor nor an Advice");
		}
		return false;
	}

	/**
	 * Create the advisor (interceptor) chain. Advisors that are sourced
	 * from a BeanFactory will be refreshed each time a new prototype instance
	 * is added. Interceptors added programmatically through the factory API
	 * are unaffected by such changes.
	 */
	// 创建顾问（拦截器）链。 每次添加新的原型实例时，都会刷新来自 BeanFactory 的顾问。
	// 通过工厂 API 以编程方式添加的拦截器不受此类更改的影响
	private synchronized void initializeAdvisorChain() throws AopConfigException, BeansException {
		if (this.advisorChainInitialized) {
			return;
		}

		if (!ObjectUtils.isEmpty(this.interceptorNames)) {
			if (this.beanFactory == null) {
				throw new IllegalStateException("No BeanFactory available anymore (probably due to serialization) " +
						"- cannot resolve interceptor names " + Arrays.asList(this.interceptorNames));
			}

			// Globals can't be last unless we specified a targetSource using the property...
			// 除非我们使用属性指定 targetSource，否则 Globals 不能是最后一个...
			if (this.interceptorNames[this.interceptorNames.length - 1].endsWith(GLOBAL_SUFFIX) &&
					this.targetName == null && this.targetSource == EMPTY_TARGET_SOURCE) {
				throw new AopConfigException("Target required after globals");
			}

			// Materialize interceptor chain from bean names.
			// 从 bean 名称实现拦截器链
			for (String name : this.interceptorNames) {
				// 如果是以 * 结尾的，即模糊查找
				if (name.endsWith(GLOBAL_SUFFIX)) {
					if (!(this.beanFactory instanceof ListableBeanFactory)) {
						throw new AopConfigException(
								"Can only use global advisors or interceptors with a ListableBeanFactory");
					}
					addGlobalAdvisors((ListableBeanFactory) this.beanFactory,
							name.substring(0, name.length() - GLOBAL_SUFFIX.length()));
				}

				else {
					// If we get here, we need to add a named interceptor.
					// We must check if it's a singleton or prototype.
					// 如果我们到达这里，我们需要添加一个命名拦截器。
					// 我们必须检查它是单例还是原型。
					Object advice;
					if (this.singleton || this.beanFactory.isSingleton(name)) { // 处理单例
						// Add the real Advisor/Advice to the chain.
						// 将真正的 AdvisorAdvice 添加到链中。依赖查找获取 Advice
						advice = this.beanFactory.getBean(name);
					}
					else {
						// It's a prototype Advice or Advisor: replace with a prototype.
						// Avoid unnecessary creation of prototype bean just for advisor chain initialization.
						// 它是原型 Advice 或 Advisor：替换为原型。
						// 避免仅为顾问链初始化而不必要地创建原型 bean。
						advice = new PrototypePlaceholderAdvisor(name); // 处理配置 bean 不存在，暂时当作占位符使用
					}
					addAdvisorOnChainCreation(advice);
				}
			}
		}

		this.advisorChainInitialized = true;
	}


	/**
	 * Return an independent advisor chain.
	 * We need to do this every time a new prototype instance is returned,
	 * to return distinct instances of prototype Advisors and Advices.
	 */
	// 返回一个独立的顾问链。
	// 每次返回新的原型实例时，我们都需要这样做，以返回原型顾问和建议的不同实例。
	private List<Advisor> freshAdvisorChain() {
		Advisor[] advisors = getAdvisors();
		List<Advisor> freshAdvisors = new ArrayList<>(advisors.length);
		for (Advisor advisor : advisors) {
			if (advisor instanceof PrototypePlaceholderAdvisor) {
				PrototypePlaceholderAdvisor pa = (PrototypePlaceholderAdvisor) advisor;
				if (logger.isDebugEnabled()) {
					logger.debug("Refreshing bean named '" + pa.getBeanName() + "'");
				}
				// Replace the placeholder with a fresh prototype instance resulting from a getBean lookup
				// 用 getBean 查找产生的新原型实例替换占位符
				if (this.beanFactory == null) {
					throw new IllegalStateException("No BeanFactory available anymore (probably due to " +
							"serialization) - cannot resolve prototype advisor '" + pa.getBeanName() + "'");
				}
				Object bean = this.beanFactory.getBean(pa.getBeanName());
				Advisor refreshedAdvisor = namedBeanToAdvisor(bean);
				freshAdvisors.add(refreshedAdvisor);
			}
			else {
				// Add the shared instance.
				// 添加共享实例
				freshAdvisors.add(advisor);
			}
		}
		return freshAdvisors;
	}

	/**
	 * Add all global interceptors and pointcuts.
	 */
	// 添加所有全局拦截器和切入点
	private void addGlobalAdvisors(ListableBeanFactory beanFactory, String prefix) {
		String[] globalAdvisorNames =
				BeanFactoryUtils.beanNamesForTypeIncludingAncestors(beanFactory, Advisor.class);
		String[] globalInterceptorNames =
				BeanFactoryUtils.beanNamesForTypeIncludingAncestors(beanFactory, Interceptor.class);
		if (globalAdvisorNames.length > 0 || globalInterceptorNames.length > 0) {
			List<Object> beans = new ArrayList<>(globalAdvisorNames.length + globalInterceptorNames.length);
			for (String name : globalAdvisorNames) {
				if (name.startsWith(prefix)) {
					// 通过依赖查找逐个添加
					beans.add(beanFactory.getBean(name));
				}
			}
			for (String name : globalInterceptorNames) {
				if (name.startsWith(prefix)) {
					// // 通过依赖查找逐个添加
					beans.add(beanFactory.getBean(name));
				}
			}
			// 排序，Advisor 是可以排序的
			AnnotationAwareOrderComparator.sort(beans);
			for (Object bean : beans) {
				addAdvisorOnChainCreation(bean);
			}
		}
	}

	/**
	 * Invoked when advice chain is created.
	 * <p>Add the given advice, advisor or object to the interceptor list.
	 * Because of these three possibilities, we can't type the signature
	 * more strongly.
	 * @param next advice, advisor or target object
	 */
	// 创建建议链时调用。
	// 将给定的建议、顾问或对象添加到拦截器列表中。 由于这三种可能性，我们不能更强烈地键入签名。
	// 参形：
	//			next – 建议、顾问或目标对象
	private void addAdvisorOnChainCreation(Object next) {
		// We need to convert to an Advisor if necessary so that our source reference
		// matches what we find from superclass interceptors.
		// 如有必要，我们需要转换为顾问，以便我们的源引用与我们从超类拦截器中找到的内容相匹配。
		addAdvisor(namedBeanToAdvisor(next));
	}

	/**
	 * Return a TargetSource to use when creating a proxy. If the target was not
	 * specified at the end of the interceptorNames list, the TargetSource will be
	 * this class's TargetSource member. Otherwise, we get the target bean and wrap
	 * it in a TargetSource if necessary.
	 */
	// 返回创建代理时要使用的 TargetSource。如果在interceptorNames 列表的末尾未指定目标，
	// 则TargetSource 将是此类的TargetSource 成员。否则，我们获取目标 bean 并在必要时将其包装在 TargetSource 中
	private TargetSource freshTargetSource() {
		if (this.targetName == null) {
			// Not refreshing target: bean name not specified in 'interceptorNames'
			// 不刷新目标：“interceptorNames”中未指定 bean 名称
			return this.targetSource;
		}
		else {
			if (this.beanFactory == null) {
				throw new IllegalStateException("No BeanFactory available anymore (probably due to serialization) " +
						"- cannot resolve target with name '" + this.targetName + "'");
			}
			if (logger.isDebugEnabled()) {
				logger.debug("Refreshing target with name '" + this.targetName + "'");
			}
			Object target = this.beanFactory.getBean(this.targetName);
			return (target instanceof TargetSource ? (TargetSource) target : new SingletonTargetSource(target));
		}
	}

	/**
	 * Convert the following object sourced from calling getBean() on a name in the
	 * interceptorNames array to an Advisor or TargetSource.
	 */
	// 将以下源自对 interceptorNames 数组中的名称调用 getBean() 的对象转换为 Advisor 或 TargetSource
	private Advisor namedBeanToAdvisor(Object next) {
		try {
			return this.advisorAdapterRegistry.wrap(next);
		}
		catch (UnknownAdviceTypeException ex) {
			// We expected this to be an Advisor or Advice,
			// but it wasn't. This is a configuration error.
			throw new AopConfigException("Unknown advisor type " + next.getClass() +
					"; can only include Advisor or Advice type beans in interceptorNames chain " +
					"except for last entry which may also be target instance or TargetSource", ex);
		}
	}

	/**
	 * Blow away and recache singleton on an advice change.
	 */
	// 在建议更改时吹走并重新缓存单例
	@Override
	protected void adviceChanged() {
		super.adviceChanged();
		if (this.singleton) {
			logger.debug("Advice has changed; re-caching singleton instance");
			synchronized (this) {
				this.singletonInstance = null;
			}
		}
	}


	//---------------------------------------------------------------------
	// Serialization support
	//---------------------------------------------------------------------

	private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
		// Rely on default serialization; just initialize state after deserialization.
		// 依赖默认序列化；反序列化后只需初始化状态。
		ois.defaultReadObject();

		// Initialize transient fields.
		// 初始化瞬态字段。
		this.proxyClassLoader = ClassUtils.getDefaultClassLoader();
	}


	/**
	 * Used in the interceptor chain where we need to replace a bean with a prototype
	 * on creating a proxy.
	 */
	// 在拦截器链中使用，我们需要在创建代理时用原型替换 bean
	private static class PrototypePlaceholderAdvisor implements Advisor, Serializable {

		private final String beanName;

		private final String message;

		public PrototypePlaceholderAdvisor(String beanName) {
			this.beanName = beanName;
			this.message = "Placeholder for prototype Advisor/Advice with bean name '" + beanName + "'";
		}

		public String getBeanName() {
			return this.beanName;
		}

		// 这个占位符不能长期使用，会抛出这个异常
		@Override
		public Advice getAdvice() {
			throw new UnsupportedOperationException("Cannot invoke methods: " + this.message);
		}

		@Override
		public boolean isPerInstance() {
			throw new UnsupportedOperationException("Cannot invoke methods: " + this.message);
		}

		@Override
		public String toString() {
			return this.message;
		}
	}

}
