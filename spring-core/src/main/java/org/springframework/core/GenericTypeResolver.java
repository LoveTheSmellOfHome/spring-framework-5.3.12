/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.core;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ConcurrentReferenceHashMap;

/**
 * Helper class for resolving generic types against type variables.
 *
 * <p>Mainly intended for usage within the framework, resolving method
 * parameter types even when they are declared generically.
 *
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Sam Brannen
 * @author Phillip Webb
 * @since 2.5.2
 */
// 用于针对类型变量解析泛型类型的助手类
// <p>主要用于框架内的使用，解析方法参数类型，即使它们是泛型声明的
// Spring 泛型类型辅助类
public final class GenericTypeResolver {

	/** Cache from Class to TypeVariable Map. */
	@SuppressWarnings("rawtypes")
	private static final Map<Class<?>, Map<TypeVariable, Type>> typeVariableCache = new ConcurrentReferenceHashMap<>();


	private GenericTypeResolver() {
	}


	/**
	 * Determine the target type for the given generic parameter type.
	 * @param methodParameter the method parameter specification
	 * @param implementationClass the class to resolve type variables against
	 * @return the corresponding generic parameter or return type
	 * @deprecated since 5.2 in favor of {@code methodParameter.withContainingClass(implementationClass).getParameterType()}
	 */
	@Deprecated
	public static Class<?> resolveParameterType(MethodParameter methodParameter, Class<?> implementationClass) {
		Assert.notNull(methodParameter, "MethodParameter must not be null");
		Assert.notNull(implementationClass, "Class must not be null");
		methodParameter.setContainingClass(implementationClass);
		return methodParameter.getParameterType();
	}

	/**
	 * Determine the target type for the generic return type of the given method,
	 * where formal type variables are declared on the given class.
	 * @param method the method to introspect
	 * @param clazz the class to resolve type variables against
	 * @return the corresponding generic parameter or return type
	 */
	// 确定给定方法的泛型返回类型的目标类型，其中在给定类上声明了正式类型变量
	// @param method 内省的方法
	// @param clazz 用于解析类型变量的类
	// @return 对应的泛型参数或返回类型
	// 处理 类型[Type]相关的方法
	public static Class<?> resolveReturnType(Method method, Class<?> clazz) {
		Assert.notNull(method, "Method must not be null");
		Assert.notNull(clazz, "Class must not be null");
		return ResolvableType.forMethodReturnType(method, clazz).resolve(method.getReturnType());
	}

	/**
	 * Resolve the single type argument of the given generic interface against the given
	 * target method which is assumed to return the given interface or an implementation
	 * of it.
	 * @param method the target method to check the return type of
	 * @param genericIfc the generic interface or superclass to resolve the type argument from
	 * @return the resolved parameter type of the method return type, or {@code null}
	 * if not resolvable or if the single argument is of type {@link WildcardType}.
	 */
	// 针对给定的目标方法解析给定泛型接口的单一类型参数，该方法假定返回给定接口或其实现
	// 处理泛型参数类型（ParameterizedType）相关方法 如：Collection<E>
	// @param method 检查返回类型的目标方法
	// @param genericIfc 解析类型参数 ParameterizedType(如：List<Object>) 的泛型接口或超类，即泛型所在的类型 List，
	//                   genericIfc 就是 List.(在比如 String 本身不带泛型，但是它继承 Comparable<String> 带有泛型，
	//                   因此 String 的泛型接口或者父类 genericIfc 就是 Comparable。class)
	// @return 方法返回类型的已解析参数类型（如：List<Object> 中的泛型Object），
	// 		   如果不可解析或单个参数为 {@link WildcardType} 类型，则为 {@code null}。
	@Nullable
	public static Class<?> resolveReturnTypeArgument(Method method, Class<?> genericIfc) {
		Assert.notNull(method, "Method must not be null");
		// 获取指定 method，指定泛型接口的泛型参数类型(返回值类型带泛型)
		// resolvableType：java.lang.Comparable<java.lang.String>
		ResolvableType resolvableType = ResolvableType.forMethodReturnType(method).as(genericIfc);
		// 是否不包含泛型 || 泛型参数类型是泛型通配类型 resolvableType.getType()：java.lang.Comparable<java.lang.String>
		if (!resolvableType.hasGenerics() || resolvableType.getType() instanceof WildcardType) {
			return null;
		}
		return getSingleGeneric(resolvableType);
	}

