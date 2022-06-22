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
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.aopalliance.aop.Advice;

import org.springframework.aop.Advisor;
import org.springframework.aop.DynamicIntroductionAdvice;
import org.springframework.aop.IntroductionAdvisor;
import org.springframework.aop.IntroductionInfo;
import org.springframework.aop.TargetSource;
import org.springframework.aop.support.DefaultIntroductionAdvisor;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.aop.target.EmptyTargetSource;
import org.springframework.aop.target.SingletonTargetSource;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;

/**
 * Base class for AOP proxy configuration managers.
 * These are not themselves AOP proxies, but subclasses of this class are
 * normally factories from which AOP proxy instances are obtained directly.
 *
 * <p>This class frees subclasses of the housekeeping of Advices
 * and Advisors, but doesn't actually implement proxy creation
 * methods, which are provided by subclasses.
 *
 * <p>This class is serializable; subclasses need not be.
 * This class is used to hold snapshots of proxies.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see org.springframework.aop.framework.AopProxy
 */
// AOP 代理配置管理器的基类。 它们本身不是 AOP 代理，但此类的子类通常是直接从中获取 AOP 代理实例的工厂。
// 该类释放了 Advices 和 Advisors 的内务处理的子类，但实际上并未实现由子类提供的代理创建方法。
// 这个类是可序列化的； 子类不需要。 此类用于保存代理的快照。
// 代理配置类:包含了 AOP API,IoC 以及 AspectJ 和 IoC 的整合，这是入口
// AopProxyFactory 配置管理器，代理对象的配置，比如说 TargetClass,Advice 所转换的 MethodInterceptor
// DynamicInterceptionAdvice
//
// 一个 bean 对应一个 AdvisedSupport,一一对应即一个 bean 一个配置
public class AdvisedSupport extends ProxyConfig implements Advised {

	/** use serialVersionUID from Spring 2.0 for interoperability. */
	private static final long serialVersionUID = 2651364800145442165L;


	/**
	 * Canonical TargetSource when there's no target, and behavior is
	 * supplied by the advisors.
	 */
	// 没有目标时的规范 TargetSource，行为由 advisors 顾问提供
	public static final TargetSource EMPTY_TARGET_SOURCE = EmptyTargetSource.INSTANCE;


	/** Package-protected to allow direct access for efficiency. */
	// Package-protected 允许直接访问以提高效率
	// 目标代理对象
	TargetSource targetSource = EMPTY_TARGET_SOURCE;

	/** Whether the Advisors are already filtered for the specific target class. */
	// 顾问 Advisors 是否已针对特定目标类进行过滤
	private boolean preFiltered = false;

	/** The AdvisorChainFactory to use. */
	// 要使用的 AdvisorChainFactory,唯一实现 DefaultAdvisorChainFactory
	AdvisorChainFactory advisorChainFactory = new DefaultAdvisorChainFactory();

	/** Cache with Method as key and advisor chain List as value. */
	// 以 Method 为 key 缓存，以advisor chain 顾问链 List 为值
	private transient Map<MethodCacheKey, List<Object>> methodCache;

	/**
	 * Interfaces to be implemented by the proxy. Held in List to keep the order
	 * of registration, to create JDK proxy with specified order of interfaces.
	 */
	// 要由代理实现的接口。 在 List 中保持注册顺序，创建指定接口顺序的 JDK 代理
	private List<Class<?>> interfaces = new ArrayList<>();

	/**
	 * List of Advisors. If an Advice is added, it will be wrapped
	 * in an Advisor before being added to this List.
	 */
	// 顾问名单。 如果添加了 Advice，它将在添加到此 List 之前包装在 Advisor 中。
	// 拦截器
	private List<Advisor> advisors = new ArrayList<>();


	/**
	 * No-arg constructor for use as a JavaBean.
	 */
	// 用作 JavaBean 的无参数构造函数
	// AOP 配置
	public AdvisedSupport() {
		this.methodCache = new ConcurrentHashMap<>(32);
	}

	/**
	 * Create a AdvisedSupport instance with the given parameters.
	 * @param interfaces the proxied interfaces
	 */
	// 使用给定的参数创建一个 AdvisedSupport 实例。
	// 形参：
	// 			interfaces - 代理接口
	public AdvisedSupport(Class<?>... interfaces) {
		this();
		setInterfaces(interfaces);
	}


	/**
	 * Set the given object as target.
	 * Will create a SingletonTargetSource for the object.
	 * @see #setTargetSource
	 * @see org.springframework.aop.target.SingletonTargetSource
	 */
	// 将给定对象设置为目标。 将为对象创建一个 SingletonTargetSource
	public void setTarget(Object target) {
		setTargetSource(new SingletonTargetSource(target));
	}

