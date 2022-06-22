/*
 * Copyright 2002-2013 the original author or authors.
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

import java.util.List;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.lang.Nullable;

/**
 * A method resolver attempts locate a method and returns a command executor that can be
 * used to invoke that method. The command executor will be cached but if it 'goes stale'
 * the resolvers will be called again.
 *
 * @author Andy Clement
 * @since 3.0
 */
// 方法解析器尝试定位方法并返回可用于调用该方法的命令执行器。
// 命令执行器将被缓存，但如果它“过时”，将再次调用解析器。
public interface MethodResolver {

	/**
	 * Within the supplied context determine a suitable method on the supplied object that
	 * can handle the specified arguments. Return a {@link MethodExecutor} that can be used
	 * to invoke that method, or {@code null} if no method could be found.
	 * @param context the current evaluation context
	 * @param targetObject the object upon which the method is being called
	 * @param argumentTypes the arguments that the constructor must be able to handle
	 * @return a MethodExecutor that can invoke the method, or null if the method cannot be found
	 */
	// 在提供的上下文中，在提供的对象上确定可以处理指定参数的合适方法。 返回可用于调用该方法的MethodExecutor ，如果找不到方法，则返回null 。
	// 形参：
	// context - 当前的评估上下文
	// targetObject – 调用方法的对象
	// argumentTypes – 构造函数必须能够处理的参数
	// 返回值：可以调用该方法的 MethodExecutor，如果找不到该方法，则为 null
	@Nullable
	MethodExecutor resolve(EvaluationContext context, Object targetObject, String name,
			List<TypeDescriptor> argumentTypes) throws AccessException;

}