	/**
	 * Resolve the single type argument of the given generic interface against
	 * the given target class which is assumed to implement the generic interface
	 * and possibly declare a concrete type for its type variable.
	 * @param clazz the target class to check against
	 * @param genericIfc the generic interface or superclass to resolve the type argument from
	 * @return the resolved type of the argument, or {@code null} if not resolvable
	 */
	// 针对给定的目标类解析给定泛型接口的单个类型参数，该类假定实现泛型接口并可能为其类型变量声明具体类型。
	// 处理泛型参数类型（ParameterizedType）相关方法，如：Collection<E>
	@Nullable
	public static Class<?> resolveTypeArgument(Class<?> clazz, Class<?> genericIfc) {
		ResolvableType resolvableType = ResolvableType.forClass(clazz).as(genericIfc);
		if (!resolvableType.hasGenerics()) {
			return null;
		}
		return getSingleGeneric(resolvableType);
	}

	@Nullable
	private static Class<?> getSingleGeneric(ResolvableType resolvableType) {
		Assert.isTrue(resolvableType.getGenerics().length == 1,
				() -> "Expected 1 type argument on generic interface [" + resolvableType +
				"] but found " + resolvableType.getGenerics().length);
		return resolvableType.getGeneric().resolve();
	}


	/**
	 * Resolve the type arguments of the given generic interface against the given
	 * target class which is assumed to implement the generic interface and possibly
	 * declare concrete types for its type variables.
	 * @param clazz the target class to check against
	 * @param genericIfc the generic interface or superclass to resolve the type argument from
	 * @return the resolved type of each argument, with the array size matching the
	 * number of actual type arguments, or {@code null} if not resolvable
	 */
	// 针对给定的目标类解析给定泛型接口的类型参数，该类假定实现泛型接口并可能为其类型变量声明具体类型。
	// 处理泛型参数类型（ParameterizedType）相关方法，如：Collection<E>
	@Nullable
	public static Class<?>[] resolveTypeArguments(Class<?> clazz, Class<?> genericIfc) {
		ResolvableType type = ResolvableType.forClass(clazz).as(genericIfc);
		if (!type.hasGenerics() || type.isEntirelyUnresolvable()) {
			return null;
		}
		return type.resolveGenerics(Object.class);
	}

	/**
	 * Resolve the given generic type against the given context class,
	 * substituting type variables as far as possible.
	 * @param genericType the (potentially) generic type
	 * @param contextClass a context class for the target type, for example a class
	 * in which the target type appears in a method signature (can be {@code null})
	 * @return the resolved type (possibly the given generic type as-is)
	 * @since 5.0
	 */
	// 针对给定的上下文类解析给定的泛型类型，尽可能替换类型变量
	// @param genericType（可能）泛型类型
	// @param contextClass 目标类型的上下文类，例如目标类型出现在方法签名中的类（可以是{@code null}）
	// @return 解析的类型（可能是给定的泛型类型
	// 处理类型[Type] 相关方法
	public static Type resolveType(Type genericType, @Nullable Class<?> contextClass) {
		if (contextClass != null) {
			if (genericType instanceof TypeVariable) {
				ResolvableType resolvedTypeVariable = resolveVariable(
						(TypeVariable<?>) genericType, ResolvableType.forClass(contextClass));
				if (resolvedTypeVariable != ResolvableType.NONE) {
					Class<?> resolved = resolvedTypeVariable.resolve();
					if (resolved != null) {
						return resolved;
					}
				}
			}
			else if (genericType instanceof ParameterizedType) {
				ResolvableType resolvedType = ResolvableType.forType(genericType);
				if (resolvedType.hasUnresolvableGenerics()) {
					ParameterizedType parameterizedType = (ParameterizedType) genericType;
					Class<?>[] generics = new Class<?>[parameterizedType.getActualTypeArguments().length];
					Type[] typeArguments = parameterizedType.getActualTypeArguments();
					ResolvableType contextType = ResolvableType.forClass(contextClass);
					for (int i = 0; i < typeArguments.length; i++) {
						Type typeArgument = typeArguments[i];
						if (typeArgument instanceof TypeVariable) {
							ResolvableType resolvedTypeArgument = resolveVariable(
									(TypeVariable<?>) typeArgument, contextType);
							if (resolvedTypeArgument != ResolvableType.NONE) {
								generics[i] = resolvedTypeArgument.resolve();
							}
							else {
								generics[i] = ResolvableType.forType(typeArgument).resolve();
							}
						}
						else {
							generics[i] = ResolvableType.forType(typeArgument).resolve();
						}
					}
					Class<?> rawClass = resolvedType.getRawClass();
					if (rawClass != null) {
						return ResolvableType.forClassWithGenerics(rawClass, generics).getType();
					}
				}
			}
		}
		return genericType;
	}

