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

package org.springframework.beans.factory.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.BeanMetadataElement;
import org.springframework.beans.Mergeable;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;

/**
 * Holder for constructor argument values, typically as part of a bean definition.
 *
 * <p>Supports values for a specific index in the constructor argument list
 * as well as for generic argument matches by type.
 *
 * @author Juergen Hoeller
 * @since 09.11.2003
 * @see BeanDefinition#getConstructorArgumentValues
 */
// 构造函数参数值的持有者，通常作为 bean 定义的一部分
// <p>支持构造函数参数列表中特定索引的值以及按类型匹配的泛型参数
public class ConstructorArgumentValues {

	private final Map<Integer, ValueHolder> indexedArgumentValues = new LinkedHashMap<>();

	private final List<ValueHolder> genericArgumentValues = new ArrayList<>();


	/**
	 * Create a new empty ConstructorArgumentValues object.
	 */
	// 创建一个新的空 ConstructorArgumentValues 对象。
	public ConstructorArgumentValues() {
	}

	/**
	 * Deep copy constructor.
	 * @param original the ConstructorArgumentValues to copy
	 */
	// 深拷贝构造函数。 @param 原始要复制的 ConstructorArgumentValues
	public ConstructorArgumentValues(ConstructorArgumentValues original) {
		addArgumentValues(original);
	}


	/**
	 * Copy all given argument values into this object, using separate holder
	 * instances to keep the values independent from the original object.
	 * <p>Note: Identical ValueHolder instances will only be registered once,
	 * to allow for merging and re-merging of argument value definitions. Distinct
	 * ValueHolder instances carrying the same content are of course allowed.
	 */
	// 将所有给定的参数值复制到这个对象中，使用单独的持有者实例来保持这些值独立于原始对象。
	// <p>注意：相同的 ValueHolder 实例只会被注册一次，以允许合并和重新合并参数值定义。
	// 携带相同内容的 Distinct ValueHolder 实例当然是允许的。
	public void addArgumentValues(@Nullable ConstructorArgumentValues other) {
		if (other != null) {
			other.indexedArgumentValues.forEach(
				(index, argValue) -> addOrMergeIndexedArgumentValue(index, argValue.copy())
			);
			other.genericArgumentValues.stream()
					.filter(valueHolder -> !this.genericArgumentValues.contains(valueHolder))
					.forEach(valueHolder -> addOrMergeGenericArgumentValue(valueHolder.copy()));
		}
	}


	/**
	 * Add an argument value for the given index in the constructor argument list.
	 * @param index the index in the constructor argument list
	 * @param value the argument value
	 */
	// 在构造函数参数列表中为给定索引添加参数值
	// @param index 构造函数参数列表中的索引
	// @param value 参数值
	public void addIndexedArgumentValue(int index, @Nullable Object value) {
		addIndexedArgumentValue(index, new ValueHolder(value));
	}

	/**
	 * Add an argument value for the given index in the constructor argument list.
	 * @param index the index in the constructor argument list
	 * @param value the argument value
	 * @param type the type of the constructor argument
	 */
	// 在构造函数参数列表中为给定索引添加参数值
	// @param index 构造函数参数列表中的索引
	// @param value 参数值
	// @param type 构造函数参数的类型
	public void addIndexedArgumentValue(int index, @Nullable Object value, String type) {
		addIndexedArgumentValue(index, new ValueHolder(value, type));
	}

	/**
	 * Add an argument value for the given index in the constructor argument list.
	 * @param index the index in the constructor argument list
	 * @param newValue the argument value in the form of a ValueHolder
	 */
	// 在构造函数参数列表中为给定索引添加参数值
	// @param index 构造函数参数列表中的索引
	// @param newValue ValueHolder 形式的参数值
	public void addIndexedArgumentValue(int index, ValueHolder newValue) {
		Assert.isTrue(index >= 0, "Index must not be negative");
		Assert.notNull(newValue, "ValueHolder must not be null");
		addOrMergeIndexedArgumentValue(index, newValue);
	}

