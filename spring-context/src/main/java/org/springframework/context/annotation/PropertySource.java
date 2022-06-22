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

import org.springframework.core.io.support.PropertySourceFactory;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation providing a convenient and declarative mechanism for adding a
 * {@link org.springframework.core.env.PropertySource PropertySource} to Spring's
 * {@link org.springframework.core.env.Environment Environment}. To be used in
 * conjunction with @{@link Configuration} classes.
 *
 * <h3>Example usage</h3>
 *
 * <p>Given a file {@code app.properties} containing the key/value pair
 * {@code testbean.name=myTestBean}, the following {@code @Configuration} class
 * uses {@code @PropertySource} to contribute {@code app.properties} to the
 * {@code Environment}'s set of {@code PropertySources}.
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;PropertySource("classpath:/com/myco/app.properties")
 * public class AppConfig {
 *
 *     &#064;Autowired
 *     Environment env;
 *
 *     &#064;Bean
 *     public TestBean testBean() {
 *         TestBean testBean = new TestBean();
 *         testBean.setName(env.getProperty("testbean.name"));
 *         return testBean;
 *     }
 * }</pre>
 *
 * <p>Notice that the {@code Environment} object is
 * {@link org.springframework.beans.factory.annotation.Autowired @Autowired} into the
 * configuration class and then used when populating the {@code TestBean} object. Given
 * the configuration above, a call to {@code testBean.getName()} will return "myTestBean".
 *
 * <h3>Resolving <code>${...}</code> placeholders in {@code <bean>} and {@code @Value} annotations</h3>
 *
 * <p>In order to resolve ${...} placeholders in {@code <bean>} definitions or {@code @Value}
 * annotations using properties from a {@code PropertySource}, you must ensure that an
 * appropriate <em>embedded value resolver</em> is registered in the {@code BeanFactory}
 * used by the {@code ApplicationContext}. This happens automatically when using
 * {@code <context:property-placeholder>} in XML. When using {@code @Configuration} classes
 * this can be achieved by explicitly registering a {@code PropertySourcesPlaceholderConfigurer}
 * via a {@code static} {@code @Bean} method. Note, however, that explicit registration
 * of a {@code PropertySourcesPlaceholderConfigurer} via a {@code static} {@code @Bean}
 * method is typically only required if you need to customize configuration such as the
 * placeholder syntax, etc. See the "Working with externalized values" section of
 * {@link Configuration @Configuration}'s javadocs and "a note on
 * BeanFactoryPostProcessor-returning {@code @Bean} methods" of {@link Bean @Bean}'s
 * javadocs for details and examples.
 *
 * <h3>Resolving ${...} placeholders within {@code @PropertySource} resource locations</h3>
 *
 * <p>Any ${...} placeholders present in a {@code @PropertySource} {@linkplain #value()
 * resource location} will be resolved against the set of property sources already
 * registered against the environment. For example:
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;PropertySource("classpath:/com/${my.placeholder:default/path}/app.properties")
 * public class AppConfig {
 *
 *     &#064;Autowired
 *     Environment env;
 *
 *     &#064;Bean
 *     public TestBean testBean() {
 *         TestBean testBean = new TestBean();
 *         testBean.setName(env.getProperty("testbean.name"));
 *         return testBean;
 *     }
 * }</pre>
 *
 * <p>Assuming that "my.placeholder" is present in one of the property sources already
 * registered &mdash; for example, system properties or environment variables &mdash;
 * the placeholder will be resolved to the corresponding value. If not, then "default/path"
 * will be used as a default. Expressing a default value (delimited by colon ":") is
 * optional. If no default is specified and a property cannot be resolved, an {@code
 * IllegalArgumentException} will be thrown.
 *
 * <h3>A note on property overriding with {@code @PropertySource}</h3>
 *
 * <p>In cases where a given property key exists in more than one {@code .properties}
 * file, the last {@code @PropertySource} annotation processed will 'win' and override
 * any previous key with the same name.
 *
 * <p>For example, given two properties files {@code a.properties} and
 * {@code b.properties}, consider the following two configuration classes
 * that reference them with {@code @PropertySource} annotations:
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;PropertySource("classpath:/com/myco/a.properties")
 * public class ConfigA { }
 *
 * &#064;Configuration
 * &#064;PropertySource("classpath:/com/myco/b.properties")
 * public class ConfigB { }
 * </pre>
 *
 * <p>The override ordering depends on the order in which these classes are registered
 * with the application context.
 *
 * <pre class="code">
 * AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
 * ctx.register(ConfigA.class);
 * ctx.register(ConfigB.class);
 * ctx.refresh();
 * </pre>
 *
 * <p>In the scenario above, the properties in {@code b.properties} will override any
 * duplicates that exist in {@code a.properties}, because {@code ConfigB} was registered
 * last.
 *
 * <p>In certain situations, it may not be possible or practical to tightly control
 * property source ordering when using {@code @PropertySource} annotations. For example,
 * if the {@code @Configuration} classes above were registered via component-scanning,
 * the ordering is difficult to predict. In such cases &mdash; and if overriding is important
 * &mdash; it is recommended that the user fall back to using the programmatic
 * {@code PropertySource} API. See {@link org.springframework.core.env.ConfigurableEnvironment
 * ConfigurableEnvironment} and {@link org.springframework.core.env.MutablePropertySources
 * MutablePropertySources} javadocs for details.
 *
 * <p><b>NOTE: This annotation is repeatable according to Java 8 conventions.</b>
 * However, all such {@code @PropertySource} annotations need to be declared at the same
 * level: either directly on the configuration class or as meta-annotations on the
 * same custom annotation. Mixing direct annotations and meta-annotations is not
 * recommended since direct annotations will effectively override meta-annotations.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author Phillip Webb
 * @author Sam Brannen
 * @since 3.1
 * @see PropertySources
 * @see Configuration
 * @see org.springframework.core.env.PropertySource
 * @see org.springframework.core.env.ConfigurableEnvironment#getPropertySources()
 * @see org.springframework.core.env.MutablePropertySources
 */
