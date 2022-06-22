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

package org.springframework.aop.target;

import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;

/**
 * Base class for dynamic {@link org.springframework.aop.TargetSource} implementations
 * that create new prototype bean instances to support a pooling or
 * new-instance-per-invocation strategy.
 *
 * <p>Such TargetSources must run in a {@link BeanFactory}, as it needs to
 * call the {@code getBean} method to create a new prototype instance.
 * Therefore, this base class extends {@link AbstractBeanFactoryBasedTargetSource}.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see org.springframework.beans.factory.BeanFactory#getBean
 * @see PrototypeTargetSource
 * @see ThreadLocalTargetSource
 * @see CommonsPool2TargetSource
 */
// 动态 org.springframework.aop.TargetSource 实现的基类，它创建新的原型 bean 实例以支持池或每次调用新实例的策略。
//
// 这样的 TargetSource 必须在 BeanFactory 中运行，因为它需要调用 getBean 方法来创建新的原型实例。 因此，这个基类
// 扩展了AbstractBeanFactoryBasedTargetSource 。
@SuppressWarnings("serial")
public abstract class AbstractPrototypeBasedTargetSource extends AbstractBeanFactoryBasedTargetSource {

	// 生命周期在前
	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		// 设置 IoC 容器
		super.setBeanFactory(beanFactory);

		// Check whether the target bean is defined as prototype.
		// 检查目标 bean 是否定义为原型，如果不是就抛异常，要求 bean 类型是原型
		if (!beanFactory.isPrototype(getTargetBeanName())) {
			throw new BeanDefinitionStoreException(
					"Cannot use prototype-based TargetSource against non-prototype bean with name '" +
					getTargetBeanName() + "': instances would not be independent");
		}
	}

	/**
	 * Subclasses should call this method to create a new prototype instance.
	 * @throws BeansException if bean creation failed
	 */
	// 子类应该调用这个方法来创建一个新的原型实例。
	// 抛出：BeansException – 如果 bean 创建失败
	// 生命周期在后
	protected Object newPrototypeInstance() throws BeansException {
		if (logger.isDebugEnabled()) {
			logger.debug("Creating new instance of bean '" + getTargetBeanName() + "'");
		}
		// 通过依赖查找获取对应的 Bean 对象，如果 bean 的 scope 是原型，那么每次依赖查找的时候都会创建一个新的实例对象，
		// 那么这个新的实例和 IoC 容器是脱钩的，也就是说 IoC 容器对于 Prototype 这个作用域，只关注
		// 它的 BeanDefinition,最终这个对象是个游离对象。或者说是托管对象
		// 深克隆：通过依赖查找创建原型 bean,复制品会和 Spring IoC 脱钩。后面的释放需要自己取管理。
		return getBeanFactory().getBean(getTargetBeanName());
	}

	/**
	 * Subclasses should call this method to destroy an obsolete prototype instance.
	 * @param target the bean instance to destroy
	 */
	// 子类应该调用这个方法来销毁一个过时的原型实例。
	// 参形：target -- 要销毁的 bean 实例
	protected void destroyPrototypeInstance(Object target) {
		if (logger.isDebugEnabled()) {
			logger.debug("Destroying instance of bean '" + getTargetBeanName() + "'");
		}
		if (getBeanFactory() instanceof ConfigurableBeanFactory) {
			((ConfigurableBeanFactory) getBeanFactory()).destroyBean(getTargetBeanName(), target);
		}
		else if (target instanceof DisposableBean) {
			try {
				((DisposableBean) target).destroy();
			}
			catch (Throwable ex) {
				logger.warn("Destroy method on bean with name '" + getTargetBeanName() + "' threw an exception", ex);
			}
		}
	}


	//---------------------------------------------------------------------
	// Serialization support
	//---------------------------------------------------------------------

	private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
		throw new NotSerializableException("A prototype-based TargetSource itself is not deserializable - " +
				"just a disconnected SingletonTargetSource or EmptyTargetSource is");
	}

	/**
	 * Replaces this object with a SingletonTargetSource on serialization.
	 * Protected as otherwise it won't be invoked for subclasses.
	 * (The {@code writeReplace()} method must be visible to the class
	 * being serialized.)
	 * <p>With this implementation of this method, there is no need to mark
	 * non-serializable fields in this class or subclasses as transient.
	 */
	// 在序列化时用 SingletonTargetSource 替换此对象。 受到保护，否则它不会被子类调用。
	//（writeReplace()方法必须对被序列化的类可见。）
	//
	// 使用此方法的此实现，无需将此类或子类中的不可序列化字段标记为瞬态。
	protected Object writeReplace() throws ObjectStreamException {
		if (logger.isDebugEnabled()) {
			logger.debug("Disconnecting TargetSource [" + this + "]");
		}
		try {
			// Create disconnected SingletonTargetSource/EmptyTargetSource.
			// 创建断开连接的 SingletonTargetSource/EmptyTargetSource
			Object target = getTarget();
			return (target != null ? new SingletonTargetSource(target) :
					EmptyTargetSource.forClass(getTargetClass()));
		}
		catch (Exception ex) {
			String msg = "Cannot get target for disconnecting TargetSource [" + this + "]";
			logger.error(msg, ex);
			throw new NotSerializableException(msg + ": " + ex);
		}
	}

}
