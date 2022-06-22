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

package org.springframework.transaction.interceptor;

import java.lang.reflect.Method;

import org.springframework.lang.Nullable;

/**
 * Strategy interface used by {@link TransactionInterceptor} for metadata retrieval.
 *
 * <p>Implementations know how to source transaction attributes, whether from configuration,
 * metadata attributes at source level (such as Java 5 annotations), or anywhere else.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 15.04.2003
 * @see TransactionInterceptor#setTransactionAttributeSource
 * @see TransactionProxyFactoryBean#setTransactionAttributeSource
 * @see org.springframework.transaction.annotation.AnnotationTransactionAttributeSource
 */
// TransactionInterceptor 用于元数据检索的策略接口。
// 实现知道如何获取事务属性，无论是从配置、源级别的元数据属性（例如 Java 5 注释）还是其他任何地方。
public interface TransactionAttributeSource {

	/**
	 * Determine whether the given class is a candidate for transaction attributes
	 * in the metadata format of this {@code TransactionAttributeSource}.
	 * <p>If this method returns {@code false}, the methods on the given class
	 * will not get traversed for {@link #getTransactionAttribute} introspection.
	 * Returning {@code false} is therefore an optimization for non-affected
	 * classes, whereas {@code true} simply means that the class needs to get
	 * fully introspected for each method on the given class individually.
	 * @param targetClass the class to introspect
	 * @return {@code false} if the class is known to have no transaction
	 * attributes at class or method level; {@code true} otherwise. The default
	 * implementation returns {@code true}, leading to regular introspection.
	 * @since 5.2
	 */
	// 确定给定类是否是此TransactionAttributeSource元数据格式的事务属性的候选者。
	//
	// 如果此方法返回 false ，则不会遍历给定类上的方法以进行 getTransactionAttribute 内省。因此，
	// 返回 false 是对不受影响的类的优化，而 true 仅表示该类需要针对给定类上的每个方法单独进行完全自省。
	// 参形：
	//			targetClass – 要自省的类
	// 返回值：
	//			如果已知该类在类或方法级别没有事务属性，则为false ；否则为true 。默认实现返回true ，导致定期自省。
	default boolean isCandidateClass(Class<?> targetClass) {
		return true;
	}

	/**
	 * Return the transaction attribute for the given method,
	 * or {@code null} if the method is non-transactional.
	 * @param method the method to introspect
	 * @param targetClass the target class (may be {@code null},
	 * in which case the declaring class of the method must be used)
	 * @return the matching transaction attribute, or {@code null} if none found
	 */
	// 返回给定方法的事务属性，如果方法是非事务性的，则返回null 。
	// 参形：
	//			method - 自省的方法
	//			targetClass – 目标类（可能是null ，在这种情况下必须使用方法的声明类）
	// 返回值：
	//			匹配的事务属性，如果没有找到，则返回null
	@Nullable
	TransactionAttribute getTransactionAttribute(Method method, @Nullable Class<?> targetClass);

}
