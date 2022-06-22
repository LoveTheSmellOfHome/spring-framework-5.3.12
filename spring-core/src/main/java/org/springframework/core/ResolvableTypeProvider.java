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

package org.springframework.core;

import org.springframework.lang.Nullable;

/**
 * Any object can implement this interface to provide its actual {@link ResolvableType}.
 *
 * <p>Such information is very useful when figuring out if the instance matches a generic
 * signature as Java does not convey the signature at runtime.
 *
 * <p>Users of this interface should be careful in complex hierarchy scenarios, especially
 * when the generic type signature of the class changes in sub-classes. It is always
 * possible to return {@code null} to fallback on a default behavior.
 *
 * @author Stephane Nicoll
 * @since 4.2
 */
// 任何对象都可以实现此接口以提供其实际的 {@link ResolvableType}。
//
// <p>此类信息在确定实例是否与泛型签名匹配时非常有用，因为 Java 不会在运行时传达签名。
//
// <p>这个接口的用户在复杂的层次结构场景中应该小心，特别是当类的泛型类型签名在子类中发生变化时。
// 始终可以返回 {@code null} 以回退默认行为。
public interface ResolvableTypeProvider {

	/**
	 * Return the {@link ResolvableType} describing this instance
	 * (or {@code null} if some sort of default should be applied instead).
	 */
	// 返回描述此实例的 {@link ResolvableType}（如果应改为应用某种默认值，则返回 {@code null}）。
	@Nullable
	ResolvableType getResolvableType();

}
