/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.aop.aspectj;

import org.springframework.core.Ordered;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.io.Serializable;

/**
 * Implementation of {@link AspectInstanceFactory} that is backed by a
 * specified singleton object, returning the same instance for every
 * {@link #getAspectInstance()} call.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 2.0
 * @see SimpleAspectInstanceFactory
 */
// 由指定的单例对象支持的 AspectInstanceFactory 的实现，为每个 getAspectInstance() 调用返回相同的实例。
@SuppressWarnings("serial")
public class SingletonAspectInstanceFactory implements AspectInstanceFactory, Serializable {

	// 指定的单例对象实例
	private final Object aspectInstance;


	/**
	 * Create a new SingletonAspectInstanceFactory for the given aspect instance.
	 * @param aspectInstance the singleton aspect instance
	 */
	// 为给定的切面实例创建一个新的 SingletonAspectInstanceFactory。
	// 参形：aspectInstance – 单例方面实例，工厂保存的对象
	public SingletonAspectInstanceFactory(Object aspectInstance) {
		Assert.notNull(aspectInstance, "Aspect instance must not be null");
		this.aspectInstance = aspectInstance;
	}


	@Override
	public final Object getAspectInstance() {
		return this.aspectInstance;
	}

	@Override
	@Nullable
	public ClassLoader getAspectClassLoader() {
		return this.aspectInstance.getClass().getClassLoader();
	}

	/**
	 * Determine the order for this factory's aspect instance,
	 * either an instance-specific order expressed through implementing
	 * the {@link org.springframework.core.Ordered} interface,
	 * or a fallback order.
	 * @see org.springframework.core.Ordered
	 * @see #getOrderForAspectClass
	 */
	// 确定该工厂的切面实例的顺序，可以是通过实现 Ordered 接口表达的特定于实例的顺序，也可以是回退顺序
	@Override
	public int getOrder() {
		if (this.aspectInstance instanceof Ordered) {
			return ((Ordered) this.aspectInstance).getOrder();
		}
		return getOrderForAspectClass(this.aspectInstance.getClass());
	}

	/**
	 * Determine a fallback order for the case that the aspect instance
	 * does not express an instance-specific order through implementing
	 * the {@link org.springframework.core.Ordered} interface.
	 * <p>The default implementation simply returns {@code Ordered.LOWEST_PRECEDENCE}.
	 * @param aspectClass the aspect class
	 */
	// 通过实现 Ordered 接口，确定切面实例不表达特定于实例的顺序的情况下的回退顺序。
	// 默认实现只返回Ordered.LOWEST_PRECEDENCE 。
	// 参形：
	//			aspectClass – 切面类
	protected int getOrderForAspectClass(Class<?> aspectClass) {
		return Ordered.LOWEST_PRECEDENCE;
	}

}
