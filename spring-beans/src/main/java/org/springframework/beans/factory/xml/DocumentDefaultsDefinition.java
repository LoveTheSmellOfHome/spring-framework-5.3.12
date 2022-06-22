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

package org.springframework.beans.factory.xml;

import org.springframework.beans.factory.parsing.DefaultsDefinition;
import org.springframework.lang.Nullable;

/**
 * Simple JavaBean that holds the defaults specified at the {@code <beans>}
 * level in a standard Spring XML bean definition document:
 * {@code default-lazy-init}, {@code default-autowire}, etc.
 *
 * @author Juergen Hoeller
 * @since 2.0.2
 */
// 保存在标准 Spring XML bean 定义文档中 {@code <beans>} 级别指定的默认值的简单 JavaBean：
// {@code default-lazy-init}、{@code default-autowire} 等。
public class DocumentDefaultsDefinition implements DefaultsDefinition {

	@Nullable
	private String lazyInit;

	@Nullable
	private String merge;

	@Nullable
	private String autowire;

	@Nullable
	private String autowireCandidates;

	@Nullable
	private String initMethod;

	@Nullable
	private String destroyMethod;

	@Nullable
	private Object source;


	/**
	 * Set the default lazy-init flag for the document that's currently parsed.
	 */
	// 为当前解析的文档设置默认的延迟初始化标志
	public void setLazyInit(@Nullable String lazyInit) {
		this.lazyInit = lazyInit;
	}

	/**
	 * Return the default lazy-init flag for the document that's currently parsed.
	 */
	// 返回当前解析的文档的默认延迟初始化标志
	@Nullable
	public String getLazyInit() {
		return this.lazyInit;
	}

	/**
	 * Set the default merge setting for the document that's currently parsed.
	 */
	// 为当前解析的文档设置默认合并设置
	public void setMerge(@Nullable String merge) {
		this.merge = merge;
	}

	/**
	 * Return the default merge setting for the document that's currently parsed.
	 */
	// 返回当前解析的文档的默认合并设置
	@Nullable
	public String getMerge() {
		return this.merge;
	}

	/**
	 * Set the default autowire setting for the document that's currently parsed.
	 */
	// 为当前解析的文档设置默认的自动装配设置
	public void setAutowire(@Nullable String autowire) {
		this.autowire = autowire;
	}

	/**
	 * Return the default autowire setting for the document that's currently parsed.
	 */
	// 返回当前解析的文档的默认自动装配设置
	@Nullable
	public String getAutowire() {
		return this.autowire;
	}

	/**
	 * Set the default autowire-candidate pattern for the document that's currently parsed.
	 * Also accepts a comma-separated list of patterns.
	 */
	// 为当前解析的文档设置默认的 autowire-candidate 模式。还接受逗号分隔的模式列表
	public void setAutowireCandidates(@Nullable String autowireCandidates) {
		this.autowireCandidates = autowireCandidates;
	}

	/**
	 * Return the default autowire-candidate pattern for the document that's currently parsed.
	 * May also return a comma-separated list of patterns.
	 */
	// 返回当前解析的文档的默认自动连接候选模式。也可以返回以逗号分隔的模式列表
	@Nullable
	public String getAutowireCandidates() {
		return this.autowireCandidates;
	}

	/**
	 * Set the default init-method setting for the document that's currently parsed.
	 */
	// 为当前解析的文档设置默认的 init-method 设置
	public void setInitMethod(@Nullable String initMethod) {
		this.initMethod = initMethod;
	}

	/**
	 * Return the default init-method setting for the document that's currently parsed.
	 */
	// 返回当前解析的文档的默认初始化方法设置
	@Nullable
	public String getInitMethod() {
		return this.initMethod;
	}

	/**
	 * Set the default destroy-method setting for the document that's currently parsed.
	 */
	// 为当前解析的文档设置默认的 destroy-method 设置
	public void setDestroyMethod(@Nullable String destroyMethod) {
		this.destroyMethod = destroyMethod;
	}

	/**
	 * Return the default destroy-method setting for the document that's currently parsed.
	 */
	// 返回当前解析的文档的默认销毁方法设置
	@Nullable
	public String getDestroyMethod() {
		return this.destroyMethod;
	}

	/**
	 * Set the configuration source {@code Object} for this metadata element.
	 * <p>The exact type of the object will depend on the configuration mechanism used.
	 */
	// 为此元数据元素设置配置源 {@code Object}。 <p>对象的确切类型将取决于所使用的配置机制
	public void setSource(@Nullable Object source) {
		this.source = source;
	}

	@Override
	@Nullable
	public Object getSource() {
		return this.source;
	}

}
