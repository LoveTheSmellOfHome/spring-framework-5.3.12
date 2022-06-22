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

package org.springframework.util;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.lang.Nullable;

/**
 * Simple customizable helper class for creating new {@link Thread} instances.
 * Provides various bean properties: thread name prefix, thread priority, etc.
 *
 * <p>Serves as base class for thread factories such as
 * {@link org.springframework.scheduling.concurrent.CustomizableThreadFactory}.
 *
 * @author Juergen Hoeller
 * @since 2.0.3
 * @see org.springframework.scheduling.concurrent.CustomizableThreadFactory
 */
// 自定义线程创建器：用于创建新 Thread 实例的简单可定制帮助程序类。提供各种bean属性：线程名前缀、线程优先级等。
// 作为org.springframework.scheduling.concurrent.CustomizableThreadFactory等线程工厂的基类。
@SuppressWarnings("serial")
public class CustomizableThreadCreator implements Serializable {

	private String threadNamePrefix;

	private int threadPriority = Thread.NORM_PRIORITY;

	private boolean daemon = false;

	@Nullable
	private ThreadGroup threadGroup;

	private final AtomicInteger threadCount = new AtomicInteger();


	/**
	 * Create a new CustomizableThreadCreator with default thread name prefix.
	 */
	// 使用默认线程名称前缀创建一个新的 CustomizableThreadCreator
	public CustomizableThreadCreator() {
		this.threadNamePrefix = getDefaultThreadNamePrefix();
	}

	/**
	 * Create a new CustomizableThreadCreator with the given thread name prefix.
	 * @param threadNamePrefix the prefix to use for the names of newly created threads
	 */
	// 使用给定的线程名称前缀创建一个新的 CustomizableThreadCreator。
	// 参形：
	//			threadNamePrefix – 用于新创建线程名称的前缀
	public CustomizableThreadCreator(@Nullable String threadNamePrefix) {
		this.threadNamePrefix = (threadNamePrefix != null ? threadNamePrefix : getDefaultThreadNamePrefix());
	}


	/**
	 * Specify the prefix to use for the names of newly created threads.
	 * Default is "SimpleAsyncTaskExecutor-".
	 */
	// 指定用于新创建线程名称的前缀。默认为 “SimpleAsyncTaskExecutor-”。
	public void setThreadNamePrefix(@Nullable String threadNamePrefix) {
		this.threadNamePrefix = (threadNamePrefix != null ? threadNamePrefix : getDefaultThreadNamePrefix());
	}

	/**
	 * Return the thread name prefix to use for the names of newly
	 * created threads.
	 */
	// 返回线程名称前缀以用于新创建线程的名称
	public String getThreadNamePrefix() {
		return this.threadNamePrefix;
	}

	/**
	 * Set the priority of the threads that this factory creates.
	 * Default is 5.
	 * @see java.lang.Thread#NORM_PRIORITY
	 */
	// 设置此工厂创建的线程的优先级。默认值为 5
	public void setThreadPriority(int threadPriority) {
		this.threadPriority = threadPriority;
	}

	/**
	 * Return the priority of the threads that this factory creates.
	 */
	// 返回此工厂创建的线程的优先级
	public int getThreadPriority() {
		return this.threadPriority;
	}

	/**
	 * Set whether this factory is supposed to create daemon threads,
	 * just executing as long as the application itself is running.
	 * <p>Default is "false": Concrete factories usually support explicit cancelling.
	 * Hence, if the application shuts down, Runnables will by default finish their
	 * execution.
	 * <p>Specify "true" for eager shutdown of threads which still actively execute
	 * a {@link Runnable} at the time that the application itself shuts down.
	 * @see java.lang.Thread#setDaemon
	 */
	// 设置这个工厂是否应该创建守护线程，只要应用程序本身正在运行就执行。
	// 默认为 “false”：具体工厂通常支持显式取消。因此，如果应用程序关闭，Runnables 将默认完成其执行。
	// 指定 “true” 以急切关闭在应用程序本身关闭时仍积极执行 Runnable 的线程。
	public void setDaemon(boolean daemon) {
		this.daemon = daemon;
	}

	/**
	 * Return whether this factory should create daemon threads.
	 */
	// 返回此工厂是否应该创建守护线程
	public boolean isDaemon() {
		return this.daemon;
	}

	/**
	 * Specify the name of the thread group that threads should be created in.
	 * @see #setThreadGroup
	 */
	// 指定应在其中创建线程的线程组的名称。
	public void setThreadGroupName(String name) {
		this.threadGroup = new ThreadGroup(name);
	}

	/**
	 * Specify the thread group that threads should be created in.
	 * @see #setThreadGroupName
	 */
	// 指定应在其中创建线程的线程组
	public void setThreadGroup(@Nullable ThreadGroup threadGroup) {
		this.threadGroup = threadGroup;
	}

	/**
	 * Return the thread group that threads should be created in
	 * (or {@code null} for the default group).
	 */
	// 返回应在其中创建线程的线程组（或默认组为null ）
	@Nullable
	public ThreadGroup getThreadGroup() {
		return this.threadGroup;
	}


	/**
	 * Template method for the creation of a new {@link Thread}.
	 * <p>The default implementation creates a new Thread for the given
	 * {@link Runnable}, applying an appropriate thread name.
	 * @param runnable the Runnable to execute
	 * @see #nextThreadName()
	 */
	// 用于创建新Thread的模板方法。
	// 默认实现为给定的Runnable创建一个新的线程，应用适当的线程名称。
	// 参形：
	//			runnable – 要执行的 Runnable
	public Thread createThread(Runnable runnable) {
		Thread thread = new Thread(getThreadGroup(), runnable, nextThreadName());
		thread.setPriority(getThreadPriority());
		thread.setDaemon(isDaemon());
		return thread;
	}

	/**
	 * Return the thread name to use for a newly created {@link Thread}.
	 * <p>The default implementation returns the specified thread name prefix
	 * with an increasing thread count appended: e.g. "SimpleAsyncTaskExecutor-0".
	 * @see #getThreadNamePrefix()
	 */
	// 返回要用于新创建的 Thread 的线程名称。
	// 默认实现返回指定的线程名称前缀，并附加增加的线程数：例如“SimpleAsyncTaskExecutor-0”。
	protected String nextThreadName() {
		return getThreadNamePrefix() + this.threadCount.incrementAndGet();
	}

	/**
	 * Build the default thread name prefix for this factory.
	 * @return the default thread name prefix (never {@code null})
	 */
	// 为此工厂构建默认线程名称前缀。
	// 返回值：
	//			默认线程名称前缀（从不为null
	protected String getDefaultThreadNamePrefix() {
		return ClassUtils.getShortName(getClass()) + "-";
	}

}
