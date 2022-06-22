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

package org.aopalliance.intercept;

import javax.annotation.Nonnull;

/**
 * Intercepts the construction of a new object.
 *
 * <p>The user should implement the {@link
 * #construct(ConstructorInvocation)} method to modify the original
 * behavior. E.g. the following class implements a singleton
 * interceptor (allows only one unique instance for the intercepted
 * class):
 *
 * <pre class=code>
 * class DebuggingInterceptor implements ConstructorInterceptor {
 *   Object instance=null;
 *
 *   Object construct(ConstructorInvocation i) throws Throwable {
 *     if(instance==null) {
 *       return instance=i.proceed();
 *     } else {
 *       throw new Exception("singleton does not allow multiple instance");
 *     }
 *   }
 * }
 * </pre>
 *
 * @author Rod Johnson
 */
// 拦截新对象的构造。
// 用户应该实现construct(ConstructorInvocation)方法来修改原始行为。 例如，下面的类实现了一个
// 单例拦截器（只允许被拦截的类有一个唯一的实例）：
//   class DebuggingInterceptor implements ConstructorInterceptor {
//     Object instance=null;
//
//     Object construct(ConstructorInvocation i) throws Throwable {
//       if(instance==null) {
//         return instance=i.proceed();
//       } else {
//         throw new Exception("singleton does not allow multiple instance");
//       }
//     }
//   }
public interface ConstructorInterceptor extends Interceptor  {

	/**
	 * Implement this method to perform extra treatments before and
	 * after the construction of a new object. Polite implementations
	 * would certainly like to invoke {@link Joinpoint#proceed()}.
	 * @param invocation the construction joinpoint
	 * @return the newly created object, which is also the result of
	 * the call to {@link Joinpoint#proceed()}; might be replaced by
	 * the interceptor
	 * @throws Throwable if the interceptors or the target object
	 * throws an exception
	 */
	// 实现此方法以在构建新对象之前和之后执行额外的处理。 礼貌的实现当然想调用Joinpoint.proceed() 。
	// 形参：
	//			invocation - 构造连接点
	// 返回值：
	//			新创建的对象，也是调用Joinpoint.proceed() ； 可能会被拦截器取代
	// 抛出：
	//			Throwable – 如果拦截器或目标对象抛出异常
	@Nonnull
	Object construct(ConstructorInvocation invocation) throws Throwable;

}
