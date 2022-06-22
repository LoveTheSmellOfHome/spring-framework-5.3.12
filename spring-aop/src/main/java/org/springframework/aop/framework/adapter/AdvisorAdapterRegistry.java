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

package org.springframework.aop.framework.adapter;

import org.aopalliance.intercept.MethodInterceptor;

import org.springframework.aop.Advisor;

/**
 * Interface for registries of Advisor adapters.
 *
 * <p><i>This is an SPI interface, not to be implemented by any Spring user.</i>
 *
 * @author Rod Johnson
 * @author Rob Harrop
 */
// 顾问适配器注册表的接口。
// 这是一个 SPI 接口，任何 Spring 用户都不能实现。
// 存储并转换的注册中心，存储 AdvisorAdapter 转换器，将 Advisor 转换成一组 MethodInterceptor[]
//
// 享元模式（Flyweight）实现：将复杂结构共享，以提高内存使用率。比如 Java 中的堆结构。它是 AdvisorAdapter
// 的注册中心，这个注册中心是共享的。
public interface AdvisorAdapterRegistry {

	/**
	 * Return an {@link Advisor} wrapping the given advice.
	 * <p>Should by default at least support
	 * {@link org.aopalliance.intercept.MethodInterceptor},
	 * {@link org.springframework.aop.MethodBeforeAdvice},
	 * {@link org.springframework.aop.AfterReturningAdvice},
	 * {@link org.springframework.aop.ThrowsAdvice}.
	 * @param advice an object that should be an advice
	 * @return an Advisor wrapping the given advice (never {@code null};
	 * if the advice parameter is an Advisor, it is to be returned as-is)
	 * @throws UnknownAdviceTypeException if no registered advisor adapter
	 * can wrap the supposed advice
	 */
	// 返回包装给定建议的Advisor 。
	// 默认至少应该支持 MethodInterceptor , org.springframework.aop.MethodBeforeAdvice ,
	// org.springframework.aop.AfterReturningAdvice , org.springframework.aop.ThrowsAdvice 。
	// 参形：
	//			advice - 应该是建议的对象
	// 返回值：
	//			包装给定建议的顾问（绝不为null ；如果建议参数是顾问，则按原样返回）
	// 抛出：
	//			UnknownAdviceTypeException – 如果没有注册的顾问适配器可以包装假定的建议
	Advisor wrap(Object advice) throws UnknownAdviceTypeException;

	/**
	 * Return an array of AOP Alliance MethodInterceptors to allow use of the
	 * given Advisor in an interception-based framework.
	 * <p>Don't worry about the pointcut associated with the {@link Advisor}, if it is
	 * a {@link org.springframework.aop.PointcutAdvisor}: just return an interceptor.
	 * @param advisor the Advisor to find an interceptor for
	 * @return an array of MethodInterceptors to expose this Advisor's behavior
	 * @throws UnknownAdviceTypeException if the Advisor type is
	 * not understood by any registered AdvisorAdapter
	 */
	// 返回一个 AOP Alliance MethodInterceptors 数组，以允许在基于拦截的框架中使用给定的 Advisor。
	// 不要担心与 Advisor 关联的切入点，如果它是org.springframework.aop.PointcutAdvisor ：只需返回一个拦截器。
	// 参形：
	//			advisor – 寻找拦截器的顾问
	// 返回值：
	//			一组 MethodInterceptor 以公开此顾问的行为
	// 抛出：
	//			UnknownAdviceTypeException – 如果任何注册的 AdvisorAdapter 都不理解 Advisor 类型
	// 一个 Advisor 会返回多个 MethodInterceptor
	MethodInterceptor[] getInterceptors(Advisor advisor) throws UnknownAdviceTypeException;

	/**
	 * Register the given {@link AdvisorAdapter}. Note that it is not necessary to register
	 * adapters for an AOP Alliance Interceptors or Spring Advices: these must be
	 * automatically recognized by an {@code AdvisorAdapterRegistry} implementation.
	 * @param adapter an AdvisorAdapter that understands particular Advisor or Advice types
	 */
	// 注册给定的AdvisorAdapter 。 请注意，不必为 AOP 联盟拦截器或 Spring Advices 注册适配器：这些必须由AdvisorAdapterRegistry实现自动识别。
	// 参形：
	//			adapter – 了解特定顾问或建议类型的 AdvisorAdapter
	// 注册 AdvisorAdapter 适配器
	void registerAdvisorAdapter(AdvisorAdapter adapter);

}
