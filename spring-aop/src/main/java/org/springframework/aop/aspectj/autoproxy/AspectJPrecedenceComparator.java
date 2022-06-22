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

import java.util.Comparator;

import org.springframework.aop.Advisor;
import org.springframework.aop.aspectj.AspectJAopUtils;
import org.springframework.aop.aspectj.AspectJPrecedenceInformation;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.util.Assert;

/**
 * Orders AspectJ advice/advisors by precedence (<i>not</i> invocation order).
 *
 * <p>Given two pieces of advice, {@code A} and {@code B}:
 * <ul>
 * <li>If {@code A} and {@code B} are defined in different aspects, then the advice
 * in the aspect with the lowest order value has the highest precedence.</li>
 * <li>If {@code A} and {@code B} are defined in the same aspect, if one of
 * {@code A} or {@code B} is a form of <em>after</em> advice, then the advice declared
 * last in the aspect has the highest precedence. If neither {@code A} nor {@code B}
 * is a form of <em>after</em> advice, then the advice declared first in the aspect
 * has the highest precedence.</li>
 * </ul>
 *
 * <p>Important: This comparator is used with AspectJ's
 * {@link org.aspectj.util.PartialOrder PartialOrder} sorting utility. Thus, unlike
 * a normal {@link Comparator}, a return value of {@code 0} from this comparator
 * means we don't care about the ordering, not that the two elements must be sorted
 * identically.
 *
 * @author Adrian Colyer
 * @author Juergen Hoeller
 * @since 2.0
 */
// 按优先级排序 AspectJ 建议/顾问（不是调用顺序）。
//
// 给出两条建议， A和B ：
//  >如果A和B被定义在不同的方面，那么具有最低顺序值的方面的通知具有最高优先级。
//  >如果A和B定义在同一个方面，如果A或B中的一个是后通知的形式，则在方面中最后声明的通知具有最高优先级。
//   如果A和B都不是after通知的一种形式，则在方面中首先声明的通知具有最高优先级。
//
// 重要提示：此比较器与 AspectJ 的PartialOrder排序实用程序一起使用。因此，与普通的Comparator不同，
// 此比较器的返回值0意味着我们不关心排序，而不是两个元素必须以相同的方式排序。
//
// AspectJ 优先级比较器
class AspectJPrecedenceComparator implements Comparator<Advisor> {

	// 高优先级
	private static final int HIGHER_PRECEDENCE = -1;

	// 同一优先级
	private static final int SAME_PRECEDENCE = 0;

	// 低优先级
	private static final int LOWER_PRECEDENCE = 1;


	private final Comparator<? super Advisor> advisorComparator;


	/**
	 * Create a default {@code AspectJPrecedenceComparator}.
	 */
	// 创建一个默认的 AspectJPrecedenceComparator
	public AspectJPrecedenceComparator() {
		// Advisor 的操作使用的是 Spring 中 Order 或 Ordered 接口的 Comparator
		this.advisorComparator = AnnotationAwareOrderComparator.INSTANCE;
	}

	/**
	 * Create an {@code AspectJPrecedenceComparator}, using the given {@link Comparator}
	 * for comparing {@link org.springframework.aop.Advisor} instances.
	 * @param advisorComparator the {@code Comparator} to use for advisors
	 */
	// 创建一个 AspectJPrecedenceComparator，使用给定的 Comparator 比较 Advisor 实例。
	// 参形：advisorComparator – 用于顾问的 Comparator器
	public AspectJPrecedenceComparator(Comparator<? super Advisor> advisorComparator) {
		Assert.notNull(advisorComparator, "Advisor comparator must not be null");
		this.advisorComparator = advisorComparator;
	}


	@Override
	public int compare(Advisor o1, Advisor o2) {
		int advisorPrecedence = this.advisorComparator.compare(o1, o2);
		// 比较同一优先级，即在注解上标注的数字一样，或者都没有标注，或者仅标注 Order,就认为是相同优先级
		// 如果同一优先级并且注解声明在同一切面上
		if (advisorPrecedence == SAME_PRECEDENCE && declaredInSameAspect(o1, o2)) {
			// 通过切面比较优先级
			advisorPrecedence = comparePrecedenceWithinAspect(o1, o2);
		}
		return advisorPrecedence;
	}

	private int comparePrecedenceWithinAspect(Advisor advisor1, Advisor advisor2) {
		// 如果两个 Advisor 任意一个是 AfterAdvice
		boolean oneOrOtherIsAfterAdvice =
				(AspectJAopUtils.isAfterAdvice(advisor1) || AspectJAopUtils.isAfterAdvice(advisor2));
		int adviceDeclarationOrderDelta = getAspectDeclarationOrder(advisor1) - getAspectDeclarationOrder(advisor2);

		if (oneOrOtherIsAfterAdvice) {
			// the advice declared last has higher precedence
			// 最后声明的通知具有更高的优先级
			if (adviceDeclarationOrderDelta < 0) {
				// advice1 was declared before advice2
				// so advice1 has lower precedence
				// 建议 1 在建议 2 之前声明，因此建议 1 的优先级较低
				return LOWER_PRECEDENCE;
			}
			else if (adviceDeclarationOrderDelta == 0) {
				return SAME_PRECEDENCE;
			}
			else {
				return HIGHER_PRECEDENCE;
			}
		}
		else {
			// the advice declared first has higher precedence
			// 首先声明的通知具有更高的优先级
			if (adviceDeclarationOrderDelta < 0) {
				// advice1 was declared before advice2
				// so advice1 has higher precedence
				// 建议 1 在建议 2 之前声明，因此建议 1 具有更高的优先级
				return HIGHER_PRECEDENCE;
			}
			else if (adviceDeclarationOrderDelta == 0) {
				return SAME_PRECEDENCE;
			}
			else {
				return LOWER_PRECEDENCE;
			}
		}
	}

	// 判断是否在同一个切面上
	private boolean declaredInSameAspect(Advisor advisor1, Advisor advisor2) {
		return (hasAspectName(advisor1) && hasAspectName(advisor2) &&
				getAspectName(advisor1).equals(getAspectName(advisor2)));
	}

	private boolean hasAspectName(Advisor advisor) {
		return (advisor instanceof AspectJPrecedenceInformation ||
				advisor.getAdvice() instanceof AspectJPrecedenceInformation);
	}

	// pre-condition is that hasAspectName returned true
	// 前提是 hasAspectName 返回 true
	private String getAspectName(Advisor advisor) {
		AspectJPrecedenceInformation precedenceInfo = AspectJAopUtils.getAspectJPrecedenceInformationFor(advisor);
		Assert.state(precedenceInfo != null, () -> "Unresolvable AspectJPrecedenceInformation for " + advisor);
		return precedenceInfo.getAspectName();
	}

	// Aspect 自己声明的顺序
	private int getAspectDeclarationOrder(Advisor advisor) {
		AspectJPrecedenceInformation precedenceInfo = AspectJAopUtils.getAspectJPrecedenceInformationFor(advisor);
		// 利用接口中声明的顺序再次排序
		return (precedenceInfo != null ? precedenceInfo.getDeclarationOrder() : 0);
	}

}
