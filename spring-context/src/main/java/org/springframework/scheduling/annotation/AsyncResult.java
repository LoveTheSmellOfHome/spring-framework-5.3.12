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

package org.springframework.scheduling.annotation;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.springframework.lang.Nullable;
import org.springframework.util.concurrent.FailureCallback;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;
import org.springframework.util.concurrent.SuccessCallback;

/**
 * A pass-through {@code Future} handle that can be used for method signatures
 * which are declared with a {@code Future} return type for asynchronous execution.
 *
 * <p>As of Spring 4.1, this class implements {@link ListenableFuture}, not just
 * plain {@link java.util.concurrent.Future}, along with the corresponding support
 * in {@code @Async} processing.
 *
 * <p>As of Spring 4.2, this class also supports passing execution exceptions back
 * to the caller.
 *
 * @author Juergen Hoeller
 * @author Rossen Stoyanchev
 * @since 3.0
 * @param <V> the value type
 * @see Async
 * @see #forValue(Object)
 * @see #forExecutionException(Throwable)
 */
// 一个传递Future句柄，可用于方法签名，这些方法签名声明为Future返回类型以用于异步执行。
//从 Spring 4.1 开始，此类实现 ListenableFuture ，而不仅仅是普通的 Future ，以及 @Async 处理中的相应支持。
//从 Spring 4.2 开始，此类还支持将执行异常传递回调用者。
// 类型形参： < V > - 值类型
public class AsyncResult<V> implements ListenableFuture<V> {

	// 返回值
	@Nullable
	private final V value;

	// 异常
	@Nullable
	private final Throwable executionException;


	/**
	 * Create a new AsyncResult holder.
	 * @param value the value to pass through
	 */
	// 创建一个新的 AsyncResult 持有者。
	// 参形：value - 要传递的值
	public AsyncResult(@Nullable V value) {
		this(value, null);
	}

	/**
	 * Create a new AsyncResult holder.
	 * @param value the value to pass through
	 */
	// 创建一个新的 AsyncResult 持有者。
	// 参形：value - 要传递的值
	private AsyncResult(@Nullable V value, @Nullable Throwable ex) {
		this.value = value;
		this.executionException = ex;
	}


	// 是否取消正在运行的程序
	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		return false;
	}

	// 是否已经取消了
	@Override
	public boolean isCancelled() {
		return false;
	}

	// 是否正在运行
	@Override
	public boolean isDone() {
		return true;
	}

	// 获取返回结果
	@Override
	@Nullable
	public V get() throws ExecutionException {
		if (this.executionException != null) {
			throw (this.executionException instanceof ExecutionException ?
					(ExecutionException) this.executionException :
					new ExecutionException(this.executionException));
		}
		return this.value;
	}

	// 获取指定时间内的执行结果
	@Override
	@Nullable
	public V get(long timeout, TimeUnit unit) throws ExecutionException {
		return get();
	}

	// 添加回调
	@Override
	public void addCallback(ListenableFutureCallback<? super V> callback) {
		addCallback(callback, callback);
	}

	@Override
	public void addCallback(SuccessCallback<? super V> successCallback, FailureCallback failureCallback) {
		try {
			if (this.executionException != null) {
				// 失败回调
				failureCallback.onFailure(exposedException(this.executionException));
			}
			else {
				// 成功回调
				successCallback.onSuccess(this.value);
			}
		}
		catch (Throwable ex) {
			// Ignore
		}
	}

	// 完成调用
	@Override
	public CompletableFuture<V> completable() {
		if (this.executionException != null) {
			CompletableFuture<V> completable = new CompletableFuture<>();
			completable.completeExceptionally(exposedException(this.executionException));
			return completable;
		}
		else {
			return CompletableFuture.completedFuture(this.value);
		}
	}


	/**
	 * Create a new async result which exposes the given value from {@link Future#get()}.
	 * @param value the value to expose
	 * @since 4.2
	 * @see Future#get()
	 */
	// 创建一个新的异步结果，它公开来自 Future.get() 的给定值。
	// 参形：
	//			value – 要公开的值
	public static <V> ListenableFuture<V> forValue(V value) {
		return new AsyncResult<>(value, null);
	}

	/**
	 * Create a new async result which exposes the given exception as an
	 * {@link ExecutionException} from {@link Future#get()}.
	 * @param ex the exception to expose (either an pre-built {@link ExecutionException}
	 * or a cause to be wrapped in an {@link ExecutionException})
	 * @since 4.2
	 * @see ExecutionException
	 */
	// 创建一个新的异步结果，它将给定的异常公开为来自 Future.get() 的 ExecutionException 。
	// 参形：
	//			ex – 要公开的异常（预构建的 ExecutionException 或包含在 ExecutionException 中的原因）
	public static <V> ListenableFuture<V> forExecutionException(Throwable ex) {
		return new AsyncResult<>(null, ex);
	}

	/**
	 * Determine the exposed exception: either the cause of a given
	 * {@link ExecutionException}, or the original exception as-is.
	 * @param original the original as given to {@link #forExecutionException}
	 * @return the exposed exception
	 */
	// 确定暴露的异常：给定的 ExecutionException 的原因，或原样的原始异常。
	// 参形：
	//			original – 给forExecutionException的原始文件
	// 返回值：
	//			暴露的异常
	private static Throwable exposedException(Throwable original) {
		if (original instanceof ExecutionException) {
			Throwable cause = original.getCause();
			if (cause != null) {
				return cause;
			}
		}
		return original;
	}

}
