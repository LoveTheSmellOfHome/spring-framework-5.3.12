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

package org.springframework.beans.factory;

/**
 * Interface to be implemented by beans that need to react once all their properties
 * have been set by a {@link BeanFactory}: e.g. to perform custom initialization,
 * or merely to check that all mandatory properties have been set.
 *
 * <p>An alternative to implementing {@code InitializingBean} is specifying a custom
 * init method, for example in an XML bean definition. For a list of all bean
 * lifecycle methods, see the {@link BeanFactory BeanFactory javadocs}.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see DisposableBean
 * @see org.springframework.beans.factory.config.BeanDefinition#getPropertyValues()
 * @see org.springframework.beans.factory.support.AbstractBeanDefinition#getInitMethodName()
 */
// 由需要在 {@link BeanFactory} 设置所有属性后做出反应的 bean 实现的接口：例如执行自定义初始化，或仅检查是否已设置所有必需属性
// <p>实现 {@code InitializingBean} 的另一种方法是指定自定义 init 方法，例如在 XML bean 定义中。
// 有关所有 bean 生命周期方法的列表，请参阅 {@link BeanFactory BeanFactory javadocs}。
public interface InitializingBean {

	/**
	 * Invoked by the containing {@code BeanFactory} after it has set all bean properties
	 * and satisfied {@link BeanFactoryAware}, {@code ApplicationContextAware} etc.
	 * <p>This method allows the bean instance to perform validation of its overall
	 * configuration and final initialization when all bean properties have been set.
	 * @throws Exception in the event of misconfiguration (such as failure to set an
	 * essential property) or if initialization fails for any other reason
	 */
	// 在设置所有 bean 属性并满足 {@link BeanFactoryAware}、{@code ApplicationContextAware} 等之后，
	// 由包含的 {@code BeanFactory} 调用。
	// <p>此方法允许 bean 实例对其整体配置和最终初始化进行验证当所有 bean 属性都已设置时。
	// @throws 异常在配置错误的情况下（例如未能设置基本属性）或初始化因任何其他原因失败
	void afterPropertiesSet() throws Exception;

}
