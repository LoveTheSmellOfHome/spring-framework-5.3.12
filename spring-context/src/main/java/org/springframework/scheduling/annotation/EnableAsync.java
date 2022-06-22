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

package org.springframework.scheduling.annotation;

import org.springframework.context.annotation.AdviceMode;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;

import java.lang.annotation.*;

/**
 * Enables Spring's asynchronous method execution capability, similar to functionality
 * found in Spring's {@code <task:*>} XML namespace.
 *
 * <p>To be used together with @{@link Configuration Configuration} classes as follows,
 * enabling annotation-driven async processing for an entire Spring application context:
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableAsync
 * public class AppConfig {
 *
 * }</pre>
 *
 * {@code MyAsyncBean} is a user-defined type with one or more methods annotated with
 * either Spring's {@code @Async} annotation, the EJB 3.1 {@code @javax.ejb.Asynchronous}
 * annotation, or any custom annotation specified via the {@link #annotation} attribute.
 * The aspect is added transparently for any registered bean, for instance via this
 * configuration:
 *
 * <pre class="code">
 * &#064;Configuration
 * public class AnotherAppConfig {
 *
 *     &#064;Bean
 *     public MyAsyncBean asyncBean() {
 *         return new MyAsyncBean();
 *     }
 * }</pre>
 *
 * <p>By default, Spring will be searching for an associated thread pool definition:
 * either a unique {@link org.springframework.core.task.TaskExecutor} bean in the context,
 * or an {@link java.util.concurrent.Executor} bean named "taskExecutor" otherwise. If
 * neither of the two is resolvable, a {@link org.springframework.core.task.SimpleAsyncTaskExecutor}
 * will be used to process async method invocations. Besides, annotated methods having a
 * {@code void} return type cannot transmit any exception back to the caller. By default,
 * such uncaught exceptions are only logged.
 *
 * <p>To customize all this, implement {@link AsyncConfigurer} and provide:
 * <ul>
 * <li>your own {@link java.util.concurrent.Executor Executor} through the
 * {@link AsyncConfigurer#getAsyncExecutor getAsyncExecutor()} method, and</li>
 * <li>your own {@link org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler
 * AsyncUncaughtExceptionHandler} through the {@link AsyncConfigurer#getAsyncUncaughtExceptionHandler
 * getAsyncUncaughtExceptionHandler()}
 * method.</li>
 * </ul>
 *
 * <p><b>NOTE: {@link AsyncConfigurer} configuration classes get initialized early
 * in the application context bootstrap. If you need any dependencies on other beans
 * there, make sure to declare them 'lazy' as far as possible in order to let them
 * go through other post-processors as well.</b>
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableAsync
 * public class AppConfig implements AsyncConfigurer {
 *
 *     &#064;Override
 *     public Executor getAsyncExecutor() {
 *         ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
 *         executor.setCorePoolSize(7);
 *         executor.setMaxPoolSize(42);
 *         executor.setQueueCapacity(11);
 *         executor.setThreadNamePrefix("MyExecutor-");
 *         executor.initialize();
 *         return executor;
 *     }
 *
 *     &#064;Override
 *     public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
 *         return new MyAsyncUncaughtExceptionHandler();
 *     }
 * }</pre>
 *
 * <p>If only one item needs to be customized, {@code null} can be returned to
 * keep the default settings. Consider also extending from {@link AsyncConfigurerSupport}
 * when possible.
 *
 * <p>Note: In the above example the {@code ThreadPoolTaskExecutor} is not a fully managed
 * Spring bean. Add the {@code @Bean} annotation to the {@code getAsyncExecutor()} method
 * if you want a fully managed bean. In such circumstances it is no longer necessary to
 * manually call the {@code executor.initialize()} method as this will be invoked
 * automatically when the bean is initialized.
 *
 * <p>For reference, the example above can be compared to the following Spring XML
 * configuration:
 *
 * <pre class="code">
 * &lt;beans&gt;
 *
 *     &lt;task:annotation-driven executor="myExecutor" exception-handler="exceptionHandler"/&gt;
 *
 *     &lt;task:executor id="myExecutor" pool-size="7-42" queue-capacity="11"/&gt;
 *
 *     &lt;bean id="asyncBean" class="com.foo.MyAsyncBean"/&gt;
 *
 *     &lt;bean id="exceptionHandler" class="com.foo.MyAsyncUncaughtExceptionHandler"/&gt;
 *
 * &lt;/beans&gt;
 * </pre>
 *
 * The above XML-based and JavaConfig-based examples are equivalent except for the
 * setting of the <em>thread name prefix</em> of the {@code Executor}; this is because
 * the {@code <task:executor>} element does not expose such an attribute. This
 * demonstrates how the JavaConfig-based approach allows for maximum configurability
 * through direct access to actual componentry.
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
 * @author Stephane Nicoll
 * @author Sam Brannen
 * @since 3.1
 * @see Async
 * @see AsyncConfigurer
 * @see AsyncConfigurationSelector
 */
