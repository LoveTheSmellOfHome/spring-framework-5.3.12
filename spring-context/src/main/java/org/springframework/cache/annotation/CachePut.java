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

import org.springframework.core.annotation.AliasFor;

/**
 * Annotation indicating that a method (or all methods on a class) triggers a
 * {@link org.springframework.cache.Cache#put(Object, Object) cache put} operation.
 *
 * <p>In contrast to the {@link Cacheable @Cacheable} annotation, this annotation
 * does not cause the advised method to be skipped. Rather, it always causes the
 * method to be invoked and its result to be stored in the associated cache if the
 * {@link #condition()} and {@link #unless()} expressions match accordingly. Note
 * that Java8's {@code Optional} return types are automatically handled and its
 * content is stored in the cache if present.
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
// 指示方法（或类上的所有方法）触发 cache put 操作的注解。
// 
// 与 @Cacheable 注解相比，此注解不会导致建议的方法被跳过。相反，如果 condition() 和 unless() 表达式相应匹配，
// 它总是会导致调用该方法并将其结果存储在关联的缓存中。请注意，Java8 的 Optional 返回类型是自动处理的，如果存在，它的内容会存储在缓存中。
//
// 此注解可用作元注解以创建具有属性覆盖的自定义组合注解。
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface CachePut {

	/**
	 * Alias for {@link #cacheNames}.
	 */
	// cacheNames 的别名
	@AliasFor("cacheNames")
	String[] value() default {};

	/**
	 * Names of the caches to use for the cache put operation.
	 * <p>Names may be used to determine the target cache (or caches), matching
	 * the qualifier value or bean name of a specific bean definition.
	 * @since 4.2
	 * @see #value
	 * @see CacheConfig#cacheNames
	 */
	// 用于缓存放置操作的缓存名称。
	// 名称可用于确定目标缓存（或多个缓存），匹配特定 bean 定义的限定符值或 bean 名称。
	@AliasFor("value")
	String[] cacheNames() default {};

	/**
	 * Spring Expression Language (SpEL) expression for computing the key dynamically.
	 * <p>Default is {@code ""}, meaning all method parameters are considered as a key,
	 * unless a custom {@link #keyGenerator} has been set.
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
	 */
	// 用于动态计算密钥的 Spring 表达式语言 (SpEL) 表达式。
	//
	// 默认为"" ，这意味着所有方法参数都被视为一个键，除非已设置自定义keyGenerator 。
	//
	// SpEL 表达式根据提供以下元数据的专用上下文进行评估：
	//	> #result用于引用方法调用的结果。对于支持的包装器，例如Optional ， #result指的是实际对象，而不是包装器
	//	> #root.method 、 #root.target和#root.caches分别用于对method 、目标对象和受影响缓存的引用。
	//	> 方法名称 ( #root.methodName ) 和目标类 ( #root.targetClass ) 的快捷方式也可用。
	//	> 方法参数可以通过索引访问。例如，可以通过#root.args[1] 、 #p1或#a1访问第二个参数。
	//	  如果该信息可用，也可以按名称访问参数。
	String key() default "";

	/**
	 * The bean name of the custom {@link org.springframework.cache.interceptor.KeyGenerator}
	 * to use.
	 * <p>Mutually exclusive with the {@link #key} attribute.
	 * @see CacheConfig#keyGenerator
	 */
	// 要使用的自定义 org.springframework.cache.interceptor.KeyGenerator的 bean 名称。
	// 与 key 属性互斥
	String keyGenerator() default "";

	/**
	 * The bean name of the custom {@link org.springframework.cache.CacheManager} to use to
	 * create a default {@link org.springframework.cache.interceptor.CacheResolver} if none
	 * is set already.
	 * <p>Mutually exclusive with the {@link #cacheResolver} attribute.
	 * @see org.springframework.cache.interceptor.SimpleCacheResolver
	 * @see CacheConfig#cacheManager
	 */
	// 自定义 org.springframework.cache.CacheManager 的 bean 名称，用于创建默认
	// org.springframework.cache.interceptor.CacheResolver （如果尚未设置）。
	//
	// 与 cacheResolver 属性互斥。
	String cacheManager() default "";

	/**
	 * The bean name of the custom {@link org.springframework.cache.interceptor.CacheResolver}
	 * to use.
	 * @see CacheConfig#cacheResolver
	 */
	// 要使用的自定义 org.springframework.cache.interceptor.CacheResolver 的 bean 名称。
	String cacheResolver() default "";

	/**
	 * Spring Expression Language (SpEL) expression used for making the cache
	 * put operation conditional.
	 * <p>This expression is evaluated after the method has been called due to the
	 * nature of the put operation and can therefore refer to the {@code result}.
	 * <p>Default is {@code ""}, meaning the method result is always cached.
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
	 */
	// Spring Expression Language (SpEL) 表达式用于使缓存放置操作有条件。
	// 由于 put 操作的性质，该表达式在调用该方法后进行评估，因此可以引用result 。
	// 默认为"" ，表示方法结果始终被缓存。
	// SpEL 表达式根据提供以下元数据的专用上下文进行评估：
	//	> #result用于引用方法调用的结果。对于支持的包装器，例如Optional ， #result指的是实际对象，而不是包装器
	//	> #root.method 、 #root.target和#root.caches分别用于对method 、目标对象和受影响缓存的引用。
	//	> 方法名称 ( #root.methodName ) 和目标类 ( #root.targetClass ) 的快捷方式也可用。
	//	> 方法参数可以通过索引访问。例如，可以通过#root.args[1] 、 #p1或#a1访问第二个参数。
	//	  如果该信息可用，也可以按名称访问参数。
	String condition() default "";

	/**
	 * Spring Expression Language (SpEL) expression used to veto the cache put operation.
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
	// 用于否决缓存放置操作的 Spring 表达式语言 (SpEL) 表达式。
	// 默认为"" ，这意味着缓存永远不会被否决。
	// SpEL 表达式根据提供以下元数据的专用上下文进行评估：
	//   > #result用于引用方法调用的结果。对于支持的包装器，例如Optional ， #result指的是实际对象，而不是包装器
	//   > #root.method 、 #root.target和#root.caches分别用于对method 、目标对象和受影响缓存的引用。
	//	 > 方法名称 ( #root.methodName ) 和目标类 ( #root.targetClass ) 的快捷方式也可用。
	// 方法参数可以通过索引访问。例如，可以通过 #root.args[1] 、 #p1 或 #a1 访问第二个参数。如果该信息可用，也可以按名称访问参数。
	String unless() default "";

}
