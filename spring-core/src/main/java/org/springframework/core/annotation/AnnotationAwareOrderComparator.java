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

package org.springframework.core.annotation;

import org.springframework.core.DecoratingProxy;
import org.springframework.core.OrderComparator;
import org.springframework.core.annotation.MergedAnnotations.SearchStrategy;
import org.springframework.lang.Nullable;

import java.lang.reflect.AnnotatedElement;
import java.util.Arrays;
import java.util.List;

/**
 * {@code AnnotationAwareOrderComparator} is an extension of
 * {@link OrderComparator} that supports Spring's
 * {@link org.springframework.core.Ordered} interface as well as the
 * {@link Order @Order} and {@link javax.annotation.Priority @Priority}
 * annotations, with an order value provided by an {@code Ordered}
 * instance overriding a statically defined annotation value (if any).
 *
 * <p>Consult the Javadoc for {@link OrderComparator} for details on the
 * sort semantics for non-ordered objects.
 *
 * @author Juergen Hoeller
 * @author Oliver Gierke
 * @author Stephane Nicoll
 * @since 2.0.1
 * @see org.springframework.core.Ordered
 * @see org.springframework.core.annotation.Order
 * @see javax.annotation.Priority
 */
// AnnotationAwareOrderComparator 是 OrderComparator 的扩展，它支持 Spring 的 org.springframework.core.Ordered
// 接口以及 @Order 和 @Priority 注解， Ordered 实例提供的顺序值覆盖静态定义的注解值（如果有）。
// 有关无序对象的排序语义的详细信息，请参阅 OrderComparator的 Javadoc。
public class AnnotationAwareOrderComparator extends OrderComparator {

	/**
	 * Shared default instance of {@code AnnotationAwareOrderComparator}.
	 */
	// AnnotationAwareOrderComparator的共享默认实例，注解感知顺序比较器
	public static final AnnotationAwareOrderComparator INSTANCE = new AnnotationAwareOrderComparator();


	/**
	 * This implementation checks for {@link Order @Order} or
	 * {@link javax.annotation.Priority @Priority} on various kinds of
	 * elements, in addition to the {@link org.springframework.core.Ordered}
	 * check in the superclass.
	 */
	// 除了超类中的 org.springframework.core.Ordered 检查之外，此实现还检查各种元素上的 @Order 或 @Priority
	@Override
	@Nullable
	protected Integer findOrder(Object obj) {
		Integer order = super.findOrder(obj);
		if (order != null) {
			return order;
		}
		return findOrderFromAnnotation(obj);
	}

	// 查找注解上的排序大小
	@Nullable
	private Integer findOrderFromAnnotation(Object obj) {
		AnnotatedElement element = (obj instanceof AnnotatedElement ? (AnnotatedElement) obj : obj.getClass());
		MergedAnnotations annotations = MergedAnnotations.from(element, SearchStrategy.TYPE_HIERARCHY);
		Integer order = OrderUtils.getOrderFromAnnotations(element, annotations);
		if (order == null && obj instanceof DecoratingProxy) {
			return findOrderFromAnnotation(((DecoratingProxy) obj).getDecoratedClass());
		}
		return order;
	}

	/**
	 * This implementation retrieves an @{@link javax.annotation.Priority}
	 * value, allowing for additional semantics over the regular @{@link Order}
	 * annotation: typically, selecting one object over another in case of
	 * multiple matches but only one object to be returned.
	 */
	// 此实现检索 @javax.annotation.Priority 值，允许在常规 Order 注解上提供附加语义：
	// 通常，在多个匹配但仅返回一个对象的情况下选择一个对象而不是另一个对象。
	@Override
	@Nullable
	public Integer getPriority(Object obj) {
		if (obj instanceof Class) {
			return OrderUtils.getPriority((Class<?>) obj);
		}
		Integer priority = OrderUtils.getPriority(obj.getClass());
		if (priority == null  && obj instanceof DecoratingProxy) {
			return getPriority(((DecoratingProxy) obj).getDecoratedClass());
		}
		return priority;
	}


	/**
	 * Sort the given list with a default {@link AnnotationAwareOrderComparator}.
	 * <p>Optimized to skip sorting for lists with size 0 or 1,
	 * in order to avoid unnecessary array extraction.
	 * @param list the List to sort
	 * @see java.util.List#sort(java.util.Comparator)
	 */
	// 使用默认 AnnotationAwareOrderComparator 对给定列表进行排序。
	// 优化以跳过大小为 0 或 1 的列表的排序，以避免不必要的数组提取。
	// 参形：
	//			list – 要排序的列表
	public static void sort(List<?> list) {
		if (list.size() > 1) {
			list.sort(INSTANCE);
		}
	}

	/**
	 * Sort the given array with a default AnnotationAwareOrderComparator.
	 * <p>Optimized to skip sorting for lists with size 0 or 1,
	 * in order to avoid unnecessary array extraction.
	 * @param array the array to sort
	 * @see java.util.Arrays#sort(Object[], java.util.Comparator)
	 */
	// 使用默认 AnnotationAwareOrderComparator 对给定数组进行排序。
	// 优化以跳过大小为 0 或 1 的列表的排序，以避免不必要的数组提取。
	// 参形：
	//			array - 要排序的数组
	public static void sort(Object[] array) {
		if (array.length > 1) {
			Arrays.sort(array, INSTANCE);
		}
	}

	/**
	 * Sort the given array or List with a default AnnotationAwareOrderComparator,
	 * if necessary. Simply skips sorting when given any other value.
	 * <p>Optimized to skip sorting for lists with size 0 or 1,
	 * in order to avoid unnecessary array extraction.
	 * @param value the array or List to sort
	 * @see java.util.Arrays#sort(Object[], java.util.Comparator)
	 */
	// 如有必要，使用默认 AnnotationAwareOrderComparator 对给定数组或列表进行排序。当给定任何其他值时，只需跳过排序。
	// 优化以跳过大小为 0 或 1 的列表的排序，以避免不必要的数组提取。
	// 参形：
	//			value – 要排序的数组或列表
	public static void sortIfNecessary(Object value) {
		if (value instanceof Object[]) {
			sort((Object[]) value);
		}
		else if (value instanceof List) {
			sort((List<?>) value);
		}
	}

}
