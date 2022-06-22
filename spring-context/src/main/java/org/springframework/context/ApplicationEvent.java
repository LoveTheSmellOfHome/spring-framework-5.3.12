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

package org.springframework.context;

import java.time.Clock;
import java.util.EventObject;

/**
 * Class to be extended by all application events. Abstract as it
 * doesn't make sense for generic events to be published directly.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see org.springframework.context.ApplicationListener
 * @see org.springframework.context.event.EventListener
 */
// 由所有应用程序事件扩展的类(自定义 Spring 事件的公共父类)。抽象，因为直接发布通用事件没有意义。
// ApplicationEvent 抽象类继承了 EventObject 普通类
public abstract class ApplicationEvent extends EventObject {

	/** use serialVersionUID from Spring 1.2 for interoperability. */
	private static final long serialVersionUID = 7099057708183571937L;

	/** System time when the event happened. */
	// 事件构造的时候发生的事件
	private final long timestamp;


	/**
	 * Create a new {@code ApplicationEvent} with its {@link #getTimestamp() timestamp}
	 * set to {@link System#currentTimeMillis()}.
	 * @param source the object on which the event initially occurred or with
	 * which the event is associated (never {@code null})
	 * @see #ApplicationEvent(Object, Clock)
	 */
	// 创建一个新的ApplicationEvent并将其timestamp设置为System.currentTimeMillis() 。
	// 参形：
	//			source – 事件最初发生的对象或与事件相关联的对象（从不为null ）
	// 请参阅：
	//			ApplicationEvent(Object, Clock)
	public ApplicationEvent(Object source) {
		super(source);
		this.timestamp = System.currentTimeMillis();
	}

	/**
	 * Create a new {@code ApplicationEvent} with its {@link #getTimestamp() timestamp}
	 * set to the value returned by {@link Clock#millis()} in the provided {@link Clock}.
	 * <p>This constructor is typically used in testing scenarios.
	 * @param source the object on which the event initially occurred or with
	 * which the event is associated (never {@code null})
	 * @param clock a clock which will provide the timestamp
	 * @since 5.3.8
	 * @see #ApplicationEvent(Object)
	 */
	// 创建一个新的 {@code ApplicationEvent}，其 {@link getTimestamp() 时间戳} 设置为
	// {@link Clockmillis()} 在提供的 {@link Clock} 中返回的值。
	// <p>此构造函数通常用于测试场景
	// @param source 事件最初发生的对象或与事件关联的对象（永远不会{@code null}）
	// @param clock 提供时间戳的时钟
	public ApplicationEvent(Object source, Clock clock) {
		super(source);
		this.timestamp = clock.millis();
	}


	/**
	 * Return the time in milliseconds when the event occurred.
	 * @see #ApplicationEvent(Object)
	 * @see #ApplicationEvent(Object, Clock)
	 */
	// 返回事件发生的时间（以毫秒为单位）
	public final long getTimestamp() {
		return this.timestamp;
	}

}
