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
 * Superinterface for all Advisors that are driven by a pointcut.
 * This covers nearly all advisors except introduction advisors,
 * for which method-level matching doesn't apply.
 *
 * @author Rod Johnson
 */
// 由切入点驱动的所有顾问的超级接口。 这几乎涵盖了除介绍顾问之外的所有顾问，方法级别匹配不适用。
// Pointcut 和 Advice 之间的连接器
// 用户通常不会直接实现 Advisor 而是会实现 PointcutAdvisor，达到两个能力：
// 一个是传递 Advice,另一个是获取我们所说的判断，基于 Pointcut 的过滤，既有判断 Pointcut，又有动作 Advice
// 同时它还有个适配接口
public interface PointcutAdvisor extends Advisor {

	/**
	 * Get the Pointcut that drives this advisor.
	 */
	// 获取驱动这个 Advisor 的切入点
	Pointcut getPointcut();

}
