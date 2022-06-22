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

package org.springframework.aop.framework.autoproxy;

import java.util.List;

import org.springframework.aop.Advisor;
import org.springframework.aop.TargetSource;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Generic auto proxy creator that builds AOP proxies for specific beans
 * based on detected Advisors for each bean.
 *
 * <p>Subclasses may override the {@link #findCandidateAdvisors()} method to
 * return a custom list of Advisors applying to any object. Subclasses can
 * also override the inherited {@link #shouldSkip} method to exclude certain
 * objects from auto-proxying.
 *
 * <p>Advisors or advices requiring ordering should be annotated with
 * {@link org.springframework.core.annotation.Order @Order} or implement the
 * {@link org.springframework.core.Ordered} interface. This class sorts
 * advisors using the {@link AnnotationAwareOrderComparator}. Advisors that are
 * not annotated with {@code @Order} or don't implement the {@code Ordered}
 * interface will be considered as unordered; they will appear at the end of the
 * advisor chain in an undefined order.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see #findCandidateAdvisors
 */
// 通用自动代理创建者，根据检测到的每个 bean 的顾问为特定 bean 构建 AOP 代理。
//
// 子类可以覆盖 findCandidateAdvisors() 方法以返回应用于任何对象的自定义顾问列表。子类还可以覆盖继承的 shouldSkip 方法以
// 从自动代理中排除某些对象。
//
// 需要排序的顾问或建议应使用 @Order 注解或实现 org.springframework.core.Ordered 接口。此类
// 使用 AnnotationAwareOrderComparator 对顾问进行排序。未使用 @Order 注解或未实现 Ordered 接口的 Advisor 将被视为无序；
// 它们将以未定义的顺序出现在顾问链的末尾。
//
// AdvisorAutoProxyCreator：需要在 Spring 应用上下文中查找 Advisor 所对应的 bean,即 IoC 容器中定义的任意的 bean,
// 凡是 Advisor 的他都会拿出来，至于选不选择需要看 AdvisorAutoProxyCreator 的实现类型。比如 InfrastructureAdvisorAutoProxyCreator
// 只过滤基于 Spring 框架内部的 Infrastructure 基础设施，通过过滤 bean 的 Role 是 Infrastructure的。
@SuppressWarnings("serial")
public abstract class AbstractAdvisorAutoProxyCreator extends AbstractAutoProxyCreator {

	// 基于 IoC 上下文容器，来查找 Advisor 的辅助类
	@Nullable
	private BeanFactoryAdvisorRetrievalHelper advisorRetrievalHelper;


	// 关联 IoC 容器
	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		super.setBeanFactory(beanFactory);
		if (!(beanFactory instanceof ConfigurableListableBeanFactory)) {
			throw new IllegalArgumentException(
					"AdvisorAutoProxyCreator requires a ConfigurableListableBeanFactory: " + beanFactory);
		}
		initBeanFactory((ConfigurableListableBeanFactory) beanFactory);
	}

	protected void initBeanFactory(ConfigurableListableBeanFactory beanFactory) {
		// 初始化 Spring IoC 容器时候，实际上在这创建了 BeanFactoryAdvisorRetrievalHelperAdapter 辅助类
		this.advisorRetrievalHelper = new BeanFactoryAdvisorRetrievalHelperAdapter(beanFactory);
	}


	@Override
	@Nullable
	protected Object[] getAdvicesAndAdvisorsForBean(
			Class<?> beanClass, String beanName, @Nullable TargetSource targetSource) {

		// 判断上下文中有没有 Advisor Bean,通过类型依赖查找的方式找到合格的 Advisor 集合
		List<Advisor> advisors = findEligibleAdvisors(beanClass, beanName);
		if (advisors.isEmpty()) {
			return DO_NOT_PROXY;
		}
		return advisors.toArray();
	}

	/**
	 * Find all eligible Advisors for auto-proxying this class.
	 * @param beanClass the clazz to find advisors for
	 * @param beanName the name of the currently proxied bean
	 * @return the empty List, not {@code null},
	 * if there are no pointcuts or interceptors
	 * @see #findCandidateAdvisors
	 * @see #sortAdvisors
	 * @see #extendAdvisors
	 */
	// 查找所有符合条件的顾问以自动代理此类。
	// 参形：
	//			beanClass – 寻找顾问的 clazz
	//			beanName – 当前代理的 bean 的名称
	// 返回值：
	//			如果没有切入点或拦截器，则为空 List，而不是null
	protected List<Advisor> findEligibleAdvisors(Class<?> beanClass, String beanName) {
		List<Advisor> candidateAdvisors = findCandidateAdvisors();
		List<Advisor> eligibleAdvisors = findAdvisorsThatCanApply(candidateAdvisors, beanClass, beanName);
		extendAdvisors(eligibleAdvisors);
		if (!eligibleAdvisors.isEmpty()) {
			eligibleAdvisors = sortAdvisors(eligibleAdvisors);
		}
		return eligibleAdvisors;
	}

	/**
	 * Find all candidate Advisors to use in auto-proxying.
	 * @return the List of candidate Advisors
	 */
	// 查找要在自动代理中使用的所有候选顾问。
	// 返回值：
	//			候选顾问名单
	protected List<Advisor> findCandidateAdvisors() {
		Assert.state(this.advisorRetrievalHelper != null, "No BeanFactoryAdvisorRetrievalHelper available");
		return this.advisorRetrievalHelper.findAdvisorBeans();
	}

	/**
	 * Search the given candidate Advisors to find all Advisors that
	 * can apply to the specified bean.
	 * @param candidateAdvisors the candidate Advisors
	 * @param beanClass the target's bean class
	 * @param beanName the target's bean name
	 * @return the List of applicable Advisors
	 * @see ProxyCreationContext#getCurrentProxiedBeanName()
	 */
	protected List<Advisor> findAdvisorsThatCanApply(
			List<Advisor> candidateAdvisors, Class<?> beanClass, String beanName) {

		ProxyCreationContext.setCurrentProxiedBeanName(beanName);
		try {
			return AopUtils.findAdvisorsThatCanApply(candidateAdvisors, beanClass);
		}
		finally {
			ProxyCreationContext.setCurrentProxiedBeanName(null);
		}
	}

	/**
	 * Return whether the Advisor bean with the given name is eligible
	 * for proxying in the first place.
	 * @param beanName the name of the Advisor bean
	 * @return whether the bean is eligible
	 */
	// 返回具有给定名称的 Advisor bean 首先是否有资格进行代理。
	// 参形：
	//			beanName – 顾问 bean 的名称
	// 返回值：
	//			bean是否符合条件
	// filter:默认进行大过滤
	protected boolean isEligibleAdvisorBean(String beanName) {
		return true;
	}

	/**
	 * Sort advisors based on ordering. Subclasses may choose to override this
	 * method to customize the sorting strategy.
	 * @param advisors the source List of Advisors
	 * @return the sorted List of Advisors
	 * @see org.springframework.core.Ordered
	 * @see org.springframework.core.annotation.Order
	 * @see org.springframework.core.annotation.AnnotationAwareOrderComparator
	 */
	// 根据排序对顾问进行排序。子类可以选择重写此方法来自定义排序策略。
	// 参形：
	//			advisors - 顾问的来源列表
	// 返回值：
	//			排序的顾问列表
	protected List<Advisor> sortAdvisors(List<Advisor> advisors) {
		AnnotationAwareOrderComparator.sort(advisors);
		return advisors;
	}

	/**
	 * Extension hook that subclasses can override to register additional Advisors,
	 * given the sorted Advisors obtained to date.
	 * <p>The default implementation is empty.
	 * <p>Typically used to add Advisors that expose contextual information
	 * required by some of the later advisors.
	 * @param candidateAdvisors the Advisors that have already been identified as
	 * applying to a given bean
	 */
	// 给定迄今为止获得的排序顾问，子类可以覆盖以注册其他顾问的扩展挂钩。
	// 默认实现为空。
	// 通常用于添加顾问，这些顾问公开一些后来的顾问所需的上下文信息。
	// 参形：
	//			CandidateAdvisors – 已被识别为应用于给定 bean 的顾问
	protected void extendAdvisors(List<Advisor> candidateAdvisors) {
	}

	/**
	 * This auto-proxy creator always returns pre-filtered Advisors.
	 */
	// 这个自动代理创建者总是返回预先过滤的顾问
	@Override
	protected boolean advisorsPreFiltered() {
		return true;
	}


	/**
	 * Subclass of BeanFactoryAdvisorRetrievalHelper that delegates to
	 * surrounding AbstractAdvisorAutoProxyCreator facilities.
	 */
	// BeanFactoryAdvisorRetrievalHelper 的子类，它委托给周围的 AbstractAdvisorAutoProxyCreator 设施
	private class BeanFactoryAdvisorRetrievalHelperAdapter extends BeanFactoryAdvisorRetrievalHelper {

		public BeanFactoryAdvisorRetrievalHelperAdapter(ConfigurableListableBeanFactory beanFactory) {
			super(beanFactory);
		}

		// 判断是不是予以挑选的 bean
		@Override
		protected boolean isEligibleBean(String beanName) {
			// 根据子类来筛选
			return AbstractAdvisorAutoProxyCreator.this.isEligibleAdvisorBean(beanName);
		}
	}

}
