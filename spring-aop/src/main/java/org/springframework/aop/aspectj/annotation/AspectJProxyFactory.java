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

package org.springframework.aop.aspectj.annotation;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.aspectj.lang.reflect.PerClauseKind;

import org.springframework.aop.Advisor;
import org.springframework.aop.aspectj.AspectJProxyUtils;
import org.springframework.aop.aspectj.SimpleAspectInstanceFactory;
import org.springframework.aop.framework.ProxyCreatorSupport;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * AspectJ-based proxy factory, allowing for programmatic building
 * of proxies which include AspectJ aspects (code style as well
 * Java 5 annotation style).
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Ramnivas Laddad
 * @since 2.0
 * @see #addAspect(Object)
 * @see #addAspect(Class)
 * @see #getProxy()
 * @see #getProxy(ClassLoader)
 * @see org.springframework.aop.framework.ProxyFactory
 */
// 基于 AspectJ 的代理工厂，允许以编程切式构建代理，其中包括 AspectJ 切面（代码样式以及 Java 5 注解样式）。
// Spring 和 AspectJ 关联
//
// 和 AspectJ 打通的
@SuppressWarnings("serial")
public class AspectJProxyFactory extends ProxyCreatorSupport {

	/** Cache for singleton aspect instances. */
	// 缓存单例切面实例。
	//
	// 在一个 JVM 中，它的 ClassLoader 之间都是有层次性关系(继承关系)的，
	// ClassLoader 它使用双亲委派方式层次性向上查找，一旦加载过就不再加载，直接 return.
	// 对于大多数使用 ClassLoader 我们可以合并成一个，把它当前的和它的父类以及 BootStrap
	// 可以认为是一个就是当前线程上下文里的一个，所以我们去缓存的时候可以以静态的方式，即 JVM
	// 级别的缓存，这里需要区分下 ClassLoader 级别的缓存和 JVM 级别的缓存是不一样的。尽管静态
	// 字段也属于 ClassLoader.
	//
	// 所以这里的缓存加载的类几乎都是唯一的
	private static final Map<Class<?>, Object> aspectCache = new ConcurrentHashMap<>();

	private final AspectJAdvisorFactory aspectFactory = new ReflectiveAspectJAdvisorFactory();


	/**
	 * Create a new AspectJProxyFactory.
	 */
	// 创建一个新的 AspectJProxyFactory
	public AspectJProxyFactory() {
	}

	/**
	 * Create a new AspectJProxyFactory.
	 * <p>Will proxy all interfaces that the given target implements.
	 * @param target the target object to be proxied
	 */
	// 创建一个新的 AspectJProxyFactory。
	// 将代理给定目标实现的所有接口。
	// 形参：
	//			target -- 要代理的目标对象
	public AspectJProxyFactory(Object target) {
		Assert.notNull(target, "Target object must not be null");
		setInterfaces(ClassUtils.getAllInterfaces(target));
		setTarget(target);
	}

	/**
	 * Create a new {@code AspectJProxyFactory}.
	 * No target, only interfaces. Must add interceptors.
	 */
	// 创建一个新的 AspectJProxyFactory 。 没有目标，只有接口。 必须添加拦截器
	public AspectJProxyFactory(Class<?>... interfaces) {
		setInterfaces(interfaces);
	}


	/**
	 * Add the supplied aspect instance to the chain. The type of the aspect instance
	 * supplied must be a singleton aspect. True singleton lifecycle is not honoured when
	 * using this method - the caller is responsible for managing the lifecycle of any
	 * aspects added in this way.
	 * @param aspectInstance the AspectJ aspect instance
	 */
	// 将提供的切面实例添加到链中。 提供的切面实例的类型必须是单例切面。 使用此切法时不支持真正的
	// 单例生命周期 - 调用者负责管理以这种切式添加的任何切面的生命周期。
	// 形参：
	//			aspectInstance – AspectJ 切面实例
	public void addAspect(Object aspectInstance) {
		Class<?> aspectClass = aspectInstance.getClass();
		String aspectName = aspectClass.getName();
		// 获取 AspectMetadata 元数据，和 Java 反射密切相关的元数据
		AspectMetadata am = createAspectMetadata(aspectClass, aspectName);
		if (am.getAjType().getPerClause().getKind() != PerClauseKind.SINGLETON) {
			throw new IllegalArgumentException(
					"Aspect class [" + aspectClass.getName() + "] does not define a singleton aspect");
		}
		addAdvisorsFromAspectInstanceFactory(
				new SingletonMetadataAwareAspectInstanceFactory(aspectInstance, aspectName));
	}

	/**
	 * Add an aspect of the supplied type to the end of the advice chain.
	 * @param aspectClass the AspectJ aspect class
	 */
	// 将所提供类型的一个切面添加到建议链的末尾。
	// 形参：
	//			aspectClass – AspectJ 切面类
	// 与父类相比它可以添加切面
	public void addAspect(Class<?> aspectClass) {
		// 切面名称就是类名，是唯一的
		String aspectName = aspectClass.getName();
		// 获取 AspectMetadata 元数据，和 Java 反射密切相关的元数据
		AspectMetadata am = createAspectMetadata(aspectClass, aspectName);
		MetadataAwareAspectInstanceFactory instanceFactory = createAspectInstanceFactory(am, aspectClass, aspectName);
		addAdvisorsFromAspectInstanceFactory(instanceFactory);
	}


