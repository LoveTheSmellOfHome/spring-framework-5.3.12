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

package org.springframework.core.io;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

import org.springframework.lang.Nullable;

/**
 * Interface for a resource descriptor that abstracts from the actual
 * type of underlying resource, such as a file or class path resource.
 *
 * <p>An InputStream can be opened for every resource if it exists in
 * physical form, but a URL or File handle can just be returned for
 * certain resources. The actual behavior is implementation-specific.
 *
 * @author Juergen Hoeller
 * @since 28.12.2003
 * @see #getInputStream()
 * @see #getURL()
 * @see #getURI()
 * @see #getFile()
 * @see WritableResource
 * @see ContextResource
 * @see UrlResource
 * @see FileUrlResource
 * @see FileSystemResource
 * @see ClassPathResource
 * @see ByteArrayResource
 * @see InputStreamResource
 */
// 从底层资源的实际类型（例如文件或类路径资源）中抽象出来的资源描述符的接口
//
// <p>如果每个资源以物理形式存在，则可以为每个资源打开 InputStream，但是对于某些资源只能返回 URL 或文件句柄。实际行为是特定于实现的。
// 只读资源
// Spring 通过 Resource 接口把(Java 标准资源接口有三类：1.ClassLoader 2.File(文件系统 API) 3.URL) 三者统一起来，形成统一API,
// 用户不需要关注具体哪种实现
//
// Spring 中的 Resource 资源支持协议扩展，它不单单指本地资源文件，有可能是网络资源文件，扩展协议资源文件。一切皆可资源
public interface Resource extends InputStreamSource {

	/**
	 * Determine whether this resource actually exists in physical form.
	 * <p>This method performs a definitive existence check, whereas the
	 * existence of a {@code Resource} handle only guarantees a valid
	 * descriptor handle.
	 */
	// 确定该资源是否以物理形式实际存在。
	// <p>此方法执行明确的存在检查，而 {@code Resource} 句柄的存在仅保证有效的描述符句柄。
	boolean exists();

	/**
	 * Indicate whether non-empty contents of this resource can be read via
	 * {@link #getInputStream()}.
	 * <p>Will be {@code true} for typical resource descriptors that exist
	 * since it strictly implies {@link #exists()} semantics as of 5.1.
	 * Note that actual content reading may still fail when attempted.
	 * However, a value of {@code false} is a definitive indication
	 * that the resource content cannot be read.
	 * @see #getInputStream()
	 * @see #exists()
	 */
	// 指示是否可以通过 {@link getInputStream()} 读取此资源的非空内容。 <p>对于存在的典型资源描述符将是 {@code true}，
	// 因为它严格暗示了 5.1 的 {@link exists()} 语义。请注意，尝试实际阅读内容时可能仍会失败。但是，{@code false} 值是
	// 无法读取资源内容的明确指示。
	default boolean isReadable() {
		return exists();
	}

	/**
	 * Indicate whether this resource represents a handle with an open stream.
	 * If {@code true}, the InputStream cannot be read multiple times,
	 * and must be read and closed to avoid resource leaks.
	 * <p>Will be {@code false} for typical resource descriptors.
	 */
	// 指示此资源是否表示具有打开流的句柄。如果 {@code true}，则 InputStream 不能被多次读取，必须读取并关闭以避免资源泄漏。
	// <p>对于典型的资源描述符将是 {@code false}。
	default boolean isOpen() {
		return false;
	}

	/**
	 * Determine whether this resource represents a file in a file system.
	 * A value of {@code true} strongly suggests (but does not guarantee)
	 * that a {@link #getFile()} call will succeed.
	 * <p>This is conservatively {@code false} by default.
	 * @since 5.0
	 * @see #getFile()
	 */
	// 确定此资源是否代表文件系统中的文件。 {@code true} 值强烈暗示（但不保证）{@link getFile()} 调用会成功。
	// <p>这是保守的 {@code false} 默认情况下
	default boolean isFile() {
		return false;
	}

