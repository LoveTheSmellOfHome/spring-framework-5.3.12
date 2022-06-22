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

package org.aopalliance.intercept;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Intercepts calls on an interface on its way to the target. These
 * are nested "on top" of the target.
 *
 * <p>The user should implement the {@link #invoke(MethodInvocation)}
 * method to modify the original behavior. E.g. the following class
 * implements a tracing interceptor (traces all the calls on the
 * intercepted method(s)):
 *
 * <pre class=code>
 * class TracingInterceptor implements MethodInterceptor {
 *   Object invoke(MethodInvocation i) throws Throwable {
 *     System.out.println("method "+i.getMethod()+" is called on "+
 *                        i.getThis()+" with args "+i.getArguments());
 *     Object ret=i.proceed();
 *     System.out.println("method "+i.getMethod()+" returns "+ret);
 *     return ret;
 *   }
 * }
 * </pre>
 *
 * @author Rod Johnson
 */
// 在到达目标的途中拦截对接口的调用。 这些嵌套在目标的“顶部”。
//
// 用户应实现invoke(MethodInvocation)方法来修改原始行为。 例如，下面的类实现了一个跟踪
// 拦截器（跟踪对被拦截方法的所有调用）：
//   class TracingInterceptor implements MethodInterceptor {
//     Object invoke(MethodInvocation i) throws Throwable {
//       System.out.println("method "+i.getMethod()+" is called on "+
//                          i.getThis()+" with args "+i.getArguments());
//       Object ret=i.proceed();
//       System.out.println("method "+i.getMethod()+" returns "+ret);
//       return ret;
//     }
//   }
@FunctionalInterface
public interface MethodInterceptor extends Interceptor {

	/**
	 * Implement this method to perform extra treatments before and
	 * after the invocation. Polite implementations would certainly
	 * like to invoke {@link Joinpoint#proceed()}.
	 * @param invocation the method invocation joinpoint
	 * @return the result of the call to {@link Joinpoint#proceed()};
	 * might be intercepted by the interceptor
	 * @throws Throwable if the interceptors or the target object
	 * throws an exception
	 */
	// 实现此方法以在调用前后执行额外的处理。 礼貌的实现当然想调用Joinpoint.proceed() 。
	// 参形：
	//			invocation – 方法调用连接点
	// 返回值：
	//			调用 Joinpoint.proceed() 的结果； 可能被拦截器拦截
	// 抛出：
	//			Throwable – 如果拦截器或目标对象抛出异常
	@Nullable
	Object invoke(@Nonnull MethodInvocation invocation) throws Throwable;

}
