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

package org.springframework.core;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import org.springframework.lang.Nullable;

/**
 * Interface to discover parameter names for methods and constructors.
 *
 * <p>Parameter name discovery is not always possible, but various strategies are
 * available to try, such as looking for debug information that may have been
 * emitted at compile time, and looking for argname annotation values optionally
 * accompanying AspectJ annotated methods.
 *
 * @author Rod Johnson
 * @author Adrian Colyer
 * @since 2.0
 */
// 参数名称发现器：用于发现(通过字节码的方式)方法和构造函数的参数名称的接口,
// 参数名称发现并非总是可行，但可以尝试各种策略，例如查找可能在编译时发出的调试信息，以及查找可选伴随 AspectJ 注释方法的 argname 注释值。
public interface ParameterNameDiscoverer {

	/**
	 * Return parameter names for a method, or {@code null} if they cannot be determined.
	 * <p>Individual entries in the array may be {@code null} if parameter names are only
	 * available for some parameters of the given method but not for others. However,
	 * it is recommended to use stub parameter names instead wherever feasible.
	 * @param method the method to find parameter names for
	 * @return an array of parameter names if the names can be resolved,
	 * or {@code null} if they cannot
	 */
	// 返回方法的参数名称，如果无法确定，则返回 {@code null}。
	// <p>如果参数名称仅可用于给定方法的某些参数而不可用于其他参数，
	// 则数组中的单个条目可能为 {@code null}。
	// 但是，建议在可行的情况下改用存根参数名称。
	// @param method 查找参数名称的方法
	// @return 参数名称数组，如果名称可以解析，或者 {@code null} 如果不能解析
	@Nullable
	String[] getParameterNames(Method method);

	/**
	 * Return parameter names for a constructor, or {@code null} if they cannot be determined.
	 * <p>Individual entries in the array may be {@code null} if parameter names are only
	 * available for some parameters of the given constructor but not for others. However,
	 * it is recommended to use stub parameter names instead wherever feasible.
	 * @param ctor the constructor to find parameter names for
	 * @return an array of parameter names if the names can be resolved,
	 * or {@code null} if they cannot
	 */
	// 返回构造函数的参数名称，如果无法确定，则返回 {@code null}。
	// <p>如果参数名称仅可用于给定构造函数的某些参数而不可用于其他参数，则数组中的各个条目可能为 {@code null}。
	// 但是，建议在可行的情况下改用存根参数名称。
	// @param ctor 构造函数来查找参数名称
	// @return 如果名称可以解析，则为参数名称数组，如果无法解析，则为 {@code null}
	@Nullable
	String[] getParameterNames(Constructor<?> ctor);

}
