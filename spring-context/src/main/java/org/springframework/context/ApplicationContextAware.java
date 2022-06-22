/*
 * Copyright 2002-2012 the original author or authors.
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

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.Aware;

/**
 * Interface to be implemented by any object that wishes to be notified
 * of the {@link ApplicationContext} that it runs in.
 *
 * <p>Implementing this interface makes sense for example when an object
 * requires access to a set of collaborating beans. Note that configuration
 * via bean references is preferable to implementing this interface just
 * for bean lookup purposes.
 *
 * <p>This interface can also be implemented if an object needs access to file
 * resources, i.e. wants to call {@code getResource}, wants to publish
 * an application event, or requires access to the MessageSource. However,
 * it is preferable to implement the more specific {@link ResourceLoaderAware},
 * {@link ApplicationEventPublisherAware} or {@link MessageSourceAware} interface
 * in such a specific scenario.
 *
 * <p>Note that file resource dependencies can also be exposed as bean properties
 * of type {@link org.springframework.core.io.Resource}, populated via Strings
 * with automatic type conversion by the bean factory. This removes the need
 * for implementing any callback interface just for the purpose of accessing
 * a specific file resource.
 *
 * <p>{@link org.springframework.context.support.ApplicationObjectSupport} is a
 * convenience base class for application objects, implementing this interface.
 *
 * <p>For a list of all bean lifecycle methods, see the
 * {@link org.springframework.beans.factory.BeanFactory BeanFactory javadocs}.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Chris Beams
 * @see ResourceLoaderAware
 * @see ApplicationEventPublisherAware
 * @see MessageSourceAware
 * @see org.springframework.context.support.ApplicationObjectSupport
 * @see org.springframework.beans.factory.BeanFactoryAware
 */
// 任何希望被 {@link ApplicationContext} 运行时通知的对象要实现的接口
//
// <p>实现这个接口是有意义的，例如当一个对象需要访问一组协作 bean 时。
// 请注意，通过 bean 引用进行配置比仅出于 bean 查找目的实现此接口更可取。
//
// <p>如果一个对象需要访问文件资源，即想要调用{@code getResource}，想要发布一个应用程序事件，或者需要访问MessageSource，
// 也可以实现这个接口。但是，在这种特定场景中，最好实现更具体的 {@link ResourceLoaderAware}、
// {@link ApplicationEventPublisherAware}或 {@link MessageSourceAware} 接口。
//
// <p>请注意，文件资源依赖项也可以作为 {@link org.springframework.core.io.Resource} 类型的 bean 属性公开，
// 通过由 bean 工厂自动类型转换的字符串填充。这消除了为了访问特定文件资源而实现任何回调接口的需要。
//
// <p>{@link org.springframework.context.support.ApplicationObjectSupport} 是应用程序对象的一个方便的基类，实现了这个接口。
// <p>有关所有 bean 生命周期方法的列表，请参阅 {@link org.springframework.beans.factory.BeanFactory BeanFactory javadocs}。
public interface ApplicationContextAware extends Aware {

	/**
	 * Set the ApplicationContext that this object runs in.
	 * Normally this call will be used to initialize the object.
	 * <p>Invoked after population of normal bean properties but before an init callback such
	 * as {@link org.springframework.beans.factory.InitializingBean#afterPropertiesSet()}
	 * or a custom init-method. Invoked after {@link ResourceLoaderAware#setResourceLoader},
	 * {@link ApplicationEventPublisherAware#setApplicationEventPublisher} and
	 * {@link MessageSourceAware}, if applicable.
	 * @param applicationContext the ApplicationContext object to be used by this object
	 * @throws ApplicationContextException in case of context initialization errors
	 * @throws BeansException if thrown by application context methods
	 * @see org.springframework.beans.factory.BeanInitializationException
	 */
	// 设置此对象运行所在的 ApplicationContext。通常此调用将用于初始化对象。
	// <p>在填充普通 bean 属性之后但在初始化回调之前调用，例如 {@link org.springframework.beans.factory.
	// InitializingBeanafterPropertiesSet()} 或自定义初始化方法。在 {@link ResourceLoaderAwaresetResourceLoader}、
	// {@link ApplicationEventPublisherAwaresetApplicationEventPublisher} 和 {@link MessageSourceAware}（如果适用）之后调用。
	void setApplicationContext(ApplicationContext applicationContext) throws BeansException;

}
