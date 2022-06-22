/*
 * Copyright 2002-2021 the original author or authors.
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

import java.io.Serializable;

import org.springframework.util.Assert;

/**
 * Convenience superclass for configuration used in creating proxies,
 * to ensure that all proxy creators have consistent properties.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see AdvisedSupport
 */
// 用于创建代理的配置的便捷超类，以确保所有代理创建者具有一致的属性
public class ProxyConfig implements Serializable {

	/** use serialVersionUID from Spring 1.2 for interoperability. */
	private static final long serialVersionUID = -8409359707199703185L;


	// 是否使用类代理，否，默认使用 Jdk 动态代理，基于接口代理
	private boolean proxyTargetClass = false;

	// 是否优化
	private boolean optimize = false;

	// 是否不透明
	boolean opaque = false;

	// 需要暴漏的代理，来自于配置,状态模式，配置就是状态，状态影响它是否暴漏给 ThreadLocal
	boolean exposeProxy = false;

	// 配置是否需要冻结，冻结意味着配置信息不会改变
	private boolean frozen = false;


	/**
	 * Set whether to proxy the target class directly, instead of just proxying
	 * specific interfaces. Default is "false".
	 * <p>Set this to "true" to force proxying for the TargetSource's exposed
	 * target class. If that target class is an interface, a JDK proxy will be
	 * created for the given interface. If that target class is any other class,
	 * a CGLIB proxy will be created for the given class.
	 * <p>Note: Depending on the configuration of the concrete proxy factory,
	 * the proxy-target-class behavior will also be applied if no interfaces
	 * have been specified (and no interface autodetection is activated).
	 * @see org.springframework.aop.TargetSource#getTargetClass()
	 */
	// 设置是否直接代理目标类，而不是只代理特定的接口。 默认为“假”。
	//
	// 将此设置为“true”以强制代理 TargetSource 的公开目标类。
	// 如果该目标类是一个接口，则会为给定的接口创建一个 JDK 代理。 如果该目标类是任何其他类，则将为给定类创建 CGLIB 代理。
	//
	// 注意：根据具体代理工厂的配置，如果没有指定接口（并且没有激活接口自动检测），proxy-target-class 行为也将被应用。
	public void setProxyTargetClass(boolean proxyTargetClass) {
		this.proxyTargetClass = proxyTargetClass;
	}

	/**
	 * Return whether to proxy the target class directly as well as any interfaces.
	 */
	// 返回是否直接代理目标类(CGLIB)以及任何接口。类似于
	// @EnableAspectJAutoProxy#boolean proxyTargetClass() default false;
	// 状态模式，配置就是状态，状态影响 DefaultAopProxyFactory#createAopProxy() 的生成策略
	public boolean isProxyTargetClass() {
		return this.proxyTargetClass;
	}

	/**
	 * Set whether proxies should perform aggressive optimizations.
	 * The exact meaning of "aggressive optimizations" will differ
	 * between proxies, but there is usually some tradeoff.
	 * Default is "false".
	 * <p>With Spring's current proxy options, this flag effectively
	 * enforces CGLIB proxies (similar to {@link #setProxyTargetClass})
	 * but without any class validation checks (for final methods etc).
	 */
	// 设置代理是否应该执行积极的优化。 “积极优化”的确切含义因代理而异，但通常会有一些权衡。 默认为“假”。
	//
	// 使用 Spring 的当前代理选项，此标志有效地强制执行 CGLIB 代理（类似于setProxyTargetClass ）但没
	// 有任何类验证检查（对于最终方法等）。
	public void setOptimize(boolean optimize) {
		this.optimize = optimize;
	}

	/**
	 * Return whether proxies should perform aggressive optimizations.
	 */
	// 返回代理是否应该执行积极的优化。
	public boolean isOptimize() {
		return this.optimize;
	}

	/**
	 * Set whether proxies created by this configuration should be prevented
	 * from being cast to {@link Advised} to query proxy status.
	 * <p>Default is "false", meaning that any AOP proxy can be cast to
	 * {@link Advised}.
	 */
	// 设置是否应防止此配置创建的代理被 Advised 转换为 Advised 查询代理状态。
	// 默认值为“false”，这意味着任何 AOP 代理都可以转换为 Advised 。
	public void setOpaque(boolean opaque) {
		this.opaque = opaque;
	}

	/**
	 * Return whether proxies created by this configuration should be
	 * prevented from being cast to {@link Advised}.
	 */
	// 返回是否应防止此配置创建的代理被强制转换为 Advised 。
	public boolean isOpaque() {
		return this.opaque;
	}

	/**
	 * Set whether the proxy should be exposed by the AOP framework as a
	 * ThreadLocal for retrieval via the AopContext class. This is useful
	 * if an advised object needs to call another advised method on itself.
	 * (If it uses {@code this}, the invocation will not be advised).
	 * <p>Default is "false", in order to avoid unnecessary extra interception.
	 * This means that no guarantees are provided that AopContext access will
	 * work consistently within any method of the advised object.
	 */
	// 设置代理是否应由 AOP 框架公开为 ThreadLocal 以通过 AopContext 类进行检索。
	// 如果一个被通知的对象需要对自己调用另一个被通知的方法，这很有用。 （如果它使用this ，将不建议调用）。
	//
	// 默认为“false”，以避免不必要的额外拦截。 这意味着不保证 AopContext 访问将在建议对象的任何方法中一致地工作。
	public void setExposeProxy(boolean exposeProxy) {
		this.exposeProxy = exposeProxy;
	}

	/**
	 * Return whether the AOP proxy will expose the AOP proxy for
	 * each invocation.
	 */
	// 返回 AOP 代理是否会为每次调用公开 AOP 代理。
	public boolean isExposeProxy() {
		return this.exposeProxy;
	}

	/**
	 * Set whether this config should be frozen.
	 * <p>When a config is frozen, no advice changes can be made. This is
	 * useful for optimization, and useful when we don't want callers to
	 * be able to manipulate configuration after casting to Advised.
	 */
	// 设置是否应冻结此配置。
	// 当配置被冻结时，不能进行任何建议更改。 这对于优化很有用，当我们不希望调用者在转换为 Advised 后能够操作配置时很有用。
	public void setFrozen(boolean frozen) {
		this.frozen = frozen;
	}

	/**
	 * Return whether the config is frozen, and no advice changes can be made.
	 */
	// 返回配置是否被冻结，并且不能进行任何建议更改
	public boolean isFrozen() {
		return this.frozen;
	}


	/**
	 * Copy configuration from the other config object.
	 * @param other object to copy configuration from
	 */
	// 从其他配置对象复制配置。
	// 形参：其他- 要从中复制配置的对象
	public void copyFrom(ProxyConfig other) {
		Assert.notNull(other, "Other ProxyConfig object must not be null");
		this.proxyTargetClass = other.proxyTargetClass;
		this.optimize = other.optimize;
		this.exposeProxy = other.exposeProxy;
		this.frozen = other.frozen;
		this.opaque = other.opaque;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("proxyTargetClass=").append(this.proxyTargetClass).append("; ");
		sb.append("optimize=").append(this.optimize).append("; ");
		sb.append("opaque=").append(this.opaque).append("; ");
		sb.append("exposeProxy=").append(this.exposeProxy).append("; ");
		sb.append("frozen=").append(this.frozen);
		return sb.toString();
	}

}
