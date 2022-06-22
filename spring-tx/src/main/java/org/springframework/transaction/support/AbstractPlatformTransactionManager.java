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

package org.springframework.transaction.support;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.Constants;
import org.springframework.lang.Nullable;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.InvalidTimeoutException;
import org.springframework.transaction.NestedTransactionNotSupportedException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.TransactionSuspensionNotSupportedException;
import org.springframework.transaction.UnexpectedRollbackException;

/**
 * Abstract base class that implements Spring's standard transaction workflow,
 * serving as basis for concrete platform transaction managers like
 * {@link org.springframework.transaction.jta.JtaTransactionManager}.
 *
 * <p>This base class provides the following workflow handling:
 * <ul>
 * <li>determines if there is an existing transaction;
 * <li>applies the appropriate propagation behavior;
 * <li>suspends and resumes transactions if necessary;
 * <li>checks the rollback-only flag on commit;
 * <li>applies the appropriate modification on rollback
 * (actual rollback or setting rollback-only);
 * <li>triggers registered synchronization callbacks
 * (if transaction synchronization is active).
 * </ul>
 *
 * <p>Subclasses have to implement specific template methods for specific
 * states of a transaction, e.g.: begin, suspend, resume, commit, rollback.
 * The most important of them are abstract and must be provided by a concrete
 * implementation; for the rest, defaults are provided, so overriding is optional.
 *
 * <p>Transaction synchronization is a generic mechanism for registering callbacks
 * that get invoked at transaction completion time. This is mainly used internally
 * by the data access support classes for JDBC, Hibernate, JPA, etc when running
 * within a JTA transaction: They register resources that are opened within the
 * transaction for closing at transaction completion time, allowing e.g. for reuse
 * of the same Hibernate Session within the transaction. The same mechanism can
 * also be leveraged for custom synchronization needs in an application.
 *
 * <p>The state of this class is serializable, to allow for serializing the
 * transaction strategy along with proxies that carry a transaction interceptor.
 * It is up to subclasses if they wish to make their state to be serializable too.
 * They should implement the {@code java.io.Serializable} marker interface in
 * that case, and potentially a private {@code readObject()} method (according
 * to Java serialization rules) if they need to restore any transient state.
 *
 * @author Juergen Hoeller
 * @since 28.03.2003
 * @see #setTransactionSynchronization
 * @see TransactionSynchronizationManager
 * @see org.springframework.transaction.jta.JtaTransactionManager
 */
// 实现 Spring 标准事务工作流的抽象基类，作为具体平台事务管理器（如org.springframework.transaction.jta.
// JtaTransactionManager ）的基础。
//
// 此基类提供以下工作流处理：
//  >确定是否存在现有事务；
//  >应用适当的传播行为；
//  >必要时暂停和恢复事务；
//  >在提交时检查仅回滚标志；
//  >对回滚应用适当的修改（实际回滚或设置仅回滚）；
//  >触发注册的同步回调（如果事务同步处于活动状态）。
//
// 子类必须为事务的特定状态实现特定的模板方法，例如：开始、暂停、恢复、提交、回滚。其中最重要的是抽象的，
// 必须由具体的实现来提供；对于其余部分，提供了默认值，因此覆盖是可选的。
//
// 事务同步是一种通用机制，用于注册在事务完成时调用的回调。这主要在 JTA 事务中运行时由 JDBC、Hibernate、
// JPA 等的数据访问支持类在内部使用：它们注册在事务中打开的资源，以便在事务完成时关闭，例如，允许重用
// 相同的 Hibernate事务内的会话。同样的机制也可以用于应用程序中的自定义同步需求。
//
// 此类的状态是可序列化的，以允许序列化事务策略以及携带事务拦截器的代理。如果他们希望使他们的状态也可序列化，
// 这取决于子类。在这种情况下，他们应该实现java.io.Serializable标记接口，如果他们需要恢复任何瞬态状态，
// 则可能是私有的readObject()方法（根据 Java 序列化规则）。
//
// Spring 的事务是很复杂的，它利用到了 Spring AOP 里边的方法拦截，元信息采集，元信息解析，
// 同时它支持 JPA,JTA,EJB 这样的注解。并且支持不同的事务模型。除了数据库事务外，还有其他事务。
@SuppressWarnings("serial")
public abstract class AbstractPlatformTransactionManager implements PlatformTransactionManager, Serializable {

	/**
	 * Always activate transaction synchronization, even for "empty" transactions
	 * that result from PROPAGATION_SUPPORTS with no existing backend transaction.
	 * @see org.springframework.transaction.TransactionDefinition#PROPAGATION_SUPPORTS
	 * @see org.springframework.transaction.TransactionDefinition#PROPAGATION_NOT_SUPPORTED
	 * @see org.springframework.transaction.TransactionDefinition#PROPAGATION_NEVER
	 */
	// 始终激活事务同步，即使对于由 PROPAGATION_SUPPORTS 产生的没有现有后端事务的“空”事务也是如此。
	public static final int SYNCHRONIZATION_ALWAYS = 0;

	/**
	 * Activate transaction synchronization only for actual transactions,
	 * that is, not for empty ones that result from PROPAGATION_SUPPORTS with
	 * no existing backend transaction.
	 * @see org.springframework.transaction.TransactionDefinition#PROPAGATION_REQUIRED
	 * @see org.springframework.transaction.TransactionDefinition#PROPAGATION_MANDATORY
	 * @see org.springframework.transaction.TransactionDefinition#PROPAGATION_REQUIRES_NEW
	 */
	// 仅对实际事务激活事务同步，即，不针对由 PROPAGATION_SUPPORTS 产生的没有现有后端事务的空事务同步。
	public static final int SYNCHRONIZATION_ON_ACTUAL_TRANSACTION = 1;

	/**
	 * Never active transaction synchronization, not even for actual transactions.
	 */
	// 从不主动事务同步，甚至对于实际事务也不行。
	public static final int SYNCHRONIZATION_NEVER = 2;


	/** Constants instance for AbstractPlatformTransactionManager. */
	// AbstractPlatformTransactionManager 的常量实例
	private static final Constants constants = new Constants(AbstractPlatformTransactionManager.class);


	protected transient Log logger = LogFactory.getLog(getClass());

	private int transactionSynchronization = SYNCHRONIZATION_ALWAYS;

	private int defaultTimeout = TransactionDefinition.TIMEOUT_DEFAULT;

	// 默认不允许嵌套事务
	private boolean nestedTransactionAllowed = false;

	private boolean validateExistingTransaction = false;

	private boolean globalRollbackOnParticipationFailure = true;

	private boolean failEarlyOnGlobalRollbackOnly = false;

	private boolean rollbackOnCommitFailure = false;


	/**
	 * Set the transaction synchronization by the name of the corresponding constant
	 * in this class, e.g. "SYNCHRONIZATION_ALWAYS".
	 * @param constantName name of the constant
	 * @see #SYNCHRONIZATION_ALWAYS
	 */
	// 通过该类中对应常量的名称设置事务同步，例如 “SYNCHRONIZATION_ALWAYS”。
	// 参形：
	//			constantName – 常量的名称
	public final void setTransactionSynchronizationName(String constantName) {
		setTransactionSynchronization(constants.asNumber(constantName).intValue());
	}

	/**
	 * Set when this transaction manager should activate the thread-bound
	 * transaction synchronization support. Default is "always".
	 * <p>Note that transaction synchronization isn't supported for
	 * multiple concurrent transactions by different transaction managers.
	 * Only one transaction manager is allowed to activate it at any time.
	 * @see #SYNCHRONIZATION_ALWAYS
	 * @see #SYNCHRONIZATION_ON_ACTUAL_TRANSACTION
	 * @see #SYNCHRONIZATION_NEVER
	 * @see TransactionSynchronizationManager
	 * @see TransactionSynchronization
	 */
	// 设置此事务管理器应激活线程绑定事务同步支持的时间。默认为“始终”。
	// 请注意，不同事务管理器的多个并发事务不支持事务同步。任何时候都只允许一个事务管理器激活它。
	public final void setTransactionSynchronization(int transactionSynchronization) {
		this.transactionSynchronization = transactionSynchronization;
	}

	/**
	 * Return if this transaction manager should activate the thread-bound
	 * transaction synchronization support.
	 */
	// 如果此事务管理器应该激活线程绑定事务同步支持，则返回。
	public final int getTransactionSynchronization() {
		return this.transactionSynchronization;
	}

	/**
	 * Specify the default timeout that this transaction manager should apply
	 * if there is no timeout specified at the transaction level, in seconds.
	 * <p>Default is the underlying transaction infrastructure's default timeout,
	 * e.g. typically 30 seconds in case of a JTA provider, indicated by the
	 * {@code TransactionDefinition.TIMEOUT_DEFAULT} value.
	 * @see org.springframework.transaction.TransactionDefinition#TIMEOUT_DEFAULT
	 */
	// 如果在事务级别没有指定超时，则指定此事务管理器应应用的默认超时，以秒为单位。
	//
	// 默认是底层事务基础设施的默认超时，例如，在 JTA 提供者的情况下通常为 30 秒，
	// 由 TransactionDefinition.TIMEOUT_DEFAULT 值指示。
	// 
	// 请参阅：
	//				TransactionDefinition.TIMEOUT_DEFAULT
	public final void setDefaultTimeout(int defaultTimeout) {
		if (defaultTimeout < TransactionDefinition.TIMEOUT_DEFAULT) {
			throw new InvalidTimeoutException("Invalid default timeout", defaultTimeout);
		}
		this.defaultTimeout = defaultTimeout;
	}

