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

package org.springframework.util;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Support class for throttling concurrent access to a specific resource.
 *
 * <p>Designed for use as a base class, with the subclass invoking
 * the {@link #beforeAccess()} and {@link #afterAccess()} methods at
 * appropriate points of its workflow. Note that {@code afterAccess}
 * should usually be called in a finally block!
 *
 * <p>The default concurrency limit of this support class is -1
 * ("unbounded concurrency"). Subclasses may override this default;
 * check the javadoc of the concrete class that you're using.
 *
 * @author Juergen Hoeller
 * @since 1.2.5
 * @see #setConcurrencyLimit
 * @see #beforeAccess()
 * @see #afterAccess()
 * @see org.springframework.aop.interceptor.ConcurrencyThrottleInterceptor
 * @see java.io.Serializable
 */
// 用于限制对特定资源的并发访问的支持类。
//
// 设计用作基类，子类在其工作流的适当点调用 beforeAccess() 和 afterAccess() 方法。请
// 注意，通常应该在 finally 块中调用 afterAccess ！
//
// 此支持类的默认并发限制为 -1（“无限并发”）。子类可以覆盖这个默认值；检查您正在使用的具体类的 javadoc
@SuppressWarnings("serial")
public abstract class ConcurrencyThrottleSupport implements Serializable {

	/**
	 * Permit any number of concurrent invocations: that is, don't throttle concurrency.
	 */
	// 允许任意数量的并发调用：也就是说，不要限制并发。
	public static final int UNBOUNDED_CONCURRENCY = -1;

	/**
	 * Switch concurrency 'off': that is, don't allow any concurrent invocations.
	 */
	// 关闭并发：即不允许任何并发调用。
	public static final int NO_CONCURRENCY = 0;


	/** Transient to optimize serialization. */
	// 瞬态优化序列化。
	protected transient Log logger = LogFactory.getLog(getClass());

	// 对象锁
	private transient Object monitor = new Object();

	private int concurrencyLimit = UNBOUNDED_CONCURRENCY;

	private int concurrencyCount = 0;


	/**
	 * Set the maximum number of concurrent access attempts allowed.
	 * -1 indicates unbounded concurrency.
	 * <p>In principle, this limit can be changed at runtime,
	 * although it is generally designed as a config time setting.
	 * <p>NOTE: Do not switch between -1 and any concrete limit at runtime,
	 * as this will lead to inconsistent concurrency counts: A limit
	 * of -1 effectively turns off concurrency counting completely.
	 */
	// 设置允许的最大并发访问尝试次数。 -1 表示无限并发。
	//
	// 原则上，此限制可以在运行时更改，尽管它通常设计为配置时间设置。
	//
	// 注意：不要在运行时在 -1 和任何具体限制之间切换，因为这会导致并发计数不一致：-1 限制有效地完全关闭并发计数。
	public void setConcurrencyLimit(int concurrencyLimit) {
		this.concurrencyLimit = concurrencyLimit;
	}

	/**
	 * Return the maximum number of concurrent access attempts allowed.
	 */
	// 返回允许的最大并发访问尝试次数。
	public int getConcurrencyLimit() {
		return this.concurrencyLimit;
	}

	/**
	 * Return whether this throttle is currently active.
	 * @return {@code true} if the concurrency limit for this instance is active
	 * @see #getConcurrencyLimit()
	 */
	// 返回此油门当前是否处于活动状态。
	// 返回值：
	//			如果此实例的并发限制处于活动状态，则为true
	// 请参阅：
	//			getConcurrencyLimit()
	public boolean isThrottleActive() {
		return (this.concurrencyLimit >= 0);
	}


	/**
	 * To be invoked before the main execution logic of concrete subclasses.
	 * <p>This implementation applies the concurrency throttle.
	 * @see #afterAccess()
	 */
	// 在具体子类的主要执行逻辑之前被调用。
	// 此实现应用并发限制。
	protected void beforeAccess() {
		// 无限并发
		if (this.concurrencyLimit == NO_CONCURRENCY) {
			throw new IllegalStateException(
					"Currently no invocations allowed - concurrency limit set to NO_CONCURRENCY");
		}
		if (this.concurrencyLimit > 0) {
			boolean debug = logger.isDebugEnabled();
			synchronized (this.monitor) {
				boolean interrupted = false;
				// 如果超出最大并发访问尝试次数
				while (this.concurrencyCount >= this.concurrencyLimit) {
					// 设置中断抛出异常
					if (interrupted) {
						throw new IllegalStateException("Thread was interrupted while waiting for invocation access, " +
								"but concurrency limit still does not allow for entering");
					}
					if (debug) {
						logger.debug("Concurrency count " + this.concurrencyCount +
								" has reached limit " + this.concurrencyLimit + " - blocking");
					}
					try {
						// 让锁等待
						this.monitor.wait();
					}
					catch (InterruptedException ex) {
						// Re-interrupt current thread, to allow other threads to react.
						// 重新中断当前线程，让其他线程做出反应
						Thread.currentThread().interrupt();
						interrupted = true;
					}
				}
				if (debug) {
					logger.debug("Entering throttle at concurrency count " + this.concurrencyCount);
				}
				// 最大并发访问尝试次数 + 1
				this.concurrencyCount++;
			}
		}
	}

	/**
	 * To be invoked after the main execution logic of concrete subclasses.
	 * @see #beforeAccess()
	 */
	// 在具体子类的主要执行逻辑之后调用。
	protected void afterAccess() {
		if (this.concurrencyLimit >= 0) {
			// 加锁
			synchronized (this.monitor) {
				// 最大并发数 - 1
				this.concurrencyCount--;
				if (logger.isDebugEnabled()) {
					logger.debug("Returning from throttle at concurrency count " + this.concurrencyCount);
				}
				// 当前锁通知其他线程重新抢占
				this.monitor.notify();
			}
		}
	}


	//---------------------------------------------------------------------
	// Serialization support
	// 序列化支持
	//---------------------------------------------------------------------

	private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
		// Rely on default serialization, just initialize state after deserialization.
		// 依赖默认序列化，反序列化后初始化状态即可。
		ois.defaultReadObject();

		// Initialize transient fields.
		// 初始化瞬态字段
		this.logger = LogFactory.getLog(getClass());
		this.monitor = new Object();
	}

}
