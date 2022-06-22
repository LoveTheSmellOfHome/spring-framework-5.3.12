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

package org.springframework.beans.factory.support;

import org.springframework.beans.factory.config.AutowiredPropertyMarker;
import org.springframework.beans.factory.config.BeanDefinitionCustomizer;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.core.ResolvableType;
import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;

import java.util.function.Supplier;

/**
 * Programmatic means of constructing
 * {@link org.springframework.beans.factory.config.BeanDefinition BeanDefinitions}
 * using the builder pattern. Intended primarily for use when implementing Spring 2.0
 * {@link org.springframework.beans.factory.xml.NamespaceHandler NamespaceHandlers}.
 *
 * @author Rod Johnson
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 2.0
 */
// 使用构建器模式构建 {@link org.springframework.beans.factory.config.BeanDefinition BeanDefinitions} 的编程方式。
// 主要用于实现 Spring 2.0 {@link org.springframework.beans.factory.xml.NamespaceHandler NamespaceHandlers}。
public final class BeanDefinitionBuilder {

	/**
	 * Create a new {@code BeanDefinitionBuilder} used to construct a {@link GenericBeanDefinition}.
	 */
	// 创建一个新的 BeanDefinitionBuilder 用于构造 GenericBeanDefinition 。
	public static BeanDefinitionBuilder genericBeanDefinition() {
		return new BeanDefinitionBuilder(new GenericBeanDefinition());
	}

	/**
	 * Create a new {@code BeanDefinitionBuilder} used to construct a {@link GenericBeanDefinition}.
	 * @param beanClassName the class name for the bean that the definition is being created for
	 */
	// 创建一个新的 {@code BeanDefinitionBuilder} 用于构造一个 {@link GenericBeanDefinition}。
	// @param beanClassName 正在为其创建定义的 bean 的类名
	public static BeanDefinitionBuilder genericBeanDefinition(String beanClassName) {
		BeanDefinitionBuilder builder = new BeanDefinitionBuilder(new GenericBeanDefinition());
		builder.beanDefinition.setBeanClassName(beanClassName);
		return builder;
	}

	/**
	 * Create a new {@code BeanDefinitionBuilder} used to construct a {@link GenericBeanDefinition}.
	 * @param beanClass the {@code Class} of the bean that the definition is being created for
	 */
	// 创建一个新的 BeanDefinitionBuilder 用于构造 GenericBeanDefinition 。
	// 参形：
	//			beanClass – 为其创建定义的 bean 的Class
	public static BeanDefinitionBuilder genericBeanDefinition(Class<?> beanClass) {
		BeanDefinitionBuilder builder = new BeanDefinitionBuilder(new GenericBeanDefinition());
		builder.beanDefinition.setBeanClass(beanClass);
		return builder;
	}

	/**
	 * Create a new {@code BeanDefinitionBuilder} used to construct a {@link GenericBeanDefinition}.
	 * @param beanClass the {@code Class} of the bean that the definition is being created for
	 * @param instanceSupplier a callback for creating an instance of the bean
	 * @since 5.0
	 */
	// 创建一个新的 BeanDefinitionBuilder 用于构造 GenericBeanDefinition 。
	// 参形：
	//			beanClass – 为其创建定义的 bean 的 Class
	//			instanceSupplier – 创建 bean 实例的回调
	public static <T> BeanDefinitionBuilder genericBeanDefinition(Class<T> beanClass, Supplier<T> instanceSupplier) {
		BeanDefinitionBuilder builder = new BeanDefinitionBuilder(new GenericBeanDefinition());
		builder.beanDefinition.setBeanClass(beanClass);
		builder.beanDefinition.setInstanceSupplier(instanceSupplier);
		return builder;
	}

	/**
	 * Create a new {@code BeanDefinitionBuilder} used to construct a {@link RootBeanDefinition}.
	 * @param beanClassName the class name for the bean that the definition is being created for
	 */
	// 创建一个新的 {@code BeanDefinitionBuilder} 用于构造一个 {@link RootBeanDefinition}。
	// @param beanClassName 正在为其创建定义的 bean 的类名
	public static BeanDefinitionBuilder rootBeanDefinition(String beanClassName) {
		return rootBeanDefinition(beanClassName, null);
	}

