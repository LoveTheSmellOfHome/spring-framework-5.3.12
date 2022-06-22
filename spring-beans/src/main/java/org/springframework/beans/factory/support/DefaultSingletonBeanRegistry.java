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

package org.springframework.beans.factory.support;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanCreationNotAllowedException;
import org.springframework.beans.factory.BeanCurrentlyInCreationException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.SingletonBeanRegistry;
import org.springframework.core.SimpleAliasRegistry;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Generic registry for shared bean instances, implementing the
 * {@link org.springframework.beans.factory.config.SingletonBeanRegistry}.
 * Allows for registering singleton instances that should be shared
 * for all callers of the registry, to be obtained via bean name.
 *
 * <p>Also supports registration of
 * {@link org.springframework.beans.factory.DisposableBean} instances,
 * (which might or might not correspond to registered singletons),
 * to be destroyed on shutdown of the registry. Dependencies between
 * beans can be registered to enforce an appropriate shutdown order.
 *
 * <p>This class mainly serves as base class for
 * {@link org.springframework.beans.factory.BeanFactory} implementations,
 * factoring out the common management of singleton bean instances. Note that
 * the {@link org.springframework.beans.factory.config.ConfigurableBeanFactory}
 * interface extends the {@link SingletonBeanRegistry} interface.
 *
 * <p>Note that this class assumes neither a bean definition concept
 * nor a specific creation process for bean instances, in contrast to
 * {@link AbstractBeanFactory} and {@link DefaultListableBeanFactory}
 * (which inherit from it). Can alternatively also be used as a nested
 * helper to delegate to.
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see #registerSingleton
 * @see #registerDisposableBean
 * @see org.springframework.beans.factory.DisposableBean
 * @see org.springframework.beans.factory.config.ConfigurableBeanFactory
 */
// 共享 bean 实例的通用注册表，实现 {@link org.springframework.beans.factory.config.SingletonBeanRegistry}。
// 允许注册应为注册表的所有调用者共享的单例实例，通过 bean 名称获取
//
// <p>还支持注册 {@link org.springframework.beans.factory.DisposableBean} 实例（可能对应也可能不对应注册的单例），
// 在注册表关闭时销毁。可以注册 bean 之间的依赖关系以强制执行适当的关闭顺序。
//
// <p>该类主要作为{@link org.springframework.beans.factory.BeanFactory}实现的基类，分解出单例bean实例的通用管理。
// 请注意，{@link org.springframework.beans.factory.config.ConfigurableBeanFactory} 接口扩展了
// {@link SingletonBeanRegistry} 接口。
//
// <p>请注意，与 {@link AbstractBeanFactory} 和 {@link DefaultListableBeanFactory}（继承自它）相比，
// 该类既不假定 bean 定义概念，也不假定 bean 实例的特定创建过程。也可以用作嵌套的助手来委托。
public class DefaultSingletonBeanRegistry extends SimpleAliasRegistry implements SingletonBeanRegistry {

	/** Maximum number of suppressed exceptions to preserve. */
	// 要保留的最大抑制异常数
	private static final int SUPPRESSED_EXCEPTIONS_LIMIT = 100;


	/** Cache of singleton objects: bean name to bean instance. */
	// 单例对象的缓存：bean 名称到 bean 实例
	//
	// 循环引用中：
	// 一级缓存，存的是实例化、初始化、属性都注入完成的单例 bean 对象\
	private final Map<String, Object> singletonObjects = new ConcurrentHashMap<>(256);

	/** Cache of singleton factories: bean name to ObjectFactory. */
	// 单例工厂缓存：bean 名称到 ObjectFactory
	//
	// 循环引用：
	// 三级缓存：存的是 ObjectFactory 工厂,调用工厂 getObject 方法获取 bean ,进行进一步扩展\
	private final Map<String, ObjectFactory<?>> singletonFactories = new HashMap<>(16);

