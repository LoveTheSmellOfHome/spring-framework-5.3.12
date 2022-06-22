/*
 * Copyright 2002-2021 the original author or authors.
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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.springframework.lang.Nullable;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Proxy for a target JDBC {@link javax.sql.DataSource}, adding awareness of
 * Spring-managed transactions. Similar to a transactional JNDI DataSource
 * as provided by a Java EE server.
 *
 * <p>Data access code that should remain unaware of Spring's data access support
 * can work with this proxy to seamlessly participate in Spring-managed transactions.
 * Note that the transaction manager, for example {@link DataSourceTransactionManager},
 * still needs to work with the underlying DataSource, <i>not</i> with this proxy.
 *
 * <p><b>Make sure that TransactionAwareDataSourceProxy is the outermost DataSource
 * of a chain of DataSource proxies/adapters.</b> TransactionAwareDataSourceProxy
 * can delegate either directly to the target connection pool or to some
 * intermediary proxy/adapter like {@link LazyConnectionDataSourceProxy} or
 * {@link UserCredentialsDataSourceAdapter}.
 *
 * <p>Delegates to {@link DataSourceUtils} for automatically participating in
 * thread-bound transactions, for example managed by {@link DataSourceTransactionManager}.
 * {@code getConnection} calls and {@code close} calls on returned Connections
 * will behave properly within a transaction, i.e. always operate on the transactional
 * Connection. If not within a transaction, normal DataSource behavior applies.
 *
 * <p>This proxy allows data access code to work with the plain JDBC API and still
 * participate in Spring-managed transactions, similar to JDBC code in a Java EE/JTA
 * environment. However, if possible, use Spring's DataSourceUtils, JdbcTemplate or
 * JDBC operation objects to get transaction participation even without a proxy for
 * the target DataSource, avoiding the need to define such a proxy in the first place.
 *
 * <p>As a further effect, using a transaction-aware DataSource will apply remaining
 * transaction timeouts to all created JDBC (Prepared/Callable)Statement. This means
 * that all operations performed through standard JDBC will automatically participate
 * in Spring-managed transaction timeouts.
 *
 * <p><b>NOTE:</b> This DataSource proxy needs to return wrapped Connections (which
 * implement the {@link ConnectionProxy} interface) in order to handle close calls
 * properly. Use {@link Connection#unwrap} to retrieve the native JDBC Connection.
 *
 * @author Juergen Hoeller
 * @since 1.1
 * @see javax.sql.DataSource#getConnection()
 * @see java.sql.Connection#close()
 * @see DataSourceUtils#doGetConnection
 * @see DataSourceUtils#applyTransactionTimeout
 * @see DataSourceUtils#doReleaseConnection
 */
// 目标 JDBC DataSource的代理，增加了 Spring 管理事务的意识。类似于 Java EE 服务器提供的事务性 JNDI 数据源。
//
// 应该不知道 Spring 的数据访问支持的数据访问代码可以与此代理一起使用，以无缝参与 Spring 管理的事务。请注意，
// 事务管理器（例如 DataSourceTransactionManager ）仍需要使用底层 DataSource，而不是使用此代理。
//
// 确保 TransactionAwareDataSourceProxy 是数据源代理/适配器链的最外层数据源。 TransactionAwareDataSourceProxy 可以
// 直接委托给目标连接池，也可以委托给一些中间代理/适配器，如 LazyConnectionDataSourceProxy 或 UserCredentialsDataSourceAdapter 。
//
// 委托给 DataSourceUtils 以自动参与线程绑定事务，例如由 DataSourceTransactionManager 管理。
// getConnection 调用和对返回的 Connection 的 close 调用将在事务中正常运行，即始终在事务连接上操作。
// 如果不在事务中，则应用正常的 DataSource 行为。
//
// 这个代理允许数据访问代码使用普通的 JDBC API 并且仍然参与 Spring 管理的事务，类似于 Java EE/JTA 环境
// 中的 JDBC 代码。但是，如果可能，即使没有目标 DataSource 的代理，也可以使用 Spring 的
// DataSourceUtils、JdbcTemplate 或 JDBC 操作对象来获得事务参与，从而避免首先定义这样的代理。
//
// 作为进一步的效果，使用事务感知数据源会将剩余事务超时应用于所有创建的 JDBC（准备/可调用）语句。这意味着通过
// 标准 JDBC 执行的所有操作都将自动参与 Spring 管理的事务超时。
//
// 注意：此 DataSource 代理需要返回已包装的 Connections（实现 ConnectionProxy 接口）才能正确处理
// 关闭调用。使用 Connection.unwrap 检索本机 JDBC 连接。
public class TransactionAwareDataSourceProxy extends DelegatingDataSource {

