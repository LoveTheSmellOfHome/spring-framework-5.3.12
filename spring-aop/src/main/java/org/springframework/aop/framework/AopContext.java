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

package org.springframework.aop.framework;

import org.springframework.core.NamedThreadLocal;
import org.springframework.lang.Nullable;

/**
 * Class containing static methods used to obtain information about the current AOP invocation.
 *
 * <p>The {@code currentProxy()} method is usable if the AOP framework is configured to
 * expose the current proxy (not the default). It returns the AOP proxy in use. Target objects
 * or advice can use this to make advised calls, in the same way as {@code getEJBObject()}
 * can be used in EJBs. They can also use it to find advice configuration.
 *
 * <p>Spring's AOP framework does not expose proxies by default, as there is a performance cost
 * in doing so.
 *
 * <p>The functionality in this class might be used by a target object that needed access
 * to resources on the invocation. However, this approach should not be used when there is
 * a reasonable alternative, as it makes application code dependent on usage under AOP and
 * the Spring AOP framework in particular.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 13.03.2003
 */
// 包含用于获取有关当前 AOP 调用信息的静态方法的类。
//
// 如果 AOP 框架配置为公开当前代理（不是默认代理）EnableAspectJAutoProxy#exposeProxy()，
// 则currentProxy()方法可用。它返回正在使用的 AOP 代理。
// 目标对象或建议可以使用它来进行建议调用，就像在 EJB 中使用getEJBObject()一样。他们还可以使用它来查找建议配置。
//
// Spring 的 AOP 框架默认不公开代理，因为这样做会降低性能。
//
// 此类中的功能可能由需要在调用时访问资源的目标对象使用。但是，如果有合理的替代方案，则不应使用此方法，
// 因为它使应用程序代码依赖于 AOP 下的使用，尤其是 Spring AOP 框架。
public final class AopContext {

	/**
	 * ThreadLocal holder for AOP proxy associated with this thread.
	 * Will contain {@code null} unless the "exposeProxy" property on
	 * the controlling proxy configuration has been set to "true".
	 * @see ProxyConfig#setExposeProxy
	 */
	// 与此线程关联的 AOP 代理的 ThreadLocal 持有者。除非控制代理配置上的“exposeProxy”属性设置为“true”，否则将包含null
	private static final ThreadLocal<Object> currentProxy = new NamedThreadLocal<>("Current AOP proxy");


	private AopContext() {
	}


	/**
	 * Try to return the current AOP proxy. This method is usable only if the
	 * calling method has been invoked via AOP, and the AOP framework has been set
	 * to expose proxies. Otherwise, this method will throw an IllegalStateException.
	 * @return the current AOP proxy (never returns {@code null})
	 * @throws IllegalStateException if the proxy cannot be found, because the
	 * method was invoked outside an AOP invocation context, or because the
	 * AOP framework has not been configured to expose the proxy
	 */
	// 尝试返回当前的 AOP 代理。仅当调用方法已通过 AOP 调用，并且 AOP 框架已设置为公开代理时，此方法才可用。否则，此方法将抛出 IllegalStateException。
	// 返回值：
	//			当前的 AOP 代理（从不返回null ）
	// 抛出：
	//			IllegalStateException – 如果找不到代理，因为该方法是在 AOP 调用上下文之外调用的，
	//			或者因为 AOP 框架尚未配置为公开代理
	public static Object currentProxy() throws IllegalStateException {
		Object proxy = currentProxy.get();
		if (proxy == null) {
			throw new IllegalStateException(
					"Cannot find current proxy: Set 'exposeProxy' property on Advised to 'true' to make it available, and " +
							"ensure that AopContext.currentProxy() is invoked in the same thread as the AOP invocation context.");
		}
		return proxy;
	}

	/**
	 * Make the given proxy available via the {@code currentProxy()} method.
	 * <p>Note that the caller should be careful to keep the old value as appropriate.
	 * @param proxy the proxy to expose (or {@code null} to reset it)
	 * @return the old proxy, which may be {@code null} if none was bound
	 * @see #currentProxy()
	 */
	// 通过currentProxy()方法使给定的代理可用。
	// 请注意，调用者应注意适当地保留旧值。
	// 参形：
	//			proxy – 要公开的代理（或null以重置它）
	// 返回值：
	//			旧代理，如果没有绑定，则可能为null
	// 有两重语义
	@Nullable
	static Object setCurrentProxy(@Nullable Object proxy) {
		Object old = currentProxy.get();
		if (proxy != null) {
			// 替换老的
			currentProxy.set(proxy);
		}
		else {
			// 移除老的
			currentProxy.remove();
		}
		return old;
	}

}
