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

import org.springframework.lang.Nullable;

/**
 * This is the central interface in Spring's imperative transaction infrastructure.
 * Applications can use this directly, but it is not primarily meant as an API:
 * Typically, applications will work with either TransactionTemplate or
 * declarative transaction demarcation through AOP.
 *
 * <p>For implementors, it is recommended to derive from the provided
 * {@link org.springframework.transaction.support.AbstractPlatformTransactionManager}
 * class, which pre-implements the defined propagation behavior and takes care
 * of transaction synchronization handling. Subclasses have to implement
 * template methods for specific states of the underlying transaction,
 * for example: begin, suspend, resume, commit.
 *
 * <p>The default implementations of this strategy interface are
 * {@link org.springframework.transaction.jta.JtaTransactionManager} and
 * {@link org.springframework.jdbc.datasource.DataSourceTransactionManager},
 * which can serve as an implementation guide for other transaction strategies.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 16.05.2003
 * @see org.springframework.transaction.support.TransactionTemplate
 * @see org.springframework.transaction.interceptor.TransactionInterceptor
 * @see org.springframework.transaction.ReactiveTransactionManager
 */
// 这是 Spring 的命令式事务基础设施中的中心接口。应用程序可以直接使用它，但它并不主要用作 API：通常，
// 应用程序将使用 TransactionTemplate 或通过 AOP 的声明性事务划分。
//
// 对于实现者，建议从提供的 org.springframework.transaction.support.AbstractPlatformTransactionManager
// 类派生，该类预先实现定义的传播行为并负责事务同步处理。子类必须为底层事务的特定状态实现模板方法，例如：开始、暂停、
// 恢复、提交。
//
// 该策略接口的默认实现是 org.springframework.transaction.jta.JtaTransactionManager
// 和 org.springframework.jdbc.datasource.DataSourceTransactionManager ，可以作为其他事务策略的实现指南
//
// Spring 平台事务管理器
public interface PlatformTransactionManager extends TransactionManager {

