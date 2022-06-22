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

import org.springframework.util.concurrent.ListenableFuture;

/**
 * Extension of the {@link AsyncTaskExecutor} interface, adding the capability to submit
 * tasks for {@link ListenableFuture ListenableFutures}.
 *
 * @author Arjen Poutsma
 * @since 4.0
 * @see ListenableFuture
 */
// AsyncTaskExecutor 接口的扩展，添加了为 ListenableFutures 提交任务的能力。
public interface AsyncListenableTaskExecutor extends AsyncTaskExecutor {

	/**
	 * Submit a {@code Runnable} task for execution, receiving a {@code ListenableFuture}
	 * representing that task. The Future will return a {@code null} result upon completion.
	 * @param task the {@code Runnable} to execute (never {@code null})
	 * @return a {@code ListenableFuture} representing pending completion of the task
	 * @throws TaskRejectedException if the given task was not accepted
	 */
	// 提交Runnable任务以执行，接收表示该任务的ListenableFuture 。 Future 将在完成后返回null结果。
	// 参形：
	//				task – 要执行的Runnable （从不为null ）
	// 返回值：
	//				表示待完成任务的ListenableFuture
	// 抛出：
	//				TaskRejectedException – 如果给定的任务没有被接受
	ListenableFuture<?> submitListenable(Runnable task);

	/**
	 * Submit a {@code Callable} task for execution, receiving a {@code ListenableFuture}
	 * representing that task. The Future will return the Callable's result upon
	 * completion.
	 * @param task the {@code Callable} to execute (never {@code null})
	 * @return a {@code ListenableFuture} representing pending completion of the task
	 * @throws TaskRejectedException if the given task was not accepted
	 */
	// 提交 Callable 任务以执行，接收代表该任务的 ListenableFuture 。 Future 将在完成后返回 Callable 的结果。
	// 参形：
	//				task - Callable的执行（从不为null ）
	// 返回值：
	//				表示待完成任务的ListenableFuture
	// 抛出：
	//				TaskRejectedException – 如果给定的任务没有被接受
	<T> ListenableFuture<T> submitListenable(Callable<T> task);

}
