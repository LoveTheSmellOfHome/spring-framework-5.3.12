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

package org.springframework.jdbc.datasource;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.lang.Nullable;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.ResourceTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronizationUtils;
import org.springframework.util.Assert;

/**
 * {@link org.springframework.transaction.PlatformTransactionManager}
 * implementation for a single JDBC {@link javax.sql.DataSource}. This class is
 * capable of working in any environment with any JDBC driver, as long as the setup
 * uses a {@code javax.sql.DataSource} as its {@code Connection} factory mechanism.
 * Binds a JDBC Connection from the specified DataSource to the current thread,
 * potentially allowing for one thread-bound Connection per DataSource.
 *
 * <p><b>Note: The DataSource that this transaction manager operates on needs
 * to return independent Connections.</b> The Connections may come from a pool
 * (the typical case), but the DataSource must not return thread-scoped /
 * request-scoped Connections or the like. This transaction manager will
 * associate Connections with thread-bound transactions itself, according
 * to the specified propagation behavior. It assumes that a separate,
 * independent Connection can be obtained even during an ongoing transaction.
 *
 * <p>Application code is required to retrieve the JDBC Connection via
 * {@link DataSourceUtils#getConnection(DataSource)} instead of a standard
 * Java EE-style {@link DataSource#getConnection()} call. Spring classes such as
 * {@link org.springframework.jdbc.core.JdbcTemplate} use this strategy implicitly.
 * If not used in combination with this transaction manager, the
 * {@link DataSourceUtils} lookup strategy behaves exactly like the native
 * DataSource lookup; it can thus be used in a portable fashion.
 *
 * <p>Alternatively, you can allow application code to work with the standard
 * Java EE-style lookup pattern {@link DataSource#getConnection()}, for example for
 * legacy code that is not aware of Spring at all. In that case, define a
 * {@link TransactionAwareDataSourceProxy} for your target DataSource, and pass
 * that proxy DataSource to your DAOs, which will automatically participate in
 * Spring-managed transactions when accessing it.
 *
 * <p>Supports custom isolation levels, and timeouts which get applied as
 * appropriate JDBC statement timeouts. To support the latter, application code
 * must either use {@link org.springframework.jdbc.core.JdbcTemplate}, call
 * {@link DataSourceUtils#applyTransactionTimeout} for each created JDBC Statement,
 * or go through a {@link TransactionAwareDataSourceProxy} which will create
 * timeout-aware JDBC Connections and Statements automatically.
 *
 * <p>Consider defining a {@link LazyConnectionDataSourceProxy} for your target
 * DataSource, pointing both this transaction manager and your DAOs to it.
 * This will lead to optimized handling of "empty" transactions, i.e. of transactions
 * without any JDBC statements executed. A LazyConnectionDataSourceProxy will not fetch
 * an actual JDBC Connection from the target DataSource until a Statement gets executed,
 * lazily applying the specified transaction settings to the target Connection.
 *
 * <p>This transaction manager supports nested transactions via the JDBC 3.0
 * {@link java.sql.Savepoint} mechanism. The
 * {@link #setNestedTransactionAllowed "nestedTransactionAllowed"} flag defaults
 * to "true", since nested transactions will work without restrictions on JDBC
 * drivers that support savepoints (such as the Oracle JDBC driver).
 *
 * <p>This transaction manager can be used as a replacement for the
 * {@link org.springframework.transaction.jta.JtaTransactionManager} in the single
 * resource case, as it does not require a container that supports JTA, typically
 * in combination with a locally defined JDBC DataSource (e.g. an Apache Commons
 * DBCP connection pool). Switching between this local strategy and a JTA
 * environment is just a matter of configuration!
 *
 * <p>As of 4.3.4, this transaction manager triggers flush callbacks on registered
 * transaction synchronizations (if synchronization is generally active), assuming
 * resources operating on the underlying JDBC {@code Connection}. This allows for
 * setup analogous to {@code JtaTransactionManager}, in particular with respect to
 * lazily registered ORM resources (e.g. a Hibernate {@code Session}).
 *
 * <p><b>NOTE: As of 5.3, {@link org.springframework.jdbc.support.JdbcTransactionManager}
 * is available as an extended subclass which includes commit/rollback exception
 * translation, aligned with {@link org.springframework.jdbc.core.JdbcTemplate}.</b>
 *
 * @author Juergen Hoeller
 * @since 02.05.2003
 * @see #setNestedTransactionAllowed
 * @see java.sql.Savepoint
 * @see DataSourceUtils#getConnection(javax.sql.DataSource)
 * @see DataSourceUtils#applyTransactionTimeout
 * @see DataSourceUtils#releaseConnection
 * @see TransactionAwareDataSourceProxy
 * @see LazyConnectionDataSourceProxy
 * @see org.springframework.jdbc.core.JdbcTemplate
 */
