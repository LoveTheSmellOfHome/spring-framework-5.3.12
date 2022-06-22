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

package org.springframework.transaction.event;

import java.util.function.Consumer;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.PayloadApplicationEvent;
import org.springframework.core.Ordered;
import org.springframework.lang.Nullable;

/**
 * An {@link ApplicationListener} that is invoked according to a {@link TransactionPhase}.
 * This is a programmatic equivalent of the {@link TransactionalEventListener} annotation.
 *
 * <p>Adding {@link org.springframework.core.Ordered} to your listener implementation
 * allows you to prioritize that listener amongst other listeners running before or after
 * transaction completion.
 *
 * <p><b>NOTE: Transactional event listeners only work with thread-bound transactions
 * managed by a {@link org.springframework.transaction.PlatformTransactionManager
 * PlatformTransactionManager}.</b> A reactive transaction managed by a
 * {@link org.springframework.transaction.ReactiveTransactionManager ReactiveTransactionManager}
 * uses the Reactor context instead of thread-local variables, so from the perspective of
 * an event listener, there is no compatible active transaction that it can participate in.
 *
 * @author Juergen Hoeller
 * @author Oliver Drotbohm
 * @since 5.3
 * @param <E> the specific {@code ApplicationEvent} subclass to listen to
 * @see TransactionalEventListener
 * @see TransactionalApplicationListenerAdapter
 * @see #forPayload
 */
