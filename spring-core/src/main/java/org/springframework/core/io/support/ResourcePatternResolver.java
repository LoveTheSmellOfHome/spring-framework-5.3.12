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

package org.springframework.core.io.support;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.IOException;

/**
 * Strategy interface for resolving a location pattern (for example,
 * an Ant-style path pattern) into {@link Resource} objects.
 *
 * <p>This is an extension to the {@link org.springframework.core.io.ResourceLoader}
 * interface. A passed-in {@code ResourceLoader} (for example, an
 * {@link org.springframework.context.ApplicationContext} passed in via
 * {@link org.springframework.context.ResourceLoaderAware} when running in a context)
 * can be checked whether it implements this extended interface too.
 *
 * <p>{@link PathMatchingResourcePatternResolver} is a standalone implementation
 * that is usable outside an {@code ApplicationContext}, also used by
 * {@link ResourceArrayPropertyEditor} for populating {@code Resource} array bean
 * properties.
 *
 * <p>Can be used with any sort of location pattern (e.g. "/WEB-INF/*-context.xml"):
 * Input patterns have to match the strategy implementation. This interface just
 * specifies the conversion method rather than a specific pattern format.
 *
 * <p>This interface also suggests a new resource prefix "classpath*:" for all
 * matching resources from the class path. Note that the resource location is
 * expected to be a path without placeholders in this case (e.g. "/beans.xml");
 * JAR files or different directories in the class path can contain multiple files
 * of the same name.
 *
 * @author Juergen Hoeller
 * @since 1.0.2
 * @see org.springframework.core.io.Resource
 * @see org.springframework.core.io.ResourceLoader
 * @see org.springframework.context.ApplicationContext
 * @see org.springframework.context.ResourceLoaderAware
 */
// 用于将位置模式（例如，Ant 风格的路径模式）解析为 {@link Resource} 对象的策略接口。
//
// <p>这是对 {@link org.springframework.core.io.ResourceLoader} 接口的扩展。可以检查传入的{@code ResourceLoader}
//（例如，在上下文中运行时通过{@link org.springframework.context.ResourceLoaderAware}传入的
// {@link org.springframework.context.ApplicationContext}）是否也实现了这个扩展接口。
//
// <p>{@link PathMatchingResourcePatternResolver} 是一个独立的实现，可在 {@code ApplicationContext} 之外使用，
// 也被 {@link ResourceArrayPropertyEditor} 用于填充 {@code Resource} 数组 bean 属性。
//
// <p>可以与任何类型的位置模式一起使用（例如“/WEB-INF/*-context.xml”）：输入模式必须与策略实现相匹配。
// 这个接口只是指定了转换方法，而不是具体的模式格式。
//
// <p>这个接口还为类路径中的所有匹配资源建议了一个新的资源前缀“classpath*:”
// 请注意，在这种情况下，资源位置应该是没有占位符的路径（例如“/beans.xml”）； JAR 文件或类路径中的不同目录可以包含多个同名文件。
//
// 通配符资源路径加载器，使用通配符的方式
public interface ResourcePatternResolver extends ResourceLoader {

	/**
	 * Pseudo URL prefix for all matching resources from the class path: "classpath*:"
	 * <p>This differs from ResourceLoader's classpath URL prefix in that it
	 * retrieves all matching resources for a given name (e.g. "/beans.xml"),
	 * for example in the root of all deployed JAR files.
	 * @see org.springframework.core.io.ResourceLoader#CLASSPATH_URL_PREFIX
	 */
	// 类路径中所有匹配资源的伪 URL 前缀：“classpath*:”
	// <p>这与 ResourceLoader 的类路径 URL 前缀不同，它检索给定名称（例如“/beans.xml”）的所有匹配资源，
	// 例如在所有已部署 JAR 文件的根目录。
	// @see org.springframework.core.io.ResourceLoader#CLASSPATH_URL_PREFIX("classpath:")
	String CLASSPATH_ALL_URL_PREFIX = "classpath*:"; // 匹配所有 classpath 下的资源 （所有 jar包）

	/**
	 * Resolve the given location pattern into {@code Resource} objects.
	 * <p>Overlapping resource entries that point to the same physical
	 * resource should be avoided, as far as possible. The result should
	 * have set semantics.
	 * @param locationPattern the location pattern to resolve
	 * @return the corresponding {@code Resource} objects
	 * @throws IOException in case of I/O errors
	 */
	// 将给定的位置模式解析为 {@code Resource} 对象。
	// <p>应尽可能避免指向同一物理资源的重叠资源条目。结果应该具有设置语义。
	// @param locationPattern 要解析的位置模式
	// @return 对应的 {@code Resource} 对象
	Resource[] getResources(String locationPattern) throws IOException;

}
