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

package org.springframework.beans.factory;

import org.springframework.beans.BeansException;
import org.springframework.core.ResolvableType;
import org.springframework.lang.Nullable;

/**
 * The root interface for accessing a Spring bean container.
 *
 * <p>This is the basic client view of a bean container;
 * further interfaces such as {@link ListableBeanFactory} and
 * {@link org.springframework.beans.factory.config.ConfigurableBeanFactory}
 * are available for specific purposes.
 *
 * <p>This interface is implemented by objects that hold a number of bean definitions,
 * each uniquely identified by a String name. Depending on the bean definition,
 * the factory will return either an independent instance of a contained object
 * (the Prototype design pattern), or a single shared instance (a superior
 * alternative to the Singleton design pattern, in which the instance is a
 * singleton in the scope of the factory). Which type of instance will be returned
 * depends on the bean factory configuration: the API is the same. Since Spring
 * 2.0, further scopes are available depending on the concrete application
 * context (e.g. "request" and "session" scopes in a web environment).
 *
 * <p>The point of this approach is that the BeanFactory is a central registry
 * of application components, and centralizes configuration of application
 * components (no more do individual objects need to read properties files,
 * for example). See chapters 4 and 11 of "Expert One-on-One J2EE Design and
 * Development" for a discussion of the benefits of this approach.
 *
 * <p>Note that it is generally better to rely on Dependency Injection
 * ("push" configuration) to configure application objects through setters
 * or constructors, rather than use any form of "pull" configuration like a
 * BeanFactory lookup. Spring's Dependency Injection functionality is
 * implemented using this BeanFactory interface and its subinterfaces.
 *
 * <p>Normally a BeanFactory will load bean definitions stored in a configuration
 * source (such as an XML document), and use the {@code org.springframework.beans}
 * package to configure the beans. However, an implementation could simply return
 * Java objects it creates as necessary directly in Java code. There are no
 * constraints on how the definitions could be stored: LDAP, RDBMS, XML,
 * properties file, etc. Implementations are encouraged to support references
 * amongst beans (Dependency Injection).
 *
 * <p>In contrast to the methods in {@link ListableBeanFactory}, all of the
 * operations in this interface will also check parent factories if this is a
 * {@link HierarchicalBeanFactory}. If a bean is not found in this factory instance,
 * the immediate parent factory will be asked. Beans in this factory instance
 * are supposed to override beans of the same name in any parent factory.
 *
 * <p>Bean factory implementations should support the standard bean lifecycle interfaces
 * as far as possible. The full set of initialization methods and their standard order is:
 * <ol>
 * <li>BeanNameAware's {@code setBeanName}
 * <li>BeanClassLoaderAware's {@code setBeanClassLoader}
 * <li>BeanFactoryAware's {@code setBeanFactory}
 * <li>EnvironmentAware's {@code setEnvironment}
 * <li>EmbeddedValueResolverAware's {@code setEmbeddedValueResolver}
 * <li>ResourceLoaderAware's {@code setResourceLoader}
 * (only applicable when running in an application context)
 * <li>ApplicationEventPublisherAware's {@code setApplicationEventPublisher}
 * (only applicable when running in an application context)
 * <li>MessageSourceAware's {@code setMessageSource}
 * (only applicable when running in an application context)
 * <li>ApplicationContextAware's {@code setApplicationContext}
 * (only applicable when running in an application context)
 * <li>ServletContextAware's {@code setServletContext}
 * (only applicable when running in a web application context)
 * <li>{@code postProcessBeforeInitialization} methods of BeanPostProcessors
 * <li>InitializingBean's {@code afterPropertiesSet}
 * <li>a custom {@code init-method} definition
 * <li>{@code postProcessAfterInitialization} methods of BeanPostProcessors
 * </ol>
 *
 * <p>On shutdown of a bean factory, the following lifecycle methods apply:
 * <ol>
 * <li>{@code postProcessBeforeDestruction} methods of DestructionAwareBeanPostProcessors
 * <li>DisposableBean's {@code destroy}
 * <li>a custom {@code destroy-method} definition
 * </ol>
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Chris Beams
 * @since 13 April 2001
 * @see BeanNameAware#setBeanName
 * @see BeanClassLoaderAware#setBeanClassLoader
 * @see BeanFactoryAware#setBeanFactory
 * @see org.springframework.context.EnvironmentAware#setEnvironment
 * @see org.springframework.context.EmbeddedValueResolverAware#setEmbeddedValueResolver
 * @see org.springframework.context.ResourceLoaderAware#setResourceLoader
 * @see org.springframework.context.ApplicationEventPublisherAware#setApplicationEventPublisher
 * @see org.springframework.context.MessageSourceAware#setMessageSource
 * @see org.springframework.context.ApplicationContextAware#setApplicationContext
 * @see org.springframework.web.context.ServletContextAware#setServletContext
 * @see org.springframework.beans.factory.config.BeanPostProcessor#postProcessBeforeInitialization
 * @see InitializingBean#afterPropertiesSet
 * @see org.springframework.beans.factory.support.RootBeanDefinition#getInitMethodName
 * @see org.springframework.beans.factory.config.BeanPostProcessor#postProcessAfterInitialization
 * @see org.springframework.beans.factory.config.DestructionAwareBeanPostProcessor#postProcessBeforeDestruction
 * @see DisposableBean#destroy
 * @see org.springframework.beans.factory.support.RootBeanDefinition#getDestroyMethodName
 */
