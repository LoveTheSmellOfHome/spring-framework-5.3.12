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

package org.springframework.jndi;

import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.lang.Nullable;

/**
 * Convenient superclass for JNDI accessors, providing "jndiTemplate"
 * and "jndiEnvironment" bean properties.
 *
 * @author Juergen Hoeller
 * @since 1.1
 * @see #setJndiTemplate
 * @see #setJndiEnvironment
 */
// JNDI 访问器的便捷超类，提供“jndiTemplate”和“jndiEnvironment”bean 属性
public class JndiAccessor {

	/**
	 * Logger, available to subclasses.
	 */
	protected final Log logger = LogFactory.getLog(getClass());

	// 提供 JndiTemplate
	private JndiTemplate jndiTemplate = new JndiTemplate();


	/**
	 * Set the JNDI template to use for JNDI lookups.
	 * <p>You can also specify JNDI environment settings via "jndiEnvironment".
	 * @see #setJndiEnvironment
	 */
	// 设置 JNDI 模板以用于 JNDI 查找。
	// 您还可以通过“jndiEnvironment”指定 JNDI 环境设置
	public void setJndiTemplate(@Nullable JndiTemplate jndiTemplate) {
		this.jndiTemplate = (jndiTemplate != null ? jndiTemplate : new JndiTemplate());
	}

	/**
	 * Return the JNDI template to use for JNDI lookups.
	 */
	public JndiTemplate getJndiTemplate() {
		return this.jndiTemplate;
	}

	/**
	 * Set the JNDI environment to use for JNDI lookups.
	 * <p>Creates a JndiTemplate with the given environment settings.
	 * @see #setJndiTemplate
	 */
	// 设置 JNDI 环境以用于 JNDI 查找。
	// 使用给定的环境设置创建一个 JndiTemplate
	public void setJndiEnvironment(@Nullable Properties jndiEnvironment) {
		this.jndiTemplate = new JndiTemplate(jndiEnvironment);
	}

	/**
	 * Return the JNDI environment to use for JNDI lookups.
	 */
	@Nullable
	public Properties getJndiEnvironment() {
		return this.jndiTemplate.getEnvironment();
	}

}