	/**
	 * Return the default timeout that this transaction manager should apply
	 * if there is no timeout specified at the transaction level, in seconds.
	 * <p>Returns {@code TransactionDefinition.TIMEOUT_DEFAULT} to indicate
	 * the underlying transaction infrastructure's default timeout.
	 */
	// 如果在事务级别没有指定超时，则返回此事务管理器应应用的默认超时，以秒为单位。
	// 返回 TransactionDefinition.TIMEOUT_DEFAULT 以指示底层事务基础结构的默认超时
	public final int getDefaultTimeout() {
		return this.defaultTimeout;
	}

	/**
	 * Set whether nested transactions are allowed. Default is "false".
	 * <p>Typically initialized with an appropriate default by the
	 * concrete transaction manager subclass.
	 */
	// 设置是否允许嵌套事务。默认为“假”。
	// 通常由具体的事务管理器子类使用适当的默认值初始化。
	public final void setNestedTransactionAllowed(boolean nestedTransactionAllowed) {
		this.nestedTransactionAllowed = nestedTransactionAllowed;
	}

	/**
	 * Return whether nested transactions are allowed.
	 */
	// 返回是否允许嵌套事务
	public final boolean isNestedTransactionAllowed() {
		return this.nestedTransactionAllowed;
	}

	/**
	 * Set whether existing transactions should be validated before participating
	 * in them.
	 * <p>When participating in an existing transaction (e.g. with
	 * PROPAGATION_REQUIRED or PROPAGATION_SUPPORTS encountering an existing
	 * transaction), this outer transaction's characteristics will apply even
	 * to the inner transaction scope. Validation will detect incompatible
	 * isolation level and read-only settings on the inner transaction definition
	 * and reject participation accordingly through throwing a corresponding exception.
	 * <p>Default is "false", leniently ignoring inner transaction settings,
	 * simply overriding them with the outer transaction's characteristics.
	 * Switch this flag to "true" in order to enforce strict validation.
	 * @since 2.5.1
	 */
	// 设置是否应在参与现有事务之前对其进行验证。
	//
	// 当参与现有事务时（例如，使用 PROPAGATION_REQUIRED 或 PROPAGATION_SUPPORTS 遇到现有事务），
	// 此外部事务的特征甚至适用于内部事务范围。验证将检测内部事务定义上不兼容的隔离级别和只读设置，
	// 并通过抛出相应的异常拒绝参与。
	//
	// 默认为 “false”，忽略内部事务设置，简单地用外部事务的特征覆盖它们。将此标志切换为 “true” 以强制执行严格验证。
	public final void setValidateExistingTransaction(boolean validateExistingTransaction) {
		this.validateExistingTransaction = validateExistingTransaction;
	}

	/**
	 * Return whether existing transactions should be validated before participating
	 * in them.
	 * @since 2.5.1
	 */
	// 返回是否应在参与现有事务之前对其进行验证。
	public final boolean isValidateExistingTransaction() {
		return this.validateExistingTransaction;
	}

	/**
	 * Set whether to globally mark an existing transaction as rollback-only
	 * after a participating transaction failed.
	 * <p>Default is "true": If a participating transaction (e.g. with
	 * PROPAGATION_REQUIRED or PROPAGATION_SUPPORTS encountering an existing
	 * transaction) fails, the transaction will be globally marked as rollback-only.
	 * The only possible outcome of such a transaction is a rollback: The
	 * transaction originator <i>cannot</i> make the transaction commit anymore.
	 * <p>Switch this to "false" to let the transaction originator make the rollback
	 * decision. If a participating transaction fails with an exception, the caller
	 * can still decide to continue with a different path within the transaction.
	 * However, note that this will only work as long as all participating resources
	 * are capable of continuing towards a transaction commit even after a data access
	 * failure: This is generally not the case for a Hibernate Session, for example;
	 * neither is it for a sequence of JDBC insert/update/delete operations.
	 * <p><b>Note:</b>This flag only applies to an explicit rollback attempt for a
	 * subtransaction, typically caused by an exception thrown by a data access operation
	 * (where TransactionInterceptor will trigger a {@code PlatformTransactionManager.rollback()}
	 * call according to a rollback rule). If the flag is off, the caller can handle the exception
	 * and decide on a rollback, independent of the rollback rules of the subtransaction.
	 * This flag does, however, <i>not</i> apply to explicit {@code setRollbackOnly}
	 * calls on a {@code TransactionStatus}, which will always cause an eventual
	 * global rollback (as it might not throw an exception after the rollback-only call).
	 * <p>The recommended solution for handling failure of a subtransaction
	 * is a "nested transaction", where the global transaction can be rolled
	 * back to a savepoint taken at the beginning of the subtransaction.
	 * PROPAGATION_NESTED provides exactly those semantics; however, it will
	 * only work when nested transaction support is available. This is the case
	 * with DataSourceTransactionManager, but not with JtaTransactionManager.
	 * @see #setNestedTransactionAllowed
	 * @see org.springframework.transaction.jta.JtaTransactionManager
	 */
	// 设置是否在参与事务失败后将现有事务全局标记为仅回滚。
	// 
	// 默认为“true”：如果参与事务（例如 PROPAGATION_REQUIRED 或 PROPAGATION_SUPPORTS 遇到现有事务）失败，
	// 则该事务将被全局标记为仅回滚。这种事务唯一可能的结果是回滚：事务发起者不能再提交事务。
	// 
	// 将此切换为“false”以让事务发起者做出回滚决定。如果参与的事务因异常而失败，调用者仍然可以决定在事务中继续
	// 使用不同的路径。但是，请注意，只有在所有参与的资源即使在数据访问失败之后也能够继续进行事务提交时，这才会起
	// 作用：例如，Hibernate Session 通常不是这种情况；它也不是一系列 JDBC 插入/更新/删除操作。
	// 
	// 注意：此标志仅适用于子事务的显式回滚尝试，通常由数据访问操作引发的异常引起（其中 TransactionInterceptor 
	// 将根据回滚规则触发 PlatformTransactionManager.rollback()调用）。如果标志关闭，调用者可以处理异常并
	// 决定回滚，独立于子事务的回滚规则。但是，此标志不适用于 TransactionStatus 上的显式 setRollbackOnly 调用，
	// 这将始终导致最终的全局回滚（因为它可能不会在仅回滚调用之后引发异常）。
	// 
	// 处理子事务失败的推荐解决方案是“嵌套事务”，其中全局事务可以回滚到子事务开始时采用的保存点。 
	// PROPAGATION_NESTED 提供了这些语义；但是，它仅在嵌套事务支持可用时才有效。 
	// DataSourceTransactionManager 就是这种情况，但 JtaTransactionManager 不是这种情况。
	public final void setGlobalRollbackOnParticipationFailure(boolean globalRollbackOnParticipationFailure) {
		this.globalRollbackOnParticipationFailure = globalRollbackOnParticipationFailure;
	}

	/**
	 * Return whether to globally mark an existing transaction as rollback-only
	 * after a participating transaction failed.
	 */
	// 返回是否在参与事务失败后将现有事务全局标记为仅回滚
	public final boolean isGlobalRollbackOnParticipationFailure() {
		return this.globalRollbackOnParticipationFailure;
	}

	/**
	 * Set whether to fail early in case of the transaction being globally marked
	 * as rollback-only.
	 * <p>Default is "false", only causing an UnexpectedRollbackException at the
	 * outermost transaction boundary. Switch this flag on to cause an
	 * UnexpectedRollbackException as early as the global rollback-only marker
	 * has been first detected, even from within an inner transaction boundary.
	 * <p>Note that, as of Spring 2.0, the fail-early behavior for global
	 * rollback-only markers has been unified: All transaction managers will by
	 * default only cause UnexpectedRollbackException at the outermost transaction
	 * boundary. This allows, for example, to continue unit tests even after an
	 * operation failed and the transaction will never be completed. All transaction
	 * managers will only fail earlier if this flag has explicitly been set to "true".
	 * @since 2.0
	 * @see org.springframework.transaction.UnexpectedRollbackException
	 */
	// 设置在事务被全局标记为仅回滚的情况下是否提前失败。
	// 
	// 默认为“false”，仅在最外层事务边界导致 UnexpectedRollbackException。早在第一次检测到全局仅回滚标记时，
	// 即使在内部事务边界内，打开此标志也会导致 UnexpectedRollbackException。
	// 
	// 请注意，从 Spring 2.0 开始，全局仅回滚标记的早期失败行为已统一：默认情况下，所有事务管理器只会在最外层
	// 事务边界处引发 UnexpectedRollbackException。例如，即使在操作失败并且事务永远不会完成之后，这也允许
	// 继续进行单元测试。如果此标志已明确设置为“true”，则所有事务管理器只会提前失败。
	public final void setFailEarlyOnGlobalRollbackOnly(boolean failEarlyOnGlobalRollbackOnly) {
		this.failEarlyOnGlobalRollbackOnly = failEarlyOnGlobalRollbackOnly;
	}

