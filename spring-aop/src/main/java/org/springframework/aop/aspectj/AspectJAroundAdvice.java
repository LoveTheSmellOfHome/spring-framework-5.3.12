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

package org.springframework.aop.aspectj;

import java.io.Serializable;
import java.lang.reflect.Method;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.weaver.tools.JoinPointMatch;

import org.springframework.aop.ProxyMethodInvocation;
import org.springframework.lang.Nullable;

/**
 * Spring AOP around advice (MethodInterceptor) that wraps
 * an AspectJ advice method. Exposes ProceedingJoinPoint.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 2.0
 */
// 包装 AspectJ 通知方法的通知 (MethodInterceptor) 周围的 Spring AOP。 暴露 ProceedingJoinPoint
// 处理 Spring AOP @Aroud 注解标注的方法
@SuppressWarnings("serial")
public class AspectJAroundAdvice extends AbstractAspectJAdvice implements MethodInterceptor, Serializable {

	// xml 中第一个参数需要显式注入
	public AspectJAroundAdvice(
			Method aspectJAroundAdviceMethod, AspectJExpressionPointcut pointcut, AspectInstanceFactory aif) {

		super(aspectJAroundAdviceMethod, pointcut, aif);
	}


	@Override
	public boolean isBeforeAdvice() {
		return false;
	}

	@Override
	public boolean isAfterAdvice() {
		return false;
	}

	@Override
	protected boolean supportsProceedingJoinPoint() {
		return true;
	}

	@Override
	@Nullable
	public Object invoke(MethodInvocation mi) throws Throwable {
		if (!(mi instanceof ProxyMethodInvocation)) {
			throw new IllegalStateException("MethodInvocation is not a Spring ProxyMethodInvocation: " + mi);
		}
		// 包装成 ProxyMethodInvocation
		ProxyMethodInvocation pmi = (ProxyMethodInvocation) mi;

		// ProceedingJoinPoint 就是与 MethodInterceptor 的invoke 方法参数 MethodInvocation 类似，
		// Object proceed = pjp.proceed(); 调用拦截的方法，只不过这是 AOP 中的实现，不属于 Spring.
		// 它只负责拦截具体掉不掉用由用户自己控制是否调用 pjp.proceed();
		ProceedingJoinPoint pjp = lazyGetProceedingJoinPoint(pmi);
		JoinPointMatch jpm = getJoinPointMatch(pmi);
		return invokeAdviceMethod(pjp, jpm, null, null);
	}

	/**
	 * Return the ProceedingJoinPoint for the current invocation,
	 * instantiating it lazily if it hasn't been bound to the thread already.
	 * @param rmi the current Spring AOP ReflectiveMethodInvocation,
	 * which we'll use for attribute binding
	 * @return the ProceedingJoinPoint to make available to advice methods
	 */
	// 返回当前调用的 ProceedingJoinPoint，如果它还没有绑定到线程，则延迟实例化它。
	// 形参：
	// 			rmi – 当前的 Spring AOP ReflectiveMethodInvocation，我们将用于属性绑定
	// 返回值：
	//			ProceedingJoinPoint 可用于通知方法
	protected ProceedingJoinPoint lazyGetProceedingJoinPoint(ProxyMethodInvocation rmi) {
		// 封装成 Spring 自己的基于方法级别的 MethodInvocationProceedingJoinPoint
		return new MethodInvocationProceedingJoinPoint(rmi);
	}

}
