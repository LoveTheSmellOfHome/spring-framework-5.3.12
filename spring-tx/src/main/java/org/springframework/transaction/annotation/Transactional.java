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

package org.springframework.transaction.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;
import org.springframework.transaction.TransactionDefinition;

/**
 * Describes a transaction attribute on an individual method or on a class.
 *
 * <p>When this annotation is declared at the class level, it applies as a default
 * to all methods of the declaring class and its subclasses. Note that it does not
 * apply to ancestor classes up the class hierarchy; inherited methods need to be
 * locally redeclared in order to participate in a subclass-level annotation. For
 * details on method visibility constraints, consult the
 * <a href="https://docs.spring.io/spring-framework/docs/current/reference/html/data-access.html#transaction">Transaction Management</a>
 * section of the reference manual.
 *
 * <p>This annotation type is generally directly comparable to Spring's
 * {@link org.springframework.transaction.interceptor.RuleBasedTransactionAttribute}
 * class, and in fact {@link AnnotationTransactionAttributeSource} will directly
 * convert the data to the latter class, so that Spring's transaction support code
 * does not have to know about annotations. If no custom rollback rules apply,
 * the transaction will roll back on {@link RuntimeException} and {@link Error}
 * but not on checked exceptions.
 *
 * <p>For specific information about the semantics of this annotation's attributes,
 * consult the {@link org.springframework.transaction.TransactionDefinition} and
 * {@link org.springframework.transaction.interceptor.TransactionAttribute} javadocs.
 *
 * <p>This annotation commonly works with thread-bound transactions managed by a
 * {@link org.springframework.transaction.PlatformTransactionManager}, exposing a
 * transaction to all data access operations within the current execution thread.
 * <b>Note: This does NOT propagate to newly started threads within the method.</b>
 *
 * <p>Alternatively, this annotation may demarcate a reactive transaction managed
 * by a {@link org.springframework.transaction.ReactiveTransactionManager} which
 * uses the Reactor context instead of thread-local variables. As a consequence,
 * all participating data access operations need to execute within the same
 * Reactor context in the same reactive pipeline.
 *
 * @author Colin Sampaleanu
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author Mark Paluch
 * @since 1.2
 * @see org.springframework.transaction.interceptor.TransactionAttribute
 * @see org.springframework.transaction.interceptor.DefaultTransactionAttribute
 * @see org.springframework.transaction.interceptor.RuleBasedTransactionAttribute
 */
