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

package org.springframework.jndi;

import java.util.Hashtable;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;

/**
 * Helper class that simplifies JNDI operations. It provides methods to lookup and
 * bind objects, and allows implementations of the {@link JndiCallback} interface
 * to perform any operation they like with a JNDI naming context provided.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see JndiCallback
 * @see #execute
 */
// 简化 JNDI 操作的助手类。 它提供了查找和绑定对象的方法，并允许 {@link JndiCallback} 接口的实现使用
// 提供的 JNDI 命名上下文执行他们喜欢的任何操作
public class JndiTemplate {

	protected final Log logger = LogFactory.getLog(getClass());

	@Nullable
	private Properties environment;


	/**
	 * Create a new JndiTemplate instance.
	 */
	// 创建一个新的 JndiTemplate 实例
	public JndiTemplate() {
	}

	/**
	 * Create a new JndiTemplate instance, using the given environment.
	 */
	// 使用给定的环境创建一个新的 JndiTemplate 实例
	public JndiTemplate(@Nullable Properties environment) {
		this.environment = environment;
	}


	/**
	 * Set the environment for the JNDI InitialContext.
	 */
	// 为 JNDI InitialContext 设置环境
	public void setEnvironment(@Nullable Properties environment) {
		this.environment = environment;
	}

	/**
	 * Return the environment for the JNDI InitialContext, if any.
	 */
	@Nullable
	public Properties getEnvironment() {
		return this.environment;
	}


	/**
	 * Execute the given JNDI context callback implementation.
	 * @param contextCallback the JndiCallback implementation to use
	 * @return a result object returned by the callback, or {@code null}
	 * @throws NamingException thrown by the callback implementation
	 * @see #createInitialContext
	 */
	// 执行给定的 JNDI 上下文回调实现
	// @param contextCallback 要使用的 JndiCallback 实现
	// @return 回调返回的结果对象，或者 {@code null}
	// @throws NamingException 由回调实现抛出
	@Nullable
	public <T> T execute(JndiCallback<T> contextCallback) throws NamingException {
		Context ctx = getContext();
		try {
			return contextCallback.doInContext(ctx);
		}
		finally {
			releaseContext(ctx);
		}
	}

	/**
	 * Obtain a JNDI context corresponding to this template's configuration.
	 * Called by {@link #execute}; may also be called directly.
	 * <p>The default implementation delegates to {@link #createInitialContext()}.
	 * @return the JNDI context (never {@code null})
	 * @throws NamingException if context retrieval failed
	 * @see #releaseContext
	 */
	// 获取与此模板的配置相对应的 JNDI 上下文。由 {@link #execute} 调用；也可以直接调用
	// <p>默认实现委托给 {@link createInitialContext()}。
	// @return JNDI 上下文（从不{@code null}）
	// @throws NamingException 如果上下文检索失败
	public Context getContext() throws NamingException {
		return createInitialContext();
	}

	/**
	 * Release a JNDI context as obtained from {@link #getContext()}.
	 * @param ctx the JNDI context to release (may be {@code null})
	 * @see #getContext
	 */
	// 释放从 {@link #getContext()} 获得的 JNDI 上下文
	// @param ctx 要释放的 JNDI 上下文（可能是 {@code null}）
	public void releaseContext(@Nullable Context ctx) {
		if (ctx != null) {
			try {
				ctx.close();
			}
			catch (NamingException ex) {
				logger.debug("Could not close JNDI InitialContext", ex);
			}
		}
	}

	/**
	 * Create a new JNDI initial context. Invoked by {@link #getContext}.
	 * <p>The default implementation use this template's environment settings.
	 * Can be subclassed for custom contexts, e.g. for testing.
	 * @return the initial Context instance
	 * @throws NamingException in case of initialization errors
	 */
	// 创建一个新的 JNDI 初始上下文。由 {@link #getContext} 调用。
	// <p>默认实现使用此模板的环境设置。可以为自定义上下文进行子类化，例如供测试用。
	// @return 初始化 Context 实例
	// @throws NamingException 在初始化错误的情况下
	protected Context createInitialContext() throws NamingException {
		Hashtable<?, ?> icEnv = null;
		Properties env = getEnvironment();
		if (env != null) {
			icEnv = new Hashtable<>(env.size());
			CollectionUtils.mergePropertiesIntoMap(env, icEnv);
		}
		return new InitialContext(icEnv);
	}


