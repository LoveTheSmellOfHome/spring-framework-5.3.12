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

package org.springframework.aop.framework.autoproxy;

import org.springframework.aop.TargetSource;
import org.springframework.lang.Nullable;

/**
 * Implementations can create special target sources, such as pooling target
 * sources, for particular beans. For example, they may base their choice
 * on attributes, such as a pooling attribute, on the target class.
 *
 * <p>AbstractAutoProxyCreator can support a number of TargetSourceCreators,
 * which will be applied in order.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
// 实现可以为特定的 bean 创建特殊的目标源(资源池)，例如池化目标源。 例如，他们可以根据目标类的属性（例如池属性）进行选择。
// AbstractAutoProxyCreator 可以支持多个 TargetSourceCreator，它们将按顺序应用。
@FunctionalInterface
public interface TargetSourceCreator {

	/**
	 * Create a special TargetSource for the given bean, if any.
	 * @param beanClass the class of the bean to create a TargetSource for
	 * @param beanName the name of the bean
	 * @return a special TargetSource or {@code null} if this TargetSourceCreator isn't
	 * interested in the particular bean
	 */
	// 为给定的 bean 创建一个特殊的 TargetSource，如果有的话。
	// 参形：
	//			beanClass – 要为其创建 TargetSource 的 bean 的类
	//			beanName – bean 的名称
	// 返回值：
	//			如果此 TargetSourceCreator 对特定 bean 不感兴趣，则为特殊 TargetSource 或null
	@Nullable
	TargetSource getTargetSource(Class<?> beanClass, String beanName);

}
