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

package org.springframework.cache.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.AdviceMode;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;

/**
 * Enables Spring's annotation-driven cache management capability, similar to the
 * support found in Spring's {@code <cache:*>} XML namespace. To be used together
 * with @{@link org.springframework.context.annotation.Configuration Configuration}
 * classes as follows:
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableCaching
 * public class AppConfig {
 *
 *     &#064;Bean
 *     public MyService myService() {
 *         // configure and return a class having &#064;Cacheable methods
 *         return new MyService();
 *     }
 *
 *     &#064;Bean
 *     public CacheManager cacheManager() {
 *         // configure and return an implementation of Spring's CacheManager SPI
 *         SimpleCacheManager cacheManager = new SimpleCacheManager();
 *         cacheManager.setCaches(Arrays.asList(new ConcurrentMapCache("default")));
 *         return cacheManager;
 *     }
 * }</pre>
 *
 * <p>For reference, the example above can be compared to the following Spring XML
 * configuration:
 *
 * <pre class="code">
 * &lt;beans&gt;
 *
 *     &lt;cache:annotation-driven/&gt;
 *
 *     &lt;bean id="myService" class="com.foo.MyService"/&gt;
 *
 *     &lt;bean id="cacheManager" class="org.springframework.cache.support.SimpleCacheManager"&gt;
 *         &lt;property name="caches"&gt;
 *             &lt;set&gt;
 *                 &lt;bean class="org.springframework.cache.concurrent.ConcurrentMapCacheFactoryBean"&gt;
 *                     &lt;property name="name" value="default"/&gt;
 *                 &lt;/bean&gt;
 *             &lt;/set&gt;
 *         &lt;/property&gt;
 *     &lt;/bean&gt;
 *
 * &lt;/beans&gt;
 * </pre>
 *
 * In both of the scenarios above, {@code @EnableCaching} and {@code
 * <cache:annotation-driven/>} are responsible for registering the necessary Spring
 * components that power annotation-driven cache management, such as the
 * {@link org.springframework.cache.interceptor.CacheInterceptor CacheInterceptor} and the
 * proxy- or AspectJ-based advice that weaves the interceptor into the call stack when
 * {@link org.springframework.cache.annotation.Cacheable @Cacheable} methods are invoked.
 *
 * <p>If the JSR-107 API and Spring's JCache implementation are present, the necessary
 * components to manage standard cache annotations are also registered. This creates the
 * proxy- or AspectJ-based advice that weaves the interceptor into the call stack when
 * methods annotated with {@code CacheResult}, {@code CachePut}, {@code CacheRemove} or
 * {@code CacheRemoveAll} are invoked.
 *
 * <p><strong>A bean of type {@link org.springframework.cache.CacheManager CacheManager}
 * must be registered</strong>, as there is no reasonable default that the framework can
 * use as a convention. And whereas the {@code <cache:annotation-driven>} element assumes
 * a bean <em>named</em> "cacheManager", {@code @EnableCaching} searches for a cache
 * manager bean <em>by type</em>. Therefore, naming of the cache manager bean method is
 * not significant.
 *
 * <p>For those that wish to establish a more direct relationship between
 * {@code @EnableCaching} and the exact cache manager bean to be used,
 * the {@link CachingConfigurer} callback interface may be implemented.
 * Notice the {@code @Override}-annotated methods below:
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableCaching
 * public class AppConfig extends CachingConfigurerSupport {
 *
 *     &#064;Bean
 *     public MyService myService() {
 *         // configure and return a class having &#064;Cacheable methods
 *         return new MyService();
 *     }
 *
 *     &#064;Bean
 *     &#064;Override
 *     public CacheManager cacheManager() {
 *         // configure and return an implementation of Spring's CacheManager SPI
 *         SimpleCacheManager cacheManager = new SimpleCacheManager();
 *         cacheManager.setCaches(Arrays.asList(new ConcurrentMapCache("default")));
 *         return cacheManager;
 *     }
 *
 *     &#064;Bean
 *     &#064;Override
 *     public KeyGenerator keyGenerator() {
 *         // configure and return an implementation of Spring's KeyGenerator SPI
 *         return new MyKeyGenerator();
 *     }
 * }</pre>
 *
 * This approach may be desirable simply because it is more explicit, or it may be
 * necessary in order to distinguish between two {@code CacheManager} beans present in the
 * same container.
 *
 * <p>Notice also the {@code keyGenerator} method in the example above. This allows for
 * customizing the strategy for cache key generation, per Spring's {@link
 * org.springframework.cache.interceptor.KeyGenerator KeyGenerator} SPI. Normally,
 * {@code @EnableCaching} will configure Spring's
 * {@link org.springframework.cache.interceptor.SimpleKeyGenerator SimpleKeyGenerator}
 * for this purpose, but when implementing {@code CachingConfigurer}, a key generator
 * must be provided explicitly. Return {@code null} or {@code new SimpleKeyGenerator()}
 * from this method if no customization is necessary.
 *
 * <p>{@link CachingConfigurer} offers additional customization options: it is recommended
 * to extend from {@link org.springframework.cache.annotation.CachingConfigurerSupport
 * CachingConfigurerSupport} that provides a default implementation for all methods which
 * can be useful if you do not need to customize everything. See {@link CachingConfigurer}
 * Javadoc for further details.
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
 * @see CachingConfigurer
 * @see CachingConfigurationSelector
 * @see ProxyCachingConfiguration
 * @see org.springframework.cache.aspectj.AspectJCachingConfiguration
 */
