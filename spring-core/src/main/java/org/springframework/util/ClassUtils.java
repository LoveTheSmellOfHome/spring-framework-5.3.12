/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.util;

import java.beans.Introspector;
import java.io.Closeable;
import java.io.Externalizable;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;

import org.springframework.lang.Nullable;

/**
 * Miscellaneous {@code java.lang.Class} utility methods.
 * Mainly for internal use within the framework.
 *
 * @author Juergen Hoeller
 * @author Keith Donald
 * @author Rob Harrop
 * @author Sam Brannen
 * @since 1.1
 * @see TypeUtils
 * @see ReflectionUtils
 */
// 各种各样的 {@code java.lang.Class} 实用程序方法。主要供框架内部使用
public abstract class ClassUtils {

	/** Suffix for array class names: {@code "[]"}. */
	// 数组类名的后缀：{@code "[]"}
	public static final String ARRAY_SUFFIX = "[]";

	/** Prefix for internal array class names: {@code "["}. */
	// 内部数组类名的前缀：{@code "["}
	private static final String INTERNAL_ARRAY_PREFIX = "[";

	/** Prefix for internal non-primitive array class names: {@code "[L"}. */
	// 内部非原始数组类名的前缀：{@code "[L"}
	private static final String NON_PRIMITIVE_ARRAY_PREFIX = "[L";

	/** A reusable empty class array constant. */
	// 一个可重用的空类数组常量
	private static final Class<?>[] EMPTY_CLASS_ARRAY = {};

	/** The package separator character: {@code '.'}. */
	// 包分隔符：{@code '.'}
	private static final char PACKAGE_SEPARATOR = '.';

	/** The path separator character: {@code '/'}. */
	// 路径分隔符：{@code '/'}。
	private static final char PATH_SEPARATOR = '/';

	/** The nested class separator character: {@code '$'}. */
	// 嵌套的类分隔符：{@code '$'}
	private static final char NESTED_CLASS_SEPARATOR = '$';

	/** The CGLIB class separator: {@code "$$"}. */
	// CGLIB 类分隔符：{@code "$$"}
	public static final String CGLIB_CLASS_SEPARATOR = "$$";

	/** The ".class" file suffix. */
	// “.class”文件后缀
	public static final String CLASS_FILE_SUFFIX = ".class";


	/**
	 * Map with primitive wrapper type as key and corresponding primitive
	 * type as value, for example: Integer.class -> int.class.
	 */
	// Map 以包装类型为键，对应的原始类型为值，例如：Integer.class -> int.class
	private static final Map<Class<?>, Class<?>> primitiveWrapperTypeMap = new IdentityHashMap<>(9);

	/**
	 * Map with primitive type as key and corresponding wrapper
	 * type as value, for example: int.class -> Integer.class.
	 */
	// Map 以原始类型为键，对应的包装类型为值，例如：int.class -> Integer.class
	private static final Map<Class<?>, Class<?>> primitiveTypeToWrapperMap = new IdentityHashMap<>(9);

	/**
	 * Map with primitive type name as key and corresponding primitive
	 * type as value, for example: "int" -> "int.class".
	 */
	// 以原始类型名称为键，对应原始类型为值的映射，例如：“int”->“int.class”。
	private static final Map<String, Class<?>> primitiveTypeNameMap = new HashMap<>(32);

	/**
	 * Map with common Java language class name as key and corresponding Class as value.
	 * Primarily for efficient deserialization of remote invocations.
	 */
	// 以通用Java语言类名为键，对应的类为值的映射。主要用于远程调用的高效反序列化
	private static final Map<String, Class<?>> commonClassCache = new HashMap<>(64);

	/**
	 * Common Java language interfaces which are supposed to be ignored
	 * when searching for 'primary' user-level interfaces.
	 */
	// 在搜索“主要”用户级接口时应该忽略的通用 Java 语言接口
	private static final Set<Class<?>> javaLanguageInterfaces;

	/**
	 * Cache for equivalent methods on an interface implemented by the declaring class.
	 */
	// 在声明类实现的接口上缓存等效方法
	private static final Map<Method, Method> interfaceMethodCache = new ConcurrentReferenceHashMap<>(256);


	static {
		primitiveWrapperTypeMap.put(Boolean.class, boolean.class);
		primitiveWrapperTypeMap.put(Byte.class, byte.class);
		primitiveWrapperTypeMap.put(Character.class, char.class);
		primitiveWrapperTypeMap.put(Double.class, double.class);
		primitiveWrapperTypeMap.put(Float.class, float.class);
		primitiveWrapperTypeMap.put(Integer.class, int.class);
		primitiveWrapperTypeMap.put(Long.class, long.class);
		primitiveWrapperTypeMap.put(Short.class, short.class);
		primitiveWrapperTypeMap.put(Void.class, void.class);

		// Map entry iteration is less expensive to initialize than forEach with lambdas
		// 与使用 lambda 的 forEach 相比，Map 条目迭代的初始化成本更低
		for (Map.Entry<Class<?>, Class<?>> entry : primitiveWrapperTypeMap.entrySet()) {
			primitiveTypeToWrapperMap.put(entry.getValue(), entry.getKey());
			registerCommonClasses(entry.getKey());
		}

		Set<Class<?>> primitiveTypes = new HashSet<>(32);
		primitiveTypes.addAll(primitiveWrapperTypeMap.values());
		Collections.addAll(primitiveTypes, boolean[].class, byte[].class, char[].class,
				double[].class, float[].class, int[].class, long[].class, short[].class);
		for (Class<?> primitiveType : primitiveTypes) {
			primitiveTypeNameMap.put(primitiveType.getName(), primitiveType);
		}

		registerCommonClasses(Boolean[].class, Byte[].class, Character[].class, Double[].class,
				Float[].class, Integer[].class, Long[].class, Short[].class);
		registerCommonClasses(Number.class, Number[].class, String.class, String[].class,
				Class.class, Class[].class, Object.class, Object[].class);
		registerCommonClasses(Throwable.class, Exception.class, RuntimeException.class,
				Error.class, StackTraceElement.class, StackTraceElement[].class);
		registerCommonClasses(Enum.class, Iterable.class, Iterator.class, Enumeration.class,
				Collection.class, List.class, Set.class, Map.class, Map.Entry.class, Optional.class);

		Class<?>[] javaLanguageInterfaceArray = {Serializable.class, Externalizable.class,
				Closeable.class, AutoCloseable.class, Cloneable.class, Comparable.class};
		registerCommonClasses(javaLanguageInterfaceArray);
		javaLanguageInterfaces = new HashSet<>(Arrays.asList(javaLanguageInterfaceArray));
	}


	/**
	 * Register the given common classes with the ClassUtils cache.
	 */
	// 使用 ClassUtils 缓存注册给定的公共类。
	private static void registerCommonClasses(Class<?>... commonClasses) {
		for (Class<?> clazz : commonClasses) {
			commonClassCache.put(clazz.getName(), clazz);
		}
	}

	/**
	 * Return the default ClassLoader to use: typically the thread context
	 * ClassLoader, if available; the ClassLoader that loaded the ClassUtils
	 * class will be used as fallback.
	 * <p>Call this method if you intend to use the thread context ClassLoader
	 * in a scenario where you clearly prefer a non-null ClassLoader reference:
	 * for example, for class path resource loading (but not necessarily for
	 * {@code Class.forName}, which accepts a {@code null} ClassLoader
	 * reference as well).
	 * @return the default ClassLoader (only {@code null} if even the system
	 * ClassLoader isn't accessible)
	 * @see Thread#getContextClassLoader()
	 * @see ClassLoader#getSystemClassLoader()
	 */
	// 返回要使用的默认 ClassLoader：通常是线程上下文 ClassLoader，如果可用；加载 ClassUtils 类的 ClassLoader 将用作后备。
	// <p>如果您打算在明显更喜欢非空 ClassLoader 引用的场景中使用线程上下文 ClassLoader，请调用此方法：例如，
	// 用于类路径资源加载（但不一定用于 {@code Class.forName}，它也接受 {@code null} ClassLoader 引用）。
	// @return 默认的类加载器（如果系统类加载器不可访问，则仅 {@code null}）
	@Nullable
	public static ClassLoader getDefaultClassLoader() {
		ClassLoader cl = null;
		try {
			// 获取当前线程使用的 ClassLoader
			cl = Thread.currentThread().getContextClassLoader();
		}
		catch (Throwable ex) {
			// Cannot access thread context ClassLoader - falling back...
			// 无法访问线程上下文 ClassLoader - 回退...
		}
		if (cl == null) {
			// No thread context class loader -> use class loader of this class.
			// 无线程上下文类加载器 -> 使用此类的类加载器。
			cl = ClassUtils.class.getClassLoader();
			if (cl == null) {
				// getClassLoader() returning null indicates the bootstrap ClassLoader
				// getClassLoader() 返回 null 表示引导类加载器
				try {
					// 查找系统 ClassLoader
					cl = ClassLoader.getSystemClassLoader();
				}
				catch (Throwable ex) {
					// Cannot access system ClassLoader - oh well, maybe the caller can live with null...
					// 无法访问系统 ClassLoader - 哦，好吧，也许调用者可以接受 null.
				}
			}
		}
		return cl;
	}

