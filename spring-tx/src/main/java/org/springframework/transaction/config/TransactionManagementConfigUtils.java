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

package org.springframework.transaction.config;

/**
 * Configuration constants for internal sharing across subpackages.
 *
 * @author Chris Beams
 * @author Stephane Nicoll
 * @since 3.1
 */
// 用于跨子包内部共享的配置常量。
public abstract class TransactionManagementConfigUtils {

	/**
	 * The bean name of the internally managed transaction advisor (used when mode == PROXY).
	 */
	// 内部管理事务顾问的 bean 名称（在 mode == PROXY 时使用）。
	public static final String TRANSACTION_ADVISOR_BEAN_NAME =
			"org.springframework.transaction.config.internalTransactionAdvisor";

	/**
	 * The bean name of the internally managed transaction aspect (used when mode == ASPECTJ).
	 */
	// 内部管理事务切面的 bean 名称（在 mode == ASPECTJ 时使用）。
	public static final String TRANSACTION_ASPECT_BEAN_NAME =
			"org.springframework.transaction.config.internalTransactionAspect";

	/**
	 * The class name of the AspectJ transaction management aspect.
	 */
	// AspectJ 事务管理切面的类名
	public static final String TRANSACTION_ASPECT_CLASS_NAME =
			"org.springframework.transaction.aspectj.AnnotationTransactionAspect";

	/**
	 * The name of the AspectJ transaction management @{@code Configuration} class.
	 */
	// AspectJ 事务管理 @Configuration 类的名称
	public static final String TRANSACTION_ASPECT_CONFIGURATION_CLASS_NAME =
			"org.springframework.transaction.aspectj.AspectJTransactionManagementConfiguration";

	/**
	 * The bean name of the internally managed JTA transaction aspect (used when mode == ASPECTJ).
	 * @since 5.1
	 */
	// 内部管理的 JTA 事务切面的 bean 名称（在 mode == ASPECTJ 时使用
	public static final String JTA_TRANSACTION_ASPECT_BEAN_NAME =
			"org.springframework.transaction.config.internalJtaTransactionAspect";

	/**
	 * The class name of the AspectJ transaction management aspect.
	 * @since 5.1
	 */
	// AspectJ 事务管理切面的类名
	public static final String JTA_TRANSACTION_ASPECT_CLASS_NAME =
			"org.springframework.transaction.aspectj.JtaAnnotationTransactionAspect";

	/**
	 * The name of the AspectJ transaction management @{@code Configuration} class for JTA.
	 * @since 5.1
	 */
	// AspectJ 事务管理@JTA 的Configuration类的名称
	public static final String JTA_TRANSACTION_ASPECT_CONFIGURATION_CLASS_NAME =
			"org.springframework.transaction.aspectj.AspectJJtaTransactionManagementConfiguration";

	/**
	 * The bean name of the internally managed TransactionalEventListenerFactory.
	 */
	// 内部管理的 TransactionalEventListenerFactory 的 bean 名称
	public static final String TRANSACTIONAL_EVENT_LISTENER_FACTORY_BEAN_NAME =
			"org.springframework.transaction.config.internalTransactionalEventListenerFactory";

}
