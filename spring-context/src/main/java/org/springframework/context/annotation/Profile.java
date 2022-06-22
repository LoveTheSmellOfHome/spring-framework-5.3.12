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

package org.springframework.context.annotation;

import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Profiles;

import java.lang.annotation.*;

/**
 * Indicates that a component is eligible for registration when one or more
 * {@linkplain #value specified profiles} are active.
 *
 * <p>A <em>profile</em> is a named logical grouping that may be activated
 * programmatically via {@link ConfigurableEnvironment#setActiveProfiles} or declaratively
 * by setting the {@link AbstractEnvironment#ACTIVE_PROFILES_PROPERTY_NAME
 * spring.profiles.active} property as a JVM system property, as an
 * environment variable, or as a Servlet context parameter in {@code web.xml}
 * for web applications. Profiles may also be activated declaratively in
 * integration tests via the {@code @ActiveProfiles} annotation.
 *
 * <p>The {@code @Profile} annotation may be used in any of the following ways:
 * <ul>
 * <li>as a type-level annotation on any class directly or indirectly annotated with
 * {@code @Component}, including {@link Configuration @Configuration} classes</li>
 * <li>as a meta-annotation, for the purpose of composing custom stereotype annotations</li>
 * <li>as a method-level annotation on any {@link Bean @Bean} method</li>
 * </ul>
 *
 * <p>If a {@code @Configuration} class is marked with {@code @Profile}, all of the
 * {@code @Bean} methods and {@link Import @Import} annotations associated with that class
 * will be bypassed unless one or more of the specified profiles are active. A profile
 * string may contain a simple profile name (for example {@code "p1"}) or a profile
 * expression. A profile expression allows for more complicated profile logic to be
 * expressed, for example {@code "p1 & p2"}. See {@link Profiles#of(String...)} for more
 * details about supported formats.
 *
 * <p>This is analogous to the behavior in Spring XML: if the {@code profile} attribute of
 * the {@code beans} element is supplied e.g., {@code <beans profile="p1,p2">}, the
 * {@code beans} element will not be parsed unless at least profile 'p1' or 'p2' has been
 * activated. Likewise, if a {@code @Component} or {@code @Configuration} class is marked
 * with {@code @Profile({"p1", "p2"})}, that class will not be registered or processed unless
 * at least profile 'p1' or 'p2' has been activated.
 *
 * <p>If a given profile is prefixed with the NOT operator ({@code !}), the annotated
 * component will be registered if the profile is <em>not</em> active &mdash; for example,
 * given {@code @Profile({"p1", "!p2"})}, registration will occur if profile 'p1' is active
 * or if profile 'p2' is <em>not</em> active.
 *
 * <p>If the {@code @Profile} annotation is omitted, registration will occur regardless
 * of which (if any) profiles are active.
 *
 * <p><b>NOTE:</b> With {@code @Profile} on {@code @Bean} methods, a special scenario may
 * apply: In the case of overloaded {@code @Bean} methods of the same Java method name
 * (analogous to constructor overloading), an {@code @Profile} condition needs to be
 * consistently declared on all overloaded methods. If the conditions are inconsistent,
 * only the condition on the first declaration among the overloaded methods will matter.
 * {@code @Profile} can therefore not be used to select an overloaded method with a
 * particular argument signature over another; resolution between all factory methods
 * for the same bean follows Spring's constructor resolution algorithm at creation time.
 * <b>Use distinct Java method names pointing to the same {@link Bean#name bean name}
 * if you'd like to define alternative beans with different profile conditions</b>;
 * see {@code ProfileDatabaseConfig} in {@link Configuration @Configuration}'s javadoc.
 *
 * <p>When defining Spring beans via XML, the {@code "profile"} attribute of the
 * {@code <beans>} element may be used. See the documentation in the
 * {@code spring-beans} XSD (version 3.1 or greater) for details.
 *
 * @author Chris Beams
 * @author Phillip Webb
 * @author Sam Brannen
 * @since 3.1
 * @see ConfigurableEnvironment#setActiveProfiles
 * @see ConfigurableEnvironment#setDefaultProfiles
 * @see AbstractEnvironment#ACTIVE_PROFILES_PROPERTY_NAME
 * @see AbstractEnvironment#DEFAULT_PROFILES_PROPERTY_NAME
 * @see Conditional
 * @see org.springframework.test.context.ActiveProfiles
 */
