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

package org.springframework.core.io.support;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;

import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

/**
 * Subclass of {@link PropertiesPropertySource} that loads a {@link Properties} object
 * from a given {@link org.springframework.core.io.Resource} or resource location such as
 * {@code "classpath:/com/myco/foo.properties"} or {@code "file:/path/to/file.xml"}.
 *
 * <p>Both traditional and XML-based properties file formats are supported; however, in
 * order for XML processing to take effect, the underlying {@code Resource}'s
 * {@link org.springframework.core.io.Resource#getFilename() getFilename()} method must
 * return a non-{@code null} value that ends in {@code ".xml"}.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 3.1
 * @see org.springframework.core.io.Resource
 * @see org.springframework.core.io.support.EncodedResource
 */
// {@link PropertiesPropertySource} 子类，从给定的 {@link org.springframework.core.io.Resource} 或
// 资源位置加载Properties对象，例如"classpath:/com/myco/foo.properties"或"file:/path/to/file.xml" 。
//
// 支持传统和基于 XML 的属性文件格式； 但是，为了使 XML 处理生效，底层 {@code Resource}'s 的
// {@link org.springframework.core.io.Resource#getFilename() getFilename()}  方法必须返回一个以".xml"结尾的非null值。
public class ResourcePropertySource extends PropertiesPropertySource {

	/** The original resource name, if different from the given name. */
	// 原始资源名称，如果与给定名称不同
	@Nullable
	private final String resourceName;


	/**
	 * Create a PropertySource having the given name based on Properties
	 * loaded from the given encoded resource.
	 */
	// 根据从给定编码资源加载的属性创建具有给定名称的 PropertySource
	public ResourcePropertySource(String name, EncodedResource resource) throws IOException {
		super(name, PropertiesLoaderUtils.loadProperties(resource));
		this.resourceName = getNameForResource(resource.getResource());
	}

	/**
	 * Create a PropertySource based on Properties loaded from the given resource.
	 * The name of the PropertySource will be generated based on the
	 * {@link Resource#getDescription() description} of the given resource.
	 */
	// 根据从给定资源加载的属性创建一个 PropertySource。 PropertySource 的名称将根据
	public ResourcePropertySource(EncodedResource resource) throws IOException {
		super(getNameForResource(resource.getResource()), PropertiesLoaderUtils.loadProperties(resource));
		this.resourceName = null;
	}

	/**
	 * Create a PropertySource having the given name based on Properties
	 * loaded from the given encoded resource.
	 */
	// 根据从给定编码资源加载的属性创建具有给定名称的 PropertySource
	public ResourcePropertySource(String name, Resource resource) throws IOException {
		super(name, PropertiesLoaderUtils.loadProperties(new EncodedResource(resource)));
		this.resourceName = getNameForResource(resource);
	}

	/**
	 * Create a PropertySource based on Properties loaded from the given resource.
	 * The name of the PropertySource will be generated based on the
	 * {@link Resource#getDescription() description} of the given resource.
	 */
	// 根据从给定资源加载的属性创建一个 PropertySource。 PropertySource 的名称将根据给定
	// 资源的 {@link ResourcegetDescription() description} 生成。
	public ResourcePropertySource(Resource resource) throws IOException {
		super(getNameForResource(resource), PropertiesLoaderUtils.loadProperties(new EncodedResource(resource)));
		this.resourceName = null;
	}

	/**
	 * Create a PropertySource having the given name based on Properties loaded from
	 * the given resource location and using the given class loader to load the
	 * resource (assuming it is prefixed with {@code classpath:}).
	 */
	// 根据从给定资源位置加载的属性创建一个具有给定名称的 PropertySource，并使用给定的类加载器
	// 加载资源（假设它以 {@code classpath:} 为前缀）。
	public ResourcePropertySource(String name, String location, ClassLoader classLoader) throws IOException {
		this(name, new DefaultResourceLoader(classLoader).getResource(location));
	}

