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

import org.springframework.core.task.TaskRejectedException;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.support.SimpleTriggerContext;
import org.springframework.scheduling.support.TaskUtils;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ErrorHandler;

import javax.enterprise.concurrent.LastExecution;
import javax.enterprise.concurrent.ManagedScheduledExecutorService;
import java.time.Clock;
import java.util.Date;
import java.util.concurrent.*;

/**
 * Adapter that takes a {@code java.util.concurrent.ScheduledExecutorService} and
 * exposes a Spring {@link org.springframework.scheduling.TaskScheduler} for it.
 * Extends {@link ConcurrentTaskExecutor} in order to implement the
 * {@link org.springframework.scheduling.SchedulingTaskExecutor} interface as well.
 *
 * <p>Autodetects a JSR-236 {@link javax.enterprise.concurrent.ManagedScheduledExecutorService}
 * in order to use it for trigger-based scheduling if possible, instead of Spring's
 * local trigger management which ends up delegating to regular delay-based scheduling
 * against the {@code java.util.concurrent.ScheduledExecutorService} API. For JSR-236 style
 * lookup in a Java EE 7 environment, consider using {@link DefaultManagedTaskScheduler}.
 *
 * <p>Note that there is a pre-built {@link ThreadPoolTaskScheduler} that allows for
 * defining a {@link java.util.concurrent.ScheduledThreadPoolExecutor} in bean style,
 * exposing it as a Spring {@link org.springframework.scheduling.TaskScheduler} directly.
 * This is a convenient alternative to a raw ScheduledThreadPoolExecutor definition with
 * a separate definition of the present adapter class.
 *
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @since 3.0
 * @see java.util.concurrent.ScheduledExecutorService
 * @see java.util.concurrent.ScheduledThreadPoolExecutor
 * @see java.util.concurrent.Executors
 * @see DefaultManagedTaskScheduler
 * @see ThreadPoolTaskScheduler
 */
// 采用 {@code java.util.concurrent.ScheduledExecutorService} 并为其公开
// Spring {@link org.springframework.scheduling.TaskScheduler} 的适配器。
// 扩展 {@link ConcurrentTaskExecutor} 以实现
// {@link org.springframework.scheduling.SchedulingTaskExecutor} 接口。
//
// <p>自动检测 JSR-236 {@link javax.enterprise.concurrent.ManagedScheduledExecutorService}
// 以便在可能的情况下将其用于基于触发器的调度，而不是 Spring 的本地触发器管理，
// 后者最终委托给常规的基于延迟的调度针对{@code java.util.concurrent.ScheduledExecutorService} API。
// 对于 Java EE 7 环境中的 JSR-236 样式查找，请考虑使用 {@link DefaultManagedTaskScheduler}。
//
// <p>请注意，有一个预构建的 {@link ThreadPoolTaskScheduler} 允许以 bean 样式定义
// {@link java.util.concurrent.ScheduledThreadPoolExecutor}，将其公开为 Spring
// {@link org.springframework.scheduling.TaskScheduler} 直接。这是原始
// ScheduledThreadPoolExecutor 定义的一种方便的替代方法，具有当前适配器类的单独定义。
//
// 并发异步任务调度器
public class ConcurrentTaskScheduler extends ConcurrentTaskExecutor implements TaskScheduler {

	@Nullable
	private static Class<?> managedScheduledExecutorServiceClass;

	static {
		try {
			managedScheduledExecutorServiceClass = ClassUtils.forName(
					"javax.enterprise.concurrent.ManagedScheduledExecutorService",
					ConcurrentTaskScheduler.class.getClassLoader());
		}
		catch (ClassNotFoundException ex) {
			// JSR-236 API not available...
			managedScheduledExecutorServiceClass = null;
		}
	}

	// 调度执行服务
	private ScheduledExecutorService scheduledExecutor;

	private boolean enterpriseConcurrentScheduler = false;

	@Nullable
	private ErrorHandler errorHandler;

	private Clock clock = Clock.systemDefaultZone();


	/**
	 * Create a new ConcurrentTaskScheduler,
	 * using a single thread executor as default.
	 * @see java.util.concurrent.Executors#newSingleThreadScheduledExecutor()
	 */
	// 创建一个新的 ConcurrentTaskScheduler，默认使用单线程执行器
	public ConcurrentTaskScheduler() {
		super();
		this.scheduledExecutor = initScheduledExecutor(null);
	}

