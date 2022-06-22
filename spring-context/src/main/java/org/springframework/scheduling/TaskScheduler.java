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

package org.springframework.scheduling;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.ScheduledFuture;

import org.springframework.lang.Nullable;

/**
 * Task scheduler interface that abstracts the scheduling of
 * {@link Runnable Runnables} based on different kinds of triggers.
 *
 * <p>This interface is separate from {@link SchedulingTaskExecutor} since it
 * usually represents for a different kind of backend, i.e. a thread pool with
 * different characteristics and capabilities. Implementations may implement
 * both interfaces if they can handle both kinds of execution characteristics.
 *
 * <p>The 'default' implementation is
 * {@link org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler},
 * wrapping a native {@link java.util.concurrent.ScheduledExecutorService}
 * and adding extended trigger capabilities.
 *
 * <p>This interface is roughly equivalent to a JSR-236
 * {@code ManagedScheduledExecutorService} as supported in Java EE 7
 * environments but aligned with Spring's {@code TaskExecutor} model.
 *
 * @author Juergen Hoeller
 * @since 3.0
 * @see org.springframework.core.task.TaskExecutor
 * @see java.util.concurrent.ScheduledExecutorService
 * @see org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler
 */
// Spring 任务调度器接口，基于不同类型的触发器抽象 {@link Runnable Runnables} 的调度。
//
// <p>这个接口与 {@link SchedulingTaskExecutor} 是分开的，因为它通常代表不同类型的后端，
// 即具有不同特性和功能的线程池。如果实现可以处理两种类型的执行特征，则它们可以实现两种接口。
//
// <p>“默认”实现是 {@link org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler}，
// 包装了一个原生的 {@link java.util.concurrent.ScheduledExecutorService} 并添加了扩展的触发器功能。
//
// <p>该接口大致相当于 Java EE 7 环境中支持的 JSR-236 {@code ManagedScheduledExecutorService}，
// 但与 Spring 的 {@code TaskExecutor} 模型一致
//
// Spring 的调度主要分在本地的调度，基于 Java JUC 线程池来进行实现
public interface TaskScheduler {

	/**
	 * Return the clock to use for scheduling purposes.
	 * @since 5.3
	 * @see Clock#systemDefaultZone()
	 */
	// 返回以用于调度目的的时钟
	default Clock getClock() {
		return Clock.systemDefaultZone();
	}

	/**
	 * Schedule the given {@link Runnable}, invoking it whenever the trigger
	 * indicates a next execution time.
	 * <p>Execution will end once the scheduler shuts down or the returned
	 * {@link ScheduledFuture} gets cancelled.
	 * @param task the Runnable to execute whenever the trigger fires
	 * @param trigger an implementation of the {@link Trigger} interface,
	 * e.g. a {@link org.springframework.scheduling.support.CronTrigger} object
	 * wrapping a cron expression
	 * @return a {@link ScheduledFuture} representing pending completion of the task,
	 * or {@code null} if the given Trigger object never fires (i.e. returns
	 * {@code null} from {@link Trigger#nextExecutionTime})
	 * @throws org.springframework.core.task.TaskRejectedException if the given task was not accepted
	 * for internal reasons (e.g. a pool overload handling policy or a pool shutdown in progress)
	 * @see org.springframework.scheduling.support.CronTrigger
	 */
	// 调度给定的 {@link Runnable}，在触发器指示下一次执行时间时调用它。
	// <p>一旦调度程序关闭或返回的 {@link ScheduledFuture} 被取消，执行将结束。
	//
	// @param task 每当触发器触发时执行的 Runnable
	// @param trigger {@link Trigger} 接口的实现，例如一个包含 cron 表达式的
	// {@link org.springframework.scheduling.support.CronTrigger} 对象
	// @return a {@link ScheduledFuture} 表示待完成的任务，或者 {@code null}
	// 如果给定的 Trigger 对象从不触发（即从 {@link TriggernextExecutionTime} 返回 {@code null}）
	// @throws org.springframework.core.task.TaskRejectedException
	// 如果给定的任务由于内部原因（例如池过载处理策略或正在进行的池关闭）而未被接受
	@Nullable
	ScheduledFuture<?> schedule(Runnable task, Trigger trigger);

