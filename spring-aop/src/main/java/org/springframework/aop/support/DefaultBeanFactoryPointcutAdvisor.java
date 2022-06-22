/*
 * Copyright 2002-2017 the original author or authors.
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

import org.springframework.aop.Pointcut;
import org.springframework.lang.Nullable;

/**
 * Concrete BeanFactory-based PointcutAdvisor that allows for any Advice
 * to be configured as reference to an Advice bean in the BeanFactory,
 * as well as the Pointcut to be configured through a bean property.
 *
 * <p>Specifying the name of an advice bean instead of the advice object itself
 * (if running within a BeanFactory) increases loose coupling at initialization time,
 * in order to not initialize the advice object until the pointcut actually matches.
 *
 * @author Juergen Hoeller
 * @since 2.0.2
 * @see #setPointcut
 * @see #setAdviceBeanName
 */
// 基于 BeanFactory 的具体 PointcutAdvisor 允许将任何 Advice 配置为对 BeanFactory 中的 Advice bean 的引用，
// 以及通过 bean 属性配置的 Pointcut。
//
// 指定通知 bean 的名称而不是通知对象本身（如果在 BeanFactory 中运行）会增加初始化时的松散耦合，以便在切入点实际匹配之前不初始化通知对象。
//
// 有 IoC 能力的 PointcutAdvisor，它的能力来自于父类
@SuppressWarnings("serial")
public class DefaultBeanFactoryPointcutAdvisor extends AbstractBeanFactoryPointcutAdvisor {

	private Pointcut pointcut = Pointcut.TRUE;


	/**
	 * Specify the pointcut targeting the advice.
	 * <p>Default is {@code Pointcut.TRUE}.
	 * @see #setAdviceBeanName
	 */
	// 指定针对建议的切入点。
	// 默认为 Pointcut.TRUE，这里写入的都是 原型 bean
	public void setPointcut(@Nullable Pointcut pointcut) {
		this.pointcut = (pointcut != null ? pointcut : Pointcut.TRUE);
	}

	@Override
	public Pointcut getPointcut() {
		return this.pointcut;
	}


	@Override
	public String toString() {
		return getClass().getName() + ": pointcut [" + getPointcut() + "]; advice bean '" + getAdviceBeanName() + "'";
	}

}
