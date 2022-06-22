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

/**
 * Common representation of the current state of a transaction.
 * Serves as base interface for {@link TransactionStatus} as well as
 * {@link ReactiveTransaction}.
 *
 * @author Juergen Hoeller
 * @since 5.2
 */
// 事务当前状态的通用表示。作为 TransactionStatus 和 ReactiveTransaction 的基本接口。
public interface TransactionExecution {

	/**
	 * Return whether the present transaction is new; otherwise participating
	 * in an existing transaction, or potentially not running in an actual
	 * transaction in the first place.
	 */
	// 返回当前事务是否是新的；以其他方式参与现有事务，或者可能一开始就没有在实际事务中运行。
	// 如果传播级别是 NEW 的话，每一次调用 Transactional 的方法的时候，它都是一个新的事务
	boolean isNewTransaction();

	/**
	 * Set the transaction rollback-only. This instructs the transaction manager
	 * that the only possible outcome of the transaction may be a rollback, as
	 * alternative to throwing an exception which would in turn trigger a rollback.
	 */
	// 仅设当前置事务回滚。这指示事务管理器事务的唯一可能结果可能是回滚，作为抛出异常的替代方案，而异常又会触发回滚。
	void setRollbackOnly();

	/**
	 * Return whether the transaction has been marked as rollback-only
	 * (either by the application or by the transaction infrastructure).
	 */
	// 返回事务是否已被标记为仅回滚（由应用程序或事务基础结构）
	boolean isRollbackOnly();

	/**
	 * Return whether this transaction is completed, that is,
	 * whether it has already been committed or rolled back.
	 */
	// 返回此事务是否完成，即是否已经提交或回滚。
	boolean isCompleted();

}
