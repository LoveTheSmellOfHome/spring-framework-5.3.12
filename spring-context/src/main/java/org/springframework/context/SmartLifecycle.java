/*
 * Copyright 2002-2019 the original author or authors.
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
 * An extension of the {@link Lifecycle} interface for those objects that require
 * to be started upon {@code ApplicationContext} refresh and/or shutdown in a
 * particular order.
 *
 * <p>The {@link #isAutoStartup()} return value indicates whether this object should
 * be started at the time of a context refresh. The callback-accepting
 * {@link #stop(Runnable)} method is useful for objects that have an asynchronous
 * shutdown process. Any implementation of this interface <i>must</i> invoke the
 * callback's {@code run()} method upon shutdown completion to avoid unnecessary
 * delays in the overall {@code ApplicationContext} shutdown.
 *
 * <p>This interface extends {@link Phased}, and the {@link #getPhase()} method's
 * return value indicates the phase within which this {@code Lifecycle} component
 * should be started and stopped. The startup process begins with the <i>lowest</i>
 * phase value and ends with the <i>highest</i> phase value ({@code Integer.MIN_VALUE}
 * is the lowest possible, and {@code Integer.MAX_VALUE} is the highest possible).
 * The shutdown process will apply the reverse order. Any components with the
 * same value will be arbitrarily ordered within the same phase.
 *
 * <p>Example: if component B depends on component A having already started,
 * then component A should have a lower phase value than component B. During
 * the shutdown process, component B would be stopped before component A.
 *
 * <p>Any explicit "depends-on" relationship will take precedence over the phase
 * order such that the dependent bean always starts after its dependency and
 * always stops before its dependency.
 *
 * <p>Any {@code Lifecycle} components within the context that do not also
 * implement {@code SmartLifecycle} will be treated as if they have a phase
 * value of {@code 0}. This allows a {@code SmartLifecycle} component to start
 * before those {@code Lifecycle} components if the {@code SmartLifecycle}
 * component has a negative phase value, or the {@code SmartLifecycle} component
 * may start after those {@code Lifecycle} components if the {@code SmartLifecycle}
 * component has a positive phase value.
 *
 * <p>Note that, due to the auto-startup support in {@code SmartLifecycle}, a
 * {@code SmartLifecycle} bean instance will usually get initialized on startup
 * of the application context in any case. As a consequence, the bean definition
 * lazy-init flag has very limited actual effect on {@code SmartLifecycle} beans.
 *
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 3.0
 * @see LifecycleProcessor
 * @see ConfigurableApplicationContext
 */
// {@link Lifecycle} 接口的扩展，用于需要以特定顺序在 {@code ApplicationContext} 刷新和/或关闭时启动的那些对象。
//
// <p>{@link isAutoStartup()} 返回值指示该对象是否应在上下文刷新时启动。
// 接受回调的 {@link stop(Runnable)} 方法对于具有异步关闭过程的对象很有用。
// 此接口的任何实现<i>必须<i>在关闭完成时调用回调的 {@code run()} 方法，
// 以避免在整个 {@code ApplicationContext} 关闭中出现不必要的延迟。
//
// <p>这个接口扩展了{@link Phased}，{@link getPhase()} 方法的返回值指示了这个{@code Lifecycle} 组件
// 应该在哪个阶段启动和停止。启动过程从 <i>lowest<i> 相位值开始，以 <i>highest<i> 相位值结束（{@code Integer.MIN_VALUE}
// 是可能的最低值，而 {@code Integer.MAX_VALUE} 是尽可能高）。关闭过程将应用相反的顺序。任何具有相同值的组件将在同一阶段内任意排序。
//
// <p>例如：如果组件B依赖于组件A已经启动，那么组件A的相位值应该低于组件B。在关闭过程中，组件B会先于组件A停止。
//
// <p>任何显式的“依赖”关系都将优先于阶段顺序，这样依赖 bean 总是在其依赖之后开始，并始终在其依赖之前停止
//
// <p>上下文中未实现 {@code SmartLifecycle} 的任何 {@code Lifecycle} 组件将被视为具有 {@code 0} 的阶段值。
// 如果 {@code SmartLifecycle} 组件具有负相位值，则这允许 {@code SmartLifecycle} 组件在这些 {@code Lifecycle} 组件之前启动，
// 或者 {@code SmartLifecycle} 组件可能在这些 {@code Lifecycle} 之后启动如果 {@code SmartLifecycle} 组件具有正相位值。
//
// <p>请注意，由于 {@code SmartLifecycle} 中的自动启动支持，{@code SmartLifecycle} bean 实例通常会在
// 任何情况下在应用程序上下文启动时进行初始化。因此，bean 定义lazy-init 标志对{@code SmartLifecycle} bean 的实际影响非常有限。
public interface SmartLifecycle extends Lifecycle, Phased {

