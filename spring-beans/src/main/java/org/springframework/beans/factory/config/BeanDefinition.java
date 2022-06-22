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

package org.springframework.beans.factory.config;

import org.springframework.beans.BeanMetadataElement;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.core.AttributeAccessor;
import org.springframework.core.ResolvableType;
import org.springframework.lang.Nullable;

/**
 * A BeanDefinition describes a bean instance, which has property values,
 * constructor argument values, and further information supplied by
 * concrete implementations.
 *
 * <p>This is just a minimal interface: The main intention is to allow a
 * {@link BeanFactoryPostProcessor} to introspect and modify property values
 * and other bean metadata.
 *
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @since 19.03.2004
 * @see ConfigurableListableBeanFactory#getBeanDefinition
 * @see org.springframework.beans.factory.support.RootBeanDefinition
 * @see org.springframework.beans.factory.support.ChildBeanDefinition
 */
// BeanDefinition 描述了一个 bean 实例，它具有属性值、构造函数参数值以及由具体实现提供的更多信息。
// 这只是一个最小的接口：主要目的是允许 {@link BeanFactoryPostProcessor} 检查和修改属性值和其他 bean 元数据
// 以文本的方式描述一个 bean 的基本定义，内部属性就是 bean 的元数据
//
// BeanDefinition 就是 Bean 元信息的数据结构，无论是 XML 方式，Properties 方式，还是注解 @Bean 方式，它最终都会变成 BeanDefinition
// 它控制者 Bean 的状态，比如说 Bean 的属性 PropertyValues.同时控制着 Bean 的一个初始化行为或者销毁行为。我们称之为生命周期。
// 它也控制着 Bean 的创建方式，比如说静态方式，工厂方法以及 工厂 Bean.
//
// 在 Spring IoC 容器中 BeanDefinition 都是唯一的，在创建 Bean 的时候，Bean 的类型或者范围是有差异的
public interface BeanDefinition extends AttributeAccessor, BeanMetadataElement {

	/**
	 * Scope identifier for the standard singleton scope: {@value}.
	 * <p>Note that extended bean factories might support further scopes.
	 * @see #setScope
	 * @see ConfigurableBeanFactory#SCOPE_SINGLETON
	 */
	// 单例
	String SCOPE_SINGLETON = ConfigurableBeanFactory.SCOPE_SINGLETON;

	/**
	 * Scope identifier for the standard prototype scope: {@value}.
	 * <p>Note that extended bean factories might support further scopes.
	 * @see #setScope
	 * @see ConfigurableBeanFactory#SCOPE_PROTOTYPE
	 */
	// 多例
	String SCOPE_PROTOTYPE = ConfigurableBeanFactory.SCOPE_PROTOTYPE;


	/**
	 * Role hint indicating that a {@code BeanDefinition} is a major part
	 * of the application. Typically corresponds to a user-defined bean.
	 */
	// 指示 BeanDefinition 是应用程序主要部分的角色提示。 通常对应于用户定义的 bean。
	// 基于业务级别的角色
	int ROLE_APPLICATION = 0;

	/**
	 * Role hint indicating that a {@code BeanDefinition} is a supporting
	 * part of some larger configuration, typically an outer
	 * {@link org.springframework.beans.factory.parsing.ComponentDefinition}.
	 * {@code SUPPORT} beans are considered important enough to be aware
	 * of when looking more closely at a particular
	 * {@link org.springframework.beans.factory.parsing.ComponentDefinition},
	 * but not when looking at the overall configuration of an application.
	 */
	// 角色提示表明 BeanDefinition 是一些较大配置的支持部分，通常是外部
	// org.springframework.beans.factory.parsing.ComponentDefinition。SUPPORT bean 被认为足够重要，
	// 以便在更仔细地查看特定 org.springframework.beans.factory.parsing.ComponentDefinition 时意识到这一点，
	// 但在查看应用程序的整体配置时则不然。
	// 基于配置级别的角色
	int ROLE_SUPPORT = 1;

