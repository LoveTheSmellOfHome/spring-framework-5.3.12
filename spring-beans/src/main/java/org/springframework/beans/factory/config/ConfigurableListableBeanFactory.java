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

package org.springframework.beans.factory.config;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.lang.Nullable;

import java.util.Iterator;

/**
 * Configuration interface to be implemented by most listable bean factories.
 * In addition to {@link ConfigurableBeanFactory}, it provides facilities to
 * analyze and modify bean definitions, and to pre-instantiate singletons.
 *
 * <p>This subinterface of {@link org.springframework.beans.factory.BeanFactory}
 * is not meant to be used in normal application code: Stick to
 * {@link org.springframework.beans.factory.BeanFactory} or
 * {@link org.springframework.beans.factory.ListableBeanFactory} for typical
 * use cases. This interface is just meant to allow for framework-internal
 * plug'n'play even when needing access to bean factory configuration methods.
 *
 * @author Juergen Hoeller
 * @since 03.11.2003
 * @see org.springframework.context.support.AbstractApplicationContext#getBeanFactory()
 */
// 大多数可列出的bean工厂要实现的配置接口。除了ConfigurableBeanFactory之外，它还提供了
// 分析和修改 bean 定义以及预实例化单例的工具。
//
//org.springframework.beans.factory.BeanFactory的这个子接口并不意味着在正常的应用程序代码中使用：对于典型用例，
// 请坚持使用org.springframework.beans.factory.BeanFactory 或 ListableBeanFactory 。这个接口只是为了允许
// 框架内部的即插即用，即使在需要访问 bean 工厂配置方法时也是如此。
//
// 提供框架内部可写的 ListableBeanFactory
public interface ConfigurableListableBeanFactory
		extends ListableBeanFactory, AutowireCapableBeanFactory, ConfigurableBeanFactory {

	/**
	 * Ignore the given dependency type for autowiring:
	 * for example, String. Default is none.
	 * @param type the dependency type to ignore
	 */
	// 忽略给定的自动装配依赖类型：例如，字符串。默认为无
	void ignoreDependencyType(Class<?> type);

	/**
	 * Ignore the given dependency interface for autowiring.
	 * <p>This will typically be used by application contexts to register
	 * dependencies that are resolved in other ways, like BeanFactory through
	 * BeanFactoryAware or ApplicationContext through ApplicationContextAware.
	 * <p>By default, only the BeanFactoryAware interface is ignored.
	 * For further types to ignore, invoke this method for each type.
	 * @param ifc the dependency interface to ignore
	 * @see org.springframework.beans.factory.BeanFactoryAware
	 * @see org.springframework.context.ApplicationContextAware
	 */
	// 忽略给定的自动装配依赖接口。
	// <p>这通常由应用程序上下文用于注册以其他方式解析的依赖项，例如通过 BeanFactoryAware 的 BeanFactory 或通
	// 过 ApplicationContextAware 的 ApplicationContext。
	// <p>默认情况下，仅忽略 BeanFactoryAware 接口。对于要忽略的其他类型，请为每种类型调用此方法。
	// @param ifc 忽略的依赖接口 @see org.springframework.beans.factory.BeanFactoryAware
	// @see org.springframework.context.ApplicationContextAware
	void ignoreDependencyInterface(Class<?> ifc);

	/**
	 * Register a special dependency type with corresponding autowired value.
	 * <p>This is intended for factory/context references that are supposed
	 * to be autowirable but are not defined as beans in the factory:
	 * e.g. a dependency of type ApplicationContext resolved to the
	 * ApplicationContext instance that the bean is living in.
	 * <p>Note: There are no such default types registered in a plain BeanFactory,
	 * not even for the BeanFactory interface itself.
	 * @param dependencyType the dependency type to register. This will typically
	 * be a base interface such as BeanFactory, with extensions of it resolved
	 * as well if declared as an autowiring dependency (e.g. ListableBeanFactory),
	 * as long as the given value actually implements the extended interface.
	 * @param autowiredValue the corresponding autowired value. This may also be an
	 * implementation of the {@link org.springframework.beans.factory.ObjectFactory}
	 * interface, which allows for lazy resolution of the actual target value.
	 */
	// 使用相应的自动装配值注册一个特殊的依赖类型。
	// <p>这适用于应该是可自动装配但在工厂中未定义为 bean 的 factorycontext 引用：例如ApplicationContext 类型的依赖项解析
	// 为 bean 所在的 ApplicationContext 实例。
	// <p>注意：在普通 BeanFactory 中没有注册这样的默认类型，甚至对于 BeanFactory 接口本身也没有。
	// @param dependencyType 要注册的依赖类型。这通常是一个基本接口，例如 BeanFactory，如果声明为自动装配依
	// 赖项（例如 ListableBeanFactory），它的扩展也会被解析，只要给定的值实际上实现了扩展接口。
	// @param autowiredValue 相应的自动装配值。这也可能是 {@link org.springframework.beans.factory.ObjectFactory} 接口的实现，
	// 它允许延迟解析实际目标值。
	void registerResolvableDependency(Class<?> dependencyType, @Nullable Object autowiredValue);

	/**
	 * Determine whether the specified bean qualifies as an autowire candidate,
	 * to be injected into other beans which declare a dependency of matching type.
	 * <p>This method checks ancestor factories as well.
	 * @param beanName the name of the bean to check
	 * @param descriptor the descriptor of the dependency to resolve
	 * @return whether the bean should be considered as autowire candidate
	 * @throws NoSuchBeanDefinitionException if there is no bean with the given name
	 */
	// 确定指定的 bean 是否有资格作为自动装配候选者，以注入到声明匹配类型依赖项的其他 bean 中。
	// <p>这个方法也检查祖先工厂。
	boolean isAutowireCandidate(String beanName, DependencyDescriptor descriptor)
			throws NoSuchBeanDefinitionException;

	/**
	 * Return the registered BeanDefinition for the specified bean, allowing access
	 * to its property values and constructor argument value (which can be
	 * modified during bean factory post-processing).
	 * <p>A returned BeanDefinition object should not be a copy but the original
	 * definition object as registered in the factory. This means that it should
	 * be castable to a more specific implementation type, if necessary.
	 * <p><b>NOTE:</b> This method does <i>not</i> consider ancestor factories.
	 * It is only meant for accessing local bean definitions of this factory.
	 * @param beanName the name of the bean
	 * @return the registered BeanDefinition
	 * @throws NoSuchBeanDefinitionException if there is no bean with the given name
	 * defined in this factory
	 */
	// 返回指定 bean 的注册 BeanDefinition，允许访问其属性值和构造函数参数值（可以在 bean factory 后处理期间修改）。
	// <p>返回的 BeanDefinition 对象不应是副本，而是在工厂中注册的原始定义对象。这意味着，如有必要，它应该可以转换为更具体的实现类型。
	// <p><b>注意：<b>此方法<i>不<i>考虑祖先工厂。它仅用于访问此工厂的本地 bean 定义。
	BeanDefinition getBeanDefinition(String beanName) throws NoSuchBeanDefinitionException;

	/**
	 * Return a unified view over all bean names managed by this factory.
	 * <p>Includes bean definition names as well as names of manually registered
	 * singleton instances, with bean definition names consistently coming first,
	 * analogous to how type/annotation specific retrieval of bean names works.
	 * @return the composite iterator for the bean names view
	 * @since 4.1.2
	 * @see #containsBeanDefinition
	 * @see #registerSingleton
	 * @see #getBeanNamesForType
	 * @see #getBeanNamesForAnnotation
	 */
	// 返回此工厂管理的所有 bean 名称的统一视图。
	// <p> 包括 bean 定义名称以及手动注册的单例实例的名称，bean 定义名称始终排在最前面，类似于 bean 名称的类型注释特定检索的工作原理
	Iterator<String> getBeanNamesIterator();

	/**
	 * Clear the merged bean definition cache, removing entries for beans
	 * which are not considered eligible for full metadata caching yet.
	 * <p>Typically triggered after changes to the original bean definitions,
	 * e.g. after applying a {@link BeanFactoryPostProcessor}. Note that metadata
	 * for beans which have already been created at this point will be kept around.
	 * @since 4.2
	 * @see #getBeanDefinition
	 * @see #getMergedBeanDefinition
	 */
	// 清除合并的 bean 定义缓存，删除认为尚不符合完整元数据缓存条件的 bean 的条目。
	// <p>通常在更改原始 bean 定义后触发，例如应用 {@link BeanFactoryPostProcessor} 后。
	// 请注意，此时已创建的 bean 的元数据将保留
	void clearMetadataCache();

	/**
	 * Freeze all bean definitions, signalling that the registered bean definitions
	 * will not be modified or post-processed any further.
	 * <p>This allows the factory to aggressively cache bean definition metadata.
	 */
	// 冻结所有 bean 定义，表明注册的 bean 定义不会被进一步修改或后处理。
	// <p>这允许工厂积极缓存 bean 定义元数据
	void freezeConfiguration();

	/**
	 * Return whether this factory's bean definitions are frozen,
	 * i.e. are not supposed to be modified or post-processed any further.
	 * @return {@code true} if the factory's configuration is considered frozen
	 */
	// 返回此工厂的 bean 定义是否被冻结，即不应进一步修改或后处理。
	boolean isConfigurationFrozen();

	/**
	 * Ensure that all non-lazy-init singletons are instantiated, also considering
	 * {@link org.springframework.beans.factory.FactoryBean FactoryBeans}.
	 * Typically invoked at the end of factory setup, if desired.
	 * @throws BeansException if one of the singleton beans could not be created.
	 * Note: This may have left the factory with some beans already initialized!
	 * Call {@link #destroySingletons()} for full cleanup in this case.
	 * @see #destroySingletons()
	 */
	// 确保所有非延迟初始化单例都被实例化，同时考虑 {@link org.springframework.beans.factory.FactoryBean FactoryBeans}。
	// 如果需要，通常在工厂设置结束时调用。
	void preInstantiateSingletons() throws BeansException;

}
