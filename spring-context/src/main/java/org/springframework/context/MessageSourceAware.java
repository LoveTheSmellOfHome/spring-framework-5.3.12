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

package org.springframework.context;

import org.springframework.beans.factory.Aware;

/**
 * Interface to be implemented by any object that wishes to be notified
 * of the MessageSource (typically the ApplicationContext) that it runs in.
 *
 * <p>Note that the MessageSource can usually also be passed on as bean
 * reference (to arbitrary bean properties or constructor arguments), because
 * it is defined as bean with name "messageSource" in the application context.
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @since 1.1.1
 * @see ApplicationContextAware
 */
// 任何希望被通知它运行的 MessageSource（通常是 ApplicationContext）的对象要实现的接口。
// 请注意， MessageSource 通常也可以作为 bean 引用传递（到任意 bean 属性或构造函数参数），
// 因为它在应用程序上下文中被定义为名为“messageSource”的 bean
public interface MessageSourceAware extends Aware {

	/**
	 * Set the MessageSource that this object runs in.
	 * <p>Invoked after population of normal bean properties but before an init
	 * callback like InitializingBean's afterPropertiesSet or a custom init-method.
	 * Invoked before ApplicationContextAware's setApplicationContext.
	 * @param messageSource message source to be used by this object
	 */
	// 设置该对象运行所在的 MessageSource。
	// 在填充普通 bean 属性之后但在初始化回调（如 InitializingBean 的 afterPropertiesSet 或自定义初始化方法）之前调用。
	// 在 ApplicationContextAware 的 setApplicationContext 之前调用。
	void setMessageSource(MessageSource messageSource);

}
