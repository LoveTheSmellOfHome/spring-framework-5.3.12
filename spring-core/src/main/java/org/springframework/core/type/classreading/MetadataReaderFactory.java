/*
 * Copyright 2002-2012 the original author or authors.
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

import org.springframework.core.io.Resource;

import java.io.IOException;

/**
 * Factory interface for {@link MetadataReader} instances.
 * Allows for caching a MetadataReader per original resource.
 *
 * @author Juergen Hoeller
 * @since 2.5
 * @see SimpleMetadataReaderFactory
 * @see CachingMetadataReaderFactory
 */
// MetadataReader 实例的工厂接口。允许为每个原始资源缓存一个 MetadataReader
// 资源 --- 类元信息
// 在我们的 Java 文件里边，所有的文件，比如 Jar 包里边，它会有一些目录层次，目录就是我们的包名，包下就会有各种各样的 .class
// 文件，我们称之为类文件，或者字节码文件，字节码文件变成 Java 里边的 Class,或者变成 Java 里边的类对象，在 JVM 中通过解析
// 验证，加载等操作进行处理，将一个静态资源变成动态对象，尽管 Class 对象通常来说是不会变化的，不过在运行的时候，我们认为它是个
// 不变的对象，这个对象在处理过程中，自然而然就会有一些元信息，比如反射中它的类名，它的方法，构造器，字段，这些统称为元信息。
// Annotation 它是标注在 target 上，比如在类上，我们就需要通过元信息中类信息去找到注解，这里边类信息称为类元信息，注解称为
// 注解元信息，方法称为方法元信息。类元信息有个{@link org.springframework.core.type.ClassMetaData},注解元信息
// {@link org.springframework.core.type.AnnotationMetadata}
//
// 类元信息提供了两种标准实现：
// ASM 实现：{@link org.springframework.core.type.classreading.ClassMetadataReadingVisitor}
// 反射实现：{@link org.springframework.core.type.StandardAnnotationMetadata}
//
// 注解元信息提供了两种标准实现：
// {@link org.springframework.core.type.classreading.AnnotationMetadataReadingVisitor}
// {@link org.springframework.core.type.StandardAnnotationMetadata}
public interface MetadataReaderFactory {

	/**
	 * Obtain a MetadataReader for the given class name.
	 * @param className the class name (to be resolved to a ".class" file)
	 * @return a holder for the ClassReader instance (never {@code null})
	 * @throws IOException in case of I/O failure
	 */
	// 获取给定类名的 MetadataReader
	// @param className 类名（要解析为“.class”文件）
	// @return ClassReader 实例的持有者（从不{@code null}）
	MetadataReader getMetadataReader(String className) throws IOException;

	/**
	 * Obtain a MetadataReader for the given resource.
	 * @param resource the resource (pointing to a ".class" file)
	 * @return a holder for the ClassReader instance (never {@code null})
	 * @throws IOException in case of I/O failure
	 */
	// 获取给定资源的 MetadataReader
	// @param resource 资源（指向“.class”文件）
	// @return ClassReader 实例的持有者（从不{@code null}）
	MetadataReader getMetadataReader(Resource resource) throws IOException;

}
