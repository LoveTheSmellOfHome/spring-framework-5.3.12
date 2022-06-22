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

package org.springframework.scheduling.annotation;

import java.lang.reflect.Method;
import java.util.concurrent.Executor;

import org.springframework.aop.interceptor.AsyncExecutionInterceptor;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.lang.Nullable;

/**
 * Specialization of {@link AsyncExecutionInterceptor} that delegates method execution to
 * an {@code Executor} based on the {@link Async} annotation. Specifically designed to
 * support use of {@link Async#value()} executor qualification mechanism introduced in
 * Spring 3.1.2. Supports detecting qualifier metadata via {@code @Async} at the method or
 * declaring class level. See {@link #getExecutorQualifier(Method)} for details.
 *
 * @author Chris Beams
 * @author Stephane Nicoll
 * @since 3.1.2
 * @see org.springframework.scheduling.annotation.Async
 * @see org.springframework.scheduling.annotation.AsyncAnnotationAdvisor
 */
// AsyncExecutionInterceptor 的特化，将方法执行委托给基于 @Async 注解的 Executor 。专门设计用于
// 支持使用 Spring 3.1.2 中引入的 Async.value() 执行器限定机制。支持在方法或声明类级别通过 @Async
// 检测限定符元数据。有关详细信息，请参阅 getExecutorQualifier(Method) 。
public class AnnotationAsyncExecutionInterceptor extends AsyncExecutionInterceptor {

	/**
	 * Create a new {@code AnnotationAsyncExecutionInterceptor} with the given executor
	 * and a simple {@link AsyncUncaughtExceptionHandler}.
	 * @param defaultExecutor the executor to be used by default if no more specific
	 * executor has been qualified at the method level using {@link Async#value()};
	 * as of 4.2.6, a local executor for this interceptor will be built otherwise
	 */
	// 使用给定的执行器和一个简单的 AsyncUncaughtExceptionHandler 创建一个新的 AnnotationAsyncExecutionInterceptor 。
	// 参形：
	// defaultExecutor – 如果在方法级别没有使用 Async.value() 限定更多特定执行程序，
	// 则默认使用的执行程序；从 4.2.6 开始，将为此拦截器构建一个本地执行器，否则
	public AnnotationAsyncExecutionInterceptor(@Nullable Executor defaultExecutor) {
		super(defaultExecutor);
	}

	/**
	 * Create a new {@code AnnotationAsyncExecutionInterceptor} with the given executor.
	 * @param defaultExecutor the executor to be used by default if no more specific
	 * executor has been qualified at the method level using {@link Async#value()};
	 * as of 4.2.6, a local executor for this interceptor will be built otherwise
	 * @param exceptionHandler the {@link AsyncUncaughtExceptionHandler} to use to
	 * handle exceptions thrown by asynchronous method executions with {@code void}
	 * return type
	 */
	// 使用给定的执行器创建一个新的AnnotationAsyncExecutionInterceptor 。
	// 参形：
	//			defaultExecutor – 如果在方法级别没有使用A sync.value() 限定更多特定执行程序，
	//			则默认使用的执行程序；从 4.2.6 开始，将为此拦截器构建一个本地执行器，否则
	//
	//			exceptionHandler – AsyncUncaughtExceptionHandler 用于处理由具有 void 返回类型的异步方法执行引发的异常
	public AnnotationAsyncExecutionInterceptor(@Nullable Executor defaultExecutor, AsyncUncaughtExceptionHandler exceptionHandler) {
		super(defaultExecutor, exceptionHandler);
	}


	/**
	 * Return the qualifier or bean name of the executor to be used when executing the
	 * given method, specified via {@link Async#value} at the method or declaring
	 * class level. If {@code @Async} is specified at both the method and class level, the
	 * method's {@code #value} takes precedence (even if empty string, indicating that
	 * the default executor should be used preferentially).
	 * @param method the method to inspect for executor qualifier metadata
	 * @return the qualifier if specified, otherwise empty string indicating that the
	 * {@linkplain #setExecutor(Executor) default executor} should be used
	 * @see #determineAsyncExecutor(Method)
	 */
	// 返回执行给定方法时要使用的执行器的限定符或 bean 名称，通过方法或声明类级别的 Async.value 指定。
	// 如果在方法级别和类级别都指定了 @Async ，则方法的 #value 优先（即使是空字符串，表示应优先使用默认执行程序）。
	// 参形：
	//			method - 检查执行者限定符元数据的方法
	// 返回值：
	//			限定符（如果指定），否则为空字符串，指示应使用默认执行程序
	@Override
	@Nullable
	protected String getExecutorQualifier(Method method) {
		// Maintainer's note: changes made here should also be made in
		// AnnotationAsyncExecutionAspect#getExecutorQualifier
		// 维护者注：此处所做的更改也应在 AnnotationAsyncExecutionAspectgetExecutorQualifier 中进行
		Async async = AnnotatedElementUtils.findMergedAnnotation(method, Async.class);
		if (async == null) {
			async = AnnotatedElementUtils.findMergedAnnotation(method.getDeclaringClass(), Async.class);
		}
		return (async != null ? async.value() : null);
	}

}