// 访问 Spring bean 容器的根接口
//
// 这是 bean 容器的基本客户端视图； 其他接口如 ListableBeanFactory 和
// org.springframework.beans.factory.config.ConfigurableBeanFactory 用于特定目的
//
// 该接口由包含许多 bean 定义的对象实现，每个 bean 定义由一个 String 名称唯一标识。
// 根据 bean 定义，工厂将返回包含对象的独立实例（原型设计模式），或单个共享实例（单例设计模式的高级替代方案，
// 其中实例是工厂范围内的单例））。 返回哪种类型的实例取决于 bean factory 配置：API 是相同的。
// 从 Spring 2.0 开始，根据具体的应用程序上下文（例如 Web 环境中的 "request" and "session" 作用域），可以使用更多范围。
//
// 重点是 BeanFactory 是应用程序组件的中央注册表，并且集中了应用程序组件的配置（例如，单个对象不再需要读取属性文件）。
// 有关这种方法的好处的讨论，请参阅 "Expert One-on-One J2EE Design and Development"（“专家一对一 J2EE 设计和开发”）
// 的第 4 章和第 11 章
//
// 请注意，通常最好使用依赖注入("push" configuration)，通过 setter 或构造函数来配置应用程序对象，而不是使用
// 任何形式的 "pull" configuration，例如 BeanFactory 依赖查找。 Spring 的依赖注入功能是
// 使用 BeanFactory 接口及其子接口实现的
//
// 通常 BeanFactory 将加载存储在配置源（例如 XML 文档）中的 bean 定义，并使用 org.springframework.beans 包来配置 bean。
// 但是，实现可以简单地直接在 Java 代码中返回它根据需要创建的 Java 对象。定义的存储方式没有限制：LDAP、RDBMS、XML、属性文件等。
// 鼓励实现支持 bean 之间的引用（依赖注入）
//
// 与 ListableBeanFactory 中的方法相反，此接口中的所有操作还将检查父工厂是否为 HierarchicalBeanFactory。如果在此工厂实例
// 中找不到 bean，则会询问直接的父工厂。这个工厂实例中的 bean 应该覆盖任何父工厂中的同名 bean
//
// Bean 工厂实现应尽可能支持标准的 Bean 生命周期接口。全套初始化方法及其标准顺序是：
// 1.BeanNameAware's #setBeanName
// 2.BeanClassLoaderAware's #setBeanClassLoader
// 3.BeanFactoryAware's #setBeanFactory
// 4.EnvironmentAware's #setEnvironment
// 5.EmbeddedValueResolverAware's #setEmbeddedValueResolver
// 6.ResourceLoaderAware's #setResourceLoader
// (only applicable when running in an application context 仅在应用程序上下文中运行时适用)
// 7.ApplicationEventPublisherAware's #setApplicationEventPublisher
// (only applicable when running in an application context 仅在应用程序上下文中运行时适用)
// 8.MessageSourceAware's #setMessageSource
// (only applicable when running in an application context 仅在应用程序上下文中运行时适用)
// 9.ApplicationContextAware's #setApplicationContext
// (only applicable when running in an application context 仅在应用程序上下文中运行时适用)
// 10.ServletContextAware's #setServletContext
// (only applicable when running in a web application context 仅在web应用程序上下文中运行时适用)
// 11.postProcessBeforeInitialization methods of BeanPostProcessors
// 12.InitializingBean's afterPropertiesSet
// 13.a custom init-method definition
// 14.postProcessAfterInitialization methods of BeanPostProcessors
// 在关闭 bean 工厂时，以下生命周期方法适用：
// 1.postProcessBeforeDestruction methods of DestructionAwareBeanPostProcessors
// 2.DisposableBean's destroy
// 3.a custom destroy-method definition
public interface BeanFactory {