	/** Cache of early singleton objects: bean name to bean instance. */
	// 早期单例对象的缓存：bean 名称到 bean 实例,早期未处理 Bean(属性)
	//
	// 循环引用中：
	// 二级缓存 存的是实例化后的 bean 此时尚未初始化，属性未注入\
	private final Map<String, Object> earlySingletonObjects = new ConcurrentHashMap<>(16);

	/** Set of registered singletons, containing the bean names in registration order. */
	// 一组已注册的单例，包含按注册顺序排列的 bean 名称,
	// 在LinkedHashMap 中 accessOrder 此链接哈希映射的迭代排序方法：<tt>true<tt> 表示访问顺序，<tt>false<tt> 表示插入顺序
	private final Set<String> registeredSingletons = new LinkedHashSet<>(256);

	/** Names of beans that are currently in creation. */
	// 当前正在创建的 bean 的名称
	private final Set<String> singletonsCurrentlyInCreation =
			Collections.newSetFromMap(new ConcurrentHashMap<>(16));

	/** Names of beans currently excluded from in creation checks. */
	// 当前从创建检查中排除的 bean 的名称
	private final Set<String> inCreationCheckExclusions =
			Collections.newSetFromMap(new ConcurrentHashMap<>(16));

	/** Collection of suppressed Exceptions, available for associating related causes. */
	// 抑制异常的集合，可用于关联相关原因
	@Nullable
	private Set<Exception> suppressedExceptions;

	/** Flag that indicates whether we're currently within destroySingletons. */
	// 指示我们当前是否在 destroySingletons 中的标志
	private boolean singletonsCurrentlyInDestruction = false;

	/** Disposable bean instances: bean name to disposable instance. */
	// 一次性bean实例：bean名称到一次性实例
	private final Map<String, Object> disposableBeans = new LinkedHashMap<>();

	/** Map between containing bean names: bean name to Set of bean names that the bean contains. */
	// 包含 bean 名称之间的映射：bean 名称到 bean 包含的 bean 名称集
	private final Map<String, Set<String>> containedBeanMap = new ConcurrentHashMap<>(16);

	/** Map between dependent bean names: bean name to Set of dependent bean names. */
	// 依赖bean名称之间的映射：bean名称到依赖bean名称集
	private final Map<String, Set<String>> dependentBeanMap = new ConcurrentHashMap<>(64);

	/** Map between depending bean names: bean name to Set of bean names for the bean's dependencies. */
	// 依赖 bean 名称之间的映射：bean 名称到 bean 依赖项的 bean 名称集
	private final Map<String, Set<String>> dependenciesForBeanMap = new ConcurrentHashMap<>(64);


	@Override
	public void registerSingleton(String beanName, Object singletonObject) throws IllegalStateException {
		Assert.notNull(beanName, "Bean name must not be null");
		Assert.notNull(singletonObject, "Singleton object must not be null");
		// 二元操作：加锁对象本身是单例对象的缓存 ConcurrentHashMap，二元表现在逻辑先去get后去添加，若不加锁，两个线程都去get发现
		// 缓存中没有对象，然后同时走在添加模块，这样就有可能出现后边来的覆盖前边来的
		synchronized (this.singletonObjects) {
			Object oldObject = this.singletonObjects.get(beanName);
			if (oldObject != null) {
				throw new IllegalStateException("Could not register object [" + singletonObject +
						"] under bean name '" + beanName + "': there is already object [" + oldObject + "] bound");
			}
			// 注册单例 bean
			addSingleton(beanName, singletonObject);
		}
	}

