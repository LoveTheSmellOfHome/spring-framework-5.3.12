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

package org.springframework.beans;

import java.beans.PropertyDescriptor;

/**
 * The central interface of Spring's low-level JavaBeans infrastructure.
 *
 * <p>Typically not used directly but rather implicitly via a
 * {@link org.springframework.beans.factory.BeanFactory} or a
 * {@link org.springframework.validation.DataBinder}.
 *
 * <p>Provides operations to analyze and manipulate standard JavaBeans:
 * the ability to get and set property values (individually or in bulk),
 * get property descriptors, and query the readability/writability of properties.
 *
 * <p>This interface supports <b>nested properties</b> enabling the setting
 * of properties on subproperties to an unlimited depth.
 *
 * <p>A BeanWrapper's default for the "extractOldValueForEditor" setting
 * is "false", to avoid side effects caused by getter method invocations.
 * Turn this to "true" to expose present property values to custom editors.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 13 April 2001
 * @see PropertyAccessor
 * @see PropertyEditorRegistry
 * @see PropertyAccessorFactory#forBeanPropertyAccess
 * @see org.springframework.beans.factory.BeanFactory
 * @see org.springframework.validation.BeanPropertyBindingResult
 * @see org.springframework.validation.DataBinder#initBeanPropertyAccess()
 */
// Spring 的底层 JavaBeans 基础结构的中央接口
//
// <p>通常不直接使用，而是通过 {@link org.springframework.beans.factory.BeanFactory} 或
// {@link org.springframework.validation.DataBinder} 隐式使用。
//
// <p>提供分析和操作标准JavaBeans的操作：获取和设置属性值（单独或批量）、获取属性描述符、查询属性的可读性和可写性的能力。
//
// <p>此接口支持<b>嵌套属性<b>，可以将子属性的属性设置为无限深度
//
// <p>BeanWrapper 的“extractOldValueForEditor”设置默认为"false"，以避免由 getter 方法调用引起的副作用。
// 将此设置为"true",以向自定义编辑器公开当前属性值。
//
//	Spring 与 JavaBeans 交互的核心底层 API,不直接使用，通过 BeanFactory 和 DataBinder 来使用
//	BeanWrapper 和 JavaBeans 的区别：1.BeanWrapper 时基于 JavaBeans 的二次封装，简化 JavaBeans，
//	将一些不重要的概念模糊掉，如事件等。
public interface BeanWrapper extends ConfigurablePropertyAccessor {

	/**
	 * Specify a limit for array and collection auto-growing.
	 * <p>Default is unlimited on a plain BeanWrapper.
	 * @since 4.1
	 */
	// 指定数组和集合自动增长的限制。 <p>在普通 BeanWrapper 上默认是无限制的。
	void setAutoGrowCollectionLimit(int autoGrowCollectionLimit);

	/**
	 * Return the limit for array and collection auto-growing.
	 * @since 4.1
	 */
	int getAutoGrowCollectionLimit();

	/**
	 * Return the bean instance wrapped by this object.
	 */
	// 返回此对象包装的 bean 实例
	Object getWrappedInstance();

	/**
	 * Return the type of the wrapped bean instance.
	 */
	// 返回包装的 bean 实例的类型。通常是被 ClassLoader 加载后的对象
	Class<?> getWrappedClass();

	/**
	 * Obtain the PropertyDescriptors for the wrapped object
	 * (as determined by standard JavaBeans introspection).
	 * @return the PropertyDescriptors for the wrapped object
	 */
	// 获取包装对象的 PropertyDescriptors（由标准 JavaBeans 内省确定）。
	// @return 包装对象的 PropertyDescriptors
	PropertyDescriptor[] getPropertyDescriptors();

	/**
	 * Obtain the property descriptor for a specific property
	 * of the wrapped object.
	 * @param propertyName the property to obtain the descriptor for
	 * (may be a nested path, but no indexed/mapped property)
	 * @return the property descriptor for the specified property
	 * @throws InvalidPropertyException if there is no such property
	 */
	// 获取包装对象的特定属性的属性描述符
	// @param propertyName 要为其获取描述符的属性（可能是嵌套路径，但没有索引映射属性）
	PropertyDescriptor getPropertyDescriptor(String propertyName) throws InvalidPropertyException;

}