	/**
	 * Used to dereference a {@link FactoryBean} instance and distinguish it from
	 * beans <i>created</i> by the FactoryBean. For example, if the bean named
	 * {@code myJndiObject} is a FactoryBean, getting {@code &myJndiObject}
	 * will return the factory, not the instance returned by the factory.
	 */
	// 用于取消引用 {@link FactoryBean} 实例并将其与 FactoryBean <i>created<i> 的 bean 区分开来。例如，
	//如果名为 {@code myJndiObject} 的 bean 是 FactoryBean，则获取 {@code &myJndiObject} 将返回工厂，而不是工厂返回的实例。
	String FACTORY_BEAN_PREFIX = "&";


	/**
	 * Return an instance, which may be shared or independent, of the specified bean.
	 * <p>This method allows a Spring BeanFactory to be used as a replacement for the
	 * Singleton or Prototype design pattern. Callers may retain references to
	 * returned objects in the case of Singleton beans.
	 * <p>Translates aliases back to the corresponding canonical bean name.
	 * <p>Will ask the parent factory if the bean cannot be found in this factory instance.
	 * @param name the name of the bean to retrieve
	 * @return an instance of the bean
	 * @throws NoSuchBeanDefinitionException if there is no bean with the specified name
	 * @throws BeansException if the bean could not be obtained
	 */
	// 返回指定 bean 的一个实例，该实例可以是共享的，也可以是独立的。
	// <p>此方法允许使用 Spring BeanFactory 作为单例或原型设计模式的替代品。在单例 bean 的情况下，调用者可以保留对返回对象的引用。
	// <p>将别名翻译回相应的规范 bean 名称。 <p>如果在这个工厂实例中找不到bean，会询问父工厂。
	// @param name 要检索的 bean 的名称 @return bean 的一个实例
	// @throws NoSuchBeanDefinitionException 如果没有指定名称的 bean
	// @throws BeansException 如果无法获得 bean
	Object getBean(String name) throws BeansException;

	/**
	 * Return an instance, which may be shared or independent, of the specified bean.
	 * <p>Behaves the same as {@link #getBean(String)}, but provides a measure of type
	 * safety by throwing a BeanNotOfRequiredTypeException if the bean is not of the
	 * required type. This means that ClassCastException can't be thrown on casting
	 * the result correctly, as can happen with {@link #getBean(String)}.
	 * <p>Translates aliases back to the corresponding canonical bean name.
	 * <p>Will ask the parent factory if the bean cannot be found in this factory instance.
	 * @param name the name of the bean to retrieve
	 * @param requiredType type the bean must match; can be an interface or superclass
	 * @return an instance of the bean
	 * @throws NoSuchBeanDefinitionException if there is no such bean definition
	 * @throws BeanNotOfRequiredTypeException if the bean is not of the required type
	 * @throws BeansException if the bean could not be created
	 */
	// 返回指定 bean 的一个实例，该实例可以是共享的，也可以是独立的。
	//
	// 行为与getBean(String)相同，但如果 bean 不属于所需类型，则通过
	// 抛出 BeanNotOfRequiredTypeException 来提供类型安全度量。 这意味着无法
	// 在正确转换结果时抛出 ClassCastException ，就像getBean(String) 。
	//
	// 将别名转换回相应的规范 bean 名称。
	//
	// 如果在此工厂实例中找不到 bean，将询问父工厂。
	// 参形：
	//			name - 要检索的 bean 的名称
	//			requiredType – bean 必须匹配的类型； 可以是接口或超类，
	//			如果 name 是 Factory bean 的名称，则 当前类型可以是通过工厂查询的其他 bean 的类型
	// 返回值：
	//			bean 的一个实例
	// 抛出：
	//			NoSuchBeanDefinitionException – 如果没有这样的 bean 定义
	//			BeanNotOfRequiredTypeException – 如果 bean 不是所需的类型
	//			BeansException – 如果无法创建 bean
	//
	// 在指定 name 的 bean 中(一般作为引导bean，
	// containing bean)即工厂中,查找已注入的 requiredType 类型的 bean
	<T> T getBean(String name, Class<T> requiredType) throws BeansException;

