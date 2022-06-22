/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.context.annotation;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.core.type.AnnotationMetadata;

/**
 * Interface to be implemented by types that register additional bean definitions when
 * processing @{@link Configuration} classes. Useful when operating at the bean definition
 * level (as opposed to {@code @Bean} method/instance level) is desired or necessary.
 *
 * <p>Along with {@code @Configuration} and {@link ImportSelector}, classes of this type
 * may be provided to the @{@link Import} annotation (or may also be returned from an
 * {@code ImportSelector}).
 *
 * <p>An {@link ImportBeanDefinitionRegistrar} may implement any of the following
 * {@link org.springframework.beans.factory.Aware Aware} interfaces, and their respective
 * methods will be called prior to {@link #registerBeanDefinitions}:
 * <ul>
 * <li>{@link org.springframework.context.EnvironmentAware EnvironmentAware}</li>
 * <li>{@link org.springframework.beans.factory.BeanFactoryAware BeanFactoryAware}
 * <li>{@link org.springframework.beans.factory.BeanClassLoaderAware BeanClassLoaderAware}
 * <li>{@link org.springframework.context.ResourceLoaderAware ResourceLoaderAware}
 * </ul>
 *
 * <p>Alternatively, the class may provide a single constructor with one or more of
 * the following supported parameter types:
 * <ul>
 * <li>{@link org.springframework.core.env.Environment Environment}</li>
 * <li>{@link org.springframework.beans.factory.BeanFactory BeanFactory}</li>
 * <li>{@link java.lang.ClassLoader ClassLoader}</li>
 * <li>{@link org.springframework.core.io.ResourceLoader ResourceLoader}</li>
 * </ul>
 *
 * <p>See implementations and associated unit tests for usage examples.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 3.1
 * @see Import
 * @see ImportSelector
 * @see Configuration
 */
// 在处理@Configuration 类时，由注册附加bean 定义的类型实现的Configuration 。在需要或必须在 bean 定义
// 级别（相对于@Bean方法/实例级别）操作时很有用。
//
// 与 @Configuration 和 ImportSelector 一起，这种类型的类可以提供给 @Import注解（或者也可以从 ImportSelector 返回）。
//
// ImportBeanDefinitionRegistrar可以实现以下任何Aware接口，它们各自的方法将在 registerBeanDefinitions 之前调用：
//  >EnvironmentAware
//  >BeanFactoryAware
//  >BeanClassLoaderAware
//  >ResourceLoaderAware
// 或者，该类可以提供具有以下一种或多种支持的参数类型的单个构造函数：
//  >Environment
//  >BeanFactory
//  >ClassLoader
//  >ResourceLoader
// Spring 自动代理 @EnableAspectJAutoProxy 的实现原理
public interface ImportBeanDefinitionRegistrar {

	/**
	 * Register bean definitions as necessary based on the given annotation metadata of
	 * the importing {@code @Configuration} class.
	 * <p>Note that {@link BeanDefinitionRegistryPostProcessor} types may <em>not</em> be
	 * registered here, due to lifecycle constraints related to {@code @Configuration}
	 * class processing.
	 * <p>The default implementation delegates to
	 * {@link #registerBeanDefinitions(AnnotationMetadata, BeanDefinitionRegistry)}.
	 * @param importingClassMetadata annotation metadata of the importing class
	 * @param registry current bean definition registry
	 * @param importBeanNameGenerator the bean name generator strategy for imported beans:
	 * {@link ConfigurationClassPostProcessor#IMPORT_BEAN_NAME_GENERATOR} by default, or a
	 * user-provided one if {@link ConfigurationClassPostProcessor#setBeanNameGenerator}
	 * has been set. In the latter case, the passed-in strategy will be the same used for
	 * component scanning in the containing application context (otherwise, the default
	 * component-scan naming strategy is {@link AnnotationBeanNameGenerator#INSTANCE}).
	 * @since 5.2
	 * @see ConfigurationClassPostProcessor#IMPORT_BEAN_NAME_GENERATOR
	 * @see ConfigurationClassPostProcessor#setBeanNameGenerator
	 */
	// 根据导入@Configuration类的给定注解元数据，根据需要注册 bean 定义。
	//
	// 请注意，由于与 @Configuration类处理相关的生命周期限制，此处可能未注册 BeanDefinitionRegistryPostProcessor 类型。
	//
	// 默认实现委托给 registerBeanDefinitions(AnnotationMetadata, BeanDefinitionRegistry) 。
	//
	// 参形：
	//			importingClassMetadata – 导入类的注解元数据
	//			registry - 当前 bean 定义注册表
	//
	// importBeanNameGenerator - 导入 bean 的 bean 名称生成器策略：默认情况下为ConfigurationClassPostProcessor.IMPORT_BEAN_NAME_GENERATOR ，如果已设置ConfigurationClassPostProcessor.setBeanNameGenerator ，则为用户提供的策略。在后一种情况下，传入的策略将与包含应用程序上下文中的组件扫描所使用的策略相同（否则，默认的组件扫描命名策略是AnnotationBeanNameGenerator.INSTANCE ）
	default void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry,
			BeanNameGenerator importBeanNameGenerator) {

		registerBeanDefinitions(importingClassMetadata, registry);
	}

	/**
	 * Register bean definitions as necessary based on the given annotation metadata of
	 * the importing {@code @Configuration} class.
	 * <p>Note that {@link BeanDefinitionRegistryPostProcessor} types may <em>not</em> be
	 * registered here, due to lifecycle constraints related to {@code @Configuration}
	 * class processing.
	 * <p>The default implementation is empty.
	 * @param importingClassMetadata annotation metadata of the importing class
	 * @param registry current bean definition registry
	 */
	// 根据导入 @Configuration 类的给定注解元数据，根据需要注册 bean 定义。
	// 请注意，由于与 @Configuration 类处理相关的生命周期限制，此处可能未注册BeanDefinitionRegistryPostProcessor类型。
	// 默认实现为空。
	// 参形：
	//			importingClassMetadata – 导入类的注解元数据
	//			registry - 当前 bean 定义注册表
	default void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
	}

}
