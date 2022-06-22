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

package org.springframework.aop;

import org.aopalliance.aop.Advice;

/**
 * Subinterface of AOP Alliance Advice that allows additional interfaces
 * to be implemented by an Advice, and available via a proxy using that
 * interceptor. This is a fundamental AOP concept called <b>introduction</b>.
 *
 * <p>Introductions are often <b>mixins</b>, enabling the building of composite
 * objects that can achieve many of the goals of multiple inheritance in Java.
 *
 * <p>Compared to {@link IntroductionInfo}, this interface allows an advice to
 * implement a range of interfaces that is not necessarily known in advance.
 * Thus an {@link IntroductionAdvisor} can be used to specify which interfaces
 * will be exposed in an advised object.
 *
 * @author Rod Johnson
 * @since 1.1.1
 * @see IntroductionInfo
 * @see IntroductionAdvisor
 */
// AOP Alliance Advice 的子接口，允许由 Advice 实现附加接口，并且可以通过使用该拦截器的代理获得。
// 这是一个称为 Introduction 的基本 AOP 概念。
//
// 引入通常是 mixins ，可以构建复合对象，从而实现 Java 中多重继承的许多目标。
//
// 与 IntroductionInfo 相比，此接口允许通知实现事先不一定知道的一系列接口。因此， IntroductionAdvisor 可用于指定
// 将在建议对象中公开哪些接口。
public interface DynamicIntroductionAdvice extends Advice {

	/**
	 * Does this introduction advice implement the given interface?
	 * @param intf the interface to check
	 * @return whether the advice implements the specified interface
	 */
	// 这个介绍建议是否实现了给定的接口？
	// 参形：intf – 要检查的接口
	// 返回值：通知是否实现了指定的接口
	boolean implementsInterface(Class<?> intf);

}