	// 是否重新获取事务连接
	private boolean reobtainTransactionalConnections = false;


	/**
	 * Create a new TransactionAwareDataSourceProxy.
	 * @see #setTargetDataSource
	 */
	// 创建一个新的 TransactionAwareDataSourceProxy。
	public TransactionAwareDataSourceProxy() {
	}

	/**
	 * Create a new TransactionAwareDataSourceProxy.
	 * @param targetDataSource the target DataSource
	 */
	// 创建一个新的 TransactionAwareDataSourceProxy。
	// 形参: targetDataSource – the target DataSource
	public TransactionAwareDataSourceProxy(DataSource targetDataSource) {
		super(targetDataSource);
	}

	/**
	 * Specify whether to reobtain the target Connection for each operation
	 * performed within a transaction.
	 * <p>The default is "false". Specify "true" to reobtain transactional
	 * Connections for every call on the Connection proxy; this is advisable
	 * on JBoss if you hold on to a Connection handle across transaction boundaries.
	 * <p>The effect of this setting is similar to the
	 * "hibernate.connection.release_mode" value "after_statement".
	 */
	// 指定是否为事务中执行的每个操作重新获取目标连接。
	//
	// 默认值为“假”。指定 “true” 为连接代理上的每个调用重新获取事务连接；如果您跨事务边界保持连接句柄，这在 JBoss 上是可取的。
	//
	// 此设置的效果类似于 “hibernate.connection.release_mode” 值 “after_statement”
	public void setReobtainTransactionalConnections(boolean reobtainTransactionalConnections) {
		this.reobtainTransactionalConnections = reobtainTransactionalConnections;
	}


	/**
	 * Delegates to DataSourceUtils for automatically participating in Spring-managed
	 * transactions. Throws the original SQLException, if any.
	 * <p>The returned Connection handle implements the ConnectionProxy interface,
	 * allowing to retrieve the underlying target Connection.
	 * @return a transactional Connection if any, a new one else
	 * @see DataSourceUtils#doGetConnection
	 * @see ConnectionProxy#getTargetConnection
	 */
	// 委托给 DataSourceUtils 以自动参与 Spring 管理的事务。抛出原始 SQLException（如果有）。
	// 返回的 Connection 句柄实现 ConnectionProxy 接口，允许检索底层目标 Connection。
	// 返回值：
	//			一个事务连接（如果有），一个新的
	// 请参阅：
	//			DataSourceUtils.doGetConnection , ConnectionProxy.getTargetConnection
	@Override
	public Connection getConnection() throws SQLException {
		return getTransactionAwareConnectionProxy(obtainTargetDataSource());
	}

	/**
	 * Wraps the given Connection with a proxy that delegates every method call to it
	 * but delegates {@code close()} calls to DataSourceUtils.
	 * @param targetDataSource the DataSource that the Connection came from
	 * @return the wrapped Connection
	 * @see java.sql.Connection#close()
	 * @see DataSourceUtils#doReleaseConnection
	 */
	// 使用代理包装给定的 Connection，该代理将每个方法调用委托给它，但将 close() 调用委托给 DataSourceUtils。
	// 参形：
	//			targetDataSource - 连接来自的数据源
	// 返回值：
	//			包裹的连接
	protected Connection getTransactionAwareConnectionProxy(DataSource targetDataSource) {
		return (Connection) Proxy.newProxyInstance(
				ConnectionProxy.class.getClassLoader(),
				new Class<?>[] {ConnectionProxy.class},
				new TransactionAwareInvocationHandler(targetDataSource));
	}

	/**
	 * Determine whether to obtain a fixed target Connection for the proxy
	 * or to reobtain the target Connection for each operation.
	 * <p>The default implementation returns {@code true} for all
	 * standard cases. This can be overridden through the
	 * {@link #setReobtainTransactionalConnections "reobtainTransactionalConnections"}
	 * flag, which enforces a non-fixed target Connection within an active transaction.
	 * Note that non-transactional access will always use a fixed Connection.
	 * @param targetDataSource the target DataSource
	 */
	// 确定是为代理获取一个固定的目标Connection，还是为每个操作重新获取目标Connection。
	//
	// 对于所有标准情况，默认实现返回true 。这可以通过 "reobtainTransactionalConnections"
	// 标志覆盖，该标志在活动事务中强制执行非固定目标连接。请注意，非事务性访问将始终使用固定连接。
	// 参形：
	//				targetDataSource – 目标数据源
	protected boolean shouldObtainFixedConnection(DataSource targetDataSource) {
		return (!TransactionSynchronizationManager.isSynchronizationActive() ||
				!this.reobtainTransactionalConnections);
	}


