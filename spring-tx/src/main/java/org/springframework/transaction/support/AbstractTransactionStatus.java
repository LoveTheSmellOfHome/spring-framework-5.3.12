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

package org.springframework.transaction.support;

import org.springframework.lang.Nullable;
import org.springframework.transaction.NestedTransactionNotSupportedException;
import org.springframework.transaction.SavepointManager;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.TransactionUsageException;

/**
 * Abstract base implementation of the
 * {@link org.springframework.transaction.TransactionStatus} interface.
 *
 * <p>Pre-implements the handling of local rollback-only and completed flags, and
 * delegation to an underlying {@link org.springframework.transaction.SavepointManager}.
 * Also offers the option of a holding a savepoint within the transaction.
 *
 * <p>Does not assume any specific internal transaction handling, such as an
 * underlying transaction object, and no transaction synchronization mechanism.
 *
 * @author Juergen Hoeller
 * @since 1.2.3
 * @see #setRollbackOnly()
 * @see #isRollbackOnly()
 * @see #setCompleted()
 * @see #isCompleted()
 * @see #getSavepointManager()
 * @see SimpleTransactionStatus
 * @see DefaultTransactionStatus
 */
// TransactionStatus 接口的抽象基本实现。
//
// 预先实现对本地回滚和已完成标志的处理，以及对底层 SavepointManager 的委派。还提供了在事务中保存保存点的选项。
//
// 不假设任何特定的内部事务处理，例如底层事务对象，也没有事务同步机制
public abstract class AbstractTransactionStatus implements TransactionStatus {

	// 是否只回滚，默认否
	private boolean rollbackOnly = false;

	// 是否已完成，默认否
	private boolean completed = false;

	// 保存点
	@Nullable
	private Object savepoint;


	//---------------------------------------------------------------------
	// Implementation of TransactionExecution
	// 事务执行的实现
	//---------------------------------------------------------------------

	@Override
	public void setRollbackOnly() {
		this.rollbackOnly = true;
	}

	/**
	 * Determine the rollback-only flag via checking both the local rollback-only flag
	 * of this TransactionStatus and the global rollback-only flag of the underlying
	 * transaction, if any.
	 * @see #isLocalRollbackOnly()
	 * @see #isGlobalRollbackOnly()
	 */
	// 通过检查此 TransactionStatus 的本地仅回滚标志和基础事务的全局仅回滚标志（如果有）来确定仅回滚标志。
	@Override
	public boolean isRollbackOnly() {
		return (isLocalRollbackOnly() || isGlobalRollbackOnly());
	}

	/**
	 * Determine the rollback-only flag via checking this TransactionStatus.
	 * <p>Will only return "true" if the application called {@code setRollbackOnly}
	 * on this TransactionStatus object.
	 */
	// 通过检查此 TransactionStatus 确定仅回滚标志。
	// 仅当应用程序对此 TransactionStatus 对象调用 setRollbackOnly 时才会返回 “true”。
	public boolean isLocalRollbackOnly() {
		return this.rollbackOnly;
	}

	/**
	 * Template method for determining the global rollback-only flag of the
	 * underlying transaction, if any.
	 * <p>This implementation always returns {@code false}.
	 */
	// 用于确定基础事务的全局仅回滚标志的模板方法（如果有）。
	// 此实现始终返回 false 。
	public boolean isGlobalRollbackOnly() {
		return false;
	}

	/**
	 * Mark this transaction as completed, that is, committed or rolled back.
	 */
	// 将此事务标记为已完成，即已提交或回滚。
	public void setCompleted() {
		this.completed = true;
	}

	@Override
	public boolean isCompleted() {
		return this.completed;
	}


	//---------------------------------------------------------------------
	// Handling of current savepoint state
	// 当前保存点状态的处理
	//---------------------------------------------------------------------

	@Override
	public boolean hasSavepoint() {
		return (this.savepoint != null);
	}

	/**
	 * Set a savepoint for this transaction. Useful for PROPAGATION_NESTED.
	 * @see org.springframework.transaction.TransactionDefinition#PROPAGATION_NESTED
	 */
	// 为此事务设置保存点。对于 PROPAGATION_NESTED 很有用。
	protected void setSavepoint(@Nullable Object savepoint) {
		this.savepoint = savepoint;
	}