	/**
	 * Add the given singleton object to the singleton cache of this factory.
	 * <p>To be called for eager registration of singletons.
	 * @param beanName the name of the bean
	 * @param singletonObject the singleton object
	 */
	// 将给定的单例对象添加到该工厂的单例缓存中
	// 被实时注入单例对象调用，将单例 Bean 添加到工厂缓存中，后续查找会先从缓存中查找
	protected void addSingleton(String beanName, Object singletonObject) {
		// 这里加锁的原因是本方法可能会被单独调用，这里由于锁对象是同一个，按照重进入的方式
		// 本方法不会增加新的锁，而会使用方法外边同一锁对象
		synchronized (this.singletonObjects) {
			// 缓存中加入bean名称，单例对象
			this.singletonObjects.put(beanName, singletonObject);
			// 单例工厂移除beanName对象名称
			this.singletonFactories.remove(beanName);
			// 早期单例对象的缓存中移除此单例对象名称,不移除会重新创建一个新的对象。
			this.earlySingletonObjects.remove(beanName);
			// 一组已注册的单例中有序添加bean名称
			this.registeredSingletons.add(beanName);
		}
	}

	/**
	 * Add the given singleton factory for building the specified singleton
	 * if necessary.
	 * <p>To be called for eager registration of singletons, e.g. to be able to
	 * resolve circular references.
	 * @param beanName the name of the bean
	 * @param singletonFactory the factory for the singleton object
	 */
	// 如有必要，添加给定的单例工厂以构建指定的单例。 <p>被调用以急切注册单例，例如能够解决循环引用。
	// 循环引用的具体处理，3 个 Map 来处理循环依赖
	protected void addSingletonFactory(String beanName, ObjectFactory<?> singletonFactory) {
		Assert.notNull(singletonFactory, "Singleton factory must not be null");
		// 3 个 Map 来处理循环依赖(singletonObjects,singletonFactories,earlySingletonObjects)
		synchronized (this.singletonObjects) {
			if (!this.singletonObjects.containsKey(beanName)) {
				// 循环引用，一级缓存，存对象工厂
				this.singletonFactories.put(beanName, singletonFactory);
				// 二级缓存，删除工厂 beanName
				this.earlySingletonObjects.remove(beanName);
				this.registeredSingletons.add(beanName);
			}
		}
	}

	// 从单例注册中心中根据 beanName 获取对象
	@Override
	@Nullable
	public Object getSingleton(String beanName) {
		return getSingleton(beanName, true);
	}

	/**
	 * Return the (raw) singleton object registered under the given name.
	 * <p>Checks already instantiated singletons and also allows for an early
	 * reference to a currently created singleton (resolving a circular reference).
	 * @param beanName the name of the bean to look for
	 * @param allowEarlyReference whether early references should be created or not
	 * @return the registered singleton object, or {@code null} if none found
	 */
	// 返回在给定名称下注册的（原始）单例对象。
	// <p>检查已经实例化的单例，并允许对当前创建的单例进行早期引用（解决循环引用）
	// 依赖查找伴随着依赖注入
	@Nullable
	protected Object getSingleton(String beanName, boolean allowEarlyReference) {
		// Quick check for existing instance without full singleton lock
		// 快速检查没有完整单例锁的现有实例
		Object singletonObject = this.singletonObjects.get(beanName); // 一级缓存中查找
		if (singletonObject == null && isSingletonCurrentlyInCreation(beanName)) { // 正在创建
			singletonObject = this.earlySingletonObjects.get(beanName); // 二级缓存中获取对象
			if (singletonObject == null && allowEarlyReference) {
				synchronized (this.singletonObjects) {
					// Consistent creation of early reference within full singleton lock
					// 在完整的单例锁中一致地创建早期引用
					singletonObject = this.singletonObjects.get(beanName);
					if (singletonObject == null) {
						singletonObject = this.earlySingletonObjects.get(beanName);
						if (singletonObject == null) {
							// 三级缓存：查找对象工厂
							ObjectFactory<?> singletonFactory = this.singletonFactories.get(beanName);
							if (singletonFactory != null) {
								// 通过对象工厂来获得对象，getObject() 就是执行 lamda 获取原始对象或代理对象
								singletonObject = singletonFactory.getObject();
								// 二级缓存：将对象工厂获取来的对象放入二级缓存 earlySingletonObjects
								this.earlySingletonObjects.put(beanName, singletonObject);
								// 三级工厂：移除工厂对象
								this.singletonFactories.remove(beanName);
							}
						}
					}
				}
			}
		}
		return singletonObject;
	}