	/**
	 * Add an argument value for the given index in the constructor argument list,
	 * merging the new value (typically a collection) with the current value
	 * if demanded: see {@link org.springframework.beans.Mergeable}.
	 * @param key the index in the constructor argument list
	 * @param newValue the argument value in the form of a ValueHolder
	 */
	// 在构造函数参数列表中为给定索引添加一个参数值，如果需要，将新值（通常是一个集合）与当前值合并：
	// 参见 {@link org.springframework.beans.Mergeable}。
	// @param key 构造函数参数列表中的索引
	// @param newValue ValueHolder 形式的参数值
	private void addOrMergeIndexedArgumentValue(Integer key, ValueHolder newValue) {
		ValueHolder currentValue = this.indexedArgumentValues.get(key);
		if (currentValue != null && newValue.getValue() instanceof Mergeable) {
			Mergeable mergeable = (Mergeable) newValue.getValue();
			if (mergeable.isMergeEnabled()) {
				newValue.setValue(mergeable.merge(currentValue.getValue()));
			}
		}
		this.indexedArgumentValues.put(key, newValue);
	}

	/**
	 * Check whether an argument value has been registered for the given index.
	 * @param index the index in the constructor argument list
	 */
	// 检查是否已为给定索引注册了参数值。
	// @param index 构造函数参数列表中的索引
	public boolean hasIndexedArgumentValue(int index) {
		return this.indexedArgumentValues.containsKey(index);
	}

	/**
	 * Get argument value for the given index in the constructor argument list.
	 * @param index the index in the constructor argument list
	 * @param requiredType the type to match (can be {@code null} to match
	 * untyped values only)
	 * @return 参数的 ValueHolder，或者 {@code null} 如果没有设置
	 */
	// 获取构造函数参数列表中给定索引的参数值
	// @param index 构造函数参数列表中的索引
	// @param requiredType 要匹配的类型（可以是 {@code null} 以仅匹配无类型值）
	// @param requiredType 要匹配的类型（可以是 {@code null} 来匹配
	@Nullable
	public ValueHolder getIndexedArgumentValue(int index, @Nullable Class<?> requiredType) {
		return getIndexedArgumentValue(index, requiredType, null);
	}

	/**
	 * Get argument value for the given index in the constructor argument list.
	 * @param index the index in the constructor argument list
	 * @param requiredType the type to match (can be {@code null} to match
	 * untyped values only)
	 * @param requiredName the type to match (can be {@code null} to match
	 * unnamed values only, or empty String to match any name)
	 * @return the ValueHolder for the argument, or {@code null} if none set
	 */
	// 获取构造函数参数列表中给定索引的参数值
	// @param index 构造函数参数列表中的索引
	// @param requiredType 要匹配的类型（可以是 {@code null} 以仅匹配无类型值）
	// @param requiredName 要匹配的类型（可以是 {@code null} 以仅匹配未命名的值，或空字符串以匹配任何名称）
	// @return 参数的 ValueHolder，或者 {@code null} 如果没有设置
	@Nullable
	public ValueHolder getIndexedArgumentValue(int index, @Nullable Class<?> requiredType, @Nullable String requiredName) {
		Assert.isTrue(index >= 0, "Index must not be negative");
		ValueHolder valueHolder = this.indexedArgumentValues.get(index);
		if (valueHolder != null &&
				(valueHolder.getType() == null || (requiredType != null &&
						ClassUtils.matchesTypeName(requiredType, valueHolder.getType()))) &&
				(valueHolder.getName() == null || (requiredName != null &&
						(requiredName.isEmpty() || requiredName.equals(valueHolder.getName()))))) {
			return valueHolder;
		}
		return null;
	}

	/**
	 * Return the map of indexed argument values.
	 * @return unmodifiable Map with Integer index as key and ValueHolder as value
	 * @see ValueHolder
	 */
	// 返回索引参数值的映射。
	// @return unmodifiable Map 以整数索引为键，ValueHolder 为值
	public Map<Integer, ValueHolder> getIndexedArgumentValues() {
		return Collections.unmodifiableMap(this.indexedArgumentValues);
	}


