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

package org.springframework.aop.framework.autoproxy;

import java.lang.reflect.Constructor;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.aopalliance.aop.Advice;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aop.Advisor;
import org.springframework.aop.Pointcut;
import org.springframework.aop.TargetSource;
import org.springframework.aop.framework.AopInfrastructureBean;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.framework.ProxyProcessorSupport;
import org.springframework.aop.framework.adapter.AdvisorAdapterRegistry;
import org.springframework.aop.framework.adapter.GlobalAdvisorAdapterRegistry;
import org.springframework.aop.target.SingletonTargetSource;
import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.SmartInstantiationAwareBeanPostProcessor;
import org.springframework.core.SmartClassLoader;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * {@link org.springframework.beans.factory.config.BeanPostProcessor} implementation
 * that wraps each eligible bean with an AOP proxy, delegating to specified interceptors
 * before invoking the bean itself.
 *
 * <p>This class distinguishes between "common" interceptors: shared for all proxies it
 * creates, and "specific" interceptors: unique per bean instance. There need not be any
 * common interceptors. If there are, they are set using the interceptorNames property.
 * As with {@link org.springframework.aop.framework.ProxyFactoryBean}, interceptors names
 * in the current factory are used rather than bean references to allow correct handling
 * of prototype advisors and interceptors: for example, to support stateful mixins.
 * Any advice type is supported for {@link #setInterceptorNames "interceptorNames"} entries.
 *
 * <p>Such auto-proxying is particularly useful if there's a large number of beans that
 * need to be wrapped with similar proxies, i.e. delegating to the same interceptors.
 * Instead of x repetitive proxy definitions for x target beans, you can register
 * one single such post processor with the bean factory to achieve the same effect.
 *
 * <p>Subclasses can apply any strategy to decide if a bean is to be proxied, e.g. by type,
 * by name, by definition details, etc. They can also return additional interceptors that
 * should just be applied to the specific bean instance. A simple concrete implementation is
 * {@link BeanNameAutoProxyCreator}, identifying the beans to be proxied via given names.
 *
 * <p>Any number of {@link TargetSourceCreator} implementations can be used to create
 * a custom target source: for example, to pool prototype objects. Auto-proxying will
 * occur even if there is no advice, as long as a TargetSourceCreator specifies a custom
 * {@link org.springframework.aop.TargetSource}. If there are no TargetSourceCreators set,
 * or if none matches, a {@link org.springframework.aop.target.SingletonTargetSource}
 * will be used by default to wrap the target bean instance.
 *
 * @author Juergen Hoeller
 * @author Rod Johnson
 * @author Rob Harrop
 * @since 13.10.2003
 * @see #setInterceptorNames
 * @see #getAdvicesAndAdvisorsForBean
 * @see BeanNameAutoProxyCreator
 * @see DefaultAdvisorAutoProxyCreator
 */
// org.springframework.beans.factory.config.BeanPostProcessor 实现，它使用 AOP 代理包装每个符合条件的 bean，
// 在调用 bean 本身之前委托给指定的拦截器。
//
// 此类区分“通用”拦截器：为它创建的所有代理共享，以及“特定”拦截器：每个 bean 实例唯一。 不需要任何常见的拦截器。
// 如果有，则使用interceptorNames 属性设置它们。 与 org.springframework.aop.framework.ProxyFactoryBean一样，
// 使用当前工厂中的拦截器名称而不是 bean 引用来允许正确处理原型顾问和拦截器：例如，支持有状态的混合。
// "interceptorNames"条目支持任何建议类型。
//
// 如果有大量 bean 需要用类似的代理包装，即委托给相同的拦截器，这种自动代理特别有用。 您可以向 bean 工厂注册一个这样的
// 后处理器来实现相同的效果，而不是为 x 个目标 bean 进行 x 个重复的代理定义。
//
// 子类可以应用任何策略来决定是否要代理 bean，例如按类型、按名称、按定义详细信息等。它们还可以返回应仅应用于特定 bean 实例的
// 附加拦截器。 一个简单的具体实现是BeanNameAutoProxyCreator ，通过给定名称标识要代理的 bean。
//
// 可以使用任意数量的 TargetSourceCreator 实现来创建自定义目标源：例如，池原型对象。 只要 TargetSourceCreator 指定
// 自定义 TargetSource ，即使没有建议也会发生自动代理。 如果没有设置 TargetSourceCreators，或者没有匹配项，则默认情况下
// 将使用 SingletonTargetSource 来包装目标 bean 实例。
//
// 自动化代理对象，与 IoC 整合时候会把相关的 bean 自动化代理，它是 IoC 容器的自动代理创建器。它实现 BeanFactoryAware
// 代表它自身就是一个 bean,这种 bean 本身就是 Infrastructure 即 Spring 内部的基础设施 bean.它自身通过 Aware 回调来注入
//
// 一旦要形成自动代理，创建代理本身的对象 AbstractAutoProxyCreator，不能是被代理的对象。不被代理的能力来自 ProxyProcessorSupport
// 排除逻辑存在两个地方，一个是 AbstractAutoProxyCreator#isInfrastructureClass,
// 另外一个在 ConfigurationClassUtils#checkConfigurationClassCandidate
@SuppressWarnings("serial")
public abstract class AbstractAutoProxyCreator extends ProxyProcessorSupport
		implements SmartInstantiationAwareBeanPostProcessor, BeanFactoryAware {

	/**
	 * Convenience constant for subclasses: Return value for "do not proxy".
	 * @see #getAdvicesAndAdvisorsForBean
	 */
	// 子类的便利常量：“不代理”的返回值
	@Nullable
	protected static final Object[] DO_NOT_PROXY = null;

	/**
	 * Convenience constant for subclasses: Return value for
	 * "proxy without additional interceptors, just the common ones".
	 * @see #getAdvicesAndAdvisorsForBean
	 */
	// 子类的便利常量：“没有额外拦截器的代理，只有普通拦截器”的返回值
	protected static final Object[] PROXY_WITHOUT_ADDITIONAL_INTERCEPTORS = new Object[0];


	/** Logger available to subclasses. */
	protected final Log logger = LogFactory.getLog(getClass());

	/** Default is global AdvisorAdapterRegistry. */
	// 默认为全局 AdvisorAdapterRegistry，帮助我们将 Advisor 变成多个 MethodInterceptor
	private AdvisorAdapterRegistry advisorAdapterRegistry = GlobalAdvisorAdapterRegistry.getInstance();

	/**
	 * Indicates whether or not the proxy should be frozen. Overridden from super
	 * to prevent the configuration from becoming frozen too early.
	 */
	// 指示是否应冻结代理。 从 super 覆盖以防止配置过早冻结
	private boolean freezeProxy = false;

	/** Default is no common interceptors. */
	// 默认是没有通用拦截器,拦截器名称
	private String[] interceptorNames = new String[0];

	private boolean applyCommonInterceptorsFirst = true;

	// 目标来源创建
	@Nullable
	private TargetSourceCreator[] customTargetSourceCreators;

	@Nullable
	private BeanFactory beanFactory;

	private final Set<String> targetSourcedBeans = Collections.newSetFromMap(new ConcurrentHashMap<>(16));

	// 缓存
	private final Map<Object, Object> earlyProxyReferences = new ConcurrentHashMap<>(16);

	private final Map<Object, Class<?>> proxyTypes = new ConcurrentHashMap<>(16);

	private final Map<Object, Boolean> advisedBeans = new ConcurrentHashMap<>(256);


	/**
	 * Set whether or not the proxy should be frozen, preventing advice
	 * from being added to it once it is created.
	 * <p>Overridden from the super class to prevent the proxy configuration
	 * from being frozen before the proxy is created.
	 */
	// 设置是否应该冻结代理，防止建议在创建后添加到它。
	// 从超类重写以防止在创建代理之前冻结代理配置
	@Override
	public void setFrozen(boolean frozen) {
		this.freezeProxy = frozen;
	}

	@Override
	public boolean isFrozen() {
		return this.freezeProxy;
	}

	/**
	 * Specify the {@link AdvisorAdapterRegistry} to use.
	 * <p>Default is the global {@link AdvisorAdapterRegistry}.
	 * @see org.springframework.aop.framework.adapter.GlobalAdvisorAdapterRegistry
	 */
	// 指定要使用的 AdvisorAdapterRegistry 。
	// 默认是全局 AdvisorAdapterRegistry 。
	public void setAdvisorAdapterRegistry(AdvisorAdapterRegistry advisorAdapterRegistry) {
		this.advisorAdapterRegistry = advisorAdapterRegistry;
	}

	/**
	 * Set custom {@code TargetSourceCreators} to be applied in this order.
	 * If the list is empty, or they all return null, a {@link SingletonTargetSource}
	 * will be created for each bean.
	 * <p>Note that TargetSourceCreators will kick in even for target beans
	 * where no advices or advisors have been found. If a {@code TargetSourceCreator}
	 * returns a {@link TargetSource} for a specific bean, that bean will be proxied
	 * in any case.
	 * <p>{@code TargetSourceCreators} can only be invoked if this post processor is used
	 * in a {@link BeanFactory} and its {@link BeanFactoryAware} callback is triggered.
	 * @param targetSourceCreators the list of {@code TargetSourceCreators}.
	 * Ordering is significant: The {@code TargetSource} returned from the first matching
	 * {@code TargetSourceCreator} (that is, the first that returns non-null) will be used.
	 */
	// 设置要按此顺序应用的自定义TargetSourceCreators 。 如果列表为空，或者它们都返回 null，
	// 则将为每个 bean 创建一个SingletonTargetSource 。
	//
	// 请注意，即使对于没有找到建议或顾问的目标 bean，TargetSourceCreators 也会启动。
	// 如果TargetSourceCreator返回特定 bean 的TargetSource ，则无论如何都会代理该 bean。
	//
	// 仅当在BeanFactory中使用此后处理器并触发其BeanFactoryAware回调时，
	// 才能调用TargetSourceCreators 。
	//
	// 参形：
	//				targetSourceCreators – TargetSourceCreators 列表。 排序很重要：将使用从第一个
	//				匹配的 TargetSource （即返回非 null 的第一个）返回的 TargetSourceCreator
	public void setCustomTargetSourceCreators(TargetSourceCreator... targetSourceCreators) {
		this.customTargetSourceCreators = targetSourceCreators;
	}

	/**
	 * Set the common interceptors. These must be bean names in the current factory.
	 * They can be of any advice or advisor type Spring supports.
	 * <p>If this property isn't set, there will be zero common interceptors.
	 * This is perfectly valid, if "specific" interceptors such as matching
	 * Advisors are all we want.
	 */
	// 设置常用拦截器。 这些必须是当前工厂中的 bean 名称。 它们可以是 Spring 支持的任何建议或顾问类型。
	// 如果未设置此属性，则公共拦截器将为零。 如果我们只需要“特定”拦截器（例如匹配顾问），这是完全有效的。
	public void setInterceptorNames(String... interceptorNames) {
		this.interceptorNames = interceptorNames;
	}

	/**
	 * Set whether the common interceptors should be applied before bean-specific ones.
	 * Default is "true"; else, bean-specific interceptors will get applied first.
	 */
	// 设置是否应在特定于 bean 的拦截器之前应用公共拦截器。 默认为“真”； 否则，将首先应用特定于 bean 的拦截器。
	public void setApplyCommonInterceptorsFirst(boolean applyCommonInterceptorsFirst) {
		this.applyCommonInterceptorsFirst = applyCommonInterceptorsFirst;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}

	/**
	 * Return the owning {@link BeanFactory}.
	 * May be {@code null}, as this post-processor doesn't need to belong to a bean factory.
	 */
	// 返回拥有的 BeanFactory 。 可能为null ，因为此后处理器不需要属于 bean 工厂。
	@Nullable
	protected BeanFactory getBeanFactory() {
		return this.beanFactory;
	}


	// 预测 bean 的类型，目标类型 beanClass 进来，它会做一个类型的转换。
	@Override
	@Nullable
	public Class<?> predictBeanType(Class<?> beanClass, String beanName) {
		if (this.proxyTypes.isEmpty()) {
			return null;
		}
		// 如果 Proxy type 存在的话
		Object cacheKey = getCacheKey(beanClass, beanName);
		// 返回代理类型
		return this.proxyTypes.get(cacheKey);
	}

	@Override
	@Nullable
	public Constructor<?>[] determineCandidateConstructors(Class<?> beanClass, String beanName) {
		return null;
	}

	// 解决循环依赖：在早期就将数据填充进去。
	@Override
	public Object getEarlyBeanReference(Object bean, String beanName) {
		Object cacheKey = getCacheKey(bean.getClass(), beanName);
		this.earlyProxyReferences.put(cacheKey, bean);
		return wrapIfNecessary(bean, beanName, cacheKey);
	}

	// 实例化前回调：Spring AOP 自动创建过程,利用了 Spring 的声明周期。
	@Override
	public Object postProcessBeforeInstantiation(Class<?> beanClass, String beanName) {
		Object cacheKey = getCacheKey(beanClass, beanName);

		if (!StringUtils.hasLength(beanName) || !this.targetSourcedBeans.contains(beanName)) {
			if (this.advisedBeans.containsKey(cacheKey)) {
				return null;
			}
			// 判断当前 bean 是不是 Spring 内部的 Infrastructure 基础组件(过滤掉内部基础组件好比人是不能抓
			// 自己头发将自己拎起来)，或者应该被跳过
			if (isInfrastructureClass(beanClass) || shouldSkip(beanClass, beanName)) {
				this.advisedBeans.put(cacheKey, Boolean.FALSE);
				return null;
			}
		}

		// Create proxy here if we have a custom TargetSource.
		// Suppresses unnecessary default instantiation of the target bean:
		// The TargetSource will handle target instances in a custom fashion.
		//
		// 如果我们有自定义 TargetSource，请在此处创建代理。抑制目标 bean 的不必要的默认实例化：
		// TargetSource 将以自定义方式处理目标实例。
		//
		// 获取相应的 source 对象，用于处理池化技术等，获取自定义的 TargetSource
		TargetSource targetSource = getCustomTargetSource(beanClass, beanName);
		if (targetSource != null) {
			if (StringUtils.hasLength(beanName)) {
				this.targetSourcedBeans.add(beanName);
			}
			// 获取当前 bean 的拦截器
			Object[] specificInterceptors = getAdvicesAndAdvisorsForBean(beanClass, beanName, targetSource);
			// 创建代理对象,这就是个缓存对象，Spring AOP 自动创建过程,利用了 Spring 的声明周期。将我们的目标 bean 转换成代理 bean
			// 作为代理 bean 来返回
			Object proxy = createProxy(beanClass, beanName, specificInterceptors, targetSource);
			// 返回代理类型，缓存结果
			this.proxyTypes.put(cacheKey, proxy.getClass());
			return proxy;
		}

		return null;
	}

	@Override
	public PropertyValues postProcessProperties(PropertyValues pvs, Object bean, String beanName) {
		return pvs;  // skip postProcessPropertyValues
	}

	/**
	 * Create a proxy with the configured interceptors if the bean is
	 * identified as one to proxy by the subclass.
	 * @see #getAdvicesAndAdvisorsForBean
	 */
	// 如果 bean 被子类标识为要代理的一个，则使用配置的拦截器创建一个代理。
	// bean 初始化之后调用
	@Override
	public Object postProcessAfterInitialization(@Nullable Object bean, String beanName) {
		if (bean != null) {
			Object cacheKey = getCacheKey(bean.getClass(), beanName);
			if (this.earlyProxyReferences.remove(cacheKey) != bean) {
				return wrapIfNecessary(bean, beanName, cacheKey);
			}
		}
		return bean;
	}


	/**
	 * Build a cache key for the given bean class and bean name.
	 * <p>Note: As of 4.2.3, this implementation does not return a concatenated
	 * class/name String anymore but rather the most efficient cache key possible:
	 * a plain bean name, prepended with {@link BeanFactory#FACTORY_BEAN_PREFIX}
	 * in case of a {@code FactoryBean}; or if no bean name specified, then the
	 * given bean {@code Class} as-is.
	 * @param beanClass the bean class
	 * @param beanName the bean name
	 * @return the cache key for the given class and name
	 */
	// 为给定的 bean 类和 bean 名称构建缓存键。
	// 注意：从 4.2.3 开始，此实现不再返回串联的类/名称字符串，而是返回最有效的缓存键：一个普通的 bean 名称，
	// 在 FactoryBean 的情况下以BeanFactory.FACTORY_BEAN_PREFIX开头； 或者，如果没有指定 bean 名称，那么给定的 bean Class是原样的。
	// 参形：
	//			beanClass – bean 类
	//			beanName – bean 名称
	// 返回值：
	//			给定类和名称的缓存键
	protected Object getCacheKey(Class<?> beanClass, @Nullable String beanName) {
		if (StringUtils.hasLength(beanName)) {
			return (FactoryBean.class.isAssignableFrom(beanClass) ?
					BeanFactory.FACTORY_BEAN_PREFIX + beanName : beanName);
		}
		else {
			return beanClass;
		}
	}

	/**
	 * Wrap the given bean if necessary, i.e. if it is eligible for being proxied.
	 * @param bean the raw bean instance
	 * @param beanName the name of the bean
	 * @param cacheKey the cache key for metadata access
	 * @return a proxy wrapping the bean, or the raw bean instance as-is
	 */
	// 如有必要，包装给定的bean，即如果它有资格被代理。
	// 参形：
	//			bean – 原始 bean 实例
	//			beanName – bean 的名称
	//			cacheKey – 元数据访问的缓存键
	// 返回值：
	//			包装 bean 的代理，或原样的原始 bean 实例
	protected Object wrapIfNecessary(Object bean, String beanName, Object cacheKey) {
		if (StringUtils.hasLength(beanName) && this.targetSourcedBeans.contains(beanName)) {
			return bean;
		}
		if (Boolean.FALSE.equals(this.advisedBeans.get(cacheKey))) {
			return bean;
		}
		if (isInfrastructureClass(bean.getClass()) || shouldSkip(bean.getClass(), beanName)) {
			this.advisedBeans.put(cacheKey, Boolean.FALSE);
			return bean;
		}

		// Create proxy if we have advice.
		// 如果我们有建议，请创建代理
		Object[] specificInterceptors = getAdvicesAndAdvisorsForBean(bean.getClass(), beanName, null);
		if (specificInterceptors != DO_NOT_PROXY) {
			this.advisedBeans.put(cacheKey, Boolean.TRUE);
			Object proxy = createProxy(
					bean.getClass(), beanName, specificInterceptors, new SingletonTargetSource(bean));
			this.proxyTypes.put(cacheKey, proxy.getClass());
			return proxy;
		}

		this.advisedBeans.put(cacheKey, Boolean.FALSE);
		return bean;
	}

	/**
	 * Return whether the given bean class represents an infrastructure class
	 * that should never be proxied.
	 * <p>The default implementation considers Advices, Advisors and
	 * AopInfrastructureBeans as infrastructure classes.
	 * @param beanClass the class of the bean
	 * @return whether the bean represents an infrastructure class
	 * @see org.aopalliance.aop.Advice
	 * @see org.springframework.aop.Advisor
	 * @see org.springframework.aop.framework.AopInfrastructureBean
	 * @see #shouldSkip
	 */
	// 返回给定的 bean 类是否代表一个不应该被代理的基础设施类。
	// 默认实现将 Advices、Advisors 和 AopInfrastructureBeans 视为基础设施类。
	// 参形：
	//			beanClass – bean 的类
	// 返回值：
	//			bean 是否代表基础设施类
	// Spring IoC 中排除基本设置类
	protected boolean isInfrastructureClass(Class<?> beanClass) {
		// 以下类型的类将不被自动代理，因为他们属于基础设施类
		boolean retVal = Advice.class.isAssignableFrom(beanClass) ||
				Pointcut.class.isAssignableFrom(beanClass) ||
				Advisor.class.isAssignableFrom(beanClass) ||
				AopInfrastructureBean.class.isAssignableFrom(beanClass);
		if (retVal && logger.isTraceEnabled()) {
			logger.trace("Did not attempt to auto-proxy infrastructure class [" + beanClass.getName() + "]");
		}
		return retVal;
	}

	/**
	 * Subclasses should override this method to return {@code true} if the
	 * given bean should not be considered for auto-proxying by this post-processor.
	 * <p>Sometimes we need to be able to avoid this happening, e.g. if it will lead to
	 * a circular reference or if the existing target instance needs to be preserved.
	 * This implementation returns {@code false} unless the bean name indicates an
	 * "original instance" according to {@code AutowireCapableBeanFactory} conventions.
	 * @param beanClass the class of the bean
	 * @param beanName the name of the bean
	 * @return whether to skip the given bean
	 * @see org.springframework.beans.factory.config.AutowireCapableBeanFactory#ORIGINAL_INSTANCE_SUFFIX
	 */
	// 如果给定的 bean 不应被此后处理器考虑自动代理，则子类应覆盖此方法以返回true 。
	//
	// 有时我们需要能够避免这种情况发生，例如，如果它会导致循环引用或者是否需要保留现有的目标实例。 此实现返回false ，
	// 除非 bean 名称根据AutowireCapableBeanFactory约定指示“原始实例”。
	// 参形：
	//			beanClass – bean 的类
	//			beanName – bean 的名称
	// 返回值：
	//			是否跳过给定的bean
	protected boolean shouldSkip(Class<?> beanClass, String beanName) {
		// 判断 beanClass 和 beanName 是否同源
		return AutoProxyUtils.isOriginalInstance(beanName, beanClass);
	}

	/**
	 * Create a target source for bean instances. Uses any TargetSourceCreators if set.
	 * Returns {@code null} if no custom TargetSource should be used.
	 * <p>This implementation uses the "customTargetSourceCreators" property.
	 * Subclasses can override this method to use a different mechanism.
	 * @param beanClass the class of the bean to create a TargetSource for
	 * @param beanName the name of the bean
	 * @return a TargetSource for this bean
	 * @see #setCustomTargetSourceCreators
	 */
	// 为 bean 实例创建目标源。 如果设置，则使用任何 TargetSourceCreators。
	// 如果不应使用自定义 TargetSource，则返回null 。
	//
	// 此实现使用“customTargetSourceCreators”属性。 子类可以重写此方法以使用不同的机制。
	// 参形：
	//			beanClass – 要为其创建 TargetSource 的 bean 的类
	//			beanName – bean 的名称
	// 返回值：
	//			此 bean 的 TargetSource
	@Nullable
	protected TargetSource getCustomTargetSource(Class<?> beanClass, String beanName) {
		// We can't create fancy target sources for directly registered singletons.
		// 我们不能为直接注册的单例创建花哨的目标源(资源池)。
		if (this.customTargetSourceCreators != null &&
				this.beanFactory != null && this.beanFactory.containsBean(beanName)) {
			for (TargetSourceCreator tsc : this.customTargetSourceCreators) {
				TargetSource ts = tsc.getTargetSource(beanClass, beanName);
				if (ts != null) {
					// Found a matching TargetSource.
					// 找到匹配的 TargetSource
					if (logger.isTraceEnabled()) {
						logger.trace("TargetSourceCreator [" + tsc +
								"] found custom TargetSource for bean with name '" + beanName + "'");
					}
					return ts;
				}
			}
		}

		// No custom TargetSource found.
		// 未找到自定义 TargetSource
		return null;
	}

	/**
	 * Create an AOP proxy for the given bean.
	 * @param beanClass the class of the bean
	 * @param beanName the name of the bean
	 * @param specificInterceptors the set of interceptors that is
	 * specific to this bean (may be empty, but not null)
	 * @param targetSource the TargetSource for the proxy,
	 * already pre-configured to access the bean
	 * @return the AOP proxy for the bean
	 * @see #buildAdvisors
	 */
	// 为给定的 bean 创建一个 AOP 代理。
	// 参形：
	//			beanClass – bean 的类
	//			beanName – bean 的名称
	//			specificInterceptors – 特定于该 bean 的拦截器集（可能为空，但不为空）
	//			targetSource – 代理的 TargetSource，已预先配置为访问 bean
	// 返回值：
	//			bean 的 AOP 代理
	protected Object createProxy(Class<?> beanClass, @Nullable String beanName,
			@Nullable Object[] specificInterceptors, TargetSource targetSource) {

		if (this.beanFactory instanceof ConfigurableListableBeanFactory) {
			AutoProxyUtils.exposeTargetClass((ConfigurableListableBeanFactory) this.beanFactory, beanName, beanClass);
		}

		ProxyFactory proxyFactory = new ProxyFactory();
		// copy ProxyConfig 配置信息
		proxyFactory.copyFrom(this);

		if (proxyFactory.isProxyTargetClass()) {
			// Explicit handling of JDK proxy targets (for introduction advice scenarios)
			// 显式处理 JDK 代理目标（用于介绍建议场景）
			if (Proxy.isProxyClass(beanClass)) {
				// Must allow for introductions; can't just set interfaces to the proxy's interfaces only.
				// 必须允许介绍；不能只将接口设置为代理的接口。
				for (Class<?> ifc : beanClass.getInterfaces()) {
					proxyFactory.addInterface(ifc);
				}
			}
		}
		else {
			// No proxyTargetClass flag enforced, let's apply our default checks...
			// 没有强制执行 proxyTargetClass 标志，让我们应用我们的默认检查...
			if (shouldProxyTargetClass(beanClass, beanName)) {
				proxyFactory.setProxyTargetClass(true);
			}
			else {
				evaluateProxyInterfaces(beanClass, proxyFactory);
			}
		}

		Advisor[] advisors = buildAdvisors(beanName, specificInterceptors);
		proxyFactory.addAdvisors(advisors);
		proxyFactory.setTargetSource(targetSource);
		customizeProxyFactory(proxyFactory);

		proxyFactory.setFrozen(this.freezeProxy);
		if (advisorsPreFiltered()) {
			proxyFactory.setPreFiltered(true);
		}

		// Use original ClassLoader if bean class not locally loaded in overriding class loader
		// 如果 bean 类未在覆盖类加载器中本地加载，则使用原始 ClassLoader
		ClassLoader classLoader = getProxyClassLoader();
		if (classLoader instanceof SmartClassLoader && classLoader != beanClass.getClassLoader()) {
			classLoader = ((SmartClassLoader) classLoader).getOriginalClassLoader();
		}
		return proxyFactory.getProxy(classLoader);
	}

	/**
	 * Determine whether the given bean should be proxied with its target class rather than its interfaces.
	 * <p>Checks the {@link AutoProxyUtils#PRESERVE_TARGET_CLASS_ATTRIBUTE "preserveTargetClass" attribute}
	 * of the corresponding bean definition.
	 * @param beanClass the class of the bean
	 * @param beanName the name of the bean
	 * @return whether the given bean should be proxied with its target class
	 * @see AutoProxyUtils#shouldProxyTargetClass
	 */
	// 确定给定的 bean 是否应该使用它的目标类而不是它的接口来代理。
	// 检查相应 bean 定义的"preserveTargetClass" attribute 。
	// 参形：
	//			beanClass – bean 的类
	//			beanName – bean 的名称
	// 返回值：
	//			给定的 bean 是否应该用它的目标类代理
	protected boolean shouldProxyTargetClass(Class<?> beanClass, @Nullable String beanName) {
		return (this.beanFactory instanceof ConfigurableListableBeanFactory &&
				AutoProxyUtils.shouldProxyTargetClass((ConfigurableListableBeanFactory) this.beanFactory, beanName));
	}

	/**
	 * Return whether the Advisors returned by the subclass are pre-filtered
	 * to match the bean's target class already, allowing the ClassFilter check
	 * to be skipped when building advisors chains for AOP invocations.
	 * <p>Default is {@code false}. Subclasses may override this if they
	 * will always return pre-filtered Advisors.
	 * @return whether the Advisors are pre-filtered
	 * @see #getAdvicesAndAdvisorsForBean
	 * @see org.springframework.aop.framework.Advised#setPreFiltered
	 */
	// 返回子类返回的顾问是否已经预先过滤以匹配 bean 的目标类，从而允许在为 AOP 调用构建顾问链时跳过 ClassFilter 检查。
	// 默认为false 。 如果子类总是返回预过滤的顾问，则子类可能会覆盖它。
	// 返回值：
	//				顾问是否被预先过滤
	protected boolean advisorsPreFiltered() {
		return false;
	}

	/**
	 * Determine the advisors for the given bean, including the specific interceptors
	 * as well as the common interceptor, all adapted to the Advisor interface.
	 * @param beanName the name of the bean
	 * @param specificInterceptors the set of interceptors that is
	 * specific to this bean (may be empty, but not null)
	 * @return the list of Advisors for the given bean
	 */
	// 确定给定 bean 的顾问，包括特定的拦截器以及公共拦截器，所有这些都适应顾问接口。
	// 参形：
	//			beanName – bean 的名称
	//			specificInterceptors – 特定于该 bean 的拦截器集（可能为空，但不为空）
	// 返回值：
	//			给定 bean 的顾问列表
	protected Advisor[] buildAdvisors(@Nullable String beanName, @Nullable Object[] specificInterceptors) {
		// Handle prototypes correctly...
		// 正确处理原型...
		Advisor[] commonInterceptors = resolveInterceptorNames();

		List<Object> allInterceptors = new ArrayList<>();
		if (specificInterceptors != null) {
			if (specificInterceptors.length > 0) {
				// specificInterceptors may equal PROXY_WITHOUT_ADDITIONAL_INTERCEPTORS
				// specificInterceptors 可能等于 PROXY_WITHOUT_ADDITIONAL_INTERCEPTORS
				allInterceptors.addAll(Arrays.asList(specificInterceptors));
			}
			if (commonInterceptors.length > 0) {
				if (this.applyCommonInterceptorsFirst) {
					allInterceptors.addAll(0, Arrays.asList(commonInterceptors));
				}
				else {
					allInterceptors.addAll(Arrays.asList(commonInterceptors));
				}
			}
		}
		if (logger.isTraceEnabled()) {
			int nrOfCommonInterceptors = commonInterceptors.length;
			int nrOfSpecificInterceptors = (specificInterceptors != null ? specificInterceptors.length : 0);
			logger.trace("Creating implicit proxy for bean '" + beanName + "' with " + nrOfCommonInterceptors +
					" common interceptors and " + nrOfSpecificInterceptors + " specific interceptors");
		}

		Advisor[] advisors = new Advisor[allInterceptors.size()];
		for (int i = 0; i < allInterceptors.size(); i++) {
			advisors[i] = this.advisorAdapterRegistry.wrap(allInterceptors.get(i));
		}
		return advisors;
	}

	/**
	 * Resolves the specified interceptor names to Advisor objects.
	 * @see #setInterceptorNames
	 */
	// 将指定的拦截器名称解析为 Advisor 对象。
	private Advisor[] resolveInterceptorNames() {
		BeanFactory bf = this.beanFactory;
		ConfigurableBeanFactory cbf = (bf instanceof ConfigurableBeanFactory ? (ConfigurableBeanFactory) bf : null);
		List<Advisor> advisors = new ArrayList<>();
		for (String beanName : this.interceptorNames) {
			if (cbf == null || !cbf.isCurrentlyInCreation(beanName)) {
				Assert.state(bf != null, "BeanFactory required for resolving interceptor names");
				Object next = bf.getBean(beanName);
				advisors.add(this.advisorAdapterRegistry.wrap(next));
			}
		}
		return advisors.toArray(new Advisor[0]);
	}

	/**
	 * Subclasses may choose to implement this: for example,
	 * to change the interfaces exposed.
	 * <p>The default implementation is empty.
	 * @param proxyFactory a ProxyFactory that is already configured with
	 * TargetSource and interfaces and will be used to create the proxy
	 * immediately after this method returns
	 */
	// 子类可以选择实现这一点：例如，更改公开的接口。
	// 默认实现为空。
	// 参形：proxyFactory – 一个已经配置了 TargetSource 和接口的 ProxyFactory，将在此方法返回后立即用于创建代理
	protected void customizeProxyFactory(ProxyFactory proxyFactory) {
	}


	/**
	 * Return whether the given bean is to be proxied, what additional
	 * advices (e.g. AOP Alliance interceptors) and advisors to apply.
	 * @param beanClass the class of the bean to advise
	 * @param beanName the name of the bean
	 * @param customTargetSource the TargetSource returned by the
	 * {@link #getCustomTargetSource} method: may be ignored.
	 * Will be {@code null} if no custom target source is in use.
	 * @return an array of additional interceptors for the particular bean;
	 * or an empty array if no additional interceptors but just the common ones;
	 * or {@code null} if no proxy at all, not even with the common interceptors.
	 * See constants DO_NOT_PROXY and PROXY_WITHOUT_ADDITIONAL_INTERCEPTORS.
	 * @throws BeansException in case of errors
	 * @see #DO_NOT_PROXY
	 * @see #PROXY_WITHOUT_ADDITIONAL_INTERCEPTORS
	 */
	// 返回是否要代理给定的 bean、要应用的附加建议（例如 AOP 联盟拦截器）和顾问。
	// 参形：
	//			beanClass – 要建议的 bean 的类
	//			beanName – bean 的名称
	//			customTargetSource – getCustomTargetSource方法返回的 TargetSource：可以忽略。
	//			如果没有使用自定义目标源，则为null 。
	// 返回值：
	//			特定 bean 的一组附加拦截器； 如果没有额外的拦截器而只是普通的拦截器，则为空数组； 如果根本没有代理，
	//			则为null ，即使使用常见的拦截器也不行。 请参阅常量 DO_NOT_PROXY 和 PROXY_WITHOUT_ADDITIONAL_INTERCEPTORS。
	// 抛出：
	//			BeansException – 发生错误时
	// 抽象方法：模板方法
	@Nullable
	protected abstract Object[] getAdvicesAndAdvisorsForBean(Class<?> beanClass, String beanName,
			@Nullable TargetSource customTargetSource) throws BeansException;

}