	/**
	 * Return a URL handle for this resource.
	 * @throws IOException if the resource cannot be resolved as URL,
	 * i.e. if the resource is not available as descriptor
	 */
	// 返回此资源的 URL 句柄。 @throws IOException 如果资源不能被解析为 URL，即如果资源不能作为描述符
	// 拿到 URL 后我们可以 {@link URL#openConnection()} 打开远程资源，然后重新再输入，有一定的小漏洞
	URL getURL() throws IOException;

	/**
	 * Return a URI handle for this resource.
	 * @throws IOException if the resource cannot be resolved as URI,
	 * i.e. if the resource is not available as descriptor
	 * @since 2.5
	 */
	// 返回此资源的 URI 句柄。 @throws IOException 如果资源不能被解析为 URI，即如果资源不能作为描述符
	URI getURI() throws IOException;

	/**
	 * Return a File handle for this resource.
	 * @throws java.io.FileNotFoundException if the resource cannot be resolved as
	 * absolute file path, i.e. if the resource is not available in a file system
	 * @throws IOException in case of general resolution/reading failures
	 * @see #getInputStream()
	 */
	// 返回此资源的文件句柄。
	// @throws java.io.FileNotFoundException 如果资源不能解析为绝对文件路径，即如果资源在文件系统中不可用
	//  @throws IOException 在一般场景/读取失败的情况下
	File getFile() throws IOException;

	/**
	 * Return a {@link ReadableByteChannel}.
	 * <p>It is expected that each call creates a <i>fresh</i> channel.
	 * <p>The default implementation returns {@link Channels#newChannel(InputStream)}
	 * with the result of {@link #getInputStream()}.
	 * @return the byte channel for the underlying resource (must not be {@code null})
	 * @throws java.io.FileNotFoundException if the underlying resource doesn't exist
	 * @throws IOException if the content channel could not be opened
	 * @since 5.0
	 * @see #getInputStream()
	 */
	// 返回一个 {@link ReadableByteChannel}。
	// <p>预计每次调用都会创建一个 <i>fresh<i> 通道。
	// <p>默认实现返回 {@link ChannelsnewChannel(InputStream)} 和 {@link getInputStream()} 的结果。
	default ReadableByteChannel readableChannel() throws IOException {
		// 通过通道的方式去读取输入流
		return Channels.newChannel(getInputStream());
	}

	/**
	 * Determine the content length for this resource.
	 * @throws IOException if the resource cannot be resolved
	 * (in the file system or as some other known physical resource type)
	 */
	// 确定此资源的内容长度
	long contentLength() throws IOException;

	/**
	 * Determine the last-modified timestamp for this resource.
	 * @throws IOException if the resource cannot be resolved
	 * (in the file system or as some other known physical resource type)
	 */
	// 确定此资源的最后修改时间戳
	// 并不是所有的资源都支持这个方法
	long lastModified() throws IOException;

	/**
	 * Create a resource relative to this resource.
	 * @param relativePath the relative path (relative to this resource)
	 * @return the resource handle for the relative resource
	 * @throws IOException if the relative resource cannot be determined
	 */
	// 创建与此资源相关的资源
	Resource createRelative(String relativePath) throws IOException;

	/**
	 * Determine a filename for this resource, i.e. typically the last
	 * part of the path: for example, "myfile.txt".
	 * <p>Returns {@code null} if this type of resource does not
	 * have a filename.
	 */
	// 确定此资源的文件名，即通常是路径的最后一部分：例如，“myfile.txt”。
	// <p>如果此类资源没有文件名，则返回 {@code null}
	@Nullable
	String getFilename();

	/**
	 * Return a description for this resource,
	 * to be used for error output when working with the resource.
	 * <p>Implementations are also encouraged to return this value
	 * from their {@code toString} method.
	 * @see Object#toString()
	 */
	// 返回此资源的描述，用于在使用资源时输出错误。 <p>还鼓励实现从其 {@code toString} 方法返回此值。
	String getDescription();

}
