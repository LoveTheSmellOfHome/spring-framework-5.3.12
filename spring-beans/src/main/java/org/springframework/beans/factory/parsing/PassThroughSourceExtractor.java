/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.beans.factory.parsing;

import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;

/**
 * Simple {@link SourceExtractor} implementation that just passes
 * the candidate source metadata object through for attachment.
 *
 * <p>Using this implementation means that tools will get raw access to the
 * underlying configuration source metadata provided by the tool.
 *
 * <p>This implementation <strong>should not</strong> be used in a production
 * application since it is likely to keep too much metadata in memory
 * (unnecessarily).
 *
 * @author Rob Harrop
 * @since 2.0
 */
// 简单的SourceExtractor实现，它只是将候选源元数据对象传递给附件。
// 使用此实现意味着工具将获得对工具提供的底层配置源元数据的原始访问权限。
// 此实现不应在生产应用程序中使用，因为它可能在内存中保留过多元数据（不必要）。
public class PassThroughSourceExtractor implements SourceExtractor {

	/**
	 * Simply returns the supplied {@code sourceCandidate} as-is.
	 * @param sourceCandidate the source metadata
	 * @return the supplied {@code sourceCandidate}
	 */
	// 只需按原样返回提供的sourceCandidate 。
	// 形参：sourceCandidate – 源元数据，如果是方法元数据返回 StandardMethodMetadata
	// 返回值：提供的sourceCandidate
	@Override
	public Object extractSource(Object sourceCandidate, @Nullable Resource definingResource) {
		return sourceCandidate;
	}

}
