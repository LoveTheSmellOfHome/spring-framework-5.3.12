/*
 * Copyright 2002-2020 the original author or authors.
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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.core.annotation.AliasFor;
import org.springframework.core.type.filter.TypeFilter;

/**
 * Configures component scanning directives for use with @{@link Configuration} classes.
 * Provides support parallel with Spring XML's {@code <context:component-scan>} element.
 *
 * <p>Either {@link #basePackageClasses} or {@link #basePackages} (or its alias
 * {@link #value}) may be specified to define specific packages to scan. If specific
 * packages are not defined, scanning will occur from the package of the
 * class that declares this annotation.
 *
 * <p>Note that the {@code <context:component-scan>} element has an
 * {@code annotation-config} attribute; however, this annotation does not. This is because
 * in almost all cases when using {@code @ComponentScan}, default annotation config
 * processing (e.g. processing {@code @Autowired} and friends) is assumed. Furthermore,
 * when using {@link AnnotationConfigApplicationContext}, annotation config processors are
 * always registered, meaning that any attempt to disable them at the
 * {@code @ComponentScan} level would be ignored.
 *
 * <p>See {@link Configuration @Configuration}'s Javadoc for usage examples.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 3.1
 * @see Configuration
 */
// 替换性实现：配置用于@{@link Configuration} 类的组件扫描指令。
// 提供与 Spring XML 的 {@code <context:component-scan>} 元素并行的支持
//
// <p>可以指定 {@link basePackageClasses} 或 {@link basePackages}（或其别名 {@link value}）来定义要扫描的特定包。
// 如果没有定义特定的包，将从声明该注解的类的包开始扫描
//
// <p>注意 {@code <context:component-scan>} 元素有一个 {@code annotation-config} 属性；然而，这个注解没有。
// 这是因为在几乎所有使用 {@code @ComponentScan} 的情况下，默认注解配置处理（例如处理 {@code @Autowired} 和朋友）被假定。
// 此外，当使用 {@link AnnotationConfigApplicationContext} 时，注解配置处理器总是被注册，
// 这意味着任何在 {@code @ComponentScan} 级别禁用它们的尝试都将被忽略。
//
// <p>有关用法示例，请参阅 {@link Configuration @Configuration} 的 Javadoc
// {@link ComponentScanAnnotationParser} @ComponentScan 注解解析器
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Repeatable(ComponentScans.class)
public @interface ComponentScan {

	/**
	 * Alias for {@link #basePackages}.
	 * <p>Allows for more concise annotation declarations if no other attributes
	 * are needed &mdash; for example, {@code @ComponentScan("org.my.pkg")}
	 * instead of {@code @ComponentScan(basePackages = "org.my.pkg")}.
	 */
	// {@link basePackages} 的别名。
	// <p>如果不需要其他属性，则允许更简洁的注解声明——例如，{@code @ComponentScan("org.my.pkg")} 而不是
	// {@code @ComponentScan(basePackages = "org.my.pkg")}
	@AliasFor("basePackages")
	String[] value() default {};

	/**
	 * Base packages to scan for annotated components.
	 * <p>{@link #value} is an alias for (and mutually exclusive with) this
	 * attribute.
	 * <p>Use {@link #basePackageClasses} for a type-safe alternative to
	 * String-based package names.
	 */
	// 用于扫描带注解组件的基本包。
	// <p>{@link value} 是此属性的别名（并与之互斥）。
	// <p>使用 {@link basePackageClasses} 作为基于字符串的包名称的类型安全替代方案。
	@AliasFor("value")
	String[] basePackages() default {};

	/**
	 * Type-safe alternative to {@link #basePackages} for specifying the packages
	 * to scan for annotated components. The package of each class specified will be scanned.
	 * <p>Consider creating a special no-op marker class or interface in each package
	 * that serves no purpose other than being referenced by this attribute.
	 */
	// {@link basePackages} 的类型安全替代方案，用于指定要扫描带注解组件的包。将扫描指定的每个类的包。
	// <p>考虑在每个包中创建一个特殊的无操作标记类或接口，除了被此属性引用外，没有其他用途。
	Class<?>[] basePackageClasses() default {};

