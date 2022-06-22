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

package org.springframework.cache.interceptor;

import java.io.Serializable;
import java.lang.reflect.Method;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * AOP Alliance MethodInterceptor for declarative cache
 * management using the common Spring caching infrastructure
 * ({@link org.springframework.cache.Cache}).
 *
 * <p>Derives from the {@link CacheAspectSupport} class which
 * contains the integration with Spring's underlying caching API.
 * CacheInterceptor simply calls the relevant superclass methods
 * in the correct order.
 *
 * <p>CacheInterceptors are thread-safe.
 *
 * @author Costin Leau
 * @author Juergen Hoeller
 * @since 3.1
 */
// AOP 联盟 MethodInterceptor 用于声明式缓存管理，使用常见的 Spring 缓存基础架构（ org.springframework.cache.Cache ）。
//
// 派生自 CacheAspectSupport 类，该类包含与 Spring 的底层缓存 API 的集成。
// CacheInterceptor 只是以正确的顺序调用相关的超类方法。
@SuppressWarnings("serial")
public class CacheInterceptor extends CacheAspectSupport implements MethodInterceptor, Serializable {

	@Override
	@Nullable
	public Object invoke(final MethodInvocation invocation) throws Throwable {
		// 首先获取方法
		Method method = invocation.getMethod();

		// 回调操作，在这里还没执行
		CacheOperationInvoker aopAllianceInvoker = () -> {
			try {
				// 拦截方法调用
				return invocation.proceed();
			}
			catch (Throwable ex) {
				throw new CacheOperationInvoker.ThrowableWrapper(ex);
			}
		};

		// 获取调用对象
		Object target = invocation.getThis();
		Assert.state(target != null, "Target must not be null");
		try {
			// 方法在这里执行
			return execute(aopAllianceInvoker, target, method, invocation.getArguments());
		}
		catch (CacheOperationInvoker.ThrowableWrapper th) {
			throw th.getOriginal();
		}
	}

}