	/**
	 * Role hint indicating that a {@code BeanDefinition} is providing an
	 * entirely background role and has no relevance to the end-user. This hint is
	 * used when registering beans that are completely part of the internal workings
	 * of a {@link org.springframework.beans.factory.parsing.ComponentDefinition}.
	 */

	// Spring 中 Scope 和 Role 的区别：
	// 因为 Spring 是一个应用上下文，它如何区分出不同的 bean 呢？我们不能通过 Scope,Scope 是描述 bean 的存活范围
	// 比如单例的就是全局的，原型的就是每次去取每次都是新的，Request 是每次请求每次都是新的， Session 是每次用户会话
	// 每次都是新的。bean 的生命存活范围是不一样的，Role 使它扮演的角色或者 bean 的分类，
	//
	// 指示 BeanDefinition 提供完全后台角色且与最终用户无关的角色提示。在注册完全属于
	// org.springframework.beans.factory.parsing.ComponentDefinition 内部工作的 bean 时使用此提示
	// 基于 Spring 内部的
	int ROLE_INFRASTRUCTURE = 2;


	// Modifiable attributes

	/**
	 * Set the name of the parent definition of this bean definition, if any.
	 */
	void setParentName(@Nullable String parentName);

	/**
	 * Return the name of the parent definition of this bean definition, if any.
	 */
	// 返回此 bean 定义的父定义的名称，如果有的话,和 xml 中配置的 parent 一一对应,体现层次性
	@Nullable
	String getParentName();

	/**
	 * Specify the bean class name of this bean definition.
	 * <p>The class name can be modified during bean factory post-processing,
	 * typically replacing the original class name with a parsed variant of it.
	 * @see #setParentName
	 * @see #setFactoryBeanName
	 * @see #setFactoryMethodName
	 */
	// 指定此 bean 定义的 bean 类名称,以文本的形式呈现，因此需要 ClassLoader 来加载，将文本编程 java Class 对象
	// <p>可以在 bean factory 后处理期间修改类名，通常用它的解析变体替换原始类名
	void setBeanClassName(@Nullable String beanClassName);

	/**
	 * Return the current bean class name of this bean definition.
	 * <p>Note that this does not have to be the actual class name used at runtime, in
	 * case of a child definition overriding/inheriting the class name from its parent.
	 * Also, this may just be the class that a factory method is called on, or it may
	 * even be empty in case of a factory bean reference that a method is called on.
	 * Hence, do <i>not</i> consider this to be the definitive bean type at runtime but
	 * rather only use it for parsing purposes at the individual bean definition level.
	 * @see #getParentName()
	 * @see #getFactoryBeanName()
	 * @see #getFactoryMethodName()
	 */
	// 返回此 bean 定义的当前 bean 类名称
	// <p>请注意，这不是运行时使用的实际类名，以防子定义覆盖从其父级继承类名。
	// 此外，这可能只是调用工厂方法的类，或者在调用方法的工厂 bean 引用的情况下它甚至可能为空。
	// 因此，不要<i>不要<i>将其视为运行时确定的 bean 类型，而是仅将其用于单个 bean 定义级别的解析目的。
	@Nullable
	String getBeanClassName();

	/**
	 * Override the target scope of this bean, specifying a new scope name.
	 * @see #SCOPE_SINGLETON
	 * @see #SCOPE_PROTOTYPE
	 */
	void setScope(@Nullable String scope);

	/**
	 * Return the name of the current target scope for this bean,
	 * or {@code null} if not known yet.
	 */
	@Nullable
	String getScope();

	/**
	 * Set whether this bean should be lazily initialized.
	 * <p>If {@code false}, the bean will get instantiated on startup by bean
	 * factories that perform eager initialization of singletons.
	 */
	void setLazyInit(boolean lazyInit);

	/**
	 * Return whether this bean should be lazily initialized, i.e. not
	 * eagerly instantiated on startup. Only applicable to a singleton bean.
	 */
	boolean isLazyInit();

	/**
	 * Set the names of the beans that this bean depends on being initialized.
	 * The bean factory will guarantee that these beans get initialized first.
	 */
	// 设置这个 bean 依赖的 bean 的名称被初始化。 bean 工厂将保证这些 bean 首先被初始化
	void setDependsOn(@Nullable String... dependsOn);

