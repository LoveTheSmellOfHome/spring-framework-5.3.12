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

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

import org.aopalliance.aop.Advice;

import org.springframework.aop.Pointcut;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.aop.support.AbstractPointcutAdvisor;
import org.springframework.aop.support.ComposablePointcut;
import org.springframework.aop.support.annotation.AnnotationMatchingPointcut;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.function.SingletonSupplier;

/**
 * Advisor that activates asynchronous method execution through the {@link Async}
 * annotation. This annotation can be used at the method and type level in
 * implementation classes as well as in service interfaces.
 *
 * <p>This advisor detects the EJB 3.1 {@code javax.ejb.Asynchronous}
 * annotation as well, treating it exactly like Spring's own {@code Async}.
 * Furthermore, a custom async annotation type may get specified through the
 * {@link #setAsyncAnnotationType "asyncAnnotationType"} property.
 *
 * @author Juergen Hoeller
 * @since 3.0
 * @see Async
 * @see AnnotationAsyncExecutionInterceptor
 */
// 通过 @Async 注解激活异步方法执行的顾问。此注解可用于实现类以及服务接口中的方法和类型级别。
//
// 该顾问程序也检测 EJB 3.1 javax.ejb.Asynchronous 注解，将其视为 Spring 自己的 Async 。
// 此外，可以通过 "asyncAnnotationType" 属性指定自定义异步注解类型
@SuppressWarnings("serial")
public class AsyncAnnotationAdvisor extends AbstractPointcutAdvisor implements BeanFactoryAware {

	// Spring AOP 执行动过
	private Advice advice;

	// 判断筛选条件
	private Pointcut pointcut;


	/**
	 * Create a new {@code AsyncAnnotationAdvisor} for bean-style configuration.
	 */
	// 为 bean 样式配置创建一个新的 AsyncAnnotationAdvisor 。
	public AsyncAnnotationAdvisor() {
		this((Supplier<Executor>) null, (Supplier<AsyncUncaughtExceptionHandler>) null);
	}

	/**
	 * Create a new {@code AsyncAnnotationAdvisor} for the given task executor.
	 * @param executor the task executor to use for asynchronous methods
	 * (can be {@code null} to trigger default executor resolution)
	 * @param exceptionHandler the {@link AsyncUncaughtExceptionHandler} to use to
	 * handle unexpected exception thrown by asynchronous method executions
	 * @see AnnotationAsyncExecutionInterceptor#getDefaultExecutor(BeanFactory)
	 */
	// 为给定的任务执行器创建一个新的 AsyncAnnotationAdvisor 。
	// 参形：
	//			executor – 用于异步方法的任务执行器（可以为null以触发默认执行器解析）
	//			exceptionHandler - 用于处理异步方法执行引发的意外异常的 AsyncUncaughtExceptionHandler
	// 请参阅：
	//			AnnotationAsyncExecutionInterceptor.getDefaultExecutor(BeanFactory)
	public AsyncAnnotationAdvisor(
			@Nullable Executor executor, @Nullable AsyncUncaughtExceptionHandler exceptionHandler) {

		this(SingletonSupplier.ofNullable(executor), SingletonSupplier.ofNullable(exceptionHandler));
	}

	/**
	 * Create a new {@code AsyncAnnotationAdvisor} for the given task executor.
	 * @param executor the task executor to use for asynchronous methods
	 * (can be {@code null} to trigger default executor resolution)
	 * @param exceptionHandler the {@link AsyncUncaughtExceptionHandler} to use to
	 * handle unexpected exception thrown by asynchronous method executions
	 * @since 5.1
	 * @see AnnotationAsyncExecutionInterceptor#getDefaultExecutor(BeanFactory)
	 */
	// 为给定的任务执行器创建一个新的 AsyncAnnotationAdvisor 。
	// 参形：
	//			executor – 用于异步方法的任务执行器（可以为null以触发默认执行器解析）
	//			exceptionHandler - 用于处理异步方法执行引发的意外异常的 AsyncUncaughtExceptionHandler
	@SuppressWarnings("unchecked")
	public AsyncAnnotationAdvisor(
			@Nullable Supplier<Executor> executor, @Nullable Supplier<AsyncUncaughtExceptionHandler> exceptionHandler) {

		// 异步注解类型
		Set<Class<? extends Annotation>> asyncAnnotationTypes = new LinkedHashSet<>(2);
		// 添加 @Async
		asyncAnnotationTypes.add(Async.class);
		try {
			// 添加 @Asynchronous
			asyncAnnotationTypes.add((Class<? extends Annotation>)
					ClassUtils.forName("javax.ejb.Asynchronous", AsyncAnnotationAdvisor.class.getClassLoader()));
		}
		catch (ClassNotFoundException ex) {
			// If EJB 3.1 API not present, simply ignore.
		}
		// 创建 Advice，关联 Executor 执行器
		this.advice = buildAdvice(executor, exceptionHandler);
		// 创建 Pointcut,@EnableAsync 在这里构建了 Pointcut
		this.pointcut = buildPointcut(asyncAnnotationTypes);
	}


