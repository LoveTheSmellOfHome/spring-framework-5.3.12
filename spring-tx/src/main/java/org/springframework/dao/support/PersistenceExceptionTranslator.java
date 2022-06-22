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

package org.springframework.dao.support;

import org.springframework.dao.DataAccessException;
import org.springframework.lang.Nullable;

/**
 * Interface implemented by Spring integrations with data access technologies
 * that throw runtime exceptions, such as JPA and Hibernate.
 *
 * <p>This allows consistent usage of combined exception translation functionality,
 * without forcing a single translator to understand every single possible type
 * of exception.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 2.0
 */
// Spring 实现的接口与引发运行时异常的数据访问技术集成，例如 JPA 和 Hibernate。
//
// 这允许一致地使用组合的异常翻译功能，而无需强制单个翻译器理解每种可能的异常类型
@FunctionalInterface
public interface PersistenceExceptionTranslator {

	/**
	 * Translate the given runtime exception thrown by a persistence framework to a
	 * corresponding exception from Spring's generic
	 * {@link org.springframework.dao.DataAccessException} hierarchy, if possible.
	 * <p>Do not translate exceptions that are not understood by this translator:
	 * for example, if coming from another persistence framework, or resulting
	 * from user code or otherwise unrelated to persistence.
	 * <p>Of particular importance is the correct translation to
	 * DataIntegrityViolationException, for example on constraint violation.
	 * Implementations may use Spring JDBC's sophisticated exception translation
	 * to provide further information in the event of SQLException as a root cause.
	 * @param ex a RuntimeException to translate
	 * @return the corresponding DataAccessException (or {@code null} if the
	 * exception could not be translated, as in this case it may result from
	 * user code rather than from an actual persistence problem)
	 * @see org.springframework.dao.DataIntegrityViolationException
	 * @see org.springframework.jdbc.support.SQLExceptionTranslator
	 */
	// 如果可能，将持久性框架抛出的给定运行时异常转换为 Spring 的通用DataAccessException层次结构中的相应异常。
	//
	// 不要翻译此翻译器无法理解的异常：例如，如果来自另一个持久性框架，或者来自用户代码或其他与持久性无关的异常。
	//
	// 特别重要的是正确转换为 DataIntegrityViolationException，例如在违反约束时。实现可以使用 Spring JDBC 复杂的
	// 异常转换来在 SQLException 作为根本原因的情况下提供更多信息。
	// 参形：
	//			ex – 要翻译的 RuntimeException
	// 返回值：
	//			相应的 DataAccessException （如果无法翻译异常，则为null ，因为在这种情况下，它可能是由用户
	//			代码引起的，而不是由实际的持久性问题引起的）
	@Nullable
	DataAccessException translateExceptionIfPossible(RuntimeException ex);

}
