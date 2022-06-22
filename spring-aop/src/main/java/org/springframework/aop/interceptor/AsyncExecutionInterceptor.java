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

package org.springframework.aop.interceptor;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.core.BridgeMethodResolver;
import org.springframework.core.Ordered;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;

/**
 * AOP Alliance {@code MethodInterceptor} that processes method invocations
 * asynchronously, using a given {@link org.springframework.core.task.AsyncTaskExecutor}.
 * Typically used with the {@link org.springframework.scheduling.annotation.Async} annotation.
 *
 * <p>In terms of target method signatures, any parameter types are supported.
 * However, the return type is constrained to either {@code void} or
 * {@code java.util.concurrent.Future}. In the latter case, the Future handle
 * returned from the proxy will be an actual asynchronous Future that can be used
 * to track the result of the asynchronous method execution. However, since the
 * target method needs to implement the same signature, it will have to return
 * a temporary Future handle that just passes the return value through
 * (like Spring's {@link org.springframework.scheduling.annotation.AsyncResult}
 * or EJB 3.1's {@code javax.ejb.AsyncResult}).
 *
 * <p>When the return type is {@code java.util.concurrent.Future}, any exception thrown
 * during the execution can be accessed and managed by the caller. With {@code void}
 * return type however, such exceptions cannot be transmitted back. In that case an
 * {@link AsyncUncaughtExceptionHandler} can be registered to process such exceptions.
 *
 * <p>As of Spring 3.1.2 the {@code AnnotationAsyncExecutionInterceptor} subclass is
 * preferred for use due to its support for executor qualification in conjunction with
 * Spring's {@code @Async} annotation.
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author Stephane Nicoll
 * @since 3.0
 * @see org.springframework.scheduling.annotation.Async
 * @see org.springframework.scheduling.annotation.AsyncAnnotationAdvisor
 * @see org.springframework.scheduling.annotation.AnnotationAsyncExecutionInterceptor
 */
// AOP 联盟 MethodInterceptor 使用给定的 AsyncTaskExecutor 异步处理方法调用。通常与 
// org.springframework.scheduling.annotation.Async 注解一起使用。
//
// 在目标方法签名方面，支持任何参数类型。但是，返回类型被限制为 void 或 java.util.concurrent.Future 。
// 在后一种情况下，从代理返回的 Future 句柄将是一个实际的异步 Future，可用于跟踪异步方法执行的结果。但是，
// 由于目标方法需要实现相同的签名，它必须返回一个临时的 Future 句柄，该句柄只是通过返回值（如 Spring 的 
// org.springframework.scheduling.annotation.AsyncResult 或 EJB 3.1 的 javax.ejb.AsyncResult ）。
//
// 当返回类型为java.util.concurrent.Future时，调用者可以访问和管理执行过程中抛出的任何异常。然而，对于void返回类型，此类异常不能被传回。在这种情况下，可以注册AsyncUncaughtExceptionHandler来处理此类异常。
//
// 从 Spring 3.1.2 开始，首选使用AnnotationAsyncExecutionInterceptor子类，因为它支持执行器限定以及 Spring 的@Async注解。
public class AsyncExecutionInterceptor extends AsyncExecutionAspectSupport implements MethodInterceptor, Ordered {

	/**
	 * Create a new instance with a default {@link AsyncUncaughtExceptionHandler}.
	 * @param defaultExecutor the {@link Executor} (typically a Spring {@link AsyncTaskExecutor}
	 * or {@link java.util.concurrent.ExecutorService}) to delegate to;
	 * as of 4.2.6, a local executor for this interceptor will be built otherwise
	 */
	// 使用默认的 AsyncUncaughtExceptionHandler 创建一个新实例。
	// 参形：
	//			defaultExecutor – 委托给的 Executor （通常是 Spring AsyncTaskExecutor 或 
	//			java.util.concurrent.ExecutorService ）；从 4.2.6 开始，将为此拦截器构建一个本地执行器，否则
	public AsyncExecutionInterceptor(@Nullable Executor defaultExecutor) {
		super(defaultExecutor);
	}

