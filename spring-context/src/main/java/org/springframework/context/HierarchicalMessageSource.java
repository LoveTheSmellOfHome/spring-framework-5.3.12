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

package org.springframework.context;

import org.springframework.lang.Nullable;

/**
 * Sub-interface of MessageSource to be implemented by objects that
 * can resolve messages hierarchically.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
// MessageSource 的子接口由可以层次性消息解析的对象实现
public interface HierarchicalMessageSource extends MessageSource {

	/**
	 * Set the parent that will be used to try to resolve messages
	 * that this object can't resolve.
	 * @param parent the parent MessageSource that will be used to
	 * resolve messages that this object can't resolve.
	 * May be {@code null}, in which case no further resolution is possible.
	 */
	// 设置将用于尝试解析此对象无法解析的消息的父级
	// @param parent 将用于解析此对象无法解析的消息的父 MessageSource
	// 可能是 {@code null}，在这种情况下无法进一步解析
	void setParentMessageSource(@Nullable MessageSource parent);

	/**
	 * Return the parent of this MessageSource, or {@code null} if none.
	 */
	// 返回此 MessageSource 的父级，如果没有，则返回 {@code null}
	@Nullable
	MessageSource getParentMessageSource();

}
