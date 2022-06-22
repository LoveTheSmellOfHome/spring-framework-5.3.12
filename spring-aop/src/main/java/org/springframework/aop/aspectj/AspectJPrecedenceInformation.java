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

package org.springframework.aop.aspectj;

import org.springframework.core.Ordered;

/**
 * Interface to be implemented by types that can supply the information
 * needed to sort advice/advisors by AspectJ's precedence rules.
 *
 * @author Adrian Colyer
 * @since 2.0
 * @see org.springframework.aop.aspectj.autoproxy.AspectJPrecedenceComparator
 */
// 由可以提供按 AspectJ 的优先规则对通知/顾问进行排序所需的信息的类型实现的接口
// 供用户实现的 AspectJ 排序接口
public interface AspectJPrecedenceInformation extends Ordered {

	// Implementation note:
	// We need the level of indirection this interface provides as otherwise the
	// AspectJPrecedenceComparator must ask an Advisor for its Advice in all cases
	// in order to sort advisors. This causes problems with the
	// InstantiationModelAwarePointcutAdvisor which needs to delay creating
	// its advice for aspects with non-singleton instantiation models.
	//
	// 实现说明：我们需要此接口提供的间接级别，否则 AspectJPrecedenceComparator 在所有情况下都必须向顾问询问其建议，以便对顾问进行排序。
	// 这会导致 InstantiationModelAwarePointcutAdvisor 出现问题，需要延迟为非单例实例化模型的方面创建建议。

	/**
	 * Return the name of the aspect (bean) in which the advice was declared.
	 */
	// 返回声明通知的切面（bean）的名称。
	String getAspectName();

	/**
	 * Return the declaration order of the advice member within the aspect.
	 */
	// 返回切面内通知成员的声明顺序。
	int getDeclarationOrder();

	/**
	 * Return whether this is a before advice.
	 */
	// 返回这是否是之前的建议
	boolean isBeforeAdvice();

	/**
	 * Return whether this is an after advice.
	 */
	// 返回这是否是事后建议
	boolean isAfterAdvice();

}
