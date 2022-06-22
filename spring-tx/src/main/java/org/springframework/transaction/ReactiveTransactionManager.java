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

package org.springframework.transaction;

import reactor.core.publisher.Mono;

import org.springframework.lang.Nullable;

/**
 * This is the central interface in Spring's reactive transaction infrastructure.
 * Applications can use this directly, but it is not primarily meant as an API:
 * Typically, applications will work with either transactional operators or
 * declarative transaction demarcation through AOP.
 *
 * @author Mark Paluch
 * @author Juergen Hoeller
 * @since 5.2
 * @see org.springframework.transaction.reactive.TransactionalOperator
 * @see org.springframework.transaction.interceptor.TransactionInterceptor
 * @see org.springframework.transaction.PlatformTransactionManager
 */
// 这是 Spring 的反应式事务基础设施中的中心接口。应用程序可以直接使用它，但它并不主要用作 API：
// 通常，应用程序将使用事务操作符或通过 AOP 的声明性事务划分
public interface ReactiveTransactionManager extends TransactionManager {

	/**
	 * Emit a currently active reactive transaction or create a new one, according to
	 * the specified propagation behavior.
	 * <p>Note that parameters like isolation level or timeout will only be applied
	 * to new transactions, and thus be ignored when participating in active ones.
	 * <p>Furthermore, not all transaction definition settings will be supported
	 * by every transaction manager: A proper transaction manager implementation
	 * should throw an exception when unsupported settings are encountered.
	 * <p>An exception to the above rule is the read-only flag, which should be
	 * ignored if no explicit read-only mode is supported. Essentially, the
	 * read-only flag is just a hint for potential optimization.
	 * @param definition the TransactionDefinition instance,
	 * describing propagation behavior, isolation level, timeout etc.
	 * @return transaction status object representing the new or current transaction
	 * @throws TransactionException in case of lookup, creation, or system errors
	 * @throws IllegalTransactionStateException if the given transaction definition
	 * cannot be executed (for example, if a currently active transaction is in
	 * conflict with the specified propagation behavior)
	 * @see TransactionDefinition#getPropagationBehavior
	 * @see TransactionDefinition#getIsolationLevel
	 * @see TransactionDefinition#getTimeout
	 * @see TransactionDefinition#isReadOnly
	 */
	// 根据指定的传播行为，发出当前活动的反应式事务或创建新的反应式事务。
	// 请注意，隔离级别或超时等参数只会应用于新事务，因此在参与活动事务时会被忽略。
	// 此外，并非每个事务管理器都支持所有事务定义设置：正确的事务管理器实现应该在遇到不受支持的设置时抛出异常。
	// 上述规则的一个例外是只读标志，如果不支持显式只读模式，则应忽略该标志。本质上，只读标志只是潜在优化的提示。
	// 参形：
	//			定义——TransactionDefinition 实例，描述传播行为、隔离级别、超时等。
	// 返回值：
	//			表示新交易或当前交易的交易状态对象
	// 抛出：
	//			TransactionException – 在查找、创建或系统错误的情况下
	//			IllegalTransactionStateException – 如果给定的事务定义无法执行（例如，如果当前活动的事务
	//			与指定的传播行为冲突）
	Mono<ReactiveTransaction> getReactiveTransaction(@Nullable TransactionDefinition definition)
			throws TransactionException;