	/**
	 * Invocation handler that delegates close calls on JDBC Connections
	 * to DataSourceUtils for being aware of thread-bound transactions.
	 */
	// 调用处理程序，将 JDBC 连接上的关闭调用委托给 DataSourceUtils，以了解线程绑定事务,使用 JDK 动态代理
	private class TransactionAwareInvocationHandler implements InvocationHandler {

		// 数据源
		private final DataSource targetDataSource;

		// Connection
		@Nullable
		private Connection target;

		// 是否关闭，默认 否
		private boolean closed = false;

		// 关联外部数据源
		public TransactionAwareInvocationHandler(DataSource targetDataSource) {
			this.targetDataSource = targetDataSource;
		}

		@Override
		@Nullable
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			// Invocation on ConnectionProxy interface coming in...
			// 对 ConnectionProxy 接口的调用传入...

			switch (method.getName()) {
				case "equals":
					// Only considered as equal when proxies are identical.
					// 仅当代理相同时才被视为相等。
					return (proxy == args[0]);
				case "hashCode":
					// Use hashCode of Connection proxy.
					// 使用连接代理的 hashCode
					return System.identityHashCode(proxy);
				case "toString":
					// Allow for differentiating between the proxy and the raw Connection.
					// 允许区分代理和原始连接
					StringBuilder sb = new StringBuilder("Transaction-aware proxy for target Connection ");
					if (this.target != null) {
						sb.append('[').append(this.target.toString()).append(']');
					}
					else {
						sb.append(" from DataSource [").append(this.targetDataSource).append(']');
					}
					return sb.toString();
				case "close":
					// Handle close method: only close if not within a transaction.
					// 处理关闭方法：仅当不在事务中时才关闭
					DataSourceUtils.doReleaseConnection(this.target, this.targetDataSource);
					this.closed = true;
					return null;
				case "isClosed":
					return this.closed;
				case "unwrap":
					if (((Class<?>) args[0]).isInstance(proxy)) {
						return proxy;
					}
					break;
				case "isWrapperFor":
					if (((Class<?>) args[0]).isInstance(proxy)) {
						return true;
					}
					break;
			}

			if (this.target == null) {
				if (method.getName().equals("getWarnings") || method.getName().equals("clearWarnings")) {
					// Avoid creation of target Connection on pre-close cleanup (e.g. Hibernate Session)
					// 避免在关闭前清理时创建目标连接（例如休眠会话）
					return null;
				}
				if (this.closed) {
					throw new SQLException("Connection handle already closed");
				}
				// 是否可以获取目标数据源自动连接
				if (shouldObtainFixedConnection(this.targetDataSource)) {
					this.target = DataSourceUtils.doGetConnection(this.targetDataSource);
				}
			}
			Connection actualTarget = this.target;
			if (actualTarget == null) {
				actualTarget = DataSourceUtils.doGetConnection(this.targetDataSource);
			}

			if (method.getName().equals("getTargetConnection")) {
				// Handle getTargetConnection method: return underlying Connection.
				// 处理 getTargetConnection 方法：返回底层 Connection。
				return actualTarget;
			}

			// Invoke method on target Connection.
			// 在目标连接上调用方法。
			try {
				Object retVal = method.invoke(actualTarget, args);

				// If return value is a Statement, apply transaction timeout.
				// Applies to createStatement, prepareStatement, prepareCall.
				// 如果返回值为 Statement，则应用事务超时。适用于 createStatement、prepareStatement、prepareCall。
				if (retVal instanceof Statement) {
					DataSourceUtils.applyTransactionTimeout((Statement) retVal, this.targetDataSource);
				}

				return retVal;
			}
			catch (InvocationTargetException ex) {
				throw ex.getTargetException();
			}
			finally {
				if (actualTarget != this.target) {
					DataSourceUtils.doReleaseConnection(actualTarget, this.targetDataSource);
				}
			}
		}
	}

}