	/**
	 * Return an instance, which may be shared or independent, of the specified bean.
	 * <p>Allows for specifying explicit constructor arguments / factory method arguments,
	 * overriding the specified default arguments (if any) in the bean definition.
	 * @param name the name of the bean to retrieve
	 * @param args arguments to use when creating a bean instance using explicit arguments
	 * (only applied when creating a new instance as opposed to retrieving an existing one)
	 * @return an instance of the bean
	 * @throws NoSuchBeanDefinitionException if there is no such bean definition
	 * @throws BeanDefinitionStoreException if arguments have been given but
	 * the affected bean isn't a prototype
	 * @throws BeansException if the bean could not be created
	 * @since 2.5
	 */
	Object getBean(String name, Object... args) throws BeansException;

	/**
	 * Return the bean instance that uniquely matches the given object type, if any.
	 * <p>This method goes into {@link ListableBeanFactory} by-type lookup territory
	 * but may also be translated into a conventional by-name lookup based on the name
	 * of the given type. For more extensive retrieval operations across sets of beans,
	 * use {@link ListableBeanFactory} and/or {@link BeanFactoryUtils}.
	 * @param requiredType type the bean must match; can be an interface or superclass
	 * @return an instance of the single bean matching the required type
	 * @throws NoSuchBeanDefinitionException if no bean of the given type was found
	 * @throws NoUniqueBeanDefinitionException if more than one bean of the given type was found
	 * @throws BeansException if the bean could not be created
	 * @since 3.0
	 * @see ListableBeanFactory
	 */
	// 返回与给定对象类型唯一匹配的 bean 实例（如果有）
	//
	// 此方法进入ListableBeanFactory按类型查找领域，但也可以转换为基于给定类型名称的传统按名称查找。
	// 对于跨 bean 集的更广泛的检索操作，请使用ListableBeanFactory和/或BeanFactoryUtils
	//
	// 形参：requiredType – bean 必须匹配的类型； 可以是接口或超类
	// 返回值：匹配所需类型的单个 bean 的实例
	<T> T getBean(Class<T> requiredType) throws BeansException;

	/**
	 * Return an instance, which may be shared or independent, of the specified bean.
	 * <p>Allows for specifying explicit constructor arguments / factory method arguments,
	 * overriding the specified default arguments (if any) in the bean definition.
	 * <p>This method goes into {@link ListableBeanFactory} by-type lookup territory
	 * but may also be translated into a conventional by-name lookup based on the name
	 * of the given type. For more extensive retrieval operations across sets of beans,
	 * use {@link ListableBeanFactory} and/or {@link BeanFactoryUtils}.
	 * @param requiredType type the bean must match; can be an interface or superclass
	 * @param args arguments to use when creating a bean instance using explicit arguments
	 * (only applied when creating a new instance as opposed to retrieving an existing one)
	 * @return an instance of the bean
	 * @throws NoSuchBeanDefinitionException if there is no such bean definition
	 * @throws BeanDefinitionStoreException if arguments have been given but
	 * the affected bean isn't a prototype
	 * @throws BeansException if the bean could not be created
	 * @since 4.1
	 */
	// 依赖查找：传递构造方法的参数和查找类型来查找 bean,这里的参数会触底给下边类的@Nullable Object[] args 来创建bean
	// {@link AbstractAutowireCapableBeanFactory}
	// #doCreateBean(String,RootBeanDefinition, @Nullable Object[] args)
	<T> T getBean(Class<T> requiredType, Object... args) throws BeansException;

