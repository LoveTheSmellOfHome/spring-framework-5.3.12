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

package org.springframework.core;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;

/**
 * {@link Comparator} implementation for {@link Ordered} objects, sorting
 * by order value ascending, respectively by priority descending.
 *
 * <h3>{@code PriorityOrdered} Objects</h3>
 * <p>{@link PriorityOrdered} objects will be sorted with higher priority than
 * <em>plain</em> {@code Ordered} objects.
 *
 * <h3>Same Order Objects</h3>
 * <p>Objects that have the same order value will be sorted with arbitrary
 * ordering with respect to other objects with the same order value.
 *
 * <h3>Non-ordered Objects</h3>
 * <p>Any object that does not provide its own order value is implicitly
 * assigned a value of {@link Ordered#LOWEST_PRECEDENCE}, thus ending up
 * at the end of a sorted collection in arbitrary order with respect to
 * other objects with the same order value.
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 07.04.2003
 * @see Ordered
 * @see PriorityOrdered
 * @see org.springframework.core.annotation.AnnotationAwareOrderComparator
 * @see java.util.List#sort(java.util.Comparator)
 * @see java.util.Arrays#sort(Object[], java.util.Comparator)
 */
// Ordered对象的Comparator实现，按顺序值升序排序，分别按优先级降序排序
//
// PriorityOrdered对象将按比普通Ordered对象更高的优先级Ordered
//
// 任何不提供自己的 order 值的对象都被隐式分配了Ordered.LOWEST_PRECEDENCE的值，
// 因此相对于具有相同 order 值的其他对象，以任意顺序结束在排序集合的末尾。最大整数值拥有最小优先级
public class OrderComparator implements Comparator<Object> {

	/**
	 * Shared default instance of {@code OrderComparator}.
	 */
	// OrderComparator共享默认实例
	public static final OrderComparator INSTANCE = new OrderComparator();


	/**
	 * Build an adapted order comparator with the given source provider.
	 * @param sourceProvider the order source provider to use
	 * @return the adapted comparator
	 * @since 4.1
	 */
	// 使用给定的源提供程序构建一个适配的顺序比较器
	public Comparator<Object> withSourceProvider(OrderSourceProvider sourceProvider) {
		return (o1, o2) -> doCompare(o1, o2, sourceProvider);
	}

	@Override
	public int compare(@Nullable Object o1, @Nullable Object o2) {
		return doCompare(o1, o2, null);
	}

	private int doCompare(@Nullable Object o1, @Nullable Object o2, @Nullable OrderSourceProvider sourceProvider) {
		// 第一优先判断 PriorityOrdered 这个接口
		boolean p1 = (o1 instanceof PriorityOrdered);
		boolean p2 = (o2 instanceof PriorityOrdered);
		if (p1 && !p2) {
			return -1;
		}
		else if (p2 && !p1) {
			return 1;
		}

		// 第二优先判断 getOrder()
		int i1 = getOrder(o1, sourceProvider);
		int i2 = getOrder(o2, sourceProvider);
		return Integer.compare(i1, i2);
	}

	/**
	 * Determine the order value for the given object.
	 * <p>The default implementation checks against the given {@link OrderSourceProvider}
	 * using {@link #findOrder} and falls back to a regular {@link #getOrder(Object)} call.
	 * @param obj the object to check
	 * @return the order value, or {@code Ordered.LOWEST_PRECEDENCE} as fallback
	 */
	// 确定给定对象的排序值 order value
	// 默认实现使用 findOrder 检查给定的 OrderComparator.OrderSourceProvider 并回退到常规 getOrder(Object) 调用。
	private int getOrder(@Nullable Object obj, @Nullable OrderSourceProvider sourceProvider) {
		Integer order = null;
		if (obj != null && sourceProvider != null) {
			Object orderSource = sourceProvider.getOrderSource(obj);
			if (orderSource != null) {
				if (orderSource.getClass().isArray()) {
					for (Object source : ObjectUtils.toObjectArray(orderSource)) {
						// 判断是不是 Ordered 接口，
						order = findOrder(source);
						if (order != null) {
							break;
						}
					}
				}
				else {
					order = findOrder(orderSource);
				}
			}
		}
		return (order != null ? order : getOrder(obj));
	}

	/**
	 * Determine the order value for the given object.
	 * <p>The default implementation checks against the {@link Ordered} interface
	 * through delegating to {@link #findOrder}. Can be overridden in subclasses.
	 * @param obj the object to check
	 * @return the order value, or {@code Ordered.LOWEST_PRECEDENCE} as fallback
	 */
	// 确定给定对象的排序值。
	// 默认实现通过委托给findOrder来检查Ordered接口。 可以在子类中覆盖。
	// 形参：obj – 要检查的对象
	// 返回值：排序值，或Ordered.LOWEST_PRECEDENCE作为最低优先级的兜底
	protected int getOrder(@Nullable Object obj) {
		if (obj != null) {
			Integer order = findOrder(obj);
			if (order != null) {
				return order;
			}
		}
		return Ordered.LOWEST_PRECEDENCE;
	}

