/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.web.context.request;

import org.springframework.lang.Nullable;

/**
 * Abstraction for accessing attribute objects associated with a request.
 * Supports access to request-scoped attributes as well as to session-scoped
 * attributes, with the optional notion of a "global session".
 *
 * <p>Can be implemented for any kind of request/session mechanism,
 * in particular for servlet requests.
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see ServletRequestAttributes
 */
// 用于访问与请求关联的属性对象的抽象。 支持访问请求范围的属性以及会话范围的属性，具有“全局会话”的可选概念。
// 可以为任何类型的请求/会话机制实现，特别是对于 servlet 请求。
public interface RequestAttributes {

	/**
	 * Constant that indicates request scope.
	 */
	// 指示请求范围的常量
	int SCOPE_REQUEST = 0;

	/**
	 * Constant that indicates session scope.
	 * <p>This preferably refers to a locally isolated session, if such
	 * a distinction is available.
	 * Else, it simply refers to the common session.
	 */
	// 指示会话范围的常量。
	// 这优选地指的是本地隔离的会话，如果这样的区别可用的话。 否则，它只是指公共会话。
	int SCOPE_SESSION = 1;


	/**
	 * Name of the standard reference to the request object: "request".
	 * @see #resolveReference
	 */
	// 请求对象的标准引用名称：“请求”。
	String REFERENCE_REQUEST = "request";

	/**
	 * Name of the standard reference to the session object: "session".
	 * @see #resolveReference
	 */
	// 会话对象的标准引用名称：“会话”
	String REFERENCE_SESSION = "session";


	/**
	 * Return the value for the scoped attribute of the given name, if any.
	 * @param name the name of the attribute
	 * @param scope the scope identifier
	 * @return the current attribute value, or {@code null} if not found
	 */
	// 返回给定名称的范围属性的值（如果有）。
	// 形参：
	//			name - 属性的名称
	//			scope - 范围标识符
	// 返回值：
	//			当前属性值，如果未找到，则为null
	@Nullable
	Object getAttribute(String name, int scope);

	/**
	 * Set the value for the scoped attribute of the given name,
	 * replacing an existing value (if any).
	 * @param name the name of the attribute
	 * @param scope the scope identifier
	 * @param value the value for the attribute
	 */
	// 为给定名称的范围属性设置值，替换现有值（如果有）。
	// 形参：
	//			name - 属性的名称
	//			value – 属性的值
	//			scope - 范围标识符
	void setAttribute(String name, Object value, int scope);

	/**
	 * Remove the scoped attribute of the given name, if it exists.
	 * <p>Note that an implementation should also remove a registered destruction
	 * callback for the specified attribute, if any. It does, however, <i>not</i>
	 * need to <i>execute</i> a registered destruction callback in this case,
	 * since the object will be destroyed by the caller (if appropriate).
	 * @param name the name of the attribute
	 * @param scope the scope identifier
	 */
	// 删除给定名称的范围属性（如果存在）。
	// 请注意，实现还应删除指定属性的注册销毁回调（如果有）。 它，然而没有必要在这种情况下要执行的注册破坏的回调，
	// 因为对象将被调用者被破坏（如适用）。
	// 形参：
	//			name - 属性的名称
	//			scope - 范围标识符
	void removeAttribute(String name, int scope);

	/**
	 * Retrieve the names of all attributes in the scope.
	 * @param scope the scope identifier
	 * @return the attribute names as String array
	 */
	// 检索范围内所有属性的名称。
	// 形参：
	//			scope- 范围标识符
	// 返回值：
	//			属性名称为字符串数组
	String[] getAttributeNames(int scope);

	/**
	 * Register a callback to be executed on destruction of the
	 * specified attribute in the given scope.
	 * <p>Implementations should do their best to execute the callback
	 * at the appropriate time: that is, at request completion or session
	 * termination, respectively. If such a callback is not supported by the
	 * underlying runtime environment, the callback <i>must be ignored</i>
	 * and a corresponding warning should be logged.
	 * <p>Note that 'destruction' usually corresponds to destruction of the
	 * entire scope, not to the individual attribute having been explicitly
	 * removed by the application. If an attribute gets removed via this
	 * facade's {@link #removeAttribute(String, int)} method, any registered
	 * destruction callback should be disabled as well, assuming that the
	 * removed object will be reused or manually destroyed.
	 * <p><b>NOTE:</b> Callback objects should generally be serializable if
	 * they are being registered for a session scope. Otherwise the callback
	 * (or even the entire session) might not survive web app restarts.
	 * @param name the name of the attribute to register the callback for
	 * @param callback the destruction callback to be executed
	 * @param scope the scope identifier
	 */
	// 注册要在给定范围内的指定属性销毁时执行的回调。
	// 实现应该尽力在适当的时间执行回调：即分别在请求完成或会话终止时。 如果底层运行时环境不支持此类回调，
	// 则必须忽略该回调并记录相应的警告。
	// 请注意，“销毁”通常对应于整个范围的销毁，而不是应用程序已明确删除的单个属性。 如果一个属性通过这个
	// 门面的 removeAttribute(String, int)方法被移除，任何注册的销毁回调也应该被禁用，假设被移除的对象将被重用或手动销毁。
	// 注意：如果回调对象是为会话范围注册的，则它们通常应该是可序列化的。 否则回调（甚至整个会话）可能无法
	// 在 Web 应用程序重新启动后继续存在。
	// 形参：
	//			name - 要为其注册回调的属性的名称
	//			callback - 要执行的销毁回调
	// 			scope - 范围标识符
	void registerDestructionCallback(String name, Runnable callback, int scope);

	/**
	 * Resolve the contextual reference for the given key, if any.
	 * <p>At a minimum: the HttpServletRequest reference for key "request", and
	 * the HttpSession reference for key "session".
	 * @param key the contextual key
	 * @return the corresponding object, or {@code null} if none found
	 */
	// 解析给定键的上下文引用（如果有）。
	// 至少：HttpServletRequest 的引用 "request" 和 HttpSession 的引用 "session"。
	// 形参：
	// 			key – 上下文键
	// 返回值：
	// 			相应的对象，如果没有找到，则为null
	@Nullable
	Object resolveReference(String key);

	/**
	 * Return an id for the current underlying session.
	 * @return the session id as String (never {@code null})
	 */
	// 返回当前底层会话的 id。
	// 返回值：会话 ID 作为字符串（从不为null ）
	String getSessionId();

	/**
	 * Expose the best available mutex for the underlying session:
	 * that is, an object to synchronize on for the underlying session.
	 * @return the session mutex to use (never {@code null})
	 */
	// 公开底层会话的最佳可用互斥锁：即，要为底层会话同步的对象。
	// 返回值：要使用的会话互斥锁（从不为null ）
	Object getSessionMutex();

}
