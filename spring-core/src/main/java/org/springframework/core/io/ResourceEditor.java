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

package org.springframework.core.io;

import java.beans.PropertyEditorSupport;
import java.io.IOException;

import org.springframework.core.env.PropertyResolver;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * {@link java.beans.PropertyEditor Editor} for {@link Resource}
 * descriptors, to automatically convert {@code String} locations
 * e.g. {@code file:C:/myfile.txt} or {@code classpath:myfile.txt} to
 * {@code Resource} properties instead of using a {@code String} location property.
 *
 * <p>The path may contain {@code ${...}} placeholders, to be
 * resolved as {@link org.springframework.core.env.Environment} properties:
 * e.g. {@code ${user.dir}}. Unresolvable placeholders are ignored by default.
 *
 * <p>Delegates to a {@link ResourceLoader} to do the heavy lifting,
 * by default using a {@link DefaultResourceLoader}.
 *
 * @author Juergen Hoeller
 * @author Dave Syer
 * @author Chris Beams
 * @since 28.12.2003
 * @see Resource
 * @see ResourceLoader
 * @see DefaultResourceLoader
 * @see PropertyResolver#resolvePlaceholders
 */
// {@link java.beans.PropertyEditor Editor} 用于 {@link Resource} 描述符，自动转换 {@code String} 位置，
// 例如{@code file:C:/myfile.txt} 或 {@code classpath:myfile.txt} 到 {@code Resource} 属性，
// 而不是使用 {@code String} 位置属性
//
// <p>路径可能包含 {@code ${...}} 占位符，解析为 {@link org.springframework.core.env.Environment} 属性：
// 例如{@code ${user.dir}}。默认情况下会忽略无法解析的占位符。
//
// <p>委托给 {@link ResourceLoader} 来完成繁重的工作，默认情况下使用 {@link DefaultResourceLoader}。
public class ResourceEditor extends PropertyEditorSupport {

	private final ResourceLoader resourceLoader;

	@Nullable
	private PropertyResolver propertyResolver;

	private final boolean ignoreUnresolvablePlaceholders;


	/**
	 * Create a new instance of the {@link ResourceEditor} class
	 * using a {@link DefaultResourceLoader} and {@link StandardEnvironment}.
	 */
	// 使用 {@link DefaultResourceLoader} 和 {@link StandardEnvironment} 创建 {@link ResourceEditor} 类的新实例
	public ResourceEditor() {
		this(new DefaultResourceLoader(), null);
	}

	/**
	 * Create a new instance of the {@link ResourceEditor} class
	 * using the given {@link ResourceLoader} and {@link PropertyResolver}.
	 * @param resourceLoader the {@code ResourceLoader} to use
	 * @param propertyResolver the {@code PropertyResolver} to use
	 */
	// 使用给定的 {@link ResourceLoader} 和 {@link PropertyResolver} 创建 {@link ResourceEditor} 类的新实例。
	// @param resourceLoader 要使用的 {@code ResourceLoader}
	// @param propertyResolver 要使用的 {@code PropertyResolver}
	public ResourceEditor(ResourceLoader resourceLoader, @Nullable PropertyResolver propertyResolver) {
		this(resourceLoader, propertyResolver, true);
	}

	/**
	 * Create a new instance of the {@link ResourceEditor} class
	 * using the given {@link ResourceLoader}.
	 * @param resourceLoader the {@code ResourceLoader} to use
	 * @param propertyResolver the {@code PropertyResolver} to use
	 * @param ignoreUnresolvablePlaceholders whether to ignore unresolvable placeholders
	 * if no corresponding property could be found in the given {@code propertyResolver}
	 */
	// 使用给定的 {@link ResourceLoader} 创建 {@link ResourceEditor} 类的新实例
	// @param resourceLoader 要使用的 {@code ResourceLoader}
	// @param propertyResolver 要使用的 {@code PropertyResolver}
	// @param ignoreUnresolvablePlaceholders 如果在给定的 {@code propertyResolver}
	// 中找不到相应的属性，是否忽略无法解析的占位符
	public ResourceEditor(ResourceLoader resourceLoader, @Nullable PropertyResolver propertyResolver,
			boolean ignoreUnresolvablePlaceholders) {

		Assert.notNull(resourceLoader, "ResourceLoader must not be null");
		this.resourceLoader = resourceLoader;
		this.propertyResolver = propertyResolver;
		this.ignoreUnresolvablePlaceholders = ignoreUnresolvablePlaceholders;
	}


	@Override
	public void setAsText(String text) {
		if (StringUtils.hasText(text)) {
			String locationToUse = resolvePath(text).trim();
			setValue(this.resourceLoader.getResource(locationToUse));
		}
		else {
			setValue(null);
		}
	}

	/**
	 * Resolve the given path, replacing placeholders with corresponding
	 * property values from the {@code environment} if necessary.
	 * @param path the original file path
	 * @return the resolved file path
	 * @see PropertyResolver#resolvePlaceholders
	 * @see PropertyResolver#resolveRequiredPlaceholders
	 */
	// 解析给定的路径，如有必要，用来自 {@code environment} 的相应属性值替换占位符。
	// @param path 原始文件路径
	// @return 解析的文件路径
	protected String resolvePath(String path) {
		if (this.propertyResolver == null) {
			this.propertyResolver = new StandardEnvironment();
		}
		return (this.ignoreUnresolvablePlaceholders ? this.propertyResolver.resolvePlaceholders(path) :
				this.propertyResolver.resolveRequiredPlaceholders(path));
	}


	@Override
	@Nullable
	public String getAsText() {
		Resource value = (Resource) getValue();
		try {
			// Try to determine URL for resource.
			return (value != null ? value.getURL().toExternalForm() : "");
		}
		catch (IOException ex) {
			// Couldn't determine resource URL - return null to indicate
			// that there is no appropriate text representation.
			return null;
		}
	}

}
