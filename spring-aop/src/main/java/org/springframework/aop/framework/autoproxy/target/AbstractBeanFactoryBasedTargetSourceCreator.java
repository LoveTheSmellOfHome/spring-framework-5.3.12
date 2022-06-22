/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.aop.framework.autoproxy.target;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aop.TargetSource;
import org.springframework.aop.framework.AopInfrastructureBean;
import org.springframework.aop.framework.autoproxy.TargetSourceCreator;
import org.springframework.aop.target.AbstractBeanFactoryBasedTargetSource;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.lang.Nullable;

/**
 * Convenient superclass for
 * {@link org.springframework.aop.framework.autoproxy.TargetSourceCreator}
 * implementations that require creating multiple instances of a prototype bean.
 *
 * <p>Uses an internal BeanFactory to manage the target instances,
 * copying the original bean definition to this internal factory.
 * This is necessary because the original BeanFactory will just
 * contain the proxy instance created through auto-proxying.
 *
 * <p>Requires running in an
 * {@link org.springframework.beans.factory.support.AbstractBeanFactory}.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see org.springframework.aop.target.AbstractBeanFactoryBasedTargetSource
 * @see org.springframework.beans.factory.support.AbstractBeanFactory
 */
// 需要创建原型 bean 的多个实例的 TargetSourceCreator 实现的方便超类。
//
// 使用内部 BeanFactory 管理目标实例，将原始 bean 定义复制到此内部工厂。这是必要的，
// 因为原始 BeanFactory 将只包含通过自动代理创建的代理实例。
//
// 需要在 org.springframework.beans.factory.support.AbstractBeanFactory 中运行。
public abstract class AbstractBeanFactoryBasedTargetSourceCreator
		implements TargetSourceCreator, BeanFactoryAware, DisposableBean {

	protected final Log logger = LogFactory.getLog(getClass());

	private ConfigurableBeanFactory beanFactory;

	/** Internally used DefaultListableBeanFactory instances, keyed by bean name. */
	// 内部使用的 DefaultListableBeanFactory 实例，以 bean 名称为键
	private final Map<String, DefaultListableBeanFactory> internalBeanFactories =
			new HashMap<>();


	@Override
	public final void setBeanFactory(BeanFactory beanFactory) {
		if (!(beanFactory instanceof ConfigurableBeanFactory)) {
			throw new IllegalStateException("Cannot do auto-TargetSource creation with a BeanFactory " +
					"that doesn't implement ConfigurableBeanFactory: " + beanFactory.getClass());
		}
		this.beanFactory = (ConfigurableBeanFactory) beanFactory;
	}

	/**
	 * Return the BeanFactory that this TargetSourceCreators runs in.
	 */
	// 返回运行此 TargetSourceCreators 的 BeanFactory。
	protected final BeanFactory getBeanFactory() {
		return this.beanFactory;
	}


	//---------------------------------------------------------------------
	// Implementation of the TargetSourceCreator interface
	// TargetSourceCreator 接口的实现
	//---------------------------------------------------------------------

	@Override
	@Nullable
	public final TargetSource getTargetSource(Class<?> beanClass, String beanName) {
		// 通过依赖查找获取对象
		AbstractBeanFactoryBasedTargetSource targetSource =
				createBeanFactoryBasedTargetSource(beanClass, beanName);
		if (targetSource == null) {
			return null;
		}

		if (logger.isDebugEnabled()) {
			logger.debug("Configuring AbstractBeanFactoryBasedTargetSource: " + targetSource);
		}

		DefaultListableBeanFactory internalBeanFactory = getInternalBeanFactoryForBean(beanName);

		// We need to override just this bean definition, as it may reference other beans
		// and we're happy to take the parent's definition for those.
		// Always use prototype scope if demanded.
		// 我们只需要覆盖这个 bean 定义，因为它可能引用其他 bean，我们很乐意为这些 bean 采用父级的定义。
		// 如果需要，请始终使用原型范围。
		BeanDefinition bd = this.beanFactory.getMergedBeanDefinition(beanName);
		GenericBeanDefinition bdCopy = new GenericBeanDefinition(bd);
		if (isPrototypeBased()) {
			bdCopy.setScope(BeanDefinition.SCOPE_PROTOTYPE);
		}
		internalBeanFactory.registerBeanDefinition(beanName, bdCopy);

		// Complete configuring the PrototypeTargetSource.
		targetSource.setTargetBeanName(beanName);
		targetSource.setBeanFactory(internalBeanFactory);

		return targetSource;
	}

	/**
	 * Return the internal BeanFactory to be used for the specified bean.
	 * @param beanName the name of the target bean
	 * @return the internal BeanFactory to be used
	 */
	// 返回要用于指定 bean 的内部 BeanFactory。
	// 参形：
	//			beanName – 目标 bean 的名称
	// 返回值：
	//			要使用的内部 BeanFactory
	protected DefaultListableBeanFactory getInternalBeanFactoryForBean(String beanName) {
		synchronized (this.internalBeanFactories) {
			return this.internalBeanFactories.computeIfAbsent(beanName,
					name -> buildInternalBeanFactory(this.beanFactory));
		}
	}

	/**
	 * Build an internal BeanFactory for resolving target beans.
	 * @param containingFactory the containing BeanFactory that originally defines the beans
	 * @return an independent internal BeanFactory to hold copies of some target beans
	 */
	// 构建一个内部 BeanFactory 来解析目标 bean。
	// 参形：
	//			containsFactory – 最初定义 bean 的包含BeanFactory
	// 返回值：
	//			一个独立的内部 BeanFactory 来保存一些目标 bean 的副本
	protected DefaultListableBeanFactory buildInternalBeanFactory(ConfigurableBeanFactory containingFactory) {
		// Set parent so that references (up container hierarchies) are correctly resolved.
		// 设置 parent 以便正确解析引用（向上容器层次结构）。
		DefaultListableBeanFactory internalBeanFactory = new DefaultListableBeanFactory(containingFactory);

		// Required so that all BeanPostProcessors, Scopes, etc become available.
		// 必需，以便所有 BeanPostProcessors、Scopes 等都可用
		internalBeanFactory.copyConfigurationFrom(containingFactory);

		// Filter out BeanPostProcessors that are part of the AOP infrastructure,
		// since those are only meant to apply to beans defined in the original factory.
		// 过滤掉作为 AOP 基础结构一部分的 BeanPostProcessor，因为它们只适用于在原始工厂中定义的 bean。
		internalBeanFactory.getBeanPostProcessors().removeIf(beanPostProcessor ->
				beanPostProcessor instanceof AopInfrastructureBean);

		return internalBeanFactory;
	}

	/**
	 * Destroys the internal BeanFactory on shutdown of the TargetSourceCreator.
	 * @see #getInternalBeanFactoryForBean
	 */
	// 在 TargetSourceCreator 关闭时销毁内部 BeanFactory
	@Override
	public void destroy() {
		synchronized (this.internalBeanFactories) {
			for (DefaultListableBeanFactory bf : this.internalBeanFactories.values()) {
				bf.destroySingletons();
			}
		}
	}


	//---------------------------------------------------------------------
	// Template methods to be implemented by subclasses
	// 子类要实现的模板方法
	//---------------------------------------------------------------------

	/**
	 * Return whether this TargetSourceCreator is prototype-based.
	 * The scope of the target bean definition will be set accordingly.
	 * <p>Default is "true".
	 * @see org.springframework.beans.factory.config.BeanDefinition#isSingleton()
	 */
	// 返回此 TargetSourceCreator 是否基于原型。目标 bean 定义的范围将相应设置。
	// 默认为“真”。
	protected boolean isPrototypeBased() {
		return true;
	}

	/**
	 * Subclasses must implement this method to return a new AbstractPrototypeBasedTargetSource
	 * if they wish to create a custom TargetSource for this bean, or {@code null} if they are
	 * not interested it in, in which case no special target source will be created.
	 * Subclasses should not call {@code setTargetBeanName} or {@code setBeanFactory}
	 * on the AbstractPrototypeBasedTargetSource: This class' implementation of
	 * {@code getTargetSource()} will do that.
	 * @param beanClass the class of the bean to create a TargetSource for
	 * @param beanName the name of the bean
	 * @return the AbstractPrototypeBasedTargetSource, or {@code null} if we don't match this
	 */
	// 如果子类希望为此 bean 创建自定义 TargetSource，则必须实现此方法以返回新的 AbstractPrototypeBasedTargetSource，
	// 如果不感兴趣，则返回null ，在这种情况下，不会创建特殊的目标源。子类不应在 AbstractPrototypeBasedTargetSource
	// 上调用 setTargetBeanName或setBeanFactory ：此类的 getTargetSource() 实现会这样做。
	// 参形：
	//			beanClass – 要为其创建 TargetSource 的 bean 的类
	//			beanName – bean 的名称
	// 返回值：
	//			AbstractPrototypeBasedTargetSource，如果我们不匹配，则null
	@Nullable
	protected abstract AbstractBeanFactoryBasedTargetSource createBeanFactoryBasedTargetSource(
			Class<?> beanClass, String beanName);

}
