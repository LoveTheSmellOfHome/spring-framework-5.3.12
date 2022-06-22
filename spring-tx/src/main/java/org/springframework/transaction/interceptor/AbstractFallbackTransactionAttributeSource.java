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

package org.springframework.transaction.interceptor;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aop.support.AopUtils;
import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.core.MethodClassKey;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringValueResolver;

/**
 * Abstract implementation of {@link TransactionAttributeSource} that caches
 * attributes for methods and implements a fallback policy: 1. specific target
 * method; 2. target class; 3. declaring method; 4. declaring class/interface.
 *
 * <p>Defaults to using the target class's transaction attribute if none is
 * associated with the target method. Any transaction attribute associated with
 * the target method completely overrides a class transaction attribute.
 * If none found on the target class, the interface that the invoked method
 * has been called through (in case of a JDK proxy) will be checked.
 *
 * <p>This implementation caches attributes by method after they are first used.
 * If it is ever desirable to allow dynamic changing of transaction attributes
 * (which is very unlikely), caching could be made configurable. Caching is
 * desirable because of the cost of evaluating rollback rules.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 1.1
 */
// TransactionAttributeSource 的抽象实现，它为方法缓存属性并实现回退策略：
// 1. 特定目标方法； 2.目标类； 3. 声明方法； 4. 声明类/接口。
//
// 如果没有与目标方法关联，则默认使用目标类的事务属性。与目标方法关联的任何事务属性都会完全覆盖类事务属性。
// 如果在目标类上没有找到，则将检查调用方法的接口（在 JDK 代理的情况下）。
//
// 此实现在首次使用属性后按方法缓存属性。如果希望允许动态更改事务属性（这是非常不可能的），则可以将缓存设置为可配置的。
// 由于评估回滚规则的成本，缓存是可取的。
public abstract class AbstractFallbackTransactionAttributeSource
		implements TransactionAttributeSource, EmbeddedValueResolverAware {

	/**
	 * Canonical value held in cache to indicate no transaction attribute was
	 * found for this method, and we don't need to look again.
	 */
	// 缓存中保存的规范值表示没有找到此方法的事务属性，我们不需要再查看。
	@SuppressWarnings("serial")
	private static final TransactionAttribute NULL_TRANSACTION_ATTRIBUTE = new DefaultTransactionAttribute() {
		@Override
		public String toString() {
			return "null";
		}
	};


	/**
	 * Logger available to subclasses.
	 * <p>As this base class is not marked Serializable, the logger will be recreated
	 * after serialization - provided that the concrete subclass is Serializable.
	 */
	protected final Log logger = LogFactory.getLog(getClass());

	@Nullable
	private transient StringValueResolver embeddedValueResolver;

	/**
	 * Cache of TransactionAttributes, keyed by method on a specific target class.
	 * <p>As this base class is not marked Serializable, the cache will be recreated
	 * after serialization - provided that the concrete subclass is Serializable.
	 */
	// TransactionAttributes 的缓存，由特定目标类上的方法键入。
	// 由于这个基类没有标记为 Serializable，缓存将在序列化后重新创建——前提是具体的子类是 Serializable。
	private final Map<Object, TransactionAttribute> attributeCache = new ConcurrentHashMap<>(1024);


	@Override
	public void setEmbeddedValueResolver(StringValueResolver resolver) {
		this.embeddedValueResolver = resolver;
	}


	/**
	 * Determine the transaction attribute for this method invocation.
	 * <p>Defaults to the class's transaction attribute if no method attribute is found.
	 * @param method the method for the current invocation (never {@code null})
	 * @param targetClass the target class for this invocation (may be {@code null})
	 * @return a TransactionAttribute for this method, or {@code null} if the method
	 * is not transactional
	 */
	// 确定此方法调用的事务属性。
	// 如果没有找到方法属性，则默认为类的事务属性。
	// 参形：
	//			method – 当前调用的方法（从不为null ）
	//			targetClass – 此调用的目标类（可能为null ）
	// 返回值：
	//			此方法的 TransactionAttribute，如果方法不是事务性的，则为null
	@Override
	@Nullable
	public TransactionAttribute getTransactionAttribute(Method method, @Nullable Class<?> targetClass) {
		if (method.getDeclaringClass() == Object.class) {
			return null;
		}

		// First, see if we have a cached value.
		// 首先，看看我们是否有缓存值
		Object cacheKey = getCacheKey(method, targetClass);
		TransactionAttribute cached = this.attributeCache.get(cacheKey);
		if (cached != null) {
			// Value will either be canonical value indicating there is no transaction attribute,
			// or an actual transaction attribute.
			// 值可以是表示没有交易属性的规范值，也可以是实际的交易属性
			if (cached == NULL_TRANSACTION_ATTRIBUTE) {
				return null;
			}
			else {
				return cached;
			}
		}
		else {
			// We need to work it out.
			// 我们需要解决它
			TransactionAttribute txAttr = computeTransactionAttribute(method, targetClass);
			// Put it in the cache.
			// 把它放在缓存中
			if (txAttr == null) {
				this.attributeCache.put(cacheKey, NULL_TRANSACTION_ATTRIBUTE);
			}
			else {
				// 获取方法完全限定名
				String methodIdentification = ClassUtils.getQualifiedMethodName(method, targetClass);
				if (txAttr instanceof DefaultTransactionAttribute) {
					DefaultTransactionAttribute dta = (DefaultTransactionAttribute) txAttr;
					dta.setDescriptor(methodIdentification);
					dta.resolveAttributeStrings(this.embeddedValueResolver);
				}
				if (logger.isTraceEnabled()) {
					logger.trace("Adding transactional method '" + methodIdentification + "' with attribute: " + txAttr);
				}
				this.attributeCache.put(cacheKey, txAttr);
			}
			return txAttr;
		}
	}

	/**
	 * Determine a cache key for the given method and target class.
	 * <p>Must not produce same key for overloaded methods.
	 * Must produce same key for different instances of the same method.
	 * @param method the method (never {@code null})
	 * @param targetClass the target class (may be {@code null})
	 * @return the cache key (never {@code null})
	 */
	// 确定给定方法和目标类的缓存键。
	// 不能为重载方法生成相同的键。必须为同一方法的不同实例生成相同的密钥。
	// 参形：
	//			方法– 方法（从不为null ）
	//			targetClass – 目标类（可能为null ）
	// 返回值：
	//			缓存键（从不为null ）
	protected Object getCacheKey(Method method, @Nullable Class<?> targetClass) {
		return new MethodClassKey(method, targetClass);
	}

	/**
	 * Same signature as {@link #getTransactionAttribute}, but doesn't cache the result.
	 * {@link #getTransactionAttribute} is effectively a caching decorator for this method.
	 * <p>As of 4.1.8, this method can be overridden.
	 * @since 4.1.8
	 * @see #getTransactionAttribute
	 */
	// 与 getTransactionAttribute 相同的签名，但不缓存结果。 getTransactionAttribute 实际上是此方法的缓存装饰器。
	@Nullable
	protected TransactionAttribute computeTransactionAttribute(Method method, @Nullable Class<?> targetClass) {
		// Don't allow no-public methods as required.
		// 不允许根据需要使用非公共方法
		if (allowPublicMethodsOnly() && !Modifier.isPublic(method.getModifiers())) {
			return null;
		}

		// The method may be on an interface, but we need attributes from the target class.
		// If the target class is null, the method will be unchanged.
		// 该方法可能在接口上，但我们需要来自目标类的属性。如果目标类为空，则方法将保持不变。
		Method specificMethod = AopUtils.getMostSpecificMethod(method, targetClass);

		// First try is the method in the target class.
		// 首先尝试的是目标类中的方法
		TransactionAttribute txAttr = findTransactionAttribute(specificMethod);
		if (txAttr != null) {
			return txAttr;
		}

		// Second try is the transaction attribute on the target class.
		// 第二次尝试是目标类的事务属性
		txAttr = findTransactionAttribute(specificMethod.getDeclaringClass());
		if (txAttr != null && ClassUtils.isUserLevelMethod(method)) {
			return txAttr;
		}

		if (specificMethod != method) {
			// Fallback is to look at the original method.
			// Fallback就是看原来的方法。
			txAttr = findTransactionAttribute(method);
			if (txAttr != null) {
				return txAttr;
			}
			// Last fallback is the class of the original method.
			// 最后一个备用是原始方法的类。
			txAttr = findTransactionAttribute(method.getDeclaringClass());
			if (txAttr != null && ClassUtils.isUserLevelMethod(method)) {
				return txAttr;
			}
		}

		return null;
	}


	/**
	 * Subclasses need to implement this to return the transaction attribute for the
	 * given class, if any.
	 * @param clazz the class to retrieve the attribute for
	 * @return all transaction attribute associated with this class, or {@code null} if none
	 */
	// 子类需要实现这一点以返回给定类的事务属性（如果有）。
	// 参形：
	//				clazz – 检索属性的类
	// 返回值：
	//				与此类关联的所有事务属性，如果没有则为null
	@Nullable
	protected abstract TransactionAttribute findTransactionAttribute(Class<?> clazz);

	/**
	 * Subclasses need to implement this to return the transaction attribute for the
	 * given method, if any.
	 * @param method the method to retrieve the attribute for
	 * @return all transaction attribute associated with this method, or {@code null} if none
	 */
	// 子类需要实现这一点以返回给定方法的事务属性（如果有）。
	// 参形：
	//			方法- 检索属性的方法
	// 返回值：
	//			与此方法关联的所有事务属性，如果没有，则返回null
	@Nullable
	protected abstract TransactionAttribute findTransactionAttribute(Method method);

	/**
	 * Should only public methods be allowed to have transactional semantics?
	 * <p>The default implementation returns {@code false}.
	 */
	// 是否应该只允许公共方法具有事务语义？默认实现返回 false
	protected boolean allowPublicMethodsOnly() {
		return false;
	}

}
