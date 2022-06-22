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

package org.springframework.transaction.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.AdviceMode;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;

/**
 * Enables Spring's annotation-driven transaction management capability, similar to
 * the support found in Spring's {@code <tx:*>} XML namespace. To be used on
 * {@link org.springframework.context.annotation.Configuration @Configuration}
 * classes to configure traditional, imperative transaction management or
 * reactive transaction management.
 *
 * <p>The following example demonstrates imperative transaction management
 * using a {@link org.springframework.transaction.PlatformTransactionManager
 * PlatformTransactionManager}. For reactive transaction management, configure a
 * {@link org.springframework.transaction.ReactiveTransactionManager
 * ReactiveTransactionManager} instead.
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableTransactionManagement
 * public class AppConfig {
 *
 *     &#064;Bean
 *     public FooRepository fooRepository() {
 *         // configure and return a class having &#064;Transactional methods
 *         return new JdbcFooRepository(dataSource());
 *     }
 *
 *     &#064;Bean
 *     public DataSource dataSource() {
 *         // configure and return the necessary JDBC DataSource
 *     }
 *
 *     &#064;Bean
 *     public PlatformTransactionManager txManager() {
 *         return new DataSourceTransactionManager(dataSource());
 *     }
 * }</pre>
 *
 * <p>For reference, the example above can be compared to the following Spring XML
 * configuration:
 *
 * <pre class="code">
 * &lt;beans&gt;
 *
 *     &lt;tx:annotation-driven/&gt;
 *
 *     &lt;bean id="fooRepository" class="com.foo.JdbcFooRepository"&gt;
 *         &lt;constructor-arg ref="dataSource"/&gt;
 *     &lt;/bean&gt;
 *
 *     &lt;bean id="dataSource" class="com.vendor.VendorDataSource"/&gt;
 *
 *     &lt;bean id="transactionManager" class="org.sfwk...DataSourceTransactionManager"&gt;
 *         &lt;constructor-arg ref="dataSource"/&gt;
 *     &lt;/bean&gt;
 *
 * &lt;/beans&gt;
 * </pre>
 *
 * In both of the scenarios above, {@code @EnableTransactionManagement} and {@code
 * <tx:annotation-driven/>} are responsible for registering the necessary Spring
 * components that power annotation-driven transaction management, such as the
 * TransactionInterceptor and the proxy- or AspectJ-based advice that weaves the
 * interceptor into the call stack when {@code JdbcFooRepository}'s {@code @Transactional}
 * methods are invoked.
 *
 * <p>A minor difference between the two examples lies in the naming of the {@code
 * TransactionManager} bean: In the {@code @Bean} case, the name is
 * <em>"txManager"</em> (per the name of the method); in the XML case, the name is
 * <em>"transactionManager"</em>. {@code <tx:annotation-driven/>} is hard-wired to
 * look for a bean named "transactionManager" by default, however
 * {@code @EnableTransactionManagement} is more flexible; it will fall back to a by-type
 * lookup for any {@code TransactionManager} bean in the container. Thus the name
 * can be "txManager", "transactionManager", or "tm": it simply does not matter.
 *
 * <p>For those that wish to establish a more direct relationship between
 * {@code @EnableTransactionManagement} and the exact transaction manager bean to be used,
 * the {@link TransactionManagementConfigurer} callback interface may be implemented -
 * notice the {@code implements} clause and the {@code @Override}-annotated method below:
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableTransactionManagement
 * public class AppConfig implements TransactionManagementConfigurer {
 *
 *     &#064;Bean
 *     public FooRepository fooRepository() {
 *         // configure and return a class having &#064;Transactional methods
 *         return new JdbcFooRepository(dataSource());
 *     }
 *
 *     &#064;Bean
 *     public DataSource dataSource() {
 *         // configure and return the necessary JDBC DataSource
 *     }
 *
 *     &#064;Bean
 *     public PlatformTransactionManager txManager() {
 *         return new DataSourceTransactionManager(dataSource());
 *     }
 *
 *     &#064;Override
 *     public PlatformTransactionManager annotationDrivenTransactionManager() {
 *         return txManager();
 *     }
 * }</pre>
 *
 * <p>This approach may be desirable simply because it is more explicit, or it may be
 * necessary in order to distinguish between two {@code TransactionManager} beans
 * present in the same container.  As the name suggests, the
 * {@code annotationDrivenTransactionManager()} will be the one used for processing
 * {@code @Transactional} methods. See {@link TransactionManagementConfigurer} Javadoc
 * for further details.
 *
 * <p>The {@link #mode} attribute controls how advice is applied: If the mode is
 * {@link AdviceMode#PROXY} (the default), then the other attributes control the behavior
 * of the proxying. Please note that proxy mode allows for interception of calls through
 * the proxy only; local calls within the same class cannot get intercepted that way.
 *
 * <p>Note that if the {@linkplain #mode} is set to {@link AdviceMode#ASPECTJ}, then the
 * value of the {@link #proxyTargetClass} attribute will be ignored. Note also that in
 * this case the {@code spring-aspects} module JAR must be present on the classpath, with
 * compile-time weaving or load-time weaving applying the aspect to the affected classes.
 * There is no proxy involved in such a scenario; local calls will be intercepted as well.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 3.1
 * @see TransactionManagementConfigurer
 * @see TransactionManagementConfigurationSelector
 * @see ProxyTransactionManagementConfiguration
 * @see org.springframework.transaction.aspectj.AspectJTransactionManagementConfiguration
 */
