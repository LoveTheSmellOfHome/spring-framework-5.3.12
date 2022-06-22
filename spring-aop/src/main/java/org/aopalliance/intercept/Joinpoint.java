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

package org.aopalliance.intercept;

import java.lang.reflect.AccessibleObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * This interface represents a generic runtime joinpoint (in the AOP
 * terminology).
 *
 * <p>A runtime joinpoint is an <i>event</i> that occurs on a static
 * joinpoint (i.e. a location in a program). For instance, an
 * invocation is the runtime joinpoint on a method (static joinpoint).
 * The static part of a given joinpoint can be generically retrieved
 * using the {@link #getStaticPart()} method.
 *
 * <p>In the context of an interception framework, a runtime joinpoint
 * is then the reification of an access to an accessible object (a
 * method, a constructor, a field), i.e. the static part of the
 * joinpoint. It is passed to the interceptors that are installed on
 * the static joinpoint.
 *
 * @author Rod Johnson
 * @see Interceptor
 */
// 此接口表示通用运行时连接点（在 AOP 术语中）。来自 AOP 联盟的概念
//
// 运行时连接点是，在一个静态的连接点时发生的事件（即，在一个程序中的位置）。
// 例如，调用是方法上的运行时连接点（静态连接点）。 可以使用getStaticPart()方法一般检索给定连接点的静态部分。
//
// 在拦截框架的上下文中，运行时连接点是对可访问对象（方法、构造函数、字段）的访问的具体化，即连接点的静态部分。
// 它被传递给安装在静态连接点上的拦截器。
//
// 连接点或拦截点，在 Spring 中只有方法级别的连接点即 Invocation
public interface Joinpoint {

	/**
	 * Proceed to the next interceptor in the chain.
	 * <p>The implementation and the semantics of this method depends
	 * on the actual joinpoint type (see the children interfaces).
	 * @return see the children interfaces' proceed definition
	 * @throws Throwable if the joinpoint throws an exception
	 */
	// 继续处理链中的下一个拦截器。方法的调用
	// 此方法的实现和语义取决于实际的连接点类型（请参阅子接口）。
	// 返回值：
	//			查看子接口的继续定义
	// 异常：
	//			Throwable - 如果连接点抛出异常
	@Nullable
	Object proceed() throws Throwable;

	/**
	 * Return the object that holds the current joinpoint's static part.
	 * <p>For instance, the target object for an invocation.
	 * @return the object (can be null if the accessible object is static)
	 */
	// 返回保存当前连接点静态部分的对象。当前连接点所把持的对象，即调用当前方法的目标对象
	// 例如，调用的目标对象。
	// 返回值：
	//			对象（如果可访问的对象是静态的，则可以为 null）
	@Nullable
	Object getThis();

	/**
	 * Return the static part of this joinpoint.
	 * <p>The static part is an accessible object on which a chain of
	 * interceptors are installed.
	 */
	// 返回此连接点的静态部分。Spring 中指方法
	// 和 {@Link org.aopalliance.intercept.MethodInvocation}#getMethod() 返回的是同一个对象
	// 静态部分是一个可访问的对象，其上安装了一系列拦截器。
	// AccessibleObject 类是基于java 反射的 Field、Method 和 Constructor 对象的父类
	@Nonnull
	AccessibleObject getStaticPart();

}
