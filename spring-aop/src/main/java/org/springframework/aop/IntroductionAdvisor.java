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

package org.springframework.aop;

/**
 * Superinterface for advisors that perform one or more AOP <b>introductions</b>.
 *
 * <p>This interface cannot be implemented directly; subinterfaces must
 * provide the advice type implementing the introduction.
 *
 * <p>Introduction is the implementation of additional interfaces
 * (not implemented by a target) via AOP advice.
 *
 * @author Rod Johnson
 * @since 04.04.2003
 * @see IntroductionInterceptor
 */
// 执行一个或多个 AOP介绍的顾问的超级界面。
// 该接口不能直接实现； 子接口必须提供实现介绍的建议类型。
// 介绍是通过 AOP 建议实现附加接口（不是由目标实现的）。
//
// Introduction 与 Advice 连接器：它包含一部分元信息 IntroductionInfo 同时封装了 ClassFilter
// 它和 PointcutAdvisor 的区别在于，Pointcut 存在两部分过滤，一部分是类型过滤 ClassFilter，一部分是方法过滤 MethodMatcher。
// IntroductionAdvisor 只关心类型过滤，不关心方法过滤。这就是二者的区别
public interface IntroductionAdvisor extends Advisor, IntroductionInfo {

	/**
	 * Return the filter determining which target classes this introduction
	 * should apply to.
	 * <p>This represents the class part of a pointcut. Note that method
	 * matching doesn't make sense to introductions.
	 * @return the class filter
	 */
	// 返回确定此介绍应适用于哪些目标类的过滤器。
	// 这表示切入点的类部分。 请注意，方法匹配对介绍没有意义。
	// 返回值：
	//			类过滤器
	ClassFilter getClassFilter();

	/**
	 * Can the advised interfaces be implemented by the introduction advice?
	 * Invoked before adding an IntroductionAdvisor.
	 * @throws IllegalArgumentException if the advised interfaces can't be
	 * implemented by the introduction advice
	 */
	// 被建议的接口可以通过引入建议来实现吗？ 在添加 IntroductionAdvisor 之前调用。
	// 抛出：
	//			IllegalArgumentException – 如果建议的接口不能被引入建议实现
	void validateInterfaces() throws IllegalArgumentException;

}
