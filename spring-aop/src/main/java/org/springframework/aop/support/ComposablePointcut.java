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

package org.springframework.aop.support;

import java.io.Serializable;

import org.springframework.aop.ClassFilter;
import org.springframework.aop.MethodMatcher;
import org.springframework.aop.Pointcut;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Convenient class for building up pointcuts.
 *
 * <p>All methods return {@code ComposablePointcut}, so we can use concise idioms
 * like in the following example.
 *
 * <pre class="code">Pointcut pc = new ComposablePointcut()
 *                      .union(classFilter)
 *                      .intersection(methodMatcher)
 *                      .intersection(pointcut);</pre>
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @since 11.11.2003
 * @see Pointcuts
 */
// 用于建立切入点的便捷类。
// 所有方法都返回ComposablePointcut ，因此我们可以使用如下示例中的简洁习语。
// Pointcut pc = new ComposablePointcut()
//                        .union(classFilter)
//                        .intersection(methodMatcher)
//                        .intersection(pointcut);
//
// Pointcut：通过逻辑判断，判断当前的类或者方法，是不是符合条件，是 AOP 的判断模式体现。
// 这个接口就是 Pointcut 的组合实现，它利用到一些工具类（ClassFilter 工具类 - ClassFilters,MethodMatcher 工具类 - MethodMatchers
// 以及 Pointcut 工具类 - Pointcuts）来实现的。以 s 结尾的工具类是 JDK 常用的形式，在 Spring 中还存在另一种以 utils 结尾的工具类。
public class ComposablePointcut implements Pointcut, Serializable {

	/** use serialVersionUID from Spring 1.2 for interoperability. */
	private static final long serialVersionUID = -2743223737633663832L;

	private ClassFilter classFilter;

	private MethodMatcher methodMatcher;


	/**
	 * Create a default ComposablePointcut, with {@code ClassFilter.TRUE}
	 * and {@code MethodMatcher.TRUE}.
	 */
	// 使用ClassFilter.TRUE和MethodMatcher.TRUE创建一个默认的 ComposablePointcut。
	public ComposablePointcut() {
		// 默认情况下过滤器是 TRUE,意味着所有类，所有方法都可以被拦截。
		this.classFilter = ClassFilter.TRUE;
		this.methodMatcher = MethodMatcher.TRUE;
	}

	/**
	 * Create a ComposablePointcut based on the given Pointcut.
	 * @param pointcut the original Pointcut
	 */
	// 根据给定的切入点创建一个 ComposablePointcut。
	// 参形：切入点- 原始切入点
	public ComposablePointcut(Pointcut pointcut) {
		Assert.notNull(pointcut, "Pointcut must not be null");
		this.classFilter = pointcut.getClassFilter();
		this.methodMatcher = pointcut.getMethodMatcher();
	}

	/**
	 * Create a ComposablePointcut for the given ClassFilter,
	 * with {@code MethodMatcher.TRUE}.
	 * @param classFilter the ClassFilter to use
	 */
	// 使用 MethodMatcher.TRUE 为给定的MethodMatcher.TRUE创建一个 ComposablePointcut。
	// 参形：classFilter – 要使用的 ClassFilter
	public ComposablePointcut(ClassFilter classFilter) {
		Assert.notNull(classFilter, "ClassFilter must not be null");
		this.classFilter = classFilter;
		this.methodMatcher = MethodMatcher.TRUE;
	}

	/**
	 * Create a ComposablePointcut for the given MethodMatcher,
	 * with {@code ClassFilter.TRUE}.
	 * @param methodMatcher the MethodMatcher to use
	 */
	// 使用 ClassFilter.TRUE 为给定的ClassFilter.TRUE创建一个 ComposablePointcut。
	// 参形：methodMatcher – 要使用的 MethodMatcher
	public ComposablePointcut(MethodMatcher methodMatcher) {
		Assert.notNull(methodMatcher, "MethodMatcher must not be null");
		this.classFilter = ClassFilter.TRUE;
		this.methodMatcher = methodMatcher;
	}

	/**
	 * Create a ComposablePointcut for the given ClassFilter and MethodMatcher.
	 * @param classFilter the ClassFilter to use
	 * @param methodMatcher the MethodMatcher to use
	 */
	// 为给定的 ClassFilter 和 MethodMatcher 创建一个 ComposablePointcut。
	// 参形：
	//  		classFilter – 要使用的 ClassFilter
	//			methodMatcher – 要使用的 MethodMatcher
	public ComposablePointcut(ClassFilter classFilter, MethodMatcher methodMatcher) {
		Assert.notNull(classFilter, "ClassFilter must not be null");
		Assert.notNull(methodMatcher, "MethodMatcher must not be null");
		this.classFilter = classFilter;
		this.methodMatcher = methodMatcher;
	}