	/**
	 * Return the bean names that this bean depends on.
	 */
	// 获取当前 bean 的依赖
	@Nullable
	String[] getDependsOn();

	/**
	 * Set whether this bean is a candidate for getting autowired into some other bean.
	 * <p>Note that this flag is designed to only affect type-based autowiring.
	 * It does not affect explicit references by name, which will get resolved even
	 * if the specified bean is not marked as an autowire candidate. As a consequence,
	 * autowiring by name will nevertheless inject a bean if the name matches.
	 */
	// 设置此 bean 是否是自动装配到其他 bean 的候选者。
	// <p>请注意，此标志旨在仅影响基于类型的自动装配。它不会影响按名称的显式引用，即使指定的 bean 没有标记为自动装配候选者，也会被解析。
	// 因此，如果名称匹配，按名称自动装配，仍然会注入一个 bean。
	void setAutowireCandidate(boolean autowireCandidate);

	/**
	 * Return whether this bean is a candidate for getting autowired into some other bean.
	 */
	// 返回此 bean 是否是自动装配到其他 bean 的候选者
	boolean isAutowireCandidate();

	/**
	 * Set whether this bean is a primary autowire candidate.
	 * <p>If this value is {@code true} for exactly one bean among multiple
	 * matching candidates, it will serve as a tie-breaker.
	 */
	void setPrimary(boolean primary);

	/**
	 * Return whether this bean is a primary autowire candidate.
	 */
	boolean isPrimary();

	/**
	 * Specify the factory bean to use, if any.
	 * This the name of the bean to call the specified factory method on.
	 * @see #setFactoryMethodName
	 */
	// 指定要使用的工厂 bean（如果有）。这是要在其上调用指定工厂方法的 bean 的名称。
	void setFactoryBeanName(@Nullable String factoryBeanName);

	/**
	 * Return the factory bean name, if any.
	 */
	// 返回工厂 bean 名称（如果有）
	@Nullable
	String getFactoryBeanName();

	/**
	 * Specify a factory method, if any. This method will be invoked with
	 * constructor arguments, or with no arguments if none are specified.
	 * The method will be invoked on the specified factory bean, if any,
	 * or otherwise as a static method on the local bean class.
	 * @see #setFactoryBeanName
	 * @see #setBeanClassName
	 */
	// 指定工厂方法（如果有）。此方法将使用构造函数参数调用，如果没有指定则不使用任何参数。该方法将在指定的工厂 bean（如果有）上调用，
	// 或者作为本地 bean 类上的静态方法调用,如果是静态方法，这里就把静态信息填进来。如果是动态就先关联
	// #setFactoryBeanName(@Nullable String factoryBeanName);再把方法关联进来。
	void setFactoryMethodName(@Nullable String factoryMethodName);

	/**
	 * Return a factory method, if any.
	 */
	@Nullable
	String getFactoryMethodName();

	/**
	 * Return the constructor argument values for this bean.
	 * <p>The returned instance can be modified during bean factory post-processing.
	 * @return the ConstructorArgumentValues object (never {@code null})
	 */
	// 返回此 bean 的构造函数参数值。 <p>返回的实例可以在 bean factory 后处理期间进行修改
	ConstructorArgumentValues getConstructorArgumentValues();

	/**
	 * Return if there are constructor argument values defined for this bean.
	 * @since 5.0.2
	 */
	default boolean hasConstructorArgumentValues() {
		return !getConstructorArgumentValues().isEmpty();
	}

	/**
	 * Return the property values to be applied to a new instance of the bean.
	 * <p>The returned instance can be modified during bean factory post-processing.
	 * @return the MutablePropertyValues object (never {@code null})
	 */
	// 返回要应用于 bean 的新实例的属性值。通常来自 XML,将文本类型转换成目标类型,如果是 @Bean 注解
	// 那么属性值不需要做类型转换，直接用现成的数据来操作
	// <p>返回的实例可以在 bean factory 后处理期间进行修改
	MutablePropertyValues getPropertyValues();

