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

package org.springframework.aop.framework;

import java.lang.reflect.Method;
import java.util.List;

import org.springframework.lang.Nullable;

/**
 * Factory interface for advisor chains.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
// 顾问链的工厂接口
// 责任链模式 + 抽象工厂方法
public interface AdvisorChainFactory {

	/**
	 * Determine a list of {@link org.aopalliance.intercept.MethodInterceptor} objects
	 * for the given advisor chain configuration.
	 * @param config the AOP configuration in the form of an Advised object
	 * @param method the proxied method
	 * @param targetClass the target class (may be {@code null} to indicate a proxy without
	 * target object, in which case the method's declaring class is the next best option)
	 * @return a List of MethodInterceptors (may also include InterceptorAndDynamicMethodMatchers)
	 */
	// 确定给定顾问链配置的org.aopalliance.intercept.MethodInterceptor对象列表。
	// 参形：
	//			config – Advised 对象形式的 AOP 配置
	// 			method - 代理方法
	// 			targetClass – 目标类（可以为null表示没有目标对象的代理，在这种情况下，方法的声明类是下一个最佳选择）
	// 返回值：
	//			MethodInterceptor 列表（可能还包括 InterceptorAndDynamicMethodMatchers）
	// 返回的是不同对象。
	List<Object> getInterceptorsAndDynamicInterceptionAdvice(Advised config, Method method, @Nullable Class<?> targetClass);

}
