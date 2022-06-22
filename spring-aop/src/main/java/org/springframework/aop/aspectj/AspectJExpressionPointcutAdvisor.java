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

package org.springframework.aop.aspectj;

import org.springframework.aop.Pointcut;
import org.springframework.aop.support.AbstractGenericPointcutAdvisor;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.lang.Nullable;

/**
 * Spring AOP Advisor that can be used for any AspectJ pointcut expression.
 *
 * @author Rob Harrop
 * @since 2.0
 */
// 可用于任何 AspectJ 切入点表达式的 Spring AOP Advisor
@SuppressWarnings("serial")
public class AspectJExpressionPointcutAdvisor extends AbstractGenericPointcutAdvisor implements BeanFactoryAware {

	// Spring 支持的 AspectJ 原语表达式
	private final AspectJExpressionPointcut pointcut = new AspectJExpressionPointcut();

	// 传递表达式
	public void setExpression(@Nullable String expression) {
		// 将表达式透传到 Pointcut,最终帮助 Pointcut 来筛选，这里告诉我们一个事实就是这里的实现基于 Pointcut
		// 来设置我们的表达式。同时利用 AspectJ 表达式的能力来获取我们相应的元信息的能力。因为 Spring 只支持方法
		// 级别的拦截，所以这里的 Joinpoint 是一个方法级别的 Joinpoint.关于这里的元信息就是指反射中的 Method
		this.pointcut.setExpression(expression);
	}

	// 获取表达式
	@Nullable
	public String getExpression() {
		return this.pointcut.getExpression();
	}

	// location 是 Pointcut 的元信息
	public void setLocation(@Nullable String location) {
		this.pointcut.setLocation(location);
	}

	@Nullable
	public String getLocation() {
		return this.pointcut.getLocation();
	}

	public void setParameterNames(String... names) {
		this.pointcut.setParameterNames(names);
	}

	public void setParameterTypes(Class<?>... types) {
		this.pointcut.setParameterTypes(types);
	}

	// 和 IoC 容器进行关联
	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		this.pointcut.setBeanFactory(beanFactory);
	}

	@Override
	public Pointcut getPointcut() {
		return this.pointcut;
	}

}
