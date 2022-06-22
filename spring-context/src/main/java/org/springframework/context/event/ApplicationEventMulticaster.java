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

import java.util.function.Predicate;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.ResolvableType;
import org.springframework.lang.Nullable;

/**
 * Interface to be implemented by objects that can manage a number of
 * {@link ApplicationListener} objects and publish events to them.
 *
 * <p>An {@link org.springframework.context.ApplicationEventPublisher}, typically
 * a Spring {@link org.springframework.context.ApplicationContext}, can use an
 * {@code ApplicationEventMulticaster} as a delegate for actually publishing events.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @see ApplicationListener
 */
// 由可以管理多个 {@link ApplicationListener} 对象并向它们发布事件的对象实现的接口
//
// p>一个 {@link org.springframework.context.ApplicationEventPublisher}，
// 通常是一个 Spring {@link org.springframework.context.ApplicationContext}，
// 可以使用一个 {@code ApplicationEventMulticaster} 作为实际发布事件的委托
public interface ApplicationEventMulticaster {

	/**
	 * Add a listener to be notified of all events.
	 * @param listener the listener to add
	 * @see #removeApplicationListener(ApplicationListener)
	 * @see #removeApplicationListeners(Predicate)
	 */
	// 添加一个监听器以接收所有事件的通知
	void addApplicationListener(ApplicationListener<?> listener);

	/**
	 * Add a listener bean to be notified of all events.
	 * @param listenerBeanName the name of the listener bean to add
	 * @see #removeApplicationListenerBean(String)
	 * @see #removeApplicationListenerBeans(Predicate)
	 */
	// 添加一个监听器 bean，以接收所有事件的通知
	void addApplicationListenerBean(String listenerBeanName);

	/**
	 * Remove a listener from the notification list.
	 * @param listener the listener to remove
	 * @see #addApplicationListener(ApplicationListener)
	 * @see #removeApplicationListeners(Predicate)
	 */
	// 从通知列表中删除监听器
	void removeApplicationListener(ApplicationListener<?> listener);

	/**
	 * Remove a listener bean from the notification list.
	 * @param listenerBeanName the name of the listener bean to remove
	 * @see #addApplicationListenerBean(String)
	 * @see #removeApplicationListenerBeans(Predicate)
	 */
	// 从通知列表中删除监听器 bean。
	void removeApplicationListenerBean(String listenerBeanName);

	/**
	 * Remove all matching listeners from the set of registered
	 * {@code ApplicationListener} instances (which includes adapter classes
	 * such as {@link ApplicationListenerMethodAdapter}, e.g. for annotated
	 * {@link EventListener} methods).
	 * <p>Note: This just applies to instance registrations, not to listeners
	 * registered by bean name.
	 * @param predicate the predicate to identify listener instances to remove,
	 * e.g. checking {@link SmartApplicationListener#getListenerId()}
	 * @since 5.3.5
	 * @see #addApplicationListener(ApplicationListener)
	 * @see #removeApplicationListener(ApplicationListener)
	 */
	// 从已注册的 {@code ApplicationListener} 实例集合中删除所有匹配的监听器（
	// 其中包括适配器类，例如 {@link ApplicationListenerMethodAdapter}，例如
	// 用于带注解的 {@link EventListener} 方法）。
	// <p>注意：这仅适用于实例注册，不适用于通过 bean 名称注册的监听器。
	void removeApplicationListeners(Predicate<ApplicationListener<?>> predicate);

	/**
	 * Remove all matching listener beans from the set of registered
	 * listener bean names (referring to bean classes which in turn
	 * implement the {@link ApplicationListener} interface directly).
	 * <p>Note: This just applies to bean name registrations, not to
	 * programmatically registered {@code ApplicationListener} instances.
	 * @param predicate the predicate to identify listener bean names to remove
	 * @since 5.3.5
	 * @see #addApplicationListenerBean(String)
	 * @see #removeApplicationListenerBean(String)
	 */
	// 从注册的监听器 bean 名称集中删除所有匹配的监听器 bean（指的是 bean 类，这些类又直接实现了
	// {@link ApplicationListener} 接口）。
	// <p>注意：这仅适用于 bean 名称注册，不适用于以编程方式注册的 {@code ApplicationListener} 实例。
	// @param predicate 用于识别要删除的监听器 bean 名称的谓词
	void removeApplicationListenerBeans(Predicate<String> predicate);

	/**
	 * Remove all listeners registered with this multicaster.
	 * <p>After a remove call, the multicaster will perform no action
	 * on event notification until new listeners are registered.
	 * @see #removeApplicationListeners(Predicate)
	 */
	// 删除所有注册到此广播器的监听器。
	// <p>在删除调用后，广播器不会对事件通知执行任何操作，直到注册新的监听器。
	// @see removeApplicationListeners(Predicate)
	void removeAllListeners();

	/**
	 * Multicast the given application event to appropriate listeners.
	 * <p>Consider using {@link #multicastEvent(ApplicationEvent, ResolvableType)}
	 * if possible as it provides better support for generics-based events.
	 * @param event the event to multicast
	 */
	// 将给定的应用程序事件多播到适当的监听器。
	// <p>如果可能，请考虑使用 {@link multicastEvent(ApplicationEvent, ResolvableType)}，
	// 因为它为基于泛型的事件提供更好的支持。
	// @param event 要广播的事件
	void multicastEvent(ApplicationEvent event);

	/**
	 * Multicast the given application event to appropriate listeners.
	 * <p>If the {@code eventType} is {@code null}, a default type is built
	 * based on the {@code event} instance.
	 * @param event the event to multicast
	 * @param eventType the type of event (can be {@code null})
	 * @since 4.2
	 */
	// 将给定的应用程序事件广播到适当的监听器。
	// <p>如果 {@code eventType} 为 {@code null}，则基于 {@code event} 实例构建默认类型。
	// @param event 要广播的事件
	// @param eventType 事件类型（可以是{@code null}）
	void multicastEvent(ApplicationEvent event, @Nullable ResolvableType eventType);

}
