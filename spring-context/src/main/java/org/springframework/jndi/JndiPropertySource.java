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

package org.springframework.jndi;

import org.springframework.core.env.PropertySource;
import org.springframework.lang.Nullable;

import javax.naming.NamingException;

/**
 * {@link PropertySource} implementation that reads properties from an underlying Spring
 * {@link JndiLocatorDelegate}.
 *
 * <p>By default, the underlying {@code JndiLocatorDelegate} will be configured with its
 * {@link JndiLocatorDelegate#setResourceRef(boolean) "resourceRef"} property set to
 * {@code true}, meaning that names looked up will automatically be prefixed with
 * "java:comp/env/" in alignment with published
 * <a href="https://download.oracle.com/javase/jndi/tutorial/beyond/misc/policy.html">JNDI
 * naming conventions</a>. To override this setting or to change the prefix, manually
 * configure a {@code JndiLocatorDelegate} and provide it to one of the constructors here
 * that accepts it. The same applies when providing custom JNDI properties. These should
 * be specified using {@link JndiLocatorDelegate#setJndiEnvironment(java.util.Properties)}
 * prior to construction of the {@code JndiPropertySource}.
 *
 * <p>Note that {@link org.springframework.web.context.support.StandardServletEnvironment
 * StandardServletEnvironment} includes a {@code JndiPropertySource} by default, and any
 * customization of the underlying {@link JndiLocatorDelegate} may be performed within an
 * {@link org.springframework.context.ApplicationContextInitializer
 * ApplicationContextInitializer} or {@link org.springframework.web.WebApplicationInitializer
 * WebApplicationInitializer}.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 3.1
 * @see JndiLocatorDelegate
 * @see org.springframework.context.ApplicationContextInitializer
 * @see org.springframework.web.WebApplicationInitializer
 * @see org.springframework.web.context.support.StandardServletEnvironment
 */
// 从底层 {@link JndiLocatorDelegate} 读取属性的 {@link PropertySource} 实现。
//
// <p>默认情况下，底层JndiLocatorDelegate将被配置为将其"resourceRef"属性设置为true ，这意味着查找的名称将
// 自动以“java:comp/env/”为前缀，与已发布的JNDI 命名约定保持一致 。 要覆盖此设置或更改前缀，请手动配置
// {@code JndiLocatorDelegate}并将其提供给此处接受它的构造函数之一。 这同样适用于提供自定义 JNDI 属性时。 这些应该在
// 构造JndiPropertySource之前使用JndiLocatorDelegate.setJndiEnvironment(java.util.Properties)指定。
//
// <p>请注意， StandardServletEnvironment默认包含JndiPropertySource ，底层JndiLocatorDelegate任何自定义
// 都可以在ApplicationContextInitializer或WebApplicationInitializer 。
//
// Spring 內建的配置属性源 - JDNI 配置属性源
public class JndiPropertySource extends PropertySource<JndiLocatorDelegate> {

	/**
	 * Create a new {@code JndiPropertySource} with the given name
	 * and a {@link JndiLocatorDelegate} configured to prefix any names with
	 * "java:comp/env/".
	 */
	// 使用给定的名称和JndiLocatorDelegate创建一个新的JndiPropertySource ，配置为在任何名称前加上“java:comp/env/”
	public JndiPropertySource(String name) {
		this(name, JndiLocatorDelegate.createDefaultResourceRefLocator());
	}

	/**
	 * Create a new {@code JndiPropertySource} with the given name and the given
	 * {@code JndiLocatorDelegate}.
	 */
	// 创建一个新的JndiPropertySource与给定的名称和给定JndiLocatorDelegate
	public JndiPropertySource(String name, JndiLocatorDelegate jndiLocator) {
		super(name, jndiLocator);
	}


	/**
	 * This implementation looks up and returns the value associated with the given
	 * name from the underlying {@link JndiLocatorDelegate}. If a {@link NamingException}
	 * is thrown during the call to {@link JndiLocatorDelegate#lookup(String)}, returns
	 * {@code null} and issues a DEBUG-level log statement with the exception message.
	 */
	// 此实现从底层JndiLocatorDelegate查找并返回与给定名称关联的值。 如果在调用JndiLocatorDelegate.lookup(String)
	// 期间抛出NamingException ，则返回null并发出带有异常消息的调试级日志语句。
	@Override
	@Nullable
	public Object getProperty(String name) {
		if (getSource().isResourceRef() && name.indexOf(':') != -1) {
			// We're in resource-ref (prefixing with "java:comp/env") mode. Let's not bother
			// with property names with a colon it since they're probably just containing a
			// default value clause, very unlikely to match including the colon part even in
			// a textual property source, and effectively never meant to match that way in
			// JNDI where a colon indicates a separator between JNDI scheme and actual name.
			return null;
		}

		try {
			Object value = this.source.lookup(name);
			if (logger.isDebugEnabled()) {
				logger.debug("JNDI lookup for name [" + name + "] returned: [" + value + "]");
			}
			return value;
		}
		catch (NamingException ex) {
			if (logger.isDebugEnabled()) {
				logger.debug("JNDI lookup for name [" + name + "] threw NamingException " +
						"with message: " + ex.getMessage() + ". Returning null.");
			}
			return null;
		}
	}

}
