/*
 * Copyright 2002-2019 the original author or authors.
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

import java.lang.reflect.Field;

import org.springframework.core.MethodParameter;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.lang.Nullable;

/**
 * Interface that defines type conversion methods. Typically (but not necessarily)
 * implemented in conjunction with the {@link PropertyEditorRegistry} interface.
 *
 * <p><b>Note:</b> Since TypeConverter implementations are typically based on
 * {@link java.beans.PropertyEditor PropertyEditors} which aren't thread-safe,
 * TypeConverters themselves are <em>not</em> to be considered as thread-safe either.
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see SimpleTypeConverter
 * @see BeanWrapperImpl
 */
// 定义类型转换方法的接口。通常（但不一定）与 {@link PropertyEditorRegistry} 接口一起实现。
// <p><b>注意：<b>由于 TypeConverter 实现通常基于不是线程安全的 {@link java.beans.PropertyEditor PropertyEditors}，
// 因此 TypeConverter 本身<em>不<em>被视为线程安全的。
// 抽象实现：{org.springframework.beans.TypeConverterSupport}
// 简单实现：{org.springframework.beans.SimpleTypeConverter} 简单工具类
public interface TypeConverter {

	/**
	 * Convert the value to the required type (if necessary from a String).
	 * <p>Conversions from String to any type will typically use the {@code setAsText}
	 * method of the PropertyEditor class, or a Spring Converter in a ConversionService.
	 * @param value the value to convert
	 * @param requiredType the type we must convert to
	 * (or {@code null} if not known, for example in case of a collection element)
	 * @return the new value, possibly the result of type conversion
	 * @throws TypeMismatchException if type conversion failed
	 * @see java.beans.PropertyEditor#setAsText(String)
	 * @see java.beans.PropertyEditor#getValue()
	 * @see org.springframework.core.convert.ConversionService
	 * @see org.springframework.core.convert.converter.Converter
	 */
	// 将值转换为所需的类型（从字符串 -> 目标类型）。
	// <p>从 String 到任何类型的转换通常会使用 PropertyEditor 类的 {@code setAsText} 方法，
	// 或 ConversionService 中的 Spring Converter。
	// @param value 要转换的值
	// @param requiredType 我们必须转换成的目标类型（或者 {@code null} 如果未知，例如在集合元素的情况下）
	// 核心方法 convertIfNecessary 及重载方法(如果能够转换就转换，不能转换则属性保持原样)，
	// 是 ConversionService#canConvert 和 ConversionService#convert 语义的结合
	@Nullable
	<T> T convertIfNecessary(@Nullable Object value, @Nullable Class<T> requiredType) throws TypeMismatchException;

	/**
	 * Convert the value to the required type (if necessary from a String).
	 * <p>Conversions from String to any type will typically use the {@code setAsText}
	 * method of the PropertyEditor class, or a Spring Converter in a ConversionService.
	 * @param value the value to convert
	 * @param requiredType the type we must convert to
	 * (or {@code null} if not known, for example in case of a collection element)
	 * @param methodParam the method parameter that is the target of the conversion
	 * (for analysis of generic types; may be {@code null})
	 * @return the new value, possibly the result of type conversion
	 * @throws TypeMismatchException if type conversion failed
	 * @see java.beans.PropertyEditor#setAsText(String)
	 * @see java.beans.PropertyEditor#getValue()
	 * @see org.springframework.core.convert.ConversionService
	 * @see org.springframework.core.convert.converter.Converter
	 */
	// 将值转换为所需的类型（从字符串 -> 目标类型）。
	// <p>从 String 到任何类型的转换通常会使用 PropertyEditor 类的 {@code setAsText} 方法，
	// 或 ConversionService 中的 Spring Converter。
	// @param value 要转换的值
	// @param requiredType 我们必须转换成的目标类型（或者 {@code null} 如果未知，例如在集合元素的情况下）
	// @param methodParam 作为转换目标的方法参数（用于泛型分析；可能是{@code null}）
	// @return 新值，可能是类型转换的结果
	@Nullable
	<T> T convertIfNecessary(@Nullable Object value, @Nullable Class<T> requiredType,
			@Nullable MethodParameter methodParam) throws TypeMismatchException;

	/**
	 * Convert the value to the required type (if necessary from a String).
	 * <p>Conversions from String to any type will typically use the {@code setAsText}
	 * method of the PropertyEditor class, or a Spring Converter in a ConversionService.
	 * @param value the value to convert
	 * @param requiredType the type we must convert to
	 * (or {@code null} if not known, for example in case of a collection element)
	 * @param field the reflective field that is the target of the conversion
	 * (for analysis of generic types; may be {@code null})
	 * @return the new value, possibly the result of type conversion
	 * @throws TypeMismatchException if type conversion failed
	 * @see java.beans.PropertyEditor#setAsText(String)
	 * @see java.beans.PropertyEditor#getValue()
	 * @see org.springframework.core.convert.ConversionService
	 * @see org.springframework.core.convert.converter.Converter
	 */
	// @param field 作为转换目标的反射字段（用于泛型类型的分析；可能是 {@code null}）
	@Nullable
	<T> T convertIfNecessary(@Nullable Object value, @Nullable Class<T> requiredType, @Nullable Field field)
			throws TypeMismatchException;

	/**
	 * Convert the value to the required type (if necessary from a String).
	 * <p>Conversions from String to any type will typically use the {@code setAsText}
	 * method of the PropertyEditor class, or a Spring Converter in a ConversionService.
	 * @param value the value to convert
	 * @param requiredType the type we must convert to
	 * (or {@code null} if not known, for example in case of a collection element)
	 * @param typeDescriptor the type descriptor to use (may be {@code null}))
	 * @return the new value, possibly the result of type conversion
	 * @throws TypeMismatchException if type conversion failed
	 * @since 5.1.4
	 * @see java.beans.PropertyEditor#setAsText(String)
	 * @see java.beans.PropertyEditor#getValue()
	 * @see org.springframework.core.convert.ConversionService
	 * @see org.springframework.core.convert.converter.Converter
	 */
	// 将值转换为所需的类型（如有必要，从字符串）。
	// <p>从 String 到任何类型的转换通常会使用 PropertyEditor 类的 {@code setAsText} 方法，
	// 或 ConversionService 中的 Spring Converter。
	// @param typeDescriptor 要使用的类型描述符（可能是 {@code null}）
	// @param requiredType 我们必须转换为的目标类型（或 {@code null} 如果未知，例如在集合元素的情况下）
	// @param typeDescriptor 目标类型的上下文描述（可能是 {@code null}））
	// @return 新值，可能是类型转换的结果
	// @throws TypeMismatchException 如果类型转换失败
	@Nullable
	default <T> T convertIfNecessary(@Nullable Object value, @Nullable Class<T> requiredType,
			@Nullable TypeDescriptor typeDescriptor) throws TypeMismatchException {

		throw new UnsupportedOperationException("TypeDescriptor resolution not supported");
	}

}