// 根据 TransactionPhase 调用的 ApplicationListener 。这是 TransactionalEventListener 注解的编程等效项。
//
// 将 Ordered 添加到您的侦听器实现允许您在事务完成之前或之后运行的其他侦听器中优先考虑该侦听器。
//
// 注意：事务事件侦听器仅适用于由 PlatformTransactionManager 管理的线程绑定事务。
// 由 ReactiveTransactionManager 管理的反应式事务使用 Reactor 上下文而不是线程局部变量，
// 因此从事件侦听器的角度来看，它没有可以参与的兼容的活动事务。
public interface TransactionalApplicationListener<E extends ApplicationEvent>
		extends ApplicationListener<E>, Ordered {

	/**
	 * Return the execution order within transaction synchronizations.
	 * <p>Default is {@link Ordered#LOWEST_PRECEDENCE}.
	 * @see org.springframework.transaction.support.TransactionSynchronization#getOrder()
	 */
	// 返回事务同步中的执行顺序。
	// 默认为 Ordered.LOWEST_PRECEDENCE 。
	@Override
	default int getOrder() {
		return Ordered.LOWEST_PRECEDENCE;
	}

	/**
	 * Return an identifier for the listener to be able to refer to it individually.
	 * <p>It might be necessary for specific completion callback implementations
	 * to provide a specific id, whereas for other scenarios an empty String
	 * (as the common default value) is acceptable as well.
	 * @see org.springframework.context.event.SmartApplicationListener#getListenerId()
	 * @see TransactionalEventListener#id
	 * @see #addCallback
	 */
	// 为侦听器返回一个标识符，以便能够单独引用它。
	// 特定的完成回调实现可能需要提供特定的 id，而对于其他情况，空字符串（作为常见的默认值）也是可以接受的。
	default String getListenerId() {
		return "";
	}

	/**
	 * Return the {@link TransactionPhase} in which the listener will be invoked.
	 * <p>The default phase is {@link TransactionPhase#AFTER_COMMIT}.
	 */
	// 返回将在其中调用侦听器的 TransactionPhase 。
	// 默认阶段是 TransactionPhase.AFTER_COMMIT 。
	default TransactionPhase getTransactionPhase() {
		return TransactionPhase.AFTER_COMMIT;
	}

	/**
	 * Add a callback to be invoked on processing within transaction synchronization,
	 * i.e. when {@link #processEvent} is being triggered during actual transactions.
	 * @param callback the synchronization callback to apply
	 */
	// 添加在事务同步中的处理时调用的回调，即在实际事务期间触发 processEvent 时。
	// 参形：
	//			callback – 要应用的同步回调
	void addCallback(SynchronizationCallback callback);

	/**
	 * Immediately process the given {@link ApplicationEvent}. In contrast to
	 * {@link #onApplicationEvent(ApplicationEvent)}, a call to this method will
	 * directly process the given event without deferring it to the associated
	 * {@link #getTransactionPhase() transaction phase}.
	 * @param event the event to process through the target listener implementation
	 */
	// 立即处理给定的 ApplicationEvent 。与 onApplicationEvent(ApplicationEvent) 相比，调用此方法
	// 将直接处理给定事件，而不会将其推迟到关联的 transaction phase 。
	// 参形：
	//				event - 要通过目标侦听器实现处理的事件
	void processEvent(E event);


	/**
	 * Create a new {@code TransactionalApplicationListener} for the given payload consumer,
	 * to be applied in the default phase {@link TransactionPhase#AFTER_COMMIT}.
	 * @param consumer the event payload consumer
	 * @param <T> the type of the event payload
	 * @return a corresponding {@code TransactionalApplicationListener} instance
	 * @see PayloadApplicationEvent#getPayload()
	 * @see TransactionalApplicationListenerAdapter
	 */
	// 为给定的有效负载使用者创建一个新的TransactionalApplicationListener ，以在默认阶段TransactionPhase.AFTER_COMMIT中应用。
	// 参形：
	//			consumer - 事件有效负载消费者
	// 返回值：
	//			对应的 TransactionalApplicationListener实例
	static <T> TransactionalApplicationListener<PayloadApplicationEvent<T>> forPayload(Consumer<T> consumer) {
		return forPayload(TransactionPhase.AFTER_COMMIT, consumer);
	}

	/**
	 * Create a new {@code TransactionalApplicationListener} for the given payload consumer.
	 * @param phase the transaction phase in which to invoke the listener
	 * @param consumer the event payload consumer
	 * @param <T> the type of the event payload
	 * @return a corresponding {@code TransactionalApplicationListener} instance
	 * @see PayloadApplicationEvent#getPayload()
	 * @see TransactionalApplicationListenerAdapter
	 */
	// 为给定的有效负载使用者创建一个新的TransactionalApplicationListener 。
	// 参形：
	//			phase- 调用侦听器的事务阶段
	//			consumer- 事件有效负载消费者
	// 返回值：
	//			对应的 TransactionalApplicationListener 实例
	static <T> TransactionalApplicationListener<PayloadApplicationEvent<T>> forPayload(
			TransactionPhase phase, Consumer<T> consumer) {

		TransactionalApplicationListenerAdapter<PayloadApplicationEvent<T>> listener =
				new TransactionalApplicationListenerAdapter<>(event -> consumer.accept(event.getPayload()));
		listener.setTransactionPhase(phase);
		return listener;
	}


	/**
	 * Callback to be invoked on synchronization-driven event processing,
	 * wrapping the target listener invocation ({@link #processEvent}).
	 *
	 * @see #addCallback
	 * @see #processEvent
	 */
	// 在同步驱动的事件处理上调用回调，包装目标监听器调用（ processEvent ）。
	interface SynchronizationCallback {

		/**
		 * Called before transactional event listener invocation.
		 * @param event the event that transaction synchronization is about to process
		 */
		// 在事务事件侦听器调用之前调用。
		// 参形：
		//			event – 事务同步即将处理的事件
		default void preProcessEvent(ApplicationEvent event) {
		}

		/**
		 * Called after a transactional event listener invocation.
		 * @param event the event that transaction synchronization finished processing
		 * @param ex an exception that occurred during listener invocation, if any
		 */
		// 在事务事件侦听器调用之后调用。
		// 参形：
		//			event - 事务同步完成处理的事件
		//			ex – 在侦听器调用期间发生的异常（如果有）
		default void postProcessEvent(ApplicationEvent event, @Nullable Throwable ex) {
		}
	}

}
