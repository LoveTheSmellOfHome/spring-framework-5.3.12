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

package org.springframework.beans.factory.config;

import java.lang.reflect.Constructor;

import org.springframework.beans.BeansException;
import org.springframework.lang.Nullable;

/**
 * Extension of the {@link InstantiationAwareBeanPostProcessor} interface,
 * adding a callback for predicting the eventual type of a processed bean.
 *
 * <p><b>NOTE:</b> This interface is a special purpose interface, mainly for
 * internal use within the framework. In general, application-provided
 * post-processors should simply implement the plain {@link BeanPostProcessor}
 * interface or derive from the {@link InstantiationAwareBeanPostProcessorAdapter}
 * class. New methods might be added to this interface even in point releases.
 *
 * @author Juergen Hoeller
 * @since 2.0.3
 * @see InstantiationAwareBeanPostProcessorAdapter
 */
// {@link InstantiationAwareBeanPostProcessor} 接口的扩展，添加了用于预测已处理 bean 最终类型的回调。
//
//	p><b>注意：<b>这个接口是一个特殊用途的接口，主要供框架内部使用。通常，应用程序提供的后处理器应该简单地实现普通的
//	{@link BeanPostProcessor} 接口或派生自 {@link InstantiationAwareBeanPostProcessorAdapter} 类。
//	即使在小版本发布中，新方法也可能添加到此接口中
public interface SmartInstantiationAwareBeanPostProcessor extends InstantiationAwareBeanPostProcessor {

	/**
	 * Predict the type of the bean to be eventually returned from this
	 * processor's {@link #postProcessBeforeInstantiation} callback.
	 * <p>The default implementation returns {@code null}.
	 * @param beanClass the raw class of the bean
	 * @param beanName the name of the bean
	 * @return the type of the bean, or {@code null} if not predictable
	 * @throws org.springframework.beans.BeansException in case of errors
	 */
	// 预测最终从此处理器的 {@link postProcessBeforeInstantiation} 回调返回的 bean 的类型。
	// <p>默认实现返回 {@code null}。
	@Nullable
	default Class<?> predictBeanType(Class<?> beanClass, String beanName) throws BeansException {
		return null;
	}

	/**
	 * Determine the candidate constructors to use for the given bean.
	 * <p>The default implementation returns {@code null}.
	 * @param beanClass the raw class of the bean (never {@code null})
	 * @param beanName the name of the bean
	 * @return the candidate constructors, or {@code null} if none specified
	 * @throws org.springframework.beans.BeansException in case of errors
	 */
	// 在 bean 中提供拦截机制，确定用于给定 bean 的候选构造函数。
	// <p>默认实现返回 {@code null}。 @param beanClass bean 的原始类（从不{@code null}）
	// @param beanName bean 的名称
	// @return 候选构造函数，或者 {@code null} 如果没有指定
	// @throws org.springframework.beans.BeansException 以防止错误情况
	@Nullable
	default Constructor<?>[] determineCandidateConstructors(Class<?> beanClass, String beanName)
			throws BeansException {

		return null;
	}

	/**
	 * Obtain a reference for early access to the specified bean,
	 * typically for the purpose of resolving a circular reference.
	 * <p>This callback gives post-processors a chance to expose a wrapper
	 * early - that is, before the target bean instance is fully initialized.
	 * The exposed object should be equivalent to the what
	 * {@link #postProcessBeforeInitialization} / {@link #postProcessAfterInitialization}
	 * would expose otherwise. Note that the object returned by this method will
	 * be used as bean reference unless the post-processor returns a different
	 * wrapper from said post-process callbacks. In other words: Those post-process
	 * callbacks may either eventually expose the same reference or alternatively
	 * return the raw bean instance from those subsequent callbacks (if the wrapper
	 * for the affected bean has been built for a call to this method already,
	 * it will be exposes as final bean reference by default).
	 * <p>The default implementation returns the given {@code bean} as-is.
	 * @param bean the raw bean instance
	 * @param beanName the name of the bean
	 * @return the object to expose as bean reference
	 * (typically with the passed-in bean instance as default)
	 * @throws org.springframework.beans.BeansException in case of errors
	 */
	// 获取早期访问指定 bean 的引用，通常用于解析循环引用。
	// <p>此回调使后处理器有机会尽早暴露包装器 - 即在目标 bean 实例完全初始化之前。
	// 公开的对象应该等同于 {@link postProcessBeforeInitialization} {@link postProcessAfterInitialization} 否则会公开的内容。
	// 请注意，此方法返回的对象将用作 bean 引用，除非后处理器从所述后处理回调返回不同的包装器。
	// 换句话说：这些后处理回调可能最终公开相同的引用，或者从这些后续回调中返回原始 bean 实例（如果已经构建了受影响 bean 的包装器来调用此方法，
	// 则它将公开默认情况下作为最终的 bean 引用）。
	// <p>默认实现按原样返回给定的 {@code bean}。
	default Object getEarlyBeanReference(Object bean, String beanName) throws BeansException {
		return bean;
	}

}