	/**
	 * Schedule the given {@link Runnable}, invoking it at the specified execution time.
	 * <p>Execution will end once the scheduler shuts down or the returned
	 * {@link ScheduledFuture} gets cancelled.
	 * @param task the Runnable to execute whenever the trigger fires
	 * @param startTime the desired execution time for the task
	 * (if this is in the past, the task will be executed immediately, i.e. as soon as possible)
	 * @return a {@link ScheduledFuture} representing pending completion of the task
	 * @throws org.springframework.core.task.TaskRejectedException if the given task was not accepted
	 * for internal reasons (e.g. a pool overload handling policy or a pool shutdown in progress)
	 * @since 5.0
	 * @see #schedule(Runnable, Date)
	 */
	// 调度给定的 {@link Runnable}，在指定的执行时间调用它。
	// <p>一旦调度程序关闭或返回的 {@link ScheduledFuture} 被取消，执行将结束。
	// @param task 每当触发器触发时执行的 Runnable
	// @param startTime 任务所需的执行时间（如果这是过去，任务将立即执行，即尽快）
	// @return 一个 {@link ScheduledFuture} 代表待完成的任务
	// @throws org.springframework.core.task.TaskRejectedException 如果给定的任务由于内部原因（例如池过载处理策略或正在进行的池关闭）而未被接受
	default ScheduledFuture<?> schedule(Runnable task, Instant startTime) {
		return schedule(task, Date.from(startTime));
	}

	/**
	 * Schedule the given {@link Runnable}, invoking it at the specified execution time.
	 * <p>Execution will end once the scheduler shuts down or the returned
	 * {@link ScheduledFuture} gets cancelled.
	 * @param task the Runnable to execute whenever the trigger fires
	 * @param startTime the desired execution time for the task
	 * (if this is in the past, the task will be executed immediately, i.e. as soon as possible)
	 * @return a {@link ScheduledFuture} representing pending completion of the task
	 * @throws org.springframework.core.task.TaskRejectedException if the given task was not accepted
	 * for internal reasons (e.g. a pool overload handling policy or a pool shutdown in progress)
	 */
	// 调度给定的 {@link Runnable}，在指定的执行时间调用它。
	// <p>一旦调度程序关闭或返回的 {@link ScheduledFuture} 被取消，执行将结束
	// @param task 每当触发器触发时执行的 Runnable
	// @param startTime 任务所需的执行时间（如果这是过去，任务将立即执行，即尽快）
	// @return 一个 {@link ScheduledFuture} 代表待完成的任务
	// @throws org.springframework.core.task.TaskRejectedException 如果给定
	// 的任务由于内部原因（例如池过载处理策略或正在进行的池关闭）而未被接受
	ScheduledFuture<?> schedule(Runnable task, Date startTime);

	/**
	 * Schedule the given {@link Runnable}, invoking it at the specified execution time
	 * and subsequently with the given period.
	 * <p>Execution will end once the scheduler shuts down or the returned
	 * {@link ScheduledFuture} gets cancelled.
	 * @param task the Runnable to execute whenever the trigger fires
	 * @param startTime the desired first execution time for the task
	 * (if this is in the past, the task will be executed immediately, i.e. as soon as possible)
	 * @param period the interval between successive executions of the task
	 * @return a {@link ScheduledFuture} representing pending completion of the task
	 * @throws org.springframework.core.task.TaskRejectedException if  the given task was not accepted
	 * for internal reasons (e.g. a pool overload handling policy or a pool shutdown in progress)
	 * @since 5.0
	 * @see #scheduleAtFixedRate(Runnable, Date, long)
	 */
	// 调度给定的 {@link Runnable}，在指定的执行时间调用它，随后在给定的时间段内调用它。
	// <p>一旦调度程序关闭或返回的 {@link ScheduledFuture} 被取消，执行将结束。
	// @param task 每当触发器触发时执行的 Runnable
	// @param startTime 任务所需的第一次执行时间（如果这是过去，则任务将立即执行，即尽快执行）
	// @param period 任务连续执行之间的间隔
	// @return 一个 {@link ScheduledFuture} 代表待完成的任务
	// @throws org.springframework.core.task.TaskRejectedException 如果给定的任务由于
	// 内部原因（例如池过载处理策略或正在进行的池关闭）而未被接受
	default ScheduledFuture<?> scheduleAtFixedRate(Runnable task, Instant startTime, Duration period) {
		return scheduleAtFixedRate(task, Date.from(startTime), period.toMillis());
	}

	/**
	 * Schedule the given {@link Runnable}, invoking it at the specified execution time
	 * and subsequently with the given period.
	 * <p>Execution will end once the scheduler shuts down or the returned
	 * {@link ScheduledFuture} gets cancelled.
	 * @param task the Runnable to execute whenever the trigger fires
	 * @param startTime the desired first execution time for the task
	 * (if this is in the past, the task will be executed immediately, i.e. as soon as possible)
	 * @param period the interval between successive executions of the task (in milliseconds)
	 * @return a {@link ScheduledFuture} representing pending completion of the task
	 * @throws org.springframework.core.task.TaskRejectedException if  the given task was not accepted
	 * for internal reasons (e.g. a pool overload handling policy or a pool shutdown in progress)
	 */
	// 调度给定的 {@link Runnable}，在指定的执行时间调用它，随后在给定的时间段内调用它。
	// <p>一旦调度程序关闭或返回的 {@link ScheduledFuture} 被取消，执行将结束
	// @param task 每当触发器触发时执行的 Runnable
	// @param startTime 任务所需的第一次执行时间（如果这是过去，则任务将立即执行，即尽快执行）
	// @param period 任务连续执行之间的间隔（以毫秒为单位）
	// @return 一个 {@link ScheduledFuture} 代表待完成的任务
	// @throws org.springframework.core.task.TaskRejectedException 如果给定的任务由于
	// 内部原因（例如池过载处理策略或正在进行的池关闭）而未被接受
	ScheduledFuture<?> scheduleAtFixedRate(Runnable task, Date startTime, long period);