	/**
	 * Override the thread context ClassLoader with the environment's bean ClassLoader
	 * if necessary, i.e. if the bean ClassLoader is not equivalent to the thread
	 * context ClassLoader already.
	 * @param classLoaderToUse the actual ClassLoader to use for the thread context
	 * @return the original thread context ClassLoader, or {@code null} if not overridden
	 */
	// 如有必要，使用环境的 bean ClassLoader 覆盖线程上下文 ClassLoader，即，如果 bean ClassLoader 已经不等同于线程上下文 ClassLoader。
	// 参形：
	//			classLoaderToUse – 用于线程上下文的实际 ClassLoader
	// 返回值：
	//			原始线程上下文 ClassLoader，如果没有被覆盖，则返回null
	@Nullable
	public static ClassLoader overrideThreadContextClassLoader(@Nullable ClassLoader classLoaderToUse) {
		Thread currentThread = Thread.currentThread();
		ClassLoader threadContextClassLoader = currentThread.getContextClassLoader();
		if (classLoaderToUse != null && !classLoaderToUse.equals(threadContextClassLoader)) {
			currentThread.setContextClassLoader(classLoaderToUse);
			return threadContextClassLoader;
		}
		else {
			return null;
		}
	}

	/**
	 * Replacement for {@code Class.forName()} that also returns Class instances
	 * for primitives (e.g. "int") and array class names (e.g. "String[]").
	 * Furthermore, it is also capable of resolving nested class names in Java source
	 * style (e.g. "java.lang.Thread.State" instead of "java.lang.Thread$State").
	 * @param name the name of the Class
	 * @param classLoader the class loader to use
	 * (may be {@code null}, which indicates the default class loader)
	 * @return a class instance for the supplied name
	 * @throws ClassNotFoundException if the class was not found
	 * @throws LinkageError if the class file could not be loaded
	 * @see Class#forName(String, boolean, ClassLoader)
	 */
	// 替换 {@code Class.forName()} ，它还返回基本类型数据（例如“int”）和数组类型例如“String[]”）的类实例。
	// 此外，它还能够解析 Java 源代码样式中的嵌套类名（例如“java.lang.Thread.State”而不是“java.lang.Thread$State”）
	public static Class<?> forName(String name, @Nullable ClassLoader classLoader)
			throws ClassNotFoundException, LinkageError {

		Assert.notNull(name, "Name must not be null");
		Class<?> clazz = resolvePrimitiveClassName(name);
		if (clazz == null) {
			clazz = commonClassCache.get(name);
		}
		if (clazz != null) {
			return clazz;
		}

		// "java.lang.String[]" style arrays
		if (name.endsWith(ARRAY_SUFFIX)) {
			String elementClassName = name.substring(0, name.length() - ARRAY_SUFFIX.length());
			Class<?> elementClass = forName(elementClassName, classLoader);
			return Array.newInstance(elementClass, 0).getClass();
		}

		// "[Ljava.lang.String;" style arrays
		if (name.startsWith(NON_PRIMITIVE_ARRAY_PREFIX) && name.endsWith(";")) {
			String elementName = name.substring(NON_PRIMITIVE_ARRAY_PREFIX.length(), name.length() - 1);
			Class<?> elementClass = forName(elementName, classLoader);
			return Array.newInstance(elementClass, 0).getClass();
		}

		// "[[I" or "[[Ljava.lang.String;" style arrays
		if (name.startsWith(INTERNAL_ARRAY_PREFIX)) {
			String elementName = name.substring(INTERNAL_ARRAY_PREFIX.length());
			Class<?> elementClass = forName(elementName, classLoader);
			return Array.newInstance(elementClass, 0).getClass();
		}

		ClassLoader clToUse = classLoader;
		if (clToUse == null) {
			clToUse = getDefaultClassLoader();
		}
		try {
			return Class.forName(name, false, clToUse);
		}
		catch (ClassNotFoundException ex) {
			int lastDotIndex = name.lastIndexOf(PACKAGE_SEPARATOR);
			if (lastDotIndex != -1) {
				String nestedClassName =
						name.substring(0, lastDotIndex) + NESTED_CLASS_SEPARATOR + name.substring(lastDotIndex + 1);
				try {
					return Class.forName(nestedClassName, false, clToUse);
				}
				catch (ClassNotFoundException ex2) {
					// Swallow - let original exception get through
				}
			}
			throw ex;
		}
	}

	/**
	 * Resolve the given class name into a Class instance. Supports
	 * primitives (like "int") and array class names (like "String[]").
	 * <p>This is effectively equivalent to the {@code forName}
	 * method with the same arguments, with the only difference being
	 * the exceptions thrown in case of class loading failure.
	 * @param className the name of the Class
	 * @param classLoader the class loader to use
	 * (may be {@code null}, which indicates the default class loader)
	 * @return a class instance for the supplied name
	 * @throws IllegalArgumentException if the class name was not resolvable
	 * (that is, the class could not be found or the class file could not be loaded)
	 * @throws IllegalStateException if the corresponding class is resolvable but
	 * there was a readability mismatch in the inheritance hierarchy of the class
	 * (typically a missing dependency declaration in a Jigsaw module definition
	 * for a superclass or interface implemented by the class to be loaded here)
	 * @see #forName(String, ClassLoader)
	 */
	// 将给定的类名解析为 Class 实例。支持原语（如“int”）和数组类名（如“String[]”）。
	// <p>这实际上等效于具有相同参数的 {@code forName} 方法，唯一的区别是类加载失败时抛出的异常
	public static Class<?> resolveClassName(String className, @Nullable ClassLoader classLoader)
			throws IllegalArgumentException {

		try {
			return forName(className, classLoader);
		}
		catch (IllegalAccessError err) {
			throw new IllegalStateException("Readability mismatch in inheritance hierarchy of class [" +
					className + "]: " + err.getMessage(), err);
		}
		catch (LinkageError err) {
			throw new IllegalArgumentException("Unresolvable class definition for class [" + className + "]", err);
		}
		catch (ClassNotFoundException ex) {
			throw new IllegalArgumentException("Could not find class [" + className + "]", ex);
		}
	}

	/**
	 * Determine whether the {@link Class} identified by the supplied name is present
	 * and can be loaded. Will return {@code false} if either the class or
	 * one of its dependencies is not present or cannot be loaded.
	 * @param className the name of the class to check
	 * @param classLoader the class loader to use
	 * (may be {@code null} which indicates the default class loader)
	 * @return whether the specified class is present (including all of its
	 * superclasses and interfaces)
	 * @throws IllegalStateException if the corresponding class is resolvable but
	 * there was a readability mismatch in the inheritance hierarchy of the class
	 * (typically a missing dependency declaration in a Jigsaw module definition
	 * for a superclass or interface implemented by the class to be checked here)
	 */
	// 确定由提供的名称标识的 {@link Class} 是否存在并且可以加载。如果类或其依赖项之一不存在或无法加载，
	// 则将返回 {@code false}
	public static boolean isPresent(String className, @Nullable ClassLoader classLoader) {
		try {
			forName(className, classLoader);
			return true;
		}
		catch (IllegalAccessError err) {
			throw new IllegalStateException("Readability mismatch in inheritance hierarchy of class [" +
					className + "]: " + err.getMessage(), err);
		}
		catch (Throwable ex) {
			// Typically ClassNotFoundException or NoClassDefFoundError...
			// 通常是 ClassNotFoundException 或 NoClassDefFoundError...
			return false;
		}
	}

	/**
	 * Check whether the given class is visible in the given ClassLoader.
	 * @param clazz the class to check (typically an interface)
	 * @param classLoader the ClassLoader to check against
	 * (may be {@code null} in which case this method will always return {@code true})
	 */
	// 检查给定的类在给定的 ClassLoader 中是否可见
	public static boolean isVisible(Class<?> clazz, @Nullable ClassLoader classLoader) {
		if (classLoader == null) {
			return true;
		}
		try {
			if (clazz.getClassLoader() == classLoader) {
				return true;
			}
		}
		catch (SecurityException ex) {
			// Fall through to loadable check below
			// 通过下面的可加载检查
		}

		// Visible if same Class can be loaded from given ClassLoader
		// 如果可以从给定的 ClassLoader 加载相同的类，则可见
		return isLoadable(clazz, classLoader);
	}

