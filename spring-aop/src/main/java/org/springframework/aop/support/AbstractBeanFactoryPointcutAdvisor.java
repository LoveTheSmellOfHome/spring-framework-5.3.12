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

package org.springframework.aop.support;

import java.io.IOException;
import java.io.ObjectInputStream;

import org.aopalliance.aop.Advice;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Abstract BeanFactory-based PointcutAdvisor that allows for any Advice
 * to be configured as reference to an Advice bean in a BeanFactory.
 *
 * <p>Specifying the name of an advice bean instead of the advice object itself
 * (if running within a BeanFactory) increases loose coupling at initialization time,
 * in order to not initialize the advice object until the pointcut actually matches.
 *
 * @author Juergen Hoeller
 * @since 2.0.2
 * @see #setAdviceBeanName
 * @see DefaultBeanFactoryPointcutAdvisor
 */
// 基于Spring IoC 容器 BeanFactory 的抽象 PointcutAdvisor 允许将任何 Advice 配置为对 BeanFactory
// 中的 Advice bean 的引用。
//
// 指定通知 bean 的名称而不是通知对象本身（如果在 BeanFactory 中运行）会增加初始化时的松散耦合，以便在
// 切入点实际匹配之前不初始化通知对象。
@SuppressWarnings("serial")
public abstract class AbstractBeanFactoryPointcutAdvisor extends AbstractPointcutAdvisor implements BeanFactoryAware {

	// 指定容器种 bean 的名称，这里的 Advice 是通过 bean 的形式，通过名称的方法去引用它。
	@Nullable
	private String adviceBeanName;

	// 指定容器,提供一来查找的能力
	@Nullable
	private BeanFactory beanFactory;

	// 定义 Advice 动作
	@Nullable
	private transient volatile Advice advice;

	// 定义锁
	private transient volatile Object adviceMonitor = new Object();


	/**
	 * Specify the name of the advice bean that this advisor should refer to.
	 * <p>An instance of the specified bean will be obtained on first access
	 * of this advisor's advice. This advisor will only ever obtain at most one
	 * single instance of the advice bean, caching the instance for the lifetime
	 * of the advisor.
	 * @see #getAdvice()
	 */
	// 指定此顾问程序应引用的建议 bean 的名称。
	// 首次访问此顾问的建议时，将获得指定 bean 的实例。该顾问最多只能获得一个通知 bean 的实例，并在顾问的生命周期内缓存该实例
	public void setAdviceBeanName(@Nullable String adviceBeanName) {
		this.adviceBeanName = adviceBeanName;
	}

	/**
	 * Return the name of the advice bean that this advisor refers to, if any.
	 */
	// 返回此顾问所引用的建议 bean 的名称（如果有）
	@Nullable
	public String getAdviceBeanName() {
		return this.adviceBeanName;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		// 关联 IoC 工厂
		this.beanFactory = beanFactory;
		resetAdviceMonitor();
	}

	private void resetAdviceMonitor() {
		if (this.beanFactory instanceof ConfigurableBeanFactory) {
			this.adviceMonitor = ((ConfigurableBeanFactory) this.beanFactory).getSingletonMutex();
		}
		else {
			this.adviceMonitor = new Object();
		}
	}

	/**
	 * Specify a particular instance of the target advice directly,
	 * avoiding lazy resolution in {@link #getAdvice()}.
	 * @since 3.1
	 */
	// 直接指定目标通知的特定实例，避免在 getAdvice()中延迟解析
	public void setAdvice(Advice advice) {
		synchronized (this.adviceMonitor) {
			this.advice = advice;
		}
	}

	// 获取 Advice
	@Override
	public Advice getAdvice() {
		Advice advice = this.advice;
		if (advice != null) {
			return advice;
		}

		Assert.state(this.adviceBeanName != null, "'adviceBeanName' must be specified");
		Assert.state(this.beanFactory != null, "BeanFactory must be set to resolve 'adviceBeanName'");

		if (this.beanFactory.isSingleton(this.adviceBeanName)) {
			// Rely on singleton semantics provided by the factory.
			// 依赖查找：依赖工厂提供的单例语义
			// 依赖查找到底有什么作用：在内部的 Spring AOP 会经常用到依赖查找，因为它自己不具备依赖注入的能力，
			// 因为它还没到那个生命周期。就开始要查找相应的 bean。
			// 单例本身不具备状态，是天然线程安全的
			advice = this.beanFactory.getBean(this.adviceBeanName, Advice.class);
			this.advice = advice;
			return advice;
		}
		else {
			// No singleton guarantees from the factory -> let's lock locally but
			// reuse the factory's singleton lock, just in case a lazy dependency
			// of our advice bean happens to trigger the singleton lock implicitly...
			// 工厂没有单例保证 -> 让我们在本地锁定，但重用工厂的单例锁，以防万一我们的建议 bean 的惰性依赖碰巧隐式触发单例锁...
			// 原型：在并发的时候，加锁保证不产生脏数据，getBean() 本身就有锁，这里使用了 getBean()的锁
			synchronized (this.adviceMonitor) {
				advice = this.advice;
				if (advice == null) {
					advice = this.beanFactory.getBean(this.adviceBeanName, Advice.class);
					this.advice = advice;
				}
				return advice;
			}
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(getClass().getName());
		sb.append(": advice ");
		if (this.adviceBeanName != null) {
			sb.append("bean '").append(this.adviceBeanName).append('\'');
		}
		else {
			sb.append(this.advice);
		}
		return sb.toString();
	}


	//---------------------------------------------------------------------
	// Serialization support
	//---------------------------------------------------------------------

	private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
		// Rely on default serialization, just initialize state after deserialization.
		// 依赖默认序列化，反序列化后初始化状态即可
		ois.defaultReadObject();

		// Initialize transient fields.
		// 初始化瞬态字段
		resetAdviceMonitor();
	}

}