	// 设置资源池
	@Override
	public void setTargetSource(@Nullable TargetSource targetSource) {
		this.targetSource = (targetSource != null ? targetSource : EMPTY_TARGET_SOURCE);
	}

	@Override
	public TargetSource getTargetSource() {
		return this.targetSource;
	}

	/**
	 * Set a target class to be proxied, indicating that the proxy
	 * should be castable to the given class.
	 * <p>Internally, an {@link org.springframework.aop.target.EmptyTargetSource}
	 * for the given target class will be used. The kind of proxy needed
	 * will be determined on actual creation of the proxy.
	 * <p>This is a replacement for setting a "targetSource" or "target",
	 * for the case where we want a proxy based on a target class
	 * (which can be an interface or a concrete class) without having
	 * a fully capable TargetSource available.
	 * @see #setTargetSource
	 * @see #setTarget
	 */
	// 设置要代理的目标类，表示代理应该可转换为给定的类。
	// 在内部，将使用给定目标类的EmptyTargetSource 。 所需的代理类型将取决于代理的实际创建。
	// 这是对设置“targetSource”或“target”的替代，用于我们想要基于目标类（可以是接口或具体类）的代理
	// 而没有完全可用的 TargetSource 的情况。
	public void setTargetClass(@Nullable Class<?> targetClass) {
		this.targetSource = EmptyTargetSource.forClass(targetClass);
	}

	@Override
	@Nullable
	public Class<?> getTargetClass() {
		return this.targetSource.getTargetClass();
	}

	@Override
	public void setPreFiltered(boolean preFiltered) {
		this.preFiltered = preFiltered;
	}

	@Override
	public boolean isPreFiltered() {
		return this.preFiltered;
	}

	/**
	 * Set the advisor chain factory to use.
	 * <p>Default is a {@link DefaultAdvisorChainFactory}.
	 */
	// 设置要使用的顾问链工厂。
	// 默认是 DefaultAdvisorChainFactory
	public void setAdvisorChainFactory(AdvisorChainFactory advisorChainFactory) {
		Assert.notNull(advisorChainFactory, "AdvisorChainFactory must not be null");
		this.advisorChainFactory = advisorChainFactory;
	}

	/**
	 * Return the advisor chain factory to use (never {@code null}).
	 */
	// 返回要使用的顾问链工厂（从不为null ）。
	public AdvisorChainFactory getAdvisorChainFactory() {
		return this.advisorChainFactory;
	}


	/**
	 * Set the interfaces to be proxied.
	 */
	// 设置要代理的接口。
	public void setInterfaces(Class<?>... interfaces) {
		Assert.notNull(interfaces, "Interfaces must not be null");
		this.interfaces.clear();
		for (Class<?> ifc : interfaces) {
			addInterface(ifc);
		}
	}

	/**
	 * Add a new proxied interface.
	 * @param intf the additional interface to proxy
	 */
	// 添加一个新的代理接口。
	// 形参：
	//			intf – 代理的附加接口
	public void addInterface(Class<?> intf) {
		Assert.notNull(intf, "Interface must not be null");
		if (!intf.isInterface()) {
			throw new IllegalArgumentException("[" + intf.getName() + "] is not an interface");
		}
		if (!this.interfaces.contains(intf)) { // 去重操作
			this.interfaces.add(intf);
			adviceChanged();
		}
	}

	/**
	 * Remove a proxied interface.
	 * <p>Does nothing if the given interface isn't proxied.
	 * @param intf the interface to remove from the proxy
	 * @return {@code true} if the interface was removed; {@code false}
	 * if the interface was not found and hence could not be removed
	 */
	// 删除代理接口。
	// 如果给定的接口没有被代理，则什么都不做。
	// 形参：
	//			intf – 要从代理中删除的接口
	// 返回值：
	//			如果接口被移除，则为true ； 如果接口未找到，因此无法删除，则为false
	public boolean removeInterface(Class<?> intf) {
		return this.interfaces.remove(intf);
	}

	@Override
	public Class<?>[] getProxiedInterfaces() {
		return ClassUtils.toClassArray(this.interfaces);
	}

	@Override
	public boolean isInterfaceProxied(Class<?> intf) {
		for (Class<?> proxyIntf : this.interfaces) {
			if (intf.isAssignableFrom(proxyIntf)) {
				return true;
			}
		}
		return false;
	}


	@Override
	public final Advisor[] getAdvisors() {
		return this.advisors.toArray(new Advisor[0]);
	}

