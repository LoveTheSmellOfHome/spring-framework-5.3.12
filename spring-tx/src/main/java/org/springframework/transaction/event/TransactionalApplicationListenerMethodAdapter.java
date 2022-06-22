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

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.ApplicationListenerMethodAdapter;
import org.springframework.context.event.EventListener;
import org.springframework.context.event.GenericApplicationListener;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;

/**
 * {@link GenericApplicationListener} adapter that delegates the processing of
 * an event to a {@link TransactionalEventListener} annotated method. Supports
 * the exact same features as any regular {@link EventListener} annotated method
 * but is aware of the transactional context of the event publisher.
 *
 * <p>Processing of {@link TransactionalEventListener} is enabled automatically
 * when Spring's transaction management is enabled. For other cases, registering
 * a bean of type {@link TransactionalEventListenerFactory} is required.
 *
 * @author Stephane Nicoll
 * @author Juergen Hoeller
 * @since 5.3
 * @see TransactionalEventListener
 * @see TransactionalApplicationListener
 * @see TransactionalApplicationListenerAdapter
 */
// 将事件处理委托给 TransactionalEventListener 注解方法的 GenericApplicationListener适配器。
// 支持与任何常规 EventListener 注解方法完全相同的功能，但了解事件发布者的事务上下文。
//
// 当启用 Spring 的事务管理时，会自动启用 TransactionalEventListener 的处理。对于其他情况，需要
// 注册 TransactionalEventListenerFactory 类型的 bean。
public class TransactionalApplicationListenerMethodAdapter extends ApplicationListenerMethodAdapter
		implements TransactionalApplicationListener<ApplicationEvent> {

	// 事务事件监听器
	private final TransactionalEventListener annotation;

	// 事务提交阶段
	private final TransactionPhase transactionPhase;

	// 在同步驱动的事件处理上调用回调
	private final List<SynchronizationCallback> callbacks = new CopyOnWriteArrayList<>();


	/**
	 * Construct a new TransactionalApplicationListenerMethodAdapter.
	 * @param beanName the name of the bean to invoke the listener method on
	 * @param targetClass the target class that the method is declared on
	 * @param method the listener method to invoke
	 */
	// 构造一个新的 TransactionalApplicationListenerMethodAdapter。
	// 参形：
	//			beanName – 调用监听器方法的 bean 的名称
	//			targetClass – 声明方法的目标类
	//			method -- 要调用的侦听器方法
	public TransactionalApplicationListenerMethodAdapter(String beanName, Class<?> targetClass, Method method) {
		super(beanName, targetClass, method);
		TransactionalEventListener ann =
				AnnotatedElementUtils.findMergedAnnotation(method, TransactionalEventListener.class);
		if (ann == null) {
			throw new IllegalStateException("No TransactionalEventListener annotation found on method: " + method);
		}
		this.annotation = ann;
		this.transactionPhase = ann.phase();
	}


	@Override
	public TransactionPhase getTransactionPhase() {
		return this.transactionPhase;
	}

	// 添加同步回调
	@Override
	public void addCallback(SynchronizationCallback callback) {
		Assert.notNull(callback, "SynchronizationCallback must not be null");
		this.callbacks.add(callback);
	}


	// 发布事件
	@Override
	public void onApplicationEvent(ApplicationEvent event) {
		if (TransactionSynchronizationManager.isSynchronizationActive() &&
				TransactionSynchronizationManager.isActualTransactionActive()) {
			// 为当前线程注册一个新的同步事务
			TransactionSynchronizationManager.registerSynchronization(
					new TransactionalApplicationListenerSynchronization<>(event, this, this.callbacks));
		}
		else if (this.annotation.fallbackExecution()) {
			if (this.annotation.phase() == TransactionPhase.AFTER_ROLLBACK && logger.isWarnEnabled()) {
				logger.warn("Processing " + event + " as a fallback execution on AFTER_ROLLBACK phase");
			}
			// 处理事件
			processEvent(event);
		}
		else {
			// No transactional event execution at all
			// 根本没有事务性事件执行
			if (logger.isDebugEnabled()) {
				logger.debug("No transaction is active - skipping " + event);
			}
		}
	}

}
