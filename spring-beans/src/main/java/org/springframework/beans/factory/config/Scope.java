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

package org.springframework.beans.factory.config;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.lang.Nullable;

/**
 * Strategy interface used by a {@link ConfigurableBeanFactory},
 * representing a target scope to hold bean instances in.
 * This allows for extending the BeanFactory's standard scopes
 * {@link ConfigurableBeanFactory#SCOPE_SINGLETON "singleton"} and
 * {@link ConfigurableBeanFactory#SCOPE_PROTOTYPE "prototype"}
 * with custom further scopes, registered for a
 * {@link ConfigurableBeanFactory#registerScope(String, Scope) specific key}.
 *
 * <p>{@link org.springframework.context.ApplicationContext} implementations
 * such as a {@link org.springframework.web.context.WebApplicationContext}
 * may register additional standard scopes specific to their environment,
 * e.g. {@link org.springframework.web.context.WebApplicationContext#SCOPE_REQUEST "request"}
 * and {@link org.springframework.web.context.WebApplicationContext#SCOPE_SESSION "session"},
 * based on this Scope SPI.
 *
 * <p>Even if its primary use is for extended scopes in a web environment,
 * this SPI is completely generic: It provides the ability to get and put
 * objects from any underlying storage mechanism, such as an HTTP session
 * or a custom conversation mechanism. The name passed into this class's
 * {@code get} and {@code remove} methods will identify the
 * target object in the current scope.
 *
 * <p>{@code Scope} implementations are expected to be thread-safe.
 * One {@code Scope} instance can be used with multiple bean factories
 * at the same time, if desired (unless it explicitly wants to be aware of
 * the containing BeanFactory), with any number of threads accessing
 * the {@code Scope} concurrently from any number of factories.
 *
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @since 2.0
 * @see ConfigurableBeanFactory#registerScope
 * @see CustomScopeConfigurer
 * @see org.springframework.aop.scope.ScopedProxyFactoryBean
 * @see org.springframework.web.context.request.RequestScope
 * @see org.springframework.web.context.request.SessionScope
 */
// ConfigurableBeanFactory使用的 Strategy 接口，代表一个目标范围来保存 bean 实例。
// 这允许使用自定义的进一步范围扩展 BeanFactory 的标准范围"singleton"和"prototype" ，为specific key注册。
//
// org.springframework.context.ApplicationContext实现，例如
// org.springframework.web.context.WebApplicationContext 可以注册额外的标准范围特定于他们的环境，
// 例如"request"和"session" ，基于这个范围 SPI。
//
// 即使它的主要用途是用于 Web 环境中的扩展范围，该 SPI 也是完全通用的：它提供了从任何底层存储
// 机制（例如 HTTP 会话或自定义对话机制）获取和放置对象的能力。 传递给此类的get和remove方法的名称将标识当前范围内的目标对象。
//
// Scope实现预计是线程安全的。 如果需要，一个Scope实例可以同时与多个 bean 工厂一起使用（除非它明确想知道包含的 BeanFactory），
// 任意数量的线程从任意数量的工厂并发访问Scope 。
public interface Scope {

	/**
	 * Return the object with the given name from the underlying scope,
	 * {@link org.springframework.beans.factory.ObjectFactory#getObject() creating it}
	 * if not found in the underlying storage mechanism.
	 * <p>This is the central operation of a Scope, and the only operation
	 * that is absolutely required.
	 * @param name the name of the object to retrieve
	 * @param objectFactory the {@link ObjectFactory} to use to create the scoped
	 * object if it is not present in the underlying storage mechanism
	 * @return the desired object (never {@code null})
	 * @throws IllegalStateException if the underlying scope is not currently active
	 */
	// 从底层范围返回具有给定名称的对象，如果在底层存储机制中找不到，则creating it 。
	// 这是 Scope 的核心操作，也是唯一绝对需要的操作。
	// 形参：
	//			name - 要检索的对象的名称
	//			objectFactory – 用于创建作用域对象的ObjectFactory ，如果它不存在于底层存储机制中
	// 返回值：
	//			所需的对象（从不为null ）
	// IllegalStateException – 如果基础作用域当前未处于活动状态
	Object get(String name, ObjectFactory<?> objectFactory);

	/**
	 * Remove the object with the given {@code name} from the underlying scope.
	 * <p>Returns {@code null} if no object was found; otherwise
	 * returns the removed {@code Object}.
	 * <p>Note that an implementation should also remove a registered destruction
	 * callback for the specified object, if any. It does, however, <i>not</i>
	 * need to <i>execute</i> a registered destruction callback in this case,
	 * since the object will be destroyed by the caller (if appropriate).
	 * <p><b>Note: This is an optional operation.</b> Implementations may throw
	 * {@link UnsupportedOperationException} if they do not support explicitly
	 * removing an object.
	 * @param name the name of the object to remove
	 * @return the removed object, or {@code null} if no object was present
	 * @throws IllegalStateException if the underlying scope is not currently active
	 * @see #registerDestructionCallback
	 */
	// 从底层作用域中删除具有给定name的对象。
	// 如果没有找到对象，则返回null ； 否则返回移除的Object 。
	// 请注意，实现还应删除指定对象的注册销毁回调（如果有）。 它，然而，没有必要在这种情况下要执行的注册破坏的回调，
	// 因为对象将被调用者被破坏（如适用）。
	// 注意：这是一个可选操作。 如果实现不支持显式删除对象，则它们可能会抛出UnsupportedOperationException 。
	// 形参：
	//			name - 要删除的对象的名称
	// 返回值：
	//			删除的对象，如果不存在对象，则为null
	// IllegalStateException – 如果基础作用域当前未处于活动状态
	@Nullable
	Object remove(String name);