	/**
	 * Add a generic argument value to be matched by type.
	 * <p>Note: A single generic argument value will just be used once,
	 * rather than matched multiple times.
	 * @param value the argument value
	 */
	// 添加要按类型匹配的通用参数值。 <p>注意：单个通用参数值只会使用一次，而不是多次匹配
	// @param value 参数值
	public void addGenericArgumentValue(Object value) {
		this.genericArgumentValues.add(new ValueHolder(value));
	}

	/**
	 * Add a generic argument value to be matched by type.
	 * <p>Note: A single generic argument value will just be used once,
	 * rather than matched multiple times.
	 * @param value the argument value
	 * @param type the type of the constructor argument
	 */
	// 添加要按类型匹配的通用参数值。 <p>注意：单个通用参数值只会使用一次，而不是多次匹配。
	public void addGenericArgumentValue(Object value, String type) {
		this.genericArgumentValues.add(new ValueHolder(value, type));
	}

	/**
	 * Add a generic argument value to be matched by type or name (if available).
	 * <p>Note: A single generic argument value will just be used once,
	 * rather than matched multiple times.
	 * @param newValue the argument value in the form of a ValueHolder
	 * <p>Note: Identical ValueHolder instances will only be registered once,
	 * to allow for merging and re-merging of argument value definitions. Distinct
	 * ValueHolder instances carrying the same content are of course allowed.
	 */
	// 添加要按类型或名称匹配的通用参数值（如果可用）。
	// <p>注意：单个通用参数值只会使用一次，而不是多次匹配。
	// @param newValue ValueHolder 形式的参数值
	// <p>注意：相同的 ValueHolder 实例只会注册一次，以允许合并和重新合并参数值定义。携带相同内容的 Distinct ValueHolder 实例当然是允许的
	public void addGenericArgumentValue(ValueHolder newValue) {
		Assert.notNull(newValue, "ValueHolder must not be null");
		if (!this.genericArgumentValues.contains(newValue)) {
			addOrMergeGenericArgumentValue(newValue);
		}
	}

	/**
	 * Add a generic argument value, merging the new value (typically a collection)
	 * with the current value if demanded: see {@link org.springframework.beans.Mergeable}.
	 * @param newValue the argument value in the form of a ValueHolder
	 */
	// 添加一个通用参数值，如果需要，将新值（通常是一个集合）与当前值合并：参见 {@link org.springframework.beans.Mergeable}。
	private void addOrMergeGenericArgumentValue(ValueHolder newValue) {
		if (newValue.getName() != null) {
			for (Iterator<ValueHolder> it = this.genericArgumentValues.iterator(); it.hasNext();) {
				ValueHolder currentValue = it.next();
				if (newValue.getName().equals(currentValue.getName())) {
					if (newValue.getValue() instanceof Mergeable) {
						Mergeable mergeable = (Mergeable) newValue.getValue();
						if (mergeable.isMergeEnabled()) {
							newValue.setValue(mergeable.merge(currentValue.getValue()));
						}
					}
					it.remove();
				}
			}
		}
		this.genericArgumentValues.add(newValue);
	}

	/**
	 * Look for a generic argument value that matches the given type.
	 * @param requiredType the type to match
	 * @return the ValueHolder for the argument, or {@code null} if none set
	 */
	// 查找与给定类型匹配的通用参数值
	@Nullable
	public ValueHolder getGenericArgumentValue(Class<?> requiredType) {
		return getGenericArgumentValue(requiredType, null, null);
	}

	/**
	 * Look for a generic argument value that matches the given type.
	 * @param requiredType the type to match
	 * @param requiredName the name to match
	 * @return the ValueHolder for the argument, or {@code null} if none set
	 */
	// 查找与给定类型匹配的通用参数值
	@Nullable
	public ValueHolder getGenericArgumentValue(Class<?> requiredType, String requiredName) {
		return getGenericArgumentValue(requiredType, requiredName, null);
	}

