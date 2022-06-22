/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.aop.target;

import java.util.HashSet;
import java.util.Set;

import org.springframework.aop.IntroductionAdvisor;
import org.springframework.aop.support.DefaultIntroductionAdvisor;
import org.springframework.aop.support.DelegatingIntroductionInterceptor;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.core.NamedThreadLocal;

/**
 * Alternative to an object pool. This {@link org.springframework.aop.TargetSource}
 * uses a threading model in which every thread has its own copy of the target.
 * There's no contention for targets. Target object creation is kept to a minimum
 * on the running server.
 *
 * <p>Application code is written as to a normal pool; callers can't assume they
 * will be dealing with the same instance in invocations in different threads.
 * However, state can be relied on during the operations of a single thread:
 * for example, if one caller makes repeated calls on the AOP proxy.
 *
 * <p>Cleanup of thread-bound objects is performed on BeanFactory destruction,
 * calling their {@code DisposableBean.destroy()} method if available.
 * Be aware that many thread-bound objects can be around until the application
 * actually shuts down.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @see ThreadLocalTargetSourceStats
 * @see org.springframework.beans.factory.DisposableBean#destroy()
 */
// 对象池的替代方案。 这个org.springframework.aop.TargetSource 使用线程模型，其中每个线程都有自己的目标副本。
// 目标不存在竞争。 目标对象的创建在运行的服务器上保持在最低限度。
//
// 应用程序代码编写为普通池； 调用者不能假设他们将在不同线程的调用中处理相同的实例。 但是，在单个线程的操作过程中可以
// 依赖状态：例如，如果一个调用者对 AOP 代理进行重复调用。
//
// 线程绑定对象的清理在 BeanFactory 销毁时执行，如果可用，调用它们的 DisposableBean.destroy() 方法。 请注意，
// 在应用程序实际关闭之前，可能存在许多线程绑定对象。
//
// 基于线程级别的缓存 ThreadLocal
@SuppressWarnings("serial")
public class ThreadLocalTargetSource extends AbstractPrototypeBasedTargetSource
		implements ThreadLocalTargetSourceStats, DisposableBean {

	/**
	 * ThreadLocal holding the target associated with the current
	 * thread. Unlike most ThreadLocals, which are static, this variable
	 * is meant to be per thread per instance of the ThreadLocalTargetSource class.
	 */
	// ThreadLocal 持有与当前线程关联的目标。 与大多数静态的 ThreadLocals 不同，此变量意味着
	// 每个线程每个 ThreadLocalTargetSource 类的实例
	private final ThreadLocal<Object> targetInThread =
			new NamedThreadLocal<>("Thread-local instance of bean '" + getTargetBeanName() + "'");

	/**
	 * Set of managed targets, enabling us to keep track of the targets we've created.
	 */
	// 一组托管目标，使我们能够跟踪我们创建的目标
	private final Set<Object> targetSet = new HashSet<>();

	// 调用次数
	private int invocationCount;

	// 命中计数
	private int hitCount;


	/**
	 * Implementation of abstract getTarget() method.
	 * We look for a target held in a ThreadLocal. If we don't find one,
	 * we create one and bind it to the thread. No synchronization is required.
	 */
	// 抽象 getTarget() 方法的实现。 我们寻找保存在 ThreadLocal 中的目标。
	// 如果找不到，我们创建一个并将其绑定到线程。 不需要同步
	@Override
	public Object getTarget() throws BeansException {
		++this.invocationCount;
		Object target = this.targetInThread.get();
		if (target == null) {
			if (logger.isDebugEnabled()) {
				logger.debug("No target for prototype '" + getTargetBeanName() + "' bound to thread: " +
						"creating one and binding it to thread '" + Thread.currentThread().getName() + "'");
			}
			// Associate target with ThreadLocal.
			// 将目标与 ThreadLocal 关联，通过依赖查找来创建原型实例。保证每个线程都关联一个独立对象
			target = newPrototypeInstance();
			this.targetInThread.set(target);
			synchronized (this.targetSet) {
				this.targetSet.add(target); // targetSet 使用final 修饰保证集合只能添加一次
			}
		}
		else {
			++this.hitCount;
		}
		return target;
	}

	/**
	 * Dispose of targets if necessary; clear ThreadLocal.
	 * @see #destroyPrototypeInstance
	 */
	@Override
	public void destroy() {
		logger.debug("Destroying ThreadLocalTargetSource bindings");
		synchronized (this.targetSet) {
			for (Object target : this.targetSet) {
				destroyPrototypeInstance(target);
			}
			this.targetSet.clear();
		}
		// Clear ThreadLocal, just in case.
		this.targetInThread.remove();
	}


	@Override
	public int getInvocationCount() {
		return this.invocationCount;
	}

	@Override
	public int getHitCount() {
		return this.hitCount;
	}

	@Override
	public int getObjectCount() {
		synchronized (this.targetSet) {
			return this.targetSet.size();
		}
	}


	/**
	 * Return an introduction advisor mixin that allows the AOP proxy to be
	 * cast to ThreadLocalInvokerStats.
	 */
	public IntroductionAdvisor getStatsMixin() {
		DelegatingIntroductionInterceptor dii = new DelegatingIntroductionInterceptor(this);
		return new DefaultIntroductionAdvisor(dii, ThreadLocalTargetSourceStats.class);
	}

}