	/**
	 * Return the (raw) singleton object registered under the given name,
	 * creating and registering a new one if none registered yet.
	 * @param beanName the name of the bean
	 * @param singletonFactory the ObjectFactory to lazily create the singleton
	 * with, if necessary
	 * @return the registered singleton object
	 */
	// 返回以给定名称注册的（原始）单例对象，如果尚未注册，则创建并注册一个新对象，BeanDefinition->Bean
	// 如有必要，用于懒惰地方式创建单例的 ObjectFactory
	public Object getSingleton(String beanName, ObjectFactory<?> singletonFactory) {
		Assert.notNull(beanName, "Bean name must not be null");
		// singletonObjects 在 SingletonBeanRegistry#registerSingleton中注册一个单例对象，Spring IoC 没有对这个对象
		// 任何的生命周期管理，是一个外部传入的对象，singletonObjects 就是这个注册的缓存
		synchronized (this.singletonObjects) { // 读写操作同时发生，需要加锁
			Object singletonObject = this.singletonObjects.get(beanName);
			if (singletonObject == null) {
				if (this.singletonsCurrentlyInDestruction) {
					throw new BeanCreationNotAllowedException(beanName,
							"Singleton bean creation not allowed while singletons of this factory are in destruction " +
							"(Do not request a bean from a BeanFactory in a destroy method implementation!)");
				}
				if (logger.isDebugEnabled()) {
					logger.debug("Creating shared instance of singleton bean '" + beanName + "'");
				}
				beforeSingletonCreation(beanName);
				boolean newSingleton = false;
				boolean recordSuppressedExceptions = (this.suppressedExceptions == null);
				if (recordSuppressedExceptions) {
					this.suppressedExceptions = new LinkedHashSet<>();
				}
				try {
					// 这里的 singletonFactory 其实就是前边的 lamda 表达式
					singletonObject = singletonFactory.getObject();
					newSingleton = true;
				}
				catch (IllegalStateException ex) {
					// Has the singleton object implicitly appeared in the meantime ->
					// if yes, proceed with it since the exception indicates that state.
					// 在此期间是否隐式出现了单例对象 -> 如果是，则继续处理它，因为异常指示该状态
					singletonObject = this.singletonObjects.get(beanName);
					if (singletonObject == null) {
						throw ex;
					}
				}
				catch (BeanCreationException ex) {
					if (recordSuppressedExceptions) {
						for (Exception suppressedException : this.suppressedExceptions) {
							ex.addRelatedCause(suppressedException);
						}
					}
					throw ex;
				}
				finally {
					if (recordSuppressedExceptions) {
						this.suppressedExceptions = null;
					}
					afterSingletonCreation(beanName);
				}
				if (newSingleton) {
					addSingleton(beanName, singletonObject);
				}
			}
			return singletonObject;
		}
	}

	/**
	 * Register an exception that happened to get suppressed during the creation of a
	 * singleton bean instance, e.g. a temporary circular reference resolution problem.
	 * <p>The default implementation preserves any given exception in this registry's
	 * collection of suppressed exceptions, up to a limit of 100 exceptions, adding
	 * them as related causes to an eventual top-level {@link BeanCreationException}.
	 * @param ex the Exception to register
	 * @see BeanCreationException#getRelatedCauses()
	 */
	// 注册在创建单例 bean 实例期间碰巧被抑制的异常，例如一个临时的循环引用解析问题。
	// <p>默认实现会在此注册表的抑制异常集合中保留任何给定异常，最多 100 个异常，将它们作为相关原因添加到最终的顶级
	protected void onSuppressedException(Exception ex) {
		synchronized (this.singletonObjects) {
			if (this.suppressedExceptions != null && this.suppressedExceptions.size() < SUPPRESSED_EXCEPTIONS_LIMIT) {
				this.suppressedExceptions.add(ex);
			}
		}
	}

