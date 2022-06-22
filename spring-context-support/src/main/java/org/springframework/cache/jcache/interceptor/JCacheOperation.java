/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.cache.jcache.interceptor;

import java.lang.annotation.Annotation;

import javax.cache.annotation.CacheInvocationParameter;
import javax.cache.annotation.CacheMethodDetails;

import org.springframework.cache.interceptor.BasicOperation;
import org.springframework.cache.interceptor.CacheResolver;

/**
 * Model the base of JSR-107 cache operation through an interface contract.
 *
 * <p>A cache operation can be statically cached as it does not contain any
 * runtime operation of a specific cache invocation.
 *
 * @author Stephane Nicoll
 * @since 4.1
 * @param <A> the type of the JSR-107 annotation
 */
// 通过接口契约对 JSR-107 缓存操作的基础进行建模。
// 缓存操作可以静态缓存，因为它不包含特定缓存调用的任何运行时操作。
// 类型形参： < A > – JSR-107 注解的类型
public interface JCacheOperation<A extends Annotation> extends BasicOperation, CacheMethodDetails<A> {

	/**
	 * Return the {@link CacheResolver} instance to use to resolve the cache
	 * to use for this operation.
	 */
	// 返回 CacheResolver 实例以用于解析用于此操作的缓存。
	CacheResolver getCacheResolver();

	/**
	 * Return the {@link CacheInvocationParameter} instances based on the
	 * specified method arguments.
	 * <p>The method arguments must match the signature of the related method invocation
	 * @param values the parameters value for a particular invocation
	 */
	// 根据指定的方法参数返回 CacheInvocationParameter 实例。
	// 方法参数必须与相关方法调用的签名匹配
	// 参形：
	//			values – 特定调用的参数值
	CacheInvocationParameter[] getAllParameters(Object... values);

}
