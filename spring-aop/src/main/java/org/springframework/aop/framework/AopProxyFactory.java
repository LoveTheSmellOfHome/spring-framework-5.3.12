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

package org.springframework.aop.framework;

/**
 * Interface to be implemented by factories that are able to create
 * AOP proxies based on {@link AdvisedSupport} configuration objects.
 *
 * <p>Proxies should observe the following contract:
 * <ul>
 * <li>They should implement all interfaces that the configuration
 * indicates should be proxied.
 * <li>They should implement the {@link Advised} interface.
 * <li>They should implement the equals method to compare proxied
 * interfaces, advice, and target.
 * <li>They should be serializable if all advisors and target
 * are serializable.
 * <li>They should be thread-safe if advisors and target
 * are thread-safe.
 * </ul>
 *
 * <p>Proxies may or may not allow advice changes to be made.
 * If they do not permit advice changes (for example, because
 * the configuration was frozen) a proxy should throw an
 * {@link AopConfigException} on an attempted advice change.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
// 由能够基于 AdvisedSupport 配置对象创建 AOP 代理的工厂实现的接口。
// 代理人应遵守以下合同：
// 。他们应该实现配置指示应该被代理的所有接口
// 。他们应该实现 Advised 接口
// 。他们应该实现 equals 方法来比较代理接口、建议和目标
// 。如果所有顾问和目标都是可序列化的，它们应该是可序列化的
// 。如果顾问和目标是线程安全的，它们应该是线程安全的
// 代理可能允许也可能不允许更改建议。如果它们不允许更改建议（例如，因为配置被冻结），代理应该在
// 尝试更改建议时抛出 AopConfigException
// 为什么这个接口没有涉及到 AspectJ ？
//
// 抽象工厂模式：
// 		接口的命名：在类或接口的命名上通常以 Factory 结尾，被创建的类作为前缀，通常来讲一种工厂最好只创建一种类型的对象。
//		方法命名：一种方法是以 create/new/get 开头，以被创建的类型为后缀，同时以后缀作为返回值类型
// ThreadFactory
public interface AopProxyFactory {

	/**
	 * Create an {@link AopProxy} for the given AOP configuration.
	 * @param config the AOP configuration in the form of an
	 * AdvisedSupport object
	 * @return the corresponding AOP proxy
	 * @throws AopConfigException if the configuration is invalid
	 */
	// 为给定的 AOP 配置创建一个 AopProxy 。
	// 参形：
	//			config – AdvisedSupport 对象形式的 AOP 配置
	// 返回值：
	//			对应的 AOP 代理
	// 抛出：
	//			AopConfigException – 如果配置无效
	// 两个语义；一个是返回 AopProxy 对象，一个是传递 AdvisedSupport 配置对象
	AopProxy createAopProxy(AdvisedSupport config) throws AopConfigException;

}
