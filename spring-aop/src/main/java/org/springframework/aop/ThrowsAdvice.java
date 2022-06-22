/*
 * Copyright 2002-2008 the original author or authors.
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
 * Tag interface for throws advice.
 *
 * <p>There are not any methods on this interface, as methods are invoked by
 * reflection. Implementing classes must implement methods of the form:
 *
 * <pre class="code">void afterThrowing([Method, args, target], ThrowableSubclass);</pre>
 *
 * <p>Some examples of valid methods would be:
 *
 * <pre class="code">public void afterThrowing(Exception ex)</pre>
 * <pre class="code">public void afterThrowing(RemoteException)</pre>
 * <pre class="code">public void afterThrowing(Method method, Object[] args, Object target, Exception ex)</pre>
 * <pre class="code">public void afterThrowing(Method method, Object[] args, Object target, ServletException ex)</pre>
 *
 * The first three arguments are optional, and only useful if we want further
 * information about the joinpoint, as in AspectJ <b>after-throwing</b> advice.
 *
 * <p><b>Note:</b> If a throws-advice method throws an exception itself, it will
 * override the original exception (i.e. change the exception thrown to the user).
 * The overriding exception will typically be a RuntimeException; this is compatible
 * with any method signature. However, if a throws-advice method throws a checked
 * exception, it will have to match the declared exceptions of the target method
 * and is hence to some degree coupled to specific target method signatures.
 * <b>Do not throw an undeclared checked exception that is incompatible with
 * the target method's signature!</b>
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see AfterReturningAdvice
 * @see MethodBeforeAdvice
 */
// 用于抛出建议的标记接口。
// 此接口上没有任何方法，因为方法是通过反射调用的。 实现类必须实现以下形式的方法：
// void afterThrowing([Method, args, target], ThrowableSubclass);
// 一些有效方法的例子是：
// public void afterThrowing(Exception ex)
// public void afterThrowing(RemoteException)
// public void afterThrowing(Method method, Object[] args, Object target, Exception ex)
// public void afterThrowing(Method method, Object[] args, Object target, ServletException ex)
// 前三个参数是可选的，只有在我们需要有关连接点的更多信息时才有用，如在 AspectJ后抛出建议中。
// 注意：如果 throws-advice 方法本身抛出异常，它将覆盖原始异常（即更改抛出给用户的异常）。
// 覆盖异常通常是 RuntimeException； 这与任何方法签名兼容。
// 然而，如果 throws-advice 方法抛出一个已检查的异常，它将必须匹配目标方法的声明异常
// ，因此在某种程度上与特定的目标方法签名耦合。 不要抛出与目标方法的签名不兼容的未声明的检查异常！
//
// Spring 没有提供该接口的实现
public interface ThrowsAdvice extends AfterAdvice {

}