	/**
	 * Set the 'async' annotation type.
	 * <p>The default async annotation type is the {@link Async} annotation, as well
	 * as the EJB 3.1 {@code javax.ejb.Asynchronous} annotation (if present).
	 * <p>This setter property exists so that developers can provide their own
	 * (non-Spring-specific) annotation type to indicate that a method is to
	 * be executed asynchronously.
	 * @param asyncAnnotationType the desired annotation type
	 */
	// 设置 “@async” 注解类型。
	// 默认的异步注解类型是 @Async 注解，以及 EJB 3.1 javax.ejb.Asynchronous注解（如果存在）。
	// 存在这个 setter 属性，以便开发人员可以提供他们自己的（非 Spring 特定的）注解类型来指示要异步执行的方法。
	// 参形：
	//			asyncAnnotationType – 所需的注解类型
	public void setAsyncAnnotationType(Class<? extends Annotation> asyncAnnotationType) {
		Assert.notNull(asyncAnnotationType, "'asyncAnnotationType' must not be null");
		Set<Class<? extends Annotation>> asyncAnnotationTypes = new HashSet<>();
		// 添加注解类型
		asyncAnnotationTypes.add(asyncAnnotationType);
		this.pointcut = buildPointcut(asyncAnnotationTypes);
	}

	/**
	 * Set the {@code BeanFactory} to be used when looking up executors by qualifier.
	 */
	// 设置通过限定符查找执行器时要使用的 BeanFactory 。
	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		if (this.advice instanceof BeanFactoryAware) {
			((BeanFactoryAware) this.advice).setBeanFactory(beanFactory);
		}
	}


	@Override
	public Advice getAdvice() {
		return this.advice;
	}

	@Override
	public Pointcut getPointcut() {
		return this.pointcut;
	}


	protected Advice buildAdvice(
			@Nullable Supplier<Executor> executor, @Nullable Supplier<AsyncUncaughtExceptionHandler> exceptionHandler) {

		// 获取 异步注解执行拦截器
		AnnotationAsyncExecutionInterceptor interceptor = new AnnotationAsyncExecutionInterceptor(null);
		// 拦截器配置线程池
		interceptor.configure(executor, exceptionHandler);
		return interceptor;
	}

	/**
	 * Calculate a pointcut for the given async annotation types, if any.
	 * @param asyncAnnotationTypes the async annotation types to introspect
	 * @return the applicable Pointcut object, or {@code null} if none
	 */
	// 计算给定异步注解类型的切入点（如果有）。
	// 参形：
	//			asyncAnnotationTypes – 要自省的异步注解类型,注解列表
	// 返回值：
	//			适用的切入点对象，如果没有则为null
	protected Pointcut buildPointcut(Set<Class<? extends Annotation>> asyncAnnotationTypes) {
		ComposablePointcut result = null;
		for (Class<? extends Annotation> asyncAnnotationType : asyncAnnotationTypes) {
			// 处理了注解，处理注解 @Async 是否被标注上
			Pointcut cpc = new AnnotationMatchingPointcut(asyncAnnotationType, true);
			Pointcut mpc = new AnnotationMatchingPointcut(null, asyncAnnotationType, true);
			if (result == null) {
				result = new ComposablePointcut(cpc);
			}
			else {
				result.union(cpc);
			}
			result = result.union(mpc);
		}
		return (result != null ? result : Pointcut.TRUE);
	}

}
