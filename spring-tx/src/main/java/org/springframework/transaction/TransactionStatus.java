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

import java.io.Flushable;

/**
 * Representation of the status of a transaction.
 *
 * <p>Transactional code can use this to retrieve status information,
 * and to programmatically request a rollback (instead of throwing
 * an exception that causes an implicit rollback).
 *
 * <p>Includes the {@link SavepointManager} interface to provide access
 * to savepoint management facilities. Note that savepoint management
 * is only available if supported by the underlying transaction manager.
 *
 * @author Juergen Hoeller
 * @since 27.03.2003
 * @see #setRollbackOnly()
 * @see PlatformTransactionManager#getTransaction
 * @see org.springframework.transaction.support.TransactionCallback#doInTransaction
 * @see org.springframework.transaction.interceptor.TransactionInterceptor#currentTransactionStatus()
 */
// 事务状态：通过 API 的方式来管理我们的事务，这里它就涉及到保护点，保护点是 JDBC API 里面有一个 Saveingpoing API
//
// 事务代码可以使用它来检索状态信息，并以编程方式请求回滚（而不是抛出导致隐式回滚的异常）。
//
// 包括 SavepointManager 接口以提供对保存点管理设施的访问。请注意，保存点管理仅在底层事务管理器支持时才可用
//
// Spring 事务状态
public interface TransactionStatus extends TransactionExecution, SavepointManager, Flushable {

	/**
	 * Return whether this transaction internally carries a savepoint,
	 * that is, has been created as nested transaction based on a savepoint.
	 * <p>This method is mainly here for diagnostic purposes, alongside
	 * {@link #isNewTransaction()}. For programmatic handling of custom
	 * savepoints, use the operations provided by {@link SavepointManager}.
	 * @see #isNewTransaction()
	 * @see #createSavepoint()
	 * @see #rollbackToSavepoint(Object)
	 * @see #releaseSavepoint(Object)
	 */
	// 返回此事务内部是否带有保存点，即是否已创建为基于保存点的嵌套事务。
	//
	// 此方法主要用于诊断目的，与 isNewTransaction() 一起使用。对于自定义保存点的编程处理，
	// 请使用 SavepointManager提供的操作。
	boolean hasSavepoint();

	/**
	 * Flush the underlying session to the datastore, if applicable:
	 * for example, all affected Hibernate/JPA sessions.
	 * <p>This is effectively just a hint and may be a no-op if the underlying
	 * transaction manager does not have a flush concept. A flush signal may
	 * get applied to the primary resource or to transaction synchronizations,
	 * depending on the underlying resource.
	 */
	// 将底层会话刷新到数据存储区（如果适用）：例如，所有受影响的 HibernateJPA 会话。这实际上只是一个提示，
	// 如果底层事务管理器没有刷新概念，则可能是无操作的。根据底层资源，刷新信号可能会应用于主要资源或事务同步。
	@Override
	void flush();

}