	/**
	 * Check whether the given class is cache-safe in the given context,
	 * i.e. whether it is loaded by the given ClassLoader or a parent of it.
	 * @param clazz the class to analyze
	 * @param classLoader the ClassLoader to potentially cache metadata in
	 * (may be {@code null} which indicates the system class loader)
	 */
	// 检查给定的类在给定的上下文中是否是缓存安全的，即它是否由给定的 ClassLoader 或它的父级加载
	public static boolean isCacheSafe(Class<?> clazz, @Nullable ClassLoader classLoader) {
		Assert.notNull(clazz, "Class must not be null");
		try {
			ClassLoader target = clazz.getClassLoader();
			// Common cases
			// 常见案例
			if (target == classLoader || target == null) {
				return true;
			}
			if (classLoader == null) {
				return false;
			}
			// Check for match in ancestors -> positive
			// 检查祖先的匹配 -> 正面
			ClassLoader current = classLoader;
			while (current != null) {
				current = current.getParent();
				if (current == target) {
					return true;
				}
			}
			// Check for match in children -> negative
			// 检查儿童中的匹配 -> 否定
			while (target != null) {
				target = target.getParent();
				if (target == classLoader) {
					return false;
				}
			}
		}
		catch (SecurityException ex) {
			// Fall through to loadable check below
		}

		// Fallback for ClassLoaders without parent/child relationship:
		// safe if same Class can be loaded from given ClassLoader
		// 没有父子关系的类加载器的后备：如果可以从给定的类加载器加载相同的类，则安全
		return (classLoader != null && isLoadable(clazz, classLoader));
	}

	/**
	 * Check whether the given class is loadable in the given ClassLoader.
	 * @param clazz the class to check (typically an interface)
	 * @param classLoader the ClassLoader to check against
	 * @since 5.0.6
	 */
	// 检查给定的类是否可以在给定的 ClassLoader 中加载
	private static boolean isLoadable(Class<?> clazz, ClassLoader classLoader) {
		try {
			return (clazz == classLoader.loadClass(clazz.getName()));
			// Else: different class with same name found
			// 否则：找到同名的不同类
		}
		catch (ClassNotFoundException ex) {
			// No corresponding class found at all
			// 根本没有找到对应的类
			return false;
		}
	}

	/**
	 * Resolve the given class name as primitive class, if appropriate,
	 * according to the JVM's naming rules for primitive classes.
	 * <p>Also supports the JVM's internal class names for primitive arrays.
	 * Does <i>not</i> support the "[]" suffix notation for primitive arrays;
	 * this is only supported by {@link #forName(String, ClassLoader)}.
	 * @param name the name of the potentially primitive class
	 * @return the primitive class, or {@code null} if the name does not denote
	 * a primitive class or primitive array class
	 */
	// 如果合适，根据 JVM 的原始类命名规则将给定的类名解析为原始类。
	// <p>还支持原始数组的 JVM 内部类名。 <i>not<i> 是否支持原始数组的“[]”后缀表示法；这仅由
	// {@link forName(String, ClassLoader)} 支持。
	@Nullable
	public static Class<?> resolvePrimitiveClassName(@Nullable String name) {
		Class<?> result = null;
		// Most class names will be quite long, considering that they
		// SHOULD sit in a package, so a length check is worthwhile.
		// 大多数类名会很长，考虑到它们应该放在一个包中，所以长度检查是值得的
		if (name != null && name.length() <= 7) {
			// Could be a primitive - likely.
			// 可能是原始的 - 可能。
			result = primitiveTypeNameMap.get(name);
		}
		return result;
	}

	/**
	 * Check if the given class represents a primitive wrapper,
	 * i.e. Boolean, Byte, Character, Short, Integer, Long, Float, Double, or
	 * Void.
	 * @param clazz the class to check
	 * @return whether the given class is a primitive wrapper class
	 */
	// 检查给定的类是否表示原始包装器，即 Boolean、Byte、Character、Short、Integer、Long、Float、Double 或 Void。
	public static boolean isPrimitiveWrapper(Class<?> clazz) {
		Assert.notNull(clazz, "Class must not be null");
		return primitiveWrapperTypeMap.containsKey(clazz);
	}

	/**
	 * Check if the given class represents a primitive (i.e. boolean, byte,
	 * char, short, int, long, float, or double), {@code void}, or a wrapper for
	 * those types (i.e. Boolean, Byte, Character, Short, Integer, Long, Float,
	 * Double, or Void).
	 * @param clazz the class to check
	 * @return {@code true} if the given class represents a primitive, void, or
	 * a wrapper class
	 */
	// 检查给定的类是否代表原始类型（即boolean, byte,char, short, int, long, float, or double）、{@code void}
	// 或这些类型的包装器（即Boolean, Byte, Character, Short, Integer, Long, Float,Double, or Void）
	public static boolean isPrimitiveOrWrapper(Class<?> clazz) {
		Assert.notNull(clazz, "Class must not be null");
		return (clazz.isPrimitive() || isPrimitiveWrapper(clazz));
	}

	/**
	 * Check if the given class represents an array of primitives,
	 * i.e. boolean, byte, char, short, int, long, float, or double.
	 * @param clazz the class to check
	 * @return whether the given class is a primitive array class
	 */
	// 检查给定的类是否表示原始数组
	public static boolean isPrimitiveArray(Class<?> clazz) {
		Assert.notNull(clazz, "Class must not be null");
		return (clazz.isArray() && clazz.getComponentType().isPrimitive());
	}

	/**
	 * Check if the given class represents an array of primitive wrappers,
	 * i.e. Boolean, Byte, Character, Short, Integer, Long, Float, or Double.
	 * @param clazz the class to check
	 * @return whether the given class is a primitive wrapper array class
	 */
	// 检查给定的类是否表示原始包装数组
	public static boolean isPrimitiveWrapperArray(Class<?> clazz) {
		Assert.notNull(clazz, "Class must not be null");
		return (clazz.isArray() && isPrimitiveWrapper(clazz.getComponentType()));
	}

	/**
	 * Resolve the given class if it is a primitive class,
	 * returning the corresponding primitive wrapper type instead.
	 * @param clazz the class to check
	 * @return the original class, or a primitive wrapper for the original primitive type
	 */
	// 如果给定的类是原始类，则解析给定的类，返回相应的原始包装器类型
	public static Class<?> resolvePrimitiveIfNecessary(Class<?> clazz) {
		Assert.notNull(clazz, "Class must not be null");
		return (clazz.isPrimitive() && clazz != void.class ? primitiveTypeToWrapperMap.get(clazz) : clazz);
	}

	/**
	 * Check if the right-hand side type may be assigned to the left-hand side
	 * type, assuming setting by reflection. Considers primitive wrapper
	 * classes as assignable to the corresponding primitive types.
	 * @param lhsType the target type
	 * @param rhsType the value type that should be assigned to the target type
	 * @return if the target type is assignable from the value type
	 * @see TypeUtils#isAssignable(java.lang.reflect.Type, java.lang.reflect.Type)
	 */
	// 假设通过反射设置，检查右侧类型是否可以分配给左侧类型。将原始包装类视为可分配给相应的原始类型
	public static boolean isAssignable(Class<?> lhsType, Class<?> rhsType) {
		Assert.notNull(lhsType, "Left-hand side type must not be null");
		Assert.notNull(rhsType, "Right-hand side type must not be null");
		if (lhsType.isAssignableFrom(rhsType)) {
			return true;
		}
		if (lhsType.isPrimitive()) {
			Class<?> resolvedPrimitive = primitiveWrapperTypeMap.get(rhsType);
			return (lhsType == resolvedPrimitive);
		}
		else {
			Class<?> resolvedWrapper = primitiveTypeToWrapperMap.get(rhsType);
			return (resolvedWrapper != null && lhsType.isAssignableFrom(resolvedWrapper));
		}
	}

	/**
	 * Determine if the given type is assignable from the given value,
	 * assuming setting by reflection. Considers primitive wrapper classes
	 * as assignable to the corresponding primitive types.
	 * @param type the target type
	 * @param value the value that should be assigned to the type
	 * @return if the type is assignable from the value
	 */
	// 假设通过反射设置，确定给定类型是否可从给定值分配。将原始包装类视为可分配给相应的原始类型
	public static boolean isAssignableValue(Class<?> type, @Nullable Object value) {
		Assert.notNull(type, "Type must not be null");
		return (value != null ? isAssignable(type, value.getClass()) : !type.isPrimitive());
	}

	/**
	 * Convert a "/"-based resource path to a "."-based fully qualified class name.
	 * @param resourcePath the resource path pointing to a class
	 * @return the corresponding fully qualified class name
	 */
	// 将基于“/”的资源路径转换为基于“.”的完全限定类名。
	// 参形：
	//			resourcePath – 指向类的资源路径
	// 返回值：
	//			对应的全限定类名
	public static String convertResourcePathToClassName(String resourcePath) {
		Assert.notNull(resourcePath, "Resource path must not be null");
		return resourcePath.replace(PATH_SEPARATOR, PACKAGE_SEPARATOR);
	}

	/**
	 * Convert a "."-based fully qualified class name to a "/"-based resource path.
	 * @param className the fully qualified class name
	 * @return the corresponding resource path, pointing to the class
	 */
	// 将基于“.”的完全限定类名转换为基于“/”的资源路径。
	// @param className 完全限定的类名
	// @return 对应的资源路径，指向类
	public static String convertClassNameToResourcePath(String className) {
		Assert.notNull(className, "Class name must not be null");
		return className.replace(PACKAGE_SEPARATOR, PATH_SEPARATOR);
	}

