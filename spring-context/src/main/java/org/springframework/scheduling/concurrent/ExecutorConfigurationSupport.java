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

package org.springframework.scheduling.concurrent;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.lang.Nullable;

/**
 * Base class for setting up a {@link java.util.concurrent.ExecutorService}
 * (typically a {@link java.util.concurrent.ThreadPoolExecutor} or
 * {@link java.util.concurrent.ScheduledThreadPoolExecutor}).
 * Defines common configuration settings and common lifecycle handling.
 *
 * @author Juergen Hoeller
 * @since 3.0
 * @see java.util.concurrent.ExecutorService
 * @see java.util.concurrent.Executors
 * @see java.util.concurrent.ThreadPoolExecutor
 * @see java.util.concurrent.ScheduledThreadPoolExecutor
 */
// 用于设置 ExecutorService 的基类（通常是 ThreadPoolExecutor 或
// java.util.concurrent.ScheduledThreadPoolExecutor ）。
// 定义通用配置设置和通用生命周期处理。
@SuppressWarnings("serial")
public abstract class ExecutorConfigurationSupport extends CustomizableThreadFactory
		implements BeanNameAware, InitializingBean, DisposableBean {

	protected final Log logger = LogFactory.getLog(getClass());

	private ThreadFactory threadFactory = this;

	private boolean threadNamePrefixSet = false;

	// 拒绝策略：无法由ThreadPoolExecutor执行的任务的处理程序
	private RejectedExecutionHandler rejectedExecutionHandler = new ThreadPoolExecutor.AbortPolicy();

	private boolean waitForTasksToCompleteOnShutdown = false;

	private long awaitTerminationMillis = 0;

	@Nullable
	private String beanName;

	@Nullable
	private ExecutorService executor;


	/**
	 * Set the ThreadFactory to use for the ExecutorService's thread pool.
	 * Default is the underlying ExecutorService's default thread factory.
	 * <p>In a Java EE 7 or other managed environment with JSR-236 support,
	 * consider specifying a JNDI-located ManagedThreadFactory: by default,
	 * to be found at "java:comp/DefaultManagedThreadFactory".
	 * Use the "jee:jndi-lookup" namespace element in XML or the programmatic
	 * {@link org.springframework.jndi.JndiLocatorDelegate} for convenient lookup.
	 * Alternatively, consider using Spring's {@link DefaultManagedAwareThreadFactory}
	 * with its fallback to local threads in case of no managed thread factory found.
	 * @see java.util.concurrent.Executors#defaultThreadFactory()
	 * @see javax.enterprise.concurrent.ManagedThreadFactory
	 * @see DefaultManagedAwareThreadFactory
	 */
	// 设置 ThreadFactory 以用于 ExecutorService 的线程池。 Default 是底层 ExecutorService 的默认线程工厂。
	// 在 Java EE 7 或其他支持 JSR-236 的托管环境中，考虑指定一个位于 JNDI 的
	// ManagedThreadFactory：默认情况下，可以在 “java:comp/DefaultManagedThreadFactory” 中找到。
	// 使用 XML 中的“jee:jndi-lookup” 命名空间元素或编程的 org.springframework.jndi.JndiLocatorDelegate 方便查找。
	// 或者，考虑使用 Spring 的 DefaultManagedAwareThreadFactory 及其回退到本地线程，以防找不到托管线程工厂。
	public void setThreadFactory(@Nullable ThreadFactory threadFactory) {
		this.threadFactory = (threadFactory != null ? threadFactory : this);
	}

	@Override
	public void setThreadNamePrefix(@Nullable String threadNamePrefix) {
		super.setThreadNamePrefix(threadNamePrefix);
		this.threadNamePrefixSet = true;
	}

	/**
	 * Set the RejectedExecutionHandler to use for the ExecutorService.
	 * Default is the ExecutorService's default abort policy.
	 * @see java.util.concurrent.ThreadPoolExecutor.AbortPolicy
	 */
	// 设置 RejectedExecutionHandler 以用于 ExecutorService。 Default 是 ExecutorService 的默认中止策略。
	public void setRejectedExecutionHandler(@Nullable RejectedExecutionHandler rejectedExecutionHandler) {
		this.rejectedExecutionHandler =
				(rejectedExecutionHandler != null ? rejectedExecutionHandler : new ThreadPoolExecutor.AbortPolicy());
	}

	/**
	 * Set whether to wait for scheduled tasks to complete on shutdown,
	 * not interrupting running tasks and executing all tasks in the queue.
	 * <p>Default is "false", shutting down immediately through interrupting
	 * ongoing tasks and clearing the queue. Switch this flag to "true" if you
	 * prefer fully completed tasks at the expense of a longer shutdown phase.
	 * <p>Note that Spring's container shutdown continues while ongoing tasks
	 * are being completed. If you want this executor to block and wait for the
	 * termination of tasks before the rest of the container continues to shut
	 * down - e.g. in order to keep up other resources that your tasks may need -,
	 * set the {@link #setAwaitTerminationSeconds "awaitTerminationSeconds"}
	 * property instead of or in addition to this property.
	 * @see java.util.concurrent.ExecutorService#shutdown()
	 * @see java.util.concurrent.ExecutorService#shutdownNow()
	 */
	// 设置是否在关机时等待计划任务完成，不中断正在运行的任务并执行队列中的所有任务。
	//
	// 默认为“false”，通过中断正在进行的任务并清除队列立即关闭。如果您希望以更长的关闭阶段为代价来完成任务，
	// 请将此标志切换为“true”。
	//
	// 请注意，在完成正在进行的任务时，Spring 的容器关闭会继续。如果您希望此执行程序在容器的其余部分继续关闭
	// 之前阻止并等待任务终止 - 例如，为了保持您的任务可能需要的其他资源 - 设置"awaitTerminationSeconds"属性而不是或除了这个属性。
	public void setWaitForTasksToCompleteOnShutdown(boolean waitForJobsToCompleteOnShutdown) {
		this.waitForTasksToCompleteOnShutdown = waitForJobsToCompleteOnShutdown;
	}

	/**
	 * Set the maximum number of seconds that this executor is supposed to block
	 * on shutdown in order to wait for remaining tasks to complete their execution
	 * before the rest of the container continues to shut down. This is particularly
	 * useful if your remaining tasks are likely to need access to other resources
	 * that are also managed by the container.
	 * <p>By default, this executor won't wait for the termination of tasks at all.
	 * It will either shut down immediately, interrupting ongoing tasks and clearing
	 * the remaining task queue - or, if the
	 * {@link #setWaitForTasksToCompleteOnShutdown "waitForTasksToCompleteOnShutdown"}
	 * flag has been set to {@code true}, it will continue to fully execute all
	 * ongoing tasks as well as all remaining tasks in the queue, in parallel to
	 * the rest of the container shutting down.
	 * <p>In either case, if you specify an await-termination period using this property,
	 * this executor will wait for the given time (max) for the termination of tasks.
	 * As a rule of thumb, specify a significantly higher timeout here if you set
	 * "waitForTasksToCompleteOnShutdown" to {@code true} at the same time,
	 * since all remaining tasks in the queue will still get executed - in contrast
	 * to the default shutdown behavior where it's just about waiting for currently
	 * executing tasks that aren't reacting to thread interruption.
	 * @see #setAwaitTerminationMillis
	 * @see java.util.concurrent.ExecutorService#shutdown()
	 * @see java.util.concurrent.ExecutorService#awaitTermination
	 */
	// 设置此执行程序在关闭时应该阻止的最大秒数，以便在容器的其余部分继续关闭之前等待剩余任务完成执行。
	// 如果您的剩余任务可能需要访问也由容器管理的其他资源，这将特别有用。
	//
	// 默认情况下，此执行程序根本不会等待任务终止。它将立即关闭，中断正在进行的任务并清除剩余的任务队列 -
	// 或者，如果"waitForTasksToCompleteOnShutdown"标志已设置为true ，它将继续完全执行所有正在进行的任务以
	// 及队列中的所有剩余任务，与容器的其余部分关闭并行。
	//
	// 在任何一种情况下，如果您使用此属性指定等待终止期，则此执行程序将等待给定时间（最大值）以终止任务。根据经验，如果同时
	// 将“waitForTasksToCompleteOnShutdown”设置为 true ，请在此处指定显着更高的超时时间，因为队列中的所有剩余任务仍
	// 将执行 - 与默认关闭行为相反，它只是等待当前正在执行的任务对线程中断没有反应。
	public void setAwaitTerminationSeconds(int awaitTerminationSeconds) {
		this.awaitTerminationMillis = awaitTerminationSeconds * 1000L;
	}

	/**
	 * Variant of {@link #setAwaitTerminationSeconds} with millisecond precision.
	 * @since 5.2.4
	 * @see #setAwaitTerminationSeconds
	 */
	// setAwaitTerminationSeconds 的变体，精度为毫秒
	public void setAwaitTerminationMillis(long awaitTerminationMillis) {
		this.awaitTerminationMillis = awaitTerminationMillis;
	}

	@Override
	public void setBeanName(String name) {
		this.beanName = name;
	}


	/**
	 * Calls {@code initialize()} after the container applied all property values.
	 * @see #initialize()
	 */
	// 在容器应用所有属性值后调用initialize()
	@Override
	public void afterPropertiesSet() {
		initialize();
	}

	/**
	 * Set up the ExecutorService.
	 */
	// 设置 ExecutorService
	public void initialize() {
		if (logger.isDebugEnabled()) {
			logger.debug("Initializing ExecutorService" + (this.beanName != null ? " '" + this.beanName + "'" : ""));
		}
		if (!this.threadNamePrefixSet && this.beanName != null) {
			setThreadNamePrefix(this.beanName + "-");
		}
		this.executor = initializeExecutor(this.threadFactory, this.rejectedExecutionHandler);
	}

	/**
	 * Create the target {@link java.util.concurrent.ExecutorService} instance.
	 * Called by {@code afterPropertiesSet}.
	 * @param threadFactory the ThreadFactory to use
	 * @param rejectedExecutionHandler the RejectedExecutionHandler to use
	 * @return a new ExecutorService instance
	 * @see #afterPropertiesSet()
	 */
	// 创建目标ExecutorService实例。由afterPropertiesSet调用。
	// 参形：
	//			threadFactory – 要使用的 ThreadFactory
	//			deniedExecutionHandler – 要使用的 RejectedExecutionHandler
	// 返回值：
	//			一个新的 ExecutorService 实例
	protected abstract ExecutorService initializeExecutor(
			ThreadFactory threadFactory, RejectedExecutionHandler rejectedExecutionHandler);


	/**
	 * Calls {@code shutdown} when the BeanFactory destroys
	 * the task executor instance.
	 * @see #shutdown()
	 */
	// 当 BeanFactory 销毁任务执行器实例时调用shutdown
	@Override
	public void destroy() {
		shutdown();
	}

	/**
	 * Perform a shutdown on the underlying ExecutorService.
	 * @see java.util.concurrent.ExecutorService#shutdown()
	 * @see java.util.concurrent.ExecutorService#shutdownNow()
	 */
	// 对底层 ExecutorService 执行关闭。
	public void shutdown() {
		if (logger.isDebugEnabled()) {
			logger.debug("Shutting down ExecutorService" + (this.beanName != null ? " '" + this.beanName + "'" : ""));
		}
		if (this.executor != null) {
			if (this.waitForTasksToCompleteOnShutdown) {
				this.executor.shutdown();
			}
			else {
				for (Runnable remainingTask : this.executor.shutdownNow()) {
					cancelRemainingTask(remainingTask);
				}
			}
			awaitTerminationIfNecessary(this.executor);
		}
	}

	/**
	 * Cancel the given remaining task which never commended execution,
	 * as returned from {@link ExecutorService#shutdownNow()}.
	 * @param task the task to cancel (typically a {@link RunnableFuture})
	 * @since 5.0.5
	 * @see #shutdown()
	 * @see RunnableFuture#cancel(boolean)
	 */
	// 取消从ExecutorService.shutdownNow()返回的从未推荐执行的给定剩余任务。
	// 参形：
	//			task – 要取消的任务（通常是RunnableFuture ）
	protected void cancelRemainingTask(Runnable task) {
		if (task instanceof Future) {
			((Future<?>) task).cancel(true);
		}
	}

	/**
	 * Wait for the executor to terminate, according to the value of the
	 * {@link #setAwaitTerminationSeconds "awaitTerminationSeconds"} property.
	 */
	// 根据"awaitTerminationSeconds" 属性的值等待执行器终止。
	private void awaitTerminationIfNecessary(ExecutorService executor) {
		if (this.awaitTerminationMillis > 0) {
			try {
				if (!executor.awaitTermination(this.awaitTerminationMillis, TimeUnit.MILLISECONDS)) {
					if (logger.isWarnEnabled()) {
						logger.warn("Timed out while waiting for executor" +
								(this.beanName != null ? " '" + this.beanName + "'" : "") + " to terminate");
					}
				}
			}
			catch (InterruptedException ex) {
				if (logger.isWarnEnabled()) {
					logger.warn("Interrupted while waiting for executor" +
							(this.beanName != null ? " '" + this.beanName + "'" : "") + " to terminate");
				}
				// 中断线程
				Thread.currentThread().interrupt();
			}
		}
	}

}
