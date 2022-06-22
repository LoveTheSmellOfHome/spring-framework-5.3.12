/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.beans.factory.config;

import org.springframework.beans.BeansException;
import org.springframework.lang.Nullable;

/**
 * Factory hook that allows for custom modification of new bean instances &mdash;
 * for example, checking for marker interfaces or wrapping beans with proxies.
 *
 * <p>Typically, post-processors that populate beans via marker interfaces
 * or the like will implement {@link #postProcessBeforeInitialization},
 * while post-processors that wrap beans with proxies will normally
 * implement {@link #postProcessAfterInitialization}.
 *
 * <h3>Registration</h3>
 * <p>An {@code ApplicationContext} can autodetect {@code BeanPostProcessor} beans
 * in its bean definitions and apply those post-processors to any beans subsequently
 * created. A plain {@code BeanFactory} allows for programmatic registration of
 * post-processors, applying them to all beans created through the bean factory.
 *
 * <h3>Ordering</h3>
 * <p>{@code BeanPostProcessor} beans that are autodetected in an
 * {@code ApplicationContext} will be ordered according to
 * {@link org.springframework.core.PriorityOrdered} and
 * {@link org.springframework.core.Ordered} semantics. In contrast,
 * {@code BeanPostProcessor} beans that are registered programmatically with a
 * {@code BeanFactory} will be applied in the order of registration; any ordering
 * semantics expressed through implementing the
 * {@code PriorityOrdered} or {@code Ordered} interface will be ignored for
 * programmatically registered post-processors. Furthermore, the
 * {@link org.springframework.core.annotation.Order @Order} annotation is not
 * taken into account for {@code BeanPostProcessor} beans.
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 10.10.2003
 * @see InstantiationAwareBeanPostProcessor
 * @see DestructionAwareBeanPostProcessor
 * @see ConfigurableBeanFactory#addBeanPostProcessor
 * @see BeanFactoryPostProcessor
 */
// 允许自定义修改新 bean 实例的工厂钩子(回调方法)——例如，检查标记接口或用代理包装 bean
//
//	<p>通常，通过标记接口等填充 bean 的后处理器将实现 {@link postProcessBeforeInitialization}，
//	而使用代理包装 bean 的后处理器通常会实现 {@link postProcessAfterInitialization}。
//
// <h3>Registration<h3> <p>{@code ApplicationContext} 可以在其 bean 定义中自动检测 {@code BeanPostProcessor} bean，
// 并将这些后处理器应用于随后创建的任何 bean。一个普通的 {@code BeanFactory} 允许以编程方式注册后处理器，
// 将它们应用于通过 bean 工厂创建的所有 bean。
//
// <h3>Ordering<h3> <p>{@code BeanPostProcessor} 在 {@code ApplicationContext} 中自动检测的 bean 将根据
// {@link org.springframework.core.PriorityOrdered} 和 {@link org.springframework. core.Ordered} 语义。
// 相比之下，以编程方式注册到 {@code BeanFactory} 的 {@code BeanPostProcessor} bean 将按注册顺序应用；
// 对于以编程方式注册的后处理器，通过实现 {@code PriorityOrdered} 或 {@code Ordered} 接口表达的任何排序语义都将被忽略。
// 此外，{@code BeanPostProcessor} bean 不考虑 {@link org.springframework.core.annotation.Order @Order} 注解。
public interface BeanPostProcessor {

	/**
	 * Apply this {@code BeanPostProcessor} to the given new bean instance <i>before</i> any bean
	 * initialization callbacks (like InitializingBean's {@code afterPropertiesSet}
	 * or a custom init-method). The bean will already be populated with property values.
	 * The returned bean instance may be a wrapper around the original.
	 * <p>The default implementation returns the given {@code bean} as-is.
	 * @param bean the new bean instance
	 * @param beanName the name of the bean
	 * @return the bean instance to use, either the original or a wrapped one;
	 * if {@code null}, no subsequent BeanPostProcessors will be invoked
	 * @throws org.springframework.beans.BeansException in case of errors
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet
	 */
	// 将此 {@code BeanPostProcessor} 应用于给定的新 bean 实例 <i>before<i> 任何 bean 初始化回调
	// （如 InitializingBean 的{@code afterPropertiesSet} 或自定义初始化 init-method 方法）。
	// bean 将已填充有属性值。返回的 bean 实例可能是原始实例的包装器。
	// <p>默认实现按原样返回给定的 {@code bean}
	//
	// return 要使用的 bean 实例，可以是原始实例，也可以是包装好的实例；
	// 如果 {@code null}，则不会调用后续的 BeanPostProcessors
	// Bean 的初始化前阶段的接口回调
	@Nullable
	default Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}

	/**
	 * Apply this {@code BeanPostProcessor} to the given new bean instance <i>after</i> any bean
	 * initialization callbacks (like InitializingBean's {@code afterPropertiesSet}
	 * or a custom init-method). The bean will already be populated with property values.
	 * The returned bean instance may be a wrapper around the original.
	 * <p>In case of a FactoryBean, this callback will be invoked for both the FactoryBean
	 * instance and the objects created by the FactoryBean (as of Spring 2.0). The
	 * post-processor can decide whether to apply to either the FactoryBean or created
	 * objects or both through corresponding {@code bean instanceof FactoryBean} checks.
	 * <p>This callback will also be invoked after a short-circuiting triggered by a
	 * {@link InstantiationAwareBeanPostProcessor#postProcessBeforeInstantiation} method,
	 * in contrast to all other {@code BeanPostProcessor} callbacks.
	 * <p>The default implementation returns the given {@code bean} as-is.
	 * @param bean the new bean instance
	 * @param beanName the name of the bean
	 * @return the bean instance to use, either the original or a wrapped one;
	 * if {@code null}, no subsequent BeanPostProcessors will be invoked
	 * @throws org.springframework.beans.BeansException in case of errors
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet
	 * @see org.springframework.beans.factory.FactoryBean
	 */
	// 将此 {@code BeanPostProcessor} 应用于给定的新 bean 实例 <i>after<i>
	// 任何 bean 初始化回调（如 InitializingBean 的 {@code afterPropertiesSet} 或自定义初始化方法）。
	// bean 将已填充有属性值。返回的 bean 实例可能是原始实例的包装器。
	//
	// 在 FactoryBean 的情况下，将为 FactoryBean 实例和由 FactoryBean 创建的对象（从 Spring 2.0 开始）调用此回调。
	// 后处理器可以通过相应的 {@code bean instanceof FactoryBean} 检查来决定是应用于 FactoryBean 或创建的对象还是两者。
	//
	// 与所有其他 {@code BeanPostProcessor} 回调相比，此回调也将在由
	// {@link InstantiationAwareBeanPostProcessor.postProcessBeforeInstantiation} 方法触发的短路后调用。
	//
	// 默认实现按原样返回给定的 {@code bean}。
	// bean 初始化后阶段回调接口
	@Nullable
	default Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}

}
