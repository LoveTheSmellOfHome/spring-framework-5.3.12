/*
 * Copyright 2002-2020 the original author or authors.
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

import org.aopalliance.aop.Advice;

import org.springframework.aop.Advisor;
import org.springframework.aop.TargetClassAware;
import org.springframework.aop.TargetSource;

/**
 * Interface to be implemented by classes that hold the configuration
 * of a factory of AOP proxies. This configuration includes the
 * Interceptors and other advice, Advisors, and the proxied interfaces.
 *
 * <p>Any AOP proxy obtained from Spring can be cast to this interface to
 * allow manipulation of its AOP advice.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 13.03.2003
 * @see org.springframework.aop.framework.AdvisedSupport
 */
// 由持有 AOP 代理工厂配置的类实现的接口。 此配置包括拦截器和其他
// 建议(other advice)、顾问(Advisors)和代理接口(the proxied interfaces)。
//
// 从 Spring 获得的任何 AOP 代理都可以转换为该接口，以允许操作其 AOP 建议。
public interface Advised extends TargetClassAware {

	/**
	 * Return whether the Advised configuration is frozen,
	 * in which case no advice changes can be made.
	 */
	// 返回 Advised 配置是否被冻结，在这种情况下不能进行任何建议更改。
	boolean isFrozen();

	/**
	 * Are we proxying the full target class instead of specified interfaces?
	 */
	// 我们是否代理了完整的目标类而不是指定的接口？
	boolean isProxyTargetClass();

	/**
	 * Return the interfaces proxied by the AOP proxy.
	 * <p>Will not include the target class, which may also be proxied.
	 */
	// 返回由 AOP 代理代理的接口。
	// 不包括也可能被代理的目标类
	Class<?>[] getProxiedInterfaces();

	/**
	 * Determine whether the given interface is proxied.
	 * @param intf the interface to check
	 */
	// 确定给定接口是否被代理。
	// 参形：intf – 要检查的接口
	boolean isInterfaceProxied(Class<?> intf);

	/**
	 * Change the {@code TargetSource} used by this {@code Advised} object.
	 * <p>Only works if the configuration isn't {@linkplain #isFrozen frozen}.
	 * @param targetSource new TargetSource to use
	 */
	// 更改TargetSource这个使用Advised对象。
	// 仅在配置未冻结时才有效。
	// 参形：targetSource – 要使用的新 TargetSource，设置资源池
	void setTargetSource(TargetSource targetSource);

	/**
	 * Return the {@code TargetSource} used by this {@code Advised} object.
	 */
	// 返回 TargetSource 这个使用 Advised 对象
	TargetSource getTargetSource();

	/**
	 * Set whether the proxy should be exposed by the AOP framework as a
	 * {@link ThreadLocal} for retrieval via the {@link AopContext} class.
	 * <p>It can be necessary to expose the proxy if an advised object needs
	 * to invoke a method on itself with advice applied. Otherwise, if an
	 * advised object invokes a method on {@code this}, no advice will be applied.
	 * <p>Default is {@code false}, for optimal performance.
	 */
	// 设置代理是否应由 AOP 框架公开为 ThreadLocal 以通过 AopContext 类进行检索。
	//
	// 如果通知对象需要在应用了通知的情况下对其自身调用方法，则可能需要公开代理。
	// 否则，如果通知对象调用this上的方法，则不会应用通知。
	//
	// 默认为false ，以获得最佳性能。
	void setExposeProxy(boolean exposeProxy);

	/**
	 * Return whether the factory should expose the proxy as a {@link ThreadLocal}.
	 * <p>It can be necessary to expose the proxy if an advised object needs
	 * to invoke a method on itself with advice applied. Otherwise, if an
	 * advised object invokes a method on {@code this}, no advice will be applied.
	 * <p>Getting the proxy is analogous to an EJB calling {@code getEJBObject()}.
	 * @see AopContext
	 */
	// 返回工厂是否应该将代理公开为ThreadLocal 。
	//
	// 如果通知对象需要在应用了通知的情况下对其自身调用方法，则可能需要公开代理。 否则，如果通知
	// 对象调用this上的方法，则不会应用通知。
	//
	// 获取代理类似于调用getEJBObject()的 EJB。
	boolean isExposeProxy();

	/**
	 * Set whether this proxy configuration is pre-filtered so that it only
	 * contains applicable advisors (matching this proxy's target class).
	 * <p>Default is "false". Set this to "true" if the advisors have been
	 * pre-filtered already, meaning that the ClassFilter check can be skipped
	 * when building the actual advisor chain for proxy invocations.
	 * @see org.springframework.aop.ClassFilter
	 */
	// 设置是否预过滤此代理配置，使其仅包含适用的顾问（匹配此代理的目标类）。
	//
	// 默认为“假”。 如果顾问已经被预先过滤，则将此设置为“true”，这意味着在为代理调用
	// 构建实际顾问链时可以跳过 ClassFilter 检查。
	void setPreFiltered(boolean preFiltered);

