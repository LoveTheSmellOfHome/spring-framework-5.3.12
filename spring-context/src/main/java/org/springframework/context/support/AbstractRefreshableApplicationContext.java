/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.context.support;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextException;
import org.springframework.lang.Nullable;

import java.io.IOException;

/**
 * Base class for {@link org.springframework.context.ApplicationContext}
 * implementations which are supposed to support multiple calls to {@link #refresh()},
 * creating a new internal bean factory instance every time.
 * Typically (but not necessarily), such a context will be driven by
 * a set of config locations to load bean definitions from.
 *
 * <p>The only method to be implemented by subclasses is {@link #loadBeanDefinitions},
 * which gets invoked on each refresh. A concrete implementation is supposed to load
 * bean definitions into the given
 * {@link org.springframework.beans.factory.support.DefaultListableBeanFactory},
 * typically delegating to one or more specific bean definition readers.
 *
 * <p><b>Note that there is a similar base class for WebApplicationContexts.</b>
 * {@link org.springframework.web.context.support.AbstractRefreshableWebApplicationContext}
 * provides the same subclassing strategy, but additionally pre-implements
 * all context functionality for web environments. There is also a
 * pre-defined way to receive config locations for a web context.
 *
 * <p>Concrete standalone subclasses of this base class, reading in a
 * specific bean definition format, are {@link ClassPathXmlApplicationContext}
 * and {@link FileSystemXmlApplicationContext}, which both derive from the
 * common {@link AbstractXmlApplicationContext} base class;
 * {@link org.springframework.context.annotation.AnnotationConfigApplicationContext}
 * supports {@code @Configuration}-annotated classes as a source of bean definitions.
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @since 1.1.3
 * @see #loadBeanDefinitions
 * @see org.springframework.beans.factory.support.DefaultListableBeanFactory
 * @see org.springframework.web.context.support.AbstractRefreshableWebApplicationContext
 * @see AbstractXmlApplicationContext
 * @see ClassPathXmlApplicationContext
 * @see FileSystemXmlApplicationContext
 * @see org.springframework.context.annotation.AnnotationConfigApplicationContext
 */
// ApplicationContext实现的基类，它应该支持多次调用 refresh() ，每次都创建一个新的内部 bean 工厂实例。
// 通常（但不一定），这样的上下文将由一组配置位置驱动，以从中加载 bean 定义。
//
// 子类实现的唯一方法是loadBeanDefinitions ，它在每次刷新时被调用。 具体实现应该将 bean 定义加载到
// 给定的DefaultListableBeanFactory ，通常委托给一个或多个特定的 bean 定义读取器。
//
// 请注意，WebApplicationContexts 有一个类似的基类。
// org.springframework.web.context.support.AbstractRefreshableWebApplicationContext
// 提供相同的子类化策略，但另外预实现了 Web 环境的所有上下文功能。 还有一种预定义的方式来接收 Web 上下文的配置位置。
//
// 这个基类的具体独立子类，以特定的 bean 定义格式读取，是 ClassPathXmlApplicationContext
// 和 FileSystemXmlApplicationContext ，它们都派生自通用AbstractXmlApplicationContext基类；
// org.springframework.context.annotation.AnnotationConfigApplicationContext
// 支持 @Configuration -annotated 类作为 bean 定义的来源。
public abstract class AbstractRefreshableApplicationContext extends AbstractApplicationContext {

	// 是否允许 BeanDefinition 重写
	@Nullable
	private Boolean allowBeanDefinitionOverriding;

	// 是否允许循环引用
	@Nullable
	private Boolean allowCircularReferences;

	/** Bean factory for this context. */
	// 此上下文的 Bean 工厂
	@Nullable
	private volatile DefaultListableBeanFactory beanFactory;


	/**
	 * Create a new AbstractRefreshableApplicationContext with no parent.
	 */
	// 创建一个没有父级的新 AbstractRefreshableApplicationContext。
	public AbstractRefreshableApplicationContext() {
	}

	/**
	 * Create a new AbstractRefreshableApplicationContext with the given parent context.
	 * @param parent the parent context
	 */
	// 使用给定的父上下文创建一个新的 AbstractRefreshableApplicationContext。
	// 参形：parent – 父上下文
	public AbstractRefreshableApplicationContext(@Nullable ApplicationContext parent) {
		super(parent);
	}


