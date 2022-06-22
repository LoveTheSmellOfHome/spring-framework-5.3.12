/*
 * Copyright 2002-2016 the original author or authors.
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

/**
 * Implementers of this interface are expected to be able to locate types.
 * They may use a custom {@link ClassLoader} and/or deal with common
 * package prefixes (e.g. {@code java.lang}) however they wish.
 *
 * <p>See {@link org.springframework.expression.spel.support.StandardTypeLocator}
 * for an example implementation.
 *
 * @author Andy Clement
 * @since 3.0
 */
// 这个接口的实现者应该能够定位类型。
// 然而他们希望使用自定义的 {@link ClassLoader} 来处理包前缀如(e.g. {@code java.lang})
@FunctionalInterface
public interface TypeLocator {

	/**
	 * Find a type by name. The name may or may not be fully qualified
	 * (e.g. {@code String} or {@code java.lang.String}).
	 * @param typeName the type to be located
	 * @return the {@code Class} object representing that type
	 * @throws EvaluationException if there is a problem finding the type
	 */
	// 按名称查找类型。该名称可能是也可能不是完全限定的（例如 {@code String} 或 {@code java.lang.String}）。
	// @param typeName 要定位的类型
	// @return 表示该类型的 {@code Class} 对象
	// @throws EvaluationException 如果在查找类型时出现问题
	Class<?> findType(String typeName) throws EvaluationException;

}
