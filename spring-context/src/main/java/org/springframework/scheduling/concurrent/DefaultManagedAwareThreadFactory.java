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

package org.springframework.scheduling.concurrent;

import java.util.Properties;
import java.util.concurrent.ThreadFactory;

import javax.naming.NamingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.jndi.JndiLocatorDelegate;
import org.springframework.jndi.JndiTemplate;
import org.springframework.lang.Nullable;

/**
 * JNDI-based variant of {@link CustomizableThreadFactory}, performing a default lookup
 * for JSR-236's "java:comp/DefaultManagedThreadFactory" in a Java EE 7 environment,
 * falling back to the local {@link CustomizableThreadFactory} setup if not found.
 *
 * <p>This is a convenient way to use managed threads when running in a Java EE 7
 * environment, simply using regular local threads otherwise - without conditional
 * setup (i.e. without profiles).
 *
 * <p>Note: This class is not strictly JSR-236 based; it can work with any regular
 * {@link java.util.concurrent.ThreadFactory} that can be found in JNDI. Therefore,
 * the default JNDI name "java:comp/DefaultManagedThreadFactory" can be customized
 * through the {@link #setJndiName "jndiName"} bean property.
 *
 * @author Juergen Hoeller
 * @since 4.0
 */
// CustomizableThreadFactory 基于 JNDI 的变体，在 Java EE 7 环境中对 JSR-236 的
// “java:comp/DefaultManagedThreadFactory” 执行默认查找，如果未找到则回退到本地 CustomizableThreadFactory 设置。
//
// 这是在 Java EE 7 环境中运行时使用托管线程的便捷方式，否则只需使用常规本地线程 - 无需条件设置（即无需配置文件）。
//
// 注意：此类并非严格基于 JSR-236；它可以与可以在 JNDI 中找到的任何常规ThreadFactory一起使用。因此，
// 可以通过"jndiName" bean 属性自定义默认的 JNDI 名称 “java:comp/DefaultManagedThreadFactory”
@SuppressWarnings("serial")
public class DefaultManagedAwareThreadFactory extends CustomizableThreadFactory implements InitializingBean {

	protected final Log logger = LogFactory.getLog(getClass());

	private JndiLocatorDelegate jndiLocator = new JndiLocatorDelegate();

	@Nullable
	private String jndiName = "java:comp/DefaultManagedThreadFactory";

	@Nullable
	private ThreadFactory threadFactory;


	/**
	 * Set the JNDI template to use for JNDI lookups.
	 * @see org.springframework.jndi.JndiAccessor#setJndiTemplate
	 */
	// 设置 JNDI 模板以用于 JNDI 查找
	public void setJndiTemplate(JndiTemplate jndiTemplate) {
		this.jndiLocator.setJndiTemplate(jndiTemplate);
	}

	/**
	 * Set the JNDI environment to use for JNDI lookups.
	 * @see org.springframework.jndi.JndiAccessor#setJndiEnvironment
	 */
	// 设置 JNDI 环境以用于 JNDI 查找
	public void setJndiEnvironment(Properties jndiEnvironment) {
		this.jndiLocator.setJndiEnvironment(jndiEnvironment);
	}

	/**
	 * Set whether the lookup occurs in a Java EE container, i.e. if the prefix
	 * "java:comp/env/" needs to be added if the JNDI name doesn't already
	 * contain it. PersistenceAnnotationBeanPostProcessor's default is "true".
	 * @see org.springframework.jndi.JndiLocatorSupport#setResourceRef
	 */
	// 设置查找是否发生在 Java EE 容器中，即如果 JNDI 名称尚未包含前缀“java:comp/env/”，是否需要添加它。
	// PersistenceAnnotationBeanPostProcessor 的默认值为“true”。
	public void setResourceRef(boolean resourceRef) {
		this.jndiLocator.setResourceRef(resourceRef);
	}

	/**
	 * Specify a JNDI name of the {@link java.util.concurrent.ThreadFactory} to delegate to,
	 * replacing the default JNDI name "java:comp/DefaultManagedThreadFactory".
	 * <p>This can either be a fully qualified JNDI name, or the JNDI name relative
	 * to the current environment naming context if "resourceRef" is set to "true".
	 * @see #setResourceRef
	 */
	// 指定要委托给的 ThreadFactory 的 JNDI 名称，替换默认的 JNDI 名称“java:comp/DefaultManagedThreadFactory”。
	// 这可以是完全限定的 JNDI 名称，也可以是相对于当前环境命名上下文的 JNDI 名称（如果“resourceRef”设置为“true”）。
	public void setJndiName(String jndiName) {
		this.jndiName = jndiName;
	}

	@Override
	public void afterPropertiesSet() throws NamingException {
		if (this.jndiName != null) {
			try {
				this.threadFactory = this.jndiLocator.lookup(this.jndiName, ThreadFactory.class);
			}
			catch (NamingException ex) {
				if (logger.isTraceEnabled()) {
					logger.trace("Failed to retrieve [" + this.jndiName + "] from JNDI", ex);
				}
				logger.info("Could not find default managed thread factory in JNDI - " +
						"proceeding with default local thread factory");
			}
		}
	}


	@Override
	public Thread newThread(Runnable runnable) {
		if (this.threadFactory != null) {
			return this.threadFactory.newThread(runnable);
		}
		else {
			return super.newThread(runnable);
		}
	}

}