	/**
	 * Return whether this proxy configuration is pre-filtered so that it only
	 * contains applicable advisors (matching this proxy's target class).
	 */
	// 返回此代理配置是否经过预过滤，使其仅包含适用的顾问（匹配此代理的目标类）。
	boolean isPreFiltered();

	/**
	 * Return the advisors applying to this proxy.
	 * @return a list of Advisors applying to this proxy (never {@code null})
	 */
	// 返回申请此代理的顾问。
	// 返回值：应用于此代理的顾问列表（从不为null ）
	Advisor[] getAdvisors();

	/**
	 * Return the number of advisors applying to this proxy.
	 * <p>The default implementation delegates to {@code getAdvisors().length}.
	 * @since 5.3.1
	 */
	// 返回申请此代理的顾问数量。
	// 默认实现委托给getAdvisors().length
	default int getAdvisorCount() {
		return getAdvisors().length;
	}

	/**
	 * Add an advisor at the end of the advisor chain.
	 * <p>The Advisor may be an {@link org.springframework.aop.IntroductionAdvisor},
	 * in which new interfaces will be available when a proxy is next obtained
	 * from the relevant factory.
	 * @param advisor the advisor to add to the end of the chain
	 * @throws AopConfigException in case of invalid advice
	 */
	// 在顾问链的末尾添加一个顾问。
	// Advisor 可能是org.springframework.aop.IntroductionAdvisor ，当下次从相关工厂获得代理时，新接口将可用。
	// 参形：
	//			advisor – 添加到链末端的顾问
	// 抛出：
	//			AopConfigException – 无效建议的情况
	void addAdvisor(Advisor advisor) throws AopConfigException;

	/**
	 * Add an Advisor at the specified position in the chain.
	 * @param advisor the advisor to add at the specified position in the chain
	 * @param pos position in chain (0 is head). Must be valid.
	 * @throws AopConfigException in case of invalid advice
	 */
	// 在链中的指定位置添加顾问。
	// 参形：
	//			pos – 链中的位置（0 是头部）。 必须有效。
	//			advisor – 要添加到链中指定位置的顾问
	// 抛出：
	//			AopConfigException – 无效建议的情况
	void addAdvisor(int pos, Advisor advisor) throws AopConfigException;

	/**
	 * Remove the given advisor.
	 * @param advisor the advisor to remove
	 * @return {@code true} if the advisor was removed; {@code false}
	 * if the advisor was not found and hence could not be removed
	 */
	// 删除给定的顾问。
	// 参形：
	//			advisor – 要删除的顾问
	// 返回值：
	//			如果顾问被移除，则为true ； 如果未找到顾问并因此无法删除，则为false
	boolean removeAdvisor(Advisor advisor);

	/**
	 * Remove the advisor at the given index.
	 * @param index the index of advisor to remove
	 * @throws AopConfigException if the index is invalid
	 */
	// 删除给定索引处的顾问。
	// 参形：
	//			index – 要删除的顾问的索引
	// 抛出：
	//			AopConfigException – 如果索引无效
	void removeAdvisor(int index) throws AopConfigException;

	/**
	 * Return the index (from 0) of the given advisor,
	 * or -1 if no such advisor applies to this proxy.
	 * <p>The return value of this method can be used to index into the advisors array.
	 * @param advisor the advisor to search for
	 * @return index from 0 of this advisor, or -1 if there's no such advisor
	 */
	// 返回给定顾问的索引（从 0 开始），如果没有此类顾问适用于此代理，则返回 -1。
	// 此方法的返回值可用于索引顾问数组。
	// 参形：
	//			advisor – 要搜索的顾问
	// 返回值：
	//			从该顾问的 0 开始的索引，如果没有这样的顾问，则为 -1
	int indexOf(Advisor advisor);

	/**
	 * Replace the given advisor.
	 * <p><b>Note:</b> If the advisor is an {@link org.springframework.aop.IntroductionAdvisor}
	 * and the replacement is not or implements different interfaces, the proxy will need
	 * to be re-obtained or the old interfaces won't be supported and the new interface
	 * won't be implemented.
	 * @param a the advisor to replace
	 * @param b the advisor to replace it with
	 * @return whether it was replaced. If the advisor wasn't found in the
	 * list of advisors, this method returns {@code false} and does nothing.
	 * @throws AopConfigException in case of invalid advice
	 */
	// 替换给定的顾问。
	// 注意：如果advisor是org.springframework.aop.IntroductionAdvisor并且替换不是或实现了不同的接口，
	// 则需要重新获取代理，否则旧接口将不被支持，新接口将不会被支持实施的。
	// 参形：
	//			a – 要替换的顾问
	//			b – 将其替换为的顾问
	// 返回值：
	//			是否被替换。 如果在顾问列表中找不到顾问，则此方法返回false并且不执行任何操作。
	// 抛出：
	//			AopConfigException – 无效建议的情况
	boolean replaceAdvisor(Advisor a, Advisor b) throws AopConfigException;