	/**
	 * Look for the next generic argument value that matches the given type,
	 * ignoring argument values that have already been used in the current
	 * resolution process.
	 * @param requiredType the type to match (can be {@code null} to find
	 * an arbitrary next generic argument value)
	 * @param requiredName the name to match (can be {@code null} to not
	 * match argument values by name, or empty String to match any name)
	 * @param usedValueHolders a Set of ValueHolder objects that have already been used
	 * in the current resolution process and should therefore not be returned again
	 * @return the ValueHolder for the argument, or {@code null} if none found
	 */
	// 查找与给定类型匹配的下一个通用参数值，忽略已在当前解析过程中使用的参数值。
	// @param requiredType 要匹配的类型（可以是 {@code null} 以查找任意的下一个通用参数值）
	// @param requiredName 要匹配的名称（可以是 {@code null} 以不按名称匹配参数值，或空字符串以匹配任何名称）
	// @param usedValueHolders 一组 ValueHolder 对象已经在当前解析过程中使用过，因此不应再次返回
	// @return 参数的 ValueHolder，或者 {@code null} 如果没有找到
	@Nullable
	public ValueHolder getGenericArgumentValue(@Nullable Class<?> requiredType, @Nullable String requiredName,
			@Nullable Set<ValueHolder> usedValueHolders) {

		for (ValueHolder valueHolder : this.genericArgumentValues) {
			if (usedValueHolders != null && usedValueHolders.contains(valueHolder)) {
				continue;
			}
			if (valueHolder.getName() != null && (requiredName == null ||
					(!requiredName.isEmpty() && !requiredName.equals(valueHolder.getName())))) {
				continue;
			}
			if (valueHolder.getType() != null && (requiredType == null ||
					!ClassUtils.matchesTypeName(requiredType, valueHolder.getType()))) {
				continue;
			}
			if (requiredType != null && valueHolder.getType() == null && valueHolder.getName() == null &&
					!ClassUtils.isAssignableValue(requiredType, valueHolder.getValue())) {
				continue;
			}
			return valueHolder;
		}
		return null;
	}

	/**
	 * Return the list of generic argument values.
	 * @return unmodifiable List of ValueHolders
	 * @see ValueHolder
	 */
	// 返回通用参数值列表
	// @return 不可修改的 ValueHolders 列表
	public List<ValueHolder> getGenericArgumentValues() {
		return Collections.unmodifiableList(this.genericArgumentValues);
	}


	/**
	 * Look for an argument value that either corresponds to the given index
	 * in the constructor argument list or generically matches by type.
	 * @param index the index in the constructor argument list
	 * @param requiredType the parameter type to match
	 * @return the ValueHolder for the argument, or {@code null} if none set
	 */
	// 查找与构造函数参数列表中的给定索引相对应或通常按类型匹配的参数值
	@Nullable
	public ValueHolder getArgumentValue(int index, Class<?> requiredType) {
		return getArgumentValue(index, requiredType, null, null);
	}

	/**
	 * Look for an argument value that either corresponds to the given index
	 * in the constructor argument list or generically matches by type.
	 * @param index the index in the constructor argument list
	 * @param requiredType the parameter type to match
	 * @param requiredName the parameter name to match
	 * @return the ValueHolder for the argument, or {@code null} if none set
	 */
	// 查找与构造函数参数列表中的给定索引相对应或通常按类型匹配的参数值。
	@Nullable
	public ValueHolder getArgumentValue(int index, Class<?> requiredType, String requiredName) {
		return getArgumentValue(index, requiredType, requiredName, null);
	}

