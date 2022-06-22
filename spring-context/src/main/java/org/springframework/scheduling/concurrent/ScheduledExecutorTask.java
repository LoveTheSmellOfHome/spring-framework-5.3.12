/*
 * Copyright 2002-2017 the original author or authors.
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

import java.util.concurrent.TimeUnit;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * JavaBean that describes a scheduled executor task, consisting of the
 * {@link Runnable} and a delay plus period. The period needs to be specified;
 * there is no point in a default for it.
 *
 * <p>The {@link java.util.concurrent.ScheduledExecutorService} does not offer
 * more sophisticated scheduling options such as cron expressions.
 * Consider using {@link ThreadPoolTaskScheduler} for such needs.
 *
 * <p>Note that the {@link java.util.concurrent.ScheduledExecutorService} mechanism
 * uses a {@link Runnable} instance that is shared between repeated executions,
 * in contrast to Quartz which creates a new Job instance for each execution.
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see java.util.concurrent.ScheduledExecutorService#scheduleWithFixedDelay(java.lang.Runnable, long, long, java.util.concurrent.TimeUnit)
 * @see java.util.concurrent.ScheduledExecutorService#scheduleAtFixedRate(java.lang.Runnable, long, long, java.util.concurrent.TimeUnit)
 */
// JavaBean 描述了一个计划的执行任务，由Runnable和一个延迟加周期组成。需要指定期限；它的默认值没有意义。
//
// java.util.concurrent.ScheduledExecutorService 不提供更复杂的调度选项，例如 cron 表达式。考虑
// 使用 ThreadPoolTaskScheduler 来满足此类需求。
//
// 请注意， java.util.concurrent.ScheduledExecutorService 机制使用在重复执行之间共享的 Runnable 实例，
// 而 Quartz 为每次执行创建一个新的 Job 实例。
public class ScheduledExecutorTask {

	@Nullable
	private Runnable runnable;

	private long delay = 0;

	private long period = -1;

	private TimeUnit timeUnit = TimeUnit.MILLISECONDS;

	private boolean fixedRate = false;


	/**
	 * Create a new ScheduledExecutorTask,
	 * to be populated via bean properties.
	 * @see #setDelay
	 * @see #setPeriod
	 * @see #setFixedRate
	 */
	// 创建一个新的 ScheduledExecutorTask，通过 bean 属性填充
	public ScheduledExecutorTask() {
	}

	/**
	 * Create a new ScheduledExecutorTask, with default
	 * one-time execution without delay.
	 * @param executorTask the Runnable to schedule
	 */
	// 创建一个新的 ScheduledExecutorTask，默认一次性执行无延迟。
	// 参形：executorTask – 要调度的 Runnable
	public ScheduledExecutorTask(Runnable executorTask) {
		this.runnable = executorTask;
	}

	/**
	 * Create a new ScheduledExecutorTask, with default
	 * one-time execution with the given delay.
	 * @param executorTask the Runnable to schedule
	 * @param delay the delay before starting the task for the first time (ms)
	 */
	// 创建一个新的 ScheduledExecutorTask，默认一次性执行给定延迟。
	// 参形：
	//			executorTask – 要调度的 Runnable
	//			delay - 首次启动任务之前的延迟（毫秒）
	public ScheduledExecutorTask(Runnable executorTask, long delay) {
		this.runnable = executorTask;
		this.delay = delay;
	}

	/**
	 * Create a new ScheduledExecutorTask.
	 * @param executorTask the Runnable to schedule
	 * @param delay the delay before starting the task for the first time (ms)
	 * @param period the period between repeated task executions (ms)
	 * @param fixedRate whether to schedule as fixed-rate execution
	 */
	// 创建一个新的 ScheduledExecutorTask。
	// 参形：
	//			executorTask – 要调度的 Runnable
	//			delay - 首次启动任务之前的延迟（毫秒）
	//			period – 重复任务执行之间的时间间隔 (ms)
	//			fixedRate – 是否安排为固定利率执行
	public ScheduledExecutorTask(Runnable executorTask, long delay, long period, boolean fixedRate) {
		this.runnable = executorTask;
		this.delay = delay;
		this.period = period;
		this.fixedRate = fixedRate;
	}


	/**
	 * Set the Runnable to schedule as executor task.
	 */
	// 将 Runnable 设置为调度为 executor 任务。
	public void setRunnable(Runnable executorTask) {
		this.runnable = executorTask;
	}

