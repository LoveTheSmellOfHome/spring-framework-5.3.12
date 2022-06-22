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

package org.springframework.expression;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.lang.Nullable;

/**
 * A type converter can convert values between different types encountered during
 * expression evaluation. This is an SPI for the expression parser; see
 * {@link org.springframework.core.convert.ConversionService} for the primary
 * user API to Spring's conversion facilities.
 *
 * @author Andy Clement
 * @author Juergen Hoeller
 * @since 3.0
 */
// 类型转换器可以在表达式计算期间遇到的不同类型之间转换值。 这是表达式解析器的 SPI；
// 有关 Spring 转换工具的主要 API，请参阅org.springframework.core.convert.ConversionService 。
public interface TypeConverter {

	/**
	 * Return {@code true} if the type converter can convert the specified type
	 * to the desired target type.
	 * @param sourceType a type descriptor that describes the source type
	 * @param targetType a type descriptor that describes the requested result type
	 * @return {@code true} if that conversion can be performed
	 */
	// 如果类型转换器可以将指定类型转换为所需的目标类型，则返回true
	boolean canConvert(@Nullable TypeDescriptor sourceType, TypeDescriptor targetType);

	/**
	 * Convert (or coerce) a value from one type to another, for example from a
	 * {@code boolean} to a {@code String}.
	 * <p>The {@link TypeDescriptor} parameters enable support for typed collections:
	 * A caller may prefer a {@code List&lt;Integer&gt;}, for example, rather than
	 * simply any {@code List}.
	 * @param value the value to be converted
	 * @param sourceType a type descriptor that supplies extra information about the
	 * source object
	 * @param targetType a type descriptor that supplies extra information about the
	 * requested result type
	 * @return the converted value
	 * @throws EvaluationException if conversion failed or is not possible to begin with
	 */
	// 将值从一种类型转换（或强制）到另一种类型，例如从 {@code boolean} 到 {@code String}。
	// <p>{@link TypeDescriptor} 参数支持类型化集合：例如，调用者可能更喜欢 {@code List<Integer>}，
	// 而不是简单的任何 {@code List}。
	// @param value 要转换的值
	// @param sourceType 提供有关源对象的额外信息的类型描述符
	// @param targetType 提供有关请求的结果类型的额外信息的类型描述符
	// @return 转换后的值
	@Nullable
	Object convertValue(@Nullable Object value, @Nullable TypeDescriptor sourceType, TypeDescriptor targetType);

}
