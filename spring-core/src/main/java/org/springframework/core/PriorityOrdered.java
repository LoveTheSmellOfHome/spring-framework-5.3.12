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

package org.springframework.core;

/**
 * Extension of the {@link Ordered} interface, expressing a <em>priority</em>
 * ordering: {@code PriorityOrdered} objects are always applied before
 * <em>plain</em> {@link Ordered} objects regardless of their order values.
 *
 * <p>When sorting a set of {@code Ordered} objects, {@code PriorityOrdered}
 * objects and <em>plain</em> {@code Ordered} objects are effectively treated as
 * two separate subsets, with the set of {@code PriorityOrdered} objects preceding
 * the set of <em>plain</em> {@code Ordered} objects and with relative
 * ordering applied within those subsets.
 *
 * <p>This is primarily a special-purpose interface, used within the framework
 * itself for objects where it is particularly important to recognize
 * <em>prioritized</em> objects first, potentially without even obtaining the
 * remaining objects. A typical example: prioritized post-processors in a Spring
 * {@link org.springframework.context.ApplicationContext}.
 *
 * <p>Note: {@code PriorityOrdered} post-processor beans are initialized in
 * a special phase, ahead of other post-processor beans. This subtly
 * affects their autowiring behavior: they will only be autowired against
 * beans which do not require eager initialization for type matching.
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 2.5
 * @see org.springframework.beans.factory.config.PropertyOverrideConfigurer
 * @see org.springframework.beans.factory.config.PropertyPlaceholderConfigurer
 */
// Ordered接口的扩展，表示优先级排序： PriorityOrdered对象总是在普通Ordered对象之前应用，无论它们的顺序值如何。
//
// 在对一组Ordered对象进行Ordered ， PriorityOrdered对象和普通Ordered对象被有效地视为两个单独的子集，
// PriorityOrdered对象集位于普通Ordered对象集之前，并且在这些子集中应用了相对排序。
//
// 这主要是一个特殊用途的接口，在框架本身内用于对象，在这些对象中，首先识别特别重要的优先对象，甚至可能不需要获取剩余的对象。
// 一个典型的例子：Springorg.springframework.context.ApplicationContext中的优先后处理器。
//
// 注意： PriorityOrdered后处理器 bean 在一个特殊阶段初始化，在其他后处理器 bean 之前。
// 这会微妙地影响它们的自动装配行为：它们只会针对不需要预先初始化进行类型匹配的 bean 自动装配。
//
// 如果实现了 PriorityOrdered 接口会被提前初始化，提前初始化是有代价的，由于提前初始化涉及的 Bean 的优先级处理顺序，
// 它有可能处理的时候所以来的资源或对象不是那么充分，所以当我们自己实现 BeanPostProcessor 并且实现了 PriorityOrdered
// 一定要千万注意。
public interface PriorityOrdered extends Ordered {
}