	/**
	 * Return a path suitable for use with {@code ClassLoader.getResource}
	 * (also suitable for use with {@code Class.getResource} by prepending a
	 * slash ('/') to the return value). Built by taking the package of the specified
	 * class file, converting all dots ('.') to slashes ('/'), adding a trailing slash
	 * if necessary, and concatenating the specified resource name to this.
	 * <br/>As such, this function may be used to build a path suitable for
	 * loading a resource file that is in the same package as a class file,
	 * although {@link org.springframework.core.io.ClassPathResource} is usually
	 * even more convenient.
	 * @param clazz the Class whose package will be used as the base
	 * @param resourceName the resource name to append. A leading slash is optional.
	 * @return the built-up resource path
	 * @see ClassLoader#getResource
	 * @see Class#getResource
	 */
	// 返回适合与ClassLoader.getResource一起使用的路径（也适合与Class.getResource一起使用，
	// 方法是在返回值前加上斜杠 ('/')）。通过获取指定类文件的包，将所有点（'.'）转换为斜杠（'/'），
	// 必要时添加尾部斜杠，并将指定的资源名称连接到此来构建。 因此，此函数可用于构建适合加载与类文件
	// 在同一包中的资源文件的路径，尽管org.springframework.core.io.ClassPathResource通常更方便。
	// 参形：
	//			clazz – 其包将用作基础的类
	//			resourceName – 要附加的资源名称。前导斜杠是可选的。
	// 返回值：
	//			构建的资源路径
	public static String addResourcePathToPackagePath(Class<?> clazz, String resourceName) {
		Assert.notNull(resourceName, "Resource name must not be null");
		if (!resourceName.startsWith("/")) {
			return classPackageAsResourcePath(clazz) + '/' + resourceName;
		}
		return classPackageAsResourcePath(clazz) + resourceName;
	}

	/**
	 * Given an input class object, return a string which consists of the
	 * class's package name as a pathname, i.e., all dots ('.') are replaced by
	 * slashes ('/'). Neither a leading nor trailing slash is added. The result
	 * could be concatenated with a slash and the name of a resource and fed
	 * directly to {@code ClassLoader.getResource()}. For it to be fed to
	 * {@code Class.getResource} instead, a leading slash would also have
	 * to be prepended to the returned value.
	 * @param clazz the input class. A {@code null} value or the default
	 * (empty) package will result in an empty string ("") being returned.
	 * @return a path which represents the package name
	 * @see ClassLoader#getResource
	 * @see Class#getResource
	 */
	// 给定一个输入类对象，返回一个由类的包名作为路径名组成的字符串，即所有点 ('.') 都被斜杠 ('/') 替换。
	// 既不添加前斜线也不添加斜线。结果可以与斜杠和资源名称连接，并直接提供给ClassLoader.getResource() 。
	// 为了将其提供给Class.getResource ，还必须在返回值前添加一个前导斜杠。
	// 参形：
	//			clazz - 输入类。 null值或默认（空）包将导致返回空字符串（“”）。
	// 返回值：
	//			代表包名的路径
	public static String classPackageAsResourcePath(@Nullable Class<?> clazz) {
		if (clazz == null) {
			return "";
		}
		String className = clazz.getName();
		int packageEndIndex = className.lastIndexOf(PACKAGE_SEPARATOR);
		if (packageEndIndex == -1) {
			return "";
		}
		String packageName = className.substring(0, packageEndIndex);
		return packageName.replace(PACKAGE_SEPARATOR, PATH_SEPARATOR);
	}

	/**
	 * Build a String that consists of the names of the classes/interfaces
	 * in the given array.
	 * <p>Basically like {@code AbstractCollection.toString()}, but stripping
	 * the "class "/"interface " prefix before every class name.
	 * @param classes an array of Class objects
	 * @return a String of form "[com.foo.Bar, com.foo.Baz]"
	 * @see java.util.AbstractCollection#toString()
	 */
	// 构建一个由给定数组中的类/接口名称组成的字符串。
	// 基本上像AbstractCollection.toString() ，但在每个类名之前去掉“类”/“接口”前缀。
	// 参形：
	//				classes – 一个 Class 对象数组
	// 返回值：
	//				形式为“[com.foo.Bar, com.foo.Baz]”的字符串
	public static String classNamesToString(Class<?>... classes) {
		return classNamesToString(Arrays.asList(classes));
	}

	/**
	 * Build a String that consists of the names of the classes/interfaces
	 * in the given collection.
	 * <p>Basically like {@code AbstractCollection.toString()}, but stripping
	 * the "class "/"interface " prefix before every class name.
	 * @param classes a Collection of Class objects (may be {@code null})
	 * @return a String of form "[com.foo.Bar, com.foo.Baz]"
	 * @see java.util.AbstractCollection#toString()
	 */
	// 构建一个由给定集合中的类/接口名称组成的字符串。
	// 基本上像AbstractCollection.toString() ，但在每个类名之前去掉“类”/“接口”前缀。
	// 参形：
	//			classes – Class 对象的集合（可能为null ）
	// 返回值：
	//			形式为“[com.foo.Bar, com.foo.Baz]”的字符串
	public static String classNamesToString(@Nullable Collection<Class<?>> classes) {
		if (CollectionUtils.isEmpty(classes)) {
			return "[]";
		}
		StringJoiner stringJoiner = new StringJoiner(", ", "[", "]");
		for (Class<?> clazz : classes) {
			stringJoiner.add(clazz.getName());
		}
		return stringJoiner.toString();
	}

	/**
	 * Copy the given {@code Collection} into a {@code Class} array.
	 * <p>The {@code Collection} must contain {@code Class} elements only.
	 * @param collection the {@code Collection} to copy
	 * @return the {@code Class} array
	 * @since 3.1
	 * @see StringUtils#toStringArray
	 */
	// 将给定的Collection复制到Class数组中。
	// Collection必须仅包含Class元素。
	// 参形：
	//			collection - 要复制的 Collection
	// 返回值：
	//			Class 数组
	public static Class<?>[] toClassArray(@Nullable Collection<Class<?>> collection) {
		return (!CollectionUtils.isEmpty(collection) ? collection.toArray(EMPTY_CLASS_ARRAY) : EMPTY_CLASS_ARRAY);
	}

	/**
	 * Return all interfaces that the given instance implements as an array,
	 * including ones implemented by superclasses.
	 * @param instance the instance to analyze for interfaces
	 * @return all interfaces that the given instance implements as an array
	 */
	// 返回给定实例作为数组实现的所有接口，包括由超类实现的接口。
	// 参形：
	//			instance – 要分析接口的实例
	// 返回值：
	//			给定实例作为数组实现的所有接口
	public static Class<?>[] getAllInterfaces(Object instance) {
		Assert.notNull(instance, "Instance must not be null");
		return getAllInterfacesForClass(instance.getClass());
	}

	/**
	 * Return all interfaces that the given class implements as an array,
	 * including ones implemented by superclasses.
	 * <p>If the class itself is an interface, it gets returned as sole interface.
	 * @param clazz the class to analyze for interfaces
	 * @return all interfaces that the given object implements as an array
	 */
	// 返回给定类作为数组实现的所有接口，包括由超类实现的接口。
	// 如果类本身是一个接口，它将作为唯一接口返回。
	// 参形：
	//				clazz – 分析接口的类
	// 返回值：
	//				给定对象作为数组实现的所有接
	public static Class<?>[] getAllInterfacesForClass(Class<?> clazz) {
		return getAllInterfacesForClass(clazz, null);
	}

	/**
	 * Return all interfaces that the given class implements as an array,
	 * including ones implemented by superclasses.
	 * <p>If the class itself is an interface, it gets returned as sole interface.
	 * @param clazz the class to analyze for interfaces
	 * @param classLoader the ClassLoader that the interfaces need to be visible in
	 * (may be {@code null} when accepting all declared interfaces)
	 * @return all interfaces that the given object implements as an array
	 */
	// 返回给定类作为数组实现的所有接口，包括由超类实现的接口。
	// 如果类本身是一个接口，它将作为唯一接口返回。
	// 参形：
	//			clazz – 分析接口的类
	//			classLoader – 接口需要在其中可见的 ClassLoader（接受所有声明的接口时可能为null ）
	// 返回值：
	//			给定对象作为数组实现的所有接口
	public static Class<?>[] getAllInterfacesForClass(Class<?> clazz, @Nullable ClassLoader classLoader) {
		return toClassArray(getAllInterfacesForClassAsSet(clazz, classLoader));
	}

