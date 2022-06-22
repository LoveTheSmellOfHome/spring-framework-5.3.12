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

package org.springframework.transaction.interceptor;

import java.util.Collection;

import org.springframework.lang.Nullable;
import org.springframework.transaction.TransactionDefinition;

/**
 * This interface adds a {@code rollbackOn} specification to {@link TransactionDefinition}.
 * As custom {@code rollbackOn} is only possible with AOP, it resides in the AOP-related
 * transaction subpackage.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Mark Paluch
 * @since 16.03.2003
 * @see DefaultTransactionAttribute
 * @see RuleBasedTransactionAttribute
 */
// 该接口向 {@link TransactionDefinition} 添加了一个 {@code rollbackOn} 规范。
// 由于自定义 {@code rollbackOn} 仅适用于 AOP，因此它驻留在与 AOP 相关的事务子包中
public interface TransactionAttribute extends TransactionDefinition {

	/**
	 * Return a qualifier value associated with this transaction attribute.
	 * <p>This may be used for choosing a corresponding transaction manager
	 * to process this specific transaction.
	 * @since 3.0
	 */
	// 返回与此事务属性关联的限定符值。
	// 这可用于选择相应的事务管理器来处理此特定事务。
	// @Transactional 的 value 属性，TransactionManager bean 的名称
	@Nullable
	String getQualifier();

	/**
	 * Return labels associated with this transaction attribute.
	 * <p>This may be used for applying specific transactional behavior
	 * or follow a purely descriptive nature.
	 * @since 5.3
	 */
	// 返回与此交易属性关联的标签。
	// <p>这可用于应用特定的交易行为或遵循纯粹的描述性质。
	Collection<String> getLabels();

	/**
	 * Should we roll back on the given exception?
	 * @param ex the exception to evaluate
	 * @return whether to perform a rollback or not
	 */
	// 我们应该根据给定的异常回滚吗？
	// @param ex 要评估的异常
	// @return 是否执行回滚
	boolean rollbackOn(Throwable ex);

}
