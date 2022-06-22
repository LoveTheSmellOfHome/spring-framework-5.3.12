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

package org.springframework.core.io;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * {@link Resource} implementation for class path resources. Uses either a
 * given {@link ClassLoader} or a given {@link Class} for loading resources.
 *
 * <p>Supports resolution as {@code java.io.File} if the class path
 * resource resides in the file system, but not for resources in a JAR.
 * Always supports resolution as URL.
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 28.12.2003
 * @see ClassLoader#getResourceAsStream(String)
 * @see Class#getResourceAsStream(String)
 */
// {@link Resource} 类路径资源的实现。使用给定的 {@link ClassLoader} 或给定的 {@link Class} 来加载资源。
// <p>如果类路径资源驻留在文件系统中，则支持解析为 {@code java.io.File}，但不支持 JAR 中的资源。始终支持解析为 URL。
//
// Java 标准接口有三类：1.ClassLoader 2.File(文件系统 API) 3.URL
public class ClassPathResource extends AbstractFileResolvingResource {

	private final String path;

	// 它的 ClassLoader 可以为空，那么这个对象是不是不需要 ClassLoader 来加载？
	// 答案是否定的，这个 ClassLoader 最终不会为空，如果参数中指定的 classLoader 为空，它会使用默认当前线程的 classLoader
	@Nullable
	private ClassLoader classLoader;

	@Nullable
	private Class<?> clazz;


	/**
	 * Create a new {@code ClassPathResource} for {@code ClassLoader} usage.
	 * A leading slash will be removed, as the ClassLoader resource access
	 * methods will not accept it.
	 * <p>The thread context class loader will be used for
	 * loading the resource.
	 * @param path the absolute path within the class path
	 * @see java.lang.ClassLoader#getResourceAsStream(String)
	 * @see org.springframework.util.ClassUtils#getDefaultClassLoader()
	 */
	// 为 {@code ClassLoader} 使用创建一个新的 {@code ClassPathResource}。
	// 前导斜杠将被删除，因为 ClassLoader 资源访问方法将不接受它。
	// <p>线程上下文类加载器将用于加载资源。
	// @param path classpath:的绝对路径
	public ClassPathResource(String path) {
		this(path, (ClassLoader) null);
	}

	/**
	 * Create a new {@code ClassPathResource} for {@code ClassLoader} usage.
	 * A leading slash will be removed, as the ClassLoader resource access
	 * methods will not accept it.
	 * @param path the absolute path within the classpath
	 * @param classLoader the class loader to load the resource with,
	 * or {@code null} for the thread context class loader
	 * @see ClassLoader#getResourceAsStream(String)
	 */
	// 为 {@code ClassLoader} 使用创建一个新的 {@code ClassPathResource}。
	// 前导斜杠将被删除，因为 ClassLoader 资源访问方法将不接受它。
	public ClassPathResource(String path, @Nullable ClassLoader classLoader) {
		Assert.notNull(path, "Path must not be null");
		String pathToUse = StringUtils.cleanPath(path);
		if (pathToUse.startsWith("/")) {
			pathToUse = pathToUse.substring(1);
		}
		this.path = pathToUse;
		// classLoader 最终不为空
		this.classLoader = (classLoader != null ? classLoader : ClassUtils.getDefaultClassLoader());
	}

	/**
	 * Create a new {@code ClassPathResource} for {@code Class} usage.
	 * The path can be relative to the given class, or absolute within
	 * the classpath via a leading slash.
	 * @param path relative or absolute path within the class path
	 * @param clazz the class to load resources with
	 * @see java.lang.Class#getResourceAsStream
	 */
	// 为 {@code Class} 使用创建一个新的 {@code ClassPathResource}。路径可以相对于给定的类，也可以通过前导斜杠在类路径中绝对
	public ClassPathResource(String path, @Nullable Class<?> clazz) {
		Assert.notNull(path, "Path must not be null");
		this.path = StringUtils.cleanPath(path);
		this.clazz = clazz;
	}

	/**
	 * Create a new {@code ClassPathResource} with optional {@code ClassLoader}
	 * and {@code Class}. Only for internal usage.
	 * @param path relative or absolute path within the classpath
	 * @param classLoader the class loader to load the resource with, if any
	 * @param clazz the class to load resources with, if any
	 * @deprecated as of 4.3.13, in favor of selective use of
	 * {@link #ClassPathResource(String, ClassLoader)} vs {@link #ClassPathResource(String, Class)}
	 */
	// 使用可选的 {@code ClassLoader} 和 {@code Class} 创建一个新的 {@code ClassPathResource}。仅供内部使用。
	@Deprecated
	protected ClassPathResource(String path, @Nullable ClassLoader classLoader, @Nullable Class<?> clazz) {
		this.path = StringUtils.cleanPath(path);
		this.classLoader = classLoader;
		this.clazz = clazz;
	}


	/**
	 * Return the path for this resource (as resource path within the class path).
	 */
	// 返回此资源的路径（作为类路径中的资源路径）
	public final String getPath() {
		return this.path;
	}

	/**
	 * Return the ClassLoader that this resource will be obtained from.
	 */
	// 返回将从中获取此资源的 ClassLoader
	@Nullable
	public final ClassLoader getClassLoader() {
		return (this.clazz != null ? this.clazz.getClassLoader() : this.classLoader);
	}