// 启用 Spring 的异步方法执行功能，类似于 Spring 中 XML 命名空间的功能 <task:*>
// 要配合 @Configuration 使用，如下类，使注解驱动异步处理对整个 Spring 应用程序上下文生效：
//   @Configuration
//   @EnableAsync
//   public class AppConfig {
//
//   }
// MyAsyncBean 是一种用户自定义类型，具有一个或多个使用诸如 Spring 的 @Async 注解、
// EJB 3.1 @javax.ejb.Asynchronous 注解或通过 annotation 属性指定的任何自定义注解
// 进行批注的方法。对于任何已注册的 bean，该切面都是透明地添加的，例如通过以下配置：
// 	 @Configuration
//   public class AnotherAppConfig {
//
//       @Bean
//       public MyAsyncBean asyncBean() {
//           return new MyAsyncBean();
//       }
//   }
// 默认情况下，Spring 将搜索关联的线程池定义：上下文中唯一的
// org.springframework.core.task.TaskExecutor bean，或者名为“taskExecutor”的
// java.util.concurrent.Executor bean。如果两者都不可解析，则将使用
// org.springframework.core.task.SimpleAsyncTaskExecutor 来处理异步方法调用。
// 此外，具有 void 返回类型的带注解的方法不能将任何异常传输回调用者。
// 默认情况下，仅记录此类未捕获的异常。
// 要自定义所有这些，请实现 AsyncConfigurer 并提供：
// <li>你自己的{@link java.util.concurrent.Executor Executor}通过
// {@link AsyncConfigurergetAsyncExecutor #getAsyncExecutor()}方法，以及<li>
// <li>你自己的{@link org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler
// AsyncUncaughtExceptionHandler} 通过 {@link AsyncConfigurergetAsyncUncaughtExceptionHandler
// #getAsyncUncaughtExceptionHandler()} 方法。<li> <ul>
// 注意： AsyncConfigurer 配置类在应用程序上下文引导程序的早期被初始化。如果您需要对其他 bean 的任何依赖，
// 请确保尽可能将它们声明为“惰性”，以便让它们也通过其他后处理器
//   @Configuration
//   @EnableAsync
//   public class AppConfig implements AsyncConfigurer {
//
//       @Override
//       public Executor getAsyncExecutor() {
//           ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
//           executor.setCorePoolSize(7);
//           executor.setMaxPoolSize(42);
//           executor.setQueueCapacity(11);
//           executor.setThreadNamePrefix("MyExecutor-");
//           executor.initialize();
//           return executor;
//       }
//
//       @Override
//       public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
//           return new MyAsyncUncaughtExceptionHandler();
//       }
//   }
// 如果只需要定制一项，可以返回null，保持默认设置。如果可能，还考虑从 AsyncConfigurerSupport 扩展。
// 注意：在上面的例子中，ThreadPoolTaskExecutor 不是一个完全托管的 Spring bean。如果需要完全托管的 bean，
// 请将 @Bean 注释添加到 getAsyncExecutor() 方法。在这种情况下，不再需要手动调用 executor.initialize() 方法，
// 因为它会在 bean 初始化时自动调用。
// 作为参考，可以将上面的示例与以下 Spring XML 配置进行比较：
// <beans>
//
//       <task:annotation-driven executor="myExecutor" exception-handler="exceptionHandler"/>
//
//       <task:executor id="myExecutor" pool-size="7-42" queue-capacity="11"/>
//
//       <bean id="asyncBean" class="com.foo.MyAsyncBean"/>
//
//       <bean id="exceptionHandler" class="com.foo.MyAsyncUncaughtExceptionHandler"/>
//
//   </beans>
// 上面基于XML和基于JavaConfig的例子是等价的，只是设置了 Executor 的线程名前缀；这是因为该元素没有公开这样的属性。
// 这演示了基于 JavaConfig 的方法如何通过直接访问实际组件来实现最大的可配置性。
// mode 属性控制如何应用建议：如果模式是 AdviceMode.PROXY（默认），则其他属性控制代理的行为。
// 请注意，代理模式只允许通过代理拦截调用；不能以这种方式拦截同一类中的本地调用。
// 请注意，如果模式设置为 AdviceMode.ASPECTJ，则 proxyTargetClass 属性的值将被忽略。
// 还要注意，在这种情况下，spring-aspects 模块 JAR 必须存在于类路径中，编译时编织或加载时编织将方面应用于受影响的类。
// 这种场景不涉及代理；本地调用也会被拦截。
//
// 核心 API :激活异步
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(AsyncConfigurationSelector.class)
public @interface EnableAsync {

