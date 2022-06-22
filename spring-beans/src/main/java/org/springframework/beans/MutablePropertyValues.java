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

package org.springframework.beans;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;

import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

/**
 * The default implementation of the {@link PropertyValues} interface.
 * Allows simple manipulation of properties, and provides constructors
 * to support deep copy and construction from a Map.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @since 13 May 2001
 */
// {@link PropertyValues} 接口的默认实现。允许对属性进行简单操作，并提供构造函数以支持从 Map 进行深度复制和构造
@SuppressWarnings("serial")
public class MutablePropertyValues implements PropertyValues, Serializable {

	// 使用 list 方式动态添加或删除 PropertyValue
	// MutablePropertyValues 在 Bean 初始化时候用运到的，即 BeanDefinition 初始化的时候运用到的，
	// 这时候是有线程安全保护的,因此这里的 List 只是普通list,不需要加特殊的线程安全保护。与 MutablePropertySources
	// 形成了鲜明对比
	private final List<PropertyValue> propertyValueList;

	@Nullable
	private Set<String> processedProperties;

	private volatile boolean converted;


	/**
	 * Creates a new empty MutablePropertyValues object.
	 * <p>Property values can be added with the {@code add} method.
	 * @see #add(String, Object)
	 */
	// 创建一个新的空 MutablePropertyValues 对象。 <p>可以使用 {@code add} 方法添加属性值
	public MutablePropertyValues() {
		this.propertyValueList = new ArrayList<>(0);
	}

	/**
	 * Deep copy constructor. Guarantees PropertyValue references
	 * are independent, although it can't deep copy objects currently
	 * referenced by individual PropertyValue objects.
	 * @param original the PropertyValues to copy
	 * @see #addPropertyValues(PropertyValues)
	 */
	// 深拷贝构造函数。保证 PropertyValue 引用是独立的，尽管它不能深复制当前由单个 PropertyValue 对象引用的对象
	public MutablePropertyValues(@Nullable PropertyValues original) {
		// We can optimize this because it's all new:
		// There is no replacement of existing property values.
		// 我们可以优化它，因为它是全新的：没有替换现有的属性值
		if (original != null) {
			PropertyValue[] pvs = original.getPropertyValues();
			this.propertyValueList = new ArrayList<>(pvs.length);
			for (PropertyValue pv : pvs) {
				this.propertyValueList.add(new PropertyValue(pv));
			}
		}
		else {
			this.propertyValueList = new ArrayList<>(0);
		}
	}

	/**
	 * Construct a new MutablePropertyValues object from a Map.
	 * @param original a Map with property values keyed by property name Strings
	 * @see #addPropertyValues(Map)
	 */
	// 从 Map 构造一个新的 MutablePropertyValues 对象
	// @param original 一个 Map，其属性值以属性名称字符串为键
	public MutablePropertyValues(@Nullable Map<?, ?> original) {
		// We can optimize this because it's all new:
		// There is no replacement of existing property values.
		// 我们可以优化它，因为它是全新的：没有替换现有的属性值
		if (original != null) {
			this.propertyValueList = new ArrayList<>(original.size());
			original.forEach((attrName, attrValue) -> this.propertyValueList.add(
					new PropertyValue(attrName.toString(), attrValue)));
		}
		else {
			this.propertyValueList = new ArrayList<>(0);
		}
	}

	/**
	 * Construct a new MutablePropertyValues object using the given List of
	 * PropertyValue objects as-is.
	 * <p>This is a constructor for advanced usage scenarios.
	 * It is not intended for typical programmatic use.
	 * @param propertyValueList a List of PropertyValue objects
	 */
	// 使用给定的 PropertyValue 对象列表按原样构造一个新的 MutablePropertyValues 对象。
	// <p>这是一个高级使用场景的构造函数。它不适用于典型的编程用途。
	// @param propertyValueList 一个 PropertyValue 对象列表
	public MutablePropertyValues(@Nullable List<PropertyValue> propertyValueList) {
		this.propertyValueList =
				(propertyValueList != null ? propertyValueList : new ArrayList<>());
	}


	/**
	 * Return the underlying List of PropertyValue objects in its raw form.
	 * The returned List can be modified directly, although this is not recommended.
	 * <p>This is an accessor for optimized access to all PropertyValue objects.
	 * It is not intended for typical programmatic use.
	 */
	// 以原始形式返回底层的 PropertyValue 对象列表。返回的 List 可以直接修改，虽然不推荐这样做。
	// <p>这是一个用于优化访问所有 PropertyValue 对象的访问器。它不适用于典型的编程用途。
	public List<PropertyValue> getPropertyValueList() {
		return this.propertyValueList;
	}

	/**
	 * Return the number of PropertyValue entries in the list.
	 */
	// 返回列表中 PropertyValue 条目的数量。
	public int size() {
		return this.propertyValueList.size();
	}

