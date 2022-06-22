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

package org.springframework.aop.framework.adapter;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.BeforeAdvice;
import org.springframework.aop.MethodBeforeAdvice;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.io.Serializable;

/**
 * Interceptor to wrap a {@link MethodBeforeAdvice}.
 * <p>Used internally by the AOP framework; application developers should not
 * need to use this class directly.
 *
 * @author Rod Johnson
 * @see AfterReturningAdviceInterceptor
 * @see ThrowsAdviceInterceptor
 */
// 用于包装 MethodBeforeAdvice 的拦截器。Joinpoint Before Advice 标准实现
// 由 AOP 框架内部使用； 应用程序开发人员不需要直接使用此类。
// 通过组合 + 继承的方式来实现，实现了BeforeAdvice，和 MethodInterceptor 拦截两个语义。
@SuppressWarnings("serial")
public class MethodBeforeAdviceInterceptor implements MethodInterceptor, BeforeAdvice, Serializable {

	// MethodBeforeAdvice 里边有接口的定义，BeforeAdvice 没有接口定义，一般用户自定义的 MethodBeforeAdvice
	// 都会包装成 MethodBeforeAdviceInterceptor 拦截器，一一对应。
	private final MethodBeforeAdvice advice;


	/**
	 * Create a new MethodBeforeAdviceInterceptor for the given advice.
	 * @param advice the MethodBeforeAdvice to wrap
	 */
	public MethodBeforeAdviceInterceptor(MethodBeforeAdvice advice) {
		Assert.notNull(advice, "Advice must not be null");
		this.advice = advice;
	}


	// 开发人员调用的是 invoke() 方法，它来自于 Advice
	@Override
	@Nullable
	public Object invoke(MethodInvocation mi) throws Throwable {
		// 处理 @Before,它将拦截器获取的信息传递给 Advice,这就是开发者实现 MethodBeforeAdvice，可以被回调的原因
		// 所有的拦截都是来自 MethodBeforeAdviceInterceptor，即所有的 MethodBeforeAdvice 类型的自定义拦截
		// 都是来自 MethodBeforeAdviceInterceptor，换言之，有多少个 MethodBeforeAdvice,就有多少个 MethodBeforeAdviceInterceptor
		// 的实现。这个实现通常是给 Spring 内部框架实现的
		this.advice.before(mi.getMethod(), mi.getArguments(), mi.getThis());
		// 执行拦截方法
		return mi.proceed();
	}

}