	/**
	 * Set whether it should be allowed to override bean definitions by registering
	 * a different definition with the same name, automatically replacing the former.
	 * If not, an exception will be thrown. Default is "true".
	 * @see org.springframework.beans.factory.support.DefaultListableBeanFactory#setAllowBeanDefinitionOverriding
	 */
	// 设置是否应允许通过注册具有相同名称的不同定义来覆盖 bean 定义，自动替换前者。 如果没有，将抛出异常。 默认为“真”。
	public void setAllowBeanDefinitionOverriding(boolean allowBeanDefinitionOverriding) {
		this.allowBeanDefinitionOverriding = allowBeanDefinitionOverriding;
	}

	/**
	 * Set whether to allow circular references between beans - and automatically
	 * try to resolve them.
	 * <p>Default is "true". Turn this off to throw an exception when encountering
	 * a circular reference, disallowing them completely.
	 * @see org.springframework.beans.factory.support.DefaultListableBeanFactory#setAllowCircularReferences
	 */
	// 设置是否允许 bean 之间的循环引用 - 并自动尝试解决它们。
	// 默认为“真”。 在遇到循环引用时将其关闭以引发异常，完全禁止它们。
	public void setAllowCircularReferences(boolean allowCircularReferences) {
		this.allowCircularReferences = allowCircularReferences;
	}


	/**
	 * This implementation performs an actual refresh of this context's underlying
	 * bean factory, shutting down the previous bean factory (if any) and
	 * initializing a fresh bean factory for the next phase of the context's lifecycle.
	 */
	// 此实现执行此上下文的底层 bean 工厂的实际刷新，
	// 关闭先前的 bean 工厂（如果有）并为上下文生命周期的下一个阶段初始化一个新的 bean 工厂。
	@Override
	protected final void refreshBeanFactory() throws BeansException {
		// a.如果已关联了 BeanFactory,销毁 beans,关闭 BeanFactory
		if (hasBeanFactory()) {
			destroyBeans();
			closeBeanFactory();
		}
		try {
			// b.创建 BeanFactory - createBeanFactory()
			DefaultListableBeanFactory beanFactory = createBeanFactory();
			// c.设置 BeanFactory Id,每个 BeanFactory 都有唯一的 id,
			// 在 Sping Boot 和 Spring Cloud 这个 id 是有语义的
			beanFactory.setSerializationId(getId());
			// d.设置自定义工厂行为
			customizeBeanFactory(beanFactory);
			// e.加载 bean 定义
			loadBeanDefinitions(beanFactory);
			// f.关联新建 BeanFactory(局部) 到 Spring 应用上下文(整体)
			this.beanFactory = beanFactory;
		}
		catch (IOException ex) {
			throw new ApplicationContextException("I/O error parsing bean definition source for " + getDisplayName(), ex);
		}
	}

	@Override
	protected void cancelRefresh(BeansException ex) {
		DefaultListableBeanFactory beanFactory = this.beanFactory;
		if (beanFactory != null) {
			beanFactory.setSerializationId(null);
		}
		super.cancelRefresh(ex);
	}

	@Override
	protected final void closeBeanFactory() {
		DefaultListableBeanFactory beanFactory = this.beanFactory;
		if (beanFactory != null) {
			beanFactory.setSerializationId(null);
			this.beanFactory = null;
		}
	}

	/**
	 * Determine whether this context currently holds a bean factory,
	 * i.e. has been refreshed at least once and not been closed yet.
	 */
	// 判断这个上下文当前是否持有一个bean factory，即至少已经刷新了一次并且还没有关闭。
	protected final boolean hasBeanFactory() {
		return (this.beanFactory != null);
	}

	@Override
	public final ConfigurableListableBeanFactory getBeanFactory() {
		DefaultListableBeanFactory beanFactory = this.beanFactory;
		if (beanFactory == null) {
			throw new IllegalStateException("BeanFactory not initialized or already closed - " +
					"call 'refresh' before accessing beans via the ApplicationContext");
		}
		return beanFactory;
	}

	/**
	 * Overridden to turn it into a no-op: With AbstractRefreshableApplicationContext,
	 * {@link #getBeanFactory()} serves a strong assertion for an active context anyway.
	 */
	// 重写以将其变为无操作：使用 AbstractRefreshableApplicationContext，
	// getBeanFactory()无论如何都会为活动上下文提供强大的断言。
	@Override
	protected void assertBeanFactoryActive() {
	}

