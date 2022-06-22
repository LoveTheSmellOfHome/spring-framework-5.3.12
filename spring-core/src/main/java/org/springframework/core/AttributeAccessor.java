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

package org.springframework.core;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.util.function.Function;

/**
 * Interface defining a generic contract for attaching and accessing metadata
 * to/from arbitrary objects.
 *
 * @author Rob Harrop
 * @author Sam Brannen
 * @since 2.0
 */
// 定义用于从任意对象附加和访问元数据的通用合同的接口
//
// 属性上下文存储器,帮助我们存储中间一些状态，比如相同 BeanDefinition 注册来源不同,比如 User 一个是来自 XML
// 一个来自 Annotation，不同来源的 source 可能会携带些附加属性，AttributeAccessor 是一些附加属性的操作，
// 对 bean 没有任何帮助
//
// 属性存取器
public interface AttributeAccessor {

	/**
	 * Set the attribute defined by {@code name} to the supplied {@code value}.
	 * <p>If {@code value} is {@code null}, the attribute is {@link #removeAttribute removed}.
	 * <p>In general, users should take care to prevent overlaps with other
	 * metadata attributes by using fully-qualified names, perhaps using
	 * class or package names as prefix.
	 * @param name the unique attribute key
	 * @param value the attribute value to be attached
	 */
	// 将 {@code name} 定义的属性设置为提供的 {@code value}。
	// <p>如果 {@code value} 为 {@code null}，则属性为 {@link removeAttribute removed}。
	// <p>一般来说，用户应该注意防止与其他元数据属性重叠，方法是使用完全限定的名称，可能使用类或包名称作为前缀。
	// @param name 唯一的属性键
	// @param value 要附加的属性值
	void setAttribute(String name, @Nullable Object value);

	/**
	 * Get the value of the attribute identified by {@code name}.
	 * <p>Return {@code null} if the attribute doesn't exist.
	 * @param name the unique attribute key
	 * @return the current value of the attribute, if any
	 */
	// 获取由 {@code name} 标识的属性的值。
	// <p>如果属性不存在，则返回 {@code null}。
	// @param name 唯一的属性键
	// @return 属性的当前值，如果有的话
	@Nullable
	Object getAttribute(String name);

	/**
	 * Compute a new value for the attribute identified by {@code name} if
	 * necessary and {@linkplain #setAttribute set} the new value in this
	 * {@code AttributeAccessor}.
	 * <p>If a value for the attribute identified by {@code name} already exists
	 * in this {@code AttributeAccessor}, the existing value will be returned
	 * without applying the supplied compute function.
	 * <p>The default implementation of this method is not thread safe but can
	 * overridden by concrete implementations of this interface.
	 * @param <T> the type of the attribute value
	 * @param name the unique attribute key
	 * @param computeFunction a function that computes a new value for the attribute
	 * name; the function must not return a {@code null} value
	 * @return the existing value or newly computed value for the named attribute
	 * @since 5.3.3
	 * @see #getAttribute(String)
	 * @see #setAttribute(String, Object)
	 */
	// 如有必要，计算由 {@code name} 标识的属性的新值，并在此 {@code AttributeAccessor}
	// 中 {@linkplain setAttribute set} 新值。
	// <p>如果此 {@code AttributeAccessor} 中已存在由 {@code name} 标识的属性的值，
	// 则将在不应用提供的计算函数的情况下返回现有值。
	// <p>这个方法的默认实现不是线程安全的，但是可以被这个接口的具体实现覆盖。
	@SuppressWarnings("unchecked")
	default <T> T computeAttribute(String name, Function<String, T> computeFunction) {
		Assert.notNull(name, "Name must not be null");
		Assert.notNull(computeFunction, "Compute function must not be null");
		Object value = getAttribute(name);
		if (value == null) {
			value = computeFunction.apply(name);
			Assert.state(value != null,
					() -> String.format("Compute function must not return null for attribute named '%s'", name));
			setAttribute(name, value);
		}
		return (T) value;
	}

	/**
	 * Remove the attribute identified by {@code name} and return its value.
	 * <p>Return {@code null} if no attribute under {@code name} is found.
	 * @param name the unique attribute key
	 * @return the last value of the attribute, if any
	 */
	// 删除由 {@code name} 标识的属性并返回其值。
	// <p>如果未找到 {@code name} 下的属性，则返回 {@code null}。
	@Nullable
	Object removeAttribute(String name);

	/**
	 * Return {@code true} if the attribute identified by {@code name} exists.
	 * <p>Otherwise return {@code false}.
	 * @param name the unique attribute key
	 */
	// 如果由 {@code name} 标识的属性存在，则返回 {@code true}。 <p>否则返回 {@code false}。
	boolean hasAttribute(String name);

	/**
	 * Return the names of all attributes.
	 */
	// 返回所有属性的名称
	String[] attributeNames();

}
