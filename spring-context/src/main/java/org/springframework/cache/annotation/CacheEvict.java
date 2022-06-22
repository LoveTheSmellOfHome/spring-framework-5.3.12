/*
 * Copyright 2002-2016 the original author or authors.
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
 * {@link org.springframework.cache.Cache#evict(Object) cache evict} operation.
 *
 * <p>This annotation may be used as a <em>meta-annotation</em> to create custom
 * <em>composed annotations</em> with attribute overrides.
 *
 * @author Costin Leau
 * @author Stephane Nicoll
 * @author Sam Brannen
 * @since 3.1
 * @see CacheConfig
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
// 指示方法（或类上的所有方法）触发 cache evict 操作的注解。
// 此注解可用作元注解以创建具有属性覆盖的自定义组合注解。
public @interface CacheEvict {

	/**
	 * Alias for {@link #cacheNames}.
	 */
	@AliasFor("cacheNames")
	String[] value() default {};

	/**
	 * Names of the caches to use for the cache eviction operation.
	 * <p>Names may be used to determine the target cache (or caches), matching
	 * the qualifier value or bean name of a specific bean definition.
	 * @since 4.2
	 * @see #value
	 * @see CacheConfig#cacheNames
	 */
	// 用于缓存驱逐操作的缓存名称。
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
	 * <li>{@code #result} for a reference to the result of the method invocation, which
	 * can only be used if {@link #beforeInvocation()} is {@code false}. For supported
	 * wrappers such as {@code Optional}, {@code #result} refers to the actual object,
	 * not the wrapper</li>
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
	// 默认为"" ，这意味着所有方法参数都被视为一个键，除非已设置自定义keyGenerator 。
	// SpEL 表达式根据提供以下元数据的专用上下文进行评估：
	//	> #result用于引用方法调用的结果，仅当beforeInvocation()为 false 时才能使用。对于支持的包装器，
	//	   例如O ptional ， #result指的是实际对象，而不是包装器
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
	// 要使用的自定义 org.springframework.cache.interceptor.KeyGenerator 的 bean 名称。
	// 与 key 属性互斥。
	String keyGenerator() default "";

	/**
	 * The bean name of the custom {@link org.springframework.cache.CacheManager} to use to
	 * create a default {@link org.springframework.cache.interceptor.CacheResolver} if none
	 * is set already.
	 * <p>Mutually exclusive with the {@link #cacheResolver} attribute.
	 * @see org.springframework.cache.interceptor.SimpleCacheResolver
	 * @see CacheConfig#cacheManager
	 */
	// 自定义 org.springframework.cache.CacheManager的 bean 名称，用于创建
	// 默认 org.springframework.cache.interceptor.CacheResolver （如果尚未设置）。
	//
	// 与 cacheResolver 属性互斥。
	String cacheManager() default "";

	/**
	 * The bean name of the custom {@link org.springframework.cache.interceptor.CacheResolver}
	 * to use.
	 * @see CacheConfig#cacheResolver
	 */
	// 要使用的自定义 org.springframework.cache.interceptor.CacheResolver 的 bean 名称
	String cacheResolver() default "";

	/**
	 * Spring Expression Language (SpEL) expression used for making the cache
	 * eviction operation conditional.
	 * <p>Default is {@code ""}, meaning the cache eviction is always performed.
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
	// Spring 表达式语言 (SpEL) 表达式用于使缓存逐出操作有条件。
	// 默认为"" ，表示始终执行缓存逐出。
	// SpEL 表达式根据提供以下元数据的专用上下文进行评估：
	//	>  #root.method 、 #root.target和#root.caches分别用于对method 、目标对象和受影响缓存的引用。
	//	> 方法名称 ( #root.methodName ) 和目标类 ( #root.targetClass ) 的快捷方式也可用。
	//	> 方法参数可以通过索引访问。例如，可以通过#root.args[1] 、 #p1或#a1访问第二个参数。
	//	  如果该信息可用，也可以按名称访问参数。
	String condition() default "";

	/**
	 * Whether all the entries inside the cache(s) are removed.
	 * <p>By default, only the value under the associated key is removed.
	 * <p>Note that setting this parameter to {@code true} and specifying a
	 * {@link #key} is not allowed.
	 */
	// 是否删除缓存内的所有条目。
	// 默认情况下，仅删除关联键下的值。
	// 请注意，不允许将此参数设置为true并指定key 。
	boolean allEntries() default false;

	/**
	 * Whether the eviction should occur before the method is invoked.
	 * <p>Setting this attribute to {@code true}, causes the eviction to
	 * occur irrespective of the method outcome (i.e., whether it threw an
	 * exception or not).
	 * <p>Defaults to {@code false}, meaning that the cache eviction operation
	 * will occur <em>after</em> the advised method is invoked successfully (i.e.
	 * only if the invocation did not throw an exception).
	 */
	// 是否应该在调用方法之前进行驱逐。
	// 将此属性设置为 true ，无论方法结果如何（即，它是否抛出异常）都会导致驱逐发生。
	// 默认为 false ，这意味着在成功调用建议的方法后将进行缓存逐出操作（即仅在调用未引发异常的情况下）。
	boolean beforeInvocation() default false;

}