	/**
	 * Copy all given PropertyValues into this object. Guarantees PropertyValue
	 * references are independent, although it can't deep copy objects currently
	 * referenced by individual PropertyValue objects.
	 * @param other the PropertyValues to copy
	 * @return this in order to allow for adding multiple property values in a chain
	 */
	// 将所有给定的 PropertyValues 复制到此对象中。保证 PropertyValue 引用是独立的，
	// 尽管它不能深复制当前由单个 PropertyValue 对象引用的对象
	// @param 其他要复制的 PropertyValues
	// @return 这是为了允许在链中添加多个属性值
	public MutablePropertyValues addPropertyValues(@Nullable PropertyValues other) {
		if (other != null) {
			PropertyValue[] pvs = other.getPropertyValues();
			for (PropertyValue pv : pvs) {
				addPropertyValue(new PropertyValue(pv));
			}
		}
		return this;
	}

	/**
	 * Add all property values from the given Map.
	 * @param other a Map with property values keyed by property name,
	 * which must be a String
	 * @return this in order to allow for adding multiple property values in a chain
	 */
	// 添加给定 Map 中的所有属性值
	// @param other 具有以属性名称为键的属性值的 Map，它必须是一个字符串
	// @return 这是为了允许在链中添加多个属性值
	public MutablePropertyValues addPropertyValues(@Nullable Map<?, ?> other) {
		if (other != null) {
			other.forEach((attrName, attrValue) -> addPropertyValue(
					new PropertyValue(attrName.toString(), attrValue)));
		}
		return this;
	}

	/**
	 * Add a PropertyValue object, replacing any existing one for the
	 * corresponding property or getting merged with it (if applicable).
	 * @param pv the PropertyValue object to add
	 * @return this in order to allow for adding multiple property values in a chain
	 */
	// 添加一个 PropertyValue 对象，替换相应属性的任何现有对象或与其合并（如果适用）
	// @param pv 要添加的 PropertyValue 对象
	// @return 这是为了允许在链中添加多个属性值
	public MutablePropertyValues addPropertyValue(PropertyValue pv) {
		for (int i = 0; i < this.propertyValueList.size(); i++) {
			PropertyValue currentPv = this.propertyValueList.get(i);
			if (currentPv.getName().equals(pv.getName())) {
				pv = mergeIfRequired(pv, currentPv);
				setPropertyValueAt(pv, i);
				return this;
			}
		}
		this.propertyValueList.add(pv);
		return this;
	}

	/**
	 * Overloaded version of {@code addPropertyValue} that takes
	 * a property name and a property value.
	 * <p>Note: As of Spring 3.0, we recommend using the more concise
	 * and chaining-capable variant {@link #add}.
	 * @param propertyName name of the property
	 * @param propertyValue value of the property
	 * @see #addPropertyValue(PropertyValue)
	 */
	// {@code addPropertyValue} 的重载版本，它采用属性名称和属性值。
	// <p>注意：从 Spring 3.0 开始，我们建议使用更简洁和可链接的变体 {@link add}。
	// @param propertyName 属性名称
	// @param propertyValue 属性的值
	public void addPropertyValue(String propertyName, Object propertyValue) {
		addPropertyValue(new PropertyValue(propertyName, propertyValue));
	}

	/**
	 * Add a PropertyValue object, replacing any existing one for the
	 * corresponding property or getting merged with it (if applicable).
	 * @param propertyName name of the property
	 * @param propertyValue value of the property
	 * @return this in order to allow for adding multiple property values in a chain
	 */
	// 添加一个 PropertyValue 对象，替换相应属性的任何现有对象或与其合并（如果适用）
	public MutablePropertyValues add(String propertyName, @Nullable Object propertyValue) {
		addPropertyValue(new PropertyValue(propertyName, propertyValue));
		return this;
	}

	/**
	 * Modify a PropertyValue object held in this object.
	 * Indexed from 0.
	 */
	// 修改此对象中保存的 PropertyValue 对象。从 0 开始索引
	public void setPropertyValueAt(PropertyValue pv, int i) {
		this.propertyValueList.set(i, pv);
	}

	/**
	 * Merges the value of the supplied 'new' {@link PropertyValue} with that of
	 * the current {@link PropertyValue} if merging is supported and enabled.
	 * @see Mergeable
	 */
	// 如果支持并启用合并，则将提供的“新”{@link PropertyValue} 的值与当前 {@link PropertyValue} 的值合并
	private PropertyValue mergeIfRequired(PropertyValue newPv, PropertyValue currentPv) {
		Object value = newPv.getValue();
		if (value instanceof Mergeable) {
			Mergeable mergeable = (Mergeable) value;
			if (mergeable.isMergeEnabled()) {
				Object merged = mergeable.merge(currentPv.getValue());
				return new PropertyValue(newPv.getName(), merged);
			}
		}
		return newPv;
	}

