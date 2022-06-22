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

package org.springframework.context.annotation;

import org.springframework.beans.factory.parsing.Location;
import org.springframework.beans.factory.parsing.Problem;
import org.springframework.beans.factory.parsing.ProblemReporter;
import org.springframework.beans.factory.support.BeanDefinitionReader;
import org.springframework.core.io.DescriptiveResource;
import org.springframework.core.io.Resource;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import java.util.*;

/**
 * Represents a user-defined {@link Configuration @Configuration} class.
 * <p>Includes a set of {@link Bean} methods, including all such methods
 * defined in the ancestry of the class, in a 'flattened-out' manner.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author Phillip Webb
 * @since 3.0
 * @see BeanMethod
 * @see ConfigurationClassParser
 */
// 表示用户定义的 {@link Configuration @Configuration} 类。
// <p>包括一组 {@link Bean} 方法，包括在类的祖先中以“扁平化”方式定义的所有此类方法。
//
// 凡是配置类都会被包装成 ConfigurationClass，里边封装了一些注解元信息，资源等供内部使用。
// 供 ConfigurationClassParser 调用封装。
final class ConfigurationClass {

	// 注解
	private final AnnotationMetadata metadata;

	// 只读资源
	private final Resource resource;

	// bean 名称
	@Nullable
	private String beanName;

	private final Set<ConfigurationClass> importedBy = new LinkedHashSet<>(1);

	private final Set<BeanMethod> beanMethods = new LinkedHashSet<>();

	private final Map<String, Class<? extends BeanDefinitionReader>> importedResources =
			new LinkedHashMap<>();

	private final Map<ImportBeanDefinitionRegistrar, AnnotationMetadata> importBeanDefinitionRegistrars =
			new LinkedHashMap<>();

	final Set<String> skippedBeanMethods = new HashSet<>();


	/**
	 * Create a new {@link ConfigurationClass} with the given name.
	 * @param metadataReader reader used to parse the underlying {@link Class}
	 * @param beanName must not be {@code null}
	 * @see ConfigurationClass#ConfigurationClass(Class, ConfigurationClass)
	 */
	// 使用给定的名称创建一个新的 {@link ConfigurationClass}。
	// @param metadataReader 用于解析底层 {@link Class} 的 reader
	//  @param beanName 不得为 {@code null}
	ConfigurationClass(MetadataReader metadataReader, String beanName) {
		Assert.notNull(beanName, "Bean name must not be null");
		this.metadata = metadataReader.getAnnotationMetadata();
		this.resource = metadataReader.getResource();
		this.beanName = beanName;
	}

	/**
	 * Create a new {@link ConfigurationClass} representing a class that was imported
	 * using the {@link Import} annotation or automatically processed as a nested
	 * configuration class (if importedBy is not {@code null}).
	 * @param metadataReader reader used to parse the underlying {@link Class}
	 * @param importedBy the configuration class importing this one or {@code null}
	 * @since 3.1.1
	 */
	// 创建一个新的 {@link ConfigurationClass} 表示使用 {@link Import} 注解导入或作为嵌套配置类自动处理的类
	// （如果 importBy 不是 {@code null}）。
	ConfigurationClass(MetadataReader metadataReader, @Nullable ConfigurationClass importedBy) {
		this.metadata = metadataReader.getAnnotationMetadata();
		this.resource = metadataReader.getResource();
		this.importedBy.add(importedBy);
	}

	/**
	 * Create a new {@link ConfigurationClass} with the given name.
	 * @param clazz the underlying {@link Class} to represent
	 * @param beanName name of the {@code @Configuration} class bean
	 * @see ConfigurationClass#ConfigurationClass(Class, ConfigurationClass)
	 */
	// 使用给定的名称创建一个新的 {@link ConfigurationClass}。
	// @param clazz 要表示的底层 {@link Class}
	//  @param beanName {@code @Configuration} 类 bean 的名称
	ConfigurationClass(Class<?> clazz, String beanName) {
		Assert.notNull(beanName, "Bean name must not be null");
		this.metadata = AnnotationMetadata.introspect(clazz);
		this.resource = new DescriptiveResource(clazz.getName());
		this.beanName = beanName;
	}

	/**
	 * Create a new {@link ConfigurationClass} representing a class that was imported
	 * using the {@link Import} annotation or automatically processed as a nested
	 * configuration class (if imported is {@code true}).
	 * @param clazz the underlying {@link Class} to represent
	 * @param importedBy the configuration class importing this one (or {@code null})
	 * @since 3.1.1
	 */
	// 创建一个新的 {@link ConfigurationClass}，表示使用 {@link Import} 注解导入或作为
	// 嵌套配置类自动处理的类（如果导入的是 {@code true}）。
	// @param clazz 要表示的底层 {@link Class}
	// @param importedBy 导入这个的配置类（或 {@code null}）
	ConfigurationClass(Class<?> clazz, @Nullable ConfigurationClass importedBy) {
		this.metadata = AnnotationMetadata.introspect(clazz);
		this.resource = new DescriptiveResource(clazz.getName());
		this.importedBy.add(importedBy);
	}

