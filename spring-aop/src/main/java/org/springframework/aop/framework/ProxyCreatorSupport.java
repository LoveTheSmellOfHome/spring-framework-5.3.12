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

import java.util.ArrayList;
import java.util.List;

import org.springframework.util.Assert;

/**
 * Base class for proxy factories.
 * Provides convenient access to a configurable AopProxyFactory.
 *
 * @author Juergen Hoeller
 * @since 2.0.3
 * @see #createAopProxy()
 */
// 代理工厂的基类：代理创建器的支持类。提供对可配置 AopProxyFactory 的便捷访问，提供了 3 种实现：
// 适用于普通 API 实现的 ProxyFactory，和 IoC 打通的 ProxyFactoryBean 以及
// 和 AspectJ 打通的 AspectJProxyFactory，分别对应着 Spring AOP 的 3 种不同实现的代理方式。
//
// 与 AdvisedSupport 的区别是：AdvisedSupport 在于装配或者关联 AOP 配置的一个很重要信息，除了
// ProxyConfig 之外，还包含了一些 Advisor 和一些接口的存储。而 ProxyCreatorSupport 更关注于
// AopProxy 对象的创建，所以它要关联一个 AopProxyFactory 这样一个实例，因此它提供了默认的 DefaultAopProxyFactory
// 以及用户自定义的。
//
// 观察者模式实现：
// 观察者 - org.springframework.aop.framework.ProxyCreatorSupport
// 被观察者 - org.springframework.aop.framework.AdvisedSupportListener
// 通知对象 - org.springframework.aop.framework.AdvisedSupport
@SuppressWarnings("serial")
public class ProxyCreatorSupport extends AdvisedSupport {

	// 创建代理对象
	private AopProxyFactory aopProxyFactory;

	// 采用先进先出的方式来进行事件,被观察者。
	private final List<AdvisedSupportListener> listeners = new ArrayList<>();

	/** Set to true when the first AOP proxy has been created. */
	// 创建第一个 AOP 代理时设置为 true。
	private boolean active = false;


	/**
	 * Create a new ProxyCreatorSupport instance.
	 */
	// 创建一个新的默认 ProxyCreatorSupport 实例
	public ProxyCreatorSupport() {
		// 利用默认的 DefaultAopProxyFactory 实现
		this.aopProxyFactory = new DefaultAopProxyFactory();
	}

	/**
	 * Create a new ProxyCreatorSupport instance.
	 * @param aopProxyFactory the AopProxyFactory to use
	 */
	// 创建一个新的允许外部修改 ProxyCreatorSupport 实例。
	// 形参：
	//			aopProxyFactory – 要使用的 AopProxyFactory
	public ProxyCreatorSupport(AopProxyFactory aopProxyFactory) {
		Assert.notNull(aopProxyFactory, "AopProxyFactory must not be null");
		this.aopProxyFactory = aopProxyFactory;
	}


	/**
	 * Customize the AopProxyFactory, allowing different strategies
	 * to be dropped in without changing the core framework.
	 * <p>Default is {@link DefaultAopProxyFactory}, using dynamic JDK
	 * proxies or CGLIB proxies based on the requirements.
	 */
	// 自定义 AopProxyFactory，允许在不改变核心框架的情况下放入不同的策略。
	// 默认为 DefaultAopProxyFactory ，根据要求使用动态 JDK 代理或 CGLIB 代理。
	public void setAopProxyFactory(AopProxyFactory aopProxyFactory) {
		Assert.notNull(aopProxyFactory, "AopProxyFactory must not be null");
		this.aopProxyFactory = aopProxyFactory;
	}

	/**
	 * Return the AopProxyFactory that this ProxyConfig uses.
	 */
	// 返回此 ProxyConfig 使用的 AopProxyFactory
	public AopProxyFactory getAopProxyFactory() {
		return this.aopProxyFactory;
	}

	/**
	 * Add the given AdvisedSupportListener to this proxy configuration.
	 * @param listener the listener to register
	 */
	// 将给定的 AdvisedSupportListener 添加到此代理配置。
	// 形参：
	//			listener - 要注册的侦听器
	public void addListener(AdvisedSupportListener listener) {
		Assert.notNull(listener, "AdvisedSupportListener must not be null");
		this.listeners.add(listener);
	}

	/**
	 * Remove the given AdvisedSupportListener from this proxy configuration.
	 * @param listener the listener to deregister
	 */
	// 从此代理配置中删除给定的 AdvisedSupportListener。
	// 形参：
	//			listener - 要注销的侦听器
	public void removeListener(AdvisedSupportListener listener) {
		Assert.notNull(listener, "AdvisedSupportListener must not be null");
		this.listeners.remove(listener);
	}


	/**
	 * Subclasses should call this to get a new AOP proxy. They should <b>not</b>
	 * create an AOP proxy with {@code this} as an argument.
	 */
	// 子类应该调用它来获得一个新的 AOP 代理。 他们不应该创建一个 AOP 代理 this 作为参数
	// 给子类访问(调用)并且不允许子类修改线程安全的同步实现
	// 触发事件：这是个标准操作
	protected final synchronized AopProxy createAopProxy() {
		if (!this.active) { // 使用标记位同步，即使没有加 volatile 这种原子操作也是可以的
			activate();
		}
		// Jdk 动态代理还是 CGLIB 字节码提升取决于 this 配置不同，
		// 即 DefaultAopProxyFactory 中做了策略选择
		return getAopProxyFactory().createAopProxy(this);
	}

	/**
	 * Activate this proxy configuration.
	 * @see AdvisedSupportListener#activated
	 */
	// 激活此代理配置，通知
	private void activate() {
		this.active = true;
		for (AdvisedSupportListener listener : this.listeners) {
			// 遍历逐一执行，将当前的配置放进去，触发事件监听，listener 就是用户添加监听的配置类
			// 调用用户增加的监听，通知被观察者消息。通知本身是它自己 this，因为它继承了 AdvisedSupport
			listener.activated(this);
		}
	}

	/**
	 * Propagate advice change event to all AdvisedSupportListeners.
	 * @see AdvisedSupportListener#adviceChanged
	 */
	// 将通知更改事件传播到所有 AdvisedSupportListener。
	@Override
	protected void adviceChanged() {
		// 首先调用父类的 adviceChanged(),将缓存清除
		super.adviceChanged();
		synchronized (this) {
			if (this.active) { // 事件传播
				for (AdvisedSupportListener listener : this.listeners) {
					// 调用用户添加的 adviceChanged 的事件监听，listener 就是用户添加监听的 containg class
					listener.adviceChanged(this);
				}
			}
		}
	}

	/**
	 * Subclasses can call this to check whether any AOP proxies have been created yet.
	 */
	// 子类可以调用它来检查是否已经创建了任何 AOP 代理
	protected final synchronized boolean isActive() {
		return this.active;
	}

}
