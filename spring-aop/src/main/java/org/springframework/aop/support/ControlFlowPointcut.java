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

package org.springframework.aop.support;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.aop.ClassFilter;
import org.springframework.aop.MethodMatcher;
import org.springframework.aop.Pointcut;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * Pointcut and method matcher for use in simple <b>cflow</b>-style pointcut.
 * Note that evaluating such pointcuts is 10-15 times slower than evaluating
 * normal pointcuts, but they are useful in some cases.
 *
 * @author Rod Johnson
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Sam Brannen
 */
// 用于简单 cflow-style 风格切入点的切入点和方法匹配器。请注意，评估此类切入点比评估普通切入点
// 慢 10-15 倍，但它们在某些情况下很有用。
//
// Joinpoint 的便利实现，方便开发人员使用的，控制流匹配器
@SuppressWarnings("serial")
public class ControlFlowPointcut implements Pointcut, ClassFilter, MethodMatcher, Serializable {

	private final Class<?> clazz;

	@Nullable
	private final String methodName;

	private final AtomicInteger evaluations = new AtomicInteger();


	/**
	 * Construct a new pointcut that matches all control flows below that class.
	 * @param clazz the clazz
	 */
	public ControlFlowPointcut(Class<?> clazz) {
		this(clazz, null);
	}

	/**
	 * Construct a new pointcut that matches all calls below the given method
	 * in the given class. If no method name is given, matches all control flows
	 * below the given class.
	 * @param clazz the clazz
	 * @param methodName the name of the method (may be {@code null})
	 */
	public ControlFlowPointcut(Class<?> clazz, @Nullable String methodName) {
		Assert.notNull(clazz, "Class must not be null");
		this.clazz = clazz;
		this.methodName = methodName;
	}


	/**
	 * Subclasses can override this for greater filtering (and performance).
	 */
	@Override
	public boolean matches(Class<?> clazz) {
		return true;
	}

	/**
	 * Subclasses can override this if it's possible to filter out some candidate classes.
	 */
	@Override
	public boolean matches(Method method, Class<?> targetClass) {
		return true;
	}

	@Override
	public boolean isRuntime() {
		return true;
	}

	@Override
	public boolean matches(Method method, Class<?> targetClass, Object... args) {
		this.evaluations.incrementAndGet();

		// 去匹配 StackTraceElement，Throwable 会调用 fillInStackTrace()，
		// 这个操作本身是同步的，此外当跟踪的链路比较深的时候，此时通过名称和方法去判断，性能上会有损耗。
		// 此处可以使用 JDK 9 的 StackWalker 来实现。性能会好很多
		for (StackTraceElement element : new Throwable().getStackTrace()) {
			if (element.getClassName().equals(this.clazz.getName()) &&
					(this.methodName == null || element.getMethodName().equals(this.methodName))) {
				return true;
			}
		}
		return false;
	}

	/**
	 * It's useful to know how many times we've fired, for optimization.
	 */
	public int getEvaluations() {
		return this.evaluations.get();
	}


	@Override
	public ClassFilter getClassFilter() {
		return this;
	}

	@Override
	public MethodMatcher getMethodMatcher() {
		return this;
	}


	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof ControlFlowPointcut)) {
			return false;
		}
		ControlFlowPointcut that = (ControlFlowPointcut) other;
		return (this.clazz.equals(that.clazz)) && ObjectUtils.nullSafeEquals(this.methodName, that.methodName);
	}

	@Override
	public int hashCode() {
		int code = this.clazz.hashCode();
		if (this.methodName != null) {
			code = 37 * code + this.methodName.hashCode();
		}
		return code;
	}

	@Override
	public String toString() {
		return getClass().getName() + ": class = " + this.clazz.getName() + "; methodName = " + this.methodName;
	}

}
