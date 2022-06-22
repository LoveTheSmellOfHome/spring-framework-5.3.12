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

package org.springframework.context.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.AliasFor;
import org.springframework.stereotype.Component;

/**
 * Indicates that a class declares one or more {@link Bean @Bean} methods and
 * may be processed by the Spring container to generate bean definitions and
 * service requests for those beans at runtime, for example:
 *
 * <pre class="code">
 * &#064;Configuration
 * public class AppConfig {
 *
 *     &#064;Bean
 *     public MyBean myBean() {
 *         // instantiate, configure and return bean ...
 *     }
 * }</pre>
 *
 * <h2>Bootstrapping {@code @Configuration} classes</h2>
 *
 * <h3>Via {@code AnnotationConfigApplicationContext}</h3>
 *
 * <p>{@code @Configuration} classes are typically bootstrapped using either
 * {@link AnnotationConfigApplicationContext} or its web-capable variant,
 * {@link org.springframework.web.context.support.AnnotationConfigWebApplicationContext
 * AnnotationConfigWebApplicationContext}. A simple example with the former follows:
 *
 * <pre class="code">
 * AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
 * ctx.register(AppConfig.class);
 * ctx.refresh();
 * MyBean myBean = ctx.getBean(MyBean.class);
 * // use myBean ...
 * </pre>
 *
 * <p>See the {@link AnnotationConfigApplicationContext} javadocs for further details, and see
 * {@link org.springframework.web.context.support.AnnotationConfigWebApplicationContext
 * AnnotationConfigWebApplicationContext} for web configuration instructions in a
 * {@code Servlet} container.
 *
 * <h3>Via Spring {@code <beans>} XML</h3>
 *
 * <p>As an alternative to registering {@code @Configuration} classes directly against an
 * {@code AnnotationConfigApplicationContext}, {@code @Configuration} classes may be
 * declared as normal {@code <bean>} definitions within Spring XML files:
 *
 * <pre class="code">
 * &lt;beans&gt;
 *    &lt;context:annotation-config/&gt;
 *    &lt;bean class="com.acme.AppConfig"/&gt;
 * &lt;/beans&gt;
 * </pre>
 *
 * <p>In the example above, {@code <context:annotation-config/>} is required in order to
 * enable {@link ConfigurationClassPostProcessor} and other annotation-related
 * post processors that facilitate handling {@code @Configuration} classes.
 *
 * <h3>Via component scanning</h3>
 *
 * <p>{@code @Configuration} is meta-annotated with {@link Component @Component}, therefore
 * {@code @Configuration} classes are candidates for component scanning (typically using
 * Spring XML's {@code <context:component-scan/>} element) and therefore may also take
 * advantage of {@link Autowired @Autowired}/{@link javax.inject.Inject @Inject}
 * like any regular {@code @Component}. In particular, if a single constructor is present
 * autowiring semantics will be applied transparently for that constructor:
 *
 * <pre class="code">
 * &#064;Configuration
 * public class AppConfig {
 *
 *     private final SomeBean someBean;
 *
 *     public AppConfig(SomeBean someBean) {
 *         this.someBean = someBean;
 *     }
 *
 *     // &#064;Bean definition using "SomeBean"
 *
 * }</pre>
 *
 * <p>{@code @Configuration} classes may not only be bootstrapped using
 * component scanning, but may also themselves <em>configure</em> component scanning using
 * the {@link ComponentScan @ComponentScan} annotation:
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;ComponentScan("com.acme.app.services")
 * public class AppConfig {
 *     // various &#064;Bean definitions ...
 * }</pre>
 *
 * <p>See the {@link ComponentScan @ComponentScan} javadocs for details.
 *
 * <h2>Working with externalized values</h2>
 *
 * <h3>Using the {@code Environment} API</h3>
 *
 * <p>Externalized values may be looked up by injecting the Spring
 * {@link org.springframework.core.env.Environment} into a {@code @Configuration}
 * class &mdash; for example, using the {@code @Autowired} annotation:
 *
 * <pre class="code">
 * &#064;Configuration
 * public class AppConfig {
 *
 *     &#064;Autowired Environment env;
 *
 *     &#064;Bean
 *     public MyBean myBean() {
 *         MyBean myBean = new MyBean();
 *         myBean.setName(env.getProperty("bean.name"));
 *         return myBean;
 *     }
 * }</pre>
 *
 * <p>Properties resolved through the {@code Environment} reside in one or more "property
 * source" objects, and {@code @Configuration} classes may contribute property sources to
 * the {@code Environment} object using the {@link PropertySource @PropertySource}
 * annotation:
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;PropertySource("classpath:/com/acme/app.properties")
 * public class AppConfig {
 *
 *     &#064;Inject Environment env;
 *
 *     &#064;Bean
 *     public MyBean myBean() {
 *         return new MyBean(env.getProperty("bean.name"));
 *     }
 * }</pre>
 *
 * <p>See the {@link org.springframework.core.env.Environment Environment}
 * and {@link PropertySource @PropertySource} javadocs for further details.
 *
 * <h3>Using the {@code @Value} annotation</h3>
 *
 * <p>Externalized values may be injected into {@code @Configuration} classes using
 * the {@link Value @Value} annotation:
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;PropertySource("classpath:/com/acme/app.properties")
 * public class AppConfig {
 *
 *     &#064;Value("${bean.name}") String beanName;
 *
 *     &#064;Bean
 *     public MyBean myBean() {
 *         return new MyBean(beanName);
 *     }
 * }</pre>
 *
 * <p>This approach is often used in conjunction with Spring's
 * {@link org.springframework.context.support.PropertySourcesPlaceholderConfigurer
 * PropertySourcesPlaceholderConfigurer} that can be enabled <em>automatically</em>
 * in XML configuration via {@code <context:property-placeholder/>} or <em>explicitly</em>
 * in a {@code @Configuration} class via a dedicated {@code static} {@code @Bean} method
 * (see "a note on BeanFactoryPostProcessor-returning {@code @Bean} methods" of
 * {@link Bean @Bean}'s javadocs for details). Note, however, that explicit registration
 * of a {@code PropertySourcesPlaceholderConfigurer} via a {@code static} {@code @Bean}
 * method is typically only required if you need to customize configuration such as the
 * placeholder syntax, etc. Specifically, if no bean post-processor (such as a
 * {@code PropertySourcesPlaceholderConfigurer}) has registered an <em>embedded value
 * resolver</em> for the {@code ApplicationContext}, Spring will register a default
 * <em>embedded value resolver</em> which resolves placeholders against property sources
 * registered in the {@code Environment}. See the section below on composing
 * {@code @Configuration} classes with Spring XML using {@code @ImportResource}; see
 * the {@link Value @Value} javadocs; and see the {@link Bean @Bean} javadocs for details
 * on working with {@code BeanFactoryPostProcessor} types such as
 * {@code PropertySourcesPlaceholderConfigurer}.
 *
 * <h2>Composing {@code @Configuration} classes</h2>
 *
 * <h3>With the {@code @Import} annotation</h3>
 *
 * <p>{@code @Configuration} classes may be composed using the {@link Import @Import} annotation,
 * similar to the way that {@code <import>} works in Spring XML. Because
 * {@code @Configuration} objects are managed as Spring beans within the container,
 * imported configurations may be injected &mdash; for example, via constructor injection:
 *
 * <pre class="code">
 * &#064;Configuration
 * public class DatabaseConfig {
 *
 *     &#064;Bean
 *     public DataSource dataSource() {
 *         // instantiate, configure and return DataSource
 *     }
 * }
 *
 * &#064;Configuration
 * &#064;Import(DatabaseConfig.class)
 * public class AppConfig {
 *
 *     private final DatabaseConfig dataConfig;
 *
 *     public AppConfig(DatabaseConfig dataConfig) {
 *         this.dataConfig = dataConfig;
 *     }
 *
 *     &#064;Bean
 *     public MyBean myBean() {
 *         // reference the dataSource() bean method
 *         return new MyBean(dataConfig.dataSource());
 *     }
 * }</pre>
 *
 * <p>Now both {@code AppConfig} and the imported {@code DatabaseConfig} can be bootstrapped
 * by registering only {@code AppConfig} against the Spring context:
 *
 * <pre class="code">
 * new AnnotationConfigApplicationContext(AppConfig.class);</pre>
 *
 * <h3>With the {@code @Profile} annotation</h3>
 *
 * <p>{@code @Configuration} classes may be marked with the {@link Profile @Profile} annotation to
 * indicate they should be processed only if a given profile or profiles are <em>active</em>:
 *
 * <pre class="code">
 * &#064;Profile("development")
 * &#064;Configuration
 * public class EmbeddedDatabaseConfig {
 *
 *     &#064;Bean
 *     public DataSource dataSource() {
 *         // instantiate, configure and return embedded DataSource
 *     }
 * }
 *
 * &#064;Profile("production")
 * &#064;Configuration
 * public class ProductionDatabaseConfig {
 *
 *     &#064;Bean
 *     public DataSource dataSource() {
 *         // instantiate, configure and return production DataSource
 *     }
 * }</pre>
 *
 * <p>Alternatively, you may also declare profile conditions at the {@code @Bean} method level
 * &mdash; for example, for alternative bean variants within the same configuration class:
 *
 * <pre class="code">
 * &#064;Configuration
 * public class ProfileDatabaseConfig {
 *
 *     &#064;Bean("dataSource")
 *     &#064;Profile("development")
 *     public DataSource embeddedDatabase() { ... }
 *
 *     &#064;Bean("dataSource")
 *     &#064;Profile("production")
 *     public DataSource productionDatabase() { ... }
 * }</pre>
 *
 * <p>See the {@link Profile @Profile} and {@link org.springframework.core.env.Environment}
 * javadocs for further details.
 *
 * <h3>With Spring XML using the {@code @ImportResource} annotation</h3>
 *
 * <p>As mentioned above, {@code @Configuration} classes may be declared as regular Spring
 * {@code <bean>} definitions within Spring XML files. It is also possible to
 * import Spring XML configuration files into {@code @Configuration} classes using
 * the {@link ImportResource @ImportResource} annotation. Bean definitions imported from
 * XML can be injected &mdash; for example, using the {@code @Inject} annotation:
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;ImportResource("classpath:/com/acme/database-config.xml")
 * public class AppConfig {
 *
 *     &#064;Inject DataSource dataSource; // from XML
 *
 *     &#064;Bean
 *     public MyBean myBean() {
 *         // inject the XML-defined dataSource bean
 *         return new MyBean(this.dataSource);
 *     }
 * }</pre>
 *
 * <h3>With nested {@code @Configuration} classes</h3>
 *
 * <p>{@code @Configuration} classes may be nested within one another as follows:
 *
 * <pre class="code">
 * &#064;Configuration
 * public class AppConfig {
 *
 *     &#064;Inject DataSource dataSource;
 *
 *     &#064;Bean
 *     public MyBean myBean() {
 *         return new MyBean(dataSource);
 *     }
 *
 *     &#064;Configuration
 *     static class DatabaseConfig {
 *         &#064;Bean
 *         DataSource dataSource() {
 *             return new EmbeddedDatabaseBuilder().build();
 *         }
 *     }
 * }</pre>
 *
 * <p>When bootstrapping such an arrangement, only {@code AppConfig} need be registered
 * against the application context. By virtue of being a nested {@code @Configuration}
 * class, {@code DatabaseConfig} <em>will be registered automatically</em>. This avoids
 * the need to use an {@code @Import} annotation when the relationship between
 * {@code AppConfig} and {@code DatabaseConfig} is already implicitly clear.
 *
 * <p>Note also that nested {@code @Configuration} classes can be used to good effect
 * with the {@code @Profile} annotation to provide two options of the same bean to the
 * enclosing {@code @Configuration} class.
 *
 * <h2>Configuring lazy initialization</h2>
 *
 * <p>By default, {@code @Bean} methods will be <em>eagerly instantiated</em> at container
 * bootstrap time.  To avoid this, {@code @Configuration} may be used in conjunction with
 * the {@link Lazy @Lazy} annotation to indicate that all {@code @Bean} methods declared
 * within the class are by default lazily initialized. Note that {@code @Lazy} may be used
 * on individual {@code @Bean} methods as well.
 *
 * <h2>Testing support for {@code @Configuration} classes</h2>
 *
 * <p>The Spring <em>TestContext framework</em> available in the {@code spring-test} module
 * provides the {@code @ContextConfiguration} annotation which can accept an array of
 * <em>component class</em> references &mdash; typically {@code @Configuration} or
 * {@code @Component} classes.
 *
 * <pre class="code">
 * &#064;RunWith(SpringRunner.class)
 * &#064;ContextConfiguration(classes = {AppConfig.class, DatabaseConfig.class})
 * public class MyTests {
 *
 *     &#064;Autowired MyBean myBean;
 *
 *     &#064;Autowired DataSource dataSource;
 *
 *     &#064;Test
 *     public void test() {
 *         // assertions against myBean ...
 *     }
 * }</pre>
 *
 * <p>See the
 * <a href="https://docs.spring.io/spring/docs/current/spring-framework-reference/testing.html#testcontext-framework">TestContext framework</a>
 * reference documentation for details.
 *
 * <h2>Enabling built-in Spring features using {@code @Enable} annotations</h2>
 *
 * <p>Spring features such as asynchronous method execution, scheduled task execution,
 * annotation driven transaction management, and even Spring MVC can be enabled and
 * configured from {@code @Configuration} classes using their respective "{@code @Enable}"
 * annotations. See
 * {@link org.springframework.scheduling.annotation.EnableAsync @EnableAsync},
 * {@link org.springframework.scheduling.annotation.EnableScheduling @EnableScheduling},
 * {@link org.springframework.transaction.annotation.EnableTransactionManagement @EnableTransactionManagement},
 * {@link org.springframework.context.annotation.EnableAspectJAutoProxy @EnableAspectJAutoProxy},
 * and {@link org.springframework.web.servlet.config.annotation.EnableWebMvc @EnableWebMvc}
 * for details.
 *
 * <h2>Constraints when authoring {@code @Configuration} classes</h2>
 *
 * <ul>
 * <li>Configuration classes must be provided as classes (i.e. not as instances returned
 * from factory methods), allowing for runtime enhancements through a generated subclass.
 * <li>Configuration classes must be non-final (allowing for subclasses at runtime),
 * unless the {@link #proxyBeanMethods() proxyBeanMethods} flag is set to {@code false}
 * in which case no runtime-generated subclass is necessary.
 * <li>Configuration classes must be non-local (i.e. may not be declared within a method).
 * <li>Any nested configuration classes must be declared as {@code static}.
 * <li>{@code @Bean} methods may not in turn create further configuration classes
 * (any such instances will be treated as regular beans, with their configuration
 * annotations remaining undetected).
 * </ul>
 *
 * @author Rod Johnson
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 3.0
 * @see Bean
 * @see Profile
 * @see Import
 * @see ImportResource
 * @see ComponentScan
 * @see Lazy
 * @see PropertySource
 * @see AnnotationConfigApplicationContext
 * @see ConfigurationClassPostProcessor
 * @see org.springframework.core.env.Environment
 * @see org.springframework.test.context.ContextConfiguration
 */
