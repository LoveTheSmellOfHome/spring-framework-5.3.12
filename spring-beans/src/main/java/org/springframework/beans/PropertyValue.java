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

package org.springframework.beans;

import java.io.Serializable;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * Object to hold information and value for an individual bean property.
 * Using an object here, rather than just storing all properties in
 * a map keyed by property name, allows for more flexibility, and the
 * ability to handle indexed properties etc in an optimized way.
 *
 * <p>Note that the value doesn't need to be the final required type:
 * A {@link BeanWrapper} implementation should handle any necessary conversion,
 * as this object doesn't know anything about the objects it will be applied to.
 *
 * @author Rod Johnson
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 13 May 2001
 * @see PropertyValues
 * @see BeanWrapper
 */
// 用于保存单个 bean 属性的信息和值的对象。在这里使用对象，而不是仅仅将所有属性存储在以属性名称为键的映射中，可以提供更大的灵活性，
// 并能够以优化的方式处理索引属性等。
// <p>请注意，该值不需要是最终所需的类型：{@link BeanWrapper} 实现应处理任何必要的转换，因为此对象不知道将应用到的对象的任何信息。
@SuppressWarnings("serial")
public class PropertyValue extends BeanMetadataAttributeAccessor implements Serializable {

	// 不变的名称，属性的名称
	private final String name;

	// 不变的值，指属性的原始值
	@Nullable
	private final Object value;

	// 是否可选的，默认是不可选的
	private boolean optional = false;

	// 是否需要转换，默认不需要转换
	private boolean converted = false;

	// 转换后的值
	@Nullable
	private Object convertedValue;

	/** Package-visible field that indicates whether conversion is necessary. */
	// 指示是否需要转换，比如 String 类型就不需要转换
	@Nullable
	volatile Boolean conversionNecessary;

	/** Package-visible field for caching the resolved property path tokens. */
	// 用于缓存解析的属性路径标记的包可见字段
	@Nullable
	transient volatile Object resolvedTokens;


	/**
	 * Create a new PropertyValue instance.
	 * @param name the name of the property (never {@code null})
	 * @param value the value of the property (possibly before type conversion)
	 */
	// 创建一个新的 PropertyValue 实例
	// @param name 属性的名称（从不{@code null}）
	// @param value 属性的值（可能在类型转换之前）
	public PropertyValue(String name, @Nullable Object value) {
		Assert.notNull(name, "Name must not be null");
		this.name = name;
		this.value = value;
	}

	/**
	 * Copy constructor.
	 * @param original the PropertyValue to copy (never {@code null})
	 */
	// 复制构造函数
	// @param 原始要复制的 PropertyValue（从不{@code null}）
	public PropertyValue(PropertyValue original) {
		Assert.notNull(original, "Original must not be null");
		this.name = original.getName();
		this.value = original.getValue();
		this.optional = original.isOptional();
		this.converted = original.converted;
		this.convertedValue = original.convertedValue;
		this.conversionNecessary = original.conversionNecessary;
		this.resolvedTokens = original.resolvedTokens;
		setSource(original.getSource());
		copyAttributesFrom(original);
	}

	/**
	 * Constructor that exposes a new value for an original value holder.
	 * The original holder will be exposed as source of the new holder.
	 * @param original the PropertyValue to link to (never {@code null})
	 * @param newValue the new value to apply
	 */
	// 为原始值持有者公开新值的构造函数。原始持有人将作为新持有人的来源公开
	// @param 原始要链接到的 PropertyValue（从不{@code null}）
	// @param newValue 要应用的新值
	public PropertyValue(PropertyValue original, @Nullable Object newValue) {
		Assert.notNull(original, "Original must not be null");
		this.name = original.getName();
		this.value = newValue;
		this.optional = original.isOptional();
		this.conversionNecessary = original.conversionNecessary;
		this.resolvedTokens = original.resolvedTokens;
		setSource(original);
		copyAttributesFrom(original);
	}


	/**
	 * Return the name of the property.
	 */
	// 返回属性的名称
	public String getName() {
		return this.name;
	}

	/**
	 * Return the value of the property.
	 * <p>Note that type conversion will <i>not</i> have occurred here.
	 * It is the responsibility of the BeanWrapper implementation to
	 * perform type conversion.
	 */
	// 返回属性的值。 <p>请注意，这里将<i>不会<i>发生类型转换。执行类型转换是 BeanWrapper 实现的责任。
	@Nullable
	public Object getValue() {
		return this.value;
	}

	/**
	 * Return the original PropertyValue instance for this value holder.
	 * @return the original PropertyValue (either a source of this
	 * value holder or this value holder itself).
	 */
	// 返回此值持有者的原始 PropertyValue 实例。
	// @return 原始 PropertyValue（此值持有者的来源或此值持有者本身
	public PropertyValue getOriginalPropertyValue() {
		PropertyValue original = this;
		Object source = getSource();
		while (source instanceof PropertyValue && source != original) {
			original = (PropertyValue) source;
			source = original.getSource();
		}
		return original;
	}

	/**
	 * Set whether this is an optional value, that is, to be ignored
	 * when no corresponding property exists on the target class.
	 * @since 3.0
	 */
	// 设置是否为可选值，即目标类不存在对应属性时忽略。
	public void setOptional(boolean optional) {
		this.optional = optional;
	}

	/**
	 * Return whether this is an optional value, that is, to be ignored
	 * when no corresponding property exists on the target class.
	 * @since 3.0
	 */
	// 返回是否为可选值，即当目标类不存在对应属性时忽略。
	public boolean isOptional() {
		return this.optional;
	}

	/**
	 * Return whether this holder contains a converted value already ({@code true}),
	 * or whether the value still needs to be converted ({@code false}).
	 */
	// 返回此持有者是否已包含转换值（{@code true}），或者该值是否仍需要转换（{@code false}）
	public synchronized boolean isConverted() {
		return this.converted;
	}

	/**
	 * Set the converted value of this property value,
	 * after processed type conversion.
	 */
	// 设置该属性值的转换值，经过类型转换
	public synchronized void setConvertedValue(@Nullable Object value) {
		this.converted = true;
		this.convertedValue = value;
	}

	/**
	 * Return the converted value of this property value,
	 * after processed type conversion.
	 */
	// 处理类型转换后，返回该属性值的转换值
	@Nullable
	public synchronized Object getConvertedValue() {
		return this.convertedValue;
	}


	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof PropertyValue)) {
			return false;
		}
		PropertyValue otherPv = (PropertyValue) other;
		return (this.name.equals(otherPv.name) &&
				ObjectUtils.nullSafeEquals(this.value, otherPv.value) &&
				ObjectUtils.nullSafeEquals(getSource(), otherPv.getSource()));
	}

	@Override
	public int hashCode() {
		return this.name.hashCode() * 29 + ObjectUtils.nullSafeHashCode(this.value);
	}

	@Override
	public String toString() {
		return "bean property '" + this.name + "'";
	}

}
