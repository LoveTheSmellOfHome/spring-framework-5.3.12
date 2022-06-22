/*
 * Copyright 2002-2019 the original author or authors.
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

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import org.springframework.aop.ProxyMethodInvocation;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.BridgeMethodResolver;
import org.springframework.lang.Nullable;

/**
 * Spring's implementation of the AOP Alliance
 * {@link org.aopalliance.intercept.MethodInvocation} interface,
 * implementing the extended
 * {@link org.springframework.aop.ProxyMethodInvocation} interface.
 *
 * <p>Invokes the target object using reflection. Subclasses can override the
 * {@link #invokeJoinpoint()} method to change this behavior, so this is also
 * a useful base class for more specialized MethodInvocation implementations.
 *
 * <p>It is possible to clone an invocation, to invoke {@link #proceed()}
 * repeatedly (once per clone), using the {@link #invocableClone()} method.
 * It is also possible to attach custom attributes to the invocation,
 * using the {@link #setUserAttribute} / {@link #getUserAttribute} methods.
 *
 * <p><b>NOTE:</b> This class is considered internal and should not be
 * directly accessed. The sole reason for it being public is compatibility
 * with existing framework integrations (e.g. Pitchfork). For any other
 * purposes, use the {@link ProxyMethodInvocation} interface instead.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Adrian Colyer
 * @see #invokeJoinpoint
 * @see #proceed
 * @see #invocableClone
 * @see #setUserAttribute
 * @see #getUserAttribute
 */
// Spring对AOP联盟MethodInvocation接口的实现，实现了扩展的ProxyMethodInvocation接口。
//
// 使用反射调用目标对象。 子类可以重写 invokeJoinpoint() 方法来改变这种行为，所以这对于
// 更专业的 MethodInvocation 实现来说也是一个有用的基类。
//
// 可以使用 invocableClone() 方法克隆一个调用，重复调用 proceed() （每个克隆一次invocableClone() 。
// 也可以使用 setUserAttribute / getUserAttribute 方法将自定义属性附加到调用。
//
// 注意：此类被认为是内部的，不应直接访问。 它公开的唯一原因是与现有框架集成（例如 Pitchfork）的兼容性。
// 对于任何其他目的，请改用ProxyMethodInvocation接口。
public class ReflectiveMethodInvocation implements ProxyMethodInvocation, Cloneable {

	// 代理对象
	protected final Object proxy;

	// 调用方法的目标对象
	@Nullable
	protected final Object target;

	// 调用的目标方法
	protected final Method method;

	// 方法参数
	protected Object[] arguments;

	// 目标 Class
	@Nullable
	private final Class<?> targetClass;

	/**
	 * Lazily initialized map of user-specific attributes for this invocation.
	 */
	// 此调用的用户特定属性的延迟初始化映射
	@Nullable
	private Map<String, Object> userAttributes;

	/**
	 * List of MethodInterceptor and InterceptorAndDynamicMethodMatcher
	 * that need dynamic checks.
	 */
	// 需要动态检查的 MethodInterceptor 和 InterceptorAndDynamicMethodMatcher 列表，责任链链表
	protected final List<?> interceptorsAndDynamicMethodMatchers;

	/**
	 * Index from 0 of the current interceptor we're invoking.
	 * -1 until we invoke: then the current interceptor.
	 */
	// 从我们正在调用的当前拦截器的 0 开始的索引。 -1 直到我们调用：然后是当前的拦截器
	// 当前游标的起始位置
	private int currentInterceptorIndex = -1;