	/**
	 * The default phase for {@code SmartLifecycle}: {@code Integer.MAX_VALUE}.
	 * <p>This is different from the common phase {@code 0} associated with regular
	 * {@link Lifecycle} implementations, putting the typically auto-started
	 * {@code SmartLifecycle} beans into a later startup phase and an earlier
	 * shutdown phase.
	 * @since 5.1
	 * @see #getPhase()
	 * @see org.springframework.context.support.DefaultLifecycleProcessor#getPhase(Lifecycle)
	 */
	// {@code SmartLifecycle} 的默认阶段：{@code Integer.MAX_VALUE}。
	// <p>这与与常规 {@link Lifecycle} 实现相关联的公共阶段 {@code 0} 不同，
	// 将通常自动启动的 {@code SmartLifecycle} bean 放入稍后的启动阶段和更早的关闭阶段
	int DEFAULT_PHASE = Integer.MAX_VALUE;


	/**
	 * Returns {@code true} if this {@code Lifecycle} component should get
	 * started automatically by the container at the time that the containing
	 * {@link ApplicationContext} gets refreshed.
	 * <p>A value of {@code false} indicates that the component is intended to
	 * be started through an explicit {@link #start()} call instead, analogous
	 * to a plain {@link Lifecycle} implementation.
	 * <p>The default implementation returns {@code true}.
	 * @see #start()
	 * @see #getPhase()
	 * @see LifecycleProcessor#onRefresh()
	 * @see ConfigurableApplicationContext#refresh()
	 */
	// 如果此 {@code Lifecycle} 组件应在包含 {@link ApplicationContext} 刷新时由容器自动启动，则返回 {@code true}。
	// <p>{@code false} 值表示该组件旨在通过显式 {@link #start()} 调用启动，类似于普通的 {@link Lifecycle} 实现。
	// <p>默认实现返回 {@code true}。
	default boolean isAutoStartup() {
		return true;
	}

	/**
	 * Indicates that a Lifecycle component must stop if it is currently running.
	 * <p>The provided callback is used by the {@link LifecycleProcessor} to support
	 * an ordered, and potentially concurrent, shutdown of all components having a
	 * common shutdown order value. The callback <b>must</b> be executed after
	 * the {@code SmartLifecycle} component does indeed stop.
	 * <p>The {@link LifecycleProcessor} will call <i>only</i> this variant of the
	 * {@code stop} method; i.e. {@link Lifecycle#stop()} will not be called for
	 * {@code SmartLifecycle} implementations unless explicitly delegated to within
	 * the implementation of this method.
	 * <p>The default implementation delegates to {@link #stop()} and immediately
	 * triggers the given callback in the calling thread. Note that there is no
	 * synchronization between the two, so custom implementations may at least
	 * want to put the same steps within their common lifecycle monitor (if any).
	 * @see #stop()
	 * @see #getPhase()
	 */
	// 表示如果生命周期组件当前正在运行，它必须停止。
	// <p>{@link LifecycleProcessor} 使用提供的回调来支持所有具有公共关闭顺序值的组件的有序且可能并发的关闭。
	// 回调<b>必须<b>在{@code SmartLifecycle}组件确实停止后执行。
	// <p>{@link LifecycleProcessor} 将<i>仅<i>调用 {@code stop} 方法的这个变体；即 {@link Lifecycle#stop()}
	// 不会被 {@code SmartLifecycle} 实现调用，除非在此方法的实现中明确委托。
	// <p>默认实现委托给 {@link #stop()} 并立即触发调用线程中的给定回调。
	// 请注意，两者之间没有同步，因此自定义实现可能至少希望将相同的步骤放在其公共生命周期监视器（如果有）中。
	default void stop(Runnable callback) {
		stop();
		callback.run();
	}

	/**
	 * Return the phase that this lifecycle object is supposed to run in.
	 * <p>The default implementation returns {@link #DEFAULT_PHASE} in order to
	 * let {@code stop()} callbacks execute after regular {@code Lifecycle}
	 * implementations.
	 * @see #isAutoStartup()
	 * @see #start()
	 * @see #stop(Runnable)
	 * @see org.springframework.context.support.DefaultLifecycleProcessor#getPhase(Lifecycle)
	 */
	// 返回此生命周期对象应该运行的阶段。 <p>默认实现返回 {@link #DEFAULT_PHASE} 以便让 {@code stop()} 回调
	// 在常规 {@code Lifecycle} 实现之后执行
	@Override
	default int getPhase() {
		return DEFAULT_PHASE;
	}

}