	/**
	 * Schedule the given {@link Runnable}, starting as soon as possible and
	 * invoking it with the given period.
	 * <p>Execution will end once the scheduler shuts down or the returned
	 * {@link ScheduledFuture} gets cancelled.
	 * @param task the Runnable to execute whenever the trigger fires
	 * @param period the interval between successive executions of the task
	 * @return a {@link ScheduledFuture} representing pending completion of the task
	 * @throws org.springframework.core.task.TaskRejectedException if the given task was not accepted
	 * for internal reasons (e.g. a pool overload handling policy or a pool shutdown in progress)
	 * @since 5.0
	 * @see #scheduleAtFixedRate(Runnable, long)
	 */
	// 安排给定的 {@link Runnable}，尽快开始并在给定的时间段内调用它。
	// <p>一旦调度程序关闭或返回的 {@link ScheduledFuture} 被取消，执行将结束。
	// @param task 每当触发器触发时执行的 Runnable
	// @param period 任务连续执行之间的间隔
	// @return 一个 {@link ScheduledFuture} 代表待完成的任务
	// @throws org.springframework.core.task.TaskRejectedException 如果给定的任务由于
	// 内部原因（例如池过载处理策略或正在进行的池关闭）而未被接受
	default ScheduledFuture<?> scheduleAtFixedRate(Runnable task, Duration period) {
		return scheduleAtFixedRate(task, period.toMillis());
	}

	/**
	 * Schedule the given {@link Runnable}, starting as soon as possible and
	 * invoking it with the given period.
	 * <p>Execution will end once the scheduler shuts down or the returned
	 * {@link ScheduledFuture} gets cancelled.
	 * @param task the Runnable to execute whenever the trigger fires
	 * @param period the interval between successive executions of the task (in milliseconds)
	 * @return a {@link ScheduledFuture} representing pending completion of the task
	 * @throws org.springframework.core.task.TaskRejectedException if the given task was not accepted
	 * for internal reasons (e.g. a pool overload handling policy or a pool shutdown in progress)
	 */
	// 安排给定的 {@link Runnable}，尽快开始并在给定的时间段内调用它。
	// <p>一旦调度程序关闭或返回的 {@link ScheduledFuture} 被取消，执行将结束。
	// @param task 每当触发器触发时执行的 Runnable
	// @param period 任务连续执行之间的间隔（以毫秒为单位）
	// @return 一个 {@link ScheduledFuture} 代表待完成的任务
	// @throws org.springframework.core.task.TaskRejectedException 如果给定的任务
	// 由于内部原因（例如池过载处理策略或正在进行的池关闭）而未被接受
	ScheduledFuture<?> scheduleAtFixedRate(Runnable task, long period);

