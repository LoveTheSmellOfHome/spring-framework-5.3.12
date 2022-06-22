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


// TODO Is the resolver/executor model too pervasive in this package?
/**
 * Executors are built by resolvers and can be cached by the infrastructure to repeat an
 * operation quickly without going back to the resolvers. For example, the particular
 * constructor to run on a class may be discovered by the reflection constructor resolver
 * - it will then build a ConstructorExecutor that executes that constructor and the
 * ConstructorExecutor can be reused without needing to go back to the resolver to
 * discover the constructor again.
 *
 * <p>They can become stale, and in that case should throw an AccessException - this will
 * cause the infrastructure to go back to the resolvers to ask for a new one.
 *
 * @author Andy Clement
 * @since 3.0
 */
// 执行器由解析器构建，可以被基础设施缓存以快速重复操作而无需返回解析器。
// 例如，在类上运行的特定构造函数可能会被反射构造函数解析器发现——然后它会构建一个执行该构造函数的 ConstructorExecutor，
// 并且 ConstructorExecutor 可以被重用，而无需返回解析器再次发现构造函数
//
// 它们可能会变得陈旧，在这种情况下应该抛出 AccessException - 这将导致基础结构返回到解析器以请求一个新的
public interface ConstructorExecutor {

	/**
	 * Execute a constructor in the specified context using the specified arguments.
	 * @param context the evaluation context in which the command is being executed
	 * @param arguments the arguments to the constructor call, should match (in terms
	 * of number and type) whatever the command will need to run
	 * @return the new object
	 * @throws AccessException if there is a problem executing the command or the
	 * CommandExecutor is no longer valid
	 */
	// 使用指定的参数在指定的上下文中执行构造函数。
	// 形参：
	// context- 执行命令的评估上下文
	// arguments - 构造函数调用的参数，应该匹配（在数量和类型方面）命令需要运行的任何内容
	// 返回值：新对象
	// AccessException – 如果执行命令时出现问题或 CommandExecutor 不再有效
	TypedValue execute(EvaluationContext context, Object... arguments) throws AccessException;

}
