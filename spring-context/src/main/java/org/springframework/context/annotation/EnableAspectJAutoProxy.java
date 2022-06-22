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

/**
 * Enables support for handling components marked with AspectJ's {@code @Aspect} annotation,
 * similar to functionality found in Spring's {@code <aop:aspectj-autoproxy>} XML element.
 * To be used on @{@link Configuration} classes as follows:
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableAspectJAutoProxy
 * public class AppConfig {
 *
 *     &#064;Bean
 *     public FooService fooService() {
 *         return new FooService();
 *     }
 *
 *     &#064;Bean
 *     public MyAspect myAspect() {
 *         return new MyAspect();
 *     }
 * }</pre>
 *
 * Where {@code FooService} is a typical POJO component and {@code MyAspect} is an
 * {@code @Aspect}-style aspect:
 *
 * <pre class="code">
 * public class FooService {
 *
 *     // various methods
 * }</pre>
 *
 * <pre class="code">
 * &#064;Aspect
 * public class MyAspect {
 *
 *     &#064;Before("execution(* FooService+.*(..))")
 *     public void advice() {
 *         // advise FooService methods as appropriate
 *     }
 * }</pre>
 *
 * In the scenario above, {@code @EnableAspectJAutoProxy} ensures that {@code MyAspect}
 * will be properly processed and that {@code FooService} will be proxied mixing in the
 * advice that it contributes.
 *
 * <p>Users can control the type of proxy that gets created for {@code FooService} using
 * the {@link #proxyTargetClass()} attribute. The following enables CGLIB-style 'subclass'
 * proxies as opposed to the default interface-based JDK proxy approach.
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableAspectJAutoProxy(proxyTargetClass=true)
 * public class AppConfig {
 *     // ...
 * }</pre>
 *
 * <p>Note that {@code @Aspect} beans may be component-scanned like any other.
 * Simply mark the aspect with both {@code @Aspect} and {@code @Component}:
 *
 * <pre class="code">
 * package com.foo;
 *
 * &#064;Component
 * public class FooService { ... }
 *
 * &#064;Aspect
 * &#064;Component
 * public class MyAspect { ... }</pre>
 *
 * Then use the @{@link ComponentScan} annotation to pick both up:
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;ComponentScan("com.foo")
 * &#064;EnableAspectJAutoProxy
 * public class AppConfig {
 *
 *     // no explicit &#064;Bean definitions required
 * }</pre>
 *
 * <b>Note: {@code @EnableAspectJAutoProxy} applies to its local application context only,
 * allowing for selective proxying of beans at different levels.</b> Please redeclare
 * {@code @EnableAspectJAutoProxy} in each individual context, e.g. the common root web
 * application context and any separate {@code DispatcherServlet} application contexts,
 * if you need to apply its behavior at multiple levels.
 *
 * <p>This feature requires the presence of {@code aspectjweaver} on the classpath.
 * While that dependency is optional for {@code spring-aop} in general, it is required
 * for {@code @EnableAspectJAutoProxy} and its underlying facilities.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 3.1
 * @see org.aspectj.lang.annotation.Aspect
 */
// 支持处理标有 AspectJ 的@Aspect注解的组件，类似于 Spring 中的功能   XML 元素。 要使用@上Configuration类，如下所示：
//   @Configuration
//   @EnableAspectJAutoProxy
//   public class AppConfig {
//  
//       @Bean
//       public FooService fooService() {
//           return new FooService();
//       }
//  
//       @Bean
//       public MyAspect myAspect() {
//           return new MyAspect();
//       }
//   }
// 其中FooService是一个典型的 POJO 组件，而MyAspect是一个@Aspect风格的切面：
//   public class FooService {
//  
//       // various methods
//   }
//   @Aspect
//   public class MyAspect {
//  
//       @Before("execution(* FooService+.*(..))")
//       public void advice() {
//           // advise FooService methods as appropriate 根据需要建议(操作/执行) FooService 方法
//       }
//   }
// 在上面的场景中， @EnableAspectJAutoProxy 确保 MyAspect 将被正确处理，并且 FooService 将被代理混合在它提供的建议中。
// 用户可以使用proxyTargetClass()属性控制为FooService创建的代理类型。 以下启用了 CGLIB 样式的“子类”代理，
// 而不是基于默认接口的 JDK 代理方法。
//   @Configuration
//   @EnableAspectJAutoProxy(proxyTargetClass=true)
//   public class AppConfig {
//       // ...
//   }
//请注意，@ @Aspect bean 可以像任何其他 bean 一样进行组件扫描。 简单地用 @Aspect 和 @Component 标记切面：
//   package com.foo;
//  
//   @Component
//   public class FooService { ... }
//  
//   @Aspect
//   @Component
//   public class MyAspect { ... }
// 然后使用 @ComponentScan 注解扫描：
//   @Configuration
//   @ComponentScan("com.foo")
//   @EnableAspectJAutoProxy
//   public class AppConfig {
//  
//       // no explicit @Bean definitions required 不需要明确的@Bean 定义
//   }
// 注意： @EnableAspectJAutoProxy 仅适用于其本地应用程序上下文，允许在不同级别对 bean 进行选择性代理。
// 如果您需要在多个级别应用其行为，请在每个单独的上下文中重新声明 @EnableAspectJAutoProxy ，例如公共根 Web 应用
// 程序上下文和任何单独的 DispatcherServlet 应用程序上下文。
//
// 此功能需要在类路径上存在 aspectjweaver 。 虽然该依赖项通常对于spring-aop是可选的，但 @EnableAspectJAutoProxy
// 及其基础组件需要它。
//
// 这个注解的底层实现原理是 {@link AnnotationAwareAspectJAutoProxyCreator }
// 它的设计模式是通过 Enable 模块驱动来实现的，它的实现是通过 ImportBeanDefinitionRegistrar 来实现的
// 只能标注在类或接口上，通常 Enable 模块只能标注在 类，接口，枚举上，保留策略在运行时，确保反射能被获取到。
// 再看 @Import 即它的实现 AspectJAutoProxyRegistrar，两个属性方法
//
// 激活 Spring AOP 特性
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(AspectJAutoProxyRegistrar.class)
public @interface EnableAspectJAutoProxy {

	/**
	 * Indicate whether subclass-based (CGLIB) proxies are to be created as opposed
	 * to standard Java interface-based proxies. The default is {@code false}.
	 */
	// 指示是否要创建基于 (CGLIB) 子类的代理，而不是基于标准 Java 接口的代理。
	// 默认值为 false,也就是说默认使用 JDK 动态代理基于接口实现
	// 是否将类型作为代理：否默认使用 JDK 动态代理，即接口代理的方式
	boolean proxyTargetClass() default false;

	/**
	 * Indicate that the proxy should be exposed by the AOP framework as a {@code ThreadLocal}
	 * for retrieval via the {@link org.springframework.aop.framework.AopContext} class.
	 * Off by default, i.e. no guarantees that {@code AopContext} access will work.
	 * @since 4.3.1
	 */
	// 指示代理应由 AOP 框架公开为 ThreadLocal 以通过 org.springframework.aop.framework.AopContext
	// 类进行检索。 默认关闭，即不保证 AopContext 访问将起作用。
	// 是否将代理对象暴露在 AopContext 中，如果打开就会将当前对象存放在基于线程级别的 ThreadLocal 中，这个对象
	// 本身是不能跨现成的，一般用于多线程环境。
	boolean exposeProxy() default false;

}
