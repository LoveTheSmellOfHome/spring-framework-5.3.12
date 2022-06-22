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

package org.springframework.aop.framework.autoproxy;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.lang.Nullable;

/**
 * {@code BeanPostProcessor} implementation that creates AOP proxies based on all
 * candidate {@code Advisor}s in the current {@code BeanFactory}. This class is
 * completely generic; it contains no special code to handle any particular aspects,
 * such as pooling aspects.
 *
 * <p>It's possible to filter out advisors - for example, to use multiple post processors
 * of this type in the same factory - by setting the {@code usePrefix} property to true,
 * in which case only advisors beginning with the DefaultAdvisorAutoProxyCreator's bean
 * name followed by a dot (like "aapc.") will be used. This default prefix can be changed
 * from the bean name by setting the {@code advisorBeanNamePrefix} property.
 * The separator (.) will also be used in this case.
 *
 * @author Rod Johnson
 * @author Rob Harrop
 */
// BeanPostProcessor实现基于当前 BeanFactory 中的所有候选 Advisor(符合条件的 PointcutAdvisor#getPointcut,
// 并做出相应动作 Advice)创建 AOP 代理。 这个类是完全通用的；它不包含处理任何特定方面的特殊代码，例如池方面。
//
// 可以过滤掉 advisors 顾问 — 例如，在同一个工厂中使用多个这种类型的后处理器 — 通过将 usePrefix 属性
// 设置为 true，在这种情况下，只有以 DefaultAdvisorAutoProxyCreator 的 bean 名称后跟一个
// 点开头的 advisors 顾问（如“aapc.“） 将会被使用。 通过设置 advisorBeanNamePrefix 属性，
// 可以从 bean 名称更改此默认前缀。 在这种情况下也将使用分隔符 (.)。
//
// 自动代理第一种实现：默认实现
@SuppressWarnings("serial")
public class DefaultAdvisorAutoProxyCreator extends AbstractAdvisorAutoProxyCreator implements BeanNameAware {

	/** Separator between prefix and remainder of bean name. */
	// bean 名称的前缀和其余部分之间的分隔符
	public static final String SEPARATOR = ".";

	// 默认不使用前缀
	private boolean usePrefix = false;

	@Nullable
	private String advisorBeanNamePrefix;


	/**
	 * Set whether to only include advisors with a certain prefix in the bean name.
	 * <p>Default is {@code false}, including all beans of type {@code Advisor}.
	 * @see #setAdvisorBeanNamePrefix
	 */
	// 设置是否仅在 bean 名称中包含具有特定前缀的顾问。
	// 默认为 false ，包括 Advisor 类型的所有 bean
	public void setUsePrefix(boolean usePrefix) {
		this.usePrefix = usePrefix;
	}

	/**
	 * Return whether to only include advisors with a certain prefix in the bean name.
	 */
	// 返回是否仅在 bean 名称中包含具有特定前缀的顾问
	public boolean isUsePrefix() {
		return this.usePrefix;
	}

	/**
	 * Set the prefix for bean names that will cause them to be included for
	 * auto-proxying by this object. This prefix should be set to avoid circular
	 * references. Default value is the bean name of this object + a dot.
	 * @param advisorBeanNamePrefix the exclusion prefix
	 */
	// 设置 bean 名称的前缀，这将导致它们被包含在此对象的自动代理中。 应设置此前缀以避免循环引用。 默认值为该对象的 bean 名称 + 一个点。
	// 参形：
	//			advisorBeanNamePrefix – 排除前缀
	public void setAdvisorBeanNamePrefix(@Nullable String advisorBeanNamePrefix) {
		this.advisorBeanNamePrefix = advisorBeanNamePrefix;
	}

	/**
	 * Return the prefix for bean names that will cause them to be included
	 * for auto-proxying by this object.
	 */
	// 返回 bean 名称的前缀，这将导致它们被包含在此对象的自动代理中
	@Nullable
	public String getAdvisorBeanNamePrefix() {
		return this.advisorBeanNamePrefix;
	}

	@Override
	public void setBeanName(String name) {
		// If no infrastructure bean name prefix has been set, override it.
		// 如果没有设置基础设施 bean 名称前缀，则覆盖它
		if (this.advisorBeanNamePrefix == null) {
			this.advisorBeanNamePrefix = name + SEPARATOR;
		}
	}


	/**
	 * Consider {@code Advisor} beans with the specified prefix as eligible, if activated.
	 * @see #setUsePrefix
	 * @see #setAdvisorBeanNamePrefix
	 */
	// 将具有指定前缀的 Advisor bean 视为合格（如果已激活）
	// 判断是否是合格的 Advisor bean
	@Override
	protected boolean isEligibleAdvisorBean(String beanName) {
		if (!isUsePrefix()) {
			// 不使用前缀返回 true
			return true;
		}
		String prefix = getAdvisorBeanNamePrefix();
		return (prefix != null && beanName.startsWith(prefix));
	}

}
