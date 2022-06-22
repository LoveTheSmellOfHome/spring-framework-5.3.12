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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.aspectj.lang.reflect.PerClauseKind;

import org.springframework.aop.Advisor;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Helper for retrieving @AspectJ beans from a BeanFactory and building
 * Spring Advisors based on them, for use with auto-proxying.
 *
 * @author Juergen Hoeller
 * @since 2.0.2
 * @see AnnotationAwareAspectJAutoProxyCreator
 */
// 从 BeanFactory 检索 @AspectJ bean 并基于它们构建 Spring 顾问的助手，用于自动代理
//
// 构建器模式（Builder）实现：建造模式，是一种对象构建模式。它可以复杂对象的建造过程抽象出來（抽象类别），
// 使这个抽象过程的不同实现方法可以构造出不同表现（属性）的对象。此外比如常用的 Fluent 流式框架以及 StringBulilder 等
// 它们的实现都是通过改变内部状态来实现的
public class BeanFactoryAspectJAdvisorsBuilder {

	private final ListableBeanFactory beanFactory;

	// 可以获取 List<Advisor>
	private final AspectJAdvisorFactory advisorFactory;

	// 保证可见性
	@Nullable
	private volatile List<String> aspectBeanNames;

	private final Map<String, List<Advisor>> advisorsCache = new ConcurrentHashMap<>();

	private final Map<String, MetadataAwareAspectInstanceFactory> aspectFactoryCache = new ConcurrentHashMap<>();


	/**
	 * Create a new BeanFactoryAspectJAdvisorsBuilder for the given BeanFactory.
	 * @param beanFactory the ListableBeanFactory to scan
	 */
	// 为给定的 BeanFactory 创建一个新的 BeanFactoryAspectJAdvisorsBuilder。
	// 参形：
	//			beanFactory – 要扫描的 ListableBeanFactory
	public BeanFactoryAspectJAdvisorsBuilder(ListableBeanFactory beanFactory) {
		this(beanFactory, new ReflectiveAspectJAdvisorFactory(beanFactory));
	}

	/**
	 * Create a new BeanFactoryAspectJAdvisorsBuilder for the given BeanFactory.
	 * @param beanFactory the ListableBeanFactory to scan
	 * @param advisorFactory the AspectJAdvisorFactory to build each Advisor with
	 */
	// 为给定的 BeanFactory 创建一个新的 BeanFactoryAspectJAdvisorsBuilder。
	// 参形：
	//			beanFactory – 要扫描的 ListableBeanFactory
	//			advisorFactory – 用于构建每个 Advisor 的 AspectJAdvisorFactory
	public BeanFactoryAspectJAdvisorsBuilder(ListableBeanFactory beanFactory, AspectJAdvisorFactory advisorFactory) {
		Assert.notNull(beanFactory, "ListableBeanFactory must not be null");
		Assert.notNull(advisorFactory, "AspectJAdvisorFactory must not be null");
		this.beanFactory = beanFactory;
		this.advisorFactory = advisorFactory;
	}


	/**
	 * Look for AspectJ-annotated aspect beans in the current bean factory,
	 * and return to a list of Spring AOP Advisors representing them.
	 * <p>Creates a Spring Advisor for each AspectJ advice method.
	 * @return the list of {@link org.springframework.aop.Advisor} beans
	 * @see #isEligibleBean
	 */
	// 在当前 bean 工厂中查找 AspectJ 注释的方面 bean，并返回代表它们的 Spring AOP Advisor 列表。
	// 为每个 AspectJ 建议方法创建一个 Spring Advisor。
	// 返回值：
	//			Advisor bean 列表
	// 创建器模式：核心语义创建对象，以 build 作为方法前缀，通常来说一个build 只生成一种对象可以是一组，但是
	// 不排除可以生成不同的对象。build 的核心语义是生成对象。
	public List<Advisor> buildAspectJAdvisors() {
		// 首先通过临时变量接收快照
		List<String> aspectNames = this.aspectBeanNames;

		if (aspectNames == null) {
			// aspectNames == null 还没由线程处理，此时加对象锁保证线程安全
			synchronized (this) {
				aspectNames = this.aspectBeanNames;
				// 再次判断保证加锁前后数据一致性，确保后续处理是线程安全的
				if (aspectNames == null) {
					List<Advisor> advisors = new ArrayList<>();
					aspectNames = new ArrayList<>();
					String[] beanNames = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(
							this.beanFactory, Object.class, true, false);
					for (String beanName : beanNames) {
						if (!isEligibleBean(beanName)) {
							continue;
						}
						// We must be careful not to instantiate beans eagerly as in this case they
						// would be cached by the Spring container but would not have been weaved.
						// 我们必须注意不要急切地实例化 bean，因为在这种情况下它们会被 Spring 容器缓存但不会被编织。
						Class<?> beanType = this.beanFactory.getType(beanName, false);
						if (beanType == null) {
							continue;
						}
						// 过滤出 Aspect 切面
						if (this.advisorFactory.isAspect(beanType)) {
							// 在构建过程中做了一次交换。
							aspectNames.add(beanName);
							AspectMetadata amd = new AspectMetadata(beanType, beanName);
							// 如果是单例
							if (amd.getAjType().getPerClause().getKind() == PerClauseKind.SINGLETON) {
								MetadataAwareAspectInstanceFactory factory =
										new BeanFactoryAspectInstanceFactory(this.beanFactory, beanName);
								List<Advisor> classAdvisors = this.advisorFactory.getAdvisors(factory);
								if (this.beanFactory.isSingleton(beanName)) {
									// 如果是单例就通过 advisorsCache 缓存单例名称和 classAdvisors
									this.advisorsCache.put(beanName, classAdvisors);
								}
								else {
									// 非单例通过 aspectFactoryCache 缓存单例名称和对应的工厂
									this.aspectFactoryCache.put(beanName, factory);
								}
								advisors.addAll(classAdvisors);
							}
							else {
								// Per target or per this.
								// 每个目标对象对应一个 MetadataAwareAspectInstanceFactory 工厂实例
								if (this.beanFactory.isSingleton(beanName)) {
									throw new IllegalArgumentException("Bean with name '" + beanName +
											"' is a singleton, but aspect instantiation model is not singleton");
								}
								MetadataAwareAspectInstanceFactory factory =
										new PrototypeAspectInstanceFactory(this.beanFactory, beanName);
								this.aspectFactoryCache.put(beanName, factory);
								advisors.addAll(this.advisorFactory.getAdvisors(factory));
							}
						}
					}
					// 写方法
					this.aspectBeanNames = aspectNames;
					return advisors;
				}
			}
		}

		if (aspectNames.isEmpty()) {
			return Collections.emptyList();
		}
		List<Advisor> advisors = new ArrayList<>();
		for (String aspectName : aspectNames) {
			List<Advisor> cachedAdvisors = this.advisorsCache.get(aspectName);
			if (cachedAdvisors != null) {
				advisors.addAll(cachedAdvisors);
			}
			else {
				MetadataAwareAspectInstanceFactory factory = this.aspectFactoryCache.get(aspectName);
				advisors.addAll(this.advisorFactory.getAdvisors(factory));
			}
		}
		return advisors;
	}

	/**
	 * Return whether the aspect bean with the given name is eligible.
	 * @param beanName the name of the aspect bean
	 * @return whether the bean is eligible
	 */
	// 返回具有给定名称的切面 bean 是否符合条件。
	// 参形：
	//			beanName – 方面 bean 的名称
	// 返回值：
	//			bean是否符合条件
	protected boolean isEligibleBean(String beanName) {
		return true;
	}

}
