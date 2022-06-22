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

package org.springframework.aop.aspectj.autoproxy;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.aopalliance.aop.Advice;
import org.aspectj.util.PartialOrder;
import org.aspectj.util.PartialOrder.PartialComparable;

import org.springframework.aop.Advisor;
import org.springframework.aop.aspectj.AbstractAspectJAdvice;
import org.springframework.aop.aspectj.AspectJPointcutAdvisor;
import org.springframework.aop.aspectj.AspectJProxyUtils;
import org.springframework.aop.framework.autoproxy.AbstractAdvisorAutoProxyCreator;
import org.springframework.aop.interceptor.ExposeInvocationInterceptor;
import org.springframework.core.Ordered;
import org.springframework.util.ClassUtils;

/**
 * {@link org.springframework.aop.framework.autoproxy.AbstractAdvisorAutoProxyCreator}
 * subclass that exposes AspectJ's invocation context and understands AspectJ's rules
 * for advice precedence when multiple pieces of advice come from the same aspect.
 *
 * @author Adrian Colyer
 * @author Juergen Hoeller
 * @author Ramnivas Laddad
 * @since 2.0
 */
// AbstractAdvisorAutoProxyCreator 子类公开 AspectJ 的调用上下文并在多条建议来自同一切面时理解 AspectJ 的建议优先级规则
@SuppressWarnings("serial")
public class AspectJAwareAdvisorAutoProxyCreator extends AbstractAdvisorAutoProxyCreator {

	private static final Comparator<Advisor> DEFAULT_PRECEDENCE_COMPARATOR = new AspectJPrecedenceComparator();


	/**
	 * Sort the supplied {@link Advisor} instances according to AspectJ precedence.
	 * <p>If two pieces of advice come from the same aspect, they will have the same
	 * order. Advice from the same aspect is then further ordered according to the
	 * following rules:
	 * <ul>
	 * <li>If either of the pair is <em>after</em> advice, then the advice declared
	 * last gets highest precedence (i.e., runs last).</li>
	 * <li>Otherwise the advice declared first gets highest precedence (i.e., runs
	 * first).</li>
	 * </ul>
	 * <p><b>Important:</b> Advisors are sorted in precedence order, from highest
	 * precedence to lowest. "On the way in" to a join point, the highest precedence
	 * advisor should run first. "On the way out" of a join point, the highest
	 * precedence advisor should run last.
	 */
	// 根据 AspectJ 优先级对提供的Advisor实例进行排序。
	//
	// 如果两条建议来自同一方面，它们将具有相同的顺序。然后根据以下规则进一步排序来自同一方面的建议：
	//
	//  >如果这对中的任何一个在通知之后，则最后声明的通知获得最高优先级（即最后运行）。
	//  >否则，首先声明的通知获得最高优先级（即首先运行）。
	//
	// 重要提示：顾问按优先级排序，从最高优先级到最低优先级。 “在进入”连接点的途中，最高优先级的顾问应该首先运行。
	// “在退出”连接点时，最高优先级的顾问应该最后运行。
	//
	// 模板模式
	@Override
	protected List<Advisor> sortAdvisors(List<Advisor> advisors) {
		List<PartiallyComparableAdvisorHolder> partiallyComparableAdvisors = new ArrayList<>(advisors.size());
		for (Advisor advisor : advisors) {
			partiallyComparableAdvisors.add(
					new PartiallyComparableAdvisorHolder(advisor, DEFAULT_PRECEDENCE_COMPARATOR)); // 使用默认优先级比较器
		}
		List<PartiallyComparableAdvisorHolder> sorted = PartialOrder.sort(partiallyComparableAdvisors);
		if (sorted != null) {
			List<Advisor> result = new ArrayList<>(advisors.size());
			for (PartiallyComparableAdvisorHolder pcAdvisor : sorted) {
				result.add(pcAdvisor.getAdvisor());
			}
			return result;
		}
		else {
			return super.sortAdvisors(advisors);
		}
	}

	/**
	 * Add an {@link ExposeInvocationInterceptor} to the beginning of the advice chain.
	 * <p>This additional advice is needed when using AspectJ pointcut expressions
	 * and when using AspectJ-style advice.
	 */
	// 将 ExposeInvocationInterceptor 添加到建议链的开头。
	// 当使用 AspectJ 切入点表达式和使用 AspectJ 样式的建议时，需要此附加建议
	@Override
	protected void extendAdvisors(List<Advisor> candidateAdvisors) {
		// 使 Advisor 链具有 AspectJ 的能力
		AspectJProxyUtils.makeAdvisorChainAspectJCapableIfNecessary(candidateAdvisors);
	}

	@Override
	protected boolean shouldSkip(Class<?> beanClass, String beanName) {
		// TODO: Consider optimization by caching the list of the aspect names
		// TODO: 考虑通过缓存切面名称列表进行优化
		// 获取所有的 Advisor
		List<Advisor> candidateAdvisors = findCandidateAdvisors();
		for (Advisor advisor : candidateAdvisors) {
			// 如果是 AspectJPointcutAdvisor，并且 bean 名称和切面名称相同，跳过，排除框架内部的东西
			if (advisor instanceof AspectJPointcutAdvisor &&
					((AspectJPointcutAdvisor) advisor).getAspectName().equals(beanName)) {
				return true;
			}
		}
		return super.shouldSkip(beanClass, beanName);
	}


	/**
	 * Implements AspectJ's {@link PartialComparable} interface for defining partial orderings.
	 */
	// 实现 AspectJ 的 PartialOrder.PartialComparable 接口以定义部分排序。
	private static class PartiallyComparableAdvisorHolder implements PartialComparable {

		private final Advisor advisor;

		private final Comparator<Advisor> comparator;

		public PartiallyComparableAdvisorHolder(Advisor advisor, Comparator<Advisor> comparator) {
			this.advisor = advisor;
			this.comparator = comparator;
		}

		@Override
		public int compareTo(Object obj) {
			Advisor otherAdvisor = ((PartiallyComparableAdvisorHolder) obj).advisor;
			return this.comparator.compare(this.advisor, otherAdvisor);
		}

		@Override
		public int fallbackCompareTo(Object obj) {
			return 0;
		}

		public Advisor getAdvisor() {
			return this.advisor;
		}

		@Override
		public String toString() {
			Advice advice = this.advisor.getAdvice();
			StringBuilder sb = new StringBuilder(ClassUtils.getShortName(advice.getClass()));
			boolean appended = false;
			if (this.advisor instanceof Ordered) {
				sb.append(": order = ").append(((Ordered) this.advisor).getOrder());
				appended = true;
			}
			if (advice instanceof AbstractAspectJAdvice) {
				sb.append(!appended ? ": " : ", ");
				AbstractAspectJAdvice ajAdvice = (AbstractAspectJAdvice) advice;
				sb.append("aspect name = ");
				sb.append(ajAdvice.getAspectName());
				sb.append(", declaration order = ");
				sb.append(ajAdvice.getDeclarationOrder());
			}
			return sb.toString();
		}
	}

}