// 表示一个类声明了一个或多个@Bean方法，并且可能会被 Spring 容器处理以在运行时为这些 bean 生成 bean 定义和服务请求，例如：
//   @Configuration
//   public class AppConfig {
//  
//       @Bean
//       public MyBean myBean() {
//           // instantiate, configure and return bean ...
//       }
//   }
//引导@Configuration类
//通过AnnotationConfigApplicationContext
//@Configuration类通常使用AnnotationConfigApplicationContext或其支持 web 的变体AnnotationConfigWebApplicationContext引导。 前者的简单示例如下：
//   AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
//   ctx.register(AppConfig.class);
//   ctx.refresh();
//   MyBean myBean = ctx.getBean(MyBean.class);
//   // use myBean ...
//   
//有关详细信息，请参阅AnnotationConfigApplicationContext javadocs，有关Servlet容器中的 Web 配置说明，请参阅AnnotationConfigWebApplicationContext 。
//通过 Spring XML
//作为直接针对AnnotationConfigApplicationContext注册@Configuration类的替代方法， @Configuration类可以声明为正常   Spring XML 文件中的定义：
//   <beans>
//      <context:annotation-config/>
//      <bean class="com.acme.AppConfig"/>
//   </beans>
//   
//在上面的例子中，   需要启用ConfigurationClassPostProcessor和其他与注解相关的后处理器，以方便处理@Configuration类。
//通过元件扫描
//@Configuration使用@Component元注解，因此@Configuration类是组件扫描的候选者（通常使用 Spring XML 的   element)，
// 因此也可以像任何常规的@Component一样利用@Autowired / @Inject 。 特别是，如果存在单个构造函数，自动装配语义将透明地应用于该构造函数：
//   @Configuration
//   public class AppConfig {
//  
//       private final SomeBean someBean;
//  
//       public AppConfig(SomeBean someBean) {
//           this.someBean = someBean;
//       }
//  
//       // @Bean definition using "SomeBean"
//  
//   }
//@Configuration类不仅可以使用组件扫描进行引导，还可以使用@ComponentScan注解自己配置组件扫描：
//   @Configuration
//   @ComponentScan("com.acme.app.services")
//   public class AppConfig {
//       // various @Bean definitions ...
//   }
//有关详细信息，请参阅@ComponentScan javadocs。
//使用外化值
//使用Environment API
//可以通过将 Spring org.springframework.core.env.Environment注入@Configuration类来查找外化值——例如，使用@Autowired注解：
//   @Configuration
//   public class AppConfig {
//  
//       @Autowired Environment env;
//  
//       @Bean
//       public MyBean myBean() {
//           MyBean myBean = new MyBean();
//           myBean.setName(env.getProperty("bean.name"));
//           return myBean;
//       }
//   }
//通过Environment解析的属性驻留在一个或多个“属性源”对象中， @Configuration 类可以使用 @PropertySource 注解将属性源
// 贡献给Environment对象：
//   @Configuration
//   @PropertySource("classpath:/com/acme/app.properties")
//   public class AppConfig {
//  
//       @Inject Environment env;
//  
//       @Bean
//       public MyBean myBean() {
//           return new MyBean(env.getProperty("bean.name"));
//       }
//   }
//有关更多详细信息，请参阅Environment和@PropertySource javadocs。
//使用@Value注解
//可以使用@Value注解将外部化的值注入到@Configuration类中：
//   @Configuration
//   @PropertySource("classpath:/com/acme/app.properties")
//   public class AppConfig {
//  
//       @Value("${bean.name}") String beanName;
//  
//       @Bean
//       public MyBean myBean() {
//           return new MyBean(beanName);
//       }
//   }
//这种方法通常与 Spring 的PropertySourcesPlaceholderConfigurer结合使用，可以通过在 XML 配置中自动启用
//或通过专用的static @Bean方法显式地在@Configuration类中（有关详细信息，请@Bean的 javadocs 的“关于 BeanFactoryPostProcessor
// 返回@Bean方法的@Bean ”）。 但是请注意，通常仅当您需要自定义配置（例如占位符语法等）时，
// 才需要通过static @Bean方法显式注册PropertySourcesPlaceholderConfigurer 。
// 具体而言，如果没有 bean 后处理器（例如PropertySourcesPlaceholderConfigurer ）已注册ApplicationContext的嵌入值解析器，
// Spring 将注册一个默认的嵌入值解析器，它根据Environment注册的属性源解析占位符。 请参阅以下有关使用
// @ImportResource使用 Spring XML 组合@Configuration类的部分； 请参阅@Value javadocs；
// 有关使用BeanFactoryPostProcessor类型（例如PropertySourcesPlaceholderConfigurer详细信息，请参阅@Bean javadocs。
//组合@Configuration类
//使用@Import注解
//@Configuration类可以使用@Import注解组成，类似于  在 Spring XML 中工作。 因为@Configuration对象在容器内作为 Spring bean 进行管理，
// 导入的配置可能会被注入——例如，通过构造函数注入：
//   @Configuration
//   public class DatabaseConfig {
//  
//       @Bean
//       public DataSource dataSource() {
//           // instantiate, configure and return DataSource
//       }
//   }
//  
//   @Configuration
//   @Import(DatabaseConfig.class)
//   public class AppConfig {
//  
//       private final DatabaseConfig dataConfig;
//  
//       public AppConfig(DatabaseConfig dataConfig) {
//           this.dataConfig = dataConfig;
//       }
//  
//       @Bean
//       public MyBean myBean() {
//           // reference the dataSource() bean method
//           return new MyBean(dataConfig.dataSource());
//       }
//   }
//现在， AppConfig和导入的DatabaseConfig都可以通过仅针对 Spring 上下文注册AppConfig来引导：
//   new AnnotationConfigApplicationContext(AppConfig.class);
//使用@Profile注解
//@Configuration类可以用@Profile注解标记，以表明只有在给定的一个或多个配置文件处于活动状态时才应该处理它们：
//   @Profile("development")
//   @Configuration
//   public class EmbeddedDatabaseConfig {
//  
//       @Bean
//       public DataSource dataSource() {
//           // instantiate, configure and return embedded DataSource
//       }
//   }
//  
//   @Profile("production")
//   @Configuration
//   public class ProductionDatabaseConfig {
//  
//       @Bean
//       public DataSource dataSource() {
//           // instantiate, configure and return production DataSource
//       }
//   }
//或者，您也可以在@Bean方法级别声明配置文件条件 - 例如，对于同一配置类中的替代 bean 变体：
//   @Configuration
//   public class ProfileDatabaseConfig {
//  
//       @Bean("dataSource")
//       @Profile("development")
//       public DataSource embeddedDatabase() { ... }
//  
//       @Bean("dataSource")
//       @Profile("production")
//       public DataSource productionDatabase() { ... }
//   }
//有关更多详细信息，请参阅@Profile和org.springframework.core.env.Environment javadocs。
//使用@ImportResource注解的 Spring XML
//如上所述， @Configuration类可以声明为常规 Spring   Spring XML 文件中的定义。 也可以使用@ImportResource注解将 Spring XML 配置
// 文件导入到@Configuration类中。 可以注入从 XML 导入的 Bean 定义——例如，使用@Inject注解：
//   @Configuration
//   @ImportResource("classpath:/com/acme/database-config.xml")
//   public class AppConfig {
//  
//       @Inject DataSource dataSource; // from XML
//  
//       @Bean
//       public MyBean myBean() {
//           // inject the XML-defined dataSource bean
//           return new MyBean(this.dataSource);
//       }
//   }
//使用嵌套的@Configuration类
//@Configuration类可以相互嵌套，如下所示：
//   @Configuration
//   public class AppConfig {
//  
//       @Inject DataSource dataSource;
//  
//       @Bean
//       public MyBean myBean() {
//           return new MyBean(dataSource);
//       }
//  
//       @Configuration
//       static class DatabaseConfig {
//           @Bean
//           DataSource dataSource() {
//               return new EmbeddedDatabaseBuilder().build();
//           }
//       }
//   }
//在引导这样的安排时，只需要针对应用程序上下文注册AppConfig 。 由于是嵌套的@Configuration类， DatabaseConfig将被自动注册。
// 当 AppConfig 和 DatabaseConfig 之间的关系已经隐式明确时，这避免了使用@Import注解的需要。
//另请注意，嵌套的 @Configuration 类可以与 @Profile 注解一起使用，以向封闭的 @Configuration 类提供相同 bean 的两个选项。
//配置延迟初始化
//默认情况下， @Bean方法将在容器引导时急切地实例化。 为了避免这种情况， @Configuration @Lazy可以与@Lazy注解结合使用，
// 以指示类中声明的所有@Bean方法默认是延迟初始化的。 请注意， @Lazy也可以用于单独的@Bean方法。
//对 @Configuration类 的测试支持
//spring-test模块中可用的 Spring TestContext 框架提供了@ContextConfiguration注解，它可以接受
// @ContextConfiguration件类引用——通常是@Configuration或@Component类。
//   @RunWith(SpringRunner.class)
//   @ContextConfiguration(classes = {AppConfig.class, DatabaseConfig.class})
//   public class MyTests {
//  
//       @Autowired MyBean myBean;
//  
//       @Autowired DataSource dataSource;
//  
//       @Test
//       public void test() {
//           // assertions against myBean ...
//       }
//   }
//有关详细信息，请参阅TestContext 框架 参考文档。
//使用@Enable注解启用内置 Spring 功能
//Spring 的特性，例如异步方法执行、计划任务执行、注解驱动的事务管理，甚至 Spring MVC 都可以使用它们各自的
//"@Enable" 注解从 @Configuration 类中启用和配置。 有关详细@EnableAsync ，请参阅
// @EnableAsync 、 @EnableScheduling 、 @EnableTransactionManagement 、 @EnableAspectJAutoProxy和@EnableWebMvc 。
//创作 @Configuration 类时的约束
//配置类必须作为类提供（即不是作为从工厂方法返回的实例），允许通过生成的子类进行运行时增强。
//配置类必须是非最终的（允许在运行时使用子类），除非 proxyBeanMethods 标志设置为false在这种情况下不需要运行时生成的子类。
//配置类必须是非本地的（即不能在方法中声明）。
//任何嵌套的配置类都必须声明为static 。
//@Bean方法可能不会反过来创建更多的配置类（任何此类实例都将被视为常规 bean，它们的配置注解仍然未被检测到）。
//
// 从 BeanDefinition 中 Role 的角色来讲，标记本接口的都是面向业务的 bean
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface Configuration {

	/**
	 * Explicitly specify the name of the Spring bean definition associated with the
	 * {@code @Configuration} class. If left unspecified (the common case), a bean
	 * name will be automatically generated.
	 * <p>The custom name applies only if the {@code @Configuration} class is picked
	 * up via component scanning or supplied directly to an
	 * {@link AnnotationConfigApplicationContext}. If the {@code @Configuration} class
	 * is registered as a traditional XML bean definition, the name/id of the bean
	 * element will take precedence.
	 * @return the explicit component name, if any (or empty String otherwise)
	 * @see AnnotationBeanNameGenerator
	 */
	// 显式指定与@Configuration 类关联的 Spring bean 定义的名称。如果未指定（常见情况），将自动生成一个 bean 名称。
	// 自定义名称仅适用于通过组件扫描获取 @Configuration 类或直接提供给 AnnotationConfigApplicationContext 的情况。
	// 如果@Configuration 类注册为传统的XML bean 定义，则bean 元素的nameid 将优先。
	@AliasFor(annotation = Component.class)
	String value() default "";

	/**
	 * Specify whether {@code @Bean} methods should get proxied in order to enforce
	 * bean lifecycle behavior, e.g. to return shared singleton bean instances even
	 * in case of direct {@code @Bean} method calls in user code. This feature
	 * requires method interception, implemented through a runtime-generated CGLIB
	 * subclass which comes with limitations such as the configuration class and
	 * its methods not being allowed to declare {@code final}.
	 * <p>The default is {@code true}, allowing for 'inter-bean references' via direct
	 * method calls within the configuration class as well as for external calls to
	 * this configuration's {@code @Bean} methods, e.g. from another configuration class.
	 * If this is not needed since each of this particular configuration's {@code @Bean}
	 * methods is self-contained and designed as a plain factory method for container use,
	 * switch this flag to {@code false} in order to avoid CGLIB subclass processing.
	 * <p>Turning off bean method interception effectively processes {@code @Bean}
	 * methods individually like when declared on non-{@code @Configuration} classes,
	 * a.k.a. "@Bean Lite Mode" (see {@link Bean @Bean's javadoc}). It is therefore
	 * behaviorally equivalent to removing the {@code @Configuration} stereotype.
	 * @since 5.2
	 */
	// 指定是否应该代理 @Bean 方法以强制执行 bean 生命周期行为，例如即使在用户代码中直接调用 @Bean 方法的情况下，
	// 也能返回共享的单例 bean 实例。此功能需要方法拦截，通过运行时生成的 CGLIB 子类实现，
	// 该子类具有诸如配置类及其方法不允许声明为 final 等限制
    //
    // 默认值为 true，允许通过配置类中的直接方法调用以及对此配置的 @Bean 方法的外部调用进行'inter-bean references'，
	// 例如来自另一个配置类。如果不需要这样做，因为每个特定配置的 @Bean 方法都是自包含的，并且设计为容器使用的普通工厂方法，
	// 请将此标志切换为 false 以避免 CGLIB 子类处理。
	boolean proxyBeanMethods() default true;

}
