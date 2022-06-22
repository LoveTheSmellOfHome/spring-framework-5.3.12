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
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.function.Supplier;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.beans.factory.annotation.BeanFactoryAnnotationUtils;
import org.springframework.core.task.AsyncListenableTaskExecutor;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.support.TaskExecutorAdapter;
import org.springframework.lang.Nullable;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.function.SingletonSupplier;

/**
 * Base class for asynchronous method execution aspects, such as
 * {@code org.springframework.scheduling.annotation.AnnotationAsyncExecutionInterceptor}
 * or {@code org.springframework.scheduling.aspectj.AnnotationAsyncExecutionAspect}.
 *
 * <p>Provides support for <i>executor qualification</i> on a method-by-method basis.
 * {@code AsyncExecutionAspectSupport} objects must be constructed with a default {@code
 * Executor}, but each individual method may further qualify a specific {@code Executor}
 * bean to be used when executing it, e.g. through an annotation attribute.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @since 3.1.2
 */
// 异步方法执行切面的基类，例如 org.springframework.scheduling.annotation.AnnotationAsyncExecutionInterceptor
// 或 org.springframework.scheduling.aspectj.AnnotationAsyncExecutionAspect。为逐个方法的执行者资格提供支持。
// AsyncExecutionAspectSupport 对象必须使用默认的 Executor 构造，但每个单独的方法都可以进一步限定在执行时要使用的
// 特定 Executor bean，例如通过注释属性。
public abstract class AsyncExecutionAspectSupport implements BeanFactoryAware {

	/**
	 * The default name of the {@link TaskExecutor} bean to pick up: "taskExecutor".
	 * <p>Note that the initial lookup happens by type; this is just the fallback
	 * in case of multiple executor beans found in the context.
	 * @since 4.2.6
	 */
	// 要获取的TaskExecutor bean 的默认名称：“taskExecutor”。
	// 请注意，初始查找是按类型进行的；这只是在上下文中发现多个执行器 bean 的情况下的后备。
	public static final String DEFAULT_TASK_EXECUTOR_BEAN_NAME = "taskExecutor";


	protected final Log logger = LogFactory.getLog(getClass());

	private final Map<Method, AsyncTaskExecutor> executors = new ConcurrentHashMap<>(16);

	// 单例供应商
	private SingletonSupplier<Executor> defaultExecutor;

	// 异步异常处理器：
	private SingletonSupplier<AsyncUncaughtExceptionHandler> exceptionHandler;

	// 关联 Ioc 同期
	@Nullable
	private BeanFactory beanFactory;


	/**
	 * Create a new instance with a default {@link AsyncUncaughtExceptionHandler}.
	 * @param defaultExecutor the {@code Executor} (typically a Spring {@code AsyncTaskExecutor}
	 * or {@link java.util.concurrent.ExecutorService}) to delegate to, unless a more specific
	 * executor has been requested via a qualifier on the async method, in which case the
	 * executor will be looked up at invocation time against the enclosing bean factory
	 */
	// 使用默认的AsyncUncaughtExceptionHandler创建一个新实例。
	// 参形：
	// 			defaultExecutor – 要委托给的Executor （通常是 Spring AsyncTaskExecutor
	// 			或 java.util.concurrent.ExecutorService ），除非通过异步方法上的限定符请求了更具体的执行器，
	// 			在这种情况下，将在调用时查找执行器而不是 beanFactory
	public AsyncExecutionAspectSupport(@Nullable Executor defaultExecutor) {
		this.defaultExecutor = new SingletonSupplier<>(defaultExecutor, () -> getDefaultExecutor(this.beanFactory));
		this.exceptionHandler = SingletonSupplier.of(SimpleAsyncUncaughtExceptionHandler::new);
	}

