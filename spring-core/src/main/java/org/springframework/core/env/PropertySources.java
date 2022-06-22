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

package org.springframework.core.env;

import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.springframework.lang.Nullable;

/**
 * Holder containing one or more {@link PropertySource} objects.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 3.1
 * @see PropertySource
 */
// 包含一个或多个 {@link PropertySource} 对象的持有者,在实现上类似于 {@link PropertyValues}
// PropertySources 有多个元素时，遍历直到找到第一个非 null 对象，立即返回。一般与环境中属性的优先级相关
// {@link PropertySourcesPlaceholderConfigurer}
public interface PropertySources extends Iterable<PropertySource<?>> {

	/**
	 * Return a sequential {@link Stream} containing the property sources.
	 * @since 5.1
	 */
	// 返回包含属性源的顺序 {@link Stream}。
	default Stream<PropertySource<?>> stream() {
		return StreamSupport.stream(spliterator(), false);
	}

	/**
	 * Return whether a property source with the given name is contained.
	 * @param name the {@linkplain PropertySource#getName() name of the property source} to find
	 */
	// 返回是否包含具有给定名称的属性源。
	// @param name 要查找的 {@linkplain PropertySource#getName() 属性源名称}
	boolean contains(String name);

	/**
	 * Return the property source with the given name, {@code null} if not found.
	 * @param name the {@linkplain PropertySource#getName() name of the property source} to find
	 */
	// 返回具有给定名称的唯一属性源，如果未找到，则返回 {@code null}。采用层次性查找找到唯一对应的 PropertySource
	// @param name 要查找的 {@linkplain PropertySource#getName() 属性源名称}
	@Nullable
	PropertySource<?> get(String name);

}