	/**
	 * Schedule the given {@link Runnable}, invoking it at the specified execution time
	 * and subsequently with the given delay between the completion of one execution
	 * and the start of the next.
	 * <p>Execution will end once the scheduler shuts down or the returned
	 * {@link ScheduledFuture} gets cancelled.
	 * @param task the Runnable to execute whenever the trigger fires
	 * @param startTime the desired first execution time for the task
	 * (if this is in the past, the task will be executed immediately, i.e. as soon as possible)
	 * @param delay the delay between the completion of one execution and the start of the next
	 * @return a {@link ScheduledFuture} representing pending completion of the task
	 * @throws org.springframework.core.task.TaskRejectedException if the given task was not accepted
	 * for internal reasons (e.g. a pool overload handling policy or a pool shutdown in progress)
	 * @since 5.0
	 * @see #scheduleWithFixedDelay(Runnable, Date, long)
	 */
	// 调度给定的 {@link Runnable}，在指定的执行时间调用它，随后在一次执行完成和下一次开始之间具有给定的延迟。
	// <p>一旦调度程序关闭或返回的 {@link ScheduledFuture} 被取消，执行将结束。
	// @param task 每当触发器触发时执行的 Runnable
	// @param startTime 任务所需的第一次执行时间（如果这是过去，则任务将立即执行，即尽快执行）
	// @param delay 一次执行完成和下一次执行开始之间的延迟
	// @return 一个 {@link ScheduledFuture} 代表待完成的任务
	// @throws org.springframework.core.task.TaskRejectedException 如果给定的任务由于
	// 内部原因（例如池过载处理策略或正在进行的池关闭）而未被接受
	default ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, Instant startTime, Duration delay) {
		return scheduleWithFixedDelay(task, Date.from(startTime), delay.toMillis());
	}

	/**
	 * Schedule the given {@link Runnable}, invoking it at the specified execution time
	 * and subsequently with the given delay between the completion of one execution
	 * and the start of the next.
	 * <p>Execution will end once the scheduler shuts down or the returned
	 * {@link ScheduledFuture} gets cancelled.
	 * @param task the Runnable to execute whenever the trigger fires
	 * @param startTime the desired first execution time for the task
	 * (if this is in the past, the task will be executed immediately, i.e. as soon as possible)
	 * @param delay the delay between the completion of one execution and the start of the next
	 * (in milliseconds)
	 * @return a {@link ScheduledFuture} representing pending completion of the task
	 * @throws org.springframework.core.task.TaskRejectedException if the given task was not accepted
	 * for internal reasons (e.g. a pool overload handling policy or a pool shutdown in progress)
	 */
	// 调度给定的 {@link Runnable}，在指定的执行时间调用它，随后在一次执行完成和下一次开始之间具有给定的延迟。
	// <p>一旦调度程序关闭或返回的 {@link ScheduledFuture} 被取消，执行将结束。
	// @param task 每当触发器触发时执行的 Runnable
	// @param startTime 任务所需的第一次执行时间（如果这是过去，则任务将立即执行，即尽快执行）
	// @param delay 一次执行完成和下一次执行开始之间的延迟（以毫秒为单位）
	// @return 一个 {@link ScheduledFuture} 代表待完成的任务
	// @throws org.springframework.core.task.TaskRejectedException 如果给定的任务由于
	// 内部原因（例如池过载处理策略或正在进行的池关闭）而未被接受
	ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, Date startTime, long delay);

	/**
	 * Schedule the given {@link Runnable}, starting as soon as possible and invoking it with
	 * the given delay between the completion of one execution and the start of the next.
	 * <p>Execution will end once the scheduler shuts down or the returned
	 * {@link ScheduledFuture} gets cancelled.
	 * @param task the Runnable to execute whenever the trigger fires
	 * @param delay the delay between the completion of one execution and the start of the next
	 * @return a {@link ScheduledFuture} representing pending completion of the task
	 * @throws org.springframework.core.task.TaskRejectedException if the given task was not accepted
	 * for internal reasons (e.g. a pool overload handling policy or a pool shutdown in progress)
	 * @since 5.0
	 * @see #scheduleWithFixedDelay(Runnable, long)
	 */
	// 安排给定的 {@link Runnable}，尽快开始并在一次执行完成和下一次开始之间的给定延迟内调用它。
	// <p>一旦调度程序关闭或返回的 {@link ScheduledFuture} 被取消，执行将结束。
	// @param task 每当触发器触发时执行的 Runnable
	// @param delay 一次执行完成和下一次执行开始之间的延迟
	// @return 一个 {@link ScheduledFuture} 代表待完成的任务
	// @throws org.springframework.core.task.TaskRejectedException 如果给定的任务由于
	// 内部原因（例如池过载处理策略或正在进行的池关闭）而未被接受
	default ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, Duration delay) {
		return scheduleWithFixedDelay(task, delay.toMillis());
	}

	/**
	 * Schedule the given {@link Runnable}, starting as soon as possible and invoking it with
	 * the given delay between the completion of one execution and the start of the next.
	 * <p>Execution will end once the scheduler shuts down or the returned
	 * {@link ScheduledFuture} gets cancelled.
	 * @param task the Runnable to execute whenever the trigger fires
	 * @param delay the delay between the completion of one execution and the start of the next
	 * (in milliseconds)
	 * @return a {@link ScheduledFuture} representing pending completion of the task
	 * @throws org.springframework.core.task.TaskRejectedException if the given task was not accepted
	 * for internal reasons (e.g. a pool overload handling policy or a pool shutdown in progress)
	 */
	// 安排给定的 {@link Runnable}，尽快开始并在一次执行完成和下一次开始之间的给定延迟内调用它。
	// <p>一旦调度程序关闭或返回的 {@link ScheduledFuture} 被取消，执行将结束
	// @param task 每当触发器触发时执行的 Runnable
	// @param delay 一次执行完成和下一次执行开始之间的延迟（以毫秒为单位）
	// @return 一个 {@link ScheduledFuture} 代表待完成的任务
	// @throws org.springframework.core.task.TaskRejectedException 如果给定的任务由于
	// 内部原因（例如池过载处理策略或正在进行的池关闭）而未被接受
	ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, long delay);

}
