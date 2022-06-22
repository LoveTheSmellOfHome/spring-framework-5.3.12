/*
 * Copyright 2002-2014 the original author or authors.
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
 * Interface that specifies an API to programmatically manage transaction
 * savepoints in a generic fashion. Extended by TransactionStatus to
 * expose savepoint management functionality for a specific transaction.
 *
 * <p>Note that savepoints can only work within an active transaction.
 * Just use this programmatic savepoint handling for advanced needs;
 * else, a subtransaction with PROPAGATION_NESTED is preferable.
 *
 * <p>This interface is inspired by JDBC 3.0's Savepoint mechanism
 * but is independent from any specific persistence technology.
 *
 * @author Juergen Hoeller
 * @since 1.1
 * @see TransactionStatus
 * @see TransactionDefinition#PROPAGATION_NESTED
 * @see java.sql.Savepoint
 */
// 指定 API 以以通用方式以编程方式管理事务保护点的接口。由 TransactionStatus 扩展以公开特定事务的保护点管理功能。
//
// 请注意，保护点只能在活动事务中工作。只需将此程序化保护点处理用于高级需求；否则，最好使用
// 带有 PROPAGATION_NESTED 的子事务。
//
// 该接口受 JDBC 3.0 的 Savepoint 机制的启发，但独立于任何特定的持久性技术
//
// 和 JDBC 里的 ConnectionSavepoint 其实是一样的，比如在一个大的事务中，有很多小的处理方法。你不需要全局回滚，
// a 方法调用 b 方法的时候，b 方法发生了回滚。按照默认情况下 a 外面的事务也会回滚。因为它两在同一个事务中。但是如果是
// 嵌套事务，a 调用 b 的时候，b 里边有一个保护点。它可以有选择性的回滚到保护点里去。相当于小范围回滚，外面的 a 这个方法
// 外部的事务不会被回滚。就是我们所了解的嵌入型事务。释放保护点：即你的事务无论是 commit 还是 rollback,最后都可以释放保护点。
// 这其实就是数据库的一种实现机制，在局部形成一种沙箱，让错误不会外溢。
public interface SavepointManager {

	/**
	 * Create a new savepoint. You can roll back to a specific savepoint
	 * via {@code rollbackToSavepoint}, and explicitly release a savepoint
	 * that you don't need anymore via {@code releaseSavepoint}.
	 * <p>Note that most transaction managers will automatically release
	 * savepoints at transaction completion.
	 * @return a savepoint object, to be passed into
	 * {@link #rollbackToSavepoint} or {@link #releaseSavepoint}
	 * @throws NestedTransactionNotSupportedException if the underlying
	 * transaction does not support savepoints
	 * @throws TransactionException if the savepoint could not be created,
	 * for example because the transaction is not in an appropriate state
	 * @see java.sql.Connection#setSavepoint
	 */
	// 创建一个新的保护点。您可以通过rollbackToSavepoint到特定的保护点，并通过 releaseSavepoint 显式释
	// 放您不再需要的保护点。
	// 
	// 请注意，大多数事务管理器将在事务完成时自动释放保护点。
	//
	// 返回值：
	//			一个保护点对象，被传递到rollbackToSavepoint或releaseSavepoint
	// 抛出：
	//			NestedTransactionNotSupportedException – 如果底层事务不支持保护点
	//			TransactionException – 如果无法创建保护点，例如因为事务未处于适当状态
	Object createSavepoint() throws TransactionException;

	/**
	 * Roll back to the given savepoint.
	 * <p>The savepoint will <i>not</i> be automatically released afterwards.
	 * You may explicitly call {@link #releaseSavepoint(Object)} or rely on
	 * automatic release on transaction completion.
	 * @param savepoint the savepoint to roll back to
	 * @throws NestedTransactionNotSupportedException if the underlying
	 * transaction does not support savepoints
	 * @throws TransactionException if the rollback failed
	 * @see java.sql.Connection#rollback(java.sql.Savepoint)
	 */
	// 回滚到给定的保护点。
	// 保护点之后不会自动释放。您可以显式调用releaseSavepoint(Object)或依赖事务完成时的自动释放。
	// 参形：
	//			savepoint - 要回滚到的保护点
	// 抛出：
	//			NestedTransactionNotSupportedException – 如果底层事务不支持保护点
	//			TransactionException – 如果回滚失败
	void rollbackToSavepoint(Object savepoint) throws TransactionException;

	/**
	 * Explicitly release the given savepoint.
	 * <p>Note that most transaction managers will automatically release
	 * savepoints on transaction completion.
	 * <p>Implementations should fail as silently as possible if proper
	 * resource cleanup will eventually happen at transaction completion.
	 * @param savepoint the savepoint to release
	 * @throws NestedTransactionNotSupportedException if the underlying
	 * transaction does not support savepoints
	 * @throws TransactionException if the release failed
	 * @see java.sql.Connection#releaseSavepoint
	 */
	// 显式释放给定的保护点。
	// 请注意，大多数事务管理器将在事务完成时自动释放保护点。
	// 如果适当的资源清理最终会在事务完成时发生，那么实现应该尽可能静默地失败。
	// 参形：
	//			savepoint – 要释放的保护点
	// 抛出：
	//			NestedTransactionNotSupportedException – 如果底层事务不支持保护点
	//			TransactionException – 如果发布失败
	void releaseSavepoint(Object savepoint) throws TransactionException;

}