	/**
	 * Look up the object with the given name in the current JNDI context.
	 * @param name the JNDI name of the object
	 * @return object found (cannot be {@code null}; if a not so well-behaved
	 * JNDI implementations returns null, a NamingException gets thrown)
	 * @throws NamingException if there is no object with the given
	 * name bound to JNDI
	 */
	// 在当前 JNDI 上下文中查找具有给定名称的对象
	// @param name 对象的 JNDI 名称
	// @return 找到的对象（不能是 {@code null}；如果行为不正常的 JNDI 实现返回 null，则抛出 NamingException）
	// @throws NamingException 如果没有给定名称的对象绑定到 JNDI
	public Object lookup(final String name) throws NamingException {
		if (logger.isDebugEnabled()) {
			logger.debug("Looking up JNDI object with name [" + name + "]");
		}
		Object result = execute(ctx -> ctx.lookup(name));
		if (result == null) {
			throw new NameNotFoundException(
					"JNDI object with [" + name + "] not found: JNDI implementation returned null");
		}
		return result;
	}

	/**
	 * Look up the object with the given name in the current JNDI context.
	 * @param name the JNDI name of the object
	 * @param requiredType type the JNDI object must match. Can be an interface or
	 * superclass of the actual class, or {@code null} for any match. For example,
	 * if the value is {@code Object.class}, this method will succeed whatever
	 * the class of the returned instance.
	 * @return object found (cannot be {@code null}; if a not so well-behaved
	 * JNDI implementations returns null, a NamingException gets thrown)
	 * @throws NamingException if there is no object with the given
	 * name bound to JNDI
	 */
	// 在当前 JNDI 上下文中查找具有给定名称的对象
	// @param name 对象的 JNDI 名称
	// @param requiredType 类型 JNDI 对象必须匹配。可以是实际类的接口或超类，也可以是任何匹配项的 {@code null}。
	// 例如，如果值为 {@code Object.class}，则无论返回实例的类是什么，此方法都会成功
	// @return 找到的对象（不能是 {@code null}；如果行为不正常的 JNDI 实现返回 null，则抛出 NamingException）
	// @throws NamingException 如果没有给定名称的对象绑定到 JNDI
	@SuppressWarnings("unchecked")
	public <T> T lookup(String name, @Nullable Class<T> requiredType) throws NamingException {
		Object jndiObject = lookup(name);
		if (requiredType != null && !requiredType.isInstance(jndiObject)) {
			throw new TypeMismatchNamingException(name, requiredType, jndiObject.getClass());
		}
		return (T) jndiObject;
	}

	/**
	 * Bind the given object to the current JNDI context, using the given name.
	 * @param name the JNDI name of the object
	 * @param object the object to bind
	 * @throws NamingException thrown by JNDI, mostly name already bound
	 */
	// 使用给定的名称将给定的对象绑定到当前的 JNDI 上下文
	// @param name 对象的 JNDI 名称
	// @param object 要绑定的对象
	// @throws NamingException 由 JNDI 抛出，大部分名称已绑定
	public void bind(final String name, final Object object) throws NamingException {
		if (logger.isDebugEnabled()) {
			logger.debug("Binding JNDI object with name [" + name + "]");
		}
		execute(ctx -> {
			ctx.bind(name, object);
			return null;
		});
	}

	/**
	 * Rebind the given object to the current JNDI context, using the given name.
	 * Overwrites any existing binding.
	 * @param name the JNDI name of the object
	 * @param object the object to rebind
	 * @throws NamingException thrown by JNDI
	 */
	// 使用给定的名称将给定的对象重新绑定到当前的 JNDI 上下文。覆盖任何现有的绑定
	// @param name 对象的 JNDI 名称
	// @param object 要重新绑定的对象
	// @throws NamingException 由 JNDI 抛出
	public void rebind(final String name, final Object object) throws NamingException {
		if (logger.isDebugEnabled()) {
			logger.debug("Rebinding JNDI object with name [" + name + "]");
		}
		execute(ctx -> {
			ctx.rebind(name, object);
			return null;
		});
	}

	/**
	 * Remove the binding for the given name from the current JNDI context.
	 * @param name the JNDI name of the object
	 * @throws NamingException thrown by JNDI, mostly name not found
	 */
	// 从当前 JNDI 上下文中删除给定名称的绑定
	// @param name 对象的 JNDI 名称
	// @throws NamingException 由 JNDI 抛出，主要是未找到名称
	public void unbind(final String name) throws NamingException {
		if (logger.isDebugEnabled()) {
			logger.debug("Unbinding JNDI object with name [" + name + "]");
		}
		execute(ctx -> {
			ctx.unbind(name);
			return null;
		});
	}

}