	/**
	 * Construct a new ReflectiveMethodInvocation with the given arguments.
	 * @param proxy the proxy object that the invocation was made on
	 * @param target the target object to invoke
	 * @param method the method to invoke
	 * @param arguments the arguments to invoke the method with
	 * @param targetClass the target class, for MethodMatcher invocations
	 * @param interceptorsAndDynamicMethodMatchers interceptors that should be applied,
	 * along with any InterceptorAndDynamicMethodMatchers that need evaluation at runtime.
	 * MethodMatchers included in this struct must already have been found to have matched
	 * as far as was possibly statically. Passing an array might be about 10% faster,
	 * but would complicate the code. And it would work only for static pointcuts.
	 */
	// 使用给定的参数构造一个新的 ReflectiveMethodInvocation。
	// 参形：
	//			proxy – 进行调用的代理对象
	//			target - 要调用的目标对象
	//			method - 调用的方法
	//			arguments - 调用方法的参数
	//			targetClass – 目标类，用于 MethodMatcher 调用
	//			interceptorsAndDynamicMethodMatchers – 应该应用的拦截器，以及任何需要在运行时
	//			评估的 InterceptorAndDynamicMethodMatchers。 必须已经发现此结构中包含的 MethodMatchers
	//			已尽可能静态匹配。 传递一个数组可能会快 10%，但会使代码复杂化。 它只适用于静态切入点
	protected ReflectiveMethodInvocation(
			Object proxy, @Nullable Object target, Method method, @Nullable Object[] arguments,
			@Nullable Class<?> targetClass, List<Object> interceptorsAndDynamicMethodMatchers) {

		this.proxy = proxy;
		this.target = target;
		this.targetClass = targetClass;
		this.method = BridgeMethodResolver.findBridgedMethod(method);
		this.arguments = AopProxyUtils.adaptArgumentsIfNecessary(method, arguments);
		this.interceptorsAndDynamicMethodMatchers = interceptorsAndDynamicMethodMatchers; // 拦截器链
	}


	// 返回代理
	@Override
	public final Object getProxy() {
		return this.proxy;
	}

	// 返回调用方法的目标对象
	@Override
	@Nullable
	public final Object getThis() {
		return this.target;
	}

	@Override
	public final AccessibleObject getStaticPart() {
		return this.method;
	}

	/**
	 * Return the method invoked on the proxied interface.
	 * May or may not correspond with a method invoked on an underlying
	 * implementation of that interface.
	 */
	// 返回在代理接口上调用的方法。 可能与在该接口的底层实现上调用的方法对应，也可能不对应
	@Override
	public final Method getMethod() {
		return this.method;
	}

	@Override
	public final Object[] getArguments() {
		return this.arguments;
	}

	@Override
	public void setArguments(Object... arguments) {
		this.arguments = arguments;
	}


	// 责任链实现模式：采用游标的方式来递归调用
	@Override
	@Nullable
	public Object proceed() throws Throwable {
		// We start with an index of -1 and increment early.
		// 我们从 -1 的索引开始并提前递增，当前游标会滚动，从 -1 开始
		// 首先，分析当游标达到了责任链链表末端时候或者责任链链表为空，没有任何拦截器，直接调用目标方法
		if (this.currentInterceptorIndex == this.interceptorsAndDynamicMethodMatchers.size() - 1) {
			// 调用目标方法
			return invokeJoinpoint();
		}

		// 从第一个拦截器开始
		Object interceptorOrInterceptionAdvice =
				this.interceptorsAndDynamicMethodMatchers.get(++this.currentInterceptorIndex);
		// 如果条件匹配走这段逻辑(少数成立)
		if (interceptorOrInterceptionAdvice instanceof InterceptorAndDynamicMethodMatcher) {
			// Evaluate dynamic method matcher here: static part will already have
			// been evaluated and found to match.
			// 在这里评估动态方法匹配器：静态部分已经被评估并发现匹配
			InterceptorAndDynamicMethodMatcher dm =
					(InterceptorAndDynamicMethodMatcher) interceptorOrInterceptionAdvice;
			Class<?> targetClass = (this.targetClass != null ? this.targetClass : this.method.getDeclaringClass());
			if (dm.methodMatcher.matches(this.method, targetClass, this.arguments)) {
				return dm.interceptor.invoke(this);
			}
			else {
				// Dynamic matching failed.
				// Skip this interceptor and invoke the next in the chain.
				// 动态匹配失败。跳过这个拦截器并调用链中的下一个
				return proceed();
			}
		}
		else { // 大多直接走这段逻辑
			// It's an interceptor, so we just invoke it: The pointcut will have
			// been evaluated statically before this object was constructed.
			// 它是一个 AOP 拦截器，所以我们只是调用它：在构造这个对象之前，切入点将被静态评估。
			return ((MethodInterceptor) interceptorOrInterceptionAdvice).invoke(this);
		}
	}

