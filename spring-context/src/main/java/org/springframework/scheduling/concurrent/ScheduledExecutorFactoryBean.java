/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.scheduling.concurrent;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.support.DelegatingErrorHandlingRunnable;
import org.springframework.scheduling.support.TaskUtils;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

import java.util.concurrent.*;

/**
 * {@link org.springframework.beans.factory.FactoryBean} that sets up
 * a {@link java.util.concurrent.ScheduledExecutorService}
 * (by default: a {@link java.util.concurrent.ScheduledThreadPoolExecutor})
 * and exposes it for bean references.
 *
 * <p>Allows for registration of {@link ScheduledExecutorTask ScheduledExecutorTasks},
 * automatically starting the {@link ScheduledExecutorService} on initialization and
 * cancelling it on destruction of the context. In scenarios that only require static
 * registration of tasks at startup, there is no need to access the
 * {@link ScheduledExecutorService} instance itself in application code at all;
 * {@code ScheduledExecutorFactoryBean} is then just being used for lifecycle integration.
 *
 * <p>For an alternative, you may set up a {@link ScheduledThreadPoolExecutor} instance
 * directly using constructor injection, or use a factory method definition that points
 * to the {@link java.util.concurrent.Executors} class.
 * <b>This is strongly recommended in particular for common {@code @Bean} methods in
 * configuration classes, where this {@code FactoryBean} variant would force you to
 * return the {@code FactoryBean} type instead of {@code ScheduledExecutorService}.</b>
 *
 * <p>Note that {@link java.util.concurrent.ScheduledExecutorService}
 * uses a {@link Runnable} instance that is shared between repeated executions,
 * in contrast to Quartz which instantiates a new Job for each execution.
 *
 * <p><b>WARNING:</b> {@link Runnable Runnables} submitted via a native
 * {@link java.util.concurrent.ScheduledExecutorService} are removed from
 * the execution schedule once they throw an exception. If you would prefer
 * to continue execution after such an exception, switch this FactoryBean's
 * {@link #setContinueScheduledExecutionAfterException "continueScheduledExecutionAfterException"}
 * property to "true".
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see #setPoolSize
 * @see #setRemoveOnCancelPolicy
 * @see #setThreadFactory
 * @see ScheduledExecutorTask
 * @see java.util.concurrent.ScheduledExecutorService
 * @see java.util.concurrent.ScheduledThreadPoolExecutor
 */
