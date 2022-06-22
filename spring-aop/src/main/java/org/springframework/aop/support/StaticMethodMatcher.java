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

package org.springframework.aop.support;

import java.lang.reflect.Method;

import org.springframework.aop.MethodMatcher;

/**
 * Convenient abstract superclass for static method matchers, which don't care
 * about arguments at runtime.
 *
 * @author Rod Johnson
 */
// 方便的静态方法匹配器抽象超类，它不关心运行时的参数，为静态方法提供了模板，isRuntime() = false
public abstract class StaticMethodMatcher implements MethodMatcher {

	// 这个方法本身已经存在了，它并不是一个运行时的方法
	@Override
	public final boolean isRuntime() {
		return false;
	}

	// 如果 isRuntime() 时 true,那么match()方法就会去判断 args，那么这个方法就需要具体实现.
	// 如果是false,match() 方法就不支持，需要抛出异常。两个方法
	// 是二选一操作
	@Override
	public final boolean matches(Method method, Class<?> targetClass, Object... args) {
		// should never be invoked because isRuntime() returns false
		throw new UnsupportedOperationException("Illegal MethodMatcher usage");
	}

}
