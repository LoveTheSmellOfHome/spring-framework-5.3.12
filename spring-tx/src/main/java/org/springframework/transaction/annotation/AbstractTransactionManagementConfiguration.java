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

package org.springframework.transaction.annotation;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportAware;
import org.springframework.context.annotation.Role;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.lang.Nullable;
import org.springframework.transaction.TransactionManager;
import org.springframework.transaction.config.TransactionManagementConfigUtils;
import org.springframework.transaction.event.TransactionalEventListenerFactory;
import org.springframework.util.CollectionUtils;

/**
 * Abstract base {@code @Configuration} class providing common structure for enabling
 * Spring's annotation-driven transaction management capability.
 *
 * @author Chris Beams
 * @author Stephane Nicoll
 * @since 3.1
 * @see EnableTransactionManagement
 */
// 抽象基类 @Configuration 为启用 Spring 的注解驱动事务管理功能提供通用结构
@Configuration
public abstract class AbstractTransactionManagementConfiguration implements ImportAware {

	@Nullable
	protected AnnotationAttributes enableTx;

	/**
	 * Default transaction manager, as configured through a {@link TransactionManagementConfigurer}.
	 */
	// 默认事务管理器，通过 TransactionManagementConfigurer 配置
	@Nullable
	protected TransactionManager txManager;


	@Override
	public void setImportMetadata(AnnotationMetadata importMetadata) {
		this.enableTx = AnnotationAttributes.fromMap(
				// 获取注解 @EnableTransactionManagement
				importMetadata.getAnnotationAttributes(EnableTransactionManagement.class.getName(), false));
		if (this.enableTx == null) {
			throw new IllegalArgumentException(
					"@EnableTransactionManagement is not present on importing class " + importMetadata.getClassName());
		}
	}

	@Autowired(required = false)
	void setConfigurers(Collection<TransactionManagementConfigurer> configurers) {
		if (CollectionUtils.isEmpty(configurers)) {
			return;
		}
		if (configurers.size() > 1) {
			throw new IllegalStateException("Only one TransactionManagementConfigurer may exist");
		}
		// TransactionManagementConfigurer，可以允许有多个配置类，取其中的一个
		// 获取其 annotationDrivenTransactionManager
		TransactionManagementConfigurer configurer = configurers.iterator().next();
		// 初始化 TransactionManager
		this.txManager = configurer.annotationDrivenTransactionManager();
	}


	@Bean(name = TransactionManagementConfigUtils.TRANSACTIONAL_EVENT_LISTENER_FACTORY_BEAN_NAME)
	@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
	public static TransactionalEventListenerFactory transactionalEventListenerFactory() {
		return new TransactionalEventListenerFactory();
	}

}