	/**
	 * Return whether to fail early in case of the transaction being globally marked
	 * as rollback-only.
	 * @since 2.0
	 */
	// 如果事务被全局标记为仅回滚，则返回是否提前失败
	public final boolean isFailEarlyOnGlobalRollbackOnly() {
		return this.failEarlyOnGlobalRollbackOnly;
	}

	/**
	 * Set whether {@code doRollback} should be performed on failure of the
	 * {@code doCommit} call. Typically not necessary and thus to be avoided,
	 * as it can potentially override the commit exception with a subsequent
	 * rollback exception.
	 * <p>Default is "false".
	 * @see #doCommit
	 * @see #doRollback
	 */
	// 设置是否在doRollback调用失败时执行doCommit 。通常没有必要，因此应该避免，因为它可能会用后续的回滚
	// 异常覆盖提交异常。
	// 
	// 默认为“假”。
	public final void setRollbackOnCommitFailure(boolean rollbackOnCommitFailure) {
		this.rollbackOnCommitFailure = rollbackOnCommitFailure;
	}

	/**
	 * Return whether {@code doRollback} should be performed on failure of the
	 * {@code doCommit} call.
	 */
	// 返回是否应该在 doRollback 调用失败时执行 doCommit 。
	public final boolean isRollbackOnCommitFailure() {
		return this.rollbackOnCommitFailure;
	}


	//---------------------------------------------------------------------
	// Implementation of PlatformTransactionManager
	//---------------------------------------------------------------------

	/**
	 * This implementation handles propagation behavior. Delegates to
	 * {@code doGetTransaction}, {@code isExistingTransaction}
	 * and {@code doBegin}.
	 * @see #doGetTransaction
	 * @see #isExistingTransaction
	 * @see #doBegin
	 */
	// 此实现处理传播行为。代表 doGetTransaction 、 isExistingTransaction 和 doBegin 。
	@Override
	public final TransactionStatus getTransaction(@Nullable TransactionDefinition definition)
			throws TransactionException {

		// Use defaults if no transaction definition given.
		// 如果没有给出事务定义，则使用默认值
		TransactionDefinition def = (definition != null ? definition : TransactionDefinition.withDefaults());

		Object transaction = doGetTransaction();
		boolean debugEnabled = logger.isDebugEnabled();

		if (isExistingTransaction(transaction)) {
			// Existing transaction found -> check propagation behavior to find out how to behave.
			// 找到现有事务 -> 检查传播行为以了解行为方式。
			return handleExistingTransaction(def, transaction, debugEnabled);
		}

		// Check definition settings for new transaction.
		// 检查新事务的定义设置。
		if (def.getTimeout() < TransactionDefinition.TIMEOUT_DEFAULT) {
			throw new InvalidTimeoutException("Invalid transaction timeout", def.getTimeout());
		}

		// No existing transaction found -> check propagation behavior to find out how to proceed.
		// 未找到现有事务 -> 检查传播行为以了解如何继续。
		if (def.getPropagationBehavior() == TransactionDefinition.PROPAGATION_MANDATORY) {
			throw new IllegalTransactionStateException(
					"No existing transaction found for transaction marked with propagation 'mandatory'");
		}
		else if (def.getPropagationBehavior() == TransactionDefinition.PROPAGATION_REQUIRED ||
				def.getPropagationBehavior() == TransactionDefinition.PROPAGATION_REQUIRES_NEW ||
				def.getPropagationBehavior() == TransactionDefinition.PROPAGATION_NESTED) {
			SuspendedResourcesHolder suspendedResources = suspend(null);
			if (debugEnabled) {
				logger.debug("Creating new transaction with name [" + def.getName() + "]: " + def);
			}
			try {
				return startTransaction(def, transaction, debugEnabled, suspendedResources);
			}
			catch (RuntimeException | Error ex) {
				resume(null, suspendedResources);
				throw ex;
			}
		}
		else {
			// Create "empty" transaction: no actual transaction, but potentially synchronization.
			// 创建“空”事务：没有实际事务，但可能同步
			if (def.getIsolationLevel() != TransactionDefinition.ISOLATION_DEFAULT && logger.isWarnEnabled()) {
				logger.warn("Custom isolation level specified but no actual transaction initiated; " +
						"isolation level will effectively be ignored: " + def);
			}
			boolean newSynchronization = (getTransactionSynchronization() == SYNCHRONIZATION_ALWAYS);
			return prepareTransactionStatus(def, null, true, newSynchronization, debugEnabled, null);
		}
	}

	/**
	 * Start a new transaction.
	 */
	// 开始新的事务
	private TransactionStatus startTransaction(TransactionDefinition definition, Object transaction,
			boolean debugEnabled, @Nullable SuspendedResourcesHolder suspendedResources) {

		boolean newSynchronization = (getTransactionSynchronization() != SYNCHRONIZATION_NEVER);
		DefaultTransactionStatus status = newTransactionStatus(
				definition, transaction, true, newSynchronization, debugEnabled, suspendedResources);
		doBegin(transaction, definition);
		prepareSynchronization(status, definition);
		return status;
	}

	/**
	 * Create a TransactionStatus for an existing transaction.
	 */
	// 为现有事务创建 TransactionStatus。
	private TransactionStatus handleExistingTransaction(
			TransactionDefinition definition, Object transaction, boolean debugEnabled)
			throws TransactionException {

		if (definition.getPropagationBehavior() == TransactionDefinition.PROPAGATION_NEVER) {
			throw new IllegalTransactionStateException(
					"Existing transaction found for transaction marked with propagation 'never'");
		}

		if (definition.getPropagationBehavior() == TransactionDefinition.PROPAGATION_NOT_SUPPORTED) {
			if (debugEnabled) {
				logger.debug("Suspending current transaction");
			}
			Object suspendedResources = suspend(transaction);
			boolean newSynchronization = (getTransactionSynchronization() == SYNCHRONIZATION_ALWAYS);
			return prepareTransactionStatus(
					definition, null, false, newSynchronization, debugEnabled, suspendedResources);
		}

		if (definition.getPropagationBehavior() == TransactionDefinition.PROPAGATION_REQUIRES_NEW) {
			if (debugEnabled) {
				logger.debug("Suspending current transaction, creating new transaction with name [" +
						definition.getName() + "]");
			}
			SuspendedResourcesHolder suspendedResources = suspend(transaction);
			try {
				return startTransaction(definition, transaction, debugEnabled, suspendedResources);
			}
			catch (RuntimeException | Error beginEx) {
				resumeAfterBeginException(transaction, suspendedResources, beginEx);
				throw beginEx;
			}
		}

		if (definition.getPropagationBehavior() == TransactionDefinition.PROPAGATION_NESTED) {
			if (!isNestedTransactionAllowed()) {
				throw new NestedTransactionNotSupportedException(
						"Transaction manager does not allow nested transactions by default - " +
						"specify 'nestedTransactionAllowed' property with value 'true'");
			}
			if (debugEnabled) {
				logger.debug("Creating nested transaction with name [" + definition.getName() + "]");
			}
			if (useSavepointForNestedTransaction()) {
				// Create savepoint within existing Spring-managed transaction,
				// through the SavepointManager API implemented by TransactionStatus.
				// Usually uses JDBC 3.0 savepoints. Never activates Spring synchronization.
				// 通过 TransactionStatus 实现的 SavepointManager API 在现有 Spring 管理的事务中创建保存点。
				// 通常使用 JDBC 3.0 保存点。从不激活 Spring 同步。
				DefaultTransactionStatus status =
						prepareTransactionStatus(definition, transaction, false, false, debugEnabled, null);
				status.createAndHoldSavepoint();
				return status;
			}
			else {
				// Nested transaction through nested begin and commit/rollback calls.
				// Usually only for JTA: Spring synchronization might get activated here
				// in case of a pre-existing JTA transaction.
				//  通过嵌套的 begin 和 commitrollback 调用嵌套事务。通常仅适用于 JTA：
				//  如果存在预先存在的 JTA 事务，则可能会在此处激活 Spring 同步
				return startTransaction(definition, transaction, debugEnabled, null);
			}
		}

		// Assumably PROPAGATION_SUPPORTS or PROPAGATION_REQUIRED.
		// 大概是 PROPAGATION_SUPPORTS 或 PROPAGATION_REQUIRED。
		if (debugEnabled) {
			logger.debug("Participating in existing transaction");
		}
		if (isValidateExistingTransaction()) {
			if (definition.getIsolationLevel() != TransactionDefinition.ISOLATION_DEFAULT) {
				Integer currentIsolationLevel = TransactionSynchronizationManager.getCurrentTransactionIsolationLevel();
				if (currentIsolationLevel == null || currentIsolationLevel != definition.getIsolationLevel()) {
					Constants isoConstants = DefaultTransactionDefinition.constants;
					throw new IllegalTransactionStateException("Participating transaction with definition [" +
							definition + "] specifies isolation level which is incompatible with existing transaction: " +
							(currentIsolationLevel != null ?
									isoConstants.toCode(currentIsolationLevel, DefaultTransactionDefinition.PREFIX_ISOLATION) :
									"(unknown)"));
				}
			}
			if (!definition.isReadOnly()) {
				if (TransactionSynchronizationManager.isCurrentTransactionReadOnly()) {
					throw new IllegalTransactionStateException("Participating transaction with definition [" +
							definition + "] is not marked as read-only but existing transaction is");
				}
			}
		}
		boolean newSynchronization = (getTransactionSynchronization() != SYNCHRONIZATION_NEVER);
		return prepareTransactionStatus(definition, transaction, false, newSynchronization, debugEnabled, null);
	}