	/**
	 * Look for an argument value that either corresponds to the given index
	 * in the constructor argument list or generically matches by type.
	 * @param index the index in the constructor argument list
	 * @param requiredType the parameter type to match (can be {@code null}
	 * to find an untyped argument value)
	 * @param requiredName the parameter name to match (can be {@code null}
	 * to find an unnamed argument value, or empty String to match any name)
	 * @param usedValueHolders a Set of ValueHolder objects that have already
	 * been used in the current resolution process and should therefore not
	 * be returned again (allowing to return the next generic argument match
	 * in case of multiple generic argument values of the same type)
	 * @return the ValueHolder for the argument, or {@code null} if none set
	 */
	// 查找与构造函数参数列表中的给定索引相对应或通常按类型匹配的参数值。
	// @param index 在构造函数参数列表中的索引
	// @param requiredType 要匹配的参数类型（可以是 {@code null} 以查找无类型的参数值）
	// @param requiredName 要匹配的参数名称（可以是 {@code null} 到找到一个未命名的参数值，或空字符串以匹配任何名称）
	// @param usedValueHolders 一组 ValueHolder 对象，这些对象已经在当前解析过程中使用过，
	// 因此不应再次返回（允许返回下一个通用参数匹配，以防万一相同类型的多个泛型参数值）@return 参数的
	@Nullable
	public ValueHolder getArgumentValue(int index, @Nullable Class<?> requiredType,
			@Nullable String requiredName, @Nullable Set<ValueHolder> usedValueHolders) {

		Assert.isTrue(index >= 0, "Index must not be negative");
		ValueHolder valueHolder = getIndexedArgumentValue(index, requiredType, requiredName);
		if (valueHolder == null) {
			valueHolder = getGenericArgumentValue(requiredType, requiredName, usedValueHolders);
		}
		return valueHolder;
	}

	/**
	 * Return the number of argument values held in this instance,
	 * counting both indexed and generic argument values.
	 */
	// 返回此实例中保存的参数值的数量，包括索引和通用参数值。
	public int getArgumentCount() {
		return (this.indexedArgumentValues.size() + this.genericArgumentValues.size());
	}

	/**
	 * Return if this holder does not contain any argument values,
	 * neither indexed ones nor generic ones.
	 */
	// 如果此持有者不包含任何参数值，既不包含索引值也不包含通用值，则返回
	public boolean isEmpty() {
		return (this.indexedArgumentValues.isEmpty() && this.genericArgumentValues.isEmpty());
	}

