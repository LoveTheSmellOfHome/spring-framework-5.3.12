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

package org.springframework.aop.support;

import org.aopalliance.intercept.MethodInvocation;

import org.springframework.aop.DynamicIntroductionAdvice;
import org.springframework.aop.IntroductionInterceptor;
import org.springframework.aop.ProxyMethodInvocation;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Convenient implementation of the
 * {@link org.springframework.aop.IntroductionInterceptor} interface.
 *
 * <p>Subclasses merely need to extend this class and implement the interfaces
 * to be introduced themselves. In this case the delegate is the subclass
 * instance itself. Alternatively a separate delegate may implement the
 * interface, and be set via the delegate bean property.
 *
 * <p>Delegates or subclasses may implement any number of interfaces.
 * All interfaces except IntroductionInterceptor are picked up from
 * the subclass or delegate by default.
 *
 * <p>The {@code suppressInterface} method can be used to suppress interfaces
 * implemented by the delegate but which should not be introduced to the owning
 * AOP proxy.
 *
 * <p>An instance of this class is serializable if the delegate is.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 16.11.2003
 * @see #suppressInterface
 * @see DelegatePerTargetObjectIntroductionInterceptor
 */
// IntroductionInterceptor接口的便捷实现。
// 子类只需要扩展这个类并实现自己要引入的接口。在这种情况下，委托是子类实例本身。或者，单独的委托可以实现接口，
// 并通过委托 bean 属性进行设置。
//
// 委托或子类可以实现任意数量的接口。默认情况下，除 IntroductionInterceptor 之外的所有接口都是从子类或委托中提取的。
//
// suppressInterface方法可用于抑制由委托实现的接口，但不应将其引入拥有的 AOP 代理。
//
// 如果委托是可序列化的，则此类的实例是可序列化的
@SuppressWarnings("serial")
public class DelegatingIntroductionInterceptor extends IntroductionInfoSupport
		implements IntroductionInterceptor {

	/**
	 * Object that actually implements the interfaces.
	 * May be "this" if a subclass implements the introduced interfaces.
	 */
	// 实际实现接口的对象。如果子类实现了引入的接口，则可能是“this”。
	@Nullable
	private Object delegate;


	/**
	 * Construct a new DelegatingIntroductionInterceptor, providing
	 * a delegate that implements the interfaces to be introduced.
	 * @param delegate the delegate that implements the introduced interfaces
	 */
	// 构造一个新的 DelegatingIntroductionInterceptor，提供一个实现要引入的接口的委托。
	// 参形：delegate - 实现引入接口的委托
	public DelegatingIntroductionInterceptor(Object delegate) {
		init(delegate);
	}

	/**
	 * Construct a new DelegatingIntroductionInterceptor.
	 * The delegate will be the subclass, which must implement
	 * additional interfaces.
	 */
	// 构造一个新的 DelegatingIntroductionInterceptor。委托将是子类，它必须实现额外的接口。
	protected DelegatingIntroductionInterceptor() {
		init(this);
	}


	/**
	 * Both constructors use this init method, as it is impossible to pass
	 * a "this" reference from one constructor to another.
	 * @param delegate the delegate object
	 */
	// 两个构造函数都使用这个 init 方法，因为不可能将“this”引用从一个构造函数传递给另一个。
	// 参形：delegate - 委托对象
	private void init(Object delegate) {
		Assert.notNull(delegate, "Delegate must not be null");
		this.delegate = delegate;
		implementInterfacesOnObject(delegate);

		// We don't want to expose the control interface
		// 我们不想暴露控制接口
		suppressInterface(IntroductionInterceptor.class);
		suppressInterface(DynamicIntroductionAdvice.class);
	}


	/**
	 * Subclasses may need to override this if they want to perform custom
	 * behaviour in around advice. However, subclasses should invoke this
	 * method, which handles introduced interfaces and forwarding to the target.
	 */
	// 如果子类想要在周围建议中执行自定义行为，则可能需要覆盖它。但是，子类应该调用此方法，该方法处理引入的接口并转发到目标。
	@Override
	@Nullable
	public Object invoke(MethodInvocation mi) throws Throwable {
		if (isMethodOnIntroducedInterface(mi)) {
			// Using the following method rather than direct reflection, we
			// get correct handling of InvocationTargetException
			// if the introduced method throws an exception.
			// 使用以下方法而不是直接反射，如果引入的方法抛出异常，我们可以正确处理 InvocationTargetException。
			Object retVal = AopUtils.invokeJoinpointUsingReflection(this.delegate, mi.getMethod(), mi.getArguments());

			// Massage return value if possible: if the delegate returned itself,
			// we really want to return the proxy.
			// 如果可能的话，消息返回值：如果委托返回自己，我们真的想返回代理。
			if (retVal == this.delegate && mi instanceof ProxyMethodInvocation) {
				Object proxy = ((ProxyMethodInvocation) mi).getProxy();
				if (mi.getMethod().getReturnType().isInstance(proxy)) {
					retVal = proxy;
				}
			}
			return retVal;
		}

		return doProceed(mi);
	}

	/**
	 * Proceed with the supplied {@link org.aopalliance.intercept.MethodInterceptor}.
	 * Subclasses can override this method to intercept method invocations on the
	 * target object which is useful when an introduction needs to monitor the object
	 * that it is introduced into. This method is <strong>never</strong> called for
	 * {@link MethodInvocation MethodInvocations} on the introduced interfaces.
	 */
	// 继续使用提供的 org.aopalliance.intercept.MethodInterceptor 。
	// 子类可以重写此方法以拦截目标对象上的方法调用，
	// 这在引入需要监视它被引入的对象时很有用。在引入的接口上，永远不会为 MethodInvocations 调用此方法。
	@Nullable
	protected Object doProceed(MethodInvocation mi) throws Throwable {
		// If we get here, just pass the invocation on.
		// 如果我们到达这里，只需传递调用
		return mi.proceed();
	}

}
