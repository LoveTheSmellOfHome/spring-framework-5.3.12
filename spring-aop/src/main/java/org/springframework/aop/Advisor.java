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

package org.springframework.aop;

import org.aopalliance.aop.Advice;

/**
 * Base interface holding AOP <b>advice</b> (action to take at a joinpoint)
 * and a filter determining the applicability of the advice (such as
 * a pointcut). <i>This interface is not for use by Spring users, but to
 * allow for commonality in support for different types of advice.</i>
 *
 * <p>Spring AOP is based around <b>around advice</b> delivered via method
 * <b>interception</b>, compliant with the AOP Alliance interception API.
 * The Advisor interface allows support for different types of advice,
 * such as <b>before</b> and <b>after</b> advice, which need not be
 * implemented using interception.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
// 包含了 AOP建议 advice （在连接点采取的操作）和确定 Advice 合适的过滤器（通常是切入点）的基准接口。
// 这个接口是一个基准接口，包含了一个 AOP 的 Advice,我们可以认为它是包含 Advice 的容器，
// 同时它有一个过滤器去决定哪个 Advice 是合适的，通常来讲是 Pointcut
// 这个接口不是供 Spring 用户使用的，而是一个内部实现，为了支持不同类型的通知的通用性。
//
// Spring AOP 基于 around advice 通过方法拦截调用提供的建议传递，兼容 AOP 联盟 AspectJ API。
// Advisor 口允许不同类型的 advice，如建议前 before advice 和建议后  after advice，它不必使用拦截实现的支持。
//
// 这个接口类似于 Holder 的概念，一个 Advisor 包含一个 Advice,一一对应，Spring 在传递 Advice 的时候都是通过传递
// Advisor 来传递的。这个接口本身不是 AOP 的概念，而是 Spring 基于封装的思想整合了 Advice。同时它有一个 PointcutAdvisor 接口
// 代表 Spring AOP 的执行动作
public interface Advisor {

	/**
	 * Common placeholder for an empty {@code Advice} to be returned from
	 * {@link #getAdvice()} if no proper advice has been configured (yet).
	 * @since 5.0
	 */
	// 如果尚未配置适当的建议，则从getAdvice()返回空Advice通用占位符。
	Advice EMPTY_ADVICE = new Advice() {};


	/**
	 * Return the advice part of this aspect. An advice may be an
	 * interceptor, a before advice, a throws advice, etc.
	 * @return the advice that should apply if the pointcut matches
	 * @see org.aopalliance.intercept.MethodInterceptor
	 * @see BeforeAdvice
	 * @see ThrowsAdvice
	 * @see AfterReturningAdvice
	 */
	// 返回此方面的(建议部分) Advice。 建议可以是拦截器、前建议、抛出建议等。
	// 返回值：
	//				如果切入点匹配，则应适用的建议
	// 取出容器内容
	Advice getAdvice();

	/**
	 * Return whether this advice is associated with a particular instance
	 * (for example, creating a mixin) or shared with all instances of
	 * the advised class obtained from the same Spring bean factory.
	 * <p><b>Note that this method is not currently used by the framework.</b>
	 * Typical Advisor implementations always return {@code true}.
	 * Use singleton/prototype bean definitions or appropriate programmatic
	 * proxy creation to ensure that Advisors have the correct lifecycle model.
	 * @return whether this advice is associated with a particular target instance
	 */
	// 返回此建议是与特定实例相关联（例如，创建一个 mixin）还是与从同一个 Spring bean 工厂获得的建议类的所有实例共享。
	//
	// 请注意，框架当前未使用此方法。 典型的 Advisor 实现总是返回true 。 使用单例/原型 bean 定义或适当的
	// 编程代理创建来确保顾问具有正确的生命周期模型。
	//
	// 返回值：
	// 				此通知是否与特定目标实例相关联
	// 判断模式：是否关联单个实例
	boolean isPerInstance();

}
