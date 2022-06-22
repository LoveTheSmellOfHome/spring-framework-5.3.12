/*
 * Copyright 2002-2016 the original author or authors.
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

import org.aopalliance.intercept.Interceptor;

import org.springframework.aop.TargetSource;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;

/**
 * Factory for AOP proxies for programmatic use, rather than via declarative
 * setup in a bean factory. This class provides a simple way of obtaining
 * and configuring AOP proxy instances in custom user code.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @since 14.03.2003
 */
// 用于编程使用的 AOP 代理工厂，而不是通过 bean 工厂中的声明性设置。
// 此类提供了一种在自定义用户代码中获取和配置 AOP 代理实例的简单方法
//
// ProxyFactoryBean 和 ProxyFactory 的实现大同小异，前者更适合于 IoC 容器，后者则可以脱离 IoC 容器来使用
//
// 适用于普通 API 实现
//
// 工厂方法模式（Factory method）实现：
// 就像其他创建型模式一样，它也是处理在不指定对象具体类型的情况下创建对象的问题。工厂方法模式 的实质是
// “定义一个创建对象的接口，但让实现这个接口的类来决定实例化哪个类。工厂方法让类的实
// 例化推迟到子类中进行。”
//
// 分为静态工厂方法(对象工厂方法)，和动态工厂方法(类工厂方法)
@SuppressWarnings("serial")
public class ProxyFactory extends ProxyCreatorSupport {

	/**
	 * Create a new ProxyFactory.
	 */
	// 创建一个新的代理工厂
	public ProxyFactory() {
	}

	/**
	 * Create a new ProxyFactory.
	 * <p>Will proxy all interfaces that the given target implements.
	 * @param target the target object to be proxied
	 */
	// 创建一个新的代理工厂。
	// 将代理给定目标实现的所有接口。
	// 形参：
	//			target -- 要代理的目标对象
	public ProxyFactory(Object target) {
		// 设置目标对象
		setTarget(target);
		// 设置接口：这里会获取目标对象的所有接口，无法通过指定 IntroductionInfo 中代理接口来减少代理的接口。
		setInterfaces(ClassUtils.getAllInterfaces(target));
	}

	/**
	 * Create a new ProxyFactory.
	 * <p>No target, only interfaces. Must add interceptors.
	 * @param proxyInterfaces the interfaces that the proxy should implement
	 */
	// 创建一个新的代理工厂。
	// 没有目标，只有接口。 必须添加拦截器。
	// 形参：
	//			proxyInterfaces – 代理应该实现的接口
	public ProxyFactory(Class<?>... proxyInterfaces) {
		setInterfaces(proxyInterfaces);
	}

	/**
	 * Create a new ProxyFactory for the given interface and interceptor.
	 * <p>Convenience method for creating a proxy for a single interceptor,
	 * assuming that the interceptor handles all calls itself rather than
	 * delegating to a target, like in the case of remoting proxies.
	 * @param proxyInterface the interface that the proxy should implement
	 * @param interceptor the interceptor that the proxy should invoke
	 */
	// 为给定的接口和拦截器创建一个新的 ProxyFactory。
	// 为单个拦截器创建代理的便捷方法，假设拦截器自己处理所有调用而不是委托给目标，就像远程代理的情况一样。
	// 形参：
	// 			proxyInterface – 代理应该实现的接口
	//			interceptor - 代理应该调用的拦截器，关联 Advice
	public ProxyFactory(Class<?> proxyInterface, Interceptor interceptor) {
		addInterface(proxyInterface);
		addAdvice(interceptor);
	}

	/**
	 * Create a ProxyFactory for the specified {@code TargetSource},
	 * making the proxy implement the specified interface.
	 * @param proxyInterface the interface that the proxy should implement
	 * @param targetSource the TargetSource that the proxy should invoke
	 */
	// 为指定的 TargetSource 创建一个 ProxyFactory ，使代理实现指定的接口。
	// 形参：
	// 			proxyInterface – 代理应该实现的接口
	//			targetSource – 代理应调用的 TargetSource
	public ProxyFactory(Class<?> proxyInterface, TargetSource targetSource) {
		addInterface(proxyInterface);
		setTargetSource(targetSource);
	}