	/**
	 * Return a provider for the specified bean, allowing for lazy on-demand retrieval
	 * of instances, including availability and uniqueness options.
	 * @param requiredType type the bean must match; can be an interface or superclass
	 * @return a corresponding provider handle
	 * @since 5.1
	 * @see #getBeanProvider(ResolvableType)
	 */
	// 返回指定 bean 的提供者，允许延迟按需检索实例，包括可用性和唯一性选项。
	//形参：requiredType – bean 必须匹配的类型； 可以是接口或超类
	<T> ObjectProvider<T> getBeanProvider(Class<T> requiredType);

	/**
	 * Return a provider for the specified bean, allowing for lazy on-demand retrieval
	 * of instances, including availability and uniqueness options.
	 * @param requiredType type the bean must match; can be a generic type declaration.
	 * Note that collection types are not supported here, in contrast to reflective
	 * injection points. For programmatically retrieving a list of beans matching a
	 * specific type, specify the actual bean type as an argument here and subsequently
	 * use {@link ObjectProvider#orderedStream()} or its lazy streaming/iteration options.
	 * @return a corresponding provider handle
	 * @since 5.1
	 * @see ObjectProvider#iterator()
	 * @see ObjectProvider#stream()
	 * @see ObjectProvider#orderedStream()
	 */
	// 返回指定 bean 的提供者，允许延迟按需检索实例，包括可用性和唯一性选项
	// @param requiredType bean 必须匹配的类型； 可以是泛型类型声明。
	// 请注意，与反射注入点相比，此处不支持集合类型。 要以编程方式检索与特定类型匹配的 bean 列表，
	// 请在此处指定实际 bean 类型作为参数，然后使用ObjectProvider.orderedStream()或其延迟流/迭代选项。
	<T> ObjectProvider<T> getBeanProvider(ResolvableType requiredType);

	/**
	 * Does this bean factory contain a bean definition or externally registered singleton
	 * instance with the given name?
	 * <p>If the given name is an alias, it will be translated back to the corresponding
	 * canonical bean name.
	 * <p>If this factory is hierarchical, will ask any parent factory if the bean cannot
	 * be found in this factory instance.
	 * <p>If a bean definition or singleton instance matching the given name is found,
	 * this method will return {@code true} whether the named bean definition is concrete
	 * or abstract, lazy or eager, in scope or not. Therefore, note that a {@code true}
	 * return value from this method does not necessarily indicate that {@link #getBean}
	 * will be able to obtain an instance for the same name.
	 * @param name the name of the bean to query
	 * @return whether a bean with the given name is present
	 */
	// 这个 bean 工厂是否包含具有给定名称的 bean 定义或外部注册的单例实例？
	//
	//如果给定的名称是别名，它将被转换回相应的规范 bean 名称。
	//
	//如果这个工厂是分层的，如果在这个工厂实例中找不到 bean，将询问任何父工厂。
	//
	//如果找到与给定名称匹配的 bean 定义或单例实例，则无论命名的 bean 定义是具体的还是抽象的、惰性的还是急切的、
	// 范围内的，此方法都将返回true 。 因此，请注意，此方法的true返回值并不一定表示getBean将能够获得同名的实例
	boolean containsBean(String name);