	/**
	 * Apply a union with the given ClassFilter.
	 * @param other the ClassFilter to apply a union with
	 * @return this composable pointcut (for call chaining)
	 */
	// 应用与给定 ClassFilter 的并集。集合 A || B, 获得最大的匹配程度
	// 参形：
	//			other – 应用联合的 ClassFilter
	// 返回值：
	//			这个可组合的切入点（用于调用链）
	public ComposablePointcut union(ClassFilter other) {
		this.classFilter = ClassFilters.union(this.classFilter, other);
		return this;
	}

	/**
	 * Apply an intersection with the given ClassFilter.
	 * @param other the ClassFilter to apply an intersection with
	 * @return this composable pointcut (for call chaining)
	 */
	// 应用与给定 ClassFilter 的交集。集合 A && B,获得最小的匹配程度,产生中断效应。中间某个环节不成立直接 ruturn
	// 参形：
	//				other – 应用交集的 ClassFilter
	// 返回值：
	//				这个可组合的切入点（用于调用链）
	public ComposablePointcut intersection(ClassFilter other) {
		this.classFilter = ClassFilters.intersection(this.classFilter, other);
		return this;
	}

	/**
	 * Apply a union with the given MethodMatcher.
	 * @param other the MethodMatcher to apply a union with
	 * @return this composable pointcut (for call chaining)
	 */
	// 使用给定的 MethodMatcher 应用联合。
	// 参形：
	//			other – 应用联合的 MethodMatcher
	// 返回值：
	//			这个可组合的切入点（用于调用链）
	public ComposablePointcut union(MethodMatcher other) {
		this.methodMatcher = MethodMatchers.union(this.methodMatcher, other);
		return this;
	}

	/**
	 * Apply an intersection with the given MethodMatcher.
	 * @param other the MethodMatcher to apply an intersection with
	 * @return this composable pointcut (for call chaining)
	 */
	// 应用与给定 MethodMatcher 的交集。
	// 参形：
	//			other – 应用交集的 MethodMatcher
	// 返回值：
	//			这个可组合的切入点（用于调用链）
	public ComposablePointcut intersection(MethodMatcher other) {
		this.methodMatcher = MethodMatchers.intersection(this.methodMatcher, other);
		return this;
	}

	/**
	 * Apply a union with the given Pointcut.
	 * <p>Note that for a Pointcut union, methods will only match if their
	 * original ClassFilter (from the originating Pointcut) matches as well.
	 * MethodMatchers and ClassFilters from different Pointcuts will never
	 * get interleaved with each other.
	 * @param other the Pointcut to apply a union with
	 * @return this composable pointcut (for call chaining)
	 */
	// 应用具有给定切入点的联合。
	// 请注意，对于切入点联合，方法只有在其原始 ClassFilter（来自原始切入点）也匹配时才会匹配。
	// 来自不同切入点的 MethodMatchers 和 ClassFilters 永远不会相互交错。
	// 参形：
	//			other – 应用联合的切入点
	// 返回值：
	//			这个可组合的切入点（用于调用链）
	public ComposablePointcut union(Pointcut other) {
		this.methodMatcher = MethodMatchers.union(
				this.methodMatcher, this.classFilter, other.getMethodMatcher(), other.getClassFilter());
		this.classFilter = ClassFilters.union(this.classFilter, other.getClassFilter());
		return this;
	}

	/**
	 * Apply an intersection with the given Pointcut.
	 * @param other the Pointcut to apply an intersection with
	 * @return this composable pointcut (for call chaining)
	 */
	// 应用与给定切入点的交集。
	// 参形：other – 应用交集的切入点
	// 返回值：这个可组合的切入点（用于调用链）
	public ComposablePointcut intersection(Pointcut other) {
		this.classFilter = ClassFilters.intersection(this.classFilter, other.getClassFilter());
		this.methodMatcher = MethodMatchers.intersection(this.methodMatcher, other.getMethodMatcher());
		return this;
	}


	@Override
	public ClassFilter getClassFilter() {
		return this.classFilter;
	}

	@Override
	public MethodMatcher getMethodMatcher() {
		return this.methodMatcher;
	}

	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof ComposablePointcut)) {
			return false;
		}
		ComposablePointcut otherPointcut = (ComposablePointcut) other;
		return (this.classFilter.equals(otherPointcut.classFilter) &&
				this.methodMatcher.equals(otherPointcut.methodMatcher));
	}

	@Override
	public int hashCode() {
		return this.classFilter.hashCode() * 37 + this.methodMatcher.hashCode();
	}

	@Override
	public String toString() {
		return getClass().getName() + ": " + this.classFilter + ", " + this.methodMatcher;
	}

}
