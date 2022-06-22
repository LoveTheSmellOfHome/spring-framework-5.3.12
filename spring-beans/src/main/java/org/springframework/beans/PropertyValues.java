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

package org.springframework.beans;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.springframework.lang.Nullable;

/**
 * Holder containing one or more {@link PropertyValue} objects,
 * typically comprising one update for a specific target bean.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 13 May 2001
 * @see PropertyValue
 */
// 包含一个或多个 {@link PropertyValue} 对象的持有者，通常包含一个特定目标 bean 的更新
// 一个 PropertyValues 包含了多个PropertyValue
public interface PropertyValues extends Iterable<PropertyValue> {

	/**
	 * Return an {@link Iterator} over the property values.
	 * @since 5.1
	 */
	// 在属性值上返回一个 {@link Iterator}。
	@Override
	default Iterator<PropertyValue> iterator() {
		return Arrays.asList(getPropertyValues()).iterator();
	}

	/**
	 * Return a {@link Spliterator} over the property values.
	 * @since 5.1
	 */
	// 在属性值上返回一个 {@link Spliterator}
	@Override
	default Spliterator<PropertyValue> spliterator() {
		return Spliterators.spliterator(getPropertyValues(), 0);
	}

	/**
	 * Return a sequential {@link Stream} containing the property values.
	 * @since 5.1
	 */
	// 返回包含属性值的顺序 {@link Stream}
	default Stream<PropertyValue> stream() {
		return StreamSupport.stream(spliterator(), false);
	}

	/**
	 * Return an array of the PropertyValue objects held in this object.
	 */
	// 返回此对象中保存的 PropertyValue 对象的数组
	PropertyValue[] getPropertyValues();

	/**
	 * Return the property value with the given name, if any.
	 * @param propertyName the name to search for
	 * @return the property value, or {@code null} if none
	 */
	// 返回具有给定名称的属性值（如果有）。
	// @param propertyName 要搜索的名称 @return 属性值，如果没有，则为 {@code null}
	@Nullable
	PropertyValue getPropertyValue(String propertyName);

	/**
	 * Return the changes since the previous PropertyValues.
	 * Subclasses should also override {@code equals}.
	 * @param old the old property values
	 * @return the updated or new properties.
	 * Return empty PropertyValues if there are no changes.
	 * @see Object#equals
	 */
	// 返回自上一个 PropertyValues 以来的更改。子类也应该覆盖 {@code equals}。
	// @param old 旧的属性值
	// @return 更新的或新的属性
	// 如果没有更改，则返回空的 PropertyValues
	PropertyValues changesSince(PropertyValues old);

	/**
	 * Is there a property value (or other processing entry) for this property?
	 * @param propertyName the name of the property we're interested in
	 * @return whether there is a property value for this property
	 */
	// 此属性是否有属性值（或其他处理条目）
	// @param propertyName 我们感兴趣的属性的名称
	// @return 这个属性是否有属性值
	boolean contains(String propertyName);

	/**
	 * Does this holder not contain any PropertyValue objects at all?
	 */
	// 这个持有人根本不包含任何 PropertyValue 对象吗？
	boolean isEmpty();

}