	/**
	 * Create a new proxy according to the settings in this factory.
	 * <p>Can be called repeatedly. Effect will vary if we've added
	 * or removed interfaces. Can add and remove interceptors.
	 * <p>Uses a default class loader: Usually, the thread context class loader
	 * (if necessary for proxy creation).
	 * @return the proxy object
	 */
	// 根据这个工厂的设置创建一个新的代理。
	// 可以反复调用。 如果我们添加或删除接口，效果会有所不同。 可以添加和删除拦截器。
	// 使用默认类加载器：通常是线程上下文类加载器（如果需要创建代理）。
	// 返回值：
	//				代理对象
	// 并不是所有的工厂方法都是以 create/new/build 开头的，也可以通过 get 来开头
	public Object getProxy() {
		// createAopProxy() 会触发 AdvisedSupportListener#activated(AdvisedSupport advised) 事件
		// 调用标准实现
		// 底层实现委派给 createAopProxy().getProxy() 来实现的，所有实现都是抽象的。具体实现取决于 createAopProxy() 是怎么实现的
		// 它里边即包含了动态工厂方法又包含了静态工厂方法
		return createAopProxy().getProxy();
	}

	/**
	 * Create a new proxy according to the settings in this factory.
	 * <p>Can be called repeatedly. Effect will vary if we've added
	 * or removed interfaces. Can add and remove interceptors.
	 * <p>Uses the given class loader (if necessary for proxy creation).
	 * @param classLoader the class loader to create the proxy with
	 * (or {@code null} for the low-level proxy facility's default)
	 * @return the proxy object
	 */
	// 根据这个工厂的设置创建一个新的代理。
	// 可以反复调用。 如果我们添加或删除接口，效果会有所不同。 可以添加和删除拦截器。
	// 使用给定的类加载器（如果需要创建代理）。
	// 形参：
	//			classLoader – 用于创建代理的类加载器（或null用于低级代理工具的默认值）
	// 返回值：
	//			代理对象
	public Object getProxy(@Nullable ClassLoader classLoader) {
		// 调用标准实现
		return createAopProxy().getProxy(classLoader);
	}


	/**
	 * Create a new proxy for the given interface and interceptor.
	 * <p>Convenience method for creating a proxy for a single interceptor,
	 * assuming that the interceptor handles all calls itself rather than
	 * delegating to a target, like in the case of remoting proxies.
	 * @param proxyInterface the interface that the proxy should implement
	 * @param interceptor the interceptor that the proxy should invoke
	 * @return the proxy object
	 * @see #ProxyFactory(Class, org.aopalliance.intercept.Interceptor)
	 */
	// 为给定的接口和拦截器创建一个新代理。
	// 为单个拦截器创建代理的便捷方法，假设拦截器自己处理所有调用而不是委托给目标，就像远程代理的情况一样。
	// 形参：
	//			proxyInterface – 代理应该实现的接口
	// 拦截器——代理应该调用的拦截器
	// 返回值：
	//			代理对象
	@SuppressWarnings("unchecked")
	public static <T> T getProxy(Class<T> proxyInterface, Interceptor interceptor) {
		return (T) new ProxyFactory(proxyInterface, interceptor).getProxy();
	}

	/**
	 * Create a proxy for the specified {@code TargetSource},
	 * implementing the specified interface.
	 * @param proxyInterface the interface that the proxy should implement
	 * @param targetSource the TargetSource that the proxy should invoke
	 * @return the proxy object
	 * @see #ProxyFactory(Class, org.springframework.aop.TargetSource)
	 */
	// 为指定的 TargetSource 创建一个代理，实现指定的接口。
	// 形参：
	//			proxyInterface – 代理应该实现的接口
	//			targetSource – 代理应调用的 TargetSource
	// 返回值：
	//			代理对象
	@SuppressWarnings("unchecked")
	public static <T> T getProxy(Class<T> proxyInterface, TargetSource targetSource) {
		return (T) new ProxyFactory(proxyInterface, targetSource).getProxy();
	}

	/**
	 * Create a proxy for the specified {@code TargetSource} that extends
	 * the target class of the {@code TargetSource}.
	 * @param targetSource the TargetSource that the proxy should invoke
	 * @return the proxy object
	 */
	// 创建指定的代理TargetSource扩展目标类的TargetSource 。
	// 形参：
	//			targetSource – 代理应调用的 TargetSource
	// 返回值：
	//			代理对象
	public static Object getProxy(TargetSource targetSource) {
		if (targetSource.getTargetClass() == null) {
			throw new IllegalArgumentException("Cannot create class proxy for TargetSource with null target class");
		}
		ProxyFactory proxyFactory = new ProxyFactory();
		proxyFactory.setTargetSource(targetSource);
		proxyFactory.setProxyTargetClass(true);
		return proxyFactory.getProxy();
	}

}