	/**
	 * Return the Runnable to schedule as executor task.
	 */
	// 返回 Runnable 以调度为 executor 任务
	public Runnable getRunnable() {
		Assert.state(this.runnable != null, "No Runnable set");
		return this.runnable;
	}

	/**
	 * Set the delay before starting the task for the first time,
	 * in milliseconds. Default is 0, immediately starting the
	 * task after successful scheduling.
	 */
	// 设置第一次启动任务前的延迟，以毫秒为单位。默认为0，调度成功后立即启动任务
	public void setDelay(long delay) {
		this.delay = delay;
	}

	/**
	 * Return the delay before starting the job for the first time.
	 */
	// 在第一次开始作业之前返回延迟
	public long getDelay() {
		return this.delay;
	}

	/**
	 * Set the period between repeated task executions, in milliseconds.
	 * <p>Default is -1, leading to one-time execution. In case of a positive value,
	 * the task will be executed repeatedly, with the given interval in-between executions.
	 * <p>Note that the semantics of the period value vary between fixed-rate and
	 * fixed-delay execution.
	 * <p><b>Note:</b> A period of 0 (for example as fixed delay) is <i>not</i> supported,
	 * simply because {@code java.util.concurrent.ScheduledExecutorService} itself
	 * does not support it. Hence a value of 0 will be treated as one-time execution;
	 * however, that value should never be specified explicitly in the first place!
	 * @see #setFixedRate
	 * @see #isOneTimeTask()
	 * @see java.util.concurrent.ScheduledExecutorService#scheduleWithFixedDelay(Runnable, long, long, java.util.concurrent.TimeUnit)
	 */
	// 设置重复任务执行之间的时间间隔，以毫秒为单位。
	//
	// 默认为 -1，导致一次性执行。如果为正值，任务将重复执行，执行之间有给定的间隔。
	//
	// 请注意，周期值的语义在固定速率和固定延迟执行之间有所不同。
	//
	// 注意：不支持周期为 0（例如作为固定延迟），只是因为java.util.concurrent.ScheduledExecutorService
	// 本身不支持。因此，值 0 将被视为一次性执行；但是，首先不应该明确指定该值
	public void setPeriod(long period) {
		this.period = period;
	}

	/**
	 * Return the period between repeated task executions.
	 */
	// 返回重复任务执行之间的时间段。
	public long getPeriod() {
		return this.period;
	}

	/**
	 * Is this task only ever going to execute once?
	 * @return {@code true} if this task is only ever going to execute once
	 * @see #getPeriod()
	 */
	// 这个任务是否只会执行一次？
	// 返回值：
	//			如果此任务只执行一次，则为true
	public boolean isOneTimeTask() {
		return (this.period <= 0);
	}

	/**
	 * Specify the time unit for the delay and period values.
	 * Default is milliseconds ({@code TimeUnit.MILLISECONDS}).
	 * @see java.util.concurrent.TimeUnit#MILLISECONDS
	 * @see java.util.concurrent.TimeUnit#SECONDS
	 */
	// 指定延迟和周期值的时间单位。默认为毫秒（ TimeUnit.MILLISECONDS ）。
	public void setTimeUnit(@Nullable TimeUnit timeUnit) {
		this.timeUnit = (timeUnit != null ? timeUnit : TimeUnit.MILLISECONDS);
	}

	/**
	 * Return the time unit for the delay and period values.
	 */
	// 返回延迟和周期值的时间单位
	public TimeUnit getTimeUnit() {
		return this.timeUnit;
	}

	/**
	 * Set whether to schedule as fixed-rate execution, rather than
	 * fixed-delay execution. Default is "false", that is, fixed delay.
	 * <p>See ScheduledExecutorService javadoc for details on those execution modes.
	 * @see java.util.concurrent.ScheduledExecutorService#scheduleWithFixedDelay(java.lang.Runnable, long, long, java.util.concurrent.TimeUnit)
	 * @see java.util.concurrent.ScheduledExecutorService#scheduleAtFixedRate(java.lang.Runnable, long, long, java.util.concurrent.TimeUnit)
	 */
	// 设置是否调度为固定速率执行，而不是固定延迟执行。默认为“false”，即固定延迟。
	public void setFixedRate(boolean fixedRate) {
		this.fixedRate = fixedRate;
	}

	/**
	 * Return whether to schedule as fixed-rate execution.
	 */
	// 返回是否安排为固定速率执行。
	public boolean isFixedRate() {
		return this.fixedRate;
	}

}
