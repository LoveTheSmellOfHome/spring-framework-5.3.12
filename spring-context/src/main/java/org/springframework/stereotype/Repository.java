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

import org.springframework.core.annotation.AliasFor;

/**
 * Indicates that an annotated class is a "Repository", originally defined by
 * Domain-Driven Design (Evans, 2003) as "a mechanism for encapsulating storage,
 * retrieval, and search behavior which emulates a collection of objects".
 *
 * <p>Teams implementing traditional Java EE patterns such as "Data Access Object"
 * may also apply this stereotype to DAO classes, though care should be taken to
 * understand the distinction between Data Access Object and DDD-style repositories
 * before doing so. This annotation is a general-purpose stereotype and individual teams
 * may narrow their semantics and use as appropriate.
 *
 * <p>A class thus annotated is eligible for Spring
 * {@link org.springframework.dao.DataAccessException DataAccessException} translation
 * when used in conjunction with a {@link
 * org.springframework.dao.annotation.PersistenceExceptionTranslationPostProcessor
 * PersistenceExceptionTranslationPostProcessor}. The annotated class is also clarified as
 * to its role in the overall application architecture for the purpose of tooling,
 * aspects, etc.
 *
 * <p>As of Spring 2.5, this annotation also serves as a specialization of
 * {@link Component @Component}, allowing for implementation classes to be autodetected
 * through classpath scanning.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 2.0
 * @see Component
 * @see Service
 * @see org.springframework.dao.DataAccessException
 * @see org.springframework.dao.annotation.PersistenceExceptionTranslationPostProcessor
 */
// 表示带注释的类是“数据仓储”，最初由领域驱动设计 (Evans, 2003) 定义为“一种用于封装存储、检索和搜索行为的机制，它模拟对象集合”。
//
// 实现传统 Java EE 模式（例如“数据访问对象”）的团队也可以将此模型应用于 DAO 类，但在执行此操作之前应注意理解数据访问对象
// 和 DDD 样式存储库之间的区别。此注释是通用的范式注解，个别团队可能会缩小其语义范围并酌情使用。
//
// 当与 PersistenceExceptionTranslationPostProcessor 结合使用时，这样注释的类有资格进行 Spring DataAccessException 转换。
// 出于工具、方面等的目的，还阐明了带注释的类在整个应用程序架构中的作用。
//
// 从 Spring 2.5 开始，这个注解也作为 @Component 的派生，允许通过类路径扫描自动检测实现类。
// {@link SpringBootConfiguration}
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface Repository {

	/**
	 * The value may indicate a suggestion for a logical component name,
	 * to be turned into a Spring bean in case of an autodetected component.
	 * @return the suggested component name, if any (or empty String otherwise)
	 */
	@AliasFor(annotation = Component.class)
	String value() default "";

}
