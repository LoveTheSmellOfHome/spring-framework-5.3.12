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

import java.util.Iterator;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.springframework.beans.BeansException;
import org.springframework.lang.Nullable;

/**
 * A variant of {@link ObjectFactory} designed specifically for injection points,
 * allowing for programmatic optionality and lenient not-unique handling.
 *
 * <p>As of 5.1, this interface extends {@link Iterable} and provides {@link Stream}
 * support. It can be therefore be used in {@code for} loops, provides {@link #forEach}
 * iteration and allows for collection-style {@link #stream} access.
 *
 * @author Juergen Hoeller
 * @since 4.3
 * @param <T> the object type
 * @see BeanFactory#getBeanProvider
 * @see org.springframework.beans.factory.annotation.Autowired
 */
// 专为注入点设计的ObjectFactory变体，允许编程可选和宽松的非唯一处理。
// 从 5.1 开始，此接口扩展了Iterable并提供了Stream支持。 因此，它可以用于for循环，提供forEach迭代并允许集合样式的stream访问。
public interface ObjectProvider<T> extends ObjectFactory<T>, Iterable<T> {

	/**
	 * Return an instance (possibly shared or independent) of the object
	 * managed by this factory.
	 * <p>Allows for specifying explicit construction arguments, along the
	 * lines of {@link BeanFactory#getBean(String, Object...)}.
	 * @param args arguments to use when creating a corresponding instance
	 * @return an instance of the bean
	 * @throws BeansException in case of creation errors
	 * @see #getObject()
	 */
	// 返回此工厂管理的对象的实例（可能是共享的或独立的）。
	// 允许按照BeanFactory.getBean(String, Object...)指定显式构造参数。
	T getObject(Object... args) throws BeansException;

	/**
	 * Return an instance (possibly shared or independent) of the object
	 * managed by this factory.
	 * @return an instance of the bean, or {@code null} if not available
	 * @throws BeansException in case of creation errors
	 * @see #getObject()
	 */
	// 返回此工厂管理的对象的实例（可能是共享的或独立的）。
	// 返回值：bean 的一个实例，如果不可用则为null
	@Nullable
	T getIfAvailable() throws BeansException;

	/**
	 * Return an instance (possibly shared or independent) of the object
	 * managed by this factory.
	 * @param defaultSupplier a callback for supplying a default object
	 * if none is present in the factory
	 * @return an instance of the bean, or the supplied default object
	 * if no such bean is available
	 * @throws BeansException in case of creation errors
	 * @since 5.0
	 * @see #getIfAvailable()
	 */
	// 返回此工厂管理的对象的实例（可能是共享的或独立的）。
	// 形参：defaultSupplier – 如果工厂中不存在默认对象，则用于提供默认对象的回调
	// 返回值：bean 的一个实例，或者如果没有这样的 bean 可用，则提供默认对象
	// BeansException - 在创建错误的情况下
	default T getIfAvailable(Supplier<T> defaultSupplier) throws BeansException {
		T dependency = getIfAvailable();
		return (dependency != null ? dependency : defaultSupplier.get());
	}

	/**
	 * Consume an instance (possibly shared or independent) of the object
	 * managed by this factory, if available.
	 * @param dependencyConsumer a callback for processing the target object
	 * if available (not called otherwise)
	 * @throws BeansException in case of creation errors
	 * @since 5.0
	 * @see #getIfAvailable()
	 */
	// 如果可用，请使用由该工厂管理的对象的实例（可能是共享的或独立的）。
	// 形参：dependencyConsumer – 用于处理目标对象（如果可用）的回调（不以其他方式调用）
	// BeansException - 在创建错误的情况下
	default void ifAvailable(Consumer<T> dependencyConsumer) throws BeansException {
		T dependency = getIfAvailable();
		if (dependency != null) {
			dependencyConsumer.accept(dependency);
		}
	}

