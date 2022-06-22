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
import org.springframework.core.ResolvableType;

/**
 * Extended variant of the standard {@link ApplicationListener} interface,
 * exposing further metadata such as the supported event and source type.
 *
 * <p>As of Spring Framework 4.2, this interface supersedes the Class-based
 * {@link SmartApplicationListener} with full handling of generic event types.
 * As of 5.3.5, it formally extends {@link SmartApplicationListener}, adapting
 * {@link #supportsEventType(Class)} to {@link #supportsEventType(ResolvableType)}
 * with a default method.
 *
 * @author Stephane Nicoll
 * @author Juergen Hoeller
 * @since 4.2
 * @see SmartApplicationListener
 * @see GenericApplicationListenerAdapter
 */
// 标准 ApplicationListener 接口的扩展变体，公开更多元数据，例如支持的事件和源类型。
//
// 从 Spring Framework 4.2 开始，该接口取代了基于类的 SmartApplicationListener 并全面处理了通用事件类型。
// 从 5.3.5 开始，它正式扩展 SmartApplicationListener ，使用默认方法将 supportsEventType(Class) 适配
// 为 supportsEventType(ResolvableType) 。
public interface GenericApplicationListener extends SmartApplicationListener {

	/**
	 * Overrides {@link SmartApplicationListener#supportsEventType(Class)} with
	 * delegation to {@link #supportsEventType(ResolvableType)}.
	 */
	// 覆盖 SmartApplicationListener.supportsEventType(Class) 并委托至 supportsEventType(ResolvableType)
	@Override
	default boolean supportsEventType(Class<? extends ApplicationEvent> eventType) {
		return supportsEventType(ResolvableType.forClass(eventType));
	}

	/**
	 * Determine whether this listener actually supports the given event type.
	 * @param eventType the event type (never {@code null})
	 */
	// 确定此侦听器是否真正支持给定的事件类型。
	// 参形：eventType – 事件类型（从不为null ）
	boolean supportsEventType(ResolvableType eventType);

}