// 当一个或多个 {@linkplain value specified Profile} 处于活动状态时，表示组件有资格注册。
//
// 配置文件是一个命名的逻辑分组，可以通过 ConfigurableEnvironment.setActiveProfiles 以编程方式激活，
// 也可以通过将 spring.profiles.active 属性设置为 JVM 系统属性、环境变量或 web.xml 中的 Servlet 上下文参数
// 以声明方式激活 web应用程序。配置文件也可以通过@ActiveProfiles 注解在集成测试中以声明方式激活。
//
// @Profile 注解可以通过以下任何一种方式使用：
// 作为任何直接或间接用@Component 注解的类的类型级注解，包括@Configuration 类
// 作为元注解，用于编写自定义构造型注解
// 作为任何@Bean 方法的方法级注解
//
// 如果@Configuration 类用@Profile 标记，则与该类关联的所有@Bean 方法和@Import 注解都将被绕过，
// 除非一个或多个指定的配置文件处于活动状态。配置文件字符串可能包含简单的配置文件名称（例如“p1”）或配置文件表达式。
// 配置文件表达式允许表达更复杂的配置文件逻辑，例如“p1 & p2”。有关支持的格式的更多详细信息，请参阅 Profiles.of(String...)。
//
// 这类似于 Spring XML 中的行为：如果提供了 beans 元素的 profile 属性，例如，则不会解析 beans 元素，
// 除非至少激活了 profile 'p1' 或 'p2'。同样，如果@Component 或@Configuration 类用@Profile({"p1", "p2"}) 标记，
// 则除非至少激活了配置文件“p1”或“p2”，否则不会注册或处理该类。
//
// 如果给定的配置文件以 NOT 运算符 (!) 为前缀，如果配置文件未处于活动状态，则将注册带注解的组件——例如，给定@Profile({"p1", "!p2"})，
// 如果配置文件“p1”处于活动状态，或者配置文件“p2”未处于活动状态。
//
// 如果省略@Profile 注解，则无论哪个（如果有）配置文件处于活动状态，都会进行注册
//
// 注意：在@Bean 方法上使用@Profile 时，可能会出现一种特殊情况：
// 在重载相同Java 方法名称的@Bean 方法的情况下（类似于构造函数重载），需要在所有重载方法上一致地声明@Profile 条件.
// 如果条件不一致，则只有重载方法中第一个声明的条件才重要。
// @Profile 因此不能用于选择具有特定参数签名的重载方法而不是另一个方法；同一个 bean 的所有工厂方法之间的解析在创建时遵循 Spring
// 的构造函数解析算法。如果您想定义具有不同配置文件条件的替代 bean，请使用指向相同 bean 名称的不同 Java 方法名称；
// 请参阅@Configuration 的 javadoc 中的 ProfileDatabaseConfig。
//
// 当通过 XML 定义 Spring bean 时，可以使用元素的“profile”属性。有关详细信息，
// 请参阅 spring-beans XSD（版本 3.1 或更高版本）中的文档
//
// 语义：通过配置的方式来指定某种隔离环境，比如生产环境，测试环境，线上环境等
// 基于配置条件注解：Spring 条件注解 @Profile @since 3.1
// 				  关联对象----{@link org.springframework.core.env.Environment} 中的 Profiles
// 				  实现变化：从 Spring 4.0 开始，@Profile 基于 @Conditional 实现
// Spring 3.1 条件配置 API - ConfigurableEnvironment
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Conditional(ProfileCondition.class)
public @interface Profile {

	/**
	 * The set of profiles for which the annotated component should be registered.
	 */
	// 应为其注册带注解的组件的配置文件集。
	String[] value();

}
