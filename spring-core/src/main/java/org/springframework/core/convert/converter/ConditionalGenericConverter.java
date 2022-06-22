/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.core.convert.converter;

import org.springframework.core.convert.TypeDescriptor;

/**
 * A {@link GenericConverter} that may conditionally execute based on attributes
 * of the {@code source} and {@code target} {@link TypeDescriptor}.
 *
 * <p>See {@link ConditionalConverter} for details.
 *
 * @author Keith Donald
 * @author Phillip Webb
 * @since 3.0
 * @see GenericConverter
 * @see ConditionalConverter
 */
// 可以根据 {@code source} 和 {@code target} {@link TypeDescriptor} 的属性有条件地执行的 {@link GenericConverter}。
// <p>有关详细信息，请参阅 {@link ConditionalConverter}。
// 在 GenericConverter 基础上增加了前置判断，判断 source类型和 target 类型是否能被当前转换器转换
// 综合(条件，复合)类型转换器
public interface ConditionalGenericConverter extends GenericConverter, ConditionalConverter {

}