	@Override
	public int getAdvisorCount() {
		return this.advisors.size();
	}

	@Override
	public void addAdvisor(Advisor advisor) {
		int pos = this.advisors.size();
		// 先入先出，每次都会添加到末尾
		addAdvisor(pos, advisor);
	}

	@Override
	public void addAdvisor(int pos, Advisor advisor) throws AopConfigException {
		if (advisor instanceof IntroductionAdvisor) {
			// 校验
			validateIntroductionAdvisor((IntroductionAdvisor) advisor);
		}
		addAdvisorInternal(pos, advisor);
	}

	@Override
	public boolean removeAdvisor(Advisor advisor) {
		int index = indexOf(advisor);
		if (index == -1) {
			return false;
		}
		else {
			removeAdvisor(index);
			return true;
		}
	}

	@Override
	public void removeAdvisor(int index) throws AopConfigException {
		if (isFrozen()) {
			throw new AopConfigException("Cannot remove Advisor: Configuration is frozen.");
		}
		if (index < 0 || index > this.advisors.size() - 1) {
			throw new AopConfigException("Advisor index " + index + " is out of bounds: " +
					"This configuration only has " + this.advisors.size() + " advisors.");
		}

		Advisor advisor = this.advisors.remove(index);
		if (advisor instanceof IntroductionAdvisor) {
			IntroductionAdvisor ia = (IntroductionAdvisor) advisor;
			// We need to remove introduction interfaces.
			for (Class<?> ifc : ia.getInterfaces()) {
				removeInterface(ifc);
			}
		}

		adviceChanged();
	}

	@Override
	public int indexOf(Advisor advisor) {
		Assert.notNull(advisor, "Advisor must not be null");
		return this.advisors.indexOf(advisor);
	}

	@Override
	public boolean replaceAdvisor(Advisor a, Advisor b) throws AopConfigException {
		Assert.notNull(a, "Advisor a must not be null");
		Assert.notNull(b, "Advisor b must not be null");
		int index = indexOf(a);
		if (index == -1) {
			return false;
		}
		removeAdvisor(index);
		addAdvisor(index, b);
		return true;
	}

	/**
	 * Add all of the given advisors to this proxy configuration.
	 * @param advisors the advisors to register
	 */
	// 将所有给定的顾问添加到此代理配置中。
	// 形参：
	//			advisors – 要注册的顾问
	public void addAdvisors(Advisor... advisors) {
		addAdvisors(Arrays.asList(advisors));
	}

	/**
	 * Add all of the given advisors to this proxy configuration.
	 * @param advisors the advisors to register
	 */
	// 将所有给定的顾问添加到此代理配置中。
	// 形参：
	//			advisors – 要注册的顾问
	public void addAdvisors(Collection<Advisor> advisors) {
		if (isFrozen()) {
			throw new AopConfigException("Cannot add advisor: Configuration is frozen.");
		}
		if (!CollectionUtils.isEmpty(advisors)) {
			for (Advisor advisor : advisors) {
				if (advisor instanceof IntroductionAdvisor) {
					validateIntroductionAdvisor((IntroductionAdvisor) advisor);
				}
				Assert.notNull(advisor, "Advisor must not be null");
				this.advisors.add(advisor);
			}
			adviceChanged();
		}
	}

	private void validateIntroductionAdvisor(IntroductionAdvisor advisor) {
		advisor.validateInterfaces();
		// If the advisor passed validation, we can make the change.
		// 如果顾问通过验证，我们就可以进行更改。这里会获取用户自己指定的 IntroductionInfo 中的接口
		Class<?>[] ifcs = advisor.getInterfaces();
		for (Class<?> ifc : ifcs) {
			addInterface(ifc);
		}
	}

	private void addAdvisorInternal(int pos, Advisor advisor) throws AopConfigException {
		Assert.notNull(advisor, "Advisor must not be null");
		if (isFrozen()) {
			throw new AopConfigException("Cannot add advisor: Configuration is frozen.");
		}
		if (pos > this.advisors.size()) {
			throw new IllegalArgumentException(
					"Illegal position " + pos + " in advisor list with size " + this.advisors.size());
		}
		// 按照游标的方式加入集合
		this.advisors.add(pos, advisor);
		// 触发事件
		adviceChanged();
	}

	/**
	 * Allows uncontrolled access to the {@link List} of {@link Advisor Advisors}.
	 * <p>Use with care, and remember to {@link #adviceChanged() fire advice changed events}
	 * when making any modifications.
	 */
	// 允许不受控制地访问 Advisors List 。
	// 小心使用，并记住在进行任何修改时发出 fire advice changed events 。
	protected final List<Advisor> getAdvisorsInternal() {
		return this.advisors;
	}