	/**
	 * Register a callback to be executed on destruction of the specified
	 * object in the scope (or at destruction of the entire scope, if the
	 * scope does not destroy individual objects but rather only terminates
	 * in its entirety).
	 * <p><b>Note: This is an optional operation.</b> This method will only
	 * be called for scoped beans with actual destruction configuration
	 * (DisposableBean, destroy-method, DestructionAwareBeanPostProcessor).
	 * Implementations should do their best to execute a given callback
	 * at the appropriate time. If such a callback is not supported by the
	 * underlying runtime environment at all, the callback <i>must be
	 * ignored and a corresponding warning should be logged</i>.
	 * <p>Note that 'destruction' refers to automatic destruction of
	 * the object as part of the scope's own lifecycle, not to the individual
	 * scoped object having been explicitly removed by the application.
	 * If a scoped object gets removed via this facade's {@link #remove(String)}
	 * method, any registered destruction callback should be removed as well,
	 * assuming that the removed object will be reused or manually destroyed.
	 * @param name the name of the object to execute the destruction callback for
	 * @param callback the destruction callback to be executed.
	 * Note that the passed-in Runnable will never throw an exception,
	 * so it can safely be executed without an enclosing try-catch block.
	 * Furthermore, the Runnable will usually be serializable, provided
	 * that its target object is serializable as well.
	 * @throws IllegalStateException if the underlying scope is not currently active
	 * @see org.springframework.beans.factory.DisposableBean
	 * @see org.springframework.beans.factory.support.AbstractBeanDefinition#getDestroyMethodName()
	 * @see DestructionAwareBeanPostProcessor
	 */
	// 注册一个回调，以在销毁范围内的指定对象时执行（或在销毁整个范围时，如果范围不销毁单个对象而是仅整个终止）。
	//
	// 注意：这是一个可选操作。 只会为具有实际销毁配置（DisposableBean、destroy-method、
	// DestructionAwareBeanPostProcessor）的作用域 bean 调用此方法。 实现应该尽力在适当的时间执行给定的回调。
	// 如果底层运行时环境根本不支持这种回调，则必须忽略该回调并记录相应的警告。
	//
	// 请注意，“销毁”是指将对象作为范围自身生命周期的一部分自动销毁，而不是指已被应用程序明确删除的单个范围对象。
	// 如果一个作用域对象通过这个门面的remove(String)方法被移除，任何注册的销毁回调也应该被移除，
	// 假设被移除的对象将被重用或手动销毁。
	// 形参：
	// 			name - 要为其执行销毁回调的对象的名称
	// 			callback - 要执行的销毁回调。 请注意，传入的 Runnable 永远不会抛出异常，因此它可以在没有封闭的 try-catch
	// 块的情况下安全地执行。 此外，Runnable 通常是可序列化的，前提是它的目标对象也是可序列化的。
	// IllegalStateException – 如果基础作用域当前未处于活动状态
	void registerDestructionCallback(String name, Runnable callback);

	/**
	 * Resolve the contextual object for the given key, if any.
	 * E.g. the HttpServletRequest object for key "request".
	 * @param key the contextual key
	 * @return the corresponding object, or {@code null} if none found
	 * @throws IllegalStateException if the underlying scope is not currently active
	 */
	// 解析给定键的上下文对象（如果有）。 例如，键“请求”的 HttpServletRequest 对象。
	// 形参：
	// 			key – 上下文键
	// 返回值：
	//			相应的对象，如果没有找到，则为null
	// IllegalStateException – 如果基础作用域当前未处于活动状态
	@Nullable
	Object resolveContextualObject(String key);

	/**
	 * Return the <em>conversation ID</em> for the current underlying scope, if any.
	 * <p>The exact meaning of the conversation ID depends on the underlying
	 * storage mechanism. In the case of session-scoped objects, the
	 * conversation ID would typically be equal to (or derived from) the
	 * {@link javax.servlet.http.HttpSession#getId() session ID}; in the
	 * case of a custom conversation that sits within the overall session,
	 * the specific ID for the current conversation would be appropriate.
	 * <p><b>Note: This is an optional operation.</b> It is perfectly valid to
	 * return {@code null} in an implementation of this method if the
	 * underlying storage mechanism has no obvious candidate for such an ID.
	 * @return the conversation ID, or {@code null} if there is no
	 * conversation ID for the current scope
	 * @throws IllegalStateException if the underlying scope is not currently active
	 */
	// 返回当前基础范围的对话 ID （如果有）。
	//
	// 会话 ID 的确切含义取决于底层存储机制。 在会话范围对象的情况下，会话 ID 通常等于（或派生自） session ID ；
	// 对于位于整个会话中的自定义对话，当前对话的特定 ID 将是合适的。
	//
	// 注意：这是一个可选操作。 如果底层存储机制没有明显的此类 ID 候选者，则在此方法的实现中返回null是完全有效的。
	// 返回值：
	//			对话 ID，如果当前范围没有对话 ID，则为null
	// IllegalStateException – 如果基础作用域当前未处于活动状态
	@Nullable
	String getConversationId();

}