	/**
	 * Create a new {@code BeanDefinitionBuilder} used to construct a {@link RootBeanDefinition}.
	 * @param beanClassName the class name for the bean that the definition is being created for
	 * @param factoryMethodName the name of the method to use to construct the bean instance
	 */
	// 创建一个新的BeanDefinitionBuilder用于构造RootBeanDefinition 。
	// 参形：
	//			beanClassName – 为其创建定义的 bean 的类名
	//			factoryMethodName – 用于构造 bean 实例的方法的名称
	public static BeanDefinitionBuilder rootBeanDefinition(String beanClassName, @Nullable String factoryMethodName) {
		BeanDefinitionBuilder builder = new BeanDefinitionBuilder(new RootBeanDefinition());
		builder.beanDefinition.setBeanClassName(beanClassName);
		builder.beanDefinition.setFactoryMethodName(factoryMethodName);
		return builder;
	}

	/**
	 * Create a new {@code BeanDefinitionBuilder} used to construct a {@link RootBeanDefinition}.
	 * @param beanClass the {@code Class} of the bean that the definition is being created for
	 */
	// 创建一个新的 BeanDefinitionBuilder 用于构造 RootBeanDefinition 。
	// 参形：
	//			beanClass – 为其创建定义的 bean 的Class
	public static BeanDefinitionBuilder rootBeanDefinition(Class<?> beanClass) {
		return rootBeanDefinition(beanClass, (String) null);
	}

	/**
	 * Create a new {@code BeanDefinitionBuilder} used to construct a {@link RootBeanDefinition}.
	 * @param beanClass the {@code Class} of the bean that the definition is being created for
	 * @param factoryMethodName the name of the method to use to construct the bean instance
	 */
	// 创建一个新的 BeanDefinitionBuilder 用于构造 RootBeanDefinition 。
	// 参形：
	//			beanClass – 为其创建定义的 bean 的Class
	//			factoryMethodName – 用于构造 bean 实例的方法的名称
	public static BeanDefinitionBuilder rootBeanDefinition(Class<?> beanClass, @Nullable String factoryMethodName) {
		BeanDefinitionBuilder builder = new BeanDefinitionBuilder(new RootBeanDefinition());
		builder.beanDefinition.setBeanClass(beanClass);
		builder.beanDefinition.setFactoryMethodName(factoryMethodName);
		return builder;
	}

	/**
	 * Create a new {@code BeanDefinitionBuilder} used to construct a {@link RootBeanDefinition}.
	 * @param beanType the {@link ResolvableType type} of the bean that the definition is being created for
	 * @param instanceSupplier a callback for creating an instance of the bean
	 * @since 5.3.9
	 */
	// 创建一个新的BeanDefinitionBuilder用于构造RootBeanDefinition 。
	// 参形：
	//			beanType – 为其创建定义的 bean 的 type
	//			instanceSupplier – 创建 bean 实例的回调
	public static <T> BeanDefinitionBuilder rootBeanDefinition(ResolvableType beanType, Supplier<T> instanceSupplier) {
		RootBeanDefinition beanDefinition = new RootBeanDefinition();
		beanDefinition.setTargetType(beanType);
		beanDefinition.setInstanceSupplier(instanceSupplier);
		return new BeanDefinitionBuilder(beanDefinition);
	}

	/**
	 * Create a new {@code BeanDefinitionBuilder} used to construct a {@link RootBeanDefinition}.
	 * @param beanClass the {@code Class} of the bean that the definition is being created for
	 * @param instanceSupplier a callback for creating an instance of the bean
	 * @since 5.3.9
	 * @see #rootBeanDefinition(ResolvableType, Supplier)
	 */
	// 创建一个新的BeanDefinitionBuilder用于构造RootBeanDefinition 。
	// 参形：
	//			beanClass – 为其创建定义的 bean 的Class
	//			instanceSupplier – 创建 bean 实例的回调
	public static <T> BeanDefinitionBuilder rootBeanDefinition(Class<T> beanClass, Supplier<T> instanceSupplier) {
		return rootBeanDefinition(ResolvableType.forClass(beanClass), instanceSupplier);
	}

	/**
	 * Create a new {@code BeanDefinitionBuilder} used to construct a {@link ChildBeanDefinition}.
	 * @param parentName the name of the parent bean
	 */
	// 创建一个新的BeanDefinitionBuilder用于构造ChildBeanDefinition 。
	// 参形：
	//			parentName – 父 bean 的名称
	public static BeanDefinitionBuilder childBeanDefinition(String parentName) {
		return new BeanDefinitionBuilder(new ChildBeanDefinition(parentName));
	}


