/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.core.convert.converter;

import org.springframework.core.convert.TypeDescriptor;

/**
 * Allows a {@link Converter}, {@link GenericConverter} or {@link ConverterFactory} to
 * conditionally execute based on attributes of the {@code source} and {@code target}
 * {@link TypeDescriptor}.
 *
 * <p>Often used to selectively match custom conversion logic based on the presence of a
 * field or class-level characteristic, such as an annotation or method. For example, when
 * converting from a String field to a Date field, an implementation might return
 * {@code true} if the target field has also been annotated with {@code @DateTimeFormat}.
 *
 * <p>As another example, when converting from a String field to an {@code Account} field,
 * an implementation might return {@code true} if the target Account class defines a
 * {@code public static findAccount(String)} method.
 *
 * @author Phillip Webb
 * @author Keith Donald
 * @since 3.2
 * @see Converter
 * @see GenericConverter
 * @see ConverterFactory
 * @see ConditionalGenericConverter
 */
// 允许 {@link Converter}、{@link GenericConverter} 或 {@link ConverterFactory}
// 根据 {@code source} 和 {@code target} {@link TypeDescriptor} 的属性有条件地执行。
//
// <p>通常用于根据字段或类级别特征（例如注解或方法）的存在有选择地匹配自定义转换逻辑。例如，当从字符串字段转换为日期字段时，
// 如果目标字段也已使用 {@code @DateTimeFormat} 进行注解，则实现可能会返回 {@code true}。
//
// <p>再举一个例子，当从 String 字段转换为 {@code Account} 字段时，如果目标 Account 类定义了
// {@code public static findAccount(String)} 方法，则实现可能会返回 {@code true}。
// 条件类型转换器
public interface ConditionalConverter {

	/**
	 * Should the conversion from {@code sourceType} to {@code targetType} currently under
	 * consideration be selected?
	 * @param sourceType the type descriptor of the field we are converting from
	 * @param targetType the type descriptor of the field we are converting to
	 * @return true if conversion should be performed, false otherwise
	 */
	// 是否应该选择当前正在考虑的从 {@code sourceType} 到 {@code targetType} 的转换？
	// @param sourceType 我们正在转换的字段的类型描述符
	// @param targetType 我们要转换到的字段的类型描述符
	// @return 如果应该执行转换为真，否则为假
	// 类型转换前预判动作：传入类型和输出类型是否能够匹配我当前的转换器。更宽泛的实现
	// {@link org.springframework.core.convert.converter.GenericConverter}
	// 非强类型转换：使用 TypeDescriptor 来描述类型转换
	boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType);

}