	/**
	 * Create a new {@link ConfigurationClass} with the given name.
	 * @param metadata the metadata for the underlying class to represent
	 * @param beanName name of the {@code @Configuration} class bean
	 * @see ConfigurationClass#ConfigurationClass(Class, ConfigurationClass)
	 */
	// 使用给定的名称创建一个新的 {@link ConfigurationClass}
	// @param metadata 底层类要表示的元数据
	// @param beanName {@code @Configuration} 类 bean 的名称
	ConfigurationClass(AnnotationMetadata metadata, String beanName) {
		Assert.notNull(beanName, "Bean name must not be null");
		this.metadata = metadata;
		this.resource = new DescriptiveResource(metadata.getClassName());
		this.beanName = beanName;
	}


	AnnotationMetadata getMetadata() {
		// 获取注解
		return this.metadata;
	}

	Resource getResource() {
		return this.resource;
	}

	String getSimpleName() {
		return ClassUtils.getShortName(getMetadata().getClassName());
	}

	void setBeanName(String beanName) {
		this.beanName = beanName;
	}

	@Nullable
	public String getBeanName() {
		return this.beanName;
	}

	/**
	 * Return whether this configuration class was registered via @{@link Import} or
	 * automatically registered due to being nested within another configuration class.
	 * @since 3.1.1
	 * @see #getImportedBy()
	 */
	// 返回此配置类是通过@{@link Import} 注册的还是由于嵌套在另一个配置类中而自动注册的。
	public boolean isImported() {
		return !this.importedBy.isEmpty();
	}

	/**
	 * Merge the imported-by declarations from the given configuration class into this one.
	 * @since 4.0.5
	 */
	// 将给定配置类中的 import-by 声明合并到这个声明中
	void mergeImportedBy(ConfigurationClass otherConfigClass) {
		this.importedBy.addAll(otherConfigClass.importedBy);
	}

	/**
	 * Return the configuration classes that imported this class,
	 * or an empty Set if this configuration was not imported.
	 * @since 4.0.5
	 * @see #isImported()
	 */
	// 返回导入此类的配置类，如果未导入此配置，则返回空 Set
	Set<ConfigurationClass> getImportedBy() {
		return this.importedBy;
	}

	void addBeanMethod(BeanMethod method) {
		this.beanMethods.add(method);
	}

	Set<BeanMethod> getBeanMethods() {
		return this.beanMethods;
	}

	void addImportedResource(String importedResource, Class<? extends BeanDefinitionReader> readerClass) {
		this.importedResources.put(importedResource, readerClass);
	}

	void addImportBeanDefinitionRegistrar(ImportBeanDefinitionRegistrar registrar, AnnotationMetadata importingClassMetadata) {
		this.importBeanDefinitionRegistrars.put(registrar, importingClassMetadata);
	}

	Map<ImportBeanDefinitionRegistrar, AnnotationMetadata> getImportBeanDefinitionRegistrars() {
		return this.importBeanDefinitionRegistrars;
	}

	Map<String, Class<? extends BeanDefinitionReader>> getImportedResources() {
		return this.importedResources;
	}

	void validate(ProblemReporter problemReporter) {
		// A configuration class may not be final (CGLIB limitation) unless it declares proxyBeanMethods=false
		// 配置类可能不是最终的（CGLIB 限制），除非它声明了 proxyBeanMethods=false
		Map<String, Object> attributes = this.metadata.getAnnotationAttributes(Configuration.class.getName());
		if (attributes != null && (Boolean) attributes.get("proxyBeanMethods")) {
			if (this.metadata.isFinal()) {
				problemReporter.error(new FinalConfigurationProblem());
			}
			for (BeanMethod beanMethod : this.beanMethods) {
				beanMethod.validate(problemReporter);
			}
		}
	}

	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other || (other instanceof ConfigurationClass &&
				getMetadata().getClassName().equals(((ConfigurationClass) other).getMetadata().getClassName())));
	}

	@Override
	public int hashCode() {
		return getMetadata().getClassName().hashCode();
	}

	@Override
	public String toString() {
		return "ConfigurationClass: beanName '" + this.beanName + "', " + this.resource;
	}


	/**
	 * Configuration classes must be non-final to accommodate CGLIB subclassing.
	 */
	// 配置类必须是非最终的以适应 CGLIB 子类化。
	private class FinalConfigurationProblem extends Problem {

		FinalConfigurationProblem() {
			super(String.format("@Configuration class '%s' may not be final. Remove the final modifier to continue.",
					getSimpleName()), new Location(getResource(), getMetadata()));
		}
	}

}