// org.springframework.transaction.PlatformTransactionManager 实现单个 JDBC DataSource 。
// 只要设置使用 javax.sql.DataSource 作为其 Connection 工厂机制，此类就能够在任何环境中使用任何 JDBC 驱动程序。
// 将指定数据源的 JDBC 连接绑定到当前线程，可能允许每个数据源有一个线程绑定连接。
//
// 注意：这个事务管理器操作的 DataSource 需要返回独立的 Connections。 Connections 可能来自池（典型情况），
// 但 DataSource 不得返回 thread-scoped / request-scoped Connections 等。该事务管理器将根据指定的
// 传播行为将 Connections 与线程绑定事务本身相关联。它假设即使在正在进行的事务期间也可以获得单独的、独立的连接。
//
// 应用程序代码需要通过 DataSourceUtils.getConnection(DataSource) 而不是标准的 Java EE
// 风格的 DataSource.getConnection()调用来检索 JDBC 连接。 Spring 类如 org.springframework.jdbc.core.JdbcTemplate
// 隐式使用此策略。如果不与此事务管理器结合使用， DataSourceUtils 查找策略的行为与本机 DataSource 查找完全相同；
// 因此，它可以以便携式方式使用。
//
// 或者，您可以允许应用程序代码使用标准的 Java EE 样式查找模式 DataSource.getConnection() ，例如对于根本不了解 Spring 的遗留代码。
// 在这种情况下，为您的目标 DataSource 定义一个 TransactionAwareDataSourceProxy ，并将该代理 DataSource 传递给您的 DAO，
// 当访问它时，它将自动参与 Spring 管理的事务。
//
// 支持自定义隔离级别，以及作为适当的 JDBC 语句超时应用的超时。要支持后者，应用程序代码必须使用
// org.springframework.jdbc.core.JdbcTemplate ，为每个创建的 JDBC 语句调用
// DataSourceUtils.applyTransactionTimeout ，或者通过
// TransactionAwareDataSourceProxy 自动创建超时感知 JDBC 连接和语句。
//
// 考虑为您的目标 DataSource 定义一个 LazyConnectionDataSourceProxy ，将这个事务管理器和您的 DAO 都指向它。
// 这将导致对“空”事务的优化处理，即没有执行任何 JDBC 语句的事务。 LazyConnectionDataSourceProxy 在
// 执行 Statement 之前不会从目标 DataSource 获取实际的 JDBC Connection，从而将指定的事务设置延迟应用到目标 Connection。
//
// 此事务管理器通过 JDBC 3.0 java.sql.Savepoint 机制支持嵌套事务。 "nestedTransactionAllowed" 标志默认为“true”，
// 因为嵌套事务将不受支持保存点的 JDBC 驱动程序（例如 Oracle JDBC 驱动程序）的限制。
//
// 这个事务管理器可以在单一资源的情况下用作 org.springframework.transaction.jta.JtaTransactionManager 的替代品，
// 因为它不需要支持 JTA 的容器，通常与本地定义的 JDBC 数据源（例如 Apache Commons DBCP 连接池）。
// 在这种本地策略和 JTA 环境之间切换只是配置问题！
//
// 从 4.3.4 开始，此事务管理器在已注册的事务同步（如果同步通常处于活动状态）上触发刷新回调，假设资源在
// 底层 JDBC Connection上运行。这允许类似于JtaTransactionManager的设置，特别是关于
// 延迟注册的 ORM 资源（例如 Hibernate Session ）。
//
// 注意：从 5.3 开始， org.springframework.jdbc.support.JdbcTransactionManager 可作
// 为扩展子类使用，其中包括提交/回滚异常转换，与org.springframework.jdbc.core.JdbcTemplate对齐。
@SuppressWarnings("serial")
public class DataSourceTransactionManager extends AbstractPlatformTransactionManager
		implements ResourceTransactionManager, InitializingBean {

	// 数据源
	@Nullable
	private DataSource dataSource;

	// 是否执行只读，默认为否
	private boolean enforceReadOnly = false;


	/**
	 * Create a new DataSourceTransactionManager instance.
	 * A DataSource has to be set to be able to use it.
	 * @see #setDataSource
	 */
	// 创建一个新的 DataSourceTransactionManager 实例。必须设置 DataSource 才能使用它。
	public DataSourceTransactionManager() {
		setNestedTransactionAllowed(true);
	}

	/**
	 * Create a new DataSourceTransactionManager instance.
	 * @param dataSource the JDBC DataSource to manage transactions for
	 */
	// 创建一个新的 DataSourceTransactionManager 实例。
	// 参形：dataSource – 用于管理事务的 JDBC 数据源
	public DataSourceTransactionManager(DataSource dataSource) {
		this();
		// 设置 数据源
		setDataSource(dataSource);
		// 获取数据源
		afterPropertiesSet();
	}


	/**
	 * Set the JDBC DataSource that this instance should manage transactions for.
	 * <p>This will typically be a locally defined DataSource, for example an
	 * Apache Commons DBCP connection pool. Alternatively, you can also drive
	 * transactions for a non-XA J2EE DataSource fetched from JNDI. For an XA
	 * DataSource, use JtaTransactionManager.
	 * <p>The DataSource specified here should be the target DataSource to manage
	 * transactions for, not a TransactionAwareDataSourceProxy. Only data access
	 * code may work with TransactionAwareDataSourceProxy, while the transaction
	 * manager needs to work on the underlying target DataSource. If there's
	 * nevertheless a TransactionAwareDataSourceProxy passed in, it will be
	 * unwrapped to extract its target DataSource.
	 * <p><b>The DataSource passed in here needs to return independent Connections.</b>
	 * The Connections may come from a pool (the typical case), but the DataSource
	 * must not return thread-scoped / request-scoped Connections or the like.
	 * @see TransactionAwareDataSourceProxy
	 * @see org.springframework.transaction.jta.JtaTransactionManager
	 */
	// 设置此实例应为其管理事务的 JDBC 数据源。
	//
	// 这通常是本地定义的数据源，例如 Apache Commons DBCP 连接池。或者，您也可以驱动从 JNDI 获取
	// 的非 XA J2EE 数据源的事务。对于 XA 数据源，使用 JtaTransactionManager。
	//
	// 此处指定的 DataSource 应该是管理事务的目标 DataSource，而不是 TransactionAwareDataSourceProxy。
	// 只有数据访问代码可以与 TransactionAwareDataSourceProxy 一起工作，而事务管理器需要在底层
	// 目标 DataSource 上工作。如果仍然有一个 TransactionAwareDataSourceProxy 传入，它将被解包以提取其目标 DataSource。
	//
	// 这里传入的 DataSource 需要返回独立的 Connections。 Connections 可能来自池（典型情况），但 DataSource 不得
	// 返回 thread-scoped / request-scoped Connections 等。
	public void setDataSource(@Nullable DataSource dataSource) {
		if (dataSource instanceof TransactionAwareDataSourceProxy) {
			// If we got a TransactionAwareDataSourceProxy, we need to perform transactions
			// for its underlying target DataSource, else data access code won't see
			// properly exposed transactions (i.e. transactions for the target DataSource).
			//
			// 如果我们有一个 TransactionAwareDataSourceProxy，我们需要为其底层目标 DataSource 执行事务，
			// 否则数据访问代码将看不到正确公开的事务（即目标 DataSource 的事务）。
			this.dataSource = ((TransactionAwareDataSourceProxy) dataSource).getTargetDataSource();
		}
		else {
			this.dataSource = dataSource;
		}
	}

	/**
	 * Return the JDBC DataSource that this instance manages transactions for.
	 */
	// 返回此实例为其管理事务的 JDBC 数据源。
	@Nullable
	public DataSource getDataSource() {
		return this.dataSource;
	}

	/**
	 * Obtain the DataSource for actual use.
	 * @return the DataSource (never {@code null})
	 * @throws IllegalStateException in case of no DataSource set
	 * @since 5.0
	 */
	// 获取实际使用的 DataSource。
	// 返回值：
	//			数据源（从不为null ）
	// 抛出：
	//			IllegalStateException – 如果没有设置 DataSource
	// 获取数据源
	protected DataSource obtainDataSource() {
		DataSource dataSource = getDataSource();
		Assert.state(dataSource != null, "No DataSource set");
		return dataSource;
	}

	/**
	 * Specify whether to enforce the read-only nature of a transaction
	 * (as indicated by {@link TransactionDefinition#isReadOnly()}
	 * through an explicit statement on the transactional connection:
	 * "SET TRANSACTION READ ONLY" as understood by Oracle, MySQL and Postgres.
	 * <p>The exact treatment, including any SQL statement executed on the connection,
	 * can be customized through {@link #prepareTransactionalConnection}.
	 * <p>This mode of read-only handling goes beyond the {@link Connection#setReadOnly}
	 * hint that Spring applies by default. In contrast to that standard JDBC hint,
	 * "SET TRANSACTION READ ONLY" enforces an isolation-level-like connection mode
	 * where data manipulation statements are strictly disallowed. Also, on Oracle,
	 * this read-only mode provides read consistency for the entire transaction.
	 * <p>Note that older Oracle JDBC drivers (9i, 10g) used to enforce this read-only
	 * mode even for {@code Connection.setReadOnly(true}. However, with recent drivers,
	 * this strong enforcement needs to be applied explicitly, e.g. through this flag.
	 * @since 4.3.7
	 * @see #prepareTransactionalConnection
	 */
	// 指定是否强制执行事务的只读性质（由 TransactionDefinition.isReadOnly() 通过事务连接上的显式语句指示 Oracle、MySQL 和
	// Postgres 理解的 “SET TRANSACTION READ ONLY”。
	//
	// 可以通过 prepareTransactionalConnection 自定义确切的处理方式，包括在连接上执行的任何 SQL 语句。
	//
	// 这种只读处理模式超出了默认情况下 Spring 应用的 Connection.setReadOnly 提示。与该标准 JDBC 提示相比，
	// “SET TRANSACTION READ ONLY” 强制执行类似隔离级别的连接模式，其中严格禁止数据操作语句。此外，在 Oracle 上，
	// 这种只读模式为整个事务提供了读取一致性。
	//
	// 请注意，即使对于 Connection.setReadOnly (true ，旧的 Oracle JDBC 驱动程序（9i、10g）也曾用于强制执行此只读模式。
	// 但是，对于最近的驱动程序，需要显式应用这种强大的强制执行，例如通过此标志。
	//
	// 如果设置为 TRUE,提供了强读取一致性
	public void setEnforceReadOnly(boolean enforceReadOnly) {
		this.enforceReadOnly = enforceReadOnly;
	}

	/**
	 * Return whether to enforce the read-only nature of a transaction
	 * through an explicit statement on the transactional connection.
	 * @since 4.3.7
	 * @see #setEnforceReadOnly
	 */
	// 返回是否通过事务连接上的显式语句强制执行事务的只读性质。
	public boolean isEnforceReadOnly() {
		return this.enforceReadOnly;
	}

	@Override
	public void afterPropertiesSet() {
		if (getDataSource() == null) {
			throw new IllegalArgumentException("Property 'dataSource' is required");
		}
	}


	// 获取资源工厂
	@Override
	public Object getResourceFactory() {
		return obtainDataSource();
	}

	// 获取事务
	@Override
	protected Object doGetTransaction() {
		DataSourceTransactionObject txObject = new DataSourceTransactionObject();
		txObject.setSavepointAllowed(isNestedTransactionAllowed());
		ConnectionHolder conHolder =
				(ConnectionHolder) TransactionSynchronizationManager.getResource(obtainDataSource());
		txObject.setConnectionHolder(conHolder, false);
		return txObject;
	}

	// 是否存在事务
	@Override
	protected boolean isExistingTransaction(Object transaction) {
		DataSourceTransactionObject txObject = (DataSourceTransactionObject) transaction;
		return (txObject.hasConnectionHolder() && txObject.getConnectionHolder().isTransactionActive());
	}

	// 开始阶段
	@Override
	protected void doBegin(Object transaction, TransactionDefinition definition) {
		DataSourceTransactionObject txObject = (DataSourceTransactionObject) transaction;
		Connection con = null;

		try {
			if (!txObject.hasConnectionHolder() ||
					txObject.getConnectionHolder().isSynchronizedWithTransaction()) {
				// 获取 Connection 对象
				Connection newCon = obtainDataSource().getConnection();
				if (logger.isDebugEnabled()) {
					logger.debug("Acquired Connection [" + newCon + "] for JDBC transaction");
				}
				txObject.setConnectionHolder(new ConnectionHolder(newCon), true);
			}

			txObject.getConnectionHolder().setSynchronizedWithTransaction(true);
			// 获取连接对象
			con = txObject.getConnectionHolder().getConnection();

			// 当前隔离级别
			Integer previousIsolationLevel = DataSourceUtils.prepareConnectionForTransaction(con, definition);
			// 设置隔离级别
			txObject.setPreviousIsolationLevel(previousIsolationLevel);
			// 设置只读
			txObject.setReadOnly(definition.isReadOnly());

			// Switch to manual commit if necessary. This is very expensive in some JDBC drivers,
			// so we don't want to do it unnecessarily (for example if we've explicitly
			// configured the connection pool to set it already).
			//
			// 如有必要，切换到手动提交。这在一些 JDBC 驱动程序中非常昂贵，所以我们不想不必要地
			// 这样做（例如，如果我们已经显式配置连接池以设置它）
			if (con.getAutoCommit()) {
				txObject.setMustRestoreAutoCommit(true);
				if (logger.isDebugEnabled()) {
					logger.debug("Switching JDBC Connection [" + con + "] to manual commit");
				}
				con.setAutoCommit(false);
			}

			// 预备事务连接
			prepareTransactionalConnection(con, definition);
			txObject.getConnectionHolder().setTransactionActive(true);

			int timeout = determineTimeout(definition);
			if (timeout != TransactionDefinition.TIMEOUT_DEFAULT) {
				txObject.getConnectionHolder().setTimeoutInSeconds(timeout);
			}

			// Bind the connection holder to the thread.
			// 将 connection holder 绑定到线程
			if (txObject.isNewConnectionHolder()) {
				TransactionSynchronizationManager.bindResource(obtainDataSource(), txObject.getConnectionHolder());
			}
		}

		catch (Throwable ex) {
			if (txObject.isNewConnectionHolder()) {
				// 释放连接
				DataSourceUtils.releaseConnection(con, obtainDataSource());
				txObject.setConnectionHolder(null, false);
			}
			throw new CannotCreateTransactionException("Could not open JDBC Connection for transaction", ex);
		}
	}

	// 挂起事务
	@Override
	protected Object doSuspend(Object transaction) {
		DataSourceTransactionObject txObject = (DataSourceTransactionObject) transaction;
		txObject.setConnectionHolder(null);
		return TransactionSynchronizationManager.unbindResource(obtainDataSource());
	}

	@Override
	protected void doResume(@Nullable Object transaction, Object suspendedResources) {
		// 绑定数据源到当前线程
		TransactionSynchronizationManager.bindResource(obtainDataSource(), suspendedResources);
	}

	// 提交
	@Override
	protected void doCommit(DefaultTransactionStatus status) {
		DataSourceTransactionObject txObject = (DataSourceTransactionObject) status.getTransaction();
		Connection con = txObject.getConnectionHolder().getConnection();
		if (status.isDebug()) {
			logger.debug("Committing JDBC transaction on Connection [" + con + "]");
		}
		try {
			// 提交
			con.commit();
		}
		catch (SQLException ex) {
			throw translateException("JDBC commit", ex);
		}
	}

	// 回滚
	@Override
	protected void doRollback(DefaultTransactionStatus status) {
		DataSourceTransactionObject txObject = (DataSourceTransactionObject) status.getTransaction();
		Connection con = txObject.getConnectionHolder().getConnection();
		if (status.isDebug()) {
			logger.debug("Rolling back JDBC transaction on Connection [" + con + "]");
		}
		try {
			// 回滚事务
			con.rollback();
		}
		catch (SQLException ex) {
			throw translateException("JDBC rollback", ex);
		}
	}

	// 设置回滚
	@Override
	protected void doSetRollbackOnly(DefaultTransactionStatus status) {
		DataSourceTransactionObject txObject = (DataSourceTransactionObject) status.getTransaction();
		if (status.isDebug()) {
			logger.debug("Setting JDBC transaction [" + txObject.getConnectionHolder().getConnection() +
					"] rollback-only");
		}
		txObject.setRollbackOnly();
	}

	// 完成后清除
	@Override
	protected void doCleanupAfterCompletion(Object transaction) {
		DataSourceTransactionObject txObject = (DataSourceTransactionObject) transaction;

		// Remove the connection holder from the thread, if exposed.
		// 如果暴漏了，从线程上移除连接持有者
		if (txObject.isNewConnectionHolder()) {
			// 释放绑定资源，即数据源
			TransactionSynchronizationManager.unbindResource(obtainDataSource());
		}

		// Reset connection.
		Connection con = txObject.getConnectionHolder().getConnection();
		try {
			if (txObject.isMustRestoreAutoCommit()) {
				// 设置自动提交
				con.setAutoCommit(true);
			}
			// 事务完成后重置连接
			DataSourceUtils.resetConnectionAfterTransaction(
					con, txObject.getPreviousIsolationLevel(), txObject.isReadOnly());
		}
		catch (Throwable ex) {
			logger.debug("Could not reset JDBC Connection after transaction", ex);
		}

		// 如果是新的连接持有者
		if (txObject.isNewConnectionHolder()) {
			if (logger.isDebugEnabled()) {
				logger.debug("Releasing JDBC Connection [" + con + "] after transaction");
			}
			// 释放连接
			DataSourceUtils.releaseConnection(con, this.dataSource);
		}

		// 清除连接
		txObject.getConnectionHolder().clear();
	}


	/**
	 * Prepare the transactional {@code Connection} right after transaction begin.
	 * <p>The default implementation executes a "SET TRANSACTION READ ONLY" statement
	 * if the {@link #setEnforceReadOnly "enforceReadOnly"} flag is set to {@code true}
	 * and the transaction definition indicates a read-only transaction.
	 * <p>The "SET TRANSACTION READ ONLY" is understood by Oracle, MySQL and Postgres
	 * and may work with other databases as well. If you'd like to adapt this treatment,
	 * override this method accordingly.
	 * @param con the transactional JDBC Connection
	 * @param definition the current transaction definition
	 * @throws SQLException if thrown by JDBC API
	 * @since 4.3.7
	 * @see #setEnforceReadOnly
	 */
	// 事务开始后立即准备事务Connection 。
	//
	// 如果"enforceReadOnly"标志设置为true并且事务定义指示只读事务，则默认实现
	// 执行 “SET TRANSACTION READ ONLY” 语句。
	//
	// Oracle、MySQL 和 Postgres 都理解 “SET TRANSACTION READ ONLY”，也可以与其他数据库一起使用。
	// 如果您想调整这种处理方式，请相应地覆盖此方法。
	// 参形：
	//			con – 事务性 JDBC 连接
	//			定义——当前事务定义
	// 抛出：
	//			SQLException – 如果由 JDBC API 抛出
	protected void prepareTransactionalConnection(Connection con, TransactionDefinition definition)
			throws SQLException {

		if (isEnforceReadOnly() && definition.isReadOnly()) {
			try (Statement stmt = con.createStatement()) {
				// 使用 SQL 设置为只读事务，保证读取一致性
				stmt.executeUpdate("SET TRANSACTION READ ONLY");
			}
		}
	}

	/**
	 * Translate the given JDBC commit/rollback exception to a common Spring
	 * exception to propagate from the {@link #commit}/{@link #rollback} call.
	 * <p>The default implementation throws a {@link TransactionSystemException}.
	 * Subclasses may specifically identify concurrency failures etc.
	 * @param task the task description (commit or rollback)
	 * @param ex the SQLException thrown from commit/rollback
	 * @return the translated exception to throw, either a
	 * {@link org.springframework.dao.DataAccessException} or a
	 * {@link org.springframework.transaction.TransactionException}
	 * @since 5.3
	 */
	// 将给定的 JDBC 提交/回滚异常转换为常见的 Spring 异常，以从commit / rollback调用传播。
	// 默认实现抛出 TransactionSystemException 。子类可以专门识别并发故障等。
	// 参形：
	//			task – 任务描述（提交或回滚）
	//			ex – 从提交/回滚中抛出的 SQLException
	// 返回值：
	//			要抛出的翻译异常，要么是 org.springframework.dao.DataAccessException
	//			要么是 org.springframework.transaction.TransactionException
	protected RuntimeException translateException(String task, SQLException ex) {
		return new TransactionSystemException(task + " failed", ex);
	}


	/**
	 * DataSource transaction object, representing a ConnectionHolder.
	 * Used as transaction object by DataSourceTransactionManager.
	 */
	// DataSource 事务对象，代表一个 ConnectionHolder。由 DataSourceTransactionManager 用作事务对象。
	private static class DataSourceTransactionObject extends JdbcTransactionObjectSupport {

		// 是否新的连接持有者
		private boolean newConnectionHolder;

		// 是否必须重置自动提交
		private boolean mustRestoreAutoCommit;

		// 设置连接持有者
		public void setConnectionHolder(@Nullable ConnectionHolder connectionHolder, boolean newConnectionHolder) {
			super.setConnectionHolder(connectionHolder);
			this.newConnectionHolder = newConnectionHolder;
		}

		// 判断是不是新的连接持有者
		public boolean isNewConnectionHolder() {
			return this.newConnectionHolder;
		}

		// 设置必须重置自动提交
		public void setMustRestoreAutoCommit(boolean mustRestoreAutoCommit) {
			this.mustRestoreAutoCommit = mustRestoreAutoCommit;
		}

		// 判断是否必须重置自动提交
		public boolean isMustRestoreAutoCommit() {
			return this.mustRestoreAutoCommit;
		}

		// 设置只回归
		public void setRollbackOnly() {
			getConnectionHolder().setRollbackOnly();
		}

		// 判断是否只回滚
		@Override
		public boolean isRollbackOnly() {
			return getConnectionHolder().isRollbackOnly();
		}

		// 刷新
		@Override
		public void flush() {
			if (TransactionSynchronizationManager.isSynchronizationActive()) {
				TransactionSynchronizationUtils.triggerFlush();
			}
		}
	}

}