	/**
	 * Create a new ConcurrentTaskScheduler, using the given
	 * {@link java.util.concurrent.ScheduledExecutorService} as shared delegate.
	 * <p>Autodetects a JSR-236 {@link javax.enterprise.concurrent.ManagedScheduledExecutorService}
	 * in order to use it for trigger-based scheduling if possible,
	 * instead of Spring's local trigger management.
	 * @param scheduledExecutor the {@link java.util.concurrent.ScheduledExecutorService}
	 * to delegate to for {@link org.springframework.scheduling.SchedulingTaskExecutor}
	 * as well as {@link TaskScheduler} invocations
	 */
	// 创建一个新的 ConcurrentTaskScheduler，使用给定的
	// {@link java.util.concurrent.ScheduledExecutorService} 作为共享委托。
	// <p>自动检测 JSR-236 {@link javax.enterprise.concurrent.ManagedScheduledExecutorService}，
	// 以便在可能的情况下将其用于基于触发器的调度，而不是 Spring 的本地触发器管理。
	// @param scheduledExecutor {@link java.util.concurrent.ScheduledExecutorService} 委托给
	// {@link org.springframework.scheduling.SchedulingTaskExecutor} 以及 {@link TaskScheduler} 调用
	public ConcurrentTaskScheduler(ScheduledExecutorService scheduledExecutor) {
		super(scheduledExecutor);
		this.scheduledExecutor = initScheduledExecutor(scheduledExecutor);
	}

	/**
	 * Create a new ConcurrentTaskScheduler, using the given {@link java.util.concurrent.Executor}
	 * and {@link java.util.concurrent.ScheduledExecutorService} as delegates.
	 * <p>Autodetects a JSR-236 {@link javax.enterprise.concurrent.ManagedScheduledExecutorService}
	 * in order to use it for trigger-based scheduling if possible,
	 * instead of Spring's local trigger management.
	 * @param concurrentExecutor the {@link java.util.concurrent.Executor} to delegate to
	 * for {@link org.springframework.scheduling.SchedulingTaskExecutor} invocations
	 * @param scheduledExecutor the {@link java.util.concurrent.ScheduledExecutorService}
	 * to delegate to for {@link TaskScheduler} invocations
	 */
	// 创建一个新的 ConcurrentTaskScheduler，使用给定的 {@link java.util.concurrent.Executor} 和
	// {@link java.util.concurrent.ScheduledExecutorService} 作为委托。
	// <p>自动检测 JSR-236 {@link javax.enterprise.concurrent.ManagedScheduledExecutorService}，
	// 以便在可能的情况下将其用于基于触发器的调度，而不是 Spring 的本地触发器管理。
	// @param concurrentExecutor {@link java.util.concurrent.Executor} 委托给
	// {@link org.springframework.scheduling.SchedulingTaskExecutor} 调用
	// @param scheduleExecutor {@link java.util.concurrent.ScheduledExecutorService} 委托给
	// {@link TaskScheduler} 调用
	public ConcurrentTaskScheduler(Executor concurrentExecutor, ScheduledExecutorService scheduledExecutor) {
		super(concurrentExecutor);
		this.scheduledExecutor = initScheduledExecutor(scheduledExecutor);
	}


	private ScheduledExecutorService initScheduledExecutor(@Nullable ScheduledExecutorService scheduledExecutor) {
		if (scheduledExecutor != null) {
			this.scheduledExecutor = scheduledExecutor;
			this.enterpriseConcurrentScheduler = (managedScheduledExecutorServiceClass != null &&
					managedScheduledExecutorServiceClass.isInstance(scheduledExecutor));
		}
		else {
			this.scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
			this.enterpriseConcurrentScheduler = false;
		}
		return this.scheduledExecutor;
	}

	/**
	 * Specify the {@link java.util.concurrent.ScheduledExecutorService} to delegate to.
	 * <p>Autodetects a JSR-236 {@link javax.enterprise.concurrent.ManagedScheduledExecutorService}
	 * in order to use it for trigger-based scheduling if possible,
	 * instead of Spring's local trigger management.
	 * <p>Note: This will only apply to {@link TaskScheduler} invocations.
	 * If you want the given executor to apply to
	 * {@link org.springframework.scheduling.SchedulingTaskExecutor} invocations
	 * as well, pass the same executor reference to {@link #setConcurrentExecutor}.
	 * @see #setConcurrentExecutor
	 */
	// 指定要委托给的 {@link java.util.concurrent.ScheduledExecutorService}。
	// <p>自动检测 JSR-236 {@link javax.enterprise.concurrent.ManagedScheduledExecutorService}，
	// 以便在可能的情况下将其用于基于触发器的调度，而不是 Spring 的本地触发器管理。
	// <p>注意：这仅适用于 {@link TaskScheduler} 调用。如果您希望给定的执行程序也适用于
	// {@link org.springframework.scheduling.SchedulingTaskExecutor} 调用，
	// 请将相同的执行程序引用传递给 {@link setConcurrentExecutor}。
	public void setScheduledExecutor(@Nullable ScheduledExecutorService scheduledExecutor) {
		initScheduledExecutor(scheduledExecutor);
	}

	/**
	 * Provide an {@link ErrorHandler} strategy.
	 */
	// 供 {@link ErrorHandler} 策略
	public void setErrorHandler(ErrorHandler errorHandler) {
		Assert.notNull(errorHandler, "ErrorHandler must not be null");
		this.errorHandler = errorHandler;
	}