// FactoryBean 设置 ScheduledExecutorService （默认情况下： ScheduledThreadPoolExecutor ）并将其公开以供 bean 引用。
//
// 允许注册 ScheduledExecutorTasks ，在初始化时自动启动 ScheduledExecutorService 并在销毁上下文时取消它。
// 在启动时只需要静态注册任务的场景下，完全不需要在应用代码中访问 ScheduledExecutorService 实例本身；
// 然后 ScheduledExecutorFactoryBean 仅用于生命周期集成。
//
// 作为替代方案，您可以使用构造函数注入直接设置 ScheduledThreadPoolExecutor 实例，或使用指向 Executors 类的工厂方法定义。
// 特别是对于配置类中的常见 @Bean方法，强烈建议这样做，这种 FactoryBean 变体将强制您返回 FactoryBean 类型
// 而不是 ScheduledExecutorService 。
//
// 请注意， ScheduledExecutorService 使用在重复执行之间共享的 Runnable 实例，而 Quartz 则为每次执行实例化一个新作业。
//
// 警告：一旦通过本机 ScheduledExecutorService 提交的 Runnables 抛出异常，它们就会从执行计划中删除。
// 如果您希望在此类异常后继续执行，请将此 FactoryBean 的 "continueScheduledExecutionAfterException" 属性切换为“true”。
@SuppressWarnings("serial")
public class ScheduledExecutorFactoryBean extends ExecutorConfigurationSupport
		implements FactoryBean<ScheduledExecutorService> {

	private int poolSize = 1;

	@Nullable
	private ScheduledExecutorTask[] scheduledExecutorTasks;

	private boolean removeOnCancelPolicy = false;

	private boolean continueScheduledExecutionAfterException = false;

	private boolean exposeUnconfigurableExecutor = false;

	@Nullable
	private ScheduledExecutorService exposedExecutor;


	/**
	 * Set the ScheduledExecutorService's pool size.
	 * Default is 1.
	 */
	// 设置 ScheduledExecutorService 的池大小。默认值为 1
	public void setPoolSize(int poolSize) {
		Assert.isTrue(poolSize > 0, "'poolSize' must be 1 or higher");
		this.poolSize = poolSize;
	}

	/**
	 * Register a list of ScheduledExecutorTask objects with the ScheduledExecutorService
	 * that this FactoryBean creates. Depending on each ScheduledExecutorTask's settings,
	 * it will be registered via one of ScheduledExecutorService's schedule methods.
	 * @see java.util.concurrent.ScheduledExecutorService#schedule(java.lang.Runnable, long, java.util.concurrent.TimeUnit)
	 * @see java.util.concurrent.ScheduledExecutorService#scheduleWithFixedDelay(java.lang.Runnable, long, long, java.util.concurrent.TimeUnit)
	 * @see java.util.concurrent.ScheduledExecutorService#scheduleAtFixedRate(java.lang.Runnable, long, long, java.util.concurrent.TimeUnit)
	 */
	// 使用此 FactoryBean 创建的 ScheduledExecutorService 注册 ScheduledExecutorTask 对象的列表。
	// 根据每个 ScheduledExecutorTask 的设置，它将通过 ScheduledExecutorService 的调度方法之一进行注册。
	public void setScheduledExecutorTasks(ScheduledExecutorTask... scheduledExecutorTasks) {
		this.scheduledExecutorTasks = scheduledExecutorTasks;
	}

	/**
	 * Set the remove-on-cancel mode on {@link ScheduledThreadPoolExecutor}.
	 * <p>Default is {@code false}. If set to {@code true}, the target executor will be
	 * switched into remove-on-cancel mode (if possible, with a soft fallback otherwise).
	 */
	// 在 ScheduledThreadPoolExecutor 上设置 remove-on-cancel 模式。
	// 默认为 false 。如果设置为true ，目标执行器将切换到取消时删除模式（如果可能，则使用软回退）。
	public void setRemoveOnCancelPolicy(boolean removeOnCancelPolicy) {
		this.removeOnCancelPolicy = removeOnCancelPolicy;
	}

	/**
	 * Specify whether to continue the execution of a scheduled task
	 * after it threw an exception.
	 * <p>Default is "false", matching the native behavior of a
	 * {@link java.util.concurrent.ScheduledExecutorService}.
	 * Switch this flag to "true" for exception-proof execution of each task,
	 * continuing scheduled execution as in the case of successful execution.
	 * @see java.util.concurrent.ScheduledExecutorService#scheduleAtFixedRate
	 */
	// 指定是否在抛出异常后继续执行计划任务。
	//
	// 默认为 “false”，匹配 ScheduledExecutorService 的本机行为。将此标志切换为 “true” 以防异常执行每个任务，
	// 并在成功执行的情况下继续计划执行。
	public void setContinueScheduledExecutionAfterException(boolean continueScheduledExecutionAfterException) {
		this.continueScheduledExecutionAfterException = continueScheduledExecutionAfterException;
	}

	/**
	 * Specify whether this FactoryBean should expose an unconfigurable
	 * decorator for the created executor.
	 * <p>Default is "false", exposing the raw executor as bean reference.
	 * Switch this flag to "true" to strictly prevent clients from
	 * modifying the executor's configuration.
	 * @see java.util.concurrent.Executors#unconfigurableScheduledExecutorService
	 */
	// 指定此 FactoryBean 是否应为创建的执行程序公开不可配置的装饰器。
	// 默认为 “false”，将原始执行程序公开为 bean 引用。将此标志切换为“true”以严格防止客户端修改执行器的配置。
	public void setExposeUnconfigurableExecutor(boolean exposeUnconfigurableExecutor) {
		this.exposeUnconfigurableExecutor = exposeUnconfigurableExecutor;
	}


	@Override
	protected ExecutorService initializeExecutor(
			ThreadFactory threadFactory, RejectedExecutionHandler rejectedExecutionHandler) {

		ScheduledExecutorService executor =
				createExecutor(this.poolSize, threadFactory, rejectedExecutionHandler);

		if (this.removeOnCancelPolicy) {
			if (executor instanceof ScheduledThreadPoolExecutor) {
				((ScheduledThreadPoolExecutor) executor).setRemoveOnCancelPolicy(true);
			}
			else {
				logger.debug("Could not apply remove-on-cancel policy - not a ScheduledThreadPoolExecutor");
			}
		}

		// Register specified ScheduledExecutorTasks, if necessary.
		// 如有必要，注册指定的 ScheduledExecutorTasks
		if (!ObjectUtils.isEmpty(this.scheduledExecutorTasks)) {
			registerTasks(this.scheduledExecutorTasks, executor);
		}

		// Wrap executor with an unconfigurable decorator.
		// 用不可配置的装饰器包装执行器
		this.exposedExecutor = (this.exposeUnconfigurableExecutor ?
				Executors.unconfigurableScheduledExecutorService(executor) : executor);

		return executor;
	}

	/**
	 * Create a new {@link ScheduledExecutorService} instance.
	 * <p>The default implementation creates a {@link ScheduledThreadPoolExecutor}.
	 * Can be overridden in subclasses to provide custom {@link ScheduledExecutorService} instances.
	 * @param poolSize the specified pool size
	 * @param threadFactory the ThreadFactory to use
	 * @param rejectedExecutionHandler the RejectedExecutionHandler to use
	 * @return a new ScheduledExecutorService instance
	 * @see #afterPropertiesSet()
	 * @see java.util.concurrent.ScheduledThreadPoolExecutor
	 */
	// 创建一个新的ScheduledExecutorService实例。
	// 默认实现创建一个 ScheduledThreadPoolExecutor 。可以在子类中重写以提供自定义 ScheduledExecutorService 实例。
	// 参形：
	//			poolSize – 指定的池大小
	//			threadFactory – 要使用的 ThreadFactory
	//			deniedExecutionHandler – 要使用的 RejectedExecutionHandler
	// 返回值：
	//			一个新的 ScheduledExecutorService 实例
	protected ScheduledExecutorService createExecutor(
			int poolSize, ThreadFactory threadFactory, RejectedExecutionHandler rejectedExecutionHandler) {

		return new ScheduledThreadPoolExecutor(poolSize, threadFactory, rejectedExecutionHandler);
	}

	/**
	 * Register the specified {@link ScheduledExecutorTask ScheduledExecutorTasks}
	 * on the given {@link ScheduledExecutorService}.
	 * @param tasks the specified ScheduledExecutorTasks (never empty)
	 * @param executor the ScheduledExecutorService to register the tasks on.
	 */
	// 在给定的 ScheduledExecutorTasks 上注册指定的 ScheduledExecutorService 。
	// 参形：
	//			tasks – 指定的 ScheduledExecutorTasks（从不为空）
	//			executor - 用于注册任务的 ScheduledExecutorService。
	protected void registerTasks(ScheduledExecutorTask[] tasks, ScheduledExecutorService executor) {
		for (ScheduledExecutorTask task : tasks) {
			Runnable runnable = getRunnableToSchedule(task);
			if (task.isOneTimeTask()) {
				executor.schedule(runnable, task.getDelay(), task.getTimeUnit());
			}
			else {
				if (task.isFixedRate()) {
					executor.scheduleAtFixedRate(runnable, task.getDelay(), task.getPeriod(), task.getTimeUnit());
				}
				else {
					executor.scheduleWithFixedDelay(runnable, task.getDelay(), task.getPeriod(), task.getTimeUnit());
				}
			}
		}
	}

	/**
	 * Determine the actual Runnable to schedule for the given task.
	 * <p>Wraps the task's Runnable in a
	 * {@link org.springframework.scheduling.support.DelegatingErrorHandlingRunnable}
	 * that will catch and log the Exception. If necessary, it will suppress the
	 * Exception according to the
	 * {@link #setContinueScheduledExecutionAfterException "continueScheduledExecutionAfterException"}
	 * flag.
	 * @param task the ScheduledExecutorTask to schedule
	 * @return the actual Runnable to schedule (may be a decorator)
	 */
	// 确定要为给定任务安排的实际 Runnable。
	//
	// 将任务的 Runnable 包装在DelegatingErrorHandlingRunnable中，它将捕获并记录异常。如有必要，它将
	// 根据"continueScheduledExecutionAfterException"标志抑制异常。
	//
	// 参形：
	//			task – 要调度的 ScheduledExecutorTask
	// 返回值：
	//			要调度的实际 Runnable（可能是装饰器）
	protected Runnable getRunnableToSchedule(ScheduledExecutorTask task) {
		return (this.continueScheduledExecutionAfterException ?
				new DelegatingErrorHandlingRunnable(task.getRunnable(), TaskUtils.LOG_AND_SUPPRESS_ERROR_HANDLER) :
				new DelegatingErrorHandlingRunnable(task.getRunnable(), TaskUtils.LOG_AND_PROPAGATE_ERROR_HANDLER));
	}


	@Override
	@Nullable
	public ScheduledExecutorService getObject() {
		return this.exposedExecutor;
	}

	@Override
	public Class<? extends ScheduledExecutorService> getObjectType() {
		return (this.exposedExecutor != null ? this.exposedExecutor.getClass() : ScheduledExecutorService.class);
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

}
