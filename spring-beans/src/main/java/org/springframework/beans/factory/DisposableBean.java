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
 * Interface to be implemented by beans that want to release resources on destruction.
 * A {@link BeanFactory} will invoke the destroy method on individual destruction of a
 * scoped bean. An {@link org.springframework.context.ApplicationContext} is supposed
 * to dispose all of its singletons on shutdown, driven by the application lifecycle.
 *
 * <p>A Spring-managed bean may also implement Java's {@link AutoCloseable} interface
 * for the same purpose. An alternative to implementing an interface is specifying a
 * custom destroy method, for example in an XML bean definition. For a list of all
 * bean lifecycle methods, see the {@link BeanFactory BeanFactory javadocs}.
 *
 * @author Juergen Hoeller
 * @since 12.08.2003
 * @see InitializingBean
 * @see org.springframework.beans.factory.support.RootBeanDefinition#getDestroyMethodName()
 * @see org.springframework.beans.factory.config.ConfigurableBeanFactory#destroySingletons()
 * @see org.springframework.context.ConfigurableApplicationContext#close()
 */
// 由想要在销毁时释放资源的 bean 实现的接口。 {@link BeanFactory} 将在单独销毁作用域 bean 时调用 destroy 方法。
// {@link org.springframework.context.ApplicationContext} 应该在关闭时处理其所有单例，由应用程序生命周期驱动
//
// <p>出于相同目的，Spring 管理的 bean 也可以实现 Java 的 {@link AutoCloseable} 接口。
// 实现接口的另一种方法是指定自定义销毁方法，例如在 XML bean 定义中。有关所有 bean 生命周期方法的列表，
// 请参阅 {@link BeanFactory BeanFactory javadocs}。
//
// 并不是所有的 Bean 都实现了这个接口
public interface DisposableBean {

	/**
	 * Invoked by the containing {@code BeanFactory} on destruction of a bean.
	 * @throws Exception in case of shutdown errors. Exceptions will get logged
	 * but not rethrown to allow other beans to release their resources as well.
	 */
	// 在销毁 bean 时由包含 {@code BeanFactory} 调用。
	// @throws 异常在关闭错误的情况下。异常将被记录但不会重新抛出以允许其他 bean 也释放它们的资源
	void destroy() throws Exception;

}
