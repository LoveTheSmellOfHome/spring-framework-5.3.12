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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.aop.framework.autoproxy.AbstractBeanFactoryAwareAdvisingPostProcessor;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.core.task.TaskExecutor;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.function.SingletonSupplier;

import java.lang.annotation.Annotation;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

/**
 * Bean post-processor that automatically applies asynchronous invocation
 * behavior to any bean that carries the {@link Async} annotation at class or
 * method-level by adding a corresponding {@link AsyncAnnotationAdvisor} to the
 * exposed proxy (either an existing AOP proxy or a newly generated proxy that
 * implements all of the target's interfaces).
 *
 * <p>The {@link TaskExecutor} responsible for the asynchronous execution may
 * be provided as well as the annotation type that indicates a method should be
 * invoked asynchronously. If no annotation type is specified, this post-
 * processor will detect both Spring's {@link Async @Async} annotation as well
 * as the EJB 3.1 {@code javax.ejb.Asynchronous} annotation.
 *
 * <p>For methods having a {@code void} return type, any exception thrown
 * during the asynchronous method invocation cannot be accessed by the
 * caller. An {@link AsyncUncaughtExceptionHandler} can be specified to handle
 * these cases.
 *
 * <p>Note: The underlying async advisor applies before existing advisors by default,
 * in order to switch to async execution as early as possible in the invocation chain.
 *
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @since 3.0
 * @see Async
 * @see AsyncAnnotationAdvisor
 * @see #setBeforeExistingAdvisors
 * @see ScheduledAnnotationBeanPostProcessor
 */
// 通过将相应的 Async AsyncAnnotationAdvisor 到暴露的代理（现有的 AOP 代理或新生成的实现所有目标接口）。
//
// 可以提供负责异步执行的 TaskExecutor 以及指示应该异步调用方法的注解类型。如果未指定注解类型，
// 则此后处理器将检测 Spring 的 @Async 注解以及 EJB 3.1 javax.ejb.Asynchronous 注解。
//
// 对于返回类型为 void 的方法，调用者无法访问异步方法调用期间抛出的任何异常。可以
// 指定 AsyncUncaughtExceptionHandler 来处理这些情况。
//
// 注意：默认情况下，底层异步顾问在现有顾问之前应用，以便在调用链中尽早切换到异步执行
@SuppressWarnings("serial")
public class AsyncAnnotationBeanPostProcessor extends AbstractBeanFactoryAwareAdvisingPostProcessor {

	/**
	 * The default name of the {@link TaskExecutor} bean to pick up: "taskExecutor".
	 * <p>Note that the initial lookup happens by type; this is just the fallback
	 * in case of multiple executor beans found in the context.
	 * @since 4.2
	 * @see AnnotationAsyncExecutionInterceptor#DEFAULT_TASK_EXECUTOR_BEAN_NAME
	 */
	// 要获取的 TaskExecutor bean 的默认名称：“taskExecutor”。
	// 请注意，初始查找是按类型进行的；这只是在上下文中发现多个执行器 bean 的情况下的后备。
	public static final String DEFAULT_TASK_EXECUTOR_BEAN_NAME =
			AnnotationAsyncExecutionInterceptor.DEFAULT_TASK_EXECUTOR_BEAN_NAME;


	protected final Log logger = LogFactory.getLog(getClass());

	// 执行器
	@Nullable
	private Supplier<Executor> executor;

	// 异步异常处理器
	@Nullable
	private Supplier<AsyncUncaughtExceptionHandler> exceptionHandler;

	@Nullable
	private Class<? extends Annotation> asyncAnnotationType;



	public AsyncAnnotationBeanPostProcessor() {
		setBeforeExistingAdvisors(true);
	}


	/**
	 * Configure this post-processor with the given executor and exception handler suppliers,
	 * applying the corresponding default if a supplier is not resolvable.
	 * @since 5.1
	 */
	// 使用给定的执行程序和异常处理程序供应商配置此后处理器，如果供应商不可解析，则应用相应的默认值
	public void configure(
			@Nullable Supplier<Executor> executor, @Nullable Supplier<AsyncUncaughtExceptionHandler> exceptionHandler) {

		this.executor = executor;
		this.exceptionHandler = exceptionHandler;
	}

	/**
	 * Set the {@link Executor} to use when invoking methods asynchronously.
	 * <p>If not specified, default executor resolution will apply: searching for a
	 * unique {@link TaskExecutor} bean in the context, or for an {@link Executor}
	 * bean named "taskExecutor" otherwise. If neither of the two is resolvable,
	 * a local default executor will be created within the interceptor.
	 * @see AnnotationAsyncExecutionInterceptor#getDefaultExecutor(BeanFactory)
	 * @see #DEFAULT_TASK_EXECUTOR_BEAN_NAME
	 */
	// 设置异步调用方法时使用的Executor 。
	// 
	// 如果未指定，则将应用默认执行器解析：在上下文中搜索唯一的 TaskExecutor bean，否则搜索名
	// 为 “taskExecutor” 的 Executor bean。如果两者都无法解析，则将在拦截器中创建本地默认执行器。
	public void setExecutor(Executor executor) {
		this.executor = SingletonSupplier.of(executor);
	}

	/**
	 * Set the {@link AsyncUncaughtExceptionHandler} to use to handle uncaught
	 * exceptions thrown by asynchronous method executions.
	 * @since 4.1
	 */
	// 设置 AsyncUncaughtExceptionHandler 以用于处理异步方法执行引发的未捕获异常。
	public void setExceptionHandler(AsyncUncaughtExceptionHandler exceptionHandler) {
		this.exceptionHandler = SingletonSupplier.of(exceptionHandler);
	}

	/**
	 * Set the 'async' annotation type to be detected at either class or method
	 * level. By default, both the {@link Async} annotation and the EJB 3.1
	 * {@code javax.ejb.Asynchronous} annotation will be detected.
	 * <p>This setter property exists so that developers can provide their own
	 * (non-Spring-specific) annotation type to indicate that a method (or all
	 * methods of a given class) should be invoked asynchronously.
	 * @param asyncAnnotationType the desired annotation type
	 */
	// 设置要在类或方法级别检测的“异步”注解类型。默认情况下，将检测Async注解和 EJB 3.1 javax.ejb.Asynchronous注解。
	// 
	// 这个 setter 属性的存在使得开发人员可以提供他们自己的（非 Spring 特定的） 注解类型来指示一个
	// 方法（或给定类的所有方法）应该被异步调用。
	// 参形：
	//			asyncAnnotationType – 所需的注解类型
	public void setAsyncAnnotationType(Class<? extends Annotation> asyncAnnotationType) {
		Assert.notNull(asyncAnnotationType, "'asyncAnnotationType' must not be null");
		this.asyncAnnotationType = asyncAnnotationType;
	}


	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		// 关联 IoC 容器
		super.setBeanFactory(beanFactory);

		// 关联 AsyncAnnotationAdvisor.
		AsyncAnnotationAdvisor advisor = new AsyncAnnotationAdvisor(this.executor, this.exceptionHandler);
		if (this.asyncAnnotationType != null) {
			// 设置异常注解类型
			advisor.setAsyncAnnotationType(this.asyncAnnotationType);
		}
		// advisor 关联 IoC 容器
		advisor.setBeanFactory(beanFactory);
		// 在这里关联了  @EnableAsync 注解的 advisor
		this.advisor = advisor;
	}

}
