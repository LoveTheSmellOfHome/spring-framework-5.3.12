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

package org.springframework.aop.framework;

import org.springframework.lang.Nullable;

/**
 * Delegate interface for a configured AOP proxy, allowing for the creation
 * of actual proxy objects.
 *
 * <p>Out-of-the-box implementations are available for JDK dynamic proxies
 * and for CGLIB proxies, as applied by {@link DefaultAopProxyFactory}.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see DefaultAopProxyFactory
 */
// 配置的 AOP 代理的委托接口，允许创建实际的代理对象。
// 开箱即用的实现可用于 JDK 动态代理和 CGLIB 代理，由 DefaultAopProxyFactory 应用。
public interface AopProxy {

	/**
	 * Create a new proxy object.
	 * <p>Uses the AopProxy's default class loader (if necessary for proxy creation):
	 * usually, the thread context class loader.
	 * @return the new proxy object (never {@code null})
	 * @see Thread#getContextClassLoader()
	 */
	// 创建一个新的代理对象。
	// 使用 AopProxy 的默认类加载器（如果需要创建代理）：通常是线程上下文类加载器。
	// 返回值：
	//			新的代理对象（从不为null ）
	Object getProxy();

	/**
	 * Create a new proxy object.
	 * <p>Uses the given class loader (if necessary for proxy creation).
	 * {@code null} will simply be passed down and thus lead to the low-level
	 * proxy facility's default, which is usually different from the default chosen
	 * by the AopProxy implementation's {@link #getProxy()} method.
	 * @param classLoader the class loader to create the proxy with
	 * (or {@code null} for the low-level proxy facility's default)
	 * @return the new proxy object (never {@code null})
	 */
	// 创建一个新的代理对象。
	// 使用给定的类加载器（如果需要创建代理）。 null将简单地向下传递，从而导致低级代理工具的默认值，
	// 这通常与 AopProxy 实现的getProxy()方法选择的默认值不同。
	// 形参：
	//			classLoader – 用于创建代理的类加载器（或为低级代理工具的默认设置为null ）
	// 返回值：
	//			新的代理对象（从不为null ）
	Object getProxy(@Nullable ClassLoader classLoader);

}