	/**
	 * Clear this holder, removing all argument values.
	 */
	// 清除此持有者，删除所有参数值。
	public void clear() {
		this.indexedArgumentValues.clear();
		this.genericArgumentValues.clear();
	}


	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof ConstructorArgumentValues)) {
			return false;
		}
		ConstructorArgumentValues that = (ConstructorArgumentValues) other;
		if (this.genericArgumentValues.size() != that.genericArgumentValues.size() ||
				this.indexedArgumentValues.size() != that.indexedArgumentValues.size()) {
			return false;
		}
		Iterator<ValueHolder> it1 = this.genericArgumentValues.iterator();
		Iterator<ValueHolder> it2 = that.genericArgumentValues.iterator();
		while (it1.hasNext() && it2.hasNext()) {
			ValueHolder vh1 = it1.next();
			ValueHolder vh2 = it2.next();
			if (!vh1.contentEquals(vh2)) {
				return false;
			}
		}
		for (Map.Entry<Integer, ValueHolder> entry : this.indexedArgumentValues.entrySet()) {
			ValueHolder vh1 = entry.getValue();
			ValueHolder vh2 = that.indexedArgumentValues.get(entry.getKey());
			if (vh2 == null || !vh1.contentEquals(vh2)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public int hashCode() {
		int hashCode = 7;
		for (ValueHolder valueHolder : this.genericArgumentValues) {
			hashCode = 31 * hashCode + valueHolder.contentHashCode();
		}
		hashCode = 29 * hashCode;
		for (Map.Entry<Integer, ValueHolder> entry : this.indexedArgumentValues.entrySet()) {
			hashCode = 31 * hashCode + (entry.getValue().contentHashCode() ^ entry.getKey().hashCode());
		}
		return hashCode;
	}


	/**
	 * Holder for a constructor argument value, with an optional type
	 * attribute indicating the target type of the actual constructor argument.
	 */
	// 构造函数参数值的持有者，具有指示实际构造函数参数的目标类型的可选类型属性
	public static class ValueHolder implements BeanMetadataElement {

		@Nullable
		private Object value;

		@Nullable
		private String type;

		@Nullable
		private String name;

		@Nullable
		private Object source;

		private boolean converted = false;

		@Nullable
		private Object convertedValue;

		/**
		 * Create a new ValueHolder for the given value.
		 * @param value the argument value
		 */
		public ValueHolder(@Nullable Object value) {
			this.value = value;
		}

		/**
		 * Create a new ValueHolder for the given value and type.
		 * @param value the argument value
		 * @param type the type of the constructor argument
		 */
		public ValueHolder(@Nullable Object value, @Nullable String type) {
			this.value = value;
			this.type = type;
		}

		/**
		 * Create a new ValueHolder for the given value, type and name.
		 * @param value the argument value
		 * @param type the type of the constructor argument
		 * @param name the name of the constructor argument
		 */
		public ValueHolder(@Nullable Object value, @Nullable String type, @Nullable String name) {
			this.value = value;
			this.type = type;
			this.name = name;
		}

		/**
		 * Set the value for the constructor argument.
		 */
		public void setValue(@Nullable Object value) {
			this.value = value;
		}

		/**
		 * Return the value for the constructor argument.
		 */
		@Nullable
		public Object getValue() {
			return this.value;
		}

		/**
		 * Set the type of the constructor argument.
		 */
		public void setType(@Nullable String type) {
			this.type = type;
		}

		/**
		 * Return the type of the constructor argument.
		 */
		@Nullable
		public String getType() {
			return this.type;
		}

		/**
		 * Set the name of the constructor argument.
		 */
		public void setName(@Nullable String name) {
			this.name = name;
		}

		/**
		 * Return the name of the constructor argument.
		 */
		@Nullable
		public String getName() {
			return this.name;
		}

		/**
		 * Set the configuration source {@code Object} for this metadata element.
		 * <p>The exact type of the object will depend on the configuration mechanism used.
		 */
		public void setSource(@Nullable Object source) {
			this.source = source;
		}

		@Override
		@Nullable
		public Object getSource() {
			return this.source;
		}

		/**
		 * Return whether this holder contains a converted value already ({@code true}),
		 * or whether the value still needs to be converted ({@code false}).
		 */
		// 返回此持有者是否已包含转换值（{@code true}），或者该值是否仍需要转换（{@code false}
		public synchronized boolean isConverted() {
			return this.converted;
		}

		/**
		 * Set the converted value of the constructor argument,
		 * after processed type conversion.
		 */
		public synchronized void setConvertedValue(@Nullable Object value) {
			this.converted = (value != null);
			this.convertedValue = value;
		}

		/**
		 * Return the converted value of the constructor argument,
		 * after processed type conversion.
		 */
		@Nullable
		public synchronized Object getConvertedValue() {
			return this.convertedValue;
		}

		/**
		 * Determine whether the content of this ValueHolder is equal
		 * to the content of the given other ValueHolder.
		 * <p>Note that ValueHolder does not implement {@code equals}
		 * directly, to allow for multiple ValueHolder instances with the
		 * same content to reside in the same Set.
		 */
		private boolean contentEquals(ValueHolder other) {
			return (this == other ||
					(ObjectUtils.nullSafeEquals(this.value, other.value) && ObjectUtils.nullSafeEquals(this.type, other.type)));
		}

		/**
		 * Determine whether the hash code of the content of this ValueHolder.
		 * <p>Note that ValueHolder does not implement {@code hashCode}
		 * directly, to allow for multiple ValueHolder instances with the
		 * same content to reside in the same Set.
		 */
		private int contentHashCode() {
			return ObjectUtils.nullSafeHashCode(this.value) * 29 + ObjectUtils.nullSafeHashCode(this.type);
		}

		/**
		 * Create a copy of this ValueHolder: that is, an independent
		 * ValueHolder instance with the same contents.
		 */
		public ValueHolder copy() {
			ValueHolder copy = new ValueHolder(this.value, this.type, this.name);
			copy.setSource(this.source);
			return copy;
		}
	}

}
