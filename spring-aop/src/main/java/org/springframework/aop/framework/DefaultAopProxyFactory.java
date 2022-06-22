/*
 * Copyright 2002-2021 the original author or authors.
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

import java.io.Serializable;
import java.lang.reflect.Proxy;

import org.springframework.aop.SpringProxy;
import org.springframework.core.NativeDetector;

/**
 * Default {@link AopProxyFactory} implementation, creating either a CGLIB proxy
 * or a JDK dynamic proxy.
 *
 * <p>Creates a CGLIB proxy if one the following is true for a given
 * {@link AdvisedSupport} instance:
 * <ul>
 * <li>the {@code optimize} flag is set
 * <li>the {@code proxyTargetClass} flag is set
 * <li>no proxy interfaces have been specified
 * </ul>
 *
 * <p>In general, specify {@code proxyTargetClass} to enforce a CGLIB proxy,
 * or specify one or more interfaces to use a JDK dynamic proxy.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Sebastien Deleuze
 * @since 12.03.2004
 * @see AdvisedSupport#setOptimize
 * @see AdvisedSupport#setProxyTargetClass
 * @see AdvisedSupport#setInterfaces
 */
// 默认 AopProxyFactory 实现，创建 CGLIB 代理或 JDK 动态代理。
// 如果对于给定的 AdvisedSupport 实例满足以下任一条件，则创建 CGLIB 代理：
//  1.optimize 标志已设置
//  2.设置了 proxyTargetClass 标志
//  3.没有指定代理接口
// 通常，指定 proxyTargetClass 以强制执行 CGLIB 代理，或指定一个或多个接口以使用 JDK 动态代理。
@SuppressWarnings("serial")
public class DefaultAopProxyFactory implements AopProxyFactory, Serializable {


	// 策略模式：根据配置 AdvisedSupport 选择 JdkDynamicAopProxy 或者 CglibAopProxy
	@Override
	public AopProxy createAopProxy(AdvisedSupport config) throws AopConfigException {
		// 是否经过了优化，是不是一个基于类的代理(CGLIB),并且没有用户提供的接口
		if (!NativeDetector.inNativeImage() &&
				(config.isOptimize() || config.isProxyTargetClass() || hasNoUserSuppliedProxyInterfaces(config))) {
			// 通过配置找到被代理的目标对象
			Class<?> targetClass = config.getTargetClass();
			if (targetClass == null) {
				throw new AopConfigException("TargetSource cannot determine target class: " +
						"Either an interface or a target is required for proxy creation.");
			}
			// 如果是接口或者 JDK 动态代理对象，采用 JDK 动态代理的方式
			if (targetClass.isInterface() || Proxy.isProxyClass(targetClass)) {
				return new JdkDynamicAopProxy(config);
			}
			// DefaultAopProxyFactory 是一个原型对象，一个 AdvisedSupport 配置创建一个 AopProxy 对象
			return new ObjenesisCglibAopProxy(config);
		}
		else {
			// DefaultAopProxyFactory 是一个原型对象，一个 AdvisedSupport 配置创建一个 AopProxy 对象
			return new JdkDynamicAopProxy(config);
		}
	}

	/**
	 * Determine whether the supplied {@link AdvisedSupport} has only the
	 * {@link org.springframework.aop.SpringProxy} interface specified
	 * (or no proxy interfaces specified at all).
	 */
	// 确定提供的 AdvisedSupport 是否只指定了 SpringProxy 接口（或根本没有指定代理接口）
	private boolean hasNoUserSuppliedProxyInterfaces(AdvisedSupport config) {
		Class<?>[] ifcs = config.getProxiedInterfaces();
		return (ifcs.length == 0 || (ifcs.length == 1 && SpringProxy.class.isAssignableFrom(ifcs[0])));
	}

}