	/**
	 * Return all interfaces that the given instance implements as a Set,
	 * including ones implemented by superclasses.
	 * @param instance the instance to analyze for interfaces
	 * @return all interfaces that the given instance implements as a Set
	 */
	// 返回给定实例作为 Set 实现的所有接口，包括由超类实现的接口。
	// 参形：
	//				instance – 要分析接口的实例
	// 返回值：
	//				给定实例作为 Set 实现的所有接口
	public static Set<Class<?>> getAllInterfacesAsSet(Object instance) {
		Assert.notNull(instance, "Instance must not be null");
		return getAllInterfacesForClassAsSet(instance.getClass());
	}

	/**
	 * Return all interfaces that the given class implements as a Set,
	 * including ones implemented by superclasses.
	 * <p>If the class itself is an interface, it gets returned as sole interface.
	 * @param clazz the class to analyze for interfaces
	 * @return all interfaces that the given object implements as a Set
	 */
	// 返回给定类作为 Set 实现的所有接口，包括由超类实现的接口。
	// 如果类本身是一个接口，它将作为唯一接口返回。
	// 参形：
	//				clazz – 分析接口的类
	// 返回值：
	//				给定对象作为 Set 实现的所有接口
	public static Set<Class<?>> getAllInterfacesForClassAsSet(Class<?> clazz) {
		return getAllInterfacesForClassAsSet(clazz, null);
	}

	/**
	 * Return all interfaces that the given class implements as a Set,
	 * including ones implemented by superclasses.
	 * <p>If the class itself is an interface, it gets returned as sole interface.
	 * @param clazz the class to analyze for interfaces
	 * @param classLoader the ClassLoader that the interfaces need to be visible in
	 * (may be {@code null} when accepting all declared interfaces)
	 * @return all interfaces that the given object implements as a Set
	 */
	// 返回给定类作为 Set 实现的所有接口，包括由超类实现的接口。
	// 如果类本身是一个接口，它将作为唯一接口返回。
	// 参形：
	//			clazz – 分析接口的类
	//			classLoader – 接口需要在其中可见的 ClassLoader（接受所有声明的接口时可能为null ）
	// 返回值：
	//			给定对象作为 Set 实现的所有接口
	public static Set<Class<?>> getAllInterfacesForClassAsSet(Class<?> clazz, @Nullable ClassLoader classLoader) {
		Assert.notNull(clazz, "Class must not be null");
		if (clazz.isInterface() && isVisible(clazz, classLoader)) {
			return Collections.singleton(clazz);
		}
		Set<Class<?>> interfaces = new LinkedHashSet<>();
		Class<?> current = clazz;
		while (current != null) {
			Class<?>[] ifcs = current.getInterfaces();
			for (Class<?> ifc : ifcs) {
				if (isVisible(ifc, classLoader)) {
					interfaces.add(ifc);
				}
			}
			current = current.getSuperclass();
		}
		return interfaces;
	}

	/**
	 * Create a composite interface Class for the given interfaces,
	 * implementing the given interfaces in one single Class.
	 * <p>This implementation builds a JDK proxy class for the given interfaces.
	 * @param interfaces the interfaces to merge
	 * @param classLoader the ClassLoader to create the composite Class in
	 * @return the merged interface as Class
	 * @throws IllegalArgumentException if the specified interfaces expose
	 * conflicting method signatures (or a similar constraint is violated)
	 * @see java.lang.reflect.Proxy#getProxyClass
	 */
	// 为给定接口创建一个复合接口类，在一个类中实现给定接口。
	// 这个实现为给定的接口构建了一个 JDK 代理类。
	// 参形：
	//				interfaces — 要合并的接口
	//				classLoader – 用于在其中创建复合类的 ClassLoader
	// 返回值：
	//				合并后的接口为 Class
	// 抛出：
	//				IllegalArgumentException – 如果指定的接口暴露了冲突的方法签名（或违反了类似的约束）
	@SuppressWarnings("deprecation")  // on JDK 9
	public static Class<?> createCompositeInterface(Class<?>[] interfaces, @Nullable ClassLoader classLoader) {
		Assert.notEmpty(interfaces, "Interface array must not be empty");
		return Proxy.getProxyClass(classLoader, interfaces);
	}

	/**
	 * Determine the common ancestor of the given classes, if any.
	 * @param clazz1 the class to introspect
	 * @param clazz2 the other class to introspect
	 * @return the common ancestor (i.e. common superclass, one interface
	 * extending the other), or {@code null} if none found. If any of the
	 * given classes is {@code null}, the other class will be returned.
	 * @since 3.2.6
	 */
	// 确定给定类的共同祖先（如果有）。
	// 参形：
	//			clazz1 - 内省的类
	//			clazz2 – 另一个需要自省的类
	// 返回值：
	//			公共祖先（即公共超类，一个接口扩展另一个接口），如果没有找到，则返回null 。
	//			如果任何给定的类为null ，则将返回另一个类。
	@Nullable
	public static Class<?> determineCommonAncestor(@Nullable Class<?> clazz1, @Nullable Class<?> clazz2) {
		if (clazz1 == null) {
			return clazz2;
		}
		if (clazz2 == null) {
			return clazz1;
		}
		if (clazz1.isAssignableFrom(clazz2)) {
			return clazz1;
		}
		if (clazz2.isAssignableFrom(clazz1)) {
			return clazz2;
		}
		Class<?> ancestor = clazz1;
		do {
			ancestor = ancestor.getSuperclass();
			if (ancestor == null || Object.class == ancestor) {
				return null;
			}
		}
		while (!ancestor.isAssignableFrom(clazz2));
		return ancestor;
	}

	/**
	 * Determine whether the given interface is a common Java language interface:
	 * {@link Serializable}, {@link Externalizable}, {@link Closeable}, {@link AutoCloseable},
	 * {@link Cloneable}, {@link Comparable} - all of which can be ignored when looking
	 * for 'primary' user-level interfaces. Common characteristics: no service-level
	 * operations, no bean property methods, no default methods.
	 * @param ifc the interface to check
	 * @since 5.0.3
	 */
	// 确定给定接口是否是通用 Java 语言接口： Serializable 、 Externalizable 、 Closeable 、
	// AutoCloseable 、 Cloneable 、 Comparable - 在查找“主要”用户级接口时，所有这些都可以忽略。
	// 共同特点：无服务级操作，无bean属性方法，无默认方法。
	// 参形：
	//				ifc – 要检查的接口
	public static boolean isJavaLanguageInterface(Class<?> ifc) {
		return javaLanguageInterfaces.contains(ifc);
	}

	/**
	 * Determine if the supplied class is an <em>inner class</em>,
	 * i.e. a non-static member of an enclosing class.
	 * @return {@code true} if the supplied class is an inner class
	 * @since 5.0.5
	 * @see Class#isMemberClass()
	 */
	// 确定提供的类是否是内部类，即封闭类的非静态成员。
	// 返回值：
	//				如果提供的类是内部类，则为true
	public static boolean isInnerClass(Class<?> clazz) {
		return (clazz.isMemberClass() && !Modifier.isStatic(clazz.getModifiers()));
	}

	/**
	 * Check whether the given object is a CGLIB proxy.
	 * @param object the object to check
	 * @see #isCglibProxyClass(Class)
	 * @see org.springframework.aop.support.AopUtils#isCglibProxy(Object)
	 * @deprecated as of 5.2, in favor of custom (possibly narrower) checks
	 */
	// 检查给定对象是否是 CGLIB 代理。
	// 已弃用
	// 从 5.2 开始，支持自定义（可能更窄）检查
	// 参形：
	//			object – 要检查的对象
	@Deprecated
	public static boolean isCglibProxy(Object object) {
		return isCglibProxyClass(object.getClass());
	}

	/**
	 * Check whether the specified class is a CGLIB-generated class.
	 * @param clazz the class to check
	 * @see #isCglibProxyClassName(String)
	 * @deprecated as of 5.2, in favor of custom (possibly narrower) checks
	 */
	// 检查指定的类是否是CGLIB生成的类。
	// 已弃用
	// 从 5.2 开始，支持自定义（可能更窄）检查
	// 参形：
	//			clazz – 要检查的类
	// 请参阅：
	//			isCglibProxyClassName(String)
	@Deprecated
	public static boolean isCglibProxyClass(@Nullable Class<?> clazz) {
		return (clazz != null && isCglibProxyClassName(clazz.getName()));
	}

	/**
	 * Check whether the specified class name is a CGLIB-generated class.
	 * @param className the class name to check
	 * @deprecated as of 5.2, in favor of custom (possibly narrower) checks
	 */
	// 检查指定的类名是否为CGLIB生成的类。
	// 已弃用
	// 从 5.2 开始，支持自定义（可能更窄）检查
	// 参形：
	//				className – 要检查的类名
	@Deprecated
	public static boolean isCglibProxyClassName(@Nullable String className) {
		return (className != null && className.contains(CGLIB_CLASS_SEPARATOR));
	}

	/**
	 * Return the user-defined class for the given instance: usually simply
	 * the class of the given instance, but the original class in case of a
	 * CGLIB-generated subclass.
	 * @param instance the instance to check
	 * @return the user-defined class
	 */
	// 返回给定实例的用户定义类：通常只是给定实例的类，但如果是 CGLIB 生成的子类，则返回原始类。
	// 参形：
	//			instance – 要检查的实例
	// 返回值：
	//			用户定义的类
	public static Class<?> getUserClass(Object instance) {
		Assert.notNull(instance, "Instance must not be null");
		return getUserClass(instance.getClass());
	}