	/**
	 * Remove the bean with the given name from the singleton cache of this factory,
	 * to be able to clean up eager registration of a singleton if creation failed.
	 * @param beanName the name of the bean
	 * @see #getSingletonMutex()
	 */
	// 从该工厂的单例缓存中删除具有给定名称的 bean，以便能够在创建失败时清除单例的急切注册
	protected void removeSingleton(String beanName) {
		synchronized (this.singletonObjects) {
			this.singletonObjects.remove(beanName);
			this.singletonFactories.remove(beanName);
			this.earlySingletonObjects.remove(beanName);
			this.registeredSingletons.remove(beanName);
		}
	}

	@Override
	public boolean containsSingleton(String beanName) {
		return this.singletonObjects.containsKey(beanName);
	}

	@Override
	public String[] getSingletonNames() {
		synchronized (this.singletonObjects) {
			return StringUtils.toStringArray(this.registeredSingletons);
		}
	}

	@Override
	public int getSingletonCount() {
		synchronized (this.singletonObjects) {
			return this.registeredSingletons.size();
		}
	}


	public void setCurrentlyInCreation(String beanName, boolean inCreation) {
		Assert.notNull(beanName, "Bean name must not be null");
		if (!inCreation) {
			this.inCreationCheckExclusions.add(beanName);
		}
		else {
			this.inCreationCheckExclusions.remove(beanName);
		}
	}

	public boolean isCurrentlyInCreation(String beanName) {
		Assert.notNull(beanName, "Bean name must not be null");
		return (!this.inCreationCheckExclusions.contains(beanName) && isActuallyInCreation(beanName));
	}

	protected boolean isActuallyInCreation(String beanName) {
		return isSingletonCurrentlyInCreation(beanName);
	}

	/**
	 * Return whether the specified singleton bean is currently in creation
	 * (within the entire factory).
	 * @param beanName the name of the bean
	 */
	// 返回指定的单例 bean 当前是否正在创建中（在整个工厂内）。
	// @param beanName bean 的名称
	public boolean isSingletonCurrentlyInCreation(String beanName) {
		return this.singletonsCurrentlyInCreation.contains(beanName);
	}

	/**
	 * Callback before singleton creation.
	 * <p>The default implementation register the singleton as currently in creation.
	 * @param beanName the name of the singleton about to be created
	 * @see #isSingletonCurrentlyInCreation
	 */
	// 单例创建之前的回调。 <p>默认实现将单例注册为当前正在创建。
	protected void beforeSingletonCreation(String beanName) {
		if (!this.inCreationCheckExclusions.contains(beanName) && !this.singletonsCurrentlyInCreation.add(beanName)) {
			throw new BeanCurrentlyInCreationException(beanName);
		}
	}

	/**
	 * Callback after singleton creation.
	 * <p>The default implementation marks the singleton as not in creation anymore.
	 * @param beanName the name of the singleton that has been created
	 * @see #isSingletonCurrentlyInCreation
	 */
	// 创建单例后回调
	// <p>默认实现将单例标记为不再创建。
	// @param beanName 已创建的单例名称
	protected void afterSingletonCreation(String beanName) {
		if (!this.inCreationCheckExclusions.contains(beanName) && !this.singletonsCurrentlyInCreation.remove(beanName)) {
			throw new IllegalStateException("Singleton '" + beanName + "' isn't currently in creation");
		}
	}


	/**
	 * Add the given bean to the list of disposable beans in this registry.
	 * <p>Disposable beans usually correspond to registered singletons,
	 * matching the bean name but potentially being a different instance
	 * (for example, a DisposableBean adapter for a singleton that does not
	 * naturally implement Spring's DisposableBean interface).
	 * @param beanName the name of the bean
	 * @param bean the bean instance
	 */
	// 将给定的 bean 添加到此注册表中的一次性 bean 列表中。
	// <p>一次性 bean 通常对应于已注册的单例，与 bean 名称匹配，但可能是不同的实例
	// （例如，单例的 DisposableBean 适配器不自然实现 Spring 的 DisposableBean 接口）
	public void registerDisposableBean(String beanName, DisposableBean bean) {
		synchronized (this.disposableBeans) {
			this.disposableBeans.put(beanName, bean);
		}
	}

