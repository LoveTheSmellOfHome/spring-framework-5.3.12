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
import org.springframework.aop.AfterAdvice;
import org.springframework.aop.AfterReturningAdvice;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.io.Serializable;

/**
 * Interceptor to wrap an {@link org.springframework.aop.AfterReturningAdvice}.
 * Used internally by the AOP framework; application developers should not need
 * to use this class directly.
 *
 * @author Rod Johnson
 * @see MethodBeforeAdviceInterceptor
 * @see ThrowsAdviceInterceptor
 */
// 用于包装AfterReturningAdvice的拦截器。 由 AOP 框架内部使用； 应用程序开发人员不需要直接使用此类
// 每个用户的 Advice 都会包装成一个 Interceptor 来实现。
@SuppressWarnings("serial")
public class AfterReturningAdviceInterceptor implements MethodInterceptor, AfterAdvice, Serializable {

	// 用户自定义实现的 AfterReturningAdvice
	private final AfterReturningAdvice advice;


	/**
	 * Create a new AfterReturningAdviceInterceptor for the given advice.
	 * @param advice the AfterReturningAdvice to wrap
	 */
	// 为给定的建议创建一个新的 AfterReturningAdviceInterceptor。
	// 参形：
	//			advice – 要包装的 AfterReturningAdvice
	public AfterReturningAdviceInterceptor(AfterReturningAdvice advice) {
		Assert.notNull(advice, "Advice must not be null");
		this.advice = advice;
	}


	@Override
	@Nullable
		public Object invoke(MethodInvocation mi) throws Throwable {
		// 目标方法调用i、
		Object retVal = mi.proceed();
		// 目标方法返回后的回调，将方法的放回值作为参数传入后置回调，包含了返回值，方法，参数，调用对象
		this.advice.afterReturning(retVal, mi.getMethod(), mi.getArguments(), mi.getThis());
		return retVal;
	}

}
