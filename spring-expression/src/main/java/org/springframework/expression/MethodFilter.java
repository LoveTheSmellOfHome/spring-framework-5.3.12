/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.expression;

import java.lang.reflect.Method;
import java.util.List;

/**
 * MethodFilter instances allow SpEL users to fine tune the behaviour of the method
 * resolution process. Method resolution (which translates from a method name in an
 * expression to a real method to invoke) will normally retrieve candidate methods for
 * invocation via a simple call to 'Class.getMethods()' and will choose the first one that
 * is suitable for the input parameters. By registering a MethodFilter the user can
 * receive a callback and change the methods that will be considered suitable.
 *
 * @author Andy Clement
 * @since 3.0.1
 */
// MethodFilter 实例允许 SpEL 用户微调方法解析过程的行为。 方法解析（从表达式中的方法名称转换为要调用的实际方法）通常
// 会通过简单调用“Class.getMethods()”来检索候选方法以进行调用，并将选择适合输入的第一个方法参数。
// 通过注册 MethodFilter 用户可以接收回调并更改将被认为合适的方法。
@FunctionalInterface
public interface MethodFilter {

	/**
	 * Called by the method resolver to allow the SpEL user to organize the list of
	 * candidate methods that may be invoked. The filter can remove methods that should
	 * not be considered candidates and it may sort the results. The resolver will then
	 * search through the methods as returned from the filter when looking for a suitable
	 * candidate to invoke.
	 * @param methods the full list of methods the resolver was going to choose from
	 * @return a possible subset of input methods that may be sorted by order of relevance
	 */
	// 由方法解析器调用以允许 SpEL 用户组织可能被调用的候选方法列表。 过滤器可以删除不应被视为候选的方法，并且可以对结果进行排序。 当寻找合适的候选者时，解析器将搜索从过滤器返回的方法。
	// 形参：methods- 解析器将从中选择的方法的完整列表
	// 返回值：可以按相关性顺序排序的输入方法的可能子集
	List<Method> filter(List<Method> methods);

}
