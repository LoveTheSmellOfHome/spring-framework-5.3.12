/*
 * Copyright 2002-2016 the original author or authors.
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
 * Simple strategy allowing tools to control how source metadata is attached
 * to the bean definition metadata.
 *
 * <p>Configuration parsers <strong>may</strong> provide the ability to attach
 * source metadata during the parse phase. They will offer this metadata in a
 * generic format which can be further modified by a {@link SourceExtractor}
 * before being attached to the bean definition metadata.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 2.0
 * @see org.springframework.beans.BeanMetadataElement#getSource()
 * @see org.springframework.beans.factory.config.BeanDefinition
 */
// 允许工具控制源元数据如何附加到 bean 定义元数据的简单策略。
// 配置解析器可以提供在解析阶段附加源元数据的能力。 他们将以通用格式提供此元数据，在附加到 bean 定义元数据之前，
// SourceExtractor可以进一步修改该格式。
@FunctionalInterface
public interface SourceExtractor {

	/**
	 * Extract the source metadata from the candidate object supplied
	 * by the configuration parser.
	 * @param sourceCandidate the original source metadata (never {@code null})
	 * @param definingResource the resource that defines the given source object
	 * (may be {@code null})
	 * @return the source metadata object to store (may be {@code null})
	 */
	// 从配置解析器提供的候选对象中提取源元数据。
	// 形参：
	// 			sourceCandidate – 原始源元数据（从不为null ）
	// 			definitionResource – 定义给定源对象的资源（可能为null ）
	//返回值：
	//			要存储的源元数据对象（可能为null ）
	@Nullable
	Object extractSource(Object sourceCandidate, @Nullable Resource definingResource);

}