	/**
	 * The {@link BeanNameGenerator} class to be used for naming detected components
	 * within the Spring container.
	 * <p>The default value of the {@link BeanNameGenerator} interface itself indicates
	 * that the scanner used to process this {@code @ComponentScan} annotation should
	 * use its inherited bean name generator, e.g. the default
	 * {@link AnnotationBeanNameGenerator} or any custom instance supplied to the
	 * application context at bootstrap time.
	 * @see AnnotationConfigApplicationContext#setBeanNameGenerator(BeanNameGenerator)
	 * @see AnnotationBeanNameGenerator
	 * @see FullyQualifiedAnnotationBeanNameGenerator
	 */
	// 用于命名 Spring 容器内检测到的组件的 {@link BeanNameGenerator} 类。
	// <p>{@link BeanNameGenerator} 接口本身的默认值表明用于处理此 {@code @ComponentScan} 注解的扫描器应使用
	// 其继承的 bean 名称生成器，例如默认 {@link AnnotationBeanNameGenerator} 或在引导时提供给应用程序上下文的任何自定义实例。
	Class<? extends BeanNameGenerator> nameGenerator() default BeanNameGenerator.class;

	/**
	 * The {@link ScopeMetadataResolver} to be used for resolving the scope of detected components.
	 */
	// {@link ScopeMetadataResolver} 用于解析检测到的组件的范围。
	Class<? extends ScopeMetadataResolver> scopeResolver() default AnnotationScopeMetadataResolver.class;

	/**
	 * Indicates whether proxies should be generated for detected components, which may be
	 * necessary when using scopes in a proxy-style fashion.
	 * <p>The default is defer to the default behavior of the component scanner used to
	 * execute the actual scan.
	 * <p>Note that setting this attribute overrides any value set for {@link #scopeResolver}.
	 * @see ClassPathBeanDefinitionScanner#setScopedProxyMode(ScopedProxyMode)
	 */
	// 指示是否应该为检测到的组件生成代理，这在以代理风格的方式使用范围时可能是必要的。
	// <p>默认是遵循用于执行实际扫描的组件扫描器的默认行为。
	// <p>请注意，设置此属性会覆盖为 {@link scopeResolver} 设置的任何值。
	ScopedProxyMode scopedProxy() default ScopedProxyMode.DEFAULT;

	/**
	 * Controls the class files eligible for component detection.
	 * <p>Consider use of {@link #includeFilters} and {@link #excludeFilters}
	 * for a more flexible approach.
	 */
	// 控制适合组件检测的类文件。
	// <p>考虑使用 {@link includeFilters} 和 {@link excludeFilters} 以获得更灵活的方法。
	String resourcePattern() default ClassPathScanningCandidateComponentProvider.DEFAULT_RESOURCE_PATTERN;

	/**
	 * Indicates whether automatic detection of classes annotated with {@code @Component}
	 * {@code @Repository}, {@code @Service}, or {@code @Controller} should be enabled.
	 */
	// 指示是否应启用使用 {@code @Component} 、{@code @Repository}、{@code @Service} 或 {@code @Controller} 注解的类的自动检测。
	boolean useDefaultFilters() default true;

	/**
	 * Specifies which types are eligible for component scanning.
	 * <p>Further narrows the set of candidate components from everything in {@link #basePackages}
	 * to everything in the base packages that matches the given filter or filters.
	 * <p>Note that these filters will be applied in addition to the default filters, if specified.
	 * Any type under the specified base packages which matches a given filter will be included,
	 * even if it does not match the default filters (i.e. is not annotated with {@code @Component}).
	 * @see #resourcePattern()
	 * @see #useDefaultFilters()
	 */
	// 指定哪些类型有资格进行组件扫描。
	// <p>进一步将候选组件集从 {@link basePackages} 中的所有内容缩小到与给定的一个或多个过滤器匹配的基本包中的所有内容。
	// <p>请注意，除了默认过滤器（如果指定）之外，还将应用这些过滤器。将包含指定基本包下与给定过滤器匹配的任何类型，
	// 即使它与默认过滤器不匹配（即未用 {@code @Component} 注释）。
	Filter[] includeFilters() default {};

	/**
	 * Specifies which types are not eligible for component scanning.
	 * @see #resourcePattern
	 */
	// 指定哪些类型不符合组件扫描条件
	Filter[] excludeFilters() default {};

	/**
	 * Specify whether scanned beans should be registered for lazy initialization.
	 * <p>Default is {@code false}; switch this to {@code true} when desired.
	 * @since 4.1
	 */
	// 指定是否应为延迟初始化注册扫描的 bean。
	// <p>默认为{@code false}；需要时将其切换为 {@code true}。
	boolean lazyInit() default false;