	// 添加拦截后的处理，会触发 AdvisedSupportListener#adviceChanged(AdvisedSupport advised) 事件
	@Override
	public void addAdvice(Advice advice) throws AopConfigException {
		int pos = this.advisors.size();
		addAdvice(pos, advice);
	}

	/**
	 * Cannot add introductions this way unless the advice implements IntroductionInfo.
	 */
	// 除非建议实现了 IntroductionInfo，否则无法以这种方式添加介绍
	// 就像一个数组一样，从某个位置添加 advice,是个先进先出的操作。
	@Override
	public void addAdvice(int pos, Advice advice) throws AopConfigException {
		Assert.notNull(advice, "Advice must not be null");
		if (advice instanceof IntroductionInfo) {
			// We don't need an IntroductionAdvisor for this kind of introduction:
			// It's fully self-describing.
			// 对于这种介绍，我们不需要 IntroductionAdvisor：它是完全自我描述的。
			addAdvisor(pos, new DefaultIntroductionAdvisor(advice, (IntroductionInfo) advice));
		}
		else if (advice instanceof DynamicIntroductionAdvice) {
			// We need an IntroductionAdvisor for this kind of introduction.
			// 我们需要一个 IntroductionAdvisor 来进行这种介绍
			throw new AopConfigException("DynamicIntroductionAdvice may only be added as part of IntroductionAdvisor");
		}
		else {
			// 将 Advice 封装成 Advisor
			addAdvisor(pos, new DefaultPointcutAdvisor(advice));
		}
	}

	@Override
	public boolean removeAdvice(Advice advice) throws AopConfigException {
		int index = indexOf(advice);
		if (index == -1) {
			return false;
		}
		else {
			removeAdvisor(index);
			return true;
		}
	}

