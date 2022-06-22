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

import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;

/**
 * Extended interface for a resource that supports writing to it.
 * Provides an {@link #getOutputStream() OutputStream accessor}.
 *
 * @author Juergen Hoeller
 * @since 3.1
 * @see java.io.OutputStream
 */
// 支持写入的资源的扩展接口。提供一个 {@link getOutputStream() OutputStream 访问器}。
public interface WritableResource extends Resource {

	/**
	 * Indicate whether the contents of this resource can be written
	 * via {@link #getOutputStream()}.
	 * <p>Will be {@code true} for typical resource descriptors;
	 * note that actual content writing may still fail when attempted.
	 * However, a value of {@code false} is a definitive indication
	 * that the resource content cannot be modified.
	 * @see #getOutputStream()
	 * @see #isReadable()
	 */
	// 指示是否可以通过{@link getOutputStream()} 写入此资源的内容。
	// <p>对于典型的资源描述符将是 {@code true}；请注意，实际内容写入在尝试时可能仍会失败。
	// 但是，{@code false} 值是资源内容无法修改的明确指示。
	default boolean isWritable() {
		return true;
	}

	/**
	 * Return an {@link OutputStream} for the underlying resource,
	 * allowing to (over-)write its content.
	 * @throws IOException if the stream could not be opened
	 * @see #getInputStream()
	 */
	// 为底层资源返回一个 {@link OutputStream}，允许（覆盖）写入其内容
	OutputStream getOutputStream() throws IOException;

	/**
	 * Return a {@link WritableByteChannel}.
	 * <p>It is expected that each call creates a <i>fresh</i> channel.
	 * <p>The default implementation returns {@link Channels#newChannel(OutputStream)}
	 * with the result of {@link #getOutputStream()}.
	 * @return the byte channel for the underlying resource (must not be {@code null})
	 * @throws java.io.FileNotFoundException if the underlying resource doesn't exist
	 * @throws IOException if the content channel could not be opened
	 * @since 5.0
	 * @see #getOutputStream()
	 */
	// 返回一个 {@link WritableByteChannel}。
	// <p>预计每次调用都会创建一个 <i>fresh<i> 通道。
	// <p>默认实现返回 {@link ChannelsnewChannel(OutputStream)} 和 {@link getOutputStream()} 的结果
	default WritableByteChannel writableChannel() throws IOException {
		return Channels.newChannel(getOutputStream());
	}

}