	/**
	 * Declares the type filter to be used as an {@linkplain ComponentScan#includeFilters
	 * include filter} or {@linkplain ComponentScan#excludeFilters exclude filter}.
	 */
	// 声明要用作 {@linkplain ComponentScan#includeFilters 包括过滤器} 或
	// {@linkplain ComponentScan#excludeFilters 排除过滤器} 的类型过滤器。
	@Retention(RetentionPolicy.RUNTIME)
	@Target({})
	@interface Filter {

		/**
		 * The type of filter to use.
		 * <p>Default is {@link FilterType#ANNOTATION}.
		 * @see #classes
		 * @see #pattern
		 */
		// 要使用的过滤器类型。
		// <p>默认为 {@link FilterType#ANNOTATION}。
		FilterType type() default FilterType.ANNOTATION;

		/**
		 * Alias for {@link #classes}.
		 * @see #classes
		 */
		// {@link classes} 的别名。
		@AliasFor("classes")
		Class<?>[] value() default {};

		/**
		 * The class or classes to use as the filter.
		 * <p>The following table explains how the classes will be interpreted
		 * based on the configured value of the {@link #type} attribute.
		 * <table border="1">
		 * <tr><th>{@code FilterType}</th><th>Class Interpreted As</th></tr>
		 * <tr><td>{@link FilterType#ANNOTATION ANNOTATION}</td>
		 * <td>the annotation itself</td></tr>
		 * <tr><td>{@link FilterType#ASSIGNABLE_TYPE ASSIGNABLE_TYPE}</td>
		 * <td>the type that detected components should be assignable to</td></tr>
		 * <tr><td>{@link FilterType#CUSTOM CUSTOM}</td>
		 * <td>an implementation of {@link TypeFilter}</td></tr>
		 * </table>
		 * <p>When multiple classes are specified, <em>OR</em> logic is applied
		 * &mdash; for example, "include types annotated with {@code @Foo} OR {@code @Bar}".
		 * <p>Custom {@link TypeFilter TypeFilters} may optionally implement any of the
		 * following {@link org.springframework.beans.factory.Aware Aware} interfaces, and
		 * their respective methods will be called prior to {@link TypeFilter#match match}:
		 * <ul>
		 * <li>{@link org.springframework.context.EnvironmentAware EnvironmentAware}</li>
		 * <li>{@link org.springframework.beans.factory.BeanFactoryAware BeanFactoryAware}
		 * <li>{@link org.springframework.beans.factory.BeanClassLoaderAware BeanClassLoaderAware}
		 * <li>{@link org.springframework.context.ResourceLoaderAware ResourceLoaderAware}
		 * </ul>
		 * <p>Specifying zero classes is permitted but will have no effect on component
		 * scanning.
		 * @since 4.2
		 * @see #value
		 * @see #type
		 */
		// 用作过滤器的一个或多个类。
		// 下表解释了如何根据type属性的配置值解释type
		// FilterType					类解释为
		// ANNOTATION					注解本身
		// ASSIGNABLE_TYPE				检测到的组件应分配给的类型
		// CUSTOM						TypeFilter 的实现
		// 当指定多个类时，将应用 OR 逻辑——例如，“包括用@Foo OR @Bar 注释的类型”
		// 自定义类型过滤器可以选择实现以下任何 Aware 接口，并且它们各自的方法将在匹配之前被调用：
		// EnvironmentAware
		// BeanFactoryAware
		// BeanClassLoaderAware
		// ResourceLoaderAware
		// 允许指定零类，但不会影响组件扫描
		@AliasFor("value")
		Class<?>[] classes() default {};

		/**
		 * The pattern (or patterns) to use for the filter, as an alternative
		 * to specifying a Class {@link #value}.
		 * <p>If {@link #type} is set to {@link FilterType#ASPECTJ ASPECTJ},
		 * this is an AspectJ type pattern expression. If {@link #type} is
		 * set to {@link FilterType#REGEX REGEX}, this is a regex pattern
		 * for the fully-qualified class names to match.
		 * @see #type
		 * @see #classes
		 */
		// 用于过滤器的模式（或模式），作为指定类值的替代方法
		// <p>如果 {@link #type} 设置为 {@link FilterType#ASPECTJ ASPECTJ}，这是一个 AspectJ 类型的模式表达式。
		// 如果 {@link #type} 设置为 {@link FilterType#REGEX REGEX}，则这是用于匹配完全限定类名称的正则表达式模式。
		String[] pattern() default {};

	}

}
