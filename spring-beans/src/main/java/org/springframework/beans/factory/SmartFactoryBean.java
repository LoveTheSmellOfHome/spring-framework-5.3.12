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
 * Extension of the {@link FactoryBean} interface. Implementations may
 * indicate whether they always return independent instances, for the
 * case where their {@link #isSingleton()} implementation returning
 * {@code false} does not clearly indicate independent instances.
 *
 * <p>Plain {@link FactoryBean} implementations which do not implement
 * this extended interface are simply assumed to always return independent
 * instances if their {@link #isSingleton()} implementation returns
 * {@code false}; the exposed object is only accessed on demand.
 *
 * <p><b>NOTE:</b> This interface is a special purpose interface, mainly for
 * internal use within the framework and within collaborating frameworks.
 * In general, application-provided FactoryBeans should simply implement
 * the plain {@link FactoryBean} interface. New methods might be added
 * to this extended interface even in point releases.
 *
 * @author Juergen Hoeller
 * @since 2.0.3
 * @param <T> the bean type
 * @see #isPrototype()
 * @see #isSingleton()
 */
// {@link FactoryBean} 接口的扩展。实现可以指示它们是否总是返回独立实例，
// 因为它们的 {@link isSingleton()} 实现返回 {@code false} 没有明确指示独立实例
//
// <p>如果它们的 {@link isSingleton()} 实现返回 {@code false}，
// 则简单地假定未实现此扩展接口的普通 {@link FactoryBean} 实现始终返回独立实例；暴露的对象只能按需访问。
//
// <p><b>注意：<b>这个接口是一个特殊用途的接口，主要供框架内部和协作框架内部使用。
// 通常，应用程序提供的 FactoryBeans 应该简单地实现普通的 {@link FactoryBean} 接口。即使在小版本发布中，新方法也可能被添加到这个扩展接口中。
public interface SmartFactoryBean<T> extends FactoryBean<T> {

	/**
	 * Is the object managed by this factory a prototype? That is,
	 * will {@link #getObject()} always return an independent instance?
	 * <p>The prototype status of the FactoryBean itself will generally
	 * be provided by the owning {@link BeanFactory}; usually, it has to be
	 * defined as singleton there.
	 * <p>This method is supposed to strictly check for independent instances;
	 * it should not return {@code true} for scoped objects or other
	 * kinds of non-singleton, non-independent objects. For this reason,
	 * this is not simply the inverted form of {@link #isSingleton()}.
	 * <p>The default implementation returns {@code false}.
	 * @return whether the exposed object is a prototype
	 * @see #getObject()
	 * @see #isSingleton()
	 */
	// 这个工厂管理的对象是原型吗？也就是说，{@link getObject()} 会一直返回一个独立的实例吗？
	// <p>FactoryBean 本身的原型状态一般会由拥有者 {@link BeanFactory} 提供；通常，它必须在那里定义为单例。
	// <p>这个方法应该严格检查独立实例；它不应为作用域对象或其他类型的非单一、非独立对象返回 {@code true}。
	// 出于这个原因，这不仅仅是 {@link isSingleton()} 的倒置形式。
	// <p>默认实现返回 {@code false}。
	default boolean isPrototype() {
		return false;
	}

	/**
	 * Does this FactoryBean expect eager initialization, that is,
	 * eagerly initialize itself as well as expect eager initialization
	 * of its singleton object (if any)?
	 * <p>A standard FactoryBean is not expected to initialize eagerly:
	 * Its {@link #getObject()} will only be called for actual access, even
	 * in case of a singleton object. Returning {@code true} from this
	 * method suggests that {@link #getObject()} should be called eagerly,
	 * also applying post-processors eagerly. This may make sense in case
	 * of a {@link #isSingleton() singleton} object, in particular if
	 * post-processors expect to be applied on startup.
	 * <p>The default implementation returns {@code false}.
	 * @return whether eager initialization applies
	 * @see org.springframework.beans.factory.config.ConfigurableListableBeanFactory#preInstantiateSingletons()
	 */
	// 这个 FactoryBean 是否期望急切初始化，即急切初始化自身以及期望对其单例对象（如果有）的急切初始化？
	// <p>标准 FactoryBean 不应该急切地初始化：它的 {@link getObject()} 只会在实际访问时被调用，即使是在单例对象的情况下。
	// 从此方法返回 {@code true} 表明应该急切地调用 {@link getObject()}，也急切地应用后处理器。
	// 在 {@link isSingleton() singleton} 对象的情况下，这可能是有意义的，特别是如果后处理器希望在启动时应用。
	// <p>默认实现返回 {@code false}。
	default boolean isEagerInit() {
		return false;
	}

}
