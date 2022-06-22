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
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.core.annotation.AliasFor;

/**
 * Indicates that a method produces a bean to be managed by the Spring container.
 *
 * <h3>Overview</h3>
 *
 * <p>The names and semantics of the attributes to this annotation are intentionally
 * similar to those of the {@code <bean/>} element in the Spring XML schema. For
 * example:
 *
 * <pre class="code">
 *     &#064;Bean
 *     public MyBean myBean() {
 *         // instantiate and configure MyBean obj
 *         return obj;
 *     }
 * </pre>
 *
 * <h3>Bean Names</h3>
 *
 * <p>While a {@link #name} attribute is available, the default strategy for
 * determining the name of a bean is to use the name of the {@code @Bean} method.
 * This is convenient and intuitive, but if explicit naming is desired, the
 * {@code name} attribute (or its alias {@code value}) may be used. Also note
 * that {@code name} accepts an array of Strings, allowing for multiple names
 * (i.e. a primary bean name plus one or more aliases) for a single bean.
 *
 * <pre class="code">
 *     &#064;Bean({"b1", "b2"}) // bean available as 'b1' and 'b2', but not 'myBean'
 *     public MyBean myBean() {
 *         // instantiate and configure MyBean obj
 *         return obj;
 *     }
 * </pre>
 *
 * <h3>Profile, Scope, Lazy, DependsOn, Primary, Order</h3>
 *
 * <p>Note that the {@code @Bean} annotation does not provide attributes for profile,
 * scope, lazy, depends-on or primary. Rather, it should be used in conjunction with
 * {@link Scope @Scope}, {@link Lazy @Lazy}, {@link DependsOn @DependsOn} and
 * {@link Primary @Primary} annotations to declare those semantics. For example:
 *
 * <pre class="code">
 *     &#064;Bean
 *     &#064;Profile("production")
 *     &#064;Scope("prototype")
 *     public MyBean myBean() {
 *         // instantiate and configure MyBean obj
 *         return obj;
 *     }
 * </pre>
 *
 * The semantics of the above-mentioned annotations match their use at the component
 * class level: {@code @Profile} allows for selective inclusion of certain beans.
 * {@code @Scope} changes the bean's scope from singleton to the specified scope.
 * {@code @Lazy} only has an actual effect in case of the default singleton scope.
 * {@code @DependsOn} enforces the creation of specific other beans before this
 * bean will be created, in addition to any dependencies that the bean expressed
 * through direct references, which is typically helpful for singleton startup.
 * {@code @Primary} is a mechanism to resolve ambiguity at the injection point level
 * if a single target component needs to be injected but several beans match by type.
 *
 * <p>Additionally, {@code @Bean} methods may also declare qualifier annotations
 * and {@link org.springframework.core.annotation.Order @Order} values, to be
 * taken into account during injection point resolution just like corresponding
 * annotations on the corresponding component classes but potentially being very
 * individual per bean definition (in case of multiple definitions with the same
 * bean class). Qualifiers narrow the set of candidates after the initial type match;
 * order values determine the order of resolved elements in case of collection
 * injection points (with several target beans matching by type and qualifier).
 *
 * <p><b>NOTE:</b> {@code @Order} values may influence priorities at injection points,
 * but please be aware that they do not influence singleton startup order which is an
 * orthogonal concern determined by dependency relationships and {@code @DependsOn}
 * declarations as mentioned above. Also, {@link javax.annotation.Priority} is not
 * available at this level since it cannot be declared on methods; its semantics can
 * be modeled through {@code @Order} values in combination with {@code @Primary} on
 * a single bean per type.
 *
 * <h3>{@code @Bean} Methods in {@code @Configuration} Classes</h3>
 *
 * <p>Typically, {@code @Bean} methods are declared within {@code @Configuration}
 * classes. In this case, bean methods may reference other {@code @Bean} methods in the
 * same class by calling them <i>directly</i>. This ensures that references between beans
 * are strongly typed and navigable. Such so-called <em>'inter-bean references'</em> are
 * guaranteed to respect scoping and AOP semantics, just like {@code getBean()} lookups
 * would. These are the semantics known from the original 'Spring JavaConfig' project
 * which require CGLIB subclassing of each such configuration class at runtime. As a
 * consequence, {@code @Configuration} classes and their factory methods must not be
 * marked as final or private in this mode. For example:
 *
 * <pre class="code">
 * &#064;Configuration
 * public class AppConfig {
 *
 *     &#064;Bean
 *     public FooService fooService() {
 *         return new FooService(fooRepository());
 *     }
 *
 *     &#064;Bean
 *     public FooRepository fooRepository() {
 *         return new JdbcFooRepository(dataSource());
 *     }
 *
 *     // ...
 * }</pre>
 *
 * <h3>{@code @Bean} <em>Lite</em> Mode</h3>
 *
 * <p>{@code @Bean} methods may also be declared within classes that are <em>not</em>
 * annotated with {@code @Configuration}. For example, bean methods may be declared
 * in a {@code @Component} class or even in a <em>plain old class</em>. In such cases,
 * a {@code @Bean} method will get processed in a so-called <em>'lite'</em> mode.
 *
 * <p>Bean methods in <em>lite</em> mode will be treated as plain <em>factory
 * methods</em> by the container (similar to {@code factory-method} declarations
 * in XML), with scoping and lifecycle callbacks properly applied. The containing
 * class remains unmodified in this case, and there are no unusual constraints for
 * the containing class or the factory methods.
 *
 * <p>In contrast to the semantics for bean methods in {@code @Configuration} classes,
 * <em>'inter-bean references'</em> are not supported in <em>lite</em> mode. Instead,
 * when one {@code @Bean}-method invokes another {@code @Bean}-method in <em>lite</em>
 * mode, the invocation is a standard Java method invocation; Spring does not intercept
 * the invocation via a CGLIB proxy. This is analogous to inter-{@code @Transactional}
 * method calls where in proxy mode, Spring does not intercept the invocation &mdash;
 * Spring does so only in AspectJ mode.
 *
 * <p>For example:
 *
 * <pre class="code">
 * &#064;Component
 * public class Calculator {
 *     public int sum(int a, int b) {
 *         return a+b;
 *     }
 *
 *     &#064;Bean
 *     public MyBean myBean() {
 *         return new MyBean();
 *     }
 * }</pre>
 *
 * <h3>Bootstrapping</h3>
 *
 * <p>See the @{@link Configuration} javadoc for further details including how to bootstrap
 * the container using {@link AnnotationConfigApplicationContext} and friends.
 *
 * <h3>{@code BeanFactoryPostProcessor}-returning {@code @Bean} methods</h3>
 *
 * <p>Special consideration must be taken for {@code @Bean} methods that return Spring
 * {@link org.springframework.beans.factory.config.BeanFactoryPostProcessor BeanFactoryPostProcessor}
 * ({@code BFPP}) types. Because {@code BFPP} objects must be instantiated very early in the
 * container lifecycle, they can interfere with processing of annotations such as {@code @Autowired},
 * {@code @Value}, and {@code @PostConstruct} within {@code @Configuration} classes. To avoid these
 * lifecycle issues, mark {@code BFPP}-returning {@code @Bean} methods as {@code static}. For example:
 *
 * <pre class="code">
 *     &#064;Bean
 *     public static PropertySourcesPlaceholderConfigurer pspc() {
 *         // instantiate, configure and return pspc ...
 *     }
 * </pre>
 *
 * By marking this method as {@code static}, it can be invoked without causing instantiation of its
 * declaring {@code @Configuration} class, thus avoiding the above-mentioned lifecycle conflicts.
 * Note however that {@code static} {@code @Bean} methods will not be enhanced for scoping and AOP
 * semantics as mentioned above. This works out in {@code BFPP} cases, as they are not typically
 * referenced by other {@code @Bean} methods. As a reminder, an INFO-level log message will be
 * issued for any non-static {@code @Bean} methods having a return type assignable to
 * {@code BeanFactoryPostProcessor}.
 *
 * @author Rod Johnson
 * @author Costin Leau
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 3.0
 * @see Configuration
 * @see Scope
 * @see DependsOn
 * @see Lazy
 * @see Primary
 * @see org.springframework.stereotype.Component
 * @see org.springframework.beans.factory.annotation.Autowired
 * @see org.springframework.beans.factory.annotation.Value
 */