	/**
	 * Return the user-defined class for the given class: usually simply the given
	 * class, but the original class in case of a CGLIB-generated subclass.
	 * @param clazz the class to check
	 * @return the user-defined class
	 */
	// 返回给定类的用户定义类：通常只是给定类，但如果是 CGLIB 生成的子类，则返回原始类。
	// 参形：
	//			clazz – 要检查的类
	// 返回值：
	//			用户定义的类
	public static Class<?> getUserClass(Class<?> clazz) {
		if (clazz.getName().contains(CGLIB_CLASS_SEPARATOR)) {
			Class<?> superclass = clazz.getSuperclass();
			if (superclass != null && superclass != Object.class) {
				return superclass;
			}
		}
		return clazz;
	}

	/**
	 * Return a descriptive name for the given object's type: usually simply
	 * the class name, but component type class name + "[]" for arrays,
	 * and an appended list of implemented interfaces for JDK proxies.
	 * @param value the value to introspect
	 * @return the qualified name of the class
	 */
	// 返回给定对象类型的描述性名称：通常只是类名，但组件类型类名 + "[]" 用于数组，以及附加的 JDK 代理实现接口列表。
	// 参形：
	//			value - 自省的价值
	// 返回值：
	//			类的限定名
	@Nullable
	public static String getDescriptiveType(@Nullable Object value) {
		if (value == null) {
			return null;
		}
		Class<?> clazz = value.getClass();
		if (Proxy.isProxyClass(clazz)) {
			String prefix = clazz.getName() + " implementing ";
			StringJoiner result = new StringJoiner(",", prefix, "");
			for (Class<?> ifc : clazz.getInterfaces()) {
				result.add(ifc.getName());
			}
			return result.toString();
		}
		else {
			return clazz.getTypeName();
		}
	}

	/**
	 * Check whether the given class matches the user-specified type name.
	 * @param clazz the class to check
	 * @param typeName the type name to match
	 */
	// 检查给定的类是否与用户指定的类型名称匹配。
	// 参形：
	//			clazz – 要检查的类
	//			typeName – 要匹配的类型名称
	public static boolean matchesTypeName(Class<?> clazz, @Nullable String typeName) {
		return (typeName != null &&
				(typeName.equals(clazz.getTypeName()) || typeName.equals(clazz.getSimpleName())));
	}

	/**
	 * Get the class name without the qualified package name.
	 * @param className the className to get the short name for
	 * @return the class name of the class without the package name
	 * @throws IllegalArgumentException if the className is empty
	 */
	// 获取没有限定包名的类名。
	// 参形：
	//			className – 获取短名称的 className
	// 返回值：
	//			没有包名的类的类名
	public static String getShortName(String className) {
		Assert.hasLength(className, "Class name must not be empty");
		int lastDotIndex = className.lastIndexOf(PACKAGE_SEPARATOR);
		int nameEndIndex = className.indexOf(CGLIB_CLASS_SEPARATOR);
		if (nameEndIndex == -1) {
			nameEndIndex = className.length();
		}
		String shortName = className.substring(lastDotIndex + 1, nameEndIndex);
		shortName = shortName.replace(NESTED_CLASS_SEPARATOR, PACKAGE_SEPARATOR);
		return shortName;
	}

	/**
	 * Get the class name without the qualified package name.
	 * @param clazz the class to get the short name for
	 * @return the class name of the class without the package name
	 */
	// 获取没有限定包名的类名。
	// 参形：
	//			clazz – 获取短名称的类
	// 返回值：
	//			没有包名的类的类名
	public static String getShortName(Class<?> clazz) {
		return getShortName(getQualifiedName(clazz));
	}

	/**
	 * Return the short string name of a Java class in uncapitalized JavaBeans
	 * property format. Strips the outer class name in case of a nested class.
	 * @param clazz the class
	 * @return the short name rendered in a standard JavaBeans property format
	 * @see java.beans.Introspector#decapitalize(String)
	 */
	// 以非大写的 JavaBeans 属性格式返回 Java 类的短字符串名称。在嵌套类的情况下剥离外部类名称。
	// 参形：
	//			clazz – 类
	// 返回值：
	//			以标准 JavaBeans 属性格式呈现的短名称
	public static String getShortNameAsProperty(Class<?> clazz) {
		String shortName = getShortName(clazz);
		int dotIndex = shortName.lastIndexOf(PACKAGE_SEPARATOR);
		shortName = (dotIndex != -1 ? shortName.substring(dotIndex + 1) : shortName);
		return Introspector.decapitalize(shortName);
	}

	/**
	 * Determine the name of the class file, relative to the containing
	 * package: e.g. "String.class"
	 * @param clazz the class
	 * @return the file name of the ".class" file
	 */
	// 确定类文件的名称，相对于包含的包：例如“String.class”
	// 参形：
	//			clazz – 类
	// 返回值：
	//			“.class”文件的文件名
	public static String getClassFileName(Class<?> clazz) {
		Assert.notNull(clazz, "Class must not be null");
		String className = clazz.getName();
		int lastDotIndex = className.lastIndexOf(PACKAGE_SEPARATOR);
		return className.substring(lastDotIndex + 1) + CLASS_FILE_SUFFIX;
	}

	/**
	 * Determine the name of the package of the given class,
	 * e.g. "java.lang" for the {@code java.lang.String} class.
	 * @param clazz the class
	 * @return the package name, or the empty String if the class
	 * is defined in the default package
	 */
	// 确定给定类的包名，例如java.lang.String类的“java.lang”。
	// 参形：
	//			clazz – 类
	// 返回值：
	//			包名，如果类是在默认包中定义的，则为空字符串
	public static String getPackageName(Class<?> clazz) {
		Assert.notNull(clazz, "Class must not be null");
		return getPackageName(clazz.getName());
	}

	/**
	 * Determine the name of the package of the given fully-qualified class name,
	 * e.g. "java.lang" for the {@code java.lang.String} class name.
	 * @param fqClassName the fully-qualified class name
	 * @return the package name, or the empty String if the class
	 * is defined in the default package
	 */
	// 确定给定全限定类名的包名，例如java.lang.String类名的“java.lang”。
	// 参形：
	//			fqClassName – 完全限定的类名
	// 返回值：
	//			包名，如果类是在默认包中定义的，则为空字符串
	public static String getPackageName(String fqClassName) {
		Assert.notNull(fqClassName, "Class name must not be null");
		int lastDotIndex = fqClassName.lastIndexOf(PACKAGE_SEPARATOR);
		return (lastDotIndex != -1 ? fqClassName.substring(0, lastDotIndex) : "");
	}

	/**
	 * Return the qualified name of the given class: usually simply
	 * the class name, but component type class name + "[]" for arrays.
	 * @param clazz the class
	 * @return the qualified name of the class
	 */
	// 返回给定类的限定名：通常只是类名，但组件类型类名 + "[]" 用于数组。
	// 参形：
	//			clazz – 类
	// 返回值：
	//			类的限定名
	public static String getQualifiedName(Class<?> clazz) {
		Assert.notNull(clazz, "Class must not be null");
		return clazz.getTypeName();
	}

	/**
	 * Return the qualified name of the given method, consisting of
	 * fully qualified interface/class name + "." + method name.
	 * @param method the method
	 * @return the qualified name of the method
	 */
	// 返回给定方法的限定名称，由完全限定的接口类名称+“.”组成。 + 方法名称
	// 形参:
	//			method – the method
	// 返回值:
	//			the qualified name of the method
	public static String getQualifiedMethodName(Method method) {
		return getQualifiedMethodName(method, null);
	}

	/**
	 * Return the qualified name of the given method, consisting of
	 * fully qualified interface/class name + "." + method name.
	 * @param method the method
	 * @param clazz the clazz that the method is being invoked on
	 * (may be {@code null} to indicate the method's declaring class)
	 * @return the qualified name of the method
	 * @since 4.3.4
	 */
	// 返回给定方法的限定名，由完全限定的接口/类名+“.”组成+ 方法名称。
	// 参形：
	//			method — 方法
	//			clazz – 调用方法的 clazz（可能为null以指示方法的声明类）
	// 返回值：
	//			方法的限定名称
	public static String getQualifiedMethodName(Method method, @Nullable Class<?> clazz) {
		Assert.notNull(method, "Method must not be null");
		return (clazz != null ? clazz : method.getDeclaringClass()).getName() + '.' + method.getName();
	}