	/**
	 * The {@code BeanDefinition} instance we are creating.
	 */
	// 我们正在创建的 BeanDefinition 实例。
	private final AbstractBeanDefinition beanDefinition;

	/**
	 * Our current position with respect to constructor args.
	 */
	// 我们目前关于构造函数参数的位置
	private int constructorArgIndex;


	/**
	 * Enforce the use of factory methods.
	 */
	// 强制使用工厂方法。
	private BeanDefinitionBuilder(AbstractBeanDefinition beanDefinition) {
		this.beanDefinition = beanDefinition;
	}

	/**
	 * Return the current BeanDefinition object in its raw (unvalidated) form.
	 * @see #getBeanDefinition()
	 */
	// 以其原始（未验证）形式返回当前 BeanDefinition 对象。
	public AbstractBeanDefinition getRawBeanDefinition() {
		return this.beanDefinition;
	}

	/**
	 * Validate and return the created BeanDefinition object.
	 */
	// 验证并返回创建的 BeanDefinition 对象。
	public AbstractBeanDefinition getBeanDefinition() {
		this.beanDefinition.validate();
		return this.beanDefinition;
	}


	/**
	 * Set the name of the parent definition of this bean definition.
	 */
	// 设置此 bean 定义的父定义的名称
	public BeanDefinitionBuilder setParentName(String parentName) {
		this.beanDefinition.setParentName(parentName);
		return this;
	}

	/**
	 * Set the name of a static factory method to use for this definition,
	 * to be called on this bean's class.
	 */
	// 设置用于此定义的静态工厂方法的名称，以在此 bean 的类上调用。
	public BeanDefinitionBuilder setFactoryMethod(String factoryMethod) {
		this.beanDefinition.setFactoryMethodName(factoryMethod);
		return this;
	}

	/**
	 * Set the name of a non-static factory method to use for this definition,
	 * including the bean name of the factory instance to call the method on.
	 * @param factoryMethod the name of the factory method
	 * @param factoryBean the name of the bean to call the specified factory method on
	 * @since 4.3.6
	 */
	// 设置用于此定义的非静态工厂方法的名称，包括要调用该方法的工厂实例的 bean 名称。
	// 参形：
	//			factoryMethod – 工厂方法的名称
	//			factoryBean – 调用指定工厂方法的 bean 的名称
	public BeanDefinitionBuilder setFactoryMethodOnBean(String factoryMethod, String factoryBean) {
		this.beanDefinition.setFactoryMethodName(factoryMethod);
		this.beanDefinition.setFactoryBeanName(factoryBean);
		return this;
	}

	/**
	 * Add an indexed constructor arg value. The current index is tracked internally
	 * and all additions are at the present point.
	 */
	// 添加索引构造函数 arg 值。当前索引在内部进行跟踪，所有添加都在当前点
	public BeanDefinitionBuilder addConstructorArgValue(@Nullable Object value) {
		this.beanDefinition.getConstructorArgumentValues().addIndexedArgumentValue(
				this.constructorArgIndex++, value);
		return this;
	}

	/**
	 * Add a reference to a named bean as a constructor arg.
	 * @see #addConstructorArgValue(Object)
	 */
	// 添加对命名 bean 的引用作为构造函数 arg。
	public BeanDefinitionBuilder addConstructorArgReference(String beanName) {
		this.beanDefinition.getConstructorArgumentValues().addIndexedArgumentValue(
				this.constructorArgIndex++, new RuntimeBeanReference(beanName));
		return this;
	}

	/**
	 * Add the supplied property value under the given property name.
	 */
	// 在给定的属性名称下添加提供的属性值。
	public BeanDefinitionBuilder addPropertyValue(String name, @Nullable Object value) {
		this.beanDefinition.getPropertyValues().add(name, value);
		return this;
	}

	/**
	 * Add a reference to the specified bean name under the property specified.
	 * @param name the name of the property to add the reference to
	 * @param beanName the name of the bean being referenced
	 */
	// 在指定的属性下添加对指定 bean 名称的引用。
	// 参形：
	//			name - 要添加引用的属性的名称
	//			beanName – 被引用的 bean 的名称
	public BeanDefinitionBuilder addPropertyReference(String name, String beanName) {
		this.beanDefinition.getPropertyValues().add(name, new RuntimeBeanReference(beanName));
		return this;
	}

