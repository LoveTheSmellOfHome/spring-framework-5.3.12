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

import java.io.IOException;
import java.io.InputStream;

/**
 * Simple interface for objects that are sources for an {@link InputStream}.
 *
 * <p>This is the base interface for Spring's more extensive {@link Resource} interface.
 *
 * <p>For single-use streams, {@link InputStreamResource} can be used for any
 * given {@code InputStream}. Spring's {@link ByteArrayResource} or any
 * file-based {@code Resource} implementation can be used as a concrete
 * instance, allowing one to read the underlying content stream multiple times.
 * This makes this interface useful as an abstract content source for mail
 * attachments, for example.
 *
 * @author Juergen Hoeller
 * @since 20.01.2004
 * @see java.io.InputStream
 * @see Resource
 * @see InputStreamResource
 * @see ByteArrayResource
 */
// 作为 {@link InputStream} 源的对象的简单接口
//
// <p>这是 Spring 更广泛的 {@link Resource} 接口的基础接口
//
// <p>对于一次性流，{@link InputStreamResource} 可用于任何给定的 {@code InputStream}。
// Spring 的 {@link ByteArrayResource} 或任何基于文件的 {@code Resource} 实现都可以用作具体实例，允许多次读取底层内容流。
// 例如，这使得该接口可用作邮件附件的抽象内容源。
// 只读资源
public interface InputStreamSource {

	/**
	 * Return an {@link InputStream} for the content of an underlying resource.
	 * <p>It is expected that each call creates a <i>fresh</i> stream.
	 * <p>This requirement is particularly important when you consider an API such
	 * as JavaMail, which needs to be able to read the stream multiple times when
	 * creating mail attachments. For such a use case, it is <i>required</i>
	 * that each {@code getInputStream()} call returns a fresh stream.
	 * @return the input stream for the underlying resource (must not be {@code null})
	 * @throws java.io.FileNotFoundException if the underlying resource does not exist
	 * @throws IOException if the content stream could not be opened
	 * @see Resource#isReadable()
	 */
	// 为底层资源的内容返回一个 {@link InputStream}。
	// <p>预计每次调用都会创建一个 <i>fresh<i> 流。
	// <p>当您考虑 JavaMail 等 API 时，此要求尤其重要，该 API 在创建邮件附件时需要能够多次读取流。对于这样的用例，<i>要求<i>每个
	// {@code getInputStream()} 调用返回一个新的流。
	InputStream getInputStream() throws IOException;

}