	/**
	 * Create a new TransactionStatus for the given arguments,
	 * also initializing transaction synchronization as appropriate.
	 * @see #newTransactionStatus
	 * @see #prepareTransactionStatus
	 */
	// 为给定的参数创建一个新的 TransactionStatus，同时适当地初始化事务同步。
	protected final DefaultTransactionStatus prepareTransactionStatus(
			TransactionDefinition definition, @Nullable Object transaction, boolean newTransaction,
			boolean newSynchronization, boolean debug, @Nullable Object suspendedResources) {

		DefaultTransactionStatus status = newTransactionStatus(
				definition, transaction, newTransaction, newSynchronization, debug, suspendedResources);
		prepareSynchronization(status, definition);
		return status;
	}

	/**
	 * Create a TransactionStatus instance for the given arguments.
	 */
	// 为给定的参数创建一个 TransactionStatus 实例
	protected DefaultTransactionStatus newTransactionStatus(
			TransactionDefinition definition, @Nullable Object transaction, boolean newTransaction,
			boolean newSynchronization, boolean debug, @Nullable Object suspendedResources) {

		boolean actualNewSynchronization = newSynchronization &&
				!TransactionSynchronizationManager.isSynchronizationActive();
		return new DefaultTransactionStatus(
				transaction, newTransaction, actualNewSynchronization,
				definition.isReadOnly(), debug, suspendedResources);
	}

	/**
	 * Initialize transaction synchronization as appropriate.
	 */
	// 根据需要初始化事务同步
	protected void prepareSynchronization(DefaultTransactionStatus status, TransactionDefinition definition) {
		if (status.isNewSynchronization()) {
			TransactionSynchronizationManager.setActualTransactionActive(status.hasTransaction());
			TransactionSynchronizationManager.setCurrentTransactionIsolationLevel(
					definition.getIsolationLevel() != TransactionDefinition.ISOLATION_DEFAULT ?
							definition.getIsolationLevel() : null);
			TransactionSynchronizationManager.setCurrentTransactionReadOnly(definition.isReadOnly());
			TransactionSynchronizationManager.setCurrentTransactionName(definition.getName());
			TransactionSynchronizationManager.initSynchronization();
		}
	}

	/**
	 * Determine the actual timeout to use for the given definition.
	 * Will fall back to this manager's default timeout if the
	 * transaction definition doesn't specify a non-default value.
	 * @param definition the transaction definition
	 * @return the actual timeout to use
	 * @see org.springframework.transaction.TransactionDefinition#getTimeout()
	 * @see #setDefaultTimeout
	 */
	// 确定用于给定定义的实际超时。如果事务定义未指定非默认值，将回退到此管理器的默认超时。
	// 参形：
	//			definition - 事务定义
	// 返回值：
	//			实际使用的超时时间
	protected int determineTimeout(TransactionDefinition definition) {
		if (definition.getTimeout() != TransactionDefinition.TIMEOUT_DEFAULT) {
			return definition.getTimeout();
		}
		return getDefaultTimeout();
	}


