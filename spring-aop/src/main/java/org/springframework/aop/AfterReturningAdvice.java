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

package org.springframework.aop;

import org.springframework.lang.Nullable;

import java.lang.reflect.Method;

/**
 * After returning advice is invoked only on normal method return, not if an
 * exception is thrown. Such advice can see the return value, but cannot change it.
 *
 * @author Rod Johnson
 * @see MethodBeforeAdvice
 * @see ThrowsAdvice
 */
// 返回后通知仅在正常方法返回时调用，而不是在抛出异常时调用。这样的通知可以看到返回值，但不能改变它
public interface AfterReturningAdvice extends AfterAdvice {

	/**
	 * Callback after a given method successfully returned.
	 * @param returnValue the value returned by the method, if any
	 * @param method the method being invoked
	 * @param args the arguments to the method
	 * @param target the target of the method invocation. May be {@code null}.
	 * @throws Throwable if this object wishes to abort the call.
	 * Any exception thrown will be returned to the caller if it's
	 * allowed by the method signature. Otherwise the exception
	 * will be wrapped as a runtime exception.
	 */
	// 给定方法成功返回后的回调。
	// 形参：
	//			returnValue – 方法返回的值，如果有的话
	//			method- 被调用的方法
	//			args – 方法的参数
	//			target – 方法调用的目标。 可能为null 。
	// 异常：
	//			Throwable - 如果此对象希望中止调用。 如果方法签名允许，任何抛出的异常都将返回给调用者。
	//			否则异常将被包装为运行时异常。
	void afterReturning(@Nullable Object returnValue, Method method, Object[] args, @Nullable Object target) throws Throwable;

}