	private static ResolvableType resolveVariable(TypeVariable<?> typeVariable, ResolvableType contextType) {
		ResolvableType resolvedType;
		if (contextType.hasGenerics()) {
			resolvedType = ResolvableType.forType(typeVariable, contextType);
			if (resolvedType.resolve() != null) {
				return resolvedType;
			}
		}

		ResolvableType superType = contextType.getSuperType();
		if (superType != ResolvableType.NONE) {
			resolvedType = resolveVariable(typeVariable, superType);
			if (resolvedType.resolve() != null) {
				return resolvedType;
			}
		}
		for (ResolvableType ifc : contextType.getInterfaces()) {
			resolvedType = resolveVariable(typeVariable, ifc);
			if (resolvedType.resolve() != null) {
				return resolvedType;
			}
		}
		return ResolvableType.NONE;
	}

	/**
	 * Resolve the specified generic type against the given TypeVariable map.
	 * <p>Used by Spring Data.
	 * @param genericType the generic type to resolve
	 * @param map the TypeVariable Map to resolved against
	 * @return the type if it resolves to a Class, or {@code Object.class} otherwise
	 */
	@SuppressWarnings("rawtypes")
	public static Class<?> resolveType(Type genericType, Map<TypeVariable, Type> map) {
		return ResolvableType.forType(genericType, new TypeVariableMapVariableResolver(map)).toClass();
	}

	/**
	 * Build a mapping of {@link TypeVariable#getName TypeVariable names} to
	 * {@link Class concrete classes} for the specified {@link Class}.
	 * Searches all super types, enclosing types and interfaces.
	 * @see #resolveType(Type, Map)
	 */
	// 为指定的 {@link Class} 构建 {@link TypeVariablegetName TypeVariable names} 到 {@link Class 具体类}的映射。
	// 搜索所有超类型、封闭类型和接口。
	// 处理泛型类型变量（TypeVariable）相关方法 E
	@SuppressWarnings("rawtypes")
	public static Map<TypeVariable, Type> getTypeVariableMap(Class<?> clazz) {
		Map<TypeVariable, Type> typeVariableMap = typeVariableCache.get(clazz);
		if (typeVariableMap == null) {
			typeVariableMap = new HashMap<>();
			buildTypeVariableMap(ResolvableType.forClass(clazz), typeVariableMap);
			typeVariableCache.put(clazz, Collections.unmodifiableMap(typeVariableMap));
		}
		return typeVariableMap;
	}

	@SuppressWarnings("rawtypes")
	private static void buildTypeVariableMap(ResolvableType type, Map<TypeVariable, Type> typeVariableMap) {
		if (type != ResolvableType.NONE) {
			Class<?> resolved = type.resolve();
			if (resolved != null && type.getType() instanceof ParameterizedType) {
				TypeVariable<?>[] variables = resolved.getTypeParameters();
				for (int i = 0; i < variables.length; i++) {
					ResolvableType generic = type.getGeneric(i);
					while (generic.getType() instanceof TypeVariable<?>) {
						generic = generic.resolveType();
					}
					if (generic != ResolvableType.NONE) {
						typeVariableMap.put(variables[i], generic.getType());
					}
				}
			}
			buildTypeVariableMap(type.getSuperType(), typeVariableMap);
			for (ResolvableType interfaceType : type.getInterfaces()) {
				buildTypeVariableMap(interfaceType, typeVariableMap);
			}
			if (resolved != null && resolved.isMemberClass()) {
				buildTypeVariableMap(ResolvableType.forClass(resolved.getEnclosingClass()), typeVariableMap);
			}
		}
	}


	@SuppressWarnings({"serial", "rawtypes"})
	private static class TypeVariableMapVariableResolver implements ResolvableType.VariableResolver {

		private final Map<TypeVariable, Type> typeVariableMap;

		public TypeVariableMapVariableResolver(Map<TypeVariable, Type> typeVariableMap) {
			this.typeVariableMap = typeVariableMap;
		}

		@Override
		@Nullable
		public ResolvableType resolveVariable(TypeVariable<?> variable) {
			Type type = this.typeVariableMap.get(variable);
			return (type != null ? ResolvableType.forType(type) : null);
		}

		@Override
		public Object getSource() {
			return this.typeVariableMap;
		}
	}

}
