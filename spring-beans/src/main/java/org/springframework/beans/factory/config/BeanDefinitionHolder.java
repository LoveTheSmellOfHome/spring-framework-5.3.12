/*
 * Copyright 2002-2017 the original author or authors.
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

import org.springframework.beans.BeanMetadataElement;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Holder for a BeanDefinition with name and aliases.
 * Can be registered as a placeholder for an inner bean.
 *
 * <p>Can also be used for programmatic registration of inner bean
 * definitions. If you don't care about BeanNameAware and the like,
 * registering RootBeanDefinition or ChildBeanDefinition is good enough.
 *
 * @author Juergen Hoeller
 * @since 1.0.2
 * @see org.springframework.beans.factory.BeanNameAware
 * @see org.springframework.beans.factory.support.RootBeanDefinition
 * @see org.springframework.beans.factory.support.ChildBeanDefinition
 */
// 带有名称和别名的 BeanDefinition 的持有者。可以注册为内部 bean 的占位符。
//
// <p>也可用于内部 bean 定义的编程注册。如果你不关心 BeanNameAware 之类的，注册 RootBeanDefinition 或 ChildBeanDefinition 就足够了
public class BeanDefinitionHolder implements BeanMetadataElement {

	private final BeanDefinition beanDefinition;

	private final String beanName;

	@Nullable
	private final String[] aliases;


	/**
	 * Create a new BeanDefinitionHolder.
	 * @param beanDefinition the BeanDefinition to wrap
	 * @param beanName the name of the bean, as specified for the bean definition
	 */
	public BeanDefinitionHolder(BeanDefinition beanDefinition, String beanName) {
		this(beanDefinition, beanName, null);
	}

	/**
	 * Create a new BeanDefinitionHolder.
	 * @param beanDefinition the BeanDefinition to wrap
	 * @param beanName the name of the bean, as specified for the bean definition
	 * @param aliases alias names for the bean, or {@code null} if none
	 */
	public BeanDefinitionHolder(BeanDefinition beanDefinition, String beanName, @Nullable String[] aliases) {
		Assert.notNull(beanDefinition, "BeanDefinition must not be null");
		Assert.notNull(beanName, "Bean name must not be null");
		this.beanDefinition = beanDefinition;
		this.beanName = beanName;
		this.aliases = aliases;
	}

	/**
	 * Copy constructor: Create a new BeanDefinitionHolder with the
	 * same contents as the given BeanDefinitionHolder instance.
	 * <p>Note: The wrapped BeanDefinition reference is taken as-is;
	 * it is {@code not} deeply copied.
	 * @param beanDefinitionHolder the BeanDefinitionHolder to copy
	 */
	public BeanDefinitionHolder(BeanDefinitionHolder beanDefinitionHolder) {
		Assert.notNull(beanDefinitionHolder, "BeanDefinitionHolder must not be null");
		this.beanDefinition = beanDefinitionHolder.getBeanDefinition();
		this.beanName = beanDefinitionHolder.getBeanName();
		this.aliases = beanDefinitionHolder.getAliases();
	}


	/**
	 * Return the wrapped BeanDefinition.
	 */
	public BeanDefinition getBeanDefinition() {
		return this.beanDefinition;
	}

	/**
	 * Return the primary name of the bean, as specified for the bean definition.
	 */
	// 返回 bean 的主要名称，如为 bean 定义指定的。
	public String getBeanName() {
		return this.beanName;
	}

	/**
	 * Return the alias names for the bean, as specified directly for the bean definition.
	 * @return the array of alias names, or {@code null} if none
	 */
	// 返回 bean 的别名，如为 bean 定义直接指定的那样。
	// @return 别名数组，或者 {@code null} 如果没有
	@Nullable
	public String[] getAliases() {
		return this.aliases;
	}

	/**
	 * Expose the bean definition's source object.
	 * @see BeanDefinition#getSource()
	 */
	// 公开 bean 定义的源对象。
	@Override
	@Nullable
	public Object getSource() {
		return this.beanDefinition.getSource();
	}

	/**
	 * Determine whether the given candidate name matches the bean name
	 * or the aliases stored in this bean definition.
	 */
	// 确定给定的候选名称是否与存储在此 bean 定义中的 bean 名称或别名匹配
	public boolean matchesName(@Nullable String candidateName) {
		return (candidateName != null && (candidateName.equals(this.beanName) ||
				candidateName.equals(BeanFactoryUtils.transformedBeanName(this.beanName)) ||
				ObjectUtils.containsElement(this.aliases, candidateName)));
	}


	/**
	 * Return a friendly, short description for the bean, stating name and aliases.
	 * @see #getBeanName()
	 * @see #getAliases()
	 */
	// 为 bean 返回一个友好的简短描述，说明名称和别名
	public String getShortDescription() {
		if (this.aliases == null) {
			return "Bean definition with name '" + this.beanName + "'";
		}
		return "Bean definition with name '" + this.beanName + "' and aliases [" + StringUtils.arrayToCommaDelimitedString(this.aliases) + ']';
	}

	/**
	 * Return a long description for the bean, including name and aliases
	 * as well as a description of the contained {@link BeanDefinition}.
	 * @see #getShortDescription()
	 * @see #getBeanDefinition()
	 */
	// 返回 bean 的详细描述，包括名称和别名以及包含的 {@link BeanDefinition} 的描述
	public String getLongDescription() {
		return getShortDescription() + ": " + this.beanDefinition;
	}

	/**
	 * This implementation returns the long description. Can be overridden
	 * to return the short description or any kind of custom description instead.
	 * @see #getLongDescription()
	 * @see #getShortDescription()
	 */
	// 此实现返回长描述。可以覆盖以返回简短描述或任何类型的自定义描述。
	@Override
	public String toString() {
		return getLongDescription();
	}


	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof BeanDefinitionHolder)) {
			return false;
		}
		BeanDefinitionHolder otherHolder = (BeanDefinitionHolder) other;
		return this.beanDefinition.equals(otherHolder.beanDefinition) &&
				this.beanName.equals(otherHolder.beanName) &&
				ObjectUtils.nullSafeEquals(this.aliases, otherHolder.aliases);
	}

	@Override
	public int hashCode() {
		int hashCode = this.beanDefinition.hashCode();
		hashCode = 29 * hashCode + this.beanName.hashCode();
		hashCode = 29 * hashCode + ObjectUtils.nullSafeHashCode(this.aliases);
		return hashCode;
	}

}
