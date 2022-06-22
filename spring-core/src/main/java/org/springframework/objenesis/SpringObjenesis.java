/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.objenesis;

import org.springframework.core.SpringProperties;
import org.springframework.objenesis.instantiator.ObjectInstantiator;
import org.springframework.objenesis.strategy.InstantiatorStrategy;
import org.springframework.objenesis.strategy.StdInstantiatorStrategy;
import org.springframework.util.ConcurrentReferenceHashMap;

/**
 * Spring-specific variant of {@link ObjenesisStd} / {@link ObjenesisBase},
 * providing a cache based on {@code Class} keys instead of class names,
 * and allowing for selective use of the cache.
 *
 * @author Juergen Hoeller
 * @since 4.2
 * @see #isWorthTrying()
 * @see #newInstance(Class, boolean)
 */
// ObjenesisStd / ObjenesisBase Spring 特定变体，提供基于 Class 作为 key 而不是类名的缓存，并允许选择性地使用缓存
public class SpringObjenesis implements Objenesis {

	/**
	 * System property that instructs Spring to ignore Objenesis, not even attempting
	 * to use it. Setting this flag to "true" is equivalent to letting Spring find
	 * out that Objenesis isn't working at runtime, triggering the fallback code path
	 * immediately: Most importantly, this means that all CGLIB AOP proxies will be
	 * created through regular instantiation via a default constructor.
	 */
	// 指示 Spring 忽略 Objenesis 的系统属性，甚至不尝试使用它。 将此标志设置为“true”
	// 相当于让 Spring 发现 Objenesis 在运行时不工作，立即触发回退代码路径：最重要的是，
	// 这意味着所有CGLIB AOP代理将通过默认构造函数通过常规实例化创建.
	public static final String IGNORE_OBJENESIS_PROPERTY_NAME = "spring.objenesis.ignore";


	private final InstantiatorStrategy strategy;

	private final ConcurrentReferenceHashMap<Class<?>, ObjectInstantiator<?>> cache =
			new ConcurrentReferenceHashMap<>();

	private volatile Boolean worthTrying;


	/**
	 * Create a new {@code SpringObjenesis} instance with the
	 * standard instantiator strategy.
	 */
	// 使用标准实例化策略创建一个新的 SpringObjenesis 实例。
	public SpringObjenesis() {
		this(null);
	}

	/**
	 * Create a new {@code SpringObjenesis} instance with the
	 * given standard instantiator strategy.
	 * @param strategy the instantiator strategy to use
	 */
	// 使用给定的标准实例化策略创建一个新的 SpringObjenesis 实例。
	// 形参：
	//			strategy - 要使用的实例化策略
	public SpringObjenesis(InstantiatorStrategy strategy) {
		this.strategy = (strategy != null ? strategy : new StdInstantiatorStrategy());

		// Evaluate the "spring.objenesis.ignore" property upfront...
		// 预先评估“spring.objenesis.ignore”属性...
		if (SpringProperties.getFlag(SpringObjenesis.IGNORE_OBJENESIS_PROPERTY_NAME)) {
			this.worthTrying = Boolean.FALSE;
		}
	}


	/**
	 * Return whether this Objenesis instance is worth trying for instance creation,
	 * i.e. whether it hasn't been used yet or is known to work.
	 * <p>If the configured Objenesis instantiator strategy has been identified to not
	 * work on the current JVM at all or if the "spring.objenesis.ignore" property has
	 * been set to "true", this method returns {@code false}.
	 */
	// 返回此 Objenesis 实例是否值得尝试创建实例，即它是否尚未使用或已知可以工作。
	//
	//如果已确定配置的 Objenesis 实例化器策略在当前 JVM 上根本不起作用，或者“spring.objenesis.ignore”属性
	// 已设置为“true”，则此方法返回false
	public boolean isWorthTrying() {
		return (this.worthTrying != Boolean.FALSE);
	}

	/**
	 * Create a new instance of the given class via Objenesis.
	 * @param clazz the class to create an instance of
	 * @param useCache whether to use the instantiator cache
	 * (typically {@code true} but can be set to {@code false}
	 * e.g. for reloadable classes)
	 * @return the new instance (never {@code null})
	 * @throws ObjenesisException if instance creation failed
	 */
	// 通过 Objenesis 创建给定类的新实例。
	// 形参：
	//			clazz – 创建一个实例的类
	// 			useCache - 是否使用实例化缓存（通常为true但可以设置为false例如对于可重新加载的类）
	// 返回值：
	//			新实例（从不为null ）
	// 异常：
	//			ObjenesisException – 如果实例创建失败
	public <T> T newInstance(Class<T> clazz, boolean useCache) {
		if (!useCache) {
			return newInstantiatorOf(clazz).newInstance();
		}
		return getInstantiatorOf(clazz).newInstance();
	}

	public <T> T newInstance(Class<T> clazz) {
		return getInstantiatorOf(clazz).newInstance();
	}

	@SuppressWarnings("unchecked")
	public <T> ObjectInstantiator<T> getInstantiatorOf(Class<T> clazz) {
		ObjectInstantiator<?> instantiator = this.cache.get(clazz);
		if (instantiator == null) {
			ObjectInstantiator<T> newInstantiator = newInstantiatorOf(clazz);
			instantiator = this.cache.putIfAbsent(clazz, newInstantiator);
			if (instantiator == null) {
				instantiator = newInstantiator;
			}
		}
		return (ObjectInstantiator<T>) instantiator;
	}

	protected <T> ObjectInstantiator<T> newInstantiatorOf(Class<T> clazz) {
		Boolean currentWorthTrying = this.worthTrying;
		try {
			ObjectInstantiator<T> instantiator = this.strategy.newInstantiatorOf(clazz);
			if (currentWorthTrying == null) {
				this.worthTrying = Boolean.TRUE;
			}
			return instantiator;
		}
		catch (ObjenesisException ex) {
			if (currentWorthTrying == null) {
				Throwable cause = ex.getCause();
				if (cause instanceof ClassNotFoundException || cause instanceof IllegalAccessException) {
					// Indicates that the chosen instantiation strategy does not work on the given JVM.
					// Typically a failure to initialize the default SunReflectionFactoryInstantiator.
					// Let's assume that any subsequent attempts to use Objenesis will fail as well...
					// 表示所选的实例化策略在给定的 JVM 上不起作用。通常初始化默认 SunReflectionFactoryInstantiator 失败。
					// 让我们假设任何后续使用 Objenesis 的尝试都将失败......
					this.worthTrying = Boolean.FALSE;
				}
			}
			throw ex;
		}
		catch (NoClassDefFoundError err) {
			// Happening on the production version of Google App Engine, coming out of the
			// restricted "sun.reflect.ReflectionFactory" class...
			// 发生在 Google App Engine 的生产版本上，从受限制的“sun.reflect.ReflectionFactory”类中出来......
			if (currentWorthTrying == null) {
				this.worthTrying = Boolean.FALSE;
			}
			throw new ObjenesisException(err);
		}
	}

}