	/**
	 * Return if there are property values defined for this bean.
	 * @since 5.0.2
	 */
	// 描述当前 bean 是否有属性值
	default boolean hasPropertyValues() {
		return !getPropertyValues().isEmpty();
	}

	/**
	 * Set the name of the initializer method.
	 * @since 5.1
	 */
	// 设置初始化方法名称
	void setInitMethodName(@Nullable String initMethodName);

	/**
	 * Return the name of the initializer method.
	 * @since 5.1
	 */
	@Nullable
	String getInitMethodName();

	/**
	 * Set the name of the destroy method.
	 * @since 5.1
	 */
	void setDestroyMethodName(@Nullable String destroyMethodName);

	/**
	 * Return the name of the destroy method.
	 * @since 5.1
	 */
	@Nullable
	String getDestroyMethodName();

	/**
	 * Set the role hint for this {@code BeanDefinition}. The role hint
	 * provides the frameworks as well as tools an indication of
	 * the role and importance of a particular {@code BeanDefinition}.
	 * @since 5.1
	 * @see #ROLE_APPLICATION
	 * @see #ROLE_SUPPORT
	 * @see #ROLE_INFRASTRUCTURE
	 */
	void setRole(int role);

	/**
	 * Get the role hint for this {@code BeanDefinition}. The role hint
	 * provides the frameworks as well as tools an indication of
	 * the role and importance of a particular {@code BeanDefinition}.
	 * @see #ROLE_APPLICATION
	 * @see #ROLE_SUPPORT
	 * @see #ROLE_INFRASTRUCTURE
	 */
	int getRole();

	/**
	 * Set a human-readable description of this bean definition.
	 * @since 5.1
	 */
	void setDescription(@Nullable String description);

	/**
	 * Return a human-readable description of this bean definition.
	 */
	@Nullable
	String getDescription();


	// Read-only attributes

	/**
	 * Return a resolvable type for this bean definition,
	 * based on the bean class or other specific metadata.
	 * <p>This is typically fully resolved on a runtime-merged bean definition
	 * but not necessarily on a configuration-time definition instance.
	 * @return the resolvable type (potentially {@link ResolvableType#NONE})
	 * @since 5.2
	 * @see ConfigurableBeanFactory#getMergedBeanDefinition
	 */
	// 根据 bean 类或其他特定元数据，返回此 bean 定义的可解析类型。
	// <p>这通常在运行时合并的 bean 定义上完全解决，但不一定在配置时定义实例上解决
	ResolvableType getResolvableType();

	/**
	 * Return whether this a <b>Singleton</b>, with a single, shared instance
	 * returned on all calls.
	 * @see #SCOPE_SINGLETON
	 */
	// 所有调用返回一个共享对象，与isPrototype()并不互斥，二者可以同时成立
	boolean isSingleton();

	/**
	 * Return whether this a <b>Prototype</b>, with an independent instance
	 * returned for each call.
	 * @since 3.0
	 * @see #SCOPE_PROTOTYPE
	 */
	// 每次调用返回一个独立对象，与isSingleton()并不互斥，二者可以同时成立
	boolean isPrototype();

	/**
	 * Return whether this bean is "abstract", that is, not meant to be instantiated.
	 */
	// 返回此 bean 是否“抽象”，即不打算实例化。
	boolean isAbstract();

	/**
	 * Return a description of the resource that this bean definition
	 * came from (for the purpose of showing context in case of errors).
	 */
	// 返回此 bean 定义来自的资源的描述（为了在出现错误时显示上下文）
	@Nullable
	String getResourceDescription();

	/**
	 * Return the originating BeanDefinition, or {@code null} if none.
	 * <p>Allows for retrieving the decorated bean definition, if any.
	 * <p>Note that this method returns the immediate originator. Iterate through the
	 * originator chain to find the original BeanDefinition as defined by the user.
	 */
	// 返回原始 BeanDefinition，如果没有，则返回 {@code null}。
	// <p>允许检索装饰的 bean 定义，如果有的话。 <p>请注意，此方法返回直接发起者。遍历创建者链以找到用户定义的原始 BeanDefinition。
	@Nullable
	BeanDefinition getOriginatingBeanDefinition();

}