	/**
	 * Create a new {@link AsyncExecutionAspectSupport} with the given exception handler.
	 * @param defaultExecutor the {@code Executor} (typically a Spring {@code AsyncTaskExecutor}
	 * or {@link java.util.concurrent.ExecutorService}) to delegate to, unless a more specific
	 * executor has been requested via a qualifier on the async method, in which case the
	 * executor will be looked up at invocation time against the enclosing bean factory
	 * @param exceptionHandler the {@link AsyncUncaughtExceptionHandler} to use
	 */
	// 使用给定的异常处理程序创建一个新的AsyncExecutionAspectSupport 。
	// 参形：
	//			defaultExecutor – 要委托给的Executor （通常是 Spring AsyncTaskExecutor 或
	//			java.util.concurrent.ExecutorService ），除非通过异步方法上的限定符请求了更具体的执行器，
	//			在这种情况下，将在调用时查找执行器而不是 beanFactory
	//			exceptionHandler – 要使用的 AsyncUncaughtExceptionHandler
	public AsyncExecutionAspectSupport(@Nullable Executor defaultExecutor, AsyncUncaughtExceptionHandler exceptionHandler) {
		this.defaultExecutor = new SingletonSupplier<>(defaultExecutor, () -> getDefaultExecutor(this.beanFactory));
		this.exceptionHandler = SingletonSupplier.of(exceptionHandler);
	}


	/**
	 * Configure this aspect with the given executor and exception handler suppliers,
	 * applying the corresponding default if a supplier is not resolvable.
	 * @since 5.1
	 */
	// 使用给定的执行程序和异常处理程序供应商配置此切面，如果供应商不可解析，则应用相应的默认值。
	public void configure(@Nullable Supplier<Executor> defaultExecutor,
			@Nullable Supplier<AsyncUncaughtExceptionHandler> exceptionHandler) {

		this.defaultExecutor = new SingletonSupplier<>(defaultExecutor, () -> getDefaultExecutor(this.beanFactory));
		this.exceptionHandler = new SingletonSupplier<>(exceptionHandler, SimpleAsyncUncaughtExceptionHandler::new);
	}

	/**
	 * Supply the executor to be used when executing async methods.
	 * @param defaultExecutor the {@code Executor} (typically a Spring {@code AsyncTaskExecutor}
	 * or {@link java.util.concurrent.ExecutorService}) to delegate to, unless a more specific
	 * executor has been requested via a qualifier on the async method, in which case the
	 * executor will be looked up at invocation time against the enclosing bean factory
	 * @see #getExecutorQualifier(Method)
	 * @see #setBeanFactory(BeanFactory)
	 * @see #getDefaultExecutor(BeanFactory)
	 */
	// 提供执行异步方法时要使用的执行器。
	// 参形：
	// 			defaultExecutor – 要委托给的Executor （通常是 Spring AsyncTaskExecutor或
	// 			java.util.concurrent.ExecutorService ），除非通过异步方法上的限定符请求了更具体的执行器，
	// 			在这种情况下，将在调用时查找执行器而不是 beanFactory
	public void setExecutor(Executor defaultExecutor) {
		this.defaultExecutor = SingletonSupplier.of(defaultExecutor);
	}

	/**
	 * Supply the {@link AsyncUncaughtExceptionHandler} to use to handle exceptions
	 * thrown by invoking asynchronous methods with a {@code void} return type.
	 */
	// 提供 AsyncUncaughtExceptionHandler 以用于处理通过调用具有 void 返回类型的异步方法引发的异常
	public void setExceptionHandler(AsyncUncaughtExceptionHandler exceptionHandler) {
		this.exceptionHandler = SingletonSupplier.of(exceptionHandler);
	}