	/**
	 * Is this bean a shared singleton? That is, will {@link #getBean} always
	 * return the same instance?
	 * <p>Note: This method returning {@code false} does not clearly indicate
	 * independent instances. It indicates non-singleton instances, which may correspond
	 * to a scoped bean as well. Use the {@link #isPrototype} operation to explicitly
	 * check for independent instances.
	 * <p>Translates aliases back to the corresponding canonical bean name.
	 * <p>Will ask the parent factory if the bean cannot be found in this factory instance.
	 * @param name the name of the bean to query
	 * @return whether this bean corresponds to a singleton instance
	 * @throws NoSuchBeanDefinitionException if there is no bean with the given name
	 * @see #getBean
	 * @see #isPrototype
	 */
	// 这个 bean 是共享单例吗？ 也就是说， getBean总是返回相同的实例吗？
	// 注意：此方法返回false并没有明确表示独立实例。 它表示非单例实例，也可能对应于作用域 bean。
	// 使用isPrototype操作显式检查独立实例。
	// 将别名转换回相应的规范 bean 名称。
	// 如果在这个工厂实例中找不到 bean，将询问父工厂。
	boolean isSingleton(String name) throws NoSuchBeanDefinitionException;

	/**
	 * Is this bean a prototype? That is, will {@link #getBean} always return
	 * independent instances?
	 * <p>Note: This method returning {@code false} does not clearly indicate
	 * a singleton object. It indicates non-independent instances, which may correspond
	 * to a scoped bean as well. Use the {@link #isSingleton} operation to explicitly
	 * check for a shared singleton instance.
	 * <p>Translates aliases back to the corresponding canonical bean name.
	 * <p>Will ask the parent factory if the bean cannot be found in this factory instance.
	 * @param name the name of the bean to query
	 * @return whether this bean will always deliver independent instances
	 * @throws NoSuchBeanDefinitionException if there is no bean with the given name
	 * @since 2.0.3
	 * @see #getBean
	 * @see #isSingleton
	 */
	// 这个bean是原型吗？ 也就是说， getBean一直返回独立的实例吗？
	//
	// 注意：此方法返回false并没有明确表示单例对象。 它表示非独立实例，也可能对应于作用域 bean。
	// 使用isSingleton操作显式检查共享单例实例。
	//
	// 将别名转换回相应的规范 bean 名称。
	// 如果在这个工厂实例中找不到 bean，将询问父工厂。
	boolean isPrototype(String name) throws NoSuchBeanDefinitionException;

	/**
	 * Check whether the bean with the given name matches the specified type.
	 * More specifically, check whether a {@link #getBean} call for the given name
	 * would return an object that is assignable to the specified target type.
	 * <p>Translates aliases back to the corresponding canonical bean name.
	 * <p>Will ask the parent factory if the bean cannot be found in this factory instance.
	 * @param name the name of the bean to query
	 * @param typeToMatch the type to match against (as a {@code ResolvableType})
	 * @return {@code true} if the bean type matches,
	 * {@code false} if it doesn't match or cannot be determined yet
	 * @throws NoSuchBeanDefinitionException if there is no bean with the given name
	 * @since 4.2
	 * @see #getBean
	 * @see #getType
	 */
	// 检查具有给定名称的 bean 是否与指定的类型匹配。 更具体地说，检查对给定名称的 getBean 调用
	// 是否会返回可分配给指定目标类型的对象。
	//
	// 将别名转换回相应的规范 bean 名称
	//
	// 如果在这个工厂实例中找不到 bean，将询问父工厂
	boolean isTypeMatch(String name, ResolvableType typeToMatch) throws NoSuchBeanDefinitionException;

	/**
	 * Check whether the bean with the given name matches the specified type.
	 * More specifically, check whether a {@link #getBean} call for the given name
	 * would return an object that is assignable to the specified target type.
	 * <p>Translates aliases back to the corresponding canonical bean name.
	 * <p>Will ask the parent factory if the bean cannot be found in this factory instance.
	 * @param name the name of the bean to query
	 * @param typeToMatch the type to match against (as a {@code Class})
	 * @return {@code true} if the bean type matches,
	 * {@code false} if it doesn't match or cannot be determined yet
	 * @throws NoSuchBeanDefinitionException if there is no bean with the given name
	 * @since 2.0.1
	 * @see #getBean
	 * @see #getType
	 */
	// 检查具有给定名称的 bean 是否与指定的类型匹配。 更具体地说，检查对给定名称的getBean调用是否会返回可分配给指定目标类型的对象。
	// 将别名转换回相应的规范 bean 名称。
	// 如果在这个工厂实例中找不到 bean，将询问父工厂。
	boolean isTypeMatch(String name, Class<?> typeToMatch) throws NoSuchBeanDefinitionException;

