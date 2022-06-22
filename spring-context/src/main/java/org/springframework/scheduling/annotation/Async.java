/*
 * Copyright 2002-2019 the original author or authors.
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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation that marks a method as a candidate for <i>asynchronous</i> execution.
 * Can also be used at the type level, in which case all of the type's methods are
 * considered as asynchronous. Note, however, that {@code @Async} is not supported
 * on methods declared within a
 * {@link org.springframework.context.annotation.Configuration @Configuration} class.
 *
 * <p>In terms of target method signatures, any parameter types are supported.
 * However, the return type is constrained to either {@code void} or
 * {@link java.util.concurrent.Future}. In the latter case, you may declare the
 * more specific {@link org.springframework.util.concurrent.ListenableFuture} or
 * {@link java.util.concurrent.CompletableFuture} types which allow for richer
 * interaction with the asynchronous task and for immediate composition with
 * further processing steps.
 *
 * <p>A {@code Future} handle returned from the proxy will be an actual asynchronous
 * {@code Future} that can be used to track the result of the asynchronous method
 * execution. However, since the target method needs to implement the same signature,
 * it will have to return a temporary {@code Future} handle that just passes a value
 * through: e.g. Spring's {@link AsyncResult}, EJB 3.1's {@link javax.ejb.AsyncResult},
 * or {@link java.util.concurrent.CompletableFuture#completedFuture(Object)}.
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @since 3.0
 * @see AnnotationAsyncExecutionInterceptor
 * @see AsyncAnnotationAdvisor
 */
// 将方法标记为 异步 执行候选的注解。
// 也可以在类型级别使用，在这种情况下，所有类型的方法都被视为异步。但是请注意，
// 在 {@link org.springframework.context.annotation.Configuration @Configuration}
// 类中声明的方法不支持 {@code @Async}，也就是说类上同时标注 @Async 和 @Configuration 会使@Async失效
//
// Spring 异步注解
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Async {

	/**
	 * A qualifier value for the specified asynchronous operation(s).
	 * <p>May be used to determine the target executor to be used when executing
	 * the asynchronous operation(s), matching the qualifier value (or the bean
	 * name) of a specific {@link java.util.concurrent.Executor Executor} or
	 * {@link org.springframework.core.task.TaskExecutor TaskExecutor}
	 * bean definition.
	 * <p>When specified on a class-level {@code @Async} annotation, indicates that the
	 * given executor should be used for all methods within the class. Method-level use
	 * of {@code Async#value} always overrides any value set at the class level.
	 * @since 3.1.2
	 */
	// 指定异步操作的限定符值。
	//
	// 可用于确定执行异步操作时要使用的目标执行器，匹配特定 Executor 或 TaskExecutor bean 定义的
	// 限定符值（或 bean 名称）。
	//
	// 当在类级别的 @Async 注释上指定时，表示给定的执行器应该用于类中的所有方法。 Async#value 的方法级别使用
	// 始终覆盖在类级别设置的任何值。
	String value() default "";

}