	/**
	 * Determine whether the given class has a public constructor with the given signature.
	 * <p>Essentially translates {@code NoSuchMethodException} to "false".
	 * @param clazz the clazz to analyze
	 * @param paramTypes the parameter types of the method
	 * @return whether the class has a corresponding constructor
	 * @see Class#getConstructor
	 */
	// 确定给定类是否具有具有给定签名的公共构造函数。
	// 本质 NoSuchMethodException 转换为“false”。
	// 参形：
	//			clazz – 分析的 clazz
	//			paramTypes – 方法的参数类型
	// 返回值：
	//			类是否有对应的构造函数
	public static boolean hasConstructor(Class<?> clazz, Class<?>... paramTypes) {
		return (getConstructorIfAvailable(clazz, paramTypes) != null);
	}

	/**
	 * Determine whether the given class has a public constructor with the given signature,
	 * and return it if available (else return {@code null}).
	 * <p>Essentially translates {@code NoSuchMethodException} to {@code null}.
	 * @param clazz the clazz to analyze
	 * @param paramTypes the parameter types of the method
	 * @return the constructor, or {@code null} if not found
	 * @see Class#getConstructor
	 */
	// 确定给定类是否具有具有给定签名的公共构造函数，如果可用则返回它（否则返回null ）。
	// 本质 NoSuchMethodException 转换为 null 。
	// 参形：
	//			clazz – 分析的 clazz
	//			paramTypes – 方法的参数类型
	// 返回值：
	//			构造函数，如果未找到，则null
	@Nullable
	public static <T> Constructor<T> getConstructorIfAvailable(Class<T> clazz, Class<?>... paramTypes) {
		Assert.notNull(clazz, "Class must not be null");
		try {
			return clazz.getConstructor(paramTypes);
		}
		catch (NoSuchMethodException ex) {
			return null;
		}
	}

	/**
	 * Determine whether the given class has a public method with the given signature.
	 * @param clazz the clazz to analyze
	 * @param method the method to look for
	 * @return whether the class has a corresponding method
	 * @since 5.2.3
	 */
	// 确定给定类是否具有具有给定签名的公共方法。
	// 参形：
	//			clazz – 分析的 clazz
	//			method - 寻找的方法
	// 返回值：
	//			类是否有对应的方法
	public static boolean hasMethod(Class<?> clazz, Method method) {
		Assert.notNull(clazz, "Class must not be null");
		Assert.notNull(method, "Method must not be null");
		if (clazz == method.getDeclaringClass()) {
			return true;
		}
		String methodName = method.getName();
		Class<?>[] paramTypes = method.getParameterTypes();
		return getMethodOrNull(clazz, methodName, paramTypes) != null;
	}

	/**
	 * Determine whether the given class has a public method with the given signature.
	 * <p>Essentially translates {@code NoSuchMethodException} to "false".
	 * @param clazz the clazz to analyze
	 * @param methodName the name of the method
	 * @param paramTypes the parameter types of the method
	 * @return whether the class has a corresponding method
	 * @see Class#getMethod
	 */
	// 确定给定类是否具有具有给定签名的公共方法。
	// 本质 NoSuchMethodException 转换为“false”。
	// 参形：
	//			clazz – 分析的 clazz
	//			methodName – 方法的名称
	//			paramTypes – 方法的参数类型
	// 返回值：
	//			类是否有对应的方法
	public static boolean hasMethod(Class<?> clazz, String methodName, Class<?>... paramTypes) {
		return (getMethodIfAvailable(clazz, methodName, paramTypes) != null);
	}

	/**
	 * Determine whether the given class has a public method with the given signature,
	 * and return it if available (else throws an {@code IllegalStateException}).
	 * <p>In case of any signature specified, only returns the method if there is a
	 * unique candidate, i.e. a single public method with the specified name.
	 * <p>Essentially translates {@code NoSuchMethodException} to {@code IllegalStateException}.
	 * @param clazz the clazz to analyze
	 * @param methodName the name of the method
	 * @param paramTypes the parameter types of the method
	 * (may be {@code null} to indicate any signature)
	 * @return the method (never {@code null})
	 * @throws IllegalStateException if the method has not been found
	 * @see Class#getMethod
	 */
	// 确定给定类是否具有具有给定签名的公共方法，如果可用则返回它（否则抛出IllegalStateException ）。
	// 如果指定了任何签名，则仅在存在唯一候选者时返回该方法，即具有指定名称的单个公共方法。
	// 本质 NoSuchMethodException 转换为 IllegalStateException 。
	// 参形：
	//			clazz – 分析的 clazz
	//			methodName – 方法的名称
	//			paramTypes – 方法的参数类型（可以为null表示任何签名）
	// 返回值：
	//			方法（从不为null ）
	// 抛出：
	//			IllegalStateException – 如果尚未找到该方法
	public static Method getMethod(Class<?> clazz, String methodName, @Nullable Class<?>... paramTypes) {
		Assert.notNull(clazz, "Class must not be null");
		Assert.notNull(methodName, "Method name must not be null");
		if (paramTypes != null) {
			try {
				return clazz.getMethod(methodName, paramTypes);
			}
			catch (NoSuchMethodException ex) {
				throw new IllegalStateException("Expected method not found: " + ex);
			}
		}
		else {
			Set<Method> candidates = findMethodCandidatesByName(clazz, methodName);
			if (candidates.size() == 1) {
				return candidates.iterator().next();
			}
			else if (candidates.isEmpty()) {
				throw new IllegalStateException("Expected method not found: " + clazz.getName() + '.' + methodName);
			}
			else {
				throw new IllegalStateException("No unique method found: " + clazz.getName() + '.' + methodName);
			}
		}
	}

	/**
	 * Determine whether the given class has a public method with the given signature,
	 * and return it if available (else return {@code null}).
	 * <p>In case of any signature specified, only returns the method if there is a
	 * unique candidate, i.e. a single public method with the specified name.
	 * <p>Essentially translates {@code NoSuchMethodException} to {@code null}.
	 * @param clazz the clazz to analyze
	 * @param methodName the name of the method
	 * @param paramTypes the parameter types of the method
	 * (may be {@code null} to indicate any signature)
	 * @return the method, or {@code null} if not found
	 * @see Class#getMethod
	 */
	// 确定给定类是否具有具有给定签名的公共方法，如果可用则返回它（否则返回null ）。
	// 如果指定了任何签名，则仅在存在唯一候选者时返回该方法，即具有指定名称的单个公共方法。
	// 本质 NoSuchMethodException 转换为 null 。
	// 参形：
	//			clazz – 分析的 clazz
	//			methodName – 方法的名称
	//			paramTypes – 方法的参数类型（可以为null表示任何签名）
	// 返回值：
	//			方法，如果未找到，则 null
	@Nullable
	public static Method getMethodIfAvailable(Class<?> clazz, String methodName, @Nullable Class<?>... paramTypes) {
		Assert.notNull(clazz, "Class must not be null");
		Assert.notNull(methodName, "Method name must not be null");
		if (paramTypes != null) {
			return getMethodOrNull(clazz, methodName, paramTypes);
		}
		else {
			Set<Method> candidates = findMethodCandidatesByName(clazz, methodName);
			if (candidates.size() == 1) {
				return candidates.iterator().next();
			}
			return null;
		}
	}

	/**
	 * Return the number of methods with a given name (with any argument types),
	 * for the given class and/or its superclasses. Includes non-public methods.
	 * @param clazz	the clazz to check
	 * @param methodName the name of the method
	 * @return the number of methods with the given name
	 */
	// 返回给定类和/或其超类的具有给定名称（具有任何参数类型）的方法的数量。包括非公共方法。
	// 参形：
	//			clazz – 要检查的 clazz
	//			methodName – 方法的名称
	// 返回值：
	//			具有给定名称的方法的数量
	public static int getMethodCountForName(Class<?> clazz, String methodName) {
		Assert.notNull(clazz, "Class must not be null");
		Assert.notNull(methodName, "Method name must not be null");
		int count = 0;
		Method[] declaredMethods = clazz.getDeclaredMethods();
		for (Method method : declaredMethods) {
			if (methodName.equals(method.getName())) {
				count++;
			}
		}
		Class<?>[] ifcs = clazz.getInterfaces();
		for (Class<?> ifc : ifcs) {
			count += getMethodCountForName(ifc, methodName);
		}
		if (clazz.getSuperclass() != null) {
			count += getMethodCountForName(clazz.getSuperclass(), methodName);
		}
		return count;
	}

	/**
	 * Does the given class or one of its superclasses at least have one or more
	 * methods with the supplied name (with any argument types)?
	 * Includes non-public methods.
	 * @param clazz	the clazz to check
	 * @param methodName the name of the method
	 * @return whether there is at least one method with the given name
	 */
	// 给定的类或其超类之一是否至少具有一个或多个具有所提供名称的方法（具有任何参数类型）？包括非公共方法。
	// 参形：
	//			clazz – 要检查的 clazz
	//			methodName – 方法的名称
	// 返回值：
	//			是否存在至少一种具有给定名称的方法
	public static boolean hasAtLeastOneMethodWithName(Class<?> clazz, String methodName) {
		Assert.notNull(clazz, "Class must not be null");
		Assert.notNull(methodName, "Method name must not be null");
		Method[] declaredMethods = clazz.getDeclaredMethods();
		for (Method method : declaredMethods) {
			if (method.getName().equals(methodName)) {
				return true;
			}
		}
		Class<?>[] ifcs = clazz.getInterfaces();
		for (Class<?> ifc : ifcs) {
			if (hasAtLeastOneMethodWithName(ifc, methodName)) {
				return true;
			}
		}
		return (clazz.getSuperclass() != null && hasAtLeastOneMethodWithName(clazz.getSuperclass(), methodName));
	}

