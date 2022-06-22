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

package org.springframework.cache.jcache.interceptor;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.cache.interceptor.AbstractCacheInvoker;
import org.springframework.cache.interceptor.BasicOperation;
import org.springframework.cache.interceptor.CacheOperationInvocationContext;
import org.springframework.cache.interceptor.CacheOperationInvoker;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Base class for JSR-107 caching aspects, such as the {@link JCacheInterceptor}
 * or an AspectJ aspect.
 *
 * <p>Use the Spring caching abstraction for cache-related operations. No JSR-107
 * {@link javax.cache.Cache} or {@link javax.cache.CacheManager} are required to
 * process standard JSR-107 cache annotations.
 *
 * <p>The {@link JCacheOperationSource} is used for determining caching operations
 *
 * <p>A cache aspect is serializable if its {@code JCacheOperationSource} is serializable.
 *
 * @author Stephane Nicoll
 * @since 4.1
 * @see org.springframework.cache.interceptor.CacheAspectSupport
 * @see KeyGeneratorAdapter
 * @see CacheResolverAdapter
 */
// JSR-107 缓存切面的基类，例如 JCacheInterceptor 或 AspectJ 切面。
//
// 将 Spring 缓存抽象用于缓存相关操作。处理标准 JSR-107 缓存注解不需要 JSR-107 
// javax.cache.Cache或javax.cache.CacheManager 。
//
// JCacheOperationSource 用于确定缓存操作
//
// 如果 JCacheOperationSource 是可序列化的，则缓存切面是可序列化的。
public class JCacheAspectSupport extends AbstractCacheInvoker implements InitializingBean {

	protected final Log logger = LogFactory.getLog(getClass());

	// JCache 操作源
	@Nullable
	private JCacheOperationSource cacheOperationSource;

	// CacheResult 拦截器
	@Nullable
	private CacheResultInterceptor cacheResultInterceptor;

	// CachePut 拦截器
	@Nullable
	private CachePutInterceptor cachePutInterceptor;

	// 移除缓存实体拦截器
	@Nullable
	private CacheRemoveEntryInterceptor cacheRemoveEntryInterceptor;

	// 移除所有缓存拦截器
	@Nullable
	private CacheRemoveAllInterceptor cacheRemoveAllInterceptor;

	private boolean initialized = false;


	/**
	 * Set the CacheOperationSource for this cache aspect.
	 */
	// 为此缓存切面设置 CacheOperationSource。
	public void setCacheOperationSource(JCacheOperationSource cacheOperationSource) {
		Assert.notNull(cacheOperationSource, "JCacheOperationSource must not be null");
		this.cacheOperationSource = cacheOperationSource;
	}

	/**
	 * Return the CacheOperationSource for this cache aspect.
	 */
	// 返回此缓存切面的 CacheOperationSource。
	public JCacheOperationSource getCacheOperationSource() {
		Assert.state(this.cacheOperationSource != null, "The 'cacheOperationSource' property is required: " +
				"If there are no cacheable methods, then don't use a cache aspect.");
		return this.cacheOperationSource;
	}

	@Override
	public void afterPropertiesSet() {
		getCacheOperationSource();

		this.cacheResultInterceptor = new CacheResultInterceptor(getErrorHandler());
		this.cachePutInterceptor = new CachePutInterceptor(getErrorHandler());
		this.cacheRemoveEntryInterceptor = new CacheRemoveEntryInterceptor(getErrorHandler());
		this.cacheRemoveAllInterceptor = new CacheRemoveAllInterceptor(getErrorHandler());

		this.initialized = true;
	}


	@Nullable
	protected Object execute(CacheOperationInvoker invoker, Object target, Method method, Object[] args) {
		// Check whether aspect is enabled to cope with cases where the AJ is pulled in automatically
		// 检查是否开启 aspect 以应对 AJ 自动拉入的情况
		if (this.initialized) {
			Class<?> targetClass = AopProxyUtils.ultimateTargetClass(target);
			JCacheOperation<?> operation = getCacheOperationSource().getCacheOperation(method, targetClass);
			if (operation != null) {
				CacheOperationInvocationContext<?> context =
						createCacheOperationInvocationContext(target, args, operation);
				return execute(context, invoker);
			}
		}

		return invoker.invoke();
	}

	@SuppressWarnings("unchecked")
	private CacheOperationInvocationContext<?> createCacheOperationInvocationContext(
			Object target, Object[] args, JCacheOperation<?> operation) {

		return new DefaultCacheInvocationContext<>(
				(JCacheOperation<Annotation>) operation, target, args);
	}

	@SuppressWarnings("unchecked")
	@Nullable
	private Object execute(CacheOperationInvocationContext<?> context, CacheOperationInvoker invoker) {
		CacheOperationInvoker adapter = new CacheOperationInvokerAdapter(invoker);
		BasicOperation operation = context.getOperation();

		if (operation instanceof CacheResultOperation) {
			Assert.state(this.cacheResultInterceptor != null, "No CacheResultInterceptor");
			return this.cacheResultInterceptor.invoke(
					(CacheOperationInvocationContext<CacheResultOperation>) context, adapter);
		}
		else if (operation instanceof CachePutOperation) {
			Assert.state(this.cachePutInterceptor != null, "No CachePutInterceptor");
			return this.cachePutInterceptor.invoke(
					(CacheOperationInvocationContext<CachePutOperation>) context, adapter);
		}
		else if (operation instanceof CacheRemoveOperation) {
			Assert.state(this.cacheRemoveEntryInterceptor != null, "No CacheRemoveEntryInterceptor");
			return this.cacheRemoveEntryInterceptor.invoke(
					(CacheOperationInvocationContext<CacheRemoveOperation>) context, adapter);
		}
		else if (operation instanceof CacheRemoveAllOperation) {
			Assert.state(this.cacheRemoveAllInterceptor != null, "No CacheRemoveAllInterceptor");
			return this.cacheRemoveAllInterceptor.invoke(
					(CacheOperationInvocationContext<CacheRemoveAllOperation>) context, adapter);
		}
		else {
			throw new IllegalArgumentException("Cannot handle " + operation);
		}
	}

	/**
	 * Execute the underlying operation (typically in case of cache miss) and return
	 * the result of the invocation. If an exception occurs it will be wrapped in
	 * a {@code ThrowableWrapper}: the exception can be handled or modified but it
	 * <em>must</em> be wrapped in a {@code ThrowableWrapper} as well.
	 * @param invoker the invoker handling the operation being cached
	 * @return the result of the invocation
	 * @see CacheOperationInvoker#invoke()
	 */
	// 执行底层操作（通常在缓存未命中的情况下）并返回调用结果。如果发生异常，它将被包装在ThrowableWrapper中：
	// 可以处理或修改异常，但也必须将其包装在 ThrowableWrapper 中。
	// 参形：
	//			调用者——处理被缓存操作的调用者
	// 返回值：
	//			调用的结果
	@Nullable
	protected Object invokeOperation(CacheOperationInvoker invoker) {
		return invoker.invoke();
	}


	private class CacheOperationInvokerAdapter implements CacheOperationInvoker {

		private final CacheOperationInvoker delegate;

		public CacheOperationInvokerAdapter(CacheOperationInvoker delegate) {
			this.delegate = delegate;
		}

		@Override
		public Object invoke() throws ThrowableWrapper {
			return invokeOperation(this.delegate);
		}
	}

}
