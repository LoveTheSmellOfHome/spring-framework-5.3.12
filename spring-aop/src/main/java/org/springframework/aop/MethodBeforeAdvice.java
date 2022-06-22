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

package org.springframework.aop;

import java.lang.reflect.Method;

import org.springframework.lang.Nullable;

/**
 * Advice invoked before a method is invoked. Such advices cannot
 * prevent the method call proceeding, unless they throw a Throwable.
 *
 * @author Rod Johnson
 * @see AfterReturningAdvice
 * @see ThrowsAdvice
 */
// 在调用方法之前调用的建议动作。 这样的建议不能阻止方法调用的进行，除非它们抛出一个 Throwable
public interface MethodBeforeAdvice extends BeforeAdvice {

	/**
	 * Callback before a given method is invoked.
	 * @param method the method being invoked
	 * @param args the arguments to the method
	 * @param target the target of the method invocation. May be {@code null}.
	 * @throws Throwable if this object wishes to abort the call.
	 * Any exception thrown will be returned to the caller if it's
	 * allowed by the method signature. Otherwise the exception
	 * will be wrapped as a runtime exception.
	 */
	// 调用给定方法之前的回调。
	// 形参：
	//			method - 被调用的方法，其实它就是 Joinpoint
	//			args – 方法的参数
	//			target – 方法调用的目标。 可能为null 。
	// 异常：
	//			Throwable - 如果此对象希望中止调用。 如果方法签名允许，任何抛出的异常都将返回给调用者。
	//			否则异常将被包装为运行时异常。
	void before(Method method, Object[] args, @Nullable Object target) throws Throwable;

}
