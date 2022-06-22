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

package org.springframework.core.convert.converter;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * A converter converts a source object of type {@code S} to a target of type {@code T}.
 *
 * <p>Implementations of this interface are thread-safe and can be shared.
 *
 * <p>Implementations may additionally implement {@link ConditionalConverter}.
 *
 * @author Keith Donald
 * @author Josh Cummings
 * @since 3.0
 * @param <S> the source type
 * @param <T> the target type
 */
// 类型转换器将 {@code S} 类型的源对象转换为 {@code T} 类型的目标对象,一般用于单一类型转换
// <p>这个接口的实现是线程安全的并且可以共享，需要确保泛型类型是 计算类型(没有属性和局部变量来保存中间状态，不会被多线程修改)
// <p>实现可能会另外实现 {@link ConditionalConverter}
// 缺点：由于泛型擦除的原因，
@FunctionalInterface
public interface Converter<S, T> {

	/**
	 * Convert the source object of type {@code S} to target type {@code T}.
	 * @param source the source object to convert, which must be an instance of {@code S} (never {@code null})
	 * @return the converted object, which must be an instance of {@code T} (potentially {@code null})
	 * @throws IllegalArgumentException if the source cannot be converted to the desired target type
	 */
	// 将 {@code S} 类型的源对象转换为目标类型 {@code T}
	// @param source 要转换的源对象，它必须是 {@code S} 的实例（永远不会 {@code null}）
	// @return 转换后的对象，它必须是 {@code T} 的实例（可能是 {@code null}）
	// 如果源无法转换为所需的目标类型，则@throws IllegalArgumentException
	// 强类型转换，S 和 T 都是确定类型
	@Nullable
	T convert(S source);

	/**
	 * Construct a composed {@link Converter} that first applies this {@link Converter}
	 * to its input, and then applies the {@code after} {@link Converter} to the
	 * result.
	 * @param after the {@link Converter} to apply after this {@link Converter}
	 * is applied
	 * @param <U> the type of output of both the {@code after} {@link Converter}
	 * and the composed {@link Converter}
	 * @return a composed {@link Converter} that first applies this {@link Converter}
	 * and then applies the {@code after} {@link Converter}
	 * @since 5.3
	 */
	// 构造一个组合的 {@link Converter}，首先将此 {@link Converter} 应用于其输入，
	// 然后将 {@code after} {@link Converter} 应用于结果。
	//
	default <U> Converter<S, U> andThen(Converter<? super T, ? extends U> after) {
		Assert.notNull(after, "After Converter must not be null");
		return (S s) -> {
			T initialResult = convert(s);
			return (initialResult != null ? after.convert(initialResult) : null);
		};
	}

}