// 注释提供了一种方便的声明机制，用于将 {@link org.springframework.core.env.PropertySource PropertySource}
// 添加到 Spring 的 {@link org.springframework.core.env.Environment Environment}。与@{@link Configuration}
// 类结合使用。
// 外部化属性配置源,这里的资源不单单指本地资源文件，有可能是网络资源文件等
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Repeatable(PropertySources.class)
public @interface PropertySource {

	/**
	 * Indicate the name of this property source. If omitted, the {@link #factory}
	 * will generate a name based on the underlying resource (in the case of
	 * {@link org.springframework.core.io.support.DefaultPropertySourceFactory}:
	 * derived from the resource description through a corresponding name-less
	 * {@link org.springframework.core.io.support.ResourcePropertySource} constructor).
	 * @see org.springframework.core.env.PropertySource#getName()
	 * @see org.springframework.core.io.Resource#getDescription()
	 */
	// 指明该属性源的名称。如果省略，{@link factory} 将根据底层资源生成名称
	// （在 {@link org.springframework.core.io.support.DefaultPropertySourceFactory} 的情况下：
	// 通过相应的 name-less {@link org.springframework.core.io.support.ResourcePropertySource} 构造函数）
	String name() default "";

	/**
	 * Indicate the resource location(s) of the properties file to be loaded.
	 * <p>Both traditional and XML-based properties file formats are supported
	 * &mdash; for example, {@code "classpath:/com/myco/app.properties"}
	 * or {@code "file:/path/to/file.xml"}.
	 * <p>Resource location wildcards (e.g. *&#42;/*.properties) are not permitted;
	 * each location must evaluate to exactly one {@code .properties} or {@code .xml}
	 * resource.
	 * <p>${...} placeholders will be resolved against any/all property sources already
	 * registered with the {@code Environment}. See {@linkplain PropertySource above}
	 * for examples.
	 * <p>Each location will be added to the enclosing {@code Environment} as its own
	 * property source, and in the order declared.
	 */
	// 指示要加载的属性文件的资源位置。
	// 支持传统 properties 和基于 XML 的属性文件格式 — 例如:"classpath:/com/myco/app.properties" or
	// "file:/path/to/file.xml".
	// 不允许使用资源位置通配符（例如 **/*.properties）；每个位置必须评估为恰好一个 .properties 或 .xml 资源
	// ${...} 占位符将针对已在Environment注册的任何/所有属性源进行解析。 有关示例，请参见上文。
	// 每个位置都将作为其自己的属性源并按照声明的顺序添加到封闭Environment中。
	// 自定义 schema 可以确定自定义的资源文件路径，支持协议扩展。
	//
	// value 默认支持传统 properties 和基于 XML 的属性文件的路径，可扩展自定义文件路径，value 指资源文件所在的路径数组，有顺序性。
	String[] value();

	/**
	 * Indicate if a failure to find a {@link #value property resource} should be
	 * ignored.
	 * <p>{@code true} is appropriate if the properties file is completely optional.
	 * <p>Default is {@code false}.
	 * @since 4.0
	 */
	// 指示是否应忽略找不到property resource的失败。
	// 如果属性文件是完全可选的，则true是合适的。
	// 默认值为false
	boolean ignoreResourceNotFound() default false;

	/**
	 * A specific character encoding for the given resources, e.g. "UTF-8".
	 * @since 4.3
	 */
	// 指定资源的特定字符编码，例如“UTF-8” 帮助 PropertySource 指定编码，Propertie 编码默认是 ISO-8859-1
	String encoding() default "";

	/**
	 * Specify a custom {@link PropertySourceFactory}, if any.
	 * <p>By default, a default factory for standard resource files will be used.
	 * @since 4.3
	 * @see org.springframework.core.io.support.DefaultPropertySourceFactory
	 * @see org.springframework.core.io.support.ResourcePropertySource
	 */
	// 指定自定义 PropertySourceFactory ，如果有的话。PropertySourceFactory 提供了可扩展其他资源文件格式，比如 yaml 格式等
	// 默认情况下，将使用标准资源文件的默认工厂。
	Class<? extends PropertySourceFactory> factory() default PropertySourceFactory.class;

}
