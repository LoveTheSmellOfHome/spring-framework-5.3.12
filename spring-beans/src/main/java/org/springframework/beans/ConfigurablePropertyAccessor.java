/*
 * Copyright 2002-2017 the original author or authors.
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

import org.springframework.core.convert.ConversionService;
import org.springframework.lang.Nullable;

/**
 * Interface that encapsulates configuration methods for a PropertyAccessor.
 * Also extends the PropertyEditorRegistry interface, which defines methods
 * for PropertyEditor management.
 *
 * <p>Serves as base interface for {@link BeanWrapper}.
 *
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @since 2.0
 * @see BeanWrapper
 */
// 封装 PropertyAccessor 的配置方法的接口。还扩展了 PropertyEditorRegistry 接口，该接口定义了 PropertyEditor 管理的方法。
// <p>用作 {@link BeanWrapper} 的基本接口
public interface ConfigurablePropertyAccessor extends PropertyAccessor, PropertyEditorRegistry, TypeConverter {

	/**
	 * Specify a Spring 3.0 ConversionService to use for converting
	 * property values, as an alternative to JavaBeans PropertyEditors.
	 */
	// 指定用于转换属性值的 Spring 3.0 ConversionService，作为 JavaBeans PropertyEditors 的替代
	void setConversionService(@Nullable ConversionService conversionService);

	/**
	 * Return the associated ConversionService, if any.
	 */
	@Nullable
	ConversionService getConversionService();

	/**
	 * Set whether to extract the old property value when applying a
	 * property editor to a new value for a property.
	 */
	// 设置在将属性编辑器应用于属性的新值时是否提取旧属性值，是不是利用 PropertyEditor 来抽取老的值
	void setExtractOldValueForEditor(boolean extractOldValueForEditor);

	/**
	 * Return whether to extract the old property value when applying a
	 * property editor to a new value for a property.
	 */
	// 返回在将属性编辑器应用于属性的新值时是否提取旧属性值。
	boolean isExtractOldValueForEditor();

	/**
	 * Set whether this instance should attempt to "auto-grow" a
	 * nested path that contains a {@code null} value.
	 * <p>If {@code true}, a {@code null} path location will be populated
	 * with a default object value and traversed instead of resulting in a
	 * {@link NullValueInNestedPathException}.
	 * <p>Default is {@code false} on a plain PropertyAccessor instance.
	 */
	// 设置此实例是否应尝试“自动增长”包含 {@code null} 值的嵌套路径。
	// <p>如果 {@code true}，{@code null} 路径位置将填充默认对象值并遍历，而不是导致 {@link NullValueInNestedPathException}。
	// <p>在普通的 PropertyAccessor 实例上默认是 {@code false}。
	void setAutoGrowNestedPaths(boolean autoGrowNestedPaths);

	/**
	 * Return whether "auto-growing" of nested paths has been activated.
	 */
	// 返回是否已激活嵌套路径的“自动增长”
	boolean isAutoGrowNestedPaths();

}
