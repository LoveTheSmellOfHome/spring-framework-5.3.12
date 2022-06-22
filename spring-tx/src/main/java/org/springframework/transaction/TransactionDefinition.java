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

package org.springframework.transaction;

import org.springframework.lang.Nullable;

/**
 * Interface that defines Spring-compliant transaction properties.
 * Based on the propagation behavior definitions analogous to EJB CMT attributes.
 *
 * <p>Note that isolation level and timeout settings will not get applied unless
 * an actual new transaction gets started. As only {@link #PROPAGATION_REQUIRED},
 * {@link #PROPAGATION_REQUIRES_NEW} and {@link #PROPAGATION_NESTED} can cause
 * that, it usually doesn't make sense to specify those settings in other cases.
 * Furthermore, be aware that not all transaction managers will support those
 * advanced features and thus might throw corresponding exceptions when given
 * non-default values.
 *
 * <p>The {@link #isReadOnly() read-only flag} applies to any transaction context,
 * whether backed by an actual resource transaction or operating non-transactionally
 * at the resource level. In the latter case, the flag will only apply to managed
 * resources within the application, such as a Hibernate {@code Session}.
 *
 * @author Juergen Hoeller
 * @since 08.05.2003
 * @see PlatformTransactionManager#getTransaction(TransactionDefinition)
 * @see org.springframework.transaction.support.DefaultTransactionDefinition
 * @see org.springframework.transaction.interceptor.TransactionAttribute
 */
// 定义符合 Spring 的事务属性的接口。基于类似于 EJB CMT 属性的传播行为定义
//
// 请注意，除非实际的新事务开始，否则不会应用隔离级别和超时设置。由于只有
// {@link PROPAGATION_REQUIRED}、{@link PROPAGATION_REQUIRES_NEW} 和 {@link PROPAGATION_NESTED} 会导致这种情况，
// 因此在其他情况下指定这些设置通常没有意义。此外，请注意并非所有事务管理器都支持这些高级功能，因此在给定非默认值时可能会抛出相应的异常。
//
// {@link isReadOnly() 只读标志}适用于任何事务上下文，无论是由实际资源事务支持还是在资源级别以非事务方式操作。
// 在后一种情况下，该标志仅适用于应用程序内的托管资源，例如 Hibernate {@code Session}。
//
// Spring 事务定义,和 JDBC 甚至 JMX 是有一定关联的，支持 7 种级别的事务传播，5种事务隔离级别，是一个完整的逻辑事务
// 以 API 的形式封装 Transactional 的元信息
public interface TransactionDefinition {

	/**
	 * Support a current transaction; create a new one if none exists.
	 * Analogous to the EJB transaction attribute of the same name.
	 * <p>This is typically the default setting of a transaction definition,
	 * and typically defines a transaction synchronization scope.
	 */
	// 支持当前交易；如果不存在，则创建一个新的。类似于同名的 EJB 事务属性。
	//
	// 这通常是事务定义的默认设置，并且通常定义事务同步范围。
	//
	// PROPAGATION_REQUIRED 强制执行物理事务，如果尚不存在事务，则在当前范围内本地执行或参与为更大范围定义的
	// 现有“外部”事务。这是同一线程内的常见调用堆栈安排中的一个很好的默认设置（例如，委托给多个存储库方法的服务外观，
	// 其中所有底层资源都必须参与服务级事务）。
	//
	// 当传播设置为 PROPAGATION_REQUIRED 时，将为应用该设置的每个方法创建一个逻辑事务范围。每个这样的逻辑事务
	// 范围可以单独确定仅回滚状态，外部事务范围在逻辑上独立于内部事务范围。在标准 PROPAGATION_REQUIRED 行为的情况下，
	// 所有这些范围都映射到同一个物理事务。因此，在内部事务范围内设置的仅回滚标记确实会影响外部事务实际提交的机会。
	//
	// 但是，在内部事务范围设置了仅回滚标记的情况下，外部事务尚未决定回滚本身，因此回滚（由内部事务范围静默触发）是意外的。
	// UnexpectedRollbackException在该点抛出一个对应的。这是预期的行为，因此事务的调用者永远不会被误导以为执行了提交，
	// 而实际上并没有执行。因此，如果内部事务（外部调用者不知道）默默地将事务标记为仅回滚，外部调用者仍会调用提交。外部调用
	// 者需要接收一个 UnexpectedRollbackException 来清楚地表明执行了回滚。
	//
	// 必须的事务
	int PROPAGATION_REQUIRED = 0;