	/**
	 * Add an autowired marker for the specified property on the specified bean.
	 * @param name the name of the property to mark as autowired
	 * @since 5.2
	 * @see AutowiredPropertyMarker
	 */
	// 为指定 bean 上的指定属性添加自动装配标记。
	public BeanDefinitionBuilder addAutowiredProperty(String name) {
		this.beanDefinition.getPropertyValues().add(name, AutowiredPropertyMarker.INSTANCE);
		return this;
	}

	/**
	 * Set the init method for this definition.
	 */
	// 为此定义设置 init 方法。
	public BeanDefinitionBuilder setInitMethodName(@Nullable String methodName) {
		this.beanDefinition.setInitMethodName(methodName);
		return this;
	}

	/**
	 * Set the destroy method for this definition.
	 */
	// 为此定义设置销毁方法。
	public BeanDefinitionBuilder setDestroyMethodName(@Nullable String methodName) {
		this.beanDefinition.setDestroyMethodName(methodName);
		return this;
	}


	/**
	 * Set the scope of this definition.
	 * @see org.springframework.beans.factory.config.BeanDefinition#SCOPE_SINGLETON
	 * @see org.springframework.beans.factory.config.BeanDefinition#SCOPE_PROTOTYPE
	 */
	// 设置此定义的范围
	public BeanDefinitionBuilder setScope(@Nullable String scope) {
		this.beanDefinition.setScope(scope);
		return this;
	}

	/**
	 * Set whether or not this definition is abstract.
	 */
	// 设置这个定义是否是抽象的
	public BeanDefinitionBuilder setAbstract(boolean flag) {
		this.beanDefinition.setAbstract(flag);
		return this;
	}

	/**
	 * Set whether beans for this definition should be lazily initialized or not.
	 */
	// 设置此定义的 bean 是否应该延迟初始化
	public BeanDefinitionBuilder setLazyInit(boolean lazy) {
		this.beanDefinition.setLazyInit(lazy);
		return this;
	}

	/**
	 * Set the autowire mode for this definition.
	 */
	// 为此定义设置自动装配模式
	public BeanDefinitionBuilder setAutowireMode(int autowireMode) {
		this.beanDefinition.setAutowireMode(autowireMode);
		return this;
	}

	/**
	 * Set the dependency check mode for this definition.
	 */
	// 为此定义设置依赖性检查模式。
	public BeanDefinitionBuilder setDependencyCheck(int dependencyCheck) {
		this.beanDefinition.setDependencyCheck(dependencyCheck);
		return this;
	}

	/**
	 * Append the specified bean name to the list of beans that this definition
	 * depends on.
	 */
	// 将指定的 bean 名称添加到此定义所依赖的 bean 列表中
	public BeanDefinitionBuilder addDependsOn(String beanName) {
		if (this.beanDefinition.getDependsOn() == null) {
			this.beanDefinition.setDependsOn(beanName);
		}
		else {
			String[] added = ObjectUtils.addObjectToArray(this.beanDefinition.getDependsOn(), beanName);
			this.beanDefinition.setDependsOn(added);
		}
		return this;
	}

	/**
	 * Set whether this bean is a primary autowire candidate.
	 * @since 5.1.11
	 */
	public BeanDefinitionBuilder setPrimary(boolean primary) {
		this.beanDefinition.setPrimary(primary);
		return this;
	}

	/**
	 * Set the role of this definition.
	 */
	public BeanDefinitionBuilder setRole(int role) {
		this.beanDefinition.setRole(role);
		return this;
	}

	/**
	 * Set whether this bean is 'synthetic', that is, not defined by
	 * the application itself.
	 * @since 5.3.9
	 */
	// 设置这个 bean 是否是“合成的”，即不是由应用程序本身定义的
	public BeanDefinitionBuilder setSynthetic(boolean synthetic) {
		this.beanDefinition.setSynthetic(synthetic);
		return this;
	}

	/**
	 * Apply the given customizers to the underlying bean definition.
	 * @since 5.0
	 */
	// 将自定义给定的 BeanDefinition 的回调
	public BeanDefinitionBuilder applyCustomizers(BeanDefinitionCustomizer... customizers) {
		for (BeanDefinitionCustomizer customizer : customizers) {
			customizer.customize(this.beanDefinition);
		}
		return this;
	}

}