	/**
	 * Register a containment relationship between two beans,
	 * e.g. between an inner bean and its containing outer bean.
	 * <p>Also registers the containing bean as dependent on the contained bean
	 * in terms of destruction order.
	 * @param containedBeanName the name of the contained (inner) bean
	 * @param containingBeanName the name of the containing (outer) bean
	 * @see #registerDependentBean
	 */
	// 注册两个 bean 之间的包含关系，例如在内部 bean 和包含它的外部 bean 之间。
	// <p>还根据销毁顺序将包含的 bean 注册为依赖于所包含的 bean
	// @param containedBeanName 包含的（内部）bean 的名称
	// @param containingBeanName 包含（外部）bean 的名称
	public void registerContainedBean(String containedBeanName, String containingBeanName) {
		synchronized (this.containedBeanMap) {
			Set<String> containedBeans =
					this.containedBeanMap.computeIfAbsent(containingBeanName, k -> new LinkedHashSet<>(8));
			if (!containedBeans.add(containedBeanName)) {
				return;
			}
		}
		registerDependentBean(containedBeanName, containingBeanName);
	}

	/**
	 * Register a dependent bean for the given bean,
	 * to be destroyed before the given bean is destroyed.
	 * @param beanName the name of the bean
	 * @param dependentBeanName the name of the dependent bean
	 */
	// 为给定的 bean 注册一个依赖 bean，在给定的 bean 被销毁之前被销毁
	public void registerDependentBean(String beanName, String dependentBeanName) {
		String canonicalName = canonicalName(beanName);

		// 依赖 bean 名称之间的依赖
		synchronized (this.dependentBeanMap) {
			Set<String> dependentBeans =
					this.dependentBeanMap.computeIfAbsent(canonicalName, k -> new LinkedHashSet<>(8));
			if (!dependentBeans.add(dependentBeanName)) {
				return;
			}
		}

		synchronized (this.dependenciesForBeanMap) {
			Set<String> dependenciesForBean =
					this.dependenciesForBeanMap.computeIfAbsent(dependentBeanName, k -> new LinkedHashSet<>(8));
			dependenciesForBean.add(canonicalName);
		}
	}

	/**
	 * Determine whether the specified dependent bean has been registered as
	 * dependent on the given bean or on any of its transitive dependencies.
	 * @param beanName the name of the bean to check
	 * @param dependentBeanName the name of the dependent bean
	 * @since 4.0
	 */
	// 确定指定的依赖 bean 是否已注册为依赖于给定的 bean 或其任何传递依赖项。
	// @param beanName 要检查的 bean 的名称
	// @paramdependentBeanName 依赖 bean 的名称
	// @since 4.0
	protected boolean isDependent(String beanName, String dependentBeanName) {
		synchronized (this.dependentBeanMap) {
			return isDependent(beanName, dependentBeanName, null);
		}
	}

