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

package org.springframework.core.task;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

/**
 * Extended interface for asynchronous {@link TaskExecutor} implementations,
 * offering an overloaded {@link #execute(Runnable, long)} variant with a start
 * timeout parameter as well support for {@link java.util.concurrent.Callable}.
 *
 * <p>Note: The {@link java.util.concurrent.Executors} class includes a set of
 * methods that can convert some other common closure-like objects, for example,
 * {@link java.security.PrivilegedAction} to {@link Callable} before executing them.
 *
 * <p>Implementing this interface also indicates that the {@link #execute(Runnable)}
 * method will not execute its Runnable in the caller's thread but rather
 * asynchronously in some other thread.
 *
 * @author Juergen Hoeller
 * @since 2.0.3
 * @see SimpleAsyncTaskExecutor
 * @see org.springframework.scheduling.SchedulingTaskExecutor
 * @see java.util.concurrent.Callable
 * @see java.util.concurrent.Executors
 */
// {@link TaskExecutor} 实现的异步扩展接口，提供带有启动超时参数的重载 {@link #execute(Runnable, long)}
// 变体以及对 {@link java.util.concurrent.Callable} 的支持。
//
// <p>注意：{@link java.util.concurrent.Executors} 类包含一组方法，可以将一些其他常见的类似闭包的对象，
// 例如，{@link java.security.PrivilegedAction} 转换为 {@link Callable} 在执行它们之前。
//
// <p>实现这个接口也表明 {@link #execute(Runnable)} 方法不会在调用者的线程中执行它的 Runnable，
// 而是在其他一些线程中异步执行。
public interface AsyncTaskExecutor extends TaskExecutor {

	/** Constant that indicates immediate execution. */
	// 表示立即执行的常量
	long TIMEOUT_IMMEDIATE = 0;

	/** Constant that indicates no time limit. */
	// 表示没有时间限制的常数
	long TIMEOUT_INDEFINITE = Long.MAX_VALUE;


	/**
	 * Execute the given {@code task}.
	 * @param task the {@code Runnable} to execute (never {@code null})
	 * @param startTimeout the time duration (milliseconds) within which the task is
	 * supposed to start. This is intended as a hint to the executor, allowing for
	 * preferred handling of immediate tasks. Typical values are {@link #TIMEOUT_IMMEDIATE}
	 * or {@link #TIMEOUT_INDEFINITE} (the default as used by {@link #execute(Runnable)}).
	 * @throws TaskTimeoutException in case of the task being rejected because
	 * of the timeout (i.e. it cannot be started in time)
	 * @throws TaskRejectedException if the given task was not accepted
	 */
	// 执行给定的 {@code task}
	// @param task {@code Runnable} 执行任务（从不{@code null}）
	// @param startTimeout 任务应该开始的持续时间（毫秒）。
	// 这旨在作为对执行者的提示，允许优先处理即时任务。典型值为 {@link TIMEOUT_IMMEDIATE} 或
	// {@link TIMEOUT_INDEFINITE}（{@link execute(Runnable)} 使用的默认值）
	// @throws TaskTimeoutException，以防任务因超时而被拒绝（即无法及时启动）
	// @throws TaskRejectedException 如果给定的任务未被接受
	void execute(Runnable task, long startTimeout);

	/**
	 * Submit a Runnable task for execution, receiving a Future representing that task.
	 * The Future will return a {@code null} result upon completion.
	 * @param task the {@code Runnable} to execute (never {@code null})
	 * @return a Future representing pending completion of the task
	 * @throws TaskRejectedException if the given task was not accepted
	 * @since 3.0
	 */
	// 提交 Runnable 任务以执行，接收代表该任务的 Future。 Future 将在完成后返回 {@code null} 结果。
	// @param task {@code Runnable} 执行任务（从不{@code null}）
	// @return 代表待完成任务的 Future
	// @throws TaskRejectedException 如果给定的任务未被接受
	Future<?> submit(Runnable task);

	/**
	 * Submit a Callable task for execution, receiving a Future representing that task.
	 * The Future will return the Callable's result upon completion.
	 * @param task the {@code Callable} to execute (never {@code null})
	 * @return a Future representing pending completion of the task
	 * @throws TaskRejectedException if the given task was not accepted
	 * @since 3.0
	 */
	// 提交 Callable 执行任务，接收代表该任务的 Future。 Future 将在完成后返回 Callable 的结果
	// @param task {@code Callable} 执行任务（从不{@code null}）
	// @throws TaskRejectedException 如果给定的任务未被接受
	<T> Future<T> submit(Callable<T> task);

}