// 启用 Spring 的注解驱动缓存管理功能，类似于 Spring 的支持  XML 命名空间。与Configuration类一起使用如下：
//   @Configuration
//   @EnableCaching
//   public class AppConfig {
//  
//       @Bean
//       public MyService myService() {
//           // configure and return a class having @Cacheable methods
//           // 配置并返回具有@Cacheable 方法的类
//           return new MyService();
//       }
//  
//       @Bean
//       public CacheManager cacheManager() {
//           // configure and return an implementation of Spring's CacheManager SPI
//           // 配置并返回 Spring 的 CacheManager SPI 的实现
//           SimpleCacheManager cacheManager = new SimpleCacheManager();
//           cacheManager.setCaches(Arrays.asList(new ConcurrentMapCache("default")));
//           return cacheManager;
//       }
//   }
// 作为参考，可以将上面的示例与以下 Spring XML 配置进行比较：
//   <beans>
//  
//       <cache:annotation-driven/>
//  
//       <bean id="myService" class="com.foo.MyService"/>
//  
//       <bean id="cacheManager" class="org.springframework.cache.support.SimpleCacheManager">
//           <property name="caches">
//               <set>
//                   <bean class="org.springframework.cache.concurrent.ConcurrentMapCacheFactoryBean">
//                       <property name="name" value="default"/>
//                   </bean>
//               </set>
//           </property>
//       </bean>
//  
//   </beans>
//   
//
// 在上述两种情况下， @EnableCaching和 负责注册支持注解驱动的缓存管理的必要 Spring 组件，例如 CacheInterceptor 和
// 基于代理或 AspectJ 的建议，当调用 @Cacheable 方法时将拦截器编织到调用堆栈中。
//
// 如果存在 JSR-107 API 和 Spring 的 JCache 实现，则还注册了管理标准缓存注解的必要组件。当调用
// 带有 CacheResult 、 CachePut 、 CacheRemove 或 CacheRemoveAll 注解的方法时，这将创建基于代理
// 或 AspectJ 的建议，将拦截器编织到调用堆栈中。
//
// 必须注册 CacheManager 类型的 bean ，因为框架没有可以用作约定的合理默认值。而虽然元素假定一个
// 名为 “cacheManager” 的 bean， @EnableCaching 按类型搜索缓存管理器 bean。因此，缓存管理器 bean 方法的命名并不重要。
//
// 对于那些希望在 @EnableCaching 和要使用的确切缓存管理器 bean 之间建立更直接关系的人，可以实现 CachingConfigurer 回调接口。
// 注意下面的 @Override -annotated 方法：
//   @Configuration
//   @EnableCaching
//   public class AppConfig extends CachingConfigurerSupport {
//  
//       @Bean
//       public MyService myService() {
//           // configure and return a class having @Cacheable methods
//           return new MyService();
//       }
//  
//       @Bean
//       @Override
//       public CacheManager cacheManager() {
//           // configure and return an implementation of Spring's CacheManager SPI
//           SimpleCacheManager cacheManager = new SimpleCacheManager();
//           cacheManager.setCaches(Arrays.asList(new ConcurrentMapCache("default")));
//           return cacheManager;
//       }
//  
//       @Bean
//       @Override
//       public KeyGenerator keyGenerator() {
//           // configure and return an implementation of Spring's KeyGenerator SPI
//           return new MyKeyGenerator();
//       }
//   }
//
// 这种方法可能是可取的，因为它更明确，或者为了区分同一容器中存在的两个 CacheManager bean 可能是必要的。
//
// 还要注意上面示例中的 keyGenerator方法。这允许根据 Spring 的 KeyGenerator SPI 自定义缓存键生成策略。
// 通常， @EnableCaching 会为此配置 Spring 的 SimpleKeyGenerator ，但在实现 CachingConfigurer 时，
// 必须显式提供密钥生成器。如果不需要自定义，则从此方法返回 null 或 new SimpleKeyGenerator() 。
//
// CachingConfigurer 提供额外的自定义选项：建议从 CachingConfigurerSupport 扩展，它为所有方法提供默认实现，
// 如果您不需要自定义所有方法，这可能很有用。有关详细信息，请参阅 CachingConfigurer Javadoc。
//
// mode 属性控制如何应用建议：如果模式是 AdviceMode.PROXY （默认），那么其他属性控制代理的行为。请注意，代理模式
// 只允许通过代理拦截呼叫；同一类中的本地调用不能以这种方式被拦截。
//
// 请注意，如果模式设置为 AdviceMode.ASPECTJ ，则 proxyTargetClass 属性的值将被忽略。另请注意，在这种情况下， 
// spring-aspects模块 JAR 必须存在于类路径中，编译时编织或加载时编织将方面应用于受影响的类。这种情况下不涉及代理；
// 本地回调也将被拦截。
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(CachingConfigurationSelector.class)
public @interface EnableCaching {

