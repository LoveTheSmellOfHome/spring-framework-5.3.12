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

package org.springframework.aop.framework;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.aopalliance.intercept.Interceptor;
import org.aopalliance.intercept.MethodInterceptor;

import org.springframework.aop.Advisor;
import org.springframework.aop.IntroductionAdvisor;
import org.springframework.aop.IntroductionAwareMethodMatcher;
import org.springframework.aop.MethodMatcher;
import org.springframework.aop.PointcutAdvisor;
import org.springframework.aop.framework.adapter.AdvisorAdapterRegistry;
import org.springframework.aop.framework.adapter.GlobalAdvisorAdapterRegistry;
import org.springframework.lang.Nullable;

/**
 * A simple but definitive way of working out an advice chain for a Method,
 * given an {@link Advised} object. Always rebuilds each advice chain;
 * caching can be provided by subclasses.
 *
 * @author Juergen Hoeller
 * @author Rod Johnson
 * @author Adrian Colyer
 * @since 2.0.3
 */
// 在给定 Advised 对象的情况下，一种简单但确定的方法为 Method 制定建议链。 始终重建每个建议链； 缓存可以由子类提供。
@SuppressWarnings("serial")
public class DefaultAdvisorChainFactory implements AdvisorChainFactory, Serializable {

	// 筛选符合条件的 Advice
	@Override
	public List<Object> getInterceptorsAndDynamicInterceptionAdvice(
			Advised config, Method method, @Nullable Class<?> targetClass) {

		// This is somewhat tricky... We have to process introductions first,
		// but we need to preserve order in the ultimate list.
		// 这有点棘手......我们必须先处理介绍，但我们需要保持最终列表中的顺序
		//
		// 获取全局单例的 AdvisorAdapterRegistry 注册中心，内部存储 AdvisorAdapter，
		// 可以通过一个 Advisor 获取一个 MethodInterceptor
		//
		// 这里同时解释了Spring 动态代理有三种实现，分别时 JDK 动态代理，CGLIB 字节码提升以及 AspectJ
		// 那么为什么我们看到的 AopProxy API 的实现里边没有看到 AspectJ,我们知道 Spring 对 AspectJ 的
		// 实现是基于 JDK Java 反射，而反射源于 Java 的类，Java 类的来源有两种，一种是通过动态代理字节码提
		// 升形成新的类，这种场景仅适合于以接口为基准的代理，另一种方式是以类为基准的代理即 CGLIB 字节码提升
		// 形成新的类，所以呢它不需要为 AspectJ 单独生成新的代理对象。所以，我们在 Spring 处理中看到只有一个开关
		// ProxyTargetClass 等于 true 或 false,true 为 CGLIB 代理，false 为 JDK 动态代理
		AdvisorAdapterRegistry registry = GlobalAdvisorAdapterRegistry.getInstance();
		Advisor[] advisors = config.getAdvisors(); // 高频操作，空间换时间
		List<Object> interceptorList = new ArrayList<>(advisors.length);
		Class<?> actualClass = (targetClass != null ? targetClass : method.getDeclaringClass());
		Boolean hasIntroductions = null;

		// 循环查找，层层过滤
		for (Advisor advisor : advisors) {
			if (advisor instanceof PointcutAdvisor) { // 筛选切入点满足条件
				// Add it conditionally.
				// 有条件地添加它。PointcutAdvisor 作为容器类里边包含了 Spring AOP 判断点 Pointcut,以及执行动作 Advice
				PointcutAdvisor pointcutAdvisor = (PointcutAdvisor) advisor;
				// config 作为 ProxyFactory,封装了目标对象，根据类过滤器过滤代理源 Class
				if (config.isPreFiltered() || pointcutAdvisor.getPointcut().getClassFilter().matches(actualClass)) {
					// 获取符合条件的方法匹配器
					MethodMatcher mm = pointcutAdvisor.getPointcut().getMethodMatcher();
					boolean match;
					if (mm instanceof IntroductionAwareMethodMatcher) { // 过滤出所有的 IntroductionAwareMethodMatcher
						if (hasIntroductions == null) {
							hasIntroductions = hasMatchingIntroductions(advisors, actualClass);
						}
						match = ((IntroductionAwareMethodMatcher) mm).matches(method, actualClass, hasIntroductions);
					}
					else {
						// 调用 MethodMatcher#match() 方法，可自定义覆盖
						match = mm.matches(method, actualClass);
					}
					if (match) {
						// 获取方法拦截器 registry 是 DefaultAdvisorAdapterRegistry
						MethodInterceptor[] interceptors = registry.getInterceptors(advisor);
						if (mm.isRuntime()) { // 运行时情况
							// Creating a new object instance in the getInterceptors() method
							// isn't a problem as we normally cache created chains.
							// 在 getInterceptors() 方法中创建一个新的对象实例不是问题，因为我们通常会缓存创建的链
							for (MethodInterceptor interceptor : interceptors) {
								// 缓存运行时动态匹配的方法拦截器
								interceptorList.add(new InterceptorAndDynamicMethodMatcher(interceptor, mm));
							}
						}
						else {
							interceptorList.addAll(Arrays.asList(interceptors));
						}
					}
				}
			}
			else if (advisor instanceof IntroductionAdvisor) { // 筛选接口，通过 IntroductionInfo
				IntroductionAdvisor ia = (IntroductionAdvisor) advisor;
				if (config.isPreFiltered() || ia.getClassFilter().matches(actualClass)) {
					Interceptor[] interceptors = registry.getInterceptors(advisor);
					interceptorList.addAll(Arrays.asList(interceptors));
				}
			}
			else {
				Interceptor[] interceptors = registry.getInterceptors(advisor);
				interceptorList.addAll(Arrays.asList(interceptors));
			}
		}

		// 返回符合条件的 Advice 集合
		return interceptorList;
	}

	/**
	 * Determine whether the Advisors contain matching introductions.
	 */
	// 确定顾问是否包含匹配的介绍
	private static boolean hasMatchingIntroductions(Advisor[] advisors, Class<?> actualClass) {
		for (Advisor advisor : advisors) {
			if (advisor instanceof IntroductionAdvisor) {
				IntroductionAdvisor ia = (IntroductionAdvisor) advisor;
				if (ia.getClassFilter().matches(actualClass)) {
					return true;
				}
			}
		}
		return false;
	}

}