	/**
	 * Create a new {@code AsyncExecutionInterceptor}.
	 * @param defaultExecutor the {@link Executor} (typically a Spring {@link AsyncTaskExecutor}
	 * or {@link java.util.concurrent.ExecutorService}) to delegate to;
	 * as of 4.2.6, a local executor for this interceptor will be built otherwise
	 * @param exceptionHandler the {@link AsyncUncaughtExceptionHandler} to use
	 */
	// 创建一个新的AsyncExecutionInterceptor 。
	// 参形：
	//			defaultExecutor – 委托给的Executor （通常是 Spring AsyncTaskExecutor 或 
	//			java.util.concurrent.ExecutorService ）；从 4.2.6 开始，将为此拦截器构建一个本地执行器，否则
	//			
	//			exceptionHandler – 要使用的AsyncUncaughtExceptionHandler
	public AsyncExecutionInterceptor(@Nullable Executor defaultExecutor, AsyncUncaughtExceptionHandler exceptionHandler) {
		super(defaultExecutor, exceptionHandler);
	}


	/**
	 * Intercept the given method invocation, submit the actual calling of the method to
	 * the correct task executor and return immediately to the caller.
	 * @param invocation the method to intercept and make asynchronous
	 * @return {@link Future} if the original method returns {@code Future}; {@code null}
	 * otherwise.
	 */
	// 拦截给定的方法调用，将方法的实际调用提交给正确的任务执行者，并立即返回给调用者。
	// 参形：
	//			invocation - 拦截和异步的方法
	// 返回值：
	//			如果原始方法返回Future则为Future ；否则null 。
	@Override
	@Nullable
	public Object invoke(final MethodInvocation invocation) throws Throwable {
		// 获取执行的目标类型
		Class<?> targetClass = (invocation.getThis() != null ? AopUtils.getTargetClass(invocation.getThis()) : null);
		// 获取目标方法
		Method specificMethod = ClassUtils.getMostSpecificMethod(invocation.getMethod(), targetClass);
		// 获取桥接方法
		final Method userDeclaredMethod = BridgeMethodResolver.findBridgedMethod(specificMethod);
		
		// 异步执行器,获取线程池
		AsyncTaskExecutor executor = determineAsyncExecutor(userDeclaredMethod);
		if (executor == null) {
			throw new IllegalStateException(
					"No executor specified and no default executor set on AsyncExecutionInterceptor either");
		}

		// 执行异步：不需要及时返回，返回一个 task 执行任务
		Callable<Object> task = () -> {
			try {
				// 方法执行
				Object result = invocation.proceed();
				// 如果是 Future 类型，则返回 get()
				if (result instanceof Future) {
					return ((Future<?>) result).get();
				}
			}
			catch (ExecutionException ex) {
				// 处理异常
				handleError(ex.getCause(), userDeclaredMethod, invocation.getArguments());
			}
			catch (Throwable ex) {
				// 处理异常
				handleError(ex, userDeclaredMethod, invocation.getArguments());
			}
			return null;
		};

		// 提交返回结果
		return doSubmit(task, executor, invocation.getMethod().getReturnType());
	}

	/**
	 * This implementation is a no-op for compatibility in Spring 3.1.2.
	 * Subclasses may override to provide support for extracting qualifier information,
	 * e.g. via an annotation on the given method.
	 * @return always {@code null}
	 * @since 3.1.2
	 * @see #determineAsyncExecutor(Method)
	 */
	// 此实现在 Spring 3.1.2 中是为了兼容性而无操作。子类可以覆盖以提供对提取限定符信息的支持，例如通过给定方法上的注解。
	// 返回值：
	//			始终null
	@Override
	@Nullable
	protected String getExecutorQualifier(Method method) {
		return null;
	}

	/**
	 * This implementation searches for a unique {@link org.springframework.core.task.TaskExecutor}
	 * bean in the context, or for an {@link Executor} bean named "taskExecutor" otherwise.
	 * If neither of the two is resolvable (e.g. if no {@code BeanFactory} was configured at all),
	 * this implementation falls back to a newly created {@link SimpleAsyncTaskExecutor} instance
	 * for local use if no default could be found.
	 * @see #DEFAULT_TASK_EXECUTOR_BEAN_NAME
	 */
	// 此实现在上下文中搜索唯一的 org.springframework.core.task.TaskExecutor bean，否则搜索
	// 名为 “taskExecutor” 的 Executor bean。如果两者都不可解析（例如，如果根本没有配置BeanFactory ），
	// 如果找不到默认值，则此实现回退到新创建的 SimpleAsyncTaskExecutor 实例以供本地使用。
	@Override
	@Nullable
	protected Executor getDefaultExecutor(@Nullable BeanFactory beanFactory) {
		Executor defaultExecutor = super.getDefaultExecutor(beanFactory);
		return (defaultExecutor != null ? defaultExecutor : new SimpleAsyncTaskExecutor());
	}

	// 获取优先级
	@Override
	public int getOrder() {
		return Ordered.HIGHEST_PRECEDENCE;
	}

}
