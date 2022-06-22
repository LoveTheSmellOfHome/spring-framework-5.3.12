/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.beans.factory.parsing;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanReference;

/**
 * Base implementation of {@link ComponentDefinition} that provides a basic implementation of
 * {@link #getDescription} which delegates to {@link #getName}. Also provides a base implementation
 * of {@link #toString} which delegates to {@link #getDescription} in keeping with the recommended
 * implementation strategy. Also provides default implementations of {@link #getInnerBeanDefinitions}
 * and {@link #getBeanReferences} that return an empty array.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 2.0
 */
// ComponentDefinition 的基本实现，它提供了委托给 getName 的 getDescription 的基本实现。还提供了 toString 的基本实现，
// 它委托给 getDescription 以符合推荐的实现策略。还提供了返回空数组的 getInnerBeanDefinitions 和 getBeanReferences 的默认实现。
//
// 和 XML 元素密切相关的东西
public abstract class AbstractComponentDefinition implements ComponentDefinition {

	/**
	 * Delegates to {@link #getName}.
	 */
	@Override
	public String getDescription() {
		return getName();
	}

	/**
	 * Returns an empty array.
	 */
	@Override
	public BeanDefinition[] getBeanDefinitions() {
		return new BeanDefinition[0];
	}

	/**
	 * Returns an empty array.
	 */
	@Override
	public BeanDefinition[] getInnerBeanDefinitions() {
		return new BeanDefinition[0];
	}

	/**
	 * Returns an empty array.
	 */
	@Override
	public BeanReference[] getBeanReferences() {
		return new BeanReference[0];
	}

	/**
	 * Delegates to {@link #getDescription}.
	 */
	@Override
	public String toString() {
		return getDescription();
	}

}
