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

package org.springframework.beans.factory.parsing;

import java.util.ArrayList;
import java.util.List;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * {@link ComponentDefinition} implementation that holds one or more nested
 * {@link ComponentDefinition} instances, aggregating them into a named group
 * of components.
 *
 * @author Juergen Hoeller
 * @since 2.0.1
 * @see #getNestedComponents()
 */
// ComponentDefinition 实现包含一个或多个嵌套的 ComponentDefinition 实例，将它们聚合到一组命名的组
public class CompositeComponentDefinition extends AbstractComponentDefinition {

	private final String name;

	@Nullable
	private final Object source;

	private final List<ComponentDefinition> nestedComponents = new ArrayList<>();


	/**
	 * Create a new CompositeComponentDefinition.
	 * @param name the name of the composite component
	 * @param source the source element that defines the root of the composite component
	 */
	// 创建一个新的 CompositeComponentDefinition。
	// 参形：
	//			name – 复合组件的名称
	//			source – 定义复合组件根的源元素
	public CompositeComponentDefinition(String name, @Nullable Object source) {
		Assert.notNull(name, "Name must not be null");
		this.name = name;
		this.source = source;
	}


	@Override
	public String getName() {
		return this.name;
	}

	@Override
	@Nullable
	public Object getSource() {
		return this.source;
	}


	/**
	 * Add the given component as nested element of this composite component.
	 * @param component the nested component to add
	 */
	// 将给定组件添加为此复合组件的嵌套元素。
	// 参形：component – 要添加的嵌套组件
	public void addNestedComponent(ComponentDefinition component) {
		Assert.notNull(component, "ComponentDefinition must not be null");
		this.nestedComponents.add(component);
	}

	/**
	 * Return the nested components that this composite component holds.
	 * @return the array of nested components, or an empty array if none
	 */
	// 返回此复合组件所包含的嵌套组件。
	// 返回值：嵌套组件的数组，如果没有则为空数组
	public ComponentDefinition[] getNestedComponents() {
		return this.nestedComponents.toArray(new ComponentDefinition[0]);
	}

}