// 启用 Spring 的注解驱动事务管理功能，类似于 Spring 中的支持  XML 命名空间。用于 @Configuration 类来
// 配置传统的命令式事务管理或反应式事务管理。
// 
// 以下示例演示了使用 PlatformTransactionManager 的命令式事务管理。对于反应式事务管理，请改为配置
// ReactiveTransactionManager 。
//   @Configuration
//   @EnableTransactionManagement
//   public class AppConfig {
//  
//       @Bean
//       public FooRepository fooRepository() {
//           // configure and return a class having @Transactional methods
//           // 配置并返回具有@Transactional 方法的类
//           return new JdbcFooRepository(dataSource());
//       }
//  
//       @Bean
//       public DataSource dataSource() {
//           // configure and return the necessary JDBC DataSource
//           // 配置并返回必要的 JDBC DataSource
//       }
//  
//       @Bean
//       public PlatformTransactionManager txManager() {
//           return new DataSourceTransactionManager(dataSource());
//       }
//   }
// 作为参考，可以将上面的示例与以下 Spring XML 配置进行比较：
//   <beans>
//  
//       <tx:annotation-driven/>
//  
//       <bean id="fooRepository" class="com.foo.JdbcFooRepository">
//           <constructor-arg ref="dataSource"/>
//       </bean>
//  
//       <bean id="dataSource" class="com.vendor.VendorDataSource"/>
//  
//       <bean id="transactionManager" class="org.sfwk...DataSourceTransactionManager">
//           <constructor-arg ref="dataSource"/>
//       </bean>
//  
//   </beans>
//   
// 在上述两种情况下， @EnableTransactionManagement和  负责注册为注解驱动的事务管理，
// 提供支持的必要 Spring 组件，例如 TransactionInterceptor 和基于代理或 AspectJ 的建议，
// 当 JdbcFooRepository 的 @Transactional 方法被调用时，它们将拦截器编织到调用堆栈中。
//
// 两个示例之间的细微差别在于 TransactionManager bean 的命名：在 @Bean 的情况下，名称是 “txManager”
// （根据方法的名称）；在 XML 的情况下，名称是"transactionManager" 。  硬连线默认查找名为
// “transactionManager”的bean，但是 @EnableTransactionManagement 更灵活；它将回退到容器中
// 任何 TransactionManager 的 bean 按类型查找。因此，名称可以是“txManager”、“transactionManager”
// 或“tm”：这并不重要。
//
// 对于那些希望在 @EnableTransactionManagement 和要使用的确切事务管理器 bean 之间建立更直接关系的人，
// 可以实现 TransactionManagementConfigurer 回调接口 - 请注意下面的 implements 子句
// 和 @Override -annotated 方法：
//   @Configuration
//   @EnableTransactionManagement
//   public class AppConfig implements TransactionManagementConfigurer {
//  
//       @Bean
//       public FooRepository fooRepository() {
//           // configure and return a class having @Transactional methods
//           // 配置并返回具有 @Transactional 方法的类
//           return new JdbcFooRepository(dataSource());
//       }
//  
//       @Bean
//       public DataSource dataSource() {
//           // configure and return the necessary JDBC DataSource
//           // 配置并返回必要的 JDBC DataSource
//       }
//  
//       @Bean
//       public PlatformTransactionManager txManager() {
//           return new DataSourceTransactionManager(dataSource());
//       }
//  
//       @Override
//       public PlatformTransactionManager annotationDrivenTransactionManager() {
//           return txManager();
//       }
//   }
// 这种方法可能是可取的，因为它更明确，或者为了区分同一容器中存在的两个 TransactionManager bean 可能
// 是必要的。顾名思义， annotationDrivenTransactionManager() 将用于处理 @Transactional 方法。
// 有关详细信息，请参阅 TransactionManagementConfigurer Javadoc。
//
// mode 属性控制如何应用建议：如果模式是 AdviceMode.PROXY （默认），那么其他属性控制代理的行为。请注意，
// 代理模式只允许通过代理拦截回调；同一类中的本地调用不能以这种方式被拦截。
//
// 请注意，如果模式设置为 AdviceMode.ASPECTJ ，则 proxyTargetClass 属性的值将被忽略。另请注意，在这种情况下，
// spring-aspects 模块 JAR 必须存在于类路径中，编译时编织或加载时编织将方面应用于受影响的类。这种情况下不涉及代理；
// 本地回调也将被拦截。
//
// Spring 事务 Enable 模块驱动注解 @EnableTransactionManagement
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(TransactionManagementConfigurationSelector.class)
public @interface EnableTransactionManagement {

