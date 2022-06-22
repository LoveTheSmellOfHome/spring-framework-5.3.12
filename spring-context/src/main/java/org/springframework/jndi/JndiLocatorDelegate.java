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

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.springframework.core.SpringProperties;
import org.springframework.lang.Nullable;

/**
 * {@link JndiLocatorSupport} subclass with public lookup methods,
 * for convenient use as a delegate.
 *
 * @author Juergen Hoeller
 * @since 3.0.1
 */
// JndiLocatorSupport具有公共查找方法的子类，方便用作委托。
public class JndiLocatorDelegate extends JndiLocatorSupport {

	/**
	 * System property that instructs Spring to ignore a default JNDI environment, i.e.
	 * to always return {@code false} from {@link #isDefaultJndiEnvironmentAvailable()}.
	 * <p>The default is "false", allowing for regular default JNDI access e.g. in
	 * {@link JndiPropertySource}. Switching this flag to {@code true} is an optimization
	 * for scenarios where nothing is ever to be found for such JNDI fallback searches
	 * to begin with, avoiding the repeated JNDI lookup overhead.
	 * <p>Note that this flag just affects JNDI fallback searches, not explicitly configured
	 * JNDI lookups such as for a {@code DataSource} or some other environment resource.
	 * The flag literally just affects code which attempts JNDI searches based on the
	 * {@code JndiLocatorDelegate.isDefaultJndiEnvironmentAvailable()} check: in particular,
	 * {@code StandardServletEnvironment} and {@code StandardPortletEnvironment}.
	 * @since 4.3
	 * @see #isDefaultJndiEnvironmentAvailable()
	 * @see JndiPropertySource
	 */
	// 指示 Spring 忽略默认 JNDI 环境的系统属性，即始终从isDefaultJndiEnvironmentAvailable()返回false 。
	//
	// <p>默认值为“false”，允许常规默认 JNDI 访问，例如在JndiPropertySource 。 将此标志切换为true是针对此类
	// JNDI 回退搜索一开始就找不到任何内容的场景的优化，从而避免了重复的 JNDI 查找开销。
	//
	// <p>请注意，此标志仅影响 JNDI 回退搜索，而不是显式配置的 JNDI 查找，例如DataSource或某些其他环境资源。
	// 该标志字面上仅影响尝试基于JndiLocatorDelegate.isDefaultJndiEnvironmentAvailable()检查进行 JNDI 搜索的代码：特别是StandardServletEnvironment和StandardPortletEnvironment 。
	public static final String IGNORE_JNDI_PROPERTY_NAME = "spring.jndi.ignore";


	private static final boolean shouldIgnoreDefaultJndiEnvironment =
			SpringProperties.getFlag(IGNORE_JNDI_PROPERTY_NAME);


	@Override
	public Object lookup(String jndiName) throws NamingException {
		return super.lookup(jndiName);
	}

	@Override
	public <T> T lookup(String jndiName, @Nullable Class<T> requiredType) throws NamingException {
		return super.lookup(jndiName, requiredType);
	}


	/**
	 * Configure a {@code JndiLocatorDelegate} with its "resourceRef" property set to
	 * {@code true}, meaning that all names will be prefixed with "java:comp/env/".
	 * @see #setResourceRef
	 */
	// 配置JndiLocatorDelegate并将其“resourceRef”属性设置为true ，
	// 这意味着所有名称都将以“java:comp/env/”为前缀
	public static JndiLocatorDelegate createDefaultResourceRefLocator() {
		JndiLocatorDelegate jndiLocator = new JndiLocatorDelegate();
		jndiLocator.setResourceRef(true);
		return jndiLocator;
	}

	/**
	 * Check whether a default JNDI environment, as in a Java EE environment,
	 * is available on this JVM.
	 * @return {@code true} if a default InitialContext can be used,
	 * {@code false} if not
	 */
	// 检查默认 JNDI 环境（如在 Java EE 环境中）是否在此 JVM 上可用
	public static boolean isDefaultJndiEnvironmentAvailable() {
		if (shouldIgnoreDefaultJndiEnvironment) {
			return false;
		}
		try {
			new InitialContext().getEnvironment();
			return true;
		}
		catch (Throwable ex) {
			return false;
		}
	}

}