	/**
	 * Invoke the joinpoint using reflection.
	 * Subclasses can override this to use custom invocation.
	 * @return the return value of the joinpoint
	 * @throws Throwable if invoking the joinpoint resulted in an exception
	 */
	// 使用反射调用连接点。 子类可以覆盖它以使用自定义调用。
	// 返回值：
	//			连接点的返回值
	// 抛出：
	//			Throwable – 如果调用连接点导致异常
	// 调用匹配的目标方法
	@Nullable
	protected Object invokeJoinpoint() throws Throwable {
		return AopUtils.invokeJoinpointUsingReflection(this.target, this.method, this.arguments);
	}


	/**
	 * This implementation returns a shallow copy of this invocation object,
	 * including an independent copy of the original arguments array.
	 * <p>We want a shallow copy in this case: We want to use the same interceptor
	 * chain and other object references, but we want an independent value for the
	 * current interceptor index.
	 * @see java.lang.Object#clone()
	 */
	// 此实现返回此调用对象的浅表副本，包括原始参数数组的独立副本。
	// 在这种情况下，我们想要一个浅拷贝：我们想要使用相同的拦截器链和其他对象引用，但我们想要当前拦截器索引的独立值。
	@Override
	public MethodInvocation invocableClone() {
		Object[] cloneArguments = this.arguments;
		if (this.arguments.length > 0) {
			// Build an independent copy of the arguments array.
			// 构建参数数组的独立副本。
			cloneArguments = this.arguments.clone();
		}
		return invocableClone(cloneArguments);
	}

	/**
	 * This implementation returns a shallow copy of this invocation object,
	 * using the given arguments array for the clone.
	 * <p>We want a shallow copy in this case: We want to use the same interceptor
	 * chain and other object references, but we want an independent value for the
	 * current interceptor index.
	 * @see java.lang.Object#clone()
	 */
	// 此实现返回此调用对象的浅表副本，使用克隆的给定参数数组。
	// 在这种情况下，我们想要一个浅拷贝：我们想要使用相同的拦截器链和其他对象引用，但我们想要当前拦截器索引的独立值
	@Override
	public MethodInvocation invocableClone(Object... arguments) {
		// Force initialization of the user attributes Map,
		// for having a shared Map reference in the clone.
		// 强制初始化用户属性 Map，以便在克隆中具有共享的 Map 引用
		if (this.userAttributes == null) {
			this.userAttributes = new HashMap<>();
		}

		// Create the MethodInvocation clone.
		// 创建 MethodInvocation 克隆
		try {
			ReflectiveMethodInvocation clone = (ReflectiveMethodInvocation) clone();
			clone.arguments = arguments;
			return clone;
		}
		catch (CloneNotSupportedException ex) {
			throw new IllegalStateException(
					"Should be able to clone object of type [" + getClass() + "]: " + ex);
		}
	}


	@Override
	public void setUserAttribute(String key, @Nullable Object value) {
		if (value != null) {
			if (this.userAttributes == null) {
				this.userAttributes = new HashMap<>();
			}
			this.userAttributes.put(key, value);
		}
		else {
			if (this.userAttributes != null) {
				this.userAttributes.remove(key);
			}
		}
	}

	@Override
	@Nullable
	public Object getUserAttribute(String key) {
		return (this.userAttributes != null ? this.userAttributes.get(key) : null);
	}

	/**
	 * Return user attributes associated with this invocation.
	 * This method provides an invocation-bound alternative to a ThreadLocal.
	 * <p>This map is initialized lazily and is not used in the AOP framework itself.
	 * @return any user attributes associated with this invocation
	 * (never {@code null})
	 */
	// 返回与此调用关联的用户属性。 此方法提供了 ThreadLocal 的调用绑定替代方案。
	// 此映射是延迟初始化的，在 AOP 框架本身中不使用。
	// 返回值：
	//			与此调用关联的任何用户属性（从不为null ）
	public Map<String, Object> getUserAttributes() {
		if (this.userAttributes == null) {
			this.userAttributes = new HashMap<>();
		}
		return this.userAttributes;
	}


	@Override
	public String toString() {
		// Don't do toString on target, it may be proxied.
		// 不要在目标上执行 toString，它可能会被代理
		StringBuilder sb = new StringBuilder("ReflectiveMethodInvocation: ");
		sb.append(this.method).append("; ");
		if (this.target == null) {
			sb.append("target is null");
		}
		else {
			sb.append("target is of class [").append(this.target.getClass().getName()).append(']');
		}
		return sb.toString();
	}

}
