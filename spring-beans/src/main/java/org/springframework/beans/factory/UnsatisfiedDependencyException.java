/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.beans.factory;

import org.springframework.beans.BeansException;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

/**
 * Exception thrown when a bean depends on other beans or simple properties
 * that were not specified in the bean factory definition, although
 * dependency checking was enabled.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 03.09.2003
 */
// 当 bean 依赖于其他 bean 或 bean 工厂定义中未指定的简单属性时抛出异常，尽管启用了依赖项检查。
@SuppressWarnings("serial")
public class UnsatisfiedDependencyException extends BeanCreationException {

	@Nullable
	private final InjectionPoint injectionPoint;


	/**
	 * Create a new UnsatisfiedDependencyException.
	 * @param resourceDescription description of the resource that the bean definition came from
	 * @param beanName the name of the bean requested
	 * @param propertyName the name of the bean property that couldn't be satisfied
	 * @param msg the detail message
	 */
	// 创建一个新的 UnsatisfiedDependencyException。
	// 形参：
	//			resourceDescription – bean 定义来自的资源的描述
	//			beanName – 请求的 bean 的名称
	//			propertyName – 无法满足的 bean 属性的名称
	//			msg – 详细消息
	public UnsatisfiedDependencyException(
			@Nullable String resourceDescription, @Nullable String beanName, String propertyName, String msg) {

		super(resourceDescription, beanName,
				"Unsatisfied dependency expressed through bean property '" + propertyName + "'" +
				(StringUtils.hasLength(msg) ? ": " + msg : ""));
		this.injectionPoint = null;
	}

	/**
	 * Create a new UnsatisfiedDependencyException.
	 * @param resourceDescription description of the resource that the bean definition came from
	 * @param beanName the name of the bean requested
	 * @param propertyName the name of the bean property that couldn't be satisfied
	 * @param ex the bean creation exception that indicated the unsatisfied dependency
	 */
	// 创建一个新的 UnsatisfiedDependencyException。
	// 形参：
	//			resourceDescription – bean 定义来自的资源的描述
	//			beanName – 请求的 bean 的名称
	//			propertyName – 无法满足的 bean 属性的名称
	//			ex – 指示未满足依赖项的 bean 创建异常
	public UnsatisfiedDependencyException(
			@Nullable String resourceDescription, @Nullable String beanName, String propertyName, BeansException ex) {

		this(resourceDescription, beanName, propertyName, "");
		initCause(ex);
	}

	/**
	 * Create a new UnsatisfiedDependencyException.
	 * @param resourceDescription description of the resource that the bean definition came from
	 * @param beanName the name of the bean requested
	 * @param injectionPoint the injection point (field or method/constructor parameter)
	 * @param msg the detail message
	 * @since 4.3
	 */
	// 创建一个新的 UnsatisfiedDependencyException。
	// 形参：
	//			resourceDescription – bean 定义来自的资源的描述
	//			beanName – 请求的 bean 的名称
	//			injectionPoint – 注入点（字段或方法/构造函数参数）
	//			msg – 详细消息
	public UnsatisfiedDependencyException(
			@Nullable String resourceDescription, @Nullable String beanName, @Nullable InjectionPoint injectionPoint, String msg) {

		super(resourceDescription, beanName,
				"Unsatisfied dependency expressed through " + injectionPoint +
				(StringUtils.hasLength(msg) ? ": " + msg : ""));
		this.injectionPoint = injectionPoint;
	}

	/**
	 * Create a new UnsatisfiedDependencyException.
	 * @param resourceDescription description of the resource that the bean definition came from
	 * @param beanName the name of the bean requested
	 * @param injectionPoint the injection point (field or method/constructor parameter)
	 * @param ex the bean creation exception that indicated the unsatisfied dependency
	 * @since 4.3
	 */
	// 创建一个新的 UnsatisfiedDependencyException。
	// 形参：
	//			resourceDescription – bean 定义来自的资源的描述
	//			beanName – 请求的 bean 的名称
	//			injectionPoint – 注入点（字段或方法/构造函数参数）
	//			ex – 指示未满足依赖项的 bean 创建异常
	public UnsatisfiedDependencyException(
			@Nullable String resourceDescription, @Nullable String beanName, @Nullable InjectionPoint injectionPoint, BeansException ex) {

		this(resourceDescription, beanName, injectionPoint, "");
		initCause(ex);
	}


	/**
	 * Return the injection point (field or method/constructor parameter), if known.
	 * @since 4.3
	 */
	// 如果已知，则返回注入点（字段或方法/构造函数参数）。
	@Nullable
	public InjectionPoint getInjectionPoint() {
		return this.injectionPoint;
	}

}