	/**
	 * Support a current transaction; execute non-transactionally if none exists.
	 * Analogous to the EJB transaction attribute of the same name.
	 * <p><b>NOTE:</b> For transaction managers with transaction synchronization,
	 * {@code PROPAGATION_SUPPORTS} is slightly different from no transaction
	 * at all, as it defines a transaction scope that synchronization might apply to.
	 * As a consequence, the same resources (a JDBC {@code Connection}, a
	 * Hibernate {@code Session}, etc) will be shared for the entire specified
	 * scope. Note that the exact behavior depends on the actual synchronization
	 * configuration of the transaction manager!
	 * <p>In general, use {@code PROPAGATION_SUPPORTS} with care! In particular, do
	 * not rely on {@code PROPAGATION_REQUIRED} or {@code PROPAGATION_REQUIRES_NEW}
	 * <i>within</i> a {@code PROPAGATION_SUPPORTS} scope (which may lead to
	 * synchronization conflicts at runtime). If such nesting is unavoidable, make sure
	 * to configure your transaction manager appropriately (typically switching to
	 * "synchronization on actual transaction").
	 * @see org.springframework.transaction.support.AbstractPlatformTransactionManager#setTransactionSynchronization
	 * @see org.springframework.transaction.support.AbstractPlatformTransactionManager#SYNCHRONIZATION_ON_ACTUAL_TRANSACTION
	 */
	// 支持当前交易；如果不存在则以非事务方式执行。类似于同名的 EJB 事务属性。
	//
	// 注意：对于具有事务同步的事务管理器，{@code PROPAGATION_SUPPORTS} 与根本没有事务略有不同，
	// 因为它定义了同步可能适用的事务范围。因此，相同的资源（JDBC {@code Connection}、Hibernate {@code Session} 等）
	// 将在整个指定范围内共享。请注意，确切的行为取决于事务管理器的实际同步配置！
	//
	// 一般来说，请小心使用 {@code PROPAGATION_SUPPORTS}！特别是，不要依赖
	// {@code PROPAGATION_REQUIRED} 或 {@code PROPAGATION_REQUIRES_NEW}
	// 在 {@code PROPAGATION_SUPPORTS} 范围（这可能会导致运行时的同步冲突）。
	// 如果这种嵌套不可避免，请确保适当地配置您的事务管理器（通常切换到“实际事务同步”）。
	//
	// 支持事务
	int PROPAGATION_SUPPORTS = 1;

	/**
	 * Support a current transaction; throw an exception if no current transaction
	 * exists. Analogous to the EJB transaction attribute of the same name.
	 * <p>Note that transaction synchronization within a {@code PROPAGATION_MANDATORY}
	 * scope will always be driven by the surrounding transaction.
	 */
	// 支持当前交易；如果当前事务不存在，则抛出异常。类似于同名的 EJB 事务属性。
	//
	// 请注意，{@code PROPAGATION_MANDATORY} 范围内的事务同步将始终由周围的事务驱动。
	//
	// 强制的事务
	int PROPAGATION_MANDATORY = 2;