	/**
	 * Suspend the given transaction. Suspends transaction synchronization first,
	 * then delegates to the {@code doSuspend} template method.
	 * @param transaction the current transaction object
	 * (or {@code null} to just suspend active synchronizations, if any)
	 * @return an object that holds suspended resources
	 * (or {@code null} if neither transaction nor synchronization active)
	 * @see #doSuspend
	 * @see #resume
	 */
	// 暂停给定的事务。首先暂停事务同步，然后委托给 doSuspend 模板方法。
	// 参形：
	//			transaction – 当前事务对象（或null仅暂停活动同步，如果有）
	// 返回值：
	//			一个持有挂起资源的对象（如果事务和同步都没有活动，则为 null 
	@Nullable
	protected final SuspendedResourcesHolder suspend(@Nullable Object transaction) throws TransactionException {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			List<TransactionSynchronization> suspendedSynchronizations = doSuspendSynchronization();
			try {
				Object suspendedResources = null;
				if (transaction != null) {
					suspendedResources = doSuspend(transaction);
				}
				String name = TransactionSynchronizationManager.getCurrentTransactionName();
				TransactionSynchronizationManager.setCurrentTransactionName(null);
				boolean readOnly = TransactionSynchronizationManager.isCurrentTransactionReadOnly();
				TransactionSynchronizationManager.setCurrentTransactionReadOnly(false);
				Integer isolationLevel = TransactionSynchronizationManager.getCurrentTransactionIsolationLevel();
				TransactionSynchronizationManager.setCurrentTransactionIsolationLevel(null);
				boolean wasActive = TransactionSynchronizationManager.isActualTransactionActive();
				TransactionSynchronizationManager.setActualTransactionActive(false);
				return new SuspendedResourcesHolder(
						suspendedResources, suspendedSynchronizations, name, readOnly, isolationLevel, wasActive);
			}
			catch (RuntimeException | Error ex) {
				// doSuspend failed - original transaction is still active...
				// doSuspend 失败 - 原始事务仍处于活动状态...
				doResumeSynchronization(suspendedSynchronizations);
				throw ex;
			}
		}
		else if (transaction != null) {
			// Transaction active but no synchronization active.
			// 事务活动但没有同步活动。
			Object suspendedResources = doSuspend(transaction);
			return new SuspendedResourcesHolder(suspendedResources);
		}
		else {
			// Neither transaction nor synchronization active.
			// 事务和同步均未激活
			return null;
		}
	}

	/**
	 * Resume the given transaction. Delegates to the {@code doResume}
	 * template method first, then resuming transaction synchronization.
	 * @param transaction the current transaction object
	 * @param resourcesHolder the object that holds suspended resources,
	 * as returned by {@code suspend} (or {@code null} to just
	 * resume synchronizations, if any)
	 * @see #doResume
	 * @see #suspend
	 */
	// 恢复给定的事务。首先委托给doResume模板方法，然后恢复事务同步。
	// 参形：
	//				transaction - 当前事务对象
	//				resourcesHolder – 持有挂起资源的对象，由suspend返回（或null以恢复同步，如果有的话）
	protected final void resume(@Nullable Object transaction, @Nullable SuspendedResourcesHolder resourcesHolder)
			throws TransactionException {

		if (resourcesHolder != null) {
			Object suspendedResources = resourcesHolder.suspendedResources;
			if (suspendedResources != null) {
				doResume(transaction, suspendedResources);
			}
			List<TransactionSynchronization> suspendedSynchronizations = resourcesHolder.suspendedSynchronizations;
			if (suspendedSynchronizations != null) {
				TransactionSynchronizationManager.setActualTransactionActive(resourcesHolder.wasActive);
				TransactionSynchronizationManager.setCurrentTransactionIsolationLevel(resourcesHolder.isolationLevel);
				TransactionSynchronizationManager.setCurrentTransactionReadOnly(resourcesHolder.readOnly);
				TransactionSynchronizationManager.setCurrentTransactionName(resourcesHolder.name);
				doResumeSynchronization(suspendedSynchronizations);
			}
		}
	}

	/**
	 * Resume outer transaction after inner transaction begin failed.
	 */
	// 内部事务开始失败后恢复外部事务
	private void resumeAfterBeginException(
			Object transaction, @Nullable SuspendedResourcesHolder suspendedResources, Throwable beginEx) {

		try {
			resume(transaction, suspendedResources);
		}
		catch (RuntimeException | Error resumeEx) {
			String exMessage = "Inner transaction begin exception overridden by outer transaction resume exception";
			logger.error(exMessage, beginEx);
			throw resumeEx;
		}
	}

	/**
	 * Suspend all current synchronizations and deactivate transaction
	 * synchronization for the current thread.
	 * @return the List of suspended TransactionSynchronization objects
	 */
	// 暂停所有当前同步并为当前线程停用事务同步。
	// 返回值：
	//				挂起的 TransactionSynchronization 对象列表
	private List<TransactionSynchronization> doSuspendSynchronization() {
		List<TransactionSynchronization> suspendedSynchronizations =
				TransactionSynchronizationManager.getSynchronizations();
		for (TransactionSynchronization synchronization : suspendedSynchronizations) {
			synchronization.suspend();
		}
		TransactionSynchronizationManager.clearSynchronization();
		return suspendedSynchronizations;
	}

	/**
	 * Reactivate transaction synchronization for the current thread
	 * and resume all given synchronizations.
	 * @param suspendedSynchronizations a List of TransactionSynchronization objects
	 */
	// 重新激活当前线程的事务同步并恢复所有给定的同步。
	// 参形：
	//				suspendSynchronizations – TransactionSynchronization 对象列表
	private void doResumeSynchronization(List<TransactionSynchronization> suspendedSynchronizations) {
		TransactionSynchronizationManager.initSynchronization();
		for (TransactionSynchronization synchronization : suspendedSynchronizations) {
			synchronization.resume();
			TransactionSynchronizationManager.registerSynchronization(synchronization);
		}
	}


	/**
	 * This implementation of commit handles participating in existing
	 * transactions and programmatic rollback requests.
	 * Delegates to {@code isRollbackOnly}, {@code doCommit}
	 * and {@code rollback}.
	 * @see org.springframework.transaction.TransactionStatus#isRollbackOnly()
	 * @see #doCommit
	 * @see #rollback
	 */
	// 提交的这种实现处理参与现有事务和编程回滚请求。代表 isRollbackOnly 、 doCommit 和 rollback 。
	@Override
	public final void commit(TransactionStatus status) throws TransactionException {
		if (status.isCompleted()) {
			throw new IllegalTransactionStateException(
					"Transaction is already completed - do not call commit or rollback more than once per transaction");
		}

		DefaultTransactionStatus defStatus = (DefaultTransactionStatus) status;
		if (defStatus.isLocalRollbackOnly()) {
			if (defStatus.isDebug()) {
				logger.debug("Transactional code has requested rollback");
			}
			// 执行 Rollback
			processRollback(defStatus, false);
			return;
		}

		if (!shouldCommitOnGlobalRollbackOnly() && defStatus.isGlobalRollbackOnly()) {
			if (defStatus.isDebug()) {
				logger.debug("Global transaction is marked as rollback-only but transactional code requested commit");
			}
			// 执行 Rollback
			processRollback(defStatus, true);
			return;
		}

		// 执行 commit
		processCommit(defStatus);
	}

	/**
	 * Process an actual commit.
	 * Rollback-only flags have already been checked and applied.
	 * @param status object representing the transaction
	 * @throws TransactionException in case of commit failure
	 */
	// 处理实际提交。已检查并应用了仅回滚标志。
	// 参形：
	//			status – 代表事务的对象
	// 抛出：
	//			TransactionException – 在提交失败的情况下
	private void processCommit(DefaultTransactionStatus status) throws TransactionException {
		try {
			// 是否完成调用
			boolean beforeCompletionInvoked = false;

			try {
				boolean unexpectedRollback = false;
				// 准备提交
				prepareForCommit(status);
				// 触发 before Commit 回调
				triggerBeforeCommit(status);
				// 触发 before Completion 回调
				triggerBeforeCompletion(status);
				beforeCompletionInvoked = true;

				// 判断事务是否有还原点
				if (status.hasSavepoint()) {
					if (status.isDebug()) {
						logger.debug("Releasing transaction savepoint");
					}
					// 是不是只全局回滚
					unexpectedRollback = status.isGlobalRollbackOnly();
					// 释放还原点
					status.releaseHeldSavepoint();
				}
				else if (status.isNewTransaction()) { // 如果是新的事务
					if (status.isDebug()) {
						logger.debug("Initiating transaction commit");
					}
					unexpectedRollback = status.isGlobalRollbackOnly();
					// 在不同子类有不同实现，执行物理 commit，这个提交是局部的
					doCommit(status);
				}
				else if (isFailEarlyOnGlobalRollbackOnly()) {
					unexpectedRollback = status.isGlobalRollbackOnly();
				}

				// Throw UnexpectedRollbackException if we have a global rollback-only
				// marker but still didn't get a corresponding exception from commit.
				// 如果我们有一个全局回滚标记但仍然没有从提交中获得相应的异常，则抛出 UnexpectedRollbackException。
				if (unexpectedRollback) {
					throw new UnexpectedRollbackException(
							"Transaction silently rolled back because it has been marked as rollback-only");
				}
			}
			catch (UnexpectedRollbackException ex) {
				// can only be caused by doCommit
				// 只能由 doCommit 引起
				triggerAfterCompletion(status, TransactionSynchronization.STATUS_ROLLED_BACK);
				throw ex;
			}
			catch (TransactionException ex) {
				// can only be caused by doCommit
				// 只能由 doCommit 引起
				if (isRollbackOnCommitFailure()) {
					// 调用 doRollback ，正确处理回滚异常
					doRollbackOnCommitException(status, ex);
				}
				else {
					// 触发 afterCompletion 回调
					triggerAfterCompletion(status, TransactionSynchronization.STATUS_UNKNOWN);
				}
				throw ex;
			}
			catch (RuntimeException | Error ex) {
				// 异常处理
				if (!beforeCompletionInvoked) {
					// 触发 beforeCompletion 回调
					triggerBeforeCompletion(status);
				}
				// 调用 doRollback ，正确处理回滚异常
				doRollbackOnCommitException(status, ex);
				throw ex;
			}

			// Trigger afterCommit callbacks, with an exception thrown there
			// propagated to callers but the transaction still considered as committed.
			// 触发 afterCommit 回调，抛出异常并传播给调用者，但事务仍被视为已提交。
			try {
				// 触发 afterCommit 回调
				triggerAfterCommit(status);
			}
			finally {
				// 触发 afterCompletion 回调
				triggerAfterCompletion(status, TransactionSynchronization.STATUS_COMMITTED);
			}

		}
		finally {
			// 完成后清理
			cleanupAfterCompletion(status);
		}
	}

	/**
	 * This implementation of rollback handles participating in existing
	 * transactions. Delegates to {@code doRollback} and
	 * {@code doSetRollbackOnly}.
	 * @see #doRollback
	 * @see #doSetRollbackOnly
	 */
	// 这种回滚实现处理参与现有事务。代表 doRollback 和 doSetRollbackOnly 。
	@Override
	public final void rollback(TransactionStatus status) throws TransactionException {
		if (status.isCompleted()) {
			throw new IllegalTransactionStateException(
					"Transaction is already completed - do not call commit or rollback more than once per transaction");
		}

		DefaultTransactionStatus defStatus = (DefaultTransactionStatus) status;
		processRollback(defStatus, false);
	}

	/**
	 * Process an actual rollback.
	 * The completed flag has already been checked.
	 * @param status object representing the transaction
	 * @throws TransactionException in case of rollback failure
	 */
	// 处理实际的回滚。已完成标志已被检查。
	// 参形：
	//				status – 代表事务的对象
	// 抛出：
	//				TransactionException – 在回滚失败的情况下
	private void processRollback(DefaultTransactionStatus status, boolean unexpected) {
		try {
			boolean unexpectedRollback = unexpected;

			try {
				triggerBeforeCompletion(status);

				if (status.hasSavepoint()) {
					if (status.isDebug()) {
						logger.debug("Rolling back transaction to savepoint");
					}
					// 回退到保护点
					status.rollbackToHeldSavepoint();
				}
				else if (status.isNewTransaction()) {
					if (status.isDebug()) {
						logger.debug("Initiating transaction rollback");
					}
					doRollback(status);
				}
				else {
					// Participating in larger transaction
					// 参与更大的事务
					if (status.hasTransaction()) {
						if (status.isLocalRollbackOnly() || isGlobalRollbackOnParticipationFailure()) {
							if (status.isDebug()) {
								logger.debug("Participating transaction failed - marking existing transaction as rollback-only");
							}
							doSetRollbackOnly(status);
						}
						else {
							if (status.isDebug()) {
								logger.debug("Participating transaction failed - letting transaction originator decide on rollback");
							}
						}
					}
					else {
						logger.debug("Should roll back transaction but cannot - no transaction available");
					}
					// Unexpected rollback only matters here if we're asked to fail early
					// 只有当我们被要求提前失败时，意外的回滚才有意义
					if (!isFailEarlyOnGlobalRollbackOnly()) {
						unexpectedRollback = false;
					}
				}
			}
			catch (RuntimeException | Error ex) {
				triggerAfterCompletion(status, TransactionSynchronization.STATUS_UNKNOWN);
				throw ex;
			}

			triggerAfterCompletion(status, TransactionSynchronization.STATUS_ROLLED_BACK);

			// Raise UnexpectedRollbackException if we had a global rollback-only marker
			// 如果我们有一个全局回滚标记，则引发 UnexpectedRollbackException
			if (unexpectedRollback) {
				throw new UnexpectedRollbackException(
						"Transaction rolled back because it has been marked as rollback-only");
			}
		}
		finally {
			cleanupAfterCompletion(status);
		}
	}

	/**
	 * Invoke {@code doRollback}, handling rollback exceptions properly.
	 * @param status object representing the transaction
	 * @param ex the thrown application exception or error
	 * @throws TransactionException in case of rollback failure
	 * @see #doRollback
	 */
	// 调用 doRollback ，正确处理回滚异常。
	// 参形：
	//			status – 代表事务的对象
	//			ex – 抛出的应用程序异常或错误
	// 抛出：
	//			TransactionException – 在回滚失败的情况下
	private void doRollbackOnCommitException(DefaultTransactionStatus status, Throwable ex) throws TransactionException {
		try {
			if (status.isNewTransaction()) {
				if (status.isDebug()) {
					logger.debug("Initiating transaction rollback after commit exception", ex);
				}
				// 回滚
				doRollback(status);
			}
			else if (status.hasTransaction() && isGlobalRollbackOnParticipationFailure()) {
				if (status.isDebug()) {
					logger.debug("Marking existing transaction as rollback-only after commit exception", ex);
				}
				// 设置只是回滚
				doSetRollbackOnly(status);
			}
		}
		catch (RuntimeException | Error rbex) {
			logger.error("Commit exception overridden by rollback exception", ex);
			triggerAfterCompletion(status, TransactionSynchronization.STATUS_UNKNOWN);
			throw rbex;
		}
		triggerAfterCompletion(status, TransactionSynchronization.STATUS_ROLLED_BACK);
	}


	/**
	 * Trigger {@code beforeCommit} callbacks.
	 * @param status object representing the transaction
	 */
	// 触发 beforeCommit 回调。
	// 参形：
	//			status – 代表事务的对象
	protected final void triggerBeforeCommit(DefaultTransactionStatus status) {
		if (status.isNewSynchronization()) {
			TransactionSynchronizationUtils.triggerBeforeCommit(status.isReadOnly());
		}
	}

	/**
	 * Trigger {@code beforeCompletion} callbacks.
	 * @param status object representing the transaction
	 */
	// 触发 beforeCompletion 回调。
	// 参形：status – 代表事务的对象
	protected final void triggerBeforeCompletion(DefaultTransactionStatus status) {
		if (status.isNewSynchronization()) {
			TransactionSynchronizationUtils.triggerBeforeCompletion();
		}
	}

	/**
	 * Trigger {@code afterCommit} callbacks.
	 * @param status object representing the transaction
	 */
	// 触发 afterCommit 回调。
	// 参形：status – 代表事务的对象
	private void triggerAfterCommit(DefaultTransactionStatus status) {
		if (status.isNewSynchronization()) {
			TransactionSynchronizationUtils.triggerAfterCommit();
		}
	}

	/**
	 * Trigger {@code afterCompletion} callbacks.
	 * @param status object representing the transaction
	 * @param completionStatus completion status according to TransactionSynchronization constants
	 */
	// 触发 afterCompletion 回调。
	// 参形：
	//			status – 代表事务的对象
	//			completionStatus – 根据 TransactionSynchronization 常量的完成状态
	private void triggerAfterCompletion(DefaultTransactionStatus status, int completionStatus) {
		if (status.isNewSynchronization()) {
			List<TransactionSynchronization> synchronizations = TransactionSynchronizationManager.getSynchronizations();
			TransactionSynchronizationManager.clearSynchronization();
			if (!status.hasTransaction() || status.isNewTransaction()) {
				// No transaction or new transaction for the current scope ->
				// invoke the afterCompletion callbacks immediately
				// 当前范围内没有事务或新事务 -> 立即调用 afterCompletion 回调
				invokeAfterCompletion(synchronizations, completionStatus);
			}
			else if (!synchronizations.isEmpty()) {
				// Existing transaction that we participate in, controlled outside
				// of the scope of this Spring transaction manager -> try to register
				// an afterCompletion callback with the existing (JTA) transaction.
				// 我们参与的现有事务，在此 Spring 事务管理器范围之外控制 -> 尝试使用现有（JTA）事务
				// 注册 afterCompletion 回调。
				registerAfterCompletionWithExistingTransaction(status.getTransaction(), synchronizations);
			}
		}
	}

	/**
	 * Actually invoke the {@code afterCompletion} methods of the
	 * given Spring TransactionSynchronization objects.
	 * <p>To be called by this abstract manager itself, or by special implementations
	 * of the {@code registerAfterCompletionWithExistingTransaction} callback.
	 * @param synchronizations a List of TransactionSynchronization objects
	 * @param completionStatus the completion status according to the
	 * constants in the TransactionSynchronization interface
	 * @see #registerAfterCompletionWithExistingTransaction(Object, java.util.List)
	 * @see TransactionSynchronization#STATUS_COMMITTED
	 * @see TransactionSynchronization#STATUS_ROLLED_BACK
	 * @see TransactionSynchronization#STATUS_UNKNOWN
	 */
	// 实际调用给定 Spring TransactionSynchronization 对象的 afterCompletion 方法。
	//
	// 由这个抽象管理器本身调用，或者由 registerAfterCompletionWithExistingTransaction 回调的特殊实现调用。
	// 参形：
	//			synchronizations – TransactionSynchronization 对象列表
	//			completionStatus – 根据 TransactionSynchronization 接口中的常量完成状态
	protected final void invokeAfterCompletion(List<TransactionSynchronization> synchronizations, int completionStatus) {
		TransactionSynchronizationUtils.invokeAfterCompletion(synchronizations, completionStatus);
	}

	/**
	 * Clean up after completion, clearing synchronization if necessary,
	 * and invoking doCleanupAfterCompletion.
	 * @param status object representing the transaction
	 * @see #doCleanupAfterCompletion
	 */
	// 完成后清理，必要时清除同步，并调用 doCleanupAfterCompletion。
	// 参形：status – 代表事务的对象
	private void cleanupAfterCompletion(DefaultTransactionStatus status) {
		status.setCompleted();
		if (status.isNewSynchronization()) {
			TransactionSynchronizationManager.clear();
		}
		if (status.isNewTransaction()) {
			doCleanupAfterCompletion(status.getTransaction());
		}
		if (status.getSuspendedResources() != null) {
			if (status.isDebug()) {
				logger.debug("Resuming suspended transaction after completion of inner transaction");
			}
			Object transaction = (status.hasTransaction() ? status.getTransaction() : null);
			resume(transaction, (SuspendedResourcesHolder) status.getSuspendedResources());
		}
	}


	//---------------------------------------------------------------------
	// Template methods to be implemented in subclasses
	// 要在子类中实现的模板方法
	//---------------------------------------------------------------------

	/**
	 * Return a transaction object for the current transaction state.
	 * <p>The returned object will usually be specific to the concrete transaction
	 * manager implementation, carrying corresponding transaction state in a
	 * modifiable fashion. This object will be passed into the other template
	 * methods (e.g. doBegin and doCommit), either directly or as part of a
	 * DefaultTransactionStatus instance.
	 * <p>The returned object should contain information about any existing
	 * transaction, that is, a transaction that has already started before the
	 * current {@code getTransaction} call on the transaction manager.
	 * Consequently, a {@code doGetTransaction} implementation will usually
	 * look for an existing transaction and store corresponding state in the
	 * returned transaction object.
	 * @return the current transaction object
	 * @throws org.springframework.transaction.CannotCreateTransactionException
	 * if transaction support is not available
	 * @throws TransactionException in case of lookup or system errors
	 * @see #doBegin
	 * @see #doCommit
	 * @see #doRollback
	 * @see DefaultTransactionStatus#getTransaction
	 */
	// 返回当前事务状态的事务对象。
	//
	// 返回的对象通常特定于具体的事务管理器实现，以可修改的方式携带相应的事务状态。该对象将直接或
	// 作为 DefaultTransactionStatus 实例的一部分传递给其他模板方法（例如 doBegin 和 doCommit）。
	//
	// 返回的对象应包含有关任何现有事务的信息，即在事务管理器上的当前 getTransaction 调用之前已经开始的事务。
	// 因此， doGetTransaction 实现通常会查找现有事务并将相应的状态存储在返回的事务对象中。
	//
	// 返回值：
	//			当前事务对象
	// 抛出：
	//			org.springframework.transaction.CannotCreateTransactionException – 如果事务支持不可用
	//TransactionException – 在查找或系统错误的情况下
	protected abstract Object doGetTransaction() throws TransactionException;

	/**
	 * Check if the given transaction object indicates an existing transaction
	 * (that is, a transaction which has already started).
	 * <p>The result will be evaluated according to the specified propagation
	 * behavior for the new transaction. An existing transaction might get
	 * suspended (in case of PROPAGATION_REQUIRES_NEW), or the new transaction
	 * might participate in the existing one (in case of PROPAGATION_REQUIRED).
	 * <p>The default implementation returns {@code false}, assuming that
	 * participating in existing transactions is generally not supported.
	 * Subclasses are of course encouraged to provide such support.
	 * @param transaction the transaction object returned by doGetTransaction
	 * @return if there is an existing transaction
	 * @throws TransactionException in case of system errors
	 * @see #doGetTransaction
	 */
	// 检查给定的事务对象是否指示现有事务（即，已经开始的事务）。
	// 
	// 结果将根据新事务的指定传播行为进行评估。现有事务可能会暂停（在 PROPAGATION_REQUIRES_NEW 的情况下），
	// 或者新事务可能参与现有事务（在 PROPAGATION_REQUIRED 的情况下）。
	// 
	// 默认实现返回 false ，假设通常不支持参与现有事务。当然鼓励子类提供这种支持。
	// 参形：
	//				transaction – doGetTransaction 返回的事务对象
	// 返回值：
	//				如果存在现有事务
	// 抛出：
	//				TransactionException – 在系统错误的情况下
	protected boolean isExistingTransaction(Object transaction) throws TransactionException {
		return false;
	}

	/**
	 * Return whether to use a savepoint for a nested transaction.
	 * <p>Default is {@code true}, which causes delegation to DefaultTransactionStatus
	 * for creating and holding a savepoint. If the transaction object does not implement
	 * the SavepointManager interface, a NestedTransactionNotSupportedException will be
	 * thrown. Else, the SavepointManager will be asked to create a new savepoint to
	 * demarcate the start of the nested transaction.
	 * <p>Subclasses can override this to return {@code false}, causing a further
	 * call to {@code doBegin} - within the context of an already existing transaction.
	 * The {@code doBegin} implementation needs to handle this accordingly in such
	 * a scenario. This is appropriate for JTA, for example.
	 * @see DefaultTransactionStatus#createAndHoldSavepoint
	 * @see DefaultTransactionStatus#rollbackToHeldSavepoint
	 * @see DefaultTransactionStatus#releaseHeldSavepoint
	 * @see #doBegin
	 */
	// 返回是否为嵌套事务使用保存点。
	//
	// 默认为 true ，这会导致委托 DefaultTransactionStatus 来创建和保存保存点。如果事务对象没有
	// 实现 SavepointManager 接口，则会抛出 NestedTransactionNotSupportedException。否则，
	// 将要求 SavepointManager 创建一个新的保存点来划分嵌套事务的开始。
	//
	// 子类可以覆盖它以返回 false ，从而导致对 doBegin 的进一步调用 - 在已经存在的事务的上下文中。
	// 在这种情况下， doBegin 实现需要相应地处理这个问题。例如，这适用于 JTA。
	protected boolean useSavepointForNestedTransaction() {
		return true;
	}

	/**
	 * Begin a new transaction with semantics according to the given transaction
	 * definition. Does not have to care about applying the propagation behavior,
	 * as this has already been handled by this abstract manager.
	 * <p>This method gets called when the transaction manager has decided to actually
	 * start a new transaction. Either there wasn't any transaction before, or the
	 * previous transaction has been suspended.
	 * <p>A special scenario is a nested transaction without savepoint: If
	 * {@code useSavepointForNestedTransaction()} returns "false", this method
	 * will be called to start a nested transaction when necessary. In such a context,
	 * there will be an active transaction: The implementation of this method has
	 * to detect this and start an appropriate nested transaction.
	 * @param transaction the transaction object returned by {@code doGetTransaction}
	 * @param definition a TransactionDefinition instance, describing propagation
	 * behavior, isolation level, read-only flag, timeout, and transaction name
	 * @throws TransactionException in case of creation or system errors
	 * @throws org.springframework.transaction.NestedTransactionNotSupportedException
	 * if the underlying transaction does not support nesting
	 */
	// 根据给定的事务定义开始一个具有语义的新事务。不必关心应用传播行为，因为这已经由这个抽象管理器处理了。
	//
	// 当事务管理器决定实际启动一个新事务时，将调用此方法。要么之前没有任何事务，要么之前的事务已经暂停。
	//
	// 一个特殊的场景是没有保存点的嵌套事务：如果useSavepointForNestedTransaction()返回“false”，则在必
	// 要时会调用此方法启动嵌套事务。在这样的上下文中，将有一个活动事务：此方法的实现必须检测到这一点并启动一个适当
	// 的嵌套事务。
	// 参形：
	//			transaction – doGetTransaction返回的事务对象
	//			definition - 一个 TransactionDefinition 实例，描述传播行为、隔离级别、只读标志、超时和事务名称
	// 抛出：
	//			TransactionException – 在创建或系统错误的情况下
	//			NestedTransactionNotSupportedException – 如果底层事务不支持嵌套
	protected abstract void doBegin(Object transaction, TransactionDefinition definition)
			throws TransactionException;

	/**
	 * Suspend the resources of the current transaction.
	 * Transaction synchronization will already have been suspended.
	 * <p>The default implementation throws a TransactionSuspensionNotSupportedException,
	 * assuming that transaction suspension is generally not supported.
	 * @param transaction the transaction object returned by {@code doGetTransaction}
	 * @return an object that holds suspended resources
	 * (will be kept unexamined for passing it into doResume)
	 * @throws org.springframework.transaction.TransactionSuspensionNotSupportedException
	 * if suspending is not supported by the transaction manager implementation
	 * @throws TransactionException in case of system errors
	 * @see #doResume
	 */
	// 暂停当前事务的资源。事务同步将已暂停。
	// 默认实现会抛出 TransactionSuspensionNotSupportedException，假设一般不支持事务暂停。
	// 参形：
	//			transaction – doGetTransaction返回的事务对象
	// 返回值：
	//			一个包含暂停资源的对象（将保持未经检查的状态以将其传递给 doResume）
	// 抛出：
	//			TransactionSuspensionNotSupportedException – 如果事务管理器实现不支持挂起
	//			TransactionException – 在系统错误的情况下
	protected Object doSuspend(Object transaction) throws TransactionException {
		throw new TransactionSuspensionNotSupportedException(
				"Transaction manager [" + getClass().getName() + "] does not support transaction suspension");
	}

	/**
	 * Resume the resources of the current transaction.
	 * Transaction synchronization will be resumed afterwards.
	 * <p>The default implementation throws a TransactionSuspensionNotSupportedException,
	 * assuming that transaction suspension is generally not supported.
	 * @param transaction the transaction object returned by {@code doGetTransaction}
	 * @param suspendedResources the object that holds suspended resources,
	 * as returned by doSuspend
	 * @throws org.springframework.transaction.TransactionSuspensionNotSupportedException
	 * if resuming is not supported by the transaction manager implementation
	 * @throws TransactionException in case of system errors
	 * @see #doSuspend
	 */
	// 恢复当前事务的资源。事务同步将在之后恢复。
	// 默认实现会抛出 TransactionSuspensionNotSupportedException，假设一般不支持事务暂停。
	// 参形：
	//			transaction – doGetTransaction返回的事务对象
	//			suspendResources – 持有暂停资源的对象，由 doSuspend 返回
	// 抛出：
	//			TransactionSuspensionNotSupportedException – 如果事务管理器实现不支持恢复
	//			TransactionException – 在系统错误的情况下
	protected void doResume(@Nullable Object transaction, Object suspendedResources) throws TransactionException {
		throw new TransactionSuspensionNotSupportedException(
				"Transaction manager [" + getClass().getName() + "] does not support transaction suspension");
	}

	/**
	 * Return whether to call {@code doCommit} on a transaction that has been
	 * marked as rollback-only in a global fashion.
	 * <p>Does not apply if an application locally sets the transaction to rollback-only
	 * via the TransactionStatus, but only to the transaction itself being marked as
	 * rollback-only by the transaction coordinator.
	 * <p>Default is "false": Local transaction strategies usually don't hold the rollback-only
	 * marker in the transaction itself, therefore they can't handle rollback-only transactions
	 * as part of transaction commit. Hence, AbstractPlatformTransactionManager will trigger
	 * a rollback in that case, throwing an UnexpectedRollbackException afterwards.
	 * <p>Override this to return "true" if the concrete transaction manager expects a
	 * {@code doCommit} call even for a rollback-only transaction, allowing for
	 * special handling there. This will, for example, be the case for JTA, where
	 * {@code UserTransaction.commit} will check the read-only flag itself and
	 * throw a corresponding RollbackException, which might include the specific reason
	 * (such as a transaction timeout).
	 * <p>If this method returns "true" but the {@code doCommit} implementation does not
	 * throw an exception, this transaction manager will throw an UnexpectedRollbackException
	 * itself. This should not be the typical case; it is mainly checked to cover misbehaving
	 * JTA providers that silently roll back even when the rollback has not been requested
	 * by the calling code.
	 * @see #doCommit
	 * @see DefaultTransactionStatus#isGlobalRollbackOnly()
	 * @see DefaultTransactionStatus#isLocalRollbackOnly()
	 * @see org.springframework.transaction.TransactionStatus#setRollbackOnly()
	 * @see org.springframework.transaction.UnexpectedRollbackException
	 * @see javax.transaction.UserTransaction#commit()
	 * @see javax.transaction.RollbackException
	 */
	// 返回是否对已以全局方式标记为仅回滚的事务调用doCommit 。
	//
	// 如果应用程序通过 TransactionStatus 在本地将事务设置为仅回滚，则不适用，但仅适用于事务协调器
	// 将事务本身标记为仅回滚。
	//
	// 默认为“false”：本地事务策略通常不会在事务本身中保留仅回滚标记，因此它们不能将仅回滚事务作为事务
	// 提交的一部分来处理。因此，AbstractPlatformTransactionManager 在这种情况下将触发回滚，然后
	// 抛出 UnexpectedRollbackException。
	//
	// 如果具体事务管理器即使对于仅回滚事务也期望doCommit调用，则覆盖它以返回“true”，从而允许在那里进行特殊处理。
	// 例如，JTA 就是这种情况，其中UserTransaction.commit将检查只读标志本身并抛出相应的 RollbackException，
	// 其中可能包括特定原因（例如事务超时）。
	//
	// 如果此方法返回“true”但doCommit实现未抛出异常，则此事务管理器将自身抛出 UnexpectedRollbackException。
	// 这不应该是典型案例；主要检查它以覆盖行为不端的 JTA 提供程序，即使调用代码没有请求回滚，它们也会静默回滚。
	protected boolean shouldCommitOnGlobalRollbackOnly() {
		return false;
	}

	/**
	 * Make preparations for commit, to be performed before the
	 * {@code beforeCommit} synchronization callbacks occur.
	 * <p>Note that exceptions will get propagated to the commit caller
	 * and cause a rollback of the transaction.
	 * @param status the status representation of the transaction
	 * @throws RuntimeException in case of errors; will be <b>propagated to the caller</b>
	 * (note: do not throw TransactionException subclasses here!)
	 */
	// 准备提交，在beforeCommit同步回调发生之前执行。
	// 请注意，异常将传播到提交调用者并导致事务回滚。
	// 参形：
	//				status – 事务的状态表示
	// 抛出：
	//				RuntimeException – 发生错误时；将被传播给调用者（注意：不要在这里抛出 
	//				TransactionException 子类！）
	protected void prepareForCommit(DefaultTransactionStatus status) {
	}

	/**
	 * Perform an actual commit of the given transaction.
	 * <p>An implementation does not need to check the "new transaction" flag
	 * or the rollback-only flag; this will already have been handled before.
	 * Usually, a straight commit will be performed on the transaction object
	 * contained in the passed-in status.
	 * @param status the status representation of the transaction
	 * @throws TransactionException in case of commit or system errors
	 * @see DefaultTransactionStatus#getTransaction
	 */
	// 执行给定事务的实际提交。
	// 实现不需要检查“新事务”标志或仅回滚标志；这之前已经处理过了。通常，会在传入状态中包含的事务对象上执行直接提交。
	// 参形：
	//				status – 事务的状态表示
	// 抛出：
	//				TransactionException – 在提交或系统错误的情况下
	protected abstract void doCommit(DefaultTransactionStatus status) throws TransactionException;

	/**
	 * Perform an actual rollback of the given transaction.
	 * <p>An implementation does not need to check the "new transaction" flag;
	 * this will already have been handled before. Usually, a straight rollback
	 * will be performed on the transaction object contained in the passed-in status.
	 * @param status the status representation of the transaction
	 * @throws TransactionException in case of system errors
	 * @see DefaultTransactionStatus#getTransaction
	 */
	// 执行给定事务的实际回滚。
	// 实现不需要检查“新事务”标志；这之前已经处理过了。通常，对传入状态中包含的事务对象执行直接回滚。
	// 参形：
	//				status – 事务的状态表示
	// 抛出：
	//				TransactionException – 在系统错误的情况下
	protected abstract void doRollback(DefaultTransactionStatus status) throws TransactionException;

	/**
	 * Set the given transaction rollback-only. Only called on rollback
	 * if the current transaction participates in an existing one.
	 * <p>The default implementation throws an IllegalTransactionStateException,
	 * assuming that participating in existing transactions is generally not
	 * supported. Subclasses are of course encouraged to provide such support.
	 * @param status the status representation of the transaction
	 * @throws TransactionException in case of system errors
	 */
	// 仅设置给定事务回滚。如果当前事务参与现有事务，则仅在回滚时调用。
	// 默认实现会抛出 IllegalTransactionStateException，假设通常不支持参与现有事务。当然鼓励子类提供这种支持。
	// 参形：
	//				status – 事务的状态表示
	// 抛出：
	//				TransactionException – 在系统错误的情况下
	protected void doSetRollbackOnly(DefaultTransactionStatus status) throws TransactionException {
		throw new IllegalTransactionStateException(
				"Participating in existing transactions is not supported - when 'isExistingTransaction' " +
				"returns true, appropriate 'doSetRollbackOnly' behavior must be provided");
	}

	/**
	 * Register the given list of transaction synchronizations with the existing transaction.
	 * <p>Invoked when the control of the Spring transaction manager and thus all Spring
	 * transaction synchronizations end, without the transaction being completed yet. This
	 * is for example the case when participating in an existing JTA or EJB CMT transaction.
	 * <p>The default implementation simply invokes the {@code afterCompletion} methods
	 * immediately, passing in "STATUS_UNKNOWN". This is the best we can do if there's no
	 * chance to determine the actual outcome of the outer transaction.
	 * @param transaction the transaction object returned by {@code doGetTransaction}
	 * @param synchronizations a List of TransactionSynchronization objects
	 * @throws TransactionException in case of system errors
	 * @see #invokeAfterCompletion(java.util.List, int)
	 * @see TransactionSynchronization#afterCompletion(int)
	 * @see TransactionSynchronization#STATUS_UNKNOWN
	 */
	// 将给定的事务同步列表注册到现有事务。
	// 当 Spring 事务管理器的控制以及所有 Spring 事务同步结束时调用，而事务尚未完成。例如，参与现有 JTA 
	// 或 EJB CMT 事务时就是这种情况。
	// 
	// 默认实现只是立即调用afterCompletion方法，传入“STATUS_UNKNOWN”。如果没有机会确定外部事务的实际结果，
	// 这是我们能做的最好的事情。
	//
	// 参形：
	//				transaction – doGetTransaction返回的事务对象
	//				同步– TransactionSynchronization 对象列表
	// 抛出：
	//				TransactionException – 在系统错误的情况下
	protected void registerAfterCompletionWithExistingTransaction(
			Object transaction, List<TransactionSynchronization> synchronizations) throws TransactionException {

		logger.debug("Cannot register Spring after-completion synchronization with existing transaction - " +
				"processing Spring after-completion callbacks immediately, with outcome status 'unknown'");
		invokeAfterCompletion(synchronizations, TransactionSynchronization.STATUS_UNKNOWN);
	}

	/**
	 * Cleanup resources after transaction completion.
	 * <p>Called after {@code doCommit} and {@code doRollback} execution,
	 * on any outcome. The default implementation does nothing.
	 * <p>Should not throw any exceptions but just issue warnings on errors.
	 * @param transaction the transaction object returned by {@code doGetTransaction}
	 */
	// 事务完成后清理资源。
	// 在 doCommit 和 doRollback 执行后调用，对任何结果。默认实现什么也不做。
	// 不应该抛出任何异常，而只是发出错误警告。
	// 参形：
	//				transaction – doGetTransaction返回的事务对象
	protected void doCleanupAfterCompletion(Object transaction) {
	}


	//---------------------------------------------------------------------
	// Serialization support
	//---------------------------------------------------------------------

	private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
		// Rely on default serialization; just initialize state after deserialization.
		// 依赖默认序列化；反序列化后只需初始化状态
		ois.defaultReadObject();

		// Initialize transient fields.
		// 初始化瞬态字段
		this.logger = LogFactory.getLog(getClass());
	}


	/**
	 * Holder for suspended resources.
	 * Used internally by {@code suspend} and {@code resume}.
	 */
	// 挂起资源的持有者。由挂起和恢复在内部使用
	protected static final class SuspendedResourcesHolder {

		// 挂起资源
		@Nullable
		private final Object suspendedResources;

		// 事务同步器集合
		@Nullable
		private List<TransactionSynchronization> suspendedSynchronizations;

		// 名称
		@Nullable
		private String name;

		// 是否只读
		private boolean readOnly;

		// 隔离级别
		@Nullable
		private Integer isolationLevel;

		// 是否已激活
		private boolean wasActive;

		// 挂起资源持有者
		private SuspendedResourcesHolder(Object suspendedResources) {
			this.suspendedResources = suspendedResources;
		}

		private SuspendedResourcesHolder(
				@Nullable Object suspendedResources, List<TransactionSynchronization> suspendedSynchronizations,
				@Nullable String name, boolean readOnly, @Nullable Integer isolationLevel, boolean wasActive) {

			this.suspendedResources = suspendedResources;
			this.suspendedSynchronizations = suspendedSynchronizations;
			this.name = name;
			this.readOnly = readOnly;
			this.isolationLevel = isolationLevel;
			this.wasActive = wasActive;
		}
	}

}
