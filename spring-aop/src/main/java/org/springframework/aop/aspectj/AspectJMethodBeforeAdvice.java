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

package org.springframework.aop.aspectj;

import java.io.Serializable;
import java.lang.reflect.Method;

import org.springframework.aop.MethodBeforeAdvice;
import org.springframework.lang.Nullable;

/**
 * Spring AOP advice that wraps an AspectJ before method.
 *
 * @author Rod Johnson
 * @author Adrian Colyer
 * @since 2.0
 */
// 包装 AspectJ before 方法的 Spring AOP 建议，Joinpoint Before Advice AspectJ 实现
// 处理 Spring AOP 中 @Before 标注的方法，MethodBeforeAdvice 在 Spring 框架内部会被包装成 MethodBeforeAdviceInterceptor
// 换言之，AspectJMethodBeforeAdvice 也会被转换成 MethodBeforeAdviceInterceptor。
@SuppressWarnings("serial")
public class AspectJMethodBeforeAdvice extends AbstractAspectJAdvice implements MethodBeforeAdvice, Serializable {

	// 第一个参数是 拦截方法，第二个参数是注解上的表达式，比如 AspectJ 的 @Before 注解，
	// 它支持两种方式即它的注解属性 value 有两种方式，要么是表达式内容，要么是表达式名称
	// 一种是支持表达式直接输入，另一种支持引入 Pointcut，这三个参数都是来自 Spring 构造器的注入
	// 分别是来自于 FactoryBean 定义，RootBeanDefinition 定义，AspectInstanceFactory BeanDefinition 定义
	public AspectJMethodBeforeAdvice(
			Method aspectJBeforeAdviceMethod, AspectJExpressionPointcut pointcut, AspectInstanceFactory aif) {

		super(aspectJBeforeAdviceMethod, pointcut, aif);
	}


	// 具体实现 MethodBeforeAdviceInterceptor,注意 两者实现都实现了 BeforeAdvice 接口
	@Override
	public void before(Method method, Object[] args, @Nullable Object target) throws Throwable {
		// 调用目标方法，如果时 BeforeAdvice 或者 AfterAdvice,先去匹配 JoinPoint,然后调用
		invokeAdviceMethod(getJoinPointMatch(), null, null);
	}

	// 判断时 BeforeAdvice 还是 AfterAdvice,互斥操作。
	@Override
	public boolean isBeforeAdvice() {
		return true;
	}

	@Override
	public boolean isAfterAdvice() {
		return false;
	}

}