	/**
	 * Create a new transaction, suspending the current transaction if one exists.
	 * Analogous to the EJB transaction attribute of the same name.
	 * <p><b>NOTE:</b> Actual transaction suspension will not work out-of-the-box
	 * on all transaction managers. This in particular applies to
	 * {@link org.springframework.transaction.jta.JtaTransactionManager},
	 * which requires the {@code javax.transaction.TransactionManager} to be
	 * made available it to it (which is server-specific in standard Java EE).
	 * <p>A {@code PROPAGATION_REQUIRES_NEW} scope always defines its own
	 * transaction synchronizations. Existing synchronizations will be suspended
	 * and resumed appropriately.
	 * @see org.springframework.transaction.jta.JtaTransactionManager#setTransactionManager
	 */
	// 创建一个新事务，如果存在则暂停当前事务。类似于同名的 EJB 事务属性。
	//
	// 注意：实际的事务暂停不会在所有事务管理器上开箱即用。这尤其适用于
	// {@link org.springframework.transaction.jta.JtaTransactionManager}，它需要
	// {@code javax.transaction.TransactionManager} 对其可用（这是标准 Java EE 中特定于服务器的）。
	//
	// {@code PROPAGATION_REQUIRES_NEW} 范围始终定义自己的事务同步。现有的同步将被暂停并适当地恢复。
	//
	// 新的事务
	//
	// PROPAGATION_REQUIRES_NEW 与 PROPAGATION_REQUIRED 相反，始终为每个受影响的事务范围(方法)使用独立的物理事务，
	// 从不参与外部范围的现有事务。在这样的安排中，底层资源事务是不同的，因此可以独立地提交或回滚，外部事务不受内部事务
	// 的回滚状态的影响，内部事务的锁在其完成后立即释放。这样一个独立的内部事务也可以声明自己的隔离级别、超时和只读设置，
	// 而不是继承外部事务的特性。比如你有两张表 User,Account,你在存用户的时候，不会影响到账户
	int PROPAGATION_REQUIRES_NEW = 3;

	/**
	 * Do not support a current transaction; rather always execute non-transactionally.
	 * Analogous to the EJB transaction attribute of the same name.
	 * <p><b>NOTE:</b> Actual transaction suspension will not work out-of-the-box
	 * on all transaction managers. This in particular applies to
	 * {@link org.springframework.transaction.jta.JtaTransactionManager},
	 * which requires the {@code javax.transaction.TransactionManager} to be
	 * made available it to it (which is server-specific in standard Java EE).
	 * <p>Note that transaction synchronization is <i>not</i> available within a
	 * {@code PROPAGATION_NOT_SUPPORTED} scope. Existing synchronizations
	 * will be suspended and resumed appropriately.
	 * @see org.springframework.transaction.jta.JtaTransactionManager#setTransactionManager
	 */
	// 不支持当前事务；而是始终以非事务方式执行。类似于同名的 EJB 事务属性。
	//
	// 注意： 实际的事务暂停不会在所有事务管理器上开箱即用。
	// 这尤其适用于 {@link org.springframework.transaction.jta.JtaTransactionManager}，
	// 它需要 {@code javax.transaction.TransactionManager} 对其可用（这是标准 Java EE 中特定于服务器的）。
	//
	// 请注意，在 {@code PROPAGATION_NOT_SUPPORTED} 范围内，事务同步不可用。现有的同步将被暂停并适当地恢复。
	//
	// 不支持事务
	int PROPAGATION_NOT_SUPPORTED = 4;

	/**
	 * Do not support a current transaction; throw an exception if a current transaction
	 * exists. Analogous to the EJB transaction attribute of the same name.
	 * <p>Note that transaction synchronization is <i>not</i> available within a
	 * {@code PROPAGATION_NEVER} scope.
	 */
	// 不支持当前事务；如果当前事务存在则抛出异常。类似于同名的 EJB 事务属性。
	//
	// 请注意，在 {@code PROPAGATION_NEVER} 范围内，事务同步不可用。
	//
	// 不可用事务
	int PROPAGATION_NEVER = 5;

	/**
	 * Execute within a nested transaction if a current transaction exists,
	 * behave like {@link #PROPAGATION_REQUIRED} otherwise. There is no
	 * analogous feature in EJB.
	 * <p><b>NOTE:</b> Actual creation of a nested transaction will only work on
	 * specific transaction managers. Out of the box, this only applies to the JDBC
	 * {@link org.springframework.jdbc.datasource.DataSourceTransactionManager}
	 * when working on a JDBC 3.0 driver. Some JTA providers might support
	 * nested transactions as well.
	 * @see org.springframework.jdbc.datasource.DataSourceTransactionManager
	 */
	// 如果当前事务存在，则在嵌套事务中执行，否则行为类似于 {@link PROPAGATION_REQUIRED}。 EJB 中没有类似的特性。
	//
	// 注意：嵌套事务的实际创建仅适用于特定的事务管理器。开箱即用，这仅适用于处理 JDBC 3.0 驱动程序时的
	//
	// JDBC {@link org.springframework.jdbc.datasource.DataSourceTransactionManager}。一些 JTA
	// 提供者也可能支持嵌套事务。
	//
	// PROPAGATION_NESTED 使用具有多个可以回滚的保存点的单个物理事务。这种部分回滚让内部事务范围触发其范围的回滚，
	// 尽管某些操作已回滚，但外部事务能够继续物理事务。此设置通常映射到 JDBC 保存点，因此它仅适用于 JDBC 资源事务。
	// 参见 Spring 的 DataSourceTransactionManager.
	//
	// 嵌套事务：要建立还原点
	int PROPAGATION_NESTED = 6;


