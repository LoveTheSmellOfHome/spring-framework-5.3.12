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

package org.springframework.expression;

/**
 * A bean resolver can be registered with the evaluation context and will kick in
 * for bean references: {@code @myBeanName} and {@code &myBeanName} expressions.
 * The {@code &} variant syntax allows access to the factory bean where relevant.
 *
 * @author Andy Clement
 * @since 3.0.3
 */
// bean 解析器可以在评估上下文中注册，并将用于 bean 引用： @myBeanName 和 &myBeanName 表达式。
// &变体语法允许访问相关的工厂 bean
public interface BeanResolver {

	/**
	 * Look up a bean by the given name and return a corresponding instance for it.
	 * For attempting access to a factory bean, the name needs a {@code &} prefix.
	 * @param context the current evaluation context
	 * @param beanName the name of the bean to look up
	 * @return an object representing the bean
	 * @throws AccessException if there is an unexpected problem resolving the bean
	 */
	// 通过给定的名称查找 bean 并为其返回相应的实例。 为了尝试访问工厂 bean，名称需要一个&前缀。
	// 形参：上下文——当前的评估上下文
	// beanName – 要查找的 bean 的名称
	// 返回值：代表 bean 的对象
	// AccessException - 如果在解决 bean 时出现意外问题
	Object resolve(EvaluationContext context, String beanName) throws AccessException;

}
