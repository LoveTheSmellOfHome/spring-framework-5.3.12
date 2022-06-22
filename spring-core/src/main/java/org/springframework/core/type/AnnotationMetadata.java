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

package org.springframework.core.type;

import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.annotation.MergedAnnotations.SearchStrategy;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Interface that defines abstract access to the annotations of a specific
 * class, in a form that does not require that class to be loaded yet.
 *
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @author Phillip Webb
 * @author Sam Brannen
 * @since 2.5
 * @see StandardAnnotationMetadata
 * @see org.springframework.core.type.classreading.MetadataReader#getAnnotationMetadata()
 * @see AnnotatedTypeMetadata
 */
// 注解元信息：以不需要加载该类的形式定义，对特定类的注解，的抽象访问的接口，
// AnnotationMetadata 代表这些引导类(配置类)或类注解或方法注解
public interface AnnotationMetadata extends ClassMetadata, AnnotatedTypeMetadata {

	/**
	 * Get the fully qualified class names of all annotation types that
	 * are <em>present</em> on the underlying class.
	 * @return the annotation type names
	 */
	// 获取基础类上<em>标注<em> 的所有注解类型的完全限定类名称,
	// 参见 {@link java.lang.annotation.Annotation}#annotationType() 获取注解的 Class 类型
	// @return 注释类型名称
	default Set<String> getAnnotationTypes() {
		return getAnnotations().stream()
				.filter(MergedAnnotation::isDirectlyPresent)
				.map(annotation -> annotation.getType().getName())
				.collect(Collectors.toCollection(LinkedHashSet::new));
	}

	/**
	 * Get the fully qualified class names of all meta-annotation types that
	 * are <em>present</em> on the given annotation type on the underlying class.
	 * @param annotationName the fully qualified class name of the meta-annotation
	 * type to look for
	 * @return the meta-annotation type names, or an empty set if none found
	 */
	// 获取在底层类的给定注释类型上 <em>present 标注<em> 的所有元注释类型的完全限定类名
	// @param annotationName 要查找的元注解类型的完全限定类名
	// @return 元注解类型名称，如果没有找到则返回一个空集
	default Set<String> getMetaAnnotationTypes(String annotationName) {
		MergedAnnotation<?> annotation = getAnnotations().get(annotationName, MergedAnnotation::isDirectlyPresent);
		if (!annotation.isPresent()) {
			return Collections.emptySet();
		}
		return MergedAnnotations.from(annotation.getType(), SearchStrategy.INHERITED_ANNOTATIONS).stream()
				.map(mergedAnnotation -> mergedAnnotation.getType().getName())
				.collect(Collectors.toCollection(LinkedHashSet::new));
	}

	/**
	 * Determine whether an annotation of the given type is <em>present</em> on
	 * the underlying class.
	 * @param annotationName the fully qualified class name of the annotation
	 * type to look for
	 * @return {@code true} if a matching annotation is present
	 */
	// 确定给定类型的注释是否 <em>present 标注<em> 在基础类上
	// @param annotationName 要查找的注释类型的完全限定类名
	// @return {@code true} 如果存在匹配的注解
	default boolean hasAnnotation(String annotationName) {
		return getAnnotations().isDirectlyPresent(annotationName);
	}

	/**
	 * Determine whether the underlying class has an annotation that is itself
	 * annotated with the meta-annotation of the given type.
	 * @param metaAnnotationName the fully qualified class name of the
	 * meta-annotation type to look for
	 * @return {@code true} if a matching meta-annotation is present
	 */
	// 确定基础类是否有一个注解，该注解本身用给定类型的元注解进行了注解。
	// @param metaAnnotationName 要查找的元注释类型的完全限定类名
	// @return {@code true} 如果存在匹配的元注释
	default boolean hasMetaAnnotation(String metaAnnotationName) {
		return getAnnotations().get(metaAnnotationName,
				MergedAnnotation::isMetaPresent).isPresent();
	}

	/**
	 * Determine whether the underlying class has any methods that are
	 * annotated (or meta-annotated) with the given annotation type.
	 * @param annotationName the fully qualified class name of the annotation
	 * type to look for
	 */
	// 确定基础类是否具有使用给定注解类型注解（或元注解）的任何方法
	// @param annotationName 要查找的注解类型的完全限定类名
	default boolean hasAnnotatedMethods(String annotationName) {
		return !getAnnotatedMethods(annotationName).isEmpty();
	}

	/**
	 * Retrieve the method metadata for all methods that are annotated
	 * (or meta-annotated) with the given annotation type.
	 * <p>For any returned method, {@link MethodMetadata#isAnnotated} will
	 * return {@code true} for the given annotation type.
	 * @param annotationName the fully qualified class name of the annotation
	 * type to look for
	 * @return a set of {@link MethodMetadata} for methods that have a matching
	 * annotation. The return value will be an empty set if no methods match
	 * the annotation type.
	 */
	// 检索使用给定注解类型注解（或元注解）的所有方法方法的元数据集合（包含层次性，即父类中标注了相关注解的方法）。
	// <p>对于任何返回的方法，{@link MethodMetadataisAnnotated} 将为给定的注解类型返回 {@code true}。
	// @return 一组 {@link MethodMetadata} 用于具有匹配注解的方法。如果没有方法与注解类型匹配，则返回值将为空集。
	Set<MethodMetadata> getAnnotatedMethods(String annotationName);


	/**
	 * Factory method to create a new {@link AnnotationMetadata} instance
	 * for the given class using standard reflection.
	 * @param type the class to introspect
	 * @return a new {@link AnnotationMetadata} instance
	 * @since 5.2
	 */
	// 使用标准反射为给定类创建新的 {@link AnnotationMetadata} 实例的工厂方法。
	// @param type 反射相关的类
	// @return 一个新的 {@link AnnotationMetadata} 实例
	// 自省（透视）操作，类似于 X 光扫描，检查内部有什么元素，然后进行返回
	static AnnotationMetadata introspect(Class<?> type) {
		return StandardAnnotationMetadata.from(type);
	}

}