	/**
	 * Set the clock to use for scheduling purposes.
	 * <p>The default clock is the system clock for the default time zone.
	 * @since 5.3
	 * @see Clock#systemDefaultZone()
	 */
	// 设置时钟以用于调度目的。
	// <p>默认时钟是默认时区的系统时钟。
	public void setClock(Clock clock) {
		this.clock = clock;
	}

	@Override
	public Clock getClock() {
		return this.clock;
	}


	@Override
	@Nullable
	public ScheduledFuture<?> schedule(Runnable task, Trigger trigger) {
		try {
			if (this.enterpriseConcurrentScheduler) {
				return new EnterpriseConcurrentTriggerScheduler().schedule(decorateTask(task, true), trigger);
			}
			else {
				ErrorHandler errorHandler =
						(this.errorHandler != null ? this.errorHandler : TaskUtils.getDefaultErrorHandler(true));
				return new ReschedulingRunnable(task, trigger, this.clock, this.scheduledExecutor, errorHandler).schedule();
			}
		}
		catch (RejectedExecutionException ex) {
			throw new TaskRejectedException("Executor [" + this.scheduledExecutor + "] did not accept task: " + task, ex);
		}
	}

	@Override
	public ScheduledFuture<?> schedule(Runnable task, Date startTime) {
		long initialDelay = startTime.getTime() - this.clock.millis();
		try {
			return this.scheduledExecutor.schedule(decorateTask(task, false), initialDelay, TimeUnit.MILLISECONDS);
		}
		catch (RejectedExecutionException ex) {
			throw new TaskRejectedException("Executor [" + this.scheduledExecutor + "] did not accept task: " + task, ex);
		}
	}

	@Override
	public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, Date startTime, long period) {
		long initialDelay = startTime.getTime() - this.clock.millis();
		try {
			return this.scheduledExecutor.scheduleAtFixedRate(decorateTask(task, true), initialDelay, period, TimeUnit.MILLISECONDS);
		}
		catch (RejectedExecutionException ex) {
			throw new TaskRejectedException("Executor [" + this.scheduledExecutor + "] did not accept task: " + task, ex);
		}
	}

	@Override
	public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, long period) {
		try {
			return this.scheduledExecutor.scheduleAtFixedRate(decorateTask(task, true), 0, period, TimeUnit.MILLISECONDS);
		}
		catch (RejectedExecutionException ex) {
			throw new TaskRejectedException("Executor [" + this.scheduledExecutor + "] did not accept task: " + task, ex);
		}
	}

	@Override
	public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, Date startTime, long delay) {
		long initialDelay = startTime.getTime() - this.clock.millis();
		try {
			return this.scheduledExecutor.scheduleWithFixedDelay(decorateTask(task, true), initialDelay, delay, TimeUnit.MILLISECONDS);
		}
		catch (RejectedExecutionException ex) {
			throw new TaskRejectedException("Executor [" + this.scheduledExecutor + "] did not accept task: " + task, ex);
		}
	}

	@Override
	public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, long delay) {
		try {
			return this.scheduledExecutor.scheduleWithFixedDelay(decorateTask(task, true), 0, delay, TimeUnit.MILLISECONDS);
		}
		catch (RejectedExecutionException ex) {
			throw new TaskRejectedException("Executor [" + this.scheduledExecutor + "] did not accept task: " + task, ex);
		}
	}

	private Runnable decorateTask(Runnable task, boolean isRepeatingTask) {
		Runnable result = TaskUtils.decorateTaskWithErrorHandler(task, this.errorHandler, isRepeatingTask);
		if (this.enterpriseConcurrentScheduler) {
			result = ManagedTaskBuilder.buildManagedTask(result, task.toString());
		}
		return result;
	}


	/**
	 * Delegate that adapts a Spring Trigger to a JSR-236 Trigger.
	 * Separated into an inner class in order to avoid a hard dependency on the JSR-236 API.
	 */
	// 使 Spring 触发器适应 JSR-236 触发器的委托。分离成一个内部类以避免对 JSR-236 API 的硬依赖。
	private class EnterpriseConcurrentTriggerScheduler {

		public ScheduledFuture<?> schedule(Runnable task, final Trigger trigger) {
			ManagedScheduledExecutorService executor = (ManagedScheduledExecutorService) scheduledExecutor;
			return executor.schedule(task, new javax.enterprise.concurrent.Trigger() {
				@Override
				@Nullable
				public Date getNextRunTime(@Nullable LastExecution le, Date taskScheduledTime) {
					return (trigger.nextExecutionTime(le != null ?
							new SimpleTriggerContext(le.getScheduledStart(), le.getRunStart(), le.getRunEnd()) :
							new SimpleTriggerContext()));
				}
				@Override
				public boolean skipRun(LastExecution lastExecution, Date scheduledRunTime) {
					return false;
				}
			});
		}
	}

}