	/**
	 * Given a method, which may come from an interface, and a target class used
	 * in the current reflective invocation, find the corresponding target method
	 * if there is one. E.g. the method may be {@code IFoo.bar()} and the
	 * target class may be {@code DefaultFoo}. In this case, the method may be
	 * {@code DefaultFoo.bar()}. This enables attributes on that method to be found.
	 * <p><b>NOTE:</b> In contrast to {@link org.springframework.aop.support.AopUtils#getMostSpecificMethod},
	 * this method does <i>not</i> resolve Java 5 bridge methods automatically.
	 * Call {@link org.springframework.core.BridgeMethodResolver#findBridgedMethod}
	 * if bridge method resolution is desirable (e.g. for obtaining metadata from
	 * the original method definition).
	 * <p><b>NOTE:</b> Since Spring 3.1.1, if Java security settings disallow reflective
	 * access (e.g. calls to {@code Class#getDeclaredMethods} etc, this implementation
	 * will fall back to returning the originally provided method.
	 * @param method the method to be invoked, which may come from an interface
	 * @param targetClass the target class for the current invocation
	 * (may be {@code null} or may not even implement the method)
	 * @return the specific target method, or the original method if the
	 * {@code targetClass} does not implement it
	 * @see #getInterfaceMethodIfPossible
	 */
	// 给定一个可能来自接口的方法，以及当前反射调用中使用的目标类，如果有，则找到对应的目标方法。
	// 例如，该方法可能是IFoo.bar()并且目标类可能是DefaultFoo 。在这种情况下，该方法可能是DefaultFoo.bar() 。
	// 这使得可以找到该方法的属性。
	//
	// 注意：与org.springframework.aop.support.AopUtils.getMostSpecificMethod相比，
	// 此方法不会自动解析 Java 5 桥接方法。如果需要桥方法解析（例如，从原始方法定义中获取元数据），
	// 请调用org.springframework.core.BridgeMethodResolver.findBridgedMethod 。
	//
	// 注意：从 Spring 3.1.1 开始，如果 Java 安全设置不允许反射访问（例如调用Class#getDeclaredMethods等，
	// 此实现将回退到返回最初提供的方法。
	// 参形：
	//			method -- 要调用的方法，可能来自接口
	//			targetClass – 当前调用的目标类（可能为null ，甚至可能不实现该方法）
	// 返回值：
	//			特定的目标方法，或者如果targetClass没有实现它，则为原始方法
	public static Method getMostSpecificMethod(Method method, @Nullable Class<?> targetClass) {
		if (targetClass != null && targetClass != method.getDeclaringClass() && isOverridable(method, targetClass)) {
			try {
				if (Modifier.isPublic(method.getModifiers())) {
					try {
						return targetClass.getMethod(method.getName(), method.getParameterTypes());
					}
					catch (NoSuchMethodException ex) {
						return method;
					}
				}
				else {
					Method specificMethod =
							ReflectionUtils.findMethod(targetClass, method.getName(), method.getParameterTypes());
					return (specificMethod != null ? specificMethod : method);
				}
			}
			catch (SecurityException ex) {
				// Security settings are disallowing reflective access; fall back to 'method' below.
			}
		}
		return method;
	}

	/**
	 * Determine a corresponding interface method for the given method handle, if possible.
	 * <p>This is particularly useful for arriving at a public exported type on Jigsaw
	 * which can be reflectively invoked without an illegal access warning.
	 * @param method the method to be invoked, potentially from an implementation class
	 * @return the corresponding interface method, or the original method if none found
	 * @since 5.1
	 * @see #getMostSpecificMethod
	 */
	// 如果可能，为给定的方法句柄确定相应的接口方法。
	// 这对于在 Jigsaw 上获得公共导出类型特别有用，可以在没有非法访问警告的情况下进行反射调用。
	// 参形：
	//				method - 要调用的方法，可能来自实现类
	// 返回值：
	//				相应的接口方法，如果没有找到，则为原始方法
	public static Method getInterfaceMethodIfPossible(Method method) {
		if (!Modifier.isPublic(method.getModifiers()) || method.getDeclaringClass().isInterface()) {
			return method;
		}
		return interfaceMethodCache.computeIfAbsent(method, key -> {
			Class<?> current = key.getDeclaringClass();
			while (current != null && current != Object.class) {
				Class<?>[] ifcs = current.getInterfaces();
				for (Class<?> ifc : ifcs) {
					try {
						return ifc.getMethod(key.getName(), key.getParameterTypes());
					}
					catch (NoSuchMethodException ex) {
						// ignore
					}
				}
				current = current.getSuperclass();
			}
			return key;
		});
	}

	/**
	 * Determine whether the given method is declared by the user or at least pointing to
	 * a user-declared method.
	 * <p>Checks {@link Method#isSynthetic()} (for implementation methods) as well as the
	 * {@code GroovyObject} interface (for interface methods; on an implementation class,
	 * implementations of the {@code GroovyObject} methods will be marked as synthetic anyway).
	 * Note that, despite being synthetic, bridge methods ({@link Method#isBridge()}) are considered
	 * as user-level methods since they are eventually pointing to a user-declared generic method.
	 * @param method the method to check
	 * @return {@code true} if the method can be considered as user-declared; {@code false} otherwise
	 */
	// 确定给定方法是由用户声明还是至少指向用户声明的方法。
	// 检查 Method.isSynthetic() （用于实现方法）以及 GroovyObject 接口（用于接口方法；在实现类上，
	// GroovyObject 方法的实现无论如何都将被标记为合成）。请注意，尽管是合成的，桥方法（ Method.isBridge() ）
	// 被视为用户级方法，因为它们最终指向用户声明的通用方法。
	// 参形：
	//			method - 检查的方法
	// 返回值：
	//			如果方法可以被认为是用户声明的，则为 true ；否则 false
	public static boolean isUserLevelMethod(Method method) {
		Assert.notNull(method, "Method must not be null");
		return (method.isBridge() || (!method.isSynthetic() && !isGroovyObjectMethod(method)));
	}

	private static boolean isGroovyObjectMethod(Method method) {
		return method.getDeclaringClass().getName().equals("groovy.lang.GroovyObject");
	}

	/**
	 * Determine whether the given method is overridable in the given target class.
	 * @param method the method to check
	 * @param targetClass the target class to check against
	 */
	// 确定给定方法在给定目标类中是否可重写。
	// 参形：
	//			method - 检查的方法
	//			targetClass – 要检查的目标类
	private static boolean isOverridable(Method method, @Nullable Class<?> targetClass) {
		if (Modifier.isPrivate(method.getModifiers())) {
			return false;
		}
		if (Modifier.isPublic(method.getModifiers()) || Modifier.isProtected(method.getModifiers())) {
			return true;
		}
		return (targetClass == null ||
				getPackageName(method.getDeclaringClass()).equals(getPackageName(targetClass)));
	}

	/**
	 * Return a public static method of a class.
	 * @param clazz the class which defines the method
	 * @param methodName the static method name
	 * @param args the parameter types to the method
	 * @return the static method, or {@code null} if no static method was found
	 * @throws IllegalArgumentException if the method name is blank or the clazz is null
	 */
	// 返回类的公共静态方法。
	// 参形：
	//				clazz – 定义方法的类
	//				methodName – 静态方法名称
	//				args – 方法的参数类型
	// 返回值：
	//				静态方法，如果没有找到静态方法，则null
	// 抛出：
	//				IllegalArgumentException – 如果方法名称为空或 clazz 为 null
	@Nullable
	public static Method getStaticMethod(Class<?> clazz, String methodName, Class<?>... args) {
		Assert.notNull(clazz, "Class must not be null");
		Assert.notNull(methodName, "Method name must not be null");
		try {
			Method method = clazz.getMethod(methodName, args);
			return Modifier.isStatic(method.getModifiers()) ? method : null;
		}
		catch (NoSuchMethodException ex) {
			return null;
		}
	}


	@Nullable
	private static Method getMethodOrNull(Class<?> clazz, String methodName, Class<?>[] paramTypes) {
		try {
			return clazz.getMethod(methodName, paramTypes);
		}
		catch (NoSuchMethodException ex) {
			return null;
		}
	}

	private static Set<Method> findMethodCandidatesByName(Class<?> clazz, String methodName) {
		Set<Method> candidates = new HashSet<>(1);
		Method[] methods = clazz.getMethods();
		for (Method method : methods) {
			if (methodName.equals(method.getName())) {
				candidates.add(method);
			}
		}
		return candidates;
	}

}