	/**
	 * Get the savepoint for this transaction, if any.
	 */
	// 获取此事务的保存点（如果有）
	@Nullable
	protected Object getSavepoint() {
		return this.savepoint;
	}

	/**
	 * Create a savepoint and hold it for the transaction.
	 * @throws org.springframework.transaction.NestedTransactionNotSupportedException
	 * if the underlying transaction does not support savepoints
	 */
	// 创建一个保存点并为事务保存它。
	// 抛出：
	//			NestedTransactionNotSupportedException – 如果底层事务不支持保存点
	public void createAndHoldSavepoint() throws TransactionException {
		setSavepoint(getSavepointManager().createSavepoint());
	}

	/**
	 * Roll back to the savepoint that is held for the transaction
	 * and release the savepoint right afterwards.
	 */
	// 回滚到为事务保留的保存点，然后立即释放保存点。
	public void rollbackToHeldSavepoint() throws TransactionException {
		Object savepoint = getSavepoint();
		if (savepoint == null) {
			throw new TransactionUsageException(
					"Cannot roll back to savepoint - no savepoint associated with current transaction");
		}
		// 回滚到事务保存点
		getSavepointManager().rollbackToSavepoint(savepoint);
		// 释放保存点
		getSavepointManager().releaseSavepoint(savepoint);
		// 设置保存点为 null
		setSavepoint(null);
	}

	/**
	 * Release the savepoint that is held for the transaction.
	 */
	// 释放为事务保留的保存点
	public void releaseHeldSavepoint() throws TransactionException {
		Object savepoint = getSavepoint();
		if (savepoint == null) {
			throw new TransactionUsageException(
					"Cannot release savepoint - no savepoint associated with current transaction");
		}
		getSavepointManager().releaseSavepoint(savepoint);
		setSavepoint(null);
	}


	//---------------------------------------------------------------------
	// Implementation of SavepointManager
	// SavepointManager 的实现
	//---------------------------------------------------------------------

	/**
	 * This implementation delegates to a SavepointManager for the
	 * underlying transaction, if possible.
	 * @see #getSavepointManager()
	 * @see SavepointManager#createSavepoint()
	 */
	// 如果可能，此实现委托给底层事务的 SavepointManager。
	@Override
	public Object createSavepoint() throws TransactionException {
		return getSavepointManager().createSavepoint();
	}

	/**
	 * This implementation delegates to a SavepointManager for the
	 * underlying transaction, if possible.
	 * @see #getSavepointManager()
	 * @see SavepointManager#rollbackToSavepoint(Object)
	 */
	// 如果可能，此实现委托给底层事务的 SavepointManager。
	@Override
	public void rollbackToSavepoint(Object savepoint) throws TransactionException {
		getSavepointManager().rollbackToSavepoint(savepoint);
	}

	/**
	 * This implementation delegates to a SavepointManager for the
	 * underlying transaction, if possible.
	 * @see #getSavepointManager()
	 * @see SavepointManager#releaseSavepoint(Object)
	 */
	// 如果可能，此实现委托给底层事务的 SavepointManager。
	@Override
	public void releaseSavepoint(Object savepoint) throws TransactionException {
		getSavepointManager().releaseSavepoint(savepoint);
	}

	/**
	 * Return a SavepointManager for the underlying transaction, if possible.
	 * <p>Default implementation always throws a NestedTransactionNotSupportedException.
	 * @throws org.springframework.transaction.NestedTransactionNotSupportedException
	 * if the underlying transaction does not support savepoints
	 */
	// 如果可能，返回底层事务的 SavepointManager。
	// 默认实现总是抛出 NestedTransactionNotSupportedException。
	protected SavepointManager getSavepointManager() {
		throw new NestedTransactionNotSupportedException("This transaction does not support savepoints");
	}


	//---------------------------------------------------------------------
	// Flushing support
	//---------------------------------------------------------------------

	/**
	 * This implementations is empty, considering flush as a no-op.
	 */
	// 此实现是空的，将刷新视为无操作。
	@Override
	public void flush() {
	}

}