	@Override
	public int indexOf(Advice advice) {
		Assert.notNull(advice, "Advice must not be null");
		for (int i = 0; i < this.advisors.size(); i++) {
			Advisor advisor = this.advisors.get(i);
			if (advisor.getAdvice() == advice) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * Is the given advice included in any advisor within this proxy configuration?
	 * @param advice the advice to check inclusion of
	 * @return whether this advice instance is included
	 */
	// 给定的建议是否包含在此代理配置中的任何顾问程序中？
	// 形参：
	//			advice - 检查包含的建议
	// 返回值：
	//			是否包含此通知实例
	public boolean adviceIncluded(@Nullable Advice advice) {
		if (advice != null) {
			for (Advisor advisor : this.advisors) {
				if (advisor.getAdvice() == advice) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Count advices of the given class.
	 * @param adviceClass the advice class to check
	 * @return the count of the interceptors of this class or subclasses
	 */
	// 计算给定类的建议。
	// 形参：
	//			adviceClass -建议类检查
	// 返回值：
	//			这个类或子类的拦截器的数量
	public int countAdvicesOfType(@Nullable Class<?> adviceClass) {
		int count = 0;
		if (adviceClass != null) {
			for (Advisor advisor : this.advisors) {
				if (adviceClass.isInstance(advisor.getAdvice())) {
					count++;
				}
			}
		}
		return count;
	}


	/**
	 * Determine a list of {@link org.aopalliance.intercept.MethodInterceptor} objects
	 * for the given method, based on this configuration.
	 * @param method the proxied method
	 * @param targetClass the target class
	 * @return a List of MethodInterceptors (may also include InterceptorAndDynamicMethodMatchers)
	 */
	// 根据此配置确定给定方法的org.aopalliance.intercept.MethodInterceptor对象列表。
	// 形参：
	// 			方法- 代理方法
	// 			targetClass – 目标类
	// 返回值：
	//			MethodInterceptors 列表（也可能包括 InterceptorAndDynamicMethodMatchers）
	public List<Object> getInterceptorsAndDynamicInterceptionAdvice(Method method, @Nullable Class<?> targetClass) {
		// 目标方法作为缓存 key
		MethodCacheKey cacheKey = new MethodCacheKey(method);
		List<Object> cached = this.methodCache.get(cacheKey);
		if (cached == null) {
			// 获取拦截器集合
			cached = this.advisorChainFactory.getInterceptorsAndDynamicInterceptionAdvice(
					this, method, targetClass);
			this.methodCache.put(cacheKey, cached);
		}
		return cached;
	}

	/**
	 * Invoked when advice has changed.
	 */
	// 当通知改变时调用。将缓存清除
	protected void adviceChanged() {
		this.methodCache.clear();
	}

	/**
	 * Call this method on a new instance created by the no-arg constructor
	 * to create an independent copy of the configuration from the given object.
	 * @param other the AdvisedSupport object to copy configuration from
	 */
	// 在由无参数构造函数创建的新实例上调用此方法，以从给定对象创建配置的独立副本。
	// 形参：
	//			其他– 要从中复制配置的 AdvisedSupport 对象
	protected void copyConfigurationFrom(AdvisedSupport other) {
		copyConfigurationFrom(other, other.targetSource, new ArrayList<>(other.advisors));
	}

	/**
	 * Copy the AOP configuration from the given AdvisedSupport object,
	 * but allow substitution of a fresh TargetSource and a given interceptor chain.
	 * @param other the AdvisedSupport object to take proxy configuration from
	 * @param targetSource the new TargetSource
	 * @param advisors the Advisors for the chain
	 */
	// 从给定的 AdvisedSupport 对象复制 AOP 配置，但允许替换新的 TargetSource 和给定的拦截器链。
	// 形参：
	// 			other – 要从中获取代理配置的 AdvisedSupport 对象
	//			targetSource – 新的 TargetSource
	//			advisors – 链的顾问
	protected void copyConfigurationFrom(AdvisedSupport other, TargetSource targetSource, List<Advisor> advisors) {
		copyFrom(other);
		this.targetSource = targetSource;
		this.advisorChainFactory = other.advisorChainFactory;
		this.interfaces = new ArrayList<>(other.interfaces);
		for (Advisor advisor : advisors) {
			if (advisor instanceof IntroductionAdvisor) {
				validateIntroductionAdvisor((IntroductionAdvisor) advisor);
			}
			Assert.notNull(advisor, "Advisor must not be null");
			this.advisors.add(advisor);
		}
		adviceChanged();
	}

	/**
	 * Build a configuration-only copy of this AdvisedSupport,
	 * replacing the TargetSource.
	 */
	// 构建此 AdvisedSupport 的仅配置副本，替换 TargetSource
	AdvisedSupport getConfigurationOnlyCopy() {
		AdvisedSupport copy = new AdvisedSupport();
		copy.copyFrom(this);
		copy.targetSource = EmptyTargetSource.forClass(getTargetClass(), getTargetSource().isStatic());
		copy.advisorChainFactory = this.advisorChainFactory;
		copy.interfaces = new ArrayList<>(this.interfaces);
		copy.advisors = new ArrayList<>(this.advisors);
		return copy;
	}


	//---------------------------------------------------------------------
	// Serialization support
	//---------------------------------------------------------------------

	private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
		// Rely on default serialization; just initialize state after deserialization.
		// 依赖默认序列化；反序列化后只需初始化状态
		ois.defaultReadObject();

		// Initialize transient fields.
		// 初始化瞬态字段
		this.methodCache = new ConcurrentHashMap<>(32);
	}

	@Override
	public String toProxyConfigString() {
		return toString();
	}

	/**
	 * For debugging/diagnostic use.
	 */
	// 用于调试/诊断用途。
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(getClass().getName());
		sb.append(": ").append(this.interfaces.size()).append(" interfaces ");
		sb.append(ClassUtils.classNamesToString(this.interfaces)).append("; ");
		sb.append(this.advisors.size()).append(" advisors ");
		sb.append(this.advisors).append("; ");
		sb.append("targetSource [").append(this.targetSource).append("]; ");
		sb.append(super.toString());
		return sb.toString();
	}


	/**
	 * Simple wrapper class around a Method. Used as the key when
	 * caching methods, for efficient equals and hashCode comparisons.
	 */
	// 围绕方法的简单包装类。 在缓存方法时用作键，用于有效的 equals 和 hashCode 比较。
	private static final class MethodCacheKey implements Comparable<MethodCacheKey> {

		private final Method method;

		private final int hashCode;

		public MethodCacheKey(Method method) {
			this.method = method;
			this.hashCode = method.hashCode();
		}

		@Override
		public boolean equals(@Nullable Object other) {
			return (this == other || (other instanceof MethodCacheKey &&
					this.method == ((MethodCacheKey) other).method));
		}

		@Override
		public int hashCode() {
			return this.hashCode;
		}

		@Override
		public String toString() {
			return this.method.toString();
		}

		@Override
		public int compareTo(MethodCacheKey other) {
			int result = this.method.getName().compareTo(other.method.getName());
			if (result == 0) {
				result = this.method.toString().compareTo(other.method.toString());
			}
			return result;
		}
	}

}
