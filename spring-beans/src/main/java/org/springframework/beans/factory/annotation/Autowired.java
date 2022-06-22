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

package org.springframework.beans.factory.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a constructor, field, setter method, or config method as to be autowired by
 * Spring's dependency injection facilities. This is an alternative to the JSR-330
 * {@link javax.inject.Inject} annotation, adding required-vs-optional semantics.
 *
 * <h3>Autowired Constructors</h3>
 * <p>Only one constructor of any given bean class may declare this annotation with the
 * {@link #required} attribute set to {@code true}, indicating <i>the</i> constructor
 * to autowire when used as a Spring bean. Furthermore, if the {@code required}
 * attribute is set to {@code true}, only a single constructor may be annotated
 * with {@code @Autowired}. If multiple <i>non-required</i> constructors declare the
 * annotation, they will be considered as candidates for autowiring. The constructor
 * with the greatest number of dependencies that can be satisfied by matching beans
 * in the Spring container will be chosen. If none of the candidates can be satisfied,
 * then a primary/default constructor (if present) will be used. Similarly, if a
 * class declares multiple constructors but none of them is annotated with
 * {@code @Autowired}, then a primary/default constructor (if present) will be used.
 * If a class only declares a single constructor to begin with, it will always be used,
 * even if not annotated. An annotated constructor does not have to be public.
 *
 * <h3>Autowired Fields</h3>
 * <p>Fields are injected right after construction of a bean, before any config methods
 * are invoked. Such a config field does not have to be public.
 *
 * <h3>Autowired Methods</h3>
 * <p>Config methods may have an arbitrary name and any number of arguments; each of
 * those arguments will be autowired with a matching bean in the Spring container.
 * Bean property setter methods are effectively just a special case of such a general
 * config method. Such config methods do not have to be public.
 *
 * <h3>Autowired Parameters</h3>
 * <p>Although {@code @Autowired} can technically be declared on individual method
 * or constructor parameters since Spring Framework 5.0, most parts of the
 * framework ignore such declarations. The only part of the core Spring Framework
 * that actively supports autowired parameters is the JUnit Jupiter support in
 * the {@code spring-test} module (see the
 * <a href="https://docs.spring.io/spring/docs/current/spring-framework-reference/testing.html#testcontext-junit-jupiter-di">TestContext framework</a>
 * reference documentation for details).
 *
 * <h3>Multiple Arguments and 'required' Semantics</h3>
 * <p>In the case of a multi-arg constructor or method, the {@link #required} attribute
 * is applicable to all arguments. Individual parameters may be declared as Java-8 style
 * {@link java.util.Optional} or, as of Spring Framework 5.0, also as {@code @Nullable}
 * or a not-null parameter type in Kotlin, overriding the base 'required' semantics.
 *
 * <h3>Autowiring Arrays, Collections, and Maps</h3>
 * <p>In case of an array, {@link java.util.Collection}, or {@link java.util.Map}
 * dependency type, the container autowires all beans matching the declared value
 * type. For such purposes, the map keys must be declared as type {@code String}
 * which will be resolved to the corresponding bean names. Such a container-provided
 * collection will be ordered, taking into account
 * {@link org.springframework.core.Ordered Ordered} and
 * {@link org.springframework.core.annotation.Order @Order} values of the target
 * components, otherwise following their registration order in the container.
 * Alternatively, a single matching target bean may also be a generally typed
 * {@code Collection} or {@code Map} itself, getting injected as such.
 *
 * <h3>Not supported in {@code BeanPostProcessor} or {@code BeanFactoryPostProcessor}</h3>
 * <p>Note that actual injection is performed through a
 * {@link org.springframework.beans.factory.config.BeanPostProcessor
 * BeanPostProcessor} which in turn means that you <em>cannot</em>
 * use {@code @Autowired} to inject references into
 * {@link org.springframework.beans.factory.config.BeanPostProcessor
 * BeanPostProcessor} or
 * {@link org.springframework.beans.factory.config.BeanFactoryPostProcessor BeanFactoryPostProcessor}
 * types. Please consult the javadoc for the {@link AutowiredAnnotationBeanPostProcessor}
 * class (which, by default, checks for the presence of this annotation).
 *
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @author Sam Brannen
 * @since 2.5
 * @see AutowiredAnnotationBeanPostProcessor
 * @see Qualifier
 * @see Value
 */
// 将构造函数、字段、setter 方法或配置方法标记为由 Spring 的依赖注入工具自动装配。
// 这是 JSR-330 {@link javax.inject.Inject} 注解的替代方法，添加了 required-vs-optional 语义。
//
// <h3>Autowired Constructors<h3> @Autowired 标注构造函数,自动装配构造函数
// <p>任何给定 bean 类的只有一个构造函数可以声明此注解，并将 {@link required} 属性设置为 {@code true}，指示
// <i><i> 构造函数自动装配当用作 Spring bean 时。此外，如果 {@code required} 属性设置为 {@code true}，
// 则只能使用 {@code @Autowired} 标注单个构造函数。
// 如果多个 <i>non-required<i> 构造函数声明了 @Autowired 注解，它们将被视为自动装配的候选者。
// 将选择通过匹配 Spring 容器中的 bean 可以满足的依赖项数量最多的构造函数。如果没有一个候选者能被满足，
// 那么将使用一个主默认构造函数（如果存在）。类似地，如果一个类声明了多个构造函数，但没有一个用 {@code @Autowired} 注解，
// 则将使用主默认构造函数（如果存在）。如果一个类只声明一个构造函数开始，它将始终被使用，即使没有注解。
// 带注释的构造函数不必是公共的。
//
// <h3>Autowired Fields<h3> @Autowired 标注属性，自动装配属性
// <p>在构建 bean 之后，在调用任何配置方法之前，立即注入字段。这样的配置字段不必是 public。
//
// <h3>Autowired Methods<h3> @Autowired 标注方法，自动装配方法，实际上是自动装配方法参数。
// <p>配置方法可以有任意名称和任意数量的参数；这些参数中的每一个都将使用 Spring 容器中的匹配 bean 自动装配。
// Bean 属性 setter 方法实际上只是这种通用配置方法的一个特例。这样的配置方法不必是公开的。
//
// <h3>Autowired 参数<h3>
// <p>虽然从 Spring Framework 5.0 开始，{@code @Autowired} 技术上可以在独立的方法或构造函数参数上声明，
// 但框架的大部分部分都忽略了这样的声明。核心 Spring 框架中唯一主动支持自动装配参数的部分是 {@code spring-test}
// 模块中的 JUnit Jupiter 支持（参见 <a href="https:docs.spring.iospringdocscurrentspring-framework-referencetesting.
// htmltestcontext- junit-jupiter-di">TestContext framework<a> 参考文档了解详情）。
//
// <h3>Multiple Arguments and 'required' Semantics<h3> 多参数和“必需”语义
// <p>在多参数构造函数或方法的情况下，{@link required} 属性适用于所有参数。
// 单个参数可以声明为 Java-8 样式 {@link java.util.Optional} 或者，从 Spring Framework 5.0 开始，
// 也可以声明为 {@code @Nullable} 或 Kotlin 中的非空参数类型，覆盖基本的 'required ' 语义。
//
// <h3>Autowiring Arrays, Collections, and Maps<h3>自动装配数组、集合和映射
// <p>如果是数组、{@link java.util.Collection} 或 {@link java.util.Map} 依赖类型，容器会自动装配所有 bean匹配声明的值类型。
// 为此，必须将映射 key 声明为 {@code String} 类型，该类型将被解析为相应的 bean 名称。这样一个容器提供的集合将被排序，
// 考虑到目标组件的 {@link org.springframework.core.Ordered Ordered} 和
// {@link org.springframework.core.annotation.Order @Order} 值，否则按照它们的容器中的注册顺序。
// 或者，单个匹配的目标 bean 也可能是一个普通类型的 {@code Collection} 或 {@code Map} 本身，这样被注入。
//
// <h3>不支持 {@code BeanPostProcessor} 或 {@code BeanFactoryPostProcessor}<h3>
// <p>注意，实际注入是通过 {@link org.springframework.beans.factory.config.BeanPostProcessor BeanPostProcessor} 执行的
// 意味着您<em>不能<em>使用 {@code @Autowired} 将引用注入到 {@link org.springframework.beans.factory.config.
// BeanPostProcessor BeanPostProcessor} 或 {@link org.springframework.beans.factory.config .
// BeanFactoryPostProcessor BeanFactoryPostProcessor} 类型。
// 请查阅 {@link AutowiredAnnotationBeanPostProcessor} 类的 javadoc（默认情况下，它会检查此注释的存在）。
@Target({ElementType.CONSTRUCTOR, ElementType.METHOD, ElementType.PARAMETER, ElementType.FIELD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Autowired {

	/**
	 * Declares whether the annotated dependency is required.
	 * <p>Defaults to {@code true}.
	 */
	// 声明注解依赖是否是必须的，<p>默认为 {@code true}。
	boolean required() default true;

}