	/**
	 * Return an instance (possibly shared or independent) of the object
	 * managed by this factory.
	 * @return an instance of the bean, or {@code null} if not available or
	 * not unique (i.e. multiple candidates found with none marked as primary)
	 * @throws BeansException in case of creation errors
	 * @see #getObject()
	 */
	// 返回此工厂管理的对象的实例（可能是共享的或独立的）。
	// 返回值：bean 的一个实例，如果不可用或不是唯一的，则为null （即找到多个没有标记为主要的候选者）
	@Nullable
	T getIfUnique() throws BeansException;

	/**
	 * Return an instance (possibly shared or independent) of the object
	 * managed by this factory.
	 * @param defaultSupplier a callback for supplying a default object
	 * if no unique candidate is present in the factory
	 * @return an instance of the bean, or the supplied default object
	 * if no such bean is available or if it is not unique in the factory
	 * (i.e. multiple candidates found with none marked as primary)
	 * @throws BeansException in case of creation errors
	 * @since 5.0
	 * @see #getIfUnique()
	 */
	// 返回此工厂管理的对象的实例（可能是共享的或独立的）。
	// 形参：defaultSupplier – 如果工厂中不存在唯一候选对象，则用于提供默认对象的回调
	// 返回值：bean 的一个实例，或者提供的默认对象，如果没有这样的 bean 可用或者它在工厂中
	// 不是唯一的（即发现多个候选对象，但没有标记为主要的）
	default T getIfUnique(Supplier<T> defaultSupplier) throws BeansException {
		T dependency = getIfUnique();
		return (dependency != null ? dependency : defaultSupplier.get());
	}

	/**
	 * Consume an instance (possibly shared or independent) of the object
	 * managed by this factory, if unique.
	 * @param dependencyConsumer a callback for processing the target object
	 * if unique (not called otherwise)
	 * @throws BeansException in case of creation errors
	 * @since 5.0
	 * @see #getIfAvailable()
	 */
	// 如果唯一，则使用由该工厂管理的对象的实例（可能是共享的或独立的）。
	// 形参：dependencyConsumer – 用于处理目标对象的回调，如果唯一（否则不调用）
	default void ifUnique(Consumer<T> dependencyConsumer) throws BeansException {
		T dependency = getIfUnique();
		if (dependency != null) {
			dependencyConsumer.accept(dependency);
		}
	}

	/**
	 * Return an {@link Iterator} over all matching object instances,
	 * without specific ordering guarantees (but typically in registration order).
	 * @since 5.1
	 * @see #stream()
	 */
	// 在所有匹配的对象实例上返回一个Iterator ，没有特定的排序保证（但通常按注册顺序）
	@Override
	default Iterator<T> iterator() {
		return stream().iterator();
	}

	/**
	 * Return a sequential {@link Stream} over all matching object instances,
	 * without specific ordering guarantees (but typically in registration order).
	 * @since 5.1
	 * @see #iterator()
	 * @see #orderedStream()
	 */
	// 在所有匹配的对象实例上返回一个顺序Stream ，没有特定的排序保证（但通常按注册顺序）
	default Stream<T> stream() {
		throw new UnsupportedOperationException("Multi element access not supported");
	}

	/**
	 * Return a sequential {@link Stream} over all matching object instances,
	 * pre-ordered according to the factory's common order comparator.
	 * <p>In a standard Spring application context, this will be ordered
	 * according to {@link org.springframework.core.Ordered} conventions,
	 * and in case of annotation-based configuration also considering the
	 * {@link org.springframework.core.annotation.Order} annotation,
	 * analogous to multi-element injection points of list/array type.
	 * @since 5.1
	 * @see #stream()
	 * @see org.springframework.core.OrderComparator
	 */
	// 返回所有匹配对象实例的顺序Stream ，根据工厂的公共顺序比较器预先排序。
	//
	// 在标准的 Spring 应用上下文中，这将根据org.springframework.core.Ordered 约定进行排序，
	// 并且在基于注解的配置的情况下也考虑 org.springframework.core.annotation.Order 注解，
	// 类似于多元素注入列表/数组类型的点。
	default Stream<T> orderedStream() {
		throw new UnsupportedOperationException("Ordered element access not supported");
	}

}
