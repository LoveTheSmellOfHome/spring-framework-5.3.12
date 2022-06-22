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

package org.springframework.aop;

import org.springframework.lang.Nullable;

/**
 * A {@code TargetSource} is used to obtain the current "target" of
 * an AOP invocation, which will be invoked via reflection if no around
 * advice chooses to end the interceptor chain itself.
 *
 * <p>If a {@code TargetSource} is "static", it will always return
 * the same target, allowing optimizations in the AOP framework. Dynamic
 * target sources can support pooling, hot swapping, etc.
 *
 * <p>Application developers don't usually need to work with
 * {@code TargetSources} directly: this is an AOP framework interface.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
// TargetSource 用于获取 AOP 调用的当前“目标”，如果没有环绕通知选择结束拦截器链本身，则将通过反射调用该目标。
//
// 如果 TargetSource 是“静态的”，它将始终返回相同的目标，从而允许在 AOP 框架中进行优化。 动态目标源可以支持池化、热交换等。
//
// 应用程序开发人员通常不需要直接使用 TargetSources ：这是一个 AOP 框架接口。
//
// 去跟踪目标源(资源池)，TargetSource 是个标准化接口，通常用于池化技术，即一个 TargetSource 就是一个池。
public interface TargetSource extends TargetClassAware {

	/**
	 * Return the type of targets returned by this {@link TargetSource}.
	 * <p>Can return {@code null}, although certain usages of a {@code TargetSource}
	 * might just work with a predetermined target class.
	 * @return the type of targets returned by this {@link TargetSource}
	 */
	// 返回此 TargetSource 返回的目标类型。
	// 可以返回 null ，尽管 TargetSource 的某些用法可能只适用于预定的目标类。
	// 返回值：
	//			此TargetSource返回的目标类型
	@Override
	@Nullable
	Class<?> getTargetClass();

	/**
	 * Will all calls to {@link #getTarget()} return the same object?
	 * <p>In that case, there will be no need to invoke {@link #releaseTarget(Object)},
	 * and the AOP framework can cache the return value of {@link #getTarget()}.
	 * @return {@code true} if the target is immutable
	 * @see #getTarget
	 */
	// 所有对 getTarget() 的调用都会返回相同的对象吗？如果返回对象是相同对象
	// 不需要调用 releaseTarget(Object) ，AOP 框架可以缓存 getTarget() 的返回值。
	// 返回值：
	//			如果目标是不可变的，则为 true
	boolean isStatic();

	/**
	 * Return a target instance. Invoked immediately before the
	 * AOP framework calls the "target" of an AOP method invocation.
	 * @return the target object which contains the joinpoint,
	 * or {@code null} if there is no actual target instance
	 * @throws Exception if the target object can't be resolved
	 */
	// 返回一个目标实例。 在 AOP 框架调用 AOP 方法调用的“目标”之前立即调用。
	// 返回值：
	//			包含连接点的目标对象，如果没有实际的目标实例，则返回null，如果是池则返回池中实例
	// 抛出：
	//			Exception – 如果目标对象无法解析
	@Nullable
	Object getTarget() throws Exception;

	/**
	 * Release the given target object obtained from the
	 * {@link #getTarget()} method, if any.
	 * @param target object obtained from a call to {@link #getTarget()}
	 * @throws Exception if the object can't be released
	 */
	// 如果返回对象是不同对象，需要释放从 getTarget()方法获得的给定目标对象（如果有,通常用于池化技术）
	// 比如在 Spring 场景中我们去实现 DataSource,数据库管理员的替换操作，那么这时候返回不同对象来实现。
	// 同时调用本方法释放目标对象。以便于回收
	// 参形：
	//			target – 通过调用getTarget()获得的对象
	// 抛出：
	//			Exception ——如果对象不能被释放
	void releaseTarget(Object target) throws Exception;

}