	/**
	 * Use the default isolation level of the underlying datastore.
	 * All other levels correspond to the JDBC isolation levels.
	 * @see java.sql.Connection
	 */
	// 隔离级别 5 种照搬了 JDBC Connection 接口中的东西
	// 使用底层数据存储的默认隔离级别。所有其他级别对应于 JDBC 隔离级别
	int ISOLATION_DEFAULT = -1;

	/**
	 * Indicates that dirty reads, non-repeatable reads and phantom reads
	 * can occur.
	 * <p>This level allows a row changed by one transaction to be read by another
	 * transaction before any changes in that row have been committed (a "dirty read").
	 * If any of the changes are rolled back, the second transaction will have
	 * retrieved an invalid row.
	 * @see java.sql.Connection#TRANSACTION_READ_UNCOMMITTED
	 */
	// 表示可能发生脏读、不可重复读和幻读。
	//
	// 此级别允许在提交该行中的任何更改之前由另一个事务读取该行更改的行（“脏读”）。如果回滚任何更改，
	// 则第二个事务将检索到无效行。
	int ISOLATION_READ_UNCOMMITTED = 1;  // same as java.sql.Connection.TRANSACTION_READ_UNCOMMITTED;

	/**
	 * Indicates that dirty reads are prevented; non-repeatable reads and
	 * phantom reads can occur.
	 * <p>This level only prohibits a transaction from reading a row
	 * with uncommitted changes in it.
	 * @see java.sql.Connection#TRANSACTION_READ_COMMITTED
	 */
	// 表示防止脏读；可能发生不可重复读和幻读。
	//
	// 此级别仅禁止事务读取其中包含未提交更改的行。
	int ISOLATION_READ_COMMITTED = 2;  // same as java.sql.Connection.TRANSACTION_READ_COMMITTED;

	/**
	 * Indicates that dirty reads and non-repeatable reads are prevented;
	 * phantom reads can occur.
	 * <p>This level prohibits a transaction from reading a row with uncommitted changes
	 * in it, and it also prohibits the situation where one transaction reads a row,
	 * a second transaction alters the row, and the first transaction re-reads the row,
	 * getting different values the second time (a "non-repeatable read").
	 * @see java.sql.Connection#TRANSACTION_REPEATABLE_READ
	 */
	// 表示防止脏读和不可重复读；可能会发生幻读。
	//
	// 此级别禁止事务读取未提交更改的行，也禁止一个事务读取一行，第二个事务更改该行，
	// 第一个事务重新读取该行，变得不同的情况值第二次（“不可重复读取”）
	int ISOLATION_REPEATABLE_READ = 4;  // same as java.sql.Connection.TRANSACTION_REPEATABLE_READ;

	/**
	 * Indicates that dirty reads, non-repeatable reads and phantom reads
	 * are prevented.
	 * <p>This level includes the prohibitions in {@link #ISOLATION_REPEATABLE_READ}
	 * and further prohibits the situation where one transaction reads all rows that
	 * satisfy a {@code WHERE} condition, a second transaction inserts a row
	 * that satisfies that {@code WHERE} condition, and the first transaction
	 * re-reads for the same condition, retrieving the additional "phantom" row
	 * in the second read.
	 * @see java.sql.Connection#TRANSACTION_SERIALIZABLE
	 */
	// 表示防止脏读、不可重复读和幻读。
	//
	// 该级别包括{@link ISOLATION_REPEATABLE_READ}中的禁止，并进一步禁止一个事务读取满足{@code WHERE}条件的所有行，
	// 第二个事务插入满足{@code WHERE}条件的行的情况，并且第一个事务在相同条件下重新读取，在第二次读取中检索附加的“幻影”行。
	int ISOLATION_SERIALIZABLE = 8;  // same as java.sql.Connection.TRANSACTION_SERIALIZABLE;