	/**
	 * This implementation checks for the resolution of a resource URL.
	 * @see java.lang.ClassLoader#getResource(String)
	 * @see java.lang.Class#getResource(String)
	 */
	// 此实现检查资源 URL 的解析
	@Override
	public boolean exists() {
		return (resolveURL() != null);
	}

	/**
	 * This implementation checks for the resolution of a resource URL upfront,
	 * then proceeding with {@link AbstractFileResolvingResource}'s length check.
	 * @see java.lang.ClassLoader#getResource(String)
	 * @see java.lang.Class#getResource(String)
	 */
	// 此实现会预先检查资源 URL 的解析，然后继续进行 {@link AbstractFileResolvingResource} 的长度检查。
	@Override
	public boolean isReadable() {
		URL url = resolveURL();
		return (url != null && checkReadable(url));
	}

	/**
	 * Resolves a URL for the underlying class path resource.
	 * @return the resolved URL, or {@code null} if not resolvable
	 */
	// 解析底层类路径资源的 URL。
	// @return 已解析的 URL，如果无法解析，则为 {@code null}
	@Nullable
	protected URL resolveURL() {
		try {
			if (this.clazz != null) {
				return this.clazz.getResource(this.path);
			}
			else if (this.classLoader != null) {
				return this.classLoader.getResource(this.path);
			}
			else {
				return ClassLoader.getSystemResource(this.path);
			}
		}
		catch (IllegalArgumentException ex) {
			// Should not happen according to the JDK's contract:
			// see https://github.com/openjdk/jdk/pull/2662
			return null;
		}
	}

	/**
	 * This implementation opens an InputStream for the given class path resource.
	 * @see java.lang.ClassLoader#getResourceAsStream(String)
	 * @see java.lang.Class#getResourceAsStream(String)
	 */
	// 此实现为给定的类路径资源打开一个 InputStream
	@Override
	public InputStream getInputStream() throws IOException {
		InputStream is;
		if (this.clazz != null) {
			// 1. 通过当前类来读取流：class 一般在 jar 包中，class 所读取的范围只针对 jar 包，或者 jar 包所对应的资源
			is = this.clazz.getResourceAsStream(this.path);
		}
		else if (this.classLoader != null) {
			// 2. 通过 classLoader 来读取流：读取的范围更大，如指定 -cp 参数范围或者指定 ClassPath 启动参数的时候，
			// 这些范围都可以读取到
			is = this.classLoader.getResourceAsStream(this.path);
		}
		else {
			// 3.通过读取系统 ClassLoader ，就会读取全部范围
			is = ClassLoader.getSystemResourceAsStream(this.path);
		}
		if (is == null) {
			throw new FileNotFoundException(getDescription() + " cannot be opened because it does not exist");
		}
		return is;
	}

	/**
	 * This implementation returns a URL for the underlying class path resource,
	 * if available.
	 * @see java.lang.ClassLoader#getResource(String)
	 * @see java.lang.Class#getResource(String)
	 */
	// 如果可用，此实现将返回基础类路径资源的 URL
	@Override
	public URL getURL() throws IOException {
		URL url = resolveURL();
		if (url == null) {
			throw new FileNotFoundException(getDescription() + " cannot be resolved to URL because it does not exist");
		}
		return url;
	}

	/**
	 * This implementation creates a ClassPathResource, applying the given path
	 * relative to the path of the underlying resource of this descriptor.
	 * @see org.springframework.util.StringUtils#applyRelativePath(String, String)
	 */
	// 此实现创建一个 ClassPathResource，应用相对于该描述符的底层资源路径的给定路径
	@Override
	public Resource createRelative(String relativePath) {
		String pathToUse = StringUtils.applyRelativePath(this.path, relativePath);
		return (this.clazz != null ? new ClassPathResource(pathToUse, this.clazz) :
				new ClassPathResource(pathToUse, this.classLoader));
	}

	/**
	 * This implementation returns the name of the file that this class path
	 * resource refers to.
	 * @see org.springframework.util.StringUtils#getFilename(String)
	 */
	// 此实现返回此类路径资源引用的文件的名称
	@Override
	@Nullable
	public String getFilename() {
		return StringUtils.getFilename(this.path);
	}

	/**
	 * This implementation returns a description that includes the class path location.
	 */
	// 此实现返回包含类路径位置的描述
	@Override
	public String getDescription() {
		StringBuilder builder = new StringBuilder("class path resource [");
		String pathToUse = this.path;
		if (this.clazz != null && !pathToUse.startsWith("/")) {
			builder.append(ClassUtils.classPackageAsResourcePath(this.clazz));
			builder.append('/');
		}
		if (pathToUse.startsWith("/")) {
			pathToUse = pathToUse.substring(1);
		}
		builder.append(pathToUse);
		builder.append(']');
		return builder.toString();
	}


	/**
	 * This implementation compares the underlying class path locations.
	 */
	// 此实现比较基础类路径位置
	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof ClassPathResource)) {
			return false;
		}
		ClassPathResource otherRes = (ClassPathResource) other;
		return (this.path.equals(otherRes.path) &&
				ObjectUtils.nullSafeEquals(this.classLoader, otherRes.classLoader) &&
				ObjectUtils.nullSafeEquals(this.clazz, otherRes.clazz));
	}

	/**
	 * This implementation returns the hash code of the underlying
	 * class path location.
	 */
	// 此实现返回底层类路径位置的哈希码
	@Override
	public int hashCode() {
		return this.path.hashCode();
	}

}
