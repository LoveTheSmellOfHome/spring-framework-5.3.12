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

package org.springframework.context.event;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.lang.Nullable;

/**
 * Extended variant of the standard {@link ApplicationListener} interface,
 * exposing further metadata such as the supported event and source type.
 *
 * <p>For full introspection of generic event types, consider implementing
 * the {@link GenericApplicationListener} interface instead.
 *
 * @author Juergen Hoeller
 * @since 3.0
 * @see GenericApplicationListener
 * @see GenericApplicationListenerAdapter
 */
// 标准 ApplicationListener 接口的扩展变体，公开更多元数据，例如支持的事件和源类型。
//
// 要全面了解通用事件类型，请考虑实现 GenericApplicationListener 接口。
public interface SmartApplicationListener extends ApplicationListener<ApplicationEvent>, Ordered {

	/**
	 * Determine whether this listener actually supports the given event type.
	 * @param eventType the event type (never {@code null})
	 */
	// 确定此侦听器是否真正支持给定的事件类型。
	// 参形：eventType – 事件类型（从不为null ）
	boolean supportsEventType(Class<? extends ApplicationEvent> eventType);

	/**
	 * Determine whether this listener actually supports the given source type.
	 * <p>The default implementation always returns {@code true}.
	 * @param sourceType the source type, or {@code null} if no source
	 */
	// 确定此侦听器是否真正支持给定的源类型。
	// 默认实现总是返回true 。
	// 参形：sourceType – 源类型，如果没有源，则为nul
	default boolean supportsSourceType(@Nullable Class<?> sourceType) {
		return true;
	}

	/**
	 * Determine this listener's order in a set of listeners for the same event.
	 * <p>The default implementation returns {@link #LOWEST_PRECEDENCE}.
	 */
	// 在同一事件的一组侦听器中确定此侦听器的顺序。
	// 默认实现返回 LOWEST_PRECEDENCE 。
	@Override
	default int getOrder() {
		return LOWEST_PRECEDENCE;
	}

	/**
	 * Return an optional identifier for the listener.
	 * <p>The default value is an empty String.
	 * @since 5.3.5
	 * @see EventListener#id
	 * @see ApplicationEventMulticaster#removeApplicationListeners
	 */
	// 返回侦听器的可选标识符。
	// 默认值为空字符串
	default String getListenerId() {
		return "";
	}

}
