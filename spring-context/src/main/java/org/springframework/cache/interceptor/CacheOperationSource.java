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

package org.springframework.cache.interceptor;

import java.lang.reflect.Method;
import java.util.Collection;

import org.springframework.lang.Nullable;

/**
 * Interface used by {@link CacheInterceptor}. Implementations know how to source
 * cache operation attributes, whether from configuration, metadata attributes at
 * source level, or elsewhere.
 *
 * @author Costin Leau
 * @author Juergen Hoeller
 * @since 3.1
 */
// CacheInterceptor 使用的接口。实现知道如何获取缓存操作属性，无论是从配置、源级别的元数据属性还是其他地方
public interface CacheOperationSource {

	/**
	 * Determine whether the given class is a candidate for cache operations
	 * in the metadata format of this {@code CacheOperationSource}.
	 * <p>If this method returns {@code false}, the methods on the given class
	 * will not get traversed for {@link #getCacheOperations} introspection.
	 * Returning {@code false} is therefore an optimization for non-affected
	 * classes, whereas {@code true} simply means that the class needs to get
	 * fully introspected for each method on the given class individually.
	 * @param targetClass the class to introspect
	 * @return {@code false} if the class is known to have no cache operation
	 * metadata at class or method level; {@code true} otherwise. The default
	 * implementation returns {@code true}, leading to regular introspection.
	 * @since 5.2
	 */
	// 确定给定类是否是此 CacheOperationSource 元数据格式的缓存操作的候选对象。
	//
	// 如果此方法返回 false ，则不会遍历给定类上的方法以进行 getCacheOperations 内省。因此，
	// 返回 false 是对不受影响的类的优化，而 true 仅表示该类需要针对给定类上的每个方法单独进行完全自省。
	// 参形：
	//				targetClass – 要自省的类
	// 返回值：
	//				如果已知该类在类或方法级别没有缓存操作元数据，则返回false ；否则为true 。默认实现返回true ，导致定期自省。
	default boolean isCandidateClass(Class<?> targetClass) {
		return true;
	}

	/**
	 * Return the collection of cache operations for this method,
	 * or {@code null} if the method contains no <em>cacheable</em> annotations.
	 * @param method the method to introspect
	 * @param targetClass the target class (may be {@code null}, in which case
	 * the declaring class of the method must be used)
	 * @return all cache operations for this method, or {@code null} if none found
	 */
	// 返回此方法的缓存操作集合，如果该方法不包含可缓存的注释，则返回 null 。
	// 参形：
	//			method - 自省的方法
	//			targetClass – 目标类（可能是null ，在这种情况下必须使用方法的声明类）
	// 返回值：
	//			此方法的所有缓存操作，如果没有找到，则返回null
	@Nullable
	Collection<CacheOperation> getCacheOperations(Method method, @Nullable Class<?> targetClass);

}
