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

package org.springframework.stereotype;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that an annotated class is a "component".
 * Such classes are considered as candidates for auto-detection
 * when using annotation-based configuration and classpath scanning.
 *
 * <p>Other class-level annotations may be considered as identifying
 * a component as well, typically a special kind of component:
 * e.g. the {@link Repository @Repository} annotation or AspectJ's
 * {@link org.aspectj.lang.annotation.Aspect @Aspect} annotation.
 *
 * @author Mark Fisher
 * @since 2.5
 * @see Repository
 * @see Service
 * @see Controller
 * @see org.springframework.context.annotation.ClassPathBeanDefinitionScanner
 */
// 表示带注解的类是“组件”。在使用基于注解的配置和类路径扫描时，此类类被视为自动检测的候选对象。组件是应用组成的一部分
// 在注解 2.0 时代并没有提供 @ComponentScan 注解
//
// <p>其他类级别的注解也可以被视为标识组件，通常是一种特殊类型的组件：例如{@link Repository @Repository} 注解或 AspectJ 的
// {@link org.aspectj.lang.annotation.Aspect @Aspect} 注解。
//
// 1.它是个组件 2.它是个范式(模式 stereotype annotation)注解
//
// @Component “派⽣性”原理：
// 核心组件：org.springframework.context.annotation.ClassPathBeanDefinitionScanner
//		org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
// 资源处理：org.springframework.core.io.support.ResourcePatternResolver 通配符资源路径加载器
// 资源-类元信息：org.springframework.core.type.classreading.MetadataReaderFactory
// 类元信息： org.springframework.core.type.ClassMetadata
// 		ASM 实现 - org.springframework.core.type.classreading.ClassMetadataReadingVisitor
//		反射实现 - org.springframework.core.type.StandardAnnotationMetadata
// 注解元信息 - org.springframework.core.type.AnnotationMetadata
//		ASM 实现 - org.springframework.core.type.classreading.AnnotationMetadataReadingVisitor
//		反射实现 - org.springframework.core.type.StandardAnnotationMetadata
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Indexed
public @interface Component {

	/**
	 * The value may indicate a suggestion for a logical component name,
	 * to be turned into a Spring bean in case of an autodetected component.
	 * @return the suggested component name, if any (or empty String otherwise)
	 */
	// 该值可能指示对逻辑组件名称的建议，在自动检测到的组件的情况下将其转换为 Spring bean。
	// @return 建议的组件名称，如果有的话（否则为空字符串）
	String value() default "";

}