	/**
	 * Indicate whether subclass-based (CGLIB) proxies are to be created ({@code true}) as
	 * opposed to standard Java interface-based proxies ({@code false}). The default is
	 * {@code false}. <strong>Applicable only if {@link #mode()} is set to
	 * {@link AdviceMode#PROXY}</strong>.
	 * <p>Note that setting this attribute to {@code true} will affect <em>all</em>
	 * Spring-managed beans requiring proxying, not just those marked with
	 * {@code @Transactional}. For example, other beans marked with Spring's
	 * {@code @Async} annotation will be upgraded to subclass proxying at the same
	 * time. This approach has no negative impact in practice unless one is explicitly
	 * expecting one type of proxy vs another, e.g. in tests.
	 */
	// 指示是否要创建基于子类 (CGLIB) 的代理 ( true )，而不是创建标准的基于 Java 接口的代理 ( false )。
	// 默认值为 false 。仅当 mode() 设置为 AdviceMode.PROXY 。
	//
	// 请注意，将此属性设置为 true 将影响所有需要代理的 Spring 管理的 bean，而不仅仅是标记为 @Transactional 的
	// 那些。例如，其他标有 Spring 的 @Async 注解的 bean 将同时升级为子类代理。这种方法在实践中没有负面影响，
	// 除非明确期望一种类型的代理与另一种类型的代理，例如在测试中
	boolean proxyTargetClass() default false; // 使用 JDK 动态代理

	/**
	 * Indicate how transactional advice should be applied.
	 * <p><b>The default is {@link AdviceMode#PROXY}.</b>
	 * Please note that proxy mode allows for interception of calls through the proxy
	 * only. Local calls within the same class cannot get intercepted that way; an
	 * {@link Transactional} annotation on such a method within a local call will be
	 * ignored since Spring's interceptor does not even kick in for such a runtime
	 * scenario. For a more advanced mode of interception, consider switching this to
	 * {@link AdviceMode#ASPECTJ}.
	 */
	// 指出应如何应用交易建议。
	//
	// 默认值为 AdviceMode.PROXY 。请注意，代理模式只允许通过代理拦截调用(回调)。同一类中的本地调用不能以这种方式
	// 被拦截；本地调用中此类方法的 @Transactional 注解将被忽略，因为 Spring 的拦截器甚至不会在此类运行时
	// 场景中启动。对于更高级的拦截模式，请考虑将其切换为 AdviceMode.ASPECTJ 。
	AdviceMode mode() default AdviceMode.PROXY;

	/**
	 * Indicate the ordering of the execution of the transaction advisor
	 * when multiple advices are applied at a specific joinpoint.
	 * <p>The default is {@link Ordered#LOWEST_PRECEDENCE}.
	 */
	// 当在特定连接点应用多个通知时，指示事务顾问的执行顺序。
	// 默认值为 Ordered.LOWEST_PRECEDENCE
	int order() default Ordered.LOWEST_PRECEDENCE;

}
