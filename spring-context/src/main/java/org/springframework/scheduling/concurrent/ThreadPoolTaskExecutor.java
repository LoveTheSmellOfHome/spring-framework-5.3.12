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

package org.springframework.scheduling.concurrent;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.springframework.core.task.AsyncListenableTaskExecutor;
import org.springframework.core.task.TaskDecorator;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.SchedulingTaskExecutor;
import org.springframework.util.Assert;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureTask;

/**
 * JavaBean that allows for configuring a {@link java.util.concurrent.ThreadPoolExecutor}
 * in bean style (through its "corePoolSize", "maxPoolSize", "keepAliveSeconds", "queueCapacity"
 * properties) and exposing it as a Spring {@link org.springframework.core.task.TaskExecutor}.
 * This class is also well suited for management and monitoring (e.g. through JMX),
 * providing several useful attributes: "corePoolSize", "maxPoolSize", "keepAliveSeconds"
 * (all supporting updates at runtime); "poolSize", "activeCount" (for introspection only).
 *
 * <p>The default configuration is a core pool size of 1, with unlimited max pool size
 * and unlimited queue capacity. This is roughly equivalent to
 * {@link java.util.concurrent.Executors#newSingleThreadExecutor()}, sharing a single
 * thread for all tasks. Setting {@link #setQueueCapacity "queueCapacity"} to 0 mimics
 * {@link java.util.concurrent.Executors#newCachedThreadPool()}, with immediate scaling
 * of threads in the pool to a potentially very high number. Consider also setting a
 * {@link #setMaxPoolSize "maxPoolSize"} at that point, as well as possibly a higher
 * {@link #setCorePoolSize "corePoolSize"} (see also the
 * {@link #setAllowCoreThreadTimeOut "allowCoreThreadTimeOut"} mode of scaling).
 *
 * <p><b>NOTE:</b> This class implements Spring's
 * {@link org.springframework.core.task.TaskExecutor} interface as well as the
 * {@link java.util.concurrent.Executor} interface, with the former being the primary
 * interface, the other just serving as secondary convenience. For this reason, the
 * exception handling follows the TaskExecutor contract rather than the Executor contract,
 * in particular regarding the {@link org.springframework.core.task.TaskRejectedException}.
 *
 * <p>For an alternative, you may set up a ThreadPoolExecutor instance directly using
 * constructor injection, or use a factory method definition that points to the
 * {@link java.util.concurrent.Executors} class. To expose such a raw Executor as a
 * Spring {@link org.springframework.core.task.TaskExecutor}, simply wrap it with a
 * {@link org.springframework.scheduling.concurrent.ConcurrentTaskExecutor} adapter.
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see org.springframework.core.task.TaskExecutor
 * @see java.util.concurrent.ThreadPoolExecutor
 * @see ThreadPoolExecutorFactoryBean
 * @see ConcurrentTaskExecutor
 */