// 描述单个方法或类上的事务属性
//
// 当这个注解在类级别声明时，它默认应用于声明类及其子类的所有方法。请注意，它不适用于在类层次结构中向上的祖先类；
// 继承的方法需要在本地重新声明才能参与子类级别的注释。有关方法可见性约束的详细信息，请参阅参考手册的
// <a href="https:docs.spring.iospring-frameworkdocscurrentreferencehtmldata-access.htmltransaction">事务管理<a> 部分。
//
// 这个注解类型一般代替Spring的
// {@link org.springframework.transaction.interceptor.RuleBasedTransactionAttribute}类，
// 而实际上{@link AnnotationTransactionAttributeSource}会直接将数据转换为 RuleBasedTransactionAttribute 类，
// 这样Spring的事务支持代码不必知道注解。如果没有自定义回滚规则适用，事务将回滚
// {@link RuntimeException} 和 {@link Error}，但不会回滚检查异常。
//
// 有关此注解属性语义的具体信息，请参阅 {@link org.springframework.transaction.TransactionDefinition} 和
// {@link org.springframework.transaction.interceptor.TransactionAttribute} javadocs。
//
// 此注解通常与由 {@link org.springframework.transaction.PlatformTransactionManager} 管理的线程
// 绑定事务一起使用，将事务暴露给当前执行线程内的所有数据访问操作。 <b>注意：这不会传播到方法中新启动的线程。<b>
//
// 或者，这个注解可以划定一个由 {@link org.springframework.transaction.ReactiveTransactionManager}
// 管理的反应式事务，它使用 Reactor 上下文而不是线程局部变量。因此，所有参与的数据访问操作都需要在同一个反应管道中的
// 同一个 Reactor 上下文中执行
//
// Spring 事务
// 优点：高度抽象和封装良好的 API
// 缺点：1.只支持本地事务，不支持分布式事务（跨进程）； 2.这个事务不能跨线程，是单线程事务
//
// @Transactional 定义这个方法执行的时候或者类执行的时候，它所承载的这部分的 TransactionDefinition
// 这个事务定义他是逻辑性的。它并不是一个物理的连接。一个良好的调用栈，是在同一个线程中的，即创建事务后，调用
// 方法 m1,m1 调用了 m2,那么 m1,m2 都是在事务作用下。这要求我们这两个方法被同一个线程调用，否则事务会失效。
// 每个 @Transactional 标注的方法作用范围就是逻辑事务的范围，而对应着数据库事务的提交或回滚是物理事务。我们设置了
// rollback-only
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface Transactional {

	/**
	 * Alias for {@link #transactionManager}.
	 * @see #transactionManager
	 */
	// {@link transactionManager} 的别名。
	//
	// 通过 bean 的名称去关联自定义的 transactionManager,transactionManager 可能有多个，
	// 它不一定指的是和数据库相关的连接，TransactionManager bean 的名称
	@AliasFor("transactionManager")
	String value() default "";

	/**
	 * A <em>qualifier</em> value for the specified transaction.
	 * <p>May be used to determine the target transaction manager, matching the
	 * qualifier value (or the bean name) of a specific
	 * {@link org.springframework.transaction.TransactionManager TransactionManager}
	 * bean definition.
	 * @since 4.2
	 * @see #value
	 * @see org.springframework.transaction.PlatformTransactionManager
	 * @see org.springframework.transaction.ReactiveTransactionManager
	 */
	// 指定事务的 <em>qualifier<em> 值。
	//
	// 可用于确定目标事务管理器，匹配特定
	// {@link org.springframework.transaction.TransactionManager TransactionManager}
	// bean 定义的限定符值（或 bean 名称）
	@AliasFor("value")
	String transactionManager() default ""; // 事务管理器

	/**
	 * Defines zero (0) or more transaction labels.
	 * <p>Labels may be used to describe a transaction, and they can be evaluated
	 * by individual transaction managers. Labels may serve a solely descriptive
	 * purpose or map to pre-defined transaction manager-specific options.
	 * <p>See the documentation of the actual transaction manager implementation
	 * for details on how it evaluates transaction labels.
	 * @since 5.3
	 * @see org.springframework.transaction.interceptor.DefaultTransactionAttribute#getLabels()
	 */
	// 定义零 (0) 个或多个事务标签。
	// 标签可以用来描述一个事务，它们可以由各个事务管理器进行评估。标签可能仅用于描述目的或映射到预定义的事务管理器特定选项。
	// 有关如何评估事务标签的详细信息，请参阅实际事务管理器实现的文档。
	String[] label() default {};

	/**
	 * The transaction propagation type.
	 * <p>Defaults to {@link Propagation#REQUIRED}.
	 * @see org.springframework.transaction.interceptor.TransactionAttribute#getPropagationBehavior()
	 */
	// 事务传播类型
	// 默认为 {@link PropagationREQUIRED}
	Propagation propagation() default Propagation.REQUIRED;

	/**
	 * The transaction isolation level.
	 * <p>Defaults to {@link Isolation#DEFAULT}.
	 * <p>Exclusively designed for use with {@link Propagation#REQUIRED} or
	 * {@link Propagation#REQUIRES_NEW} since it only applies to newly started
	 * transactions. Consider switching the "validateExistingTransactions" flag to
	 * "true" on your transaction manager if you'd like isolation level declarations
	 * to get rejected when participating in an existing transaction with a different
	 * isolation level.
	 * @see org.springframework.transaction.interceptor.TransactionAttribute#getIsolationLevel()
	 * @see org.springframework.transaction.support.AbstractPlatformTransactionManager#setValidateExistingTransaction
	 */
	// 事务隔离级别。
	//
	// 默认为 {@link Isolation#DEFAULT}。
	//
	// 专为与 {@link Propagation#REQUIRED} 或 {@link Propagation#REQUIRES_NEW} 一起使用而设计，因为它仅适用于新启动的事务。
	//
	// 如果您希望隔离级别声明在参与具有不同隔离级别的现有事务时被拒绝，请考虑将事务管理器上的
	// “validateExistingTransactions”标志切换为“true”。
	Isolation isolation() default Isolation.DEFAULT;

	/**
	 * The timeout for this transaction (in seconds).
	 * <p>Defaults to the default timeout of the underlying transaction system.
	 * <p>Exclusively designed for use with {@link Propagation#REQUIRED} or
	 * {@link Propagation#REQUIRES_NEW} since it only applies to newly started
	 * transactions.
	 * @return the timeout in seconds
	 * @see org.springframework.transaction.interceptor.TransactionAttribute#getTimeout()
	 */
	// 此事务的超时时间（以秒为单位）。 <p>默认为底层交易系统的默认超时时间。
	//
	// 专为与 {@link Propagation#REQUIRED} 或 {@link Propagation#REQUIRES_NEW} 一起使用而设计，
	// 因为它仅适用于新启动的事务。
	int timeout() default TransactionDefinition.TIMEOUT_DEFAULT;

	/**
	 * The timeout for this transaction (in seconds).
	 * <p>Defaults to the default timeout of the underlying transaction system.
	 * <p>Exclusively designed for use with {@link Propagation#REQUIRED} or
	 * {@link Propagation#REQUIRES_NEW} since it only applies to newly started
	 * transactions.
	 * @return the timeout in seconds as a String value, e.g. a placeholder
	 * @since 5.3
	 * @see org.springframework.transaction.interceptor.TransactionAttribute#getTimeout()
	 */
	// 事务的超时时间（以秒为单位）。
	//
	// 默认为底层交易系统的默认超时时间。
	//
	// 专为与 {@link Propagation#REQUIRED} 或 {@link Propagation#REQUIRES_NEW} 一起使用而设计，
	// 因为它仅适用于新启动的事务。
	String timeoutString() default "";

	/**
	 * A boolean flag that can be set to {@code true} if the transaction is
	 * effectively read-only, allowing for corresponding optimizations at runtime.
	 * <p>Defaults to {@code false}.
	 * <p>This just serves as a hint for the actual transaction subsystem;
	 * it will <i>not necessarily</i> cause failure of write access attempts.
	 * A transaction manager which cannot interpret the read-only hint will
	 * <i>not</i> throw an exception when asked for a read-only transaction
	 * but rather silently ignore the hint.
	 * @see org.springframework.transaction.interceptor.TransactionAttribute#isReadOnly()
	 * @see org.springframework.transaction.support.TransactionSynchronizationManager#isCurrentTransactionReadOnly()
	 */
	// 如果事务实际上是只读的，则可以设置为 {@code true} 的布尔标志，允许在运行时进行相应的优化。
	//
	// 默认为 {@code false}。
	//
	// 这只是对实际事务子系统的提示；它<i>不一定<i>会导致写访问尝试失败。
	//
	// 无法解释只读提示的事务管理器在请求只读事务时将 <i>not<i> 抛出异常，而是默默地忽略该提示。
	boolean readOnly() default false;

	/**
	 * Defines zero (0) or more exception {@link Class classes}, which must be
	 * subclasses of {@link Throwable}, indicating which exception types must cause
	 * a transaction rollback.
	 * <p>By default, a transaction will be rolling back on {@link RuntimeException}
	 * and {@link Error} but not on checked exceptions (business exceptions). See
	 * {@link org.springframework.transaction.interceptor.DefaultTransactionAttribute#rollbackOn(Throwable)}
	 * for a detailed explanation.
	 * <p>This is the preferred way to construct a rollback rule (in contrast to
	 * {@link #rollbackForClassName}), matching the exception class and its subclasses.
	 * <p>Similar to {@link org.springframework.transaction.interceptor.RollbackRuleAttribute#RollbackRuleAttribute(Class clazz)}.
	 * @see #rollbackForClassName
	 * @see org.springframework.transaction.interceptor.DefaultTransactionAttribute#rollbackOn(Throwable)
	 */
	// 定义零 (0) 个或多个异常 {@link Class classes}，它们必须是 {@link Throwable} 的子类，指示哪些异常
	// 类型必须导致事务回滚。
	//
	// 默认情况下，事务将在 {@link RuntimeException} 和 {@link Error} 上回滚，但不会在已检查的异常（业务异常）上
	// 回滚。
	//
	// 有关详细说明，请参阅
	// {@link org.springframework.transaction.interceptor.DefaultTransactionAttribute#rollbackOn(Throwable)}。
	//
	// 这是构建回滚规则的首选方式（与 {@link #rollbackForClassName} 相反），匹配异常类及其子类。
	//
	// 类似于{@link org.springframework.transaction.interceptor.RollbackRuleAttributeRollbackRuleAttribute(Class clazz)}
	//
	// 处理异常状态
	Class<? extends Throwable>[] rollbackFor() default {};

	/**
	 * Defines zero (0) or more exception names (for exceptions which must be a
	 * subclass of {@link Throwable}), indicating which exception types must cause
	 * a transaction rollback.
	 * <p>This can be a substring of a fully qualified class name, with no wildcard
	 * support at present. For example, a value of {@code "ServletException"} would
	 * match {@code javax.servlet.ServletException} and its subclasses.
	 * <p><b>NB:</b> Consider carefully how specific the pattern is and whether
	 * to include package information (which isn't mandatory). For example,
	 * {@code "Exception"} will match nearly anything and will probably hide other
	 * rules. {@code "java.lang.Exception"} would be correct if {@code "Exception"}
	 * were meant to define a rule for all checked exceptions. With more unusual
	 * {@link Exception} names such as {@code "BaseBusinessException"} there is no
	 * need to use a FQN.
	 * <p>Similar to {@link org.springframework.transaction.interceptor.RollbackRuleAttribute#RollbackRuleAttribute(String exceptionName)}.
	 * @see #rollbackFor
	 * @see org.springframework.transaction.interceptor.DefaultTransactionAttribute#rollbackOn(Throwable)
	 */
	// 义零 (0) 个或多个异常名称（对于必须是 {@link Throwable} 子类的异常），指示哪些异常类型必须导致事务回滚。
  	//
	// 这可以是完全限定类名的子字符串，目前不支持通配符。例如，{@code "ServletException"} 的值将匹配
	// {@code javax.servlet.ServletException} 及其子类。
	//
	// 注意：仔细考虑模式的具体程度以及是否包含包信息（这不是强制性的）。
	// 例如，{@code "Exception"} 几乎可以匹配任何内容，并且可能会隐藏其他规则。
	// 如果 {@code "Exception"} 旨在为所有已检查的异常定义规则，则 {@code "java.lang.Exception"} 将是正确的。
	// 对于更不寻常的 {@link Exception} 名称，例如 {@code "BaseBusinessException"}，就不需要使用 FQN。
	//
	// 类似于 {@link org.springframework.transaction.interceptor.
	// RollbackRuleAttributeRollbackRuleAttribute(String exceptionName)}。
	String[] rollbackForClassName() default {};

	/**
	 * Defines zero (0) or more exception {@link Class Classes}, which must be
	 * subclasses of {@link Throwable}, indicating which exception types must
	 * <b>not</b> cause a transaction rollback.
	 * <p>This is the preferred way to construct a rollback rule (in contrast
	 * to {@link #noRollbackForClassName}), matching the exception class and
	 * its subclasses.
	 * <p>Similar to {@link org.springframework.transaction.interceptor.NoRollbackRuleAttribute#NoRollbackRuleAttribute(Class clazz)}.
	 * @see #noRollbackForClassName
	 * @see org.springframework.transaction.interceptor.DefaultTransactionAttribute#rollbackOn(Throwable)
	 */
	// 定义零 (0) 个或多个异常 {@link Class Classes}，它们必须是 {@link Throwable} 的子类，
	// 指示哪些异常类型必须<b>不<b>导致事务回滚。
	//
	// 这是构建回滚规则的首选方式（与 {@link #noRollbackForClassName} 相反），匹配异常类及其子类。
	//
	// 类似于{@link org.springframework.transaction.interceptor.NoRollbackRuleAttribute
	// #NoRollbackRuleAttribute(Class clazz)}。
	Class<? extends Throwable>[] noRollbackFor() default {};

	/**
	 * Defines zero (0) or more exception names (for exceptions which must be a
	 * subclass of {@link Throwable}) indicating which exception types must <b>not</b>
	 * cause a transaction rollback.
	 * <p>See the description of {@link #rollbackForClassName} for further
	 * information on how the specified names are treated.
	 * <p>Similar to {@link org.springframework.transaction.interceptor.NoRollbackRuleAttribute#NoRollbackRuleAttribute(String exceptionName)}.
	 * @see #noRollbackFor
	 * @see org.springframework.transaction.interceptor.DefaultTransactionAttribute#rollbackOn(Throwable)
	 */
	// 定义零 (0) 个或多个异常名称（对于必须是 {@link Throwable} 子类的异常），指示哪些异常类型
	// 必须不导致事务回滚。
	//
	// 有关如何处理指定名称的更多信息，请参阅 {@link #rollbackForClassName} 的说明。
	//
	// 类似于{@link org.springframework.transaction.interceptor.NoRollbackRuleAttribute
	// #NoRollbackRuleAttribute(String exceptionName)}。
	String[] noRollbackForClassName() default {};

}