	/**
	 * Create an internal bean factory for this context.
	 * Called for each {@link #refresh()} attempt.
	 * <p>The default implementation creates a
	 * {@link org.springframework.beans.factory.support.DefaultListableBeanFactory}
	 * with the {@linkplain #getInternalParentBeanFactory() internal bean factory} of this
	 * context's parent as parent bean factory. Can be overridden in subclasses,
	 * for example to customize DefaultListableBeanFactory's settings.
	 * @return the bean factory for this context
	 * @see org.springframework.beans.factory.support.DefaultListableBeanFactory#setAllowBeanDefinitionOverriding
	 * @see org.springframework.beans.factory.support.DefaultListableBeanFactory#setAllowEagerClassLoading
	 * @see org.springframework.beans.factory.support.DefaultListableBeanFactory#setAllowCircularReferences
	 * @see org.springframework.beans.factory.support.DefaultListableBeanFactory#setAllowRawInjectionDespiteWrapping
	 */
	// 为此上下文创建一个内部 bean 工厂。 为每次refresh()尝试调用。
	// 默认实现创建一个DefaultListableBeanFactory ，并将此上下文父级的内部 bean 工厂作为父 bean 工厂。
	// 可以在子类中重写，例如自定义 DefaultListableBeanFactory 的设置。
	// 返回值：
	//			此上下文的 bean 工厂
	protected DefaultListableBeanFactory createBeanFactory() {
		return new DefaultListableBeanFactory(getInternalParentBeanFactory());
	}

	/**
	 * Customize the internal bean factory used by this context.
	 * Called for each {@link #refresh()} attempt.
	 * <p>The default implementation applies this context's
	 * {@linkplain #setAllowBeanDefinitionOverriding "allowBeanDefinitionOverriding"}
	 * and {@linkplain #setAllowCircularReferences "allowCircularReferences"} settings,
	 * if specified. Can be overridden in subclasses to customize any of
	 * {@link DefaultListableBeanFactory}'s settings.
	 * @param beanFactory the newly created bean factory for this context
	 * @see DefaultListableBeanFactory#setAllowBeanDefinitionOverriding
	 * @see DefaultListableBeanFactory#setAllowCircularReferences
	 * @see DefaultListableBeanFactory#setAllowRawInjectionDespiteWrapping
	 * @see DefaultListableBeanFactory#setAllowEagerClassLoading
	 */
	// 自定义此上下文使用的内部 bean 工厂。 为每次{@link #refresh()}尝试调用。
	//
	// 如果指定，默认实现应用此上下文的 {@linkplain #setAllowBeanDefinitionOverriding "allowBeanDefinitionOverriding"}
	// 和 {@linkplain #setAllowCircularReferences "allowCircularReferences"} 设置。
	// 可以在子类中覆盖以自定义 {@link DefaultListableBeanFactory} 的任何设置。
	//
	// @param beanFactory – 为此上下文新创建的 bean 工长
	protected void customizeBeanFactory(DefaultListableBeanFactory beanFactory) {
		if (this.allowBeanDefinitionOverriding != null) {
			// 是否允许定义重复的 BeanDefinition, Spring Framework 中默认为 true, Spring Boot 2.1 默认为false,
			// 不允许重复定义 BeanDefinition,所以导致有些程序会失败。
			beanFactory.setAllowBeanDefinitionOverriding(this.allowBeanDefinitionOverriding);
		}
		if (this.allowCircularReferences != null) {
			// 设置是否允许循环依赖,Spring 框架默认是 true
			// 循环依赖的好处：兼容性更强
			// 循环依赖的坏处：性能更差，把允许重复定义和允许循环引用(依赖) 关掉后，让程序得到更好的性能。
			beanFactory.setAllowCircularReferences(this.allowCircularReferences);
		}
	}

	/**
	 * Load bean definitions into the given bean factory, typically through
	 * delegating to one or more bean definition readers.
	 * @param beanFactory the bean factory to load bean definitions into
	 * @throws BeansException if parsing of the bean definitions failed
	 * @throws IOException if loading of bean definition files failed
	 * @see org.springframework.beans.factory.support.PropertiesBeanDefinitionReader
	 * @see org.springframework.beans.factory.xml.XmlBeanDefinitionReader
	 */
	// 加载 BeanDefinitions (加载 Bean 定义)
	// XmlApplicationContext(XML 配置属性方式) 和 AnnotationConfigApplicationContext(注解驱动)的区别
	// 主要区别就在加载 loadBeanDefinitions,
	protected abstract void loadBeanDefinitions(DefaultListableBeanFactory beanFactory)
			throws BeansException, IOException;

}
