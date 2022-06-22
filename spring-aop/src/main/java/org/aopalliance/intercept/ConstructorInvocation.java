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

package org.aopalliance.intercept;

import java.lang.reflect.Constructor;

import javax.annotation.Nonnull;

/**
 * Description of an invocation to a constructor, given to an
 * interceptor upon constructor-call.
 *
 * <p>A constructor invocation is a joinpoint and can be intercepted
 * by a constructor interceptor.
 *
 * @author Rod Johnson
 * @see ConstructorInterceptor
 */
// 构造函数调用的描述，在构造函数调用时提供给拦截器。
// 构造函数调用是一个连接点，可以被构造函数拦截器拦截。
// 并没有被 Spring 实现，所以是不存在的。
public interface ConstructorInvocation extends Invocation {

	/**
	 * Get the constructor being called.
	 * <p>This method is a friendly implementation of the
	 * {@link Joinpoint#getStaticPart()} method (same result).
	 * @return the constructor being called
	 */
	// 获取被调用的构造函数。没有被 Spring AOP 实现，所以不会有构造器参数
	// 此方法是 Joinpoint.getStaticPart() 方法的友好实现（结果相同）。
	// 返回值：被调用的构造函数
	@Nonnull
	Constructor<?> getConstructor();

}
