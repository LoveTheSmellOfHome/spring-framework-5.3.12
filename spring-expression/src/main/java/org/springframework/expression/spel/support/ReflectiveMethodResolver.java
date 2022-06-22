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

package org.springframework.expression.spel.support;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.core.BridgeMethodResolver;
import org.springframework.core.MethodParameter;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.MethodExecutor;
import org.springframework.expression.MethodFilter;
import org.springframework.expression.MethodResolver;
import org.springframework.expression.TypeConverter;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.SpelMessage;
import org.springframework.lang.Nullable;

/**
 * Reflection-based {@link MethodResolver} used by default in {@link StandardEvaluationContext}
 * unless explicit method resolvers have been specified.
 *
 * @author Andy Clement
 * @author Juergen Hoeller
 * @author Chris Beams
 * @since 3.0
 * @see StandardEvaluationContext#addMethodResolver(MethodResolver)
 */
// 基于反射的 {@link MethodResolver}。除非已指定显式方法解析器，否则默认使用 {@link StandardEvaluationContext}
public class ReflectiveMethodResolver implements MethodResolver {

	// Using distance will ensure a more accurate match is discovered,
	// more closely following the Java rules.
	private final boolean useDistance;

	@Nullable
	private Map<Class<?>, MethodFilter> filters;


	public ReflectiveMethodResolver() {
		this.useDistance = true;
	}

	/**
	 * This constructor allows the ReflectiveMethodResolver to be configured such that it
	 * will use a distance computation to check which is the better of two close matches
	 * (when there are multiple matches). Using the distance computation is intended to
	 * ensure matches are more closely representative of what a Java compiler would do
	 * when taking into account boxing/unboxing and whether the method candidates are
	 * declared to handle a supertype of the type (of the argument) being passed in.
	 * @param useDistance {@code true} if distance computation should be used when
	 * calculating matches; {@code false} otherwise
	 */
	// 此构造函数允许配置 ReflectiveMethodResolver，以便它将使用距离计算来检查两个接近匹配中哪个更好（当有多个匹配时）。
	// 使用距离计算旨在确保匹配更能代表 Java 编译器在考虑装箱/拆箱以及是否声明候选方法来处理传入的（参数的）类型的超类型时所做的事情。
	// 形参：useDistance – 如果在计算匹配时应使用距离计算，则为true ； 否则为false
	public ReflectiveMethodResolver(boolean useDistance) {
		this.useDistance = useDistance;
	}


	/**
	 * Register a filter for methods on the given type.
	 * @param type the type to filter on
	 * @param filter the corresponding method filter,
	 * or {@code null} to clear any filter for the given type
	 */
	// 为给定类型的方法注册过滤器。
	// 形参：type – 要过滤的类型
	// filter – 相应的方法过滤器，或null以清除给定类型的任何过滤器
	public void registerMethodFilter(Class<?> type, @Nullable MethodFilter filter) {
		if (this.filters == null) {
			this.filters = new HashMap<>();
		}
		if (filter != null) {
			this.filters.put(type, filter);
		}
		else {
			this.filters.remove(type);
		}
	}

