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

package org.springframework.context;

import org.springframework.beans.factory.Aware;
import org.springframework.core.io.ResourceLoader;

/**
 * Interface to be implemented by any object that wishes to be notified of the
 * {@link ResourceLoader} (typically the ApplicationContext) that it runs in.
 * This is an alternative to a full {@link ApplicationContext} dependency via
 * the {@link org.springframework.context.ApplicationContextAware} interface.
 *
 * <p>Note that {@link org.springframework.core.io.Resource} dependencies can also
 * be exposed as bean properties of type {@code Resource} or {@code Resource[]},
 * populated via Strings with automatic type conversion by the bean factory. This
 * removes the need for implementing any callback interface just for the purpose
 * of accessing specific file resources.
 *
 * <p>You typically need a {@link ResourceLoader} when your application object has to
 * access a variety of file resources whose names are calculated. A good strategy is
 * to make the object use a {@link org.springframework.core.io.DefaultResourceLoader}
 * but still implement {@code ResourceLoaderAware} to allow for overriding when
 * running in an {@code ApplicationContext}. See
 * {@link org.springframework.context.support.ReloadableResourceBundleMessageSource}
 * for an example.
 *
 * <p>A passed-in {@code ResourceLoader} can also be checked for the
 * {@link org.springframework.core.io.support.ResourcePatternResolver} interface
 * and cast accordingly, in order to resolve resource patterns into arrays of
 * {@code Resource} objects. This will always work when running in an ApplicationContext
 * (since the context interface extends the ResourcePatternResolver interface). Use a
 * {@link org.springframework.core.io.support.PathMatchingResourcePatternResolver} as
 * default; see also the {@code ResourcePatternUtils.getResourcePatternResolver} method.
 *
 * <p>As an alternative to a {@code ResourcePatternResolver} dependency, consider
 * exposing bean properties of type {@code Resource[]} array, populated via pattern
 * Strings with automatic type conversion by the bean factory at binding time.
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @since 10.03.2004
 * @see ApplicationContextAware
 * @see org.springframework.core.io.Resource
 * @see org.springframework.core.io.ResourceLoader
 * @see org.springframework.core.io.support.ResourcePatternResolver
 */
// 由任何希望被通知它运行的ResourceLoader （通常是 ApplicationContext）的对象实现的接口。这是通过
// ApplicationContextAware 接口的完整ApplicationContext依赖项的替代方案。
//
// 请注意， org.springframework.core.io.Resource依赖项也可以作为Resource或Resource[]类型的 bean 属性公开，
// 通过由 bean 工厂自动类型转换的字符串填充。 这消除了为了访问特定文件资源而实现任何回调接口的需要。
//
// 当您的应用程序对象必须访问其名称已计算的各种文件资源时，您通常需要一个ResourceLoader 。
// 一个好的策略是让对象使用 org.springframework.core.io.DefaultResourceLoader 但仍然实现
// ResourceLoaderAware 以允许在 ApplicationContext 运行时覆盖。 有关示例，
// 请参阅org.springframework.context.support.ReloadableResourceBundleMessageSource 。
//
// 还可以检查传入的 ResourceLoader 以获取 org.springframework.core.io.support.ResourcePatternResolver
// 接口并进行相应的转换，以便将资源模式解析为 Resource 对象的数组。 在 ApplicationContext 中运行时，
// 这将始终有效（因为上下文接口扩展了 ResourcePatternResolver 接口）。 使用
// org.springframework.core.io.support.PathMatchingResourcePatternResolver 作为默认值；
// 另请参阅 ResourcePatternUtils.getResourcePatternResolver 方法。
//
// 作为 ResourcePatternResolver 依赖项的替代方案，考虑公开 Resource[] 数组类型的 bean 属性，
// 通过模式字符串填充，bean 工厂在绑定时自动进行类型转换。
public interface ResourceLoaderAware extends Aware {

	/**
	 * Set the ResourceLoader that this object runs in.
	 * <p>This might be a ResourcePatternResolver, which can be checked
	 * through {@code instanceof ResourcePatternResolver}. See also the
	 * {@code ResourcePatternUtils.getResourcePatternResolver} method.
	 * <p>Invoked after population of normal bean properties but before an init callback
	 * like InitializingBean's {@code afterPropertiesSet} or a custom init-method.
	 * Invoked before ApplicationContextAware's {@code setApplicationContext}.
	 * @param resourceLoader the ResourceLoader object to be used by this object
	 * @see org.springframework.core.io.support.ResourcePatternResolver
	 * @see org.springframework.core.io.support.ResourcePatternUtils#getResourcePatternResolver
	 */
	// 设置运行此对象的 ResourceLoader
	//
	// 这可能是一个 ResourcePatternResolver，可以通过instanceof ResourcePatternResolver进行检查。
	// 另请参阅ResourcePatternUtils.getResourcePatternResolver方法。
	//
	// 在填充普通 bean 属性之后但在初始化回调（如 InitializingBean 的afterPropertiesSet或自定义初始化方法）之前调用。
	// 在 ApplicationContextAware 的setApplicationContext之前调用
	void setResourceLoader(ResourceLoader resourceLoader);

}
