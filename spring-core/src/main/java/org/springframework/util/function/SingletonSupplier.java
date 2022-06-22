/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.util.function;

import java.util.function.Supplier;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * A {@link java.util.function.Supplier} decorator that caches a singleton result and
 * makes it available from {@link #get()} (nullable) and {@link #obtain()} (null-safe).
 *
 * <p>A {@code SingletonSupplier} can be constructed via {@code of} factory methods
 * or via constructors that provide a default supplier as a fallback. This is
 * particularly useful for method reference suppliers, falling back to a default
 * supplier for a method that returned {@code null} and caching the result.
 *
 * @author Juergen Hoeller
 * @since 5.1
 * @param <T> the type of results supplied by this supplier
 */
// 一个Supplier 装饰器，它缓存一个单例结果并使其可从 get() （可空）和 obtain() （空安全）中使用。
// 
// SingletonSupplier 可以通过工厂方法或通过提供默认供应商作为后备of构造函数来构造。
// 这对于方法引用供应商特别有用，回退到返回null并缓存结果的方法的默认供应商。
public class SingletonSupplier<T> implements Supplier<T> {

	// 实例提供者
	@Nullable
	private final Supplier<? extends T> instanceSupplier;

	// 默认提供者
	@Nullable
	private final Supplier<? extends T> defaultSupplier;

	// 单例
	@Nullable
	private volatile T singletonInstance;


	/**
	 * Build a {@code SingletonSupplier} with the given singleton instance
	 * and a default supplier for the case when the instance is {@code null}.
	 * @param instance the singleton instance (potentially {@code null})
	 * @param defaultSupplier the default supplier as a fallback
	 */
	// 使用给定的单例实例和默认供应商构建一个 SingletonSupplier ，以用于实例为 null 的情况。
	// 参形：
	//			instance – 单例实例（可能为null ）
	//			defaultSupplier – 作为后备的默认供应商
	public SingletonSupplier(@Nullable T instance, Supplier<? extends T> defaultSupplier) {
		this.instanceSupplier = null;
		this.defaultSupplier = defaultSupplier;
		this.singletonInstance = instance;
	}

	/**
	 * Build a {@code SingletonSupplier} with the given instance supplier
	 * and a default supplier for the case when the instance is {@code null}.
	 * @param instanceSupplier the immediate instance supplier
	 * @param defaultSupplier the default supplier as a fallback
	 */
	// 使用给定的实例供应商和实例为 null 的情况的默认供应商构建一个 SingletonSupplier 。
	// 参形：
	//			instanceSupplier – 直接实例供应商
	//			defaultSupplier – 作为后备的默认供应商
	public SingletonSupplier(@Nullable Supplier<? extends T> instanceSupplier, Supplier<? extends T> defaultSupplier) {
		this.instanceSupplier = instanceSupplier;
		this.defaultSupplier = defaultSupplier;
	}

	private SingletonSupplier(Supplier<? extends T> supplier) {
		this.instanceSupplier = supplier;
		this.defaultSupplier = null;
	}

	private SingletonSupplier(T singletonInstance) {
		this.instanceSupplier = null;
		this.defaultSupplier = null;
		this.singletonInstance = singletonInstance;
	}


	/**
	 * Get the shared singleton instance for this supplier.
	 * @return the singleton instance (or {@code null} if none)
	 */
	// 获取此供应商的共享单例实例。
	// 返回值：单例实例（如果没有，则为null 
	@Override
	@Nullable
	public T get() {
		T instance = this.singletonInstance;
		if (instance == null) {
			synchronized (this) {
				instance = this.singletonInstance;
				if (instance == null) {
					if (this.instanceSupplier != null) {
						instance = this.instanceSupplier.get();
					}
					if (instance == null && this.defaultSupplier != null) {
						instance = this.defaultSupplier.get();
					}
					this.singletonInstance = instance;
				}
			}
		}
		return instance;
	}

	/**
	 * Obtain the shared singleton instance for this supplier.
	 * @return the singleton instance (never {@code null})
	 * @throws IllegalStateException in case of no instance
	 */
	// 获取此供应商的共享单例实例。
	// 返回值：
	//			单例实例（从不为null ）
	// 抛出：
	//			IllegalStateException – 如果没有实例
	public T obtain() {
		T instance = get();
		Assert.state(instance != null, "No instance from Supplier");
		return instance;
	}


	/**
	 * Build a {@code SingletonSupplier} with the given singleton instance.
	 * @param instance the singleton instance (never {@code null})
	 * @return the singleton supplier (never {@code null})
	 */
	// 使用给定的单例实例构建SingletonSupplier 。
	// 参形：
	//			instance – 单例实例（从不为null ）
	// 返回值：
	//			单例供应商（从不为null 
	public static <T> SingletonSupplier<T> of(T instance) {
		return new SingletonSupplier<>(instance);
	}

	/**
	 * Build a {@code SingletonSupplier} with the given singleton instance.
	 * @param instance the singleton instance (potentially {@code null})
	 * @return the singleton supplier, or {@code null} if the instance was {@code null}
	 */
	// 使用给定的单例实例构建 SingletonSupplier 。
	// 参形：
	//			instance – 单例实例（可能为null ）
	// 返回值：
	//			单例供应商，如果实例为null ，则为null
	@Nullable
	public static <T> SingletonSupplier<T> ofNullable(@Nullable T instance) {
		return (instance != null ? new SingletonSupplier<>(instance) : null);
	}

	/**
	 * Build a {@code SingletonSupplier} with the given supplier.
	 * @param supplier the instance supplier (never {@code null})
	 * @return the singleton supplier (never {@code null})
	 */
	// 使用给定的供应商构建一个SingletonSupplier 。
	// 参形：
	//			供应商——实例供应商（从不为null ）
	// 返回值：
	//			单例供应商（从不为null ）
	public static <T> SingletonSupplier<T> of(Supplier<T> supplier) {
		return new SingletonSupplier<>(supplier);
	}

	/**
	 * Build a {@code SingletonSupplier} with the given supplier.
	 * @param supplier the instance supplier (potentially {@code null})
	 * @return the singleton supplier, or {@code null} if the instance supplier was {@code null}
	 */
	// 使用给定的供应商构建一个SingletonSupplier 。
	// 参形：
	//			supplier - 实例供应商（可能为null ）
	// 返回值：
	//			单例供应商，如果实例供应商为null ，则为null
	@Nullable
	public static <T> SingletonSupplier<T> ofNullable(@Nullable Supplier<T> supplier) {
		return (supplier != null ? new SingletonSupplier<>(supplier) : null);
	}

}