	/**
	 * Find an order value indicated by the given object.
	 * <p>The default implementation checks against the {@link Ordered} interface.
	 * Can be overridden in subclasses.
	 * @param obj the object to check
	 * @return the order value, or {@code null} if none found
	 */
	// 查找由给定对象指示的排序值。默认实现检查Ordered接口。 可以在子类中覆盖。
	// 形参：obj – 要检查的对象
	//返回值：排序值，如果没有找到，则为null
	@Nullable
	protected Integer findOrder(Object obj) {
		// 判断是不是 Ordered 接口，获取接口中的值是多少
		return (obj instanceof Ordered ? ((Ordered) obj).getOrder() : null);
	}

	/**
	 * Determine a priority value for the given object, if any.
	 * <p>The default implementation always returns {@code null}.
	 * Subclasses may override this to give specific kinds of values a
	 * 'priority' characteristic, in addition to their 'order' semantics.
	 * A priority indicates that it may be used for selecting one object over
	 * another, in addition to serving for ordering purposes in a list/array.
	 * @param obj the object to check
	 * @return the priority value, or {@code null} if none
	 * @since 4.1
	 */
	// 确定给定对象的优先级值（如果有）。
	// 默认实现始终返回null 。 除了它们的“顺序”语义之外，子类可以覆盖它以给特定种类的值一个“优先级”特征。
	// 优先级表示它可以用于选择一个对象而不是另一个对象，除了用于列表/数组中的排序目的。
	// 形参：obj – 要检查的对象
	// 返回值：优先级值，如果没有，则为null
	@Nullable
	public Integer getPriority(Object obj) {
		return null;
	}


	/**
	 * Sort the given List with a default OrderComparator.
	 * <p>Optimized to skip sorting for lists with size 0 or 1,
	 * in order to avoid unnecessary array extraction.
	 * @param list the List to sort
	 * @see java.util.List#sort(java.util.Comparator)
	 */
	// 使用默认的 OrderComparator 对给定的 List 进行排序。
	// 优化为跳过大小为 0 或 1 的列表的排序，以避免不必要的数组提取。
	// 形参：list – 要排序的列表
	// 请参阅：List.sort(Comparator)
	public static void sort(List<?> list) {
		if (list.size() > 1) {
			list.sort(INSTANCE);
		}
	}

	/**
	 * Sort the given array with a default OrderComparator.
	 * <p>Optimized to skip sorting for lists with size 0 or 1,
	 * in order to avoid unnecessary array extraction.
	 * @param array the array to sort
	 * @see java.util.Arrays#sort(Object[], java.util.Comparator)
	 */
	// 使用默认的 OrderComparator 对给定数组进行排序。
	// 优化为跳过大小为 0 或 1 的列表的排序，以避免不必要的数组提取。
	// 形参：数组– 要排序的数组
	public static void sort(Object[] array) {
		if (array.length > 1) {
			Arrays.sort(array, INSTANCE);
		}
	}

	/**
	 * Sort the given array or List with a default OrderComparator,
	 * if necessary. Simply skips sorting when given any other value.
	 * <p>Optimized to skip sorting for lists with size 0 or 1,
	 * in order to avoid unnecessary array extraction.
	 * @param value the array or List to sort
	 * @see java.util.Arrays#sort(Object[], java.util.Comparator)
	 */
	// 如有必要，使用默认的 OrderComparator 对给定的数组或列表进行排序。 当给定任何其他值时，简单地跳过排序。
	// 优化为跳过大小为 0 或 1 的列表的排序，以避免不必要的数组提取。
	// 形参：value – 要排序的数组或列表
	public static void sortIfNecessary(Object value) {
		if (value instanceof Object[]) {
			sort((Object[]) value);
		}
		else if (value instanceof List) {
			sort((List<?>) value);
		}
	}


	/**
	 * Strategy interface to provide an order source for a given object.
	 * @since 4.1
	 */
	// 为给定对象提供排序源的策略接口
	@FunctionalInterface
	public interface OrderSourceProvider {

		/**
		 * Return an order source for the specified object, i.e. an object that
		 * should be checked for an order value as a replacement to the given object.
		 * <p>Can also be an array of order source objects.
		 * <p>If the returned object does not indicate any order, the comparator
		 * will fall back to checking the original object.
		 * @param obj the object to find an order source for
		 * @return the order source for that object, or {@code null} if none found
		 */
		// 返回指定对象的排序源，即应该检查排序值作为给定对象的替换的对象。
		// 也可以是排序源对象的数组。
		// 如果返回的对象没有指示任何顺序，比较器将回退到检查原始对象。
		// 形参：obj – 要为其查找排序源的对象
		// 返回值：该对象的订单来源，如果没有找到则为null
		@Nullable
		Object getOrderSource(Object obj);
	}

}
