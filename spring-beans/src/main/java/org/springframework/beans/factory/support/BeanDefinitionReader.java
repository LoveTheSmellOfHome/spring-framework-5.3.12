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

package org.springframework.beans.factory.support;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.lang.Nullable;

/**
 * Simple interface for bean definition readers.
 * Specifies load methods with Resource and String location parameters.
 *
 * <p>Concrete bean definition readers can of course add additional
 * load and register methods for bean definitions, specific to
 * their bean definition format.
 *
 * <p>Note that a bean definition reader does not have to implement
 * this interface. It only serves as suggestion for bean definition
 * readers that want to follow standard naming conventions.
 *
 * @author Juergen Hoeller
 * @since 1.1
 * @see org.springframework.core.io.Resource
 */
// bean 定义阅读器的简单接口。使用 Resource 和 String 位置参数指定加载方法。是一个面向资源的接口
// <p>具体的 bean 定义读者当然可以为 bean 定义添加额外的加载和注册方法，特定于他们的 bean 定义格式。
// <p>请注意，bean 定义阅读器不必实现此接口。它仅作为想要遵循标准命名约定的 bean 定义读者的建议
public interface BeanDefinitionReader {

	/**
	 * Return the bean factory to register the bean definitions with.
	 * <p>The factory is exposed through the BeanDefinitionRegistry interface,
	 * encapsulating the methods that are relevant for bean definition handling.
	 */
	// 返回 bean 工厂以注册 bean 定义。
	// <p>工厂通过 BeanDefinitionRegistry 接口公开，封装了与 bean 定义处理相关的方法。
	BeanDefinitionRegistry getRegistry();

	/**
	 * Return the resource loader to use for resource locations.
	 * Can be checked for the <b>ResourcePatternResolver</b> interface and cast
	 * accordingly, for loading multiple resources for a given resource pattern.
	 * <p>A {@code null} return value suggests that absolute resource loading
	 * is not available for this bean definition reader.
	 * <p>This is mainly meant to be used for importing further resources
	 * from within a bean definition resource, for example via the "import"
	 * tag in XML bean definitions. It is recommended, however, to apply
	 * such imports relative to the defining resource; only explicit full
	 * resource locations will trigger absolute resource loading.
	 * <p>There is also a {@code loadBeanDefinitions(String)} method available,
	 * for loading bean definitions from a resource location (or location pattern).
	 * This is a convenience to avoid explicit ResourceLoader handling.
	 * @see #loadBeanDefinitions(String)
	 * @see org.springframework.core.io.support.ResourcePatternResolver
	 */
	// 返回用于资源位置的资源加载器。可以检查 <b>ResourcePatternResolver<b> 接口并进行相应的转换，以便为给定的资源模式加载多个资源。
	// <p>{@code null} 返回值表明绝对资源加载不可用于此 bean 定义阅读器。
	// <p>这主要用于从 bean 定义资源中导入更多资源，例如通过 XML bean 定义中的“import”标签。
	// 但是，建议相对于定义资源应用此类导入；只有明确的完整资源位置才会触发绝对资源加载。
	// <p>还有一个 {@code loadBeanDefinitions(String)} 方法可用，用于从资源位置（或位置模式）加载 bean 定义。
	// 这是避免显式 ResourceLoader 处理的便利。
	// @see loadBeanDefinitions(String)
	// @see org.springframework.core.io.support.ResourcePatternResolver
	@Nullable
	ResourceLoader getResourceLoader();

	/**
	 * Return the class loader to use for bean classes.
	 * <p>{@code null} suggests to not load bean classes eagerly
	 * but rather to just register bean definitions with class names,
	 * with the corresponding Classes to be resolved later (or never).
	 */
	// 返回用于 bean 类的类加载器。
	// <p>{@code null} 建议不要急切地加载 bean 类，而是只用类名注册 bean 定义，稍后（或永远不会）解析相应的类
	@Nullable
	ClassLoader getBeanClassLoader();

	/**
	 * Return the BeanNameGenerator to use for anonymous beans
	 * (without explicit bean name specified).
	 */
	// 返回 BeanNameGenerator 以用于匿名 bean（未指定显式 bean 名称）。
	BeanNameGenerator getBeanNameGenerator();


	/**
	 * Load bean definitions from the specified resource.
	 * @param resource the resource descriptor
	 * @return the number of bean definitions found
	 * @throws BeanDefinitionStoreException in case of loading or parsing errors
	 */
	// 从指定的资源加载 bean 定义。
	// @param resource 资源描述符
	// @return 找到的 bean 定义数
	// @throws BeanDefinitionStoreException 在加载或解析错误的情况下
	int loadBeanDefinitions(Resource resource) throws BeanDefinitionStoreException;

	/**
	 * Load bean definitions from the specified resources.
	 * @param resources the resource descriptors
	 * @return the number of bean definitions found
	 * @throws BeanDefinitionStoreException in case of loading or parsing errors
	 */
	// 从指定的资源加载 bean 定义。
	// @param resources 资源描述符
	// @return 找到的 bean 定义的数量
	// @throws BeanDefinitionStoreException 在加载或解析错误的情况下
	int loadBeanDefinitions(Resource... resources) throws BeanDefinitionStoreException;

	/**
	 * Load bean definitions from the specified resource location.
	 * <p>The location can also be a location pattern, provided that the
	 * ResourceLoader of this bean definition reader is a ResourcePatternResolver.
	 * @param location the resource location, to be loaded with the ResourceLoader
	 * (or ResourcePatternResolver) of this bean definition reader
	 * @return the number of bean definitions found
	 * @throws BeanDefinitionStoreException in case of loading or parsing errors
	 * @see #getResourceLoader()
	 * @see #loadBeanDefinitions(org.springframework.core.io.Resource)
	 * @see #loadBeanDefinitions(org.springframework.core.io.Resource[])
	 */
	// 从指定的资源位置加载 bean 定义。
	// <p>位置也可以是位置模式，前提是这个 bean 定义阅读器的 ResourceLoader 是一个 ResourcePatternResolver。
	// @param location 资源位置，用这个bean定义读取器的ResourceLoader（或ResourcePatternResolver）加载
	// @return找到的bean定义数
	// @throws BeanDefinitionStoreException以防加载或解析错误
	// @see getResourceLoader()
	// @see loadBeanDefinitions(org .springframework.core.io.Resource)
	// @see loadBeanDefinitions(org.springframework.core.io.Resource[])
	int loadBeanDefinitions(String location) throws BeanDefinitionStoreException;

	/**
	 * Load bean definitions from the specified resource locations.
	 * @param locations the resource locations, to be loaded with the ResourceLoader
	 * (or ResourcePatternResolver) of this bean definition reader
	 * @return the number of bean definitions found
	 * @throws BeanDefinitionStoreException in case of loading or parsing errors
	 */
	// 从指定的资源位置加载 bean 定义。
	// @param 定位资源位置，用这个 bean 定义读取器的 ResourceLoader（或 ResourcePatternResolver）加载
	// @return 找到的 bean 定义的数量 @throws BeanDefinitionStoreException 在加载或解析错误的情况
	int loadBeanDefinitions(String... locations) throws BeanDefinitionStoreException;

}