// 指示一个方法产生一个由 Spring 容器管理的 bean。
// 概述
// 此注解的属性的名称和语义有意类似于   Spring XML 模式中的元素。 例如：
//       @Bean
//       public MyBean myBean() {
//           // instantiate and configure MyBean obj
//	         // 实例化和配置 MyBean obj
//           return obj;
//       }
//   
// Bean Name
// 虽然name属性可用，但确定 bean 名称的默认策略是使用@ Bean 方法的名称。 这既方便又直观，但如果需要显式命名，
// 可以使用name属性（或其别名 value ）。 还要注意 name 接受一个字符串数组，允许为单个 bean 使用多个
// 名称（即一个主要 bean 名称加上一个或多个别名）。
//       @Bean({"b1", "b2"}) // bean available as 'b1' and 'b2', but not 'myBean'
//       					 // bean 可用作 'b1' 和 'b2'，但不能用作“myBean”
//       public MyBean myBean() {
//           // instantiate and configure MyBean obj
//			 // 实例化和配置 MyBean obj
//           return obj;
//       }
//   
// Profile、Scope、Lazy、DependsOn、Primary、Order
// 请注意， @Bean注解不提供配置文件、范围、延迟、依赖或主要的属性。 相反，它应该与 @Scope 、 @Lazy 、 @DependsOn 和
// @Primary 注解结合使用来声明这些语义。 例如：
//       @Bean
//       @Profile("production")
//       @Scope("prototype")
//       public MyBean myBean() {
//           // instantiate and configure MyBean obj
//			 // 实例化和配置 MyBean obj
//           return obj;
//       }
//   
// 上述注解的语义与它们在组件类级别的使用相匹配： @Profile 允许特定外部环境选择性地包含某些 bean。
// @Scope 将 bean 的作用域从单例更改为指定的作用域。 @Lazy 仅在默认单例范围的情况下才有实际效果。
// @DependsOn 强制在创建此 bean 之前创建特定的其他 bean，以及 bean 通过直接引用表示的任何依赖项，这通常有助于单例启动。
// 如果需要注入单个目标组件但多个 bean 按类型匹配， @Primary是一种在注入点级别解决歧义的机制。
// 此外， @Bean 方法还可以声明限定符注解 @Qualifier 和 @Order 值，在注入点解析过程中被考虑，就像相应组件类上的相应注解一样，
// 但每个 bean 定义可能非常独立（在多个定义相同的情况下）bean 类）。 限定符在初始类型匹配后缩小候选集(分组功能)；
// order 值确定在集合注入点的情况下解析元素的顺序（多个目标 bean 按类型和限定符匹配）。
// 注意： @Order值可能会影响注入点的优先级，但请注意，它们不会影响单例启动顺序，这是由依赖关系和 @DependsOn 声明确定的正交问题，
// 如上所述。 此外， {javax.annotation.Priority @Priority }在此级别不可用，因为它不能在方法上声明；
// 它的语义可以通过 @Order值结合 @Primary 在每个类型的单个 bean 上建模。
//
// @Configuration 类中的 @Bean方法
// 通常， @Bean方法在 @Configuration 类中声明。 在这种情况下，bean 方法可以通过直接调用来引用同一个类中的其他 @Bean方法。
// 这确保了 bean 之间的引用是强类型和可导航的。 这种所谓的 inter-bean references' 即“bean 间引用”保证尊重范围和 AOP 语义，
// 就像 getBean() 查找一样。 这些是从原始“Spring JavaConfig”项目中已知的语义，它们需要在运行时对每个此类配置类
// 进行 CGLIB 子类化。 因此， @Configuration 类及其工厂方法在此模式下不得标记为 final 或 private。 例如：
//   @Configuration
//   public class AppConfig {
//  
//       @Bean
//       public FooService fooService() {
//           return new FooService(fooRepository());
//       }
//  
//       @Bean
//       public FooRepository fooRepository() {
//           return new JdbcFooRepository(dataSource());
//       }
//  
//       // ...
//   }
// @Bean精简模式
// @Bean 方法也可以在没有用 @Configuration 注解的类中声明。 例如，bean 方法可以在 @Component 类中声明，
// 甚至可以在普通的旧类中声明。 在这种情况下， @Bean方法将以所谓的“精简”模式进行处理。
// 精简模式下的 Bean 方法将被容器视为普通工厂方法（类似于 XML 中的 factory-method 声明），并正确应用范围和生命周期回调。
// 在这种情况下，包含类(引导类)保持不变，并且包含类(引导类)或工厂方法没有异常约束。
// 与 @Configuration 类中 bean 方法的语义相反， lite 模式不支持 “bean 间引用” 。 相反，当一个 @Bean -method 在
// 精简模式下调用另一个 @Bean -method 时，该调用是标准的 Java 方法调用； Spring 不会通过 CGLIB 代理拦截调用。
// 这类似于 @Transactional 方法间调用，在代理模式下，Spring 不拦截调用——Spring 只在 AspectJ 模式下这样做。
// 例如：
//   @Component
//   public class Calculator {
//       public int sum(int a, int b) {
//           return a+b;
//       }
//  
//       @Bean
//       public MyBean myBean() {
//           return new MyBean();
//       }
//   }
// 引导
// 见 @Configuration 的 javadoc 进一步详情，包括如何使用来引导容器 AnnotationConfigApplicationContext 和朋友。
// BeanFactoryPostProcessor 返回 @Bean方法
// 必须特别考虑返回 Spring BeanFactoryPostProcessor ( BFPP ) 类型的 @Bean 方法。 因为 BFPP 对象必须在容器生命周期
// 的早期实例化，所以它们会干扰 @Configuration类中的 @Autowired 、 @Value 和 @PostConstruct 等注解的处理。
// 为避免这些生命周期问题， BFPP 返回 @Bean 方法标记为 static 。 例如：
//       @Bean
//       public static PropertySourcesPlaceholderConfigurer pspc() {
//           // instantiate, configure and return pspc ...
//       }
//   
// 通过将此方法标记为 static ，可以在不导致其声明的 @Configuration 类的实例化的情况下调用它，从而避免上述生命周期冲突。
// 但是请注意， @Bean ， static @Bean 方法不会针对作用域和 AOP 语义进行增强。 这适用于 BFPP 情况，因为它们通常不
// 被其他 @Bean 方法引用。 提醒一下，将为任何具有可分配给 BeanFactoryPostProcessor 的返回类型的非静态 @Bean 方法
// 发出 INFO 级别的日志消息
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Bean {

	/**
	 * Alias for {@link #name}.
	 * <p>Intended to be used when no other attributes are needed, for example:
	 * {@code @Bean("customBeanName")}.
	 * @since 4.3.3
	 * @see #name
	 */
	// name别名。
	// 旨在在不需要其他属性时使用，例如： @Bean("customBeanName") 。
	@AliasFor("name")
	String[] value() default {};

	/**
	 * The name of this bean, or if several names, a primary bean name plus aliases.
	 * <p>If left unspecified, the name of the bean is the name of the annotated method.
	 * If specified, the method name is ignored.
	 * <p>The bean name and aliases may also be configured via the {@link #value}
	 * attribute if no other attributes are declared.
	 * @see #value
	 */
	// 此 bean 的名称，或者如果有多个名称，则是主 bean 名称加上别名。
	// 如果未指定，则 bean 的名称是带注释的方法的名称。 如果指定，方法名称将被忽略。
	// 如果没有声明其他属性，也可以通过value属性配置 bean 名称和别名。
	@AliasFor("value")
	String[] name() default {};

	/**
	 * Are dependencies to be injected via convention-based autowiring by name or type?
	 * <p>Note that this autowire mode is just about externally driven autowiring based
	 * on bean property setter methods by convention, analogous to XML bean definitions.
	 * <p>The default mode does allow for annotation-driven autowiring. "no" refers to
	 * externally driven autowiring only, not affecting any autowiring demands that the
	 * bean class itself expresses through annotations.
	 * @see Autowire#BY_NAME
	 * @see Autowire#BY_TYPE
	 * @deprecated as of 5.1, since {@code @Bean} factory method argument resolution and
	 * {@code @Autowired} processing supersede name/type-based bean property injection
	 */
	// 依赖项是否通过名称或类型通过基于约定的自动装配注入？
	// 请注意，这种自动装配模式只是基于约定的 bean 属性设置方法的外部驱动自动装配，类似于 XML bean 定义。
	// 默认模式确实允许注解驱动的自动装配。 “no”仅指外部驱动的自动装配，不影响 bean 类本身通过注解表达的任何自动装配需求。
	// 已弃用
	//       从 5.1 开始，由于@Bean工厂方法参数解析和 @Autowired 处理取代了基于名称/类型的 bean 属性注入
	@Deprecated
	Autowire autowire() default Autowire.NO;

	/**
	 * Is this bean a candidate for getting autowired into some other bean?
	 * <p>Default is {@code true}; set this to {@code false} for internal delegates
	 * that are not meant to get in the way of beans of the same type in other places.
	 * @since 5.1
	 */
	// 这个 bean 是自动装配到其他 bean 的候选者吗？
	// 默认为 true;对于不打算妨碍其他地方的相同类型 bean 的内部委托，将此设置为false 。
	boolean autowireCandidate() default true;

	/**
	 * The optional name of a method to call on the bean instance during initialization.
	 * Not commonly used, given that the method may be called programmatically directly
	 * within the body of a Bean-annotated method.
	 * <p>The default value is {@code ""}, indicating no init method to be called.
	 * @see org.springframework.beans.factory.InitializingBean
	 * @see org.springframework.context.ConfigurableApplicationContext#refresh()
	 */
	// 在初始化期间调用 bean 实例的方法的可选名称。
	// 不常用，因为该方法可以直接在 Bean 注解方法的主体内以编程方式调用。
	// 默认值为"" ，表示没有要调用的 init 方法。
	String initMethod() default "";

	/**
	 * The optional name of a method to call on the bean instance upon closing the
	 * application context, for example a {@code close()} method on a JDBC
	 * {@code DataSource} implementation, or a Hibernate {@code SessionFactory} object.
	 * The method must have no arguments but may throw any exception.
	 * <p>As a convenience to the user, the container will attempt to infer a destroy
	 * method against an object returned from the {@code @Bean} method. For example, given
	 * an {@code @Bean} method returning an Apache Commons DBCP {@code BasicDataSource},
	 * the container will notice the {@code close()} method available on that object and
	 * automatically register it as the {@code destroyMethod}. This 'destroy method
	 * inference' is currently limited to detecting only public, no-arg methods named
	 * 'close' or 'shutdown'. The method may be declared at any level of the inheritance
	 * hierarchy and will be detected regardless of the return type of the {@code @Bean}
	 * method (i.e., detection occurs reflectively against the bean instance itself at
	 * creation time).
	 * <p>To disable destroy method inference for a particular {@code @Bean}, specify an
	 * empty string as the value, e.g. {@code @Bean(destroyMethod="")}. Note that the
	 * {@link org.springframework.beans.factory.DisposableBean} callback interface will
	 * nevertheless get detected and the corresponding destroy method invoked: In other
	 * words, {@code destroyMethod=""} only affects custom close/shutdown methods and
	 * {@link java.io.Closeable}/{@link java.lang.AutoCloseable} declared close methods.
	 * <p>Note: Only invoked on beans whose lifecycle is under the full control of the
	 * factory, which is always the case for singletons but not guaranteed for any
	 * other scope.
	 * @see org.springframework.beans.factory.DisposableBean
	 * @see org.springframework.context.ConfigurableApplicationContext#close()
	 */
	// 关闭应用程序上下文时在 bean 实例上调用的方法的可选名称，例如 JDBC DataSource 实现上的 close()方法
	// 或 Hibernate SessionFactory 对象。 该方法必须没有参数，但可以抛出任何异常。
	//
	// 为方便用户，容器将尝试针对从 @Bean 方法返回的对象推断 destroy 方法。 例如，给定一个 @Bean 方法
	// 返回一个 Apache 共享 DBCP BasicDataSource ，容器会注意到 close() 该对象上可用的方法和自动注册
	// 它作为 destroyMethod 。 这种 “破坏方法推断” 目前仅限于检测名为 “close” 或 “shutdown” 的公共、无参数方法。
	// 该方法可以在继承层次结构的任何级别声明，并且无论 @Bean方法的返回类型如何，都会被检测
	// 到（即，检测在创建时反射地针对 bean 实例本身发生）。
	//
	// 要禁用特定 @Bean 销毁方法推理，请指定一个空字符串作为值，例如 @Bean(destroyMethod="") 。
	// 请注意， org.springframework.beans.factory.DisposableBean 回调接口仍然会被检测到并调用相应的销毁方法：
	// 换句话说， destroyMethod=""仅影响自定义关闭/关闭方法和java.io.Closeable / AutoCloseable声明关闭方法。
	//
	// 注意：仅在生命周期完全受工厂控制的 bean 上调用，单例总是如此，但不能保证任何其他范围。
	String destroyMethod() default AbstractBeanDefinition.INFER_METHOD;

}