	/**
	 * Set the {@link BeanFactory} to be used when looking up executors by qualifier
	 * or when relying on the default executor lookup algorithm.
	 * @see #findQualifiedExecutor(BeanFactory, String)
	 * @see #getDefaultExecutor(BeanFactory)
	 */
	// 设置在通过限定符查找执行程序或依赖默认执行程序查找算法时使用的 BeanFactory 。
	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}


	/**
	 * Determine the specific executor to use when executing the given method.
	 * Should preferably return an {@link AsyncListenableTaskExecutor} implementation.
	 * @return the executor to use (or {@code null}, but just if no default executor is available)
	 */
	// 确定执行给定方法时要使用的特定执行程序。最好返回一个 AsyncListenableTaskExecutor 实现。
	// 返回值：
	//			要使用的执行程序（或null ，但前提是没有可用的默认执行程序）
	@Nullable
	protected AsyncTaskExecutor determineAsyncExecutor(Method method) {
		AsyncTaskExecutor executor = this.executors.get(method);
		if (executor == null) {
			Executor targetExecutor;
			// 方法限定名
			String qualifier = getExecutorQualifier(method);
			if (StringUtils.hasLength(qualifier)) {
				targetExecutor = findQualifiedExecutor(this.beanFactory, qualifier);
			}
			else {
				targetExecutor = this.defaultExecutor.get();
			}
			if (targetExecutor == null) {
				return null;
			}
			executor = (targetExecutor instanceof AsyncListenableTaskExecutor ?
					(AsyncListenableTaskExecutor) targetExecutor : new TaskExecutorAdapter(targetExecutor));
			this.executors.put(method, executor);
		}
		return executor;
	}

	/**
	 * Return the qualifier or bean name of the executor to be used when executing the
	 * given async method, typically specified in the form of an annotation attribute.
	 * Returning an empty string or {@code null} indicates that no specific executor has
	 * been specified and that the {@linkplain #setExecutor(Executor) default executor}
	 * should be used.
	 * @param method the method to inspect for executor qualifier metadata
	 * @return the qualifier if specified, otherwise empty String or {@code null}
	 * @see #determineAsyncExecutor(Method)
	 * @see #findQualifiedExecutor(BeanFactory, String)
	 */
	// 返回执行给定异步方法时要使用的执行程序的限定符或 bean 名称，通常以注释属性的形式指定。返回一个空字符串
	// 或 null 表示没有指定特定的执行器，应该使用默认的执行器。
	// 参形：
	//			method - 检查执行者限定符元数据的方法
	// 返回值：
	//			限定符（如果指定），否则为空 String 或null
	@Nullable
	protected abstract String getExecutorQualifier(Method method);

	/**
	 * Retrieve a target executor for the given qualifier.
	 * @param qualifier the qualifier to resolve
	 * @return the target executor, or {@code null} if none available
	 * @since 4.2.6
	 * @see #getExecutorQualifier(Method)
	 */
	// 检索给定限定符的目标执行器。
	// 参形：
	//			qualifier – 要解析的限定符
	// 返回值：
	//			目标执行者，如果没有可用则为null
	@Nullable
	protected Executor findQualifiedExecutor(@Nullable BeanFactory beanFactory, String qualifier) {
		if (beanFactory == null) {
			throw new IllegalStateException("BeanFactory must be set on " + getClass().getSimpleName() +
					" to access qualified executor '" + qualifier + "'");
		}
		return BeanFactoryAnnotationUtils.qualifiedBeanOfType(beanFactory, Executor.class, qualifier);
	}

	/**
	 * Retrieve or build a default executor for this advice instance.
	 * An executor returned from here will be cached for further use.
	 * <p>The default implementation searches for a unique {@link TaskExecutor} bean
	 * in the context, or for an {@link Executor} bean named "taskExecutor" otherwise.
	 * If neither of the two is resolvable, this implementation will return {@code null}.
	 * @param beanFactory the BeanFactory to use for a default executor lookup
	 * @return the default executor, or {@code null} if none available
	 * @since 4.2.6
	 * @see #findQualifiedExecutor(BeanFactory, String)
	 * @see #DEFAULT_TASK_EXECUTOR_BEAN_NAME
	 */
	// 为此建议实例检索或构建默认执行程序。从这里返回的执行程序将被缓存以供进一步使用。
	//
	// 默认实现在上下文中搜索唯一的TaskExecutor bean，否则搜索名为 “taskExecutor” 的 Executor bean。
	// 如果两者都不可解析，则此实现将返回null 。
	// 参形：
	//			beanFactory - 用于默认执行程序查找的 BeanFactory
	// 返回值：
	//			默认执行程序，如果没有可用，则为null
	@Nullable
	protected Executor getDefaultExecutor(@Nullable BeanFactory beanFactory) {
		if (beanFactory != null) {
			try {
				// Search for TaskExecutor bean... not plain Executor since that would
				// match with ScheduledExecutorService as well, which is unusable for
				// our purposes here. TaskExecutor is more clearly designed for it.
				// 搜索 TaskExecutor bean...不是普通的 Executor，因为它也会与 ScheduledExecutorService 匹配，
				// 这对于我们的目的来说是不可用的。 TaskExecutor 是为它设计的。
				return beanFactory.getBean(TaskExecutor.class);
			}
			catch (NoUniqueBeanDefinitionException ex) {
				logger.debug("Could not find unique TaskExecutor bean", ex);
				try {
					// 依赖查找
					return beanFactory.getBean(DEFAULT_TASK_EXECUTOR_BEAN_NAME, Executor.class);
				}
				catch (NoSuchBeanDefinitionException ex2) {
					if (logger.isInfoEnabled()) {
						logger.info("More than one TaskExecutor bean found within the context, and none is named " +
								"'taskExecutor'. Mark one of them as primary or name it 'taskExecutor' (possibly " +
								"as an alias) in order to use it for async processing: " + ex.getBeanNamesFound());
					}
				}
			}
			catch (NoSuchBeanDefinitionException ex) {
				logger.debug("Could not find default TaskExecutor bean", ex);
				try {
					// 依赖查找
					return beanFactory.getBean(DEFAULT_TASK_EXECUTOR_BEAN_NAME, Executor.class);
				}
				catch (NoSuchBeanDefinitionException ex2) {
					logger.info("No task executor bean found for async processing: " +
							"no bean of type TaskExecutor and no bean named 'taskExecutor' either");
				}
				// Giving up -> either using local default executor or none at all...
				// 放弃 -> 要么使用本地默认执行程序，要么根本不使用.....
			}
		}
		return null;
	}


	/**
	 * Delegate for actually executing the given task with the chosen executor.
	 * @param task the task to execute
	 * @param executor the chosen executor
	 * @param returnType the declared return type (potentially a {@link Future} variant)
	 * @return the execution result (potentially a corresponding {@link Future} handle)
	 */
	// 使用选定的执行者实际执行给定任务的委托。
	// 参形：
	//				task – 要执行的任务
	//				executor – 选定的执行者
	//				returnType – 声明的返回类型（可能是Future变体）
	// 返回值：
	//				执行结果（可能是对应的Future句柄
	// 异步：不阻塞主流程执行。
	@Nullable
	protected Object doSubmit(Callable<Object> task, AsyncTaskExecutor executor, Class<?> returnType) {
		// Spring 支持 CompletableFuture，ListenableFuture，Future 三种
		if (CompletableFuture.class.isAssignableFrom(returnType)) {
			return CompletableFuture.supplyAsync(() -> {
				try {
					// 异步任务回调，不阻塞主流程，慢慢执行
					return task.call();
				}
				catch (Throwable ex) {
					throw new CompletionException(ex);
				}
			}, executor); // 这里可以切换线程池，从外部传入的线程池
		}
		else if (ListenableFuture.class.isAssignableFrom(returnType)) {
			// 提交 Callable 任务以执行，接收代表该任务的 ListenableFuture 。 Future 将在完成后返回 Callable 的结果
			// 需要得到结果
			return ((AsyncListenableTaskExecutor) executor).submitListenable(task);
		}
		else if (Future.class.isAssignableFrom(returnType)) {
			// 需要得到执行结果
			return executor.submit(task);
		}
		else {
			executor.submit(task);
			// 其他情况返回 null, 对于线程池来说并不重要。
			return null;
		}
	}

	/**
	 * Handles a fatal error thrown while asynchronously invoking the specified
	 * {@link Method}.
	 * <p>If the return type of the method is a {@link Future} object, the original
	 * exception can be propagated by just throwing it at the higher level. However,
	 * for all other cases, the exception will not be transmitted back to the client.
	 * In that later case, the current {@link AsyncUncaughtExceptionHandler} will be
	 * used to manage such exception.
	 * @param ex the exception to handle
	 * @param method the method that was invoked
	 * @param params the parameters used to invoke the method
	 */
	// 处理异步调用指定 Method 时引发的致命错误。
	//
	// 如果方法的返回类型是 Future 对象，则可以通过将其抛出到更高级别来传播原始异常。但是，对于所有其他情况，
	// 异常不会被传输回客户端。在后一种情况下，当前的 AsyncUncaughtExceptionHandler 将用于管理此类异常。
	// 参形：
	//			ex – 要处理的异常
	//			method - 被调用的方法
	//			params – 用于调用方法的参数
	protected void handleError(Throwable ex, Method method, Object... params) throws Exception {
		if (Future.class.isAssignableFrom(method.getReturnType())) {
			// 重新抛出给定的exception
			ReflectionUtils.rethrowException(ex);
		}
		else {
			// Could not transmit the exception to the caller with default executor
			// 无法使用默认执行程序将异常传输给调用者
			try {
				// 一个默认的 AsyncUncaughtExceptionHandler，它只记录异常
				this.exceptionHandler.obtain().handleUncaughtException(ex, method, params);
			}
			catch (Throwable ex2) {
				logger.warn("Exception handler for async method '" + method.toGenericString() +
						"' threw unexpected exception itself", ex2);
			}
		}
	}

}
