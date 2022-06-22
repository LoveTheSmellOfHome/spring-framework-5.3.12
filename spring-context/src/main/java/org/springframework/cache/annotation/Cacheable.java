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

package org.springframework.cache.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.Callable;

import org.springframework.core.annotation.AliasFor;

/**
 * Annotation indicating that the result of invoking a method (or all methods
 * in a class) can be cached.
 *
 * <p>Each time an advised method is invoked, caching behavior will be applied,
 * checking whether the method has been already invoked for the given arguments.
 * A sensible default simply uses the method parameters to compute the key, but
 * a SpEL expression can be provided via the {@link #key} attribute, or a custom
 * {@link org.springframework.cache.interceptor.KeyGenerator} implementation can
 * replace the default one (see {@link #keyGenerator}).
 *
 * <p>If no value is found in the cache for the computed key, the target method
 * will be invoked and the returned value will be stored in the associated cache.
 * Note that {@link java.util.Optional} return types are unwrapped automatically.
 * If an {@code Optional} value is {@linkplain java.util.Optional#isPresent()
 * present}, it will be stored in the associated cache. If an {@code Optional}
 * value is not present, {@code null} will be stored in the associated cache.
 *
 * <p>This annotation may be used as a <em>meta-annotation</em> to create custom
 * <em>composed annotations</em> with attribute overrides.
 *
 * @author Costin Leau
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @author Sam Brannen
 * @since 3.1
 * @see CacheConfig
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
// 指示调用方法（或类中的所有方法）的结果可以被缓存的注解。
//
// 每次调用建议的方法时，都会应用缓存行为，检查是否已经为给定的参数调用了该方法。合理的默认值只是使用方法参数来计算密钥，
// 但是可以通过 key 属性提供 SpEL 表达式，或者自定义 org.springframework.cache.interceptor.KeyGenerator
// 实现可以替换默认值（参见keyGenerator ）。
//
// 如果在缓存中没有找到计算键的值，则将调用目标方法并将返回的值存储在关联的缓存中。请注意， java.util.Optional 返回类型会自动展开。
// 如果存在 Optional 值，它将存储在关联的缓存中。如果不存在 Optional 值，则 null 将存储在关联的缓存中。
//
// 此注解可用作元注解以创建具有属性覆盖的自定义组合注解。
public @interface Cacheable {

	/**
	 * Alias for {@link #cacheNames}.
	 */
	// cacheNames的别名。
	@AliasFor("cacheNames")
	String[] value() default {};

	/**
	 * Names of the caches in which method invocation results are stored.
	 * <p>Names may be used to determine the target cache (or caches), matching
	 * the qualifier value or bean name of a specific bean definition.
	 * @since 4.2
	 * @see #value
	 * @see CacheConfig#cacheNames
	 */
	// 存储方法调用结果的缓存的名称。
	// 名称可用于确定目标缓存（或多个缓存），匹配特定 bean 定义的限定符值或 bean 名称。
	@AliasFor("value")
	String[] cacheNames() default {};

	/**
	 * Spring Expression Language (SpEL) expression for computing the key dynamically.
	 * <p>Default is {@code ""}, meaning all method parameters are considered as a key,
	 * unless a custom {@link #keyGenerator} has been configured.
	 * <p>The SpEL expression evaluates against a dedicated context that provides the
	 * following meta-data:
	 * <ul>
	 * <li>{@code #root.method}, {@code #root.target}, and {@code #root.caches} for
	 * references to the {@link java.lang.reflect.Method method}, target object, and
	 * affected cache(s) respectively.</li>
	 * <li>Shortcuts for the method name ({@code #root.methodName}) and target class
	 * ({@code #root.targetClass}) are also available.
	 * <li>Method arguments can be accessed by index. For instance the second argument
	 * can be accessed via {@code #root.args[1]}, {@code #p1} or {@code #a1}. Arguments
	 * can also be accessed by name if that information is available.</li>
	 * </ul>
	 */
	// 用于动态计算密钥的 Spring 表达式语言 (SpEL) 表达式。
	//
	// 默认为"" ，这意味着所有方法参数都被视为一个键，除非配置了自定义keyGenerator 。
	//
	// SpEL 表达式根据提供以下元数据的专用上下文进行评估：
	//  > #root.method 、 #root.target 和 #root.caches 分别用于对 method 、目标对象和受影响缓存的引用。
	//  > 方法名称 ( #root.methodName ) 和目标类 ( #root.targetClass ) 的快捷方式也可用。
	//  > 方法参数可以通过索引访问。例如，可以通过#root.args[1] 、 #p1 或 #a1 访问第二个参数。如果该信息可用，
	//    也可以按名称访问参数。
	String key() default "";

	/**
	 * The bean name of the custom {@link org.springframework.cache.interceptor.KeyGenerator}
	 * to use.
	 * <p>Mutually exclusive with the {@link #key} attribute.
	 * @see CacheConfig#keyGenerator
	 */
	// 要使用的自定义 org.springframework.cache.interceptor.KeyGenerator 的 bean 名称。
	// 与 key 属性互斥。
	// 请参阅：
	//			CacheConfig.keyGenerator
	String keyGenerator() default "";

	/**
	 * The bean name of the custom {@link org.springframework.cache.CacheManager} to use to
	 * create a default {@link org.springframework.cache.interceptor.CacheResolver} if none
	 * is set already.
	 * <p>Mutually exclusive with the {@link #cacheResolver}  attribute.
	 * @see org.springframework.cache.interceptor.SimpleCacheResolver
	 * @see CacheConfig#cacheManager
	 */
	// 自定义 org.springframework.cache.CacheManager 的 bean 名称，用于创建默认
	// org.springframework.cache.interceptor.CacheResolver （如果尚未设置）。
	//
	// 与 cacheResolver 属性互斥。
	// 请参阅：
	//			org.springframework.cache.interceptor.SimpleCacheResolver , CacheConfig.cacheManager
	String cacheManager() default "";

	/**
	 * The bean name of the custom {@link org.springframework.cache.interceptor.CacheResolver}
	 * to use.
	 * @see CacheConfig#cacheResolver
	 */
	// 要使用的自定义 org.springframework.cache.interceptor.CacheResolver 的 bean 名称。
	// 请参阅：
	//			CacheConfig.cacheResolver
	String cacheResolver() default "";

	/**
	 * Spring Expression Language (SpEL) expression used for making the method
	 * caching conditional.
	 * <p>Default is {@code ""}, meaning the method result is always cached.
	 * <p>The SpEL expression evaluates against a dedicated context that provides the
	 * following meta-data:
	 * <ul>
	 * <li>{@code #root.method}, {@code #root.target}, and {@code #root.caches} for
	 * references to the {@link java.lang.reflect.Method method}, target object, and
	 * affected cache(s) respectively.</li>
	 * <li>Shortcuts for the method name ({@code #root.methodName}) and target class
	 * ({@code #root.targetClass}) are also available.
	 * <li>Method arguments can be accessed by index. For instance the second argument
	 * can be accessed via {@code #root.args[1]}, {@code #p1} or {@code #a1}. Arguments
	 * can also be accessed by name if that information is available.</li>
	 * </ul>
	 */
	// Spring 表达式语言 (SpEL) 表达式用于使方法缓存有条件。
	//
	// 默认为"" ，表示方法结果始终被缓存。
	//
	// SpEL 表达式根据提供以下元数据的专用上下文进行评估：
	//	> #root.method 、 #root.target 和 #root.caches 分别用于对 method 、目标对象和受影响缓存的引用。
	//	> 方法名称 ( #root.methodName ) 和目标类 ( #root.targetClass ) 的快捷方式也可用。
	//	> 方法参数可以通过索引访问。例如，可以通过 #root.args[1] 、 #p1 或 #a1访问第二个参数。
	//	  如果该信息可用，也可以按名称访问参数
	String condition() default "";

	/**
	 * Spring Expression Language (SpEL) expression used to veto method caching.
	 * <p>Unlike {@link #condition}, this expression is evaluated after the method
	 * has been called and can therefore refer to the {@code result}.
	 * <p>Default is {@code ""}, meaning that caching is never vetoed.
	 * <p>The SpEL expression evaluates against a dedicated context that provides the
	 * following meta-data:
	 * <ul>
	 * <li>{@code #result} for a reference to the result of the method invocation. For
	 * supported wrappers such as {@code Optional}, {@code #result} refers to the actual
	 * object, not the wrapper</li>
	 * <li>{@code #root.method}, {@code #root.target}, and {@code #root.caches} for
	 * references to the {@link java.lang.reflect.Method method}, target object, and
	 * affected cache(s) respectively.</li>
	 * <li>Shortcuts for the method name ({@code #root.methodName}) and target class
	 * ({@code #root.targetClass}) are also available.
	 * <li>Method arguments can be accessed by index. For instance the second argument
	 * can be accessed via {@code #root.args[1]}, {@code #p1} or {@code #a1}. Arguments
	 * can also be accessed by name if that information is available.</li>
	 * </ul>
	 * @since 3.2
	 */
	// 用于否决方法缓存的 Spring 表达式语言 (SpEL) 表达式。
	//
	// 与 condition 不同，此表达式在方法被调用后计算，因此可以引用result 。
	//
	// 默认为"" ，这意味着缓存永远不会被否决。
	//
	// SpEL 表达式根据提供以下元数据的专用上下文进行评估：
	//	> #result用于引用方法调用的结果。对于支持的包装器，例如Optional ， #result 指的是实际对象，而不是包装器
	//	> #root.method 、 #root.target和 #root.caches 分别用于对 method 、目标对象和受影响缓存的引用。
	//	> 方法名称 ( #root.methodName ) 和目标类 ( #root.targetClass ) 的快捷方式也可用。
	//	> 方法参数可以通过索引访问。例如，可以通过 #root.args[1] 、 #p1 或 #a1 访问第二个参数。如果该信息可用，也可以按名称访问参数。
	String unless() default "";

	/**
	 * Synchronize the invocation of the underlying method if several threads are
	 * attempting to load a value for the same key. The synchronization leads to
	 * a couple of limitations:
	 * <ol>
	 * <li>{@link #unless()} is not supported</li>
	 * <li>Only one cache may be specified</li>
	 * <li>No other cache-related operation can be combined</li>
	 * </ol>
	 * This is effectively a hint and the actual cache provider that you are
	 * using may not support it in a synchronized fashion. Check your provider
	 * documentation for more details on the actual semantics.
	 * @since 4.3
	 * @see org.springframework.cache.Cache#get(Object, Callable)
	 */
	// 如果多个线程试图为同一个键加载一个值，则同步底层方法的调用。同步导致了一些限制：
	//	1.不支持unless()
	//	2.只能指定一个缓存
	//	3.没有其他缓存相关的操作可以组合
	// 这实际上是一个提示，您使用的实际缓存提供程序可能不以同步方式支持它。
	// 检查您的提供者文档以获取有关实际语义的更多详细信息。
	boolean sync() default false;

}