	/**
	 * Locate a method on a type. There are three kinds of match that might occur:
	 * <ol>
	 * <li>an exact match where the types of the arguments match the types of the constructor
	 * <li>an in-exact match where the types we are looking for are subtypes of those defined on the constructor
	 * <li>a match where we are able to convert the arguments into those expected by the constructor,
	 * according to the registered type converter
	 * </ol>
	 */
	// 定位一个类型的方法。 可能发生三种匹配：
	// 1.参数类型与构造函数类型匹配的精确匹配
	// 2.不完全匹配，其中我们要查找的类型是构造函数中定义的类型的子类型
	// 3.根据注册的类型转换器，我们能够将参数转换为构造函数期望的参数的匹配项
	@Override
	@Nullable
	public MethodExecutor resolve(EvaluationContext context, Object targetObject, String name,
			List<TypeDescriptor> argumentTypes) throws AccessException {

		try {
			TypeConverter typeConverter = context.getTypeConverter();
			Class<?> type = (targetObject instanceof Class ? (Class<?>) targetObject : targetObject.getClass());
			ArrayList<Method> methods = new ArrayList<>(getMethods(type, targetObject));

			// If a filter is registered for this type, call it
			// 如果为此类型注册了过滤器，则调用它
			MethodFilter filter = (this.filters != null ? this.filters.get(type) : null);
			if (filter != null) {
				List<Method> filtered = filter.filter(methods);
				methods = (filtered instanceof ArrayList ? (ArrayList<Method>) filtered : new ArrayList<>(filtered));
			}

			// Sort methods into a sensible order
			// 将方法排序为合理的顺序
			if (methods.size() > 1) {
				methods.sort((m1, m2) -> {
					int m1pl = m1.getParameterCount();
					int m2pl = m2.getParameterCount();
					// vararg methods go last
					// vararg 方法放在最后
					if (m1pl == m2pl) {
						if (!m1.isVarArgs() && m2.isVarArgs()) {
							return -1;
						}
						else if (m1.isVarArgs() && !m2.isVarArgs()) {
							return 1;
						}
						else {
							return 0;
						}
					}
					return Integer.compare(m1pl, m2pl);
				});
			}

			// Resolve any bridge methods
			// 解析任何桥接方法
			for (int i = 0; i < methods.size(); i++) {
				methods.set(i, BridgeMethodResolver.findBridgedMethod(methods.get(i)));
			}

			// Remove duplicate methods (possible due to resolved bridge methods)
			// 删除重复的方法（可能由于已解决的桥接方法）
			Set<Method> methodsToIterate = new LinkedHashSet<>(methods);

			Method closeMatch = null;
			int closeMatchDistance = Integer.MAX_VALUE;
			Method matchRequiringConversion = null;
			boolean multipleOptions = false;

			for (Method method : methodsToIterate) {
				if (method.getName().equals(name)) {
					int paramCount = method.getParameterCount();
					List<TypeDescriptor> paramDescriptors = new ArrayList<>(paramCount);
					for (int i = 0; i < paramCount; i++) {
						paramDescriptors.add(new TypeDescriptor(new MethodParameter(method, i)));
					}
					ReflectionHelper.ArgumentsMatchInfo matchInfo = null;
					if (method.isVarArgs() && argumentTypes.size() >= (paramCount - 1)) {
						// *sigh* complicated
						matchInfo = ReflectionHelper.compareArgumentsVarargs(paramDescriptors, argumentTypes, typeConverter);
					}
					else if (paramCount == argumentTypes.size()) {
						// Name and parameter number match, check the arguments
						matchInfo = ReflectionHelper.compareArguments(paramDescriptors, argumentTypes, typeConverter);
					}
					if (matchInfo != null) {
						if (matchInfo.isExactMatch()) {
							return new ReflectiveMethodExecutor(method);
						}
						else if (matchInfo.isCloseMatch()) {
							if (this.useDistance) {
								int matchDistance = ReflectionHelper.getTypeDifferenceWeight(paramDescriptors, argumentTypes);
								if (closeMatch == null || matchDistance < closeMatchDistance) {
									// This is a better match...
									closeMatch = method;
									closeMatchDistance = matchDistance;
								}
							}
							else {
								// Take this as a close match if there isn't one already
								if (closeMatch == null) {
									closeMatch = method;
								}
							}
						}
						else if (matchInfo.isMatchRequiringConversion()) {
							if (matchRequiringConversion != null) {
								multipleOptions = true;
							}
							matchRequiringConversion = method;
						}
					}
				}
			}
			if (closeMatch != null) {
				return new ReflectiveMethodExecutor(closeMatch);
			}
			else if (matchRequiringConversion != null) {
				if (multipleOptions) {
					throw new SpelEvaluationException(SpelMessage.MULTIPLE_POSSIBLE_METHODS, name);
				}
				return new ReflectiveMethodExecutor(matchRequiringConversion);
			}
			else {
				return null;
			}
		}
		catch (EvaluationException ex) {
			throw new AccessException("Failed to resolve method", ex);
		}
	}

	private Set<Method> getMethods(Class<?> type, Object targetObject) {
		if (targetObject instanceof Class) {
			Set<Method> result = new LinkedHashSet<>();
			// Add these so that static methods are invocable on the type: e.g. Float.valueOf(..)
			Method[] methods = getMethods(type);
			for (Method method : methods) {
				if (Modifier.isStatic(method.getModifiers())) {
					result.add(method);
				}
			}
			// Also expose methods from java.lang.Class itself
			Collections.addAll(result, getMethods(Class.class));
			return result;
		}
		else if (Proxy.isProxyClass(type)) {
			Set<Method> result = new LinkedHashSet<>();
			// Expose interface methods (not proxy-declared overrides) for proper vararg introspection
			for (Class<?> ifc : type.getInterfaces()) {
				Method[] methods = getMethods(ifc);
				for (Method method : methods) {
					if (isCandidateForInvocation(method, type)) {
						result.add(method);
					}
				}
			}
			return result;
		}
		else {
			Set<Method> result = new LinkedHashSet<>();
			Method[] methods = getMethods(type);
			for (Method method : methods) {
				if (isCandidateForInvocation(method, type)) {
					result.add(method);
				}
			}
			return result;
		}
	}

	/**
	 * Return the set of methods for this type. The default implementation returns the
	 * result of {@link Class#getMethods()} for the given {@code type}, but subclasses
	 * may override in order to alter the results, e.g. specifying static methods
	 * declared elsewhere.
	 * @param type the class for which to return the methods
	 * @since 3.1.1
	 */
	// 返回此类型的方法集。 默认实现为给定type返回Class.getMethods()的结果，但子类可能会覆盖以更改结果，例如指定在别处声明的静态方法。
	// 形参：type – 返回方法的类
	protected Method[] getMethods(Class<?> type) {
		return type.getMethods();
	}

	/**
	 * Determine whether the given {@code Method} is a candidate for method resolution
	 * on an instance of the given target class.
	 * <p>The default implementation considers any method as a candidate, even for
	 * static methods sand non-user-declared methods on the {@link Object} base class.
	 * @param method the Method to evaluate
	 * @param targetClass the concrete target class that is being introspected
	 * @since 4.3.15
	 */
	// 确定给定的 Method 是否是给定目标类实例的方法解析的候选者。
	// 默认实现将任何方法视为候选方法，即使对于Object基类上的静态方法和非用户声明的方法也是如此。
	protected boolean isCandidateForInvocation(Method method, Class<?> targetClass) {
		return true;
	}

}
