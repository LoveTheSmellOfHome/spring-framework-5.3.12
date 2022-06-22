/*
 * Copyright 2002-2020 the original author or authors.
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

import java.lang.reflect.Constructor;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * {@link MethodInterceptor Interceptor} that publishes an
 * {@code ApplicationEvent} to all {@code ApplicationListeners}
 * registered with an {@code ApplicationEventPublisher} after each
 * <i>successful</i> method invocation.
 *
 * <p>Note that this interceptor is only capable of publishing <i>stateless</i>
 * events configured via the
 * {@link #setApplicationEventClass "applicationEventClass"} property.
 *
 * @author Dmitriy Kopylenko
 * @author Juergen Hoeller
 * @author Rick Evans
 * @see #setApplicationEventClass
 * @see org.springframework.context.ApplicationEvent
 * @see org.springframework.context.ApplicationListener
 * @see org.springframework.context.ApplicationEventPublisher
 * @see org.springframework.context.ApplicationContext
 */
// 每次成功调用方法后，将ApplicationEventPublisher发布到向ApplicationEvent注册的所有ApplicationListeners的Interceptor 。
// 请注意，此拦截器只能发布通过"applicationEventClass"属性配置的无状态事件。
//
// Spring AOP 在 Spring 事件（Events）的核心 API，利用 Spring 事件做了一些黑科技
// 特性描述：
//		  当 Spring AOP 代理 Bean 中的 JoinPoint 方法执行后，Spring ApplicationContext 将发布一个
//		  自定义事件（ApplicationEvent 子类，可以任意定义）
// 使用限制：
//		  EventPublicationInterceptor 关联的 ApplicationEvent 子类必须存在单参数的构造器，单构造器参数必须是 Object,可以重载
//		  EventPublicationInterceptor 需要被声明为 Spring Bean，它里边有一些生命周期的操作
//
public class EventPublicationInterceptor
		implements MethodInterceptor, ApplicationEventPublisherAware, InitializingBean {

	@Nullable
	private Constructor<?> applicationEventClassConstructor;

	@Nullable
	private ApplicationEventPublisher applicationEventPublisher;


	/**
	 * Set the application event class to publish.
	 * <p>The event class <b>must</b> have a constructor with a single
	 * {@code Object} argument for the event source. The interceptor
	 * will pass in the invoked object.
	 * @throws IllegalArgumentException if the supplied {@code Class} is
	 * {@code null} or if it is not an {@code ApplicationEvent} subclass or
	 * if it does not expose a constructor that takes a single {@code Object} argument
	 */
	// 将应用程序事件类设置为发布。
	// 事件类必须有一个构造函数，其中包含事件源的单个Object参数。拦截器将传入调用的对象。
	// 抛出：
	// IllegalArgumentException – 如果提供的Class为null ，或者它不是ApplicationEvent子类，
	// 或者它没有公开采用单个Object参数的构造函数
	// 在我们初始化 EventPublicationInterceptor 时候，首先必须设置 applicationEventClass，否则在
	// afterPropertiesSet() 调用时候就会抛出异常。
	//
	// 关联目标（自定义） 事件类型
	public void setApplicationEventClass(Class<?> applicationEventClass) {
		if (ApplicationEvent.class == applicationEventClass ||
				!ApplicationEvent.class.isAssignableFrom(applicationEventClass)) {
			throw new IllegalArgumentException("'applicationEventClass' needs to extend ApplicationEvent");
		}
		try {
			// 构造器参数必须是单参数且是 Object 类型的限制
			this.applicationEventClassConstructor = applicationEventClass.getConstructor(Object.class);
		}
		catch (NoSuchMethodException ex) {
			throw new IllegalArgumentException("ApplicationEvent class [" +
					applicationEventClass.getName() + "] does not have the required Object constructor: " + ex);
		}
	}

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
		// 生命周期回调：关联 applicationEventPublisher
		this.applicationEventPublisher = applicationEventPublisher;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		// 生命周期回调：applicationEventClassConstructor 的非空判断
		if (this.applicationEventClassConstructor == null) {
			throw new IllegalArgumentException("Property 'applicationEventClass' is required");
		}
	}


	@Override
	@Nullable
	public Object invoke(MethodInvocation invocation) throws Throwable {
		// 目标方法执行
		Object retVal = invocation.proceed();

		Assert.state(this.applicationEventClassConstructor != null, "No ApplicationEvent class set");
		// 方法执行完后，获取自定义的单参数，且参数类型
		ApplicationEvent event = (ApplicationEvent)
				this.applicationEventClassConstructor.newInstance(invocation.getThis());

		Assert.state(this.applicationEventPublisher != null, "No ApplicationEventPublisher available");
		// 发送事件
		this.applicationEventPublisher.publishEvent(event);

		return retVal;
	}

}
