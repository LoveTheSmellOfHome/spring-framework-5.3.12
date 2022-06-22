/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.core.env;

/**
 * Interface indicating a component that contains and exposes an {@link Environment} reference.
 *
 * <p>All Spring application contexts are EnvironmentCapable, and the interface is used primarily
 * for performing {@code instanceof} checks in framework methods that accept BeanFactory
 * instances that may or may not actually be ApplicationContext instances in order to interact
 * with the environment if indeed it is available.
 *
 * <p>As mentioned, {@link org.springframework.context.ApplicationContext ApplicationContext}
 * extends EnvironmentCapable, and thus exposes a {@link #getEnvironment()} method; however,
 * {@link org.springframework.context.ConfigurableApplicationContext ConfigurableApplicationContext}
 * redefines {@link org.springframework.context.ConfigurableApplicationContext#getEnvironment
 * getEnvironment()} and narrows the signature to return a {@link ConfigurableEnvironment}.
 * The effect is that an Environment object is 'read-only' until it is being accessed from
 * a ConfigurableApplicationContext, at which point it too may be configured.
 *
 * @author Chris Beams
 * @since 3.1
 * @see Environment
 * @see ConfigurableEnvironment
 * @see org.springframework.context.ConfigurableApplicationContext#getEnvironment()
 */
// 指示包含和公开 Environment 引用的组件的接口。
//
// 所有 Spring 应用程序上下文都是 EnvironmentCapable，并且该接口主要用于在框架方法中执行 instanceof 检查，
// 这些方法接受 BeanFactory 实例，这些实例可能实际上是也可能不是 ApplicationContext 实例，以便与环境交互（如果确实可用）。
//
// 如前所述，ApplicationContext 扩展了 EnvironmentCapable，因此公开了一个 getEnvironment()方法；但是，
// ConfigurableApplicationContext 重新定义 getEnvironment() 并缩小了签名以返回 ConfigurableEnvironment 。
// 效果是 Environment 对象在从 ConfigurableApplicationContext 访问之前是“只读的”，此时它也可以被配置。
//
// 接口看它是不是具备 Environment 能力，如果具备返回当前 Environment 对象
public interface EnvironmentCapable {

	/**
	 * Return the {@link Environment} associated with this component.
	 */
	Environment getEnvironment();

}