	/**
	 * Use the default timeout of the underlying transaction system,
	 * or none if timeouts are not supported.
	 */
	// 使用底层事务系统的默认超时，如果不支持超时，则不使用，-1 永不超时
	int TIMEOUT_DEFAULT = -1;


	/**
	 * Return the propagation behavior.
	 * <p>Must return one of the {@code PROPAGATION_XXX} constants
	 * defined on {@link TransactionDefinition this interface}.
	 * <p>The default is {@link #PROPAGATION_REQUIRED}.
	 * @return the propagation behavior
	 * @see #PROPAGATION_REQUIRED
	 * @see org.springframework.transaction.support.TransactionSynchronizationManager#isActualTransactionActive()
	 */
	// 获取事务传播行为。
	// 必须返回在 {@link TransactionDefinition this interface} 上定义的 {@code PROPAGATION_XXX} 常量之一。
	//
	// 默认值为 {@link PROPAGATION_REQUIRED}。
	default int getPropagationBehavior() {
		// 默认方式需要事务传播
		return PROPAGATION_REQUIRED;
	}

	/**
	 * Return the isolation level.
	 * <p>Must return one of the {@code ISOLATION_XXX} constants defined on
	 * {@link TransactionDefinition this interface}. Those constants are designed
	 * to match the values of the same constants on {@link java.sql.Connection}.
	 * <p>Exclusively designed for use with {@link #PROPAGATION_REQUIRED} or
	 * {@link #PROPAGATION_REQUIRES_NEW} since it only applies to newly started
	 * transactions. Consider switching the "validateExistingTransactions" flag to
	 * "true" on your transaction manager if you'd like isolation level declarations
	 * to get rejected when participating in an existing transaction with a different
	 * isolation level.
	 * <p>The default is {@link #ISOLATION_DEFAULT}. Note that a transaction manager
	 * that does not support custom isolation levels will throw an exception when
	 * given any other level than {@link #ISOLATION_DEFAULT}.
	 * @return the isolation level
	 * @see #ISOLATION_DEFAULT
	 * @see org.springframework.transaction.support.AbstractPlatformTransactionManager#setValidateExistingTransaction
	 */
	// 返回隔离级别。和 JDBC 相关
	// 必须返回在 {@link TransactionDefinition this interface} 上定义的 {@code ISOLATION_XXX} 常量之一。
	// 这些常量旨在匹配 {@link java.sql.Connection} 上相同常量的值。
	//
	// 专为与 {@link PROPAGATION_REQUIRED} 或 {@link PROPAGATION_REQUIRES_NEW} 一起使用而设计，
	// 因为它仅适用于新启动的事务。如果您希望隔离级别声明在参与具有不同隔离级别的现有事务时被拒绝，
	// 请考虑将事务管理器上的“validateExistingTransactions”标志切换为“true”。
	//
	// 默认值为 {@link ISOLATION_DEFAULT}。请注意，当给定除 {@link ISOLATION_DEFAULT} 之外的任何其他级别时，
	// 不支持自定义隔离级别的事务管理器将引发异常。
	default int getIsolationLevel() {
		return ISOLATION_DEFAULT;
	}

	/**
	 * Return the transaction timeout.
	 * <p>Must return a number of seconds, or {@link #TIMEOUT_DEFAULT}.
	 * <p>Exclusively designed for use with {@link #PROPAGATION_REQUIRED} or
	 * {@link #PROPAGATION_REQUIRES_NEW} since it only applies to newly started
	 * transactions.
	 * <p>Note that a transaction manager that does not support timeouts will throw
	 * an exception when given any other timeout than {@link #TIMEOUT_DEFAULT}.
	 * <p>The default is {@link #TIMEOUT_DEFAULT}.
	 * @return the transaction timeout
	 */
	// 获取事务执行超时时间。
	// <p>必须返回秒数，或 {@link TIMEOUT_DEFAULT}。
	// <p>专为与 {@link PROPAGATION_REQUIRED} 或 {@link PROPAGATION_REQUIRES_NEW} 一起使用而设计，因为它仅适用于新启动的事务。
	// <p>请注意，不支持超时的事务管理器将在给定除 {@link TIMEOUT_DEFAULT} 之外的任何其他超时时抛出异常。
	// <p>默认值为 {@link TIMEOUT_DEFAULT}。
	default int getTimeout() {
		return TIMEOUT_DEFAULT;
	}

