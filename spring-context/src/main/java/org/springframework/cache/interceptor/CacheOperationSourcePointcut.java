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

import java.io.Serializable;
import java.lang.reflect.Method;

import org.springframework.aop.ClassFilter;
import org.springframework.aop.support.StaticMethodMatcherPointcut;
import org.springframework.cache.CacheManager;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

/**
 * A Pointcut that matches if the underlying {@link CacheOperationSource}
 * has an attribute for a given method.
 *
 * @author Costin Leau
 * @author Juergen Hoeller
 * @since 3.1
 */
// 如果基础 CacheOperationSource 具有给定方法的属性，则匹配的切入点。
@SuppressWarnings("serial")
abstract class CacheOperationSourcePointcut extends StaticMethodMatcherPointcut implements Serializable {

	protected CacheOperationSourcePointcut() {
		// 设置缓存草走源类过滤器
		setClassFilter(new CacheOperationSourceClassFilter());
	}


	@Override
	public boolean matches(Method method, Class<?> targetClass) {
		CacheOperationSource cas = getCacheOperationSource();
		// cas 不为空 && 获取 Collection<CacheOperation> 集合不为空
		return (cas != null && !CollectionUtils.isEmpty(cas.getCacheOperations(method, targetClass)));
	}

	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof CacheOperationSourcePointcut)) {
			return false;
		}
		CacheOperationSourcePointcut otherPc = (CacheOperationSourcePointcut) other;
		return ObjectUtils.nullSafeEquals(getCacheOperationSource(), otherPc.getCacheOperationSource());
	}

	@Override
	public int hashCode() {
		return CacheOperationSourcePointcut.class.hashCode();
	}

	@Override
	public String toString() {
		return getClass().getName() + ": " + getCacheOperationSource();
	}


	/**
	 * Obtain the underlying {@link CacheOperationSource} (may be {@code null}).
	 * To be implemented by subclasses.
	 */
	// 获取底层 CacheOperationSource （可能为null ）。由子类实现。
	@Nullable
	protected abstract CacheOperationSource getCacheOperationSource();


	/**
	 * {@link ClassFilter} that delegates to {@link CacheOperationSource#isCandidateClass}
	 * for filtering classes whose methods are not worth searching to begin with.
	 */
	// ClassFilter 委托给 CacheOperationSource.isCandidateClass 用于过滤其方法不值得一开始搜索的类。
	private class CacheOperationSourceClassFilter implements ClassFilter {

		@Override
		public boolean matches(Class<?> clazz) {
			if (CacheManager.class.isAssignableFrom(clazz)) {
				return false;
			}
			CacheOperationSource cas = getCacheOperationSource();
			return (cas == null || cas.isCandidateClass(clazz));
		}
	}

}
