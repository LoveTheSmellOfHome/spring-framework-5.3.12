/*
 * Copyright 2002-2016 the original author or authors.
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

package org.aopalliance.intercept;

import java.lang.reflect.Method;

import javax.annotation.Nonnull;

/**
 * Description of an invocation to a method, given to an interceptor
 * upon method-call.
 *
 * <p>A method invocation is a joinpoint and can be intercepted by a
 * method interceptor.
 *
 * @author Rod Johnson
 * @see MethodInterceptor
 */
// 方法调用的描述，在方法调用时提供给拦截器。
// 方法调用是一个连接点，可以被方法拦截器拦截。间接说明了 Spring 里边它只支持方法级别的 Joinpoint
// Spring AOP 只实现了方法调用，没有实现构造器调用，会具体到某个方法上面去
//
// 命令模式：MethodInvocation 是被拦截的东西
public interface MethodInvocation extends Invocation {

	/**
	 * Get the method being called.
	 * <p>This method is a friendly implementation of the
	 * {@link Joinpoint#getStaticPart()} method (same result).
	 * @return the method being called
	 */
	// 获取被调用的方法。
	// 和 {@link org.aopalliance.intercept.Joinpoint}#getStaticPart() 返回的是同一个对象
	// 此方法是 Joinpoint.getStaticPart() 方法的友好实现（结果相同）。
	// 返回值：
	//			被调用的方法
	@Nonnull
	Method getMethod();

}