	/**
	 * Return a currently active transaction or create a new one, according to
	 * the specified propagation behavior.
	 * <p>Note that parameters like isolation level or timeout will only be applied
	 * to new transactions, and thus be ignored when participating in active ones.
	 * <p>Furthermore, not all transaction definition settings will be supported
	 * by every transaction manager: A proper transaction manager implementation
	 * should throw an exception when unsupported settings are encountered.
	 * <p>An exception to the above rule is the read-only flag, which should be
	 * ignored if no explicit read-only mode is supported. Essentially, the
	 * read-only flag is just a hint for potential optimization.
	 * @param definition the TransactionDefinition instance (can be {@code null} for defaults),
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
	// 根据指定的传播行为返回当前活动的事务或创建新事务。
	//
	// 请注意，隔离级别或超时等参数只会应用于新事务，因此在参与活动事务时会被忽略。
	//
	// 此外，并非每个事务管理器都支持所有事务定义设置：当遇到不受支持的设置时，正确的事务管理器实现应该抛出异常。
	//
	// 上述规则的一个例外是只读标志，如果不支持显式只读模式，则应忽略该标志。本质上，只读标志只是潜在优化的提示。
	// 参形：定义– TransactionDefinition 实例（默认可以为null ），描述传播行为、隔离级别、超时等。
	// 返回值：
	//			表示新交易或当前交易的交易状态对象
	// 抛出：
	//			TransactionException – 在查找、创建或系统错误的情况下
	//			IllegalTransactionStateException – 如果给定的事务定义无法执行（例如，如果当前活动的事务
	//			与指定的传播行为冲突）
	//
	// 它获取的并不是物理的事务，而是逻辑的事务，Spring 会将事务的自动提交改为 false,当方法执行完成后，并且事务
	// 状态没有问题，它会显式的去提交。所以这个时候在阶段性的 TransactionDefinition 里面，它有一个阶段性的东西。
	// 比如当我们再用 @Transactional 注解的时候，如果两个方法上面的行为不一样，比如它上面的传播级别不一样。或者 Rollback
	// 回滚策略不一样，这时候它会有两个 TransactionDefinition.所以它的状态也会不一样。因为每个方法执行阶段的状态本来
	// 就不一样
	//
	// TransactionDefinition 是死的，比如你去定义一个 Transaction 的时候，你定义了事务的传播以及隔离级别，
	// 一旦定义好后，这个方法中方法是静态的，运行时通常不会修改。定义完成后，这个定义就是一个固化的。但是它的状态不一定，
	// 它的状态会伴随着你的错误和异常会发生一些回滚或者其他情况。比如根据你的隔离级别和传播级别判断它是不是一个新的事务。
	TransactionStatus getTransaction(@Nullable TransactionDefinition definition)
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
	 * @param status object returned by the {@code getTransaction} method
	 * @throws UnexpectedRollbackException in case of an unexpected rollback
	 * that the transaction coordinator initiated
	 * @throws HeuristicCompletionException in case of a transaction failure
	 * caused by a heuristic decision on the side of the transaction coordinator
	 * @throws TransactionSystemException in case of commit or system errors
	 * (typically caused by fundamental resource failures)
	 * @throws IllegalTransactionStateException if the given transaction
	 * is already completed (that is, committed or rolled back)
	 * @see TransactionStatus#setRollbackOnly
	 */
	// 提交给定的事务，就其状态而言。如果事务已以编程方式标记为仅回滚，则执行回滚。
	//
	// 如果事务不是新事务，则省略提交以正确参与周围事务。如果先前的事务已暂停以便能够创建新事务，则在
	// 提交新事务后恢复先前的事务。
	//
	// 注意，当 commit 调用完成时，无论是正常还是抛出异常，事务都必须完全完成并清理。在这种情况下，不应期望回滚调用。
	//
	// 如果此方法抛出 TransactionException 以外的异常，则某些提交前错误会导致提交尝试失败。
	// 例如，O/R 映射工具可能试图在提交之前将更改刷新到数据库，结果 DataAccessException 导致事务失败。
	// 在这种情况下，原始异常将传播到此提交方法的调用者。
	// 参形：
	//			status – getTransaction方法返回的对象
	// 抛出：
	//			UnexpectedRollbackException – 在事务协调器发起的意外回滚的情况下
	//			HeuristicCompletionException – 如果事务协调器一方的启发式决策导致事务失败
	//			TransactionSystemException – 在提交或系统错误的情况下（通常由基本资源故障引起）
	//			IllegalTransactionStateException – 如果给定的事务已经完成（即提交或回滚）
	// commit 某一个 Transaction 的状态。即某一个逻辑事务上的状态。局部性操作
	void commit(TransactionStatus status) throws TransactionException;

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
	 * @param status object returned by the {@code getTransaction} method
	 * @throws TransactionSystemException in case of rollback or system errors
	 * (typically caused by fundamental resource failures)
	 * @throws IllegalTransactionStateException if the given transaction
	 * is already completed (that is, committed or rolled back)
	 */
	// 执行给定事务的回滚。
	//
	// 如果事务不是新事务，只需将其设置为仅回滚以正确参与周围事务。如果先前的事务已暂停以便能够创建新事务，
	// 则在回滚新事务后恢复先前的事务。
	//
	// 如果提交引发异常，请勿在事务上调用回滚。当提交返回时，事务将已经完成并清理，即使在提交异常的情况下也是如此。
	// 因此，提交失败后的回滚调用将导致 IllegalTransactionStateException。
	// 参形：
	//			status – getTransaction方法返回的对象
	// 抛出：
	//			TransactionSystemException – 在回滚或系统错误的情况下（通常由基本资源故障引起）
	//			IllegalTransactionStateException – 如果给定的事务已经完成（即提交或回滚）
	// rollback 某一个 Transaction 的状态。即某一个逻辑事务上的状态。局部性操作
	void rollback(TransactionStatus status) throws TransactionException;

}
