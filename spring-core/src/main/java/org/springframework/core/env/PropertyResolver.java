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

package org.springframework.core.env;

import org.springframework.lang.Nullable;

/**
 * Interface for resolving properties against any underlying source.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 3.1
 * @see Environment
 * @see PropertySourcesPropertyResolver
 */
// 用于针对任何基础源解析属性的接口,属性处理器，包含了属性的存储，属性的类型转化，属性的占位符处理
public interface PropertyResolver {

	/**
	 * Return whether the given property key is available for resolution,
	 * i.e. if the value for the given key is not {@code null}.
	 */
	// 返回给定的属性键是否可用于解析，即如果给定键的值不是 {@code null}。
	boolean containsProperty(String key);

	/**
	 * Return the property value associated with the given key,
	 * or {@code null} if the key cannot be resolved.
	 * @param key the property name to resolve
	 * @see #getProperty(String, String)
	 * @see #getProperty(String, Class)
	 * @see #getRequiredProperty(String)
	 */
	// 返回与给定键关联的属性值，如果无法解析键，则返回 {@code null}。 @param key 要解析的属性名称
	@Nullable
	String getProperty(String key);

	/**
	 * Return the property value associated with the given key, or
	 * {@code defaultValue} if the key cannot be resolved.
	 * @param key the property name to resolve
	 * @param defaultValue the default value to return if no value is found
	 * @see #getRequiredProperty(String)
	 * @see #getProperty(String, Class)
	 */
	// 返回与给定键关联的属性值，如果无法解析键，则返回 {@code defaultValue}。
	// @param key 要解析的属性名称
	// @param defaultValue 未找到值时返回的默认值
	String getProperty(String key, String defaultValue);

	/**
	 * Return the property value associated with the given key,
	 * or {@code null} if the key cannot be resolved.
	 * @param key the property name to resolve
	 * @param targetType the expected type of the property value
	 * @see #getRequiredProperty(String, Class)
	 */
	// 返回与给定键关联的属性值，如果无法解析键，则返回 {@code null}。
	// @param key 要解析的属性名称
	// @param targetType 属性值的预期类型
	@Nullable
	<T> T getProperty(String key, Class<T> targetType);

	/**
	 * Return the property value associated with the given key,
	 * or {@code defaultValue} if the key cannot be resolved.
	 * @param key the property name to resolve
	 * @param targetType the expected type of the property value
	 * @param defaultValue the default value to return if no value is found
	 * @see #getRequiredProperty(String, Class)
	 */
	// 返回与给定键关联的属性值，如果无法解析键，则返回 {@code defaultValue}。
	// @param key 要解析的属性名称
	// @param targetType 属性值的预期类型
	// @param defaultValue 未找到值时返回的默认值,兜底方案
	<T> T getProperty(String key, Class<T> targetType, T defaultValue);

	/**
	 * Return the property value associated with the given key (never {@code null}).
	 * @throws IllegalStateException if the key cannot be resolved
	 * @see #getRequiredProperty(String, Class)
	 */
	// 返回与给定键关联的属性值（从不{@code null}）。
	// 如果无法解析密钥，则@throws IllegalStateException
	String getRequiredProperty(String key) throws IllegalStateException;

	/**
	 * Return the property value associated with the given key, converted to the given
	 * targetType (never {@code null}).
	 * @throws IllegalStateException if the given key cannot be resolved
	 */
	// 返回与给定键关联的属性值，转换为给定的 targetType（永远不会 {@code null}）
	<T> T getRequiredProperty(String key, Class<T> targetType) throws IllegalStateException;

	/**
	 * Resolve ${...} placeholders in the given text, replacing them with corresponding
	 * property values as resolved by {@link #getProperty}. Unresolvable placeholders with
	 * no default value are ignored and passed through unchanged.
	 * @param text the String to resolve
	 * @return the resolved String (never {@code null})
	 * @throws IllegalArgumentException if given text is {@code null}
	 * @see #resolveRequiredPlaceholders
	 */
	// 解析给定文本中的 ${...} 占位符，将它们替换为由 {@link getProperty} 解析的相应属性值。
	// 没有默认值的无法解析的占位符将被忽略并保持不变
	// @param text 要解析的字符串
	// @return 解析后的字符串（从不{@code null}）
	String resolvePlaceholders(String text);

	/**
	 * Resolve ${...} placeholders in the given text, replacing them with corresponding
	 * property values as resolved by {@link #getProperty}. Unresolvable placeholders with
	 * no default value will cause an IllegalArgumentException to be thrown.
	 * @return the resolved String (never {@code null})
	 * @throws IllegalArgumentException if given text is {@code null}
	 * or if any placeholders are unresolvable
	 */
	// 解析给定文本中的 ${...} 占位符，将它们替换为由 {@link getProperty} 解析的相应属性值。
	// 没有默认值的无法解析的占位符将导致抛出 IllegalArgumentException。
	// @return 解析后的字符串（从不{@code null}）
	String resolveRequiredPlaceholders(String text) throws IllegalArgumentException;

}
