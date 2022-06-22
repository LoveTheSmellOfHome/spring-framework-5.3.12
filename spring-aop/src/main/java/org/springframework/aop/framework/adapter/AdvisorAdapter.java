/*
 * Copyright 2002-2012 the original author or authors.
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

import org.aopalliance.aop.Advice;
import org.aopalliance.intercept.MethodInterceptor;

import org.springframework.aop.Advisor;

/**
 * Interface allowing extension to the Spring AOP framework to allow
 * handling of new Advisors and Advice types.
 *
 * <p>Implementing objects can create AOP Alliance Interceptors from
 * custom advice types, enabling these advice types to be used
 * in the Spring AOP framework, which uses interception under the covers.
 *
 * <p>There is no need for most Spring users to implement this interface;
 * do so only if you need to introduce more Advisor or Advice types to Spring.
 *
 * @author Rod Johnson
 */
// 接口允许扩展 Spring AOP 框架以允许处理新的顾问和建议类型。
// 实现对象可以从自定义通知类型创建 AOP 联盟拦截器，使这些通知类型能够在 Spring AOP 框架中使用，该框架在后台使用拦截。
// 大多数 Spring 用户不需要实现这个接口； 仅当您需要向 Spring 引入更多 Advisor 或 Advice 类型时才这样做。
//
// Spring AOP 的适配，基于 Advisor 的适配，将 Advisor 转换成 MethodInterceptor
//
// 适配器模式
public interface AdvisorAdapter {

	/**
	 * Does this adapter understand this advice object? Is it valid to
	 * invoke the {@code getInterceptors} method with an Advisor that
	 * contains this advice as an argument?
	 * @param advice an Advice such as a BeforeAdvice
	 * @return whether this adapter understands the given advice object
	 * @see #getInterceptor(org.springframework.aop.Advisor)
	 * @see org.springframework.aop.BeforeAdvice
	 */
	// 这个适配器是否理解这个建议对象？ 使用包含此建议作为参数的 Advisor 调用 getInterceptors 方法是否有效？
	// 参形：
	//			advice – 建议，例如 BeforeAdvice
	// 返回值：
	//			此适配器是否理解给定的建议对象
	boolean supportsAdvice(Advice advice);

	/**
	 * Return an AOP Alliance MethodInterceptor exposing the behavior of
	 * the given advice to an interception-based AOP framework.
	 * <p>Don't worry about any Pointcut contained in the Advisor;
	 * the AOP framework will take care of checking the pointcut.
	 * @param advisor the Advisor. The supportsAdvice() method must have
	 * returned true on this object
	 * @return an AOP Alliance interceptor for this Advisor. There's
	 * no need to cache instances for efficiency, as the AOP framework
	 * caches advice chains.
	 */
	// 返回一个 AOP Alliance MethodInterceptor，将给定通知的行为暴露给基于拦截的 AOP 框架。
	// 不要担心顾问中包含的任何切入点； AOP 框架将负责检查切入点。
	// 参形：
	//			advisor - 顾问。 supportsAdvice() 方法必须在此对象上返回 true，适配对象
	// 返回值：
	//			此顾问的 AOP 联盟拦截器。 无需缓存实例以提高效率，因为 AOP 框架缓存了建议链。目标对象
	MethodInterceptor getInterceptor(Advisor advisor);

}