	/**
	 * Determine the type of the bean with the given name. More specifically,
	 * determine the type of object that {@link #getBean} would return for the given name.
	 * <p>For a {@link FactoryBean}, return the type of object that the FactoryBean creates,
	 * as exposed by {@link FactoryBean#getObjectType()}. This may lead to the initialization
	 * of a previously uninitialized {@code FactoryBean} (see {@link #getType(String, boolean)}).
	 * <p>Translates aliases back to the corresponding canonical bean name.
	 * <p>Will ask the parent factory if the bean cannot be found in this factory instance.
	 * @param name the name of the bean to query
	 * @return the type of the bean, or {@code null} if not determinable
	 * @throws NoSuchBeanDefinitionException if there is no bean with the given name
	 * @since 1.1.2
	 * @see #getBean
	 * @see #isTypeMatch
	 */
	// 确定具有给定名称的 bean 的类型。 更具体地说，确定getBean将为给定名称返回的对象类型。
	// 对于FactoryBean ，返回由FactoryBean.getObjectType()公开的 FactoryBean 创建的对象类型。
	// 这可能会导致之前未初始化的FactoryBean初始化（请参阅getType(String, boolean) ）。
	// 将别名转换回相应的规范 bean 名称
	// 如果在这个工厂实例中找不到 bean，将询问父工厂
	@Nullable
	Class<?> getType(String name) throws NoSuchBeanDefinitionException;

	/**
	 * Determine the type of the bean with the given name. More specifically,
	 * determine the type of object that {@link #getBean} would return for the given name.
	 * <p>For a {@link FactoryBean}, return the type of object that the FactoryBean creates,
	 * as exposed by {@link FactoryBean#getObjectType()}. Depending on the
	 * {@code allowFactoryBeanInit} flag, this may lead to the initialization of a previously
	 * uninitialized {@code FactoryBean} if no early type information is available.
	 * <p>Translates aliases back to the corresponding canonical bean name.
	 * <p>Will ask the parent factory if the bean cannot be found in this factory instance.
	 * @param name the name of the bean to query
	 * @param allowFactoryBeanInit whether a {@code FactoryBean} may get initialized
	 * just for the purpose of determining its object type
	 * @return the type of the bean, or {@code null} if not determinable
	 * @throws NoSuchBeanDefinitionException if there is no bean with the given name
	 * @since 5.2
	 * @see #getBean
	 * @see #isTypeMatch
	 */
	// 确定具有给定名称的 bean 的类型。 更具体地说，确定getBean将为给定名称返回的对象类型。
	//
	// 对于FactoryBean ，返回由FactoryBean.getObjectType()公开的 FactoryBean 创建的对象类型。
	// 根据allowFactoryBeanInit标志，如果没有可用的早期类型信息，这可能会导致先前未初始化的FactoryBean的初始化
	//
	// 将别名转换回相应的规范 bean 名称
	//
	// 如果在这个工厂实例中找不到 bean，将询问父工厂
	@Nullable
	Class<?> getType(String name, boolean allowFactoryBeanInit) throws NoSuchBeanDefinitionException;

	/**
	 * Return the aliases for the given bean name, if any.
	 * <p>All of those aliases point to the same bean when used in a {@link #getBean} call.
	 * <p>If the given name is an alias, the corresponding original bean name
	 * and other aliases (if any) will be returned, with the original bean name
	 * being the first element in the array.
	 * <p>Will ask the parent factory if the bean cannot be found in this factory instance.
	 * @param name the bean name to check for aliases
	 * @return the aliases, or an empty array if none
	 * @see #getBean
	 */
	// 返回给定 bean 名称的别名（如果有）
	// 在 getBean() 调用中使用时，所有这些别名都指向同一个 bean。
	// 如果给定的名称是别名，则将返回相应的原始 bean 名称和其他别名（如果有），原始 bean 名称是数组中的第一个元素。
	// 如果在这个工厂实例中找不到 bean，将询问父工厂
	String[] getAliases(String name);

}