	/**
	 * Create a PropertySource based on Properties loaded from the given resource
	 * location and use the given class loader to load the resource, assuming it is
	 * prefixed with {@code classpath:}. The name of the PropertySource will be
	 * generated based on the {@link Resource#getDescription() description} of the
	 * resource.
	 */
	// 根据从给定资源位置加载的属性创建一个 PropertySource，并使用给定的类加载器加载资源，假设它以
	// {@code classpath:} 为前缀。 PropertySource 的名称将根据资源的 {@link Resource#getDescription() description} 生成。
	public ResourcePropertySource(String location, ClassLoader classLoader) throws IOException {
		this(new DefaultResourceLoader(classLoader).getResource(location));
	}

	/**
	 * Create a PropertySource having the given name based on Properties loaded from
	 * the given resource location. The default thread context class loader will be
	 * used to load the resource (assuming the location string is prefixed with
	 * {@code classpath:}.
	 */
	// 根据从给定资源位置加载的属性创建具有给定名称的 PropertySource。默认线程上下文类加载器将用于加载
	// 资源（假设位置字符串以 {@code classpath:} 为前缀。
	public ResourcePropertySource(String name, String location) throws IOException {
		this(name, new DefaultResourceLoader().getResource(location));
	}

	/**
	 * Create a PropertySource based on Properties loaded from the given resource
	 * location. The name of the PropertySource will be generated based on the
	 * {@link Resource#getDescription() description} of the resource.
	 */
	// 根据从给定资源位置加载的属性创建一个 PropertySource。 PropertySource 的名称将
	// 根据资源的 {@link Resource#getDescription() description} 生成。
	public ResourcePropertySource(String location) throws IOException {
		this(new DefaultResourceLoader().getResource(location));
	}

	private ResourcePropertySource(String name, @Nullable String resourceName, Map<String, Object> source) {
		super(name, source);
		this.resourceName = resourceName;
	}


	/**
	 * Return a potentially adapted variant of this {@link ResourcePropertySource},
	 * overriding the previously given (or derived) name with the specified name.
	 * @since 4.0.4
	 */
	// 回此 {@link ResourcePropertySource} 的潜在改编变体，用指定的名称覆盖先前给定的（或派生的）名称
	public ResourcePropertySource withName(String name) {
		if (this.name.equals(name)) {
			return this;
		}
		// Store the original resource name if necessary...
		if (this.resourceName != null) {
			if (this.resourceName.equals(name)) {
				return new ResourcePropertySource(this.resourceName, null, this.source);
			}
			else {
				return new ResourcePropertySource(name, this.resourceName, this.source);
			}
		}
		else {
			// Current name is resource name -> preserve it in the extra field...
			return new ResourcePropertySource(name, this.name, this.source);
		}
	}

	/**
	 * Return a potentially adapted variant of this {@link ResourcePropertySource},
	 * overriding the previously given name (if any) with the original resource name
	 * (equivalent to the name generated by the name-less constructor variants).
	 * @since 4.1
	 */
	// 返回此 {@link ResourcePropertySource} 的潜在适应变体，用原始资源
	// 名称（相当于无名称构造函数变体生成的名称）覆盖先前给定的名称（如果有）。
	public ResourcePropertySource withResourceName() {
		if (this.resourceName == null) {
			return this;
		}
		return new ResourcePropertySource(this.resourceName, null, this.source);
	}


	/**
	 * Return the description for the given Resource; if the description is
	 * empty, return the class name of the resource plus its identity hash code.
	 * @see org.springframework.core.io.Resource#getDescription()
	 */
	// 返回给定资源的描述；如果描述为空，则返回资源的类名及其身份哈希码。
	private static String getNameForResource(Resource resource) {
		String name = resource.getDescription();
		if (!StringUtils.hasText(name)) {
			name = resource.getClass().getSimpleName() + "@" + System.identityHashCode(resource);
		}
		return name;
	}

}
