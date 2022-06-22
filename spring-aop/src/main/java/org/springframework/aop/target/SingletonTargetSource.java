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

package org.springframework.aop.target;

import org.springframework.aop.TargetSource;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

import java.io.Serializable;

/**
 * Implementation of the {@link org.springframework.aop.TargetSource} interface
 * that holds a given object. This is the default implementation of the TargetSource
 * interface, as used by the Spring AOP framework. There is usually no need to
 * create objects of this class in application code.
 *
 * <p>This class is serializable. However, the actual serializability of a
 * SingletonTargetSource will depend on whether the target is serializable.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see org.springframework.aop.framework.AdvisedSupport#setTarget(Object)
 */
// 包含给定对象的 TargetSource 接口的实现。这是 Spring AOP 框架使用的 TargetSource 接口的默认实现。
// 通常不需要在应用程序代码中创建此类的对象。
//
// 这个类是可序列化的。但是，SingletonTargetSource 的实际可序列化性将取决于目标是否可序列化。
public class SingletonTargetSource implements TargetSource, Serializable {

	/** use serialVersionUID from Spring 1.2 for interoperability. */
	private static final long serialVersionUID = 9031246629662423738L;


	/** Target cached and invoked using reflection. */
	// 使用反射缓存和调用目标。final 只允许被初始化一次
	private final Object target;


	/**
	 * Create a new SingletonTargetSource for the given target.
	 * @param target the target object
	 */
	// 为给定的目标创建一个新的 SingletonTargetSource。
	// 参形：target – 目标对象,一般来说是 IoC 容器中的单例 bean.
	// 所以这里还是通过 IoC 来实现基于容器级别的单例
	public SingletonTargetSource(Object target) {
		Assert.notNull(target, "Target object must not be null");
		this.target = target;
	}


	@Override
	public Class<?> getTargetClass() {
		return this.target.getClass();
	}

	@Override
	public Object getTarget() {
		return this.target;
	}

	// 单例对象不需要释放目标对象至池内
	@Override
	public void releaseTarget(Object target) {
		// nothing to do
	}

	// 单例对象返回 true
	@Override
	public boolean isStatic() {
		return true;
	}


	/**
	 * Two invoker interceptors are equal if they have the same target or if the
	 * targets or the targets are equal.
	 */
	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof SingletonTargetSource)) {
			return false;
		}
		SingletonTargetSource otherTargetSource = (SingletonTargetSource) other;
		return this.target.equals(otherTargetSource.target);
	}

	/**
	 * SingletonTargetSource uses the hash code of the target object.
	 */
	@Override
	public int hashCode() {
		return this.target.hashCode();
	}

	@Override
	public String toString() {
		return "SingletonTargetSource for target object [" + ObjectUtils.identityToString(this.target) + "]";
	}

}