	/**
	 * Add all {@link Advisor Advisors} from the supplied {@link MetadataAwareAspectInstanceFactory}
	 * to the current chain. Exposes any special purpose {@link Advisor Advisors} if needed.
	 * @see AspectJProxyUtils#makeAdvisorChainAspectJCapableIfNecessary(List)
	 */
	// 将提供的MetadataAwareAspectInstanceFactory中的所有Advisors添加到当前链。 
	// 如果需要，公开任何特殊用途的Advisors 。
	private void addAdvisorsFromAspectInstanceFactory(MetadataAwareAspectInstanceFactory instanceFactory) {
		List<Advisor> advisors = this.aspectFactory.getAdvisors(instanceFactory);
		Class<?> targetClass = getTargetClass();
		Assert.state(targetClass != null, "Unresolvable target class");
		advisors = AopUtils.findAdvisorsThatCanApply(advisors, targetClass);
		AspectJProxyUtils.makeAdvisorChainAspectJCapableIfNecessary(advisors);
		AnnotationAwareOrderComparator.sort(advisors);
		addAdvisors(advisors);
	}

	/**
	 * Create an {@link AspectMetadata} instance for the supplied aspect type.
	 */
	// 为提供的切面类型创建一个AspectMetadata实例。
	// 这里是基于 API 的实现，并不和 IoC 打通
	private AspectMetadata createAspectMetadata(Class<?> aspectClass, String aspectName) {
		AspectMetadata am = new AspectMetadata(aspectClass, aspectName);
		if (!am.getAjType().isAspect()) {
			throw new IllegalArgumentException("Class [" + aspectClass.getName() + "] is not a valid aspect type");
		}
		return am;
	}

	/**
	 * Create a {@link MetadataAwareAspectInstanceFactory} for the supplied aspect type. If the aspect type
	 * has no per clause, then a {@link SingletonMetadataAwareAspectInstanceFactory} is returned, otherwise
	 * a {@link PrototypeAspectInstanceFactory} is returned.
	 */
	// 为提供的切面类型创建 MetadataAwareAspectInstanceFactory 。 如果切面类型没有 per 子句，
	// 则返回 SingletonMetadataAwareAspectInstanceFactory ，否则返回 PrototypeAspectInstanceFactory
	private MetadataAwareAspectInstanceFactory createAspectInstanceFactory(
			AspectMetadata am, Class<?> aspectClass, String aspectName) {

		MetadataAwareAspectInstanceFactory instanceFactory;
		if (am.getAjType().getPerClause().getKind() == PerClauseKind.SINGLETON) {
			// Create a shared aspect instance.
			// 创建一个共享切面实例,基于对象创建
			Object instance = getSingletonAspectInstance(aspectClass);
			instanceFactory = new SingletonMetadataAwareAspectInstanceFactory(instance, aspectName);
		}
		else {
			// Create a factory for independent aspect instances.
			// 为独立切面实例创建工厂，基于类创建
			instanceFactory = new SimpleMetadataAwareAspectInstanceFactory(aspectClass, aspectName);
		}
		return instanceFactory;
	}

	/**
	 * Get the singleton aspect instance for the supplied aspect type.
	 * An instance is created if one cannot be found in the instance cache.
	 */
	// 获取提供的切面类型的单例切面实例。 如果在实例缓存中找不到实例，则会创建一个实例。
	private Object getSingletonAspectInstance(Class<?> aspectClass) {
		return aspectCache.computeIfAbsent(aspectClass,
				clazz -> new SimpleAspectInstanceFactory(clazz).getAspectInstance());
	}


	/**
	 * Create a new proxy according to the settings in this factory.
	 * <p>Can be called repeatedly. Effect will vary if we've added
	 * or removed interfaces. Can add and remove interceptors.
	 * <p>Uses a default class loader: Usually, the thread context class loader
	 * (if necessary for proxy creation).
	 * @return the new proxy
	 */
	// 根据此工厂中的设置创建新代理。
	// 可以反复调用。 如果我们添加或删除接口，效果会有所不同。 可以添加和删除拦截器。
	// 使用默认类加载器：通常是线程上下文类加载器（如果需要创建代理）。
	// 返回值：
	//			新代理
	@SuppressWarnings("unchecked")
	public <T> T getProxy() {
		// 调用标准实现
		return (T) createAopProxy().getProxy();
	}

	/**
	 * Create a new proxy according to the settings in this factory.
	 * <p>Can be called repeatedly. Effect will vary if we've added
	 * or removed interfaces. Can add and remove interceptors.
	 * <p>Uses the given class loader (if necessary for proxy creation).
	 * @param classLoader the class loader to create the proxy with
	 * @return the new proxy
	 */
	// 根据此工厂中的设置创建新代理。
	// 可以反复调用。 如果我们添加或删除接口，效果会有所不同。 可以添加和删除拦截器。
	// 使用给定的类加载器（如果需要创建代理）。
	// 形参：
	//			classLoader – 用于创建代理的类加载器
	// 返回值：
	//			新代理
	@SuppressWarnings("unchecked")
	public <T> T getProxy(ClassLoader classLoader) {
		// 调用标准实现
		return (T) createAopProxy().getProxy(classLoader);
	}

}