	private boolean isDependent(String beanName, String dependentBeanName, @Nullable Set<String> alreadySeen) {
		if (alreadySeen != null && alreadySeen.contains(beanName)) {
			return false;
		}
		String canonicalName = canonicalName(beanName);
		Set<String> dependentBeans = this.dependentBeanMap.get(canonicalName);
		if (dependentBeans == null) {
			return false;
		}
		if (dependentBeans.contains(dependentBeanName)) {
			return true;
		}
		for (String transitiveDependency : dependentBeans) {
			if (alreadySeen == null) {
				alreadySeen = new HashSet<>();
			}
			alreadySeen.add(beanName);
			if (isDependent(transitiveDependency, dependentBeanName, alreadySeen)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Determine whether a dependent bean has been registered for the given name.
	 * @param beanName the name of the bean to check
	 */
	// 确定是否已为给定名称注册了依赖 bean。 @param beanName 要检查的 bean 的名称
	protected boolean hasDependentBean(String beanName) {
		return this.dependentBeanMap.containsKey(beanName);
	}

	/**
	 * Return the names of all beans which depend on the specified bean, if any.
	 * @param beanName the name of the bean
	 * @return the array of dependent bean names, or an empty array if none
	 */
	// 返回依赖于指定 bean 的所有 bean 的名称（如果有）
	// @param beanName bean 的名称
	// @return 依赖 bean 名称的数组，如果没有则为空数组
	public String[] getDependentBeans(String beanName) {
		Set<String> dependentBeans = this.dependentBeanMap.get(beanName);
		if (dependentBeans == null) {
			return new String[0];
		}
		synchronized (this.dependentBeanMap) {
			return StringUtils.toStringArray(dependentBeans);
		}
	}

	/**
	 * Return the names of all beans that the specified bean depends on, if any.
	 * @param beanName the name of the bean
	 * @return the array of names of beans which the bean depends on,
	 * or an empty array if none
	 */
	// 返回指定 bean 所依赖的所有 bean 的名称（如果有）。
	// @param beanName bean 的名称
	// @return bean 依赖的 bean 名称数组，如果没有则返回空数组
	public String[] getDependenciesForBean(String beanName) {
		Set<String> dependenciesForBean = this.dependenciesForBeanMap.get(beanName);
		if (dependenciesForBean == null) {
			return new String[0];
		}
		synchronized (this.dependenciesForBeanMap) {
			return StringUtils.toStringArray(dependenciesForBean);
		}
	}

	public void destroySingletons() {
		if (logger.isTraceEnabled()) {
			logger.trace("Destroying singletons in " + this);
		}
		// 名称，单例对象
		synchronized (this.singletonObjects) {
			// 当前单例正在毁灭中
			this.singletonsCurrentlyInDestruction = true;
		}

		String[] disposableBeanNames;
		// 一次性bean实例：bean名称到一次性实例，即实现了 DisposableBean 的接口集合
		synchronized (this.disposableBeans) {
			disposableBeanNames = StringUtils.toStringArray(this.disposableBeans.keySet());
		}
		for (int i = disposableBeanNames.length - 1; i >= 0; i--) {
			// 销毁给定的 bean。如果找到相应的一次性 bean 实例，则委托给 {@code destroyBean}
			destroySingleton(disposableBeanNames[i]);
		}

		// 通过 beanName 来销毁bean相关容器
		this.containedBeanMap.clear();
		this.dependentBeanMap.clear();
		this.dependenciesForBeanMap.clear();

		// 清除缓存，做完全部这些，对象并不是立马销毁，只是和容器进行了脱钩，
		// 在GC的情况下，所有这些对象被标记为不可达对象，可以被 JVM 销毁
		clearSingletonCache();
	}

	/**
	 * Clear all cached singleton instances in this registry.
	 * @since 4.3.15
	 */
	// 清除此注册表中所有缓存的单例实例
	protected void clearSingletonCache() {
		synchronized (this.singletonObjects) {
			this.singletonObjects.clear();
			this.singletonFactories.clear();
			this.earlySingletonObjects.clear();
			this.registeredSingletons.clear();
			this.singletonsCurrentlyInDestruction = false;
		}
	}

	/**
	 * Destroy the given bean. Delegates to {@code destroyBean}
	 * if a corresponding disposable bean instance is found.
	 * @param beanName the name of the bean
	 * @see #destroyBean
	 */
	// 销毁给定的 bean。如果找到相应的一次性 bean 实例，则委托给 {@code destroyBean}
	public void destroySingleton(String beanName) {
		// Remove a registered singleton of the given name, if any.
		// 删除给定名称的已注册单例（如果有）
		removeSingleton(beanName);

		// Destroy the corresponding DisposableBean instance.
		// 销毁相应的 DisposableBean 实例
		DisposableBean disposableBean;
		synchronized (this.disposableBeans) {
			disposableBean = (DisposableBean) this.disposableBeans.remove(beanName);
		}
		destroyBean(beanName, disposableBean);
	}

	/**
	 * Destroy the given bean. Must destroy beans that depend on the given
	 * bean before the bean itself. Should not throw any exceptions.
	 * @param beanName the name of the bean
	 * @param bean the bean instance to destroy
	 */
	// 销毁给定的 bean。必须在 bean 本身之前销毁依赖于给定 bean 的 bean。不应抛出任何异常
	// @param beanName bean 的名称
	// @param bean 要销毁的 bean 实例
	protected void destroyBean(String beanName, @Nullable DisposableBean bean) {
		// Trigger destruction of dependent beans first...
		// 首先触发依赖bean的销毁
		Set<String> dependencies;
		synchronized (this.dependentBeanMap) {
			// Within full synchronization in order to guarantee a disconnected Set
			// 在完全同步内以保证断开的 Set
			dependencies = this.dependentBeanMap.remove(beanName);
		}
		if (dependencies != null) {
			if (logger.isTraceEnabled()) {
				// 为 bean(beanName) 检索依赖 beans:dependencies
				logger.trace("Retrieved dependent beans for bean '" + beanName + "': " + dependencies);
			}
			for (String dependentBeanName : dependencies) {
				// 迭代逐一销毁依赖的bean
				destroySingleton(dependentBeanName);
			}
		}

		// Actually destroy the bean now...
		// 实际上现在销毁bean...
		if (bean != null) {
			try {
				// 销毁 bean
				bean.destroy();
			}
			catch (Throwable ex) {
				if (logger.isWarnEnabled()) {
					logger.warn("Destruction of bean with name '" + beanName + "' threw an exception", ex);
				}
			}
		}

		// Trigger destruction of contained beans...
		// 触发销毁包含的 bean...
		Set<String> containedBeans;
		synchronized (this.containedBeanMap) {
			// Within full synchronization in order to guarantee a disconnected Set
			// 在完全同步内以保证断开的 Set
			containedBeans = this.containedBeanMap.remove(beanName);
		}
		if (containedBeans != null) {
			for (String containedBeanName : containedBeans) {
				destroySingleton(containedBeanName);
			}
		}

		// Remove destroyed bean from other beans' dependencies.
		// 从其他 bean 的依赖项中删除被破坏的 bean
		synchronized (this.dependentBeanMap) {
			for (Iterator<Map.Entry<String, Set<String>>> it = this.dependentBeanMap.entrySet().iterator(); it.hasNext();) {
				Map.Entry<String, Set<String>> entry = it.next();
				Set<String> dependenciesToClean = entry.getValue();
				dependenciesToClean.remove(beanName);
				if (dependenciesToClean.isEmpty()) {
					it.remove();
				}
			}
		}

		// Remove destroyed bean's prepared dependency information.
		// 删除销毁的 bean 准备好的依赖信息
		this.dependenciesForBeanMap.remove(beanName);
	}

	/**
	 * Exposes the singleton mutex to subclasses and external collaborators.
	 * <p>Subclasses should synchronize on the given Object if they perform
	 * any sort of extended singleton creation phase. In particular, subclasses
	 * should <i>not</i> have their own mutexes involved in singleton creation,
	 * to avoid the potential for deadlocks in lazy-init situations.
	 */
	// 将单例互斥体暴露给子类和外部合作者。
	// <p>如果子类执行任何类型的扩展单例创建阶段，它们应该在给定的对象上同步。
	// 特别是，子类应该<i>不<i>在单例创建中使用它们自己的互斥锁，以避免在惰性初始化情况下潜在的死锁
	@Override
	public final Object getSingletonMutex() {
		return this.singletonObjects;
	}

}
