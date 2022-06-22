/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.core.env;

import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.lang.Nullable;

/**
 * Configuration interface to be implemented by most if not all {@link PropertyResolver}
 * types. Provides facilities for accessing and customizing the
 * {@link org.springframework.core.convert.ConversionService ConversionService}
 * used when converting property values from one type to another.
 *
 * @author Chris Beams
 * @since 3.1
 */
// 由大多数 {@link PropertyResolver} 类型实现的配置接口
// 提供用于访问和自定义将属性值从一种类型转换为另一种类型时使用的
// {@link org.springframework.core.convert.ConversionService ConversionService} 的设施。
public interface ConfigurablePropertyResolver extends PropertyResolver {

	/**
	 * Return the {@link ConfigurableConversionService} used when performing type
	 * conversions on properties.
	 * <p>The configurable nature of the returned conversion service allows for
	 * the convenient addition and removal of individual {@code Converter} instances:
	 * <pre class="code">
	 * ConfigurableConversionService cs = env.getConversionService();
	 * cs.addConverter(new FooConverter());
	 * </pre>
	 * @see PropertyResolver#getProperty(String, Class)
	 * @see org.springframework.core.convert.converter.ConverterRegistry#addConverter
	 */
	// 返回对属性执行类型转换时使用的 {@link ConfigurableConversionService}。
	// 返回的转换服务的可配置特性允许方便地添加和删除单个 Converter 实例：
	// 				ConfigurableConversionService cs = env.getConversionService();
	//	   			cs.addConverter(new FooConverter());
	ConfigurableConversionService getConversionService();

	/**
	 * Set the {@link ConfigurableConversionService} to be used when performing type
	 * conversions on properties.
	 * <p><strong>Note:</strong> as an alternative to fully replacing the
	 * {@code ConversionService}, consider adding or removing individual
	 * {@code Converter} instances by drilling into {@link #getConversionService()}
	 * and calling methods such as {@code #addConverter}.
	 * @see PropertyResolver#getProperty(String, Class)
	 * @see #getConversionService()
	 * @see org.springframework.core.convert.converter.ConverterRegistry#addConverter
	 */
	// 设置在对属性执行类型转换时要使用的 {@link ConfigurableConversionService}
	// 注意：作为完全替换 ConversionService 的替代方法，请考虑通过钻取 #getConversionService() 并调用
	// 诸如 addConverter 之类的方法来添加或删除单个 Converter 实例
	void setConversionService(ConfigurableConversionService conversionService);

	/**
	 * Set the prefix that placeholders replaced by this resolver must begin with.
	 */
	// 设置此解析器替换的占位符必须以前缀开头
	void setPlaceholderPrefix(String placeholderPrefix);

	/**
	 * Set the suffix that placeholders replaced by this resolver must end with.
	 */
	// 设置此解析器替换的占位符必须以结尾的后缀
	void setPlaceholderSuffix(String placeholderSuffix);

	/**
	 * Specify the separating character between the placeholders replaced by this
	 * resolver and their associated default value, or {@code null} if no such
	 * special character should be processed as a value separator.
	 */
	// 指定由该解析器替换的占位符与其关联的默认值之间的分隔符，如果不应将此类特殊字符作为值分隔符处理，则指定为 null
	void setValueSeparator(@Nullable String valueSeparator);

	/**
	 * Set whether to throw an exception when encountering an unresolvable placeholder
	 * nested within the value of a given property. A {@code false} value indicates strict
	 * resolution, i.e. that an exception will be thrown. A {@code true} value indicates
	 * that unresolvable nested placeholders should be passed through in their unresolved
	 * ${...} form.
	 * <p>Implementations of {@link #getProperty(String)} and its variants must inspect
	 * the value set here to determine correct behavior when property values contain
	 * unresolvable placeholders.
	 * @since 3.2
	 */
	// 设置在遇到嵌套在给定属性值中的无法解析的占位符时是否抛出异常。 false 值表示严格解析，即会抛出异常。
	// true 值表示不可解析的嵌套占位符应以其未解析的 ${...} 形式传递。
	void setIgnoreUnresolvableNestedPlaceholders(boolean ignoreUnresolvableNestedPlaceholders);

	/**
	 * Specify which properties must be present, to be verified by
	 * {@link #validateRequiredProperties()}.
	 */
	// 指定必须存在的属性，以通过 {@link #validateRequiredProperties()}. 进行验证
	void setRequiredProperties(String... requiredProperties);

	/**
	 * Validate that each of the properties specified by
	 * {@link #setRequiredProperties} is present and resolves to a
	 * non-{@code null} value.
	 * @throws MissingRequiredPropertiesException if any of the required
	 * properties are not resolvable.
	 */
	// 验证 {@link #setRequiredProperties} 指定的每个属性是否存在并解析为非空值
	// 如果任何必需的属性不可解析，则@throws MissingRequiredPropertiesException。
	void validateRequiredProperties() throws MissingRequiredPropertiesException;

}
