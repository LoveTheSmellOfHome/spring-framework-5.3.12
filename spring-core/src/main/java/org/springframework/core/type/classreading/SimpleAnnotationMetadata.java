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

package org.springframework.core.type.classreading;

import org.springframework.asm.Opcodes;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.MethodMetadata;
import org.springframework.lang.Nullable;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * {@link AnnotationMetadata} created from a
 * {@link SimpleAnnotationMetadataReadingVisitor}.
 *
 * @author Phillip Webb
 * @author Sam Brannen
 * @since 5.2
 */
// 通过 ASM 字节码提升实现
// java 反射 和 ASM 的区别：
// 1.反射是读取持久堆上存储的类信息。而 ASM 是直接处理 .class 字节码的小工具（工具虽小，但是功能非常强大！）
// 3.反射对性能的开销比较大，反射读取类信息时需要进行类加载处理，而 ASM 则不需要将类加载到内存中。
// 4.反射相对于 ASM 来说使用方便，想直接操纵 ASM 的话需要有 JVM 指令基础
final class SimpleAnnotationMetadata implements AnnotationMetadata {

	private final String className;

	private final int access;

	@Nullable
	private final String enclosingClassName;

	@Nullable
	private final String superClassName;

	private final boolean independentInnerClass;

	private final String[] interfaceNames;

	private final String[] memberClassNames;

	private final MethodMetadata[] annotatedMethods;

	private final MergedAnnotations annotations;

	@Nullable
	private Set<String> annotationTypes;


	SimpleAnnotationMetadata(String className, int access, @Nullable String enclosingClassName,
			@Nullable String superClassName, boolean independentInnerClass, String[] interfaceNames,
			String[] memberClassNames, MethodMetadata[] annotatedMethods, MergedAnnotations annotations) {

		this.className = className;
		this.access = access;
		this.enclosingClassName = enclosingClassName;
		this.superClassName = superClassName;
		this.independentInnerClass = independentInnerClass;
		this.interfaceNames = interfaceNames;
		this.memberClassNames = memberClassNames;
		this.annotatedMethods = annotatedMethods;
		this.annotations = annotations;
	}

	@Override
	public String getClassName() {
		return this.className;
	}

	@Override
	public boolean isInterface() {
		return (this.access & Opcodes.ACC_INTERFACE) != 0;
	}

	@Override
	public boolean isAnnotation() {
		return (this.access & Opcodes.ACC_ANNOTATION) != 0;
	}

	@Override
	public boolean isAbstract() {
		return (this.access & Opcodes.ACC_ABSTRACT) != 0;
	}

	@Override
	public boolean isFinal() {
		return (this.access & Opcodes.ACC_FINAL) != 0;
	}

	@Override
	public boolean isIndependent() {
		return (this.enclosingClassName == null || this.independentInnerClass);
	}

	@Override
	@Nullable
	public String getEnclosingClassName() {
		return this.enclosingClassName;
	}

	@Override
	@Nullable
	public String getSuperClassName() {
		return this.superClassName;
	}

	@Override
	public String[] getInterfaceNames() {
		return this.interfaceNames.clone();
	}

	@Override
	public String[] getMemberClassNames() {
		return this.memberClassNames.clone();
	}

	// 通过 ASM 直接从字节码中获取 String 相关的信息来填充，不需要加载，速度很快。但是在 Spring 5 中这种方式被舍弃了，
	// 通过 @Indexed 注解在编译时就确定了相关的信息。会更快
	@Override
	public Set<String> getAnnotationTypes() {
		Set<String> annotationTypes = this.annotationTypes;
		if (annotationTypes == null) {
			annotationTypes = Collections.unmodifiableSet(
					AnnotationMetadata.super.getAnnotationTypes());
			this.annotationTypes = annotationTypes;
		}
		return annotationTypes;
	}

	@Override
	public Set<MethodMetadata> getAnnotatedMethods(String annotationName) {
		Set<MethodMetadata> annotatedMethods = null;
		for (MethodMetadata annotatedMethod : this.annotatedMethods) {
			if (annotatedMethod.isAnnotated(annotationName)) {
				if (annotatedMethods == null) {
					annotatedMethods = new LinkedHashSet<>(4);
				}
				annotatedMethods.add(annotatedMethod);
			}
		}
		return annotatedMethods != null ? annotatedMethods : Collections.emptySet();
	}

	@Override
	public MergedAnnotations getAnnotations() {
		return this.annotations;
	}

	@Override
	public boolean equals(@Nullable Object obj) {
		return ((this == obj) || ((obj instanceof SimpleAnnotationMetadata) &&
				this.className.equals(((SimpleAnnotationMetadata) obj).className)));
	}

	@Override
	public int hashCode() {
		return this.className.hashCode();
	}

	@Override
	public String toString() {
		return this.className;
	}

}