	/**
	 * Indicate whether subclass-based (CGLIB) proxies are to be created as opposed
	 * to standard Java interface-based proxies. The default is {@code false}. <strong>
	 * Applicable only if {@link #mode()} is set to {@link AdviceMode#PROXY}</strong>.
	 * <p>Note that setting this attribute to {@code true} will affect <em>all</em>
	 * Spring-managed beans requiring proxying, not just those marked with {@code @Cacheable}.
	 * For example, other beans marked with Spring's {@code @Transactional} annotation will
	 * be upgraded to subclass proxying at the same time. This approach has no negative
	 * impact in practice unless one is explicitly expecting one type of proxy vs another,
	 * e.g. in tests.
	 */
	// 指示是否要创建基于子类 (CGLIB) 的代理，而不是创建标准的基于 Java 接口的代理。
	// 默认值为 false 。仅当 mode()设置为 AdviceMode.PROXY 。
	// 
	// 请注意，将此属性设置为 true 将影响所有需要代理的 Spring 管理的 bean，而
	// 不仅仅是标记有 @Cacheable的那些。例如，其他标有 Spring 的 @Transactional 注解的 bean 将
	// 同时升级为子类代理。这种方法在实践中没有负面影响，除非明确期望一种类型的代理与另一种类型的代理，例如在测试中。
	boolean proxyTargetClass() default false;

	/**
	 * Indicate how caching advice should be applied.
	 * <p><b>The default is {@link AdviceMode#PROXY}.</b>
	 * Please note that proxy mode allows for interception of calls through the proxy
	 * only. Local calls within the same class cannot get intercepted that way;
	 * a caching annotation on such a method within a local call will be ignored
	 * since Spring's interceptor does not even kick in for such a runtime scenario.
	 * For a more advanced mode of interception, consider switching this to
	 * {@link AdviceMode#ASPECTJ}.
	 */
	// 指示应如何应用缓存建议。
	//
	// 默认值为 AdviceMode.PROXY 。请注意，代理模式只允许通过代理拦截呼叫。同一类中的本地调用不能
	// 以这种方式被拦截；本地调用中此类方法上的缓存注解将被忽略，因为 Spring 的拦截器甚至不会在此类运行时场景中启动。
	// 对于更高级的拦截模式，请考虑将其切换为 AdviceMode.ASPECTJ 。
	AdviceMode mode() default AdviceMode.PROXY;

	/**
	 * Indicate the ordering of the execution of the caching advisor
	 * when multiple advices are applied at a specific joinpoint.
	 * <p>The default is {@link Ordered#LOWEST_PRECEDENCE}.
	 */
	// 当在特定连接点应用多个建议时，指示缓存顾问的执行顺序。
	// 默认值为 Ordered.LOWEST_PRECEDENCE 。
	int order() default Ordered.LOWEST_PRECEDENCE;

}
