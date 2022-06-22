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

package org.springframework.aop.framework;

import org.springframework.aop.Advisor;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.SmartClassLoader;
import org.springframework.lang.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Base class for {@link BeanPostProcessor} implementations that apply a
 * Spring AOP {@link Advisor} to specific beans.
 *
 * @author Juergen Hoeller
 * @since 3.2
 */
// 将 Spring AOP Advisor 应用于特定 bean 的 BeanPostProcessor 实现的基类。
@SuppressWarnings("serial")
public abstract class AbstractAdvisingBeanPostProcessor extends ProxyProcessorSupport implements BeanPostProcessor {

	// Spring Advisor 链路
	@Nullable
	protected Advisor advisor;

	protected boolean beforeExistingAdvisors = false;

	private final Map<Class<?>, Boolean> eligibleBeans = new ConcurrentHashMap<>(256);


	/**
	 * Set whether this post-processor's advisor is supposed to apply before
	 * existing advisors when encountering a pre-advised object.
	 * <p>Default is "false", applying the advisor after existing advisors, i.e.
	 * as close as possible to the target method. Switch this to "true" in order
	 * for this post-processor's advisor to wrap existing advisors as well.
	 * <p>Note: Check the concrete post-processor's javadoc whether it possibly
	 * changes this flag by default, depending on the nature of its advisor.
	 */
	// 设置当遇到预先建议的对象时，此后处理器的顾问是否应该在现有顾问之前应用。
	//
	// 默认为 “false”，在现有顾问之后应用顾问，即尽可能接近目标方法。将此切换为 “true”，以便此后处理器的顾问也包装现有顾问。
	//
	// 注意：检查具体后处理器的 javadoc 是否可能默认更改此标志，具体取决于其顾问的性质。
	public void setBeforeExistingAdvisors(boolean beforeExistingAdvisors) {
		this.beforeExistingAdvisors = beforeExistingAdvisors;
	}


	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) {
		return bean;
	}

	// @EnableAsync Spring AOP 在 Spring 本地调度（Scheduling）中并没有使用 AbstractAutoProxyCreator
	// EnableAsync 的实现和 Spring Transaction,Spring Cashing 是不一样的，尽管它创建了 Proxy 的 ConfigurationClass,
	// 但是它没有直接创建（注入） 一个 AbstractAutoProxyCreator bean(即没有利用 Spring BeanDefinition 去注册一个
	// AbstractAutoProxyCreator),而是利用生命周期(BeanPostProcessor#postProcessAfterInitialization)，
	// 通过 BeanFactory 的 API 来创建一个代理对象，同时它利用到 PointcutAdvisor 的特点，注入 Advisor
	// 此种实现更加优雅(毕竟 @Async 是在 Spring 后期提供)，不会和 Spring 内部产生冲突。
	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) {
		if (this.advisor == null || bean instanceof AopInfrastructureBean) {
			// Ignore AOP infrastructure such as scoped proxies.
			// 忽略 AOP 基础架构，例如作用域代理。
			return bean;
		}

		if (bean instanceof Advised) {
			Advised advised = (Advised) bean;
			if (!advised.isFrozen() && isEligible(AopUtils.getTargetClass(bean))) {
				// Add our local Advisor to the existing proxy's Advisor chain...
				// 将我们的本地顾问添加到现有代理的顾问链中......
				if (this.beforeExistingAdvisors) {
					advised.addAdvisor(0, this.advisor);
				}
				else {
					advised.addAdvisor(this.advisor);
				}
				return bean;
			}
		}

		// 如果是目标 bean
		if (isEligible(bean, beanName)) {
			// 利用生命周期，通过 BeanFactory 的 API 创建一个代理对象
			ProxyFactory proxyFactory = prepareProxyFactory(bean, beanName);
			// 排除一些接口
			if (!proxyFactory.isProxyTargetClass()) {
				evaluateProxyInterfaces(bean.getClass(), proxyFactory);
			}
			// 在这里关联了  @EnableAsync 注解的 advisor
			proxyFactory.addAdvisor(this.advisor);
			customizeProxyFactory(proxyFactory);

			// Use original ClassLoader if bean class not locally loaded in overriding class loader
			// 如果 bean 类未在覆盖类加载器中本地加载，则使用原始 ClassLoader
			ClassLoader classLoader = getProxyClassLoader();
			if (classLoader instanceof SmartClassLoader && classLoader != bean.getClass().getClassLoader()) {
				classLoader = ((SmartClassLoader) classLoader).getOriginalClassLoader();
			}
			return proxyFactory.getProxy(classLoader);
		}

		// No proxy needed.
		// 不需要代理
		return bean;
	}

	/**
	 * Check whether the given bean is eligible for advising with this
	 * post-processor's {@link Advisor}.
	 * <p>Delegates to {@link #isEligible(Class)} for target class checking.
	 * Can be overridden e.g. to specifically exclude certain beans by name.
	 * <p>Note: Only called for regular bean instances but not for existing
	 * proxy instances which implement {@link Advised} and allow for adding
	 * the local {@link Advisor} to the existing proxy's {@link Advisor} chain.
	 * For the latter, {@link #isEligible(Class)} is being called directly,
	 * with the actual target class behind the existing proxy (as determined
	 * by {@link AopUtils#getTargetClass(Object)}).
	 * @param bean the bean instance
	 * @param beanName the name of the bean
	 * @see #isEligible(Class)
	 */
	// 检查给定的 bean 是否有资格使用此后处理器的 Advisor 提供建议。
	//
	// 委托 isEligible(Class) 进行目标类检查。可以被覆盖，例如通过名称专门排除某些bean。
	//
	// 注意：仅对常规 bean 实例调用，而不对实现 Advised 并允许将本地 Advisor 添加到现有代理的 Advisor 链的现有代理实例调用。
	// 对于后者，直接调用 isEligible(Class) ，实际目标类位于现有代理后面（由AopUtils.getTargetClass(Object)确定）。
	protected boolean isEligible(Object bean, String beanName) {
		return isEligible(bean.getClass());
	}

	/**
	 * Check whether the given class is eligible for advising with this
	 * post-processor's {@link Advisor}.
	 * <p>Implements caching of {@code canApply} results per bean target class.
	 * @param targetClass the class to check against
	 * @see AopUtils#canApply(Advisor, Class)
	 */
	// 检查给定的类是否有资格使用此后处理器的Advisor提供建议。
	// 实现每个 bean 目标类的canApply结果缓存。
	// 参形：
	//			targetClass – 要检查的类
	protected boolean isEligible(Class<?> targetClass) {
		Boolean eligible = this.eligibleBeans.get(targetClass);
		if (eligible != null) {
			return eligible;
		}
		if (this.advisor == null) {
			return false;
		}
		eligible = AopUtils.canApply(this.advisor, targetClass);
		this.eligibleBeans.put(targetClass, eligible);
		return eligible;
	}

	/**
	 * Prepare a {@link ProxyFactory} for the given bean.
	 * <p>Subclasses may customize the handling of the target instance and in
	 * particular the exposure of the target class. The default introspection
	 * of interfaces for non-target-class proxies and the configured advisor
	 * will be applied afterwards; {@link #customizeProxyFactory} allows for
	 * late customizations of those parts right before proxy creation.
	 * @param bean the bean instance to create a proxy for
	 * @param beanName the corresponding bean name
	 * @return the ProxyFactory, initialized with this processor's
	 * {@link ProxyConfig} settings and the specified bean
	 * @since 4.2.3
	 * @see #customizeProxyFactory
	 */
	// 为给定的 bean 准备一个 ProxyFactory 。
	//
	// 子类可以自定义目标实例的处理，特别是目标类的公开。非目标类代理和配置顾问的默认接口自省将在之后应用；
	// customizeProxyFactory 允许在代理创建之前对这些部分进行后期定制。
	// 参形：
	//			bean -- 要为其创建代理的 bean 实例
	//			beanName – 对应的 bean 名称
	// 返回值：
	//			ProxyFactory，使用此处理器的 ProxyConfig 设置和指定的 bean 进行初始化
	// 产生一个代理对象
	protected ProxyFactory prepareProxyFactory(Object bean, String beanName) {
		ProxyFactory proxyFactory = new ProxyFactory();
		// 辅助配置
		proxyFactory.copyFrom(this);
		// 设置代理的目标 bean
		proxyFactory.setTarget(bean);
		return proxyFactory;
	}

	/**
	 * Subclasses may choose to implement this: for example,
	 * to change the interfaces exposed.
	 * <p>The default implementation is empty.
	 * @param proxyFactory the ProxyFactory that is already configured with
	 * target, advisor and interfaces and will be used to create the proxy
	 * immediately after this method returns
	 * @since 4.2.3
	 * @see #prepareProxyFactory
	 */
	// 子类可以选择实现这一点：例如，更改公开的接口。
	// 默认实现为空。
	// 参形：
	//				proxyFactory – 已配置目标、顾问和接口的 ProxyFactory，
	//				将在此方法返回后立即用于创建代理
	protected void customizeProxyFactory(ProxyFactory proxyFactory) {
	}

}
