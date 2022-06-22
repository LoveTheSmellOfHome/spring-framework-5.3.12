/*
 * Copyright 2002-2015 the original author or authors.
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

import org.aopalliance.intercept.MethodInvocation;

import org.springframework.lang.Nullable;

/**
 * Extension of the AOP Alliance {@link org.aopalliance.intercept.MethodInvocation}
 * interface, allowing access to the proxy that the method invocation was made through.
 *
 * <p>Useful to be able to substitute return values with the proxy,
 * if necessary, for example if the invocation target returned itself.
 *
 * @author Juergen Hoeller
 * @author Adrian Colyer
 * @since 1.1.3
 * @see org.springframework.aop.framework.ReflectiveMethodInvocation
 * @see org.springframework.aop.support.DelegatingIntroductionInterceptor
 */
// AOP Alliance MethodInvocation 接口的扩展，允许访问通过该方法调用的代理。
// 如有必要，例如如果调用目标返回自身，则能够用代理替换返回值很有用
public interface ProxyMethodInvocation extends MethodInvocation {

	/**
	 * Return the proxy that this method invocation was made through.
	 * @return the original proxy object
	 */
	// 返回此方法调用所通过的代理。
	// 返回值：原始代理对象
	Object getProxy();

	/**
	 * Create a clone of this object. If cloning is done before {@code proceed()}
	 * is invoked on this object, {@code proceed()} can be invoked once per clone
	 * to invoke the joinpoint (and the rest of the advice chain) more than once.
	 * @return an invocable clone of this invocation.
	 * {@code proceed()} can be called once per clone.
	 */
	// 创建此对象的克隆。 如果在此对象上调用proceed()之前完成克隆，则可以在每个克隆中调用一次proceed()
	// 以多次调用连接点（和建议链的其余部分）。
	//
	// 返回值：此调用的可调用克隆。 每个克隆都可以调用一次 proceed()
	MethodInvocation invocableClone();

	/**
	 * Create a clone of this object. If cloning is done before {@code proceed()}
	 * is invoked on this object, {@code proceed()} can be invoked once per clone
	 * to invoke the joinpoint (and the rest of the advice chain) more than once.
	 * @param arguments the arguments that the cloned invocation is supposed to use,
	 * overriding the original arguments
	 * @return an invocable clone of this invocation.
	 * {@code proceed()} can be called once per clone.
	 */
	// 创建此对象的克隆。 如果在此对象上调用proceed()之前完成克隆，则可以在每个克隆中调用一次proceed()以多次调用连接点（和建议链的其余部分）。
	// 参形：
	//			arguments - 克隆调用应该使用的参数，覆盖原始参数
	// 返回值：
	//			此调用的可调用克隆。 每个克隆都可以调用一次proceed()
	MethodInvocation invocableClone(Object... arguments);

	/**
	 * Set the arguments to be used on subsequent invocations in the any advice
	 * in this chain.
	 * @param arguments the argument array
	 */
	// 在此链中的任何建议中设置要在后续调用中使用的参数。
	// 参形：arguments - 参数数组
	void setArguments(Object... arguments);

	/**
	 * Add the specified user attribute with the given value to this invocation.
	 * <p>Such attributes are not used within the AOP framework itself. They are
	 * just kept as part of the invocation object, for use in special interceptors.
	 * @param key the name of the attribute
	 * @param value the value of the attribute, or {@code null} to reset it
	 */
	// 将具有给定值的指定用户属性添加到此调用。
	// AOP 框架本身不使用这些属性。 它们只是作为调用对象的一部分保存，用于特殊的拦截器。
	// 参形：
	//			key – 属性的名称
	//			value - 属性的值，或null重置它
	void setUserAttribute(String key, @Nullable Object value);

	/**
	 * Return the value of the specified user attribute.
	 * @param key the name of the attribute
	 * @return the value of the attribute, or {@code null} if not set
	 * @see #setUserAttribute
	 */
	// 返回指定用户属性的值。
	// 形参：
	//			key – 属性的名称
	// 返回值：
	//			属性的值，如果未设置，则为null
	@Nullable
	Object getUserAttribute(String key);

}