	/**
	 * Add the given AOP Alliance advice to the tail of the advice (interceptor) chain.
	 * <p>This will be wrapped in a DefaultPointcutAdvisor with a pointcut that always
	 * applies, and returned from the {@code getAdvisors()} method in this wrapped form.
	 * <p>Note that the given advice will apply to all invocations on the proxy,
	 * even to the {@code toString()} method! Use appropriate advice implementations
	 * or specify appropriate pointcuts to apply to a narrower set of methods.
	 * @param advice the advice to add to the tail of the chain
	 * @throws AopConfigException in case of invalid advice
	 * @see #addAdvice(int, Advice)
	 * @see org.springframework.aop.support.DefaultPointcutAdvisor
	 */
	// 将给定的 AOP 联盟建议添加到建议（拦截器）链的尾部。
	//
	// 这将被包装在具有始终适用的切入点的 DefaultPointcutAdvisor 中，并以这种包装形式从getAdvisors()方法返回。
	//
	// 请注意，给定的建议将适用于代理上的所有调用，甚至适用于toString()方法！ 使用适当的建议
	// 实现或指定适当的切入点以应用于更窄的方法集。
	// 参形：
	//			advice - 添加到链尾部的建议
	// 抛出：
	//			AopConfigException – 无效建议的情况
	void addAdvice(Advice advice) throws AopConfigException;

	/**
	 * Add the given AOP Alliance Advice at the specified position in the advice chain.
	 * <p>This will be wrapped in a {@link org.springframework.aop.support.DefaultPointcutAdvisor}
	 * with a pointcut that always applies, and returned from the {@link #getAdvisors()}
	 * method in this wrapped form.
	 * <p>Note: The given advice will apply to all invocations on the proxy,
	 * even to the {@code toString()} method! Use appropriate advice implementations
	 * or specify appropriate pointcuts to apply to a narrower set of methods.
	 * @param pos index from 0 (head)
	 * @param advice the advice to add at the specified position in the advice chain
	 * @throws AopConfigException in case of invalid advice
	 */
	// 在建议链中的指定位置添加给定的 AOP 联盟建议。
	//
	// 这将被包装在一个带有始终适用的切入点的org.springframework.aop.support.DefaultPointcutAdvisor ，
	// 并以这种包装形式从getAdvisors()方法返回。
	//
	// 注意：给定的建议将适用于代理上的所有调用，甚至适用于toString()方法！ 使用适当的建议实现或指定
	// 适当的切入点以应用于更窄的方法集。
	// 参形：
	//			pos – 从 0 开始的索引（头部）
	//			advice - 在建议链中的指定位置添加的建议
	// 抛出：
	//			AopConfigException – 无效建议的情况
	void addAdvice(int pos, Advice advice) throws AopConfigException;

	/**
	 * Remove the Advisor containing the given advice.
	 * @param advice the advice to remove
	 * @return {@code true} of the advice was found and removed;
	 * {@code false} if there was no such advice
	 */
	// 删除包含给定建议的顾问。
	// 参形：
	//			advice - 删除的建议
	// 返回值：
	//			发现并删除了建议的true ； 如果没有这样的建议，则为false
	boolean removeAdvice(Advice advice);

	/**
	 * Return the index (from 0) of the given AOP Alliance Advice,
	 * or -1 if no such advice is an advice for this proxy.
	 * <p>The return value of this method can be used to index into
	 * the advisors array.
	 * @param advice the AOP Alliance advice to search for
	 * @return index from 0 of this advice, or -1 if there's no such advice
	 */
	// 返回给定 AOP 联盟建议的索引（从 0 开始），如果没有这样的建议是此代理的建议，则返回 -1。
	// 此方法的返回值可用于索引顾问数组。
	// 参形：
	//			advice – AOP 联盟建议搜索
	// 返回值：
	//			从这个建议的 0 开始索引，如果没有这样的建议，则为 -1
	int indexOf(Advice advice);

	/**
	 * As {@code toString()} will normally be delegated to the target,
	 * this returns the equivalent for the AOP proxy.
	 * @return a string description of the proxy configuration
	 */
	// 由于 toString() 通常会被委托给目标，这将返回 AOP 代理的等价物。
	// 返回值：代理配置的字符串描述
	String toProxyConfigString();

}