// JavaBean 允许以 bean 样式配置ThreadPoolExecutor （通过其“corePoolSize”、“maxPoolSize”、“keepAliveSeconds”、
// “queueCapacity”属性）并将其公开为 Spring org.springframework.core.task.TaskExecutor 。
// 这个类也非常适合管理和监视（例如通过 JMX），提供了几个有用的属性：“corePoolSize”、“maxPoolSize”、
// “keepAliveSeconds”（所有支持在运行时更新）； “poolSize”、“activeCount”（仅用于自省）。
//
// 默认配置是核心池大小为 1，最大池大小无限制，队列容量无限制。这大致相当于java.util.concurrent.Executors.
// newSingleThreadExecutor() ，为所有任务共享一个线程。将"queueCapacity"设置为 0 模仿
// java.util.concurrent.Executors.newCachedThreadPool() ，池中的线程立即缩放到可能非常高的数量。还可以考虑在该点
// 设置 "maxPoolSize" ，以及可能更高的"corePoolSize" （另请参见"allowCoreThreadTimeOut"缩放模式）。
//
// 注意：这个类实现了 Spring 的org.springframework.core.task.TaskExecutor 接口以及 Executor 接口，前者是主要接口，
// 另一个只是辅助便利。出于这个原因，异常处理遵循 TaskExecutor 合同而不是 Executor 合同，特别是关于TaskRejectedException 。
//
// 作为替代方案，您可以使用构造函数注入直接设置 ThreadPoolExecutor 实例，或者使用指向 java.util.concurrent.Executors
// 类的工厂方法定义。要将这样的原始 Executor 公开为 Spring org.springframework.core.task.TaskExecutor ，
// 只需用 ConcurrentTaskExecutor 适配器包装它。
@SuppressWarnings("serial")
public class ThreadPoolTaskExecutor extends ExecutorConfigurationSupport
		implements AsyncListenableTaskExecutor, SchedulingTaskExecutor {

	// 设置线程池大小监视器
	private final Object poolSizeMonitor = new Object();

	// 默认线程池大小
	private int corePoolSize = 1;

	private int maxPoolSize = Integer.MAX_VALUE;

	private int keepAliveSeconds = 60;

	private int queueCapacity = Integer.MAX_VALUE;

	// 是否允许核心线程超时
	private boolean allowCoreThreadTimeOut = false;

	@Nullable
	private TaskDecorator taskDecorator;

	@Nullable
	private ThreadPoolExecutor threadPoolExecutor;

	// Runnable decorator to user-level FutureTask, if different
	private final Map<Runnable, Object> decoratedTaskMap =
			new ConcurrentReferenceHashMap<>(16, ConcurrentReferenceHashMap.ReferenceType.WEAK);


	/**
	 * Set the ThreadPoolExecutor's core pool size.
	 * Default is 1.
	 * <p><b>This setting can be modified at runtime, for example through JMX.</b>
	 */
	// 设置 ThreadPoolExecutor 的核心池大小。默认值为 1。
	// 此设置可以在运行时修改，例如通过 JMX。
	public void setCorePoolSize(int corePoolSize) {
		synchronized (this.poolSizeMonitor) {
			if (this.threadPoolExecutor != null) {
				this.threadPoolExecutor.setCorePoolSize(corePoolSize);
			}
			this.corePoolSize = corePoolSize;
		}
	}

	/**
	 * Return the ThreadPoolExecutor's core pool size.
	 */
	// 返回 ThreadPoolExecutor 的核心池大小。
	public int getCorePoolSize() {
		synchronized (this.poolSizeMonitor) {
			return this.corePoolSize;
		}
	}

	/**
	 * Set the ThreadPoolExecutor's maximum pool size.
	 * Default is {@code Integer.MAX_VALUE}.
	 * <p><b>This setting can be modified at runtime, for example through JMX.</b>
	 */
	// 设置 ThreadPoolExecutor 的最大池大小。默认为Integer.MAX_VALUE 。
	// 此设置可以在运行时修改，例如通过 JMX
	public void setMaxPoolSize(int maxPoolSize) {
		synchronized (this.poolSizeMonitor) {
			if (this.threadPoolExecutor != null) {
				this.threadPoolExecutor.setMaximumPoolSize(maxPoolSize);
			}
			this.maxPoolSize = maxPoolSize;
		}
	}

	/**
	 * Return the ThreadPoolExecutor's maximum pool size.
	 */
	// 返回 ThreadPoolExecutor 的最大池大小。
	public int getMaxPoolSize() {
		synchronized (this.poolSizeMonitor) {
			return this.maxPoolSize;
		}
	}

	/**
	 * Set the ThreadPoolExecutor's keep-alive seconds.
	 * Default is 60.
	 * <p><b>This setting can be modified at runtime, for example through JMX.</b>
	 */
	// 设置 ThreadPoolExecutor 的 keep-alive 秒数。默认值为 60。
	// 此设置可以在运行时修改，例如通过 JMX。
	public void setKeepAliveSeconds(int keepAliveSeconds) {
		synchronized (this.poolSizeMonitor) {
			if (this.threadPoolExecutor != null) {
				this.threadPoolExecutor.setKeepAliveTime(keepAliveSeconds, TimeUnit.SECONDS);
			}
			this.keepAliveSeconds = keepAliveSeconds;
		}
	}

	/**
	 * Return the ThreadPoolExecutor's keep-alive seconds.
	 */
	// 返回 ThreadPoolExecutor 的 keep-alive 秒数
	public int getKeepAliveSeconds() {
		synchronized (this.poolSizeMonitor) {
			return this.keepAliveSeconds;
		}
	}

	/**
	 * Set the capacity for the ThreadPoolExecutor's BlockingQueue.
	 * Default is {@code Integer.MAX_VALUE}.
	 * <p>Any positive value will lead to a LinkedBlockingQueue instance;
	 * any other value will lead to a SynchronousQueue instance.
	 * @see java.util.concurrent.LinkedBlockingQueue
	 * @see java.util.concurrent.SynchronousQueue
	 */
	// 设置 ThreadPoolExecutor 的 BlockingQueue 的容量。默认为 Integer.MAX_VALUE 。
	// 任何正值都会导致 LinkedBlockingQueue 实例；任何其他值都将导致 SynchronousQueue 实例
	public void setQueueCapacity(int queueCapacity) {
		this.queueCapacity = queueCapacity;
	}

	/**
	 * Specify whether to allow core threads to time out. This enables dynamic
	 * growing and shrinking even in combination with a non-zero queue (since
	 * the max pool size will only grow once the queue is full).
	 * <p>Default is "false".
	 * @see java.util.concurrent.ThreadPoolExecutor#allowCoreThreadTimeOut(boolean)
	 */
	// 指定是否允许核心线程超时。即使与非零队列结合使用，这也可以实现动态增长和收缩（因为最大池大小只会在队列满时才会增长）。
	// 默认为“假”。
	public void setAllowCoreThreadTimeOut(boolean allowCoreThreadTimeOut) {
		this.allowCoreThreadTimeOut = allowCoreThreadTimeOut;
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
	 * See the {@code ThreadPoolExecutor#afterExecute} javadoc for an example
	 * of how to access exceptions in such a {@code Future} case.
	 * @since 4.3
	 */
	// 指定一个自定义TaskDecorator以应用于任何即将执行的Runnable 。
	//
	// 请注意，这样的装饰器不一定应用于用户提供的Runnable / Callable ，而是应用于实际的执行回调（可能是用户提供的任务的包装器）。
	//
	// 主要用例是围绕任务的调用设置一些执行上下文，或者为任务执行提供一些监控/统计。
	//
	// 注意： TaskDecorator 实现中的异常处理仅限于通过 execute 调用执行的普通 Runnable 执行。
	// 在#submit调用的情况下，暴露的 Runnable 将是一个 FutureTask ，它不会传播任何异常；您可能必须强制转换它
	// 并调用 Future#get 来评估异常。请参阅ThreadPoolExecutor#afterExecute javadoc，了解如何在此类Future情况下访问异常的示例。
	public void setTaskDecorator(TaskDecorator taskDecorator) {
		this.taskDecorator = taskDecorator;
	}


	/**
	 * Note: This method exposes an {@link ExecutorService} to its base class
	 * but stores the actual {@link ThreadPoolExecutor} handle internally.
	 * Do not override this method for replacing the executor, rather just for
	 * decorating its {@code ExecutorService} handle or storing custom state.
	 */
	// 注意：此方法向其基类公开 ExecutorService ，但在内部存储实际的 ThreadPoolExecutor 句柄。不要重写此方法来替换执行器，
	// 而只是为了装饰其 ExecutorService 句柄或存储自定义状态。
	@Override
	protected ExecutorService initializeExecutor(
			ThreadFactory threadFactory, RejectedExecutionHandler rejectedExecutionHandler) {

		BlockingQueue<Runnable> queue = createQueue(this.queueCapacity);

		ThreadPoolExecutor executor;
		if (this.taskDecorator != null) {
			executor = new ThreadPoolExecutor(
					this.corePoolSize, this.maxPoolSize, this.keepAliveSeconds, TimeUnit.SECONDS,
					queue, threadFactory, rejectedExecutionHandler) {
				@Override
				public void execute(Runnable command) {
					Runnable decorated = taskDecorator.decorate(command);
					if (decorated != command) {
						decoratedTaskMap.put(decorated, command);
					}
					super.execute(decorated);
				}
			};
		}
		else {
			executor = new ThreadPoolExecutor(
					this.corePoolSize, this.maxPoolSize, this.keepAliveSeconds, TimeUnit.SECONDS,
					queue, threadFactory, rejectedExecutionHandler);

		}

		if (this.allowCoreThreadTimeOut) {
			executor.allowCoreThreadTimeOut(true);
		}

		this.threadPoolExecutor = executor;
		return executor;
	}

	/**
	 * Create the BlockingQueue to use for the ThreadPoolExecutor.
	 * <p>A LinkedBlockingQueue instance will be created for a positive
	 * capacity value; a SynchronousQueue else.
	 * @param queueCapacity the specified queue capacity
	 * @return the BlockingQueue instance
	 * @see java.util.concurrent.LinkedBlockingQueue
	 * @see java.util.concurrent.SynchronousQueue
	 */
	// 创建用于 ThreadPoolExecutor 的 BlockingQueue。
	// 将为正容量值创建一个 LinkedBlockingQueue 实例；一个 SynchronousQueue 其他。
	// 参形：
	//			queueCapacity – 指定的队列容量
	// 返回值：
	//			BlockingQueue 实例
	protected BlockingQueue<Runnable> createQueue(int queueCapacity) {
		if (queueCapacity > 0) {
			return new LinkedBlockingQueue<>(queueCapacity);
		}
		else {
			return new SynchronousQueue<>();
		}
	}

	/**
	 * Return the underlying ThreadPoolExecutor for native access.
	 * @return the underlying ThreadPoolExecutor (never {@code null})
	 * @throws IllegalStateException if the ThreadPoolTaskExecutor hasn't been initialized yet
	 */
	// 返回底层 ThreadPoolExecutor 以进行本机访问。
	// 返回值：
	//			底层 ThreadPoolExecutor（从不为null ）
	// 抛出：
	//			IllegalStateException – 如果 ThreadPoolTaskExecutor 尚未初始化
	public ThreadPoolExecutor getThreadPoolExecutor() throws IllegalStateException {
		Assert.state(this.threadPoolExecutor != null, "ThreadPoolTaskExecutor not initialized");
		return this.threadPoolExecutor;
	}

	/**
	 * Return the current pool size.
	 * @see java.util.concurrent.ThreadPoolExecutor#getPoolSize()
	 */
	// 返回当前池大小
	public int getPoolSize() {
		if (this.threadPoolExecutor == null) {
			// Not initialized yet: assume core pool size.
			// 尚未初始化：假设核心池大小
			return this.corePoolSize;
		}
		return this.threadPoolExecutor.getPoolSize();
	}

	/**
	 * Return the number of currently active threads.
	 * @see java.util.concurrent.ThreadPoolExecutor#getActiveCount()
	 */
	// 返回当前活动线程的数量
	public int getActiveCount() {
		if (this.threadPoolExecutor == null) {
			// Not initialized yet: assume no active threads.
			// 尚未初始化：假设没有活动线程
			return 0;
		}
		return this.threadPoolExecutor.getActiveCount();
	}


	@Override
	public void execute(Runnable task) {
		Executor executor = getThreadPoolExecutor();
		try {
			executor.execute(task);
		}
		catch (RejectedExecutionException ex) {
			throw new TaskRejectedException("Executor [" + executor + "] did not accept task: " + task, ex);
		}
	}

	@Override
	public void execute(Runnable task, long startTimeout) {
		execute(task);
	}

	@Override
	public Future<?> submit(Runnable task) {
		ExecutorService executor = getThreadPoolExecutor();
		try {
			return executor.submit(task);
		}
		catch (RejectedExecutionException ex) {
			throw new TaskRejectedException("Executor [" + executor + "] did not accept task: " + task, ex);
		}
	}

	@Override
	public <T> Future<T> submit(Callable<T> task) {
		ExecutorService executor = getThreadPoolExecutor();
		try {
			return executor.submit(task);
		}
		catch (RejectedExecutionException ex) {
			throw new TaskRejectedException("Executor [" + executor + "] did not accept task: " + task, ex);
		}
	}

	@Override
	public ListenableFuture<?> submitListenable(Runnable task) {
		ExecutorService executor = getThreadPoolExecutor();
		try {
			ListenableFutureTask<Object> future = new ListenableFutureTask<>(task, null);
			executor.execute(future);
			return future;
		}
		catch (RejectedExecutionException ex) {
			throw new TaskRejectedException("Executor [" + executor + "] did not accept task: " + task, ex);
		}
	}

	@Override
	public <T> ListenableFuture<T> submitListenable(Callable<T> task) {
		ExecutorService executor = getThreadPoolExecutor();
		try {
			ListenableFutureTask<T> future = new ListenableFutureTask<>(task);
			executor.execute(future);
			return future;
		}
		catch (RejectedExecutionException ex) {
			throw new TaskRejectedException("Executor [" + executor + "] did not accept task: " + task, ex);
		}
	}

	@Override
	protected void cancelRemainingTask(Runnable task) {
		super.cancelRemainingTask(task);
		// Cancel associated user-level Future handle as well
		Object original = this.decoratedTaskMap.get(task);
		if (original instanceof Future) {
			((Future<?>) original).cancel(true);
		}
	}

}