	/**
	 * Commit the given transaction, with regard to its status. If the transaction
	 * has been marked rollback-only programmatically, perform a rollback.
	 * <p>If the transaction wasn't a new one, omit the commit for proper
	 * participation in the surrounding transaction. If a previous transaction
	 * has been suspended to be able to create a new one, resume the previous
	 * transaction after committing the new one.
	 * <p>Note that when the commit call completes, no matter if normally or
	 * throwing an exception, the transaction must be fully completed and
	 * cleaned up. No rollback call should be expected in such a case.
	 * <p>If this method throws an exception other than a TransactionException,
	 * then some before-commit error caused the commit attempt to fail. For
	 * example, an O/R Mapping tool might have tried to flush changes to the
	 * database right before commit, with the resulting DataAccessException
	 * causing the transaction to fail. The original exception will be
	 * propagated to the caller of this commit method in such a case.
	 * @param transaction object returned by the {@code getTransaction} method
	 * @throws UnexpectedRollbackException in case of an unexpected rollback
	 * that the transaction coordinator initiated
	 * @throws HeuristicCompletionException in case of a transaction failure
	 * caused by a heuristic decision on the side of the transaction coordinator
	 * @throws TransactionSystemException in case of commit or system errors
	 * (typically caused by fundamental resource failures)
	 * @throws IllegalTransactionStateException if the given transaction
	 * is already completed (that is, committed or rolled back)
	 * @see ReactiveTransaction#setRollbackOnly
	 */
	// 提交给定的事务，就其状态而言。如果事务已以编程方式标记为仅回滚，则执行回滚。
	//
	// 如果事务不是新事务，则省略提交以正确参与周围事务。如果先前的事务已暂停以便能够创建新事务，则在提交新事务后
	// 恢复先前的事务。
	//
	// 注意，当commit调用完成时，无论是正常还是抛出异常，事务都必须完全完成并清理。在这种情况下，不应期望回滚调用。
	//
	// 如果此方法抛出 TransactionException 以外的异常，则某些提交前错误会导致提交尝试失败。例如，O/R 映射工具
	// 可能试图在提交之前将更改刷新到数据库，结果 DataAccessException 导致事务失败。在这种情况下，原始异常将传播
	// 到此提交方法的调用者。
	// 参形：
	//			transaction – getTransaction方法返回的对象
	// 抛出：
	//			UnexpectedRollbackException – 在事务协调器发起的意外回滚的情况下
	//			HeuristicCompletionException – 如果事务协调器一方的启发式决策导致事务失败
	//			TransactionSystemException – 在提交或系统错误的情况下（通常由基本资源故障引起）
	//			IllegalTransactionStateException – 如果给定的事务已经完成（即提交或回滚）
	Mono<Void> commit(ReactiveTransaction transaction) throws TransactionException;

	/**
	 * Perform a rollback of the given transaction.
	 * <p>If the transaction wasn't a new one, just set it rollback-only for proper
	 * participation in the surrounding transaction. If a previous transaction
	 * has been suspended to be able to create a new one, resume the previous
	 * transaction after rolling back the new one.
	 * <p><b>Do not call rollback on a transaction if commit threw an exception.</b>
	 * The transaction will already have been completed and cleaned up when commit
	 * returns, even in case of a commit exception. Consequently, a rollback call
	 * after commit failure will lead to an IllegalTransactionStateException.
	 * @param transaction object returned by the {@code getTransaction} method
	 * @throws TransactionSystemException in case of rollback or system errors
	 * (typically caused by fundamental resource failures)
	 * @throws IllegalTransactionStateException if the given transaction
	 * is already completed (that is, committed or rolled back)
	 */
	// 执行给定事务的回滚。
	// 如果事务不是新事务，只需将其设置为仅回滚以正确参与周围事务。如果先前的事务已暂停以便能够创建新事务，
	// 则在回滚新事务后恢复先前的事务。
	// 如果提交引发异常，请勿在事务上调用回滚。当提交返回时，事务将已经完成并清理，即使在提交异常的情况下也是如此。
	// 因此，提交失败后的回滚调用将导致 IllegalTransactionStateException。
	// 参形：
	//			transaction – getTransaction方法返回的对象
	// 抛出：
	//			TransactionSystemException – 在回滚或系统错误的情况下（通常由基本资源故障引起）
	//			IllegalTransactionStateException – 如果给定的事务已经完成（即提交或回滚）
	Mono<Void> rollback(ReactiveTransaction transaction) throws TransactionException;

}
