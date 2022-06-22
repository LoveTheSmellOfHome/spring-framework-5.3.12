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

package org.springframework.core.type.classreading;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;

/**
 * Simple implementation of the {@link MetadataReaderFactory} interface,
 * creating a new ASM {@link org.springframework.asm.ClassReader} for every request.
 *
 * @author Juergen Hoeller
 * @since 2.5
 */
// @link MetadataReaderFactory} 接口的简单实现，为每个请求创建一个新的 ASM
// {@link org.springframework.asm.ClassReader}。通过 Java 字节码来操作，
public class SimpleMetadataReaderFactory implements MetadataReaderFactory {

	private final ResourceLoader resourceLoader;


	/**
	 * Create a new SimpleMetadataReaderFactory for the default class loader.
	 */
	// 为默认的类加载器创建一个新的 SimpleMetadataReaderFactory。
	public SimpleMetadataReaderFactory() {
		this.resourceLoader = new DefaultResourceLoader();
	}

	/**
	 * Create a new SimpleMetadataReaderFactory for the given resource loader.
	 * @param resourceLoader the Spring ResourceLoader to use
	 * (also determines the ClassLoader to use)
	 */
	// 为给定的资源加载器创建一个新的 SimpleMetadataReaderFactory。
	// @param resourceLoader 要使用的 Spring ResourceLoader（也决定要使用的 ClassLoader）
	public SimpleMetadataReaderFactory(@Nullable ResourceLoader resourceLoader) {
		this.resourceLoader = (resourceLoader != null ? resourceLoader : new DefaultResourceLoader());
	}

	/**
	 * Create a new SimpleMetadataReaderFactory for the given class loader.
	 * @param classLoader the ClassLoader to use
	 */
	// 为给定的类加载器创建一个新的 SimpleMetadataReaderFactory。
	// @param classLoader 要使用的类加载器
	public SimpleMetadataReaderFactory(@Nullable ClassLoader classLoader) {
		this.resourceLoader =
				(classLoader != null ? new DefaultResourceLoader(classLoader) : new DefaultResourceLoader());
	}


	/**
	 * Return the ResourceLoader that this MetadataReaderFactory has been
	 * 	 * constructed with.
	 */
	// 返回构建此 MetadataReaderFactory 的 ResourceLoader。
	public final ResourceLoader getResourceLoader() {
		return this.resourceLoader;
	}


	// 通过类名的方式来读取
	@Override
	public MetadataReader getMetadataReader(String className) throws IOException {
		try {
			String resourcePath = ResourceLoader.CLASSPATH_URL_PREFIX +
					ClassUtils.convertClassNameToResourcePath(className) + ClassUtils.CLASS_FILE_SUFFIX;
			// 资源路径 -> 资源
			Resource resource = this.resourceLoader.getResource(resourcePath);
			return getMetadataReader(resource);
		}
		catch (FileNotFoundException ex) {
			// Maybe an inner class name using the dot name syntax? Need to use the dollar syntax here...
			// ClassUtils.forName has an equivalent check for resolution into Class references later on.
			// 也许使用点名称语法的内部类名称？需要在这里使用美元语法... ClassUtils.forName 有一个等效的检查，
			// 以便稍后解析为类引用。
			int lastDotIndex = className.lastIndexOf('.');
			if (lastDotIndex != -1) {
				String innerClassName =
						className.substring(0, lastDotIndex) + '$' + className.substring(lastDotIndex + 1);
				String innerClassResourcePath = ResourceLoader.CLASSPATH_URL_PREFIX +
						ClassUtils.convertClassNameToResourcePath(innerClassName) + ClassUtils.CLASS_FILE_SUFFIX;
				Resource innerClassResource = this.resourceLoader.getResource(innerClassResourcePath);
				if (innerClassResource.exists()) {
					return getMetadataReader(innerClassResource);
				}
			}
			throw ex;
		}
	}

	// 通过资源的方式来读取
	@Override
	public MetadataReader getMetadataReader(Resource resource) throws IOException {
		// 资源 -> SimpleMetadataReader 对象
		return new SimpleMetadataReader(resource, this.resourceLoader.getClassLoader());
	}

}
