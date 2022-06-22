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

package org.springframework.jndi;

import javax.naming.Context;
import javax.naming.NamingException;

import org.springframework.lang.Nullable;

/**
 * Callback interface to be implemented by classes that need to perform an
 * operation (such as a lookup) in a JNDI context. This callback approach
 * is valuable in simplifying error handling, which is performed by the
 * JndiTemplate class. This is a similar to JdbcTemplate's approach.
 *
 * <p>Note that there is hardly any need to implement this callback
 * interface, as JndiTemplate provides all usual JNDI operations via
 * convenience methods.
 *
 * @author Rod Johnson
 * @param <T> the resulting object type
 * @see JndiTemplate
 * @see org.springframework.jdbc.core.JdbcTemplate
 */
// 回调接口由需要在 JNDI 上下文中执行操作（例如查找）的类实现。这种回调方法对于简化
// 由 JndiTemplate 类执行的错误处理很有价值。这类似于 JdbcTemplate 的方法。
//
// <p>请注意，几乎不需要实现此回调接口，因为 JndiTemplate 通过便捷方法提供了所有常用的 JNDI 操作
@FunctionalInterface
public interface JndiCallback<T> {

	/**
	 * Do something with the given JNDI context.
	 * <p>Implementations don't need to worry about error handling
	 * or cleanup, as the JndiTemplate class will handle this.
	 * @param ctx the current JNDI context
	 * @return a result object, or {@code null}
	 * @throws NamingException if thrown by JNDI methods
	 */
	// 使用给定的 JNDI 上下文做一些事情
	// <p>实现不需要担心错误处理或清理，因为 JndiTemplate 类会处理这个
	// @param ctx 当前 JNDI 上下文
	// @return  结果对象，或 {@code null}
	@Nullable
	T doInContext(Context ctx) throws NamingException;

}

