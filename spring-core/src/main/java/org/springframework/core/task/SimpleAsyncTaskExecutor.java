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

package org.springframework.core.task;

import java.io.Serializable;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ThreadFactory;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ConcurrencyThrottleSupport;
import org.springframework.util.CustomizableThreadCreator;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureTask;

/**
 * {@link TaskExecutor} implementation that fires up a new Thread for each task,
 * executing it asynchronously.
 *
 * <p>Supports limiting concurrent threads through the "concurrencyLimit"
 * bean property. By default, the number of concurrent threads is unlimited.
 *
 * <p><b>NOTE: This implementation does not reuse threads!</b> Consider a
 * thread-pooling TaskExecutor implementation instead, in particular for
 * executing a large number of short-lived tasks.
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see #setConcurrencyLimit
 * @see SyncTaskExecutor
 * @see org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
 * @see org.springframework.scheduling.commonj.WorkManagerTaskExecutor
 */
// TaskExecutor 实现为每个任务启动一个新线程，异步执行它。
// 支持通过“concurrencyLimit” bean 属性限制并发线程。默认情况下，并发线程数是无限的。
// 注意：此实现不重用线程！考虑一个线程池 TaskExecutor 实现，特别是用于执行大量短期任务。
@SuppressWarnings("serial")
public class SimpleAsyncTaskExecutor extends CustomizableThreadCreator
		implements AsyncListenableTaskExecutor, Serializable {

	/**
	 * Permit any number of concurrent invocations: that is, don't throttle concurrency.
	 * @see ConcurrencyThrottleSupport#UNBOUNDED_CONCURRENCY
	 */
	// 允许任意数量的并发调用：也就是说，不要限制并发
	public static final int UNBOUNDED_CONCURRENCY = ConcurrencyThrottleSupport.UNBOUNDED_CONCURRENCY;

	/**
	 * Switch concurrency 'off': that is, don't allow any concurrent invocations.
	 * @see ConcurrencyThrottleSupport#NO_CONCURRENCY
	 */
	// 关闭并发：即不允许任何并发调用。
	public static final int NO_CONCURRENCY = ConcurrencyThrottleSupport.NO_CONCURRENCY;


	/** Internal concurrency throttle used by this executor. */
	// 此执行程序使用的内部并发限制。
	private final ConcurrencyThrottleAdapter concurrencyThrottle = new ConcurrencyThrottleAdapter();

	// 线程工厂
	@Nullable
	private ThreadFactory threadFactory;

	// 任务装饰器
	@Nullable
	private TaskDecorator taskDecorator;


	/**
	 * Create a new SimpleAsyncTaskExecutor with default thread name prefix.
	 */
	// 使用默认线程名称前缀创建一个新的 SimpleAsyncTaskExecutor。
	public SimpleAsyncTaskExecutor() {
		super();
	}

	/**
	 * Create a new SimpleAsyncTaskExecutor with the given thread name prefix.
	 * @param threadNamePrefix the prefix to use for the names of newly created threads
	 */
	// 使用给定的线程名称前缀创建一个新的 SimpleAsyncTaskExecutor。
	// 参形：
	//			threadNamePrefix – 用于新创建线程名称的前缀
	public SimpleAsyncTaskExecutor(String threadNamePrefix) {
		super(threadNamePrefix);
	}

	/**
	 * Create a new SimpleAsyncTaskExecutor with the given external thread factory.
	 * @param threadFactory the factory to use for creating new Threads
	 */
	// 使用给定的外部线程工厂创建一个新的 SimpleAsyncTaskExecutor。
	// 参形：
	//			threadFactory – 用于创建新线程的工厂
	public SimpleAsyncTaskExecutor(ThreadFactory threadFactory) {
		this.threadFactory = threadFactory;
	}


	/**
	 * Specify an external factory to use for creating new Threads,
	 * instead of relying on the local properties of this executor.
	 * <p>You may specify an inner ThreadFactory bean or also a ThreadFactory reference
	 * obtained from JNDI (on a Java EE 6 server) or some other lookup mechanism.
	 * @see #setThreadNamePrefix
	 * @see #setThreadPriority
	 */
	// 指定用于创建新线程的外部工厂，而不是依赖此执行程序的本地属性。
	//
	// 您可以指定一个内部 ThreadFactory bean 或从 JNDI（在 Java EE 6 服务器上）
	// 或其他一些查找机制获得的 ThreadFactory 引用
	public void setThreadFactory(@Nullable ThreadFactory threadFactory) {
		this.threadFactory = threadFactory;
	}

	/**
	 * Return the external factory to use for creating new Threads, if any.
	 */
	// 返回外部工厂以用于创建新线程（如果有）
	@Nullable
	public final ThreadFactory getThreadFactory() {
		return this.threadFactory;
	}

	/**
	 * Specify a custom {@link TaskDecorator} to be applied to any {@link Runnable}
	 * about to be executed.
	 * <p>Note that such a decorator is not necessarily being applied to the
	 * user-supplied {@code Runnable}/{@code Callable} but rather to the actual
	 * execution callback (which may be a wrapper around the user-supplied task).
	 * <p>The primary use case is to set some execution context around the task's
	 * invocation, or to provide some monitoring/statistics for task execution.
	 * <p><b>NOTE:</b> Exception handling in {@code TaskDecorator} implementations
	 * is limited to plain {@code Runnable} execution via {@code execute} calls.
	 * In case of {@code #submit} calls, the exposed {@code Runnable} will be a
	 * {@code FutureTask} which does not propagate any exceptions; you might
	 * have to cast it and call {@code Future#get} to evaluate exceptions.
	 * @since 4.3
	 */
	// 指定一个自定义TaskDecorator以应用于任何即将执行的Runnable 。
	//
	// 请注意，这样的装饰器不一定应用于用户提供的Runnable / Callable ，
	// 而是应用于实际的执行回调（可能是用户提供的任务的包装器）。
	//
	// 主要用例是围绕任务的调用设置一些执行上下文，或者为任务执行提供一些监控/统计。
	//
	// 注意： TaskDecorator实现中的异常处理仅限于通过execute调用执行的普通Runnable执行。
	// 在#submit调用的情况下，暴露的Runnable将是一个FutureTask ，它不会传播任何异常；
	// 您可能必须强制转换它并调用Future#get来评估异常
	public final void setTaskDecorator(TaskDecorator taskDecorator) {
		this.taskDecorator = taskDecorator;
	}

	/**
	 * Set the maximum number of parallel accesses allowed.
	 * -1 indicates no concurrency limit at all.
	 * <p>In principle, this limit can be changed at runtime,
	 * although it is generally designed as a config time setting.
	 * NOTE: Do not switch between -1 and any concrete limit at runtime,
	 * as this will lead to inconsistent concurrency counts: A limit
	 * of -1 effectively turns off concurrency counting completely.
	 * @see #UNBOUNDED_CONCURRENCY
	 */
	// 设置允许的最大并行访问数。 -1 表示根本没有并发限制。
	//
	// 原则上，此限制可以在运行时更改，尽管它通常设计为配置时间设置。注意：不要在运行时在 -1 和任何具体限制之间切换，
	// 因为这会导致并发计数不一致：-1 限制有效地完全关闭并发计数
	public void setConcurrencyLimit(int concurrencyLimit) {
		this.concurrencyThrottle.setConcurrencyLimit(concurrencyLimit);
	}

	/**
	 * Return the maximum number of parallel accesses allowed.
	 */
	// 返回允许的最大并行访问数。
	public final int getConcurrencyLimit() {
		return this.concurrencyThrottle.getConcurrencyLimit();
	}

	/**
	 * Return whether this throttle is currently active.
	 * @return {@code true} if the concurrency limit for this instance is active
	 * @see #getConcurrencyLimit()
	 * @see #setConcurrencyLimit
	 */
	// 返回此油门当前是否处于活动状态。
	// 返回值：
	//			如果此实例的并发限制处于活动状态，则为 true
	public final boolean isThrottleActive() {
		return this.concurrencyThrottle.isThrottleActive();
	}


	/**
	 * Executes the given task, within a concurrency throttle
	 * if configured (through the superclass's settings).
	 * @see #doExecute(Runnable)
	 */
	// 如果已配置（通过超类的设置），则在并发限制内执行给定的任务。
	@Override
	public void execute(Runnable task) {
		execute(task, TIMEOUT_INDEFINITE);
	}

	/**
	 * Executes the given task, within a concurrency throttle
	 * if configured (through the superclass's settings).
	 * <p>Executes urgent tasks (with 'immediate' timeout) directly,
	 * bypassing the concurrency throttle (if active). All other
	 * tasks are subject to throttling.
	 * @see #TIMEOUT_IMMEDIATE
	 * @see #doExecute(Runnable)
	 */
	// 如果已配置（通过超类的设置），则在并发限制内执行给定的任务。
	// 绕过并发限制（如果处于活动状态）直接执行紧急任务（具有“立即”超时）。所有其他任务都受到限制
	@Override
	public void execute(Runnable task, long startTimeout) {
		Assert.notNull(task, "Runnable must not be null");
		Runnable taskToUse = (this.taskDecorator != null ? this.taskDecorator.decorate(task) : task);
		if (isThrottleActive() && startTimeout > TIMEOUT_IMMEDIATE) {
			this.concurrencyThrottle.beforeAccess();
			doExecute(new ConcurrencyThrottlingRunnable(taskToUse));
		}
		else {
			doExecute(taskToUse);
		}
	}

	// 提交任务
	@Override
	public Future<?> submit(Runnable task) {
		FutureTask<Object> future = new FutureTask<>(task, null);
		execute(future, TIMEOUT_INDEFINITE);
		return future;
	}

	@Override
	public <T> Future<T> submit(Callable<T> task) {
		FutureTask<T> future = new FutureTask<>(task);
		execute(future, TIMEOUT_INDEFINITE);
		return future;
	}

	@Override
	public ListenableFuture<?> submitListenable(Runnable task) {
		ListenableFutureTask<Object> future = new ListenableFutureTask<>(task, null);
		execute(future, TIMEOUT_INDEFINITE);
		return future;
	}

	@Override
	public <T> ListenableFuture<T> submitListenable(Callable<T> task) {
		ListenableFutureTask<T> future = new ListenableFutureTask<>(task);
		execute(future, TIMEOUT_INDEFINITE);
		return future;
	}

	/**
	 * Template method for the actual execution of a task.
	 * <p>The default implementation creates a new Thread and starts it.
	 * @param task the Runnable to execute
	 * @see #setThreadFactory
	 * @see #createThread
	 * @see java.lang.Thread#start()
	 */
	// 用于实际执行任务的模板方法。
	// 默认实现创建一个新线程并启动它。
	// 参形：
	//				task – 要执行的 Runnable
	protected void doExecute(Runnable task) {
		Thread thread = (this.threadFactory != null ? this.threadFactory.newThread(task) : createThread(task));
		thread.start();
	}


	/**
	 * Subclass of the general ConcurrencyThrottleSupport class,
	 * making {@code beforeAccess()} and {@code afterAccess()}
	 * visible to the surrounding class.
	 */
	// 通用 ConcurrencyThrottleSupport 类的子类，使 beforeAccess() 和 afterAccess() 对周围类可见。
	private static class ConcurrencyThrottleAdapter extends ConcurrencyThrottleSupport {

		@Override
		protected void beforeAccess() {
			super.beforeAccess();
		}

		@Override
		protected void afterAccess() {
			super.afterAccess();
		}
	}


	/**
	 * This Runnable calls {@code afterAccess()} after the
	 * target Runnable has finished its execution.
	 */
	// 此 Runnable 在目标 Runnable 完成执行后调用 afterAccess() 。
	private class ConcurrencyThrottlingRunnable implements Runnable {

		private final Runnable target;

		public ConcurrencyThrottlingRunnable(Runnable target) {
			this.target = target;
		}

		@Override
		public void run() {
			try {
				// 目标线程执行
				this.target.run();
			}
			finally {
				concurrencyThrottle.afterAccess();
			}
		}
	}

}
