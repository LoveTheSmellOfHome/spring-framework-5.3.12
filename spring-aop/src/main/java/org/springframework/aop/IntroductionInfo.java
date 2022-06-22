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
 * Interface supplying the information necessary to describe an introduction.
 *
 * <p>{@link IntroductionAdvisor IntroductionAdvisors} must implement this
 * interface. If an {@link org.aopalliance.aop.Advice} implements this,
 * it may be used as an introduction without an {@link IntroductionAdvisor}.
 * In this case, the advice is self-describing, providing not only the
 * necessary behavior, but describing the interfaces it introduces.
 *
 * @author Rod Johnson
 * @since 1.1.1
 */
// 提供描述介绍所需信息的接口。
//
// IntroductionAdvisors 必须实现这个接口。 如果 org.aopalliance.aop.Advice 实现了这一点，
// 它可以用作没有 IntroductionAdvisor 。 在这种情况下，建议是自描述的，不仅提供必要的行为，而且描述它引入的接口。
// IntroductionInfo 动态的管理代理对象实现的接口，它可以扩展目标对象之外的接口，但是最好使用目标对象实现的接口的全集或者子集
public interface IntroductionInfo {

	/**
	 * Return the additional interfaces introduced by this Advisor or Advice.
	 * @return the introduced interfaces
	 */
	// 返回此 Advisor 或 Advice 引入的附加接口。
	// 返回值：引入的接口
	Class<?>[] getInterfaces();

}