	/**
	 * Return whether to optimize as a read-only transaction.
	 * <p>The read-only flag applies to any transaction context, whether backed
	 * by an actual resource transaction ({@link #PROPAGATION_REQUIRED}/
	 * {@link #PROPAGATION_REQUIRES_NEW}) or operating non-transactionally at
	 * the resource level ({@link #PROPAGATION_SUPPORTS}). In the latter case,
	 * the flag will only apply to managed resources within the application,
	 * such as a Hibernate {@code Session}.
	 * <p>This just serves as a hint for the actual transaction subsystem;
	 * it will <i>not necessarily</i> cause failure of write access attempts.
	 * A transaction manager which cannot interpret the read-only hint will
	 * <i>not</i> throw an exception when asked for a read-only transaction.
	 * @return {@code true} if the transaction is to be optimized as read-only
	 * ({@code false} by default)
	 * @see org.springframework.transaction.support.TransactionSynchronization#beforeCommit(boolean)
	 * @see org.springframework.transaction.support.TransactionSynchronizationManager#isCurrentTransactionReadOnly()
	 */
	// 返回是不是只读事务。只读事务作用范围，影响对性能来说会比较小一点
	// 只读标志适用于任何事务上下文，无论是由实际资源事务支持（{@link #PROPAGATION_REQUIRED}/
	// {@link #PROPAGATION_REQUIRES_NEW}）
	// 还是在资源级别以非事务方式操作（{@link #PROPAGATION_SUPPORTS}）。在后一种情况下，该标志仅适用于应用
	// 程序内的托管资源，
	// 例如 Hibernate {@code Session}。
	//
	// 这只是对实际事务子系统的提示；它<i>不一定<i>会导致写访问尝试失败。
	// 无法解释只读提示的事务管理器将在请求只读事务时<i>not<i> 抛出异常。
	// @return {@code true} 如果交易被优化为只读（默认为 {@code false}）
	//
	// 是否为只读事务，一部分阶段定义
	default boolean isReadOnly() {
		return false;
	}

	/**
	 * Return the name of this transaction. Can be {@code null}.
	 * <p>This will be used as the transaction name to be shown in a
	 * transaction monitor, if applicable (for example, WebLogic's).
	 * <p>In case of Spring's declarative transactions, the exposed name will be
	 * the {@code fully-qualified class name + "." + method name} (by default).
	 * @return the name of this transaction ({@code null} by default}
	 * @see org.springframework.transaction.interceptor.TransactionAspectSupport
	 * @see org.springframework.transaction.support.TransactionSynchronizationManager#getCurrentTransactionName()
	 */
	// 返回此交易的名称。可以是 {@code null}。
	// <p>如果适用（例如，WebLogic 的），这将用作要在事务监视器中显示的事务名称。
	// <p>在 Spring 的声明性事务的情况下，公开的名称将是 {@code 完全限定的类名 + "." + 方法名称}（默认）。
	// @return 此交易的名称（默认为 {@code null}}
	@Nullable
	default String getName() {
		return null;
	}


	// Static builder methods
	// 静态构建器方法

	/**
	 * Return an unmodifiable {@code TransactionDefinition} with defaults.
	 * <p>For customization purposes, use the modifiable
	 * {@link org.springframework.transaction.support.DefaultTransactionDefinition}
	 * instead.
	 * @since 5.2
	 */
	// 使用默认值返回不可修改的 {@code TransactionDefinition}。
	// <p>出于自定义目的，请改用可修改的 {@link org.springframework.transaction.support.DefaultTransactionDefinition}。
	static TransactionDefinition withDefaults() {
		return StaticTransactionDefinition.INSTANCE;
	}

}
