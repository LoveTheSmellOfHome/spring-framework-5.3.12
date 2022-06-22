/*
 * Copyright 2002-2018 the original author or authors.
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

/**
 * A common interface defining methods for start/stop lifecycle control.
 * The typical use case for this is to control asynchronous processing.
 * <b>NOTE: This interface does not imply specific auto-startup semantics.
 * Consider implementing {@link SmartLifecycle} for that purpose.</b>
 *
 * <p>Can be implemented by both components (typically a Spring bean defined in a
 * Spring context) and containers  (typically a Spring {@link ApplicationContext}
 * itself). Containers will propagate start/stop signals to all components that
 * apply within each container, e.g. for a stop/restart scenario at runtime.
 *
 * <p>Can be used for direct invocations or for management operations via JMX.
 * In the latter case, the {@link org.springframework.jmx.export.MBeanExporter}
 * will typically be defined with an
 * {@link org.springframework.jmx.export.assembler.InterfaceBasedMBeanInfoAssembler},
 * restricting the visibility of activity-controlled components to the Lifecycle
 * interface.
 *
 * <p>Note that the present {@code Lifecycle} interface is only supported on
 * <b>top-level singleton beans</b>. On any other component, the {@code Lifecycle}
 * interface will remain undetected and hence ignored. Also, note that the extended
 * {@link SmartLifecycle} interface provides sophisticated integration with the
 * application context's startup and shutdown phases.
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see SmartLifecycle
 * @see ConfigurableApplicationContext
 * @see org.springframework.jms.listener.AbstractMessageListenerContainer
 * @see org.springframework.scheduling.quartz.SchedulerFactoryBean
 */
// 定义 start/stop 生命周期控制方法的通用接口。典型的案例是控制异步处理。
// <b>注意：此接口不暗示特定的自动启动语义。实现自动启动语义的请实现 {@link SmartLifecycle}。<b>
//
// <p>可以由组件（通常是定义在 Spring 上下文中的 Spring bean）和容器（通常是 Spring {@link ApplicationContext} 本身）来实现。
// 容器会将 start/stop 信号传播到适用于每个容器内的所有组件，例如用于在运行时停止重启的场景
//
// <p>可用于直接调用或通过 JMX 进行管理操作。在后一种情况下，{@link org.springframework.jmx.export.MBeanExporter}
// 通常会使用 {@link org.springframework.jmx.export.assembler.InterfaceBasedMBeanInfoAssembler} 定义，
// 将活动控制组件的可见性限制为生命周期接口。
//
// <p>请注意，目前的 {@code Lifecycle} 接口仅在 <b>顶级单例 bean<b> 上受支持。在任何其他组件上，
// {@code Lifecycle} 接口将保持未被检测到，因此被忽略。另请注意，扩展的 {@link SmartLifecycle}
// 接口提供了与应用程序上下文的启动和关闭阶段的复杂集成。
//
// 生命周期接口
public interface Lifecycle {

	/**
	 * Start this component.
	 * <p>Should not throw an exception if the component is already running.
	 * <p>In the case of a container, this will propagate the start signal to all
	 * components that apply.
	 * @see SmartLifecycle#isAutoStartup()
	 */
	// 启动这个组件。
	// <p>如果组件已经在运行，则不应抛出异常。
	// <p>在容器的情况下，这会将启动信号传播到所有适用的组件。
	void start();

	/**
	 * Stop this component, typically in a synchronous fashion, such that the component is
	 * fully stopped upon return of this method. Consider implementing {@link SmartLifecycle}
	 * and its {@code stop(Runnable)} variant when asynchronous stop behavior is necessary.
	 * <p>Note that this stop notification is not guaranteed to come before destruction:
	 * On regular shutdown, {@code Lifecycle} beans will first receive a stop notification
	 * before the general destruction callbacks are being propagated; however, on hot
	 * refresh during a context's lifetime or on aborted refresh attempts, a given bean's
	 * destroy method will be called without any consideration of stop signals upfront.
	 * <p>Should not throw an exception if the component is not running (not started yet).
	 * <p>In the case of a container, this will propagate the stop signal to all components
	 * that apply.
	 * @see SmartLifecycle#stop(Runnable)
	 * @see org.springframework.beans.factory.DisposableBean#destroy()
	 */
	// 停止此组件，通常以同步方式停止，以便在返回此方法时完全停止该组件。当需要异步停止行为时，
	// 考虑实施 {@link SmartLifecycle} 及其 {@code stop(Runnable)} 变体。
	// <p>请注意，此停止通知不能保证在销毁之前出现：在常规关闭时，{@code Lifecycle} bean 将在传播一般销毁回调之前首先收到停止通知；
	// 然而，在上下文生命周期中的热刷新或中止刷新尝试时，将调用给定 bean 的 destroy 方法，而无需预先考虑停止信号。
	// <p>如果组件未运行（尚未启动），则不应抛出异常。 <p>在容器的情况下，这会将停止信号传播到所有适用的组件。
	void stop();

	/**
	 * Check whether this component is currently running.
	 * <p>In the case of a container, this will return {@code true} only if <i>all</i>
	 * components that apply are currently running.
	 * @return whether the component is currently running
	 */
	// 检查此组件当前是否正在运行。
	// <p>在容器的情况下，仅当应用的 <i>所有<i> 组件当前正在运行时，才会返回 {@code true}。
	boolean isRunning();

}