	/**
	 * Indicate the 'async' annotation type to be detected at either class
	 * or method level.
	 * <p>By default, both Spring's @{@link Async} annotation and the EJB 3.1
	 * {@code @javax.ejb.Asynchronous} annotation will be detected.
	 * <p>This attribute exists so that developers can provide their own
	 * custom annotation type to indicate that a method (or all methods of
	 * a given class) should be invoked asynchronously.
	 */
	// 指示要在类或方法级别检测到的“异步”注释类型
	// 默认情况下，Spring 的 @Async注释和 EJB 3.1 @javax.ejb.Asynchronous注解都会被检测到。
	// 存在此属性，以便开发人员可以提供自己的自定义注释类型，以指示应异步调用方法（或给定类的所有方法）。
	Class<? extends Annotation> annotation() default Annotation.class;

	/**
	 * Indicate whether subclass-based (CGLIB) proxies are to be created as opposed
	 * to standard Java interface-based proxies.
	 * <p><strong>Applicable only if the {@link #mode} is set to {@link AdviceMode#PROXY}</strong>.
	 * <p>The default is {@code false}.
	 * <p>Note that setting this attribute to {@code true} will affect <em>all</em>
	 * Spring-managed beans requiring proxying, not just those marked with {@code @Async}.
	 * For example, other beans marked with Spring's {@code @Transactional} annotation
	 * will be upgraded to subclass proxying at the same time. This approach has no
	 * negative impact in practice unless one is explicitly expecting one type of proxy
	 * vs. another &mdash; for example, in tests.
	 */
	// 指示是否要创建基于子类 (CGLIB) 的代理，而不是基于标准 Java 接口的代理。
	// 仅当mode设置为AdviceMode.PROXY
	// 默认值为false 。
	// 请注意，将此属性设置为true将影响所有需要代理的 Spring 管理的 bean，而不仅仅是那些标记为@Async 。
	// 例如，其他标注了 Spring 的@Transactional注解的 bean 会同时升级为子类代理。
	// 这种方法在实践中没有负面影响，除非人们明确期望一种类型的代理与另一种类型的代理——例如，在测试中。
	boolean proxyTargetClass() default false;

	/**
	 * Indicate how async advice should be applied.
	 * <p><b>The default is {@link AdviceMode#PROXY}.</b>
	 * Please note that proxy mode allows for interception of calls through the proxy
	 * only. Local calls within the same class cannot get intercepted that way; an
	 * {@link Async} annotation on such a method within a local call will be ignored
	 * since Spring's interceptor does not even kick in for such a runtime scenario.
	 * For a more advanced mode of interception, consider switching this to
	 * {@link AdviceMode#ASPECTJ}.
	 */
	// 指示应如何应用异步建议
	// 默认值为AdviceMode.PROXY 。 请注意，代理模式只允许通过代理拦截调用。 不能以这种方式拦截同一类中的本地调用；
	// 本地调用中此类方法上的 @Async 注解将被忽略，因为 Spring 的拦截器甚至不会针对此类运行时场景启动。
	// 对于更高级的拦截模式，请考虑将其切换为AdviceMode.ASPECTJ 。
	AdviceMode mode() default AdviceMode.PROXY;

	/**
	 * Indicate the order in which the {@link AsyncAnnotationBeanPostProcessor}
	 * should be applied.
	 * <p>The default is {@link Ordered#LOWEST_PRECEDENCE} in order to run
	 * after all other post-processors, so that it can add an advisor to
	 * existing proxies rather than double-proxy.
	 */
	// 指示应应用 AsyncAnnotationBeanPostProcessor 的顺序。
	int order() default Ordered.LOWEST_PRECEDENCE;

}