	/**
	 * Remove the given PropertyValue, if contained.
	 * @param pv the PropertyValue to remove
	 */
	// 删除给定的属性值（如果包含）。 @param pv 要删除的属性值
	public void removePropertyValue(PropertyValue pv) {
		this.propertyValueList.remove(pv);
	}

	/**
	 * Overloaded version of {@code removePropertyValue} that takes a property name.
	 * @param propertyName name of the property
	 * @see #removePropertyValue(PropertyValue)
	 */
	// 采用属性名称的 {@code removePropertyValue} 的重载版本
	// @param propertyName 属性名称
	public void removePropertyValue(String propertyName) {
		this.propertyValueList.remove(getPropertyValue(propertyName));
	}


	@Override
	public Iterator<PropertyValue> iterator() {
		return Collections.unmodifiableList(this.propertyValueList).iterator();
	}

	@Override
	public Spliterator<PropertyValue> spliterator() {
		return Spliterators.spliterator(this.propertyValueList, 0);
	}

	@Override
	public Stream<PropertyValue> stream() {
		return this.propertyValueList.stream();
	}

	@Override
	public PropertyValue[] getPropertyValues() {
		return this.propertyValueList.toArray(new PropertyValue[0]);
	}

	@Override
	@Nullable
	public PropertyValue getPropertyValue(String propertyName) {
		for (PropertyValue pv : this.propertyValueList) {
			if (pv.getName().equals(propertyName)) {
				return pv;
			}
		}
		return null;
	}

	/**
	 * Get the raw property value, if any.
	 * @param propertyName the name to search for
	 * @return the raw property value, or {@code null} if none found
	 * @since 4.0
	 * @see #getPropertyValue(String)
	 * @see PropertyValue#getValue()
	 */
	// 获取原始属性值（如果有）。
	// @param propertyName 要搜索的名称
	// @return 原始属性值，或者 {@code null} 如果没有找到
	@Nullable
	public Object get(String propertyName) {
		PropertyValue pv = getPropertyValue(propertyName);
		return (pv != null ? pv.getValue() : null);
	}

	@Override
	public PropertyValues changesSince(PropertyValues old) {
		MutablePropertyValues changes = new MutablePropertyValues();
		if (old == this) {
			return changes;
		}

		// for each property value in the new set
		// 对于新集合中的每个属性值
		for (PropertyValue newPv : this.propertyValueList) {
			// if there wasn't an old one, add it
			PropertyValue pvOld = old.getPropertyValue(newPv.getName());
			if (pvOld == null || !pvOld.equals(newPv)) {
				changes.addPropertyValue(newPv);
			}
		}
		return changes;
	}

	@Override
	public boolean contains(String propertyName) {
		return (getPropertyValue(propertyName) != null ||
				(this.processedProperties != null && this.processedProperties.contains(propertyName)));
	}

	@Override
	public boolean isEmpty() {
		return this.propertyValueList.isEmpty();
	}


	/**
	 * Register the specified property as "processed" in the sense
	 * of some processor calling the corresponding setter method
	 * outside of the PropertyValue(s) mechanism.
	 * <p>This will lead to {@code true} being returned from
	 * a {@link #contains} call for the specified property.
	 * @param propertyName the name of the property.
	 */
	// 在某些处理器调用 PropertyValue(s) 机制之外的相应 setter 方法的意义上，将指定的属性注册为“已处理”。
	// <p>这将导致 {@code true} 从对指定属性的 {@link contains} 调用返回
	public void registerProcessedProperty(String propertyName) {
		if (this.processedProperties == null) {
			this.processedProperties = new HashSet<>(4);
		}
		this.processedProperties.add(propertyName);
	}

	/**
	 * Clear the "processed" registration of the given property, if any.
	 * @since 3.2.13
	 */
	public void clearProcessedProperty(String propertyName) {
		if (this.processedProperties != null) {
			this.processedProperties.remove(propertyName);
		}
	}

	/**
	 * Mark this holder as containing converted values only
	 * (i.e. no runtime resolution needed anymore).
	 */
	// 将此持有者标记为仅包含转换值（即不再需要运行时分辨率）。
	public void setConverted() {
		this.converted = true;
	}

	/**
	 * Return whether this holder contains converted values only ({@code true}),
	 * or whether the values still need to be converted ({@code false}).
	 */
	// 返回此持有者是否仅包含转换后的值 ({@code true})，
	// 或者值是否仍需要转换 ({@code false})。
	public boolean isConverted() {
		return this.converted;
	}


	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other || (other instanceof MutablePropertyValues &&
				this.propertyValueList.equals(((MutablePropertyValues) other).propertyValueList)));
	}

	@Override
	public int hashCode() {
		return this.propertyValueList.hashCode();
	}

	@Override
	public String toString() {
		PropertyValue[] pvs = getPropertyValues();
		if (pvs.length > 0) {
			return "PropertyValues: length=" + pvs.length + "; " + StringUtils.arrayToDelimitedString(pvs, "; ");
		}
		return "PropertyValues: length=0";
	}

}
