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

package org.springframework.cache.interceptor;

import org.springframework.lang.Nullable;

/**
 * Abstract the invocation of a cache operation.
 *
 * <p>Does not provide a way to transmit checked exceptions but
 * provide a special exception that should be used to wrap any
 * exception that was thrown by the underlying invocation.
 * Callers are expected to handle this issue type specifically.
 *
 * @author Stephane Nicoll
 * @since 4.1
 */
// 抽象缓存操作的调用。
// 不提供传输检查异常的方法，但提供一个特殊异常，用于包装底层调用引发的任何异常。调用者应专门处理此问题类型。
@FunctionalInterface
public interface CacheOperationInvoker {

	/**
	 * Invoke the cache operation defined by this instance. Wraps any exception
	 * that is thrown during the invocation in a {@link ThrowableWrapper}.
	 * @return the result of the operation
	 * @throws ThrowableWrapper if an error occurred while invoking the operation
	 */
	// 调用此实例定义的缓存操作。将调用期间引发的任何异常包装在CacheOperationInvoker.ThrowableWrapper中。
	// 返回值：
	//				操作的结果
	// 抛出：
	//				CacheOperationInvoker.ThrowableWrapper – 如果在调用操作时发生错误
	@Nullable
	Object invoke() throws ThrowableWrapper;


	/**
	 * Wrap any exception thrown while invoking {@link #invoke()}.
	 */
	// 包装调用 invoke() 时抛出的任何异常。
	@SuppressWarnings("serial")
	class ThrowableWrapper extends RuntimeException {

		private final Throwable original;

		public ThrowableWrapper(Throwable original) {
			super(original.getMessage(), original);
			this.original = original;
		}

		public Throwable getOriginal() {
			return this.original;
		}
	}

}
